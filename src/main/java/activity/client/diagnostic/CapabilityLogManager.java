package activity.client.diagnostic;

import activity.client.gui.overlay.ClientNotification;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class CapabilityLogManager {
    private static final int MAX_ENTRIES = 2000;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());
    private static final File LOG_FILE = new File("logs", "capabilities.log");

    public record CapabilityEntry(long timestamp, String capability, String action, boolean success, String details) {
        public String formatLine() {
            String time = TIME_FMT.format(Instant.ofEpochMilli(timestamp));
            String status = success ? "SUCCESS" : "FAIL";
            return String.format(Locale.ROOT, "[%s] [%s] %s -> %s [%s]", time, capability.toUpperCase(Locale.ROOT), action, details, status);
        }
    }

    private static final ConcurrentLinkedDeque<CapabilityEntry> entries = new ConcurrentLinkedDeque<>();
    private static volatile boolean loggingEnabled = true;
    private static volatile boolean hudFeedback = false;
    private static volatile boolean fileReady = false;

    private CapabilityLogManager() {}

    public static boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    public static void setLoggingEnabled(boolean enabled) {
        loggingEnabled = enabled;
    }

    public static boolean isHudFeedbackEnabled() {
        return hudFeedback;
    }

    public static void setHudFeedbackEnabled(boolean feedback) {
        hudFeedback = feedback;
    }

    public static void log(String capability, String action, boolean success, String details) {
        if (!loggingEnabled) return;
        CapabilityEntry entry = new CapabilityEntry(System.currentTimeMillis(), capability != null ? capability : "ability", action != null ? action : "use", success, details != null ? details : "");
        entries.addLast(entry);
        while (entries.size() > MAX_ENTRIES) {
            entries.pollFirst();
        }

        DiagnosticEngine.recordAction(capability, action, success, details);
        appendToFile(entry.formatLine());

        if (hudFeedback) {
            notifyActionbar(entry);
        }
    }

    private static synchronized void appendToFile(String line) {
        try {
            if (!fileReady) {
                File parent = LOG_FILE.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                fileReady = true;
            }
            if (LOG_FILE.exists() && LOG_FILE.length() > 3 * 1024 * 1024L) {
                File backup = new File(LOG_FILE.getParentFile(), "capabilities.log.old");
                if (backup.exists()) backup.delete();
                LOG_FILE.renameTo(backup);
            }
            Files.writeString(LOG_FILE.toPath(), line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable ignored) {}
    }

    private static void notifyActionbar(CapabilityEntry entry) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null || mc.player == null) return;
            String color = entry.success() ? "§a" : "§c";
            String msg = String.format("§7[§6%s§7] %s%s§7: %s", entry.capability(), color, entry.action(), entry.details());
            mc.player.sendMessage(Text.literal(msg), true);
        } catch (Throwable ignored) {}
    }

    public static List<CapabilityEntry> getHistory() {
        return new ArrayList<>(entries);
    }

    public static int getEntryCount() {
        return entries.size();
    }

    public static void clear() {
        entries.clear();
        try {
            if (LOG_FILE.exists()) {
                Files.writeString(LOG_FILE.toPath(), "", StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (Throwable ignored) {}
        ClientNotification.show(Text.literal(activity.client.i18n.LocalizationService.isRussianPreferred()
                ? "История логов способностей очищена"
                : "Capability log history cleared"));
    }

    public static String dumpFullReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("================================================================================\n");
        sb.append("                 NIVORAT CLIENT CAPABILITY EXECUTION LOG                        \n");
        sb.append("================================================================================\n");
        sb.append("Export Time: ").append(TIME_FMT.format(Instant.now())).append("\n");
        sb.append("Total Events Recorded: ").append(entries.size()).append("\n");
        sb.append("Operating System: ").append(System.getProperty("os.name")).append("\n");
        sb.append("Java: ").append(System.getProperty("java.version")).append("\n\n");

        if (entries.isEmpty()) {
            sb.append("No capability events recorded in this session yet.\n");
        } else {
            for (CapabilityEntry entry : entries) {
                sb.append(entry.formatLine()).append("\n");
            }
        }
        sb.append("================================================================================\n");
        return sb.toString();
    }

    public static boolean copyToClipboard() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return false;
        String dump = dumpFullReport();
        int count = entries.size();
        mc.keyboard.setClipboard(dump);
        ClientNotification.show(Text.literal(activity.client.i18n.LocalizationService.isRussianPreferred()
                ? "Логи скопированы в буфер! (" + count + " событий)"
                : "Logs copied to clipboard! (" + count + " events)"));
        return true;
    }

    public static void openLogsFolder() {
        try {
            File logsDir = new File("logs");
            if (!logsDir.exists()) logsDir.mkdirs();
            Util.getOperatingSystem().open(logsDir);
        } catch (Throwable ignored) {}
    }
}

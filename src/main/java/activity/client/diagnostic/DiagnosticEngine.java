package activity.client.diagnostic;

import activity.client.module.api.IModule;
import activity.client.module.api.ModuleRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

public final class DiagnosticEngine {
    private static final int MAX_FPS_DROPS = 100;
    private static final int MAX_ACTIONS = 250;
    private static final int MAX_ERRORS = 100;
    private static final int MAX_PACKETS = 500;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter FULL_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());
    private static final File CRASH_LOG_FILE = new File("logs", "crash_diagnostic.log");

    private static volatile boolean active = true;
    private static long lastFrameTimeNs = 0L;
    private static long frameCount = 0L;
    private static double rollingFps = 60.0;

    static {
        try {
            Thread.UncaughtExceptionHandler orig = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
                try {
                    recordError("crash_handler", t.getName(), e);
                    writeCrashReport(t, e);
                } catch (Throwable ignored) {}
                if (orig != null) {
                    orig.uncaughtException(t, e);
                }
            });
        } catch (Throwable ignored) {}
    }

    public record FpsDropEvent(long timestampMs, double fps, long frameTimeMs, String activeModules, long usedMemMb, long maxMemMb) {}
    public record ActionEvent(long timestampMs, String module, String action, boolean success, String details) {}
    public record ErrorEvent(long timestampMs, String module, String phase, String errorType, String message, String stackTrace) {}
    public record PacketEvent(long timestampMs, String packetType, String summary) {}

    private static final ConcurrentLinkedDeque<FpsDropEvent> fpsDrops = new ConcurrentLinkedDeque<>();
    private static final ConcurrentLinkedDeque<ActionEvent> actionHistory = new ConcurrentLinkedDeque<>();
    private static final ConcurrentLinkedDeque<ErrorEvent> errorHistory = new ConcurrentLinkedDeque<>();
    private static final ConcurrentLinkedDeque<PacketEvent> packetHistory = new ConcurrentLinkedDeque<>();

    private DiagnosticEngine() {}

    public static boolean isActive() {
        return active;
    }

    public static void onFrame() {
        if (!active) return;
        long now = System.nanoTime();
        if (lastFrameTimeNs > 0L) {
            long deltaNs = now - lastFrameTimeNs;
            double currentFps = 1_000_000_000.0 / Math.max(1L, deltaNs);
            rollingFps = (rollingFps * 0.9) + (currentFps * 0.1);
            long frameMs = deltaNs / 1_000_000L;
            if (frameMs >= 35L || (rollingFps > 45.0 && currentFps < 25.0)) {
                recordFpsDrop(System.currentTimeMillis(), currentFps, frameMs);
            }
        }
        lastFrameTimeNs = now;
        frameCount++;
    }

    private static void recordFpsDrop(long nowMs, double currentFps, long frameMs) {
        if (!active) return;
        Runtime runtime = Runtime.getRuntime();
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
        long maxMb = runtime.maxMemory() / (1024L * 1024L);
        String modules = getActiveModulesString();
        fpsDrops.addLast(new FpsDropEvent(nowMs, currentFps, frameMs, modules, usedMb, maxMb));
        while (fpsDrops.size() > MAX_FPS_DROPS) {
            fpsDrops.pollFirst();
        }
    }

    public static void recordAction(String module, String action, boolean success, String details) {
        if (!active) return;
        actionHistory.addLast(new ActionEvent(System.currentTimeMillis(), module != null ? module : "unknown", action != null ? action : "action", success, details != null ? details : ""));
        while (actionHistory.size() > MAX_ACTIONS) {
            actionHistory.pollFirst();
        }
    }

    public static void recordError(String module, String phase, Throwable error) {
        if (!active) return;
        long now = System.currentTimeMillis();
        String type = error != null ? error.getClass().getSimpleName() : "Error";
        String msg = error != null && error.getMessage() != null ? error.getMessage() : "No message";
        StringBuilder trace = new StringBuilder();
        if (error != null) {
            for (int i = 0; i < Math.min(16, error.getStackTrace().length); i++) {
                trace.append(" at ").append(error.getStackTrace()[i].toString()).append("\n");
            }
        }
        errorHistory.addLast(new ErrorEvent(now, module != null ? module : "core", phase != null ? phase : "execute", type, msg, trace.toString()));
        while (errorHistory.size() > MAX_ERRORS) {
            errorHistory.pollFirst();
        }
        writeErrorLog(module, phase, type, msg, trace.toString());
    }

    private static synchronized void writeErrorLog(String module, String phase, String type, String msg, String trace) {
        try {
            File parent = CRASH_LOG_FILE.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            String time = FULL_TIME_FMT.format(Instant.now());
            String entry = String.format(Locale.ROOT, "[%s] [ERROR] [%s:%s] %s: %s%n%s", time, module, phase, type, msg, trace);
            Files.writeString(CRASH_LOG_FILE.toPath(), entry + System.lineSeparator(), StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Throwable ignored) {}
    }

    private static synchronized void writeCrashReport(Thread t, Throwable e) {
        try {
            File parent = CRASH_LOG_FILE.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            String time = FULL_TIME_FMT.format(Instant.now());
            StringBuilder sb = new StringBuilder();
            sb.append("\n==================== CRASH / UNCAUGHT EXCEPTION ====================\n");
            sb.append("Time: ").append(time).append("\n");
            sb.append("Thread: ").append(t != null ? t.getName() : "unknown").append("\n");
            sb.append("Exception: ").append(e != null ? e.getClass().getName() : "null").append(": ").append(e != null ? e.getMessage() : "null").append("\n");
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.player != null) {
                sb.append("Player Pos: ").append(String.format(Locale.ROOT, "%.2f, %.2f, %.2f", mc.player.getX(), mc.player.getY(), mc.player.getZ())).append("\n");
                sb.append("Player Yaw/Pitch: ").append(String.format(Locale.ROOT, "%.2f / %.2f", mc.player.getYaw(), mc.player.getPitch())).append("\n");
                sb.append("Held Slot: ").append(mc.player.getInventory().getSelectedSlot()).append(" (").append(mc.player.getMainHandStack().getItem().toString()).append(")\n");
                sb.append("Health: ").append(mc.player.getHealth()).append(" / ").append(mc.player.getMaxHealth()).append("\n");
            }
            if (mc != null && mc.world != null) {
                sb.append("Dimension: ").append(mc.world.getRegistryKey().getValue().toString()).append("\n");
            }
            sb.append("Recent Actions (last 10):\n");
            var actList = new ArrayList<>(actionHistory);
            int startIdx = Math.max(0, actList.size() - 10);
            for (int i = startIdx; i < actList.size(); i++) {
                ActionEvent act = actList.get(i);
                sb.append("  [").append(TIME_FMT.format(Instant.ofEpochMilli(act.timestampMs()))).append("] ")
                  .append(act.module()).append(":").append(act.action())
                  .append(" success=").append(act.success()).append(" ").append(act.details()).append("\n");
            }
            sb.append("Stacktrace:\n");
            if (e != null) {
                java.io.StringWriter sw = new java.io.StringWriter();
                e.printStackTrace(new java.io.PrintWriter(sw));
                sb.append(sw.toString());
            }
            sb.append("====================================================================\n\n");
            Files.writeString(CRASH_LOG_FILE.toPath(), sb.toString(), StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Throwable ignored) {}
    }

    public static void recordPacket(Packet<?> packet) {
        if (!active || packet == null) return;
        long now = System.currentTimeMillis();
        String name = packet.getClass().getSimpleName();
        String summary = summarizePacket(packet);
        packetHistory.addLast(new PacketEvent(now, name, summary));
        while (packetHistory.size() > MAX_PACKETS) {
            packetHistory.pollFirst();
        }
    }

    private static String summarizePacket(Packet<?> packet) {
        if (packet instanceof ClickSlotC2SPacket click) {
            return "syncId=" + click.syncId() + ", slot=" + click.slot() + ", button=" + click.button() + ", action=" + click.actionType();
        }
        if (packet instanceof UpdateSelectedSlotC2SPacket update) {
            return "slot=" + update.getSelectedSlot();
        }
        if (packet instanceof PlayerActionC2SPacket action) {
            return "action=" + action.getAction() + ", pos=" + action.getPos().toShortString() + ", dir=" + action.getDirection();
        }
        if (packet instanceof PlayerInteractItemC2SPacket interact) {
            return "hand=" + interact.getHand() + ", yaw=" + Math.round(interact.getYaw()) + ", pitch=" + Math.round(interact.getPitch());
        }
        if (packet instanceof PlayerMoveC2SPacket move) {
            return "onGround=" + move.isOnGround() + ", pos=" + move.changesPosition() + ", look=" + move.changesLook();
        }
        if (packet instanceof HandSwingC2SPacket swing) {
            return "hand=" + swing.getHand();
        }
        return packet.toString();
    }

    public static String getActiveModulesString() {
        try {
            return ModuleRegistry.getAll().stream()
                    .filter(IModule::isEnabled)
                    .map(IModule::getId)
                    .collect(Collectors.joining(", "));
        } catch (Throwable ignored) {
            return "unavailable";
        }
    }

    public static List<FpsDropEvent> getFpsDrops() {
        return List.copyOf(fpsDrops);
    }

    public static List<ActionEvent> getActionHistory() {
        return List.copyOf(actionHistory);
    }

    public static List<ErrorEvent> getErrorHistory() {
        return List.copyOf(errorHistory);
    }

    public static List<PacketEvent> getPacketHistory() {
        return List.copyOf(packetHistory);
    }

    public static String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("================================================================================\n");
        sb.append("                       NIVORAT CLIENT DIAGNOSTIC REPORT                         \n");
        sb.append("================================================================================\n");
        sb.append("Generated at: ").append(TIME_FMT.format(Instant.now())).append("\n");
        sb.append("Operating System: ").append(System.getProperty("os.name")).append(" (").append(System.getProperty("os.arch")).append(")\n");
        sb.append("Java Runtime: ").append(System.getProperty("java.version")).append(" (").append(System.getProperty("java.vendor")).append(")\n");
        Runtime runtime = Runtime.getRuntime();
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
        long maxMb = runtime.maxMemory() / (1024L * 1024L);
        sb.append("Memory Usage: ").append(usedMb).append(" MB / ").append(maxMb).append(" MB\n");
        sb.append("Current Rolling FPS: ").append(String.format(Locale.ROOT, "%.1f", rollingFps)).append("\n");
        sb.append("Active Modules: ").append(getActiveModulesString()).append("\n\n");

        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("SECTION 1: UNEXECUTED ACTIONS & FAILED CLICKS (").append(actionHistory.stream().filter(a -> !a.success()).count()).append(" failures recorded)\n");
        sb.append("--------------------------------------------------------------------------------\n");
        if (actionHistory.isEmpty()) {
            sb.append("No actions recorded.\n");
        } else {
            for (ActionEvent act : actionHistory) {
                sb.append(String.format("[%s] [%s] %s::%s -> %s\n",
                        TIME_FMT.format(Instant.ofEpochMilli(act.timestampMs())),
                        act.success() ? "SUCCESS" : "FAILED",
                        act.module(), act.action(), act.details()));
            }
        }
        sb.append("\n");

        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("SECTION 2: MODULE RUNTIME ERRORS & EXCEPTIONS (").append(errorHistory.size()).append(")\n");
        sb.append("--------------------------------------------------------------------------------\n");
        if (errorHistory.isEmpty()) {
            sb.append("No runtime errors logged.\n");
        } else {
            for (ErrorEvent err : errorHistory) {
                sb.append(String.format("[%s] Module: %s | Phase: %s | %s: %s\n%s",
                        TIME_FMT.format(Instant.ofEpochMilli(err.timestampMs())),
                        err.module(), err.phase(), err.errorType(), err.message(), err.stackTrace()));
            }
        }
        sb.append("\n");

        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("SECTION 3: FPS DROPS & STUTTER SPIKES (").append(fpsDrops.size()).append(")\n");
        sb.append("--------------------------------------------------------------------------------\n");
        if (fpsDrops.isEmpty()) {
            sb.append("No significant FPS drops recorded (all frames < 35ms).\n");
        } else {
            for (FpsDropEvent drop : fpsDrops) {
                sb.append(String.format("[%s] Drop: %.1f FPS (Frame time: %d ms) | Mem: %d/%d MB | Active: [%s]\n",
                        TIME_FMT.format(Instant.ofEpochMilli(drop.timestampMs())),
                        drop.fps(), drop.frameTimeMs(), drop.usedMemMb(), drop.maxMemMb(), drop.activeModules()));
            }
        }
        sb.append("\n");

        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("SECTION 4: RECENT OUTGOING PACKET STREAM (Last ").append(packetHistory.size()).append(" packets)\n");
        sb.append("--------------------------------------------------------------------------------\n");
        if (packetHistory.isEmpty()) {
            sb.append("No packets captured.\n");
        } else {
            for (PacketEvent pkt : packetHistory) {
                sb.append(String.format("[%s] C2S -> %s (%s)\n",
                        TIME_FMT.format(Instant.ofEpochMilli(pkt.timestampMs())),
                        pkt.packetType(), pkt.summary()));
            }
        }
        sb.append("================================================================================\n");
        return sb.toString();
    }

    public static File exportReportToFile(File destinationDirectory) {
        try {
            if (destinationDirectory == null) {
                destinationDirectory = new File("logs");
            }
            if (!destinationDirectory.exists()) {
                destinationDirectory.mkdirs();
            }
            File target = new File(destinationDirectory, "nivorat_diagnostic_report.log");
            String content = generateReport();
            Files.writeString(target.toPath(), content, StandardCharsets.UTF_8);
            return target;
        } catch (Throwable error) {
            recordError("diagnostic", "export", error);
            return null;
        }
    }

    public static void purgeForCapitulation() {
        active = false;
        fpsDrops.clear();
        actionHistory.clear();
        errorHistory.clear();
        packetHistory.clear();
        lastFrameTimeNs = 0L;
        frameCount = 0L;
        rollingFps = 60.0;
    }

    public static void resetForTesting() {
        active = true;
        fpsDrops.clear();
        actionHistory.clear();
        errorHistory.clear();
        packetHistory.clear();
        lastFrameTimeNs = 0L;
        frameCount = 0L;
        rollingFps = 60.0;
    }
}

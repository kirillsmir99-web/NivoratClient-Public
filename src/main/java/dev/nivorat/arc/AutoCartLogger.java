package dev.nivorat.arc;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class AutoCartLogger {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final File LOG_FILE = new File("logs", "autocart.log");
    private static volatile boolean fileReady = false;

    private AutoCartLogger() {}

    private static synchronized void append(String line) {
        try {
            if (!fileReady) {
                File parent = LOG_FILE.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                fileReady = true;
            }
            if (LOG_FILE.exists() && LOG_FILE.length() > 2 * 1024 * 1024L) {
                File backup = new File(LOG_FILE.getParentFile(), "autocart.log.old");
                if (backup.exists()) backup.delete();
                LOG_FILE.renameTo(backup);
            }
            Files.writeString(LOG_FILE.toPath(), line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable ignored) {}
    }

    public static void log(String phase, boolean success, String details) {
        String ts = LocalDateTime.now().format(TIME_FMT);
        String formatted = String.format("[%s] [%s] %s%s", ts, phase, details, success ? "" : " [FAIL]");
        append(formatted);
        activity.client.diagnostic.CapabilityLogManager.log("auto_cart", phase, success, details);
    }

    public static void logMacroStart(int drawTicks, int slot) {
        log("MACRO_START", true, "draw_ticks=" + drawTicks + " slot=" + slot);
    }

    public static void logAimStart(float fromPitch, float toPitch, float fromYaw, float toYaw, long durationMs, double gcd) {
        log("AIM_START", true, String.format(java.util.Locale.ROOT,
                "duration=%dms pitch=%.2f->%.2f yaw=%.2f->%.2f gcd=%.6f",
                durationMs, fromPitch, toPitch, fromYaw, toYaw, gcd));
    }

    public static void logAimFinish(long elapsedMs, float remainingError) {
        log("AIM_FINISH", true, String.format(java.util.Locale.ROOT,
                "elapsed=%dms remaining_error=%.3f°", elapsedMs, remainingError));
    }

    public static void logPlacement(String type, int x, int y, int z, long delayMs, boolean success) {
        log("PLACE_" + type.toUpperCase(java.util.Locale.ROOT), success,
                String.format(java.util.Locale.ROOT, "pos=[%d, %d, %d] delay=%dms", x, y, z, delayMs));
    }

    public static void logExplosion(double dist, int drawTicks) {
        log("EXPLOSION", true, String.format(java.util.Locale.ROOT,
                "distance=%.2fm bow_draw_ticks=%d", dist, drawTicks));
    }

    public static void logCancel(String reason) {
        log("CANCEL", false, "reason=" + reason);
    }

    public static void logNeuralDiagnostics(float inPitch, float inYaw, float profilePitch, float profileYaw, float finalPitch, float finalYaw) {
        log("NEURAL_DIAG", true, String.format(java.util.Locale.ROOT,
                "INPUT CAMERA=[p=%.2f, y=%.2f] PROFILE TARGET=[p=%.2f, y=%.2f] FINAL CAMERA OUTPUT=[p=%.2f, y=%.2f]",
                inPitch, inYaw, profilePitch, profileYaw, finalPitch, finalYaw));
    }

    public static void logSampleQuality(String action, boolean accepted, float quality, String reason) {
        log("SAMPLE_QUALITY", accepted, String.format(java.util.Locale.ROOT,
                "action=%s quality=%.2f reason=%s", action, quality, reason));
    }

    public static void logMasteryCategories(float c1, float c2, float c3, float c4, float c5, int totalMastery) {
        log("MASTERY_UPDATE", true, String.format(java.util.Locale.ROOT,
                "cam=%.2f timing=%.2f combined=%.2f real=%.2f stab=%.2f -> total=%d%%",
                c1, c2, c3, c4, c5, totalMastery));
    }
}

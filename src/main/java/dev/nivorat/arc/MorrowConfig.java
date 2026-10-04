package dev.nivorat.arc;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class MorrowConfig {
    private static Path safePath(String name) {
        try {
            var l = net.fabricmc.loader.api.FabricLoader.getInstance();
            if (l != null && l.getConfigDir() != null) return l.getConfigDir().resolve(name);
        } catch (Throwable ignored) {}
        return Path.of("config", name);
    }

    public static final Path CONFIG_PATH = safePath("autocart.properties");

    public static final String MODE_CLASSIC = "classic";
    public static final String MODE_ADAPTIVE = "adaptive";
    public static String cartMode = MODE_CLASSIC;

    public static int placementChance = dev.nivorat.arc.internal.ConfigDomain.i(523123992);
    public static boolean legitMode = true;
    public static boolean randomDelay = true;
    public static double maxDistance = dev.nivorat.arc.internal.ConfigDomain.d(4616527384189849805L);
    public static boolean allowSelfCart = false;
    public static boolean allowPitPlacement = true;
    public static int minDelayMs = dev.nivorat.arc.internal.ConfigDomain.i(523124094);
    public static int maxDelayMs = dev.nivorat.arc.internal.ConfigDomain.i(523123996);
    public static boolean useMainhandCart = true;
    public static String cameraMode = "packet";
    public static boolean autoCamera = false;
    public static int cameraSmoothnessMs = dev.nivorat.arc.internal.ConfigDomain.i(523124002);
    public static boolean cameraReturn = false;
    public static int cameraReturnSmoothnessMs = dev.nivorat.arc.internal.ConfigDomain.i(523123992);
    public static int cameraCurve = dev.nivorat.arc.internal.ConfigDomain.i(523124044);
    public static int cameraRandomness = dev.nivorat.arc.internal.ConfigDomain.i(523124079);
    public static boolean cameraMouseGcd = true;
    public static boolean adaptiveAim = true;
    public static boolean autonomousPlacement = true;
    public static boolean debugLogs = false;

    public static final int PRESET_FAST = 0;
    public static final int PRESET_MEDIUM = 1;
    public static final int PRESET_SAFE = 2;
    public static final int PRESET_LEARNED = 3;
    public static final int PRESET_CUSTOM = 4;
    public static int preset = PRESET_MEDIUM;

    public enum CartPreset {
        FAST("Быстрый"),
        MEDIUM("Баланс"),
        SAFE("Безопасный"),
        LEARNED("Обученный"),
        CUSTOM("Свой");

        private final String title;
        CartPreset(String title) { this.title = title; }
        public String getTitle() { return title; }
        @Override public String toString() { return title; }
    }

    private MorrowConfig() { }

    public static void applyPreset(int p) {
        dev.nivorat.arc.internal.ArcPresetEngine.applyPreset(p);
    }

    public static int getMinDelayMs() {
        return minDelayMs;
    }

    public static int getMaxDelayMs() {
        return Math.max(minDelayMs, maxDelayMs);
    }

    public static String getRandomizerRangeDesc() {
        return "§e" + minDelayMs + "-" + maxDelayMs + "мс";
    }

    public static void load() {
        dev.nivorat.arc.internal.ArcPresetEngine.load();
    }

    public static void save() {
    }

    public static void resetDefaults() {
        dev.nivorat.arc.internal.ArcPresetEngine.resetDefaults();
    }
}

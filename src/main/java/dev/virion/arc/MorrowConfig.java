package dev.virion.arc;

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

    public static int placementChance = 100;
    public static boolean legitMode = true;
    public static boolean randomDelay = true;
    public static double maxDistance = 4.4D;
    public static boolean allowSelfCart = false;
    public static boolean allowPitPlacement = true;
    public static int minDelayMs = 70;
    public static int maxDelayMs = 110;
    public static boolean useMainhandCart = true;
    public static String cameraMode = "off";
    public static boolean autoCamera = false;
    public static int cameraSmoothnessMs = 140;
    public static boolean cameraReturn = true;
    public static int cameraReturnSmoothnessMs = 120;
    public static int cameraCurve = 40;
    public static int cameraRandomness = 35;
    public static boolean cameraMouseGcd = true;

    public static final int PRESET_FAST = 0;
    public static final int PRESET_MEDIUM = 1;
    public static final int PRESET_SAFE = 2;
    public static int preset = PRESET_MEDIUM;

    public enum CartPreset {
        FAST("Быстрый"),
        MEDIUM("Баланс"),
        SAFE("Безопасный");

        private final String title;
        CartPreset(String title) { this.title = title; }
        public String getTitle() { return title; }
        @Override public String toString() { return title; }
    }

    private MorrowConfig() { }

    public static void applyPreset(int p) {
        preset = clamp(p, 0, 2);
        switch (preset) {
            case PRESET_FAST -> {
                placementChance = 100;
                legitMode = false;
                maxDistance = 4.5D;
                allowPitPlacement = true;
                randomDelay = true;
                minDelayMs = 40;
                maxDelayMs = 60;
                cameraMode = "packet";
                autoCamera = false;
                cameraSmoothnessMs = 80;
                cameraReturn = true;
                cameraReturnSmoothnessMs = 80;
                cameraCurve = 20;
                cameraRandomness = 20;
                cameraMouseGcd = true;
            }
            case PRESET_MEDIUM -> {
                placementChance = 100;
                legitMode = true;
                maxDistance = 4.4D;
                allowPitPlacement = true;
                randomDelay = true;
                minDelayMs = 70;
                maxDelayMs = 110;
                cameraMode = "auto";
                autoCamera = true;
                cameraSmoothnessMs = 140;
                cameraReturn = true;
                cameraReturnSmoothnessMs = 120;
                cameraCurve = 40;
                cameraRandomness = 35;
                cameraMouseGcd = true;
            }
            case PRESET_SAFE -> {
                placementChance = 100;
                legitMode = true;
                maxDistance = 4.2D;
                allowPitPlacement = true;
                randomDelay = true;
                minDelayMs = 120;
                maxDelayMs = 180;
                cameraMode = "off";
                autoCamera = false;
                cameraSmoothnessMs = 180;
                cameraReturn = true;
                cameraReturnSmoothnessMs = 160;
                cameraCurve = 50;
                cameraRandomness = 50;
                cameraMouseGcd = true;
            }
        }
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
        if (!Files.exists(CONFIG_PATH)) {
            return;
        }
        try (InputStream input = Files.newInputStream(CONFIG_PATH)) {
            Properties properties = new Properties();
            properties.load(input);
            placementChance = clamp(Integer.parseInt(properties.getProperty("placementChance", "100")), 0, 100);
            legitMode = Boolean.parseBoolean(properties.getProperty("legitMode", "true"));
            randomDelay = Boolean.parseBoolean(properties.getProperty("randomDelay", "true"));
            preset = clamp(Integer.parseInt(properties.getProperty("preset", "1")), 0, 2);
            allowPitPlacement = Boolean.parseBoolean(properties.getProperty("allowPitPlacement", "true"));
            maxDistance = clampDouble(Double.parseDouble(properties.getProperty("maxDistance", "4.4")), 1.5D, 4.5D);
            allowSelfCart = Boolean.parseBoolean(properties.getProperty("allowSelfCart", "false"));
            minDelayMs = clamp(Integer.parseInt(properties.getProperty("minDelayMs", "70")), 10, 200);
            maxDelayMs = clamp(Integer.parseInt(properties.getProperty("maxDelayMs", "110")), 10, 300);
            useMainhandCart = Boolean.parseBoolean(properties.getProperty("useMainhandCart", "true"));
            cameraMode = properties.getProperty("cameraMode", "off");
            autoCamera = Boolean.parseBoolean(properties.getProperty("autoCamera", "false"));
            cameraSmoothnessMs = clamp(Integer.parseInt(properties.getProperty("cameraSmoothnessMs", "140")), 50, 300);
            cameraReturn = Boolean.parseBoolean(properties.getProperty("cameraReturn", "true"));
            cameraReturnSmoothnessMs = clamp(Integer.parseInt(properties.getProperty("cameraReturnSmoothnessMs", "120")), 50, 300);
            cameraCurve = clamp(Integer.parseInt(properties.getProperty("cameraCurve", "40")), 0, 100);
            cameraRandomness = clamp(Integer.parseInt(properties.getProperty("cameraRandomness", "35")), 0, 100);
            cameraMouseGcd = Boolean.parseBoolean(properties.getProperty("cameraMouseGcd", "true"));
            if (maxDelayMs < minDelayMs) maxDelayMs = minDelayMs;
        } catch (Exception ignored) {
            resetDefaults();
        }
    }

    public static void save() {
    }

    public static void resetDefaults() {
        placementChance = 100;
        legitMode = true;
        randomDelay = true;
        preset = PRESET_MEDIUM;
        allowPitPlacement = true;
        maxDistance = 4.4D;
        allowSelfCart = false;
        minDelayMs = 70;
        maxDelayMs = 110;
        useMainhandCart = true;
        cameraMode = "off";
        autoCamera = false;
        cameraSmoothnessMs = 140;
        cameraReturn = true;
        cameraReturnSmoothnessMs = 120;
        cameraCurve = 40;
        cameraRandomness = 35;
        cameraMouseGcd = true;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

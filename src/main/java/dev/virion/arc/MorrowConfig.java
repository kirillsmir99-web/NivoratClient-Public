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
    public static int minDelayMs = 50;
    public static int maxDelayMs = 80;
    public static boolean useMainhandCart = true;
    public static String cameraMode = "auto";
    public static boolean autoCamera = true;
    public static int cameraSmoothnessMs = 110;
    public static boolean cameraReturn = true;
    public static int cameraReturnSmoothnessMs = 100;
    public static int cameraCurve = 40;
    public static int cameraRandomness = 35;
    public static boolean cameraMouseGcd = true;
    public static boolean neuralAim = true;
    public static boolean autonomousPlacement = true;

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
        preset = clamp(p, 0, 4);
        switch (preset) {
            case PRESET_FAST -> {
                placementChance = 100;
                legitMode = false;
                maxDistance = 4.5D;
                allowPitPlacement = true;
                randomDelay = true;
                minDelayMs = 35;
                maxDelayMs = 50;
                cameraMode = "packet";
                autoCamera = false;
                cameraSmoothnessMs = 55;
                cameraReturn = true;
                cameraReturnSmoothnessMs = 55;
                cameraCurve = 20;
                cameraRandomness = 20;
                cameraMouseGcd = true;
                neuralAim = true;
                autonomousPlacement = true;
            }
            case PRESET_MEDIUM -> {
                placementChance = 100;
                legitMode = true;
                maxDistance = 4.4D;
                allowPitPlacement = true;
                randomDelay = true;
                minDelayMs = 50;
                maxDelayMs = 80;
                cameraMode = "auto";
                autoCamera = true;
                cameraSmoothnessMs = 110;
                cameraReturn = true;
                cameraReturnSmoothnessMs = 100;
                cameraCurve = 40;
                cameraRandomness = 35;
                cameraMouseGcd = true;
                neuralAim = true;
                autonomousPlacement = true;
            }
            case PRESET_SAFE -> {
                placementChance = 100;
                legitMode = true;
                maxDistance = 4.2D;
                allowPitPlacement = true;
                randomDelay = true;
                minDelayMs = 70;
                maxDelayMs = 100;
                cameraMode = "auto";
                autoCamera = true;
                cameraSmoothnessMs = 160;
                cameraReturn = true;
                cameraReturnSmoothnessMs = 150;
                cameraCurve = 45;
                cameraRandomness = 40;
                cameraMouseGcd = true;
                neuralAim = true;
                autonomousPlacement = true;
            }
            case PRESET_LEARNED -> {
                ArcNeuralMotorProfile prof = ArcNeuralMotorProfile.getInstance();
                placementChance = 100;
                legitMode = true;
                maxDistance = 4.4D;
                allowPitPlacement = true;
                randomDelay = true;
                minDelayMs = prof.getLearnedMinDelayMs();
                maxDelayMs = prof.getLearnedMaxDelayMs();
                cameraMode = "auto";
                autoCamera = true;
                cameraSmoothnessMs = prof.getLearnedCameraSmoothness();
                cameraReturn = true;
                cameraReturnSmoothnessMs = Math.max(35, prof.getLearnedCameraSmoothness() - 10);
                cameraCurve = Math.round(prof.getCurvatureBias() * 100.0f);
                cameraRandomness = Math.round(prof.getTremorVolatility() * 500.0f);
                cameraMouseGcd = true;
                neuralAim = true;
                autonomousPlacement = true;
            }
            case PRESET_CUSTOM -> {
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
            preset = clamp(Integer.parseInt(properties.getProperty("preset", "1")), 0, 4);
            allowPitPlacement = Boolean.parseBoolean(properties.getProperty("allowPitPlacement", "true"));
            maxDistance = clampDouble(Double.parseDouble(properties.getProperty("maxDistance", "4.4")), 1.5D, 4.5D);
            allowSelfCart = Boolean.parseBoolean(properties.getProperty("allowSelfCart", "false"));
            minDelayMs = clamp(Integer.parseInt(properties.getProperty("minDelayMs", "50")), 10, 200);
            maxDelayMs = clamp(Integer.parseInt(properties.getProperty("maxDelayMs", "80")), 10, 300);
            useMainhandCart = Boolean.parseBoolean(properties.getProperty("useMainhandCart", "true"));
            cameraMode = properties.getProperty("cameraMode", "auto");
            autoCamera = Boolean.parseBoolean(properties.getProperty("autoCamera", "true"));
            cameraSmoothnessMs = clamp(Integer.parseInt(properties.getProperty("cameraSmoothnessMs", "110")), 35, 300);
            cameraReturn = Boolean.parseBoolean(properties.getProperty("cameraReturn", "true"));
            cameraReturnSmoothnessMs = clamp(Integer.parseInt(properties.getProperty("cameraReturnSmoothnessMs", "100")), 35, 300);
            cameraCurve = clamp(Integer.parseInt(properties.getProperty("cameraCurve", "40")), 0, 100);
            cameraRandomness = clamp(Integer.parseInt(properties.getProperty("cameraRandomness", "35")), 0, 100);
            cameraMouseGcd = Boolean.parseBoolean(properties.getProperty("cameraMouseGcd", "true"));
            neuralAim = Boolean.parseBoolean(properties.getProperty("neuralAim", "true"));
            autonomousPlacement = Boolean.parseBoolean(properties.getProperty("autonomousPlacement", "true"));
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
        minDelayMs = 50;
        maxDelayMs = 80;
        useMainhandCart = true;
        cameraMode = "auto";
        autoCamera = true;
        cameraSmoothnessMs = 110;
        cameraReturn = true;
        cameraReturnSmoothnessMs = 100;
        cameraCurve = 40;
        cameraRandomness = 35;
        cameraMouseGcd = true;
        neuralAim = true;
        autonomousPlacement = true;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

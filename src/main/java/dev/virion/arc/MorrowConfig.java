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

    // Presets: 0 = Fast, 1 = Balance (Medium), 2 = Safe
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
            }
            case PRESET_MEDIUM -> {
                placementChance = 100;
                legitMode = true;
                maxDistance = 4.4D;
                allowPitPlacement = true;
                randomDelay = true;
                minDelayMs = 70;
                maxDelayMs = 110;
            }
            case PRESET_SAFE -> {
                placementChance = 100;
                legitMode = true;
                maxDistance = 4.2D;
                allowPitPlacement = true;
                randomDelay = true;
                minDelayMs = 120;
                maxDelayMs = 180;
            }
        }
        save();
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
            save();
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
            if (maxDelayMs < minDelayMs) maxDelayMs = minDelayMs;
        } catch (Exception ignored) {
            resetDefaults();
        }
    }

    public static void save() {
        try {
            if (CONFIG_PATH.getParent() != null) Files.createDirectories(CONFIG_PATH.getParent());
            Properties properties = new Properties();
            properties.setProperty("placementChance", String.valueOf(clamp(placementChance, 0, 100)));
            properties.setProperty("legitMode", String.valueOf(legitMode));
            properties.setProperty("randomDelay", String.valueOf(randomDelay));
            properties.setProperty("preset", String.valueOf(preset));
            properties.setProperty("allowPitPlacement", String.valueOf(allowPitPlacement));
            properties.setProperty("maxDistance", String.valueOf(maxDistance));
            properties.setProperty("allowSelfCart", String.valueOf(allowSelfCart));
            properties.setProperty("minDelayMs", String.valueOf(minDelayMs));
            properties.setProperty("maxDelayMs", String.valueOf(maxDelayMs));
            try (OutputStream output = Files.newOutputStream(CONFIG_PATH)) {
                properties.store(output, "AutoCart settings");
            }
        } catch (Exception ignored) { }
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
        save();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

package dev.carthud;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public final class CartHudConfig {
    public static boolean enabled = true;
    public static int customX = -1;
    public static int customY = -1;

    private static final File CONFIG_FILE = new File("config/cart_hud.properties");
    private static final File STORAGE_CONFIG_FILE = new File("config/storage_tweaks.properties");
    private static boolean saving = false;

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            loadFromStorageTweaks();
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(CONFIG_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                switch (key) {
                    case "enabled" -> enabled = Boolean.parseBoolean(value);
                    case "custom_x", "hud_x" -> customX = Integer.parseInt(value);
                    case "custom_y", "hud_y" -> customY = Integer.parseInt(value);
                }
            }
        } catch (Exception ignored) {
        }

        if (customX < 0 && STORAGE_CONFIG_FILE.exists()) {
            loadFromStorageTweaks();
        }
    }

    private static void loadFromStorageTweaks() {
        if (!STORAGE_CONFIG_FILE.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(STORAGE_CONFIG_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                if ("hud_x".equals(key) || "custom_x".equals(key)) {
                    customX = Integer.parseInt(value);
                } else if ("hud_y".equals(key) || "custom_y".equals(key)) {
                    customY = Integer.parseInt(value);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        if (saving) {
            return;
        }
        saving = true;
        try {
            File dir = CONFIG_FILE.getParentFile();
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                writer.write("enabled=" + enabled + "\n");
                writer.write("hud_x=" + customX + "\n");
                writer.write("hud_y=" + customY + "\n");
            } catch (IOException ignored) {
            }

            dev.storage.RefillConfig.customX = customX;
            dev.storage.RefillConfig.customY = customY;
        } finally {
            saving = false;
        }
    }
}

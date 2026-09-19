package dev.storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public final class RefillConfig {
    public static boolean enabled = true;
    public static int refillDelayTicks = 2;
    public static int chance = 100;
    public static boolean legitMode = true;
    public static boolean autoClose = true;
    public static boolean showHud = true;
    public static boolean randomDelay = true;
    public static int customX = -1;
    public static int customY = -1;

    private static final File CONFIG_FILE = new File("config/storage_tweaks.properties");
    private static final File CARTHUD_CONFIG_FILE = new File("config/cart_hud.properties");
    private static boolean saving = false;

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            loadFromCartHud();
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
                    case "refill_delay_ticks" -> refillDelayTicks = Integer.parseInt(value);
                    case "chance" -> chance = Integer.parseInt(value);
                    case "legit_mode" -> legitMode = Boolean.parseBoolean(value);
                    case "auto_close" -> autoClose = Boolean.parseBoolean(value);
                    case "show_hud" -> showHud = Boolean.parseBoolean(value);
                    case "random_delay" -> randomDelay = Boolean.parseBoolean(value);
                    case "custom_x", "hud_x" -> customX = Integer.parseInt(value);
                    case "custom_y", "hud_y" -> customY = Integer.parseInt(value);
                }
            }
        } catch (Exception ignored) {
        }

        if (customX < 0 && CARTHUD_CONFIG_FILE.exists()) {
            loadFromCartHud();
        }
    }

    private static void loadFromCartHud() {
        if (!CARTHUD_CONFIG_FILE.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(CARTHUD_CONFIG_FILE))) {
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
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
            return;
        }
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
                writer.write("refill_delay_ticks=" + refillDelayTicks + "\n");
                writer.write("chance=" + chance + "\n");
                writer.write("legit_mode=" + legitMode + "\n");
                writer.write("auto_close=" + autoClose + "\n");
                writer.write("show_hud=" + showHud + "\n");
                writer.write("random_delay=" + randomDelay + "\n");
                writer.write("hud_x=" + customX + "\n");
                writer.write("hud_y=" + customY + "\n");
            } catch (IOException ignored) {
            }

            dev.carthud.CartHudConfig.customX = customX;
            dev.carthud.CartHudConfig.customY = customY;
        } finally {
            saving = false;
        }
    }
}

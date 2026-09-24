package dev.luminance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public final class AnchorConfig {
    public static boolean enabled = true;
    public static String mode = "smart";
    public static boolean autoExplode = false;
    public static boolean autoReturn = true;
    public static int chargeDelayTicks = 1;
    public static int explodeDelayTicks = 1;
    public static int chance = 90;
    public static boolean legitMode = true;
    public static int targetCharges = 1;
    public static String preset = "BALANCED";
    public static String presetDouble = "fast";
    public static double doubleDelayTicks = 1.0;
    public static boolean doubleAutoExplode = true;
    public static boolean doubleChain = true;

    private static final File CONFIG_FILE = new File("config/luminance_tweaks.properties");

    public static void applyPreset(String p) {
        preset = p;
        switch (p) {
            case "FAST" -> {
                chargeDelayTicks = 0;
                explodeDelayTicks = 0;
                chance = 100;
            }
            case "MEDIUM" -> {
                chargeDelayTicks = 1;
                explodeDelayTicks = 1;
                chance = 95;
            }
            case "BALANCED" -> {
                chargeDelayTicks = 1;
                explodeDelayTicks = 1;
                chance = 90;
            }
            case "SAFE" -> {
                chargeDelayTicks = 2;
                explodeDelayTicks = 2;
                chance = 85;
            }
        }
        save();
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
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
                    case "mode" -> mode = value;
                    case "auto_explode" -> autoExplode = Boolean.parseBoolean(value);
                    case "auto_return" -> autoReturn = Boolean.parseBoolean(value);
                    case "charge_delay_ticks" -> chargeDelayTicks = Integer.parseInt(value);
                    case "explode_delay_ticks" -> explodeDelayTicks = Integer.parseInt(value);
                    case "chance" -> chance = Integer.parseInt(value);
                    case "legit_mode" -> legitMode = Boolean.parseBoolean(value);
                    case "target_charges" -> targetCharges = Math.max(1, Math.min(4, Integer.parseInt(value)));
                    case "preset" -> preset = value;
                    case "preset_double" -> presetDouble = value;
                    case "double_delay_ticks" -> doubleDelayTicks = Double.parseDouble(value);
                    case "double_auto_explode" -> doubleAutoExplode = Boolean.parseBoolean(value);
                    case "double_chain" -> doubleChain = Boolean.parseBoolean(value);
                }
            }
        } catch (Exception e) {
            save();
        }
    }

    public static void save() {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
            return;
        }
        try {
            File dir = CONFIG_FILE.getParentFile();
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                writer.write("enabled=" + enabled + "\n");
                writer.write("mode=" + mode + "\n");
                writer.write("auto_explode=" + autoExplode + "\n");
                writer.write("auto_return=" + autoReturn + "\n");
                writer.write("charge_delay_ticks=" + chargeDelayTicks + "\n");
                writer.write("explode_delay_ticks=" + explodeDelayTicks + "\n");
                writer.write("chance=" + chance + "\n");
                writer.write("legit_mode=" + legitMode + "\n");
                writer.write("target_charges=" + targetCharges + "\n");
                writer.write("preset=" + preset + "\n");
                writer.write("preset_double=" + presetDouble + "\n");
                writer.write("double_delay_ticks=" + doubleDelayTicks + "\n");
                writer.write("double_auto_explode=" + doubleAutoExplode + "\n");
                writer.write("double_chain=" + doubleChain + "\n");
            }
        } catch (IOException ignored) {
        }
    }
}

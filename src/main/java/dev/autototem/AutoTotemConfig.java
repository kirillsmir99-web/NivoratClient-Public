package dev.autototem;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class AutoTotemConfig {
    private static Path safePath(String name) {
        try {
            var l = net.fabricmc.loader.api.FabricLoader.getInstance();
            if (l != null && l.getConfigDir() != null) return l.getConfigDir().resolve(name);
        } catch (Throwable ignored) {}
        return Path.of("config", name);
    }

    private static final Path CONFIG_PATH = safePath("autototem.properties");

    public static boolean enabled = true;
    public static double triggerHearts = 3.0;
    public static double restoreHearts = 6.0;
    public static double mainhandTriggerHearts = 3.0;
    public static double mainhandRestoreHearts = 6.0;
    public static double offhandTriggerHearts = 2.0;
    public static double offhandRestoreHearts = 5.0;
    public static double crystalTriggerHearts = 3.0;
    public static double crystalRestoreHearts = 6.0;
    public static int chance = 100;
    public static boolean returnItem = true;
    public static boolean returnOnPop = true;
    public static int mode = 1;
    public static boolean autoRefill = true;
    public static int refillSlot = -1;

    private AutoTotemConfig() { }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
            Properties props = new Properties();
            props.load(in);
            triggerHearts = Math.max(0.5, Math.min(10.0, Double.parseDouble(props.getProperty("triggerHearts", "3.0"))));
            restoreHearts = Math.max(0.5, Math.min(20.0, Double.parseDouble(props.getProperty("restoreHearts", "6.0"))));
            mainhandTriggerHearts = Math.max(0.5, Math.min(10.0, Double.parseDouble(props.getProperty("mainhandTriggerHearts", "3.0"))));
            mainhandRestoreHearts = Math.max(0.5, Math.min(20.0, Double.parseDouble(props.getProperty("mainhandRestoreHearts", "6.0"))));
            offhandTriggerHearts = Math.max(0.5, Math.min(10.0, Double.parseDouble(props.getProperty("offhandTriggerHearts", "2.0"))));
            offhandRestoreHearts = Math.max(0.5, Math.min(20.0, Double.parseDouble(props.getProperty("offhandRestoreHearts", "5.0"))));
            crystalTriggerHearts = Math.max(0.5, Math.min(10.0, Double.parseDouble(props.getProperty("crystalTriggerHearts", "3.0"))));
            crystalRestoreHearts = Math.max(0.5, Math.min(20.0, Double.parseDouble(props.getProperty("crystalRestoreHearts", "6.0"))));
            chance = Math.max(10, Math.min(100, Integer.parseInt(props.getProperty("chance", "100"))));
            returnItem = Boolean.parseBoolean(props.getProperty("returnItem", "true"));
            returnOnPop = Boolean.parseBoolean(props.getProperty("returnOnPop", "true"));
            mode = Integer.parseInt(props.getProperty("mode", "1"));
            autoRefill = Boolean.parseBoolean(props.getProperty("autoRefill", "true"));
            refillSlot = Integer.parseInt(props.getProperty("refillSlot", "-1"));
        } catch (Exception ignored) {
            triggerHearts = 3.0;
            restoreHearts = 6.0;
            mainhandTriggerHearts = 3.0;
            mainhandRestoreHearts = 6.0;
            offhandTriggerHearts = 2.0;
            offhandRestoreHearts = 5.0;
            crystalTriggerHearts = 3.0;
            crystalRestoreHearts = 6.0;
            chance = 100;
            returnItem = true;
            returnOnPop = true;
            mode = 1;
            autoRefill = true;
            refillSlot = -1;
        }
    }

    public static void save() {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
            return;
        }
        try {
            if (CONFIG_PATH.getParent() != null && !Files.exists(CONFIG_PATH.getParent())) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }
            Properties props = new Properties();
            props.setProperty("triggerHearts", String.valueOf(triggerHearts));
            props.setProperty("restoreHearts", String.valueOf(restoreHearts));
            props.setProperty("mainhandTriggerHearts", String.valueOf(mainhandTriggerHearts));
            props.setProperty("mainhandRestoreHearts", String.valueOf(mainhandRestoreHearts));
            props.setProperty("offhandTriggerHearts", String.valueOf(offhandTriggerHearts));
            props.setProperty("offhandRestoreHearts", String.valueOf(offhandRestoreHearts));
            props.setProperty("crystalTriggerHearts", String.valueOf(crystalTriggerHearts));
            props.setProperty("crystalRestoreHearts", String.valueOf(crystalRestoreHearts));
            props.setProperty("chance", String.valueOf(chance));
            props.setProperty("returnItem", String.valueOf(returnItem));
            props.setProperty("returnOnPop", String.valueOf(returnOnPop));
            props.setProperty("mode", String.valueOf(mode));
            props.setProperty("autoRefill", String.valueOf(autoRefill));
            props.setProperty("refillSlot", String.valueOf(refillSlot));
            try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
                props.store(out, "AutoTotem configuration");
            }
        } catch (Exception ignored) {
        }
    }
}

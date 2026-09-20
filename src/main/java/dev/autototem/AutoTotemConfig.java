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
    public static int triggerHearts = 3;
    public static int restoreHearts = 6;
    public static int mainhandTriggerHearts = 3;
    public static int mainhandRestoreHearts = 6;
    public static int offhandTriggerHearts = 2;
    public static int offhandRestoreHearts = 5;
    public static int crystalTriggerHearts = 3;
    public static int crystalRestoreHearts = 6;
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
            triggerHearts = Math.max(1, Math.min(9, Integer.parseInt(props.getProperty("triggerHearts", "3"))));
            restoreHearts = Math.max(1, Math.min(20, Integer.parseInt(props.getProperty("restoreHearts", "6"))));
            mainhandTriggerHearts = Math.max(1, Math.min(9, Integer.parseInt(props.getProperty("mainhandTriggerHearts", "3"))));
            mainhandRestoreHearts = Math.max(1, Math.min(20, Integer.parseInt(props.getProperty("mainhandRestoreHearts", "6"))));
            offhandTriggerHearts = Math.max(1, Math.min(9, Integer.parseInt(props.getProperty("offhandTriggerHearts", "2"))));
            offhandRestoreHearts = Math.max(1, Math.min(20, Integer.parseInt(props.getProperty("offhandRestoreHearts", "5"))));
            crystalTriggerHearts = Math.max(1, Math.min(9, Integer.parseInt(props.getProperty("crystalTriggerHearts", "3"))));
            crystalRestoreHearts = Math.max(1, Math.min(20, Integer.parseInt(props.getProperty("crystalRestoreHearts", "6"))));
            chance = Math.max(10, Math.min(100, Integer.parseInt(props.getProperty("chance", "100"))));
            returnItem = Boolean.parseBoolean(props.getProperty("returnItem", "true"));
            returnOnPop = Boolean.parseBoolean(props.getProperty("returnOnPop", "true"));
            mode = Integer.parseInt(props.getProperty("mode", "1"));
            autoRefill = Boolean.parseBoolean(props.getProperty("autoRefill", "true"));
            refillSlot = Integer.parseInt(props.getProperty("refillSlot", "-1"));
        } catch (Exception ignored) {
            triggerHearts = 3;
            restoreHearts = 6;
            mainhandTriggerHearts = 3;
            mainhandRestoreHearts = 6;
            offhandTriggerHearts = 2;
            offhandRestoreHearts = 5;
            crystalTriggerHearts = 3;
            crystalRestoreHearts = 6;
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

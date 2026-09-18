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
    public static int chance = 100;
    public static boolean returnItem = true;
    public static boolean returnOnPop = true;
    public static int mode = 1;

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
            restoreHearts = Math.max(4, Math.min(10, Integer.parseInt(props.getProperty("restoreHearts", "6"))));
            if (restoreHearts <= triggerHearts) {
                restoreHearts = triggerHearts + 1;
            }
            chance = Math.max(10, Math.min(100, Integer.parseInt(props.getProperty("chance", "100"))));
            returnItem = Boolean.parseBoolean(props.getProperty("returnItem", "true"));
            returnOnPop = Boolean.parseBoolean(props.getProperty("returnOnPop", "true"));
            mode = Integer.parseInt(props.getProperty("mode", "1"));
        } catch (Exception ignored) {
            triggerHearts = 3;
            restoreHearts = 6;
            chance = 100;
            returnItem = true;
            returnOnPop = true;
            mode = 1;
        }
    }

    public static void save() {
        try {
            if (CONFIG_PATH.getParent() != null && !Files.exists(CONFIG_PATH.getParent())) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }
            Properties props = new Properties();
            props.setProperty("triggerHearts", String.valueOf(triggerHearts));
            props.setProperty("restoreHearts", String.valueOf(restoreHearts));
            props.setProperty("chance", String.valueOf(chance));
            props.setProperty("returnItem", String.valueOf(returnItem));
            props.setProperty("returnOnPop", String.valueOf(returnOnPop));
            props.setProperty("mode", String.valueOf(mode));
            try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
                props.store(out, "AutoTotem configuration");
            }
        } catch (Exception ignored) {
        }
    }
}

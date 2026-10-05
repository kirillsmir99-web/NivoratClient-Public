package dev.buffer;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class BufferPipelineConfig {
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
    public static boolean countAbsorption = false;
    public static int chance = 100;
    public static boolean returnItem = true;
    public static boolean returnOnPop = true;
    public static int mode = 2;
    public static boolean autoRefill = true;
    public static int refillSlot = -1;

    public static double swapBackDelay = 3.0;
    public static boolean alwaysOffhand = true;
    public static boolean ignoreWhenUsing = true;
    public static boolean predictiveDamage = true;
    public static boolean predictCrystals = true;
    public static boolean predictFall = true;
    public static boolean predictMace = true;
    public static boolean predictTrident = true;
    public static boolean lowTotemNotify = true;
    public static String inventorySource = "rage";
    public static double fovFilterDegrees = 110.0;
    public static int livenessMinTicks = 3;

    private BufferPipelineConfig() { }

    public static void validateHysteresis() {
        if (restoreHearts > 0.0 && restoreHearts <= triggerHearts) {
            restoreHearts = triggerHearts + 1.0;
        }
        if (mainhandRestoreHearts > 0.0 && mainhandRestoreHearts <= mainhandTriggerHearts) {
            mainhandRestoreHearts = mainhandTriggerHearts + 1.0;
        }
        if (offhandRestoreHearts > 0.0 && offhandRestoreHearts <= offhandTriggerHearts) {
            offhandRestoreHearts = offhandTriggerHearts + 1.0;
        }
        if (crystalRestoreHearts > 0.0 && crystalRestoreHearts <= crystalTriggerHearts) {
            crystalRestoreHearts = crystalTriggerHearts + 1.0;
        }
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            return;
        }
        try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
            Properties props = new Properties();
            props.load(in);
            triggerHearts = Math.max(0.5, Math.min(10.0, Double.parseDouble(props.getProperty("triggerHearts", "3.0"))));
            restoreHearts = Math.max(0.0, Math.min(20.0, Double.parseDouble(props.getProperty("restoreHearts", "6.0"))));
            mainhandTriggerHearts = Math.max(0.5, Math.min(10.0, Double.parseDouble(props.getProperty("mainhandTriggerHearts", "3.0"))));
            mainhandRestoreHearts = Math.max(0.0, Math.min(20.0, Double.parseDouble(props.getProperty("mainhandRestoreHearts", "6.0"))));
            offhandTriggerHearts = Math.max(0.5, Math.min(10.0, Double.parseDouble(props.getProperty("offhandTriggerHearts", "2.0"))));
            offhandRestoreHearts = Math.max(0.0, Math.min(20.0, Double.parseDouble(props.getProperty("offhandRestoreHearts", "5.0"))));
            crystalTriggerHearts = Math.max(0.5, Math.min(10.0, Double.parseDouble(props.getProperty("crystalTriggerHearts", "3.0"))));
            crystalRestoreHearts = Math.max(0.0, Math.min(20.0, Double.parseDouble(props.getProperty("crystalRestoreHearts", "6.0"))));
            countAbsorption = Boolean.parseBoolean(props.getProperty("countAbsorption", "false"));
            chance = Math.max(10, Math.min(100, Integer.parseInt(props.getProperty("chance", "100"))));
            returnItem = Boolean.parseBoolean(props.getProperty("returnItem", "true"));
            returnOnPop = Boolean.parseBoolean(props.getProperty("returnOnPop", "true"));
            mode = Integer.parseInt(props.getProperty("mode", "2"));
            autoRefill = Boolean.parseBoolean(props.getProperty("autoRefill", "true"));
            refillSlot = Integer.parseInt(props.getProperty("refillSlot", "-1"));

            swapBackDelay = Double.parseDouble(props.getProperty("swapBackDelay", "3.0"));
            alwaysOffhand = Boolean.parseBoolean(props.getProperty("alwaysOffhand", "true"));
            ignoreWhenUsing = Boolean.parseBoolean(props.getProperty("ignoreWhenUsing", "true"));
            predictiveDamage = Boolean.parseBoolean(props.getProperty("predictiveDamage", "true"));
            predictCrystals = Boolean.parseBoolean(props.getProperty("predictCrystals", "true"));
            predictFall = Boolean.parseBoolean(props.getProperty("predictFall", "true"));
            predictMace = Boolean.parseBoolean(props.getProperty("predictMace", "true"));
            predictTrident = Boolean.parseBoolean(props.getProperty("predictTrident", "true"));
            lowTotemNotify = Boolean.parseBoolean(props.getProperty("lowTotemNotify", "true"));
            inventorySource = props.getProperty("inventorySource", "rage");
            fovFilterDegrees = Math.max(30.0, Math.min(180.0, Double.parseDouble(props.getProperty("fovFilterDegrees", "110.0"))));
            livenessMinTicks = Math.max(0, Math.min(20, Integer.parseInt(props.getProperty("livenessMinTicks", "3"))));

            validateHysteresis();
        } catch (Exception ignored) {
            triggerHearts = 3.0;
            restoreHearts = 6.0;
            mainhandTriggerHearts = 3.0;
            mainhandRestoreHearts = 6.0;
            offhandTriggerHearts = 2.0;
            offhandRestoreHearts = 5.0;
            crystalTriggerHearts = 3.0;
            crystalRestoreHearts = 6.0;
            countAbsorption = false;
            chance = 100;
            returnItem = true;
            returnOnPop = true;
            mode = 2;
            autoRefill = true;
            refillSlot = -1;
            swapBackDelay = 3.0;
            alwaysOffhand = true;
            ignoreWhenUsing = true;
            predictiveDamage = true;
            predictCrystals = true;
            predictFall = true;
            predictMace = true;
            predictTrident = true;
            lowTotemNotify = true;
            inventorySource = "rage";
            fovFilterDegrees = 110.0;
            livenessMinTicks = 3;
        }
    }

    public static void save() {
        validateHysteresis();
        try {
            if (CONFIG_PATH.getParent() != null && !Files.exists(CONFIG_PATH.getParent())) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }
            try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
                Properties props = new Properties();
                props.setProperty("triggerHearts", String.valueOf(triggerHearts));
                props.setProperty("restoreHearts", String.valueOf(restoreHearts));
                props.setProperty("mainhandTriggerHearts", String.valueOf(mainhandTriggerHearts));
                props.setProperty("mainhandRestoreHearts", String.valueOf(mainhandRestoreHearts));
                props.setProperty("offhandTriggerHearts", String.valueOf(offhandTriggerHearts));
                props.setProperty("offhandRestoreHearts", String.valueOf(offhandRestoreHearts));
                props.setProperty("crystalTriggerHearts", String.valueOf(crystalTriggerHearts));
                props.setProperty("crystalRestoreHearts", String.valueOf(crystalRestoreHearts));
                props.setProperty("countAbsorption", String.valueOf(countAbsorption));
                props.setProperty("chance", String.valueOf(chance));
                props.setProperty("returnItem", String.valueOf(returnItem));
                props.setProperty("returnOnPop", String.valueOf(returnOnPop));
                props.setProperty("mode", String.valueOf(mode));
                props.setProperty("autoRefill", String.valueOf(autoRefill));
                props.setProperty("refillSlot", String.valueOf(refillSlot));
                props.setProperty("swapBackDelay", String.valueOf(swapBackDelay));
                props.setProperty("alwaysOffhand", String.valueOf(alwaysOffhand));
                props.setProperty("ignoreWhenUsing", String.valueOf(ignoreWhenUsing));
                props.setProperty("predictiveDamage", String.valueOf(predictiveDamage));
                props.setProperty("predictCrystals", String.valueOf(predictCrystals));
                props.setProperty("predictFall", String.valueOf(predictFall));
                props.setProperty("predictMace", String.valueOf(predictMace));
                props.setProperty("predictTrident", String.valueOf(predictTrident));
                props.setProperty("lowTotemNotify", String.valueOf(lowTotemNotify));
                props.setProperty("inventorySource", inventorySource);
                props.setProperty("fovFilterDegrees", String.valueOf(fovFilterDegrees));
                props.setProperty("livenessMinTicks", String.valueOf(livenessMinTicks));
                props.store(out, null);
            }
        } catch (Exception ignored) {}
    }
}

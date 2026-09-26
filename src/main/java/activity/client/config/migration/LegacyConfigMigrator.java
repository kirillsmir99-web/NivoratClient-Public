package activity.client.config.migration;

import activity.client.ActivityClient;
import activity.client.config.ActivityConfig;
import activity.client.config.NivoratConfigManager;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.keybind.Keybind;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class LegacyConfigMigrator {

    public static final String MIGRATION_MARKER_FILE = ".nivorat_legacy_migrated";
    public static final int CURRENT_MIGRATION_VERSION = 1;

    public static final String AUTO_MACE_PRIMARY = "redstone_optimizer.properties";
    public static final String AUTO_MACE_LEGACY = "impact_tweaks.properties";
    public static final String AUTO_MACE_PROP = "automace.properties";
    public static final String AUTO_MACE_JSON = "automace.json";

    public static final String AUTO_SPEAR = "momentum_tweaks.properties";
    public static final String AUTO_SPEAR_SECONDARY = "autospear.properties";
    public static final String AUTO_SPEAR_JSON = "autospear.json";

    public static final String AUTO_SHIELDBREAKER = "autoshieldbreaker.properties";
    public static final String AUTO_SHIELDBREAKER_JSON = "autoshieldbreaker.json";

    public static final String AUTO_STUN_SLIME = "autostunslime.properties";
    public static final String AUTO_STUN_SLIME_JSON = "autostunslime.json";
    public static final String AUTO_STUN_SLAM = "autostunslam.properties";
    public static final String AUTO_STUN_SLAM_JSON = "autostunslam.json";

    public static final String AUTO_TOTEM = "autototem.properties";
    public static final String AUTO_TOTEM_JSON = "autototem.json";

    public static final String AUTO_CART = "autocart.properties";
    public static final String AUTO_CART_JSON = "autocart.json";
    public static final String AUTO_CART_SECONDARY = "morrow.properties";
    public static final String AUTO_CART_SECONDARY_JSON = "morrow.json";

    public static final String AUTO_ANCHOR_PRIMARY = "luminance_tweaks.properties";
    public static final String AUTO_ANCHOR_SECONDARY = "autoanchor.properties";
    public static final String AUTO_ANCHOR_JSON = "autoanchor.json";

    public static final String CART_REFILL = "storage_tweaks.properties";
    public static final String CART_REFILL_SECONDARY = "cartrefill.properties";
    public static final String CART_REFILL_JSON = "cartrefill.json";

    public static final String HP_REAPER = "hp_reaper.properties";
    public static final String HP_REAPER_JSON = "hp_reaper.json";
    public static final String HP_REAPER_SECONDARY = "vitality_tweaks.properties";
    public static final String HP_REAPER_TERTIARY = "hpreaper.properties";
    public static final String HP_REAPER_TERTIARY_JSON = "hpreaper.json";

    public static final String AUTO_TOOL = "autotool.json";
    public static final String AUTO_TOOL_PROP = "autotool.properties";

    public static final String AUTO_GG = "autogg.json";
    public static final String AUTO_GG_PROP = "autogg.properties";

    public static final String CART_HUD = "cart_hud.properties";
    public static final String CART_HUD_JSON = "cart_hud.json";
    public static final String CART_HUD_SECONDARY = "carthud.properties";
    public static final String CART_HUD_SECONDARY_JSON = "carthud.json";

    public static final String KEYS_REDSTONE = "redstone_keys.properties";
    public static final String KEYS_PVP = "pvp_keys.properties";
    public static final String KEYS_JSON = "keys.json";

    private static final List<String> ALL_LEGACY_FILENAMES = List.of(
        AUTO_MACE_PRIMARY,
        AUTO_MACE_LEGACY,
        AUTO_MACE_PROP,
        AUTO_MACE_JSON,
        "auto_mace.properties",
        "auto_mace.json",
        AUTO_SPEAR,
        AUTO_SPEAR_SECONDARY,
        AUTO_SPEAR_JSON,
        "auto_spear.properties",
        "auto_spear.json",
        AUTO_SHIELDBREAKER,
        AUTO_SHIELDBREAKER_JSON,
        "auto_shieldbreaker.properties",
        "auto_shieldbreaker.json",
        AUTO_STUN_SLIME,
        AUTO_STUN_SLIME_JSON,
        AUTO_STUN_SLAM,
        AUTO_STUN_SLAM_JSON,
        "auto_stun_slime.properties",
        "auto_stun_slime.json",
        "auto_stun_slam.properties",
        "auto_stun_slam.json",
        AUTO_TOTEM,
        AUTO_TOTEM_JSON,
        "auto_totem.properties",
        "auto_totem.json",
        AUTO_CART,
        AUTO_CART_JSON,
        AUTO_CART_SECONDARY,
        AUTO_CART_SECONDARY_JSON,
        "auto_cart.properties",
        "auto_cart.json",
        AUTO_ANCHOR_PRIMARY,
        AUTO_ANCHOR_SECONDARY,
        AUTO_ANCHOR_JSON,
        "auto_anchor.properties",
        "auto_anchor.json",
        CART_REFILL,
        CART_REFILL_SECONDARY,
        CART_REFILL_JSON,
        "cart_refill.properties",
        "cart_refill.json",
        HP_REAPER,
        HP_REAPER_JSON,
        HP_REAPER_SECONDARY,
        HP_REAPER_TERTIARY,
        HP_REAPER_TERTIARY_JSON,
        "hp_reaper.properties",
        AUTO_TOOL,
        AUTO_TOOL_PROP,
        "auto_tool.json",
        "auto_tool.properties",
        AUTO_GG,
        AUTO_GG_PROP,
        "auto_gg.json",
        "auto_gg.properties",
        CART_HUD,
        CART_HUD_JSON,
        CART_HUD_SECONDARY,
        CART_HUD_SECONDARY_JSON,
        "cart_hud.json",
        KEYS_REDSTONE,
        KEYS_PVP,
        KEYS_JSON
    );

    private LegacyConfigMigrator() {}

    public static Path resolveConfigDir() {
        try {
            FabricLoader loader = FabricLoader.getInstance();
            if (loader != null && loader.getConfigDir() != null) {
                return loader.getConfigDir();
            }
        } catch (Throwable ignored) {}
        return Path.of("config");
    }

    public static boolean hasAnyLegacyConfig(Path configDir) {
        if (configDir == null || !Files.exists(configDir)) return false;
        for (String filename : ALL_LEGACY_FILENAMES) {
            if (Files.exists(configDir.resolve(filename))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAlreadyMigrated(ActivityConfig config, Path configDir) {
        if (config != null && config.legacyMigrationDone && config.legacyMigrationVersion >= CURRENT_MIGRATION_VERSION) {
            return true;
        }
        if (configDir != null && Files.exists(configDir.resolve(MIGRATION_MARKER_FILE))) {
            if (config != null) {
                config.legacyMigrationDone = true;
                config.legacyMigrationVersion = CURRENT_MIGRATION_VERSION;
            }
            return true;
        }
        return false;
    }

    public static void markMigrated(ActivityConfig config, Path configDir) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
            return;
        }
        if (config != null) {
            config.legacyMigrationDone = true;
            config.legacyMigrationVersion = CURRENT_MIGRATION_VERSION;
        }
        if (configDir != null) {
            try {
                if (!Files.exists(configDir)) {
                    Files.createDirectories(configDir);
                }
                Path markerPath = configDir.resolve(MIGRATION_MARKER_FILE);
                String markerContent = "migration_version=" + CURRENT_MIGRATION_VERSION + "\n" +
                                       "migrated_at=" + System.currentTimeMillis() + "\n" +
                                       "client=CooldownHUD\n";
                Files.writeString(markerPath, markerContent, StandardCharsets.UTF_8);
            } catch (Exception e) {
                ActivityClient.LOGGER.debug("[CooldownHUD] Could not write migration marker: {}", e.getMessage());
            }
        }
    }

    public static void cleanupLegacyFiles(Path configDir) {
        if (configDir == null || !Files.exists(configDir)) return;
        for (String filename : ALL_LEGACY_FILENAMES) {
            try {
                Files.deleteIfExists(configDir.resolve(filename));
            } catch (Exception ignored) {}
        }
    }

    public static boolean checkAndMigrate(ActivityConfig config) {
        Path configDir = resolveConfigDir();
        return migrate(config, configDir, false);
    }

    public static boolean migrate(ActivityConfig config, Path configDir) {
        return migrate(config, configDir, false);
    }

    public static boolean migrate(ActivityConfig config, Path configDir, boolean force) {
        if (config == null || configDir == null) return false;

        if (!force && isAlreadyMigrated(config, configDir)) {
            ActivityClient.LOGGER.debug("[CooldownHUD] Legacy config migration already completed. Skipping.");
            return false;
        }

        if (!hasAnyLegacyConfig(configDir)) {
            ActivityClient.LOGGER.debug("[CooldownHUD] No legacy mod configs detected in {}. Marking as clean install.", configDir);
            markMigrated(config, configDir);
            return false;
        }

        ActivityClient.LOGGER.debug("[CooldownHUD] Found legacy mod configs in {}. Starting consolidated migration...", configDir);

        int migratedModules = 0;

        if (migrateAutoMace(config, configDir)) migratedModules++;

        if (migrateAutoSpear(config, configDir)) migratedModules++;

        if (migrateAutoShieldbreaker(config, configDir)) migratedModules++;

        if (migrateAutoStunSlam(config, configDir)) migratedModules++;

        if (migrateAutoTotem(config, configDir)) migratedModules++;

        if (migrateAutoCart(config, configDir)) migratedModules++;

        if (migrateAutoAnchor(config, configDir)) migratedModules++;

        if (migrateCartRefill(config, configDir)) migratedModules++;

        if (migrateHpReaper(config, configDir)) migratedModules++;

        if (migrateAutoTool(config, configDir)) migratedModules++;

        if (migrateAutoGG(config, configDir)) migratedModules++;

        if (migrateCartHud(config, configDir)) migratedModules++;

        migrateKeybinds(config, configDir);

        config.sanitize();
        ModuleRegistry.loadAll(config);
        NivoratConfigManager.syncToModules(config);

        markMigrated(config, configDir);
        cleanupLegacyFiles(configDir);

        ActivityClient.LOGGER.debug("[CooldownHUD] Legacy migration finished. Migrated {} module configurations successfully.", migratedModules);
        return true;
    }

    private static boolean migrateAutoMace(ActivityConfig config, Path dir) {
        LegacyConfigData data = loadLegacyData(dir, AUTO_MACE_PRIMARY, AUTO_MACE_LEGACY, AUTO_MACE_PROP, AUTO_MACE_JSON, "auto_mace.properties", "auto_mace.json");
        if (data == null) return false;

        Boolean enabled = data.getBoolean("enabled");
        if (enabled != null) config.autoMaceEnabled = enabled;

        String enchantModeStr = data.getString("enchant_mode", "enchantMode");
        if (enchantModeStr != null) {
            if ("1".equals(enchantModeStr) || "breach".equalsIgnoreCase(enchantModeStr) || "breach_only".equalsIgnoreCase(enchantModeStr)) {
                config.autoMaceEnchantMode = "breach_only";
            } else if ("2".equals(enchantModeStr) || "density".equalsIgnoreCase(enchantModeStr) || "density_only".equalsIgnoreCase(enchantModeStr)) {
                config.autoMaceEnchantMode = "density_only";
            } else {
                config.autoMaceEnchantMode = "smart";
            }
        } else if (data.containsKey("smart_mode") || data.containsKey("smartMode")) {
            Boolean sm = data.getBoolean("smart_mode", "smartMode");
            config.autoMaceEnchantMode = (sm != null && sm) ? "smart" : "breach_only";
        }

        String missBeh = data.getString("miss_behavior", "missBehavior");
        if (missBeh != null) {
            if ("1".equals(missBeh) || "empty_swap".equalsIgnoreCase(missBeh) || "no_hit_swap".equalsIgnoreCase(missBeh)) {
                config.autoMaceMissBehavior = "empty_swap";
            } else {
                config.autoMaceMissBehavior = "sword_hit";
            }
        }

        String sourceModeStr = data.getString("source_mode", "sourceMode");
        if (sourceModeStr != null) {
            if ("0".equals(sourceModeStr) || "sword_only".equalsIgnoreCase(sourceModeStr)) {
                config.autoMaceSourceMode = "sword_only";
            } else if ("1".equals(sourceModeStr) || "axe_only".equalsIgnoreCase(sourceModeStr)) {
                config.autoMaceSourceMode = "axe_only";
            } else {
                config.autoMaceSourceMode = "sword_and_axe";
            }
        }

        Double delay = data.getDouble("restore_delay_ms", "restoreDelayMs", "propagation_delay", "propagationDelay");
        if (delay != null) config.autoMaceRestoreDelayMs = delay;

        Boolean randDelay = data.getBoolean("random_delay", "randomDelay");
        if (randDelay != null) config.autoMaceRandomDelay = randDelay;

        Boolean legit = data.getBoolean("legit_mode", "legitMode", "safe_mode", "safeMode");
        if (legit != null) config.autoMaceLegitMode = legit;

        Double missChance = data.getDouble("miss_chance", "missChance", "variance_pct", "variancePct");
        if (missChance != null) config.autoMaceMissChance = missChance;

        migrateModuleKeybind(data, config.autoMaceKeybind, "keybind", "key", "key_auto_mace", "bind");

        return true;
    }

    private static boolean migrateAutoSpear(ActivityConfig config, Path dir) {
        LegacyConfigData data = loadLegacyData(dir, AUTO_SPEAR, AUTO_SPEAR_SECONDARY, AUTO_SPEAR_JSON, "auto_spear.properties", "auto_spear.json");
        if (data == null) return false;

        Boolean enabled = data.getBoolean("enabled");
        if (enabled != null) config.autoSpearEnabled = enabled;

        String secModeStr = data.getString("security_mode", "securityMode");
        if (secModeStr != null) {
            if ("1".equals(secModeStr) || "semi_legit".equalsIgnoreCase(secModeStr)) {
                config.autoSpearSecurityMode = "semi_legit";
            } else if ("2".equals(secModeStr) || "rage".equalsIgnoreCase(secModeStr)) {
                config.autoSpearSecurityMode = "rage";
            } else {
                config.autoSpearSecurityMode = "legit";
            }
        }

        String prioStr = data.getString("priority_mode", "priorityMode");
        if (prioStr != null) {
            if ("1".equals(prioStr) || "lunge_1".equalsIgnoreCase(prioStr)) {
                config.autoSpearPriorityMode = "lunge_1";
            } else if ("2".equals(prioStr) || "lunge_2".equalsIgnoreCase(prioStr)) {
                config.autoSpearPriorityMode = "lunge_2";
            } else if ("3".equals(prioStr) || "lunge_3".equalsIgnoreCase(prioStr)) {
                config.autoSpearPriorityMode = "lunge_3";
            } else if ("4".equals(prioStr) || "random".equalsIgnoreCase(prioStr)) {
                config.autoSpearPriorityMode = "random";
            } else {
                config.autoSpearPriorityMode = "auto";
            }
        }

        Double delay = data.getDouble("max_delay_ms", "maxDelayMs", "restore_delay_ms", "restoreDelayMs");
        if (delay != null) config.autoSpearRestoreDelayMs = delay;

        Double missChance = data.getDouble("miss_chance", "missChance", "variance_pct", "variancePct");
        if (missChance != null) config.autoSpearMissChance = missChance;

        Boolean randDelay = data.getBoolean("random_delay", "randomDelay");
        if (randDelay != null) config.autoSpearRandomDelay = randDelay;

        migrateModuleKeybind(data, config.autoSpearKeybind, "keybind", "key", "key_spear_swap", "bind");

        return true;
    }

    private static boolean migrateAutoShieldbreaker(ActivityConfig config, Path dir) {
        LegacyConfigData data = loadLegacyData(dir, AUTO_SHIELDBREAKER, AUTO_SHIELDBREAKER_JSON, "auto_shieldbreaker.properties", "auto_shieldbreaker.json");
        if (data == null) return false;

        Boolean enabled = data.getBoolean("enabled");
        if (enabled != null) config.autoShieldbreakerEnabled = enabled;

        String modeStr = data.getString("mode");
        if (modeStr != null) {
            config.autoShieldbreakerMode = ("1".equals(modeStr) || "semi_auto".equalsIgnoreCase(modeStr))
                    ? "semi_auto" : "full_auto";
        } else if (data.containsKey("full_auto") || data.containsKey("fullAuto")) {
            Boolean full = data.getBoolean("full_auto", "fullAuto");
            config.autoShieldbreakerMode = (full != null && full) ? "full_auto" : "semi_auto";
        }

        Boolean legit = data.getBoolean("legitMode", "legit_mode");
        if (legit != null) config.autoShieldbreakerLegitMode = legit;

        Boolean abort = data.getBoolean("abortOnManualSwitch", "abort_on_manual_switch");
        if (abort != null) config.autoShieldbreakerAbortOnManualSwitch = abort;

        Double chance = data.getDouble("chance");
        if (chance != null) config.autoShieldbreakerChance = chance;

        Double dist = data.getDouble("distance", "triggerDistance", "trigger_distance");
        if (dist != null) config.autoShieldbreakerDistance = dist;

        Double swDelay = data.getDouble("switch_delay_ms", "switchDelayMs");
        if (swDelay != null) {
            config.autoShieldbreakerSwitchDelayMs = swDelay;
        } else {
            Double swSec = data.getDouble("switchDelaySec", "switch_delay_sec");
            if (swSec != null) {
                config.autoShieldbreakerSwitchDelayMs = swSec * 1000.0;
            }
        }

        Double restDelay = data.getDouble("restore_delay_ms", "restoreDelayMs");
        if (restDelay != null) config.autoShieldbreakerRestoreDelayMs = restDelay;

        Boolean randDelay = data.getBoolean("randomDelay", "random_delay");
        if (randDelay != null) config.autoShieldbreakerRandomDelay = randDelay;

        migrateModuleKeybind(data, config.autoShieldbreakerKeybind, "keybind", "key", "key_shield_breaker", "bind");

        return true;
    }

    private static boolean migrateAutoStunSlam(ActivityConfig config, Path dir) {
        LegacyConfigData data = loadLegacyData(
            dir,
            AUTO_STUN_SLIME,
            AUTO_STUN_SLIME_JSON,
            AUTO_STUN_SLAM,
            AUTO_STUN_SLAM_JSON,
            "auto_stun_slime.properties",
            "auto_stun_slime.json",
            "auto_stun_slam.properties",
            "auto_stun_slam.json"
        );
        if (data == null) return false;

        Boolean enabled = data.getBoolean("enabled");
        if (enabled != null) config.autoStunSlamEnabled = enabled;

        String modeStr = data.getString("mode");
        if (modeStr != null) {
            config.autoStunSlamMode = ("1".equals(modeStr) || "semi_auto".equalsIgnoreCase(modeStr))
                    ? "semi_auto" : "full_auto";
        } else if (data.containsKey("full_auto") || data.containsKey("fullAuto")) {
            Boolean full = data.getBoolean("full_auto", "fullAuto");
            config.autoStunSlamMode = (full != null && full) ? "full_auto" : "semi_auto";
        }

        Boolean legit = data.getBoolean("legitMode", "legit_mode");
        if (legit != null) config.autoStunSlamLegitMode = legit;

        Double chance = data.getDouble("chance");
        if (chance != null) config.autoStunSlamChance = chance;

        Double dist = data.getDouble("distance", "triggerDistance", "trigger_distance");
        if (dist != null) config.autoStunSlamDistance = dist;

        Double airTime = data.getDouble("air_time_sec", "airTimeSec");
        if (airTime != null) config.autoStunSlamAirTimeSec = airTime;

        Double axeDelay = data.getDouble("axe_delay_ms", "axeDelayMs");
        if (axeDelay != null) config.autoStunSlamAxeDelayMs = axeDelay;

        Double maceDelay = data.getDouble("mace_delay_ms", "maceDelayMs");
        if (maceDelay != null) config.autoStunSlamMaceDelayMs = maceDelay;

        Double restDelay = data.getDouble("restore_delay_ms", "restoreDelayMs");
        if (restDelay != null) config.autoStunSlamRestoreDelayMs = restDelay;

        Boolean randDelay = data.getBoolean("randomDelay", "random_delay");
        if (randDelay != null) config.autoStunSlamRandomDelay = randDelay;

        migrateModuleKeybind(data, config.autoStunSlamKeybind, "keybind", "key", "key_auto_stun", "key_auto_stun_slime", "key_auto_stun_slam", "bind");

        return true;
    }

    private static boolean migrateAutoTotem(ActivityConfig config, Path dir) {
        LegacyConfigData data = loadLegacyData(dir, AUTO_TOTEM, AUTO_TOTEM_JSON, "auto_totem.properties", "auto_totem.json");
        if (data == null) return false;

        Boolean enabled = data.getBoolean("enabled");
        if (enabled != null) config.autoTotemEnabled = enabled;

        Double trig = data.getDouble("trigger_hearts", "triggerHearts");
        if (trig != null) config.autoTotemTriggerHearts = trig;

        Double rest = data.getDouble("restore_hearts", "restoreHearts");
        if (rest != null) config.autoTotemRestoreHearts = rest;

        Double chance = data.getDouble("chance");
        if (chance != null) config.autoTotemChance = chance;

        Boolean retItem = data.getBoolean("return_item", "returnItem");
        if (retItem != null) config.autoTotemReturnItem = retItem;

        Boolean retPop = data.getBoolean("return_on_pop", "returnOnPop");
        if (retPop != null) config.autoTotemReturnOnPop = retPop;

        String modeStr = data.getString("mode", "slot_mode", "slotMode");
        if (modeStr != null) {
            config.autoTotemMode = ("2".equals(modeStr) || "offhand".equalsIgnoreCase(modeStr)) ? "offhand" : "main_hand";
        }

        migrateModuleKeybind(data, config.autoTotemKeybind, "keybind", "key", "key_autototem", "bind");

        return true;
    }

    private static boolean migrateAutoCart(ActivityConfig config, Path dir) {
        LegacyConfigData data = loadLegacyData(dir, AUTO_CART, AUTO_CART_JSON, AUTO_CART_SECONDARY, AUTO_CART_SECONDARY_JSON, "auto_cart.properties", "auto_cart.json");
        if (data == null) return false;

        Boolean enabled = data.getBoolean("enabled");
        if (enabled != null) config.autoCartEnabled = enabled;

        Double placeChance = data.getDouble("placement_chance", "placementChance");
        if (placeChance != null) config.autoCartPlacementChance = placeChance;

        Boolean legit = data.getBoolean("legit_mode", "legitMode");
        if (legit != null) config.autoCartLegitMode = legit;

        Boolean randDelay = data.getBoolean("random_delay", "randomDelay");
        if (randDelay != null) config.autoCartRandomDelay = randDelay;

        String presetStr = data.getString("preset", "cart_preset", "cartPreset");
        if (presetStr != null) {
            if ("0".equals(presetStr) || "fast".equalsIgnoreCase(presetStr)) {
                config.autoCartPreset = "fast";
            } else if ("2".equals(presetStr) || "safe".equalsIgnoreCase(presetStr)) {
                config.autoCartPreset = "safe";
            } else {
                config.autoCartPreset = "medium";
            }
        }

        Double maxDist = data.getDouble("max_distance", "maxDistance");
        if (maxDist != null) config.autoCartMaxDistance = maxDist;

        Boolean allowSelf = data.getBoolean("allow_self_cart", "allowSelfCart");
        if (allowSelf != null) config.autoCartAllowSelfCart = allowSelf;

        Boolean allowPit = data.getBoolean("allow_pit_placement", "allowPitPlacement");
        if (allowPit != null) config.autoCartAllowPitPlacement = allowPit;

        Double minDelay = data.getDouble("min_delay_ms", "minDelayMs");
        if (minDelay != null) config.autoCartMinDelayMs = minDelay;

        Double maxDelay = data.getDouble("max_delay_ms", "maxDelayMs");
        if (maxDelay != null) config.autoCartMaxDelayMs = maxDelay;

        Double railDelay = data.getDouble("rail_delay", "railDelay");
        if (railDelay != null) config.autoCartRailDelay = railDelay;

        Double cartDelay = data.getDouble("cart_delay", "cartDelay");
        if (cartDelay != null) config.autoCartCartDelay = cartDelay;

        Double restoreDelay = data.getDouble("restore_delay", "restoreDelay");
        if (restoreDelay != null) config.autoCartRestoreDelay = restoreDelay;

        migrateModuleKeybind(data, config.autoCartKeybind, "keybind", "key", "key_cart", "bind");

        return true;
    }

    private static boolean migrateAutoAnchor(ActivityConfig config, Path dir) {
        LegacyConfigData data = loadLegacyData(dir, AUTO_ANCHOR_PRIMARY, AUTO_ANCHOR_SECONDARY, AUTO_ANCHOR_JSON, "auto_anchor.properties", "auto_anchor.json");
        if (data == null) return false;

        Boolean enabled = data.getBoolean("enabled");
        if (enabled != null) config.autoAnchorEnabled = enabled;

        Boolean autoExplode = data.getBoolean("auto_explode", "autoExplode");
        if (autoExplode != null) config.autoAnchorAutoExplode = autoExplode;

        Boolean autoReturn = data.getBoolean("auto_return", "autoReturn");
        if (autoReturn != null) config.autoAnchorAutoReturn = autoReturn;

        Double chargeDelay = data.getDouble("charge_delay_ticks", "chargeDelayTicks", "charge_delay");
        if (chargeDelay != null) config.autoAnchorChargeDelay = chargeDelay;

        Double explodeDelay = data.getDouble("explode_delay_ticks", "explodeDelayTicks", "explode_delay");
        if (explodeDelay != null) config.autoAnchorExplodeDelay = explodeDelay;

        Double chance = data.getDouble("chance", "anchor_chance", "anchorChance");
        if (chance != null) config.autoAnchorChance = chance;

        Boolean legit = data.getBoolean("legit_mode", "legitMode", "safe_mode", "safeMode");
        if (legit != null) config.autoAnchorLegitMode = legit;

        Double charges = data.getDouble("target_charges", "targetCharges");
        if (charges != null) config.autoAnchorTargetCharges = charges;

        String presetStr = data.getString("preset", "anchor_preset", "anchorPreset");
        if (presetStr != null) {
            if ("FAST".equalsIgnoreCase(presetStr) || "fast".equalsIgnoreCase(presetStr)) config.autoAnchorPreset = "fast";
            else if ("MEDIUM".equalsIgnoreCase(presetStr) || "medium".equalsIgnoreCase(presetStr)) config.autoAnchorPreset = "medium";
            else if ("SAFE".equalsIgnoreCase(presetStr) || "safe".equalsIgnoreCase(presetStr)) config.autoAnchorPreset = "safe";
            else config.autoAnchorPreset = "balanced";
        }

        migrateModuleKeybind(data, config.autoAnchorKeybind, "keybind", "key", "key_auto_anchor", "bind");

        return true;
    }

    private static boolean migrateCartRefill(ActivityConfig config, Path dir) {
        LegacyConfigData data = loadLegacyData(dir, CART_REFILL, CART_REFILL_SECONDARY, CART_REFILL_JSON, "cart_refill.properties", "cart_refill.json");
        if (data == null) return false;

        Boolean enabled = data.getBoolean("enabled");
        if (enabled != null) config.cartRefillEnabled = enabled;

        Double delay = data.getDouble("refill_delay_ticks", "refillDelayTicks");
        if (delay != null) config.cartRefillDelayTicks = delay;

        Double chance = data.getDouble("chance", "refill_chance", "refillChance");
        if (chance != null) config.cartRefillChance = chance;

        Boolean legit = data.getBoolean("legit_mode", "legitMode", "safe_mode", "safeMode");
        if (legit != null) config.cartRefillLegitMode = legit;

        Boolean autoClose = data.getBoolean("auto_close", "autoClose", "auto_close_container");
        if (autoClose != null) config.cartRefillAutoClose = autoClose;

        Boolean randDelay = data.getBoolean("random_delay", "randomDelay");
        if (randDelay != null) config.cartRefillRandomDelay = randDelay;

        migrateModuleKeybind(data, config.cartRefillKeybind, "keybind", "key", "key_cart_refill", "bind");

        return true;
    }

    private static boolean migrateHpReaper(ActivityConfig config, Path dir) {
        LegacyConfigData data = loadLegacyData(dir, HP_REAPER, HP_REAPER_JSON, HP_REAPER_SECONDARY, HP_REAPER_TERTIARY, HP_REAPER_TERTIARY_JSON, "hp_reaper.properties");
        if (data == null) return false;

        Boolean enabled = data.getBoolean("enabled");
        if (enabled != null) config.hpReaperEnabled = enabled;

        String mode = data.getString("display_mode", "displayMode", "hud_mode", "hudMode");
        if (mode != null) {
            if ("OWN_HEALTH".equalsIgnoreCase(mode) || "own_hp".equalsIgnoreCase(mode)) {
                config.hpReaperMode = "own_hp";
            } else if ("DIFFERENCE".equalsIgnoreCase(mode) || "damage_diff".equalsIgnoreCase(mode)) {
                config.hpReaperMode = "damage_diff";
            } else if ("COMPACT".equalsIgnoreCase(mode) || "compact".equalsIgnoreCase(mode)) {
                config.hpReaperMode = "compact";
            } else {
                config.hpReaperMode = "target_hp";
            }
        }

        String filter = data.getString("target_filter", "targetFilter", "filter_type", "filterType");
        if (filter != null) {
            if ("PLAYERS_ONLY".equalsIgnoreCase(filter) || "players_only".equalsIgnoreCase(filter)) {
                config.hpReaperTargetFilter = "players_only";
            } else if ("HOSTILE_ONLY".equalsIgnoreCase(filter) || "hostile_and_players".equalsIgnoreCase(filter)) {
                config.hpReaperTargetFilter = "hostile_and_players";
            } else {
                config.hpReaperTargetFilter = "all_entities";
            }
        }

        Integer ownX = data.getInt("own_health_x", "ownHealthX", "own_hp_x", "ownHpX");
        if (ownX != null) config.hpReaperOwnHealthX = ownX;
        Integer ownY = data.getInt("own_health_y", "ownHealthY", "own_hp_y", "ownHpY");
        if (ownY != null) config.hpReaperOwnHealthY = ownY;

        Integer chX = data.getInt("crosshair_target_x", "crosshairTargetX", "crosshair_x", "crosshairX");
        if (chX != null) config.hpReaperCrosshairTargetX = chX;
        Integer chY = data.getInt("crosshair_target_y", "crosshairTargetY", "crosshair_y", "crosshairY");
        if (chY != null) config.hpReaperCrosshairTargetY = chY;

        Integer tgtX = data.getInt("target_health_x", "targetHealthX", "target_hp_x", "targetHpX");
        if (tgtX != null) config.hpReaperTargetHealthX = tgtX;
        Integer tgtY = data.getInt("target_health_y", "targetHealthY", "target_hp_y", "targetHpY");
        if (tgtY != null) config.hpReaperTargetHealthY = tgtY;

        Integer diffX = data.getInt("diff_x", "diffX");
        if (diffX != null) config.hpReaperDiffX = diffX;
        Integer diffY = data.getInt("diff_y", "diffY");
        if (diffY != null) config.hpReaperDiffY = diffY;

        migrateModuleKeybind(data, config.hpReaperKeybind, "keybind", "key", "key_hpreaper", "bind");

        return true;
    }

    private static boolean migrateAutoTool(ActivityConfig config, Path dir) {
        LegacyConfigData data = loadLegacyData(dir, AUTO_TOOL, AUTO_TOOL_PROP, "auto_tool.json", "auto_tool.properties");
        if (data == null) return false;

        Boolean enabled = data.getBoolean("enabled");
        if (enabled != null) config.autoToolEnabled = enabled;

        Boolean singleSlot = data.getBoolean("singleSlotMode", "single_slot_mode");
        if (singleSlot != null) config.autoToolSingleSlotMode = singleSlot;

        Boolean legit = data.getBoolean("legitMode", "legit_mode");
        if (legit != null) config.autoToolLegitMode = legit;

        Boolean guard = data.getBoolean("combatGuard", "combat_guard");
        if (guard != null) config.autoToolCombatGuard = guard;

        Boolean instant = data.getBoolean("ignoreInstantBreak", "ignore_instant_break");
        if (instant != null) config.autoToolIgnoreInstantBreak = instant;

        Boolean lock = data.getBoolean("lockWhileMining", "lock_while_mining");
        if (lock != null) config.autoToolLockWhileMining = lock;

        Boolean saver = data.getBoolean("durabilitySaver", "durability_saver");
        if (saver != null) config.autoToolDurabilitySaver = saver;

        Double thresh = data.getDouble("durabilityThreshold", "durability_threshold");
        if (thresh != null) config.autoToolDurabilityThreshold = thresh;

        Boolean silk = data.getBoolean("preferSilkTouch", "prefer_silk_touch");
        if (silk != null) config.autoToolPreferSilkTouch = silk;

        Boolean restore = data.getBoolean("restorePreviousItem", "restore_previous", "restorePrevious");
        if (restore != null) config.autoToolRestorePrevious = restore;

        migrateModuleKeybind(data, config.autoToolKeybind, "keybind", "key", "key_autotool", "bind");

        return true;
    }

    private static boolean migrateAutoGG(ActivityConfig config, Path dir) {
        LegacyConfigData data = loadLegacyData(dir, AUTO_GG, AUTO_GG_PROP, "auto_gg.json", "auto_gg.properties");
        if (data == null) return false;

        Boolean enabled = data.getBoolean("enabled");
        if (enabled != null) config.autoGGEnabled = enabled;

        String phrase = data.getString("phrase");
        if (phrase != null && !phrase.isBlank()) config.autoGGPhrase = phrase;

        Boolean onKill = data.getBoolean("sendOnKill", "send_on_kill");
        if (onKill != null) config.autoGGSendOnKill = onKill;

        Boolean onDeath = data.getBoolean("sendOnOwnDeath", "send_on_own_death");
        if (onDeath != null) config.autoGGSendOnOwnDeath = onDeath;

        Boolean randOrder = data.getBoolean("randomOrder", "random_order");
        if (randOrder != null) config.autoGGRandomOrder = randOrder;

        if (data.json != null && data.json.has("phrases") && data.json.get("phrases").isJsonArray()) {
            JsonArray arr = data.json.getAsJsonArray("phrases");
            int selected = data.json.has("selected") ? data.json.get("selected").getAsInt() : 0;
            if (selected >= 0 && selected < arr.size()) {
                config.autoGGPhrase = arr.get(selected).getAsString();
            } else if (!arr.isEmpty()) {
                config.autoGGPhrase = arr.get(0).getAsString();
            }
        }

        Double delay = data.getDouble("delayMs", "delay_ms");
        if (delay != null) config.autoGGDelayMs = delay;

        migrateModuleKeybind(data, config.autoGGKeybind, "keybind", "key", "key_autogg", "bind");

        return true;
    }

    private static boolean migrateCartHud(ActivityConfig config, Path dir) {
        LegacyConfigData data = loadLegacyData(dir, CART_HUD, CART_HUD_JSON, CART_HUD_SECONDARY, CART_HUD_SECONDARY_JSON, "cart_hud.json");
        if (data == null) return false;

        Boolean enabled = data.getBoolean("enabled");
        if (enabled != null) config.cartHudEnabled = enabled;

        Integer x = data.getInt("custom_x", "customX");
        if (x != null) config.cartHudCustomX = x;

        Integer y = data.getInt("custom_y", "customY");
        if (y != null) config.cartHudCustomY = y;

        migrateModuleKeybind(data, config.cartHudKeybind, "keybind", "key", "key_carthud", "bind");

        return true;
    }

    private static void migrateKeybinds(ActivityConfig config, Path dir) {
        Path redstoneKeys = dir.resolve(KEYS_REDSTONE);
        Path pvpKeys = dir.resolve(KEYS_PVP);
        Path keysJson = dir.resolve(KEYS_JSON);

        Properties p = new Properties();
        if (Files.exists(pvpKeys)) {
            Properties pvp = loadProperties(pvpKeys);
            if (pvp != null) p.putAll(pvp);
        }
        if (Files.exists(redstoneKeys)) {
            Properties r = loadProperties(redstoneKeys);
            if (r != null) p.putAll(r);
        }

        if (!p.isEmpty()) {
            applyKeybind(p, config.autoMaceKeybind, "key_auto_mace", "auto_mace");
            applyKeybind(p, config.autoSpearKeybind, "key_spear_swap", "spear_swap", "key_auto_spear", "auto_spear");
            applyKeybind(p, config.autoShieldbreakerKeybind, "key_shield_breaker", "shield_breaker", "key_auto_shieldbreaker", "auto_shieldbreaker");
            applyKeybind(p, config.autoStunSlamKeybind, "key_auto_stun", "auto_stun", "key_auto_stun_slime", "key_auto_stun_slam", "autostunslime", "autostunslam", "auto_stun_slime", "auto_stun_slam");
            applyKeybind(p, config.autoTotemKeybind, "key_autototem", "autototem", "key_auto_totem", "auto_totem");
            applyKeybind(p, config.autoCartKeybind, "key_cart", "cart", "key_auto_cart", "auto_cart");
            applyKeybind(p, config.autoAnchorKeybind, "key_auto_anchor", "auto_anchor");
            applyKeybind(p, config.cartRefillKeybind, "key_cart_refill", "cart_refill");
            applyKeybind(p, config.hpReaperKeybind, "key_hpreaper", "hpreaper", "key_hp_reaper", "hp_reaper");
            applyKeybind(p, config.autoToolKeybind, "key_autotool", "autotool", "key_auto_tool", "auto_tool");
            applyKeybind(p, config.autoGGKeybind, "key_autogg", "autogg", "key_auto_gg", "auto_gg");
            applyKeybind(p, config.cartHudKeybind, "key_carthud", "carthud", "key_cart_hud", "cart_hud");
        }

        if (Files.exists(keysJson)) {
            try {
                String json = Files.readString(keysJson, StandardCharsets.UTF_8);
                JsonElement el = JsonParser.parseString(json);
                if (el.isJsonObject()) {
                    JsonObject obj = el.getAsJsonObject();
                    applyJsonKeybind(obj, config.autoMaceKeybind, "auto_mace", "autoMace");
                    applyJsonKeybind(obj, config.autoSpearKeybind, "auto_spear", "autoSpear");
                    applyJsonKeybind(obj, config.autoShieldbreakerKeybind, "auto_shieldbreaker", "autoShieldbreaker");
                    applyJsonKeybind(obj, config.autoStunSlamKeybind, "auto_stun_slam", "autoStunSlam", "auto_stun_slime", "autoStunSlime");
                    applyJsonKeybind(obj, config.autoTotemKeybind, "auto_totem", "autoTotem");
                    applyJsonKeybind(obj, config.autoCartKeybind, "auto_cart", "autoCart");
                    applyJsonKeybind(obj, config.autoAnchorKeybind, "auto_anchor", "autoAnchor");
                    applyJsonKeybind(obj, config.cartRefillKeybind, "cart_refill", "cartRefill");
                    applyJsonKeybind(obj, config.hpReaperKeybind, "hp_reaper", "hpReaper");
                    applyJsonKeybind(obj, config.autoToolKeybind, "auto_tool", "autoTool");
                    applyJsonKeybind(obj, config.autoGGKeybind, "auto_gg", "autoGG");
                    applyJsonKeybind(obj, config.cartHudKeybind, "cart_hud", "cartHud");
                }
            } catch (Exception ignored) {}
        }
    }

    private static void migrateModuleKeybind(LegacyConfigData data, Keybind target, String... candidateKeys) {
        if (data == null || target == null) return;
        for (String k : candidateKeys) {
            if (data.containsKey(k)) {
                if (data.json != null && data.json.has(k)) {
                    JsonElement el = data.json.get(k);
                    Keybind kb = parseKeybindJson(el);
                    if (kb != null) {
                        target.copyFrom(kb);
                        return;
                    }
                }
                String val = data.getString(k);
                if (val != null && !val.isBlank()) {
                    Keybind kb = parseKeybind(val);
                    if (kb != null) {
                        target.copyFrom(kb);
                        return;
                    }
                }
            }
        }
    }

    private static void applyKeybind(Properties p, Keybind target, String... candidateKeys) {
        if (target == null) return;
        for (String k : candidateKeys) {
            String val = p.getProperty(k);
            if (val != null && !val.isBlank()) {
                Keybind parsed = parseKeybind(val);
                if (parsed != null) {
                    target.copyFrom(parsed);
                    return;
                }
            }
        }
    }

    private static void applyJsonKeybind(JsonObject obj, Keybind target, String... candidateKeys) {
        if (obj == null || target == null) return;
        for (String k : candidateKeys) {
            if (obj.has(k)) {
                Keybind kb = parseKeybindJson(obj.get(k));
                if (kb != null) {
                    target.copyFrom(kb);
                    return;
                }
            }
        }
    }

    public static Keybind parseKeybind(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String[] parts = raw.contains(",") ? raw.split(",") : raw.split(":");
        try {
            int code = Integer.parseInt(parts[0].trim());
            boolean ctrl = parts.length > 1 && Boolean.parseBoolean(parts[1].trim());
            boolean shift = parts.length > 2 && Boolean.parseBoolean(parts[2].trim());
            boolean alt = parts.length > 3 && Boolean.parseBoolean(parts[3].trim());
            return new Keybind(code, ctrl, shift, alt);
        } catch (Exception e) {
            return null;
        }
    }

    public static Keybind parseKeybindJson(JsonElement el) {
        if (el == null || el.isJsonNull()) return null;
        if (el.isJsonPrimitive()) {
            if (el.getAsJsonPrimitive().isNumber()) {
                return new Keybind(el.getAsInt());
            }
            return parseKeybind(el.getAsString());
        }
        if (el.isJsonObject()) {
            JsonObject o = el.getAsJsonObject();
            int code = o.has("code") ? o.get("code").getAsInt() : (o.has("keyCode") ? o.get("keyCode").getAsInt() : -1);
            boolean ctrl = o.has("ctrl") ? o.get("ctrl").getAsBoolean() : false;
            boolean shift = o.has("shift") ? o.get("shift").getAsBoolean() : false;
            boolean alt = o.has("alt") ? o.get("alt").getAsBoolean() : false;
            return new Keybind(code, ctrl, shift, alt);
        }
        return null;
    }

    private static Properties loadProperties(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            Properties p = new Properties();
            p.load(in);
            return p;
        } catch (Exception e) {
            ActivityClient.LOGGER.debug("[CooldownHUD] Failed to read {}: {}", path, e.getMessage());
            return null;
        }
    }

    private static LegacyConfigData loadLegacyData(Path dir, String... fileCandidates) {
        if (dir == null || !Files.exists(dir)) return null;
        for (String filename : fileCandidates) {
            Path file = dir.resolve(filename);
            if (Files.exists(file)) {
                if (filename.endsWith(".json")) {
                    try {
                        String content = Files.readString(file, StandardCharsets.UTF_8);
                        JsonElement parsed = JsonParser.parseString(content);
                        if (parsed.isJsonObject()) {
                            return new LegacyConfigData(parsed.getAsJsonObject());
                        }
                    } catch (Exception e) {
                        ActivityClient.LOGGER.debug("[CooldownHUD] Failed to parse JSON {}: {}", file, e.getMessage());
                    }
                } else {
                    Properties p = loadProperties(file);
                    if (p != null) {
                        return new LegacyConfigData(p);
                    }
                }
            }
        }
        return null;
    }

    private static final class LegacyConfigData {
        private final Properties props;
        private final JsonObject json;

        LegacyConfigData(Properties props) {
            this.props = props;
            this.json = null;
        }

        LegacyConfigData(JsonObject json) {
            this.props = null;
            this.json = json;
        }

        boolean containsKey(String key) {
            if (props != null) return props.containsKey(key);
            if (json != null) return json.has(key);
            return false;
        }

        String getString(String... keys) {
            for (String k : keys) {
                if (props != null && props.containsKey(k)) {
                    String val = props.getProperty(k);
                    if (val != null) return val.trim();
                }
                if (json != null && json.has(k)) {
                    try {
                        JsonElement el = json.get(k);
                        if (!el.isJsonNull()) {
                            return el.getAsString().trim();
                        }
                    } catch (Exception ignored) {}
                }
            }
            return null;
        }

        Boolean getBoolean(String... keys) {
            for (String k : keys) {
                if (props != null && props.containsKey(k)) {
                    return Boolean.parseBoolean(props.getProperty(k).trim());
                }
                if (json != null && json.has(k)) {
                    try {
                        JsonElement el = json.get(k);
                        if (!el.isJsonNull()) {
                            return el.getAsBoolean();
                        }
                    } catch (Exception ignored) {}
                }
            }
            return null;
        }

        Double getDouble(String... keys) {
            for (String k : keys) {
                if (props != null && props.containsKey(k)) {
                    try {
                        return Double.parseDouble(props.getProperty(k).trim());
                    } catch (NumberFormatException ignored) {}
                }
                if (json != null && json.has(k)) {
                    try {
                        JsonElement el = json.get(k);
                        if (!el.isJsonNull()) {
                            return el.getAsDouble();
                        }
                    } catch (Exception ignored) {}
                }
            }
            return null;
        }

        Integer getInt(String... keys) {
            for (String k : keys) {
                if (props != null && props.containsKey(k)) {
                    try {
                        return Integer.parseInt(props.getProperty(k).trim());
                    } catch (NumberFormatException ignored) {}
                }
                if (json != null && json.has(k)) {
                    try {
                        JsonElement el = json.get(k);
                        if (!el.isJsonNull()) {
                            return el.getAsInt();
                        }
                    } catch (Exception ignored) {}
                }
            }
            return null;
        }
    }
}

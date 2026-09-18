package activity.client.config;

import activity.client.config.migration.LegacyConfigMigrator;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.keybind.Keybind;
import activity.client.module.setting.BooleanSetting;
import activity.client.module.setting.EnumSetting;
import activity.client.module.setting.IntegerSetting;
import activity.client.module.setting.NumberSetting;
import activity.client.module.setting.Setting;
import activity.client.module.setting.StringSetting;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Unified Nivorat Configuration Manager providing high-level structured access
 * to module states, keybindings, dynamic setting values, and legacy migration.
 *
 * <p>Integrates directly with {@link ActivityConfigManager} while exposing the structured
 * hierarchical schema required by the Nivorat Module SDK:
 * <pre>
 * client:
 *   ui: ...
 *   sound: ...
 *   fonts: ...
 * modules:
 *   auto_mace:
 *     enabled: true
 *     keybind: [key, ctrl, shift, alt]
 *     settings:
 *       source_mode: "sword_and_axe"
 *       restore_delay: 90.0
 * </pre>
 */
public final class NivoratConfigManager {

    private NivoratConfigManager() {}

    /**
     * @return current active Activity/Nivorat configuration model
     */
    public static ActivityConfig getConfig() {
        return ActivityConfigManager.getConfig();
    }

    /**
     * Retrieves structured configuration entry for the specified module ID,
     * seamlessly handling legacy aliases such as "auto_stun_slime" -> "auto_stun_slam".
     *
     * @param moduleId canonical ID or legacy alias
     * @return module configuration entry or null if absent
     */
    public static ActivityConfig.ModuleConfigEntry getModuleConfig(String moduleId) {
        return getModuleConfig(getConfig(), moduleId);
    }

    /**
     * Retrieves structured configuration entry for the specified module ID from the given config,
     * seamlessly handling legacy aliases such as "auto_stun_slime" -> "auto_stun_slam".
     *
     * @param config   target configuration instance
     * @param moduleId canonical ID or legacy alias
     * @return module configuration entry or null if absent
     */
    public static ActivityConfig.ModuleConfigEntry getModuleConfig(ActivityConfig config, String moduleId) {
        if (config == null || config.modules == null || moduleId == null) return null;
        String clean = moduleId.replace("_", "").toLowerCase(java.util.Locale.ROOT);
        if ("autostunslime".equals(clean) || "autostunslam".equals(clean)) {
            ActivityConfig.ModuleConfigEntry entry = config.modules.get("auto_stun_slam");
            if (entry != null) return entry;
            entry = config.modules.get("auto_stun_slime");
            if (entry != null) return entry;
        }
        ActivityConfig.ModuleConfigEntry direct = config.modules.get(moduleId);
        if (direct != null) return direct;
        for (Map.Entry<String, ActivityConfig.ModuleConfigEntry> entry : config.modules.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(moduleId) || entry.getKey().replace("_", "").equalsIgnoreCase(clean)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Stores structured configuration entry for the specified module,
     * seamlessly handling legacy aliases such as "auto_stun_slime" -> "auto_stun_slam".
     *
     * @param moduleId canonical ID or legacy alias
     * @param entry    module configuration entry
     */
    public static void setModuleConfig(String moduleId, ActivityConfig.ModuleConfigEntry entry) {
        setModuleConfig(getConfig(), moduleId, entry);
    }

    /**
     * Stores structured configuration entry for the specified module into the given config,
     * seamlessly handling legacy aliases such as "auto_stun_slime" -> "auto_stun_slam".
     *
     * @param config   target configuration instance
     * @param moduleId canonical ID or legacy alias
     * @param entry    module configuration entry
     */
    public static void setModuleConfig(ActivityConfig config, String moduleId, ActivityConfig.ModuleConfigEntry entry) {
        if (config == null || moduleId == null || entry == null) return;
        if (config.modules == null) {
            config.modules = new LinkedHashMap<>();
        }
        String clean = moduleId.replace("_", "").toLowerCase(java.util.Locale.ROOT);
        String canonicalId = ("autostunslime".equals(clean) || "autostunslam".equals(clean))
                ? "auto_stun_slam"
                : moduleId;
        config.modules.put(canonicalId, entry);
        if ("auto_stun_slam".equals(canonicalId)) {
            config.modules.remove("auto_stun_slime");
        }
        syncFromModules(config);
        config.syncFromModuleEntries();
        markDirty();
    }

    /**
     * @param moduleId canonical ID or legacy alias
     * @return true if module is marked enabled in configuration
     */
    public static boolean isModuleEnabled(String moduleId) {
        return isModuleEnabled(getConfig(), moduleId);
    }

    /**
     * @param config   target configuration instance
     * @param moduleId canonical ID or legacy alias
     * @return true if module is marked enabled in configuration
     */
    public static boolean isModuleEnabled(ActivityConfig config, String moduleId) {
        ActivityConfig.ModuleConfigEntry entry = getModuleConfig(config, moduleId);
        return entry != null && entry.enabled;
    }

    /**
     * Updates module enabled state in configuration and active module instance.
     *
     * @param moduleId canonical ID or legacy alias
     * @param enabled  new enabled state
     */
    public static void setModuleEnabled(String moduleId, boolean enabled) {
        setModuleEnabled(getConfig(), moduleId, enabled);
    }

    /**
     * Updates module enabled state in configuration and active module instance.
     *
     * @param config   target configuration instance
     * @param moduleId canonical ID or legacy alias
     * @param enabled  new enabled state
     */
    public static void setModuleEnabled(ActivityConfig config, String moduleId, boolean enabled) {
        ActivityConfig.ModuleConfigEntry entry = getModuleConfig(config, moduleId);
        if (entry != null) {
            entry.enabled = enabled;
            IModule mod = ModuleRegistry.get(moduleId);
            if (mod != null) {
                mod.setEnabled(enabled);
            }
            syncFromModules(config);
            config.syncFromModuleEntries();
            markDirty();
        }
    }

    /**
     * Loads the configuration from disk, synchronizes modules, and verifies legacy migration.
     */
    public static ActivityConfig load() {
        ActivityConfig config = ActivityConfigManager.load();
        syncFromModules(config);
        return config;
    }

    /**
     * Persists all module states and saves configuration atomically to disk.
     */
    public static boolean save() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        if (config != null) {
            syncToModules(config);
        }
        return ActivityConfigManager.save();
    }

    /**
     * Marks configuration as dirty, scheduling it for atomic disk write.
     */
    public static void markDirty() {
        ActivityConfigManager.markDirty();
    }

    /**
     * Resets configuration to factory defaults.
     */
    public static void resetDefaults() {
        ActivityConfigManager.resetDefaults();
    }

    /**
     * @return true if legacy configuration migration has completed
     */
    public static boolean isLegacyMigrated() {
        ActivityConfig config = getConfig();
        return config != null && config.legacyMigrationDone;
    }

    /**
     * Explicitly triggers inspection and migration of legacy mod configuration files.
     *
     * @return true if legacy configs were discovered and imported, false otherwise
     */
    public static boolean importLegacyConfigs() {
        ActivityConfig config = getConfig();
        if (config == null) return false;
        boolean migrated = LegacyConfigMigrator.checkAndMigrate(config);
        if (migrated) {
            ModuleRegistry.loadAll(config);
            syncToModules(config);
            markDirty();
            save();
        }
        return migrated;
    }

    /**
     * Synchronizes registered {@link IModule} states into the structured config map.
     */
    public static void syncToModules(ActivityConfig config) {
        if (config == null) return;
        if (config.modules == null) {
            config.modules = new LinkedHashMap<>();
        }
        for (IModule module : ModuleRegistry.getAll()) {
            if (module == null) continue;
            Map<String, Object> settingMap = new LinkedHashMap<>();
            for (Setting<?> setting : module.getSettings()) {
                if (setting == null || setting instanceof activity.client.module.setting.ActionSetting || setting instanceof activity.client.module.setting.KeybindSetting) {
                    continue;
                }
                Object val = setting.get();
                if (val != null && !(val instanceof Runnable)) {
                    settingMap.put(setting.getId(), val);
                }
            }

            ActivityConfig.ModuleConfigEntry entry = config.modules.computeIfAbsent(
                    module.getId(),
                    id -> new ActivityConfig.ModuleConfigEntry(module.isEnabled(), module.getKeybind())
            );
            entry.enabled = module.isEnabled();
            if (module.getKeybind() != null) {
                entry.keybind.copyFrom(module.getKeybind());
            }
            entry.settings.putAll(settingMap);
        }
    }

    /**
     * Synchronizes structured config values back into module settings,
     * applying validation, bounds clamping, and legacy alias resolution.
     */
    public static void syncFromModules(ActivityConfig config) {
        if (config == null || config.modules == null || config.modules.isEmpty()) return;
        for (IModule module : ModuleRegistry.getAll()) {
            if (module == null) continue;
            String modId = module.getId();
            ActivityConfig.ModuleConfigEntry entry = getModuleConfig(config, modId);
            if (entry != null) {
                module.setEnabled(entry.enabled);
                if (entry.keybind != null) {
                    module.getKeybind().copyFrom(entry.keybind);
                }
                if (entry.settings != null && !entry.settings.isEmpty()) {
                    applyModuleSettings(module, entry.settings);
                }
            }
        }
    }

    /**
     * Safely applies setting values from a raw map onto module settings,
     * automatically clamping numbers, validating enums, and defaulting missing or malformed entries.
     *
     * @param module      target module
     * @param settingsMap map of raw setting id to value
     */
    public static void applyModuleSettings(IModule module, Map<String, Object> settingsMap) {
        if (module == null || settingsMap == null || settingsMap.isEmpty()) return;
        for (Setting<?> setting : module.getSettings()) {
            if (setting == null) continue;
            Object val = settingsMap.get(setting.getId());
            if (val == null) continue;

            try {
                if (setting instanceof BooleanSetting boolSetting) {
                    if (val instanceof Boolean b) {
                        boolSetting.set(b);
                    } else if (val instanceof String s) {
                        boolSetting.set(Boolean.parseBoolean(s));
                    }
                } else if (setting instanceof IntegerSetting intSetting) {
                    if (val instanceof Number n) {
                        intSetting.set(n.intValue());
                    } else if (val instanceof String s) {
                        intSetting.set(Integer.parseInt(s.trim()));
                    }
                } else if (setting instanceof NumberSetting numSetting) {
                    if (val instanceof Number n) {
                        double d = n.doubleValue();
                        if (Double.isNaN(d) || Double.isInfinite(d)) d = numSetting.getDefaultValue();
                        numSetting.set(d);
                    } else if (val instanceof String s) {
                        double d = Double.parseDouble(s.trim());
                        if (Double.isNaN(d) || Double.isInfinite(d)) d = numSetting.getDefaultValue();
                        numSetting.set(d);
                    }
                } else if (setting instanceof EnumSetting enumSetting) {
                    enumSetting.set(String.valueOf(val));
                } else if (setting instanceof StringSetting strSetting) {
                    strSetting.set(String.valueOf(val));
                }
            } catch (Exception ignored) {
                // Out-of-range, bad format, or invalid enum handled gracefully without crashing
            }
        }
    }
}

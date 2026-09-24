package activity.client.config;

import java.util.Map;

public final class NivoratConfigManager {

    private NivoratConfigManager() {}

    public static ActivityConfig getConfig() {
        return CooldownConfigManager.getConfig();
    }

    public static ActivityConfig.ModuleConfigEntry getModuleConfig(String moduleId) {
        return CooldownConfigManager.getModuleConfig(moduleId);
    }

    public static ActivityConfig.ModuleConfigEntry getModuleConfig(ActivityConfig config, String moduleId) {
        return CooldownConfigManager.getModuleConfig(config, moduleId);
    }

    public static void setModuleConfig(String moduleId, ActivityConfig.ModuleConfigEntry entry) {
        CooldownConfigManager.setModuleConfig(moduleId, entry);
    }

    public static void setModuleConfig(ActivityConfig config, String moduleId, ActivityConfig.ModuleConfigEntry entry) {
        CooldownConfigManager.setModuleConfig(config, moduleId, entry);
    }

    public static boolean isModuleEnabled(String moduleId) {
        return CooldownConfigManager.isModuleEnabled(moduleId);
    }

    public static boolean isModuleEnabled(ActivityConfig config, String moduleId) {
        return CooldownConfigManager.isModuleEnabled(config, moduleId);
    }

    public static void setModuleEnabled(String moduleId, boolean enabled) {
        CooldownConfigManager.setModuleEnabled(moduleId, enabled);
    }

    public static void setModuleEnabled(ActivityConfig config, String moduleId, boolean enabled) {
        CooldownConfigManager.setModuleEnabled(config, moduleId, enabled);
    }

    public static ActivityConfig load() {
        return CooldownConfigManager.load();
    }

    public static boolean save() {
        return CooldownConfigManager.save();
    }

    public static void markDirty() {
        CooldownConfigManager.markDirty();
    }

    public static void resetDefaults() {
        CooldownConfigManager.resetDefaults();
    }

    public static boolean isLegacyMigrated() {
        return CooldownConfigManager.isLegacyMigrated();
    }

    public static boolean importLegacyConfigs() {
        return CooldownConfigManager.importLegacyConfigs();
    }

    public static void syncToModules(ActivityConfig config) {
        CooldownConfigManager.syncToModules(config);
    }

    public static void syncFromModules(ActivityConfig config) {
        CooldownConfigManager.syncFromModules(config);
    }

    public static void applyModuleSettings(activity.client.module.api.IModule module, Map<String, Object> settingsMap) {
        CooldownConfigManager.applyModuleSettings(module, settingsMap);
    }
}

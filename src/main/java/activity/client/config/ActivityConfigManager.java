package activity.client.config;

import activity.client.ActivityClient;
import activity.client.module.api.ModuleRegistry;
import activity.client.util.Obf;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public final class ActivityConfigManager {

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .serializeSpecialFloatingPointValues()
        .create();

    private static final Gson GSON_COMPACT = new GsonBuilder()
        .disableHtmlEscaping()
        .serializeSpecialFloatingPointValues()
        .create();

    private static Path resolveConfigDir() {
        try {
            net.fabricmc.loader.api.FabricLoader loader = FabricLoader.getInstance();
            if (loader != null && loader.getConfigDir() != null) {
                return loader.getConfigDir();
            }
        } catch (Throwable ignored) {
        }
        return Path.of("config");
    }

    private static Path resolveConfigPath() {
        return resolveConfigDir().resolve("cooldownhud.json");
    }

    private static Path resolveLegacyConfigPath() {
        return resolveConfigDir().resolve("activity.json");
    }

    private static final Path CONFIG_PATH = resolveConfigPath();

    private static ActivityConfig currentConfig = new ActivityConfig();
    private static ActivityConfig savedSnapshot = new ActivityConfig();
    private static boolean manualDirty = false;

    private ActivityConfigManager() {}

    public static synchronized ActivityConfig getConfig() {
        return currentConfig;
    }

    public static synchronized void setConfig(ActivityConfig config) {
        if (config == null) return;
        config.sanitize();
        currentConfig = config;
        ModuleRegistry.loadAll(currentConfig);
        CooldownConfigManager.syncFromModules(currentConfig);
        currentConfig.syncModuleConfigEntries();
        CooldownConfigManager.syncToModules(currentConfig);
        checkDirty();
    }

    public static synchronized boolean isDirty() {
        return manualDirty || !currentConfig.equals(savedSnapshot);
    }

    public static synchronized void markDirty() {
        manualDirty = true;
    }

    public static synchronized void clearDirty() {
        manualDirty = false;
        savedSnapshot = currentConfig.copy();
    }

    public static synchronized void resetDefaults() {
        currentConfig.resetToDefaults();
        ModuleRegistry.loadAll(currentConfig);
        CooldownConfigManager.syncToModules(currentConfig);
        checkDirty();
    }

    public static synchronized void applyPreset(String presetName) {
        if (presetName == null || presetName.isBlank()) return;
        currentConfig.applyPreset(presetName);
        ModuleRegistry.loadAll(currentConfig);
        markDirty();
        save();
    }

    public static synchronized String exportPresetString() {
        currentConfig.syncModuleConfigEntries();
        currentConfig.sanitize();
        JsonObject root = new JsonObject();
        root.addProperty("type", "cooldownhud_config");
        root.addProperty("configVersion", currentConfig.configVersion);
        root.addProperty("activeProfile", currentConfig.activeProfile != null ? currentConfig.activeProfile : "default");
        root.addProperty("payload", Obf.encrypt(GSON_COMPACT.toJson(currentConfig)));
        return GSON.toJson(root);
    }

    public static synchronized String exportPresetCompact() {
        return Base64.getEncoder().encodeToString(exportPresetString().getBytes(StandardCharsets.UTF_8));
    }

    public static synchronized boolean importPresetString(String data) {
        if (data == null || data.isBlank()) return false;
        String trimmed = data.trim();
        try {
            String jsonToParse = trimmed;
            if (!trimmed.startsWith("{")) {
                try {
                    byte[] decoded = Base64.getMimeDecoder().decode(trimmed);
                    jsonToParse = new String(decoded, StandardCharsets.UTF_8);
                } catch (Exception exMime) {
                    try {
                        byte[] decoded = Base64.getDecoder().decode(trimmed);
                        jsonToParse = new String(decoded, StandardCharsets.UTF_8);
                    } catch (Exception ignored) {
                    }
                }
            }

            if (jsonToParse.contains("\"payload\"")) {
                try {
                    JsonObject obj = JsonParser.parseString(jsonToParse).getAsJsonObject();
                    if (obj.has("payload")) {
                        String decrypted = Obf.decrypt(obj.get("payload").getAsString());
                        if (decrypted != null && !decrypted.isEmpty()) {
                            jsonToParse = decrypted;
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            ActivityConfig imported = GSON.fromJson(jsonToParse, ActivityConfig.class);
            if (imported == null) return false;
            if (jsonToParse.contains("\"client\"")) {
                imported.syncFromClientSection();
            }
            if (jsonToParse.contains("\"modules\"")) {
                imported.syncFromModuleEntries();
            }
            imported.sanitize();
            setConfig(imported);
            save();
            return true;
        } catch (Exception e) {
            ActivityClient.LOGGER.debug("[Activity] Failed to parse imported preset: {}", e.getMessage());
            return false;
        }
    }

    private static void checkDirty() {
        manualDirty = !currentConfig.equals(savedSnapshot);
    }

    public static synchronized ActivityConfig load() {
        Path configPath = resolveConfigPath();
        Path legacyPath = resolveLegacyConfigPath();

        if (!Files.exists(configPath)) {
            if (Files.exists(legacyPath)) {
                try {
                    String legacyJson = Files.readString(legacyPath, StandardCharsets.UTF_8);
                    ActivityConfig loaded = GSON.fromJson(legacyJson, ActivityConfig.class);
                    if (loaded != null) {
                        if (legacyJson.contains("\"client\"")) loaded.syncFromClientSection();
                        if (legacyJson.contains("\"modules\"")) loaded.syncFromModuleEntries();
                        loaded.sanitize();
                        activity.client.config.migration.LegacyConfigMigrator.checkAndMigrate(loaded);
                        currentConfig = loaded;
                        CooldownConfigManager.syncFromModules(currentConfig);
                        ModuleRegistry.loadAll(currentConfig);
                        CooldownConfigManager.syncToModules(currentConfig);
                        savedSnapshot = currentConfig.copy();
                        manualDirty = false;
                        save();
                        cleanLegacyFiles();
                        return currentConfig;
                    }
                } catch (Exception ignored) {
                }
            }

            currentConfig = new ActivityConfig();
            activity.client.config.migration.LegacyConfigMigrator.checkAndMigrate(currentConfig);
            ModuleRegistry.loadAll(currentConfig);
            CooldownConfigManager.syncToModules(currentConfig);
            savedSnapshot = currentConfig.copy();
            manualDirty = false;
            save();
            cleanLegacyFiles();
            return currentConfig;
        }

        try {
            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            ActivityConfig loaded = null;

            if (json.contains("\"profileData\"")) {
                try {
                    JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                    if (root.has("profileData")) {
                        String enc = root.get("profileData").getAsString();
                        String dec = Obf.decrypt(enc);
                        if (dec != null && !dec.isEmpty()) {
                            loaded = GSON.fromJson(dec, ActivityConfig.class);
                            if (dec.contains("\"client\"")) loaded.syncFromClientSection();
                            if (dec.contains("\"modules\"")) loaded.syncFromModuleEntries();
                        }
                    }
                    if (loaded != null && root.has("hud") && root.get("hud").isJsonObject()) {
                        JsonObject hud = root.getAsJsonObject("hud");
                        if (hud.has("enabled")) loaded.cooldownHudEnabled = hud.get("enabled").getAsBoolean();
                        if (hud.has("customX")) loaded.cooldownHudCustomX = hud.get("customX").getAsInt();
                        if (hud.has("customY")) loaded.cooldownHudCustomY = hud.get("customY").getAsInt();
                        if (hud.has("vertical")) loaded.cooldownHudVertical = hud.get("vertical").getAsBoolean();
                        if (hud.has("minDuration")) loaded.cooldownHudMinDuration = hud.get("minDuration").getAsDouble();
                        if (hud.has("activeProfile")) loaded.activeProfile = hud.get("activeProfile").getAsString();
                    }
                } catch (Exception ignored) {
                }
            }

            if (loaded == null) {
                loaded = GSON.fromJson(json, ActivityConfig.class);
                if (json.contains("\"client\"")) loaded.syncFromClientSection();
                if (json.contains("\"modules\"")) loaded.syncFromModuleEntries();
            }

            if (loaded == null) {
                throw new JsonParseException("Parsed configuration resulted in null object");
            }

            loaded.sanitize();
            activity.client.config.migration.LegacyConfigMigrator.checkAndMigrate(loaded);
            currentConfig = loaded;
            CooldownConfigManager.syncFromModules(currentConfig);
            ModuleRegistry.loadAll(currentConfig);
            CooldownConfigManager.syncToModules(currentConfig);
            savedSnapshot = currentConfig.copy();
            manualDirty = false;
            cleanLegacyFiles();
            return currentConfig;
        } catch (Exception e) {
            ActivityClient.LOGGER.debug("[Activity] Failed to parse configuration at {}: {}", configPath, e.getMessage());
            handleCorruptedConfig(e);
            cleanLegacyFiles();
            return currentConfig;
        }
    }

    public static synchronized boolean save() {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
            return false;
        }
        try {
            ModuleRegistry.saveAll(currentConfig);
            CooldownConfigManager.syncToModules(currentConfig);
            currentConfig.sanitize();

            JsonObject root = new JsonObject();
            root.addProperty("configVersion", currentConfig.configVersion);

            JsonObject hud = new JsonObject();
            hud.addProperty("enabled", currentConfig.cooldownHudEnabled);
            hud.addProperty("customX", currentConfig.cooldownHudCustomX);
            hud.addProperty("customY", currentConfig.cooldownHudCustomY);
            hud.addProperty("vertical", currentConfig.cooldownHudVertical);
            hud.addProperty("scale", 1.0);
            hud.addProperty("minDuration", currentConfig.cooldownHudMinDuration);
            hud.addProperty("opacity", 0.95);
            hud.addProperty("showLabels", true);
            hud.addProperty("activeProfile", currentConfig.activeProfile != null ? currentConfig.activeProfile : "По умолчанию");

            JsonArray items = new JsonArray();
            items.add("minecraft:ender_pearl");
            items.add("minecraft:wind_charge");
            items.add("minecraft:golden_apple");
            items.add("minecraft:enchanted_golden_apple");
            items.add("minecraft:totem_of_undying");
            items.add("minecraft:tnt_minecart");
            items.add("minecraft:respawn_anchor");
            items.add("minecraft:shield");
            items.add("minecraft:mace");
            items.add("minecraft:trident");
            hud.add("trackedItems", items);

            root.add("hud", hud);

            String fullJson = GSON_COMPACT.toJson(currentConfig);
            root.addProperty("profileData", Obf.encrypt(fullJson));

            String outputJson = GSON.toJson(root);

            Path configPath = resolveConfigPath();
            Path parentDir = configPath.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }

            Path tempPath = configPath.resolveSibling("cooldownhud.json.tmp");
            Files.writeString(tempPath, outputJson, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

            try {
                Files.move(tempPath, configPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException moveEx) {
                Files.move(tempPath, configPath, StandardCopyOption.REPLACE_EXISTING);
            }

            cleanLegacyFiles();

            savedSnapshot = currentConfig.copy();
            manualDirty = false;
            return true;
        } catch (IOException e) {
            ActivityClient.LOGGER.debug("[Activity] Failed to save configuration to {}: {}", CONFIG_PATH, e.getMessage());
            return false;
        }
    }

    private static void cleanLegacyFiles() {
        try {
            Path legacy = resolveLegacyConfigPath();
            if (Files.exists(legacy)) {
                Files.deleteIfExists(legacy);
            }
            Path dir = legacy.getParent();
            if (dir != null) {
                Files.deleteIfExists(dir.resolve("autogg.json"));
                Files.deleteIfExists(dir.resolve("autotool.json"));
                Files.deleteIfExists(dir.resolve("activity_presets.json"));
                Files.deleteIfExists(dir.resolve("activity.json.tmp"));
                Files.deleteIfExists(dir.resolve("activity_presets.json.tmp"));
            }
        } catch (Throwable ignored) {
        }
    }

    private static void handleCorruptedConfig(Exception cause) {
        try {
            Path configPath = resolveConfigPath();
            long timestamp = System.currentTimeMillis();
            Path backupPath = configPath.resolveSibling("cooldownhud.json.corrupted_" + timestamp + ".bak");
            Files.copy(configPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
        }

        currentConfig = new ActivityConfig();
        activity.client.config.migration.LegacyConfigMigrator.checkAndMigrate(currentConfig);
        ModuleRegistry.loadAll(currentConfig);
        NivoratConfigManager.syncToModules(currentConfig);
        save();
    }

    public static Path getConfigPath() {
        return resolveConfigPath();
    }

    public static synchronized void purgeForCapitulation() {
        currentConfig = new ActivityConfig();
        currentConfig.menuKeybind = new activity.client.module.keybind.Keybind();
        savedSnapshot = null;
        manualDirty = false;
    }
}

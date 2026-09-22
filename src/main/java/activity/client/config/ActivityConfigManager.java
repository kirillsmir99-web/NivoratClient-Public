package activity.client.config;

import activity.client.ActivityClient;
import activity.client.module.api.ModuleRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
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

    private static Path resolveConfigPath() {
        try {
            net.fabricmc.loader.api.FabricLoader loader = FabricLoader.getInstance();
            if (loader != null && loader.getConfigDir() != null) {
                return loader.getConfigDir().resolve("activity.json");
            }
        } catch (Throwable ignored) {
        }
        return Path.of("config", "activity.json");
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
        NivoratConfigManager.syncFromModules(currentConfig);
        currentConfig.syncModuleConfigEntries();
        NivoratConfigManager.syncToModules(currentConfig);
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
        NivoratConfigManager.syncToModules(currentConfig);
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
        return GSON.toJson(currentConfig);
    }

    public static synchronized String exportPresetCompact() {
        String json = exportPresetString();
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
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
                } catch (IllegalArgumentException exMime) {
                    try {
                        byte[] decoded = Base64.getDecoder().decode(trimmed);
                        jsonToParse = new String(decoded, StandardCharsets.UTF_8);
                    } catch (IllegalArgumentException ignored) {
                    }
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
        if (!Files.exists(CONFIG_PATH)) {
            ActivityClient.LOGGER.debug("[Activity] Config file not found at {}. Generating default configuration.", CONFIG_PATH);
            currentConfig = new ActivityConfig();
            activity.client.config.migration.LegacyConfigMigrator.checkAndMigrate(currentConfig);
            ModuleRegistry.loadAll(currentConfig);
            NivoratConfigManager.syncToModules(currentConfig);
            savedSnapshot = currentConfig.copy();
            manualDirty = false;
            save();
            return currentConfig;
        }

        try {
            String json = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
            ActivityConfig loaded = GSON.fromJson(json, ActivityConfig.class);

            if (loaded == null) {
                throw new JsonParseException("Parsed configuration resulted in null object");
            }

            if (json.contains("\"client\"")) {
                loaded.syncFromClientSection();
            }
            if (json.contains("\"modules\"")) {
                loaded.syncFromModuleEntries();
            }

            loaded.sanitize();
            activity.client.config.migration.LegacyConfigMigrator.checkAndMigrate(loaded);
            currentConfig = loaded;
            NivoratConfigManager.syncFromModules(currentConfig);
            ModuleRegistry.loadAll(currentConfig);
            NivoratConfigManager.syncToModules(currentConfig);
            savedSnapshot = currentConfig.copy();
            manualDirty = false;
            ActivityClient.LOGGER.debug("[Activity] Successfully loaded configuration from {}.", CONFIG_PATH);
            return currentConfig;
        } catch (Exception e) {
            ActivityClient.LOGGER.debug("[Activity] Failed to parse configuration at {}: {}", CONFIG_PATH, e.getMessage());
            handleCorruptedConfig(e);
            return currentConfig;
        }
    }

    public static synchronized boolean save() {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
            return false;
        }
        try {
            ModuleRegistry.saveAll(currentConfig);
            NivoratConfigManager.syncToModules(currentConfig);
            currentConfig.sanitize();
            String json = GSON.toJson(currentConfig);

            Path parentDir = CONFIG_PATH.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }

            Path tempPath = CONFIG_PATH.resolveSibling("activity.json.tmp");
            Files.writeString(tempPath, json, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

            try {
                Files.move(tempPath, CONFIG_PATH, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException moveEx) {

                Files.move(tempPath, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
            }

            savedSnapshot = currentConfig.copy();
            manualDirty = false;
            ActivityClient.LOGGER.debug("[Activity] Configuration atomically saved to {}.", CONFIG_PATH);
            return true;
        } catch (IOException e) {
            ActivityClient.LOGGER.debug("[Activity] Failed to save configuration to {}: {}", CONFIG_PATH, e.getMessage());
            return false;
        }
    }

    private static void handleCorruptedConfig(Exception cause) {
        try {
            long timestamp = System.currentTimeMillis();
            Path backupPath = CONFIG_PATH.resolveSibling("activity.json.corrupted_" + timestamp + ".bak");
            Files.copy(CONFIG_PATH, backupPath, StandardCopyOption.REPLACE_EXISTING);
            ActivityClient.LOGGER.debug("[Activity] Emergency backup of corrupted configuration saved to {}.", backupPath);
        } catch (IOException ioException) {
            ActivityClient.LOGGER.debug("[Activity] Could not create backup of corrupted configuration: {}", ioException.getMessage());
        }

        currentConfig = new ActivityConfig();
        activity.client.config.migration.LegacyConfigMigrator.checkAndMigrate(currentConfig);
        ModuleRegistry.loadAll(currentConfig);
        NivoratConfigManager.syncToModules(currentConfig);
        save();
        ActivityClient.LOGGER.debug("[Activity] Factory default configuration restored.");
    }

    public static Path getConfigPath() {
        return CONFIG_PATH;
    }

    public static synchronized void purgeForCapitulation() {
        currentConfig = new ActivityConfig();
        currentConfig.menuKeybind = new activity.client.module.keybind.Keybind();
        savedSnapshot = null;
        manualDirty = false;
    }
}

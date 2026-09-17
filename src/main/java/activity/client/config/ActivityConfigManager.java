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

/**
 * Thread-safe configuration manager for the Activity mod.
 *
 * <p>Key Invariants:
 * <ul>
 *   <li><b>Atomic disk writes</b>: Configuration is written to a temporary file before being atomically
 *       swapped, preventing zero-byte files and corruption during unexpected shutdowns.</li>
 *   <li><b>Corrupted JSON recovery</b>: When syntax corruption is detected, creates a timestamped backup
 *       copy and safely falls back to factory defaults without crashing the game.</li>
 *   <li><b>Dirty-state tracking</b>: Avoids redundant disk writes; persists only upon explicit user
 *       actions (Save / Apply / Reset All), screen exit, or shutdown. Zero frame-by-frame disk operations.</li>
 *   <li><b>Module synchronization</b>: Automatically synchronizes settings with {@link ModuleRegistry}
 *       on load, save, reset, and setConfig.</li>
 * </ul>
 */
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

    /**
     * @return the active in-memory configuration object
     */
    public static synchronized ActivityConfig getConfig() {
        return currentConfig;
    }

    /**
     * Replaces the in-memory configuration and synchronizes with registered modules.
     *
     * @param config new configuration
     */
    public static synchronized void setConfig(ActivityConfig config) {
        if (config == null) return;
        config.sanitize();
        currentConfig = config;
        ModuleRegistry.loadAll(currentConfig);
        checkDirty();
    }

    /**
     * @return true if there are unsaved in-memory changes compared to the last disk snapshot
     */
    public static synchronized boolean isDirty() {
        return manualDirty || !currentConfig.equals(savedSnapshot);
    }

    /**
     * Marks the configuration as dirty, indicating unsaved changes exist.
     */
    public static synchronized void markDirty() {
        manualDirty = true;
    }

    /**
     * Clears the dirty flag.
     */
    public static synchronized void clearDirty() {
        manualDirty = false;
        savedSnapshot = currentConfig.copy();
    }

    /**
     * Resets the active configuration to factory defaults and synchronizes all modules.
     */
    public static synchronized void resetDefaults() {
        currentConfig.resetToDefaults();
        ModuleRegistry.loadAll(currentConfig);
        checkDirty();
    }

    /**
     * Applies a named preset, updates module registry, and saves configuration.
     *
     * @param presetName name of preset ("default", "legit", "rage", "utility")
     */
    public static synchronized void applyPreset(String presetName) {
        if (presetName == null || presetName.isBlank()) return;
        currentConfig.applyPreset(presetName);
        ModuleRegistry.loadAll(currentConfig);
        markDirty();
        save();
    }

    /**
     * Exports the active configuration as a formatted JSON string.
     */
    public static synchronized String exportPresetString() {
        return GSON.toJson(currentConfig);
    }

    /**
     * Exports the active configuration as a compact Base64 encoded string for easy sharing.
     */
    public static synchronized String exportPresetCompact() {
        String json = GSON.toJson(currentConfig);
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Imports configuration from a JSON or Base64 string.
     *
     * @param data JSON or Base64 string
     * @return true if successfully imported and applied
     */
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
            imported.sanitize();
            setConfig(imported);
            save();
            return true;
        } catch (Exception e) {
            ActivityClient.LOGGER.error("[Activity] Failed to parse imported preset: {}", e.getMessage());
            return false;
        }
    }

    private static void checkDirty() {
        manualDirty = !currentConfig.equals(savedSnapshot);
    }

    /**
     * Loads the configuration from disk into memory.
     *
     * <p>If the configuration file does not exist, a default file is created atomically.
     * If the file is corrupted, an emergency timestamped backup is made and defaults are restored.
     *
     * @return the loaded or restored configuration
     */
    public static synchronized ActivityConfig load() {
        if (!Files.exists(CONFIG_PATH)) {
            ActivityClient.LOGGER.info("[Activity] Config file not found at {}. Generating default configuration.", CONFIG_PATH);
            currentConfig = new ActivityConfig();
            ModuleRegistry.loadAll(currentConfig);
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

            loaded.sanitize();
            currentConfig = loaded;
            ModuleRegistry.loadAll(currentConfig);
            savedSnapshot = loaded.copy();
            manualDirty = false;
            ActivityClient.LOGGER.info("[Activity] Successfully loaded configuration from {}.", CONFIG_PATH);
            return currentConfig;
        } catch (Exception e) {
            ActivityClient.LOGGER.error("[Activity] Failed to parse configuration at {}: {}", CONFIG_PATH, e.getMessage());
            handleCorruptedConfig(e);
            return currentConfig;
        }
    }

    /**
     * Atomically saves the current in-memory configuration to disk.
     *
     * @return true if saved successfully, false otherwise
     */
    public static synchronized boolean save() {
        try {
            ModuleRegistry.saveAll(currentConfig);
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
                // Fallback for filesystems that do not support atomic move
                Files.move(tempPath, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
            }

            savedSnapshot = currentConfig.copy();
            manualDirty = false;
            ActivityClient.LOGGER.debug("[Activity] Configuration atomically saved to {}.", CONFIG_PATH);
            return true;
        } catch (IOException e) {
            ActivityClient.LOGGER.error("[Activity] Failed to save configuration to {}: {}", CONFIG_PATH, e.getMessage());
            return false;
        }
    }

    /**
     * Emergency handler for corrupted configuration files.
     * Preserves user data by making a backup copy before resetting to clean defaults.
     */
    private static void handleCorruptedConfig(Exception cause) {
        try {
            long timestamp = System.currentTimeMillis();
            Path backupPath = CONFIG_PATH.resolveSibling("activity.json.corrupted_" + timestamp + ".bak");
            Files.copy(CONFIG_PATH, backupPath, StandardCopyOption.REPLACE_EXISTING);
            ActivityClient.LOGGER.warn("[Activity] Emergency backup of corrupted configuration saved to {}.", backupPath);
        } catch (IOException ioException) {
            ActivityClient.LOGGER.error("[Activity] Could not create backup of corrupted configuration: {}", ioException.getMessage());
        }

        currentConfig = new ActivityConfig();
        ModuleRegistry.loadAll(currentConfig);
        save();
        ActivityClient.LOGGER.info("[Activity] Factory default configuration restored.");
    }

    public static Path getConfigPath() {
        return CONFIG_PATH;
    }
}

package activity.client.config.preset;

import activity.client.ActivityClient;
import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.module.api.ModuleRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Centralized manager and repository for custom presets in NivoratClient.
 *
 * <p>Key Guarantees:
 * <ul>
 *   <li>Single built-in preset: "По умолчанию" ("default"), which always exists, cannot be deleted,
 *       and resets all module parameters to factory defaults.</li>
 *   <li>Safe atomic disk persistence: presets are stored in {@code activity_presets.json} using
 *       temporary swap files to prevent corruption.</li>
 *   <li>Zero per-frame disk operations: files are written only when creating, overwriting, deleting,
 *       or importing a preset.</li>
 * </ul>
 */
public final class PresetManager {

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .serializeSpecialFloatingPointValues()
        .create();

    private static Path resolvePresetsPath() {
        try {
            FabricLoader loader = FabricLoader.getInstance();
            if (loader != null && loader.getConfigDir() != null) {
                return loader.getConfigDir().resolve("activity_presets.json");
            }
        } catch (Throwable ignored) {
        }
        return Path.of("config", "activity_presets.json");
    }

    private static final Path PRESETS_PATH = resolvePresetsPath();

    private static final Preset DEFAULT_PRESET = Preset.createDefault(PresetSerializer.extractSettingsSnapshot(new ActivityConfig()));

    private static final List<Preset> customPresets = new CopyOnWriteArrayList<>();
    private static boolean initialized = false;

    static {
        loadAll();
    }

    private PresetManager() {}

    /**
     * Returns the immutable built-in default preset.
     */
    public static Preset getDefaultPreset() {
        return DEFAULT_PRESET;
    }

    /**
     * Returns an unmodifiable list of all presets, with "По умолчанию" always at index 0.
     */
    public static synchronized List<Preset> getPresets() {
        ensureInitialized();
        List<Preset> all = new ArrayList<>();
        all.add(DEFAULT_PRESET);
        all.addAll(customPresets);
        return Collections.unmodifiableList(all);
    }

    /**
     * Looks up a preset by its unique ID.
     */
    public static synchronized Preset getPresetById(String id) {
        if (id == null || id.isBlank() || Preset.DEFAULT_PRESET_ID.equalsIgnoreCase(id) || "По умолчанию".equalsIgnoreCase(id)) {
            return DEFAULT_PRESET;
        }
        ensureInitialized();
        for (Preset p : customPresets) {
            if (p.getId().equalsIgnoreCase(id)) {
                return p;
            }
        }
        // Fallback: lookup by name
        return getPresetByName(id);
    }

    /**
     * Looks up a preset by its display name.
     */
    public static synchronized Preset getPresetByName(String name) {
        if (name == null || name.isBlank() || Preset.DEFAULT_PRESET_NAME.equalsIgnoreCase(name) || "default".equalsIgnoreCase(name)) {
            return DEFAULT_PRESET;
        }
        ensureInitialized();
        for (Preset p : customPresets) {
            if (p.getName().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    /**
     * @return true if a preset with the given name already exists (including default).
     */
    public static synchronized boolean hasPresetNamed(String name) {
        if (name == null) return false;
        String trimmed = name.trim();
        if (Preset.DEFAULT_PRESET_NAME.equalsIgnoreCase(trimmed) || "default".equalsIgnoreCase(trimmed)) {
            return true;
        }
        ensureInitialized();
        for (Preset p : customPresets) {
            if (p.getName().equalsIgnoreCase(trimmed)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates and persists a new custom preset from the current configuration.
     *
     * @param name   preset display name
     * @param config source configuration to snapshot
     * @return newly created preset
     * @throws IllegalArgumentException if name is empty, too long, or contains control characters
     */
    public static synchronized Preset createPreset(String name, ActivityConfig config) {
        String cleanName = validatePresetName(name);
        JsonObject snapshot = PresetSerializer.extractSettingsSnapshot(config != null ? config : ActivityConfigManager.getConfig());

        Preset preset = Preset.createCustom(cleanName, snapshot);
        customPresets.add(preset);
        saveAll();
        return preset;
    }

    /**
     * Overwrites an existing custom preset's settings with the current configuration snapshot.
     */
    public static synchronized void overwritePreset(Preset existing, ActivityConfig config) {
        if (existing == null || existing.isBuiltin()) return;
        JsonObject snapshot = PresetSerializer.extractSettingsSnapshot(config != null ? config : ActivityConfigManager.getConfig());
        existing.setSettings(snapshot);
        existing.setUpdatedAt(System.currentTimeMillis());
        saveAll();
    }

    /**
     * Overwrites an existing custom preset's settings with a specific JsonObject snapshot.
     */
    public static synchronized void overwritePresetWithSettings(Preset existing, JsonObject settings) {
        if (existing == null || existing.isBuiltin()) return;
        existing.setSettings(settings);
        existing.setUpdatedAt(System.currentTimeMillis());
        saveAll();
    }

    /**
     * Adds an imported preset. If a preset with the same name exists and overwrite is false,
     * generates a unique name (e.g., "Name (1)").
     */
    public static synchronized Preset addOrOverwriteImported(Preset imported, boolean overwrite) {
        if (imported == null) return DEFAULT_PRESET;

        Preset existing = getPresetByName(imported.getName());
        if (existing != null && !existing.isBuiltin() && overwrite) {
            existing.setSettings(imported.getSettings());
            existing.setUpdatedAt(System.currentTimeMillis());
            saveAll();
            return existing;
        }

        String finalName = imported.getName();
        if (hasPresetNamed(finalName)) {
            int counter = 1;
            while (hasPresetNamed(finalName + " (" + counter + ")")) {
                counter++;
            }
            finalName = finalName + " (" + counter + ")";
        }

        Preset newPreset = Preset.createCustom(finalName, imported.getSettings());
        customPresets.add(newPreset);
        saveAll();
        return newPreset;
    }

    /**
     * Deletes a custom preset by ID. Built-in default preset cannot be deleted.
     *
     * @param presetId ID of preset to delete
     * @return true if deleted, false if not found or if built-in
     */
    public static synchronized boolean deletePreset(String presetId) {
        if (presetId == null || Preset.DEFAULT_PRESET_ID.equalsIgnoreCase(presetId)) {
            return false;
        }
        ensureInitialized();
        boolean removed = customPresets.removeIf(p -> p.getId().equalsIgnoreCase(presetId));
        if (removed) {
            saveAll();
            // If active profile matches deleted preset, revert to default
            ActivityConfig cfg = ActivityConfigManager.getConfig();
            if (cfg != null && (presetId.equalsIgnoreCase(cfg.activeProfile) || !hasPresetNamed(cfg.activeProfile))) {
                cfg.activeProfile = Preset.DEFAULT_PRESET_ID;
                ActivityConfigManager.markDirty();
                ActivityConfigManager.save();
            }
        }
        return removed;
    }

    /**
     * Applies a preset to the given configuration instance.
     * Synchronizes module registry and saves to disk.
     */
    public static synchronized void applyPreset(Preset preset, ActivityConfig target) {
        if (preset == null || target == null) return;

        if (preset.isBuiltin()) {
            // Restore factory defaults while keeping window coordinates and maximize state
            int origX = target.windowPosX;
            int origY = target.windowPosY;
            int origW = target.windowWidth;
            int origH = target.windowHeight;
            boolean origMax = target.windowMaximized;
            int origUnmaxX = target.unmaximizedX;
            int origUnmaxY = target.unmaximizedY;
            int origUnmaxW = target.unmaximizedWidth;
            int origUnmaxH = target.unmaximizedHeight;
            ActivityConfig freshDefaults = new ActivityConfig();
            PresetSerializer.applySettingsSnapshot(PresetSerializer.extractSettingsSnapshot(freshDefaults), target);
            target.windowPosX = origX;
            target.windowPosY = origY;
            target.windowWidth = origW;
            target.windowHeight = origH;
            target.windowMaximized = origMax;
            target.unmaximizedX = origUnmaxX;
            target.unmaximizedY = origUnmaxY;
            target.unmaximizedWidth = origUnmaxW;
            target.unmaximizedHeight = origUnmaxH;
            target.activeProfile = Preset.DEFAULT_PRESET_ID;
        } else {
            PresetSerializer.applySettingsSnapshot(preset.getSettings(), target);
            target.activeProfile = preset.getName();
        }

        ModuleRegistry.loadAll(target);
        ActivityConfigManager.markDirty();
        ActivityConfigManager.save();
    }

    /**
     * Applies a preset by name or ID to the active global configuration.
     */
    public static synchronized void applyPresetByName(String nameOrId, ActivityConfig target) {
        Preset preset = getPresetById(nameOrId);
        if (preset == null) {
            preset = getPresetByName(nameOrId);
        }
        if (preset == null) {
            preset = DEFAULT_PRESET;
        }
        applyPreset(preset, target);
    }

    /**
     * Validates a candidate preset name.
     */
    public static String validatePresetName(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Название пресета не может быть пустым");
        }
        String trimmed = raw.trim();
        if (trimmed.length() > 32) {
            throw new IllegalArgumentException("Название пресета не может превышать 32 символа");
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (Character.isISOControl(c) || c == '\n' || c == '\r' || c == '\t') {
                throw new IllegalArgumentException("Название пресета содержит недопустимые управляющие символы");
            }
        }
        return trimmed;
    }

    private static void ensureInitialized() {
        if (!initialized) {
            loadAll();
        }
    }

    public static synchronized void loadAll() {
        initialized = true;
        customPresets.clear();

        if (!Files.exists(PRESETS_PATH)) {
            return;
        }

        try {
            String json = Files.readString(PRESETS_PATH, StandardCharsets.UTF_8);
            if (json == null || json.isBlank()) return;

            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonArray()) return;

            JsonArray array = parsed.getAsJsonArray();
            for (JsonElement elem : array) {
                if (!elem.isJsonObject()) continue;
                JsonObject obj = elem.getAsJsonObject();

                String id = obj.has("id") ? obj.get("id").getAsString() : null;
                String name = obj.has("name") ? obj.get("name").getAsString() : null;
                if (id == null || name == null || Preset.DEFAULT_PRESET_ID.equalsIgnoreCase(id)) {
                    continue;
                }

                long createdAt = obj.has("createdAt") ? obj.get("createdAt").getAsLong() : System.currentTimeMillis();
                long updatedAt = obj.has("updatedAt") ? obj.get("updatedAt").getAsLong() : createdAt;
                int schema = obj.has("schemaVersion") ? obj.get("schemaVersion").getAsInt() : Preset.CURRENT_SCHEMA_VERSION;
                String clientVer = obj.has("clientVersion") ? obj.get("clientVersion").getAsString() : Preset.CURRENT_CLIENT_VERSION;
                JsonObject settings = obj.has("settings") && obj.get("settings").isJsonObject() ? obj.getAsJsonObject("settings") : new JsonObject();

                customPresets.add(new Preset(id, name, createdAt, updatedAt, schema, clientVer, false, settings));
            }
        } catch (Exception e) {
            ActivityClient.LOGGER.debug("[NivoratClient] Failed to load custom presets from {}: {}", PRESETS_PATH, e.getMessage());
        }
    }

    public static synchronized void saveAll() {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
            return;
        }
        try {
            Path parent = PRESETS_PATH.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            JsonArray array = new JsonArray();
            for (Preset p : customPresets) {
                if (p.isBuiltin()) continue;
                JsonObject obj = new JsonObject();
                obj.addProperty("id", p.getId());
                obj.addProperty("name", p.getName());
                obj.addProperty("createdAt", p.getCreatedAt());
                obj.addProperty("updatedAt", p.getUpdatedAt());
                obj.addProperty("schemaVersion", p.getSchemaVersion());
                obj.addProperty("clientVersion", p.getClientVersion());
                obj.add("settings", p.getSettings());
                array.add(obj);
            }

            String json = GSON.toJson(array);
            Path tempPath = PRESETS_PATH.resolveSibling(PRESETS_PATH.getFileName().toString() + ".tmp");
            Files.writeString(tempPath, json, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

            try {
                Files.move(tempPath, PRESETS_PATH, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempPath, PRESETS_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            ActivityClient.LOGGER.debug("[NivoratClient] Failed to save custom presets to {}: {}", PRESETS_PATH, e.getMessage());
        }
    }

    /**
     * Clears custom presets in memory and optionally on disk (used in tests).
     */
    public static synchronized void resetToDefaults() {
        customPresets.clear();
        saveAll();
    }
}

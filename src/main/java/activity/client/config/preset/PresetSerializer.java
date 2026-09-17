package activity.client.config.preset;

import activity.client.config.ActivityConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.Set;

/**
 * Serializer and validator for presets, handling JSON formatting for both
 * clipboard interchange and local disk storage.
 *
 * <p>Strictly excludes transient UI state (window coordinates, current search, active profile)
 * and verifies schema version and bounds during import.
 */
public final class PresetSerializer {

    public static final int MAX_IMPORT_BYTES = 65536; // 64 KB safety limit

    private static final Set<String> TRANSIENT_FIELDS = Set.of(
        "windowPosX",
        "windowPosY",
        "windowWidth",
        "windowHeight",
        "windowMaximized",
        "unmaximizedX",
        "unmaximizedY",
        "unmaximizedWidth",
        "unmaximizedHeight",
        "searchFilter",
        "filterCategory",
        "matchCase",
        "activeProfile"
    );

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .serializeSpecialFloatingPointValues()
        .create();

    private static final Gson GSON_COMPACT = new GsonBuilder()
        .disableHtmlEscaping()
        .serializeSpecialFloatingPointValues()
        .create();

    private PresetSerializer() {}

    /**
     * Extracts a clean snapshot of module and gameplay settings from the provided config,
     * removing transient UI state.
     */
    public static JsonObject extractSettingsSnapshot(ActivityConfig config) {
        if (config == null) return new JsonObject();
        JsonElement tree = GSON.toJsonTree(config);
        if (!tree.isJsonObject()) return new JsonObject();

        JsonObject obj = tree.getAsJsonObject();
        for (String transientField : TRANSIENT_FIELDS) {
            obj.remove(transientField);
        }
        return obj;
    }

    /**
     * Applies snapshot settings onto a target configuration instance, preserving
     * existing transient UI state (window position, active profile, search filter).
     */
    public static void applySettingsSnapshot(JsonObject snapshot, ActivityConfig target) {
        if (snapshot == null || target == null) return;

        // Preserve transient UI state
        int origX = target.windowPosX;
        int origY = target.windowPosY;
        int origW = target.windowWidth;
        int origH = target.windowHeight;
        boolean origMax = target.windowMaximized;
        int origUnmaxX = target.unmaximizedX;
        int origUnmaxY = target.unmaximizedY;
        int origUnmaxW = target.unmaximizedWidth;
        int origUnmaxH = target.unmaximizedHeight;
        String origSearch = target.searchFilter;
        String origCat = target.filterCategory;
        boolean origMatchCase = target.matchCase;
        String origProfile = target.activeProfile;

        try {
            ActivityConfig deserialized = GSON.fromJson(snapshot, ActivityConfig.class);
            if (deserialized != null) {
                // Copy all module and gameplay settings
                copySettings(deserialized, target);
            }
        } catch (Exception ignored) {
        } finally {
            // Restore transient UI state
            target.windowPosX = origX;
            target.windowPosY = origY;
            target.windowWidth = origW;
            target.windowHeight = origH;
            target.windowMaximized = origMax;
            target.unmaximizedX = origUnmaxX;
            target.unmaximizedY = origUnmaxY;
            target.unmaximizedWidth = origUnmaxW;
            target.unmaximizedHeight = origUnmaxH;
            target.searchFilter = origSearch;
            target.filterCategory = origCat;
            target.matchCase = origMatchCase;
            target.activeProfile = origProfile;
        }
    }

    private static void copySettings(ActivityConfig src, ActivityConfig dst) {
        // Combat
        dst.autoMaceEnabled = src.autoMaceEnabled;
        dst.autoMaceKeybind.copyFrom(src.autoMaceKeybind);
        dst.autoMaceSourceMode = src.autoMaceSourceMode;
        dst.autoMaceEnchantMode = src.autoMaceEnchantMode;
        dst.autoMaceRestoreDelayMs = src.autoMaceRestoreDelayMs;
        dst.autoMaceLegitMode = src.autoMaceLegitMode;
        dst.autoMaceMissChance = src.autoMaceMissChance;

        dst.autoSpearEnabled = src.autoSpearEnabled;
        dst.autoSpearKeybind.copyFrom(src.autoSpearKeybind);
        dst.autoSpearRestoreDelayMs = src.autoSpearRestoreDelayMs;

        dst.autoShieldbreakerEnabled = src.autoShieldbreakerEnabled;
        dst.autoShieldbreakerKeybind.copyFrom(src.autoShieldbreakerKeybind);
        dst.autoShieldbreakerMode = src.autoShieldbreakerMode;
        dst.autoShieldbreakerDistance = src.autoShieldbreakerDistance;
        dst.autoShieldbreakerChance = src.autoShieldbreakerChance;
        dst.autoShieldbreakerSwitchDelayMs = src.autoShieldbreakerSwitchDelayMs;
        dst.autoShieldbreakerRestoreDelayMs = src.autoShieldbreakerRestoreDelayMs;
        dst.autoShieldbreakerLegitMode = src.autoShieldbreakerLegitMode;

        dst.autoStunSlamEnabled = src.autoStunSlamEnabled;
        dst.autoStunSlamKeybind.copyFrom(src.autoStunSlamKeybind);
        dst.autoStunSlamMode = src.autoStunSlamMode;
        dst.autoStunSlamDistance = src.autoStunSlamDistance;
        dst.autoStunSlamChance = src.autoStunSlamChance;
        dst.autoStunSlamAxeDelayMs = src.autoStunSlamAxeDelayMs;
        dst.autoStunSlamMaceDelayMs = src.autoStunSlamMaceDelayMs;
        dst.autoStunSlamRestoreDelayMs = src.autoStunSlamRestoreDelayMs;
        dst.autoStunSlamLegitMode = src.autoStunSlamLegitMode;

        if (src.pinnedModules != null) {
            dst.pinnedModules = new java.util.ArrayList<>(src.pinnedModules);
        }

        // Defense
        dst.autoTotemEnabled = src.autoTotemEnabled;
        dst.autoTotemKeybind.copyFrom(src.autoTotemKeybind);
        dst.autoTotemTriggerHearts = src.autoTotemTriggerHearts;
        dst.autoTotemRestoreHearts = src.autoTotemRestoreHearts;
        dst.autoTotemChance = src.autoTotemChance;
        dst.autoTotemReturnItem = src.autoTotemReturnItem;
        dst.autoTotemReturnOnPop = src.autoTotemReturnOnPop;

        dst.autoCartEnabled = src.autoCartEnabled;
        dst.autoCartKeybind.copyFrom(src.autoCartKeybind);
        dst.autoCartPlacementChance = src.autoCartPlacementChance;
        dst.autoCartRailDelay = src.autoCartRailDelay;
        dst.autoCartCartDelay = src.autoCartCartDelay;
        dst.autoCartRestoreDelay = src.autoCartRestoreDelay;
        dst.autoCartLegitMode = src.autoCartLegitMode;

        dst.autoAnchorEnabled = src.autoAnchorEnabled;
        dst.autoAnchorKeybind.copyFrom(src.autoAnchorKeybind);
        dst.autoAnchorAutoExplode = src.autoAnchorAutoExplode;
        dst.autoAnchorAutoReturn = src.autoAnchorAutoReturn;
        dst.autoAnchorChargeDelay = src.autoAnchorChargeDelay;
        dst.autoAnchorChance = src.autoAnchorChance;
        dst.autoAnchorLegitMode = src.autoAnchorLegitMode;

        dst.cartRefillEnabled = src.cartRefillEnabled;
        dst.cartRefillKeybind.copyFrom(src.cartRefillKeybind);
        dst.cartRefillDelayTicks = src.cartRefillDelayTicks;
        dst.cartRefillChance = src.cartRefillChance;
        dst.cartRefillLegitMode = src.cartRefillLegitMode;
        dst.cartRefillAutoClose = src.cartRefillAutoClose;

        // Utility
        dst.hpReaperEnabled = src.hpReaperEnabled;
        dst.hpReaperKeybind.copyFrom(src.hpReaperKeybind);
        dst.hpReaperMode = src.hpReaperMode;

        dst.autoToolEnabled = src.autoToolEnabled;
        dst.autoToolKeybind.copyFrom(src.autoToolKeybind);
        dst.autoToolCombatGuard = src.autoToolCombatGuard;
        dst.autoToolDurabilitySaver = src.autoToolDurabilitySaver;
        dst.autoToolDurabilityThreshold = src.autoToolDurabilityThreshold;
        dst.autoToolPreferSilkTouch = src.autoToolPreferSilkTouch;
        dst.autoToolRestorePrevious = src.autoToolRestorePrevious;

        dst.autoGGEnabled = src.autoGGEnabled;
        dst.autoGGKeybind.copyFrom(src.autoGGKeybind);
        dst.autoGGPhrase = src.autoGGPhrase;
        dst.autoGGSendOnOwnDeath = src.autoGGSendOnOwnDeath;

        // Visuals & Themes
        dst.overlayEnabled = src.overlayEnabled;
        dst.darkThemeEnabled = src.darkThemeEnabled;
        dst.hudPosition = src.hudPosition;
        dst.overlayOpacity = src.overlayOpacity;
        dst.autoHideOnChat = src.autoHideOnChat;
        dst.hideInF3 = src.hideInF3;
        dst.showCoordinates = src.showCoordinates;
        dst.showFps = src.showFps;
        dst.showBiome = src.showBiome;
        dst.showWorldTime = src.showWorldTime;
        dst.showDirection = src.showDirection;
        dst.coordFormat = src.coordFormat;
        dst.hudPadding = src.hudPadding;
        dst.customTitle = src.customTitle;
        dst.textShadow = src.textShadow;
        dst.themeVariant = src.themeVariant;
        dst.compactMode = src.compactMode;
        dst.tooltipsEnabled = src.tooltipsEnabled;
        dst.showKeyHints = src.showKeyHints;
        dst.smoothTransitions = src.smoothTransitions;
        dst.soundVolume = src.soundVolume;
        dst.audioClicks = src.audioClicks;
        dst.fontFamily = src.fontFamily;
        dst.typographySize = src.typographySize;
        dst.windowOpacity = src.windowOpacity;
        dst.panelOpacity = src.panelOpacity;
        dst.glassEffect = src.glassEffect;
        dst.soundEnabled = src.soundEnabled;
        dst.sliderSoundEnabled = src.sliderSoundEnabled;
        dst.animationsEnabled = src.animationsEnabled;
        dst.spatialOpenAnimation = src.spatialOpenAnimation;
    }

    /**
     * Serializes a preset for clipboard copying in the required schema:
     * <pre>
     * {
     *   "schemaVersion": 1,
     *   "clientVersion": "v1.0.0",
     *   "presetName": "...",
     *   "settings": { ... }
     * }
     * </pre>
     */
    public static String toClipboardJson(Preset preset) {
        if (preset == null) return "";

        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", preset.getSchemaVersion());
        root.addProperty("clientVersion", preset.getClientVersion());
        root.addProperty("presetName", preset.getName());
        root.add("settings", preset.getSettings() != null ? preset.getSettings() : new JsonObject());

        return GSON.toJson(root);
    }

    /**
     * Validates and deserializes a clipboard JSON string into a new {@link Preset} instance.
     *
     * @param raw clipboard text
     * @return deserialized preset
     * @throws PresetValidationException if text is null, oversized, invalid JSON, or missing required fields
     */
    public static Preset fromClipboardJson(String raw) throws PresetValidationException {
        if (raw == null || raw.isBlank()) {
            throw new PresetValidationException("Буфер обмена пуст");
        }

        String trimmed = raw.trim();
        if (trimmed.length() > MAX_IMPORT_BYTES) {
            throw new PresetValidationException("Размер пресета превышает допустимый лимит (64 КБ)");
        }

        JsonElement parsed;
        try {
            parsed = JsonParser.parseString(trimmed);
        } catch (JsonParseException e) {
            throw new PresetValidationException("Некорректный синтаксис JSON: " + e.getMessage());
        }

        if (!parsed.isJsonObject()) {
            throw new PresetValidationException("Содержимое буфера не является JSON-объектом");
        }

        JsonObject obj = parsed.getAsJsonObject();

        // 1. Validate schemaVersion
        int schema = Preset.CURRENT_SCHEMA_VERSION;
        if (obj.has("schemaVersion")) {
            try {
                schema = obj.get("schemaVersion").getAsInt();
            } catch (Exception e) {
                throw new PresetValidationException("Недопустимый формат schemaVersion");
            }
        }
        if (schema < 1 || schema > Preset.CURRENT_SCHEMA_VERSION) {
            throw new PresetValidationException("Неподдерживаемая версия схемы пресета: " + schema);
        }

        // 2. Validate clientVersion
        String clientVer = Preset.CURRENT_CLIENT_VERSION;
        if (obj.has("clientVersion")) {
            clientVer = obj.get("clientVersion").getAsString();
        }

        // 3. Validate presetName
        String name = null;
        if (obj.has("presetName")) {
            name = obj.get("presetName").getAsString();
        } else if (obj.has("name")) {
            name = obj.get("name").getAsString();
        }
        if (name == null || name.trim().isEmpty()) {
            throw new PresetValidationException("Отсутствует имя пресета (presetName)");
        }
        name = name.trim();
        if (name.length() > 32) {
            name = name.substring(0, 32);
        }

        // 4. Validate settings object
        if (!obj.has("settings") || !obj.get("settings").isJsonObject()) {
            // Check if root itself is a legacy config export
            if (obj.has("autoMaceEnabled") || obj.has("configVersion")) {
                JsonObject legacySettings = obj.deepCopy();
                for (String tf : TRANSIENT_FIELDS) {
                    legacySettings.remove(tf);
                }
                return Preset.createCustom(name, legacySettings);
            }
            throw new PresetValidationException("Отсутствует блок настроек (settings)");
        }

        JsonObject settings = obj.getAsJsonObject("settings").deepCopy();
        for (String tf : TRANSIENT_FIELDS) {
            settings.remove(tf);
        }

        return Preset.createCustom(name, settings);
    }

    public static class PresetValidationException extends Exception {
        public PresetValidationException(String message) {
            super(message);
        }
    }
}

package activity.client.config;

import activity.client.module.keybind.Keybind;

import java.util.Objects;

/**
 * Data model representing all configurable settings across the Activity GUI framework.
 *
 * <p>Contains configuration parameters for Combat, Defense, Utility, HUD, Profiles, and Engine settings.
 * Designed for safe JSON serialization via Gson, dirty tracking via {@link #equals(Object)},
 * and deep cloning via {@link #copy()}.
 */
public class ActivityConfig {

    public static final String PRESET_DEFAULT = "default";

    public int configVersion = 2;

    // ==========================================
    // 0. QUICK ACCESS & PINNED MODULES
    // ==========================================
    public java.util.List<String> pinnedModules = new java.util.ArrayList<>();

    public boolean isPinned(String moduleId) {
        if (this.pinnedModules == null || moduleId == null) return false;
        if ("auto_stun_slime".equals(moduleId) || "auto_stun_slam".equals(moduleId)) {
            return this.pinnedModules.contains("auto_stun_slam") || this.pinnedModules.contains("auto_stun_slime");
        }
        return this.pinnedModules.contains(moduleId);
    }

    public void setPinned(String moduleId, boolean pinned) {
        if (this.pinnedModules == null) {
            this.pinnedModules = new java.util.ArrayList<>();
        }
        if (moduleId == null) return;
        String canonical = "auto_stun_slime".equals(moduleId) ? "auto_stun_slam" : moduleId;
        if (pinned) {
            if (!this.pinnedModules.contains(canonical)) {
                this.pinnedModules.add(canonical);
            }
        } else {
            this.pinnedModules.remove(canonical);
            if ("auto_stun_slam".equals(canonical)) {
                this.pinnedModules.remove("auto_stun_slime");
            }
        }
    }

    public java.util.List<String> getPinnedModules() {
        if (this.pinnedModules == null) {
            this.pinnedModules = new java.util.ArrayList<>();
        }
        return java.util.Collections.unmodifiableList(this.pinnedModules);
    }

    // ==========================================
    // 1. COMBAT MODULES (Оружие и свапы)
    // ==========================================
    // AutoMace
    public boolean autoMaceEnabled = true;
    public Keybind autoMaceKeybind = new Keybind();
    public String autoMaceSourceMode = "sword_and_axe";
    public String autoMaceEnchantMode = "smart";
    public String autoMaceMissBehavior = "sword_hit";
    public double autoMaceRestoreDelayMs = 90.0;
    public boolean autoMaceLegitMode = true;
    public double autoMaceMissChance = 10.0;
    public boolean autoMaceRandomDelay = true;

    // AutoSpear
    public boolean autoSpearEnabled = true;
    public Keybind autoSpearKeybind = new Keybind();
    public Keybind autoSpearTriggerKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_TAB);
    public String autoSpearSecurityMode = "legit";
    public String autoSpearPriorityMode = "auto";
    public double autoSpearRestoreDelayMs = 185.0;
    public double autoSpearMissChance = 0.0;
    public boolean autoSpearRandomDelay = true;

    // AutoShieldbreaker
    public boolean autoShieldbreakerEnabled = true;
    public Keybind autoShieldbreakerKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_J, true, true, false);
    public String autoShieldbreakerMode = "full_auto";
    public double autoShieldbreakerDistance = 2.85;
    public double autoShieldbreakerChance = 100.0;
    public double autoShieldbreakerSwitchDelayMs = 50.0;
    public double autoShieldbreakerRestoreDelayMs = 50.0;
    public boolean autoShieldbreakerRandomDelay = true;
    public boolean autoShieldbreakerAbortOnManualSwitch = true;
    public boolean autoShieldbreakerLegitMode = true;

    // AutoStunSlam (Авто Стан Слэм)
    public boolean autoStunSlamEnabled = true;
    public Keybind autoStunSlamKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_M, true, true, false);
    public String autoStunSlamMode = "full_auto";
    public double autoStunSlamDistance = 2.4;
    public double autoStunSlamChance = 75.0;
    public double autoStunSlamAirTimeSec = 1.0;
    public double autoStunSlamAxeDelayMs = 45.0;
    public double autoStunSlamMaceDelayMs = 45.0;
    public double autoStunSlamRestoreDelayMs = 50.0;
    public boolean autoStunSlamRandomDelay = true;
    public boolean autoStunSlamLegitMode = true;

    // Backward-compatibility legacy alias fields for JSON deserialization
    public Boolean autoStunSlimeEnabled = null;
    public Keybind autoStunSlimeKeybind = null;
    public String autoStunSlimeMode = null;
    public Double autoStunSlimeDistance = null;
    public Double autoStunSlimeChance = null;
    public Double autoStunSlimeAirTimeSec = null;
    public Double autoStunSlimeAxeDelayMs = null;
    public Double autoStunSlimeMaceDelayMs = null;
    public Double autoStunSlimeRestoreDelayMs = null;
    public Boolean autoStunSlimeRandomDelay = null;
    public Boolean autoStunSlimeLegitMode = null;

    // ==========================================
    // 2. DEFENSE MODULES (Защита и карты)
    // ==========================================
    // AutoTotem
    public boolean autoTotemEnabled = true;
    public Keybind autoTotemKeybind = new Keybind();
    public String autoTotemMode = "main_hand";
    public double autoTotemTriggerHearts = 3.0;
    public double autoTotemRestoreHearts = 6.0;
    public double autoTotemChance = 100.0;
    public boolean autoTotemReturnItem = true;
    public boolean autoTotemReturnOnPop = true;

    // AutoCart
    public boolean autoCartEnabled = true;
    public Keybind autoCartKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_I, true, true, false);
    public String autoCartPreset = "medium";
    public double autoCartPlacementChance = 100.0;
    public double autoCartMaxDistance = 4.4;
    public double autoCartMinDelayMs = 70.0;
    public double autoCartMaxDelayMs = 110.0;
    public boolean autoCartAllowSelfCart = false;
    public boolean autoCartAllowPitPlacement = true;
    public boolean autoCartRandomDelay = true;
    public boolean autoCartLegitMode = true;
    public double autoCartRailDelay = 2.0;
    public double autoCartCartDelay = 2.0;
    public double autoCartRestoreDelay = 2.0;

    // AutoAnchor
    public boolean autoAnchorEnabled = true;
    public Keybind autoAnchorKeybind = new Keybind();
    public String autoAnchorPreset = "balanced";
    public boolean autoAnchorAutoExplode = false;
    public boolean autoAnchorAutoReturn = true;
    public double autoAnchorChargeDelay = 1.0;
    public double autoAnchorExplodeDelay = 1.0;
    public double autoAnchorChance = 85.0;
    public double autoAnchorTargetCharges = 1.0;
    public boolean autoAnchorLegitMode = true;

    // CartRefill
    public boolean cartRefillEnabled = true;
    public Keybind cartRefillKeybind = new Keybind();
    public double cartRefillDelayTicks = 2.0;
    public double cartRefillChance = 100.0;
    public boolean cartRefillAutoClose = true;
    public boolean cartRefillRandomDelay = true;
    public boolean cartRefillLegitMode = true;

    // ==========================================
    // 3. UTILITY MODULES (Утилиты и HUD)
    // ==========================================
    // HPReaper
    public boolean hpReaperEnabled = true;
    public Keybind hpReaperKeybind = new Keybind();
    public String hpReaperMode = "target_hp";
    public String hpReaperTargetFilter = "all_entities";
    public int hpReaperOwnHealthX = -1;
    public int hpReaperOwnHealthY = -1;
    public int hpReaperCrosshairTargetX = -1;
    public int hpReaperCrosshairTargetY = -1;
    public int hpReaperTargetHealthX = -1;
    public int hpReaperTargetHealthY = -1;
    public int hpReaperDiffX = -1;
    public int hpReaperDiffY = -1;

    // AutoTool
    public boolean autoToolEnabled = true;
    public Keybind autoToolKeybind = new Keybind();
    public boolean autoToolCombatGuard = true;
    public boolean autoToolDurabilitySaver = true;
    public double autoToolDurabilityThreshold = 5.0;
    public boolean autoToolPreferSilkTouch = false;
    public boolean autoToolRestorePrevious = true;
    public boolean autoToolLegitMode = true;
    public boolean autoToolSingleSlotMode = false;
    public boolean autoToolIgnoreInstantBreak = true;
    public boolean autoToolLockWhileMining = true;

    // AutoGG
    public boolean autoGGEnabled = true;
    public Keybind autoGGKeybind = new Keybind();
    public Keybind autoGGMenuKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_G);
    public String autoGGPhrase = "GGWP";
    public boolean autoGGSendOnKill = true;
    public boolean autoGGSendOnOwnDeath = false;
    public boolean autoGGRandomOrder = false;
    public double autoGGDelayMs = 950.0;

    // CartHUD
    public boolean cartHudEnabled = true;
    public Keybind cartHudKeybind = new Keybind();
    public int cartHudCustomX = -1;
    public int cartHudCustomY = -1;

    // ==========================================
    // 4. HUD & VISUALS
    // ==========================================
    public boolean overlayEnabled = true;
    public boolean darkThemeEnabled = true;
    public String hudPosition = "top_right";
    public double overlayOpacity = 85.0;
    public boolean autoHideOnChat = false;
    public boolean hideInF3 = true;
    public String searchFilter = "";
    public String filterCategory = "Все категории";
    public boolean matchCase = false;

    public boolean showCoordinates = true;
    public boolean showFps = true;
    public boolean showBiome = true;
    public boolean showWorldTime = false;
    public boolean showDirection = true;
    public String coordFormat = "X, Y, Z";
    public double hudPadding = 8.0;
    public String customTitle = "Activity HUD";
    public boolean textShadow = true;

    // ==========================================
    // 5. PROFILES & SYSTEM SETTINGS
    // ==========================================
    public String activeProfile = PRESET_DEFAULT;
    public String themeVariant = "Фирменная тёмная";
    public boolean compactMode = false;
    public boolean tooltipsEnabled = true;
    public boolean showKeyHints = true;
    public boolean smoothTransitions = true;
    public double soundVolume = 70.0;
    public boolean audioClicks = true;
    public String customPrefix = "ACT";
    public String toastStyle = "Компактно справа";

    public boolean debugLogging = false;
    public boolean profilerActive = false;
    public boolean asyncTickEnabled = true;
    public String logLevel = "INFO";
    public boolean benchmarksEnabled = false;
    public double maxCacheEntries = 256.0;
    public boolean scissorOpt = true;
    public String filterRegex = ".*";
    public String gcPolicy = "Консервативный";

    // ==========================================
    // 6. PHASE 2: VISUAL, FONT, SOUND, WINDOW
    // ==========================================
    public String fontFamily = "onest";
    public String typographySize = "normal";
    public double windowOpacity = 85.0;
    public double panelOpacity = 65.0;
    public boolean glassEffect = true;
    public int windowPosX = -1;
    public int windowPosY = -1;
    public int windowWidth = -1;
    public int windowHeight = -1;
    public boolean windowMaximized = false;
    public int unmaximizedX = -1;
    public int unmaximizedY = -1;
    public int unmaximizedWidth = -1;
    public int unmaximizedHeight = -1;
    public boolean soundEnabled = true;
    public String soundProfile = "serene";
    public boolean sliderSoundEnabled = true;
    public boolean animationsEnabled = true;
    public boolean spatialOpenAnimation = true;

    // ==========================================
    // 7. STRUCTURED MODULE PERSISTENCE (NIVORAT SDK)
    // ==========================================
    public static class ModuleConfigEntry {
        public boolean enabled = true;
        public Keybind keybind = new Keybind();
        public java.util.Map<String, Object> settings = new java.util.LinkedHashMap<>();

        public ModuleConfigEntry() {}

        public ModuleConfigEntry(boolean enabled, Keybind keybind) {
            this.enabled = enabled;
            if (keybind != null) {
                this.keybind.copyFrom(keybind);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ModuleConfigEntry that)) return false;
            return this.enabled == that.enabled &&
                    java.util.Objects.equals(this.keybind, that.keybind) &&
                    java.util.Objects.equals(this.settings, that.settings);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(enabled, keybind, settings);
        }
    }

    public java.util.Map<String, ModuleConfigEntry> modules = new java.util.LinkedHashMap<>();

    // ==========================================
    // 8. STRUCTURED CLIENT PERSISTENCE (NIVORAT SCHEMA)
    // ==========================================
    public static class UiSection {
        public boolean overlayEnabled = true;
        public boolean darkThemeEnabled = true;
        public String hudPosition = "top_right";
        public double overlayOpacity = 85.0;
        public boolean autoHideOnChat = false;
        public boolean hideInF3 = true;
        public boolean showCoordinates = true;
        public boolean showFps = true;
        public boolean showBiome = true;
        public boolean showWorldTime = false;
        public boolean showDirection = true;
        public String coordFormat = "X, Y, Z";
        public double hudPadding = 8.0;
        public String customTitle = "Activity HUD";
        public boolean textShadow = true;
        public String themeVariant = "Фирменная тёмная";
        public boolean compactMode = false;
        public boolean tooltipsEnabled = true;
        public boolean showKeyHints = true;
        public boolean smoothTransitions = true;
        public boolean animationsEnabled = true;
        public boolean spatialOpenAnimation = true;
        public double windowOpacity = 85.0;
        public double panelOpacity = 65.0;
        public boolean glassEffect = true;
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UiSection that)) return false;
            return overlayEnabled == that.overlayEnabled &&
                    darkThemeEnabled == that.darkThemeEnabled &&
                    autoHideOnChat == that.autoHideOnChat &&
                    hideInF3 == that.hideInF3 &&
                    showCoordinates == that.showCoordinates &&
                    showFps == that.showFps &&
                    showBiome == that.showBiome &&
                    showWorldTime == that.showWorldTime &&
                    showDirection == that.showDirection &&
                    textShadow == that.textShadow &&
                    compactMode == that.compactMode &&
                    tooltipsEnabled == that.tooltipsEnabled &&
                    showKeyHints == that.showKeyHints &&
                    smoothTransitions == that.smoothTransitions &&
                    animationsEnabled == that.animationsEnabled &&
                    spatialOpenAnimation == that.spatialOpenAnimation &&
                    glassEffect == that.glassEffect &&
                    Double.compare(overlayOpacity, that.overlayOpacity) == 0 &&
                    Double.compare(hudPadding, that.hudPadding) == 0 &&
                    Double.compare(windowOpacity, that.windowOpacity) == 0 &&
                    Double.compare(panelOpacity, that.panelOpacity) == 0 &&
                    Objects.equals(hudPosition, that.hudPosition) &&
                    Objects.equals(coordFormat, that.coordFormat) &&
                    Objects.equals(customTitle, that.customTitle) &&
                    Objects.equals(themeVariant, that.themeVariant);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                overlayEnabled, darkThemeEnabled, hudPosition, overlayOpacity, autoHideOnChat,
                hideInF3, showCoordinates, showFps, showBiome, showWorldTime, showDirection,
                coordFormat, hudPadding, customTitle, textShadow, themeVariant, compactMode,
                tooltipsEnabled, showKeyHints, smoothTransitions, animationsEnabled,
                spatialOpenAnimation, windowOpacity, panelOpacity, glassEffect
            );
        }
    }

    public static class SoundSection {
        public boolean soundEnabled = true;
        public String soundProfile = "serene";
        public double soundVolume = 70.0;
        public boolean audioClicks = true;
        public boolean sliderSoundEnabled = true;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SoundSection that)) return false;
            return soundEnabled == that.soundEnabled &&
                    audioClicks == that.audioClicks &&
                    sliderSoundEnabled == that.sliderSoundEnabled &&
                    Double.compare(soundVolume, that.soundVolume) == 0 &&
                    Objects.equals(soundProfile, that.soundProfile);
        }

        @Override
        public int hashCode() {
            return Objects.hash(soundEnabled, soundProfile, soundVolume, audioClicks, sliderSoundEnabled);
        }
    }

    public static class FontsSection {
        public String fontFamily = "onest";
        public String typographySize = "normal";

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FontsSection that)) return false;
            return Objects.equals(fontFamily, that.fontFamily) &&
                    Objects.equals(typographySize, that.typographySize);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fontFamily, typographySize);
        }
    }

    public static class ClientSection {
        public UiSection ui = new UiSection();
        public SoundSection sound = new SoundSection();
        public FontsSection fonts = new FontsSection();

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ClientSection that)) return false;
            return Objects.equals(ui, that.ui) &&
                    Objects.equals(sound, that.sound) &&
                    Objects.equals(fonts, that.fonts);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ui, sound, fonts);
        }
    }

    public ClientSection client = new ClientSection();

    // Legacy migration state
    public boolean legacyMigrationDone = false;
    public int legacyMigrationVersion = 0;

    public ActivityConfig() {}

    /**
     * Applies a named gameplay configuration preset.
     *
     * @param presetName name or ID of preset
     */
    public void applyPreset(String presetName) {
        if (presetName == null || presetName.isBlank()) return;
        activity.client.config.preset.PresetManager.applyPresetByName(presetName, this);
    }

    /**
     * Resets all parameters to their factory default values.
     */
    public void resetToDefaults() {
        this.configVersion = 2;

        // Combat
        this.autoMaceEnabled = true;
        this.autoMaceKeybind.clear();
        this.autoMaceSourceMode = "sword_and_axe";
        this.autoMaceEnchantMode = "smart";
        this.autoMaceMissBehavior = "sword_hit";
        this.autoMaceRestoreDelayMs = 90.0;
        this.autoMaceLegitMode = true;
        this.autoMaceMissChance = 10.0;
        this.autoMaceRandomDelay = true;

        this.autoSpearEnabled = true;
        this.autoSpearKeybind.clear();
        this.autoSpearTriggerKeybind.set(org.lwjgl.glfw.GLFW.GLFW_KEY_TAB, false, false, false);
        this.autoSpearSecurityMode = "legit";
        this.autoSpearPriorityMode = "auto";
        this.autoSpearRestoreDelayMs = 185.0;
        this.autoSpearMissChance = 0.0;
        this.autoSpearRandomDelay = true;

        this.autoShieldbreakerEnabled = true;
        this.autoShieldbreakerKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_J, true, true, false);
        this.autoShieldbreakerMode = "full_auto";
        this.autoShieldbreakerDistance = 2.85;
        this.autoShieldbreakerChance = 100.0;
        this.autoShieldbreakerSwitchDelayMs = 50.0;
        this.autoShieldbreakerRestoreDelayMs = 50.0;
        this.autoShieldbreakerRandomDelay = true;
        this.autoShieldbreakerAbortOnManualSwitch = true;
        this.autoShieldbreakerLegitMode = true;

        this.autoStunSlamEnabled = true;
        this.autoStunSlamKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_M, true, true, false);
        this.autoStunSlamMode = "full_auto";
        this.autoStunSlamDistance = 2.4;
        this.autoStunSlamChance = 75.0;
        this.autoStunSlamAirTimeSec = 1.0;
        this.autoStunSlamAxeDelayMs = 45.0;
        this.autoStunSlamMaceDelayMs = 45.0;
        this.autoStunSlamRestoreDelayMs = 50.0;
        this.autoStunSlamRandomDelay = true;
        this.autoStunSlamLegitMode = true;

        this.autoStunSlimeEnabled = null;
        this.autoStunSlimeKeybind = null;
        this.autoStunSlimeMode = null;
        this.autoStunSlimeDistance = null;
        this.autoStunSlimeChance = null;
        this.autoStunSlimeAirTimeSec = null;
        this.autoStunSlimeAxeDelayMs = null;
        this.autoStunSlimeMaceDelayMs = null;
        this.autoStunSlimeRestoreDelayMs = null;
        this.autoStunSlimeRandomDelay = null;
        this.autoStunSlimeLegitMode = null;

        if (this.pinnedModules != null) {
            this.pinnedModules.clear();
        }

        // Defense
        this.autoTotemEnabled = true;
        this.autoTotemKeybind.clear();
        this.autoTotemMode = "main_hand";
        this.autoTotemTriggerHearts = 3.0;
        this.autoTotemRestoreHearts = 6.0;
        this.autoTotemChance = 100.0;
        this.autoTotemReturnItem = true;
        this.autoTotemReturnOnPop = true;

        this.autoCartEnabled = true;
        this.autoCartKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_I, true, true, false);
        this.autoCartPreset = "medium";
        this.autoCartPlacementChance = 100.0;
        this.autoCartMaxDistance = 4.4;
        this.autoCartMinDelayMs = 70.0;
        this.autoCartMaxDelayMs = 110.0;
        this.autoCartAllowSelfCart = false;
        this.autoCartAllowPitPlacement = true;
        this.autoCartRandomDelay = true;
        this.autoCartRailDelay = 2.0;
        this.autoCartCartDelay = 2.0;
        this.autoCartRestoreDelay = 2.0;
        this.autoCartLegitMode = true;

        this.autoAnchorEnabled = true;
        this.autoAnchorKeybind.clear();
        this.autoAnchorPreset = "balanced";
        this.autoAnchorAutoExplode = false;
        this.autoAnchorAutoReturn = true;
        this.autoAnchorChargeDelay = 1.0;
        this.autoAnchorExplodeDelay = 1.0;
        this.autoAnchorChance = 85.0;
        this.autoAnchorTargetCharges = 1.0;
        this.autoAnchorLegitMode = true;

        this.cartRefillEnabled = true;
        this.cartRefillKeybind.clear();
        this.cartRefillDelayTicks = 2.0;
        this.cartRefillChance = 100.0;
        this.cartRefillAutoClose = true;
        this.cartRefillRandomDelay = true;
        this.cartRefillLegitMode = true;

        // Utility
        this.hpReaperEnabled = true;
        this.hpReaperKeybind.clear();
        this.hpReaperMode = "target_hp";
        this.hpReaperTargetFilter = "all_entities";
        this.hpReaperOwnHealthX = -1;
        this.hpReaperOwnHealthY = -1;
        this.hpReaperCrosshairTargetX = -1;
        this.hpReaperCrosshairTargetY = -1;
        this.hpReaperTargetHealthX = -1;
        this.hpReaperTargetHealthY = -1;
        this.hpReaperDiffX = -1;
        this.hpReaperDiffY = -1;

        this.autoToolEnabled = true;
        this.autoToolKeybind.clear();
        this.autoToolCombatGuard = true;
        this.autoToolDurabilitySaver = true;
        this.autoToolDurabilityThreshold = 5.0;
        this.autoToolPreferSilkTouch = false;
        this.autoToolRestorePrevious = true;
        this.autoToolLegitMode = true;
        this.autoToolSingleSlotMode = false;
        this.autoToolIgnoreInstantBreak = true;
        this.autoToolLockWhileMining = true;

        this.autoGGEnabled = true;
        this.autoGGKeybind.clear();
        this.autoGGMenuKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_G);
        this.autoGGPhrase = "GGWP";
        this.autoGGSendOnKill = true;
        this.autoGGSendOnOwnDeath = false;
        this.autoGGRandomOrder = false;
        this.autoGGDelayMs = 950.0;

        this.cartHudEnabled = true;
        this.cartHudKeybind.clear();
        this.cartHudCustomX = -1;
        this.cartHudCustomY = -1;

        // HUD & System
        this.overlayEnabled = true;
        this.darkThemeEnabled = true;
        this.hudPosition = "top_right";
        this.overlayOpacity = 85.0;
        this.autoHideOnChat = false;
        this.hideInF3 = true;
        this.searchFilter = "";
        this.filterCategory = "Все категории";
        this.matchCase = false;

        this.activeProfile = PRESET_DEFAULT;
        this.themeVariant = "Фирменная тёмная";
        this.compactMode = false;
        this.tooltipsEnabled = true;
        this.showKeyHints = true;
        this.smoothTransitions = true;
        this.soundVolume = 70.0;
        this.audioClicks = true;
        this.customPrefix = "ACT";
        this.toastStyle = "Компактно справа";

        this.showCoordinates = true;
        this.showFps = true;
        this.showBiome = true;
        this.showWorldTime = false;
        this.showDirection = true;
        this.coordFormat = "X, Y, Z";
        this.hudPadding = 8.0;
        this.customTitle = "Activity HUD";
        this.textShadow = true;

        this.debugLogging = false;
        this.profilerActive = false;
        this.asyncTickEnabled = true;
        this.logLevel = "INFO";
        this.benchmarksEnabled = false;
        this.maxCacheEntries = 256.0;
        this.scissorOpt = true;
        this.filterRegex = ".*";
        this.gcPolicy = "Консервативный";

        this.fontFamily = "onest";
        this.typographySize = "normal";
        this.windowOpacity = 85.0;
        this.panelOpacity = 65.0;
        this.glassEffect = true;
        this.windowPosX = -1;
        this.windowPosY = -1;
        this.windowWidth = -1;
        this.windowHeight = -1;
        this.windowMaximized = false;
        this.unmaximizedX = -1;
        this.unmaximizedY = -1;
        this.unmaximizedWidth = -1;
        this.unmaximizedHeight = -1;
        this.soundEnabled = true;
        this.soundProfile = "serene";
        this.soundVolume = 70.0;
        this.sliderSoundEnabled = true;
        this.animationsEnabled = true;
        this.spatialOpenAnimation = true;
        this.client = new ClientSection();
        syncClientSection();
        syncModuleConfigEntries();
    }

    /**
     * Validates and sanitizes loaded configuration values against bounds, nulls, and NaNs.
     */
    public void sanitize() {
        if (this.modules == null) {
            this.modules = new java.util.LinkedHashMap<>();
        } else {
            ModuleConfigEntry slime = null;
            for (String k : new java.util.ArrayList<>(this.modules.keySet())) {
                String cleanK = k.replace("_", "").toLowerCase(java.util.Locale.ROOT);
                if ("autostunslime".equals(cleanK)) {
                    slime = this.modules.remove(k);
                    break;
                }
            }
            if (slime != null) {
                if (!this.modules.containsKey("auto_stun_slam") || this.modules.get("auto_stun_slam") == null) {
                    this.modules.put("auto_stun_slam", slime);
                }
                if (slime.keybind != null && !slime.keybind.isUnbound()) {
                    this.autoStunSlamKeybind.copyFrom(slime.keybind);
                }
                this.autoStunSlamEnabled = slime.enabled;
            }
        }

        // Keybind null guards
        if (this.autoMaceKeybind == null) this.autoMaceKeybind = new Keybind();
        if (this.autoSpearKeybind == null) this.autoSpearKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_TAB);
        if (this.autoSpearTriggerKeybind == null) this.autoSpearTriggerKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_TAB);
        if (this.autoShieldbreakerKeybind == null) this.autoShieldbreakerKeybind = new Keybind();
        if (this.autoStunSlamKeybind == null) this.autoStunSlamKeybind = new Keybind();
        if (this.autoTotemKeybind == null) this.autoTotemKeybind = new Keybind();
        if (this.autoCartKeybind == null) this.autoCartKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_I, true, true, false);
        if (this.autoAnchorKeybind == null) this.autoAnchorKeybind = new Keybind();
        if (this.cartRefillKeybind == null) this.cartRefillKeybind = new Keybind();
        if (this.hpReaperKeybind == null) this.hpReaperKeybind = new Keybind();
        if (this.autoToolKeybind == null) this.autoToolKeybind = new Keybind();
        if (this.autoGGKeybind == null) this.autoGGKeybind = new Keybind();
        if (this.autoGGMenuKeybind == null) this.autoGGMenuKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_G);
        if (this.cartHudKeybind == null) this.cartHudKeybind = new Keybind();

        // Migration from legacy autoStunSlime JSON fields if present
        if (this.autoStunSlimeEnabled != null) {
            this.autoStunSlamEnabled = this.autoStunSlimeEnabled;
            this.autoStunSlimeEnabled = null;
        }
        if (this.autoStunSlimeKeybind != null) {
            this.autoStunSlamKeybind.copyFrom(this.autoStunSlimeKeybind);
            this.autoStunSlimeKeybind = null;
        }
        if (this.autoStunSlimeMode != null) {
            this.autoStunSlamMode = this.autoStunSlimeMode;
            this.autoStunSlimeMode = null;
        }
        if (this.autoStunSlimeDistance != null) {
            this.autoStunSlamDistance = this.autoStunSlimeDistance;
            this.autoStunSlimeDistance = null;
        }
        if (this.autoStunSlimeChance != null) {
            this.autoStunSlamChance = this.autoStunSlimeChance;
            this.autoStunSlimeChance = null;
        }
        if (this.autoStunSlimeAirTimeSec != null) {
            this.autoStunSlamAirTimeSec = this.autoStunSlimeAirTimeSec;
            this.autoStunSlimeAirTimeSec = null;
        }
        if (this.autoStunSlimeAxeDelayMs != null) {
            this.autoStunSlamAxeDelayMs = this.autoStunSlimeAxeDelayMs;
            this.autoStunSlimeAxeDelayMs = null;
        }
        if (this.autoStunSlimeMaceDelayMs != null) {
            this.autoStunSlamMaceDelayMs = this.autoStunSlimeMaceDelayMs;
            this.autoStunSlimeMaceDelayMs = null;
        }
        if (this.autoStunSlimeRestoreDelayMs != null) {
            this.autoStunSlamRestoreDelayMs = this.autoStunSlimeRestoreDelayMs;
            this.autoStunSlimeRestoreDelayMs = null;
        }
        if (this.autoStunSlimeRandomDelay != null) {
            this.autoStunSlamRandomDelay = this.autoStunSlimeRandomDelay;
            this.autoStunSlimeRandomDelay = null;
        }
        if (this.autoStunSlimeLegitMode != null) {
            this.autoStunSlamLegitMode = this.autoStunSlimeLegitMode;
            this.autoStunSlimeLegitMode = null;
        }

        // Pinned modules migration and null guard
        if (this.pinnedModules == null) {
            this.pinnedModules = new java.util.ArrayList<>();
        } else {
            for (int i = 0; i < this.pinnedModules.size(); i++) {
                String p = this.pinnedModules.get(i);
                if (p != null && "autostunslime".equals(p.replace("_", "").toLowerCase(java.util.Locale.ROOT))) {
                    this.pinnedModules.set(i, "auto_stun_slam");
                }
            }
        }

        // Sliders & Numbers
        this.autoMaceRestoreDelayMs = clampSanitize(this.autoMaceRestoreDelayMs, 30.0, 300.0, 90.0);
        this.autoMaceMissChance = clampSanitize(this.autoMaceMissChance, 0.0, 50.0, 10.0);
        this.autoSpearRestoreDelayMs = clampSanitize(this.autoSpearRestoreDelayMs, 10.0, 500.0, 185.0);
        this.autoSpearMissChance = clampSanitize(this.autoSpearMissChance, 0.0, 50.0, 0.0);

        this.autoShieldbreakerDistance = clampSanitize(this.autoShieldbreakerDistance, 1.5, 4.5, 2.85);
        this.autoShieldbreakerChance = clampSanitize(this.autoShieldbreakerChance, 10.0, 100.0, 100.0);
        this.autoShieldbreakerSwitchDelayMs = clampSanitize(this.autoShieldbreakerSwitchDelayMs, 10.0, 200.0, 50.0);
        this.autoShieldbreakerRestoreDelayMs = clampSanitize(this.autoShieldbreakerRestoreDelayMs, 10.0, 200.0, 50.0);

        this.autoStunSlamDistance = clampSanitize(this.autoStunSlamDistance, 1.5, 4.0, 2.4);
        this.autoStunSlamChance = clampSanitize(this.autoStunSlamChance, 10.0, 100.0, 75.0);
        this.autoStunSlamAirTimeSec = clampSanitize(this.autoStunSlamAirTimeSec, 0.1, 5.0, 1.0);
        this.autoStunSlamAxeDelayMs = clampSanitize(this.autoStunSlamAxeDelayMs, 10.0, 200.0, 45.0);
        this.autoStunSlamMaceDelayMs = clampSanitize(this.autoStunSlamMaceDelayMs, 10.0, 200.0, 45.0);
        this.autoStunSlamRestoreDelayMs = clampSanitize(this.autoStunSlamRestoreDelayMs, 10.0, 200.0, 50.0);

        this.autoTotemTriggerHearts = clampSanitize(this.autoTotemTriggerHearts, 1.0, 9.0, 3.0);
        this.autoTotemRestoreHearts = clampSanitize(this.autoTotemRestoreHearts, 4.0, 10.0, 6.0);
        this.autoTotemChance = clampSanitize(this.autoTotemChance, 10.0, 100.0, 100.0);

        this.autoCartPlacementChance = clampSanitize(this.autoCartPlacementChance, 0.0, 100.0, 100.0);
        this.autoCartMaxDistance = clampSanitize(this.autoCartMaxDistance, 1.5, 4.5, 4.4);
        this.autoCartMinDelayMs = clampSanitize(this.autoCartMinDelayMs, 10.0, 200.0, 70.0);
        this.autoCartMaxDelayMs = clampSanitize(this.autoCartMaxDelayMs, 10.0, 300.0, 110.0);
        this.autoCartRailDelay = clampSanitize(this.autoCartRailDelay, 0.0, 10.0, 2.0);
        this.autoCartCartDelay = clampSanitize(this.autoCartCartDelay, 0.0, 10.0, 2.0);
        this.autoCartRestoreDelay = clampSanitize(this.autoCartRestoreDelay, 0.0, 10.0, 2.0);

        this.autoAnchorChargeDelay = clampSanitize(this.autoAnchorChargeDelay, 0.0, 10.0, 1.0);
        this.autoAnchorExplodeDelay = clampSanitize(this.autoAnchorExplodeDelay, 0.0, 10.0, 1.0);
        this.autoAnchorChance = clampSanitize(this.autoAnchorChance, 10.0, 100.0, 85.0);
        this.autoAnchorTargetCharges = clampSanitize(this.autoAnchorTargetCharges, 1.0, 4.0, 1.0);

        this.cartRefillDelayTicks = clampSanitize(this.cartRefillDelayTicks, 0.0, 10.0, 2.0);
        this.cartRefillChance = clampSanitize(this.cartRefillChance, 10.0, 100.0, 100.0);

        this.autoToolDurabilityThreshold = clampSanitize(this.autoToolDurabilityThreshold, 1.0, 50.0, 5.0);
        this.autoGGDelayMs = clampSanitize(this.autoGGDelayMs, 100.0, 3000.0, 950.0);

        this.overlayOpacity = clampSanitize(this.overlayOpacity, 10.0, 100.0, 85.0);
        this.soundVolume = clampSanitize(this.soundVolume, 0.0, 100.0, 70.0);
        this.hudPadding = clampSanitize(this.hudPadding, 0.0, 64.0, 8.0);
        this.maxCacheEntries = clampSanitize(this.maxCacheEntries, 16.0, 2048.0, 256.0);

        // String migration and non-null guards
        if ("Меч и топор".equals(this.autoMaceSourceMode) || "sword_and_axe".equals(this.autoMaceSourceMode)) this.autoMaceSourceMode = "sword_and_axe";
        else if ("Только меч".equals(this.autoMaceSourceMode) || "sword_only".equals(this.autoMaceSourceMode)) this.autoMaceSourceMode = "sword_only";
        else if ("Только топор".equals(this.autoMaceSourceMode) || "axe_only".equals(this.autoMaceSourceMode)) this.autoMaceSourceMode = "axe_only";
        else this.autoMaceSourceMode = "sword_and_axe";

        if ("Умный выбор".equals(this.autoMaceEnchantMode) || "smart".equals(this.autoMaceEnchantMode)) this.autoMaceEnchantMode = "smart";
        else if ("Только Пробивание".equals(this.autoMaceEnchantMode) || "breach_only".equals(this.autoMaceEnchantMode)) this.autoMaceEnchantMode = "breach_only";
        else if ("Только Плотность".equals(this.autoMaceEnchantMode) || "density_only".equals(this.autoMaceEnchantMode)) this.autoMaceEnchantMode = "density_only";
        else this.autoMaceEnchantMode = "smart";

        if ("Удар мечом".equals(this.autoMaceMissBehavior) || "sword_hit".equals(this.autoMaceMissBehavior)) this.autoMaceMissBehavior = "sword_hit";
        else if ("Холостой свап".equals(this.autoMaceMissBehavior) || "empty_swap".equals(this.autoMaceMissBehavior)) this.autoMaceMissBehavior = "empty_swap";
        else this.autoMaceMissBehavior = "sword_hit";

        if ("Легитный".equals(this.autoSpearSecurityMode) || "legit".equals(this.autoSpearSecurityMode)) this.autoSpearSecurityMode = "legit";
        else if ("Полу-легит".equals(this.autoSpearSecurityMode) || "semi_legit".equals(this.autoSpearSecurityMode)) this.autoSpearSecurityMode = "semi_legit";
        else if ("Рейдж".equals(this.autoSpearSecurityMode) || "rage".equals(this.autoSpearSecurityMode)) this.autoSpearSecurityMode = "rage";
        else this.autoSpearSecurityMode = "legit";

        if ("Авто".equals(this.autoSpearPriorityMode) || "auto".equals(this.autoSpearPriorityMode)) this.autoSpearPriorityMode = "auto";
        else if ("Выпад I".equals(this.autoSpearPriorityMode) || "lunge_1".equals(this.autoSpearPriorityMode)) this.autoSpearPriorityMode = "lunge_1";
        else if ("Выпад II".equals(this.autoSpearPriorityMode) || "lunge_2".equals(this.autoSpearPriorityMode)) this.autoSpearPriorityMode = "lunge_2";
        else if ("Выпад III".equals(this.autoSpearPriorityMode) || "lunge_3".equals(this.autoSpearPriorityMode)) this.autoSpearPriorityMode = "lunge_3";
        else if ("Случайный".equals(this.autoSpearPriorityMode) || "random".equals(this.autoSpearPriorityMode)) this.autoSpearPriorityMode = "random";
        else this.autoSpearPriorityMode = "auto";

        if ("Полный авто".equals(this.autoShieldbreakerMode) || "full_auto".equals(this.autoShieldbreakerMode)) this.autoShieldbreakerMode = "full_auto";
        else if ("Полу-авто".equals(this.autoShieldbreakerMode) || "semi_auto".equals(this.autoShieldbreakerMode)) this.autoShieldbreakerMode = "semi_auto";
        else this.autoShieldbreakerMode = "full_auto";

        if ("Полный авто".equals(this.autoStunSlamMode) || "full_auto".equals(this.autoStunSlamMode)) this.autoStunSlamMode = "full_auto";
        else if ("Полу-авто".equals(this.autoStunSlamMode) || "semi_auto".equals(this.autoStunSlamMode)) this.autoStunSlamMode = "semi_auto";
        else this.autoStunSlamMode = "full_auto";

        if ("Основная рука".equals(this.autoTotemMode) || "main_hand".equals(this.autoTotemMode)) this.autoTotemMode = "main_hand";
        else if ("Вторая рука".equals(this.autoTotemMode) || "offhand".equals(this.autoTotemMode)) this.autoTotemMode = "offhand";
        else this.autoTotemMode = "main_hand";

        if ("Быстрый".equals(this.autoCartPreset) || "fast".equals(this.autoCartPreset)) this.autoCartPreset = "fast";
        else if ("Средний".equals(this.autoCartPreset) || "medium".equals(this.autoCartPreset)) this.autoCartPreset = "medium";
        else if ("Безопасный".equals(this.autoCartPreset) || "safe".equals(this.autoCartPreset)) this.autoCartPreset = "safe";
        else this.autoCartPreset = "fast";

        if ("Быстрый".equals(this.autoAnchorPreset) || "fast".equals(this.autoAnchorPreset)) this.autoAnchorPreset = "fast";
        else if ("Средний".equals(this.autoAnchorPreset) || "medium".equals(this.autoAnchorPreset)) this.autoAnchorPreset = "medium";
        else if ("Сбалансированный".equals(this.autoAnchorPreset) || "balanced".equals(this.autoAnchorPreset)) this.autoAnchorPreset = "balanced";
        else if ("Безопасный".equals(this.autoAnchorPreset) || "safe".equals(this.autoAnchorPreset)) this.autoAnchorPreset = "safe";
        else this.autoAnchorPreset = "balanced";

        if ("Числовое HP цели".equals(this.hpReaperMode) || "target_hp".equals(this.hpReaperMode)) this.hpReaperMode = "target_hp";
        else if ("Своё здоровье".equals(this.hpReaperMode) || "own_hp".equals(this.hpReaperMode)) this.hpReaperMode = "own_hp";
        else if ("Разница урона".equals(this.hpReaperMode) || "damage_diff".equals(this.hpReaperMode)) this.hpReaperMode = "damage_diff";
        else if ("Компактный".equals(this.hpReaperMode) || "compact".equals(this.hpReaperMode)) this.hpReaperMode = "compact";
        else this.hpReaperMode = "target_hp";

        if ("ALL_ENTITIES".equalsIgnoreCase(this.hpReaperTargetFilter) || "all_entities".equals(this.hpReaperTargetFilter)) this.hpReaperTargetFilter = "all_entities";
        else if ("PLAYERS_ONLY".equalsIgnoreCase(this.hpReaperTargetFilter) || "players_only".equals(this.hpReaperTargetFilter)) this.hpReaperTargetFilter = "players_only";
        else if ("HOSTILE_AND_PLAYERS".equalsIgnoreCase(this.hpReaperTargetFilter) || "hostile_and_players".equals(this.hpReaperTargetFilter)) this.hpReaperTargetFilter = "hostile_and_players";
        else this.hpReaperTargetFilter = "all_entities";

        if ("Сверху справа".equals(this.hudPosition) || "top_right".equals(this.hudPosition)) this.hudPosition = "top_right";
        else if ("Сверху слева".equals(this.hudPosition) || "top_left".equals(this.hudPosition)) this.hudPosition = "top_left";
        else if ("Снизу справа".equals(this.hudPosition) || "bottom_right".equals(this.hudPosition)) this.hudPosition = "bottom_right";
        else if ("Снизу слева".equals(this.hudPosition) || "bottom_left".equals(this.hudPosition)) this.hudPosition = "bottom_left";
        else this.hudPosition = "top_right";

        if ("По умолчанию".equals(this.activeProfile) || "default".equals(this.activeProfile) || this.activeProfile == null || this.activeProfile.isBlank()) {
            this.activeProfile = PRESET_DEFAULT;
        }

        if (this.autoGGPhrase == null) this.autoGGPhrase = "GGWP";
        if (this.searchFilter == null) this.searchFilter = "";
        if (this.filterCategory == null || this.filterCategory.isBlank()) this.filterCategory = "Все категории";
        if (this.themeVariant == null || this.themeVariant.isBlank()) this.themeVariant = "Фирменная тёмная";
        if (this.customPrefix == null || this.customPrefix.isBlank()) this.customPrefix = "ACT";
        if (this.toastStyle == null || this.toastStyle.isBlank()) this.toastStyle = "Компактно справа";
        if (this.coordFormat == null || this.coordFormat.isBlank()) this.coordFormat = "X, Y, Z";
        if (this.customTitle == null || this.customTitle.isBlank()) this.customTitle = "Activity HUD";
        if (this.logLevel == null || this.logLevel.isBlank()) this.logLevel = "INFO";
        if (this.filterRegex == null || this.filterRegex.isBlank()) this.filterRegex = ".*";
        if (this.gcPolicy == null || this.gcPolicy.isBlank()) this.gcPolicy = "Консервативный";

        this.windowOpacity = clampSanitize(this.windowOpacity, 30.0, 100.0, 85.0);
        this.panelOpacity = clampSanitize(this.panelOpacity, 20.0, 100.0, 65.0);
        if (!"minecraft".equals(this.fontFamily) && !"onest".equals(this.fontFamily) &&
            !"inter".equals(this.fontFamily) && !"manrope".equals(this.fontFamily) &&
            !"rubik".equals(this.fontFamily) && !"default".equals(this.fontFamily) &&
            !"retro_pixel".equals(this.fontFamily)) {
            this.fontFamily = "onest";
        }
        if (!"small".equals(this.typographySize) && !"normal".equals(this.typographySize) && !"large".equals(this.typographySize)) {
            this.typographySize = "normal";
        }
        if (!"serene".equals(this.soundProfile) && !"classic".equals(this.soundProfile) && !"minecraft".equals(this.soundProfile)) {
            this.soundProfile = "serene";
        }
        if (this.windowPosX < -1 || this.windowPosX > 10000) this.windowPosX = -1;
        if (this.windowPosY < -1 || this.windowPosY > 10000) this.windowPosY = -1;
        if (this.windowWidth < -1 || this.windowWidth > 10000) this.windowWidth = -1;
        if (this.windowHeight < -1 || this.windowHeight > 10000) this.windowHeight = -1;
        if (this.unmaximizedX < -1 || this.unmaximizedX > 10000) this.unmaximizedX = -1;
        if (this.unmaximizedY < -1 || this.unmaximizedY > 10000) this.unmaximizedY = -1;
        if (this.unmaximizedWidth < -1 || this.unmaximizedWidth > 10000) this.unmaximizedWidth = -1;
        if (this.unmaximizedHeight < -1 || this.unmaximizedHeight > 10000) this.unmaximizedHeight = -1;

        syncClientSection();
        syncModuleConfigEntries();
    }

    public void syncClientSection() {
        if (this.client == null) {
            this.client = new ClientSection();
        }
        if (this.client.ui == null) {
            this.client.ui = new UiSection();
        }
        if (this.client.sound == null) {
            this.client.sound = new SoundSection();
        }
        if (this.client.fonts == null) {
            this.client.fonts = new FontsSection();
        }

        this.client.ui.overlayEnabled = this.overlayEnabled;
        this.client.ui.darkThemeEnabled = this.darkThemeEnabled;
        this.client.ui.hudPosition = this.hudPosition;
        this.client.ui.overlayOpacity = this.overlayOpacity;
        this.client.ui.autoHideOnChat = this.autoHideOnChat;
        this.client.ui.hideInF3 = this.hideInF3;
        this.client.ui.showCoordinates = this.showCoordinates;
        this.client.ui.showFps = this.showFps;
        this.client.ui.showBiome = this.showBiome;
        this.client.ui.showWorldTime = this.showWorldTime;
        this.client.ui.showDirection = this.showDirection;
        this.client.ui.coordFormat = this.coordFormat;
        this.client.ui.hudPadding = this.hudPadding;
        this.client.ui.customTitle = this.customTitle;
        this.client.ui.textShadow = this.textShadow;
        this.client.ui.themeVariant = this.themeVariant;
        this.client.ui.compactMode = this.compactMode;
        this.client.ui.tooltipsEnabled = this.tooltipsEnabled;
        this.client.ui.showKeyHints = this.showKeyHints;
        this.client.ui.smoothTransitions = this.smoothTransitions;
        this.client.ui.animationsEnabled = this.animationsEnabled;
        this.client.ui.spatialOpenAnimation = this.spatialOpenAnimation;
        this.client.ui.windowOpacity = this.windowOpacity;
        this.client.ui.panelOpacity = this.panelOpacity;
        this.client.ui.glassEffect = this.glassEffect;

        this.client.sound.soundEnabled = this.soundEnabled;
        this.client.sound.soundProfile = this.soundProfile;
        this.client.sound.soundVolume = this.soundVolume;
        this.client.sound.audioClicks = this.audioClicks;
        this.client.sound.sliderSoundEnabled = this.sliderSoundEnabled;

        this.client.fonts.fontFamily = this.fontFamily;
        this.client.fonts.typographySize = this.typographySize;
    }

    public void syncFromClientSection() {
        if (this.client == null) return;
        if (this.client.ui != null) {
            this.overlayEnabled = this.client.ui.overlayEnabled;
            this.darkThemeEnabled = this.client.ui.darkThemeEnabled;
            if (this.client.ui.hudPosition != null) this.hudPosition = this.client.ui.hudPosition;
            this.overlayOpacity = this.client.ui.overlayOpacity;
            this.autoHideOnChat = this.client.ui.autoHideOnChat;
            this.hideInF3 = this.client.ui.hideInF3;
            this.showCoordinates = this.client.ui.showCoordinates;
            this.showFps = this.client.ui.showFps;
            this.showBiome = this.client.ui.showBiome;
            this.showWorldTime = this.client.ui.showWorldTime;
            this.showDirection = this.client.ui.showDirection;
            if (this.client.ui.coordFormat != null) this.coordFormat = this.client.ui.coordFormat;
            this.hudPadding = this.client.ui.hudPadding;
            if (this.client.ui.customTitle != null) this.customTitle = this.client.ui.customTitle;
            this.textShadow = this.client.ui.textShadow;
            if (this.client.ui.themeVariant != null) this.themeVariant = this.client.ui.themeVariant;
            this.compactMode = this.client.ui.compactMode;
            this.tooltipsEnabled = this.client.ui.tooltipsEnabled;
            this.showKeyHints = this.client.ui.showKeyHints;
            this.smoothTransitions = this.client.ui.smoothTransitions;
            this.animationsEnabled = this.client.ui.animationsEnabled;
            this.spatialOpenAnimation = this.client.ui.spatialOpenAnimation;
            this.windowOpacity = this.client.ui.windowOpacity;
            this.panelOpacity = this.client.ui.panelOpacity;
            this.glassEffect = this.client.ui.glassEffect;
        }
        if (this.client.sound != null) {
            this.soundEnabled = this.client.sound.soundEnabled;
            if (this.client.sound.soundProfile != null) this.soundProfile = this.client.sound.soundProfile;
            this.soundVolume = this.client.sound.soundVolume;
            this.audioClicks = this.client.sound.audioClicks;
            this.sliderSoundEnabled = this.client.sound.sliderSoundEnabled;
        }
        if (this.client.fonts != null) {
            if (this.client.fonts.fontFamily != null) this.fontFamily = this.client.fonts.fontFamily;
            if (this.client.fonts.typographySize != null) this.typographySize = this.client.fonts.typographySize;
        }
    }

    public void syncModuleConfigEntries() {
        if (this.modules == null) {
            this.modules = new java.util.LinkedHashMap<>();
        }
        syncModuleEntry("auto_mace", this.autoMaceEnabled, this.autoMaceKeybind);
        populateMaceSettings(this.modules.get("auto_mace"));

        syncModuleEntry("auto_spear", this.autoSpearEnabled, this.autoSpearKeybind);
        populateSpearSettings(this.modules.get("auto_spear"));

        syncModuleEntry("auto_shieldbreaker", this.autoShieldbreakerEnabled, this.autoShieldbreakerKeybind);
        populateShieldbreakerSettings(this.modules.get("auto_shieldbreaker"));

        syncModuleEntry("auto_stun_slam", this.autoStunSlamEnabled, this.autoStunSlamKeybind);
        populateStunSlamSettings(this.modules.get("auto_stun_slam"));

        syncModuleEntry("auto_totem", this.autoTotemEnabled, this.autoTotemKeybind);
        populateTotemSettings(this.modules.get("auto_totem"));

        syncModuleEntry("auto_cart", this.autoCartEnabled, this.autoCartKeybind);
        populateCartSettings(this.modules.get("auto_cart"));

        syncModuleEntry("auto_anchor", this.autoAnchorEnabled, this.autoAnchorKeybind);
        populateAnchorSettings(this.modules.get("auto_anchor"));

        syncModuleEntry("cart_refill", this.cartRefillEnabled, this.cartRefillKeybind);
        populateCartRefillSettings(this.modules.get("cart_refill"));

        syncModuleEntry("hp_reaper", this.hpReaperEnabled, this.hpReaperKeybind);
        populateHpReaperSettings(this.modules.get("hp_reaper"));

        syncModuleEntry("auto_tool", this.autoToolEnabled, this.autoToolKeybind);
        populateToolSettings(this.modules.get("auto_tool"));

        syncModuleEntry("auto_gg", this.autoGGEnabled, this.autoGGKeybind);
        populateGGSettings(this.modules.get("auto_gg"));

        syncModuleEntry("cart_hud", this.cartHudEnabled, this.cartHudKeybind);
        populateCartHudSettings(this.modules.get("cart_hud"));
    }

    private void syncModuleEntry(String id, boolean enabled, Keybind keybind) {
        ModuleConfigEntry entry = this.modules.computeIfAbsent(id, k -> new ModuleConfigEntry(enabled, keybind));
        entry.enabled = enabled;
        if (keybind != null) {
            if (!keybind.isUnbound()) {
                entry.keybind.copyFrom(keybind);
            } else if (entry.keybind != null && !entry.keybind.isUnbound()) {
                keybind.copyFrom(entry.keybind);
            }
        }
    }

    private void populateMaceSettings(ModuleConfigEntry entry) {
        if (entry == null) return;
        entry.settings.put("source_mode", this.autoMaceSourceMode);
        entry.settings.put("enchant_mode", this.autoMaceEnchantMode);
        entry.settings.put("miss_behavior", this.autoMaceMissBehavior);
        entry.settings.put("restore_delay", this.autoMaceRestoreDelayMs);
        entry.settings.put("miss_chance", this.autoMaceMissChance);
        entry.settings.put("random_delay", this.autoMaceRandomDelay);
        entry.settings.put("legit_mode", this.autoMaceLegitMode);
    }

    private void populateSpearSettings(ModuleConfigEntry entry) {
        if (entry == null) return;
        entry.settings.put("security_mode", this.autoSpearSecurityMode);
        entry.settings.put("priority_mode", this.autoSpearPriorityMode);
        entry.settings.put("restore_delay", this.autoSpearRestoreDelayMs);
        entry.settings.put("miss_chance", this.autoSpearMissChance);
        entry.settings.put("random_delay", this.autoSpearRandomDelay);
        entry.settings.put("trigger_keybind", this.autoSpearTriggerKeybind);
    }

    private void populateShieldbreakerSettings(ModuleConfigEntry entry) {
        if (entry == null) return;
        entry.settings.put("mode", this.autoShieldbreakerMode);
        entry.settings.put("distance", this.autoShieldbreakerDistance);
        entry.settings.put("chance", this.autoShieldbreakerChance);
        entry.settings.put("switch_delay", this.autoShieldbreakerSwitchDelayMs);
        entry.settings.put("restore_delay", this.autoShieldbreakerRestoreDelayMs);
        entry.settings.put("random_delay", this.autoShieldbreakerRandomDelay);
        entry.settings.put("abort_on_manual_switch", this.autoShieldbreakerAbortOnManualSwitch);
        entry.settings.put("legit_mode", this.autoShieldbreakerLegitMode);
    }

    private void populateStunSlamSettings(ModuleConfigEntry entry) {
        if (entry == null) return;
        entry.settings.put("mode", this.autoStunSlamMode);
        entry.settings.put("distance", this.autoStunSlamDistance);
        entry.settings.put("chance", this.autoStunSlamChance);
        entry.settings.put("air_time", this.autoStunSlamAirTimeSec);
        entry.settings.put("axe_delay", this.autoStunSlamAxeDelayMs);
        entry.settings.put("mace_delay", this.autoStunSlamMaceDelayMs);
        entry.settings.put("restore_delay", this.autoStunSlamRestoreDelayMs);
        entry.settings.put("random_delay", this.autoStunSlamRandomDelay);
        entry.settings.put("legit_mode", this.autoStunSlamLegitMode);
    }

    private void populateTotemSettings(ModuleConfigEntry entry) {
        if (entry == null) return;
        entry.settings.put("mode", this.autoTotemMode);
        entry.settings.put("trigger_hearts", this.autoTotemTriggerHearts);
        entry.settings.put("restore_hearts", this.autoTotemRestoreHearts);
        entry.settings.put("chance", this.autoTotemChance);
        entry.settings.put("return_item", this.autoTotemReturnItem);
        entry.settings.put("return_on_pop", this.autoTotemReturnOnPop);
    }

    private void populateCartSettings(ModuleConfigEntry entry) {
        if (entry == null) return;
        entry.settings.put("preset", this.autoCartPreset);
        entry.settings.put("placement_chance", this.autoCartPlacementChance);
        entry.settings.put("max_distance", this.autoCartMaxDistance);
        entry.settings.put("min_delay", this.autoCartMinDelayMs);
        entry.settings.put("max_delay", this.autoCartMaxDelayMs);
        entry.settings.put("allow_self_cart", this.autoCartAllowSelfCart);
        entry.settings.put("allow_pit_placement", this.autoCartAllowPitPlacement);
        entry.settings.put("random_delay", this.autoCartRandomDelay);
        entry.settings.put("rail_delay", this.autoCartRailDelay);
        entry.settings.put("cart_delay", this.autoCartCartDelay);
        entry.settings.put("restore_delay", this.autoCartRestoreDelay);
        entry.settings.put("legit_mode", this.autoCartLegitMode);
    }

    private void populateAnchorSettings(ModuleConfigEntry entry) {
        if (entry == null) return;
        entry.settings.put("preset", this.autoAnchorPreset);
        entry.settings.put("auto_explode", this.autoAnchorAutoExplode);
        entry.settings.put("auto_return", this.autoAnchorAutoReturn);
        entry.settings.put("charge_delay", this.autoAnchorChargeDelay);
        entry.settings.put("explode_delay", this.autoAnchorExplodeDelay);
        entry.settings.put("chance", this.autoAnchorChance);
        entry.settings.put("target_charges", this.autoAnchorTargetCharges);
        entry.settings.put("legit_mode", this.autoAnchorLegitMode);
    }

    private void populateCartRefillSettings(ModuleConfigEntry entry) {
        if (entry == null) return;
        entry.settings.put("delay_ticks", this.cartRefillDelayTicks);
        entry.settings.put("chance", this.cartRefillChance);
        entry.settings.put("auto_close", this.cartRefillAutoClose);
        entry.settings.put("random_delay", this.cartRefillRandomDelay);
        entry.settings.put("legit_mode", this.cartRefillLegitMode);
    }

    private void populateHpReaperSettings(ModuleConfigEntry entry) {
        if (entry == null) return;
        entry.settings.put("mode", this.hpReaperMode);
        entry.settings.put("target_filter", this.hpReaperTargetFilter);
        entry.settings.put("own_hp_x", this.hpReaperOwnHealthX);
        entry.settings.put("own_hp_y", this.hpReaperOwnHealthY);
        entry.settings.put("crosshair_x", this.hpReaperCrosshairTargetX);
        entry.settings.put("crosshair_y", this.hpReaperCrosshairTargetY);
        entry.settings.put("target_hp_x", this.hpReaperTargetHealthX);
        entry.settings.put("target_hp_y", this.hpReaperTargetHealthY);
        entry.settings.put("diff_x", this.hpReaperDiffX);
        entry.settings.put("diff_y", this.hpReaperDiffY);
    }

    private void populateToolSettings(ModuleConfigEntry entry) {
        if (entry == null) return;
        entry.settings.put("combat_guard", this.autoToolCombatGuard);
        entry.settings.put("durability_saver", this.autoToolDurabilitySaver);
        entry.settings.put("durability_threshold", this.autoToolDurabilityThreshold);
        entry.settings.put("prefer_silk_touch", this.autoToolPreferSilkTouch);
        entry.settings.put("restore_previous", this.autoToolRestorePrevious);
        entry.settings.put("legit_mode", this.autoToolLegitMode);
        entry.settings.put("single_slot_mode", this.autoToolSingleSlotMode);
        entry.settings.put("ignore_instant_break", this.autoToolIgnoreInstantBreak);
        entry.settings.put("lock_while_mining", this.autoToolLockWhileMining);
    }

    private void populateGGSettings(ModuleConfigEntry entry) {
        if (entry == null) return;
        entry.settings.put("phrase", this.autoGGPhrase);
        entry.settings.put("send_on_kill", this.autoGGSendOnKill);
        entry.settings.put("send_on_own_death", this.autoGGSendOnOwnDeath);
        entry.settings.put("random_order", this.autoGGRandomOrder);
        entry.settings.put("delay_ms", this.autoGGDelayMs);
        entry.settings.put("menu_keybind", this.autoGGMenuKeybind);
    }

    private void populateCartHudSettings(ModuleConfigEntry entry) {
        if (entry == null) return;
        entry.settings.put("custom_x", this.cartHudCustomX);
        entry.settings.put("custom_y", this.cartHudCustomY);
    }

    private static String getSettingString(java.util.Map<String, Object> map, String key, String def) {
        if (map == null) return def;
        Object v = map.get(key);
        if (v == null) return def;
        return String.valueOf(v);
    }

    private static double getSettingDouble(java.util.Map<String, Object> map, String key, double def) {
        if (map == null) return def;
        Object v = map.get(key);
        if (v instanceof Number n) {
            double d = n.doubleValue();
            return (Double.isNaN(d) || Double.isInfinite(d)) ? def : d;
        }
        if (v instanceof String s) {
            try {
                double d = Double.parseDouble(s.trim());
                return (Double.isNaN(d) || Double.isInfinite(d)) ? def : d;
            } catch (Exception ignored) {}
        }
        return def;
    }

    private static int getSettingInt(java.util.Map<String, Object> map, String key, int def) {
        if (map == null) return def;
        Object v = map.get(key);
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (Exception ignored) {}
        }
        return def;
    }

    private static boolean getSettingBoolean(java.util.Map<String, Object> map, String key, boolean def) {
        if (map == null) return def;
        Object v = map.get(key);
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return Boolean.parseBoolean(s.trim());
        return def;
    }

    private static Keybind getSettingKeybind(java.util.Map<String, Object> map, String key, Keybind def) {
        if (map == null) return def;
        Object v = map.get(key);
        if (v instanceof Keybind kb) return kb;
        if (v instanceof com.google.gson.internal.LinkedTreeMap<?, ?> tree) {
            try {
                int code = ((Number) tree.get("keyCode")).intValue();
                boolean ctrl = Boolean.TRUE.equals(tree.get("ctrl"));
                boolean shift = Boolean.TRUE.equals(tree.get("shift"));
                boolean alt = Boolean.TRUE.equals(tree.get("alt"));
                return new Keybind(code, ctrl, shift, alt);
            } catch (Exception ignored) {}
        }
        return def;
    }

    /**
     * Looks up module entry handling case-insensitivity and AutoStun aliases.
     */
    public ModuleConfigEntry getModuleEntry(String id) {
        if (this.modules == null || id == null) return null;
        String clean = id.replace("_", "").toLowerCase(java.util.Locale.ROOT);
        if ("autostunslam".equals(clean) || "autostunslime".equals(clean)) {
            ModuleConfigEntry direct = this.modules.get("auto_stun_slam");
            if (direct != null) return direct;
            direct = this.modules.get("auto_stun_slime");
            if (direct != null) return direct;
        }
        ModuleConfigEntry direct = this.modules.get(id);
        if (direct != null) return direct;
        for (java.util.Map.Entry<String, ModuleConfigEntry> e : this.modules.entrySet()) {
            if (e.getKey().equalsIgnoreCase(id) || e.getKey().replace("_", "").equalsIgnoreCase(clean)) {
                return e.getValue();
            }
        }
        return null;
    }

    public void syncFromModuleEntries() {
        if (this.modules == null) return;
        ModuleConfigEntry mace = getModuleEntry("auto_mace");
        if (mace != null) {
            this.autoMaceEnabled = mace.enabled;
            if (mace.keybind != null) this.autoMaceKeybind.copyFrom(mace.keybind);
            if (mace.settings != null && !mace.settings.isEmpty()) {
                this.autoMaceSourceMode = getSettingString(mace.settings, "source_mode", this.autoMaceSourceMode);
                this.autoMaceEnchantMode = getSettingString(mace.settings, "enchant_mode", this.autoMaceEnchantMode);
                this.autoMaceMissBehavior = getSettingString(mace.settings, "miss_behavior", this.autoMaceMissBehavior);
                this.autoMaceRestoreDelayMs = getSettingDouble(mace.settings, "restore_delay", this.autoMaceRestoreDelayMs);
                this.autoMaceMissChance = getSettingDouble(mace.settings, "miss_chance", this.autoMaceMissChance);
                this.autoMaceRandomDelay = getSettingBoolean(mace.settings, "random_delay", this.autoMaceRandomDelay);
                this.autoMaceLegitMode = getSettingBoolean(mace.settings, "legit_mode", this.autoMaceLegitMode);
            }
        }
        ModuleConfigEntry spear = getModuleEntry("auto_spear");
        if (spear != null) {
            this.autoSpearEnabled = spear.enabled;
            if (spear.keybind != null) this.autoSpearKeybind.copyFrom(spear.keybind);
            if (spear.settings != null && !spear.settings.isEmpty()) {
                this.autoSpearSecurityMode = getSettingString(spear.settings, "security_mode", this.autoSpearSecurityMode);
                this.autoSpearPriorityMode = getSettingString(spear.settings, "priority_mode", this.autoSpearPriorityMode);
                this.autoSpearRestoreDelayMs = getSettingDouble(spear.settings, "restore_delay", this.autoSpearRestoreDelayMs);
                this.autoSpearMissChance = getSettingDouble(spear.settings, "miss_chance", this.autoSpearMissChance);
                this.autoSpearRandomDelay = getSettingBoolean(spear.settings, "random_delay", this.autoSpearRandomDelay);
                Keybind tkb = getSettingKeybind(spear.settings, "trigger_keybind", null);
                if (tkb != null) this.autoSpearTriggerKeybind.copyFrom(tkb);
            }
        }
        ModuleConfigEntry sb = getModuleEntry("auto_shieldbreaker");
        if (sb != null) {
            this.autoShieldbreakerEnabled = sb.enabled;
            if (sb.keybind != null) this.autoShieldbreakerKeybind.copyFrom(sb.keybind);
            if (sb.settings != null && !sb.settings.isEmpty()) {
                this.autoShieldbreakerMode = getSettingString(sb.settings, "mode", this.autoShieldbreakerMode);
                this.autoShieldbreakerDistance = getSettingDouble(sb.settings, "distance", this.autoShieldbreakerDistance);
                this.autoShieldbreakerChance = getSettingDouble(sb.settings, "chance", this.autoShieldbreakerChance);
                this.autoShieldbreakerSwitchDelayMs = getSettingDouble(sb.settings, "switch_delay", this.autoShieldbreakerSwitchDelayMs);
                this.autoShieldbreakerRestoreDelayMs = getSettingDouble(sb.settings, "restore_delay", this.autoShieldbreakerRestoreDelayMs);
                this.autoShieldbreakerRandomDelay = getSettingBoolean(sb.settings, "random_delay", this.autoShieldbreakerRandomDelay);
                this.autoShieldbreakerAbortOnManualSwitch = getSettingBoolean(sb.settings, "abort_on_manual_switch", this.autoShieldbreakerAbortOnManualSwitch);
                this.autoShieldbreakerLegitMode = getSettingBoolean(sb.settings, "legit_mode", this.autoShieldbreakerLegitMode);
            }
        }
        ModuleConfigEntry slam = getModuleEntry("auto_stun_slam");
        if (slam != null) {
            this.autoStunSlamEnabled = slam.enabled;
            if (slam.keybind != null) this.autoStunSlamKeybind.copyFrom(slam.keybind);
            if (slam.settings != null && !slam.settings.isEmpty()) {
                this.autoStunSlamMode = getSettingString(slam.settings, "mode", this.autoStunSlamMode);
                this.autoStunSlamDistance = getSettingDouble(slam.settings, "distance", this.autoStunSlamDistance);
                this.autoStunSlamChance = getSettingDouble(slam.settings, "chance", this.autoStunSlamChance);
                this.autoStunSlamAirTimeSec = getSettingDouble(slam.settings, "air_time", this.autoStunSlamAirTimeSec);
                this.autoStunSlamAxeDelayMs = getSettingDouble(slam.settings, "axe_delay", this.autoStunSlamAxeDelayMs);
                this.autoStunSlamMaceDelayMs = getSettingDouble(slam.settings, "mace_delay", this.autoStunSlamMaceDelayMs);
                this.autoStunSlamRestoreDelayMs = getSettingDouble(slam.settings, "restore_delay", this.autoStunSlamRestoreDelayMs);
                this.autoStunSlamRandomDelay = getSettingBoolean(slam.settings, "random_delay", this.autoStunSlamRandomDelay);
                this.autoStunSlamLegitMode = getSettingBoolean(slam.settings, "legit_mode", this.autoStunSlamLegitMode);
            }
        }
        ModuleConfigEntry totem = getModuleEntry("auto_totem");
        if (totem != null) {
            this.autoTotemEnabled = totem.enabled;
            if (totem.keybind != null) this.autoTotemKeybind.copyFrom(totem.keybind);
            if (totem.settings != null && !totem.settings.isEmpty()) {
                this.autoTotemMode = getSettingString(totem.settings, "mode", this.autoTotemMode);
                this.autoTotemTriggerHearts = getSettingDouble(totem.settings, "trigger_hearts", this.autoTotemTriggerHearts);
                this.autoTotemRestoreHearts = getSettingDouble(totem.settings, "restore_hearts", this.autoTotemRestoreHearts);
                this.autoTotemChance = getSettingDouble(totem.settings, "chance", this.autoTotemChance);
                this.autoTotemReturnItem = getSettingBoolean(totem.settings, "return_item", this.autoTotemReturnItem);
                this.autoTotemReturnOnPop = getSettingBoolean(totem.settings, "return_on_pop", this.autoTotemReturnOnPop);
            }
        }
        ModuleConfigEntry cart = getModuleEntry("auto_cart");
        if (cart != null) {
            this.autoCartEnabled = cart.enabled;
            if (cart.keybind != null) this.autoCartKeybind.copyFrom(cart.keybind);
            if (cart.settings != null && !cart.settings.isEmpty()) {
                this.autoCartPreset = getSettingString(cart.settings, "preset", this.autoCartPreset);
                this.autoCartPlacementChance = getSettingDouble(cart.settings, "placement_chance", this.autoCartPlacementChance);
                this.autoCartMaxDistance = getSettingDouble(cart.settings, "max_distance", this.autoCartMaxDistance);
                this.autoCartMinDelayMs = getSettingDouble(cart.settings, "min_delay", this.autoCartMinDelayMs);
                this.autoCartMaxDelayMs = getSettingDouble(cart.settings, "max_delay", this.autoCartMaxDelayMs);
                this.autoCartAllowSelfCart = getSettingBoolean(cart.settings, "allow_self_cart", this.autoCartAllowSelfCart);
                this.autoCartAllowPitPlacement = getSettingBoolean(cart.settings, "allow_pit_placement", this.autoCartAllowPitPlacement);
                this.autoCartRandomDelay = getSettingBoolean(cart.settings, "random_delay", this.autoCartRandomDelay);
                this.autoCartRailDelay = getSettingDouble(cart.settings, "rail_delay", this.autoCartRailDelay);
                this.autoCartCartDelay = getSettingDouble(cart.settings, "cart_delay", this.autoCartCartDelay);
                this.autoCartRestoreDelay = getSettingDouble(cart.settings, "restore_delay", this.autoCartRestoreDelay);
                this.autoCartLegitMode = getSettingBoolean(cart.settings, "legit_mode", this.autoCartLegitMode);
            }
        }
        ModuleConfigEntry anchor = getModuleEntry("auto_anchor");
        if (anchor != null) {
            this.autoAnchorEnabled = anchor.enabled;
            if (anchor.keybind != null) this.autoAnchorKeybind.copyFrom(anchor.keybind);
            if (anchor.settings != null && !anchor.settings.isEmpty()) {
                this.autoAnchorPreset = getSettingString(anchor.settings, "preset", this.autoAnchorPreset);
                this.autoAnchorAutoExplode = getSettingBoolean(anchor.settings, "auto_explode", this.autoAnchorAutoExplode);
                this.autoAnchorAutoReturn = getSettingBoolean(anchor.settings, "auto_return", this.autoAnchorAutoReturn);
                this.autoAnchorChargeDelay = getSettingDouble(anchor.settings, "charge_delay", this.autoAnchorChargeDelay);
                this.autoAnchorExplodeDelay = getSettingDouble(anchor.settings, "explode_delay", this.autoAnchorExplodeDelay);
                this.autoAnchorChance = getSettingDouble(anchor.settings, "chance", this.autoAnchorChance);
                this.autoAnchorTargetCharges = getSettingDouble(anchor.settings, "target_charges", this.autoAnchorTargetCharges);
                this.autoAnchorLegitMode = getSettingBoolean(anchor.settings, "legit_mode", this.autoAnchorLegitMode);
            }
        }
        ModuleConfigEntry refill = getModuleEntry("cart_refill");
        if (refill != null) {
            this.cartRefillEnabled = refill.enabled;
            if (refill.keybind != null) this.cartRefillKeybind.copyFrom(refill.keybind);
            if (refill.settings != null && !refill.settings.isEmpty()) {
                this.cartRefillDelayTicks = getSettingDouble(refill.settings, "delay_ticks", this.cartRefillDelayTicks);
                this.cartRefillChance = getSettingDouble(refill.settings, "chance", this.cartRefillChance);
                this.cartRefillAutoClose = getSettingBoolean(refill.settings, "auto_close", this.cartRefillAutoClose);
                this.cartRefillRandomDelay = getSettingBoolean(refill.settings, "random_delay", this.cartRefillRandomDelay);
                this.cartRefillLegitMode = getSettingBoolean(refill.settings, "legit_mode", this.cartRefillLegitMode);
            }
        }
        ModuleConfigEntry reaper = getModuleEntry("hp_reaper");
        if (reaper != null) {
            this.hpReaperEnabled = reaper.enabled;
            if (reaper.keybind != null) this.hpReaperKeybind.copyFrom(reaper.keybind);
            if (reaper.settings != null && !reaper.settings.isEmpty()) {
                this.hpReaperMode = getSettingString(reaper.settings, "mode", this.hpReaperMode);
                this.hpReaperTargetFilter = getSettingString(reaper.settings, "target_filter", this.hpReaperTargetFilter);
                this.hpReaperOwnHealthX = getSettingInt(reaper.settings, "own_hp_x", this.hpReaperOwnHealthX);
                this.hpReaperOwnHealthY = getSettingInt(reaper.settings, "own_hp_y", this.hpReaperOwnHealthY);
                this.hpReaperCrosshairTargetX = getSettingInt(reaper.settings, "crosshair_x", this.hpReaperCrosshairTargetX);
                this.hpReaperCrosshairTargetY = getSettingInt(reaper.settings, "crosshair_y", this.hpReaperCrosshairTargetY);
                this.hpReaperTargetHealthX = getSettingInt(reaper.settings, "target_hp_x", this.hpReaperTargetHealthX);
                this.hpReaperTargetHealthY = getSettingInt(reaper.settings, "target_hp_y", this.hpReaperTargetHealthY);
                this.hpReaperDiffX = getSettingInt(reaper.settings, "diff_x", this.hpReaperDiffX);
                this.hpReaperDiffY = getSettingInt(reaper.settings, "diff_y", this.hpReaperDiffY);
            }
        }
        ModuleConfigEntry tool = getModuleEntry("auto_tool");
        if (tool != null) {
            this.autoToolEnabled = tool.enabled;
            if (tool.keybind != null) this.autoToolKeybind.copyFrom(tool.keybind);
            if (tool.settings != null && !tool.settings.isEmpty()) {
                this.autoToolCombatGuard = getSettingBoolean(tool.settings, "combat_guard", this.autoToolCombatGuard);
                this.autoToolDurabilitySaver = getSettingBoolean(tool.settings, "durability_saver", this.autoToolDurabilitySaver);
                this.autoToolDurabilityThreshold = getSettingDouble(tool.settings, "durability_threshold", this.autoToolDurabilityThreshold);
                this.autoToolPreferSilkTouch = getSettingBoolean(tool.settings, "prefer_silk_touch", this.autoToolPreferSilkTouch);
                this.autoToolRestorePrevious = getSettingBoolean(tool.settings, "restore_previous", this.autoToolRestorePrevious);
                this.autoToolLegitMode = getSettingBoolean(tool.settings, "legit_mode", this.autoToolLegitMode);
                this.autoToolSingleSlotMode = getSettingBoolean(tool.settings, "single_slot_mode", this.autoToolSingleSlotMode);
                this.autoToolIgnoreInstantBreak = getSettingBoolean(tool.settings, "ignore_instant_break", this.autoToolIgnoreInstantBreak);
                this.autoToolLockWhileMining = getSettingBoolean(tool.settings, "lock_while_mining", this.autoToolLockWhileMining);
            }
        }
        ModuleConfigEntry gg = getModuleEntry("auto_gg");
        if (gg != null) {
            this.autoGGEnabled = gg.enabled;
            if (gg.keybind != null) this.autoGGKeybind.copyFrom(gg.keybind);
            if (gg.settings != null && !gg.settings.isEmpty()) {
                this.autoGGPhrase = getSettingString(gg.settings, "phrase", this.autoGGPhrase);
                this.autoGGSendOnKill = getSettingBoolean(gg.settings, "send_on_kill", this.autoGGSendOnKill);
                this.autoGGSendOnOwnDeath = getSettingBoolean(gg.settings, "send_on_own_death", this.autoGGSendOnOwnDeath);
                this.autoGGRandomOrder = getSettingBoolean(gg.settings, "random_order", this.autoGGRandomOrder);
                this.autoGGDelayMs = getSettingDouble(gg.settings, "delay_ms", this.autoGGDelayMs);
                Keybind mkb = getSettingKeybind(gg.settings, "menu_keybind", null);
                if (mkb != null) this.autoGGMenuKeybind.copyFrom(mkb);
            }
        }
        ModuleConfigEntry hud = getModuleEntry("cart_hud");
        if (hud != null) {
            this.cartHudEnabled = hud.enabled;
            if (hud.keybind != null) this.cartHudKeybind.copyFrom(hud.keybind);
            if (hud.settings != null && !hud.settings.isEmpty()) {
                this.cartHudCustomX = getSettingInt(hud.settings, "custom_x", this.cartHudCustomX);
                this.cartHudCustomY = getSettingInt(hud.settings, "custom_y", this.cartHudCustomY);
            }
        }
    }

    private static double clampSanitize(double val, double min, double max, double def) {
        if (Double.isNaN(val) || Double.isInfinite(val)) return def;
        return Math.clamp(val, min, max);
    }

    /**
     * Creates a detached deep copy of this configuration.
     */
    public ActivityConfig copy() {
        this.syncClientSection();
        this.syncModuleConfigEntries();
        ActivityConfig copy = new ActivityConfig();
        copy.configVersion = this.configVersion;
        copy.pinnedModules = new java.util.ArrayList<>(this.pinnedModules);
        if (this.modules != null) {
            copy.modules = new java.util.LinkedHashMap<>();
            for (java.util.Map.Entry<String, ModuleConfigEntry> entry : this.modules.entrySet()) {
                if (entry.getValue() != null) {
                    ModuleConfigEntry e = new ModuleConfigEntry(entry.getValue().enabled, entry.getValue().keybind);
                    e.settings.putAll(entry.getValue().settings);
                    copy.modules.put(entry.getKey(), e);
                }
            }
        }

        // Combat
        copy.autoMaceEnabled = this.autoMaceEnabled;
        copy.autoMaceKeybind.copyFrom(this.autoMaceKeybind);
        copy.autoMaceSourceMode = this.autoMaceSourceMode;
        copy.autoMaceEnchantMode = this.autoMaceEnchantMode;
        copy.autoMaceMissBehavior = this.autoMaceMissBehavior;
        copy.autoMaceRestoreDelayMs = this.autoMaceRestoreDelayMs;
        copy.autoMaceLegitMode = this.autoMaceLegitMode;
        copy.autoMaceMissChance = this.autoMaceMissChance;
        copy.autoMaceRandomDelay = this.autoMaceRandomDelay;

        copy.autoSpearEnabled = this.autoSpearEnabled;
        copy.autoSpearKeybind.copyFrom(this.autoSpearKeybind);
        copy.autoSpearTriggerKeybind.copyFrom(this.autoSpearTriggerKeybind);
        copy.autoSpearSecurityMode = this.autoSpearSecurityMode;
        copy.autoSpearPriorityMode = this.autoSpearPriorityMode;
        copy.autoSpearRestoreDelayMs = this.autoSpearRestoreDelayMs;
        copy.autoSpearMissChance = this.autoSpearMissChance;
        copy.autoSpearRandomDelay = this.autoSpearRandomDelay;

        copy.autoShieldbreakerEnabled = this.autoShieldbreakerEnabled;
        copy.autoShieldbreakerKeybind.copyFrom(this.autoShieldbreakerKeybind);
        copy.autoShieldbreakerMode = this.autoShieldbreakerMode;
        copy.autoShieldbreakerDistance = this.autoShieldbreakerDistance;
        copy.autoShieldbreakerChance = this.autoShieldbreakerChance;
        copy.autoShieldbreakerSwitchDelayMs = this.autoShieldbreakerSwitchDelayMs;
        copy.autoShieldbreakerRestoreDelayMs = this.autoShieldbreakerRestoreDelayMs;
        copy.autoShieldbreakerRandomDelay = this.autoShieldbreakerRandomDelay;
        copy.autoShieldbreakerAbortOnManualSwitch = this.autoShieldbreakerAbortOnManualSwitch;
        copy.autoShieldbreakerLegitMode = this.autoShieldbreakerLegitMode;

        copy.autoStunSlamEnabled = this.autoStunSlamEnabled;
        copy.autoStunSlamKeybind.copyFrom(this.autoStunSlamKeybind);
        copy.autoStunSlamMode = this.autoStunSlamMode;
        copy.autoStunSlamDistance = this.autoStunSlamDistance;
        copy.autoStunSlamChance = this.autoStunSlamChance;
        copy.autoStunSlamAirTimeSec = this.autoStunSlamAirTimeSec;
        copy.autoStunSlamAxeDelayMs = this.autoStunSlamAxeDelayMs;
        copy.autoStunSlamMaceDelayMs = this.autoStunSlamMaceDelayMs;
        copy.autoStunSlamRestoreDelayMs = this.autoStunSlamRestoreDelayMs;
        copy.autoStunSlamRandomDelay = this.autoStunSlamRandomDelay;
        copy.autoStunSlamLegitMode = this.autoStunSlamLegitMode;

        // Defense
        copy.autoTotemEnabled = this.autoTotemEnabled;
        copy.autoTotemKeybind.copyFrom(this.autoTotemKeybind);
        copy.autoTotemMode = this.autoTotemMode;
        copy.autoTotemTriggerHearts = this.autoTotemTriggerHearts;
        copy.autoTotemRestoreHearts = this.autoTotemRestoreHearts;
        copy.autoTotemChance = this.autoTotemChance;
        copy.autoTotemReturnItem = this.autoTotemReturnItem;
        copy.autoTotemReturnOnPop = this.autoTotemReturnOnPop;

        copy.autoCartEnabled = this.autoCartEnabled;
        copy.autoCartKeybind.copyFrom(this.autoCartKeybind);
        copy.autoCartPreset = this.autoCartPreset;
        copy.autoCartPlacementChance = this.autoCartPlacementChance;
        copy.autoCartMaxDistance = this.autoCartMaxDistance;
        copy.autoCartMinDelayMs = this.autoCartMinDelayMs;
        copy.autoCartMaxDelayMs = this.autoCartMaxDelayMs;
        copy.autoCartAllowSelfCart = this.autoCartAllowSelfCart;
        copy.autoCartAllowPitPlacement = this.autoCartAllowPitPlacement;
        copy.autoCartRandomDelay = this.autoCartRandomDelay;
        copy.autoCartRailDelay = this.autoCartRailDelay;
        copy.autoCartCartDelay = this.autoCartCartDelay;
        copy.autoCartRestoreDelay = this.autoCartRestoreDelay;
        copy.autoCartLegitMode = this.autoCartLegitMode;

        copy.autoAnchorEnabled = this.autoAnchorEnabled;
        copy.autoAnchorKeybind.copyFrom(this.autoAnchorKeybind);
        copy.autoAnchorPreset = this.autoAnchorPreset;
        copy.autoAnchorAutoExplode = this.autoAnchorAutoExplode;
        copy.autoAnchorAutoReturn = this.autoAnchorAutoReturn;
        copy.autoAnchorChargeDelay = this.autoAnchorChargeDelay;
        copy.autoAnchorExplodeDelay = this.autoAnchorExplodeDelay;
        copy.autoAnchorChance = this.autoAnchorChance;
        copy.autoAnchorTargetCharges = this.autoAnchorTargetCharges;
        copy.autoAnchorLegitMode = this.autoAnchorLegitMode;

        copy.cartRefillEnabled = this.cartRefillEnabled;
        copy.cartRefillKeybind.copyFrom(this.cartRefillKeybind);
        copy.cartRefillDelayTicks = this.cartRefillDelayTicks;
        copy.cartRefillChance = this.cartRefillChance;
        copy.cartRefillAutoClose = this.cartRefillAutoClose;
        copy.cartRefillRandomDelay = this.cartRefillRandomDelay;
        copy.cartRefillLegitMode = this.cartRefillLegitMode;

        // Utility
        copy.hpReaperEnabled = this.hpReaperEnabled;
        copy.hpReaperKeybind.copyFrom(this.hpReaperKeybind);
        copy.hpReaperMode = this.hpReaperMode;
        copy.hpReaperTargetFilter = this.hpReaperTargetFilter;
        copy.hpReaperOwnHealthX = this.hpReaperOwnHealthX;
        copy.hpReaperOwnHealthY = this.hpReaperOwnHealthY;
        copy.hpReaperCrosshairTargetX = this.hpReaperCrosshairTargetX;
        copy.hpReaperCrosshairTargetY = this.hpReaperCrosshairTargetY;
        copy.hpReaperTargetHealthX = this.hpReaperTargetHealthX;
        copy.hpReaperTargetHealthY = this.hpReaperTargetHealthY;
        copy.hpReaperDiffX = this.hpReaperDiffX;
        copy.hpReaperDiffY = this.hpReaperDiffY;

        copy.autoToolEnabled = this.autoToolEnabled;
        copy.autoToolKeybind.copyFrom(this.autoToolKeybind);
        copy.autoToolCombatGuard = this.autoToolCombatGuard;
        copy.autoToolDurabilitySaver = this.autoToolDurabilitySaver;
        copy.autoToolDurabilityThreshold = this.autoToolDurabilityThreshold;
        copy.autoToolPreferSilkTouch = this.autoToolPreferSilkTouch;
        copy.autoToolRestorePrevious = this.autoToolRestorePrevious;
        copy.autoToolLegitMode = this.autoToolLegitMode;
        copy.autoToolSingleSlotMode = this.autoToolSingleSlotMode;
        copy.autoToolIgnoreInstantBreak = this.autoToolIgnoreInstantBreak;
        copy.autoToolLockWhileMining = this.autoToolLockWhileMining;

        copy.autoGGEnabled = this.autoGGEnabled;
        copy.autoGGKeybind.copyFrom(this.autoGGKeybind);
        copy.autoGGMenuKeybind.copyFrom(this.autoGGMenuKeybind);
        copy.autoGGPhrase = this.autoGGPhrase;
        copy.autoGGSendOnKill = this.autoGGSendOnKill;
        copy.autoGGSendOnOwnDeath = this.autoGGSendOnOwnDeath;
        copy.autoGGRandomOrder = this.autoGGRandomOrder;
        copy.autoGGDelayMs = this.autoGGDelayMs;

        copy.cartHudEnabled = this.cartHudEnabled;
        copy.cartHudKeybind.copyFrom(this.cartHudKeybind);
        copy.cartHudCustomX = this.cartHudCustomX;
        copy.cartHudCustomY = this.cartHudCustomY;

        // HUD & Profiles
        copy.overlayEnabled = this.overlayEnabled;
        copy.darkThemeEnabled = this.darkThemeEnabled;
        copy.hudPosition = this.hudPosition;
        copy.overlayOpacity = this.overlayOpacity;
        copy.autoHideOnChat = this.autoHideOnChat;
        copy.hideInF3 = this.hideInF3;
        copy.searchFilter = this.searchFilter;
        copy.filterCategory = this.filterCategory;
        copy.matchCase = this.matchCase;

        copy.activeProfile = this.activeProfile;
        copy.themeVariant = this.themeVariant;
        copy.compactMode = this.compactMode;
        copy.tooltipsEnabled = this.tooltipsEnabled;
        copy.showKeyHints = this.showKeyHints;
        copy.smoothTransitions = this.smoothTransitions;
        copy.soundVolume = this.soundVolume;
        copy.audioClicks = this.audioClicks;
        copy.customPrefix = this.customPrefix;
        copy.toastStyle = this.toastStyle;

        copy.showCoordinates = this.showCoordinates;
        copy.showFps = this.showFps;
        copy.showBiome = this.showBiome;
        copy.showWorldTime = this.showWorldTime;
        copy.showDirection = this.showDirection;
        copy.coordFormat = this.coordFormat;
        copy.hudPadding = this.hudPadding;
        copy.customTitle = this.customTitle;
        copy.textShadow = this.textShadow;

        copy.debugLogging = this.debugLogging;
        copy.profilerActive = this.profilerActive;
        copy.asyncTickEnabled = this.asyncTickEnabled;
        copy.logLevel = this.logLevel;
        copy.benchmarksEnabled = this.benchmarksEnabled;
        copy.maxCacheEntries = this.maxCacheEntries;
        copy.scissorOpt = this.scissorOpt;
        copy.filterRegex = this.filterRegex;
        copy.gcPolicy = this.gcPolicy;

        copy.fontFamily = this.fontFamily;
        copy.typographySize = this.typographySize;
        copy.windowOpacity = this.windowOpacity;
        copy.panelOpacity = this.panelOpacity;
        copy.glassEffect = this.glassEffect;
        copy.windowPosX = this.windowPosX;
        copy.windowPosY = this.windowPosY;
        copy.windowWidth = this.windowWidth;
        copy.windowHeight = this.windowHeight;
        copy.windowMaximized = this.windowMaximized;
        copy.unmaximizedX = this.unmaximizedX;
        copy.unmaximizedY = this.unmaximizedY;
        copy.unmaximizedWidth = this.unmaximizedWidth;
        copy.unmaximizedHeight = this.unmaximizedHeight;
        copy.soundEnabled = this.soundEnabled;
        copy.soundProfile = this.soundProfile;
        copy.sliderSoundEnabled = this.sliderSoundEnabled;
        copy.animationsEnabled = this.animationsEnabled;
        copy.spatialOpenAnimation = this.spatialOpenAnimation;

        copy.syncClientSection();
        copy.syncModuleConfigEntries();
        copy.legacyMigrationDone = this.legacyMigrationDone;
        copy.legacyMigrationVersion = this.legacyMigrationVersion;

        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActivityConfig that)) return false;

        return this.configVersion == that.configVersion &&
               // Combat
               this.autoMaceEnabled == that.autoMaceEnabled &&
               this.autoMaceLegitMode == that.autoMaceLegitMode &&
               this.autoMaceRandomDelay == that.autoMaceRandomDelay &&
               Double.compare(this.autoMaceRestoreDelayMs, that.autoMaceRestoreDelayMs) == 0 &&
               Double.compare(this.autoMaceMissChance, that.autoMaceMissChance) == 0 &&
               Objects.equals(this.autoMaceKeybind, that.autoMaceKeybind) &&
               Objects.equals(this.autoMaceSourceMode, that.autoMaceSourceMode) &&
               Objects.equals(this.autoMaceEnchantMode, that.autoMaceEnchantMode) &&
               Objects.equals(this.autoMaceMissBehavior, that.autoMaceMissBehavior) &&

               this.autoSpearEnabled == that.autoSpearEnabled &&
               this.autoSpearRandomDelay == that.autoSpearRandomDelay &&
               Double.compare(this.autoSpearRestoreDelayMs, that.autoSpearRestoreDelayMs) == 0 &&
               Double.compare(this.autoSpearMissChance, that.autoSpearMissChance) == 0 &&
               Objects.equals(this.autoSpearKeybind, that.autoSpearKeybind) &&
               Objects.equals(this.autoSpearTriggerKeybind, that.autoSpearTriggerKeybind) &&
               Objects.equals(this.autoSpearSecurityMode, that.autoSpearSecurityMode) &&
               Objects.equals(this.autoSpearPriorityMode, that.autoSpearPriorityMode) &&

               this.autoShieldbreakerEnabled == that.autoShieldbreakerEnabled &&
               this.autoShieldbreakerLegitMode == that.autoShieldbreakerLegitMode &&
               this.autoShieldbreakerRandomDelay == that.autoShieldbreakerRandomDelay &&
               this.autoShieldbreakerAbortOnManualSwitch == that.autoShieldbreakerAbortOnManualSwitch &&
               Double.compare(this.autoShieldbreakerDistance, that.autoShieldbreakerDistance) == 0 &&
               Double.compare(this.autoShieldbreakerChance, that.autoShieldbreakerChance) == 0 &&
               Double.compare(this.autoShieldbreakerSwitchDelayMs, that.autoShieldbreakerSwitchDelayMs) == 0 &&
               Double.compare(this.autoShieldbreakerRestoreDelayMs, that.autoShieldbreakerRestoreDelayMs) == 0 &&
               Objects.equals(this.autoShieldbreakerKeybind, that.autoShieldbreakerKeybind) &&
               Objects.equals(this.autoShieldbreakerMode, that.autoShieldbreakerMode) &&

               Objects.equals(this.pinnedModules, that.pinnedModules) &&
               this.autoStunSlamEnabled == that.autoStunSlamEnabled &&
               this.autoStunSlamLegitMode == that.autoStunSlamLegitMode &&
               this.autoStunSlamRandomDelay == that.autoStunSlamRandomDelay &&
               Double.compare(this.autoStunSlamDistance, that.autoStunSlamDistance) == 0 &&
               Double.compare(this.autoStunSlamChance, that.autoStunSlamChance) == 0 &&
               Double.compare(this.autoStunSlamAirTimeSec, that.autoStunSlamAirTimeSec) == 0 &&
               Double.compare(this.autoStunSlamAxeDelayMs, that.autoStunSlamAxeDelayMs) == 0 &&
               Double.compare(this.autoStunSlamMaceDelayMs, that.autoStunSlamMaceDelayMs) == 0 &&
               Double.compare(this.autoStunSlamRestoreDelayMs, that.autoStunSlamRestoreDelayMs) == 0 &&
               Objects.equals(this.autoStunSlamKeybind, that.autoStunSlamKeybind) &&
               Objects.equals(this.autoStunSlamMode, that.autoStunSlamMode) &&

                // Defense
                this.autoTotemEnabled == that.autoTotemEnabled &&
                this.autoTotemReturnItem == that.autoTotemReturnItem &&
                this.autoTotemReturnOnPop == that.autoTotemReturnOnPop &&
                Double.compare(this.autoTotemTriggerHearts, that.autoTotemTriggerHearts) == 0 &&
                Double.compare(this.autoTotemRestoreHearts, that.autoTotemRestoreHearts) == 0 &&
                Double.compare(this.autoTotemChance, that.autoTotemChance) == 0 &&
                Objects.equals(this.autoTotemKeybind, that.autoTotemKeybind) &&
                Objects.equals(this.autoTotemMode, that.autoTotemMode) &&

                this.autoCartEnabled == that.autoCartEnabled &&
                this.autoCartAllowSelfCart == that.autoCartAllowSelfCart &&
                this.autoCartAllowPitPlacement == that.autoCartAllowPitPlacement &&
                this.autoCartRandomDelay == that.autoCartRandomDelay &&
                this.autoCartLegitMode == that.autoCartLegitMode &&
                Double.compare(this.autoCartPlacementChance, that.autoCartPlacementChance) == 0 &&
                Double.compare(this.autoCartMaxDistance, that.autoCartMaxDistance) == 0 &&
                Double.compare(this.autoCartMinDelayMs, that.autoCartMinDelayMs) == 0 &&
                Double.compare(this.autoCartMaxDelayMs, that.autoCartMaxDelayMs) == 0 &&
                Double.compare(this.autoCartRailDelay, that.autoCartRailDelay) == 0 &&
                Double.compare(this.autoCartCartDelay, that.autoCartCartDelay) == 0 &&
                Double.compare(this.autoCartRestoreDelay, that.autoCartRestoreDelay) == 0 &&
                Objects.equals(this.autoCartKeybind, that.autoCartKeybind) &&
                Objects.equals(this.autoCartPreset, that.autoCartPreset) &&

                this.autoAnchorEnabled == that.autoAnchorEnabled &&
                this.autoAnchorAutoExplode == that.autoAnchorAutoExplode &&
                this.autoAnchorAutoReturn == that.autoAnchorAutoReturn &&
                this.autoAnchorLegitMode == that.autoAnchorLegitMode &&
                Double.compare(this.autoAnchorChargeDelay, that.autoAnchorChargeDelay) == 0 &&
                Double.compare(this.autoAnchorExplodeDelay, that.autoAnchorExplodeDelay) == 0 &&
                Double.compare(this.autoAnchorChance, that.autoAnchorChance) == 0 &&
                Double.compare(this.autoAnchorTargetCharges, that.autoAnchorTargetCharges) == 0 &&
                Objects.equals(this.autoAnchorKeybind, that.autoAnchorKeybind) &&
                Objects.equals(this.autoAnchorPreset, that.autoAnchorPreset) &&

                this.cartRefillEnabled == that.cartRefillEnabled &&
                this.cartRefillLegitMode == that.cartRefillLegitMode &&
                this.cartRefillAutoClose == that.cartRefillAutoClose &&
                this.cartRefillRandomDelay == that.cartRefillRandomDelay &&
                Double.compare(this.cartRefillDelayTicks, that.cartRefillDelayTicks) == 0 &&
                Double.compare(this.cartRefillChance, that.cartRefillChance) == 0 &&
                Objects.equals(this.cartRefillKeybind, that.cartRefillKeybind) &&

               // Utility
               this.hpReaperEnabled == that.hpReaperEnabled &&
               this.hpReaperOwnHealthX == that.hpReaperOwnHealthX &&
               this.hpReaperOwnHealthY == that.hpReaperOwnHealthY &&
               this.hpReaperCrosshairTargetX == that.hpReaperCrosshairTargetX &&
               this.hpReaperCrosshairTargetY == that.hpReaperCrosshairTargetY &&
               this.hpReaperTargetHealthX == that.hpReaperTargetHealthX &&
               this.hpReaperTargetHealthY == that.hpReaperTargetHealthY &&
               this.hpReaperDiffX == that.hpReaperDiffX &&
               this.hpReaperDiffY == that.hpReaperDiffY &&
               Objects.equals(this.hpReaperKeybind, that.hpReaperKeybind) &&
               Objects.equals(this.hpReaperMode, that.hpReaperMode) &&
               Objects.equals(this.hpReaperTargetFilter, that.hpReaperTargetFilter) &&

               this.autoToolEnabled == that.autoToolEnabled &&
               this.autoToolCombatGuard == that.autoToolCombatGuard &&
               this.autoToolDurabilitySaver == that.autoToolDurabilitySaver &&
               this.autoToolPreferSilkTouch == that.autoToolPreferSilkTouch &&
               this.autoToolRestorePrevious == that.autoToolRestorePrevious &&
               this.autoToolLegitMode == that.autoToolLegitMode &&
               this.autoToolSingleSlotMode == that.autoToolSingleSlotMode &&
               this.autoToolIgnoreInstantBreak == that.autoToolIgnoreInstantBreak &&
               this.autoToolLockWhileMining == that.autoToolLockWhileMining &&
               Double.compare(this.autoToolDurabilityThreshold, that.autoToolDurabilityThreshold) == 0 &&
               Objects.equals(this.autoToolKeybind, that.autoToolKeybind) &&

               this.autoGGEnabled == that.autoGGEnabled &&
               this.autoGGSendOnKill == that.autoGGSendOnKill &&
               this.autoGGSendOnOwnDeath == that.autoGGSendOnOwnDeath &&
               this.autoGGRandomOrder == that.autoGGRandomOrder &&
               Double.compare(this.autoGGDelayMs, that.autoGGDelayMs) == 0 &&
               Objects.equals(this.autoGGKeybind, that.autoGGKeybind) &&
               Objects.equals(this.autoGGMenuKeybind, that.autoGGMenuKeybind) &&
               Objects.equals(this.autoGGPhrase, that.autoGGPhrase) &&

               this.cartHudEnabled == that.cartHudEnabled &&
               this.cartHudCustomX == that.cartHudCustomX &&
               this.cartHudCustomY == that.cartHudCustomY &&
               Objects.equals(this.cartHudKeybind, that.cartHudKeybind) &&

               // HUD & System
               this.overlayEnabled == that.overlayEnabled &&
               this.darkThemeEnabled == that.darkThemeEnabled &&
               this.autoHideOnChat == that.autoHideOnChat &&
               this.hideInF3 == that.hideInF3 &&
               this.matchCase == that.matchCase &&
               this.showCoordinates == that.showCoordinates &&
               this.showFps == that.showFps &&
               this.showBiome == that.showBiome &&
               this.showWorldTime == that.showWorldTime &&
               this.showDirection == that.showDirection &&
               this.textShadow == that.textShadow &&
               this.compactMode == that.compactMode &&
               this.tooltipsEnabled == that.tooltipsEnabled &&
               this.showKeyHints == that.showKeyHints &&
               this.smoothTransitions == that.smoothTransitions &&
               this.audioClicks == that.audioClicks &&
               this.debugLogging == that.debugLogging &&
               this.profilerActive == that.profilerActive &&
               this.asyncTickEnabled == that.asyncTickEnabled &&
               this.benchmarksEnabled == that.benchmarksEnabled &&
               this.scissorOpt == that.scissorOpt &&
               Double.compare(this.overlayOpacity, that.overlayOpacity) == 0 &&
               Double.compare(this.hudPadding, that.hudPadding) == 0 &&
               Double.compare(this.soundVolume, that.soundVolume) == 0 &&
               Double.compare(this.maxCacheEntries, that.maxCacheEntries) == 0 &&
               Double.compare(this.windowOpacity, that.windowOpacity) == 0 &&
               Double.compare(this.panelOpacity, that.panelOpacity) == 0 &&
               this.glassEffect == that.glassEffect &&
               this.windowPosX == that.windowPosX &&
               this.windowPosY == that.windowPosY &&
               this.windowWidth == that.windowWidth &&
               this.windowHeight == that.windowHeight &&
               this.windowMaximized == that.windowMaximized &&
               this.unmaximizedX == that.unmaximizedX &&
               this.unmaximizedY == that.unmaximizedY &&
               this.unmaximizedWidth == that.unmaximizedWidth &&
               this.unmaximizedHeight == that.unmaximizedHeight &&
               this.soundEnabled == that.soundEnabled &&
               Objects.equals(this.soundProfile, that.soundProfile) &&
               this.sliderSoundEnabled == that.sliderSoundEnabled &&
               this.animationsEnabled == that.animationsEnabled &&
               this.spatialOpenAnimation == that.spatialOpenAnimation &&
               Objects.equals(this.fontFamily, that.fontFamily) &&
               Objects.equals(this.typographySize, that.typographySize) &&
               Objects.equals(this.hudPosition, that.hudPosition) &&
               Objects.equals(this.searchFilter, that.searchFilter) &&
               Objects.equals(this.filterCategory, that.filterCategory) &&
               Objects.equals(this.activeProfile, that.activeProfile) &&
               Objects.equals(this.themeVariant, that.themeVariant) &&
               Objects.equals(this.customPrefix, that.customPrefix) &&
               Objects.equals(this.toastStyle, that.toastStyle) &&
               Objects.equals(this.coordFormat, that.coordFormat) &&
               Objects.equals(this.customTitle, that.customTitle) &&
               Objects.equals(this.logLevel, that.logLevel) &&
               Objects.equals(this.filterRegex, that.filterRegex) &&
               Objects.equals(this.gcPolicy, that.gcPolicy) &&
               Objects.equals(this.client, that.client) &&
               Objects.equals(this.modules, that.modules);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(
            configVersion,
            pinnedModules,
            client,
            modules,
            // Combat
            autoMaceEnabled, autoMaceKeybind, autoMaceSourceMode, autoMaceEnchantMode, autoMaceMissBehavior, autoMaceRestoreDelayMs, autoMaceLegitMode, autoMaceMissChance, autoMaceRandomDelay,
            autoSpearEnabled, autoSpearKeybind, autoSpearTriggerKeybind, autoSpearSecurityMode, autoSpearPriorityMode, autoSpearRestoreDelayMs, autoSpearMissChance, autoSpearRandomDelay,
            autoShieldbreakerEnabled, autoShieldbreakerKeybind, autoShieldbreakerMode, autoShieldbreakerDistance, autoShieldbreakerChance,
            autoShieldbreakerSwitchDelayMs, autoShieldbreakerRestoreDelayMs, autoShieldbreakerRandomDelay, autoShieldbreakerAbortOnManualSwitch, autoShieldbreakerLegitMode,
            autoStunSlamEnabled, autoStunSlamKeybind, autoStunSlamMode, autoStunSlamDistance, autoStunSlamChance
        );
        result = 31 * result + Objects.hash(
            autoStunSlamAirTimeSec, autoStunSlamAxeDelayMs, autoStunSlamMaceDelayMs, autoStunSlamRestoreDelayMs, autoStunSlamRandomDelay, autoStunSlamLegitMode,
            // Defense
            autoTotemEnabled, autoTotemKeybind, autoTotemMode, autoTotemTriggerHearts, autoTotemRestoreHearts, autoTotemChance, autoTotemReturnItem, autoTotemReturnOnPop,
            autoCartEnabled, autoCartKeybind, autoCartPreset, autoCartPlacementChance, autoCartMaxDistance, autoCartMinDelayMs, autoCartMaxDelayMs,
            autoCartAllowSelfCart, autoCartAllowPitPlacement, autoCartRandomDelay, autoCartRailDelay, autoCartCartDelay, autoCartRestoreDelay, autoCartLegitMode,
            autoAnchorEnabled, autoAnchorKeybind, autoAnchorPreset, autoAnchorAutoExplode, autoAnchorAutoReturn, autoAnchorChargeDelay, autoAnchorExplodeDelay, autoAnchorChance, autoAnchorTargetCharges, autoAnchorLegitMode,
            cartRefillEnabled, cartRefillKeybind, cartRefillDelayTicks, cartRefillChance, cartRefillAutoClose, cartRefillRandomDelay, cartRefillLegitMode
        );
        result = 31 * result + Objects.hash(
            // Utility
            hpReaperEnabled, hpReaperKeybind, hpReaperMode, hpReaperTargetFilter,
            hpReaperOwnHealthX, hpReaperOwnHealthY, hpReaperCrosshairTargetX, hpReaperCrosshairTargetY, hpReaperTargetHealthX, hpReaperTargetHealthY, hpReaperDiffX, hpReaperDiffY,
            autoToolEnabled, autoToolKeybind, autoToolCombatGuard, autoToolDurabilitySaver, autoToolDurabilityThreshold, autoToolPreferSilkTouch, autoToolRestorePrevious, autoToolLegitMode, autoToolSingleSlotMode, autoToolIgnoreInstantBreak, autoToolLockWhileMining,
            autoGGEnabled, autoGGKeybind, autoGGMenuKeybind, autoGGPhrase, autoGGSendOnKill, autoGGSendOnOwnDeath, autoGGRandomOrder, autoGGDelayMs,
            cartHudEnabled, cartHudKeybind, cartHudCustomX, cartHudCustomY,
            // HUD & System
            overlayEnabled, darkThemeEnabled, hudPosition, overlayOpacity, autoHideOnChat, hideInF3, searchFilter, filterCategory, matchCase
        );
        result = 31 * result + Objects.hash(
            activeProfile, themeVariant, compactMode, tooltipsEnabled, showKeyHints, smoothTransitions, soundVolume, audioClicks,
            customPrefix, toastStyle, showCoordinates, showFps, showBiome, showWorldTime, showDirection, coordFormat, hudPadding,
            customTitle, textShadow, debugLogging, profilerActive, asyncTickEnabled, logLevel, benchmarksEnabled, maxCacheEntries,
            scissorOpt, filterRegex, gcPolicy,
            fontFamily, typographySize, windowOpacity, panelOpacity, glassEffect, windowPosX, windowPosY, windowWidth, windowHeight, windowMaximized, unmaximizedX, unmaximizedY, unmaximizedWidth, unmaximizedHeight, soundEnabled, soundProfile, sliderSoundEnabled, animationsEnabled, spatialOpenAnimation
        );
        return result;
    }
}

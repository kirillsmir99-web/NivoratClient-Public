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
    public double autoMaceRestoreDelayMs = 90.0;
    public boolean autoMaceLegitMode = true;
    public double autoMaceMissChance = 10.0;

    // AutoSpear
    public boolean autoSpearEnabled = true;
    public Keybind autoSpearKeybind = new Keybind();
    public double autoSpearRestoreDelayMs = 70.0;

    // AutoShieldbreaker
    public boolean autoShieldbreakerEnabled = true;
    public Keybind autoShieldbreakerKeybind = new Keybind();
    public String autoShieldbreakerMode = "full_auto";
    public double autoShieldbreakerDistance = 2.85;
    public double autoShieldbreakerChance = 100.0;
    public double autoShieldbreakerSwitchDelayMs = 50.0;
    public double autoShieldbreakerRestoreDelayMs = 50.0;
    public boolean autoShieldbreakerLegitMode = true;

    // AutoStunSlam (Авто Стан Слэм)
    public boolean autoStunSlamEnabled = true;
    public Keybind autoStunSlamKeybind = new Keybind();
    public String autoStunSlamMode = "full_auto";
    public double autoStunSlamDistance = 2.4;
    public double autoStunSlamChance = 75.0;
    public double autoStunSlamAxeDelayMs = 45.0;
    public double autoStunSlamMaceDelayMs = 45.0;
    public double autoStunSlamRestoreDelayMs = 50.0;
    public boolean autoStunSlamLegitMode = true;

    // Backward-compatibility legacy alias fields for JSON deserialization
    public Boolean autoStunSlimeEnabled = null;
    public Keybind autoStunSlimeKeybind = null;
    public String autoStunSlimeMode = null;
    public Double autoStunSlimeDistance = null;
    public Double autoStunSlimeChance = null;
    public Double autoStunSlimeAxeDelayMs = null;
    public Double autoStunSlimeMaceDelayMs = null;
    public Double autoStunSlimeRestoreDelayMs = null;
    public Boolean autoStunSlimeLegitMode = null;

    // ==========================================
    // 2. DEFENSE MODULES (Защита и карты)
    // ==========================================
    // AutoTotem
    public boolean autoTotemEnabled = true;
    public Keybind autoTotemKeybind = new Keybind();
    public double autoTotemTriggerHearts = 3.0;
    public double autoTotemRestoreHearts = 6.0;
    public double autoTotemChance = 100.0;
    public boolean autoTotemReturnItem = true;
    public boolean autoTotemReturnOnPop = true;

    // AutoCart
    public boolean autoCartEnabled = true;
    public Keybind autoCartKeybind = new Keybind();
    public double autoCartPlacementChance = 70.0;
    public double autoCartRailDelay = 2.0;
    public double autoCartCartDelay = 2.0;
    public double autoCartRestoreDelay = 2.0;
    public boolean autoCartLegitMode = true;

    // AutoAnchor
    public boolean autoAnchorEnabled = true;
    public Keybind autoAnchorKeybind = new Keybind();
    public boolean autoAnchorAutoExplode = false;
    public boolean autoAnchorAutoReturn = true;
    public double autoAnchorChargeDelay = 1.0;
    public double autoAnchorChance = 85.0;
    public boolean autoAnchorLegitMode = true;

    // CartRefill
    public boolean cartRefillEnabled = true;
    public Keybind cartRefillKeybind = new Keybind();
    public double cartRefillDelayTicks = 2.0;
    public double cartRefillChance = 100.0;
    public boolean cartRefillLegitMode = true;
    public boolean cartRefillAutoClose = true;

    // ==========================================
    // 3. UTILITY MODULES (Утилиты и HUD)
    // ==========================================
    // HPReaper
    public boolean hpReaperEnabled = true;
    public Keybind hpReaperKeybind = new Keybind();
    public String hpReaperMode = "target_hp";

    // AutoTool
    public boolean autoToolEnabled = true;
    public Keybind autoToolKeybind = new Keybind();
    public boolean autoToolCombatGuard = true;
    public boolean autoToolDurabilitySaver = true;
    public double autoToolDurabilityThreshold = 5.0;
    public boolean autoToolPreferSilkTouch = false;
    public boolean autoToolRestorePrevious = true;

    // AutoGG
    public boolean autoGGEnabled = true;
    public Keybind autoGGKeybind = new Keybind();
    public String autoGGPhrase = "GGWP";
    public boolean autoGGSendOnOwnDeath = false;

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
        this.autoMaceRestoreDelayMs = 90.0;
        this.autoMaceLegitMode = true;
        this.autoMaceMissChance = 10.0;

        this.autoSpearEnabled = true;
        this.autoSpearKeybind.clear();
        this.autoSpearRestoreDelayMs = 70.0;

        this.autoShieldbreakerEnabled = true;
        this.autoShieldbreakerKeybind.clear();
        this.autoShieldbreakerMode = "full_auto";
        this.autoShieldbreakerDistance = 2.85;
        this.autoShieldbreakerChance = 100.0;
        this.autoShieldbreakerSwitchDelayMs = 50.0;
        this.autoShieldbreakerRestoreDelayMs = 50.0;
        this.autoShieldbreakerLegitMode = true;

        this.autoStunSlamEnabled = true;
        this.autoStunSlamKeybind.clear();
        this.autoStunSlamMode = "full_auto";
        this.autoStunSlamDistance = 2.4;
        this.autoStunSlamChance = 75.0;
        this.autoStunSlamAxeDelayMs = 45.0;
        this.autoStunSlamMaceDelayMs = 45.0;
        this.autoStunSlamRestoreDelayMs = 50.0;
        this.autoStunSlamLegitMode = true;

        this.autoStunSlimeEnabled = null;
        this.autoStunSlimeKeybind = null;
        this.autoStunSlimeMode = null;
        this.autoStunSlimeDistance = null;
        this.autoStunSlimeChance = null;
        this.autoStunSlimeAxeDelayMs = null;
        this.autoStunSlimeMaceDelayMs = null;
        this.autoStunSlimeRestoreDelayMs = null;
        this.autoStunSlimeLegitMode = null;

        if (this.pinnedModules != null) {
            this.pinnedModules.clear();
        }

        // Defense
        this.autoTotemEnabled = true;
        this.autoTotemKeybind.clear();
        this.autoTotemTriggerHearts = 3.0;
        this.autoTotemRestoreHearts = 6.0;
        this.autoTotemChance = 100.0;
        this.autoTotemReturnItem = true;
        this.autoTotemReturnOnPop = true;

        this.autoCartEnabled = true;
        this.autoCartKeybind.clear();
        this.autoCartPlacementChance = 70.0;
        this.autoCartRailDelay = 2.0;
        this.autoCartCartDelay = 2.0;
        this.autoCartRestoreDelay = 2.0;
        this.autoCartLegitMode = true;

        this.autoAnchorEnabled = true;
        this.autoAnchorKeybind.clear();
        this.autoAnchorAutoExplode = false;
        this.autoAnchorAutoReturn = true;
        this.autoAnchorChargeDelay = 1.0;
        this.autoAnchorChance = 85.0;
        this.autoAnchorLegitMode = true;

        this.cartRefillEnabled = true;
        this.cartRefillKeybind.clear();
        this.cartRefillDelayTicks = 2.0;
        this.cartRefillChance = 100.0;
        this.cartRefillLegitMode = true;
        this.cartRefillAutoClose = true;

        // Utility
        this.hpReaperEnabled = true;
        this.hpReaperKeybind.clear();
        this.hpReaperMode = "target_hp";

        this.autoToolEnabled = true;
        this.autoToolKeybind.clear();
        this.autoToolCombatGuard = true;
        this.autoToolDurabilitySaver = true;
        this.autoToolDurabilityThreshold = 5.0;
        this.autoToolPreferSilkTouch = false;
        this.autoToolRestorePrevious = true;

        this.autoGGEnabled = true;
        this.autoGGKeybind.clear();
        this.autoGGPhrase = "GGWP";
        this.autoGGSendOnOwnDeath = false;

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
    }

    /**
     * Validates and sanitizes loaded configuration values against bounds, nulls, and NaNs.
     */
    public void sanitize() {
        // Keybind null guards
        if (this.autoMaceKeybind == null) this.autoMaceKeybind = new Keybind();
        if (this.autoSpearKeybind == null) this.autoSpearKeybind = new Keybind();
        if (this.autoShieldbreakerKeybind == null) this.autoShieldbreakerKeybind = new Keybind();
        if (this.autoStunSlamKeybind == null) this.autoStunSlamKeybind = new Keybind();
        if (this.autoTotemKeybind == null) this.autoTotemKeybind = new Keybind();
        if (this.autoCartKeybind == null) this.autoCartKeybind = new Keybind();
        if (this.autoAnchorKeybind == null) this.autoAnchorKeybind = new Keybind();
        if (this.cartRefillKeybind == null) this.cartRefillKeybind = new Keybind();
        if (this.hpReaperKeybind == null) this.hpReaperKeybind = new Keybind();
        if (this.autoToolKeybind == null) this.autoToolKeybind = new Keybind();
        if (this.autoGGKeybind == null) this.autoGGKeybind = new Keybind();

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
        if (this.autoStunSlimeLegitMode != null) {
            this.autoStunSlamLegitMode = this.autoStunSlimeLegitMode;
            this.autoStunSlimeLegitMode = null;
        }

        // Pinned modules migration and null guard
        if (this.pinnedModules == null) {
            this.pinnedModules = new java.util.ArrayList<>();
        } else {
            for (int i = 0; i < this.pinnedModules.size(); i++) {
                if ("auto_stun_slime".equals(this.pinnedModules.get(i))) {
                    this.pinnedModules.set(i, "auto_stun_slam");
                }
            }
        }

        // Sliders & Numbers
        this.autoMaceRestoreDelayMs = clampSanitize(this.autoMaceRestoreDelayMs, 30.0, 300.0, 90.0);
        this.autoMaceMissChance = clampSanitize(this.autoMaceMissChance, 0.0, 50.0, 10.0);
        this.autoSpearRestoreDelayMs = clampSanitize(this.autoSpearRestoreDelayMs, 10.0, 500.0, 70.0);

        this.autoShieldbreakerDistance = clampSanitize(this.autoShieldbreakerDistance, 1.5, 4.5, 2.85);
        this.autoShieldbreakerChance = clampSanitize(this.autoShieldbreakerChance, 10.0, 100.0, 100.0);
        this.autoShieldbreakerSwitchDelayMs = clampSanitize(this.autoShieldbreakerSwitchDelayMs, 10.0, 200.0, 50.0);
        this.autoShieldbreakerRestoreDelayMs = clampSanitize(this.autoShieldbreakerRestoreDelayMs, 10.0, 200.0, 50.0);

        this.autoStunSlamDistance = clampSanitize(this.autoStunSlamDistance, 1.5, 4.0, 2.4);
        this.autoStunSlamChance = clampSanitize(this.autoStunSlamChance, 10.0, 100.0, 75.0);
        this.autoStunSlamAxeDelayMs = clampSanitize(this.autoStunSlamAxeDelayMs, 10.0, 200.0, 45.0);
        this.autoStunSlamMaceDelayMs = clampSanitize(this.autoStunSlamMaceDelayMs, 10.0, 200.0, 45.0);
        this.autoStunSlamRestoreDelayMs = clampSanitize(this.autoStunSlamRestoreDelayMs, 10.0, 200.0, 50.0);

        this.autoTotemTriggerHearts = clampSanitize(this.autoTotemTriggerHearts, 1.0, 9.0, 3.0);
        this.autoTotemRestoreHearts = clampSanitize(this.autoTotemRestoreHearts, 4.0, 10.0, 6.0);
        this.autoTotemChance = clampSanitize(this.autoTotemChance, 10.0, 100.0, 100.0);

        this.autoCartPlacementChance = clampSanitize(this.autoCartPlacementChance, 0.0, 100.0, 70.0);
        this.autoCartRailDelay = clampSanitize(this.autoCartRailDelay, 0.0, 10.0, 2.0);
        this.autoCartCartDelay = clampSanitize(this.autoCartCartDelay, 0.0, 10.0, 2.0);
        this.autoCartRestoreDelay = clampSanitize(this.autoCartRestoreDelay, 0.0, 10.0, 2.0);

        this.autoAnchorChargeDelay = clampSanitize(this.autoAnchorChargeDelay, 0.0, 10.0, 1.0);
        this.autoAnchorChance = clampSanitize(this.autoAnchorChance, 10.0, 100.0, 85.0);

        this.cartRefillDelayTicks = clampSanitize(this.cartRefillDelayTicks, 0.0, 10.0, 2.0);
        this.cartRefillChance = clampSanitize(this.cartRefillChance, 10.0, 100.0, 100.0);

        this.autoToolDurabilityThreshold = clampSanitize(this.autoToolDurabilityThreshold, 1.0, 50.0, 5.0);

        this.overlayOpacity = clampSanitize(this.overlayOpacity, 10.0, 100.0, 85.0);
        this.soundVolume = clampSanitize(this.soundVolume, 0.0, 100.0, 70.0);
        this.hudPadding = clampSanitize(this.hudPadding, 0.0, 64.0, 8.0);
        this.maxCacheEntries = clampSanitize(this.maxCacheEntries, 16.0, 2048.0, 256.0);

        // String migration and non-null guards
        if ("Меч и топор".equals(this.autoMaceSourceMode) || "sword_and_axe".equals(this.autoMaceSourceMode)) this.autoMaceSourceMode = "sword_and_axe";
        else if ("Только меч".equals(this.autoMaceSourceMode) || "sword_only".equals(this.autoMaceSourceMode)) this.autoMaceSourceMode = "sword_only";
        else if ("Только топор".equals(this.autoMaceSourceMode) || "axe_only".equals(this.autoMaceSourceMode)) this.autoMaceSourceMode = "axe_only";
        else if (this.autoMaceSourceMode == null || this.autoMaceSourceMode.isBlank()) this.autoMaceSourceMode = "sword_and_axe";

        if ("Умный выбор".equals(this.autoMaceEnchantMode) || "smart".equals(this.autoMaceEnchantMode)) this.autoMaceEnchantMode = "smart";
        else if ("Только Пробивание".equals(this.autoMaceEnchantMode) || "breach_only".equals(this.autoMaceEnchantMode)) this.autoMaceEnchantMode = "breach_only";
        else if ("Только Плотность".equals(this.autoMaceEnchantMode) || "density_only".equals(this.autoMaceEnchantMode)) this.autoMaceEnchantMode = "density_only";
        else if (this.autoMaceEnchantMode == null || this.autoMaceEnchantMode.isBlank()) this.autoMaceEnchantMode = "smart";

        if ("Полный авто".equals(this.autoShieldbreakerMode) || "full_auto".equals(this.autoShieldbreakerMode)) this.autoShieldbreakerMode = "full_auto";
        else if ("Полу-авто".equals(this.autoShieldbreakerMode) || "semi_auto".equals(this.autoShieldbreakerMode)) this.autoShieldbreakerMode = "semi_auto";
        else if (this.autoShieldbreakerMode == null || this.autoShieldbreakerMode.isBlank()) this.autoShieldbreakerMode = "full_auto";

        if ("Полный авто".equals(this.autoStunSlamMode) || "full_auto".equals(this.autoStunSlamMode)) this.autoStunSlamMode = "full_auto";
        else if ("Полу-авто".equals(this.autoStunSlamMode) || "semi_auto".equals(this.autoStunSlamMode)) this.autoStunSlamMode = "semi_auto";
        else if (this.autoStunSlamMode == null || this.autoStunSlamMode.isBlank()) this.autoStunSlamMode = "full_auto";

        if ("Числовое HP цели".equals(this.hpReaperMode) || "target_hp".equals(this.hpReaperMode)) this.hpReaperMode = "target_hp";
        else if ("Своё здоровье".equals(this.hpReaperMode) || "own_hp".equals(this.hpReaperMode)) this.hpReaperMode = "own_hp";
        else if ("Разница урона".equals(this.hpReaperMode) || "damage_diff".equals(this.hpReaperMode)) this.hpReaperMode = "damage_diff";
        else if ("Компактный".equals(this.hpReaperMode) || "compact".equals(this.hpReaperMode)) this.hpReaperMode = "compact";
        else if (this.hpReaperMode == null || this.hpReaperMode.isBlank()) this.hpReaperMode = "target_hp";

        if ("Сверху справа".equals(this.hudPosition) || "top_right".equals(this.hudPosition)) this.hudPosition = "top_right";
        else if ("Сверху слева".equals(this.hudPosition) || "top_left".equals(this.hudPosition)) this.hudPosition = "top_left";
        else if ("Снизу справа".equals(this.hudPosition) || "bottom_right".equals(this.hudPosition)) this.hudPosition = "bottom_right";
        else if ("Снизу слева".equals(this.hudPosition) || "bottom_left".equals(this.hudPosition)) this.hudPosition = "bottom_left";
        else if (this.hudPosition == null || this.hudPosition.isBlank()) this.hudPosition = "top_right";

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
    }

    private static double clampSanitize(double val, double min, double max, double def) {
        if (Double.isNaN(val) || Double.isInfinite(val)) return def;
        return Math.clamp(val, min, max);
    }

    /**
     * Creates a detached deep copy of this configuration.
     */
    public ActivityConfig copy() {
        ActivityConfig copy = new ActivityConfig();
        copy.configVersion = this.configVersion;
        copy.pinnedModules = new java.util.ArrayList<>(this.pinnedModules);

        // Combat
        copy.autoMaceEnabled = this.autoMaceEnabled;
        copy.autoMaceKeybind.copyFrom(this.autoMaceKeybind);
        copy.autoMaceSourceMode = this.autoMaceSourceMode;
        copy.autoMaceEnchantMode = this.autoMaceEnchantMode;
        copy.autoMaceRestoreDelayMs = this.autoMaceRestoreDelayMs;
        copy.autoMaceLegitMode = this.autoMaceLegitMode;
        copy.autoMaceMissChance = this.autoMaceMissChance;

        copy.autoSpearEnabled = this.autoSpearEnabled;
        copy.autoSpearKeybind.copyFrom(this.autoSpearKeybind);
        copy.autoSpearRestoreDelayMs = this.autoSpearRestoreDelayMs;

        copy.autoShieldbreakerEnabled = this.autoShieldbreakerEnabled;
        copy.autoShieldbreakerKeybind.copyFrom(this.autoShieldbreakerKeybind);
        copy.autoShieldbreakerMode = this.autoShieldbreakerMode;
        copy.autoShieldbreakerDistance = this.autoShieldbreakerDistance;
        copy.autoShieldbreakerChance = this.autoShieldbreakerChance;
        copy.autoShieldbreakerSwitchDelayMs = this.autoShieldbreakerSwitchDelayMs;
        copy.autoShieldbreakerRestoreDelayMs = this.autoShieldbreakerRestoreDelayMs;
        copy.autoShieldbreakerLegitMode = this.autoShieldbreakerLegitMode;

        copy.autoStunSlamEnabled = this.autoStunSlamEnabled;
        copy.autoStunSlamKeybind.copyFrom(this.autoStunSlamKeybind);
        copy.autoStunSlamMode = this.autoStunSlamMode;
        copy.autoStunSlamDistance = this.autoStunSlamDistance;
        copy.autoStunSlamChance = this.autoStunSlamChance;
        copy.autoStunSlamAxeDelayMs = this.autoStunSlamAxeDelayMs;
        copy.autoStunSlamMaceDelayMs = this.autoStunSlamMaceDelayMs;
        copy.autoStunSlamRestoreDelayMs = this.autoStunSlamRestoreDelayMs;
        copy.autoStunSlamLegitMode = this.autoStunSlamLegitMode;

        // Defense
        copy.autoTotemEnabled = this.autoTotemEnabled;
        copy.autoTotemKeybind.copyFrom(this.autoTotemKeybind);
        copy.autoTotemTriggerHearts = this.autoTotemTriggerHearts;
        copy.autoTotemRestoreHearts = this.autoTotemRestoreHearts;
        copy.autoTotemChance = this.autoTotemChance;
        copy.autoTotemReturnItem = this.autoTotemReturnItem;
        copy.autoTotemReturnOnPop = this.autoTotemReturnOnPop;

        copy.autoCartEnabled = this.autoCartEnabled;
        copy.autoCartKeybind.copyFrom(this.autoCartKeybind);
        copy.autoCartPlacementChance = this.autoCartPlacementChance;
        copy.autoCartRailDelay = this.autoCartRailDelay;
        copy.autoCartCartDelay = this.autoCartCartDelay;
        copy.autoCartRestoreDelay = this.autoCartRestoreDelay;
        copy.autoCartLegitMode = this.autoCartLegitMode;

        copy.autoAnchorEnabled = this.autoAnchorEnabled;
        copy.autoAnchorKeybind.copyFrom(this.autoAnchorKeybind);
        copy.autoAnchorAutoExplode = this.autoAnchorAutoExplode;
        copy.autoAnchorAutoReturn = this.autoAnchorAutoReturn;
        copy.autoAnchorChargeDelay = this.autoAnchorChargeDelay;
        copy.autoAnchorChance = this.autoAnchorChance;
        copy.autoAnchorLegitMode = this.autoAnchorLegitMode;

        copy.cartRefillEnabled = this.cartRefillEnabled;
        copy.cartRefillKeybind.copyFrom(this.cartRefillKeybind);
        copy.cartRefillDelayTicks = this.cartRefillDelayTicks;
        copy.cartRefillChance = this.cartRefillChance;
        copy.cartRefillLegitMode = this.cartRefillLegitMode;
        copy.cartRefillAutoClose = this.cartRefillAutoClose;

        // Utility
        copy.hpReaperEnabled = this.hpReaperEnabled;
        copy.hpReaperKeybind.copyFrom(this.hpReaperKeybind);
        copy.hpReaperMode = this.hpReaperMode;

        copy.autoToolEnabled = this.autoToolEnabled;
        copy.autoToolKeybind.copyFrom(this.autoToolKeybind);
        copy.autoToolCombatGuard = this.autoToolCombatGuard;
        copy.autoToolDurabilitySaver = this.autoToolDurabilitySaver;
        copy.autoToolDurabilityThreshold = this.autoToolDurabilityThreshold;
        copy.autoToolPreferSilkTouch = this.autoToolPreferSilkTouch;
        copy.autoToolRestorePrevious = this.autoToolRestorePrevious;

        copy.autoGGEnabled = this.autoGGEnabled;
        copy.autoGGKeybind.copyFrom(this.autoGGKeybind);
        copy.autoGGPhrase = this.autoGGPhrase;
        copy.autoGGSendOnOwnDeath = this.autoGGSendOnOwnDeath;

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
               Double.compare(this.autoMaceRestoreDelayMs, that.autoMaceRestoreDelayMs) == 0 &&
               Double.compare(this.autoMaceMissChance, that.autoMaceMissChance) == 0 &&
               Objects.equals(this.autoMaceKeybind, that.autoMaceKeybind) &&
               Objects.equals(this.autoMaceSourceMode, that.autoMaceSourceMode) &&
               Objects.equals(this.autoMaceEnchantMode, that.autoMaceEnchantMode) &&

               this.autoSpearEnabled == that.autoSpearEnabled &&
               Double.compare(this.autoSpearRestoreDelayMs, that.autoSpearRestoreDelayMs) == 0 &&
               Objects.equals(this.autoSpearKeybind, that.autoSpearKeybind) &&

               this.autoShieldbreakerEnabled == that.autoShieldbreakerEnabled &&
               this.autoShieldbreakerLegitMode == that.autoShieldbreakerLegitMode &&
               Double.compare(this.autoShieldbreakerDistance, that.autoShieldbreakerDistance) == 0 &&
               Double.compare(this.autoShieldbreakerChance, that.autoShieldbreakerChance) == 0 &&
               Double.compare(this.autoShieldbreakerSwitchDelayMs, that.autoShieldbreakerSwitchDelayMs) == 0 &&
               Double.compare(this.autoShieldbreakerRestoreDelayMs, that.autoShieldbreakerRestoreDelayMs) == 0 &&
               Objects.equals(this.autoShieldbreakerKeybind, that.autoShieldbreakerKeybind) &&
               Objects.equals(this.autoShieldbreakerMode, that.autoShieldbreakerMode) &&

               Objects.equals(this.pinnedModules, that.pinnedModules) &&
               this.autoStunSlamEnabled == that.autoStunSlamEnabled &&
               this.autoStunSlamLegitMode == that.autoStunSlamLegitMode &&
               Double.compare(this.autoStunSlamDistance, that.autoStunSlamDistance) == 0 &&
               Double.compare(this.autoStunSlamChance, that.autoStunSlamChance) == 0 &&
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

               this.autoCartEnabled == that.autoCartEnabled &&
               this.autoCartLegitMode == that.autoCartLegitMode &&
               Double.compare(this.autoCartPlacementChance, that.autoCartPlacementChance) == 0 &&
               Double.compare(this.autoCartRailDelay, that.autoCartRailDelay) == 0 &&
               Double.compare(this.autoCartCartDelay, that.autoCartCartDelay) == 0 &&
               Double.compare(this.autoCartRestoreDelay, that.autoCartRestoreDelay) == 0 &&
               Objects.equals(this.autoCartKeybind, that.autoCartKeybind) &&

               this.autoAnchorEnabled == that.autoAnchorEnabled &&
               this.autoAnchorAutoExplode == that.autoAnchorAutoExplode &&
               this.autoAnchorAutoReturn == that.autoAnchorAutoReturn &&
               this.autoAnchorLegitMode == that.autoAnchorLegitMode &&
               Double.compare(this.autoAnchorChargeDelay, that.autoAnchorChargeDelay) == 0 &&
               Double.compare(this.autoAnchorChance, that.autoAnchorChance) == 0 &&
               Objects.equals(this.autoAnchorKeybind, that.autoAnchorKeybind) &&

               this.cartRefillEnabled == that.cartRefillEnabled &&
               this.cartRefillLegitMode == that.cartRefillLegitMode &&
               this.cartRefillAutoClose == that.cartRefillAutoClose &&
               Double.compare(this.cartRefillDelayTicks, that.cartRefillDelayTicks) == 0 &&
               Double.compare(this.cartRefillChance, that.cartRefillChance) == 0 &&
               Objects.equals(this.cartRefillKeybind, that.cartRefillKeybind) &&

               // Utility
               this.hpReaperEnabled == that.hpReaperEnabled &&
               Objects.equals(this.hpReaperKeybind, that.hpReaperKeybind) &&
               Objects.equals(this.hpReaperMode, that.hpReaperMode) &&

               this.autoToolEnabled == that.autoToolEnabled &&
               this.autoToolCombatGuard == that.autoToolCombatGuard &&
               this.autoToolDurabilitySaver == that.autoToolDurabilitySaver &&
               this.autoToolPreferSilkTouch == that.autoToolPreferSilkTouch &&
               this.autoToolRestorePrevious == that.autoToolRestorePrevious &&
               Double.compare(this.autoToolDurabilityThreshold, that.autoToolDurabilityThreshold) == 0 &&
               Objects.equals(this.autoToolKeybind, that.autoToolKeybind) &&

               this.autoGGEnabled == that.autoGGEnabled &&
               this.autoGGSendOnOwnDeath == that.autoGGSendOnOwnDeath &&
               Objects.equals(this.autoGGKeybind, that.autoGGKeybind) &&
               Objects.equals(this.autoGGPhrase, that.autoGGPhrase) &&

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
               Objects.equals(this.gcPolicy, that.gcPolicy);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(
            configVersion,
            pinnedModules,
            // Combat
            autoMaceEnabled, autoMaceKeybind, autoMaceSourceMode, autoMaceEnchantMode, autoMaceRestoreDelayMs, autoMaceLegitMode, autoMaceMissChance,
            autoSpearEnabled, autoSpearKeybind, autoSpearRestoreDelayMs,
            autoShieldbreakerEnabled, autoShieldbreakerKeybind, autoShieldbreakerMode, autoShieldbreakerDistance, autoShieldbreakerChance,
            autoShieldbreakerSwitchDelayMs, autoShieldbreakerRestoreDelayMs, autoShieldbreakerLegitMode,
            autoStunSlamEnabled, autoStunSlamKeybind, autoStunSlamMode, autoStunSlamDistance, autoStunSlamChance
        );
        result = 31 * result + Objects.hash(
            autoStunSlamAxeDelayMs, autoStunSlamMaceDelayMs, autoStunSlamRestoreDelayMs, autoStunSlamLegitMode,
            // Defense
            autoTotemEnabled, autoTotemKeybind, autoTotemTriggerHearts, autoTotemRestoreHearts, autoTotemChance, autoTotemReturnItem, autoTotemReturnOnPop,
            autoCartEnabled, autoCartKeybind, autoCartPlacementChance, autoCartRailDelay, autoCartCartDelay, autoCartRestoreDelay, autoCartLegitMode,
            autoAnchorEnabled, autoAnchorKeybind, autoAnchorAutoExplode, autoAnchorAutoReturn, autoAnchorChargeDelay, autoAnchorChance, autoAnchorLegitMode,
            cartRefillEnabled, cartRefillKeybind, cartRefillDelayTicks, cartRefillChance, cartRefillLegitMode, cartRefillAutoClose
        );
        result = 31 * result + Objects.hash(
            // Utility
            hpReaperEnabled, hpReaperKeybind, hpReaperMode,
            autoToolEnabled, autoToolKeybind, autoToolCombatGuard, autoToolDurabilitySaver, autoToolDurabilityThreshold, autoToolPreferSilkTouch, autoToolRestorePrevious,
            autoGGEnabled, autoGGKeybind, autoGGPhrase, autoGGSendOnOwnDeath,
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

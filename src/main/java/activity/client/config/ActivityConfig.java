package activity.client.config;

import activity.client.module.keybind.Keybind;

import java.util.Objects;

public class ActivityConfig {

    public static final String PRESET_DEFAULT = "default";

    public int configVersion = 2;

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

    public boolean autoMaceEnabled = true;
    public Keybind autoMaceKeybind = new Keybind();
    public String autoMaceSourceMode = "sword_and_axe";
    public String autoMaceEnchantMode = "smart";
    public String autoMaceMissBehavior = "sword_hit";
    public double autoMaceRestoreDelayMs = 90.0;
    public boolean autoMaceLegitMode = true;
    public double autoMaceMissChance = 10.0;
    public boolean autoMaceRandomDelay = true;
    public String autoMaceSwapType = "new";
    public String autoMaceEngineMode = "new";
    public double autoMaceMinFallDistance = 1.25;
    public boolean autoMaceAutoSwitch = true;
    public boolean autoMaceSilentAim = true;
    public double autoMaceSilentAimRange = 4.0;
    public boolean autoMaceMovementFix = true;
    public boolean autoMaceStunSlam = true;
    public double autoMaceHitboxExpand = 1.5;
    public boolean autoMaceTargetPlayers = true;
    public boolean autoMaceTargetMobs = true;
    public boolean autoMaceStayOnMace = false;
    public double autoMaceAttackDelayMs = 60.0;
    public boolean autoMaceHumanMode = true;
    public boolean autoMaceRandomJitter = true;

    public boolean autoSpearEnabled = true;
    public Keybind autoSpearKeybind = new Keybind();
    public Keybind autoSpearTriggerKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_TAB);
    public String autoSpearSecurityMode = "legit";
    public String autoSpearPriorityMode = "auto";
    public double autoSpearRestoreDelayMs = 185.0;
    public double autoSpearMissChance = 0.0;
    public boolean autoSpearRandomDelay = true;
    public boolean autoSpearMaxSpeed = false;
    public boolean autoSpearCheckCharge = false;

    public boolean autoShieldbreakerEnabled = true;
    public Keybind autoShieldbreakerKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_J, true, true, false);
    public String autoShieldbreakerMode = "full_auto";
    public double autoShieldbreakerDistance = 2.85;
    public double autoShieldbreakerChance = 100.0;
    public double autoShieldbreakerSwitchDelayMs = 50.0;
    public double autoShieldbreakerRestoreDelayMs = 50.0;
    public double autoShieldbreakerReactionDelaySec = 0.0;
    public boolean autoShieldbreakerRandomDelay = true;
    public boolean autoShieldbreakerAbortOnManualSwitch = true;
    public boolean autoShieldbreakerLegitMode = true;
    public boolean autoShieldbreakerCheckAirTime = true;
    public double autoShieldbreakerMaxAirTimeSec = 1.0;

    public boolean autoStunSlamEnabled = true;
    public Keybind autoStunSlamKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_M, true, true, false);
    public String autoStunSlamPreset = "new";
    public String autoStunSlamEngineMode = "new";
    public String autoStunSlamMode = "full_auto";
    public double autoStunSlamDistance = 2.85;
    public double autoStunSlamChance = 100.0;
    public String autoStunSlamAirCondition = "blocks";
    public double autoStunSlamMinFall = 3.0;
    public double autoStunSlamAirTimeSec = 1.0;
    public double autoStunSlamAxeDelayMs = 0.0;
    public boolean autoStunSlamAxeRandomizer = true;
    public double autoStunSlamAxeJitterMs = 10.0;
    public double autoStunSlamMaceDelayMs = 0.0;
    public boolean autoStunSlamMaceRandomizer = true;
    public double autoStunSlamMaceJitterMs = 15.0;
    public String autoStunSlamEnchantPreference = "auto";
    public double autoStunSlamRestoreDelayMs = 50.0;
    public boolean autoStunSlamRestoreRandomizer = true;
    public boolean autoStunSlamStayOnWeapon = false;
    public boolean autoStunSlamRandomDelay = true;
    public boolean autoStunSlamLegitMode = true;
    public double autoStunSlamNewDistance = 3.0;
    public String autoStunSlamNewMode = "full_auto";
    public double autoStunSlamNewChance = 100.0;
    public double autoStunSlamNewAttackDelayMs = 0.0;
    public String autoStunSlamNewAirCondition = "blocks";
    public double autoStunSlamNewMinFall = 3.0;
    public double autoStunSlamNewAirTimeSec = 1.0;
    public String autoStunSlamNewEnchant = "smart";
    public boolean autoStunSlamNewSilentAim = true;
    public boolean autoStunSlamNewRandomizer = true;
    public double autoStunSlamNewJitter = 15.0;
    public boolean autoStunSlamNewStayOnMace = false;

    public boolean autoPearlCatchEnabled = true;
    public Keybind autoPearlCatchKeybind = new Keybind();
    public Keybind autoPearlCatchActionKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_V, false, false, false);
    public Keybind autoPearlCatchHorizontalKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_C, false, false, false);
    public Keybind autoPearlCatchThrowKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_V, false, false, false);
    public Keybind autoPearlCatchAsyncKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_V, false, false, false);
    public String autoPearlCatchMode = "semi_auto";
    public String autoPearlCatchDirection = "vertical";
    public double autoPearlCatchThrowDelay = 2.0;
    public boolean autoPearlCatchRestoreSlot = true;
    public boolean autoPearlCatchRestoreCamera = false;
    public double autoPearlCatchRotationTimeMs = 135.0;
    public boolean autoPearlCatchLegitMode = true;
    public double autoPearlCatchHorizontalOffset = 8.0;
    public boolean autoPearlCatchRandomDelay = true;
    public double autoPearlCatchRandomSpreadMs = 15.0;

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

    public boolean autoTotemEnabled = true;
    public Keybind autoTotemKeybind = new Keybind();
    public String autoTotemMode = "main_hand";
    public double autoTotemTriggerHearts = 3.0;
    public double autoTotemRestoreHearts = 6.0;
    public double autoTotemMainhandTriggerHearts = 3.0;
    public double autoTotemMainhandRestoreHearts = 6.0;
    public double autoTotemOffhandTriggerHearts = 2.0;
    public double autoTotemOffhandRestoreHearts = 5.0;
    public double autoTotemCrystalTriggerHearts = 3.0;
    public double autoTotemCrystalRestoreHearts = 6.0;
    public double autoTotemChance = 100.0;
    public boolean autoTotemReturnItem = true;
    public boolean autoTotemReturnOnPop = true;
    public boolean autoTotemAutoRefill = true;
    public String autoTotemRefillSlot = "auto";
    public boolean autoTotemCountAbsorption = false;
    public double autoTotemSwapBackDelay = 3.0;
    public boolean autoTotemAlwaysOffhand = true;
    public boolean autoTotemIgnoreWhenUsing = true;
    public boolean autoTotemPredictiveDamage = true;
    public boolean autoTotemPredictCrystals = true;
    public boolean autoTotemPredictFall = true;
    public boolean autoTotemPredictMace = true;
    public boolean autoTotemPredictTrident = true;
    public boolean autoTotemLowTotemNotify = true;
    public String autoTotemInventorySource = "rage";

    public boolean autoCartEnabled = true;
    public Keybind autoCartKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_I, true, true, false);
    public Keybind autoCartMacroKeybind = new Keybind();
    public double autoCartMacroDrawTicks = 6.0;
    public String autoCartMode = "classic";
    public String autoCartPreset = "medium";
    public double autoCartPlacementChance = 100.0;
    public double autoCartMaxDistance = 4.4;
    public double autoCartMinDelayMs = 50.0;
    public double autoCartMaxDelayMs = 80.0;
    public boolean autoCartAllowSelfCart = false;
    public boolean autoCartAllowPitPlacement = true;
    public boolean autoCartRandomDelay = true;
    public boolean autoCartLegitMode = true;
    public double autoCartRailDelay = 2.0;
    public double autoCartCartDelay = 2.0;
    public double autoCartRestoreDelay = 2.0;
    public boolean autoCartUseMainHand = true;
    public String autoCartCameraMode = "auto";
    public boolean autoCartAutoCamera = true;
    public double autoCartCameraSmoothness = 110.0;
    public boolean autoCartCameraReturn = true;
    public double autoCartCameraReturnSmoothness = 100.0;
    public double autoCartCameraCurve = 40.0;
    public double autoCartCameraRandomness = 35.0;
    public boolean autoCartCameraMouseGcd = true;
    public boolean autoCartAdaptiveAim = true;
    public boolean autoCartAutonomousPlacement = true;

    public boolean autoAnchorEnabled = true;
    public Keybind autoAnchorKeybind = new Keybind();
    public String autoAnchorMode = "smart";
    public String autoAnchorPreset = "balanced";
    public String autoAnchorPresetDouble = "fast";
    public double autoAnchorDoubleDelay = 1.0;
    public boolean autoAnchorDoubleAutoExplode = true;
    public boolean autoAnchorDoubleChain = true;
    public boolean autoAnchorAutoExplode = false;
    public boolean autoAnchorAutoReturn = true;
    public double autoAnchorChargeDelay = 1.0;
    public double autoAnchorExplodeDelay = 1.0;
    public double autoAnchorChance = 85.0;
    public double autoAnchorTargetCharges = 1.0;
    public boolean autoAnchorLegitMode = true;

    public boolean cartRefillEnabled = true;
    public Keybind cartRefillKeybind = new Keybind();
    public double cartRefillDelayTicks = 2.0;
    public double cartRefillChance = 100.0;
    public boolean cartRefillAutoClose = true;
    public boolean cartRefillRandomDelay = true;
    public double cartRefillRandomSpreadTicks = 1.0;
    public boolean cartRefillLegitMode = true;

    public boolean hpReaperEnabled = true;
    public boolean hpReaperShowArmor = true;
    public boolean hpReaperShowDifference = true;
    public boolean hpReaperLowHealthHearts = true;
    public int hpReaperLowHealthThreshold = 8;
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

    public boolean autoToolEnabled = true;
    public Keybind autoToolKeybind = new Keybind();
    public boolean autoToolCombatGuard = true;
    public boolean autoToolWeaponSwitch = true;
    public boolean autoToolDurabilitySaver = true;
    public double autoToolDurabilityThreshold = 5.0;
    public boolean autoToolPreferSilkTouch = false;
    public boolean autoToolRestorePrevious = true;
    public boolean autoToolLegitMode = true;
    public boolean autoToolSingleSlotMode = false;
    public int autoToolSingleSlot = 0;
    public boolean autoToolIgnoreInstantBreak = true;
    public boolean autoToolLockWhileMining = true;

    public boolean autoGGEnabled = true;
    public Keybind autoGGKeybind = new Keybind();
    public Keybind autoGGMenuKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_G);
    public String autoGGPhrase = "GGWP";
    public boolean autoGGSendOnKill = true;
    public boolean autoGGSendOnOwnDeath = true;
    public boolean autoGGRandomOrder = false;
    public double autoGGDelayMs = 950.0;

    public boolean cartHudEnabled = true;
    public Keybind cartHudKeybind = new Keybind();
    public int cartHudCustomX = -1;
    public int cartHudCustomY = -1;

    public boolean cooldownHudEnabled = true;
    public Keybind cooldownHudKeybind = new Keybind();
    public int cooldownHudCustomX = -1;
    public int cooldownHudCustomY = -1;
    public boolean cooldownHudVertical = false;
    public double cooldownHudMinDuration = 2.5;

    public boolean waterDropEnabled = true;
    public Keybind waterDropKeybind = new Keybind();
    public String waterDropMode = "inventory";
    public double waterDropFallThreshold = 4.0;
    public boolean waterDropPickupWater = true;
    public boolean waterDropSwitchBack = true;
    public String waterDropCameraMode = "off";
    public double waterDropPitchThreshold = 45.0;
    public double waterDropPickupDelayMs = 85.0;
    public double waterDropSwitchDelayMs = 130.0;
    public boolean waterDropRandomDelay = true;
    public String waterDropTargetSlot = "9";
    public boolean waterDropCombatGuard = true;
    public boolean waterDropPearlGuard = true;
    public boolean waterDropNetherAdapter = true;
    public boolean waterDropEnableWater = true;
    public boolean waterDropEnableWindCharge = false;
    public boolean waterDropEnableHayBlock = true;
    public boolean waterDropEnableSlimeBlock = true;
    public boolean waterDropEnableCobweb = true;
    public boolean waterDropEnablePowderSnow = true;

    public boolean clickPearlEnabled = true;
    public Keybind clickPearlKeybind = new Keybind();
    public Keybind clickPearlTriggerKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_V, false, false, false);
    public String clickPearlMode = "fast";
    public String clickPearlSearchMode = "hotbar";
    public boolean clickPearlSwitchBack = true;
    public boolean clickPearlReturnPearl = false;
    public double clickPearlSwitchDelayMs = 50.0;
    public boolean clickPearlCheckCooldown = true;
    public boolean clickPearlPreferOffhand = true;
    public boolean clickPearlRandomDelay = true;
    public boolean clickPearlSwingHand = true;
    public String clickPearlTargetSlot = "9";
    public boolean clickPearlCombatGuard = true;

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
    public int hudCustomX = -1;
    public int hudCustomY = -1;
    public boolean hudShowActiveModules = false;

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

    public Keybind menuKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
    public String menuCommand = "nt";
    public String guiTheme = "client";
    public String fontFamily = "minecraft";
    public String typographySize = "normal";
    public String language = "auto";
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
        public int hudCustomX = -1;
        public int hudCustomY = -1;
        public boolean hudShowActiveModules = false;
        public Keybind menuKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
        public String menuCommand = "nt";
        public String language = "auto";
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
                    hudCustomX == that.hudCustomX &&
                    hudCustomY == that.hudCustomY &&
                    hudShowActiveModules == that.hudShowActiveModules &&
                    Double.compare(overlayOpacity, that.overlayOpacity) == 0 &&
                    Double.compare(hudPadding, that.hudPadding) == 0 &&
                    Double.compare(windowOpacity, that.windowOpacity) == 0 &&
                    Double.compare(panelOpacity, that.panelOpacity) == 0 &&
                    Objects.equals(hudPosition, that.hudPosition) &&
                    Objects.equals(coordFormat, that.coordFormat) &&
                    Objects.equals(customTitle, that.customTitle) &&
                    Objects.equals(themeVariant, that.themeVariant) &&
                    Objects.equals(menuKeybind, that.menuKeybind) &&
                    Objects.equals(language, that.language);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                overlayEnabled, darkThemeEnabled, hudPosition, overlayOpacity, autoHideOnChat,
                hideInF3, showCoordinates, showFps, showBiome, showWorldTime, showDirection,
                coordFormat, hudPadding, customTitle, textShadow, themeVariant, compactMode,
                tooltipsEnabled, showKeyHints, smoothTransitions, animationsEnabled,
                spatialOpenAnimation, windowOpacity, panelOpacity, glassEffect, menuKeybind, language
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
        public String fontFamily = "minecraft";
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

    public boolean legacyMigrationDone = false;
    public int legacyMigrationVersion = 0;

    public ActivityConfig() {}

    public void applyPreset(String presetName) {
        if (presetName == null || presetName.isBlank()) return;
        activity.client.config.preset.PresetManager.applyPresetByName(presetName, this);
    }

    public void resetToDefaults() {
        this.configVersion = 2;

        this.autoMaceEnabled = true;
        this.autoMaceKeybind.clear();
        this.autoMaceSourceMode = "sword_and_axe";
        this.autoMaceEnchantMode = "smart";
        this.autoMaceMissBehavior = "sword_hit";
        this.autoMaceRestoreDelayMs = 90.0;
        this.autoMaceLegitMode = true;
        this.autoMaceMissChance = 10.0;
        this.autoMaceRandomDelay = true;
        this.autoMaceSwapType = "new";
        this.autoMaceEngineMode = "new";
        this.autoMaceMinFallDistance = 1.25;
        this.autoMaceAutoSwitch = true;
        this.autoMaceSilentAim = true;
        this.autoMaceSilentAimRange = 4.0;
        this.autoMaceMovementFix = true;
        this.autoMaceStunSlam = true;
        this.autoMaceHitboxExpand = 1.5;
        this.autoMaceTargetPlayers = true;
        this.autoMaceTargetMobs = true;
        this.autoMaceStayOnMace = false;
        this.autoMaceAttackDelayMs = 60.0;
        this.autoMaceHumanMode = true;
        this.autoMaceRandomJitter = true;

        this.autoSpearEnabled = true;
        this.autoSpearKeybind.clear();
        this.autoSpearTriggerKeybind.set(org.lwjgl.glfw.GLFW.GLFW_KEY_TAB, false, false, false);
        this.autoSpearSecurityMode = "legit";
        this.autoSpearPriorityMode = "auto";
        this.autoSpearRestoreDelayMs = 185.0;
        this.autoSpearMissChance = 0.0;
        this.autoSpearRandomDelay = true;
        this.autoSpearMaxSpeed = false;
        this.autoSpearCheckCharge = false;

        this.autoShieldbreakerEnabled = true;
        this.autoShieldbreakerKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_J, true, true, false);
        this.autoShieldbreakerMode = "full_auto";
        this.autoShieldbreakerDistance = 2.85;
        this.autoShieldbreakerChance = 100.0;
        this.autoShieldbreakerSwitchDelayMs = 50.0;
        this.autoShieldbreakerRestoreDelayMs = 50.0;
        this.autoShieldbreakerReactionDelaySec = 0.0;
        this.autoShieldbreakerRandomDelay = true;
        this.autoShieldbreakerAbortOnManualSwitch = true;
        this.autoShieldbreakerLegitMode = true;

        this.autoStunSlamEnabled = true;
        this.autoStunSlamKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_M, true, true, false);
        this.autoStunSlamPreset = "new";
        this.autoStunSlamEngineMode = "new";
        this.autoStunSlamMode = "full_auto";
        this.autoStunSlamDistance = 2.85;
        this.autoStunSlamChance = 100.0;
        this.autoStunSlamAirCondition = "blocks";
        this.autoStunSlamMinFall = 3.0;
        this.autoStunSlamAirTimeSec = 1.0;
        this.autoStunSlamAxeDelayMs = 0.0;
        this.autoStunSlamAxeRandomizer = true;
        this.autoStunSlamAxeJitterMs = 10.0;
        this.autoStunSlamMaceDelayMs = 0.0;
        this.autoStunSlamMaceRandomizer = true;
        this.autoStunSlamMaceJitterMs = 15.0;
        this.autoStunSlamEnchantPreference = "auto";
        this.autoStunSlamRestoreDelayMs = 50.0;
        this.autoStunSlamRestoreRandomizer = true;
        this.autoStunSlamStayOnWeapon = false;
        this.autoStunSlamRandomDelay = true;
        this.autoStunSlamLegitMode = true;
        this.autoStunSlamNewDistance = 3.0;
        this.autoStunSlamNewMode = "full_auto";
        this.autoStunSlamNewChance = 100.0;
        this.autoStunSlamNewAttackDelayMs = 0.0;
        this.autoStunSlamNewAirCondition = "blocks";
        this.autoStunSlamNewMinFall = 3.0;
        this.autoStunSlamNewAirTimeSec = 1.0;
        this.autoStunSlamNewEnchant = "smart";
        this.autoStunSlamNewSilentAim = true;
        this.autoStunSlamNewRandomizer = true;
        this.autoStunSlamNewJitter = 15.0;
        this.autoStunSlamNewStayOnMace = false;

        this.autoPearlCatchEnabled = true;
        this.autoPearlCatchKeybind = new Keybind();
        this.autoPearlCatchActionKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_V, false, false, false);
        this.autoPearlCatchHorizontalKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_C, false, false, false);
        this.autoPearlCatchThrowKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_V, false, false, false);
        this.autoPearlCatchAsyncKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_V, false, false, false);
        this.autoPearlCatchMode = "semi_auto";
        this.autoPearlCatchDirection = "vertical";
        this.autoPearlCatchThrowDelay = 2.0;
        this.autoPearlCatchRestoreSlot = true;
        this.autoPearlCatchRestoreCamera = false;
        this.autoPearlCatchRotationTimeMs = 135.0;
        this.autoPearlCatchLegitMode = true;
        this.autoPearlCatchHorizontalOffset = 8.0;
        this.autoPearlCatchRandomDelay = true;
        this.autoPearlCatchRandomSpreadMs = 15.0;

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

        this.autoTotemEnabled = true;
        this.autoTotemKeybind.clear();
        this.autoTotemMode = "main_hand";
        this.autoTotemTriggerHearts = 3.0;
        this.autoTotemRestoreHearts = 6.0;
        this.autoTotemMainhandTriggerHearts = 3.0;
        this.autoTotemMainhandRestoreHearts = 6.0;
        this.autoTotemOffhandTriggerHearts = 2.0;
        this.autoTotemOffhandRestoreHearts = 5.0;
        this.autoTotemCrystalTriggerHearts = 3.0;
        this.autoTotemCrystalRestoreHearts = 6.0;
        this.autoTotemChance = 100.0;
        this.autoTotemReturnItem = true;
        this.autoTotemReturnOnPop = true;
        this.autoTotemAutoRefill = true;
        this.autoTotemRefillSlot = "auto";

        this.autoCartEnabled = true;
        this.autoCartKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_I, true, true, false);
        this.autoCartMacroKeybind = new Keybind();
        this.autoCartMacroDrawTicks = 6.0;
        this.autoCartMode = "classic";
        this.autoCartPreset = "medium";
        this.autoCartPlacementChance = 100.0;
        this.autoCartMaxDistance = 4.4;
        this.autoCartMinDelayMs = 50.0;
        this.autoCartMaxDelayMs = 80.0;
        this.autoCartAllowSelfCart = false;
        this.autoCartAllowPitPlacement = true;
        this.autoCartRandomDelay = true;
        this.autoCartRailDelay = 2.0;
        this.autoCartCartDelay = 2.0;
        this.autoCartRestoreDelay = 2.0;
        this.autoCartLegitMode = true;
        this.autoCartUseMainHand = true;
        this.autoCartCameraMode = "auto";
        this.autoCartAutoCamera = true;
        this.autoCartCameraSmoothness = 110.0;
        this.autoCartCameraReturn = true;
        this.autoCartCameraReturnSmoothness = 100.0;
        this.autoCartCameraCurve = 40.0;
        this.autoCartCameraRandomness = 35.0;
        this.autoCartCameraMouseGcd = true;
        this.autoCartAdaptiveAim = true;
        this.autoCartAutonomousPlacement = true;

        this.autoAnchorEnabled = true;
        this.autoAnchorKeybind.clear();
        this.autoAnchorMode = "smart";
        this.autoAnchorPreset = "balanced";
        this.autoAnchorPresetDouble = "fast";
        this.autoAnchorDoubleDelay = 1.0;
        this.autoAnchorDoubleAutoExplode = true;
        this.autoAnchorDoubleChain = true;
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
        this.cartRefillRandomSpreadTicks = 1.0;
        this.cartRefillLegitMode = true;

        this.hpReaperEnabled = true;
        this.hpReaperShowArmor = true;
        this.hpReaperShowDifference = true;
        this.hpReaperLowHealthHearts = true;
        this.hpReaperLowHealthThreshold = 8;

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
        this.autoToolWeaponSwitch = true;
        this.autoToolDurabilitySaver = true;
        this.autoToolDurabilityThreshold = 5.0;
        this.autoToolPreferSilkTouch = false;
        this.autoToolRestorePrevious = true;
        this.autoToolLegitMode = true;
        this.autoToolSingleSlotMode = false;
        this.autoToolSingleSlot = 0;
        this.autoToolIgnoreInstantBreak = true;
        this.autoToolLockWhileMining = true;

        this.autoGGEnabled = true;
        this.autoGGKeybind.clear();
        this.autoGGMenuKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_G);
        this.autoGGPhrase = "GGWP";
        this.autoGGSendOnKill = true;
        this.autoGGSendOnOwnDeath = true;
        this.autoGGRandomOrder = false;
        this.autoGGDelayMs = 950.0;

        this.cartHudEnabled = true;
        this.cartHudKeybind.clear();
        this.cartHudCustomX = -1;
        this.cartHudCustomY = -1;

        this.cooldownHudEnabled = true;
        this.cooldownHudKeybind.clear();
        this.cooldownHudCustomX = -1;
        this.cooldownHudCustomY = -1;
        this.cooldownHudVertical = false;
        this.cooldownHudMinDuration = 2.5;

        this.waterDropEnabled = true;
        this.waterDropKeybind.clear();
        this.waterDropMode = "inventory";
        this.waterDropFallThreshold = 4.0;
        this.waterDropPickupWater = true;
        this.waterDropSwitchBack = true;
        this.waterDropCameraMode = "off";
        this.waterDropPitchThreshold = 45.0;
        this.waterDropPickupDelayMs = 85.0;
        this.waterDropSwitchDelayMs = 130.0;
        this.waterDropRandomDelay = true;
        this.waterDropTargetSlot = "9";
        this.waterDropCombatGuard = true;
        this.waterDropPearlGuard = true;
        this.waterDropNetherAdapter = true;
        this.waterDropEnableWater = true;
        this.waterDropEnableWindCharge = false;
        this.waterDropEnableHayBlock = true;
        this.waterDropEnableSlimeBlock = true;
        this.waterDropEnableCobweb = true;
        this.waterDropEnablePowderSnow = true;

        this.clickPearlEnabled = true;
        this.clickPearlKeybind.clear();
        this.clickPearlTriggerKeybind.set(org.lwjgl.glfw.GLFW.GLFW_KEY_V, false, false, false);
        this.clickPearlMode = "fast";
        this.clickPearlSearchMode = "hotbar";
        this.clickPearlSwitchBack = true;
        this.clickPearlReturnPearl = false;
        this.clickPearlSwitchDelayMs = 50.0;
        this.clickPearlCheckCooldown = true;
        this.clickPearlPreferOffhand = true;
        this.clickPearlRandomDelay = true;
        this.clickPearlSwingHand = true;
        this.clickPearlTargetSlot = "9";
        this.clickPearlCombatGuard = true;

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
        this.hudCustomX = -1;
        this.hudCustomY = -1;
        this.hudShowActiveModules = false;

        this.debugLogging = false;
        this.profilerActive = false;
        this.asyncTickEnabled = true;
        this.logLevel = "INFO";
        this.benchmarksEnabled = false;
        this.maxCacheEntries = 256.0;
        this.scissorOpt = true;
        this.filterRegex = ".*";
        this.gcPolicy = "Консервативный";

        this.guiTheme = "client";
        this.fontFamily = "minecraft";
        this.typographySize = "normal";
        this.language = "auto";
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
        this.menuKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
        this.menuCommand = "nt";
        this.client = new ClientSection();
        syncClientSection();
        syncModuleConfigEntries();
    }

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

        if (this.autoMaceKeybind == null) this.autoMaceKeybind = new Keybind();
        if (this.autoSpearKeybind == null) this.autoSpearKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_TAB);
        if (this.autoSpearTriggerKeybind == null) this.autoSpearTriggerKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_TAB);
        if (this.autoShieldbreakerKeybind == null) this.autoShieldbreakerKeybind = new Keybind();
        if (this.autoStunSlamKeybind == null) this.autoStunSlamKeybind = new Keybind();
        if (this.autoTotemKeybind == null) this.autoTotemKeybind = new Keybind();
        if (this.autoCartKeybind == null) this.autoCartKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_I, true, true, false);
        if (this.autoCartMacroKeybind == null) this.autoCartMacroKeybind = new Keybind();
        if (this.autoAnchorKeybind == null) this.autoAnchorKeybind = new Keybind();
        if (this.cartRefillKeybind == null) this.cartRefillKeybind = new Keybind();
        if (this.hpReaperKeybind == null) this.hpReaperKeybind = new Keybind();
        if (this.autoToolKeybind == null) this.autoToolKeybind = new Keybind();
        if (this.autoGGKeybind == null) this.autoGGKeybind = new Keybind();
        if (this.autoPearlCatchKeybind == null) this.autoPearlCatchKeybind = new Keybind();
        if (this.autoPearlCatchActionKeybind == null) this.autoPearlCatchActionKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_V, false, false, false);
        if (this.autoPearlCatchHorizontalKeybind == null) this.autoPearlCatchHorizontalKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_C, false, false, false);
        if (this.autoGGMenuKeybind == null) this.autoGGMenuKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_G);
        if (this.cartHudKeybind == null) this.cartHudKeybind = new Keybind();
        if (this.cooldownHudKeybind == null) this.cooldownHudKeybind = new Keybind();
        if (this.waterDropKeybind == null) this.waterDropKeybind = new Keybind();
        if (this.clickPearlKeybind == null) this.clickPearlKeybind = new Keybind();
        if (this.clickPearlTriggerKeybind == null) this.clickPearlTriggerKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_V, false, false, false);
        if (this.menuKeybind == null) this.menuKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
        if (this.menuCommand == null || this.menuCommand.isBlank()) this.menuCommand = "nt";

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

        if ("our_old".equals(this.autoMaceSwapType) || "old".equals(this.autoMaceSwapType)) {
            this.autoMaceSwapType = "old";
        } else if ("test_mode".equals(this.autoMaceSwapType) || "new".equals(this.autoMaceSwapType)) {
            this.autoMaceSwapType = "new";
        } else {
            this.autoMaceSwapType = "new";
        }
        this.autoMaceEngineMode = this.autoMaceSwapType;
        this.autoMaceMinFallDistance = clampSanitize(this.autoMaceMinFallDistance, 0.5, 5.0, 1.25);
        this.autoMaceSilentAimRange = clampSanitize(this.autoMaceSilentAimRange, 2.0, 20.0, 4.0);
        this.autoMaceHitboxExpand = clampSanitize(this.autoMaceHitboxExpand, 1.0, 5.0, 1.5);
        this.autoMaceAttackDelayMs = clampSanitize(this.autoMaceAttackDelayMs, 0.0, 500.0, 60.0);
        if ("our_old".equals(this.autoStunSlamPreset) || "old".equals(this.autoStunSlamPreset)) {
            this.autoStunSlamPreset = "old";
        } else if ("test_mode".equals(this.autoStunSlamPreset) || "new".equals(this.autoStunSlamPreset)) {
            this.autoStunSlamPreset = "new";
        } else {
            this.autoStunSlamPreset = "new";
        }
        this.autoStunSlamEngineMode = this.autoStunSlamPreset;
        this.autoMaceRestoreDelayMs = clampSanitize(this.autoMaceRestoreDelayMs, 30.0, 300.0, 90.0);
        this.autoMaceMissChance = clampSanitize(this.autoMaceMissChance, 0.0, 50.0, 10.0);
        this.autoSpearRestoreDelayMs = clampSanitize(this.autoSpearRestoreDelayMs, 0.0, 500.0, 185.0);
        this.autoSpearMissChance = clampSanitize(this.autoSpearMissChance, 0.0, 50.0, 0.0);

        this.autoShieldbreakerDistance = clampSanitize(this.autoShieldbreakerDistance, 1.5, 4.5, 2.85);
        this.autoShieldbreakerChance = clampSanitize(this.autoShieldbreakerChance, 10.0, 100.0, 100.0);
        this.autoShieldbreakerSwitchDelayMs = clampSanitize(this.autoShieldbreakerSwitchDelayMs, 10.0, 200.0, 50.0);
        this.autoShieldbreakerRestoreDelayMs = clampSanitize(this.autoShieldbreakerRestoreDelayMs, 10.0, 200.0, 50.0);
        this.autoShieldbreakerReactionDelaySec = clampSanitize(this.autoShieldbreakerReactionDelaySec, 0.0, 2.0, 0.0);
        this.autoShieldbreakerMaxAirTimeSec = clampSanitize(this.autoShieldbreakerMaxAirTimeSec, 0.1, 5.0, 1.0);

        this.autoStunSlamDistance = clampSanitize(this.autoStunSlamDistance, 1.5, 4.5, 2.85);
        this.autoStunSlamChance = clampSanitize(this.autoStunSlamChance, 10.0, 100.0, 100.0);
        if (!"time".equals(this.autoStunSlamAirCondition) && !"both".equals(this.autoStunSlamAirCondition) && !"any".equals(this.autoStunSlamAirCondition)) {
            this.autoStunSlamAirCondition = "blocks";
        }
        this.autoStunSlamMinFall = clampSanitize(this.autoStunSlamMinFall, 0.0, 10.0, 3.0);
        this.autoStunSlamAirTimeSec = clampSanitize(this.autoStunSlamAirTimeSec, 0.0, 5.0, 1.0);
        this.autoStunSlamAxeDelayMs = clampSanitize(this.autoStunSlamAxeDelayMs, 0.0, 200.0, 0.0);
        this.autoStunSlamAxeJitterMs = clampSanitize(this.autoStunSlamAxeJitterMs, 0.0, 100.0, 10.0);
        this.autoStunSlamMaceDelayMs = clampSanitize(this.autoStunSlamMaceDelayMs, 0.0, 200.0, 0.0);
        this.autoStunSlamMaceJitterMs = clampSanitize(this.autoStunSlamMaceJitterMs, 0.0, 100.0, 15.0);
        this.autoStunSlamRestoreDelayMs = clampSanitize(this.autoStunSlamRestoreDelayMs, 0.0, 300.0, 50.0);
        this.autoStunSlamNewDistance = clampSanitize(this.autoStunSlamNewDistance, 1.5, 5.0, 3.0);
        if (!"semi_auto".equals(this.autoStunSlamNewMode)) {
            this.autoStunSlamNewMode = "full_auto";
        }
        this.autoStunSlamNewChance = clampSanitize(this.autoStunSlamNewChance, 10.0, 100.0, 100.0);
        this.autoStunSlamNewAttackDelayMs = clampSanitize(this.autoStunSlamNewAttackDelayMs, 0.0, 100.0, 0.0);
        if (!"time".equals(this.autoStunSlamNewAirCondition) && !"both".equals(this.autoStunSlamNewAirCondition) && !"any".equals(this.autoStunSlamNewAirCondition)) {
            this.autoStunSlamNewAirCondition = "blocks";
        }
        this.autoStunSlamNewMinFall = clampSanitize(this.autoStunSlamNewMinFall, 0.0, 10.0, 3.0);
        this.autoStunSlamNewAirTimeSec = clampSanitize(this.autoStunSlamNewAirTimeSec, 0.0, 5.0, 1.0);
        this.autoStunSlamNewJitter = clampSanitize(this.autoStunSlamNewJitter, 0.0, 100.0, 15.0);

        this.autoPearlCatchThrowDelay = clampSanitize(this.autoPearlCatchThrowDelay, 1.0, 20.0, 2.0);
        this.autoPearlCatchRotationTimeMs = clampSanitize(this.autoPearlCatchRotationTimeMs, 50.0, 500.0, 135.0);
        this.autoPearlCatchHorizontalOffset = clampSanitize(this.autoPearlCatchHorizontalOffset, 0.0, 25.0, 8.0);
        this.autoPearlCatchRandomSpreadMs = clampSanitize(this.autoPearlCatchRandomSpreadMs, 0.0, 50.0, 15.0);

        this.autoTotemTriggerHearts = clampSanitize(this.autoTotemTriggerHearts, 0.5, 10.0, 3.0);
        this.autoTotemRestoreHearts = clampSanitize(this.autoTotemRestoreHearts, 0.0, 20.0, 6.0);
        this.autoTotemMainhandTriggerHearts = clampSanitize(this.autoTotemMainhandTriggerHearts, 0.5, 10.0, 3.0);
        this.autoTotemMainhandRestoreHearts = clampSanitize(this.autoTotemMainhandRestoreHearts, 0.0, 20.0, 6.0);
        this.autoTotemOffhandTriggerHearts = clampSanitize(this.autoTotemOffhandTriggerHearts, 0.5, 10.0, 2.0);
        this.autoTotemOffhandRestoreHearts = clampSanitize(this.autoTotemOffhandRestoreHearts, 0.0, 20.0, 5.0);
        this.autoTotemCrystalTriggerHearts = clampSanitize(this.autoTotemCrystalTriggerHearts, 0.5, 10.0, 3.0);
        this.autoTotemCrystalRestoreHearts = clampSanitize(this.autoTotemCrystalRestoreHearts, 0.0, 20.0, 6.0);
        this.autoTotemChance = clampSanitize(this.autoTotemChance, 10.0, 100.0, 100.0);

        this.autoCartPlacementChance = clampSanitize(this.autoCartPlacementChance, 0.0, 100.0, 100.0);
        this.autoCartMacroDrawTicks = Math.rint(clampSanitize(this.autoCartMacroDrawTicks, 3.0, 20.0, 6.0));
        this.autoCartMaxDistance = clampSanitize(this.autoCartMaxDistance, 1.5, 4.5, 4.4);
        this.autoCartMinDelayMs = clampSanitize(this.autoCartMinDelayMs, 10.0, 200.0, 70.0);
        this.autoCartMaxDelayMs = clampSanitize(this.autoCartMaxDelayMs, 10.0, 300.0, 110.0);
        this.autoCartRailDelay = clampSanitize(this.autoCartRailDelay, 0.0, 10.0, 2.0);
        this.autoCartCartDelay = clampSanitize(this.autoCartCartDelay, 0.0, 10.0, 2.0);
        this.autoCartRestoreDelay = clampSanitize(this.autoCartRestoreDelay, 0.0, 10.0, 2.0);
        this.autoCartCameraSmoothness = clampSanitize(this.autoCartCameraSmoothness, 50.0, 300.0, 140.0);
        this.autoCartCameraReturnSmoothness = clampSanitize(this.autoCartCameraReturnSmoothness, 50.0, 300.0, 120.0);
        this.autoCartCameraCurve = clampSanitize(this.autoCartCameraCurve, 0.0, 100.0, 40.0);
        this.autoCartCameraRandomness = clampSanitize(this.autoCartCameraRandomness, 0.0, 100.0, 35.0);
        if (!"off".equalsIgnoreCase(this.autoCartCameraMode) && !"packet".equalsIgnoreCase(this.autoCartCameraMode) && !"auto".equalsIgnoreCase(this.autoCartCameraMode)) {
            this.autoCartCameraMode = "off";
        }

        this.autoAnchorChargeDelay = clampSanitize(this.autoAnchorChargeDelay, 0.0, 10.0, 1.0);
        this.autoAnchorExplodeDelay = clampSanitize(this.autoAnchorExplodeDelay, 0.0, 10.0, 1.0);
        this.autoAnchorChance = clampSanitize(this.autoAnchorChance, 10.0, 100.0, 85.0);
        this.autoAnchorTargetCharges = clampSanitize(this.autoAnchorTargetCharges, 1.0, 4.0, 1.0);
        this.autoAnchorDoubleDelay = clampSanitize(this.autoAnchorDoubleDelay, 0.0, 5.0, 1.0);

        this.cartRefillDelayTicks = clampSanitize(this.cartRefillDelayTicks, 0.0, 10.0, 2.0);
        this.cartRefillChance = clampSanitize(this.cartRefillChance, 10.0, 100.0, 100.0);
        this.cartRefillRandomSpreadTicks = clampSanitize(this.cartRefillRandomSpreadTicks, 0.0, 5.0, 1.0);

        this.autoToolDurabilityThreshold = clampSanitize(this.autoToolDurabilityThreshold, 1.0, 50.0, 5.0);
        this.autoToolSingleSlot = Math.max(0, Math.min(8, this.autoToolSingleSlot));
        this.autoGGDelayMs = clampSanitize(this.autoGGDelayMs, 100.0, 3000.0, 950.0);
        this.waterDropFallThreshold = clampSanitize(this.waterDropFallThreshold, 3.0, 20.0, 4.0);
        this.waterDropPitchThreshold = clampSanitize(this.waterDropPitchThreshold, 30.0, 90.0, 45.0);
        this.waterDropPickupDelayMs = clampSanitize(this.waterDropPickupDelayMs, 30.0, 300.0, 85.0);
        this.waterDropSwitchDelayMs = clampSanitize(this.waterDropSwitchDelayMs, 30.0, 300.0, 130.0);
        if (!"hotbar".equals(this.waterDropMode) && !"inventory".equals(this.waterDropMode)) {
            this.waterDropMode = "inventory";
        }
        if (!"off".equals(this.waterDropCameraMode) && !"auto".equals(this.waterDropCameraMode) && !"packet".equals(this.waterDropCameraMode)) {
            this.waterDropCameraMode = "off";
        }
        if (this.waterDropTargetSlot == null || !java.util.List.of("1", "2", "3", "4", "5", "6", "7", "8", "9").contains(this.waterDropTargetSlot)) {
            this.waterDropTargetSlot = "9";
        }

        this.clickPearlSwitchDelayMs = clampSanitize(this.clickPearlSwitchDelayMs, 0.0, 300.0, 50.0);
        if (!"fast".equals(this.clickPearlMode) && !"legit".equals(this.clickPearlMode) && !"safe".equals(this.clickPearlMode)) {
            this.clickPearlMode = "fast";
        }
        if (!"hotbar".equals(this.clickPearlSearchMode) && !"inventory".equals(this.clickPearlSearchMode)) {
            this.clickPearlSearchMode = "hotbar";
        }
        if (this.clickPearlTargetSlot == null || !java.util.List.of("1", "2", "3", "4", "5", "6", "7", "8", "9").contains(this.clickPearlTargetSlot)) {
            this.clickPearlTargetSlot = "9";
        }

        this.overlayOpacity = clampSanitize(this.overlayOpacity, 10.0, 100.0, 85.0);
        this.soundVolume = clampSanitize(this.soundVolume, 0.0, 100.0, 70.0);
        this.hudPadding = clampSanitize(this.hudPadding, 0.0, 64.0, 8.0);
        this.maxCacheEntries = clampSanitize(this.maxCacheEntries, 16.0, 2048.0, 256.0);

        if ("Любой предмет".equals(this.autoMaceSourceMode) || "any".equals(this.autoMaceSourceMode) || "Any".equalsIgnoreCase(this.autoMaceSourceMode)) this.autoMaceSourceMode = "any";
        else if ("Меч и топор".equals(this.autoMaceSourceMode) || "sword_and_axe".equals(this.autoMaceSourceMode)) this.autoMaceSourceMode = "sword_and_axe";
        else if ("Только меч".equals(this.autoMaceSourceMode) || "sword_only".equals(this.autoMaceSourceMode)) this.autoMaceSourceMode = "sword_only";
        else if ("Только топор".equals(this.autoMaceSourceMode) || "axe_only".equals(this.autoMaceSourceMode)) this.autoMaceSourceMode = "axe_only";
        else this.autoMaceSourceMode = "sword_and_axe";

        if ("Автомат".equals(this.autoMaceEnchantMode) || "Умный выбор".equals(this.autoMaceEnchantMode) || "Auto".equalsIgnoreCase(this.autoMaceEnchantMode) || "smart".equals(this.autoMaceEnchantMode)) this.autoMaceEnchantMode = "smart";
        else if ("Пробитие".equals(this.autoMaceEnchantMode) || "Только Пробивание".equals(this.autoMaceEnchantMode) || "Breach".equalsIgnoreCase(this.autoMaceEnchantMode) || "breach_only".equals(this.autoMaceEnchantMode)) this.autoMaceEnchantMode = "breach_only";
        else if ("Плотность".equals(this.autoMaceEnchantMode) || "Только Плотность".equals(this.autoMaceEnchantMode) || "Density".equalsIgnoreCase(this.autoMaceEnchantMode) || "density_only".equals(this.autoMaceEnchantMode)) this.autoMaceEnchantMode = "density_only";
        else this.autoMaceEnchantMode = "smart";

        if ("Удар мечом".equals(this.autoMaceMissBehavior) || "sword_hit".equals(this.autoMaceMissBehavior)) this.autoMaceMissBehavior = "sword_hit";
        else if ("Холостой свап".equals(this.autoMaceMissBehavior) || "empty_swap".equals(this.autoMaceMissBehavior)) this.autoMaceMissBehavior = "empty_swap";
        else this.autoMaceMissBehavior = "sword_hit";

        if ("Легитный".equals(this.autoSpearSecurityMode) || "Безопасный".equals(this.autoSpearSecurityMode) || "Safe".equalsIgnoreCase(this.autoSpearSecurityMode) || "legit".equals(this.autoSpearSecurityMode)) this.autoSpearSecurityMode = "legit";
        else if ("Полу-легит".equals(this.autoSpearSecurityMode) || "Сбалансированный".equals(this.autoSpearSecurityMode) || "Balanced".equalsIgnoreCase(this.autoSpearSecurityMode) || "semi_legit".equals(this.autoSpearSecurityMode)) this.autoSpearSecurityMode = "semi_legit";
        else if ("Рейдж".equals(this.autoSpearSecurityMode) || "rage".equalsIgnoreCase(this.autoSpearSecurityMode)) this.autoSpearSecurityMode = "rage";
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

        if ("Полный авто".equals(this.autoPearlCatchMode) || "full_auto".equals(this.autoPearlCatchMode)) this.autoPearlCatchMode = "full_auto";
        else if ("Полу-авто".equals(this.autoPearlCatchMode) || "semi_auto".equals(this.autoPearlCatchMode) || "Асинк".equals(this.autoPearlCatchMode) || "async".equals(this.autoPearlCatchMode) || "async_locator".equals(this.autoPearlCatchMode)) this.autoPearlCatchMode = "semi_auto";
        else this.autoPearlCatchMode = "semi_auto";

        if ("Горизонтальный".equals(this.autoPearlCatchDirection) || "horizontal".equals(this.autoPearlCatchDirection)) this.autoPearlCatchDirection = "horizontal";
        else if ("Вертикальный".equals(this.autoPearlCatchDirection) || "vertical".equals(this.autoPearlCatchDirection)) this.autoPearlCatchDirection = "vertical";
        else this.autoPearlCatchDirection = "vertical";

        if ("Основная рука".equals(this.autoTotemMode) || "main_hand".equals(this.autoTotemMode)) this.autoTotemMode = "main_hand";
        else if ("Вторая рука".equals(this.autoTotemMode) || "offhand".equals(this.autoTotemMode)) this.autoTotemMode = "offhand";
        else if ("Кристаллы".equals(this.autoTotemMode) || "crystal".equals(this.autoTotemMode)) this.autoTotemMode = "crystal";
        else this.autoTotemMode = "main_hand";

        if ("Быстрый".equals(this.autoCartPreset) || "fast".equals(this.autoCartPreset)) this.autoCartPreset = "fast";
        else if ("Средний".equals(this.autoCartPreset) || "medium".equals(this.autoCartPreset)) this.autoCartPreset = "medium";
        else if ("Безопасный".equals(this.autoCartPreset) || "safe".equals(this.autoCartPreset)) this.autoCartPreset = "safe";
        else if ("Обученный".equals(this.autoCartPreset) || "learned".equals(this.autoCartPreset)) this.autoCartPreset = "learned";
        else this.autoCartPreset = "fast";

        if ("Быстрый".equals(this.autoAnchorPreset) || "fast".equals(this.autoAnchorPreset)) this.autoAnchorPreset = "fast";
        else if ("Средний".equals(this.autoAnchorPreset) || "medium".equals(this.autoAnchorPreset)) this.autoAnchorPreset = "medium";
        else if ("Сбалансированный".equals(this.autoAnchorPreset) || "balanced".equals(this.autoAnchorPreset)) this.autoAnchorPreset = "balanced";
        else if ("Безопасный".equals(this.autoAnchorPreset) || "safe".equals(this.autoAnchorPreset)) this.autoAnchorPreset = "safe";
        else this.autoAnchorPreset = "balanced";

        if ("double".equalsIgnoreCase(this.autoAnchorMode) || "Double Anchor".equalsIgnoreCase(this.autoAnchorMode) || "Двойной анкор".equalsIgnoreCase(this.autoAnchorMode)) this.autoAnchorMode = "double";
        else this.autoAnchorMode = "smart";

        if ("Быстрый".equalsIgnoreCase(this.autoAnchorPresetDouble) || "fast".equalsIgnoreCase(this.autoAnchorPresetDouble)) this.autoAnchorPresetDouble = "fast";
        else if ("Легитный".equalsIgnoreCase(this.autoAnchorPresetDouble) || "legit".equalsIgnoreCase(this.autoAnchorPresetDouble)) this.autoAnchorPresetDouble = "legit";
        else if ("Пользовательский".equalsIgnoreCase(this.autoAnchorPresetDouble) || "custom".equalsIgnoreCase(this.autoAnchorPresetDouble)) this.autoAnchorPresetDouble = "custom";
        else this.autoAnchorPresetDouble = "fast";

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
            this.fontFamily = "minecraft";
        }
        if (!"small".equals(this.typographySize) && !"normal".equals(this.typographySize) && !"large".equals(this.typographySize)) {
            this.typographySize = "normal";
        }
        if (!"auto".equals(this.language) && !"ru".equals(this.language) && !"en".equals(this.language)) {
            this.language = "auto";
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
        this.client.ui.hudCustomX = this.hudCustomX;
        this.client.ui.hudCustomY = this.hudCustomY;
        this.client.ui.hudShowActiveModules = this.hudShowActiveModules;
        this.client.ui.menuKeybind = this.menuKeybind;
        this.client.ui.menuCommand = this.menuCommand;
        this.client.ui.language = this.language;

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
            this.hudCustomX = this.client.ui.hudCustomX;
            this.hudCustomY = this.client.ui.hudCustomY;
            this.hudShowActiveModules = this.client.ui.hudShowActiveModules;
            if (this.client.ui.menuKeybind != null) this.menuKeybind = this.client.ui.menuKeybind;
            if (this.client.ui.menuCommand != null && !this.client.ui.menuCommand.isBlank()) this.menuCommand = this.client.ui.menuCommand;
            if (this.client.ui.language != null) this.language = this.client.ui.language;
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

        syncModuleEntry("auto_pearl_catch", this.autoPearlCatchEnabled, this.autoPearlCatchKeybind);
        populatePearlCatchSettings(this.modules.get("auto_pearl_catch"));

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

        syncModuleEntry("cooldown_hud", this.cooldownHudEnabled, this.cooldownHudKeybind);
        populateCooldownHudSettings(this.modules.get("cooldown_hud"));

        syncModuleEntry("water_drop", this.waterDropEnabled, this.waterDropKeybind);
        populateWaterDropSettings(this.modules.get("water_drop"));

        syncModuleEntry("click_pearl", this.clickPearlEnabled, this.clickPearlKeybind);
        populateClickPearlSettings(this.modules.get("click_pearl"));
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
        entry.settings.put("swap_type", this.autoMaceSwapType);
        entry.settings.put("source_mode", this.autoMaceSourceMode);
        entry.settings.put("enchant_mode", this.autoMaceEnchantMode);
        entry.settings.put("miss_behavior", this.autoMaceMissBehavior);
        entry.settings.put("restore_delay", this.autoMaceRestoreDelayMs);
        entry.settings.put("miss_chance", this.autoMaceMissChance);
        entry.settings.put("random_delay", this.autoMaceRandomDelay);
        entry.settings.put("engine_mode", this.autoMaceEngineMode);
        entry.settings.put("min_fall_distance", this.autoMaceMinFallDistance);
        entry.settings.put("auto_switch", this.autoMaceAutoSwitch);
        entry.settings.put("silent_aim", this.autoMaceSilentAim);
        entry.settings.put("silent_aim_range", this.autoMaceSilentAimRange);
        entry.settings.put("movement_fix", this.autoMaceMovementFix);
        entry.settings.put("hitbox_expand", this.autoMaceHitboxExpand);
        entry.settings.put("target_players", this.autoMaceTargetPlayers);
        entry.settings.put("target_mobs", this.autoMaceTargetMobs);
        entry.settings.put("stay_on_mace", this.autoMaceStayOnMace);
        entry.settings.put("attack_delay", this.autoMaceAttackDelayMs);
        entry.settings.put("human_mode", this.autoMaceHumanMode);
        entry.settings.put("random_jitter", this.autoMaceRandomJitter);
        entry.settings.put("legit_mode", this.autoMaceLegitMode);
    }

    private void populateSpearSettings(ModuleConfigEntry entry) {
        if (entry == null) return;
        entry.settings.put("security_mode", this.autoSpearSecurityMode);
        entry.settings.put("priority_mode", this.autoSpearPriorityMode);
        entry.settings.put("restore_delay", this.autoSpearRestoreDelayMs);
        entry.settings.put("miss_chance", this.autoSpearMissChance);
        entry.settings.put("random_delay", this.autoSpearRandomDelay);
        entry.settings.put("max_speed", this.autoSpearMaxSpeed);
        entry.settings.put("check_charge", this.autoSpearCheckCharge);
        entry.settings.put("trigger_keybind", this.autoSpearTriggerKeybind);
    }

    private void populateShieldbreakerSettings(ModuleConfigEntry entry) {
        if (entry == null) return;
        entry.settings.put("mode", this.autoShieldbreakerMode);
        entry.settings.put("distance", this.autoShieldbreakerDistance);
        entry.settings.put("chance", this.autoShieldbreakerChance);
        entry.settings.put("switch_delay", this.autoShieldbreakerSwitchDelayMs);
        entry.settings.put("restore_delay", this.autoShieldbreakerRestoreDelayMs);
        entry.settings.put("reaction_delay", this.autoShieldbreakerReactionDelaySec);
        entry.settings.put("random_delay", this.autoShieldbreakerRandomDelay);
        entry.settings.put("abort_on_manual_switch", this.autoShieldbreakerAbortOnManualSwitch);
        entry.settings.put("legit_mode", this.autoShieldbreakerLegitMode);
        entry.settings.put("check_air_time", this.autoShieldbreakerCheckAirTime);
        entry.settings.put("max_air_time", this.autoShieldbreakerMaxAirTimeSec);
    }

    private void populateStunSlamSettings(ModuleConfigEntry entry) {
        if (entry == null) return;
        entry.settings.put("preset", this.autoStunSlamPreset);
        entry.settings.put("mode", this.autoStunSlamMode);
        entry.settings.put("distance", this.autoStunSlamDistance);
        entry.settings.put("chance", this.autoStunSlamChance);
        entry.settings.put("air_time", this.autoStunSlamAirTimeSec);
        entry.settings.put("axe_delay", this.autoStunSlamAxeDelayMs);
        entry.settings.put("axe_randomizer", this.autoStunSlamAxeRandomizer);
        entry.settings.put("axe_jitter", this.autoStunSlamAxeJitterMs);
        entry.settings.put("mace_delay", this.autoStunSlamMaceDelayMs);
        entry.settings.put("mace_randomizer", this.autoStunSlamMaceRandomizer);
        entry.settings.put("mace_jitter", this.autoStunSlamMaceJitterMs);
        entry.settings.put("enchant_preference", this.autoStunSlamEnchantPreference);
        entry.settings.put("restore_delay", this.autoStunSlamRestoreDelayMs);
        entry.settings.put("restore_randomizer", this.autoStunSlamRestoreRandomizer);
        entry.settings.put("stay_on_weapon", this.autoStunSlamStayOnWeapon);
        entry.settings.put("random_delay", this.autoStunSlamRandomDelay);
        entry.settings.put("legit_mode", this.autoStunSlamLegitMode);
        entry.settings.put("new_distance", this.autoStunSlamNewDistance);
        entry.settings.put("new_mode", this.autoStunSlamNewMode);
        entry.settings.put("new_chance", this.autoStunSlamNewChance);
        entry.settings.put("new_delay", this.autoStunSlamNewAttackDelayMs);
        entry.settings.put("air_condition", this.autoStunSlamAirCondition);
        entry.settings.put("min_fall", this.autoStunSlamMinFall);
        entry.settings.put("new_air_condition", this.autoStunSlamNewAirCondition);
        entry.settings.put("new_min_fall", this.autoStunSlamNewMinFall);
        entry.settings.put("new_air_time", this.autoStunSlamNewAirTimeSec);
        entry.settings.put("new_enchant", this.autoStunSlamNewEnchant);
        entry.settings.put("new_silent_aim", this.autoStunSlamNewSilentAim);
        entry.settings.put("new_randomizer", this.autoStunSlamNewRandomizer);
        entry.settings.put("new_jitter", this.autoStunSlamNewJitter);
        entry.settings.put("new_stay_on_mace", this.autoStunSlamNewStayOnMace);
    }

    private void populatePearlCatchSettings(ModuleConfigEntry entry) {
        if (entry == null) return;
        entry.settings.put("mode", this.autoPearlCatchMode);
        entry.settings.put("direction", this.autoPearlCatchDirection);
        entry.settings.put("action_keybind", this.autoPearlCatchActionKeybind);
        entry.settings.put("horizontal_keybind", this.autoPearlCatchHorizontalKeybind);
        entry.settings.put("throw_keybind", this.autoPearlCatchThrowKeybind);
        entry.settings.put("async_keybind", this.autoPearlCatchAsyncKeybind);
        entry.settings.put("throw_delay", this.autoPearlCatchThrowDelay);
        entry.settings.put("restore_slot", this.autoPearlCatchRestoreSlot);
        entry.settings.put("restore_camera", this.autoPearlCatchRestoreCamera);
        entry.settings.put("rotation_time_ms", this.autoPearlCatchRotationTimeMs);
        entry.settings.put("legit_mode", this.autoPearlCatchLegitMode);
        entry.settings.put("horizontal_offset", this.autoPearlCatchHorizontalOffset);
        entry.settings.put("random_delay", this.autoPearlCatchRandomDelay);
    }

    private void populateTotemSettings(ModuleConfigEntry entry) {
        if (entry == null) return;
        entry.settings.put("mode", this.autoTotemMode);
        entry.settings.put("trigger_hearts", this.autoTotemTriggerHearts);
        entry.settings.put("restore_hearts", this.autoTotemRestoreHearts);
        entry.settings.put("mainhand_trigger_hearts", this.autoTotemMainhandTriggerHearts);
        entry.settings.put("mainhand_restore_hearts", this.autoTotemMainhandRestoreHearts);
        entry.settings.put("offhand_trigger_hearts", this.autoTotemOffhandTriggerHearts);
        entry.settings.put("offhand_restore_hearts", this.autoTotemOffhandRestoreHearts);
        entry.settings.put("crystal_trigger_hearts", this.autoTotemCrystalTriggerHearts);
        entry.settings.put("crystal_restore_hearts", this.autoTotemCrystalRestoreHearts);
        entry.settings.put("count_absorption", this.autoTotemCountAbsorption);
        entry.settings.put("chance", this.autoTotemChance);
        entry.settings.put("return_item", this.autoTotemReturnItem);
        entry.settings.put("return_on_pop", this.autoTotemReturnOnPop);
        entry.settings.put("auto_refill", this.autoTotemAutoRefill);
        entry.settings.put("refill_slot", this.autoTotemRefillSlot);
        entry.settings.put("swap_back_delay", this.autoTotemSwapBackDelay);
        entry.settings.put("always_offhand", this.autoTotemAlwaysOffhand);
        entry.settings.put("ignore_when_using", this.autoTotemIgnoreWhenUsing);
        entry.settings.put("predictive_damage", this.autoTotemPredictiveDamage);
        entry.settings.put("predict_crystals", this.autoTotemPredictCrystals);
        entry.settings.put("predict_fall", this.autoTotemPredictFall);
        entry.settings.put("predict_mace", this.autoTotemPredictMace);
        entry.settings.put("predict_trident", this.autoTotemPredictTrident);
        entry.settings.put("low_totem_notify", this.autoTotemLowTotemNotify);
        entry.settings.put("inventory_source", this.autoTotemInventorySource);
    }

    private void populateCartSettings(ModuleConfigEntry entry) {
        if (entry == null) return;
        entry.settings.put("macro_keybind", this.autoCartMacroKeybind);
        entry.settings.put("macro_draw_ticks", this.autoCartMacroDrawTicks);
        entry.settings.put("cart_mode", this.autoCartMode);
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
        entry.settings.put("use_mainhand_cart", this.autoCartUseMainHand);
        entry.settings.put("camera_mode", this.autoCartCameraMode);
        entry.settings.put("camera_smoothness", this.autoCartCameraSmoothness);
        entry.settings.put("camera_return", this.autoCartCameraReturn);
        entry.settings.put("camera_return_smoothness", this.autoCartCameraReturnSmoothness);
        entry.settings.put("camera_curve", this.autoCartCameraCurve);
        entry.settings.put("camera_randomness", this.autoCartCameraRandomness);
        entry.settings.put("camera_mouse_gcd", this.autoCartCameraMouseGcd);
        entry.settings.put("adaptive_aim", this.autoCartAdaptiveAim);
        entry.settings.put("autonomous_placement", this.autoCartAutonomousPlacement);
    }

    private void populateAnchorSettings(ModuleConfigEntry entry) {
        if (entry == null) return;
        entry.settings.put("mode", this.autoAnchorMode);
        entry.settings.put("preset", this.autoAnchorPreset);
        entry.settings.put("preset_double", this.autoAnchorPresetDouble);
        entry.settings.put("double_delay", this.autoAnchorDoubleDelay);
        entry.settings.put("double_auto_explode", this.autoAnchorDoubleAutoExplode);
        entry.settings.put("double_chain", this.autoAnchorDoubleChain);
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
        entry.settings.put("random_spread", this.cartRefillRandomSpreadTicks);
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
        entry.settings.put("weapon_switch", this.autoToolWeaponSwitch);
        entry.settings.put("durability_saver", this.autoToolDurabilitySaver);
        entry.settings.put("durability_threshold", this.autoToolDurabilityThreshold);
        entry.settings.put("prefer_silk_touch", this.autoToolPreferSilkTouch);
        entry.settings.put("restore_previous", this.autoToolRestorePrevious);
        entry.settings.put("legit_mode", this.autoToolLegitMode);
        entry.settings.put("single_slot_mode", this.autoToolSingleSlotMode);
        entry.settings.put("single_slot", this.autoToolSingleSlot);
        entry.settings.put("ignore_instant_break", this.autoToolIgnoreInstantBreak);
        entry.settings.put("lock_while_mining", this.autoToolLockWhileMining);
    }

    private void populateGGSettings(ModuleConfigEntry entry) {
        if (entry == null) return;
        entry.settings.put("phrase", this.autoGGPhrase);
        entry.settings.put("send_on_kill", this.autoGGSendOnKill);
        entry.settings.put("send_on_own_death", this.autoGGSendOnOwnDeath);
        entry.settings.put("send_on_death", this.autoGGSendOnOwnDeath);
        entry.settings.put("random_order", this.autoGGRandomOrder);
        entry.settings.put("delay_ms", this.autoGGDelayMs);
        entry.settings.put("menu_keybind", this.autoGGMenuKeybind);
    }

    private void populateCartHudSettings(ModuleConfigEntry entry) {
        if (entry == null) return;
        entry.settings.put("custom_x", this.cartHudCustomX);
        entry.settings.put("custom_y", this.cartHudCustomY);
    }

    private void populateCooldownHudSettings(ModuleConfigEntry entry) {
        if (entry == null) return;
        entry.settings.put("custom_x", this.cooldownHudCustomX);
        entry.settings.put("custom_y", this.cooldownHudCustomY);
        entry.settings.put("vertical", this.cooldownHudVertical);
        entry.settings.put("min_duration", this.cooldownHudMinDuration);
    }

    private void populateWaterDropSettings(ModuleConfigEntry entry) {
        if (entry == null) return;
        entry.settings.put("mode", this.waterDropMode);
        entry.settings.put("fall_threshold", this.waterDropFallThreshold);
        entry.settings.put("pickup_water", this.waterDropPickupWater);
        entry.settings.put("switch_back", this.waterDropSwitchBack);
        entry.settings.put("camera_mode", this.waterDropCameraMode);
        entry.settings.put("pitch_threshold", this.waterDropPitchThreshold);
        entry.settings.put("pickup_delay", this.waterDropPickupDelayMs);
        entry.settings.put("switch_delay", this.waterDropSwitchDelayMs);
        entry.settings.put("random_delay", this.waterDropRandomDelay);
        entry.settings.put("target_slot", this.waterDropTargetSlot);
        entry.settings.put("combat_guard", this.waterDropCombatGuard);
        entry.settings.put("pearl_guard", this.waterDropPearlGuard);
        entry.settings.put("nether_adapter", this.waterDropNetherAdapter);
        entry.settings.put("enable_water", this.waterDropEnableWater);
        entry.settings.put("enable_wind_charge", this.waterDropEnableWindCharge);
        entry.settings.put("enable_hay_block", this.waterDropEnableHayBlock);
        entry.settings.put("enable_slime_block", this.waterDropEnableSlimeBlock);
        entry.settings.put("enable_cobweb", this.waterDropEnableCobweb);
        entry.settings.put("enable_powder_snow", this.waterDropEnablePowderSnow);
    }

    private void populateClickPearlSettings(ModuleConfigEntry entry) {
        if (entry == null) return;
        entry.settings.put("trigger_keybind", this.clickPearlTriggerKeybind);
        entry.settings.put("mode", this.clickPearlMode);
        entry.settings.put("search_mode", this.clickPearlSearchMode);
        entry.settings.put("switch_back", this.clickPearlSwitchBack);
        entry.settings.put("return_pearl", this.clickPearlReturnPearl);
        entry.settings.put("switch_delay", this.clickPearlSwitchDelayMs);
        entry.settings.put("check_cooldown", this.clickPearlCheckCooldown);
        entry.settings.put("prefer_offhand", this.clickPearlPreferOffhand);
        entry.settings.put("random_delay", this.clickPearlRandomDelay);
        entry.settings.put("swing_hand", this.clickPearlSwingHand);
        entry.settings.put("target_slot", this.clickPearlTargetSlot);
        entry.settings.put("combat_guard", this.clickPearlCombatGuard);
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
                this.autoMaceSwapType = getSettingString(mace.settings, "swap_type", this.autoMaceSwapType);
                if (mace.settings.containsKey("engine_mode") && !mace.settings.containsKey("swap_type")) {
                    this.autoMaceSwapType = getSettingString(mace.settings, "engine_mode", this.autoMaceSwapType);
                }
                this.autoMaceEngineMode = this.autoMaceSwapType;
                this.autoMaceSourceMode = getSettingString(mace.settings, "source_mode", this.autoMaceSourceMode);
                this.autoMaceEnchantMode = getSettingString(mace.settings, "enchant_mode", this.autoMaceEnchantMode);
                this.autoMaceMissBehavior = getSettingString(mace.settings, "miss_behavior", this.autoMaceMissBehavior);
                this.autoMaceRestoreDelayMs = getSettingDouble(mace.settings, "restore_delay", this.autoMaceRestoreDelayMs);
                this.autoMaceMissChance = getSettingDouble(mace.settings, "miss_chance", this.autoMaceMissChance);
                this.autoMaceRandomDelay = getSettingBoolean(mace.settings, "random_delay", this.autoMaceRandomDelay);
                this.autoMaceMinFallDistance = getSettingDouble(mace.settings, "min_fall_distance", this.autoMaceMinFallDistance);
                this.autoMaceAutoSwitch = getSettingBoolean(mace.settings, "auto_switch", this.autoMaceAutoSwitch);
                this.autoMaceSilentAim = getSettingBoolean(mace.settings, "silent_aim", this.autoMaceSilentAim);
                this.autoMaceSilentAimRange = getSettingDouble(mace.settings, "silent_aim_range", this.autoMaceSilentAimRange);
                this.autoMaceMovementFix = getSettingBoolean(mace.settings, "movement_fix", this.autoMaceMovementFix);
                this.autoMaceHitboxExpand = getSettingDouble(mace.settings, "hitbox_expand", this.autoMaceHitboxExpand);
                this.autoMaceTargetPlayers = getSettingBoolean(mace.settings, "target_players", this.autoMaceTargetPlayers);
                this.autoMaceTargetMobs = getSettingBoolean(mace.settings, "target_mobs", this.autoMaceTargetMobs);
                this.autoMaceStayOnMace = getSettingBoolean(mace.settings, "stay_on_mace", this.autoMaceStayOnMace);
                this.autoMaceAttackDelayMs = getSettingDouble(mace.settings, "attack_delay", this.autoMaceAttackDelayMs);
                this.autoMaceHumanMode = getSettingBoolean(mace.settings, "human_mode", this.autoMaceHumanMode);
                this.autoMaceRandomJitter = getSettingBoolean(mace.settings, "random_jitter", this.autoMaceRandomJitter);
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
                this.autoSpearMaxSpeed = getSettingBoolean(spear.settings, "max_speed", this.autoSpearMaxSpeed);
                this.autoSpearCheckCharge = getSettingBoolean(spear.settings, "check_charge", this.autoSpearCheckCharge);
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
                this.autoShieldbreakerReactionDelaySec = getSettingDouble(sb.settings, "reaction_delay", this.autoShieldbreakerReactionDelaySec);
                this.autoShieldbreakerRandomDelay = getSettingBoolean(sb.settings, "random_delay", this.autoShieldbreakerRandomDelay);
                this.autoShieldbreakerAbortOnManualSwitch = getSettingBoolean(sb.settings, "abort_on_manual_switch", this.autoShieldbreakerAbortOnManualSwitch);
                this.autoShieldbreakerLegitMode = getSettingBoolean(sb.settings, "legit_mode", this.autoShieldbreakerLegitMode);
                this.autoShieldbreakerCheckAirTime = getSettingBoolean(sb.settings, "check_air_time", this.autoShieldbreakerCheckAirTime);
                this.autoShieldbreakerMaxAirTimeSec = getSettingDouble(sb.settings, "max_air_time", this.autoShieldbreakerMaxAirTimeSec);
            }
        }
        ModuleConfigEntry slam = getModuleEntry("auto_stun_slam");
        if (slam != null) {
            this.autoStunSlamEnabled = slam.enabled;
            if (slam.keybind != null) this.autoStunSlamKeybind.copyFrom(slam.keybind);
            if (slam.settings != null && !slam.settings.isEmpty()) {
                this.autoStunSlamPreset = getSettingString(slam.settings, "preset", this.autoStunSlamPreset);
                if (slam.settings.containsKey("engine_mode") && !slam.settings.containsKey("preset")) {
                    this.autoStunSlamPreset = getSettingString(slam.settings, "engine_mode", this.autoStunSlamPreset);
                }
                this.autoStunSlamEngineMode = this.autoStunSlamPreset;
                this.autoStunSlamMode = getSettingString(slam.settings, "mode", this.autoStunSlamMode);
                this.autoStunSlamDistance = getSettingDouble(slam.settings, "distance", this.autoStunSlamDistance);
                this.autoStunSlamChance = getSettingDouble(slam.settings, "chance", this.autoStunSlamChance);
                this.autoStunSlamAirTimeSec = getSettingDouble(slam.settings, "air_time", this.autoStunSlamAirTimeSec);
                this.autoStunSlamAxeDelayMs = getSettingDouble(slam.settings, "axe_delay", this.autoStunSlamAxeDelayMs);
                this.autoStunSlamAxeRandomizer = getSettingBoolean(slam.settings, "axe_randomizer", this.autoStunSlamAxeRandomizer);
                this.autoStunSlamAxeJitterMs = getSettingDouble(slam.settings, "axe_jitter", this.autoStunSlamAxeJitterMs);
                this.autoStunSlamMaceDelayMs = getSettingDouble(slam.settings, "mace_delay", this.autoStunSlamMaceDelayMs);
                this.autoStunSlamMaceRandomizer = getSettingBoolean(slam.settings, "mace_randomizer", this.autoStunSlamMaceRandomizer);
                this.autoStunSlamMaceJitterMs = getSettingDouble(slam.settings, "mace_jitter", this.autoStunSlamMaceJitterMs);
                this.autoStunSlamEnchantPreference = getSettingString(slam.settings, "enchant_preference", this.autoStunSlamEnchantPreference);
                this.autoStunSlamRestoreDelayMs = getSettingDouble(slam.settings, "restore_delay", this.autoStunSlamRestoreDelayMs);
                this.autoStunSlamRestoreRandomizer = getSettingBoolean(slam.settings, "restore_randomizer", this.autoStunSlamRestoreRandomizer);
                this.autoStunSlamStayOnWeapon = getSettingBoolean(slam.settings, "stay_on_weapon", this.autoStunSlamStayOnWeapon);
                this.autoStunSlamRandomDelay = getSettingBoolean(slam.settings, "random_delay", this.autoStunSlamRandomDelay);
                this.autoStunSlamLegitMode = getSettingBoolean(slam.settings, "legit_mode", this.autoStunSlamLegitMode);
                this.autoStunSlamNewDistance = getSettingDouble(slam.settings, "new_distance", this.autoStunSlamNewDistance);
                this.autoStunSlamNewMode = getSettingString(slam.settings, "new_mode", this.autoStunSlamNewMode);
                this.autoStunSlamNewChance = getSettingDouble(slam.settings, "new_chance", this.autoStunSlamNewChance);
                this.autoStunSlamNewAttackDelayMs = getSettingDouble(slam.settings, "new_delay", this.autoStunSlamNewAttackDelayMs);
                this.autoStunSlamAirCondition = getSettingString(slam.settings, "air_condition", this.autoStunSlamAirCondition);
                this.autoStunSlamMinFall = getSettingDouble(slam.settings, "min_fall", this.autoStunSlamMinFall);
                this.autoStunSlamNewAirCondition = getSettingString(slam.settings, "new_air_condition", this.autoStunSlamNewAirCondition);
                this.autoStunSlamNewMinFall = getSettingDouble(slam.settings, "new_min_fall", this.autoStunSlamNewMinFall);
                this.autoStunSlamNewAirTimeSec = getSettingDouble(slam.settings, "new_air_time", this.autoStunSlamNewAirTimeSec);
                this.autoStunSlamNewEnchant = getSettingString(slam.settings, "new_enchant", this.autoStunSlamNewEnchant);
                this.autoStunSlamNewSilentAim = getSettingBoolean(slam.settings, "new_silent_aim", this.autoStunSlamNewSilentAim);
                this.autoStunSlamNewRandomizer = getSettingBoolean(slam.settings, "new_randomizer", this.autoStunSlamNewRandomizer);
                this.autoStunSlamNewJitter = getSettingDouble(slam.settings, "new_jitter", this.autoStunSlamNewJitter);
                this.autoStunSlamNewStayOnMace = getSettingBoolean(slam.settings, "new_stay_on_mace", this.autoStunSlamNewStayOnMace);
            }
        }
        ModuleConfigEntry pearlCatch = getModuleEntry("auto_pearl_catch");
        if (pearlCatch != null) {
            this.autoPearlCatchEnabled = pearlCatch.enabled;
            if (pearlCatch.keybind != null) this.autoPearlCatchKeybind.copyFrom(pearlCatch.keybind);
            if (pearlCatch.settings != null && !pearlCatch.settings.isEmpty()) {
                this.autoPearlCatchMode = getSettingString(pearlCatch.settings, "mode", this.autoPearlCatchMode);
                this.autoPearlCatchDirection = getSettingString(pearlCatch.settings, "direction", this.autoPearlCatchDirection);
                Keybind actKb = getSettingKeybind(pearlCatch.settings, "action_keybind", null);
                if (actKb != null) this.autoPearlCatchActionKeybind.copyFrom(actKb);
                Keybind horKb = getSettingKeybind(pearlCatch.settings, "horizontal_keybind", null);
                if (horKb != null) this.autoPearlCatchHorizontalKeybind.copyFrom(horKb);
                Keybind thrKb = getSettingKeybind(pearlCatch.settings, "throw_keybind", null);
                if (thrKb != null) this.autoPearlCatchThrowKeybind.copyFrom(thrKb);
                Keybind asyKb = getSettingKeybind(pearlCatch.settings, "async_keybind", null);
                if (asyKb != null) this.autoPearlCatchAsyncKeybind.copyFrom(asyKb);
                this.autoPearlCatchThrowDelay = getSettingDouble(pearlCatch.settings, "throw_delay", this.autoPearlCatchThrowDelay);
                this.autoPearlCatchRestoreSlot = getSettingBoolean(pearlCatch.settings, "restore_slot", this.autoPearlCatchRestoreSlot);
                this.autoPearlCatchRestoreCamera = getSettingBoolean(pearlCatch.settings, "restore_camera", this.autoPearlCatchRestoreCamera);
                this.autoPearlCatchRotationTimeMs = getSettingDouble(pearlCatch.settings, "rotation_time_ms", this.autoPearlCatchRotationTimeMs);
                this.autoPearlCatchLegitMode = getSettingBoolean(pearlCatch.settings, "legit_mode", this.autoPearlCatchLegitMode);
                this.autoPearlCatchHorizontalOffset = getSettingDouble(pearlCatch.settings, "horizontal_offset", this.autoPearlCatchHorizontalOffset);
                this.autoPearlCatchRandomDelay = getSettingBoolean(pearlCatch.settings, "random_delay", this.autoPearlCatchRandomDelay);
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
                this.autoTotemMainhandTriggerHearts = getSettingDouble(totem.settings, "mainhand_trigger_hearts", this.autoTotemMainhandTriggerHearts);
                this.autoTotemMainhandRestoreHearts = getSettingDouble(totem.settings, "mainhand_restore_hearts", this.autoTotemMainhandRestoreHearts);
                this.autoTotemOffhandTriggerHearts = getSettingDouble(totem.settings, "offhand_trigger_hearts", this.autoTotemOffhandTriggerHearts);
                this.autoTotemOffhandRestoreHearts = getSettingDouble(totem.settings, "offhand_restore_hearts", this.autoTotemOffhandRestoreHearts);
                this.autoTotemCrystalTriggerHearts = getSettingDouble(totem.settings, "crystal_trigger_hearts", this.autoTotemCrystalTriggerHearts);
                this.autoTotemCrystalRestoreHearts = getSettingDouble(totem.settings, "crystal_restore_hearts", this.autoTotemCrystalRestoreHearts);
                this.autoTotemCountAbsorption = getSettingBoolean(totem.settings, "count_absorption", this.autoTotemCountAbsorption);
                this.autoTotemChance = getSettingDouble(totem.settings, "chance", this.autoTotemChance);
                this.autoTotemReturnItem = getSettingBoolean(totem.settings, "return_item", this.autoTotemReturnItem);
                this.autoTotemReturnOnPop = getSettingBoolean(totem.settings, "return_on_pop", this.autoTotemReturnOnPop);
                this.autoTotemAutoRefill = getSettingBoolean(totem.settings, "auto_refill", this.autoTotemAutoRefill);
                this.autoTotemRefillSlot = getSettingString(totem.settings, "refill_slot", this.autoTotemRefillSlot);
                this.autoTotemSwapBackDelay = getSettingDouble(totem.settings, "swap_back_delay", this.autoTotemSwapBackDelay);
                this.autoTotemAlwaysOffhand = getSettingBoolean(totem.settings, "always_offhand", this.autoTotemAlwaysOffhand);
                this.autoTotemIgnoreWhenUsing = getSettingBoolean(totem.settings, "ignore_when_using", this.autoTotemIgnoreWhenUsing);
                this.autoTotemPredictiveDamage = getSettingBoolean(totem.settings, "predictive_damage", this.autoTotemPredictiveDamage);
                this.autoTotemPredictCrystals = getSettingBoolean(totem.settings, "predict_crystals", this.autoTotemPredictCrystals);
                this.autoTotemPredictFall = getSettingBoolean(totem.settings, "predict_fall", this.autoTotemPredictFall);
                this.autoTotemPredictMace = getSettingBoolean(totem.settings, "predict_mace", this.autoTotemPredictMace);
                this.autoTotemPredictTrident = getSettingBoolean(totem.settings, "predict_trident", this.autoTotemPredictTrident);
                this.autoTotemLowTotemNotify = getSettingBoolean(totem.settings, "low_totem_notify", this.autoTotemLowTotemNotify);
                this.autoTotemInventorySource = getSettingString(totem.settings, "inventory_source", this.autoTotemInventorySource);
            }
        }
        ModuleConfigEntry cart = getModuleEntry("auto_cart");
        if (cart != null) {
            this.autoCartEnabled = cart.enabled;
            if (cart.keybind != null) this.autoCartKeybind.copyFrom(cart.keybind);
            if (cart.settings != null && !cart.settings.isEmpty()) {
                this.autoCartMode = getSettingString(cart.settings, "cart_mode", this.autoCartMode);
                this.autoCartPreset = getSettingString(cart.settings, "preset", this.autoCartPreset);
                Keybind macroBind = getSettingKeybind(cart.settings, "macro_keybind", null);
                if (macroBind != null) this.autoCartMacroKeybind.copyFrom(macroBind);
                this.autoCartMacroDrawTicks = getSettingDouble(cart.settings, "macro_draw_ticks", this.autoCartMacroDrawTicks);
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
                this.autoCartUseMainHand = getSettingBoolean(cart.settings, "use_mainhand_cart", this.autoCartUseMainHand);
                this.autoCartCameraMode = getSettingString(cart.settings, "camera_mode", this.autoCartCameraMode);
                this.autoCartAutoCamera = "auto".equalsIgnoreCase(this.autoCartCameraMode) || getSettingBoolean(cart.settings, "auto_camera", this.autoCartAutoCamera);
                this.autoCartCameraSmoothness = getSettingDouble(cart.settings, "camera_smoothness", this.autoCartCameraSmoothness);
                this.autoCartCameraReturn = getSettingBoolean(cart.settings, "camera_return", this.autoCartCameraReturn);
                this.autoCartCameraReturnSmoothness = getSettingDouble(cart.settings, "camera_return_smoothness", this.autoCartCameraReturnSmoothness);
                this.autoCartCameraCurve = getSettingDouble(cart.settings, "camera_curve", this.autoCartCameraCurve);
                this.autoCartCameraRandomness = getSettingDouble(cart.settings, "camera_randomness", this.autoCartCameraRandomness);
                this.autoCartCameraMouseGcd = getSettingBoolean(cart.settings, "camera_mouse_gcd", this.autoCartCameraMouseGcd);
                this.autoCartAdaptiveAim = getSettingBoolean(cart.settings, "adaptive_aim", this.autoCartAdaptiveAim);
                this.autoCartAutonomousPlacement = getSettingBoolean(cart.settings, "autonomous_placement", this.autoCartAutonomousPlacement);
            }
        }
        ModuleConfigEntry anchor = getModuleEntry("auto_anchor");
        if (anchor != null) {
            this.autoAnchorEnabled = anchor.enabled;
            if (anchor.keybind != null) this.autoAnchorKeybind.copyFrom(anchor.keybind);
            if (anchor.settings != null && !anchor.settings.isEmpty()) {
                this.autoAnchorMode = getSettingString(anchor.settings, "mode", this.autoAnchorMode);
                this.autoAnchorPreset = getSettingString(anchor.settings, "preset", this.autoAnchorPreset);
                this.autoAnchorPresetDouble = getSettingString(anchor.settings, "preset_double", this.autoAnchorPresetDouble);
                this.autoAnchorDoubleDelay = getSettingDouble(anchor.settings, "double_delay", this.autoAnchorDoubleDelay);
                this.autoAnchorDoubleAutoExplode = getSettingBoolean(anchor.settings, "double_auto_explode", this.autoAnchorDoubleAutoExplode);
                this.autoAnchorDoubleChain = getSettingBoolean(anchor.settings, "double_chain", this.autoAnchorDoubleChain);
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
                this.cartRefillRandomSpreadTicks = getSettingDouble(refill.settings, "random_spread", this.cartRefillRandomSpreadTicks);
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
                this.autoToolWeaponSwitch = getSettingBoolean(tool.settings, "weapon_switch", this.autoToolWeaponSwitch);
                this.autoToolDurabilitySaver = getSettingBoolean(tool.settings, "durability_saver", this.autoToolDurabilitySaver);
                this.autoToolDurabilityThreshold = getSettingDouble(tool.settings, "durability_threshold", this.autoToolDurabilityThreshold);
                this.autoToolPreferSilkTouch = getSettingBoolean(tool.settings, "prefer_silk_touch", this.autoToolPreferSilkTouch);
                this.autoToolRestorePrevious = getSettingBoolean(tool.settings, "restore_previous", this.autoToolRestorePrevious);
                this.autoToolLegitMode = getSettingBoolean(tool.settings, "legit_mode", this.autoToolLegitMode);
                this.autoToolSingleSlotMode = getSettingBoolean(tool.settings, "single_slot_mode", this.autoToolSingleSlotMode);
                this.autoToolSingleSlot = getSettingInt(tool.settings, "single_slot", this.autoToolSingleSlot);
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
                this.autoGGSendOnOwnDeath = getSettingBoolean(gg.settings, "send_on_death", getSettingBoolean(gg.settings, "send_on_own_death", this.autoGGSendOnOwnDeath));
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
        ModuleConfigEntry cd = getModuleEntry("cooldown_hud");
        if (cd != null) {
            this.cooldownHudEnabled = cd.enabled;
            if (cd.keybind != null) this.cooldownHudKeybind.copyFrom(cd.keybind);
            if (cd.settings != null && !cd.settings.isEmpty()) {
                this.cooldownHudCustomX = getSettingInt(cd.settings, "custom_x", this.cooldownHudCustomX);
                this.cooldownHudCustomY = getSettingInt(cd.settings, "custom_y", this.cooldownHudCustomY);
                this.cooldownHudVertical = getSettingBoolean(cd.settings, "vertical", this.cooldownHudVertical);
                this.cooldownHudMinDuration = getSettingDouble(cd.settings, "min_duration", this.cooldownHudMinDuration);
            }
        }

        ModuleConfigEntry wd = getModuleEntry("water_drop");
        if (wd != null) {
            this.waterDropEnabled = wd.enabled;
            if (wd.keybind != null) this.waterDropKeybind.copyFrom(wd.keybind);
            if (wd.settings != null && !wd.settings.isEmpty()) {
                this.waterDropMode = getSettingString(wd.settings, "mode", this.waterDropMode);
                this.waterDropFallThreshold = getSettingDouble(wd.settings, "fall_threshold", this.waterDropFallThreshold);
                this.waterDropPickupWater = getSettingBoolean(wd.settings, "pickup_water", this.waterDropPickupWater);
                this.waterDropSwitchBack = getSettingBoolean(wd.settings, "switch_back", this.waterDropSwitchBack);
                this.waterDropCameraMode = getSettingString(wd.settings, "camera_mode", this.waterDropCameraMode);
                this.waterDropPitchThreshold = getSettingDouble(wd.settings, "pitch_threshold", this.waterDropPitchThreshold);
                this.waterDropPickupDelayMs = getSettingDouble(wd.settings, "pickup_delay", this.waterDropPickupDelayMs);
                this.waterDropSwitchDelayMs = getSettingDouble(wd.settings, "switch_delay", this.waterDropSwitchDelayMs);
                this.waterDropRandomDelay = getSettingBoolean(wd.settings, "random_delay", this.waterDropRandomDelay);
                this.waterDropTargetSlot = getSettingString(wd.settings, "target_slot", this.waterDropTargetSlot);
                this.waterDropCombatGuard = getSettingBoolean(wd.settings, "combat_guard", this.waterDropCombatGuard);
                this.waterDropPearlGuard = getSettingBoolean(wd.settings, "pearl_guard", this.waterDropPearlGuard);
                this.waterDropNetherAdapter = getSettingBoolean(wd.settings, "nether_adapter", this.waterDropNetherAdapter);
                this.waterDropEnableWater = getSettingBoolean(wd.settings, "enable_water", this.waterDropEnableWater);
                this.waterDropEnableWindCharge = getSettingBoolean(wd.settings, "enable_wind_charge", this.waterDropEnableWindCharge);
                this.waterDropEnableHayBlock = getSettingBoolean(wd.settings, "enable_hay_block", this.waterDropEnableHayBlock);
                this.waterDropEnableSlimeBlock = getSettingBoolean(wd.settings, "enable_slime_block", this.waterDropEnableSlimeBlock);
                this.waterDropEnableCobweb = getSettingBoolean(wd.settings, "enable_cobweb", this.waterDropEnableCobweb);
                this.waterDropEnablePowderSnow = getSettingBoolean(wd.settings, "enable_powder_snow", this.waterDropEnablePowderSnow);
            }
        }

        ModuleConfigEntry cp = getModuleEntry("click_pearl");
        if (cp != null) {
            this.clickPearlEnabled = cp.enabled;
            if (cp.keybind != null) this.clickPearlKeybind.copyFrom(cp.keybind);
            if (cp.settings != null && !cp.settings.isEmpty()) {
                Keybind tkb = getSettingKeybind(cp.settings, "trigger_keybind", null);
                if (tkb != null) this.clickPearlTriggerKeybind.copyFrom(tkb);
                this.clickPearlMode = getSettingString(cp.settings, "mode", this.clickPearlMode);
                this.clickPearlSearchMode = getSettingString(cp.settings, "search_mode", this.clickPearlSearchMode);
                this.clickPearlSwitchBack = getSettingBoolean(cp.settings, "switch_back", this.clickPearlSwitchBack);
                this.clickPearlReturnPearl = getSettingBoolean(cp.settings, "return_pearl", this.clickPearlReturnPearl);
                this.clickPearlSwitchDelayMs = getSettingDouble(cp.settings, "switch_delay", this.clickPearlSwitchDelayMs);
                this.clickPearlCheckCooldown = getSettingBoolean(cp.settings, "check_cooldown", this.clickPearlCheckCooldown);
                this.clickPearlPreferOffhand = getSettingBoolean(cp.settings, "prefer_offhand", this.clickPearlPreferOffhand);
                this.clickPearlRandomDelay = getSettingBoolean(cp.settings, "random_delay", this.clickPearlRandomDelay);
                this.clickPearlSwingHand = getSettingBoolean(cp.settings, "swing_hand", this.clickPearlSwingHand);
                this.clickPearlTargetSlot = getSettingString(cp.settings, "target_slot", this.clickPearlTargetSlot);
                this.clickPearlCombatGuard = getSettingBoolean(cp.settings, "combat_guard", this.clickPearlCombatGuard);
            }
        }
    }

    private static double clampSanitize(double val, double min, double max, double def) {
        if (Double.isNaN(val) || Double.isInfinite(val)) return def;
        return Math.clamp(val, min, max);
    }

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

        copy.autoMaceEnabled = this.autoMaceEnabled;
        copy.autoMaceKeybind.copyFrom(this.autoMaceKeybind);
        copy.autoMaceSourceMode = this.autoMaceSourceMode;
        copy.autoMaceEnchantMode = this.autoMaceEnchantMode;
        copy.autoMaceMissBehavior = this.autoMaceMissBehavior;
        copy.autoMaceRestoreDelayMs = this.autoMaceRestoreDelayMs;
        copy.autoMaceLegitMode = this.autoMaceLegitMode;
        copy.autoMaceMissChance = this.autoMaceMissChance;
        copy.autoMaceRandomDelay = this.autoMaceRandomDelay;
        copy.autoMaceEngineMode = this.autoMaceEngineMode;
        copy.autoMaceMinFallDistance = this.autoMaceMinFallDistance;
        copy.autoMaceAutoSwitch = this.autoMaceAutoSwitch;
        copy.autoMaceSilentAim = this.autoMaceSilentAim;
        copy.autoMaceSilentAimRange = this.autoMaceSilentAimRange;
        copy.autoMaceMovementFix = this.autoMaceMovementFix;
        copy.autoMaceStunSlam = this.autoMaceStunSlam;
        copy.autoMaceHitboxExpand = this.autoMaceHitboxExpand;
        copy.autoMaceTargetPlayers = this.autoMaceTargetPlayers;
        copy.autoMaceTargetMobs = this.autoMaceTargetMobs;
        copy.autoMaceStayOnMace = this.autoMaceStayOnMace;
        copy.autoMaceAttackDelayMs = this.autoMaceAttackDelayMs;
        copy.autoMaceSwapType = this.autoMaceSwapType;
        copy.autoMaceHumanMode = this.autoMaceHumanMode;
        copy.autoMaceRandomJitter = this.autoMaceRandomJitter;
        copy.autoStunSlamPreset = this.autoStunSlamPreset;
        copy.autoStunSlamEngineMode = this.autoStunSlamEngineMode;

        copy.autoSpearEnabled = this.autoSpearEnabled;
        copy.autoSpearKeybind.copyFrom(this.autoSpearKeybind);
        copy.autoSpearTriggerKeybind.copyFrom(this.autoSpearTriggerKeybind);
        copy.autoSpearSecurityMode = this.autoSpearSecurityMode;
        copy.autoSpearPriorityMode = this.autoSpearPriorityMode;
        copy.autoSpearRestoreDelayMs = this.autoSpearRestoreDelayMs;
        copy.autoSpearMissChance = this.autoSpearMissChance;
        copy.autoSpearRandomDelay = this.autoSpearRandomDelay;
        copy.autoSpearMaxSpeed = this.autoSpearMaxSpeed;
        copy.autoSpearCheckCharge = this.autoSpearCheckCharge;

        copy.autoShieldbreakerEnabled = this.autoShieldbreakerEnabled;
        copy.autoShieldbreakerKeybind.copyFrom(this.autoShieldbreakerKeybind);
        copy.autoShieldbreakerMode = this.autoShieldbreakerMode;
        copy.autoShieldbreakerDistance = this.autoShieldbreakerDistance;
        copy.autoShieldbreakerChance = this.autoShieldbreakerChance;
        copy.autoShieldbreakerSwitchDelayMs = this.autoShieldbreakerSwitchDelayMs;
        copy.autoShieldbreakerRestoreDelayMs = this.autoShieldbreakerRestoreDelayMs;
        copy.autoShieldbreakerReactionDelaySec = this.autoShieldbreakerReactionDelaySec;
        copy.autoShieldbreakerRandomDelay = this.autoShieldbreakerRandomDelay;
        copy.autoShieldbreakerAbortOnManualSwitch = this.autoShieldbreakerAbortOnManualSwitch;
        copy.autoShieldbreakerLegitMode = this.autoShieldbreakerLegitMode;
        copy.autoShieldbreakerCheckAirTime = this.autoShieldbreakerCheckAirTime;
        copy.autoShieldbreakerMaxAirTimeSec = this.autoShieldbreakerMaxAirTimeSec;

        copy.autoStunSlamEnabled = this.autoStunSlamEnabled;
        copy.autoStunSlamKeybind.copyFrom(this.autoStunSlamKeybind);
        copy.autoStunSlamPreset = this.autoStunSlamPreset;
        copy.autoStunSlamEngineMode = this.autoStunSlamEngineMode;
        copy.autoStunSlamMode = this.autoStunSlamMode;
        copy.autoStunSlamDistance = this.autoStunSlamDistance;
        copy.autoStunSlamChance = this.autoStunSlamChance;
        copy.autoStunSlamAirCondition = this.autoStunSlamAirCondition;
        copy.autoStunSlamMinFall = this.autoStunSlamMinFall;
        copy.autoStunSlamAirTimeSec = this.autoStunSlamAirTimeSec;
        copy.autoStunSlamAxeDelayMs = this.autoStunSlamAxeDelayMs;
        copy.autoStunSlamAxeRandomizer = this.autoStunSlamAxeRandomizer;
        copy.autoStunSlamAxeJitterMs = this.autoStunSlamAxeJitterMs;
        copy.autoStunSlamMaceDelayMs = this.autoStunSlamMaceDelayMs;
        copy.autoStunSlamMaceRandomizer = this.autoStunSlamMaceRandomizer;
        copy.autoStunSlamMaceJitterMs = this.autoStunSlamMaceJitterMs;
        copy.autoStunSlamEnchantPreference = this.autoStunSlamEnchantPreference;
        copy.autoStunSlamRestoreDelayMs = this.autoStunSlamRestoreDelayMs;
        copy.autoStunSlamRestoreRandomizer = this.autoStunSlamRestoreRandomizer;
        copy.autoStunSlamStayOnWeapon = this.autoStunSlamStayOnWeapon;
        copy.autoStunSlamRandomDelay = this.autoStunSlamRandomDelay;
        copy.autoStunSlamLegitMode = this.autoStunSlamLegitMode;
        copy.autoStunSlamNewDistance = this.autoStunSlamNewDistance;
        copy.autoStunSlamNewMode = this.autoStunSlamNewMode;
        copy.autoStunSlamNewChance = this.autoStunSlamNewChance;
        copy.autoStunSlamNewAttackDelayMs = this.autoStunSlamNewAttackDelayMs;
        copy.autoStunSlamNewAirCondition = this.autoStunSlamNewAirCondition;
        copy.autoStunSlamNewMinFall = this.autoStunSlamNewMinFall;
        copy.autoStunSlamNewAirTimeSec = this.autoStunSlamNewAirTimeSec;
        copy.autoStunSlamNewEnchant = this.autoStunSlamNewEnchant;
        copy.autoStunSlamNewSilentAim = this.autoStunSlamNewSilentAim;
        copy.autoStunSlamNewRandomizer = this.autoStunSlamNewRandomizer;
        copy.autoStunSlamNewJitter = this.autoStunSlamNewJitter;
        copy.autoStunSlamNewStayOnMace = this.autoStunSlamNewStayOnMace;

        copy.autoPearlCatchEnabled = this.autoPearlCatchEnabled;
        copy.autoPearlCatchKeybind.copyFrom(this.autoPearlCatchKeybind);
        copy.autoPearlCatchActionKeybind.copyFrom(this.autoPearlCatchActionKeybind);
        copy.autoPearlCatchHorizontalKeybind.copyFrom(this.autoPearlCatchHorizontalKeybind);
        copy.autoPearlCatchThrowKeybind.copyFrom(this.autoPearlCatchThrowKeybind);
        copy.autoPearlCatchAsyncKeybind.copyFrom(this.autoPearlCatchAsyncKeybind);
        copy.autoPearlCatchMode = this.autoPearlCatchMode;
        copy.autoPearlCatchDirection = this.autoPearlCatchDirection;
        copy.autoPearlCatchThrowDelay = this.autoPearlCatchThrowDelay;
        copy.autoPearlCatchRestoreSlot = this.autoPearlCatchRestoreSlot;
        copy.autoPearlCatchRestoreCamera = this.autoPearlCatchRestoreCamera;
        copy.autoPearlCatchRotationTimeMs = this.autoPearlCatchRotationTimeMs;
        copy.autoPearlCatchLegitMode = this.autoPearlCatchLegitMode;
        copy.autoPearlCatchHorizontalOffset = this.autoPearlCatchHorizontalOffset;
        copy.autoPearlCatchRandomDelay = this.autoPearlCatchRandomDelay;
        copy.autoPearlCatchRandomSpreadMs = this.autoPearlCatchRandomSpreadMs;

        copy.autoTotemEnabled = this.autoTotemEnabled;
        copy.autoTotemKeybind.copyFrom(this.autoTotemKeybind);
        copy.autoTotemMode = this.autoTotemMode;
        copy.autoTotemTriggerHearts = this.autoTotemTriggerHearts;
        copy.autoTotemRestoreHearts = this.autoTotemRestoreHearts;
        copy.autoTotemMainhandTriggerHearts = this.autoTotemMainhandTriggerHearts;
        copy.autoTotemMainhandRestoreHearts = this.autoTotemMainhandRestoreHearts;
        copy.autoTotemOffhandTriggerHearts = this.autoTotemOffhandTriggerHearts;
        copy.autoTotemOffhandRestoreHearts = this.autoTotemOffhandRestoreHearts;
        copy.autoTotemCrystalTriggerHearts = this.autoTotemCrystalTriggerHearts;
        copy.autoTotemCrystalRestoreHearts = this.autoTotemCrystalRestoreHearts;
        copy.autoTotemCountAbsorption = this.autoTotemCountAbsorption;
        copy.autoTotemChance = this.autoTotemChance;
        copy.autoTotemReturnItem = this.autoTotemReturnItem;
        copy.autoTotemReturnOnPop = this.autoTotemReturnOnPop;
        copy.autoTotemAutoRefill = this.autoTotemAutoRefill;
        copy.autoTotemRefillSlot = this.autoTotemRefillSlot;
        copy.autoTotemSwapBackDelay = this.autoTotemSwapBackDelay;
        copy.autoTotemAlwaysOffhand = this.autoTotemAlwaysOffhand;
        copy.autoTotemIgnoreWhenUsing = this.autoTotemIgnoreWhenUsing;
        copy.autoTotemPredictiveDamage = this.autoTotemPredictiveDamage;
        copy.autoTotemPredictCrystals = this.autoTotemPredictCrystals;
        copy.autoTotemPredictFall = this.autoTotemPredictFall;
        copy.autoTotemPredictMace = this.autoTotemPredictMace;
        copy.autoTotemPredictTrident = this.autoTotemPredictTrident;
        copy.autoTotemLowTotemNotify = this.autoTotemLowTotemNotify;
        copy.autoTotemInventorySource = this.autoTotemInventorySource;

        copy.autoCartEnabled = this.autoCartEnabled;
        copy.autoCartKeybind.copyFrom(this.autoCartKeybind);
        copy.autoCartMacroKeybind.copyFrom(this.autoCartMacroKeybind);
        copy.autoCartMacroDrawTicks = this.autoCartMacroDrawTicks;
        copy.autoCartMode = this.autoCartMode;
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
        copy.autoCartUseMainHand = this.autoCartUseMainHand;
        copy.autoCartCameraMode = this.autoCartCameraMode;
        copy.autoCartAutoCamera = this.autoCartAutoCamera;
        copy.autoCartCameraSmoothness = this.autoCartCameraSmoothness;
        copy.autoCartCameraReturn = this.autoCartCameraReturn;
        copy.autoCartCameraReturnSmoothness = this.autoCartCameraReturnSmoothness;
        copy.autoCartCameraCurve = this.autoCartCameraCurve;
        copy.autoCartCameraRandomness = this.autoCartCameraRandomness;
        copy.autoCartCameraMouseGcd = this.autoCartCameraMouseGcd;
        copy.autoCartAdaptiveAim = this.autoCartAdaptiveAim;
        copy.autoCartAutonomousPlacement = this.autoCartAutonomousPlacement;

        copy.autoAnchorEnabled = this.autoAnchorEnabled;
        copy.autoAnchorKeybind.copyFrom(this.autoAnchorKeybind);
        copy.autoAnchorMode = this.autoAnchorMode;
        copy.autoAnchorPreset = this.autoAnchorPreset;
        copy.autoAnchorPresetDouble = this.autoAnchorPresetDouble;
        copy.autoAnchorDoubleDelay = this.autoAnchorDoubleDelay;
        copy.autoAnchorDoubleAutoExplode = this.autoAnchorDoubleAutoExplode;
        copy.autoAnchorDoubleChain = this.autoAnchorDoubleChain;
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
        copy.cartRefillRandomSpreadTicks = this.cartRefillRandomSpreadTicks;
        copy.cartRefillLegitMode = this.cartRefillLegitMode;

        copy.hpReaperEnabled = this.hpReaperEnabled;
        copy.hpReaperShowArmor = this.hpReaperShowArmor;
        copy.hpReaperShowDifference = this.hpReaperShowDifference;
        copy.hpReaperLowHealthHearts = this.hpReaperLowHealthHearts;
        copy.hpReaperLowHealthThreshold = this.hpReaperLowHealthThreshold;

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
        copy.autoToolSingleSlot = this.autoToolSingleSlot;
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

        copy.cooldownHudEnabled = this.cooldownHudEnabled;
        copy.cooldownHudKeybind.copyFrom(this.cooldownHudKeybind);
        copy.cooldownHudCustomX = this.cooldownHudCustomX;
        copy.cooldownHudCustomY = this.cooldownHudCustomY;
        copy.cooldownHudVertical = this.cooldownHudVertical;
        copy.cooldownHudMinDuration = this.cooldownHudMinDuration;

        copy.waterDropEnabled = this.waterDropEnabled;
        copy.waterDropKeybind.copyFrom(this.waterDropKeybind);
        copy.waterDropMode = this.waterDropMode;
        copy.waterDropFallThreshold = this.waterDropFallThreshold;
        copy.waterDropPickupWater = this.waterDropPickupWater;
        copy.waterDropSwitchBack = this.waterDropSwitchBack;
        copy.waterDropCameraMode = this.waterDropCameraMode;
        copy.waterDropPitchThreshold = this.waterDropPitchThreshold;
        copy.waterDropPickupDelayMs = this.waterDropPickupDelayMs;
        copy.waterDropSwitchDelayMs = this.waterDropSwitchDelayMs;
        copy.waterDropRandomDelay = this.waterDropRandomDelay;
        copy.waterDropTargetSlot = this.waterDropTargetSlot;
        copy.waterDropCombatGuard = this.waterDropCombatGuard;
        copy.waterDropPearlGuard = this.waterDropPearlGuard;
        copy.waterDropNetherAdapter = this.waterDropNetherAdapter;
        copy.waterDropEnableWater = this.waterDropEnableWater;
        copy.waterDropEnableWindCharge = this.waterDropEnableWindCharge;
        copy.waterDropEnableHayBlock = this.waterDropEnableHayBlock;
        copy.waterDropEnableSlimeBlock = this.waterDropEnableSlimeBlock;
        copy.waterDropEnableCobweb = this.waterDropEnableCobweb;
        copy.waterDropEnablePowderSnow = this.waterDropEnablePowderSnow;

        copy.clickPearlEnabled = this.clickPearlEnabled;
        copy.clickPearlKeybind.copyFrom(this.clickPearlKeybind);
        copy.clickPearlTriggerKeybind.copyFrom(this.clickPearlTriggerKeybind);
        copy.clickPearlMode = this.clickPearlMode;
        copy.clickPearlSearchMode = this.clickPearlSearchMode;
        copy.clickPearlSwitchBack = this.clickPearlSwitchBack;
        copy.clickPearlReturnPearl = this.clickPearlReturnPearl;
        copy.clickPearlSwitchDelayMs = this.clickPearlSwitchDelayMs;
        copy.clickPearlCheckCooldown = this.clickPearlCheckCooldown;
        copy.clickPearlPreferOffhand = this.clickPearlPreferOffhand;
        copy.clickPearlRandomDelay = this.clickPearlRandomDelay;
        copy.clickPearlSwingHand = this.clickPearlSwingHand;
        copy.clickPearlTargetSlot = this.clickPearlTargetSlot;
        copy.clickPearlCombatGuard = this.clickPearlCombatGuard;

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
        copy.hudCustomX = this.hudCustomX;
        copy.hudCustomY = this.hudCustomY;
        copy.hudShowActiveModules = this.hudShowActiveModules;

        copy.debugLogging = this.debugLogging;
        copy.profilerActive = this.profilerActive;
        copy.asyncTickEnabled = this.asyncTickEnabled;
        copy.logLevel = this.logLevel;
        copy.benchmarksEnabled = this.benchmarksEnabled;
        copy.maxCacheEntries = this.maxCacheEntries;
        copy.scissorOpt = this.scissorOpt;
        copy.filterRegex = this.filterRegex;
        copy.gcPolicy = this.gcPolicy;

        copy.guiTheme = this.guiTheme;
        copy.fontFamily = this.fontFamily;
        copy.typographySize = this.typographySize;
        copy.language = this.language;
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
        copy.menuKeybind.copyFrom(this.menuKeybind);
        copy.menuCommand = this.menuCommand;

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

               this.autoMaceEnabled == that.autoMaceEnabled &&
               this.autoMaceLegitMode == that.autoMaceLegitMode &&
               this.autoMaceRandomDelay == that.autoMaceRandomDelay &&
               Double.compare(this.autoMaceRestoreDelayMs, that.autoMaceRestoreDelayMs) == 0 &&
               Double.compare(this.autoMaceMissChance, that.autoMaceMissChance) == 0 &&
               Objects.equals(this.autoMaceKeybind, that.autoMaceKeybind) &&
               Objects.equals(this.autoMaceSourceMode, that.autoMaceSourceMode) &&
               Objects.equals(this.autoMaceEnchantMode, that.autoMaceEnchantMode) &&
               Objects.equals(this.autoMaceMissBehavior, that.autoMaceMissBehavior) &&
               Objects.equals(this.autoMaceEngineMode, that.autoMaceEngineMode) &&
               Double.compare(this.autoMaceMinFallDistance, that.autoMaceMinFallDistance) == 0 &&
               this.autoMaceAutoSwitch == that.autoMaceAutoSwitch &&
               this.autoMaceSilentAim == that.autoMaceSilentAim &&
               Double.compare(this.autoMaceSilentAimRange, that.autoMaceSilentAimRange) == 0 &&
               this.autoMaceMovementFix == that.autoMaceMovementFix &&
               this.autoMaceStunSlam == that.autoMaceStunSlam &&
               Double.compare(this.autoMaceHitboxExpand, that.autoMaceHitboxExpand) == 0 &&
               this.autoMaceTargetPlayers == that.autoMaceTargetPlayers &&
               this.autoMaceTargetMobs == that.autoMaceTargetMobs &&
               this.autoMaceStayOnMace == that.autoMaceStayOnMace &&
               Double.compare(this.autoMaceAttackDelayMs, that.autoMaceAttackDelayMs) == 0 &&
               Objects.equals(this.autoMaceSwapType, that.autoMaceSwapType) &&
               this.autoMaceHumanMode == that.autoMaceHumanMode &&
               this.autoMaceRandomJitter == that.autoMaceRandomJitter &&
               Objects.equals(this.autoStunSlamPreset, that.autoStunSlamPreset) &&
               Objects.equals(this.autoStunSlamEngineMode, that.autoStunSlamEngineMode) &&

               this.autoSpearEnabled == that.autoSpearEnabled &&
               this.autoSpearRandomDelay == that.autoSpearRandomDelay &&
               this.autoSpearMaxSpeed == that.autoSpearMaxSpeed &&
               this.autoSpearCheckCharge == that.autoSpearCheckCharge &&
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
               Double.compare(this.autoShieldbreakerReactionDelaySec, that.autoShieldbreakerReactionDelaySec) == 0 &&
               Objects.equals(this.autoShieldbreakerKeybind, that.autoShieldbreakerKeybind) &&
               Objects.equals(this.autoShieldbreakerMode, that.autoShieldbreakerMode) &&
               this.autoShieldbreakerCheckAirTime == that.autoShieldbreakerCheckAirTime &&
               Double.compare(this.autoShieldbreakerMaxAirTimeSec, that.autoShieldbreakerMaxAirTimeSec) == 0 &&

               Objects.equals(this.pinnedModules, that.pinnedModules) &&
               this.autoStunSlamEnabled == that.autoStunSlamEnabled &&
               this.autoStunSlamLegitMode == that.autoStunSlamLegitMode &&
               this.autoStunSlamRandomDelay == that.autoStunSlamRandomDelay &&
               Double.compare(this.autoStunSlamDistance, that.autoStunSlamDistance) == 0 &&
               Double.compare(this.autoStunSlamChance, that.autoStunSlamChance) == 0 &&
               Objects.equals(this.autoStunSlamAirCondition, that.autoStunSlamAirCondition) &&
               Double.compare(this.autoStunSlamMinFall, that.autoStunSlamMinFall) == 0 &&
               Double.compare(this.autoStunSlamAirTimeSec, that.autoStunSlamAirTimeSec) == 0 &&
               Double.compare(this.autoStunSlamAxeDelayMs, that.autoStunSlamAxeDelayMs) == 0 &&
               this.autoStunSlamAxeRandomizer == that.autoStunSlamAxeRandomizer &&
               Double.compare(this.autoStunSlamAxeJitterMs, that.autoStunSlamAxeJitterMs) == 0 &&
               Double.compare(this.autoStunSlamMaceDelayMs, that.autoStunSlamMaceDelayMs) == 0 &&
               this.autoStunSlamMaceRandomizer == that.autoStunSlamMaceRandomizer &&
               Double.compare(this.autoStunSlamMaceJitterMs, that.autoStunSlamMaceJitterMs) == 0 &&
               Objects.equals(this.autoStunSlamEnchantPreference, that.autoStunSlamEnchantPreference) &&
               Double.compare(this.autoStunSlamRestoreDelayMs, that.autoStunSlamRestoreDelayMs) == 0 &&
               this.autoStunSlamRestoreRandomizer == that.autoStunSlamRestoreRandomizer &&
               this.autoStunSlamStayOnWeapon == that.autoStunSlamStayOnWeapon &&
               Double.compare(this.autoStunSlamNewDistance, that.autoStunSlamNewDistance) == 0 &&
               Objects.equals(this.autoStunSlamNewMode, that.autoStunSlamNewMode) &&
               Double.compare(this.autoStunSlamNewChance, that.autoStunSlamNewChance) == 0 &&
               Double.compare(this.autoStunSlamNewAttackDelayMs, that.autoStunSlamNewAttackDelayMs) == 0 &&
               Objects.equals(this.autoStunSlamNewAirCondition, that.autoStunSlamNewAirCondition) &&
               Double.compare(this.autoStunSlamNewMinFall, that.autoStunSlamNewMinFall) == 0 &&
               Double.compare(this.autoStunSlamNewAirTimeSec, that.autoStunSlamNewAirTimeSec) == 0 &&
               Objects.equals(this.autoStunSlamNewEnchant, that.autoStunSlamNewEnchant) &&
               this.autoStunSlamNewSilentAim == that.autoStunSlamNewSilentAim &&
               this.autoStunSlamNewRandomizer == that.autoStunSlamNewRandomizer &&
               Double.compare(this.autoStunSlamNewJitter, that.autoStunSlamNewJitter) == 0 &&
               this.autoStunSlamNewStayOnMace == that.autoStunSlamNewStayOnMace &&
               Objects.equals(this.autoStunSlamKeybind, that.autoStunSlamKeybind) &&
               Objects.equals(this.autoStunSlamMode, that.autoStunSlamMode) &&

                this.autoPearlCatchEnabled == that.autoPearlCatchEnabled &&
                this.autoPearlCatchLegitMode == that.autoPearlCatchLegitMode &&
                this.autoPearlCatchRestoreSlot == that.autoPearlCatchRestoreSlot &&
                this.autoPearlCatchRestoreCamera == that.autoPearlCatchRestoreCamera &&
                this.autoPearlCatchRandomDelay == that.autoPearlCatchRandomDelay &&
                Double.compare(this.autoPearlCatchThrowDelay, that.autoPearlCatchThrowDelay) == 0 &&
                Double.compare(this.autoPearlCatchRotationTimeMs, that.autoPearlCatchRotationTimeMs) == 0 &&
                Double.compare(this.autoPearlCatchHorizontalOffset, that.autoPearlCatchHorizontalOffset) == 0 &&
                Double.compare(this.autoPearlCatchRandomSpreadMs, that.autoPearlCatchRandomSpreadMs) == 0 &&
                Objects.equals(this.autoPearlCatchKeybind, that.autoPearlCatchKeybind) &&
                Objects.equals(this.autoPearlCatchActionKeybind, that.autoPearlCatchActionKeybind) &&
                Objects.equals(this.autoPearlCatchHorizontalKeybind, that.autoPearlCatchHorizontalKeybind) &&
                Objects.equals(this.autoPearlCatchThrowKeybind, that.autoPearlCatchThrowKeybind) &&
                Objects.equals(this.autoPearlCatchAsyncKeybind, that.autoPearlCatchAsyncKeybind) &&
                Objects.equals(this.autoPearlCatchMode, that.autoPearlCatchMode) &&
                Objects.equals(this.autoPearlCatchDirection, that.autoPearlCatchDirection) &&

                 this.autoTotemEnabled == that.autoTotemEnabled &&
                 this.autoTotemReturnItem == that.autoTotemReturnItem &&
                 this.autoTotemReturnOnPop == that.autoTotemReturnOnPop &&
                 Double.compare(this.autoTotemTriggerHearts, that.autoTotemTriggerHearts) == 0 &&
                 Double.compare(this.autoTotemRestoreHearts, that.autoTotemRestoreHearts) == 0 &&
                 Double.compare(this.autoTotemMainhandTriggerHearts, that.autoTotemMainhandTriggerHearts) == 0 &&
                 Double.compare(this.autoTotemMainhandRestoreHearts, that.autoTotemMainhandRestoreHearts) == 0 &&
                 Double.compare(this.autoTotemOffhandTriggerHearts, that.autoTotemOffhandTriggerHearts) == 0 &&
                 Double.compare(this.autoTotemOffhandRestoreHearts, that.autoTotemOffhandRestoreHearts) == 0 &&
                 Double.compare(this.autoTotemCrystalTriggerHearts, that.autoTotemCrystalTriggerHearts) == 0 &&
                 Double.compare(this.autoTotemCrystalRestoreHearts, that.autoTotemCrystalRestoreHearts) == 0 &&
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
                this.autoCartUseMainHand == that.autoCartUseMainHand &&
                Objects.equals(this.autoCartKeybind, that.autoCartKeybind) &&
                Objects.equals(this.autoCartMode, that.autoCartMode) &&
                Objects.equals(this.autoCartPreset, that.autoCartPreset) &&
                Objects.equals(this.autoCartCameraMode, that.autoCartCameraMode) &&
                this.autoCartAutoCamera == that.autoCartAutoCamera &&
                Double.compare(this.autoCartCameraSmoothness, that.autoCartCameraSmoothness) == 0 &&
                this.autoCartCameraReturn == that.autoCartCameraReturn &&
                Double.compare(this.autoCartCameraReturnSmoothness, that.autoCartCameraReturnSmoothness) == 0 &&
                Double.compare(this.autoCartCameraCurve, that.autoCartCameraCurve) == 0 &&
                Double.compare(this.autoCartCameraRandomness, that.autoCartCameraRandomness) == 0 &&
                this.autoCartCameraMouseGcd == that.autoCartCameraMouseGcd &&
                this.autoCartAdaptiveAim == that.autoCartAdaptiveAim &&
                this.autoCartAutonomousPlacement == that.autoCartAutonomousPlacement &&

                this.autoAnchorEnabled == that.autoAnchorEnabled &&
                this.autoAnchorAutoExplode == that.autoAnchorAutoExplode &&
                this.autoAnchorAutoReturn == that.autoAnchorAutoReturn &&
                this.autoAnchorLegitMode == that.autoAnchorLegitMode &&
                this.autoAnchorDoubleAutoExplode == that.autoAnchorDoubleAutoExplode &&
                this.autoAnchorDoubleChain == that.autoAnchorDoubleChain &&
                Double.compare(this.autoAnchorChargeDelay, that.autoAnchorChargeDelay) == 0 &&
                Double.compare(this.autoAnchorExplodeDelay, that.autoAnchorExplodeDelay) == 0 &&
                Double.compare(this.autoAnchorDoubleDelay, that.autoAnchorDoubleDelay) == 0 &&
                Double.compare(this.autoAnchorChance, that.autoAnchorChance) == 0 &&
                Double.compare(this.autoAnchorTargetCharges, that.autoAnchorTargetCharges) == 0 &&
                Objects.equals(this.autoAnchorKeybind, that.autoAnchorKeybind) &&
                Objects.equals(this.autoAnchorMode, that.autoAnchorMode) &&
                Objects.equals(this.autoAnchorPreset, that.autoAnchorPreset) &&
                Objects.equals(this.autoAnchorPresetDouble, that.autoAnchorPresetDouble) &&

                this.cartRefillEnabled == that.cartRefillEnabled &&
                this.cartRefillLegitMode == that.cartRefillLegitMode &&
                this.cartRefillAutoClose == that.cartRefillAutoClose &&
                this.cartRefillRandomDelay == that.cartRefillRandomDelay &&
                Double.compare(this.cartRefillDelayTicks, that.cartRefillDelayTicks) == 0 &&
                Double.compare(this.cartRefillChance, that.cartRefillChance) == 0 &&
                Double.compare(this.cartRefillRandomSpreadTicks, that.cartRefillRandomSpreadTicks) == 0 &&
                Objects.equals(this.cartRefillKeybind, that.cartRefillKeybind) &&

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
               this.autoToolSingleSlot == that.autoToolSingleSlot &&
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

                this.cooldownHudEnabled == that.cooldownHudEnabled &&
                this.cooldownHudCustomX == that.cooldownHudCustomX &&
                this.cooldownHudCustomY == that.cooldownHudCustomY &&
                this.cooldownHudVertical == that.cooldownHudVertical &&
                Double.compare(this.cooldownHudMinDuration, that.cooldownHudMinDuration) == 0 &&
                Objects.equals(this.cooldownHudKeybind, that.cooldownHudKeybind) &&

                this.waterDropEnabled == that.waterDropEnabled &&
                Objects.equals(this.waterDropKeybind, that.waterDropKeybind) &&
                Objects.equals(this.waterDropMode, that.waterDropMode) &&
                Double.compare(this.waterDropFallThreshold, that.waterDropFallThreshold) == 0 &&
                this.waterDropPickupWater == that.waterDropPickupWater &&
                this.waterDropSwitchBack == that.waterDropSwitchBack &&
                Objects.equals(this.waterDropCameraMode, that.waterDropCameraMode) &&
                Double.compare(this.waterDropPitchThreshold, that.waterDropPitchThreshold) == 0 &&
                Double.compare(this.waterDropPickupDelayMs, that.waterDropPickupDelayMs) == 0 &&
                Double.compare(this.waterDropSwitchDelayMs, that.waterDropSwitchDelayMs) == 0 &&
                this.waterDropRandomDelay == that.waterDropRandomDelay &&
                Objects.equals(this.waterDropTargetSlot, that.waterDropTargetSlot) &&
                this.waterDropCombatGuard == that.waterDropCombatGuard &&
                this.waterDropPearlGuard == that.waterDropPearlGuard &&
                this.waterDropNetherAdapter == that.waterDropNetherAdapter &&
                this.waterDropEnableWater == that.waterDropEnableWater &&
                this.waterDropEnableWindCharge == that.waterDropEnableWindCharge &&
                this.waterDropEnableHayBlock == that.waterDropEnableHayBlock &&
                this.waterDropEnableSlimeBlock == that.waterDropEnableSlimeBlock &&
                this.waterDropEnableCobweb == that.waterDropEnableCobweb &&
                this.waterDropEnablePowderSnow == that.waterDropEnablePowderSnow &&

                this.clickPearlEnabled == that.clickPearlEnabled &&
                Objects.equals(this.clickPearlKeybind, that.clickPearlKeybind) &&
                Objects.equals(this.clickPearlTriggerKeybind, that.clickPearlTriggerKeybind) &&
                Objects.equals(this.clickPearlMode, that.clickPearlMode) &&
                Objects.equals(this.clickPearlSearchMode, that.clickPearlSearchMode) &&
                this.clickPearlSwitchBack == that.clickPearlSwitchBack &&
                this.clickPearlReturnPearl == that.clickPearlReturnPearl &&
                Double.compare(this.clickPearlSwitchDelayMs, that.clickPearlSwitchDelayMs) == 0 &&
                this.clickPearlCheckCooldown == that.clickPearlCheckCooldown &&
                this.clickPearlPreferOffhand == that.clickPearlPreferOffhand &&
                this.clickPearlRandomDelay == that.clickPearlRandomDelay &&
                this.clickPearlSwingHand == that.clickPearlSwingHand &&
                Objects.equals(this.clickPearlTargetSlot, that.clickPearlTargetSlot) &&
                this.clickPearlCombatGuard == that.clickPearlCombatGuard &&

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
               this.hudCustomX == that.hudCustomX &&
               this.hudCustomY == that.hudCustomY &&
               this.hudShowActiveModules == that.hudShowActiveModules &&
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
               Objects.equals(this.guiTheme, that.guiTheme) &&
               Objects.equals(this.fontFamily, that.fontFamily) &&
               Objects.equals(this.typographySize, that.typographySize) &&
               Objects.equals(this.language, that.language) &&
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
               Objects.equals(this.modules, that.modules) &&
               Objects.equals(this.menuKeybind, that.menuKeybind) &&
               Objects.equals(this.menuCommand, that.menuCommand);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(
            configVersion,
            pinnedModules,
            client,
            modules,
            menuKeybind,
            menuCommand,

            autoMaceEnabled, autoMaceKeybind, autoMaceSourceMode, autoMaceEnchantMode, autoMaceMissBehavior, autoMaceRestoreDelayMs, autoMaceLegitMode, autoMaceMissChance, autoMaceRandomDelay, autoMaceEngineMode, autoMaceSwapType, autoMaceMinFallDistance, autoMaceAutoSwitch, autoMaceSilentAim, autoMaceSilentAimRange, autoMaceMovementFix, autoMaceStunSlam, autoMaceHitboxExpand, autoMaceTargetPlayers, autoMaceTargetMobs, autoMaceStayOnMace, autoMaceAttackDelayMs, autoMaceHumanMode, autoMaceRandomJitter, autoStunSlamEngineMode, autoStunSlamPreset,
            autoSpearEnabled, autoSpearKeybind, autoSpearTriggerKeybind, autoSpearSecurityMode, autoSpearPriorityMode, autoSpearRestoreDelayMs, autoSpearMissChance, autoSpearRandomDelay, autoSpearMaxSpeed, autoSpearCheckCharge,
            autoShieldbreakerEnabled, autoShieldbreakerKeybind, autoShieldbreakerMode, autoShieldbreakerDistance, autoShieldbreakerChance,
            autoShieldbreakerSwitchDelayMs, autoShieldbreakerRestoreDelayMs, autoShieldbreakerReactionDelaySec, autoShieldbreakerRandomDelay, autoShieldbreakerAbortOnManualSwitch, autoShieldbreakerLegitMode, autoShieldbreakerCheckAirTime, autoShieldbreakerMaxAirTimeSec,
            autoStunSlamEnabled, autoStunSlamKeybind, autoStunSlamMode, autoStunSlamDistance, autoStunSlamChance
        );
        result = 31 * result + Objects.hash(
            autoStunSlamAirCondition, autoStunSlamMinFall, autoStunSlamAirTimeSec, autoStunSlamAxeDelayMs, autoStunSlamAxeRandomizer, autoStunSlamAxeJitterMs, autoStunSlamMaceDelayMs, autoStunSlamMaceRandomizer, autoStunSlamMaceJitterMs, autoStunSlamEnchantPreference, autoStunSlamRestoreDelayMs, autoStunSlamRestoreRandomizer, autoStunSlamStayOnWeapon, autoStunSlamRandomDelay, autoStunSlamLegitMode, autoStunSlamNewDistance, autoStunSlamNewMode, autoStunSlamNewChance, autoStunSlamNewAttackDelayMs, autoStunSlamNewAirCondition, autoStunSlamNewMinFall, autoStunSlamNewAirTimeSec, autoStunSlamNewEnchant, autoStunSlamNewSilentAim, autoStunSlamNewRandomizer, autoStunSlamNewJitter, autoStunSlamNewStayOnMace,
            autoPearlCatchEnabled, autoPearlCatchKeybind, autoPearlCatchActionKeybind, autoPearlCatchHorizontalKeybind, autoPearlCatchThrowKeybind, autoPearlCatchAsyncKeybind,
            autoPearlCatchMode, autoPearlCatchDirection, autoPearlCatchThrowDelay, autoPearlCatchRestoreSlot,
            autoPearlCatchRestoreCamera, autoPearlCatchRotationTimeMs, autoPearlCatchLegitMode, autoPearlCatchHorizontalOffset, autoPearlCatchRandomDelay, autoPearlCatchRandomSpreadMs,

            autoTotemEnabled, autoTotemKeybind, autoTotemMode, autoTotemTriggerHearts, autoTotemRestoreHearts, autoTotemCrystalTriggerHearts, autoTotemCrystalRestoreHearts, autoTotemChance, autoTotemReturnItem, autoTotemReturnOnPop,
            autoCartEnabled, autoCartKeybind, autoCartPreset, autoCartPlacementChance, autoCartMaxDistance, autoCartMinDelayMs, autoCartMaxDelayMs,
            autoCartAllowSelfCart, autoCartAllowPitPlacement, autoCartRandomDelay, autoCartRailDelay, autoCartCartDelay, autoCartRestoreDelay, autoCartLegitMode, autoCartUseMainHand,
            autoCartCameraMode, autoCartAutoCamera, autoCartCameraSmoothness, autoCartCameraReturn, autoCartCameraReturnSmoothness, autoCartCameraCurve, autoCartCameraRandomness, autoCartCameraMouseGcd, autoCartAdaptiveAim, autoCartAutonomousPlacement,
            autoAnchorEnabled, autoAnchorKeybind, autoAnchorMode, autoAnchorPreset, autoAnchorPresetDouble, autoAnchorDoubleDelay, autoAnchorDoubleAutoExplode, autoAnchorDoubleChain, autoAnchorAutoExplode, autoAnchorAutoReturn, autoAnchorChargeDelay, autoAnchorExplodeDelay, autoAnchorChance, autoAnchorTargetCharges, autoAnchorLegitMode,
            cartRefillEnabled, cartRefillKeybind, cartRefillDelayTicks, cartRefillChance, cartRefillAutoClose, cartRefillRandomDelay, cartRefillRandomSpreadTicks, cartRefillLegitMode
        );
        result = 31 * result + Objects.hash(

            hpReaperEnabled,
            hpReaperShowArmor,
            hpReaperShowDifference,
            hpReaperLowHealthHearts,
            hpReaperLowHealthThreshold,
 hpReaperKeybind, hpReaperMode, hpReaperTargetFilter,
            hpReaperOwnHealthX, hpReaperOwnHealthY, hpReaperCrosshairTargetX, hpReaperCrosshairTargetY, hpReaperTargetHealthX, hpReaperTargetHealthY, hpReaperDiffX, hpReaperDiffY,
            autoToolEnabled, autoToolKeybind, autoToolCombatGuard, autoToolDurabilitySaver, autoToolDurabilityThreshold, autoToolPreferSilkTouch, autoToolRestorePrevious, autoToolLegitMode, autoToolSingleSlotMode, autoToolSingleSlot, autoToolIgnoreInstantBreak, autoToolLockWhileMining,
            autoGGEnabled, autoGGKeybind, autoGGMenuKeybind, autoGGPhrase, autoGGSendOnKill, autoGGSendOnOwnDeath, autoGGRandomOrder, autoGGDelayMs,
            cartHudEnabled, cartHudKeybind, cartHudCustomX, cartHudCustomY,
            cooldownHudEnabled, cooldownHudKeybind, cooldownHudCustomX, cooldownHudCustomY, cooldownHudVertical, cooldownHudMinDuration,
            waterDropEnabled, waterDropKeybind, waterDropMode, waterDropFallThreshold, waterDropPickupWater,
            waterDropSwitchBack, waterDropCameraMode, waterDropPitchThreshold, waterDropPickupDelayMs, waterDropSwitchDelayMs,
            waterDropRandomDelay, waterDropTargetSlot, waterDropCombatGuard, waterDropPearlGuard, waterDropNetherAdapter,
            waterDropEnableWater, waterDropEnableWindCharge, waterDropEnableHayBlock, waterDropEnableSlimeBlock,
            waterDropEnableCobweb, waterDropEnablePowderSnow,

            clickPearlEnabled, clickPearlKeybind, clickPearlTriggerKeybind, clickPearlMode, clickPearlSearchMode,
            clickPearlSwitchBack, clickPearlReturnPearl, clickPearlSwitchDelayMs, clickPearlCheckCooldown, clickPearlPreferOffhand,
            clickPearlRandomDelay, clickPearlSwingHand, clickPearlTargetSlot, clickPearlCombatGuard,

            overlayEnabled, darkThemeEnabled, hudPosition, overlayOpacity, autoHideOnChat, hideInF3, searchFilter, filterCategory, matchCase
        );
        result = 31 * result + Objects.hash(
            activeProfile, themeVariant, compactMode, tooltipsEnabled, showKeyHints, smoothTransitions, soundVolume, audioClicks,
            customPrefix, toastStyle, showCoordinates, showFps, showBiome, showWorldTime, showDirection, coordFormat, hudPadding,
            customTitle, textShadow, hudCustomX, hudCustomY, hudShowActiveModules, debugLogging, profilerActive, asyncTickEnabled, logLevel, benchmarksEnabled, maxCacheEntries,
            scissorOpt, filterRegex, gcPolicy,
            guiTheme, fontFamily, typographySize, language, windowOpacity, panelOpacity, glassEffect, windowPosX, windowPosY, windowWidth, windowHeight, windowMaximized, unmaximizedX, unmaximizedY, unmaximizedWidth, unmaximizedHeight, soundEnabled, soundProfile, sliderSoundEnabled, animationsEnabled, spatialOpenAnimation
        );
        return result;
    }
}

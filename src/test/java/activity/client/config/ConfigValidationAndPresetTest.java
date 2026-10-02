package activity.client.config;

import activity.client.config.preset.Preset;
import activity.client.config.preset.PresetManager;
import activity.client.config.preset.PresetSerializer;
import activity.client.module.keybind.Keybind;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigValidationAndPresetTest {

    @BeforeEach
    void setUp() {
        PresetManager.resetToDefaults();
        ActivityConfigManager.resetDefaults();
    }

    @AfterEach
    void tearDown() {
        PresetManager.resetToDefaults();
    }

    @Test
    void testStructuredSchemaClientAndModules() {
        ActivityConfig config = new ActivityConfig();

        assertNotNull(config.client);
        assertNotNull(config.client.ui);
        assertNotNull(config.client.sound);
        assertNotNull(config.client.fonts);

        config.overlayEnabled = false;
        config.soundVolume = 45.0;
        config.fontFamily = "inter";
        config.syncClientSection();

        assertFalse(config.client.ui.overlayEnabled);
        assertEquals(45.0, config.client.sound.soundVolume);
        assertEquals("inter", config.client.fonts.fontFamily);

        config.client.ui.overlayEnabled = true;
        config.client.sound.soundVolume = 85.0;
        config.client.fonts.fontFamily = "manrope";
        config.syncFromClientSection();

        assertTrue(config.overlayEnabled);
        assertEquals(85.0, config.soundVolume);
        assertEquals("manrope", config.fontFamily);

        assertNotNull(config.modules);
        ActivityConfig.ModuleConfigEntry entry = new ActivityConfig.ModuleConfigEntry(true, new Keybind(82, true, false, false));
        entry.settings.put("source_mode", "sword_only");
        entry.settings.put("restore_delay", 110.0);
        config.modules.put("auto_mace", entry);

        ActivityConfig.ModuleConfigEntry fetched = NivoratConfigManager.getModuleConfig(config, "auto_mace");
        assertNotNull(fetched);
        assertTrue(fetched.enabled);
        assertEquals(82, fetched.keybind.getKeyCode());
        assertEquals("sword_only", fetched.settings.get("source_mode"));
        assertEquals(110.0, fetched.settings.get("restore_delay"));
    }

    @Test
    void testSanitizationClampingOutOfRangeNumbers() {
        ActivityConfig config = new ActivityConfig();

        config.autoMaceRestoreDelayMs = -999.0;
        config.autoMaceMissChance = 999.0;
        config.autoSpearRestoreDelayMs = 10000.0;
        config.autoSpearMissChance = -50.0;
        config.autoShieldbreakerDistance = 10.0;
        config.autoShieldbreakerChance = 500.0;
        config.autoStunSlamDistance = 0.5;
        config.autoStunSlamChance = -10.0;
        config.autoTotemTriggerHearts = 99.0;
        config.autoTotemRestoreHearts = -1.0;
        config.autoCartPlacementChance = 250.0;
        config.autoCartMaxDistance = 50.0;
        config.autoAnchorChance = 1000.0;
        config.cartRefillChance = -100.0;
        config.autoToolDurabilityThreshold = 500.0;
        config.autoGGDelayMs = 50.0;
        config.overlayOpacity = 5.0;
        config.windowOpacity = 500.0;
        config.panelOpacity = 5.0;

        config.sanitize();

        assertEquals(30.0, config.autoMaceRestoreDelayMs, "autoMaceRestoreDelayMs min 30.0");
        assertEquals(50.0, config.autoMaceMissChance, "autoMaceMissChance max 50.0");
        assertEquals(500.0, config.autoSpearRestoreDelayMs, "autoSpearRestoreDelayMs max 500.0");
        assertEquals(0.0, config.autoSpearMissChance, "autoSpearMissChance min 0.0");
        assertEquals(4.5, config.autoShieldbreakerDistance, "autoShieldbreakerDistance max 4.5");
        assertEquals(100.0, config.autoShieldbreakerChance, "autoShieldbreakerChance max 100.0");
        assertEquals(1.5, config.autoStunSlamDistance, "autoStunSlamDistance min 1.5");
        assertEquals(10.0, config.autoStunSlamChance, "autoStunSlamChance min 10.0");
        assertEquals(10.0, config.autoTotemTriggerHearts, "autoTotemTriggerHearts max 10.0");
        assertEquals(0.0, config.autoTotemRestoreHearts, "autoTotemRestoreHearts min 0.0");
        config.autoTotemRestoreHearts = 99.0;
        config.sanitize();
        assertEquals(20.0, config.autoTotemRestoreHearts, "autoTotemRestoreHearts max 20.0");
        assertEquals(100.0, config.autoCartPlacementChance, "autoCartPlacementChance max 100.0");
        assertEquals(4.5, config.autoCartMaxDistance, "autoCartMaxDistance max 4.5");
        assertEquals(100.0, config.autoAnchorChance, "autoAnchorChance max 100.0");
        assertEquals(10.0, config.cartRefillChance, "cartRefillChance min 10.0");
        assertEquals(50.0, config.autoToolDurabilityThreshold, "autoToolDurabilityThreshold max 50.0");
        assertEquals(100.0, config.autoGGDelayMs, "autoGGDelayMs min 100.0");
        assertEquals(10.0, config.overlayOpacity, "overlayOpacity min 10.0");
        assertEquals(100.0, config.windowOpacity, "windowOpacity max 100.0");
        assertEquals(20.0, config.panelOpacity, "panelOpacity min 20.0");
    }

    @Test
    void testSanitizationEnumFallbackToDefaults() {
        ActivityConfig config = new ActivityConfig();

        config.autoMaceSourceMode = "invalid_source";
        config.autoMaceEnchantMode = "broken_enchant";
        config.autoMaceMissBehavior = "unknown_behavior";
        config.autoSpearSecurityMode = "ultra_god";
        config.autoSpearPriorityMode = "non_existent_lunge";
        config.autoShieldbreakerMode = "weird_mode";
        config.autoStunSlamMode = "nonsense";
        config.autoTotemMode = "third_hand";
        config.autoCartPreset = "illegal_preset";
        config.autoAnchorPreset = "insane";
        config.hpReaperMode = "unsupported_hud";
        config.hpReaperTargetFilter = "aliens_only";
        config.hudPosition = "center_nowhere";
        config.fontFamily = "comic_sans_ms";
        config.typographySize = "gigantic";
        config.soundProfile = "techno";

        config.sanitize();

        assertEquals("sword_and_axe", config.autoMaceSourceMode);
        assertEquals("smart", config.autoMaceEnchantMode);
        assertEquals("sword_hit", config.autoMaceMissBehavior);
        assertEquals("legit", config.autoSpearSecurityMode);
        assertEquals("auto", config.autoSpearPriorityMode);
        assertEquals("full_auto", config.autoShieldbreakerMode);
        assertEquals("full_auto", config.autoStunSlamMode);
        assertEquals("main_hand", config.autoTotemMode);
        assertEquals("fast", config.autoCartPreset);
        assertEquals("balanced", config.autoAnchorPreset);
        assertEquals("target_hp", config.hpReaperMode);
        assertEquals("all_entities", config.hpReaperTargetFilter);
        assertEquals("top_right", config.hudPosition);
        assertEquals("minecraft", config.fontFamily);
        assertEquals("normal", config.typographySize);
        assertEquals("serene", config.soundProfile);
    }

    @Test
    void testAutoStunSlimeAliasMigration() {
        ActivityConfig config = new ActivityConfig();

        config.autoStunSlimeEnabled = true;
        config.autoStunSlimeDistance = 3.6;
        config.autoStunSlimeChance = 82.0;

        ActivityConfig.ModuleConfigEntry slimeEntry = new ActivityConfig.ModuleConfigEntry(true, new Keybind(70));
        slimeEntry.settings.put("distance", 3.6);
        config.modules.put("auto_stun_slime", slimeEntry);

        config.pinnedModules.add("auto_stun_slime");

        config.sanitize();

        assertTrue(config.autoStunSlamEnabled);
        assertEquals(3.6, config.autoStunSlamDistance);
        assertEquals(82.0, config.autoStunSlamChance);
        assertNull(config.autoStunSlimeEnabled);
        assertNull(config.autoStunSlimeDistance);

        assertTrue(config.modules.containsKey("auto_stun_slam"));
        assertFalse(config.modules.containsKey("auto_stun_slime"));

        assertTrue(config.pinnedModules.contains("auto_stun_slam"));
        assertFalse(config.pinnedModules.contains("auto_stun_slime"));

        ActivityConfig.ModuleConfigEntry viaAlias = NivoratConfigManager.getModuleConfig(config, "auto_stun_slime");
        assertNotNull(viaAlias);
        assertEquals(70, viaAlias.keybind.getKeyCode());
    }

    @Test
    void testPresetPreservesAll12ModulesAndStripsTransientUi() {
        ActivityConfig config = new ActivityConfig();
        config.autoMaceEnabled = false;
        config.autoSpearRestoreDelayMs = 210.0;
        config.autoShieldbreakerDistance = 3.4;
        config.autoStunSlamChance = 95.0;
        config.autoTotemTriggerHearts = 5.0;
        config.autoCartPlacementChance = 85.0;
        config.autoAnchorChance = 75.0;
        config.cartRefillDelayTicks = 4.0;
        config.hpReaperMode = "damage_diff";
        config.autoToolPreferSilkTouch = true;
        config.autoGGPhrase = "GG WP everyone";
        config.cartHudCustomX = 250;

        ActivityConfig.ModuleConfigEntry maceEntry = new ActivityConfig.ModuleConfigEntry(false, new Keybind(66));
        maceEntry.settings.put("restore_delay", 140.0);
        config.modules.put("auto_mace", maceEntry);

        config.windowPosX = 400;
        config.windowPosY = 300;
        config.windowWidth = 800;
        config.windowHeight = 600;
        config.searchFilter = "pvp_search";
        config.filterCategory = "Бой";
        config.matchCase = true;
        config.activeProfile = "custom_1";

        JsonObject snapshot = PresetSerializer.extractSettingsSnapshot(config);

        assertFalse(snapshot.has("windowPosX"));
        assertFalse(snapshot.has("windowPosY"));
        assertFalse(snapshot.has("windowWidth"));
        assertFalse(snapshot.has("windowHeight"));
        assertFalse(snapshot.has("searchFilter"));
        assertFalse(snapshot.has("filterCategory"));
        assertFalse(snapshot.has("matchCase"));
        assertFalse(snapshot.has("activeProfile"));

        ActivityConfig target = new ActivityConfig();
        target.windowPosX = 111;
        target.windowPosY = 222;
        target.searchFilter = "original_search";

        PresetSerializer.applySettingsSnapshot(snapshot, target);

        assertEquals(111, target.windowPosX);
        assertEquals(222, target.windowPosY);
        assertEquals("original_search", target.searchFilter);

        assertFalse(target.autoMaceEnabled);
        assertEquals(210.0, target.autoSpearRestoreDelayMs);
        assertEquals(3.4, target.autoShieldbreakerDistance);
        assertEquals(95.0, target.autoStunSlamChance);
        assertEquals(5.0, target.autoTotemTriggerHearts);
        assertEquals(85.0, target.autoCartPlacementChance);
        assertEquals(75.0, target.autoAnchorChance);
        assertEquals(4.0, target.cartRefillDelayTicks);
        assertEquals("damage_diff", target.hpReaperMode);
        assertTrue(target.autoToolPreferSilkTouch);
        assertEquals("GG WP everyone", target.autoGGPhrase);
        assertEquals(250, target.cartHudCustomX);

        assertTrue(target.modules.containsKey("auto_mace"));
        assertFalse(target.modules.get("auto_mace").enabled);
        assertEquals(66, target.modules.get("auto_mace").keybind.getKeyCode());
    }

    @Test
    void testFactoryResetIsolatesCustomPresets() {
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        cfg.autoMaceEnabled = false;
        cfg.autoTotemTriggerHearts = 5.5;

        Preset custom = PresetManager.createPreset("Saved Competitor Preset", cfg);
        assertNotNull(custom);
        assertTrue(PresetManager.hasPresetNamed("Saved Competitor Preset"));

        ActivityConfigManager.resetDefaults();

        ActivityConfig current = ActivityConfigManager.getConfig();
        assertTrue(current.autoMaceEnabled, "autoMaceEnabled must be reset to true");
        assertEquals(3.0, current.autoTotemTriggerHearts, "autoTotemTriggerHearts must be reset to 3.0");

        assertTrue(PresetManager.hasPresetNamed("Saved Competitor Preset"), "Custom presets must NOT be deleted by factory reset");
        Preset retrieved = PresetManager.getPresetByName("Saved Competitor Preset");
        assertNotNull(retrieved);
        assertEquals("Saved Competitor Preset", retrieved.getName());

        List<Preset> presets = PresetManager.getPresets();
        assertTrue(presets.stream().anyMatch(p -> "Saved Competitor Preset".equals(p.getName())));
    }

    @Test
    void testFullModulePresetRoundTripAll12Modules() {
        ActivityConfig src = new ActivityConfig();

        src.autoMaceEnabled = false;
        src.autoMaceKeybind.set(66, true, false, false);
        src.autoMaceSourceMode = "axe_only";
        src.autoMaceEnchantMode = "breach_only";
        src.autoMaceMissBehavior = "empty_swap";
        src.autoMaceRestoreDelayMs = 135.0;
        src.autoMaceMissChance = 25.0;
        src.autoMaceRandomDelay = false;
        src.autoMaceLegitMode = false;

        src.autoSpearEnabled = false;
        src.autoSpearKeybind.set(86, false, false, false);
        src.autoSpearSecurityMode = "rage";
        src.autoSpearPriorityMode = "lunge_3";
        src.autoSpearRestoreDelayMs = 230.0;
        src.autoSpearMissChance = 15.0;
        src.autoSpearRandomDelay = false;

        src.autoShieldbreakerEnabled = false;
        src.autoShieldbreakerMode = "semi_auto";
        src.autoShieldbreakerDistance = 3.3;
        src.autoShieldbreakerChance = 88.0;
        src.autoShieldbreakerSwitchDelayMs = 70.0;
        src.autoShieldbreakerRestoreDelayMs = 80.0;
        src.autoShieldbreakerRandomDelay = false;
        src.autoShieldbreakerAbortOnManualSwitch = false;
        src.autoShieldbreakerLegitMode = false;

        src.autoStunSlamEnabled = false;
        src.autoStunSlamMode = "semi_auto";
        src.autoStunSlamDistance = 3.2;
        src.autoStunSlamChance = 92.0;
        src.autoStunSlamAirTimeSec = 2.5;
        src.autoStunSlamAxeDelayMs = 65.0;
        src.autoStunSlamMaceDelayMs = 75.0;
        src.autoStunSlamRestoreDelayMs = 85.0;
        src.autoStunSlamRandomDelay = false;
        src.autoStunSlamLegitMode = false;

        src.autoTotemEnabled = false;
        src.autoTotemMode = "offhand";
        src.autoTotemTriggerHearts = 4.5;
        src.autoTotemRestoreHearts = 7.5;
        src.autoTotemMainhandTriggerHearts = 3.5;
        src.autoTotemMainhandRestoreHearts = 6.5;
        src.autoTotemOffhandTriggerHearts = 2.5;
        src.autoTotemOffhandRestoreHearts = 5.5;
        src.autoTotemCrystalTriggerHearts = 4.0;
        src.autoTotemCrystalRestoreHearts = 7.0;
        src.autoTotemCountAbsorption = true;
        src.autoTotemChance = 90.0;
        src.autoTotemReturnItem = false;
        src.autoTotemReturnOnPop = false;

        src.autoCartEnabled = false;
        src.autoCartPreset = "safe";
        src.autoCartPlacementChance = 85.0;
        src.autoCartMaxDistance = 3.6;
        src.autoCartMinDelayMs = 80.0;
        src.autoCartMaxDelayMs = 140.0;
        src.autoCartAllowSelfCart = true;
        src.autoCartAllowPitPlacement = false;
        src.autoCartRandomDelay = false;
        src.autoCartRailDelay = 3.0;
        src.autoCartCartDelay = 4.0;
        src.autoCartRestoreDelay = 5.0;
        src.autoCartLegitMode = false;

        src.autoAnchorEnabled = false;
        src.autoAnchorPreset = "fast";
        src.autoAnchorAutoExplode = true;
        src.autoAnchorAutoReturn = false;
        src.autoAnchorChargeDelay = 2.5;
        src.autoAnchorExplodeDelay = 3.5;
        src.autoAnchorChance = 95.0;
        src.autoAnchorTargetCharges = 3.0;
        src.autoAnchorLegitMode = false;

        src.cartRefillEnabled = false;
        src.cartRefillDelayTicks = 4.0;
        src.cartRefillChance = 88.0;
        src.cartRefillAutoClose = false;
        src.cartRefillRandomDelay = false;
        src.cartRefillLegitMode = false;

        src.hpReaperEnabled = false;
        src.hpReaperMode = "damage_diff";
        src.hpReaperTargetFilter = "players_only";
        src.hpReaperOwnHealthX = 120;
        src.hpReaperOwnHealthY = 220;
        src.hpReaperCrosshairTargetX = 320;
        src.hpReaperCrosshairTargetY = 420;
        src.hpReaperTargetHealthX = 520;
        src.hpReaperTargetHealthY = 620;
        src.hpReaperDiffX = 720;
        src.hpReaperDiffY = 820;

        src.autoToolEnabled = false;
        src.autoToolCombatGuard = false;
        src.autoToolDurabilitySaver = false;
        src.autoToolDurabilityThreshold = 15.0;
        src.autoToolPreferSilkTouch = true;
        src.autoToolRestorePrevious = false;
        src.autoToolLegitMode = false;
        src.autoToolSingleSlotMode = true;
        src.autoToolIgnoreInstantBreak = false;
        src.autoToolLockWhileMining = false;

        src.autoGGEnabled = false;
        src.autoGGPhrase = "Good Game!";
        src.autoGGSendOnKill = false;
        src.autoGGSendOnOwnDeath = true;
        src.autoGGRandomOrder = true;
        src.autoGGDelayMs = 1200.0;

        src.cartHudEnabled = false;
        src.cartHudCustomX = 350;
        src.cartHudCustomY = 450;

        src.windowPosX = 777;
        src.windowPosY = 888;
        src.searchFilter = "transient_filter";
        src.legacyMigrationDone = true;
        src.legacyMigrationVersion = 1;

        JsonObject snapshot = PresetSerializer.extractSettingsSnapshot(src);
        assertFalse(snapshot.has("windowPosX"));
        assertFalse(snapshot.has("windowPosY"));
        assertFalse(snapshot.has("searchFilter"));
        assertFalse(snapshot.has("legacyMigrationDone"));
        assertFalse(snapshot.has("legacyMigrationVersion"));

        ActivityConfig dst = new ActivityConfig();
        dst.windowPosX = 50;
        dst.windowPosY = 60;
        dst.searchFilter = "keep_this";

        PresetSerializer.applySettingsSnapshot(snapshot, dst);

        assertEquals(50, dst.windowPosX);
        assertEquals(60, dst.windowPosY);
        assertEquals("keep_this", dst.searchFilter);

        assertFalse(dst.autoMaceEnabled);
        assertEquals("axe_only", dst.autoMaceSourceMode);
        assertEquals("breach_only", dst.autoMaceEnchantMode);
        assertEquals("empty_swap", dst.autoMaceMissBehavior);
        assertEquals(135.0, dst.autoMaceRestoreDelayMs);
        assertEquals(25.0, dst.autoMaceMissChance);
        assertFalse(dst.autoMaceRandomDelay);
        assertFalse(dst.autoMaceLegitMode);

        assertFalse(dst.autoSpearEnabled);
        assertEquals("rage", dst.autoSpearSecurityMode);
        assertEquals("lunge_3", dst.autoSpearPriorityMode);
        assertEquals(230.0, dst.autoSpearRestoreDelayMs);
        assertEquals(15.0, dst.autoSpearMissChance);
        assertFalse(dst.autoSpearRandomDelay);

        assertFalse(dst.autoShieldbreakerEnabled);
        assertEquals("semi_auto", dst.autoShieldbreakerMode);
        assertEquals(3.3, dst.autoShieldbreakerDistance);
        assertEquals(88.0, dst.autoShieldbreakerChance);
        assertEquals(70.0, dst.autoShieldbreakerSwitchDelayMs);
        assertEquals(80.0, dst.autoShieldbreakerRestoreDelayMs);
        assertFalse(dst.autoShieldbreakerRandomDelay);
        assertFalse(dst.autoShieldbreakerAbortOnManualSwitch);
        assertFalse(dst.autoShieldbreakerLegitMode);

        assertFalse(dst.autoStunSlamEnabled);
        assertEquals("semi_auto", dst.autoStunSlamMode);
        assertEquals(3.2, dst.autoStunSlamDistance);
        assertEquals(92.0, dst.autoStunSlamChance);
        assertEquals(2.5, dst.autoStunSlamAirTimeSec);
        assertEquals(65.0, dst.autoStunSlamAxeDelayMs);
        assertEquals(75.0, dst.autoStunSlamMaceDelayMs);
        assertEquals(85.0, dst.autoStunSlamRestoreDelayMs);
        assertFalse(dst.autoStunSlamRandomDelay);
        assertFalse(dst.autoStunSlamLegitMode);

        assertFalse(dst.autoTotemEnabled);
        assertEquals("offhand", dst.autoTotemMode);
        assertEquals(4.5, dst.autoTotemTriggerHearts);
        assertEquals(7.5, dst.autoTotemRestoreHearts);
        assertEquals(3.5, dst.autoTotemMainhandTriggerHearts);
        assertEquals(6.5, dst.autoTotemMainhandRestoreHearts);
        assertEquals(2.5, dst.autoTotemOffhandTriggerHearts);
        assertEquals(5.5, dst.autoTotemOffhandRestoreHearts);
        assertEquals(4.0, dst.autoTotemCrystalTriggerHearts);
        assertEquals(7.0, dst.autoTotemCrystalRestoreHearts);
        assertTrue(dst.autoTotemCountAbsorption);
        assertEquals(90.0, dst.autoTotemChance);
        assertFalse(dst.autoTotemReturnItem);
        assertFalse(dst.autoTotemReturnOnPop);

        assertFalse(dst.autoCartEnabled);
        assertEquals("safe", dst.autoCartPreset);
        assertEquals(85.0, dst.autoCartPlacementChance);
        assertEquals(3.6, dst.autoCartMaxDistance);
        assertEquals(80.0, dst.autoCartMinDelayMs);
        assertEquals(140.0, dst.autoCartMaxDelayMs);
        assertTrue(dst.autoCartAllowSelfCart);
        assertFalse(dst.autoCartAllowPitPlacement);
        assertFalse(dst.autoCartRandomDelay);
        assertEquals(3.0, dst.autoCartRailDelay);
        assertEquals(4.0, dst.autoCartCartDelay);
        assertEquals(5.0, dst.autoCartRestoreDelay);
        assertFalse(dst.autoCartLegitMode);

        assertFalse(dst.autoAnchorEnabled);
        assertEquals("fast", dst.autoAnchorPreset);
        assertTrue(dst.autoAnchorAutoExplode);
        assertFalse(dst.autoAnchorAutoReturn);
        assertEquals(2.5, dst.autoAnchorChargeDelay);
        assertEquals(3.5, dst.autoAnchorExplodeDelay);
        assertEquals(95.0, dst.autoAnchorChance);
        assertEquals(3.0, dst.autoAnchorTargetCharges);
        assertFalse(dst.autoAnchorLegitMode);

        assertFalse(dst.cartRefillEnabled);
        assertEquals(4.0, dst.cartRefillDelayTicks);
        assertEquals(88.0, dst.cartRefillChance);
        assertFalse(dst.cartRefillAutoClose);
        assertFalse(dst.cartRefillRandomDelay);
        assertFalse(dst.cartRefillLegitMode);

        assertFalse(dst.hpReaperEnabled);
        assertEquals("damage_diff", dst.hpReaperMode);
        assertEquals("players_only", dst.hpReaperTargetFilter);
        assertEquals(120, dst.hpReaperOwnHealthX);
        assertEquals(220, dst.hpReaperOwnHealthY);
        assertEquals(320, dst.hpReaperCrosshairTargetX);
        assertEquals(420, dst.hpReaperCrosshairTargetY);
        assertEquals(520, dst.hpReaperTargetHealthX);
        assertEquals(620, dst.hpReaperTargetHealthY);
        assertEquals(720, dst.hpReaperDiffX);
        assertEquals(820, dst.hpReaperDiffY);

        assertFalse(dst.autoToolEnabled);
        assertFalse(dst.autoToolCombatGuard);
        assertFalse(dst.autoToolDurabilitySaver);
        assertEquals(15.0, dst.autoToolDurabilityThreshold);
        assertTrue(dst.autoToolPreferSilkTouch);
        assertFalse(dst.autoToolRestorePrevious);
        assertFalse(dst.autoToolLegitMode);
        assertTrue(dst.autoToolSingleSlotMode);
        assertFalse(dst.autoToolIgnoreInstantBreak);
        assertFalse(dst.autoToolLockWhileMining);

        assertFalse(dst.autoGGEnabled);
        assertEquals("Good Game!", dst.autoGGPhrase);
        assertFalse(dst.autoGGSendOnKill);
        assertTrue(dst.autoGGSendOnOwnDeath);
        assertTrue(dst.autoGGRandomOrder);
        assertEquals(1200.0, dst.autoGGDelayMs);

        assertFalse(dst.cartHudEnabled);
        assertEquals(350, dst.cartHudCustomX);
        assertEquals(450, dst.cartHudCustomY);
    }

    @Test
    void testAutoStunSlimeAliasInModuleRegistryAndConfigManager() {

        activity.client.module.api.IModule slamViaCanonical = activity.client.module.api.ModuleRegistry.get("auto_stun_slam");
        activity.client.module.api.IModule slamViaAlias = activity.client.module.api.ModuleRegistry.get("auto_stun_slime");
        activity.client.module.api.IModule slamViaCamelAlias = activity.client.module.api.ModuleRegistry.get("AutoStunSlime");
        activity.client.module.api.IModule slamViaCamelCanonical = activity.client.module.api.ModuleRegistry.get("AutoStunSlam");

        assertNotNull(slamViaCanonical);
        assertSame(slamViaCanonical, slamViaAlias);
        assertSame(slamViaCanonical, slamViaCamelAlias);
        assertSame(slamViaCanonical, slamViaCamelCanonical);

        ActivityConfig config = new ActivityConfig();
        config.autoStunSlamEnabled = false;
        config.syncModuleConfigEntries();

        ActivityConfig.ModuleConfigEntry entrySlime = NivoratConfigManager.getModuleConfig(config, "auto_stun_slime");
        ActivityConfig.ModuleConfigEntry entrySlam = NivoratConfigManager.getModuleConfig(config, "auto_stun_slam");
        ActivityConfig.ModuleConfigEntry entryCamelSlime = NivoratConfigManager.getModuleConfig(config, "AutoStunSlime");
        ActivityConfig.ModuleConfigEntry entryCamelSlam = NivoratConfigManager.getModuleConfig(config, "AutoStunSlam");

        assertNotNull(entrySlam);
        assertSame(entrySlam, entrySlime);
        assertSame(entrySlam, entryCamelSlime);
        assertSame(entrySlam, entryCamelSlam);
        assertFalse(entrySlam.enabled);
    }

    @Test
    void testSyncModuleEntriesPopulatesAndReadsAll12Modules() {
        ActivityConfig cfg = new ActivityConfig();
        cfg.autoMaceSourceMode = "axe_only";
        cfg.autoSpearSecurityMode = "rage";
        cfg.autoShieldbreakerDistance = 3.9;
        cfg.autoStunSlamAirTimeSec = 2.2;
        cfg.autoTotemRestoreHearts = 8.5;
        cfg.autoCartPlacementChance = 77.0;
        cfg.autoAnchorTargetCharges = 2.0;
        cfg.cartRefillDelayTicks = 5.0;
        cfg.hpReaperMode = "compact";
        cfg.autoToolDurabilityThreshold = 22.0;
        cfg.autoGGPhrase = "GF";
        cfg.cartHudCustomX = 180;

        cfg.syncModuleConfigEntries();

        assertEquals(16, cfg.modules.size());
        assertEquals("fast", cfg.modules.get("click_pearl").settings.get("mode"));
        assertEquals("axe_only", cfg.modules.get("auto_mace").settings.get("source_mode"));
        assertEquals("rage", cfg.modules.get("auto_spear").settings.get("security_mode"));
        assertEquals(3.9, cfg.modules.get("auto_shieldbreaker").settings.get("distance"));
        assertEquals(2.2, cfg.modules.get("auto_stun_slam").settings.get("air_time"));
        assertEquals(8.5, cfg.modules.get("auto_totem").settings.get("restore_hearts"));
        assertEquals(77.0, cfg.modules.get("auto_cart").settings.get("placement_chance"));
        assertEquals(2.0, cfg.modules.get("auto_anchor").settings.get("target_charges"));
        assertEquals(5.0, cfg.modules.get("cart_refill").settings.get("delay_ticks"));
        assertEquals("compact", cfg.modules.get("hp_reaper").settings.get("mode"));
        assertEquals(22.0, cfg.modules.get("auto_tool").settings.get("durability_threshold"));
        assertEquals("GF", cfg.modules.get("auto_gg").settings.get("phrase"));
        assertEquals(180, cfg.modules.get("cart_hud").settings.get("custom_x"));

        cfg.modules.get("auto_mace").settings.put("source_mode", "sword_only");
        cfg.modules.get("auto_spear").settings.put("security_mode", "semi_legit");
        cfg.modules.get("auto_shieldbreaker").settings.put("distance", 2.1);
        cfg.modules.get("auto_stun_slam").settings.put("air_time", 3.0);
        cfg.modules.get("auto_totem").settings.put("restore_hearts", 9.0);
        cfg.modules.get("auto_cart").settings.put("placement_chance", 99.0);
        cfg.modules.get("auto_anchor").settings.put("target_charges", 4.0);
        cfg.modules.get("cart_refill").settings.put("delay_ticks", 1.0);
        cfg.modules.get("hp_reaper").settings.put("mode", "own_hp");
        cfg.modules.get("auto_tool").settings.put("durability_threshold", 8.0);
        cfg.modules.get("auto_gg").settings.put("phrase", "Victory!");
        cfg.modules.get("cart_hud").settings.put("custom_x", 310);

        cfg.syncFromModuleEntries();

        assertEquals("sword_only", cfg.autoMaceSourceMode);
        assertEquals("semi_legit", cfg.autoSpearSecurityMode);
        assertEquals(2.1, cfg.autoShieldbreakerDistance);
        assertEquals(3.0, cfg.autoStunSlamAirTimeSec);
        assertEquals(9.0, cfg.autoTotemRestoreHearts);
        assertEquals(99.0, cfg.autoCartPlacementChance);
        assertEquals(4.0, cfg.autoAnchorTargetCharges);
        assertEquals(1.0, cfg.cartRefillDelayTicks);
        assertEquals("own_hp", cfg.hpReaperMode);
        assertEquals(8.0, cfg.autoToolDurabilityThreshold);
        assertEquals("Victory!", cfg.autoGGPhrase);
        assertEquals(310, cfg.cartHudCustomX);
    }

    @Test
    void testDirtyTrackingWithPinnedModulesAndClientSection() {
        ActivityConfig original = new ActivityConfig();
        ActivityConfig modified = original.copy();

        assertTrue(original.equals(modified));
        assertEquals(original.hashCode(), modified.hashCode());

        modified.setPinned("auto_mace", true);
        assertFalse(original.equals(modified), "Modifying pinnedModules must change equals()");
        assertNotEquals(original.hashCode(), modified.hashCode());

        modified.setPinned("auto_mace", false);
        assertTrue(original.equals(modified));

        modified.client.ui.overlayEnabled = false;
        assertFalse(original.equals(modified), "Modifying client.ui must change equals()");
        assertNotEquals(original.hashCode(), modified.hashCode());
    }

    @Test
    void testNivoratConfigManagerSetModuleConfigAndEnabled() {
        ActivityConfig cfg = new ActivityConfig();

        ActivityConfig.ModuleConfigEntry entry = new ActivityConfig.ModuleConfigEntry(true, new Keybind(77));
        entry.settings.put("distance", 3.7);
        NivoratConfigManager.setModuleConfig(cfg, "auto_stun_slime", entry);

        assertTrue(cfg.modules.containsKey("auto_stun_slam"));
        assertFalse(cfg.modules.containsKey("auto_stun_slime"));
        assertEquals(3.7, cfg.modules.get("auto_stun_slam").settings.get("distance"));

        NivoratConfigManager.setModuleEnabled("auto_mace", false);
        assertFalse(NivoratConfigManager.isModuleEnabled("auto_mace"));

        NivoratConfigManager.setModuleEnabled("auto_stun_slime", false);
        assertFalse(NivoratConfigManager.isModuleEnabled("auto_stun_slam"));
        assertFalse(NivoratConfigManager.isModuleEnabled("auto_stun_slime"));
    }

    @Test
    void testNumberSettingNaNSafety() {
        double[] store = new double[]{100.0};
        activity.client.module.setting.NumberSetting setting =
            new activity.client.module.setting.NumberSetting(
                "test_delay",
                net.minecraft.text.Text.literal("Delay"),
                net.minecraft.text.Text.literal("Desc"),
                activity.client.module.setting.SettingGroup.BEHAVIOR,
                10.0, 500.0, 5.0, "ms", false, 100.0,
                () -> store[0],
                val -> store[0] = val
            );

        setting.set(Double.NaN);
        assertEquals(100.0, setting.get(), "NaN must not corrupt NumberSetting value");

        setting.set(Double.POSITIVE_INFINITY);
        assertEquals(100.0, setting.get(), "Infinity must not corrupt NumberSetting value");

        setting.set(Double.NEGATIVE_INFINITY);
        assertEquals(100.0, setting.get(), "-Infinity must not corrupt NumberSetting value");

        activity.client.module.api.IModule breaker = activity.client.module.api.ModuleRegistry.get("auto_shieldbreaker");
        assertNotNull(breaker);
        activity.client.module.setting.Setting<?> delaySetting = breaker.getSetting("restore_delay");
        assertTrue(delaySetting instanceof activity.client.module.setting.NumberSetting);
        activity.client.module.setting.NumberSetting breakerDelay = (activity.client.module.setting.NumberSetting) delaySetting;

        breakerDelay.set(Double.NaN);
        assertEquals(breakerDelay.getDefaultValue(), breakerDelay.get());
        assertFalse(Double.isNaN(breakerDelay.get()));
    }

    @Test
    void testPresetPreservesProfilerAndMigrationFlags() {
        ActivityConfig src = new ActivityConfig();
        src.profilerActive = true;
        src.legacyMigrationDone = true;
        src.legacyMigrationVersion = 1;

        JsonObject snapshot = PresetSerializer.extractSettingsSnapshot(src);
        assertFalse(snapshot.has("profilerActive"));
        assertFalse(snapshot.has("legacyMigrationDone"));
        assertFalse(snapshot.has("legacyMigrationVersion"));

        ActivityConfig target = new ActivityConfig();
        target.profilerActive = true;
        target.legacyMigrationDone = true;
        target.legacyMigrationVersion = 1;

        PresetSerializer.applySettingsSnapshot(snapshot, target);
        assertTrue(target.profilerActive, "profilerActive must remain true after applying preset");
        assertTrue(target.legacyMigrationDone, "legacyMigrationDone must remain true after applying preset");
        assertEquals(1, target.legacyMigrationVersion, "legacyMigrationVersion must remain 1 after applying preset");
    }

    @Test
    void testRenamePreset() {
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        Preset custom = PresetManager.createPreset("Original Name", cfg);
        assertNotNull(custom);
        assertEquals("Original Name", custom.getName());

        cfg.activeProfile = "Original Name";

        boolean renamed = PresetManager.renamePreset(custom, "Renamed Name");
        assertTrue(renamed);
        assertEquals("Renamed Name", custom.getName());
        assertEquals("Renamed Name", cfg.activeProfile);

        Preset custom2 = PresetManager.createPreset("Second Preset", cfg);
        assertFalse(PresetManager.renamePreset(custom2, "Renamed Name"));

        assertFalse(PresetManager.renamePreset(PresetManager.getDefaultPreset(), "Any Name"));
        assertFalse(PresetManager.renamePreset(custom, null));
        assertFalse(PresetManager.renamePreset(custom, ""));
        assertFalse(PresetManager.renamePreset(custom, "   "));
        assertFalse(PresetManager.renamePreset(custom, "This Name Is Exceeding Thirty Two Characters Limit"));
        assertFalse(PresetManager.renamePreset(custom, "Invalid\nName"));
    }

    @Test
    void testSettingsTabResetToDefaultFont() {
        activity.client.gui.tab.SettingsTab settingsTab = new activity.client.gui.tab.SettingsTab();
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        activity.client.gui.font.FontManager.setFontFamily("rubik");
        cfg.fontFamily = "rubik";
        settingsTab.resetDefaults();
        assertEquals("minecraft", cfg.fontFamily);
        assertEquals(activity.client.gui.font.FontFamily.MINECRAFT, activity.client.gui.font.FontManager.getActiveFontFamily());
    }

    @Test
    void testDropdownRightClick() {
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        Preset p = PresetManager.createPreset("RightClickTarget", cfg);
        activity.client.gui.overlay.OverlayManager om = new activity.client.gui.overlay.OverlayManager();
        activity.client.gui.component.ActivityDropdown<Preset> dropdown = new activity.client.gui.component.ActivityDropdown<>(
            10, 10, 100, 20, om, java.util.List.of(p), p, pr -> net.minecraft.text.Text.literal(pr.getName()), null
        );
        java.util.concurrent.atomic.AtomicReference<Preset> rightClicked = new java.util.concurrent.atomic.AtomicReference<>();
        dropdown.setOnRightClick(rightClicked::set);
        assertNotNull(dropdown.getOnRightClick());

        net.minecraft.client.gui.Click rmb = new net.minecraft.client.gui.Click(15.0, 15.0, new net.minecraft.client.input.MouseInput(1, 0));
        boolean handled = dropdown.mouseClicked(rmb, false);
        assertTrue(handled);
        assertEquals(p, rightClicked.get());
    }

    @Test
    void testAutoTotemPresetRoundTripPerModeAndAbsorption() {
        ActivityConfig src = new ActivityConfig();
        src.autoTotemMode = "crystal";
        src.autoTotemMainhandTriggerHearts = 2.5;
        src.autoTotemMainhandRestoreHearts = 0.0;
        src.autoTotemOffhandTriggerHearts = 1.0;
        src.autoTotemOffhandRestoreHearts = 4.5;
        src.autoTotemCrystalTriggerHearts = 5.5;
        src.autoTotemCrystalRestoreHearts = 18.5;
        src.autoTotemCountAbsorption = true;

        com.google.gson.JsonObject snapshot = PresetSerializer.extractSettingsSnapshot(src);
        ActivityConfig dst = new ActivityConfig();
        PresetSerializer.applySettingsSnapshot(snapshot, dst);

        assertEquals("crystal", dst.autoTotemMode);
        assertEquals(2.5, dst.autoTotemMainhandTriggerHearts);
        assertEquals(0.0, dst.autoTotemMainhandRestoreHearts);
        assertEquals(1.0, dst.autoTotemOffhandTriggerHearts);
        assertEquals(4.5, dst.autoTotemOffhandRestoreHearts);
        assertEquals(5.5, dst.autoTotemCrystalTriggerHearts);
        assertEquals(18.5, dst.autoTotemCrystalRestoreHearts);
        assertTrue(dst.autoTotemCountAbsorption);
    }
}

package activity.client.module;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.config.preset.PresetSerializer;
import activity.client.gui.tab.UtilityTab;
import activity.client.module.api.BuiltinModules;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.api.NivoratModule;
import activity.client.module.impl.defense.CartRefillModule;
import activity.client.module.impl.utility.AutoGGModule;
import activity.client.module.impl.utility.AutoToolModule;
import activity.client.module.impl.utility.CartHudModule;
import activity.client.module.impl.utility.HPReaperModule;
import activity.client.module.service.CartStateService;
import activity.client.module.setting.ActionSetting;
import activity.client.module.setting.BooleanSetting;
import activity.client.module.setting.EnumSetting;
import activity.client.module.setting.NumberSetting;
import activity.client.module.setting.Setting;
import activity.client.module.setting.SettingGroup;
import activity.client.module.setting.StringSetting;
import dev.carthud.CartHudConfig;
import dev.carthud.CartHudOverlay;
import dev.hpreaper.HealthHudOverlay;
import dev.hpreaper.VitalityConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.elarion.autogg.AutoGGClient;
import ru.elarion.autotool.AutoToolClient;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test suite for Phase 5 migration of utility and HUD modules:
 * HPReaper, AutoTool, AutoGG, and CartHUD into NivoratClient.
 */
public class UtilityModulesMigrationTest {

    @BeforeAll
    static void initRegistry() {
        BuiltinModules.registerAll();
    }

    @BeforeEach
    void resetState() {
        ActivityConfigManager.resetDefaults();
    }

    @Test
    void testUtilityModulesRegisteredAsNivoratModule() {
        List<String> utilityIds = List.of(HPReaperModule.ID, AutoToolModule.ID, AutoGGModule.ID, CartHudModule.ID);
        for (String id : utilityIds) {
            IModule module = ModuleRegistry.get(id);
            assertNotNull(module, "Module " + id + " must be registered in ModuleRegistry");
            assertEquals(ModuleCategory.UTILITY, module.getCategory(), "Module " + id + " must have category UTILITY");
            assertInstanceOf(NivoratModule.class, module, "Module " + id + " must extend NivoratModule");
        }
    }

    @Test
    void testUtilitySettingGroupsStrictOrder() {
        List<String> utilityIds = List.of(HPReaperModule.ID, AutoToolModule.ID, AutoGGModule.ID, CartHudModule.ID);
        for (String id : utilityIds) {
            IModule module = ModuleRegistry.get(id);
            assertNotNull(module);
            List<Setting<?>> settings = module.getSettings();
            assertFalse(settings.isEmpty(), "Module " + id + " must have settings registered");

            int lastOrdinal = -1;
            for (Setting<?> s : settings) {
                SettingGroup group = s.getGroup();
                assertNotNull(group, "Setting " + s.getId() + " in " + id + " must have a group");
                assertTrue(group.ordinal() >= lastOrdinal,
                        "Setting " + s.getId() + " in module " + id + " violates group ordering: "
                                + group + " (ordinal " + group.ordinal() + ") < previous (ordinal " + lastOrdinal + ")");
                lastOrdinal = group.ordinal();
            }
        }
    }

    @Test
    void testHPReaperSettingsAndEngineSync() {
        IModule mod = ModuleRegistry.get(HPReaperModule.ID);
        assertInstanceOf(HPReaperModule.class, mod);
        HPReaperModule hpMod = (HPReaperModule) mod;
        ActivityConfig config = ActivityConfigManager.getConfig();

        // Default keybind unbound
        assertEquals(-1, mod.getKeybind().getKeyCode());

        // Settings presence
        assertNotNull(mod.getSetting("mode"));
        assertNotNull(mod.getSetting("display_mode")); // alias check
        assertNotNull(mod.getSetting("target_filter"));
        assertNotNull(mod.getSetting("open_editor"));

        // Custom section presence
        assertTrue(hpMod.hasCustomSection(), "HPReaperModule must provide custom section");

        // Test display mode sync
        EnumSetting modeSetting = (EnumSetting) mod.getSetting("mode");
        assertEquals(List.of("target_hp", "own_hp", "damage_diff", "compact"), modeSetting.getOptions());

        modeSetting.set("own_hp");
        assertEquals("own_hp", config.hpReaperMode);
        assertEquals(HealthHudOverlay.DisplayMode.OWN_HEALTH, VitalityConfig.displayMode);

        modeSetting.set("damage_diff");
        assertEquals("damage_diff", config.hpReaperMode);
        assertEquals(HealthHudOverlay.DisplayMode.OWN_TARGET_AND_DIFFERENCE, VitalityConfig.displayMode);

        modeSetting.set("compact");
        assertEquals("compact", config.hpReaperMode);
        assertEquals(HealthHudOverlay.DisplayMode.CROSSHAIR_AND_TARGET, VitalityConfig.displayMode);

        modeSetting.set("target_hp");
        assertEquals("target_hp", config.hpReaperMode);
        assertEquals(HealthHudOverlay.DisplayMode.TARGET_HEALTH, VitalityConfig.displayMode);

        // Test target filter sync
        EnumSetting filterSetting = (EnumSetting) mod.getSetting("target_filter");
        assertEquals(List.of("all_entities", "players_only", "hostile_and_players"), filterSetting.getOptions());

        filterSetting.set("players_only");
        assertEquals("players_only", config.hpReaperTargetFilter);
        assertEquals(VitalityConfig.TargetFilter.PLAYERS_ONLY, VitalityConfig.targetFilter);

        filterSetting.set("hostile_and_players");
        assertEquals("hostile_and_players", config.hpReaperTargetFilter);
        assertEquals(VitalityConfig.TargetFilter.HOSTILE_AND_PLAYERS, VitalityConfig.targetFilter);

        filterSetting.set("all_entities");
        assertEquals("all_entities", config.hpReaperTargetFilter);
        assertEquals(VitalityConfig.TargetFilter.ALL_ENTITIES, VitalityConfig.targetFilter);

        // Test enable/disable disables displayMode in VitalityConfig
        mod.setEnabled(false);
        assertFalse(config.hpReaperEnabled);
        assertEquals(HealthHudOverlay.DisplayMode.DISABLED, VitalityConfig.displayMode);

        mod.setEnabled(true);
        assertTrue(config.hpReaperEnabled);
        assertEquals(HealthHudOverlay.DisplayMode.TARGET_HEALTH, VitalityConfig.displayMode);

        // Test coordinates persistence and sync
        config.hpReaperCrosshairTargetX = 150;
        config.hpReaperCrosshairTargetY = 220;
        hpMod.syncControllerConfig(config);
        assertEquals(150, VitalityConfig.crosshairTargetX);
        assertEquals(220, VitalityConfig.crosshairTargetY);

        VitalityConfig.ownHealthX = 80;
        VitalityConfig.ownHealthY = 90;
        hpMod.saveToConfig(config);
        assertEquals(80, config.hpReaperOwnHealthX);
        assertEquals(90, config.hpReaperOwnHealthY);
    }

    @Test
    void testAutoToolSettingsAndEngineSync() {
        IModule mod = ModuleRegistry.get(AutoToolModule.ID);
        assertInstanceOf(AutoToolModule.class, mod);
        AutoToolModule toolMod = (AutoToolModule) mod;
        ActivityConfig config = ActivityConfigManager.getConfig();

        // Check keybind
        assertEquals(-1, mod.getKeybind().getKeyCode());

        // Check settings presence & aliases
        assertNotNull(mod.getSetting("prefer_silk"));
        assertNotNull(mod.getSetting("prefer_silk_touch")); // alias
        assertNotNull(mod.getSetting("restore_previous"));
        assertNotNull(mod.getSetting("restore_previous_item")); // alias
        assertNotNull(mod.getSetting("weapon_switch"));
        assertNotNull(mod.getSetting("durability_threshold"));
        assertNotNull(mod.getSetting("combat_guard"));
        assertNotNull(mod.getSetting("durability_saver"));
        assertNotNull(mod.getSetting("ignore_instant_break"));
        assertNotNull(mod.getSetting("lock_while_mining"));
        assertNotNull(mod.getSetting("legit_mode"));
        assertNotNull(mod.getSetting("single_slot_mode"));

        // Test prefer_silk sync
        BooleanSetting silkSetting = (BooleanSetting) mod.getSetting("prefer_silk");
        silkSetting.set(true);
        assertTrue(config.autoToolPreferSilkTouch);
        assertTrue(AutoToolClient.CONFIG.preferSilkTouch);
        silkSetting.set(false);
        assertFalse(config.autoToolPreferSilkTouch);
        assertFalse(AutoToolClient.CONFIG.preferSilkTouch);

        // Test weapon_switch sync
        BooleanSetting weaponSetting = (BooleanSetting) mod.getSetting("weapon_switch");
        weaponSetting.set(false);
        assertFalse(config.autoToolWeaponSwitch);
        assertFalse(AutoToolClient.CONFIG.weaponSwitch);
        weaponSetting.set(true);
        assertTrue(config.autoToolWeaponSwitch);
        assertTrue(AutoToolClient.CONFIG.weaponSwitch);

        // Test restore_previous sync
        BooleanSetting restoreSetting = (BooleanSetting) mod.getSetting("restore_previous");
        restoreSetting.set(false);
        assertFalse(config.autoToolRestorePrevious);
        assertFalse(AutoToolClient.CONFIG.restorePreviousItem);
        restoreSetting.set(true);
        assertTrue(config.autoToolRestorePrevious);
        assertTrue(AutoToolClient.CONFIG.restorePreviousItem);

        // Test durability threshold sync
        NumberSetting duraSetting = (NumberSetting) mod.getSetting("durability_threshold");
        assertEquals(1.0, duraSetting.getMin(), 0.001);
        assertEquals(50.0, duraSetting.getMax(), 0.001);
        assertEquals(1.0, duraSetting.getStep(), 0.001);
        assertTrue(duraSetting.isIntegerOnly());
        duraSetting.set(12.0);
        assertEquals(12.0, config.autoToolDurabilityThreshold, 0.001);
        assertEquals(12, AutoToolClient.CONFIG.durabilityThreshold);

        // Test combat guard sync
        BooleanSetting combatGuard = (BooleanSetting) mod.getSetting("combat_guard");
        combatGuard.set(false);
        assertFalse(config.autoToolCombatGuard);
        assertFalse(AutoToolClient.CONFIG.combatGuard);
        combatGuard.set(true);
        assertTrue(config.autoToolCombatGuard);
        assertTrue(AutoToolClient.CONFIG.combatGuard);

        // Test durability saver sync
        BooleanSetting duraSaver = (BooleanSetting) mod.getSetting("durability_saver");
        duraSaver.set(false);
        assertFalse(config.autoToolDurabilitySaver);
        assertFalse(AutoToolClient.CONFIG.durabilitySaver);
        duraSaver.set(true);
        assertTrue(config.autoToolDurabilitySaver);
        assertTrue(AutoToolClient.CONFIG.durabilitySaver);

        // Test ignore instant break sync
        BooleanSetting ignoreInstant = (BooleanSetting) mod.getSetting("ignore_instant_break");
        ignoreInstant.set(false);
        assertFalse(config.autoToolIgnoreInstantBreak);
        assertFalse(AutoToolClient.CONFIG.ignoreInstantBreak);
        ignoreInstant.set(true);
        assertTrue(config.autoToolIgnoreInstantBreak);
        assertTrue(AutoToolClient.CONFIG.ignoreInstantBreak);

        // Test lock while mining sync
        BooleanSetting lockMining = (BooleanSetting) mod.getSetting("lock_while_mining");
        lockMining.set(false);
        assertFalse(config.autoToolLockWhileMining);
        assertFalse(AutoToolClient.CONFIG.lockWhileMining);
        lockMining.set(true);
        assertTrue(config.autoToolLockWhileMining);
        assertTrue(AutoToolClient.CONFIG.lockWhileMining);

        // Test legit mode sync
        BooleanSetting legitMode = (BooleanSetting) mod.getSetting("legit_mode");
        legitMode.set(false);
        assertFalse(config.autoToolLegitMode);
        assertFalse(AutoToolClient.CONFIG.legitMode);
        legitMode.set(true);
        assertTrue(config.autoToolLegitMode);
        assertTrue(AutoToolClient.CONFIG.legitMode);

        // Test single slot mode sync
        BooleanSetting singleSlot = (BooleanSetting) mod.getSetting("single_slot_mode");
        singleSlot.set(true);
        assertTrue(config.autoToolSingleSlotMode);
        assertTrue(AutoToolClient.CONFIG.singleSlotMode);
        singleSlot.set(false);
        assertFalse(config.autoToolSingleSlotMode);
        assertFalse(AutoToolClient.CONFIG.singleSlotMode);

        // Test module enable/disable sync
        mod.setEnabled(false);
        assertFalse(config.autoToolEnabled);
        assertFalse(AutoToolClient.CONFIG.enabled);

        mod.setEnabled(true);
        assertTrue(config.autoToolEnabled);
        assertTrue(AutoToolClient.CONFIG.enabled);
    }

    @Test
    void testAutoGGSettingsAndEngineSync() {
        IModule mod = ModuleRegistry.get(AutoGGModule.ID);
        assertInstanceOf(AutoGGModule.class, mod);
        AutoGGModule ggMod = (AutoGGModule) mod;
        ActivityConfig config = ActivityConfigManager.getConfig();

        // Keybind default
        assertEquals(-1, mod.getKeybind().getKeyCode());

        // Settings presence
        assertNotNull(mod.getSetting("phrase"));
        assertNotNull(mod.getSetting("gg_phrase")); // alias
        assertNotNull(mod.getSetting("random_order"));
        assertNotNull(mod.getSetting("delay_ms"));
        assertNotNull(mod.getSetting("send_on_kill"));
        assertNotNull(mod.getSetting("send_on_death"));

        // Test phrase sync
        StringSetting phraseSetting = (StringSetting) mod.getSetting("phrase");
        phraseSetting.set("Well Played!");
        assertEquals("Well Played!", config.autoGGPhrase);
        assertTrue(AutoGGClient.CONFIG.phrases.contains("Well Played!"));

        // Test random order sync
        BooleanSetting randomOrder = (BooleanSetting) mod.getSetting("random_order");
        randomOrder.set(true);
        assertTrue(config.autoGGRandomOrder);
        assertTrue(AutoGGClient.CONFIG.randomOrder);
        randomOrder.set(false);
        assertFalse(config.autoGGRandomOrder);
        assertFalse(AutoGGClient.CONFIG.randomOrder);

        // Test delay_ms sync
        NumberSetting delaySetting = (NumberSetting) mod.getSetting("delay_ms");
        assertEquals(100.0, delaySetting.getMin(), 0.001);
        assertEquals(3000.0, delaySetting.getMax(), 0.001);
        assertEquals(50.0, delaySetting.getStep(), 0.001);
        assertTrue(delaySetting.isIntegerOnly());
        delaySetting.set(1200.0);
        assertEquals(1200.0, config.autoGGDelayMs, 0.001);
        assertEquals(1200.0, AutoGGClient.customDelayMs, 0.001);

        // Test send_on_kill sync
        BooleanSetting sendKill = (BooleanSetting) mod.getSetting("send_on_kill");
        sendKill.set(false);
        assertFalse(config.autoGGSendOnKill);
        assertFalse(AutoGGClient.CONFIG.sendOnKill);
        sendKill.set(true);
        assertTrue(config.autoGGSendOnKill);
        assertTrue(AutoGGClient.CONFIG.sendOnKill);

        // Test send_on_death sync
        BooleanSetting sendDeath = (BooleanSetting) mod.getSetting("send_on_death");
        sendDeath.set(true);
        assertTrue(config.autoGGSendOnOwnDeath);
        assertTrue(AutoGGClient.CONFIG.sendOnOwnDeath);
        sendDeath.set(false);
        assertFalse(config.autoGGSendOnOwnDeath);
        assertFalse(AutoGGClient.CONFIG.sendOnOwnDeath);

        // Test enable/disable sync
        mod.setEnabled(false);
        assertFalse(config.autoGGEnabled);
        assertFalse(AutoGGClient.CONFIG.enabled);

        mod.setEnabled(true);
        assertTrue(config.autoGGEnabled);
        assertTrue(AutoGGClient.CONFIG.enabled);

        // Test recordAttack invocation
        assertDoesNotThrow(() -> AutoGGClient.recordAttack(42));
    }

    @Test
    void testCartHudSettingsAndIsolation() {
        IModule mod = ModuleRegistry.get(CartHudModule.ID);
        assertInstanceOf(CartHudModule.class, mod);
        CartHudModule cartHudMod = (CartHudModule) mod;
        ActivityConfig config = ActivityConfigManager.getConfig();

        // Default keybind
        assertEquals(-1, mod.getKeybind().getKeyCode());

        // Settings presence
        assertNotNull(mod.getSetting("open_editor"));
        assertNotNull(mod.getSetting("reset_position"));

        // Test reset position action
        config.cartHudCustomX = 250;
        config.cartHudCustomY = 300;
        cartHudMod.syncEngineConfig(config);
        assertEquals(250, CartHudConfig.customX);
        assertEquals(300, CartHudConfig.customY);

        ActionSetting resetAction = (ActionSetting) mod.getSetting("reset_position");
        resetAction.execute();
        assertEquals(-1, config.cartHudCustomX);
        assertEquals(-1, config.cartHudCustomY);
        assertEquals(-1, CartHudConfig.customX);
        assertEquals(-1, CartHudConfig.customY);

        // Test enable/disable sync
        mod.setEnabled(false);
        assertFalse(config.cartHudEnabled);
        assertFalse(CartHudConfig.enabled);

        mod.setEnabled(true);
        assertTrue(config.cartHudEnabled);
        assertTrue(CartHudConfig.enabled);

        // Verify CartRefill does NOT own HUD rendering or coordinates
        IModule refillMod = ModuleRegistry.get(CartRefillModule.ID);
        assertNotNull(refillMod);
        assertNull(refillMod.getSetting("open_editor"), "CartRefill must NOT have open_editor setting");
        assertNull(refillMod.getSetting("reset_position"), "CartRefill must NOT have reset_position setting");

        // Verify CartHudOverlay delegates to CartStateService
        assertDoesNotThrow(() -> CartHudOverlay.countCarts(null));
        assertEquals(0, CartHudOverlay.countCarts(null));
    }

    @Test
    void testUtilityLocalization() throws Exception {
        try (InputStream ruStream = getClass().getResourceAsStream("/assets/activity/lang/ru_ru.json")) {
            assertNotNull(ruStream, "ru_ru.json must exist in classpath");
            String ruJson = new String(ruStream.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(ruJson.contains("\"activity.module.hp_reaper.name\": \"HPReaper\""));
            assertTrue(ruJson.contains("\"activity.module.auto_tool.name\": \"AutoTool\""));
            assertTrue(ruJson.contains("\"activity.module.auto_gg.name\": \"AutoGG\""));
            assertTrue(ruJson.contains("\"activity.module.cart_hud.name\": \"CartHUD\""));
            assertTrue(ruJson.contains("\"activity.setting.utility.hpreaper_mode\": \"Режим отображения\""));
            assertTrue(ruJson.contains("\"activity.setting.utility.target_filter\": \"Фильтр целей\""));
            assertTrue(ruJson.contains("\"activity.dropdown.target_filter.all_entities\": \"Все существа\""));
            assertTrue(ruJson.contains("\"activity.dropdown.target_filter.players_only\": \"Только игроки\""));
            assertTrue(ruJson.contains("\"activity.setting.utility.prefer_silk\": \"Приоритет Шёлка\""));
            assertTrue(ruJson.contains("\"activity.setting.utility.restore_previous\": \"Возврат предмета\""));
            assertTrue(ruJson.contains("\"activity.setting.utility.durability_threshold\": \"Порог прочности\""));
            assertTrue(ruJson.contains("\"activity.setting.utility.combat_guard\": \"Блокировка свапа в бою\""));
            assertTrue(ruJson.contains("\"activity.setting.utility.durability_saver\": \"Защита от поломки\""));
            assertTrue(ruJson.contains("\"activity.setting.utility.ignore_instant_break\": \"Игнорировать мгновенные\""));
            assertTrue(ruJson.contains("\"activity.setting.utility.lock_while_mining\": \"Блокировка при копании\""));
            assertTrue(ruJson.contains("\"activity.setting.utility.legit_mode\": \"Легитный режим\""));
            assertTrue(ruJson.contains("\"activity.setting.utility.single_slot_mode\": \"Один слот\""));
            assertTrue(ruJson.contains("\"activity.setting.utility.phrase\": \"Фраза GG\""));
            assertTrue(ruJson.contains("\"activity.setting.utility.random_order\": \"Случайный порядок\""));
            assertTrue(ruJson.contains("\"activity.setting.utility.delay_ms\": \"Задержка отправки\""));
            assertTrue(ruJson.contains("\"activity.setting.utility.send_on_kill\": \"Отправлять при победе\""));
            assertTrue(ruJson.contains("\"activity.setting.utility.send_on_death\": \"Отправлять при своей смерти\""));
            assertTrue(ruJson.contains("\"activity.setting.utility.reset_carthud_pos\": \"Сбросить позицию\""));
        }

        try (InputStream enStream = getClass().getResourceAsStream("/assets/activity/lang/en_us.json")) {
            assertNotNull(enStream, "en_us.json must exist in classpath");
            String enJson = new String(enStream.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(enJson.contains("\"activity.module.hp_reaper.name\": \"HPReaper\""));
            assertTrue(enJson.contains("\"activity.module.auto_tool.name\": \"AutoTool\""));
            assertTrue(enJson.contains("\"activity.module.auto_gg.name\": \"AutoGG\""));
            assertTrue(enJson.contains("\"activity.module.cart_hud.name\": \"CartHUD\""));
            assertTrue(enJson.contains("\"activity.setting.utility.hpreaper_mode\": \"Display Mode\""));
            assertTrue(enJson.contains("\"activity.setting.utility.target_filter\": \"Target Filter\""));
            assertTrue(enJson.contains("\"activity.dropdown.target_filter.all_entities\": \"All Entities\""));
            assertTrue(enJson.contains("\"activity.dropdown.target_filter.players_only\": \"Players Only\""));
            assertTrue(enJson.contains("\"activity.setting.utility.prefer_silk\": \"Prefer Silk Touch\""));
            assertTrue(enJson.contains("\"activity.setting.utility.restore_previous\": \"Restore Previous\""));
            assertTrue(enJson.contains("\"activity.setting.utility.durability_threshold\": \"Durability Threshold\""));
            assertTrue(enJson.contains("\"activity.setting.utility.combat_guard\": \"Combat Guard\""));
            assertTrue(enJson.contains("\"activity.setting.utility.durability_saver\": \"Durability Saver\""));
            assertTrue(enJson.contains("\"activity.setting.utility.ignore_instant_break\": \"Ignore Instant Break\""));
            assertTrue(enJson.contains("\"activity.setting.utility.lock_while_mining\": \"Lock While Mining\""));
            assertTrue(enJson.contains("\"activity.setting.utility.legit_mode\": \"Legit Mode\""));
            assertTrue(enJson.contains("\"activity.setting.utility.single_slot_mode\": \"Single Slot Mode\""));
            assertTrue(enJson.contains("\"activity.setting.utility.phrase\": \"GG Phrase\""));
            assertTrue(enJson.contains("\"activity.setting.utility.random_order\": \"Random Order\""));
            assertTrue(enJson.contains("\"activity.setting.utility.delay_ms\": \"Send Delay\""));
            assertTrue(enJson.contains("\"activity.setting.utility.send_on_kill\": \"Send On Kill\""));
            assertTrue(enJson.contains("\"activity.setting.utility.send_on_death\": \"Send On Own Death\""));
            assertTrue(enJson.contains("\"activity.setting.utility.reset_carthud_pos\": \"Reset Position\""));
        }
    }

    @Test
    void testUtilityTabResetDefaults() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        assertNotNull(config);

        // Mutate fields
        config.hpReaperEnabled = false;
        config.hpReaperMode = "compact";
        config.hpReaperTargetFilter = "players_only";
        config.hpReaperOwnHealthX = 120;
        config.autoToolEnabled = false;
        config.autoToolPreferSilkTouch = true;
        config.autoToolLegitMode = false;
        config.autoGGEnabled = false;
        config.autoGGPhrase = "Custom Phrase";
        config.autoGGDelayMs = 2500.0;
        config.cartHudEnabled = false;
        config.cartHudCustomX = 400;

        UtilityTab tab = new UtilityTab();
        tab.resetDefaults();

        // Check restored defaults
        assertTrue(config.hpReaperEnabled);
        assertEquals("target_hp", config.hpReaperMode);
        assertEquals("all_entities", config.hpReaperTargetFilter);
        assertEquals(-1, config.hpReaperOwnHealthX);
        assertTrue(config.autoToolEnabled);
        assertFalse(config.autoToolPreferSilkTouch);
        assertTrue(config.autoToolLegitMode);
        assertTrue(config.autoGGEnabled);
        assertEquals("GGWP", config.autoGGPhrase);
        assertEquals(950.0, config.autoGGDelayMs, 0.001);
        assertTrue(config.cartHudEnabled);
        assertEquals(-1, config.cartHudCustomX);
    }

    @Test
    void testPresetManagementCrossPresetPersistence() {
        ActivityConfig src = new ActivityConfig();
        src.hpReaperEnabled = false;
        src.hpReaperMode = "damage_diff";
        src.hpReaperTargetFilter = "hostile_and_players";
        src.hpReaperCrosshairTargetX = 50;
        src.autoToolPreferSilkTouch = true;
        src.autoToolLegitMode = false;
        src.autoToolSingleSlotMode = true;
        src.autoGGSendOnKill = false;
        src.autoGGDelayMs = 1500.0;
        src.cartHudEnabled = false;
        src.cartHudCustomX = 180;
        src.cartHudCustomY = 240;

        ActivityConfig dst = new ActivityConfig();
        PresetSerializer.copySettings(src, dst);

        assertEquals(src.hpReaperEnabled, dst.hpReaperEnabled);
        assertEquals(src.hpReaperMode, dst.hpReaperMode);
        assertEquals(src.hpReaperTargetFilter, dst.hpReaperTargetFilter);
        assertEquals(src.hpReaperCrosshairTargetX, dst.hpReaperCrosshairTargetX);
        assertEquals(src.autoToolPreferSilkTouch, dst.autoToolPreferSilkTouch);
        assertEquals(src.autoToolLegitMode, dst.autoToolLegitMode);
        assertEquals(src.autoToolSingleSlotMode, dst.autoToolSingleSlotMode);
        assertEquals(src.autoGGSendOnKill, dst.autoGGSendOnKill);
        assertEquals(src.autoGGDelayMs, dst.autoGGDelayMs, 0.001);
        assertEquals(src.cartHudEnabled, dst.cartHudEnabled);
        assertEquals(src.cartHudCustomX, dst.cartHudCustomX);
        assertEquals(src.cartHudCustomY, dst.cartHudCustomY);
    }

    @Test
    void testCustomSectionCardHeight() {
        IModule hpMod = ModuleRegistry.get(HPReaperModule.ID);
        assertNotNull(hpMod);
        assertTrue(hpMod.hasCustomSection());

        activity.client.gui.tab.UtilityTab tab = new activity.client.gui.tab.UtilityTab();
        activity.client.gui.layout.ScrollContainer container = new activity.client.gui.layout.ScrollContainer(0, 0, 400, 600);

        int calculatedH = activity.client.gui.builder.ModuleCardBuilder.buildCard(
                tab, null, container, hpMod, 10, 10, 300, 280
        );

        activity.client.gui.component.ActivityPanel card = tab.getModuleCard(hpMod.getId());
        assertNotNull(card, "HPReaper card must be registered in tab");
        assertEquals(calculatedH, card.getHeight(), "Card panel height must match calculated height including custom section");
        assertTrue(card.getHeight() > 22 + (1 + hpMod.getSettings().size()) * 26, "Card height must include custom section height");
    }

    @Test
    void testAutoGGSelectedPhraseSync() {
        IModule mod = ModuleRegistry.get(AutoGGModule.ID);
        assertNotNull(mod);
        AutoGGModule ggMod = (AutoGGModule) mod;

        StringSetting phraseSetting = (StringSetting) ggMod.getSetting("phrase");
        assertNotNull(phraseSetting);

        // Set to "EZ" which is at index 2 in defaults
        phraseSetting.set("EZ");
        assertEquals("EZ", AutoGGClient.CONFIG.phrases.get(AutoGGClient.CONFIG.selected));

        // Set to custom phrase
        phraseSetting.set("Great Fight!");
        assertEquals("Great Fight!", AutoGGClient.CONFIG.phrases.get(AutoGGClient.CONFIG.selected));
        assertFalse(AutoGGClient.CONFIG.randomOrder);
        assertEquals("Great Fight!", AutoGGClient.CONFIG.phrase());
    }

    @Test
    void testHpHudEditorScreenConstruction() {
        assertDoesNotThrow(() -> {
            assertNotNull(dev.hpreaper.HpHudEditorScreen.class.getConstructor(net.minecraft.client.gui.screen.Screen.class));
            assertNotNull(dev.hpreaper.HpHudEditorScreen.class.getConstructor());
            if (net.minecraft.client.MinecraftClient.getInstance() != null) {
                dev.hpreaper.HpHudEditorScreen screen = new dev.hpreaper.HpHudEditorScreen(null);
                assertNotNull(screen);
            }
        });
    }
}

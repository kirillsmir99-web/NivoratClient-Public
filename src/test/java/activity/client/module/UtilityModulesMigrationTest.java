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
import activity.client.module.impl.defense.ChunkBufferModule;
import activity.client.module.impl.utility.AudioWaveModule;
import activity.client.module.impl.utility.ModelMeshModule;
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
import dev.audio.AudioSyncClient;
import dev.mesh.ModelMeshClient;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UtilityModulesMigrationTest {

    @BeforeAll
    static void initRegistry() {
        net.minecraft.SharedConstants.createGameVersion();
        try {
            for (var field : net.minecraft.Bootstrap.class.getDeclaredFields()) if (field.getType() == boolean.class) {
                field.setAccessible(true); field.setBoolean(null, true);
            }
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
        BuiltinModules.registerAll();
    }

    @BeforeEach
    void resetState() {
        ActivityConfigManager.resetDefaults();
    }

    @Test
    void testUtilityModulesRegisteredAsNivoratModule() {
        List<String> utilityIds = List.of(HPReaperModule.ID, ModelMeshModule.ID, AudioWaveModule.ID, CartHudModule.ID);
        for (String id : utilityIds) {
            IModule module = ModuleRegistry.get(id);
            assertNotNull(module, "Module " + id + " must be registered in ModuleRegistry");
            assertEquals(ModuleCategory.UTILITY, module.getCategory(), "Module " + id + " must have category UTILITY");
            assertInstanceOf(NivoratModule.class, module, "Module " + id + " must extend NivoratModule");
        }
    }

    @Test
    void testUtilitySettingGroupsStrictOrder() {
        List<String> utilityIds = List.of(HPReaperModule.ID, ModelMeshModule.ID, AudioWaveModule.ID, CartHudModule.ID);
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

        assertEquals(-1, mod.getKeybind().getKeyCode());

        assertNotNull(mod.getSetting("mode"));
        assertNotNull(mod.getSetting("display_mode"));
        assertNotNull(mod.getSetting("target_filter"));
        assertNotNull(mod.getSetting("open_editor"));

        assertTrue(hpMod.hasCustomSection(), "HPReaperModule must provide custom section");

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

        mod.setEnabled(false);
        assertFalse(config.hpReaperEnabled);
        assertEquals(HealthHudOverlay.DisplayMode.DISABLED, VitalityConfig.displayMode);

        mod.setEnabled(true);
        assertTrue(config.hpReaperEnabled);
        assertEquals(HealthHudOverlay.DisplayMode.TARGET_HEALTH, VitalityConfig.displayMode);

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
        IModule mod = ModuleRegistry.get(ModelMeshModule.ID);
        assertInstanceOf(ModelMeshModule.class, mod);
        ModelMeshModule toolMod = (ModelMeshModule) mod;
        ActivityConfig config = ActivityConfigManager.getConfig();

        assertEquals(-1, mod.getKeybind().getKeyCode());

        assertNotNull(mod.getSetting("prefer_silk"));
        assertNotNull(mod.getSetting("prefer_silk_touch"));
        assertNotNull(mod.getSetting("restore_previous"));
        assertNotNull(mod.getSetting("restore_previous_item"));
        assertNotNull(mod.getSetting("weapon_switch"));
        assertNotNull(mod.getSetting("durability_threshold"));
        assertNotNull(mod.getSetting("combat_guard"));
        assertNotNull(mod.getSetting("durability_saver"));
        assertNotNull(mod.getSetting("ignore_instant_break"));
        assertNotNull(mod.getSetting("lock_while_mining"));
        assertNotNull(mod.getSetting("legit_mode"));
        assertNotNull(mod.getSetting("single_slot_mode"));
        assertNotNull(mod.getSetting("single_slot"));

        BooleanSetting silkSetting = (BooleanSetting) mod.getSetting("prefer_silk");
        silkSetting.set(true);
        assertTrue(config.autoToolPreferSilkTouch);
        assertTrue(ModelMeshClient.CONFIG.preferSilkTouch);
        silkSetting.set(false);
        assertFalse(config.autoToolPreferSilkTouch);
        assertFalse(ModelMeshClient.CONFIG.preferSilkTouch);

        BooleanSetting weaponSetting = (BooleanSetting) mod.getSetting("weapon_switch");
        weaponSetting.set(false);
        assertFalse(config.autoToolWeaponSwitch);
        assertFalse(ModelMeshClient.CONFIG.weaponSwitch);
        weaponSetting.set(true);
        assertTrue(config.autoToolWeaponSwitch);
        assertTrue(ModelMeshClient.CONFIG.weaponSwitch);

        BooleanSetting restoreSetting = (BooleanSetting) mod.getSetting("restore_previous");
        restoreSetting.set(false);
        assertFalse(config.autoToolRestorePrevious);
        assertFalse(ModelMeshClient.CONFIG.restorePreviousItem);
        restoreSetting.set(true);
        assertTrue(config.autoToolRestorePrevious);
        assertTrue(ModelMeshClient.CONFIG.restorePreviousItem);

        NumberSetting duraSetting = (NumberSetting) mod.getSetting("durability_threshold");
        assertEquals(1.0, duraSetting.getMin(), 0.001);
        assertEquals(50.0, duraSetting.getMax(), 0.001);
        assertEquals(1.0, duraSetting.getStep(), 0.001);
        assertTrue(duraSetting.isIntegerOnly());
        duraSetting.set(12.0);
        assertEquals(12.0, config.autoToolDurabilityThreshold, 0.001);
        assertEquals(12, ModelMeshClient.CONFIG.durabilityThreshold);

        BooleanSetting combatGuard = (BooleanSetting) mod.getSetting("combat_guard");
        combatGuard.set(false);
        assertFalse(config.autoToolCombatGuard);
        assertFalse(ModelMeshClient.CONFIG.combatGuard);
        combatGuard.set(true);
        assertTrue(config.autoToolCombatGuard);
        assertTrue(ModelMeshClient.CONFIG.combatGuard);

        BooleanSetting duraSaver = (BooleanSetting) mod.getSetting("durability_saver");
        duraSaver.set(false);
        assertFalse(config.autoToolDurabilitySaver);
        assertFalse(ModelMeshClient.CONFIG.durabilitySaver);
        duraSaver.set(true);
        assertTrue(config.autoToolDurabilitySaver);
        assertTrue(ModelMeshClient.CONFIG.durabilitySaver);

        BooleanSetting ignoreInstant = (BooleanSetting) mod.getSetting("ignore_instant_break");
        ignoreInstant.set(false);
        assertFalse(config.autoToolIgnoreInstantBreak);
        assertFalse(ModelMeshClient.CONFIG.ignoreInstantBreak);
        ignoreInstant.set(true);
        assertTrue(config.autoToolIgnoreInstantBreak);
        assertTrue(ModelMeshClient.CONFIG.ignoreInstantBreak);

        BooleanSetting lockMining = (BooleanSetting) mod.getSetting("lock_while_mining");
        lockMining.set(false);
        assertFalse(config.autoToolLockWhileMining);
        assertFalse(ModelMeshClient.CONFIG.lockWhileMining);
        lockMining.set(true);
        assertTrue(config.autoToolLockWhileMining);
        assertTrue(ModelMeshClient.CONFIG.lockWhileMining);

        BooleanSetting legitMode = (BooleanSetting) mod.getSetting("legit_mode");
        legitMode.set(false);
        assertFalse(config.autoToolLegitMode);
        assertFalse(ModelMeshClient.CONFIG.legitMode);
        legitMode.set(true);
        assertTrue(config.autoToolLegitMode);
        assertTrue(ModelMeshClient.CONFIG.legitMode);

        BooleanSetting singleSlot = (BooleanSetting) mod.getSetting("single_slot_mode");
        EnumSetting singleSlotSelector = (EnumSetting) mod.getSetting("single_slot");
        assertNotNull(singleSlotSelector);
        assertEquals(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9"), singleSlotSelector.getOptions());

        singleSlot.set(false);
        assertFalse(config.autoToolSingleSlotMode);
        assertFalse(ModelMeshClient.CONFIG.singleSlotMode);
        assertFalse(singleSlotSelector.isVisible());

        singleSlot.set(true);
        assertTrue(config.autoToolSingleSlotMode);
        assertTrue(ModelMeshClient.CONFIG.singleSlotMode);
        assertTrue(singleSlotSelector.isVisible());

        singleSlotSelector.set("4");
        assertEquals(3, config.autoToolSingleSlot);
        assertEquals(3, ModelMeshClient.CONFIG.singleSlot);

        singleSlot.set(false);
        assertFalse(config.autoToolSingleSlotMode);
        assertFalse(ModelMeshClient.CONFIG.singleSlotMode);

        mod.setEnabled(false);
        assertFalse(config.autoToolEnabled);
        assertFalse(ModelMeshClient.CONFIG.enabled);

        mod.setEnabled(true);
        assertTrue(config.autoToolEnabled);
        assertTrue(ModelMeshClient.CONFIG.enabled);
    }

    @Test
    void testAutoGGSettingsAndEngineSync() {
        IModule mod = ModuleRegistry.get(AudioWaveModule.ID);
        assertInstanceOf(AudioWaveModule.class, mod);
        AudioWaveModule ggMod = (AudioWaveModule) mod;
        ActivityConfig config = ActivityConfigManager.getConfig();

        assertEquals(-1, mod.getKeybind().getKeyCode());

        assertNotNull(mod.getSetting("phrase"));
        assertNotNull(mod.getSetting("gg_phrase"));
        assertNotNull(mod.getSetting("random_order"));
        assertNotNull(mod.getSetting("delay_ms"));
        assertNotNull(mod.getSetting("send_on_kill"));
        assertNotNull(mod.getSetting("send_on_death"));

        StringSetting phraseSetting = (StringSetting) mod.getSetting("phrase");
        phraseSetting.set("Well Played!");
        assertEquals("Well Played!", config.autoGGPhrase);
        assertTrue(AudioSyncClient.CONFIG.phrases.contains("Well Played!"));

        BooleanSetting randomOrder = (BooleanSetting) mod.getSetting("random_order");
        randomOrder.set(true);
        assertTrue(config.autoGGRandomOrder);
        assertTrue(AudioSyncClient.CONFIG.randomOrder);
        randomOrder.set(false);
        assertFalse(config.autoGGRandomOrder);
        assertFalse(AudioSyncClient.CONFIG.randomOrder);

        NumberSetting delaySetting = (NumberSetting) mod.getSetting("delay_ms");
        assertEquals(100.0, delaySetting.getMin(), 0.001);
        assertEquals(3000.0, delaySetting.getMax(), 0.001);
        assertEquals(50.0, delaySetting.getStep(), 0.001);
        assertTrue(delaySetting.isIntegerOnly());
        delaySetting.set(1200.0);
        assertEquals(1200.0, config.autoGGDelayMs, 0.001);
        assertEquals(1200.0, AudioSyncClient.customDelayMs, 0.001);

        BooleanSetting sendKill = (BooleanSetting) mod.getSetting("send_on_kill");
        sendKill.set(false);
        assertFalse(config.autoGGSendOnKill);
        assertFalse(AudioSyncClient.CONFIG.sendOnKill);
        sendKill.set(true);
        assertTrue(config.autoGGSendOnKill);
        assertTrue(AudioSyncClient.CONFIG.sendOnKill);

        BooleanSetting sendDeath = (BooleanSetting) mod.getSetting("send_on_death");
        sendDeath.set(true);
        assertTrue(config.autoGGSendOnOwnDeath);
        assertTrue(AudioSyncClient.CONFIG.sendOnOwnDeath);
        sendDeath.set(false);
        assertFalse(config.autoGGSendOnOwnDeath);
        assertFalse(AudioSyncClient.CONFIG.sendOnOwnDeath);

        mod.setEnabled(false);
        assertFalse(config.autoGGEnabled);
        assertFalse(AudioSyncClient.CONFIG.enabled);

        mod.setEnabled(true);
        assertTrue(config.autoGGEnabled);
        assertTrue(AudioSyncClient.CONFIG.enabled);

        assertDoesNotThrow(() -> AudioSyncClient.recordAttack(42));
    }

    @Test
    void testCartHudSettingsAndIsolation() {
        IModule mod = ModuleRegistry.get(CartHudModule.ID);
        assertInstanceOf(CartHudModule.class, mod);
        CartHudModule cartHudMod = (CartHudModule) mod;
        ActivityConfig config = ActivityConfigManager.getConfig();

        assertEquals(-1, mod.getKeybind().getKeyCode());

        assertNotNull(mod.getSetting("open_editor"));
        assertNotNull(mod.getSetting("reset_position"));

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

        mod.setEnabled(false);
        assertFalse(config.cartHudEnabled);
        assertFalse(CartHudConfig.enabled);

        mod.setEnabled(true);
        assertTrue(config.cartHudEnabled);
        assertTrue(CartHudConfig.enabled);

        IModule refillMod = ModuleRegistry.get(ChunkBufferModule.ID);
        assertNotNull(refillMod);
        assertNull(refillMod.getSetting("open_editor"), "CartRefill must NOT have open_editor setting");
        assertNull(refillMod.getSetting("reset_position"), "CartRefill must NOT have reset_position setting");

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
            assertTrue(ruJson.contains("\"activity.setting.utility.single_slot\": \"Слот для свапа\""));
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
            assertTrue(enJson.contains("\"activity.setting.utility.single_slot\": \"Swap Slot\""));
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
        src.autoToolSingleSlot = 3;
        src.autoGGSendOnKill = false;
        src.autoGGDelayMs = 1500.0;
        src.cartHudEnabled = false;
        src.cartHudCustomX = 180;
        src.cartHudCustomY = 240;
        src.hudCustomX = 120;
        src.hudCustomY = 80;
        src.hudShowActiveModules = false;

        ActivityConfig dst = new ActivityConfig();
        PresetSerializer.copySettings(src, dst);

        assertEquals(src.hpReaperEnabled, dst.hpReaperEnabled);
        assertEquals(src.hpReaperMode, dst.hpReaperMode);
        assertEquals(src.hpReaperTargetFilter, dst.hpReaperTargetFilter);
        assertEquals(src.hpReaperCrosshairTargetX, dst.hpReaperCrosshairTargetX);
        assertEquals(src.autoToolPreferSilkTouch, dst.autoToolPreferSilkTouch);
        assertEquals(src.autoToolLegitMode, dst.autoToolLegitMode);
        assertEquals(src.autoToolSingleSlotMode, dst.autoToolSingleSlotMode);
        assertEquals(src.autoToolSingleSlot, dst.autoToolSingleSlot);
        assertEquals(src.autoGGSendOnKill, dst.autoGGSendOnKill);
        assertEquals(src.autoGGDelayMs, dst.autoGGDelayMs, 0.001);
        assertEquals(src.cartHudEnabled, dst.cartHudEnabled);
        assertEquals(src.cartHudCustomX, dst.cartHudCustomX);
        assertEquals(src.cartHudCustomY, dst.cartHudCustomY);
        assertEquals(src.hudCustomX, dst.hudCustomX);
        assertEquals(src.hudCustomY, dst.hudCustomY);
        assertEquals(src.hudShowActiveModules, dst.hudShowActiveModules);
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
        IModule mod = ModuleRegistry.get(AudioWaveModule.ID);
        assertNotNull(mod);
        AudioWaveModule ggMod = (AudioWaveModule) mod;

        StringSetting phraseSetting = (StringSetting) ggMod.getSetting("phrase");
        assertNotNull(phraseSetting);

        phraseSetting.set("EZ");
        assertEquals("EZ", AudioSyncClient.CONFIG.phrases.get(AudioSyncClient.CONFIG.selected));

        phraseSetting.set("Great Fight!");
        assertEquals("Great Fight!", AudioSyncClient.CONFIG.phrases.get(AudioSyncClient.CONFIG.selected));
        assertFalse(AudioSyncClient.CONFIG.randomOrder);
        assertEquals("Great Fight!", AudioSyncClient.CONFIG.phrase());
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

    @Test
    void testNivoratHudEditorScreenConstruction() {
        assertDoesNotThrow(() -> {
            assertNotNull(activity.client.gui.hud.NivoratHudEditorScreen.class.getConstructor(net.minecraft.client.gui.screen.Screen.class));
            assertNotNull(activity.client.gui.hud.NivoratHudEditorScreen.class.getConstructor());
            if (net.minecraft.client.MinecraftClient.getInstance() != null) {
                activity.client.gui.hud.NivoratHudEditorScreen screen = new activity.client.gui.hud.NivoratHudEditorScreen(null);
                assertNotNull(screen);
            }
        });
    }

    @Test
    void testHealthHudOverlayCompatibilityMethods() {
        assertDoesNotThrow(() -> {
            assertNotNull(dev.hpreaper.HealthHudOverlay.class.getMethod("trackTarget", net.minecraft.entity.player.PlayerEntity.class));
            assertNotNull(dev.hpreaper.HealthHudOverlay.class.getMethod("trackTarget", net.minecraft.entity.LivingEntity.class));
            assertNotNull(dev.hpreaper.HealthHudOverlay.class.getMethod("trackTarget", net.minecraft.entity.Entity.class));
            assertNotNull(dev.hpreaper.HealthHudOverlay.class.getMethod("render", net.minecraft.client.gui.DrawContext.class, float.class));
            assertNotNull(dev.carthud.CartHudOverlay.class.getMethod("render", net.minecraft.client.gui.DrawContext.class, float.class));

            dev.hpreaper.HealthHudOverlay.trackTarget((net.minecraft.entity.player.PlayerEntity) null);
            dev.hpreaper.HealthHudOverlay.trackTarget((net.minecraft.entity.LivingEntity) null);
            dev.hpreaper.HealthHudOverlay.trackTarget((net.minecraft.entity.Entity) null);
            dev.hpreaper.HealthHudOverlay.render(null, 1.0f);
            dev.carthud.CartHudOverlay.render(null, 1.0f);
        });
    }
}

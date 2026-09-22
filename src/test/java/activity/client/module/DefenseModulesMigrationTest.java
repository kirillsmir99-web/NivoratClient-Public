package activity.client.module;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.module.api.BuiltinModules;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.api.NivoratModule;
import activity.client.module.impl.defense.AutoAnchorModule;
import activity.client.module.impl.defense.AutoCartModule;
import activity.client.module.impl.defense.AutoTotemModule;
import activity.client.module.impl.defense.CartRefillModule;
import activity.client.module.service.CartStateService;
import activity.client.module.setting.BooleanSetting;
import activity.client.module.setting.EnumSetting;
import activity.client.module.setting.NumberSetting;
import activity.client.module.setting.Setting;
import activity.client.module.setting.SettingGroup;
import dev.autototem.AutoTotemConfig;
import dev.luminance.AnchorConfig;
import dev.storage.RefillConfig;
import dev.virion.arc.MorrowConfig;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test suite for Phase 4 migration of defense modules:
 * AutoTotem, AutoCart, AutoAnchor, and CartRefill into NivoratClient.
 */
public class DefenseModulesMigrationTest {

    @BeforeAll
    static void initRegistry() {
        BuiltinModules.registerAll();
    }

    @BeforeEach
    void resetState() {
        ActivityConfigManager.resetDefaults();
    }

    @Test
    void testDefenseModulesRegisteredAsNivoratModule() {
        List<String> defenseIds = List.of(AutoTotemModule.ID, AutoCartModule.ID, AutoAnchorModule.ID, CartRefillModule.ID);
        for (String id : defenseIds) {
            IModule module = ModuleRegistry.get(id);
            assertNotNull(module, "Module " + id + " must be registered");
            assertEquals(ModuleCategory.DEFENSE, module.getCategory(), "Module " + id + " must be DEFENSE");
            assertInstanceOf(NivoratModule.class, module, "Module " + id + " must extend NivoratModule");
        }
    }

    @Test
    void testDefenseSettingGroupsStrictOrder() {
        List<String> defenseIds = List.of(AutoTotemModule.ID, AutoCartModule.ID, AutoAnchorModule.ID, CartRefillModule.ID);
        for (String id : defenseIds) {
            IModule module = ModuleRegistry.get(id);
            assertNotNull(module);
            List<Setting<?>> settings = module.getSettings();
            assertFalse(settings.isEmpty(), "Module " + id + " must have settings");

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
    void testAutoTotemSettingsAndEngineSync() {
        IModule mod = ModuleRegistry.get(AutoTotemModule.ID);
        assertInstanceOf(AutoTotemModule.class, mod);
        AutoTotemModule totemMod = (AutoTotemModule) mod;
        ActivityConfig config = ActivityConfigManager.getConfig();

        // Default keybind must be unbound
        assertEquals(-1, mod.getKeybind().getKeyCode());

        // Check settings presence
        assertNotNull(mod.getSetting("mode"));
        assertNotNull(mod.getSetting("trigger_hearts"));
        assertNotNull(mod.getSetting("restore_hearts"));
        assertNotNull(mod.getSetting("chance"));
        assertNotNull(mod.getSetting("return_item"));
        assertNotNull(mod.getSetting("return_on_pop"));

        // Mode enum sync
        EnumSetting modeSetting = (EnumSetting) mod.getSetting("mode");
        assertEquals(List.of("main_hand", "offhand", "crystal"), modeSetting.getOptions());

        modeSetting.set("crystal");
        assertEquals("crystal", config.autoTotemMode);
        assertEquals(3, AutoTotemConfig.mode);

        modeSetting.set("offhand");
        assertEquals("offhand", config.autoTotemMode);
        assertEquals(2, AutoTotemConfig.mode);

        modeSetting.set("main_hand");
        assertEquals("main_hand", config.autoTotemMode);
        assertEquals(1, AutoTotemConfig.mode);

        // Trigger hearts sync
        NumberSetting triggerSetting = (NumberSetting) mod.getSetting("trigger_hearts");
        assertEquals(0.5, triggerSetting.getMin(), 0.001);
        assertEquals(10.0, triggerSetting.getMax(), 0.001);
        assertEquals(0.5, triggerSetting.getStep(), 0.001);
        assertFalse(triggerSetting.isIntegerOnly());
        triggerSetting.set(4.5);
        assertEquals(4.5, config.autoTotemTriggerHearts, 0.001);
        assertEquals(4.5, AutoTotemConfig.triggerHearts, 0.001);

        // Restore hearts sync
        NumberSetting restoreSetting = (NumberSetting) mod.getSetting("restore_hearts");
        assertEquals(0.5, restoreSetting.getMin(), 0.001);
        assertEquals(10.0, restoreSetting.getMax(), 0.001);
        assertEquals(0.5, restoreSetting.getStep(), 0.001);
        assertFalse(restoreSetting.isIntegerOnly());
        restoreSetting.set(8.5);
        assertEquals(8.5, config.autoTotemRestoreHearts, 0.001);
        assertEquals(8.5, AutoTotemConfig.restoreHearts, 0.001);

        // Chance sync
        NumberSetting chanceSetting = (NumberSetting) mod.getSetting("chance");
        assertEquals(10.0, chanceSetting.getMin(), 0.001);
        assertEquals(100.0, chanceSetting.getMax(), 0.001);
        chanceSetting.set(90.0);
        assertEquals(90.0, config.autoTotemChance, 0.001);
        assertEquals(90, AutoTotemConfig.chance);

        // Return item sync
        BooleanSetting returnItemSetting = (BooleanSetting) mod.getSetting("return_item");
        returnItemSetting.set(false);
        assertFalse(config.autoTotemReturnItem);
        assertFalse(AutoTotemConfig.returnItem);
        returnItemSetting.set(true);
        assertTrue(config.autoTotemReturnItem);
        assertTrue(AutoTotemConfig.returnItem);

        // Return on pop sync
        BooleanSetting returnOnPopSetting = (BooleanSetting) mod.getSetting("return_on_pop");
        returnOnPopSetting.set(false);
        assertFalse(config.autoTotemReturnOnPop);
        assertFalse(AutoTotemConfig.returnOnPop);
        returnOnPopSetting.set(true);
        assertTrue(config.autoTotemReturnOnPop);
        assertTrue(AutoTotemConfig.returnOnPop);

        // Refill slot sync
        EnumSetting refillSlotSetting = (EnumSetting) mod.getSetting("refill_slot");
        assertNotNull(refillSlotSetting);
        refillSlotSetting.set("3");
        assertEquals("3", config.autoTotemRefillSlot);
        assertEquals(2, AutoTotemConfig.refillSlot);
        refillSlotSetting.set("auto");
        assertEquals("auto", config.autoTotemRefillSlot);
        assertEquals(-1, AutoTotemConfig.refillSlot);

        // Toggle / setEnabled sync
        totemMod.setEnabled(false);
        assertFalse(totemMod.isEnabled());
        assertFalse(totemMod.getController().isEnabled());
        assertFalse(AutoTotemConfig.enabled);

        totemMod.setEnabled(true);
        assertTrue(totemMod.isEnabled());
        assertTrue(totemMod.getController().isEnabled());
        assertTrue(AutoTotemConfig.enabled);
    }

    @Test
    void testAutoTotemFractionalHearts() {
        IModule mod = ModuleRegistry.get(AutoTotemModule.ID);
        assertNotNull(mod);
        ActivityConfig config = ActivityConfigManager.getConfig();

        NumberSetting triggerSetting = (NumberSetting) mod.getSetting("trigger_hearts");
        NumberSetting restoreSetting = (NumberSetting) mod.getSetting("restore_hearts");

        // 0.5 ❤ threshold
        triggerSetting.set(0.5);
        assertEquals(0.5, config.autoTotemTriggerHearts, 0.001);
        assertEquals(0.5, AutoTotemConfig.triggerHearts, 0.001);

        // 1.5 ❤ threshold
        triggerSetting.set(1.5);
        assertEquals(1.5, config.autoTotemTriggerHearts, 0.001);
        assertEquals(1.5, AutoTotemConfig.triggerHearts, 0.001);

        // 2.5 ❤ threshold
        triggerSetting.set(2.5);
        assertEquals(2.5, config.autoTotemTriggerHearts, 0.001);
        assertEquals(2.5, AutoTotemConfig.triggerHearts, 0.001);

        // 5.5 ❤ restore threshold
        restoreSetting.set(5.5);
        assertEquals(5.5, config.autoTotemRestoreHearts, 0.001);
        assertEquals(5.5, AutoTotemConfig.restoreHearts, 0.001);
    }

    @Test
    void testAutoCartSettingsAndEngineSync() {
        IModule mod = ModuleRegistry.get(AutoCartModule.ID);
        assertInstanceOf(AutoCartModule.class, mod);
        AutoCartModule cartMod = (AutoCartModule) mod;
        ActivityConfig config = ActivityConfigManager.getConfig();

        // Default keybind must be Ctrl+Shift+I
        assertEquals(GLFW.GLFW_KEY_I, mod.getKeybind().getKeyCode());
        assertTrue(mod.getKeybind().isCtrl());
        assertTrue(mod.getKeybind().isShift());

        // Check settings presence
        assertNotNull(mod.getSetting("preset"));
        assertNotNull(mod.getSetting("placement_chance"));
        assertNotNull(mod.getSetting("max_distance"));
        assertNotNull(mod.getSetting("min_delay"));
        assertNotNull(mod.getSetting("max_delay"));
        assertNotNull(mod.getSetting("allow_self_cart"));
        assertNotNull(mod.getSetting("allow_pit_placement"));
        assertNotNull(mod.getSetting("random_delay"));
        assertNotNull(mod.getSetting("legit_mode"));
        assertNotNull(mod.getSetting("use_mainhand_cart"));

        // Preset setting sync
        EnumSetting presetSetting = (EnumSetting) mod.getSetting("preset");
        assertEquals(List.of("fast", "medium", "safe"), presetSetting.getOptions());

        presetSetting.set("fast");
        assertEquals("fast", config.autoCartPreset);
        assertEquals(MorrowConfig.PRESET_FAST, MorrowConfig.preset);
        assertFalse(config.autoCartLegitMode);
        assertEquals(40.0, config.autoCartMinDelayMs, 0.001);
        assertEquals(60.0, config.autoCartMaxDelayMs, 0.001);

        presetSetting.set("safe");
        assertEquals("safe", config.autoCartPreset);
        assertEquals(MorrowConfig.PRESET_SAFE, MorrowConfig.preset);
        assertTrue(config.autoCartLegitMode);
        assertEquals(120.0, config.autoCartMinDelayMs, 0.001);
        assertEquals(180.0, config.autoCartMaxDelayMs, 0.001);

        presetSetting.set("medium");
        assertEquals("medium", config.autoCartPreset);
        assertEquals(MorrowConfig.PRESET_MEDIUM, MorrowConfig.preset);
        assertTrue(config.autoCartLegitMode);
        assertEquals(70.0, config.autoCartMinDelayMs, 0.001);
        assertEquals(110.0, config.autoCartMaxDelayMs, 0.001);

        // Distance bounds
        NumberSetting distSetting = (NumberSetting) mod.getSetting("max_distance");
        assertEquals(1.5, distSetting.getMin(), 0.001);
        assertEquals(4.5, distSetting.getMax(), 0.001);
        assertEquals(0.1, distSetting.getStep(), 0.001);
        assertFalse(distSetting.isIntegerOnly());
        distSetting.set(4.0);
        assertEquals(4.0, config.autoCartMaxDistance, 0.001);
        assertEquals(4.0, MorrowConfig.maxDistance, 0.001);

        // Allow self cart
        BooleanSetting selfCartSetting = (BooleanSetting) mod.getSetting("allow_self_cart");
        selfCartSetting.set(true);
        assertTrue(config.autoCartAllowSelfCart);
        assertTrue(MorrowConfig.allowSelfCart);
        selfCartSetting.set(false);
        assertFalse(config.autoCartAllowSelfCart);
        assertFalse(MorrowConfig.allowSelfCart);

        // Allow pit placement
        BooleanSetting pitSetting = (BooleanSetting) mod.getSetting("allow_pit_placement");
        pitSetting.set(false);
        assertFalse(config.autoCartAllowPitPlacement);
        assertFalse(MorrowConfig.allowPitPlacement);
        pitSetting.set(true);
        assertTrue(config.autoCartAllowPitPlacement);
        assertTrue(MorrowConfig.allowPitPlacement);

        // Legit mode
        BooleanSetting legitSetting = (BooleanSetting) mod.getSetting("legit_mode");
        legitSetting.set(false);
        assertFalse(config.autoCartLegitMode);
        assertFalse(MorrowConfig.legitMode);
        legitSetting.set(true);
        assertTrue(config.autoCartLegitMode);
        assertTrue(MorrowConfig.legitMode);

        // Use main-hand cart
        BooleanSetting useMainhandSetting = (BooleanSetting) mod.getSetting("use_mainhand_cart");
        assertNotNull(useMainhandSetting);
        useMainhandSetting.set(false);
        assertFalse(config.autoCartUseMainHand);
        assertFalse(MorrowConfig.useMainhandCart);
        useMainhandSetting.set(true);
        assertTrue(config.autoCartUseMainHand);
        assertTrue(MorrowConfig.useMainhandCart);

        // Controller toggle
        cartMod.setEnabled(false);
        assertFalse(cartMod.isEnabled());
        assertFalse(cartMod.getController().isEnabled());

        cartMod.setEnabled(true);
        assertTrue(cartMod.isEnabled());
        assertTrue(cartMod.getController().isEnabled());
    }

    @Test
    void testAutoAnchorSettingsAndEngineSync() {
        IModule mod = ModuleRegistry.get(AutoAnchorModule.ID);
        assertInstanceOf(AutoAnchorModule.class, mod);
        AutoAnchorModule anchorMod = (AutoAnchorModule) mod;
        ActivityConfig config = ActivityConfigManager.getConfig();

        // Default keybind must be unbound
        assertEquals(-1, mod.getKeybind().getKeyCode());

        // Check settings presence
        assertNotNull(mod.getSetting("preset"));
        assertNotNull(mod.getSetting("auto_explode"));
        assertNotNull(mod.getSetting("auto_return"));
        assertNotNull(mod.getSetting("charge_delay"));
        assertNotNull(mod.getSetting("explode_delay"));
        assertNotNull(mod.getSetting("chance"));
        assertNotNull(mod.getSetting("target_charges"));
        assertNotNull(mod.getSetting("legit_mode"));

        // Preset setting sync
        EnumSetting presetSetting = (EnumSetting) mod.getSetting("preset");
        assertEquals(List.of("fast", "medium", "balanced", "safe"), presetSetting.getOptions());

        presetSetting.set("fast");
        assertEquals("fast", config.autoAnchorPreset);
        assertEquals("FAST", AnchorConfig.preset);
        assertEquals(0.0, config.autoAnchorChargeDelay, 0.001);
        assertEquals(0.0, config.autoAnchorExplodeDelay, 0.001);
        assertEquals(100.0, config.autoAnchorChance, 0.001);

        presetSetting.set("medium");
        assertEquals("medium", config.autoAnchorPreset);
        assertEquals("MEDIUM", AnchorConfig.preset);
        assertEquals(1.0, config.autoAnchorChargeDelay, 0.001);
        assertEquals(1.0, config.autoAnchorExplodeDelay, 0.001);
        assertEquals(95.0, config.autoAnchorChance, 0.001);

        presetSetting.set("safe");
        assertEquals("safe", config.autoAnchorPreset);
        assertEquals("SAFE", AnchorConfig.preset);
        assertEquals(2.0, config.autoAnchorChargeDelay, 0.001);
        assertEquals(2.0, config.autoAnchorExplodeDelay, 0.001);
        assertEquals(85.0, config.autoAnchorChance, 0.001);

        presetSetting.set("balanced");
        assertEquals("balanced", config.autoAnchorPreset);
        assertEquals("BALANCED", AnchorConfig.preset);
        assertEquals(1.0, config.autoAnchorChargeDelay, 0.001);
        assertEquals(1.0, config.autoAnchorExplodeDelay, 0.001);
        assertEquals(90.0, config.autoAnchorChance, 0.001);

        // Auto explode
        BooleanSetting autoExplodeSetting = (BooleanSetting) mod.getSetting("auto_explode");
        autoExplodeSetting.set(true);
        assertTrue(config.autoAnchorAutoExplode);
        assertTrue(AnchorConfig.autoExplode);
        autoExplodeSetting.set(false);
        assertFalse(config.autoAnchorAutoExplode);
        assertFalse(AnchorConfig.autoExplode);

        // Auto return
        BooleanSetting autoReturnSetting = (BooleanSetting) mod.getSetting("auto_return");
        autoReturnSetting.set(false);
        assertFalse(config.autoAnchorAutoReturn);
        assertFalse(AnchorConfig.autoReturn);
        autoReturnSetting.set(true);
        assertTrue(config.autoAnchorAutoReturn);
        assertTrue(AnchorConfig.autoReturn);

        // Target charges
        NumberSetting chargesSetting = (NumberSetting) mod.getSetting("target_charges");
        assertEquals(1.0, chargesSetting.getMin(), 0.001);
        assertEquals(4.0, chargesSetting.getMax(), 0.001);
        assertTrue(chargesSetting.isIntegerOnly());
        chargesSetting.set(3.0);
        assertEquals(3.0, config.autoAnchorTargetCharges, 0.001);
        assertEquals(3, AnchorConfig.targetCharges);

        // Controller toggle
        anchorMod.setEnabled(false);
        assertFalse(anchorMod.isEnabled());
        assertFalse(anchorMod.getController().isEnabled());
        assertFalse(AnchorConfig.enabled);

        anchorMod.setEnabled(true);
        assertTrue(anchorMod.isEnabled());
        assertTrue(anchorMod.getController().isEnabled());
        assertTrue(AnchorConfig.enabled);
    }

    @Test
    void testCartRefillSettingsAndEngineSync() {
        IModule mod = ModuleRegistry.get(CartRefillModule.ID);
        assertInstanceOf(CartRefillModule.class, mod);
        CartRefillModule refillMod = (CartRefillModule) mod;
        ActivityConfig config = ActivityConfigManager.getConfig();

        // Default keybind must be unbound
        assertEquals(-1, mod.getKeybind().getKeyCode());

        // Check settings presence
        assertNotNull(mod.getSetting("delay_ticks"));
        assertNotNull(mod.getSetting("chance"));
        assertNotNull(mod.getSetting("auto_close"));
        assertNotNull(mod.getSetting("random_delay"));
        assertNotNull(mod.getSetting("legit_mode"));

        // Delay ticks
        NumberSetting delaySetting = (NumberSetting) mod.getSetting("delay_ticks");
        assertEquals(0.0, delaySetting.getMin(), 0.001);
        assertEquals(10.0, delaySetting.getMax(), 0.001);
        assertTrue(delaySetting.isIntegerOnly());
        delaySetting.set(4.0);
        assertEquals(4.0, config.cartRefillDelayTicks, 0.001);
        assertEquals(4, RefillConfig.refillDelayTicks);

        // Chance
        NumberSetting chanceSetting = (NumberSetting) mod.getSetting("chance");
        assertEquals(10.0, chanceSetting.getMin(), 0.001);
        assertEquals(100.0, chanceSetting.getMax(), 0.001);
        chanceSetting.set(80.0);
        assertEquals(80.0, config.cartRefillChance, 0.001);
        assertEquals(80, RefillConfig.chance);

        // Auto close
        BooleanSetting autoCloseSetting = (BooleanSetting) mod.getSetting("auto_close");
        autoCloseSetting.set(false);
        assertFalse(config.cartRefillAutoClose);
        assertFalse(RefillConfig.autoClose);
        autoCloseSetting.set(true);
        assertTrue(config.cartRefillAutoClose);
        assertTrue(RefillConfig.autoClose);

        // Random delay
        BooleanSetting randomDelaySetting = (BooleanSetting) mod.getSetting("random_delay");
        randomDelaySetting.set(false);
        assertFalse(config.cartRefillRandomDelay);
        assertFalse(RefillConfig.randomDelay);
        randomDelaySetting.set(true);
        assertTrue(config.cartRefillRandomDelay);
        assertTrue(RefillConfig.randomDelay);

        // Legit mode
        BooleanSetting legitSetting = (BooleanSetting) mod.getSetting("legit_mode");
        legitSetting.set(false);
        assertFalse(config.cartRefillLegitMode);
        assertFalse(RefillConfig.legitMode);
        legitSetting.set(true);
        assertTrue(config.cartRefillLegitMode);
        assertTrue(RefillConfig.legitMode);

        // Controller toggle
        refillMod.setEnabled(false);
        assertFalse(refillMod.isEnabled());
        assertFalse(refillMod.getController().isEnabled());
        assertFalse(RefillConfig.enabled);

        refillMod.setEnabled(true);
        assertTrue(refillMod.isEnabled());
        assertTrue(refillMod.getController().isEnabled());
        assertTrue(RefillConfig.enabled);
    }

    @Test
    void testCartStateServiceListenersAndEvents() {
        AtomicBoolean placedNotified = new AtomicBoolean(false);
        AtomicInteger refilledSlot = new AtomicInteger(-1);

        CartStateService.CartEventListener listener = new CartStateService.CartEventListener() {
            @Override
            public void onCartPlaced(BlockPos pos) {
                placedNotified.set(true);
            }

            @Override
            public void onCartRefilled(int hotbarSlot) {
                refilledSlot.set(hotbarSlot);
            }
        };

        CartStateService.addListener(listener);

        CartStateService.notifyCartPlaced(new BlockPos(10, 64, 10));
        assertTrue(placedNotified.get(), "CartStateService listener must receive onCartPlaced");

        CartStateService.notifyCartRefilled(3);
        assertEquals(3, refilledSlot.get(), "CartStateService listener must receive onCartRefilled with slot 3");

        CartStateService.removeListener(listener);
        placedNotified.set(false);
        CartStateService.notifyCartPlaced(new BlockPos(0, 0, 0));
        assertFalse(placedNotified.get(), "Removed listener must not receive events");

        // Reset
        CartStateService.reset();
    }

    @Test
    void testDefenseLocalization() throws Exception {
        try (InputStream ruStream = getClass().getResourceAsStream("/assets/activity/lang/ru_ru.json")) {
            assertNotNull(ruStream, "ru_ru.json must exist in classpath");
            String ruJson = new String(ruStream.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(ruJson.contains("\"activity.setting.defense.trigger_hearts\": \"Порог срабатывания\""));
            assertTrue(ruJson.contains("\"activity.setting.defense.restore_hearts\": \"Порог возврата\""));
            assertTrue(ruJson.contains("\"activity.setting.defense.cart_preset\": \"Пресет установки\""));
            assertTrue(ruJson.contains("\"activity.setting.defense.anchor_preset\": \"Пресет якоря\""));
            assertTrue(ruJson.contains("\"activity.dropdown.cart_preset.fast\": \"Быстрый\""));
            assertTrue(ruJson.contains("\"activity.dropdown.totem_mode.main_hand\": \"В руке (Удержание)\""));
            assertTrue(ruJson.contains("\"activity.dropdown.anchor_preset.balanced\": \"Сбалансированный\""));
            assertTrue(ruJson.contains("\"activity.setting.defense.auto_close\": \"Авто-закрытие\""));
            assertTrue(ruJson.contains("\"activity.setting.defense.delay_ticks\": \"Задержка пополнения\""));
        }

        try (InputStream enStream = getClass().getResourceAsStream("/assets/activity/lang/en_us.json")) {
            assertNotNull(enStream, "en_us.json must exist in classpath");
            String enJson = new String(enStream.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(enJson.contains("\"activity.setting.defense.trigger_hearts\": \"Trigger Threshold\""));
            assertTrue(enJson.contains("\"activity.setting.defense.restore_hearts\": \"Restore Threshold\""));
            assertTrue(enJson.contains("\"activity.setting.defense.cart_preset\": \"Cart Preset\""));
            assertTrue(enJson.contains("\"activity.setting.defense.anchor_preset\": \"Anchor Preset\""));
            assertTrue(enJson.contains("\"activity.dropdown.cart_preset.fast\": \"Fast\""));
            assertTrue(enJson.contains("\"activity.dropdown.totem_mode.main_hand\": \"Main Hand (Hold)\""));
            assertTrue(enJson.contains("\"activity.dropdown.anchor_preset.balanced\": \"Balanced\""));
            assertTrue(enJson.contains("\"activity.setting.defense.auto_close\": \"Auto Close\""));
            assertTrue(enJson.contains("\"activity.setting.defense.delay_ticks\": \"Refill Delay Ticks\""));
        }
    }
}

package activity.client.module;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.module.api.BuiltinModules;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.impl.combat.ClickPearlModule;
import activity.client.module.setting.Setting;
import activity.client.module.stub.ClickPearlStub;
import dev.pearl.ClickPearlConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClickPearlModuleTest {

    @BeforeAll
    static void initAll() {
        BuiltinModules.registerAll();
    }

    @BeforeEach
    void setUp() {
        ActivityConfigManager.resetDefaults();
    }

    @Test
    @DisplayName("ClickPearl: Module is registered in registry with correct COMBAT category")
    void testModuleRegistrationAndCategory() {
        IModule mod = ModuleRegistry.get(ClickPearlModule.ID);
        assertNotNull(mod, "ClickPearlModule must be registered in ModuleRegistry");
        assertEquals("click_pearl", mod.getId());
        assertEquals(ModuleCategory.COMBAT, mod.getCategory());
        assertNotNull(mod.getName());
        assertFalse(mod.getName().getString().isBlank());
        assertNotNull(mod.getDescription());
        assertFalse(mod.getDescription().getString().isBlank());
    }

    @Test
    @DisplayName("ClickPearl: Settings count and essential settings exist")
    void testSettingsStructure() {
        IModule mod = ModuleRegistry.get(ClickPearlModule.ID);
        assertNotNull(mod);
        List<Setting<?>> settings = mod.getSettings();
        assertTrue(settings.size() >= 10, "ClickPearl must have at least 10 granular settings");

        assertNotNull(mod.getSetting("trigger_keybind"));
        assertNotNull(mod.getSetting("mode"));
        assertNotNull(mod.getSetting("search_mode"));
        assertNotNull(mod.getSetting("switch_back"));
        assertNotNull(mod.getSetting("return_pearl"));
        assertNotNull(mod.getSetting("switch_delay"));
        assertNotNull(mod.getSetting("check_cooldown"));
        assertNotNull(mod.getSetting("prefer_offhand"));
        assertNotNull(mod.getSetting("random_delay"));
        assertNotNull(mod.getSetting("swing_hand"));
        assertNotNull(mod.getSetting("target_slot"));
        assertNotNull(mod.getSetting("combat_guard"));
    }

    @Test
    @DisplayName("ClickPearl: Config synchronization round-trip")
    void testConfigSyncRoundTrip() {
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        assertNotNull(cfg);

        cfg.clickPearlMode = "legit";
        cfg.clickPearlSearchMode = "inventory";
        cfg.clickPearlSwitchBack = false;
        cfg.clickPearlReturnPearl = true;
        cfg.clickPearlSwitchDelayMs = 75.0;
        cfg.clickPearlCheckCooldown = false;
        cfg.clickPearlPreferOffhand = false;
        cfg.clickPearlRandomDelay = false;
        cfg.clickPearlSwingHand = false;
        cfg.clickPearlTargetSlot = "5";
        cfg.clickPearlCombatGuard = false;

        cfg.syncModuleConfigEntries();

        ActivityConfig copy = cfg.copy();
        assertEquals("legit", copy.clickPearlMode);
        assertEquals("inventory", copy.clickPearlSearchMode);
        assertFalse(copy.clickPearlSwitchBack);
        assertTrue(copy.clickPearlReturnPearl);
        assertEquals(75.0, copy.clickPearlSwitchDelayMs);
        assertFalse(copy.clickPearlCheckCooldown);
        assertFalse(copy.clickPearlPreferOffhand);
        assertFalse(copy.clickPearlRandomDelay);
        assertFalse(copy.clickPearlSwingHand);
        assertEquals("5", copy.clickPearlTargetSlot);
        assertFalse(copy.clickPearlCombatGuard);

        assertEquals(cfg, copy);
        assertEquals(cfg.hashCode(), copy.hashCode());
    }

    @Test
    @DisplayName("ClickPearl: ClickPearlConfig mapping from ActivityConfig")
    void testControllerConfigMapping() {
        ActivityConfig cfg = new ActivityConfig();
        cfg.clickPearlMode = "safe";
        cfg.clickPearlSearchMode = "inventory";
        cfg.clickPearlSwitchBack = true;
        cfg.clickPearlReturnPearl = true;
        cfg.clickPearlSwitchDelayMs = 120.0;
        cfg.clickPearlCheckCooldown = false;
        cfg.clickPearlPreferOffhand = true;
        cfg.clickPearlRandomDelay = true;
        cfg.clickPearlSwingHand = true;
        cfg.clickPearlTargetSlot = "7";
        cfg.clickPearlCombatGuard = true;

        ClickPearlModule.syncControllerConfig(cfg, true);

        assertTrue(ClickPearlConfig.enabled);
        assertEquals("safe", ClickPearlConfig.mode);
        assertEquals("inventory", ClickPearlConfig.searchMode);
        assertTrue(ClickPearlConfig.switchBack);
        assertTrue(ClickPearlConfig.returnPearl);
        assertEquals(120.0, ClickPearlConfig.switchDelayMs);
        assertFalse(ClickPearlConfig.checkCooldown);
        assertTrue(ClickPearlConfig.preferOffhand);
        assertTrue(ClickPearlConfig.randomDelay);
        assertTrue(ClickPearlConfig.swingHand);
        assertEquals(7, ClickPearlConfig.targetHotbarSlot);
        assertEquals(6, ClickPearlConfig.getTargetHotbarIndex());
        assertTrue(ClickPearlConfig.combatGuard);
    }

    @Test
    @DisplayName("ClickPearl: Default values sanitize properly within ranges")
    void testSanitizeLimits() {
        ActivityConfig cfg = new ActivityConfig();
        cfg.clickPearlSwitchDelayMs = 999.0;
        cfg.clickPearlMode = "invalid_mode";
        cfg.clickPearlSearchMode = "invalid_search";
        cfg.clickPearlTargetSlot = "invalid_slot";

        cfg.sanitize();

        assertEquals(300.0, cfg.clickPearlSwitchDelayMs);
        assertEquals("fast", cfg.clickPearlMode);
        assertEquals("hotbar", cfg.clickPearlSearchMode);
        assertEquals("9", cfg.clickPearlTargetSlot);

        cfg.clickPearlSwitchDelayMs = -50.0;
        cfg.sanitize();
        assertEquals(0.0, cfg.clickPearlSwitchDelayMs);
    }

    @Test
    @DisplayName("ClickPearl: ClickPearlStub round-trip")
    void testStubSync() {
        ActivityConfig cfg = new ActivityConfig();
        cfg.clickPearlEnabled = false;
        cfg.clickPearlMode = "safe";
        cfg.clickPearlSearchMode = "inventory";
        cfg.clickPearlSwitchBack = false;
        cfg.clickPearlReturnPearl = true;
        cfg.clickPearlSwitchDelayMs = 80.0;
        cfg.clickPearlCheckCooldown = false;
        cfg.clickPearlPreferOffhand = false;
        cfg.clickPearlRandomDelay = false;
        cfg.clickPearlSwingHand = false;
        cfg.clickPearlTargetSlot = "4";
        cfg.clickPearlCombatGuard = false;

        ClickPearlStub stub = new ClickPearlStub();
        stub.loadFromConfig(cfg);

        assertFalse(stub.isEnabled());
        assertEquals("safe", stub.mode);
        assertEquals("inventory", stub.searchMode);
        assertFalse(stub.switchBack);
        assertEquals(80.0, stub.switchDelayMs);
        assertFalse(stub.checkCooldown);
        assertFalse(stub.preferOffhand);
        assertFalse(stub.randomDelay);
        assertFalse(stub.swingHand);
        assertEquals("4", stub.targetSlot);
        assertFalse(stub.combatGuard);

        stub.mode = "fast";
        stub.targetSlot = "2";
        stub.saveToConfig(cfg);

        assertEquals("fast", cfg.clickPearlMode);
        assertEquals("2", cfg.clickPearlTargetSlot);
    }
}

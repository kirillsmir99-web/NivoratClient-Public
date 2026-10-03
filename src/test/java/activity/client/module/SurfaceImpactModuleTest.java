package activity.client.module;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.module.api.BuiltinModules;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.impl.utility.SurfaceImpactModule;
import activity.client.module.setting.Setting;
import dev.impact.SurfaceImpactConfig;
import dev.impact.SurfaceImpactController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurfaceImpactModuleTest {

    @BeforeAll
    static void initAll() {
        BuiltinModules.registerAll();
    }

    @BeforeEach
    void setUp() {
        ActivityConfigManager.resetDefaults();
    }

    @Test
    @DisplayName("WaterDrop: Module is registered in registry with correct UTILITY category")
    void testModuleRegistrationAndCategory() {
        IModule mod = ModuleRegistry.get(SurfaceImpactModule.ID);
        assertNotNull(mod, "SurfaceImpactModule must be registered in ModuleRegistry");
        assertEquals("water_drop", mod.getId());
        assertEquals(ModuleCategory.UTILITY, mod.getCategory());
        assertNotNull(mod.getName());
        assertFalse(mod.getName().getString().isBlank());
        assertNotNull(mod.getDescription());
        assertFalse(mod.getDescription().getString().isBlank());
    }

    @Test
    @DisplayName("WaterDrop: Settings count and essential settings exist")
    void testSettingsStructure() {
        IModule mod = ModuleRegistry.get(SurfaceImpactModule.ID);
        assertNotNull(mod);
        List<Setting<?>> settings = mod.getSettings();
        assertTrue(settings.size() >= 17, "WaterDrop must have at least 17 granular settings");

        assertNotNull(mod.getSetting("mode"));
        assertNotNull(mod.getSetting("fall_threshold"));
        assertNotNull(mod.getSetting("pickup_water"));
        assertNotNull(mod.getSetting("switch_back"));
        assertNotNull(mod.getSetting("camera_mode"));
        assertNotNull(mod.getSetting("pitch_threshold"));
        assertNotNull(mod.getSetting("pickup_delay"));
        assertNotNull(mod.getSetting("switch_delay"));
        assertNotNull(mod.getSetting("random_delay"));
        assertNotNull(mod.getSetting("target_slot"));
        assertNotNull(mod.getSetting("combat_guard"));
        assertNotNull(mod.getSetting("pearl_guard"));
        assertNotNull(mod.getSetting("nether_adapter"));
        assertNotNull(mod.getSetting("enable_water"));
        assertNotNull(mod.getSetting("enable_wind_charge"));
        assertNotNull(mod.getSetting("enable_hay_block"));
        assertNotNull(mod.getSetting("enable_slime_block"));
        assertNotNull(mod.getSetting("enable_cobweb"));
        assertNotNull(mod.getSetting("enable_powder_snow"));
    }

    @Test
    @DisplayName("WaterDrop: Config synchronization round-trip")
    void testConfigSyncRoundTrip() {
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        assertNotNull(cfg);

        cfg.waterDropMode = "inventory";
        cfg.waterDropFallThreshold = 7.5;
        cfg.waterDropPickupWater = false;
        cfg.waterDropCameraMode = "packet";
        cfg.waterDropPickupDelayMs = 120.0;
        cfg.waterDropSwitchDelayMs = 180.0;
        cfg.waterDropTargetSlot = "3";
        cfg.waterDropCombatGuard = false;
        cfg.waterDropPearlGuard = false;
        cfg.waterDropNetherAdapter = false;
        cfg.waterDropEnableWindCharge = false;

        cfg.syncModuleConfigEntries();

        ActivityConfig copy = cfg.copy();
        assertEquals("inventory", copy.waterDropMode);
        assertEquals(7.5, copy.waterDropFallThreshold);
        assertFalse(copy.waterDropPickupWater);
        assertEquals("packet", copy.waterDropCameraMode);
        assertEquals(120.0, copy.waterDropPickupDelayMs);
        assertEquals(180.0, copy.waterDropSwitchDelayMs);
        assertEquals("3", copy.waterDropTargetSlot);
        assertFalse(copy.waterDropCombatGuard);
        assertFalse(copy.waterDropPearlGuard);
        assertFalse(copy.waterDropNetherAdapter);
        assertFalse(copy.waterDropEnableWindCharge);

        assertEquals(cfg, copy);
        assertEquals(cfg.hashCode(), copy.hashCode());
    }

    @Test
    @DisplayName("WaterDrop: SurfaceImpactConfig mapping from ActivityConfig")
    void testControllerConfigMapping() {
        ActivityConfig cfg = new ActivityConfig();
        cfg.waterDropMode = "inventory";
        cfg.waterDropFallThreshold = 6.0;
        cfg.waterDropPickupWater = false;
        cfg.waterDropSwitchBack = false;
        cfg.waterDropCombatGuard = true;
        cfg.waterDropPearlGuard = true;
        cfg.waterDropNetherAdapter = true;
        cfg.waterDropEnableWindCharge = false;

        SurfaceImpactModule.syncControllerConfig(cfg, true);

        assertEquals(1, SurfaceImpactConfig.mode);
        assertEquals(6, SurfaceImpactConfig.fallThreshold);
        assertFalse(SurfaceImpactConfig.pickupWater);
        assertFalse(SurfaceImpactConfig.switchBack);
        assertTrue(SurfaceImpactConfig.combatGuard);
        assertTrue(SurfaceImpactConfig.pearlGuard);
        assertTrue(SurfaceImpactConfig.netherAdapter);
        assertFalse(SurfaceImpactConfig.enableWindCharge);
    }

    @Test
    @DisplayName("WaterDrop: Default values sanitize properly within ranges")
    void testSanitizeLimits() {
        ActivityConfig cfg = new ActivityConfig();
        cfg.waterDropFallThreshold = 999.0;
        cfg.waterDropPickupDelayMs = -50.0;
        cfg.waterDropPitchThreshold = 10.0;
        cfg.waterDropTargetSlot = "invalid";

        cfg.sanitize();

        assertEquals(20.0, cfg.waterDropFallThreshold);
        assertEquals(30.0, cfg.waterDropPickupDelayMs);
        assertEquals(30.0, cfg.waterDropPitchThreshold);
        assertEquals("9", cfg.waterDropTargetSlot);
    }

    @Test
    @DisplayName("WaterDrop: Controller idle state and inventory screen ownership")
    void testInventoryScreenOwnership() {
        SurfaceImpactModule mod = (SurfaceImpactModule) ModuleRegistry.get(SurfaceImpactModule.ID);
        assertNotNull(mod);
        SurfaceImpactController ctrl = mod.getController();
        assertNotNull(ctrl);
        assertFalse(ctrl.isBusy());
        assertFalse(ctrl.ownsInventoryScreen(null));
        assertFalse(mod.canTickWhileScreenOpen(null));
    }
}

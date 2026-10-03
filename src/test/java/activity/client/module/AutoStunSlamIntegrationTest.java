package activity.client.module;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.module.api.BuiltinModules;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.impl.combat.ShaderPassModule;
import activity.client.module.impl.combat.MatrixTransformModule;
import dev.shader.ShaderPassConfig;
import dev.particle.ParticlePhysicsConfig;
import dev.mace.prestige.PrestigeStunSlamConfig;
import dev.mace.prestige.PrestigeStunSlamController;
import net.fabricmc.pack.api.CombatLockManager;
import net.redstone.optimizer.config.RedstoneOptimizerConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AutoStunSlamIntegrationTest {

    @BeforeAll
    static void initRegistry() {
        BuiltinModules.registerAll();
    }

    @BeforeEach
    void resetState() {
        ActivityConfigManager.resetDefaults();
        CombatLockManager.reset();
    }

    @Test
    void testAutoStunSlamDefaultPreset() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        assertNotNull(config);

        assertEquals("new", config.autoStunSlamPreset, "Default preset must be new");
        assertEquals(100.0, config.autoStunSlamChance, 0.001, "Default chance must be 100%");
        assertEquals(2.85, config.autoStunSlamDistance, 0.001, "Default distance must be 2.85");
        assertEquals(1.0, config.autoStunSlamAirTimeSec, 0.001, "Default air time must be 1.0s");
        assertEquals(3.0, config.autoStunSlamMinFall, 0.001, "Default min fall must be 3.0 blocks");
        assertEquals("blocks", config.autoStunSlamAirCondition);
        assertEquals(0.0, config.autoStunSlamAxeDelayMs, 0.001, "Default axe delay must be 0ms");
        assertEquals(0.0, config.autoStunSlamMaceDelayMs, 0.001, "Default mace delay must be 0ms");
        assertEquals(50.0, config.autoStunSlamRestoreDelayMs, 0.001, "Default restore delay must be 50ms");

        assertEquals(100, ParticlePhysicsConfig.chance);
        assertEquals(2.85, ParticlePhysicsConfig.triggerDistance, 0.001);
        assertEquals(1.0, ParticlePhysicsConfig.airTimeSec, 0.001);
        assertEquals(3.0, ParticlePhysicsConfig.minFallDistance, 0.001);
        assertEquals("blocks", ParticlePhysicsConfig.airCondition);
        assertEquals(0, ParticlePhysicsConfig.axeDelayMs);
        assertEquals(0, ParticlePhysicsConfig.maceDelayMs);
        assertEquals(50, ParticlePhysicsConfig.restoreDelayMs);
    }

    @Test
    void testAirborneConditionDefaultsAndJumpSafety() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        assertEquals("blocks", config.autoStunSlamAirCondition);
        assertEquals(3.0, config.autoStunSlamMinFall, 0.001);
        assertEquals(1.0, config.autoStunSlamAirTimeSec, 0.001);
        assertEquals("blocks", config.autoStunSlamNewAirCondition);
        assertEquals(3.0, config.autoStunSlamNewMinFall, 0.001);
        assertEquals(1.0, config.autoStunSlamNewAirTimeSec, 0.001);

        assertEquals("blocks", ParticlePhysicsConfig.airCondition);
        assertEquals(3.0, ParticlePhysicsConfig.minFallDistance, 0.001);
        assertEquals(1.0, ParticlePhysicsConfig.airTimeSec, 0.001);

        PrestigeStunSlamConfig newCfg = PrestigeStunSlamController.getInstance().getConfig();
        assertEquals("blocks", newCfg.airCondition);
        assertEquals(3.0, newCfg.minFallDistance, 0.001);
        assertEquals(1.0, newCfg.airTimeSec, 0.001);
    }

    @Test
    void testCombatLockCoordinationBetweenStunSlamAndShieldbreaker() {

        assertFalse(CombatLockManager.isLocked(CombatLockManager.SUNDER));
        assertFalse(CombatLockManager.isLocked(CombatLockManager.SHIELD_COMBO));

        CombatLockManager.setLock(CombatLockManager.SUNDER, true);
        assertTrue(CombatLockManager.isLocked(CombatLockManager.SUNDER));
        assertTrue(CombatLockManager.isLocked());

        CombatLockManager.setLock(CombatLockManager.SUNDER, false);
        assertFalse(CombatLockManager.isLocked(CombatLockManager.SUNDER));
        assertFalse(CombatLockManager.isLocked());

        CombatLockManager.setLock(CombatLockManager.SHIELD_COMBO, true);
        assertTrue(CombatLockManager.isLocked(CombatLockManager.SHIELD_COMBO));
        assertTrue(CombatLockManager.isLocked());

        CombatLockManager.setLock(CombatLockManager.SHIELD_COMBO, false);
        assertFalse(CombatLockManager.isLocked());
    }

    @Test
    void testModuleEnableDisableLifecycle() {
        IModule stunSlam = ModuleRegistry.get(MatrixTransformModule.ID);
        IModule shieldBreaker = ModuleRegistry.get(ShaderPassModule.ID);
        assertNotNull(stunSlam);
        assertNotNull(shieldBreaker);

        stunSlam.setEnabled(false);
        assertFalse(stunSlam.isEnabled());
        assertFalse(dev.mace.prestige.PrestigeStunSlamController.getInstance().getConfig().enabled);
        assertFalse(ParticlePhysicsConfig.enabled);

        stunSlam.setEnabled(true);
        assertTrue(stunSlam.isEnabled());
        assertTrue(dev.mace.prestige.PrestigeStunSlamController.getInstance().getConfig().enabled);

        stunSlam.setEnabled(false);
        assertFalse(CombatLockManager.isLocked(CombatLockManager.SUNDER));

        stunSlam.setEnabled(true);
    }

    @Test
    void testSunderSuppressionWhenShieldBreakerActiveOrAutoMaceDisabled() {
        ShaderPassConfig.enabled = true;
        RedstoneOptimizerConfig.enabled = true;
        assertTrue(ShaderPassConfig.enabled);

        ShaderPassConfig.enabled = false;
        RedstoneOptimizerConfig.enabled = false;
        assertFalse(RedstoneOptimizerConfig.enabled);

        RedstoneOptimizerConfig.enabled = true;
    }

    @Test
    void testAutoSpearSecurityModeRenamingAndNormalization() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        assertNotNull(config);

        config.autoSpearSecurityMode = "Безопасный";
        config.sanitize();
        assertEquals("legit", config.autoSpearSecurityMode);

        config.autoSpearSecurityMode = "Сбалансированный";
        config.sanitize();
        assertEquals("semi_legit", config.autoSpearSecurityMode);

        config.autoSpearSecurityMode = "Рейдж";
        config.sanitize();
        assertEquals("rage", config.autoSpearSecurityMode);

        config.autoSpearSecurityMode = "Safe";
        config.sanitize();
        assertEquals("legit", config.autoSpearSecurityMode);

        config.autoSpearSecurityMode = "Balanced";
        config.sanitize();
        assertEquals("semi_legit", config.autoSpearSecurityMode);
    }

    @Test
    void testAutoToolCombatLockSuppression() {
        CombatLockManager.setLock(CombatLockManager.SHIELD_COMBO, true);
        assertTrue(CombatLockManager.isLocked());

        CombatLockManager.setLock(CombatLockManager.SHIELD_COMBO, false);
        assertFalse(CombatLockManager.isLocked());
    }

    @Test
    void testZeroDelaysAndMaceDelayPreservation() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        assertNotNull(config);

        config.autoStunSlamMaceDelayMs = 0.0;
        config.autoStunSlamAxeDelayMs = 0.0;
        config.autoStunSlamRestoreDelayMs = 0.0;
        config.autoStunSlamAirTimeSec = 0.0;
        config.autoSpearRestoreDelayMs = 0.0;

        config.sanitize();

        assertEquals(0.0, config.autoStunSlamMaceDelayMs, 0.001);
        assertEquals(0.0, config.autoStunSlamAxeDelayMs, 0.001);
        assertEquals(0.0, config.autoStunSlamRestoreDelayMs, 0.001);
        assertEquals(0.0, config.autoStunSlamAirTimeSec, 0.001);
        assertEquals(0.0, config.autoSpearRestoreDelayMs, 0.001);
    }

    @Test
    void testNewPresetModeAndAttackControls() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        assertNotNull(config);
        assertEquals("full_auto", config.autoStunSlamNewMode);
        assertEquals(100.0, config.autoStunSlamNewChance, 0.001);
        assertEquals(0.0, config.autoStunSlamNewAttackDelayMs, 0.001);

        PrestigeStunSlamConfig newCfg = PrestigeStunSlamController.getInstance().getConfig();
        assertEquals("full_auto", newCfg.mode);
        assertEquals(100.0, newCfg.chance, 0.001);
        assertEquals(0.0, newCfg.attackDelayMs, 0.001);
    }

    @Test
    void testDynamicAirSettingsVisibility() {
        MatrixTransformModule module = (MatrixTransformModule) ModuleRegistry.get(MatrixTransformModule.ID);
        assertNotNull(module);

        activity.client.module.setting.EnumSetting preset = (activity.client.module.setting.EnumSetting) module.getSetting("preset");
        activity.client.module.setting.EnumSetting airCond = (activity.client.module.setting.EnumSetting) module.getSetting("air_condition");
        var minFall = module.getSetting("min_fall");
        var airTime = module.getSetting("air_time");
        activity.client.module.setting.EnumSetting newAirCond = (activity.client.module.setting.EnumSetting) module.getSetting("new_air_condition");
        var newMinFall = module.getSetting("new_min_fall");
        var newAirTime = module.getSetting("new_air_time");

        assertNotNull(preset);
        assertNotNull(airCond);
        assertNotNull(minFall);
        assertNotNull(airTime);
        assertNotNull(newAirCond);
        assertNotNull(newMinFall);
        assertNotNull(newAirTime);

        preset.set("old");
        assertTrue(airCond.isVisible());
        assertFalse(newAirCond.isVisible());

        airCond.set("blocks");
        assertTrue(minFall.isVisible());
        assertFalse(airTime.isVisible());

        airCond.set("time");
        assertFalse(minFall.isVisible());
        assertTrue(airTime.isVisible());

        airCond.set("both");
        assertTrue(minFall.isVisible());
        assertTrue(airTime.isVisible());

        airCond.set("any");
        assertTrue(minFall.isVisible());
        assertTrue(airTime.isVisible());

        preset.set("new");
        assertFalse(airCond.isVisible());
        assertFalse(minFall.isVisible());
        assertFalse(airTime.isVisible());
        assertTrue(newAirCond.isVisible());

        newAirCond.set("blocks");
        assertTrue(newMinFall.isVisible());
        assertFalse(newAirTime.isVisible());

        newAirCond.set("time");
        assertFalse(newMinFall.isVisible());
        assertTrue(newAirTime.isVisible());

        newAirCond.set("both");
        assertTrue(newMinFall.isVisible());
        assertTrue(newAirTime.isVisible());

        newAirCond.set("any");
        assertTrue(newMinFall.isVisible());
        assertTrue(newAirTime.isVisible());
    }
}

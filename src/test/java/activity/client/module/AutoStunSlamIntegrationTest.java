package activity.client.module;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.module.api.BuiltinModules;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.impl.combat.AutoShieldbreakerModule;
import activity.client.module.impl.combat.AutoStunSlamModule;
import dev.nivora.ShieldBreakerConfig;
import dev.sunder.SunderConfig;
import net.fabricmc.pack.api.CombatLockManager;
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

        assertEquals(100.0, config.autoStunSlamChance, 0.001, "Default chance must be 100%");
        assertEquals(2.85, config.autoStunSlamDistance, 0.001, "Default distance must be 2.85");
        assertEquals(0.1, config.autoStunSlamAirTimeSec, 0.001, "Default air time must be 0.1s");
        assertEquals(0.0, config.autoStunSlamAxeDelayMs, 0.001, "Default axe delay must be 0ms");
        assertEquals(0.0, config.autoStunSlamMaceDelayMs, 0.001, "Default mace delay must be 0ms");
        assertEquals(50.0, config.autoStunSlamRestoreDelayMs, 0.001, "Default restore delay must be 50ms");

        assertEquals(100, SunderConfig.chance);
        assertEquals(2.85, SunderConfig.triggerDistance, 0.001);
        assertEquals(0.1, SunderConfig.airTimeSec, 0.001);
        assertEquals(0, SunderConfig.axeDelayMs);
        assertEquals(0, SunderConfig.maceDelayMs);
        assertEquals(50, SunderConfig.restoreDelayMs);
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
        IModule stunSlam = ModuleRegistry.get(AutoStunSlamModule.ID);
        IModule shieldBreaker = ModuleRegistry.get(AutoShieldbreakerModule.ID);
        assertNotNull(stunSlam);
        assertNotNull(shieldBreaker);

        stunSlam.setEnabled(false);
        assertFalse(stunSlam.isEnabled());
        assertFalse(SunderConfig.enabled);

        stunSlam.setEnabled(true);
        assertTrue(stunSlam.isEnabled());
        assertTrue(SunderConfig.enabled);

        stunSlam.setEnabled(false);
        assertFalse(CombatLockManager.isLocked(CombatLockManager.SUNDER));

        stunSlam.setEnabled(true);
    }
}

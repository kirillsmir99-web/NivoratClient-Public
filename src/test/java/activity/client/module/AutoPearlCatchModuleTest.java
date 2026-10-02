package activity.client.module;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.module.api.BuiltinModules;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.impl.combat.RaycastPredictorModule;
import activity.client.module.keybind.Keybind;
import activity.client.module.setting.BooleanSetting;
import activity.client.module.setting.EnumSetting;
import activity.client.module.setting.KeybindSetting;
import activity.client.module.setting.NumberSetting;
import activity.client.module.setting.Setting;
import activity.client.module.setting.SettingGroup;
import dev.raycast.RaycastInterpolator;
import dev.raycast.RaycastPredictorController;
import dev.raycast.RaycastTrajectory;
import net.fabricmc.pack.api.CombatLockManager;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AutoPearlCatchModuleTest {

    @BeforeAll
    static void initAll() {
        BuiltinModules.registerAll();
    }

    @BeforeEach
    void setUp() {
        ActivityConfigManager.resetDefaults();
        CombatLockManager.reset();
    }

    @Test
    @DisplayName("AutoPearlCatch is registered with correct metadata and COMBAT category")
    void testModuleRegistrationAndMetadata() {
        IModule module = ModuleRegistry.get(RaycastPredictorModule.ID);
        assertNotNull(module, "AutoPearlCatch must be registered in ModuleRegistry");
        assertEquals(RaycastPredictorModule.ID, module.getId());
        assertEquals(ModuleCategory.COMBAT, module.getCategory());
        assertNotNull(module.getName());
        assertNotNull(module.getDescription());

        IModule alias1 = ModuleRegistry.get("pearl_catch");
        assertNotNull(alias1);
        assertEquals(RaycastPredictorModule.ID, alias1.getId());

        IModule alias2 = ModuleRegistry.get("pearlcatch");
        assertNotNull(alias2);
        assertEquals(RaycastPredictorModule.ID, alias2.getId());
    }

    @Test
    @DisplayName("AutoPearlCatch settings respect 5-tier SettingGroup ordering")
    void testSettingGroupsOrdering() {
        IModule module = ModuleRegistry.get(RaycastPredictorModule.ID);
        assertNotNull(module);
        List<Setting<?>> settings = module.getSettings();
        assertFalse(settings.isEmpty(), "AutoPearlCatch must have settings registered");

        int lastOrdinal = -1;
        for (Setting<?> s : settings) {
            SettingGroup group = s.getGroup();
            assertNotNull(group, "Setting " + s.getId() + " must have a valid group");
            assertTrue(group.ordinal() >= lastOrdinal,
                    "Setting " + s.getId() + " in group " + group + " violates group ordering");
            lastOrdinal = group.ordinal();
        }
    }

    @Test
    @DisplayName("AutoPearlCatch settings register and match expected defaults")
    void testSettingsRegistrationAndDefaults() {
        IModule module = ModuleRegistry.get(RaycastPredictorModule.ID);
        assertNotNull(module);

        EnumSetting modeSetting = (EnumSetting) module.getSetting("mode");
        assertNotNull(modeSetting);
        assertEquals("semi_auto", modeSetting.get());

        assertNull(module.getSetting("direction"), "Direction setting must be hidden");

        KeybindSetting throwKb = (KeybindSetting) module.getSetting("throw_keybind");
        assertNotNull(throwKb);
        assertEquals(GLFW.GLFW_KEY_V, throwKb.get().getKeyCode());
        assertTrue(throwKb.isVisible(), "throw_keybind must be visible in semi_auto mode");

        KeybindSetting actionKb = (KeybindSetting) module.getSetting("action_keybind");
        assertNotNull(actionKb);
        assertEquals(GLFW.GLFW_KEY_V, actionKb.get().getKeyCode());
        assertFalse(actionKb.isVisible(), "action_keybind must be hidden in semi_auto mode");

        KeybindSetting horKb = (KeybindSetting) module.getSetting("horizontal_keybind");
        assertNotNull(horKb);
        assertEquals(GLFW.GLFW_KEY_C, horKb.get().getKeyCode());
        assertFalse(horKb.isVisible(), "horizontal_keybind must be hidden in semi_auto mode");

        modeSetting.set("full_auto");
        assertFalse(throwKb.isVisible(), "throw_keybind must be hidden in full_auto mode");
        assertTrue(actionKb.isVisible(), "action_keybind must be visible in full_auto mode");
        assertTrue(horKb.isVisible(), "horizontal_keybind must be visible in full_auto mode");
        modeSetting.set("semi_auto");

        NumberSetting throwDelay = (NumberSetting) module.getSetting("throw_delay");
        assertNotNull(throwDelay);
        assertEquals(2.0, throwDelay.get().doubleValue(), 0.001);

        NumberSetting rotationTime = (NumberSetting) module.getSetting("rotation_time_ms");
        assertNotNull(rotationTime);
        assertEquals(135.0, rotationTime.get().doubleValue(), 0.001);

        NumberSetting horizontalOffset = (NumberSetting) module.getSetting("horizontal_offset");
        assertNotNull(horizontalOffset);
        assertEquals(8.0, horizontalOffset.get().doubleValue(), 0.001);

        BooleanSetting restoreSlot = (BooleanSetting) module.getSetting("restore_slot");
        assertNotNull(restoreSlot);
        assertTrue(restoreSlot.get());

        BooleanSetting restoreCamera = (BooleanSetting) module.getSetting("restore_camera");
        assertNotNull(restoreCamera);
        assertFalse(restoreCamera.get());

        BooleanSetting legitMode = (BooleanSetting) module.getSetting("legit_mode");
        assertNotNull(legitMode);
        assertTrue(legitMode.get());

        List<Setting<?>> settings = module.getSettings();
        List<String> settingIds = settings.stream().map(Setting::getId).toList();
        int idxThrowDelay = settingIds.indexOf("throw_delay");
        int idxRotationTime = settingIds.indexOf("rotation_time_ms");
        int idxHorizontalOffset = settingIds.indexOf("horizontal_offset");
        int idxRestoreSlot = settingIds.indexOf("restore_slot");
        int idxRestoreCamera = settingIds.indexOf("restore_camera");
        int idxLegitMode = settingIds.indexOf("legit_mode");

        assertTrue(idxThrowDelay < idxRotationTime);
        assertTrue(idxRotationTime < idxHorizontalOffset);
        assertTrue(idxHorizontalOffset < idxRestoreSlot);
        assertTrue(idxRestoreSlot < idxRestoreCamera);
        assertTrue(idxRestoreCamera < idxLegitMode);
        assertEquals(settingIds.size() - 1, idxLegitMode, "legit_mode must be strictly the last setting");
    }

    @Test
    @DisplayName("Config persistence roundtrip preserves AutoPearlCatch values")
    void testConfigPersistenceRoundtrip() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        assertNotNull(config);

        config.autoPearlCatchEnabled = false;
        config.autoPearlCatchMode = "full_auto";
        config.autoPearlCatchDirection = "horizontal";
        config.autoPearlCatchThrowDelay = 4.0;
        config.autoPearlCatchRestoreSlot = false;
        config.autoPearlCatchRestoreCamera = true;
        config.autoPearlCatchRotationTimeMs = 200.0;
        config.autoPearlCatchLegitMode = false;
        config.autoPearlCatchHorizontalOffset = 12.5;
        config.autoPearlCatchActionKeybind = new Keybind(GLFW.GLFW_KEY_X, true, false, false);
        config.autoPearlCatchHorizontalKeybind = new Keybind(GLFW.GLFW_KEY_Z, false, true, false);
        config.autoPearlCatchThrowKeybind = new Keybind(GLFW.GLFW_KEY_B, true, false, false);

        config.syncModuleConfigEntries();

        ActivityConfig copy = config.copy();
        assertEquals(config, copy);

        assertEquals(false, copy.autoPearlCatchEnabled);
        assertEquals("full_auto", copy.autoPearlCatchMode);
        assertEquals("horizontal", copy.autoPearlCatchDirection);
        assertEquals(4.0, copy.autoPearlCatchThrowDelay, 0.001);
        assertEquals(false, copy.autoPearlCatchRestoreSlot);
        assertEquals(true, copy.autoPearlCatchRestoreCamera);
        assertEquals(200.0, copy.autoPearlCatchRotationTimeMs, 0.001);
        assertEquals(false, copy.autoPearlCatchLegitMode);
        assertEquals(12.5, copy.autoPearlCatchHorizontalOffset, 0.001);
        assertEquals(GLFW.GLFW_KEY_X, copy.autoPearlCatchActionKeybind.getKeyCode());
        assertTrue(copy.autoPearlCatchActionKeybind.isCtrl());
        assertEquals(GLFW.GLFW_KEY_Z, copy.autoPearlCatchHorizontalKeybind.getKeyCode());
        assertTrue(copy.autoPearlCatchHorizontalKeybind.isShift());
        assertEquals(GLFW.GLFW_KEY_B, copy.autoPearlCatchThrowKeybind.getKeyCode());
        assertTrue(copy.autoPearlCatchThrowKeybind.isCtrl());
    }

    @Test
    @DisplayName("RaycastTrajectory kinematic 3D solver finds valid intercept solutions")
    void testKinematic3DSolver() {

        RaycastTrajectory.Solution solStill = RaycastTrajectory.solve3D(2, 0.0f, Vec3d.ZERO, true, -1.0f);
        assertNotNull(solStill);
        assertTrue(solStill.valid(), "3D solver must find valid solution when stationary");
        assertTrue(solStill.residualError() <= 0.5, "Stationary residual error must be <= 0.5: " + solStill.residualError());
        assertTrue(solStill.pearlPitch() <= -15.0f && solStill.pearlPitch() >= -35.0f, "Pearl pitch must be in realistic launch window");
        assertTrue(solStill.windPitch() > solStill.pearlPitch(), "Wind pitch must be higher than pearl pitch to hit from behind");
        assertTrue(solStill.interceptTick() >= 3 && solStill.interceptTick() <= 20, "Intercept tick must be within reasonable range");

        Vec3d sprintVel = new Vec3d(0.0, 0.0, 0.28);
        RaycastTrajectory.Solution solSprint = RaycastTrajectory.solve3D(2, 0.0f, sprintVel, true, -1.0f);
        assertNotNull(solSprint);
        assertTrue(solSprint.valid(), "Sprint solution must be valid");
        assertTrue(solSprint.residualError() <= 0.5, "Sprint residual error must be <= 0.5: " + solSprint.residualError());

        Vec3d fallVel = new Vec3d(0.0, -0.4, 0.1);
        RaycastTrajectory.Solution solFall = RaycastTrajectory.solve3D(2, 0.0f, fallVel, false, -1.0f);
        assertNotNull(solFall);
        assertTrue(solFall.valid(), "Fall solution must be valid");
        assertTrue(solFall.residualError() <= 0.5, "Fall residual error must be <= 0.5: " + solFall.residualError());

        Vec3d windJumpVel = new Vec3d(0.0, 0.9, 0.0);
        RaycastTrajectory.Solution solWindJump = RaycastTrajectory.solve3D(2, 0.0f, windJumpVel, false, -1.0f);
        assertNotNull(solWindJump);
        assertTrue(solWindJump.valid(), "Wind jump solution must be valid");
        assertTrue(solWindJump.residualError() <= 0.5, "Wind jump residual error must be <= 0.5: " + solWindJump.residualError());

        Vec3d extremeWindJumpVel = new Vec3d(0.0, 1.45, 0.15);
        RaycastTrajectory.Solution solExtreme = RaycastTrajectory.solve3D(2, 0.0f, extremeWindJumpVel, false, -1.0f);
        assertNotNull(solExtreme);
        assertTrue(solExtreme.valid(), "Extreme wind jump solution must be valid");
        assertTrue(solExtreme.residualError() <= 0.5, "Extreme wind jump residual error must be <= 0.5: " + solExtreme.residualError());

        Vec3d sprintVelX = new Vec3d(0.28, 0.0, 0.0);
        RaycastTrajectory.Solution solSprintX = RaycastTrajectory.solve3D(2, -90.0f, sprintVelX, true, -1.0f);
        assertNotNull(solSprintX);
        assertTrue(solSprintX.valid());
        assertTrue(solSprintX.residualError() <= 0.5);

        Vec3d dir = RaycastTrajectory.getDirectionVector(-25.0f, 45.0f);
        assertEquals(1.0, dir.length(), 0.001);
    }

    @Test
    @DisplayName("RaycastTrajectory legacy helpers remain backward-compatible")
    void testPearlCatchTrajectoryBallistics() {
        float normalPitch = RaycastTrajectory.calculateOptimalPearlPitch(2, null, false);
        assertEquals(-28.0f, normalPitch, 0.001f);

        float sprintPitch = RaycastTrajectory.calculateOptimalPearlPitch(2, null, true);
        assertEquals(-27.0f, sprintPitch, 0.001f);

        float delay1Pitch = RaycastTrajectory.calculateOptimalPearlPitch(1, null, false);
        assertEquals(-26.5f, delay1Pitch, 0.001f);

        float offsetDelay2 = RaycastTrajectory.calculateWindChargePitchOffset(2);
        assertEquals(8.0f, offsetDelay2, 0.001f);

        float offsetDelay1 = RaycastTrajectory.calculateWindChargePitchOffset(1);
        assertEquals(5.5f, offsetDelay1, 0.001f);
    }

    @Test
    @DisplayName("RaycastInterpolator lifecycle, GCD calculation, and state transitions")
    void testCameraInterpolatorMath() {
        RaycastInterpolator interpolator = new RaycastInterpolator();
        assertFalse(interpolator.isActive());

        interpolator.start(0.0f, -45.0f, 0.0f, 90.0f, 150L, true);
        assertTrue(interpolator.isActive());

        double gcd = RaycastInterpolator.calculateMouseGcd(null);
        assertTrue(gcd > 0.0001, "Mouse GCD must be positive and non-zero");

        interpolator.reset();
        assertFalse(interpolator.isActive());
    }

    @Test
    @DisplayName("Safety raycasts and latency helpers are robust against null client")
    void testSafetyRaycastsNullSafety() {
        assertFalse(RaycastPredictorController.isCeilingBlocked(null, 10.0));
        assertFalse(RaycastPredictorController.isForwardBlocked(null, 0.0f, 0.0f, 2.0));
        assertEquals(0, RaycastPredictorController.getPlayerLatency(null));
    }

    @Test
    @DisplayName("CombatLockManager handles PEARL_CATCH lock correctly")
    void testCombatLockManagerIntegration() {
        assertFalse(CombatLockManager.isLocked());

        CombatLockManager.setLock(CombatLockManager.PEARL_CATCH, true);
        assertTrue(CombatLockManager.isLocked());

        CombatLockManager.setLock(CombatLockManager.PEARL_CATCH, false);
        assertFalse(CombatLockManager.isLocked());
    }

    @Test
    @DisplayName("RaycastTrajectory converges at throwDelay = 5 across all movement profiles")
    void testThrowDelayFiveKinematicConvergence() {

        RaycastTrajectory.Solution solStill = RaycastTrajectory.solve3D(5, 0.0f, Vec3d.ZERO, true, -1.0f);
        assertNotNull(solStill);
        assertTrue(solStill.valid(), "Delay=5 still solution must be valid");
        assertTrue(solStill.residualError() <= 0.5, "Delay=5 still residual error <= 0.5: " + solStill.residualError());
        assertEquals(solStill.computedOffset(), solStill.pitchOffset(), 0.0001f, "computedOffset must equal pitchOffset");

        Vec3d sprintVel = new Vec3d(0.0, 0.0, 0.28);
        RaycastTrajectory.Solution solSprint = RaycastTrajectory.solve3D(5, 0.0f, sprintVel, true, -1.0f);
        assertNotNull(solSprint);
        assertTrue(solSprint.valid(), "Delay=5 sprint solution must be valid");
        assertTrue(solSprint.residualError() <= 0.5, "Delay=5 sprint residual error <= 0.5: " + solSprint.residualError());

        Vec3d fallVel = new Vec3d(0.0, -0.4, 0.1);
        RaycastTrajectory.Solution solFall = RaycastTrajectory.solve3D(5, 0.0f, fallVel, false, -1.0f);
        assertNotNull(solFall);
        assertTrue(solFall.valid(), "Delay=5 fall solution must be valid");
        assertTrue(solFall.residualError() <= 0.5, "Delay=5 fall residual error <= 0.5: " + solFall.residualError());

        Vec3d windJumpVel = new Vec3d(0.0, 0.9, 0.0);
        RaycastTrajectory.Solution solWindJump = RaycastTrajectory.solve3D(5, 0.0f, windJumpVel, false, -1.0f);
        assertNotNull(solWindJump);
        assertTrue(solWindJump.valid(), "Delay=5 wind jump solution must be valid");
        assertTrue(solWindJump.residualError() <= 0.5, "Delay=5 wind jump residual error <= 0.5: " + solWindJump.residualError());

        Vec3d extremeWindJumpVel = new Vec3d(0.0, 1.45, 0.15);
        RaycastTrajectory.Solution solExtreme = RaycastTrajectory.solve3D(5, 0.0f, extremeWindJumpVel, false, -1.0f);
        assertNotNull(solExtreme);
        assertTrue(solExtreme.valid(), "Delay=5 extreme wind jump solution must be valid");
        assertTrue(solExtreme.residualError() <= 0.5, "Delay=5 extreme wind jump residual error <= 0.5: " + solExtreme.residualError());

        Vec3d sprintVelX = new Vec3d(0.28, 0.0, 0.0);
        RaycastTrajectory.Solution solSprintX = RaycastTrajectory.solve3D(5, -90.0f, sprintVelX, true, -1.0f);
        assertNotNull(solSprintX);
        assertTrue(solSprintX.valid(), "Delay=5 lateral sprint solution must be valid");
        assertTrue(solSprintX.residualError() <= 0.5, "Delay=5 lateral sprint residual error <= 0.5: " + solSprintX.residualError());
    }

    @Test
    @DisplayName("Camera angle degree wrapping and GCD quantization at boundary values")
    void testCameraYawWrappingAndGcdBoundaryCases() {

        float lastAppliedYaw = 180.5f;
        float targetWindYaw = -179.0f;
        float deltaYaw = net.minecraft.util.math.MathHelper.wrapDegrees(targetWindYaw - lastAppliedYaw);
        assertEquals(0.5f, deltaYaw, 0.001f, "Yaw delta across 180° boundary must wrap smoothly to +0.5°");
        assertTrue(Math.abs(deltaYaw) <= 180.0f, "Delta yaw must never exceed 180 degrees in a single frame");

        float playerCumulativeYaw = 365.0f;
        float targetNormalizedYaw = 5.0f;
        float cumDelta = net.minecraft.util.math.MathHelper.wrapDegrees(targetNormalizedYaw - playerCumulativeYaw);
        assertEquals(0.0f, cumDelta, 0.001f, "Cumulative yaw must wrap to 0 delta rather than -360° snap");

        float extremeYaw = 725.0f;
        float extremeDelta = net.minecraft.util.math.MathHelper.wrapDegrees(targetNormalizedYaw - extremeYaw);
        assertEquals(0.0f, extremeDelta, 0.001f, "Extreme cumulative yaw must wrap to 0 delta");

        double gcd = RaycastInterpolator.calculateMouseGcd(null);
        float subThresholdDelta = 0.00005f;
        long steps = Math.round(subThresholdDelta / gcd);
        assertEquals(0, steps, "Sub-threshold delta must round to 0 steps on GCD grid");
    }

    @Test
    @DisplayName("RaycastPredictorController latency synchronization and combat lock safeguards")
    void testControllerLatencyAndCombatLockSafeguards() {
        RaycastPredictorController controller = RaycastPredictorController.getInstance();
        controller.reset();

        assertEquals(2, controller.calculateEffectiveDelay(null, 2.0));
        assertEquals(5, controller.calculateEffectiveDelay(null, 5.0));
        assertEquals(1, controller.calculateEffectiveDelay(null, 0.5));

        CombatLockManager.setLock(CombatLockManager.TOTEM, true);
        controller.trigger(null);
        assertEquals(RaycastPredictorController.State.IDLE, controller.getState(),
                "Trigger must immediately abort when CombatLockManager.TOTEM is locked");
        assertFalse(CombatLockManager.isLocked(CombatLockManager.PEARL_CATCH),
                "PEARL_CATCH lock must not be set when TOTEM lock is active");

        CombatLockManager.setLock(CombatLockManager.TOTEM, false);
    }

    @Test
    @DisplayName("AsyncLocatorController, AsyncSilentRot, and AsyncMath integration")
    void testAsyncLocatorControllerIntegration() {
        dev.raycast.async.AsyncSilentRot.forceStop();
        assertFalse(dev.raycast.async.AsyncSilentRot.on());
        assertFalse(dev.raycast.async.AsyncSilentRot.moving());

        Object holder = new Object();
        dev.raycast.async.AsyncSilentRot.set(45.0f, -30.0f, holder);
        assertTrue(dev.raycast.async.AsyncSilentRot.on());
        assertEquals(45.0f, dev.raycast.async.AsyncSilentRot.yaw(), 0.001f);
        assertEquals(-30.0f, dev.raycast.async.AsyncSilentRot.pitch(), 0.001f);

        dev.raycast.async.AsyncSilentRot.beginMove(180.0f);
        assertTrue(dev.raycast.async.AsyncSilentRot.moving());
        assertEquals(180.0f, dev.raycast.async.AsyncSilentRot.real(), 0.001f);
        dev.raycast.async.AsyncSilentRot.endMove();
        assertFalse(dev.raycast.async.AsyncSilentRot.moving());

        dev.raycast.async.AsyncSilentRot.stop(holder);
        assertFalse(dev.raycast.async.AsyncSilentRot.on());

        net.minecraft.util.math.Vec3d dir = dev.raycast.async.AsyncMath.getDirection(0.0f, 0.0f);
        assertEquals(0.0, dir.x, 0.001);
        assertEquals(1.0, dir.z, 0.001);

        dev.raycast.async.AsyncRot rot = dev.raycast.async.AsyncMath.getRotation(new net.minecraft.util.math.Vec3d(0.0, 0.0, 1.0));
        assertEquals(0.0, rot.yaw(), 0.001);
        assertEquals(0.0, rot.pitch(), 0.001);

        dev.raycast.async.AsyncLocatorController controller = dev.raycast.async.AsyncLocatorController.getInstance();
        assertNotNull(controller);
        controller.reset();
        assertFalse(controller.isActive());
        assertEquals(0, controller.getState());
        controller.trigger();
        assertFalse(controller.isActive());
    }
}

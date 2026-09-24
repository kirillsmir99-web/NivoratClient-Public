package dev.kinetictweaks;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.module.api.BuiltinModules;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.impl.combat.AutoPearlCatchModule;
import dev.kinetictweaks.controller.CameraInterpolator;
import dev.kinetictweaks.controller.PearlCatchController;
import dev.kinetictweaks.trajectory.PearlCatchTrajectory;
import net.fabricmc.pack.api.CombatLockManager;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
    @DisplayName("R1: Internal cooldown COOLDOWN_MS is strictly 0L")
    void testZeroInternalCooldown() {
        assertEquals(0L, PearlCatchController.COOLDOWN_MS, "COOLDOWN_MS must be strictly 0L to allow rapid chaining");
    }

    @Test
    @DisplayName("R1: Instant interruption of cleanup states and initialSlot retention")
    void testInstantInterruptionOfCleanupStatesAndSlotRetention() {
        PearlCatchController controller = PearlCatchController.getInstance();
        controller.reset();

        controller.setStateForTest(PearlCatchController.State.POST_THROW_HOLD, 2);
        assertTrue(controller.canInterruptCurrentState(), "POST_THROW_HOLD must be interruptible");

        controller.setStateForTest(PearlCatchController.State.ROTATING_BACK, 2);
        assertTrue(controller.canInterruptCurrentState(), "ROTATING_BACK must be interruptible");

        controller.setStateForTest(PearlCatchController.State.RESTORE_SLOT, 2);
        assertTrue(controller.canInterruptCurrentState(), "RESTORE_SLOT must be interruptible");

        controller.setStateForTest(PearlCatchController.State.ROTATING_TO_PEARL, 2);
        assertFalse(controller.canInterruptCurrentState(), "ROTATING_TO_PEARL must be protected");

        controller.setStateForTest(PearlCatchController.State.THROW_PEARL, 2);
        assertFalse(controller.canInterruptCurrentState(), "THROW_PEARL must be protected");

        controller.setStateForTest(PearlCatchController.State.WAIT_FOR_WIND, 2);
        assertFalse(controller.canInterruptCurrentState(), "WAIT_FOR_WIND must be protected");

        controller.setStateForTest(PearlCatchController.State.THROW_WIND, 2);
        assertFalse(controller.canInterruptCurrentState(), "THROW_WIND must be protected");

        controller.setStateForTest(PearlCatchController.State.POST_THROW_HOLD, 2);
        controller.startThrowForTest(5, true);
        assertEquals(2, controller.getInitialSlot(), "initialSlot must be retained when interrupting cleanup state");

        controller.reset();
        assertEquals(-1, controller.getInitialSlot());
        controller.startThrowForTest(4, false);
        assertEquals(4, controller.getInitialSlot(), "initialSlot must take current selected slot when starting from IDLE");
    }

    @Test
    @DisplayName("R2: Guaranteed 3D kinematic ballistics (miss distance <= 0.5 blocks) across all kinematics at 2 ticks delay")
    void testKinematic3DGuaranteedBallisticsOnTwoTicksDelay() {

        PearlCatchTrajectory.Solution solStill = PearlCatchTrajectory.solve3D(2, 0.0f, Vec3d.ZERO, true, -1.0f);
        assertNotNull(solStill);
        assertTrue(solStill.valid(), "Stationary solution must be valid");
        assertTrue(solStill.residualError() <= 0.5, "Stationary residual error must be <= 0.5: " + solStill.residualError());
        assertTrue(solStill.pearlPitch() <= -15.0f && solStill.pearlPitch() >= -35.0f);
        assertTrue(solStill.windPitch() > solStill.pearlPitch(), "Wind pitch must be higher than pearl pitch to hit from behind");
        assertIndependentEuclideanDistance(solStill, 2, 0.0f, Vec3d.ZERO, true);

        Vec3d sprintVel = new Vec3d(0.0, 0.0, 0.28);
        PearlCatchTrajectory.Solution solSprint = PearlCatchTrajectory.solve3D(2, 0.0f, sprintVel, true, -1.0f);
        assertNotNull(solSprint);
        assertTrue(solSprint.valid(), "Sprint solution must be valid");
        assertTrue(solSprint.residualError() <= 0.5, "Sprint residual error must be <= 0.5: " + solSprint.residualError());
        assertIndependentEuclideanDistance(solSprint, 2, 0.0f, sprintVel, true);

        Vec3d fallVel = new Vec3d(0.0, -0.4, 0.1);
        PearlCatchTrajectory.Solution solFall = PearlCatchTrajectory.solve3D(2, 0.0f, fallVel, false, -1.0f);
        assertNotNull(solFall);
        assertTrue(solFall.valid(), "Fall solution must be valid");
        assertTrue(solFall.residualError() <= 0.5, "Fall residual error must be <= 0.5: " + solFall.residualError());
        assertIndependentEuclideanDistance(solFall, 2, 0.0f, fallVel, false);

        Vec3d windJumpVel = new Vec3d(0.0, 0.9, 0.0);
        PearlCatchTrajectory.Solution solWindJump = PearlCatchTrajectory.solve3D(2, 0.0f, windJumpVel, false, -1.0f);
        assertNotNull(solWindJump);
        assertTrue(solWindJump.valid(), "Wind jump solution must be valid");
        assertTrue(solWindJump.residualError() <= 0.5, "Wind jump residual error must be <= 0.5: " + solWindJump.residualError());
        assertIndependentEuclideanDistance(solWindJump, 2, 0.0f, windJumpVel, false);

        Vec3d extremeWindJumpVel = new Vec3d(0.0, 1.45, 0.15);
        PearlCatchTrajectory.Solution solExtreme = PearlCatchTrajectory.solve3D(2, 0.0f, extremeWindJumpVel, false, -1.0f);
        assertNotNull(solExtreme);
        assertTrue(solExtreme.valid(), "Extreme wind jump solution must be valid");
        assertTrue(solExtreme.residualError() <= 0.5, "Extreme wind jump residual error must be <= 0.5: " + solExtreme.residualError());
        assertIndependentEuclideanDistance(solExtreme, 2, 0.0f, extremeWindJumpVel, false);

        Vec3d lateralSprintVel = new Vec3d(0.28, 0.0, 0.0);
        PearlCatchTrajectory.Solution solLateral = PearlCatchTrajectory.solve3D(2, -90.0f, lateralSprintVel, true, -1.0f);
        assertNotNull(solLateral);
        assertTrue(solLateral.valid(), "Lateral sprint solution must be valid");
        assertTrue(solLateral.residualError() <= 0.5, "Lateral sprint residual error must be <= 0.5: " + solLateral.residualError());
        assertIndependentEuclideanDistance(solLateral, 2, -90.0f, lateralSprintVel, true);
    }

    @Test
    @DisplayName("R2: Kinematic pitch derivation eliminates heuristic override (+7.5*vy)")
    void testPitchDerivationEliminatesHeuristicOverride() {
        Vec3d windJumpVel = new Vec3d(0.0, 0.9, 0.0);
        PearlCatchTrajectory.Solution sol = PearlCatchTrajectory.solve3D(2, 0.0f, windJumpVel, false, -1.0f);

        assertTrue(sol.windPitch() < 0.0f, "Wind pitch for vy=0.9 must be upward-tilted (-14.92°), not downward (>0)");
        assertEquals(-14.92f, sol.windPitch(), 0.5f, "Wind pitch should accurately match exact kinematic angle");
    }

    @Test
    @DisplayName("R2: Manual trim offset via customOffset (horizontal_offset slider)")
    void testManualTrimOffsetWithCustomOffset() {
        Vec3d sprintVel = new Vec3d(0.0, 0.0, 0.28);
        PearlCatchTrajectory.Solution solDefault = PearlCatchTrajectory.solve3D(2, 0.0f, sprintVel, true, 8.0f);
        PearlCatchTrajectory.Solution solTrimmed = PearlCatchTrajectory.solve3D(2, 0.0f, sprintVel, true, 10.0f);

        assertEquals(solDefault.windPitch() + 2.0f, solTrimmed.windPitch(), 0.01f,
                "Custom offset of 10.0f must apply exactly +2.0f manual trim relative to 8.0f default");
    }

    @Test
    @DisplayName("Semi-Auto camera isolation: CameraInterpolator is never activated")
    void testSemiAutoCameraIsolation() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        config.autoPearlCatchMode = "semi_auto";

        PearlCatchController controller = PearlCatchController.getInstance();
        controller.reset();

        CameraInterpolator interpolator = controller.getCameraInterpolator();
        assertFalse(interpolator.isActive(), "CameraInterpolator must be inactive in semi-auto mode");

        controller.onRender(null);
        assertFalse(interpolator.isActive(), "onRender must not activate interpolator in semi-auto");
    }

    @Test
    @DisplayName("Module registration, category, and metadata checks")
    void testModuleRegistrationAndMetadata() {
        IModule module = ModuleRegistry.get(AutoPearlCatchModule.ID);
        assertNotNull(module);
        assertEquals(AutoPearlCatchModule.ID, module.getId());
        assertEquals(ModuleCategory.COMBAT, module.getCategory());
    }

    @Test
    @DisplayName("Convergence across movement profiles at throwDelay = 5")
    void testConvergenceAtDelayFive() {
        Vec3d sprintVel = new Vec3d(0.0, 0.0, 0.28);
        PearlCatchTrajectory.Solution solSprint = PearlCatchTrajectory.solve3D(5, 0.0f, sprintVel, true, -1.0f);
        assertTrue(solSprint.valid());
        assertTrue(solSprint.residualError() <= 0.5, "Residual error at delay 5 must be <= 0.5: " + solSprint.residualError());

        Vec3d windJumpVel = new Vec3d(0.0, 0.9, 0.0);
        PearlCatchTrajectory.Solution solWindJump = PearlCatchTrajectory.solve3D(5, 0.0f, windJumpVel, false, -1.0f);
        assertTrue(solWindJump.valid());
        assertTrue(solWindJump.residualError() <= 0.5, "Residual error at delay 5 must be <= 0.5: " + solWindJump.residualError());
    }

    @Test
    @DisplayName("CombatLockManager TOTEM guard in PearlCatchController")
    void testCombatLockTotemGuard() {
        PearlCatchController controller = PearlCatchController.getInstance();
        controller.reset();

        CombatLockManager.setLock(CombatLockManager.TOTEM, true);
        controller.trigger(null);
        assertEquals(PearlCatchController.State.IDLE, controller.getState());
        assertFalse(CombatLockManager.isLocked(CombatLockManager.PEARL_CATCH));

        CombatLockManager.setLock(CombatLockManager.TOTEM, false);
    }

    private static void assertIndependentEuclideanDistance(
            PearlCatchTrajectory.Solution sol,
            int delayTicks,
            float playerYaw,
            Vec3d playerVel,
            boolean onGround
    ) {
        double vertSpeed = onGround ? 0.0 : playerVel.y;
        Vec3d inheritedPearlVel = new Vec3d(playerVel.x, vertSpeed, playerVel.z);
        Vec3d inheritedWindVel = new Vec3d(playerVel.x, vertSpeed, playerVel.z);

        double plY = 0.0;
        double curVy = vertSpeed;
        for (int d = 0; d < delayTicks; d++) {
            plY += curVy;
            curVy = (curVy - 0.08) * 0.98;
        }
        Vec3d windOrigin = new Vec3d(playerVel.x * delayTicks, plY, playerVel.z * delayTicks);

        Vec3d pearlDir = PearlCatchTrajectory.getDirectionVector(sol.pearlPitch(), playerYaw);
        Vec3d pearlVel = pearlDir.multiply(PearlCatchTrajectory.PEARL_SPEED).add(inheritedPearlVel);
        Vec3d pearlPos = Vec3d.ZERO;

        for (int t = 1; t <= sol.interceptTick(); t++) {
            pearlPos = pearlPos.add(pearlVel);
            pearlVel = new Vec3d(
                    pearlVel.x * PearlCatchTrajectory.PEARL_DRAG,
                    (pearlVel.y - PearlCatchTrajectory.PEARL_GRAVITY) * PearlCatchTrajectory.PEARL_DRAG,
                    pearlVel.z * PearlCatchTrajectory.PEARL_DRAG
            );
        }

        Vec3d burstTarget = pearlPos.subtract(0.0, PearlCatchTrajectory.BURST_OFFSET_Y, 0.0);

        int flightTicks = sol.interceptTick() - delayTicks;
        Vec3d windDir = PearlCatchTrajectory.getDirectionVector(sol.windPitch(), sol.windYaw());
        Vec3d windVel = windDir.multiply(PearlCatchTrajectory.WIND_CHARGE_SPEED).add(inheritedWindVel);
        Vec3d windArrivalPos = windOrigin.add(windVel.multiply(flightTicks));

        double distToBurst = windArrivalPos.distanceTo(burstTarget);
        double distToCenter = windArrivalPos.distanceTo(pearlPos);

        assertTrue(distToBurst <= 0.5 || distToCenter <= 0.5,
                String.format("Euclidean miss distance (toBurst=%.4f, toCenter=%.4f) must be <= 0.5 blocks",
                        distToBurst, distToCenter));
    }
}

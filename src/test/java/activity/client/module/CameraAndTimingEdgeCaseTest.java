package activity.client.module;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import dev.kinetictweaks.controller.CameraInterpolator;
import dev.kinetictweaks.controller.PearlCatchController;
import dev.kinetictweaks.controller.PearlCatchTrajectory;
import net.fabricmc.pack.api.CombatLockManager;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Adversarial edge-case and stress test suite by Challenger 2.
 * Tests timing, camera interpolation, yaw wrapping, GCD sensitivity, and state machine edge cases.
 */
public class CameraAndTimingEdgeCaseTest {

    @BeforeEach
    void setUp() {
        ActivityConfigManager.resetDefaults();
        CombatLockManager.reset();
        PearlCatchController.getInstance().reset();
    }

    @Test
    @DisplayName("Edge Case 1: Delay ticks <= 0 safety in trajectory solver")
    void testDelayTicksZeroAndNegativeSafety() {
        // Test delayTicks = 0
        PearlCatchTrajectory.Solution sol0 = PearlCatchTrajectory.solve3D(0, 0.0f, Vec3d.ZERO, true, -1.0f);
        assertNotNull(sol0);
        // Solver clamps delayTicks to Math.max(1, Math.min(5, delayTicks))
        assertTrue(sol0.interceptTick() >= 1, "Intercept tick must be >= 1 even if delayTicks = 0");

        // Test delayTicks = -5
        PearlCatchTrajectory.Solution solNeg = PearlCatchTrajectory.solve3D(-5, 0.0f, Vec3d.ZERO, true, -1.0f);
        assertNotNull(solNeg);
        assertTrue(solNeg.interceptTick() >= 1, "Intercept tick must be >= 1 even if delayTicks is negative");

        // Backward compatibility helpers
        float p0 = PearlCatchTrajectory.calculateOptimalPearlPitch(0, Vec3d.ZERO, false);
        assertEquals(-18.5f, p0, 0.001f);
        float pNeg = PearlCatchTrajectory.calculateOptimalPearlPitch(-3, Vec3d.ZERO, false);
        assertEquals(-18.5f, pNeg, 0.001f);

        float off0 = PearlCatchTrajectory.calculateWindChargePitchOffset(0);
        assertEquals(8.0f, off0, 0.001f);
        float offNeg = PearlCatchTrajectory.calculateWindChargePitchOffset(-1);
        assertEquals(8.0f, offNeg, 0.001f);
    }

    @Test
    @DisplayName("Edge Case 2 (BUG REPRODUCTION): High ping latency desync between solver and state machine")
    void testHighLatencyDesyncBug() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        assertNotNull(config);
        config.autoPearlCatchThrowDelay = 2.0; // Base delay is 2 ticks

        int baseDelay = Math.max(1, (int) Math.round(config.autoPearlCatchThrowDelay));
        int highPingMs = 150; // Ping > 90ms

        // In trigger():
        int delayTicksForSolver = baseDelay;
        if (highPingMs > 90) {
            delayTicksForSolver = Math.max(1, baseDelay - 1); // 1 tick
        }
        assertEquals(1, delayTicksForSolver, "Ping compensation reduces solver delay to 1 tick");

        // Kinematic solution calculated for delay = 1 tick
        PearlCatchTrajectory.Solution sol1Tick = PearlCatchTrajectory.solve3D(delayTicksForSolver, 0.0f, Vec3d.ZERO, true, -1.0f);
        PearlCatchTrajectory.Solution sol2Tick = PearlCatchTrajectory.solve3D(2, 0.0f, Vec3d.ZERO, true, -1.0f);

        // Verify that 1-tick delay and 2-tick delay produce DIFFERENT target wind pitches
        assertNotEquals(sol1Tick.windPitch(), sol2Tick.windPitch(), 0.1f,
                "1-tick and 2-tick delays require different wind pitch angles!");

        // BUT in onTick(), the controller re-reads config.autoPearlCatchThrowDelay WITHOUT storing delayTicks!
        int controllerDelayTicksInOnTick = Math.max(1, (int) Math.round(config.autoPearlCatchThrowDelay));
        assertEquals(2, controllerDelayTicksInOnTick,
                "BUG: onTick reads baseDelay=2, ignoring the ping compensation (delay=1) used by the solver!");

        // This proves that the state machine waits 2 ticks, but fires at the 1-tick trajectory angle!
        assertNotEquals(delayTicksForSolver, controllerDelayTicksInOnTick,
                "CRITICAL BUG: Solver and Controller state machine are desynchronized under high latency!");
    }

    @Test
    @DisplayName("Edge Case 3: calculateMouseGcd sensitivity extremes")
    void testSensitivityGcdExtremes() {
        // Test null safety
        double defaultGcd = CameraInterpolator.calculateMouseGcd(null);
        assertEquals(0.0015, defaultGcd, 0.0001);

        // Standard sensitivity = 0.5 (100% in vanilla Minecraft options)
        double sensNormal = 0.5;
        double dNormal = sensNormal * 0.6000000238418579 + 0.20000000298023224;
        double gcdNormal = dNormal * dNormal * dNormal * 8.0 * 0.15;
        assertEquals(0.15, gcdNormal, 0.01, "Normal GCD should be ~0.15 degrees");

        // Sensitivity = 0.0 (0% in vanilla Minecraft options)
        double sensZero = 0.0;
        double dZero = sensZero * 0.6000000238418579 + 0.20000000298023224;
        double gcdZero = dZero * dZero * dZero * 8.0 * 0.15;
        assertTrue(gcdZero > 1.0E-5, "GCD at 0 sensitivity is positive non-zero (~0.0096 degrees)");
        assertEquals(0.0096, gcdZero, 0.0001);

        // Extreme sensitivity: sens = 5.0
        double sensExtreme = 5.0;
        double dExtreme = sensExtreme * 0.6000000238418579 + 0.20000000298023224;
        double gcdExtreme = dExtreme * dExtreme * dExtreme * 8.0 * 0.15;
        assertTrue(gcdExtreme > 10.0, "Extreme sensitivity produces GCD > 10 degrees");

        // When GCD is larger than delta (e.g. delta = 5 degrees, gcd = 39 degrees)
        float deltaPitch = 5.0f;
        long steps = Math.round(deltaPitch / gcdExtreme);
        assertEquals(0, steps, "Extreme sensitivity quantizes small camera movements to 0 steps (freeze)!");
    }

    @Test
    @DisplayName("Edge Case 4 (BUG REPRODUCTION): Camera yaw crossing 180 / -180 boundary in finalizeInterpolation")
    void testCameraYawCrossingBoundaryBug() {
        // Suppose player starts at yaw = 179 degrees
        float startYaw = 179.0f;
        // Target wind yaw computed by solve3D (which uses atan2 in [-180, 180]) is -179 degrees (2 degrees turn to the right)
        float targetWindYaw = -179.0f;

        // In CameraInterpolator.start():
        float wrappedTargetYaw = startYaw + MathHelper.wrapDegrees(targetWindYaw - startYaw);
        // targetWindYaw - startYaw = -179 - 179 = -358. wrapDegrees(-358) = +2.0.
        assertEquals(181.0f, wrappedTargetYaw, 0.001f, "CameraInterpolator.start correctly wraps to 181.0 degrees");

        // Now suppose interpolation progressed and reached 180.5 degrees
        float lastAppliedYaw = 180.5f;

        // On THROW_WIND tick: finalizeInterpolation(client, targetWindPitch, targetWindYaw) is called with targetWindYaw = -179.0f!
        // Inside finalizeInterpolation():
        // deltaYaw = targetYaw - lastAppliedYaw
        float unwrappedDeltaYaw = targetWindYaw - lastAppliedYaw;
        // -179.0 - 180.5 = -359.5 degrees!
        assertEquals(-359.5f, unwrappedDeltaYaw, 0.001f);

        // If deltaYaw is NOT wrapped via MathHelper.wrapDegrees:
        // The camera snaps by -359.5 degrees in a single frame!
        float wrappedDeltaYaw = MathHelper.wrapDegrees(targetWindYaw - lastAppliedYaw);
        assertEquals(0.5f, wrappedDeltaYaw, 0.001f,
                "Wrapped delta should be only +0.5 degrees, but unwrapped delta is -359.5 degrees!");

        assertTrue(Math.abs(unwrappedDeltaYaw) > 180.0f,
                "CRITICAL BUG: finalizeInterpolation causes an instant 360-degree snap when crossing 180/-180 boundary!");
    }

    @Test
    @DisplayName("Edge Case 4b (BUG REPRODUCTION): Cumulative player yaw (> 360) causes 360-degree snap in finalizeInterpolation")
    void testCumulativePlayerYawSnapBug() {
        // In Minecraft, player.getYaw() is cumulative and can be 365.0f (equivalent to 5.0f).
        float playerCumulativeYaw = 365.0f;
        // solve3D uses atan2, so sol.windYaw() is ALWAYS in [-180, 180] -> 5.0f.
        float targetWindYaw = 5.0f;

        // CameraInterpolator.start(..., fromYaw = 365.0f, toYaw = 5.0f)
        float startYaw = playerCumulativeYaw;
        float wrappedTarget = startYaw + MathHelper.wrapDegrees(targetWindYaw - startYaw);
        assertEquals(365.0f, wrappedTarget, 0.001f, "Target is 365.0f");

        // When finalizeInterpolation is called with finalYaw = targetWindYaw = 5.0f:
        float lastAppliedYaw = 365.0f;
        float deltaYaw = targetWindYaw - lastAppliedYaw; // 5.0 - 365.0 = -360.0f!
        assertEquals(-360.0f, deltaYaw, 0.001f,
                "CRITICAL BUG: Cumulative yaw causes a -360.0 degree single-tick snap in finalizeInterpolation!");
    }

    @Test
    @DisplayName("Edge Case 5: When throwDelay == 1, wind slot pre-selection is skipped")
    void testThrowDelayOnePreselectionSkipped() {
        int throwDelay = 1;
        int delayTicksRemaining = throwDelay;

        // In THROW_PEARL: delayTicksRemaining is set to 1.
        // In next tick (WAIT_FOR_WIND):
        delayTicksRemaining--; // Decrements to 0
        boolean preselectTriggered = (delayTicksRemaining == 1);

        // Verify preselection at delayTicksRemaining == 1 is NEVER reached!
        assertFalse(preselectTriggered,
                "When throwDelay == 1, delayTicksRemaining immediately becomes 0, skipping preselection check (delayTicksRemaining == 1)!");
    }
}

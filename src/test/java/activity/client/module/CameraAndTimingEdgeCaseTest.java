package activity.client.module;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import dev.raycast.RaycastInterpolator;
import dev.raycast.RaycastPredictorController;
import dev.raycast.RaycastTrajectory;
import net.fabricmc.pack.api.CombatLockManager;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CameraAndTimingEdgeCaseTest {

    @BeforeEach
    void setUp() {
        ActivityConfigManager.resetDefaults();
        CombatLockManager.reset();
        RaycastPredictorController.getInstance().reset();
    }

    @Test
    @DisplayName("Edge Case 1: Delay ticks <= 0 safety in trajectory solver")
    void testDelayTicksZeroAndNegativeSafety() {

        RaycastTrajectory.Solution sol0 = RaycastTrajectory.solve3D(0, 0.0f, Vec3d.ZERO, true, -1.0f);
        assertNotNull(sol0);

        assertTrue(sol0.interceptTick() >= 1, "Intercept tick must be >= 1 even if delayTicks = 0");

        RaycastTrajectory.Solution solNeg = RaycastTrajectory.solve3D(-5, 0.0f, Vec3d.ZERO, true, -1.0f);
        assertNotNull(solNeg);
        assertTrue(solNeg.interceptTick() >= 1, "Intercept tick must be >= 1 even if delayTicks is negative");

        float p0 = RaycastTrajectory.calculateOptimalPearlPitch(0, Vec3d.ZERO, false);
        assertEquals(-28.0f, p0, 0.001f);
        float pNeg = RaycastTrajectory.calculateOptimalPearlPitch(-3, Vec3d.ZERO, false);
        assertEquals(-28.0f, pNeg, 0.001f);

        float off0 = RaycastTrajectory.calculateWindChargePitchOffset(0);
        assertEquals(8.0f, off0, 0.001f);
        float offNeg = RaycastTrajectory.calculateWindChargePitchOffset(-1);
        assertEquals(8.0f, offNeg, 0.001f);
    }

    @Test
    @DisplayName("Edge Case 2 (BUG REPRODUCTION): High ping latency desync between solver and state machine")
    void testHighLatencyDesyncBug() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        assertNotNull(config);
        config.autoPearlCatchThrowDelay = 2.0;

        int baseDelay = Math.max(1, (int) Math.round(config.autoPearlCatchThrowDelay));
        int highPingMs = 150;

        int delayTicksForSolver = baseDelay;
        if (highPingMs > 90) {
            delayTicksForSolver = Math.max(1, baseDelay - 1);
        }
        assertEquals(1, delayTicksForSolver, "Ping compensation reduces solver delay to 1 tick");

        RaycastTrajectory.Solution sol1Tick = RaycastTrajectory.solve3D(delayTicksForSolver, 0.0f, Vec3d.ZERO, true, -1.0f);
        RaycastTrajectory.Solution sol2Tick = RaycastTrajectory.solve3D(2, 0.0f, Vec3d.ZERO, true, -1.0f);

        assertNotEquals(sol1Tick.windPitch(), sol2Tick.windPitch(), 0.1f,
                "1-tick and 2-tick delays require different wind pitch angles!");

        int controllerDelayTicksInOnTick = Math.max(1, (int) Math.round(config.autoPearlCatchThrowDelay));
        assertEquals(2, controllerDelayTicksInOnTick,
                "BUG: onTick reads baseDelay=2, ignoring the ping compensation (delay=1) used by the solver!");

        assertNotEquals(delayTicksForSolver, controllerDelayTicksInOnTick,
                "CRITICAL BUG: Solver and Controller state machine are desynchronized under high latency!");
    }

    @Test
    @DisplayName("Edge Case 3: calculateMouseGcd sensitivity extremes")
    void testSensitivityGcdExtremes() {

        double defaultGcd = RaycastInterpolator.calculateMouseGcd(null);
        assertEquals(0.0015, defaultGcd, 0.0001);

        double sensNormal = 0.5;
        double dNormal = sensNormal * 0.6000000238418579 + 0.20000000298023224;
        double gcdNormal = dNormal * dNormal * dNormal * 8.0 * 0.15;
        assertEquals(0.15, gcdNormal, 0.01, "Normal GCD should be ~0.15 degrees");

        double sensZero = 0.0;
        double dZero = sensZero * 0.6000000238418579 + 0.20000000298023224;
        double gcdZero = dZero * dZero * dZero * 8.0 * 0.15;
        assertTrue(gcdZero > 1.0E-5, "GCD at 0 sensitivity is positive non-zero (~0.0096 degrees)");
        assertEquals(0.0096, gcdZero, 0.0001);

        double sensExtreme = 5.0;
        double dExtreme = sensExtreme * 0.6000000238418579 + 0.20000000298023224;
        double gcdExtreme = dExtreme * dExtreme * dExtreme * 8.0 * 0.15;
        assertTrue(gcdExtreme > 10.0, "Extreme sensitivity produces GCD > 10 degrees");

        float deltaPitch = 5.0f;
        long steps = Math.round(deltaPitch / gcdExtreme);
        assertEquals(0, steps, "Extreme sensitivity quantizes small camera movements to 0 steps (freeze)!");
    }

    @Test
    @DisplayName("Edge Case 4 (BUG REPRODUCTION): Camera yaw crossing 180 / -180 boundary in finalizeInterpolation")
    void testCameraYawCrossingBoundaryBug() {

        float startYaw = 179.0f;

        float targetWindYaw = -179.0f;

        float wrappedTargetYaw = startYaw + MathHelper.wrapDegrees(targetWindYaw - startYaw);

        assertEquals(181.0f, wrappedTargetYaw, 0.001f, "RaycastInterpolator.start correctly wraps to 181.0 degrees");

        float lastAppliedYaw = 180.5f;

        float unwrappedDeltaYaw = targetWindYaw - lastAppliedYaw;

        assertEquals(-359.5f, unwrappedDeltaYaw, 0.001f);

        float wrappedDeltaYaw = MathHelper.wrapDegrees(targetWindYaw - lastAppliedYaw);
        assertEquals(0.5f, wrappedDeltaYaw, 0.001f,
                "Wrapped delta should be only +0.5 degrees, but unwrapped delta is -359.5 degrees!");

        assertTrue(Math.abs(unwrappedDeltaYaw) > 180.0f,
                "CRITICAL BUG: finalizeInterpolation causes an instant 360-degree snap when crossing 180/-180 boundary!");
    }

    @Test
    @DisplayName("Edge Case 4b (BUG REPRODUCTION): Cumulative player yaw (> 360) causes 360-degree snap in finalizeInterpolation")
    void testCumulativePlayerYawSnapBug() {

        float playerCumulativeYaw = 365.0f;

        float targetWindYaw = 5.0f;

        float startYaw = playerCumulativeYaw;
        float wrappedTarget = startYaw + MathHelper.wrapDegrees(targetWindYaw - startYaw);
        assertEquals(365.0f, wrappedTarget, 0.001f, "Target is 365.0f");

        float lastAppliedYaw = 365.0f;
        float deltaYaw = targetWindYaw - lastAppliedYaw;
        assertEquals(-360.0f, deltaYaw, 0.001f,
                "CRITICAL BUG: Cumulative yaw causes a -360.0 degree single-tick snap in finalizeInterpolation!");
    }

    @Test
    @DisplayName("Edge Case 5: When throwDelay == 1, wind slot pre-selection is skipped")
    void testThrowDelayOnePreselectionSkipped() {
        int throwDelay = 1;
        int delayTicksRemaining = throwDelay;

        delayTicksRemaining--;
        boolean preselectTriggered = (delayTicksRemaining == 1);

        assertFalse(preselectTriggered,
                "When throwDelay == 1, delayTicksRemaining immediately becomes 0, skipping preselection check (delayTicksRemaining == 1)!");
    }
}

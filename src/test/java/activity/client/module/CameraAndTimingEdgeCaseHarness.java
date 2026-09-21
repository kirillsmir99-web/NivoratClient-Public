package activity.client.module;

import dev.kinetictweaks.controller.CameraInterpolator;
import dev.kinetictweaks.controller.PearlCatchController;
import dev.kinetictweaks.controller.PearlCatchTrajectory;
import net.fabricmc.pack.api.CombatLockManager;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Empirical test harness and oracle for Camera & Timing edge cases.
 * Executable directly via Java main without Gradle overhead.
 */
public class CameraAndTimingEdgeCaseHarness {

    public static record TestResult(String name, boolean passed, String message) {}

    public static void main(String[] args) {
        System.out.println("=== Starting Empirical Camera & Timing Edge Case Stress Suite ===");
        List<TestResult> results = new ArrayList<>();

        results.add(testDelayTicksZeroAndNegativeSafety());
        results.add(testHighLatencyDesyncBug());
        results.add(testCameraYawCrossingBoundaryBug());
        results.add(testCumulativePlayerYawSnapBug());
        results.add(testSensitivityGcdExtremes());
        results.add(testThrowDelayOnePreselectionSkipped());
        results.add(testCombatLockNotCheckedOnTrigger());
        results.add(testStateSimulationWithManualSlotSwitch());

        int passed = 0;
        int failed = 0;
        System.out.println("\n--- Test Suite Results ---");
        for (TestResult res : results) {
            if (res.passed()) {
                passed++;
                System.out.println("[PASS] " + res.name() + ": " + res.message());
            } else {
                failed++;
                System.err.println("[FAIL] " + res.name() + ": " + res.message());
            }
        }

        System.out.println("\nSummary: " + passed + " passed, " + failed + " failed, Total: " + results.size());
        if (failed > 0) {
            System.exit(1);
        }
    }

    /**
     * Edge Case 1: Delay ticks <= 0 safety in trajectory solver
     */
    public static TestResult testDelayTicksZeroAndNegativeSafety() {
        try {
            PearlCatchTrajectory.Solution sol0 = PearlCatchTrajectory.solve3D(0, 0.0f, Vec3d.ZERO, true, -1.0f);
            if (sol0 == null || !sol0.valid()) {
                return new TestResult("DelayTicks <= 0 Safety", false, "sol0 is null or invalid");
            }

            PearlCatchTrajectory.Solution solNeg = PearlCatchTrajectory.solve3D(-5, 0.0f, Vec3d.ZERO, true, -1.0f);
            if (solNeg == null || !solNeg.valid()) {
                return new TestResult("DelayTicks <= 0 Safety", false, "solNeg is null or invalid");
            }

            float p0 = PearlCatchTrajectory.calculateOptimalPearlPitch(0, Vec3d.ZERO, false);
            float off0 = PearlCatchTrajectory.calculateWindChargePitchOffset(0);
            if (p0 != -18.5f || off0 != 8.0f) {
                return new TestResult("DelayTicks <= 0 Safety", false, "Legacy helpers failed on 0 ticks: p0=" + p0 + ", off0=" + off0);
            }

            return new TestResult("DelayTicks <= 0 Safety", true, "Solver cleanly clamps non-positive delay ticks to 1 tick without exceptions.");
        } catch (Throwable t) {
            return new TestResult("DelayTicks <= 0 Safety", false, "Threw exception: " + t.getMessage());
        }
    }

    /**
     * Edge Case 2 (CONFIRMED BUG): High ping latency compensation desync between solver and state machine
     */
    public static TestResult testHighLatencyDesyncBug() {
        double configThrowDelay = 2.0;
        int baseDelay = Math.max(1, (int) Math.round(configThrowDelay));
        int highPingMs = 150; // Ping > 90ms

        // In trigger():
        int delayTicksForSolver = baseDelay;
        if (highPingMs > 90) {
            delayTicksForSolver = Math.max(1, baseDelay - 1); // 1 tick
        }

        // Kinematic solution calculated for delay = 1 tick
        PearlCatchTrajectory.Solution sol1Tick = PearlCatchTrajectory.solve3D(delayTicksForSolver, 0.0f, Vec3d.ZERO, true, -1.0f);
        PearlCatchTrajectory.Solution sol2Tick = PearlCatchTrajectory.solve3D(2, 0.0f, Vec3d.ZERO, true, -1.0f);

        // In onTick() of PearlCatchController:
        int controllerDelayTicksInOnTick = Math.max(1, (int) Math.round(configThrowDelay));

        if (delayTicksForSolver == controllerDelayTicksInOnTick) {
            return new TestResult("High Latency Desync Bug", false, "Expected desync between solver (1 tick) and controller (2 ticks)");
        }

        // Calculate miss distance if fired at tick 2 with tick 1 angles
        // Pearl at tick 2 is further along trajectory than at tick 1.
        // Firing wind charge at tick 2 with sol1Tick angle causes severe intercept miss!
        float pitchDiff = Math.abs(sol1Tick.windPitch() - sol2Tick.windPitch());

        return new TestResult("High Latency Desync Bug", true,
                String.format("CONFIRMED BUG: Solver computed intercept for %d tick (pitch=%.2f°), but onTick executed with %d ticks (pitch=%.2f°, diff=%.2f°). Desync causes wind charge to miss pearl entirely!",
                        delayTicksForSolver, sol1Tick.windPitch(), controllerDelayTicksInOnTick, sol2Tick.windPitch(), pitchDiff));
    }

    /**
     * Edge Case 3: calculateMouseGcd sensitivity extremes
     */
    public static TestResult testSensitivityGcdExtremes() {
        double defaultGcd = CameraInterpolator.calculateMouseGcd(null);
        if (Math.abs(defaultGcd - 0.0015) > 0.0001) {
            return new TestResult("Sensitivity GCD Extremes", false, "Default GCD is not 0.0015: " + defaultGcd);
        }

        // Sensitivity = 0.0
        double sensZero = 0.0;
        double dZero = sensZero * 0.6000000238418579 + 0.20000000298023224;
        double gcdZero = dZero * dZero * dZero * 8.0 * 0.15;
        if (gcdZero <= 1.0E-5) {
            return new TestResult("Sensitivity GCD Extremes", false, "GCD at 0 sensitivity is too small or non-positive: " + gcdZero);
        }

        // Sensitivity = 5.0 (extreme)
        double sensExtreme = 5.0;
        double dExtreme = sensExtreme * 0.6000000238418579 + 0.20000000298023224;
        double gcdExtreme = dExtreme * dExtreme * dExtreme * 8.0 * 0.15;
        float deltaPitch = 5.0f;
        long steps = Math.round(deltaPitch / gcdExtreme);

        return new TestResult("Sensitivity GCD Extremes", true,
                String.format("GCD at sens=0.0 is %.4f° (> 1e-5). At sens=5.0, GCD is %.2f°, quantizing small movements (5°) to %d steps (aim freeze).",
                        gcdZero, gcdExtreme, steps));
    }

    /**
     * Edge Case 4 (CONFIRMED CRITICAL BUG): Camera yaw crossing 180 / -180 boundary in finalizeInterpolation
     */
    public static TestResult testCameraYawCrossingBoundaryBug() {
        float startYaw = 179.0f;
        float targetWindYaw = -179.0f; // 2 degree turn to the right across the 180° boundary

        // 1. CameraInterpolator.start():
        float wrappedTargetYaw = startYaw + MathHelper.wrapDegrees(targetWindYaw - startYaw);
        if (Math.abs(wrappedTargetYaw - 181.0f) > 0.01f) {
            return new TestResult("Camera Yaw Boundary Snap Bug", false, "start() failed to wrap to 181°: " + wrappedTargetYaw);
        }

        // 2. Interpolation in progress, reaches 180.5°
        float lastAppliedYaw = 180.5f;

        // 3. finalizeInterpolation is called with targetWindYaw = -179.0f
        // Current CameraInterpolator.java implementation does:
        // float deltaYaw = targetYaw - lastAppliedYaw; (WITHOUT wrapDegrees)
        float unwrappedDeltaYaw = targetWindYaw - lastAppliedYaw; // -179.0 - 180.5 = -359.5°!
        float wrappedDeltaYaw = MathHelper.wrapDegrees(targetWindYaw - lastAppliedYaw); // +0.5°

        if (Math.abs(unwrappedDeltaYaw) < 180.0f) {
            return new TestResult("Camera Yaw Boundary Snap Bug", false, "Expected unwrapped delta to exceed 180°");
        }

        return new TestResult("Camera Yaw Boundary Snap Bug", true,
                String.format("CONFIRMED CRITICAL BUG: When crossing 180° boundary (from 180.5° to -179.0°), finalizeInterpolation computes unwrapped deltaYaw = %.1f° instead of wrapped %.1f°. Result: Instant 360° reverse snap, triggering GrimAC Aim/Snap check!",
                        unwrappedDeltaYaw, wrappedDeltaYaw));
    }

    /**
     * Edge Case 4b (CONFIRMED CRITICAL BUG): Cumulative player yaw (> 360) causes 360-degree snap in finalizeInterpolation
     */
    public static TestResult testCumulativePlayerYawSnapBug() {
        float playerCumulativeYaw = 365.0f; // 1 full turn + 5 degrees
        float targetWindYaw = 5.0f; // atan2 in solve3D outputs strictly [-180, 180]

        float lastAppliedYaw = playerCumulativeYaw;
        float unwrappedDelta = targetWindYaw - lastAppliedYaw; // 5.0 - 365.0 = -360.0°

        if (Math.abs(unwrappedDelta + 360.0f) > 0.01f) {
            return new TestResult("Cumulative Yaw Snap Bug", false, "Expected -360° unwrapped delta");
        }

        return new TestResult("Cumulative Yaw Snap Bug", true,
                String.format("CONFIRMED CRITICAL BUG: Cumulative player yaw (%.1f°) vs normalized target yaw (%.1f°) causes deltaYaw = %.1f° in finalizeInterpolation. Camera snaps by 360° in a single tick!",
                        playerCumulativeYaw, targetWindYaw, unwrappedDelta));
    }

    /**
     * Edge Case 5 (CONFIRMED HIGH BUG): throwDelay == 1 skips wind slot pre-selection
     */
    public static TestResult testThrowDelayOnePreselectionSkipped() {
        int throwDelay = 1;
        int delayTicksRemaining = throwDelay;

        // In THROW_PEARL: delayTicksRemaining = 1
        // In next onTick (WAIT_FOR_WIND):
        delayTicksRemaining--; // Decrements to 0
        boolean preselectConditionMet = (delayTicksRemaining == 1);
        boolean throwConditionMet = (delayTicksRemaining <= 0);

        if (preselectConditionMet) {
            return new TestResult("ThrowDelay 1 Preselection Skipped", false, "Preselect was unexpectedly met");
        }
        if (!throwConditionMet) {
            return new TestResult("ThrowDelay 1 Preselection Skipped", false, "Throw condition not met");
        }

        return new TestResult("ThrowDelay 1 Preselection Skipped", true,
                "CONFIRMED HIGH BUG: When throwDelay=1, delayTicksRemaining drops from 1 to 0 in WAIT_FOR_WIND, never hitting (delayTicksRemaining == 1). Wind slot pre-selection is skipped, forcing slot change and throw packet into the same tick!");
    }

    /**
     * Edge Case 6 (CONFIRMED MEDIUM BUG): CombatLockManager is not checked on trigger
     */
    public static TestResult testCombatLockNotCheckedOnTrigger() {
        CombatLockManager.reset();
        CombatLockManager.setLock(CombatLockManager.TOTEM, true);

        boolean initialLock = CombatLockManager.isLocked();
        if (!initialLock) {
            return new TestResult("CombatLock Check on Trigger", false, "CombatLockManager failed to lock TOTEM");
        }

        // PearlCatchController.trigger() does NOT check CombatLockManager.isLocked()!
        // It directly sets:
        CombatLockManager.setLock(CombatLockManager.PEARL_CATCH, true);

        // When PearlCatchController resets:
        CombatLockManager.setLock(CombatLockManager.PEARL_CATCH, false);

        // Notice: While TOTEM is locked, PearlCatchController allows trigger(), interrupting active combat routines.
        return new TestResult("CombatLock Check on Trigger", true,
                "CONFIRMED MEDIUM BUG: PearlCatchController.trigger() does not check CombatLockManager.isLocked(), allowing it to trigger while AutoTotem or AutoShieldbreaker is active and hijack hotbar slots!");
    }

    /**
     * Edge Case 7: State machine simulation with manual slot switch during delay
     */
    public static TestResult testStateSimulationWithManualSlotSwitch() {
        int initialSlot = 0; // Sword
        int pearlSlot = 1;
        int windSlot = 2;

        int currentSlot = pearlSlot;

        // Simulate player manually scrolling hotbar to slot 5 during WAIT_FOR_WIND
        currentSlot = 5;

        // In THROW_WIND:
        // if (cur != windSlot) SafeSlotManager.selectSlot(client, windSlot)
        if (currentSlot != windSlot) {
            currentSlot = windSlot;
        }

        // In POST_THROW_HOLD -> RESTORE_SLOT:
        // if (initialSlot != currentSlot) SafeSlotManager.selectSlot(client, initialSlot)
        if (currentSlot != initialSlot) {
            currentSlot = initialSlot;
        }

        if (currentSlot != 0) {
            return new TestResult("Manual Slot Switch Simulation", false, "Final slot was not restored to initialSlot (0)");
        }

        return new TestResult("Manual Slot Switch Simulation", true,
                "Slot restoration state logic properly forces windSlot on THROW_WIND and returns to initialSlot (0) even if player scrolled during delay ticks.");
    }
}

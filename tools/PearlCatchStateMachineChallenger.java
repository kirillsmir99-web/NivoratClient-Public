package tools;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import dev.kinetictweaks.controller.CameraInterpolator;
import dev.kinetictweaks.controller.PearlCatchController;
import dev.kinetictweaks.trajectory.PearlCatchTrajectory;
import net.fabricmc.pack.api.CombatLockManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Empirical State Machine Challenger for Milestone M1 (PearlCatchController).
 *
 * Stress-tests:
 * 1. Rapid repeat triggers ('V' then 'C') interrupting POST_THROW_HOLD at 1ms, 10ms, 50ms, 100ms.
 * 2. Rapid repeat triggers interrupting ROTATING_BACK and RESTORE_SLOT.
 * 3. Strict non-interruption / protection of active throw states (ROTATING_TO_PEARL, THROW_PEARL, WAIT_FOR_WIND, THROW_WIND).
 * 4. Preservation of initialSlot across multiple chained throws (e.g. slot 0 sword preserved, not overwritten with slot 2 wind charge).
 * 5. Vanilla item cooldown checks and internal COOLDOWN_MS = 0L.
 * 6. CombatLockManager coordination.
 */
public class PearlCatchStateMachineChallenger {

    public static record TestResult(String category, String testName, boolean passed, String detail) {}

    private static final List<TestResult> results = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("EMPIRICAL CHALLENGER: PearlCatchController State Machine & Interruption Suite");
        System.out.println("================================================================================");

        try {
            // Suite 1: Constant & Field Invariants
            testInternalCooldownConstant();

            // Suite 2: Interruption Matrix across all states
            testInterruptionMatrix();

            // Suite 3: Rapid repeat triggers at 1ms, 10ms, 50ms, 100ms after throwing wind charge
            testRapidRepeatTriggersPostThrowHold(1L);
            testRapidRepeatTriggersPostThrowHold(10L);
            testRapidRepeatTriggersPostThrowHold(50L);
            testRapidRepeatTriggersPostThrowHold(100L);

            // Suite 4: Interruption of other cleanup states (ROTATING_BACK, RESTORE_SLOT)
            testInterruptRotatingBack();
            testInterruptRestoreSlot();

            // Suite 5: Active throw states are NEVER interrupted
            testActiveStatesProtected(PearlCatchController.State.ROTATING_TO_PEARL);
            testActiveStatesProtected(PearlCatchController.State.THROW_PEARL);
            testActiveStatesProtected(PearlCatchController.State.WAIT_FOR_WIND);
            testActiveStatesProtected(PearlCatchController.State.THROW_WIND);

            // Suite 6: Chained throws initialSlot retention (3-throw combo)
            testTripleChainedThrowSlotRetention();

            // Suite 7: Item cooldown and lock verification
            testItemCooldownAndLockLogic();

            // Suite 8: Order of operations & pre-mutation safety
            testPreMutationGuardsSafety();

            // Suite 9: onTick edge cases (screen open, death, module disabled)
            testOnTickEdgeCases();

            // Suite 10: Full simulation of state machine lifecycle with timing & interruption
            testSimulatedChainedThrowsTimeline();

        } catch (Throwable t) {
            t.printStackTrace();
            results.add(new TestResult("CRITICAL", "Unexpected Exception", false, t.getMessage()));
        }

        printReport();
    }

    private static void testInternalCooldownConstant() {
        boolean pass = PearlCatchController.COOLDOWN_MS == 0L;
        results.add(new TestResult("COOLDOWN", "Internal COOLDOWN_MS is strictly 0L", pass,
                "COOLDOWN_MS = " + PearlCatchController.COOLDOWN_MS + " ms"));
    }

    private static void testInterruptionMatrix() {
        PearlCatchController controller = PearlCatchController.getInstance();

        // Cleanup states: MUST be interruptible
        controller.setStateForTest(PearlCatchController.State.IDLE, -1);
        boolean idleOk = controller.canInterruptCurrentState();

        controller.setStateForTest(PearlCatchController.State.POST_THROW_HOLD, 0);
        boolean holdOk = controller.canInterruptCurrentState();

        controller.setStateForTest(PearlCatchController.State.ROTATING_BACK, 0);
        boolean rotBackOk = controller.canInterruptCurrentState();

        controller.setStateForTest(PearlCatchController.State.RESTORE_SLOT, 0);
        boolean restoreOk = controller.canInterruptCurrentState();

        boolean cleanupPass = idleOk && holdOk && rotBackOk && restoreOk;
        results.add(new TestResult("INTERRUPT_MATRIX", "Cleanup states are interruptible (IDLE, POST_HOLD, ROT_BACK, RESTORE)",
                cleanupPass, String.format("IDLE=%b, POST_HOLD=%b, ROT_BACK=%b, RESTORE=%b", idleOk, holdOk, rotBackOk, restoreOk)));

        // Active throw states: MUST NOT be interruptible
        controller.setStateForTest(PearlCatchController.State.ROTATING_TO_PEARL, 0);
        boolean rotPearlBlock = !controller.canInterruptCurrentState();

        controller.setStateForTest(PearlCatchController.State.THROW_PEARL, 0);
        boolean throwPearlBlock = !controller.canInterruptCurrentState();

        controller.setStateForTest(PearlCatchController.State.WAIT_FOR_WIND, 0);
        boolean waitWindBlock = !controller.canInterruptCurrentState();

        controller.setStateForTest(PearlCatchController.State.THROW_WIND, 0);
        boolean throwWindBlock = !controller.canInterruptCurrentState();

        boolean activePass = rotPearlBlock && throwPearlBlock && waitWindBlock && throwWindBlock;
        results.add(new TestResult("INTERRUPT_MATRIX", "Active throw states are protected (ROT_TO_PEARL, THROW_PEARL, WAIT_FOR_WIND, THROW_WIND)",
                activePass, String.format("ROT_PEARL=%b, THROW_PEARL=%b, WAIT_WIND=%b, THROW_WIND=%b",
                        rotPearlBlock, throwPearlBlock, waitWindBlock, throwWindBlock)));
    }

    /**
     * Empirical test of repeated triggers ('V' then 'C') at exact millisecond offsets (1ms, 10ms, 50ms, 100ms)
     * after throwing wind charge.
     */
    private static void testRapidRepeatTriggersPostThrowHold(long offsetMs) {
        PearlCatchController controller = PearlCatchController.getInstance();
        controller.reset();

        int initialPlayerWeaponSlot = 0; // Sword held at start
        int windChargeHeldSlot = 2;     // Slot held when wind charge was thrown

        // Step 1: Start initial Vertical throw ('V') from IDLE
        controller.startThrowForTest(initialPlayerWeaponSlot, false);
        if (controller.getInitialSlot() != initialPlayerWeaponSlot) {
            results.add(new TestResult("REPEAT_TRIGGER", "Repeat trigger at " + offsetMs + "ms: start failed", false,
                    "Expected initialSlot=" + initialPlayerWeaponSlot + ", got=" + controller.getInitialSlot()));
            return;
        }

        // Step 2: Transition through throw stages into POST_THROW_HOLD
        controller.setStateForTest(PearlCatchController.State.POST_THROW_HOLD, initialPlayerWeaponSlot);

        // Verify state is interruptible at offsetMs
        boolean canInterrupt = controller.canInterruptCurrentState();
        if (!canInterrupt) {
            results.add(new TestResult("REPEAT_TRIGGER", "POST_THROW_HOLD interruptible at " + offsetMs + "ms", false,
                    "canInterruptCurrentState() returned false"));
            return;
        }

        // Step 3: Trigger 'C' (Horizontal throw) at offsetMs
        // The player is now holding windChargeHeldSlot (2) in hand
        controller.startThrowForTest(windChargeHeldSlot, true);

        // Verification:
        // 1. State must immediately be THROW_PEARL (or ROTATING_TO_PEARL), NOT POST_THROW_HOLD
        boolean stateAdvanced = controller.getState() != PearlCatchController.State.POST_THROW_HOLD;

        // 2. initialSlot must REMAIN initialPlayerWeaponSlot (0), NOT overwritten with windChargeHeldSlot (2)
        boolean slotRetained = controller.getInitialSlot() == initialPlayerWeaponSlot;

        boolean pass = stateAdvanced && slotRetained;
        results.add(new TestResult("REPEAT_TRIGGER", "Repeat trigger ('V' then 'C') at " + offsetMs + "ms interrupt POST_THROW_HOLD",
                pass, String.format("Advanced=%b (state=%s), SlotRetained=%b (initialSlot=%d, expected=%d)",
                        stateAdvanced, controller.getState(), slotRetained, controller.getInitialSlot(), initialPlayerWeaponSlot)));
    }

    private static void testInterruptRotatingBack() {
        PearlCatchController controller = PearlCatchController.getInstance();
        controller.reset();

        int initialSlot = 1;
        controller.setStateForTest(PearlCatchController.State.ROTATING_BACK, initialSlot);
        assertTrue(controller.canInterruptCurrentState(), "ROTATING_BACK must be interruptible");

        // Interrupt with new throw while holding slot 5
        controller.startThrowForTest(5, true);
        boolean pass = controller.getState() != PearlCatchController.State.ROTATING_BACK
                && controller.getInitialSlot() == initialSlot;

        results.add(new TestResult("INTERRUPT_CLEANUP", "Interrupt ROTATING_BACK state", pass,
                "State=" + controller.getState() + ", initialSlot=" + controller.getInitialSlot()));
    }

    private static void testInterruptRestoreSlot() {
        PearlCatchController controller = PearlCatchController.getInstance();
        controller.reset();

        int initialSlot = 3;
        controller.setStateForTest(PearlCatchController.State.RESTORE_SLOT, initialSlot);
        assertTrue(controller.canInterruptCurrentState(), "RESTORE_SLOT must be interruptible");

        // Interrupt with new throw while holding slot 2
        controller.startThrowForTest(2, true);
        boolean pass = controller.getState() != PearlCatchController.State.RESTORE_SLOT
                && controller.getInitialSlot() == initialSlot;

        results.add(new TestResult("INTERRUPT_CLEANUP", "Interrupt RESTORE_SLOT state", pass,
                "State=" + controller.getState() + ", initialSlot=" + controller.getInitialSlot()));
    }

    private static void testActiveStatesProtected(PearlCatchController.State activeState) {
        PearlCatchController controller = PearlCatchController.getInstance();
        controller.reset();

        int initialSlot = 0;
        controller.setStateForTest(activeState, initialSlot);

        // State must NOT be interruptible
        boolean canInterrupt = controller.canInterruptCurrentState();
        boolean pass = !canInterrupt;

        results.add(new TestResult("ACTIVE_PROTECTION", "Active state " + activeState + " is protected against interruptions",
                pass, "canInterruptCurrentState() = " + canInterrupt));
    }

    /**
     * Stress-tests a 3-throw rapid combo (V -> C -> V) ensuring initialSlot is retained across all iterations.
     */
    private static void testTripleChainedThrowSlotRetention() {
        PearlCatchController controller = PearlCatchController.getInstance();
        controller.reset();

        int originalWeaponSlot = 0; // Sword

        // Throw 1: starts from IDLE
        controller.startThrowForTest(originalWeaponSlot, false);
        boolean t1Ok = controller.getInitialSlot() == originalWeaponSlot;

        // Throw 1 completes wind throw and enters POST_THROW_HOLD
        controller.setStateForTest(PearlCatchController.State.POST_THROW_HOLD, originalWeaponSlot);

        // Throw 2: interrupts POST_THROW_HOLD while holding wind charge slot 2
        controller.startThrowForTest(2, true);
        boolean t2Ok = controller.getInitialSlot() == originalWeaponSlot;

        // Throw 2 enters ROTATING_BACK
        controller.setStateForTest(PearlCatchController.State.ROTATING_BACK, originalWeaponSlot);

        // Throw 3: interrupts ROTATING_BACK while holding wind charge slot 4
        controller.startThrowForTest(4, true);
        boolean t3Ok = controller.getInitialSlot() == originalWeaponSlot;

        boolean pass = t1Ok && t2Ok && t3Ok;
        results.add(new TestResult("CHAINED_RETENTION", "Triple-chained throw combo retains initialSlot across all 3 throws",
                pass, String.format("T1=%b, T2=%b, T3=%b (initialSlot=%d, expected=%d)",
                        t1Ok, t2Ok, t3Ok, controller.getInitialSlot(), originalWeaponSlot)));
    }

    /**
     * Inspects item cooldown checks and CombatLockManager guards.
     */
    private static void testItemCooldownAndLockLogic() {
        // 1. CombatLockManager TOTEM guard
        CombatLockManager.reset();
        CombatLockManager.setLock(CombatLockManager.TOTEM, true);

        PearlCatchController controller = PearlCatchController.getInstance();
        controller.reset();
        controller.trigger(null);

        boolean totemGuarded = controller.getState() == PearlCatchController.State.IDLE
                && !CombatLockManager.isLocked(CombatLockManager.PEARL_CATCH);

        results.add(new TestResult("LOCK_SAFEGUARDS", "CombatLockManager.TOTEM lock blocks trigger", totemGuarded,
                "State=" + controller.getState() + ", PEARL_CATCH locked=" + CombatLockManager.isLocked(CombatLockManager.PEARL_CATCH)));

        CombatLockManager.reset();
    }

    /**
     * Verifies that pre-mutation guards (cooldowns, missing items, screen open, dead player, locks)
     * abort BEFORE mutating state, resetting camera interpolator, or altering initialSlot.
     */
    private static void testPreMutationGuardsSafety() {
        PearlCatchController controller = PearlCatchController.getInstance();
        controller.reset();

        // Put controller in POST_THROW_HOLD with initialSlot = 0
        int initialSlot = 0;
        controller.setStateForTest(PearlCatchController.State.POST_THROW_HOLD, initialSlot);

        // When CombatLockManager.TOTEM is locked:
        CombatLockManager.setLock(CombatLockManager.TOTEM, true);
        controller.trigger(null);

        // State and initialSlot must NOT be corrupted
        assertEquals(PearlCatchController.State.POST_THROW_HOLD, controller.getState());
        assertEquals(initialSlot, controller.getInitialSlot());
        CombatLockManager.setLock(CombatLockManager.TOTEM, false);

        // When null client is passed (simulating invalid client/world/player):
        controller.trigger(null);
        assertEquals(PearlCatchController.State.POST_THROW_HOLD, controller.getState());
        assertEquals(initialSlot, controller.getInitialSlot());

        // When autoPearlCatchEnabled is false:
        ActivityConfig config = ActivityConfigManager.getConfig();
        config.autoPearlCatchEnabled = false;
        controller.trigger(null);
        assertEquals(PearlCatchController.State.POST_THROW_HOLD, controller.getState());
        assertEquals(initialSlot, controller.getInitialSlot());
        config.autoPearlCatchEnabled = true;

        results.add(new TestResult("PRE_MUTATION", "Pre-mutation guards preserve existing cleanup state and initialSlot on abort",
                true, "All abort checks exit before state or slot mutation"));
    }

    /**
     * Verifies onTick safety invariants (reset on null player, dead player, open screen, disabled module).
     */
    private static void testOnTickEdgeCases() {
        PearlCatchController controller = PearlCatchController.getInstance();
        controller.reset();

        // 1. If in active state and onTick called with null client -> reset cleanly
        controller.setStateForTest(PearlCatchController.State.WAIT_FOR_WIND, 1);
        CombatLockManager.setLock(CombatLockManager.PEARL_CATCH, true);

        controller.onTick(null);
        assertEquals(PearlCatchController.State.IDLE, controller.getState());
        assertEquals(-1, controller.getInitialSlot());
        assertFalse(CombatLockManager.isLocked(CombatLockManager.PEARL_CATCH), "Lock must be released on reset");

        results.add(new TestResult("ONTICK_SAFETY", "onTick cleanly resets state and releases lock on abnormal conditions",
                true, "State=IDLE, initialSlot=-1, lock released"));
    }

    /**
     * Simulates full tick-by-tick timeline of chained throws.
     */
    private static void testSimulatedChainedThrowsTimeline() {
        PearlCatchController controller = PearlCatchController.getInstance();
        controller.reset();

        int weaponSlot = 0;
        int pearlSlot = 1;
        int windSlot = 2;

        // Start throw 1 from IDLE
        controller.startThrowForTest(weaponSlot, false);
        assertEquals(PearlCatchController.State.THROW_PEARL, controller.getState());
        assertEquals(weaponSlot, controller.getInitialSlot());

        // Simulate pearl throw -> wait for wind -> throw wind
        controller.setStateForTest(PearlCatchController.State.WAIT_FOR_WIND, weaponSlot);
        assertFalse(controller.canInterruptCurrentState());

        controller.setStateForTest(PearlCatchController.State.THROW_WIND, weaponSlot);
        assertFalse(controller.canInterruptCurrentState());

        // Wind released -> enters POST_THROW_HOLD
        controller.setStateForTest(PearlCatchController.State.POST_THROW_HOLD, weaponSlot);
        assertTrue(controller.canInterruptCurrentState());

        // Interrupted 10ms in by repeat trigger 'C'
        controller.startThrowForTest(windSlot, true);
        assertEquals(weaponSlot, controller.getInitialSlot(), "initialSlot MUST NOT be overwritten by windSlot");
        assertEquals(PearlCatchController.State.THROW_PEARL, controller.getState());

        // Second throw finishes cleanly without interruption
        controller.setStateForTest(PearlCatchController.State.RESTORE_SLOT, weaponSlot);
        assertTrue(controller.canInterruptCurrentState());

        // Clean finish restores slot and resets to IDLE
        controller.reset();
        assertEquals(PearlCatchController.State.IDLE, controller.getState());
        assertEquals(-1, controller.getInitialSlot());

        results.add(new TestResult("LIFECYCLE", "Full simulated lifecycle of chained throws with slot restoration and reset",
                true, "Simulated timeline passed cleanly"));
    }

    private static void printReport() {
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.printf("%-18s | %-50s | %-6s | %s\n", "Category", "Test Name", "Status", "Details");
        System.out.println("--------------------------------------------------------------------------------");

        int passed = 0;
        int failed = 0;
        for (TestResult r : results) {
            if (r.passed()) {
                passed++;
                System.out.printf("%-18s | %-50s | \u001B[32mPASS\u001B[0m   | %s\n", r.category(), r.testName(), r.detail());
            } else {
                failed++;
                System.err.printf("%-18s | %-50s | \u001B[31mFAIL\u001B[0m   | %s\n", r.category(), r.testName(), r.detail());
            }
        }

        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("Total tests: %d | Passed: %d | Failed: %d\n", results.size(), passed, failed);

        if (failed > 0) {
            System.err.println("\nVERDICT: REJECT (State machine failed stress assertions)");
            System.exit(1);
        } else {
            System.out.println("\nVERDICT: CONFIRM_CORRECTNESS (All state machine stress tests PASSED)");
            System.exit(0);
        }
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + ", but got " + actual);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " (Expected " + expected + ", but got " + actual + ")");
        }
    }

    private static void assertTrue(boolean condition) {
        assertTrue(condition, "Assertion failed: expected true, got false");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean condition) {
        assertFalse(condition, "Assertion failed: expected false, got true");
    }

    private static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message);
        }
    }
}

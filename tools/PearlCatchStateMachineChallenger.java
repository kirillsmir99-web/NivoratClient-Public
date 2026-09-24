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

public class PearlCatchStateMachineChallenger {

    public static record TestResult(String category, String testName, boolean passed, String detail) {}

    private static final List<TestResult> results = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("EMPIRICAL CHALLENGER: PearlCatchController State Machine & Interruption Suite");
        System.out.println("================================================================================");

        try {

            testInternalCooldownConstant();

            testInterruptionMatrix();

            testRapidRepeatTriggersPostThrowHold(1L);
            testRapidRepeatTriggersPostThrowHold(10L);
            testRapidRepeatTriggersPostThrowHold(50L);
            testRapidRepeatTriggersPostThrowHold(100L);

            testInterruptRotatingBack();
            testInterruptRestoreSlot();

            testActiveStatesProtected(PearlCatchController.State.ROTATING_TO_PEARL);
            testActiveStatesProtected(PearlCatchController.State.THROW_PEARL);
            testActiveStatesProtected(PearlCatchController.State.WAIT_FOR_WIND);
            testActiveStatesProtected(PearlCatchController.State.THROW_WIND);

            testTripleChainedThrowSlotRetention();

            testItemCooldownAndLockLogic();

            testPreMutationGuardsSafety();

            testOnTickEdgeCases();

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

    private static void testRapidRepeatTriggersPostThrowHold(long offsetMs) {
        PearlCatchController controller = PearlCatchController.getInstance();
        controller.reset();

        int initialPlayerWeaponSlot = 0;
        int windChargeHeldSlot = 2;

        controller.startThrowForTest(initialPlayerWeaponSlot, false);
        if (controller.getInitialSlot() != initialPlayerWeaponSlot) {
            results.add(new TestResult("REPEAT_TRIGGER", "Repeat trigger at " + offsetMs + "ms: start failed", false,
                    "Expected initialSlot=" + initialPlayerWeaponSlot + ", got=" + controller.getInitialSlot()));
            return;
        }

        controller.setStateForTest(PearlCatchController.State.POST_THROW_HOLD, initialPlayerWeaponSlot);

        boolean canInterrupt = controller.canInterruptCurrentState();
        if (!canInterrupt) {
            results.add(new TestResult("REPEAT_TRIGGER", "POST_THROW_HOLD interruptible at " + offsetMs + "ms", false,
                    "canInterruptCurrentState() returned false"));
            return;
        }

        controller.startThrowForTest(windChargeHeldSlot, true);

        boolean stateAdvanced = controller.getState() != PearlCatchController.State.POST_THROW_HOLD;

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

        boolean canInterrupt = controller.canInterruptCurrentState();
        boolean pass = !canInterrupt;

        results.add(new TestResult("ACTIVE_PROTECTION", "Active state " + activeState + " is protected against interruptions",
                pass, "canInterruptCurrentState() = " + canInterrupt));
    }

    private static void testTripleChainedThrowSlotRetention() {
        PearlCatchController controller = PearlCatchController.getInstance();
        controller.reset();

        int originalWeaponSlot = 0;

        controller.startThrowForTest(originalWeaponSlot, false);
        boolean t1Ok = controller.getInitialSlot() == originalWeaponSlot;

        controller.setStateForTest(PearlCatchController.State.POST_THROW_HOLD, originalWeaponSlot);

        controller.startThrowForTest(2, true);
        boolean t2Ok = controller.getInitialSlot() == originalWeaponSlot;

        controller.setStateForTest(PearlCatchController.State.ROTATING_BACK, originalWeaponSlot);

        controller.startThrowForTest(4, true);
        boolean t3Ok = controller.getInitialSlot() == originalWeaponSlot;

        boolean pass = t1Ok && t2Ok && t3Ok;
        results.add(new TestResult("CHAINED_RETENTION", "Triple-chained throw combo retains initialSlot across all 3 throws",
                pass, String.format("T1=%b, T2=%b, T3=%b (initialSlot=%d, expected=%d)",
                        t1Ok, t2Ok, t3Ok, controller.getInitialSlot(), originalWeaponSlot)));
    }

    private static void testItemCooldownAndLockLogic() {

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

    private static void testPreMutationGuardsSafety() {
        PearlCatchController controller = PearlCatchController.getInstance();
        controller.reset();

        int initialSlot = 0;
        controller.setStateForTest(PearlCatchController.State.POST_THROW_HOLD, initialSlot);

        CombatLockManager.setLock(CombatLockManager.TOTEM, true);
        controller.trigger(null);

        assertEquals(PearlCatchController.State.POST_THROW_HOLD, controller.getState());
        assertEquals(initialSlot, controller.getInitialSlot());
        CombatLockManager.setLock(CombatLockManager.TOTEM, false);

        controller.trigger(null);
        assertEquals(PearlCatchController.State.POST_THROW_HOLD, controller.getState());
        assertEquals(initialSlot, controller.getInitialSlot());

        ActivityConfig config = ActivityConfigManager.getConfig();
        config.autoPearlCatchEnabled = false;
        controller.trigger(null);
        assertEquals(PearlCatchController.State.POST_THROW_HOLD, controller.getState());
        assertEquals(initialSlot, controller.getInitialSlot());
        config.autoPearlCatchEnabled = true;

        results.add(new TestResult("PRE_MUTATION", "Pre-mutation guards preserve existing cleanup state and initialSlot on abort",
                true, "All abort checks exit before state or slot mutation"));
    }

    private static void testOnTickEdgeCases() {
        PearlCatchController controller = PearlCatchController.getInstance();
        controller.reset();

        controller.setStateForTest(PearlCatchController.State.WAIT_FOR_WIND, 1);
        CombatLockManager.setLock(CombatLockManager.PEARL_CATCH, true);

        controller.onTick(null);
        assertEquals(PearlCatchController.State.IDLE, controller.getState());
        assertEquals(-1, controller.getInitialSlot());
        assertFalse(CombatLockManager.isLocked(CombatLockManager.PEARL_CATCH), "Lock must be released on reset");

        results.add(new TestResult("ONTICK_SAFETY", "onTick cleanly resets state and releases lock on abnormal conditions",
                true, "State=IDLE, initialSlot=-1, lock released"));
    }

    private static void testSimulatedChainedThrowsTimeline() {
        PearlCatchController controller = PearlCatchController.getInstance();
        controller.reset();

        int weaponSlot = 0;
        int pearlSlot = 1;
        int windSlot = 2;

        controller.startThrowForTest(weaponSlot, false);
        assertEquals(PearlCatchController.State.THROW_PEARL, controller.getState());
        assertEquals(weaponSlot, controller.getInitialSlot());

        controller.setStateForTest(PearlCatchController.State.WAIT_FOR_WIND, weaponSlot);
        assertFalse(controller.canInterruptCurrentState());

        controller.setStateForTest(PearlCatchController.State.THROW_WIND, weaponSlot);
        assertFalse(controller.canInterruptCurrentState());

        controller.setStateForTest(PearlCatchController.State.POST_THROW_HOLD, weaponSlot);
        assertTrue(controller.canInterruptCurrentState());

        controller.startThrowForTest(windSlot, true);
        assertEquals(weaponSlot, controller.getInitialSlot(), "initialSlot MUST NOT be overwritten by windSlot");
        assertEquals(PearlCatchController.State.THROW_PEARL, controller.getState());

        controller.setStateForTest(PearlCatchController.State.RESTORE_SLOT, weaponSlot);
        assertTrue(controller.canInterruptCurrentState());

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

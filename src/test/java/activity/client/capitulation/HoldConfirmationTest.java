package activity.client.capitulation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HoldConfirmationTest {
    @Test void shortClickAndReleaseCannotDeactivate() {
        var hold = new HoldConfirmation(2000);
        hold.begin(100);
        assertFalse(hold.update(2099, true));
        hold.cancel();
        assertFalse(hold.update(5000, true));
    }
    @Test void losingFocusOrLeavingButtonResetsTheEntireHold() {
        var hold = new HoldConfirmation(2000);
        hold.begin(100);
        assertFalse(hold.update(1800, false));
        assertEquals(0, hold.progress(2000));
        hold.begin(2000);
        assertFalse(hold.update(3999, true));
        assertTrue(hold.update(4000, true));
    }
    @Test void triggersOnlyOnceAndRepeatedPressDoesNotRestartTimer() {
        var hold = new HoldConfirmation(2000);
        hold.begin(100);
        hold.begin(1000);
        assertTrue(hold.update(2100, true));
        assertFalse(hold.update(2200, true));
    }
    @Test void inactiveOrBackwardClockCannotTrigger() {
        var hold = new HoldConfirmation(2000);
        assertFalse(hold.update(10000, true));
        hold.begin(2000);
        assertFalse(hold.update(1000, true));
        assertEquals(0, hold.progress(1000));
    }
}

package activity.client.gui.component;

import activity.client.gui.layout.WindowLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class WindowControlButtonsTest {

    private AtomicBoolean toggleAllCalled;
    private AtomicBoolean refreshCalled;
    private AtomicBoolean maximizeCalled;
    private AtomicBoolean closeCalled;
    private WindowControlButtons buttons;
    private WindowLayout layout;

    @BeforeEach
    void setUp() {
        toggleAllCalled = new AtomicBoolean(false);
        refreshCalled = new AtomicBoolean(false);
        maximizeCalled = new AtomicBoolean(false);
        closeCalled = new AtomicBoolean(false);

        buttons = new WindowControlButtons(
            () -> toggleAllCalled.set(true),
            () -> refreshCalled.set(true),
            () -> maximizeCalled.set(true),
            () -> closeCalled.set(true),
            () -> false
        );

        layout = WindowLayout.compute(854, 480);
    }

    @Test
    void testDimensionsAndCoordinates() {
        assertEquals(76, WindowControlButtons.TOTAL_WIDTH);
        assertEquals(16, WindowControlButtons.BTN_SIZE);
        assertEquals(4, WindowControlButtons.BTN_GAP);

        int startX = buttons.getStartX(layout);
        int startY = buttons.getStartY(layout);

        assertTrue(startX > layout.headerX);
        assertTrue(startX + WindowControlButtons.TOTAL_WIDTH <= layout.headerX + layout.headerWidth);
        assertTrue(startY >= layout.headerY);
        assertTrue(startY + WindowControlButtons.BTN_SIZE <= layout.headerY + layout.headerHeight);
    }

    @Test
    void testMouseOver() {
        int startX = buttons.getStartX(layout);
        int startY = buttons.getStartY(layout);

        assertTrue(buttons.isMouseOver(startX + 2, startY + 2, layout));
        assertTrue(buttons.isMouseOver(startX + WindowControlButtons.TOTAL_WIDTH - 2, startY + 2, layout));
        assertFalse(buttons.isMouseOver(startX - 10, startY, layout));
        assertFalse(buttons.isMouseOver(startX + WindowControlButtons.TOTAL_WIDTH + 10, startY, layout));
        assertFalse(buttons.isMouseOver(startX, startY - 10, layout));
        assertFalse(buttons.isMouseOver(startX, startY + WindowControlButtons.BTN_SIZE + 10, layout));
    }

    @Test
    void testPressAndReleaseToggleAll() {
        int startX = buttons.getStartX(layout);
        int startY = buttons.getStartY(layout);

        assertTrue(buttons.mouseClicked(startX + 4, startY + 4, 0, layout));
        assertEquals(0, buttons.getPressedButton());
        assertFalse(toggleAllCalled.get(), "Action should not fire until release");

        assertTrue(buttons.mouseReleased(startX + 4, startY + 4, 0, layout));
        assertEquals(-1, buttons.getPressedButton());
        assertTrue(toggleAllCalled.get(), "Toggle all action should fire on mouse release");
    }

    @Test
    void testPressAndReleaseRefresh() {
        int startX = buttons.getStartX(layout);
        int startY = buttons.getStartY(layout);
        int refreshX = startX + WindowControlButtons.BTN_SIZE + WindowControlButtons.BTN_GAP + 2;

        assertTrue(buttons.mouseClicked(refreshX, startY + 4, 0, layout));
        assertEquals(1, buttons.getPressedButton());
        assertFalse(refreshCalled.get(), "Action should not fire until release");

        assertTrue(buttons.mouseReleased(refreshX, startY + 4, 0, layout));
        assertEquals(-1, buttons.getPressedButton());
        assertTrue(refreshCalled.get(), "Refresh action should fire on mouse release");
    }

    @Test
    void testPressAndReleaseMaximize() {
        int startX = buttons.getStartX(layout);
        int startY = buttons.getStartY(layout);
        int maximizeX = startX + (WindowControlButtons.BTN_SIZE + WindowControlButtons.BTN_GAP) * 2 + 2;

        assertTrue(buttons.mouseClicked(maximizeX, startY + 4, 0, layout));
        assertEquals(2, buttons.getPressedButton());
        assertFalse(maximizeCalled.get());

        assertTrue(buttons.mouseReleased(maximizeX, startY + 4, 0, layout));
        assertEquals(-1, buttons.getPressedButton());
        assertTrue(maximizeCalled.get());
    }

    @Test
    void testPressAndReleaseClose() {
        int startX = buttons.getStartX(layout);
        int startY = buttons.getStartY(layout);
        int closeX = startX + (WindowControlButtons.BTN_SIZE + WindowControlButtons.BTN_GAP) * 3 + 2;

        assertTrue(buttons.mouseClicked(closeX, startY + 4, 0, layout));
        assertEquals(3, buttons.getPressedButton());
        assertFalse(closeCalled.get());

        assertTrue(buttons.mouseReleased(closeX, startY + 4, 0, layout));
        assertEquals(-1, buttons.getPressedButton());
        assertTrue(closeCalled.get());
    }

    @Test
    void testDragOffCancellation() {
        int startX = buttons.getStartX(layout);
        int startY = buttons.getStartY(layout);

        assertTrue(buttons.mouseClicked(startX + 4, startY + 4, 0, layout));
        assertEquals(0, buttons.getPressedButton());

        assertFalse(buttons.mouseReleased(startX - 100, startY - 100, 0, layout));
        assertEquals(-1, buttons.getPressedButton(), "Pressed state must be cleared on release even if outside");
        assertFalse(toggleAllCalled.get(), "Action must NOT fire when released outside the button");
    }

    @Test
    void testDirectTrigger() {
        buttons.triggerAction(0);
        assertTrue(toggleAllCalled.get());

        buttons.triggerAction(1);
        assertTrue(refreshCalled.get());

        buttons.triggerAction(2);
        assertTrue(maximizeCalled.get());

        buttons.triggerAction(3);
        assertTrue(closeCalled.get());
    }
}

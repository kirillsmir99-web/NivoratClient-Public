package activity.client.gui.layout;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WindowDragControllerTest {

    private WindowDragController controller;
    private WindowLayout layout;

    @BeforeEach
    void setUp() {
        ActivityConfigManager.resetDefaults();
        controller = new WindowDragController();
        controller.init();
        layout = WindowLayout.compute(854, 480);
    }

    @Test
    void testInitialStateCentered() {
        assertEquals(-1, controller.getWindowX());
        assertEquals(-1, controller.getWindowY());
        assertFalse(controller.isDragging());
    }

    @Test
    void testStartDragInsideHeader() {
        // Click on header bar
        double clickX = layout.headerX + 10;
        double clickY = layout.headerY + 5;

        assertTrue(controller.startDrag(clickX, clickY, layout, false));
        assertTrue(controller.isDragging());
    }

    @Test
    void testStartDragOverControlsBlocked() {
        double clickX = layout.headerX + 10;
        double clickY = layout.headerY + 5;

        assertFalse(controller.startDrag(clickX, clickY, layout, true), "Drag must not start if click is over control buttons");
        assertFalse(controller.isDragging());
    }

    @Test
    void testStartDragOutsideHeader() {
        // Click in content area or outside
        assertFalse(controller.startDrag(layout.contentX + 10, layout.contentY + 50, layout, false));
        assertFalse(controller.isDragging());
    }

    @Test
    void testDragAndClampWithinScreen() {
        double startX = layout.headerX + 20;
        double startY = layout.headerY + 10;
        assertTrue(controller.startDrag(startX, startY, layout, false));

        // Drag 50px right, 30px down
        assertTrue(controller.onDrag(startX + 50, startY + 30, 854, 480, layout.windowWidth, layout.windowHeight));
        assertEquals(layout.windowX + 50, controller.getWindowX());
        assertEquals(layout.windowY + 30, controller.getWindowY());

        // Drag off-screen to the left and top (negative)
        controller.onDrag(startX - 1000, startY - 1000, 854, 480, layout.windowWidth, layout.windowHeight);
        assertEquals(0, controller.getWindowX(), "Window X must clamp to 0 on left boundary");
        assertEquals(0, controller.getWindowY(), "Window Y must clamp to 0 on top boundary");

        // Drag off-screen to the right and bottom (exceeding screen dimensions)
        controller.onDrag(startX + 5000, startY + 5000, 854, 480, layout.windowWidth, layout.windowHeight);
        assertEquals(854 - layout.windowWidth, controller.getWindowX(), "Window X must clamp to screenWidth - windowWidth");
        assertEquals(480 - layout.windowHeight, controller.getWindowY(), "Window Y must clamp to screenHeight - windowHeight");
    }

    @Test
    void testStopDragPersistsPosition() {
        double startX = layout.headerX + 20;
        double startY = layout.headerY + 10;
        controller.startDrag(startX, startY, layout, false);
        controller.onDrag(startX + 40, startY + 20, 854, 480, layout.windowWidth, layout.windowHeight);

        assertTrue(controller.stopDrag());
        assertFalse(controller.isDragging());

        ActivityConfig config = ActivityConfigManager.getConfig();
        assertNotNull(config);
        assertEquals(controller.getWindowX(), config.windowPosX);
        assertEquals(controller.getWindowY(), config.windowPosY);
    }

    @Test
    void testRecenterResetsCoordinates() {
        double startX = layout.headerX + 20;
        double startY = layout.headerY + 10;
        controller.startDrag(startX, startY, layout, false);
        controller.onDrag(startX + 40, startY + 20, 854, 480, layout.windowWidth, layout.windowHeight);
        controller.stopDrag();

        assertTrue(controller.getWindowX() >= 0);
        assertTrue(controller.getWindowY() >= 0);

        controller.recenter();
        assertEquals(-1, controller.getWindowX(), "Window X should reset to -1 (centered)");
        assertEquals(-1, controller.getWindowY(), "Window Y should reset to -1 (centered)");

        ActivityConfig config = ActivityConfigManager.getConfig();
        assertNotNull(config);
        assertEquals(-1, config.windowPosX);
        assertEquals(-1, config.windowPosY);
    }

    @Test
    void testMaximizeAndRestoreState() {
        // Set initial position
        double startX = layout.headerX + 10;
        double startY = layout.headerY + 5;
        controller.startDrag(startX, startY, layout, false);
        controller.onDrag(startX + 30, startY + 20, 854, 480, layout.windowWidth, layout.windowHeight);
        controller.stopDrag();

        int originalX = controller.getWindowX();
        int originalY = controller.getWindowY();
        int originalW = layout.windowWidth;
        int originalH = layout.windowHeight;

        // Maximize
        controller.maximize(layout);
        assertTrue(controller.isMaximized());

        ActivityConfig config = ActivityConfigManager.getConfig();
        assertTrue(config.windowMaximized);
        assertEquals(originalX, controller.getUnmaximizedX());
        assertEquals(originalY, controller.getUnmaximizedY());
        assertEquals(originalW, controller.getUnmaximizedWidth());
        assertEquals(originalH, controller.getUnmaximizedHeight());

        // Restore
        controller.restore();
        assertFalse(controller.isMaximized());
        assertFalse(config.windowMaximized);
        assertEquals(originalX, controller.getWindowX());
        assertEquals(originalY, controller.getWindowY());
        assertEquals(originalW, controller.getWindowWidth());
        assertEquals(originalH, controller.getWindowHeight());
    }

    @Test
    void testToggleMaximize() {
        assertFalse(controller.isMaximized());

        controller.toggleMaximize(layout);
        assertTrue(controller.isMaximized());

        controller.toggleMaximize(layout);
        assertFalse(controller.isMaximized());
    }

    @Test
    void testStartDragWhileMaximizedRestoresWindow() {
        controller.maximize(layout);
        assertTrue(controller.isMaximized());

        // Maximize layout
        WindowLayout maxLayout = WindowLayout.compute(854, 480, -1, -1, -1, -1, true);

        // Start drag on header while maximized
        boolean started = controller.startDrag(maxLayout.headerX + 50, maxLayout.headerY + 5, maxLayout, false);
        assertTrue(started);
        assertTrue(controller.isDragging());
        assertFalse(controller.isMaximized(), "Dragging from maximized state must restore window to unmaximized");
    }

    @Test
    void testClampWindowPositionWhileMaximizedPreservesUnmaximizedPosition() {
        controller.setWindowPosition(300, 100);
        controller.setWindowDimensions(500, 320);
        controller.maximize(layout);
        assertTrue(controller.isMaximized());
        assertEquals(300, controller.getUnmaximizedX());
        assertEquals(100, controller.getUnmaximizedY());

        // While maximized, clampWindowPosition called with maximized bounds (e.g. 826x452 on 854x480)
        controller.clampWindowPosition(854, 480, 826, 452);

        // Unmaximized position must NOT be clamped to 854 - 826 = 28!
        assertEquals(300, controller.getUnmaximizedX());
        assertEquals(100, controller.getUnmaximizedY());
        assertEquals(300, controller.getWindowX());
        assertEquals(100, controller.getWindowY());

        // Restore returns original unmaximized position and dimensions
        controller.restore();
        assertFalse(controller.isMaximized());
        assertEquals(300, controller.getWindowX());
        assertEquals(100, controller.getWindowY());
        assertEquals(500, controller.getWindowWidth());
        assertEquals(320, controller.getWindowHeight());
    }

    @Test
    void testClampWindowPositionScreenResizeWhileMaximized() {
        controller.setWindowPosition(500, 300);
        controller.setWindowDimensions(500, 320);
        controller.maximize(layout);

        // Screen resized smaller to 400x300
        controller.clampWindowPosition(400, 300, 380, 280);

        // Unmaximized dimensions and position must be safely clamped within the new 400x300 screen
        assertTrue(controller.getUnmaximizedWidth() <= 400);
        assertTrue(controller.getUnmaximizedHeight() <= 300);
        assertTrue(controller.getUnmaximizedX() + controller.getUnmaximizedWidth() <= 400);
        assertTrue(controller.getUnmaximizedY() + controller.getUnmaximizedHeight() <= 300);

        controller.restore();
        assertTrue(controller.getWindowX() >= 0);
        assertTrue(controller.getWindowY() >= 0);
        assertTrue(controller.getWindowX() + controller.getWindowWidth() <= 400);
        assertTrue(controller.getWindowY() + controller.getWindowHeight() <= 300);
    }
}

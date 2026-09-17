package activity.client.gui.layout;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

public class WindowLayoutTest {

    @ParameterizedTest(name = "Resolution {0}x{1}")
    @CsvSource({
        // Full HD (1920x1080) at scales 1, 2, 3, 4, 5
        "1920, 1080",
        "960, 540",
        "640, 360",
        "480, 270",
        "384, 216",

        // 720p (1280x720) at scales 1, 2, 3, 4
        "1280, 720",
        "640, 360",
        "426, 240",
        "320, 180",

        // Minecraft default windowed (854x480) at scales 1, 2, 3, 4
        "854, 480",
        "427, 240",
        "284, 160",
        "213, 120",

        // Small / Pojav / Old 4:3 resolutions
        "1024, 768",
        "512, 384",
        "341, 256",
        "256, 192",

        // Extreme bounds
        "160, 100",
        "120, 80",
        "100, 60"
    })
    void testWindowLayoutBoundsAndNoOverflow(int screenWidth, int screenHeight) {
        WindowLayout layout = WindowLayout.compute(screenWidth, screenHeight);

        assertNotNull(layout);
        assertNotNull(layout.sizeClass);

        // Window must not exceed screen dimensions
        assertTrue(layout.windowWidth <= screenWidth,
            "windowWidth (" + layout.windowWidth + ") exceeds screenWidth (" + screenWidth + ")");
        assertTrue(layout.windowHeight <= screenHeight,
            "windowHeight (" + layout.windowHeight + ") exceeds screenHeight (" + screenHeight + ")");

        // Window coordinates must be non-negative and fit within screen
        assertTrue(layout.windowX >= 0, "windowX is negative: " + layout.windowX);
        assertTrue(layout.windowY >= 0, "windowY is negative: " + layout.windowY);
        assertTrue(layout.windowX + layout.windowWidth <= screenWidth,
            "Window extends past screen right edge: " + (layout.windowX + layout.windowWidth) + " > " + screenWidth);
        assertTrue(layout.windowY + layout.windowHeight <= screenHeight,
            "Window extends past screen bottom edge: " + (layout.windowY + layout.windowHeight) + " > " + screenHeight);

        // Content area must be strictly contained inside the window (NO OVERFLOW)
        assertTrue(layout.contentX >= layout.windowX, "contentX is outside window");
        assertTrue(layout.contentY >= layout.windowY, "contentY is outside window");
        assertTrue(layout.contentX + layout.contentWidth <= layout.windowX + layout.windowWidth,
            "Content area overflows window right border: " + (layout.contentX + layout.contentWidth) + " > " + (layout.windowX + layout.windowWidth));
        assertTrue(layout.contentY + layout.contentHeight <= layout.windowY + layout.windowHeight,
            "Content area overflows window bottom border: " + (layout.contentY + layout.contentHeight) + " > " + (layout.windowY + layout.windowHeight));

        // Sidebar must fit inside window
        assertTrue(layout.sidebarX >= layout.windowX);
        assertTrue(layout.sidebarWidth > 0);
        assertTrue(layout.sidebarX + layout.sidebarWidth <= layout.contentX);

        // Header must fit inside window
        assertEquals(layout.windowX, layout.headerX);
        assertEquals(layout.windowY, layout.headerY);
        assertEquals(layout.windowWidth, layout.headerWidth);
        assertTrue(layout.headerHeight > 0);
    }

    @Test
    void testCustomWindowPositionClamping() {
        int screenWidth = 854;
        int screenHeight = 480;

        // Custom position way out of bounds (negative)
        WindowLayout layoutNeg = WindowLayout.compute(screenWidth, screenHeight, -50, -50);
        // Negative coordinates trigger default centering
        assertEquals((screenWidth - layoutNeg.windowWidth) / 2, layoutNeg.windowX);
        assertEquals((screenHeight - layoutNeg.windowHeight) / 2, layoutNeg.windowY);

        // Custom position way out of bounds (too large)
        WindowLayout layoutLarge = WindowLayout.compute(screenWidth, screenHeight, 2000, 2000);
        assertTrue(layoutLarge.windowX + layoutLarge.windowWidth <= screenWidth);
        assertTrue(layoutLarge.windowY + layoutLarge.windowHeight <= screenHeight);
    }

    @Test
    void testDragControllerClampWindowPosition() {
        WindowDragController controller = new WindowDragController();
        WindowLayout layout = WindowLayout.compute(854, 480);

        // Start drag and drag within screen
        assertTrue(controller.startDrag(layout.headerX + 10, layout.headerY + 5, layout, false));
        controller.onDrag(200, 150, 854, 480, layout.windowWidth, layout.windowHeight);
        controller.clampWindowPosition(854, 480, layout.windowWidth, layout.windowHeight);
        assertTrue(controller.getWindowX() >= 0);
        assertTrue(controller.getWindowY() >= 0);
        assertTrue(controller.getWindowX() <= 854 - layout.windowWidth);
        assertTrue(controller.getWindowY() <= 480 - layout.windowHeight);

        // Screen resized smaller (e.g. GUI scale increased)
        controller.clampWindowPosition(427, 240, 300, 200);
        assertTrue(controller.getWindowX() >= 0);
        assertTrue(controller.getWindowY() >= 0);
        assertTrue(controller.getWindowX() <= 427 - 300);
        assertTrue(controller.getWindowY() <= 240 - 200);
    }

    @ParameterizedTest(name = "Screen {0}x{1} Maximize Margins")
    @CsvSource({
        "1920, 1080",
        "1440, 900",
        "1280, 720",
        "960, 540",
        "854, 480",
        "640, 360",
        "480, 270",
        "427, 240",
        "384, 216",
        "320, 180",
        "256, 192",
        "240, 160",
        "213, 120"
    })
    void testMaximizeMarginsStrictlyBetween10And16LogicalPx(int screenWidth, int screenHeight) {
        WindowLayout layout = WindowLayout.compute(screenWidth, screenHeight, -1, -1, -1, -1, true);

        assertNotNull(layout);
        assertTrue(layout.isMaximized, "Layout must be marked as maximized");

        int marginLeft = layout.windowX;
        int marginTop = layout.windowY;
        int marginRight = screenWidth - (layout.windowX + layout.windowWidth);
        int marginBottom = screenHeight - (layout.windowY + layout.windowHeight);

        // All margins must be strictly within 10 to 16 logical pixels
        assertTrue(marginLeft >= 10 && marginLeft <= 16,
            "marginLeft (" + marginLeft + ") outside [10, 16] for " + screenWidth + "x" + screenHeight);
        assertTrue(marginTop >= 10 && marginTop <= 16,
            "marginTop (" + marginTop + ") outside [10, 16] for " + screenWidth + "x" + screenHeight);
        assertTrue(marginRight >= 10 && marginRight <= 16,
            "marginRight (" + marginRight + ") outside [10, 16] for " + screenWidth + "x" + screenHeight);
        assertTrue(marginBottom >= 10 && marginBottom <= 16,
            "marginBottom (" + marginBottom + ") outside [10, 16] for " + screenWidth + "x" + screenHeight);

        // Must NOT be 0 (must not be system fullscreen)
        assertNotEquals(0, marginLeft);
        assertNotEquals(0, marginTop);
        assertNotEquals(0, marginRight);
        assertNotEquals(0, marginBottom);
    }

    @Test
    void testRestoreDimensions() {
        int screenWidth = 960;
        int screenHeight = 540;

        // Custom restored size
        int customW = 550;
        int customH = 340;
        int customX = 100;
        int customY = 80;

        WindowLayout restored = WindowLayout.compute(screenWidth, screenHeight, customX, customY, customW, customH, false);
        assertEquals(customW, restored.windowWidth);
        assertEquals(customH, restored.windowHeight);
        assertEquals(customX, restored.windowX);
        assertEquals(customY, restored.windowY);
        assertFalse(restored.isMaximized);

        // Clamped if screen shrunk
        WindowLayout clamped = WindowLayout.compute(400, 300, customX, customY, customW, customH, false);
        assertTrue(clamped.windowWidth <= 400);
        assertTrue(clamped.windowHeight <= 300);
        assertTrue(clamped.windowX + clamped.windowWidth <= 400);
        assertTrue(clamped.windowY + clamped.windowHeight <= 300);
    }
}

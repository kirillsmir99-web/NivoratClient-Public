package activity.client.gui.touch;

import activity.client.gui.component.ActivityButton;
import activity.client.gui.component.ActivityToggle;
import activity.client.gui.icon.ActivityIcon;
import activity.client.gui.layout.WindowLayout;
import activity.client.gui.theme.ActivityMetrics;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

public class TouchHitboxResponsiveTest {

    @Test
    void testMetricsConstants() {
        assertEquals(24, ActivityMetrics.TOUCH_TARGET_MIN_SIZE);
        assertEquals(28, ActivityMetrics.TOUCH_TARGET_LARGE);
        assertEquals(4, ActivityMetrics.TOUCH_HITBOX_PADDING);
        assertEquals(500, ActivityMetrics.RESPONSIVE_TWO_COLUMN_BREAKPOINT);
        assertEquals(1000, ActivityMetrics.CONTENT_MAX_WIDTH);
    }

    @Test
    void testToggleHitboxPadding() {
        int x = 100;
        int y = 50;
        ActivityToggle toggle = new ActivityToggle(x, y, false, state -> {});

        assertEquals(28, toggle.getWidth());
        assertEquals(14, toggle.getHeight());

        assertTrue(toggle.isMouseOver(x + 5, y + 5));

        assertTrue(toggle.isMouseOver(x - 3, y));
        assertTrue(toggle.isMouseOver(x + 28 + 2, y));
        assertTrue(toggle.isMouseOver(x, y - 5));
        assertTrue(toggle.isMouseOver(x, y + 14 + 4));

        assertFalse(toggle.isMouseOver(x - 5, y));
        assertFalse(toggle.isMouseOver(x + 28 + 5, y));
        assertFalse(toggle.isMouseOver(x, y - 7));
        assertFalse(toggle.isMouseOver(x, y + 14 + 7));
    }

    @Test
    void testButtonTouchPadding() {
        int x = 200;
        int y = 150;
        int size = 16;

        ActivityButton iconBtn = new ActivityButton(
            x, y, size, size,
            ActivityIcon.TRASH,
            Text.empty(),
            ActivityButton.Variant.DANGER,
            b -> {}
        );

        assertEquals(ActivityMetrics.TOUCH_HITBOX_PADDING, iconBtn.getTouchPadding());

        assertTrue(iconBtn.isMouseOver(x + 8, y + 8));

        assertTrue(iconBtn.isMouseOver(x - 4, y));
        assertTrue(iconBtn.isMouseOver(x + size + 3, y));
        assertTrue(iconBtn.isMouseOver(x, y - 4));
        assertTrue(iconBtn.isMouseOver(x, y + size + 3));

        assertFalse(iconBtn.isMouseOver(x - 6, y));
        assertFalse(iconBtn.isMouseOver(x + size + 6, y));
        assertFalse(iconBtn.isMouseOver(x, y - 6));
        assertFalse(iconBtn.isMouseOver(x, y + size + 6));
    }

    @ParameterizedTest(name = "GUI Scale simulation for physical {0}x{1} at scale {2} -> logical {3}x{4}")
    @CsvSource({
        "1920, 1080, 2, 960, 540",
        "1920, 1080, 3, 640, 360",
        "1920, 1080, 4, 480, 270",
        "2560, 1440, 2, 1280, 720",
        "2560, 1440, 3, 853, 480",
        "2560, 1440, 4, 640, 360",
        "1280, 720, 2, 640, 360",
        "1280, 720, 3, 426, 240",
        "1280, 720, 4, 320, 180"
    })
    void testGuiScaleSimulationResponsive(int physW, int physH, int scale, int logW, int logH) {
        WindowLayout layout = WindowLayout.compute(logW, logH);

        assertNotNull(layout);
        assertTrue(layout.windowWidth <= logW);
        assertTrue(layout.windowHeight <= logH);
        assertTrue(layout.windowX >= 0);
        assertTrue(layout.windowY >= 0);
        assertTrue(layout.contentWidth > 0);
        assertTrue(layout.contentHeight > 0);

        int effectiveContentWidth = Math.min(layout.contentWidth - 10, ActivityMetrics.CONTENT_MAX_WIDTH);
        boolean expectTwoColumns = effectiveContentWidth >= ActivityMetrics.RESPONSIVE_TWO_COLUMN_BREAKPOINT;

        if (expectTwoColumns) {
            int colGap = ActivityMetrics.COLUMN_GAP;
            int cardW = (effectiveContentWidth - colGap) / 2;
            assertTrue(cardW >= 240, "Card width in 2-column mode must be at least 240px");
        }
    }

    @Test
    void testHeaderControlButtonsTouchHitbox() {
        WindowLayout compactLayout = WindowLayout.compute(426, 240);
        activity.client.gui.component.WindowControlButtons btns = new activity.client.gui.component.WindowControlButtons(
            () -> {}, () -> {}, () -> {}, () -> false
        );

        int startX = btns.getStartX(compactLayout);
        int startY = btns.getStartY(compactLayout);

        assertTrue(btns.isMouseOver(startX, startY - 4, compactLayout));
        assertTrue(btns.isMouseOver(startX, startY + 16 + 3, compactLayout));

        assertTrue(btns.isMouseOver(startX - 4, startY + 2, compactLayout));
        assertTrue(btns.isMouseOver(startX + activity.client.gui.component.WindowControlButtons.TOTAL_WIDTH + 3, startY + 2, compactLayout));

        assertFalse(btns.isMouseOver(startX - 8, startY + 2, compactLayout));
        assertFalse(btns.isMouseOver(startX + activity.client.gui.component.WindowControlButtons.TOTAL_WIDTH + 8, startY + 2, compactLayout));
        assertFalse(btns.isMouseOver(startX, startY - 8, compactLayout));
        assertFalse(btns.isMouseOver(startX, startY + 16 + 8, compactLayout));
    }

    @Test
    void testSocialButtonsTouchHitbox() {
        int x = 50;
        int y = 80;
        int w = 180;
        int h = ActivityMetrics.CONTROL_HEIGHT;

        ActivityButton socialBtn = new ActivityButton(x, y, w, h, ActivityIcon.TELEGRAM, Text.literal("Telegram"), b -> {});
        socialBtn.setTouchPadding(ActivityMetrics.TOUCH_HITBOX_PADDING);

        assertEquals(4, socialBtn.getTouchPadding());

        assertTrue(socialBtn.isMouseOver(x - 4, y));
        assertTrue(socialBtn.isMouseOver(x + w + 3, y));
        assertTrue(socialBtn.isMouseOver(x + 10, y - 4));
        assertTrue(socialBtn.isMouseOver(x + 10, y + h + 3));

        assertFalse(socialBtn.isMouseOver(x - 6, y));
        assertFalse(socialBtn.isMouseOver(x + w + 6, y));
        assertFalse(socialBtn.isMouseOver(x + 10, y - 6));
        assertFalse(socialBtn.isMouseOver(x + 10, y + h + 6));
    }

    @ParameterizedTest(name = "Maximize margins for {0}x{1} (GUI Scale {2})")
    @CsvSource({
        "960, 540, 2",
        "640, 360, 3",
        "480, 270, 4",
        "320, 180, 4",
        "256, 192, 4",
        "240, 160, 4"
    })
    void testMaximizeAcrossGuiScalesAndSmallWindows(int screenW, int screenH, int scale) {
        WindowLayout layout = WindowLayout.compute(screenW, screenH, -1, -1, -1, -1, true);

        int marginLeft = layout.windowX;
        int marginTop = layout.windowY;
        int marginRight = screenW - (layout.windowX + layout.windowWidth);
        int marginBottom = screenH - (layout.windowY + layout.windowHeight);

        assertTrue(marginLeft >= 10 && marginLeft <= 16, "marginLeft: " + marginLeft);
        assertTrue(marginTop >= 10 && marginTop <= 16, "marginTop: " + marginTop);
        assertTrue(marginRight >= 10 && marginRight <= 16, "marginRight: " + marginRight);
        assertTrue(marginBottom >= 10 && marginBottom <= 16, "marginBottom: " + marginBottom);
    }
}

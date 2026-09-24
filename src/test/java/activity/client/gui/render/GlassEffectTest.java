package activity.client.gui.render;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.component.ActivityPanel;
import activity.client.gui.theme.ActivityColors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GlassEffectTest {

    @BeforeEach
    void setUp() {
        ActivityConfigManager.resetDefaults();
    }

    @Test
    void testScaleAlphaPercentBasic() {
        int solidRed = 0xFFFF0000;
        int halfAlpha = ActivityColors.scaleAlphaPercent(solidRed, 50.0);
        int alphaHalf = (halfAlpha >>> 24) & 0xFF;
        assertEquals(127, alphaHalf, 1, "50% of 255 should be approximately 127");

        int zeroAlpha = ActivityColors.scaleAlphaPercent(solidRed, 0.0);
        assertEquals(0, (zeroAlpha >>> 24) & 0xFF, "0% should yield 0 alpha");

        int fullAlpha = ActivityColors.scaleAlphaPercent(solidRed, 100.0);
        assertEquals(255, (fullAlpha >>> 24) & 0xFF, "100% should preserve 255 alpha");
    }

    @Test
    void testScaleAlphaPercentPanelAndWindowDefaults() {

        int winColor = ActivityColors.WINDOW_BACKGROUND;
        int winAlphaOriginal = (winColor >>> 24) & 0xFF;
        int scaledWin = ActivityColors.scaleAlphaPercent(winColor, 85.0);
        int winAlphaScaled = (scaledWin >>> 24) & 0xFF;
        int expectedWinAlpha = (int) (winAlphaOriginal * 0.85);
        assertEquals(expectedWinAlpha, winAlphaScaled, 1);

        int panelColor = ActivityColors.PANEL_BACKGROUND;
        int panelAlphaOriginal = (panelColor >>> 24) & 0xFF;
        int scaledPanel = ActivityColors.scaleAlphaPercent(panelColor, 65.0);
        int panelAlphaScaled = (scaledPanel >>> 24) & 0xFF;
        int expectedPanelAlpha = (int) (panelAlphaOriginal * 0.65);
        assertEquals(expectedPanelAlpha, panelAlphaScaled, 1);
    }

    @Test
    void testScaleAlphaPercentRGBPreservation() {
        int color = 0x8038BDF8;
        int scaled = ActivityColors.scaleAlphaPercent(color, 80.0);
        assertEquals(0x38BDF8, scaled & 0x00FFFFFF, "RGB channels must be strictly preserved");
    }

    @Test
    void testConfigOpacitySanitizationAndClamping() {
        ActivityConfig config = new ActivityConfig();

        assertEquals(85.0, config.windowOpacity, 0.001);
        assertEquals(65.0, config.panelOpacity, 0.001);
        assertTrue(config.glassEffect);

        config.windowOpacity = 10.0;
        config.panelOpacity = 5.0;
        config.sanitize();
        assertEquals(30.0, config.windowOpacity, 0.001, "Window opacity below 30% must be clamped to 30%");
        assertEquals(20.0, config.panelOpacity, 0.001, "Panel opacity below 20% must be clamped to 20%");

        config.windowOpacity = 150.0;
        config.panelOpacity = 200.0;
        config.sanitize();
        assertEquals(100.0, config.windowOpacity, 0.001, "Window opacity above 100% must be clamped to 100%");
        assertEquals(100.0, config.panelOpacity, 0.001, "Panel opacity above 100% must be clamped to 100%");

        config.windowOpacity = Double.NaN;
        config.panelOpacity = Double.POSITIVE_INFINITY;
        config.sanitize();
        assertEquals(85.0, config.windowOpacity, 0.001, "NaN window opacity must reset to default 85%");
        assertEquals(65.0, config.panelOpacity, 0.001, "Infinite panel opacity must reset to default 65%");
    }

    @Test
    void testConfigCopyAndEqualsWithGlassProperties() {
        ActivityConfig config1 = new ActivityConfig();
        config1.windowOpacity = 70.0;
        config1.panelOpacity = 45.0;
        config1.glassEffect = false;

        ActivityConfig copy = config1.copy();
        assertEquals(config1.windowOpacity, copy.windowOpacity, 0.001);
        assertEquals(config1.panelOpacity, copy.panelOpacity, 0.001);
        assertEquals(config1.glassEffect, copy.glassEffect);
        assertEquals(config1, copy);
        assertEquals(config1.hashCode(), copy.hashCode());

        copy.glassEffect = true;
        assertNotEquals(config1, copy);
    }

    @Test
    void testPanelHighlightAnimationTimer() {
        ActivityPanel panel = new ActivityPanel(10, 10, 100, 50);
        assertFalse(panel.isHighlighted());

        panel.flashHighlight();
        assertTrue(panel.isHighlighted());
    }
}

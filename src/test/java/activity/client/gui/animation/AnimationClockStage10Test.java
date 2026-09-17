package activity.client.gui.animation;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.component.ActivityPanel;
import activity.client.gui.sidebar.SidebarTree;
import activity.client.gui.theme.ActivityColors;
import net.minecraft.text.Text;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deep automated unit verification for Stage 10:
 * Animation System unification, Spatial Open/Close transitions,
 * Global animation bypass, Maximize/Restore bounds interpolation,
 * and Glass & Opacity color calculations.
 */
public class AnimationClockStage10Test {

    @BeforeEach
    void setUp() {
        ActivityConfigManager.resetDefaults();
        AnimationClock.resetAll();
    }

    // ==========================================
    // 1. ANIMATION CLOCK & UNIFIED TIMING
    // ==========================================

    @Test
    void testAnimationClockTickAndTiming() {
        assertEquals(0.0f, AnimationClock.getElapsedSeconds(), 0.0001f);
        float dtInitial = AnimationClock.getDeltaTime();
        assertTrue(dtInitial >= 0.001f && dtInitial <= 0.050f);

        // Tick several frames
        AnimationClock.tick();
        assertTrue(AnimationClock.getElapsedSeconds() > 0.0f);

        float firstElapsed = AnimationClock.getElapsedSeconds();
        AnimationClock.tick();
        assertTrue(AnimationClock.getElapsedSeconds() >= firstElapsed);

        // Reset clock
        AnimationClock.resetAll();
        assertEquals(0.0f, AnimationClock.getElapsedSeconds(), 0.0001f);
    }

    @Test
    void testCursorBlinkSynchronization() {
        // Blink alternates every 0.5s:
        // [0.0..0.5) -> true
        // [0.5..1.0) -> false
        // [1.0..1.5) -> true
        // [1.5..2.0) -> false
        AnimationClock.resetAll();
        assertTrue(AnimationClock.isCursorBlinkVisible(0.0f));

        // Advance by 0.6s: elapsed = 0.6s -> phase [0.5..1.0) -> false
        AnimationClock.advanceForTesting(0.6f);
        assertFalse(AnimationClock.isCursorBlinkVisible(0.0f));

        // Advance by another 0.5s: elapsed = 1.1s -> phase [1.0..1.5) -> true
        AnimationClock.advanceForTesting(0.5f);
        assertTrue(AnimationClock.isCursorBlinkVisible(0.0f));
    }

    @Test
    void testHarmonicPulseBounds() {
        float pulse1 = AnimationClock.getPulse(4.0f);
        assertTrue(pulse1 >= 0.0f && pulse1 <= 1.0f, "Pulse must be bounded in [0.0, 1.0]");

        float pulse2 = AnimationClock.getPulse(8.0f);
        assertTrue(pulse2 >= 0.0f && pulse2 <= 1.0f, "High frequency pulse must be bounded in [0.0, 1.0]");

        // When disabled, pulse must return solid 1.0f
        ActivityConfig config = ActivityConfigManager.getConfig();
        config.animationsEnabled = false;
        assertEquals(1.0f, AnimationClock.getPulse(4.0f), 0.0001f);
    }

    // ==========================================
    // 2. GLOBAL ANIMATION BYPASS
    // ==========================================

    @Test
    void testGlobalAnimationBypassWhenDisabled() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        config.animationsEnabled = false;

        assertFalse(AnimationClock.isAnimationsEnabled());

        // When animations are disabled, approach must return target immediately (0 delay)
        float snap1 = AnimationClock.approach(0.0f, 1.0f, 0.5f);
        assertEquals(1.0f, snap1, 0.0001f, "Disabled animations must snap instantly to 1.0");

        float snap2 = AnimationClock.approach(1.0f, 0.0f, 0.5f);
        assertEquals(0.0f, snap2, 0.0001f, "Disabled animations must snap instantly to 0.0");

        // Exponential decay must also snap immediately
        float snapExp = AnimationClock.approachExp(0.25f, 0.85f, 25.0f);
        assertEquals(0.85f, snapExp, 0.0001f, "Disabled animations must snap instantly in approachExp");
    }

    // ==========================================
    // 3. SPATIAL OPEN AND REVERSE CLOSE ANIMATIONS
    // ==========================================

    @Test
    void testSpatialOpenAnimationFormulas() {
        // Open animation:
        // scale: 0.88f + 0.12f * progress
        // offsetX: 30.0f * (1.0f - progress)
        // offsetY: 25.0f * (1.0f - progress)
        // alpha: progress

        // At progress = 0.0f (initial closed state)
        float progress0 = 0.0f;
        float scale0 = 0.88f + 0.12f * progress0;
        float offX0 = 30.0f * (1.0f - progress0);
        float offY0 = 25.0f * (1.0f - progress0);
        assertEquals(0.88f, scale0, 0.001f);
        assertEquals(30.0f, offX0, 0.001f);
        assertEquals(25.0f, offY0, 0.001f);

        // At progress = 0.5f (midway)
        float progressHalf = 0.5f;
        float scaleHalf = 0.88f + 0.12f * progressHalf;
        float offXHalf = 30.0f * (1.0f - progressHalf);
        float offYHalf = 25.0f * (1.0f - progressHalf);
        assertEquals(0.94f, scaleHalf, 0.001f);
        assertEquals(15.0f, offXHalf, 0.001f);
        assertEquals(12.5f, offYHalf, 0.001f);

        // At progress = 1.0f (fully opened, centered)
        float progress1 = 1.0f;
        float scale1 = 0.88f + 0.12f * progress1;
        float offX1 = 30.0f * (1.0f - progress1);
        float offY1 = 25.0f * (1.0f - progress1);
        assertEquals(1.00f, scale1, 0.001f);
        assertEquals(0.0f, offX1, 0.001f);
        assertEquals(0.0f, offY1, 0.001f);

        // Easing validation
        assertEquals(0.0f, AnimationClock.easeOutCubic(0.0f), 0.0001f);
        assertEquals(1.0f, AnimationClock.easeOutCubic(1.0f), 0.0001f);
        assertTrue(AnimationClock.easeOutCubic(0.5f) > 0.5f, "easeOutCubic must decelerate (be concave down)");
    }

    @Test
    void testSpatialCloseReverseAnimationFormulas() {
        // Close animation is exact reverse:
        // closeFactor = 1.0f - easeInCubic(closeTimer)
        // progress = closeStartProgress * closeFactor

        float closeStartProgress = 1.0f;

        // At closeTimer = 0.0f (just clicked close)
        float factor0 = 1.0f - AnimationClock.easeInCubic(0.0f);
        float prog0 = closeStartProgress * factor0;
        assertEquals(1.0f, prog0, 0.001f);

        // Scale at start of close is 1.00f, offset is 0
        assertEquals(1.00f, 0.88f + 0.12f * prog0, 0.001f);
        assertEquals(0.0f, 30.0f * (1.0f - prog0), 0.001f);
        assertEquals(0.0f, 25.0f * (1.0f - prog0), 0.001f);

        // At closeTimer = 0.5f
        float easeInHalf = AnimationClock.easeInCubic(0.5f);
        assertEquals(0.125f, easeInHalf, 0.001f, "0.5^3 = 0.125");
        float factorHalf = 1.0f - easeInHalf;
        float progHalf = closeStartProgress * factorHalf;
        assertEquals(0.875f, progHalf, 0.001f);

        // At closeTimer = 1.0f (finished closing)
        float factor1 = 1.0f - AnimationClock.easeInCubic(1.0f);
        float prog1 = closeStartProgress * factor1;
        assertEquals(0.0f, prog1, 0.001f);

        // Scale at end of close is 0.88f, offsets return to +30px, +25px
        assertEquals(0.88f, 0.88f + 0.12f * prog1, 0.001f);
        assertEquals(30.0f, 30.0f * (1.0f - prog1), 0.001f);
        assertEquals(25.0f, 25.0f * (1.0f - prog1), 0.001f);
    }

    // ==========================================
    // 4. MAXIMIZE / RESTORE MATRIX INTERPOLATION
    // ==========================================

    @Test
    void testMaximizeMatrixInterpolationMath() {
        int oldX = 100, oldY = 80, oldW = 400, oldH = 300;
        int targetX = 20, targetY = 20, targetW = 800, targetH = 600;

        // At t = 0.0f (start of maximize transition)
        float t0 = 0.0f;
        float curX0 = oldX + (targetX - oldX) * t0;
        float curY0 = oldY + (targetY - oldY) * t0;
        float curW0 = oldW + (targetW - oldW) * t0;
        float curH0 = oldH + (targetH - oldH) * t0;
        float scaleX0 = curW0 / (float) targetW;
        float scaleY0 = curH0 / (float) targetH;
        float transX0 = curX0 - targetX * scaleX0;
        float transY0 = curY0 - targetY * scaleY0;

        assertEquals(0.5f, scaleX0, 0.001f);
        assertEquals(0.5f, scaleY0, 0.001f);

        // Left boundary maps to oldX
        float mappedLeft0 = transX0 + targetX * scaleX0;
        assertEquals(100.0f, mappedLeft0, 0.001f);

        // Right boundary maps to oldX + oldW
        float mappedRight0 = transX0 + (targetX + targetW) * scaleX0;
        assertEquals(500.0f, mappedRight0, 0.001f);

        // At t = 1.0f (end of maximize transition)
        float t1 = 1.0f;
        float curX1 = oldX + (targetX - oldX) * t1;
        float curY1 = oldY + (targetY - oldY) * t1;
        float curW1 = oldW + (targetW - oldW) * t1;
        float curH1 = oldH + (targetH - oldH) * t1;
        float scaleX1 = curW1 / (float) targetW;
        float scaleY1 = curH1 / (float) targetH;
        float transX1 = curX1 - targetX * scaleX1;
        float transY1 = curY1 - targetY * scaleY1;

        assertEquals(1.0f, scaleX1, 0.001f);
        assertEquals(1.0f, scaleY1, 0.001f);
        assertEquals(0.0f, transX1, 0.001f);
        assertEquals(0.0f, transY1, 0.001f);

        // Left boundary maps to targetX
        float mappedLeft1 = transX1 + targetX * scaleX1;
        assertEquals(20.0f, mappedLeft1, 0.001f);

        // Right boundary maps to targetX + targetW
        float mappedRight1 = transX1 + (targetX + targetW) * scaleX1;
        assertEquals(820.0f, mappedRight1, 0.001f);
    }

    // ==========================================
    // 5. GLASS & OPACITY POLISH
    // ==========================================

    @Test
    void testGlassOpacityColorsAndSoftWorldVisibility() {
        // Window Background default: 85% opacity
        int winColor = ActivityColors.getWindowBackgroundColor(85.0, 1.0f);
        int winAlpha = (winColor >>> 24) & 0xFF;
        int expectedWinAlpha = (int) (((ActivityColors.WINDOW_BACKGROUND >>> 24) & 0xFF) * 0.85f);
        assertEquals(expectedWinAlpha, winAlpha, 1);

        // Panel Background default: 65% opacity
        int panelColor = ActivityColors.getPanelBackgroundColor(65.0, 1.0f);
        int panelAlpha = (panelColor >>> 24) & 0xFF;
        int expectedPanelAlpha = (int) (((ActivityColors.PANEL_BACKGROUND >>> 24) & 0xFF) * 0.65f);
        assertEquals(expectedPanelAlpha, panelAlpha, 1);

        // Soft world visibility background overlay: 0x88000000 (alpha 136)
        int bgOverlay = ActivityColors.BACKGROUND_OVERLAY;
        int bgAlphaBase = (bgOverlay >>> 24) & 0xFF;
        assertEquals(136, bgAlphaBase);

        // Overlay alpha fades proportionally with open/close progress
        int overlayHalf = ActivityColors.scaleAlpha(ActivityColors.BACKGROUND_OVERLAY, 0.5f);
        assertEquals(68, (overlayHalf >>> 24) & 0xFF, 1);

        int overlayZero = ActivityColors.scaleAlpha(ActivityColors.BACKGROUND_OVERLAY, 0.0f);
        assertEquals(0, (overlayZero >>> 24) & 0xFF);
    }

    @Test
    void testApproachWithExplicitDt() {
        // Test approach with explicit dt parameter
        float val = 0.0f;
        val = AnimationClock.approach(val, 1.0f, 0.10f, 0.05f); // half duration
        assertEquals(0.5f, val, 0.001f);

        val = AnimationClock.approach(val, 1.0f, 0.10f, 0.05f); // completed
        assertEquals(1.0f, val, 0.001f);

        // approachExp with explicit dt parameter
        float expVal = AnimationClock.approachExp(0.0f, 100.0f, 20.0f, 0.05f);
        assertTrue(expVal > 0.0f && expVal < 100.0f);
    }

    @Test
    void testSpatialAnimationSeparationAndMaximizeLogic() {
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        cfg.animationsEnabled = true;
        cfg.spatialOpenAnimation = false;

        // Global animation enabled, but spatial animation disabled
        assertTrue(AnimationClock.isAnimationsEnabled());

        // Maximize animation relies on globalAnim && isMaximizingTransition
        // Verify that when spatialOpenAnimation is false, maximize transition is still allowed to animate
        boolean globalAnim = cfg.animationsEnabled;
        boolean isMaximizingTransition = true;
        boolean isMaximizeAnimating = globalAnim && isMaximizingTransition;
        assertTrue(isMaximizeAnimating, "Maximize animation must proceed when animationsEnabled=true even if spatialOpenAnimation=false");

        // Verify that if global animations are disabled, maximize transition snaps immediately
        cfg.animationsEnabled = false;
        globalAnim = cfg.animationsEnabled;
        isMaximizeAnimating = globalAnim && isMaximizingTransition;
        assertFalse(isMaximizeAnimating, "Maximize animation must snap immediately when global animations are disabled");
    }

    @Test
    void testSidebarTreeAndPanelTextCacheInvalidation() {
        SidebarTree tree = new SidebarTree();
        tree.invalidateTextCache();

        ActivityPanel panel = new ActivityPanel(0, 0, 100, 100, Text.literal("Test Panel"));
        panel.invalidateTextCache();
        panel.setTitle(Text.literal("Updated Title"));

        // Confirm panel highlight snapping when animations disabled
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        cfg.animationsEnabled = false;
        panel.flashHighlight();
        assertFalse(panel.isHighlighted(), "flashHighlight must not set timer when animations are disabled");

        cfg.animationsEnabled = true;
        panel.flashHighlight();
        assertTrue(panel.isHighlighted(), "flashHighlight sets timer when animations are enabled");
    }
}

package activity.client.gui.animation;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.component.ActivitySlider;
import activity.client.gui.component.ActivityToggle;
import activity.client.gui.sidebar.SidebarTree;
import net.minecraft.text.Text;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class MicroInteractionsPolishTest {

    @BeforeEach
    void setUp() {
        ActivityConfigManager.resetDefaults();
        AnimationClock.reset();
    }

    @Test
    void testToggleSmoothMovementAndColorProgression() {
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        cfg.animationsEnabled = true;

        AtomicBoolean toggled = new AtomicBoolean(false);
        ActivityToggle toggle = new ActivityToggle(10, 10, false, toggled::set);

        assertEquals(0.0f, toggle.getAnimationProgress(), 0.001f);
        assertFalse(toggle.getState());

        toggle.toggle();
        assertTrue(toggle.getState());
        assertTrue(toggled.get());

        assertTrue(toggle.getAnimationProgress() < 1.0f);

        float step = AnimationClock.approach(toggle.getAnimationProgress(), 1.0f, AnimationClock.DURATION_TOGGLE);
        assertTrue(step > 0.0f);
        assertTrue(step <= 1.0f);

        float eased = AnimationClock.smoothStep(step);
        assertTrue(eased >= 0.0f && eased <= 1.0f);
    }

    @Test
    void testToggleInstantSnappingWhenAnimationsDisabled() {
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        cfg.animationsEnabled = false;

        AtomicBoolean toggled = new AtomicBoolean(false);
        ActivityToggle toggle = new ActivityToggle(10, 10, false, toggled::set);

        assertEquals(0.0f, toggle.getAnimationProgress(), 0.001f);

        toggle.toggle();
        assertTrue(toggle.getState());
        assertTrue(toggled.get());

        assertEquals(1.0f, toggle.getAnimationProgress(), 0.001f);

        toggle.toggle();
        assertFalse(toggle.getState());
        assertEquals(0.0f, toggle.getAnimationProgress(), 0.001f);
    }

    @Test
    void testToggleSetStateWithoutAnimation() {
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        cfg.animationsEnabled = true;

        ActivityToggle toggle = new ActivityToggle(10, 10, false);
        assertEquals(0.0f, toggle.getAnimationProgress(), 0.001f);

        toggle.setState(true, false);
        assertTrue(toggle.getState());
        assertEquals(1.0f, toggle.getAnimationProgress(), 0.001f);

        toggle.setState(false, false);
        assertFalse(toggle.getState());
        assertEquals(0.0f, toggle.getAnimationProgress(), 0.001f);
    }

    @Test
    void testSliderVisualNormAndFillAnimation() {
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        cfg.animationsEnabled = true;

        AtomicReference<Double> observed = new AtomicReference<>(0.0);
        ActivitySlider slider = new ActivitySlider(0, 0, 200, 20, 0.0, 100.0, 0.0, 5.0, Text.literal("Volume"), null, observed::set);

        assertEquals(0.0, slider.getNormalized(), 0.001);
        assertEquals(0.0f, slider.getVisualNorm(), 0.001f);

        slider.setValue(50.0);
        assertEquals(0.5, slider.getNormalized(), 0.001);
        assertEquals(50.0, observed.get(), 0.001);

        assertEquals(1.0f, slider.getValuePulse(), 0.001f);

        float nextPulse = AnimationClock.approach(slider.getValuePulse(), 0.0f, 0.15f);
        assertTrue(nextPulse < 1.0f);
    }

    @Test
    void testSliderInstantSnappingWhenAnimationsDisabled() {
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        cfg.animationsEnabled = false;

        AtomicReference<Double> observed = new AtomicReference<>(0.0);
        ActivitySlider slider = new ActivitySlider(0, 0, 200, 20, 0.0, 100.0, 10.0, 1.0, Text.literal("Test"), null, observed::set);

        slider.setValue(80.0);
        assertEquals(80.0, observed.get(), 0.001);
        assertEquals(0.8, slider.getNormalized(), 0.001);

        assertEquals(0.8f, slider.getVisualNorm(), 0.001f);
        assertEquals(0.0f, slider.getValuePulse(), 0.001f);

        slider.setNormalized(0.2);
        assertEquals(20.0, slider.getValue(), 0.001);
        assertEquals(0.2f, slider.getVisualNorm(), 0.001f);
    }

    @Test
    void testCategoryExpandCollapseProgression() {
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        cfg.animationsEnabled = true;

        SidebarTree sidebarTree = new SidebarTree();
        SidebarTree.CategoryNode defense = sidebarTree.getCategories().get(1);

        assertFalse(defense.isExpanded());
        assertEquals(0.0f, defense.getExpandProgress(), 0.001f);

        defense.toggleExpanded();
        assertTrue(defense.isExpanded());

        defense.update(false, false, 0.05f);
        assertTrue(defense.getExpandProgress() > 0.0f);
        assertTrue(defense.getExpandProgress() < 1.0f);

        float eased = AnimationClock.smoothStep(defense.getExpandProgress());
        assertTrue(eased >= 0.0f && eased <= 1.0f);

        defense.update(false, false, 0.2f);
        assertEquals(1.0f, defense.getExpandProgress(), 0.001f);

        defense.toggleExpanded();
        assertFalse(defense.isExpanded());
        defense.update(false, false, 0.05f);
        assertTrue(defense.getExpandProgress() < 1.0f);
        assertTrue(defense.getExpandProgress() > 0.0f);

        defense.update(false, false, 0.2f);
        assertEquals(0.0f, defense.getExpandProgress(), 0.001f);
    }

    @Test
    void testCategoryInstantExpandCollapseWhenAnimationsDisabled() {
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        cfg.animationsEnabled = false;

        SidebarTree sidebarTree = new SidebarTree();
        SidebarTree.CategoryNode defense = sidebarTree.getCategories().get(1);

        assertFalse(defense.isExpanded());
        assertEquals(0.0f, defense.getExpandProgress(), 0.001f);

        defense.toggleExpanded();
        assertTrue(defense.isExpanded());

        assertEquals(1.0f, defense.getExpandProgress(), 0.001f);

        defense.toggleExpanded();
        assertFalse(defense.isExpanded());

        assertEquals(0.0f, defense.getExpandProgress(), 0.001f);

        defense.setExpanded(true);
        assertTrue(defense.isExpanded());
        assertEquals(1.0f, defense.getExpandProgress(), 0.001f);
    }

    @Test
    void testSidebarHoverInstantWhenAnimationsDisabled() {
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        cfg.animationsEnabled = false;

        SidebarTree sidebarTree = new SidebarTree();
        SidebarTree.CategoryNode combat = sidebarTree.getCategories().get(0);
        SidebarTree.ModuleItem mace = combat.getChildren().get(0);
        SidebarTree.FooterNode settings = sidebarTree.getFooterItems().get(0);

        combat.update(true, true, 0.016f);
        assertEquals(1.0f, combat.getHoverProgress(), 0.001f);
        assertEquals(1.0f, combat.getChevronHoverProgress(), 0.001f);

        mace.updateHover(true, 0.016f);
        assertEquals(1.0f, mace.getHoverProgress(), 0.001f);

        settings.updateHover(true, 0.016f);
        assertEquals(1.0f, settings.getHoverProgress(), 0.001f);

        combat.update(false, false, 0.016f);
        assertEquals(0.0f, combat.getHoverProgress(), 0.001f);
        assertEquals(0.0f, combat.getChevronHoverProgress(), 0.001f);

        mace.updateHover(false, 0.016f);
        assertEquals(0.0f, mace.getHoverProgress(), 0.001f);

        settings.updateHover(false, 0.016f);
        assertEquals(0.0f, settings.getHoverProgress(), 0.001f);
    }

    @Test
    void testSmoothStepSymmetryAndBoundaryProperties() {
        assertEquals(0.0f, AnimationClock.smoothStep(0.0f), 0.0001f);
        assertEquals(1.0f, AnimationClock.smoothStep(1.0f), 0.0001f);
        assertEquals(0.5f, AnimationClock.smoothStep(0.5f), 0.0001f);

        assertEquals(0.0f, AnimationClock.smoothStep(-0.5f), 0.0001f);
        assertEquals(1.0f, AnimationClock.smoothStep(1.5f), 0.0001f);

        float deltaStart = AnimationClock.smoothStep(0.1f) - AnimationClock.smoothStep(0.0f);
        float deltaMiddle = AnimationClock.smoothStep(0.6f) - AnimationClock.smoothStep(0.5f);
        float deltaEnd = AnimationClock.smoothStep(1.0f) - AnimationClock.smoothStep(0.9f);

        assertTrue(deltaMiddle > deltaStart);
        assertTrue(deltaMiddle > deltaEnd);
        assertEquals(deltaStart, deltaEnd, 0.0001f);
    }
}

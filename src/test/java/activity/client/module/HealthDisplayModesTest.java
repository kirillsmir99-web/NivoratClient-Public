package activity.client.module;

import dev.hpreaper.HealthHudOverlay;
import org.junit.jupiter.api.Test;
import static dev.hpreaper.HealthHudOverlay.DisplayMode.*;
import static org.junit.jupiter.api.Assertions.*;

class HealthDisplayModesTest {
    @Test void disabledHasNoValuesOrPreview() {
        assertFalse(HealthHudOverlay.showsOwn(DISABLED));
        assertFalse(HealthHudOverlay.showsTarget(DISABLED));
        assertFalse(HealthHudOverlay.showsDifference(DISABLED, true));
        assertEquals(0, HealthHudOverlay.getPreviewWidth(null, DISABLED));
        assertEquals(0, HealthHudOverlay.getPreviewHeight(DISABLED));
    }
    @Test void ownModeOnlyShowsOwnHealth() {
        assertTrue(HealthHudOverlay.showsOwn(OWN_HEALTH));
        assertFalse(HealthHudOverlay.showsTarget(OWN_HEALTH));
        assertFalse(HealthHudOverlay.showsDifference(OWN_HEALTH, true));
    }
    @Test void targetModeOnlyShowsTarget() {
        assertFalse(HealthHudOverlay.showsOwn(TARGET_HEALTH));
        assertTrue(HealthHudOverlay.showsTarget(TARGET_HEALTH));
        assertFalse(HealthHudOverlay.showsDifference(TARGET_HEALTH, true));
    }
    @Test void combinedModeDoesNotInheritDifferenceToggle() {
        assertTrue(HealthHudOverlay.showsOwn(CROSSHAIR_AND_TARGET));
        assertTrue(HealthHudOverlay.showsTarget(CROSSHAIR_AND_TARGET));
        assertFalse(HealthHudOverlay.showsDifference(CROSSHAIR_AND_TARGET, true));
    }
    @Test void differenceOnlyAppliesToItsOwnMode() {
        assertTrue(HealthHudOverlay.showsOwn(OWN_TARGET_AND_DIFFERENCE));
        assertTrue(HealthHudOverlay.showsTarget(OWN_TARGET_AND_DIFFERENCE));
        assertTrue(HealthHudOverlay.showsDifference(OWN_TARGET_AND_DIFFERENCE, true));
        assertFalse(HealthHudOverlay.showsDifference(OWN_TARGET_AND_DIFFERENCE, false));
    }
}

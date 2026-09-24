package activity.client.gui.icon;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.sound.ActivitySoundEvents;
import activity.client.gui.sound.SoundManager;
import activity.client.gui.sound.SoundProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class IconAndSoundSystemTest {

    private static final List<String> ALL_27_SEMANTIC_IDS = List.of(
        "SEARCH", "REFRESH", "MAXIMIZE", "RESTORE", "CLOSE", "TRASH", "PIN", "INFO",
        "SETTINGS", "PROFILE", "SAVE", "COPY", "IMPORT", "RESET", "SOUND", "FONT",
        "ANIMATION", "GLASS", "KEYBIND", "TELEGRAM", "YOUTUBE", "TIKTOK", "DISCORD",
        "DONATE", "EXTERNAL", "CHECK", "WARNING"
    );

    @BeforeEach
    void setUp() {
        ActivityConfigManager.resetDefaults();
        SoundManager.resetSliderTracking();
        SoundManager.resetHoverTracking();
    }

    @Test
    void testAll27SemanticIconsResolvedCaseInsensitive() {
        for (String id : ALL_27_SEMANTIC_IDS) {

            ActivityIcon iconUpper = ActivityIconManager.getIcon(id);
            assertNotNull(iconUpper, "Icon must resolve for uppercase: " + id);
            if (!"INFO".equalsIgnoreCase(id)) {
                assertNotEquals(ActivityIcon.INFO, iconUpper, "Semantic icon must not fallback to INFO: " + id);
            }

            ActivityIcon iconLower = ActivityIconManager.getIcon(id.toLowerCase());
            assertSame(iconUpper, iconLower, "Lowercase and uppercase must resolve to same icon: " + id);
        }
    }

    @Test
    void testSemanticIconAtlasUVCoordinates() {
        for (String id : ALL_27_SEMANTIC_IDS) {
            ActivityIcon icon = ActivityIconManager.getIcon(id);
            assertTrue(icon.hasAtlasRegion(), "Semantic icon must have valid atlas UV region: " + id);
            assertEquals(24, icon.getAtlasRegionW(), "Cell width must be 24: " + id);
            assertEquals(24, icon.getAtlasRegionH(), "Cell height must be 24: " + id);
            assertTrue(icon.getAtlasU() >= 0 && icon.getAtlasU() < ActivityIconRenderer.ATLAS_WIDTH,
                "Atlas U must be within atlas width (192): " + icon.getAtlasU());
            assertTrue(icon.getAtlasV() >= 0 && icon.getAtlasV() < ActivityIconRenderer.ATLAS_HEIGHT,
                "Atlas V must be within atlas height (96): " + icon.getAtlasV());
        }
    }

    @Test
    void testAtlasConstantIsZeroAllocations() {
        assertNotNull(ActivityIconRenderer.ATLAS_ID);
        assertEquals("nivoratclient", ActivityIconRenderer.ATLAS_ID.getNamespace());
        assertEquals("textures/gui/nivorat_icons_atlas.png", ActivityIconRenderer.ATLAS_ID.getPath());
        assertEquals(192, ActivityIconRenderer.ATLAS_WIDTH);
        assertEquals(120, ActivityIconRenderer.ATLAS_HEIGHT);
    }

    @Test
    void testIconColorResolutionAndAlpha() {

        int colorDisabled = ActivityIconRenderer.resolveColor(0xFFFFFFFF, false, false, true);
        assertEquals(activity.client.gui.theme.ActivityColors.TEXT_DISABLED, colorDisabled);

        int colorSelected = ActivityIconRenderer.resolveColor(0xFFFFFFFF, false, true, false);
        assertEquals(activity.client.gui.theme.ActivityColors.ACCENT_PRIMARY, colorSelected);

        int colorHover = ActivityIconRenderer.resolveColor(0xFFFFFFFF, true, false, false);
        assertEquals(activity.client.gui.theme.ActivityColors.TEXT_PRIMARY, colorHover);

        int full = 0xFFFFFFFF;
        int half = ActivityIconRenderer.applyAlpha(full, 0.5f);
        assertEquals(0x80, (half >>> 24) & 0xFF);

        int zero = ActivityIconRenderer.applyAlpha(full, 0.0f);
        assertEquals(0, zero);
    }

    @Test
    void testSoundProfileEnumAndDefaults() {
        assertEquals(SoundProfile.SERENE, SoundProfile.fromId("serene"));
        assertEquals(SoundProfile.CLASSIC, SoundProfile.fromId("classic"));
        assertEquals(SoundProfile.MINECRAFT, SoundProfile.fromId("minecraft"));

        assertEquals(SoundProfile.SERENE, SoundProfile.fromId(null));
        assertEquals(SoundProfile.SERENE, SoundProfile.fromId("unknown_profile"));

        ActivityConfig config = ActivityConfigManager.getConfig();
        assertNotNull(config);
        assertEquals("serene", config.soundProfile);
        assertEquals(SoundProfile.SERENE, SoundManager.getSoundProfile());
    }

    @Test
    void testAll27SereneEventsRegistered() {
        assertNotNull(ActivitySoundEvents.SERENE_OPEN);
        assertNotNull(ActivitySoundEvents.SERENE_CLOSE);
        assertNotNull(ActivitySoundEvents.SERENE_BUTTON_PRIMARY);
        assertNotNull(ActivitySoundEvents.SERENE_BUTTON_SECONDARY);
        assertNotNull(ActivitySoundEvents.SERENE_HOVER);
        assertNotNull(ActivitySoundEvents.SERENE_TOGGLE_ON);
        assertNotNull(ActivitySoundEvents.SERENE_TOGGLE_OFF);
        assertNotNull(ActivitySoundEvents.SERENE_DROPDOWN_OPEN);
        assertNotNull(ActivitySoundEvents.SERENE_DROPDOWN_CLOSE);
        assertNotNull(ActivitySoundEvents.SERENE_CATEGORY_EXPAND);
        assertNotNull(ActivitySoundEvents.SERENE_CATEGORY_COLLAPSE);
        assertNotNull(ActivitySoundEvents.SERENE_MODAL_OPEN);
        assertNotNull(ActivitySoundEvents.SERENE_MODAL_CLOSE);
        assertNotNull(ActivitySoundEvents.SERENE_SUCCESS);
        assertNotNull(ActivitySoundEvents.SERENE_WARNING);
        assertNotNull(ActivitySoundEvents.SERENE_ERROR);
        assertNotNull(ActivitySoundEvents.SERENE_SLIDER_TICK);
        assertNotNull(ActivitySoundEvents.SERENE_PIN);
        assertNotNull(ActivitySoundEvents.SERENE_UNPIN);
        assertNotNull(ActivitySoundEvents.SERENE_COPY);
        assertNotNull(ActivitySoundEvents.SERENE_IMPORT);
        assertNotNull(ActivitySoundEvents.SERENE_DELETE);
        assertNotNull(ActivitySoundEvents.SERENE_LINK_OPEN);
        assertNotNull(ActivitySoundEvents.SERENE_RELOAD);
        assertNotNull(ActivitySoundEvents.SERENE_MAXIMIZE);
        assertNotNull(ActivitySoundEvents.SERENE_RESTORE);
        assertNotNull(ActivitySoundEvents.SERENE_SEARCH_FOCUS);
    }

    @Test
    void testStrictSliderQuantizationRule() throws InterruptedException {

        SoundManager.resetSliderTracking();
        assertEquals(Long.MIN_VALUE, SoundManager.getLastSliderNotch());

        SoundManager.playSliderTick(50.0, 0.0, 100.0, 1.0);
        assertEquals(50L, SoundManager.getLastSliderNotch(), "First tick must update notch to 50");

        Thread.sleep(40);
        long notchBefore = SoundManager.getLastSliderNotch();
        SoundManager.playSliderTick(50.2, 0.0, 100.0, 1.0);
        assertEquals(notchBefore, SoundManager.getLastSliderNotch(), "Identical notch (50 -> 50) must not trigger new tick!");

        Thread.sleep(40);
        SoundManager.playSliderTick(55.0, 0.0, 100.0, 5.0);
        assertEquals(11L, SoundManager.getLastSliderNotch(), "Quantized notch changed (55 / 5 = 11), tick must fire");

        Thread.sleep(40);
        long notch55 = SoundManager.getLastSliderNotch();
        SoundManager.playSliderTick(55.0, 0.0, 100.0, 5.0);
        assertEquals(notch55, SoundManager.getLastSliderNotch(), "55 -> 55 must produce NO sound");
    }

    @Test
    void testHoverDebounceRateLimiting() throws InterruptedException {
        SoundManager.resetHoverTracking();
        assertEquals(0L, SoundManager.getLastHoverTime());

        SoundManager.playHover();
        long t1 = SoundManager.getLastHoverTime();
        assertTrue(t1 > 0L, "First hover must record timestamp");

        SoundManager.playHover();
        long t2 = SoundManager.getLastHoverTime();
        assertEquals(t1, t2, "Hover within < 50ms must be debounced without updating timestamp");

        Thread.sleep(55);
        SoundManager.playHover();
        long t3 = SoundManager.getLastHoverTime();
        assertTrue(t3 > t1, "Hover after 55ms must update timestamp");
    }

    @Test
    void testConfigSoundProfileSerializationAndSanitize() {
        ActivityConfig config = new ActivityConfig();
        config.resetToDefaults();
        assertEquals("serene", config.soundProfile);

        config.soundProfile = null;
        config.sanitize();
        assertEquals("serene", config.soundProfile);

        config.soundProfile = "invalid_profile";
        config.sanitize();
        assertEquals("serene", config.soundProfile);

        config.soundProfile = "classic";
        config.sanitize();
        assertEquals("classic", config.soundProfile);

        ActivityConfig copy = config.copy();
        assertEquals("classic", copy.soundProfile);
        assertEquals(config, copy);
        assertEquals(config.hashCode(), copy.hashCode());
    }
}

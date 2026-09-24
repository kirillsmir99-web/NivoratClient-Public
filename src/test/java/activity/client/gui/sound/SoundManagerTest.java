package activity.client.gui.sound;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SoundManagerTest {

    @BeforeEach
    void setUp() {
        ActivityConfigManager.resetDefaults();
        SoundManager.setUseCustomSounds(false);
    }

    @AfterEach
    void tearDown() {
        SoundManager.setUseCustomSounds(false);
    }

    @Test
    void testSoundEnabledMasterToggle() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        assertTrue(SoundManager.isSoundEnabled());

        config.soundEnabled = false;
        assertFalse(SoundManager.isSoundEnabled());

        config.soundEnabled = true;
        assertTrue(SoundManager.isSoundEnabled());
    }

    @Test
    void testSliderSoundEnabledToggle() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        assertTrue(SoundManager.isSliderSoundEnabled());

        config.sliderSoundEnabled = false;
        assertFalse(SoundManager.isSliderSoundEnabled());

        config.sliderSoundEnabled = true;
        config.soundEnabled = false;
        assertFalse(SoundManager.isSliderSoundEnabled());
    }

    @Test
    void testVolumeMultiplier() {
        ActivityConfig config = ActivityConfigManager.getConfig();

        config.soundVolume = 100.0;
        assertEquals(1.0f, SoundManager.getVolumeMultiplier(), 0.001f);

        config.soundVolume = 80.0;
        assertEquals(0.8f, SoundManager.getVolumeMultiplier(), 0.001f);

        config.soundVolume = 0.0;
        assertEquals(0.0f, SoundManager.getVolumeMultiplier(), 0.001f);

        config.soundVolume = -15.0;
        assertEquals(0.0f, SoundManager.getVolumeMultiplier(), 0.001f, "Under-bounds volume must clamp to 0.0");

        config.soundVolume = 150.0;
        assertEquals(1.0f, SoundManager.getVolumeMultiplier(), 0.001f, "Over-bounds volume must clamp to 1.0");
    }

    @Test
    void testCustomSoundsFlag() {
        assertFalse(SoundManager.isUseCustomSounds());
        SoundManager.setUseCustomSounds(true);
        assertTrue(SoundManager.isUseCustomSounds());
        SoundManager.setUseCustomSounds(false);
        assertFalse(SoundManager.isUseCustomSounds());
    }

    @Test
    void testHeadlessSoundCallsDoNotThrow() {

        assertDoesNotThrow(() -> {
            SoundManager.playOpen();
            SoundManager.playClose();
            SoundManager.playHover();
            SoundManager.playButtonPrimary();
            SoundManager.playButtonSecondary();
            SoundManager.playClick();
            SoundManager.playToggle(true);
            SoundManager.playToggle(false);
            SoundManager.playDropdownOpen();
            SoundManager.playDropdownClose();
            SoundManager.playCategoryExpand();
            SoundManager.playCategoryCollapse();
            SoundManager.playModalOpen();
            SoundManager.playModalClose();
            SoundManager.playSuccess();
            SoundManager.playWarning();
            SoundManager.playError();
            SoundManager.playPin();
            SoundManager.playUnpin();
            SoundManager.playCopy();
            SoundManager.playImport();
            SoundManager.playDelete();
            SoundManager.playLinkOpen();
            SoundManager.playReload();
            SoundManager.playMaximize();
            SoundManager.playRestore();
            SoundManager.playSearchFocus();
            SoundManager.playTabSwitch();
            SoundManager.playPresetSave();
            SoundManager.playPresetApply();
            SoundManager.playPresetReset();
            SoundManager.playSliderTick(50.0, 0.0, 100.0, 1.0);
            SoundManager.playSliderTick(51.0, 0.0, 100.0, 1.0);
        });

        SoundManager.setUseCustomSounds(true);
        assertDoesNotThrow(() -> {
            SoundManager.playOpen();
            SoundManager.playClose();
            SoundManager.playHover();
            SoundManager.playButtonPrimary();
            SoundManager.playButtonSecondary();
            SoundManager.playClick();
            SoundManager.playToggle(true);
            SoundManager.playToggle(false);
            SoundManager.playDropdownOpen();
            SoundManager.playDropdownClose();
            SoundManager.playCategoryExpand();
            SoundManager.playCategoryCollapse();
            SoundManager.playPresetSave();
            SoundManager.playPresetApply();
            SoundManager.playPresetReset();
            SoundManager.playSliderTick(25.0, 0.0, 100.0, 5.0);
        });
    }

    @Test
    void testSoundIdentifiers() {

        assertEquals("nivoratclient:ui.serene.open", ActivitySoundEvents.SERENE_OPEN_ID.toString());
        assertEquals("nivoratclient:ui.serene.close", ActivitySoundEvents.SERENE_CLOSE_ID.toString());
        assertEquals("nivoratclient:ui.serene.button_primary", ActivitySoundEvents.SERENE_BUTTON_PRIMARY_ID.toString());
        assertEquals("nivoratclient:ui.serene.button_secondary", ActivitySoundEvents.SERENE_BUTTON_SECONDARY_ID.toString());
        assertEquals("nivoratclient:ui.serene.hover", ActivitySoundEvents.SERENE_HOVER_ID.toString());
        assertEquals("nivoratclient:ui.serene.toggle_on", ActivitySoundEvents.SERENE_TOGGLE_ON_ID.toString());
        assertEquals("nivoratclient:ui.serene.toggle_off", ActivitySoundEvents.SERENE_TOGGLE_OFF_ID.toString());
        assertEquals("nivoratclient:ui.serene.dropdown_open", ActivitySoundEvents.SERENE_DROPDOWN_OPEN_ID.toString());
        assertEquals("nivoratclient:ui.serene.dropdown_close", ActivitySoundEvents.SERENE_DROPDOWN_CLOSE_ID.toString());
        assertEquals("nivoratclient:ui.serene.category_expand", ActivitySoundEvents.SERENE_CATEGORY_EXPAND_ID.toString());
        assertEquals("nivoratclient:ui.serene.category_collapse", ActivitySoundEvents.SERENE_CATEGORY_COLLAPSE_ID.toString());
        assertEquals("nivoratclient:ui.serene.slider_tick", ActivitySoundEvents.SERENE_SLIDER_TICK_ID.toString());
        assertEquals("nivoratclient:ui.serene.success", ActivitySoundEvents.SERENE_SUCCESS_ID.toString());
        assertEquals("nivoratclient:ui.serene.warning", ActivitySoundEvents.SERENE_WARNING_ID.toString());
        assertEquals("nivoratclient:ui.serene.error", ActivitySoundEvents.SERENE_ERROR_ID.toString());

        assertEquals("nivoratclient:ui.classic.open", ActivitySoundEvents.CLASSIC_OPEN_ID.toString());
        assertEquals("nivoratclient:ui.classic.close", ActivitySoundEvents.CLASSIC_CLOSE_ID.toString());
        assertEquals("nivoratclient:ui.classic.button", ActivitySoundEvents.CLASSIC_BUTTON_ID.toString());
        assertEquals("nivoratclient:ui.classic.slider_tick", ActivitySoundEvents.CLASSIC_SLIDER_TICK_ID.toString());

        assertEquals(ActivitySoundEvents.SERENE_OPEN_ID, ActivitySoundEvents.MENU_OPEN_ID);
        assertEquals(ActivitySoundEvents.SERENE_CLOSE_ID, ActivitySoundEvents.MENU_CLOSE_ID);
        assertEquals(ActivitySoundEvents.SERENE_HOVER_ID, ActivitySoundEvents.HOVER_ID);
        assertEquals(ActivitySoundEvents.SERENE_BUTTON_PRIMARY_ID, ActivitySoundEvents.SELECT_ID);
        assertEquals(ActivitySoundEvents.SERENE_BUTTON_PRIMARY_ID, ActivitySoundEvents.CLICK_ID);
        assertEquals(ActivitySoundEvents.SERENE_SLIDER_TICK_ID, ActivitySoundEvents.SLIDER_TICK_ID);
    }
}

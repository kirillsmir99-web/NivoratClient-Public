package activity.client.module;

import activity.client.config.ActivityConfig;
import activity.client.gui.component.ActivityDropdown;
import activity.client.gui.component.DropdownPopup;
import activity.client.gui.tab.AboutTab;
import activity.client.module.stub.AutoMaceStub;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class Stage13FixPassTest {

    private AboutTab.UrlOpener originalOpener;

    @BeforeEach
    void setUp() {
        originalOpener = AboutTab.URL_OPENER;
    }

    @AfterEach
    void tearDown() {
        AboutTab.URL_OPENER = originalOpener;
    }

    // =========================================================================
    // 1. AUTO MACE STUB SOURCE MODE VALIDATION [IMP-01]
    // =========================================================================

    @Test
    void testAutoMaceStubAllowedSourceModes() {
        assertEquals("sword_and_axe", AutoMaceStub.DEFAULT_SOURCE_MODE);
        assertTrue(AutoMaceStub.ALLOWED_SOURCE_MODES.contains("sword_and_axe"));
        assertTrue(AutoMaceStub.ALLOWED_SOURCE_MODES.contains("sword_only"));
        assertTrue(AutoMaceStub.ALLOWED_SOURCE_MODES.contains("axe_only"));
        assertEquals(3, AutoMaceStub.ALLOWED_SOURCE_MODES.size());
    }

    @Test
    void testSanitizeSourceMode() {
        assertEquals("sword_and_axe", AutoMaceStub.sanitizeSourceMode("sword_and_axe"));
        assertEquals("sword_only", AutoMaceStub.sanitizeSourceMode("sword_only"));
        assertEquals("axe_only", AutoMaceStub.sanitizeSourceMode("axe_only"));

        // Invalid, null, and empty fallbacks
        assertEquals("sword_and_axe", AutoMaceStub.sanitizeSourceMode(null));
        assertEquals("sword_and_axe", AutoMaceStub.sanitizeSourceMode(""));
        assertEquals("sword_and_axe", AutoMaceStub.sanitizeSourceMode("invalid_mode"));
        assertEquals("sword_and_axe", AutoMaceStub.sanitizeSourceMode("SWORD_ONLY"));
    }

    @Test
    void testAutoMaceLoadFromConfigSanitizesInvalid() {
        AutoMaceStub stub = new AutoMaceStub();
        ActivityConfig config = new ActivityConfig();

        // Valid load
        config.autoMaceSourceMode = "sword_only";
        stub.loadFromConfig(config);
        assertEquals("sword_only", stub.sourceMode);

        config.autoMaceSourceMode = "axe_only";
        stub.loadFromConfig(config);
        assertEquals("axe_only", stub.sourceMode);

        // Invalid value in config -> fallback
        config.autoMaceSourceMode = "corrupted_mode";
        stub.loadFromConfig(config);
        assertEquals("sword_and_axe", stub.sourceMode);

        // Null value in config -> fallback
        config.autoMaceSourceMode = null;
        stub.loadFromConfig(config);
        assertEquals("sword_and_axe", stub.sourceMode);
    }

    @Test
    void testAutoMaceSaveToConfigPreventsInvalidPersistence() {
        AutoMaceStub stub = new AutoMaceStub();
        ActivityConfig config = new ActivityConfig();

        // Directly set invalid sourceMode on stub instance
        stub.sourceMode = "illegal_value";
        stub.saveToConfig(config);

        assertEquals("sword_and_axe", config.autoMaceSourceMode);
        assertEquals("sword_and_axe", stub.sourceMode);

        // Directly set null sourceMode on stub instance
        stub.sourceMode = null;
        stub.saveToConfig(config);

        assertEquals("sword_and_axe", config.autoMaceSourceMode);
        assertEquals("sword_and_axe", stub.sourceMode);

        // Valid sourceMode is preserved
        stub.sourceMode = "axe_only";
        stub.saveToConfig(config);
        assertEquals("axe_only", config.autoMaceSourceMode);
        assertEquals("axe_only", stub.sourceMode);
    }

    // =========================================================================
    // 2. DROPDOWN POPUP UPWARDS FLIP CLAMPING [IMP-02]
    // =========================================================================

    @Test
    void testDropdownPopupConstructionAndClamping() {
        ActivityDropdown<String> dropdown = new ActivityDropdown<>(
            20, 150, 100, 20,
            null,
            List.of("Option A", "Option B", "Option C"),
            "Option A",
            net.minecraft.text.Text::literal,
            val -> {}
        );

        DropdownPopup<String> popup = new DropdownPopup<>(dropdown);
        assertNotNull(popup);
        assertTrue(popup.getRenderHeight() >= 4);
        assertFalse(popup.isClosed());

        // getRenderY should never be negative or exceed screen bounds
        int ry = popup.getRenderY();
        assertTrue(ry >= 0, "Render Y should be non-negative: " + ry);
    }

    // =========================================================================
    // 3. ABOUT TAB FEEDBACK ON OPEN URL FAILURE [POL-02]
    // =========================================================================

    @Test
    void testAboutTabOpenUrlErrorFallbackWithoutScreen() {
        AboutTab.URL_OPENER = url -> {
            throw new IOException("Simulated browser failure");
        };

        // Must not throw, should handle safely, copy to clipboard, and log
        assertDoesNotThrow(() -> AboutTab.openUrl("https://example.com/test", null));
    }
}

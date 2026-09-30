package activity.client.module;

import activity.client.config.ActivityConfig;
import activity.client.gui.component.ActivityDropdown;
import activity.client.gui.component.DropdownPopup;
import activity.client.gui.tab.AboutTab;
import activity.client.module.stub.ParticlePhysicsStub;
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

    @Test
    void testAutoMaceStubAllowedSourceModes() {
        assertEquals("sword_and_axe", ParticlePhysicsStub.DEFAULT_SOURCE_MODE);
        assertTrue(ParticlePhysicsStub.ALLOWED_SOURCE_MODES.contains("sword_and_axe"));
        assertTrue(ParticlePhysicsStub.ALLOWED_SOURCE_MODES.contains("sword_only"));
        assertTrue(ParticlePhysicsStub.ALLOWED_SOURCE_MODES.contains("axe_only"));
        assertEquals(3, ParticlePhysicsStub.ALLOWED_SOURCE_MODES.size());
    }

    @Test
    void testSanitizeSourceMode() {
        assertEquals("sword_and_axe", ParticlePhysicsStub.sanitizeSourceMode("sword_and_axe"));
        assertEquals("sword_only", ParticlePhysicsStub.sanitizeSourceMode("sword_only"));
        assertEquals("axe_only", ParticlePhysicsStub.sanitizeSourceMode("axe_only"));

        assertEquals("sword_and_axe", ParticlePhysicsStub.sanitizeSourceMode(null));
        assertEquals("sword_and_axe", ParticlePhysicsStub.sanitizeSourceMode(""));
        assertEquals("sword_and_axe", ParticlePhysicsStub.sanitizeSourceMode("invalid_mode"));
        assertEquals("sword_and_axe", ParticlePhysicsStub.sanitizeSourceMode("SWORD_ONLY"));
    }

    @Test
    void testAutoMaceLoadFromConfigSanitizesInvalid() {
        ParticlePhysicsStub stub = new ParticlePhysicsStub();
        ActivityConfig config = new ActivityConfig();

        config.autoMaceSourceMode = "sword_only";
        stub.loadFromConfig(config);
        assertEquals("sword_only", stub.sourceMode);

        config.autoMaceSourceMode = "axe_only";
        stub.loadFromConfig(config);
        assertEquals("axe_only", stub.sourceMode);

        config.autoMaceSourceMode = "corrupted_mode";
        stub.loadFromConfig(config);
        assertEquals("sword_and_axe", stub.sourceMode);

        config.autoMaceSourceMode = null;
        stub.loadFromConfig(config);
        assertEquals("sword_and_axe", stub.sourceMode);
    }

    @Test
    void testAutoMaceSaveToConfigPreventsInvalidPersistence() {
        ParticlePhysicsStub stub = new ParticlePhysicsStub();
        ActivityConfig config = new ActivityConfig();

        stub.sourceMode = "illegal_value";
        stub.saveToConfig(config);

        assertEquals("sword_and_axe", config.autoMaceSourceMode);
        assertEquals("sword_and_axe", stub.sourceMode);

        stub.sourceMode = null;
        stub.saveToConfig(config);

        assertEquals("sword_and_axe", config.autoMaceSourceMode);
        assertEquals("sword_and_axe", stub.sourceMode);

        stub.sourceMode = "axe_only";
        stub.saveToConfig(config);
        assertEquals("axe_only", config.autoMaceSourceMode);
        assertEquals("axe_only", stub.sourceMode);
    }

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

        int ry = popup.getRenderY();
        assertTrue(ry >= 0, "Render Y should be non-negative: " + ry);
    }

    @Test
    void testAboutTabOpenUrlErrorFallbackWithoutScreen() {
        AboutTab.URL_OPENER = url -> {
            throw new IOException("Simulated browser failure");
        };

        assertDoesNotThrow(() -> AboutTab.openUrl("https://example.com/test", null));
    }
}

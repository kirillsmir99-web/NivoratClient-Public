package activity.client.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PresetManagementTest {

    @BeforeEach
    void setUp() {
        ActivityConfigManager.resetDefaults();
    }

    @Test
    void testExportPresetProducesValidJsonAndBase64() {
        String json = ActivityConfigManager.exportPresetString();
        assertNotNull(json);
        assertTrue(json.contains("\"configVersion\""));

        String base64 = ActivityConfigManager.exportPresetCompact();
        assertNotNull(base64);
        assertFalse(base64.isBlank());
        assertFalse(base64.contains("\n"));
    }

    @Test
    void testImportPresetFromJson() {
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        cfg.autoMaceEnabled = false;
        cfg.windowOpacity = 42.0;
        String exported = ActivityConfigManager.exportPresetString();

        // Reset to defaults
        ActivityConfigManager.resetDefaults();
        assertTrue(ActivityConfigManager.getConfig().autoMaceEnabled);

        // Import back
        boolean ok = ActivityConfigManager.importPresetString(exported);
        assertTrue(ok);
        assertFalse(ActivityConfigManager.getConfig().autoMaceEnabled);
        assertEquals(42.0, ActivityConfigManager.getConfig().windowOpacity);
    }

    @Test
    void testImportPresetFromBase64() {
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        cfg.autoTotemEnabled = false;
        cfg.fontFamily = "retro_pixel";
        String base64 = ActivityConfigManager.exportPresetCompact();

        // Reset
        ActivityConfigManager.resetDefaults();
        assertTrue(ActivityConfigManager.getConfig().autoTotemEnabled);

        // Import from base64
        boolean ok = ActivityConfigManager.importPresetString(base64);
        assertTrue(ok);
        assertFalse(ActivityConfigManager.getConfig().autoTotemEnabled);
        assertEquals("retro_pixel", ActivityConfigManager.getConfig().fontFamily);
    }

    @Test
    void testApplyPresets() {
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        cfg.autoMaceLegitMode = true;
        cfg.autoMaceEnabled = false;
        activity.client.config.preset.Preset custom = activity.client.config.preset.PresetManager.createPreset("My Custom PvP", cfg);

        // Mutate current config
        cfg.autoMaceEnabled = true;
        cfg.autoMaceLegitMode = false;

        // Apply custom preset
        ActivityConfigManager.applyPreset(custom.getName());
        assertEquals("My Custom PvP", ActivityConfigManager.getConfig().activeProfile);
        assertFalse(ActivityConfigManager.getConfig().autoMaceEnabled);
        assertTrue(ActivityConfigManager.getConfig().autoMaceLegitMode);

        // Apply default preset
        ActivityConfigManager.applyPreset("default");
        assertEquals("default", ActivityConfigManager.getConfig().activeProfile);
        assertTrue(ActivityConfigManager.getConfig().autoMaceEnabled);

        // Clean up
        activity.client.config.preset.PresetManager.deletePreset(custom.getId());
    }

    @Test
    void testImportInvalidStringReturnsFalse() {
        assertFalse(ActivityConfigManager.importPresetString(""));
        assertFalse(ActivityConfigManager.importPresetString("   "));
        assertFalse(ActivityConfigManager.importPresetString(null));
        assertFalse(ActivityConfigManager.importPresetString("invalid non-json non-base64 {[[["));
    }
}

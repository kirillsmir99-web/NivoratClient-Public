package activity.client.config.preset;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CustomPresetSystemTest {

    @BeforeEach
    void setUp() {
        PresetManager.resetToDefaults();
        ActivityConfigManager.resetDefaults();
    }

    @AfterEach
    void tearDown() {
        PresetManager.resetToDefaults();
    }

    @Test
    void testDefaultPresetAlwaysExistsAndCannotBeDeleted() {
        Preset def = PresetManager.getDefaultPreset();
        assertNotNull(def, "Default preset must never be null");
        assertTrue(def.isBuiltin(), "Default preset must be built-in");
        assertEquals(Preset.DEFAULT_PRESET_ID, def.getId());
        assertEquals(Preset.DEFAULT_PRESET_NAME, def.getName());
        assertFalse(def.isDeletable(), "Default preset cannot be deletable");

        // Attempt delete
        boolean deleted = PresetManager.deletePreset(Preset.DEFAULT_PRESET_ID);
        assertFalse(deleted, "Deleting default preset must return false");

        // First item in list is always default
        List<Preset> presets = PresetManager.getPresets();
        assertFalse(presets.isEmpty());
        assertEquals(Preset.DEFAULT_PRESET_ID, presets.get(0).getId());
    }

    @Test
    void testCreateAndLookupCustomPreset() {
        ActivityConfig cfg = new ActivityConfig();
        cfg.autoMaceEnabled = false;
        cfg.autoTotemTriggerHearts = 5.5;

        Preset p = PresetManager.createPreset("My PvP Setup", cfg);
        assertNotNull(p);
        assertEquals("My PvP Setup", p.getName());
        assertFalse(p.isBuiltin());
        assertTrue(p.isDeletable());

        // Lookup by ID and name
        Preset byId = PresetManager.getPresetById(p.getId());
        assertNotNull(byId);
        assertEquals(p.getId(), byId.getId());

        Preset byName = PresetManager.getPresetByName("My PvP Setup");
        assertNotNull(byName);
        assertEquals(p.getId(), byName.getId());

        assertTrue(PresetManager.hasPresetNamed("My PvP Setup"));
    }

    @Test
    void testPresetNameValidation() {
        ActivityConfig cfg = new ActivityConfig();

        // Blank name rejected
        assertThrows(IllegalArgumentException.class, () -> PresetManager.createPreset("", cfg));
        assertThrows(IllegalArgumentException.class, () -> PresetManager.createPreset("   ", cfg));
        assertThrows(IllegalArgumentException.class, () -> PresetManager.createPreset(null, cfg));

        // Too long (>32 chars) rejected
        assertThrows(IllegalArgumentException.class, () -> PresetManager.createPreset("A".repeat(33), cfg));

        // Control characters rejected
        assertThrows(IllegalArgumentException.class, () -> PresetManager.createPreset("Bad\nName", cfg));
        assertThrows(IllegalArgumentException.class, () -> PresetManager.createPreset("Bad\tName", cfg));
    }

    @Test
    void testSnapshotExcludesTransientUiState() {
        ActivityConfig cfg = new ActivityConfig();
        cfg.windowPosX = 450;
        cfg.windowPosY = 220;
        cfg.searchFilter = "auto_totem";
        cfg.filterCategory = "Защита";
        cfg.matchCase = true;
        cfg.activeProfile = "custom_preset_123";

        JsonObject snapshot = PresetSerializer.extractSettingsSnapshot(cfg);
        assertFalse(snapshot.has("windowPosX"), "Transient windowPosX must be stripped");
        assertFalse(snapshot.has("windowPosY"), "Transient windowPosY must be stripped");
        assertFalse(snapshot.has("searchFilter"), "Transient searchFilter must be stripped");
        assertFalse(snapshot.has("filterCategory"), "Transient filterCategory must be stripped");
        assertFalse(snapshot.has("matchCase"), "Transient matchCase must be stripped");
        assertFalse(snapshot.has("activeProfile"), "Transient activeProfile must be stripped");

        // Apply snapshot to new config with its own window position
        ActivityConfig target = new ActivityConfig();
        target.windowPosX = 100;
        target.windowPosY = 150;
        PresetSerializer.applySettingsSnapshot(snapshot, target);

        assertEquals(100, target.windowPosX, "Target windowPosX must be preserved");
        assertEquals(150, target.windowPosY, "Target windowPosY must be preserved");
    }

    @Test
    void testApplyPresetSynchronizesConfig() {
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        cfg.autoMaceEnabled = false;
        cfg.autoShieldbreakerDistance = 3.5;

        Preset custom = PresetManager.createPreset("Distance Config", cfg);

        // Reset to factory defaults
        ActivityConfigManager.resetDefaults();
        assertTrue(ActivityConfigManager.getConfig().autoMaceEnabled);
        assertEquals(2.85, ActivityConfigManager.getConfig().autoShieldbreakerDistance);

        // Apply custom preset
        PresetManager.applyPreset(custom, ActivityConfigManager.getConfig());
        assertFalse(ActivityConfigManager.getConfig().autoMaceEnabled);
        assertEquals(3.5, ActivityConfigManager.getConfig().autoShieldbreakerDistance);
        assertEquals("Distance Config", ActivityConfigManager.getConfig().activeProfile);

        // Re-apply default preset
        PresetManager.applyPreset(PresetManager.getDefaultPreset(), ActivityConfigManager.getConfig());
        assertTrue(ActivityConfigManager.getConfig().autoMaceEnabled);
        assertEquals(2.85, ActivityConfigManager.getConfig().autoShieldbreakerDistance);
        assertEquals(Preset.DEFAULT_PRESET_ID, ActivityConfigManager.getConfig().activeProfile);
    }

    @Test
    void testDeletePresetRevertsActiveProfile() {
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        Preset custom = PresetManager.createPreset("Temp Config", cfg);
        cfg.activeProfile = custom.getName();

        boolean deleted = PresetManager.deletePreset(custom.getId());
        assertTrue(deleted);
        assertNull(PresetManager.getPresetByName("Temp Config"));
        assertEquals(Preset.DEFAULT_PRESET_ID, ActivityConfigManager.getConfig().activeProfile);
    }

    @Test
    void testExportToClipboardJsonStructure() {
        ActivityConfig cfg = new ActivityConfig();
        cfg.autoCartPlacementChance = 95.0;

        Preset preset = Preset.createCustom("Cart Master", PresetSerializer.extractSettingsSnapshot(cfg));
        String json = PresetSerializer.toClipboardJson(preset);

        assertNotNull(json);
        assertTrue(json.contains("\"schemaVersion\": 1"));
        assertTrue(json.contains("\"clientVersion\": \"v1.0.0\""));
        assertTrue(json.contains("\"presetName\": \"Cart Master\""));
        assertTrue(json.contains("\"settings\":"));
        assertFalse(json.contains("windowPosX"));
    }

    @Test
    void testImportFromClipboardJsonValid() throws Exception {
        String json = """
            {
              "schemaVersion": 1,
              "clientVersion": "v1.0.0",
              "presetName": "Imported PvP",
              "settings": {
                "autoMaceEnabled": false,
                "autoCartPlacementChance": 88.0
              }
            }
            """;

        Preset imported = PresetSerializer.fromClipboardJson(json);
        assertNotNull(imported);
        assertEquals("Imported PvP", imported.getName());
        assertFalse(imported.isBuiltin());

        ActivityConfig target = new ActivityConfig();
        PresetSerializer.applySettingsSnapshot(imported.getSettings(), target);
        assertFalse(target.autoMaceEnabled);
        assertEquals(88.0, target.autoCartPlacementChance);
    }

    @Test
    void testImportFromClipboardJsonOversizedRejected() {
        String oversized = "{\"schemaVersion\":1,\"clientVersion\":\"v1.0.0\",\"presetName\":\"Big\",\"settings\":{\"" + "X".repeat(70000) + "\":true}}";
        assertThrows(PresetSerializer.PresetValidationException.class, () -> PresetSerializer.fromClipboardJson(oversized));
    }

    @Test
    void testImportFromClipboardJsonCorruptRejected() {
        assertThrows(PresetSerializer.PresetValidationException.class, () -> PresetSerializer.fromClipboardJson("not a json string"));
        assertThrows(PresetSerializer.PresetValidationException.class, () -> PresetSerializer.fromClipboardJson("{\"schemaVersion\": \"invalid\"}"));
        assertThrows(PresetSerializer.PresetValidationException.class, () -> PresetSerializer.fromClipboardJson("[]"));
        assertThrows(PresetSerializer.PresetValidationException.class, () -> PresetSerializer.fromClipboardJson(""));
        assertThrows(PresetSerializer.PresetValidationException.class, () -> PresetSerializer.fromClipboardJson(null));
    }

    @Test
    void testDuplicateResolutionSaveAsNewVsOverwrite() {
        ActivityConfig cfg1 = new ActivityConfig();
        cfg1.autoTotemTriggerHearts = 2.0;
        Preset original = PresetManager.createPreset("Duel Setup", cfg1);
        assertEquals("Duel Setup", original.getName());

        ActivityConfig cfg2 = new ActivityConfig();
        cfg2.autoTotemTriggerHearts = 4.0;
        Preset candidate = Preset.createCustom("Duel Setup", PresetSerializer.extractSettingsSnapshot(cfg2));

        // Save as new (generates unique suffix)
        Preset asNew = PresetManager.addOrOverwriteImported(candidate, false);
        assertEquals("Duel Setup (1)", asNew.getName());
        assertEquals(3, PresetManager.getPresets().size()); // default + original + asNew

        // Overwrite
        ActivityConfig cfg3 = new ActivityConfig();
        cfg3.autoTotemTriggerHearts = 5.0;
        Preset candidateOverwrite = Preset.createCustom("Duel Setup", PresetSerializer.extractSettingsSnapshot(cfg3));
        Preset overwritten = PresetManager.addOrOverwriteImported(candidateOverwrite, true);

        assertEquals("Duel Setup", overwritten.getName());
        assertEquals(original.getId(), overwritten.getId());
        assertEquals(3, PresetManager.getPresets().size());
    }
}

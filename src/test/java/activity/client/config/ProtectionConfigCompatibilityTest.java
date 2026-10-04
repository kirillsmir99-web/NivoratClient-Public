package activity.client.config;

import activity.client.config.preset.Preset;
import activity.client.config.preset.PresetSerializer;
import activity.client.module.keybind.Keybind;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ProtectionConfigCompatibilityTest {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeSpecialFloatingPointValues()
            .create();

    private static final String LEGACY_UNPROTECTED_FIXTURE = """
    {
      "configVersion": 2,
      "autoMaceEnabled": true,
      "autoMaceKeybind": {
        "keyCode": 86,
        "ctrl": true,
        "shift": false,
        "alt": false
      },
      "autoMaceSourceMode": "sword_and_axe",
      "autoMaceRestoreDelayMs": 95.0,
      "autoMaceMissChance": 12.0,
      "autoSpearEnabled": false,
      "autoShieldbreakerEnabled": true,
      "autoShieldbreakerDistance": 3.1,
      "autoTotemEnabled": true,
      "autoTotemMode": "crystal",
      "autoTotemTriggerHearts": 3.5,
      "autoTotemCrystalTriggerHearts": 6.0,
      "autoTotemCountAbsorption": true,
      "autoCartEnabled": true,
      "autoCartMaxDistance": 4.2,
      "autoCartPlacementChance": 98.0,
      "autoCartCameraSmoothness": 115.0,
      "autoAnchorEnabled": true,
      "autoAnchorChance": 95.0,
      "overlayEnabled": false,
      "soundVolume": 85.0,
      "fontFamily": "inter",
      "pinnedModules": [
        "auto_cart",
        "auto_totem"
      ]
    }
    """;

    @Test
    @DisplayName("Legacy un-obfuscated JSON fixture parses flawlessly into ActivityConfig")
    void testLegacyFixtureDeserialization() {
        ActivityConfig config = GSON.fromJson(LEGACY_UNPROTECTED_FIXTURE, ActivityConfig.class);
        assertNotNull(config);
        assertEquals(2, config.configVersion);
        assertTrue(config.autoMaceEnabled);
        assertNotNull(config.autoMaceKeybind);
        assertEquals(86, config.autoMaceKeybind.getKeyCode());
        assertTrue(config.autoMaceKeybind.isCtrl());
        assertFalse(config.autoMaceKeybind.isShift());
        assertEquals("sword_and_axe", config.autoMaceSourceMode);
        assertEquals(95.0, config.autoMaceRestoreDelayMs);
        assertEquals(12.0, config.autoMaceMissChance);
        assertFalse(config.autoSpearEnabled);
        assertTrue(config.autoShieldbreakerEnabled);
        assertEquals(3.1, config.autoShieldbreakerDistance);
        assertTrue(config.autoTotemEnabled);
        assertEquals("crystal", config.autoTotemMode);
        assertEquals(3.5, config.autoTotemTriggerHearts);
        assertEquals(6.0, config.autoTotemCrystalTriggerHearts);
        assertTrue(config.autoTotemCountAbsorption);
        assertTrue(config.autoCartEnabled);
        assertEquals(4.2, config.autoCartMaxDistance);
        assertEquals(98.0, config.autoCartPlacementChance);
        assertEquals(115.0, config.autoCartCameraSmoothness);
        assertTrue(config.autoAnchorEnabled);
        assertEquals(95.0, config.autoAnchorChance);
        assertFalse(config.overlayEnabled);
        assertEquals(85.0, config.soundVolume);
        assertEquals("inter", config.fontFamily);
        assertTrue(config.isPinned("auto_cart"));
        assertTrue(config.isPinned("auto_totem"));
    }

    @Test
    @DisplayName("Serialized JSON strictly preserves external schema property names")
    void testSerializedNamesPreservedInJson() {
        ActivityConfig config = new ActivityConfig();
        config.autoMaceEnabled = true;
        config.autoCartCameraSmoothness = 120.0;
        config.autoTotemMode = "offhand";
        config.autoAnchorChance = 80.0;
        config.autoShieldbreakerDistance = 3.5;

        String json = GSON.toJson(config);
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

        assertTrue(obj.has("configVersion"), "JSON must retain configVersion key");
        assertTrue(obj.has("autoMaceEnabled"), "JSON must retain autoMaceEnabled key");
        assertTrue(obj.has("autoCartCameraSmoothness"), "JSON must retain autoCartCameraSmoothness key");
        assertTrue(obj.has("autoTotemMode"), "JSON must retain autoTotemMode key");
        assertTrue(obj.has("autoAnchorChance"), "JSON must retain autoAnchorChance key");
        assertTrue(obj.has("autoShieldbreakerDistance"), "JSON must retain autoShieldbreakerDistance key");
        assertTrue(obj.has("pinnedModules"), "JSON must retain pinnedModules key");

        JsonObject keybindObj = obj.getAsJsonObject("autoMaceKeybind");
        assertNotNull(keybindObj);
        assertTrue(keybindObj.has("keyCode"), "Keybind must retain keyCode key");
        assertTrue(keybindObj.has("ctrl"), "Keybind must retain ctrl key");
        assertTrue(keybindObj.has("shift"), "Keybind must retain shift key");
        assertTrue(keybindObj.has("alt"), "Keybind must retain alt key");
    }

    @Test
    @DisplayName("Round-trip serialization maintains value equality without mutation")
    void testRoundTripSerialization() {
        ActivityConfig original = new ActivityConfig();
        original.autoMaceEnabled = false;
        original.autoMaceRestoreDelayMs = 110.0;
        original.autoMaceKeybind = new Keybind(77, false, true, true);
        original.autoCartCameraSmoothness = 145.0;
        original.autoTotemMode = "crystal";
        original.autoTotemCrystalTriggerHearts = 8.0;
        original.soundVolume = 33.0;
        original.fontFamily = "sf-pro";

        String json = GSON.toJson(original);
        ActivityConfig parsed = GSON.fromJson(json, ActivityConfig.class);

        assertEquals(original.autoMaceEnabled, parsed.autoMaceEnabled);
        assertEquals(original.autoMaceRestoreDelayMs, parsed.autoMaceRestoreDelayMs);
        assertEquals(original.autoMaceKeybind.getKeyCode(), parsed.autoMaceKeybind.getKeyCode());
        assertEquals(original.autoMaceKeybind.isCtrl(), parsed.autoMaceKeybind.isCtrl());
        assertEquals(original.autoMaceKeybind.isShift(), parsed.autoMaceKeybind.isShift());
        assertEquals(original.autoMaceKeybind.isAlt(), parsed.autoMaceKeybind.isAlt());
        assertEquals(original.autoCartCameraSmoothness, parsed.autoCartCameraSmoothness);
        assertEquals(original.autoTotemMode, parsed.autoTotemMode);
        assertEquals(original.autoTotemCrystalTriggerHearts, parsed.autoTotemCrystalTriggerHearts);
        assertEquals(original.soundVolume, parsed.soundVolume);
        assertEquals(original.fontFamily, parsed.fontFamily);
    }

    @Test
    @DisplayName("Preset export and import preserves settings across snapshot boundaries")
    void testPresetSnapshotRoundTrip() throws Exception {
        ActivityConfig src = new ActivityConfig();
        src.autoMaceEnabled = false;
        src.autoCartCameraSmoothness = 135.0;
        src.autoAnchorChance = 77.0;

        JsonObject snapshot = PresetSerializer.extractSettingsSnapshot(src);
        assertTrue(snapshot.has("autoMaceEnabled"));
        assertTrue(snapshot.has("autoCartCameraSmoothness"));
        assertTrue(snapshot.has("autoAnchorChance"));

        Preset preset = Preset.createCustom("TestProfile", snapshot);
        String clipboard = PresetSerializer.toClipboardJson(preset);
        assertNotNull(clipboard);
        assertTrue(clipboard.contains("cooldownhud_preset"));

        Preset imported = PresetSerializer.fromClipboardJson(clipboard);
        assertEquals("TestProfile", imported.getName());

        ActivityConfig target = new ActivityConfig();
        PresetSerializer.applySettingsSnapshot(imported.getSettings(), target);

        assertFalse(target.autoMaceEnabled);
        assertEquals(135.0, target.autoCartCameraSmoothness);
        assertEquals(77.0, target.autoAnchorChance);
    }
}

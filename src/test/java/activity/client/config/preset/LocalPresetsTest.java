package activity.client.config.preset;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.EnumSet;
import static org.junit.jupiter.api.Assertions.*;

class LocalPresetsTest {
    @Test void moduleTemplateExcludesBindingsAndHud() throws Exception {
        JsonObject root = LocalPresets.capture("Мой набор", LocalPresets.Template.DEFAULTS, EnumSet.of(LocalPresets.Part.MODULES));
        var preview = LocalPresets.parse(root.toString());
        assertEquals("Мой набор", preview.name());
        var settings = preview.sections().getAsJsonObject("modules");
        assertFalse(settings.has("autoMaceKeybind"));
        assertFalse(settings.has("hpReaperEnabled"));
        assertFalse(settings.has("language"));
        assertFalse(settings.has("guiTheme"));
        assertFalse(settings.has("soundVolume"));
        assertFalse(settings.getAsJsonObject("modules").has("hp_reaper"));
        assertFalse(settings.getAsJsonObject("modules").getAsJsonObject("auto_mace").has("keybind"));
    }
    @Test void bindingPresetDoesNotToggleModules() throws Exception {
        var root = LocalPresets.capture("Бинды", LocalPresets.Template.DEFAULTS, EnumSet.of(LocalPresets.Part.BINDS));
        var settings = LocalPresets.parse(root.toString()).sections().getAsJsonObject("binds");
        assertTrue(settings.has("autoMaceKeybind"));
        assertFalse(settings.has("autoMaceEnabled"));
        assertFalse(settings.getAsJsonObject("modules").getAsJsonObject("auto_mace").has("enabled"));
    }
    @Test void partialMergePreservesUnselectedValues() {
        JsonObject before = JsonParser.parseString("{\"modules\":{\"auto_mace\":{\"enabled\":true,\"keybind\":{\"keyCode\":70}}},\"hudX\":50}").getAsJsonObject();
        JsonObject change = JsonParser.parseString("{\"modules\":{\"auto_mace\":{\"keybind\":{\"keyCode\":71}}}}").getAsJsonObject();
        JsonObject after = LocalPresets.merge(before, change);
        assertTrue(after.getAsJsonObject("modules").getAsJsonObject("auto_mace").get("enabled").getAsBoolean());
        assertEquals(50, after.get("hudX").getAsInt());
        assertEquals(70, before.getAsJsonObject("modules").getAsJsonObject("auto_mace").getAsJsonObject("keybind").get("keyCode").getAsInt());
    }
    @Test void parseRejectsForeignSectionsAndDoesNotChangeConfig() {
        ActivityConfig before = ActivityConfigManager.getConfig().copy();
        assertThrows(IOException.class, () -> LocalPresets.parse("{\"type\":\"nivoratclient_preset\",\"version\":1,\"name\":\"X\",\"sections\":{\"binds\":{\"autoMaceEnabled\":true}}}"));
        assertEquals(before, ActivityConfigManager.getConfig());
    }
    @Test void emptyNameAndEmptyPartsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> LocalPresets.capture("", LocalPresets.Template.DEFAULTS, EnumSet.of(LocalPresets.Part.HUD)));
        assertThrows(IllegalArgumentException.class, () -> LocalPresets.capture("X", LocalPresets.Template.DEFAULTS, EnumSet.noneOf(LocalPresets.Part.class)));
    }
    @Test void hudRoundTripDoesNotIncludeCombatSettings() throws Exception {
        var root = LocalPresets.capture("HUD", LocalPresets.Template.DEFAULTS, EnumSet.of(LocalPresets.Part.HUD));
        var settings = LocalPresets.parse(root.toString()).sections().getAsJsonObject("hud");
        assertTrue(settings.has("hpReaperEnabled"));
        assertFalse(settings.has("autoMaceEnabled"));
        assertTrue(settings.getAsJsonObject("modules").has("hp_reaper"));
    }
}

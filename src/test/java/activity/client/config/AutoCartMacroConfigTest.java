package activity.client.config;

import activity.client.module.keybind.Keybind;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AutoCartMacroConfigTest {
    @Test void copiedAndSerializedPresetsRetainMacroSettings() {
        ActivityConfig config = new ActivityConfig();
        config.autoCartMacroKeybind = new Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_TAB, false, false, false);
        config.autoCartMacroDrawTicks = 9;
        ActivityConfig copy = config.copy();
        assertEquals(config.autoCartMacroKeybind, copy.autoCartMacroKeybind);
        assertNotSame(config.autoCartMacroKeybind, copy.autoCartMacroKeybind);
        assertEquals(9, copy.autoCartMacroDrawTicks);
        var gson = new com.google.gson.Gson();
        ActivityConfig imported = gson.fromJson(gson.toJson(config), ActivityConfig.class);
        imported.sanitize();
        assertEquals(config.autoCartMacroKeybind, imported.autoCartMacroKeybind);
        assertEquals(9, imported.autoCartMacroDrawTicks);
    }

    @Test void invalidImportedSettingsGetSafeDefaults() {
        ActivityConfig config = new ActivityConfig();
        config.autoCartMacroKeybind = null;
        config.autoCartMacroDrawTicks = Double.NaN;
        config.sanitize();
        assertNotNull(config.autoCartMacroKeybind);
        assertEquals(6, config.autoCartMacroDrawTicks);
        config.autoCartMacroDrawTicks = -100;
        config.sanitize();
        assertEquals(3, config.autoCartMacroDrawTicks);
        config.autoCartMacroDrawTicks = 100;
        config.sanitize();
        assertEquals(20, config.autoCartMacroDrawTicks);
    }
}

package activity.client.gui;

import activity.client.config.ActivityConfigManager;
import activity.client.gui.custom.NativeBindAssignment;
import activity.client.gui.custom.SettingsBridge;
import activity.client.module.api.BuiltinModules;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.api.NivoratModule;
import activity.client.module.keybind.Keybind;
import activity.client.module.setting.KeybindSetting;
import activity.client.module.setting.SettingGroup;
import net.minecraft.text.Text;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;
import static org.junit.jupiter.api.Assertions.*;

class NativeBindingCaptureTest {
    @BeforeEach void reset() { BuiltinModules.registerAll(); ActivityConfigManager.resetDefaults(); NativeBindAssignment.cancel(); }
    private activity.client.gui.custom.api.ui.settings.impl.BindSetting widget(Keybind key) {
        var module = new NivoratModule("capture_test", Text.literal("Test"), Text.empty(), ModuleCategory.COMBAT) {};
        module.registerSetting(new KeybindSetting("trigger", Text.literal("Throw"), Text.empty(), SettingGroup.GENERAL, key, () -> key, value -> key.copyFrom(value)));
        return new activity.client.gui.custom.api.ui.settings.impl.BindSetting((activity.client.gui.custom.api.modules.settings.impl.BindSetting) SettingsBridge.models(module).getFirst());
    }
    @Test void specialKeysEndCaptureAndRetainTheirCodes() {
        int[] keys = {258, 280, 341, 340, 71};
        for (int code : keys) {
            var key = new Keybind(); var widget = widget(key); widget.setListening(true);
            widget.capture(code, code == 340 ? GLFW.GLFW_MOD_SHIFT : code == 341 ? GLFW.GLFW_MOD_CONTROL : 0, false);
            assertFalse(widget.isListening());
            if (NativeBindAssignment.isOpen()) NativeBindAssignment.confirm();
            assertEquals(code, key.getKeyCode());
            assertTrue(key.matchesKey(code, code == 340 ? GLFW.GLFW_MOD_SHIFT : code == 341 ? GLFW.GLFW_MOD_CONTROL : 0));
        }
    }
    @Test void mouseButtonsUseLegacyEncodingAndCanBeReassigned() {
        var key = new Keybind(); var widget = widget(key);
        for (int button : new int[]{0, 1, 2, 3, 4, 7}) {
            widget.setListening(true); widget.capture(button, 0, true);
            if (NativeBindAssignment.isOpen()) NativeBindAssignment.confirm();
            assertFalse(widget.isListening()); assertTrue(key.isMouseButton());
            assertEquals(button, key.getMouseButton()); assertTrue(key.matchesButton(button, 0));
        }
        widget.capture(256, 0, false); assertTrue(key.isUnbound());
    }
    @Test void duplicateBindingRequiresConfirmationAndCancelPreservesBoth() {
        var other = (KeybindSetting) ModuleRegistry.get("auto_spear").getSetting("trigger_keybind");
        other.get().set(258, false, false, false);
        var key = new Keybind(290); var widget = widget(key);
        widget.capture(258, 0, false);
        assertTrue(NativeBindAssignment.isOpen()); assertEquals(290, key.getKeyCode());
        NativeBindAssignment.cancel(); assertEquals(258, other.get().getKeyCode()); assertEquals(290, key.getKeyCode());
        widget.capture(258, 0, false); NativeBindAssignment.confirm();
        assertEquals(258, key.getKeyCode()); assertTrue(other.get().isUnbound());
    }
}

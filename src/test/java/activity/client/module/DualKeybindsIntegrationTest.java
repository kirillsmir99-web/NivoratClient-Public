package activity.client.module;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.module.api.BuiltinModules;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.impl.combat.AutoSpearModule;
import activity.client.module.impl.utility.AutoGGModule;
import activity.client.module.keybind.Keybind;
import activity.client.module.setting.KeybindSetting;
import activity.client.module.setting.Setting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.*;

public class DualKeybindsIntegrationTest {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @BeforeAll
    static void init() {
        BuiltinModules.registerAll();
    }

    @BeforeEach
    void resetState() {
        ActivityConfigManager.resetDefaults();
    }

    @Test
    @DisplayName("AutoSpear: Toggle keybind is unbound by default and trigger keybind is TAB")
    void testAutoSpearKeybindDefaults() {
        AutoSpearModule module = (AutoSpearModule) ModuleRegistry.get(AutoSpearModule.ID);
        assertNotNull(module, "AutoSpearModule must be registered");

        assertTrue(module.getKeybind().isUnbound(), "AutoSpear primary toggle keybind must be unbound by default");

        Setting<?> triggerSetting = module.getSetting("trigger_keybind");
        assertNotNull(triggerSetting, "trigger_keybind setting must exist on AutoSpearModule");
        assertTrue(triggerSetting instanceof KeybindSetting, "trigger_keybind must be a KeybindSetting");

        Keybind triggerKeybind = ((KeybindSetting) triggerSetting).get();
        assertNotNull(triggerKeybind, "triggerKeybind must not be null");
        assertEquals(GLFW.GLFW_KEY_TAB, triggerKeybind.getKeyCode(), "Default trigger keybind must be TAB (GLFW_KEY_TAB)");
        assertFalse(triggerKeybind.isCtrl(), "TAB trigger should not require CTRL");
        assertFalse(triggerKeybind.isShift(), "TAB trigger should not require SHIFT");
        assertFalse(triggerKeybind.isAlt(), "TAB trigger should not require ALT");
    }

    @Test
    @DisplayName("AutoGG: Menu keybind setting exists and defaults to GLFW_KEY_G")
    void testAutoGGMenuKeybindDefaults() {
        AutoGGModule module = (AutoGGModule) ModuleRegistry.get(AutoGGModule.ID);
        assertNotNull(module, "AutoGGModule must be registered");

        Setting<?> menuSetting = module.getSetting("menu_keybind");
        assertNotNull(menuSetting, "menu_keybind setting must exist on AutoGGModule");
        assertTrue(menuSetting instanceof KeybindSetting, "menu_keybind must be a KeybindSetting");

        Keybind menuKeybind = ((KeybindSetting) menuSetting).get();
        assertNotNull(menuKeybind, "menuKeybind must not be null");
        assertEquals(GLFW.GLFW_KEY_G, menuKeybind.getKeyCode(), "Default menu keybind must be G (GLFW_KEY_G)");
    }

    @Test
    @DisplayName("ActivityConfig: Dual keybind persistence, copying, and JSON serialization")
    void testConfigDualKeybindsLifecycle() {
        ActivityConfig config = new ActivityConfig();
        assertEquals(GLFW.GLFW_KEY_TAB, config.autoSpearTriggerKeybind.getKeyCode());
        assertEquals(GLFW.GLFW_KEY_G, config.autoGGMenuKeybind.getKeyCode());
        assertTrue(config.autoSpearKeybind.isUnbound());

        config.autoSpearTriggerKeybind.set(GLFW.GLFW_KEY_V, true, false, false);
        config.autoGGMenuKeybind.set(GLFW.GLFW_KEY_H, false, false, false);

        ActivityConfig copy = config.copy();
        assertEquals(GLFW.GLFW_KEY_V, copy.autoSpearTriggerKeybind.getKeyCode());
        assertTrue(copy.autoSpearTriggerKeybind.isCtrl());
        assertEquals(GLFW.GLFW_KEY_H, copy.autoGGMenuKeybind.getKeyCode());
        assertEquals(config, copy);

        String json = GSON.toJson(config);
        ActivityConfig deserialized = GSON.fromJson(json, ActivityConfig.class);
        assertEquals(GLFW.GLFW_KEY_V, deserialized.autoSpearTriggerKeybind.getKeyCode());
        assertTrue(deserialized.autoSpearTriggerKeybind.isCtrl());
        assertEquals(GLFW.GLFW_KEY_H, deserialized.autoGGMenuKeybind.getKeyCode());
    }

    @Test
    @DisplayName("KeybindSetting: Action trigger callback execution on key event")
    void testKeybindSettingTriggerPress() {
        boolean[] triggered = new boolean[]{false};
        KeybindSetting setting = new KeybindSetting(
                "test_keybind",
                net.minecraft.text.Text.literal("Test"),
                net.minecraft.text.Text.literal("Desc"),
                activity.client.module.setting.SettingGroup.GENERAL,
                new Keybind(GLFW.GLFW_KEY_K),
                () -> new Keybind(GLFW.GLFW_KEY_K),
                val -> {}
        );
        setting.onPress(client -> triggered[0] = true);

        setting.triggerPress(null);
        assertTrue(triggered[0], "onPress callback must be invoked when triggerPress is called");
    }

    @Test
    @DisplayName("Keybind: matchesKey matches when Ctrl is held unless Ctrl is explicitly required")
    void testKeybindMatchingWithCtrlHeld() {
        Keybind tabWithoutCtrl = new Keybind(GLFW.GLFW_KEY_TAB, false, false, false);
        assertTrue(tabWithoutCtrl.matchesKey(GLFW.GLFW_KEY_TAB, 0));
        assertTrue(tabWithoutCtrl.matchesKey(GLFW.GLFW_KEY_TAB, GLFW.GLFW_MOD_CONTROL),
                "Keybind without Ctrl must still trigger when Ctrl is held (e.g. sprinting)");
        assertFalse(tabWithoutCtrl.matchesKey(GLFW.GLFW_KEY_TAB, GLFW.GLFW_MOD_SHIFT));
        assertFalse(tabWithoutCtrl.matchesKey(GLFW.GLFW_KEY_TAB, GLFW.GLFW_MOD_ALT));

        Keybind tabWithCtrl = new Keybind(GLFW.GLFW_KEY_TAB, true, false, false);
        assertFalse(tabWithCtrl.matchesKey(GLFW.GLFW_KEY_TAB, 0));
        assertTrue(tabWithCtrl.matchesKey(GLFW.GLFW_KEY_TAB, GLFW.GLFW_MOD_CONTROL));
        assertFalse(tabWithCtrl.matchesKey(GLFW.GLFW_KEY_TAB, GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SHIFT));
    }

    @Test
    @DisplayName("KeybindManager: findConflict and unbindConflict behavior")
    void testKeybindConflictAndUnbind() {
        AutoSpearModule spear = (AutoSpearModule) ModuleRegistry.get(AutoSpearModule.ID);
        assertNotNull(spear);
        KeybindSetting triggerSetting = (KeybindSetting) spear.getSetting("trigger_keybind");
        assertNotNull(triggerSetting);
        triggerSetting.get().set(GLFW.GLFW_KEY_TAB, false, false, false);

        Keybind candidateTab = new Keybind(GLFW.GLFW_KEY_TAB, false, false, false);
        Keybind cartKeybind = new Keybind(GLFW.GLFW_KEY_C, false, false, false);

        String conflict = activity.client.module.keybind.KeybindManager.findConflict(candidateTab, cartKeybind);
        assertNotNull(conflict, "Must find conflict when candidateTab is already used by AutoSpear");

        assertNull(activity.client.module.keybind.KeybindManager.findConflict(candidateTab, triggerSetting.get()),
                "Must not report conflict with self");

        activity.client.module.keybind.KeybindManager.unbindConflict(candidateTab, cartKeybind);
        assertTrue(triggerSetting.get().isUnbound(), "Conflicting keybind in AutoSpear must be unbound after unbindConflict");

        spear.getKeybind().set(GLFW.GLFW_KEY_V, false, false, false);
        Keybind candidateV = new Keybind(GLFW.GLFW_KEY_V, false, false, false);
        String primaryConflict = activity.client.module.keybind.KeybindManager.findConflict(candidateV, cartKeybind);
        assertNotNull(primaryConflict, "Must detect conflict on primary module keybind");
        activity.client.module.keybind.KeybindManager.unbindConflict(candidateV, cartKeybind);
        assertTrue(spear.getKeybind().isUnbound(), "Primary keybind must be cleared after unbindConflict");
    }
}

package activity.client.module.setting;

import activity.client.module.keybind.Keybind;
import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Keybind configuration setting.
 */
public class KeybindSetting extends Setting<Keybind> {

    private final Supplier<Keybind> getter;
    private final Consumer<Keybind> setter;

    public KeybindSetting(String id, Text name, Text description, SettingGroup group,
                          Keybind defaultValue, Supplier<Keybind> getter, Consumer<Keybind> setter) {
        super(id, name, description, group, defaultValue);
        this.getter = getter;
        this.setter = setter;
    }

    public KeybindSetting(String id, Text name, Text description, SettingSection section,
                          Keybind defaultValue, Supplier<Keybind> getter, Consumer<Keybind> setter) {
        super(id, name, description, section, defaultValue);
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public Keybind get() {
        return getter != null ? getter.get() : getDefaultValue();
    }

    @Override
    public void set(Keybind value) {
        if (value != null && setter != null) {
            setter.accept(value);
        }
        notifyListeners(value);
        activity.client.module.keybind.KeybindManager.rebuildBoundKeybinds();
    }
}

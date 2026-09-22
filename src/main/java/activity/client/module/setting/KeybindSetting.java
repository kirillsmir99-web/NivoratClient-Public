package activity.client.module.setting;

import activity.client.module.keybind.Keybind;
import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class KeybindSetting extends Setting<Keybind> {

    private final Supplier<Keybind> getter;
    private final Consumer<Keybind> setter;
    private Runnable onPress;
    private java.util.function.Consumer<net.minecraft.client.MinecraftClient> onPressClient;
    private java.util.function.Consumer<net.minecraft.client.MinecraftClient> onReleaseClient;

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

    public KeybindSetting onPress(Runnable action) {
        this.onPress = action;
        return this;
    }

    public KeybindSetting onPress(java.util.function.Consumer<net.minecraft.client.MinecraftClient> action) {
        this.onPressClient = action;
        return this;
    }

    public KeybindSetting onRelease(java.util.function.Consumer<net.minecraft.client.MinecraftClient> action) {
        this.onReleaseClient = action;
        return this;
    }

    public void triggerPress(net.minecraft.client.MinecraftClient client) {
        if (onPress != null) {
            try {
                onPress.run();
            } catch (Throwable ignored) {}
        }
        if (onPressClient != null) {
            try {
                onPressClient.accept(client);
            } catch (Throwable ignored) {}
        }
        notifyListeners(get());
    }

    public void triggerRelease(net.minecraft.client.MinecraftClient client) {
        if (onReleaseClient != null) {
            try {
                onReleaseClient.accept(client);
            } catch (Throwable ignored) {}
        }
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

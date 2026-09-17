package activity.client.module.keybind;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Observable setting binding for a {@link Keybind} configuration entry.
 */
public class KeybindSetting {

    private final String id;
    private final String name;
    private final Keybind defaultValue;
    private final Keybind value;
    private final List<Consumer<Keybind>> listeners = new ArrayList<>();

    public KeybindSetting(String id, String name, Keybind defaultValue) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.defaultValue = new Keybind(
            defaultValue.getKeyCode(),
            defaultValue.isCtrl(),
            defaultValue.isShift(),
            defaultValue.isAlt()
        );
        this.value = new Keybind(
            defaultValue.getKeyCode(),
            defaultValue.isCtrl(),
            defaultValue.isShift(),
            defaultValue.isAlt()
        );
    }

    public KeybindSetting(String id, String name, int defaultKeyCode) {
        this(id, name, new Keybind(defaultKeyCode));
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Keybind getValue() {
        return this.value;
    }

    public Keybind getDefaultValue() {
        return this.defaultValue;
    }

    public void setValue(Keybind newValue) {
        if (newValue != null) {
            this.value.copyFrom(newValue);
            notifyListeners();
        }
    }

    public void set(int keyCode, boolean ctrl, boolean shift, boolean alt) {
        this.value.set(keyCode, ctrl, shift, alt);
        notifyListeners();
    }

    public void clear() {
        this.value.clear();
        notifyListeners();
    }

    public void reset() {
        setValue(this.defaultValue);
    }

    public void addListener(Consumer<Keybind> listener) {
        if (listener != null && !this.listeners.contains(listener)) {
            this.listeners.add(listener);
        }
    }

    public void removeListener(Consumer<Keybind> listener) {
        this.listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Consumer<Keybind> listener : this.listeners) {
            listener.accept(this.value);
        }
    }
}

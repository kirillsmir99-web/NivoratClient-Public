package activity.client.module.setting;

import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Boolean toggle setting bound directly to the config model and runtime controller.
 */
public class BooleanSetting extends Setting<Boolean> {

    private final Supplier<Boolean> getter;
    private final Consumer<Boolean> setter;

    public BooleanSetting(String id, Text name, Text description, SettingGroup group,
                          boolean defaultValue, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        super(id, name, description, group, defaultValue);
        this.getter = getter;
        this.setter = setter;
    }

    public BooleanSetting(String id, Text name, Text description, SettingSection section,
                          boolean defaultValue, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        super(id, name, description, section, defaultValue);
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public Boolean get() {
        return getter != null ? getter.get() : getDefaultValue();
    }

    @Override
    public void set(Boolean value) {
        boolean val = value != null ? value : getDefaultValue();
        if (setter != null) {
            setter.accept(val);
        }
        notifyListeners(val);
    }
}

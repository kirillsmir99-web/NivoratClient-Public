package activity.client.module.setting;

import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * String text field setting.
 */
public class StringSetting extends Setting<String> {

    private final Supplier<String> getter;
    private final Consumer<String> setter;

    public StringSetting(String id, Text name, Text description, SettingGroup group,
                         String defaultValue, Supplier<String> getter, Consumer<String> setter) {
        super(id, name, description, group, defaultValue);
        this.getter = getter;
        this.setter = setter;
    }

    public StringSetting(String id, Text name, Text description, SettingSection section,
                         String defaultValue, Supplier<String> getter, Consumer<String> setter) {
        super(id, name, description, section, defaultValue);
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public String get() {
        String val = getter != null ? getter.get() : getDefaultValue();
        return val != null ? val : getDefaultValue();
    }

    @Override
    public void set(String value) {
        String val = value != null ? value : getDefaultValue();
        if (setter != null) {
            setter.accept(val);
        }
        notifyListeners(val);
    }
}

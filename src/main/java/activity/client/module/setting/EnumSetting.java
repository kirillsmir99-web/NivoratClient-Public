package activity.client.module.setting;

import net.minecraft.text.Text;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class EnumSetting extends Setting<String> {

    private final List<String> options;
    private final Supplier<String> getter;
    private final Consumer<String> setter;
    private final java.util.function.Function<String, Text> nameProvider;

    public EnumSetting(String id, Text name, Text description, SettingGroup group,
                       List<String> options, String defaultValue,
                       Supplier<String> getter, Consumer<String> setter) {
        this(id, name, description, group, options, defaultValue, null, getter, setter);
    }

    public EnumSetting(String id, Text name, Text description, SettingGroup group,
                       List<String> options, String defaultValue,
                       java.util.function.Function<String, Text> nameProvider,
                       Supplier<String> getter, Consumer<String> setter) {
        super(id, name, description, group, defaultValue);
        this.options = options != null ? Collections.unmodifiableList(options) : Collections.emptyList();
        this.nameProvider = nameProvider;
        this.getter = getter;
        this.setter = setter;
    }

    public EnumSetting(String id, Text name, Text description, SettingSection section,
                       List<String> options, String defaultValue,
                       Supplier<String> getter, Consumer<String> setter) {
        this(id, name, description, section, options, defaultValue, null, getter, setter);
    }

    public EnumSetting(String id, Text name, Text description, SettingSection section,
                       List<String> options, String defaultValue,
                       java.util.function.Function<String, Text> nameProvider,
                       Supplier<String> getter, Consumer<String> setter) {
        super(id, name, description, section, defaultValue);
        this.options = options != null ? Collections.unmodifiableList(options) : Collections.emptyList();
        this.nameProvider = nameProvider;
        this.getter = getter;
        this.setter = setter;
    }

    public List<String> getOptions() {
        return options;
    }

    public Text getOptionName(String option) {
        if (nameProvider != null) {
            return nameProvider.apply(option);
        }
        return Text.literal(option);
    }

    @Override
    public String get() {
        String val = getter != null ? getter.get() : getDefaultValue();
        return val != null ? val : getDefaultValue();
    }

    @Override
    public void set(String value) {
        String val = (value != null && options.contains(value)) ? value : getDefaultValue();
        if (setter != null) {
            setter.accept(val);
        }
        notifyListeners(val);
    }
}

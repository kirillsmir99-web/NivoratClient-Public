package activity.client.module.setting;

import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Supplier;

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

    private String confirmTitle;
    private String confirmLine1;
    private String confirmLine2;

    public BooleanSetting requireConfirm(String title, String line1, String line2) {
        this.confirmTitle = title;
        this.confirmLine1 = line1;
        this.confirmLine2 = line2;
        return this;
    }

    public boolean hasConfirmation() {
        return confirmLine1 != null;
    }

    public String getConfirmTitle() {
        return confirmTitle;
    }

    public String getConfirmLine1() {
        return confirmLine1;
    }

    public String getConfirmLine2() {
        return confirmLine2;
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

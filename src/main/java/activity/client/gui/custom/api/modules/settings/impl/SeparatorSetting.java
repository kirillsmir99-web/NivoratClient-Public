package activity.client.gui.custom.api.modules.settings.impl;
import java.util.function.Supplier;
import activity.client.gui.custom.api.modules.settings.Setting;

public class SeparatorSetting
extends Setting {
    public SeparatorSetting(String string) {
        super(string);
    }

    public SeparatorSetting visible(Supplier<Boolean> supplier) {
        this.setVisibilityCondition(supplier);
        return this;
    }
}

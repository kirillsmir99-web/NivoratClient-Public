package activity.client.module.setting;

import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Convenience double slider setting extending {@link NumberSetting}.
 */
public class DoubleSetting extends NumberSetting {

    public DoubleSetting(String id, Text name, Text description, SettingGroup group,
                         double min, double max, double step, NumberUnit unit,
                         double defaultValue, Supplier<Double> getter, Consumer<Double> setter) {
        super(id, name, description, group, min, max, step, unit != null ? unit.getId() : "", false, defaultValue, getter, setter);
    }

    public DoubleSetting(String id, Text name, Text description, SettingSection section,
                         double min, double max, double step, NumberUnit unit,
                         double defaultValue, Supplier<Double> getter, Consumer<Double> setter) {
        super(id, name, description, section != null ? section.toGroup() : SettingGroup.GENERAL,
                min, max, step, unit != null ? unit.getId() : "", false, defaultValue, getter, setter);
        this.setSection(section);
    }
}

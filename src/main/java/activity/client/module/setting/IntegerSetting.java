package activity.client.module.setting;

import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Typed integer slider/counter setting with unit formatting and bounds constraints.
 */
public class IntegerSetting extends Setting<Integer> {

    private final int min;
    private final int max;
    private final int step;
    private final NumberUnit numberUnit;
    private final Supplier<Integer> getter;
    private final Consumer<Integer> setter;

    public IntegerSetting(String id, Text name, Text description, SettingGroup group,
                          int min, int max, int step, NumberUnit unit,
                          int defaultValue, Supplier<Integer> getter, Consumer<Integer> setter) {
        super(id, name, description, group, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step > 0 ? step : 1;
        this.numberUnit = unit != null ? unit : NumberUnit.NONE;
        this.getter = getter;
        this.setter = setter;
    }

    public IntegerSetting(String id, Text name, Text description, SettingSection section,
                          int min, int max, int step, NumberUnit unit,
                          int defaultValue, Supplier<Integer> getter, Consumer<Integer> setter) {
        super(id, name, description, section, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step > 0 ? step : 1;
        this.numberUnit = unit != null ? unit : NumberUnit.NONE;
        this.getter = getter;
        this.setter = setter;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public int getStep() {
        return step;
    }

    public NumberUnit getNumberUnit() {
        return numberUnit;
    }

    public String formatValue(int value) {
        return numberUnit.format(value, true);
    }

    public String formatCurrentValue() {
        return formatValue(get());
    }

    @Override
    public Integer get() {
        return getter != null ? getter.get() : getDefaultValue();
    }

    @Override
    public void set(Integer value) {
        int val = value != null ? value : getDefaultValue();
        val = Math.clamp(val, min, max);
        if (step > 1) {
            val = Math.round((float)(val - min) / step) * step + min;
            val = Math.clamp(val, min, max);
        }
        if (setter != null) {
            setter.accept(val);
        }
        notifyListeners(val);
    }
}

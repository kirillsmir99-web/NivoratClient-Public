package activity.client.module.setting;

import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Numeric slider setting supporting bounds, step intervals, and unit formatting.
 */
public class NumberSetting extends Setting<Double> {

    private final double min;
    private final double max;
    private final double step;
    private final String unit;
    private final NumberUnit numberUnit;
    private final boolean integerOnly;
    private final Supplier<Double> getter;
    private final Consumer<Double> setter;

    public NumberSetting(String id, Text name, Text description, SettingGroup group,
                         double min, double max, double step, String unit, boolean integerOnly,
                         double defaultValue, Supplier<Double> getter, Consumer<Double> setter) {
        super(id, name, description, group, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
        this.unit = unit != null ? unit : "";
        this.numberUnit = NumberUnit.fromString(unit);
        this.integerOnly = integerOnly;
        this.getter = getter;
        this.setter = setter;
    }

    public NumberSetting(String id, Text name, Text description, SettingGroup group,
                         double min, double max, double step, NumberUnit unit, boolean integerOnly,
                         double defaultValue, Supplier<Double> getter, Consumer<Double> setter) {
        super(id, name, description, group, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
        this.numberUnit = unit != null ? unit : NumberUnit.NONE;
        this.unit = this.numberUnit.getSuffix().trim();
        this.integerOnly = integerOnly;
        this.getter = getter;
        this.setter = setter;
    }

    public NumberSetting(String id, Text name, Text description, SettingSection section,
                         double min, double max, double step, NumberUnit unit, boolean integerOnly,
                         double defaultValue, Supplier<Double> getter, Consumer<Double> setter) {
        super(id, name, description, section, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
        this.numberUnit = unit != null ? unit : NumberUnit.NONE;
        this.unit = this.numberUnit.getSuffix().trim();
        this.integerOnly = integerOnly;
        this.getter = getter;
        this.setter = setter;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStep() {
        return step;
    }

    public String getUnit() {
        return unit;
    }

    public NumberUnit getNumberUnit() {
        return numberUnit;
    }

    public String formatValue(double value) {
        if (numberUnit != null && numberUnit != NumberUnit.NONE) {
            return numberUnit.format(value, integerOnly);
        }
        if (unit != null && !unit.isEmpty()) {
            String numStr = integerOnly
                    ? String.format(java.util.Locale.ROOT, "%.0f", value)
                    : String.format(java.util.Locale.ROOT, "%.1f", value);
            return numStr + (unit.startsWith(" ") || unit.startsWith("%") ? unit : " " + unit);
        }
        return integerOnly
                ? String.format(java.util.Locale.ROOT, "%.0f", value)
                : String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    public String formatCurrentValue() {
        return formatValue(get());
    }

    public boolean isIntegerOnly() {
        return integerOnly;
    }

    @Override
    public Double get() {
        return getter != null ? getter.get() : getDefaultValue();
    }

    @Override
    public void set(Double value) {
        double val = (value != null && !Double.isNaN(value) && !Double.isInfinite(value)) ? value : getDefaultValue();
        val = Math.clamp(val, min, max);
        if (step > 0.0) {
            val = Math.round((val - min) / step) * step + min;
            val = Math.clamp(val, min, max);
        }
        if (integerOnly) {
            val = Math.round(val);
        }
        if (setter != null) {
            setter.accept(val);
        }
        notifyListeners(val);
    }
}

package activity.client.gui.custom.api.modules.settings.impl;
import java.util.function.Supplier;
import activity.client.gui.custom.api.modules.settings.Setting;

public class BooleanSetting
extends Setting {
    private boolean value;
    private boolean defaultValue;
    private boolean defaultCaptured;

    public BooleanSetting(String string, String string2) {
        super(string, string2);
    }

    public BooleanSetting(String string, String string2, boolean bl) {
        this(string, string2);
        this.setValue(bl);
    }

    public boolean getValue() {
        return this.value;
    }

    public BooleanSetting setValue(boolean bl) {
        boolean bl2 = this.value != bl;
        this.value = bl;
        if (!this.defaultCaptured) {
            this.defaultValue = bl;
            this.defaultCaptured = true;
        }
        if (bl2) {
            this.notifyChanged();
        }
        return this;
    }

    public boolean getDefaultValue() {
        return this.defaultValue;
    }

    public BooleanSetting visible(Supplier<Boolean> supplier) {
        this.setVisibilityCondition(supplier);
        return this;
    }

    private java.util.function.Consumer<Runnable> confirmEnablePrompt;

    public BooleanSetting onEnableConfirm(java.util.function.Consumer<Runnable> prompt) {
        this.confirmEnablePrompt = prompt;
        return this;
    }

    public BooleanSetting toggle() {
        if (!this.value && this.confirmEnablePrompt != null) {
            this.confirmEnablePrompt.accept(() -> this.setValue(true));
            return this;
        }
        return this.setValue(!this.value);
    }

    public boolean isValue() {
        return this.value;
    }

    public BooleanSetting visibleWhen(Supplier<Boolean> supplier) {
        return this.visible(supplier);
    }
}

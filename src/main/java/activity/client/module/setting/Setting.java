package activity.client.module.setting;

import net.minecraft.text.Text;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public abstract class Setting<T> {

    private final String id;
    private final Text name;
    private final Text description;
    private final SettingGroup group;
    private SettingSection section;
    private final T defaultValue;
    private final List<Consumer<T>> listeners = new CopyOnWriteArrayList<>();
    private java.util.function.BooleanSupplier visibilityCondition;

    public Setting(String id, Text name, Text description, SettingGroup group, T defaultValue) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.group = group != null ? group : SettingGroup.GENERAL;
        this.section = this.group.toSection();
        this.defaultValue = defaultValue;
    }

    public Setting(String id, Text name, Text description, SettingSection section, T defaultValue) {
        this(id, name, description, section != null ? section.toGroup() : SettingGroup.GENERAL, defaultValue);
        this.section = section;
    }

    public String getId() {
        return id;
    }

    public Text getName() {
        return name;
    }

    public Text getDisplayName() {
        return name;
    }

    public Text getDescription() {
        return description;
    }

    public Text getTooltip() {
        return description;
    }

    public SettingGroup getGroup() {
        return group;
    }

    public SettingSection getSection() {
        return section != null ? section : (group != null ? group.toSection() : SettingSection.GENERAL);
    }

    public Setting<T> setSection(SettingSection section) {
        this.section = section;
        return this;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public abstract T get();

    public abstract void set(T value);

    public T getCurrentValue() {
        return get();
    }

    public void setCurrentValue(T value) {
        set(value);
    }

    public void reset() {
        set(defaultValue);
    }

    public boolean isVisible() {
        return visibilityCondition == null || visibilityCondition.getAsBoolean();
    }

    public Setting<T> visibleWhen(java.util.function.BooleanSupplier condition) {
        this.visibilityCondition = condition;
        return this;
    }

    public <O> Setting<T> visibleWhen(Setting<O> parentSetting, java.util.function.Predicate<O> predicate) {
        this.visibilityCondition = () -> parentSetting != null && parentSetting.isVisible() && predicate.test(parentSetting.get());
        if (parentSetting != null) {
            parentSetting.addListener(v -> this.notifyListeners(this.get()));
        }
        return this;
    }

    public Setting<T> visibleWhen(BooleanSetting parentSetting) {
        return visibleWhen(parentSetting, Boolean::booleanValue);
    }

    public Setting<T> visibleWhen(EnumSetting parentSetting, String expectedValue) {
        return visibleWhen(parentSetting, val -> java.util.Objects.equals(val, expectedValue));
    }

    public Setting<T> setVisibilityCondition(java.util.function.BooleanSupplier condition) {
        this.visibilityCondition = condition;
        return this;
    }

    public java.util.function.BooleanSupplier getVisibilityCondition() {
        return visibilityCondition;
    }

    public void addListener(Consumer<T> listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(Consumer<T> listener) {
        listeners.remove(listener);
    }

    protected void notifyListeners(T newValue) {
        for (Consumer<T> listener : listeners) {
            try {
                listener.accept(newValue);
            } catch (Throwable ignored) {}
        }
    }
}

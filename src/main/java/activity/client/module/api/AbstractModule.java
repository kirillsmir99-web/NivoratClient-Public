package activity.client.module.api;

import activity.client.config.ActivityConfig;
import activity.client.module.keybind.Keybind;
import activity.client.module.setting.ActionSetting;
import activity.client.module.setting.BooleanSetting;
import activity.client.module.setting.DoubleSetting;
import activity.client.module.setting.EnumSetting;
import activity.client.module.setting.IntegerSetting;
import activity.client.module.setting.KeybindSetting;
import activity.client.module.setting.NumberSetting;
import activity.client.module.setting.NumberUnit;
import activity.client.module.setting.Setting;
import activity.client.module.setting.SettingGroup;
import activity.client.module.setting.SettingSection;
import activity.client.module.setting.StringSetting;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class AbstractModule implements IModule {

    protected final String id;
    protected final Text name;
    protected final Text description;
    protected final ModuleCategory category;
    protected final Keybind keybind = new Keybind();
    protected final List<Setting<?>> settings = new ArrayList<>();
    protected boolean enabled;
    protected ModuleMetadata metadata;

    public AbstractModule(String id, Text name, Text description, ModuleCategory category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category != null ? category : ModuleCategory.UTILITY;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Text getName() {
        return name;
    }

    @Override
    public Text getDescription() {
        return description;
    }

    @Override
    public ModuleCategory getCategory() {
        return category;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
                onEnable();
            } else {
                onDisable();
            }
            ModuleEventDispatcher.updateActiveModules();
        }
    }

    public void toggle() {
        setEnabled(!isEnabled());
    }

    @Override
    public void loadFromConfig(ActivityConfig config) {}

    @Override
    public void saveToConfig(ActivityConfig config) {}

    @Override
    public Keybind getKeybind() {
        return keybind;
    }

    @Override
    public ModuleStatus getStatus() {
        return isEnabled() ? ModuleStatus.READY : ModuleStatus.DISABLED;
    }

    @Override
    public boolean isStub() {
        return false;
    }

    @Override
    public ModuleMetadata getMetadata() {
        if (this.metadata == null) {
            this.metadata = ModuleMetadata.builder(id)
                    .displayName(name)
                    .description(description)
                    .category(category)
                    .author("kt1xW")
                    .version("1.0.0")
                    .keybind(keybind)
                    .build();
        }
        return this.metadata;
    }

    @Override
    public List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    public void onEnable() {}

    public void onDisable() {}

    public <S extends Setting<?>> S registerSetting(S setting) {
        if (setting != null && !settings.contains(setting)) {
            settings.add(setting);
        }
        return setting;
    }

    public BooleanSetting registerBoolean(String id, Text name, Text description, SettingGroup group,
                                          boolean defaultValue, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return registerSetting(new BooleanSetting(id, name, description, group, defaultValue, getter, setter));
    }

    public BooleanSetting registerBoolean(String id, Text name, Text description, SettingSection section,
                                          boolean defaultValue, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return registerSetting(new BooleanSetting(id, name, description, section, defaultValue, getter, setter));
    }

    public NumberSetting registerNumber(String id, Text name, Text description, SettingGroup group,
                                        double min, double max, double step, String unit, boolean integerOnly,
                                        double defaultValue, Supplier<Double> getter, Consumer<Double> setter) {
        return registerSetting(new NumberSetting(id, name, description, group, min, max, step, unit, integerOnly, defaultValue, getter, setter));
    }

    public NumberSetting registerNumber(String id, Text name, Text description, SettingGroup group,
                                        double min, double max, double step, NumberUnit unit, boolean integerOnly,
                                        double defaultValue, Supplier<Double> getter, Consumer<Double> setter) {
        return registerSetting(new NumberSetting(id, name, description, group, min, max, step, unit, integerOnly, defaultValue, getter, setter));
    }

    public NumberSetting registerNumber(String id, Text name, Text description, SettingSection section,
                                        double min, double max, double step, NumberUnit unit, boolean integerOnly,
                                        double defaultValue, Supplier<Double> getter, Consumer<Double> setter) {
        return registerSetting(new NumberSetting(id, name, description, section, min, max, step, unit, integerOnly, defaultValue, getter, setter));
    }

    public IntegerSetting registerInteger(String id, Text name, Text description, SettingSection section,
                                          int min, int max, int step, NumberUnit unit,
                                          int defaultValue, Supplier<Integer> getter, Consumer<Integer> setter) {
        return registerSetting(new IntegerSetting(id, name, description, section, min, max, step, unit, defaultValue, getter, setter));
    }

    public IntegerSetting registerInteger(String id, Text name, Text description, SettingGroup group,
                                          int min, int max, int step, NumberUnit unit,
                                          int defaultValue, Supplier<Integer> getter, Consumer<Integer> setter) {
        return registerSetting(new IntegerSetting(id, name, description, group, min, max, step, unit, defaultValue, getter, setter));
    }

    public DoubleSetting registerDouble(String id, Text name, Text description, SettingSection section,
                                        double min, double max, double step, NumberUnit unit,
                                        double defaultValue, Supplier<Double> getter, Consumer<Double> setter) {
        return registerSetting(new DoubleSetting(id, name, description, section, min, max, step, unit, defaultValue, getter, setter));
    }

    public DoubleSetting registerDouble(String id, Text name, Text description, SettingGroup group,
                                        double min, double max, double step, NumberUnit unit,
                                        double defaultValue, Supplier<Double> getter, Consumer<Double> setter) {
        return registerSetting(new DoubleSetting(id, name, description, group, min, max, step, unit, defaultValue, getter, setter));
    }

    public EnumSetting registerEnum(String id, Text name, Text description, SettingGroup group,
                                    List<String> options, String defaultValue,
                                    Supplier<String> getter, Consumer<String> setter) {
        return registerSetting(new EnumSetting(id, name, description, group, options, defaultValue, getter, setter));
    }

    public EnumSetting registerEnum(String id, Text name, Text description, SettingSection section,
                                    List<String> options, String defaultValue,
                                    Supplier<String> getter, Consumer<String> setter) {
        return registerSetting(new EnumSetting(id, name, description, section, options, defaultValue, getter, setter));
    }

    public EnumSetting registerEnum(String id, Text name, Text description, SettingGroup group,
                                    List<String> options, String defaultValue,
                                    java.util.function.Function<String, Text> nameProvider,
                                    Supplier<String> getter, Consumer<String> setter) {
        return registerSetting(new EnumSetting(id, name, description, group, options, defaultValue, nameProvider, getter, setter));
    }

    public EnumSetting registerEnum(String id, Text name, Text description, SettingGroup group,
                                    List<String> options, String defaultValue,
                                    java.util.function.Function<String, Text> nameProvider,
                                    java.util.function.Function<String, Text> tooltipProvider,
                                    Supplier<String> getter, Consumer<String> setter) {
        return registerSetting(new EnumSetting(id, name, description, group, options, defaultValue, nameProvider, tooltipProvider, getter, setter));
    }

    public EnumSetting registerEnum(String id, Text name, Text description, SettingSection section,
                                    List<String> options, String defaultValue,
                                    java.util.function.Function<String, Text> nameProvider,
                                    Supplier<String> getter, Consumer<String> setter) {
        return registerSetting(new EnumSetting(id, name, description, section, options, defaultValue, nameProvider, getter, setter));
    }

    public EnumSetting registerEnum(String id, Text name, Text description, SettingSection section,
                                    List<String> options, String defaultValue,
                                    java.util.function.Function<String, Text> nameProvider,
                                    java.util.function.Function<String, Text> tooltipProvider,
                                    Supplier<String> getter, Consumer<String> setter) {
        return registerSetting(new EnumSetting(id, name, description, section, options, defaultValue, nameProvider, tooltipProvider, getter, setter));
    }

    public KeybindSetting registerKeybind(String id, Text name, Text description, SettingGroup group,
                                          Keybind defaultValue, Supplier<Keybind> getter, Consumer<Keybind> setter) {
        return registerSetting(new KeybindSetting(id, name, description, group, defaultValue, getter, setter));
    }

    public KeybindSetting registerKeybind(String id, Text name, Text description, SettingSection section,
                                          Keybind defaultValue, Supplier<Keybind> getter, Consumer<Keybind> setter) {
        return registerSetting(new KeybindSetting(id, name, description, section, defaultValue, getter, setter));
    }

    public StringSetting registerString(String id, Text name, Text description, SettingGroup group,
                                        String defaultValue, Supplier<String> getter, Consumer<String> setter) {
        return registerSetting(new StringSetting(id, name, description, group, defaultValue, getter, setter));
    }

    public StringSetting registerString(String id, Text name, Text description, SettingSection section,
                                        String defaultValue, Supplier<String> getter, Consumer<String> setter) {
        return registerSetting(new StringSetting(id, name, description, section, defaultValue, getter, setter));
    }

    public ActionSetting registerAction(String id, Text name, Text description, SettingGroup group, Runnable action) {
        return registerSetting(new ActionSetting(id, name, description, group, action));
    }

    public ActionSetting registerAction(String id, Text name, Text description, SettingSection section, Runnable action) {
        return registerSetting(new ActionSetting(id, name, description, section, action));
    }
}

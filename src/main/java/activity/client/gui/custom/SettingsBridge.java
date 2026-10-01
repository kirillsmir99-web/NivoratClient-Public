package activity.client.gui.custom;

import activity.client.module.api.IModule;
import activity.client.module.setting.*;
import activity.client.gui.custom.api.modules.settings.Setting;
import activity.client.config.ActivityConfigManager;
import java.util.List;
import java.util.ArrayList;

public final class SettingsBridge {
    private SettingsBridge() {}
    public static List<Setting> models(IModule module) {
        List<Setting> models = new ArrayList<>();
        for (var source : module.getSettings()) {
            String name = VisualText.settingName(module,source), desc = VisualText.settingDescription(module,source);
            Setting model = null;
            if (source instanceof BooleanSetting s) {
                var m = new activity.client.gui.custom.api.modules.settings.impl.BooleanSetting(name,desc,Boolean.TRUE.equals(s.get()));
                m.setChangeListener(() -> { s.set(m.getValue()); save(module); }); model = m;
            } else if (source instanceof IntegerSetting s) {
                var m = new activity.client.gui.custom.api.modules.settings.impl.SliderSetting(name,desc).range(s.getMin(),s.getMax()).increment(s.getStep()).setValue(s.get().floatValue());
                m.setFormatter(value -> s.formatValue(Math.round(value)));
                m.setChangeListener(() -> { s.set(Math.round(m.getValue())); save(module); }); model = m;
            } else if (source instanceof NumberSetting s) {
                var m = new activity.client.gui.custom.api.modules.settings.impl.SliderSetting(name,desc).range((float)s.getMin(),(float)s.getMax()).increment((float)s.getStep()).setValue(s.get().floatValue());
                m.setFormatter(value -> s.formatValue(value.doubleValue()));
                m.setChangeListener(() -> { s.set((double)m.getValue()); save(module); }); model = m;
            } else if (source instanceof EnumSetting s) {
                var m = new activity.client.gui.custom.api.modules.settings.impl.SelectSetting(name,desc).value(s.getOptions().toArray(String[]::new)).selected(s.get());
                m.setLabelProvider(option -> VisualText.resolve(s.getOptionName(option)));
                m.setChangeListener(() -> { s.set(m.getSelected()); save(module); }); model = m;
            } else if (source instanceof StringSetting s) {
                var m = new activity.client.gui.custom.api.modules.settings.impl.TextSetting(name,desc).setText(s.get());
                m.setChangeListener(() -> { s.set(m.getText()); save(module); }); model = m;
            } else if (source instanceof KeybindSetting s) {
                var m = new activity.client.gui.custom.api.modules.settings.impl.BindSetting(name,desc).setKey(s.get().getKeyCode());
                m.setSource(s);
                m.setChangeListener(() -> { var key=s.get();int code=m.requestedKey();if(code>=0&&code<=7)key.set(activity.client.module.keybind.Keybind.MOUSE_OFFSET-code,key.isCtrl(),key.isShift(),key.isAlt());else key.set(code,key.isCtrl(),key.isShift(),key.isAlt());s.set(key);save(module); }); model = m;
            } else if (source instanceof ActionSetting s) {
                var m = new activity.client.gui.custom.api.modules.settings.impl.ButtonSetting(name,desc).label(name).onClick(s::execute);m.setLabelProvider(()->VisualText.settingName(module,s));model=m;
            }
            if (model != null) { model.setTextProviders(()->VisualText.settingName(module,source),()->VisualText.settingDescription(module,source));model.setVisibilityCondition(source::isVisible); models.add(model); }
        }
        return models;
    }
    private static void save(IModule module) { module.saveToConfig(ActivityConfigManager.getConfig());ActivityConfigManager.markDirty(); }
}

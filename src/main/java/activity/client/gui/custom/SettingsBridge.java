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
                if (s.hasConfirmation()) {
                    m.onEnableConfirm(action -> {
                        boolean ru = activity.client.i18n.LocalizationService.isRussianPreferred();
                        String title = s.getConfirmTitle() != null ? s.getConfirmTitle() : (ru ? "Предупреждение" : "Warning");
                        String line1 = s.getConfirmLine1();
                        String line2 = s.getConfirmLine2();
                        String confirmText = ru ? "Включить" : "Enable";
                        String cancelText = ru ? "Отмена" : "Cancel";
                        activity.client.gui.custom.api.ui.modal.UnifiedConfirmModal.show(
                                title,
                                line1,
                                line2,
                                0xFFAA28,
                                List.of(
                                        new activity.client.gui.custom.api.ui.modal.UnifiedConfirmModal.ModalButton(cancelText, activity.client.gui.custom.api.ui.button.UnifiedButton.Variant.SECONDARY, () -> {}),
                                        new activity.client.gui.custom.api.ui.modal.UnifiedConfirmModal.ModalButton(confirmText, activity.client.gui.custom.api.ui.button.UnifiedButton.Variant.PRIMARY, action)
                                ),
                                () -> {}
                        );
                    });
                }
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
                var opts = "preset".equals(s.getId()) ? s.getOptions().stream().filter(o -> !"learned".equalsIgnoreCase(o)).toArray(String[]::new) : s.getOptions().toArray(String[]::new);
                var m = new activity.client.gui.custom.api.modules.settings.impl.SelectSetting(name,desc).value(opts).selected(s.get());
                m.setLabelProvider(option -> VisualText.resolve(s.getOptionName(option)));
                m.setChangeListener(() -> {
                    s.set(m.getSelected());
                    if (!s.get().equals(m.getSelected())) {
                        m.selected(s.get());
                    }
                    save(module);
                }); model = m;
            } else if (source instanceof StringSetting s) {
                var m = new activity.client.gui.custom.api.modules.settings.impl.TextSetting(name,desc).setText(s.get());
                m.setChangeListener(() -> { s.set(m.getText()); save(module); }); model = m;
            } else if (source instanceof KeybindSetting s) {
                var m = new activity.client.gui.custom.api.modules.settings.impl.BindSetting(name,desc).setKey(s.get().getKeyCode());
                m.setSource(s);
                m.setChangeListener(() -> { var key=s.get();int code=m.requestedKey();if(code==1002)code=2;if(code>=0&&code<=7)key.set(activity.client.module.keybind.Keybind.MOUSE_OFFSET-code,key.isCtrl(),key.isShift(),key.isAlt());else key.set(code,key.isCtrl(),key.isShift(),key.isAlt());s.set(key);save(module); }); model = m;
            } else if (source instanceof ActionSetting s) {
                var m = new activity.client.gui.custom.api.modules.settings.impl.ButtonSetting(name,desc).label("Открыть").onClick(s::execute);m.setLabelProvider(()->"Открыть");model=m;
            }
            if (model != null) { model.setTextProviders(()->VisualText.settingName(module,source),()->VisualText.settingDescription(module,source));model.setVisibilityCondition(source::isVisible); models.add(model); }
        }
        return models;
    }
    private static void save(IModule module) {
        module.saveToConfig(ActivityConfigManager.getConfig());
        activity.client.config.CooldownConfigManager.syncToModules(ActivityConfigManager.getConfig());
        ActivityConfigManager.markDirty();
        ActivityConfigManager.save();
    }
}

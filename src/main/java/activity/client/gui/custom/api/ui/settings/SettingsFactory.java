package activity.client.gui.custom.api.ui.settings;
import java.util.ArrayList;
import java.util.List;
import activity.client.module.api.IModule;
import activity.client.gui.custom.api.modules.settings.impl.BooleanSetting;
import activity.client.gui.custom.api.modules.settings.impl.ButtonSetting;
import activity.client.gui.custom.api.ui.settings.Setting;
import activity.client.gui.custom.api.ui.settings.impl.BindSetting;
import activity.client.gui.custom.api.ui.settings.impl.BoolSetting;
import activity.client.gui.custom.api.ui.settings.impl.ButtonRowSetting;
import activity.client.gui.custom.api.ui.settings.impl.ColorSetting;
import activity.client.gui.custom.api.ui.settings.impl.MultiSelectSetting;
import activity.client.gui.custom.api.ui.settings.impl.SelectSetting;
import activity.client.gui.custom.api.ui.settings.impl.SeparatorSetting;
import activity.client.gui.custom.api.ui.settings.impl.SliderSetting;
import activity.client.gui.custom.api.ui.settings.impl.TextSetting;

public final class SettingsFactory {
    private SettingsFactory() {
    }

    public static Setting create(activity.client.gui.custom.api.modules.settings.Setting setting) {
        if (setting instanceof BooleanSetting) {
            BooleanSetting booleanSetting = (BooleanSetting)setting;
            return new BoolSetting(booleanSetting);
        }
        if (setting instanceof activity.client.gui.custom.api.modules.settings.impl.SliderSetting) {
            activity.client.gui.custom.api.modules.settings.impl.SliderSetting sliderSetting = (activity.client.gui.custom.api.modules.settings.impl.SliderSetting)setting;
            return new SliderSetting(sliderSetting);
        }
        if (setting instanceof activity.client.gui.custom.api.modules.settings.impl.ColorSetting) {
            activity.client.gui.custom.api.modules.settings.impl.ColorSetting colorSetting = (activity.client.gui.custom.api.modules.settings.impl.ColorSetting)setting;
            return new ColorSetting(colorSetting);
        }
        if (setting instanceof activity.client.gui.custom.api.modules.settings.impl.SelectSetting) {
            activity.client.gui.custom.api.modules.settings.impl.SelectSetting selectSetting = (activity.client.gui.custom.api.modules.settings.impl.SelectSetting)setting;
            return new SelectSetting(selectSetting);
        }
        if (setting instanceof activity.client.gui.custom.api.modules.settings.impl.MultiSelectSetting) {
            activity.client.gui.custom.api.modules.settings.impl.MultiSelectSetting multiSelectSetting = (activity.client.gui.custom.api.modules.settings.impl.MultiSelectSetting)setting;
            return new MultiSelectSetting(multiSelectSetting);
        }
        if (setting instanceof activity.client.gui.custom.api.modules.settings.impl.BindSetting) {
            activity.client.gui.custom.api.modules.settings.impl.BindSetting bindSetting = (activity.client.gui.custom.api.modules.settings.impl.BindSetting)setting;
            return new BindSetting(bindSetting);
        }
        if (setting instanceof activity.client.gui.custom.api.modules.settings.impl.SeparatorSetting) {
            activity.client.gui.custom.api.modules.settings.impl.SeparatorSetting separatorSetting = (activity.client.gui.custom.api.modules.settings.impl.SeparatorSetting)setting;
            return new SeparatorSetting(separatorSetting);
        }
        if (setting instanceof activity.client.gui.custom.api.modules.settings.impl.TextSetting) {
            activity.client.gui.custom.api.modules.settings.impl.TextSetting textSetting = (activity.client.gui.custom.api.modules.settings.impl.TextSetting)setting;
            return new TextSetting(textSetting);
        }
        if (setting instanceof ButtonSetting) {
            ButtonSetting buttonSetting = (ButtonSetting)setting;
            return new ButtonRowSetting(buttonSetting);
        }
        return null;
    }

    public static boolean shouldHaveToggleKeybind(IModule module) {
        if (module == null) return false;
        String id = module.getId().toLowerCase(java.util.Locale.ROOT).replace("_", "");
        return !id.equals("cooldownhud")
            && !id.equals("autogg")
            && !id.equals("hpreaper")
            && !id.equals("carthud");
    }

    public static List<Setting> build(activity.client.gui.custom.api.modules.Module module) {
        if (module.delegate != null) return build(module.delegate);
        TextSetting.unfocusAll();
        List<Setting> result = new ArrayList<>();
        for (var model : module.getSettings().all()) {
            Setting widget = create(model);
            if (widget != null) result.add(widget);
        }
        return result;
    }

    public static List<Setting> build(IModule module) {
        TextSetting.unfocusAll();
        ArrayList<Setting> arrayList = new ArrayList<Setting>();
        if (shouldHaveToggleKeybind(module)) {
            var toggleBind = new activity.client.gui.custom.api.modules.settings.impl.BindSetting(
                "Клавиша включения",
                "Клавиша для быстрого включения и выключения модуля"
            );
            toggleBind.setTextProviders(
                () -> activity.client.i18n.LocalizationService.isRussianPreferred() ? "Клавиша включения" : "Toggle Keybind",
                () -> activity.client.i18n.LocalizationService.isRussianPreferred() ? "Клавиша для быстрого включения и выключения модуля" : "Keybind to toggle module on or off"
            );
            toggleBind.setBoundKeybind(module.getKeybind());
            toggleBind.setChangeListener(() -> {
                module.saveToConfig(activity.client.config.ActivityConfigManager.getConfig());
                activity.client.config.CooldownConfigManager.syncToModules(activity.client.config.ActivityConfigManager.getConfig());
                activity.client.config.ActivityConfigManager.markDirty();
                activity.client.config.ActivityConfigManager.save();
                activity.client.module.keybind.KeybindManager.rebuildBoundKeybinds();
            });
            Setting widget = create(toggleBind);
            if (widget != null) arrayList.add(widget);
        }
        for (activity.client.gui.custom.api.modules.settings.Setting setting : activity.client.gui.custom.SettingsBridge.models(module)) {
            Setting setting2 = SettingsFactory.create(setting);
            if (setting2 == null) continue;
            arrayList.add(setting2);
        }
        return arrayList;
    }
}

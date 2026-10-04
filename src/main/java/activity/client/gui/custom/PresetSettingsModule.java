package activity.client.gui.custom;

public final class PresetSettingsModule extends activity.client.gui.custom.api.modules.Module {
    public PresetSettingsModule() { super("Presets", "Локальные пресеты, импорт и экспорт с предпросмотром", activity.client.gui.custom.api.modules.Category.DISPLAY); }
    @Override public String getDisplayName() { return activity.client.i18n.LocalizationService.isRussianPreferred() ? "Пресеты" : "Presets"; }
    @Override public String getDescription() { return activity.client.i18n.LocalizationService.isRussianPreferred() ? "Локальные файлы настроек, выбор разделов и экспорт" : "Local settings files, section selection and export"; }
    @Override public void toggle() {}
}

package activity.client.gui.custom;

import activity.client.diagnostic.CapabilityLogManager;
import activity.client.gui.custom.api.modules.Category;
import activity.client.gui.custom.api.modules.Module;
import activity.client.gui.custom.api.modules.settings.impl.BooleanSetting;
import activity.client.gui.custom.api.modules.settings.impl.ButtonSetting;
import activity.client.gui.custom.api.modules.settings.impl.SeparatorSetting;
import activity.client.gui.custom.api.modules.settings.impl.TextSetting;
import activity.client.i18n.LocalizationService;

public final class CapabilityLogsModule extends Module {
    public static final String ID = "capability_logs";

    private final BooleanSetting loggingEnabled;
    private final BooleanSetting hudFeedback;
    private final ButtonSetting copyButton;
    private final ButtonSetting clearButton;
    private final ButtonSetting openFolderButton;

    public CapabilityLogsModule() {
        super(ID, "Полная запись использованных способностей и копирование в буфер", Category.DISPLAY);

        register(new SeparatorSetting("Управление логированием"));

        this.loggingEnabled = register(new BooleanSetting(
                "Запись способностей",
                "Фиксировать все активации способностей, задержки, углы и результаты.",
                CapabilityLogManager.isLoggingEnabled()
        ));
        this.loggingEnabled.setChangeListener(() -> CapabilityLogManager.setLoggingEnabled(this.loggingEnabled.getValue()));

        this.hudFeedback = register(new BooleanSetting(
                "Actionbar уведомления",
                "Выводить живой статус действий над хотбаром во время игры.",
                CapabilityLogManager.isHudFeedbackEnabled()
        ));
        this.hudFeedback.setChangeListener(() -> CapabilityLogManager.setHudFeedbackEnabled(this.hudFeedback.getValue()));

        register(new SeparatorSetting("Действия с логами"));

        this.copyButton = register(new ButtonSetting(
                "Копировать все логи",
                "Скопировать полный отчет по всем использованным способностям в буфер обмена."
        ).label("Копировать в буфер").onClick(this::copyLogsToClipboard));

        this.clearButton = register(new ButtonSetting(
                "Очистить историю",
                "Сбросить накопленную историю действий и очистить лог-файл."
        ).label("Очистить").onClick(CapabilityLogManager::clear));

        this.openFolderButton = register(new ButtonSetting(
                "Папка логов",
                "Открыть системную папку logs в проводнике."
        ).label("Открыть папку").onClick(CapabilityLogManager::openLogsFolder));
    }

    public void copyLogsToClipboard() {
        CapabilityLogManager.copyToClipboard();
    }

    @Override
    public String getDisplayName() {
        return LocalizationService.isRussianPreferred() ? "Логи способностей" : "Capability Logs";
    }

    @Override
    public String getDescription() {
        return LocalizationService.isRussianPreferred()
                ? "Запись действий всех способностей • копирование полного лога в буфер"
                : "Full capability execution logs • copy to clipboard";
    }

    @Override
    public void toggle() {
        copyLogsToClipboard();
    }
}

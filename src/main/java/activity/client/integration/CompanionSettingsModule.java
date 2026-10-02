package activity.client.integration;

import activity.client.gui.custom.api.modules.Category;
import activity.client.gui.custom.api.modules.Module;

public final class CompanionSettingsModule extends Module {
    private final NivoratEcosystem.Section section;

    public CompanionSettingsModule(NivoratEcosystem.Section section) {
        super("companion_" + section.group(), "", Category.DISPLAY);
        this.section = section;
    }

    public String group() { return section.group(); }
    @Override public String getDisplayName() {
        return activity.client.i18n.LocalizationService.isRussianPreferred() ? section.titleRu() : section.titleEn();
    }
    @Override public String getDescription() {
        return activity.client.i18n.LocalizationService.isRussianPreferred()
                ? "Общие настройки экосистемы Nivorat. Открыть в дополнении."
                : "Shared Nivorat settings. Open in the companion.";
    }
    @Override public void toggle() {}
}

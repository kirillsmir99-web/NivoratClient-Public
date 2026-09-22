package activity.client.module.setting;

import net.minecraft.text.Text;

public enum SettingGroup {
    GENERAL("activity.group.general", "Основные"),
    BEHAVIOR("activity.group.behavior", "Поведение"),
    EXTRA("activity.group.extra", "Дополнительно"),
    HUD("activity.group.hud", "HUD и интерфейс"),
    ADVANCED("activity.group.advanced", "Расширенные");

    private final String translationKey;
    private final String fallbackName;

    SettingGroup(String translationKey, String fallbackName) {
        this.translationKey = translationKey;
        this.fallbackName = fallbackName;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public Text getTitle() {
        return Text.translatable(translationKey);
    }

    public String getFallbackName() {
        return fallbackName;
    }

    public SettingSection toSection() {
        return switch (this) {
            case GENERAL -> SettingSection.GENERAL;
            case BEHAVIOR -> SettingSection.BEHAVIOR;
            case HUD, EXTRA -> SettingSection.HUD;
            case ADVANCED -> SettingSection.ADVANCED;
        };
    }
}

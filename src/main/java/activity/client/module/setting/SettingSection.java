package activity.client.module.setting;

import net.minecraft.text.Text;

public enum SettingSection {
    GENERAL("activity.group.general", "Основные"),
    BEHAVIOR("activity.group.behavior", "Поведение"),
    ADVANCED("activity.group.advanced", "Расширенные"),
    HUD("activity.group.hud", "HUD и интерфейс");

    private final String translationKey;
    private final String fallbackName;

    SettingSection(String translationKey, String fallbackName) {
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

    public SettingGroup toGroup() {
        return switch (this) {
            case GENERAL -> SettingGroup.GENERAL;
            case BEHAVIOR -> SettingGroup.BEHAVIOR;
            case ADVANCED -> SettingGroup.ADVANCED;
            case HUD -> SettingGroup.HUD;
        };
    }
}

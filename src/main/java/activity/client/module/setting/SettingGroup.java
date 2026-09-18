package activity.client.module.setting;

import net.minecraft.text.Text;

/**
 * Standardized logical grouping for module settings.
 *
 * <p>Enforces a consistent vertical order across all module cards:
 * <ol>
 *     <li>Status & Keybind (header)</li>
 *     <li>GENERAL (Основные настройки)</li>
 *     <li>BEHAVIOR (Поведение и боевые тайминги)</li>
 *     <li>EXTRA (Дополнительно, фильтры и визуал)</li>
 *     <li>ADVANCED (Опасные / расширенные настройки)</li>
 * </ol>
 */
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

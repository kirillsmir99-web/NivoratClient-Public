package activity.client.module.setting;

import net.minecraft.text.Text;

/**
 * Standardized unified sections for NivoratClient module settings.
 *
 * Enforces clean, logical separation of options across all module configuration views:
 * <ul>
 *     <li>GENERAL: Primary switches, source modes, target selectors</li>
 *     <li>BEHAVIOR: Operational timings, delays, distances, chances</li>
 *     <li>ADVANCED: Anti-cheat tolerances, safety modes, fine tuning</li>
 *     <li>HUD: Visual overlays, indicators, screen editors</li>
 * </ul>
 */
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

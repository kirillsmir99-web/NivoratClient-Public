package activity.client.module.api;

import net.minecraft.text.Text;

/**
 * Functional categories for Activity modules.
 */
public enum ModuleCategory {
    COMBAT("activity.category.combat", "Оружие и свапы"),
    DEFENSE("activity.category.defense", "Защита и карты"),
    UTILITY("activity.category.utility", "Утилиты и HUD"),
    UTILITY_HUD("activity.category.utility", "Утилиты и HUD"),
    CONFIG("activity.category.config", "Профили и бинды");

    private final String translationKey;
    private final String defaultTitle;

    ModuleCategory(String translationKey, String defaultTitle) {
        this.translationKey = translationKey;
        this.defaultTitle = defaultTitle;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public Text getDisplayText() {
        return Text.translatable(this.translationKey);
    }

    public String getDefaultTitle() {
        return this.defaultTitle;
    }

    public String getId() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}

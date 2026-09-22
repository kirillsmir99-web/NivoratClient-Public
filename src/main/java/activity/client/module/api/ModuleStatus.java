package activity.client.module.api;

import net.minecraft.text.Text;

public enum ModuleStatus {
    READY("activity.status.ready", "Готов", 0xFF36B37E),
    STUB_PENDING_CORE("activity.status.stub", "Заглушка (ожидает ядро)", 0xFFE5A93C),
    DISABLED("activity.status.disabled", "Отключен", 0xFF8D94A3);

    private final String translationKey;
    private final String defaultLabel;
    private final int color;

    ModuleStatus(String translationKey, String defaultLabel, int color) {
        this.translationKey = translationKey;
        this.defaultLabel = defaultLabel;
        this.color = color;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public Text getDisplayText() {
        return Text.translatable(this.translationKey);
    }

    public String getDefaultLabel() {
        return this.defaultLabel;
    }

    public int getColor() {
        return this.color;
    }
}

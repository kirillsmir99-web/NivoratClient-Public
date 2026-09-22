package activity.client.module.setting;

import java.util.Locale;

public enum NumberUnit {
    NONE("", ""),
    MS("ms", " ms"),
    TICKS("ticks", " ticks"),
    PERCENT("%", "%"),
    HP("HP", " HP"),
    BLOCKS("blocks", " bl"),
    CPS("CPS", " CPS");

    private final String id;
    private final String suffix;

    NumberUnit(String id, String suffix) {
        this.id = id;
        this.suffix = suffix;
    }

    public String getId() {
        return id;
    }

    public String getSuffix() {
        return suffix;
    }

    public static NumberUnit fromString(String raw) {
        if (raw == null || raw.isBlank()) return NONE;
        String s = raw.trim().toLowerCase(Locale.ROOT);
        return switch (s) {
            case "ms", "мс" -> MS;
            case "ticks", "tick", "тики", "тиков", "т" -> TICKS;
            case "%", "percent", "процент", "процентов" -> PERCENT;
            case "hp", "хп", "hearts", "heart", "сердец" -> HP;
            case "blocks", "block", "bl", "блоки", "блоков", "б" -> BLOCKS;
            case "cps", "кпс" -> CPS;
            default -> NONE;
        };
    }

    public String format(double value, boolean integerOnly) {
        boolean isWhole = integerOnly || Math.abs(value - Math.round(value)) < 1e-6;
        String numStr = isWhole
                ? String.format(Locale.ROOT, "%.0f", value)
                : String.format(Locale.ROOT, "%.1f", value);
        return numStr + suffix;
    }
}

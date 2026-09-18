package activity.client.module.setting;

import java.util.Locale;

/**
 * Standard unit types for numeric settings in NivoratClient.
 *
 * <p>Automates formatted text representation with proper suffixes and spacing,
 * ensuring UI components do not have to manually craft display strings.
 */
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

    /**
     * Resolves a NumberUnit from arbitrary legacy string inputs (e.g., "ms", " ms", "%", "bl", "blocks").
     */
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

    /**
     * Formats a double value with the unit suffix.
     *
     * @param value       numeric value
     * @param integerOnly whether to omit decimal places
     * @return formatted string (e.g. "90 ms", "2.5 bl", "75%")
     */
    public String format(double value, boolean integerOnly) {
        String numStr = integerOnly
                ? String.format(Locale.ROOT, "%.0f", value)
                : String.format(Locale.ROOT, "%.1f", value);
        return numStr + suffix;
    }
}

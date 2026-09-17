package activity.client.gui.font;

/**
 * Supported typography sizes across the GUI framework.
 * Encapsulates scale factor, base height, and translation key.
 */
public enum TypographySize {
    SMALL("small", "activity.typography.size.small", 0.90f, 8),
    NORMAL("normal", "activity.typography.size.normal", 1.00f, 9),
    LARGE("large", "activity.typography.size.large", 1.15f, 11);

    private final String id;
    private final String translationKey;
    private final float scaleFactor;
    private final int baseFontHeight;

    TypographySize(String id, String translationKey, float scaleFactor, int baseFontHeight) {
        this.id = id;
        this.translationKey = translationKey;
        this.scaleFactor = scaleFactor;
        this.baseFontHeight = baseFontHeight;
    }

    public String getId() {
        return this.id;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public float getScaleFactor() {
        return this.scaleFactor;
    }

    public int getBaseFontHeight() {
        return this.baseFontHeight;
    }

    /**
     * Resolves a TypographySize from its string identifier with graceful fallback to NORMAL.
     *
     * @param id typography size identifier ("small", "normal", "large")
     * @return matching TypographySize or NORMAL if null or unrecognized
     */
    public static TypographySize fromId(String id) {
        if (id == null || id.isBlank()) {
            return NORMAL;
        }
        String normalized = id.trim().toLowerCase();
        for (TypographySize size : values()) {
            if (size.id.equals(normalized)) {
                return size;
            }
        }
        return NORMAL;
    }
}

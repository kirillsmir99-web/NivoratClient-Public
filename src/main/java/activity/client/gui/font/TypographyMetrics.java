package activity.client.gui.font;

import java.util.EnumMap;
import java.util.Map;

public final class TypographyMetrics {

    private static final Map<FontFamily, Map<TypographySize, TypographyMetrics>> CACHE = new EnumMap<>(FontFamily.class);

    static {
        for (FontFamily family : FontFamily.values()) {
            Map<TypographySize, TypographyMetrics> sizeMap = new EnumMap<>(TypographySize.class);
            for (TypographySize size : TypographySize.values()) {
                sizeMap.put(size, calculateMetrics(family, size));
            }
            CACHE.put(family, sizeMap);
        }
    }

    private final FontFamily fontFamily;
    private final TypographySize size;
    private final int lineHeight;
    private final float verticalOffset;
    private final int baseline;
    private final int horizontalPadding;
    private final float scaleFactor;

    public TypographyMetrics(FontFamily fontFamily, TypographySize size, int lineHeight,
                             float verticalOffset, int baseline, int horizontalPadding, float scaleFactor) {
        this.fontFamily = fontFamily;
        this.size = size;
        this.lineHeight = lineHeight;
        this.verticalOffset = verticalOffset;
        this.baseline = baseline;
        this.horizontalPadding = horizontalPadding;
        this.scaleFactor = scaleFactor;
    }

    public static TypographyMetrics get(FontFamily family, TypographySize size) {
        FontFamily resolvedFamily = family != null ? family : FontFamily.ONEST;
        TypographySize resolvedSize = size != null ? size : TypographySize.NORMAL;

        Map<TypographySize, TypographyMetrics> sizeMap = CACHE.get(resolvedFamily);
        if (sizeMap != null) {
            TypographyMetrics metrics = sizeMap.get(resolvedSize);
            if (metrics != null) {
                return metrics;
            }
        }
        return calculateMetrics(resolvedFamily, resolvedSize);
    }

    private static TypographyMetrics calculateMetrics(FontFamily family, TypographySize size) {
        float scale = size.getScaleFactor();
        int baseHeight = 10;
        float vOffset = 0.0f;
        int baseline = 8;
        int hPadding = 0;

        switch (family != null ? family : FontFamily.ONEST) {
            case MINECRAFT, DEFAULT, RETRO_PIXEL -> {
                baseHeight = 9;
                vOffset = 0.0f;
                baseline = 7;
            }
            case ONEST -> {
                baseHeight = 10;
                vOffset = 0.5f;
                baseline = 8;
            }
            case INTER -> {
                baseHeight = 10;
                vOffset = 0.5f;
                baseline = 8;
            }
            case MANROPE -> {
                baseHeight = 10;
                vOffset = 0.0f;
                baseline = 8;
            }
            case RUBIK -> {
                baseHeight = 10;
                vOffset = 0.0f;
                baseline = 8;
            }
            default -> {
                baseHeight = 10;
                vOffset = 0.0f;
                baseline = 8;
            }
        }

        int calculatedLineHeight;
        if (size == TypographySize.SMALL) {
            calculatedLineHeight = Math.max(7, Math.round(baseHeight * 0.90f));
            baseline = Math.max(6, Math.round(baseline * 0.90f));
        } else if (size == TypographySize.LARGE) {
            calculatedLineHeight = Math.round(baseHeight * 1.15f);
            baseline = Math.round(baseline * 1.15f);
        } else {
            calculatedLineHeight = baseHeight;
        }

        return new TypographyMetrics(family, size, calculatedLineHeight, vOffset, baseline, hPadding, scale);
    }

    public FontFamily getFontFamily() {
        return fontFamily;
    }

    public TypographySize getSize() {
        return size;
    }

    public int getLineHeight() {
        return lineHeight;
    }

    public float getVerticalOffset() {
        return verticalOffset;
    }

    public int getBaseline() {
        return baseline;
    }

    public int getHorizontalPadding() {
        return horizontalPadding;
    }

    public float getScaleFactor() {
        return scaleFactor;
    }

    public int getAdjustedY(int y) {
        return Math.round(y + verticalOffset);
    }

    public int getCenterY(int parentY, int parentHeight) {
        return parentY + (parentHeight - lineHeight) / 2 + Math.round(verticalOffset);
    }
}

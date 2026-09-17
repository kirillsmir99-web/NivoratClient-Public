package activity.client.gui.font;

import activity.client.gui.theme.ActivityColors;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Centralized typography rendering and text measurement abstraction layer for NivoratClient.
 *
 * <p>Ensures exact visual and measurement synchronization across all typography scales
 * (SMALL = 0.90x, NORMAL = 1.00x, LARGE = 1.15x).
 * Scales matrix transformations during DrawContext operations and scales font measurements
 * so UI components, card heights, dropdowns, and buttons remain perfectly aligned without overlaps.
 */
public final class UiTextRenderer {

    private UiTextRenderer() {}

    /**
     * Returns the active typography scale factor (e.g. 0.90f, 1.00f, 1.15f).
     */
    public static float getScaleFactor() {
        return NivoratFontManager.getActiveTypographySize().getScaleFactor();
    }

    /**
     * Computes the rendered, scale-adjusted width of a Text component.
     */
    public static int getWidth(TextRenderer tr, Text text) {
        if (tr == null || text == null) return 0;
        return NivoratFontManager.getWidth(tr, text);
    }

    /**
     * Computes the rendered, scale-adjusted width of a plain String.
     */
    public static int getWidth(TextRenderer tr, String text) {
        if (tr == null || text == null || text.isEmpty()) return 0;
        return NivoratFontManager.getWidth(tr, text);
    }

    /**
     * Returns the scaled font height based on active typography size.
     */
    public static int getFontHeight(TextRenderer tr) {
        float scale = getScaleFactor();
        int baseHeight = tr != null ? tr.fontHeight : 9;
        return Math.max(1, Math.round(baseHeight * scale));
    }

    /**
     * Returns the active line height from typography metrics.
     */
    public static int getLineHeight() {
        return NivoratFontManager.getMetrics().getLineHeight();
    }

    /**
     * Returns the vertical offset for baseline adjustment.
     */
    public static float getVerticalOffset() {
        return NivoratFontManager.getMetrics().getVerticalOffset();
    }

    /**
     * Vertically centers text inside a container of given height.
     */
    public static int getCenterY(int parentY, int parentHeight, TextRenderer tr) {
        return parentY + (parentHeight - getFontHeight(tr)) / 2 + Math.round(getVerticalOffset());
    }

    /**
     * Renders a styled Text component with active typography scaling applied.
     */
    public static void drawText(DrawContext context, TextRenderer tr, Text text, int x, int y, int color, boolean shadow) {
        if (context == null || tr == null || text == null) return;
        Text wrapped = NivoratFontManager.wrap(text);
        float scale = getScaleFactor();

        if (Math.abs(scale - 1.0f) < 0.001f) {
            if (shadow) {
                context.drawTextWithShadow(tr, wrapped, x, y, color);
            } else {
                context.drawText(tr, wrapped, x, y, color, false);
            }
        } else {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float) x, (float) y);
            context.getMatrices().scale(scale, scale);
            if (shadow) {
                context.drawTextWithShadow(tr, wrapped, 0, 0, color);
            } else {
                context.drawText(tr, wrapped, 0, 0, color, false);
            }
            context.getMatrices().popMatrix();
        }
    }

    /**
     * Renders a plain string literal with active font and typography scaling applied.
     */
    public static void drawText(DrawContext context, TextRenderer tr, String text, int x, int y, int color, boolean shadow) {
        drawText(context, tr, NivoratFontManager.literal(text), x, y, color, shadow);
    }

    /**
     * Renders text with drop shadow using active font and scaling.
     */
    public static void drawTextWithShadow(DrawContext context, TextRenderer tr, Text text, int x, int y, int color) {
        drawText(context, tr, text, x, y, color, true);
    }

    /**
     * Renders plain string literal with drop shadow using active font and scaling.
     */
    public static void drawTextWithShadow(DrawContext context, TextRenderer tr, String text, int x, int y, int color) {
        drawText(context, tr, text, x, y, color, true);
    }

    /**
     * Renders horizontally centered text taking active scaling into account.
     */
    public static void drawCenteredText(DrawContext context, TextRenderer tr, Text text, int centerX, int y, int color, boolean shadow) {
        if (context == null || tr == null || text == null) return;
        int width = getWidth(tr, text);
        drawText(context, tr, text, centerX - width / 2, y, color, shadow);
    }

    /**
     * Renders horizontally centered text with shadow taking active scaling into account.
     */
    public static void drawCenteredTextWithShadow(DrawContext context, TextRenderer tr, Text text, int centerX, int y, int color) {
        drawCenteredText(context, tr, text, centerX, y, color, true);
    }

    /**
     * Renders an OrderedText line with active typography scaling applied.
     */
    public static void drawOrderedText(DrawContext context, TextRenderer tr, OrderedText text, int x, int y, int color, boolean shadow) {
        if (context == null || tr == null || text == null) return;
        float scale = getScaleFactor();

        if (Math.abs(scale - 1.0f) < 0.001f) {
            if (shadow) {
                context.drawTextWithShadow(tr, text, x, y, color);
            } else {
                context.drawText(tr, text, x, y, color, false);
            }
        } else {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float) x, (float) y);
            context.getMatrices().scale(scale, scale);
            if (shadow) {
                context.drawTextWithShadow(tr, text, 0, 0, color);
            } else {
                context.drawText(tr, text, 0, 0, color, false);
            }
            context.getMatrices().popMatrix();
        }
    }

    /**
     * Wraps lines of text so each rendered line fits within maxWidth at current typography scale.
     */
    public static List<OrderedText> wrapLines(TextRenderer tr, Text text, int maxWidth) {
        if (tr == null || text == null) return List.of();
        float scale = getScaleFactor();
        int unscaledMaxW = scale > 0.001f ? Math.max(1, Math.round(maxWidth / scale)) : maxWidth;
        return tr.wrapLines(NivoratFontManager.wrap(text), unscaledMaxW);
    }

    /**
     * Trims plain text string to fit inside maxWidth at current typography scale.
     */
    public static String trimToWidth(TextRenderer tr, String text, int maxWidth) {
        if (tr == null || text == null) return "";
        float scale = getScaleFactor();
        int unscaledMaxW = scale > 0.001f ? Math.max(1, Math.round(maxWidth / scale)) : maxWidth;
        return tr.trimToWidth(text, unscaledMaxW);
    }

    /**
     * Trims Text component to fit inside maxWidth at current typography scale.
     */
    public static Text trimToWidth(TextRenderer tr, Text text, int maxWidth) {
        if (tr == null || text == null) return Text.empty();
        float scale = getScaleFactor();
        int unscaledMaxW = scale > 0.001f ? Math.max(1, Math.round(maxWidth / scale)) : maxWidth;
        String trimmed = tr.trimToWidth(text.getString(), unscaledMaxW);
        return NivoratFontManager.literal(trimmed);
    }
}

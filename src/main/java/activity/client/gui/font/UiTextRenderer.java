package activity.client.gui.font;

import activity.client.gui.theme.ActivityColors;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.List;

public final class UiTextRenderer {

    private UiTextRenderer() {}

    public static float getScaleFactor() {
        return CooldownFontManager.getActiveTypographySize().getScaleFactor();
    }

    public static int getWidth(TextRenderer tr, Text text) {
        if (tr == null || text == null) return 0;
        return CooldownFontManager.getWidth(tr, text);
    }

    public static int getWidth(TextRenderer tr, String text) {
        if (tr == null || text == null || text.isEmpty()) return 0;
        return CooldownFontManager.getWidth(tr, text);
    }

    public static int getFontHeight(TextRenderer tr) {
        float scale = getScaleFactor();
        int baseHeight = tr != null ? tr.fontHeight : 9;
        return Math.max(1, Math.round(baseHeight * scale));
    }

    public static int getLineHeight() {
        return CooldownFontManager.getMetrics().getLineHeight();
    }

    public static float getVerticalOffset() {
        return CooldownFontManager.getMetrics().getVerticalOffset();
    }

    public static int getCenterY(int parentY, int parentHeight, TextRenderer tr) {
        return parentY + (parentHeight - getFontHeight(tr)) / 2 + Math.round(getVerticalOffset());
    }

    public static void drawText(DrawContext context, TextRenderer tr, Text text, int x, int y, int color, boolean shadow) {
        if (context == null || tr == null || text == null) return;
        Text wrapped = CooldownFontManager.wrap(text);
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

    public static void drawText(DrawContext context, TextRenderer tr, String text, int x, int y, int color, boolean shadow) {
        drawText(context, tr, CooldownFontManager.literal(text), x, y, color, shadow);
    }

    public static void drawTextWithShadow(DrawContext context, TextRenderer tr, Text text, int x, int y, int color) {
        drawText(context, tr, text, x, y, color, true);
    }

    public static void drawTextWithShadow(DrawContext context, TextRenderer tr, String text, int x, int y, int color) {
        drawText(context, tr, text, x, y, color, true);
    }

    public static void drawCenteredText(DrawContext context, TextRenderer tr, Text text, int centerX, int y, int color, boolean shadow) {
        if (context == null || tr == null || text == null) return;
        int width = getWidth(tr, text);
        drawText(context, tr, text, centerX - width / 2, y, color, shadow);
    }

    public static void drawCenteredTextWithShadow(DrawContext context, TextRenderer tr, Text text, int centerX, int y, int color) {
        drawCenteredText(context, tr, text, centerX, y, color, true);
    }

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

    public static List<OrderedText> wrapLines(TextRenderer tr, Text text, int maxWidth) {
        if (tr == null || text == null) return List.of();
        float scale = getScaleFactor();
        int unscaledMaxW = scale > 0.001f ? Math.max(1, Math.round(maxWidth / scale)) : maxWidth;
        return tr.wrapLines(CooldownFontManager.wrap(text), unscaledMaxW);
    }

    public static String trimToWidth(TextRenderer tr, String text, int maxWidth) {
        if (tr == null || text == null) return "";
        float scale = getScaleFactor();
        int unscaledMaxW = scale > 0.001f ? Math.max(1, Math.round(maxWidth / scale)) : maxWidth;
        return tr.trimToWidth(text, unscaledMaxW);
    }

    public static Text trimToWidth(TextRenderer tr, Text text, int maxWidth) {
        if (tr == null || text == null) return Text.empty();
        float scale = getScaleFactor();
        int unscaledMaxW = scale > 0.001f ? Math.max(1, Math.round(maxWidth / scale)) : maxWidth;
        String trimmed = tr.trimToWidth(text.getString(), unscaledMaxW);
        return CooldownFontManager.literal(trimmed);
    }
}

package activity.client.gui.render;

import activity.client.gui.theme.ActivityColors;
import activity.client.gui.theme.ActivityMetrics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.text.Text;

public final class ActivityGuiRenderer {
    private ActivityGuiRenderer() {}

    public static void playClickSound() {
        activity.client.gui.sound.SoundManager.playClick();
    }

    public static void fill(DrawContext context, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) return;
        context.fill(x, y, x + width, y + height, color);
    }

    public static void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) return;

        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);

        if (height > 2) {
            context.fill(x, y + 1, x + 1, y + height - 1, color);
            context.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
        }
    }

    public static void drawGlassHighlight(DrawContext context, int x, int y, int width, int height, float alphaFactor) {
        if (width <= 2 || height <= 2 || alphaFactor <= 0.005f) return;
        int primary = ActivityColors.scaleAlpha(ActivityColors.GLASS_HIGHLIGHT_PRIMARY, alphaFactor);
        int secondary = ActivityColors.scaleAlpha(ActivityColors.GLASS_HIGHLIGHT_SECONDARY, alphaFactor);

        context.fill(x + 1, y + 1, x + width - 1, y + 2, primary);

        if (height > 3) {
            context.fill(x + 1, y + 2, x + 2, y + height - 1, secondary);
        }

        int cornerGlint = ActivityColors.scaleAlpha(0x40FFFFFF, alphaFactor);
        context.fill(x + 1, y + 1, x + 2, y + 2, cornerGlint);
    }

    public static void drawGlassHighlight(DrawContext context, int x, int y, int width, int height) {
        drawGlassHighlight(context, x, y, width, height, 1.0f);
    }

    public static void drawPanel(DrawContext context, int x, int y, int width, int height, int bgColor, int borderColor, boolean glassEffect) {
        fill(context, x, y, width, height, bgColor);
        drawBorder(context, x, y, width, height, borderColor);
        if (glassEffect) {
            float alpha = (float) (borderColor >>> 24) / 255.0f;
            drawGlassHighlight(context, x, y, width, height, alpha);
            int innerRefract = ActivityColors.scaleAlpha(0x15000000, alpha);
            context.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, innerRefract);
            context.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, innerRefract);
        }
    }

    public static void drawPanel(DrawContext context, int x, int y, int width, int height, int bgColor, int borderColor) {
        boolean glass = activity.client.config.ActivityConfigManager.getConfig() != null &&
                        activity.client.config.ActivityConfigManager.getConfig().glassEffect;
        drawPanel(context, x, y, width, height, bgColor, borderColor, glass);
    }

    public static void drawWindowFrame(DrawContext context, int x, int y, int width, int height, int bgColor, int borderColor, boolean glassEffect) {
        float alpha = (float) (borderColor >>> 24) / 255.0f;
        if (glassEffect) {
            int shadow1 = ActivityColors.scaleAlpha(ActivityColors.SHADOW_SUBTLE, alpha);
            int shadow2 = ActivityColors.scaleAlpha(0x18000000, alpha);
            drawBorder(context, x - 2, y - 2, width + 4, height + 4, shadow2);
            drawBorder(context, x - 1, y - 1, width + 2, height + 2, shadow1);
        }
        fill(context, x, y, width, height, bgColor);
        drawBorder(context, x, y, width, height, borderColor);
        if (glassEffect) {
            drawGlassHighlight(context, x, y, width, height, alpha);
            int innerRefract = ActivityColors.scaleAlpha(0x20000000, alpha);
            context.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, innerRefract);
            context.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, innerRefract);
        }
    }

    public static void drawHorizontalLine(DrawContext context, int x, int y, int width, int color) {
        if (width <= 0) return;
        context.fill(x, y, x + width, y + 1, color);
    }

    public static void drawVerticalLine(DrawContext context, int x, int y, int height, int color) {
        if (height <= 0) return;
        context.fill(x, y, x + 1, y + height, color);
    }

    public static void drawHeader(DrawContext context, int x, int y, int width, int height, Text title, TextRenderer textRenderer) {
        fill(context, x, y, width, height, ActivityColors.HEADER_BACKGROUND);
        drawHorizontalLine(context, x, y + height - 1, width, ActivityColors.BORDER);

        int fontH = activity.client.gui.font.UiTextRenderer.getFontHeight(textRenderer);
        int textY = y + (height - fontH) / 2;
        activity.client.gui.font.UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, title, x + width / 2, textY, ActivityColors.TEXT_PRIMARY);
    }

    public static void drawSidebarItem(DrawContext context, int x, int y, int width, int height,
                                       activity.client.gui.icon.ActivityIcon icon,
                                       Text label, boolean selected, float hoverProgress, TextRenderer textRenderer) {
        if (selected) {
            fill(context, x, y, width, height, ActivityColors.ITEM_SELECTED_BG);
            fill(context, x, y, ActivityMetrics.INDICATOR_WIDTH, height, ActivityColors.ITEM_SELECTED_BAR);
        } else if (hoverProgress > 0.001f) {
            int maxAlpha = (ActivityColors.ITEM_HOVER_BG >>> 24) & 0xFF;
            int alpha = (int) (maxAlpha * hoverProgress);
            int hoverColor = (alpha << 24) | (ActivityColors.ITEM_HOVER_BG & 0x00FFFFFF);
            fill(context, x, y, width, height, hoverColor);
        }

        int textColor;
        if (selected) {
            textColor = ActivityColors.TEXT_ACCENT;
        } else if (hoverProgress > 0.001f) {
            textColor = ActivityColors.interpolateColor(ActivityColors.TEXT_SECONDARY, ActivityColors.TEXT_PRIMARY, hoverProgress);
        } else {
            textColor = ActivityColors.TEXT_SECONDARY;
        }

        int textX = x + ActivityMetrics.PADDING_WINDOW + 2;
        if (icon != null) {
            int iconY = y + (height - icon.getHeight()) / 2;
            activity.client.gui.icon.ActivityIconRenderer.draw(context, icon, textX, iconY, textColor);
            textX += icon.getWidth() + 6;
        }

        int fontH = activity.client.gui.font.UiTextRenderer.getFontHeight(textRenderer);
        int textY = y + (height - fontH) / 2;
        activity.client.gui.font.UiTextRenderer.drawTextWithShadow(context, textRenderer, label, textX, textY, textColor);
    }

    public static void drawSidebarItem(DrawContext context, int x, int y, int width, int height,
                                       Text label, boolean selected, float hoverProgress, TextRenderer textRenderer) {
        drawSidebarItem(context, x, y, width, height, null, label, selected, hoverProgress, textRenderer);
    }

    public static void drawSidebarItem(DrawContext context, int x, int y, int width, int height,
                                       Text label, boolean selected, boolean hovered, TextRenderer textRenderer) {
        drawSidebarItem(context, x, y, width, height, label, selected, hovered ? 1.0f : 0.0f, textRenderer);
    }

    public static void drawText(DrawContext context, TextRenderer tr, Text text, int x, int y, int color) {
        activity.client.gui.font.UiTextRenderer.drawTextWithShadow(context, tr, text, x, y, color);
    }

    public static void drawText(DrawContext context, TextRenderer tr, String text, int x, int y, int color) {
        activity.client.gui.font.UiTextRenderer.drawTextWithShadow(context, tr, text, x, y, color);
    }

    public static int getTextWidth(TextRenderer tr, Text text) {
        return activity.client.gui.font.UiTextRenderer.getWidth(tr, text);
    }

    public static int getTextWidth(TextRenderer tr, String text) {
        return activity.client.gui.font.UiTextRenderer.getWidth(tr, text);
    }
}

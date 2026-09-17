package activity.client.gui.render;

import activity.client.gui.theme.ActivityColors;
import activity.client.gui.theme.ActivityMetrics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.text.Text;

/**
 * Dedicated renderer for software-rendered panels, 1px borders, and headers.
 * Uses vanilla Minecraft 1.21.11 DrawContext routines without legacy GL calls.
 */
public final class ActivityGuiRenderer {
    private ActivityGuiRenderer() {}

    /**
     * Plays tactile UI click sound via SoundManager.
     */
    public static void playClickSound() {
        activity.client.gui.sound.SoundManager.playClick();
    }

    /**
     * Renders a solid or semi-transparent filled rectangle.
     */
    public static void fill(DrawContext context, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) return;
        context.fill(x, y, x + width, y + height, color);
    }

    /**
     * Renders a crisp 1-pixel border rectangle without gaps or overlapping corner pixels.
     */
    public static void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) return;
        // Top and bottom horizontal lines
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        // Left and right vertical lines
        if (height > 2) {
            context.fill(x, y + 1, x + 1, y + height - 1, color);
            context.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
        }
    }

    /**
     * Renders a 1px glass specular highlight rim (top and left edges) with corner specular glint and explicit alpha scaling.
     */
    public static void drawGlassHighlight(DrawContext context, int x, int y, int width, int height, float alphaFactor) {
        if (width <= 2 || height <= 2 || alphaFactor <= 0.005f) return;
        int primary = ActivityColors.scaleAlpha(ActivityColors.GLASS_HIGHLIGHT_PRIMARY, alphaFactor);
        int secondary = ActivityColors.scaleAlpha(ActivityColors.GLASS_HIGHLIGHT_SECONDARY, alphaFactor);
        // Top edge specular line
        context.fill(x + 1, y + 1, x + width - 1, y + 2, primary);
        // Left edge specular line
        if (height > 3) {
            context.fill(x + 1, y + 2, x + 2, y + height - 1, secondary);
        }
        // Top-left corner specular glint
        int cornerGlint = ActivityColors.scaleAlpha(0x40FFFFFF, alphaFactor);
        context.fill(x + 1, y + 1, x + 2, y + 2, cornerGlint);
    }

    /**
     * Renders a 1px glass specular highlight rim (top and left edges).
     */
    public static void drawGlassHighlight(DrawContext context, int x, int y, int width, int height) {
        drawGlassHighlight(context, x, y, width, height, 1.0f);
    }

    /**
     * Renders a panel containing a background fill, a 1px border, and optional glass highlights and refraction rims.
     */
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

    /**
     * Renders a panel containing a background fill and a 1px border, using config glass effect setting.
     */
    public static void drawPanel(DrawContext context, int x, int y, int width, int height, int bgColor, int borderColor) {
        boolean glass = activity.client.config.ActivityConfigManager.getConfig() != null &&
                        activity.client.config.ActivityConfigManager.getConfig().glassEffect;
        drawPanel(context, x, y, width, height, bgColor, borderColor, glass);
    }

    /**
     * Renders the outer window frame with soft multi-layer drop shadow, translucent body, border, and glass highlights.
     */
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

    /**
     * Renders a horizontal divider line.
     */
    public static void drawHorizontalLine(DrawContext context, int x, int y, int width, int color) {
        if (width <= 0) return;
        context.fill(x, y, x + width, y + 1, color);
    }

    /**
     * Renders a vertical divider line.
     */
    public static void drawVerticalLine(DrawContext context, int x, int y, int height, int color) {
        if (height <= 0) return;
        context.fill(x, y, x + 1, y + height, color);
    }

    /**
     * Renders the window header bar with dark styling, title, and bottom divider.
     */
    public static void drawHeader(DrawContext context, int x, int y, int width, int height, Text title, TextRenderer textRenderer) {
        fill(context, x, y, width, height, ActivityColors.HEADER_BACKGROUND);
        drawHorizontalLine(context, x, y + height - 1, width, ActivityColors.BORDER);

        int fontH = activity.client.gui.font.UiTextRenderer.getFontHeight(textRenderer);
        int textY = y + (height - fontH) / 2;
        activity.client.gui.font.UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, title, x + width / 2, textY, ActivityColors.TEXT_PRIMARY);
    }

    /**
     * Renders a sidebar tab item with an icon, smooth hover transition progress, active selection state, and left indicator bar.
     */
    public static void drawSidebarItem(DrawContext context, int x, int y, int width, int height,
                                       activity.client.gui.icon.ActivityIcon icon,
                                       Text label, boolean selected, float hoverProgress, TextRenderer textRenderer) {
        if (selected) {
            fill(context, x, y, width, height, ActivityColors.ITEM_SELECTED_BG);
            fill(context, x, y, ActivityMetrics.INDICATOR_WIDTH, height, ActivityColors.ITEM_SELECTED_BAR);
        } else if (hoverProgress > 0.001f) {
            int maxAlpha = (ActivityColors.ITEM_HOVER_BG >>> 24) & 0xFF; // 0x1A = 26
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

    /**
     * Renders a sidebar tab item with smooth hover transition progress, active selection state, and left indicator bar.
     */
    public static void drawSidebarItem(DrawContext context, int x, int y, int width, int height,
                                       Text label, boolean selected, float hoverProgress, TextRenderer textRenderer) {
        drawSidebarItem(context, x, y, width, height, null, label, selected, hoverProgress, textRenderer);
    }

    /**
     * Renders a sidebar tab item with boolean hover state (backward-compatible).
     */
    public static void drawSidebarItem(DrawContext context, int x, int y, int width, int height,
                                       Text label, boolean selected, boolean hovered, TextRenderer textRenderer) {
        drawSidebarItem(context, x, y, width, height, label, selected, hovered ? 1.0f : 0.0f, textRenderer);
    }

    /**
     * Renders text with the active Activity font and typography scale applied.
     */
    public static void drawText(DrawContext context, TextRenderer tr, Text text, int x, int y, int color) {
        activity.client.gui.font.UiTextRenderer.drawTextWithShadow(context, tr, text, x, y, color);
    }

    /**
     * Renders string literal with the active Activity font and typography scale applied.
     */
    public static void drawText(DrawContext context, TextRenderer tr, String text, int x, int y, int color) {
        activity.client.gui.font.UiTextRenderer.drawTextWithShadow(context, tr, text, x, y, color);
    }

    /**
     * Calculates width of Text under the current active Activity font and typography scale.
     */
    public static int getTextWidth(TextRenderer tr, Text text) {
        return activity.client.gui.font.UiTextRenderer.getWidth(tr, text);
    }

    /**
     * Calculates width of String under the current active Activity font and typography scale.
     */
    public static int getTextWidth(TextRenderer tr, String text) {
        return activity.client.gui.font.UiTextRenderer.getWidth(tr, text);
    }
}

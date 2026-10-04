package activity.client.gui.custom.api.ui.button;

import activity.client.gui.custom.api.ui.theme.ClientAccent;
import activity.client.gui.custom.api.ui.theme.ThemeManager;
import activity.client.gui.custom.utils.color.ColorUtil;
import activity.client.gui.custom.utils.render.fonts.Fonts;
import activity.client.gui.custom.utils.render.render2d.Render2D;
import activity.client.gui.custom.utils.render.render2d.glow.BuiltGlow;
import net.minecraft.client.gui.DrawContext;

public final class UnifiedButton {
    public static final float DEFAULT_HEIGHT = 17.0f;
    public static final float DEFAULT_RADIUS = 4.0f;
    public static final float DEFAULT_PAD_X = 10.0f;
    public static final float DEFAULT_FONT_SIZE = 5.2f;

    public enum Variant {
        PRIMARY,
        SECONDARY,
        DANGER
    }

    private UnifiedButton() {}

    public static float computeWidth(String text, float minWidth) {
        float textW = Fonts.MONTSERRAT_MEDIUM.width(text, DEFAULT_FONT_SIZE);
        return Math.max(minWidth, textW + DEFAULT_PAD_X * 2.0f);
    }

    public static boolean isHovered(float x, float y, float w, float h, float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    public static void render(DrawContext drawContext, float x, float y, float w, float h, String text, Variant variant, float alpha, boolean hovered) {
        if (alpha <= 0.005f || w <= 0.0f || h <= 0.0f) return;
        int bgCol;
        int borderCol;
        int textCol = ColorUtil.rgba(255, 255, 255, Math.round(255.0f * (hovered ? 1.0f : 0.9f) * alpha));

        switch (variant) {
            case PRIMARY -> {
                bgCol = ClientAccent.accent((hovered ? 245.0f : 200.0f) * alpha);
                borderCol = ThemeManager.rgba(0xFFFFFF, (hovered ? 65.0f : 30.0f) * alpha);
                if (hovered) {
                    Render2D.glow(new BuiltGlow(x, y, w, h, new float[]{DEFAULT_RADIUS, DEFAULT_RADIUS, DEFAULT_RADIUS, DEFAULT_RADIUS}, ClientAccent.accent(255.0f), 0.35f, 4.0f, alpha));
                }
            }
            case DANGER -> {
                bgCol = ThemeManager.rgba(0xA51D2D, (hovered ? 240.0f : 190.0f) * alpha);
                borderCol = ThemeManager.rgba(0xFF4D5E, (hovered ? 120.0f : 60.0f) * alpha);
                if (hovered) {
                    Render2D.glow(new BuiltGlow(x, y, w, h, new float[]{DEFAULT_RADIUS, DEFAULT_RADIUS, DEFAULT_RADIUS, DEFAULT_RADIUS}, ThemeManager.rgba(0xFF3344, 200.0f), 0.30f, 4.0f, alpha));
                }
            }
            case SECONDARY -> {
                bgCol = ThemeManager.rgba(0x141822, (hovered ? 210.0f : 140.0f) * alpha);
                borderCol = ThemeManager.rgba(0xFFFFFF, (hovered ? 45.0f : 20.0f) * alpha);
            }
            default -> {
                bgCol = ThemeManager.rgba(0x141822, 140.0f * alpha);
                borderCol = ThemeManager.rgba(0xFFFFFF, 20.0f * alpha);
            }
        }

        Render2D.rect(x, y, w, h, DEFAULT_RADIUS, bgCol);
        Render2D.outline(x, y, w, h, DEFAULT_RADIUS, 0.6f, borderCol);

        float textW = Fonts.MONTSERRAT_MEDIUM.width(text, DEFAULT_FONT_SIZE);
        float textX = x + (w - textW) * 0.5f;
        float textY = y + (h - DEFAULT_FONT_SIZE) * 0.5f + 0.5f;
        Fonts.MONTSERRAT_MEDIUM.draw(text, textX, textY, DEFAULT_FONT_SIZE, textCol);
    }
}

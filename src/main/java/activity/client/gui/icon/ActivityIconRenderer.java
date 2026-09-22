package activity.client.gui.icon;

import activity.client.gui.theme.ActivityColors;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class ActivityIconRenderer {

    public static final Identifier ATLAS_ID = Identifier.of("nivoratclient", "textures/gui/nivorat_icons_atlas.png");
    public static final int ATLAS_WIDTH = 192;
    public static final int ATLAS_HEIGHT = 96;

    private ActivityIconRenderer() {}

    public static int applyAlpha(int argb, float alpha) {
        if (alpha >= 1.0f) return argb;
        if (alpha <= 0.0f) return 0;
        int a = (argb >>> 24) & 0xFF;
        int newA = Math.round(a * Math.clamp(alpha, 0.0f, 1.0f));
        return (newA << 24) | (argb & 0x00FFFFFF);
    }

    public static int resolveColor(int baseTint, boolean hover, boolean selected, boolean disabled) {
        if (disabled) {
            return ActivityColors.TEXT_DISABLED;
        }
        if (selected) {
            return ActivityColors.ACCENT_PRIMARY;
        }
        if (hover) {
            return ActivityColors.TEXT_PRIMARY;
        }
        return (baseTint != 0) ? baseTint : ActivityColors.TEXT_SECONDARY;
    }

    public static int resolveStateColor(boolean hovered, boolean active, boolean enabled) {
        return resolveColor(0, hovered, active, !enabled);
    }

    public static void drawIcon(DrawContext context, ActivityIcon icon, int x, int y, int size, int tint, float alpha,
                                boolean hover, boolean selected, boolean disabled) {
        if (icon == null || context == null || size <= 0) return;
        int effectiveColor = resolveColor(tint, hover, selected, disabled);
        draw(context, icon, x, y, size, effectiveColor, alpha);
    }

    public static void draw(DrawContext context, ActivityIcon icon, int x, int y, int size, int color, float alphaFactor) {
        if (icon == null || context == null || size <= 0) return;
        int argb = applyAlpha(color, alphaFactor);
        if ((argb >>> 24) == 0) return;

        if (icon.hasAtlasRegion()) {
            try {
                context.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    ATLAS_ID,
                    x, y,
                    (float) icon.getAtlasU(), (float) icon.getAtlasV(),
                    size, size,
                    icon.getAtlasRegionW(), icon.getAtlasRegionH(),
                    ATLAS_WIDTH, ATLAS_HEIGHT,
                    argb
                );
                return;
            } catch (Throwable ignored) {

            }
        }

        drawSpansSized(context, icon, x, y, size, argb);
    }

    public static void draw(DrawContext context, ActivityIcon icon, int x, int y, int color, float alphaFactor) {
        draw(context, icon, x, y, (icon != null ? icon.getWidth() : 10), color, alphaFactor);
    }

    private static void drawSpansSized(DrawContext context, ActivityIcon icon, int x, int y, int size, int argb) {
        int baseW = icon.getWidth();
        int baseH = icon.getHeight();
        if (baseW <= 0 || baseH <= 0) return;

        float scaleX = (float) size / baseW;
        float scaleY = (float) size / baseH;
        int[][] spans = icon.getHorizontalSpans();

        for (int[] span : spans) {
            int row = span[0];
            int startCol = span[1];
            int endCol = span[2];

            int x1 = x + Math.round(startCol * scaleX);
            int x2 = x + Math.round(endCol * scaleX);
            int y1 = y + Math.round(row * scaleY);
            int y2 = y + Math.round((row + 1) * scaleY);

            if (x2 > x1 && y2 > y1) {
                context.fill(x1, y1, x2, y2, argb);
            }
        }
    }

    public static void draw(DrawContext context, ActivityIcon icon, int x, int y, int color) {
        draw(context, icon, x, y, (icon != null ? icon.getWidth() : 10), color, 1.0f);
    }

    public static void drawSized(DrawContext context, ActivityIcon icon, int x, int y, int size, int color) {
        draw(context, icon, x, y, size, color, 1.0f);
    }

    public static void drawScaled(DrawContext context, ActivityIcon icon, int x, int y, int scale, int color) {
        int baseSize = (icon != null ? icon.getWidth() : 10);
        draw(context, icon, x, y, baseSize * Math.max(1, scale), color, 1.0f);
    }

    public static void draw(DrawContext context, ActivityIcon icon, int x, int y) {
        draw(context, icon, x, y, (icon != null ? icon.getWidth() : 10), ActivityColors.TEXT_SECONDARY, 1.0f);
    }

    public static void drawCentered(DrawContext context, ActivityIcon icon, int boxX, int boxY, int boxW, int boxH, int color) {
        int size = (icon != null ? icon.getWidth() : 10);
        drawCenteredSized(context, icon, boxX, boxY, boxW, boxH, size, color);
    }

    public static void drawCenteredSized(DrawContext context, ActivityIcon icon, int boxX, int boxY, int boxW, int boxH, int size, int color) {
        if (icon == null || context == null || size <= 0) return;
        int cx = boxX + (boxW - size) / 2;
        int cy = boxY + (boxH - size) / 2;
        draw(context, icon, cx, cy, size, color, 1.0f);
    }

    public static void drawState(DrawContext context, ActivityIcon icon, int x, int y, boolean hovered, boolean active) {
        drawState(context, icon, x, y, hovered, active, true);
    }

    public static void drawState(DrawContext context, ActivityIcon icon, int x, int y, boolean hovered, boolean active, boolean enabled) {
        int color = resolveStateColor(hovered, active, enabled);
        draw(context, icon, x, y, (icon != null ? icon.getWidth() : 10), color, 1.0f);
    }

    public static void drawStateSized(DrawContext context, ActivityIcon icon, int x, int y, int size, boolean hovered, boolean active) {
        drawStateSized(context, icon, x, y, size, hovered, active, true);
    }

    public static void drawStateSized(DrawContext context, ActivityIcon icon, int x, int y, int size, boolean hovered, boolean active, boolean enabled) {
        int color = resolveStateColor(hovered, active, enabled);
        draw(context, icon, x, y, size, color, 1.0f);
    }

    public static void drawStateCentered(DrawContext context, ActivityIcon icon, int boxX, int boxY, int boxW, int boxH, boolean hovered, boolean active, boolean enabled) {
        int color = resolveStateColor(hovered, active, enabled);
        drawCentered(context, icon, boxX, boxY, boxW, boxH, color);
    }

    public static void drawStateCenteredSized(DrawContext context, ActivityIcon icon, int boxX, int boxY, int boxW, int boxH, int size, boolean hovered, boolean active, boolean enabled) {
        int color = resolveStateColor(hovered, active, enabled);
        drawCenteredSized(context, icon, boxX, boxY, boxW, boxH, size, color);
    }
}

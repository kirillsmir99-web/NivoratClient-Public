package activity.client.gui.render;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class RoundedPanelRenderer {
    private static final Identifier TEXTURE = Identifier.of("activity", "textures/gui/rounded_panel.png");
    private static final Identifier BORDER = Identifier.of("activity", "textures/gui/rounded_border.png");
    private RoundedPanelRenderer() { }
    public static void draw(DrawContext context, int x, int y, int width, int height, int color) {
        draw(context, TEXTURE, x, y, width, height, color);
    }
    public static void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        draw(context, BORDER, x, y, width, height, color);
    }
    private static void draw(DrawContext context, Identifier texture, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0 || (color >>> 24) == 0) return;
        int corner = Math.min(7, Math.min(width / 2, height / 2));
        for (int row = 0; row < 3; row++) {
            int top = row == 0 ? y : row == 1 ? y + corner : y + height - corner;
            int h = row == 1 ? height - corner * 2 : corner;
            int v = row == 0 ? 0 : row == 1 ? 7 : 9;
            int rh = row == 1 ? 2 : 7;
            for (int col = 0; col < 3; col++) {
                int left = col == 0 ? x : col == 1 ? x + corner : x + width - corner;
                int w = col == 1 ? width - corner * 2 : corner;
                int u = col == 0 ? 0 : col == 1 ? 7 : 9;
                int rw = col == 1 ? 2 : 7;
                if (w > 0 && h > 0) context.drawTexture(RenderPipelines.GUI_TEXTURED, texture,
                    left, top, u, v, w, h, rw, rh, 16, 16, color);
            }
        }
    }
}

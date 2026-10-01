package activity.client.gui.custom.api.ui;

import activity.client.gui.custom.utils.render.render2d.Render2D;
import activity.client.gui.custom.utils.color.ColorUtil;


public final class BrandMark {
    private BrandMark() {}
    public static void draw(float x, float y, float size, float alpha) {
        if (size <= 0 || alpha <= .005f) return;
        Render2D.image("activity:textures/gui/client_logo.png", x, y, size, size,
            ColorUtil.rgba(255, 255, 255, Math.round(255 * Math.max(0, Math.min(1, alpha)))));
    }
}

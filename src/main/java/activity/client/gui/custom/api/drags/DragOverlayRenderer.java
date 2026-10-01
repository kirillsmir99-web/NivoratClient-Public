package activity.client.gui.custom.api.drags;

import net.minecraft.client.gui.DrawContext;
import activity.client.gui.custom.VisualMaterial;
import activity.client.gui.custom.utils.render.render2d.Render2D;

public final class DragOverlayRenderer {
    private DragOverlayRenderer() {}

    public static void render(DrawContext context, DragController controller, float x, float y, float w, float h, float alpha) {
        if (context == null || w <= 0 || h <= 0 || !Float.isFinite(alpha)) return;
        VisualMaterial style = VisualMaterial.getInstance();
        int accent = style == null ? 0x80CFF5FF : style.clientPrimaryColorOpaque();
        int color = (Math.round(Math.clamp(alpha, 0f, 1f) * 170f) << 24) | (accent & 0xFFFFFF);
        Render2D.outline(x, y, w, h, 5, 5, 5, 5, 1, color, color, color, color);
        float lineX = controller.getSnapLineX(), lineY = controller.getSnapLineY();
        if (Float.isFinite(lineX)) Render2D.outline(lineX, 0, .8f, Position.screenHeight(), 0, 0, 0, 0, .8f, color, color, color, color);
        if (Float.isFinite(lineY)) Render2D.outline(0, lineY, Position.screenWidth(), .8f, 0, 0, 0, 0, .8f, color, color, color, color);
    }

    public static void clampToScreen(DragController controller, float w, float h) {
        if (!Float.isFinite(w) || !Float.isFinite(h)) return;
        float x = controller.getTargetX(), y = controller.getTargetY();
        controller.setTargetX(Position.clampX(Float.isFinite(x) ? x : Position.SCREEN_MARGIN, Math.max(0, w)));
        controller.setTargetY(Position.clampY(Float.isFinite(y) ? y : Position.SCREEN_MARGIN, Math.max(0, h)));
    }


    public static void applyGridSnap(DragController controller, float w, float h) {
        if (!Float.isFinite(w) || !Float.isFinite(h)) return;
        float screenW = Position.screenWidth(), screenH = Position.screenHeight();
        float x = snap(controller.getTargetX(), w, screenW);
        float y = snap(controller.getTargetY(), h, screenH);
        if (Float.isFinite(x)) {
            controller.setTargetX(x);
            controller.setSnapLineX(Math.abs(x - (screenW-w)*.5f) < .01f ? screenW*.5f : (x <= Position.SCREEN_MARGIN ? x : x+w));
        }
        if (Float.isFinite(y)) {
            controller.setTargetY(y);
            controller.setSnapLineY(Math.abs(y - (screenH-h)*.5f) < .01f ? screenH*.5f : (y <= Position.SCREEN_MARGIN ? y : y+h));
        }
        clampToScreen(controller, w, h);
    }

    private static float snap(float value, float size, float extent) {
        if (size > extent - 2*Position.SCREEN_MARGIN) return Float.NaN;
        float center = (extent-size)*.5f;
        if (Math.abs(value-center) <= 4) return center;
        if (Math.abs(value-Position.SCREEN_MARGIN) <= 4) return Position.SCREEN_MARGIN;
        float end = extent-size-Position.SCREEN_MARGIN;
        return Math.abs(value-end) <= 4 ? end : Float.NaN;
    }
}

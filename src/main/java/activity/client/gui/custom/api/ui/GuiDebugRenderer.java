package activity.client.gui.custom.api.ui;

import net.minecraft.client.gui.DrawContext;
import activity.client.gui.custom.api.drags.Position;
import activity.client.gui.custom.utils.render.fonts.Fonts;
import activity.client.gui.custom.utils.render.render2d.Render2D;


public final class GuiDebugRenderer {
    public static boolean ENABLED = false;

    private GuiDebugRenderer() {}

    public static void renderRect(float x, float y, float w, float h, int color) {
        if (!ENABLED) return;
        Render2D.outline(x, y, w, h, 0.0f, 1.0f, color);
    }

    public static void renderInputRect(float x, float y, float w, float h) {
        if (!ENABLED) return;

        renderRect(x, y, w, h, 0xCC00FFFF);
    }

    public static void renderRenderRect(float x, float y, float w, float h) {
        if (!ENABLED) return;

        renderRect(x, y, w, h, 0xCC00FF00);
    }

    public static void renderClipRect(float x, float y, float w, float h) {
        if (!ENABLED) return;

        renderRect(x, y, w, h, 0xCCFF0000);
    }

    public static void renderAnchor(float x, float y) {
        if (!ENABLED) return;

        Render2D.circle(x, y, 3.0f, 0xFFFFA500);
    }

    public static void renderMouseCoords(DrawContext drawContext) {
        if (!ENABLED) return;
        float mx = Position.mouseX();
        float my = Position.mouseY();
        String coords = String.format("M: (%.1f, %.1f)", mx, my);
        Fonts.MONTSERRAT_MEDIUM.draw(coords, mx + 10.0f, my - 8.0f, 5.5f, 0xFF00FFFF);
    }

    public static void logInvisibleInteractive(String name, int row, float alpha) {
        if (ENABLED) {
            System.err.println("[NV-UI-DEBUG] Invisible interactive element: " + name + " (row " + row + ", alpha=" + alpha + ")");
        }
    }

    public static void recordRect(String name, float x, float y, float w, float h, boolean visible) {
        if (!ENABLED) return;
        renderRenderRect(x, y, w, h);
    }

    public static void logCategoryMismatch(String catName, int count, int rendered) {
        if (ENABLED) {
            System.err.println("[NV-UI-DEBUG] Category " + catName + " count mismatch: declared=" + count + ", rendered=" + rendered);
        }
    }
}

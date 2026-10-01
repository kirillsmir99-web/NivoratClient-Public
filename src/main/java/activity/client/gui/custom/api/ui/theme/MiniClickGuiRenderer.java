package activity.client.gui.custom.api.ui.theme;

import net.minecraft.client.gui.DrawContext;


public final class MiniClickGuiRenderer {

    public static void renderMiniClickGui(DrawContext drawContext, float x, float y, float w, float h, ThemeDraft draft, int bgMode, float alpha, float dt) {
        if (alpha <= 0.005f || draft == null) return;
        PreviewRenderContext ctx = new PreviewRenderContext(draft, alpha, bgMode, dt);
        LauncherMirrorPreview.render(drawContext, x, y, w, h, ctx);
    }
}

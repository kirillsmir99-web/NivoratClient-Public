package activity.client.gui.custom.api.ui.header;

import activity.client.gui.custom.utils.render.fonts.Fonts;
import net.minecraft.client.gui.DrawContext;

public final class SectionHeader {
    public static final float TITLE_SIZE = 7.5f;

    private SectionHeader() {}

    public static void render(DrawContext drawContext, String title, float x, float y, float alpha) {
        if (alpha <= 0.005f || title == null || title.isEmpty()) return;
        int color = ((Math.max(0, Math.min(255, Math.round(245.0f * alpha)))) << 24) | 0xFFFFFF;
        Fonts.MONTSERRAT_BOLD.draw(title, x, y, TITLE_SIZE, color);
    }
}

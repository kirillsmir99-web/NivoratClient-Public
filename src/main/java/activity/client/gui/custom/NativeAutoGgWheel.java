package activity.client.gui.custom;

import activity.client.gui.custom.api.ui.theme.ClientAccent;
import activity.client.gui.custom.api.ui.theme.ThemeManager;
import activity.client.gui.custom.hud.HudIcons;
import activity.client.gui.custom.utils.render.fonts.Fonts;
import activity.client.gui.custom.utils.render.others.RectUtil;
import activity.client.gui.custom.utils.render.render2d.Render2D;
import activity.client.gui.custom.utils.render.render2d.glass.BuiltGlass;
import activity.client.gui.custom.utils.render.render2d.radialglass.BuiltRadialGlass;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

public final class NativeAutoGgWheel {
    private static final String FOOTER = "Nivorat";
    private static final float FOOTER_Y = 169f;

    private NativeAutoGgWheel() {}

    public static void render(DrawContext context, int cx, int cy, List<String> phrases,
                              String selected, int hovered, boolean hubHovered) {
        try (var frame = UnifiedHudRender.begin(context)) {
            int count = phrases.size();
            int sectors = count < 3 ? 4 : count;
            float step = (float) (Math.PI * 2 / sectors);
            for (int i = 0; i < sectors; i++) {
                int owner = count == 0 ? -1 : count < 3 ? i * count / sectors : i;
                boolean over = owner == hovered && owner >= 0;
                boolean active = owner >= 0 && phrases.get(owner).equalsIgnoreCase(selected);
                float midpoint = (i + .5f) * step;
                Render2D.radialGlass(new BuiltRadialGlass(cx, cy, 56, 148, midpoint,
                        step * .5f - .022f, 7f, 1f,
                        over ? ClientAccent.accent(180) : ThemeManager.rgba(0x15131d, 245),
                        ThemeManager.rgba(0x0d0f17, 245), 0, 1, 2.2f,
                        ClientAccent.accentBright(over ? 255 : active ? 210 : 105),
                        .65f, false, .32f, .006f, 3,
                        ClientAccent.accentBright(255), over ? .4f : active ? .12f : .025f, 0));
            }
            BuiltGlass hubGlass = new BuiltGlass(
                    cx - 46, cy - 46, 92, 92,
                    46, 46, 46, 46,
                    hubHovered ? ClientAccent.accent(180) : ThemeManager.rgba(0x15131d, 245),
                    1.0f,
                    2.2f,
                    ClientAccent.accentBright(hubHovered ? 255 : 105),
                    0.65f,
                    false,
                    0.32f,
                    0.006f,
                    0.5f,
                    0.0f
            ).withBlurRadius(30.0f).withSecondColor(ThemeManager.rgba(0x0d0f17, 245), 0.0f).withoutMosaic();
            Render2D.glass(hubGlass);
            Render2D.outline(cx - 46, cy - 46, 92, 92, 46, .65f,
                    ClientAccent.accent(hubHovered ? 235 : 100));
            HudIcons.draw(HudIcons.CHAT, cx - 8, cy - 28, 16, ClientAccent.accentBright(245));
            centered("AutoGG", cx, cy - 5, 10, 0xffedf0f6);
            centered("ru".equals(VisualText.language()) ? "Настройки" : "Settings",
                    cx, cy + 14, 6.5f, ClientAccent.accentSoft(hubHovered ? 255 : 190));
            for (int i = 0; i < count; i++) {
                double angle = -Math.PI / 2 + (i + .5) * Math.PI * 2 / count;
                float tx = cx + (float) Math.cos(angle) * 102;
                float ty = cy + (float) Math.sin(angle) * 102;
                boolean active = phrases.get(i).equalsIgnoreCase(selected);
                int color = hovered == i ? 0xffffffff : active ? ClientAccent.accentBright(255) : 0xffd9dfe9;
                HudIcons.draw(HudIcons.CHAT, tx - 6, ty - 21, 12,
                        hovered == i || active ? ClientAccent.accentBright(235) : 0x95cdd3df);
                String text = CustomRender.fit(VisualText.safe(phrases.get(i), VisualText.language()),
                        count <= 4 ? 95f : 68f, 8f);
                centered(text, tx, ty, 8f, color);
                if (active) {
                    Render2D.rect(tx - 8, ty + 16, 16, 1.5f, .75f, ClientAccent.accentBright(245));
                }
            }
            centered(FOOTER, cx, cy + FOOTER_Y, 6f, ClientAccent.accentSoft(190));
        }
    }

    public static boolean isFooterHovered(double x, double y, int cx, int cy) {
        float halfWidth = Fonts.MONTSERRAT_MEDIUM.width(FOOTER, 6f) / 2 + 5;
        return x >= cx - halfWidth && x <= cx + halfWidth && y >= cy + FOOTER_Y - 3 && y <= cy + FOOTER_Y + 11;
    }

    private static void centered(String text, float x, float y, float size, int color) {
        Fonts.MONTSERRAT_MEDIUM.draw(text, x - Fonts.MONTSERRAT_MEDIUM.width(text, size) / 2, y, size, color);
    }
}

package activity.client.gui.custom;

import activity.client.config.ActivityConfigManager;
import activity.client.gui.custom.api.drags.Position;
import activity.client.gui.custom.api.ui.theme.ClientAccent;
import activity.client.gui.custom.mixin.accessor.GuiGraphicsExtractorAccessor;
import activity.client.gui.custom.utils.render.fonts.Fonts;
import activity.client.gui.custom.utils.render.render2d.Render2D;
import activity.client.module.keybind.Keybind;
import activity.client.module.keybind.KeybindManager;
import net.minecraft.client.gui.DrawContext;

public final class NativeBindAssignment {
    private static Keybind current;
    private static Keybind candidate;
    private static Runnable assignment;
    private static String conflict;

    private NativeBindAssignment() {}

    public static void request(Keybind existing, Keybind next, Runnable apply) {
        String owner = KeybindManager.findConflict(next, existing);
        if (owner == null) {
            apply.run();
            ActivityConfigManager.markDirty();
            return;
        }
        current = existing;
        candidate = next;
        assignment = apply;
        conflict = owner;
    }

    public static boolean isOpen() { return assignment != null; }
    public static void cancel() { assignment = null; current = null; candidate = null; conflict = null; }
    public static void confirm() {
        if (!isOpen()) return;
        Runnable apply = assignment;
        KeybindManager.unbindConflict(candidate, current);
        cancel();
        apply.run();
        ActivityConfigManager.markDirty();
    }

    public static boolean keyPressed(int key) {
        if (!isOpen()) return false;
        if (key == 256) cancel();
        else if (key == 257 || key == 335) confirm();
        return true;
    }

    public static boolean click(float mouseX, float mouseY, int button) {
        if (!isOpen()) return false;
        float x = (Position.screenWidth() - 260f) / 2f;
        float y = (Position.screenHeight() - 110f) / 2f;
        if (button == 0 && mouseY >= y + 78f && mouseY <= y + 99f) {
            if (mouseX >= x + 14f && mouseX <= x + 124f) confirm();
            else if (mouseX >= x + 136f && mouseX <= x + 246f) cancel();
        }
        return true;
    }

    public static void render(DrawContext context, float alpha) {
        if (!isOpen()) return;
        Render2D.flush();
        ((GuiGraphicsExtractorAccessor) context).nv_getGuiRenderState().createNewRootLayer();
        Render2D.beginFrame(context);
        boolean ru = activity.client.i18n.LocalizationService.isRussianPreferred();
        float x = (Position.screenWidth() - 260f) / 2f;
        float y = (Position.screenHeight() - 110f) / 2f;
        Render2D.rect(0, 0, Position.screenWidth(), Position.screenHeight(), 0, 0x77000000);
        CustomRender.panel(context, x, y, 260, 110, 10, alpha);
        Render2D.outline(x, y, 260, 110, 9, .7f, ClientAccent.accentSoft(190));
        Fonts.MONTSERRAT_MEDIUM.draw(ru ? "Этот бинд уже занят" : "This binding is already used", x + 14, y + 12, 9, -1);
        Fonts.MONTSERRAT_MEDIUM.draw(CustomRender.fit(candidate.format() + " · " + conflict, 232, 7), x + 14, y + 33, 7, 0xffd9dfe9);
        Fonts.MONTSERRAT_MEDIUM.draw(ru ? "Перенести бинд? Старое назначение будет снято." : "Move the binding? The old assignment will be cleared.", x + 14, y + 53, 6, 0xffb9c0d0);
        Render2D.rect(x + 14, y + 78, 110, 21, 4, ClientAccent.accent(230));
        Render2D.rect(x + 136, y + 78, 110, 21, 4, 0xff292d3b);
        Fonts.MONTSERRAT_MEDIUM.draw(ru ? "Перенести" : "Move", x + 36, y + 84, 7, -1);
        Fonts.MONTSERRAT_MEDIUM.draw(ru ? "Отмена" : "Cancel", x + 165, y + 84, 7, -1);
    }
}

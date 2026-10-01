package activity.client.gui.custom;

import activity.client.gui.custom.api.drags.Position;
import activity.client.gui.custom.api.ui.theme.ClientAccent;
import activity.client.gui.custom.utils.render.fonts.Fonts;
import activity.client.gui.custom.utils.render.render2d.Render2D;
import activity.client.gui.custom.utils.render.render2d.Render2DCoordinateSpace;
import activity.client.gui.custom.utils.sounds.Sounds;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

public abstract class NativeHudEditorScreen extends Screen {
    private final Screen parent;
    private final String heading;
    private float panelX = 12, panelY = 18;
    private int drag;
    private float grabX, grabY;
    protected NativeHudEditorScreen(String heading, Screen parent) {
        super(Text.literal(heading)); this.heading = heading; this.parent = parent;
    }
    protected abstract int hudX();
    protected abstract int hudY();
    protected abstract int hudWidth();
    protected abstract int hudHeight();
    protected abstract void moveHud(float x, float y);
    protected abstract void resetHud();
    protected abstract void saveHud();
    protected abstract void drawHud(DrawContext context);
    protected String modeLabel() { return null; }
    protected void cycleMode() {}
    protected boolean ru() { return activity.client.i18n.LocalizationService.isRussianPreferred(); }
    private int panelHeight() { return modeLabel() == null ? 102 : 124; }
    @Override protected void init() { Sounds.play("module_settings_open"); }
    private boolean inside(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
    @Override public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) return false;
        float scale = Render2DCoordinateSpace.guiIndependentScale();
        float mx = (float) click.x() / scale, my = (float) click.y() / scale;
        if (inside(mx, my, panelX, panelY, 166, panelHeight())) {
            if (my < panelY + 26) { drag = 1; grabX = mx - panelX; grabY = my - panelY; setDragging(true); }
            else {
                int row = (int) ((my - panelY - 54) / 22);
                if (my >= panelY + 54 && mx >= panelX + 8 && mx <= panelX + 158) {
                    if (modeLabel() != null && row == 0) cycleMode();
                    else if (row == (modeLabel() == null ? 0 : 1)) { resetHud(); saveHud(); }
                    else if (row == (modeLabel() == null ? 1 : 2)) { close(); return true; }
                    Sounds.play("buttonclick");
                }
            }
            return true;
        }
        if (hudWidth() > 0 && inside(mx, my, hudX() - 4, hudY() - (hudHeight() > 24 ? 23 : 4), hudWidth() + 8, hudHeight() + 8)) {
            drag = 2; grabX = mx - hudX(); grabY = my - hudY(); setDragging(true); Sounds.play("buttonclick"); return true;
        }
        return false;
    }
    @Override public boolean mouseDragged(Click click, double dx, double dy) {
        float scale = Render2DCoordinateSpace.guiIndependentScale();
        float mx = (float) click.x() / scale, my = (float) click.y() / scale;
        if (drag == 1) {
            panelX = Math.clamp(mx - grabX, 2, Math.max(2, Position.screenWidth() - 168));
            panelY = Math.clamp(my - grabY, 2, Math.max(2, Position.screenHeight() - panelHeight() - 2));
        } else if (drag == 2) moveHud(mx - grabX, my - grabY);
        return drag != 0;
    }
    @Override public boolean mouseReleased(Click click) {
        if (click.button() != 0 || drag == 0) return false;
        drag = 0; setDragging(false); saveHud(); Sounds.play("buttonclick"); return true;
    }
    @Override public boolean keyPressed(KeyInput input) {
        int step = input.hasShift() ? 5 : 1;
        switch (input.key()) {
            case 256, 257 -> close();
            case 258 -> { cycleMode(); Sounds.play("buttonclick"); }
            case 82 -> { resetHud(); saveHud(); Sounds.play("buttonclick"); }
            case 262 -> moveHud(hudX() + step, hudY());
            case 263 -> moveHud(hudX() - step, hudY());
            case 264 -> moveHud(hudX(), hudY() + step);
            case 265 -> moveHud(hudX(), hudY() - step);
            default -> { return false; }
        }
        return true;
    }
    @Override public void renderBackground(DrawContext context, int mx, int my, float delta) {}
    @Override public void render(DrawContext context, int mx, int my, float delta) {
        drawHud(context);
        try (var frame = UnifiedHudRender.beginNative(context)) {
            CustomRender.panel(context, panelX, panelY, 166, panelHeight(), 7, 1);
            int row = 0;
            if (modeLabel() != null) buttonBackground(row++);
            buttonBackground(row++);
            buttonBackground(row);
            Render2D.flush();
            context.createNewRootLayer();
            Render2D.beginFrame(context);
            Fonts.MONTSERRAT_MEDIUM.draw(heading, panelX + 9, panelY + 10, 9, -1);
            Fonts.MONTSERRAT_MEDIUM.draw(ru() ? "Перетащите HUD или заголовок" : "Drag HUD or window heading", panelX + 9, panelY + 29, 6, 0xffaeb8cc);
            Fonts.MONTSERRAT_MEDIUM.draw("X " + hudX() + "  ·  Y " + hudY(), panelX + 9, panelY + 41, 7, ClientAccent.accentBright(255));
            row = 0;
            if (modeLabel() != null) buttonText(row++, modeLabel());
            buttonText(row++, ru() ? "Сбросить позицию" : "Reset position");
            buttonText(row, ru() ? "Готово" : "Done");
        }
    }
    private void buttonBackground(int row) {
        float y = panelY + 54 + row * 22;
        boolean hover = inside(Position.mouseX(), Position.mouseY(), panelX + 8, y, 150, 18);
        Render2D.rect(panelX + 8, y, 150, 18, 4, hover ? ClientAccent.accent(180) : 0xff252936);
    }
    private void buttonText(int row, String label) {
        Fonts.MONTSERRAT_MEDIUM.draw(CustomRender.fit(label, 136, 7), panelX + 15, panelY + 59 + row * 22, 7, -1);
    }
    @Override public void close() { drag = 0; setDragging(false); saveHud(); Sounds.play("module_settings_close"); client.setScreen(parent); }
    @Override public boolean shouldPause() { return false; }
}

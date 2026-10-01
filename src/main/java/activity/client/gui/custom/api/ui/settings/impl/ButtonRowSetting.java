package activity.client.gui.custom.api.ui.settings.impl;

import activity.client.gui.custom.api.drags.Position;
import activity.client.gui.custom.api.modules.settings.impl.ButtonSetting;
import activity.client.gui.custom.api.ui.settings.RenderHelper;
import activity.client.gui.custom.api.ui.settings.Setting;
import activity.client.gui.custom.api.ui.theme.ClientAccent;
import activity.client.gui.custom.utils.render.fonts.Fonts;
import activity.client.gui.custom.utils.render.render2d.Render2D;
import activity.client.gui.custom.utils.sounds.Sounds;

public class ButtonRowSetting implements Setting {
    public static final float HEIGHT = 18.0f;
    private static final float BTN_H = 12.0f;
    private final ButtonSetting backend;

    public ButtonRowSetting(ButtonSetting backend) {
        this.backend = backend;
    }

    @Override
    public String description(){return this.backend.getDescription();}

    public String name() {
        return this.backend.getDisplayName();
    }

    @Override
    public float height() {
        return HEIGHT;
    }

    private static int rgba(int r, int g, int b, float a) {
        int alpha = Math.max(0, Math.min(255, Math.round(a)));
        return (alpha & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }

    private String getDisplayLabel() {
        String label = this.backend.getLabel();
        String fallback = (label == null || label.trim().isEmpty()) ? "Нажать" : label;
        return activity.client.gui.custom.api.localization.Lang.get("ui.button." + activity.client.gui.custom.api.localization.Lang.toKey(fallback), fallback);
    }

    private float getBtnWidth() {
        String label = this.getDisplayLabel();
        float textW = Fonts.MONTSERRAT_MEDIUM.width(label, 5.5f);
        return Math.max(36.0f, textW + 12.0f);
    }

    @Override
    public void render(float x, float y, float width, float alpha) {
        String label = this.getDisplayLabel();
        float btnW = this.getBtnWidth();
        float btnX = x + width - btnW - 4.0f;
        float btnY = y + (HEIGHT - BTN_H) * 0.5f;


        float maxLabelW = btnX - (x + 6.0f) - 4.0f;
        RenderHelper.drawName(this.backend.getDisplayName(), x, y, maxLabelW, alpha);


        float mouseX = Position.mouseX();
        float mouseY = Position.mouseY();
        boolean hover = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + BTN_H;

        int bgCol = rgba(18, 20, 26, (hover ? 160.0f : 110.0f) * alpha);
        Render2D.rect(btnX, btnY, btnW, BTN_H, 3.0f, bgCol);

        int borderCol = hover ? ClientAccent.accentBright(180.0f * alpha) : rgba(255, 255, 255, 22.0f * alpha);
        Render2D.outline(btnX, btnY, btnW, BTN_H, 3.0f, 0.5f, borderCol);

        float textW = Fonts.MONTSERRAT_MEDIUM.width(label, 5.5f);
        int textCol = hover ? ClientAccent.accentBright(245.0f * alpha) : rgba(225, 230, 245, 210.0f * alpha);
        Fonts.MONTSERRAT_MEDIUM.draw(label, btnX + (btnW - textW) * 0.5f, btnY + 2.5f, 5.5f, textCol);
    }

    @Override
    public boolean isVisible() {
        return this.backend.isVisible();
    }

    @Override
    public boolean click(float x, float y, float width, float mouseX, float mouseY) {
        float btnW = this.getBtnWidth();
        float btnX = x + width - btnW - 4.0f;
        float btnY = y + (HEIGHT - BTN_H) * 0.5f;
        if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + BTN_H) {
            this.backend.click();
            Sounds.play("buttonclick");
            return true;
        }
        return false;
    }

    @Override
    public float preferredWidth() {
        return Fonts.MONTSERRAT_MEDIUM.width(this.backend.getDisplayName(), 6.0f) + this.getBtnWidth() + 20.0f;
    }
}

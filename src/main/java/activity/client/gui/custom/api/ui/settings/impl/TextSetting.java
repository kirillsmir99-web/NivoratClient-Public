package activity.client.gui.custom.api.ui.settings.impl;
import java.awt.Color;
import activity.client.gui.custom.api.ui.settings.RenderHelper;
import activity.client.gui.custom.api.ui.settings.Setting;
import activity.client.gui.custom.api.ui.theme.ClientAccent;
import activity.client.gui.custom.utils.render.fonts.Fonts;
import activity.client.gui.custom.utils.render.render2d.Render2D;

public class TextSetting
implements Setting {
    private static final float FIELD_W = 92.0f;
    private static final float FONT_SIZE = 6.0f;
    private static TextSetting focused;
    private final activity.client.gui.custom.api.modules.settings.impl.TextSetting backend;

    public TextSetting(activity.client.gui.custom.api.modules.settings.impl.TextSetting textSetting) {
        this.backend = textSetting;
    }

    @Override
    public String description(){return this.backend.getDescription();}

    public String name() {
        return this.backend.getDisplayName();
    }

    public void typeChar(int n) {
        if (Character.isISOControl(n)) {
            return;
        }
        String string = this.backend.getValue();
        if (string.length() >= this.backend.getMaxLength()) {
            return;
        }
        this.backend.setText(string + new String(Character.toChars(n)));
    }

    public boolean typeKey(int n) {
        switch (n) {
            case 259: {
                String string = this.backend.getValue();
                if (!string.isEmpty()) {
                    this.backend.setText(string.substring(0, string.length() - 1));
                }
                return true;
            }
            case 256:
            case 257:
            case 335: {
                focused = null;
                return true;
            }
        }
        return false;
    }

    @Override
    public float height() {
        return 16.0f;
    }

    @Override
    public void render(float f, float f2, float f3, float f4) {
        String string;
        boolean bl;
        float f5 = 12.0f;
        float fieldW = Math.min(84.0f, Math.max(56.0f, f3 * 0.44f));
        float f6 = f + f3 - fieldW - 4.0f;
        float f7 = f2 + (16.0f - f5) * 0.5f;
        boolean bl2 = this.isFocused();
        RenderHelper.drawName(this.backend.getDisplayName(), f, f2, f6 - (f + 6.0f) - 4.0f, f4);
        Render2D.rect(f6, f7, fieldW, f5, 3.0f, new Color(0, 0, 0, (int)(60.0f * f4)).getRGB());
        if (bl2) {
            Render2D.outline(f6, f7, fieldW, f5, 3.0f, 0.6f, ClientAccent.accentBright(190.0f * f4));
        }
        boolean bl3 = bl = (string = this.backend.getValue()) == null || string.isEmpty();
        String string2 = bl ? (this.backend.getPlaceholder().isEmpty() ? "..." : this.backend.getPlaceholder()) : string;
        float f8 = fieldW - 10.0f;
        String string3 = string2;
        while (string3.length() > 1 && Fonts.MONTSERRAT_MEDIUM.width(string3, 6.0f) > f8) {
            string3 = string3.substring(1);
        }
        int n = bl ? new Color(255, 255, 255, (int)(110.0f * f4)).getRGB() : ClientAccent.accentSoft(220.0f * f4);
        Fonts.MONTSERRAT_MEDIUM.draw(string3, f6 + 5.0f, f7 + 2.5f, 6.0f, n);
        if (bl2 && System.currentTimeMillis() / 500L % 2L == 0L) {
            float f9 = f6 + 5.0f + (bl ? 0.0f : Fonts.MONTSERRAT_MEDIUM.width(string3, 6.0f)) + 0.5f;
            Render2D.rect(f9, f7 + 2.0f, 0.8f, f5 - 4.0f, 0.0f, ClientAccent.accentBright(220.0f * f4));
        }
    }

    @Override
    public boolean isVisible() {
        return this.backend.isVisible();
    }

    @Override
    public boolean click(float f, float f2, float f3, float f4, float f5) {
        float f6 = 12.0f;
        float fieldW = Math.min(84.0f, Math.max(56.0f, f3 * 0.44f));
        float f7 = f + f3 - fieldW - 4.0f;
        float f8 = f2 + (16.0f - f6) * 0.5f;
        if (f4 >= f7 && f4 <= f7 + fieldW && f5 >= f8 && f5 <= f8 + f6) {
            focused = this.isFocused() ? null : this;
            return true;
        }
        if (this.isFocused()) {
            focused = null;
        }
        return false;
    }

    public boolean isFocused() {
        return focused == this;
    }

    public static void unfocusAll() {
        focused = null;
    }

    public static boolean isAnyFocused() {
        return focused != null;
    }

    @Override
    public float preferredWidth() {
        return 6.0f + Fonts.MONTSERRAT_MEDIUM.width(this.backend.getDisplayName(), 6.5f) + 6.0f + 92.0f + 8.0f;
    }
}

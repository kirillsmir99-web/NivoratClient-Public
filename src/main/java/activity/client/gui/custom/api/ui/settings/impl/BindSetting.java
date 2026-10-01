package activity.client.gui.custom.api.ui.settings.impl;
import activity.client.gui.custom.api.ui.settings.RenderHelper;
import activity.client.gui.custom.api.ui.settings.Setting;
import activity.client.gui.custom.utils.key.KeyBind;
import activity.client.gui.custom.utils.render.fonts.Fonts;

public class BindSetting
implements Setting {
    private final activity.client.gui.custom.api.modules.settings.impl.BindSetting backend;
    private boolean listening;

    public BindSetting(activity.client.gui.custom.api.modules.settings.impl.BindSetting bindSetting) {
        this.backend = bindSetting;
    }

    public activity.client.gui.custom.api.modules.settings.impl.BindSetting backend(){return backend;}

    @Override
    public String description(){return this.backend.getDescription();}

    public String name() {
        return this.backend.getDisplayName();
    }

    public void setKey(int n) {
        this.backend.setKey(n);
    }

    @Override
    public float height() {
        return 16.0f;
    }

    @Override
    public void render(float f, float f2, float f3, float f4) {
        String string = this.listening ? "Press key..." : new KeyBind(this.backend.getKey()).getDisplayName();
        float f5 = Fonts.MONTSERRAT_MEDIUM.width(string, 6.0f) + 10.0f;
        float f6 = f + f3 - f5 - 4.0f;
        float f7 = f2 + 2.0f;
        RenderHelper.drawName(this.backend.getDisplayName(), f, f2, f6 - (f + 6.0f) - 4.0f, f4);
        RenderHelper.drawBtn(f6, f7, f5, 12.0f, string, f4);
    }

    @Override
    public boolean isVisible() {
        return this.backend.isVisible();
    }

    @Override
    public boolean click(float f, float f2, float f3, float f4, float f5) {
        String string = this.listening ? "Press key..." : new KeyBind(this.backend.getKey()).getDisplayName();
        float f6 = Fonts.MONTSERRAT_MEDIUM.width(string, 6.0f) + 10.0f;
        float f7 = f + f3 - f6 - 4.0f;
        float f8 = f2 + 2.0f;
        if (f4 >= f7 && f4 <= f7 + f6 && f5 >= f8 && f5 <= f8 + 12.0f) {
            this.listening = !this.listening;
            return true;
        }
        return false;
    }

    public void setListening(boolean bl) {
        this.listening = bl;
    }

    public boolean isListening() {
        return this.listening;
    }

    @Override
    public float preferredWidth() {
        String string = new KeyBind(this.backend.getKey()).getDisplayName();
        return 6.0f + Fonts.MONTSERRAT_MEDIUM.width(this.backend.getDisplayName(), 6.5f) + 6.0f + Fonts.MONTSERRAT_MEDIUM.width(string, 6.0f) + 10.0f + 8.0f;
    }
}

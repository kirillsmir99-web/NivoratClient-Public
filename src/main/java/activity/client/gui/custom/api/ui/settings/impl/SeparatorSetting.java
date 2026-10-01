package activity.client.gui.custom.api.ui.settings.impl;

import activity.client.gui.custom.api.ui.settings.NvSectionHeader;
import activity.client.gui.custom.api.ui.settings.Setting;
import activity.client.gui.custom.api.ui.theme.ClientAccent;
import activity.client.gui.custom.utils.render.fonts.Fonts;
import activity.client.gui.custom.utils.render.render2d.Render2D;

public class SeparatorSetting
implements Setting {
    public static final float HEIGHT = 18.0f;
    private final activity.client.gui.custom.api.modules.settings.impl.SeparatorSetting backend;

    public SeparatorSetting(activity.client.gui.custom.api.modules.settings.impl.SeparatorSetting separatorSetting) {
        this.backend = separatorSetting;
    }

    @Override
    public String description(){return this.backend.getDescription();}

    public String name() {
        return this.backend.getDisplayName();
    }

    @Override
    public float height() {
        return 20.0f;
    }

    @Override
    public void render(float f, float f2, float f3, float f4) {
        NvSectionHeader.render(f, f2, f3, this.backend.getDisplayName(), NvSectionHeader.Style.NORMAL, f4);
    }

    @Override
    public boolean isVisible() {
        return this.backend.isVisible();
    }

    @Override
    public boolean click(float f, float f2, float f3, float f4, float f5) {
        return false;
    }

    @Override
    public float preferredWidth() {
        return Fonts.MONTSERRAT_MEDIUM.width(this.backend.getDisplayName(), 6.0f) + 48.0f;
    }
}

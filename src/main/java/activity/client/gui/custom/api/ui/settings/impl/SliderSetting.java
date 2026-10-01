package activity.client.gui.custom.api.ui.settings.impl;
import java.awt.Color;
import activity.client.gui.custom.api.drags.Position;
import activity.client.gui.custom.api.ui.settings.RenderHelper;
import activity.client.gui.custom.api.ui.settings.Setting;
import activity.client.gui.custom.api.ui.theme.ClientAccent;
import activity.client.gui.custom.utils.render.fonts.Fonts;
import activity.client.gui.custom.utils.render.render2d.Render2D;
import activity.client.gui.custom.utils.sounds.Sounds;

public class SliderSetting
implements Setting {
    private final activity.client.gui.custom.api.modules.settings.impl.SliderSetting backend;
    private boolean dragging;
    private String lastSoundText;
    private float visProgress = -1.0f;
    private long lastNs = System.nanoTime();
    public static final float ROW_H = 22.0f;
    public static final float BAR_H = 3.0f;
    public static final float BAR_TOP = 16.0f;

    public SliderSetting(activity.client.gui.custom.api.modules.settings.impl.SliderSetting sliderSetting) {
        this.backend = sliderSetting;
    }

    @Override
    public String description(){return this.backend.getDescription();}

    public String name() {
        return this.backend.getDisplayName();
    }

    @Override
    public float height() {
        return 22.0f;
    }

    private static float clamp01(float f) {
        return f < 0.0f ? 0.0f : (f > 1.0f ? 1.0f : f);
    }

    @Override
    public void render(float f, float f2, float f3, float f4) {
        float f5;
        long l = System.nanoTime();
        float f6 = Math.min(0.1f, (float)(l - this.lastNs) / 1.0E9f);
        this.lastNs = l;
        float f7 = f + 6.0f;
        float f8 = f3 - 12.0f;
        float f9 = f2 + 16.0f;
        if (this.dragging) {
            f5 = Position.mouseX();
            float f10 = SliderSetting.clamp01((f5 - f7) / f8);
            float prevVal = this.backend.getValue();
            this.backend.setValue(this.backend.getMin() + f10 * (this.backend.getMax() - this.backend.getMin()));
            float newVal = this.backend.getValue();
            if (Float.compare(prevVal, newVal) != 0) {
                Sounds.play("slider");
            }
        }
        f5 = 6.0f;
        String string = this.formatValue(this.backend.getValue());
        float f11 = Fonts.MONTSERRAT_MEDIUM.width(string, f5);
        float f12 = f + f3 - 6.0f - f11;
        Fonts.MONTSERRAT_MEDIUM.draw(string, f12, f2 + 5.0f + 0.25f, f5, SliderSetting.rgba(255, 255, 255, 238.0f * f4));
        RenderHelper.drawName(this.backend.getDisplayName(), f, f2, f12 - (f + 6.0f) - 6.0f, f4);
        float f13 = this.backend.getProgress();
        if (this.visProgress < 0.0f) {
            this.visProgress = f13;
        }
        this.visProgress += (f13 - this.visProgress) * (1.0f - (float)Math.exp(-f6 * 18.0f));
        if (Math.abs(f13 - this.visProgress) < 0.0015f) {
            this.visProgress = f13;
        }
        float f14 = f7 + f8 * SliderSetting.clamp01(this.visProgress);
        float f15 = 1.5f;
        Render2D.rect(f7, f9, f8, 3.0f, f15, SliderSetting.rgba(16, 16, 16, 64.0f * f4));
        if (f14 - f7 > 0.6f) {
            int n = ClientAccent.gradientA(215.0f * f4);
            int n2 = ClientAccent.gradientB(215.0f * f4);
            Render2D.rect(f7, f9, f14 - f7, 3.0f, f15, n, n2, n2, n);
            Render2D.outline(f7, f9, f14 - f7, 3.0f, f15, 0.5f, ClientAccent.accentBright(150.0f * f4));
        }
        float f16 = 6.5f;
        float f17 = 4.0f;
        float f18 = 2.0f;
        float f19 = Math.max(f7, Math.min(f7 + f8 - f16, f14 - f16 * 0.5f));
        float f20 = f9 + 1.5f - f17 * 0.5f;
        Render2D.rect(f19, f20, f16, f17, f18, SliderSetting.rgba(255, 255, 255, 245.0f * f4));
        Render2D.outline(f19 - 0.5f, f20 - 0.5f, f16 + 1.0f, f17 + 1.0f, f18, 0.5f, SliderSetting.rgba(16, 16, 16, 128.0f * f4));
    }

    private static int rgba(int n, int n2, int n3, float f) {
        int n4 = Math.max(0, Math.min(255, Math.round(f)));
        if (n4 <= 0) {
            return 0;
        }
        return new Color(n, n2, n3, n4).getRGB();
    }

    @Override
    public boolean isVisible() {
        return this.backend.isVisible();
    }

    @Override
    public boolean click(float f, float f2, float f3, float f4, float f5) {
        float f6 = f + 6.0f;
        float f7 = f3 - 12.0f;
        float f8 = f2 + 16.0f;
        if (f4 >= f6 - 4.0f && f4 <= f6 + f7 + 4.0f && f5 >= f8 - 5.0f && f5 <= f8 + 3.0f + 5.0f) {
            this.dragging = true;
            float f9 = SliderSetting.clamp01((f4 - f6) / f7);
            this.backend.setValue(this.backend.getMin() + f9 * (this.backend.getMax() - this.backend.getMin()));
            this.lastSoundText = this.formatValue(this.backend.getValue());
            Sounds.play("slider");
            return true;
        }
        return false;
    }

    @Override
    public void releaseDrag() {
        this.dragging = false;
    }

    @Override
    public boolean middleClick(float f, float f2, float f3, float f4, float f5) {
        float f6 = f + 6.0f;
        float f7 = f3 - 12.0f;
        float f8 = f2 + 16.0f;
        if (f4 >= f6 - 4.0f && f4 <= f6 + f7 + 4.0f && f5 >= f8 - 5.0f && f5 <= f8 + 3.0f + 5.0f) {
            this.dragging = false;
            this.backend.setValue(this.backend.getDefaultValue());
            Sounds.play("slider");
            return true;
        }
        return false;
    }

    @Override
    public float preferredWidth() {
        float f = Fonts.MONTSERRAT_MEDIUM.width(this.backend.getDisplayName(), 6.5f);
        float f2 = Fonts.MONTSERRAT_MEDIUM.width(this.formatValue(this.backend.getMax()), 6.0f);
        return Math.max(100.0f, 6.0f + f + 6.0f + f2 + 6.0f);
    }

    private String formatValue(float f) {
        String formatted=backend.formatted(f);if(formatted!=null)return formatted;
        if ("Размер".equals(this.backend.getName()) || "Свой масштаб".equals(this.backend.getName())) {
            return Math.round(f * 100.0f) + "%";
        }
        if (this.backend.isInteger()) {
            return String.format("%.0f", Float.valueOf(f));
        }
        float f2 = this.backend.getIncrement();
        int n = 1;
        if (f2 > 0.0f) {
            float f3 = f2;
            for (n = 0; n < 3 && Math.abs(f3 - (float)Math.round(f3)) > 1.0E-4f; ++n) {
                f3 *= 10.0f;
            }
            n = Math.max(n, 1);
        }
        return String.format("%." + n + "f", Float.valueOf(f));
    }
}

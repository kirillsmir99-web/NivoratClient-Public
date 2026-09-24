package activity.client.gui.component;

import activity.client.gui.animation.AnimationClock;
import activity.client.gui.render.ActivityGuiRenderer;
import activity.client.gui.theme.ActivityColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

public class ActivitySlider extends ActivityComponent {

    private double min;
    private double max;
    private double step;
    private double value;

    @Nullable
    private Text label;
    private Function<Double, Text> valueFormatter;
    private Consumer<Double> onChanged;

    private boolean dragging = false;
    private final int thumbWidth = 8;
    private final int thumbHeight = 12;

    @Nullable
    private Text cachedValueText = null;
    @Nullable
    private Text cachedWrappedValue = null;
    private int cachedValueWidth = -1;

    @Nullable
    private Text cachedDisplayLabel = null;
    private int lastCalculatedLabelMaxW = -1;

    private int cachedLabelWidth = -1;
    private int cachedSampleMaxTextWidth = -1;
    private float thumbHoverProgress = 0.0f;
    private float visualNorm = -1.0f;
    private float valuePulse = 0.0f;

    private void updateValueCache() {
        this.cachedValueText = this.valueFormatter.apply(this.value);
        this.cachedWrappedValue = activity.client.gui.font.FontManager.wrap(this.cachedValueText);
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc != null ? mc.textRenderer : null;
        this.cachedValueWidth = (tr != null && this.cachedWrappedValue != null) ? activity.client.gui.font.UiTextRenderer.getWidth(tr, this.cachedWrappedValue) : 20;
    }

    public float getVisualNorm() {
        return visualNorm >= 0.0f ? visualNorm : (float) getNormalized();
    }

    public float getThumbHoverProgress() {
        return thumbHoverProgress;
    }

    public float getValuePulse() {
        return valuePulse;
    }

    public ActivitySlider(int x, int y, int width, int height,
                          double min, double max, double initialValue, double step,
                          @Nullable Text label,
                          @Nullable Function<Double, Text> valueFormatter,
                          @Nullable Consumer<Double> onChanged) {
        super(x, y, width, height);
        this.min = Double.isNaN(min) || Double.isInfinite(min) ? 0.0 : min;
        this.max = Double.isNaN(max) || Double.isInfinite(max) ? 1.0 : max;
        if (this.min > this.max) {
            double temp = this.min;
            this.min = this.max;
            this.max = temp;
        }
        this.step = (Double.isNaN(step) || Double.isInfinite(step) || step < 0) ? 0.0 : step;
        this.label = label;
        this.valueFormatter = valueFormatter != null ? valueFormatter : (val -> Text.literal(String.format(Locale.ROOT, "%.0f", val)));
        this.onChanged = onChanged;
        this.setValue(Double.isNaN(initialValue) || Double.isInfinite(initialValue) ? this.min : initialValue);
    }

    public ActivitySlider(int x, int y, int width, int height,
                          double min, double max, double initialValue,
                          @Nullable Text label,
                          @Nullable Consumer<Double> onChanged) {
        this(x, y, width, height, min, max, initialValue, 0.0, label, null, onChanged);
    }

    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        if (Double.isNaN(min) || Double.isInfinite(min)) return;
        this.min = min;
        if (this.min > this.max) {
            this.max = this.min;
        }
        this.cachedSampleMaxTextWidth = -1;
        setValue(this.value);
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        if (Double.isNaN(max) || Double.isInfinite(max)) return;
        this.max = max;
        if (this.max < this.min) {
            this.min = this.max;
        }
        this.cachedSampleMaxTextWidth = -1;
        setValue(this.value);
    }

    public double getStep() {
        return step;
    }

    public void setStep(double step) {
        if (Double.isNaN(step) || Double.isInfinite(step) || step < 0) step = 0.0;
        this.step = step;
        setValue(this.value);
    }

    @Nullable
    public Text getLabel() {
        return label;
    }

    public void setLabel(@Nullable Text label) {
        this.label = label;
        this.cachedLabelWidth = -1;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double newValue) {
        if (Double.isNaN(newValue) || Double.isInfinite(newValue)) {
            return;
        }
        double clamped = Math.clamp(newValue, this.min, this.max);
        if (this.step > 0) {
            clamped = this.min + Math.round((clamped - this.min) / this.step) * this.step;
            clamped = Math.clamp(clamped, this.min, this.max);
        }

        if (Double.compare(this.value, clamped) != 0 || this.cachedValueText == null) {
            double oldValue = this.value;
            this.value = clamped;
            this.cachedValueText = this.valueFormatter.apply(this.value);
            if (this.onChanged != null) {
                this.onChanged.accept(this.value);
            }
            if (Double.compare(oldValue, clamped) != 0) {
                if (AnimationClock.isAnimationsEnabled()) {
                    this.valuePulse = 1.0f;
                } else {
                    this.valuePulse = 0.0f;
                    this.visualNorm = (float) getNormalized();
                }
                activity.client.gui.sound.SoundManager.playSliderRatchet(this.value, this.min, this.max, this.step);
            }
        }
    }

    public void setValueSilently(double newValue) {
        if (!Double.isFinite(newValue)) return;
        double clamped = Math.clamp(newValue, this.min, this.max);
        if (this.step > 0) {
            clamped = this.min + Math.round((clamped - this.min) / this.step) * this.step;
            clamped = Math.clamp(clamped, this.min, this.max);
        }
        this.value = clamped;
        this.cachedValueText = this.valueFormatter.apply(clamped);
        this.visualNorm = (float) getNormalized();
        this.valuePulse = 0.0f;
    }

    public double getNormalized() {
        if (this.max <= this.min) return 0.0;
        return (this.value - this.min) / (this.max - this.min);
    }

    public void setNormalized(double norm) {
        if (Double.isNaN(norm) || Double.isInfinite(norm)) return;
        double clampedNorm = Math.clamp(norm, 0.0, 1.0);
        setValue(this.min + clampedNorm * (this.max - this.min));
        if (!AnimationClock.isAnimationsEnabled()) {
            this.visualNorm = (float) clampedNorm;
        }
    }

    public boolean isDragging() {
        return dragging;
    }

    private boolean shouldDrawInternalLabel() {
        return this.label != null && this.width >= 120;
    }

    private int getLabelWidth() {
        if (this.label == null) return 0;
        if (this.cachedLabelWidth < 0) {
            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            if (tr != null) {
                this.cachedLabelWidth = activity.client.gui.font.UiTextRenderer.getWidth(tr, this.label);
            } else {
                return 0;
            }
        }
        return this.cachedLabelWidth;
    }

    private int getTrackX() {
        if (!shouldDrawInternalLabel()) return this.x + 4;
        int maxLabelW = (int) (this.width * 0.40f);
        int labelW = Math.min(getLabelWidth(), maxLabelW);
        return this.x + labelW + 8;
    }

    private int getSampleMaxWidth() {
        if (this.cachedSampleMaxTextWidth < 0) {
            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            if (tr != null) {
                int minW = activity.client.gui.font.UiTextRenderer.getWidth(tr, this.valueFormatter.apply(this.min));
                int maxW = activity.client.gui.font.UiTextRenderer.getWidth(tr, this.valueFormatter.apply(this.max));
                this.cachedSampleMaxTextWidth = Math.max(minW, maxW) + 6;
            } else {
                return 20;
            }
        }
        return this.cachedSampleMaxTextWidth;
    }

    private int getTrackWidth() {
        int valueWidth = getSampleMaxWidth();
        int startX = getTrackX();
        int endX = this.x + this.width - valueWidth - 2;
        return Math.max(30, endX - startX);
    }

    private void updateFromMouse(double mouseX) {
        int trackX = getTrackX();
        int trackW = getTrackWidth();
        if (trackW <= this.thumbWidth) return;
        double norm = (mouseX - trackX - this.thumbWidth / 2.0) / (trackW - this.thumbWidth);
        setNormalized(Math.clamp(norm, 0.0, 1.0));
    }

    @Override
    protected void renderComponent(DrawContext context, int mouseX, int mouseY, float delta) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int fontH = activity.client.gui.font.UiTextRenderer.getFontHeight(tr);
        int textY = this.y + (this.height - fontH) / 2;

        boolean animEnabled = AnimationClock.isAnimationsEnabled();
        float targetNorm = (float) getNormalized();

        if (!animEnabled) {
            this.visualNorm = targetNorm;
            this.thumbHoverProgress = (this.enabled && (this.hovered || this.dragging)) ? 1.0f : 0.0f;
            this.valuePulse = 0.0f;
        } else {
            if (this.visualNorm < 0.0f) {
                this.visualNorm = targetNorm;
            } else if (this.dragging) {

                float decay = (this.step > 0) ? 38.0f : 55.0f;
                this.visualNorm = AnimationClock.approachExp(this.visualNorm, targetNorm, decay);
            } else {

                this.visualNorm = AnimationClock.approachExp(this.visualNorm, targetNorm, 24.0f);
            }
            if (Math.abs(this.visualNorm - targetNorm) < 0.0005f) {
                this.visualNorm = targetNorm;
            }

            boolean isHovered = this.enabled && (this.hovered || this.dragging);
            float targetHover = isHovered ? 1.0f : 0.0f;
            this.thumbHoverProgress = AnimationClock.approach(this.thumbHoverProgress, targetHover, AnimationClock.DURATION_HOVER);
            this.valuePulse = AnimationClock.approach(this.valuePulse, 0.0f, 0.18f);
        }

        if (shouldDrawInternalLabel()) {
            int labelColor = this.enabled ? ActivityColors.TEXT_PRIMARY : ActivityColors.TEXT_DISABLED;
            int maxLabelW = (int) (this.width * 0.40f);
            Text displayLabel = this.label;
            int labelW = activity.client.gui.font.UiTextRenderer.getWidth(tr, this.label);
            if (labelW > maxLabelW) {
                String trimmed = activity.client.gui.font.UiTextRenderer.trimToWidth(tr, this.label.getString(), Math.max(8, maxLabelW - 6)) + "…";
                displayLabel = Text.literal(trimmed);
            }
            activity.client.gui.font.UiTextRenderer.drawTextWithShadow(context, tr, displayLabel, this.x + 2, textY, labelColor);
        }

        if (this.cachedValueText == null) {
            this.cachedValueText = this.valueFormatter.apply(this.value);
        }
        int valueWidth = activity.client.gui.font.UiTextRenderer.getWidth(tr, this.cachedValueText);
        int valueX = this.x + this.width - valueWidth - 2;

        float activeFactor = Math.max(this.dragging ? 1.0f : 0.0f, Math.max(this.thumbHoverProgress * 0.7f, this.valuePulse));
        int valueColor = this.enabled
            ? ActivityColors.interpolateColor(ActivityColors.TEXT_PRIMARY, ActivityColors.TEXT_ACCENT, activeFactor)
            : ActivityColors.TEXT_DISABLED;
        activity.client.gui.font.UiTextRenderer.drawTextWithShadow(context, tr, this.cachedValueText, valueX, textY, valueColor);

        int trackX = getTrackX();
        int trackW = getTrackWidth();
        int trackH = 3;
        int trackY = this.y + (this.height - trackH) / 2;

        float norm = Math.clamp(this.visualNorm, 0.0f, 1.0f);
        int thumbX = trackX + (int) Math.round(norm * (trackW - this.thumbWidth));
        int thumbY = this.y + (this.height - this.thumbHeight) / 2;

        int emptyTrackColor = 0xFF20242D;
        int emptyTrackBorder = ActivityColors.interpolateColor(ActivityColors.BORDER_INPUT, ActivityColors.BORDER_HOVER, this.thumbHoverProgress * 0.7f);
        ActivityGuiRenderer.fill(context, trackX, trackY, trackW, trackH, emptyTrackColor);
        ActivityGuiRenderer.drawBorder(context, trackX, trackY, trackW, trackH, emptyTrackBorder);

        int filledWidth = thumbX - trackX + this.thumbWidth / 2;
        if (filledWidth > 0) {
            int baseFill = ActivityColors.ACCENT_PRIMARY;
            int brightFill = ActivityColors.ACCENT_LIGHT;
            float fillLuminance = Math.max(this.thumbHoverProgress, this.dragging ? 1.0f : 0.0f);
            int fillColor = this.enabled ? ActivityColors.interpolateColor(baseFill, brightFill, fillLuminance) : ActivityColors.BORDER;
            ActivityGuiRenderer.fill(context, trackX, trackY, filledWidth, trackH, fillColor);

            if (this.enabled) {
                ActivityGuiRenderer.fill(context, trackX, trackY, filledWidth, 1, 0x40FFFFFF);
            }

            if (filledWidth >= 2 && this.enabled) {
                float tipPulse = Math.max(0.45f, this.valuePulse * 0.85f);
                int tipColor = ActivityColors.interpolateColor(fillColor, 0xFF80D8FF, tipPulse);
                ActivityGuiRenderer.fill(context, trackX + filledWidth - 2, trackY, 2, trackH, tipColor);
            }
        }

        int normalBorder = ActivityColors.BORDER_CARD;
        int activeBorder = (this.dragging || this.focused) ? ActivityColors.ACCENT_PRIMARY : ActivityColors.BORDER_HOVER;
        float borderProgress = (this.dragging || this.focused) ? 1.0f : this.thumbHoverProgress;
        int thumbBorder = ActivityColors.interpolateColor(normalBorder, activeBorder, borderProgress);
        int thumbBg = this.enabled ? 0xFFFFFFFF : ActivityColors.TEXT_DISABLED;

        ActivityGuiRenderer.drawPanel(context, thumbX, thumbY, this.thumbWidth, this.thumbHeight, thumbBg, thumbBorder);

        if (this.enabled) {
            int handleSpecular = this.thumbHoverProgress > 0.001f ? 0x60FFFFFF : 0x40FFFFFF;
            ActivityGuiRenderer.fill(context, thumbX + 1, thumbY + 1, this.thumbWidth - 2, 1, handleSpecular);
            ActivityGuiRenderer.fill(context, thumbX + 1, thumbY + this.thumbHeight - 2, this.thumbWidth - 2, 1, 0x22000000);
        }

        if (this.enabled && this.thumbHoverProgress > 0.001f) {
            int halo1 = ActivityColors.withAlpha(activeBorder, (int) (170 * this.thumbHoverProgress));
            ActivityGuiRenderer.drawBorder(context, thumbX - 1, thumbY - 1, this.thumbWidth + 2, this.thumbHeight + 2, halo1);
            if (this.thumbHoverProgress > 0.15f) {
                int halo2 = ActivityColors.withAlpha(activeBorder, (int) (40 * this.thumbHoverProgress));
                ActivityGuiRenderer.drawBorder(context, thumbX - 2, thumbY - 2, this.thumbWidth + 4, this.thumbHeight + 4, halo2);
            }
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (!this.enabled || !this.visible || click.button() != 0) {
            return false;
        }

        int trackX = getTrackX();
        int trackW = getTrackWidth();
        int trackY = this.y + (this.height - this.thumbHeight) / 2;

        boolean inInteractiveArea = click.x() >= trackX - 2 && click.x() <= trackX + trackW + 2 &&
                                   click.y() >= trackY && click.y() <= trackY + this.thumbHeight;

        if (inInteractiveArea || this.isMouseOver(click.x(), click.y())) {
            this.dragging = true;
            this.setFocused(true);
            updateFromMouse(click.x());
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (this.dragging) {
            this.dragging = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (this.enabled && this.visible && this.dragging) {
            updateFromMouse(click.x());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.enabled && this.visible && this.isMouseOver(mouseX, mouseY)) {
            double stepAmount = (this.step > 0) ? this.step : (this.max - this.min) * 0.05;
            setValue(this.value + stepAmount * Math.signum(verticalAmount));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (!this.enabled || !this.visible || !this.focused) {
            return false;
        }

        double stepAmount = (this.step > 0) ? this.step : (this.max - this.min) * 0.05;

        switch (input.key()) {
            case GLFW.GLFW_KEY_LEFT -> {
                setValue(this.value - stepAmount);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                setValue(this.value + stepAmount);
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                setValue(this.min);
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                setValue(this.max);
                return true;
            }
        }

        return false;
    }

    @Override
    public void onFontChanged() {
        this.cachedLabelWidth = -1;
        this.cachedSampleMaxTextWidth = -1;
        this.cachedValueText = null;
    }
}

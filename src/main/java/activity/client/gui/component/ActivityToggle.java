package activity.client.gui.component;

import activity.client.gui.animation.AnimationClock;
import activity.client.gui.render.ActivityGuiRenderer;
import activity.client.gui.theme.ActivityColors;
import activity.client.gui.theme.ActivityMetrics;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;

import java.util.function.Consumer;

public class ActivityToggle extends ActivityComponent {

    private boolean state = false;
    private float animationProgress = 0.0f;
    private float hoverProgress = 0.0f;
    private Consumer<Boolean> onToggle;
    private int touchPaddingX = ActivityMetrics.TOUCH_TOGGLE_PAD_X;
    private int touchPaddingY = ActivityMetrics.TOUCH_TOGGLE_PAD_Y;

    public ActivityToggle(int x, int y, boolean initialState, Consumer<Boolean> onToggle) {
        super(x, y, ActivityMetrics.TOGGLE_WIDTH, ActivityMetrics.TOGGLE_HEIGHT);
        this.state = initialState;
        this.animationProgress = initialState ? 1.0f : 0.0f;
        this.onToggle = onToggle;
    }

    public int getTouchPaddingX() {
        return touchPaddingX;
    }

    public void setTouchPaddingX(int touchPaddingX) {
        this.touchPaddingX = Math.max(0, touchPaddingX);
    }

    public int getTouchPaddingY() {
        return touchPaddingY;
    }

    public void setTouchPaddingY(int touchPaddingY) {
        this.touchPaddingY = Math.max(0, touchPaddingY);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!this.visible) return false;
        return mouseX >= (this.x - this.touchPaddingX) && mouseX < (this.x + this.width + this.touchPaddingX) &&
               mouseY >= (this.y - this.touchPaddingY) && mouseY < (this.y + this.height + this.touchPaddingY);
    }

    public ActivityToggle(int x, int y, boolean initialState) {
        this(x, y, initialState, null);
    }

    public ActivityToggle(int x, int y) {
        this(x, y, false, null);
    }

    public boolean getState() {
        return state;
    }

    public void setState(boolean state) {
        this.setState(state, true);
    }

    public void setState(boolean state, boolean animate) {
        this.state = state;
        if (!animate || !AnimationClock.isAnimationsEnabled()) {
            this.animationProgress = state ? 1.0f : 0.0f;
        }
    }

    public float getAnimationProgress() {
        return animationProgress;
    }

    public float getHoverProgress() {
        return hoverProgress;
    }

    @FunctionalInterface
    public interface ToggleConfirmHandler {

        boolean interceptToggle(boolean currentState, boolean targetState, Runnable proceed, Runnable cancel);
    }

    private ToggleConfirmHandler confirmHandler = null;

    public void setConfirmHandler(ToggleConfirmHandler confirmHandler) {
        this.confirmHandler = confirmHandler;
    }

    public ToggleConfirmHandler getConfirmHandler() {
        return this.confirmHandler;
    }

    public void setConfirmTurnOff(java.util.function.BiFunction<Runnable, Runnable, activity.client.gui.modal.BaseModal> modalFactory, activity.client.gui.modal.ModalManager modalManager) {
        this.setConfirmHandler((currentState, targetState, proceed, cancel) -> {
            if (currentState && !targetState) {
                if (modalFactory != null && modalManager != null) {
                    activity.client.gui.modal.BaseModal modal = modalFactory.apply(proceed, cancel);
                    modalManager.open(modal);
                    return true;
                }
            }
            return false;
        });
    }

    public void setConfirmTurnOff(net.minecraft.text.Text title, net.minecraft.text.Text desc, net.minecraft.text.Text confirmText, activity.client.gui.modal.ModalManager modalManager) {
        this.setConfirmHandler((currentState, targetState, proceed, cancel) -> {
            if (currentState && !targetState) {
                if (modalManager != null) {
                    modalManager.showConfirmation(title, desc, confirmText, true, proceed);
                    return true;
                }
            }
            return false;
        });
    }

    public void setOnToggle(Consumer<Boolean> onToggle) {
        this.onToggle = onToggle;
    }

    public void toggle() {
        boolean targetState = !this.state;
        if (this.confirmHandler != null && this.confirmHandler.interceptToggle(this.state, targetState, () -> applyState(targetState), () -> {})) {
            return;
        }
        applyState(targetState);
    }

    public void applyState(boolean newState) {
        this.state = newState;
        if (!AnimationClock.isAnimationsEnabled()) {
            this.animationProgress = this.state ? 1.0f : 0.0f;
        }
        activity.client.gui.sound.SoundManager.playToggle(this.state);
        if (this.onToggle != null) {
            this.onToggle.accept(this.state);
        }
    }

    @Override
    protected void renderComponent(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean animEnabled = AnimationClock.isAnimationsEnabled();
        float target = this.state ? 1.0f : 0.0f;
        float targetHover = (this.hovered && this.enabled) ? 1.0f : 0.0f;

        if (!animEnabled) {
            this.animationProgress = target;
            this.hoverProgress = targetHover;
        } else {
            this.animationProgress = AnimationClock.approach(this.animationProgress, target, AnimationClock.DURATION_TOGGLE);
            this.hoverProgress = AnimationClock.approach(this.hoverProgress, targetHover, AnimationClock.DURATION_HOVER);
        }

        float eased = AnimationClock.smoothStep(this.animationProgress);

        int trackColor = ActivityColors.interpolateColor(ActivityColors.STATE_OFF_BG, ActivityColors.STATE_ON_BG, eased);
        if (this.hoverProgress > 0.001f && this.enabled) {
            trackColor = ActivityColors.interpolateColor(trackColor, ActivityColors.withAlpha(0xFFFFFFFF, 30), this.hoverProgress * 0.25f);
        }

        int normalBorder = ActivityColors.BORDER_INPUT;
        int activeBorder = ActivityColors.interpolateColor(normalBorder, 0xFF35D96B, eased * 0.55f);
        int hoverBorder = ActivityColors.BORDER_HOVER;
        int borderColor = ActivityColors.interpolateColor(activeBorder, hoverBorder, this.hoverProgress);

        ActivityGuiRenderer.drawPanel(context, this.x, this.y, this.width, this.height, trackColor, borderColor);

        if (this.width > 4 && this.height > 4) {
            ActivityGuiRenderer.fill(context, this.x + 1, this.y + 1, this.width - 2, 1, 0x22000000);
            if (eased > 0.01f && this.enabled) {
                int innerGlow = ActivityColors.withAlpha(ActivityColors.STATE_ON_BG, (int) (45 * eased));
                ActivityGuiRenderer.fill(context, this.x + 1, this.y + 1, this.width - 2, 1, innerGlow);
            }
        }

        if (this.focused && this.enabled) {
            ActivityGuiRenderer.drawBorder(context, this.x - 1, this.y - 1, this.width + 2, this.height + 2, ActivityColors.ACCENT_PRIMARY);
        }

        int knobBaseWidth = 11;
        int knobHeight = this.height - 4;
        int knobY = this.y + 2;
        int minKnobX = this.x + 2;
        int maxKnobX = this.x + this.width - knobBaseWidth - 2;

        float velocity = (float) Math.sin(this.animationProgress * Math.PI);
        int stretch = animEnabled ? Math.round(velocity * 1.8f) : 0;
        int knobWidth = knobBaseWidth + stretch;

        int knobX = minKnobX + Math.round(eased * (maxKnobX - minKnobX));
        if (this.state) {
            knobX = Math.min(knobX, maxKnobX + knobBaseWidth - knobWidth);
        } else {
            knobX = Math.max(minKnobX, knobX - stretch);
        }
        knobX = Math.clamp(knobX, minKnobX, this.x + this.width - knobWidth - 2);

        int knobColor = this.enabled ? ActivityColors.TOGGLE_KNOB : ActivityColors.TEXT_DISABLED;
        ActivityGuiRenderer.fill(context, knobX, knobY, knobWidth, knobHeight, knobColor);

        if (this.enabled) {
            int topGlint = this.hoverProgress > 0.001f ? 0x60FFFFFF : 0x48FFFFFF;
            ActivityGuiRenderer.fill(context, knobX, knobY, knobWidth, 1, topGlint);
            ActivityGuiRenderer.fill(context, knobX, knobY + knobHeight - 1, knobWidth, 1, 0x24000000);
            int knobBorder = ActivityColors.interpolateColor(0x18000000, 0x3026D95F, eased);
            ActivityGuiRenderer.drawBorder(context, knobX, knobY, knobWidth, knobHeight, knobBorder);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (this.enabled && this.visible && click.button() == 0 && this.isMouseOver(click.x(), click.y())) {
            this.toggle();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (this.enabled && this.visible && this.focused && input.isEnterOrSpace()) {
            this.toggle();
            return true;
        }
        return false;
    }
}

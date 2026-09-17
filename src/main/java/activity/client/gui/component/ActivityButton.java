package activity.client.gui.component;

import activity.client.gui.animation.AnimationClock;
import activity.client.gui.render.ActivityGuiRenderer;
import activity.client.gui.render.ScissorHelper;
import activity.client.gui.theme.ActivityColors;
import activity.client.gui.theme.ActivityMetrics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

import activity.client.gui.icon.ActivityIcon;
import activity.client.gui.icon.ActivityIconRenderer;
import org.jetbrains.annotations.Nullable;

/**
 * Modern dark button component adhering to the Activity design system.
 * Supports SECONDARY, PRIMARY, and DANGER visual variants with smooth
 * 100ms hover transition progress (subtle fade in/out hover overlay and border highlight).
 * Also supports optional ActivityIcon displayed to the left of the label or centered standalone.
 */
public class ActivityButton extends ActivityComponent {

    public enum Variant {
        SECONDARY,
        PRIMARY,
        DANGER
    }

    @FunctionalInterface
    public interface PressAction {
        void onPress(ActivityButton button);
    }

    @Nullable
    private ActivityIcon icon;
    private Text message;
    private Variant variant = Variant.SECONDARY;
    private PressAction onPress;

    private float hoverProgress = 0.0f;
    private int touchPadding = 0;

    private Text cachedDisplayText = null;
    private int cachedDisplayWidth = -1;
    private int lastCalculatedWidth = -1;

    public ActivityButton(int x, int y, int width, int height, Text message, PressAction onPress) {
        this(x, y, width, height, null, message, Variant.SECONDARY, onPress);
    }

    public ActivityButton(int x, int y, int width, int height, Text message, Variant variant, PressAction onPress) {
        this(x, y, width, height, null, message, variant, onPress);
    }

    public ActivityButton(int x, int y, int width, int height, @Nullable ActivityIcon icon, @Nullable Text message, PressAction onPress) {
        this(x, y, width, height, icon, message, Variant.SECONDARY, onPress);
    }

    public ActivityButton(int x, int y, int width, int height, @Nullable ActivityIcon icon, @Nullable Text message, Variant variant, PressAction onPress) {
        super(x, y, width, height);
        this.icon = icon;
        this.message = message;
        this.variant = variant;
        this.onPress = onPress;
        if ((this.message == null || this.message.getString().isEmpty()) && this.width <= 24) {
            this.touchPadding = ActivityMetrics.TOUCH_HITBOX_PADDING;
        }
    }

    public int getTouchPadding() {
        return touchPadding;
    }

    public void setTouchPadding(int touchPadding) {
        this.touchPadding = Math.max(0, touchPadding);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!this.visible) return false;
        int pad = this.touchPadding;
        return mouseX >= (this.x - pad) && mouseX < (this.x + this.width + pad) &&
               mouseY >= (this.y - pad) && mouseY < (this.y + this.height + pad);
    }

    @Nullable
    public ActivityIcon getIcon() {
        return icon;
    }

    public void setIcon(@Nullable ActivityIcon icon) {
        this.icon = icon;
    }

    public Text getMessage() {
        return message;
    }

    public void setMessage(Text message) {
        this.message = message;
        this.cachedDisplayText = null;
        this.cachedDisplayWidth = -1;
    }

    @Override
    public void onFontChanged() {
        this.cachedDisplayText = null;
        this.cachedDisplayWidth = -1;
    }

    public Variant getVariant() {
        return variant;
    }

    public void setVariant(Variant variant) {
        this.variant = variant;
    }

    public void setOnPress(PressAction onPress) {
        this.onPress = onPress;
    }

    public void onPress() {
        if (this.enabled && this.visible && this.onPress != null) {
            this.onPress.onPress(this);
        }
    }

    @Override
    protected void renderComponent(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean isHovered = this.enabled && this.hovered;
        float targetHover = isHovered ? 1.0f : 0.0f;
        this.hoverProgress = AnimationClock.approach(this.hoverProgress, targetHover, AnimationClock.DURATION_HOVER);

        int bgColor;
        int borderColor;
        int textColor;

        switch (this.variant) {
            case PRIMARY -> {
                bgColor = ActivityColors.interpolateColor(ActivityColors.BUTTON_PRIMARY_BG, ActivityColors.BUTTON_PRIMARY_HOVER, this.hoverProgress);
                borderColor = ActivityColors.interpolateColor(ActivityColors.ACCENT_PRIMARY, ActivityColors.ACCENT_LIGHT, this.hoverProgress);
                textColor = this.enabled ? ActivityColors.TEXT_PRIMARY : ActivityColors.TEXT_DISABLED;
            }
            case DANGER -> {
                bgColor = ActivityColors.BUTTON_DANGER_BG;
                borderColor = ActivityColors.interpolateColor(ActivityColors.BUTTON_DANGER_BORDER, ActivityColors.BUTTON_DANGER_HOVER_BORDER, this.hoverProgress);
                int activeTextColor = ActivityColors.interpolateColor(ActivityColors.DANGER, 0xFFFF8080, this.hoverProgress);
                textColor = this.enabled ? activeTextColor : ActivityColors.TEXT_DISABLED;
            }
            case SECONDARY -> {
                int targetBg = ActivityColors.withAlpha(ActivityColors.BUTTON_SECONDARY_BG, 0xF0);
                bgColor = ActivityColors.interpolateColor(ActivityColors.BUTTON_SECONDARY_BG, targetBg, this.hoverProgress);
                borderColor = ActivityColors.interpolateColor(ActivityColors.BORDER, ActivityColors.BORDER_HOVER, this.hoverProgress);
                int activeTextColor = ActivityColors.interpolateColor(ActivityColors.TEXT_SECONDARY, ActivityColors.TEXT_PRIMARY, this.hoverProgress);
                textColor = this.enabled ? activeTextColor : ActivityColors.TEXT_DISABLED;
            }
            default -> {
                bgColor = ActivityColors.BUTTON_SECONDARY_BG;
                borderColor = ActivityColors.BORDER;
                textColor = ActivityColors.TEXT_PRIMARY;
            }
        }

        // Draw background and 1px border
        ActivityGuiRenderer.drawPanel(context, this.x, this.y, this.width, this.height, bgColor, borderColor);

        // Hover highlight overlay for secondary buttons (subtle 100ms fade in/out)
        if (this.variant == Variant.SECONDARY && this.hoverProgress > 0.001f) {
            int maxAlpha = (ActivityColors.BUTTON_SECONDARY_HOVER >>> 24) & 0xFF; // 0x33 = 51
            int overlayAlpha = (int) (maxAlpha * this.hoverProgress);
            int overlayColor = (overlayAlpha << 24) | (ActivityColors.BUTTON_SECONDARY_HOVER & 0x00FFFFFF);
            ActivityGuiRenderer.fill(context, this.x + 1, this.y + 1, this.width - 2, this.height - 2, overlayColor);
        }

        // Focused outline highlight
        if (this.focused && this.enabled) {
            ActivityGuiRenderer.drawBorder(context, this.x, this.y, this.width, this.height, ActivityColors.ACCENT_PRIMARY);
        }

        // Draw button label and/or icon with responsive clipping
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int fontH = activity.client.gui.font.UiTextRenderer.getFontHeight(tr);
        int textY = this.y + (this.height - fontH) / 2;

        ScissorHelper.pushScissor(context, this.x + 2, this.y + 1, Math.max(1, this.width - 4), Math.max(1, this.height - 2));
        try {
            if (this.icon != null && this.message != null) {
                int iconSize = this.icon.getWidth();
                int gap = 4;
                if (this.width < iconSize + 16) {
                    // Ultra-narrow: show icon only
                    ActivityIconRenderer.drawCentered(context, this.icon, this.x, this.y, this.width, this.height, textColor);
                } else {
                    int maxTextW = this.width - iconSize - gap - 6;
                    if (this.cachedDisplayText == null || this.lastCalculatedWidth != this.width) {
                        int fullWidth = activity.client.gui.font.UiTextRenderer.getWidth(tr, this.message);
                        if (maxTextW > 8 && fullWidth > maxTextW) {
                            int ellW = activity.client.gui.font.UiTextRenderer.getWidth(tr, "…");
                            int targetW = Math.max(0, maxTextW - ellW);
                            String trimmed = activity.client.gui.font.UiTextRenderer.trimToWidth(tr, this.message.getString(), targetW) + "…";
                            if (activity.client.gui.font.UiTextRenderer.getWidth(tr, trimmed) > maxTextW) {
                                trimmed = activity.client.gui.font.UiTextRenderer.trimToWidth(tr, trimmed, Math.max(0, maxTextW));
                            }
                            this.cachedDisplayText = Text.literal(trimmed);
                        } else {
                            this.cachedDisplayText = this.message;
                        }
                        this.cachedDisplayWidth = activity.client.gui.font.UiTextRenderer.getWidth(tr, this.cachedDisplayText);
                        this.lastCalculatedWidth = this.width;
                    }
                    int totalContentWidth = iconSize + gap + this.cachedDisplayWidth;
                    int startX = Math.max(this.x + 3, this.x + (this.width - totalContentWidth) / 2);
                    int iconY = this.y + (this.height - this.icon.getHeight()) / 2;
                    ActivityIconRenderer.draw(context, this.icon, startX, iconY, textColor);
                    activity.client.gui.font.UiTextRenderer.drawTextWithShadow(context, tr, this.cachedDisplayText, startX + iconSize + gap, textY, textColor);
                }
            } else if (this.icon != null) {
                ActivityIconRenderer.drawCentered(context, this.icon, this.x, this.y, this.width, this.height, textColor);
            } else if (this.message != null) {
                if (this.cachedDisplayText == null || this.lastCalculatedWidth != this.width) {
                    int maxTextW = this.width - 6;
                    int fullWidth = activity.client.gui.font.UiTextRenderer.getWidth(tr, this.message);
                    if (maxTextW > 8 && fullWidth > maxTextW) {
                        int ellW = activity.client.gui.font.UiTextRenderer.getWidth(tr, "…");
                        int targetW = Math.max(0, maxTextW - ellW);
                        String trimmed = activity.client.gui.font.UiTextRenderer.trimToWidth(tr, this.message.getString(), targetW) + "…";
                        if (activity.client.gui.font.UiTextRenderer.getWidth(tr, trimmed) > maxTextW) {
                            trimmed = activity.client.gui.font.UiTextRenderer.trimToWidth(tr, trimmed, Math.max(0, maxTextW));
                        }
                        this.cachedDisplayText = Text.literal(trimmed);
                    } else {
                        this.cachedDisplayText = this.message;
                    }
                    this.cachedDisplayWidth = activity.client.gui.font.UiTextRenderer.getWidth(tr, this.cachedDisplayText);
                    this.lastCalculatedWidth = this.width;
                }
                activity.client.gui.font.UiTextRenderer.drawCenteredTextWithShadow(context, tr, this.cachedDisplayText, this.x + this.width / 2, textY, textColor);
            }
        } finally {
            ScissorHelper.popScissor(context);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (this.enabled && this.visible && click.button() == 0 && this.isMouseOver(click.x(), click.y())) {
            if (this.variant == Variant.PRIMARY) {
                activity.client.gui.sound.SoundManager.playButtonPrimary();
            } else {
                activity.client.gui.sound.SoundManager.playButtonSecondary();
            }
            if (this.onPress != null) {
                this.onPress.onPress(this);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (this.enabled && this.visible && this.focused && input.isEnterOrSpace()) {
            if (this.variant == Variant.PRIMARY) {
                activity.client.gui.sound.SoundManager.playButtonPrimary();
            } else {
                activity.client.gui.sound.SoundManager.playButtonSecondary();
            }
            if (this.onPress != null) {
                this.onPress.onPress(this);
            }
            return true;
        }
        return false;
    }
}

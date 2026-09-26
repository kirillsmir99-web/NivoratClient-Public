package activity.client.gui.component;

import activity.client.gui.animation.AnimationClock;
import activity.client.gui.overlay.OverlayManager;
import activity.client.gui.render.ActivityGuiRenderer;
import activity.client.gui.render.ScissorHelper;
import activity.client.gui.theme.ActivityColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import activity.client.gui.icon.ActivityIcon;
import activity.client.gui.icon.ActivityIconRenderer;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class ActivityDropdown<T> extends ActivityComponent {

    private final OverlayManager overlayManager;
    private List<T> options;
    private T selectedOption;
    private Function<T, Text> nameProvider;
    @Nullable
    private Function<T, Text> tooltipProvider = null;
    private Consumer<T> onSelect;
    @Nullable
    private Consumer<T> onRightClick = null;

    private boolean expanded = false;
    @Nullable
    private DropdownPopup<T> activePopup = null;

    private float hoverProgress = 0.0f;
    private float focusProgress = 0.0f;

    private Text cachedWrappedLabel = null;
    private T lastSelectedOption = null;

    @Override
    public void onFontChanged() {
        this.cachedWrappedLabel = null;
        if (this.activePopup != null) {
            this.activePopup.onFontChanged();
        }
    }

    public ActivityDropdown(int x, int y, int width, int height,
                            OverlayManager overlayManager,
                            List<T> options,
                            T initialSelection,
                            Function<T, Text> nameProvider,
                            Consumer<T> onSelect) {
        super(x, y, width, height);
        this.overlayManager = overlayManager;
        this.options = options != null ? options : Collections.emptyList();
        this.selectedOption = initialSelection != null ? initialSelection : (this.options.isEmpty() ? null : this.options.get(0));
        this.nameProvider = nameProvider != null ? nameProvider : (item -> Text.literal(String.valueOf(item)));
        this.onSelect = onSelect;
    }

    public List<T> getOptions() {
        return options;
    }

    public void setOptions(List<T> options) {
        this.options = options != null ? options : Collections.emptyList();
        this.cachedWrappedLabel = null;
        if (!this.options.contains(this.selectedOption) && !this.options.isEmpty()) {
            setSelectedOption(this.options.get(0));
        }
    }

    public T getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(T selectedOption) {
        this.selectedOption = selectedOption;
        this.cachedWrappedLabel = null;
        if (this.onSelect != null) {
            this.onSelect.accept(this.selectedOption);
        }
    }

    public void setSelectedOptionSilently(T selectedOption) {
        this.selectedOption = selectedOption;
        this.cachedWrappedLabel = null;
    }

    public Consumer<T> getOnSelect() {
        return onSelect;
    }

    public void setOnSelect(Consumer<T> onSelect) {
        this.onSelect = onSelect;
    }

    @Nullable
    public Function<T, Text> getTooltipProvider() {
        return tooltipProvider;
    }

    public void setTooltipProvider(@Nullable Function<T, Text> tooltipProvider) {
        this.tooltipProvider = tooltipProvider;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        if (!expanded && this.activePopup != null) {
            this.activePopup = null;
        }
    }

    public void toggle() {
        if (this.expanded) {
            closePopup();
        } else {
            openPopup();
        }
    }

    public void openPopup() {
        if (!this.enabled || !this.visible || this.options.isEmpty()) return;

        activity.client.gui.sound.SoundManager.playDropdownOpen();
        this.expanded = true;
        this.activePopup = new DropdownPopup<>(this);
        this.overlayManager.open(this.activePopup);
    }

    public void closePopup() {
        if (this.activePopup != null) {
            this.overlayManager.close(this.activePopup);
            this.activePopup = null;
        }
        if (this.expanded) {
            activity.client.gui.sound.SoundManager.playDropdownClose();
        }
        this.expanded = false;
    }

    @Override
    protected void renderComponent(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean isHovered = this.enabled && this.hovered;
        float targetHover = isHovered ? 1.0f : 0.0f;
        float targetFocus = (this.expanded || (this.focused && this.enabled)) ? 1.0f : 0.0f;

        this.hoverProgress = AnimationClock.approach(this.hoverProgress, targetHover, AnimationClock.DURATION_HOVER);
        this.focusProgress = AnimationClock.approach(this.focusProgress, targetFocus, AnimationClock.DURATION_FOCUS);

        int baseBorder = ActivityColors.interpolateColor(ActivityColors.BORDER_INPUT, ActivityColors.BORDER_HOVER, this.hoverProgress);
        int borderColor = ActivityColors.interpolateColor(baseBorder, ActivityColors.ACCENT_PRIMARY, this.focusProgress);

        ActivityGuiRenderer.drawPanel(context, this.x, this.y, this.width, this.height, ActivityColors.FIELD_BACKGROUND, borderColor);

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int fontH = activity.client.gui.font.UiTextRenderer.getFontHeight(tr);
        int textY = this.y + (this.height - fontH) / 2;

        if (this.selectedOption != null) {
            if (this.cachedWrappedLabel == null || !java.util.Objects.equals(this.selectedOption, this.lastSelectedOption)) {
                this.lastSelectedOption = this.selectedOption;
                Text label = this.nameProvider.apply(this.selectedOption);
                this.cachedWrappedLabel = activity.client.gui.font.FontManager.wrap(label);
            }

            int maxTextWidth = this.width - 24;

            if (maxTextWidth > 0 && this.height > 2) {
                ScissorHelper.pushScissor(context, this.x + 4, this.y + 1, maxTextWidth, this.height - 2);
                try {
                    int textColor = this.enabled ? ActivityColors.TEXT_PRIMARY : ActivityColors.TEXT_DISABLED;
                    activity.client.gui.font.UiTextRenderer.drawTextWithShadow(context, tr, this.cachedWrappedLabel, this.x + 6, textY, textColor);
                } finally {
                    ScissorHelper.popScissor(context);
                }
            }
        }

        int arrowColor = (this.hovered || this.expanded) ? ActivityColors.TEXT_PRIMARY : ActivityColors.TEXT_SECONDARY;
        ActivityIconRenderer.drawCentered(context, this.expanded ? ActivityIcon.CHEVRON_UP : ActivityIcon.CHEVRON_DOWN,
            this.x + this.width - 16, this.y, 14, this.height, arrowColor);
    }

    public void setOnRightClick(@Nullable Consumer<T> onRightClick) {
        this.onRightClick = onRightClick;
    }

    @Nullable
    public Consumer<T> getOnRightClick() {
        return this.onRightClick;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (!this.enabled || !this.visible || !this.isMouseOver(click.x(), click.y())) {
            return false;
        }
        if (click.button() == 0) {
            this.toggle();
            return true;
        } else if (click.button() == 1 && this.onRightClick != null && this.selectedOption != null) {
            this.onRightClick.accept(this.selectedOption);
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

    public Function<T, Text> getNameProvider() {
        return nameProvider;
    }

    public OverlayManager getOverlayManager() {
        return overlayManager;
    }
}

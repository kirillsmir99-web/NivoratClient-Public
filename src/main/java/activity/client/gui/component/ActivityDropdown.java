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

/**
 * Modern dark dropdown selector component adhering to the Activity design system.
 *
 * <p>Features:
 * <ul>
 *   <li>1px crisp border with input styling (or accent glow on focus/open).</li>
 *   <li>Right-aligned indicator chevron icon (procedural pixel-art).</li>
 *   <li>Opens an isolated floating {@link DropdownPopup} in the {@link OverlayManager} (Z=7 layer).</li>
 *   <li>Guarantees strict click-through prevention.</li>
 *   <li>Escape key closes popup without closing the parent screen.</li>
 * </ul>
 *
 * @param <T> the type of options in this dropdown
 */
public class ActivityDropdown<T> extends ActivityComponent {

    private final OverlayManager overlayManager;
    private List<T> options;
    private T selectedOption;
    private Function<T, Text> nameProvider;
    private Consumer<T> onSelect;

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

    public Consumer<T> getOnSelect() {
        return onSelect;
    }

    public void setOnSelect(Consumer<T> onSelect) {
        this.onSelect = onSelect;
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

        // Draw cavity well background and 1px border
        ActivityGuiRenderer.drawPanel(context, this.x, this.y, this.width, this.height, ActivityColors.FIELD_BACKGROUND, borderColor);

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int fontH = activity.client.gui.font.UiTextRenderer.getFontHeight(tr);
        int textY = this.y + (this.height - fontH) / 2;

        // Draw current selected text (clipped to width minus arrow space)
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

        // Draw dropdown chevron icon
        int arrowColor = (this.hovered || this.expanded) ? ActivityColors.TEXT_PRIMARY : ActivityColors.TEXT_SECONDARY;
        ActivityIconRenderer.drawCentered(context, this.expanded ? ActivityIcon.CHEVRON_UP : ActivityIcon.CHEVRON_DOWN,
            this.x + this.width - 16, this.y, 14, this.height, arrowColor);
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

    public Function<T, Text> getNameProvider() {
        return nameProvider;
    }

    public OverlayManager getOverlayManager() {
        return overlayManager;
    }
}

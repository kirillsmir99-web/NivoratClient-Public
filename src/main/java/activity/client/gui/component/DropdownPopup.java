package activity.client.gui.component;

import activity.client.gui.ActivityScreen;
import activity.client.gui.animation.AnimationClock;
import activity.client.gui.layout.WindowLayout;
import activity.client.gui.overlay.Overlay;
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

import java.util.List;

public class DropdownPopup<T> implements Overlay {

    private final ActivityDropdown<T> dropdown;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int itemHeight = 18;
    private final int maxVisible = 6;

    private int scrollOffset = 0;
    private boolean closed = false;
    private float expandProgress = 0.0f;
    private Text[] cachedWrappedItems = null;

    public void onFontChanged() {
        this.cachedWrappedItems = null;
    }

    private int getEffectiveVisibleCount() {
        return Math.max(1, (this.height - 2) / this.itemHeight);
    }

    public DropdownPopup(ActivityDropdown<T> dropdown) {
        this.dropdown = dropdown;
        MinecraftClient mc = MinecraftClient.getInstance();
        int screenWidth = mc != null && mc.getWindow() != null ? mc.getWindow().getScaledWidth() : 400;
        int screenHeight = mc != null && mc.getWindow() != null ? mc.getWindow().getScaledHeight() : 300;
        this.width = Math.min(dropdown.getWidth(), Math.max(40, screenWidth - 8));
        this.x = Math.clamp(dropdown.getX(), 4, Math.max(4, screenWidth - this.width - 4));

        int topCeiling = ActivityMetrics.SCREEN_MARGIN_SMALL;
        if (mc != null && mc.currentScreen instanceof ActivityScreen activityScreen) {
            WindowLayout layout = activityScreen.computeLayout();
            if (layout != null) {
                topCeiling = Math.max(topCeiling, layout.headerY + layout.headerHeight);
            }
        }

        List<T> options = dropdown.getOptions();
        int maxAllowed = Math.max(1, Math.min(this.maxVisible, (screenHeight - 20) / this.itemHeight));
        int visibleCount = Math.min(options.size(), maxAllowed);
        int initialHeight = visibleCount * this.itemHeight + 2;

        int potentialBottomY = dropdown.getY() + dropdown.getHeight() + 1;
        int spaceBelow = (screenHeight - ActivityMetrics.SCREEN_MARGIN_SMALL) - potentialBottomY;
        int spaceAbove = dropdown.getY() - 1 - topCeiling;

        boolean overflowsBottom = potentialBottomY + initialHeight > screenHeight - ActivityMetrics.SCREEN_MARGIN_SMALL;
        boolean canFitAbove = dropdown.getY() - initialHeight - 1 >= topCeiling;

        if (overflowsBottom && (canFitAbove || spaceAbove > spaceBelow)) {
            if (initialHeight > spaceAbove && spaceAbove >= this.itemHeight + 2) {
                int allowedAbove = Math.max(1, (spaceAbove - 2) / this.itemHeight);
                visibleCount = Math.min(visibleCount, allowedAbove);
            }
            this.height = visibleCount * this.itemHeight + 2;
            this.y = Math.max(topCeiling, dropdown.getY() - this.height - 1);
        } else {
            this.height = initialHeight;
            this.y = Math.min(potentialBottomY, Math.max(topCeiling, screenHeight - this.height - ActivityMetrics.SCREEN_MARGIN_SMALL));
        }
    }

    public int getRenderY() {
        boolean isFlipped = this.y < this.dropdown.getY();
        if (isFlipped) {
            int curH = getRenderHeight();
            return Math.max(this.y, this.dropdown.getY() - curH - 1);
        }
        return this.y;
    }

    public int getRenderHeight() {
        float eased = AnimationClock.easeOutCubic(this.expandProgress);
        return Math.max(4, Math.round(eased * this.height));
    }

    @Override
    public boolean contains(double mouseX, double mouseY) {
        int ry = getRenderY();
        int rh = getRenderHeight();
        return mouseX >= this.x && mouseX < this.x + this.width &&
               mouseY >= ry && mouseY < ry + rh;
    }

    @Override
    public boolean shouldCloseOnClickOutside() {
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void close() {
        if (!this.closed) {
            activity.client.gui.sound.SoundManager.playDropdownClose();
        }
        this.closed = true;
        this.dropdown.setExpanded(false);
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.closed) return;

        List<T> options = this.dropdown.getOptions();
        if (options.isEmpty()) return;

        this.expandProgress = AnimationClock.approach(this.expandProgress, 1.0f, AnimationClock.DURATION_EXPAND);
        int ry = getRenderY();
        int rh = getRenderHeight();

        ScissorHelper.pushScissor(context, this.x, ry, this.width, rh);
        try {

            int popupBg = 0xF80E1015;
            ActivityGuiRenderer.fill(context, this.x, ry, this.width, rh, popupBg);

            int effVisible = getEffectiveVisibleCount();
            int visibleCount = Math.min(options.size(), effVisible);
            TextRenderer tr = MinecraftClient.getInstance().textRenderer;

            for (int i = 0; i < visibleCount; i++) {
                int itemIndex = this.scrollOffset + i;
                if (itemIndex >= options.size()) break;

                T item = options.get(itemIndex);
                int rowY = ry + 1 + i * this.itemHeight;
                boolean isHovered = mouseX >= this.x && mouseX < this.x + this.width &&
                                    mouseY >= rowY && mouseY < rowY + this.itemHeight;
                boolean isSelected = item.equals(this.dropdown.getSelectedOption());

                if (isSelected) {
                    ActivityGuiRenderer.fill(context, this.x + 1, rowY, this.width - 2, this.itemHeight, ActivityColors.ITEM_SELECTED_BG);
                    ActivityGuiRenderer.fill(context, this.x + 1, rowY, ActivityMetrics.INDICATOR_WIDTH, this.itemHeight, ActivityColors.ITEM_SELECTED_BAR);
                } else if (isHovered) {
                    ActivityGuiRenderer.fill(context, this.x + 1, rowY, this.width - 2, this.itemHeight, ActivityColors.ITEM_HOVER_BG);
                }

                if (i > 0) {
                    ActivityGuiRenderer.drawHorizontalLine(context, this.x + 2, rowY, this.width - 4, ActivityColors.BORDER_DIVIDER);
                }

                if (this.cachedWrappedItems == null || this.cachedWrappedItems.length != options.size()) {
                    this.cachedWrappedItems = new Text[options.size()];
                }
                Text wrappedText = this.cachedWrappedItems[itemIndex];
                if (wrappedText == null) {
                    Text rawText = this.dropdown.getNameProvider().apply(item);
                    wrappedText = activity.client.gui.font.FontManager.wrap(rawText);
                    this.cachedWrappedItems[itemIndex] = wrappedText;
                }

                int fontH = activity.client.gui.font.UiTextRenderer.getFontHeight(tr);
                int textY = rowY + (this.itemHeight - fontH) / 2;
                int textColor = isSelected ? ActivityColors.TEXT_ACCENT : (isHovered ? ActivityColors.TEXT_PRIMARY : ActivityColors.TEXT_SECONDARY);

                ScissorHelper.pushScissor(context, this.x + 6, rowY, Math.max(0, this.width - 12), this.itemHeight);
                try {
                    activity.client.gui.font.UiTextRenderer.drawTextWithShadow(context, tr, wrappedText, this.x + 8, textY, textColor);
                } finally {
                    ScissorHelper.popScissor(context);
                }
            }

            if (options.size() > effVisible) {
                int scrollbarX = this.x + this.width - 3;
                int trackH = rh - 4;
                if (trackH > 8) {
                    ActivityGuiRenderer.fill(context, scrollbarX, ry + 2, 2, trackH, ActivityColors.SCROLLBAR_TRACK);
                    int thumbH = Math.max(8, trackH * effVisible / options.size());
                    int maxScroll = options.size() - effVisible;
                    int thumbY = ry + 2 + (trackH - thumbH) * this.scrollOffset / maxScroll;
                    ActivityGuiRenderer.fill(context, scrollbarX, thumbY, 2, thumbH, ActivityColors.BORDER_HOVER);
                }
            }
        } finally {
            ScissorHelper.popScissor(context);
        }

        ActivityGuiRenderer.drawBorder(context, this.x, ry, this.width, rh, ActivityColors.BORDER_LIGHT);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0 || !contains(click.x(), click.y())) {
            return false;
        }

        int ry = getRenderY();
        List<T> options = this.dropdown.getOptions();
        int relativeY = (int) click.y() - (ry + 1);
        int row = relativeY / this.itemHeight;
        int targetIndex = this.scrollOffset + row;

        if (targetIndex >= 0 && targetIndex < options.size()) {
            T selected = options.get(targetIndex);
            this.dropdown.setSelectedOption(selected);
            activity.client.gui.sound.SoundManager.playSelect();
            this.close();
            this.dropdown.getOverlayManager().close(this);
            return true;
        }

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        List<T> options = this.dropdown.getOptions();
        int effVisible = getEffectiveVisibleCount();
        if (options.size() > effVisible) {
            int maxScroll = options.size() - effVisible;
            this.scrollOffset = Math.clamp(this.scrollOffset - (int) Math.signum(verticalAmount), 0, Math.max(0, maxScroll));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.isEscape()) {
            this.close();
            this.dropdown.getOverlayManager().close(this);
            return true;
        }
        return false;
    }
}

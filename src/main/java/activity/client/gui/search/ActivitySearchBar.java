package activity.client.gui.search;

import activity.client.gui.animation.AnimationClock;
import activity.client.gui.component.ActivityComponent;
import activity.client.gui.font.FontManager;
import activity.client.gui.icon.ActivityIcon;
import activity.client.gui.icon.ActivityIconRenderer;
import activity.client.gui.render.ActivityGuiRenderer;
import activity.client.gui.render.ScissorHelper;
import activity.client.gui.theme.ActivityColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Modern interactive search bar component with autocomplete dropdown and keyboard navigation.
 */
public class ActivitySearchBar extends ActivityComponent {

    private static final Text PLACEHOLDER = Text.translatable("activity.gui.search_placeholder");

    private String text = "";
    private int cursor = 0;
    private float focusAnimation = 0.0f;
    private final Consumer<SearchController.SearchResult> onSelect;
    private Consumer<String> onQueryChange;

    private List<SearchController.SearchResult> searchResults = new ArrayList<>();
    private int selectedResultIndex = -1;
    private boolean popupOpen = false;

    public ActivitySearchBar(int x, int y, int width, int height, Consumer<SearchController.SearchResult> onSelect) {
        super(x, y, width, height);
        this.onSelect = onSelect;
    }

    public void setOnQueryChange(Consumer<String> onQueryChange) {
        this.onQueryChange = onQueryChange;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text != null ? text : "";
        this.cursor = Math.clamp(this.cursor, 0, this.text.length());
        updateSearch();
    }

    public void clear() {
        this.text = "";
        this.cursor = 0;
        this.searchResults.clear();
        this.selectedResultIndex = -1;
        this.popupOpen = false;
        if (this.onQueryChange != null) {
            this.onQueryChange.accept("");
        }
    }

    private void updateSearch() {
        if (this.text.isBlank()) {
            this.searchResults.clear();
            this.selectedResultIndex = -1;
            this.popupOpen = false;
        } else {
            this.searchResults = SearchController.search(this.text, 6);
            this.selectedResultIndex = this.searchResults.isEmpty() ? -1 : 0;
            this.popupOpen = !this.searchResults.isEmpty();
        }
        if (this.onQueryChange != null) {
            this.onQueryChange.accept(this.text);
        }
    }

    @Override
    public void setFocused(boolean focused) {
        if (focused && !this.focused) {
            activity.client.gui.sound.SoundManager.playSearchFocus();
        }
        super.setFocused(focused);
        if (!focused) {
            this.popupOpen = false;
        }
    }

    @Override
    protected void renderComponent(DrawContext context, int mouseX, int mouseY, float delta) {
        float dt = AnimationClock.getDeltaTime();
        float targetFocus = this.focused ? 1.0f : 0.0f;
        this.focusAnimation = AnimationClock.approach(this.focusAnimation, targetFocus, 0.12f);

        int bg = ActivityColors.scaleAlphaPercent(ActivityColors.FIELD_BACKGROUND, 80.0 * this.alpha);
        int baseBorder = ActivityColors.interpolateColor(ActivityColors.BORDER, ActivityColors.ACCENT_PRIMARY, this.focusAnimation);
        int border = ActivityColors.scaleAlpha(baseBorder, this.alpha);

        // Background and border
        ActivityGuiRenderer.drawPanel(context, this.x, this.y, this.width, this.height, bg, border, false);

        // Search icon on the left (12 logical px, centered vertically, left padding 7px)
        int iconSize = 12;
        int iconLeftPad = 7;
        int iconX = this.x + iconLeftPad;
        int iconY = this.y + (this.height - iconSize) / 2;
        int iconColor = ActivityColors.scaleAlpha(this.focused ? ActivityColors.ACCENT_LIGHT : ActivityColors.TEXT_MUTED, this.alpha);
        ActivityIconRenderer.drawSized(context, ActivityIcon.SEARCH, iconX, iconY, iconSize, iconColor);

        // Input text clipping area
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int textX = iconX + iconSize + 6;
        int clearBtnSize = 10;
        int clearBtnRightPad = 6;
        int clearX = this.x + this.width - clearBtnSize - clearBtnRightPad;
        int textW = Math.max(1, (this.text.isEmpty() ? this.x + this.width - 6 : clearX - 4) - textX);
        int textY = this.y + (this.height - textRenderer.fontHeight) / 2;

        ScissorHelper.pushScissor(context, textX, this.y, textW, this.height);
        try {
            if (this.text.isEmpty()) {
                int phColor = ActivityColors.scaleAlpha(ActivityColors.TEXT_MUTED, this.alpha);
                ActivityGuiRenderer.drawText(context, textRenderer, PLACEHOLDER, textX, textY, phColor);
            } else {
                int txtColor = ActivityColors.scaleAlpha(ActivityColors.TEXT_PRIMARY, this.alpha);
                ActivityGuiRenderer.drawText(context, textRenderer, FontManager.wrap(Text.literal(this.text)), textX, textY, txtColor);

                // Cursor rendering
                if (this.focused && AnimationClock.isCursorBlinkVisible()) {
                    int cursorX = textX + textRenderer.getWidth(this.text.substring(0, this.cursor));
                    int curColor = ActivityColors.scaleAlpha(ActivityColors.TEXT_PRIMARY, this.alpha);
                    ActivityGuiRenderer.fill(context, cursorX, textY - 1, 1, textRenderer.fontHeight + 2, curColor);
                }
            }
        } finally {
            ScissorHelper.popScissor(context);
        }

        // Clear icon on the right (if text is not empty)
        if (!this.text.isEmpty()) {
            int clearY = this.y + (this.height - clearBtnSize) / 2;
            boolean clearHovered = (mouseX >= clearX - 2 && mouseX < clearX + clearBtnSize + 2 && mouseY >= this.y && mouseY < this.y + this.height);
            int clearColor = ActivityColors.scaleAlpha(clearHovered ? ActivityColors.TEXT_PRIMARY : ActivityColors.TEXT_MUTED, this.alpha);
            ActivityIconRenderer.draw(context, ActivityIcon.CLOSE, clearX, clearY, clearColor);
        }
    }

    private int getPopupWidth() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int screenW = mc != null && mc.getWindow() != null ? mc.getWindow().getScaledWidth() : 400;
        int maxW = Math.max(90, Math.min(260, screenW - 16));
        int minW = Math.min(140, maxW);
        return Math.clamp(this.width + 50, minW, maxW);
    }

    private int getPopupX() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int screenW = mc != null && mc.getWindow() != null ? mc.getWindow().getScaledWidth() : 400;
        int popupW = getPopupWidth();
        int maxX = Math.max(0, screenW - popupW - 4);
        int minX = Math.min(4, maxX);
        return Math.clamp(this.x + this.width - popupW, minX, maxX);
    }

    private int getPopupHeight() {
        int count = Math.min(this.searchResults.size(), getMaxVisibleResults());
        return Math.max(26, count * 22 + 4);
    }

    private boolean shouldFlipPopupUp() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int screenH = mc != null && mc.getWindow() != null ? mc.getWindow().getScaledHeight() : 300;
        int count = Math.min(this.searchResults.size(), 6);
        int estH = Math.max(26, count * 22 + 4);
        int potentialBottomY = this.y + this.height + 3 + estH;
        return potentialBottomY > screenH - 6 && this.y - estH - 3 >= 6;
    }

    private int getPopupY() {
        int popupH = getPopupHeight();
        if (shouldFlipPopupUp()) {
            return this.y - popupH - 3;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        int screenH = mc != null && mc.getWindow() != null ? mc.getWindow().getScaledHeight() : 300;
        int targetY = this.y + this.height + 3;
        return Math.min(targetY, Math.max(4, screenH - popupH - 4));
    }

    private int getMaxVisibleResults() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int screenH = mc != null && mc.getWindow() != null ? mc.getWindow().getScaledHeight() : 300;
        int rowHeight = 22;
        int spaceBelow = Math.max(0, screenH - (this.y + this.height + 3) - 8);
        int spaceAbove = Math.max(0, this.y - 8);
        int availableH = shouldFlipPopupUp() ? spaceAbove : spaceBelow;
        return Math.max(1, Math.min(6, availableH / rowHeight));
    }

    /**
     * Renders the floating search results popup overlay.
     */
    public void renderPopup(DrawContext context, int mouseX, int mouseY) {
        if (!this.popupOpen || this.searchResults.isEmpty() || !this.focused) return;

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int rowHeight = 22;
        int popupW = getPopupWidth();
        int popupH = getPopupHeight();
        int popupX = getPopupX();
        int popupY = getPopupY();

        // Dark glass popup background
        int popupBg = ActivityColors.scaleAlphaPercent(ActivityColors.PANEL_BACKGROUND, 95.0);
        ActivityGuiRenderer.drawWindowFrame(context, popupX, popupY, popupW, popupH, popupBg, ActivityColors.BORDER, true);

        int maxVis = getMaxVisibleResults();
        int curY = popupY + 2;
        for (int i = 0; i < this.searchResults.size() && i < maxVis; i++) {
            SearchController.SearchResult res = this.searchResults.get(i);
            boolean isHovered = (mouseX >= popupX && mouseX < popupX + popupW && mouseY >= curY && mouseY < curY + rowHeight);
            boolean isSelected = (i == this.selectedResultIndex);

            if (isSelected || isHovered) {
                int rowBg = isSelected ? ActivityColors.ITEM_SELECTED_BG : ActivityColors.ITEM_HOVER_BG;
                ActivityGuiRenderer.fill(context, popupX + 2, curY, popupW - 4, rowHeight, rowBg);
                if (isSelected) {
                    ActivityGuiRenderer.fill(context, popupX + 2, curY, 2, rowHeight, ActivityColors.ACCENT_PRIMARY);
                }
            }

            // Icon
            int itemIconY = curY + (rowHeight - 10) / 2;
            int itemIconColor = isSelected ? ActivityColors.ACCENT_LIGHT : ActivityColors.TEXT_SECONDARY;
            ActivityIconRenderer.draw(context, res.entry().icon(), popupX + 6, itemIconY, itemIconColor);

            // Title & breadcrumb
            int titleY = curY + 3;
            int breadcrumbY = curY + 12;
            int textLeft = popupX + 20;
            int maxTextW = popupW - 24;

            Text titleText = FontManager.wrap(res.entry().title());
            Text displayTitle = textRenderer.getWidth(titleText) > maxTextW
                ? Text.literal(textRenderer.trimToWidth(titleText.getString(), Math.max(8, maxTextW - 6)) + "…")
                : titleText;
            ActivityGuiRenderer.drawText(context, textRenderer, displayTitle, textLeft, titleY, ActivityColors.TEXT_PRIMARY);

            Text bcText = FontManager.wrap(res.entry().breadcrumb());
            Text displayBc = textRenderer.getWidth(bcText) > maxTextW
                ? Text.literal(textRenderer.trimToWidth(bcText.getString(), Math.max(8, maxTextW - 6)) + "…")
                : bcText;
            ActivityGuiRenderer.drawText(context, textRenderer, displayBc, textLeft, breadcrumbY, ActivityColors.TEXT_MUTED);

            curY += rowHeight;
        }
    }

    public boolean isMouseOverPopup(double mouseX, double mouseY) {
        if (!this.popupOpen || this.searchResults.isEmpty() || !this.focused) return false;
        int popupW = getPopupWidth();
        int popupH = getPopupHeight();
        int popupX = getPopupX();
        int popupY = getPopupY();
        return mouseX >= popupX && mouseX < popupX + popupW && mouseY >= popupY && mouseY < popupY + popupH;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (!this.visible || !this.enabled) return false;

        double mx = click.x();
        double my = click.y();

        // Check clear button click
        if (!this.text.isEmpty()) {
            int clearX = this.x + this.width - 18;
            if (mx >= clearX && mx <= this.x + this.width && my >= this.y && my < this.y + this.height) {
                clear();
                return true;
            }
        }

        // Check popup click
        if (isMouseOverPopup(mx, my)) {
            int rowHeight = 22;
            int popupY = getPopupY();
            int clickedIndex = (int) ((my - popupY - 2) / rowHeight);
            if (clickedIndex >= 0 && clickedIndex < this.searchResults.size() && clickedIndex < getMaxVisibleResults()) {
                activateResult(this.searchResults.get(clickedIndex));
            }
            return true;
        }

        // Check search bar click
        if (isMouseOver(mx, my)) {
            setFocused(true);
            return true;
        }

        if (this.focused) {
            setFocused(false);
            this.popupOpen = false;
        }
        return false;
    }

    private void activateResult(SearchController.SearchResult result) {
        activity.client.gui.sound.SoundManager.playSelect();
        if (result != null && this.onSelect != null) {
            this.onSelect.accept(result);
        }
        this.popupOpen = false;
        setFocused(false);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (!this.focused) return false;

        int keyCode = input.key();

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            setFocused(false);
            this.popupOpen = false;
            return true;
        }

        if (this.popupOpen && !this.searchResults.isEmpty()) {
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                this.selectedResultIndex = (this.selectedResultIndex + 1) % this.searchResults.size();
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_UP) {
                this.selectedResultIndex = (this.selectedResultIndex - 1 + this.searchResults.size()) % this.searchResults.size();
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (this.selectedResultIndex >= 0 && this.selectedResultIndex < this.searchResults.size()) {
                    activateResult(this.searchResults.get(this.selectedResultIndex));
                    return true;
                }
            }
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (this.cursor > 0 && !this.text.isEmpty()) {
                this.text = this.text.substring(0, this.cursor - 1) + this.text.substring(this.cursor);
                this.cursor--;
                updateSearch();
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (this.cursor < this.text.length()) {
                this.text = this.text.substring(0, this.cursor) + this.text.substring(this.cursor + 1);
                updateSearch();
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            if (this.cursor > 0) this.cursor--;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            if (this.cursor < this.text.length()) this.cursor++;
            return true;
        }

        if (input.isSelectAll()) {
            this.text = "";
            this.cursor = 0;
            updateSearch();
            return true;
        } else if (input.isPaste()) {
            String clipboard = MinecraftClient.getInstance().keyboard.getClipboard();
            if (clipboard != null && !clipboard.isBlank()) {
                this.text = this.text.substring(0, this.cursor) + clipboard + this.text.substring(this.cursor);
                this.cursor += clipboard.length();
                updateSearch();
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (!this.focused) return false;

        if (input.isValidChar()) {
            String typed = input.asString();
            this.text = this.text.substring(0, this.cursor) + typed + this.text.substring(this.cursor);
            this.cursor += typed.length();
            updateSearch();
            return true;
        }
        return false;
    }



    public void closePopup() {
        this.popupOpen = false;
    }

    public boolean isPopupOpen() {
        return this.popupOpen;
    }
}

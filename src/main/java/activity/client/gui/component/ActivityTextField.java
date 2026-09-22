package activity.client.gui.component;

import activity.client.gui.animation.AnimationClock;
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
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class ActivityTextField extends ActivityComponent {

    private String text = "";
    private int maxLength = 256;
    @Nullable
    private Text placeholder;

    private int cursorPosition = 0;
    private int selectionEnd = 0;
    private int firstVisibleIndex = 0;

    private float cursorBlinkStartTime = 0.0f;
    private Consumer<String> onChanged;

    private float hoverProgress = 0.0f;
    private float focusProgress = 0.0f;
    private Text cachedWrappedPlaceholder = null;

    public ActivityTextField(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public ActivityTextField(int x, int y, int width, int height, @Nullable Text placeholder) {
        super(x, y, width, height);
        this.placeholder = placeholder;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        if (text == null) text = "";
        if (text.length() > this.maxLength) {
            text = text.substring(0, this.maxLength);
        }
        this.text = text;
        this.setCursorPosition(this.text.length());
        if (this.onChanged != null) {
            this.onChanged.accept(this.text);
        }
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
        if (this.text.length() > maxLength) {
            this.text = this.text.substring(0, maxLength);
            this.setCursorPosition(this.text.length());
        }
    }

    @Nullable
    public Text getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(@Nullable Text placeholder) {
        this.placeholder = placeholder;
        this.cachedWrappedPlaceholder = null;
    }

    public void setOnChanged(Consumer<String> onChanged) {
        this.onChanged = onChanged;
    }

    public void setCursorPosition(int pos) {
        this.cursorPosition = Math.clamp(pos, 0, this.text.length());
        this.selectionEnd = this.cursorPosition;
        this.cursorBlinkStartTime = AnimationClock.getElapsedSeconds();
        clampVisible();
    }

    public boolean hasSelection() {
        return this.cursorPosition != this.selectionEnd;
    }

    public String getSelectedText() {
        int start = Math.min(this.cursorPosition, this.selectionEnd);
        int end = Math.max(this.cursorPosition, this.selectionEnd);
        return this.text.substring(start, end);
    }

    public void deleteSelection() {
        if (!hasSelection()) return;
        int start = Math.min(this.cursorPosition, this.selectionEnd);
        int end = Math.max(this.cursorPosition, this.selectionEnd);
        this.text = this.text.substring(0, start) + this.text.substring(end);
        this.setCursorPosition(start);
        if (this.onChanged != null) {
            this.onChanged.accept(this.text);
        }
    }

    public void insertText(String str) {
        if (str == null || str.isEmpty()) return;
        deleteSelection();

        int available = this.maxLength - this.text.length();
        if (available <= 0) return;
        if (str.length() > available) {
            str = str.substring(0, available);
        }

        this.text = this.text.substring(0, this.cursorPosition) + str + this.text.substring(this.cursorPosition);
        this.setCursorPosition(this.cursorPosition + str.length());
        if (this.onChanged != null) {
            this.onChanged.accept(this.text);
        }
    }

    private void clampVisible() {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc != null ? mc.textRenderer : null;
        if (tr == null) return;
        int innerWidth = this.width - 8;
        if (innerWidth <= 0) return;

        if (this.cursorPosition < this.firstVisibleIndex) {
            this.firstVisibleIndex = this.cursorPosition;
        }

        String visibleStr = this.text.substring(this.firstVisibleIndex);
        String rendered = activity.client.gui.font.UiTextRenderer.trimToWidth(tr, visibleStr, innerWidth);
        int endIndex = this.firstVisibleIndex + rendered.length();

        if (this.cursorPosition > endIndex) {
            this.firstVisibleIndex += this.cursorPosition - endIndex;
        }
        this.firstVisibleIndex = Math.clamp(this.firstVisibleIndex, 0, this.text.length());
    }

    @Override
    protected void renderComponent(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.width <= 2 || this.height <= 2) return;

        boolean isHovered = this.enabled && this.hovered;
        float targetHover = isHovered ? 1.0f : 0.0f;
        float targetFocus = (this.focused && this.enabled) ? 1.0f : 0.0f;

        this.hoverProgress = AnimationClock.approach(this.hoverProgress, targetHover, AnimationClock.DURATION_HOVER);
        this.focusProgress = AnimationClock.approach(this.focusProgress, targetFocus, AnimationClock.DURATION_FOCUS);

        int baseBorder = ActivityColors.interpolateColor(ActivityColors.BORDER_INPUT, ActivityColors.BORDER_HOVER, this.hoverProgress);
        int borderColor = ActivityColors.interpolateColor(baseBorder, ActivityColors.ACCENT_PRIMARY, this.focusProgress);

        ActivityGuiRenderer.drawPanel(context, this.x, this.y, this.width, this.height, ActivityColors.FIELD_BACKGROUND, borderColor);

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int fontH = activity.client.gui.font.UiTextRenderer.getFontHeight(tr);
        int innerX = this.x + 4;
        int innerY = this.y + (this.height - fontH) / 2;
        int innerWidth = this.width - 8;

        ScissorHelper.pushScissor(context, this.x + 1, this.y + 1, Math.max(0, this.width - 2), Math.max(0, this.height - 2));
        try {
            if (this.text.isEmpty() && !this.focused && this.placeholder != null) {
                if (this.cachedWrappedPlaceholder == null) {
                    this.cachedWrappedPlaceholder = activity.client.gui.font.FontManager.wrap(this.placeholder);
                }
                activity.client.gui.font.UiTextRenderer.drawTextWithShadow(context, tr, this.cachedWrappedPlaceholder, innerX, innerY, ActivityColors.TEXT_MUTED);
            } else {
                clampVisible();
                String visibleText = this.text.substring(this.firstVisibleIndex);
                String rendered = activity.client.gui.font.UiTextRenderer.trimToWidth(tr, visibleText, innerWidth);

                activity.client.gui.font.UiTextRenderer.drawTextWithShadow(context, tr, rendered, innerX, innerY, this.enabled ? ActivityColors.TEXT_PRIMARY : ActivityColors.TEXT_DISABLED);

                if (this.focused && this.enabled) {
                    boolean cursorVisible = AnimationClock.isCursorBlinkVisible(this.cursorBlinkStartTime);
                    if (cursorVisible && this.cursorPosition >= this.firstVisibleIndex) {
                        String beforeCursor = this.text.substring(this.firstVisibleIndex, this.cursorPosition);
                        int cursorX = innerX + activity.client.gui.font.UiTextRenderer.getWidth(tr, beforeCursor);
                        ActivityGuiRenderer.fill(context, cursorX, innerY - 1, 1, fontH + 2, ActivityColors.TEXT_PRIMARY);
                    }
                }

                if (hasSelection()) {
                    int selStart = Math.min(this.cursorPosition, this.selectionEnd);
                    int selEnd = Math.max(this.cursorPosition, this.selectionEnd);

                    if (selStart < this.firstVisibleIndex + rendered.length() && selEnd > this.firstVisibleIndex) {
                        int visSelStart = Math.max(selStart, this.firstVisibleIndex);
                        int visSelEnd = Math.min(selEnd, this.firstVisibleIndex + rendered.length());

                        int selX1 = innerX + activity.client.gui.font.UiTextRenderer.getWidth(tr, this.text.substring(this.firstVisibleIndex, visSelStart));
                        int selX2 = innerX + activity.client.gui.font.UiTextRenderer.getWidth(tr, this.text.substring(this.firstVisibleIndex, visSelEnd));

                        ActivityGuiRenderer.fill(context, selX1, innerY - 1, selX2 - selX1, fontH + 2, ActivityColors.ACCENT_MUTED);
                    }
                }
            }
        } finally {
            ScissorHelper.popScissor(context);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (!this.visible || !this.enabled) return false;

        boolean clickedInside = this.isMouseOver(click.x(), click.y());
        this.setFocused(clickedInside);

        if (clickedInside && click.button() == 0) {
            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            int innerX = this.x + 4;
            int clickX = (int) click.x() - innerX;

            String visibleText = this.text.substring(this.firstVisibleIndex);
            String rendered = activity.client.gui.font.UiTextRenderer.trimToWidth(tr, visibleText, clickX);
            this.setCursorPosition(this.firstVisibleIndex + rendered.length());
            return true;
        }

        return false;
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (!this.visible || !this.enabled || !this.focused) return false;

        if (input.isValidChar()) {
            this.insertText(input.asString());
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (!this.visible || !this.enabled || !this.focused) return false;

        if (input.isSelectAll()) {
            this.selectionEnd = 0;
            this.cursorPosition = this.text.length();
            return true;
        } else if (input.isCopy()) {
            if (hasSelection()) {
                MinecraftClient.getInstance().keyboard.setClipboard(getSelectedText());
            }
            return true;
        } else if (input.isPaste()) {
            String clip = MinecraftClient.getInstance().keyboard.getClipboard();
            this.insertText(clip);
            return true;
        } else if (input.isCut()) {
            if (hasSelection()) {
                MinecraftClient.getInstance().keyboard.setClipboard(getSelectedText());
                deleteSelection();
            }
            return true;
        }

        int key = input.key();

        switch (key) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (hasSelection()) {
                    deleteSelection();
                } else if (this.cursorPosition > 0) {
                    this.text = this.text.substring(0, this.cursorPosition - 1) + this.text.substring(this.cursorPosition);
                    this.setCursorPosition(this.cursorPosition - 1);
                    if (this.onChanged != null) {
                        this.onChanged.accept(this.text);
                    }
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (hasSelection()) {
                    deleteSelection();
                } else if (this.cursorPosition < this.text.length()) {
                    this.text = this.text.substring(0, this.cursorPosition) + this.text.substring(this.cursorPosition + 1);
                    if (this.onChanged != null) {
                        this.onChanged.accept(this.text);
                    }
                }
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                if (input.hasShift()) {
                    this.cursorPosition = Math.max(0, this.cursorPosition - 1);
                } else {
                    if (hasSelection()) {
                        this.setCursorPosition(Math.min(this.cursorPosition, this.selectionEnd));
                    } else {
                        this.setCursorPosition(this.cursorPosition - 1);
                    }
                }
                clampVisible();
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (input.hasShift()) {
                    this.cursorPosition = Math.min(this.text.length(), this.cursorPosition + 1);
                } else {
                    if (hasSelection()) {
                        this.setCursorPosition(Math.max(this.cursorPosition, this.selectionEnd));
                    } else {
                        this.setCursorPosition(this.cursorPosition + 1);
                    }
                }
                clampVisible();
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                if (input.hasShift()) {
                    this.cursorPosition = 0;
                } else {
                    this.setCursorPosition(0);
                }
                clampVisible();
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                if (input.hasShift()) {
                    this.cursorPosition = this.text.length();
                } else {
                    this.setCursorPosition(this.text.length());
                }
                clampVisible();
                return true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                this.setFocused(false);
                return true;
            }
        }

        return false;
    }

    @Override
    public void onFontChanged() {
        this.cachedWrappedPlaceholder = null;
        clampVisible();
    }
}

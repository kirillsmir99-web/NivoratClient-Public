package activity.client.gui.component;

import activity.client.gui.animation.AnimationClock;
import activity.client.gui.render.ActivityGuiRenderer;
import activity.client.gui.render.ScissorHelper;
import activity.client.gui.theme.ActivityColors;
import activity.client.module.keybind.Keybind;
import activity.client.module.keybind.KeybindSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Reusable keybind configuration component for the Activity design system.
 * Supports listening mode, single keys, mouse buttons, and modifier combinations (Ctrl, Shift, Alt).
 */
public class ActivityKeybindButton extends ActivityComponent {

    private final Keybind keybind;
    @Nullable
    private final KeybindSetting setting;
    @Nullable
    private Consumer<Keybind> onChanged;

    private boolean listening = false;
    private float hoverProgress = 0.0f;

    private Text cachedWrappedLabel = null;
    private boolean lastListening = false;
    private int lastKeyCode = Integer.MIN_VALUE;
    private boolean lastCtrl = false;
    private boolean lastShift = false;
    private boolean lastAlt = false;

    @Override
    public void onFontChanged() {
        this.cachedWrappedLabel = null;
    }

    public ActivityKeybindButton(int x, int y, int width, int height, Keybind keybind, @Nullable Consumer<Keybind> onChanged) {
        super(x, y, width, height);
        this.keybind = Objects.requireNonNull(keybind, "keybind");
        this.setting = null;
        this.onChanged = onChanged;
    }

    public ActivityKeybindButton(int x, int y, int width, int height, KeybindSetting setting) {
        super(x, y, width, height);
        this.setting = Objects.requireNonNull(setting, "setting");
        this.keybind = setting.getValue();
        this.onChanged = setting::setValue;
    }

    public ActivityKeybindButton(int x, int y, int width, int height, Keybind keybind) {
        this(x, y, width, height, keybind, null);
    }

    public Keybind getKeybind() {
        return this.keybind;
    }

    @Nullable
    public KeybindSetting getSetting() {
        return this.setting;
    }

    public boolean isListening() {
        return this.listening;
    }

    public void setListening(boolean listening) {
        this.listening = listening;
    }

    public void setOnChanged(@Nullable Consumer<Keybind> onChanged) {
        this.onChanged = onChanged;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused && this.listening) {
            this.listening = false;
        }
    }

    public static boolean isModifierKey(int code) {
        return code == GLFW.GLFW_KEY_LEFT_CONTROL || code == GLFW.GLFW_KEY_RIGHT_CONTROL
            || code == GLFW.GLFW_KEY_LEFT_SHIFT || code == GLFW.GLFW_KEY_RIGHT_SHIFT
            || code == GLFW.GLFW_KEY_LEFT_ALT || code == GLFW.GLFW_KEY_RIGHT_ALT
            || code == GLFW.GLFW_KEY_LEFT_SUPER || code == GLFW.GLFW_KEY_RIGHT_SUPER;
    }

    private static boolean hasCtrlModifier() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return false;
        long handle = client.getWindow().getHandle();
        if (handle == 0L) return false;
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    private static boolean hasShiftModifier() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return false;
        long handle = client.getWindow().getHandle();
        if (handle == 0L) return false;
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    private static boolean hasAltModifier() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return false;
        long handle = client.getWindow().getHandle();
        if (handle == 0L) return false;
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
    }

    private void notifyChanged() {
        if (this.setting != null) {
            this.setting.setValue(this.keybind);
        }
        if (this.onChanged != null) {
            this.onChanged.accept(this.keybind);
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

        if (this.listening) {
            float pulse = AnimationClock.getPulse(8.0f);
            bgColor = ActivityColors.BUTTON_PRIMARY_BG;
            borderColor = ActivityColors.interpolateColor(ActivityColors.ACCENT_PRIMARY, ActivityColors.ACCENT_LIGHT, pulse);
            textColor = ActivityColors.interpolateColor(0xFFFFD043, 0xFFFFA020, pulse);
        } else {
            int targetBg = ActivityColors.withAlpha(ActivityColors.BUTTON_SECONDARY_BG, 0xF0);
            bgColor = ActivityColors.interpolateColor(ActivityColors.BUTTON_SECONDARY_BG, targetBg, this.hoverProgress);

            if (this.focused && this.enabled) {
                borderColor = ActivityColors.ACCENT_PRIMARY;
            } else {
                borderColor = ActivityColors.interpolateColor(ActivityColors.BORDER, ActivityColors.BORDER_HOVER, this.hoverProgress);
            }

            if (!this.enabled) {
                textColor = ActivityColors.TEXT_DISABLED;
            } else if (this.keybind.isUnbound()) {
                textColor = ActivityColors.TEXT_MUTED;
            } else {
                textColor = ActivityColors.interpolateColor(ActivityColors.TEXT_SECONDARY, ActivityColors.TEXT_PRIMARY, this.hoverProgress);
            }
        }

        // Base background and border
        ActivityGuiRenderer.drawPanel(context, this.x, this.y, this.width, this.height, bgColor, borderColor);

        // Hover overlay
        if (!this.listening && this.hoverProgress > 0.001f) {
            int maxAlpha = (ActivityColors.BUTTON_SECONDARY_HOVER >>> 24) & 0xFF;
            int overlayAlpha = (int) (maxAlpha * this.hoverProgress);
            int overlayColor = (overlayAlpha << 24) | (ActivityColors.BUTTON_SECONDARY_HOVER & 0x00FFFFFF);
            ActivityGuiRenderer.fill(context, this.x + 1, this.y + 1, this.width - 2, this.height - 2, overlayColor);
        }

        // Focused outline (if not listening)
        if (!this.listening && this.focused && this.enabled) {
            ActivityGuiRenderer.drawBorder(context, this.x, this.y, this.width, this.height, ActivityColors.ACCENT_PRIMARY);
        }

        // Label formatting with cached text
        if (this.cachedWrappedLabel == null ||
            this.listening != this.lastListening ||
            this.keybind.getKeyCode() != this.lastKeyCode ||
            this.keybind.isCtrl() != this.lastCtrl ||
            this.keybind.isShift() != this.lastShift ||
            this.keybind.isAlt() != this.lastAlt) {

            this.lastListening = this.listening;
            this.lastKeyCode = this.keybind.getKeyCode();
            this.lastCtrl = this.keybind.isCtrl();
            this.lastShift = this.keybind.isShift();
            this.lastAlt = this.keybind.isAlt();

            Text labelText;
            if (this.listening) {
                labelText = Text.translatable("activity.keybind.listening");
            } else if (this.keybind.isUnbound()) {
                labelText = Text.literal("[ ").append(Text.translatable("activity.keybind.none")).append(" ]");
            } else {
                labelText = Text.literal("[ " + this.keybind.format() + " ]");
            }
            this.cachedWrappedLabel = activity.client.gui.font.FontManager.wrap(labelText);
        }

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int textY = this.y + (this.height - tr.fontHeight) / 2;

        ScissorHelper.pushScissor(context, this.x + 2, this.y + 1, this.width - 4, this.height - 2);
        try {
            context.drawCenteredTextWithShadow(tr, this.cachedWrappedLabel, this.x + this.width / 2, textY, textColor);
        } finally {
            ScissorHelper.popScissor(context);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (!this.enabled || !this.visible) return false;

        boolean mouseOver = this.isMouseOver(click.x(), click.y());
        int button = click.button();

        if (this.listening) {
            if (button == 0) {
                if (mouseOver) {
                    // Clicking LMB directly on the listening button cancels listening mode
                    setListening(false);
                    ActivityGuiRenderer.playClickSound();
                    return true;
                } else {
                    // Clicking LMB outside cancels listening and allows click to route to the target
                    setListening(false);
                    return false;
                }
            } else {
                // Mouse buttons > 0 (RMB=1, Middle=2, Mouse4=3, Mouse5=4, etc.) are bound as hotkeys
                boolean ctrl = hasCtrlModifier();
                boolean shift = hasShiftModifier();
                boolean alt = hasAltModifier();
                this.keybind.set(Keybind.MOUSE_OFFSET - button, ctrl, shift, alt);
                setListening(false);
                notifyChanged();
                ActivityGuiRenderer.playClickSound();
                return true;
            }
        } else {
            if (mouseOver && button == 0) {
                setListening(true);
                setFocused(true);
                ActivityGuiRenderer.playClickSound();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (!this.enabled || !this.visible) return false;

        if (this.listening) {
            int code = input.key();
            int mods = input.modifiers();

            // 1. Escape cancels listening mode without modifying the bind
            if (input.isEscape() || code == GLFW.GLFW_KEY_ESCAPE) {
                setListening(false);
                ActivityGuiRenderer.playClickSound();
                return true;
            }

            // 2. Backspace or Delete clears the keybind (sets to Unbound)
            if (code == GLFW.GLFW_KEY_BACKSPACE || code == GLFW.GLFW_KEY_DELETE) {
                this.keybind.clear();
                setListening(false);
                notifyChanged();
                ActivityGuiRenderer.playClickSound();
                return true;
            }

            // 3. Modifier keys pressed alone (Ctrl, Shift, Alt, Super) are ignored
            if (isModifierKey(code)) {
                return true;
            }

            // 4. Regular key with optional modifiers
            boolean ctrl = input.hasCtrl() || (mods & GLFW.GLFW_MOD_CONTROL) != 0;
            boolean shift = input.hasShift() || (mods & GLFW.GLFW_MOD_SHIFT) != 0;
            boolean alt = input.hasAlt() || (mods & GLFW.GLFW_MOD_ALT) != 0;

            this.keybind.set(code, ctrl, shift, alt);
            setListening(false);
            notifyChanged();
            ActivityGuiRenderer.playClickSound();
            return true;
        } else {
            // When focused and not listening: Enter or Space activates listening mode
            if (this.focused && input.isEnterOrSpace()) {
                setListening(true);
                ActivityGuiRenderer.playClickSound();
                return true;
            }
        }

        return false;
    }
}

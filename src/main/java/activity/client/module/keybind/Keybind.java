package activity.client.module.keybind;

import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

/**
 * Encapsulates a configurable keybind combination supporting keyboard keys,
 * mouse buttons, and modifier keys (Ctrl, Shift, Alt).
 */
public final class Keybind {

    public static final int UNBOUND = -1;
    public static final int MOUSE_OFFSET = -100;

    private int keyCode;
    private boolean ctrl;
    private boolean shift;
    private boolean alt;

    public Keybind() {
        this(UNBOUND, false, false, false);
    }

    public Keybind(int keyCode) {
        this(keyCode, false, false, false);
    }

    public Keybind(int keyCode, boolean ctrl, boolean shift, boolean alt) {
        this.keyCode = keyCode;
        this.ctrl = ctrl;
        this.shift = shift;
        this.alt = alt;
    }

    public int getKeyCode() {
        return this.keyCode;
    }

    public boolean isCtrl() {
        return this.ctrl;
    }

    public boolean isShift() {
        return this.shift;
    }

    public boolean isAlt() {
        return this.alt;
    }

    public void set(int keyCode, boolean ctrl, boolean shift, boolean alt) {
        this.keyCode = keyCode;
        this.ctrl = ctrl;
        this.shift = shift;
        this.alt = alt;
        KeybindManager.rebuildBoundKeybinds();
    }

    public void setKey(int keyCode, int modifiers) {
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        boolean alt = (modifiers & GLFW.GLFW_MOD_ALT) != 0;
        set(keyCode, ctrl, shift, alt);
    }

    public void setMouseButton(int button, int modifiers) {
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        boolean alt = (modifiers & GLFW.GLFW_MOD_ALT) != 0;
        set(MOUSE_OFFSET - button, ctrl, shift, alt);
    }

    public void clear() {
        set(UNBOUND, false, false, false);
    }

    public void copyFrom(Keybind other) {
        if (other != null) {
            set(other.keyCode, other.ctrl, other.shift, other.alt);
        }
    }

    public boolean isUnbound() {
        return this.keyCode == UNBOUND || (this.keyCode > MOUSE_OFFSET && this.keyCode <= 0);
    }

    public boolean isMouseButton() {
        return this.keyCode <= MOUSE_OFFSET;
    }

    public int getMouseButton() {
        return isMouseButton() ? MOUSE_OFFSET - this.keyCode : -1;
    }

    /**
     * Checks if this keybind matches the given key code and GLFW modifier bitfield.
     */
    public boolean matchesKey(int key, int modifiers) {
        if (isUnbound() || isMouseButton()) return false;
        if (this.keyCode != key) return false;

        boolean ctrlDown = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shiftDown = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        boolean altDown = (modifiers & GLFW.GLFW_MOD_ALT) != 0;

        return this.ctrl == ctrlDown && this.shift == shiftDown && this.alt == altDown;
    }

    /**
     * Checks if this keybind matches the given mouse button and GLFW modifier bitfield.
     */
    public boolean matchesButton(int button, int modifiers) {
        if (isUnbound() || !isMouseButton()) return false;
        if (getMouseButton() != button) return false;

        boolean ctrlDown = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shiftDown = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        boolean altDown = (modifiers & GLFW.GLFW_MOD_ALT) != 0;

        return this.ctrl == ctrlDown && this.shift == shiftDown && this.alt == altDown;
    }

    /**
     * Checks if this keybind matches the Minecraft client KeyInput event.
     */
    public boolean matchesKeyInput(KeyInput input) {
        if (input == null) return false;
        return matchesKey(input.key(), input.modifiers());
    }

    /**
     * Tests whether this keybind is currently held in the game window.
     */
    public boolean matchesWindow(Window window, boolean ctrlDown, boolean shiftDown, boolean altDown) {
        if (isUnbound() || window == null || window.getHandle() == 0L) return false;
        if (this.ctrl != ctrlDown || this.shift != shiftDown || this.alt != altDown) return false;

        if (isMouseButton()) {
            int button = getMouseButton();
            return GLFW.glfwGetMouseButton(window.getHandle(), button) == GLFW.GLFW_PRESS;
        }

        return InputUtil.isKeyPressed(window, this.keyCode);
    }

    /**
     * Produces a human-readable, localized string representation of the keybind.
     */
    public String format() {
        if (isUnbound()) {
            try {
                return Text.translatable("activity.keybind.none").getString();
            } catch (Throwable ignored) {
                return "Нет";
            }
        }

        StringBuilder sb = new StringBuilder();
        if (this.ctrl) sb.append("Ctrl + ");
        if (this.shift) sb.append("Shift + ");
        if (this.alt) sb.append("Alt + ");

        if (isMouseButton()) {
            int button = getMouseButton();
            try {
                switch (button) {
                    case 0 -> sb.append(Text.translatable("activity.keybind.mouse.lmb").getString());
                    case 1 -> sb.append(Text.translatable("activity.keybind.mouse.rmb").getString());
                    case 2 -> sb.append(Text.translatable("activity.keybind.mouse.wheel").getString());
                    default -> sb.append(Text.translatable("activity.keybind.mouse.button", String.valueOf(button + 1)).getString());
                }
            } catch (Throwable ignored) {
                switch (button) {
                    case 0 -> sb.append("ЛКМ");
                    case 1 -> sb.append("ПКМ");
                    case 2 -> sb.append("Колесико");
                    default -> sb.append("Мышь ").append(button + 1);
                }
            }
            return sb.toString();
        }

        String name = null;
        try {
            InputUtil.Key key = InputUtil.Type.KEYSYM.createFromCode(this.keyCode);
            if (key != null) {
                name = key.getLocalizedText().getString();
                if (name != null && name.startsWith("key.keyboard.")) {
                    name = name.substring("key.keyboard.".length());
                } else if (name != null && name.startsWith("key.")) {
                    name = name.substring("key.".length());
                }
            }
        } catch (Throwable ignored) {}

        if (name == null || name.isBlank()) {
            try {
                name = GLFW.glfwGetKeyName(this.keyCode, 0);
            } catch (Throwable ignored) {}
        }

        if (name == null || name.isBlank()) {
            sb.append("K").append(this.keyCode);
        } else {
            sb.append(name.toUpperCase());
        }

        return sb.toString();
    }

    /**
     * Returns a Minecraft Text representation of the formatted keybind.
     */
    public Text getDisplayText() {
        if (isUnbound()) {
            return Text.translatable("activity.keybind.none");
        }
        return Text.literal(format());
    }

    /**
     * Returns a string representation of the formatted keybind.
     */
    public String getDisplayString() {
        return format();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Keybind keybind)) return false;
        return this.keyCode == keybind.keyCode &&
               this.ctrl == keybind.ctrl &&
               this.shift == keybind.shift &&
               this.alt == keybind.alt;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.keyCode, this.ctrl, this.shift, this.alt);
    }

    @Override
    public String toString() {
        return "Keybind[" + format() + ", code=" + this.keyCode + "]";
    }
}

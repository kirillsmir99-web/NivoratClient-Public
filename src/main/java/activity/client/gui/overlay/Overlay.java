package activity.client.gui.overlay;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;

public interface Overlay extends Element, Drawable {

    @Override
    void render(DrawContext context, int mouseX, int mouseY, float delta);

    boolean contains(double mouseX, double mouseY);

    default boolean shouldCloseOnClickOutside() {
        return true;
    }

    default boolean shouldCloseOnEsc() {
        return true;
    }

    default boolean blocksBackgroundClicks() {
        return true;
    }

    default void onOpen() {}

    void close();

    boolean isClosed();

    @Override
    default boolean mouseClicked(Click click, boolean doubled) {
        return false;
    }

    @Override
    default boolean mouseReleased(Click click) {
        return false;
    }

    @Override
    default boolean mouseDragged(Click click, double deltaX, double deltaY) {
        return false;
    }

    @Override
    default boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return false;
    }

    @Override
    default boolean keyPressed(KeyInput input) {
        return false;
    }

    @Override
    default boolean keyReleased(KeyInput input) {
        return false;
    }

    @Override
    default boolean charTyped(CharInput input) {
        return false;
    }

    @Override
    default boolean isMouseOver(double mouseX, double mouseY) {
        return contains(mouseX, mouseY);
    }

    @Override
    default void setFocused(boolean focused) {}

    @Override
    default boolean isFocused() {
        return false;
    }
}

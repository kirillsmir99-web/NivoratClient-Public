package activity.client.gui.overlay;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;

/**
 * Contract for top-level floating elements in the Activity GUI framework.
 * Overlays render on top of all regular components (Z=7) and take priority
 * during input event dispatching.
 *
 * <p>Typical implementations include dropdown menus, popups, modals, and tooltips.
 */
public interface Overlay extends Element, Drawable {

    /**
     * Renders the overlay content on top of other screen elements.
     *
     * @param context the draw context
     * @param mouseX  current mouse X coordinate
     * @param mouseY  current mouse Y coordinate
     * @param delta   partial tick delta time
     */
    @Override
    void render(DrawContext context, int mouseX, int mouseY, float delta);

    /**
     * Determines whether the given mouse coordinates lie within the interactive bounds of this overlay.
     * Used by {@link OverlayManager} to route clicks to the overlay or detect outside clicks.
     *
     * @param mouseX the mouse X coordinate
     * @param mouseY the mouse Y coordinate
     * @return true if the coordinate is within this overlay's bounds
     */
    boolean contains(double mouseX, double mouseY);

    /**
     * @return true if this overlay should automatically close when the user clicks outside its bounds
     */
    default boolean shouldCloseOnClickOutside() {
        return true;
    }

    /**
     * @return true if this overlay should close when the user presses the Escape key
     */
    default boolean shouldCloseOnEsc() {
        return true;
    }

    /**
     * @return true if outside clicks and unhandled inputs should be blocked from reaching background elements.
     * Modals and dropdowns return true (default), non-intrusive toasts return false.
     */
    default boolean blocksBackgroundClicks() {
        return true;
    }

    /**
     * Called when the overlay is opened and registered with the {@link OverlayManager}.
     */
    default void onOpen() {}

    /**
     * Closes the overlay and releases any captured resources or states.
     */
    void close();

    /**
     * @return true if this overlay is marked as closed and should be discarded
     */
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

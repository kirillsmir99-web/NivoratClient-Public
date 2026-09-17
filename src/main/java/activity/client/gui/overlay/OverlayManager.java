package activity.client.gui.overlay;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;

/**
 * Centralized manager for floating overlays in the Activity GUI framework.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Maintains active overlay stack (Z=7 Layer).</li>
 *   <li>Intercepts mouse and keyboard inputs before they reach regular components.</li>
 *   <li>Guarantees strict click-through prevention (outside clicks dismiss the overlay and consume the event).</li>
 *   <li>Intercepts Escape key to close the overlay first without closing the underlying Screen.</li>
 * </ul>
 */
public class OverlayManager {

    private final Deque<Overlay> overlayStack = new ArrayDeque<>();

    /**
     * Opens a new overlay and places it on top of the stack.
     *
     * @param overlay the overlay to open
     */
    public void open(Overlay overlay) {
        if (overlay == null) return;
        this.overlayStack.push(overlay);
        overlay.onOpen();
    }

    /**
     * Closes and removes a specific overlay from the stack.
     *
     * @param overlay the overlay to close
     */
    public void close(Overlay overlay) {
        if (overlay == null) return;
        overlay.close();
        if (overlay.isClosed()) {
            this.overlayStack.remove(overlay);
        }
    }

    /**
     * Closes and removes the topmost overlay.
     */
    public void closeTop() {
        if (!this.overlayStack.isEmpty()) {
            Overlay top = this.overlayStack.peek();
            if (top != null) {
                top.close();
                if (top.isClosed()) {
                    this.overlayStack.pop();
                }
            }
        }
    }

    /**
     * Closes and removes all active overlays.
     */
    public void clear() {
        while (!this.overlayStack.isEmpty()) {
            Overlay overlay = this.overlayStack.pop();
            overlay.close();
        }
    }

    /**
     * @return true if there is at least one active overlay
     */
    public boolean hasActiveOverlay() {
        pruneClosed();
        return !this.overlayStack.isEmpty();
    }

    /**
     * @return the topmost active overlay, or null if none
     */
    @Nullable
    public Overlay getActiveOverlay() {
        pruneClosed();
        return this.overlayStack.peek();
    }

    /**
     * Renders all active overlays from bottom to top of the stack.
     */
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        pruneClosed();
        if (this.overlayStack.isEmpty()) return;

        if (this.overlayStack.size() == 1) {
            Overlay single = this.overlayStack.peek();
            if (single != null && !single.isClosed()) {
                single.render(context, mouseX, mouseY, delta);
            }
            return;
        }

        var iterator = this.overlayStack.descendingIterator();
        while (iterator.hasNext()) {
            Overlay overlay = iterator.next();
            if (!overlay.isClosed()) {
                overlay.render(context, mouseX, mouseY, delta);
            }
        }
    }

    /**
     * Closes and removes all overlays matching the given filter.
     *
     * @param predicate filter to identify overlays to close
     */
    public void closeMatching(Predicate<Overlay> predicate) {
        if (predicate == null || this.overlayStack.isEmpty()) return;
        List<Overlay> snapshot = new ArrayList<>(this.overlayStack);
        for (Overlay overlay : snapshot) {
            if (predicate.test(overlay)) {
                close(overlay);
                this.overlayStack.remove(overlay);
            }
        }
        pruneClosed();
    }

    /**
     * Dispatches mouse click events to active overlays from top to bottom.
     *
     * <p>If the click is within an overlay, it is forwarded.
     * If outside, outside-close overlays are dismissed. If any overlay blocks background clicks,
     * the click is consumed to prevent click-through.
     *
     * @param click   the click record
     * @param doubled whether it is a double-click
     * @return true if the click was consumed
     */
    public boolean mouseClicked(Click click, boolean doubled) {
        pruneClosed();
        if (this.overlayStack.isEmpty()) {
            return false;
        }

        if (this.overlayStack.size() == 1) {
            Overlay overlay = this.overlayStack.peek();
            if (overlay != null && !overlay.isClosed()) {
                if (overlay.contains(click.x(), click.y())) {
                    overlay.mouseClicked(click, doubled);
                    return true;
                }

                if (overlay.shouldCloseOnClickOutside()) {
                    close(overlay);
                }

                if (overlay.blocksBackgroundClicks()) {
                    return true;
                }
            }
            return false;
        }

        List<Overlay> snapshot = new ArrayList<>(this.overlayStack);
        for (Overlay overlay : snapshot) {
            if (overlay.isClosed()) continue;

            if (overlay.contains(click.x(), click.y())) {
                overlay.mouseClicked(click, doubled);
                return true;
            }

            if (overlay.shouldCloseOnClickOutside()) {
                close(overlay);
            }

            if (overlay.blocksBackgroundClicks()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Dispatches mouse release events to the active overlays from top to bottom.
     */
    public boolean mouseReleased(Click click) {
        pruneClosed();
        if (this.overlayStack.isEmpty()) {
            return false;
        }

        if (this.overlayStack.size() == 1) {
            Overlay overlay = this.overlayStack.peek();
            if (overlay != null && !overlay.isClosed()) {
                if (overlay.contains(click.x(), click.y()) || overlay.blocksBackgroundClicks()) {
                    overlay.mouseReleased(click);
                    return true;
                }
            }
            return false;
        }

        List<Overlay> snapshot = new ArrayList<>(this.overlayStack);
        for (Overlay overlay : snapshot) {
            if (overlay.isClosed()) continue;

            if (overlay.contains(click.x(), click.y())) {
                overlay.mouseReleased(click);
                return true;
            }

            if (overlay.blocksBackgroundClicks()) {
                overlay.mouseReleased(click);
                return true;
            }
        }
        return false;
    }

    /**
     * Dispatches mouse drag events to active overlays from top to bottom.
     */
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        pruneClosed();
        if (this.overlayStack.isEmpty()) {
            return false;
        }

        if (this.overlayStack.size() == 1) {
            Overlay overlay = this.overlayStack.peek();
            if (overlay != null && !overlay.isClosed()) {
                if (overlay.contains(click.x(), click.y()) || overlay.blocksBackgroundClicks()) {
                    overlay.mouseDragged(click, deltaX, deltaY);
                    return true;
                }
            }
            return false;
        }

        List<Overlay> snapshot = new ArrayList<>(this.overlayStack);
        for (Overlay overlay : snapshot) {
            if (overlay.isClosed()) continue;

            if (overlay.contains(click.x(), click.y()) || overlay.blocksBackgroundClicks()) {
                overlay.mouseDragged(click, deltaX, deltaY);
                return true;
            }
        }
        return false;
    }

    /**
     * Dispatches mouse scroll events to active overlays from top to bottom.
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        pruneClosed();
        if (this.overlayStack.isEmpty()) {
            return false;
        }

        if (this.overlayStack.size() == 1) {
            Overlay overlay = this.overlayStack.peek();
            if (overlay != null && !overlay.isClosed()) {
                if (overlay.contains(mouseX, mouseY) || overlay.blocksBackgroundClicks()) {
                    overlay.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
                    return true;
                }
            }
            return false;
        }

        List<Overlay> snapshot = new ArrayList<>(this.overlayStack);
        for (Overlay overlay : snapshot) {
            if (overlay.isClosed()) continue;

            if (overlay.contains(mouseX, mouseY) || overlay.blocksBackgroundClicks()) {
                overlay.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
                return true;
            }
        }
        return false;
    }

    /**
     * Dispatches key press events to active overlays from top to bottom.
     * Intercepts Escape if {@link Overlay#shouldCloseOnEsc()} is true, closing the top-most matching overlay.
     */
    public boolean keyPressed(KeyInput input) {
        pruneClosed();
        if (this.overlayStack.isEmpty()) {
            return false;
        }

        if (this.overlayStack.size() == 1) {
            Overlay overlay = this.overlayStack.peek();
            if (overlay != null && !overlay.isClosed()) {
                if (overlay.keyPressed(input)) {
                    return true;
                }

                if (input.isEscape() && overlay.shouldCloseOnEsc()) {
                    close(overlay);
                    return true;
                }

                if (overlay.blocksBackgroundClicks()) {
                    return true;
                }
            }
            return false;
        }

        List<Overlay> snapshot = new ArrayList<>(this.overlayStack);
        for (Overlay overlay : snapshot) {
            if (overlay.isClosed()) continue;

            if (overlay.keyPressed(input)) {
                return true;
            }

            if (input.isEscape() && overlay.shouldCloseOnEsc()) {
                close(overlay);
                return true;
            }

            if (overlay.blocksBackgroundClicks()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Dispatches key release events to active overlays from top to bottom.
     */
    public boolean keyReleased(KeyInput input) {
        pruneClosed();
        if (this.overlayStack.isEmpty()) {
            return false;
        }

        if (this.overlayStack.size() == 1) {
            Overlay overlay = this.overlayStack.peek();
            if (overlay != null && !overlay.isClosed()) {
                if (overlay.keyReleased(input)) {
                    return true;
                }

                if (overlay.blocksBackgroundClicks()) {
                    return true;
                }
            }
            return false;
        }

        List<Overlay> snapshot = new ArrayList<>(this.overlayStack);
        for (Overlay overlay : snapshot) {
            if (overlay.isClosed()) continue;

            if (overlay.keyReleased(input)) {
                return true;
            }

            if (overlay.blocksBackgroundClicks()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Dispatches character typed events to active overlays from top to bottom.
     */
    public boolean charTyped(CharInput input) {
        pruneClosed();
        if (this.overlayStack.isEmpty()) {
            return false;
        }

        if (this.overlayStack.size() == 1) {
            Overlay overlay = this.overlayStack.peek();
            if (overlay != null && !overlay.isClosed()) {
                if (overlay.charTyped(input)) {
                    return true;
                }

                if (overlay.blocksBackgroundClicks()) {
                    return true;
                }
            }
            return false;
        }

        List<Overlay> snapshot = new ArrayList<>(this.overlayStack);
        for (Overlay overlay : snapshot) {
            if (overlay.isClosed()) continue;

            if (overlay.charTyped(input)) {
                return true;
            }

            if (overlay.blocksBackgroundClicks()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the mouse is currently over any active overlay.
     */
    public boolean isMouseOver(double mouseX, double mouseY) {
        pruneClosed();
        if (this.overlayStack.isEmpty()) {
            return false;
        }
        if (this.overlayStack.size() == 1) {
            Overlay single = this.overlayStack.peek();
            return single != null && !single.isClosed() && single.contains(mouseX, mouseY);
        }
        for (Overlay overlay : this.overlayStack) {
            if (!overlay.isClosed() && overlay.contains(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    private void pruneClosed() {
        if (this.overlayStack.isEmpty()) return;
        this.overlayStack.removeIf(Overlay::isClosed);
    }
}

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

public class OverlayManager {

    private final Deque<Overlay> overlayStack = new ArrayDeque<>();

    public void open(Overlay overlay) {
        if (overlay == null) return;
        this.overlayStack.push(overlay);
        overlay.onOpen();
    }

    public void close(Overlay overlay) {
        if (overlay == null) return;
        overlay.close();
        if (overlay.isClosed()) {
            this.overlayStack.remove(overlay);
        }
    }

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

    public void clear() {
        while (!this.overlayStack.isEmpty()) {
            Overlay overlay = this.overlayStack.pop();
            overlay.close();
        }
    }

    public boolean hasActiveOverlay() {
        pruneClosed();
        return !this.overlayStack.isEmpty();
    }

    @Nullable
    public Overlay getActiveOverlay() {
        pruneClosed();
        return this.overlayStack.peek();
    }

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

package activity.client.gui.layout;

import activity.client.gui.animation.AnimationClock;
import activity.client.gui.component.ActivityComponent;
import activity.client.gui.overlay.OverlayManager;
import activity.client.gui.render.ActivityGuiRenderer;
import activity.client.gui.render.ScissorHelper;
import activity.client.gui.theme.ActivityColors;
import activity.client.gui.theme.ActivityMetrics;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Modern scrollable container component for the Activity GUI framework.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Viewport clipping via {@link ScissorHelper} using native DrawContext scissors.</li>
 *   <li>Axis-Aligned Bounding Box (AABB) culling of off-screen children during render and input dispatch.</li>
 *   <li>Smooth scrolling interpolation (configurable lerp factor adhering to 80-150ms animation budget).</li>
 *   <li>Draggable scrollbar thumb with continuous mouse capture and hover feedback.</li>
 *   <li>Mouse wheel scrolling with configurable step size.</li>
 *   <li>Keyboard navigation support (Page Up, Page Down, Home, End, Tab auto-scroll to focused child).</li>
 *   <li>Overlay isolation: automatically dismisses detached floating dropdown popups upon container scrolling.</li>
 * </ul>
 */
public class ScrollContainer extends ActivityComponent {

    /**
     * Internal metadata entry pairing a component with its relative layout coordinates.
     */
    public static final class ScrollEntry {
        private final ActivityComponent component;
        private final int relX;
        private int relY;
        private final int origRelY;

        public ScrollEntry(ActivityComponent component, int relX, int relY) {
            this.component = component;
            this.relX = relX;
            this.relY = relY;
            this.origRelY = relY;
        }

        public ActivityComponent getComponent() {
            return component;
        }

        public int getRelX() {
            return relX;
        }

        public int getRelY() {
            return relY;
        }

        public int getOrigRelY() {
            return origRelY;
        }

        public void setRelY(int relY) {
            this.relY = relY;
        }
    }

    private final List<ScrollEntry> entries = new ArrayList<>();
    private int contentHeight = 0;
    private int contentPaddingBottom = 12;

    private double scrollAmount = 0.0;
    private double targetScrollAmount = 0.0;
    private double scrollStep = 22.0;
    private boolean smoothScrolling = true;

    private boolean isDraggingScrollbar = false;
    private int dragClickOffsetY = 0;

    private boolean drawBackground = false;
    private int backgroundColor = ActivityColors.PANEL_INNER_BG;
    private int borderColor = ActivityColors.BORDER;

    @Nullable
    private OverlayManager overlayManager = null;
    @Nullable
    private Consumer<Double> onScroll = null;
    @Nullable
    private ActivityComponent focusedChild = null;

    public ScrollContainer(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public ScrollContainer() {
        this(0, 0, 0, 0);
    }

    // --- Component Management ---

    /**
     * Adds a child component, calculating its relative coordinates automatically from its current position.
     */
    public <T extends ActivityComponent> T addChild(T component) {
        if (component == null) return null;
        return addChild(component, component.getX() - this.x, component.getY() - this.y);
    }

    /**
     * Adds a child component with explicit relative layout coordinates.
     */
    public <T extends ActivityComponent> T addChild(T component, int relX, int relY) {
        if (component == null) return null;
        this.entries.add(new ScrollEntry(component, relX, relY));
        component.setX(this.x + relX);
        component.setY(this.y + relY - (int) Math.round(this.scrollAmount));
        this.contentHeight = computeContentHeight();
        return component;
    }

    public boolean hasChild(ActivityComponent component) {
        if (component == null) return false;
        for (ScrollEntry entry : this.entries) {
            if (entry.component == component) return true;
        }
        return false;
    }

    public void removeChild(ActivityComponent component) {
        this.entries.removeIf(entry -> entry.component == component);
        if (this.focusedChild == component) {
            clearFocus();
        }
        this.contentHeight = computeContentHeight();
        clampScroll();
    }

    public int getComponentRelY(ActivityComponent component) {
        if (component == null) return -1;
        for (ScrollEntry entry : this.entries) {
            if (entry.component == component) {
                return entry.relY;
            }
        }
        return -1;
    }

    public int getComponentOrigRelY(ActivityComponent component) {
        if (component == null) return -1;
        for (ScrollEntry entry : this.entries) {
            if (entry.component == component) {
                return entry.origRelY;
            }
        }
        return -1;
    }

    public int getComponentOrigRelX(ActivityComponent component) {
        if (component == null) return -1;
        for (ScrollEntry entry : this.entries) {
            if (entry.component == component) {
                return entry.relX;
            }
        }
        return -1;
    }

    public void setComponentRelY(ActivityComponent component, int newRelY) {
        if (component == null) return;
        for (ScrollEntry entry : this.entries) {
            if (entry.component == component) {
                entry.setRelY(newRelY);
                component.setY(this.y + newRelY - (int) Math.round(this.scrollAmount));
                break;
            }
        }
    }

    public void restoreOriginalPositions() {
        for (ScrollEntry entry : this.entries) {
            entry.setRelY(entry.origRelY);
            entry.component.setY(this.y + entry.origRelY - (int) Math.round(this.scrollAmount));
        }
        this.contentHeight = computeContentHeight();
        clampScroll();
    }

    public void recomputeContentHeight() {
        this.contentHeight = computeContentHeight();
        clampScroll();
    }

    public void scrollToChild(ActivityComponent component) {
        int relY = getComponentRelY(component);
        if (relY >= 0) {
            scrollTo(relY);
        }
    }

    public void clearChildren() {
        this.entries.clear();
        clearFocus();
        this.contentHeight = 0;
        this.scrollAmount = 0.0;
        this.targetScrollAmount = 0.0;
    }

    public List<ActivityComponent> getChildren() {
        List<ActivityComponent> list = new ArrayList<>(this.entries.size());
        for (ScrollEntry entry : this.entries) {
            list.add(entry.component);
        }
        return Collections.unmodifiableList(list);
    }

    public List<ScrollEntry> getEntries() {
        return Collections.unmodifiableList(this.entries);
    }

    /**
     * Computes the total scrollable content height based on child positions and heights.
     */
    public int computeContentHeight() {
        int maxBottom = 0;
        for (ScrollEntry entry : this.entries) {
            if (entry.component.isVisible()) {
                int bottom = entry.relY + entry.component.getHeight();
                if (bottom > maxBottom) {
                    maxBottom = bottom;
                }
            }
        }
        return maxBottom + this.contentPaddingBottom;
    }

    public void setContentPaddingBottom(int contentPaddingBottom) {
        this.contentPaddingBottom = contentPaddingBottom;
        this.contentHeight = computeContentHeight();
    }

    public int getContentHeight() {
        return contentHeight;
    }

    public void setContentHeight(int contentHeight) {
        this.contentHeight = contentHeight;
        clampScroll();
    }

    // --- Geometry Overrides ---

    @Override
    public void setX(int x) {
        super.setX(x);
        updateChildrenPositions();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        updateChildrenPositions();
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        this.contentHeight = computeContentHeight();
        clampScroll();
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        this.contentHeight = computeContentHeight();
        clampScroll();
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        this.contentHeight = computeContentHeight();
        clampScroll();
        updateChildrenPositions();
    }

    public void updateChildrenPositions() {
        int scroll = (int) Math.round(this.scrollAmount);
        for (ScrollEntry entry : this.entries) {
            entry.component.setX(this.x + entry.relX);
            entry.component.setY(this.y + entry.relY - scroll);
        }
    }

    // --- Scrolling Mechanics ---

    public int getMaxScroll() {
        return Math.max(0, this.contentHeight - this.height);
    }

    public double getScrollAmount() {
        return this.scrollAmount;
    }

    public void setScrollAmount(double amount) {
        double clamped = Math.clamp(amount, 0, getMaxScroll());
        if (this.scrollAmount != clamped || this.targetScrollAmount != clamped) {
            this.scrollAmount = this.targetScrollAmount = clamped;
            closeOverlayIfOpen();
            updateChildrenPositions();
            if (this.onScroll != null) {
                this.onScroll.accept(this.scrollAmount);
            }
        }
    }

    public void scrollTo(double target) {
        double clamped = Math.clamp(target, 0, getMaxScroll());
        if (this.targetScrollAmount != clamped) {
            this.targetScrollAmount = clamped;
            closeOverlayIfOpen();
            if (!this.smoothScrolling) {
                setScrollAmount(clamped);
            }
        }
    }

    public void scrollBy(double delta) {
        scrollTo(this.targetScrollAmount + delta);
    }

    /**
     * Automatically scrolls the viewport to reveal the specified child component if it is out of view.
     */
    public void scrollToVisible(ActivityComponent component) {
        if (component == null) return;
        for (ScrollEntry entry : this.entries) {
            if (entry.component == component) {
                int compTop = entry.relY;
                int compBottom = entry.relY + component.getHeight();
                if (compTop < this.targetScrollAmount) {
                    scrollTo(compTop);
                } else if (compBottom > this.targetScrollAmount + this.height) {
                    scrollTo(compBottom - this.height + 4);
                }
                break;
            }
        }
    }

    private void clampScroll() {
        int max = getMaxScroll();
        if (this.scrollAmount > max) {
            this.scrollAmount = max;
        }
        if (this.targetScrollAmount > max) {
            this.targetScrollAmount = max;
        }
        updateChildrenPositions();
    }

    private void closeOverlayIfOpen() {
        if (this.overlayManager != null && this.overlayManager.hasActiveOverlay()) {
            this.overlayManager.clear();
        }
    }

    public void setOverlayManager(@Nullable OverlayManager overlayManager) {
        this.overlayManager = overlayManager;
    }

    public void setOnScroll(@Nullable Consumer<Double> onScroll) {
        this.onScroll = onScroll;
    }

    public boolean isSmoothScrolling() {
        return smoothScrolling;
    }

    public void setSmoothScrolling(boolean smoothScrolling) {
        this.smoothScrolling = smoothScrolling;
    }

    public double getScrollStep() {
        return scrollStep;
    }

    public void setScrollStep(double scrollStep) {
        this.scrollStep = scrollStep;
    }

    public void setDrawBackground(boolean drawBackground) {
        this.drawBackground = drawBackground;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
    }

    // --- Focus Management ---

    @Nullable
    public ActivityComponent getFocusedChild() {
        if (this.focusedChild != null && !this.focusedChild.isFocused()) {
            this.focusedChild = null;
        }
        return focusedChild;
    }

    public void setFocusedChild(@Nullable ActivityComponent child) {
        if (child != null && !hasChild(child)) {
            clearFocus();
            return;
        }
        if (this.focusedChild != child) {
            if (this.focusedChild != null) {
                this.focusedChild.setFocused(false);
            }
            this.focusedChild = child;
            if (this.focusedChild != null) {
                this.focusedChild.setFocused(true);
            }
        }
    }

    public void clearFocus() {
        if (this.focusedChild != null) {
            this.focusedChild.setFocused(false);
            this.focusedChild = null;
        }
    }

    public List<ActivityComponent> getFocusableComponents() {
        List<ActivityComponent> focusable = new ArrayList<>();
        for (ScrollEntry entry : this.entries) {
            ActivityComponent comp = entry.component;
            if (comp.isVisible() && comp.isEnabled()) {
                focusable.add(comp);
            }
        }
        return focusable;
    }

    // --- Tick Lifecycle ---

    @Override
    public void tick() {
        super.tick();
        for (ScrollEntry entry : this.entries) {
            entry.component.tick();
        }
    }

    // --- Rendering Lifecycle ---

    @Override
    protected void renderComponent(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.width <= 2 || this.height <= 2) return;

        // Smooth scroll interpolation via centralized AnimationClock (~120ms exponential response)
        if (this.smoothScrolling) {
            double diff = this.targetScrollAmount - this.scrollAmount;
            if (Math.abs(diff) > 0.02) {
                this.scrollAmount = AnimationClock.approachExp(this.scrollAmount, this.targetScrollAmount, AnimationClock.SCROLL_DECAY_RATE);
                updateChildrenPositions();
                if (this.onScroll != null) {
                    this.onScroll.accept(this.scrollAmount);
                }
            } else if (this.scrollAmount != this.targetScrollAmount) {
                this.scrollAmount = this.targetScrollAmount;
                updateChildrenPositions();
                if (this.onScroll != null) {
                    this.onScroll.accept(this.scrollAmount);
                }
            }
        }

        if (this.drawBackground) {
            ActivityGuiRenderer.drawPanel(context, this.x, this.y, this.width, this.height, this.backgroundColor, this.borderColor);
        }

        // Viewport Scissor Clipping
        ScissorHelper.pushScissor(context, this.x, this.y, this.width, this.height);

        try {
            for (ScrollEntry entry : this.entries) {
                ActivityComponent comp = entry.component;
                if (comp.isVisible() && ScissorHelper.intersects(comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight(),
                                                                 this.x, this.y, this.width, this.height)) {
                    comp.render(context, mouseX, mouseY, delta);
                }
            }
        } finally {
            ScissorHelper.popScissor(context);
        }

        // Render scrollbar outside scissor clipping
        renderScrollbar(context, mouseX, mouseY);
    }

    private void renderScrollbar(DrawContext context, int mouseX, int mouseY) {
        int maxScroll = getMaxScroll();
        if (maxScroll <= 0) return;

        int scrollbarW = ActivityMetrics.SCROLLBAR_WIDTH;
        int trackX = this.x + this.width - scrollbarW - 1;
        int trackY = this.y + 2;
        int trackH = this.height - 4;
        if (trackH <= 4) return;

        // Draw track
        ActivityGuiRenderer.fill(context, trackX, trackY, scrollbarW, trackH, ActivityColors.SCROLLBAR_TRACK);

        // Compute thumb height and Y
        int thumbH = Math.max(ActivityMetrics.SCROLLBAR_MIN_THUMB, (int) Math.round((double) this.height / this.contentHeight * trackH));
        int availableH = trackH - thumbH;
        int thumbY = trackY + (int) Math.round(availableH * (this.scrollAmount / maxScroll));

        boolean isHovered = mouseX >= trackX - 2 && mouseX <= trackX + scrollbarW + 2 &&
                            mouseY >= thumbY && mouseY <= thumbY + thumbH;

        int thumbColor = this.isDraggingScrollbar ? ActivityColors.SCROLLBAR_THUMB_DRAG :
                         (isHovered ? ActivityColors.SCROLLBAR_THUMB_HOVER : ActivityColors.SCROLLBAR_THUMB);

        ActivityGuiRenderer.fill(context, trackX, thumbY, scrollbarW, thumbH, thumbColor);
    }

    // --- Input Handling ---

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (!this.visible || !this.enabled) return false;

        int maxScroll = getMaxScroll();
        int scrollbarW = ActivityMetrics.SCROLLBAR_WIDTH;
        int trackX = this.x + this.width - scrollbarW - 1;
        int trackY = this.y + 2;
        int trackH = this.height - 4;

        // Priority 1: Click on scrollbar track or thumb
        if (maxScroll > 0 && click.button() == 0 && trackH > 4 &&
            click.x() >= trackX - 2 && click.x() <= trackX + scrollbarW + 2 &&
            click.y() >= trackY && click.y() <= trackY + trackH) {

            int thumbH = Math.max(ActivityMetrics.SCROLLBAR_MIN_THUMB, (int) Math.round((double) this.height / this.contentHeight * trackH));
            int availableH = trackH - thumbH;
            int thumbY = trackY + (int) Math.round(availableH * (this.scrollAmount / maxScroll));

            if (click.y() >= thumbY && click.y() <= thumbY + thumbH) {
                this.isDraggingScrollbar = true;
                this.dragClickOffsetY = (int) click.y() - thumbY;
                return true;
            } else {
                // Click on track above or below thumb
                if (click.y() < thumbY) {
                    scrollBy(-this.height * 0.75);
                } else {
                    scrollBy(this.height * 0.75);
                }
                return true;
            }
        }

        // Priority 2: Interactive children within viewport
        if (isMouseOver(click.x(), click.y())) {
            for (int i = this.entries.size() - 1; i >= 0; i--) {
                ActivityComponent comp = this.entries.get(i).component;
                if (comp.isVisible() && comp.isEnabled() && comp.isMouseOver(click.x(), click.y()) &&
                    comp.getY() + comp.getHeight() > this.y && comp.getY() < this.y + this.height) {
                    if (comp.mouseClicked(click, doubled)) {
                        if (comp.isFocused()) {
                            setFocusedChild(comp);
                        } else {
                            clearFocus();
                        }
                        return true;
                    }
                }
            }

            // Click landed in empty container area: clear child focus and consume event
            clearFocus();
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (this.isDraggingScrollbar) {
            this.isDraggingScrollbar = false;
            return true;
        }

        boolean handled = false;
        for (int i = this.entries.size() - 1; i >= 0; i--) {
            ActivityComponent comp = this.entries.get(i).component;
            if (comp.isVisible() && comp.mouseReleased(click)) {
                handled = true;
            }
        }
        return handled;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (this.isDraggingScrollbar) {
            int maxScroll = getMaxScroll();
            if (maxScroll > 0) {
                int trackY = this.y + 2;
                int trackH = this.height - 4;
                int thumbH = Math.max(ActivityMetrics.SCROLLBAR_MIN_THUMB, (int) Math.round((double) this.height / this.contentHeight * trackH));
                int availableH = trackH - thumbH;
                if (availableH > 0) {
                    int targetThumbY = (int) click.y() - this.dragClickOffsetY;
                    double progress = (double) (targetThumbY - trackY) / availableH;
                    setScrollAmount(progress * maxScroll);
                }
            }
            return true;
        }

        for (int i = this.entries.size() - 1; i >= 0; i--) {
            ActivityComponent comp = this.entries.get(i).component;
            if (comp.isVisible() && comp.mouseDragged(click, deltaX, deltaY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isMouseOver(mouseX, mouseY)) {
            for (int i = this.entries.size() - 1; i >= 0; i--) {
                ActivityComponent comp = this.entries.get(i).component;
                if (comp.isVisible() && comp.isEnabled() && comp.isMouseOver(mouseX, mouseY) &&
                    comp.getY() + comp.getHeight() > this.y && comp.getY() < this.y + this.height) {
                    if (comp.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
                        return true;
                    }
                }
            }
            scrollBy(-verticalAmount * this.scrollStep);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (this.focusedChild != null && !this.focusedChild.isFocused()) {
            this.focusedChild = null;
        }

        // First forward to focused child
        if (this.focusedChild != null && this.focusedChild.isVisible() && this.focusedChild.isEnabled()) {
            if (this.focusedChild.keyPressed(input)) {
                if (!this.focusedChild.isFocused()) {
                    this.focusedChild = null;
                }
                return true;
            }
        }
        if (this.focusedChild != null && !this.focusedChild.isFocused()) {
            this.focusedChild = null;
        }

        // Forward to other interactive children
        for (int i = this.entries.size() - 1; i >= 0; i--) {
            ActivityComponent comp = this.entries.get(i).component;
            if (comp != this.focusedChild && comp.isVisible() && comp.isEnabled() && comp.keyPressed(input)) {
                return true;
            }
        }

        // Keyboard scrolling navigation
        int key = input.key();
        if (key == GLFW.GLFW_KEY_PAGE_UP) {
            scrollBy(-this.height * 0.8);
            return true;
        } else if (key == GLFW.GLFW_KEY_PAGE_DOWN) {
            scrollBy(this.height * 0.8);
            return true;
        } else if (key == GLFW.GLFW_KEY_HOME) {
            scrollTo(0);
            return true;
        } else if (key == GLFW.GLFW_KEY_END) {
            scrollTo(getMaxScroll());
            return true;
        }

        return false;
    }

    @Override
    public boolean keyReleased(KeyInput input) {
        if (this.focusedChild != null && !this.focusedChild.isFocused()) {
            this.focusedChild = null;
        }

        if (this.focusedChild != null && this.focusedChild.isVisible() && this.focusedChild.keyReleased(input)) {
            return true;
        }

        for (int i = this.entries.size() - 1; i >= 0; i--) {
            ActivityComponent comp = this.entries.get(i).component;
            if (comp != this.focusedChild && comp.isVisible() && comp.keyReleased(input)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (this.focusedChild != null && !this.focusedChild.isFocused()) {
            this.focusedChild = null;
        }

        if (this.focusedChild != null && this.focusedChild.isVisible() && this.focusedChild.isEnabled()) {
            if (this.focusedChild.charTyped(input)) {
                if (!this.focusedChild.isFocused()) {
                    this.focusedChild = null;
                }
                return true;
            }
        }
        if (this.focusedChild != null && !this.focusedChild.isFocused()) {
            this.focusedChild = null;
        }

        for (int i = this.entries.size() - 1; i >= 0; i--) {
            ActivityComponent comp = this.entries.get(i).component;
            if (comp != this.focusedChild && comp.isVisible() && comp.isEnabled() && comp.charTyped(input)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onFontChanged() {
        for (ScrollEntry entry : this.entries) {
            entry.component.onFontChanged();
        }
        this.contentHeight = computeContentHeight();
        clampScroll();
    }
}

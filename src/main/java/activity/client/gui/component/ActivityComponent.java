package activity.client.gui.component;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;

/**
 * Base abstract class for all custom UI components in the Activity framework.
 *
 * <p>Encapsulates layout geometry (x, y, width, height), visibility, enabled state,
 * focus, hover detection, and lifecycle hooks for rendering and event handling.
 */
public abstract class ActivityComponent implements Element, Drawable {

    protected int x;
    protected int y;
    protected int width;
    protected int height;

    protected boolean visible = true;
    protected boolean enabled = true;
    protected boolean focused = false;
    protected boolean hovered = false;
    protected float alpha = 1.0f;

    public ActivityComponent(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public ActivityComponent() {
        this(0, 0, 0, 0);
    }

    // --- Geometry ---

    public int getX() {
        return this.x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return this.y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return this.width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return this.height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    // --- State flags ---

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isHovered() {
        return this.hovered;
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.visible && mouseX >= this.x && mouseX < this.x + this.width &&
               mouseY >= this.y && mouseY < this.y + this.height;
    }

    public float getAlpha() {
        return this.alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = Math.clamp(alpha, 0.0f, 1.0f);
    }

    // --- Render Lifecycle ---

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!this.visible) return;

        this.hovered = this.isMouseOver(mouseX, mouseY);
        renderComponent(context, mouseX, mouseY, delta);
    }

    /**
     * Procedurally renders the component contents.
     * Called by {@link #render(DrawContext, int, int, float)} only when {@link #isVisible()} is true.
     *
     * @param context draw context
     * @param mouseX  mouse X position
     * @param mouseY  mouse Y position
     * @param delta   frame delta time
     */
    protected abstract void renderComponent(DrawContext context, int mouseX, int mouseY, float delta);

    // --- Input Handling Hooks ---

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        return false;
    }

    @Override
    public boolean keyReleased(KeyInput input) {
        return false;
    }

    @Override
    public boolean charTyped(CharInput input) {
        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {}

    /**
     * Periodic tick callback for animations, timers, and state transitions.
     */
    public void tick() {}

    /**
     * Called when the active font changes, allowing components to invalidate
     * cached text measurements and re-measure bounds.
     */
    public void onFontChanged() {}

    /**
     * Called when the parent container or screen layout bounds change.
     *
     * @param parentWidth  new parent width
     * @param parentHeight new parent height
     */
    public void onLayoutResized(int parentWidth, int parentHeight) {}
}

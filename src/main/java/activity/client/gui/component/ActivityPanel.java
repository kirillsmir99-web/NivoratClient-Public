package activity.client.gui.component;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.animation.AnimationClock;
import activity.client.gui.font.FontManager;
import activity.client.gui.render.ActivityGuiRenderer;
import activity.client.gui.theme.ActivityColors;
import activity.client.gui.theme.ActivityMetrics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Structural card container component.
 * Supports background fill, 1px border, optional dark header bar, and nested child components.
 */
public class ActivityPanel extends ActivityComponent {

    private int backgroundColor = ActivityColors.PANEL_BACKGROUND;
    private int borderColor = ActivityColors.BORDER;
    @Nullable
    private Text title;
    private int headerHeight = 20;
    private float highlightTimer = 0.0f;

    private final List<ActivityComponent> children = new ArrayList<>();

    private Text cachedDisplayTitle = null;
    private int lastMaxTitleW = -1;

    public void invalidateTextCache() {
        this.cachedDisplayTitle = null;
        this.lastMaxTitleW = -1;
    }

    public void flashHighlight() {
        if (!AnimationClock.isAnimationsEnabled()) {
            this.highlightTimer = 0.0f;
            return;
        }
        this.highlightTimer = 1.5f;
    }

    public boolean isHighlighted() {
        return this.highlightTimer > 0.0f;
    }

    public ActivityPanel(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public ActivityPanel(int x, int y, int width, int height, @Nullable Text title) {
        super(x, y, width, height);
        this.title = title;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public int getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
    }

    @Nullable
    public Text getTitle() {
        return title;
    }

    public void setTitle(@Nullable Text title) {
        this.title = title;
        invalidateTextCache();
    }

    public int getHeaderHeight() {
        return headerHeight;
    }

    public void setHeaderHeight(int headerHeight) {
        this.headerHeight = headerHeight;
    }

    public <T extends ActivityComponent> T addChild(T child) {
        if (child != null && !this.children.contains(child)) {
            this.children.add(child);
        }
        return child;
    }

    public void removeChild(ActivityComponent child) {
        this.children.remove(child);
    }

    public void clearChildren() {
        this.children.clear();
    }

    public List<ActivityComponent> getChildren() {
        return children;
    }

    @Override
    public void onFontChanged() {
        invalidateTextCache();
        for (ActivityComponent child : this.children) {
            child.onFontChanged();
        }
    }

    @Override
    public void onLayoutResized(int parentWidth, int parentHeight) {
        invalidateTextCache();
        for (ActivityComponent child : this.children) {
            child.onLayoutResized(parentWidth, parentHeight);
        }
    }

    @Override
    protected void renderComponent(DrawContext context, int mouseX, int mouseY, float delta) {
        int currentBorder = this.borderColor;
        if (this.highlightTimer > 0.001f) {
            if (!AnimationClock.isAnimationsEnabled()) {
                this.highlightTimer = 0.0f;
            } else {
                float dt = AnimationClock.getDeltaTime();
                this.highlightTimer = Math.max(0.0f, this.highlightTimer - dt);
                float pulse = (float) Math.abs(Math.sin((1.5f - this.highlightTimer) * Math.PI * 2.5));
                currentBorder = ActivityColors.interpolateColor(this.borderColor, ActivityColors.ACCENT_PRIMARY, pulse * (this.highlightTimer / 1.5f));
            }
        }

        ActivityConfig config = ActivityConfigManager.getConfig();
        double panelOpacity = config != null ? config.panelOpacity : 65.0;
        boolean glassEffect = config == null || config.glassEffect;

        int effectiveBg = ActivityColors.scaleAlphaPercent(this.backgroundColor, panelOpacity * this.alpha);
        currentBorder = ActivityColors.scaleAlpha(currentBorder, this.alpha);

        // Draw panel background, 1px crisp border, and subtle glass highlights
        ActivityGuiRenderer.drawPanel(context, this.x, this.y, this.width, this.height, effectiveBg, currentBorder, glassEffect);

        // Draw header bar if title is present
        if (this.title != null) {
            int effectiveHeaderBg = ActivityColors.scaleAlphaPercent(ActivityColors.HEADER_BACKGROUND, panelOpacity * this.alpha);
            int dividerColor = ActivityColors.scaleAlpha(ActivityColors.BORDER_DIVIDER, this.alpha);
            ActivityGuiRenderer.fill(context, this.x + 1, this.y + 1, this.width - 2, this.headerHeight - 1, effectiveHeaderBg);
            ActivityGuiRenderer.drawHorizontalLine(context, this.x, this.y + this.headerHeight - 1, this.width, dividerColor);

            if (glassEffect) {
                float borderAlpha = (float) (currentBorder >>> 24) / 255.0f;
                ActivityGuiRenderer.drawGlassHighlight(context, this.x, this.y, this.width, this.headerHeight, borderAlpha);
            }

            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            int fontH = activity.client.gui.font.UiTextRenderer.getFontHeight(tr);
            int textY = this.y + (this.headerHeight - fontH) / 2;
            int maxTitleW = Math.max(10, this.width - ActivityMetrics.PADDING_PANEL * 2);

            Text display;
            if (this.cachedDisplayTitle != null && this.lastMaxTitleW == maxTitleW) {
                display = this.cachedDisplayTitle;
            } else {
                int fullW = activity.client.gui.font.UiTextRenderer.getWidth(tr, this.title);
                if (maxTitleW > 8 && fullW > maxTitleW) {
                    int ellW = activity.client.gui.font.UiTextRenderer.getWidth(tr, "…");
                    int targetW = Math.max(0, maxTitleW - ellW);
                    String trimmed = activity.client.gui.font.UiTextRenderer.trimToWidth(tr, this.title.getString(), targetW) + "…";
                    if (activity.client.gui.font.UiTextRenderer.getWidth(tr, trimmed) > maxTitleW) {
                        trimmed = activity.client.gui.font.UiTextRenderer.trimToWidth(tr, trimmed, Math.max(0, maxTitleW));
                    }
                    this.cachedDisplayTitle = Text.literal(trimmed);
                } else {
                    this.cachedDisplayTitle = this.title;
                }
                this.lastMaxTitleW = maxTitleW;
                display = this.cachedDisplayTitle;
            }
            int titleColor = ActivityColors.scaleAlpha(ActivityColors.TEXT_PRIMARY, this.alpha);
            activity.client.gui.font.UiTextRenderer.drawTextWithShadow(context, tr, display, this.x + ActivityMetrics.PADDING_PANEL, textY, titleColor);
        }

        // Render children
        for (ActivityComponent child : this.children) {
            if (child.isVisible()) {
                child.setAlpha(this.alpha);
                child.render(context, mouseX, mouseY, delta);
            }
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (!this.visible || !this.enabled) return false;

        for (int i = this.children.size() - 1; i >= 0; i--) {
            ActivityComponent child = this.children.get(i);
            if (child.isVisible() && child.isEnabled() && child.isMouseOver(click.x(), click.y())) {
                if (child.mouseClicked(click, doubled)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (!this.visible || !this.enabled) return false;

        for (int i = this.children.size() - 1; i >= 0; i--) {
            ActivityComponent child = this.children.get(i);
            if (child.isVisible() && child.mouseReleased(click)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (!this.visible || !this.enabled) return false;

        for (int i = this.children.size() - 1; i >= 0; i--) {
            ActivityComponent child = this.children.get(i);
            if (child.isVisible() && child.mouseDragged(click, deltaX, deltaY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!this.visible || !this.enabled) return false;

        for (int i = this.children.size() - 1; i >= 0; i--) {
            ActivityComponent child = this.children.get(i);
            if (child.isVisible() && child.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (!this.visible || !this.enabled) return false;

        for (int i = this.children.size() - 1; i >= 0; i--) {
            ActivityComponent child = this.children.get(i);
            if (child.isVisible() && child.isEnabled() && child.keyPressed(input)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyReleased(KeyInput input) {
        if (!this.visible || !this.enabled) return false;

        for (int i = this.children.size() - 1; i >= 0; i--) {
            ActivityComponent child = this.children.get(i);
            if (child.isVisible() && child.keyReleased(input)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (!this.visible || !this.enabled) return false;

        for (int i = this.children.size() - 1; i >= 0; i--) {
            ActivityComponent child = this.children.get(i);
            if (child.isVisible() && child.isEnabled() && child.charTyped(input)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void tick() {
        for (ActivityComponent child : this.children) {
            child.tick();
        }
    }
}

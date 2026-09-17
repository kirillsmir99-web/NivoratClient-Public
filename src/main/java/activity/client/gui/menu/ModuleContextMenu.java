package activity.client.gui.menu;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.ActivityScreen;
import activity.client.gui.animation.AnimationClock;
import activity.client.gui.font.FontManager;
import activity.client.gui.icon.ActivityIcon;
import activity.client.gui.icon.ActivityIconRenderer;
import activity.client.gui.overlay.Overlay;
import activity.client.gui.render.ActivityGuiRenderer;
import activity.client.gui.sound.SoundManager;
import activity.client.gui.theme.ActivityColors;
import activity.client.gui.theme.ActivityMetrics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Compact acrylic glass context menu opened upon right-clicking a module.
 *
 * <p>Actions:
 * <ul>
 *   <li>Pin / Unpin ("Закрепить" / "Открепить")</li>
 *   <li>About Module ("О модификации")</li>
 * </ul>
 *
 * <p>UX Features:
 * <ul>
 *   <li>~100ms smooth scale & alpha open animation.</li>
 *   <li>Screen edge aware clamping to prevent clipping.</li>
 *   <li>Outside click dismiss with click-through protection (via {@link Overlay}).</li>
 *   <li>Escape key closes menu before closing screen.</li>
 * </ul>
 */
public class ModuleContextMenu implements Overlay {

    public static final int MENU_WIDTH = 135;
    public static final int ITEM_HEIGHT = 20;
    public static final int PADDING = 3;
    public static final int MENU_HEIGHT = PADDING * 2 + ITEM_HEIGHT * 2 + 1;

    private final ActivityScreen screen;
    private final String moduleId;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int itemHeight;

    private boolean closed = false;
    private float openProgress = 0.0f;
    private int hoveredIndex = -1;

    public ModuleContextMenu(ActivityScreen screen, String moduleId, double mouseX, double mouseY) {
        this.screen = screen;
        this.moduleId = moduleId;

        MinecraftClient mc = MinecraftClient.getInstance();
        int screenW = mc != null && mc.getWindow() != null ? mc.getWindow().getScaledWidth() : 400;
        int screenH = mc != null && mc.getWindow() != null ? mc.getWindow().getScaledHeight() : 300;

        this.itemHeight = (screenW < 560 || screenH < 360) ? ActivityMetrics.TOUCH_TARGET_MIN_SIZE : ITEM_HEIGHT;
        this.width = (screenW < 560 || screenH < 360) ? 140 : MENU_WIDTH;
        this.height = PADDING * 2 + this.itemHeight * 2 + 1;

        // Screen-edge aware clamping
        int targetX = (int) Math.round(mouseX);
        int targetY = (int) Math.round(mouseY);

        if (targetX + this.width > screenW - 4) {
            targetX = Math.max(4, screenW - this.width - 4);
        } else {
            targetX = Math.max(4, targetX);
        }

        if (targetY + this.height > screenH - 4) {
            targetY = Math.max(4, targetY - this.height);
        } else {
            targetY = Math.max(4, targetY);
        }

        this.x = targetX;
        this.y = targetY;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getModuleId() {
        return moduleId;
    }

    public float getOpenProgress() {
        return openProgress;
    }

    @Override
    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX < this.x + this.width &&
               mouseY >= this.y && mouseY < this.y + this.height;
    }

    @Override
    public boolean shouldCloseOnClickOutside() {
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void close() {
        this.closed = true;
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.closed) return;

        float dt = AnimationClock.getDeltaTime();
        float target = 1.0f;
        if (!AnimationClock.isAnimationsEnabled()) {
            this.openProgress = target;
        } else {
            this.openProgress = AnimationClock.approach(this.openProgress, target, 0.10f, dt);
        }

        float eased = AnimationClock.easeOutCubic(this.openProgress);
        if (eased <= 0.01f) return;

        ActivityConfig config = ActivityConfigManager.getConfig();
        boolean isPinned = config != null && config.isPinned(this.moduleId);

        // Update hovered index
        this.hoveredIndex = -1;
        int item0Y = this.y + PADDING;
        int item1Y = item0Y + this.itemHeight + 1;

        if (mouseX >= this.x + PADDING && mouseX < this.x + this.width - PADDING) {
            if (mouseY >= item0Y && mouseY < item0Y + this.itemHeight) {
                this.hoveredIndex = 0;
            } else if (mouseY >= item1Y && mouseY < item1Y + this.itemHeight) {
                this.hoveredIndex = 1;
            }
        }

        int bgColor = ActivityColors.scaleAlpha(ActivityColors.WINDOW_BACKGROUND, eased);
        int borderColor = ActivityColors.scaleAlpha(ActivityColors.BORDER_HOVER, eased);

        // Drop shadow & glass panel
        ActivityGuiRenderer.fill(context, this.x - 1, this.y - 1, this.width + 2, this.height + 2, ActivityColors.scaleAlpha(0x40000000, eased));
        ActivityGuiRenderer.drawPanel(context, this.x, this.y, this.width, this.height, bgColor, borderColor, true);

        // 1px separator between items
        int sepY = item0Y + this.itemHeight;
        ActivityGuiRenderer.drawHorizontalLine(context, this.x + PADDING, sepY, this.width - PADDING * 2, ActivityColors.scaleAlpha(ActivityColors.BORDER_DIVIDER, eased));

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer textRenderer = mc != null ? mc.textRenderer : null;

        // Item 0: Pin / Unpin
        renderItem(
            context,
            textRenderer,
            this.x + PADDING,
            item0Y,
            this.width - PADDING * 2,
            this.itemHeight,
            ActivityIcon.PIN,
            Text.translatable(isPinned ? "activity.menu.unpin" : "activity.menu.pin"),
            this.hoveredIndex == 0,
            isPinned,
            eased
        );

        // Item 1: About Module
        renderItem(
            context,
            textRenderer,
            this.x + PADDING,
            item1Y,
            this.width - PADDING * 2,
            this.itemHeight,
            ActivityIcon.INFO,
            Text.translatable("activity.menu.about_module"),
            this.hoveredIndex == 1,
            false,
            eased
        );
    }

    private void renderItem(
        DrawContext context,
        TextRenderer textRenderer,
        int itemX,
        int itemY,
        int itemW,
        int itemH,
        ActivityIcon icon,
        Text label,
        boolean hovered,
        boolean accentColor,
        float alphaFactor
    ) {
        if (hovered) {
            int hoverBg = ActivityColors.scaleAlpha(ActivityColors.ITEM_HOVER_BG, alphaFactor);
            ActivityGuiRenderer.fill(context, itemX, itemY, itemW, itemH, hoverBg);
        }

        int textColor;
        if (accentColor) {
            textColor = ActivityColors.TEXT_ACCENT;
        } else if (hovered) {
            textColor = ActivityColors.TEXT_PRIMARY;
        } else {
            textColor = ActivityColors.TEXT_SECONDARY;
        }
        textColor = ActivityColors.scaleAlpha(textColor, alphaFactor);

        // Icon
        if (icon != null) {
            int iconY = itemY + (itemH - icon.getHeight()) / 2;
            ActivityIconRenderer.draw(context, icon, itemX + 4, iconY, textColor);
        }

        // Label
        if (textRenderer != null) {
            int textX = itemX + (icon != null ? icon.getWidth() + 8 : 4);
            int textY = itemY + (itemH - textRenderer.fontHeight) / 2;
            Text wrapped = FontManager.wrap(label);
            context.drawTextWithShadow(textRenderer, wrapped, textX, textY, textColor);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (this.closed) return false;

        if (!contains(click.x(), click.y())) {
            close();
            return true;
        }

        int item0Y = this.y + PADDING;
        int item1Y = item0Y + this.itemHeight + 1;

        if (click.x() >= this.x + PADDING && click.x() < this.x + this.width - PADDING) {
            if (click.y() >= item0Y && click.y() < item0Y + this.itemHeight) {
                // Item 0: Pin / Unpin
                ActivityConfig config = ActivityConfigManager.getConfig();
                if (config != null) {
                    boolean wasPinned = config.isPinned(this.moduleId);
                    config.setPinned(this.moduleId, !wasPinned);
                    ActivityConfigManager.save();
                }
                SoundManager.playClick();
                close();
                return true;
            } else if (click.y() >= item1Y && click.y() < item1Y + this.itemHeight) {
                // Item 1: About Module
                SoundManager.playClick();
                close();
                if (this.screen != null) {
                    this.screen.openAboutModuleSheet(this.moduleId);
                }
                return true;
            }
        }

        return true;
    }
}

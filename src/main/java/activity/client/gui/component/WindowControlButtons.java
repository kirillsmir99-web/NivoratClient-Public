package activity.client.gui.component;

import activity.client.gui.animation.AnimationClock;
import activity.client.gui.font.FontManager;
import activity.client.gui.icon.ActivityIcon;
import activity.client.gui.icon.ActivityIconRenderer;
import activity.client.gui.layout.WindowLayout;
import activity.client.gui.render.ActivityGuiRenderer;
import activity.client.gui.theme.ActivityColors;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import java.util.function.BooleanSupplier;

public class WindowControlButtons {

    public static final int BTN_SIZE = 16;
    public static final int BTN_GAP = 4;
    public static final int TOTAL_WIDTH = BTN_SIZE * 4 + BTN_GAP * 3;

    private static final Text TOOLTIP_TOGGLE_DISABLE = Text.translatable("activity.button.disable_all_modules");
    private static final Text TOOLTIP_TOGGLE_ENABLE = Text.translatable("activity.button.enable_all_modules");
    private static final Text TOOLTIP_RELOAD = Text.translatable("activity.control.reload");
    private static final Text TOOLTIP_MAXIMIZE = Text.translatable("activity.control.maximize");
    private static final Text TOOLTIP_RESTORE = Text.translatable("activity.control.restore");
    private static final Text TOOLTIP_CLOSE = Text.translatable("activity.control.close");

    private final Runnable onToggleAll;
    private final Runnable onRefresh;
    private final Runnable onToggleMaximize;
    private final Runnable onClose;
    private final BooleanSupplier isMaximizedSupplier;

    private int hoveredButton = -1;
    private int lastHoveredButton = -1;
    private int pressedButton = -1;
    private final float[] hoverProgress = new float[4];

    public WindowControlButtons(Runnable onToggleAll, Runnable onRefresh, Runnable onToggleMaximize, Runnable onClose, BooleanSupplier isMaximizedSupplier) {
        this.onToggleAll = onToggleAll;
        this.onRefresh = onRefresh;
        this.onToggleMaximize = onToggleMaximize;
        this.onClose = onClose;
        this.isMaximizedSupplier = isMaximizedSupplier;
    }

    public WindowControlButtons(Runnable onRefresh, Runnable onToggleMaximize, Runnable onClose, BooleanSupplier isMaximizedSupplier) {
        this(null, onRefresh, onToggleMaximize, onClose, isMaximizedSupplier);
    }

    public WindowControlButtons(Runnable onRecenter, Runnable onReload, Runnable onClose) {
        this(null, onReload, onRecenter, onClose, () -> false);
    }

    public int getStartX(WindowLayout layout) {
        return layout.headerX + layout.headerWidth - TOTAL_WIDTH - 6;
    }

    public int getStartY(WindowLayout layout) {
        return layout.headerY + (layout.headerHeight - BTN_SIZE) / 2;
    }

    public boolean isMouseOver(double mouseX, double mouseY, WindowLayout layout) {
        int startX = getStartX(layout);
        int startY = getStartY(layout);
        int padY = Math.max(4, (layout.headerHeight - BTN_SIZE) / 2);
        int padX = (layout.isCompact() || layout.isSmallScreen()) ? activity.client.gui.theme.ActivityMetrics.TOUCH_HITBOX_PADDING : 0;
        return mouseX >= startX - padX && mouseX < startX + TOTAL_WIDTH + padX &&
               mouseY >= startY - padY && mouseY < startY + BTN_SIZE + padY;
    }

    public int getHoveredButton() {
        return hoveredButton;
    }

    public int getPressedButton() {
        return pressedButton;
    }

    public void clearPressed() {
        this.pressedButton = -1;
    }

    public void render(DrawContext context, int mouseX, int mouseY, WindowLayout layout) {
        render(context, mouseX, mouseY, layout, 1.0f);
    }

    public void render(DrawContext context, int mouseX, int mouseY, WindowLayout layout, float alphaFactor) {
        int startX = getStartX(layout);
        int startY = getStartY(layout);
        int padY = Math.max(4, (layout.headerHeight - BTN_SIZE) / 2);
        int padX = (layout.isCompact() || layout.isSmallScreen()) ? activity.client.gui.theme.ActivityMetrics.TOUCH_HITBOX_PADDING : 0;

        this.hoveredButton = -1;

        boolean maximized = this.isMaximizedSupplier != null && this.isMaximizedSupplier.getAsBoolean();
        ActivityIcon maxRestoreIcon = maximized ? ActivityIcon.RESTORE : ActivityIcon.MAXIMIZE;
        boolean anyModuleEnabled = activity.client.module.api.ModuleRegistry.isAnyModuleEnabled();
        ActivityIcon toggleIcon = anyModuleEnabled ? ActivityIcon.ENABLED : ActivityIcon.DISABLED;
        ActivityIcon[] icons = {toggleIcon, ActivityIcon.REFRESH, maxRestoreIcon, ActivityIcon.CLOSE};

        for (int i = 0; i < 4; i++) {
            int bx = startX + i * (BTN_SIZE + BTN_GAP);
            int by = startY;

            int hitLeft = (i == 0) ? (startX - padX) : bx - BTN_GAP / 2;
            int hitRight = (i == 3) ? (startX + TOTAL_WIDTH + padX) : bx + BTN_SIZE + BTN_GAP / 2;
            boolean isHovered = (mouseX >= hitLeft && mouseX < hitRight &&
                                 mouseY >= by - padY && mouseY < by + BTN_SIZE + padY);
            if (isHovered) {
                this.hoveredButton = i;
            }

            boolean isPressed = (this.pressedButton == i && isHovered);

            float targetHover = isHovered ? 1.0f : 0.0f;
            this.hoverProgress[i] = AnimationClock.approach(this.hoverProgress[i], targetHover, 0.12f);

            int iconY = isPressed ? by + 1 : by;

            if (i == 3) {

                if (isPressed) {
                    int bg = ActivityColors.scaleAlpha(0x65C93B3D, alphaFactor);
                    int border = ActivityColors.scaleAlpha(0xFFC93B3D, alphaFactor);
                    ActivityGuiRenderer.drawPanel(context, bx, by, BTN_SIZE, BTN_SIZE, bg, border, false);
                    ActivityIconRenderer.drawCentered(context, icons[i], bx, iconY, BTN_SIZE, BTN_SIZE, ActivityColors.scaleAlpha(0xFFFFFFFF, alphaFactor));
                } else if (this.hoverProgress[i] > 0.01f) {
                    int bgAlpha = (int) (0x40 * this.hoverProgress[i]);
                    int borderAlpha = (int) (0x80 * this.hoverProgress[i]);
                    int bg = ActivityColors.scaleAlpha((bgAlpha << 24) | 0x00C93B3D, alphaFactor);
                    int border = ActivityColors.scaleAlpha((borderAlpha << 24) | 0x00C93B3D, alphaFactor);
                    ActivityGuiRenderer.drawPanel(context, bx, by, BTN_SIZE, BTN_SIZE, bg, border, false);
                    int iconCol = ActivityColors.interpolateColor(ActivityColors.TEXT_SECONDARY, 0xFFFFAAAA, this.hoverProgress[i]);
                    ActivityIconRenderer.drawCentered(context, icons[i], bx, iconY, BTN_SIZE, BTN_SIZE, ActivityColors.scaleAlpha(iconCol, alphaFactor));
                } else {
                    ActivityIconRenderer.drawCentered(context, icons[i], bx, iconY, BTN_SIZE, BTN_SIZE, ActivityColors.scaleAlpha(ActivityColors.TEXT_SECONDARY, alphaFactor));
                }
            } else if (i == 0) {

                int highlightColor = anyModuleEnabled ? ActivityColors.SUCCESS : ActivityColors.WARNING;
                if (isPressed) {
                    int bg = ActivityColors.scaleAlpha((0x45 << 24) | (highlightColor & 0x00FFFFFF), alphaFactor);
                    int border = ActivityColors.scaleAlpha((0x90 << 24) | (highlightColor & 0x00FFFFFF), alphaFactor);
                    ActivityGuiRenderer.drawPanel(context, bx, by, BTN_SIZE, BTN_SIZE, bg, border, false);
                    ActivityIconRenderer.drawCentered(context, icons[i], bx, iconY, BTN_SIZE, BTN_SIZE, ActivityColors.scaleAlpha(0xFFFFFFFF, alphaFactor));
                } else if (this.hoverProgress[i] > 0.01f) {
                    int bgAlpha = (int) (0x28 * this.hoverProgress[i]);
                    int borderAlpha = (int) (0x60 * this.hoverProgress[i]);
                    int bg = ActivityColors.scaleAlpha((bgAlpha << 24) | (highlightColor & 0x00FFFFFF), alphaFactor);
                    int border = ActivityColors.scaleAlpha((borderAlpha << 24) | (highlightColor & 0x00FFFFFF), alphaFactor);
                    ActivityGuiRenderer.drawPanel(context, bx, by, BTN_SIZE, BTN_SIZE, bg, border, false);
                    int iconCol = ActivityColors.interpolateColor(ActivityColors.TEXT_SECONDARY, highlightColor, this.hoverProgress[i]);
                    ActivityIconRenderer.drawCentered(context, icons[i], bx, iconY, BTN_SIZE, BTN_SIZE, ActivityColors.scaleAlpha(iconCol, alphaFactor));
                } else {
                    int defaultIconCol = anyModuleEnabled ? ActivityColors.SUCCESS : ActivityColors.TEXT_SECONDARY;
                    ActivityIconRenderer.drawCentered(context, icons[i], bx, iconY, BTN_SIZE, BTN_SIZE, ActivityColors.scaleAlpha(defaultIconCol, alphaFactor));
                }
            } else {

                if (isPressed) {
                    int bg = ActivityColors.scaleAlpha(0x45FFFFFF, alphaFactor);
                    int border = ActivityColors.scaleAlpha(0x90FFFFFF, alphaFactor);
                    ActivityGuiRenderer.drawPanel(context, bx, by, BTN_SIZE, BTN_SIZE, bg, border, false);
                    ActivityIconRenderer.drawCentered(context, icons[i], bx, iconY, BTN_SIZE, BTN_SIZE, ActivityColors.scaleAlpha(0xFFFFFFFF, alphaFactor));
                } else if (this.hoverProgress[i] > 0.01f) {
                    int bgAlpha = (int) (0x22 * this.hoverProgress[i]);
                    int borderAlpha = (int) (0x45 * this.hoverProgress[i]);
                    int bg = ActivityColors.scaleAlpha((bgAlpha << 24) | 0x00FFFFFF, alphaFactor);
                    int border = ActivityColors.scaleAlpha((borderAlpha << 24) | 0x00FFFFFF, alphaFactor);
                    ActivityGuiRenderer.drawPanel(context, bx, by, BTN_SIZE, BTN_SIZE, bg, border, false);
                    int iconCol = ActivityColors.interpolateColor(ActivityColors.TEXT_SECONDARY, ActivityColors.TEXT_PRIMARY, this.hoverProgress[i]);
                    ActivityIconRenderer.drawCentered(context, icons[i], bx, iconY, BTN_SIZE, BTN_SIZE, ActivityColors.scaleAlpha(iconCol, alphaFactor));
                } else {
                    ActivityIconRenderer.drawCentered(context, icons[i], bx, iconY, BTN_SIZE, BTN_SIZE, ActivityColors.scaleAlpha(ActivityColors.TEXT_SECONDARY, alphaFactor));
                }
            }
        }

        if (this.hoveredButton != -1 && this.hoveredButton != this.lastHoveredButton) {
            activity.client.gui.sound.SoundManager.playHover();
        }
        this.lastHoveredButton = this.hoveredButton;
    }

    public void renderTooltips(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY) {
        if (this.pressedButton != -1) return;
        if (this.hoveredButton == 0) {
            boolean anyModuleEnabled = activity.client.module.api.ModuleRegistry.isAnyModuleEnabled();
            Text tooltip = anyModuleEnabled ? TOOLTIP_TOGGLE_DISABLE : TOOLTIP_TOGGLE_ENABLE;
            context.drawTooltip(textRenderer, FontManager.wrap(tooltip), mouseX, mouseY);
        } else if (this.hoveredButton == 1) {
            context.drawTooltip(textRenderer, FontManager.wrap(TOOLTIP_RELOAD), mouseX, mouseY);
        } else if (this.hoveredButton == 2) {
            boolean maximized = this.isMaximizedSupplier != null && this.isMaximizedSupplier.getAsBoolean();
            Text tooltip = maximized ? TOOLTIP_RESTORE : TOOLTIP_MAXIMIZE;
            context.drawTooltip(textRenderer, FontManager.wrap(tooltip), mouseX, mouseY);
        } else if (this.hoveredButton == 3) {
            context.drawTooltip(textRenderer, FontManager.wrap(TOOLTIP_CLOSE), mouseX, mouseY);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, WindowLayout layout) {
        if (button != 0 || !isMouseOver(mouseX, mouseY, layout)) return false;

        int startX = getStartX(layout);
        int startY = getStartY(layout);
        int padY = Math.max(4, (layout.headerHeight - BTN_SIZE) / 2);
        int padX = (layout.isCompact() || layout.isSmallScreen()) ? activity.client.gui.theme.ActivityMetrics.TOUCH_HITBOX_PADDING : 0;

        for (int i = 0; i < 4; i++) {
            int bx = startX + i * (BTN_SIZE + BTN_GAP);
            int hitLeft = (i == 0) ? (startX - padX) : bx - BTN_GAP / 2;
            int hitRight = (i == 3) ? (startX + TOTAL_WIDTH + padX) : bx + BTN_SIZE + BTN_GAP / 2;

            if (mouseX >= hitLeft && mouseX < hitRight && mouseY >= startY - padY && mouseY < startY + BTN_SIZE + padY) {
                this.pressedButton = i;
                return true;
            }
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button, WindowLayout layout) {
        if (button != 0 || this.pressedButton == -1) return false;

        int pressed = this.pressedButton;
        this.pressedButton = -1;

        int startX = getStartX(layout);
        int startY = getStartY(layout);
        int padY = Math.max(4, (layout.headerHeight - BTN_SIZE) / 2);
        int padX = (layout.isCompact() || layout.isSmallScreen()) ? activity.client.gui.theme.ActivityMetrics.TOUCH_HITBOX_PADDING : 0;

        int bx = startX + pressed * (BTN_SIZE + BTN_GAP);
        int hitLeft = (pressed == 0) ? (startX - padX) : bx - BTN_GAP / 2;
        int hitRight = (pressed == 3) ? (startX + TOTAL_WIDTH + padX) : bx + BTN_SIZE + BTN_GAP / 2;

        if (mouseX >= hitLeft && mouseX < hitRight && mouseY >= startY - padY && mouseY < startY + BTN_SIZE + padY) {
            if (pressed == 0) {
                if (this.onToggleAll != null) {
                    this.onToggleAll.run();
                } else {
                    boolean target = !activity.client.module.api.ModuleRegistry.isAnyModuleEnabled();
                    activity.client.module.api.ModuleRegistry.setAllEnabled(target);
                    activity.client.gui.sound.SoundManager.playToggle(target);
                }
            } else if (pressed == 1 && this.onRefresh != null) {
                activity.client.gui.sound.SoundManager.playReload();
                this.onRefresh.run();
            } else if (pressed == 2 && this.onToggleMaximize != null) {
                boolean maximized = this.isMaximizedSupplier != null && this.isMaximizedSupplier.getAsBoolean();
                if (maximized) {
                    activity.client.gui.sound.SoundManager.playRestore();
                } else {
                    activity.client.gui.sound.SoundManager.playMaximize();
                }
                this.onToggleMaximize.run();
            } else if (pressed == 3 && this.onClose != null) {
                activity.client.gui.sound.SoundManager.playClose();
                this.onClose.run();
            }
            return true;
        }
        return false;
    }

    public void triggerAction(int index) {
        if (index == 0) {
            if (this.onToggleAll != null) {
                this.onToggleAll.run();
            } else {
                boolean target = !activity.client.module.api.ModuleRegistry.isAnyModuleEnabled();
                activity.client.module.api.ModuleRegistry.setAllEnabled(target);
                activity.client.gui.sound.SoundManager.playToggle(target);
            }
        } else if (index == 1 && this.onRefresh != null) {
            this.onRefresh.run();
        } else if (index == 2 && this.onToggleMaximize != null) {
            this.onToggleMaximize.run();
        } else if (index == 3 && this.onClose != null) {
            this.onClose.run();
        }
    }
}

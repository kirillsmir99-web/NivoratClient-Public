package activity.client.gui.hud;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.font.UiTextRenderer;
import activity.client.gui.render.ActivityGuiRenderer;
import activity.client.gui.sound.SoundManager;
import activity.client.gui.theme.ActivityColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class NivoratHudEditorScreen extends Screen {

    private final Screen parent;
    private boolean isDragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    private int panelX = -1;
    private int panelY = -1;
    private static final int PANEL_W = 140;
    private static final int PANEL_H = 114;
    private boolean isPanelDragging = false;
    private int panelDragOffsetX = 0;
    private int panelDragOffsetY = 0;

    private int btnResetX, btnResetY, btnResetW, btnResetH;
    private int btnDoneX, btnDoneY, btnDoneW, btnDoneH;
    private int lastHoveredBtn = -1;

    public NivoratHudEditorScreen(Screen parent) {
        super(Text.literal("HUD NivoratClient • Настройка позиции"));
        this.parent = parent;
    }

    public NivoratHudEditorScreen() {
        this(null);
    }

    @Override
    public void close() {
        isDragging = false;
        isPanelDragging = false;
        SoundManager.playClose();
        ActivityConfigManager.markDirty();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        } else {
            super.close();
        }
    }

    @Override
    protected void init() {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
            if (this.client != null) this.client.setScreen(null);
            else super.close();
            return;
        }
        isDragging = false;
        isPanelDragging = false;
        this.clearChildren();
        if (panelX < 0 || panelY < 0) {
            panelX = 16;
            panelY = Math.max(16, (height - PANEL_H) / 2);
        }
        SoundManager.playOpen();
    }

    private void nudge(int dx, int dy) {
        ActivityConfig config = ActivityConfigManager.getConfig();
        if (config == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        int totalW = ActivityHudOverlay.getTotalWidth(mc, config);
        int totalH = ActivityHudOverlay.getTotalHeight(mc, config);
        int curX = ActivityHudOverlay.getEffectiveX(config, width, totalW);
        int curY = ActivityHudOverlay.getEffectiveY(config, height, totalH);
        int newX = Math.max(2, Math.min(width - totalW - 2, curX + dx));
        int newY = Math.max(2, Math.min(height - totalH - 2, curY + dy));
        config.hudCustomX = newX;
        config.hudCustomY = newY;
        ActivityConfigManager.markDirty();
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        int step = input.hasShift() ? 5 : 1;

        if (key == GLFW.GLFW_KEY_LEFT) {
            nudge(-step, 0);
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT) {
            nudge(step, 0);
            return true;
        }
        if (key == GLFW.GLFW_KEY_UP) {
            nudge(0, -step);
            return true;
        }
        if (key == GLFW.GLFW_KEY_DOWN) {
            nudge(0, step);
            return true;
        }
        if (key == GLFW.GLFW_KEY_R) {
            ActivityConfig c = ActivityConfigManager.getConfig();
            if (c != null) {
                c.hudCustomX = -1;
                c.hudCustomY = -1;
                ActivityConfigManager.markDirty();
            }
            SoundManager.playSelect();
            return true;
        }
        if (input.isEscape()) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x();
        double my = click.y();
        int button = click.buttonInfo().button();

        btnResetW = PANEL_W - 20;
        btnResetH = 20;
        btnResetX = panelX + 10;
        btnResetY = panelY + 52;

        btnDoneW = PANEL_W - 20;
        btnDoneH = 22;
        btnDoneX = panelX + 10;
        btnDoneY = panelY + 78;

        ActivityConfig config = ActivityConfigManager.getConfig();

        if (button == 0) {
            if (mx >= btnResetX && mx <= btnResetX + btnResetW && my >= btnResetY && my <= btnResetY + btnResetH) {
                if (config != null) {
                    config.hudCustomX = -1;
                    config.hudCustomY = -1;
                    ActivityConfigManager.markDirty();
                }
                SoundManager.playSelect();
                return true;
            }
            if (mx >= btnDoneX && mx <= btnDoneX + btnDoneW && my >= btnDoneY && my <= btnDoneY + btnDoneH) {
                SoundManager.playClick();
                close();
                return true;
            }
            if (mx >= panelX && mx <= panelX + PANEL_W && my >= panelY && my <= panelY + PANEL_H) {
                isPanelDragging = true;
                panelDragOffsetX = (int) Math.round(mx - panelX);
                panelDragOffsetY = (int) Math.round(my - panelY);
                return true;
            }
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        int totalW = ActivityHudOverlay.getTotalWidth(mc, config);
        int totalH = ActivityHudOverlay.getTotalHeight(mc, config);
        int curX = ActivityHudOverlay.getEffectiveX(config, width, totalW);
        int curY = ActivityHudOverlay.getEffectiveY(config, height, totalH);

        boolean inside = mx >= curX - 8 && mx <= curX + totalW + 8 && my >= curY - 8 && my <= curY + totalH + 8;

        if (button == 0 && inside) {
            isDragging = true;
            dragOffsetX = (int) Math.round(mx - curX);
            dragOffsetY = (int) Math.round(my - curY);
            SoundManager.playClick();
            return true;
        } else if (button == 1 && inside) {
            if (config != null) {
                config.hudCustomX = -1;
                config.hudCustomY = -1;
                ActivityConfigManager.markDirty();
            }
            SoundManager.playSelect();
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.buttonInfo().button() == 0) {
            if (isPanelDragging) {
                isPanelDragging = false;
                return true;
            }
            if (isDragging) {
                isDragging = false;
                ActivityConfigManager.markDirty();
                return true;
            }
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (isPanelDragging) {
            int newPX = (int) Math.round(click.x() - panelDragOffsetX);
            int newPY = (int) Math.round(click.y() - panelDragOffsetY);
            panelX = Math.max(2, Math.min(width - PANEL_W - 2, newPX));
            panelY = Math.max(2, Math.min(height - PANEL_H - 2, newPY));
            return true;
        }
        if (isDragging) {
            ActivityConfig config = ActivityConfigManager.getConfig();
            MinecraftClient mc = MinecraftClient.getInstance();
            int totalW = ActivityHudOverlay.getTotalWidth(mc, config);
            int totalH = ActivityHudOverlay.getTotalHeight(mc, config);
            int newX = (int) Math.round(click.x() - dragOffsetX);
            int newY = (int) Math.round(click.y() - dragOffsetY);
            if (config != null) {
                config.hudCustomX = Math.max(2, Math.min(width - totalW - 2, newX));
                config.hudCustomY = Math.max(2, Math.min(height - totalH - 2, newY));
                ActivityConfigManager.markDirty();
            }
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        ActivityConfig config = ActivityConfigManager.getConfig();
        MinecraftClient mc = MinecraftClient.getInstance();

        int totalW = ActivityHudOverlay.getTotalWidth(mc, config);
        int totalH = ActivityHudOverlay.getTotalHeight(mc, config);
        int currentX = ActivityHudOverlay.getEffectiveX(config, width, totalW);
        int currentY = ActivityHudOverlay.getEffectiveY(config, height, totalH);

        ActivityGuiRenderer.fill(context, 0, 0, width, height, ActivityColors.BACKGROUND_OVERLAY);

        if (isDragging) {
            if (Math.abs(currentX - 6) <= 2) {
                ActivityGuiRenderer.drawVerticalLine(context, 6, 0, height, 0x403EA4E8);
            }
            if (Math.abs(currentX + totalW - (width - 6)) <= 2) {
                ActivityGuiRenderer.drawVerticalLine(context, width - 6, 0, height, 0x403EA4E8);
            }
            if (Math.abs(currentY - 6) <= 2) {
                ActivityGuiRenderer.drawHorizontalLine(context, 0, 6, width, 0x403EA4E8);
            }
            if (Math.abs(currentY + totalH - (height - 6)) <= 2) {
                ActivityGuiRenderer.drawHorizontalLine(context, 0, height - 6, width, 0x403EA4E8);
            }
        }

        boolean isHovered = mouseX >= currentX - 8 && mouseX <= currentX + totalW + 8 && mouseY >= currentY - 8 && mouseY <= currentY + totalH + 8;

        if (mc != null) {
            ActivityHudOverlay.renderHud(context, mc, config, currentX, currentY, totalW, width);
        }

        int boxBorder = (isDragging || isHovered) ? 0xCC3EA4E8 : 0x443EA4E8;
        ActivityGuiRenderer.drawBorder(context, currentX - 3, currentY - 3, totalW + 6, totalH + 6, boxBorder);

        if (isHovered || isDragging) {
            int chipW = 100;
            int chipH = 15;
            int chipX = currentX + (totalW - chipW) / 2;
            chipX = Math.max(4, Math.min(width - chipW - 4, chipX));
            int chipY = (currentY - chipH - 8 >= 4) ? (currentY - chipH - 8) : (currentY + totalH + 8);

            ActivityGuiRenderer.drawPanel(context, chipX, chipY, chipW, chipH, ActivityColors.PANEL_INNER_BG, ActivityColors.BORDER, true);
            ActivityGuiRenderer.fill(context, chipX + 5, chipY + 5, 4, 4, ActivityColors.ACCENT_PRIMARY);
            if (textRenderer != null) {
                UiTextRenderer.drawTextWithShadow(context, textRenderer, Text.literal("X: " + currentX + "  Y: " + currentY), chipX + 13, chipY + 4, ActivityColors.TEXT_PRIMARY);
            }
        }

        ActivityGuiRenderer.drawWindowFrame(context, panelX, panelY, PANEL_W, PANEL_H, ActivityColors.WINDOW_BACKGROUND, ActivityColors.BORDER, true);
        ActivityGuiRenderer.fill(context, panelX + 1, panelY + 1, PANEL_W - 2, 28, ActivityColors.HEADER_BACKGROUND);
        ActivityGuiRenderer.drawGlassHighlight(context, panelX, panelY, PANEL_W, PANEL_H, 1.0f);

        if (textRenderer != null) {
            ActivityGuiRenderer.fill(context, panelX + 8, panelY + 8, 5, 5, ActivityColors.ACCENT_PRIMARY);
            UiTextRenderer.drawTextWithShadow(context, textRenderer, Text.literal("NIVORAT HUD"), panelX + 18, panelY + 7, ActivityColors.TEXT_PRIMARY);
            UiTextRenderer.drawTextWithShadow(context, textRenderer, Text.literal("Настройка HUD"), panelX + 18, panelY + 18, ActivityColors.TEXT_MUTED);

            ActivityGuiRenderer.drawHorizontalLine(context, panelX + 6, panelY + 31, PANEL_W - 12, 0x44353B49);

            String posStr = (config != null && config.hudCustomX < 0 && config.hudCustomY < 0) ? "АВТО-ПОЗИЦИЯ" : ("X: " + currentX + " | Y: " + currentY);
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal(posStr), panelX + PANEL_W / 2, panelY + 37, ActivityColors.ACCENT_LIGHT);
        }

        btnResetW = PANEL_W - 20;
        btnResetH = 20;
        btnResetX = panelX + 10;
        btnResetY = panelY + 52;

        btnDoneW = PANEL_W - 20;
        btnDoneH = 22;
        btnDoneX = panelX + 10;
        btnDoneY = panelY + 78;

        int hoveredBtn = -1;
        if (mouseX >= btnResetX && mouseX <= btnResetX + btnResetW && mouseY >= btnResetY && mouseY <= btnResetY + btnResetH) hoveredBtn = 1;
        else if (mouseX >= btnDoneX && mouseX <= btnDoneX + btnDoneW && mouseY >= btnDoneY && mouseY <= btnDoneY + btnDoneH) hoveredBtn = 2;

        if (hoveredBtn != lastHoveredBtn) {
            if (hoveredBtn != -1) SoundManager.playHoverImmediate();
            lastHoveredBtn = hoveredBtn;
        }

        int resetBg = (hoveredBtn == 1) ? ActivityColors.BUTTON_SECONDARY_HOVER : ActivityColors.BUTTON_SECONDARY_BG;
        ActivityGuiRenderer.drawPanel(context, btnResetX, btnResetY, btnResetW, btnResetH, resetBg, (hoveredBtn == 1) ? ActivityColors.BORDER_HOVER : ActivityColors.BORDER, true);
        if (textRenderer != null) {
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal("Сбросить"), btnResetX + btnResetW / 2, btnResetY + 6, 0xFFFFFFFF);
        }

        int doneBg = (hoveredBtn == 2) ? ActivityColors.BUTTON_PRIMARY_HOVER : ActivityColors.BUTTON_PRIMARY_BG;
        int doneBorder = (hoveredBtn == 2) ? ActivityColors.ACCENT_LIGHT : ActivityColors.ACCENT_PRIMARY;
        ActivityGuiRenderer.drawPanel(context, btnDoneX, btnDoneY, btnDoneW, btnDoneH, doneBg, doneBorder, true);
        if (textRenderer != null) {
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal("Готово"), btnDoneX + btnDoneW / 2, btnDoneY + 7, 0xFFFFFFFF);
        }

        if (textRenderer != null) {
            String hint = "ЛКМ — перемещение • ПКМ / R — сброс • Стрелки — подгонка (+Shift x5)";
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal(hint), width / 2, height - 16, ActivityColors.TEXT_MUTED);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

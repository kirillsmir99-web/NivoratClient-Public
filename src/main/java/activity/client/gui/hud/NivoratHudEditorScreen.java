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

public class NivoratHudEditorScreen extends Screen {

    private final Screen parent;
    private boolean isDragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    private int panelX = -1;
    private int panelY = -1;
    private static final int PANEL_W = 150;
    private static final int PANEL_H = 138;
    private boolean isPanelDragging = false;
    private int panelDragOffsetX = 0;
    private int panelDragOffsetY = 0;

    private int btnUpX, btnUpY, btnUpW, btnUpH;
    private int btnDownX, btnDownY, btnDownW, btnDownH;
    private int btnLeftX, btnLeftY, btnLeftW, btnLeftH;
    private int btnRightX, btnRightY, btnRightW, btnRightH;
    private int btnResetX, btnResetY, btnResetW, btnResetH;
    private int btnDoneX, btnDoneY, btnDoneW, btnDoneH;
    private int lastHoveredBtn = -1;

    public NivoratHudEditorScreen(Screen parent) {
        super(Text.literal("Водяной знак • Настройка позиции"));
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
        ActivityConfigManager.save();
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
            panelX = 20;
            panelY = Math.max(20, (height - PANEL_H) / 2);
        }
        SoundManager.playOpen();
    }

    private void nudge(int dx, int dy) {
        ActivityConfig config = ActivityConfigManager.getConfig();
        if (config == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null) return;

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
        if (input == null) return super.keyPressed(input);
        int key = input.key();
        int step = input.hasShift() ? 10 : 1;

        if (key == GLFW.GLFW_KEY_LEFT) {
            nudge(-step, 0);
            SoundManager.playHoverImmediate();
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT) {
            nudge(step, 0);
            SoundManager.playHoverImmediate();
            return true;
        }
        if (key == GLFW.GLFW_KEY_UP) {
            nudge(0, -step);
            SoundManager.playHoverImmediate();
            return true;
        }
        if (key == GLFW.GLFW_KEY_DOWN) {
            nudge(0, step);
            SoundManager.playHoverImmediate();
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
        if (input.isEscape() || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click == null) return false;
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.buttonInfo().button();

        if (button == 0) {
            if (mouseX >= panelX && mouseX <= panelX + PANEL_W && mouseY >= panelY && mouseY <= panelY + 28) {
                isPanelDragging = true;
                panelDragOffsetX = (int) Math.round(mouseX - panelX);
                panelDragOffsetY = (int) Math.round(mouseY - panelY);
                return true;
            }

            // Arrow buttons
            if (mouseX >= btnUpX && mouseX <= btnUpX + btnUpW && mouseY >= btnUpY && mouseY <= btnUpY + btnUpH) {
                nudge(0, -1);
                SoundManager.playClick();
                return true;
            }
            if (mouseX >= btnDownX && mouseX <= btnDownX + btnDownW && mouseY >= btnDownY && mouseY <= btnDownY + btnDownH) {
                nudge(0, 1);
                SoundManager.playClick();
                return true;
            }
            if (mouseX >= btnLeftX && mouseX <= btnLeftX + btnLeftW && mouseY >= btnLeftY && mouseY <= btnLeftY + btnLeftH) {
                nudge(-1, 0);
                SoundManager.playClick();
                return true;
            }
            if (mouseX >= btnRightX && mouseX <= btnRightX + btnRightW && mouseY >= btnRightY && mouseY <= btnRightY + btnRightH) {
                nudge(1, 0);
                SoundManager.playClick();
                return true;
            }

            // Reset button
            if (mouseX >= btnResetX && mouseX <= btnResetX + btnResetW && mouseY >= btnResetY && mouseY <= btnResetY + btnResetH) {
                ActivityConfig config = ActivityConfigManager.getConfig();
                if (config != null) {
                    config.hudCustomX = -1;
                    config.hudCustomY = -1;
                    ActivityConfigManager.markDirty();
                }
                SoundManager.playClick();
                return true;
            }

            // Done button
            if (mouseX >= btnDoneX && mouseX <= btnDoneX + btnDoneW && mouseY >= btnDoneY && mouseY <= btnDoneY + btnDoneH) {
                SoundManager.playClick();
                close();
                return true;
            }

            // HUD drag click
            ActivityConfig config = ActivityConfigManager.getConfig();
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.textRenderer != null && config != null) {
                int totalW = ActivityHudOverlay.getTotalWidth(mc, config);
                int totalH = ActivityHudOverlay.getTotalHeight(mc, config);
                int curX = ActivityHudOverlay.getEffectiveX(config, width, totalW);
                int curY = ActivityHudOverlay.getEffectiveY(config, height, totalH);

                if (mouseX >= curX - 6 && mouseX <= curX + totalW + 6 && mouseY >= curY - 6 && mouseY <= curY + totalH + 6) {
                    isDragging = true;
                    dragOffsetX = (int) Math.round(mouseX - curX);
                    dragOffsetY = (int) Math.round(mouseY - curY);
                    SoundManager.playClick();
                    return true;
                }
            }
        } else if (button == 1) {
            ActivityConfig config = ActivityConfigManager.getConfig();
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.textRenderer != null && config != null) {
                int totalW = ActivityHudOverlay.getTotalWidth(mc, config);
                int totalH = ActivityHudOverlay.getTotalHeight(mc, config);
                int curX = ActivityHudOverlay.getEffectiveX(config, width, totalW);
                int curY = ActivityHudOverlay.getEffectiveY(config, height, totalH);

                if (mouseX >= curX - 6 && mouseX <= curX + totalW + 6 && mouseY >= curY - 6 && mouseY <= curY + totalH + 6) {
                    config.hudCustomX = -1;
                    config.hudCustomY = -1;
                    ActivityConfigManager.markDirty();
                    SoundManager.playSelect();
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click != null && click.buttonInfo().button() == 0) {
            isDragging = false;
            isPanelDragging = false;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (click == null) return false;
        double mouseX = click.x();
        double mouseY = click.y();

        if (isPanelDragging) {
            panelX = (int) Math.round(mouseX - panelDragOffsetX);
            panelY = (int) Math.round(mouseY - panelDragOffsetY);
            panelX = Math.max(2, Math.min(width - PANEL_W - 2, panelX));
            panelY = Math.max(2, Math.min(height - PANEL_H - 2, panelY));
            return true;
        }

        if (isDragging) {
            ActivityConfig config = ActivityConfigManager.getConfig();
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.textRenderer != null && config != null) {
                int totalW = ActivityHudOverlay.getTotalWidth(mc, config);
                int totalH = ActivityHudOverlay.getTotalHeight(mc, config);

                int newX = (int) Math.round(mouseX - dragOffsetX);
                int newY = (int) Math.round(mouseY - dragOffsetY);

                // Magnetic snap to edges
                if (Math.abs(newX - 6) < 8) newX = 6;
                if (Math.abs(newX + totalW - (width - 6)) < 8) newX = width - totalW - 6;
                if (Math.abs(newY - 6) < 8) newY = 6;
                if (Math.abs(newY + totalH - (height - 6)) < 8) newY = height - totalH - 6;

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

        ActivityGuiRenderer.fill(context, 0, 0, width, height, ActivityColors.BACKGROUND_OVERLAY);

        if (mc != null && textRenderer != null) {
            int totalW = ActivityHudOverlay.getTotalWidth(mc, config);
            int totalH = ActivityHudOverlay.getTotalHeight(mc, config);
            int currentX = ActivityHudOverlay.getEffectiveX(config, width, totalW);
            int currentY = ActivityHudOverlay.getEffectiveY(config, height, totalH);

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

            boolean isHovered = mouseX >= currentX - 6 && mouseX <= currentX + totalW + 6 && mouseY >= currentY - 6 && mouseY <= currentY + totalH + 6;

            int boxBorder = (isDragging || isHovered) ? 0xCC3EA4E8 : 0x443EA4E8;
            int boxBg = (isDragging || isHovered) ? 0x880A0D14 : 0x440A0D14;
            ActivityGuiRenderer.drawPanel(context, currentX - 4, currentY - 4, totalW + 8, totalH + 8, boxBg, boxBorder, true);

            // Render live watermark
            ActivityHudOverlay.renderHud(context, mc, config, currentX, currentY, totalW, width);

            if (isHovered || isDragging) {
                int chipW = 90;
                int chipH = 15;
                int chipX = currentX + (totalW - chipW) / 2;
                chipX = Math.max(4, Math.min(width - chipW - 4, chipX));
                int chipY = (currentY - chipH - 8 >= 4) ? (currentY - chipH - 8) : (currentY + totalH + 8);

                ActivityGuiRenderer.drawPanel(context, chipX, chipY, chipW, chipH, ActivityColors.PANEL_INNER_BG, ActivityColors.BORDER, true);
                ActivityGuiRenderer.fill(context, chipX + 5, chipY + 5, 4, 4, ActivityColors.ACCENT_PRIMARY);
                UiTextRenderer.drawTextWithShadow(context, textRenderer, Text.literal("X: " + currentX + "  Y: " + currentY), chipX + 13, chipY + 4, ActivityColors.TEXT_PRIMARY);
            }

            // Draggable control panel
            ActivityGuiRenderer.drawWindowFrame(context, panelX, panelY, PANEL_W, PANEL_H, ActivityColors.WINDOW_BACKGROUND, ActivityColors.BORDER, true);
            ActivityGuiRenderer.fill(context, panelX + 1, panelY + 1, PANEL_W - 2, 26, ActivityColors.HEADER_BACKGROUND);
            ActivityGuiRenderer.drawGlassHighlight(context, panelX, panelY, PANEL_W, PANEL_H, 1.0f);

            ActivityGuiRenderer.fill(context, panelX + 8, panelY + 8, 5, 5, ActivityColors.ACCENT_PRIMARY);
            UiTextRenderer.drawTextWithShadow(context, textRenderer, Text.literal("ВОДЯНОЙ ЗНАК"), panelX + 18, panelY + 6, ActivityColors.TEXT_PRIMARY);
            UiTextRenderer.drawTextWithShadow(context, textRenderer, Text.literal("Позиция HUD"), panelX + 18, panelY + 16, ActivityColors.TEXT_MUTED);

            ActivityGuiRenderer.drawHorizontalLine(context, panelX + 6, panelY + 28, PANEL_W - 12, 0x44353B49);

            String posStr = (config != null && config.hudCustomX < 0 && config.hudCustomY < 0) ? "АВТО-ПОЗИЦИЯ" : ("X: " + currentX + " | Y: " + currentY);
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal(posStr), panelX + PANEL_W / 2, panelY + 34, ActivityColors.ACCENT_LIGHT);

            // Nudge arrow buttons
            int arrowSize = 16;
            int arrowCenterX = panelX + PANEL_W / 2;
            int arrowTopY = panelY + 46;

            btnUpX = arrowCenterX - arrowSize / 2;
            btnUpY = arrowTopY;
            btnUpW = arrowSize;
            btnUpH = arrowSize;

            btnLeftX = arrowCenterX - arrowSize - 2;
            btnLeftY = arrowTopY + arrowSize + 2;
            btnLeftW = arrowSize;
            btnLeftH = arrowSize;

            btnRightX = arrowCenterX + 2;
            btnRightY = arrowTopY + arrowSize + 2;
            btnRightW = arrowSize;
            btnRightH = arrowSize;

            btnDownX = arrowCenterX - arrowSize / 2;
            btnDownY = arrowTopY + (arrowSize + 2) * 2;
            btnDownW = arrowSize;
            btnDownH = arrowSize;

            // Render arrow buttons
            drawNudgeButton(context, btnUpX, btnUpY, btnUpW, btnUpH, "▲", mouseX, mouseY);
            drawNudgeButton(context, btnDownX, btnDownY, btnDownW, btnDownH, "▼", mouseX, mouseY);
            drawNudgeButton(context, btnLeftX, btnLeftY, btnLeftW, btnLeftH, "◄", mouseX, mouseY);
            drawNudgeButton(context, btnRightX, btnRightY, btnRightW, btnRightH, "►", mouseX, mouseY);
        }

        int btnRowW = (PANEL_W - 24) / 2;
        btnResetW = btnRowW;
        btnResetH = 20;
        btnResetX = panelX + 10;
        btnResetY = panelY + 110;

        btnDoneW = btnRowW;
        btnDoneH = 20;
        btnDoneX = panelX + 14 + btnRowW;
        btnDoneY = panelY + 110;

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
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal("Сброс"), btnResetX + btnResetW / 2, btnResetY + 6, 0xFFFFFFFF);
        }

        int doneBg = (hoveredBtn == 2) ? ActivityColors.BUTTON_PRIMARY_HOVER : ActivityColors.BUTTON_PRIMARY_BG;
        int doneBorder = (hoveredBtn == 2) ? ActivityColors.ACCENT_LIGHT : ActivityColors.ACCENT_PRIMARY;
        ActivityGuiRenderer.drawPanel(context, btnDoneX, btnDoneY, btnDoneW, btnDoneH, doneBg, doneBorder, true);
        if (textRenderer != null) {
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal("Готово"), btnDoneX + btnDoneW / 2, btnDoneY + 6, 0xFFFFFFFF);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawNudgeButton(DrawContext context, int x, int y, int w, int h, String symbol, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        int bg = hovered ? ActivityColors.BUTTON_SECONDARY_HOVER : ActivityColors.BUTTON_SECONDARY_BG;
        int border = hovered ? ActivityColors.BORDER_HOVER : ActivityColors.BORDER;
        ActivityGuiRenderer.drawPanel(context, x, y, w, h, bg, border, true);
        if (textRenderer != null) {
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal(symbol), x + w / 2, y + (h - 9) / 2, hovered ? ActivityColors.ACCENT_LIGHT : ActivityColors.TEXT_PRIMARY);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

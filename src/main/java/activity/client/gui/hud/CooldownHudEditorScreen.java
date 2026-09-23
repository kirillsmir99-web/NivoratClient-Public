package activity.client.gui.hud;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.font.UiTextRenderer;
import activity.client.gui.render.ActivityGuiRenderer;
import activity.client.gui.sound.SoundManager;
import activity.client.gui.theme.ActivityColors;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.impl.utility.CooldownHudModule;
import activity.client.module.service.CooldownTrackerService;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class CooldownHudEditorScreen extends Screen {

    private final Screen parent;
    private boolean isDragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    private int panelX = -1;
    private int panelY = -1;
    private static final int PANEL_W = 160;
    private static final int PANEL_H = 126;
    private boolean isPanelDragging = false;
    private int panelDragOffsetX = 0;
    private int panelDragOffsetY = 0;

    private int btnToggleOrientX, btnToggleOrientY, btnToggleOrientW, btnToggleOrientH;
    private int btnResetX, btnResetY, btnResetW, btnResetH;
    private int btnDoneX, btnDoneY, btnDoneW, btnDoneH;
    private int lastHoveredBtn = -1;

    public CooldownHudEditorScreen(Screen parent) {
        super(Text.literal("Cooldown HUD • Настройка позиции"));
        this.parent = parent;
    }

    @Override
    public void close() {
        SoundManager.playClose();
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            ActivityConfigManager.save();
        }
        if (this.client != null) {
            this.client.setScreen(this.parent);
        } else {
            super.close();
        }
    }

    @Override
    protected void init() {
        super.init();
        isDragging = false;
        isPanelDragging = false;
        if (panelX < 0 || panelY < 0) {
            panelX = 20;
            panelY = Math.max(20, (height - PANEL_H) / 2);
        }
        SoundManager.playOpen();
    }

    private void nudge(int dx, int dy) {
        ActivityConfig config = ActivityConfigManager.getConfig();
        boolean vertical = config != null && config.cooldownHudVertical;
        List<CooldownTrackerService.CooldownEntry> mockEntries = CooldownHudOverlay.getMockEntriesForPreview();
        int totalW = CooldownHudOverlay.calculateTotalWidth(textRenderer, mockEntries, vertical);
        int totalH = CooldownHudOverlay.calculateTotalHeight(mockEntries, vertical);

        int curX = CooldownHudOverlay.getEffectiveX(width);
        int curY = CooldownHudOverlay.getEffectiveY(height);

        int newX = Math.max(2, Math.min(width - totalW - 2, curX + dx));
        int newY = Math.max(2, Math.min(height - totalH - 2, curY + dy));

        if (config != null) {
            config.cooldownHudCustomX = newX;
            config.cooldownHudCustomY = newY;
            ActivityConfigManager.save();
        }
    }

    private void resetPosition() {
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            c.cooldownHudCustomX = -1;
            c.cooldownHudCustomY = -1;
            ActivityConfigManager.save();
        }
        SoundManager.playSelect();
    }

    private void toggleOrientation() {
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            boolean newVertical = !c.cooldownHudVertical;
            c.cooldownHudVertical = newVertical;
            IModule mod = ModuleRegistry.get(CooldownHudModule.ID);
            if (mod instanceof CooldownHudModule chm) {
                chm.vertical = newVertical;
            }
            ActivityConfigManager.save();
        }
        SoundManager.playClick();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) Math.round(click.x());
        int my = (int) Math.round(click.y());
        int button = click.buttonInfo().button();

        if (button == 0) {
            if (mx >= btnToggleOrientX && mx <= btnToggleOrientX + btnToggleOrientW &&
                my >= btnToggleOrientY && my <= btnToggleOrientY + btnToggleOrientH) {
                toggleOrientation();
                return true;
            }

            if (mx >= btnResetX && mx <= btnResetX + btnResetW &&
                my >= btnResetY && my <= btnResetY + btnResetH) {
                resetPosition();
                return true;
            }

            if (mx >= btnDoneX && mx <= btnDoneX + btnDoneW &&
                my >= btnDoneY && my <= btnDoneY + btnDoneH) {
                SoundManager.playClick();
                close();
                return true;
            }

            if (mx >= panelX && mx <= panelX + PANEL_W && my >= panelY && my <= panelY + PANEL_H) {
                isPanelDragging = true;
                panelDragOffsetX = mx - panelX;
                panelDragOffsetY = my - panelY;
                return true;
            }
        }

        ActivityConfig config = ActivityConfigManager.getConfig();
        boolean vertical = config != null && config.cooldownHudVertical;
        List<CooldownTrackerService.CooldownEntry> mockEntries = CooldownHudOverlay.getMockEntriesForPreview();

        int curX = CooldownHudOverlay.getEffectiveX(width);
        int curY = CooldownHudOverlay.getEffectiveY(height);
        int totalW = CooldownHudOverlay.calculateTotalWidth(textRenderer, mockEntries, vertical);
        int totalH = CooldownHudOverlay.calculateTotalHeight(mockEntries, vertical);

        int boxX = curX - 6;
        int boxY = curY - 6;
        int boxW = totalW + 12;
        int boxH = totalH + 12;

        boolean insideWidget = mx >= boxX && mx <= boxX + boxW && my >= boxY && my <= boxY + boxH;

        if (button == 0 && insideWidget) {
            isDragging = true;
            dragOffsetX = mx - curX;
            dragOffsetY = my - curY;
            SoundManager.playClick();
            return true;
        } else if (button == 1 && insideWidget) {
            resetPosition();
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
                ActivityConfigManager.save();
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
            boolean vertical = config != null && config.cooldownHudVertical;
            List<CooldownTrackerService.CooldownEntry> mockEntries = CooldownHudOverlay.getMockEntriesForPreview();
            int totalW = CooldownHudOverlay.calculateTotalWidth(textRenderer, mockEntries, vertical);
            int totalH = CooldownHudOverlay.calculateTotalHeight(mockEntries, vertical);

            int newX = (int) Math.round(click.x() - dragOffsetX);
            int newY = (int) Math.round(click.y() - dragOffsetY);

            int centerX = width / 2 - totalW / 2;
            if (Math.abs(newX - centerX) <= 4) {
                newX = centerX;
            }
            int centerY = height / 2 - totalH / 2;
            if (Math.abs(newY - centerY) <= 4) {
                newY = centerY;
            }

            int clampedX = Math.max(2, Math.min(width - totalW - 2, newX));
            int clampedY = Math.max(2, Math.min(height - totalH - 2, newY));

            if (config != null) {
                config.cooldownHudCustomX = clampedX;
                config.cooldownHudCustomY = clampedY;
                ActivityConfigManager.markDirty();
            }
            return true;
        }

        return super.mouseDragged(click, offsetX, offsetY);
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
            resetPosition();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        ActivityGuiRenderer.fill(context, 0, 0, width, height, ActivityColors.BACKGROUND_OVERLAY);

        ActivityConfig config = ActivityConfigManager.getConfig();
        boolean vertical = config != null && config.cooldownHudVertical;
        List<CooldownTrackerService.CooldownEntry> mockEntries = CooldownHudOverlay.getMockEntriesForPreview();

        int curX = CooldownHudOverlay.getEffectiveX(width);
        int curY = CooldownHudOverlay.getEffectiveY(height);
        int totalW = CooldownHudOverlay.calculateTotalWidth(textRenderer, mockEntries, vertical);
        int totalH = CooldownHudOverlay.calculateTotalHeight(mockEntries, vertical);

        if (isDragging) {
            if (Math.abs(curX + totalW / 2 - width / 2) <= 1) {
                ActivityGuiRenderer.drawVerticalLine(context, width / 2, 0, height, 0x40FFFFFF);
            }
            if (Math.abs(curY + totalH / 2 - height / 2) <= 1) {
                ActivityGuiRenderer.drawHorizontalLine(context, 0, height / 2, width, 0x40FFFFFF);
            }
        }

        int boxX = curX - 6;
        int boxY = curY - 6;
        int boxW = totalW + 12;
        int boxH = totalH + 12;

        boolean hovered = mouseX >= boxX && mouseX <= boxX + boxW && mouseY >= boxY && mouseY <= boxY + boxH;

        int fillAlpha = (isDragging || hovered) ? 0x30FFFFFF : 0x18FFFFFF;
        ActivityGuiRenderer.fill(context, boxX, boxY, boxW, boxH, fillAlpha);

        int borderColor = (isDragging || hovered) ? 0x90FFFFFF : 0x35FFFFFF;
        ActivityGuiRenderer.drawBorder(context, boxX, boxY, boxW, boxH, borderColor);

        CooldownHudOverlay.renderCooldownList(context, textRenderer, mockEntries, curX, curY, vertical);

        if (hovered || isDragging) {
            int chipW = 96;
            int chipH = 15;
            int chipX = curX + (totalW - chipW) / 2;
            chipX = Math.max(4, Math.min(width - chipW - 4, chipX));
            int chipY = (curY - chipH - 8 >= 4) ? (curY - chipH - 8) : (curY + totalH + 8);

            ActivityGuiRenderer.drawPanel(context, chipX, chipY, chipW, chipH, ActivityColors.PANEL_INNER_BG, ActivityColors.BORDER, true);
            ActivityGuiRenderer.fill(context, chipX + 5, chipY + 5, 4, 4, ActivityColors.ACCENT_PRIMARY);
            if (textRenderer != null) {
                UiTextRenderer.drawTextWithShadow(context, textRenderer, Text.literal("X: " + curX + "  Y: " + curY), chipX + 13, chipY + 4, ActivityColors.TEXT_PRIMARY);
            }
        }

        renderControlPanel(context, mouseX, mouseY, vertical);

        if (textRenderer != null) {
            String hint = "ЛКМ — перемещение • ПКМ / R — сброс • Стрелки — подгонка (+Shift x5)";
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal(hint), width / 2, height - 16, ActivityColors.TEXT_MUTED);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderControlPanel(DrawContext context, int mouseX, int mouseY, boolean vertical) {
        ActivityGuiRenderer.drawWindowFrame(context, panelX, panelY, PANEL_W, PANEL_H, ActivityColors.WINDOW_BACKGROUND, ActivityColors.BORDER, true);
        ActivityGuiRenderer.fill(context, panelX + 1, panelY + 1, PANEL_W - 2, 24, ActivityColors.HEADER_BACKGROUND);
        ActivityGuiRenderer.drawGlassHighlight(context, panelX, panelY, PANEL_W, PANEL_H, 1.0f);

        if (textRenderer != null) {
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal("Cooldown HUD"), panelX + PANEL_W / 2, panelY + 7, ActivityColors.TEXT_PRIMARY);
        }

        btnToggleOrientX = panelX + 10;
        btnToggleOrientY = panelY + 30;
        btnToggleOrientW = PANEL_W - 20;
        btnToggleOrientH = 20;

        String orientText = vertical ? "Вид: Вертикальный" : "Вид: Горизонтальный";
        boolean hoverOrient = mouseX >= btnToggleOrientX && mouseX <= btnToggleOrientX + btnToggleOrientW &&
                             mouseY >= btnToggleOrientY && mouseY <= btnToggleOrientY + btnToggleOrientH;
        int bgOrient = hoverOrient ? ActivityColors.BUTTON_SECONDARY_HOVER : ActivityColors.BUTTON_SECONDARY_BG;
        int borderOrient = hoverOrient ? ActivityColors.BORDER_HOVER : ActivityColors.BORDER;
        ActivityGuiRenderer.drawPanel(context, btnToggleOrientX, btnToggleOrientY, btnToggleOrientW, btnToggleOrientH, bgOrient, borderOrient, true);
        if (textRenderer != null) {
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal(orientText), btnToggleOrientX + btnToggleOrientW / 2, btnToggleOrientY + 6, ActivityColors.TEXT_PRIMARY);
        }

        btnResetX = panelX + 10;
        btnResetY = panelY + 56;
        btnResetW = (PANEL_W - 26) / 2;
        btnResetH = 22;

        boolean hoverReset = mouseX >= btnResetX && mouseX <= btnResetX + btnResetW &&
                            mouseY >= btnResetY && mouseY <= btnResetY + btnResetH;
        int bgReset = hoverReset ? ActivityColors.BUTTON_SECONDARY_HOVER : ActivityColors.BUTTON_SECONDARY_BG;
        int borderReset = hoverReset ? ActivityColors.BORDER_HOVER : ActivityColors.BORDER;
        ActivityGuiRenderer.drawPanel(context, btnResetX, btnResetY, btnResetW, btnResetH, bgReset, borderReset, true);
        if (textRenderer != null) {
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal("Сбросить"), btnResetX + btnResetW / 2, btnResetY + 7, 0xFFFF7777);
        }

        btnDoneX = btnResetX + btnResetW + 6;
        btnDoneY = btnResetY;
        btnDoneW = btnResetW;
        btnDoneH = 22;

        boolean hoverDone = mouseX >= btnDoneX && mouseX <= btnDoneX + btnDoneW &&
                           mouseY >= btnDoneY && mouseY <= btnDoneY + btnDoneH;
        int bgDone = hoverDone ? ActivityColors.BUTTON_PRIMARY_HOVER : ActivityColors.BUTTON_PRIMARY_BG;
        int borderDone = hoverDone ? ActivityColors.ACCENT_LIGHT : ActivityColors.ACCENT_PRIMARY;
        ActivityGuiRenderer.drawPanel(context, btnDoneX, btnDoneY, btnDoneW, btnDoneH, bgDone, borderDone, true);
        if (textRenderer != null) {
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal("Готово"), btnDoneX + btnDoneW / 2, btnDoneY + 7, 0xFFFFFFFF);
        }

        int hoveredBtn = hoverOrient ? 0 : (hoverReset ? 1 : (hoverDone ? 2 : -1));
        if (hoveredBtn != lastHoveredBtn) {
            if (hoveredBtn != -1) SoundManager.playHoverImmediate();
            lastHoveredBtn = hoveredBtn;
        }

        if (textRenderer != null) {
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal("Тяните мышкой или ПКМ"), panelX + PANEL_W / 2, panelY + 86, ActivityColors.TEXT_MUTED);
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal("Стрелки — подгонка"), panelX + PANEL_W / 2, panelY + 100, ActivityColors.TEXT_MUTED);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

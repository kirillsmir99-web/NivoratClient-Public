package dev.hpreaper;

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

public final class HpHudEditorScreen extends Screen {
    private static final int[] SHELL_RADII = { 8, 6, 4, 2 };
    private static final float[] SHELL_WEIGHTS = { 0.12f, 0.22f, 0.32f, 0.34f };
    private static final int[][] PRECOMPUTED_DX;

    static {
        PRECOMPUTED_DX = new int[SHELL_RADII.length][];
        for (int i = 0; i < SHELL_RADII.length; i++) {
            int r = SHELL_RADII[i];
            PRECOMPUTED_DX[i] = new int[2 * r + 1];
            for (int dy = -r; dy <= r; dy++) {
                PRECOMPUTED_DX[i][dy + r] = (int) Math.round(Math.sqrt(r * r - dy * dy));
            }
        }
    }

    private final Screen parent;
    private boolean isDragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    // Draggable vertical floating card widget
    private int panelX = -1;
    private int panelY = -1;
    private static final int PANEL_W = 140;
    private static final int PANEL_H = 138;
    private boolean isPanelDragging = false;
    private int panelDragOffsetX = 0;
    private int panelDragOffsetY = 0;

    // Interactive button bounds inside floating panel
    private int btnModeX, btnModeY, btnModeW, btnModeH;
    private int btnResetX, btnResetY, btnResetW, btnResetH;
    private int btnDoneX, btnDoneY, btnDoneW, btnDoneH;

    private int lastHoveredBtn = -1;

    public HpHudEditorScreen(Screen parent) {
        super(Text.literal("HP Reaper • Настройка позиции HUD"));
        this.parent = parent;
    }

    public HpHudEditorScreen() {
        this(null);
    }

    @Override
    public void close() {
        isDragging = false;
        isPanelDragging = false;
        SoundManager.playClose();
        VitalityConfig.save();
        activity.client.config.ActivityConfig c = activity.client.config.ActivityConfigManager.getConfig();
        if (c != null) {
            c.hpReaperOwnHealthX = VitalityConfig.ownHealthX;
            c.hpReaperOwnHealthY = VitalityConfig.ownHealthY;
            c.hpReaperCrosshairTargetX = VitalityConfig.crosshairTargetX;
            c.hpReaperCrosshairTargetY = VitalityConfig.crosshairTargetY;
            c.hpReaperTargetHealthX = VitalityConfig.targetHealthX;
            c.hpReaperTargetHealthY = VitalityConfig.targetHealthY;
            c.hpReaperDiffX = VitalityConfig.diffX;
            c.hpReaperDiffY = VitalityConfig.diffY;
            activity.client.config.ActivityConfigManager.markDirty();
        }
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
        int elementW = HealthHudOverlay.getPreviewWidth(textRenderer, VitalityConfig.displayMode);
        int elementH = HealthHudOverlay.getPreviewHeight(VitalityConfig.displayMode);
        int curX = HealthHudOverlay.getEffectiveX(VitalityConfig.displayMode, width, elementW);
        int curY = HealthHudOverlay.getEffectiveY(VitalityConfig.displayMode, height, elementH);
        int newX = Math.max(2, Math.min(width - elementW - 2, curX + dx));
        int newY = Math.max(4, Math.min(height - elementH - 2, curY + dy));
        VitalityConfig.setModePos(VitalityConfig.displayMode, newX, newY);
        VitalityConfig.save();
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
            VitalityConfig.resetModePos(VitalityConfig.displayMode);
            VitalityConfig.save();
            SoundManager.playSelect();
            return true;
        }
        if (key == GLFW.GLFW_KEY_TAB) {
            HealthHudOverlay.cycleDisplayMode();
            VitalityConfig.save();
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

        btnModeW = PANEL_W - 20;
        btnModeH = 20;
        btnModeX = panelX + 10;
        btnModeY = panelY + 52;

        btnResetW = PANEL_W - 20;
        btnResetH = 20;
        btnResetX = panelX + 10;
        btnResetY = panelY + 76;

        btnDoneW = PANEL_W - 20;
        btnDoneH = 22;
        btnDoneX = panelX + 10;
        btnDoneY = panelY + 102;

        // 1. Floating panel buttons or dragging
        if (button == 0) {
            if (mx >= btnModeX && mx <= btnModeX + btnModeW && my >= btnModeY && my <= btnModeY + btnModeH) {
                HealthHudOverlay.cycleDisplayMode();
                VitalityConfig.save();
                SoundManager.playSelect();
                return true;
            }
            if (mx >= btnResetX && mx <= btnResetX + btnResetW && my >= btnResetY && my <= btnResetY + btnResetH) {
                VitalityConfig.resetModePos(VitalityConfig.displayMode);
                VitalityConfig.save();
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

        // 2. Draggable preview card
        int elementW = HealthHudOverlay.getPreviewWidth(textRenderer, VitalityConfig.displayMode);
        int elementH = HealthHudOverlay.getPreviewHeight(VitalityConfig.displayMode);
        int currentX = HealthHudOverlay.getEffectiveX(VitalityConfig.displayMode, width, elementW);
        int currentY = HealthHudOverlay.getEffectiveY(VitalityConfig.displayMode, height, elementH);

        boolean inside = mx >= currentX - 10 && mx <= currentX + elementW + 10 && my >= currentY - 10 && my <= currentY + elementH + 10;

        if (button == 0 && inside) {
            isDragging = true;
            dragOffsetX = (int) Math.round(mx - currentX);
            dragOffsetY = (int) Math.round(my - currentY);
            SoundManager.playClick();
            return true;
        } else if (button == 1 && inside) {
            VitalityConfig.resetModePos(VitalityConfig.displayMode);
            VitalityConfig.save();
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
                VitalityConfig.save();
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
            int elementW = HealthHudOverlay.getPreviewWidth(textRenderer, VitalityConfig.displayMode);
            int elementH = HealthHudOverlay.getPreviewHeight(VitalityConfig.displayMode);
            int newX = (int) Math.round(click.x() - dragOffsetX);
            int newY = (int) Math.round(click.y() - dragOffsetY);

            int centerX = width / 2 - elementW / 2;
            if (Math.abs(newX - centerX) <= 4) {
                newX = centerX;
            }
            if (Math.abs(newX - 14) <= 5) {
                newX = 14;
            }

            int centerY = (height - elementH) / 2;
            if (Math.abs(newY - centerY) <= 4) {
                newY = centerY;
            }

            int clampedX = Math.max(2, Math.min(width - elementW - 2, newX));
            int clampedY = Math.max(4, Math.min(height - elementH - 2, newY));
            VitalityConfig.setModePos(VitalityConfig.displayMode, clampedX, clampedY);
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (context == null) return;

        int elementW = HealthHudOverlay.getPreviewWidth(textRenderer, VitalityConfig.displayMode);
        int elementH = HealthHudOverlay.getPreviewHeight(VitalityConfig.displayMode);

        if (panelX < 0 || panelY < 0) {
            panelX = 16;
            panelY = Math.max(16, (height - PANEL_H) / 2);
        }

        if (isPanelDragging) {
            int newPX = (int) Math.round(mouseX - panelDragOffsetX);
            int newPY = (int) Math.round(mouseY - panelDragOffsetY);
            panelX = Math.max(2, Math.min(width - PANEL_W - 2, newPX));
            panelY = Math.max(2, Math.min(height - PANEL_H - 2, newPY));
        }

        if (isDragging) {
            int newX = (int) Math.round(mouseX - dragOffsetX);
            int newY = (int) Math.round(mouseY - dragOffsetY);

            int centerX = width / 2 - elementW / 2;
            if (Math.abs(newX - centerX) <= 4) {
                newX = centerX;
            }
            if (Math.abs(newX - 14) <= 5) {
                newX = 14;
            }

            int centerY = (height - elementH) / 2;
            if (Math.abs(newY - centerY) <= 4) {
                newY = centerY;
            }

            int clampedX = Math.max(2, Math.min(width - elementW - 2, newX));
            int clampedY = Math.max(4, Math.min(height - elementH - 2, newY));
            VitalityConfig.setModePos(VitalityConfig.displayMode, clampedX, clampedY);
        }

        int currentX = HealthHudOverlay.getEffectiveX(VitalityConfig.displayMode, width, elementW);
        int currentY = HealthHudOverlay.getEffectiveY(VitalityConfig.displayMode, height, elementH);

        // Dark dim background
        ActivityGuiRenderer.fill(context, 0, 0, width, height, ActivityColors.BACKGROUND_OVERLAY);

        // Snap guide lines
        if (isDragging) {
            if (Math.abs(currentX - 14) <= 1) {
                ActivityGuiRenderer.drawVerticalLine(context, 14, 0, height, 0x40FFFFFF);
            }
            if (Math.abs(currentX + elementW / 2 - width / 2) <= 1) {
                ActivityGuiRenderer.drawVerticalLine(context, width / 2, 0, height, 0x40FFFFFF);
            }
            if (Math.abs(currentY + elementH / 2 - height / 2) <= 1) {
                ActivityGuiRenderer.drawHorizontalLine(context, 0, height / 2, width, 0x40FFFFFF);
            }
        }

        // Preview card highlight & soft compact halo
        int padX = 3;
        int padY = 2;
        int haloX = currentX - padX;
        int haloY = currentY - padY;
        int haloW = elementW + padX * 2;
        int haloH = elementH + padY * 2;

        boolean isHovered = mouseX >= currentX - 8 && mouseX <= currentX + elementW + 8 && mouseY >= currentY - 8 && mouseY <= currentY + elementH + 8;

        long timeMs = System.currentTimeMillis();
        double phase = (timeMs % 2400L) / 2400.0 * 2.0 * Math.PI;
        float pulse = (float) (0.5 + 0.5 * Math.sin(phase));

        float stateMultiplier = isDragging ? 1.25f : (isHovered ? 1.12f : 1.00f);
        float peakAlpha = (0.10f + 0.08f * pulse) * stateMultiplier;

        renderCapsulePulse(context, haloX, haloY, haloX + haloW, haloY + haloH, peakAlpha);

        // Render actual vertical HUD preview element
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) {
            HealthHudOverlay.renderPreview(context, mc, currentX, currentY, VitalityConfig.displayMode);
        }

        // Coordinate tooltip chip
        if (isHovered || isDragging) {
            int chipW = 96;
            int chipH = 15;
            int chipX = currentX + (elementW - chipW) / 2;
            chipX = Math.max(4, Math.min(width - chipW - 4, chipX));
            int chipY = (currentY - chipH - 6 >= 4) ? (currentY - chipH - 6) : (currentY + elementH + 6);

            ActivityGuiRenderer.drawPanel(context, chipX, chipY, chipW, chipH, ActivityColors.PANEL_INNER_BG, ActivityColors.BORDER, true);
            ActivityGuiRenderer.fill(context, chipX + 5, chipY + 5, 4, 4, ActivityColors.ACCENT_PRIMARY);
            if (textRenderer != null) {
                UiTextRenderer.drawTextWithShadow(context, textRenderer, Text.literal("X: " + currentX + "  Y: " + currentY), chipX + 13, chipY + 4, ActivityColors.TEXT_PRIMARY);
            }
        }

        // ==========================================
        // FLOATING DRAGGABLE CONTROL CARD (LEFT DOCKED)
        // ==========================================
        ActivityGuiRenderer.drawWindowFrame(context, panelX, panelY, PANEL_W, PANEL_H, ActivityColors.WINDOW_BACKGROUND, ActivityColors.BORDER, true);
        ActivityGuiRenderer.fill(context, panelX + 1, panelY + 1, PANEL_W - 2, 28, ActivityColors.HEADER_BACKGROUND);
        ActivityGuiRenderer.drawGlassHighlight(context, panelX, panelY, PANEL_W, PANEL_H, 1.0f);

        if (textRenderer != null) {
            ActivityGuiRenderer.fill(context, panelX + 8, panelY + 8, 5, 5, ActivityColors.ACCENT_PRIMARY);
            UiTextRenderer.drawTextWithShadow(context, textRenderer, Text.literal("HP REAPER"), panelX + 18, panelY + 7, ActivityColors.TEXT_PRIMARY);
            UiTextRenderer.drawTextWithShadow(context, textRenderer, Text.literal("Настройка HUD"), panelX + 18, panelY + 18, ActivityColors.TEXT_MUTED);

            ActivityGuiRenderer.drawHorizontalLine(context, panelX + 6, panelY + 31, PANEL_W - 12, 0x44353B49);

            String posStr = (VitalityConfig.getModeX(VitalityConfig.displayMode) < 0 && VitalityConfig.getModeY(VitalityConfig.displayMode) < 0)
                    ? "АВТО-ПОЗИЦИЯ" : ("X: " + currentX + " | Y: " + currentY);
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal(posStr), panelX + PANEL_W / 2, panelY + 37, ActivityColors.ACCENT_LIGHT);
        }

        btnModeW = PANEL_W - 20;
        btnModeH = 20;
        btnModeX = panelX + 10;
        btnModeY = panelY + 52;

        btnResetW = PANEL_W - 20;
        btnResetH = 20;
        btnResetX = panelX + 10;
        btnResetY = panelY + 76;

        btnDoneW = PANEL_W - 20;
        btnDoneH = 22;
        btnDoneX = panelX + 10;
        btnDoneY = panelY + 102;

        int hoveredBtn = -1;
        if (mouseX >= btnModeX && mouseX <= btnModeX + btnModeW && mouseY >= btnModeY && mouseY <= btnModeY + btnModeH) hoveredBtn = 1;
        else if (mouseX >= btnResetX && mouseX <= btnResetX + btnResetW && mouseY >= btnResetY && mouseY <= btnResetY + btnResetH) hoveredBtn = 2;
        else if (mouseX >= btnDoneX && mouseX <= btnDoneX + btnDoneW && mouseY >= btnDoneY && mouseY <= btnDoneY + btnDoneH) hoveredBtn = 3;

        if (hoveredBtn != lastHoveredBtn) {
            if (hoveredBtn != -1) SoundManager.playHoverImmediate();
            lastHoveredBtn = hoveredBtn;
        }

        // 1. Mode Button
        int modeBg = (hoveredBtn == 1) ? ActivityColors.BUTTON_SECONDARY_HOVER : ActivityColors.BUTTON_SECONDARY_BG;
        ActivityGuiRenderer.drawPanel(context, btnModeX, btnModeY, btnModeW, btnModeH, modeBg, (hoveredBtn == 1) ? ActivityColors.BORDER_HOVER : ActivityColors.BORDER, true);
        if (textRenderer != null) {
            String modeName = VitalityConfig.displayMode.getShortName();
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal(modeName), btnModeX + btnModeW / 2, btnModeY + 6, 0xFFFFFFFF);
        }

        // 2. Reset Button
        int resetBg = (hoveredBtn == 2) ? ActivityColors.BUTTON_SECONDARY_HOVER : ActivityColors.BUTTON_SECONDARY_BG;
        ActivityGuiRenderer.drawPanel(context, btnResetX, btnResetY, btnResetW, btnResetH, resetBg, (hoveredBtn == 2) ? ActivityColors.BORDER_HOVER : ActivityColors.BORDER, true);
        if (textRenderer != null) {
            String rstText = "Сбросить";
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal(rstText), btnResetX + btnResetW / 2, btnResetY + 6, 0xFFFFFFFF);
        }

        // 3. Done Button
        int doneBg = (hoveredBtn == 3) ? ActivityColors.BUTTON_PRIMARY_HOVER : ActivityColors.BUTTON_PRIMARY_BG;
        int doneBorder = (hoveredBtn == 3) ? ActivityColors.ACCENT_LIGHT : ActivityColors.ACCENT_PRIMARY;
        ActivityGuiRenderer.drawPanel(context, btnDoneX, btnDoneY, btnDoneW, btnDoneH, doneBg, doneBorder, true);
        if (textRenderer != null) {
            String dnText = "Готово";
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal(dnText), btnDoneX + btnDoneW / 2, btnDoneY + 7, 0xFFFFFFFF);
        }

        // Subtle bottom hint bar
        if (textRenderer != null) {
            String hint = "ЛКМ — перемещение • ПКМ — сброс • TAB — режим • Стрелки — подгонка (+Shift x5)";
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal(hint), width / 2, height - 16, ActivityColors.TEXT_MUTED);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderCapsulePulse(DrawContext context, int x1, int y1, int x2, int y2, float peakAlpha) {
        for (int i = 0; i < SHELL_RADII.length; i++) {
            int r = SHELL_RADII[i];
            int a = Math.max(0, Math.min(255, Math.round(SHELL_WEIGHTS[i] * peakAlpha * 255.0f)));
            if (a <= 0) continue;
            int color = (a << 24) | 0x00FFFFFF;
            int[] dxTable = PRECOMPUTED_DX[i];

            for (int dy = -r; dy < 0; dy++) {
                int y = y1 + dy;
                int dx = dxTable[dy + r];
                context.fill(x1 - dx, y, x2 + dx, y + 1, color);
            }
            for (int y = y1; y <= y2; y++) {
                context.fill(x1 - r, y, x2 + r, y + 1, color);
            }
            for (int dy = 1; dy <= r; dy++) {
                int y = y2 + dy;
                int dx = dxTable[dy + r];
                context.fill(x1 - dx, y, x2 + dx, y + 1, color);
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

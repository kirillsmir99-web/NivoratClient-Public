package dev.hpreaper;

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
    private static final int[] SHELL_RADII = { 16, 14, 12, 10, 8, 6, 4, 2 };
    private static final float[] SHELL_WEIGHTS = { 0.0381f, 0.1084f, 0.1622f, 0.1913f, 0.1913f, 0.1622f, 0.1084f, 0.0381f };
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

    // Interactive button bounds in the top banner
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
        isDragging = false;
        this.clearChildren();
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

        // 1. Top banner buttons
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
        }

        // 2. Draggable preview card
        int elementW = HealthHudOverlay.getPreviewWidth(textRenderer, VitalityConfig.displayMode);
        int elementH = HealthHudOverlay.getPreviewHeight(VitalityConfig.displayMode);
        int currentX = HealthHudOverlay.getEffectiveX(VitalityConfig.displayMode, width, elementW);
        int currentY = HealthHudOverlay.getEffectiveY(VitalityConfig.displayMode, height, elementH);

        boolean inside = mx >= currentX - 16 && mx <= currentX + elementW + 16 && my >= currentY - 16 && my <= currentY + elementH + 16;

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
        if (click.buttonInfo().button() == 0 && isDragging) {
            isDragging = false;
            VitalityConfig.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
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
                ActivityGuiRenderer.drawVerticalLine(context, 14, 0, height, 0x5000D2FF);
            }
            if (Math.abs(currentX + elementW / 2 - width / 2) <= 1) {
                ActivityGuiRenderer.drawVerticalLine(context, width / 2, 0, height, 0x5000D2FF);
            }
            if (Math.abs(currentY + elementH / 2 - height / 2) <= 1) {
                ActivityGuiRenderer.drawHorizontalLine(context, 0, height / 2, width, 0x5000D2FF);
            }
        }

        // Preview card highlight & halo
        int padX = 5;
        int padY = 5;
        int haloX = currentX - padX;
        int haloY = currentY - padY;
        int haloW = elementW + padX * 2;
        int haloH = elementH + padY * 2;

        boolean isHovered = mouseX >= currentX - 16 && mouseX <= currentX + elementW + 16 && mouseY >= currentY - 16 && mouseY <= currentY + elementH + 16;

        long timeMs = System.currentTimeMillis();
        double phase = (timeMs % 2400L) / 2400.0 * 2.0 * Math.PI;
        float pulse = (float) (0.5 + 0.5 * Math.sin(phase));

        float stateMultiplier = isDragging ? 1.30f : (isHovered ? 1.15f : 1.00f);
        float peakAlpha = (0.12f + 0.08f * pulse) * stateMultiplier;

        renderCapsulePulse(context, haloX, haloY, haloX + haloW, haloY + haloH, peakAlpha);

        int boxBg = isDragging ? 0x442B79C2 : (isHovered ? 0x2A2B79C2 : 0x1A0E1015);
        int boxBorder = isDragging ? ActivityColors.ACCENT_LIGHT : (isHovered ? ActivityColors.BORDER_HOVER : ActivityColors.BORDER_LIGHT);
        ActivityGuiRenderer.drawPanel(context, haloX, haloY, haloW, haloH, boxBg, boxBorder, true);

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
            int chipY = (haloY - chipH - 4 >= 4) ? (haloY - chipH - 4) : (haloY + haloH + 4);

            ActivityGuiRenderer.drawPanel(context, chipX, chipY, chipW, chipH, ActivityColors.PANEL_INNER_BG, ActivityColors.BORDER, true);
            ActivityGuiRenderer.fill(context, chipX + 5, chipY + 5, 4, 4, ActivityColors.ACCENT_PRIMARY);
            if (textRenderer != null) {
                context.drawTextWithShadow(textRenderer, Text.literal("X: " + currentX + "  Y: " + currentY), chipX + 13, chipY + 4, ActivityColors.TEXT_PRIMARY);
            }
        }

        // ==========================================
        // TOP LAUNCHER BANNER (SLEEK & COMPACT)
        // ==========================================
        int bannerW = Math.min(540, width - 20);
        int bannerH = 34;
        int bannerX = (width - bannerW) / 2;
        int bannerY = 10;

        ActivityGuiRenderer.drawWindowFrame(context, bannerX, bannerY, bannerW, bannerH, ActivityColors.WINDOW_BACKGROUND, ActivityColors.BORDER, true);
        ActivityGuiRenderer.fill(context, bannerX + 1, bannerY + 1, bannerW - 2, bannerH - 2, ActivityColors.HEADER_BACKGROUND);
        ActivityGuiRenderer.drawGlassHighlight(context, bannerX, bannerY, bannerW, bannerH, 1.0f);

        if (textRenderer != null) {
            ActivityGuiRenderer.fill(context, bannerX + 8, bannerY + 8, 5, 5, ActivityColors.ACCENT_PRIMARY);
            context.drawTextWithShadow(textRenderer, Text.literal("HP REAPER • НАСТРОЙКА HUD"), bannerX + 18, bannerY + 7, ActivityColors.TEXT_PRIMARY);

            String posStr = (VitalityConfig.getModeX(VitalityConfig.displayMode) < 0 && VitalityConfig.getModeY(VitalityConfig.displayMode) < 0)
                    ? "АВТО-ПОЗИЦИЯ" : ("X: " + currentX + " | Y: " + currentY);
            context.drawTextWithShadow(textRenderer, Text.literal(posStr), bannerX + 18, bannerY + 19, ActivityColors.TEXT_ACCENT);
        }

        // Buttons in top bar:
        btnDoneW = 60;
        btnDoneH = 20;
        btnDoneX = bannerX + bannerW - btnDoneW - 6;
        btnDoneY = bannerY + (bannerH - btnDoneH) / 2;

        btnResetW = 100;
        btnResetH = 20;
        btnResetX = btnDoneX - btnResetW - 4;
        btnResetY = btnDoneY;

        btnModeW = 120;
        btnModeH = 20;
        btnModeX = btnResetX - btnModeW - 4;
        btnModeY = btnDoneY;

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
            int mw = textRenderer.getWidth(modeName);
            context.drawTextWithShadow(textRenderer, Text.literal(modeName), btnModeX + (btnModeW - mw) / 2, btnModeY + 6, 0xFFFFFFFF);
        }

        // 2. Reset Button
        int resetBg = (hoveredBtn == 2) ? ActivityColors.BUTTON_SECONDARY_HOVER : ActivityColors.BUTTON_SECONDARY_BG;
        ActivityGuiRenderer.drawPanel(context, btnResetX, btnResetY, btnResetW, btnResetH, resetBg, (hoveredBtn == 2) ? ActivityColors.BORDER_HOVER : ActivityColors.BORDER, true);
        if (textRenderer != null) {
            String rstText = "Сбросить";
            int rw = textRenderer.getWidth(rstText);
            context.drawTextWithShadow(textRenderer, Text.literal(rstText), btnResetX + (btnResetW - rw) / 2, btnResetY + 6, ActivityColors.TEXT_PRIMARY);
        }

        // 3. Done Button
        int doneBg = (hoveredBtn == 3) ? 0xFF33DCFF : ActivityColors.ACCENT_PRIMARY;
        ActivityGuiRenderer.fill(context, btnDoneX, btnDoneY, btnDoneW, btnDoneH, doneBg);
        ActivityGuiRenderer.drawBorder(context, btnDoneX, btnDoneY, btnDoneW, btnDoneH, (hoveredBtn == 3) ? 0xFFFFFFFF : ActivityColors.BORDER);
        if (textRenderer != null) {
            String dnText = "Готово";
            int dw = textRenderer.getWidth(dnText);
            context.drawTextWithShadow(textRenderer, Text.literal(dnText), btnDoneX + (btnDoneW - dw) / 2, btnDoneY + 6, 0xFF0E1015);
        }

        // Subtle bottom hint bar
        if (textRenderer != null) {
            String hint = "ЛКМ — перемещение • ПКМ — сброс • TAB — режим • Стрелки — подгонка (+Shift x5)";
            int hintW = textRenderer.getWidth(hint);
            context.drawTextWithShadow(textRenderer, Text.literal(hint), (width - hintW) / 2, height - 16, ActivityColors.TEXT_MUTED);
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

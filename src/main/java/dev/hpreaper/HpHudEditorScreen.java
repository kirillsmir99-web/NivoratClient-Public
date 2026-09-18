package dev.hpreaper;

import activity.client.gui.render.ActivityGuiRenderer;
import activity.client.gui.theme.ActivityColors;
import activity.client.gui.sound.SoundManager;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
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

    private ButtonWidget tabOwn;
    private ButtonWidget tabEverywhere;
    private ButtonWidget tabTarget;
    private ButtonWidget tabDiff;

    private ButtonWidget btnFilter;
    private ButtonWidget btnApplyAll;
    private ButtonWidget btnResetMode;
    private ButtonWidget btnResetAll;
    private ButtonWidget btnDone;
    private ButtonWidget btnClose;

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

        int dockW = Math.min(520, width - 20);
        int dockH = 88;
        int dockX = (width - dockW) / 2;
        int dockY = height - dockH - 12;

        btnClose = addDrawableChild(ButtonWidget.builder(
            Text.literal("✕"),
            b -> {
                SoundManager.playClick();
                close();
            }
        ).dimensions(dockX + dockW - 19, dockY + 3, 16, 14).build());

        int innerW = dockW - 16;
        int tabGap = 4;
        int tabW = (innerW - 3 * tabGap) / 4;

        tabOwn = addDrawableChild(ButtonWidget.builder(
            Text.literal("Своё HP"),
            b -> selectMode(HealthHudOverlay.DisplayMode.OWN_HEALTH)
        ).dimensions(dockX + 8, dockY + 24, tabW, 20).build());

        tabEverywhere = addDrawableChild(ButtonWidget.builder(
            Text.literal("Везде"),
            b -> selectMode(HealthHudOverlay.DisplayMode.CROSSHAIR_AND_TARGET)
        ).dimensions(dockX + 8 + (tabW + tabGap), dockY + 24, tabW, 20).build());

        tabTarget = addDrawableChild(ButtonWidget.builder(
            Text.literal("Только цель"),
            b -> selectMode(HealthHudOverlay.DisplayMode.TARGET_HEALTH)
        ).dimensions(dockX + 8 + (tabW + tabGap) * 2, dockY + 24, tabW, 20).build());

        tabDiff = addDrawableChild(ButtonWidget.builder(
            Text.literal("Своё+Цель+Разница"),
            b -> selectMode(HealthHudOverlay.DisplayMode.OWN_TARGET_AND_DIFFERENCE)
        ).dimensions(dockX + 8 + (tabW + tabGap) * 3, dockY + 24, tabW, 20).build());

        int btnGap = 4;
        int filterW = 115;
        int applyW = 125;
        int resetModeW = 105;
        int resetAllW = 85;
        int doneW = Math.max(50, innerW - (filterW + applyW + resetModeW + resetAllW + 4 * btnGap));

        btnFilter = addDrawableChild(ButtonWidget.builder(
            getFilterButtonText(),
            b -> {
                VitalityConfig.targetFilter = VitalityConfig.targetFilter.next();
                b.setMessage(getFilterButtonText());
                VitalityConfig.save();
                SoundManager.playClick();
            }
        ).dimensions(dockX + 8, dockY + 48, filterW, 20).build());

        btnApplyAll = addDrawableChild(ButtonWidget.builder(
            Text.literal("Применить ко всем"),
            b -> {
                int elementW = HealthHudOverlay.getPreviewWidth(textRenderer, VitalityConfig.displayMode);
                int elementH = HealthHudOverlay.getPreviewHeight(VitalityConfig.displayMode);
                int currentX = HealthHudOverlay.getEffectiveX(VitalityConfig.displayMode, width, elementW);
                int currentY = HealthHudOverlay.getEffectiveY(VitalityConfig.displayMode, height, elementH);
                VitalityConfig.applyPosToAll(currentX, currentY);
                VitalityConfig.save();
                SoundManager.playSuccess();
            }
        ).dimensions(dockX + 8 + filterW + btnGap, dockY + 48, applyW, 20).build());

        btnResetMode = addDrawableChild(ButtonWidget.builder(
            Text.literal("Сбросить режим"),
            b -> {
                VitalityConfig.resetModePos(VitalityConfig.displayMode);
                VitalityConfig.save();
                SoundManager.playClick();
            }
        ).dimensions(dockX + 8 + filterW + applyW + btnGap * 2, dockY + 48, resetModeW, 20).build());

        btnResetAll = addDrawableChild(ButtonWidget.builder(
            Text.literal("Сбросить всё"),
            b -> {
                VitalityConfig.resetAll();
                VitalityConfig.save();
                SoundManager.playClick();
            }
        ).dimensions(dockX + 8 + filterW + applyW + resetModeW + btnGap * 3, dockY + 48, resetAllW, 20).build());

        btnDone = addDrawableChild(ButtonWidget.builder(
            Text.literal("Готово"),
            b -> {
                SoundManager.playClick();
                close();
            }
        ).dimensions(dockX + 8 + filterW + applyW + resetModeW + resetAllW + btnGap * 4, dockY + 48, doneW, 20).build());

        updateTabStates();
    }

    private void selectMode(HealthHudOverlay.DisplayMode mode) {
        VitalityConfig.displayMode = mode;
        VitalityConfig.save();
        updateTabStates();
        SoundManager.playSelect();
    }

    private void updateTabStates() {
        HealthHudOverlay.DisplayMode current = VitalityConfig.displayMode;
        if (tabOwn != null) {
            tabOwn.active = (current != HealthHudOverlay.DisplayMode.OWN_HEALTH);
        }
        if (tabEverywhere != null) {
            tabEverywhere.active = (current != HealthHudOverlay.DisplayMode.CROSSHAIR_AND_TARGET);
        }
        if (tabTarget != null) {
            tabTarget.active = (current != HealthHudOverlay.DisplayMode.TARGET_HEALTH);
        }
        if (tabDiff != null) {
            tabDiff.active = (current != HealthHudOverlay.DisplayMode.OWN_TARGET_AND_DIFFERENCE);
        }
    }

    private Text getFilterButtonText() {
        return Text.literal("Цели: " + VitalityConfig.targetFilter.getShortName());
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
            SoundManager.playClick();
            return true;
        }
        if (key == GLFW.GLFW_KEY_TAB) {
            HealthHudOverlay.cycleDisplayMode();
            updateTabStates();
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
        int elementW = HealthHudOverlay.getPreviewWidth(textRenderer, VitalityConfig.displayMode);
        int elementH = HealthHudOverlay.getPreviewHeight(VitalityConfig.displayMode);
        int currentX = HealthHudOverlay.getEffectiveX(VitalityConfig.displayMode, width, elementW);
        int currentY = HealthHudOverlay.getEffectiveY(VitalityConfig.displayMode, height, elementH);

        double mx = click.x();
        double my = click.y();
        boolean inside = mx >= currentX - 12 && mx <= currentX + elementW + 12 && my >= currentY - 12 && my <= currentY + elementH + 12;

        if (click.buttonInfo().button() == 0 && inside) {
            isDragging = true;
            dragOffsetX = (int) Math.round(mx - currentX);
            dragOffsetY = (int) Math.round(my - currentY);
            SoundManager.playClick();
            return true;
        } else if (click.buttonInfo().button() == 1 && inside) {
            VitalityConfig.resetModePos(VitalityConfig.displayMode);
            VitalityConfig.save();
            SoundManager.playClick();
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
            if (Math.abs(newX - centerX) <= 3) {
                newX = centerX;
            }
            if (Math.abs(newX - 14) <= 4) {
                newX = 14;
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
        if (context == null) {
            return;
        }

        int elementW = HealthHudOverlay.getPreviewWidth(textRenderer, VitalityConfig.displayMode);
        int elementH = HealthHudOverlay.getPreviewHeight(VitalityConfig.displayMode);

        if (isDragging) {
            int newX = (int) Math.round(mouseX - dragOffsetX);
            int newY = (int) Math.round(mouseY - dragOffsetY);

            int centerX = width / 2 - elementW / 2;
            if (Math.abs(newX - centerX) <= 3) {
                newX = centerX;
            }
            if (Math.abs(newX - 14) <= 4) {
                newX = 14;
            }

            int clampedX = Math.max(2, Math.min(width - elementW - 2, newX));
            int clampedY = Math.max(4, Math.min(height - elementH - 2, newY));
            VitalityConfig.setModePos(VitalityConfig.displayMode, clampedX, clampedY);
        }

        int currentX = HealthHudOverlay.getEffectiveX(VitalityConfig.displayMode, width, elementW);
        int currentY = HealthHudOverlay.getEffectiveY(VitalityConfig.displayMode, height, elementH);

        ActivityGuiRenderer.fill(context, 0, 0, width, height, ActivityColors.BACKGROUND_OVERLAY);

        if (isDragging) {
            if (Math.abs(currentX + elementW / 2 - width / 2) <= 1) {
                ActivityGuiRenderer.drawVerticalLine(context, width / 2, 0, height, 0x502B79C2);
            }
            if (Math.abs(currentY + elementH / 2 - height / 2) <= 1) {
                ActivityGuiRenderer.drawHorizontalLine(context, 0, height / 2, width, 0x502B79C2);
            }
        }

        int padX = 5;
        int padY = 3;
        int boxX = currentX - padX;
        int boxY = currentY - padY;
        int boxW = elementW + padX * 2;
        int boxH = elementH + padY * 2;

        boolean isHovered = mouseX >= boxX - 2 && mouseX <= boxX + boxW + 2 && mouseY >= boxY - 2 && mouseY <= boxY + boxH + 2;

        long timeMs = System.currentTimeMillis();
        double phase = (timeMs % 2400L) / 2400.0 * 2.0 * Math.PI;
        float pulse = (float) (0.5 + 0.5 * Math.sin(phase));

        float stateMultiplier = isDragging ? 1.30f : (isHovered ? 1.15f : 1.00f);
        float peakAlpha = (0.12f + 0.08f * pulse) * stateMultiplier;

        renderCapsulePulse(context, boxX, boxY, boxX + boxW, boxY + boxH, peakAlpha);

        int boxBg = isDragging ? 0x442B79C2 : (isHovered ? 0x2A2B79C2 : 0x1A0E1015);
        int boxBorder = isDragging ? ActivityColors.ACCENT_LIGHT : (isHovered ? ActivityColors.BORDER_HOVER : ActivityColors.BORDER_LIGHT);
        ActivityGuiRenderer.drawPanel(context, boxX, boxY, boxW, boxH, boxBg, boxBorder, true);

        if (client != null) {
            HealthHudOverlay.renderPreview(context, client, currentX, currentY, VitalityConfig.displayMode);
        }

        if (isHovered || isDragging) {
            int chipW = 106;
            int chipH = 15;
            int chipX = currentX + (elementW - chipW) / 2;
            chipX = Math.max(4, Math.min(width - chipW - 4, chipX));
            int chipY = (boxY - chipH - 4 >= 4) ? (boxY - chipH - 4) : (boxY + boxH + 4);

            ActivityGuiRenderer.drawPanel(context, chipX, chipY, chipW, chipH, ActivityColors.PANEL_INNER_BG, ActivityColors.BORDER, true);
            ActivityGuiRenderer.fill(context, chipX + 5, chipY + 5, 4, 4, ActivityColors.ACCENT_PRIMARY);
            if (textRenderer != null) {
                context.drawTextWithShadow(textRenderer, Text.literal("X: " + currentX + "  Y: " + currentY), chipX + 13, chipY + 4, ActivityColors.TEXT_PRIMARY);
            }
        }

        int dockW = Math.min(520, width - 20);
        int dockH = 88;
        int dockX = (width - dockW) / 2;
        int dockY = height - dockH - 12;

        ActivityGuiRenderer.drawWindowFrame(context, dockX, dockY, dockW, dockH, ActivityColors.WINDOW_BACKGROUND, ActivityColors.BORDER, true);

        ActivityGuiRenderer.fill(context, dockX + 1, dockY + 1, dockW - 2, 19, ActivityColors.HEADER_BACKGROUND);
        ActivityGuiRenderer.drawHorizontalLine(context, dockX, dockY + 20, dockW, ActivityColors.BORDER_DIVIDER);
        ActivityGuiRenderer.drawGlassHighlight(context, dockX, dockY, dockW, 20, 1.0f);

        if (textRenderer != null) {
            ActivityGuiRenderer.fill(context, dockX + 8, dockY + 7, 5, 5, ActivityColors.ACCENT_PRIMARY);
            context.drawTextWithShadow(textRenderer, Text.literal("HP REAPER"), dockX + 17, dockY + 6, ActivityColors.TEXT_PRIMARY);
            context.drawTextWithShadow(textRenderer, Text.literal("•  НАСТРОЙКА HUD"), dockX + 74, dockY + 6, ActivityColors.TEXT_SECONDARY);

            int modeX = VitalityConfig.getModeX(VitalityConfig.displayMode);
            int modeY = VitalityConfig.getModeY(VitalityConfig.displayMode);
            String posStr = (modeX < 0 && modeY < 0) ? "АВТО-ПОЗИЦИЯ" : ("X: " + currentX + " | Y: " + currentY);
            int posStrW = textRenderer.getWidth(posStr);
            context.drawTextWithShadow(textRenderer, Text.literal(posStr), dockX + dockW - 26 - posStrW, dockY + 6, ActivityColors.TEXT_ACCENT);

            String hint = "ЛКМ — перемещение • ПКМ — сброс • Стрелки — подгонка (+Shift x5)";
            int hintW = textRenderer.getWidth(hint);
            context.drawTextWithShadow(textRenderer, Text.literal(hint), dockX + (dockW - hintW) / 2, dockY + 72, ActivityColors.TEXT_MUTED);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderCapsulePulse(DrawContext context, int x1, int y1, int x2, int y2, float peakAlpha) {
        for (int i = 0; i < SHELL_RADII.length; i++) {
            int r = SHELL_RADII[i];
            int a = Math.max(0, Math.min(255, Math.round(SHELL_WEIGHTS[i] * peakAlpha * 255.0f)));
            if (a <= 0) {
                continue;
            }
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

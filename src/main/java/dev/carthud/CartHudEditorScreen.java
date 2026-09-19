package dev.carthud;

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

public final class CartHudEditorScreen extends Screen {
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

    // Draggable vertical floating card widget
    private int panelX = -1;
    private int panelY = -1;
    private static final int PANEL_W = 140;
    private static final int PANEL_H = 114;
    private boolean isPanelDragging = false;
    private int panelDragOffsetX = 0;
    private int panelDragOffsetY = 0;

    // Interactive button bounds inside floating panel
    private int btnResetX, btnResetY, btnResetW, btnResetH;
    private int btnDoneX, btnDoneY, btnDoneW, btnDoneH;
    private int lastHoveredBtn = -1;

    public CartHudEditorScreen(Screen parent) {
        super(Text.literal("Cart HUD • Настройка позиции"));
        this.parent = parent;
    }

    public CartHudEditorScreen() {
        this(null);
    }

    @Override
    public void close() {
        isDragging = false;
        isPanelDragging = false;
        SoundManager.playClose();
        CartHudConfig.save();
        activity.client.config.ActivityConfig c = activity.client.config.ActivityConfigManager.getConfig();
        if (c != null) {
            c.cartHudCustomX = CartHudConfig.customX;
            c.cartHudCustomY = CartHudConfig.customY;
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
        isPanelDragging = false;
        this.clearChildren();
        if (panelX < 0 || panelY < 0) {
            panelX = 16;
            panelY = Math.max(16, (height - PANEL_H) / 2);
        }
        SoundManager.playOpen();
    }

    private void nudge(int dx, int dy) {
        int boxW = CartHudOverlay.ELEMENT_WIDTH;
        int boxH = CartHudOverlay.ELEMENT_HEIGHT;
        int curX = CartHudOverlay.getEffectiveX(width);
        int curY = CartHudOverlay.getEffectiveY(height);
        int newX = Math.max(2, Math.min(width - boxW - 2, curX + dx));
        int newY = Math.max(4, Math.min(height - boxH - 2, curY + dy));
        CartHudConfig.customX = newX;
        CartHudConfig.customY = newY;
        CartHudConfig.save();
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
            CartHudConfig.customX = -1;
            CartHudConfig.customY = -1;
            CartHudConfig.save();
            activity.client.config.ActivityConfig c = activity.client.config.ActivityConfigManager.getConfig();
            if (c != null) {
                c.cartHudCustomX = -1;
                c.cartHudCustomY = -1;
                activity.client.config.ActivityConfigManager.markDirty();
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

        // 1. Floating panel buttons or dragging
        if (button == 0) {
            if (mx >= btnResetX && mx <= btnResetX + btnResetW && my >= btnResetY && my <= btnResetY + btnResetH) {
                CartHudConfig.customX = -1;
                CartHudConfig.customY = -1;
                CartHudConfig.save();
                activity.client.config.ActivityConfig c = activity.client.config.ActivityConfigManager.getConfig();
                if (c != null) {
                    c.cartHudCustomX = -1;
                    c.cartHudCustomY = -1;
                    activity.client.config.ActivityConfigManager.markDirty();
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

        // 2. Draggable cart element (+10px generous hitbox)
        int currentX = CartHudOverlay.getEffectiveX(width);
        int currentY = CartHudOverlay.getEffectiveY(height);
        int boxW = CartHudOverlay.ELEMENT_WIDTH;
        int boxH = CartHudOverlay.ELEMENT_HEIGHT;

        boolean inside = mx >= currentX - 10 && mx <= currentX + boxW + 10 && my >= currentY - 10 && my <= currentY + boxH + 10;

        if (button == 0 && inside) {
            isDragging = true;
            dragOffsetX = (int) Math.round(mx - currentX);
            dragOffsetY = (int) Math.round(my - currentY);
            SoundManager.playClick();
            return true;
        } else if (button == 1 && inside) {
            CartHudConfig.customX = -1;
            CartHudConfig.customY = -1;
            CartHudConfig.save();
            activity.client.config.ActivityConfig c = activity.client.config.ActivityConfigManager.getConfig();
            if (c != null) {
                c.cartHudCustomX = -1;
                c.cartHudCustomY = -1;
                activity.client.config.ActivityConfigManager.markDirty();
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
                CartHudConfig.save();
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
            int boxW = CartHudOverlay.ELEMENT_WIDTH;
            int boxH = CartHudOverlay.ELEMENT_HEIGHT;
            int newX = (int) Math.round(click.x() - dragOffsetX);
            int newY = (int) Math.round(click.y() - dragOffsetY);

            int centerX = width / 2 - boxW / 2;
            if (Math.abs(newX - centerX) <= 4) {
                newX = centerX;
            }
            if (Math.abs(newX - 14) <= 5) {
                newX = 14;
            }

            int centerY = height / 2 - boxH / 2;
            if (Math.abs(newY - centerY) <= 4) {
                newY = centerY;
            }

            CartHudConfig.customX = Math.max(2, Math.min(width - boxW - 2, newX));
            CartHudConfig.customY = Math.max(4, Math.min(height - boxH - 2, newY));
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

        int boxW = CartHudOverlay.ELEMENT_WIDTH;
        int boxH = CartHudOverlay.ELEMENT_HEIGHT;

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

            int centerX = width / 2 - boxW / 2;
            if (Math.abs(newX - centerX) <= 4) {
                newX = centerX;
            }
            if (Math.abs(newX - 14) <= 5) {
                newX = 14;
            }

            int centerY = height / 2 - boxH / 2;
            if (Math.abs(newY - centerY) <= 4) {
                newY = centerY;
            }

            CartHudConfig.customX = Math.max(2, Math.min(width - boxW - 2, newX));
            CartHudConfig.customY = Math.max(4, Math.min(height - boxH - 2, newY));
        }

        int currentX = CartHudOverlay.getEffectiveX(width);
        int currentY = CartHudOverlay.getEffectiveY(height);

        ActivityGuiRenderer.fill(context, 0, 0, width, height, ActivityColors.BACKGROUND_OVERLAY);

        if (isDragging) {
            if (Math.abs(currentX - 14) <= 1) {
                ActivityGuiRenderer.drawVerticalLine(context, 14, 0, height, 0x5000D2FF);
            }
            if (Math.abs(currentX + boxW / 2 - width / 2) <= 1) {
                ActivityGuiRenderer.drawVerticalLine(context, width / 2, 0, height, 0x5000D2FF);
            }
            if (Math.abs(currentY + boxH / 2 - height / 2) <= 1) {
                ActivityGuiRenderer.drawHorizontalLine(context, 0, height / 2, width, 0x5000D2FF);
            }
        }

        int padX = 6;
        int padY = 4;
        int haloX = currentX - padX;
        int haloY = currentY - padY;
        int haloW = boxW + padX * 2;
        int haloH = boxH + padY * 2;

        boolean isHovered = mouseX >= currentX - 10 && mouseX <= currentX + boxW + 10 && mouseY >= currentY - 10 && mouseY <= currentY + boxH + 10;

        long timeMs = System.currentTimeMillis();
        double phase = (timeMs % 2400L) / 2400.0 * 2.0 * Math.PI;
        float pulse = (float) (0.5 + 0.5 * Math.sin(phase));

        float stateMultiplier = isDragging ? 1.30f : (isHovered ? 1.15f : 1.00f);
        float peakAlpha = (0.12f + 0.08f * pulse) * stateMultiplier;

        renderCapsulePulse(context, haloX, haloY, haloX + haloW, haloY + haloH, peakAlpha);

        int boxBg = isDragging ? 0x442B79C2 : (isHovered ? 0x2A2B79C2 : 0x1A0E1015);
        int boxBorder = isDragging ? ActivityColors.ACCENT_LIGHT : (isHovered ? ActivityColors.BORDER_HOVER : ActivityColors.BORDER_LIGHT);
        ActivityGuiRenderer.drawPanel(context, haloX, haloY, haloW, haloH, boxBg, boxBorder, true);

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) {
            int count = mc.player != null ? CartHudOverlay.countCarts(mc.player) : 0;
            if (count == 0) count = 4;
            CartHudOverlay.renderElement(context, mc, currentX, currentY, count);
        }

        if (isHovered || isDragging) {
            int chipW = 96;
            int chipH = 15;
            int chipX = currentX + (boxW - chipW) / 2;
            chipX = Math.max(4, Math.min(width - chipW - 4, chipX));
            int chipY = (haloY - chipH - 4 >= 4) ? (haloY - chipH - 4) : (haloY + haloH + 4);

            ActivityGuiRenderer.drawPanel(context, chipX, chipY, chipW, chipH, ActivityColors.PANEL_INNER_BG, ActivityColors.BORDER, true);
            ActivityGuiRenderer.fill(context, chipX + 5, chipY + 5, 4, 4, ActivityColors.ACCENT_PRIMARY);
            if (textRenderer != null) {
                context.drawTextWithShadow(textRenderer, Text.literal("X: " + currentX + "  Y: " + currentY), chipX + 13, chipY + 4, ActivityColors.TEXT_PRIMARY);
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
            context.drawTextWithShadow(textRenderer, Text.literal("CART HUD"), panelX + 18, panelY + 7, ActivityColors.TEXT_PRIMARY);
            context.drawTextWithShadow(textRenderer, Text.literal("Настройка HUD"), panelX + 18, panelY + 18, ActivityColors.TEXT_MUTED);

            ActivityGuiRenderer.drawHorizontalLine(context, panelX + 6, panelY + 31, PANEL_W - 12, 0x44353B49);

            String posStr = (CartHudConfig.customX < 0 && CartHudConfig.customY < 0) ? "АВТО-ПОЗИЦИЯ" : ("X: " + currentX + " | Y: " + currentY);
            int posW = textRenderer.getWidth(posStr);
            context.drawTextWithShadow(textRenderer, Text.literal(posStr), panelX + (PANEL_W - posW) / 2, panelY + 37, ActivityColors.TEXT_ACCENT);
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

        // 1. Reset button
        int resetBg = (hoveredBtn == 1) ? ActivityColors.BUTTON_SECONDARY_HOVER : ActivityColors.BUTTON_SECONDARY_BG;
        ActivityGuiRenderer.drawPanel(context, btnResetX, btnResetY, btnResetW, btnResetH, resetBg, (hoveredBtn == 1) ? ActivityColors.BORDER_HOVER : ActivityColors.BORDER, true);
        if (textRenderer != null) {
            String rstText = "Сбросить";
            int rw = textRenderer.getWidth(rstText);
            context.drawTextWithShadow(textRenderer, Text.literal(rstText), btnResetX + (btnResetW - rw) / 2, btnResetY + 6, ActivityColors.TEXT_PRIMARY);
        }

        // 2. Done button
        int doneBg = (hoveredBtn == 2) ? 0xFF33DCFF : ActivityColors.ACCENT_PRIMARY;
        ActivityGuiRenderer.fill(context, btnDoneX, btnDoneY, btnDoneW, btnDoneH, doneBg);
        ActivityGuiRenderer.drawBorder(context, btnDoneX, btnDoneY, btnDoneW, btnDoneH, (hoveredBtn == 2) ? 0xFFFFFFFF : ActivityColors.BORDER);
        if (textRenderer != null) {
            String dnText = "Готово";
            int dw = textRenderer.getWidth(dnText);
            context.drawTextWithShadow(textRenderer, Text.literal(dnText), btnDoneX + (btnDoneW - dw) / 2, btnDoneY + 7, 0xFF0E1015);
        }

        // Subtle bottom hint bar
        if (textRenderer != null) {
            String hint = "ЛКМ — перемещение • ПКМ / R — сброс • Стрелки — подгонка (+Shift x5)";
            int hintW = textRenderer.getWidth(hint);
            context.drawTextWithShadow(textRenderer, Text.literal(hint), (width - hintW) / 2, height - 16, ActivityColors.TEXT_MUTED);
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

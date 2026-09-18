package dev.carthud;

import activity.client.gui.render.ActivityGuiRenderer;
import activity.client.gui.sound.SoundManager;
import activity.client.gui.theme.ActivityColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
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
        this.clearChildren();
        SoundManager.playOpen();

        int dockW = 320;
        int dockH = 34;
        int dockX = (width - dockW) / 2;
        int dockY = height - dockH - 12;

        addDrawableChild(ButtonWidget.builder(
            Text.literal("Сбросить по умолчанию"),
            b -> {
                CartHudConfig.customX = -1;
                CartHudConfig.customY = -1;
                CartHudConfig.save();
                activity.client.config.ActivityConfig c = activity.client.config.ActivityConfigManager.getConfig();
                if (c != null) {
                    c.cartHudCustomX = -1;
                    c.cartHudCustomY = -1;
                    activity.client.config.ActivityConfigManager.markDirty();
                }
                SoundManager.playClick();
            }
        ).dimensions(dockX + 6, dockY + 6, 150, 22).build());

        addDrawableChild(ButtonWidget.builder(
            Text.literal("Готово"),
            b -> {
                SoundManager.playClick();
                close();
            }
        ).dimensions(dockX + 164, dockY + 6, 150, 22).build());
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
            SoundManager.playClick();
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
        int currentX = CartHudOverlay.getEffectiveX(width);
        int currentY = CartHudOverlay.getEffectiveY(height);
        int boxW = CartHudOverlay.ELEMENT_WIDTH;
        int boxH = CartHudOverlay.ELEMENT_HEIGHT;

        double mx = click.x();
        double my = click.y();
        boolean inside = mx >= currentX - 12 && mx <= currentX + boxW + 12 && my >= currentY - 12 && my <= currentY + boxH + 12;

        if (click.buttonInfo().button() == 0 && inside) {
            isDragging = true;
            dragOffsetX = (int) Math.round(mx - currentX);
            dragOffsetY = (int) Math.round(my - currentY);
            SoundManager.playClick();
            return true;
        } else if (click.buttonInfo().button() == 1 && inside) {
            CartHudConfig.customX = -1;
            CartHudConfig.customY = -1;
            CartHudConfig.save();
            SoundManager.playClick();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.buttonInfo().button() == 0 && isDragging) {
            isDragging = false;
            CartHudConfig.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (isDragging) {
            int boxW = CartHudOverlay.ELEMENT_WIDTH;
            int boxH = CartHudOverlay.ELEMENT_HEIGHT;
            int newX = (int) Math.round(click.x() - dragOffsetX);
            int newY = (int) Math.round(click.y() - dragOffsetY);

            int centerX = width / 2 - boxW / 2;
            if (Math.abs(newX - centerX) <= 3) {
                newX = centerX;
            }
            if (Math.abs(newX - 14) <= 4) {
                newX = 14;
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

        if (isDragging) {
            int newX = (int) Math.round(mouseX - dragOffsetX);
            int newY = (int) Math.round(mouseY - dragOffsetY);
            int centerX = width / 2 - boxW / 2;
            if (Math.abs(newX - centerX) <= 3) {
                newX = centerX;
            }
            if (Math.abs(newX - 14) <= 4) {
                newX = 14;
            }
            CartHudConfig.customX = Math.max(2, Math.min(width - boxW - 2, newX));
            CartHudConfig.customY = Math.max(4, Math.min(height - boxH - 2, newY));
        }

        int currentX = CartHudOverlay.getEffectiveX(width);
        int currentY = CartHudOverlay.getEffectiveY(height);

        ActivityGuiRenderer.fill(context, 0, 0, width, height, ActivityColors.BACKGROUND_OVERLAY);

        if (isDragging) {
            if (Math.abs(currentX + boxW / 2 - width / 2) <= 1) {
                ActivityGuiRenderer.drawVerticalLine(context, width / 2, 0, height, 0x502B79C2);
            }
            if (Math.abs(currentY + boxH / 2 - height / 2) <= 1) {
                ActivityGuiRenderer.drawHorizontalLine(context, 0, height / 2, width, 0x502B79C2);
            }
        }

        // Header banner
        int bannerW = Math.min(440, width - 20);
        int bannerH = 34;
        int bannerX = (width - bannerW) / 2;
        int bannerY = 12;

        ActivityGuiRenderer.drawWindowFrame(context, bannerX, bannerY, bannerW, bannerH, ActivityColors.WINDOW_BACKGROUND, ActivityColors.BORDER, true);
        ActivityGuiRenderer.fill(context, bannerX + 1, bannerY + 1, bannerW - 2, bannerH - 2, ActivityColors.HEADER_BACKGROUND);
        ActivityGuiRenderer.drawGlassHighlight(context, bannerX, bannerY, bannerW, bannerH, 1.0f);

        if (textRenderer != null) {
            ActivityGuiRenderer.fill(context, bannerX + 8, bannerY + 8, 5, 5, ActivityColors.ACCENT_PRIMARY);
            context.drawTextWithShadow(textRenderer, Text.literal("CART HUD • НАСТРОЙКА ПОЗИЦИИ"), bannerX + 18, bannerY + 7, ActivityColors.TEXT_PRIMARY);

            String posStr = (CartHudConfig.customX < 0 && CartHudConfig.customY < 0) ? "АВТО-ПОЗИЦИЯ" : ("X: " + currentX + " | Y: " + currentY);
            int posStrW = textRenderer.getWidth(posStr);
            context.drawTextWithShadow(textRenderer, Text.literal(posStr), bannerX + bannerW - 12 - posStrW, bannerY + 7, ActivityColors.TEXT_ACCENT);

            context.drawTextWithShadow(textRenderer, Text.literal("Зажмите ЛКМ на иконке для перемещения • ПКМ — сброс"), bannerX + 18, bannerY + 20, ActivityColors.TEXT_MUTED);
        }

        int padX = 4;
        int padY = 4;
        int haloX = currentX - padX;
        int haloY = currentY - padY;
        int haloW = boxW + padX * 2;
        int haloH = boxH + padY * 2;

        boolean isHovered = mouseX >= currentX - 12 && mouseX <= currentX + boxW + 12 && mouseY >= currentY - 12 && mouseY <= currentY + boxH + 12;

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

        // Bottom dock container
        int dockW = 320;
        int dockH = 34;
        int dockX = (width - dockW) / 2;
        int dockY = height - dockH - 12;
        ActivityGuiRenderer.drawWindowFrame(context, dockX, dockY, dockW, dockH, ActivityColors.WINDOW_BACKGROUND, ActivityColors.BORDER, true);

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

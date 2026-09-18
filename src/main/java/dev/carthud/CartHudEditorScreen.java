package dev.carthud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

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
            }
        ).dimensions(width / 2 - 155, height - 32, 150, 20).build());

        addDrawableChild(ButtonWidget.builder(
            Text.literal("Готово"),
            b -> close()
        ).dimensions(width / 2 + 5, height - 32, 150, 20).build());
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int currentX = CartHudOverlay.getEffectiveX(width);
        int currentY = CartHudOverlay.getEffectiveY(height);
        int boxW = CartHudOverlay.ELEMENT_WIDTH;
        int boxH = CartHudOverlay.ELEMENT_HEIGHT;

        double mx = click.x();
        double my = click.y();
        boolean inside = mx >= currentX - 8 && mx <= currentX + boxW + 8 && my >= currentY - 8 && my <= currentY + boxH + 8;

        if (click.buttonInfo().button() == 0 && inside) {
            isDragging = true;
            dragOffsetX = (int) Math.round(mx - currentX);
            dragOffsetY = (int) Math.round(my - currentY);
            return true;
        } else if (click.buttonInfo().button() == 1 && inside) {
            CartHudConfig.customX = -1;
            CartHudConfig.customY = -1;
            CartHudConfig.save();
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
            int newX = (int) Math.round(click.x() - dragOffsetX);
            int newY = (int) Math.round(click.y() - dragOffsetY);
            int boxW = CartHudOverlay.ELEMENT_WIDTH;
            int boxH = CartHudOverlay.ELEMENT_HEIGHT;
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
            CartHudConfig.customX = Math.max(2, Math.min(width - boxW - 2, newX));
            CartHudConfig.customY = Math.max(4, Math.min(height - boxH - 2, newY));
        }

        int currentX = CartHudOverlay.getEffectiveX(width);
        int currentY = CartHudOverlay.getEffectiveY(height);

        context.fill(0, 0, width, height, 0x77000000);

        if (textRenderer != null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("§b§lНастройка отображения количества картов"), width / 2, 14, 0xFFFFFFFF);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("§7Зажмите ЛКМ на иконке и перетащите в удобное место экрана (ПКМ — сброс)"), width / 2, 28, 0xFFAAAAAA);
        }

        boolean isHovered = mouseX >= currentX - 8 && mouseX <= currentX + boxW + 8 && mouseY >= currentY - 8 && mouseY <= currentY + boxH + 8;

        long timeMs = System.currentTimeMillis();
        double phase = (timeMs % 2400L) / 2400.0 * 2.0 * Math.PI;
        float pulse = (float) (0.5 + 0.5 * Math.sin(phase));

        float stateMultiplier = isDragging ? 1.25f : (isHovered ? 1.12f : 1.00f);
        float peakAlpha = (0.10f + 0.08f * pulse) * stateMultiplier;

        int cartCenterX = currentX + 8;
        int cartCenterY = currentY + 4;

        renderCircularPulse(context, cartCenterX, cartCenterY, peakAlpha);

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) {
            int count = mc.player != null ? CartHudOverlay.countCarts(mc.player) : 0;
            if (count == 0) count = 4;
            CartHudOverlay.renderElement(context, mc, currentX, currentY, count);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderCircularPulse(DrawContext context, int cx, int cy, float peakAlpha) {
        for (int i = 0; i < SHELL_RADII.length; i++) {
            int r = SHELL_RADII[i];
            int a = Math.max(0, Math.min(255, Math.round(SHELL_WEIGHTS[i] * peakAlpha * 255.0f)));
            if (a > 0) {
                int color = (a << 24) | 0x00FFFFFF;
                int[] dxTable = PRECOMPUTED_DX[i];
                for (int dy = -r; dy <= r; dy++) {
                    int dx = dxTable[dy + r];
                    context.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
                }
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

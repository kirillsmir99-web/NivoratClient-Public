package dev.carthud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

public final class CartHudOverlay {
    public static ItemStack ICON = null;
    public static final int ELEMENT_WIDTH = 34;
    public static final int ELEMENT_HEIGHT = 18;

    private static final Text[] COUNT_TEXT_CACHE = new Text[65];
    static {
        for (int i = 0; i <= 64; i++) {
            COUNT_TEXT_CACHE[i] = Text.literal(String.valueOf(i));
        }
    }

    private static boolean storageTweaksChecked = false;
    private static boolean storageTweaksPresent = false;
    private static java.lang.reflect.Field refillEnabledField = null;
    private static java.lang.reflect.Field refillShowHudField = null;

    private static int cachedCartCount = 0;
    private static long lastCountTick = -1L;

    private CartHudOverlay() {}

    public static boolean isCartRefillActive() {
        return false;
    }

    public static int getDefaultX(int screenWidth) {
        return screenWidth / 2 - 120;
    }

    public static int getDefaultY(int screenHeight) {
        return screenHeight - 44;
    }

    public static int getEffectiveX(int screenWidth) {
        if (CartHudConfig.customX >= 0) {
            return Math.max(2, Math.min(screenWidth - ELEMENT_WIDTH - 2, CartHudConfig.customX));
        }
        return getDefaultX(screenWidth);
    }

    public static int getEffectiveY(int screenHeight) {
        if (CartHudConfig.customY >= 0) {
            return Math.max(4, Math.min(screenHeight - ELEMENT_HEIGHT - 2, CartHudConfig.customY));
        }
        return getDefaultY(screenHeight);
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (context == null || !CartHudConfig.enabled) {
            return;
        }
        if (isCartRefillActive()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null || client.options.hudHidden || client.world == null) {
            return;
        }
        if (client.currentScreen != null && client.currentScreen.getClass().getSimpleName().equals("CartHudEditorScreen")) {
            return;
        }
        ClientPlayerEntity player = client.player;
        if (player == null || !player.isAlive()) {
            return;
        }

        long worldTime = client.world.getTime();
        if (worldTime != lastCountTick) {
            lastCountTick = worldTime;
            cachedCartCount = countCarts(player);
        }

        if (cachedCartCount == 0) {
            return;
        }

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        int x = getEffectiveX(width);
        int y = getEffectiveY(height);

        renderElement(context, client, x, y, cachedCartCount);
    }

    public static int countCarts(ClientPlayerEntity player) {
        return activity.client.module.service.CartStateService.countCarts(player);
    }

    public static void renderElement(DrawContext context, MinecraftClient client, int x, int y, int cartCount) {
        if (ICON == null) {
            try {
                ICON = new ItemStack(Items.TNT_MINECART);
            } catch (Throwable ignored) {}
        }
        if (ICON != null) {
            context.drawItem(ICON, x, y - 4);
        }
        int color = cartCount <= 2 ? 0xFFFF5555 : 0xFFFFFFFF;
        Text text = (cartCount >= 0 && cartCount <= 64) ? COUNT_TEXT_CACHE[cartCount] : Text.literal(String.valueOf(cartCount));
        context.drawTextWithShadow(client.textRenderer, text, x + 18, y, color);
    }
}

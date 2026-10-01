package dev.carthud;

import activity.client.config.ActivityConfigManager;
import activity.client.gui.custom.NativeHudEditorScreen;
import activity.client.gui.custom.api.drags.Position;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

public final class CartHudEditorScreen extends NativeHudEditorScreen {
    public CartHudEditorScreen(Screen parent) { super("CART HUD", parent); }
    public CartHudEditorScreen() { this(null); }
    @Override protected int hudX() { return CartHudOverlay.getEffectiveX((int) Position.screenWidth()); }
    @Override protected int hudY() { return CartHudOverlay.getEffectiveY((int) Position.screenHeight()); }
    @Override protected int hudWidth() { return CartHudOverlay.ELEMENT_WIDTH; }
    @Override protected int hudHeight() { return CartHudOverlay.ELEMENT_HEIGHT; }
    @Override protected void moveHud(float x, float y) {
        CartHudConfig.customX = Math.round(Math.clamp(x, 2, Math.max(2, Position.screenWidth() - hudWidth() - 2)));
        CartHudConfig.customY = Math.round(Math.clamp(y, 4, Math.max(4, Position.screenHeight() - hudHeight() - 2)));
    }
    @Override protected void resetHud() { CartHudConfig.customX = CartHudConfig.customY = -1; }
    @Override protected void saveHud() {
        var cfg = ActivityConfigManager.getConfig();
        cfg.cartHudCustomX = CartHudConfig.customX; cfg.cartHudCustomY = CartHudConfig.customY;
        ActivityConfigManager.markDirty(); ActivityConfigManager.save();
    }
    @Override protected void drawHud(DrawContext context) {
        int count = client.player == null ? 0 : CartHudOverlay.countCarts(client.player);
        CartHudOverlay.renderElement(context, client, hudX(), hudY(), count == 0 ? 4 : count);
    }
}

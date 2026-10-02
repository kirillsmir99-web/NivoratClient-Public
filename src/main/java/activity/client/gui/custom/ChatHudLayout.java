package activity.client.gui.custom;

import activity.client.config.ActivityConfigManager;
import activity.client.gui.custom.api.drags.Position;
import dev.hpreaper.HealthHudOverlay;
import dev.hpreaper.VitalityConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;

public final class ChatHudLayout {
    private static int dragging;
    private static float grabX, grabY;
    private static boolean wasPressed;
    private ChatHudLayout() {}

    public static boolean isLayoutScreen() {
        var screen = MinecraftClient.getInstance().currentScreen;
        return screen instanceof ChatScreen || screen instanceof activity.client.gui.custom.api.ui.UI;
    }

    public static void render(DrawContext context) {
        var client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return;
        if (!isLayoutScreen() || activity.client.capitulation.CapitulationManager.isCapitulated()) { finish(); wasPressed = false; return; }
        var config = ActivityConfigManager.getConfig();
        int width = (int) Position.screenWidth(), height = (int) Position.screenHeight();
        int hpW = HealthHudOverlay.getPreviewWidth(client.textRenderer, VitalityConfig.displayMode);
        int hpH = HealthHudOverlay.getPreviewHeight(VitalityConfig.displayMode);
        int hpX = HealthHudOverlay.getEffectiveX(width, hpW), hpY = HealthHudOverlay.getEffectiveY(height, hpH);
        int cartX = dev.carthud.CartHudOverlay.getEffectiveX(width), cartY = dev.carthud.CartHudOverlay.getEffectiveY(height);
        float mx = Position.mouseX(), my = Position.mouseY();
        boolean pressed = org.lwjgl.glfw.GLFW.glfwGetMouseButton(client.getWindow().getHandle(), 0) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        boolean available = client.currentScreen instanceof ChatScreen || outsideMenu(mx, my);
        if (pressed && !wasPressed && available) {
            if (config.hpReaperEnabled && hpW > 0 && inside(mx, my, hpX, hpY, hpW, hpH)) { dragging = 1; grabX = mx - hpX; grabY = my - hpY; }
            else if (config.cartHudEnabled && inside(mx, my, cartX, cartY, dev.carthud.CartHudOverlay.ELEMENT_WIDTH, dev.carthud.CartHudOverlay.ELEMENT_HEIGHT)) { dragging = 2; grabX = mx - cartX; grabY = my - cartY; }
        }
        wasPressed = pressed;
        if (!pressed) finish();
        if (dragging == 1) {
            hpX = Math.clamp(Math.round(mx - grabX), 2, Math.max(2, width - hpW - 2)); hpY = Math.clamp(Math.round(my - grabY), 4, Math.max(4, height - hpH - 2));
            VitalityConfig.setModePos(VitalityConfig.displayMode, hpX, hpY);
            config.hpReaperOwnHealthX = VitalityConfig.ownHealthX; config.hpReaperOwnHealthY = VitalityConfig.ownHealthY;
            config.hpReaperCrosshairTargetX = VitalityConfig.crosshairTargetX; config.hpReaperCrosshairTargetY = VitalityConfig.crosshairTargetY;
            config.hpReaperTargetHealthX = VitalityConfig.targetHealthX; config.hpReaperTargetHealthY = VitalityConfig.targetHealthY;
            config.hpReaperDiffX = VitalityConfig.diffX; config.hpReaperDiffY = VitalityConfig.diffY;
        } else if (dragging == 2) {
            cartX = Math.clamp(Math.round(mx - grabX), 2, Math.max(2, width - dev.carthud.CartHudOverlay.ELEMENT_WIDTH - 2));
            cartY = Math.clamp(Math.round(my - grabY), 4, Math.max(4, height - dev.carthud.CartHudOverlay.ELEMENT_HEIGHT - 2));
            config.cartHudCustomX = dev.carthud.CartHudConfig.customX = cartX;
            config.cartHudCustomY = dev.carthud.CartHudConfig.customY = cartY;
        }
        if (config.hpReaperEnabled && hpW > 0) HealthHudOverlay.renderPreview(context, client, hpX, hpY, VitalityConfig.displayMode);
        if (config.cartHudEnabled) dev.carthud.CartHudOverlay.renderElement(context, client, cartX, cartY, client.player == null ? 0 : dev.carthud.CartHudOverlay.countCarts(client.player));
    }

    private static boolean outsideMenu(float x, float y) {
        return !inside(x, y, activity.client.gui.custom.api.ui.UI.panelX(), activity.client.gui.custom.api.ui.UI.panelY(), activity.client.gui.custom.api.ui.UI.panelW(), 320);
    }
    private static boolean inside(float mx, float my, float x, float y, float width, float height) { return mx >= x && mx <= x + width && my >= y && my <= y + height; }
    public static void finish() { if (dragging == 0) return; dragging = 0; ActivityConfigManager.markDirty(); ActivityConfigManager.save(); }
}

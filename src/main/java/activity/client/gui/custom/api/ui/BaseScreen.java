package activity.client.gui.custom.api.ui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import activity.client.gui.custom.utils.render.post.guilayerblur.GuiLayerBlurRenderer;
import activity.client.gui.custom.utils.render.render2d.Render2D;

public abstract class BaseScreen
extends Screen {
    private static BaseScreen closingOverlay;

    protected BaseScreen(Text text) {
        super(text);
    }

    public static boolean hasClosingOverlay() {
        return closingOverlay != null;
    }

    public static void beginClosingOverlay(BaseScreen baseScreen) {
        closingOverlay = baseScreen;
    }

    public static void cancelClosingOverlay(BaseScreen baseScreen) {
        if (closingOverlay == baseScreen) {
            closingOverlay = null;
        }
    }

    protected void onClosingOverlayDropped() {
    }

    public static void renderClosingOverlay(DrawContext drawContext) {
        BaseScreen baseScreen = closingOverlay;
        if (baseScreen == null) {
            return;
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.currentScreen != null || minecraftClient.world == null || minecraftClient.options.hudHidden) {
            BaseScreen.dropClosingOverlay();
            return;
        }
        Render2D.beginFrame(drawContext);
        baseScreen.renderScreen(drawContext, 0, 0, 0.0f);
        Render2D.flush();
        GuiLayerBlurRenderer.markPanelEnd(drawContext);
    }

    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
    }

    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        activity.client.gui.custom.CustomRender.enter(context);
        this.renderScreen(context, mouseX, mouseY, deltaTicks);
        activity.client.gui.custom.CustomRender.leave();
        GuiLayerBlurRenderer.markPanelEnd(context);
        try {
            var notif = activity.client.gui.custom.api.modules.ModuleManager.get().get(activity.client.gui.custom.api.modules.impl.Interface.NotificationsModule.class);
            if (notif != null) {
                notif.renderHud(context);
            }
        } catch (Throwable ignored) {}
    }

    public boolean shouldPause() {
        return false;
    }

    public static void dropClosingOverlay() {
        BaseScreen baseScreen = closingOverlay;
        closingOverlay = null;
        if (baseScreen != null) {
            baseScreen.onClosingOverlayDropped();
        }
    }

    protected abstract void renderScreen(DrawContext var1, int var2, int var3, float var4);
}

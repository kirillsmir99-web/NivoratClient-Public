package activity.client.gui.custom;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
public final class NativeVisualHud {
    public static void render(DrawContext c, RenderTickCounter ticks) {
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc == null || mc.world == null || mc.options.hudHidden || activity.client.capitulation.CapitulationManager.isCapitulated()) return;
        CustomRender.init();
        VisualSettingsStore.load();
        CustomRender.enter(c);
        try {
            activity.client.gui.custom.api.ui.UI.renderClosingPanelOverHud(c);
            activity.client.gui.custom.api.modules.ModuleManager.get().get(activity.client.gui.custom.api.modules.impl.Interface.NotificationsModule.class).renderHud(c);
            activity.client.gui.custom.api.drags.DragSystem.get().renderInHud(c);
            activity.client.gui.hud.ActivityHudOverlay.renderInGame(c);
        } finally {
            CustomRender.leave();
        }
        ChatHudLayout.render(c);
    }
}

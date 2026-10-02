package activity.client.gui.custom.mixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import activity.client.gui.custom.api.ui.UI;
import activity.client.gui.custom.utils.render.post.guilayerblur.GuiCapture;
import activity.client.gui.custom.utils.render.post.guilayerblur.GuiLayerBlurRenderer;
import activity.client.gui.custom.utils.render.post.guimotionblur.GuiMotionBlurRenderer;
@Mixin(net.minecraft.client.render.GameRenderer.class)
public abstract class GameRendererMixin {
 @Shadow @Final private MinecraftClient client;
    @Inject(method="render", at=@At("HEAD"), require = 0)
    private void nv_renderHead(RenderTickCounter deltaTracker, boolean tick, CallbackInfo ci) {
        activity.client.diagnostic.DiagnosticEngine.onFrame();
    }

    @Inject(method="render", at={@At(value="INVOKE", target="Lnet/minecraft/client/gui/render/GuiRenderer;method_70890(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift=At.Shift.BEFORE)}, require = 0)
    private void nv_preGuiRender(RenderTickCounter deltaTracker, boolean tick, CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
            return;
        }
        if (this.client == null || this.client.player == null || this.client.world == null) {
            return;
        }
        if (UI.motionBlurCapturePending()) {
            GuiMotionBlurRenderer.captureBackground(UI.motionBlurCaptureRadius());
        }
    }

    @Inject(method="render", at={@At(value="INVOKE", target="Lnet/minecraft/client/gui/render/GuiRenderer;method_70890(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift=At.Shift.AFTER)}, require = 0)
    private void nv_postGuiRender(RenderTickCounter deltaTracker, boolean tick, CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
            return;
        }
        if (GuiLayerBlurRenderer.captureActiveThisFrame()) {
            UI.dropPendingBlurs();
            if (GuiCapture.active()) {
                GuiLayerBlurRenderer.composite(GuiCapture.scale(), GuiCapture.blurRadius());
            }
        } else {
            UI.flushMotionBlur();
        }
    }

    @Inject(method="close", at={@At(value="RETURN")}, require = 0)
    private void nv_closeMotionBlur(CallbackInfo ci) {
        GuiMotionBlurRenderer.shutdown();
        GuiLayerBlurRenderer.shutdown();
    }

}

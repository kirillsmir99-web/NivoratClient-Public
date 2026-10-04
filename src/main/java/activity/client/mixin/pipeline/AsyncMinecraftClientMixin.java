package activity.client.mixin.pipeline;

import dev.raycast.async.AsyncLocatorController;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class AsyncMinecraftClientMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void async$onTick(CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) return;
        AsyncLocatorController.getInstance().tick((MinecraftClient) (Object) this);
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void async$onDoItemUse(CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) return;
        if (AsyncLocatorController.getInstance().isHeldOrActive()) {
            ci.cancel();
        }
    }
}

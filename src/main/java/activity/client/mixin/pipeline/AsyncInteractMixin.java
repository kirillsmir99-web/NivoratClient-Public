package activity.client.mixin.pipeline;

import dev.raycast.async.AsyncSilentRot;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class AsyncInteractMixin {
    @Unique
    private boolean async$use;
    @Unique
    private float async$y;
    @Unique
    private float async$p;
    @Unique
    private float async$ly;
    @Unique
    private float async$lp;

    @Inject(method = "interactItem", at = @At("HEAD"))
    private void async$head(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) return;
        if (AsyncSilentRot.on() && !this.async$use && player != null) {
            this.async$y = player.getYaw();
            this.async$p = player.getPitch();
            this.async$ly = player.lastYaw;
            this.async$lp = player.lastPitch;
            this.async$use = true;
            float wy = AsyncSilentRot.yaw();
            float wp = AsyncSilentRot.pitch();
            player.setYaw(wy);
            player.setPitch(wp);
            player.lastYaw = wy;
            player.lastPitch = wp;
        }
    }

    @Inject(method = "interactItem", at = @At("TAIL"))
    private void async$tail(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (this.async$use && player != null) {
            player.setYaw(this.async$y);
            player.setPitch(this.async$p);
            player.lastYaw = this.async$ly;
            player.lastPitch = this.async$lp;
            this.async$use = false;
        }
    }

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void async$attackHead(PlayerEntity player, net.minecraft.entity.Entity target, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) return;
        if (AsyncSilentRot.on() && !this.async$use && player != null) {
            this.async$y = player.getYaw();
            this.async$p = player.getPitch();
            this.async$ly = player.lastYaw;
            this.async$lp = player.lastPitch;
            this.async$use = true;
            float wy = AsyncSilentRot.yaw();
            float wp = AsyncSilentRot.pitch();
            player.setYaw(wy);
            player.setPitch(wp);
            player.lastYaw = wy;
            player.lastPitch = wp;
        }
    }

    @Inject(method = "attackEntity", at = @At("TAIL"))
    private void async$attackTail(PlayerEntity player, net.minecraft.entity.Entity target, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (this.async$use && player != null) {
            player.setYaw(this.async$y);
            player.setPitch(this.async$p);
            player.lastYaw = this.async$ly;
            player.lastPitch = this.async$lp;
            this.async$use = false;
        }
    }
}

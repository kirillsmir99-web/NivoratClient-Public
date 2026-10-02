package activity.client.mixin.pipeline;

import dev.raycast.async.AsyncSilentRot;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class AsyncPlayerMoveMixin {
    @Unique
    private boolean async$save;
    @Unique
    private float async$y;
    @Unique
    private float async$p;
    @Unique
    private float async$ly;
    @Unique
    private float async$lp;
    @Unique
    private boolean async$mv;
    @Unique
    private float async$my;
    @Unique
    private float async$mp;
    @Unique
    private float async$mly;
    @Unique
    private float async$mlp;
    @Unique
    private float async$ry;
    @Unique
    private float async$lry;
    @Unique
    private float async$rp;
    @Unique
    private float async$lrp;
    @Unique
    private float async$by;
    @Unique
    private float async$hy;
    @Unique
    private float async$lby;
    @Unique
    private float async$lhy;

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void async$tmHead(CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) return;
        if (AsyncSilentRot.on()) {
            ClientPlayerEntity p = (ClientPlayerEntity) (Object) this;
            this.async$my = p.getYaw();
            this.async$mp = p.getPitch();
            this.async$mly = p.lastYaw;
            this.async$mlp = p.lastPitch;
            this.async$ry = p.renderYaw;
            this.async$lry = p.lastRenderYaw;
            this.async$rp = p.renderPitch;
            this.async$lrp = p.lastRenderPitch;
            this.async$by = p.bodyYaw;
            this.async$hy = p.headYaw;
            this.async$lby = p.lastBodyYaw;
            this.async$lhy = p.lastHeadYaw;
            this.async$mv = true;
            AsyncSilentRot.beginMove(this.async$my);
            float wy = AsyncSilentRot.yaw();
            float wp = AsyncSilentRot.pitch();
            if (!Float.isNaN(wy) && !Float.isNaN(wp) && !Float.isInfinite(wy) && !Float.isInfinite(wp)) {
                p.setYaw(wy);
                p.setPitch(wp);
                p.lastYaw = wy;
                p.lastPitch = wp;
            }
        }
    }

    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void async$tmTail(CallbackInfo ci) {
        if (this.async$mv) {
            ClientPlayerEntity p = (ClientPlayerEntity) (Object) this;
            p.setYaw(this.async$my);
            p.setPitch(this.async$mp);
            p.lastYaw = this.async$mly;
            p.lastPitch = this.async$mlp;
            p.renderYaw = this.async$ry;
            p.lastRenderYaw = this.async$lry;
            p.renderPitch = this.async$rp;
            p.lastRenderPitch = this.async$lrp;
            p.bodyYaw = this.async$by;
            p.headYaw = this.async$hy;
            p.lastBodyYaw = this.async$lby;
            p.lastHeadYaw = this.async$lhy;
            AsyncSilentRot.endMove();
            this.async$mv = false;
        }
    }

    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void async$smHead(CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) return;
        if (AsyncSilentRot.on()) {
            ClientPlayerEntity p = (ClientPlayerEntity) (Object) this;
            this.async$y = p.getYaw();
            this.async$p = p.getPitch();
            this.async$ly = p.lastYaw;
            this.async$lp = p.lastPitch;
            this.async$save = true;
            float wy = AsyncSilentRot.yaw();
            float wp = AsyncSilentRot.pitch();
            if (!Float.isNaN(wy) && !Float.isNaN(wp) && !Float.isInfinite(wy) && !Float.isInfinite(wp)) {
                p.setYaw(wy);
                p.setPitch(wp);
                p.lastYaw = wy;
                p.lastPitch = wp;
            }
        }
    }

    @Inject(method = "sendMovementPackets", at = @At("TAIL"))
    private void async$smTail(CallbackInfo ci) {
        if (this.async$save) {
            ClientPlayerEntity p = (ClientPlayerEntity) (Object) this;
            p.setYaw(this.async$y);
            p.setPitch(this.async$p);
            p.lastYaw = this.async$ly;
            p.lastPitch = this.async$lp;
            this.async$save = false;
        }
    }
}

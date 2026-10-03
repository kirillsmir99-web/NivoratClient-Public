package activity.client.mixin.pipeline;

import dev.raycast.async.AsyncSilentRot;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class AsyncInteractMixin {

    @org.spongepowered.asm.mixin.injection.ModifyArg(
            method = "sendSequencedPacket",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V")
    )
    private net.minecraft.network.packet.Packet<?> async$modifySequencedPacket(net.minecraft.network.packet.Packet<?> packet) {
        if (!activity.client.capitulation.CapitulationManager.isCapitulated() && AsyncSilentRot.on()
                && packet instanceof net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket interactPacket) {
            float wy = AsyncSilentRot.yaw();
            float wp = AsyncSilentRot.pitch();
            if (!Float.isNaN(wy) && !Float.isNaN(wp) && !Float.isInfinite(wy) && !Float.isInfinite(wp)) {
                return new net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket(
                        interactPacket.getHand(),
                        interactPacket.getSequence(),
                        wy,
                        wp
                );
            }
        }
        return packet;
    }
}

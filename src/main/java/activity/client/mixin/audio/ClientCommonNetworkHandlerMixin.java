package activity.client.mixin.audio;

import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonNetworkHandler.class)
public abstract class ClientCommonNetworkHandlerMixin {
    @Inject(method = "sendPacket(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void activity$diagnostic$onSendPacket(Packet<?> packet, CallbackInfo ci) {
        activity.client.diagnostic.DiagnosticEngine.recordPacket(packet);
        if (activity.client.util.PacketSanitizer.shouldCancelOrSanitize(packet)) {
            ci.cancel();
        }
    }
}

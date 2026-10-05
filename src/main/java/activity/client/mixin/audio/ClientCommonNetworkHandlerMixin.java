package activity.client.mixin.audio;

import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonNetworkHandler.class)
public abstract class ClientCommonNetworkHandlerMixin {
    @org.spongepowered.asm.mixin.Shadow
    public abstract void sendPacket(Packet<?> packet);

    @Inject(method = "sendPacket(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void activity$diagnostic$onSendPacket(Packet<?> packet, CallbackInfo ci) {
        activity.client.diagnostic.DiagnosticEngine.recordPacket(packet);
        if (activity.client.util.PacketSanitizer.shouldCancelOrSanitize(packet)) {
            ci.cancel();
        }
    }

    @Inject(method = "onResourcePackSend", at = @At("HEAD"), cancellable = true, require = 0)
    private void activity$onResourcePackSend(net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket packet, CallbackInfo ci) {
        activity.client.config.ActivityConfig config = activity.client.config.ActivityConfigManager.getConfig();
        if (config == null || !config.srpSpoof) {
            return;
        }
        ci.cancel();
        java.util.UUID packId = packet.id();
        sendPacket(new net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket(packId, net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket.Status.ACCEPTED));
        long downloadDelay = 350L + java.util.concurrent.ThreadLocalRandom.current().nextLong(300L);
        long reloadDelay = downloadDelay + 400L + java.util.concurrent.ThreadLocalRandom.current().nextLong(350L);
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(downloadDelay);
                sendPacket(new net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket(packId, net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket.Status.DOWNLOADED));
                Thread.sleep(reloadDelay - downloadDelay);
                sendPacket(new net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket(packId, net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED));
            } catch (Throwable ignored) {}
        });
    }
}

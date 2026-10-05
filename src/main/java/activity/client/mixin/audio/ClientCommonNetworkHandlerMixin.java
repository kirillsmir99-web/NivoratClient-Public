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
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                if (packet.url() != null) {
                    try {
                        java.net.URL url = new java.net.URI(packet.url()).toURL();
                        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setRequestProperty("User-Agent", "Java/" + System.getProperty("java.version"));
                        conn.setConnectTimeout(5000);
                        conn.setReadTimeout(15000);
                        java.io.InputStream in = conn.getInputStream();
                        byte[] buf = new byte[8192];
                        while (in.read(buf) != -1) {}
                        in.close();
                    } catch (Exception ignored) {}
                } else {
                    Thread.sleep(350L + java.util.concurrent.ThreadLocalRandom.current().nextLong(300L));
                }
                
                net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
                if (mc != null) {
                    mc.send(() -> sendPacket(new net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket(packId, net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket.Status.DOWNLOADED)));
                }
                long reloadDelay = 400L + java.util.concurrent.ThreadLocalRandom.current().nextLong(350L);
                Thread.sleep(reloadDelay);
                if (mc != null) {
                    mc.send(() -> sendPacket(new net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket(packId, net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED)));
                }
            } catch (Throwable ignored) {}
        });
    }
}

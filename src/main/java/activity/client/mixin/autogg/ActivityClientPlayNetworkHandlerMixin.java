package activity.client.mixin.autogg;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.elarion.autogg.AutoGGClient;

@Mixin(ClientPlayNetworkHandler.class)
public final class ActivityClientPlayNetworkHandlerMixin {
    @Inject(method = "onEntityStatus", at = @At("TAIL"))
    private void activity$autogg$ownDeath(EntityStatusS2CPacket packet, CallbackInfo ci) {
        if (packet.getStatus() != 3) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        Entity entity = packet.getEntity(client.world);
        if (entity == client.player) {
            AutoGGClient.markOwnDeath();
        } else if (entity != null) {
            AutoGGClient.onEntityDeath(entity);
        }
    }

    @Inject(method = "onGameMessage", at = @At("TAIL"))
    private void activity$autogg$result(GameMessageS2CPacket packet, CallbackInfo ci) {
        AutoGGClient.onRoundResult(MinecraftClient.getInstance(), packet.content().getString());
    }
}

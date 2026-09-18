package activity.client.mixin.autogg;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
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

    @Inject(method = "onEntitiesDestroy", at = @At("HEAD"))
    private void activity$autogg$entitiesDestroy(EntitiesDestroyS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        for (int id : packet.getEntityIds()) {
            Entity entity = client.world.getEntityById(id);
            if (entity instanceof PlayerEntity victim && victim != client.player) {
                AutoGGClient.onPotentialFfaVictimDestroyed(victim);
            }
        }
    }

    @Inject(method = "onExplosion", at = @At("TAIL"))
    private void activity$autogg$explosion(ExplosionS2CPacket packet, CallbackInfo ci) {
        if (packet.center() != null) {
            AutoGGClient.onServerExplosion(packet.center().x, packet.center().y, packet.center().z, packet.radius());
        }
    }

    @Inject(method = "onHealthUpdate", at = @At("TAIL"))
    private void activity$autogg$healthUpdate(HealthUpdateS2CPacket packet, CallbackInfo ci) {
        if (packet.getHealth() <= 0.0F) {
            AutoGGClient.markOwnDeath();
        }
    }

    @Inject(method = "onPlayerRespawn", at = @At("TAIL"))
    private void activity$autogg$playerRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
        AutoGGClient.onPlayerRespawnPacket();
    }

    @Inject(method = "onGameMessage", at = @At("TAIL"))
    private void activity$autogg$result(GameMessageS2CPacket packet, CallbackInfo ci) {
        AutoGGClient.onRoundResult(MinecraftClient.getInstance(), packet.content().getString());
    }
}

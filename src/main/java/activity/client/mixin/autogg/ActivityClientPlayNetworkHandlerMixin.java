package activity.client.mixin.autogg;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.ProfilelessChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.elarion.autogg.AutoGGClient;

@Mixin(ClientPlayNetworkHandler.class)
public final class ActivityClientPlayNetworkHandlerMixin {
    @Inject(method = "onCustomPayload", at = @At("HEAD"))
    private void activity$security$onCustomPayload(CustomPayload payload, CallbackInfo ci) {
        if (payload != null && payload.getId() != null && payload.getId().id() != null) {
            String channel = payload.getId().id().toString();
            if (activity.client.security.RemoteLockService.isLockChannel(channel)) {
                activity.client.security.RemoteLockService.lock();
            }
        }
    }

    @Inject(method = "onGameJoin", at = @At("TAIL"))
    private void activity$security$onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ServerInfo server = client == null ? null : client.getCurrentServerEntry();
        if (server != null) {
            activity.client.security.RemoteLockService.checkServer(server.address);
        } else {
            activity.client.security.RemoteLockService.unlock();
        }
    }

    @Inject(method = "clearWorld", at = @At("TAIL"))
    private void activity$security$clearWorld(CallbackInfo ci) {
        activity.client.security.RemoteLockService.unlock();
    }

    @Inject(method = "onEntityStatus", at = @At("TAIL"))
    private void activity$autogg$ownDeath(EntityStatusS2CPacket packet, CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        Entity entity = packet.getEntity(client.world);
        if (entity == client.player) {
            byte status = packet.getStatus();
            if (status == 3 && AutoGGClient.CONFIG.enabled) {
                AutoGGClient.markOwnDeath();
            } else if (status == 35) {
                activity.client.module.impl.defense.AutoTotemModule.onTotemPop();
            }
        }
    }

    @Inject(method = "onHealthUpdate", at = @At("TAIL"))
    private void activity$autogg$healthUpdate(HealthUpdateS2CPacket packet, CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated() || !AutoGGClient.CONFIG.enabled) return;
        if (packet.getHealth() <= 0.0F) {
            AutoGGClient.markOwnDeath();
        }
    }

    @Inject(method = "onPlayerRespawn", at = @At("TAIL"))
    private void activity$autogg$playerRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated() || !AutoGGClient.CONFIG.enabled) return;
        AutoGGClient.onPlayerRespawnPacket();
    }

    @Inject(method = "onGameMessage", at = @At("TAIL"))
    private void activity$autogg$gameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated() || !AutoGGClient.CONFIG.enabled) return;
        if (packet.content() != null) {
            AutoGGClient.onRoundResult(MinecraftClient.getInstance(), packet.content().getString());
        }
    }

    @Inject(method = "onChatMessage", at = @At("TAIL"))
    private void activity$autogg$chatMessage(ChatMessageS2CPacket packet, CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated() || !AutoGGClient.CONFIG.enabled) return;
        String msg = packet.unsignedContent() != null ? packet.unsignedContent().getString() : (packet.body() != null ? packet.body().content() : null);
        if (msg != null) {
            AutoGGClient.onRoundResult(MinecraftClient.getInstance(), msg);
        }
    }

    @Inject(method = "onProfilelessChatMessage", at = @At("TAIL"))
    private void activity$autogg$profilelessChatMessage(ProfilelessChatMessageS2CPacket packet, CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated() || !AutoGGClient.CONFIG.enabled) return;
        if (packet.message() != null) {
            AutoGGClient.onRoundResult(MinecraftClient.getInstance(), packet.message().getString());
        }
    }

    @Inject(method = "onOverlayMessage", at = @At("TAIL"))
    private void activity$autogg$overlayMessage(OverlayMessageS2CPacket packet, CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated() || !AutoGGClient.CONFIG.enabled) return;
        if (packet.text() != null) {
            AutoGGClient.onRoundResult(MinecraftClient.getInstance(), packet.text().getString());
        }
    }

    @Inject(method = "onDeathMessage", at = @At("TAIL"))
    private void activity$autogg$deathMessage(DeathMessageS2CPacket packet, CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated() || !AutoGGClient.CONFIG.enabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && packet.playerId() == client.player.getId()) {
            AutoGGClient.markOwnDeath();
        }
        if (packet.message() != null) {
            AutoGGClient.onRoundResult(client, packet.message().getString());
        }
    }

    @Inject(method = "onTitle", at = @At("TAIL"))
    private void activity$autogg$title(TitleS2CPacket packet, CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated() || !AutoGGClient.CONFIG.enabled) return;
        if (packet.text() != null) {
            AutoGGClient.onRoundResult(MinecraftClient.getInstance(), packet.text().getString());
        }
    }

    @Inject(method = "onSubtitle", at = @At("TAIL"))
    private void activity$autogg$subtitle(SubtitleS2CPacket packet, CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated() || !AutoGGClient.CONFIG.enabled) return;
        if (packet.text() != null) {
            AutoGGClient.onRoundResult(MinecraftClient.getInstance(), packet.text().getString());
        }
    }

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void activity$onSendChatMessage(String content, CallbackInfo ci) {
        if (content != null && content.startsWith("/")) {
            if (activity$executeMenuCommand(content)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "sendChatCommand", at = @At("HEAD"), cancellable = true)
    private void activity$onSendChatCommand(String command, CallbackInfo ci) {
        if (activity$executeMenuCommand(command)) {
            ci.cancel();
        }
    }

    private boolean activity$executeMenuCommand(String command) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
            return false;
        }
        if (command == null) return false;
        String clean = command.trim();
        if (clean.startsWith("/")) clean = clean.substring(1).trim();
        String activeCmd = activity.client.config.ActivityConfigManager.getConfig() != null
                ? activity.client.config.ActivityConfigManager.getConfig().menuCommand
                : "nt";
        if (activeCmd == null || activeCmd.isBlank()) activeCmd = "nt";
        if (activeCmd.startsWith("/")) activeCmd = activeCmd.substring(1).trim();

        if (clean.equalsIgnoreCase(activeCmd)) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null) {
                mc.send(() -> {
                    try {
                        mc.setScreen(new activity.client.gui.ActivityScreen());
                    } catch (Throwable t) {
                        try {
                            activity.client.gui.ActivityScreen.clearSession();
                            mc.setScreen(new activity.client.gui.ActivityScreen());
                        } catch (Throwable ignored) {}
                    }
                });
            }
            return true;
        }
        return false;
    }
}

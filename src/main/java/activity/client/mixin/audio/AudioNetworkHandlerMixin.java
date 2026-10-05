package activity.client.mixin.audio;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
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
import dev.audio.AudioSyncClient;

@Mixin(ClientPlayNetworkHandler.class)
public final class AudioNetworkHandlerMixin {
    @Inject(method = "onGameJoin", at = @At("TAIL"))
    private void activity$security$onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) return;
        AudioSyncClient.onServerTransferOrRespawn();
    }

    @Inject(method = "clearWorld", at = @At("TAIL"))
    private void activity$security$clearWorld(CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) return;
        AudioSyncClient.onServerTransferOrRespawn();
    }

    @Inject(method = "onEntityStatus", at = @At("TAIL"))
    private void activity$autogg$ownDeath(EntityStatusS2CPacket packet, CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) return;
        if (AudioSyncClient.isInGracePeriod()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        Entity entity = packet.getEntity(client.world);
        if (entity == client.player) {
            byte status = packet.getStatus();
            if (status == 3 && AudioSyncClient.CONFIG.enabled) {
                AudioSyncClient.markOwnDeath();
            } else if (status == 35) {
                activity.client.module.impl.defense.BufferPipelineModule.onTotemPop();
            }
        }
    }

    @Inject(method = "onHealthUpdate", at = @At("TAIL"))
    private void activity$autogg$healthUpdate(HealthUpdateS2CPacket packet, CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) return;
        activity.client.module.impl.defense.BufferPipelineModule.onHealthUpdate(packet.getHealth());
        if (!AudioSyncClient.CONFIG.enabled) return;
        if (AudioSyncClient.isInGracePeriod()) return;
        if (packet.getHealth() <= 0.0F) {
            AudioSyncClient.markOwnDeath();
        }
    }

    @Inject(method = "onPlayerRespawn", at = @At("TAIL"))
    private void activity$autogg$playerRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated() || !AudioSyncClient.CONFIG.enabled) return;
        AudioSyncClient.onPlayerRespawnPacket();
    }

    @Inject(method = "onExplosion", at = @At("TAIL"))
    private void activity$audiowave$onExplosion(ExplosionS2CPacket packet, CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) return;
        if (packet != null) {
            try {
                activity.client.module.impl.utility.AudioWaveTracker.recordExplosion(packet.center().x, packet.center().y, packet.center().z);
            } catch (Throwable ignored) {}
        }
    }

    @Inject(method = "onGameMessage", at = @At("TAIL"))
    private void activity$autogg$gameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated() || !AudioSyncClient.CONFIG.enabled || AudioSyncClient.isInGracePeriod()) return;
        if (packet.content() != null) {
            AudioSyncClient.onRoundResult(MinecraftClient.getInstance(), packet.content().getString());
        }
    }

    @Inject(method = "onChatMessage", at = @At("TAIL"))
    private void activity$autogg$chatMessage(ChatMessageS2CPacket packet, CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated() || !AudioSyncClient.CONFIG.enabled || AudioSyncClient.isInGracePeriod()) return;
        String msg = packet.unsignedContent() != null ? packet.unsignedContent().getString() : (packet.body() != null ? packet.body().content() : null);
        if (msg != null) {
            AudioSyncClient.onRoundResult(MinecraftClient.getInstance(), msg);
        }
    }

    @Inject(method = "onProfilelessChatMessage", at = @At("TAIL"))
    private void activity$autogg$profilelessChatMessage(ProfilelessChatMessageS2CPacket packet, CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated() || !AudioSyncClient.CONFIG.enabled || AudioSyncClient.isInGracePeriod()) return;
        if (packet.message() != null) {
            AudioSyncClient.onRoundResult(MinecraftClient.getInstance(), packet.message().getString());
        }
    }

    @Inject(method = "onOverlayMessage", at = @At("TAIL"))
    private void activity$autogg$overlayMessage(OverlayMessageS2CPacket packet, CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated() || !AudioSyncClient.CONFIG.enabled || AudioSyncClient.isInGracePeriod()) return;
        if (packet.text() != null) {
            AudioSyncClient.onRoundResult(MinecraftClient.getInstance(), packet.text().getString());
        }
    }

    @Inject(method = "onDeathMessage", at = @At("TAIL"))
    private void activity$autogg$deathMessage(DeathMessageS2CPacket packet, CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated() || !AudioSyncClient.CONFIG.enabled) return;
        if (AudioSyncClient.isInGracePeriod()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && packet.playerId() == client.player.getId()) {
            AudioSyncClient.markOwnDeath();
        }
        if (packet.message() != null) {
            AudioSyncClient.onRoundResult(client, packet.message().getString());
        }
    }

    @Inject(method = "onTitle", at = @At("TAIL"))
    private void activity$autogg$title(TitleS2CPacket packet, CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated() || !AudioSyncClient.CONFIG.enabled || AudioSyncClient.isInGracePeriod()) return;
        if (packet.text() != null) {
            AudioSyncClient.onRoundResult(MinecraftClient.getInstance(), packet.text().getString());
        }
    }

    @Inject(method = "onSubtitle", at = @At("TAIL"))
    private void activity$autogg$subtitle(SubtitleS2CPacket packet, CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated() || !AudioSyncClient.CONFIG.enabled || AudioSyncClient.isInGracePeriod()) return;
        if (packet.text() != null) {
            AudioSyncClient.onRoundResult(MinecraftClient.getInstance(), packet.text().getString());
        }
    }

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void activity$onSendChatMessage(String content, CallbackInfo ci) {
        if (activity$executeMenuCommand(content, false)) {
            ci.cancel();
        }
    }

    @Inject(method = "sendChatCommand", at = @At("HEAD"), cancellable = true)
    private void activity$onSendChatCommand(String command, CallbackInfo ci) {
        if (activity$executeMenuCommand(command, true)) {
            ci.cancel();
        }
    }

    private boolean activity$executeMenuCommand(String input, boolean isCommand) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
            return false;
        }
        if (input == null || input.isBlank()) return false;

        activity.client.config.ActivityConfig cfg = activity.client.config.ActivityConfigManager.getConfig();
        String activeCmd = cfg != null ? cfg.menuCommand : null;
        if (activeCmd == null || activeCmd.isBlank()) {
            return false;
        }
        while (activeCmd.startsWith("/")) {
            activeCmd = activeCmd.substring(1).trim();
        }
        if (activeCmd.isEmpty()) return false;

        String clean = input.trim();
        if (!isCommand) {
            if (!clean.startsWith("/")) {
                return false;
            }
            while (clean.startsWith("/")) {
                clean = clean.substring(1).trim();
            }
        } else {
            while (clean.startsWith("/")) {
                clean = clean.substring(1).trim();
            }
        }

        int spaceIdx = clean.indexOf(' ');
        String firstWord = spaceIdx > 0 ? clean.substring(0, spaceIdx) : clean;

        if (firstWord.equalsIgnoreCase(activeCmd)) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null) {
                mc.send(() -> {
                    try {
                        mc.setScreen(activity.client.gui.custom.api.ui.UI.INSTANCE);
                    } catch (Throwable t) {
                        try {
                            activity.client.gui.ActivityScreen.clearSession();
                            mc.setScreen(activity.client.gui.custom.api.ui.UI.INSTANCE);
                        } catch (Throwable ignored) {}
                    }
                });
            }
            return true;
        }
        return false;
    }
}

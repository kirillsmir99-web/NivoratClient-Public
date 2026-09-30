package dev.audio;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AudioSyncClient implements ClientModInitializer {
    public static final AudioSyncConfig CONFIG = AudioSyncConfig.load();
    private static final long SEND_COOLDOWN_MS = 8_000L;
    private static final long SERVER_TRANSFER_GRACE_MS = 4000L;
    private static final Map<Integer, Long> recentAttacks = new ConcurrentHashMap<>();

    public static double customDelayMs = 950.0;
    private static AudioSyncClient active = new AudioSyncClient();
    private static long lastSentAt;
    private static long scheduledSendTime = 0L;
    private static String pendingPhrase = null;
    private static long lastServerTransferTime = 0L;

    private boolean localDiedThisRound;

    public AudioSyncClient() {}

    @Override
    public void onInitializeClient() {
        ensureActive();
    }

    public static void ensureActive() {
        if (active == null) {
            active = new AudioSyncClient();
        }
    }

    public static void recordAttack(int entityId) {
        recentAttacks.put(entityId, System.currentTimeMillis());
        activity.client.module.impl.utility.AudioWaveTracker.recordAttack(entityId);
    }

    public static void recordAttack(int entityId, net.minecraft.util.math.Vec3d pos) {
        recentAttacks.put(entityId, System.currentTimeMillis());
        activity.client.module.impl.utility.AudioWaveTracker.recordAttack(entityId, pos);
    }

    public static void tick(MinecraftClient client) {
        if (active != null) {
            active.handleTick(client);
        }
    }

    public void handleTick(MinecraftClient client) {
        if (client == null) return;
        if (isInGracePeriod()) {
            localDiedThisRound = false;
            return;
        }
        if (client.player != null) {
            boolean isDead = client.player.isDead() || client.player.getHealth() <= 0.0F
                    || (client.currentScreen instanceof net.minecraft.client.gui.screen.DeathScreen);
            if (isDead) {
                if (!localDiedThisRound) {
                    handleOwnDeath();
                }
            } else {
                localDiedThisRound = false;
            }
        }

        if (!recentAttacks.isEmpty()) {
            long now = System.currentTimeMillis();
            recentAttacks.entrySet().removeIf(entry -> (now - entry.getValue()) > 4000L);
        }

        if (pendingPhrase != null && System.currentTimeMillis() >= scheduledSendTime) {
            if (client.player != null && client.player.networkHandler != null) {
                boolean currentlyDead = client.player.isDead() || client.player.getHealth() <= 0.0F
                        || (client.currentScreen instanceof net.minecraft.client.gui.screen.DeathScreen);
                long waitTime = System.currentTimeMillis() - (scheduledSendTime - Math.max(50L, (long) customDelayMs));
                if (currentlyDead && waitTime < 1800L) {

                    return;
                }
                if (CONFIG.enabled && !pendingPhrase.isBlank()) {
                    if (pendingPhrase.startsWith("/")) {
                        client.player.networkHandler.sendChatCommand(pendingPhrase.substring(1));
                    } else {
                        client.player.networkHandler.sendChatMessage(pendingPhrase);
                    }
                    lastSentAt = System.currentTimeMillis();
                }
                pendingPhrase = null;
                scheduledSendTime = 0L;
            }
        }
    }

    public static void sendPhraseDirect(String phrase) {
        if (phrase == null || phrase.isBlank()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null && client.player.networkHandler != null) {
            if (phrase.startsWith("/")) {
                client.player.networkHandler.sendChatCommand(phrase.substring(1));
            } else {
                client.player.networkHandler.sendChatMessage(phrase);
            }
            lastSentAt = System.currentTimeMillis();
        }
    }

    public static void markOwnDeath() {
        if (isInGracePeriod()) return;
        ensureActive();
        if (active != null) {
            active.handleOwnDeath();
        }
    }

    public static void onPlayerRespawnPacket() {
        onServerTransferOrRespawn();
    }

    public static void onServerTransferOrRespawn() {
        lastServerTransferTime = System.currentTimeMillis();
        pendingPhrase = null;
        scheduledSendTime = 0L;
        if (active != null) {
            active.localDiedThisRound = false;
        }
        recentAttacks.clear();
        activity.client.module.impl.utility.AudioWaveTracker.reset();
    }

    public static boolean isInGracePeriod() {
        return (System.currentTimeMillis() - lastServerTransferTime) < SERVER_TRANSFER_GRACE_MS;
    }

    public static void onPotentialFfaVictimDestroyed(Entity victim) {

    }

    public static void onPotentialFfaVictimDestroyed(int entityId) {

    }

    public static void onServerExplosion(double x, double y, double z, float radius) {

    }

    public void handleOwnDeath() {
        if (isInGracePeriod()) return;
        localDiedThisRound = true;
        if (!CONFIG.enabled || !CONFIG.sendOnOwnDeath) return;
        long now = System.currentTimeMillis();
        if (now - lastSentAt > SEND_COOLDOWN_MS && pendingPhrase == null) {
            String phrase = CONFIG.nextPhrase();
            if (phrase != null && !phrase.isBlank()) {
                pendingPhrase = phrase;
                scheduledSendTime = now + Math.max(50L, (long) customDelayMs);
            }
        }
    }

    public void onConfirmedKill(String victimName) {
        if (isInGracePeriod()) return;
        if (!CONFIG.enabled || !CONFIG.sendOnKill) return;
        long now = System.currentTimeMillis();
        if (now - lastSentAt > SEND_COOLDOWN_MS && pendingPhrase == null) {
            String phrase = CONFIG.nextPhrase();
            if (phrase != null && !phrase.isBlank()) {
                pendingPhrase = phrase;
                scheduledSendTime = now + Math.max(50L, (long) customDelayMs);
            }
        }
    }

    public static void triggerConfirmedKill(String victimName) {
        if (isInGracePeriod()) return;
        ensureActive();
        if (active != null) {
            active.onConfirmedKill(victimName);
        }
    }

    public static void onEntityDeath(Entity entity) {

    }

    private void handleEntityDeath(Entity entity) {

    }

    public static void onRoundResult(MinecraftClient client, String message) {
        ensureActive();
        if (active != null) active.handleRoundResult(client, message);
    }

    private void handleRoundResult(MinecraftClient client, String message) {
        if (client == null || client.player == null || message == null || !CONFIG.enabled || isInGracePeriod()) return;

        String lower = message.toLowerCase(java.util.Locale.ROOT);
        if (!lower.contains("побед") && !lower.contains("выигр") && !lower.contains("won") && !lower.contains("victor")
                && !lower.contains("убил") && !lower.contains("умер") && !lower.contains("погиб") && !lower.contains("died")
                && !lower.contains("kill") && !lower.contains("dead") && !lower.contains("defeat") && !lower.contains("проигр")
                && !lower.contains("поражен") && !lower.contains("дуэл") && !lower.contains("duel")
                && !lower.contains("lost") && !lower.contains("slain") && !lower.contains("ранил") && !lower.contains("одолел")) {
            return;
        }

        String playerName = client.player.getName().getString();

        if (activity.client.module.impl.utility.AudioWaveTracker.isOwnDeathMessage(message, playerName)) {
            handleOwnDeath();
            return;
        }

        if (CONFIG.sendOnKill) {
            String directVictim = activity.client.module.impl.utility.AudioWaveTracker.parseDirectKill(message, playerName);
            if (directVictim != null) {
                onConfirmedKill(directVictim);
                return;
            }
        }

        boolean playerWon = activity.client.module.impl.utility.AudioWaveTracker.isDuelWinMessage(message, playerName);
        boolean playerLost = activity.client.module.impl.utility.AudioWaveTracker.isDuelLossMessage(message, playerName);

        if ((playerWon && !localDiedThisRound && CONFIG.sendOnKill) || (playerLost && CONFIG.sendOnOwnDeath)) {
            String phrase = CONFIG.nextPhrase();
            long now = System.currentTimeMillis();
            if (CONFIG.enabled && phrase != null && !phrase.isBlank() && now - lastSentAt > SEND_COOLDOWN_MS && pendingPhrase == null) {
                pendingPhrase = phrase;
                scheduledSendTime = now + Math.max(50L, (long) customDelayMs);
            }
        }
        if (client.player != null && !client.player.isDead() && client.player.getHealth() > 0.0F
                && !(client.currentScreen instanceof net.minecraft.client.gui.screen.DeathScreen)) {
            localDiedThisRound = false;
        }
    }

    public static boolean hasPendingPhrase() {
        return pendingPhrase != null;
    }

    public static String getPendingPhrase() {
        return pendingPhrase;
    }

    public static long getScheduledSendTime() {
        return scheduledSendTime;
    }

    public static boolean isLocalDiedThisRound() {
        return active != null && active.localDiedThisRound;
    }

    public static void setLastSentAtForTest(long time) {
        lastSentAt = time;
    }

    public static void resetStateForTest() {
        pendingPhrase = null;
        scheduledSendTime = 0L;
        lastSentAt = 0L;
        lastServerTransferTime = 0L;
        if (active != null) {
            active.localDiedThisRound = false;
        }
        recentAttacks.clear();
        activity.client.module.impl.utility.AudioWaveTracker.reset();
    }
}


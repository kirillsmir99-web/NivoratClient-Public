package ru.elarion.autogg;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AutoGGClient {
    public static final AutoGGConfig CONFIG = AutoGGConfig.load();
    private static final long SEND_COOLDOWN_MS = 8_000L;
    private static final Map<Integer, Long> recentAttacks = new ConcurrentHashMap<>();

    public static double customDelayMs = 950.0;
    private static AutoGGClient active = new AutoGGClient();
    private static long lastSentAt;
    private static long scheduledSendTime = 0L;
    private static String pendingPhrase = null;

    private boolean localDiedThisRound;

    public static void ensureActive() {
        if (active == null) {
            active = new AutoGGClient();
        }
    }

    public static void recordAttack(int entityId) {
        recentAttacks.put(entityId, System.currentTimeMillis());
        activity.client.module.impl.utility.AutoGGKillTracker.recordAttack(entityId);
    }

    public static void recordAttack(int entityId, net.minecraft.util.math.Vec3d pos) {
        recentAttacks.put(entityId, System.currentTimeMillis());
        activity.client.module.impl.utility.AutoGGKillTracker.recordAttack(entityId, pos);
    }

    public static void tick(MinecraftClient client) {
        if (active != null) {
            active.handleTick(client);
        }
    }

    public void handleTick(MinecraftClient client) {
        if (client == null) return;
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
                    // Briefly hold phrase until player respawns so the packet is accepted by server
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
        ensureActive();
        if (active != null) {
            active.handleOwnDeath();
        }
    }

    public static void onPlayerRespawnPacket() {
        ensureActive();
        if (active != null) {
            active.handleOwnDeath();
        }
    }

    public static void onPotentialFfaVictimDestroyed(Entity victim) {
        // Disabled: Speculative FFA victim destroyed attribution disabled to prevent false triggers on third-party deaths
    }

    public static void onPotentialFfaVictimDestroyed(int entityId) {
        // Disabled: Speculative FFA victim destroyed attribution disabled to prevent false triggers on third-party deaths
    }

    public static void onServerExplosion(double x, double y, double z, float radius) {
        // Disabled: Speculative explosion proximity attacks disabled to prevent false triggers on third-party deaths
    }

    public void handleOwnDeath() {
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

    public static void onEntityDeath(Entity entity) {
        // Speculative proximity entity deaths disabled to prevent false triggers on FFA
    }

    private void handleEntityDeath(Entity entity) {
        // Speculative proximity entity deaths disabled to prevent false triggers on FFA
    }

    public static void onRoundResult(MinecraftClient client, String message) {
        ensureActive();
        if (active != null) active.handleRoundResult(client, message);
    }

    private void handleRoundResult(MinecraftClient client, String message) {
        if (client == null || client.player == null || message == null) return;

        String playerName = client.player.getName().getString();

        // 1. Check if our player died via server chat death message
        if (activity.client.module.impl.utility.AutoGGKillTracker.isOwnDeathMessage(message, playerName)) {
            handleOwnDeath();
            return;
        }

        // 2. Check server chat kill feed for kills scored by our player
        String killVictim = activity.client.module.impl.utility.AutoGGKillTracker.parseChatKill(message, playerName);
        if (killVictim != null) {
            onConfirmedKill(killVictim);
            return;
        }

        // 3. Check round results (Duel / Arena win-loss)
        boolean playerWon = activity.client.module.impl.utility.AutoGGKillTracker.isDuelWinMessage(message, playerName);
        boolean playerLost = activity.client.module.impl.utility.AutoGGKillTracker.isDuelLossMessage(message, playerName);

        if ((playerWon && !localDiedThisRound) || (playerLost && CONFIG.sendOnOwnDeath)) {
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
        if (active != null) {
            active.localDiedThisRound = false;
        }
        recentAttacks.clear();
    }
}


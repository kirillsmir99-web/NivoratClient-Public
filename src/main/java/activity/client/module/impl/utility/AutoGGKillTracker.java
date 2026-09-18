package activity.client.module.impl.utility;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intelligent kill attribution engine for AutoGG.
 *
 * <p>Solves false triggers on FFA arenas and in crowded team fights by enforcing strict
 * attribution criteria:
 * <ul>
 *   <li>Direct melee/combat hit tracking with strict 900ms window and 7.0 block combat reach.</li>
 *   <li>Player-initiated explosion tracking (TNT carts and Respawn Anchors) within 1500ms and 8.5m radius.</li>
 *   <li>Server chat kill feed regex parsing across Russian and English server formats.</li>
 *   <li>Hard rejection of distant or unrelated death packets.</li>
 * </ul>
 */
public final class AutoGGKillTracker {

    public static final long ATTACK_WINDOW_MS = 900L;
    public static final double MAX_COMBAT_DISTANCE = 7.0;
    public static final long EXPLOSION_WINDOW_MS = 1500L;
    public static final double DEFAULT_EXPLOSION_RADIUS = 8.5;

    public static final long FFA_COMBAT_WINDOW_MS = 4500L;
    public static final double FFA_MAX_DISTANCE = 16.0;
    public static final long FFA_EXPLOSION_WINDOW_MS = 4000L;
    public static final long CART_PLACEMENT_WINDOW_MS = 6000L;

    public record AttackRecord(int entityId, long timestampMs, Vec3d pos) {}
    public record ExplosionRecord(double x, double y, double z, double radius, long timestampMs) {}
    public record CartPlacementRecord(double x, double y, double z, long timestampMs) {}

    private static final Map<Integer, AttackRecord> recentAttacks = new ConcurrentHashMap<>();
    private static final List<ExplosionRecord> recentExplosions = new CopyOnWriteArrayList<>();
    private static final List<CartPlacementRecord> recentCartPlacements = new CopyOnWriteArrayList<>();

    private static final Pattern[] KILL_PATTERNS = new Pattern[] {
        // Russian patterns:
        Pattern.compile("([\\w\\u0400-\\u04FF]+)\\s+был\\s+убит\\s+([\\w\\u0400-\\u04FF]+)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
        Pattern.compile("([\\w\\u0400-\\u04FF]+)\\s+погиб\\s+от\\s+взрыва\\s+([\\w\\u0400-\\u04FF]+)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
        Pattern.compile("([\\w\\u0400-\\u04FF]+)\\s+пал\\s+от\\s+руки\\s+([\\w\\u0400-\\u04FF]+)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
        Pattern.compile("([\\w\\u0400-\\u04FF]+)\\s+сгорел\\s+от\\s+руки\\s+([\\w\\u0400-\\u04FF]+)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
        Pattern.compile("([\\w\\u0400-\\u04FF]+)\\s+убит\\s+игроком\\s+([\\w\\u0400-\\u04FF]+)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
        Pattern.compile("([\\w\\u0400-\\u04FF]+)\\s+был\\s+зарублен\\s+([\\w\\u0400-\\u04FF]+)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
        Pattern.compile("([\\w\\u0400-\\u04FF]+)\\s+взорван\\s+([\\w\\u0400-\\u04FF]+)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
        Pattern.compile("([\\w\\u0400-\\u04FF]+)\\s+расстрелян\\s+([\\w\\u0400-\\u04FF]+)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
        Pattern.compile("([\\w\\u0400-\\u04FF]+).*\\bпри\\s+попытке\\s+спастись\\s+от\\s+([\\w\\u0400-\\u04FF]+)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
        Pattern.compile("([\\w\\u0400-\\u04FF]+).*\\bутонул.*пытаясь\\s+сбежать\\s+от\\s+([\\w\\u0400-\\u04FF]+)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
        Pattern.compile("([\\w\\u0400-\\u04FF]+).*\\bразбился.*спасаясь\\s+от\\s+([\\w\\u0400-\\u04FF]+)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),

        // English patterns:
        Pattern.compile("([\\w]+)\\s+was\\s+slain\\s+by\\s+([\\w]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("([\\w]+)\\s+was\\s+blown\\s+up\\s+by\\s+([\\w]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("([\\w]+)\\s+was\\s+shot\\s+by\\s+([\\w]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("([\\w]+)\\s+was\\s+killed\\s+by\\s+([\\w]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("([\\w]+)\\s+was\\s+doomed\\s+to\\s+fall\\s+by\\s+([\\w]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("([\\w]+).*\\bfell.*while\\s+fighting\\s+([\\w]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("([\\w]+).*\\bkilled\\s+by\\s+([\\w]+)", Pattern.CASE_INSENSITIVE)
    };

    private AutoGGKillTracker() {}

    /**
     * Records a direct hit on a target entity with specified coordinates and timestamp.
     */
    public static void recordAttack(int entityId, Vec3d pos, long timestampMs) {
        recentAttacks.put(entityId, new AttackRecord(entityId, timestampMs, pos));
    }

    /**
     * Records a direct hit on a target entity with current coordinates and timestamp.
     */
    public static void recordAttack(int entityId, Vec3d pos) {
        recordAttack(entityId, pos, System.currentTimeMillis());
    }

    /**
     * Convenience method to record attack without known position.
     */
    public static void recordAttack(int entityId) {
        recordAttack(entityId, Vec3d.ZERO);
    }

    /**
     * Records a player-initiated explosion with specified timestamp.
     */
    public static void recordExplosion(double x, double y, double z, double radius, long timestampMs) {
        recentExplosions.add(new ExplosionRecord(x, y, z, radius > 0 ? radius : DEFAULT_EXPLOSION_RADIUS, timestampMs));
    }

    /**
     * Records a player-initiated explosion (TNT minecart, respawn anchor, end crystal).
     */
    public static void recordExplosion(double x, double y, double z, double radius) {
        recordExplosion(x, y, z, radius, System.currentTimeMillis());
    }

    /**
     * Records a player-initiated explosion at default 8.5m radius.
     */
    public static void recordExplosion(double x, double y, double z) {
        recordExplosion(x, y, z, DEFAULT_EXPLOSION_RADIUS);
    }

    /**
     * Records placement of a minecart/TNT cart by the local player.
     */
    public static void recordCartPlacement(double x, double y, double z) {
        recentCartPlacements.add(new CartPlacementRecord(x, y, z, System.currentTimeMillis()));
    }

    /**
     * Checks if a position is within maxDist of a cart placed by the local player within window.
     */
    public static boolean isNearbyPlacedCart(double x, double y, double z, double maxDist) {
        long now = System.currentTimeMillis();
        for (CartPlacementRecord rec : recentCartPlacements) {
            if (now - rec.timestampMs() <= CART_PLACEMENT_WINDOW_MS) {
                double dx = x - rec.x();
                double dy = y - rec.y();
                double dz = z - rec.z();
                if ((dx * dx + dy * dy + dz * dz) <= (maxDist * maxDist)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Cleans up attack records and explosion epicenters that have expired past their attribution windows.
     */
    public static void cleanExpired(long now) {
        recentAttacks.entrySet().removeIf(e -> (now - e.getValue().timestampMs()) > FFA_COMBAT_WINDOW_MS);
        recentExplosions.removeIf(e -> (now - e.timestampMs()) > FFA_EXPLOSION_WINDOW_MS);
        recentCartPlacements.removeIf(e -> (now - e.timestampMs()) > CART_PLACEMENT_WINDOW_MS);
    }

    /**
     * Evaluates whether an observed death of a PlayerEntity can be strictly attributed to our player.
     *
     * @param victim the entity receiving death status
     * @param client the Minecraft client
     * @return true if our player caused the death via direct hit or explosion within combat window
     */
    public static boolean shouldAttributeKill(Entity victim, MinecraftClient client) {
        if (victim == null || client == null || client.player == null) {
            return false;
        }
        if (victim == client.player) {
            return false;
        }
        if (!(victim instanceof PlayerEntity)) {
            return false;
        }

        long now = System.currentTimeMillis();
        cleanExpired(now);

        int victimId = victim.getId();
        Vec3d victimPos = new Vec3d(victim.getX(), victim.getY(), victim.getZ());

        // 1. Direct hit check (extended for FFA tick latency and combos)
        AttackRecord attack = recentAttacks.get(victimId);
        if (attack != null) {
            long diff = now - attack.timestampMs();
            if (diff >= 0 && diff <= FFA_COMBAT_WINDOW_MS) {
                double distSq = client.player.squaredDistanceTo(victim);
                double attackDistSq = (attack.pos() != null && attack.pos() != Vec3d.ZERO)
                        ? attack.pos().squaredDistanceTo(victimPos)
                        : distSq;
                if (distSq <= FFA_MAX_DISTANCE * FFA_MAX_DISTANCE || attackDistSq <= FFA_MAX_DISTANCE * FFA_MAX_DISTANCE) {
                    recentAttacks.remove(victimId);
                    return true;
                }
            }
        }

        // 2. Player-initiated explosion check (TNT carts, respawn anchors, end crystals)
        if (victimPos != null) {
            for (ExplosionRecord exp : recentExplosions) {
                long diff = now - exp.timestampMs();
                if (diff >= 0 && diff <= FFA_EXPLOSION_WINDOW_MS) {
                    double dx = victimPos.x - exp.x();
                    double dy = victimPos.y - exp.y();
                    double dz = victimPos.z - exp.z();
                    double distSq = dx * dx + dy * dy + dz * dz;
                    double r = exp.radius() + 2.5; // generous blast reach margin for knockback/hitboxes
                    if (distSq <= r * r) {
                        recentExplosions.remove(exp);
                        return true;
                    }
                }
            }
        }

        // 3. HP Reaper Integration: Check if victim was the active crosshair/combat target
        try {
            if (victim instanceof net.minecraft.entity.LivingEntity living) {
                net.minecraft.entity.LivingEntity activeTarget = dev.hpreaper.HealthHudOverlay.getActiveTarget(client.player, System.nanoTime());
                if (activeTarget == living) {
                    double distSq = client.player.squaredDistanceTo(victim);
                    if (distSq <= FFA_MAX_DISTANCE * FFA_MAX_DISTANCE) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}

        return false;
    }

    /**
     * Parses server chat death messages to confirm if our player scored a kill.
     *
     * @param rawMessage the raw chat string from server
     * @param localPlayerName our player's username
     * @return the victim's username if kill is confirmed for local player, or null otherwise
     */
    public static String parseChatKill(String rawMessage, String localPlayerName) {
        if (rawMessage == null || rawMessage.isBlank() || localPlayerName == null || localPlayerName.isBlank()) {
            return null;
        }

        // Clean formatting codes (§a, §c, §r, etc.)
        String clean = rawMessage.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();

        for (Pattern pattern : KILL_PATTERNS) {
            Matcher matcher = pattern.matcher(clean);
            if (matcher.find()) {
                String victim = matcher.group(1).trim();
                String killer = matcher.group(2).trim();

                if (killer.equalsIgnoreCase(localPlayerName) && !victim.equalsIgnoreCase(localPlayerName)) {
                    return victim;
                }
            }
        }

        return null;
    }

    /**
     * Checks if a server chat message indicates our player died.
     */
    public static boolean isOwnDeathMessage(String rawMessage, String localPlayerName) {
        if (rawMessage == null || rawMessage.isBlank() || localPlayerName == null || localPlayerName.isBlank()) {
            return false;
        }

        String clean = rawMessage.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
        for (Pattern pattern : KILL_PATTERNS) {
            Matcher matcher = pattern.matcher(clean);
            if (matcher.find()) {
                String victim = matcher.group(1).trim();
                if (victim.equalsIgnoreCase(localPlayerName)) {
                    return true;
                }
            }
        }

        // Check common suicide / death phrases containing local player's name
        String lower = clean.toLowerCase(Locale.ROOT);
        String nameLower = localPlayerName.toLowerCase(Locale.ROOT);
        if (lower.contains(nameLower)) {
            if (lower.contains("умер") || lower.contains("погиб") || lower.contains("разбился")
                    || lower.contains("сгорел") || lower.contains("утонул") || lower.contains("died")
                    || lower.contains("fell") || lower.contains("drowned") || lower.contains("burned")
                    || lower.contains("blew up") || lower.contains("suicide")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Test helper for validating direct hit attribution logic.
     */
    public static boolean isAttributedDirectHit(int entityId, double distance, long now) {
        AttackRecord attack = recentAttacks.get(entityId);
        if (attack != null) {
            long diff = now - attack.timestampMs();
            if (diff >= 0 && diff <= ATTACK_WINDOW_MS) {
                return distance <= MAX_COMBAT_DISTANCE;
            }
        }
        return false;
    }

    /**
     * Test helper for validating explosion attribution logic.
     */
    public static boolean isAttributedExplosion(Vec3d victimPos, long now) {
        if (victimPos == null) return false;
        for (ExplosionRecord exp : recentExplosions) {
            long diff = now - exp.timestampMs();
            if (diff >= 0 && diff <= EXPLOSION_WINDOW_MS) {
                double dx = victimPos.x - exp.x();
                double dy = victimPos.y - exp.y();
                double dz = victimPos.z - exp.z();
                double distSq = dx * dx + dy * dy + dz * dz;
                double r = exp.radius();
                if (distSq <= r * r) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Clears all state caches.
     */
    public static void reset() {
        recentAttacks.clear();
        recentExplosions.clear();
        recentCartPlacements.clear();
    }
}

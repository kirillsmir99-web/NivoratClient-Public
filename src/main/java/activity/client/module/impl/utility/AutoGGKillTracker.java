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
    private static final Map<Integer, String> recentVictimNames = new ConcurrentHashMap<>();
    private static final Map<Integer, Vec3d> lastKnownTargetPositions = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> lastAttributedKills = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastAttributedKillNames = new ConcurrentHashMap<>();

    // Direct kill notice patterns where group 1 is victim (e.g. server says "Вы убили <Victim>")
    private static final Pattern[] DIRECT_YOU_KILLED_PATTERNS = new Pattern[] {
        Pattern.compile("(?:Вы|Вы\\s+успешно)\\s+(?:убили|уничтожили|одолели|победили|казнили)\\s+(?:игрока\\s+)?([\\w\\u0400-\\u04FF]+)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
        Pattern.compile("(?:You\\s+killed|You\\s+slayed|You\\s+defeated)\\s+([\\w]+)", Pattern.CASE_INSENSITIVE)
    };

    // Killer-Victim patterns where group 1 is killer, group 2 is victim
    private static final Pattern[] KILLER_VICTIM_PATTERNS = new Pattern[] {
        Pattern.compile("([\\w\\u0400-\\u04FF]+)\\s+(?:убил|зарубил|взорвал|расстрелял|уничтожил)\\s+([\\w\\u0400-\\u04FF]+)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
        Pattern.compile("\\[.*?\\]\\s*([\\w\\u0400-\\u04FF]+)\\s+(?:убил|зарубил|взорвал)\\s+([\\w\\u0400-\\u04FF]+)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
        Pattern.compile("([\\w\\u0400-\\u04FF]+)\\s*(?:⚔|->|»)\\s*([\\w\\u0400-\\u04FF]+)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
        Pattern.compile("([\\w]+)\\s+killed\\s+([\\w]+)", Pattern.CASE_INSENSITIVE)
    };

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

    private static final Pattern DUEL_WIN_REGEX_PATTERN = Pattern.compile(
        "(?i)(?u)(?:\\[.*?\\]\\s*)?(?:(?:игрок[а-я]*|player)\\s+)?([\\w\\u0400-\\u04FF]+)\\s+(?:победил|выиграл|одолел|разгромил|одержал\\s+победу|won(?:\\s+the\\s+duel)?|defeated)(?:\\s+(?:в\\s+дуэли\\s+)?(?:у\\s+|над\\s+|against\\s+)?(?:(?:игрок[а-я]*|player)\\s+)?([\\w\\u0400-\\u04FF]+))?"
    );

    private static final Pattern DUEL_LOSS_REGEX_PATTERN = Pattern.compile(
        "(?i)(?u)(?:\\[.*?\\]\\s*)?(?:(?:игрок[а-я]*|player)\\s+)?([\\w\\u0400-\\u04FF]+)\\s+(?:победил|выиграл|одолел|разгромил|одержал\\s+победу|won(?:\\s+the\\s+duel)?|defeated)(?:\\s+(?:в\\s+дуэли\\s+)?(?:у\\s+|над\\s+|against\\s+)?(?:(?:игрок[а-я]*|player)\\s+)?([\\w\\u0400-\\u04FF]+))"
    );

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

    public static void recordVictimName(int entityId, String name) {
        if (name != null && !name.isBlank()) {
            recentVictimNames.put(entityId, name);
        }
    }

    public static boolean isRecentlyAttacked(int entityId) {
        long now = System.currentTimeMillis();
        AttackRecord attack = recentAttacks.get(entityId);
        return attack != null && (now - attack.timestampMs()) <= FFA_COMBAT_WINDOW_MS;
    }

    /**
     * Cleans up attack records, explosions, and attribution debounce caches past expiration.
     */
    public static void cleanExpired(long now) {
        recentAttacks.entrySet().removeIf(e -> (now - e.getValue().timestampMs()) > FFA_COMBAT_WINDOW_MS);
        recentExplosions.removeIf(e -> (now - e.timestampMs()) > FFA_EXPLOSION_WINDOW_MS);
        recentCartPlacements.removeIf(e -> (now - e.timestampMs()) > CART_PLACEMENT_WINDOW_MS);
        lastAttributedKills.entrySet().removeIf(e -> (now - e.getValue()) > 8000L);
        lastAttributedKillNames.entrySet().removeIf(e -> (now - e.getValue()) > 8000L);
    }

    /**
     * Evaluates whether an observed death of a PlayerEntity can be strictly attributed to our player.
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
     * Real-time tick evaluation for FFA arenas where standard death packets are withheld by server plugins.
     */
    public static boolean shouldAttributeFfaKill(PlayerEntity victim, MinecraftClient client, long now) {
        if (victim == null || client == null || client.player == null) return false;
        if (victim == client.player) return false;

        int victimId = victim.getId();
        String victimName = victim.getName().getString();

        // Check if recently attributed within 4000ms
        Long lastKill = lastAttributedKills.get(victimId);
        if (lastKill != null && (now - lastKill) < 4000L) {
            return false;
        }
        Long lastNameKill = lastAttributedKillNames.get(victimName.toLowerCase(Locale.ROOT));
        if (lastNameKill != null && (now - lastNameKill) < 4000L) {
            return false;
        }

        Vec3d currentPos = new Vec3d(victim.getX(), victim.getY(), victim.getZ());
        Vec3d prevPos = lastKnownTargetPositions.get(victimId);

        // 1. Check if in combat window (direct sword/melee attack, cart/crystal explosion, or HP Reaper active target)
        boolean inCombat = false;
        AttackRecord attack = recentAttacks.get(victimId);
        if (attack != null && (now - attack.timestampMs()) <= FFA_COMBAT_WINDOW_MS) {
            inCombat = true;
        }

        if (!inCombat) {
            for (ExplosionRecord exp : recentExplosions) {
                if ((now - exp.timestampMs()) <= FFA_EXPLOSION_WINDOW_MS) {
                    double r = exp.radius() + 3.5;
                    double rSq = r * r;
                    // Check current position
                    double dx = currentPos.x - exp.x();
                    double dy = currentPos.y - exp.y();
                    double dz = currentPos.z - exp.z();
                    if ((dx * dx + dy * dy + dz * dz) <= rSq) {
                        inCombat = true;
                        break;
                    }
                    // Check previous position before teleport
                    if (prevPos != null) {
                        double px = prevPos.x - exp.x();
                        double py = prevPos.y - exp.y();
                        double pz = prevPos.z - exp.z();
                        if ((px * px + py * py + pz * pz) <= rSq) {
                            inCombat = true;
                            break;
                        }
                    }
                }
            }
        }

        if (!inCombat) {
            try {
                net.minecraft.entity.LivingEntity activeTarget = dev.hpreaper.HealthHudOverlay.getActiveTarget(client.player, System.nanoTime());
                if (activeTarget == victim && client.player.squaredDistanceTo(victim) <= FFA_MAX_DISTANCE * FFA_MAX_DISTANCE) {
                    inCombat = true;
                }
            } catch (Throwable ignored) {}
        }

        if (!inCombat) {
            lastKnownTargetPositions.put(victimId, currentPos);
            return false;
        }

        // 2. Combat verified. Check if victim died / reached 0 HP / removed / teleported to spawn
        boolean dead = false;
        if (victim.isDead() || victim.getHealth() <= 0.0F || victim.isRemoved() || !victim.isAlive()) {
            dead = true;
        }

        if (!dead) {
            try {
                float extractedHp = dev.hpreaper.HealthHudOverlay.extractEntityHealth(victim);
                if (extractedHp <= 0.0F) {
                    dead = true;
                }
            } catch (Throwable ignored) {}
        }

        // 3. FFA instant respawn teleport check: victim was close (< 16m) and suddenly jumped > 20m away in 1 tick
        if (!dead && prevPos != null) {
            double distMovedSq = prevPos.squaredDistanceTo(currentPos);
            double distToPlayerSq = client.player.squaredDistanceTo(prevPos);
            if (distToPlayerSq <= 16.0 * 16.0 && distMovedSq > 20.0 * 20.0) {
                dead = true;
            }
        }
        lastKnownTargetPositions.put(victimId, currentPos);

        if (dead) {
            lastAttributedKills.put(victimId, now);
            lastAttributedKillNames.put(victimName.toLowerCase(Locale.ROOT), now);
            recentAttacks.remove(victimId);
            return true;
        }

        return false;
    }

    public static void updateTargetPositions(MinecraftClient client) {
        if (client == null || client.world == null) return;
        for (PlayerEntity p : client.world.getPlayers()) {
            if (p != null && p != client.player) {
                lastKnownTargetPositions.put(p.getId(), new Vec3d(p.getX(), p.getY(), p.getZ()));
            }
        }
    }

    /**
     * Parses server chat death messages to confirm if our player scored a kill.
     */
    public static String parseChatKill(String rawMessage, String localPlayerName) {
        if (rawMessage == null || rawMessage.isBlank() || localPlayerName == null || localPlayerName.isBlank()) {
            return null;
        }

        // Clean formatting codes (§a, §c, §r, etc.)
        String clean = rawMessage.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();

        // 1. Direct "Вы убили <Victim>" notices
        for (Pattern pattern : DIRECT_YOU_KILLED_PATTERNS) {
            Matcher matcher = pattern.matcher(clean);
            if (matcher.find()) {
                String victim = matcher.group(1).trim();
                if (!victim.equalsIgnoreCase(localPlayerName)) {
                    return victim;
                }
            }
        }

        // 2. <Killer> убил <Victim> patterns
        for (Pattern pattern : KILLER_VICTIM_PATTERNS) {
            Matcher matcher = pattern.matcher(clean);
            if (matcher.find()) {
                String killer = matcher.group(1).trim();
                String victim = matcher.group(2).trim();
                if (killer.equalsIgnoreCase(localPlayerName) && !victim.equalsIgnoreCase(localPlayerName)) {
                    return victim;
                }
            }
        }

        // 3. <Victim> был убит <Killer> patterns
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

    public static boolean isSystemDuelMessage(String clean) {
        if (clean == null || clean.isBlank()) return false;
        String lower = clean.toLowerCase(Locale.ROOT);
        return lower.contains("дуэл") || lower.contains("duel")
                || lower.contains("победител") || lower.contains("победил")
                || lower.contains("выиграл") || lower.contains("одержал победу")
                || lower.contains("winner") || lower.contains("victory")
                || lower.contains("поражение") || lower.contains("проиграл")
                || lower.contains("проигравший") || lower.contains("loser")
                || lower.contains("игра окончена") || lower.contains("раунд окончен")
                || lower.contains("матч окончен") || lower.contains("конец дуэли");
    }

    private static boolean isPlayerChatMessage(String clean) {
        if (clean == null || clean.isBlank()) return false;
        // Standard vanilla/server player chat format: <PlayerName> ...
        if (clean.startsWith("<") && clean.contains(">")) {
            return true;
        }
        int colonIdx = clean.indexOf(':');
        if (colonIdx > 0) {
            String prefix = clean.substring(0, colonIdx).trim().toLowerCase(Locale.ROOT);
            if (prefix.contains("]")) {
                prefix = prefix.substring(prefix.lastIndexOf(']') + 1).trim();
            }
            if (prefix.contains("»")) {
                prefix = prefix.substring(prefix.lastIndexOf('»') + 1).trim();
            }
            if (prefix.contains("побед") || prefix.contains("выигр") || prefix.contains("дуэл")
                    || prefix.contains("duel") || prefix.contains("winner") || prefix.contains("проигр")
                    || prefix.contains("loser") || prefix.contains("арена") || prefix.contains("arena")
                    || prefix.contains("сервер") || prefix.contains("server") || prefix.contains("инфо")
                    || prefix.contains("info") || prefix.contains("результат") || prefix.contains("счет")
                    || prefix.contains("счёт") || prefix.contains("убийство") || prefix.contains("килл")
                    || prefix.contains("kill") || prefix.contains("окончен") || prefix.contains("завершен")) {
                return false;
            }
            if (!prefix.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a server chat message indicates our player died.
     */
    public static boolean isOwnDeathMessage(String rawMessage, String localPlayerName) {
        if (rawMessage == null || rawMessage.isBlank() || localPlayerName == null || localPlayerName.isBlank()) {
            return false;
        }

        String clean = rawMessage.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
        if (isPlayerChatMessage(clean)) {
            return false;
        }
        String lower = clean.toLowerCase(Locale.ROOT);

        // If this message confirms a kill scored BY our player, it is definitely NOT own death
        if (parseChatKill(rawMessage, localPlayerName) != null) {
            return false;
        }

        // 1. Direct "Вы погибли", "Вас убил ...", "Вы умерли" notices
        if (lower.contains("вы погибли") || lower.contains("вы умерли") || lower.contains("вас убил")
                || lower.contains("вы были убиты") || lower.contains("вы разбились") || lower.contains("вы сгорели")
                || lower.contains("вы утонули") || lower.contains("вы подорвались") || lower.contains("вы взорвались")
                || lower.contains("you died") || lower.contains("you were killed") || lower.contains("you were slain")) {
            return true;
        }

        // 2. Standard victim patterns where localPlayer is victim (group 1)
        for (Pattern pattern : KILL_PATTERNS) {
            Matcher matcher = pattern.matcher(clean);
            if (matcher.find()) {
                String victim = matcher.group(1).trim();
                if (victim.equalsIgnoreCase(localPlayerName)) {
                    return true;
                }
            }
        }

        // 3. Killer-victim patterns where localPlayer is victim (group 2)
        for (Pattern pattern : KILLER_VICTIM_PATTERNS) {
            Matcher matcher = pattern.matcher(clean);
            if (matcher.find()) {
                String victim = matcher.group(2).trim();
                if (victim.equalsIgnoreCase(localPlayerName)) {
                    return true;
                }
            }
        }

        // 4. Fallback check for messages containing player name (strict word boundary) and death keywords
        Pattern namePattern = Pattern.compile("(?i)(?u)\\b" + Pattern.quote(localPlayerName) + "\\b");
        if (namePattern.matcher(clean).find()) {
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
     * Parses direct private kill notices where the server informs our player ('Вы убили <Player>', etc.).
     */
    public static String parseDirectKill(String rawMessage, String localPlayerName) {
        if (rawMessage == null || rawMessage.isBlank() || localPlayerName == null || localPlayerName.isBlank()) {
            return null;
        }
        String clean = rawMessage.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
        if (isPlayerChatMessage(clean)) {
            return null;
        }
        for (Pattern pattern : DIRECT_YOU_KILLED_PATTERNS) {
            Matcher matcher = pattern.matcher(clean);
            if (matcher.find()) {
                String victim = matcher.group(1).trim();
                if (!victim.equalsIgnoreCase(localPlayerName)) {
                    return victim;
                }
            }
        }
        return null;
    }

    /**
     * Checks if a message represents an explicit Duel / PvP victory announcement for the local player.
     */
    public static boolean isDuelWinMessage(String rawMessage, String localPlayerName) {
        if (rawMessage == null || rawMessage.isBlank() || localPlayerName == null || localPlayerName.isBlank()) {
            return false;
        }
        String clean = rawMessage.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
        if (isPlayerChatMessage(clean)) {
            return false;
        }
        String lower = clean.toLowerCase(Locale.ROOT);

        // Exact Title / Subtitle wins
        if (clean.equalsIgnoreCase("победа") || clean.equalsIgnoreCase("победа!") || clean.equalsIgnoreCase("victory") || clean.equalsIgnoreCase("victory!")) {
            return true;
        }

        // 1. Direct "Вы победили" notices
        if (lower.contains("вы победили") || lower.contains("вы выиграли") || lower.contains("вы одержали победу")
                || lower.contains("ваша победа") || lower.contains("you won") || lower.contains("victory!")) {
            return true;
        }

        Pattern namePattern = Pattern.compile("(?i)(?u)\\b" + Pattern.quote(localPlayerName) + "\\b");

        // 2. Structured duel format: "Победил: <Winner> ... Проиграл: <Loser>"
        int winnerIdx = lower.indexOf("победил:");
        if (winnerIdx < 0) winnerIdx = lower.indexOf("победитель:");
        if (winnerIdx < 0) winnerIdx = lower.indexOf("winner:");
        if (winnerIdx < 0) winnerIdx = lower.indexOf("победа:");
        if (winnerIdx < 0) winnerIdx = lower.indexOf("победитель дуэли:");

        int loserIdx = lower.indexOf("проиграл:");
        if (loserIdx < 0) loserIdx = lower.indexOf("проигравший:");
        if (loserIdx < 0) loserIdx = lower.indexOf("loser:");

        if (winnerIdx >= 0 && loserIdx >= 0) {
            String winnerPart;
            String loserPart;
            if (winnerIdx < loserIdx) {
                winnerPart = clean.substring(winnerIdx, loserIdx);
                loserPart = clean.substring(loserIdx);
            } else {
                loserPart = clean.substring(loserIdx, winnerIdx);
                winnerPart = clean.substring(winnerIdx);
            }
            boolean inWinner = namePattern.matcher(winnerPart).find();
            boolean inLoser = namePattern.matcher(loserPart).find();
            return inWinner && !inLoser;
        }

        if (winnerIdx >= 0) {
            String winnerPart = clean.substring(winnerIdx);
            String[] tokens = winnerPart.split("[,|;\\n\\r.]");
            if (tokens.length > 0 && namePattern.matcher(tokens[0]).find()) {
                return true;
            }
        }

        // 3. Pattern: "[Префикс] <Winner> победил/выиграл/одолел/разгромил/одержал победу [игрока/игроком] <Loser>" or "<Winner> won the duel"
        Matcher m = DUEL_WIN_REGEX_PATTERN.matcher(clean);
        if (m.find()) {
            String winner = m.group(1).trim();
            if (winner.equalsIgnoreCase(localPlayerName)) {
                return true;
            }
        }

        // 4. Extracted winner check across Russian/English server formats
        String winner = extractDuelWinner(clean);
        if (winner != null && winner.equalsIgnoreCase(localPlayerName)) {
            return true;
        }

        return false;
    }

    /**
     * Extracts declared duel winner name from chat message if present.
     */
    public static String extractDuelWinner(String clean) {
        if (clean == null || clean.isBlank()) return null;

        // 1. "победитель[: -—] <Winner>" / "winner[: -—] <Winner>"
        Pattern p1 = Pattern.compile("(?i)(?u)(?:победител[ьяе]|winner)\\s*(?:дуэли)?\\s*[:—–\\-]\\s*(?:игрок[а-я]*|player)?\\s*([a-zA-Z0-9_\\u0400-\\u04FF]{2,16})");
        Matcher m1 = p1.matcher(clean);
        if (m1.find()) {
            return m1.group(1).trim();
        }

        // 2. "победил[: -—] <Winner>" / "выиграл[: -—] <Winner>" / "победу одержал: <Winner>"
        Pattern p2 = Pattern.compile("(?i)(?u)(?:победил|выиграл|одержал\\s+победу)\\s*(?:в\\s+дуэли)?\\s*[:—–\\-]\\s*(?:игрок[а-я]*|player)?\\s*([a-zA-Z0-9_\\u0400-\\u04FF]{2,16})");
        Matcher m2 = p2.matcher(clean);
        if (m2.find()) {
            return m2.group(1).trim();
        }

        // 3. "<Winner> победил/выиграл/одолел/разгромил/одержал победу/won/defeated"
        Matcher m3 = DUEL_WIN_REGEX_PATTERN.matcher(clean);
        if (m3.find()) {
            return m3.group(1).trim();
        }

        return null;
    }

    /**
     * Extracts declared duel loser name from chat message if present.
     */
    public static String extractDuelLoser(String clean) {
        if (clean == null || clean.isBlank()) return null;

        // "проиграл[: -—] <Loser>" / "проигравший[: -—] <Loser>" / "loser[: -—] <Loser>"
        Pattern p1 = Pattern.compile("(?i)(?u)(?:проигравш[ийея]|проиграл|потерпел\\s+поражение|loser)\\s*(?:в\\s+дуэли)?\\s*[:—–\\-]\\s*(?:игрок[а-я]*|player)?\\s*([a-zA-Z0-9_\\u0400-\\u04FF]{2,16})");
        Matcher m1 = p1.matcher(clean);
        if (m1.find()) {
            return m1.group(1).trim();
        }

        Matcher m2 = DUEL_LOSS_REGEX_PATTERN.matcher(clean);
        if (m2.find()) {
            return m2.group(2) != null ? m2.group(2).trim() : null;
        }

        return null;
    }

    /**
     * Checks if a message represents an explicit Duel / PvP loss announcement for the local player.
     */
    public static boolean isDuelLossMessage(String rawMessage, String localPlayerName) {
        if (rawMessage == null || rawMessage.isBlank() || localPlayerName == null || localPlayerName.isBlank()) {
            return false;
        }
        String clean = rawMessage.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
        if (isPlayerChatMessage(clean)) {
            return false;
        }
        String lower = clean.toLowerCase(Locale.ROOT);

        // Exact Title / Subtitle loss
        if (clean.equalsIgnoreCase("поражение") || clean.equalsIgnoreCase("поражение!") || clean.equalsIgnoreCase("defeat") || clean.equalsIgnoreCase("defeat!")) {
            return true;
        }

        // Direct "Вы проиграли" notices
        if (lower.contains("вы проиграли") || lower.contains("вы потерпели поражение") || lower.contains("you lost") || lower.contains("defeat!")) {
            return true;
        }

        Pattern namePattern = Pattern.compile("(?i)(?u)\\b" + Pattern.quote(localPlayerName) + "\\b");

        int winnerIdx = lower.indexOf("победил:");
        if (winnerIdx < 0) winnerIdx = lower.indexOf("победитель:");
        if (winnerIdx < 0) winnerIdx = lower.indexOf("winner:");

        int loserIdx = lower.indexOf("проиграл:");
        if (loserIdx < 0) loserIdx = lower.indexOf("проигравший:");
        if (loserIdx < 0) loserIdx = lower.indexOf("loser:");

        if (winnerIdx >= 0 && loserIdx >= 0) {
            String winnerPart;
            String loserPart;
            if (winnerIdx < loserIdx) {
                winnerPart = clean.substring(winnerIdx, loserIdx);
                loserPart = clean.substring(loserIdx);
            } else {
                loserPart = clean.substring(loserIdx, winnerIdx);
                winnerPart = clean.substring(winnerIdx);
            }
            boolean inWinner = namePattern.matcher(winnerPart).find();
            boolean inLoser = namePattern.matcher(loserPart).find();
            return inLoser && !inWinner;
        }

        if (loserIdx >= 0) {
            String loserPart = clean.substring(loserIdx);
            String[] tokens = loserPart.split("[,|;\\n\\r.]");
            if (tokens.length > 0 && namePattern.matcher(tokens[0]).find()) {
                return true;
            }
        }

        Matcher m = DUEL_LOSS_REGEX_PATTERN.matcher(clean);
        if (m.find()) {
            String loser = m.group(2) != null ? m.group(2).trim() : null;
            if (loser != null && loser.equalsIgnoreCase(localPlayerName)) {
                return true;
            }
        }

        // Extracted loser check
        String loser = extractDuelLoser(clean);
        if (loser != null && loser.equalsIgnoreCase(localPlayerName)) {
            return true;
        }

        // When a duel completion notice declares someone else as the winner
        String winner = extractDuelWinner(clean);
        if (winner != null && !winner.equalsIgnoreCase(localPlayerName) && isSystemDuelMessage(clean)) {
            return true;
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
        recentVictimNames.clear();
        lastKnownTargetPositions.clear();
        lastAttributedKills.clear();
        lastAttributedKillNames.clear();
    }
}

package activity.client.module;

import activity.client.module.impl.utility.AutoGGKillTracker;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification unit tests for AutoGGKillTracker:
 * Direct melee attribution, explosion radius correlation, and multi-lingual chat kill parsing.
 */
public class AutoGGKillTrackerTest {

    @BeforeEach
    void setUp() {
        AutoGGKillTracker.reset();
    }

    @Test
    @DisplayName("Direct hit: Attributed when within 900ms window and <= 7.0 blocks")
    void testDirectHitWithinWindowAndReach() {
        int entityId = 42;
        long now = 10_000L;
        AutoGGKillTracker.recordAttack(entityId, new Vec3d(5.0, 64.0, 5.0), now);

        // Directly at 3.0 blocks within 400ms -> attributed
        assertTrue(AutoGGKillTracker.isAttributedDirectHit(entityId, 3.0, now + 400L));

        // At exactly 7.0 blocks within 899ms -> attributed
        assertTrue(AutoGGKillTracker.isAttributedDirectHit(entityId, 7.0, now + 899L));
    }

    @Test
    @DisplayName("Direct hit: Rejected when beyond 7.0 blocks reach")
    void testDirectHitBeyondReach() {
        int entityId = 42;
        long now = 10_000L;
        AutoGGKillTracker.recordAttack(entityId, new Vec3d(0.0, 64.0, 0.0), now);

        // At 7.1 blocks -> rejected
        assertFalse(AutoGGKillTracker.isAttributedDirectHit(entityId, 7.1, now + 200L));

        // At 15.0 blocks -> rejected
        assertFalse(AutoGGKillTracker.isAttributedDirectHit(entityId, 15.0, now + 100L));
    }

    @Test
    @DisplayName("Direct hit: Rejected when window exceeds 900ms")
    void testDirectHitExpiredWindow() {
        int entityId = 42;
        long now = 10_000L;
        AutoGGKillTracker.recordAttack(entityId, new Vec3d(0.0, 64.0, 0.0), now);

        // At 901ms -> rejected
        assertFalse(AutoGGKillTracker.isAttributedDirectHit(entityId, 3.0, now + 901L));

        // At 2000ms -> rejected
        assertFalse(AutoGGKillTracker.isAttributedDirectHit(entityId, 2.0, now + 2000L));
    }

    @Test
    @DisplayName("Direct hit: Unrecorded entity id returns false")
    void testDirectHitUnknownEntity() {
        assertFalse(AutoGGKillTracker.isAttributedDirectHit(999, 2.0, System.currentTimeMillis()));
    }

    @Test
    @DisplayName("Explosion: Attributed when target inside 8.5m radius and within 1500ms")
    void testExplosionAttributionWithinRadiusAndWindow() {
        long now = 20_000L;
        AutoGGKillTracker.recordExplosion(10.0, 64.0, 10.0, 8.5, now);

        // Victim at (13.0, 64.0, 10.0) -> distance 3.0m <= 8.5m within 1000ms -> attributed
        Vec3d victimPos = new Vec3d(13.0, 64.0, 10.0);
        assertTrue(AutoGGKillTracker.isAttributedExplosion(victimPos, now + 1000L));

        // Victim at 8.4m distance within 1490ms -> attributed
        Vec3d nearEdge = new Vec3d(10.0 + 8.4, 64.0, 10.0);
        assertTrue(AutoGGKillTracker.isAttributedExplosion(nearEdge, now + 1490L));
    }

    @Test
    @DisplayName("Explosion: Rejected when target outside blast radius")
    void testExplosionOutsideRadius() {
        long now = 20_000L;
        AutoGGKillTracker.recordExplosion(0.0, 64.0, 0.0, 8.5, now);

        // Victim at (10.0, 64.0, 0.0) -> distance 10.0m > 8.5m -> rejected
        Vec3d farVictim = new Vec3d(10.0, 64.0, 0.0);
        assertFalse(AutoGGKillTracker.isAttributedExplosion(farVictim, now + 500L));
    }

    @Test
    @DisplayName("Explosion: Rejected when window exceeds 1500ms")
    void testExplosionExpiredWindow() {
        long now = 20_000L;
        AutoGGKillTracker.recordExplosion(0.0, 64.0, 0.0, 8.5, now);

        Vec3d closeVictim = new Vec3d(2.0, 64.0, 0.0);
        assertFalse(AutoGGKillTracker.isAttributedExplosion(closeVictim, now + 1501L));
        assertFalse(AutoGGKillTracker.isAttributedExplosion(closeVictim, now + 3000L));
    }

    @Test
    @DisplayName("Explosion: Null target coordinates safely rejected")
    void testExplosionNullPos() {
        AutoGGKillTracker.recordExplosion(0.0, 64.0, 0.0, 8.5, System.currentTimeMillis());
        assertFalse(AutoGGKillTracker.isAttributedExplosion(null, System.currentTimeMillis()));
    }

    @Test
    @DisplayName("Chat kill parsing: Russian server kill feed formats")
    void testRussianChatKillParsing() {
        String local = "Nivorat";

        // Classic byl ubit
        assertEquals("Enemy1", AutoGGKillTracker.parseChatKill("Enemy1 был убит Nivorat", local));

        // Color codes stripped
        assertEquals("Alex", AutoGGKillTracker.parseChatKill("§cAlex §7был убит §aNivorat", local));

        // Pogib ot vzryva
        assertEquals("CreeperMan", AutoGGKillTracker.parseChatKill("CreeperMan погиб от взрыва Nivorat", local));

        // Pal ot ruki
        assertEquals("Warrior99", AutoGGKillTracker.parseChatKill("Warrior99 пал от руки Nivorat", local));

        // Vzorvan
        assertEquals("Target1", AutoGGKillTracker.parseChatKill("Target1 взорван Nivorat", local));

        // Zarublen
        assertEquals("Target2", AutoGGKillTracker.parseChatKill("Target2 был зарублен Nivorat", local));

        // Rasstrelyan
        assertEquals("Target3", AutoGGKillTracker.parseChatKill("Target3 расстрелян Nivorat", local));
    }

    @Test
    @DisplayName("Chat kill parsing: English server kill feed formats")
    void testEnglishChatKillParsing() {
        String local = "Nivorat";

        // was slain by
        assertEquals("Player123", AutoGGKillTracker.parseChatKill("Player123 was slain by Nivorat", local));

        // was blown up by
        assertEquals("BombVictim", AutoGGKillTracker.parseChatKill("BombVictim was blown up by Nivorat", local));

        // was shot by
        assertEquals("BowTarget", AutoGGKillTracker.parseChatKill("BowTarget was shot by Nivorat", local));

        // was killed by
        assertEquals("BadGuy", AutoGGKillTracker.parseChatKill("BadGuy was killed by Nivorat", local));
    }

    @Test
    @DisplayName("Chat kill parsing: Discards kills made by others or local player suicide/death")
    void testChatKillAttributionRejection() {
        String local = "Nivorat";

        // Someone else killed the victim
        assertNull(AutoGGKillTracker.parseChatKill("Victim was slain by OtherPlayer", local));

        // Local player died to someone else
        assertNull(AutoGGKillTracker.parseChatKill("Nivorat was slain by OtherPlayer", local));

        // Local player died to themselves
        assertNull(AutoGGKillTracker.parseChatKill("Nivorat был убит Nivorat", local));

        // Unrelated chat messages
        assertNull(AutoGGKillTracker.parseChatKill("GGWP everyone!", local));
        assertNull(AutoGGKillTracker.parseChatKill("<Nivorat> Hello world", local));
        assertNull(AutoGGKillTracker.parseChatKill(null, local));
        assertNull(AutoGGKillTracker.parseChatKill("Enemy was slain by Nivorat", null));
    }

    @Test
    @DisplayName("Cart placement: Tracking proximity to placed carts within 6000ms window")
    void testCartPlacementTracking() {
        AutoGGKillTracker.recordCartPlacement(10.0, 64.0, 10.0);

        // Distance 2 blocks <= 8.5m -> true
        assertTrue(AutoGGKillTracker.isNearbyPlacedCart(12.0, 64.0, 10.0, 8.5));
        // Distance 8.0 blocks <= 8.5m -> true
        assertTrue(AutoGGKillTracker.isNearbyPlacedCart(10.0, 64.0, 18.0, 8.5));

        // Distance 20 blocks > 8.5m -> false
        assertFalse(AutoGGKillTracker.isNearbyPlacedCart(30.0, 64.0, 10.0, 8.5));
    }

    @Test
    @DisplayName("Own death message parsing: Accurately identifies when local player died")
    void testIsOwnDeathMessage() {
        String local = "Nivorat";

        assertTrue(AutoGGKillTracker.isOwnDeathMessage("Nivorat был убит OtherPlayer", local));
        assertTrue(AutoGGKillTracker.isOwnDeathMessage("Nivorat was slain by Enemy", local));
        assertTrue(AutoGGKillTracker.isOwnDeathMessage("Nivorat погиб от взрыва Enemy", local));
        assertTrue(AutoGGKillTracker.isOwnDeathMessage("§cNivorat §7разбился", local));
        assertTrue(AutoGGKillTracker.isOwnDeathMessage("Nivorat drowned", local));

        // Direct server notices where player username is omitted (addressed as "Вы" / "Вас")
        assertTrue(AutoGGKillTracker.isOwnDeathMessage("Вы погибли", local));
        assertTrue(AutoGGKillTracker.isOwnDeathMessage("Вы умерли", local));
        assertTrue(AutoGGKillTracker.isOwnDeathMessage("Вас убил BadGuy", local));
        assertTrue(AutoGGKillTracker.isOwnDeathMessage("Вы были убиты игроком BadGuy", local));
        assertTrue(AutoGGKillTracker.isOwnDeathMessage("Вы разбились", local));
        assertTrue(AutoGGKillTracker.isOwnDeathMessage("Вы сгорели в лаве", local));
        assertTrue(AutoGGKillTracker.isOwnDeathMessage("Вы подорвались", local));
        assertTrue(AutoGGKillTracker.isOwnDeathMessage("You died", local));
        assertTrue(AutoGGKillTracker.isOwnDeathMessage("You were killed by Enemy", local));

        // Kills scored by local player should NOT be marked as own death
        assertFalse(AutoGGKillTracker.isOwnDeathMessage("Enemy был убит Nivorat", local));
        assertFalse(AutoGGKillTracker.isOwnDeathMessage("Enemy was slain by Nivorat", local));
        assertFalse(AutoGGKillTracker.isOwnDeathMessage("Вы убили Enemy", local));
        assertFalse(AutoGGKillTracker.isOwnDeathMessage("Вы успешно убили игрока Enemy", local));
        assertFalse(AutoGGKillTracker.isOwnDeathMessage("Hello everyone in chat!", local));
        assertFalse(AutoGGKillTracker.isOwnDeathMessage(null, local));
    }

    @Test
    @DisplayName("Direct kill notices: Only 2nd person notices attribute; server broadcasts like 'Kill: ...' rejected")
    void testDirectKillAttribution() {
        String local = "Nivorat";

        // Direct 2nd person kill notices
        assertEquals("Enemy1", AutoGGKillTracker.parseDirectKill("Вы убили Enemy1", local));
        assertEquals("ProPlayer", AutoGGKillTracker.parseDirectKill("Вы успешно убили игрока ProPlayer", local));
        assertEquals("TargetX", AutoGGKillTracker.parseDirectKill("Вы одолели игрока TargetX", local));
        assertEquals("Gamer", AutoGGKillTracker.parseDirectKill("You killed Gamer", local));
        assertEquals("Boss", AutoGGKillTracker.parseDirectKill("You slayed Boss", local));
        assertEquals("Enemy2", AutoGGKillTracker.parseDirectKill("You defeated Enemy2", local));

        // Global server broadcasts must NOT be attributed as direct kills
        assertNull(AutoGGKillTracker.parseDirectKill("Убийство: Creeper99", local));
        assertNull(AutoGGKillTracker.parseDirectKill("Килл: Speedy", local));
        assertNull(AutoGGKillTracker.parseDirectKill("Kill: FastGuy", local));

        // Player chat messages mentioning direct kills must NOT trigger
        assertNull(AutoGGKillTracker.parseDirectKill("<Steve> Вы убили Enemy1", local));
        assertNull(AutoGGKillTracker.parseDirectKill("Steve: You killed Gamer", local));
        assertNull(AutoGGKillTracker.parseDirectKill("[VIP] Steve: Вы убили ProPlayer", local));

        // Self-kill rejected
        assertNull(AutoGGKillTracker.parseDirectKill("Вы убили Nivorat", local));
    }

    @Test
    @DisplayName("Duel Win Detection: Direct notices, titles, structured and regex announcements")
    void testDuelWinDetection() {
        String local = "Nivorat";

        // Title packets / exact messages
        assertTrue(AutoGGKillTracker.isDuelWinMessage("ПОБЕДА!", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Победа", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("VICTORY!", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Victory", local));

        // Direct notices
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Вы победили в дуэли!", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Вы выиграли дуэль!", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Вы одержали победу над игроком Enemy!", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Ваша победа!", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("You won the duel!", local));

        // Structured formats (both winner-first and loser-first)
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Победил: Nivorat | Проиграл: Enemy", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Победитель: Nivorat, Проигравший: Enemy", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Проиграл: Enemy, Победил: Nivorat", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Winner: Nivorat, Loser: Enemy", local));

        // Regex duel formats
        assertTrue(AutoGGKillTracker.isDuelWinMessage("[Дуэли] Nivorat одержал победу над игроком Enemy", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Nivorat выиграл дуэль у Enemy", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Nivorat won the duel", local));

        // Rejections: local player lost
        assertFalse(AutoGGKillTracker.isDuelWinMessage("Победил: Enemy | Проиграл: Nivorat", local));
        assertFalse(AutoGGKillTracker.isDuelWinMessage("Проиграл: Nivorat, Победил: Enemy", local));
        assertFalse(AutoGGKillTracker.isDuelWinMessage("[Дуэли] Enemy победил игрока Nivorat", local));

        // Rejections: unrelated players
        assertFalse(AutoGGKillTracker.isDuelWinMessage("Победил: PlayerA | Проиграл: PlayerB", local));
        assertFalse(AutoGGKillTracker.isDuelWinMessage("PlayerA won the duel", local));

        // Rejections: player chat messages
        assertFalse(AutoGGKillTracker.isDuelWinMessage("<Troll> Вы победили!", local));
        assertFalse(AutoGGKillTracker.isDuelWinMessage("Steve: You won", local));
        assertFalse(AutoGGKillTracker.isDuelWinMessage("[VIP] Steve: Вы одержали победу", local));
    }

    @Test
    @DisplayName("Duel Loss Detection: Direct notices, titles, structured and regex announcements")
    void testDuelLossDetection() {
        String local = "Nivorat";

        // Title packets / exact messages
        assertTrue(AutoGGKillTracker.isDuelLossMessage("ПОРАЖЕНИЕ!", local));
        assertTrue(AutoGGKillTracker.isDuelLossMessage("Поражение", local));
        assertTrue(AutoGGKillTracker.isDuelLossMessage("DEFEAT!", local));
        assertTrue(AutoGGKillTracker.isDuelLossMessage("Defeat", local));

        // Direct notices
        assertTrue(AutoGGKillTracker.isDuelLossMessage("Вы проиграли в дуэли!", local));
        assertTrue(AutoGGKillTracker.isDuelLossMessage("Вы потерпели поражение!", local));
        assertTrue(AutoGGKillTracker.isDuelLossMessage("You lost the duel!", local));

        // Structured formats (both winner-first and loser-first)
        assertTrue(AutoGGKillTracker.isDuelLossMessage("Победил: Enemy | Проиграл: Nivorat", local));
        assertTrue(AutoGGKillTracker.isDuelLossMessage("Проиграл: Nivorat, Победил: Enemy", local));
        assertTrue(AutoGGKillTracker.isDuelLossMessage("Winner: Enemy, Loser: Nivorat", local));

        // Regex duel formats
        assertTrue(AutoGGKillTracker.isDuelLossMessage("[Дуэли] Enemy победил игрока Nivorat", local));
        assertTrue(AutoGGKillTracker.isDuelLossMessage("Enemy одержал победу над игроком Nivorat", local));

        // Rejections: local player won
        assertFalse(AutoGGKillTracker.isDuelLossMessage("Победил: Nivorat | Проиграл: Enemy", local));
        assertFalse(AutoGGKillTracker.isDuelLossMessage("Проиграл: Enemy, Победил: Nivorat", local));
        assertFalse(AutoGGKillTracker.isDuelLossMessage("[Дуэли] Nivorat победил игрока Enemy", local));

        // Rejections: player chat messages
        assertFalse(AutoGGKillTracker.isDuelLossMessage("<Troll> Вы проиграли!", local));
        assertFalse(AutoGGKillTracker.isDuelLossMessage("Steve: You lost", local));
    }

    @Test
    @DisplayName("Own death: Word boundary prevents false triggers on substring names")
    void testOwnDeathSubstringWordBoundary() {
        // Player named 'Dan' should NOT trigger when 'DangerZone' dies
        assertFalse(AutoGGKillTracker.isOwnDeathMessage("DangerZone умер", "Dan"));
        assertFalse(AutoGGKillTracker.isOwnDeathMessage("DangerZone погиб в лаве", "Dan"));

        // Player named 'Alex' should NOT trigger when 'Alexander' dies
        assertFalse(AutoGGKillTracker.isOwnDeathMessage("Alexander умер", "Alex"));
        assertFalse(AutoGGKillTracker.isOwnDeathMessage("Alexander разбился", "Alex"));

        // Exact name Dan DOES trigger
        assertTrue(AutoGGKillTracker.isOwnDeathMessage("Dan умер", "Dan"));
        assertTrue(AutoGGKillTracker.isOwnDeathMessage("Dan погиб в лаве", "Dan"));
        assertTrue(AutoGGKillTracker.isOwnDeathMessage("§cDan §7разбился", "Dan"));

        // Player chat messages mentioning death do NOT trigger own death
        assertFalse(AutoGGKillTracker.isOwnDeathMessage("<Enemy> Вы погибли", "Nivorat"));
        assertFalse(AutoGGKillTracker.isOwnDeathMessage("Enemy: You died", "Nivorat"));
    }

    @Test
    @DisplayName("Killer-victim PvP formats: '<Killer> убил <Victim>', 'Killer ⚔ Victim', 'Killer » Victim'")
    void testKillerVictimPatterns() {
        String local = "Nivorat";

        assertEquals("Target1", AutoGGKillTracker.parseChatKill("Nivorat убил Target1", local));
        assertEquals("Target2", AutoGGKillTracker.parseChatKill("Nivorat зарубил Target2", local));
        assertEquals("CartVictim", AutoGGKillTracker.parseChatKill("Nivorat взорвал CartVictim", local));
        assertEquals("SniperTarget", AutoGGKillTracker.parseChatKill("Nivorat расстрелял SniperTarget", local));
        assertEquals("ArenaEnemy", AutoGGKillTracker.parseChatKill("[FFA] Nivorat ⚔ ArenaEnemy", local));
        assertEquals("DuoEnemy", AutoGGKillTracker.parseChatKill("Nivorat -> DuoEnemy", local));
        assertEquals("SwordVictim", AutoGGKillTracker.parseChatKill("Nivorat » SwordVictim", local));
        assertEquals("VictimEN", AutoGGKillTracker.parseChatKill("Nivorat killed VictimEN", local));

        // Another player scored the kill -> null
        assertNull(AutoGGKillTracker.parseChatKill("OtherPlayer убил Target1", local));
        assertNull(AutoGGKillTracker.parseChatKill("OtherPlayer ⚔ Target1", local));

        // Normal chat message must NOT be misidentified as a kill
        assertNull(AutoGGKillTracker.parseChatKill("<Nivorat> Hello world", local));
        assertNull(AutoGGKillTracker.parseChatKill("<Nivorat> Good game everyone", local));
    }

    @Test
    @DisplayName("Combat tracking: isRecentlyAttacked within 4500ms window")
    void testIsRecentlyAttacked() {
        int targetId = 1234;
        assertFalse(AutoGGKillTracker.isRecentlyAttacked(targetId));

        AutoGGKillTracker.recordAttack(targetId);
        assertTrue(AutoGGKillTracker.isRecentlyAttacked(targetId));

        AutoGGKillTracker.reset();
        assertFalse(AutoGGKillTracker.isRecentlyAttacked(targetId));
    }

    @Test
    @DisplayName("Duel results: Russian & English duel server announcements with colons parsed accurately")
    void testDuelAnnouncementsWithColons() {
        String local = "Nivorat";

        // Wins
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Дуэль завершена! Победитель: Nivorat", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Дуэль окончена! Победитель: Nivorat", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Результаты дуэли: Победитель: Nivorat", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Дуэли » Победитель: Nivorat", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("[Дуэли] Игра окончена. Победил: Nivorat", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Победитель дуэли — Nivorat", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Winner: Nivorat", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Nivorat won the duel", local));

        // When someone else wins, local did not win
        assertFalse(AutoGGKillTracker.isDuelWinMessage("Дуэль завершена! Победитель: OtherGuy", local));
        assertFalse(AutoGGKillTracker.isDuelWinMessage("Winner: OtherGuy", local));

        // Losses
        assertTrue(AutoGGKillTracker.isDuelLossMessage("Дуэль окончена! Проигравший: Nivorat", local));
        assertTrue(AutoGGKillTracker.isDuelLossMessage("Дуэль завершена! Победитель: OtherGuy, Проиграл: Nivorat", local));
        assertTrue(AutoGGKillTracker.isDuelLossMessage("Loser: Nivorat", local));
        assertTrue(AutoGGKillTracker.isDuelLossMessage("Winner: OtherGuy, Loser: Nivorat", local));

        // When local won, it's not a loss
        assertFalse(AutoGGKillTracker.isDuelLossMessage("Дуэль завершена! Победитель: Nivorat", local));
        assertFalse(AutoGGKillTracker.isDuelLossMessage("Winner: Nivorat", local));

        // False positives: regular player chatting in angle brackets should NOT trigger duel win/loss
        assertFalse(AutoGGKillTracker.isDuelWinMessage("<RandomPlayer> Дуэль завершена! Победитель: Nivorat", local));
        assertFalse(AutoGGKillTracker.isDuelLossMessage("<RandomPlayer> Дуэль окончена! Проигравший: Nivorat", local));
    }
}

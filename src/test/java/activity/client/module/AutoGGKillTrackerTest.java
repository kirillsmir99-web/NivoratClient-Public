package activity.client.module;

import activity.client.module.impl.utility.AutoGGKillTracker;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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

        assertTrue(AutoGGKillTracker.isAttributedDirectHit(entityId, 3.0, now + 400L));

        assertTrue(AutoGGKillTracker.isAttributedDirectHit(entityId, 7.0, now + 899L));
    }

    @Test
    @DisplayName("Direct hit: Rejected when beyond 7.0 blocks reach")
    void testDirectHitBeyondReach() {
        int entityId = 42;
        long now = 10_000L;
        AutoGGKillTracker.recordAttack(entityId, new Vec3d(0.0, 64.0, 0.0), now);

        assertFalse(AutoGGKillTracker.isAttributedDirectHit(entityId, 7.1, now + 200L));

        assertFalse(AutoGGKillTracker.isAttributedDirectHit(entityId, 15.0, now + 100L));
    }

    @Test
    @DisplayName("Direct hit: Rejected when window exceeds 900ms")
    void testDirectHitExpiredWindow() {
        int entityId = 42;
        long now = 10_000L;
        AutoGGKillTracker.recordAttack(entityId, new Vec3d(0.0, 64.0, 0.0), now);

        assertFalse(AutoGGKillTracker.isAttributedDirectHit(entityId, 3.0, now + 901L));

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

        Vec3d victimPos = new Vec3d(13.0, 64.0, 10.0);
        assertTrue(AutoGGKillTracker.isAttributedExplosion(victimPos, now + 1000L));

        Vec3d nearEdge = new Vec3d(10.0 + 8.4, 64.0, 10.0);
        assertTrue(AutoGGKillTracker.isAttributedExplosion(nearEdge, now + 1490L));
    }

    @Test
    @DisplayName("Explosion: Rejected when target outside blast radius")
    void testExplosionOutsideRadius() {
        long now = 20_000L;
        AutoGGKillTracker.recordExplosion(0.0, 64.0, 0.0, 8.5, now);

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

        assertEquals("Enemy1", AutoGGKillTracker.parseChatKill("Enemy1 был убит Nivorat", local));

        assertEquals("Alex", AutoGGKillTracker.parseChatKill("§cAlex §7был убит §aNivorat", local));

        assertEquals("CreeperMan", AutoGGKillTracker.parseChatKill("CreeperMan погиб от взрыва Nivorat", local));

        assertEquals("Warrior99", AutoGGKillTracker.parseChatKill("Warrior99 пал от руки Nivorat", local));

        assertEquals("Target1", AutoGGKillTracker.parseChatKill("Target1 взорван Nivorat", local));

        assertEquals("Target2", AutoGGKillTracker.parseChatKill("Target2 был зарублен Nivorat", local));

        assertEquals("Target3", AutoGGKillTracker.parseChatKill("Target3 расстрелян Nivorat", local));
    }

    @Test
    @DisplayName("Chat kill parsing: English server kill feed formats")
    void testEnglishChatKillParsing() {
        String local = "Nivorat";

        assertEquals("Player123", AutoGGKillTracker.parseChatKill("Player123 was slain by Nivorat", local));

        assertEquals("BombVictim", AutoGGKillTracker.parseChatKill("BombVictim was blown up by Nivorat", local));

        assertEquals("BowTarget", AutoGGKillTracker.parseChatKill("BowTarget was shot by Nivorat", local));

        assertEquals("BadGuy", AutoGGKillTracker.parseChatKill("BadGuy was killed by Nivorat", local));
    }

    @Test
    @DisplayName("Chat kill parsing: Discards kills made by others or local player suicide/death")
    void testChatKillAttributionRejection() {
        String local = "Nivorat";

        assertNull(AutoGGKillTracker.parseChatKill("Victim was slain by OtherPlayer", local));

        assertNull(AutoGGKillTracker.parseChatKill("Nivorat was slain by OtherPlayer", local));

        assertNull(AutoGGKillTracker.parseChatKill("Nivorat был убит Nivorat", local));

        assertNull(AutoGGKillTracker.parseChatKill("GGWP everyone!", local));
        assertNull(AutoGGKillTracker.parseChatKill("<Nivorat> Hello world", local));
        assertNull(AutoGGKillTracker.parseChatKill(null, local));
        assertNull(AutoGGKillTracker.parseChatKill("Enemy was slain by Nivorat", null));
    }

    @Test
    @DisplayName("Cart placement: Tracking proximity to placed carts within 6000ms window")
    void testCartPlacementTracking() {
        AutoGGKillTracker.recordCartPlacement(10.0, 64.0, 10.0);

        assertTrue(AutoGGKillTracker.isNearbyPlacedCart(12.0, 64.0, 10.0, 8.5));

        assertTrue(AutoGGKillTracker.isNearbyPlacedCart(10.0, 64.0, 18.0, 8.5));

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

        assertTrue(AutoGGKillTracker.isOwnDeathMessage("Вы погибли", local));
        assertTrue(AutoGGKillTracker.isOwnDeathMessage("Вы умерли", local));
        assertTrue(AutoGGKillTracker.isOwnDeathMessage("Вас убил BadGuy", local));
        assertTrue(AutoGGKillTracker.isOwnDeathMessage("Вы были убиты игроком BadGuy", local));
        assertTrue(AutoGGKillTracker.isOwnDeathMessage("Вы разбились", local));
        assertTrue(AutoGGKillTracker.isOwnDeathMessage("Вы сгорели в лаве", local));
        assertTrue(AutoGGKillTracker.isOwnDeathMessage("Вы подорвались", local));
        assertTrue(AutoGGKillTracker.isOwnDeathMessage("You died", local));
        assertTrue(AutoGGKillTracker.isOwnDeathMessage("You were killed by Enemy", local));

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

        assertEquals("Enemy1", AutoGGKillTracker.parseDirectKill("Вы убили Enemy1", local));
        assertEquals("ProPlayer", AutoGGKillTracker.parseDirectKill("Вы успешно убили игрока ProPlayer", local));
        assertEquals("TargetX", AutoGGKillTracker.parseDirectKill("Вы одолели игрока TargetX", local));
        assertEquals("Gamer", AutoGGKillTracker.parseDirectKill("You killed Gamer", local));
        assertEquals("Boss", AutoGGKillTracker.parseDirectKill("You slayed Boss", local));
        assertEquals("Enemy2", AutoGGKillTracker.parseDirectKill("You defeated Enemy2", local));

        assertNull(AutoGGKillTracker.parseDirectKill("Убийство: Creeper99", local));
        assertNull(AutoGGKillTracker.parseDirectKill("Килл: Speedy", local));
        assertNull(AutoGGKillTracker.parseDirectKill("Kill: FastGuy", local));

        assertNull(AutoGGKillTracker.parseDirectKill("<Steve> Вы убили Enemy1", local));
        assertNull(AutoGGKillTracker.parseDirectKill("Steve: You killed Gamer", local));
        assertNull(AutoGGKillTracker.parseDirectKill("[VIP] Steve: Вы убили ProPlayer", local));

        assertNull(AutoGGKillTracker.parseDirectKill("Вы убили Nivorat", local));
    }

    @Test
    @DisplayName("Duel Win Detection: Direct notices, titles, structured and regex announcements")
    void testDuelWinDetection() {
        String local = "Nivorat";

        assertTrue(AutoGGKillTracker.isDuelWinMessage("ПОБЕДА!", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Победа", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("VICTORY!", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Victory", local));

        assertTrue(AutoGGKillTracker.isDuelWinMessage("Вы победили в дуэли!", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Вы выиграли дуэль!", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Вы одержали победу над игроком Enemy!", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Ваша победа!", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("You won the duel!", local));

        assertTrue(AutoGGKillTracker.isDuelWinMessage("Победил: Nivorat | Проиграл: Enemy", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Победитель: Nivorat, Проигравший: Enemy", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Проиграл: Enemy, Победил: Nivorat", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Winner: Nivorat, Loser: Enemy", local));

        assertTrue(AutoGGKillTracker.isDuelWinMessage("[Дуэли] Nivorat одержал победу над игроком Enemy", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Nivorat выиграл дуэль у Enemy", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Nivorat won the duel", local));

        assertFalse(AutoGGKillTracker.isDuelWinMessage("Победил: Enemy | Проиграл: Nivorat", local));
        assertFalse(AutoGGKillTracker.isDuelWinMessage("Проиграл: Nivorat, Победил: Enemy", local));
        assertFalse(AutoGGKillTracker.isDuelWinMessage("[Дуэли] Enemy победил игрока Nivorat", local));

        assertFalse(AutoGGKillTracker.isDuelWinMessage("Победил: PlayerA | Проиграл: PlayerB", local));
        assertFalse(AutoGGKillTracker.isDuelWinMessage("PlayerA won the duel", local));

        assertFalse(AutoGGKillTracker.isDuelWinMessage("<Troll> Вы победили!", local));
        assertFalse(AutoGGKillTracker.isDuelWinMessage("Steve: You won", local));
        assertFalse(AutoGGKillTracker.isDuelWinMessage("[VIP] Steve: Вы одержали победу", local));
    }

    @Test
    @DisplayName("Duel Loss Detection: Direct notices, titles, structured and regex announcements")
    void testDuelLossDetection() {
        String local = "Nivorat";

        assertTrue(AutoGGKillTracker.isDuelLossMessage("ПОРАЖЕНИЕ!", local));
        assertTrue(AutoGGKillTracker.isDuelLossMessage("Поражение", local));
        assertTrue(AutoGGKillTracker.isDuelLossMessage("DEFEAT!", local));
        assertTrue(AutoGGKillTracker.isDuelLossMessage("Defeat", local));

        assertTrue(AutoGGKillTracker.isDuelLossMessage("Вы проиграли в дуэли!", local));
        assertTrue(AutoGGKillTracker.isDuelLossMessage("Вы потерпели поражение!", local));
        assertTrue(AutoGGKillTracker.isDuelLossMessage("You lost the duel!", local));

        assertTrue(AutoGGKillTracker.isDuelLossMessage("Победил: Enemy | Проиграл: Nivorat", local));
        assertTrue(AutoGGKillTracker.isDuelLossMessage("Проиграл: Nivorat, Победил: Enemy", local));
        assertTrue(AutoGGKillTracker.isDuelLossMessage("Winner: Enemy, Loser: Nivorat", local));

        assertTrue(AutoGGKillTracker.isDuelLossMessage("[Дуэли] Enemy победил игрока Nivorat", local));
        assertTrue(AutoGGKillTracker.isDuelLossMessage("Enemy одержал победу над игроком Nivorat", local));

        assertFalse(AutoGGKillTracker.isDuelLossMessage("Победил: Nivorat | Проиграл: Enemy", local));
        assertFalse(AutoGGKillTracker.isDuelLossMessage("Проиграл: Enemy, Победил: Nivorat", local));
        assertFalse(AutoGGKillTracker.isDuelLossMessage("[Дуэли] Nivorat победил игрока Enemy", local));

        assertFalse(AutoGGKillTracker.isDuelLossMessage("<Troll> Вы проиграли!", local));
        assertFalse(AutoGGKillTracker.isDuelLossMessage("Steve: You lost", local));
    }

    @Test
    @DisplayName("Own death: Word boundary prevents false triggers on substring names")
    void testOwnDeathSubstringWordBoundary() {

        assertFalse(AutoGGKillTracker.isOwnDeathMessage("DangerZone умер", "Dan"));
        assertFalse(AutoGGKillTracker.isOwnDeathMessage("DangerZone погиб в лаве", "Dan"));

        assertFalse(AutoGGKillTracker.isOwnDeathMessage("Alexander умер", "Alex"));
        assertFalse(AutoGGKillTracker.isOwnDeathMessage("Alexander разбился", "Alex"));

        assertTrue(AutoGGKillTracker.isOwnDeathMessage("Dan умер", "Dan"));
        assertTrue(AutoGGKillTracker.isOwnDeathMessage("Dan погиб в лаве", "Dan"));
        assertTrue(AutoGGKillTracker.isOwnDeathMessage("§cDan §7разбился", "Dan"));

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

        assertNull(AutoGGKillTracker.parseChatKill("OtherPlayer убил Target1", local));
        assertNull(AutoGGKillTracker.parseChatKill("OtherPlayer ⚔ Target1", local));

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

        assertTrue(AutoGGKillTracker.isDuelWinMessage("Дуэль завершена! Победитель: Nivorat", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Дуэль окончена! Победитель: Nivorat", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Результаты дуэли: Победитель: Nivorat", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Дуэли » Победитель: Nivorat", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("[Дуэли] Игра окончена. Победил: Nivorat", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Победитель дуэли — Nivorat", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Winner: Nivorat", local));
        assertTrue(AutoGGKillTracker.isDuelWinMessage("Nivorat won the duel", local));

        assertFalse(AutoGGKillTracker.isDuelWinMessage("Дуэль завершена! Победитель: OtherGuy", local));
        assertFalse(AutoGGKillTracker.isDuelWinMessage("Winner: OtherGuy", local));

        assertTrue(AutoGGKillTracker.isDuelLossMessage("Дуэль окончена! Проигравший: Nivorat", local));
        assertTrue(AutoGGKillTracker.isDuelLossMessage("Дуэль завершена! Победитель: OtherGuy, Проиграл: Nivorat", local));
        assertTrue(AutoGGKillTracker.isDuelLossMessage("Loser: Nivorat", local));
        assertTrue(AutoGGKillTracker.isDuelLossMessage("Winner: OtherGuy, Loser: Nivorat", local));

        assertFalse(AutoGGKillTracker.isDuelLossMessage("Дуэль завершена! Победитель: Nivorat", local));
        assertFalse(AutoGGKillTracker.isDuelLossMessage("Winner: Nivorat", local));

        assertFalse(AutoGGKillTracker.isDuelWinMessage("<RandomPlayer> Дуэль завершена! Победитель: Nivorat", local));
        assertFalse(AutoGGKillTracker.isDuelLossMessage("<RandomPlayer> Дуэль окончена! Проигравший: Nivorat", local));
    }

    @Test
    @DisplayName("Lobby and Welcome Messages: Rejection to prevent false AutoGG triggers on server connect")
    void testLobbyWelcomeMessagesRejection() {
        String local = "Nivorat";

        assertTrue(AutoGGKillTracker.isLobbyOrWelcomeMessage("Добро пожаловать на сервер Дуэлей!"));
        assertTrue(AutoGGKillTracker.isLobbyOrWelcomeMessage("Welcome to the server, Nivorat!"));
        assertTrue(AutoGGKillTracker.isLobbyOrWelcomeMessage("Ваша статистика: Побед: 125, Поражений: 30"));
        assertTrue(AutoGGKillTracker.isLobbyOrWelcomeMessage("Игрок Nivorat подключился к серверу"));
        assertTrue(AutoGGKillTracker.isLobbyOrWelcomeMessage("Онлайн: 45/100 | Сервер: Duels-1"));
        assertTrue(AutoGGKillTracker.isLobbyOrWelcomeMessage("Наш Discord: discord.gg/example"));
        assertTrue(AutoGGKillTracker.isLobbyOrWelcomeMessage("Переход в лобби..."));

        assertFalse(AutoGGKillTracker.isDuelWinMessage("[Дуэли] Добро пожаловать, Nivorat! Побед: 50, Поражений: 10", local));
        assertFalse(AutoGGKillTracker.isDuelWinMessage("Статистика игрока Nivorat: побед: 100", local));
        assertFalse(AutoGGKillTracker.isDuelWinMessage("Игрок Nivorat подключился к серверу Duels", local));

        assertFalse(AutoGGKillTracker.isDuelLossMessage("[Дуэли] Добро пожаловать, Nivorat! Поражений: 20", local));
        assertFalse(AutoGGKillTracker.isDuelLossMessage("Сервер перезагружается, переход в лобби", local));

        assertNull(AutoGGKillTracker.parseChatKill("Nivorat подключился к серверу", local));
        assertFalse(AutoGGKillTracker.isOwnDeathMessage("Nivorat подключился к серверу", local));
    }

    @Test
    @DisplayName("Third-party duels: Messages between other players never trigger loss for local player")
    void testThirdPartyDuelRejection() {
        String local = "Nivorat";

        assertFalse(AutoGGKillTracker.isDuelLossMessage("Дуэль завершена! Победитель: Steve, Проиграл: Alex", local));
        assertFalse(AutoGGKillTracker.isDuelLossMessage("[Дуэли] Steve победил игрока Alex", local));
        assertFalse(AutoGGKillTracker.isDuelLossMessage("Winner: Steve, Loser: Alex", local));
        assertFalse(AutoGGKillTracker.isDuelLossMessage("Steve одержал победу над игроком Alex", local));
        assertFalse(AutoGGKillTracker.isDuelLossMessage("Дуэль окончена! Победитель: Steve", local));
    }

    @Test
    @DisplayName("AutoGGClient: Server transfer resets pending state and enters grace period")
    void testAutoGGServerTransferGracePeriod() {
        ru.elarion.autogg.AutoGGClient.resetStateForTest();
        assertFalse(ru.elarion.autogg.AutoGGClient.isInGracePeriod());
        assertFalse(ru.elarion.autogg.AutoGGClient.hasPendingPhrase());

        ru.elarion.autogg.AutoGGClient.onServerTransferOrRespawn();
        assertTrue(ru.elarion.autogg.AutoGGClient.isInGracePeriod());
        assertFalse(ru.elarion.autogg.AutoGGClient.hasPendingPhrase());

        ru.elarion.autogg.AutoGGClient.markOwnDeath();
        assertFalse(ru.elarion.autogg.AutoGGClient.hasPendingPhrase());

        ru.elarion.autogg.AutoGGClient.triggerConfirmedKill("Enemy");
        assertFalse(ru.elarion.autogg.AutoGGClient.hasPendingPhrase());

        ru.elarion.autogg.AutoGGClient.resetStateForTest();
        assertFalse(ru.elarion.autogg.AutoGGClient.isInGracePeriod());
    }
}

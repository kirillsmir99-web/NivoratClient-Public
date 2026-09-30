package activity.client.module;

import activity.client.module.impl.utility.AudioWaveTracker;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AutoGGKillTrackerTest {

    @BeforeEach
    void setUp() {
        AudioWaveTracker.reset();
    }

    @Test
    @DisplayName("Direct hit: Attributed when within 900ms window and <= 7.0 blocks")
    void testDirectHitWithinWindowAndReach() {
        int entityId = 42;
        long now = 10_000L;
        AudioWaveTracker.recordAttack(entityId, new Vec3d(5.0, 64.0, 5.0), now);

        assertTrue(AudioWaveTracker.isAttributedDirectHit(entityId, 3.0, now + 400L));

        assertTrue(AudioWaveTracker.isAttributedDirectHit(entityId, 7.0, now + 899L));
    }

    @Test
    @DisplayName("Direct hit: Rejected when beyond 7.0 blocks reach")
    void testDirectHitBeyondReach() {
        int entityId = 42;
        long now = 10_000L;
        AudioWaveTracker.recordAttack(entityId, new Vec3d(0.0, 64.0, 0.0), now);

        assertFalse(AudioWaveTracker.isAttributedDirectHit(entityId, 7.1, now + 200L));

        assertFalse(AudioWaveTracker.isAttributedDirectHit(entityId, 15.0, now + 100L));
    }

    @Test
    @DisplayName("Direct hit: Rejected when window exceeds 900ms")
    void testDirectHitExpiredWindow() {
        int entityId = 42;
        long now = 10_000L;
        AudioWaveTracker.recordAttack(entityId, new Vec3d(0.0, 64.0, 0.0), now);

        assertFalse(AudioWaveTracker.isAttributedDirectHit(entityId, 3.0, now + 901L));

        assertFalse(AudioWaveTracker.isAttributedDirectHit(entityId, 2.0, now + 2000L));
    }

    @Test
    @DisplayName("Direct hit: Unrecorded entity id returns false")
    void testDirectHitUnknownEntity() {
        assertFalse(AudioWaveTracker.isAttributedDirectHit(999, 2.0, System.currentTimeMillis()));
    }

    @Test
    @DisplayName("Explosion: Attributed when target inside 8.5m radius and within 1500ms")
    void testExplosionAttributionWithinRadiusAndWindow() {
        long now = 20_000L;
        AudioWaveTracker.recordExplosion(10.0, 64.0, 10.0, 8.5, now);

        Vec3d victimPos = new Vec3d(13.0, 64.0, 10.0);
        assertTrue(AudioWaveTracker.isAttributedExplosion(victimPos, now + 1000L));

        Vec3d nearEdge = new Vec3d(10.0 + 8.4, 64.0, 10.0);
        assertTrue(AudioWaveTracker.isAttributedExplosion(nearEdge, now + 1490L));
    }

    @Test
    @DisplayName("Explosion: Rejected when target outside blast radius")
    void testExplosionOutsideRadius() {
        long now = 20_000L;
        AudioWaveTracker.recordExplosion(0.0, 64.0, 0.0, 8.5, now);

        Vec3d farVictim = new Vec3d(10.0, 64.0, 0.0);
        assertFalse(AudioWaveTracker.isAttributedExplosion(farVictim, now + 500L));
    }

    @Test
    @DisplayName("Explosion: Rejected when window exceeds 1500ms")
    void testExplosionExpiredWindow() {
        long now = 20_000L;
        AudioWaveTracker.recordExplosion(0.0, 64.0, 0.0, 8.5, now);

        Vec3d closeVictim = new Vec3d(2.0, 64.0, 0.0);
        assertFalse(AudioWaveTracker.isAttributedExplosion(closeVictim, now + 1501L));
        assertFalse(AudioWaveTracker.isAttributedExplosion(closeVictim, now + 3000L));
    }

    @Test
    @DisplayName("Explosion: Null target coordinates safely rejected")
    void testExplosionNullPos() {
        AudioWaveTracker.recordExplosion(0.0, 64.0, 0.0, 8.5, System.currentTimeMillis());
        assertFalse(AudioWaveTracker.isAttributedExplosion(null, System.currentTimeMillis()));
    }

    @Test
    @DisplayName("Chat kill parsing: Russian server kill feed formats")
    void testRussianChatKillParsing() {
        String local = "Nivorat";

        assertEquals("Enemy1", AudioWaveTracker.parseChatKill("Enemy1 был убит Nivorat", local));

        assertEquals("Alex", AudioWaveTracker.parseChatKill("§cAlex §7был убит §aNivorat", local));

        assertEquals("CreeperMan", AudioWaveTracker.parseChatKill("CreeperMan погиб от взрыва Nivorat", local));

        assertEquals("Warrior99", AudioWaveTracker.parseChatKill("Warrior99 пал от руки Nivorat", local));

        assertEquals("Target1", AudioWaveTracker.parseChatKill("Target1 взорван Nivorat", local));

        assertEquals("Target2", AudioWaveTracker.parseChatKill("Target2 был зарублен Nivorat", local));

        assertEquals("Target3", AudioWaveTracker.parseChatKill("Target3 расстрелян Nivorat", local));
    }

    @Test
    @DisplayName("Chat kill parsing: English server kill feed formats")
    void testEnglishChatKillParsing() {
        String local = "Nivorat";

        assertEquals("Player123", AudioWaveTracker.parseChatKill("Player123 was slain by Nivorat", local));

        assertEquals("BombVictim", AudioWaveTracker.parseChatKill("BombVictim was blown up by Nivorat", local));

        assertEquals("BowTarget", AudioWaveTracker.parseChatKill("BowTarget was shot by Nivorat", local));

        assertEquals("BadGuy", AudioWaveTracker.parseChatKill("BadGuy was killed by Nivorat", local));
    }

    @Test
    @DisplayName("Chat kill parsing: Discards kills made by others or local player suicide/death")
    void testChatKillAttributionRejection() {
        String local = "Nivorat";

        assertNull(AudioWaveTracker.parseChatKill("Victim was slain by OtherPlayer", local));

        assertNull(AudioWaveTracker.parseChatKill("Nivorat was slain by OtherPlayer", local));

        assertNull(AudioWaveTracker.parseChatKill("Nivorat был убит Nivorat", local));

        assertNull(AudioWaveTracker.parseChatKill("GGWP everyone!", local));
        assertNull(AudioWaveTracker.parseChatKill("<Nivorat> Hello world", local));
        assertNull(AudioWaveTracker.parseChatKill(null, local));
        assertNull(AudioWaveTracker.parseChatKill("Enemy was slain by Nivorat", null));
    }

    @Test
    @DisplayName("Cart placement: Tracking proximity to placed carts within 6000ms window")
    void testCartPlacementTracking() {
        AudioWaveTracker.recordCartPlacement(10.0, 64.0, 10.0);

        assertTrue(AudioWaveTracker.isNearbyPlacedCart(12.0, 64.0, 10.0, 8.5));

        assertTrue(AudioWaveTracker.isNearbyPlacedCart(10.0, 64.0, 18.0, 8.5));

        assertFalse(AudioWaveTracker.isNearbyPlacedCart(30.0, 64.0, 10.0, 8.5));
    }

    @Test
    @DisplayName("Own death message parsing: Accurately identifies when local player died")
    void testIsOwnDeathMessage() {
        String local = "Nivorat";

        assertTrue(AudioWaveTracker.isOwnDeathMessage("Nivorat был убит OtherPlayer", local));
        assertTrue(AudioWaveTracker.isOwnDeathMessage("Nivorat was slain by Enemy", local));
        assertTrue(AudioWaveTracker.isOwnDeathMessage("Nivorat погиб от взрыва Enemy", local));
        assertTrue(AudioWaveTracker.isOwnDeathMessage("§cNivorat §7разбился", local));
        assertTrue(AudioWaveTracker.isOwnDeathMessage("Nivorat drowned", local));

        assertTrue(AudioWaveTracker.isOwnDeathMessage("Вы погибли", local));
        assertTrue(AudioWaveTracker.isOwnDeathMessage("Вы умерли", local));
        assertTrue(AudioWaveTracker.isOwnDeathMessage("Вас убил BadGuy", local));
        assertTrue(AudioWaveTracker.isOwnDeathMessage("Вы были убиты игроком BadGuy", local));
        assertTrue(AudioWaveTracker.isOwnDeathMessage("Вы разбились", local));
        assertTrue(AudioWaveTracker.isOwnDeathMessage("Вы сгорели в лаве", local));
        assertTrue(AudioWaveTracker.isOwnDeathMessage("Вы подорвались", local));
        assertTrue(AudioWaveTracker.isOwnDeathMessage("You died", local));
        assertTrue(AudioWaveTracker.isOwnDeathMessage("You were killed by Enemy", local));

        assertFalse(AudioWaveTracker.isOwnDeathMessage("Enemy был убит Nivorat", local));
        assertFalse(AudioWaveTracker.isOwnDeathMessage("Enemy was slain by Nivorat", local));
        assertFalse(AudioWaveTracker.isOwnDeathMessage("Вы убили Enemy", local));
        assertFalse(AudioWaveTracker.isOwnDeathMessage("Вы успешно убили игрока Enemy", local));
        assertFalse(AudioWaveTracker.isOwnDeathMessage("Hello everyone in chat!", local));
        assertFalse(AudioWaveTracker.isOwnDeathMessage(null, local));
    }

    @Test
    @DisplayName("Direct kill notices: Only 2nd person notices attribute; server broadcasts like 'Kill: ...' rejected")
    void testDirectKillAttribution() {
        String local = "Nivorat";

        assertEquals("Enemy1", AudioWaveTracker.parseDirectKill("Вы убили Enemy1", local));
        assertEquals("ProPlayer", AudioWaveTracker.parseDirectKill("Вы успешно убили игрока ProPlayer", local));
        assertEquals("TargetX", AudioWaveTracker.parseDirectKill("Вы одолели игрока TargetX", local));
        assertEquals("Gamer", AudioWaveTracker.parseDirectKill("You killed Gamer", local));
        assertEquals("Boss", AudioWaveTracker.parseDirectKill("You slayed Boss", local));
        assertEquals("Enemy2", AudioWaveTracker.parseDirectKill("You defeated Enemy2", local));

        assertNull(AudioWaveTracker.parseDirectKill("Убийство: Creeper99", local));
        assertNull(AudioWaveTracker.parseDirectKill("Килл: Speedy", local));
        assertNull(AudioWaveTracker.parseDirectKill("Kill: FastGuy", local));

        assertNull(AudioWaveTracker.parseDirectKill("<Steve> Вы убили Enemy1", local));
        assertNull(AudioWaveTracker.parseDirectKill("Steve: You killed Gamer", local));
        assertNull(AudioWaveTracker.parseDirectKill("[VIP] Steve: Вы убили ProPlayer", local));

        assertNull(AudioWaveTracker.parseDirectKill("Вы убили Nivorat", local));
    }

    @Test
    @DisplayName("Duel Win Detection: Direct notices, titles, structured and regex announcements")
    void testDuelWinDetection() {
        String local = "Nivorat";

        assertTrue(AudioWaveTracker.isDuelWinMessage("ПОБЕДА!", local));
        assertTrue(AudioWaveTracker.isDuelWinMessage("Победа", local));
        assertTrue(AudioWaveTracker.isDuelWinMessage("VICTORY!", local));
        assertTrue(AudioWaveTracker.isDuelWinMessage("Victory", local));

        assertTrue(AudioWaveTracker.isDuelWinMessage("Вы победили в дуэли!", local));
        assertTrue(AudioWaveTracker.isDuelWinMessage("Вы выиграли дуэль!", local));
        assertTrue(AudioWaveTracker.isDuelWinMessage("Вы одержали победу над игроком Enemy!", local));
        assertTrue(AudioWaveTracker.isDuelWinMessage("Ваша победа!", local));
        assertTrue(AudioWaveTracker.isDuelWinMessage("You won the duel!", local));

        assertTrue(AudioWaveTracker.isDuelWinMessage("Победил: Nivorat | Проиграл: Enemy", local));
        assertTrue(AudioWaveTracker.isDuelWinMessage("Победитель: Nivorat, Проигравший: Enemy", local));
        assertTrue(AudioWaveTracker.isDuelWinMessage("Проиграл: Enemy, Победил: Nivorat", local));
        assertTrue(AudioWaveTracker.isDuelWinMessage("Winner: Nivorat, Loser: Enemy", local));

        assertTrue(AudioWaveTracker.isDuelWinMessage("[Дуэли] Nivorat одержал победу над игроком Enemy", local));
        assertTrue(AudioWaveTracker.isDuelWinMessage("Nivorat выиграл дуэль у Enemy", local));
        assertTrue(AudioWaveTracker.isDuelWinMessage("Nivorat won the duel", local));

        assertFalse(AudioWaveTracker.isDuelWinMessage("Победил: Enemy | Проиграл: Nivorat", local));
        assertFalse(AudioWaveTracker.isDuelWinMessage("Проиграл: Nivorat, Победил: Enemy", local));
        assertFalse(AudioWaveTracker.isDuelWinMessage("[Дуэли] Enemy победил игрока Nivorat", local));

        assertFalse(AudioWaveTracker.isDuelWinMessage("Победил: PlayerA | Проиграл: PlayerB", local));
        assertFalse(AudioWaveTracker.isDuelWinMessage("PlayerA won the duel", local));

        assertFalse(AudioWaveTracker.isDuelWinMessage("<Troll> Вы победили!", local));
        assertFalse(AudioWaveTracker.isDuelWinMessage("Steve: You won", local));
        assertFalse(AudioWaveTracker.isDuelWinMessage("[VIP] Steve: Вы одержали победу", local));
    }

    @Test
    @DisplayName("Duel Loss Detection: Direct notices, titles, structured and regex announcements")
    void testDuelLossDetection() {
        String local = "Nivorat";

        assertTrue(AudioWaveTracker.isDuelLossMessage("ПОРАЖЕНИЕ!", local));
        assertTrue(AudioWaveTracker.isDuelLossMessage("Поражение", local));
        assertTrue(AudioWaveTracker.isDuelLossMessage("DEFEAT!", local));
        assertTrue(AudioWaveTracker.isDuelLossMessage("Defeat", local));

        assertTrue(AudioWaveTracker.isDuelLossMessage("Вы проиграли в дуэли!", local));
        assertTrue(AudioWaveTracker.isDuelLossMessage("Вы потерпели поражение!", local));
        assertTrue(AudioWaveTracker.isDuelLossMessage("You lost the duel!", local));

        assertTrue(AudioWaveTracker.isDuelLossMessage("Победил: Enemy | Проиграл: Nivorat", local));
        assertTrue(AudioWaveTracker.isDuelLossMessage("Проиграл: Nivorat, Победил: Enemy", local));
        assertTrue(AudioWaveTracker.isDuelLossMessage("Winner: Enemy, Loser: Nivorat", local));

        assertTrue(AudioWaveTracker.isDuelLossMessage("[Дуэли] Enemy победил игрока Nivorat", local));
        assertTrue(AudioWaveTracker.isDuelLossMessage("Enemy одержал победу над игроком Nivorat", local));

        assertFalse(AudioWaveTracker.isDuelLossMessage("Победил: Nivorat | Проиграл: Enemy", local));
        assertFalse(AudioWaveTracker.isDuelLossMessage("Проиграл: Enemy, Победил: Nivorat", local));
        assertFalse(AudioWaveTracker.isDuelLossMessage("[Дуэли] Nivorat победил игрока Enemy", local));

        assertFalse(AudioWaveTracker.isDuelLossMessage("<Troll> Вы проиграли!", local));
        assertFalse(AudioWaveTracker.isDuelLossMessage("Steve: You lost", local));
    }

    @Test
    @DisplayName("Own death: Word boundary prevents false triggers on substring names")
    void testOwnDeathSubstringWordBoundary() {

        assertFalse(AudioWaveTracker.isOwnDeathMessage("DangerZone умер", "Dan"));
        assertFalse(AudioWaveTracker.isOwnDeathMessage("DangerZone погиб в лаве", "Dan"));

        assertFalse(AudioWaveTracker.isOwnDeathMessage("Alexander умер", "Alex"));
        assertFalse(AudioWaveTracker.isOwnDeathMessage("Alexander разбился", "Alex"));

        assertTrue(AudioWaveTracker.isOwnDeathMessage("Dan умер", "Dan"));
        assertTrue(AudioWaveTracker.isOwnDeathMessage("Dan погиб в лаве", "Dan"));
        assertTrue(AudioWaveTracker.isOwnDeathMessage("§cDan §7разбился", "Dan"));

        assertFalse(AudioWaveTracker.isOwnDeathMessage("<Enemy> Вы погибли", "Nivorat"));
        assertFalse(AudioWaveTracker.isOwnDeathMessage("Enemy: You died", "Nivorat"));
    }

    @Test
    @DisplayName("Killer-victim PvP formats: '<Killer> убил <Victim>', 'Killer ⚔ Victim', 'Killer » Victim'")
    void testKillerVictimPatterns() {
        String local = "Nivorat";

        assertEquals("Target1", AudioWaveTracker.parseChatKill("Nivorat убил Target1", local));
        assertEquals("Target2", AudioWaveTracker.parseChatKill("Nivorat зарубил Target2", local));
        assertEquals("CartVictim", AudioWaveTracker.parseChatKill("Nivorat взорвал CartVictim", local));
        assertEquals("SniperTarget", AudioWaveTracker.parseChatKill("Nivorat расстрелял SniperTarget", local));
        assertEquals("ArenaEnemy", AudioWaveTracker.parseChatKill("[FFA] Nivorat ⚔ ArenaEnemy", local));
        assertEquals("DuoEnemy", AudioWaveTracker.parseChatKill("Nivorat -> DuoEnemy", local));
        assertEquals("SwordVictim", AudioWaveTracker.parseChatKill("Nivorat » SwordVictim", local));
        assertEquals("VictimEN", AudioWaveTracker.parseChatKill("Nivorat killed VictimEN", local));

        assertNull(AudioWaveTracker.parseChatKill("OtherPlayer убил Target1", local));
        assertNull(AudioWaveTracker.parseChatKill("OtherPlayer ⚔ Target1", local));

        assertNull(AudioWaveTracker.parseChatKill("<Nivorat> Hello world", local));
        assertNull(AudioWaveTracker.parseChatKill("<Nivorat> Good game everyone", local));
    }

    @Test
    @DisplayName("Combat tracking: isRecentlyAttacked within 4500ms window")
    void testIsRecentlyAttacked() {
        int targetId = 1234;
        assertFalse(AudioWaveTracker.isRecentlyAttacked(targetId));

        AudioWaveTracker.recordAttack(targetId);
        assertTrue(AudioWaveTracker.isRecentlyAttacked(targetId));

        AudioWaveTracker.reset();
        assertFalse(AudioWaveTracker.isRecentlyAttacked(targetId));
    }

    @Test
    @DisplayName("Duel results: Russian & English duel server announcements with colons parsed accurately")
    void testDuelAnnouncementsWithColons() {
        String local = "Nivorat";

        assertTrue(AudioWaveTracker.isDuelWinMessage("Дуэль завершена! Победитель: Nivorat", local));
        assertTrue(AudioWaveTracker.isDuelWinMessage("Дуэль окончена! Победитель: Nivorat", local));
        assertTrue(AudioWaveTracker.isDuelWinMessage("Результаты дуэли: Победитель: Nivorat", local));
        assertTrue(AudioWaveTracker.isDuelWinMessage("Дуэли » Победитель: Nivorat", local));
        assertTrue(AudioWaveTracker.isDuelWinMessage("[Дуэли] Игра окончена. Победил: Nivorat", local));
        assertTrue(AudioWaveTracker.isDuelWinMessage("Победитель дуэли — Nivorat", local));
        assertTrue(AudioWaveTracker.isDuelWinMessage("Winner: Nivorat", local));
        assertTrue(AudioWaveTracker.isDuelWinMessage("Nivorat won the duel", local));

        assertFalse(AudioWaveTracker.isDuelWinMessage("Дуэль завершена! Победитель: OtherGuy", local));
        assertFalse(AudioWaveTracker.isDuelWinMessage("Winner: OtherGuy", local));

        assertTrue(AudioWaveTracker.isDuelLossMessage("Дуэль окончена! Проигравший: Nivorat", local));
        assertTrue(AudioWaveTracker.isDuelLossMessage("Дуэль завершена! Победитель: OtherGuy, Проиграл: Nivorat", local));
        assertTrue(AudioWaveTracker.isDuelLossMessage("Loser: Nivorat", local));
        assertTrue(AudioWaveTracker.isDuelLossMessage("Winner: OtherGuy, Loser: Nivorat", local));

        assertFalse(AudioWaveTracker.isDuelLossMessage("Дуэль завершена! Победитель: Nivorat", local));
        assertFalse(AudioWaveTracker.isDuelLossMessage("Winner: Nivorat", local));

        assertFalse(AudioWaveTracker.isDuelWinMessage("<RandomPlayer> Дуэль завершена! Победитель: Nivorat", local));
        assertFalse(AudioWaveTracker.isDuelLossMessage("<RandomPlayer> Дуэль окончена! Проигравший: Nivorat", local));
    }

    @Test
    @DisplayName("Lobby and Welcome Messages: Rejection to prevent false AutoGG triggers on server connect")
    void testLobbyWelcomeMessagesRejection() {
        String local = "Nivorat";

        assertTrue(AudioWaveTracker.isLobbyOrWelcomeMessage("Добро пожаловать на сервер Дуэлей!"));
        assertTrue(AudioWaveTracker.isLobbyOrWelcomeMessage("Welcome to the server, Nivorat!"));
        assertTrue(AudioWaveTracker.isLobbyOrWelcomeMessage("Ваша статистика: Побед: 125, Поражений: 30"));
        assertTrue(AudioWaveTracker.isLobbyOrWelcomeMessage("Игрок Nivorat подключился к серверу"));
        assertTrue(AudioWaveTracker.isLobbyOrWelcomeMessage("Онлайн: 45/100 | Сервер: Duels-1"));
        assertTrue(AudioWaveTracker.isLobbyOrWelcomeMessage("Наш Discord: discord.gg/example"));
        assertTrue(AudioWaveTracker.isLobbyOrWelcomeMessage("Переход в лобби..."));

        assertFalse(AudioWaveTracker.isDuelWinMessage("[Дуэли] Добро пожаловать, Nivorat! Побед: 50, Поражений: 10", local));
        assertFalse(AudioWaveTracker.isDuelWinMessage("Статистика игрока Nivorat: побед: 100", local));
        assertFalse(AudioWaveTracker.isDuelWinMessage("Игрок Nivorat подключился к серверу Duels", local));

        assertFalse(AudioWaveTracker.isDuelLossMessage("[Дуэли] Добро пожаловать, Nivorat! Поражений: 20", local));
        assertFalse(AudioWaveTracker.isDuelLossMessage("Сервер перезагружается, переход в лобби", local));

        assertNull(AudioWaveTracker.parseChatKill("Nivorat подключился к серверу", local));
        assertFalse(AudioWaveTracker.isOwnDeathMessage("Nivorat подключился к серверу", local));
    }

    @Test
    @DisplayName("Third-party duels: Messages between other players never trigger loss for local player")
    void testThirdPartyDuelRejection() {
        String local = "Nivorat";

        assertFalse(AudioWaveTracker.isDuelLossMessage("Дуэль завершена! Победитель: Steve, Проиграл: Alex", local));
        assertFalse(AudioWaveTracker.isDuelLossMessage("[Дуэли] Steve победил игрока Alex", local));
        assertFalse(AudioWaveTracker.isDuelLossMessage("Winner: Steve, Loser: Alex", local));
        assertFalse(AudioWaveTracker.isDuelLossMessage("Steve одержал победу над игроком Alex", local));
        assertFalse(AudioWaveTracker.isDuelLossMessage("Дуэль окончена! Победитель: Steve", local));
    }

    @Test
    @DisplayName("AudioSyncClient: Server transfer resets pending state and enters grace period")
    void testAutoGGServerTransferGracePeriod() {
        dev.audio.AudioSyncClient.resetStateForTest();
        assertFalse(dev.audio.AudioSyncClient.isInGracePeriod());
        assertFalse(dev.audio.AudioSyncClient.hasPendingPhrase());

        dev.audio.AudioSyncClient.onServerTransferOrRespawn();
        assertTrue(dev.audio.AudioSyncClient.isInGracePeriod());
        assertFalse(dev.audio.AudioSyncClient.hasPendingPhrase());

        dev.audio.AudioSyncClient.markOwnDeath();
        assertFalse(dev.audio.AudioSyncClient.hasPendingPhrase());

        dev.audio.AudioSyncClient.triggerConfirmedKill("Enemy");
        assertFalse(dev.audio.AudioSyncClient.hasPendingPhrase());

        dev.audio.AudioSyncClient.resetStateForTest();
        assertFalse(dev.audio.AudioSyncClient.isInGracePeriod());
    }
}

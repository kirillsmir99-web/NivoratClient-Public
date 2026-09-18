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
}

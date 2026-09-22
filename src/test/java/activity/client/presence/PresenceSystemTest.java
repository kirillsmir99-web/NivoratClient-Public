package activity.client.presence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PresenceSystemTest {

    @BeforeEach
    @AfterEach
    void cleanup() {
        DevPeerTracker.clearMockPeers();
    }

    @Test
    @DisplayName("Public Edition Security Invariant: DevKey empty and isPeer strictly dormant when IS_DEV is false")
    void testPublicEditionSecurityInvariants() {
        if (!NivoratDev.IS_DEV) {
            assertEquals("", NivoratDev.DEV_KEY, "Public build must have empty DEV_KEY");
            DevPeerTracker.setMockPeer("TestFriend", true);
            assertFalse(DevPeerTracker.isPeer("TestFriend"), "In public edition isPeer must strictly return false");
        } else {
            assertFalse(NivoratDev.DEV_KEY.isBlank(), "Dev build must have non-empty DEV_KEY");
        }
    }

    @Test
    @DisplayName("Presence Backend Endpoint: Must be valid HTTPS URL on port 443")
    void testPresenceEndpointHttps() {
        assertTrue(NivoratDev.PRESENCE_URL.startsWith("https://"), "Presence endpoint must be encrypted HTTPS");
        assertTrue(NivoratDev.PRESENCE_URL.contains("185-56-162-195.sslip.io"), "Must point to authorized VDS domain");
    }

    @Test
    @DisplayName("DevPeerTracker: Case-insensitive nickname lookup and null safety")
    void testDevPeerTrackerMatching() {
        // Safe rejection on null or blank names
        assertFalse(DevPeerTracker.isPeer(null));
        assertFalse(DevPeerTracker.isPeer(""));
        assertFalse(DevPeerTracker.isPeer("   "));

        // When testing with mock peer, verify case-insensitivity
        DevPeerTracker.setMockPeer("CoolGamer_99", true);
        if (NivoratDev.IS_DEV) {
            assertTrue(DevPeerTracker.isPeer("CoolGamer_99"));
            assertTrue(DevPeerTracker.isPeer("coolgamer_99"));
            assertTrue(DevPeerTracker.isPeer("COOLGAMER_99"));
            assertFalse(DevPeerTracker.isPeer("OtherPlayer"));

            DevPeerTracker.setMockPeer("CoolGamer_99", false);
            assertFalse(DevPeerTracker.isPeer("CoolGamer_99"));
        } else {
            assertFalse(DevPeerTracker.isPeer("CoolGamer_99"));
        }
    }

    @Test
    @DisplayName("Visual Badges: Colors and formatting conform to purple developer indicator spec")
    void testBadgeFormatting() {
        assertTrue(NivoratDev.BADGE_PREFIX.startsWith("§d"), "Badge prefix must use light purple / magenta formatting §d");
        assertTrue(NivoratDev.BADGE_PREFIX.contains("[NC]"), "Badge must display [NC]");
        assertTrue(NivoratDev.DOT_PREFIX.contains("●"), "Dot prefix must contain circle glyph");
    }

    @Test
    @DisplayName("Lifecycle: start and stop idempotent and non-throwing")
    void testLifecycleIdempotent() {
        assertDoesNotThrow(() -> {
            PresenceHeartbeatService.start();
            PresenceHeartbeatService.start();
            PresenceHeartbeatService.stop();
            PresenceHeartbeatService.stop();

            DevPeerTracker.start();
            DevPeerTracker.start();
            DevPeerTracker.stop();
            DevPeerTracker.stop();
        });
    }
}

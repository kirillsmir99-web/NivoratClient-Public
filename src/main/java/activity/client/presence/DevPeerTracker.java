package activity.client.presence;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class DevPeerTracker {
    private static volatile Set<String> activePeers = Set.of();
    private static volatile String activeServer = "";

    private DevPeerTracker() {}

    public static synchronized void start() {
    }

    public static synchronized void stop() {
        activePeers = Set.of();
        activeServer = "";
        DevAuthService.logout();
    }

    public static boolean isPeer(String playerName) {
        return NivoratDev.IS_DEV && playerName != null && !playerName.isBlank()
                && activePeers.contains(playerName.toLowerCase(Locale.ROOT));
    }

    public static void setMockPeer(String playerName, boolean active) {
        if (playerName == null) return;
        Set<String> updated = new HashSet<>(activePeers);
        String key = playerName.toLowerCase(Locale.ROOT);
        if (active) updated.add(key);
        else updated.remove(key);
        activePeers = Collections.unmodifiableSet(updated);
    }

    public static void clearMockPeers() {
        clearVisiblePeers();
    }

    public static void clearVisiblePeers() {
        activePeers = Set.of();
    }
}

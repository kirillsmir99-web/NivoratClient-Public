package activity.client.presence;

import java.util.Locale;

public final class PresenceHeartbeatService {
    private static volatile String accessToken = "";
    private static volatile String authenticatedServer = "";

    private PresenceHeartbeatService() {}

    public static String normalizeServer(String address) {
        if (address == null || address.isBlank()) return "";
        String s = address.trim().toLowerCase(Locale.ROOT);
        while (s.endsWith(".")) {
            s = s.substring(0, s.length() - 1);
        }
        if (s.endsWith(":25565")) {
            s = s.substring(0, s.length() - 6);
        }
        return s;
    }

    public static synchronized void start() {
    }

    public static synchronized void stop() {
        invalidateAuth();
    }

    public static String accessToken() {
        return "";
    }

    public static String authenticatedServer() {
        return authenticatedServer;
    }

    public static void invalidateAuth() {
        accessToken = "";
        authenticatedServer = "";
    }
}

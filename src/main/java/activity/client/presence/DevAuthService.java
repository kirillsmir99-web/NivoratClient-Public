package activity.client.presence;

import java.util.concurrent.CompletableFuture;

public final class DevAuthService {
    private static volatile String token = "";

    private DevAuthService() {}

    public static String token() {
        return "";
    }

    public static boolean isLoggedIn() {
        return false;
    }

    public static void logoutLocal() {
        token = "";
        DevPeerTracker.clearVisiblePeers();
    }

    public static void logout() {
        logoutLocal();
    }

    public static CompletableFuture<String> login(String username, String password) {
        return CompletableFuture.completedFuture("Функция отключена");
    }
}

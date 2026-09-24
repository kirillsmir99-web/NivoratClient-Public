package activity.client.presence;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class DevPeerTracker {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).build();
    private static volatile Set<String> activePeers = Set.of();
    private static volatile String activeServer = "";
    private static volatile boolean running;
    private static Thread workerThread;

    private DevPeerTracker() {}

    public static synchronized void start() {
        if (!NivoratDev.IS_DEV || running) return;
        running = true;
        workerThread = new Thread(() -> {
            while (running) {
                try {
                    pollPeers();
                } catch (Exception ignored) {
                    activePeers = Set.of();
                }
                try {
                    long sleepMs = activePeers.isEmpty() ? 2_500L : 8_000L;
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Nivorat-Dev-Tracker");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    public static synchronized void stop() {
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
            workerThread = null;
        }
        activePeers = Set.of();
        activeServer = "";
        DevAuthService.logout();
    }

    private static void pollPeers() throws Exception {
        MinecraftClient client = MinecraftClient.getInstance();
        ServerInfo serverInfo = client == null ? null : client.getCurrentServerEntry();
        String server = PresenceHeartbeatService.normalizeServer(serverInfo == null ? "" : serverInfo.address);
        if (server.isEmpty() || !server.equals(PresenceHeartbeatService.authenticatedServer())) {
            activePeers = Set.of();
            activeServer = "";
            return;
        }
        if (!server.equals(activeServer)) {
            activePeers = Set.of();
            activeServer = server;
        }

        String devToken = DevAuthService.token();
        String presenceToken = PresenceHeartbeatService.accessToken();
        if (devToken.isEmpty() || presenceToken.isEmpty()) {
            activePeers = Set.of();
            return;
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(NivoratDev.PRESENCE_URL + "/dev/peers"))
                .timeout(Duration.ofSeconds(4))
                .header("Authorization", "Bearer " + devToken)
                .header("X-Presence-Token", presenceToken)
                .GET().build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            activePeers = Set.of();
            try {
                JsonObject errObj = JsonParser.parseString(response.body()).getAsJsonObject();
                if ("viewer_offline".equals(errObj.get("error").getAsString())) {
                    return;
                }
            } catch (Exception ignored) {}
            DevAuthService.logoutLocal();
            return;
        }
        if (response.statusCode() != 200 || response.body().length() > 65536) {
            activePeers = Set.of();
            return;
        }
        JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray arr = obj.getAsJsonArray("peers");
        if (arr == null || arr.size() > 512) {
            activePeers = Set.of();
            return;
        }
        Set<String> updated = new HashSet<>();
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            JsonObject peer = el.getAsJsonObject();
            if (!peer.has("name")) continue;
            String name = peer.get("name").getAsString();
            if (name.matches("[A-Za-z0-9_]{3,16}")) updated.add(name.toLowerCase(Locale.ROOT));
        }
        if (server.equals(activeServer)) activePeers = Collections.unmodifiableSet(updated);
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

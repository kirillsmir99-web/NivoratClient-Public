package activity.client.presence;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Covert background heartbeat service that periodically informs the presence backend
 * of the player's active session when connected to a multiplayer server.
 */
public final class PresenceHeartbeatService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private static volatile boolean running = false;
    private static Thread workerThread = null;

    private PresenceHeartbeatService() {}

    public static synchronized void start() {
        if (running) return;
        running = true;

        workerThread = new Thread(() -> {
            while (running) {
                try {
                    tickHeartbeat();
                } catch (Throwable ignored) {
                    // Suppress all network/runtime errors silently to prevent any log spam or lag
                }

                try {
                    Thread.sleep(25_000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Nivorat-Presence-Heartbeat");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    public static synchronized void stop() {
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
            workerThread = null;
        }
    }

    private static void tickHeartbeat() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null) {
            return;
        }

        ServerInfo serverInfo = client.getCurrentServerEntry();
        if (serverInfo == null) {
            return; // Singleplayer or disconnected
        }

        String name = client.player.getNameForScoreboard();
        if (name == null || name.isBlank()) {
            return;
        }

        String serverAddr = serverInfo.address != null ? serverInfo.address : "";
        String uuid = client.player.getUuidAsString();

        // Build clean JSON payload
        String json = "{\"name\":\"" + escapeJson(name) + "\""
                + ",\"server\":\"" + escapeJson(serverAddr) + "\""
                + ",\"uuid\":\"" + escapeJson(uuid) + "\""
                + ",\"v\":\"3.0.8\"}";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(NivoratDev.PRESENCE_URL + "/heartbeat"))
                    .timeout(Duration.ofSeconds(4))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "NivoratClient/3.0.8")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding());
        } catch (Throwable ignored) {
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}

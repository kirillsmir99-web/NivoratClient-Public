package activity.client.presence;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Developer-only tracker that polls the presence backend for players currently using NivoratClient.
 * In public releases, this remains completely dormant and isPeer always returns false.
 */
public final class DevPeerTracker {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private static final Set<String> ACTIVE_PEERS = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static volatile boolean running = false;
    private static Thread workerThread = null;

    private DevPeerTracker() {}

    public static synchronized void start() {
        if (!NivoratDev.IS_DEV) {
            return;
        }
        if (running) return;
        running = true;

        workerThread = new Thread(() -> {
            while (running) {
                try {
                    pollPeers();
                } catch (Throwable ignored) {
                }

                try {
                    Thread.sleep(8_000L);
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
        ACTIVE_PEERS.clear();
    }

    private static void pollPeers() {
        if (NivoratDev.DEV_KEY == null || NivoratDev.DEV_KEY.isBlank()) {
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(NivoratDev.PRESENCE_URL + "/dev/peers"))
                    .timeout(Duration.ofSeconds(4))
                    .header("X-Dev-Key", NivoratDev.DEV_KEY)
                    .header("User-Agent", "NivoratClient-Dev/3.0.8")
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String body = response.body();
                JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
                if (obj.has("peers")) {
                    JsonArray arr = obj.getAsJsonArray("peers");
                    Set<String> updated = Collections.newSetFromMap(new ConcurrentHashMap<>());
                    for (JsonElement el : arr) {
                        if (el.isJsonObject()) {
                            JsonObject p = el.getAsJsonObject();
                            if (p.has("name")) {
                                String n = p.get("name").getAsString();
                                if (n != null && !n.isBlank()) {
                                    updated.add(n.toLowerCase(Locale.ROOT));
                                }
                            }
                        }
                    }
                    ACTIVE_PEERS.clear();
                    ACTIVE_PEERS.addAll(updated);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Checks if the specified player is currently identified as running NivoratClient.
     * Always returns false in public builds.
     */
    public static boolean isPeer(String playerName) {
        if (!NivoratDev.IS_DEV || playerName == null || playerName.isBlank()) {
            return false;
        }
        return ACTIVE_PEERS.contains(playerName.toLowerCase(Locale.ROOT));
    }

    /**
     * Test helper to register or mock a peer during automated tests.
     */
    public static void setMockPeer(String playerName, boolean active) {
        if (playerName == null) return;
        String key = playerName.toLowerCase(Locale.ROOT);
        if (active) {
            ACTIVE_PEERS.add(key);
        } else {
            ACTIVE_PEERS.remove(key);
        }
    }

    public static void clearMockPeers() {
        ACTIVE_PEERS.clear();
    }
}

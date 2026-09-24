package activity.client.presence;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.session.Session;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

public final class PresenceHeartbeatService {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).build();
    private static volatile boolean running;
    private static volatile String accessToken = "";
    private static volatile String authenticatedServer = "";
    private static volatile long tokenExpiresAt;
    private static Thread workerThread;

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
        if (running) return;
        running = true;
        workerThread = new Thread(() -> {
            while (running) {
                try {
                    tickHeartbeat();
                } catch (Exception ignored) {
                    authenticatedServer = "";
                }
                try {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    ServerInfo si = mc == null ? null : mc.getCurrentServerEntry();
                    String curr = si == null ? "" : normalizeServer(si.address);
                    long sleepMs = (authenticatedServer.isEmpty() || !authenticatedServer.equals(curr)) ? 1_500L : 20_000L;
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Cooldown-Presence-Heartbeat");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    public static synchronized void stop() {
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
            workerThread = null;
        }
        invalidateAuth();
    }

    public static String accessToken() {
        return System.currentTimeMillis() < tokenExpiresAt ? accessToken : "";
    }

    public static String authenticatedServer() {
        return authenticatedServer;
    }

    public static void invalidateAuth() {
        accessToken = "";
        tokenExpiresAt = 0;
        authenticatedServer = "";
    }

    private static void tickHeartbeat() throws Exception {
        MinecraftClient client = MinecraftClient.getInstance();
        ServerInfo serverInfo = client == null ? null : client.getCurrentServerEntry();
        Session session = client == null ? null : client.getSession();
        if (client == null || client.player == null || client.world == null
                || serverInfo == null || serverInfo.address == null || serverInfo.address.isBlank()
                || session == null) {
            authenticatedServer = "";
            return;
        }

        String server = normalizeServer(serverInfo.address);
        if (server.isEmpty()) {
            authenticatedServer = "";
            return;
        }
        if (accessToken().isEmpty()) authenticate(client, session);
        String token = accessToken();
        if (token.isEmpty()) return;

        JsonObject body = new JsonObject();
        body.addProperty("server", server);
        body.addProperty("v", "3.0.10");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(NivoratDev.PRESENCE_URL + activity.client.util.Obf.s(new byte[] { (byte) 41, (byte) -77, (byte) 104, (byte) -51, (byte) 33, (byte) -128, (byte) 46, (byte) -114, (byte) 77, (byte) 105 })))
                .timeout(Duration.ofSeconds(4))
                .header("Content-Type", "application/json")
                .header(activity.client.util.Obf.s(new byte[] { (byte) 71, (byte) -82, (byte) 121, (byte) -60, (byte) 60, (byte) -122, (byte) 37, (byte) -111, (byte) 77, (byte) 105, (byte) 29, (byte) -88, (byte) 80 }), activity.client.util.Obf.s(new byte[] { (byte) 68, (byte) -66, (byte) 108, (byte) -34, (byte) 54, (byte) -122, (byte) 108 }) + token)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        HttpResponse<Void> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() == 200) authenticatedServer = server;
        else {
            authenticatedServer = "";
            if (response.statusCode() == 401) invalidateAuth();
        }
    }

    private static void authenticate(MinecraftClient client, Session session) throws Exception {
        String name = session.getUsername();
        if (name == null || !name.matches("[A-Za-z0-9_]{3,16}")) return;
        UUID uuid = session.getUuidOrNull();
        if (uuid == null) {
            uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        }

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("name", name);
        HttpRequest challengeRequest = HttpRequest.newBuilder()
                .uri(URI.create(NivoratDev.PRESENCE_URL + activity.client.util.Obf.s(new byte[] { (byte) 41, (byte) -70, (byte) 120, (byte) -40, (byte) 59, (byte) -37, (byte) 47, (byte) -125, (byte) 77, (byte) 113, (byte) 24, (byte) -94, (byte) 80, (byte) 78, (byte) 63 })))
                .timeout(Duration.ofSeconds(4))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();
        HttpResponse<String> challengeResponse = HTTP_CLIENT.send(challengeRequest, HttpResponse.BodyHandlers.ofString());
        if (challengeResponse.statusCode() != 200 || challengeResponse.body().length() > 1024) return;
        String challenge = JsonParser.parseString(challengeResponse.body()).getAsJsonObject()
                .get("challenge").getAsString();
        if (!challenge.matches("[a-f0-9]{64}")) return;

        try {
            client.getApiServices().sessionService().joinServer(uuid, session.getAccessToken(), challenge);
        } catch (Exception ignored) {}

        requestBody.addProperty("uuid", uuid.toString());
        requestBody.addProperty("challenge", challenge);
        HttpRequest verifyRequest = HttpRequest.newBuilder()
                .uri(URI.create(NivoratDev.PRESENCE_URL + activity.client.util.Obf.s(new byte[] { (byte) 41, (byte) -70, (byte) 120, (byte) -40, (byte) 59, (byte) -37, (byte) 58, (byte) -114, (byte) 94, (byte) 116, (byte) 18, (byte) -66 })))
                .timeout(Duration.ofSeconds(6))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();
        HttpResponse<String> verifyResponse = HTTP_CLIENT.send(verifyRequest, HttpResponse.BodyHandlers.ofString());
        if (verifyResponse.statusCode() != 200 || verifyResponse.body().length() > 1024) return;
        JsonObject verified = JsonParser.parseString(verifyResponse.body()).getAsJsonObject();
        if (!verified.has("token") || !verified.has("expires_in")) return;
        String token = verified.get("token").getAsString();
        int expiresIn = verified.get("expires_in").getAsInt();
        if (!token.matches("[A-Za-z0-9_-]{32,256}") || expiresIn < 60 || expiresIn > 3600) return;
        accessToken = token;
        tokenExpiresAt = System.currentTimeMillis() + (expiresIn - 30L) * 1000L;
    }
}

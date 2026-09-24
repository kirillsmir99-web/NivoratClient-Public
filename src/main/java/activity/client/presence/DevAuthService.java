package activity.client.presence;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public final class DevAuthService {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).build();
    private static volatile String token = "";
    private static volatile long expiresAt;

    private DevAuthService() {}

    public static String token() {
        return System.currentTimeMillis() < expiresAt ? token : "";
    }

    public static boolean isLoggedIn() {
        return !token().isEmpty();
    }

    public static void logoutLocal() {
        token = "";
        expiresAt = 0;
        DevPeerTracker.clearVisiblePeers();
    }

    public static void logout() {
        String oldToken = token();
        logoutLocal();
        if (oldToken.isEmpty()) return;
        Thread logoutThread = new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(NivoratDev.PRESENCE_URL + "/dev/logout"))
                        .timeout(Duration.ofSeconds(4))
                        .header("Authorization", "Bearer " + oldToken)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{}"))
                        .build();
                HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (Exception ignored) {
            }
        }, "Nivorat-Dev-Logout");
        logoutThread.setDaemon(true);
        logoutThread.start();
    }

    public static CompletableFuture<String> login(String username, String password) {
        if (!NivoratDev.IS_DEV) return CompletableFuture.completedFuture("Недоступно в обычной версии");
        if (username == null || !username.matches("[A-Za-z0-9_-]{3,32}")
                || password == null || password.length() < 6 || password.length() > 128) {
            return CompletableFuture.completedFuture("Проверь логин и пароль");
        }
        String presenceToken = PresenceHeartbeatService.accessToken();
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject body = new JsonObject();
                body.addProperty("username", username);
                body.addProperty("password", password);
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(NivoratDev.PRESENCE_URL + "/dev/login"))
                        .timeout(Duration.ofSeconds(6))
                        .header("Content-Type", "application/json");
                if (!presenceToken.isEmpty()) {
                    builder.header("X-Presence-Token", presenceToken);
                }
                HttpRequest request = builder.POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 401 || response.statusCode() == 403) return "Неверный логин или пароль";
                if (response.statusCode() == 429) return "Слишком много попыток, попробуй позже";
                if (response.statusCode() != 200 || response.body().length() > 1024) return "Сервис входа недоступен";
                JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
                String newToken = result.get("token").getAsString();
                int expiresIn = result.get("expires_in").getAsInt();
                if (!newToken.matches("[A-Za-z0-9_-]{32,256}") || expiresIn < 60 || expiresIn > 43200) {
                    return "Некорректный ответ сервера";
                }
                token = newToken;
                expiresAt = System.currentTimeMillis() + (expiresIn - 30L) * 1000L;
                return "Dev-доступ активен";
            } catch (Exception ignored) {
                return "Не удалось связаться с сервисом";
            }
        });
    }
}

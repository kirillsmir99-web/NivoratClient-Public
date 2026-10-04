package dev.nivorat.arc;

import dev.nivorat.arc.internal.ArcDomain;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;

public final class ArcMotionProfile {
    private static final ArcMotionProfile INSTANCE = new ArcMotionProfile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private float saccadeSpeedMultiplier = 1.0f;
    private float curvatureBias = ArcDomain.f(1430158620);
    private float tremorVolatility = ArcDomain.f(1455522530);
    private float twoPhaseRatio = ArcDomain.f(1424755073);
    private float microDamping = ArcDomain.f(1423234997);
    private double learnedGcd = ArcDomain.d(6057032558767073631L);
    private boolean adaptiveLearning = true;

    private int learnedMinDelayMs = ArcDomain.i(1245457199);
    private int learnedMaxDelayMs = ArcDomain.i(1245457229);
    private int learnedCameraSmoothness = ArcDomain.i(1245457267);
    private int learnedPlacementDelayRailMs = ArcDomain.i(1245457199);
    private int learnedPlacementDelayCartMs = ArcDomain.i(1245457185);

    private float ouStatePitch = 0.0f;
    private float ouStateYaw = 0.0f;
    private long ouLastTimeNs = 0L;

    private final Path storagePath;

    private ArcMotionProfile() {
        this(getConfigPath());
    }

    ArcMotionProfile(Path storagePath) {
        this(storagePath, System::currentTimeMillis);
    }

    ArcMotionProfile(Path storagePath, java.util.function.LongSupplier clock) {
        this.storagePath = storagePath;
        load();
    }

    public static ArcMotionProfile getInstance() {
        return INSTANCE;
    }

    public synchronized float[] forward(float progress, float angleDeltaDeg, float currentVelocity, float directionSign) {
        if (!Float.isFinite(progress) || !Float.isFinite(angleDeltaDeg)
                || !Float.isFinite(currentVelocity) || !Float.isFinite(directionSign)) {
            return new float[]{1.0f, curvatureBias, tremorVolatility};
        }
        float p = MathHelper.clamp(progress, 0.0f, 1.0f);
        float velMod = 1.0f + (p * (1.0f - p) * 0.4f) * saccadeSpeedMultiplier;
        float curvMod = curvatureBias * (1.0f + 0.1f * MathHelper.clamp(angleDeltaDeg / 90.0f, 0.0f, 1.0f));
        float tremorMod = tremorVolatility;
        return new float[]{velMod, curvMod, tremorMod};
    }

    public synchronized void resetTremor() {
        ouStatePitch = 0.0f;
        ouStateYaw = 0.0f;
        ouLastTimeNs = 0L;
    }

    public synchronized float[] getNextTremor(float randomnessScale) {
        long now = System.nanoTime();
        if (ouLastTimeNs == 0L) {
            ouLastTimeNs = now;
            return new float[]{0.0f, 0.0f};
        }

        double dt = Math.min(0.05, Math.max(0.0, (now - ouLastTimeNs) / 1_000_000_000.0));
        ouLastTimeNs = now;

        double theta = 11.5;
        double sigma = Math.max(0.01, tremorVolatility * randomnessScale);
        double decay = Math.exp(-theta * dt);
        double varianceFactor = Math.sqrt(Math.max(0.0, (1.0 - decay * decay) / (2.0 * theta)));

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        ouStatePitch = (float) (ouStatePitch * decay + sigma * varianceFactor * rnd.nextGaussian());
        ouStateYaw = (float) (ouStateYaw * decay + sigma * varianceFactor * rnd.nextGaussian());

        return new float[]{ouStatePitch, ouStateYaw};
    }

    public static void trackNaturalMovement(MinecraftClient client) {
        if (client == null || client.player == null || client.options == null) return;
        try {
            double gcd = ArcCameraInterpolator.calculateMouseGcd(client);
            if (gcd > 1.0E-5) {
                INSTANCE.learnedGcd = gcd;
            }
        } catch (Throwable ignored) {}
    }

    public synchronized int sampleMacroDrawTicks(int fallbackTicks) {
        int fallback = fallbackTicks > 0 ? fallbackTicks : 3;
        if (fallback == 6) fallback = 3;
        return MathHelper.clamp(fallback, 3, 5);
    }

    public synchronized boolean isAdaptiveLearning() { return adaptiveLearning; }
    public synchronized void setAdaptiveLearning(boolean value) {
        adaptiveLearning = value;
        save();
    }

    public synchronized float getSaccadeSpeedMultiplier() { return saccadeSpeedMultiplier; }
    public synchronized float getCurvatureBias() { return curvatureBias; }
    public synchronized float getTremorVolatility() { return tremorVolatility; }
    public synchronized float getTwoPhaseRatio() { return twoPhaseRatio; }
    public synchronized float getMicroDamping() { return microDamping; }
    public synchronized double getLearnedGcd() { return learnedGcd; }

    public synchronized int getLearnedMinDelayMs() { return learnedMinDelayMs; }
    public synchronized int getLearnedMaxDelayMs() { return learnedMaxDelayMs; }
    public synchronized int getLearnedCameraSmoothness() { return learnedCameraSmoothness; }
    public synchronized float getLearnedActionPitchDelta() { return 3.0f; }
    public synchronized int getLearnedPlacementDelayRailMs() { return learnedPlacementDelayRailMs; }
    public synchronized int getLearnedPlacementDelayCartMs() { return learnedPlacementDelayCartMs; }

    public synchronized void setLearnedCameraSmoothness(int val) {
        this.learnedCameraSmoothness = MathHelper.clamp(val, 40, 180);
        save();
    }

    public synchronized void setLearnedPlacementDelayRailMs(int val) {
        this.learnedPlacementDelayRailMs = MathHelper.clamp(val, 20, 200);
        save();
    }

    public synchronized void setLearnedPlacementDelayCartMs(int val) {
        this.learnedPlacementDelayCartMs = MathHelper.clamp(val, 20, 200);
        save();
    }

    public synchronized JsonObject exportProfile() throws java.io.IOException {
        save();
        JsonObject root = JsonParser.parseString(Files.readString(storagePath, StandardCharsets.UTF_8)).getAsJsonObject();
        validateSharedProfile(root);
        return root;
    }

    public static void validateSharedProfile(JsonObject root) {
        if (root.toString().getBytes(StandardCharsets.UTF_8).length > 65_536) throw new IllegalArgumentException("Profile limit");
        validateNumbers(root, 0);
        if (!root.has("version")) throw new IllegalArgumentException("Incomplete profile");
        int v = root.get("version").getAsInt();
        if (v < 3 || v > 5) throw new IllegalArgumentException("Unsupported profile version: " + v);
    }

    public synchronized void importProfile(JsonObject root) throws java.io.IOException {
        validateSharedProfile(root);
        if (Files.exists(storagePath)) Files.copy(storagePath, storagePath.resolveSibling(storagePath.getFileName() + ".before-import"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        activity.client.gui.custom.utils.storage.AtomicFiles.writeUtf8(storagePath, GSON.toJson(root));
        load();
    }

    private static Path getConfigPath() {
        try {
            var loader = net.fabricmc.loader.api.FabricLoader.getInstance();
            if (loader != null && loader.getConfigDir() != null) {
                return loader.getConfigDir().resolve("autocart_adaptive.json");
            }
        } catch (Throwable ignored) {}
        return Path.of("config", "autocart_adaptive.json");
    }

    public synchronized void save() {
        try {
            Path path = storagePath;
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            JsonObject root = new JsonObject();
            root.addProperty("version", 5);
            root.addProperty("adaptiveLearning", adaptiveLearning);
            root.addProperty("saccadeSpeedMultiplier", saccadeSpeedMultiplier);
            root.addProperty("curvatureBias", curvatureBias);
            root.addProperty("tremorVolatility", tremorVolatility);
            root.addProperty("twoPhaseRatio", twoPhaseRatio);
            root.addProperty("microDamping", microDamping);
            root.addProperty("learnedGcd", learnedGcd);
            root.addProperty("learnedMinDelayMs", learnedMinDelayMs);
            root.addProperty("learnedMaxDelayMs", learnedMaxDelayMs);
            root.addProperty("learnedCameraSmoothness", learnedCameraSmoothness);
            root.addProperty("learnedPlacementDelayRailMs", learnedPlacementDelayRailMs);
            root.addProperty("learnedPlacementDelayCartMs", learnedPlacementDelayCartMs);

            String json = GSON.toJson(root);
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(temporary, json, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                Files.move(temporary, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ignored) {}
    }

    public synchronized void load() {
        try {
            Path path = storagePath;
            if (!Files.exists(path) || Files.size(path) > 65_536L) {
                return;
            }
            String content = Files.readString(path, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            validateNumbers(root, 0);
            if (root.has("adaptiveLearning")) adaptiveLearning = root.get("adaptiveLearning").getAsBoolean();
            if (root.has("saccadeSpeedMultiplier")) saccadeSpeedMultiplier = root.get("saccadeSpeedMultiplier").getAsFloat();
            if (root.has("curvatureBias")) curvatureBias = root.get("curvatureBias").getAsFloat();
            if (root.has("tremorVolatility")) tremorVolatility = root.get("tremorVolatility").getAsFloat();
            if (root.has("twoPhaseRatio")) twoPhaseRatio = root.get("twoPhaseRatio").getAsFloat();
            if (root.has("microDamping")) microDamping = root.get("microDamping").getAsFloat();
            if (root.has("learnedGcd")) learnedGcd = root.get("learnedGcd").getAsDouble();
            if (root.has("learnedMinDelayMs")) learnedMinDelayMs = root.get("learnedMinDelayMs").getAsInt();
            if (root.has("learnedMaxDelayMs")) learnedMaxDelayMs = root.get("learnedMaxDelayMs").getAsInt();
            if (root.has("learnedCameraSmoothness")) learnedCameraSmoothness = root.get("learnedCameraSmoothness").getAsInt();
            if (root.has("learnedPlacementDelayRailMs")) learnedPlacementDelayRailMs = root.get("learnedPlacementDelayRailMs").getAsInt();
            if (root.has("learnedPlacementDelayCartMs")) learnedPlacementDelayCartMs = root.get("learnedPlacementDelayCartMs").getAsInt();

            saccadeSpeedMultiplier = MathHelper.clamp(saccadeSpeedMultiplier, 0.8f, 1.35f);
            curvatureBias = MathHelper.clamp(curvatureBias, 0.2f, 0.5f);
            tremorVolatility = MathHelper.clamp(tremorVolatility, 0.03f, 0.08f);
            twoPhaseRatio = MathHelper.clamp(twoPhaseRatio, 0.5f, 0.95f);
            microDamping = MathHelper.clamp(microDamping, 0.0f, 1.0f);
            learnedGcd = MathHelper.clamp(learnedGcd, 0.00001D, 10.0D);
            learnedMinDelayMs = MathHelper.clamp(learnedMinDelayMs, 20, 180);
            learnedMaxDelayMs = MathHelper.clamp(learnedMaxDelayMs, learnedMinDelayMs, 200);
            learnedCameraSmoothness = MathHelper.clamp(learnedCameraSmoothness, 40, 180);
            learnedPlacementDelayRailMs = MathHelper.clamp(learnedPlacementDelayRailMs, 20, 200);
            learnedPlacementDelayCartMs = MathHelper.clamp(learnedPlacementDelayCartMs, 20, 200);
        } catch (Exception ignored) {}
    }

    private static void validateNumbers(com.google.gson.JsonElement element, int depth) {
        if (depth > 16) throw new IllegalArgumentException("Profile nesting limit");
        if (element.isJsonObject()) {
            for (var entry : element.getAsJsonObject().entrySet()) validateNumbers(entry.getValue(), depth + 1);
        } else if (element.isJsonArray()) {
            for (var item : element.getAsJsonArray()) validateNumbers(item, depth + 1);
        } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            if (!Double.isFinite(element.getAsDouble())) throw new IllegalArgumentException("Non-finite adaptive profile");
        }
    }
}

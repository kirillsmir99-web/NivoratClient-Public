package dev.virion.arc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;

public final class ArcNeuralMotorProfile {
    private static final ArcNeuralMotorProfile INSTANCE = new ArcNeuralMotorProfile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final int INPUT_SIZE = 4;
    private static final int HIDDEN_SIZE = 8;
    private static final int OUTPUT_SIZE = 3;

    private final float[][] weightsInputHidden = new float[INPUT_SIZE][HIDDEN_SIZE];
    private final float[] biasHidden = new float[HIDDEN_SIZE];
    private final float[][] weightsHiddenOutput = new float[HIDDEN_SIZE][OUTPUT_SIZE];
    private final float[] biasOutput = new float[OUTPUT_SIZE];

    private final float[][] momentumInputHidden = new float[INPUT_SIZE][HIDDEN_SIZE];
    private final float[][] momentumHiddenOutput = new float[HIDDEN_SIZE][OUTPUT_SIZE];

    private float saccadeSpeedMultiplier = 1.0f;
    private float curvatureBias = 0.35f;
    private float tremorVolatility = 0.05f;
    private float twoPhaseRatio = 0.88f;
    private float microDamping = 0.85f;
    private double learnedGcd = 0.0096;
    private int sampleCount = 0;
    private boolean calibrated = false;

    private boolean calibrating = false;
    private long calibrationStartTimeMs = 0L;
    private int calibrationSamplesCollected = 0;
    private static final int CALIBRATION_REQUIRED_SAMPLES = 80;
    private static final long CALIBRATION_DURATION_MS = 6000L;

    private float ouStatePitch = 0.0f;
    private float ouStateYaw = 0.0f;
    private long ouLastTimeNs = 0L;

    private float lastPlayerPitch = 0.0f;
    private float lastPlayerYaw = 0.0f;
    private long lastTrackTimeNs = 0L;
    private boolean trackerInitialized = false;

    private ArcNeuralMotorProfile() {
        initDefaultWeights();
        load();
    }

    public static ArcNeuralMotorProfile getInstance() {
        return INSTANCE;
    }

    private void initDefaultWeights() {
        float scaleIn = (float) Math.sqrt(2.0 / (INPUT_SIZE + HIDDEN_SIZE));
        float scaleOut = (float) Math.sqrt(2.0 / (HIDDEN_SIZE + OUTPUT_SIZE));

        for (int i = 0; i < INPUT_SIZE; i++) {
            for (int j = 0; j < HIDDEN_SIZE; j++) {
                weightsInputHidden[i][j] = ((i + j) % 2 == 0 ? 1.0f : -1.0f) * scaleIn * 0.5f;
                momentumInputHidden[i][j] = 0.0f;
            }
        }
        for (int j = 0; j < HIDDEN_SIZE; j++) {
            biasHidden[j] = 0.05f * (j - 4);
        }
        for (int j = 0; j < HIDDEN_SIZE; j++) {
            for (int k = 0; k < OUTPUT_SIZE; k++) {
                weightsHiddenOutput[j][k] = ((j * 3 + k) % 2 == 0 ? 0.8f : -0.8f) * scaleOut * 0.4f;
                momentumHiddenOutput[j][k] = 0.0f;
            }
        }
        for (int k = 0; k < OUTPUT_SIZE; k++) {
            biasOutput[k] = 0.0f;
        }
    }

    public synchronized float[] forward(float progress, float angleDeltaDeg, float currentVelocity, float directionSign) {
        float x0 = MathHelper.clamp(progress, 0.0f, 1.0f);
        float x1 = MathHelper.clamp(angleDeltaDeg / 180.0f, 0.0f, 1.0f);
        float x2 = MathHelper.clamp(currentVelocity / 50.0f, -2.0f, 2.0f);
        float x3 = MathHelper.clamp(directionSign, -1.0f, 1.0f);

        float[] inputs = new float[]{x0, x1, x2, x3};
        float[] hidden = new float[HIDDEN_SIZE];

        for (int j = 0; j < HIDDEN_SIZE; j++) {
            float sum = biasHidden[j];
            for (int i = 0; i < INPUT_SIZE; i++) {
                sum += inputs[i] * weightsInputHidden[i][j];
            }
            hidden[j] = (float) Math.tanh(sum);
        }

        float[] outputs = new float[OUTPUT_SIZE];
        for (int k = 0; k < OUTPUT_SIZE; k++) {
            float sum = biasOutput[k];
            for (int j = 0; j < HIDDEN_SIZE; j++) {
                sum += hidden[j] * weightsHiddenOutput[j][k];
            }
            outputs[k] = (float) Math.tanh(sum);
        }

        float velocityMod = 1.0f + outputs[0] * 0.15f * saccadeSpeedMultiplier;
        float curvatureMod = curvatureBias * (1.0f + outputs[1] * 0.25f);
        float tremorMod = tremorVolatility * (1.0f + outputs[2] * 0.20f);

        return new float[]{velocityMod, curvatureMod, tremorMod};
    }

    public synchronized void trainOnline(float progress, float angleDeltaDeg, float currentVelocity, float directionSign, float targetVelMod, float targetCurvMod, float targetTremorMod) {
        float x0 = MathHelper.clamp(progress, 0.0f, 1.0f);
        float x1 = MathHelper.clamp(angleDeltaDeg / 180.0f, 0.0f, 1.0f);
        float x2 = MathHelper.clamp(currentVelocity / 50.0f, -2.0f, 2.0f);
        float x3 = MathHelper.clamp(directionSign, -1.0f, 1.0f);

        float[] inputs = new float[]{x0, x1, x2, x3};
        float[] hiddenRaw = new float[HIDDEN_SIZE];
        float[] hidden = new float[HIDDEN_SIZE];

        for (int j = 0; j < HIDDEN_SIZE; j++) {
            float sum = biasHidden[j];
            for (int i = 0; i < INPUT_SIZE; i++) {
                sum += inputs[i] * weightsInputHidden[i][j];
            }
            hiddenRaw[j] = sum;
            hidden[j] = (float) Math.tanh(sum);
        }

        float[] outputs = new float[OUTPUT_SIZE];
        for (int k = 0; k < OUTPUT_SIZE; k++) {
            float sum = biasOutput[k];
            for (int j = 0; j < HIDDEN_SIZE; j++) {
                sum += hidden[j] * weightsHiddenOutput[j][k];
            }
            outputs[k] = (float) Math.tanh(sum);
        }

        float[] targets = new float[]{
            MathHelper.clamp((targetVelMod - 1.0f) / 0.15f, -1.0f, 1.0f),
            MathHelper.clamp((targetCurvMod - curvatureBias) / Math.max(0.01f, curvatureBias * 0.25f), -1.0f, 1.0f),
            MathHelper.clamp((targetTremorMod - tremorVolatility) / Math.max(0.01f, tremorVolatility * 0.20f), -1.0f, 1.0f)
        };

        float learningRate = 0.015f;
        float momentumFactor = 0.85f;

        float[] deltaOutput = new float[OUTPUT_SIZE];
        for (int k = 0; k < OUTPUT_SIZE; k++) {
            float err = targets[k] - outputs[k];
            float dtanh = 1.0f - outputs[k] * outputs[k];
            deltaOutput[k] = err * dtanh;
        }

        float[] deltaHidden = new float[HIDDEN_SIZE];
        for (int j = 0; j < HIDDEN_SIZE; j++) {
            float sum = 0.0f;
            for (int k = 0; k < OUTPUT_SIZE; k++) {
                sum += deltaOutput[k] * weightsHiddenOutput[j][k];
            }
            float dtanh = 1.0f - hidden[j] * hidden[j];
            deltaHidden[j] = sum * dtanh;
        }

        for (int j = 0; j < HIDDEN_SIZE; j++) {
            for (int k = 0; k < OUTPUT_SIZE; k++) {
                float grad = learningRate * deltaOutput[k] * hidden[j];
                momentumHiddenOutput[j][k] = momentumFactor * momentumHiddenOutput[j][k] + grad;
                weightsHiddenOutput[j][k] += momentumHiddenOutput[j][k];
            }
        }
        for (int k = 0; k < OUTPUT_SIZE; k++) {
            biasOutput[k] += learningRate * deltaOutput[k];
        }

        for (int i = 0; i < INPUT_SIZE; i++) {
            for (int j = 0; j < HIDDEN_SIZE; j++) {
                float grad = learningRate * deltaHidden[j] * inputs[i];
                momentumInputHidden[i][j] = momentumFactor * momentumInputHidden[i][j] + grad;
                weightsInputHidden[i][j] += momentumInputHidden[i][j];
            }
        }
        for (int j = 0; j < HIDDEN_SIZE; j++) {
            biasHidden[j] += learningRate * deltaHidden[j];
        }

        sampleCount++;
        if (sampleCount % 50 == 0) {
            save();
        }
    }

    public synchronized float[] getNextTremor(float randomnessScale) {
        long now = System.nanoTime();
        if (ouLastTimeNs == 0L) {
            ouLastTimeNs = now;
            return new float[]{0.0f, 0.0f};
        }

        double dt = Math.min(0.05, Math.max(0.001, (now - ouLastTimeNs) / 1_000_000_000.0));
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
        if (client == null || client.player == null) {
            return;
        }
        INSTANCE.trackFrame(client);
    }

    private synchronized void trackFrame(MinecraftClient client) {
        long now = System.nanoTime();
        float currentPitch = client.player.getPitch();
        float currentYaw = client.player.getYaw();

        if (!trackerInitialized) {
            lastPlayerPitch = currentPitch;
            lastPlayerYaw = currentYaw;
            lastTrackTimeNs = now;
            trackerInitialized = true;
            return;
        }

        double dt = Math.max(0.001, (now - lastTrackTimeNs) / 1_000_000_000.0);
        lastTrackTimeNs = now;

        float deltaPitch = currentPitch - lastPlayerPitch;
        float deltaYaw = MathHelper.wrapDegrees(currentYaw - lastPlayerYaw);

        lastPlayerPitch = currentPitch;
        lastPlayerYaw = currentYaw;

        float magSq = deltaPitch * deltaPitch + deltaYaw * deltaYaw;
        if (magSq < 0.0001f) {
            return;
        }

        float mag = (float) Math.sqrt(magSq);
        float velocity = (float) (mag / dt);

        if (client.options != null) {
            double gcd = ArcCameraInterpolator.calculateMouseGcd(client);
            if (gcd > 1.0E-5) {
                this.learnedGcd = gcd;
            }
        }

        float direction = deltaYaw >= 0.0f ? 1.0f : -1.0f;
        float observedVelMod = MathHelper.clamp(velocity / 300.0f, 0.7f, 1.4f);
        float observedCurvMod = MathHelper.clamp(Math.abs(deltaPitch) / Math.max(0.1f, mag), 0.1f, 0.6f);
        float observedTremor = MathHelper.clamp(mag / 20.0f, 0.02f, 0.09f);

        trainOnline(0.5f, mag, velocity, direction, observedVelMod, observedCurvMod, observedTremor);

        if (calibrating) {
            calibrationSamplesCollected++;
            long elapsed = System.currentTimeMillis() - calibrationStartTimeMs;
            if (calibrationSamplesCollected >= CALIBRATION_REQUIRED_SAMPLES || elapsed >= CALIBRATION_DURATION_MS) {
                finishCalibration();
            }
        }
    }

    public synchronized void startCalibration() {
        calibrating = true;
        calibrationStartTimeMs = System.currentTimeMillis();
        calibrationSamplesCollected = 0;
    }

    public synchronized void cancelCalibration() {
        calibrating = false;
        calibrationStartTimeMs = 0L;
        calibrationSamplesCollected = 0;
    }

    private synchronized void finishCalibration() {
        calibrating = false;
        calibrated = true;
        saccadeSpeedMultiplier = MathHelper.clamp(saccadeSpeedMultiplier, 0.85f, 1.25f);
        curvatureBias = MathHelper.clamp(curvatureBias, 0.20f, 0.50f);
        tremorVolatility = MathHelper.clamp(tremorVolatility, 0.03f, 0.08f);
        save();
    }

    public synchronized boolean isCalibrating() {
        return calibrating;
    }

    public synchronized int getCalibrationProgressPercent() {
        if (!calibrating) {
            return calibrated ? 100 : 0;
        }
        int bySamples = (calibrationSamplesCollected * 100) / CALIBRATION_REQUIRED_SAMPLES;
        long elapsed = System.currentTimeMillis() - calibrationStartTimeMs;
        int byTime = (int) ((elapsed * 100) / CALIBRATION_DURATION_MS);
        return MathHelper.clamp(Math.max(bySamples, byTime), 0, 99);
    }

    public synchronized boolean isCalibrated() {
        return calibrated;
    }

    public synchronized float getSaccadeSpeedMultiplier() {
        return saccadeSpeedMultiplier;
    }

    public synchronized float getCurvatureBias() {
        return curvatureBias;
    }

    public synchronized float getTremorVolatility() {
        return tremorVolatility;
    }

    public synchronized float getTwoPhaseRatio() {
        return twoPhaseRatio;
    }

    public synchronized float getMicroDamping() {
        return microDamping;
    }

    public synchronized double getLearnedGcd() {
        return learnedGcd;
    }

    public synchronized int getSampleCount() {
        return sampleCount;
    }

    private static Path getConfigPath() {
        try {
            var loader = net.fabricmc.loader.api.FabricLoader.getInstance();
            if (loader != null && loader.getConfigDir() != null) {
                return loader.getConfigDir().resolve("autocart_neural.json");
            }
        } catch (Throwable ignored) {}
        return Path.of("config", "autocart_neural.json");
    }

    public synchronized void save() {
        try {
            Path path = getConfigPath();
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            JsonObject root = new JsonObject();
            root.addProperty("version", 1);
            root.addProperty("calibrated", calibrated);
            root.addProperty("sampleCount", sampleCount);
            root.addProperty("saccadeSpeedMultiplier", saccadeSpeedMultiplier);
            root.addProperty("curvatureBias", curvatureBias);
            root.addProperty("tremorVolatility", tremorVolatility);
            root.addProperty("twoPhaseRatio", twoPhaseRatio);
            root.addProperty("microDamping", microDamping);
            root.addProperty("learnedGcd", learnedGcd);

            JsonObject weightsObj = new JsonObject();
            for (int i = 0; i < INPUT_SIZE; i++) {
                for (int j = 0; j < HIDDEN_SIZE; j++) {
                    weightsObj.addProperty("ih_" + i + "_" + j, weightsInputHidden[i][j]);
                }
            }
            for (int j = 0; j < HIDDEN_SIZE; j++) {
                for (int k = 0; k < OUTPUT_SIZE; k++) {
                    weightsObj.addProperty("ho_" + j + "_" + k, weightsHiddenOutput[j][k]);
                }
            }
            root.add("weights", weightsObj);

            String json = GSON.toJson(root);
            Files.writeString(path, json, StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
    }

    public synchronized void load() {
        try {
            Path path = getConfigPath();
            if (!Files.exists(path)) {
                return;
            }
            String content = Files.readString(path, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            if (root.has("calibrated")) {
                calibrated = root.get("calibrated").getAsBoolean();
            }
            if (root.has("sampleCount")) {
                sampleCount = root.get("sampleCount").getAsInt();
            }
            if (root.has("saccadeSpeedMultiplier")) {
                saccadeSpeedMultiplier = root.get("saccadeSpeedMultiplier").getAsFloat();
            }
            if (root.has("curvatureBias")) {
                curvatureBias = root.get("curvatureBias").getAsFloat();
            }
            if (root.has("tremorVolatility")) {
                tremorVolatility = root.get("tremorVolatility").getAsFloat();
            }
            if (root.has("twoPhaseRatio")) {
                twoPhaseRatio = root.get("twoPhaseRatio").getAsFloat();
            }
            if (root.has("microDamping")) {
                microDamping = root.get("microDamping").getAsFloat();
            }
            if (root.has("learnedGcd")) {
                learnedGcd = root.get("learnedGcd").getAsDouble();
            }

            if (root.has("weights")) {
                JsonObject weightsObj = root.getAsJsonObject("weights");
                for (int i = 0; i < INPUT_SIZE; i++) {
                    for (int j = 0; j < HIDDEN_SIZE; j++) {
                        String key = "ih_" + i + "_" + j;
                        if (weightsObj.has(key)) {
                            weightsInputHidden[i][j] = weightsObj.get(key).getAsFloat();
                        }
                    }
                }
                for (int j = 0; j < HIDDEN_SIZE; j++) {
                    for (int k = 0; k < OUTPUT_SIZE; k++) {
                        String key = "ho_" + j + "_" + k;
                        if (weightsObj.has(key)) {
                            weightsHiddenOutput[j][k] = weightsObj.get(key).getAsFloat();
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}

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
    public static final long CALIBRATION_DURATION_MS = 300000L;

    private int learnedMinDelayMs = 50;
    private int learnedMaxDelayMs = 80;
    private int learnedCameraSmoothness = 110;
    private int learnedPlacementDelayRailMs = 50;
    private int learnedPlacementDelayCartMs = 60;

    private int bowShotsCount = 0;
    private int railsPlacedCount = 0;
    private int cartsPlacedCount = 0;
    private int explosionsCount = 0;
    private int manualDetonationsCount = 0;
    private int totalActionsCount = 0;

    private long lastBowReleaseTimeMs = 0L;
    private long lastRailPlacementTimeMs = 0L;
    private long lastCartPlacementTimeMs = 0L;
    private net.minecraft.util.math.BlockPos lastPlacedCartPos = null;

    private long sumDeltaT1 = 0L;
    private int countDeltaT1 = 0;
    private long sumDeltaT2 = 0L;
    private int countDeltaT2 = 0;

    private float accumulatedVelMod = 0.0f;
    private float accumulatedCurvMod = 0.0f;
    private float accumulatedTremor = 0.0f;
    private long flickDurationMs = 0L;
    private long sumFlickDurationMs = 0L;
    private int totalFlicksMeasured = 0;

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

        if (ArcCameraInterpolator.isAnyActive()) {
            lastPlayerPitch = currentPitch;
            lastPlayerYaw = currentYaw;
            lastTrackTimeNs = now;
            return;
        }

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
            accumulatedVelMod += observedVelMod;
            accumulatedCurvMod += observedCurvMod;
            accumulatedTremor += observedTremor;

            if (mag > 2.0f) {
                flickDurationMs += (long) (dt * 1000.0);
            } else if (flickDurationMs > 0L) {
                if (flickDurationMs >= 30L && flickDurationMs <= 500L) {
                    sumFlickDurationMs += flickDurationMs;
                    totalFlicksMeasured++;
                }
                flickDurationMs = 0L;
            }

            long elapsed = System.currentTimeMillis() - calibrationStartTimeMs;
            if (elapsed >= CALIBRATION_DURATION_MS) {
                finishCalibration();
            }
        }
    }

    public synchronized void recordBowRelease(int drawTicks) {
        if (!calibrating) return;
        long now = System.currentTimeMillis();
        lastBowReleaseTimeMs = now;
        bowShotsCount++;
        totalActionsCount++;
    }

    public synchronized void recordRailPlacement(net.minecraft.util.math.BlockPos pos) {
        if (!calibrating) return;
        long now = System.currentTimeMillis();
        railsPlacedCount++;
        totalActionsCount++;
        if (lastBowReleaseTimeMs > 0L) {
            long dt1 = now - lastBowReleaseTimeMs;
            if (dt1 >= 30L && dt1 <= 3000L) {
                sumDeltaT1 += dt1;
                countDeltaT1++;
            }
        }
        lastRailPlacementTimeMs = now;
    }

    public synchronized void recordCartPlacement(net.minecraft.util.math.BlockPos pos) {
        if (!calibrating) return;
        long now = System.currentTimeMillis();
        cartsPlacedCount++;
        totalActionsCount++;
        lastPlacedCartPos = pos != null ? pos.toImmutable() : null;
        if (lastRailPlacementTimeMs > 0L) {
            long dt2 = now - lastRailPlacementTimeMs;
            if (dt2 >= 20L && dt2 <= 3000L) {
                sumDeltaT2 += dt2;
                countDeltaT2++;
            }
        }
        lastCartPlacementTimeMs = now;
    }

    public synchronized void recordExplosion(double x, double y, double z) {
        if (!calibrating) return;
        long now = System.currentTimeMillis();
        explosionsCount++;
        if (lastCartPlacementTimeMs > 0L) {
            long dtExp = now - lastCartPlacementTimeMs;
            if (dtExp >= 0L && dtExp <= 4000L) {
                boolean matched = true;
                if (lastPlacedCartPos != null) {
                    double dx = x - (lastPlacedCartPos.getX() + 0.5D);
                    double dy = y - (lastPlacedCartPos.getY() + 0.5D);
                    double dz = z - (lastPlacedCartPos.getZ() + 0.5D);
                    double distSq = dx * dx + dy * dy + dz * dz;
                    matched = distSq <= 144.0D;
                }
                if (matched) {
                    manualDetonationsCount++;
                    totalActionsCount++;
                    lastCartPlacementTimeMs = 0L;
                    lastPlacedCartPos = null;
                }
            }
        }
    }

    public synchronized void startCalibration() {
        calibrating = true;
        calibrationStartTimeMs = System.currentTimeMillis();
        calibrationSamplesCollected = 0;
        accumulatedVelMod = 0.0f;
        accumulatedCurvMod = 0.0f;
        accumulatedTremor = 0.0f;
        flickDurationMs = 0L;
        sumFlickDurationMs = 0L;
        totalFlicksMeasured = 0;
        bowShotsCount = 0;
        railsPlacedCount = 0;
        cartsPlacedCount = 0;
        explosionsCount = 0;
        manualDetonationsCount = 0;
        totalActionsCount = 0;
        lastBowReleaseTimeMs = 0L;
        lastRailPlacementTimeMs = 0L;
        lastCartPlacementTimeMs = 0L;
        lastPlacedCartPos = null;
        sumDeltaT1 = 0L;
        countDeltaT1 = 0;
        sumDeltaT2 = 0L;
        countDeltaT2 = 0;
    }

    public synchronized void cancelCalibration() {
        calibrating = false;
        calibrationStartTimeMs = 0L;
    }

    public synchronized void finishCalibration() {
        calibrating = false;
        calibrated = true;

        if (calibrationSamplesCollected > 0) {
            float avgVel = accumulatedVelMod / calibrationSamplesCollected;
            float avgCurv = accumulatedCurvMod / calibrationSamplesCollected;
            float avgTremor = accumulatedTremor / calibrationSamplesCollected;

            saccadeSpeedMultiplier = MathHelper.clamp(avgVel, 0.80f, 1.35f);
            curvatureBias = MathHelper.clamp(avgCurv, 0.20f, 0.50f);
            tremorVolatility = MathHelper.clamp(avgTremor, 0.03f, 0.08f);
        } else {
            saccadeSpeedMultiplier = MathHelper.clamp(saccadeSpeedMultiplier, 0.85f, 1.25f);
            curvatureBias = MathHelper.clamp(curvatureBias, 0.20f, 0.50f);
            tremorVolatility = MathHelper.clamp(tremorVolatility, 0.03f, 0.08f);
        }

        if (totalFlicksMeasured > 5) {
            learnedCameraSmoothness = MathHelper.clamp((int) Math.round((double) sumFlickDurationMs / totalFlicksMeasured), 40, 180);
        } else {
            learnedCameraSmoothness = MathHelper.clamp(Math.round(110.0f / saccadeSpeedMultiplier), 45, 180);
        }

        if (countDeltaT1 > 0 && countDeltaT2 > 0) {
            int avgT1 = (int) Math.round((double) sumDeltaT1 / countDeltaT1);
            int avgT2 = (int) Math.round((double) sumDeltaT2 / countDeltaT2);
            int minObserved = Math.min(avgT1, avgT2);
            int maxObserved = Math.max(avgT1, avgT2);
            learnedMinDelayMs = MathHelper.clamp((int) Math.round(minObserved * 0.85), 25, 120);
            learnedMaxDelayMs = MathHelper.clamp((int) Math.round(maxObserved * 1.15), learnedMinDelayMs + 10, 180);
            learnedPlacementDelayRailMs = MathHelper.clamp(avgT1, 20, 200);
            learnedPlacementDelayCartMs = MathHelper.clamp(avgT2, 20, 200);
        } else if (countDeltaT2 > 0) {
            int avgT2 = (int) Math.round((double) sumDeltaT2 / countDeltaT2);
            learnedMinDelayMs = MathHelper.clamp((int) Math.round(avgT2 * 0.85), 25, 120);
            learnedMaxDelayMs = MathHelper.clamp((int) Math.round(avgT2 * 1.15), learnedMinDelayMs + 10, 180);
            learnedPlacementDelayRailMs = learnedMinDelayMs;
            learnedPlacementDelayCartMs = MathHelper.clamp(avgT2, 20, 200);
        } else {
            learnedMinDelayMs = MathHelper.clamp(Math.round(50.0f / saccadeSpeedMultiplier), 30, 90);
            learnedMaxDelayMs = MathHelper.clamp(Math.round(80.0f / saccadeSpeedMultiplier), learnedMinDelayMs + 15, 140);
            learnedPlacementDelayRailMs = learnedMinDelayMs;
            learnedPlacementDelayCartMs = (learnedMinDelayMs + learnedMaxDelayMs) / 2;
        }

        save();
    }

    public synchronized boolean isCalibrating() {
        if (calibrating) {
            long elapsed = System.currentTimeMillis() - calibrationStartTimeMs;
            if (elapsed >= CALIBRATION_DURATION_MS) {
                finishCalibration();
                return false;
            }
        }
        return calibrating;
    }

    public synchronized int getCalibrationProgressPercent() {
        if (!calibrating) {
            return calibrated ? 100 : 0;
        }
        long elapsed = System.currentTimeMillis() - calibrationStartTimeMs;
        int byTime = (int) ((elapsed * 100L) / CALIBRATION_DURATION_MS);
        return MathHelper.clamp(byTime, 0, 99);
    }

    public synchronized long getCalibrationRemainingTimeMs() {
        if (!calibrating) {
            return 0L;
        }
        long elapsed = System.currentTimeMillis() - calibrationStartTimeMs;
        return Math.max(0L, CALIBRATION_DURATION_MS - elapsed);
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

    public synchronized int getLearnedMinDelayMs() {
        return learnedMinDelayMs;
    }

    public synchronized int getLearnedMaxDelayMs() {
        return learnedMaxDelayMs;
    }

    public synchronized int getLearnedCameraSmoothness() {
        return learnedCameraSmoothness;
    }

    public synchronized int getLearnedPlacementDelayRailMs() {
        return learnedPlacementDelayRailMs;
    }

    public synchronized int getLearnedPlacementDelayCartMs() {
        return learnedPlacementDelayCartMs;
    }

    public synchronized int getBowShotsCount() {
        return bowShotsCount;
    }

    public synchronized int getRailsPlacedCount() {
        return railsPlacedCount;
    }

    public synchronized int getCartsPlacedCount() {
        return cartsPlacedCount;
    }

    public synchronized int getExplosionsCount() {
        return explosionsCount;
    }

    public synchronized int getManualDetonationsCount() {
        return manualDetonationsCount;
    }

    public synchronized int getTotalActionsCount() {
        return totalActionsCount;
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
            root.addProperty("version", 2);
            root.addProperty("calibrated", calibrated);
            root.addProperty("sampleCount", sampleCount);
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
            root.addProperty("bowShotsCount", bowShotsCount);
            root.addProperty("railsPlacedCount", railsPlacedCount);
            root.addProperty("cartsPlacedCount", cartsPlacedCount);
            root.addProperty("explosionsCount", explosionsCount);
            root.addProperty("manualDetonationsCount", manualDetonationsCount);
            root.addProperty("totalActionsCount", totalActionsCount);

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
            if (root.has("learnedMinDelayMs")) {
                learnedMinDelayMs = root.get("learnedMinDelayMs").getAsInt();
            }
            if (root.has("learnedMaxDelayMs")) {
                learnedMaxDelayMs = root.get("learnedMaxDelayMs").getAsInt();
            }
            if (root.has("learnedCameraSmoothness")) {
                learnedCameraSmoothness = root.get("learnedCameraSmoothness").getAsInt();
            }
            if (root.has("learnedPlacementDelayRailMs")) {
                learnedPlacementDelayRailMs = root.get("learnedPlacementDelayRailMs").getAsInt();
            }
            if (root.has("learnedPlacementDelayCartMs")) {
                learnedPlacementDelayCartMs = root.get("learnedPlacementDelayCartMs").getAsInt();
            }
            if (root.has("bowShotsCount")) {
                bowShotsCount = root.get("bowShotsCount").getAsInt();
            }
            if (root.has("railsPlacedCount")) {
                railsPlacedCount = root.get("railsPlacedCount").getAsInt();
            }
            if (root.has("cartsPlacedCount")) {
                cartsPlacedCount = root.get("cartsPlacedCount").getAsInt();
            }
            if (root.has("explosionsCount")) {
                explosionsCount = root.get("explosionsCount").getAsInt();
            }
            if (root.has("manualDetonationsCount")) {
                manualDetonationsCount = root.get("manualDetonationsCount").getAsInt();
            }
            if (root.has("totalActionsCount")) {
                totalActionsCount = root.get("totalActionsCount").getAsInt();
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

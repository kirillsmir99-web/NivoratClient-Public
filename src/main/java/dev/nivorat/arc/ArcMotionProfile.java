package dev.nivorat.arc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class ArcMotionProfile {
    private static final ArcMotionProfile INSTANCE = new ArcMotionProfile();
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
    private final float[] inputsScratch = new float[INPUT_SIZE];
    private final float[] hiddenScratch = new float[HIDDEN_SIZE];
    private final float[] outputsScratch = new float[OUTPUT_SIZE];
    private final float[] deltaOutputScratch = new float[OUTPUT_SIZE];
    private final float[] deltaHiddenScratch = new float[HIDDEN_SIZE];

    private float saccadeSpeedMultiplier = 1.0f;
    private float curvatureBias = 0.35f;
    private float tremorVolatility = 0.05f;
    private float twoPhaseRatio = 0.88f;
    private float microDamping = 0.85f;
    private double learnedGcd = 0.0096;
    private int sampleCount = 0;
    private boolean calibrated = false;
    private boolean lastCalibrationSucceeded = false;
    private long lastSaveTimeMs = System.currentTimeMillis();
    private boolean dirty = false;

    private ArcCalibrationState state = ArcCalibrationState.WAITING;
    private boolean calibrating = false;
    private boolean calibrationPaused = false;
    private boolean adaptiveLearning = true;
    private long pauseStartedMs;
    private TrainingSnapshot calibrationSnapshot;
    private long calibrationStartTimeMs = 0L;
    private int calibrationSamplesCollected = 0;
    public static final long CALIBRATION_DURATION_MS = 300000L;
    private long targetCalibrationDurationMs = CALIBRATION_DURATION_MS;

    private int learnedMinDelayMs = 50;
    private int learnedMaxDelayMs = 80;
    private int learnedCameraSmoothness = 110;
    private int learnedPlacementDelayRailMs = 50;
    private int learnedPlacementDelayCartMs = 60;

    private int calibrationSessionCount = 0;
    private float confidenceScore = 0.0f;
    private int masteryPercent = 0;

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
    private int lastBowDrawTicks = 0;
    private final List<Integer> successfulDrawTicks = new ArrayList<>();

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
    private final Path storagePath;
    private final java.util.function.LongSupplier clock;
    private final ArcMotorAnalysisEngine analysisEngine = new ArcMotorAnalysisEngine();

    private ArcMotionProfile() {
        this(getConfigPath());
    }

    ArcMotionProfile(Path storagePath) {
        this(storagePath, System::currentTimeMillis);
    }

    ArcMotionProfile(Path storagePath, java.util.function.LongSupplier clock) {
        this.storagePath = storagePath;
        this.clock = clock;
        initDefaultWeights();
        load();
    }

    public static ArcMotionProfile getInstance() {
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
        if (!Float.isFinite(progress) || !Float.isFinite(angleDeltaDeg)
                || !Float.isFinite(currentVelocity) || !Float.isFinite(directionSign)) {
            return new float[]{1.0f, curvatureBias, tremorVolatility};
        }
        float x0 = MathHelper.clamp(progress, 0.0f, 1.0f);
        float x1 = MathHelper.clamp(angleDeltaDeg / 180.0f, 0.0f, 1.0f);
        float x2 = MathHelper.clamp(currentVelocity / 50.0f, -2.0f, 2.0f);
        float x3 = MathHelper.clamp(directionSign, -1.0f, 1.0f);

        float[] inputs = inputsScratch;
        inputs[0] = x0; inputs[1] = x1; inputs[2] = x2; inputs[3] = x3;
        float[] hidden = hiddenScratch;

        for (int j = 0; j < HIDDEN_SIZE; j++) {
            float sum = biasHidden[j];
            for (int i = 0; i < INPUT_SIZE; i++) {
                sum += inputs[i] * weightsInputHidden[i][j];
            }
            hidden[j] = (float) Math.tanh(sum);
        }

        float[] outputs = outputsScratch;
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
        if (!Float.isFinite(progress) || !Float.isFinite(angleDeltaDeg)
                || !Float.isFinite(currentVelocity) || !Float.isFinite(directionSign)
                || !Float.isFinite(targetVelMod) || !Float.isFinite(targetCurvMod)
                || !Float.isFinite(targetTremorMod)) return;
        float x0 = MathHelper.clamp(progress, 0.0f, 1.0f);
        float x1 = MathHelper.clamp(angleDeltaDeg / 180.0f, 0.0f, 1.0f);
        float x2 = MathHelper.clamp(currentVelocity / 50.0f, -2.0f, 2.0f);
        float x3 = MathHelper.clamp(directionSign, -1.0f, 1.0f);

        float[] inputs = inputsScratch;
        inputs[0] = x0; inputs[1] = x1; inputs[2] = x2; inputs[3] = x3;
        float[] hidden = hiddenScratch;

        for (int j = 0; j < HIDDEN_SIZE; j++) {
            float sum = biasHidden[j];
            for (int i = 0; i < INPUT_SIZE; i++) {
                sum += inputs[i] * weightsInputHidden[i][j];
            }
            hidden[j] = (float) Math.tanh(sum);
        }

        float[] outputs = outputsScratch;
        for (int k = 0; k < OUTPUT_SIZE; k++) {
            float sum = biasOutput[k];
            for (int j = 0; j < HIDDEN_SIZE; j++) {
                sum += hidden[j] * weightsHiddenOutput[j][k];
            }
            outputs[k] = (float) Math.tanh(sum);
        }

        float[] targets = new float[]{
                MathHelper.clamp((targetVelMod - 1.0f) / Math.max(0.01f, 0.15f * saccadeSpeedMultiplier), -1.0f, 1.0f),
                MathHelper.clamp((targetCurvMod - curvatureBias) / Math.max(0.01f, curvatureBias * 0.25f), -1.0f, 1.0f),
                MathHelper.clamp((targetTremorMod - tremorVolatility) / Math.max(0.01f, tremorVolatility * 0.20f), -1.0f, 1.0f)
        };

        float learningRate = 0.015f;
        float momentumFactor = 0.85f;

        float[] deltaOutput = deltaOutputScratch;
        for (int k = 0; k < OUTPUT_SIZE; k++) {
            float err = targets[k] - outputs[k];
            float dtanh = 1.0f - outputs[k] * outputs[k];
            deltaOutput[k] = err * dtanh;
        }

        float[] deltaHidden = deltaHiddenScratch;
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

        if (sampleCount < Integer.MAX_VALUE) sampleCount++;
        dirty = true;
    }

    public synchronized void flushCheckpoint() {
        if (!calibrating && dirty && System.currentTimeMillis() - lastSaveTimeMs >= 30_000L) save();
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
        if (client == null || client.player == null) {
            return;
        }
        INSTANCE.trackFrame(client);
    }

    private synchronized void trackFrame(MinecraftClient client) {
        ArcInputRecorder.getInstance().updateScreenAndFocusState(client);

        if (calibrationPaused || (!calibrating && (!adaptiveLearning || !calibrated))) {
            trackerInitialized = false;
            return;
        }
        long now = System.nanoTime();
        float currentPitch = client.player.getPitch();
        float currentYaw = client.player.getYaw();

        if (client.currentScreen != null || client.world == null
                || net.fabricmc.pack.api.CombatLockManager.isLocked() || ArcCameraInterpolator.isAnyActive()) {
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

        double dt = (now - lastTrackTimeNs) / 1_000_000_000.0;
        if (dt < 0.05) return;
        lastTrackTimeNs = now;

        float deltaPitch = currentPitch - lastPlayerPitch;
        float deltaYaw = MathHelper.wrapDegrees(currentYaw - lastPlayerYaw);

        lastPlayerPitch = currentPitch;
        lastPlayerYaw = currentYaw;

        if (dt > 0.25 || !Float.isFinite(deltaPitch) || !Float.isFinite(deltaYaw)) return;

        if (client.options != null) {
            double gcd = ArcCameraInterpolator.calculateMouseGcd(client);
            if (gcd > 1.0E-5) {
                this.learnedGcd = gcd;
            }
        }

        boolean isMoving = client.player != null && (Math.abs(client.player.getVelocity().x) > 0.03 || Math.abs(client.player.getVelocity().z) > 0.03 || !client.player.isOnGround());
        observeMovement(deltaPitch, deltaYaw, dt, isMoving);
    }

    synchronized void observeMovement(float deltaPitch, float deltaYaw, double dt) {
        observeMovement(deltaPitch, deltaYaw, dt, false);
    }

    synchronized void observeMovement(float deltaPitch, float deltaYaw, double dt, boolean isMoving) {
        if (calibrationPaused || (!calibrating && (!adaptiveLearning || !calibrated))
                || !Double.isFinite(dt) || dt < 0.05 || dt > 0.25
                || !Float.isFinite(deltaPitch) || !Float.isFinite(deltaYaw)) return;
        float magSq = deltaPitch * deltaPitch + deltaYaw * deltaYaw;
        if (!Float.isFinite(magSq) || magSq < 0.0001f || magSq > 8100.0f) return;
        float mag = (float) Math.sqrt(magSq);
        float velocity = (float) (mag / dt);

        int quad = ArcMotorFrame.computeQuadrant(deltaPitch, deltaYaw);
        ArcMotorFrame frame = new ArcMotorFrame(clock.getAsLong(), deltaPitch, deltaYaw, mag, velocity, 0.0f, quad, ArcMotorFrame.ActionType.CAMERA_TICK, (long) (dt * 1000.0), isMoving);
        analysisEngine.recordFrame(frame);

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

            long elapsed = clock.getAsLong() - calibrationStartTimeMs;
            if (elapsed >= targetCalibrationDurationMs) {
                finishCalibration();
            }
        }
    }

    public synchronized void recordBowRelease(int drawTicks) {
        if (!calibrating || calibrationPaused || drawTicks < 3) return;
        long now = clock.getAsLong();
        if (now - lastBowReleaseTimeMs < 50L) return;
        lastBowReleaseTimeMs = now;
        lastBowDrawTicks = MathHelper.clamp(drawTicks, 3, 20);
        bowShotsCount++;
        totalActionsCount++;
        boolean isMoving = false;
        try {
            var mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc != null && mc.player != null) {
                isMoving = Math.abs(mc.player.getVelocity().x) > 0.03 || Math.abs(mc.player.getVelocity().z) > 0.03 || !mc.player.isOnGround();
            }
        } catch (Throwable ignored) {}
        analysisEngine.recordBowRelease(isMoving);
    }

    public synchronized void recordRailPlacement(net.minecraft.util.math.BlockPos pos) {
        if (!calibrating || calibrationPaused) return;
        long now = clock.getAsLong();
        railsPlacedCount++;
        totalActionsCount++;
        long dt1 = 0L;
        if (lastBowReleaseTimeMs > 0L) {
            dt1 = now - lastBowReleaseTimeMs;
            if (dt1 >= 30L && dt1 <= 3000L) {
                sumDeltaT1 += dt1;
                countDeltaT1++;
            }
            lastBowReleaseTimeMs = 0L;
        }
        boolean isMoving = false;
        float curPitch = 0.0f, curYaw = 0.0f;
        try {
            var mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc != null && mc.player != null) {
                isMoving = Math.abs(mc.player.getVelocity().x) > 0.03 || Math.abs(mc.player.getVelocity().z) > 0.03 || !mc.player.isOnGround();
                curPitch = mc.player.getPitch();
                curYaw = mc.player.getYaw();
            }
        } catch (Throwable ignored) {}
        float pitchDiff = lastPlayerPitch != 0.0f ? curPitch - lastPlayerPitch : 0.0f;
        float yawDiff = lastPlayerYaw != 0.0f ? MathHelper.wrapDegrees(curYaw - lastPlayerYaw) : 0.0f;
        analysisEngine.recordRailPlacement(dt1, isMoving, pitchDiff, yawDiff);
        lastRailPlacementTimeMs = now;
    }

    public synchronized void recordCartPlacement(net.minecraft.util.math.BlockPos pos) {
        if (!calibrating || calibrationPaused) return;
        long now = clock.getAsLong();
        cartsPlacedCount++;
        totalActionsCount++;
        lastPlacedCartPos = pos != null ? pos.toImmutable() : null;
        long dt2 = 0L;
        if (lastRailPlacementTimeMs > 0L) {
            dt2 = now - lastRailPlacementTimeMs;
            if (dt2 >= 20L && dt2 <= 3000L) {
                sumDeltaT2 += dt2;
                countDeltaT2++;
            }
            lastRailPlacementTimeMs = 0L;
        }
        boolean isMoving = false;
        float curPitch = 0.0f, curYaw = 0.0f;
        try {
            var mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc != null && mc.player != null) {
                isMoving = Math.abs(mc.player.getVelocity().x) > 0.03 || Math.abs(mc.player.getVelocity().z) > 0.03 || !mc.player.isOnGround();
                curPitch = mc.player.getPitch();
                curYaw = mc.player.getYaw();
                if (pos != null) {
                    analysisEngine.recordSituation(mc.player.getEyePos().distanceTo(net.minecraft.util.math.Vec3d.ofCenter(pos)));
                }
            }
        } catch (Throwable ignored) {}
        float pitchDiff = lastPlayerPitch != 0.0f ? curPitch - lastPlayerPitch : 0.0f;
        float yawDiff = lastPlayerYaw != 0.0f ? MathHelper.wrapDegrees(curYaw - lastPlayerYaw) : 0.0f;
        analysisEngine.recordCartPlacement(dt2, isMoving, pitchDiff, yawDiff);
        lastCartPlacementTimeMs = now;
    }

    public synchronized void recordExplosion(double x, double y, double z) {
        if (!calibrating || calibrationPaused) return;
        long now = clock.getAsLong();
        explosionsCount++;
        if (lastCartPlacementTimeMs > 0L) {
            long dtExp = now - lastCartPlacementTimeMs;
            if (dtExp >= 0L && dtExp <= 4000L) {
                boolean matched = true;
                double dist = 0.0;
                if (lastPlacedCartPos != null) {
                    double dx = x - (lastPlacedCartPos.getX() + 0.5D);
                    double dy = y - (lastPlacedCartPos.getY() + 0.5D);
                    double dz = z - (lastPlacedCartPos.getZ() + 0.5D);
                    double distSq = dx * dx + dy * dy + dz * dz;
                    matched = distSq <= 144.0D;
                    dist = Math.sqrt(distSq);
                }
                if (matched) {
                    manualDetonationsCount++;
                    totalActionsCount++;
                    analysisEngine.recordDetonation();
                    if (lastBowDrawTicks >= 3) {
                        successfulDrawTicks.add(lastBowDrawTicks);
                        if (successfulDrawTicks.size() > 50) successfulDrawTicks.remove(0);
                    }
                    AutoCartLogger.logExplosion(dist, lastBowDrawTicks);
                    lastCartPlacementTimeMs = 0L;
                    lastPlacedCartPos = null;
                }
            }
        }
    }

    public synchronized int sampleMacroDrawTicks(int fallbackTicks) {
        if (!successfulDrawTicks.isEmpty()) {
            int idx = ThreadLocalRandom.current().nextInt(successfulDrawTicks.size());
            int base = successfulDrawTicks.get(idx);
            int jitter = ThreadLocalRandom.current().nextInt(-1, 2);
            return MathHelper.clamp(base + jitter, 3, 20);
        }
        int fallback = fallbackTicks > 0 ? fallbackTicks : 3;
        if (fallback == 6) fallback = 3;
        return MathHelper.clamp(fallback, 3, 5);
    }

    public synchronized void startCalibration() {
        if (calibrating) return;
        calibrationSnapshot = new TrainingSnapshot(this);
        lastCalibrationSucceeded = false;
        calibrating = true;
        calibrationPaused = false;
        state = ArcCalibrationState.RECORDING;
        trackerInitialized = false;
        calibrationStartTimeMs = clock.getAsLong();
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
        analysisEngine.reset();
        ArcInputRecorder.getInstance().reset();
    }

    public synchronized void cancelCalibration() {
        if (calibrationSnapshot != null) calibrationSnapshot.restore(this);
        calibrationSnapshot = null;
        calibrating = false;
        calibrationPaused = false;
        state = calibrated ? ArcCalibrationState.READY : ArcCalibrationState.WAITING;
        lastCalibrationSucceeded = false;
        trackerInitialized = false;
        calibrationStartTimeMs = 0L;
        analysisEngine.reset();
        ArcInputRecorder.getInstance().reset();
    }

    public synchronized void pauseCalibration() {
        if (!calibrating || calibrationPaused) return;
        calibrationPaused = true;
        state = ArcCalibrationState.PAUSED;
        pauseStartedMs = clock.getAsLong();
        trackerInitialized = false;
        lastBowReleaseTimeMs = lastRailPlacementTimeMs = lastCartPlacementTimeMs = 0L;
        lastPlacedCartPos = null;
        flickDurationMs = 0L;
    }

    public synchronized void resumeCalibration() {
        if (!calibrating || !calibrationPaused) return;
        calibrationStartTimeMs += Math.max(0L, clock.getAsLong() - pauseStartedMs);
        calibrationPaused = false;
        state = ArcCalibrationState.RECORDING;
        trackerInitialized = false;
    }

    public synchronized boolean hasCalibrationSession() { return calibrating; }
    public synchronized boolean isCalibrationPaused() { return calibrating && calibrationPaused; }
    public synchronized boolean isReadyToFinish() {
        return (calibrationSamplesCollected >= 20 && countDeltaT1 >= 3 && countDeltaT2 >= 3)
                || analysisEngine.computeMasteryPercent() >= 75;
    }
    public synchronized int getCalibrationSamplesCollected() { return calibrationSamplesCollected; }
    public synchronized int getShotRailPairs() { return countDeltaT1; }
    public synchronized int getRailCartPairs() { return countDeltaT2; }
    public synchronized boolean isAdaptiveLearning() { return adaptiveLearning; }
    public synchronized void setAdaptiveLearning(boolean value) {
        adaptiveLearning = value;
        trackerInitialized = false;
        dirty = true;
        if (!calibrating) save();
    }

    public synchronized void finishCalibration() {
        if (!calibrating) return;
        state = ArcCalibrationState.PROCESSING;
        calibrating = false;
        calibrationPaused = false;
        trackerInitialized = false;
        lastCalibrationSucceeded = isReadyToFinish();
        if (!lastCalibrationSucceeded) {
            if (calibrationSnapshot != null) calibrationSnapshot.restore(this);
            calibrationSnapshot = null;
            state = calibrated ? ArcCalibrationState.READY : ArcCalibrationState.WAITING;
            return;
        }

        boolean wasCalibrated = this.calibrated;
        calibrationSnapshot = null;
        calibrated = true;
        state = ArcCalibrationState.READY;
        calibrationSessionCount++;
        masteryPercent = Math.max(masteryPercent, analysisEngine.computeMasteryPercent());
        confidenceScore = MathHelper.clamp(0.4f + 0.15f * calibrationSessionCount + 0.0005f * sampleCount, 0.0f, 1.0f);

        float newSaccade;
        float newCurvature;
        float newTremor;

        if (calibrationSamplesCollected > 0) {
            float avgVel = accumulatedVelMod / calibrationSamplesCollected;
            float avgCurv = accumulatedCurvMod / calibrationSamplesCollected;
            float avgTremor = accumulatedTremor / calibrationSamplesCollected;

            newSaccade = MathHelper.clamp(avgVel, 0.80f, 1.35f);
            newCurvature = MathHelper.clamp(avgCurv, 0.20f, 0.50f);
            newTremor = MathHelper.clamp(avgTremor, 0.03f, 0.08f);
        } else {
            newSaccade = MathHelper.clamp(saccadeSpeedMultiplier, 0.85f, 1.25f);
            newCurvature = MathHelper.clamp(curvatureBias, 0.20f, 0.50f);
            newTremor = MathHelper.clamp(tremorVolatility, 0.03f, 0.08f);
        }

        int newSmoothness;
        if (totalFlicksMeasured > 5) {
            newSmoothness = MathHelper.clamp((int) Math.round((double) sumFlickDurationMs / totalFlicksMeasured), 40, 180);
        } else {
            newSmoothness = MathHelper.clamp(Math.round(110.0f / newSaccade), 45, 180);
        }

        int newMinDelay;
        int newMaxDelay;
        int newRailDelay;
        int newCartDelay;

        if (countDeltaT1 > 0 && countDeltaT2 > 0) {
            int avgT1 = (int) Math.round((double) sumDeltaT1 / countDeltaT1);
            int avgT2 = (int) Math.round((double) sumDeltaT2 / countDeltaT2);
            int minObserved = Math.min(avgT1, avgT2);
            int maxObserved = Math.max(avgT1, avgT2);
            newMinDelay = MathHelper.clamp((int) Math.round(minObserved * 0.85), 25, 120);
            newMaxDelay = MathHelper.clamp((int) Math.round(maxObserved * 1.15), newMinDelay + 10, 180);
            newRailDelay = MathHelper.clamp(avgT1, 20, 200);
            newCartDelay = MathHelper.clamp(avgT2, 20, 200);
        } else if (countDeltaT2 > 0) {
            int avgT2 = (int) Math.round((double) sumDeltaT2 / countDeltaT2);
            newMinDelay = MathHelper.clamp((int) Math.round(avgT2 * 0.85), 25, 120);
            newMaxDelay = MathHelper.clamp((int) Math.round(avgT2 * 1.15), newMinDelay + 10, 180);
            newRailDelay = newMinDelay;
            newCartDelay = MathHelper.clamp(avgT2, 20, 200);
        } else {
            newMinDelay = MathHelper.clamp(Math.round(50.0f / newSaccade), 30, 90);
            newMaxDelay = MathHelper.clamp(Math.round(80.0f / newSaccade), newMinDelay + 15, 140);
            newRailDelay = newMinDelay;
            newCartDelay = (newMinDelay + newMaxDelay) / 2;
        }

        if (wasCalibrated) {
            float alpha = 0.45f;
            saccadeSpeedMultiplier = (1.0f - alpha) * saccadeSpeedMultiplier + alpha * newSaccade;
            curvatureBias = (1.0f - alpha) * curvatureBias + alpha * newCurvature;
            tremorVolatility = (1.0f - alpha) * tremorVolatility + alpha * newTremor;
            learnedCameraSmoothness = Math.round((1.0f - alpha) * learnedCameraSmoothness + alpha * newSmoothness);
            learnedMinDelayMs = Math.round((1.0f - alpha) * learnedMinDelayMs + alpha * newMinDelay);
            learnedMaxDelayMs = Math.round((1.0f - alpha) * learnedMaxDelayMs + alpha * newMaxDelay);
            learnedPlacementDelayRailMs = Math.round((1.0f - alpha) * learnedPlacementDelayRailMs + alpha * newRailDelay);
            learnedPlacementDelayCartMs = Math.round((1.0f - alpha) * learnedPlacementDelayCartMs + alpha * newCartDelay);
        } else {
            saccadeSpeedMultiplier = newSaccade;
            curvatureBias = newCurvature;
            tremorVolatility = newTremor;
            learnedCameraSmoothness = newSmoothness;
            learnedMinDelayMs = newMinDelay;
            learnedMaxDelayMs = newMaxDelay;
            learnedPlacementDelayRailMs = newRailDelay;
            learnedPlacementDelayCartMs = newCartDelay;
        }

        save();
    }

    public synchronized void resetCalibration() {
        calibrationSnapshot = null;
        calibrationPaused = false;
        lastCalibrationSucceeded = false;
        trackerInitialized = false;
        this.calibrated = false;
        this.calibrating = false;
        this.state = ArcCalibrationState.WAITING;
        this.sampleCount = 0;
        this.calibrationSamplesCollected = 0;
        this.calibrationSessionCount = 0;
        this.confidenceScore = 0.0f;
        this.masteryPercent = 0;
        this.bowShotsCount = 0;
        this.railsPlacedCount = 0;
        this.cartsPlacedCount = 0;
        this.explosionsCount = 0;
        this.manualDetonationsCount = 0;
        this.totalActionsCount = 0;
        this.sumDeltaT1 = 0L;
        this.countDeltaT1 = 0;
        this.sumDeltaT2 = 0L;
        this.countDeltaT2 = 0;
        this.successfulDrawTicks.clear();
        this.lastBowDrawTicks = 0;
        this.accumulatedVelMod = 0.0f;
        this.accumulatedCurvMod = 0.0f;
        this.accumulatedTremor = 0.0f;
        this.totalFlicksMeasured = 0;
        this.sumFlickDurationMs = 0L;
        this.saccadeSpeedMultiplier = 1.0f;
        this.curvatureBias = 0.35f;
        this.tremorVolatility = 0.05f;
        this.twoPhaseRatio = 0.88f;
        this.microDamping = 0.85f;
        this.learnedGcd = 0.0096;
        this.learnedMinDelayMs = 50;
        this.learnedMaxDelayMs = 80;
        this.learnedCameraSmoothness = 110;
        this.learnedPlacementDelayRailMs = 50;
        this.learnedPlacementDelayCartMs = 60;
        initDefaultWeights();
        analysisEngine.reset();
        ArcInputRecorder.getInstance().reset();
        save();
    }

    public synchronized boolean isCalibrating() {
        if (calibrating && !calibrationPaused) {
            long elapsed = clock.getAsLong() - calibrationStartTimeMs;
            if (elapsed >= targetCalibrationDurationMs) {
                finishCalibration();
                return false;
            }
        }
        return calibrating && !calibrationPaused;
    }

    public synchronized int getCalibrationProgressPercent() {
        if (!calibrating) {
            return calibrated ? 100 : 0;
        }
        return MathHelper.clamp(analysisEngine.computeMasteryPercent(), 0, 99);
    }

    public synchronized int getMasteryPercent() {
        if (calibrating) {
            return analysisEngine.computeMasteryPercent();
        }
        return calibrated ? Math.max(100, masteryPercent) : 0;
    }

    public synchronized float getConfidenceScore() {
        return calibrated ? MathHelper.clamp(confidenceScore > 0.0f ? confidenceScore : 0.65f, 0.0f, 1.0f) : 0.0f;
    }

    public synchronized List<String> getMissingDataHints() {
        return analysisEngine.getMissingDataHints();
    }

    public synchronized ArcCalibrationState getState() {
        if (calibrating) {
            return calibrationPaused ? ArcCalibrationState.PAUSED : ArcCalibrationState.RECORDING;
        }
        return calibrated ? ArcCalibrationState.READY : ArcCalibrationState.WAITING;
    }

    public synchronized long getCalibrationRemainingTimeMs() {
        if (!calibrating) {
            return 0L;
        }
        long elapsed = (calibrationPaused ? pauseStartedMs : clock.getAsLong()) - calibrationStartTimeMs;
        return Math.max(0L, targetCalibrationDurationMs - elapsed);
    }

    public synchronized long getCalibrationElapsedMs() {
        if (!calibrating) {
            return 0L;
        }
        return Math.max(0L, (calibrationPaused ? pauseStartedMs : clock.getAsLong()) - calibrationStartTimeMs);
    }

    public synchronized long getTargetCalibrationDurationMs() {
        return targetCalibrationDurationMs;
    }

    public synchronized void setTargetCalibrationDurationMs(long ms) {
        this.targetCalibrationDurationMs = Math.max(60000L, ms);
        this.dirty = true;
    }

    public synchronized int getTargetCalibrationDurationMinutes() {
        return (int) Math.round((double) targetCalibrationDurationMs / 60000.0);
    }

    public synchronized void setTargetCalibrationDurationMinutes(int minutes) {
        setTargetCalibrationDurationMs((long) MathHelper.clamp(minutes, 1, 10) * 60000L);
    }

    public synchronized boolean isCalibrated() {
        return calibrated;
    }

    public synchronized boolean didLastCalibrationSucceed() {
        return lastCalibrationSucceeded;
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

    public synchronized float getLearnedActionPitchDelta() {
        return analysisEngine.getAverageActionPitchDelta();
    }

    public synchronized int getLearnedPlacementDelayRailMs() {
        return learnedPlacementDelayRailMs;
    }

    public synchronized int getLearnedPlacementDelayCartMs() {
        return learnedPlacementDelayCartMs;
    }

    public synchronized void setLearnedCameraSmoothness(int val) {
        this.learnedCameraSmoothness = MathHelper.clamp(val, 40, 180);
        this.dirty = true;
    }

    public synchronized void setLearnedPlacementDelayRailMs(int val) {
        this.learnedPlacementDelayRailMs = MathHelper.clamp(val, 20, 200);
        this.dirty = true;
    }

    public synchronized void setLearnedPlacementDelayCartMs(int val) {
        this.learnedPlacementDelayCartMs = MathHelper.clamp(val, 20, 200);
        this.dirty = true;
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

    public synchronized JsonObject exportProfile() throws java.io.IOException {
        if (calibrating) throw new java.io.IOException("Завершите калибровку перед экспортом профиля");
        save();
        JsonObject root = JsonParser.parseString(Files.readString(storagePath, StandardCharsets.UTF_8)).getAsJsonObject();
        validateSharedProfile(root);
        return root;
    }

    public static void validateSharedProfile(JsonObject root) {
        if (root.toString().getBytes(StandardCharsets.UTF_8).length > 65_536) throw new IllegalArgumentException("Профиль превышает 64 КБ");
        validateNumbers(root);
        validateProfile(root);
        if (!root.has("version")) throw new IllegalArgumentException("Неполный профиль Автокарта");
        int v = root.get("version").getAsInt();
        if (v < 3 || v > 4) throw new IllegalArgumentException("Неподдерживаемая версия профиля: " + v);
        if (!root.has("weights") || !root.has("biasHidden") || !root.has("biasOutput")) throw new IllegalArgumentException("Неполный профиль Автокарта");
        JsonObject weights = root.getAsJsonObject("weights");
        for (int i = 0; i < INPUT_SIZE; i++) for (int j = 0; j < HIDDEN_SIZE; j++) requireNumber(weights.get("ih_" + i + "_" + j));
        for (int j = 0; j < HIDDEN_SIZE; j++) for (int k = 0; k < OUTPUT_SIZE; k++) requireNumber(weights.get("ho_" + j + "_" + k));
    }

    public synchronized void importProfile(JsonObject root) throws java.io.IOException {
        if (calibrating) throw new java.io.IOException("Завершите калибровку перед импортом профиля");
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
        if (calibrating) return;
        try {
            Path path = storagePath;
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            JsonObject root = new JsonObject();
            root.addProperty("version", 4);
            root.addProperty("calibrated", calibrated);
            root.addProperty("adaptiveLearning", adaptiveLearning);
            root.addProperty("sampleCount", sampleCount);
            root.addProperty("calibrationSessionCount", calibrationSessionCount);
            root.addProperty("confidenceScore", getConfidenceScore());
            root.addProperty("masteryPercent", masteryPercent);
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
            root.addProperty("targetCalibrationDurationMs", targetCalibrationDurationMs);
            if (!successfulDrawTicks.isEmpty()) {
                com.google.gson.JsonArray ticksArr = new com.google.gson.JsonArray();
                for (int t : successfulDrawTicks) ticksArr.add(t);
                root.add("successfulDrawTicks", ticksArr);
            }

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
            root.add("biasHidden", GSON.toJsonTree(biasHidden));
            root.add("biasOutput", GSON.toJsonTree(biasOutput));

            String json = GSON.toJson(root);
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(temporary, json, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                Files.move(temporary, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
            lastSaveTimeMs = System.currentTimeMillis();
        } catch (Exception ignored) {}
    }

    public synchronized void load() {
        try {
            Path path = storagePath;
            if (!Files.exists(path)) {
                return;
            }
            if (Files.size(path) > 65_536L) return;
            String content = Files.readString(path, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            validateNumbers(root);
            validateProfile(root);
            if (root.has("adaptiveLearning")) adaptiveLearning = root.get("adaptiveLearning").getAsBoolean();
            if (root.has("calibrated")) {
                calibrated = root.get("calibrated").getAsBoolean();
                state = calibrated ? ArcCalibrationState.READY : ArcCalibrationState.WAITING;
            }
            if (root.has("sampleCount")) {
                sampleCount = root.get("sampleCount").getAsInt();
            }
            if (root.has("calibrationSessionCount")) {
                calibrationSessionCount = root.get("calibrationSessionCount").getAsInt();
            }
            if (root.has("confidenceScore")) {
                confidenceScore = root.get("confidenceScore").getAsFloat();
            }
            if (root.has("masteryPercent")) {
                masteryPercent = root.get("masteryPercent").getAsInt();
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
            if (root.has("targetCalibrationDurationMs")) {
                targetCalibrationDurationMs = root.get("targetCalibrationDurationMs").getAsLong();
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
            if (root.has("successfulDrawTicks") && root.get("successfulDrawTicks").isJsonArray()) {
                successfulDrawTicks.clear();
                for (var elem : root.getAsJsonArray("successfulDrawTicks")) {
                    if (elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isNumber()) {
                        successfulDrawTicks.add(MathHelper.clamp(elem.getAsInt(), 3, 20));
                    }
                }
            }

            if (root.has("weights")) {
                JsonObject weightsObj = root.getAsJsonObject("weights");
                for (int i = 0; i < INPUT_SIZE; i++) {
                    for (int j = 0; j < HIDDEN_SIZE; j++) {
                        String key = "ih_" + i + "_" + j;
                        if (weightsObj.has(key)) {
                            weightsInputHidden[i][j] = MathHelper.clamp(weightsObj.get(key).getAsFloat(), -8.0f, 8.0f);
                        }
                    }
                }
                for (int j = 0; j < HIDDEN_SIZE; j++) {
                    for (int k = 0; k < OUTPUT_SIZE; k++) {
                        String key = "ho_" + j + "_" + k;
                        if (weightsObj.has(key)) {
                            weightsHiddenOutput[j][k] = MathHelper.clamp(weightsObj.get(key).getAsFloat(), -8.0f, 8.0f);
                        }
                    }
                }
            }
            loadBias(root, "biasHidden", biasHidden);
            loadBias(root, "biasOutput", biasOutput);
            saccadeSpeedMultiplier = MathHelper.clamp(saccadeSpeedMultiplier, 0.8f, 1.35f);
            curvatureBias = MathHelper.clamp(curvatureBias, 0.2f, 0.5f);
            tremorVolatility = MathHelper.clamp(tremorVolatility, 0.03f, 0.08f);
            twoPhaseRatio = MathHelper.clamp(twoPhaseRatio, 0.5f, 0.95f);
            microDamping = MathHelper.clamp(microDamping, 0.0f, 1.0f);
            learnedGcd = MathHelper.clamp(learnedGcd, 0.00001D, 10.0D);
            sampleCount = Math.max(0, sampleCount);
            learnedMinDelayMs = MathHelper.clamp(learnedMinDelayMs, 20, 180);
            learnedMaxDelayMs = MathHelper.clamp(learnedMaxDelayMs, learnedMinDelayMs, 200);
            learnedCameraSmoothness = MathHelper.clamp(learnedCameraSmoothness, 40, 180);
            learnedPlacementDelayRailMs = MathHelper.clamp(learnedPlacementDelayRailMs, 20, 200);
            learnedPlacementDelayCartMs = MathHelper.clamp(learnedPlacementDelayCartMs, 20, 200);
            bowShotsCount = Math.max(0, bowShotsCount);
            railsPlacedCount = Math.max(0, railsPlacedCount);
            cartsPlacedCount = Math.max(0, cartsPlacedCount);
            explosionsCount = Math.max(0, explosionsCount);
            manualDetonationsCount = Math.max(0, manualDetonationsCount);
            totalActionsCount = Math.max(0, totalActionsCount);
        } catch (Exception ignored) {}
    }

    private static void validateProfile(JsonObject root) {
        for (String key : new String[]{"sampleCount", "saccadeSpeedMultiplier", "curvatureBias",
                "tremorVolatility", "twoPhaseRatio", "microDamping", "learnedGcd", "learnedMinDelayMs",
                "learnedMaxDelayMs", "learnedCameraSmoothness", "learnedPlacementDelayRailMs",
                "learnedPlacementDelayCartMs", "bowShotsCount", "railsPlacedCount", "cartsPlacedCount",
                "explosionsCount", "manualDetonationsCount", "totalActionsCount",
                "calibrationSessionCount", "confidenceScore", "masteryPercent"}) {
            if (root.has(key)) requireNumber(root.get(key));
        }
        for (String key : new String[]{"calibrated", "adaptiveLearning"}) {
            if (root.has(key) && (!root.get(key).isJsonPrimitive()
                    || !root.getAsJsonPrimitive(key).isBoolean())) {
                throw new IllegalArgumentException("Invalid profile flag");
            }
        }
        if (root.has("successfulDrawTicks")) {
            if (!root.get("successfulDrawTicks").isJsonArray()) throw new IllegalArgumentException("Invalid profile draw ticks");
            for (var elem : root.getAsJsonArray("successfulDrawTicks")) requireNumber(elem);
        }
        validateBias(root, "biasHidden", HIDDEN_SIZE);
        validateBias(root, "biasOutput", OUTPUT_SIZE);
        if (root.has("weights")) {
            JsonObject weights = root.getAsJsonObject("weights");
            for (int i = 0; i < INPUT_SIZE; i++) {
                for (int j = 0; j < HIDDEN_SIZE; j++) {
                    String key = "ih_" + i + "_" + j;
                    if (weights.has(key)) requireNumber(weights.get(key));
                }
            }
            for (int j = 0; j < HIDDEN_SIZE; j++) {
                for (int k = 0; k < OUTPUT_SIZE; k++) {
                    String key = "ho_" + j + "_" + k;
                    if (weights.has(key)) requireNumber(weights.get(key));
                }
            }
        }
    }

    private static void validateBias(JsonObject root, String key, int size) {
        if (!root.has(key)) return;
        var values = root.getAsJsonArray(key);
        if (values.size() != size) throw new IllegalArgumentException("Invalid bias dimensions");
        for (var value : values) requireNumber(value);
    }

    private static void requireNumber(com.google.gson.JsonElement value) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()
                || !Float.isFinite(value.getAsFloat())) {
            throw new IllegalArgumentException("Invalid profile number");
        }
    }

    private static void validateNumbers(com.google.gson.JsonElement element) {
        validateNumbers(element, 0);
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

    private static void loadBias(JsonObject root, String key, float[] destination) {
        if (!root.has(key)) return;
        var values = root.getAsJsonArray(key);
        if (values.size() != destination.length) throw new IllegalArgumentException("Invalid bias dimensions");
        for (int i = 0; i < destination.length; i++) {
            destination[i] = MathHelper.clamp(values.get(i).getAsFloat(), -8.0f, 8.0f);
        }
    }

    private static final class TrainingSnapshot {
        private final float[][] input, output, inputMomentum, outputMomentum;
        private final float[] hiddenBias, outputBias;
        private final int samples;
        private final int[] actions;
        private final double gcd;
        private final boolean dirty;
        private final List<Integer> successfulTicks;
        private final ArcCalibrationState previousState;
        private final int previousMastery;
        private final int previousSessionCount;
        private final float previousConfidence;

        private TrainingSnapshot(ArcMotionProfile profile) {
            input = copy(profile.weightsInputHidden);
            output = copy(profile.weightsHiddenOutput);
            inputMomentum = copy(profile.momentumInputHidden);
            outputMomentum = copy(profile.momentumHiddenOutput);
            hiddenBias = profile.biasHidden.clone();
            outputBias = profile.biasOutput.clone();
            samples = profile.sampleCount;
            actions = new int[]{profile.bowShotsCount, profile.railsPlacedCount, profile.cartsPlacedCount,
                    profile.explosionsCount, profile.manualDetonationsCount, profile.totalActionsCount};
            gcd = profile.learnedGcd;
            dirty = profile.dirty;
            successfulTicks = new ArrayList<>(profile.successfulDrawTicks);
            previousState = profile.state;
            previousMastery = profile.masteryPercent;
            previousSessionCount = profile.calibrationSessionCount;
            previousConfidence = profile.confidenceScore;
        }

        private void restore(ArcMotionProfile profile) {
            restore(input, profile.weightsInputHidden);
            restore(output, profile.weightsHiddenOutput);
            restore(inputMomentum, profile.momentumInputHidden);
            restore(outputMomentum, profile.momentumHiddenOutput);
            System.arraycopy(hiddenBias, 0, profile.biasHidden, 0, hiddenBias.length);
            System.arraycopy(outputBias, 0, profile.biasOutput, 0, outputBias.length);
            profile.sampleCount = samples;
            profile.bowShotsCount = actions[0];
            profile.railsPlacedCount = actions[1];
            profile.cartsPlacedCount = actions[2];
            profile.explosionsCount = actions[3];
            profile.manualDetonationsCount = actions[4];
            profile.totalActionsCount = actions[5];
            profile.learnedGcd = gcd;
            profile.dirty = dirty;
            profile.successfulDrawTicks.clear();
            profile.successfulDrawTicks.addAll(successfulTicks);
            profile.state = previousState;
            profile.masteryPercent = previousMastery;
            profile.calibrationSessionCount = previousSessionCount;
            profile.confidenceScore = previousConfidence;
        }

        private static float[][] copy(float[][] source) {
            return java.util.Arrays.stream(source).map(float[]::clone).toArray(float[][]::new);
        }

        private static void restore(float[][] source, float[][] destination) {
            for (int i = 0; i < source.length; i++) System.arraycopy(source[i], 0, destination[i], 0, source[i].length);
        }
    }
}

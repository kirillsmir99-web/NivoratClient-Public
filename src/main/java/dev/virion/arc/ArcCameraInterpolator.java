package dev.virion.arc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

import java.util.concurrent.ThreadLocalRandom;

public final class ArcCameraInterpolator {
    private boolean active = false;
    private boolean returning = false;
    private float startPitch;
    private float targetPitch;
    private float startYaw;
    private float targetYaw;
    private long startTimeNs;
    private long durationNs;
    private float lastAppliedPitch;
    private float lastAppliedYaw;
    private double remainderPitch;
    private double remainderYaw;
    private float arcPitch;
    private float arcYaw;
    private float randomnessFactor = 0.35f;
    private boolean useGcd = true;
    private boolean twoPhaseAim = true;

    public void start(float fromPitch, float toPitch, float fromYaw, float toYaw, long durationMs, float randomness) {
        start(fromPitch, toPitch, fromYaw, toYaw, durationMs, randomness, 0.40f, true);
    }

    public void start(float fromPitch, float toPitch, float fromYaw, float toYaw, long durationMs, float randomness, float curve, boolean useMouseGcd) {
        this.returning = false;
        this.useGcd = useMouseGcd;
        this.startPitch = MathHelper.clamp(fromPitch, -90.0f, 90.0f);
        this.targetPitch = MathHelper.clamp(toPitch, -90.0f, 90.0f);
        this.startYaw = fromYaw;
        this.targetYaw = fromYaw + MathHelper.wrapDegrees(toYaw - fromYaw);
        this.randomnessFactor = MathHelper.clamp(randomness, 0.0f, 1.0f);
        float safeCurve = MathHelper.clamp(curve, 0.0f, 1.0f);

        ArcNeuralMotorProfile profile = ArcNeuralMotorProfile.getInstance();
        float speedMultiplier = profile.getSaccadeSpeedMultiplier();

        long varianceMs = (long) ((ThreadLocalRandom.current().nextDouble() - 0.5) * 20.0 * randomnessFactor);
        long baseDuration = Math.max(35L, Math.round(durationMs / Math.max(0.5f, speedMultiplier)));
        long effectiveMs = Math.max(35L, baseDuration + varianceMs);

        this.durationNs = effectiveMs * 1_000_000L;
        this.startTimeNs = System.nanoTime();
        this.lastAppliedPitch = this.startPitch;
        this.lastAppliedYaw = this.startYaw;
        this.remainderPitch = 0.0;
        this.remainderYaw = 0.0;
        this.twoPhaseAim = effectiveMs >= 75L;

        float lateralSign = MathHelper.wrapDegrees(toYaw - fromYaw) >= 0.0f ? 1.0f : -1.0f;
        float curvatureBase = profile.getCurvatureBias() * safeCurve;
        this.arcPitch = (float) ((ThreadLocalRandom.current().nextDouble() - 0.5) * 2.5 * curvatureBase);
        this.arcYaw = lateralSign * curvatureBase * 3.5f;

        this.active = true;
    }

    public void startReturn(float toPitch, float toYaw, long durationMs, float randomness) {
        startReturn(toPitch, toYaw, durationMs, randomness, 0.30f, true);
    }

    public void startReturn(float toPitch, float toYaw, long durationMs, float randomness, float curve, boolean useMouseGcd) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) {
            reset();
            return;
        }
        float curPitch = mc.player.getPitch();
        float curYaw = mc.player.getYaw();
        this.returning = true;
        this.useGcd = useMouseGcd;
        this.startPitch = MathHelper.clamp(curPitch, -90.0f, 90.0f);
        this.targetPitch = MathHelper.clamp(toPitch, -90.0f, 90.0f);
        this.startYaw = curYaw;
        this.targetYaw = curYaw + MathHelper.wrapDegrees(toYaw - curYaw);
        this.randomnessFactor = MathHelper.clamp(randomness, 0.0f, 1.0f);
        float safeCurve = MathHelper.clamp(curve, 0.0f, 1.0f);

        ArcNeuralMotorProfile profile = ArcNeuralMotorProfile.getInstance();
        float speedMultiplier = profile.getSaccadeSpeedMultiplier();

        long varianceMs = (long) ((ThreadLocalRandom.current().nextDouble() - 0.5) * 15.0 * randomnessFactor);
        long baseDuration = Math.max(30L, Math.round(durationMs / Math.max(0.5f, speedMultiplier)));
        long effectiveMs = Math.max(30L, baseDuration + varianceMs);

        this.durationNs = effectiveMs * 1_000_000L;
        this.startTimeNs = System.nanoTime();
        this.lastAppliedPitch = this.startPitch;
        this.lastAppliedYaw = this.startYaw;
        this.remainderPitch = 0.0;
        this.remainderYaw = 0.0;
        this.twoPhaseAim = effectiveMs >= 75L;

        float lateralSign = MathHelper.wrapDegrees(toYaw - curYaw) >= 0.0f ? 1.0f : -1.0f;
        float curvatureBase = profile.getCurvatureBias() * safeCurve * 0.75f;
        this.arcPitch = (float) ((ThreadLocalRandom.current().nextDouble() - 0.5) * 2.0 * curvatureBase);
        this.arcYaw = lateralSign * curvatureBase * 2.5f;

        this.active = true;
    }

    public void onRender(MinecraftClient client) {
        if (!active || client == null || client.player == null) {
            return;
        }

        long elapsedNs = System.nanoTime() - startTimeNs;
        double progress = (double) elapsedNs / (double) durationNs;

        if (progress >= 1.0) {
            applyRotation(client, targetPitch, targetYaw);
            active = false;
            returning = false;
            return;
        }

        double t = MathHelper.clamp(progress, 0.0, 1.0);
        ArcNeuralMotorProfile profile = ArcNeuralMotorProfile.getInstance();

        float totalYawDelta = Math.abs(MathHelper.wrapDegrees(targetYaw - startYaw));
        float totalPitchDelta = Math.abs(targetPitch - startPitch);
        float totalAngle = (float) Math.sqrt(totalYawDelta * totalYawDelta + totalPitchDelta * totalPitchDelta);
        float dirSign = MathHelper.wrapDegrees(targetYaw - startYaw) >= 0.0f ? 1.0f : -1.0f;

        float[] neuralOutputs = profile.forward((float) t, totalAngle, 20.0f, dirSign);
        float velMod = neuralOutputs[0];
        float curveMod = neuralOutputs[1];
        float tremorMod = neuralOutputs[2];

        double smooth;
        if (twoPhaseAim) {
            double splitPoint = profile.getTwoPhaseRatio();
            if (t < splitPoint) {
                double subT = t / splitPoint;
                smooth = minimumJerk(subT) * 0.90 * velMod;
            } else {
                double subT = (t - splitPoint) / (1.0 - splitPoint);
                smooth = 0.90 + minimumJerk(subT) * 0.10;
            }
        } else {
            smooth = minimumJerk(t) * velMod;
        }
        smooth = MathHelper.clamp(smooth, 0.0, 1.0);

        double arcWeight = Math.sin(t * Math.PI) * curveMod;
        float calculatedPitch = (float) (startPitch + (targetPitch - startPitch) * smooth + arcPitch * arcWeight);
        float calculatedYaw = (float) (startYaw + (targetYaw - startYaw) * smooth + arcYaw * arcWeight);

        if (randomnessFactor > 0.03f && t > 0.08 && t < 0.92) {
            float[] ouTremor = profile.getNextTremor(randomnessFactor * tremorMod);
            calculatedPitch += ouTremor[0];
            calculatedYaw += ouTremor[1];
        }

        applyRotation(client, calculatedPitch, calculatedYaw);
    }

    private static double minimumJerk(double t) {
        double clamped = MathHelper.clamp(t, 0.0, 1.0);
        return clamped * clamped * clamped * (clamped * (clamped * 6.0 - 15.0) + 10.0);
    }

    private void applyRotation(MinecraftClient client, float pitch, float yaw) {
        if (client.player == null) return;

        float finalPitch = MathHelper.clamp(pitch, -90.0f, 90.0f);
        float deltaYaw = MathHelper.wrapDegrees(yaw - lastAppliedYaw);
        float deltaPitch = finalPitch - lastAppliedPitch;
        float finalYaw;

        if (this.useGcd && client.options != null) {
            double gcd = calculateMouseGcd(client);
            if (gcd > 1.0E-5) {
                double accumYaw = deltaYaw + remainderYaw;
                double accumPitch = deltaPitch + remainderPitch;

                long yawSteps = Math.round(accumYaw / gcd);
                long pitchSteps = Math.round(accumPitch / gcd);

                remainderYaw = accumYaw - (yawSteps * gcd);
                remainderPitch = accumPitch - (pitchSteps * gcd);

                finalYaw = (float) (lastAppliedYaw + (yawSteps * gcd));
                finalPitch = (float) MathHelper.clamp(lastAppliedPitch + (pitchSteps * gcd), -90.0f, 90.0f);
            } else {
                finalYaw = lastAppliedYaw + deltaYaw;
            }
        } else {
            finalYaw = lastAppliedYaw + deltaYaw;
        }

        client.player.setPitch(finalPitch);
        client.player.setYaw(finalYaw);
        client.player.lastPitch = finalPitch;
        client.player.lastYaw = finalYaw;

        this.lastAppliedPitch = finalPitch;
        this.lastAppliedYaw = finalYaw;
    }

    public static double calculateMouseGcd(MinecraftClient client) {
        if (client == null || client.options == null) return 0.0015;
        double sens = client.options.getMouseSensitivity().getValue();
        double d = sens * 0.6000000238418579 + 0.20000000298023224;
        return d * d * d * 8.0 * 0.15;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isReturning() {
        return returning;
    }

    public void reset() {
        this.active = false;
        this.returning = false;
        this.remainderPitch = 0.0;
        this.remainderYaw = 0.0;
    }
}

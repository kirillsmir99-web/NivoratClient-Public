package dev.nivorat.arc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

import java.util.concurrent.ThreadLocalRandom;

public final class ArcCameraInterpolator {
    private static volatile boolean anyActive = false;
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

    public static boolean isAnyActive() {
        return anyActive;
    }

    public void start(float fromPitch, float toPitch, float fromYaw, float toYaw, long durationMs, float randomness) {
        start(fromPitch, toPitch, fromYaw, toYaw, durationMs, randomness, 0.40f, true);
    }

    public void start(float fromPitch, float toPitch, float fromYaw, float toYaw, long durationMs, float randomness, float curve, boolean useMouseGcd) {
        this.returning = false;
        this.useGcd = useMouseGcd;
        this.startPitch = MathHelper.clamp(fromPitch, -90.0f, 90.0f);
        boolean isAssisted = "assisted".equalsIgnoreCase(MorrowConfig.cameraMode);
        this.targetPitch = MathHelper.clamp(toPitch, -90.0f, 90.0f);
        this.targetYaw = fromYaw + MathHelper.wrapDegrees(toYaw - fromYaw);
        this.startYaw = fromYaw;
        this.randomnessFactor = MathHelper.clamp(randomness, 0.0f, 1.0f);
        float safeCurve = MathHelper.clamp(curve, 0.0f, 1.0f);

        ArcMotionProfile profile = ArcMotionProfile.getInstance();
        profile.resetTremor();
        float speedMultiplier = MorrowConfig.adaptiveAim ? profile.getSaccadeSpeedMultiplier() : 1.0f;

        float lateralSign = MathHelper.wrapDegrees(toYaw - fromYaw) >= 0.0f ? 1.0f : -1.0f;
        float deltaPitch = Math.abs(toPitch - fromPitch);
        float deltaYaw = Math.abs(MathHelper.wrapDegrees(toYaw - fromYaw));
        float totalDist = (float) Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
        double angleFactor = MathHelper.clamp(0.70 + (totalDist / 60.0) * 0.45, 0.65, 1.40);

        long varianceMs = (long) ((ThreadLocalRandom.current().nextDouble() - 0.5) * 16.0 * randomnessFactor);
        long minBaseDuration = isAssisted ? 130L : (durationMs <= 50L ? 35L : 45L);
        long baseDuration = Math.max(minBaseDuration, Math.round(durationMs * angleFactor / Math.max(0.5f, speedMultiplier)));
        long effectiveMs = Math.max(minBaseDuration, baseDuration + varianceMs);

        this.durationNs = effectiveMs * 1_000_000L;
        this.startTimeNs = System.nanoTime();
        this.lastAppliedPitch = this.startPitch;
        this.lastAppliedYaw = this.startYaw;
        this.remainderPitch = 0.0;
        this.remainderYaw = 0.0;
        this.twoPhaseAim = false;

        float curvatureBase = (durationMs <= 50L ? 0.08f : (MorrowConfig.adaptiveAim ? profile.getCurvatureBias() : 0.28f)) * safeCurve;
        this.arcPitch = (float) ((ThreadLocalRandom.current().nextDouble() - 0.5) * 0.25 * curvatureBase);
        this.arcYaw = lateralSign * curvatureBase * 2.8f;

        this.active = true;
        anyActive = true;
        MinecraftClient currentMc = MinecraftClient.getInstance();
        AutoCartLogger.logAimStart(fromPitch, targetPitch, fromYaw, targetYaw, effectiveMs, calculateMouseGcd(currentMc));
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
        this.targetYaw = curYaw + MathHelper.wrapDegrees(toYaw - curYaw);
        this.startYaw = curYaw;
        this.randomnessFactor = MathHelper.clamp(randomness, 0.0f, 1.0f);
        float safeCurve = MathHelper.clamp(curve, 0.0f, 1.0f);

        ArcMotionProfile profile = ArcMotionProfile.getInstance();
        profile.resetTremor();
        float speedMultiplier = MorrowConfig.adaptiveAim ? profile.getSaccadeSpeedMultiplier() : 1.0f;

        float deltaPitch = Math.abs(toPitch - curPitch);
        float deltaYaw = Math.abs(MathHelper.wrapDegrees(toYaw - curYaw));
        float totalDist = (float) Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
        double angleFactor = MathHelper.clamp(0.70 + (totalDist / 60.0) * 0.45, 0.65, 1.40);

        long varianceMs = (long) ((ThreadLocalRandom.current().nextDouble() - 0.5) * 12.0 * randomnessFactor);
        long minBaseDuration = durationMs <= 50L ? 35L : 40L;
        long baseDuration = Math.max(minBaseDuration, Math.round(durationMs * angleFactor / Math.max(0.5f, speedMultiplier)));
        long effectiveMs = Math.max(minBaseDuration, baseDuration + varianceMs);

        this.durationNs = effectiveMs * 1_000_000L;
        this.startTimeNs = System.nanoTime();
        this.lastAppliedPitch = this.startPitch;
        this.lastAppliedYaw = this.startYaw;
        this.remainderPitch = 0.0;
        this.remainderYaw = 0.0;
        this.twoPhaseAim = false;

        float lateralSign = MathHelper.wrapDegrees(toYaw - curYaw) >= 0.0f ? 1.0f : -1.0f;
        float curvatureBase = (durationMs <= 50L ? 0.06f : (MorrowConfig.adaptiveAim ? profile.getCurvatureBias() : 0.25f)) * safeCurve * 0.75f;
        this.arcPitch = (float) ((ThreadLocalRandom.current().nextDouble() - 0.5) * 0.2 * curvatureBase);
        this.arcYaw = lateralSign * curvatureBase * 2.2f;

        this.active = true;
        anyActive = true;
        AutoCartLogger.logAimStart(curPitch, targetPitch, curYaw, targetYaw, effectiveMs, calculateMouseGcd(mc));
    }

    public void onRender(MinecraftClient client) {
        if (!active || client == null || client.player == null) {
            return;
        }

        float curPitch = client.player.getPitch();
        float curYaw = client.player.getYaw();
        float manualDeltaPitch = curPitch - this.lastAppliedPitch;
        float manualDeltaYaw = MathHelper.wrapDegrees(curYaw - this.lastAppliedYaw);

        if (this.returning) {
            if (Math.abs(manualDeltaPitch) > 0.12f || Math.abs(manualDeltaYaw) > 0.12f) {
                reset();
                return;
            }
        } else {
            if (Math.abs(manualDeltaPitch) > 3.2f || Math.abs(manualDeltaYaw) > 3.2f) {
                reset();
                return;
            }
            if (Math.abs(manualDeltaPitch) > 0.001f || Math.abs(manualDeltaYaw) > 0.001f) {
                this.startPitch += manualDeltaPitch;
                this.targetPitch += manualDeltaPitch;
                this.startYaw += manualDeltaYaw;
                this.targetYaw += manualDeltaYaw;
            }
        }

        long elapsedNs = System.nanoTime() - startTimeNs;
        double progress = (double) elapsedNs / (double) durationNs;

        if (progress >= 1.0) {
            applyRotation(client, targetPitch, targetYaw);
            active = false;
            returning = false;
            anyActive = false;
            float err = Math.abs(client.player.getPitch() - targetPitch) + Math.abs(MathHelper.wrapDegrees(client.player.getYaw() - targetYaw));
            AutoCartLogger.logAimFinish(elapsedNs / 1_000_000L, err);
            return;
        }

        double t = MathHelper.clamp(progress, 0.0, 1.0);
        ArcMotionProfile profile = ArcMotionProfile.getInstance();

        float totalYawDelta = Math.abs(MathHelper.wrapDegrees(targetYaw - startYaw));
        float totalPitchDelta = Math.abs(targetPitch - startPitch);
        float totalAngle = (float) Math.sqrt(totalYawDelta * totalYawDelta + totalPitchDelta * totalPitchDelta);
        float dirSign = MathHelper.wrapDegrees(targetYaw - startYaw) >= 0.0f ? 1.0f : -1.0f;

        float velMod = 1.0f;
        float curveMod = 1.0f;
        float tremorMod = 1.0f;
        if (MorrowConfig.adaptiveAim) {
            float nominalVelocity = totalAngle / Math.max(0.001f, durationNs / 1_000_000_000f);
            float[] adaptiveOutputs = profile.forward((float) t, totalAngle, nominalVelocity, dirSign);
            velMod = adaptiveOutputs[0];
            curveMod = adaptiveOutputs[1];
            tremorMod = adaptiveOutputs[2];
        }

        float c = (velMod - 1.0f) * 0.25f;
        double warpedT = MathHelper.clamp(t + c * t * (1.0 - t), 0.0, 1.0);
        double smooth = minimumJerk(warpedT);

        double taper = Math.min(1.0, Math.sin((1.0 - t) * Math.PI * 0.5) * 2.2);
        double arcWeight = Math.sin(t * Math.PI) * taper * curveMod;
        float calculatedPitch = (float) (startPitch + (targetPitch - startPitch) * smooth + arcPitch * arcWeight);
        float calculatedYaw = (float) (startYaw + (targetYaw - startYaw) * smooth + arcYaw * arcWeight);

        if (randomnessFactor > 0.03f && MorrowConfig.adaptiveAim) {
            float[] ouTremor = profile.getNextTremor(randomnessFactor * tremorMod);
            double envelope = Math.sin(t * Math.PI) * taper;
            calculatedPitch += (float) (ouTremor[0] * envelope);
            calculatedYaw += (float) (ouTremor[1] * envelope);
        }

        applyRotation(client, calculatedPitch, calculatedYaw);
    }

    public void holdAimWithMicroTremor(MinecraftClient client, float pitch, float yaw) {
        if (client == null || client.player == null || active) return;
        ArcMotionProfile profile = ArcMotionProfile.getInstance();
        float[] ou = profile.getNextTremor(0.02f);
        float curPitch = client.player.getPitch();
        float curYaw = client.player.getYaw();
        applyRotation(client, MathHelper.clamp(curPitch + ou[0] * 0.02f, -90.0f, 90.0f), curYaw + ou[1] * 0.02f);
    }

    private static double minimumJerk(double t) {
        double clamped = MathHelper.clamp(t, 0.0, 1.0);
        return clamped * clamped * clamped * (clamped * (clamped * 6.0 - 15.0) + 10.0);
    }

    private void applyRotation(MinecraftClient client, float pitch, float yaw) {
        if (client.player == null) return;

        float finalPitch = MathHelper.clamp(pitch, -90.0f, 90.0f);
        float deltaYaw = MathHelper.wrapDegrees(yaw - startYaw);
        float deltaPitch = finalPitch - startPitch;
        float finalYaw;

        if (this.useGcd && client.options != null) {
            double gcd = calculateMouseGcd(client);
            if (gcd > 1.0E-5) {
                finalYaw = quantizeAngle(startYaw, startYaw + deltaYaw, gcd);
                finalPitch = MathHelper.clamp(quantizeAngle(startPitch, startPitch + deltaPitch, gcd), -90.0f, 90.0f);
            } else {
                finalYaw = startYaw + deltaYaw;
            }
        } else {
            finalYaw = startYaw + deltaYaw;
        }

        client.player.setPitch(finalPitch);
        client.player.setYaw(finalYaw);

        this.lastAppliedPitch = finalPitch;
        this.lastAppliedYaw = finalYaw;
    }

    static float quantizeAngle(float origin, float desired, double step) {
        if (!Float.isFinite(origin) || !Float.isFinite(desired) || !Double.isFinite(step) || step <= 0.0) return origin;
        return (float) (origin + Math.round((desired - origin) / step) * step);
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
        anyActive = false;
    }
}

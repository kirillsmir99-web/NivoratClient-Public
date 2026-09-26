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
    private float arcPitch;
    private float arcYaw;
    private float randomnessFactor = 0.35f;
    private boolean useGcd = true;

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

        long varianceMs = (long) ((ThreadLocalRandom.current().nextDouble() - 0.5) * 30.0 * randomnessFactor);
        long effectiveMs = Math.max(40L, durationMs + varianceMs);
        this.durationNs = effectiveMs * 1_000_000L;
        this.startTimeNs = System.nanoTime();
        this.lastAppliedPitch = this.startPitch;
        this.lastAppliedYaw = this.startYaw;

        this.arcPitch = (float) ((ThreadLocalRandom.current().nextDouble() - 0.5) * 3.0 * safeCurve);
        this.arcYaw = (float) ((ThreadLocalRandom.current().nextDouble() - 0.5) * 4.0 * safeCurve);
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

        long varianceMs = (long) ((ThreadLocalRandom.current().nextDouble() - 0.5) * 20.0 * randomnessFactor);
        long effectiveMs = Math.max(30L, durationMs + varianceMs);
        this.durationNs = effectiveMs * 1_000_000L;
        this.startTimeNs = System.nanoTime();
        this.lastAppliedPitch = this.startPitch;
        this.lastAppliedYaw = this.startYaw;

        this.arcPitch = (float) ((ThreadLocalRandom.current().nextDouble() - 0.5) * 2.0 * safeCurve);
        this.arcYaw = (float) ((ThreadLocalRandom.current().nextDouble() - 0.5) * 3.0 * safeCurve);
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

        double t = Math.max(0.0, Math.min(1.0, progress));
        double smooth = t * t * t * (t * (t * 6.0 - 15.0) + 10.0);

        double arcWeight = Math.sin(t * Math.PI);
        float calculatedPitch = (float) (startPitch + (targetPitch - startPitch) * smooth + arcPitch * arcWeight);
        float calculatedYaw = (float) (startYaw + (targetYaw - startYaw) * smooth + arcYaw * arcWeight);

        if (randomnessFactor > 0.05f && t > 0.1 && t < 0.9) {
            float tremorPitch = (float) ((ThreadLocalRandom.current().nextDouble() - 0.5) * 0.08 * randomnessFactor);
            float tremorYaw = (float) ((ThreadLocalRandom.current().nextDouble() - 0.5) * 0.12 * randomnessFactor);
            calculatedPitch += tremorPitch;
            calculatedYaw += tremorYaw;
        }

        applyRotation(client, calculatedPitch, calculatedYaw);
    }

    private void applyRotation(MinecraftClient client, float pitch, float yaw) {
        if (client.player == null) return;

        float finalPitch = MathHelper.clamp(pitch, -90.0f, 90.0f);
        float deltaYaw = MathHelper.wrapDegrees(yaw - lastAppliedYaw);
        float finalYaw = lastAppliedYaw + deltaYaw;

        if (this.useGcd && client.options != null) {
            double gcd = calculateMouseGcd(client);
            if (gcd > 1.0E-5) {
                float deltaPitch = finalPitch - lastAppliedPitch;
                long pitchSteps = Math.round(deltaPitch / gcd);
                long yawSteps = Math.round(deltaYaw / gcd);

                finalPitch = (float) (lastAppliedPitch + (pitchSteps * gcd));
                finalYaw = (float) (lastAppliedYaw + (yawSteps * gcd));
                finalPitch = MathHelper.clamp(finalPitch, -90.0f, 90.0f);
            }
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
    }
}

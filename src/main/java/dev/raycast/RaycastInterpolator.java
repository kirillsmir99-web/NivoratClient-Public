package dev.raycast;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

public final class RaycastInterpolator {
    private boolean active = false;
    private float startPitch;
    private float targetPitch;
    private float startYaw;
    private float targetYaw;
    private long startTimeNs;
    private long durationNs;
    private float lastAppliedPitch;
    private float lastAppliedYaw;
    private boolean gcdEnabled = true;

    public void start(float fromPitch, float toPitch, float fromYaw, float toYaw, long durationMs) {
        start(fromPitch, toPitch, fromYaw, toYaw, durationMs, true);
    }

    public void start(float fromPitch, float toPitch, float fromYaw, float toYaw, long durationMs, boolean useGcd) {
        this.startPitch = MathHelper.clamp(fromPitch, -90.0f, 90.0f);
        this.targetPitch = MathHelper.clamp(toPitch, -90.0f, 90.0f);
        this.startYaw = fromYaw;
        this.targetYaw = fromYaw + MathHelper.wrapDegrees(toYaw - fromYaw);
        this.durationNs = Math.max(20_000_000L, durationMs * 1_000_000L);
        this.startTimeNs = System.nanoTime();
        this.lastAppliedPitch = this.startPitch;
        this.lastAppliedYaw = this.startYaw;
        this.gcdEnabled = useGcd;
        this.active = true;
    }

    public void onRender(MinecraftClient client) {
        if (!active || client == null || client.player == null) {
            return;
        }

        long elapsedNs = System.nanoTime() - startTimeNs;
        double progress = (double) elapsedNs / (double) durationNs;

        if (progress >= 1.0) {
            applyRotation(client, targetPitch, targetYaw, gcdEnabled);
            active = false;
            return;
        }

        double t = Math.max(0.0, Math.min(1.0, progress));

        double smooth = t * t * t * (t * (t * 6.0 - 15.0) + 10.0);

        float calculatedPitch = (float) (startPitch + (targetPitch - startPitch) * smooth);
        float calculatedYaw = (float) (startYaw + (targetYaw - startYaw) * smooth);

        applyRotation(client, calculatedPitch, calculatedYaw, gcdEnabled);
    }

    private void applyRotation(MinecraftClient client, float pitch, float yaw, boolean useGcd) {
        if (client.player == null) return;

        float finalPitch = MathHelper.clamp(pitch, -90.0f, 90.0f);
        float deltaYaw = MathHelper.wrapDegrees(yaw - lastAppliedYaw);
        float finalYaw = lastAppliedYaw + deltaYaw;

        if (useGcd && client.options != null) {
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

    public void setGcdEnabled(boolean enabled) {
        this.gcdEnabled = enabled;
    }

    public void finalizeInterpolation(MinecraftClient client, float finalPitch, float finalYaw) {
        this.active = false;
        if (client != null && client.player != null) {
            float clampedPitch = MathHelper.clamp(finalPitch, -90.0f, 90.0f);
            float deltaYaw = MathHelper.wrapDegrees(finalYaw - client.player.getYaw());
            float targetQuantizedYaw = client.player.getYaw() + deltaYaw;

            if (gcdEnabled && client.options != null) {
                double gcd = calculateMouseGcd(client);
                if (gcd > 1.0E-5) {
                    float deltaPitch = clampedPitch - client.player.getPitch();
                    long pitchSteps = Math.round(deltaPitch / gcd);
                    long yawSteps = Math.round(deltaYaw / gcd);

                    clampedPitch = (float) (client.player.getPitch() + (pitchSteps * gcd));
                    targetQuantizedYaw = (float) (client.player.getYaw() + (yawSteps * gcd));
                    clampedPitch = MathHelper.clamp(clampedPitch, -90.0f, 90.0f);
                }
            }

            client.player.setPitch(clampedPitch);
            client.player.setYaw(targetQuantizedYaw);
            client.player.lastPitch = clampedPitch;
            client.player.lastYaw = targetQuantizedYaw;
            this.lastAppliedPitch = clampedPitch;
            this.lastAppliedYaw = targetQuantizedYaw;
        }
    }

    public void reset() {
        this.active = false;
    }
}

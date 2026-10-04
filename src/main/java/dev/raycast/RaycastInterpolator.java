package dev.raycast;

import dev.raycast.internal.RaycastDomain;
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

        double smooth = evaluateSmootherstep(t);

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

        this.lastAppliedPitch = finalPitch;
        this.lastAppliedYaw = finalYaw;
    }

    private static double evaluateSmootherstep(double t) {
        double c6 = RaycastDomain.d(9007934327105682714L) ;
        double c15 = RaycastDomain.d(9022008075941215514L) ;
        double c10 = RaycastDomain.d(9024822825708322074L) ;
        return t * t * t * (t * (t * c6 - c15) + c10);
    }

    public static double calculateMouseGcd(MinecraftClient client) {
        if (client == null || client.options == null) return RaycastDomain.d(162710128242721760L) ;
        double sens = client.options.getMouseSensitivity().getValue();
        double d = sens * RaycastDomain.d(214395663899512361L)  + RaycastDomain.d(203511986363294848L) ;
        return d * d * d * RaycastDomain.d(9023696925801479450L)  * RaycastDomain.d(205388464644771369L) ;
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

            this.lastAppliedPitch = clampedPitch;
            this.lastAppliedYaw = targetQuantizedYaw;
        }
    }

    public void reset() {
        this.active = false;
    }
}

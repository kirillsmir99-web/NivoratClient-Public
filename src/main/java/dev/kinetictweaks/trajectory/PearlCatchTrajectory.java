package dev.kinetictweaks.trajectory;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class PearlCatchTrajectory {
    public static final double PEARL_SPEED = 1.5;
    public static final double PEARL_GRAVITY = 0.03;
    public static final double PEARL_DRAG = 0.99;
    public static final double WIND_CHARGE_SPEED = 1.5;
    public static final double BURST_OFFSET_Y = 0.38; // Aim slightly below pearl so wind burst pushes pearl forward and upward

    private PearlCatchTrajectory() {}

    public record Solution(
            float pearlPitch,
            float windPitch,
            float windYaw,
            float computedOffset,
            int interceptTick,
            double residualError,
            boolean valid
    ) {
        public float pitchOffset() {
            return computedOffset;
        }

        public static Solution fallback(float basePearlPitch, float offset) {
            return new Solution(basePearlPitch, basePearlPitch + offset, 0.0f, offset, 4, 0.0, true);
        }
    }

    /**
     * Solves for the 3D trajectory intercept between Ender Pearl and Wind Charge.
     * Takes into account 3D player velocity (including vertical motion from jump/fall/knockback),
     * gravity, air drag, and throw delay.
     *
     * @param delayTicks Delay in ticks between pearl throw and wind charge release
     * @param playerYaw Current player yaw in degrees
     * @param playerVel Current player velocity vector
     * @param onGround Whether player is on ground (affects initial velocity addition in vanilla)
     * @param customOffset Optional manual offset override (or <= 0 to compute dynamically)
     * @return Fully resolved Solution with optimal pearl pitch and required wind charge angles
     */
    public static Solution solve3D(
            int delayTicks,
            float playerYaw,
            Vec3d playerVel,
            boolean onGround,
            float customOffset
    ) {
        int safeDelay = Math.max(1, Math.min(5, delayTicks));
        Vec3d safeVel = playerVel != null ? playerVel : Vec3d.ZERO;
        double horizontalSpeed = Math.hypot(safeVel.x, safeVel.z);
        double verticalSpeed = onGround ? 0.0 : safeVel.y;
        Vec3d inheritedPearlVel = new Vec3d(safeVel.x, verticalSpeed, safeVel.z);
        Vec3d inheritedWindVel = new Vec3d(safeVel.x, verticalSpeed, safeVel.z);

        // Optimal base pearl pitch
        float nominalPearlPitch = calculateOptimalPearlPitch(safeDelay, safeVel, horizontalSpeed > 0.18);

        // Player displacement over delayTicks with player physics (gravity 0.08, drag 0.98)
        double plY = 0.0;
        double curVy = verticalSpeed;
        for (int d = 0; d < safeDelay; d++) {
            plY += curVy;
            curVy = (curVy - 0.08) * 0.98;
        }
        Vec3d pearlOrigin = Vec3d.ZERO;
        Vec3d windOrigin = new Vec3d(safeVel.x * safeDelay, plY, safeVel.z * safeDelay);

        // Test candidate pearl pitches around the nominal pitch
        float[] candidatePitches = new float[]{
                nominalPearlPitch,
                nominalPearlPitch - 1.0f,
                nominalPearlPitch + 1.0f,
                nominalPearlPitch - 2.0f,
                nominalPearlPitch + 2.0f
        };

        float bestPearlPitch = nominalPearlPitch;
        int bestTick = safeDelay + 3;
        Vec3d bestNeededVel = Vec3d.ZERO;
        Vec3d bestAimTarget = Vec3d.ZERO;
        double minSpeedError = Double.MAX_VALUE;
        boolean foundSolution = false;

        for (float testPearlPitch : candidatePitches) {
            Vec3d pearlDir = getDirectionVector(testPearlPitch, playerYaw);
            Vec3d pearlVel0 = pearlDir.multiply(PEARL_SPEED).add(inheritedPearlVel);

            // Simulate pearl tick by tick
            Vec3d pPos = pearlOrigin;
            Vec3d pVel = pearlVel0;

            int maxTicks = Math.min(30, safeDelay + 20);
            for (int t = 1; t <= maxTicks; t++) {
                pPos = pPos.add(pVel);
                pVel = new Vec3d(pVel.x * PEARL_DRAG, (pVel.y - PEARL_GRAVITY) * PEARL_DRAG, pVel.z * PEARL_DRAG);

                if (t <= safeDelay) {
                    continue;
                }

                double flightTicks = t - safeDelay;
                if (flightTicks < 1.0) continue;

                // Aim slightly below the pearl so the wind explosion pushes the pearl up and forward
                Vec3d aimTarget = pPos.subtract(0.0, BURST_OFFSET_Y, 0.0);
                Vec3d neededVel = aimTarget.subtract(windOrigin).multiply(1.0 / flightTicks).subtract(inheritedWindVel);
                double neededSpeed = neededVel.length();

                double speedError = Math.abs(neededSpeed - WIND_CHARGE_SPEED);
                if (speedError < minSpeedError) {
                    minSpeedError = speedError;
                    bestPearlPitch = testPearlPitch;
                    bestTick = t;
                    bestNeededVel = neededVel;
                    bestAimTarget = aimTarget;

                    if (speedError < 0.08) {
                        foundSolution = true;
                    }
                }
            }
        }

        // Derive bestWindPitch directly from bestNeededVel using exact kinematic trigonometry
        double hypotXZ = Math.hypot(bestNeededVel.x, bestNeededVel.z);
        float bestWindPitch = - (float) Math.toDegrees(Math.atan2(bestNeededVel.y, hypotXZ));
        if (customOffset > 0.0f) {
            bestWindPitch += (customOffset - 8.0f);
        }

        float bestWindYaw = (float) Math.toDegrees(Math.atan2(-bestNeededVel.x, bestNeededVel.z));

        // Calculate actual physical wind charge position at bestTick to determine exact residual Euclidean error
        double flightTicks = bestTick - safeDelay;
        Vec3d actualWindDir = getDirectionVector(bestWindPitch, bestWindYaw);
        Vec3d actualWindVel = actualWindDir.multiply(WIND_CHARGE_SPEED).add(inheritedWindVel);
        Vec3d actualWindPos = windOrigin.add(actualWindVel.multiply(flightTicks));

        // Accurate Euclidean distance in blocks to the aim target
        double residualDist = actualWindPos.distanceTo(bestAimTarget);

        float computedOffset = bestWindPitch - bestPearlPitch;

        return new Solution(
                bestPearlPitch,
                bestWindPitch,
                bestWindYaw,
                computedOffset,
                bestTick,
                residualDist,
                foundSolution || residualDist <= 0.5
        );
    }

    public static Vec3d getDirectionVector(float pitch, float yaw) {
        float f = pitch * 0.017453292F;
        float g = -yaw * 0.017453292F;
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d(i * j, -k, h * j);
    }

    /**
     * Helper for basic pitch calculation.
     */
    public static float calculateOptimalPearlPitch(int delayTicks, Vec3d velocity, boolean sprinting) {
        double horizontalSpeed = velocity != null ? Math.hypot(velocity.x, velocity.z) : 0.0;

        float basePitch;
        switch (delayTicks) {
            case 1 -> basePitch = -26.5f;
            case 3 -> basePitch = -29.5f;
            case 4 -> basePitch = -31.0f;
            case 2 -> basePitch = -28.0f;
            default -> basePitch = -28.0f;
        }

        if (sprinting || horizontalSpeed > 0.18) {
            basePitch += 1.0f; // Flatter launch when running: -27.0f for delay 2
        }

        return MathHelper.clamp(basePitch, -45.0f, -15.0f);
    }

    /**
     * Helper for wind charge offset.
     */
    public static float calculateWindChargePitchOffset(int delayTicks) {
        return switch (delayTicks) {
            case 1 -> 5.5f;
            case 3 -> 10.0f;
            case 4 -> 12.0f;
            case 2 -> 8.0f;
            default -> 8.0f;
        };
    }
}

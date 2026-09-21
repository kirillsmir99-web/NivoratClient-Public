package dev.kinetictweaks.controller;

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
     * @return Fully resolved InterceptSolution with optimal pearl pitch and required wind charge angles
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

        // Player vertical displacement and velocity decay over delayTicks
        double plY = 0.0;
        double curVy = onGround ? 0.0 : safeVel.y;
        for (int i = 0; i < safeDelay; i++) {
            plY += curVy;
            curVy = (curVy - 0.08) * 0.98;
        }

        // Exact spawn geometry: pearl origin y = -0.1 relative to player eye, wind charge origin y = 0.0 relative to player eye.
        Vec3d pearlOrigin = new Vec3d(0.0, -0.1, 0.0);
        Vec3d windOrigin = new Vec3d(safeVel.x * safeDelay, plY, safeVel.z * safeDelay);

        // Inherited wind velocity at tick delay: horizontal = (safeVel.x, safeVel.z), vertical = onGround ? 0.0 : curVy.
        Vec3d inheritedPearlVel = new Vec3d(safeVel.x, onGround ? 0.0 : safeVel.y, safeVel.z);
        Vec3d inheritedWindVel = new Vec3d(safeVel.x, onGround ? 0.0 : curVy, safeVel.z);

        float bestPearlPitch = calculateOptimalPearlPitch(safeDelay, safeVel, Math.hypot(safeVel.x, safeVel.z) > 0.18);
        float bestWindPitch = bestPearlPitch;
        float bestWindYaw = playerYaw;
        int bestTick = safeDelay + 14;
        double minResidualError = Double.MAX_VALUE;

        // Sweep pearl pitch over realistic horizontal launch window [-35.0f, -12.0f]
        for (int pInt = -350; pInt <= -120; pInt += 2) {
            float testPearlPitch = pInt / 10.0f;
            Vec3d pearlDir = getDirectionVector(testPearlPitch, playerYaw);
            Vec3d pearlVel0 = pearlDir.multiply(PEARL_SPEED).add(inheritedPearlVel);

            // Compute pearl position using exact damped Euler integration (pearl speed 1.5, drag 0.99, gravity 0.03)
            Vec3d pPos = pearlOrigin;
            Vec3d pVel = pearlVel0;

            int maxT = safeDelay + 36;
            for (int t = 1; t <= maxT; t++) {
                pPos = pPos.add(pVel);
                pVel = new Vec3d(pVel.x * PEARL_DRAG, pVel.y * PEARL_DRAG - PEARL_GRAVITY, pVel.z * PEARL_DRAG);

                if (t <= safeDelay) {
                    continue;
                }

                double flightTicks = t - safeDelay;
                if (flightTicks < 2.0) {
                    continue;
                }

                Vec3d aimTarget = pPos.subtract(0.0, BURST_OFFSET_Y, 0.0);
                Vec3d neededVel = aimTarget.subtract(windOrigin).multiply(1.0 / flightTicks).subtract(inheritedWindVel);
                double neededSpeed = neededVel.length();
                if (neededSpeed < 1.0E-6) {
                    continue;
                }

                Vec3d dir = neededVel.normalize();
                float candWindYaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
                float candWindPitch = (float) Math.toDegrees(Math.atan2(-dir.y, Math.hypot(dir.x, dir.z)));

                // Compute residualError using actual wind charge flight vector (direction from bestWindPitch/bestWindYaw * 1.5 + inheritedWindVel)
                Vec3d candWindDir = getDirectionVector(candWindPitch, candWindYaw);
                Vec3d actualWindVel = candWindDir.multiply(WIND_CHARGE_SPEED).add(inheritedWindVel);
                Vec3d actualWindPos = windOrigin.add(actualWindVel.multiply(flightTicks));
                double residualError = actualWindPos.distanceTo(aimTarget);

                if (residualError < minResidualError) {
                    minResidualError = residualError;
                    bestPearlPitch = testPearlPitch;
                    bestWindPitch = candWindPitch;
                    bestWindYaw = candWindYaw;
                    bestTick = t;
                }
            }
        }

        float computedOffset = bestWindPitch - bestPearlPitch;
        boolean valid = minResidualError <= 0.5;

        return new Solution(
                bestPearlPitch,
                bestWindPitch,
                bestWindYaw,
                computedOffset,
                bestTick,
                minResidualError,
                valid
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
     * Backward-compatible helper for basic pitch calculation.
     */
    public static float calculateOptimalPearlPitch(int delayTicks, Vec3d velocity, boolean sprinting) {
        double horizontalSpeed = velocity != null ? Math.hypot(velocity.x, velocity.z) : 0.0;
        double verticalSpeed = velocity != null ? velocity.y : 0.0;

        float basePitch;
        switch (delayTicks) {
            case 1 -> basePitch = -17.5f;
            case 3 -> basePitch = -19.5f;
            case 4 -> basePitch = -20.5f;
            case 2 -> basePitch = -18.5f;
            default -> basePitch = -18.5f;
        }

        if (sprinting || horizontalSpeed > 0.18) {
            basePitch += 1.0f; // Flatter launch when running
        }

        // Vertical velocity compensation:
        // When jumping / wind jumping upward (verticalSpeed > 0), the pearl inherits player upward velocity.
        // To prevent the pearl from rocketing too high, launch angle should be flatter (add positive pitch).
        // When falling (verticalSpeed < 0), pearl inherits downward velocity, so pitch up (subtract pitch).
        if (verticalSpeed > 0.05) {
            basePitch += (float) (Math.min(verticalSpeed, 1.5) * 2.0);
        } else if (verticalSpeed < -0.05) {
            basePitch -= (float) (Math.min(Math.abs(verticalSpeed), 1.5) * 2.0);
        }

        return MathHelper.clamp(basePitch, -35.0f, -10.0f);
    }

    /**
     * Backward-compatible helper for wind charge offset.
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

package dev.raycast;

import activity.client.util.Obf;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class RaycastTrajectory {
    public static final double PEARL_SPEED = dev.raycast.internal.RaycastDomain.d(207900655223733530L);
    public static final double PEARL_GRAVITY = dev.raycast.internal.RaycastDomain.d(181310406754263970L);
    public static final double PEARL_DRAG = dev.raycast.internal.RaycastDomain.d(213132216485493428L);
    public static final double WIND_CHARGE_SPEED = dev.raycast.internal.RaycastDomain.d(207900655223733530L);
    public static final double BURST_OFFSET_Y = dev.raycast.internal.RaycastDomain.d(198947149418906952L);

    private RaycastTrajectory() {}

    public static Vec3d advancePearlVelocity(Vec3d velocity) {
        return new Vec3d(velocity.x * PEARL_DRAG, (velocity.y - PEARL_GRAVITY) * PEARL_DRAG, velocity.z * PEARL_DRAG);
    }

    public static Vec3d pearlPosition(Vec3d origin, Vec3d velocity, double ticks) {
        int whole = Math.max(0, (int) Math.floor(ticks));
        double power = Math.pow(PEARL_DRAG, whole);
        double sum = PEARL_DRAG * (1 - power) / (1 - PEARL_DRAG);
        double gravity = PEARL_GRAVITY * PEARL_DRAG / (1 - PEARL_DRAG);
        double fraction = Math.max(0, ticks - whole);
        double nextPower = power * PEARL_DRAG;
        return origin.add(velocity.x * (sum + fraction * nextPower),
                velocity.y * sum - gravity * (whole - sum) + fraction * ((velocity.y + gravity) * nextPower - gravity),
                velocity.z * (sum + fraction * nextPower));
    }

    public static Solution solveIntercept(Vec3d pearlOrigin, Vec3d pearlVelocity, int pearlAge,
                                          Vec3d windOrigin, Vec3d inheritedWindVelocity) {
        double dtStep = dev.raycast.internal.RaycastDomain.d(201145255782677786L);
        double previousTime = dtStep;
        double previousError = speedError(pearlOrigin, pearlVelocity, pearlAge, windOrigin, inheritedWindVelocity, previousTime);
        double bestTime = previousTime;
        double bestError = Math.abs(previousError);
        double maxSearchTime = dev.raycast.internal.RaycastDomain.d(9017504476313845018L);
        for (double time = dev.raycast.internal.RaycastDomain.d(214656054664789274L); time <= maxSearchTime; time += dtStep) {
            double error = speedError(pearlOrigin, pearlVelocity, pearlAge, windOrigin, inheritedWindVelocity, time);
            if (previousError * error <= 0) {
                double lo = previousTime, hi = time;
                int maxIterations = dev.raycast.internal.RaycastDomain.i(1817922085);
                for (int iteration = 0; iteration < maxIterations; iteration++) {
                    double mid = (lo + hi) * dev.raycast.internal.RaycastDomain.d(214656054664789274L);
                    double midError = speedError(pearlOrigin, pearlVelocity, pearlAge, windOrigin, inheritedWindVelocity, mid);
                    if (previousError * midError <= 0) hi = mid;
                    else { lo = mid; previousError = midError; }
                }
                bestTime = (lo + hi) * dev.raycast.internal.RaycastDomain.d(214656054664789274L);
                bestError = 0;
                break;
            }
            if (Math.abs(error) < bestError) { bestError = Math.abs(error); bestTime = time; }
            previousError = error;
            previousTime = time;
        }
        Vec3d target = pearlPosition(pearlOrigin, pearlVelocity, pearlAge + bestTime);
        Vec3d needed = target.subtract(windOrigin).multiply(1 / bestTime).subtract(inheritedWindVelocity);
        float pitch = -(float) Math.toDegrees(Math.atan2(needed.y, Math.hypot(needed.x, needed.z)));
        float yaw = (float) Math.toDegrees(Math.atan2(-needed.x, needed.z));
        double residual = bestError * bestTime;
        return new Solution(0, pitch, yaw, 0, (int) Math.ceil(pearlAge + bestTime), residual,
                Double.isFinite(residual) && residual <= .3 && needed.lengthSquared() > 1e-8);
    }

    private static double speedError(Vec3d origin, Vec3d velocity, int age, Vec3d windOrigin, Vec3d inherited, double time) {
        return pearlPosition(origin, velocity, age + time).subtract(windOrigin).multiply(1 / time).subtract(inherited).length() - WIND_CHARGE_SPEED;
    }

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

        float nominalPearlPitch = calculateOptimalPearlPitch(safeDelay, safeVel, horizontalSpeed > 0.18);

        double plY = 0.0;
        double curVy = verticalSpeed;
        for (int d = 0; d < safeDelay; d++) {
            plY += curVy;
            curVy = (curVy - 0.08) * 0.98;
        }
        Vec3d pearlOrigin = Vec3d.ZERO;
        Vec3d windOrigin = new Vec3d(safeVel.x * safeDelay, plY, safeVel.z * safeDelay);

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

        double hypotXZ = Math.hypot(bestNeededVel.x, bestNeededVel.z);
        float bestWindPitch = - (float) Math.toDegrees(Math.atan2(bestNeededVel.y, hypotXZ));
        if (customOffset > 0.0f) {
            bestWindPitch += (customOffset - 8.0f);
        }

        float bestWindYaw = (float) Math.toDegrees(Math.atan2(-bestNeededVel.x, bestNeededVel.z));

        double flightTicks = bestTick - safeDelay;
        Vec3d actualWindDir = getDirectionVector(bestWindPitch, bestWindYaw);
        Vec3d actualWindVel = actualWindDir.multiply(WIND_CHARGE_SPEED).add(inheritedWindVel);
        Vec3d actualWindPos = windOrigin.add(actualWindVel.multiply(flightTicks));

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
            basePitch += 1.0f;
        }

        return MathHelper.clamp(basePitch, -45.0f, -15.0f);
    }

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

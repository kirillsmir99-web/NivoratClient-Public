package activity.client.module;

import dev.raycast.RaycastTrajectory;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PearlInterceptTest {
    private Vec3d independentlyAdvance(Vec3d origin, Vec3d velocity, double time) {
        int ticks = (int) time;
        for (int i = 0; i < ticks; i++) {
            velocity = new Vec3d(velocity.x * .99, (velocity.y - .03) * .99, velocity.z * .99);
            origin = origin.add(velocity);
        }
        Vec3d next = new Vec3d(velocity.x * .99, (velocity.y - .03) * .99, velocity.z * .99);
        return origin.add(next.multiply(time - ticks));
    }
    @Test void movingAndAirborneThrowsHaveActualFlightIntersections() {
        int valid = 0;
        for (float pitch : new float[]{-89.5f, -28f}) for (double vy : new double[]{0, .42, -.6}) for (double speed : new double[]{0, .28, .45}) for (int delay : new int[]{1, 2, 4}) {
            Vec3d movement = new Vec3d(speed, vy, .08);
            Vec3d origin = new Vec3d(0, 65.52, 0);
            Vec3d velocity = RaycastTrajectory.getDirectionVector(pitch, 17).multiply(1.5).add(movement);
            Vec3d wind = new Vec3d(speed * delay, 65.62 + vy * delay, .08 * delay);
            var solution = RaycastTrajectory.solveIntercept(origin, velocity, delay, wind, movement);
            if (!solution.valid()) continue;
            valid++;
            Vec3d windVelocity = RaycastTrajectory.getDirectionVector(solution.windPitch(), solution.windYaw()).multiply(1.5).add(movement);
            double best = Double.MAX_VALUE;
            for (double t = .25; t <= 30; t += .01) best = Math.min(best, independentlyAdvance(origin, velocity, delay + t).distanceTo(wind.add(windVelocity.multiply(t))));
            assertTrue(best < .31, "Missed moving/airborne interception: " + best);
        }
        assertTrue(valid >= 45, "Too few usable intersections: " + valid);
    }
    @Test void firstTickAppliesGravityAndDragBeforeMovement() {
        Vec3d result = RaycastTrajectory.pearlPosition(Vec3d.ZERO, new Vec3d(1, 1, 1), 1);
        assertEquals(.99, result.x, 1e-12); assertEquals(.9603, result.y, 1e-12); assertEquals(.99, result.z, 1e-12);
    }
}

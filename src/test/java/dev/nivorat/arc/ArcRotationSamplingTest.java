package dev.nivorat.arc;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArcRotationSamplingTest {
    @Test void quantizedTrajectoryDoesNotDependOnNumberOfRenderedFrames() {
        float sparse = trajectory(5, 0.5f);
        float dense = trajectory(500, 0.5f);
        assertEquals(sparse, dense);
        assertEquals(trajectory(5, 1f), trajectory(500, 1f));
    }

    @Test void monotonicTargetsDoNotCreateBackwardJitter() {
        float previous = 12.3f;
        for (int frame = 0; frame <= 2000; frame++) {
            float current = ArcCameraInterpolator.quantizeAngle(12.3f, 12.3f + frame * 0.001f, 0.15);
            assertTrue(current >= previous);
            assertEquals(Math.rint((current - 12.3f) / 0.15), (current - 12.3f) / 0.15, 0.00001);
            previous = current;
        }
        assertEquals(0f, ArcCameraInterpolator.quantizeAngle(0f, Float.NaN, 0.15));
    }

    private static float trajectory(int frames, float end) {
        float result = 0f;
        for (int i = 0; i <= frames; i++) {
            float progress = end * i / frames;
            double curve = progress * progress * progress * (progress * (progress * 6 - 15) + 10);
            result = ArcCameraInterpolator.quantizeAngle(12.3f, 12.3f + (float) curve * 45f, 0.15);
        }
        return result;
    }
}

package dev.nivorat.arc;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class ArcMotorEngineTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("State machine transitions through WAITING, RECORDING, PAUSED, READY")
    void testStateMachineTransitions() {
        ArcMotionProfile profile = new ArcMotionProfile(tempDir.resolve("state_test.json"));

        assertEquals(ArcCalibrationState.WAITING, profile.getState());

        profile.startCalibration();
        assertEquals(ArcCalibrationState.RECORDING, profile.getState());

        profile.pauseCalibration();
        assertEquals(ArcCalibrationState.PAUSED, profile.getState());

        profile.resumeCalibration();
        assertEquals(ArcCalibrationState.RECORDING, profile.getState());

        profile.cancelCalibration();
        assertEquals(ArcCalibrationState.WAITING, profile.getState());
    }

    @Test
    @DisplayName("Empty engine yields zero mastery and zero coverage")
    void testEmptyEngineYieldsZeroMastery() {
        ArcMotorAnalysisEngine engine = new ArcMotorAnalysisEngine();
        assertEquals(0, engine.computeMasteryPercent());
        assertEquals(0.0f, engine.computeKinematicCoverage(), 0.001f);
        assertEquals(0.0f, engine.computeActionCoverage(), 0.001f);
        assertEquals(0.0f, engine.computeTimingConsistency(), 0.001f);
        assertEquals(0.0f, engine.computeVolumeCoverage(), 0.001f);
    }

    @Test
    @DisplayName("Kinematic coverage scales with directional quadrants and velocity tiers")
    void testKinematicCoverageAcrossQuadrants() {
        ArcMotorAnalysisEngine engine = new ArcMotorAnalysisEngine();

        for (int q = 0; q < 8; q++) {
            for (int i = 0; i < 5; i++) {
                ArcMotorFrame frame = new ArcMotorFrame(1000L + (q * 5 + i) * 50L, 5.0f, 5.0f, 10.0f, 150.0f, 0.0f, q, ArcMotorFrame.ActionType.CAMERA_TICK, 50L);
                engine.recordFrame(frame);
            }
        }

        assertTrue(engine.computeKinematicCoverage() > 0.4f, "Multi-quadrant movements should produce substantial coverage");
        assertTrue(engine.computeVolumeCoverage() > 0.5f, "Volume coverage should increase with frame count");
    }

    @Test
    @DisplayName("Action coverage scales with bow, rail, cart, and detonation actions")
    void testActionCoverageAndHints() {
        ArcMotorAnalysisEngine engine = new ArcMotorAnalysisEngine();
        assertFalse(engine.getMissingDataHints().isEmpty());

        for (int i = 0; i < 3; i++) {
            engine.recordBowRelease();
            engine.recordRailPlacement(80L);
            engine.recordCartPlacement(70L);
        }
        engine.recordDetonation();
        engine.recordDetonation();

        assertEquals(1.0f, engine.computeActionCoverage(), 0.001f);
        assertTrue(engine.computeTimingConsistency() > 0.5f);
    }

    @Test
    @DisplayName("Outlier filtering rejects impossible mouse flicks and timing anomalies")
    void testOutlierFilterRejection() {
        assertFalse(ArcActionValidator.isValidKinematicDelta(200.0f, 0.0f, 0.05));
        assertFalse(ArcActionValidator.isValidKinematicDelta(0.0f, 100.0f, 0.05));
        assertFalse(ArcActionValidator.isValidKinematicDelta(Float.NaN, 5.0f, 0.05));
        assertFalse(ArcActionValidator.isValidKinematicDelta(5.0f, Float.POSITIVE_INFINITY, 0.05));
        assertTrue(ArcActionValidator.isValidKinematicDelta(15.0f, -8.0f, 0.05));

        assertFalse(ArcActionValidator.isValidActionDelay(5L));
        assertFalse(ArcActionValidator.isValidActionDelay(10000L));
        assertTrue(ArcActionValidator.isValidActionDelay(80L));

        assertFalse(ArcActionValidator.isValidBowDrawTicks(1));
        assertFalse(ArcActionValidator.isValidBowDrawTicks(25));
        assertTrue(ArcActionValidator.isValidBowDrawTicks(4));
    }

    @Test
    @DisplayName("Bayesian multi-session accumulation blends new session with existing profile")
    void testBayesianMultiSessionAccumulation() throws Exception {
        AtomicLong clock = new AtomicLong(1000L);
        Path path = tempDir.resolve("bayesian_profile.json");
        ArcMotionProfile profile = new ArcMotionProfile(path, clock::get);

        profile.startCalibration();
        for (int i = 0; i < 50; i++) {
            profile.observeMovement(4.0f, 3.0f, 0.05);
        }
        BlockPos pos = new BlockPos(5, 64, 5);
        for (int i = 0; i < 3; i++) {
            profile.recordBowRelease(4);
            clock.addAndGet(80);
            profile.recordRailPlacement(pos);
            clock.addAndGet(70);
            profile.recordCartPlacement(pos);
            clock.addAndGet(50);
            profile.recordExplosion(5.5, 64.5, 5.5);
            clock.addAndGet(50);
        }
        profile.finishCalibration();
        assertTrue(profile.isCalibrated());

        float initialConfidence = profile.getConfidenceScore();
        assertTrue(initialConfidence > 0.1f);
        int initialMinDelay = profile.getLearnedMinDelayMs();

        ArcMotionProfile session2 = new ArcMotionProfile(path, clock::get);
        assertTrue(session2.isCalibrated());
        session2.startCalibration();
        for (int i = 0; i < 50; i++) {
            session2.observeMovement(6.0f, 4.0f, 0.05);
        }
        for (int i = 0; i < 3; i++) {
            session2.recordBowRelease(5);
            clock.addAndGet(90);
            session2.recordRailPlacement(pos);
            clock.addAndGet(80);
            session2.recordCartPlacement(pos);
            clock.addAndGet(60);
            session2.recordExplosion(5.5, 64.5, 5.5);
            clock.addAndGet(60);
        }
        session2.finishCalibration();

        assertTrue(session2.getConfidenceScore() >= initialConfidence, "Multi-session accumulation should preserve or increase confidence");
        assertTrue(session2.getLearnedMinDelayMs() > 0);
    }

    @Test
    @DisplayName("Backwards compatibility loader for v3 JSON schema")
    void testBackwardsCompatibilityV3Schema() throws Exception {
        Path path = tempDir.resolve("legacy_v3.json");
        String v3Json = "{\n" +
                "  \"calibrated\": true,\n" +
                "  \"sampleCount\": 150,\n" +
                "  \"learnedMinDelayMs\": 45,\n" +
                "  \"learnedMaxDelayMs\": 75,\n" +
                "  \"learnedCameraSmoothness\": 95,\n" +
                "  \"curvatureBias\": 0.35,\n" +
                "  \"tremorVolatility\": 0.04,\n" +
                "  \"successfulDrawTicks\": [4, 5, 4]\n" +
                "}";
        Files.writeString(path, v3Json);

        ArcMotionProfile profile = new ArcMotionProfile(path);
        assertTrue(profile.isCalibrated());
        assertEquals(45, profile.getLearnedMinDelayMs());
        assertEquals(75, profile.getLearnedMaxDelayMs());
        assertEquals(95, profile.getLearnedCameraSmoothness());
    }

    @Test
    @DisplayName("Corrupted JSON safely resets without crash")
    void testCorruptedProfileHandling() throws Exception {
        Path path = tempDir.resolve("corrupted.json");
        Files.writeString(path, "{ corrupted json syntax !!!");

        ArcMotionProfile profile = new ArcMotionProfile(path);
        assertFalse(profile.isCalibrated());
        assertEquals(50, profile.getLearnedMinDelayMs());
        assertEquals(80, profile.getLearnedMaxDelayMs());
    }

    @Test
    @DisplayName("Pure camera shaking is strictly capped at <= 25% mastery")
    void testCameraShakingMasteryCappedAt25Percent() {
        ArcMotorAnalysisEngine engine = new ArcMotorAnalysisEngine();

        for (int frame = 0; frame < 500; frame++) {
            int quad = frame % 8;
            float vel = 15.0f + (frame % 8) * 25.0f;
            float mag = 1.0f + (frame % 6) * 5.0f;
            float dYaw = (frame % 2 == 0 ? 1.0f : -1.0f) * mag;
            float dPitch = (frame % 3 == 0 ? 0.8f : -0.8f) * (mag * 0.5f);
            ArcMotorFrame f = new ArcMotorFrame(1000L + frame * 50L, dPitch, dYaw, mag, vel, 0.0f, quad, ArcMotorFrame.ActionType.CAMERA_TICK, 50L, true);
            engine.recordFrame(f);
        }

        assertTrue(engine.computeCameraCoverage() > 0.7f, "Camera coverage should be high after 500 frames");

        int mastery35s = engine.computeMasteryPercent(35000L, 60000L);
        assertTrue(mastery35s <= 25, "Pure camera movement must be strictly capped at <= 25% mastery without action samples: was " + mastery35s);

        int masteryFullTime = engine.computeMasteryPercent(60000L, 60000L);
        assertTrue(masteryFullTime <= 25, "Even at 100% time elapsed, pure camera movement must not exceed 25%: was " + masteryFullTime);
    }
}

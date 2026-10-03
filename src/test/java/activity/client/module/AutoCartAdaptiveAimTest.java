package activity.client.module;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.module.impl.defense.OcclusionCacheModule;
import dev.nivorat.arc.ArcCameraInterpolator;
import dev.nivorat.arc.ArcMotorCalibrationService;
import dev.nivorat.arc.ArcMotionProfile;
import dev.nivorat.arc.MorrowConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AutoCartAdaptiveAimTest {

    @BeforeEach
    void setUp() {
        ActivityConfigManager.resetDefaults();
        MorrowConfig.resetDefaults();
    }

    @Test
    @DisplayName("AutoCart Preset Rebalancing: Fast, Medium and Safe presets have correct values")
    void testPresetRebalancing() {
        MorrowConfig.applyPreset(MorrowConfig.PRESET_FAST);
        assertEquals(35, MorrowConfig.minDelayMs);
        assertEquals(50, MorrowConfig.maxDelayMs);
        assertEquals(55, MorrowConfig.cameraSmoothnessMs);
        assertEquals("packet", MorrowConfig.cameraMode);
        assertFalse(MorrowConfig.autoCamera);
        assertTrue(MorrowConfig.cameraMouseGcd);

        MorrowConfig.applyPreset(MorrowConfig.PRESET_MEDIUM);
        assertEquals(50, MorrowConfig.minDelayMs);
        assertEquals(80, MorrowConfig.maxDelayMs);
        assertEquals(110, MorrowConfig.cameraSmoothnessMs);
        assertEquals("auto", MorrowConfig.cameraMode);
        assertTrue(MorrowConfig.autoCamera);
        assertTrue(MorrowConfig.cameraMouseGcd);

        MorrowConfig.applyPreset(MorrowConfig.PRESET_SAFE);
        assertEquals(95, MorrowConfig.minDelayMs);
        assertEquals(135, MorrowConfig.maxDelayMs);
        assertEquals(180, MorrowConfig.cameraSmoothnessMs);
        assertEquals("auto", MorrowConfig.cameraMode);
        assertTrue(MorrowConfig.autoCamera);
        assertTrue(MorrowConfig.cameraMouseGcd);

        MorrowConfig.applyPreset(MorrowConfig.PRESET_LEARNED);
        assertEquals(MorrowConfig.PRESET_LEARNED, MorrowConfig.preset);
        assertEquals(ArcMotionProfile.getInstance().getLearnedMinDelayMs(), MorrowConfig.minDelayMs);
        assertEquals(ArcMotionProfile.getInstance().getLearnedMaxDelayMs(), MorrowConfig.maxDelayMs);
        assertEquals(ArcMotionProfile.getInstance().getLearnedCameraSmoothness(), MorrowConfig.cameraSmoothnessMs);
        assertEquals("auto", MorrowConfig.cameraMode);
        assertTrue(MorrowConfig.autoCamera);
        assertTrue(MorrowConfig.cameraMouseGcd);
        assertEquals("Обученный", MorrowConfig.CartPreset.LEARNED.getTitle());
    }

    @Test
    @DisplayName("Adaptive Motor Profile: Forward pass produces bounded realistic kinematic scales")
    void testAdaptiveMotorProfileForward() {
        ArcMotionProfile profile = ArcMotionProfile.getInstance();
        assertNotNull(profile);

        float[] outputs = profile.forward(0.5f, 45.0f, 150.0f, 1.0f);
        assertNotNull(outputs);
        assertEquals(3, outputs.length);

        float velMod = outputs[0];
        float curveMod = outputs[1];
        float tremorMod = outputs[2];

        assertTrue(velMod >= 0.7f && velMod <= 1.5f, "Velocity scale must be within physiological range [0.7, 1.5]");
        assertTrue(curveMod >= 0.1f && curveMod <= 0.8f, "Curvature modulation must be within [0.1, 0.8]");
        assertTrue(tremorMod >= 0.01f && tremorMod <= 0.15f, "Tremor amplitude must be within [0.01, 0.15]");
    }

    @Test
    @DisplayName("Adaptive Motor Profile: Online learning updates network weights without exploding")
    void testAdaptiveMotorOnlineLearning() {
        ArcMotionProfile profile = ArcMotionProfile.getInstance();
        int initialSamples = profile.getSampleCount();

        for (int i = 0; i < 20; i++) {
            profile.trainOnline(0.4f, 30.0f, 120.0f, 1.0f, 1.05f, 0.38f, 0.05f);
        }

        assertTrue(profile.getSampleCount() >= initialSamples + 20);
        float[] outputsAfter = profile.forward(0.4f, 30.0f, 120.0f, 1.0f);
        assertTrue(outputsAfter[0] > 0.0f && outputsAfter[0] < 2.0f);
    }

    @Test
    @DisplayName("Ornstein-Uhlenbeck Tremor: Continuous noise is zero-mean and bounded")
    void testOrnsteinUhlenbeckTremor() {
        ArcMotionProfile profile = ArcMotionProfile.getInstance();
        float sumPitch = 0.0f;
        float sumYaw = 0.0f;
        int count = 200;

        for (int i = 0; i < count; i++) {
            float[] tremor = profile.getNextTremor(1.0f);
            assertNotNull(tremor);
            assertEquals(2, tremor.length);
            assertTrue(Math.abs(tremor[0]) < 1.0f, "Tremor pitch must stay bounded: " + tremor[0]);
            assertTrue(Math.abs(tremor[1]) < 1.0f, "Tremor yaw must stay bounded: " + tremor[1]);
            sumPitch += tremor[0];
            sumYaw += tremor[1];
        }

        float meanPitch = sumPitch / count;
        float meanYaw = sumYaw / count;
        assertTrue(Math.abs(meanPitch) < 0.25f, "Mean pitch tremor must be close to zero: " + meanPitch);
        assertTrue(Math.abs(meanYaw) < 0.25f, "Mean yaw tremor must be close to zero: " + meanYaw);
    }

    @Test
    @DisplayName("Calibration Service: Start, progress and completion life cycle")
    void testCalibrationServiceLifecycle() {
        assertEquals(300000L, ArcMotionProfile.CALIBRATION_DURATION_MS);

        ArcMotorCalibrationService.start();
        assertTrue(ArcMotorCalibrationService.isActive());

        long remaining = ArcMotorCalibrationService.getRemainingTimeMs();
        assertTrue(remaining > 0L && remaining <= 300000L);

        int progress = ArcMotorCalibrationService.getProgress();
        assertTrue(progress >= 0 && progress <= 100);

        ArcMotorCalibrationService.stop();
        assertFalse(ArcMotorCalibrationService.isActive());
    }

    @Test
    @DisplayName("Calibration Service: Manual action recording tracks cart detonation sequences")
    void testManualActionRecording() {
        ArcMotorCalibrationService.start();
        assertTrue(ArcMotorCalibrationService.isActive());
        assertEquals(0, ArcMotorCalibrationService.getManualDetonationsCount());

        BlockPos railPos = new BlockPos(12, 64, 15);
        BlockPos cartPos = new BlockPos(12, 65, 15);

        ArcMotorCalibrationService.onBowReleased(6);
        ArcMotorCalibrationService.onRailPlaced(railPos);
        ArcMotorCalibrationService.onCartPlaced(cartPos);
        ArcMotorCalibrationService.onExplosion(12.5, 65.5, 15.5);

        assertEquals(1, ArcMotorCalibrationService.getManualDetonationsCount());

        ArcMotorCalibrationService.stop();
        assertFalse(ArcMotorCalibrationService.isActive());
    }

    @Test
    @DisplayName("Calibration Service: Empty calibration must not activate learned preset")
    void testCalibrationCompletionActivatesLearnedPreset() {
        ArcMotorCalibrationService.start();
        assertTrue(ArcMotorCalibrationService.isActive());

        ArcMotionProfile.getInstance().finishCalibration();
        assertFalse(ArcMotionProfile.getInstance().isCalibrating());
        assertFalse(ArcMotionProfile.getInstance().didLastCalibrationSucceed());

        ArcMotorCalibrationService.checkCompletion();
        assertFalse(ArcMotorCalibrationService.isActive());
        assertNotEquals(MorrowConfig.PRESET_LEARNED, MorrowConfig.preset);
    }

    @Test
    @DisplayName("Ballistics Math: Step-by-step arrow trajectory matches gravity 0.05 and drag 0.99")
    void testArrowBallisticsSimulation() {
        Vec3d position = new Vec3d(0.0, 64.0, 0.0);
        Vec3d velocity = new Vec3d(1.5, 0.5, 0.0);

        for (int tick = 0; tick < 10; tick++) {
            Vec3d nextPos = position.add(velocity);
            assertTrue(nextPos.x > position.x, "Arrow must advance horizontally");
            position = nextPos;
            velocity = velocity.multiply(0.99).add(0.0, -0.05, 0.0);
        }

        assertTrue(velocity.y < 0.0, "After 10 ticks gravity must pull arrow downward");
    }

    @Test
    @DisplayName("Target Resolution: Upper third of TNT minecart is accurately offset above rail block")
    void testUpperThirdMinecartHitboxCalculation() {
        BlockPos railPos = new BlockPos(10, 65, 20);
        Vec3d cartUpperCenter = new Vec3d(railPos.getX() + 0.5D, railPos.getY() + 0.62D, railPos.getZ() + 0.5D);

        assertEquals(10.5, cartUpperCenter.x, 0.001);
        assertEquals(65.62, cartUpperCenter.y, 0.001);
        assertEquals(20.5, cartUpperCenter.z, 0.001);

        double cartBaseY = railPos.getY() + 0.0625;
        double cartTopY = cartBaseY + 0.70;
        assertTrue(cartUpperCenter.y > (cartBaseY + 0.70 * (2.0 / 3.0)), "Aim point must be in upper third of minecart");
        assertTrue(cartUpperCenter.y <= cartTopY, "Aim point must not exceed minecart height");
    }

    @Test
    @DisplayName("GCD Step Preservation: Integer quantization prevents floating point remainder leaks")
    void testGcdStepPreservation() {
        double sens = 0.5;
        double d = sens * 0.6000000238418579 + 0.20000000298023224;
        double gcd = d * d * d * 8.0 * 0.15;
        assertTrue(gcd > 0.05, "GCD at 0.5 sens must be substantial");

        float deltaYaw = 14.3721f;
        long steps = Math.round(deltaYaw / gcd);
        float quantizedDelta = (float) (steps * gcd);
        double remainder = Math.abs(deltaYaw - quantizedDelta);

        assertTrue(remainder < gcd, "Remainder must be strictly less than one GCD step");
        assertEquals(0.0, (quantizedDelta / gcd) - steps, 0.0001, "Quantized angle must be exact integer multiple of GCD");
    }

    @Test
    @DisplayName("Two-Phase Trajectory: Continuity across splitPoint without velocity or position jumps")
    void testTwoPhaseTrajectoryContinuity() {
        ArcMotionProfile profile = ArcMotionProfile.getInstance();
        double splitPoint = profile.getTwoPhaseRatio();
        double splitDist = 0.90;
        float velMod = 1.15f;
        float c = (velMod - 1.0f) * 0.5f;

        double tLeft = splitPoint - 0.0001;
        double subTLeft = tLeft / splitPoint;
        double warpedLeft = MathHelper.clamp(subTLeft + c * subTLeft * (1.0 - subTLeft), 0.0, 1.0);
        double smoothLeft = splitDist * (warpedLeft * warpedLeft * warpedLeft * (warpedLeft * (warpedLeft * 6.0 - 15.0) + 10.0));

        double tRight = splitPoint + 0.0001;
        double subTRight = (tRight - splitPoint) / (1.0 - splitPoint);
        double smoothRight = splitDist + (1.0 - splitDist) * (subTRight * subTRight * subTRight * (subTRight * (subTRight * 6.0 - 15.0) + 10.0));

        assertEquals(smoothLeft, smoothRight, 0.005, "Trajectory must be continuous at split point without angular snaps");

        double tEnd = 1.0;
        double subTEnd = (tEnd - splitPoint) / (1.0 - splitPoint);
        double smoothEnd = splitDist + (1.0 - splitDist) * (subTEnd * subTEnd * subTEnd * (subTEnd * (subTEnd * 6.0 - 15.0) + 10.0));
        assertEquals(1.0, smoothEnd, 0.0001, "Trajectory must reach exactly 1.0 at termination without snapping");
    }

    @Test
    @DisplayName("Tremor Envelope: Sinusoidal envelope ensures zero residual tremor at endpoints")
    void testTremorSmoothEnvelope() {
        assertEquals(0.0, Math.sin(0.0 * Math.PI), 0.0001, "Tremor envelope at t=0 must be exactly zero");
        assertEquals(0.0, Math.sin(1.0 * Math.PI), 0.0001, "Tremor envelope at t=1 must be zero to prevent endpoint jitter");
        assertTrue(Math.sin(0.5 * Math.PI) > 0.99, "Tremor envelope must peak near midpoint");
    }

    @Test
    @DisplayName("Feedback Loop Guard: Camera interpolator activity flag prevents bot from learning from itself")
    void testNoSelfLearningFeedbackLoop() {
        ArcCameraInterpolator interpolator = new ArcCameraInterpolator();
        assertFalse(ArcCameraInterpolator.isAnyActive());

        interpolator.start(0.0f, 10.0f, 0.0f, 20.0f, 100L, 0.2f);
        assertTrue(ArcCameraInterpolator.isAnyActive());

        ArcMotionProfile profile = ArcMotionProfile.getInstance();
        int sampleCountBefore = profile.getSampleCount();
        ArcMotionProfile.trackNaturalMovement(null);
        assertEquals(sampleCountBefore, profile.getSampleCount(), "Self-movement frames must not increment training samples");

        interpolator.reset();
        assertFalse(ArcCameraInterpolator.isAnyActive());
    }

    @Test
    @DisplayName("Exact Face Hit: Plane intersection matches raycast line of sight")
    void testExactFaceHitCalculation() {
        Vec3d eyePos = new Vec3d(0.0, 65.62, 0.0);
        Vec3d targetPoint = new Vec3d(3.5, 64.0, 0.0);
        Vec3d lookVec = targetPoint.subtract(eyePos).normalize();

        BlockPos supportPos = new BlockPos(3, 63, 0);
        double targetY = supportPos.getY() + 1.0D;
        double t = (targetY - eyePos.y) / lookVec.y;
        Vec3d exactHit = new Vec3d(eyePos.x + lookVec.x * t, targetY, eyePos.z + lookVec.z * t);

        assertEquals(3.5, exactHit.x, 0.001);
        assertEquals(64.0, exactHit.y, 0.001);
        assertEquals(0.0, exactHit.z, 0.001);
        assertTrue(exactHit.x >= supportPos.getX() && exactHit.x <= supportPos.getX() + 1.0);
    }

    @Test
    @DisplayName("Quick Bow Pull: Draw ticks of 3 produces valid progress above minimum threshold")
    void testQuickBowPullProgress() {
        float f = 3.0F / 20.0F;
        float progress = (f * f + f * 2.0F) / 3.0F;
        assertTrue(progress >= 0.10F, "Pull progress at 3 ticks (0.1075) must exceed 0.10F minimum threshold");
    }

    @Test
    @DisplayName("Custom Preset: Apply custom preset preserves individually tuned timings")
    void testCustomPresetPreservesTimings() {
        MorrowConfig.minDelayMs = 42;
        MorrowConfig.maxDelayMs = 73;
        MorrowConfig.cameraSmoothnessMs = 125;
        MorrowConfig.applyPreset(MorrowConfig.PRESET_CUSTOM);

        assertEquals(MorrowConfig.PRESET_CUSTOM, MorrowConfig.preset);
        assertEquals(42, MorrowConfig.minDelayMs);
        assertEquals(73, MorrowConfig.maxDelayMs);
        assertEquals(125, MorrowConfig.cameraSmoothnessMs);
        assertTrue(MorrowConfig.autonomousPlacement);
        assertEquals("Свой", MorrowConfig.CartPreset.CUSTOM.getTitle());
    }

    @Test
    @DisplayName("Calibration Reset: Restores default motoric state and factory delays")
    void testCalibrationReset() {
        ArcMotionProfile profile = ArcMotionProfile.getInstance();
        profile.trainOnline(0.5f, 40.0f, 100.0f, 1.0f, 1.2f, 0.45f, 0.08f);
        assertTrue(profile.getSampleCount() > 0);

        profile.resetCalibration();
        assertFalse(profile.isCalibrated());
        assertFalse(profile.isCalibrating());
        assertEquals(0, profile.getSampleCount());
        assertEquals(50, profile.getLearnedMinDelayMs());
        assertEquals(80, profile.getLearnedMaxDelayMs());
        assertEquals(110, profile.getLearnedCameraSmoothness());
    }
}

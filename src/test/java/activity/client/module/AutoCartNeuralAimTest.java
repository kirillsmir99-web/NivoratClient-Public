package activity.client.module;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.module.impl.defense.OcclusionCacheModule;
import dev.virion.arc.ArcCameraInterpolator;
import dev.virion.arc.ArcMotorCalibrationService;
import dev.virion.arc.ArcNeuralMotorProfile;
import dev.virion.arc.MorrowConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AutoCartNeuralAimTest {

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
        assertEquals(70, MorrowConfig.minDelayMs);
        assertEquals(100, MorrowConfig.maxDelayMs);
        assertEquals(160, MorrowConfig.cameraSmoothnessMs);
        assertEquals("auto", MorrowConfig.cameraMode);
        assertTrue(MorrowConfig.autoCamera);
        assertTrue(MorrowConfig.cameraMouseGcd);
    }

    @Test
    @DisplayName("Neural Motor Profile: Forward pass produces bounded realistic kinematic scales")
    void testNeuralMotorProfileForward() {
        ArcNeuralMotorProfile profile = ArcNeuralMotorProfile.getInstance();
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
    @DisplayName("Neural Motor Profile: Online learning updates network weights without exploding")
    void testNeuralMotorOnlineLearning() {
        ArcNeuralMotorProfile profile = ArcNeuralMotorProfile.getInstance();
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
        ArcNeuralMotorProfile profile = ArcNeuralMotorProfile.getInstance();
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
        ArcMotorCalibrationService.start();
        assertTrue(ArcMotorCalibrationService.isActive());

        int progress = ArcMotorCalibrationService.getProgress();
        assertTrue(progress >= 0 && progress <= 100);

        ArcMotorCalibrationService.stop();
        assertFalse(ArcMotorCalibrationService.isActive());
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
}

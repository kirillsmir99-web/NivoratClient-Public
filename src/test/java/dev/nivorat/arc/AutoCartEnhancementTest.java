package dev.nivorat.arc;

import dev.vector.VectorStreamConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class AutoCartEnhancementTest {
    @TempDir
    Path directory;

    @Test
    void macroUsesFastFallbackWhenUncalibrated() {
        Path path = directory.resolve("motor.json");
        ArcMotionProfile profile = new ArcMotionProfile(path);
        int sampled = profile.sampleMacroDrawTicks(6);
        assertTrue(sampled >= 3 && sampled <= 5);
    }

    @Test
    void vectorStreamRageModeHasZeroDelayFloor() {
        VectorStreamConfig.securityMode = VectorStreamConfig.MODE_RAGE;
        assertEquals(0, VectorStreamConfig.getMinFloor());
    }

    @Test
    void vectorStreamMaxSpeedHasZeroDelayFloor() {
        VectorStreamConfig.securityMode = VectorStreamConfig.MODE_LEGIT;
        VectorStreamConfig.maxSpeed = true;
        assertEquals(0, VectorStreamConfig.getMinFloor());
        VectorStreamConfig.maxSpeed = false;
    }

    @Test
    void clickPearlStatesIncludePhysicalInventoryFlow() {
        assertEquals(dev.pearl.ClickPearlController.State.OPENING_INVENTORY, dev.pearl.ClickPearlController.State.valueOf("OPENING_INVENTORY"));
        assertEquals(dev.pearl.ClickPearlController.State.CLOSING_INVENTORY, dev.pearl.ClickPearlController.State.valueOf("CLOSING_INVENTORY"));
        assertEquals(dev.pearl.ClickPearlController.State.OPENING_RETURN, dev.pearl.ClickPearlController.State.valueOf("OPENING_RETURN"));
        assertEquals(dev.pearl.ClickPearlController.State.CLOSING_RETURN, dev.pearl.ClickPearlController.State.valueOf("CLOSING_RETURN"));
    }

    @Test
    void slotSwitchingWaitsForCameraInterpolationToComplete() throws Exception {
        MorrowConfig.autoCamera = true;
        AutoCartController controller = new AutoCartController();
        java.lang.reflect.Field camField = AutoCartController.class.getDeclaredField("cameraInterpolator");
        camField.setAccessible(true);
        ArcCameraInterpolator interpolator = (ArcCameraInterpolator) camField.get(controller);
        assertNotNull(interpolator, "cameraInterpolator field must exist in AutoCartController");

        interpolator.start(0.0f, 20.0f, 0.0f, 30.0f, 500L, 0.1f);
        assertTrue(interpolator.isActive(), "Camera interpolation must be active after start");

        String code = java.nio.file.Files.readString(Path.of("src/main/java/dev/nivorat/arc/AutoCartController.java"));
        assertTrue(code.contains("if (MorrowConfig.autoCamera && cameraInterpolator.isActive()) return;"),
                "AutoCartController must halt placement and wait for camera interpolation before switching slots");
        assertTrue(code.contains("if (MorrowConfig.autoCamera && cameraInterpolator.isActive()) {"),
                "AutoCartController must wait for camera interpolation in PLACE_CART stage");

        interpolator.reset();
    }
}

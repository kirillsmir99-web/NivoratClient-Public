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
    void successfulDetonationsTrainMacroDrawTicks() {
        AtomicLong clock = new AtomicLong(1000L);
        Path path = directory.resolve("motor.json");
        ArcMotionProfile profile = new ArcMotionProfile(path, clock::get);
        profile.startCalibration();

        var pos = new net.minecraft.util.math.BlockPos(10, 64, 10);
        profile.recordBowRelease(4);
        clock.addAndGet(80);
        profile.recordRailPlacement(pos);
        clock.addAndGet(70);
        profile.recordCartPlacement(pos);
        clock.addAndGet(50);
        profile.recordExplosion(10.5, 64.5, 10.5);

        assertEquals(1, profile.getManualDetonationsCount());

        for (int i = 0; i < 20; i++) {
            int sampled = profile.sampleMacroDrawTicks(6);
            assertTrue(sampled >= 3 && sampled <= 5);
        }
    }

    @Test
    void exportAndImportRetainsSuccessfulDrawTicks() throws Exception {
        AtomicLong clock = new AtomicLong(1000L);
        Path path = directory.resolve("motor.json");
        ArcMotionProfile profile = new ArcMotionProfile(path, clock::get);
        profile.startCalibration();

        for (int i = 0; i < 25; i++) profile.observeMovement(2f, 3f, 0.05);

        var pos = new net.minecraft.util.math.BlockPos(0, 64, 0);
        for (int i = 0; i < 3; i++) {
            profile.recordBowRelease(4);
            clock.addAndGet(100);
            profile.recordRailPlacement(pos);
            clock.addAndGet(75);
            profile.recordCartPlacement(pos);
            clock.addAndGet(50);
            profile.recordExplosion(0.5, 64.5, 0.5);
            clock.addAndGet(50);
        }

        profile.finishCalibration();
        assertTrue(profile.isCalibrated());

        var exported = profile.exportProfile();
        assertTrue(exported.has("successfulDrawTicks"));

        Path secondPath = directory.resolve("second.json");
        ArcMotionProfile imported = new ArcMotionProfile(secondPath, clock::get);
        imported.importProfile(exported);

        int sampled = imported.sampleMacroDrawTicks(6);
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
    void calibrationHudUsesWatermarkDividers() throws Exception {
        String code = java.nio.file.Files.readString(Path.of("src/main/java/activity/client/gui/hud/ActivityHudOverlay.java"));
        assertTrue(code.contains("\" | Калибровка: \""));
        assertTrue(code.contains("\" | \""));
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
        assertFalse(interpolator.isActive(), "Camera interpolation must be inactive after reset");
    }

    @Test
    void configurableCalibrationDurationSupported() {
        Path path = directory.resolve("motor_duration.json");
        ArcMotionProfile profile = new ArcMotionProfile(path);
        assertEquals(5, profile.getTargetCalibrationDurationMinutes());
        assertEquals(300000L, profile.getTargetCalibrationDurationMs());

        profile.setTargetCalibrationDurationMinutes(2);
        assertEquals(2, profile.getTargetCalibrationDurationMinutes());
        assertEquals(120000L, profile.getTargetCalibrationDurationMs());

        profile.setTargetCalibrationDurationMinutes(1);
        assertEquals(1, profile.getTargetCalibrationDurationMinutes());
        assertEquals(60000L, profile.getTargetCalibrationDurationMs());

        profile.setTargetCalibrationDurationMinutes(5);
        assertEquals(5, profile.getTargetCalibrationDurationMinutes());
        assertEquals(300000L, profile.getTargetCalibrationDurationMs());
    }

    @Test
    void autoCartLoggerRecordsToDiagnosticEngineAndFile() {
        AutoCartLogger.log("TEST_EVENT", true, "sample_diagnostic_detail");
        String report = activity.client.diagnostic.DiagnosticEngine.generateReport();
        assertTrue(report.contains("auto_cart") && report.contains("TEST_EVENT"));
        java.io.File logFile = new java.io.File("logs", "autocart.log");
        assertTrue(logFile.exists());
    }
}

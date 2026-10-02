package dev.nivorat.arc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

class ArcMotionPersistenceTest {
    @TempDir Path directory;

    @Test void learningReducesErrorAndSurvivesRestart() throws Exception {
        Path path = directory.resolve("motor.json");
        ArcMotionProfile profile = new ArcMotionProfile(path);
        float[] target = {1.08f, 0.38f, 0.055f};
        float before = error(profile.forward(0.4f, 30f, 120f, 1f), target);
        for (int i = 0; i < 250; i++) profile.trainOnline(0.4f, 30f, 120f, 1f, target[0], target[1], target[2]);
        float[] trained = profile.forward(0.4f, 30f, 120f, 1f);
        assertTrue(error(trained, target) < before * 0.2f);
        assertFalse(Files.exists(path), "Training must not write on every render sample");
        profile.save();
        assertArrayEquals(trained, new ArcMotionProfile(path).forward(0.4f, 30f, 120f, 1f), 0.000001f);
        assertFalse(Files.exists(directory.resolve("motor.json.tmp")));
    }

    @Test void invalidTrainingDoesNotPoisonNetwork() {
        ArcMotionProfile profile = new ArcMotionProfile(directory.resolve("motor.json"));
        float[] before = profile.forward(0.4f, 30f, 120f, 1f);
        profile.trainOnline(Float.NaN, 30f, 120f, 1f, 1f, 0.35f, 0.05f);
        profile.trainOnline(0.4f, 30f, Float.POSITIVE_INFINITY, 1f, 1f, 0.35f, 0.05f);
        assertEquals(0, profile.getSampleCount());
        assertArrayEquals(before, profile.forward(0.4f, 30f, 120f, 1f));
    }

    @Test void emptyCalibrationDoesNotClaimSuccess() {
        ArcMotionProfile profile = new ArcMotionProfile(directory.resolve("motor.json"));
        profile.startCalibration();
        profile.finishCalibration();
        assertFalse(profile.isCalibrated());
        assertFalse(profile.didLastCalibrationSucceed());
    }

    @Test void malformedProfileCannotPartiallyChangeExistingState() throws Exception {
        var path = directory.resolve("motor.json");
        var profile = new ArcMotionProfile(path);
        var before = profile.forward(0.4f, 30f, 120f, 1f);
        Files.writeString(path, "{\"calibrated\":true,\"sampleCount\":500,\"biasHidden\":[1]}");
        profile.load();
        assertFalse(profile.isCalibrated());
        assertEquals(0, profile.getSampleCount());
        assertArrayEquals(before, profile.forward(0.4f, 30f, 120f, 1f));
        Files.writeString(path, "{\"sampleCount\":500,\"biasOutput\":[0,0,\"invalid\"]}");
        profile.load();
        assertEquals(0, profile.getSampleCount());
        assertArrayEquals(before, profile.forward(0.4f, 30f, 120f, 1f));
    }

    @Test void excessiveProfileSizeIsRejected() throws Exception {
        var path = directory.resolve("motor.json");
        Files.writeString(path, "{\"calibrated\":true,\"padding\":\"" + "x".repeat(65_536) + "\"}");
        assertFalse(new ArcMotionProfile(path).isCalibrated());
    }

    private static float error(float[] output, float[] target) {
        float result = 0;
        for (int i = 0; i < output.length; i++) result += (output[i] - target[i]) * (output[i] - target[i]);
        return result;
    }

    @Test void pauseFreezesTimerAndObservationsAndResumeKeepsSamples() {
        var time = new java.util.concurrent.atomic.AtomicLong(1000L);
        var profile = new ArcMotionProfile(directory.resolve("motor.json"), time::get);
        profile.startCalibration();
        profile.observeMovement(2f, 3f, 0.05);
        time.addAndGet(1000);
        profile.pauseCalibration();
        long remaining = profile.getCalibrationRemainingTimeMs();
        int samples = profile.getSampleCount();
        time.addAndGet(600000);
        profile.observeMovement(2f, 3f, 0.05);
        profile.recordBowRelease(6);
        assertFalse(profile.isCalibrating());
        assertTrue(profile.hasCalibrationSession());
        assertEquals(remaining, profile.getCalibrationRemainingTimeMs());
        assertEquals(samples, profile.getSampleCount());
        assertEquals(0, profile.getBowShotsCount());
        profile.resumeCalibration();
        assertTrue(profile.isCalibrating());
        assertEquals(remaining, profile.getCalibrationRemainingTimeMs());
        profile.observeMovement(2f, 3f, 0.05);
        assertEquals(samples + 1, profile.getSampleCount());
    }

    @Test void cancellingRestoresWeightsAndDoesNotOverwriteSavedProfile() throws Exception {
        var path = directory.resolve("motor.json");
        var profile = new ArcMotionProfile(path);
        profile.save();
        String saved = Files.readString(path);
        float[] before = profile.forward(0.5f, 5f, 60f, 1f);
        profile.startCalibration();
        for (int i = 0; i < 50; i++) profile.observeMovement(2f, 3f, 0.05);
        profile.save();
        assertEquals(saved, Files.readString(path));
        profile.cancelCalibration();
        assertEquals(0, profile.getSampleCount());
        assertArrayEquals(before, profile.forward(0.5f, 5f, 60f, 1f));
        profile.observeMovement(2f, 3f, 0.05);
        assertEquals(0, profile.getSampleCount());
    }

    @Test void completeManualSequencesProducePersistentProfileAndCanFreezeLearning() {
        var time = new java.util.concurrent.atomic.AtomicLong(1000L);
        var path = directory.resolve("motor.json");
        var profile = new ArcMotionProfile(path, time::get);
        profile.startCalibration();
        for (int i = 0; i < 25; i++) profile.observeMovement(2f, 3f, 0.05);
        var pos = new net.minecraft.util.math.BlockPos(0, 64, 0);
        for (int i = 0; i < 3; i++) {
            profile.recordBowRelease(6);
            time.addAndGet(100);
            profile.recordRailPlacement(pos);
            time.addAndGet(75);
            profile.recordCartPlacement(pos);
            time.addAndGet(100);
        }
        assertTrue(profile.isReadyToFinish());
        profile.finishCalibration();
        assertTrue(profile.didLastCalibrationSucceed());
        assertTrue(profile.isCalibrated());
        assertEquals(100, profile.getLearnedPlacementDelayRailMs());
        assertEquals(75, profile.getLearnedPlacementDelayCartMs());
        var reloaded = new ArcMotionProfile(path);
        assertTrue(reloaded.isCalibrated());
        assertArrayEquals(profile.forward(0.5f, 5f, 60f, 1f), reloaded.forward(0.5f, 5f, 60f, 1f));
        profile.setAdaptiveLearning(false);
        int samples = profile.getSampleCount();
        profile.observeMovement(2f, 3f, 0.05);
        assertEquals(samples, profile.getSampleCount());
        assertFalse(new ArcMotionProfile(path).isAdaptiveLearning());
    }

    @Test void pausePreventsPairingActionsAcrossThePauseAndInvalidMovementIsIgnored() {
        var time = new java.util.concurrent.atomic.AtomicLong(1000L);
        var profile = new ArcMotionProfile(directory.resolve("motor.json"), time::get);
        profile.startCalibration();
        profile.recordBowRelease(6);
        profile.pauseCalibration();
        time.addAndGet(50);
        profile.resumeCalibration();
        profile.recordRailPlacement(new net.minecraft.util.math.BlockPos(0, 64, 0));
        assertEquals(0, profile.getShotRailPairs());
        profile.observeMovement(Float.NaN, 3f, 0.05);
        profile.observeMovement(2f, 3f, 0.5);
        profile.observeMovement(200f, 3f, 0.05);
        assertEquals(0, profile.getSampleCount());
    }
}

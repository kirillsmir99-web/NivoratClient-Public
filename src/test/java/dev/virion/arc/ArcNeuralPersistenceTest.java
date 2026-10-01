package dev.virion.arc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

class ArcNeuralPersistenceTest {
    @TempDir Path directory;

    @Test void learningReducesErrorAndSurvivesRestart() throws Exception {
        Path path = directory.resolve("motor.json");
        ArcNeuralMotorProfile profile = new ArcNeuralMotorProfile(path);
        float[] target = {1.08f, 0.38f, 0.055f};
        float before = error(profile.forward(0.4f, 30f, 120f, 1f), target);
        for (int i = 0; i < 250; i++) profile.trainOnline(0.4f, 30f, 120f, 1f, target[0], target[1], target[2]);
        float[] trained = profile.forward(0.4f, 30f, 120f, 1f);
        assertTrue(error(trained, target) < before * 0.2f);
        assertFalse(Files.exists(path), "Training must not write on every render sample");
        profile.save();
        assertArrayEquals(trained, new ArcNeuralMotorProfile(path).forward(0.4f, 30f, 120f, 1f), 0.000001f);
        assertFalse(Files.exists(directory.resolve("motor.json.tmp")));
    }

    @Test void invalidTrainingDoesNotPoisonNetwork() {
        ArcNeuralMotorProfile profile = new ArcNeuralMotorProfile(directory.resolve("motor.json"));
        float[] before = profile.forward(0.4f, 30f, 120f, 1f);
        profile.trainOnline(Float.NaN, 30f, 120f, 1f, 1f, 0.35f, 0.05f);
        profile.trainOnline(0.4f, 30f, Float.POSITIVE_INFINITY, 1f, 1f, 0.35f, 0.05f);
        assertEquals(0, profile.getSampleCount());
        assertArrayEquals(before, profile.forward(0.4f, 30f, 120f, 1f));
    }

    @Test void emptyCalibrationDoesNotClaimSuccess() {
        ArcNeuralMotorProfile profile = new ArcNeuralMotorProfile(directory.resolve("motor.json"));
        profile.startCalibration();
        profile.finishCalibration();
        assertFalse(profile.isCalibrated());
        assertFalse(profile.didLastCalibrationSucceed());
    }

    private static float error(float[] output, float[] target) {
        float result = 0;
        for (int i = 0; i < output.length; i++) result += (output[i] - target[i]) * (output[i] - target[i]);
        return result;
    }
}

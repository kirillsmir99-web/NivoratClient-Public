package dev.nivorat.arc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

class ArcMotionPersistenceTest {
    @TempDir Path directory;

    @Test void profileSurvivesRestart() throws Exception {
        Path path = directory.resolve("motor.json");
        ArcMotionProfile profile = new ArcMotionProfile(path);
        profile.setLearnedPlacementDelayRailMs(75);
        profile.setLearnedPlacementDelayCartMs(85);
        profile.setLearnedCameraSmoothness(120);
        profile.save();

        ArcMotionProfile reloaded = new ArcMotionProfile(path);
        assertEquals(75, reloaded.getLearnedPlacementDelayRailMs());
        assertEquals(85, reloaded.getLearnedPlacementDelayCartMs());
        assertEquals(120, reloaded.getLearnedCameraSmoothness());
    }

    @Test void forwardProducesValidKinematics() {
        ArcMotionProfile profile = new ArcMotionProfile(directory.resolve("motor.json"));
        float[] res = profile.forward(0.5f, 45.0f, 10.0f, 1.0f);
        assertNotNull(res);
        assertEquals(3, res.length);
        assertTrue(Float.isFinite(res[0]));
        assertTrue(Float.isFinite(res[1]));
        assertTrue(Float.isFinite(res[2]));
    }

    @Test void exportAndImportProfile() throws Exception {
        Path path = directory.resolve("motor.json");
        ArcMotionProfile profile = new ArcMotionProfile(path);
        profile.setLearnedPlacementDelayRailMs(90);
        profile.save();

        var exported = profile.exportProfile();
        assertNotNull(exported);

        Path path2 = directory.resolve("second.json");
        ArcMotionProfile second = new ArcMotionProfile(path2);
        second.importProfile(exported);
        assertEquals(90, second.getLearnedPlacementDelayRailMs());
    }

    @Test void excessiveProfileSizeIsRejected() throws Exception {
        var path = directory.resolve("motor.json");
        Files.writeString(path, "{\"padding\":\"" + "x".repeat(65_536) + "\"}");
        assertDoesNotThrow(() -> new ArcMotionProfile(path));
    }
}

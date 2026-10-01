package activity.client.module;

import net.fabricmc.pack.api.GaussianTimingEngine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ActionTimingRegressionTest {
    @Test void positiveDelaysNeverCollapseToSameTick() {
        assertEquals(1, GaussianTimingEngine.toActionTicks(30));
        assertEquals(2, GaussianTimingEngine.toActionTicks(51));
        assertEquals(1, GaussianTimingEngine.toActionTicks(Double.NaN));
        assertEquals(2, GaussianTimingEngine.sampleActionTicks(100, false));
        for (int i = 0; i < 1000; i++) {
            int delay = GaussianTimingEngine.sampleActionTicks(30, true);
            assertTrue(delay >= 1 && delay <= 3);
        }
    }
}

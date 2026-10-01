package activity.client.gui;

import activity.client.module.api.*;
import activity.client.gui.custom.api.modules.Category;
import activity.client.gui.custom.api.modules.ModuleManager;
import activity.client.module.impl.utility.ModelMeshModule;
import activity.client.module.service.CooldownTrackerService.CooldownEntry;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;

class OptimizationRegressionTest {
    @Test void registrySnapshotIsImmutableAndUpdatesImmediatelyOnReplacementAndRemoval() {
        List<IModule> before = ModuleRegistry.getAll();
        assertSame(before, ModuleRegistry.getAll());
        assertThrows(UnsupportedOperationException.class, () -> before.clear());
        IModule original = ModuleRegistry.get(ModelMeshModule.ID);
        var manager = ModuleManager.get();
        manager.getAll();
        var replacement = new ModelMeshModule();
        try {
            ModuleRegistry.register(replacement);
            assertNotSame(before, ModuleRegistry.getAll());
            assertSame(replacement, manager.findByName(ModelMeshModule.ID).delegate);
            var kit = manager.forCategory(Category.SMP);
            assertSame(kit, manager.forCategory(Category.SMP));
            assertSame(manager.getSearchModules(), manager.getSearchModules());
            ModuleRegistry.unregister(ModelMeshModule.ID);
            assertNull(manager.findByName(ModelMeshModule.ID));
            assertTrue(before.contains(original));
        } finally { ModuleRegistry.register(original); }
    }

    @Test void timerFormattingExactlyMatchesOriginalAcrossAllBoundaryTicks() {
        var entry = new CooldownEntry(null, 200);
        for (int tick = -2; tick <= 600; tick++) {
            entry.remainingTicks = tick;
            float seconds = Math.max(0f, tick / 20f);
            String expected = seconds <= 0 ? "0.0s" : seconds > 5 ? ((int)Math.ceil(seconds)) + "s" : String.format(Locale.ROOT, "%.1fs", seconds);
            assertEquals(expected, entry.getFormattedRemaining(), "tick " + tick);
            assertSame(entry.getFormattedRemaining(), entry.getFormattedRemaining());
        }
    }
}

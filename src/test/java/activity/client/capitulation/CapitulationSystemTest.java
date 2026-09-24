package activity.client.capitulation;

import activity.client.module.api.BuiltinModules;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleEventDispatcher;
import activity.client.module.api.ModuleRegistry;
import net.minecraft.util.ActionResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Capitulation (Emergency Deactivation & Stealth) System Tests")
public class CapitulationSystemTest {

    @BeforeEach
    void setUp() {
        CapitulationManager.resetForTesting();
        BuiltinModules.registerAll();
    }

    @AfterEach
    void tearDown() {
        CapitulationManager.resetForTesting();
    }

    @Test
    @DisplayName("Capitulation state is false by default")
    void testDefaultState() {
        assertFalse(CapitulationManager.isCapitulated(), "Should not be capitulated on startup");
    }

    @Test
    @DisplayName("Capitulation disables all modules in memory and purges dispatchers")
    void testCapitulationDeactivatesAllModules() {

        for (IModule module : ModuleRegistry.getAll()) {
            module.setEnabled(true);
        }
        ModuleEventDispatcher.updateActiveModules();

        assertTrue(ModuleEventDispatcher.getActiveTickModules().length > 0, "Active tick modules must be present");
        assertTrue(ModuleEventDispatcher.getActiveAttackModules().length > 0, "Active attack modules must be present");

        CapitulationManager.capitulate(null);

        assertTrue(CapitulationManager.isCapitulated(), "Capitulation flag must be true");

        for (IModule module : ModuleRegistry.getAll()) {
            assertFalse(module.isEnabled(), "Module " + module.getId() + " must be disabled after capitulation");
        }

        assertEquals(0, ModuleEventDispatcher.getActiveTickModules().length, "Active tick modules must be 0");
        assertEquals(0, ModuleEventDispatcher.getActiveAttackModules().length, "Active attack modules must be 0");
        assertEquals(0, ModuleEventDispatcher.getActiveHudModules().length, "Active hud modules must be 0");

        ActionResult attackResult = ModuleEventDispatcher.onAttackEntity(null, null, null, null, null);
        assertEquals(ActionResult.PASS, attackResult, "Attack entity must pass with zero evaluation");

        ModuleEventDispatcher.updateActiveModules();
        assertEquals(0, ModuleEventDispatcher.getActiveTickModules().length);
    }

    @Test
    @DisplayName("Capitulation cannot be undone until full reset (simulating game restart)")
    void testIrreversibleUntilRestart() {
        CapitulationManager.capitulate(null);
        assertTrue(CapitulationManager.isCapitulated());

        for (IModule module : ModuleRegistry.getAll()) {
            module.setEnabled(true);
        }
        ModuleEventDispatcher.updateActiveModules();

        assertEquals(0, ModuleEventDispatcher.getActiveTickModules().length);
        assertEquals(0, ModuleEventDispatcher.getActiveAttackModules().length);

        CapitulationManager.resetForTesting();
        assertFalse(CapitulationManager.isCapitulated());

        ModuleEventDispatcher.updateActiveModules();
        assertTrue(ModuleEventDispatcher.getActiveTickModules().length > 0);
    }

    @Test
    @DisplayName("Capitulation suppresses disk writes to prevent filesystem and USN journal trace detection")
    void testDiskSaveSuppressionDuringCapitulation() {
        CapitulationManager.capitulate(null);
        assertTrue(CapitulationManager.isCapitulated());

        boolean saved = activity.client.config.ActivityConfigManager.save();
        assertFalse(saved, "Config save must be blocked during capitulation");
    }

    @Test
    @DisplayName("Capitulation purges in-memory search keyword index and config models from heap")
    void testHeapPurgeDuringCapitulation() {

        assertFalse(activity.client.gui.search.SearchController.search("mace", 5).isEmpty());

        CapitulationManager.capitulate(null);

        assertTrue(activity.client.gui.search.SearchController.search("mace", 5).isEmpty());

        assertTrue(activity.client.config.ActivityConfigManager.getConfig().menuKeybind.isUnbound());
    }
}

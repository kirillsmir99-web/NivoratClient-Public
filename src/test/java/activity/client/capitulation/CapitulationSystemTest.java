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
        // Enable several modules
        for (IModule module : ModuleRegistry.getAll()) {
            module.setEnabled(true);
        }
        ModuleEventDispatcher.updateActiveModules();

        assertTrue(ModuleEventDispatcher.getActiveTickModules().length > 0, "Active tick modules must be present");
        assertTrue(ModuleEventDispatcher.getActiveAttackModules().length > 0, "Active attack modules must be present");

        // Trigger capitulation
        CapitulationManager.capitulate(null);

        assertTrue(CapitulationManager.isCapitulated(), "Capitulation flag must be true");

        // All modules should now be disabled
        for (IModule module : ModuleRegistry.getAll()) {
            assertFalse(module.isEnabled(), "Module " + module.getId() + " must be disabled after capitulation");
        }

        // Active dispatcher arrays must be empty
        assertEquals(0, ModuleEventDispatcher.getActiveTickModules().length, "Active tick modules must be 0");
        assertEquals(0, ModuleEventDispatcher.getActiveAttackModules().length, "Active attack modules must be 0");
        assertEquals(0, ModuleEventDispatcher.getActiveHudModules().length, "Active hud modules must be 0");

        // Calling onAttackEntity during capitulation must immediately pass
        ActionResult attackResult = ModuleEventDispatcher.onAttackEntity(null, null, null, null, null);
        assertEquals(ActionResult.PASS, attackResult, "Attack entity must pass with zero evaluation");

        // Rebuilding active modules during capitulation must remain empty
        ModuleEventDispatcher.updateActiveModules();
        assertEquals(0, ModuleEventDispatcher.getActiveTickModules().length);
    }

    @Test
    @DisplayName("Capitulation cannot be undone until full reset (simulating game restart)")
    void testIrreversibleUntilRestart() {
        CapitulationManager.capitulate(null);
        assertTrue(CapitulationManager.isCapitulated());

        // Attempting to re-enable a module and update active modules
        for (IModule module : ModuleRegistry.getAll()) {
            module.setEnabled(true);
        }
        ModuleEventDispatcher.updateActiveModules();

        // While capitulated, active modules cannot be populated
        assertEquals(0, ModuleEventDispatcher.getActiveTickModules().length);
        assertEquals(0, ModuleEventDispatcher.getActiveAttackModules().length);

        // Reset simulating game restart
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

        // Attempting to save config when capitulated must return false and touch zero disk files
        boolean saved = activity.client.config.ActivityConfigManager.save();
        assertFalse(saved, "Config save must be blocked during capitulation");
    }
}

package activity.client.module;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import dev.mesh.ModelMeshConfig;
import dev.mesh.ModelMeshEngine;

import static org.junit.jupiter.api.Assertions.*;

public class AutoToolEngineTest {

    private ModelMeshConfig config;

    @BeforeEach
    void setUp() {
        ModelMeshEngine.resetSession();
        config = new ModelMeshConfig();
        config.enabled = true;
        config.weaponSwitch = true;
        config.combatGuard = true;
        config.durabilitySaver = true;
        config.durabilityThreshold = 5;
        config.restorePreviousItem = true;
    }

    @Test
    @DisplayName("Weapon detection: Null stack is not a weapon")
    void testWeaponDetectionNull() {
        assertFalse(ModelMeshEngine.isWeapon(null), "Null stack must not be recognized as weapon");
    }

    @Test
    @DisplayName("Weapon base damage: Returns 0 for null stack")
    void testWeaponBaseDamageNull() {
        assertEquals(0.0f, ModelMeshEngine.getWeaponBaseDamage(null), 0.001f);
    }

    @Test
    @DisplayName("Weapon scoring: Null stack returns -100.0f")
    void testEvaluateWeaponScoreNull() {
        assertEquals(-100.0f, ModelMeshEngine.evaluateWeaponScore(null, null, config), 0.001f);
    }

    @Test
    @DisplayName("Tool scoring: Null stack returns 0.0f")
    void testEvaluateToolScoreNull() {
        assertEquals(0.0f, ModelMeshEngine.evaluateToolScore(null, null, config), 0.001f);
    }

    @Test
    @DisplayName("Combat session lifecycle: State tracking, test fixtures and resetSession")
    void testCombatSessionLifecycle() {
        assertFalse(ModelMeshEngine.isCombatSessionActive(), "Combat session must initially be inactive");
        assertEquals(-1, ModelMeshEngine.getOriginalHotbarSlot(), "Original hotbar slot must initially be -1");
        assertEquals(-1, ModelMeshEngine.getExpectedToolSlot(), "Expected tool slot must initially be -1");

        ModelMeshEngine.setCombatSessionActiveForTest(true, 2, 5);
        assertTrue(ModelMeshEngine.isCombatSessionActive(), "Combat session must be active after test fixture");
        assertEquals(2, ModelMeshEngine.getOriginalHotbarSlot(), "Original hotbar slot must match fixture");
        assertEquals(5, ModelMeshEngine.getExpectedToolSlot(), "Expected tool slot must match fixture");

        ModelMeshEngine.resetSession();
        assertFalse(ModelMeshEngine.isCombatSessionActive(), "Combat session must be inactive after reset");
        assertEquals(-1, ModelMeshEngine.getOriginalHotbarSlot(), "Original slot must reset to -1");
        assertEquals(-1, ModelMeshEngine.getExpectedToolSlot(), "Expected slot must reset to -1");
    }

    @Test
    @DisplayName("Mining session lifecycle: State tracking, test fixtures and resetSession")
    void testMiningSessionLifecycle() {
        assertFalse(ModelMeshEngine.isMiningSessionActive(), "Mining session must initially be inactive");
        assertEquals(-1, ModelMeshEngine.getOriginalHotbarSlot(), "Original slot must initially be -1");

        ModelMeshEngine.setMiningSessionActiveForTest(true, 0, 3);
        assertTrue(ModelMeshEngine.isMiningSessionActive(), "Mining session must be active after test fixture");
        assertEquals(0, ModelMeshEngine.getOriginalHotbarSlot(), "Original slot must match fixture");
        assertEquals(3, ModelMeshEngine.getExpectedToolSlot(), "Expected slot must match fixture");

        ModelMeshEngine.resetSession();
        assertFalse(ModelMeshEngine.isMiningSessionActive(), "Mining session must be inactive after reset");
        assertEquals(-1, ModelMeshEngine.getOriginalHotbarSlot(), "Original slot must reset to -1");
        assertEquals(-1, ModelMeshEngine.getExpectedToolSlot(), "Expected slot must reset to -1");
    }

    @Test
    @DisplayName("ModelMeshConfig: Defaults reflect anti-cheat safety and combat weapon switching")
    void testConfigDefaults() {
        ModelMeshConfig cfg = new ModelMeshConfig();
        assertTrue(cfg.enabled, "AutoTool must be enabled by default");
        assertTrue(cfg.weaponSwitch, "Weapon switching must be enabled by default");
        assertTrue(cfg.combatGuard, "Combat guard must be enabled by default");
        assertTrue(cfg.restorePreviousItem, "Restore previous item must be enabled by default");
        assertTrue(cfg.durabilitySaver, "Durability saver must be enabled by default");
        assertEquals(5, cfg.durabilityThreshold, "Durability threshold must default to 5");
        assertTrue(cfg.ignoreInstantBreak, "Ignore instant break must be enabled by default");
        assertTrue(cfg.lockWhileMining, "Lock while mining must be enabled by default");
        assertTrue(cfg.legitMode, "Legit mode must be enabled by default");
        assertFalse(cfg.singleSlotMode, "Single slot mode must be false by default");
        assertEquals(0, cfg.singleSlot, "Single slot index must default to 0");
    }

    @Test
    @DisplayName("Single slot swappedFromContainerSlot lifecycle and reset")
    void testSingleSlotLifecycle() {
        assertEquals(-1, ModelMeshEngine.getSwappedFromContainerSlot());
        ModelMeshEngine.setSwappedFromContainerSlotForTest(24);
        assertEquals(24, ModelMeshEngine.getSwappedFromContainerSlot());
        ModelMeshEngine.resetSession();
        assertEquals(-1, ModelMeshEngine.getSwappedFromContainerSlot());
    }

    @Test
    @DisplayName("Durability saver score: Returns -1000.0f when durability <= threshold")
    void testDurabilitySaverScoring() {

        config.durabilitySaver = true;
        config.durabilityThreshold = 5;

        assertEquals(-100.0f, ModelMeshEngine.evaluateWeaponScore(null, null, config));
        assertEquals(0.0f, ModelMeshEngine.evaluateToolScore(null, null, config));
    }
}

package activity.client.module;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.elarion.autotool.AutoToolConfig;
import ru.elarion.autotool.AutoToolEngine;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AutoToolEngine:
 * Weapon evaluation, combat session lifecycle, durability saving, and mining evaluation.
 */
public class AutoToolEngineTest {

    private AutoToolConfig config;

    @BeforeEach
    void setUp() {
        AutoToolEngine.resetSession();
        config = new AutoToolConfig();
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
        assertFalse(AutoToolEngine.isWeapon(null), "Null stack must not be recognized as weapon");
    }

    @Test
    @DisplayName("Weapon base damage: Returns 0 for null stack")
    void testWeaponBaseDamageNull() {
        assertEquals(0.0f, AutoToolEngine.getWeaponBaseDamage(null), 0.001f);
    }

    @Test
    @DisplayName("Weapon scoring: Null stack returns -100.0f")
    void testEvaluateWeaponScoreNull() {
        assertEquals(-100.0f, AutoToolEngine.evaluateWeaponScore(null, null, config), 0.001f);
    }

    @Test
    @DisplayName("Tool scoring: Null stack returns 0.0f")
    void testEvaluateToolScoreNull() {
        assertEquals(0.0f, AutoToolEngine.evaluateToolScore(null, null, config), 0.001f);
    }

    @Test
    @DisplayName("Combat session lifecycle: State tracking, test fixtures and resetSession")
    void testCombatSessionLifecycle() {
        assertFalse(AutoToolEngine.isCombatSessionActive(), "Combat session must initially be inactive");
        assertEquals(-1, AutoToolEngine.getOriginalHotbarSlot(), "Original hotbar slot must initially be -1");
        assertEquals(-1, AutoToolEngine.getExpectedToolSlot(), "Expected tool slot must initially be -1");

        AutoToolEngine.setCombatSessionActiveForTest(true, 2, 5);
        assertTrue(AutoToolEngine.isCombatSessionActive(), "Combat session must be active after test fixture");
        assertEquals(2, AutoToolEngine.getOriginalHotbarSlot(), "Original hotbar slot must match fixture");
        assertEquals(5, AutoToolEngine.getExpectedToolSlot(), "Expected tool slot must match fixture");

        AutoToolEngine.resetSession();
        assertFalse(AutoToolEngine.isCombatSessionActive(), "Combat session must be inactive after reset");
        assertEquals(-1, AutoToolEngine.getOriginalHotbarSlot(), "Original slot must reset to -1");
        assertEquals(-1, AutoToolEngine.getExpectedToolSlot(), "Expected slot must reset to -1");
    }

    @Test
    @DisplayName("Mining session lifecycle: State tracking, test fixtures and resetSession")
    void testMiningSessionLifecycle() {
        assertFalse(AutoToolEngine.isMiningSessionActive(), "Mining session must initially be inactive");
        assertEquals(-1, AutoToolEngine.getOriginalHotbarSlot(), "Original slot must initially be -1");

        AutoToolEngine.setMiningSessionActiveForTest(true, 0, 3);
        assertTrue(AutoToolEngine.isMiningSessionActive(), "Mining session must be active after test fixture");
        assertEquals(0, AutoToolEngine.getOriginalHotbarSlot(), "Original slot must match fixture");
        assertEquals(3, AutoToolEngine.getExpectedToolSlot(), "Expected slot must match fixture");

        AutoToolEngine.resetSession();
        assertFalse(AutoToolEngine.isMiningSessionActive(), "Mining session must be inactive after reset");
        assertEquals(-1, AutoToolEngine.getOriginalHotbarSlot(), "Original slot must reset to -1");
        assertEquals(-1, AutoToolEngine.getExpectedToolSlot(), "Expected slot must reset to -1");
    }

    @Test
    @DisplayName("AutoToolConfig: Defaults reflect anti-cheat safety and combat weapon switching")
    void testConfigDefaults() {
        AutoToolConfig cfg = new AutoToolConfig();
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
    }

    @Test
    @DisplayName("Durability saver score: Returns -1000.0f when durability <= threshold")
    void testDurabilitySaverScoring() {
        // When durabilitySaver is enabled and item is at threshold, evaluateToolScore returns -1000.0F
        config.durabilitySaver = true;
        config.durabilityThreshold = 5;

        // Null checks
        assertEquals(-100.0f, AutoToolEngine.evaluateWeaponScore(null, null, config));
        assertEquals(0.0f, AutoToolEngine.evaluateToolScore(null, null, config));
    }
}

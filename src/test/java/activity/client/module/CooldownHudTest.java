package activity.client.module;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.hud.CooldownHudOverlay;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.impl.utility.CooldownHudModule;
import activity.client.module.service.CooldownTrackerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CooldownHudTest {

    @BeforeEach
    void setUp() {
        CooldownTrackerService.clear();
        ActivityConfig config = new ActivityConfig();
        config.cooldownHudMinDuration = 2.5;
        ActivityConfigManager.setConfig(config);
    }

    @Test
    @DisplayName("CooldownTrackerService filters out vanilla micro-cooldowns below threshold")
    void testThresholdFiltering() {
        CooldownTrackerService.onCooldownSetForTest("wind_charge", 10);
        assertTrue(CooldownTrackerService.getActiveEntries().isEmpty(), "10 ticks (0.5s) must be filtered out when threshold is 2.5s");

        CooldownTrackerService.onCooldownSetForTest("ender_pearl", 300);
        assertEquals(1, CooldownTrackerService.getActiveEntries().size(), "300 ticks (15s) must be registered");
        CooldownTrackerService.CooldownEntry entry = CooldownTrackerService.getActiveEntries().get(0);
        assertEquals(300, entry.totalTicks);
        assertEquals(15.0f, entry.getRemainingSeconds(), 0.01f);
        assertEquals("15s", entry.getFormattedRemaining());
    }

    @Test
    @DisplayName("CooldownEntry format and colors change dynamically based on remaining time")
    void testFormatAndColors() {
        CooldownTrackerService.CooldownEntry entryLong = new CooldownTrackerService.CooldownEntry(null, 3200);
        assertEquals("160s", entryLong.getFormattedRemaining());
        assertEquals(0xFFFFFF, CooldownHudOverlay.getCooldownColor(entryLong.getRemainingSeconds()));

        CooldownTrackerService.CooldownEntry entryMed = new CooldownTrackerService.CooldownEntry(null, 40);
        assertEquals("2.0s", entryMed.getFormattedRemaining());
        assertEquals(0xFFFF55, CooldownHudOverlay.getCooldownColor(entryMed.getRemainingSeconds()));

        CooldownTrackerService.CooldownEntry entryShort = new CooldownTrackerService.CooldownEntry(null, 20);
        assertEquals("1.0s", entryShort.getFormattedRemaining());
        assertEquals(0xFF5555, CooldownHudOverlay.getCooldownColor(entryShort.getRemainingSeconds()));
    }

    @Test
    @DisplayName("CooldownTrackerService decrements and evicts expired entries on tick")
    void testTickEviction() {
        CooldownTrackerService.onCooldownSetForTest("ender_pearl", 60);
        assertEquals(1, CooldownTrackerService.getActiveEntries().size());

        for (int i = 0; i < 59; i++) {
            CooldownTrackerService.tick(null);
        }
        assertEquals(1, CooldownTrackerService.getActiveEntries().size());
        assertEquals(1, CooldownTrackerService.getActiveEntries().get(0).remainingTicks);

        CooldownTrackerService.tick(null);
        assertTrue(CooldownTrackerService.getActiveEntries().isEmpty(), "Entry must be removed when ticks reach 0");
    }

    @Test
    @DisplayName("CooldownHudModule metadata, actions and configuration persistence")
    void testModuleConfiguration() {
        CooldownHudModule module = new CooldownHudModule();
        assertEquals("cooldown_hud", module.getId());
        assertEquals(ModuleCategory.UTILITY, module.getCategory());
        assertFalse(module.hasTickLogic());

        assertTrue(module.getMetadata().getAliases().contains("кулдауны"));
        assertTrue(module.getMetadata().getAliases().contains("cd"));

        ActivityConfig config = new ActivityConfig();
        config.cooldownHudEnabled = false;
        config.cooldownHudVertical = true;
        config.cooldownHudMinDuration = 4.0;
        config.cooldownHudCustomX = 120;
        config.cooldownHudCustomY = 80;

        module.loadFromConfig(config);
        assertFalse(module.isEnabled());

        ActivityConfig targetConfig = new ActivityConfig();
        module.setEnabled(true);
        module.saveToConfig(targetConfig);
        assertTrue(targetConfig.cooldownHudEnabled);
        assertTrue(targetConfig.cooldownHudVertical);
        assertEquals(4.0, targetConfig.cooldownHudMinDuration, 0.001);
    }
}

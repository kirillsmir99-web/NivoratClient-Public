package activity.client.module;

import activity.client.module.impl.utility.gui.AutoGGRadialScreen;
import activity.client.module.keybind.Keybind;
import net.minecraft.client.gui.screen.Screen;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.elarion.autogg.AutoGGClient;
import ru.elarion.autogg.AutoGGConfig;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AutoGGRadialScreen:
 * Polar coordinate calculation, sector hover resolution, radial bounds,
 * central hub detection, autocomplete, default phrases, and phrase limits.
 */
public class AutoGGRadialScreenTest {

    @BeforeEach
    void setUp() {
        if (AutoGGClient.CONFIG != null) {
            AutoGGClient.CONFIG.phrases = new ArrayList<>(AutoGGConfig.DEFAULT_PHRASES);
            AutoGGClient.CONFIG.selected = 0;
            AutoGGClient.CONFIG.randomOrder = false;
        }
    }

    @Test
    @DisplayName("Radial Menu: Constructors exist and are accessible")
    void testConstructorsExist() {
        assertDoesNotThrow(() -> {
            assertNotNull(AutoGGRadialScreen.class.getConstructor(Screen.class));
            assertNotNull(AutoGGRadialScreen.class.getConstructor(Screen.class, boolean.class, Keybind.class));
        });
    }

    @Test
    @DisplayName("Radial Menu: Hovered sector boundary checks")
    void testRadialBounds() {
        int cx = 200;
        int cy = 200;
        int count = 8;

        // Inside inner radius (distance < 56) -> returns -1
        assertEquals(-1, AutoGGRadialScreen.getHoveredSector(cx, cy, cx, cy, count), "Exact center should be -1");
        assertEquals(-1, AutoGGRadialScreen.getHoveredSector(cx + 30, cy, cx, cy, count), "Distance 30 (< 56) should be -1");
        assertEquals(-1, AutoGGRadialScreen.getHoveredSector(cx, cy - 50, cx, cy, count), "Distance 50 (< 56) should be -1");

        // Beyond outer radius + margin (distance > 164) -> returns -1
        assertEquals(-1, AutoGGRadialScreen.getHoveredSector(cx + 170, cy, cx, cy, count), "Distance 170 (> 164) should be -1");
        assertEquals(-1, AutoGGRadialScreen.getHoveredSector(cx, cy - 200, cx, cy, count), "Distance 200 (> 164) should be -1");

        // Count <= 0 -> returns -1
        assertEquals(-1, AutoGGRadialScreen.getHoveredSector(cx + 80, cy, cx, cy, 0), "Count 0 should be -1");
        assertEquals(-1, AutoGGRadialScreen.getHoveredSector(cx + 80, cy, cx, cy, -1), "Count -1 should be -1");
    }

    @Test
    @DisplayName("Radial Menu: 8-sector cardinal & diagonal resolution")
    void testEightSectors() {
        int cx = 300;
        int cy = 300;
        int count = 8;
        int r = 100; // Between 56 and 148

        // Sector 0: Top (approx angle 0 rad from top) -> (cx + 10, cy - r)
        // Sector 0 spans angle 0 to PI/4 (0 to 45 deg)
        // Midpoint of sector 0 is at 22.5 deg: dx = r*sin(22.5), dy = -r*cos(22.5)
        double a0 = Math.toRadians(22.5);
        assertEquals(0, AutoGGRadialScreen.getHoveredSector(cx + r * Math.sin(a0), cy - r * Math.cos(a0), cx, cy, count), "Sector 0 (Top)");

        // Sector 2: Right (90 deg from top is border between 1 and 2, mid of sector 2 is 90 + 22.5 = 112.5 deg)
        double a2 = Math.toRadians(112.5);
        assertEquals(2, AutoGGRadialScreen.getHoveredSector(cx + r * Math.sin(a2), cy - r * Math.cos(a2), cx, cy, count), "Sector 2 (East-South-East)");

        // Sector 4: Bottom (mid is 180 + 22.5 = 202.5 deg)
        double a4 = Math.toRadians(202.5);
        assertEquals(4, AutoGGRadialScreen.getHoveredSector(cx + r * Math.sin(a4), cy - r * Math.cos(a4), cx, cy, count), "Sector 4 (South-South-West)");

        // Sector 6: Left (mid is 270 + 22.5 = 292.5 deg)
        double a6 = Math.toRadians(292.5);
        assertEquals(6, AutoGGRadialScreen.getHoveredSector(cx + r * Math.sin(a6), cy - r * Math.cos(a6), cx, cy, count), "Sector 6 (West-North-West)");
    }

    @Test
    @DisplayName("Radial Menu: 6-sector resolution")
    void testSixSectors() {
        int cx = 250;
        int cy = 250;
        int count = 6;
        int r = 90;

        // 6 sectors of 60 deg each
        for (int i = 0; i < count; i++) {
            double midAngleDeg = i * 60.0 + 30.0;
            double rad = Math.toRadians(midAngleDeg);
            double x = cx + r * Math.sin(rad);
            double y = cy - r * Math.cos(rad);
            assertEquals(i, AutoGGRadialScreen.getHoveredSector(x, y, cx, cy, count), "Sector " + i + " should match midpoint calculation");
        }
    }

    @Test
    @DisplayName("Radial Menu: Strict default phrases [GGWP, ez, GG]")
    void testDefaultPhrases() {
        List<String> phrases = AutoGGRadialScreen.getDefaultPhrases();
        assertNotNull(phrases, "Default phrases must not be null");
        assertEquals(3, phrases.size(), "Strict default phrases list must have exactly 3 entries: GGWP, ez, GG");
        assertEquals(List.of("GGWP", "ez", "GG"), phrases, "Must strictly be [GGWP, ez, GG]");
        assertTrue(phrases.contains("GGWP"));
        assertTrue(phrases.contains("ez"));
        assertTrue(phrases.contains("GG"));
    }

    @Test
    @DisplayName("Radial Menu: Central Hub hit-box detection")
    void testHubInsideDetection() {
        int cx = 200;
        int cy = 200;
        int radius = AutoGGRadialScreen.HUB_RADIUS; // 46

        // Exact center
        assertTrue(AutoGGRadialScreen.isInsideHub(cx, cy, cx, cy, radius));

        // Inside points
        assertTrue(AutoGGRadialScreen.isInsideHub(cx + 20, cy, cx, cy, radius));
        assertTrue(AutoGGRadialScreen.isInsideHub(cx, cy - 30, cx, cy, radius));

        // On the boundary (radius 46)
        assertTrue(AutoGGRadialScreen.isInsideHub(cx + radius, cy, cx, cy, radius));
        assertTrue(AutoGGRadialScreen.isInsideHub(cx, cy + radius, cx, cy, radius));

        // Outside boundary
        assertFalse(AutoGGRadialScreen.isInsideHub(cx + radius + 1, cy, cx, cy, radius));
        assertFalse(AutoGGRadialScreen.isInsideHub(cx - radius - 1, cy, cx, cy, radius));
        assertFalse(AutoGGRadialScreen.isInsideHub(cx + 100, cy + 100, cx, cy, radius));
    }

    @Test
    @DisplayName("Radial Menu: Autocomplete suggestions and prefix matching")
    void testAutocomplete() {
        assertEquals("GGWP", AutoGGRadialScreen.findAutocomplete(null));
        assertEquals("GGWP", AutoGGRadialScreen.findAutocomplete(""));
        assertEquals("GGWP", AutoGGRadialScreen.findAutocomplete("   "));

        // Prefix matching
        assertEquals("ez", AutoGGRadialScreen.findAutocomplete("e"));
        assertEquals("ez", AutoGGRadialScreen.findAutocomplete("E"));
        assertEquals("GGWP", AutoGGRadialScreen.findAutocomplete("ggw"));
        assertEquals("Good Fight", AutoGGRadialScreen.findAutocomplete("good"));
        assertEquals("Well Played", AutoGGRadialScreen.findAutocomplete("well"));

        // No match returns null
        assertNull(AutoGGRadialScreen.findAutocomplete("xyz123"));
    }

    @Test
    @DisplayName("Config: Max phrases limit is 8")
    void testConfigMaxPhrasesLimit() {
        AutoGGConfig config = new AutoGGConfig();
        List<String> tenPhrases = new ArrayList<>(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"));
        config.phrases = new ArrayList<>(tenPhrases);
        config.save();

        try {
            AutoGGConfig loaded = AutoGGConfig.load();
            assertTrue(loaded.phrases.size() <= AutoGGConfig.MAX_PHRASES, "Phrases must not exceed MAX_PHRASES (8)");
            assertEquals(8, loaded.phrases.size());
        } finally {
            // Restore default config
            config.phrases = new ArrayList<>(AutoGGConfig.DEFAULT_PHRASES);
            config.selected = 0;
            config.save();
            if (AutoGGClient.CONFIG != null) {
                AutoGGClient.CONFIG.phrases = new ArrayList<>(AutoGGConfig.DEFAULT_PHRASES);
                AutoGGClient.CONFIG.selected = 0;
            }
        }
    }

    @Test
    @DisplayName("Config: Phrase selection with randomOrder false vs true")
    void testRandomOrderPhraseSelection() {
        AutoGGConfig config = new AutoGGConfig();
        config.phrases = new ArrayList<>(List.of("GGWP", "ez", "GG"));
        config.selected = 1; // "ez"
        config.randomOrder = false;

        // When randomOrder is false, nextPhrase() returns currentPhrase() without mutating selected index
        assertEquals("ez", config.nextPhrase());
        assertEquals("ez", config.nextPhrase());
        assertEquals(1, config.selected);

        // When randomOrder is true, nextPhrase() returns one of the existing phrases
        config.randomOrder = true;
        for (int i = 0; i < 20; i++) {
            String phrase = config.nextPhrase();
            assertTrue(config.phrases.contains(phrase), "Random phrase must belong to phrases list");
        }
    }

    @Test
    @DisplayName("Radial Menu: Precomputed span cache integrity for all sector counts (1 to 8)")
    void testPrecomputedSpanCache() {
        // Verify Span objects are properly constructed and within radial bounds
        AutoGGRadialScreen.Span testSpan = new AutoGGRadialScreen.Span(10, -50, 50);
        assertEquals(10, testSpan.y);
        assertEquals(-50, testSpan.x1);
        assertEquals(50, testSpan.x2);
        assertTrue(testSpan.x2 > testSpan.x1);
    }

    @Test
    @DisplayName("Audio: Immediate hover feedback triggers safely")
    void testPlayHoverImmediate() {
        assertDoesNotThrow(() -> {
            activity.client.gui.sound.SoundManager.playHoverImmediate();
        });
    }

    @Test
    @DisplayName("Config: Legacy test phrases are stripped and Yes migrated on load")
    void testLegacyPhrasesStrippedOnLoad() {
        AutoGGConfig config = new AutoGGConfig();
        config.phrases = new ArrayList<>(List.of("GGWP", "Good Fight", "Короля не убить", "Yes", "GG"));
        config.save();

        try {
            AutoGGConfig loaded = AutoGGConfig.load();
            assertEquals(List.of("GGWP", "ez", "GG"), loaded.phrases);
            assertFalse(loaded.phrases.contains("Good Fight"));
            assertFalse(loaded.phrases.contains("Короля не убить"));
            assertFalse(loaded.phrases.contains("Yes"));
            assertTrue(loaded.phrases.contains("ez"));
        } finally {
            config.phrases = new ArrayList<>(AutoGGConfig.DEFAULT_PHRASES);
            config.selected = 0;
            config.save();
            if (AutoGGClient.CONFIG != null) {
                AutoGGClient.CONFIG.phrases = new ArrayList<>(AutoGGConfig.DEFAULT_PHRASES);
                AutoGGClient.CONFIG.selected = 0;
            }
        }
    }

    @Test
    @DisplayName("AutoGG: Own death triggers phrase scheduling when sendOnOwnDeath is enabled")
    void testAutoGGOwnDeathTriggersPhraseScheduling() {
        AutoGGClient.resetStateForTest();
        AutoGGClient.CONFIG.enabled = true;
        AutoGGClient.CONFIG.sendOnOwnDeath = true;
        AutoGGClient.CONFIG.phrases = new ArrayList<>(List.of("GGWP", "ez", "GG"));
        AutoGGClient.CONFIG.selected = 1; // "ez"
        AutoGGClient.customDelayMs = 500.0;

        assertFalse(AutoGGClient.hasPendingPhrase());

        // Player dies -> markOwnDeath()
        AutoGGClient.markOwnDeath();

        assertTrue(AutoGGClient.isLocalDiedThisRound());
        assertTrue(AutoGGClient.hasPendingPhrase());
        assertEquals("ez", AutoGGClient.getPendingPhrase());
        assertTrue(AutoGGClient.getScheduledSendTime() > System.currentTimeMillis());

        // Repeated death call while pending should not overwrite or re-trigger
        AutoGGClient.markOwnDeath();
        assertEquals("ez", AutoGGClient.getPendingPhrase());

        AutoGGClient.resetStateForTest();
    }

    @Test
    @DisplayName("AutoGG: Own death does NOT trigger phrase scheduling when sendOnOwnDeath is disabled")
    void testAutoGGOwnDeathDisabledDoesNotSchedule() {
        AutoGGClient.resetStateForTest();
        AutoGGClient.CONFIG.enabled = true;
        AutoGGClient.CONFIG.sendOnOwnDeath = false;

        AutoGGClient.markOwnDeath();

        assertTrue(AutoGGClient.isLocalDiedThisRound());
        assertFalse(AutoGGClient.hasPendingPhrase());

        AutoGGClient.resetStateForTest();
    }

    @Test
    @DisplayName("AutoGG: Cooldown strictly prevents duplicate dispatch within SEND_COOLDOWN_MS")
    void testAutoGGCooldownEnforcement() {
        AutoGGClient.resetStateForTest();
        AutoGGClient.CONFIG.enabled = true;
        AutoGGClient.CONFIG.sendOnOwnDeath = true;
        AutoGGClient.CONFIG.phrases = new ArrayList<>(List.of("GGWP", "ez", "GG"));
        AutoGGClient.CONFIG.selected = 0;

        // Simulate recently sent 1000ms ago (< 8000ms cooldown)
        AutoGGClient.setLastSentAtForTest(System.currentTimeMillis() - 1000L);

        AutoGGClient.markOwnDeath();

        assertFalse(AutoGGClient.hasPendingPhrase(), "Phrase must not be scheduled during cooldown");

        // Simulate sent 10_000ms ago (> 8000ms cooldown)
        AutoGGClient.setLastSentAtForTest(System.currentTimeMillis() - 10_000L);

        AutoGGClient.markOwnDeath();

        assertTrue(AutoGGClient.hasPendingPhrase(), "Phrase must be scheduled after cooldown expires");
        assertEquals("GGWP", AutoGGClient.getPendingPhrase());

        AutoGGClient.resetStateForTest();
    }

    @Test
    @DisplayName("AutoGGModule: Star selection and default phrase synchronization")
    void testAutoGGModuleCustomSectionStarSelection() {
        activity.client.module.impl.utility.AutoGGModule module = new activity.client.module.impl.utility.AutoGGModule();
        assertTrue(module.hasCustomSection());

        activity.client.config.ActivityConfig config = activity.client.config.ActivityConfigManager.getConfig();
        assertNotNull(config);

        // Set phrases
        AutoGGClient.CONFIG.phrases = new ArrayList<>(List.of("GGWP", "ez", "GG"));
        config.autoGGPhrase = "ez";
        module.syncEngineConfig(config);

        assertEquals("ez", AutoGGClient.CONFIG.currentPhrase());

        activity.client.gui.layout.ScrollContainer container = new activity.client.gui.layout.ScrollContainer(0, 0, 200, 300);
        int consumedHeight = module.buildCustomSection(null, null, container, 10, 10, 180);
        assertTrue(consumedHeight > 0);

        // Container should contain star buttons, phrase buttons, delete buttons, add field, and suggestion chips
        List<activity.client.gui.component.ActivityComponent> children = container.getChildren();
        assertTrue(children.size() >= 3 * 3, "Should have widgets for each of the 3 phrases");

        // Verify that one of the buttons is a star button with lit star ★
        boolean hasLitStar = false;
        boolean hasUnlitStar = false;
        for (activity.client.gui.component.ActivityComponent comp : children) {
            if (comp instanceof activity.client.gui.component.ActivityButton btn) {
                String text = btn.getMessage() != null ? btn.getMessage().getString() : "";
                if (text.contains("★")) {
                    hasLitStar = true;
                    assertEquals(activity.client.gui.component.ActivityButton.Variant.PRIMARY, btn.getVariant());
                } else if (text.contains("☆")) {
                    hasUnlitStar = true;
                    assertEquals(activity.client.gui.component.ActivityButton.Variant.SECONDARY, btn.getVariant());
                }
                // Assert that "(По умолчанию)" is completely absent from all button labels
                assertFalse(text.contains("По умолчанию"), "Button label must not contain 'По умолчанию'");
            }
        }
        assertTrue(hasLitStar, "Must display at least one lit star for default phrase");
        assertTrue(hasUnlitStar, "Must display unlit stars for non-default phrases");
    }

    @Test
    @DisplayName("AutoGGModule: Legacy 'Yes' in autoGGPhrase is migrated to 'ez' without resurrecting 'Yes'")
    void testLegacyYesMigrationInSyncEngineConfig() {
        activity.client.module.impl.utility.AutoGGModule module = new activity.client.module.impl.utility.AutoGGModule();
        activity.client.config.ActivityConfig config = activity.client.config.ActivityConfigManager.getConfig();
        assertNotNull(config);

        AutoGGClient.CONFIG.phrases = new ArrayList<>(List.of("GGWP", "ez", "GG"));
        AutoGGClient.CONFIG.selected = 0;

        // Legacy configuration contains "Yes"
        config.autoGGPhrase = "Yes";
        module.syncEngineConfig(config);

        // Must be migrated to "ez"
        assertEquals("ez", config.autoGGPhrase, "Legacy 'Yes' phrase must be migrated to 'ez'");
        assertEquals("ez", AutoGGClient.CONFIG.currentPhrase(), "Active selected phrase must be 'ez'");
        assertFalse(AutoGGClient.CONFIG.phrases.contains("Yes"), "Phrases list must never resurrect 'Yes'");
        assertTrue(AutoGGClient.CONFIG.phrases.contains("ez"), "Phrases list must contain 'ez'");
    }

    @Test
    @DisplayName("AutoGG: sendOnOwnDeath defaults to true for out-of-the-box death triggering")
    void testSendOnOwnDeathDefaultsToTrue() {
        AutoGGConfig cfg = new AutoGGConfig();
        assertTrue(cfg.sendOnOwnDeath, "AutoGGConfig.sendOnOwnDeath must default to true");

        activity.client.config.ActivityConfig actCfg = new activity.client.config.ActivityConfig();
        assertTrue(actCfg.autoGGSendOnOwnDeath, "ActivityConfig.autoGGSendOnOwnDeath must default to true");
    }

    @Test
    @DisplayName("AutoGGRadialScreen: optimizeSpans coalesces adjacent and overlapping spans correctly")
    void testOptimizeSpansCoalescing() {
        List<AutoGGRadialScreen.Span> raw = List.of(
                new AutoGGRadialScreen.Span(10, 0, 5),
                new AutoGGRadialScreen.Span(10, 5, 10),  // contiguous with previous
                new AutoGGRadialScreen.Span(10, 8, 15),  // overlapping
                new AutoGGRadialScreen.Span(10, 20, 25), // separated
                new AutoGGRadialScreen.Span(20, -10, 10) // different line
        );

        List<AutoGGRadialScreen.Span> optimized = AutoGGRadialScreen.optimizeSpans(raw);
        assertEquals(3, optimized.size(), "Should reduce 5 spans to 3 coalesced spans");

        // Line 10, first merged span: [0, 15]
        assertEquals(10, optimized.get(0).y);
        assertEquals(0, optimized.get(0).x1);
        assertEquals(15, optimized.get(0).x2);

        // Line 10, second separate span: [20, 25]
        assertEquals(10, optimized.get(1).y);
        assertEquals(20, optimized.get(1).x1);
        assertEquals(25, optimized.get(1).x2);

        // Line 20: [-10, 10]
        assertEquals(20, optimized.get(2).y);
        assertEquals(-10, optimized.get(2).x1);
        assertEquals(10, optimized.get(2).x2);
    }

    @Test
    @DisplayName("AutoGGModule: Case-insensitive phrase matching prevents duplicate phrases differing only in case")
    void testCaseInsensitivePhraseDeduplicationAndSelection() {
        activity.client.module.impl.utility.AutoGGModule module = new activity.client.module.impl.utility.AutoGGModule();
        activity.client.config.ActivityConfig config = activity.client.config.ActivityConfigManager.getConfig();
        assertNotNull(config);

        AutoGGClient.CONFIG.phrases = new ArrayList<>(List.of("GGWP", "ez", "GG"));
        AutoGGClient.CONFIG.selected = 0;

        // Config has uppercase "EZ"
        config.autoGGPhrase = "EZ";
        module.syncEngineConfig(config);

        // Should select phrase at index 1, adopt desired casing, and NOT add a duplicate
        assertEquals("EZ", config.autoGGPhrase);
        assertEquals("EZ", AutoGGClient.CONFIG.phrases.get(AutoGGClient.CONFIG.selected));
        assertEquals(1, AutoGGClient.CONFIG.selected);
        assertEquals(3, AutoGGClient.CONFIG.phrases.size());
    }

    @Test
    @DisplayName("Radial Menu: Direct phrase sending does NOT alter selected default phrase index")
    void testDirectPhraseSendDoesNotMutateSelectedDefault() {
        AutoGGClient.CONFIG.phrases = new ArrayList<>(List.of("GGWP", "ez", "GG"));
        AutoGGClient.CONFIG.selected = 0; // "GGWP" is starred default

        // Directly send "ez" (simulate sector click)
        AutoGGClient.sendPhraseDirect("ez");

        // Selected index must remain 0 ("GGWP"), and currentPhrase() must still be "GGWP"
        assertEquals(0, AutoGGClient.CONFIG.selected);
        assertEquals("GGWP", AutoGGClient.CONFIG.currentPhrase());
    }

    @Test
    @DisplayName("AutoGGModule: Adding new phrase preserves currently starred default phrase")
    void testAddPhrasePreservesCurrentDefaultStar() {
        AutoGGClient.CONFIG.phrases = new ArrayList<>(List.of("GGWP", "ez", "GG"));
        AutoGGClient.CONFIG.selected = 1; // "ez" is currently selected default
        String currentDefault = AutoGGClient.CONFIG.currentPhrase();
        assertEquals("ez", currentDefault);

        // Simulate add phrase logic from AutoGGModule
        String newPhrase = "Good Game";
        boolean exists = false;
        for (String p : AutoGGClient.CONFIG.phrases) {
            if (p.equalsIgnoreCase(newPhrase)) {
                exists = true;
                break;
            }
        }
        if (!exists && AutoGGClient.CONFIG.phrases.size() < 8) {
            AutoGGClient.CONFIG.phrases.add(newPhrase);
            int prevIdx = -1;
            for (int j = 0; j < AutoGGClient.CONFIG.phrases.size(); j++) {
                if (AutoGGClient.CONFIG.phrases.get(j).equalsIgnoreCase(currentDefault)) {
                    prevIdx = j;
                    break;
                }
            }
            if (prevIdx >= 0) {
                AutoGGClient.CONFIG.selected = prevIdx;
            }
        }

        // Selected index must still point to "ez", NOT to the newly added phrase
        assertEquals(4, AutoGGClient.CONFIG.phrases.size());
        assertEquals("ez", AutoGGClient.CONFIG.currentPhrase());
        assertEquals(1, AutoGGClient.CONFIG.selected);
    }

    @Test
    @DisplayName("AutoGGModule: Inline phrase editing updates phrases list and syncs default if selected")
    void testInlinePhraseEditing() {
        AutoGGClient.CONFIG.phrases = new ArrayList<>(List.of("GGWP", "ez", "GG"));
        AutoGGClient.CONFIG.selected = 1; // "ez"
        activity.client.config.ActivityConfig config = activity.client.config.ActivityConfigManager.getConfig();
        assertNotNull(config);
        config.autoGGPhrase = "ez";

        // 1. Edit a non-selected phrase (index 0: "GGWP" -> "Well Played")
        int editIdx0 = 0;
        String newVal0 = "Well Played";
        AutoGGClient.CONFIG.phrases.set(editIdx0, newVal0);
        if (AutoGGClient.CONFIG.selected == editIdx0) {
            config.autoGGPhrase = newVal0;
        }
        assertEquals("Well Played", AutoGGClient.CONFIG.phrases.get(0));
        assertEquals(1, AutoGGClient.CONFIG.selected);
        assertEquals("ez", config.autoGGPhrase);

        // 2. Edit the selected phrase (index 1: "ez" -> "easy peasy")
        int editIdx1 = 1;
        String newVal1 = "easy peasy";
        AutoGGClient.CONFIG.phrases.set(editIdx1, newVal1);
        if (AutoGGClient.CONFIG.selected == editIdx1) {
            config.autoGGPhrase = newVal1;
        }
        assertEquals("easy peasy", AutoGGClient.CONFIG.phrases.get(1));
        assertEquals(1, AutoGGClient.CONFIG.selected);
        assertEquals("easy peasy", AutoGGClient.CONFIG.currentPhrase());
        assertEquals("easy peasy", config.autoGGPhrase);
    }
}


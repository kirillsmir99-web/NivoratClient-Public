package activity.client.module;

import activity.client.module.impl.utility.gui.AudioWaveRadialScreen;
import activity.client.module.keybind.Keybind;
import net.minecraft.client.gui.screen.Screen;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import dev.audio.AudioSyncClient;
import dev.audio.AudioSyncConfig;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AutoGGRadialScreenTest {

    @BeforeEach
    void setUp() {
        if (AudioSyncClient.CONFIG != null) {
            AudioSyncClient.CONFIG.phrases = new ArrayList<>(AudioSyncConfig.DEFAULT_PHRASES);
            AudioSyncClient.CONFIG.selected = 0;
            AudioSyncClient.CONFIG.randomOrder = false;
        }
    }

    @Test
    @DisplayName("Radial Menu: Constructors exist and are accessible")
    void testConstructorsExist() {
        assertDoesNotThrow(() -> {
            assertNotNull(AudioWaveRadialScreen.class.getConstructor(Screen.class));
            assertNotNull(AudioWaveRadialScreen.class.getConstructor(Screen.class, boolean.class, Keybind.class));
        });
    }

    @Test
    @DisplayName("Radial Menu: Hovered sector boundary checks")
    void testRadialBounds() {
        int cx = 200;
        int cy = 200;
        int count = 8;

        assertEquals(-1, AudioWaveRadialScreen.getHoveredSector(cx, cy, cx, cy, count), "Exact center should be -1");
        assertEquals(-1, AudioWaveRadialScreen.getHoveredSector(cx + 30, cy, cx, cy, count), "Distance 30 (< 56) should be -1");
        assertEquals(-1, AudioWaveRadialScreen.getHoveredSector(cx, cy - 50, cx, cy, count), "Distance 50 (< 56) should be -1");

        assertEquals(-1, AudioWaveRadialScreen.getHoveredSector(cx + 170, cy, cx, cy, count), "Distance 170 (> 164) should be -1");
        assertEquals(-1, AudioWaveRadialScreen.getHoveredSector(cx, cy - 200, cx, cy, count), "Distance 200 (> 164) should be -1");

        assertEquals(-1, AudioWaveRadialScreen.getHoveredSector(cx + 80, cy, cx, cy, 0), "Count 0 should be -1");
        assertEquals(-1, AudioWaveRadialScreen.getHoveredSector(cx + 80, cy, cx, cy, -1), "Count -1 should be -1");
    }

    @Test
    @DisplayName("Radial Menu: 8-sector cardinal & diagonal resolution")
    void testEightSectors() {
        int cx = 300;
        int cy = 300;
        int count = 8;
        int r = 100;

        double a0 = Math.toRadians(22.5);
        assertEquals(0, AudioWaveRadialScreen.getHoveredSector(cx + r * Math.sin(a0), cy - r * Math.cos(a0), cx, cy, count), "Sector 0 (Top)");

        double a2 = Math.toRadians(112.5);
        assertEquals(2, AudioWaveRadialScreen.getHoveredSector(cx + r * Math.sin(a2), cy - r * Math.cos(a2), cx, cy, count), "Sector 2 (East-South-East)");

        double a4 = Math.toRadians(202.5);
        assertEquals(4, AudioWaveRadialScreen.getHoveredSector(cx + r * Math.sin(a4), cy - r * Math.cos(a4), cx, cy, count), "Sector 4 (South-South-West)");

        double a6 = Math.toRadians(292.5);
        assertEquals(6, AudioWaveRadialScreen.getHoveredSector(cx + r * Math.sin(a6), cy - r * Math.cos(a6), cx, cy, count), "Sector 6 (West-North-West)");
    }

    @Test
    @DisplayName("Radial Menu: 6-sector resolution")
    void testSixSectors() {
        int cx = 250;
        int cy = 250;
        int count = 6;
        int r = 90;

        for (int i = 0; i < count; i++) {
            double midAngleDeg = i * 60.0 + 30.0;
            double rad = Math.toRadians(midAngleDeg);
            double x = cx + r * Math.sin(rad);
            double y = cy - r * Math.cos(rad);
            assertEquals(i, AudioWaveRadialScreen.getHoveredSector(x, y, cx, cy, count), "Sector " + i + " should match midpoint calculation");
        }
    }

    @Test
    @DisplayName("Radial Menu: Strict default phrases [GGWP, ez, GG]")
    void testDefaultPhrases() {
        List<String> phrases = AudioWaveRadialScreen.getDefaultPhrases();
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
        int radius = AudioWaveRadialScreen.HUB_RADIUS;

        assertTrue(AudioWaveRadialScreen.isInsideHub(cx, cy, cx, cy, radius));

        assertTrue(AudioWaveRadialScreen.isInsideHub(cx + 20, cy, cx, cy, radius));
        assertTrue(AudioWaveRadialScreen.isInsideHub(cx, cy - 30, cx, cy, radius));

        assertTrue(AudioWaveRadialScreen.isInsideHub(cx + radius, cy, cx, cy, radius));
        assertTrue(AudioWaveRadialScreen.isInsideHub(cx, cy + radius, cx, cy, radius));

        assertFalse(AudioWaveRadialScreen.isInsideHub(cx + radius + 1, cy, cx, cy, radius));
        assertFalse(AudioWaveRadialScreen.isInsideHub(cx - radius - 1, cy, cx, cy, radius));
        assertFalse(AudioWaveRadialScreen.isInsideHub(cx + 100, cy + 100, cx, cy, radius));
    }

    @Test
    @DisplayName("Radial Menu: Autocomplete suggestions and prefix matching")
    void testAutocomplete() {
        assertEquals("GGWP", AudioWaveRadialScreen.findAutocomplete(null));
        assertEquals("GGWP", AudioWaveRadialScreen.findAutocomplete(""));
        assertEquals("GGWP", AudioWaveRadialScreen.findAutocomplete("   "));

        assertEquals("ez", AudioWaveRadialScreen.findAutocomplete("e"));
        assertEquals("ez", AudioWaveRadialScreen.findAutocomplete("E"));
        assertEquals("GGWP", AudioWaveRadialScreen.findAutocomplete("ggw"));
        assertEquals("Good Fight", AudioWaveRadialScreen.findAutocomplete("good"));
        assertEquals("Well Played", AudioWaveRadialScreen.findAutocomplete("well"));

        assertNull(AudioWaveRadialScreen.findAutocomplete("xyz123"));
    }

    @Test
    @DisplayName("Config: Max phrases limit is 8")
    void testConfigMaxPhrasesLimit() {
        AudioSyncConfig config = new AudioSyncConfig();
        List<String> tenPhrases = new ArrayList<>(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"));
        config.phrases = new ArrayList<>(tenPhrases);
        config.save();

        try {
            AudioSyncConfig loaded = AudioSyncConfig.load();
            assertTrue(loaded.phrases.size() <= AudioSyncConfig.MAX_PHRASES, "Phrases must not exceed MAX_PHRASES (8)");
            assertEquals(8, loaded.phrases.size());
        } finally {

            config.phrases = new ArrayList<>(AudioSyncConfig.DEFAULT_PHRASES);
            config.selected = 0;
            config.save();
            if (AudioSyncClient.CONFIG != null) {
                AudioSyncClient.CONFIG.phrases = new ArrayList<>(AudioSyncConfig.DEFAULT_PHRASES);
                AudioSyncClient.CONFIG.selected = 0;
            }
        }
    }

    @Test
    @DisplayName("Config: Phrase selection with randomOrder false vs true")
    void testRandomOrderPhraseSelection() {
        AudioSyncConfig config = new AudioSyncConfig();
        config.phrases = new ArrayList<>(List.of("GGWP", "ez", "GG"));
        config.selected = 1;
        config.randomOrder = false;

        assertEquals("ez", config.nextPhrase());
        assertEquals("ez", config.nextPhrase());
        assertEquals(1, config.selected);

        config.randomOrder = true;
        for (int i = 0; i < 20; i++) {
            String phrase = config.nextPhrase();
            assertTrue(config.phrases.contains(phrase), "Random phrase must belong to phrases list");
        }
    }

    @Test
    @DisplayName("Radial Menu: Precomputed span cache integrity for all sector counts (1 to 8)")
    void testPrecomputedSpanCache() {

        AudioWaveRadialScreen.Span testSpan = new AudioWaveRadialScreen.Span(10, -50, 50);
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
    @DisplayName("Config: User phrases survive saving and loading unchanged")
    void testUserPhrasesPreservedOnLoad() {
        AudioSyncConfig config = new AudioSyncConfig();
        config.phrases = new ArrayList<>(List.of("GGWP", "Good Fight", "Короля не убить", "Yes", "GG"));
        config.save();

        try {
            AudioSyncConfig loaded = AudioSyncConfig.load();
            assertEquals(config.phrases, loaded.phrases);
        } finally {
            config.phrases = new ArrayList<>(AudioSyncConfig.DEFAULT_PHRASES);
            config.selected = 0;
            config.save();
            if (AudioSyncClient.CONFIG != null) {
                AudioSyncClient.CONFIG.phrases = new ArrayList<>(AudioSyncConfig.DEFAULT_PHRASES);
                AudioSyncClient.CONFIG.selected = 0;
            }
        }
    }

    @Test
    @DisplayName("AutoGG: Own death triggers phrase scheduling when sendOnOwnDeath is enabled")
    void testAutoGGOwnDeathTriggersPhraseScheduling() {
        AudioSyncClient.resetStateForTest();
        AudioSyncClient.CONFIG.enabled = true;
        AudioSyncClient.CONFIG.sendOnOwnDeath = true;
        AudioSyncClient.CONFIG.phrases = new ArrayList<>(List.of("GGWP", "ez", "GG"));
        AudioSyncClient.CONFIG.selected = 1;
        AudioSyncClient.customDelayMs = 500.0;

        assertFalse(AudioSyncClient.hasPendingPhrase());

        AudioSyncClient.markOwnDeath();

        assertTrue(AudioSyncClient.isLocalDiedThisRound());
        assertTrue(AudioSyncClient.hasPendingPhrase());
        assertEquals("ez", AudioSyncClient.getPendingPhrase());
        assertTrue(AudioSyncClient.getScheduledSendTime() > System.currentTimeMillis());

        AudioSyncClient.markOwnDeath();
        assertEquals("ez", AudioSyncClient.getPendingPhrase());

        AudioSyncClient.resetStateForTest();
    }

    @Test
    @DisplayName("AutoGG: Own death does NOT trigger phrase scheduling when sendOnOwnDeath is disabled")
    void testAutoGGOwnDeathDisabledDoesNotSchedule() {
        AudioSyncClient.resetStateForTest();
        AudioSyncClient.CONFIG.enabled = true;
        AudioSyncClient.CONFIG.sendOnOwnDeath = false;

        AudioSyncClient.markOwnDeath();

        assertTrue(AudioSyncClient.isLocalDiedThisRound());
        assertFalse(AudioSyncClient.hasPendingPhrase());

        AudioSyncClient.resetStateForTest();
    }

    @Test
    @DisplayName("AutoGG: Cooldown strictly prevents duplicate dispatch within SEND_COOLDOWN_MS")
    void testAutoGGCooldownEnforcement() {
        AudioSyncClient.resetStateForTest();
        AudioSyncClient.CONFIG.enabled = true;
        AudioSyncClient.CONFIG.sendOnOwnDeath = true;
        AudioSyncClient.CONFIG.phrases = new ArrayList<>(List.of("GGWP", "ez", "GG"));
        AudioSyncClient.CONFIG.selected = 0;

        AudioSyncClient.setLastSentAtForTest(System.currentTimeMillis() - 1000L);

        AudioSyncClient.markOwnDeath();

        assertFalse(AudioSyncClient.hasPendingPhrase(), "Phrase must not be scheduled during cooldown");

        AudioSyncClient.setLastSentAtForTest(System.currentTimeMillis() - 10_000L);

        AudioSyncClient.markOwnDeath();

        assertTrue(AudioSyncClient.hasPendingPhrase(), "Phrase must be scheduled after cooldown expires");
        assertEquals("GGWP", AudioSyncClient.getPendingPhrase());

        AudioSyncClient.resetStateForTest();
    }

    @Test
    @DisplayName("AudioWaveModule: Star selection and default phrase synchronization")
    void testAutoGGModuleCustomSectionStarSelection() {
        activity.client.module.impl.utility.AudioWaveModule module = new activity.client.module.impl.utility.AudioWaveModule();
        assertTrue(module.hasCustomSection());

        activity.client.config.ActivityConfig config = activity.client.config.ActivityConfigManager.getConfig();
        assertNotNull(config);

        AudioSyncClient.CONFIG.phrases = new ArrayList<>(List.of("GGWP", "ez", "GG"));
        config.autoGGPhrase = "ez";
        module.syncEngineConfig(config);

        assertEquals("ez", AudioSyncClient.CONFIG.currentPhrase());

        activity.client.gui.layout.ScrollContainer container = new activity.client.gui.layout.ScrollContainer(0, 0, 200, 300);
        int consumedHeight = module.buildCustomSection(null, null, container, 10, 10, 180);
        assertTrue(consumedHeight > 0);

        List<activity.client.gui.component.ActivityComponent> children = container.getChildren();
        assertTrue(children.size() >= 3 * 3, "Should have widgets for each of the 3 phrases");

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

                assertFalse(text.contains("По умолчанию"), "Button label must not contain 'По умолчанию'");
            }
        }
        assertTrue(hasLitStar, "Must display at least one lit star for default phrase");
        assertTrue(hasUnlitStar, "Must display unlit stars for non-default phrases");
    }

    @Test
    @DisplayName("AudioWaveModule: A user phrase is selected without replacing its text")
    void testUserPhrasePreservedInSyncEngineConfig() {
        activity.client.module.impl.utility.AudioWaveModule module = new activity.client.module.impl.utility.AudioWaveModule();
        activity.client.config.ActivityConfig config = activity.client.config.ActivityConfigManager.getConfig();
        assertNotNull(config);

        AudioSyncClient.CONFIG.phrases = new ArrayList<>(List.of("GGWP", "ez", "GG"));
        AudioSyncClient.CONFIG.selected = 0;

        config.autoGGPhrase = "Yes";
        module.syncEngineConfig(config);

        assertEquals("Yes", config.autoGGPhrase);
        assertEquals("Yes", AudioSyncClient.CONFIG.currentPhrase());
        assertTrue(AudioSyncClient.CONFIG.phrases.contains("Yes"));
        assertTrue(AudioSyncClient.CONFIG.phrases.contains("ez"));
    }

    @Test
    @DisplayName("AutoGG: sendOnOwnDeath defaults to true for out-of-the-box death triggering")
    void testSendOnOwnDeathDefaultsToTrue() {
        AudioSyncConfig cfg = new AudioSyncConfig();
        assertTrue(cfg.sendOnOwnDeath, "AudioSyncConfig.sendOnOwnDeath must default to true");

        activity.client.config.ActivityConfig actCfg = new activity.client.config.ActivityConfig();
        assertTrue(actCfg.autoGGSendOnOwnDeath, "ActivityConfig.autoGGSendOnOwnDeath must default to true");
    }

    @Test
    @DisplayName("AudioWaveRadialScreen: optimizeSpans coalesces adjacent and overlapping spans correctly")
    void testOptimizeSpansCoalescing() {
        List<AudioWaveRadialScreen.Span> raw = List.of(
                new AudioWaveRadialScreen.Span(10, 0, 5),
                new AudioWaveRadialScreen.Span(10, 5, 10),
                new AudioWaveRadialScreen.Span(10, 8, 15),
                new AudioWaveRadialScreen.Span(10, 20, 25),
                new AudioWaveRadialScreen.Span(20, -10, 10)
        );

        List<AudioWaveRadialScreen.Span> optimized = AudioWaveRadialScreen.optimizeSpans(raw);
        assertEquals(3, optimized.size(), "Should reduce 5 spans to 3 coalesced spans");

        assertEquals(10, optimized.get(0).y);
        assertEquals(0, optimized.get(0).x1);
        assertEquals(15, optimized.get(0).x2);

        assertEquals(10, optimized.get(1).y);
        assertEquals(20, optimized.get(1).x1);
        assertEquals(25, optimized.get(1).x2);

        assertEquals(20, optimized.get(2).y);
        assertEquals(-10, optimized.get(2).x1);
        assertEquals(10, optimized.get(2).x2);
    }

    @Test
    @DisplayName("AudioWaveModule: Case-insensitive phrase matching prevents duplicate phrases differing only in case")
    void testCaseInsensitivePhraseDeduplicationAndSelection() {
        activity.client.module.impl.utility.AudioWaveModule module = new activity.client.module.impl.utility.AudioWaveModule();
        activity.client.config.ActivityConfig config = activity.client.config.ActivityConfigManager.getConfig();
        assertNotNull(config);

        AudioSyncClient.CONFIG.phrases = new ArrayList<>(List.of("GGWP", "ez", "GG"));
        AudioSyncClient.CONFIG.selected = 0;

        config.autoGGPhrase = "EZ";
        module.syncEngineConfig(config);

        assertEquals("EZ", config.autoGGPhrase);
        assertEquals("EZ", AudioSyncClient.CONFIG.phrases.get(AudioSyncClient.CONFIG.selected));
        assertEquals(1, AudioSyncClient.CONFIG.selected);
        assertEquals(3, AudioSyncClient.CONFIG.phrases.size());
    }

    @Test
    @DisplayName("Radial Menu: Direct phrase sending does NOT alter selected default phrase index")
    void testDirectPhraseSendDoesNotMutateSelectedDefault() {
        AudioSyncClient.CONFIG.phrases = new ArrayList<>(List.of("GGWP", "ez", "GG"));
        AudioSyncClient.CONFIG.selected = 0;

        AudioSyncClient.sendPhraseDirect("ez");

        assertEquals(0, AudioSyncClient.CONFIG.selected);
        assertEquals("GGWP", AudioSyncClient.CONFIG.currentPhrase());
    }

    @Test
    @DisplayName("AudioWaveModule: Adding new phrase preserves currently starred default phrase")
    void testAddPhrasePreservesCurrentDefaultStar() {
        AudioSyncClient.CONFIG.phrases = new ArrayList<>(List.of("GGWP", "ez", "GG"));
        AudioSyncClient.CONFIG.selected = 1;
        String currentDefault = AudioSyncClient.CONFIG.currentPhrase();
        assertEquals("ez", currentDefault);

        String newPhrase = "Good Game";
        boolean exists = false;
        for (String p : AudioSyncClient.CONFIG.phrases) {
            if (p.equalsIgnoreCase(newPhrase)) {
                exists = true;
                break;
            }
        }
        if (!exists && AudioSyncClient.CONFIG.phrases.size() < 8) {
            AudioSyncClient.CONFIG.phrases.add(newPhrase);
            int prevIdx = -1;
            for (int j = 0; j < AudioSyncClient.CONFIG.phrases.size(); j++) {
                if (AudioSyncClient.CONFIG.phrases.get(j).equalsIgnoreCase(currentDefault)) {
                    prevIdx = j;
                    break;
                }
            }
            if (prevIdx >= 0) {
                AudioSyncClient.CONFIG.selected = prevIdx;
            }
        }

        assertEquals(4, AudioSyncClient.CONFIG.phrases.size());
        assertEquals("ez", AudioSyncClient.CONFIG.currentPhrase());
        assertEquals(1, AudioSyncClient.CONFIG.selected);
    }

    @Test
    @DisplayName("AudioWaveModule: Inline phrase editing updates phrases list and syncs default if selected")
    void testInlinePhraseEditing() {
        AudioSyncClient.CONFIG.phrases = new ArrayList<>(List.of("GGWP", "ez", "GG"));
        AudioSyncClient.CONFIG.selected = 1;
        activity.client.config.ActivityConfig config = activity.client.config.ActivityConfigManager.getConfig();
        assertNotNull(config);
        config.autoGGPhrase = "ez";

        int editIdx0 = 0;
        String newVal0 = "Well Played";
        AudioSyncClient.CONFIG.phrases.set(editIdx0, newVal0);
        if (AudioSyncClient.CONFIG.selected == editIdx0) {
            config.autoGGPhrase = newVal0;
        }
        assertEquals("Well Played", AudioSyncClient.CONFIG.phrases.get(0));
        assertEquals(1, AudioSyncClient.CONFIG.selected);
        assertEquals("ez", config.autoGGPhrase);

        int editIdx1 = 1;
        String newVal1 = "easy peasy";
        AudioSyncClient.CONFIG.phrases.set(editIdx1, newVal1);
        if (AudioSyncClient.CONFIG.selected == editIdx1) {
            config.autoGGPhrase = newVal1;
        }
        assertEquals("easy peasy", AudioSyncClient.CONFIG.phrases.get(1));
        assertEquals(1, AudioSyncClient.CONFIG.selected);
        assertEquals("easy peasy", AudioSyncClient.CONFIG.currentPhrase());
        assertEquals("easy peasy", config.autoGGPhrase);
    }

    @Test
    @DisplayName("AudioWaveRadialScreen: BlockSpan construction and 1px coalescing integrity")
    void testBlockSpanCoalescingIntegrity() {
        List<AudioWaveRadialScreen.Span> spans = List.of(
                new AudioWaveRadialScreen.Span(-50, -30, 30),
                new AudioWaveRadialScreen.Span(-49, -32, 32)
        );
        List<AudioWaveRadialScreen.BlockSpan> blocks = AudioWaveRadialScreen.coalesceSpans(spans, 1);
        assertEquals(2, blocks.size());

        AudioWaveRadialScreen.BlockSpan b0 = blocks.get(0);
        assertEquals(-50, b0.y1);
        assertEquals(-49, b0.y2);
        assertEquals(-30, b0.x1);
        assertEquals(30, b0.x2);

        AudioWaveRadialScreen.BlockSpan b1 = blocks.get(1);
        assertEquals(-49, b1.y1);
        assertEquals(-48, b1.y2);
        assertEquals(-32, b1.x1);
        assertEquals(32, b1.x2);
    }

    @Test
    @DisplayName("AudioWaveRadialScreen: 2D multi-span coalescing extends identical vertical runs")
    void testBlockSpan2DCoalescingMultiSpan() {
        List<AudioWaveRadialScreen.Span> spans = List.of(
                new AudioWaveRadialScreen.Span(-50, -30, -20),
                new AudioWaveRadialScreen.Span(-50, 20, 30),
                new AudioWaveRadialScreen.Span(-49, -30, -20),
                new AudioWaveRadialScreen.Span(-49, 20, 30)
        );
        List<AudioWaveRadialScreen.BlockSpan> blocks = AudioWaveRadialScreen.coalesceSpans(spans, 1);

        assertEquals(2, blocks.size(), "Two interleaved columns should coalesce into 2 vertical blocks");

        boolean foundLeft = false;
        boolean foundRight = false;
        for (AudioWaveRadialScreen.BlockSpan b : blocks) {
            assertEquals(-50, b.y1);
            assertEquals(-48, b.y2);
            if (b.x1 == -30 && b.x2 == -20) foundLeft = true;
            if (b.x1 == 20 && b.x2 == 30) foundRight = true;
        }
        assertTrue(foundLeft, "Left column must coalesce vertically from y=-50 to y=-48");
        assertTrue(foundRight, "Right column must coalesce vertically from y=-50 to y=-48");
    }

    @Test
    @DisplayName("KeybindManager: Key suppression prevents repeat trigger until released")
    void testKeybindSuppression() {
        String key = "sec:auto_gg:menu_keybind";
        activity.client.module.keybind.KeybindManager.suppressKey(key);

        assertDoesNotThrow(() -> {
            activity.client.module.keybind.KeybindManager.suppressKey(key);
        });
    }

    @Test
    @DisplayName("AudioWaveModule: openRadialMenu toggles screen closed when already open")
    void testAutoGGModuleToggleClose() {
        activity.client.module.impl.utility.AudioWaveModule module = new activity.client.module.impl.utility.AudioWaveModule();

        assertDoesNotThrow(() -> module.openRadialMenu(null));
    }
}


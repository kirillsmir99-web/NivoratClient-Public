package activity.client.module;

import activity.client.module.impl.utility.gui.AutoGGRadialScreen;
import activity.client.module.keybind.Keybind;
import net.minecraft.client.gui.screen.Screen;
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
    @DisplayName("Radial Menu: Strict default phrases [GGWP, Yes, GG]")
    void testDefaultPhrases() {
        List<String> phrases = AutoGGRadialScreen.getDefaultPhrases();
        assertNotNull(phrases, "Default phrases must not be null");
        assertEquals(3, phrases.size(), "Strict default phrases list must have exactly 3 entries: GGWP, Yes, GG");
        assertEquals(List.of("GGWP", "Yes", "GG"), phrases, "Must strictly be [GGWP, Yes, GG]");
        assertTrue(phrases.contains("GGWP"));
        assertTrue(phrases.contains("Yes"));
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
        assertEquals("Yes", AutoGGRadialScreen.findAutocomplete("ye"));
        assertEquals("Yes", AutoGGRadialScreen.findAutocomplete("Y"));
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
        config.phrases = new ArrayList<>(List.of("GGWP", "Yes", "GG"));
        config.selected = 1; // "Yes"
        config.randomOrder = false;

        // When randomOrder is false, nextPhrase() returns currentPhrase() without mutating selected index
        assertEquals("Yes", config.nextPhrase());
        assertEquals("Yes", config.nextPhrase());
        assertEquals(1, config.selected);

        // When randomOrder is true, nextPhrase() returns one of the existing phrases
        config.randomOrder = true;
        for (int i = 0; i < 20; i++) {
            String phrase = config.nextPhrase();
            assertTrue(config.phrases.contains(phrase), "Random phrase must belong to phrases list");
        }
    }
}

package activity.client.gui.font;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class NivoratFontSystemTest {

    @BeforeEach
    void setUp() {
        NivoratFontManager.setFont(FontFamily.ONEST, TypographySize.NORMAL, false);
    }

    @Test
    void testFontFamilyResolutionAndFallback() {
        assertEquals(FontFamily.ONEST, FontFamily.fromId("onest"));
        assertEquals(FontFamily.INTER, FontFamily.fromId("inter"));
        assertEquals(FontFamily.MANROPE, FontFamily.fromId("manrope"));
        assertEquals(FontFamily.RUBIK, FontFamily.fromId("rubik"));
        assertEquals(FontFamily.MINECRAFT, FontFamily.fromId("minecraft"));
        assertEquals(FontFamily.MINECRAFT, FontFamily.fromId("default"));

        assertEquals(FontFamily.MINECRAFT, FontFamily.fromId(null));
        assertEquals(FontFamily.MINECRAFT, FontFamily.fromId(""));
        assertEquals(FontFamily.MINECRAFT, FontFamily.fromId("unknown_font_123"));
    }

    @Test
    void testTypographySizeResolutionAndScaling() {
        assertEquals(TypographySize.SMALL, TypographySize.fromId("small"));
        assertEquals(TypographySize.NORMAL, TypographySize.fromId("normal"));
        assertEquals(TypographySize.LARGE, TypographySize.fromId("large"));

        assertEquals(TypographySize.NORMAL, TypographySize.fromId(null));
        assertEquals(TypographySize.NORMAL, TypographySize.fromId("invalid_size"));

        assertTrue(TypographySize.SMALL.getScaleFactor() < TypographySize.NORMAL.getScaleFactor());
        assertTrue(TypographySize.LARGE.getScaleFactor() > TypographySize.NORMAL.getScaleFactor());
        assertEquals(1.00f, TypographySize.NORMAL.getScaleFactor(), 0.001f);
    }

    @Test
    void testTypographyMetricsMatrix() {
        for (FontFamily family : FontFamily.values()) {
            for (TypographySize size : TypographySize.values()) {
                TypographyMetrics metrics = TypographyMetrics.get(family, size);
                assertNotNull(metrics, "Metrics must exist for " + family + " at size " + size);
                assertTrue(metrics.getLineHeight() > 0, "Line height must be positive");
                assertTrue(metrics.getBaseline() > 0, "Baseline must be positive");
                assertEquals(size.getScaleFactor(), metrics.getScaleFactor(), 0.001f);

                int centerY = metrics.getCenterY(10, 20);
                assertTrue(centerY >= 10 && centerY <= 30, "Center Y must be inside parent bounds");
            }
        }

        TypographyMetrics small = TypographyMetrics.get(FontFamily.ONEST, TypographySize.SMALL);
        TypographyMetrics normal = TypographyMetrics.get(FontFamily.ONEST, TypographySize.NORMAL);
        TypographyMetrics large = TypographyMetrics.get(FontFamily.ONEST, TypographySize.LARGE);

        assertTrue(small.getLineHeight() <= normal.getLineHeight());
        assertTrue(large.getLineHeight() >= normal.getLineHeight());
    }

    @Test
    void testRuntimeFontSwitchingAndListenerNotification() {
        AtomicBoolean listenerNotified = new AtomicBoolean(false);
        Runnable listener = () -> listenerNotified.set(true);

        NivoratFontManager.addListener(listener);
        try {
            NivoratFontManager.setFontFamily(FontFamily.INTER, false);
            assertTrue(listenerNotified.get(), "Listener should be notified upon font family switch");
            assertEquals(FontFamily.INTER, NivoratFontManager.getActiveFontFamily());
            assertEquals("inter", NivoratFontManager.getActiveFamily());

            listenerNotified.set(false);
            NivoratFontManager.setTypographySize(TypographySize.LARGE, false);
            assertTrue(listenerNotified.get(), "Listener should be notified upon typography size switch");
            assertEquals(TypographySize.LARGE, NivoratFontManager.getActiveTypographySize());
        } finally {
            NivoratFontManager.removeListener(listener);
        }
    }

    @Test
    void testFontAvailabilityFallback() {

        assertTrue(NivoratFontManager.isFontAvailable(FontFamily.MINECRAFT));
        assertTrue(NivoratFontManager.isFontAvailable(FontFamily.DEFAULT));

        NivoratFontManager.setFontFamily(FontFamily.MINECRAFT, false);
        assertEquals(FontFamily.MINECRAFT, NivoratFontManager.getActiveFontFamily());
    }

    @Test
    void testCyrillicAndSpecialCharacterStrings() {
        String cyrillicUpper = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ";
        String cyrillicLower = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя";
        String symbolsAndDigits = "0123456789 () % @ # ! - + =";
        String longSetting = "Автоматический выбор наилучшего инструмента при разрушении блоков (100.0%)";

        for (FontFamily family : List.of(FontFamily.ONEST, FontFamily.INTER, FontFamily.MANROPE, FontFamily.RUBIK, FontFamily.MINECRAFT)) {
            assertNotNull(NivoratFontManager.wrap(net.minecraft.text.Text.literal(cyrillicUpper), family));
            assertNotNull(NivoratFontManager.wrap(net.minecraft.text.Text.literal(cyrillicLower), family));
            assertNotNull(NivoratFontManager.wrap(net.minecraft.text.Text.literal(symbolsAndDigits), family));
            assertNotNull(NivoratFontManager.wrap(net.minecraft.text.Text.literal(longSetting), family));
        }
    }

    @Test
    void testFontLocalizationPresence() {
        InputStream isRu = getClass().getResourceAsStream("/assets/activity/lang/ru_ru.json");
        assertNotNull(isRu, "ru_ru.json must exist in resources");
        JsonObject ru = new Gson().fromJson(new InputStreamReader(isRu, StandardCharsets.UTF_8), JsonObject.class);

        assertTrue(ru.has("activity.font.minecraft"));
        assertTrue(ru.has("activity.font.onest"));
        assertTrue(ru.has("activity.font.inter"));
        assertTrue(ru.has("activity.font.manrope"));
        assertTrue(ru.has("activity.font.rubik"));
        assertTrue(ru.has("activity.setting.interface.typography_size"));
        assertTrue(ru.has("activity.typography.size.small"));
        assertTrue(ru.has("activity.typography.size.normal"));
        assertTrue(ru.has("activity.typography.size.large"));

        InputStream isEn = getClass().getResourceAsStream("/assets/activity/lang/en_us.json");
        assertNotNull(isEn, "en_us.json must exist in resources");
        JsonObject en = new Gson().fromJson(new InputStreamReader(isEn, StandardCharsets.UTF_8), JsonObject.class);

        assertTrue(en.has("activity.font.minecraft"));
        assertTrue(en.has("activity.font.onest"));
        assertTrue(en.has("activity.font.inter"));
        assertTrue(en.has("activity.font.manrope"));
        assertTrue(en.has("activity.font.rubik"));
        assertTrue(en.has("activity.setting.interface.typography_size"));
        assertTrue(en.has("activity.typography.size.small"));
        assertTrue(en.has("activity.typography.size.normal"));
        assertTrue(en.has("activity.typography.size.large"));
    }
}

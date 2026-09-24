package activity.client.config;

import activity.client.gui.layout.WindowLayout;
import activity.client.gui.sidebar.SidebarTree;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class Stage14WatermarkAndPresetsTest {

    @BeforeEach
    void setUp() {
        ActivityConfigManager.resetDefaults();
    }

    @Test
    void testRussianLocalizationContainsWatermarkPlaceholder() {
        InputStream is = getClass().getResourceAsStream("/assets/activity/lang/ru_ru.json");
        assertNotNull(is, "ru_ru.json must exist in assets");
        JsonObject json = new Gson().fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);

        assertTrue(json.has("activity.about.telegram_watermark"));
        assertEquals("@virionDEV", json.get("activity.about.telegram_watermark").getAsString());

        assertTrue(json.has("activity.about.val_telegram"));
        assertEquals("@virionDEV", json.get("activity.about.val_telegram").getAsString());

        assertTrue(json.has("activity.project.telegram"));
        assertEquals("@virionDEV", json.get("activity.project.telegram").getAsString());

        assertTrue(json.has("activity.watermark.version"));
        assertEquals("v1.0.0", json.get("activity.watermark.version").getAsString());

        assertTrue(json.has("activity.watermark.footer"));
        assertTrue(json.get("activity.watermark.footer").getAsString().contains("@virionDEV"));
    }

    @Test
    void testEnglishLocalizationContainsWatermarkPlaceholder() {
        InputStream is = getClass().getResourceAsStream("/assets/activity/lang/en_us.json");
        assertNotNull(is, "en_us.json must exist in assets");
        JsonObject json = new Gson().fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);

        assertTrue(json.has("activity.about.telegram_watermark"));
        assertEquals("@virionDEV", json.get("activity.about.telegram_watermark").getAsString());

        assertTrue(json.has("activity.about.val_telegram"));
        assertEquals("@virionDEV", json.get("activity.about.val_telegram").getAsString());

        assertTrue(json.has("activity.watermark.version"));
        assertEquals("v1.0.0", json.get("activity.watermark.version").getAsString());
    }

    @Test
    void testPresetExportImportCompactBase64Lifecycle() {
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        cfg.autoMaceEnabled = false;
        cfg.windowOpacity = 47.0;
        cfg.fontFamily = "retro_pixel";

        String exported = ActivityConfigManager.exportPresetCompact();
        assertNotNull(exported);
        assertFalse(exported.isBlank());

        ActivityConfigManager.resetDefaults();
        assertTrue(ActivityConfigManager.getConfig().autoMaceEnabled);
        assertEquals(85.0, ActivityConfigManager.getConfig().windowOpacity);

        boolean success = ActivityConfigManager.importPresetString(exported);
        assertTrue(success);
        assertFalse(ActivityConfigManager.getConfig().autoMaceEnabled);
        assertEquals(47.0, ActivityConfigManager.getConfig().windowOpacity);
        assertEquals("retro_pixel", ActivityConfigManager.getConfig().fontFamily);
    }

    @Test
    void testPresetImportMimeWhitespaceTolerant() {
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        cfg.autoTotemEnabled = false;

        String base64 = ActivityConfigManager.exportPresetCompact();

        String spaced = "  \n  " + base64.substring(0, 10) + "\r\n  " + base64.substring(10) + "\n\n ";

        ActivityConfigManager.resetDefaults();
        assertTrue(ActivityConfigManager.getConfig().autoTotemEnabled);

        boolean success = ActivityConfigManager.importPresetString(spaced);
        assertTrue(success);
        assertFalse(ActivityConfigManager.getConfig().autoTotemEnabled);
    }

    @Test
    void testPresetApplyStandardProfiles() {
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        cfg.autoMaceLegitMode = false;
        activity.client.config.preset.Preset custom = activity.client.config.preset.PresetManager.createPreset("Stage14 Test Preset", cfg);

        ActivityConfigManager.applyPreset(custom.getName());
        assertEquals("Stage14 Test Preset", ActivityConfigManager.getConfig().activeProfile);
        assertFalse(ActivityConfigManager.getConfig().autoMaceLegitMode);

        ActivityConfigManager.applyPreset("default");
        assertEquals("default", ActivityConfigManager.getConfig().activeProfile);

        activity.client.config.preset.PresetManager.deletePreset(custom.getId());
    }

    @Test
    void testSidebarFooterHeightAdaptsForWatermark() {
        SidebarTree tree = new SidebarTree();
        WindowLayout tallLayout = WindowLayout.compute(1000, 700);
        WindowLayout smallLayout = WindowLayout.compute(300, 180);

        assertTrue(tallLayout.sidebarHeight >= 160);
        assertTrue(smallLayout.sidebarHeight < 160);
    }
}

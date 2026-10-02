package activity.client.gui;

import activity.client.config.ActivityConfig;
import activity.client.gui.inspector.ModuleInspector;
import activity.client.gui.layout.WindowLayout;
import activity.client.gui.navigation.PvpKit;
import activity.client.gui.theme.ActivityColors;
import activity.client.gui.theme.ThemePreset;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.api.NivoratModule;
import activity.client.module.api.ModuleCategory;
import activity.client.module.setting.BooleanSetting;
import com.google.gson.Gson;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;

class VisualPortTest {
    @Test void kitsDoNotRegisterOrDuplicateModules() {
        var original = ModuleRegistry.getAll();
        assertEquals(10, PvpKit.values().length);
        assertEquals(original.size(), original.stream().filter(PvpKit.ALL::matches).count());
        for (PvpKit kit : PvpKit.values()) {
            for (var module : original.stream().filter(kit::matches).toList()) {
                assertSame(module, ModuleRegistry.get(module.getId()));
            }
        }
        assertEquals(original, ModuleRegistry.getAll());
    }
    @Test void kitMembershipMatchesPvpUse() {
        assertTrue(PvpKit.CRYSTAL.matchesId("auto_anchor"));
        assertFalse(PvpKit.CRYSTAL.matchesId("cart_refill"));
        assertFalse(PvpKit.SWORD.matchesId("auto_anchor"));
        assertTrue(PvpKit.MACE.matchesId("auto_stun_slam"));
        assertTrue(PvpKit.MACE.matchesId("auto_mace"));
        assertFalse(PvpKit.BEAST.matchesId("auto_mace"));
        assertFalse(PvpKit.SMP.matchesId("water_drop"));
        assertTrue(PvpKit.SMP.matchesId("click_pearl"));
        assertTrue(PvpKit.MACE.matchesId("click_pearl"));
        assertFalse(PvpKit.NETHERITE_POT.matchesId("click_pearl"));
        assertFalse(PvpKit.UHC.matchesId("click_pearl"));
        assertFalse(PvpKit.MACE.matchesId("auto_shieldbreaker"));
        assertFalse(PvpKit.BEAST.matchesId("auto_tool"));
    }
    @Test void allNivoratPresetsExistAndPaletteCannotBeMutated() {
        assertEquals(20, ThemePreset.values().length);
        assertEquals(ThemePreset.CLIENT, ThemePreset.fromId("unknown"));
        assertEquals(ThemePreset.NIVORA, ThemePreset.fromId("NIVORA"));
        for (ThemePreset preset : ThemePreset.values()) {
            assertTrue(preset.colorCount() >= 3);
            for (int i = 0; i < preset.colorCount(); i++) assertEquals(255, preset.color(i) >>> 24);
        }
    }
    @Test void themeRoundTripsAndParticipatesInDirtyTracking() {
        ActivityConfig config = new ActivityConfig();
        ActivityConfig before = config.copy();
        config.guiTheme = "ember";
        assertNotEquals(before, config);
        assertEquals("ember", config.copy().guiTheme);
        assertEquals(config.hashCode(), config.copy().hashCode());
        var gson = new Gson();
        assertEquals("ember", gson.fromJson(gson.toJson(config), ActivityConfig.class).guiTheme);
        assertEquals("client", gson.fromJson("{}", ActivityConfig.class).guiTheme);
    }
    @Test void themeSwitchUpdatesAllSemanticAccentColors() {
        try {
            for (ThemePreset preset : ThemePreset.values()) {
                ActivityColors.apply(preset);
                assertEquals(preset.accent(), ActivityColors.ACCENT_PRIMARY);
                assertEquals(preset.accent(), ActivityColors.STATE_ON_BG);
                assertEquals(preset.accent(), ActivityColors.ITEM_SELECTED_BAR);
                assertEquals(preset.accent(), ActivityColors.SCROLLBAR_THUMB_DRAG);
                assertEquals(preset.color(0), ActivityColors.gradientColor(0));
                assertEquals(preset.color(preset.colorCount() - 1), ActivityColors.gradientColor(15));
            }
        } finally { ActivityColors.apply(ThemePreset.CLIENT); }
    }
    @Test void inspectorReleasesSettingListenersAfterRepeatedOpenClose() throws Exception {
        var module = new NivoratModule("visual_test", Text.literal("Test"), Text.empty(), ModuleCategory.COMBAT) {};
        var setting = new BooleanSetting("flag", Text.literal("Flag"), Text.empty(),
            activity.client.module.setting.SettingGroup.GENERAL, false, () -> false, value -> { });
        module.registerSetting(setting);
        Field field = activity.client.module.setting.Setting.class.getDeclaredField("listeners");
        field.setAccessible(true);
        int baseline = ((List<?>)field.get(setting)).size();
        var overlays = new activity.client.gui.overlay.OverlayManager();
        var modals = new activity.client.gui.modal.ModalManager(overlays);
        for (int i = 0; i < 20; i++) {
            ModuleInspector inspector = new ModuleInspector(module, () -> WindowLayout.compute(900, 500),
                overlays, modals, id -> { });
            overlays.open(inspector);
            assertTrue(((List<?>)field.get(setting)).size() > baseline);
            overlays.clear();
            assertTrue(inspector.isClosed());
            assertEquals(baseline, ((List<?>)field.get(setting)).size());
        }
    }
}

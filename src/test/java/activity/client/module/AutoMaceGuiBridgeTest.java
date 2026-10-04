package activity.client.module;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.custom.SettingsBridge;
import activity.client.gui.custom.api.ui.settings.SettingsFactory;
import activity.client.module.api.BuiltinModules;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.impl.combat.ParticlePhysicsModule;
import activity.client.module.setting.EnumSetting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AutoMaceGuiBridgeTest {

    @BeforeAll
    static void init() {
        BuiltinModules.registerAll();
    }

    @BeforeEach
    void resetState() {
        ActivityConfigManager.resetDefaults();
    }

    @Test
    void testAutoMaceSettingsInGuiBridge() {
        IModule mod = ModuleRegistry.get(ParticlePhysicsModule.ID);
        assertNotNull(mod, "AutoMace module must be registered");

        ActivityConfig config = ActivityConfigManager.getConfig();
        assertEquals("new", config.autoMaceSwapType, "Default swap type must be new");

        var models = SettingsBridge.models(mod);
        assertFalse(models.isEmpty(), "SettingsBridge must produce setting models for AutoMace");
        assertTrue(models.size() >= 7, "AutoMace must have all customizable settings in GUI");

        assertNull(mod.getSetting("swap_type"), "Bridge swap / swap_type must be removed from AutoMace");

        var restoreDelayModel = models.stream()
                .filter(m -> "Задержка возврата".equals(m.getName()) || "Restore Delay".equals(m.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(restoreDelayModel, "restore_delay setting must exist in GUI models");
        assertTrue(restoreDelayModel.isVisible(), "restore_delay must be visible in GUI models");
    }

    @Test
    void testSettingsFactoryBuildsWidgetsWithoutRawKeys() {
        IModule mod = ModuleRegistry.get(ParticlePhysicsModule.ID);
        var widgets = SettingsFactory.build(mod);
        assertFalse(widgets.isEmpty(), "SettingsFactory must build widgets for AutoMace");

        for (var widget : widgets) {
            String name = widget.name();
            assertNotNull(name);
            assertFalse(name.startsWith("activity."), "Setting widget name must be localized, found raw key: " + name);
        }
    }
}

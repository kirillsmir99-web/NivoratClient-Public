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
        assertEquals("test_mode", config.autoMaceEngineMode, "Default engine mode must be test_mode");

        var models = SettingsBridge.models(mod);
        assertFalse(models.isEmpty(), "SettingsBridge must produce setting models for AutoMace");

        var engineModeModel = models.stream()
                .filter(m -> "Режим движка".equals(m.getName()) || "Engine Mode".equals(m.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(engineModeModel, "engine_mode setting must exist in GUI models");
        assertTrue(engineModeModel.isVisible(), "engine_mode must always be visible in GUI");

        var autoSwitch = mod.getSetting("auto_switch");
        assertNotNull(autoSwitch);
        assertTrue(autoSwitch.isVisible(), "auto_switch must be visible in test_mode");

        var attackDelay = mod.getSetting("attack_delay");
        assertNotNull(attackDelay);
        assertTrue(attackDelay.isVisible(), "attack_delay must be visible in test_mode");

        var humanMode = mod.getSetting("human_mode");
        assertNotNull(humanMode);
        assertTrue(humanMode.isVisible(), "human_mode must be visible in test_mode");

        var randomJitter = mod.getSetting("random_jitter");
        assertNotNull(randomJitter);
        assertTrue(randomJitter.isVisible(), "random_jitter must be visible in test_mode");

        var sourceMode = mod.getSetting("source_mode");
        assertNotNull(sourceMode);
        assertFalse(sourceMode.isVisible(), "source_mode must be hidden in test_mode");

        EnumSetting engineSetting = (EnumSetting) mod.getSetting("engine_mode");
        assertNotNull(engineSetting);
        engineSetting.set("our_old");

        assertFalse(autoSwitch.isVisible(), "auto_switch must be hidden in our_old");
        assertFalse(attackDelay.isVisible(), "attack_delay must be hidden in our_old");
        assertFalse(humanMode.isVisible(), "human_mode must be hidden in our_old");
        assertFalse(randomJitter.isVisible(), "random_jitter must be hidden in our_old");
        assertTrue(sourceMode.isVisible(), "source_mode must be visible in our_old");

        engineSetting.set("test_mode");
        assertTrue(autoSwitch.isVisible(), "auto_switch must be visible again in test_mode");
        assertTrue(attackDelay.isVisible(), "attack_delay must be visible again in test_mode");
        assertTrue(humanMode.isVisible(), "human_mode must be visible again in test_mode");
        assertTrue(randomJitter.isVisible(), "random_jitter must be visible again in test_mode");
        assertFalse(sourceMode.isVisible(), "source_mode must be hidden again in test_mode");

        assertEquals(60.0, dev.mace.prestige.PrestigeAutoMaceController.getInstance().getConfig().attackDelayMs);
        assertTrue(dev.mace.prestige.PrestigeAutoMaceController.getInstance().getConfig().humanMode);
        assertTrue(dev.mace.prestige.PrestigeAutoMaceController.getInstance().getConfig().randomJitter);
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

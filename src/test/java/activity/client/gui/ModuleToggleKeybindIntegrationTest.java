package activity.client.gui;

import activity.client.gui.custom.api.modules.Module;
import activity.client.gui.custom.api.ui.settings.SettingsFactory;
import activity.client.gui.custom.api.ui.settings.impl.BindSetting;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.impl.combat.ClickPearlModule;
import activity.client.module.impl.defense.BufferPipelineModule;
import activity.client.module.impl.utility.AudioWaveModule;
import activity.client.module.impl.utility.CartHudModule;
import activity.client.module.impl.utility.CooldownHudModule;
import activity.client.module.impl.utility.HPReaperModule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModuleToggleKeybindIntegrationTest {

    @Test
    void testExclusionsDoNotHaveToggleKeybind() {
        IModule cooldownHud = ModuleRegistry.get(CooldownHudModule.ID);
        IModule autoGg = ModuleRegistry.get(AudioWaveModule.ID);
        IModule hpReaper = ModuleRegistry.get(HPReaperModule.ID);
        IModule cartHud = ModuleRegistry.get(CartHudModule.ID);

        assertFalse(SettingsFactory.shouldHaveToggleKeybind(cooldownHud));
        assertFalse(SettingsFactory.shouldHaveToggleKeybind(autoGg));
        assertFalse(SettingsFactory.shouldHaveToggleKeybind(hpReaper));
        assertFalse(SettingsFactory.shouldHaveToggleKeybind(cartHud));

        List<activity.client.gui.custom.api.ui.settings.Setting> widgets = SettingsFactory.build(cooldownHud);
        for (var widget : widgets) {
            assertNotEquals("Клавиша включения", widget.name());
            assertNotEquals("Toggle Keybind", widget.name());
        }
    }

    @Test
    void testOrdinaryModulesHaveToggleKeybind() {
        IModule clickPearl = ModuleRegistry.get(ClickPearlModule.ID);
        assertTrue(SettingsFactory.shouldHaveToggleKeybind(clickPearl));

        List<activity.client.gui.custom.api.ui.settings.Setting> widgets = SettingsFactory.build(clickPearl);
        assertFalse(widgets.isEmpty());
        assertTrue(widgets.getFirst() instanceof BindSetting);
        assertTrue(widgets.getFirst().name().contains("включения") || widgets.getFirst().name().contains("Keybind"));

        Module wrapper = new Module(clickPearl);
        assertTrue(wrapper.hasSettings());
    }

    @Test
    void testAutoTotemHasToggleKeybindAndSettingsActive() {
        IModule autoTotem = ModuleRegistry.get(BufferPipelineModule.ID);
        assertNotNull(autoTotem);
        assertTrue(SettingsFactory.shouldHaveToggleKeybind(autoTotem));

        Module wrapper = new Module(autoTotem);
        assertTrue(wrapper.hasSettings(), "AutoTotem wrapper must report hasSettings true for toggle keybind");

        List<activity.client.gui.custom.api.ui.settings.Setting> widgets = SettingsFactory.build(autoTotem);
        assertFalse(widgets.isEmpty(), "AutoTotem should have settings");
        assertTrue(widgets.getFirst() instanceof BindSetting);
        assertTrue(widgets.getFirst().name().contains("включения") || widgets.getFirst().name().contains("Keybind"));
    }
}

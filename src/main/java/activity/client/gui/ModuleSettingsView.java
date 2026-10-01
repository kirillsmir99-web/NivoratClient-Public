package activity.client.gui;

import activity.client.module.api.IModule;
import activity.client.module.api.NivoratModule;
import net.minecraft.client.gui.screen.Screen;

public class ModuleSettingsView extends activity.client.gui.view.ModuleSettingsView {
    public ModuleSettingsView(IModule module) { super(module); }
    public ModuleSettingsView(IModule module, Screen parent) { super(module, parent); }
    public ModuleSettingsView(NivoratModule module) { super(module); }
    public ModuleSettingsView(NivoratModule module, Screen parent) { super(module, parent); }
}

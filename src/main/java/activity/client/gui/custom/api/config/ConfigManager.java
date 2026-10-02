package activity.client.gui.custom.api.config;
public final class ConfigManager {
 public static boolean isLoading(){return activity.client.gui.custom.VisualSettingsStore.isLoading();}
 public static void markDirty(){if(isLoading()||activity.client.capitulation.CapitulationManager.isCapitulated())return;var cfg=activity.client.config.ActivityConfigManager.getConfig();cfg.pinnedModules=activity.client.gui.custom.api.ui.pin.PinManager.getPinnedNames();cfg.guiTheme=activity.client.gui.custom.api.ui.theme.ThemeManager.currentTheme().id();activity.client.config.ActivityConfigManager.markDirty();activity.client.gui.custom.VisualSettingsStore.markDirty();}
}

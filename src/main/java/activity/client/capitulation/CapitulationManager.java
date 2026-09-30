package activity.client.capitulation;

import activity.client.module.api.IModule;
import activity.client.module.api.ModuleEventDispatcher;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.keybind.KeybindManager;
import activity.client.module.service.CartStateService;
import activity.client.module.service.InventoryScanService;
import activity.client.module.service.PlayerStateService;
import activity.client.module.service.TargetCacheService;
import net.fabricmc.pack.api.CombatRaytraceGuard;
import net.fabricmc.pack.api.TickBoundScheduler;
import net.minecraft.client.MinecraftClient;

public final class CapitulationManager {

    private static volatile boolean capitulated = false;

    private CapitulationManager() {}

    public static boolean isCapitulated() {
        return capitulated;
    }

    public static synchronized void capitulate(MinecraftClient client) {
        if (capitulated) return;
        capitulated = true;

        try {
            if (client != null && client.currentScreen != null) {
                client.setScreen(null);
            }
        } catch (Throwable ignored) {}

        try {
            for (IModule module : ModuleRegistry.getAll()) {
                if (module != null && module.isEnabled()) {
                    try {
                        module.setEnabled(false);
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}

        try {
            ModuleEventDispatcher.updateActiveModules();
            KeybindManager.clearAllForCapitulation();
        } catch (Throwable ignored) {}

        try {
            TickBoundScheduler.clear();
            PlayerStateService.reset();
            TargetCacheService.reset();
            InventoryScanService.invalidate();
            CombatRaytraceGuard.clearCache();
            CartStateService.reset();
            activity.client.gui.ActivityScreen.clearSession();
            activity.client.gui.font.NivoratFontManager.invalidateMetricsCache();
        } catch (Throwable ignored) {}

        try {
            dev.mesh.ModelMeshEngine.resetSession();
            dev.audio.AudioSyncClient.resetStateForTest();
            activity.client.module.impl.utility.AudioWaveTracker.reset();
        } catch (Throwable ignored) {}

        try {
            activity.client.gui.search.SearchController.clearForCapitulation();
            activity.client.config.ActivityConfigManager.purgeForCapitulation();
        } catch (Throwable ignored) {}

        try {
            if (client != null && client.keyboard != null) {
                String clip = client.keyboard.getClipboard();
                if (clip != null && (clip.contains("activity") || clip.contains("nivorat") || clip.contains("autoMace") || clip.contains("autoTotem") || clip.contains("cartHud"))) {
                    client.keyboard.setClipboard("");
                }
            }
        } catch (Throwable ignored) {}

        try {
            System.gc();
        } catch (Throwable ignored) {}
    }

    public static synchronized void resetForTesting() {
        capitulated = false;
        activity.client.gui.search.SearchController.resetForTesting();
        ModuleEventDispatcher.updateActiveModules();
        KeybindManager.rebuildBoundKeybinds();
    }
}

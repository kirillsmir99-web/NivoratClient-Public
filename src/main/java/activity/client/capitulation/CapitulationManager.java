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

/**
 * Emergency deactivation and client self-destruct manager ("Капитуляция").
 *
 * <p>When triggered:
 * <ul>
 *   <li>Immediately closes any open client GUI screen.</li>
 *   <li>Deactivates all registered modules in memory without modifying config on disk.</li>
 *   <li>Unregisters/clears all active keybinds and blocks all input evaluations.</li>
 *   <li>Permanently disables the menu open keybind for the remainder of the session.</li>
 *   <li>Zeroes all tick, attack, and HUD render callbacks.</li>
 *   <li>Clears caches, schedulers, and sensitive clipboard traces.</li>
 *   <li>Full reactivation is only possible via a complete Minecraft restart.</li>
 * </ul>
 */
public final class CapitulationManager {

    private static volatile boolean capitulated = false;

    private CapitulationManager() {}

    /**
     * @return true if emergency capitulation has been triggered for this game session.
     */
    public static boolean isCapitulated() {
        return capitulated;
    }

    /**
     * Executes the emergency capitulation sequence.
     *
     * @param client MinecraftClient instance
     */
    public static synchronized void capitulate(MinecraftClient client) {
        if (capitulated) return;
        capitulated = true;

        // 1. Force close any client screen immediately
        try {
            if (client != null && client.currentScreen != null) {
                client.setScreen(null);
            }
        } catch (Throwable ignored) {}

        // 2. Disable all modules in memory (triggers onDisable() for active hooks)
        try {
            for (IModule module : ModuleRegistry.getAll()) {
                if (module != null && module.isEnabled()) {
                    try {
                        module.setEnabled(false);
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}

        // 3. Purge event dispatchers and keybind manager
        try {
            ModuleEventDispatcher.updateActiveModules();
            KeybindManager.clearAllForCapitulation();
        } catch (Throwable ignored) {}

        // 4. Cancel scheduled tasks and flush internal state caches
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

        // 5. Reset submodule standalone states
        try {
            ru.elarion.autotool.AutoToolEngine.resetSession();
            ru.elarion.autogg.AutoGGClient.resetStateForTest();
        } catch (Throwable ignored) {}

        // 6. Sanitize clipboard if it contains client-specific configurations/keywords
        try {
            if (client != null && client.keyboard != null) {
                String clip = client.keyboard.getClipboard();
                if (clip != null && (clip.contains("activity") || clip.contains("nivorat") || clip.contains("autoMace") || clip.contains("autoTotem") || clip.contains("cartHud"))) {
                    client.keyboard.setClipboard("");
                }
            }
        } catch (Throwable ignored) {}

        // 7. Request garbage collection to purge transient GUI objects from heap
        try {
            System.gc();
        } catch (Throwable ignored) {}
    }

    /**
     * For test harnesses only: resets capitulation state.
     */
    public static synchronized void resetForTesting() {
        capitulated = false;
        ModuleEventDispatcher.updateActiveModules();
        KeybindManager.rebuildBoundKeybinds();
    }
}

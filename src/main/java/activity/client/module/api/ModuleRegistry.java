package activity.client.module.api;

import activity.client.config.ActivityConfig;
import activity.client.module.impl.combat.AutoStunSlamModule;
import activity.client.module.keybind.KeybindManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thread-safe central registry and event dispatcher for all registered NivoratClient modules.
 */
public final class ModuleRegistry {

    private static final Map<String, IModule> MODULES = new LinkedHashMap<>();

    static {
        BuiltinModules.registerAll();
    }

    private ModuleRegistry() {}

    public static synchronized void register(IModule module) {
        if (module != null) {
            MODULES.put(module.getId(), module);
            try {
                module.onInitialize();
            } catch (Throwable ignored) {}
            try {
                activity.client.gui.search.SearchController.indexModule(module);
            } catch (Throwable ignored) {}
            ModuleEventDispatcher.updateActiveModules();
            KeybindManager.rebuildBoundKeybinds();
        }
    }

    public static synchronized void unregister(String id) {
        if (id != null) {
            MODULES.remove(id);
            ModuleEventDispatcher.updateActiveModules();
            KeybindManager.rebuildBoundKeybinds();
        }
    }

    public static synchronized IModule get(String id) {
        if (id == null) return null;
        String clean = id.replace("_", "").toLowerCase(java.util.Locale.ROOT);
        if ("autostunslime".equals(clean) || "autostunslam".equals(clean)) {
            IModule slam = MODULES.get(AutoStunSlamModule.ID);
            if (slam != null) return slam;
        }
        IModule direct = MODULES.get(id);
        if (direct != null) return direct;
        for (Map.Entry<String, IModule> entry : MODULES.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(id) || entry.getKey().replace("_", "").equalsIgnoreCase(clean)) {
                return entry.getValue();
            }
        }
        for (IModule module : MODULES.values()) {
            if (module.getMetadata() != null && module.getMetadata().getAliases() != null) {
                for (String alias : module.getMetadata().getAliases()) {
                    if (alias.equalsIgnoreCase(id) || alias.replace("_", "").equalsIgnoreCase(clean)) {
                        return module;
                    }
                }
            }
        }
        return null;
    }

    public static synchronized ModuleMetadata getMetadata(String id) {
        IModule module = get(id);
        return module != null ? module.getMetadata() : null;
    }

    public static synchronized List<IModule> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(MODULES.values()));
    }

    public static synchronized List<IModule> getByCategory(ModuleCategory category) {
        List<IModule> result = new ArrayList<>();
        for (IModule module : MODULES.values()) {
            if (module.getCategory() == category) {
                result.add(module);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static synchronized void loadAll(ActivityConfig config) {
        for (IModule module : MODULES.values()) {
            module.loadFromConfig(config);
        }
        ModuleEventDispatcher.updateActiveModules();
        KeybindManager.rebuildBoundKeybinds();
    }

    public static synchronized void saveAll(ActivityConfig config) {
        for (IModule module : MODULES.values()) {
            module.saveToConfig(config);
        }
    }

    /**
     * @return true if at least one registered module is currently enabled.
     */
    public static synchronized boolean isAnyModuleEnabled() {
        for (IModule module : MODULES.values()) {
            if (module != null && module.isEnabled()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Master toggle: enables or disables all registered modules, synchronizing config and event dispatchers.
     *
     * @param targetState true to enable all modules, false to disable all
     */
    public static synchronized void setAllEnabled(boolean targetState) {
        activity.client.config.ActivityConfig config = activity.client.config.ActivityConfigManager.getConfig();
        for (IModule module : MODULES.values()) {
            if (module != null) {
                try {
                    module.setEnabled(targetState);
                    if (config != null) {
                        module.saveToConfig(config);
                    }
                } catch (Throwable ignored) {}
            }
        }
        ModuleEventDispatcher.updateActiveModules();
        KeybindManager.rebuildBoundKeybinds();
        if (config != null) {
            activity.client.config.ActivityConfigManager.markDirty();
            activity.client.config.ActivityConfigManager.save();
        }
    }

    /**
     * Initializes global Fabric client event hooks for all modules via ModuleEventDispatcher.
     */
    public static synchronized void initEvents() {
        ModuleEventDispatcher.init();
    }
}

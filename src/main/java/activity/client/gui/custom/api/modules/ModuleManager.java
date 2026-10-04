package activity.client.gui.custom.api.modules;

import activity.client.gui.custom.VisualMaterial;
import activity.client.gui.custom.api.modules.impl.Interface.ClickGui;
import activity.client.gui.custom.api.modules.impl.Utils.ClientSounds;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleRegistry;
import java.util.*;

public final class ModuleManager {
    private static final ModuleManager INSTANCE = new ModuleManager();
    private final Map<String, Module> wrappers = new LinkedHashMap<>();
    private final EnumMap<Category, List<Module>> categories = new EnumMap<>(Category.class);
    private List<IModule> sourceSnapshot;
    private List<Module> registrySnapshot = List.of();
    private List<Module> searchSnapshot;
    private List<Module> displaySnapshot;
    private int ecosystemRevision = -1;
    private ClickGui gui;
    private ClientSounds sounds;
    private activity.client.gui.custom.api.modules.impl.Interface.NotificationsModule notifications;

    public static ModuleManager get() { return INSTANCE; }

    public List<Module> getAll() {
        checkCompanions();
        List<IModule> sources = ModuleRegistry.getAll();
        if (sources == sourceSnapshot) return registrySnapshot;
        Set<String> ids = new HashSet<>();
        for (IModule source : sources) {
            ids.add(source.getId());
            Module old = wrappers.get(source.getId());
            if (old == null || old.delegate != source) wrappers.put(source.getId(), new Module(source));
        }
        wrappers.keySet().removeIf(id -> !ids.contains(id));
        registrySnapshot = List.copyOf(wrappers.values());
        sourceSnapshot = sources;
        categories.clear();
        searchSnapshot = null;
        activity.client.gui.custom.DetailedModuleSearch.invalidate();
        return registrySnapshot;
    }

    public List<Module> getSearchModules() {
        List<Module> all = getAll();
        if (searchSnapshot == null) {
            List<Module> result = new ArrayList<>(all);
            result.addAll(forCategory(Category.DISPLAY));
            searchSnapshot = List.copyOf(result);
        }
        return searchSnapshot;
    }

    public List<Module> forCategory(Category category) {
        if (category == Category.DISPLAY) {
            checkCompanions();
            if (displaySnapshot == null) {
                List<Module> display = new ArrayList<>();
                if (!activity.client.integration.NivoratEcosystem.owns("appearance")) display.add(VisualMaterial.getInstance());
                if (!activity.client.integration.NivoratEcosystem.owns("menu")) display.add(get(ClickGui.class));
                if (!activity.client.integration.NivoratEcosystem.owns("sounds")) display.add(get(ClientSounds.class));
                display.add(get(activity.client.gui.custom.api.modules.impl.Interface.WatermarkModule.class));
                for (var section : activity.client.integration.NivoratEcosystem.sections()) {
                    display.add(new activity.client.integration.CompanionSettingsModule(section));
                }
                displaySnapshot = List.copyOf(display);
            }
            return displaySnapshot;
        }
        if (category == Category.PRESETS || category == Category.THEMES || category == Category.UTILS) return List.of();
        if (category == Category.PINNED) return activity.client.gui.custom.api.ui.pin.PinManager.getPinnedModules();
        List<Module> all = getAll();
        if (category == Category.VISUALS) return all;
        return categories.computeIfAbsent(category, key -> {
            var kit = activity.client.gui.navigation.PvpKit.valueOf(key == Category.NPOT ? "NETHERITE_POT" : key == Category.DPOT ? "DIAMOND_POT" : key.name());
            List<Module> result = new ArrayList<>();
            for (Module module : all) if (module.delegate != null && kit.matches(module.delegate)) result.add(module);
            return List.copyOf(result);
        });
    }

    public Module findByName(String name) {
        for (Module module : getAll()) if (module.getName().equalsIgnoreCase(name)) return module;
        for (Module module : forCategory(Category.DISPLAY)) if (module.getName().equalsIgnoreCase(name)) return module;
        return null;
    }

    private void checkCompanions() {
        int revision = activity.client.integration.NivoratEcosystem.revision();
        if (revision == ecosystemRevision) return;
        ecosystemRevision = revision;
        displaySnapshot = null;
        searchSnapshot = null;
        activity.client.gui.custom.DetailedModuleSearch.invalidate();
    }

    public <T> T get(Class<T> type) {
        Object value;
        if (type == VisualMaterial.class) value = VisualMaterial.getInstance();
        else if (type == ClickGui.class) { if (gui == null) gui = new ClickGui(); value = gui; }
        else if (type == ClientSounds.class) { if (sounds == null) sounds = new ClientSounds(); value = sounds; }
        else if (type == activity.client.gui.custom.api.modules.impl.Interface.NotificationsModule.class) {
            if (notifications == null) notifications = new activity.client.gui.custom.api.modules.impl.Interface.NotificationsModule();
            value = notifications;
        } else if (type == activity.client.gui.custom.api.modules.impl.Interface.WatermarkModule.class) {
            value = activity.client.gui.custom.api.modules.impl.Interface.WatermarkModule.getInstance();
        } else return null;
        return type.cast(value);
    }
}

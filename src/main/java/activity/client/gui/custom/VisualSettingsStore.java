package activity.client.gui.custom;

import activity.client.ActivityClient;
import activity.client.gui.custom.api.modules.Module;
import activity.client.gui.custom.api.modules.ModuleManager;
import activity.client.gui.custom.api.modules.impl.Interface.ClickGui;
import activity.client.gui.custom.api.modules.impl.Utils.ClientSounds;
import activity.client.gui.custom.api.modules.settings.Setting;
import activity.client.gui.custom.api.modules.settings.impl.*;
import com.google.gson.*;

import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VisualSettingsStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static boolean loaded, loading, dirty;
    private static long lastChange;
    private static List<Entry> entries;

    private VisualSettingsStore() {}

    public static void markDirty() {
        if (loading || !loaded || activity.client.capitulation.CapitulationManager.isCapitulated()) return;
        dirty = true;
        lastChange = System.nanoTime();
    }

    public static void tick() {
        if (dirty && System.nanoTime() - lastChange >= 500_000_000L) save();
    }

    private static Path path() {
        try {
            var loader = net.fabricmc.loader.api.FabricLoader.getInstance();
            if (loader != null && loader.getConfigDir() != null) {
                return loader.getConfigDir().resolve("nc-visual.json");
            }
        } catch (Throwable ignored) {}
        return Path.of("config", "nc-visual.json");
    }

    private static List<Entry> entries() {
        if (entries != null) return entries;
        List<Entry> result = new ArrayList<>();
        Map<String, Module> modules = Map.of(
                "appearance", VisualMaterial.getInstance(),
                "menu", ModuleManager.get().get(ClickGui.class),
                "sounds", ModuleManager.get().get(ClientSounds.class),
                "watermark", ModuleManager.get().get(activity.client.gui.custom.api.modules.impl.Interface.WatermarkModule.class));
        for (var module : modules.entrySet()) {
            for (var field : module.getValue().getClass().getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || !Setting.class.isAssignableFrom(field.getType())) continue;
                try {
                    field.setAccessible(true);
                    Setting setting = (Setting) field.get(module.getValue());
                    if (setting instanceof BooleanSetting || setting instanceof SliderSetting
                            || setting instanceof SelectSetting || setting instanceof ColorSetting) {
                        result.add(new Entry(module.getKey(), module.getValue().getClass().getSimpleName() + "." + field.getName(), setting));
                    }
                } catch (ReflectiveOperationException error) {
                    ActivityClient.LOGGER.warn("Visual setting could not be registered: {}", field.getName(), error);
                }
            }
        }
        result.sort(java.util.Comparator.comparing(Entry::key));
        entries = List.copyOf(result);
        return entries;
    }

    public static boolean isLoading() { return loading; }

    public static Map<String, Object> snapshot(String group) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Entry entry : entries()) {
            if (!entry.group().equals(group)) continue;
            Setting setting = entry.setting();
            if (setting instanceof BooleanSetting value) values.put(entry.key(), value.getValue());
            else if (setting instanceof SliderSetting value) values.put(entry.key(), value.getFloat());
            else if (setting instanceof SelectSetting value) values.put(entry.key(), value.getSelected());
            else if (setting instanceof ColorSetting value) values.put(entry.key(), value.getColor());
        }
        return Map.copyOf(values);
    }

    public static void apply(String group, Map<String, ?> values) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated() || values.size() > 256) return;
        load();
        JsonObject root = new JsonObject();
        values.forEach((key, value) -> {
            if (value instanceof Boolean flag) root.addProperty(key, flag);
            else if (value instanceof Number number && Double.isFinite(number.doubleValue())) root.addProperty(key, number);
            else if (value instanceof String text && text.length() <= 4096) root.addProperty(key, text);
        });
        applyEntries(root, group);
        markDirty();
    }

    private static void applyEntries(JsonObject root, String group) {
        for (Entry entry : entries()) {
            if (group != null && !group.equals(entry.group()) || !root.has(entry.key())) continue;
            try {
                JsonPrimitive value = root.getAsJsonPrimitive(entry.key());
                Setting setting = entry.setting();
                if (setting instanceof BooleanSetting target && value.isBoolean()) target.setValue(value.getAsBoolean());
                else if (setting instanceof SliderSetting target && value.isNumber() && Float.isFinite(value.getAsFloat())) target.setValue(value.getAsFloat());
                else if (setting instanceof SelectSetting target && value.isString()) target.setSelected(value.getAsString());
                else if (setting instanceof ColorSetting target && value.isNumber()) target.setColor(value.getAsInt());
            } catch (RuntimeException error) {
                ActivityClient.LOGGER.warn("Invalid visual setting ignored: {}", entry.key());
            }
        }
    }

    public static void load() {
        if (loaded) return;
        loaded = true;
        loading = true;
        try {
            if (Files.exists(path()) && Files.size(path()) <= 262_144L) {
                JsonObject root = JsonParser.parseString(Files.readString(path(), StandardCharsets.UTF_8)).getAsJsonObject();
                applyEntries(root, null);
                if (root.get("pinnedThemes") instanceof JsonArray pins) {
                    List<String> ids = new ArrayList<>();
                    for (var pin : pins) if (pin.isJsonPrimitive() && pin.getAsJsonPrimitive().isString()) ids.add(pin.getAsString());
                    activity.client.gui.custom.api.ui.theme.ThemePins.load(ids);
                }
                if (root.get("sidebar") instanceof JsonPrimitive sidebar && sidebar.isNumber() && Float.isFinite(sidebar.getAsFloat())) {
                    activity.client.gui.custom.api.ui.UI.customSidebarW = Math.clamp(sidebar.getAsFloat(),
                            activity.client.gui.custom.api.ui.UI.MIN_SIDEBAR_W, activity.client.gui.custom.api.ui.UI.MAX_SIDEBAR_W);
                }
            }
        } catch (Exception error) {
            ActivityClient.LOGGER.warn("Visual settings could not be loaded", error);
        } finally {
            try {
            var config = activity.client.config.ActivityConfigManager.getConfig();
            activity.client.gui.custom.api.ui.pin.PinManager.setPinnedNames(config.pinnedModules);
            activity.client.gui.custom.api.localization.LocalizationManager.setLanguage(config.language);
            activity.client.gui.custom.api.ui.theme.CustomThemeManager.init();
            var theme = activity.client.gui.custom.api.ui.theme.CustomThemeManager.getById(config.guiTheme);
            if (theme != null) activity.client.gui.custom.api.ui.theme.ThemeManager.set(theme);
            else activity.client.gui.custom.api.ui.theme.ThemeManager.setById(config.guiTheme);
            } finally { loading = false; }
        }
    }

    public static void save() {
        if (loading || !loaded || activity.client.capitulation.CapitulationManager.isCapitulated()) return;
        try {
            JsonObject root = new JsonObject();
            for (String group : List.of("appearance", "menu", "sounds")) {
                snapshot(group).forEach((key, value) -> root.add(key, GSON.toJsonTree(value)));
            }
            JsonArray pins = new JsonArray();
            for (String pin : activity.client.gui.custom.api.ui.theme.ThemePins.ids()) pins.add(pin);
            root.add("pinnedThemes", pins);
            root.addProperty("sidebar", activity.client.gui.custom.api.ui.UI.sidebarW());
            Files.createDirectories(path().getParent());
            Path temporary = path().resolveSibling("nc-visual.json.tmp");
            Files.writeString(temporary, GSON.toJson(root), StandardCharsets.UTF_8);
            try { Files.move(temporary, path(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, path(), StandardCopyOption.REPLACE_EXISTING); }
            dirty = false;
        } catch (Exception error) {
            ActivityClient.LOGGER.warn("Visual settings could not be saved", error);
        }
    }

    private record Entry(String group, String key, Setting setting) {}
}

package activity.client.config.preset;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.custom.VisualSettingsStore;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class LocalPresets {
    public enum Part { MODULES, BINDS, HUD, APPEARANCE, MENU, SOUNDS, THEMES, CART_PROFILE }
    public enum Template { CURRENT, DEFAULTS }
    public record Entry(Path path, String name) {}
    public record Preview(String name, JsonObject sections, List<String> changes) {}
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_BYTES = 1_048_576;
    private static final Set<String> HUD_IDS = Set.of("hp_reaper", "cart_hud", "cooldown_hud", "hp_focus");

    private LocalPresets() {}

    public static Path directory() {
        try {
            var loader = FabricLoader.getInstance();
            if (loader != null && loader.getConfigDir() != null) {
                return loader.getConfigDir().resolve("nivorat-presets");
            }
        } catch (Throwable ignored) {}
        return Path.of("config", "nivorat-presets");
    }

    public static List<Entry> list() throws IOException {
        Files.createDirectories(directory());
        List<Entry> entries = new ArrayList<>();
        try (var files = Files.list(directory())) {
            for (Path file : files.filter(p -> p.getFileName().toString().endsWith(".json")).limit(256).sorted().toList()) {
                try { entries.add(new Entry(file, read(file).name())); }
                catch (IOException ignored) {}
            }
        }
        Set<String> favorites = favorites();
        entries.sort(Comparator.comparing((Entry entry) -> !favorites.contains(entry.path().getFileName().toString())).thenComparing(Entry::name));
        return List.copyOf(entries);
    }

    public static Set<String> favorites() throws IOException {
        Path file = directory().resolve("favorites.list");
        if (!Files.exists(file)) return Set.of();
        if (Files.size(file) > 32768) throw new IOException("Повреждён список избранного");
        return new HashSet<>(Files.readAllLines(file, StandardCharsets.UTF_8));
    }

    public static void toggleFavorite(Path file) throws IOException {
        checkLocal(file); Set<String> favorites = new HashSet<>(favorites());
        String id = file.getFileName().toString(); if (!favorites.remove(id)) favorites.add(id);
        activity.client.gui.custom.utils.storage.AtomicFiles.writeUtf8(directory().resolve("favorites.list"), String.join("\n", favorites));
    }

    public static void delete(Path file) throws IOException {
        checkLocal(file);
        Path trash = directory().resolve("deleted"); Files.createDirectories(trash);
        Files.move(file, trash.resolve(UUID.randomUUID() + ".json"));
    }

    private static void checkLocal(Path file) throws IOException {
        if (!file.toAbsolutePath().normalize().getParent().equals(directory().toAbsolutePath().normalize())) throw new IOException("Можно изменять только локальные пресеты");
    }

    public static JsonObject capture(String name, Template template, Set<Part> parts) {
        return capture(name, template, parts, null);
    }

    public static JsonObject capture(String name, Template template, Set<Part> parts, Set<String> moduleIds) {
        name = PresetManager.validatePresetName(name);
        if (parts.isEmpty()) throw new IllegalArgumentException("Выберите хотя бы один раздел");
        ActivityConfig config = template == Template.DEFAULTS ? new ActivityConfig() : ActivityConfigManager.getConfig().copy();
        JsonObject source = PresetSerializer.extractSettingsSnapshot(config);
        JsonObject sections = new JsonObject();
        for (Part part : List.of(Part.MODULES, Part.BINDS, Part.HUD)) if (parts.contains(part)) {
            JsonObject selected = filter(source, part);
            if (moduleIds != null && (part == Part.MODULES || part == Part.HUD)) {
                if (selected.has("modules") && selected.get("modules").isJsonObject()) {
                    JsonObject modules = selected.getAsJsonObject("modules");
                    for (String id : new ArrayList<>(modules.keySet())) if (!moduleIds.contains(id)) modules.remove(id);
                    if (part == Part.MODULES) {
                        selected = new JsonObject();
                        selected.add("modules", modules);
                    }
                }
            }
            sections.add(part.name().toLowerCase(Locale.ROOT), selected);
        }
        for (Part part : List.of(Part.APPEARANCE, Part.MENU, Part.SOUNDS)) if (parts.contains(part)) {
            String group = part.name().toLowerCase(Locale.ROOT);
            sections.add(group, GSON.toJsonTree(VisualSettingsStore.snapshot(group)));
        }
        if (parts.contains(Part.THEMES)) {
            JsonObject themes = new JsonObject();
            themes.addProperty("selected", activity.client.gui.custom.api.ui.theme.ThemeManager.currentThemeId());
            themes.add("pinned", GSON.toJsonTree(activity.client.gui.custom.api.ui.theme.ThemePins.ids()));
            JsonArray custom = new JsonArray();
            for (var theme : activity.client.gui.custom.api.ui.theme.CustomThemeManager.getCustomThemes()) custom.add(theme.toJson());
            themes.add("custom", custom); sections.add("themes", themes);
        }
        if (parts.contains(Part.CART_PROFILE)) {
            try { sections.add("cart_profile", dev.nivorat.arc.ArcMotionProfile.getInstance().exportProfile()); }
            catch (IOException error) { throw new IllegalArgumentException(error.getMessage(), error); }
        }
        JsonObject root = new JsonObject();
        root.addProperty("type", "nivoratclient_preset");
        root.addProperty("version", 1);
        root.addProperty("name", name);
        root.add("sections", sections);
        return root;
    }

    private static JsonObject filter(JsonObject source, Part part) {
        JsonObject result = new JsonObject();
        for (var entry : source.entrySet()) {
            String key = entry.getKey();
            if (key.equals("client")) {
                if (part == Part.HUD && entry.getValue().isJsonObject()) {
                    JsonObject client = new JsonObject();
                    JsonObject ui = new JsonObject();
                    var sourceUi = entry.getValue().getAsJsonObject().get("ui");
                    if (sourceUi instanceof JsonObject obj) for (var value : obj.entrySet())
                        if (value.getKey().startsWith("hud")) ui.add(value.getKey(), value.getValue().deepCopy());
                    client.add("ui", ui); result.add("client", client);
                }
                continue;
            }
            if (key.equals("modules") && entry.getValue() instanceof JsonObject modules) {
                JsonObject selected = new JsonObject();
                for (var module : modules.entrySet()) {
                    boolean hud = HUD_IDS.contains(module.getKey());
                    if (part != Part.BINDS && (part == Part.HUD) != hud) continue;
                    selected.add(module.getKey(), filterBinds(module.getValue(), part == Part.BINDS));
                }
                result.add(key, selected);
            } else if (isHud(key) == (part == Part.HUD) && isBind(key) == (part == Part.BINDS)
                    && (part != Part.MODULES || isModuleField(key))) {
                result.add(key, entry.getValue().deepCopy());
            }
        }
        return result;
    }

    private static boolean isHud(String key) { String lower = key.toLowerCase(Locale.ROOT); return lower.startsWith("hpreaper") || lower.startsWith("hpfocus") || lower.startsWith("carthud") || lower.startsWith("cooldownhud") || lower.startsWith("hud") || Set.of("overlayOpacity", "autoHideOnChat", "hideInF3", "showCoordinates", "showFps", "showBiome", "showWorldTime", "showDirection", "coordFormat", "customTitle", "textShadow").contains(key); }
    private static boolean isModuleField(String key) {
        if (key.equals("pinnedModules")) return true;
        String normalized = key.toLowerCase(Locale.ROOT);
        for (var module : activity.client.module.api.ModuleRegistry.getAll()) if (normalized.startsWith(module.getId().replace("_", ""))) return true;
        return false;
    }
    private static boolean isBind(String key) { String lower = key.toLowerCase(Locale.ROOT); return lower.contains("keybind") || lower.endsWith("bind"); }

    private static JsonElement filterBinds(JsonElement element, boolean onlyBinds) {
        if (!element.isJsonObject()) return element.deepCopy();
        JsonObject result = new JsonObject();
        for (var entry : element.getAsJsonObject().entrySet()) {
            if (isBind(entry.getKey()) == onlyBinds) result.add(entry.getKey(), entry.getValue().deepCopy());
            else if (!isBind(entry.getKey()) && entry.getValue().isJsonObject()) {
                JsonElement nested = filterBinds(entry.getValue(), onlyBinds);
                if (!nested.getAsJsonObject().isEmpty()) result.add(entry.getKey(), nested);
            }
        }
        return result;
    }

    public static Path create(String name, Template template, Set<Part> parts) throws IOException {
        return create(name, template, parts, null);
    }

    public static Path create(String name, Template template, Set<Part> parts, Set<String> moduleIds) throws IOException {
        String cleanName = PresetManager.validatePresetName(name);
        if (list().stream().anyMatch(entry -> entry.name().equalsIgnoreCase(cleanName))) throw new IOException("Пресет с таким названием уже существует");
        JsonObject root = capture(name, template, parts, moduleIds);
        Files.createDirectories(directory());
        Path file = directory().resolve(UUID.randomUUID() + ".json");
        write(file, root);
        return file;
    }

    public static Preview read(Path path) throws IOException {
        if (!Files.isRegularFile(path) || Files.size(path) > MAX_BYTES) throw new IOException("Файл отсутствует или превышает 1 МБ");
        String raw = Files.readString(path, StandardCharsets.UTF_8).trim();
        Preview preview = parse(raw);
        if (!raw.startsWith("NVP1:")) {
            try {
                JsonObject root = new JsonObject();
                root.addProperty("type", "nivoratclient_preset");
                root.addProperty("version", 1);
                root.addProperty("name", preview.name());
                root.add("sections", preview.sections());
                write(path, root);
            } catch (Throwable ignored) {}
        }
        return preview;
    }

    public static Preview parse(String text) throws IOException {
        if (text == null || text.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) throw new IOException("Пресет превышает 1 МБ");
        String candidate = text.trim();
        if (candidate.startsWith("NVP1:")) {
            String decrypted = activity.client.util.Obf.decrypt(candidate.substring(5));
            if (decrypted == null || decrypted.isEmpty()) throw new IOException("Не удалось расшифровать пресет");
            candidate = decrypted.trim();
        } else if (!candidate.startsWith("{")) {
            String decrypted = activity.client.util.Obf.decrypt(candidate);
            if (decrypted != null && decrypted.trim().startsWith("{")) {
                candidate = decrypted.trim();
            }
        }
        try {
            JsonElement tree = JsonParser.parseString(candidate);
            checkTree(tree, 0);
            JsonObject root = tree.getAsJsonObject();
            if (!root.get("type").getAsString().equals("nivoratclient_preset") || root.get("version").getAsInt() != 1)
                throw new IllegalArgumentException("Неподдерживаемый формат пресета");
            String name = PresetManager.validatePresetName(root.get("name").getAsString());
            JsonObject sections = root.getAsJsonObject("sections").deepCopy();
            if (sections.isEmpty() || sections.size() > Part.values().length) throw new IllegalArgumentException("Нет разделов настроек");
            for (var section : sections.entrySet()) {
                Part part = Part.valueOf(section.getKey().toUpperCase(Locale.ROOT));
                JsonObject values = section.getValue().getAsJsonObject();
                if (part == Part.MODULES || part == Part.BINDS || part == Part.HUD) {
                    JsonObject allowed = filter(values, part);
                    if (!allowed.equals(values)) throw new IllegalArgumentException("Настройки не соответствуют разделу");
                    ActivityConfig decoded = GSON.fromJson(values, ActivityConfig.class);
                    decoded.sanitize();
                } else if (part == Part.CART_PROFILE) dev.nivorat.arc.ArcMotionProfile.validateSharedProfile(values);
                else if (part == Part.THEMES) {
                    if (values.getAsJsonArray("custom").size() > 64 || values.getAsJsonArray("pinned").size() > 128) throw new IllegalArgumentException("Слишком много тем");
                    values.get("selected").getAsString();
                    for (var theme : values.getAsJsonArray("custom")) activity.client.gui.custom.api.ui.theme.CustomTheme.fromJson(theme.getAsJsonObject());
                } else for (var value : values.entrySet()) if (!value.getValue().isJsonPrimitive())
                    throw new IllegalArgumentException("Недопустимое значение оформления");
            }
            return new Preview(name, sections, List.copyOf(sections.keySet()));
        } catch (RuntimeException error) { throw new IOException("Некорректный пресет: " + error.getMessage(), error); }
    }

    private static void checkTree(JsonElement tree, int depth) {
        if (depth > 24) throw new IllegalArgumentException("Слишком глубокая структура");
        if (tree.isJsonNull()) return;
        if (tree instanceof JsonObject obj) for (var value : obj.entrySet()) checkTree(value.getValue(), depth + 1);
        else if (tree instanceof JsonArray array) for (var value : array) checkTree(value, depth + 1);
        else if (tree.getAsJsonPrimitive().isNumber() && !Double.isFinite(tree.getAsDouble())) throw new IllegalArgumentException("Некорректное число");
    }

    public static JsonObject merge(JsonObject current, JsonObject incoming) {
        JsonObject result = current.deepCopy();
        for (var entry : incoming.entrySet()) {
            if (entry.getValue() instanceof JsonObject object && result.get(entry.getKey()) instanceof JsonObject old)
                result.add(entry.getKey(), merge(old, object));
            else result.add(entry.getKey(), entry.getValue().deepCopy());
        }
        return result;
    }

    public static List<String> changes(Preview preview) {
        List<String> result = new ArrayList<>();
        JsonObject current = PresetSerializer.extractSettingsSnapshot(ActivityConfigManager.getConfig().copy());
        for (var section : preview.sections().entrySet()) {
            if (section.getKey().equals("cart_profile")) { result.add("Автокарт: сохранённый профиль параметров будет заменён"); continue; }
            if (section.getKey().equals("themes")) { result.add("Темы: " + section.getValue().getAsJsonObject().getAsJsonArray("custom").size() + " пользовательских; выбранная тема и закрепления"); continue; }
            JsonObject before = switch (section.getKey()) {
                case "modules" -> filter(current, Part.MODULES);
                case "binds" -> filter(current, Part.BINDS);
                case "hud" -> filter(current, Part.HUD);
                default -> GSON.toJsonTree(VisualSettingsStore.snapshot(section.getKey())).getAsJsonObject();
            };
            diff(before, section.getValue().getAsJsonObject(), section.getKey(), result);
        }
        return List.copyOf(result);
    }

    private static void diff(JsonObject before, JsonObject after, String prefix, List<String> result) {
        for (var value : after.entrySet()) {
            String key = prefix + "." + value.getKey();
            if (value.getValue() instanceof JsonObject object && before.get(value.getKey()) instanceof JsonObject old) diff(old, object, key, result);
            else if (!value.getValue().equals(before.get(value.getKey()))) result.add(describePath(key) + ": " + formatValue(before.get(value.getKey())) + " → " + formatValue(value.getValue()));
        }
    }

    private static String describePath(String path) {
        String[] nodes = path.split("\\.");
        boolean ru = activity.client.i18n.LocalizationService.isRussianPreferred();
        String prefix = switch (nodes[0]) { case "modules" -> ru ? "Модули" : "Modules"; case "binds" -> ru ? "Бинды" : "Bindings"; case "hud" -> "HUD"; case "menu" -> ru ? "Меню" : "Menu"; case "sounds" -> ru ? "Звуки" : "Sounds"; default -> ru ? "Оформление" : "Appearance"; };
        String leaf = nodes[nodes.length - 1];
        for (int i = 1; i < nodes.length; i++) {
            var module = activity.client.module.api.ModuleRegistry.get(nodes[i]);
            if (module == null) continue;
            String label = leaf.equals("enabled") ? (ru ? "Включено" : "Enabled") : leaf.replace('_', ' ');
            for (int j = i + 1; j < nodes.length; j++) { var setting = module.getSetting(nodes[j]); if (setting != null) { label = activity.client.gui.custom.VisualText.settingName(module, setting); break; } }
            return activity.client.gui.custom.VisualText.moduleName(module) + " / " + label;
        }
        return prefix + " / " + leaf.replaceAll("([a-z])([A-Z])", "$1 $2").replace('_', ' ');
    }

    private static String formatValue(JsonElement value) {
        if (value == null || value.isJsonNull()) return "-";
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) return activity.client.i18n.LocalizationService.isRussianPreferred() ? value.getAsBoolean() ? "Вкл" : "Выкл" : value.getAsBoolean() ? "On" : "Off";
        return value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() ? value.getAsString() : value.toString();
    }

    public static void apply(Preview preview) throws IOException {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) throw new IOException("Клиент остановлен до перезапуска");
        EnumSet<Part> backupParts = EnumSet.of(Part.MODULES, Part.BINDS, Part.HUD, Part.APPEARANCE, Part.MENU, Part.SOUNDS, Part.THEMES);
        if (preview.sections().has("cart_profile")) backupParts.add(Part.CART_PROFILE);
        Path backups = directory().resolve("before-apply"); Files.createDirectories(backups);
        write(backups.resolve(System.currentTimeMillis() + "-" + UUID.randomUUID() + ".json"), capture("Перед применением", Template.CURRENT, backupParts));
        if (preview.sections().has("cart_profile")) dev.nivorat.arc.ArcMotionProfile.getInstance().importProfile(preview.sections().getAsJsonObject("cart_profile"));
        JsonObject merged = GSON.toJsonTree(ActivityConfigManager.getConfig()).getAsJsonObject();
        for (String group : List.of("modules", "binds", "hud")) if (preview.sections().has(group)) merged = merge(merged, preview.sections().getAsJsonObject(group));
        String preservedTheme = ActivityConfigManager.getConfig() != null && ActivityConfigManager.getConfig().guiTheme != null && !ActivityConfigManager.getConfig().guiTheme.isEmpty()
                ? ActivityConfigManager.getConfig().guiTheme
                : activity.client.gui.custom.api.ui.theme.ThemeManager.currentThemeId();
        ActivityConfig next = GSON.fromJson(merged, ActivityConfig.class);
        next.syncFromModuleEntries(); next.sanitize();
        if (!preview.sections().has("themes")) {
            next.guiTheme = preservedTheme;
        }
        ActivityConfigManager.setConfig(next);
        if (preview.sections().has("themes")) {
            var themes = preview.sections().getAsJsonObject("themes");
            activity.client.gui.custom.api.ui.theme.CustomThemeManager.init();
            for (var theme : themes.getAsJsonArray("custom")) if (!activity.client.gui.custom.api.ui.theme.CustomThemeManager.save(activity.client.gui.custom.api.ui.theme.CustomTheme.fromJson(theme.getAsJsonObject()))) throw new IOException("Не удалось сохранить тему");
            List<String> pins = new ArrayList<>(); for (var pin : themes.getAsJsonArray("pinned")) pins.add(pin.getAsString());
            activity.client.gui.custom.api.ui.theme.ThemePins.load(pins);
            activity.client.gui.custom.api.ui.theme.ThemeManager.setById(themes.get("selected").getAsString());
        } else {
            activity.client.gui.custom.api.ui.theme.ThemeManager.setById(preservedTheme);
            next.guiTheme = preservedTheme;
        }
        for (String group : List.of("appearance", "menu", "sounds")) if (preview.sections().has(group)) {
            Map<String, Object> values = new LinkedHashMap<>();
            preview.sections().getAsJsonObject(group).entrySet().forEach(entry -> {
                JsonPrimitive value = entry.getValue().getAsJsonPrimitive();
                values.put(entry.getKey(), value.isBoolean() ? value.getAsBoolean() : value.isNumber() ? value.getAsNumber() : value.getAsString());
            });
            VisualSettingsStore.apply(group, values);
        }
        if (!preview.sections().has("themes")) {
            activity.client.gui.custom.api.ui.theme.ThemeManager.setById(preservedTheme);
            next.guiTheme = preservedTheme;
        }
        ActivityConfigManager.markDirty(); ActivityConfigManager.save(); VisualSettingsStore.save();
    }

    public static Path importString(String text) throws IOException {
        Preview preview = parse(text);
        JsonObject root = new JsonObject(); root.addProperty("type", "nivoratclient_preset"); root.addProperty("version", 1);
        String name = preview.name();
        Set<String> names = new HashSet<>(); for (var entry : list()) names.add(entry.name().toLowerCase(Locale.ROOT));
        for (int i = 1; names.contains(name.toLowerCase(Locale.ROOT)); i++) { String suffix = " (" + i + ")"; name = preview.name().substring(0, Math.min(preview.name().length(), 32 - suffix.length())) + suffix; }
        root.addProperty("name", name); root.add("sections", preview.sections());
        Files.createDirectories(directory()); Path destination = directory().resolve(UUID.randomUUID() + ".json");
        write(destination, root); return destination;
    }

    public static String exportString(Path source) throws IOException {
        Preview preview = read(source);
        JsonObject root = new JsonObject();
        root.addProperty("type", "nivoratclient_preset");
        root.addProperty("version", 1);
        root.addProperty("name", preview.name());
        root.add("sections", preview.sections());
        String text = GSON.toJson(root);
        return "NVP1:" + activity.client.util.Obf.encrypt(text);
    }

    public static Path importFile(Path external) throws IOException {
        Preview preview = read(external);
        JsonObject root = new JsonObject(); root.addProperty("type", "nivoratclient_preset"); root.addProperty("version", 1);
        String name = preview.name();
        Set<String> names = new HashSet<>(); for (var entry : list()) names.add(entry.name().toLowerCase(Locale.ROOT));
        for (int i = 1; names.contains(name.toLowerCase(Locale.ROOT)); i++) { String suffix = " (" + i + ")"; name = preview.name().substring(0, Math.min(preview.name().length(), 32 - suffix.length())) + suffix; }
        root.addProperty("name", name); root.add("sections", preview.sections());
        Files.createDirectories(directory()); Path destination = directory().resolve(UUID.randomUUID() + ".json");
        write(destination, root); return destination;
    }

    public static Path export(Path source) throws IOException {
        Preview preview = read(source);
        Path exports = directory().resolve("exports"); Files.createDirectories(exports);
        Path destination = exports.resolve(source.getFileName());
        JsonObject root = new JsonObject();
        root.addProperty("type", "nivoratclient_preset");
        root.addProperty("version", 1);
        root.addProperty("name", preview.name());
        root.add("sections", preview.sections());
        write(destination, root); return destination;
    }

    private static void write(Path file, JsonObject root) throws IOException {
        String text = GSON.toJson(root); parse(text);
        String encrypted = "NVP1:" + activity.client.util.Obf.encrypt(text);
        activity.client.gui.custom.utils.storage.AtomicFiles.writeUtf8(file, encrypted);
    }
}

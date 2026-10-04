package activity.client.gui.custom.api.ui.theme;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

public final class CustomThemeManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<CustomTheme> customThemes = new ArrayList<>();
    private static Path themesDir;
    private static boolean initialized = false;

    private CustomThemeManager() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        try {
            Path base = null;
            try {
                var loader = FabricLoader.getInstance();
                if (loader != null && loader.getGameDir() != null) {
                    base = loader.getGameDir();
                }
            } catch (Throwable ignored) {}
            if (base == null) base = Path.of(".");
            themesDir = base.resolve("nc-visual").resolve("themes");
            if (!Files.exists(themesDir)) {
                Files.createDirectories(themesDir);
            }
            loadAll();
        } catch (Exception ignored) {
        }
    }

    public static List<CustomTheme> getCustomThemes() {
        if (!initialized) init();
        return Collections.unmodifiableList(customThemes);
    }

    public static CustomTheme getById(String id) {
        if (id == null) return null;
        if (!initialized) init();
        for (CustomTheme t : customThemes) {
            if (t.id().equalsIgnoreCase(id)) {
                return t;
            }
        }
        return null;
    }

    public static void loadAll() {
        if (themesDir == null || !Files.exists(themesDir)) return;
        customThemes.clear();
        try (Stream<Path> stream = Files.list(themesDir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(p -> {
                try (BufferedReader reader = Files.newBufferedReader(p)) {
                    JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                    CustomTheme theme = CustomTheme.fromJson(obj);
                    if (theme != null) {
                        customThemes.add(theme);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean save(CustomTheme theme) {
        if (theme == null || themesDir == null) return false;
        try {
            if (!Files.exists(themesDir)) {
                Files.createDirectories(themesDir);
            }
            String safeName = theme.id().replaceAll("[^a-zA-Z0-9_-]", "_") + ".json";
            Path file = themesDir.resolve(safeName);
            activity.client.gui.custom.utils.storage.AtomicFiles.writeUtf8(file, GSON.toJson(theme.toJson()));
            int existing = -1;
            for (int i = 0; i < customThemes.size(); i++) if (customThemes.get(i).id().equals(theme.id())) { existing = i; break; }
            if (existing >= 0) customThemes.set(existing, theme);
            else customThemes.add(theme);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean delete(CustomTheme theme) {
        if (theme == null || themesDir == null) return false;
        try {
            String safeName = theme.id().replaceAll("[^a-zA-Z0-9_-]", "_") + ".json";
            Path file = themesDir.resolve(safeName);
            if (Files.exists(file)) {
                Files.delete(file);
            }
            customThemes.remove(theme);
            if (ThemeManager.currentTheme() == theme) {
                ThemeManager.set(Theme.NIVORA);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static CustomTheme duplicate(ITheme source) {
        if (source == null) return null;
        String newId = "custom_" + System.currentTimeMillis();
        String newName = source.displayName() + " (Copy)";
        int[] paletteCopy = source.palette().clone();
        CustomTheme copy = new CustomTheme(newId, newName, source.profile(), paletteCopy);
        if (source instanceof CustomTheme custom) copy.setPaletteAlpha(custom.paletteAlpha());
        save(copy);
        return copy;
    }

    public static void exportToClipboard(CustomTheme theme) {
        if (theme == null) return;
        try {
            String json = GSON.toJson(theme.toJson());
            MinecraftClient.getInstance().keyboard.setClipboard(json);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static CustomTheme importFromClipboard() {
        try {
            String clipboard = MinecraftClient.getInstance().keyboard.getClipboard();
            if (clipboard == null || clipboard.isBlank()) return null;
            JsonObject obj = JsonParser.parseString(clipboard).getAsJsonObject();
            CustomTheme theme = CustomTheme.fromJson(obj);
            if (theme != null) {
                save(theme);
                return theme;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }
}

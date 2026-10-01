package activity.client.gui.custom;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

public final class CooldownSelections {
    private static final List<String> selected = new ArrayList<>();
    private static boolean loaded;
    private CooldownSelections() {}
    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("nc-cooldown-items.json"); }
    public static List<String> values() {
        if (!loaded) {
            loaded = true;
            try {
                if (Files.exists(path())) {
                    List<String> values = new Gson().fromJson(Files.readString(path()), new TypeToken<List<String>>() {}.getType());
                    if (values != null) for (String value : values) if (value != null && !selected.contains(value)) selected.add(value);
                }
            } catch (Exception error) { activity.client.ActivityClient.LOGGER.warn("Cooldown selections could not be loaded", error); }
        }
        return List.copyOf(selected);
    }
    public static void toggle(String value) {
        values();
        if (!selected.remove(value)) selected.add(value);
        save();
    }
    public static boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String id = "id:" + Registries.ITEM.getId(stack.getItem());
        String name = "name:" + stack.getName().getString();
        for (String value : values()) if (value.equals(id) || value.equalsIgnoreCase(name)) return true;
        return false;
    }
    public static String normalize(String text) { return text.toLowerCase(Locale.ROOT).replace('_', ' ').replace('-', ' ').trim(); }
    private static void save() {
        try { activity.client.gui.custom.utils.storage.AtomicFiles.writeUtf8(path(), new Gson().toJson(selected)); }
        catch (Exception error) { activity.client.ActivityClient.LOGGER.warn("Cooldown selections could not be saved", error); }
    }
}

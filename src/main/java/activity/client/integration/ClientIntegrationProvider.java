package activity.client.integration;

import activity.client.ActivityClient;
import activity.client.gui.custom.VisualSettingsStore;
import activity.client.gui.custom.api.ui.UI;
import net.minecraft.client.gui.screen.Screen;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ClientIntegrationProvider implements Supplier<Map<String, Object>> {
    @Override
    public Map<String, Object> get() {
        return Map.of("version", 1, "modId", ActivityClient.MOD_ID, "sections", List.of(
                section("appearance", "Оформление интерфейса", "Interface appearance"),
                section("menu", "Параметры меню", "Menu settings"),
                section("sounds", "Звуковые эффекты", "Sound effects")),
                "stop", (Runnable) () -> activity.client.capitulation.CapitulationManager.capitulate(net.minecraft.client.MinecraftClient.getInstance()));
    }

    private static Map<String, Object> section(String group, String ru, String en) {
        return Map.of("group", group, "titleRu", ru, "titleEn", en,
                "open", (Function<Screen, Screen>) parent -> UI.integrationScreen(parent, group),
                "read", (Supplier<Map<String, Object>>) () -> VisualSettingsStore.snapshot(group),
                "apply", (Consumer<Map<String, Object>>) values -> VisualSettingsStore.apply(group, values));
    }
}

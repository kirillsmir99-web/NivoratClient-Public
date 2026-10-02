package activity.client.integration;

import activity.client.ActivityClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public final class NivoratEcosystem {
    public static final String ENTRYPOINT = "nivorat:settings_v1";
    private static final Set<String> SHARED_GROUPS = Set.of("appearance", "menu", "sounds");
    private static final Map<String, Section> sections = new LinkedHashMap<>();
    private static final List<Runnable> stopCallbacks = new ArrayList<>();
    private static int revision;
    private static boolean stopped;

    private NivoratEcosystem() {}

    public static void discover() {
        sections.clear();
        stopCallbacks.clear();
        stopped = false;
        revision++;
        try {
            for (var container : FabricLoader.getInstance().getEntrypointContainers(ENTRYPOINT, Supplier.class)) {
                String provider = container.getProvider().getMetadata().getId();
                if (provider.equals(ActivityClient.MOD_ID)) continue;
                try { register(provider, container.getEntrypoint().get()); }
                catch (Exception | LinkageError error) { ActivityClient.LOGGER.warn("Companion adapter failed: {}", provider, error); }
            }
        } catch (Exception | LinkageError error) {
            ActivityClient.LOGGER.warn("Companion discovery failed; using local settings", error);
        }
    }

    static void register(String provider, Object descriptor) {
        if (!(descriptor instanceof Map<?, ?> data) || !Integer.valueOf(1).equals(data.get("version"))
                || !provider.equals(data.get("modId")) || !(data.get("sections") instanceof List<?> list)
                || list.size() > 16) return;
        List<Section> accepted = new ArrayList<>();
        for (Object value : list) {
            if (!(value instanceof Map<?, ?> section) || !(section.get("group") instanceof String group)
                    || !SHARED_GROUPS.contains(group) || !(section.get("open") instanceof Function<?, ?> factory)
                    || !(section.get("titleRu") instanceof String ru) || ru.isBlank() || ru.length() > 80
                    || !(section.get("titleEn") instanceof String en) || en.isBlank() || en.length() > 80) continue;
            accepted.add(new Section(provider, group, ru, en, screenFactory(factory)));
        }
        for (Section section : accepted) {
            Section old = sections.get(section.group());
            if (old == null || section.provider().compareTo(old.provider()) < 0) sections.put(section.group(), section);
        }
        if (!accepted.isEmpty() && data.get("stop") instanceof Runnable stop) stopCallbacks.add(stop);
        revision++;
    }

    @SuppressWarnings("unchecked")
    private static Function<Screen, Screen> screenFactory(Function<?, ?> function) {
        return parent -> {
            Object screen = ((Function<Screen, ?>) function).apply(parent);
            if (!(screen instanceof Screen result) || result == parent) throw new IllegalStateException("Invalid companion screen");
            return result;
        };
    }

    public static List<Section> sections() { return List.copyOf(sections.values()); }
    public static boolean owns(String group) { return !stopped && sections.containsKey(group); }
    public static int revision() { return revision; }

    public static boolean open(String group, Screen parent) {
        if (stopped || activity.client.capitulation.CapitulationManager.isCapitulated()) return false;
        Section section = sections.get(group);
        if (section == null) return false;
        try {
            MinecraftClient.getInstance().setScreen(section.open().apply(parent));
            return true;
        } catch (Exception | LinkageError error) {
            sections.remove(group);
            revision++;
            ActivityClient.LOGGER.warn("Companion settings failed; restored local section {}", group, error);
            activity.client.gui.overlay.ClientNotification.show(net.minecraft.text.Text.literal(
                    activity.client.i18n.LocalizationService.isRussianPreferred()
                            ? "Настройки дополнения недоступны. Восстановлены настройки клиента."
                            : "Companion settings unavailable. Client settings restored."));
            return false;
        }
    }

    public static void stopCompanions() {
        if (stopped) return;
        stopped = true;
        for (Runnable stop : stopCallbacks) {
            try { stop.run(); }
            catch (Exception | LinkageError error) { ActivityClient.LOGGER.warn("Companion stop failed", error); }
        }
        sections.clear();
        stopCallbacks.clear();
        revision++;
    }

    public record Section(String provider, String group, String titleRu, String titleEn,
                          Function<Screen, Screen> open) {}
}

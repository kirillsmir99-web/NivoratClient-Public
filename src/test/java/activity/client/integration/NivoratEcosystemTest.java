package activity.client.integration;

import activity.client.gui.custom.api.modules.ModuleManager;
import activity.client.gui.custom.api.modules.Category;
import activity.client.gui.custom.api.modules.impl.Interface.ClickGui;
import net.minecraft.client.gui.screen.Screen;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;

class NivoratEcosystemTest {
    @BeforeEach void before() { NivoratEcosystem.discover(); }
    @AfterEach void after() { NivoratEcosystem.stopCompanions(); NivoratEcosystem.discover(); }

    private static Map<String, Object> section(String group) {
        return Map.of("group", group, "titleRu", "Настройки Visual", "titleEn", "Visual settings",
                "open", (Function<Screen, Screen>) parent -> null);
    }
    @Test void absenceKeepsAllLocalSettings() {
        assertFalse(NivoratEcosystem.owns("menu"));
        assertTrue(ModuleManager.get().forCategory(Category.DISPLAY).stream().anyMatch(m -> m instanceof ClickGui));
    }
    @Test void incompatibleOrMisidentifiedAdapterCannotHideAnything() {
        NivoratEcosystem.register("nv", Map.of("version", 2, "modId", "nv", "sections", List.of(section("menu"))));
        assertFalse(NivoratEcosystem.owns("menu"));
        NivoratEcosystem.register("nv", Map.of("version", 1, "modId", "other", "sections", List.of(section("menu"))));
        assertFalse(NivoratEcosystem.owns("menu"));
    }
    @Test void compatibleAdapterReplacesOnlyItsDeclaredSettingGroup() {
        NivoratEcosystem.register("nv", Map.of("version", 1, "modId", "nv", "sections", List.of(section("menu"), section("combat"))));
        assertTrue(NivoratEcosystem.owns("menu"));
        assertFalse(NivoratEcosystem.owns("combat"));
        var settings = ModuleManager.get().forCategory(Category.DISPLAY);
        assertFalse(settings.stream().anyMatch(m -> m instanceof ClickGui));
        assertEquals(4, settings.size());
        assertEquals(1, settings.stream().filter(m -> m instanceof CompanionSettingsModule).count());
    }
    @Test void stopRunsOnceAndRestoresLocalOwnership() {
        var count = new AtomicInteger();
        NivoratEcosystem.register("nv", Map.of("version", 1, "modId", "nv", "sections", List.of(section("menu")), "stop", (Runnable) count::incrementAndGet));
        NivoratEcosystem.stopCompanions();
        NivoratEcosystem.stopCompanions();
        assertEquals(1, count.get());
        assertFalse(NivoratEcosystem.owns("menu"));
        assertTrue(ModuleManager.get().forCategory(Category.DISPLAY).stream().anyMatch(m -> m instanceof ClickGui));
    }
}

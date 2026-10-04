package activity.client.gui;

import activity.client.gui.custom.api.modules.Category;
import activity.client.gui.custom.api.modules.Module;
import activity.client.gui.custom.api.modules.ModuleManager;
import activity.client.gui.navigation.PvpKit;
import activity.client.module.api.BuiltinModules;
import activity.client.module.api.ModuleRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class KitCategorizationExactMatchTest {

    @BeforeAll
    static void init() {
        if (ModuleRegistry.getAll().isEmpty()) {
            BuiltinModules.registerAll();
        }
    }

    @Test
    void testNetheritePotInputKitExactMatch() {
        Set<String> expected = Set.of("auto_totem", "hp_reaper", "auto_gg", "cooldown_hud");
        assertKitExactMatch(PvpKit.NETHERITE_POT, Category.NPOT, expected);
    }

    @Test
    void testCrystalKitExactMatch() {
        Set<String> expected = Set.of("click_pearl", "auto_totem", "auto_anchor", "hp_reaper", "cooldown_hud", "auto_gg");
        assertKitExactMatch(PvpKit.CRYSTAL, Category.CRYSTAL, expected);
    }

    @Test
    void testUhcKitExactMatch() {
        Set<String> expected = Set.of("auto_shieldbreaker", "auto_gg", "cooldown_hud", "auto_tool");
        assertKitExactMatch(PvpKit.UHC, Category.UHC, expected);
    }

    @Test
    void testSmpKitExactMatch() {
        Set<String> expected = Set.of("auto_shieldbreaker", "click_pearl", "auto_totem", "hp_reaper", "auto_tool", "auto_gg", "cooldown_hud");
        assertKitExactMatch(PvpKit.SMP, Category.SMP, expected);
    }

    @Test
    void testMaceMesaKitExactMatch() {
        Set<String> expected = Set.of(
                "auto_mace", "auto_spear", "auto_stun_slam", "auto_pearl_catch",
                "click_pearl", "elytra_swap", "auto_totem", "hp_reaper", "auto_gg", "cooldown_hud"
        );
        assertKitExactMatch(PvpKit.MACE, Category.MACE, expected);
        assertFalse(PvpKit.MACE.matchesId("auto_shieldbreaker"));
    }

    @Test
    void testBeastBestagKitExactMatch() {
        Set<String> expected = Set.of("auto_gg", "hp_reaper");
        assertKitExactMatch(PvpKit.BEAST, Category.BEAST, expected);
    }

    @Test
    void testSwordVpKitExactMatch() {
        Set<String> expected = Set.of("click_pearl", "hp_reaper", "cooldown_hud", "auto_gg");
        assertKitExactMatch(PvpKit.SWORD, Category.SWORD, expected);
    }

    @Test
    void testAxeKitExactMatch() {
        Set<String> expected = Set.of("auto_shieldbreaker", "auto_tool", "auto_gg", "hp_reaper", "cooldown_hud");
        assertKitExactMatch(PvpKit.AXE, Category.AXE, expected);
    }

    @Test
    void testDiamondPotDepoteKitExactMatch() {
        Set<String> expected = Set.of("hp_reaper", "auto_gg", "cooldown_hud");
        assertKitExactMatch(PvpKit.DIAMOND_POT, Category.DPOT, expected);
        assertFalse(PvpKit.DIAMOND_POT.matchesId("click_pearl"));
    }

    @Test
    void testCartKitExactMatch() {
        Set<String> expected = Set.of(
            "click_pearl", "auto_totem", "hp_reaper", "auto_gg",
            "cooldown_hud", "cart_refill", "auto_cart", "cart_hud"
        );
        assertKitExactMatch(PvpKit.CART, Category.CART, expected);
        assertTrue(PvpKit.CART.matchesId("auto_cart"));
        assertTrue(PvpKit.CART.matchesId("cart_refill"));
        assertTrue(PvpKit.CART.matchesId("cart_hud"));
        assertFalse(PvpKit.CART.matchesId("auto_mace"));
    }

    @Test
    void testUtilsCategoryReturnsEmptySafely() {
        List<Module> modules = ModuleManager.get().forCategory(Category.UTILS);
        assertNotNull(modules);
        assertTrue(modules.isEmpty());
    }

    @Test
    void testPresetsCategoryReturnsEmptySafely() {
        List<Module> modules = ModuleManager.get().forCategory(Category.PRESETS);
        assertNotNull(modules);
        assertTrue(modules.isEmpty());
    }

    @Test
    void testDisplayCategoryDoesNotThrow() {
        assertDoesNotThrow(() -> {
            List<Module> modules = ModuleManager.get().forCategory(Category.DISPLAY);
            assertNotNull(modules);
            for (Module m : modules) {
                assertNotNull(m.getId());
            }
        });
    }

    private void assertKitExactMatch(PvpKit kit, Category category, Set<String> expectedIds) {
        Set<String> kitMatchedIds = ModuleRegistry.getAll().stream()
                .filter(kit::matches)
                .map(m -> m.getId())
                .collect(Collectors.toSet());
        assertEquals(expectedIds, kitMatchedIds, "PvpKit." + kit.name() + " mismatch");

        Set<String> categoryMatchedIds = ModuleManager.get().forCategory(category).stream()
                .map(m -> m.getId())
                .collect(Collectors.toSet());
        assertEquals(expectedIds, categoryMatchedIds, "Category." + category.name() + " mismatch in ModuleManager");
    }
}

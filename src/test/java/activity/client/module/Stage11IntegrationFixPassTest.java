package activity.client.module;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.sidebar.SidebarTree;
import activity.client.gui.tab.CombatTab;
import activity.client.gui.tab.DefenseTab;
import activity.client.gui.tab.UtilityTab;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleEventDispatcher;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.api.NivoratModule;
import activity.client.module.impl.utility.CartHudModule;
import activity.client.module.setting.BooleanSetting;
import activity.client.module.setting.Setting;
import activity.client.module.setting.SettingSection;
import net.minecraft.text.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class Stage11IntegrationFixPassTest {

    private static final String DYNAMIC_COMBAT_ID = "custom_dynamic_combat_test";

    @BeforeEach
    void setUp() {
        ActivityConfigManager.resetDefaults();
        ModuleRegistry.unregister(DYNAMIC_COMBAT_ID);
        ModuleEventDispatcher.updateActiveModules();
    }

    @AfterEach
    void tearDown() {
        ModuleRegistry.unregister(DYNAMIC_COMBAT_ID);
        ActivityConfigManager.resetDefaults();
        ModuleEventDispatcher.updateActiveModules();
    }

    @Test
    @DisplayName("SidebarTree.isModuleActive queries structured ModuleConfigEntry for dynamic modules")
    void testSidebarTreeDynamicModuleActiveQuery() {
        NivoratModule dynamicMod = new NivoratModule(
                DYNAMIC_COMBAT_ID,
                Text.literal("Dynamic Combat"),
                Text.literal("Dynamic Combat Description"),
                ModuleCategory.COMBAT
        ) {};

        ModuleRegistry.register(dynamicMod);

        ActivityConfig config = ActivityConfigManager.getConfig();
        assertNotNull(config);

        ActivityConfig.ModuleConfigEntry entry = new ActivityConfig.ModuleConfigEntry();
        entry.enabled = false;
        config.modules.put(DYNAMIC_COMBAT_ID, entry);

        SidebarTree sidebarTree = new SidebarTree();
        SidebarTree.CategoryNode combatNode = sidebarTree.getCategories().get(0);
        assertEquals("combat", combatNode.getId());

        SidebarTree.ModuleItem childItem = combatNode.getChildren().stream()
                .filter(item -> DYNAMIC_COMBAT_ID.equals(item.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull(childItem, "Dynamic module must be present in SidebarTree combat category");
        assertFalse(childItem.isActive(), "Dynamic module isActive must reflect ModuleConfigEntry.enabled == false");

        entry.enabled = true;
        assertTrue(childItem.isActive(), "Dynamic module isActive must reflect ModuleConfigEntry.enabled == true");
    }

    @Test
    @DisplayName("CombatTab, DefenseTab, and UtilityTab report correct ModuleCategory")
    void testTabCategoryReporting() {
        CombatTab combatTab = new CombatTab();
        DefenseTab defenseTab = new DefenseTab();
        UtilityTab utilityTab = new UtilityTab();

        assertEquals(ModuleCategory.COMBAT, combatTab.getCategory());
        assertEquals(ModuleCategory.DEFENSE, defenseTab.getCategory());
        assertEquals(ModuleCategory.UTILITY, utilityTab.getCategory());
    }

    @Test
    @DisplayName("Tab resetDefaults resets Setting values for all modules in category including dynamic modules")
    void testTabResetDefaultsResetsDynamicModuleSettings() {
        CombatTab combatTab = new CombatTab();

        AtomicBoolean customFlag = new AtomicBoolean(false);
        class CustomCombatModule extends NivoratModule {
            CustomCombatModule() {
                super(DYNAMIC_COMBAT_ID, Text.literal("Custom Combat"), Text.literal("Desc"), ModuleCategory.COMBAT);
                registerBoolean("custom_flag", Text.literal("Custom Flag"), Text.literal("Desc"),
                        SettingSection.GENERAL, true, customFlag::get, customFlag::set);
            }
        }

        CustomCombatModule customMod = new CustomCombatModule();
        ModuleRegistry.register(customMod);

        Setting<?> setting = customMod.getSetting("custom_flag");
        assertNotNull(setting);
        ((BooleanSetting) setting).set(false);
        assertFalse(customFlag.get());

        combatTab.resetDefaults();

        assertTrue(customFlag.get(), "Custom module setting must be reset to defaultValue (true)");
        assertTrue((Boolean) setting.get());
    }

    @Test
    @DisplayName("IModule hasTickLogic default is true, CartHudModule overrides to false")
    void testHasTickLogicContract() {
        IModule cartHud = ModuleRegistry.get(CartHudModule.ID);
        assertNotNull(cartHud);
        assertFalse(cartHud.hasTickLogic(), "CartHudModule must return hasTickLogic() == false");

        IModule autoMace = ModuleRegistry.get("auto_mace");
        assertNotNull(autoMace);
        assertTrue(autoMace.hasTickLogic(), "ParticlePhysicsModule must return hasTickLogic() == true");
    }

    @Test
    @DisplayName("ModuleEventDispatcher excludes CartHudModule from tick modules via hasTickLogic")
    void testModuleEventDispatcherExcludesCartHudFromTick() {
        IModule cartHud = ModuleRegistry.get(CartHudModule.ID);
        assertNotNull(cartHud);
        cartHud.setEnabled(true);

        ModuleEventDispatcher.updateActiveModules();

        IModule[] tickModules = ModuleEventDispatcher.getActiveTickModules();
        boolean cartInTick = Arrays.stream(tickModules).anyMatch(m -> CartHudModule.ID.equals(m.getId()));
        assertFalse(cartInTick, "CartHudModule must not be present in activeTickModules");

        IModule[] hudModules = ModuleEventDispatcher.getActiveHudModules();
        boolean cartInHud = Arrays.stream(hudModules).anyMatch(m -> CartHudModule.ID.equals(m.getId()));
        assertTrue(cartInHud, "CartHudModule must still be present in activeHudModules");
    }

    @Test
    @DisplayName("Regression: All 14 built-in modules are registered and correctly categorized")
    void testAll12ModulesRegression() {
        List<String> expectedCombat = List.of("auto_mace", "auto_spear", "auto_shieldbreaker", "auto_stun_slam", "auto_pearl_catch");
        List<String> expectedDefense = List.of("auto_totem", "auto_cart", "auto_anchor", "cart_refill");
        List<String> expectedUtility = List.of("hp_reaper", "auto_tool", "auto_gg", "cart_hud", "cooldown_hud");

        assertEquals(14, ModuleRegistry.getAll().size(), "Total built-in modules count must be 14");

        for (String id : expectedCombat) {
            IModule mod = ModuleRegistry.get(id);
            assertNotNull(mod, "Module " + id + " must be registered");
            assertEquals(ModuleCategory.COMBAT, mod.getCategory(), "Module " + id + " must be COMBAT");
            assertNotNull(mod.getName());
            assertNotNull(mod.getDescription());
            assertFalse(mod.getSettings().isEmpty(), "Module " + id + " must have settings");
        }

        for (String id : expectedDefense) {
            IModule mod = ModuleRegistry.get(id);
            assertNotNull(mod, "Module " + id + " must be registered");
            assertEquals(ModuleCategory.DEFENSE, mod.getCategory(), "Module " + id + " must be DEFENSE");
            assertNotNull(mod.getName());
            assertNotNull(mod.getDescription());
            assertFalse(mod.getSettings().isEmpty(), "Module " + id + " must have settings");
        }

        for (String id : expectedUtility) {
            IModule mod = ModuleRegistry.get(id);
            assertNotNull(mod, "Module " + id + " must be registered");
            assertEquals(ModuleCategory.UTILITY, mod.getCategory(), "Module " + id + " must be UTILITY");
            assertNotNull(mod.getName());
            assertNotNull(mod.getDescription());
            assertFalse(mod.getSettings().isEmpty(), "Module " + id + " must have settings");
        }
    }
}

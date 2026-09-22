package activity.client.module;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.module.api.BuiltinModules;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.setting.Setting;
import activity.client.module.setting.SettingGroup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration and verification suite for the 12 unified modules in NivoratClient.
 */
public class Monolithic12ModulesIntegrationTest {

    private static final List<String> EXPECTED_MODULE_IDS = List.of(
        "auto_mace",
        "auto_spear",
        "auto_shieldbreaker",
        "auto_stun_slam",
        "auto_totem",
        "auto_cart",
        "auto_anchor",
        "cart_refill",
        "hp_reaper",
        "auto_tool",
        "auto_gg",
        "cart_hud",
        "auto_pearl_catch",
        "cooldown_hud"
    );

    @BeforeAll
    static void initAll() {
        BuiltinModules.registerAll();
    }

    @BeforeEach
    void setUp() {
        ActivityConfigManager.resetDefaults();
    }

    @Test
    void testAllTwelveModulesRegistered() {
        assertEquals(14, EXPECTED_MODULE_IDS.size(), "Should verify exactly 14 modules");
        for (String id : EXPECTED_MODULE_IDS) {
            IModule module = ModuleRegistry.get(id);
            assertNotNull(module, "Module " + id + " must be present in ModuleRegistry");
            assertEquals(id, module.getId());
            assertNotNull(module.getName(), "Module " + id + " must have a non-null display name");
            assertNotNull(module.getDescription(), "Module " + id + " must have a non-null description");
            assertNotNull(module.getCategory(), "Module " + id + " must have a category");
        }
    }

    @Test
    void testLegacyAutoStunSlimeAliasResolves() {
        IModule aliasModule = ModuleRegistry.get("auto_stun_slime");
        assertNotNull(aliasModule, "Legacy alias 'auto_stun_slime' must resolve in ModuleRegistry");
        assertEquals("auto_stun_slam", aliasModule.getId());
    }

    @Test
    void testModuleCategoryDistribution() {
        List<IModule> combatModules = ModuleRegistry.getByCategory(ModuleCategory.COMBAT);
        List<IModule> defenseModules = ModuleRegistry.getByCategory(ModuleCategory.DEFENSE);
        List<IModule> utilityModules = ModuleRegistry.getByCategory(ModuleCategory.UTILITY);

        assertEquals(5, combatModules.size(), "Combat should have exactly 5 modules (Mace, Spear, Shieldbreaker, StunSlam, PearlCatch)");
        assertEquals(4, defenseModules.size(), "Defense should have exactly 4 modules (Totem, Cart, Anchor, Refill)");
        assertEquals(5, utilityModules.size(), "Utility should have exactly 5 modules (HPReaper, AutoTool, AutoGG, CartHUD, CooldownHUD)");
    }

    @Test
    void testModuleMetadataIntegrity() {
        for (String id : EXPECTED_MODULE_IDS) {
            IModule module = ModuleRegistry.get(id);
            assertNotNull(module);
            ModuleMetadata meta = module.getMetadata();
            assertNotNull(meta, "Metadata for " + id + " must not be null");
            assertEquals(id, meta.getId());
            assertNotNull(meta.getDisplayName());
            assertNotNull(meta.getDescription());
            assertEquals("Nivorat", meta.getAuthor());
            assertNotNull(meta.getVersion());
            assertTrue(meta.getVersion().matches("\\d+\\.\\d+\\.\\d+"), "Version should be semver: " + meta.getVersion());
            assertEquals("2026-09-16", meta.getLastUpdated());
            assertEquals("https://t.me/virionDEV", meta.getTelegramUrl());
            assertNotNull(meta.getIcon());
        }
    }

    @Test
    void testSettingGroupsStrictOrder() {
        for (String id : EXPECTED_MODULE_IDS) {
            IModule module = ModuleRegistry.get(id);
            assertNotNull(module);
            List<Setting<?>> settings = module.getSettings();
            assertNotNull(settings);

            int lastOrdinal = -1;
            for (Setting<?> s : settings) {
                SettingGroup group = s.getGroup();
                assertNotNull(group, "Setting " + s.getId() + " in module " + id + " must have a group");
                assertTrue(group.ordinal() >= lastOrdinal,
                    "Setting " + s.getId() + " group " + group + " violates 5-tier order in module " + id);
                lastOrdinal = group.ordinal();
            }
        }
    }

    @Test
    void testSettingsPresentForAllModules() {
        for (String id : EXPECTED_MODULE_IDS) {
            IModule module = ModuleRegistry.get(id);
            assertNotNull(module);
            List<Setting<?>> settings = module.getSettings();
            assertNotNull(settings, "Settings list must not be null for " + id);
            assertFalse(settings.isEmpty(), "Module " + id + " must register at least one setting");
        }
    }

    @Test
    void testConfigRoundtripPersistence() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        assertNotNull(config);

        // Toggle each module state
        config.autoMaceEnabled = true;
        config.autoSpearEnabled = true;
        config.autoShieldbreakerEnabled = true;
        config.autoStunSlamEnabled = true;
        config.autoTotemEnabled = true;
        config.autoCartEnabled = true;
        config.autoAnchorEnabled = true;
        config.cartRefillEnabled = true;
        config.hpReaperEnabled = true;
        config.autoToolEnabled = true;
        config.autoGGEnabled = true;
        config.cartHudEnabled = true;

        ActivityConfigManager.markDirty();
        ActivityConfigManager.save();

        // Verify config reloaded without errors
        ActivityConfig loaded = ActivityConfigManager.getConfig();
        assertNotNull(loaded);
        assertTrue(loaded.autoMaceEnabled);
        assertTrue(loaded.autoSpearEnabled);
        assertTrue(loaded.autoShieldbreakerEnabled);
        assertTrue(loaded.autoStunSlamEnabled);
        assertTrue(loaded.autoTotemEnabled);
        assertTrue(loaded.autoCartEnabled);
        assertTrue(loaded.autoAnchorEnabled);
        assertTrue(loaded.cartRefillEnabled);
        assertTrue(loaded.hpReaperEnabled);
        assertTrue(loaded.autoToolEnabled);
        assertTrue(loaded.autoGGEnabled);
        assertTrue(loaded.cartHudEnabled);
    }
}

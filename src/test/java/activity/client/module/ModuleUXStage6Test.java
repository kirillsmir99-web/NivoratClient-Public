package activity.client.module;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.icon.ActivityIcon;
import activity.client.gui.menu.ModuleContextMenu;
import activity.client.gui.sheet.AboutModuleSheet;
import activity.client.gui.sidebar.SidebarTree;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.keybind.Keybind;
import activity.client.module.stub.AutoStunSlamStub;
import activity.client.module.stub.AutoStunSlimeStub;
import net.minecraft.text.Text;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive verification for Stage 6 — Module UX.
 * Tests AutoStunSlam rename, backward-compatible migration, ModuleMetadata model,
 * Context Menu overlay, Quick Access sidebar tree, and About Module sheet.
 */
public class ModuleUXStage6Test {

    @BeforeEach
    void setUp() {
        ActivityConfigManager.resetDefaults();
    }

    // =========================================================================
    // 1. AUTOSTUNSLAM & BACKWARD COMPATIBILITY
    // =========================================================================

    @Test
    void testAutoStunSlamStubDefaults() {
        IModule module = ModuleRegistry.get(AutoStunSlamStub.ID);
        assertNotNull(module, "AutoStunSlamStub must be registered in ModuleRegistry");
        assertEquals("auto_stun_slam", module.getId());
        assertEquals(ModuleCategory.COMBAT, module.getCategory());
        assertNotNull(module.getName());
        assertNotNull(module.getDescription());

        ActivityConfig config = ActivityConfigManager.getConfig();
        assertNotNull(config);
        assertTrue(config.autoStunSlamEnabled);
        assertEquals(2.4, config.autoStunSlamDistance, 0.001);
        assertEquals(75.0, config.autoStunSlamChance, 0.001);
        assertEquals(45.0, config.autoStunSlamAxeDelayMs, 0.001);
        assertEquals(45.0, config.autoStunSlamMaceDelayMs, 0.001);
        assertEquals(50.0, config.autoStunSlamRestoreDelayMs, 0.001);
        assertTrue(config.autoStunSlamLegitMode);
    }

    @Test
    void testAutoStunSlimeAliasResolution() {
        // Old ID auto_stun_slime resolves to the same stub
        IModule aliasModule = ModuleRegistry.get("auto_stun_slime");
        assertNotNull(aliasModule, "ModuleRegistry must resolve auto_stun_slime alias");
        assertEquals("auto_stun_slam", aliasModule.getId());

        ModuleMetadata meta = ModuleRegistry.getMetadata("auto_stun_slime");
        assertNotNull(meta);
        assertEquals("auto_stun_slam", meta.getId());
    }

    @Test
    void testLegacyConfigMigration() {
        ActivityConfig oldConfig = new ActivityConfig();
        // Simulate legacy config loaded with old autoStunSlime fields
        oldConfig.autoStunSlimeEnabled = false;
        oldConfig.autoStunSlimeDistance = 3.5;
        oldConfig.autoStunSlimeChance = 90.0;
        oldConfig.autoStunSlimeAxeDelayMs = 60.0;
        oldConfig.autoStunSlimeMaceDelayMs = 65.0;
        oldConfig.autoStunSlimeRestoreDelayMs = 70.0;
        oldConfig.autoStunSlimeLegitMode = false;

        // Trigger sanitize / migration
        oldConfig.sanitize();

        // New canonical fields must reflect migrated values
        assertFalse(oldConfig.autoStunSlamEnabled);
        assertEquals(3.5, oldConfig.autoStunSlamDistance, 0.001);
        assertEquals(90.0, oldConfig.autoStunSlamChance, 0.001);
        assertEquals(60.0, oldConfig.autoStunSlamAxeDelayMs, 0.001);
        assertEquals(65.0, oldConfig.autoStunSlamMaceDelayMs, 0.001);
        assertEquals(70.0, oldConfig.autoStunSlamRestoreDelayMs, 0.001);
        assertFalse(oldConfig.autoStunSlamLegitMode);
    }

    @Test
    void testLegacyPinMigration() {
        ActivityConfig config = new ActivityConfig();
        config.setPinned("auto_stun_slime", true);

        assertTrue(config.isPinned("auto_stun_slam"));
        assertTrue(config.isPinned("auto_stun_slime"));

        config.setPinned("auto_stun_slam", false);
        assertFalse(config.isPinned("auto_stun_slam"));
        assertFalse(config.isPinned("auto_stun_slime"));
    }

    // =========================================================================
    // 2. MODULE METADATA MODEL
    // =========================================================================

    @Test
    void testModuleMetadataDefaultsAndBuilder() {
        Keybind kb = new Keybind(71, false, false, false); // G key
        ModuleMetadata meta = ModuleMetadata.builder("test_module")
            .displayName(Text.literal("Test Module"))
            .description(Text.literal("Test description"))
            .category(ModuleCategory.DEFENSE)
            .icon(ActivityIcon.DEFENSE)
            .keybind(kb)
            .build();

        assertEquals("test_module", meta.getId());
        assertEquals("Test Module", meta.getDisplayName().getString());
        assertEquals("Test description", meta.getDescription().getString());
        assertEquals("Nivorat", meta.getAuthor());
        assertEquals("1.0.0", meta.getVersion());
        assertEquals("2026-09-16", meta.getLastUpdated());
        assertEquals(ModuleCategory.DEFENSE, meta.getCategory());
        assertEquals("https://t.me/virionDEV", meta.getTelegramUrl());
        assertEquals(ActivityIcon.DEFENSE, meta.getIcon());
        assertSame(kb, meta.getKeybind());
        assertTrue(meta.hasKeybind());
        assertEquals("[G]", meta.getKeybindDisplay());
    }

    @Test
    void testModuleMetadataUnboundKeybindDisplay() {
        ModuleMetadata meta = ModuleMetadata.builder("unbound_module")
            .displayName(Text.literal("Unbound"))
            .keybind(new Keybind())
            .build();

        assertFalse(meta.hasKeybind());
        assertEquals("[-]", meta.getKeybindDisplay());
    }

    @Test
    void testAllRegistryModulesHaveCompleteMetadata() {
        for (IModule module : ModuleRegistry.getAll()) {
            ModuleMetadata meta = module.getMetadata();
            assertNotNull(meta, "Module " + module.getId() + " must have metadata");
            assertEquals(module.getId(), meta.getId());
            assertNotNull(meta.getDisplayName());
            assertNotNull(meta.getDescription());
            assertEquals("Nivorat", meta.getAuthor());
            assertEquals("1.0.0", meta.getVersion());
            assertEquals("2026-09-16", meta.getLastUpdated());
            assertEquals("https://t.me/virionDEV", meta.getTelegramUrl());
            assertNotNull(meta.getCategory());
            assertNotNull(meta.getIcon());
        }
    }

    // =========================================================================
    // 3. CONTEXT MENU (RIGHT CLICK)
    // =========================================================================

    @Test
    void testModuleContextMenuBoundsClamping() {
        // High coordinate far outside bounds
        ModuleContextMenu menu = new ModuleContextMenu(null, "auto_mace", 9999, 9999);
        assertNotNull(menu);
        assertTrue(menu.getX() >= 4);
        assertTrue(menu.getY() >= 4);
        assertTrue(menu.getWidth() > 0);
        assertTrue(menu.getHeight() > 0);

        // Negative coordinates
        ModuleContextMenu menuNeg = new ModuleContextMenu(null, "auto_mace", -50, -50);
        assertEquals(4, menuNeg.getX());
        assertEquals(4, menuNeg.getY());
    }

    @Test
    void testModuleContextMenuContainsAndClose() {
        ModuleContextMenu menu = new ModuleContextMenu(null, "auto_mace", 50, 50);
        assertFalse(menu.isClosed());
        assertTrue(menu.contains(50, 50));
        assertTrue(menu.contains(menu.getX() + 10, menu.getY() + 10));
        assertFalse(menu.contains(menu.getX() - 10, menu.getY()));
        assertFalse(menu.contains(menu.getX(), menu.getY() + menu.getHeight() + 10));

        assertTrue(menu.shouldCloseOnClickOutside());
        assertTrue(menu.shouldCloseOnEsc());

        menu.close();
        assertTrue(menu.isClosed());
    }

    // =========================================================================
    // 4. QUICK ACCESS & PINNED MODULES
    // =========================================================================

    @Test
    void testPinPersistence() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        assertFalse(config.isPinned("auto_mace"));
        assertFalse(config.isPinned("auto_spear"));

        config.setPinned("auto_mace", true);
        config.setPinned("auto_spear", true);

        assertTrue(config.isPinned("auto_mace"));
        assertTrue(config.isPinned("auto_spear"));
        assertEquals(List.of("auto_mace", "auto_spear"), config.getPinnedModules());

        config.setPinned("auto_mace", false);
        assertFalse(config.isPinned("auto_mace"));
        assertTrue(config.isPinned("auto_spear"));
        assertEquals(List.of("auto_spear"), config.getPinnedModules());
    }

    @Test
    void testSidebarTreeQuickAccessHeightCalculation() {
        SidebarTree tree = new SidebarTree();
        ActivityConfig config = ActivityConfigManager.getConfig();

        assertEquals(0, tree.getQuickAccessHeight(config.getPinnedModules()));

        config.setPinned("auto_mace", true);
        int h1 = tree.getQuickAccessHeight(config.getPinnedModules());
        assertTrue(h1 > 0);

        config.setPinned("auto_spear", true);
        int h2 = tree.getQuickAccessHeight(config.getPinnedModules());
        assertTrue(h2 > h1);
    }

    // =========================================================================
    // 5. ABOUT MODULE SHEET
    // =========================================================================

    @Test
    void testAboutModuleSheetInitialization() {
        AboutModuleSheet sheet = new AboutModuleSheet(null, "auto_stun_slam");
        assertEquals("auto_stun_slam", sheet.getModuleId());
        assertNotNull(sheet.getMetadata());
        assertEquals("auto_stun_slam", sheet.getMetadata().getId());
        assertEquals("Nivorat", sheet.getMetadata().getAuthor());
        assertEquals("https://t.me/virionDEV", sheet.getMetadata().getTelegramUrl());

        assertFalse(sheet.isClosed());
        assertTrue(sheet.shouldCloseOnClickOutside());
        assertTrue(sheet.shouldCloseOnEsc());

        sheet.close();
        assertTrue(sheet.isClosed());
    }
}

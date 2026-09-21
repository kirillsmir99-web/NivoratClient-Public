package activity.client.module;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.ModuleSettingsView;
import activity.client.gui.icon.ActivityIcon;
import activity.client.gui.layout.ScrollContainer;
import activity.client.gui.menu.ModuleContextMenu;
import activity.client.gui.sheet.AboutModuleSheet;
import activity.client.gui.sidebar.SidebarTree;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.api.NivoratModule;
import activity.client.module.keybind.Keybind;
import activity.client.module.setting.BooleanSetting;
import activity.client.module.setting.SettingGroup;
import activity.client.module.stub.AutoStunSlamStub;
import activity.client.module.stub.AutoStunSlimeStub;
import net.minecraft.text.Text;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
        assertEquals(2.85, config.autoStunSlamDistance, 0.001);
        assertEquals(100.0, config.autoStunSlamChance, 0.001);
        assertEquals(0.0, config.autoStunSlamAxeDelayMs, 0.001);
        assertEquals(0.0, config.autoStunSlamMaceDelayMs, 0.001);
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
            assertNotNull(meta.getAuthor());
            assertNotNull(meta.getVersion());
            assertTrue(meta.getVersion().matches("\\d+\\.\\d+\\.\\d+"), "Version should be semver: " + meta.getVersion());
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

    @Test
    void testTelegramAliasInModuleMetadata() {
        ModuleMetadata meta = ModuleMetadata.builder("test_tg")
                .displayName(Text.literal("TG Test"))
                .telegram("https://t.me/virionDEV")
                .build();
        assertEquals("https://t.me/virionDEV", meta.getTelegram());
        assertEquals("https://t.me/virionDEV", meta.getTelegramUrl());

        IModule mace = ModuleRegistry.get("auto_mace");
        assertNotNull(mace);
        assertNotNull(mace.getMetadata().getTelegram());
        assertEquals("https://t.me/virionDEV", mace.getMetadata().getTelegram());
    }

    // =========================================================================
    // 6. GENERIC MODULE SETTINGS VIEW & REACTIVE VISIBILITY
    // =========================================================================

    @Test
    void testGenericModuleSettingsViewStandaloneAndOverlays() {
        IModule mace = ModuleRegistry.get("auto_mace");
        assertNotNull(mace);
        assertTrue(mace instanceof NivoratModule);

        assertDoesNotThrow(() -> {
            assertNotNull(ModuleSettingsView.class.getConstructor(IModule.class));
            assertNotNull(ModuleSettingsView.class.getConstructor(IModule.class, net.minecraft.client.gui.screen.Screen.class));
            assertNotNull(ModuleSettingsView.class.getConstructor(NivoratModule.class));
            assertNotNull(ModuleSettingsView.class.getConstructor(NivoratModule.class, net.minecraft.client.gui.screen.Screen.class));
        });

        activity.client.gui.overlay.OverlayManager overlayManager = new activity.client.gui.overlay.OverlayManager();
        activity.client.gui.modal.ModalManager modalManager = new activity.client.gui.modal.ModalManager(overlayManager);
        assertNotNull(overlayManager);
        assertNotNull(modalManager);
        assertFalse(overlayManager.hasActiveOverlay());

        AboutModuleSheet sheet = new AboutModuleSheet(null, mace.getId());
        overlayManager.open(sheet);
        assertTrue(overlayManager.hasActiveOverlay());
        overlayManager.clear();
        assertFalse(overlayManager.hasActiveOverlay());

        if (net.minecraft.client.MinecraftClient.getInstance() != null) {
            ModuleSettingsView view = new ModuleSettingsView((NivoratModule) mace);
            assertEquals(mace, view.getModule());
            assertNull(view.getParentScreen());
            assertNotNull(view.getMetadata());
            assertEquals("auto_mace", view.getMetadata().getId());
            assertEquals("https://t.me/virionDEV", view.getMetadata().getTelegram());
            assertNotNull(view.getOverlayManager());
            assertNotNull(view.getModalManager());

            assertFalse(view.getOverlayManager().hasActiveOverlay());
            view.openAboutModuleSheet(mace.getId());
            assertTrue(view.getOverlayManager().hasActiveOverlay());
            view.getOverlayManager().clear();
            assertFalse(view.getOverlayManager().hasActiveOverlay());
        }
    }

    @Test
    void testBuildCardAllTwelveModulesUnified() {
        ScrollContainer container = new ScrollContainer(0, 0, 400, 600);
        List<String> twelveIds = List.of(
            "auto_mace", "auto_spear", "auto_shieldbreaker", "auto_stun_slam",
            "auto_totem", "auto_cart", "auto_anchor", "cart_refill",
            "hp_reaper", "auto_tool", "auto_gg", "cart_hud"
        );
        for (String id : twelveIds) {
            IModule mod = ModuleRegistry.get(id);
            assertNotNull(mod, "Module " + id + " must be present in registry");
            int height = ModuleSettingsView.buildCard(null, null, container, mod, 0, 0, 380, 360);
            assertTrue(height > 40, "Height for " + id + " must be > 40px, was: " + height);
        }
    }

    @Test
    void testReactiveConditionalVisibilityInModuleSettingsView() {
        TestConditionalModule testMod = new TestConditionalModule();
        ScrollContainer container = new ScrollContainer(0, 0, 400, 600);

        AtomicInteger reloadCount = new AtomicInteger(0);
        AtomicBoolean aboutOpened = new AtomicBoolean(false);

        // Initially mode is FALSE -> dependent setting is not visible
        assertFalse(testMod.modeSetting.get());
        assertFalse(testMod.dependentSetting.isVisible());

        int hInitial = ModuleSettingsView.buildCard(
                null, null, container, testMod, 0, 0, 400, 380,
                id -> aboutOpened.set(true), null, null, reloadCount::incrementAndGet
        );
        assertTrue(hInitial > 0);
        assertEquals(0, reloadCount.get());

        // Toggle mode to TRUE: dependent setting becomes visible
        testMod.modeSetting.set(true);
        assertTrue(testMod.dependentSetting.isVisible());

        // Triggering setting change alerts reload callback because visibility changed
        testMod.notifyModeChanged();
        assertEquals(1, reloadCount.get(), "Changing mode must trigger reload when visibility changes");
    }

    @Test
    void testSidebarTreeRebuildAndRefresh() {
        SidebarTree tree = new SidebarTree();
        assertEquals(4, tree.getCategories().size());

        tree.refresh();
        assertEquals(4, tree.getCategories().size());

        tree.rebuildNodes();
        assertEquals(4, tree.getCategories().size());
        assertEquals(5, tree.getCategories().get(0).getChildren().size());
        assertEquals(4, tree.getCategories().get(1).getChildren().size());
        assertEquals(5, tree.getCategories().get(2).getChildren().size());
    }

    private static class TestConditionalModule extends NivoratModule {
        final BooleanSetting modeSetting;
        final BooleanSetting dependentSetting;
        private boolean enabledState = false;
        private boolean advancedOption = false;

        TestConditionalModule() {
            super("test_conditional", Text.literal("Test Module"), Text.literal("Test Desc"), ModuleCategory.COMBAT);

            this.modeSetting = registerBoolean(
                    "enable_advanced",
                    Text.literal("Advanced Mode"),
                    Text.literal("Toggles advanced options"),
                    SettingGroup.GENERAL,
                    false,
                    () -> this.enabledState,
                    val -> this.enabledState = val
            );

            this.dependentSetting = registerBoolean(
                    "advanced_option",
                    Text.literal("Advanced Option"),
                    Text.literal("Only shown when advanced mode is on"),
                    SettingGroup.GENERAL,
                    false,
                    () -> this.advancedOption,
                    val -> this.advancedOption = val
            );
            this.dependentSetting.visibleWhen(this.modeSetting);
        }

        void notifyModeChanged() {
            this.modeSetting.set(this.modeSetting.get());
        }
    }
}

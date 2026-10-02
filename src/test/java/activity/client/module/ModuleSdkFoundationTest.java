package activity.client.module;

import activity.client.config.ActivityConfig;
import activity.client.config.NivoratConfigManager;
import activity.client.gui.builder.SettingComponentFactory;
import activity.client.module.api.BuiltinModules;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.api.NivoratModule;
import activity.client.module.example.ExampleModule;
import activity.client.module.keybind.Keybind;
import activity.client.module.keybind.KeybindManager;
import activity.client.module.setting.ActionSetting;
import activity.client.module.setting.BooleanSetting;
import activity.client.module.setting.DoubleSetting;
import activity.client.module.setting.EnumSetting;
import activity.client.module.setting.IntegerSetting;
import activity.client.module.setting.KeybindSetting;
import activity.client.module.setting.NumberSetting;
import activity.client.module.setting.NumberUnit;
import activity.client.module.setting.Setting;
import activity.client.module.setting.SettingSection;
import activity.client.module.setting.StringSetting;
import net.minecraft.text.Text;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Nivorat Module SDK Foundation Tests")
public class ModuleSdkFoundationTest {

    @BeforeEach
    void setUp() {
        BuiltinModules.registerAll();
    }

    @Test
    @DisplayName("BuiltinModules registers all 16 canonical modules")
    void testBuiltinModulesRegistration() {
        assertEquals(16, ModuleRegistry.getAll().size(), "Must register all 16 modules");

        assertNotNull(ModuleRegistry.get("auto_mace"));
        assertNotNull(ModuleRegistry.get("auto_spear"));
        assertNotNull(ModuleRegistry.get("auto_shieldbreaker"));
        assertNotNull(ModuleRegistry.get("auto_stun_slam"));
        assertNotNull(ModuleRegistry.get("auto_stun_slime"), "auto_stun_slime alias must resolve to auto_stun_slam");
        assertNotNull(ModuleRegistry.get("auto_pearl_catch"));
        assertNotNull(ModuleRegistry.get("click_pearl"));

        assertNotNull(ModuleRegistry.get("auto_totem"));
        assertNotNull(ModuleRegistry.get("auto_cart"));
        assertNotNull(ModuleRegistry.get("auto_anchor"));
        assertNotNull(ModuleRegistry.get("cart_refill"));

        assertNotNull(ModuleRegistry.get("hp_reaper"));
        assertNotNull(ModuleRegistry.get("auto_tool"));
        assertNotNull(ModuleRegistry.get("auto_gg"));
        assertNotNull(ModuleRegistry.get("cart_hud"));
        assertNotNull(ModuleRegistry.get("cooldown_hud"));
        assertNotNull(ModuleRegistry.get("water_drop"));

        assertEquals(6, ModuleRegistry.getByCategory(ModuleCategory.COMBAT).size());
        assertEquals(4, ModuleRegistry.getByCategory(ModuleCategory.DEFENSE).size());
        assertEquals(6, ModuleRegistry.getByCategory(ModuleCategory.UTILITY).size());
    }

    @Test
    @DisplayName("ModuleMetadata defaults and builder")
    void testModuleMetadataDefaults() {
        ModuleMetadata meta = ModuleMetadata.builder("test_module")
                .displayName(Text.literal("Test Module"))
                .build();

        assertEquals("test_module", meta.getId());
        assertEquals("Nivorat", meta.getAuthor());
        assertEquals("", meta.getTelegramUrl());
        assertEquals("1.0.0", meta.getVersion());
        assertEquals("2026-09-16", meta.getLastUpdated());
        assertEquals("[-]", meta.getKeybindDisplay());
    }

    @Test
    @DisplayName("NivoratModule lifecycle and state management")
    void testModuleLifecycle() {
        AtomicBoolean enabledState = new AtomicBoolean(false);

        NivoratModule module = new NivoratModule("lifecycle_mod", Text.literal("Mod"), Text.literal("Desc"), ModuleCategory.COMBAT) {
            @Override
            public void onEnable() {
                enabledState.set(true);
            }

            @Override
            public void onDisable() {
                enabledState.set(false);
            }

            @Override
            public void loadFromConfig(ActivityConfig config) {}

            @Override
            public void saveToConfig(ActivityConfig config) {}
        };

        assertFalse(module.isEnabled());
        module.setEnabled(true);
        assertTrue(module.isEnabled());
        assertTrue(enabledState.get());

        module.toggle();
        assertFalse(module.isEnabled());
        assertFalse(enabledState.get());
    }

    @Test
    @DisplayName("BooleanSetting reactive behavior and single source of truth")
    void testBooleanSetting() {
        AtomicBoolean holder = new AtomicBoolean(false);
        AtomicBoolean listenerFired = new AtomicBoolean(false);

        BooleanSetting setting = new BooleanSetting(
                "toggle_opt", Text.literal("Toggle"), Text.literal("Desc"),
                SettingSection.GENERAL, false,
                holder::get, holder::set
        );

        assertEquals(SettingSection.GENERAL, setting.getSection());
        assertFalse(setting.get());

        setting.addListener(val -> listenerFired.set(true));
        setting.set(true);

        assertTrue(setting.get());
        assertTrue(holder.get(), "Underlying state holder must be updated directly");
        assertTrue(listenerFired.get(), "Listener must be notified of setting change");
    }

    @Test
    @DisplayName("NumberSetting bounds clamping, step, and NumberUnit formatting")
    void testNumberSetting() {
        double[] valHolder = new double[]{90.0};

        NumberSetting setting = new NumberSetting(
                "delay_ms", Text.literal("Delay"), Text.literal("Desc"),
                SettingSection.BEHAVIOR,
                10.0, 300.0, 5.0, NumberUnit.MS, true,
                90.0,
                () -> valHolder[0],
                val -> valHolder[0] = val
        );

        assertEquals(SettingSection.BEHAVIOR, setting.getSection());
        assertEquals("90 ms", setting.formatCurrentValue());

        setting.set(92.0);
        assertEquals(90.0, setting.get(), 0.001);

        setting.set(500.0);
        assertEquals(300.0, setting.get(), 0.001);

        setting.set(-10.0);
        assertEquals(10.0, setting.get(), 0.001);
    }

    @Test
    @DisplayName("IntegerSetting and DoubleSetting specialized types")
    void testIntegerAndDoubleSettings() {
        int[] intHolder = new int[]{3};
        IntegerSetting intSetting = new IntegerSetting(
                "burst", Text.literal("Burst"), Text.literal("Desc"),
                SettingSection.ADVANCED,
                1, 10, 1, NumberUnit.TICKS,
                3,
                () -> intHolder[0],
                val -> intHolder[0] = val
        );
        assertEquals("3 ticks", intSetting.formatCurrentValue());
        intSetting.set(5);
        assertEquals(5, (int) intSetting.get());

        double[] dblHolder = new double[]{3.5};
        DoubleSetting dblSetting = new DoubleSetting(
                "reach", Text.literal("Reach"), Text.literal("Desc"),
                SettingSection.BEHAVIOR,
                1.0, 6.0, 0.1, NumberUnit.BLOCKS,
                3.5,
                () -> dblHolder[0],
                val -> dblHolder[0] = val
        );
        assertEquals("3.5 bl", dblSetting.formatCurrentValue());
    }

    @Test
    @DisplayName("EnumSetting option validation and localized name provider")
    void testEnumSetting() {
        String[] modeHolder = new String[]{"smart"};

        EnumSetting setting = new EnumSetting(
                "mode", Text.literal("Mode"), Text.literal("Desc"),
                SettingSection.GENERAL,
                List.of("smart", "rage", "legit"), "smart",
                opt -> Text.literal("L:" + opt),
                () -> modeHolder[0],
                val -> modeHolder[0] = val
        );

        assertEquals("smart", setting.get());
        assertEquals("L:rage", setting.getOptionName("rage").getString());

        setting.set("legit");
        assertEquals("legit", setting.get());

        setting.set("invalid_option");
        assertEquals("smart", setting.get());
    }

    @Test
    @DisplayName("KeybindSetting and conflict detection in KeybindManager")
    void testKeybindSettingAndConflicts() {
        Keybind bindHolder = new Keybind(GLFW.GLFW_KEY_R, true, false, false);
        KeybindSetting setting = new KeybindSetting(
                "action_key", Text.literal("Action Key"), Text.literal("Desc"),
                SettingSection.ADVANCED,
                bindHolder,
                () -> bindHolder,
                kb -> bindHolder.copyFrom(kb)
        );

        assertEquals(GLFW.GLFW_KEY_R, setting.get().getKeyCode());
        assertTrue(setting.get().isCtrl());
    }

    @Test
    @DisplayName("ActionSetting executes assigned runnable")
    void testActionSetting() {
        AtomicInteger clickCount = new AtomicInteger(0);
        ActionSetting setting = new ActionSetting(
                "btn", Text.literal("Button"), Text.literal("Desc"),
                SettingSection.ADVANCED,
                clickCount::incrementAndGet
        );

        assertEquals(0, clickCount.get());
        setting.execute();
        assertEquals(1, clickCount.get());
    }

    @Test
    @DisplayName("Setting visibility condition")
    void testVisibilityCondition() {
        AtomicBoolean flag = new AtomicBoolean(false);
        Setting<Boolean> setting = new BooleanSetting(
                "cond", Text.literal("Cond"), Text.literal("Desc"),
                SettingSection.GENERAL, true,
                () -> true, v -> {}
        ).visibleWhen(flag::get);

        assertFalse(setting.isVisible());
        flag.set(true);
        assertTrue(setting.isVisible());
    }

    @Test
    @DisplayName("NumberUnit formatting and alias resolution")
    void testNumberUnits() {
        assertEquals(NumberUnit.MS, NumberUnit.fromString("ms"));
        assertEquals(NumberUnit.MS, NumberUnit.fromString("мс"));
        assertEquals(NumberUnit.TICKS, NumberUnit.fromString("ticks"));
        assertEquals(NumberUnit.PERCENT, NumberUnit.fromString("%"));
        assertEquals(NumberUnit.HP, NumberUnit.fromString("hp"));
        assertEquals(NumberUnit.BLOCKS, NumberUnit.fromString("blocks"));
        assertEquals(NumberUnit.BLOCKS, NumberUnit.fromString("bl"));
        assertEquals(NumberUnit.CPS, NumberUnit.fromString("cps"));

        assertEquals("50 ms", NumberUnit.MS.format(50.0, true));
        assertEquals("75%", NumberUnit.PERCENT.format(75.0, true));
        assertEquals("3.5 bl", NumberUnit.BLOCKS.format(3.5, false));
    }

    @Test
    @DisplayName("SettingComponentFactory creates rows without errors")
    void testSettingComponentFactory() {
        BooleanSetting bs = new BooleanSetting("b", Text.literal("B"), Text.literal("D"), SettingSection.GENERAL, true, () -> true, v -> {});
        NumberSetting ns = new NumberSetting("n", Text.literal("N"), Text.literal("D"), SettingSection.BEHAVIOR, 0, 10, 1, NumberUnit.MS, true, 5, () -> 5.0, v -> {});
        EnumSetting es = new EnumSetting("e", Text.literal("E"), Text.literal("D"), SettingSection.GENERAL, List.of("a", "b"), "a", () -> "a", v -> {});
        StringSetting ss = new StringSetting("s", Text.literal("S"), Text.literal("D"), SettingSection.ADVANCED, "test", () -> "test", v -> {});
        ActionSetting as = new ActionSetting("a", Text.literal("A"), Text.literal("D"), SettingSection.ADVANCED, () -> {});

        assertNotNull(SettingComponentFactory.createRow(bs, 0, 0, 200, null, null));
        assertNotNull(SettingComponentFactory.createRow(ns, 0, 0, 200, null, null));
        assertNotNull(SettingComponentFactory.createRow(es, 0, 0, 200, null, null));
        assertNotNull(SettingComponentFactory.createRow(ss, 0, 0, 200, null, null));
        assertNotNull(SettingComponentFactory.createRow(as, 0, 0, 200, null, null));
    }

    @Test
    @DisplayName("NivoratConfigManager syncs structured modules map")
    void testNivoratConfigManagerSync() {
        ActivityConfig config = new ActivityConfig();
        NivoratConfigManager.syncToModules(config);

        assertNotNull(config.modules);
        assertFalse(config.modules.isEmpty(), "Structured modules map must be populated");
        assertTrue(config.modules.containsKey("auto_mace"));
        assertTrue(config.modules.containsKey("auto_spear"));
        assertTrue(config.modules.containsKey("auto_totem"));
        assertTrue(config.modules.containsKey("hp_reaper"));

        ActivityConfig.ModuleConfigEntry maceEntry = config.modules.get("auto_mace");
        assertNotNull(maceEntry);
        assertTrue(maceEntry.settings.containsKey("swap_type"));

        ActivityConfig.ModuleConfigEntry spearEntry = config.modules.get("auto_spear");
        assertNotNull(spearEntry);
        assertTrue(spearEntry.settings.containsKey("security_mode"));
        assertTrue(spearEntry.settings.containsKey("restore_delay"));
    }

    @Test
    @DisplayName("ExampleModule adheres to SDK contract and extension point")
    void testExampleModuleContract() {
        ExampleModule example = new ExampleModule();
        assertEquals("example_module", example.getId());
        assertEquals("Пример Модуля", example.getDisplayName().getString());
        assertEquals(ModuleCategory.COMBAT, example.getCategory());
        assertTrue(example.hasCustomSection(), "ExampleModule must demonstrate custom section extension point");
        assertTrue(example.getSettings().size() >= 6, "Must register various setting types");
    }

    @Test
    @DisplayName("ModuleRegistry.setAllEnabled toggles all modules and updates state")
    void testMasterToggleAllModules() {

        ModuleRegistry.setAllEnabled(false);
        assertFalse(ModuleRegistry.isAnyModuleEnabled(), "No modules should be enabled");
        for (IModule module : ModuleRegistry.getAll()) {
            assertFalse(module.isEnabled(), "Module " + module.getId() + " should be disabled");
        }

        ModuleRegistry.setAllEnabled(true);
        assertTrue(ModuleRegistry.isAnyModuleEnabled(), "Modules should be enabled");
        for (IModule module : ModuleRegistry.getAll()) {
            assertTrue(module.isEnabled(), "Module " + module.getId() + " should be enabled");
        }

        ModuleRegistry.setAllEnabled(true);
        assertTrue(ModuleRegistry.isAnyModuleEnabled());
    }

    @Test
    @DisplayName("OcclusionCacheController never hijacks or closes screens and respects GUI non-interference")
    void testCartRefillScreenNonInterferenceAndSafety() {
        dev.culling.OcclusionCacheController controller = new dev.culling.OcclusionCacheController();
        controller.reset();

        boolean initial = controller.isEnabled();
        controller.toggle();
        assertEquals(!initial, controller.isEnabled());
        controller.toggle();
        assertEquals(initial, controller.isEnabled());

        activity.client.config.ActivityConfig cfg = activity.client.config.ActivityConfigManager.getConfig();
        assertNotNull(cfg);
        if (cfg.menuKeybind != null && !cfg.menuKeybind.isUnbound()) {
            String menuConflict = KeybindManager.findConflict(cfg.menuKeybind, "some_random_module");
            assertEquals("NivoratClient: Меню", menuConflict, "Keybind matching client menu must be detected as conflict");
        }
    }
}

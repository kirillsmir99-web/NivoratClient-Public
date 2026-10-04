package activity.client.module;

import activity.client.NivoratClient;
import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.config.preset.PresetManager;
import activity.client.gui.ModuleSettingsView;
import activity.client.gui.layout.ScrollContainer;
import activity.client.gui.search.SearchController;
import activity.client.gui.sheet.AboutModuleSheet;
import activity.client.gui.sidebar.SidebarTree;
import activity.client.gui.tab.CombatTab;
import activity.client.gui.tab.DefenseTab;
import activity.client.gui.tab.UtilityTab;
import activity.client.module.api.BuiltinModules;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleEventDispatcher;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.api.ModuleStatus;
import activity.client.module.keybind.Keybind;
import activity.client.module.keybind.KeybindManager;
import activity.client.module.service.CartStateService;
import activity.client.module.service.InventoryScanService;
import activity.client.module.service.PlayerStateService;
import activity.client.module.service.TargetCacheService;
import activity.client.module.setting.BooleanSetting;
import activity.client.module.setting.NumberSetting;
import activity.client.config.NivoratConfigManager;
import activity.client.module.setting.DoubleSetting;
import activity.client.module.setting.EnumSetting;
import activity.client.module.setting.IntegerSetting;
import activity.client.module.setting.KeybindSetting;
import activity.client.module.setting.Setting;
import activity.client.module.setting.SettingGroup;
import activity.client.module.setting.StringSetting;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.pack.api.CombatRaytraceGuard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.lwjgl.glfw.GLFW;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

public class ReleaseVerificationTest {

    private static final List<String> ALL_TWELVE_MODULE_IDS = List.of(
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
            "cooldown_hud",
            "water_drop",
            "click_pearl",
            "elytra_swap"
    );

    private static final Map<String, String> RUSSIAN_SEARCH_KEYWORDS = Map.ofEntries(
            Map.entry("auto_mace", "булава"),
            Map.entry("auto_spear", "копье"),
            Map.entry("auto_shieldbreaker", "щит"),
            Map.entry("auto_stun_slam", "стан"),
            Map.entry("auto_totem", "тотем"),
            Map.entry("auto_cart", "вагонетка"),
            Map.entry("auto_anchor", "якорь"),
            Map.entry("cart_refill", "рефилл"),
            Map.entry("hp_reaper", "хп"),
            Map.entry("auto_tool", "инструмент"),
            Map.entry("auto_gg", "гг"),
            Map.entry("cart_hud", "хад"),
            Map.entry("auto_pearl_catch", "перл"),
            Map.entry("cooldown_hud", "кд"),
            Map.entry("water_drop", "вода"),
            Map.entry("click_pearl", "клик"),
            Map.entry("elytra_swap", "элитра")
    );

    @BeforeAll
    static void initAll() {
        BuiltinModules.registerAll();
        SearchController.indexAllModulesFromRegistry();
        ModuleEventDispatcher.init();
    }

    @BeforeEach
    void setUp() {
        ActivityConfigManager.resetDefaults();
        ModuleEventDispatcher.updateActiveModules();
    }

    @AfterEach
    void tearDown() {
        ActivityConfigManager.resetDefaults();
        ModuleEventDispatcher.updateActiveModules();
    }

    @Test
    @DisplayName("Clean Install: fabric.mod.json has standard dependencies and zero legacy JAR references")
    void testCleanInstallFabricModJsonSpecification() {
        InputStream is = getClass().getResourceAsStream("/fabric.mod.json");
        assertNotNull(is, "fabric.mod.json must be present in classpath");

        JsonObject root = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals("activity", root.get("id").getAsString(), "Mod ID must be activity");
        String modName = root.get("name").getAsString();
        assertTrue("NivoratClient".equals(modName) || "CooldownHUD".equals(modName) || "PulseHUD".equals(modName) || "MemoryLeakFix".equals(modName), "Mod Name must be valid");

        JsonObject depends = root.getAsJsonObject("depends");
        assertNotNull(depends, "depends section must exist");

        assertTrue(depends.has("fabricloader"), "Must depend on fabricloader");
        assertTrue(depends.has("minecraft"), "Must depend on minecraft");
        assertTrue(depends.has("java"), "Must depend on java");
        assertTrue(depends.has("fabric-api"), "Must depend on fabric-api");
        assertEquals(4, depends.size(), "Clean install must depend ONLY on loader, mc, java, and fabric-api");

        assertTrue(root.has("provides"), "Must have provides section");
        boolean providesNivorat = false;
        for (var elem : root.getAsJsonArray("provides")) {
            if ("nivoratclient".equalsIgnoreCase(elem.getAsString())) {
                providesNivorat = true;
                break;
            }
        }
        assertTrue(providesNivorat, "Must provide 'nivoratclient' for compatibility");

        JsonObject entrypoints = root.getAsJsonObject("entrypoints");
        assertNotNull(entrypoints);
        String clientEntry = entrypoints.getAsJsonArray("client").get(0).getAsString();
        assertTrue("activity.client.CooldownHudClient".equals(clientEntry) || "activity.client.NivoratClient".equals(clientEntry), "Client entrypoint must be CooldownHudClient or NivoratClient");

        assertTrue(root.has("mixins"), "fabric.mod.json must declare mixins");
        var mixinArray = root.getAsJsonArray("mixins");
        assertEquals(5, mixinArray.size(), "Should declare the original four configs and custom renderer integration");
        List<String> declaredMixins = new java.util.ArrayList<>();
        for (var m : mixinArray) {
            declaredMixins.add(m.getAsString());
        }
        assertTrue(declaredMixins.contains("activity.pipeline.mixins.json") || declaredMixins.contains("activity.autotool.mixins.json"), "Must declare activity.pipeline.mixins.json");
        assertTrue(declaredMixins.contains("activity.audio.mixins.json") || declaredMixins.contains("activity.autogg.mixins.json"), "Must declare activity.audio.mixins.json");
        assertTrue(declaredMixins.contains("activity.dev.mixins.json"), "Must declare activity.dev.mixins.json");
        assertTrue(declaredMixins.contains("activity.cooldown.mixins.json"), "Must declare activity.cooldown.mixins.json");
        assertTrue(declaredMixins.contains("activity.custom-render.mixins.json"), "Must declare custom GPU renderer integration");
        for (String mixin : declaredMixins) {
            assertTrue(mixin.startsWith("activity."), "Mixin config " + mixin + " must be namespaced with 'activity.'");
            assertNotNull(getClass().getResourceAsStream("/" + mixin), "Mixin config " + mixin + " must exist on classpath");
        }
    }

    @Test
    @DisplayName("Clean Install: Exactly 16 built-in modules registered, zero external legacy stubs")
    void testCleanInstallBuiltinModuleCountAndReadiness() {
        assertEquals(17, ALL_TWELVE_MODULE_IDS.size(), "Must track exactly 16 modules");
        List<IModule> allModules = ModuleRegistry.getAll();
        assertTrue(allModules.size() >= 17, "ModuleRegistry must have at least 16 modules");

        for (String id : ALL_TWELVE_MODULE_IDS) {
            IModule mod = ModuleRegistry.get(id);
            assertNotNull(mod, "Module " + id + " must be registered in ModuleRegistry");
            assertFalse(mod.isStub(), "Module " + id + " must be a native unified module, not a stub");
            mod.setEnabled(true);
            assertEquals(ModuleStatus.READY, mod.getStatus(), "Module " + id + " must report READY status when enabled");
        }
    }

    @Test
    @DisplayName("Clean Install: Isolated environment initializes clean default config with all 16 modules")
    void testCleanInstallIsolatedConfigInitialization() {
        ActivityConfig freshConfig = new ActivityConfig();
        freshConfig.sanitize();
        freshConfig.syncModuleConfigEntries();
        NivoratConfigManager.syncFromModules(freshConfig);

        assertTrue(freshConfig.autoMaceEnabled, "AutoMace must be enabled by default");
        assertTrue(freshConfig.autoSpearEnabled, "AutoSpear must be enabled by default");
        assertTrue(freshConfig.autoShieldbreakerEnabled, "AutoShieldbreaker must be enabled by default");
        assertTrue(freshConfig.autoStunSlamEnabled, "AutoStunSlam must be enabled by default");
        assertTrue(freshConfig.autoPearlCatchEnabled, "AutoPearlCatch must be enabled by default");
        assertTrue(freshConfig.autoTotemEnabled, "AutoTotem must be enabled by default");
        assertTrue(freshConfig.autoCartEnabled, "AutoCart must be enabled by default");
        assertTrue(freshConfig.autoAnchorEnabled, "AutoAnchor must be enabled by default");
        assertTrue(freshConfig.cartRefillEnabled, "CartRefill must be enabled by default");
        assertTrue(freshConfig.hpReaperEnabled, "HPReaper must be enabled by default");
        assertTrue(freshConfig.autoToolEnabled, "AutoTool must be enabled by default");
        assertTrue(freshConfig.autoGGEnabled, "AutoGG must be enabled by default");
        assertTrue(freshConfig.cartHudEnabled, "CartHUD must be enabled by default");
        assertTrue(freshConfig.cooldownHudEnabled, "CooldownHUD must be enabled by default");

        assertNotNull(freshConfig.modules, "modules map must exist");
        for (String id : ALL_TWELVE_MODULE_IDS) {
            assertTrue(freshConfig.modules.containsKey(id), "modules map must contain clean entry for " + id);
            ActivityConfig.ModuleConfigEntry entry = freshConfig.modules.get(id);
            assertNotNull(entry, "Entry for " + id + " must not be null");
            assertTrue(entry.enabled, "Module " + id + " must be default enabled in clean config");
        }
    }

    @Test
    @DisplayName("Clean Install: only pinned ClientSpoofer is nested and all module classes are present")
    void testCleanInstallMonolithicJarStructure() throws Exception {
        Path jarPath = Path.of("build", "libs", "NivoratClient.jar");
        if (!Files.exists(jarPath)) {
            jarPath = Path.of("build", "libs", "CooldownHUD.jar");
        }
        if (!Files.exists(jarPath)) {
            jarPath = Path.of("build", "libs", "NivoratClient.jar");
        }
        Assumptions.assumeTrue(Files.exists(jarPath), "NivoratClient.jar, CooldownHUD.jar or NivoratClient.jar must exist in build/libs/ to verify structure");

        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            long nestedJars = zip.stream().filter(e -> e.getName().endsWith(".jar")).count();
            assertEquals(1, nestedJars, "Only the pinned ClientSpoofer dependency may be nested");
            var dependency = zip.getEntry("META-INF/jars/ClientSpoofer-1.21.11-1.4.0.jar");
            assertNotNull(dependency);
            byte[] bytes;
            try (var stream = zip.getInputStream(dependency)) { bytes = stream.readAllBytes(); }
            String hash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
            assertEquals("c4dabf0cc8bca6f8b582f0ee7c4aff43c535f4d282458cbf48baa32131159ea9", hash);
            try (var stream = zip.getInputStream(zip.getEntry("fabric.mod.json"))) {
                var descriptor = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                assertEquals(dependency.getName(), descriptor.getAsJsonArray("jars").get(0).getAsJsonObject().get("file").getAsString());
            }

            assertNotNull(zip.getEntry("fabric.mod.json"), "JAR must contain fabric.mod.json");
            assertTrue(zip.getEntry("activity/client/CooldownHudClient.class") != null || zip.getEntry("activity/client/NivoratClient.class") != null, "JAR must contain CooldownHudClient.class or NivoratClient.class");

            assertTrue(zip.getEntry("activity.pipeline.mixins.json") != null || zip.getEntry("activity.autotool.mixins.json") != null, "JAR must contain activity.pipeline.mixins.json or activity.autotool.mixins.json");
            assertTrue(zip.getEntry("activity.audio.mixins.json") != null || zip.getEntry("activity.autogg.mixins.json") != null, "JAR must contain activity.audio.mixins.json or activity.autogg.mixins.json");
            assertNull(zip.getEntry("autotool.mixins.json"), "JAR must NOT contain un-namespaced autotool.mixins.json");
            assertNull(zip.getEntry("autogg.mixins.json"), "JAR must NOT contain un-namespaced autogg.mixins.json");

            assertTrue(zip.getEntry("activity/client/mixin/audio/AudioNetworkHandlerMixin.class") != null || zip.getEntry("activity/client/mixin/autogg/ActivityClientPlayNetworkHandlerMixin.class") != null, "JAR must contain AudioNetworkHandlerMixin.class");
            assertTrue(zip.getEntry("activity/client/mixin/pipeline/PipelineInteractionManagerMixin.class") != null || zip.getEntry("activity/client/mixin/autotool/ActivityClientPlayerInteractionManagerMixin.class") != null, "JAR must contain PipelineInteractionManagerMixin.class");
            assertTrue(zip.getEntry("activity/client/mixin/pipeline/PipelineInteractionManagerAccessor.class") != null || zip.getEntry("activity/client/mixin/autotool/ActivityClientPlayerInteractionManagerAccessor.class") != null, "JAR must contain PipelineInteractionManagerAccessor.class");

            assertNull(zip.getEntry("ru/elarion/autogg/mixin/ClientPlayNetworkHandlerMixin.class"), "JAR must NOT contain legacy ClientPlayNetworkHandlerMixin.class");
            assertNull(zip.getEntry("ru/elarion/autotool/mixin/ClientPlayerInteractionManagerMixin.class"), "JAR must NOT contain legacy ClientPlayerInteractionManagerMixin.class");
            assertNull(zip.getEntry("ru/elarion/autotool/mixin/ClientPlayerInteractionManagerAccessor.class"), "JAR must NOT contain legacy ClientPlayerInteractionManagerAccessor.class");

            for (String id : ALL_TWELVE_MODULE_IDS) {
                IModule mod = ModuleRegistry.get(id);
                assertNotNull(mod, "Module " + id + " must be registered");
                String classPath = mod.getClass().getName().replace('.', '/') + ".class";
                assertNotNull(zip.getEntry(classPath), "JAR must contain compiled module class: " + classPath);
            }
        }
    }

    @ParameterizedTest(name = "UI exists: {0}")
    @ValueSource(strings = {
            "auto_mace", "auto_spear", "auto_shieldbreaker", "auto_stun_slam",
            "auto_totem", "auto_cart", "auto_anchor", "cart_refill",
            "hp_reaper", "auto_tool", "auto_gg", "cart_hud", "auto_pearl_catch",
            "cooldown_hud", "water_drop", "click_pearl", "elytra_swap"
    })
    void testModuleUIExists(String moduleId) {
        IModule mod = ModuleRegistry.get(moduleId);
        assertNotNull(mod, "Module " + moduleId + " must exist");
        assertNotNull(mod.getName(), "Module " + moduleId + " must have a non-null name");
        assertFalse(mod.getName().getString().isBlank(), "Module " + moduleId + " must have a non-empty name");
        assertNotNull(mod.getDescription(), "Module " + moduleId + " must have a non-null description");
        assertNotNull(mod.getCategory(), "Module " + moduleId + " must have a category");

        ScrollContainer container = new ScrollContainer(0, 0, 400, 600);
        int cardHeight = ModuleSettingsView.buildCard(null, null, container, mod, 0, 0, 380, 360);
        assertTrue(cardHeight > 40, "Card height for " + moduleId + " must be > 40px, got: " + cardHeight);
        assertFalse(container.getChildren().isEmpty(), "Card for " + moduleId + " must populate interactive components into ScrollContainer");

        if (net.minecraft.client.MinecraftClient.getInstance() != null) {
            ModuleSettingsView standalone = new ModuleSettingsView(mod);
            assertEquals(mod, standalone.getModule(), "Standalone view must retain reference to module " + moduleId);
            assertNotNull(standalone.getTitle(), "Standalone view must have title");
            assertEquals(mod.getName().getString(), standalone.getTitle().getString());
        } else {
            assertDoesNotThrow(() -> {
                assertNotNull(ModuleSettingsView.class.getConstructor(IModule.class));
                assertNotNull(ModuleSettingsView.class.getConstructor(IModule.class, net.minecraft.client.gui.screen.Screen.class));
            });
        }

        switch (mod.getCategory()) {
            case COMBAT -> {
                CombatTab tab = new CombatTab();
                tab.buildTab(null, container, 0, 0, 700);
                assertNotNull(tab.getModuleCard(moduleId), "CombatTab must contain card for " + moduleId);
            }
            case DEFENSE -> {
                DefenseTab tab = new DefenseTab();
                tab.buildTab(null, container, 0, 0, 700);
                assertNotNull(tab.getModuleCard(moduleId), "DefenseTab must contain card for " + moduleId);
            }
            case UTILITY, UTILITY_HUD -> {
                UtilityTab tab = new UtilityTab();
                tab.buildTab(null, container, 0, 0, 700);
                assertNotNull(tab.getModuleCard(moduleId), "UtilityTab must contain card for " + moduleId);
            }
            default -> fail("Unexpected category for module " + moduleId + ": " + mod.getCategory());
        }
    }

    @ParameterizedTest(name = "Enable and Disable works: {0}")
    @ValueSource(strings = {
            "auto_mace", "auto_spear", "auto_shieldbreaker", "auto_stun_slam",
            "auto_totem", "auto_cart", "auto_anchor", "cart_refill",
            "hp_reaper", "auto_tool", "auto_gg", "cart_hud", "auto_pearl_catch",
            "cooldown_hud", "water_drop", "click_pearl"
    })
    void testModuleEnableDisableWorks(String moduleId) {
        IModule mod = ModuleRegistry.get(moduleId);
        assertNotNull(mod);
        ActivityConfig config = ActivityConfigManager.getConfig();

        mod.setEnabled(true);
        assertTrue(mod.isEnabled(), "Module " + moduleId + " must be enabled");
        assertEquals(ModuleStatus.READY, mod.getStatus());
        mod.saveToConfig(config);

        ModuleEventDispatcher.updateActiveModules();
        if (mod.hasTickLogic()) {
            boolean inTick = Arrays.asList(ModuleEventDispatcher.getActiveTickModules()).contains(mod);
            assertTrue(inTick, "Enabled module " + moduleId + " with tick logic must be in activeTickModules");
        }

        mod.setEnabled(false);
        assertFalse(mod.isEnabled(), "Module " + moduleId + " must be disabled");
        assertEquals(ModuleStatus.DISABLED, mod.getStatus());
        mod.saveToConfig(config);

        ModuleEventDispatcher.updateActiveModules();
        boolean inTickDisabled = Arrays.asList(ModuleEventDispatcher.getActiveTickModules()).contains(mod);
        assertFalse(inTickDisabled, "Disabled module " + moduleId + " must not be in activeTickModules");
        boolean inAttackDisabled = Arrays.asList(ModuleEventDispatcher.getActiveAttackModules()).contains(mod);
        assertFalse(inAttackDisabled, "Disabled module " + moduleId + " must not be in activeAttackModules");
        boolean inHudDisabled = Arrays.asList(ModuleEventDispatcher.getActiveHudModules()).contains(mod);
        assertFalse(inHudDisabled, "Disabled module " + moduleId + " must not be in activeHudModules");
    }

    @ParameterizedTest(name = "Keybind works: {0}")
    @ValueSource(strings = {
            "auto_mace", "auto_spear", "auto_shieldbreaker", "auto_stun_slam",
            "auto_totem", "auto_cart", "auto_anchor", "cart_refill",
            "hp_reaper", "auto_tool", "auto_gg", "cart_hud", "auto_pearl_catch",
            "cooldown_hud", "water_drop", "click_pearl"
    })
    void testModuleKeybindWorks(String moduleId) {
        IModule mod = ModuleRegistry.get(moduleId);
        assertNotNull(mod);
        Keybind kb = mod.getKeybind();
        assertNotNull(kb, "Module " + moduleId + " must have a non-null Keybind instance");

        kb.set(GLFW.GLFW_KEY_P, true, false, false);
        assertEquals(GLFW.GLFW_KEY_P, kb.getKeyCode());
        assertTrue(kb.isCtrl());
        assertFalse(kb.isShift());
        assertFalse(kb.isAlt());

        assertTrue(kb.matchesKey(GLFW.GLFW_KEY_P, GLFW.GLFW_MOD_CONTROL), "Keybind must match Key+Ctrl");
        assertFalse(kb.matchesKey(GLFW.GLFW_KEY_P, 0), "Keybind must not match without Ctrl modifier");

        KeybindManager.rebuildBoundKeybinds();
        boolean inPrimaries = Arrays.stream(KeybindManager.getBoundPrimaries())
                .anyMatch(bp -> bp.module().getId().equalsIgnoreCase(moduleId));
        assertTrue(inPrimaries, "KeybindManager must include bound module " + moduleId + " in primaries");

        String conflict = KeybindManager.findConflict(kb, "unrelated_other_module");
        assertNotNull(conflict, "KeybindManager must flag conflict for duplicate keybind");
        String selfConflict = KeybindManager.findConflict(kb, moduleId);
        assertNull(selfConflict, "KeybindManager must not flag conflict against self");

        ActivityConfig config = new ActivityConfig();
        mod.saveToConfig(config);

        kb.clear();
        assertTrue(kb.isUnbound());
        KeybindManager.rebuildBoundKeybinds();
        boolean removedFromPrimaries = Arrays.stream(KeybindManager.getBoundPrimaries())
                .noneMatch(bp -> bp.module().getId().equalsIgnoreCase(moduleId));
        assertTrue(removedFromPrimaries, "KeybindManager must remove unbound module from primaries");

        mod.loadFromConfig(config);
        assertEquals(GLFW.GLFW_KEY_P, kb.getKeyCode(), "Keybind for " + moduleId + " must be restored from config");
        assertTrue(kb.isCtrl());

        kb.clear();
        KeybindManager.rebuildBoundKeybinds();
    }

    @ParameterizedTest(name = "Settings work: {0}")
    @ValueSource(strings = {
            "auto_mace", "auto_spear", "auto_shieldbreaker", "auto_stun_slam",
            "auto_totem", "auto_cart", "auto_anchor", "cart_refill",
            "hp_reaper", "auto_tool", "auto_gg", "cart_hud", "auto_pearl_catch",
            "cooldown_hud", "water_drop", "click_pearl"
    })
    void testModuleSettingsWork(String moduleId) {
        IModule mod = ModuleRegistry.get(moduleId);
        assertNotNull(mod);
        List<Setting<?>> settings = mod.getSettings();
        assertNotNull(settings, "Settings for " + moduleId + " must not be null");
        assertFalse(settings.isEmpty(), "Module " + moduleId + " must have at least one registered setting");

        int lastOrdinal = -1;
        for (Setting<?> s : settings) {
            assertNotNull(s.getId(), "Setting ID cannot be null in " + moduleId);
            assertNotNull(s.getDisplayName(), "Setting displayName cannot be null in " + moduleId);
            assertNotNull(s.getDescription(), "Setting description cannot be null in " + moduleId);
            assertNotNull(s.getDefaultValue(), "Default value cannot be null for " + s.getId() + " in " + moduleId);
            SettingGroup group = s.getGroup();
            assertNotNull(group, "Setting " + s.getId() + " must have a group");
            assertTrue(group.ordinal() >= lastOrdinal,
                    "Setting " + s.getId() + " in " + moduleId + " violates 5-tier group order");
            lastOrdinal = group.ordinal();

            if (s instanceof BooleanSetting bs) {
                boolean original = bs.get();
                bs.set(!original);
                assertEquals(!original, bs.get(), "Boolean setting " + bs.getId() + " must toggle");
                bs.set(original);
            } else if (s instanceof NumberSetting ns) {
                double original = ns.get();
                double testVal = Math.min(ns.getMax(), ns.getMin() + ns.getStep());
                ns.set(testVal);
                assertEquals(testVal, ns.get(), 0.001, "Number setting " + ns.getId() + " must update");
                ns.set(ns.getMax() + 1000.0);
                assertEquals(ns.getMax(), ns.get(), 0.001, "Number setting " + ns.getId() + " must clamp to max");
                ns.set(ns.getMin() - 1000.0);
                assertEquals(ns.getMin(), ns.get(), 0.001, "Number setting " + ns.getId() + " must clamp to min");
                ns.set(original);
            } else if (s instanceof StringSetting ss) {
                String original = ss.get();
                ss.set("TestCustomString");
                assertEquals("TestCustomString", ss.get(), "String setting " + ss.getId() + " must update");
                ss.set(original);
            } else if (s instanceof EnumSetting es) {
                List<String> options = es.getOptions();
                assertNotNull(options, "Enum setting " + es.getId() + " must provide enum options");
                assertFalse(options.isEmpty(), "Enum options must not be empty");
                String original = es.get();
                assertNotNull(original);
                es.set(options.get(0));
                es.set(original);
            } else if (s instanceof KeybindSetting ks) {
                Keybind original = ks.get();
                Keybind testKey = new Keybind(GLFW.GLFW_KEY_H, true, false, false);
                ks.set(testKey);
                assertEquals(GLFW.GLFW_KEY_H, ks.get().getKeyCode());
                ks.set(original);
            }
        }
    }

    @ParameterizedTest(name = "Config persists: {0}")
    @ValueSource(strings = {
            "auto_mace", "auto_spear", "auto_shieldbreaker", "auto_stun_slam",
            "auto_totem", "auto_cart", "auto_anchor", "cart_refill",
            "hp_reaper", "auto_tool", "auto_gg", "cart_hud", "auto_pearl_catch",
            "cooldown_hud", "water_drop", "click_pearl"
    })
    void testModuleConfigPersists(String moduleId) {
        IModule mod = ModuleRegistry.get(moduleId);
        assertNotNull(mod);

        ActivityConfig config1 = new ActivityConfig();
        mod.setEnabled(true);
        mod.getKeybind().set(GLFW.GLFW_KEY_K, true, false, false);

        Setting<?> testSetting = mod.getSettings().isEmpty() ? null : mod.getSettings().get(0);
        Object originalSettingVal = null;
        if (testSetting instanceof BooleanSetting bs) {
            originalSettingVal = bs.get();
            bs.set(!(Boolean) originalSettingVal);
        } else if (testSetting instanceof NumberSetting ns) {
            originalSettingVal = ns.get();
            ns.set(Math.min(ns.getMax(), ns.getMin() + ns.getStep()));
        }

        mod.saveToConfig(config1);

        Gson gson = new Gson();
        String json = gson.toJson(config1);
        ActivityConfig config2 = gson.fromJson(json, ActivityConfig.class);
        assertNotNull(config2, "Deserialized config must not be null");

        mod.setEnabled(false);
        mod.getKeybind().clear();
        if (testSetting != null && originalSettingVal != null) {
            testSetting.reset();
        }
        mod.loadFromConfig(config2);

        assertTrue(mod.isEnabled(), "Enabled state for " + moduleId + " must persist across JSON serialization");
        assertEquals(GLFW.GLFW_KEY_K, mod.getKeybind().getKeyCode(), "Keybind for " + moduleId + " must persist");
        assertTrue(mod.getKeybind().isCtrl());

        if (testSetting instanceof BooleanSetting bs && originalSettingVal != null) {
            assertEquals(!(Boolean) originalSettingVal, bs.get(), "Mutated boolean setting must persist");
            bs.set((Boolean) originalSettingVal);
        } else if (testSetting instanceof NumberSetting ns && originalSettingVal != null) {
            assertEquals(Math.min(ns.getMax(), ns.getMin() + ns.getStep()), ns.get(), 0.001, "Mutated number setting must persist");
            ns.set((Double) originalSettingVal);
        }
    }

    @ParameterizedTest(name = "Preset restores: {0}")
    @ValueSource(strings = {
            "auto_mace", "auto_spear", "auto_shieldbreaker", "auto_stun_slam",
            "auto_totem", "auto_cart", "auto_anchor", "cart_refill",
            "hp_reaper", "auto_tool", "auto_gg", "cart_hud", "auto_pearl_catch",
            "cooldown_hud", "water_drop", "click_pearl"
    })
    void testModulePresetRestores(String moduleId) {
        IModule mod = ModuleRegistry.get(moduleId);
        assertNotNull(mod);

        ActivityConfig config = ActivityConfigManager.getConfig();
        assertNotNull(config);

        mod.setEnabled(false);
        mod.getKeybind().set(GLFW.GLFW_KEY_L, false, true, false);
        Setting<?> testSetting = mod.getSettings().isEmpty() ? null : mod.getSettings().get(0);
        if (testSetting instanceof BooleanSetting bs) {
            bs.set(!bs.getDefaultValue());
        } else if (testSetting instanceof NumberSetting ns) {
            ns.set(ns.getMax());
        }
        mod.saveToConfig(config);

        PresetManager.applyPreset(PresetManager.getDefaultPreset(), config);
        mod.loadFromConfig(config);

        assertTrue(mod.isEnabled(), "Default preset must restore enabled=true for " + moduleId);
        assertTrue(mod.getKeybind().getKeyCode() != GLFW.GLFW_KEY_L || mod.getKeybind().isUnbound(),
                "Custom test keybind must be reset by default preset");
        if (testSetting instanceof BooleanSetting bs) {
            assertEquals(bs.getDefaultValue(), bs.get(), "Preset must restore default setting value for " + bs.getId());
        } else if (testSetting instanceof NumberSetting ns) {
            assertEquals(ns.getDefaultValue(), ns.get(), 0.001, "Preset must restore default setting value for " + ns.getId());
        }
    }

    @ParameterizedTest(name = "Search finds it: {0}")
    @ValueSource(strings = {
            "auto_mace", "auto_spear", "auto_shieldbreaker", "auto_stun_slam",
            "auto_totem", "auto_cart", "auto_anchor", "cart_refill",
            "hp_reaper", "auto_tool", "auto_gg", "cart_hud", "auto_pearl_catch",
            "cooldown_hud", "water_drop", "click_pearl"
    })
    void testModuleSearchFindsIt(String moduleId) {
        IModule mod = ModuleRegistry.get(moduleId);
        assertNotNull(mod);

        List<SearchController.SearchResult> idResults = SearchController.search(moduleId, 10);
        assertFalse(idResults.isEmpty(), "Search query for " + moduleId + " must return results");
        boolean foundModId = idResults.stream().anyMatch(r -> moduleId.equalsIgnoreCase(r.entry().moduleId()));
        assertTrue(foundModId, "Search results must contain module " + moduleId);

        String ruKeyword = RUSSIAN_SEARCH_KEYWORDS.get(moduleId);
        if (ruKeyword != null) {
            List<SearchController.SearchResult> ruResults = SearchController.search(ruKeyword, 10);
            assertFalse(ruResults.isEmpty(), "Russian search for '" + ruKeyword + "' must return results");
            boolean foundRu = ruResults.stream().anyMatch(r -> moduleId.equalsIgnoreCase(r.entry().moduleId()));
            assertTrue(foundRu, "Russian search for '" + ruKeyword + "' must match module " + moduleId);
        }

        String enName = mod.getName().getString();
        if (enName != null && !enName.isBlank()) {
            List<SearchController.SearchResult> enResults = SearchController.search(enName, 10);
            assertFalse(enResults.isEmpty(), "English search for '" + enName + "' must return results");
            boolean foundEn = enResults.stream().anyMatch(r -> moduleId.equalsIgnoreCase(r.entry().moduleId()));
            assertTrue(foundEn, "English search for '" + enName + "' must match module " + moduleId);
        }
    }

    @ParameterizedTest(name = "Pin works: {0}")
    @ValueSource(strings = {
            "auto_mace", "auto_spear", "auto_shieldbreaker", "auto_stun_slam",
            "auto_totem", "auto_cart", "auto_anchor", "cart_refill",
            "hp_reaper", "auto_tool", "auto_gg", "cart_hud", "auto_pearl_catch",
            "cooldown_hud", "water_drop", "click_pearl"
    })
    void testModulePinWorks(String moduleId) {
        ActivityConfig config = ActivityConfigManager.getConfig();
        assertNotNull(config);

        config.setPinned(moduleId, false);
        assertFalse(config.isPinned(moduleId), "Module " + moduleId + " should not be pinned initially");

        config.setPinned(moduleId, true);
        assertTrue(config.isPinned(moduleId), "Module " + moduleId + " must be pinned after setPinned(true)");
        assertTrue(config.getPinnedModules().contains(moduleId), "Pinned list must contain " + moduleId);

        config.setPinned(moduleId, false);
        assertFalse(config.isPinned(moduleId), "Module " + moduleId + " must not be pinned after setPinned(false)");
    }

    @ParameterizedTest(name = "Quick Access works: {0}")
    @ValueSource(strings = {
            "auto_mace", "auto_spear", "auto_shieldbreaker", "auto_stun_slam",
            "auto_totem", "auto_cart", "auto_anchor", "cart_refill",
            "hp_reaper", "auto_tool", "auto_gg", "cart_hud", "auto_pearl_catch",
            "cooldown_hud", "water_drop", "click_pearl"
    })
    void testModuleQuickAccessWorks(String moduleId) {
        ActivityConfig config = ActivityConfigManager.getConfig();
        assertNotNull(config);
        SidebarTree tree = new SidebarTree();

        int heightUnpinned = tree.getQuickAccessHeight(config.getPinnedModules());

        config.setPinned(moduleId, true);
        int heightPinned = tree.getQuickAccessHeight(config.getPinnedModules());
        assertTrue(heightPinned > heightUnpinned, "Quick access height must increase when module " + moduleId + " is pinned");

        config.setPinned(moduleId, false);
    }

    @ParameterizedTest(name = "About Module works: {0}")
    @ValueSource(strings = {
            "auto_mace", "auto_spear", "auto_shieldbreaker", "auto_stun_slam",
            "auto_totem", "auto_cart", "auto_anchor", "cart_refill",
            "hp_reaper", "auto_tool", "auto_gg", "cart_hud", "auto_pearl_catch",
            "cooldown_hud", "water_drop", "click_pearl"
    })
    void testModuleAboutSheetWorks(String moduleId) {
        AboutModuleSheet sheet = new AboutModuleSheet(null, moduleId);
        assertEquals(moduleId, sheet.getModuleId());
        assertNotNull(sheet.getMetadata(), "Metadata for " + moduleId + " must not be null");

        ModuleMetadata meta = sheet.getMetadata();
        assertEquals(moduleId, meta.getId());
        assertEquals("Nivorat", meta.getAuthor());
        assertNotNull(meta.getVersion());
        assertTrue(meta.getVersion().matches("\\d+\\.\\d+\\.\\d+"), "Version must be semver: " + meta.getVersion());
        assertEquals("", meta.getTelegramUrl());
        assertNotNull(meta.getIcon(), "Icon for " + moduleId + " must not be null");

        assertFalse(sheet.isClosed());
        sheet.close();
        assertTrue(sheet.isClosed());
    }

    @Test
    @DisplayName("Global Test: Simulated Minecraft restart restores complete 12-module configuration")
    void testGlobalPersistenceRestartSimulation() {
        ActivityConfig original = ActivityConfigManager.getConfig();

        original.autoMaceEnabled = false;
        original.autoSpearEnabled = true;
        original.autoSpearRestoreDelayMs = 210.0;
        original.autoShieldbreakerEnabled = false;
        original.autoStunSlamDistance = 3.2;
        original.autoTotemTriggerHearts = 5.0;
        original.autoCartAllowSelfCart = true;
        original.autoAnchorAutoExplode = true;
        original.cartRefillChance = 80.0;
        original.hpReaperMode = "damage_diff";
        original.autoToolCombatGuard = false;
        original.autoGGPhrase = "GG Well Played!";
        original.setPinned("auto_mace", true);
        original.setPinned("cart_hud", true);

        String exportedJson = ActivityConfigManager.exportPresetString();
        assertNotNull(exportedJson);
        assertFalse(exportedJson.isBlank());

        ActivityConfigManager.resetDefaults();
        ActivityConfig wiped = ActivityConfigManager.getConfig();
        assertTrue(wiped.autoMaceEnabled, "Wiped config should have default autoMaceEnabled=true");
        assertFalse(wiped.isPinned("auto_mace"), "Wiped config should have no pins");

        boolean imported = ActivityConfigManager.importPresetString(exportedJson);
        assertTrue(imported, "Configuration must import successfully upon restart simulation");

        ActivityConfig restored = ActivityConfigManager.getConfig();
        assertFalse(restored.autoMaceEnabled, "autoMaceEnabled must be persisted as false");
        assertEquals(210.0, restored.autoSpearRestoreDelayMs, 0.001);
        assertFalse(restored.autoShieldbreakerEnabled);
        assertEquals(3.2, restored.autoStunSlamDistance, 0.001);
        assertEquals(5.0, restored.autoTotemTriggerHearts, 0.001);
        assertTrue(restored.autoCartAllowSelfCart);
        assertTrue(restored.autoAnchorAutoExplode);
        assertEquals(80.0, restored.cartRefillChance, 0.001);
        assertEquals("damage_diff", restored.hpReaperMode);
        assertFalse(restored.autoToolCombatGuard);
        assertEquals("GG Well Played!", restored.autoGGPhrase);
        assertTrue(restored.isPinned("auto_mace"));
        assertTrue(restored.isPinned("cart_hud"));

        assertFalse(ModuleRegistry.get("auto_mace").isEnabled());
        assertFalse(ModuleRegistry.get("auto_shieldbreaker").isEnabled());
    }

    @Test
    @DisplayName("Global Test: World/server switch clears all shared caches without leaks")
    void testGlobalLifecycleWorldSwitchCacheInvalidation() {

        PlayerStateService.setBusyForTest(true);
        PlayerStateService.setAirTicksForTest(25);
        PlayerStateService.setHealthForTest(12.0f, 4.0f);
        InventoryScanService.setCachedSwordSlotForTest(3);
        InventoryScanService.setCachedTotemSlotForTest(7);
        CombatRaytraceGuard.markDispatcherManaged();
        TargetCacheService.reset();

        assertTrue(PlayerStateService.isBusy());
        assertEquals(25, PlayerStateService.getAirTicks());
        assertEquals(3, InventoryScanService.getCachedSwordSlot());

        ModuleEventDispatcher.onClientTick(null);

        assertFalse(PlayerStateService.isBusy(), "PlayerStateService busy state must be reset on world switch");
        assertEquals(0, PlayerStateService.getAirTicks(), "PlayerStateService air ticks must be reset on world switch");
        assertEquals(20.0f, PlayerStateService.getHealth(), 0.001, "PlayerStateService health must be reset to default 20.0");
        assertEquals(0.0f, PlayerStateService.getAbsorption(), 0.001, "PlayerStateService absorption must be reset to 0.0");

        assertEquals(-2, InventoryScanService.getCachedSwordSlot(), "InventoryScanService sword slot must be invalidated (-2)");
        assertEquals(-2, InventoryScanService.getCachedTotemSlot(), "InventoryScanService totem slot must be invalidated (-2)");

        assertEquals(0, TargetCacheService.getShieldCacheSize(), "TargetCacheService must be empty on world switch");
        assertEquals(0, CombatRaytraceGuard.getEntityCacheSize(), "CombatRaytraceGuard entity cache must be clear");
        assertEquals(0, CombatRaytraceGuard.getBlockCacheSize(), "CombatRaytraceGuard block cache must be clear");
        assertEquals(0, CartStateService.countCarts(null), "CartStateService cart count for null player must be 0");
    }

    @Test
    @DisplayName("Global Test: ModuleEventDispatcher.init is strictly idempotent (zero duplicate hooks)")
    void testGlobalEventRegistrationIdempotency() {

        for (int i = 0; i < 10; i++) {
            assertDoesNotThrow(ModuleEventDispatcher::init, "ModuleEventDispatcher.init() must be safe to call repeatedly");
            assertDoesNotThrow(ModuleRegistry::initEvents, "ModuleRegistry.initEvents() must be safe to call repeatedly");
        }

        ModuleEventDispatcher.updateActiveModules();
        IModule[] activeTicks = ModuleEventDispatcher.getActiveTickModules();
        assertNotNull(activeTicks);

        int len1 = activeTicks.length;
        ModuleEventDispatcher.updateActiveModules();
        int len2 = ModuleEventDispatcher.getActiveTickModules().length;
        assertEquals(len1, len2, "Active module arrays must not duplicate entries across updates");

        assertDoesNotThrow(() -> ModuleEventDispatcher.onClientTick(null));
    }
}

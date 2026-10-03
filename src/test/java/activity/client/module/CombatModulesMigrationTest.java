package activity.client.module;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.module.api.BuiltinModules;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.api.NivoratModule;
import activity.client.module.impl.combat.ParticlePhysicsModule;
import activity.client.module.impl.combat.ShaderPassModule;
import activity.client.module.impl.combat.VectorStreamModule;
import activity.client.module.impl.combat.MatrixTransformModule;
import activity.client.module.setting.BooleanSetting;
import activity.client.module.setting.DoubleSetting;
import activity.client.module.setting.EnumSetting;
import activity.client.module.setting.IntegerSetting;
import activity.client.module.setting.KeybindSetting;
import activity.client.module.setting.NumberSetting;
import activity.client.module.setting.Setting;
import activity.client.module.setting.SettingGroup;
import dev.vector.VectorStreamConfig;
import dev.shader.ShaderPassConfig;
import dev.particle.ParticlePhysicsConfig;
import net.redstone.optimizer.config.RedstoneOptimizerConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CombatModulesMigrationTest {

    @BeforeAll
    static void initRegistry() {
        BuiltinModules.registerAll();
    }

    @BeforeEach
    void resetState() {
        ActivityConfigManager.resetDefaults();
    }

    @Test
    void testCombatModulesRegisteredAsNivoratModule() {
        List<String> combatIds = List.of(ParticlePhysicsModule.ID, VectorStreamModule.ID, ShaderPassModule.ID, MatrixTransformModule.ID);
        for (String id : combatIds) {
            IModule module = ModuleRegistry.get(id);
            assertNotNull(module, "Module " + id + " must be registered");
            assertEquals(ModuleCategory.COMBAT, module.getCategory(), "Module " + id + " must be COMBAT");
            assertInstanceOf(NivoratModule.class, module, "Module " + id + " must extend NivoratModule");
        }
    }

    @Test
    void testCombatSettingGroupsStrictOrder() {
        List<String> combatIds = List.of(ParticlePhysicsModule.ID, VectorStreamModule.ID, ShaderPassModule.ID, MatrixTransformModule.ID);
        for (String id : combatIds) {
            IModule module = ModuleRegistry.get(id);
            assertNotNull(module);
            List<Setting<?>> settings = module.getSettings();
            assertFalse(settings.isEmpty(), "Module " + id + " must have settings");

            int lastOrdinal = -1;
            for (Setting<?> s : settings) {
                SettingGroup group = s.getGroup();
                assertNotNull(group, "Setting " + s.getId() + " in " + id + " must have a group");
                assertTrue(group.ordinal() >= lastOrdinal,
                        "Setting " + s.getId() + " in module " + id + " violates group ordering: "
                                + group + " (ordinal " + group.ordinal() + ") < previous (ordinal " + lastOrdinal + ")");
                lastOrdinal = group.ordinal();
            }
        }
    }

    @Test
    void testAutoMaceSettingsAndEngineSync() {
        IModule mod = ModuleRegistry.get(ParticlePhysicsModule.ID);
        assertInstanceOf(ParticlePhysicsModule.class, mod);
        ActivityConfig config = ActivityConfigManager.getConfig();

        assertEquals(-1, mod.getKeybind().getKeyCode());

        assertNotNull(mod.getSetting("swap_type"));
        assertNotNull(mod.getSetting("source_mode"));
        assertNotNull(mod.getSetting("enchant_mode"));
        assertNotNull(mod.getSetting("miss_behavior"));
        assertNotNull(mod.getSetting("restore_delay"));
        assertNotNull(mod.getSetting("miss_chance"));
        assertNotNull(mod.getSetting("random_delay"));
        assertNotNull(mod.getSetting("legit_mode"));

        BooleanSetting randSetting = (BooleanSetting) mod.getSetting("random_delay");
        assertTrue(config.autoMaceRandomDelay);
        assertTrue(RedstoneOptimizerConfig.randomDelay);
        randSetting.set(false);
        assertFalse(config.autoMaceRandomDelay);
        assertFalse(RedstoneOptimizerConfig.randomDelay);
        assertEquals(RedstoneOptimizerConfig.restoreDelayMs, RedstoneOptimizerConfig.randomMaxRestoreDelayMs);
        randSetting.set(true);
        assertTrue(config.autoMaceRandomDelay);
        assertTrue(RedstoneOptimizerConfig.randomDelay);

        EnumSetting missSetting = (EnumSetting) mod.getSetting("miss_behavior");
        assertEquals(List.of("sword_hit", "empty_swap"), missSetting.getOptions());

        missSetting.set("empty_swap");
        assertEquals("empty_swap", config.autoMaceMissBehavior);
        assertEquals(RedstoneOptimizerConfig.MISS_EMPTY_SWAP, RedstoneOptimizerConfig.missBehavior);

        missSetting.set("sword_hit");
        assertEquals("sword_hit", config.autoMaceMissBehavior);
        assertEquals(RedstoneOptimizerConfig.MISS_SWORD_HIT, RedstoneOptimizerConfig.missBehavior);

        EnumSetting sourceSetting = (EnumSetting) mod.getSetting("source_mode");
        sourceSetting.set("sword_only");
        assertEquals(RedstoneOptimizerConfig.MODE_SWORD_ONLY, RedstoneOptimizerConfig.sourceMode);

        sourceSetting.set("axe_only");
        assertEquals(RedstoneOptimizerConfig.MODE_AXE_ONLY, RedstoneOptimizerConfig.sourceMode);

        sourceSetting.set("sword_and_axe");
        assertEquals(RedstoneOptimizerConfig.MODE_SWORD_AND_AXE, RedstoneOptimizerConfig.sourceMode);

        sourceSetting.set("any");
        assertEquals(RedstoneOptimizerConfig.MODE_ANY, RedstoneOptimizerConfig.sourceMode);

        NumberSetting delaySetting = (NumberSetting) mod.getSetting("restore_delay");
        delaySetting.set(120.0);
        assertEquals(120, RedstoneOptimizerConfig.restoreDelayMs);

        EnumSetting swapSetting = (EnumSetting) mod.getSetting("swap_type");
        assertEquals(List.of("new", "old"), swapSetting.getOptions());
        assertEquals("new", swapSetting.get());
        assertEquals("new", config.autoMaceSwapType);

        swapSetting.set("old");
        assertEquals("old", config.autoMaceSwapType);
        assertTrue(RedstoneOptimizerConfig.enabled);

        swapSetting.set("new");
        assertEquals("new", config.autoMaceSwapType);
        assertTrue(RedstoneOptimizerConfig.enabled);
    }

    @Test
    void testAutoSpearSettingsAndControllerSync() {
        IModule mod = ModuleRegistry.get(VectorStreamModule.ID);
        assertInstanceOf(VectorStreamModule.class, mod);
        ActivityConfig config = ActivityConfigManager.getConfig();

        assertTrue(mod.getKeybind().isUnbound(), "AutoSpear primary toggle keybind is unbound by default to avoid collision with trigger keybind");
        Setting<?> triggerKbSetting = mod.getSetting("trigger_keybind");
        assertNotNull(triggerKbSetting, "trigger_keybind must be registered on AutoSpear");
        assertInstanceOf(KeybindSetting.class, triggerKbSetting);
        assertEquals(GLFW.GLFW_KEY_TAB, ((KeybindSetting) triggerKbSetting).get().getKeyCode(), "Default trigger keybind must be TAB");

        assertNotNull(mod.getSetting("security_mode"));
        assertNotNull(mod.getSetting("priority_mode"));
        assertNotNull(mod.getSetting("restore_delay"));
        assertNotNull(mod.getSetting("miss_chance"));
        assertNotNull(mod.getSetting("random_delay"));

        assertNull(mod.getSetting("legit_mode"), "AutoSpear must NOT have a boolean legit_mode setting");

        NumberSetting initDelaySetting = (NumberSetting) mod.getSetting("restore_delay");
        assertEquals(185.0, initDelaySetting.get(), 0.001);
        assertEquals(185, VectorStreamConfig.maxDelayMs);

        EnumSetting secSetting = (EnumSetting) mod.getSetting("security_mode");
        assertEquals(List.of("legit", "semi_legit", "rage"), secSetting.getOptions());

        secSetting.set("semi_legit");
        assertEquals("semi_legit", config.autoSpearSecurityMode);
        assertEquals(VectorStreamConfig.MODE_SEMI_LEGIT, VectorStreamConfig.securityMode);

        secSetting.set("rage");
        assertEquals("rage", config.autoSpearSecurityMode);
        assertEquals(VectorStreamConfig.MODE_RAGE, VectorStreamConfig.securityMode);

        secSetting.set("legit");
        assertEquals("legit", config.autoSpearSecurityMode);
        assertEquals(VectorStreamConfig.MODE_LEGIT, VectorStreamConfig.securityMode);

        EnumSetting prioSetting = (EnumSetting) mod.getSetting("priority_mode");
        prioSetting.set("lunge_1");
        assertEquals(VectorStreamConfig.PRIORITY_LUNGE_1, VectorStreamConfig.priorityMode);

        prioSetting.set("lunge_2");
        assertEquals(VectorStreamConfig.PRIORITY_LUNGE_2, VectorStreamConfig.priorityMode);

        prioSetting.set("lunge_3");
        assertEquals(VectorStreamConfig.PRIORITY_LUNGE_3, VectorStreamConfig.priorityMode);

        prioSetting.set("random");
        assertEquals(VectorStreamConfig.PRIORITY_RANDOM, VectorStreamConfig.priorityMode);

        prioSetting.set("auto");
        assertEquals(VectorStreamConfig.PRIORITY_AUTO, VectorStreamConfig.priorityMode);

        NumberSetting delaySetting = (NumberSetting) mod.getSetting("restore_delay");
        delaySetting.set(85.0);
        assertEquals(85, VectorStreamConfig.maxDelayMs);

        NumberSetting missSetting = (NumberSetting) mod.getSetting("miss_chance");
        missSetting.set(5.0);
        assertEquals(5, VectorStreamConfig.missChance);

        BooleanSetting randSetting = (BooleanSetting) mod.getSetting("random_delay");
        randSetting.set(false);
        assertFalse(VectorStreamConfig.randomDelay);
        randSetting.set(true);
        assertTrue(VectorStreamConfig.randomDelay);
    }

    @Test
    void testAutoShieldbreakerSettingsAndControllerSync() {
        IModule mod = ModuleRegistry.get(ShaderPassModule.ID);
        assertInstanceOf(ShaderPassModule.class, mod);
        ActivityConfig config = ActivityConfigManager.getConfig();

        assertEquals(GLFW.GLFW_KEY_J, mod.getKeybind().getKeyCode());
        assertTrue(mod.getKeybind().isCtrl());
        assertTrue(mod.getKeybind().isShift());

        assertNotNull(mod.getSetting("mode"));
        assertNotNull(mod.getSetting("distance"));
        assertNotNull(mod.getSetting("chance"));
        assertNotNull(mod.getSetting("switch_delay"));
        assertNotNull(mod.getSetting("restore_delay"));
        assertNotNull(mod.getSetting("random_delay"));
        assertNotNull(mod.getSetting("abort_on_manual_switch"));
        assertNotNull(mod.getSetting("legit_mode"));

        EnumSetting modeSetting = (EnumSetting) mod.getSetting("mode");
        modeSetting.set("semi_auto");
        assertEquals(ShaderPassConfig.MODE_SEMI_AUTO, ShaderPassConfig.mode);
        modeSetting.set("full_auto");
        assertEquals(ShaderPassConfig.MODE_FULL_AUTO, ShaderPassConfig.mode);

        NumberSetting distSetting = (NumberSetting) mod.getSetting("distance");
        distSetting.set(3.2);
        assertEquals(3.2, ShaderPassConfig.triggerDistance, 0.001);

        NumberSetting chanceSetting = (NumberSetting) mod.getSetting("chance");
        chanceSetting.set(80.0);
        assertEquals(80, ShaderPassConfig.chance);

        NumberSetting switchSetting = (NumberSetting) mod.getSetting("switch_delay");
        switchSetting.set(40.0);
        assertEquals(40, ShaderPassConfig.switchDelayMs);

        NumberSetting restoreSetting = (NumberSetting) mod.getSetting("restore_delay");
        restoreSetting.set(60.0);
        assertEquals(60, ShaderPassConfig.restoreDelayMs);

        assertNotNull(mod.getSetting("reaction_delay"));
        NumberSetting reactSetting = (NumberSetting) mod.getSetting("reaction_delay");
        reactSetting.set(0.35);
        assertEquals(0.35, ShaderPassConfig.reactionDelaySec, 0.001);
        assertEquals(0.35, config.autoShieldbreakerReactionDelaySec, 0.001);

        BooleanSetting randSetting = (BooleanSetting) mod.getSetting("random_delay");
        randSetting.set(false);
        assertFalse(ShaderPassConfig.randomDelay);
        randSetting.set(true);
        assertTrue(ShaderPassConfig.randomDelay);

        BooleanSetting abortSetting = (BooleanSetting) mod.getSetting("abort_on_manual_switch");
        abortSetting.set(false);
        assertFalse(config.autoShieldbreakerAbortOnManualSwitch);
        assertFalse(ShaderPassConfig.abortOnManualSwitch);
        abortSetting.set(true);
        assertTrue(config.autoShieldbreakerAbortOnManualSwitch);
        assertTrue(ShaderPassConfig.abortOnManualSwitch);

        BooleanSetting legitSetting = (BooleanSetting) mod.getSetting("legit_mode");
        legitSetting.set(false);
        assertFalse(ShaderPassConfig.legitMode);
        legitSetting.set(true);
        assertTrue(ShaderPassConfig.legitMode);
    }

    @Test
    void testAutoStunSlamSettingsAndControllerSync() {
        IModule mod = ModuleRegistry.get(MatrixTransformModule.ID);
        assertInstanceOf(MatrixTransformModule.class, mod);
        ActivityConfig config = ActivityConfigManager.getConfig();

        assertEquals(GLFW.GLFW_KEY_M, mod.getKeybind().getKeyCode());
        assertTrue(mod.getKeybind().isCtrl());
        assertTrue(mod.getKeybind().isShift());

        assertEquals("activity.module.auto_stun_slam.name", mod.getName().getString());

        assertNotNull(mod.getSetting("mode"));
        assertNotNull(mod.getSetting("distance"));
        assertNotNull(mod.getSetting("chance"));
        assertNotNull(mod.getSetting("air_condition"));
        assertNotNull(mod.getSetting("min_fall"));
        assertNotNull(mod.getSetting("air_time"));
        assertNotNull(mod.getSetting("axe_delay"));
        assertNotNull(mod.getSetting("mace_delay"));
        assertNotNull(mod.getSetting("restore_delay"));
        assertNotNull(mod.getSetting("random_delay"));
        assertNotNull(mod.getSetting("legit_mode"));

        NumberSetting airSetting = (NumberSetting) mod.getSetting("air_time");
        airSetting.set(1.5);
        assertEquals(1.5, config.autoStunSlamAirTimeSec, 0.001);
        assertEquals(1.5, ParticlePhysicsConfig.airTimeSec, 0.001);

        BooleanSetting randSetting = (BooleanSetting) mod.getSetting("random_delay");
        randSetting.set(false);
        assertFalse(config.autoStunSlamRandomDelay);
        assertFalse(ParticlePhysicsConfig.randomDelay);
        randSetting.set(true);
        assertTrue(config.autoStunSlamRandomDelay);
        assertTrue(ParticlePhysicsConfig.randomDelay);

        NumberSetting axeSetting = (NumberSetting) mod.getSetting("axe_delay");
        axeSetting.set(50.0);
        assertEquals(50, ParticlePhysicsConfig.axeDelayMs);

        NumberSetting maceSetting = (NumberSetting) mod.getSetting("mace_delay");
        maceSetting.set(55.0);
        assertEquals(55, ParticlePhysicsConfig.maceDelayMs);

        NumberSetting restoreSetting = (NumberSetting) mod.getSetting("restore_delay");
        restoreSetting.set(65.0);
        assertEquals(65, ParticlePhysicsConfig.restoreDelayMs);
    }

    @Test
    void testAutoStunSlimeLegacyAliasResolution() {
        IModule aliasMod = ModuleRegistry.get("auto_stun_slime");
        assertNotNull(aliasMod);
        assertEquals("auto_stun_slam", aliasMod.getId());
        assertSame(ModuleRegistry.get("auto_stun_slam"), aliasMod);
    }

    @Test
    void testActivityConfigLegacyMigration() {
        ActivityConfig config = new ActivityConfig();

        config.autoStunSlimeEnabled = false;
        config.autoStunSlimeAirTimeSec = 2.5;
        config.autoStunSlimeRandomDelay = false;
        config.autoStunSlimeChance = 90.0;

        config.sanitize();

        assertFalse(config.autoStunSlamEnabled);
        assertEquals(2.5, config.autoStunSlamAirTimeSec, 0.001);
        assertFalse(config.autoStunSlamRandomDelay);
        assertEquals(90.0, config.autoStunSlamChance, 0.001);

        assertNull(config.autoStunSlimeEnabled);
        assertNull(config.autoStunSlimeAirTimeSec);
        assertNull(config.autoStunSlimeRandomDelay);
        assertNull(config.autoStunSlimeChance);
    }

    @Test
    void testActivityConfigCopyAndEquals() {
        ActivityConfig config1 = new ActivityConfig();
        config1.autoMaceMissBehavior = "empty_swap";
        config1.autoMaceRandomDelay = false;
        config1.autoSpearSecurityMode = "rage";
        config1.autoSpearPriorityMode = "lunge_3";
        config1.autoSpearMissChance = 12.0;
        config1.autoSpearRandomDelay = false;
        config1.autoShieldbreakerRandomDelay = false;
        config1.autoShieldbreakerAbortOnManualSwitch = false;
        config1.autoStunSlamAirTimeSec = 2.2;
        config1.autoStunSlamRandomDelay = false;

        ActivityConfig copy = config1.copy();
        assertEquals(config1, copy);
        assertEquals(config1.hashCode(), copy.hashCode());

        copy.autoSpearSecurityMode = "legit";
        assertNotEquals(config1, copy);
    }

    @Test
    void testCombatLocalization() throws Exception {
        java.io.InputStream ruStream = getClass().getResourceAsStream("/assets/activity/lang/ru_ru.json");
        assertNotNull(ruStream, "ru_ru.json must exist in classpath");
        String ruJson = new String(ruStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(ruJson.contains("\"activity.module.auto_stun_slam.name\": \"Авто Стан Слэм\""),
                "ru_ru.json must translate auto_stun_slam to 'Авто Стан Слэм'");
        assertTrue(ruJson.contains("\"activity.setting.combat.abort_on_manual_switch\": \"Прерывать при ручном свапе\""),
                "ru_ru.json must translate abort_on_manual_switch");

        java.io.InputStream enStream = getClass().getResourceAsStream("/assets/activity/lang/en_us.json");
        assertNotNull(enStream, "en_us.json must exist in classpath");
        String enJson = new String(enStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(enJson.contains("\"activity.module.auto_stun_slam.name\": \"AutoStunSlam\""),
                "en_us.json must translate auto_stun_slam to 'AutoStunSlam'");
        assertTrue(enJson.contains("\"activity.setting.combat.abort_on_manual_switch\": \"Abort on Manual Switch\""),
                "en_us.json must translate abort_on_manual_switch");
    }
}

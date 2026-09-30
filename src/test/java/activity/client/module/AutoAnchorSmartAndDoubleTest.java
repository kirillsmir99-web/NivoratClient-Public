package activity.client.module;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.mixin.pipeline.PipelineInteractionManagerAccessor;
import activity.client.module.api.BuiltinModules;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.impl.defense.LightmapFilterModule;
import activity.client.module.setting.EnumSetting;
import dev.lighting.LightmapFilterConfig;
import dev.lighting.LightmapFilterController;
import net.fabricmc.pack.api.SafeSlotManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class AutoAnchorSmartAndDoubleTest {

    @BeforeAll
    static void initRegistry() {
        BuiltinModules.registerAll();
    }

    @BeforeEach
    void resetState() {
        ActivityConfigManager.resetDefaults();
    }

    @Test
    void testAutoAnchorModeSettingsAndConfigSync() {
        IModule mod = ModuleRegistry.get(LightmapFilterModule.ID);
        assertNotNull(mod, "AutoAnchor module must be registered");
        assertInstanceOf(LightmapFilterModule.class, mod);

        EnumSetting modeSetting = (EnumSetting) mod.getSetting("mode");
        assertNotNull(modeSetting, "Mode setting must exist");
        assertEquals(List.of("smart", "double"), modeSetting.getOptions());
        assertEquals("smart", modeSetting.get());

        ActivityConfig config = ActivityConfigManager.getConfig();
        assertNotNull(config);
        assertEquals("smart", config.autoAnchorMode);
        assertEquals("smart", LightmapFilterConfig.mode);

        modeSetting.set("double");
        assertEquals("double", config.autoAnchorMode);
        assertEquals("double", LightmapFilterConfig.mode);

        modeSetting.set("smart");
        assertEquals("smart", config.autoAnchorMode);
        assertEquals("smart", LightmapFilterConfig.mode);
    }

    @Test
    void testGaussianTimingPresetsAndSafeDistribution() {
        LightmapFilterController controller = new LightmapFilterController();

        LightmapFilterConfig.preset = "FAST";
        for (int i = 0; i < 50; i++) {
            long delay = controller.getActionDelay(0);
            assertTrue(delay >= 35L && delay <= 55L, "Fast delay " + delay + " out of [35, 55] ms range");
            long cycleInterval = controller.getMinCycleIntervalMs();
            assertTrue(cycleInterval >= 35L && cycleInterval <= 55L, "Fast cycle interval " + cycleInterval + " out of [35, 55] ms range");
        }

        LightmapFilterConfig.preset = "MEDIUM";
        for (int i = 0; i < 50; i++) {
            long delay = controller.getActionDelay(0);
            assertTrue(delay >= 65L && delay <= 90L, "Medium delay " + delay + " out of [65, 90] ms range");
            long cycleInterval = controller.getMinCycleIntervalMs();
            assertTrue(cycleInterval >= 65L && cycleInterval <= 90L, "Medium cycle interval " + cycleInterval + " out of [65, 90] ms range");
        }

        LightmapFilterConfig.preset = "SAFE";
        for (int i = 0; i < 50; i++) {
            long delay = controller.getActionDelay(0);
            assertTrue(delay >= 120L && delay <= 160L, "Safe delay " + delay + " out of [120, 160] ms range");
            long cycleInterval = controller.getMinCycleIntervalMs();
            assertTrue(cycleInterval >= 120L && cycleInterval <= 160L, "Safe cycle interval " + cycleInterval + " out of [120, 160] ms range");
        }

        LightmapFilterConfig.preset = "BALANCED";
        for (int i = 0; i < 50; i++) {
            long delay = controller.getActionDelay(0);
            assertTrue(delay >= 70L && delay <= 100L, "Balanced delay " + delay + " out of [70, 100] ms range");
            long cycleInterval = controller.getMinCycleIntervalMs();
            assertTrue(cycleInterval >= 70L && cycleInterval <= 100L, "Balanced cycle interval " + cycleInterval + " out of [70, 100] ms range");
        }
    }

    @Test
    void testNullStackAndPlayerSafety() {
        LightmapFilterController controller = new LightmapFilterController();

        assertFalse(controller.isWeaponItem(null), "Null stack is not weapon");
        assertTrue(controller.isSafeDetonateItem(null), "Null stack is safe detonate item");
        assertEquals(0, controller.resolveDetonateSlot((net.minecraft.client.network.ClientPlayerEntity) null, 3, 1), "Null player defaults to 0");
        assertEquals(0, controller.resolveDetonateSlot((net.minecraft.entity.player.PlayerInventory) null, 3, 1), "Null inventory defaults to 0");
    }

    @Test
    void testWeaponAndSafeItemDetection() {
        LightmapFilterController controller = new LightmapFilterController();

        assertFalse(controller.isWeaponItem(null));
        assertTrue(controller.isSafeDetonateItem(null));

        assertTrue(LightmapFilterController.isWeaponName("diamond_sword"));
        assertTrue(LightmapFilterController.isWeaponName("netherite_axe"));
        assertTrue(LightmapFilterController.isWeaponName("mace"));
        assertTrue(LightmapFilterController.isWeaponName("iron_sword"));
        assertTrue(LightmapFilterController.isWeaponName("golden_axe"));
        assertTrue(LightmapFilterController.isWeaponName("алмазный_меч"));
        assertTrue(LightmapFilterController.isWeaponName("булава"));
        assertTrue(LightmapFilterController.isWeaponName("незеритовый_топор"));

        assertFalse(LightmapFilterController.isWeaponName("diamond_pickaxe"), "Pickaxe must NEVER be recognized as weapon");
        assertFalse(LightmapFilterController.isWeaponName("netherite_pickaxe"));
        assertFalse(LightmapFilterController.isWeaponName("iron_shovel"));
        assertFalse(LightmapFilterController.isWeaponName("obsidian"));
        assertFalse(LightmapFilterController.isWeaponName("respawn_anchor"));
        assertFalse(LightmapFilterController.isWeaponName(null));
        assertFalse(LightmapFilterController.isWeaponName(""));

        assertTrue(LightmapFilterController.isSafeDetonateName("totem_of_undying"));
        assertTrue(LightmapFilterController.isSafeDetonateName("stick"));
        assertTrue(LightmapFilterController.isSafeDetonateName("feather"));
        assertTrue(LightmapFilterController.isSafeDetonateName(null));

        assertFalse(LightmapFilterController.isSafeDetonateName("respawn_anchor"));
        assertFalse(LightmapFilterController.isSafeDetonateName("glowstone"));
        assertFalse(LightmapFilterController.isSafeDetonateName("obsidian"));
        assertFalse(LightmapFilterController.isSafeDetonateName("dirt"));
        assertFalse(LightmapFilterController.isSafeDetonateName("stone"));
        assertFalse(LightmapFilterController.isSafeDetonateName("tnt"));
        assertFalse(LightmapFilterController.isSafeDetonateName("shield"));
        assertFalse(LightmapFilterController.isSafeDetonateName("bow"));
        assertFalse(LightmapFilterController.isSafeDetonateName("trident"));
    }

    @Test
    void testSafeSlotManagerEliminatedReflectionAndHasMixinInvoker() throws NoSuchMethodException {
        for (Field f : SafeSlotManager.class.getDeclaredFields()) {
            assertNotEquals(Field.class, f.getType(), "SafeSlotManager must not declare java.lang.reflect.Field instances");
        }

        Method invokeSyncMethod = PipelineInteractionManagerAccessor.class.getMethod("invokeSyncSelectedSlot");
        assertNotNull(invokeSyncMethod, "PipelineInteractionManagerAccessor must declare invokeSyncSelectedSlot");
        assertEquals(void.class, invokeSyncMethod.getReturnType());

        Method setSlotMethod = PipelineInteractionManagerAccessor.class.getMethod("activity$setLastSelectedSlot", int.class);
        assertNotNull(setSlotMethod);

        Method getSlotMethod = PipelineInteractionManagerAccessor.class.getMethod("activity$getLastSelectedSlot");
        assertNotNull(getSlotMethod);
        assertEquals(int.class, getSlotMethod.getReturnType());

        assertFalse(SafeSlotManager.setLastSelectedSlot(null, 3));
        assertEquals(-1, SafeSlotManager.getLastSelectedSlot(null));
        assertFalse(SafeSlotManager.selectSlot(null, 3));
        assertFalse(SafeSlotManager.selectSlot(null, -1));
        assertFalse(SafeSlotManager.selectSlot(null, 9));
        SafeSlotManager.reset();
    }

    @Test
    void testConfigPresetSanitization() {
        ActivityConfig config = new ActivityConfig();

        config.autoAnchorMode = "invalid_mode";
        config.sanitize();
        assertEquals("smart", config.autoAnchorMode);

        config.autoAnchorMode = "Double Anchor";
        config.sanitize();
        assertEquals("double", config.autoAnchorMode);

        config.autoAnchorMode = "double";
        config.sanitize();
        assertEquals("double", config.autoAnchorMode);

        config.autoAnchorMode = "Smart Auto";
        config.sanitize();
        assertEquals("smart", config.autoAnchorMode);
    }

    @Test
    void testZeroCommentsInMainSourceTree() throws IOException {
        File srcDir = new File("src/main/java");
        if (!srcDir.exists()) {
            srcDir = new File("../src/main/java");
        }
        assertTrue(srcDir.exists(), "src/main/java must exist");

        Pattern stringLiteralPattern = Pattern.compile("\"[^\"]*\"");
        Files.walk(srcDir.toPath())
            .filter(p -> p.toString().endsWith(".java"))
            .forEach(p -> {
                try {
                    List<String> lines = Files.readAllLines(p);
                    for (int i = 0; i < lines.size(); i++) {
                        String line = lines.get(i).trim();
                        String cleaned = stringLiteralPattern.matcher(line).replaceAll("");
                        assertFalse(cleaned.contains("//"), "Comment '//' found in " + p + ":" + (i + 1));
                        assertFalse(cleaned.contains("/*"), "Comment '/*' found in " + p + ":" + (i + 1));
                        assertFalse(cleaned.contains("*/"), "Comment '*/' found in " + p + ":" + (i + 1));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
    }
}

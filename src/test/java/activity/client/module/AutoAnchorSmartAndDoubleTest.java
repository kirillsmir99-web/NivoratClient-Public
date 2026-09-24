package activity.client.module;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.mixin.autotool.ActivityClientPlayerInteractionManagerAccessor;
import activity.client.module.api.BuiltinModules;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.impl.defense.AutoAnchorModule;
import activity.client.module.setting.EnumSetting;
import dev.luminance.AnchorConfig;
import dev.luminance.AnchorController;
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
        IModule mod = ModuleRegistry.get(AutoAnchorModule.ID);
        assertNotNull(mod, "AutoAnchor module must be registered");
        assertInstanceOf(AutoAnchorModule.class, mod);

        EnumSetting modeSetting = (EnumSetting) mod.getSetting("mode");
        assertNotNull(modeSetting, "Mode setting must exist");
        assertEquals(List.of("smart", "double"), modeSetting.getOptions());
        assertEquals("smart", modeSetting.get());

        ActivityConfig config = ActivityConfigManager.getConfig();
        assertNotNull(config);
        assertEquals("smart", config.autoAnchorMode);
        assertEquals("smart", AnchorConfig.mode);

        modeSetting.set("double");
        assertEquals("double", config.autoAnchorMode);
        assertEquals("double", AnchorConfig.mode);

        modeSetting.set("smart");
        assertEquals("smart", config.autoAnchorMode);
        assertEquals("smart", AnchorConfig.mode);
    }

    @Test
    void testGaussianTimingPresetsAndSafeDistribution() {
        AnchorController controller = new AnchorController();

        AnchorConfig.preset = "FAST";
        for (int i = 0; i < 50; i++) {
            long delay = controller.getActionDelay(0);
            assertTrue(delay >= 35L && delay <= 55L, "Fast delay " + delay + " out of [35, 55] ms range");
            long cycleInterval = controller.getMinCycleIntervalMs();
            assertTrue(cycleInterval >= 35L && cycleInterval <= 55L, "Fast cycle interval " + cycleInterval + " out of [35, 55] ms range");
        }

        AnchorConfig.preset = "MEDIUM";
        for (int i = 0; i < 50; i++) {
            long delay = controller.getActionDelay(0);
            assertTrue(delay >= 65L && delay <= 90L, "Medium delay " + delay + " out of [65, 90] ms range");
            long cycleInterval = controller.getMinCycleIntervalMs();
            assertTrue(cycleInterval >= 65L && cycleInterval <= 90L, "Medium cycle interval " + cycleInterval + " out of [65, 90] ms range");
        }

        AnchorConfig.preset = "SAFE";
        for (int i = 0; i < 50; i++) {
            long delay = controller.getActionDelay(0);
            assertTrue(delay >= 120L && delay <= 160L, "Safe delay " + delay + " out of [120, 160] ms range");
            long cycleInterval = controller.getMinCycleIntervalMs();
            assertTrue(cycleInterval >= 120L && cycleInterval <= 160L, "Safe cycle interval " + cycleInterval + " out of [120, 160] ms range");
        }

        AnchorConfig.preset = "BALANCED";
        for (int i = 0; i < 50; i++) {
            long delay = controller.getActionDelay(0);
            assertTrue(delay >= 70L && delay <= 100L, "Balanced delay " + delay + " out of [70, 100] ms range");
            long cycleInterval = controller.getMinCycleIntervalMs();
            assertTrue(cycleInterval >= 70L && cycleInterval <= 100L, "Balanced cycle interval " + cycleInterval + " out of [70, 100] ms range");
        }
    }

    @Test
    void testNullStackAndPlayerSafety() {
        AnchorController controller = new AnchorController();

        assertFalse(controller.isWeaponItem(null), "Null stack is not weapon");
        assertTrue(controller.isSafeDetonateItem(null), "Null stack is safe detonate item");
        assertEquals(0, controller.resolveDetonateSlot((net.minecraft.client.network.ClientPlayerEntity) null, 3, 1), "Null player defaults to 0");
        assertEquals(0, controller.resolveDetonateSlot((net.minecraft.entity.player.PlayerInventory) null, 3, 1), "Null inventory defaults to 0");
    }

    @Test
    void testWeaponAndSafeItemDetection() {
        AnchorController controller = new AnchorController();

        assertFalse(controller.isWeaponItem(null));
        assertTrue(controller.isSafeDetonateItem(null));

        assertTrue(AnchorController.isWeaponName("diamond_sword"));
        assertTrue(AnchorController.isWeaponName("netherite_axe"));
        assertTrue(AnchorController.isWeaponName("mace"));
        assertTrue(AnchorController.isWeaponName("iron_sword"));
        assertTrue(AnchorController.isWeaponName("golden_axe"));
        assertTrue(AnchorController.isWeaponName("алмазный_меч"));
        assertTrue(AnchorController.isWeaponName("булава"));
        assertTrue(AnchorController.isWeaponName("незеритовый_топор"));

        assertFalse(AnchorController.isWeaponName("diamond_pickaxe"), "Pickaxe must NEVER be recognized as weapon");
        assertFalse(AnchorController.isWeaponName("netherite_pickaxe"));
        assertFalse(AnchorController.isWeaponName("iron_shovel"));
        assertFalse(AnchorController.isWeaponName("obsidian"));
        assertFalse(AnchorController.isWeaponName("respawn_anchor"));
        assertFalse(AnchorController.isWeaponName(null));
        assertFalse(AnchorController.isWeaponName(""));

        assertTrue(AnchorController.isSafeDetonateName("totem_of_undying"));
        assertTrue(AnchorController.isSafeDetonateName("stick"));
        assertTrue(AnchorController.isSafeDetonateName("feather"));
        assertTrue(AnchorController.isSafeDetonateName(null));

        assertFalse(AnchorController.isSafeDetonateName("respawn_anchor"));
        assertFalse(AnchorController.isSafeDetonateName("glowstone"));
        assertFalse(AnchorController.isSafeDetonateName("obsidian"));
        assertFalse(AnchorController.isSafeDetonateName("dirt"));
        assertFalse(AnchorController.isSafeDetonateName("stone"));
        assertFalse(AnchorController.isSafeDetonateName("tnt"));
        assertFalse(AnchorController.isSafeDetonateName("shield"));
        assertFalse(AnchorController.isSafeDetonateName("bow"));
        assertFalse(AnchorController.isSafeDetonateName("trident"));
    }

    @Test
    void testSafeSlotManagerEliminatedReflectionAndHasMixinInvoker() throws NoSuchMethodException {
        for (Field f : SafeSlotManager.class.getDeclaredFields()) {
            assertNotEquals(Field.class, f.getType(), "SafeSlotManager must not declare java.lang.reflect.Field instances");
        }

        Method invokeSyncMethod = ActivityClientPlayerInteractionManagerAccessor.class.getMethod("invokeSyncSelectedSlot");
        assertNotNull(invokeSyncMethod, "ActivityClientPlayerInteractionManagerAccessor must declare invokeSyncSelectedSlot");
        assertEquals(void.class, invokeSyncMethod.getReturnType());

        Method setSlotMethod = ActivityClientPlayerInteractionManagerAccessor.class.getMethod("activity$setLastSelectedSlot", int.class);
        assertNotNull(setSlotMethod);

        Method getSlotMethod = ActivityClientPlayerInteractionManagerAccessor.class.getMethod("activity$getLastSelectedSlot");
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

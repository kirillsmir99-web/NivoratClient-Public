package activity.client.module.impl.combat;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.icon.ActivityIcon;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.NivoratModule;
import activity.client.module.setting.SettingGroup;
import dev.vector.VectorStreamConfig;
import dev.vector.VectorStreamController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class VectorStreamModule extends NivoratModule {
    public static final String ID = "auto_spear";
    private final VectorStreamController controller = new VectorStreamController();
    private final activity.client.module.setting.KeybindSetting triggerKeybindSetting;

    public VectorStreamModule() {
        super(ID, Text.translatable("activity.module.auto_spear.name"), Text.translatable("activity.module.auto_spear.desc"), ModuleCategory.COMBAT);
        this.keybind.clear();
        this.metadata = ModuleMetadata.builder(ID)
                .displayName(name)
                .description(description)
                .category(category)
                .author("Nivorat")
                .version("2.1.0")
                .icon(ActivityIcon.COMBAT)
                .keybind(keybind)
                .aliases("autospear", "spear", "копье", "копьё", "автокопье", "авто-копье", "авто копье", "выпад", "выпад копьем", "выпад копьём", "задержка", "delay", "restore")
                .build();

        this.triggerKeybindSetting = registerKeybind("trigger_keybind", Text.translatable("activity.setting.combat.trigger_keybind"),
                Text.translatable("activity.setting.combat.trigger_keybind.desc"), SettingGroup.GENERAL,
                new activity.client.module.keybind.Keybind(GLFW.GLFW_KEY_TAB),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoSpearTriggerKeybind : new activity.client.module.keybind.Keybind(GLFW.GLFW_KEY_TAB);
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoSpearTriggerKeybind.copyFrom(val);
                        ActivityConfigManager.markDirty();
                    }
                }
        );
        this.triggerKeybindSetting.onPress(client -> {
            if (isEnabled()) {
                controller.onTrigger(client);
            }
        });
        this.triggerKeybindSetting.onRelease(client -> {
            controller.onKeyRelease(client);
        });
        registerEnum("security_mode", Text.translatable("activity.setting.combat.security_mode"),
                Text.translatable("activity.setting.combat.security_mode.desc"), SettingGroup.GENERAL,
                List.of("legit", "semi_legit", "rage"), "legit",
                opt -> Text.translatable("activity.dropdown.security_mode." + opt),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoSpearSecurityMode : "legit";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoSpearSecurityMode = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerEnum("priority_mode", Text.translatable("activity.setting.combat.priority_mode"),
                Text.translatable("activity.setting.combat.priority_mode.desc"), SettingGroup.GENERAL,
                List.of("auto", "lunge_1", "lunge_2", "lunge_3", "random"), "auto",
                opt -> Text.translatable("activity.dropdown.priority_mode." + opt),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoSpearPriorityMode : "auto";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoSpearPriorityMode = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("restore_delay", Text.translatable("activity.setting.combat.restore_delay"),
                Text.translatable("activity.setting.combat.restore_delay.desc"), SettingGroup.BEHAVIOR,
                0.0, 500.0, 5.0, " ms", true, 185.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoSpearRestoreDelayMs : 185.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoSpearRestoreDelayMs = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("miss_chance", Text.translatable("activity.setting.combat.miss_chance"),
                Text.translatable("activity.setting.combat.miss_chance.desc"), SettingGroup.BEHAVIOR,
                0.0, 50.0, 1.0, "%", true, 0.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoSpearMissChance : 0.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoSpearMissChance = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("max_speed", Text.literal("Максимальная скорость (0-Tick)"),
                Text.literal("Мгновенный удар копьем и возврат оружия в том же тике"), SettingGroup.ADVANCED,
                false,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoSpearMaxSpeed;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoSpearMaxSpeed = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("check_charge", Text.literal("Проверка заряда атаки"),
                Text.literal("Ожидать перезарядки удара оружия перед использованием копья"), SettingGroup.ADVANCED,
                false,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoSpearCheckCharge;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoSpearCheckCharge = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("random_delay", Text.translatable("activity.setting.combat.random_delay"),
                Text.translatable("activity.setting.combat.random_delay.desc"), SettingGroup.ADVANCED,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoSpearRandomDelay;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoSpearRandomDelay = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        activity.client.module.setting.BooleanSetting fastSwapSetting = registerBoolean("fast_swap",
                Text.translatable("activity.setting.combat.fast_swap"),
                Text.translatable("activity.setting.combat.fast_swap.desc"),
                SettingGroup.ADVANCED,
                false,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoSpearFastSwap;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoSpearFastSwap = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );
        fastSwapSetting.requireConfirm(
                "Предупреждение",
                "Вы уверены, что хотите включить?",
                "Это экспериментальная функция."
        );
    }

    private void syncControllerConfig(ActivityConfig c) {
        if (c == null) return;
        VectorStreamConfig.enabled = c.autoSpearEnabled;
        VectorStreamConfig.maxDelayMs = (int) c.autoSpearRestoreDelayMs;
        VectorStreamConfig.randomDelay = c.autoSpearRandomDelay;
        VectorStreamConfig.missChance = (int) c.autoSpearMissChance;
        VectorStreamConfig.maxSpeed = c.autoSpearMaxSpeed;
        VectorStreamConfig.checkCharge = c.autoSpearCheckCharge;
        VectorStreamConfig.fastSwap = c.autoSpearFastSwap;

        if ("semi_legit".equals(c.autoSpearSecurityMode)) {
            VectorStreamConfig.securityMode = VectorStreamConfig.MODE_SEMI_LEGIT;
        } else if ("rage".equals(c.autoSpearSecurityMode)) {
            VectorStreamConfig.securityMode = VectorStreamConfig.MODE_RAGE;
        } else {
            VectorStreamConfig.securityMode = VectorStreamConfig.MODE_LEGIT;
        }

        if ("lunge_1".equals(c.autoSpearPriorityMode)) {
            VectorStreamConfig.priorityMode = VectorStreamConfig.PRIORITY_LUNGE_1;
        } else if ("lunge_2".equals(c.autoSpearPriorityMode)) {
            VectorStreamConfig.priorityMode = VectorStreamConfig.PRIORITY_LUNGE_2;
        } else if ("lunge_3".equals(c.autoSpearPriorityMode)) {
            VectorStreamConfig.priorityMode = VectorStreamConfig.PRIORITY_LUNGE_3;
        } else if ("random".equals(c.autoSpearPriorityMode)) {
            VectorStreamConfig.priorityMode = VectorStreamConfig.PRIORITY_RANDOM;
        } else {
            VectorStreamConfig.priorityMode = VectorStreamConfig.PRIORITY_AUTO;
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            c.autoSpearEnabled = enabled;
            ActivityConfigManager.markDirty();
        }
        VectorStreamConfig.enabled = enabled;
    }

    @Override
    public void onDisable() {
        controller.onKeyRelease(null);
        net.fabricmc.pack.api.CombatLockManager.setLock("pvp.spear_active", false);
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (isEnabled()) {
            if (controller.isKeyHeld() && !isTriggerKeyPressed(client)) {
                controller.onKeyRelease(client);
            }
            controller.tick(client);
        }
    }

    private boolean isTriggerKeyPressed(MinecraftClient client) {
        if (client == null || client.getWindow() == null || client.currentScreen != null) return false;
        activity.client.module.keybind.Keybind kb = triggerKeybindSetting != null ? triggerKeybindSetting.get() : null;
        if (kb == null || kb.isUnbound()) return false;
        net.minecraft.client.util.Window window = client.getWindow();
        if (window.getHandle() == 0L) return false;
        boolean ctrl = net.minecraft.client.util.InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || net.minecraft.client.util.InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
        boolean shift = net.minecraft.client.util.InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || net.minecraft.client.util.InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
        boolean alt = net.minecraft.client.util.InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_ALT)
                || net.minecraft.client.util.InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_ALT);
        return kb.matchesWindow(window, ctrl, shift, alt);
    }

    @Override
    public ActionResult onAttackEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        if (isEnabled()) {
            return controller.onAttackEntity(player, world, hand, entity, hitResult);
        }
        return ActionResult.PASS;
    }

    @Override
    public void loadFromConfig(ActivityConfig config) {
        if (config == null) return;
        this.enabled = config.autoSpearEnabled;
        this.keybind.copyFrom(config.autoSpearKeybind);
        syncControllerConfig(config);
    }

    public void triggerManual(MinecraftClient client) {
        if (isEnabled() && client != null) {
            controller.onTrigger(client);
        }
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        config.autoSpearEnabled = this.enabled;
        config.autoSpearKeybind.copyFrom(this.keybind);
        syncControllerConfig(config);
    }
}

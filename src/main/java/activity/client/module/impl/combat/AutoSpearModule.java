package activity.client.module.impl.combat;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.icon.ActivityIcon;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.NivoratModule;
import activity.client.module.setting.SettingGroup;
import dev.momentum.SpearConfig;
import dev.momentum.SpearSwapController;
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

public class AutoSpearModule extends NivoratModule {
    public static final String ID = "auto_spear";
    private final SpearSwapController controller = new SpearSwapController();

    public AutoSpearModule() {
        super(ID, Text.translatable("activity.module.auto_spear.name"), Text.translatable("activity.module.auto_spear.desc"), ModuleCategory.COMBAT);
        this.keybind.clear();
        this.metadata = ModuleMetadata.builder(ID)
                .displayName(name)
                .description(description)
                .category(category)
                .author("kt1xW")
                .version("2.1.0")
                .icon(ActivityIcon.COMBAT)
                .keybind(keybind)
                .aliases("autospear", "spear", "копье", "копьё", "автокопье", "авто-копье", "авто копье", "выпад", "выпад копьем", "выпад копьём", "задержка", "delay", "restore")
                .build();

        registerKeybind("trigger_keybind", Text.translatable("activity.setting.combat.trigger_keybind"),
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
        ).onPress(client -> {
            if (isEnabled()) {
                controller.onTrigger(client);
            }
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
    }

    private void syncControllerConfig(ActivityConfig c) {
        if (c == null) return;
        SpearConfig.enabled = c.autoSpearEnabled;
        SpearConfig.maxDelayMs = (int) c.autoSpearRestoreDelayMs;
        SpearConfig.randomDelay = c.autoSpearRandomDelay;
        SpearConfig.missChance = (int) c.autoSpearMissChance;

        if ("semi_legit".equals(c.autoSpearSecurityMode)) {
            SpearConfig.securityMode = SpearConfig.MODE_SEMI_LEGIT;
        } else if ("rage".equals(c.autoSpearSecurityMode)) {
            SpearConfig.securityMode = SpearConfig.MODE_RAGE;
        } else {
            SpearConfig.securityMode = SpearConfig.MODE_LEGIT;
        }

        if ("lunge_1".equals(c.autoSpearPriorityMode)) {
            SpearConfig.priorityMode = SpearConfig.PRIORITY_LUNGE_1;
        } else if ("lunge_2".equals(c.autoSpearPriorityMode)) {
            SpearConfig.priorityMode = SpearConfig.PRIORITY_LUNGE_2;
        } else if ("lunge_3".equals(c.autoSpearPriorityMode)) {
            SpearConfig.priorityMode = SpearConfig.PRIORITY_LUNGE_3;
        } else if ("random".equals(c.autoSpearPriorityMode)) {
            SpearConfig.priorityMode = SpearConfig.PRIORITY_RANDOM;
        } else {
            SpearConfig.priorityMode = SpearConfig.PRIORITY_AUTO;
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
        SpearConfig.enabled = enabled;
    }

    @Override
    public void onDisable() {
        net.fabricmc.pack.api.CombatLockManager.setLock("pvp.spear_active", false);
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (isEnabled()) {
            controller.tick(client);
        }
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

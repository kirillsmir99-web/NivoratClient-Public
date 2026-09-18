package activity.client.module.impl.combat;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.icon.ActivityIcon;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.NivoratModule;
import activity.client.module.setting.SettingGroup;
import dev.sunder.SunderConfig;
import dev.sunder.SunderController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

import java.util.List;

public class AutoStunSlamModule extends NivoratModule {
    public static final String ID = "auto_stun_slam";
    public static final String LEGACY_ID = "auto_stun_slime";
    private final SunderController controller = new SunderController();

    public AutoStunSlamModule() {
        super(ID, Text.translatable("activity.module.auto_stun_slam.name"), Text.translatable("activity.module.auto_stun_slam.desc"), ModuleCategory.COMBAT);
        this.keybind.set(org.lwjgl.glfw.GLFW.GLFW_KEY_M, true, true, false);
        this.metadata = ModuleMetadata.builder(ID)
                .displayName(name)
                .description(description)
                .category(category)
                .author("Nivorat")
                .version("1.1.2")
                .icon(ActivityIcon.COMBAT)
                .keybind(keybind)
                .aliases("autostunslam", "stunslam", "slam", "stun", "стан слэм", "стан-слэм", "авто стан слэм", "авто-стан-слэм", "стан", "блок", "задержка", "delay", "дистанция", "autostunslime", "auto_stun_slime", "стан слизь", "слизь")
                .build();

        // 1. GENERAL
        registerEnum("mode", Text.translatable("activity.setting.combat.slam_mode"),
                Text.translatable("activity.setting.combat.slam_mode.desc"), SettingGroup.GENERAL,
                List.of("full_auto", "semi_auto"), "full_auto",
                opt -> Text.translatable("activity.dropdown.breaker." + opt),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoStunSlamMode : "full_auto";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamMode = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        // 2. BEHAVIOR
        registerNumber("distance", Text.translatable("activity.setting.combat.slam_distance"),
                Text.translatable("activity.setting.combat.slam_distance.desc"), SettingGroup.BEHAVIOR,
                1.5, 4.0, 0.1, " бл.", false, 2.4,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoStunSlamDistance : 2.4;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamDistance = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("chance", Text.translatable("activity.setting.combat.chance_label"),
                Text.translatable("activity.setting.combat.chance_label.desc"), SettingGroup.BEHAVIOR,
                10.0, 100.0, 5.0, "%", true, 75.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoStunSlamChance : 75.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamChance = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("air_time", Text.translatable("activity.setting.combat.air_time"),
                Text.translatable("activity.setting.combat.air_time.desc"), SettingGroup.BEHAVIOR,
                0.1, 5.0, 0.1, " s", false, 1.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoStunSlamAirTimeSec : 1.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamAirTimeSec = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("axe_delay", Text.translatable("activity.setting.combat.axe_delay"),
                Text.translatable("activity.setting.combat.axe_delay.desc"), SettingGroup.BEHAVIOR,
                10.0, 200.0, 5.0, " ms", true, 45.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoStunSlamAxeDelayMs : 45.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamAxeDelayMs = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("mace_delay", Text.translatable("activity.setting.combat.mace_delay"),
                Text.translatable("activity.setting.combat.mace_delay.desc"), SettingGroup.BEHAVIOR,
                10.0, 200.0, 5.0, " ms", true, 45.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoStunSlamMaceDelayMs : 45.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamMaceDelayMs = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("restore_delay", Text.translatable("activity.setting.combat.restore_delay"),
                Text.translatable("activity.setting.combat.restore_delay.desc"), SettingGroup.BEHAVIOR,
                10.0, 200.0, 5.0, " ms", true, 50.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoStunSlamRestoreDelayMs : 50.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamRestoreDelayMs = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        // 3. ADVANCED
        registerBoolean("random_delay", Text.translatable("activity.setting.combat.random_delay"),
                Text.translatable("activity.setting.combat.random_delay.desc"), SettingGroup.ADVANCED,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoStunSlamRandomDelay;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamRandomDelay = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("legit_mode", Text.translatable("activity.setting.combat.legit_mode"),
                Text.translatable("activity.setting.combat.legit_mode.desc"), SettingGroup.ADVANCED,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoStunSlamLegitMode;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamLegitMode = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );
    }

    private void syncControllerConfig(ActivityConfig c) {
        if (c == null) return;
        SunderConfig.enabled = c.autoStunSlamEnabled;
        SunderConfig.mode = "semi_auto".equals(c.autoStunSlamMode) ? SunderConfig.MODE_SEMI_AUTO : SunderConfig.MODE_FULL_AUTO;
        SunderConfig.triggerDistance = c.autoStunSlamDistance;
        SunderConfig.chance = (int) c.autoStunSlamChance;
        SunderConfig.airTimeSec = c.autoStunSlamAirTimeSec;
        SunderConfig.axeDelayMs = (int) c.autoStunSlamAxeDelayMs;
        SunderConfig.maceDelayMs = (int) c.autoStunSlamMaceDelayMs;
        SunderConfig.restoreDelayMs = (int) c.autoStunSlamRestoreDelayMs;
        SunderConfig.randomDelay = c.autoStunSlamRandomDelay;
        SunderConfig.legitMode = c.autoStunSlamLegitMode;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            c.autoStunSlamEnabled = enabled;
            ActivityConfigManager.markDirty();
        }
        SunderConfig.enabled = enabled;
    }

    @Override
    public void onDisable() {
        controller.reset();
        net.fabricmc.pack.api.CombatLockManager.setLock("pvp.sunder_active", false);
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
        this.enabled = config.autoStunSlamEnabled;
        this.keybind.copyFrom(config.autoStunSlamKeybind);
        syncControllerConfig(config);
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        config.autoStunSlamEnabled = this.enabled;
        config.autoStunSlamKeybind.copyFrom(this.keybind);
        syncControllerConfig(config);
    }
}

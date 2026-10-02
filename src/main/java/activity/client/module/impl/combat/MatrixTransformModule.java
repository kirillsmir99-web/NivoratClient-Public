package activity.client.module.impl.combat;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.icon.ActivityIcon;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.NivoratModule;
import activity.client.module.setting.SettingGroup;
import dev.particle.ParticlePhysicsConfig;
import dev.particle.ParticlePhysicsController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

import java.util.List;

public class MatrixTransformModule extends NivoratModule {
    public static final String ID = "auto_stun_slam";
    public static final String LEGACY_ID = "auto_stun_slime";
    private final ParticlePhysicsController controller = new ParticlePhysicsController();

    public MatrixTransformModule() {
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

        registerNumber("distance", Text.translatable("activity.setting.combat.slam_distance"),
                Text.translatable("activity.setting.combat.slam_distance.desc"), SettingGroup.BEHAVIOR,
                1.5, 4.0, 0.1, " бл.", false, 2.85,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoStunSlamDistance : 2.85;
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
                10.0, 100.0, 5.0, "%", true, 100.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoStunSlamChance : 100.0;
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
                0.0, 3.0, 0.05, " s", false, 0.1,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoStunSlamAirTimeSec : 0.1;
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
                0.0, 200.0, 5.0, " ms", true, 0.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoStunSlamAxeDelayMs : 0.0;
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
                0.0, 200.0, 5.0, " ms", true, 0.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoStunSlamMaceDelayMs : 0.0;
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
                0.0, 200.0, 5.0, " ms", true, 50.0,
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
        ParticlePhysicsConfig.enabled = c.autoStunSlamEnabled;
        ParticlePhysicsConfig.mode = "semi_auto".equals(c.autoStunSlamMode) ? ParticlePhysicsConfig.MODE_SEMI_AUTO : ParticlePhysicsConfig.MODE_FULL_AUTO;
        ParticlePhysicsConfig.triggerDistance = c.autoStunSlamDistance;
        ParticlePhysicsConfig.chance = (int) c.autoStunSlamChance;
        ParticlePhysicsConfig.airTimeSec = c.autoStunSlamAirTimeSec;
        ParticlePhysicsConfig.axeDelayMs = (int) c.autoStunSlamAxeDelayMs;
        ParticlePhysicsConfig.maceDelayMs = (int) c.autoStunSlamMaceDelayMs;
        ParticlePhysicsConfig.restoreDelayMs = (int) c.autoStunSlamRestoreDelayMs;
        ParticlePhysicsConfig.randomDelay = c.autoStunSlamRandomDelay;
        ParticlePhysicsConfig.legitMode = c.autoStunSlamLegitMode;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            c.autoStunSlamEnabled = enabled;
            ActivityConfigManager.markDirty();
        }
        ParticlePhysicsConfig.enabled = enabled;
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

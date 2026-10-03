package activity.client.module.impl.combat;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.icon.ActivityIcon;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.NivoratModule;
import activity.client.module.setting.SettingGroup;
import dev.mace.prestige.PrestigeStunSlamConfig;
import dev.mace.prestige.PrestigeStunSlamController;
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
                .version("2.5.0")
                .icon(ActivityIcon.COMBAT)
                .keybind(keybind)
                .aliases("autostunslam", "stunslam", "slam", "stun", "стан слэм", "стан-слэм", "авто стан слэм", "авто-стан-слэм", "стан", "блок", "задержка", "delay", "дистанция", "autostunslime", "auto_stun_slime", "стан слизь", "слизь")
                .build();

        var presetSetting = registerEnum("preset", Text.translatable("activity.setting.combat.slam_preset"),
                Text.translatable("activity.setting.combat.slam_preset.desc"), SettingGroup.GENERAL,
                List.of("new", "old"), "new",
                opt -> Text.translatable("activity.dropdown.slam_preset." + opt),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && "old".equals(c.autoStunSlamPreset) ? "old" : "new";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamPreset = val;
                        c.autoStunSlamEngineMode = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerEnum("mode", Text.translatable("activity.setting.combat.stage1_mode"),
                Text.translatable("activity.setting.combat.stage1_mode.desc"), SettingGroup.GENERAL,
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
        ).visibleWhen(presetSetting, "old");

        registerNumber("distance", Text.translatable("activity.setting.combat.stage1_distance"),
                Text.translatable("activity.setting.combat.stage1_distance.desc"), SettingGroup.BEHAVIOR,
                1.5, 4.5, 0.1, " бл.", false, 3.2,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoStunSlamDistance : 3.2;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamDistance = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(presetSetting, "old");

        registerNumber("chance", Text.translatable("activity.setting.combat.stage1_chance"),
                Text.translatable("activity.setting.combat.stage1_chance.desc"), SettingGroup.BEHAVIOR,
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
        ).visibleWhen(presetSetting, "old");

        var airConditionSetting = registerEnum("air_condition", Text.translatable("activity.setting.combat.stage1_air_condition"),
                Text.translatable("activity.setting.combat.stage1_air_condition.desc"), SettingGroup.BEHAVIOR,
                List.of("blocks", "time", "both", "any"), "blocks",
                opt -> Text.translatable("activity.dropdown.air_condition." + opt),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoStunSlamAirCondition : "blocks";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamAirCondition = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(presetSetting, "old");

        registerNumber("min_fall", Text.translatable("activity.setting.combat.stage1_fall"),
                Text.translatable("activity.setting.combat.stage1_fall.desc"), SettingGroup.BEHAVIOR,
                0.0, 10.0, 0.1, " бл.", false, 3.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoStunSlamMinFall : 3.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamMinFall = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(airConditionSetting, val -> "blocks".equalsIgnoreCase(val) || "both".equalsIgnoreCase(val) || "any".equalsIgnoreCase(val));

        registerNumber("air_time", Text.translatable("activity.setting.combat.stage1_air_time"),
                Text.translatable("activity.setting.combat.stage1_air_time.desc"), SettingGroup.BEHAVIOR,
                0.0, 5.0, 0.05, " s", false, 1.0,
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
        ).visibleWhen(airConditionSetting, val -> "time".equalsIgnoreCase(val) || "both".equalsIgnoreCase(val) || "any".equalsIgnoreCase(val));

        registerNumber("axe_delay", Text.translatable("activity.setting.combat.stage2_axe_delay"),
                Text.translatable("activity.setting.combat.stage2_axe_delay.desc"), SettingGroup.BEHAVIOR,
                0.0, 100.0, 5.0, " ms", true, 0.0,
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
        ).visibleWhen(presetSetting, "old");

        registerBoolean("axe_randomizer", Text.translatable("activity.setting.combat.stage2_axe_randomizer"),
                Text.translatable("activity.setting.combat.stage2_axe_randomizer.desc"), SettingGroup.BEHAVIOR,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoStunSlamAxeRandomizer;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamAxeRandomizer = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(presetSetting, "old");

        registerNumber("axe_jitter", Text.translatable("activity.setting.combat.stage2_axe_jitter"),
                Text.translatable("activity.setting.combat.stage2_axe_jitter.desc"), SettingGroup.BEHAVIOR,
                0.0, 50.0, 5.0, " ms", true, 10.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoStunSlamAxeJitterMs : 10.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamAxeJitterMs = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(presetSetting, "old");

        registerNumber("mace_delay", Text.translatable("activity.setting.combat.stage3_mace_delay"),
                Text.translatable("activity.setting.combat.stage3_mace_delay.desc"), SettingGroup.BEHAVIOR,
                0.0, 100.0, 5.0, " ms", true, 20.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoStunSlamMaceDelayMs : 20.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamMaceDelayMs = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(presetSetting, "old");

        registerBoolean("mace_randomizer", Text.translatable("activity.setting.combat.stage3_mace_randomizer"),
                Text.translatable("activity.setting.combat.stage3_mace_randomizer.desc"), SettingGroup.BEHAVIOR,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoStunSlamMaceRandomizer;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamMaceRandomizer = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(presetSetting, "old");

        registerNumber("mace_jitter", Text.translatable("activity.setting.combat.stage3_mace_jitter"),
                Text.translatable("activity.setting.combat.stage3_mace_jitter.desc"), SettingGroup.BEHAVIOR,
                0.0, 50.0, 5.0, " ms", true, 15.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoStunSlamMaceJitterMs : 15.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamMaceJitterMs = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(presetSetting, "old");

        registerEnum("enchant_preference", Text.translatable("activity.setting.combat.stage3_enchant"),
                Text.translatable("activity.setting.combat.stage3_enchant.desc"), SettingGroup.BEHAVIOR,
                List.of("auto", "density", "breach"), "auto",
                opt -> Text.translatable("activity.dropdown.enchant." + opt),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoStunSlamEnchantPreference : "auto";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamEnchantPreference = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(presetSetting, "old");

        registerNumber("restore_delay", Text.translatable("activity.setting.combat.stage4_restore_delay"),
                Text.translatable("activity.setting.combat.stage4_restore_delay.desc"), SettingGroup.BEHAVIOR,
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
        ).visibleWhen(presetSetting, "old");

        registerNumber("new_distance", Text.translatable("activity.setting.combat.new_stage1_distance"),
                Text.translatable("activity.setting.combat.new_stage1_distance.desc"), SettingGroup.BEHAVIOR,
                1.5, 5.0, 0.1, " бл.", false, 3.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoStunSlamNewDistance : 3.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamNewDistance = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(presetSetting, "new");

        registerNumber("new_chance", Text.translatable("activity.setting.combat.new_stage1_chance"),
                Text.translatable("activity.setting.combat.new_stage1_chance.desc"), SettingGroup.BEHAVIOR,
                10.0, 100.0, 5.0, "%", true, 100.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoStunSlamNewChance : 100.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamNewChance = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(presetSetting, "new");

        var newAirConditionSetting = registerEnum("new_air_condition", Text.translatable("activity.setting.combat.new_stage1_air_condition"),
                Text.translatable("activity.setting.combat.new_stage1_air_condition.desc"), SettingGroup.BEHAVIOR,
                List.of("blocks", "time", "both", "any"), "blocks",
                opt -> Text.translatable("activity.dropdown.air_condition." + opt),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoStunSlamNewAirCondition : "blocks";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamNewAirCondition = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(presetSetting, "new");

        registerNumber("new_min_fall", Text.translatable("activity.setting.combat.new_stage1_fall"),
                Text.translatable("activity.setting.combat.new_stage1_fall.desc"), SettingGroup.BEHAVIOR,
                0.0, 10.0, 0.1, " бл.", false, 3.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoStunSlamNewMinFall : 3.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamNewMinFall = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(newAirConditionSetting, val -> "blocks".equalsIgnoreCase(val) || "both".equalsIgnoreCase(val) || "any".equalsIgnoreCase(val));

        registerNumber("new_air_time", Text.translatable("activity.setting.combat.new_stage1_air_time"),
                Text.translatable("activity.setting.combat.new_stage1_air_time.desc"), SettingGroup.BEHAVIOR,
                0.0, 5.0, 0.05, " s", false, 1.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoStunSlamNewAirTimeSec : 1.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamNewAirTimeSec = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(newAirConditionSetting, val -> "time".equalsIgnoreCase(val) || "both".equalsIgnoreCase(val) || "any".equalsIgnoreCase(val));

        registerNumber("new_delay", Text.translatable("activity.setting.combat.new_stage2_delay"),
                Text.translatable("activity.setting.combat.new_stage2_delay.desc"), SettingGroup.BEHAVIOR,
                0.0, 100.0, 5.0, " ms", true, 0.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoStunSlamNewAttackDelayMs : 0.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamNewAttackDelayMs = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(presetSetting, "new");

        registerEnum("new_enchant", Text.translatable("activity.setting.combat.new_stage2_enchant"),
                Text.translatable("activity.setting.combat.new_stage2_enchant.desc"), SettingGroup.BEHAVIOR,
                List.of("smart", "breach_only", "density_only"), "smart",
                opt -> Text.translatable("activity.dropdown.enchant." + opt),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoStunSlamNewEnchant : "smart";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamNewEnchant = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(presetSetting, "new");

        registerBoolean("new_silent_aim", Text.translatable("activity.setting.combat.new_stage2_silent_aim"),
                Text.translatable("activity.setting.combat.new_stage2_silent_aim.desc"), SettingGroup.BEHAVIOR,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoStunSlamNewSilentAim;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamNewSilentAim = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(presetSetting, "new");

        registerBoolean("restore_randomizer", Text.translatable("activity.setting.combat.stage4_restore_randomizer"),
                Text.translatable("activity.setting.combat.stage4_restore_randomizer.desc"), SettingGroup.ADVANCED,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoStunSlamRestoreRandomizer;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamRestoreRandomizer = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(presetSetting, "old");

        registerBoolean("stay_on_weapon", Text.translatable("activity.setting.combat.stage4_stay_weapon"),
                Text.translatable("activity.setting.combat.stage4_stay_weapon.desc"), SettingGroup.ADVANCED,
                false,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoStunSlamStayOnWeapon;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamStayOnWeapon = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(presetSetting, "old");

        registerBoolean("random_delay", Text.translatable("activity.setting.combat.stage4_random_delay"),
                Text.translatable("activity.setting.combat.stage4_random_delay.desc"), SettingGroup.ADVANCED,
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
        ).visibleWhen(presetSetting, "old");

        registerBoolean("legit_mode", Text.translatable("activity.setting.combat.stage4_legit_mode"),
                Text.translatable("activity.setting.combat.stage4_legit_mode.desc"), SettingGroup.ADVANCED,
                false,
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
        ).visibleWhen(presetSetting, "old");

        registerBoolean("new_randomizer", Text.translatable("activity.setting.combat.new_stage3_randomizer"),
                Text.translatable("activity.setting.combat.new_stage3_randomizer.desc"), SettingGroup.ADVANCED,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoStunSlamNewRandomizer;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamNewRandomizer = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(presetSetting, "new");

        registerNumber("new_jitter", Text.translatable("activity.setting.combat.new_stage3_jitter"),
                Text.translatable("activity.setting.combat.new_stage3_jitter.desc"), SettingGroup.ADVANCED,
                0.0, 50.0, 5.0, " ms", true, 15.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoStunSlamNewJitter : 15.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamNewJitter = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(presetSetting, "new");

        registerBoolean("new_stay_on_mace", Text.translatable("activity.setting.combat.new_stage3_stay_mace"),
                Text.translatable("activity.setting.combat.new_stage3_stay_mace.desc"), SettingGroup.ADVANCED,
                false,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoStunSlamNewStayOnMace;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoStunSlamNewStayOnMace = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(presetSetting, "new");
    }

    private void syncControllerConfig(ActivityConfig c) {
        if (c == null) return;
        boolean isNew = "new".equals(c.autoStunSlamPreset);

        ParticlePhysicsConfig.enabled = c.autoStunSlamEnabled && !isNew;
        ParticlePhysicsConfig.mode = "semi_auto".equals(c.autoStunSlamMode) ? ParticlePhysicsConfig.MODE_SEMI_AUTO : ParticlePhysicsConfig.MODE_FULL_AUTO;
        ParticlePhysicsConfig.triggerDistance = c.autoStunSlamDistance;
        ParticlePhysicsConfig.chance = (int) c.autoStunSlamChance;
        ParticlePhysicsConfig.airCondition = c.autoStunSlamAirCondition;
        ParticlePhysicsConfig.minFallDistance = c.autoStunSlamMinFall;
        ParticlePhysicsConfig.airTimeSec = c.autoStunSlamAirTimeSec;
        ParticlePhysicsConfig.axeDelayMs = (int) c.autoStunSlamAxeDelayMs;
        ParticlePhysicsConfig.axeJitterMs = c.autoStunSlamAxeRandomizer ? c.autoStunSlamAxeJitterMs : 0.0;
        ParticlePhysicsConfig.maceDelayMs = (int) c.autoStunSlamMaceDelayMs;
        ParticlePhysicsConfig.maceJitterMs = c.autoStunSlamMaceRandomizer ? c.autoStunSlamMaceJitterMs : 0.0;
        if ("density".equals(c.autoStunSlamEnchantPreference)) {
            ParticlePhysicsConfig.enchantPreference = ParticlePhysicsConfig.ENCHANT_DENSITY;
        } else if ("breach".equals(c.autoStunSlamEnchantPreference)) {
            ParticlePhysicsConfig.enchantPreference = ParticlePhysicsConfig.ENCHANT_BREACH;
        } else {
            ParticlePhysicsConfig.enchantPreference = ParticlePhysicsConfig.ENCHANT_AUTO;
        }
        ParticlePhysicsConfig.restoreDelayMs = (int) c.autoStunSlamRestoreDelayMs;
        ParticlePhysicsConfig.stayOnWeapon = c.autoStunSlamStayOnWeapon;
        ParticlePhysicsConfig.randomDelay = c.autoStunSlamRandomDelay;
        ParticlePhysicsConfig.legitMode = c.autoStunSlamLegitMode;

        PrestigeStunSlamConfig pc = PrestigeStunSlamController.getInstance().getConfig();
        pc.enabled = c.autoStunSlamEnabled && isNew;
        pc.mode = c.autoStunSlamNewMode;
        pc.chance = c.autoStunSlamNewChance;
        pc.attackDelayMs = c.autoStunSlamNewAttackDelayMs;
        pc.triggerDistance = c.autoStunSlamNewDistance;
        pc.airCondition = c.autoStunSlamNewAirCondition;
        pc.minFallDistance = c.autoStunSlamNewMinFall;
        pc.airTimeSec = c.autoStunSlamNewAirTimeSec;
        pc.enchantMode = c.autoStunSlamNewEnchant;
        pc.silentAim = c.autoStunSlamNewSilentAim;
        pc.randomizer = c.autoStunSlamNewRandomizer;
        pc.randomJitterMs = c.autoStunSlamNewJitter;
        pc.stayOnMace = c.autoStunSlamNewStayOnMace;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            c.autoStunSlamEnabled = enabled;
            ActivityConfigManager.markDirty();
        }
        syncControllerConfig(c);
    }

    @Override
    public void onDisable() {
        controller.reset();
        PrestigeStunSlamController.getInstance().reset();
        net.fabricmc.pack.api.CombatLockManager.setLock("pvp.sunder_active", false);
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (isEnabled()) {
            ActivityConfig c = ActivityConfigManager.getConfig();
            if (c != null && "new".equals(c.autoStunSlamPreset)) {
                PrestigeStunSlamController.getInstance().tick(client);
            } else {
                controller.tick(client);
            }
        }
    }

    @Override
    public ActionResult onAttackEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        if (isEnabled()) {
            ActivityConfig c = ActivityConfigManager.getConfig();
            if (c != null && "new".equals(c.autoStunSlamPreset)) {
                return PrestigeStunSlamController.getInstance().onAttackEntity(player, world, hand, entity, hitResult);
            } else {
                return controller.onAttackEntity(player, world, hand, entity, hitResult);
            }
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

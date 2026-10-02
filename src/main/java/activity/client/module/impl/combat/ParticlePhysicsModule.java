package activity.client.module.impl.combat;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.icon.ActivityIcon;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.NivoratModule;
import activity.client.module.setting.SettingGroup;
import dev.mace.prestige.PrestigeAutoMaceConfig;
import dev.mace.prestige.PrestigeAutoMaceController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import net.redstone.optimizer.config.RedstoneOptimizerConfig;
import net.redstone.optimizer.engine.RedstoneTickEngine;

import java.util.List;

public class ParticlePhysicsModule extends NivoratModule {
    public static final String ID = "auto_mace";
    private final RedstoneTickEngine engine = new RedstoneTickEngine();

    public ParticlePhysicsModule() {
        super(ID, Text.translatable("activity.module.auto_mace.name"), Text.translatable("activity.module.auto_mace.desc"), ModuleCategory.COMBAT);
        this.metadata = ModuleMetadata.builder(ID)
                .displayName(name)
                .description(description)
                .category(category)
                .author("Nivorat")
                .version("2.5.0")
                .icon(ActivityIcon.COMBAT)
                .keybind(keybind)
                .aliases("automace", "mace", "булава", "автобулава", "авто-булава", "авто булава", "свап", "swap", "чары", "enchant", "miss", "промах", "legit", "легит", "prestige", "станслэм")
                .build();

        var engineModeSetting = registerEnum("engine_mode", Text.translatable("activity.setting.combat.engine_mode"),
                Text.translatable("activity.setting.combat.engine_mode.desc"), SettingGroup.GENERAL,
                List.of("test_mode", "our_old"), "test_mode",
                opt -> Text.translatable("activity.dropdown.engine_mode." + opt),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoMaceEngineMode : "test_mode";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoMaceEngineMode = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("auto_switch", Text.translatable("activity.setting.combat.auto_switch"),
                Text.translatable("activity.setting.combat.auto_switch.desc"), SettingGroup.GENERAL,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoMaceAutoSwitch;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoMaceAutoSwitch = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(engineModeSetting, "test_mode");

        registerBoolean("stun_slam", Text.translatable("activity.setting.combat.stun_slam"),
                Text.translatable("activity.setting.combat.stun_slam.desc"), SettingGroup.GENERAL,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoMaceStunSlam;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoMaceStunSlam = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(engineModeSetting, "test_mode");

        registerEnum("source_mode", Text.translatable("activity.setting.combat.source_mode"),
                Text.translatable("activity.setting.combat.source_mode.desc"), SettingGroup.GENERAL,
                List.of("sword_and_axe", "sword_only", "axe_only"), "sword_and_axe",
                opt -> Text.translatable("activity.dropdown.source." + opt),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoMaceSourceMode : "sword_and_axe";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoMaceSourceMode = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(engineModeSetting, "our_old");

        registerEnum("enchant_mode", Text.translatable("activity.setting.combat.enchant_mode"),
                Text.translatable("activity.setting.combat.enchant_mode.desc"), SettingGroup.GENERAL,
                List.of("smart", "breach_only", "density_only"), "smart",
                opt -> Text.translatable("activity.dropdown.enchant." + opt),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoMaceEnchantMode : "smart";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoMaceEnchantMode = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerEnum("miss_behavior", Text.translatable("activity.setting.combat.miss_behavior"),
                Text.translatable("activity.setting.combat.miss_behavior.desc"), SettingGroup.GENERAL,
                List.of("sword_hit", "empty_swap"), "sword_hit",
                opt -> Text.translatable("activity.dropdown.miss_behavior." + opt),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoMaceMissBehavior : "sword_hit";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoMaceMissBehavior = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(engineModeSetting, "our_old");

        registerNumber("min_fall_distance", Text.translatable("activity.setting.combat.min_fall_distance"),
                Text.translatable("activity.setting.combat.min_fall_distance.desc"), SettingGroup.BEHAVIOR,
                0.5, 5.0, 0.05, " бл.", false, 1.25,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoMaceMinFallDistance : 1.25;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoMaceMinFallDistance = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(engineModeSetting, "test_mode");

        registerNumber("silent_aim_range", Text.translatable("activity.setting.combat.silent_aim_range"),
                Text.translatable("activity.setting.combat.silent_aim_range.desc"), SettingGroup.BEHAVIOR,
                2.0, 20.0, 0.5, " бл.", false, 4.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoMaceSilentAimRange : 4.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoMaceSilentAimRange = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(engineModeSetting, "test_mode");

        registerNumber("hitbox_expand", Text.translatable("activity.setting.combat.hitbox_expand"),
                Text.translatable("activity.setting.combat.hitbox_expand.desc"), SettingGroup.BEHAVIOR,
                1.0, 5.0, 0.1, "x", false, 1.5,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoMaceHitboxExpand : 1.5;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoMaceHitboxExpand = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(engineModeSetting, "test_mode");

        registerNumber("restore_delay", Text.translatable("activity.setting.combat.restore_delay"),
                Text.translatable("activity.setting.combat.restore_delay.desc"), SettingGroup.BEHAVIOR,
                30.0, 300.0, 5.0, " ms", true, 90.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoMaceRestoreDelayMs : 90.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoMaceRestoreDelayMs = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(engineModeSetting, "our_old");

        registerNumber("miss_chance", Text.translatable("activity.setting.combat.miss_chance"),
                Text.translatable("activity.setting.combat.miss_chance.desc"), SettingGroup.BEHAVIOR,
                0.0, 50.0, 1.0, "%", true, 10.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoMaceMissChance : 10.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoMaceMissChance = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(engineModeSetting, "our_old");

        registerNumber("attack_delay", Text.translatable("activity.setting.combat.attack_delay"),
                Text.translatable("activity.setting.combat.attack_delay.desc"), SettingGroup.BEHAVIOR,
                20.0, 250.0, 5.0, " ms", true, 60.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoMaceAttackDelayMs : 60.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoMaceAttackDelayMs = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(engineModeSetting, "test_mode");

        registerBoolean("silent_aim", Text.translatable("activity.setting.combat.silent_aim"),
                Text.translatable("activity.setting.combat.silent_aim.desc"), SettingGroup.ADVANCED,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoMaceSilentAim;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoMaceSilentAim = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(engineModeSetting, "test_mode");

        registerBoolean("movement_fix", Text.translatable("activity.setting.combat.movement_fix"),
                Text.translatable("activity.setting.combat.movement_fix.desc"), SettingGroup.ADVANCED,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoMaceMovementFix;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoMaceMovementFix = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(engineModeSetting, "test_mode");

        registerBoolean("stay_on_mace", Text.translatable("activity.setting.combat.stay_on_mace"),
                Text.translatable("activity.setting.combat.stay_on_mace.desc"), SettingGroup.ADVANCED,
                false,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoMaceStayOnMace;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoMaceStayOnMace = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(engineModeSetting, "test_mode");

        registerBoolean("target_players", Text.translatable("activity.setting.combat.target_players"),
                Text.translatable("activity.setting.combat.target_players.desc"), SettingGroup.ADVANCED,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoMaceTargetPlayers;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoMaceTargetPlayers = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(engineModeSetting, "test_mode");

        registerBoolean("target_mobs", Text.translatable("activity.setting.combat.target_mobs"),
                Text.translatable("activity.setting.combat.target_mobs.desc"), SettingGroup.ADVANCED,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoMaceTargetMobs;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoMaceTargetMobs = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(engineModeSetting, "test_mode");

        registerBoolean("random_delay", Text.translatable("activity.setting.combat.random_delay"),
                Text.translatable("activity.setting.combat.random_delay.desc"), SettingGroup.ADVANCED,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoMaceRandomDelay;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoMaceRandomDelay = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(engineModeSetting, "our_old");

        registerBoolean("legit_mode", Text.translatable("activity.setting.combat.legit_mode"),
                Text.translatable("activity.setting.combat.legit_mode.desc"), SettingGroup.ADVANCED,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoMaceLegitMode;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoMaceLegitMode = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(engineModeSetting, "our_old");

        registerBoolean("human_mode", Text.translatable("activity.setting.combat.human_mode"),
                Text.translatable("activity.setting.combat.human_mode.desc"), SettingGroup.ADVANCED,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoMaceHumanMode;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoMaceHumanMode = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(engineModeSetting, "test_mode");

        registerBoolean("random_jitter", Text.translatable("activity.setting.combat.random_jitter"),
                Text.translatable("activity.setting.combat.random_jitter.desc"), SettingGroup.ADVANCED,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoMaceRandomJitter;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoMaceRandomJitter = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(engineModeSetting, "test_mode");
    }

    private void syncEngineConfig(ActivityConfig c) {
        if (c == null) return;

        RedstoneOptimizerConfig.enabled = c.autoMaceEnabled && "our_old".equals(c.autoMaceEngineMode);
        RedstoneOptimizerConfig.restoreDelayMs = (int) c.autoMaceRestoreDelayMs;
        RedstoneOptimizerConfig.randomDelay = c.autoMaceRandomDelay;
        if (c.autoMaceRandomDelay) {
            RedstoneOptimizerConfig.randomMaxRestoreDelayMs = Math.max((int) c.autoMaceRestoreDelayMs + 30, 120);
        } else {
            RedstoneOptimizerConfig.randomMaxRestoreDelayMs = (int) c.autoMaceRestoreDelayMs;
        }
        RedstoneOptimizerConfig.legitMode = c.autoMaceLegitMode;
        RedstoneOptimizerConfig.missChance = (int) c.autoMaceMissChance;
        if ("breach_only".equals(c.autoMaceEnchantMode)) {
            RedstoneOptimizerConfig.enchantMode = RedstoneOptimizerConfig.ENCHANT_BREACH_ONLY;
        } else if ("density_only".equals(c.autoMaceEnchantMode)) {
            RedstoneOptimizerConfig.enchantMode = RedstoneOptimizerConfig.ENCHANT_DENSITY_ONLY;
        } else {
            RedstoneOptimizerConfig.enchantMode = RedstoneOptimizerConfig.ENCHANT_SMART;
        }
        if ("sword_only".equals(c.autoMaceSourceMode)) {
            RedstoneOptimizerConfig.sourceMode = RedstoneOptimizerConfig.MODE_SWORD_ONLY;
        } else if ("axe_only".equals(c.autoMaceSourceMode)) {
            RedstoneOptimizerConfig.sourceMode = RedstoneOptimizerConfig.MODE_AXE_ONLY;
        } else {
            RedstoneOptimizerConfig.sourceMode = RedstoneOptimizerConfig.MODE_SWORD_AND_AXE;
        }
        if ("empty_swap".equals(c.autoMaceMissBehavior)) {
            RedstoneOptimizerConfig.missBehavior = RedstoneOptimizerConfig.MISS_EMPTY_SWAP;
        } else {
            RedstoneOptimizerConfig.missBehavior = RedstoneOptimizerConfig.MISS_SWORD_HIT;
        }

        PrestigeAutoMaceConfig pc = PrestigeAutoMaceController.getInstance().getConfig();
        pc.enabled = c.autoMaceEnabled && "test_mode".equals(c.autoMaceEngineMode);
        pc.minFallDistance = c.autoMaceMinFallDistance;
        pc.autoSwitch = c.autoMaceAutoSwitch;
        pc.silentAim = c.autoMaceSilentAim;
        pc.silentAimRange = c.autoMaceSilentAimRange;
        pc.movementFix = c.autoMaceMovementFix;
        pc.stunSlam = c.autoMaceStunSlam;
        pc.hitbox = true;
        pc.hitboxExpand = c.autoMaceHitboxExpand;
        pc.targetPlayers = c.autoMaceTargetPlayers;
        pc.targetMobs = c.autoMaceTargetMobs;
        pc.stayOnMace = c.autoMaceStayOnMace;
        pc.attackDelayMs = c.autoMaceAttackDelayMs;
        pc.humanMode = c.autoMaceHumanMode;
        pc.randomJitter = c.autoMaceRandomJitter;
        pc.enchantMode = c.autoMaceEnchantMode;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            c.autoMaceEnabled = enabled;
            ActivityConfigManager.markDirty();
        }
        syncEngineConfig(c);
    }

    @Override
    public void onDisable() {
        engine.reset();
        PrestigeAutoMaceController.getInstance().reset();
        net.fabricmc.pack.api.CombatLockManager.setLock("pvp.mace_active", false);
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (isEnabled()) {
            ActivityConfig c = ActivityConfigManager.getConfig();
            if (c != null && "test_mode".equals(c.autoMaceEngineMode)) {
                PrestigeAutoMaceController.getInstance().tick(client);
            } else {
                engine.tick(client);
            }
        }
    }

    @Override
    public ActionResult onAttackEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        if (isEnabled()) {
            ActivityConfig c = ActivityConfigManager.getConfig();
            if (c != null && "our_old".equals(c.autoMaceEngineMode)) {
                return engine.onAttackEntity(player, world, hand, entity, hitResult);
            }
        }
        return ActionResult.PASS;
    }

    @Override
    public void loadFromConfig(ActivityConfig config) {
        if (config == null) return;
        this.enabled = config.autoMaceEnabled;
        this.keybind.copyFrom(config.autoMaceKeybind);
        syncEngineConfig(config);
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        config.autoMaceEnabled = this.enabled;
        config.autoMaceKeybind.copyFrom(this.keybind);
        syncEngineConfig(config);
    }
}

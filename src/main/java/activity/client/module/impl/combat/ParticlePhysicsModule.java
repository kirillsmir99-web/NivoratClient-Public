package activity.client.module.impl.combat;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.icon.ActivityIcon;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.NivoratModule;
import activity.client.module.setting.SettingGroup;
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
                .aliases("automace", "mace", "булава", "автобулава", "авто-булава", "авто булава", "свап", "swap", "bridge", "bridge swap", "бридж")
                .build();

        registerEnum("swap_type", Text.translatable("activity.setting.combat.swap_type"),
                Text.translatable("activity.setting.combat.swap_type.desc"), SettingGroup.GENERAL,
                List.of("new", "old"), "new",
                opt -> Text.translatable("activity.dropdown.swap_type." + opt),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && "old".equals(c.autoMaceSwapType) ? "old" : "new";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoMaceSwapType = val;
                        c.autoMaceEngineMode = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

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
        );

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
        );

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
        );

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
        );

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
        );

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
        );
    }

    private void syncEngineConfig(ActivityConfig c) {
        if (c == null) return;
        RedstoneOptimizerConfig.enabled = c.autoMaceEnabled;
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
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            c.autoMaceEnabled = enabled;
            ActivityConfigManager.markDirty();
        }
        RedstoneOptimizerConfig.enabled = enabled;
    }

    @Override
    public void onDisable() {
        engine.reset();
        net.fabricmc.pack.api.CombatLockManager.setLock("pvp.mace_active", false);
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (isEnabled()) {
            engine.tick(client);
        }
    }

    @Override
    public ActionResult onAttackEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        if (isEnabled()) {
            return engine.onAttackEntity(player, world, hand, entity, hitResult);
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

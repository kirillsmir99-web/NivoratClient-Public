package activity.client.module.impl.combat;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.icon.ActivityIcon;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.NivoratModule;
import activity.client.module.setting.SettingGroup;
import dev.shader.ShaderPassController;
import dev.shader.ShaderPassConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

import java.util.List;

public class ShaderPassModule extends NivoratModule {
    public static final String ID = "auto_shieldbreaker";
    private final ShaderPassController controller = new ShaderPassController();

    public ShaderPassModule() {
        super(ID, Text.translatable("activity.module.auto_shieldbreaker.name"), Text.translatable("activity.module.auto_shieldbreaker.desc"), ModuleCategory.COMBAT);
        this.keybind.set(org.lwjgl.glfw.GLFW.GLFW_KEY_J, true, true, false);
        this.metadata = ModuleMetadata.builder(ID)
                .displayName(name)
                .description(description)
                .category(category)
                .author("Nivorat")
                .version("1.4.2")
                .icon(ActivityIcon.COMBAT)
                .keybind(keybind)
                .aliases("autoshieldbreaker", "shieldbreaker", "shield", "breaker", "сбив щита", "автосбив щита", "авто-щит", "щит", "топор", "ломатель", "axe", "дистанция", "distance", "шанс", "chance")
                .build();

        registerEnum("mode", Text.translatable("activity.setting.combat.breaker_mode"),
                Text.translatable("activity.setting.combat.breaker_mode.desc"), SettingGroup.GENERAL,
                List.of("full_auto", "semi_auto"), "full_auto",
                opt -> Text.translatable("activity.dropdown.breaker." + opt),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoShieldbreakerMode : "full_auto";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoShieldbreakerMode = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("distance", Text.translatable("activity.setting.combat.breaker_distance"),
                Text.translatable("activity.setting.combat.breaker_distance.desc"), SettingGroup.BEHAVIOR,
                1.5, 4.5, 0.05, " бл.", false, 2.85,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoShieldbreakerDistance : 2.85;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoShieldbreakerDistance = val;
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
                    return c != null ? c.autoShieldbreakerChance : 100.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoShieldbreakerChance = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("switch_delay", Text.translatable("activity.setting.combat.switch_delay"),
                Text.translatable("activity.setting.combat.switch_delay.desc"), SettingGroup.BEHAVIOR,
                10.0, 200.0, 5.0, " ms", true, 50.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoShieldbreakerSwitchDelayMs : 50.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoShieldbreakerSwitchDelayMs = val;
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
                    return c != null ? c.autoShieldbreakerRestoreDelayMs : 50.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoShieldbreakerRestoreDelayMs = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("reaction_delay", Text.translatable("activity.setting.combat.reaction_delay"),
                Text.translatable("activity.setting.combat.reaction_delay.desc"), SettingGroup.BEHAVIOR,
                0.0, 2.0, 0.05, " s", false, 0.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoShieldbreakerReactionDelaySec : 0.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoShieldbreakerReactionDelaySec = val;
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
                    return c != null && c.autoShieldbreakerRandomDelay;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoShieldbreakerRandomDelay = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("abort_on_manual_switch", Text.translatable("activity.setting.combat.abort_on_manual_switch"),
                Text.translatable("activity.setting.combat.abort_on_manual_switch.desc"), SettingGroup.ADVANCED,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoShieldbreakerAbortOnManualSwitch;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoShieldbreakerAbortOnManualSwitch = val;
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
                    return c != null && c.autoShieldbreakerLegitMode;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoShieldbreakerLegitMode = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );
    }

    private void syncControllerConfig(ActivityConfig c) {
        if (c == null) return;
        ShaderPassConfig.enabled = c.autoShieldbreakerEnabled;
        ShaderPassConfig.mode = "semi_auto".equals(c.autoShieldbreakerMode) ? ShaderPassConfig.MODE_SEMI_AUTO : ShaderPassConfig.MODE_FULL_AUTO;
        ShaderPassConfig.triggerDistance = c.autoShieldbreakerDistance;
        ShaderPassConfig.chance = (int) c.autoShieldbreakerChance;
        ShaderPassConfig.switchDelayMs = (int) c.autoShieldbreakerSwitchDelayMs;
        ShaderPassConfig.restoreDelayMs = (int) c.autoShieldbreakerRestoreDelayMs;
        ShaderPassConfig.reactionDelaySec = c.autoShieldbreakerReactionDelaySec;
        ShaderPassConfig.randomDelay = c.autoShieldbreakerRandomDelay;
        ShaderPassConfig.abortOnManualSwitch = c.autoShieldbreakerAbortOnManualSwitch;
        ShaderPassConfig.legitMode = c.autoShieldbreakerLegitMode;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            c.autoShieldbreakerEnabled = enabled;
            ActivityConfigManager.markDirty();
        }
        ShaderPassConfig.enabled = enabled;
    }

    @Override
    public void onDisable() {
        controller.reset();
        net.fabricmc.pack.api.CombatLockManager.setLock("pvp.shield_combo_active", false);
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
        this.enabled = config.autoShieldbreakerEnabled;
        this.keybind.copyFrom(config.autoShieldbreakerKeybind);
        syncControllerConfig(config);
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        config.autoShieldbreakerEnabled = this.enabled;
        config.autoShieldbreakerKeybind.copyFrom(this.keybind);
        syncControllerConfig(config);
    }
}

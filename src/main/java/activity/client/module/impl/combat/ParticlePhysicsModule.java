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
                .aliases("automace", "mace", "булава", "автобулава", "авто-булава", "авто булава", "свап", "swap")
                .build();

        registerEnum("swap_type", Text.translatable("activity.setting.combat.swap_type"),
                Text.translatable("activity.setting.combat.swap_type.desc"), SettingGroup.GENERAL,
                List.of("old", "new"), "old",
                opt -> Text.translatable("activity.dropdown.swap_type." + opt),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && "new".equals(c.autoMaceSwapType) ? "new" : "old";
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
    }

    private void syncEngineConfig(ActivityConfig c) {
        if (c == null) return;
        boolean isNew = "new".equals(c.autoMaceSwapType);

        RedstoneOptimizerConfig.enabled = c.autoMaceEnabled && !isNew;
        RedstoneOptimizerConfig.restoreDelayMs = (int) c.autoMaceRestoreDelayMs;
        RedstoneOptimizerConfig.randomDelay = c.autoMaceRandomDelay;
        if (c.autoMaceRandomDelay) {
            RedstoneOptimizerConfig.randomMaxRestoreDelayMs = Math.max((int) c.autoMaceRestoreDelayMs + 30, 120);
        } else {
            RedstoneOptimizerConfig.randomMaxRestoreDelayMs = (int) c.autoMaceRestoreDelayMs;
        }
        RedstoneOptimizerConfig.legitMode = c.autoMaceLegitMode;
        RedstoneOptimizerConfig.missChance = (int) c.autoMaceMissChance;

        PrestigeAutoMaceConfig pc = PrestigeAutoMaceController.getInstance().getConfig();
        pc.enabled = c.autoMaceEnabled && isNew;
        pc.minFallDistance = c.autoMaceMinFallDistance;
        pc.autoSwitch = c.autoMaceAutoSwitch;
        pc.silentAim = c.autoMaceSilentAim;
        pc.silentAimRange = c.autoMaceSilentAimRange;
        pc.movementFix = c.autoMaceMovementFix;
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
            if (c != null && "new".equals(c.autoMaceSwapType)) {
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
            if (c != null && !"new".equals(c.autoMaceSwapType)) {
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

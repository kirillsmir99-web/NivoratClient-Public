package activity.client.module.impl.utility;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.ActivityScreen;
import activity.client.gui.component.ActivityButton;
import activity.client.gui.icon.ActivityIcon;
import activity.client.gui.layout.ScrollContainer;
import activity.client.gui.tab.ActivityTab;
import activity.client.gui.theme.ActivityMetrics;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.NivoratModule;
import activity.client.module.setting.Setting;
import activity.client.module.setting.SettingGroup;
import dev.hpreaper.HealthHudOverlay;
import dev.hpreaper.VitalityConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

import java.util.List;

public class HPReaperModule extends NivoratModule {
    public static final String ID = "hp_reaper";

    public HPReaperModule() {
        super(ID, Text.translatable("activity.module.hp_reaper.name"), Text.translatable("activity.module.hp_reaper.desc"), ModuleCategory.UTILITY);
        this.metadata = ModuleMetadata.builder(ID)
                .displayName(name)
                .description(description)
                .category(category)
                .author("Nivorat")
                .version("1.2.0")
                .icon(ActivityIcon.UTILITY)
                .keybind(keybind)
                .aliases("hpreaper", "reaper", "хп", "hp", "жнец hp", "жнец", "хпреапер", "здоровье", "индикатор", "цель", "target", "урон", "damage")
                .build();

        registerEnum("mode", Text.translatable("activity.setting.utility.hpreaper_mode"),
                Text.translatable("activity.setting.utility.hpreaper_mode.desc"), SettingGroup.GENERAL,
                List.of("target_hp", "own_hp", "damage_diff", "compact"), "target_hp",
                opt -> Text.translatable("activity.dropdown.hp_mode." + opt),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.hpReaperMode : "target_hp";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.hpReaperMode = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerEnum("target_filter", Text.translatable("activity.setting.utility.target_filter"),
                Text.translatable("activity.setting.utility.target_filter.desc"), SettingGroup.GENERAL,
                List.of("all_entities", "players_only", "hostile_and_players"), "all_entities",
                opt -> Text.translatable("activity.dropdown.target_filter." + opt),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.hpReaperTargetFilter : "all_entities";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.hpReaperTargetFilter = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("show_armor", Text.literal("Броня"), Text.literal("Показывать предметы брони выбранной цели"), SettingGroup.EXTRA, true,
                () -> ActivityConfigManager.getConfig().hpReaperShowArmor,
                value -> ActivityConfigManager.getConfig().hpReaperShowArmor = value);
        registerBoolean("show_difference", Text.literal("Разница HP"), Text.literal("Показывать разницу вашего здоровья и здоровья цели"), SettingGroup.EXTRA, true,
                () -> ActivityConfigManager.getConfig().hpReaperShowDifference,
                value -> ActivityConfigManager.getConfig().hpReaperShowDifference = value);
        registerBoolean("low_health_hearts", Text.literal("HP Focus"), Text.literal("Векторные сердца Nivorat при низком здоровье"), SettingGroup.EXTRA, true,
                () -> ActivityConfigManager.getConfig().hpReaperLowHealthHearts,
                value -> ActivityConfigManager.getConfig().hpReaperLowHealthHearts = value);
        registerInteger("low_health_threshold", Text.literal("Порог HP Focus"), Text.literal("Показывать сердца ниже этого количества HP"), SettingGroup.EXTRA, 1, 40, 1, activity.client.module.setting.NumberUnit.NONE, 8,
                () -> ActivityConfigManager.getConfig().hpReaperLowHealthThreshold,
                value -> ActivityConfigManager.getConfig().hpReaperLowHealthThreshold = value);
    }

    @Override
    public Setting<?> getSetting(String id) {
        if ("display_mode".equals(id)) {
            return super.getSetting("mode");
        }
        return super.getSetting(id);
    }

    @Override
    public boolean hasCustomSection() {
        return true;
    }

    @Override
    public int buildCustomSection(ActivityTab tab, ActivityScreen screen, ScrollContainer container,
                                  int startX, int startY, int innerRowW) {
        int btnGap = 4;
        int btnW = Math.max(50, (innerRowW - 2 * btnGap) / 3);

        ActivityButton btnResetMode = new ActivityButton(
                startX, startY, btnW, ActivityMetrics.CONTROL_HEIGHT,
                Text.translatable("activity.setting.utility.hpreaper_reset_mode"),
                b -> {
                    VitalityConfig.resetModePos(VitalityConfig.displayMode);
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        saveToConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        ActivityButton btnApplyAll = new ActivityButton(
                startX + btnW + btnGap, startY, btnW, ActivityMetrics.CONTROL_HEIGHT,
                Text.translatable("activity.setting.utility.hpreaper_apply_all"),
                b -> {
                    int x = VitalityConfig.getModeX(VitalityConfig.displayMode);
                    int y = VitalityConfig.getModeY(VitalityConfig.displayMode);
                    VitalityConfig.applyPosToAll(x, y);
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        saveToConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        ActivityButton btnResetAll = new ActivityButton(
                startX + (btnW + btnGap) * 2, startY, btnW, ActivityMetrics.CONTROL_HEIGHT,
                Text.translatable("activity.setting.utility.hpreaper_reset_all"),
                ActivityButton.Variant.DANGER,
                b -> {
                    VitalityConfig.resetAll();
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        saveToConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        if (tab != null) {
            tab.addControl(container, btnResetMode);
            tab.addControl(container, btnApplyAll);
            tab.addControl(container, btnResetAll);
        } else if (container != null) {
            container.addChild(btnResetMode);
            container.addChild(btnApplyAll);
            container.addChild(btnResetAll);
        }

        return ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
    }

    public void syncControllerConfig(ActivityConfig c) {
        if (c == null) return;
        if (!c.hpReaperEnabled) {
            VitalityConfig.displayMode = HealthHudOverlay.DisplayMode.DISABLED;
        } else {
            switch (c.hpReaperMode) {
                case "own_hp" -> VitalityConfig.displayMode = HealthHudOverlay.DisplayMode.OWN_HEALTH;
                case "target_hp" -> VitalityConfig.displayMode = HealthHudOverlay.DisplayMode.TARGET_HEALTH;
                case "damage_diff" -> VitalityConfig.displayMode = HealthHudOverlay.DisplayMode.OWN_TARGET_AND_DIFFERENCE;
                case "compact" -> VitalityConfig.displayMode = HealthHudOverlay.DisplayMode.CROSSHAIR_AND_TARGET;
                default -> VitalityConfig.displayMode = HealthHudOverlay.DisplayMode.TARGET_HEALTH;
            }
        }

        switch (c.hpReaperTargetFilter) {
            case "players_only" -> VitalityConfig.targetFilter = VitalityConfig.TargetFilter.PLAYERS_ONLY;
            case "hostile_and_players" -> VitalityConfig.targetFilter = VitalityConfig.TargetFilter.HOSTILE_AND_PLAYERS;
            default -> VitalityConfig.targetFilter = VitalityConfig.TargetFilter.ALL_ENTITIES;
        }

        VitalityConfig.ownHealthX = c.hpReaperOwnHealthX;
        VitalityConfig.ownHealthY = c.hpReaperOwnHealthY;
        VitalityConfig.crosshairTargetX = c.hpReaperCrosshairTargetX;
        VitalityConfig.crosshairTargetY = c.hpReaperCrosshairTargetY;
        VitalityConfig.targetHealthX = c.hpReaperTargetHealthX;
        VitalityConfig.targetHealthY = c.hpReaperTargetHealthY;
        VitalityConfig.diffX = c.hpReaperDiffX;
        VitalityConfig.diffY = c.hpReaperDiffY;
    }

    @Override
    public void onInitialize() {
        VitalityConfig.load();
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            syncControllerConfig(c);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            c.hpReaperEnabled = enabled;
            ActivityConfigManager.markDirty();
            syncControllerConfig(c);
        }
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (isEnabled()) {
            HealthHudOverlay.updateTick(client);
            if (client.player != null && client.player.hurtTime > 0) {
                Entity attacker = client.player.getAttacker();
                if (attacker == null) {
                    attacker = client.player.getLastAttacker();
                }
                LivingEntity living = HealthHudOverlay.resolveLivingEntity(attacker);
                if (HealthHudOverlay.isValidTarget(client.player, living)) {
                    HealthHudOverlay.trackCombatTarget(living);
                }
            }
        }
    }

    @Override
    public ActionResult onAttackEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        if (isEnabled() && world.isClient()) {
            LivingEntity living = HealthHudOverlay.resolveLivingEntity(entity);
            if (HealthHudOverlay.isValidTarget(player, living)) {
                HealthHudOverlay.trackCombatTarget(living);
            }
        }
        return ActionResult.PASS;
    }

    @Override
    public void onRenderHud(DrawContext context, RenderTickCounter tickCounter) {
        if (isEnabled()) {
            HealthHudOverlay.render(context, tickCounter);
        }
    }

    @Override
    public void loadFromConfig(ActivityConfig config) {
        if (config == null) return;
        this.enabled = config.hpReaperEnabled;
        this.keybind.copyFrom(config.hpReaperKeybind);
        syncControllerConfig(config);
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        config.hpReaperEnabled = this.enabled;
        config.hpReaperKeybind.copyFrom(this.keybind);
        config.hpReaperOwnHealthX = VitalityConfig.ownHealthX;
        config.hpReaperOwnHealthY = VitalityConfig.ownHealthY;
        config.hpReaperCrosshairTargetX = VitalityConfig.crosshairTargetX;
        config.hpReaperCrosshairTargetY = VitalityConfig.crosshairTargetY;
        config.hpReaperTargetHealthX = VitalityConfig.targetHealthX;
        config.hpReaperTargetHealthY = VitalityConfig.targetHealthY;
        config.hpReaperDiffX = VitalityConfig.diffX;
        config.hpReaperDiffY = VitalityConfig.diffY;
        syncControllerConfig(config);
    }
}

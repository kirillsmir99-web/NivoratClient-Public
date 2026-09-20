package activity.client.module.impl.defense;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.icon.ActivityIcon;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.NivoratModule;
import activity.client.module.setting.SettingGroup;
import dev.luminance.AnchorConfig;
import dev.luminance.AnchorController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;

public class AutoAnchorModule extends NivoratModule {
    public static final String ID = "auto_anchor";
    private final AnchorController controller = new AnchorController();

    public AutoAnchorModule() {
        super(ID, Text.translatable("activity.module.auto_anchor.name"), Text.translatable("activity.module.auto_anchor.desc"), ModuleCategory.DEFENSE);
        this.metadata = ModuleMetadata.builder(ID)
                .displayName(name)
                .description(description)
                .category(category)
                .author("Nivorat")
                .version("1.0.0")
                .icon(ActivityIcon.DEFENSE)
                .keybind(keybind)
                .aliases("autoanchor", "anchor", "якорь", "автоякорь", "авто-якорь", "взрыв якоря", "взрыв якорей", "незер", "взрыв", "задержка", "delay", "заряд", "charge", "chance", "шанс", "подрыв якоря")
                .build();

        // 1. GENERAL
        registerEnum("preset", Text.translatable("activity.setting.defense.anchor_preset"),
                Text.translatable("activity.setting.defense.anchor_preset.desc"), SettingGroup.GENERAL,
                List.of("fast", "medium", "balanced", "safe"), "balanced",
                opt -> Text.translatable("activity.dropdown.anchor_preset." + opt),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoAnchorPreset : "balanced";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoAnchorPreset = val;
                        double chargeD, explodeD, chance;
                        if ("fast".equalsIgnoreCase(val)) {
                            chargeD = 0.0;
                            explodeD = 0.0;
                            chance = 100.0;
                        } else if ("medium".equalsIgnoreCase(val)) {
                            chargeD = 1.0;
                            explodeD = 1.0;
                            chance = 95.0;
                        } else if ("balanced".equalsIgnoreCase(val)) {
                            chargeD = 1.0;
                            explodeD = 1.0;
                            chance = 90.0;
                        } else if ("safe".equalsIgnoreCase(val)) {
                            chargeD = 2.0;
                            explodeD = 2.0;
                            chance = 85.0;
                        } else {
                            chargeD = 1.0;
                            explodeD = 1.0;
                            chance = 90.0;
                        }
                        c.autoAnchorChargeDelay = chargeD;
                        c.autoAnchorExplodeDelay = explodeD;
                        c.autoAnchorChance = chance;

                        updateNumberSetting("charge_delay", chargeD);
                        updateNumberSetting("explode_delay", explodeD);
                        updateNumberSetting("chance", chance);

                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("auto_explode", Text.translatable("activity.setting.defense.auto_explode"),
                Text.translatable("activity.setting.defense.auto_explode.desc"), SettingGroup.GENERAL,
                false,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoAnchorAutoExplode;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoAnchorAutoExplode = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("auto_return", Text.translatable("activity.setting.defense.auto_return"),
                Text.translatable("activity.setting.defense.auto_return.desc"), SettingGroup.GENERAL,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoAnchorAutoReturn;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoAnchorAutoReturn = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        // 2. BEHAVIOR
        registerNumber("charge_delay", Text.translatable("activity.setting.defense.charge_delay"),
                Text.translatable("activity.setting.defense.charge_delay.desc"), SettingGroup.BEHAVIOR,
                0.0, 10.0, 1.0, " t", true, 1.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoAnchorChargeDelay : 1.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoAnchorChargeDelay = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("explode_delay", Text.translatable("activity.setting.defense.explode_delay"),
                Text.translatable("activity.setting.defense.explode_delay.desc"), SettingGroup.BEHAVIOR,
                0.0, 10.0, 1.0, " t", true, 1.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoAnchorExplodeDelay : 1.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoAnchorExplodeDelay = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("chance", Text.translatable("activity.setting.combat.chance_label"),
                Text.translatable("activity.setting.combat.chance_label.desc"), SettingGroup.BEHAVIOR,
                10.0, 100.0, 5.0, "%", true, 85.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoAnchorChance : 85.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoAnchorChance = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("target_charges", Text.translatable("activity.setting.defense.target_charges"),
                Text.translatable("activity.setting.defense.target_charges.desc"), SettingGroup.BEHAVIOR,
                1.0, 4.0, 1.0, " c", true, 1.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoAnchorTargetCharges : 1.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoAnchorTargetCharges = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        // 3. ADVANCED
        registerBoolean("legit_mode", Text.translatable("activity.setting.combat.legit_mode"),
                Text.translatable("activity.setting.combat.legit_mode.desc"), SettingGroup.ADVANCED,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoAnchorLegitMode;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoAnchorLegitMode = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );
    }

    private void syncControllerConfig(ActivityConfig c) {
        if (c == null) return;
        AnchorConfig.enabled = this.enabled;
        AnchorConfig.autoExplode = c.autoAnchorAutoExplode;
        AnchorConfig.autoReturn = c.autoAnchorAutoReturn;
        AnchorConfig.chargeDelayTicks = (int) Math.round(c.autoAnchorChargeDelay);
        AnchorConfig.explodeDelayTicks = (int) Math.round(c.autoAnchorExplodeDelay);
        AnchorConfig.chance = (int) c.autoAnchorChance;
        AnchorConfig.targetCharges = (int) Math.round(c.autoAnchorTargetCharges);
        AnchorConfig.legitMode = c.autoAnchorLegitMode;
        AnchorConfig.preset = c.autoAnchorPreset != null ? c.autoAnchorPreset.toUpperCase(Locale.ROOT) : "BALANCED";
    }

    public AnchorController getController() {
        return controller;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            c.autoAnchorEnabled = enabled;
            ActivityConfigManager.markDirty();
        }
        if (controller.isEnabled() != enabled) {
            controller.toggle();
        }
        AnchorConfig.enabled = enabled;
    }

    @Override
    public void onDisable() {
        net.fabricmc.pack.api.CombatLockManager.setLock("pvp.anchor_active", false);
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (isEnabled()) {
            controller.tick(client);
        }
    }

    @Override
    public void loadFromConfig(ActivityConfig config) {
        if (config == null) return;
        this.enabled = config.autoAnchorEnabled;
        this.keybind.copyFrom(config.autoAnchorKeybind);
        if (controller.isEnabled() != this.enabled) {
            controller.toggle();
        }
        syncControllerConfig(config);
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        config.autoAnchorEnabled = this.enabled;
        config.autoAnchorKeybind.copyFrom(this.keybind);
        syncControllerConfig(config);
    }

    private void updateNumberSetting(String id, double val) {
        activity.client.module.setting.Setting<?> s = getSetting(id);
        if (s instanceof activity.client.module.setting.NumberSetting ns) {
            ns.set(val);
        }
    }

    private void updateBooleanSetting(String id, boolean val) {
        activity.client.module.setting.Setting<?> s = getSetting(id);
        if (s instanceof activity.client.module.setting.BooleanSetting bs) {
            bs.set(val);
        }
    }
}

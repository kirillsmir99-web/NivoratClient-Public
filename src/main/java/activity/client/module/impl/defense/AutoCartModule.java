package activity.client.module.impl.defense;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.icon.ActivityIcon;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.NivoratModule;
import activity.client.module.setting.SettingGroup;
import dev.virion.arc.MorrowConfig;
import dev.virion.arc.VirionArcController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.List;

public class AutoCartModule extends NivoratModule {
    public static final String ID = "auto_cart";
    private final VirionArcController controller = new VirionArcController();

    public AutoCartModule() {
        super(ID, Text.translatable("activity.module.auto_cart.name"), Text.translatable("activity.module.auto_cart.desc"), ModuleCategory.DEFENSE);
        this.keybind.set(org.lwjgl.glfw.GLFW.GLFW_KEY_I, true, true, false);
        this.metadata = ModuleMetadata.builder(ID)
                .displayName(name)
                .description(description)
                .category(category)
                .author("Nivorat")
                .version("2.1.2")
                .icon(ActivityIcon.DEFENSE)
                .keybind(keybind)
                .aliases("autocart", "cart", "вагонетка", "вагонетки", "автовагонетка", "авто-вагонетка", "автокарт", "авто-карт", "подрыв вагонеток", "подрыв", "тнт", "tnt", "delay", "задержка", "дистанция", "distance", "яма", "pit", "рельсы", "rails")
                .build();

        // 1. GENERAL
        registerEnum("preset", Text.translatable("activity.setting.defense.cart_preset"),
                Text.translatable("activity.setting.defense.cart_preset.desc"), SettingGroup.GENERAL,
                List.of("fast", "medium", "safe"), "medium",
                opt -> Text.translatable("activity.dropdown.cart_preset." + opt),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoCartPreset : "medium";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoCartPreset = val;
                        double minD, maxD, chance, maxDist;
                        boolean pit, legit;
                        if ("fast".equalsIgnoreCase(val)) {
                            minD = 40.0;
                            maxD = 60.0;
                            chance = 100.0;
                            maxDist = 4.5;
                            pit = true;
                            legit = false;
                        } else if ("safe".equalsIgnoreCase(val)) {
                            minD = 120.0;
                            maxD = 180.0;
                            chance = 100.0;
                            maxDist = 4.2;
                            pit = true;
                            legit = true;
                        } else {
                            minD = 70.0;
                            maxD = 110.0;
                            chance = 100.0;
                            maxDist = 4.4;
                            pit = true;
                            legit = true;
                        }
                        c.autoCartMinDelayMs = minD;
                        c.autoCartMaxDelayMs = maxD;
                        c.autoCartPlacementChance = chance;
                        c.autoCartMaxDistance = maxDist;
                        c.autoCartAllowPitPlacement = pit;
                        c.autoCartRandomDelay = true;
                        c.autoCartLegitMode = legit;

                        updateNumberSetting("min_delay", minD);
                        updateNumberSetting("max_delay", maxD);
                        updateNumberSetting("placement_chance", chance);
                        updateNumberSetting("max_distance", maxDist);
                        updateBooleanSetting("allow_pit_placement", pit);
                        updateBooleanSetting("legit_mode", legit);

                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        // 2. BEHAVIOR
        registerNumber("placement_chance", Text.translatable("activity.setting.defense.cart_chance"),
                Text.translatable("activity.setting.defense.cart_chance.desc"), SettingGroup.BEHAVIOR,
                0.0, 100.0, 5.0, "%", true, 100.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoCartPlacementChance : 100.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoCartPlacementChance = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("max_distance", Text.translatable("activity.setting.defense.max_distance"),
                Text.translatable("activity.setting.defense.max_distance.desc"), SettingGroup.BEHAVIOR,
                1.5, 4.5, 0.1, " m", false, 4.4,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoCartMaxDistance : 4.4;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoCartMaxDistance = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("min_delay", Text.translatable("activity.setting.defense.min_delay"),
                Text.translatable("activity.setting.defense.min_delay.desc"), SettingGroup.BEHAVIOR,
                10.0, 200.0, 5.0, " ms", true, 70.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoCartMinDelayMs : 70.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoCartMinDelayMs = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("max_delay", Text.translatable("activity.setting.defense.max_delay"),
                Text.translatable("activity.setting.defense.max_delay.desc"), SettingGroup.BEHAVIOR,
                10.0, 300.0, 5.0, " ms", true, 110.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoCartMaxDelayMs : 110.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoCartMaxDelayMs = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("random_delay", Text.translatable("activity.setting.defense.random_delay"),
                Text.translatable("activity.setting.defense.random_delay.desc"), SettingGroup.BEHAVIOR,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoCartRandomDelay;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoCartRandomDelay = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        // 3. EXTRA
        registerBoolean("allow_self_cart", Text.translatable("activity.setting.defense.allow_self_cart"),
                Text.translatable("activity.setting.defense.allow_self_cart.desc"), SettingGroup.EXTRA,
                false,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoCartAllowSelfCart;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoCartAllowSelfCart = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("allow_pit_placement", Text.translatable("activity.setting.defense.allow_pit_placement"),
                Text.translatable("activity.setting.defense.allow_pit_placement.desc"), SettingGroup.EXTRA,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoCartAllowPitPlacement;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoCartAllowPitPlacement = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("use_mainhand_cart", Text.translatable("activity.setting.defense.use_mainhand_cart"),
                Text.translatable("activity.setting.defense.use_mainhand_cart.desc"), SettingGroup.EXTRA,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoCartUseMainHand;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoCartUseMainHand = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        // 4. ADVANCED
        registerBoolean("legit_mode", Text.translatable("activity.setting.combat.legit_mode"),
                Text.translatable("activity.setting.combat.legit_mode.desc"), SettingGroup.ADVANCED,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoCartLegitMode;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoCartLegitMode = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );
    }

    private void syncControllerConfig(ActivityConfig c) {
        if (c == null) return;
        MorrowConfig.placementChance = (int) Math.round(c.autoCartPlacementChance);
        MorrowConfig.maxDistance = c.autoCartMaxDistance;
        MorrowConfig.minDelayMs = (int) Math.round(c.autoCartMinDelayMs);
        MorrowConfig.maxDelayMs = (int) Math.round(c.autoCartMaxDelayMs);
        MorrowConfig.allowSelfCart = c.autoCartAllowSelfCart;
        MorrowConfig.allowPitPlacement = c.autoCartAllowPitPlacement;
        MorrowConfig.useMainhandCart = c.autoCartUseMainHand;
        MorrowConfig.randomDelay = c.autoCartRandomDelay;
        MorrowConfig.legitMode = c.autoCartLegitMode;

        if ("safe".equals(c.autoCartPreset)) {
            MorrowConfig.preset = MorrowConfig.PRESET_SAFE;
        } else if ("medium".equals(c.autoCartPreset)) {
            MorrowConfig.preset = MorrowConfig.PRESET_MEDIUM;
        } else {
            MorrowConfig.preset = MorrowConfig.PRESET_FAST;
        }
    }

    public VirionArcController getController() {
        return controller;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            c.autoCartEnabled = enabled;
            ActivityConfigManager.markDirty();
        }
        if (controller.isEnabled() != enabled) {
            controller.toggle();
        }
    }

    @Override
    public void onDisable() {
        net.fabricmc.pack.api.CombatLockManager.setLock("pvp.cart_placement_active", false);
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
        this.enabled = config.autoCartEnabled;
        this.keybind.copyFrom(config.autoCartKeybind);
        if (controller.isEnabled() != this.enabled) {
            controller.toggle();
        }
        syncControllerConfig(config);
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        config.autoCartEnabled = this.enabled;
        config.autoCartKeybind.copyFrom(this.keybind);
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

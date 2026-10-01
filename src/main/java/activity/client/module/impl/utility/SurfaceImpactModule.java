package activity.client.module.impl.utility;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.icon.ActivityIcon;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.NivoratModule;
import activity.client.module.setting.Setting;
import activity.client.module.setting.SettingGroup;
import dev.impact.SurfaceImpactConfig;
import dev.impact.SurfaceImpactController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.List;

public class SurfaceImpactModule extends NivoratModule {
    public static final String ID = "water_drop";
    private final SurfaceImpactController controller = new SurfaceImpactController();

    public SurfaceImpactModule() {
        super(ID, Text.translatable("activity.module.water_drop.name"), Text.translatable("activity.module.water_drop.desc"), ModuleCategory.UTILITY);
        this.metadata = ModuleMetadata.builder(ID)
                .displayName(name)
                .description(description)
                .category(category)
                .author("kt1xW")
                .version("1.0.0")
                .icon(ActivityIcon.UTILITY)
                .keybind(keybind)
                .aliases("waterdrop", "water_drop", "water", "вода", "дроп", "водный дроп", "mlg", "млг", "ведро", "bucket", "падение", "fall", "виндчардж", "windcharge", "снег", "паутина", "сноп", "save", "сейв")
                .build();

        registerEnum("mode", Text.translatable("activity.setting.utility.water_drop_mode"),
                Text.translatable("activity.setting.utility.water_drop_mode.desc"), SettingGroup.GENERAL,
                List.of("hotbar", "inventory"), "hotbar",
                opt -> Text.translatable("activity.dropdown.water_drop_mode." + opt),
                opt -> Text.translatable("activity.dropdown.water_drop_mode." + opt + ".desc"),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.waterDropMode : "hotbar";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.waterDropMode = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("fall_threshold", Text.translatable("activity.setting.utility.fall_threshold"),
                Text.translatable("activity.setting.utility.fall_threshold.desc"), SettingGroup.GENERAL,
                3.0, 20.0, 1.0, " бл", true, 4.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.waterDropFallThreshold : 4.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.waterDropFallThreshold = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("pickup_water", Text.translatable("activity.setting.utility.pickup_water"),
                Text.translatable("activity.setting.utility.pickup_water.desc"), SettingGroup.GENERAL,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c == null || c.waterDropPickupWater;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.waterDropPickupWater = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("switch_back", Text.translatable("activity.setting.utility.switch_back"),
                Text.translatable("activity.setting.utility.switch_back.desc"), SettingGroup.GENERAL,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c == null || c.waterDropSwitchBack;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.waterDropSwitchBack = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerEnum("camera_mode", Text.translatable("activity.setting.utility.water_drop_camera_mode"),
                Text.translatable("activity.setting.utility.water_drop_camera_mode.desc"), SettingGroup.BEHAVIOR,
                List.of("off", "auto", "packet"), "off",
                opt -> Text.translatable("activity.dropdown.water_drop_camera_mode." + opt),
                opt -> Text.translatable("activity.dropdown.water_drop_camera_mode." + opt + ".desc"),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.waterDropCameraMode : "off";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.waterDropCameraMode = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("pitch_threshold", Text.translatable("activity.setting.utility.pitch_threshold"),
                Text.translatable("activity.setting.utility.pitch_threshold.desc"), SettingGroup.BEHAVIOR,
                30.0, 90.0, 5.0, "°", true, 45.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.waterDropPitchThreshold : 45.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.waterDropPitchThreshold = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("pickup_delay", Text.translatable("activity.setting.utility.pickup_delay"),
                Text.translatable("activity.setting.utility.pickup_delay.desc"), SettingGroup.BEHAVIOR,
                30.0, 300.0, 5.0, " ms", true, 85.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.waterDropPickupDelayMs : 85.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.waterDropPickupDelayMs = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("switch_delay", Text.translatable("activity.setting.utility.switch_delay"),
                Text.translatable("activity.setting.utility.switch_delay.desc"), SettingGroup.BEHAVIOR,
                30.0, 300.0, 5.0, " ms", true, 130.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.waterDropSwitchDelayMs : 130.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.waterDropSwitchDelayMs = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("random_delay", Text.translatable("activity.setting.utility.random_delay"),
                Text.translatable("activity.setting.utility.random_delay.desc"), SettingGroup.BEHAVIOR,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c == null || c.waterDropRandomDelay;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.waterDropRandomDelay = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerEnum("target_slot", Text.translatable("activity.setting.utility.target_slot"),
                Text.translatable("activity.setting.utility.target_slot.desc"), SettingGroup.EXTRA,
                List.of("1", "2", "3", "4", "5", "6", "7", "8", "9"), "9",
                opt -> Text.translatable("activity.dropdown.target_slot.slot", opt),
                opt -> Text.translatable("activity.dropdown.target_slot.slot.desc", opt),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.waterDropTargetSlot != null ? c.waterDropTargetSlot : "9";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.waterDropTargetSlot = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("combat_guard", Text.translatable("activity.setting.utility.water_drop_combat_guard"),
                Text.translatable("activity.setting.utility.water_drop_combat_guard.desc"), SettingGroup.EXTRA,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c == null || c.waterDropCombatGuard;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.waterDropCombatGuard = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("pearl_guard", Text.translatable("activity.setting.utility.pearl_guard"),
                Text.translatable("activity.setting.utility.pearl_guard.desc"), SettingGroup.EXTRA,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c == null || c.waterDropPearlGuard;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.waterDropPearlGuard = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("nether_adapter", Text.translatable("activity.setting.utility.nether_adapter"),
                Text.translatable("activity.setting.utility.nether_adapter.desc"), SettingGroup.EXTRA,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c == null || c.waterDropNetherAdapter;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.waterDropNetherAdapter = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("enable_water", Text.translatable("activity.setting.utility.enable_water"),
                Text.translatable("activity.setting.utility.enable_water.desc"), SettingGroup.ADVANCED,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c == null || c.waterDropEnableWater;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.waterDropEnableWater = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("enable_wind_charge", Text.translatable("activity.setting.utility.enable_wind_charge"),
                Text.translatable("activity.setting.utility.enable_wind_charge.desc"), SettingGroup.ADVANCED,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c == null || c.waterDropEnableWindCharge;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.waterDropEnableWindCharge = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("enable_hay_block", Text.translatable("activity.setting.utility.enable_hay_block"),
                Text.translatable("activity.setting.utility.enable_hay_block.desc"), SettingGroup.ADVANCED,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c == null || c.waterDropEnableHayBlock;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.waterDropEnableHayBlock = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("enable_slime_block", Text.translatable("activity.setting.utility.enable_slime_block"),
                Text.translatable("activity.setting.utility.enable_slime_block.desc"), SettingGroup.ADVANCED,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c == null || c.waterDropEnableSlimeBlock;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.waterDropEnableSlimeBlock = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("enable_cobweb", Text.translatable("activity.setting.utility.enable_cobweb"),
                Text.translatable("activity.setting.utility.enable_cobweb.desc"), SettingGroup.ADVANCED,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c == null || c.waterDropEnableCobweb;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.waterDropEnableCobweb = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("enable_powder_snow", Text.translatable("activity.setting.utility.enable_powder_snow"),
                Text.translatable("activity.setting.utility.enable_powder_snow.desc"), SettingGroup.ADVANCED,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c == null || c.waterDropEnablePowderSnow;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.waterDropEnablePowderSnow = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );
    }

    public void syncControllerConfig(ActivityConfig c) {
        syncControllerConfig(c, this.enabled);
    }

    public static void syncControllerConfig(ActivityConfig c, boolean enabled) {
        if (c == null) return;
        SurfaceImpactConfig.enabled = enabled;
        SurfaceImpactConfig.mode = "inventory".equalsIgnoreCase(c.waterDropMode) ? 1 : 0;
        SurfaceImpactConfig.fallThreshold = (int) Math.round(c.waterDropFallThreshold);
        SurfaceImpactConfig.pickupWater = c.waterDropPickupWater;
        SurfaceImpactConfig.switchBack = c.waterDropSwitchBack;
        SurfaceImpactConfig.cameraMode = c.waterDropCameraMode != null ? c.waterDropCameraMode : "off";
        SurfaceImpactConfig.pitchThreshold = (float) c.waterDropPitchThreshold;
        SurfaceImpactConfig.pickupDelayMs = c.waterDropPickupDelayMs;
        SurfaceImpactConfig.switchDelayMs = c.waterDropSwitchDelayMs;
        SurfaceImpactConfig.randomDelay = c.waterDropRandomDelay;
        SurfaceImpactConfig.targetHotbarSlot = parseTargetSlot(c.waterDropTargetSlot);
        SurfaceImpactConfig.combatGuard = c.waterDropCombatGuard;
        SurfaceImpactConfig.pearlGuard = c.waterDropPearlGuard;
        SurfaceImpactConfig.netherAdapter = c.waterDropNetherAdapter;
        SurfaceImpactConfig.enableWater = c.waterDropEnableWater;
        SurfaceImpactConfig.enableWindCharge = c.waterDropEnableWindCharge;
        SurfaceImpactConfig.enableHayBlock = c.waterDropEnableHayBlock;
        SurfaceImpactConfig.enableSlimeBlock = c.waterDropEnableSlimeBlock;
        SurfaceImpactConfig.enableCobweb = c.waterDropEnableCobweb;
        SurfaceImpactConfig.enablePowderSnow = c.waterDropEnablePowderSnow;
    }

    private static int parseTargetSlot(String slotStr) {
        if (slotStr == null) return 9;
        try {
            int val = Integer.parseInt(slotStr.trim());
            return Math.max(1, Math.min(9, val));
        } catch (Exception ignored) {
            return 9;
        }
    }

    public SurfaceImpactController getController() {
        return controller;
    }

    @Override
    public boolean hasTickLogic() {
        return true;
    }

    @Override
    public Setting<?> getSetting(String id) {
        if ("target_hotbar_slot".equals(id)) {
            return super.getSetting("target_slot");
        }
        if ("min_fall_height".equals(id) || "fall_distance".equals(id)) {
            return super.getSetting("fall_threshold");
        }
        if ("auto_pickup".equals(id)) {
            return super.getSetting("pickup_water");
        }
        if ("auto_switch_back".equals(id)) {
            return super.getSetting("switch_back");
        }
        return super.getSetting(id);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            c.waterDropEnabled = enabled;
            ActivityConfigManager.markDirty();
        }
        SurfaceImpactConfig.enabled = enabled;
        if (!enabled) {
            controller.reset();
        }
    }

    @Override
    public void onDisable() {
        controller.reset();
    }

    @Override
    public void onCleanupTick(MinecraftClient client) { controller.cleanup(); }

    @Override
    public void onTick(MinecraftClient client) {
        if (isEnabled()) {
            controller.tick(client);
        }
    }

    @Override
    public void loadFromConfig(ActivityConfig config) {
        if (config == null) return;
        this.enabled = config.waterDropEnabled;
        this.keybind.copyFrom(config.waterDropKeybind);
        syncControllerConfig(config);
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        config.waterDropEnabled = this.enabled;
        config.waterDropKeybind.copyFrom(this.keybind);
        syncControllerConfig(config);
    }
}

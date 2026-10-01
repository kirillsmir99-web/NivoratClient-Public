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
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

import java.util.List;

public class OcclusionCacheModule extends NivoratModule {
    public static final String ID = "auto_cart";
    private final VirionArcController controller = new VirionArcController();
    private boolean resetCalibrationPending = false;

    public OcclusionCacheModule() {
        super(ID, Text.translatable("activity.module.auto_cart.name"), Text.translatable("activity.module.auto_cart.desc"), ModuleCategory.DEFENSE);
        this.keybind.set(org.lwjgl.glfw.GLFW.GLFW_KEY_I, true, true, false);
        this.metadata = ModuleMetadata.builder(ID)
                .displayName(name)
                .description(description)
                .category(category)
                .author("kt1xW")
                .version("2.1.2")
                .icon(ActivityIcon.DEFENSE)
                .keybind(keybind)
                .aliases("autocart", "cart", "вагонетка", "вагонетки", "автовагонетка", "авто-вагонетка", "автокарт", "авто-карт", "подрыв вагонеток", "подрыв", "тнт", "tnt", "delay", "задержка", "дистанция", "distance", "яма", "pit", "рельсы", "rails", "камера", "camera", "автокамера", "плавная камера")
                .build();

        registerEnum("preset", Text.translatable("activity.setting.defense.cart_preset"),
                Text.translatable("activity.setting.defense.cart_preset.desc"), SettingGroup.GENERAL,
                List.of("fast", "medium", "safe", "learned", "custom"), "medium",
                opt -> Text.translatable("activity.dropdown.cart_preset." + opt),
                opt -> Text.translatable("activity.dropdown.cart_preset." + opt + ".desc"),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoCartPreset : "medium";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoCartPreset = val;
                        if ("custom".equalsIgnoreCase(val)) {
                            syncControllerConfig(c);
                            ActivityConfigManager.markDirty();
                            return;
                        }
                        double minD, maxD, chance, maxDist;
                        boolean pit, legit;
                        String camMode;
                        double camSmooth, camRetSmooth, camCurve, camRand;
                        boolean camRet = true, camGcd = true;
                        if ("fast".equalsIgnoreCase(val)) {
                            minD = 35.0;
                            maxD = 50.0;
                            chance = 100.0;
                            maxDist = 4.5;
                            pit = true;
                            legit = false;
                            camMode = "packet";
                            camSmooth = 55.0;
                            camRetSmooth = 55.0;
                            camCurve = 20.0;
                            camRand = 20.0;
                        } else if ("safe".equalsIgnoreCase(val)) {
                            minD = 70.0;
                            maxD = 100.0;
                            chance = 100.0;
                            maxDist = 4.2;
                            pit = true;
                            legit = true;
                            camMode = "auto";
                            camSmooth = 160.0;
                            camRetSmooth = 150.0;
                            camCurve = 45.0;
                            camRand = 40.0;
                        } else if ("learned".equalsIgnoreCase(val)) {
                            dev.virion.arc.ArcNeuralMotorProfile prof = dev.virion.arc.ArcNeuralMotorProfile.getInstance();
                            minD = prof.getLearnedMinDelayMs();
                            maxD = prof.getLearnedMaxDelayMs();
                            chance = 100.0;
                            maxDist = 4.4;
                            pit = true;
                            legit = true;
                            camMode = "auto";
                            camSmooth = prof.getLearnedCameraSmoothness();
                            camRetSmooth = Math.max(35.0, prof.getLearnedCameraSmoothness() - 10.0);
                            camCurve = Math.round(prof.getCurvatureBias() * 100.0f);
                            camRand = Math.round(prof.getTremorVolatility() * 500.0f);
                        } else {
                            minD = 50.0;
                            maxD = 80.0;
                            chance = 100.0;
                            maxDist = 4.4;
                            pit = true;
                            legit = true;
                            camMode = "auto";
                            camSmooth = 110.0;
                            camRetSmooth = 100.0;
                            camCurve = 40.0;
                            camRand = 35.0;
                        }
                        c.autoCartMinDelayMs = minD;
                        c.autoCartMaxDelayMs = maxD;
                        c.autoCartPlacementChance = chance;
                        c.autoCartMaxDistance = maxDist;
                        c.autoCartAllowPitPlacement = pit;
                        c.autoCartRandomDelay = true;
                        c.autoCartLegitMode = legit;
                        c.autoCartCameraMode = camMode;
                        c.autoCartAutoCamera = "auto".equalsIgnoreCase(camMode);
                        c.autoCartCameraSmoothness = camSmooth;
                        c.autoCartCameraReturn = camRet;
                        c.autoCartCameraReturnSmoothness = camRetSmooth;
                        c.autoCartCameraCurve = camCurve;
                        c.autoCartCameraRandomness = camRand;
                        c.autoCartCameraMouseGcd = camGcd;

                        updateNumberSetting("min_delay", minD);
                        updateNumberSetting("max_delay", maxD);
                        updateNumberSetting("placement_chance", chance);
                        updateNumberSetting("max_distance", maxDist);
                        updateBooleanSetting("allow_pit_placement", pit);
                        updateBooleanSetting("legit_mode", legit);
                        updateEnumSetting("camera_mode", camMode);
                        updateNumberSetting("camera_smoothness", camSmooth);
                        updateBooleanSetting("camera_return", camRet);
                        updateNumberSetting("camera_return_smoothness", camRetSmooth);
                        updateNumberSetting("camera_curve", camCurve);
                        updateNumberSetting("camera_randomness", camRand);
                        updateBooleanSetting("camera_mouse_gcd", camGcd);

                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

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
                10.0, 200.0, 5.0, " ms", true, 50.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoCartMinDelayMs : 50.0;
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
                10.0, 300.0, 5.0, " ms", true, 80.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoCartMaxDelayMs : 80.0;
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

        registerEnum("camera_mode", Text.translatable("activity.setting.defense.cart_camera_mode"),
                Text.translatable("activity.setting.defense.cart_camera_mode.desc"), SettingGroup.BEHAVIOR,
                List.of("off", "packet", "auto", "assisted"), "auto",
                opt -> Text.translatable("activity.dropdown.cart_camera_mode." + opt),
                opt -> Text.translatable("activity.dropdown.cart_camera_mode." + opt + ".desc"),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoCartCameraMode : "auto";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoCartCameraMode = val;
                        c.autoCartAutoCamera = "auto".equalsIgnoreCase(val) || "assisted".equalsIgnoreCase(val);
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("camera_smoothness", Text.translatable("activity.setting.defense.cart_camera_smoothness"),
                Text.translatable("activity.setting.defense.cart_camera_smoothness.desc"), SettingGroup.BEHAVIOR,
                35.0, 300.0, 5.0, " ms", true, 110.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoCartCameraSmoothness : 110.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoCartCameraSmoothness = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("camera_return", Text.translatable("activity.setting.defense.cart_camera_return"),
                Text.translatable("activity.setting.defense.cart_camera_return.desc"), SettingGroup.BEHAVIOR,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoCartCameraReturn;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoCartCameraReturn = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("camera_return_smoothness", Text.translatable("activity.setting.defense.cart_camera_return_smoothness"),
                Text.translatable("activity.setting.defense.cart_camera_return_smoothness.desc"), SettingGroup.BEHAVIOR,
                35.0, 300.0, 5.0, " ms", true, 100.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoCartCameraReturnSmoothness : 100.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoCartCameraReturnSmoothness = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("camera_curve", Text.translatable("activity.setting.defense.cart_camera_curve"),
                Text.translatable("activity.setting.defense.cart_camera_curve.desc"), SettingGroup.BEHAVIOR,
                0.0, 100.0, 5.0, "%", true, 40.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoCartCameraCurve : 40.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoCartCameraCurve = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("camera_randomness", Text.translatable("activity.setting.defense.cart_camera_randomness"),
                Text.translatable("activity.setting.defense.cart_camera_randomness.desc"), SettingGroup.BEHAVIOR,
                0.0, 100.0, 5.0, "%", true, 35.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoCartCameraRandomness : 35.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoCartCameraRandomness = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("camera_mouse_gcd", Text.translatable("activity.setting.defense.cart_camera_mouse_gcd"),
                Text.translatable("activity.setting.defense.cart_camera_mouse_gcd.desc"), SettingGroup.BEHAVIOR,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoCartCameraMouseGcd;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoCartCameraMouseGcd = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("autonomous_placement", Text.translatable("activity.setting.defense.autonomous_placement"),
                Text.translatable("activity.setting.defense.autonomous_placement.desc"), SettingGroup.BEHAVIOR,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c == null || c.autoCartAutonomousPlacement;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoCartAutonomousPlacement = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

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

        registerBoolean("neural_aim", Text.translatable("activity.setting.defense.neural_aim"),
                Text.translatable("activity.setting.defense.neural_aim.desc"), SettingGroup.ADVANCED,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoCartNeuralAim;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoCartNeuralAim = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("motor_calibration", Text.translatable("activity.setting.defense.motor_calibration"),
                Text.translatable("activity.setting.defense.motor_calibration.desc"), SettingGroup.ADVANCED,
                false,
                () -> dev.virion.arc.ArcMotorCalibrationService.isActive(),
                val -> {
                    if (val) {
                        dev.virion.arc.ArcMotorCalibrationService.start();
                    } else {
                        dev.virion.arc.ArcMotorCalibrationService.stop();
                    }
                }
        );

        registerBoolean("reset_calibration", Text.translatable("activity.setting.defense.reset_calibration"),
                Text.translatable("activity.setting.defense.reset_calibration.desc"), SettingGroup.ADVANCED,
                false,
                () -> resetCalibrationPending,
                val -> {
                    resetCalibrationPending = val;
                    if (val) {
                        dev.virion.arc.ArcNeuralMotorProfile.getInstance().resetCalibration();
                        activity.client.gui.overlay.ClientNotification.show(Text.translatable("activity.toast.calibration_reset"));
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
        MorrowConfig.cameraMode = c.autoCartCameraMode != null ? c.autoCartCameraMode : "auto";
        MorrowConfig.autoCamera = c.autoCartAutoCamera;
        MorrowConfig.cameraSmoothnessMs = (int) Math.round(c.autoCartCameraSmoothness);
        MorrowConfig.cameraReturn = c.autoCartCameraReturn;
        MorrowConfig.cameraReturnSmoothnessMs = (int) Math.round(c.autoCartCameraReturnSmoothness);
        MorrowConfig.cameraCurve = (int) Math.round(c.autoCartCameraCurve);
        MorrowConfig.cameraRandomness = (int) Math.round(c.autoCartCameraRandomness);
        MorrowConfig.cameraMouseGcd = c.autoCartCameraMouseGcd;
        MorrowConfig.neuralAim = c.autoCartNeuralAim;
        MorrowConfig.autonomousPlacement = c.autoCartAutonomousPlacement;

        if ("safe".equals(c.autoCartPreset)) {
            MorrowConfig.preset = MorrowConfig.PRESET_SAFE;
        } else if ("medium".equals(c.autoCartPreset)) {
            MorrowConfig.preset = MorrowConfig.PRESET_MEDIUM;
        } else if ("learned".equals(c.autoCartPreset)) {
            MorrowConfig.preset = MorrowConfig.PRESET_LEARNED;
        } else if ("custom".equals(c.autoCartPreset)) {
            MorrowConfig.preset = MorrowConfig.PRESET_CUSTOM;
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
        controller.resetSession(MinecraftClient.getInstance());
        net.fabricmc.pack.api.CombatLockManager.setLock("pvp.cart_placement_active", false);
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (isEnabled() || dev.virion.arc.ArcMotorCalibrationService.isActive()) {
            controller.tick(client);
        }
    }

    @Override
    public void onRenderHud(DrawContext context, RenderTickCounter tickCounter) {
        if (!dev.virion.arc.ArcMotorCalibrationService.isActive() || context == null) {
            return;
        }
        renderCalibrationHud(context);
    }

    private void renderCalibrationHud(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) return;

        long remainingMs = dev.virion.arc.ArcMotorCalibrationService.getRemainingTimeMs();
        long totalSec = (remainingMs + 999L) / 1000L;
        long min = totalSec / 60L;
        long sec = totalSec % 60L;
        String timeStr = String.format("%02d:%02d", min, sec);

        int detonated = dev.virion.arc.ArcMotorCalibrationService.getManualDetonationsCount();
        int progress = dev.virion.arc.ArcMotorCalibrationService.getProgress();

        String hudText = String.format("[КАЛИБРОВКА AUTOCART]  %s  |  Подрывов: %d  |  Моторика: %d%%", timeStr, detonated, progress);

        int textW = client.textRenderer.getWidth(hudText);
        int padX = 8;
        int padY = 4;
        int boxW = textW + padX * 2;
        int boxH = client.textRenderer.fontHeight + padY * 2;
        int boxX = (context.getScaledWindowWidth() - boxW) / 2;
        int boxY = 6;

        context.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xD00A0D14);
        context.fill(boxX, boxY, boxX + boxW, boxY + 1, 0x503EA4E8);
        context.fill(boxX, boxY + boxH - 1, boxX + boxW, boxY + boxH, 0x503EA4E8);
        context.fill(boxX, boxY + 1, boxX + 1, boxY + boxH - 1, 0x503EA4E8);
        context.fill(boxX + boxW - 1, boxY + 1, boxX + boxW, boxY + boxH - 1, 0x503EA4E8);

        int barW = (int) Math.round((boxW - 2) * (progress / 100.0));
        if (barW > 0) {
            context.fill(boxX + 1, boxY + boxH - 2, boxX + 1 + barW, boxY + boxH - 1, 0xFF3EA4E8);
        }

        context.drawTextWithShadow(client.textRenderer, Text.literal(hudText), boxX + padX, boxY + padY, 0xFFFFFFFF);
    }

    public void activateLearnedPreset() {
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            c.autoCartPreset = "learned";
            dev.virion.arc.ArcNeuralMotorProfile prof = dev.virion.arc.ArcNeuralMotorProfile.getInstance();
            double minD = prof.getLearnedMinDelayMs();
            double maxD = prof.getLearnedMaxDelayMs();
            double chance = 100.0;
            double maxDist = 4.4;
            boolean pit = true;
            boolean legit = true;
            String camMode = "auto";
            double camSmooth = prof.getLearnedCameraSmoothness();
            double camRetSmooth = Math.max(35.0, prof.getLearnedCameraSmoothness() - 10.0);
            double camCurve = Math.round(prof.getCurvatureBias() * 100.0f);
            double camRand = Math.round(prof.getTremorVolatility() * 500.0f);
            boolean camRet = true, camGcd = true;

            c.autoCartMinDelayMs = minD;
            c.autoCartMaxDelayMs = maxD;
            c.autoCartPlacementChance = chance;
            c.autoCartMaxDistance = maxDist;
            c.autoCartAllowPitPlacement = pit;
            c.autoCartRandomDelay = true;
            c.autoCartLegitMode = legit;
            c.autoCartCameraMode = camMode;
            c.autoCartAutoCamera = true;
            c.autoCartCameraSmoothness = camSmooth;
            c.autoCartCameraReturn = camRet;
            c.autoCartCameraReturnSmoothness = camRetSmooth;
            c.autoCartCameraCurve = camCurve;
            c.autoCartCameraRandomness = camRand;
            c.autoCartCameraMouseGcd = camGcd;

            updateEnumSetting("preset", "learned");
            updateNumberSetting("min_delay", minD);
            updateNumberSetting("max_delay", maxD);
            updateNumberSetting("placement_chance", chance);
            updateNumberSetting("max_distance", maxDist);
            updateBooleanSetting("allow_pit_placement", pit);
            updateBooleanSetting("legit_mode", legit);
            updateEnumSetting("camera_mode", camMode);
            updateNumberSetting("camera_smoothness", camSmooth);
            updateBooleanSetting("camera_return", camRet);
            updateNumberSetting("camera_return_smoothness", camRetSmooth);
            updateNumberSetting("camera_curve", camCurve);
            updateNumberSetting("camera_randomness", camRand);
            updateBooleanSetting("camera_mouse_gcd", camGcd);

            syncControllerConfig(c);
            ActivityConfigManager.markDirty();
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

    private void updateEnumSetting(String id, String val) {
        activity.client.module.setting.Setting<?> s = getSetting(id);
        if (s instanceof activity.client.module.setting.EnumSetting es) {
            es.set(val);
        }
    }
}

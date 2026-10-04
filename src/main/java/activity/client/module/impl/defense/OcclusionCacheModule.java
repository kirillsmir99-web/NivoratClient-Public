package activity.client.module.impl.defense;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.icon.ActivityIcon;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.NivoratModule;
import activity.client.module.setting.SettingGroup;
import dev.nivorat.arc.MorrowConfig;
import dev.nivorat.arc.AutoCartController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

import java.util.List;

public class OcclusionCacheModule extends NivoratModule {
    public static final String ID = "auto_cart";
    private final AutoCartController controller = new AutoCartController();

    public OcclusionCacheModule() {
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
                .aliases("autocart", "cart", "вагонетка", "вагонетки", "автовагонетка", "авто-вагонетка", "автокарт", "авто-карт", "подрыв вагонеток", "подрыв", "тнт", "tnt", "delay", "задержка", "дистанция", "distance", "яма", "pit", "рельсы", "rails", "камера", "camera", "автокамера", "плавная камера")
                .build();

        java.util.function.BooleanSupplier isClassic = () -> {
            ActivityConfig c = ActivityConfigManager.getConfig();
            return c == null || !"beta_neural".equalsIgnoreCase(c.autoCartMode);
        };
        java.util.function.BooleanSupplier isBetaNeural = () -> {
            ActivityConfig c = ActivityConfigManager.getConfig();
            return c != null && "beta_neural".equalsIgnoreCase(c.autoCartMode);
        };
        java.util.concurrent.atomic.AtomicBoolean expertSettingsExpanded = new java.util.concurrent.atomic.AtomicBoolean(false);
        java.util.function.BooleanSupplier isProfileSettingVisible = () -> isClassic.getAsBoolean() || expertSettingsExpanded.get();

        registerEnum("cart_mode", Text.translatable("activity.setting.defense.cart_mode"),
                Text.translatable("activity.setting.defense.cart_mode.desc"), SettingGroup.GENERAL,
                List.of("classic", "beta_neural"), "classic",
                opt -> Text.translatable("activity.dropdown.cart_mode." + opt),
                opt -> Text.translatable("activity.dropdown.cart_mode." + opt + ".desc"),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoCartMode != null ? c.autoCartMode : "classic";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoCartMode = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

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
                            minD = 95.0;
                            maxD = 135.0;
                            chance = 100.0;
                            maxDist = 4.2;
                            pit = true;
                            legit = true;
                            camMode = "auto";
                            camSmooth = 180.0;
                            camRetSmooth = 165.0;
                            camCurve = 45.0;
                            camRand = 40.0;
                        } else if ("learned".equalsIgnoreCase(val)) {
                            dev.nivorat.arc.ArcMotionProfile prof = dev.nivorat.arc.ArcMotionProfile.getInstance();
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
        ).visibleWhen(isClassic);

        registerKeybind("macro_keybind", Text.translatable("activity.setting.defense.cart_macro"),
                Text.translatable("activity.setting.defense.cart_macro.desc"), SettingGroup.GENERAL,
                new activity.client.module.keybind.Keybind(),
                () -> ActivityConfigManager.getConfig().autoCartMacroKeybind,
                value -> {
                    ActivityConfigManager.getConfig().autoCartMacroKeybind.copyFrom(value);
                    ActivityConfigManager.markDirty();
                }).onPress(client -> {
                    int drawTicks = dev.nivorat.arc.ArcMotionProfile.getInstance().sampleMacroDrawTicks((int) ActivityConfigManager.getConfig().autoCartMacroDrawTicks);
                    if (!controller.startMacro(client, drawTicks)) {
                        activity.client.gui.overlay.ClientNotification.show(Text.translatable("activity.toast.cart_macro_unavailable"));
                    }
                });
        registerNumber("macro_draw_ticks", Text.translatable("activity.setting.defense.cart_macro_draw"),
                Text.translatable("activity.setting.defense.cart_macro_draw.desc"), SettingGroup.GENERAL,
                3.0, 20.0, 1.0, "", true, 6.0,
                () -> ActivityConfigManager.getConfig().autoCartMacroDrawTicks,
                value -> {
                    ActivityConfigManager.getConfig().autoCartMacroDrawTicks = value;
                    ActivityConfigManager.markDirty();
                });

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
        ).visibleWhen(isProfileSettingVisible);

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
        ).visibleWhen(isProfileSettingVisible);

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
        ).visibleWhen(isProfileSettingVisible);

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
        ).visibleWhen(isProfileSettingVisible);

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
        ).visibleWhen(isProfileSettingVisible);

        registerBoolean("expert_settings", Text.literal("Параметры профиля"),
                Text.literal("Отображать детальные параметры наведения камеры и моторики"),
                SettingGroup.BEHAVIOR,
                false,
                expertSettingsExpanded::get,
                expertSettingsExpanded::set
        ).visibleWhen(isBetaNeural);

        java.util.function.Supplier<String> getCameraModeVal = () -> {
            ActivityConfig c = ActivityConfigManager.getConfig();
            return c != null && c.autoCartCameraMode != null ? c.autoCartCameraMode : "auto";
        };
        java.util.function.BooleanSupplier isCameraSmoothAim = () -> {
            String m = getCameraModeVal.get();
            return !"off".equalsIgnoreCase(m) && !"packet".equalsIgnoreCase(m);
        };
        java.util.function.BooleanSupplier isCameraVisible = isProfileSettingVisible;

        registerEnum("camera_mode", Text.translatable("activity.setting.defense.cart_camera_mode"),
                Text.translatable("activity.setting.defense.cart_camera_mode.desc"), SettingGroup.BEHAVIOR,
                List.of("off", "packet", "auto"), "auto",
                opt -> Text.translatable("activity.dropdown.cart_camera_mode." + opt),
                opt -> Text.translatable("activity.dropdown.cart_camera_mode." + opt + ".desc"),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && !"assisted".equalsIgnoreCase(c.autoCartCameraMode) ? c.autoCartCameraMode : "auto";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoCartCameraMode = "assisted".equalsIgnoreCase(val) ? "auto" : val;
                        c.autoCartAutoCamera = "auto".equalsIgnoreCase(c.autoCartCameraMode);
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(isCameraVisible);

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
        ).visibleWhen(() -> isCameraVisible.getAsBoolean() && isCameraSmoothAim.getAsBoolean());

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
        ).visibleWhen(() -> isCameraVisible.getAsBoolean() && isCameraSmoothAim.getAsBoolean());

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
        ).visibleWhen(() -> isCameraVisible.getAsBoolean() && isCameraSmoothAim.getAsBoolean() && ActivityConfigManager.getConfig() != null && ActivityConfigManager.getConfig().autoCartCameraReturn);

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
        ).visibleWhen(() -> isProfileSettingVisible.getAsBoolean() && isCameraSmoothAim.getAsBoolean());

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
        ).visibleWhen(() -> isProfileSettingVisible.getAsBoolean() && isCameraSmoothAim.getAsBoolean());

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
        ).visibleWhen(isCameraVisible);

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
        ).visibleWhen(isProfileSettingVisible);

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
        ).visibleWhen(isProfileSettingVisible);

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
        ).visibleWhen(isProfileSettingVisible);

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
        ).visibleWhen(isProfileSettingVisible);

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
        ).visibleWhen(isClassic);

        registerBoolean("adaptive_aim", Text.translatable("activity.setting.defense.adaptive_aim"),
                Text.translatable("activity.setting.defense.adaptive_aim.desc"), SettingGroup.ADVANCED,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoCartAdaptiveAim;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoCartAdaptiveAim = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(isBetaNeural);

        registerAction("motor_calibration", Text.translatable("activity.setting.defense.motor_calibration"),
                Text.translatable("activity.setting.defense.motor_calibration.desc"), SettingGroup.ADVANCED,
                () -> {
                    activity.client.gui.custom.AutoCartCalibrationDrawer.toggle();
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc != null && mc.currentScreen != activity.client.gui.custom.api.ui.UI.INSTANCE) {
                        mc.send(() -> mc.setScreen(activity.client.gui.custom.api.ui.UI.INSTANCE));
                    }
                }
        );

        registerBoolean("adaptive_learning", Text.translatable("activity.setting.defense.adaptive_learning"),
                Text.translatable("activity.setting.defense.adaptive_learning.desc"), SettingGroup.ADVANCED,
                true, () -> dev.nivorat.arc.ArcMotionProfile.getInstance().isAdaptiveLearning(),
                value -> dev.nivorat.arc.ArcMotionProfile.getInstance().setAdaptiveLearning(value)
        ).visibleWhen(isBetaNeural);
    }

    private void syncControllerConfig(ActivityConfig c) {
        if (c == null) return;
        boolean isNeural = "beta_neural".equalsIgnoreCase(c.autoCartMode);
        MorrowConfig.cartMode = isNeural ? MorrowConfig.MODE_BETA_NEURAL : MorrowConfig.MODE_CLASSIC;
        MorrowConfig.placementChance = (int) Math.round(c.autoCartPlacementChance);
        MorrowConfig.maxDistance = c.autoCartMaxDistance;
        MorrowConfig.minDelayMs = (int) Math.round(c.autoCartMinDelayMs);
        MorrowConfig.maxDelayMs = (int) Math.round(c.autoCartMaxDelayMs);
        MorrowConfig.allowSelfCart = c.autoCartAllowSelfCart;
        MorrowConfig.allowPitPlacement = c.autoCartAllowPitPlacement;
        MorrowConfig.useMainhandCart = c.autoCartUseMainHand;
        MorrowConfig.randomDelay = c.autoCartRandomDelay;
        MorrowConfig.legitMode = c.autoCartLegitMode;
        MorrowConfig.autonomousPlacement = c.autoCartAutonomousPlacement;

        if (isNeural) {
            MorrowConfig.adaptiveAim = c.autoCartAdaptiveAim;
            MorrowConfig.cameraMode = c.autoCartCameraMode != null ? c.autoCartCameraMode : "auto";
            MorrowConfig.autoCamera = c.autoCartAutoCamera && !"off".equalsIgnoreCase(MorrowConfig.cameraMode);
            MorrowConfig.cameraSmoothnessMs = (int) Math.round(c.autoCartCameraSmoothness);
            MorrowConfig.cameraReturn = c.autoCartCameraReturn;
            MorrowConfig.cameraReturnSmoothnessMs = (int) Math.round(c.autoCartCameraReturnSmoothness);
            MorrowConfig.cameraCurve = (int) Math.round(c.autoCartCameraCurve);
            MorrowConfig.cameraRandomness = (int) Math.round(c.autoCartCameraRandomness);
            MorrowConfig.cameraMouseGcd = c.autoCartCameraMouseGcd;
        } else {
            MorrowConfig.adaptiveAim = false;
            MorrowConfig.cameraMode = c.autoCartCameraMode != null ? c.autoCartCameraMode : "packet";
            MorrowConfig.autoCamera = c.autoCartAutoCamera && !"off".equalsIgnoreCase(MorrowConfig.cameraMode);
            MorrowConfig.cameraSmoothnessMs = (int) Math.round(c.autoCartCameraSmoothness);
            MorrowConfig.cameraReturn = c.autoCartCameraReturn;
            MorrowConfig.cameraReturnSmoothnessMs = (int) Math.round(c.autoCartCameraReturnSmoothness);
            MorrowConfig.cameraCurve = (int) Math.round(c.autoCartCameraCurve);
            MorrowConfig.cameraRandomness = (int) Math.round(c.autoCartCameraRandomness);
            MorrowConfig.cameraMouseGcd = c.autoCartCameraMouseGcd;
        }

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

    public AutoCartController getController() {
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
        if (isEnabled() || dev.nivorat.arc.ArcMotorCalibrationService.isActive()) {
            controller.tick(client);
        }
    }

    @Override
    public void onRenderHud(DrawContext context, RenderTickCounter tickCounter) {
    }

    public void activateLearnedPreset() {
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            c.autoCartMode = "beta_neural";
            c.autoCartPreset = "learned";
            updateEnumSetting("cart_mode", "beta_neural");
            dev.nivorat.arc.ArcMotionProfile prof = dev.nivorat.arc.ArcMotionProfile.getInstance();
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

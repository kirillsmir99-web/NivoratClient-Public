package activity.client.module.impl.combat;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.icon.ActivityIcon;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.NivoratModule;
import activity.client.module.keybind.Keybind;
import activity.client.module.setting.KeybindSetting;
import activity.client.module.setting.SettingGroup;
import dev.kinetictweaks.controller.PearlCatchController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class AutoPearlCatchModule extends NivoratModule {
    public static final String ID = "auto_pearl_catch";
    private final PearlCatchController controller = PearlCatchController.getInstance();

    public AutoPearlCatchModule() {
        super(ID, Text.translatable("activity.module.auto_pearl_catch.name"), Text.translatable("activity.module.auto_pearl_catch.desc"), ModuleCategory.COMBAT);
        this.keybind.clear();
        this.metadata = ModuleMetadata.builder(ID)
                .displayName(name)
                .description(description)
                .category(category)
                .author("kt1xW")
                .version("1.0.0")
                .icon(ActivityIcon.COMBAT)
                .keybind(keybind)
                .aliases("pearlcatch", "pearl_catch", "autopearlcatch", "pearl", "эндерперл", "перл", "пёрл", "перлкэтч", "windcharge", "ветер", "заряд ветра", "катч", "catch")
                .build();

        var modeSetting = registerEnum("mode", Text.translatable("activity.setting.combat.pearl_catch_mode"),
                Text.translatable("activity.setting.combat.pearl_catch_mode.desc"), SettingGroup.GENERAL,
                List.of("semi_auto", "full_auto"), "semi_auto",
                opt -> Text.translatable("activity.dropdown.pearl_catch_mode." + opt),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoPearlCatchMode : "semi_auto";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoPearlCatchMode = val;
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerKeybind("throw_keybind",
                Text.translatable("activity.setting.combat.pearl_catch_throw_keybind"),
                Text.translatable("activity.setting.combat.pearl_catch_throw_keybind.desc"),
                SettingGroup.GENERAL,
                new Keybind(GLFW.GLFW_KEY_V, false, false, false),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoPearlCatchThrowKeybind : new Keybind(GLFW.GLFW_KEY_V);
                },
                kb -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoPearlCatchThrowKeybind.copyFrom(kb);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).onPress(client -> {
            if (isEnabled()) {
                controller.trigger(client, PearlCatchController.Mode.VERTICAL);
            }
        }).visibleWhen(modeSetting, "semi_auto");

        registerKeybind("action_keybind",
                Text.translatable("activity.setting.combat.pearl_catch_action_keybind"),
                Text.translatable("activity.setting.combat.pearl_catch_action_keybind.desc"),
                SettingGroup.GENERAL,
                new Keybind(GLFW.GLFW_KEY_V, false, false, false),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoPearlCatchActionKeybind : new Keybind(GLFW.GLFW_KEY_V);
                },
                kb -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoPearlCatchActionKeybind.copyFrom(kb);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).onPress(client -> {
            if (isEnabled()) {
                controller.trigger(client, PearlCatchController.Mode.VERTICAL);
            }
        }).visibleWhen(modeSetting, "full_auto");

        registerKeybind("horizontal_keybind",
                Text.translatable("activity.setting.combat.pearl_catch_horizontal_keybind"),
                Text.translatable("activity.setting.combat.pearl_catch_horizontal_keybind.desc"),
                SettingGroup.GENERAL,
                new Keybind(GLFW.GLFW_KEY_C, false, false, false),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoPearlCatchHorizontalKeybind : new Keybind(GLFW.GLFW_KEY_C);
                },
                kb -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoPearlCatchHorizontalKeybind.copyFrom(kb);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).onPress(client -> {
            if (isEnabled()) {
                controller.trigger(client, PearlCatchController.Mode.HORIZONTAL);
            }
        }).visibleWhen(modeSetting, "full_auto");

        registerNumber("throw_delay", Text.translatable("activity.setting.combat.throw_delay"),
                Text.translatable("activity.setting.combat.throw_delay.desc"), SettingGroup.BEHAVIOR,
                1.0, 5.0, 1.0, " t", true, 2.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoPearlCatchThrowDelay : 2.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoPearlCatchThrowDelay = val;
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("rotation_time_ms", Text.translatable("activity.setting.combat.rotation_time_ms"),
                Text.translatable("activity.setting.combat.rotation_time_ms.desc"), SettingGroup.BEHAVIOR,
                50.0, 250.0, 5.0, " ms", true, 135.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoPearlCatchRotationTimeMs : 135.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoPearlCatchRotationTimeMs = val;
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(modeSetting, "full_auto");

        registerNumber("horizontal_offset", Text.translatable("activity.setting.combat.horizontal_offset"),
                Text.translatable("activity.setting.combat.horizontal_offset.desc"), SettingGroup.BEHAVIOR,
                4.0, 16.0, 0.5, "°", false, 8.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoPearlCatchHorizontalOffset : 8.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoPearlCatchHorizontalOffset = val;
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(modeSetting, "full_auto");

        registerBoolean("restore_slot", Text.translatable("activity.setting.combat.restore_slot"),
                Text.translatable("activity.setting.combat.restore_slot.desc"), SettingGroup.BEHAVIOR,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoPearlCatchRestoreSlot;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoPearlCatchRestoreSlot = val;
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("restore_camera", Text.translatable("activity.setting.combat.restore_camera"),
                Text.translatable("activity.setting.combat.restore_camera.desc"), SettingGroup.BEHAVIOR,
                false,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoPearlCatchRestoreCamera;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoPearlCatchRestoreCamera = val;
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(modeSetting, "full_auto");

        registerBoolean("random_delay", Text.translatable("activity.setting.combat.random_jitter"),
                Text.translatable("activity.setting.combat.random_jitter.desc"), SettingGroup.BEHAVIOR,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoPearlCatchRandomDelay;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoPearlCatchRandomDelay = val;
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("legit_mode", Text.translatable("activity.setting.combat.legit_mode"),
                Text.translatable("activity.setting.combat.legit_mode.desc"), SettingGroup.ADVANCED,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoPearlCatchLegitMode;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoPearlCatchLegitMode = val;
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(modeSetting, "full_auto");
    }

    public PearlCatchController getController() {
        return controller;
    }

    public void trigger(MinecraftClient client) {
        if (isEnabled()) {
            controller.trigger(client, PearlCatchController.Mode.VERTICAL);
        }
    }

    public void trigger(MinecraftClient client, PearlCatchController.Mode mode) {
        if (isEnabled()) {
            controller.trigger(client, mode);
        }
    }

    public void triggerHorizontal(MinecraftClient client) {
        if (isEnabled()) {
            controller.trigger(client, PearlCatchController.Mode.HORIZONTAL);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            c.autoPearlCatchEnabled = enabled;
            ActivityConfigManager.markDirty();
        }
        if (!enabled) {
            controller.reset();
        }
    }

    @Override
    public void onDisable() {
        controller.reset();
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (isEnabled()) {
            controller.onTick(client);
        }
    }

    @Override
    public void loadFromConfig(ActivityConfig config) {
        if (config == null) return;
        this.enabled = config.autoPearlCatchEnabled;
        this.keybind.copyFrom(config.autoPearlCatchKeybind);
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        config.autoPearlCatchEnabled = this.enabled;
        config.autoPearlCatchKeybind.copyFrom(this.keybind);
    }
}

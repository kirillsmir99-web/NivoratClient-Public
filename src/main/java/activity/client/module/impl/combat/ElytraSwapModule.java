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
import dev.elytra.ElytraSwapConfig;
import dev.elytra.ElytraSwapController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class ElytraSwapModule extends NivoratModule {
    public static final String ID = "elytra_swap";
    private final ElytraSwapController controller = ElytraSwapController.getInstance();
    private final KeybindSetting triggerKeybindSetting;

    public ElytraSwapModule() {
        super(ID, Text.translatable("activity.module.elytra_swap.name"), Text.translatable("activity.module.elytra_swap.desc"), ModuleCategory.COMBAT);
        this.keybind.clear();
        this.metadata = ModuleMetadata.builder(ID)
                .displayName(name)
                .description(description)
                .category(category)
                .author("Nivorat")
                .version("1.0.0")
                .icon(ActivityIcon.COMBAT)
                .keybind(keybind)
                .aliases("elytraswap", "elytra_swap", "elytra", "элитрасвап", "элитра", "свапэлитры", "свап элитры", "нагрудник")
                .build();

        this.triggerKeybindSetting = registerKeybind("trigger_keybind",
                Text.translatable("activity.setting.combat.elytra_swap_trigger_keybind"),
                Text.translatable("activity.setting.combat.elytra_swap_trigger_keybind.desc"),
                SettingGroup.GENERAL,
                new Keybind(),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.elytraSwapTriggerKeybind : new Keybind();
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.elytraSwapTriggerKeybind.copyFrom(val);
                        ActivityConfigManager.markDirty();
                    }
                }
        );
        this.triggerKeybindSetting.onPress(client -> {
            if (isEnabled()) {
                controller.trigger(client);
            }
        });

        registerNumber("restore_delay",
                Text.translatable("activity.setting.combat.elytra_swap_restore_delay"),
                Text.translatable("activity.setting.combat.elytra_swap_restore_delay.desc"),
                SettingGroup.GENERAL,
                30.0, 300.0, 5.0, "ms", false, 85.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.elytraSwapRestoreDelayMs : 85.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.elytraSwapRestoreDelayMs = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("random_delay",
                Text.translatable("activity.setting.combat.elytra_swap_random_delay"),
                Text.translatable("activity.setting.combat.elytra_swap_random_delay.desc"),
                SettingGroup.GENERAL,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.elytraSwapRandomDelay : true;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.elytraSwapRandomDelay = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("auto_restore",
                Text.translatable("activity.setting.combat.elytra_swap_auto_restore"),
                Text.translatable("activity.setting.combat.elytra_swap_auto_restore.desc"),
                SettingGroup.GENERAL,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.elytraSwapAutoRestore : true;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.elytraSwapAutoRestore = val;
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
        ElytraSwapConfig.enabled = enabled;
        ElytraSwapConfig.restoreDelayMs = c.elytraSwapRestoreDelayMs;
        ElytraSwapConfig.randomDelay = c.elytraSwapRandomDelay;
        ElytraSwapConfig.autoRestore = c.elytraSwapAutoRestore;
    }

    public ElytraSwapController getController() {
        return controller;
    }

    @Override
    public boolean hasTickLogic() {
        return true;
    }

    @Override
    public void onClientTick(MinecraftClient client) {
        controller.onTick(client);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        controller.reset();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            c.elytraSwapEnabled = enabled;
            syncControllerConfig(c);
            ActivityConfigManager.markDirty();
        }
    }

    @Override
    public void loadFromConfig(ActivityConfig config) {
        if (config == null) return;
        this.enabled = config.elytraSwapEnabled;
        if (config.elytraSwapKeybind != null) {
            this.keybind.copyFrom(config.elytraSwapKeybind);
        }
        syncControllerConfig(config, this.enabled);
    }
}

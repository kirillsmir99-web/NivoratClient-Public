package activity.client.module.impl.defense;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.icon.ActivityIcon;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.NivoratModule;
import activity.client.module.setting.SettingGroup;
import dev.autototem.AutoTotemConfig;
import dev.autototem.AutoTotemController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.List;

public class AutoTotemModule extends NivoratModule {
    public static final String ID = "auto_totem";
    private final AutoTotemController controller = new AutoTotemController();

    public AutoTotemModule() {
        super(ID, Text.translatable("activity.module.auto_totem.name"), Text.translatable("activity.module.auto_totem.desc"), ModuleCategory.DEFENSE);
        this.metadata = ModuleMetadata.builder(ID)
                .displayName(name)
                .description(description)
                .category(category)
                .author("Nivorat")
                .version("1.0.3")
                .icon(ActivityIcon.DEFENSE)
                .keybind(keybind)
                .aliases("autototem", "totem", "тотем", "автототем", "авто-тотем", "hearts", "сердца", "поп", "pop", "здоровье", "хп", "hp", "chance", "шанс")
                .build();

        // 1. GENERAL
        registerEnum("mode", Text.translatable("activity.setting.defense.mode"),
                Text.translatable("activity.setting.defense.mode.desc"), SettingGroup.GENERAL,
                List.of("main_hand", "offhand"), "main_hand",
                opt -> Text.translatable("activity.dropdown.totem_mode." + opt),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoTotemMode : "main_hand";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoTotemMode = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        // 2. BEHAVIOR
        registerNumber("trigger_hearts", Text.translatable("activity.setting.defense.trigger_hearts"),
                Text.translatable("activity.setting.defense.trigger_hearts.desc"), SettingGroup.BEHAVIOR,
                1.0, 9.0, 1.0, " ❤", true, 3.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoTotemTriggerHearts : 3.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoTotemTriggerHearts = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("restore_hearts", Text.translatable("activity.setting.defense.restore_hearts"),
                Text.translatable("activity.setting.defense.restore_hearts.desc"), SettingGroup.BEHAVIOR,
                4.0, 10.0, 1.0, " ❤", true, 6.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoTotemRestoreHearts : 6.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoTotemRestoreHearts = val;
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
                    return c != null ? c.autoTotemChance : 100.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoTotemChance = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        // 3. EXTRA
        registerBoolean("return_item", Text.translatable("activity.setting.defense.return_item"),
                Text.translatable("activity.setting.defense.return_item.desc"), SettingGroup.EXTRA,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoTotemReturnItem;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoTotemReturnItem = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("return_on_pop", Text.translatable("activity.setting.defense.return_on_pop"),
                Text.translatable("activity.setting.defense.return_on_pop.desc"), SettingGroup.EXTRA,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoTotemReturnOnPop;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoTotemReturnOnPop = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );
    }

    private void syncControllerConfig(ActivityConfig c) {
        if (c == null) return;
        AutoTotemConfig.enabled = this.enabled;
        AutoTotemConfig.mode = "offhand".equals(c.autoTotemMode) ? 2 : 1;
        AutoTotemConfig.triggerHearts = (int) Math.round(c.autoTotemTriggerHearts);
        AutoTotemConfig.restoreHearts = (int) Math.round(c.autoTotemRestoreHearts);
        AutoTotemConfig.chance = (int) c.autoTotemChance;
        AutoTotemConfig.returnItem = c.autoTotemReturnItem;
        AutoTotemConfig.returnOnPop = c.autoTotemReturnOnPop;
    }

    public AutoTotemController getController() {
        return controller;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            c.autoTotemEnabled = enabled;
            ActivityConfigManager.markDirty();
        }
        AutoTotemConfig.enabled = enabled;
        if (controller.isEnabled() != enabled) {
            controller.toggle();
        }
    }

    @Override
    public void onDisable() {
        net.fabricmc.pack.api.CombatLockManager.setLock("pvp.totem_active", false);
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
        this.enabled = config.autoTotemEnabled;
        this.keybind.copyFrom(config.autoTotemKeybind);
        if (controller.isEnabled() != this.enabled) {
            controller.toggle();
        }
        syncControllerConfig(config);
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        config.autoTotemEnabled = this.enabled;
        config.autoTotemKeybind.copyFrom(this.keybind);
        syncControllerConfig(config);
    }
}

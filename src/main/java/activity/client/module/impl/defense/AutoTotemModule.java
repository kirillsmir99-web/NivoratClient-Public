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
                List.of("main_hand", "offhand", "crystal"), "main_hand",
                opt -> Text.translatable("activity.dropdown.totem_mode." + opt),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoTotemMode : "main_hand";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoTotemMode = val;
                        activity.client.module.setting.NumberSetting triggerSetting = (activity.client.module.setting.NumberSetting) getSetting("trigger_hearts");
                        activity.client.module.setting.NumberSetting restoreSetting = (activity.client.module.setting.NumberSetting) getSetting("restore_hearts");
                        if ("crystal".equals(val)) {
                            if (triggerSetting != null) triggerSetting.set(c.autoTotemCrystalTriggerHearts);
                            if (restoreSetting != null) restoreSetting.set(c.autoTotemCrystalRestoreHearts);
                        } else if ("offhand".equals(val)) {
                            if (triggerSetting != null) triggerSetting.set(c.autoTotemOffhandTriggerHearts);
                            if (restoreSetting != null) restoreSetting.set(c.autoTotemOffhandRestoreHearts);
                        } else {
                            if (triggerSetting != null) triggerSetting.set(c.autoTotemMainhandTriggerHearts);
                            if (restoreSetting != null) restoreSetting.set(c.autoTotemMainhandRestoreHearts);
                        }
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        // 2. BEHAVIOR
        registerNumber("trigger_hearts", Text.translatable("activity.setting.defense.trigger_hearts"),
                Text.translatable("activity.setting.defense.trigger_hearts.desc"), SettingGroup.BEHAVIOR,
                0.5, 10.0, 0.5, " ❤", false, 3.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c == null) return 3.0;
                    if ("crystal".equals(c.autoTotemMode)) return c.autoTotemCrystalTriggerHearts;
                    if ("offhand".equals(c.autoTotemMode)) return c.autoTotemOffhandTriggerHearts;
                    return c.autoTotemMainhandTriggerHearts;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        if ("crystal".equals(c.autoTotemMode)) {
                            c.autoTotemCrystalTriggerHearts = val;
                        } else if ("offhand".equals(c.autoTotemMode)) {
                            c.autoTotemOffhandTriggerHearts = val;
                        } else {
                            c.autoTotemMainhandTriggerHearts = val;
                        }
                        c.autoTotemTriggerHearts = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("restore_hearts", Text.translatable("activity.setting.defense.restore_hearts"),
                Text.translatable("activity.setting.defense.restore_hearts.desc"), SettingGroup.BEHAVIOR,
                0.5, 10.0, 0.5, " ❤", false, 6.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c == null) return 6.0;
                    if ("crystal".equals(c.autoTotemMode)) return c.autoTotemCrystalRestoreHearts;
                    if ("offhand".equals(c.autoTotemMode)) return c.autoTotemOffhandRestoreHearts;
                    return c.autoTotemMainhandRestoreHearts;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        if ("crystal".equals(c.autoTotemMode)) {
                            c.autoTotemCrystalRestoreHearts = val;
                        } else if ("offhand".equals(c.autoTotemMode)) {
                            c.autoTotemOffhandRestoreHearts = val;
                        } else {
                            c.autoTotemMainhandRestoreHearts = val;
                        }
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

        registerBoolean("auto_refill", Text.translatable("activity.setting.defense.auto_refill"),
                Text.translatable("activity.setting.defense.auto_refill.desc"), SettingGroup.EXTRA,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoTotemAutoRefill;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoTotemAutoRefill = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerEnum("refill_slot", Text.translatable("activity.setting.defense.refill_slot"),
                Text.translatable("activity.setting.defense.refill_slot.desc"), SettingGroup.EXTRA,
                List.of("auto", "1", "2", "3", "4", "5", "6", "7", "8", "9"), "auto",
                opt -> "auto".equals(opt)
                        ? Text.translatable("activity.setting.defense.refill_slot.auto")
                        : Text.translatable("activity.setting.defense.refill_slot.slot", opt),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoTotemRefillSlot != null ? c.autoTotemRefillSlot : "auto";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoTotemRefillSlot = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );
    }

    private void syncControllerConfig(ActivityConfig c) {
        if (c == null) return;
        AutoTotemConfig.enabled = this.enabled;
        boolean isOffhand = "offhand".equals(c.autoTotemMode);
        boolean isCrystal = "crystal".equals(c.autoTotemMode);
        AutoTotemConfig.mode = isCrystal ? 3 : (isOffhand ? 2 : 1);
        AutoTotemConfig.mainhandTriggerHearts = c.autoTotemMainhandTriggerHearts;
        AutoTotemConfig.mainhandRestoreHearts = c.autoTotemMainhandRestoreHearts;
        AutoTotemConfig.offhandTriggerHearts = c.autoTotemOffhandTriggerHearts;
        AutoTotemConfig.offhandRestoreHearts = c.autoTotemOffhandRestoreHearts;
        AutoTotemConfig.crystalTriggerHearts = c.autoTotemCrystalTriggerHearts;
        AutoTotemConfig.crystalRestoreHearts = c.autoTotemCrystalRestoreHearts;
        if (isCrystal) {
            AutoTotemConfig.triggerHearts = AutoTotemConfig.crystalTriggerHearts;
            AutoTotemConfig.restoreHearts = AutoTotemConfig.crystalRestoreHearts;
        } else if (isOffhand) {
            AutoTotemConfig.triggerHearts = AutoTotemConfig.offhandTriggerHearts;
            AutoTotemConfig.restoreHearts = AutoTotemConfig.offhandRestoreHearts;
        } else {
            AutoTotemConfig.triggerHearts = AutoTotemConfig.mainhandTriggerHearts;
            AutoTotemConfig.restoreHearts = AutoTotemConfig.mainhandRestoreHearts;
        }
        AutoTotemConfig.chance = (int) c.autoTotemChance;
        AutoTotemConfig.returnItem = c.autoTotemReturnItem;
        AutoTotemConfig.returnOnPop = c.autoTotemReturnOnPop;
        AutoTotemConfig.autoRefill = c.autoTotemAutoRefill;
        AutoTotemConfig.refillSlot = parseRefillSlot(c.autoTotemRefillSlot);
    }

    private static int parseRefillSlot(String slotStr) {
        if (slotStr == null || "auto".equalsIgnoreCase(slotStr)) {
            return -1;
        }
        try {
            int slotNum = Integer.parseInt(slotStr.trim());
            if (slotNum >= 1 && slotNum <= 9) {
                return slotNum - 1; // convert 1..9 to 0..8
            }
        } catch (Exception ignored) {}
        return -1;
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
        activity.client.module.setting.NumberSetting triggerSetting = (activity.client.module.setting.NumberSetting) getSetting("trigger_hearts");
        activity.client.module.setting.NumberSetting restoreSetting = (activity.client.module.setting.NumberSetting) getSetting("restore_hearts");
        if ("crystal".equals(config.autoTotemMode)) {
            if (triggerSetting != null) triggerSetting.set(config.autoTotemCrystalTriggerHearts);
            if (restoreSetting != null) restoreSetting.set(config.autoTotemCrystalRestoreHearts);
        } else if ("offhand".equals(config.autoTotemMode)) {
            if (triggerSetting != null) triggerSetting.set(config.autoTotemOffhandTriggerHearts);
            if (restoreSetting != null) restoreSetting.set(config.autoTotemOffhandRestoreHearts);
        } else {
            if (triggerSetting != null) triggerSetting.set(config.autoTotemMainhandTriggerHearts);
            if (restoreSetting != null) restoreSetting.set(config.autoTotemMainhandRestoreHearts);
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

    public static void onTotemPop() {
        activity.client.module.api.IModule mod = activity.client.module.api.ModuleRegistry.get(ID);
        if (mod instanceof AutoTotemModule atm && atm.isEnabled()) {
            atm.getController().onTotemPop();
        }
    }
}

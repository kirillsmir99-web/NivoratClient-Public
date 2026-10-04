package activity.client.module.impl.defense;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.icon.ActivityIcon;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.NivoratModule;
import activity.client.module.setting.SettingGroup;
import dev.buffer.BufferPipelineConfig;
import dev.buffer.BufferPipelineController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.List;

public class BufferPipelineModule extends NivoratModule {
    public static final String ID = "auto_totem";
    private final BufferPipelineController controller = new BufferPipelineController();

    public BufferPipelineModule() {
        super(ID, Text.translatable("activity.module.auto_totem.name"), Text.translatable("activity.module.auto_totem.desc"), ModuleCategory.DEFENSE);
        this.metadata = ModuleMetadata.builder(ID)
                .displayName(name)
                .description(description)
                .category(category)
                .author("Nivorat")
                .version("1.0.4")
                .icon(ActivityIcon.DEFENSE)
                .keybind(keybind)
                .aliases("autototem", "totem", "тотем", "автототем", "авто-тотем", "hearts", "сердца", "поп", "pop", "здоровье", "хп", "hp", "chance", "шанс")
                .build();

        registerEnum("mode", Text.translatable("activity.setting.defense.mode"),
                Text.translatable("activity.setting.defense.mode.desc"), SettingGroup.GENERAL,
                List.of("main_hand", "offhand", "crystal"), "offhand",
                opt -> Text.translatable("activity.dropdown.totem_mode." + opt),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoTotemMode : "offhand";
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

        registerEnum("inventory_source", Text.translatable("activity.setting.defense.inventory_source"),
                Text.translatable("activity.setting.defense.inventory_source.desc"), SettingGroup.GENERAL,
                List.of("hotbar", "legit", "rage"), "rage",
                opt -> Text.translatable("activity.dropdown.inventory_source." + opt),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoTotemInventorySource != null ? c.autoTotemInventorySource : "rage";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoTotemInventorySource = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

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
                0.0, 20.0, 0.5, " ❤", false, 6.0,
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

        registerNumber("swap_back_delay", Text.translatable("activity.setting.defense.swap_back_delay"),
                Text.translatable("activity.setting.defense.swap_back_delay.desc"), SettingGroup.BEHAVIOR,
                0.5, 10.0, 0.5, " s", false, 3.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoTotemSwapBackDelay : 3.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoTotemSwapBackDelay = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("always_offhand", Text.translatable("activity.setting.defense.always_offhand"),
                Text.translatable("activity.setting.defense.always_offhand.desc"), SettingGroup.BEHAVIOR,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoTotemAlwaysOffhand;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoTotemAlwaysOffhand = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("ignore_when_using", Text.translatable("activity.setting.defense.ignore_when_using"),
                Text.translatable("activity.setting.defense.ignore_when_using.desc"), SettingGroup.BEHAVIOR,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoTotemIgnoreWhenUsing;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoTotemIgnoreWhenUsing = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("count_absorption", Text.translatable("activity.setting.defense.count_absorption"),
                Text.translatable("activity.setting.defense.count_absorption.desc"), SettingGroup.EXTRA,
                false,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoTotemCountAbsorption;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoTotemCountAbsorption = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

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

        registerBoolean("predictive_damage", Text.translatable("activity.setting.defense.predictive_damage"),
                Text.translatable("activity.setting.defense.predictive_damage.desc"), SettingGroup.EXTRA,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoTotemPredictiveDamage;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoTotemPredictiveDamage = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("predict_crystals", Text.translatable("activity.setting.defense.predict_crystals"),
                Text.translatable("activity.setting.defense.predict_crystals.desc"), SettingGroup.EXTRA,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoTotemPredictCrystals;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoTotemPredictCrystals = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("predict_fall", Text.translatable("activity.setting.defense.predict_fall"),
                Text.translatable("activity.setting.defense.predict_fall.desc"), SettingGroup.EXTRA,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoTotemPredictFall;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoTotemPredictFall = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("predict_mace", Text.translatable("activity.setting.defense.predict_mace"),
                Text.translatable("activity.setting.defense.predict_mace.desc"), SettingGroup.EXTRA,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoTotemPredictMace;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoTotemPredictMace = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("predict_trident", Text.translatable("activity.setting.defense.predict_trident"),
                Text.translatable("activity.setting.defense.predict_trident.desc"), SettingGroup.EXTRA,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoTotemPredictTrident;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoTotemPredictTrident = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("low_totem_notify", Text.translatable("activity.setting.defense.low_totem_notify"),
                Text.translatable("activity.setting.defense.low_totem_notify.desc"), SettingGroup.EXTRA,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoTotemLowTotemNotify;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoTotemLowTotemNotify = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );
    }

    private void syncControllerConfig(ActivityConfig c) {
        if (c == null) return;
        BufferPipelineConfig.enabled = this.enabled;
        boolean isOffhand = "offhand".equals(c.autoTotemMode);
        boolean isCrystal = "crystal".equals(c.autoTotemMode);
        BufferPipelineConfig.mode = isCrystal ? 3 : (isOffhand ? 2 : 1);
        BufferPipelineConfig.mainhandTriggerHearts = c.autoTotemMainhandTriggerHearts;
        BufferPipelineConfig.mainhandRestoreHearts = c.autoTotemMainhandRestoreHearts;
        BufferPipelineConfig.offhandTriggerHearts = c.autoTotemOffhandTriggerHearts;
        BufferPipelineConfig.offhandRestoreHearts = c.autoTotemOffhandRestoreHearts;
        BufferPipelineConfig.crystalTriggerHearts = c.autoTotemCrystalTriggerHearts;
        BufferPipelineConfig.crystalRestoreHearts = c.autoTotemCrystalRestoreHearts;
        if (isCrystal) {
            BufferPipelineConfig.triggerHearts = BufferPipelineConfig.crystalTriggerHearts;
            BufferPipelineConfig.restoreHearts = BufferPipelineConfig.crystalRestoreHearts;
        } else if (isOffhand) {
            BufferPipelineConfig.triggerHearts = BufferPipelineConfig.offhandTriggerHearts;
            BufferPipelineConfig.restoreHearts = BufferPipelineConfig.offhandRestoreHearts;
        } else {
            BufferPipelineConfig.triggerHearts = BufferPipelineConfig.mainhandTriggerHearts;
            BufferPipelineConfig.restoreHearts = BufferPipelineConfig.mainhandRestoreHearts;
        }
        BufferPipelineConfig.countAbsorption = c.autoTotemCountAbsorption;
        BufferPipelineConfig.chance = (int) c.autoTotemChance;
        BufferPipelineConfig.returnItem = c.autoTotemReturnItem;
        BufferPipelineConfig.returnOnPop = c.autoTotemReturnOnPop;
        BufferPipelineConfig.autoRefill = c.autoTotemAutoRefill;
        BufferPipelineConfig.refillSlot = parseRefillSlot(c.autoTotemRefillSlot);
        BufferPipelineConfig.swapBackDelay = c.autoTotemSwapBackDelay;
        BufferPipelineConfig.alwaysOffhand = c.autoTotemAlwaysOffhand;
        BufferPipelineConfig.ignoreWhenUsing = c.autoTotemIgnoreWhenUsing;
        BufferPipelineConfig.predictiveDamage = c.autoTotemPredictiveDamage;
        BufferPipelineConfig.predictCrystals = c.autoTotemPredictCrystals;
        BufferPipelineConfig.predictFall = c.autoTotemPredictFall;
        BufferPipelineConfig.predictMace = c.autoTotemPredictMace;
        BufferPipelineConfig.predictTrident = c.autoTotemPredictTrident;
        BufferPipelineConfig.lowTotemNotify = c.autoTotemLowTotemNotify;
        BufferPipelineConfig.inventorySource = c.autoTotemInventorySource != null ? c.autoTotemInventorySource : "rage";
        BufferPipelineConfig.validateHysteresis();
    }

    private static int parseRefillSlot(String slotStr) {
        if (slotStr == null || "auto".equalsIgnoreCase(slotStr)) {
            return -1;
        }
        try {
            int slotNum = Integer.parseInt(slotStr.trim());
            if (slotNum >= 1 && slotNum <= 9) {
                return slotNum - 1;
            }
        } catch (Exception ignored) {}
        return -1;
    }

    public BufferPipelineController getController() {
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
        BufferPipelineConfig.enabled = enabled;
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
        activity.client.module.setting.BooleanSetting countAbsSetting = (activity.client.module.setting.BooleanSetting) getSetting("count_absorption");
        if (countAbsSetting != null) {
            countAbsSetting.set(config.autoTotemCountAbsorption);
        }
        activity.client.module.setting.BooleanSetting retPopSetting = (activity.client.module.setting.BooleanSetting) getSetting("return_on_pop");
        if (retPopSetting != null) {
            retPopSetting.set(config.autoTotemReturnOnPop);
        }
        activity.client.module.setting.EnumSetting invSourceSetting = (activity.client.module.setting.EnumSetting) getSetting("inventory_source");
        if (invSourceSetting != null && config.autoTotemInventorySource != null) {
            invSourceSetting.set(config.autoTotemInventorySource);
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
        if (mod instanceof BufferPipelineModule atm && atm.isEnabled()) {
            atm.getController().onTotemPop();
        }
    }

    public static void onHealthUpdate(float health) {
        activity.client.module.api.IModule mod = activity.client.module.api.ModuleRegistry.get(ID);
        if (mod instanceof BufferPipelineModule atm && atm.isEnabled()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                atm.getController().tick(client);
            }
        }
    }
}

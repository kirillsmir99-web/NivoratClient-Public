package activity.client.module.impl.utility;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.icon.ActivityIcon;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.NivoratModule;
import activity.client.module.setting.Setting;
import activity.client.module.setting.SettingGroup;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import ru.elarion.autotool.AutoToolClient;
import ru.elarion.autotool.AutoToolEngine;

public class AutoToolModule extends NivoratModule {
    public static final String ID = "auto_tool";

    public AutoToolModule() {
        super(ID, Text.translatable("activity.module.auto_tool.name"), Text.translatable("activity.module.auto_tool.desc"), ModuleCategory.UTILITY);
        this.metadata = ModuleMetadata.builder(ID)
                .displayName(name)
                .description(description)
                .category(category)
                .author("Nivorat")
                .version("1.0.0")
                .icon(ActivityIcon.UTILITY)
                .keybind(keybind)
                .aliases("autotool", "tool", "инструмент", "автоинструмент", "авто-инструмент", "кирка", "лопата", "шелк", "шёлковое касание", "silk touch", "прочность", "durability")
                .build();

        registerBoolean("prefer_silk", Text.translatable("activity.setting.utility.prefer_silk"),
                Text.translatable("activity.setting.utility.prefer_silk.desc"), SettingGroup.GENERAL,
                false,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoToolPreferSilkTouch;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoToolPreferSilkTouch = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("restore_previous", Text.translatable("activity.setting.utility.restore_previous"),
                Text.translatable("activity.setting.utility.restore_previous.desc"), SettingGroup.GENERAL,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoToolRestorePrevious;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoToolRestorePrevious = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("weapon_switch", Text.translatable("activity.setting.utility.weapon_switch"),
                Text.translatable("activity.setting.utility.weapon_switch.desc"), SettingGroup.GENERAL,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoToolWeaponSwitch;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoToolWeaponSwitch = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("durability_threshold", Text.translatable("activity.setting.utility.durability_threshold"),
                Text.translatable("activity.setting.utility.durability_threshold.desc"), SettingGroup.BEHAVIOR,
                1.0, 50.0, 1.0, "%", true, 5.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoToolDurabilityThreshold : 5.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoToolDurabilityThreshold = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("combat_guard", Text.translatable("activity.setting.utility.combat_guard"),
                Text.translatable("activity.setting.utility.combat_guard.desc"), SettingGroup.EXTRA,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoToolCombatGuard;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoToolCombatGuard = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("durability_saver", Text.translatable("activity.setting.utility.durability_saver"),
                Text.translatable("activity.setting.utility.durability_saver.desc"), SettingGroup.EXTRA,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoToolDurabilitySaver;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoToolDurabilitySaver = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("ignore_instant_break", Text.translatable("activity.setting.utility.ignore_instant_break"),
                Text.translatable("activity.setting.utility.ignore_instant_break.desc"), SettingGroup.EXTRA,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoToolIgnoreInstantBreak;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoToolIgnoreInstantBreak = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("lock_while_mining", Text.translatable("activity.setting.utility.lock_while_mining"),
                Text.translatable("activity.setting.utility.lock_while_mining.desc"), SettingGroup.EXTRA,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoToolLockWhileMining;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoToolLockWhileMining = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("legit_mode", Text.translatable("activity.setting.utility.legit_mode"),
                Text.translatable("activity.setting.utility.legit_mode.desc"), SettingGroup.ADVANCED,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoToolLegitMode;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoToolLegitMode = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        activity.client.module.setting.BooleanSetting singleSlotSetting = registerBoolean("single_slot_mode", Text.translatable("activity.setting.utility.single_slot_mode"),
                Text.translatable("activity.setting.utility.single_slot_mode.desc"), SettingGroup.ADVANCED,
                false,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoToolSingleSlotMode;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoToolSingleSlotMode = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerEnum("single_slot", Text.translatable("activity.setting.utility.single_slot"),
                Text.translatable("activity.setting.utility.single_slot.desc"), SettingGroup.ADVANCED,
                List.of("1", "2", "3", "4", "5", "6", "7", "8", "9"), "1",
                opt -> Text.translatable("activity.setting.utility.single_slot.slot", opt),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? String.valueOf(c.autoToolSingleSlot + 1) : "1";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        try {
                            c.autoToolSingleSlot = Math.max(0, Math.min(8, Integer.parseInt(val) - 1));
                        } catch (Exception e) {
                            c.autoToolSingleSlot = 0;
                        }
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).visibleWhen(singleSlotSetting);
    }

    @Override
    public Setting<?> getSetting(String id) {
        if ("prefer_silk_touch".equals(id)) {
            return super.getSetting("prefer_silk");
        }
        if ("restore_previous_item".equals(id)) {
            return super.getSetting("restore_previous");
        }
        return super.getSetting(id);
    }

    public void syncEngineConfig(ActivityConfig c) {
        if (c == null) return;
        AutoToolClient.CONFIG.enabled = c.autoToolEnabled;
        AutoToolClient.CONFIG.preferSilkTouch = c.autoToolPreferSilkTouch;
        AutoToolClient.CONFIG.restorePreviousItem = c.autoToolRestorePrevious;
        AutoToolClient.CONFIG.durabilityThreshold = (int) Math.round(c.autoToolDurabilityThreshold);
        AutoToolClient.CONFIG.combatGuard = c.autoToolCombatGuard;
        AutoToolClient.CONFIG.weaponSwitch = c.autoToolWeaponSwitch;
        AutoToolClient.CONFIG.durabilitySaver = c.autoToolDurabilitySaver;
        AutoToolClient.CONFIG.legitMode = c.autoToolLegitMode;
        AutoToolClient.CONFIG.singleSlotMode = c.autoToolSingleSlotMode;
        AutoToolClient.CONFIG.singleSlot = c.autoToolSingleSlot;
        AutoToolClient.CONFIG.ignoreInstantBreak = c.autoToolIgnoreInstantBreak;
        AutoToolClient.CONFIG.lockWhileMining = c.autoToolLockWhileMining;
    }

    @Override
    public void onInitialize() {
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            syncEngineConfig(c);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            c.autoToolEnabled = enabled;
            ActivityConfigManager.markDirty();
        }
        AutoToolClient.CONFIG.enabled = enabled;
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (isEnabled()) {
            AutoToolEngine.tick(client);
        }
    }

    @Override
    public ActionResult onAttackEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        if (isEnabled()) {
            AutoToolEngine.onAttackEntity(entity);
        }
        return ActionResult.PASS;
    }

    @Override
    public void loadFromConfig(ActivityConfig config) {
        if (config == null) return;
        this.enabled = config.autoToolEnabled;
        this.keybind.copyFrom(config.autoToolKeybind);
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null && c != config) {
            c.autoToolEnabled = config.autoToolEnabled;
            c.autoToolKeybind.copyFrom(config.autoToolKeybind);
            c.autoToolRestorePrevious = config.autoToolRestorePrevious;
            c.autoToolPreferSilkTouch = config.autoToolPreferSilkTouch;
            c.autoToolSingleSlotMode = config.autoToolSingleSlotMode;
            c.autoToolSingleSlot = config.autoToolSingleSlot;
            c.autoToolLegitMode = config.autoToolLegitMode;
            c.autoToolLockWhileMining = config.autoToolLockWhileMining;
            c.autoToolDurabilityThreshold = config.autoToolDurabilityThreshold;
            c.autoToolCombatGuard = config.autoToolCombatGuard;
            c.autoToolWeaponSwitch = config.autoToolWeaponSwitch;
            c.autoToolDurabilitySaver = config.autoToolDurabilitySaver;
            c.autoToolIgnoreInstantBreak = config.autoToolIgnoreInstantBreak;
        }
        syncEngineConfig(config);
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        config.autoToolEnabled = this.enabled;
        config.autoToolKeybind.copyFrom(this.keybind);
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null && c != config) {
            config.autoToolRestorePrevious = c.autoToolRestorePrevious;
            config.autoToolPreferSilkTouch = c.autoToolPreferSilkTouch;
            config.autoToolSingleSlotMode = c.autoToolSingleSlotMode;
            config.autoToolSingleSlot = c.autoToolSingleSlot;
            config.autoToolLegitMode = c.autoToolLegitMode;
            config.autoToolLockWhileMining = c.autoToolLockWhileMining;
            config.autoToolDurabilityThreshold = c.autoToolDurabilityThreshold;
            config.autoToolCombatGuard = c.autoToolCombatGuard;
            config.autoToolDurabilitySaver = c.autoToolDurabilitySaver;
            config.autoToolIgnoreInstantBreak = c.autoToolIgnoreInstantBreak;
        }
        syncEngineConfig(config);
    }
}

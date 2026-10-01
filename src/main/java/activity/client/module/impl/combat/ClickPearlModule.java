package activity.client.module.impl.combat;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.icon.ActivityIcon;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.NivoratModule;
import activity.client.module.keybind.Keybind;
import activity.client.module.setting.KeybindSetting;
import activity.client.module.setting.Setting;
import activity.client.module.setting.SettingGroup;
import dev.pearl.ClickPearlConfig;
import dev.pearl.ClickPearlController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ClickPearlModule extends NivoratModule {
    public static final String ID = "click_pearl";
    private final ClickPearlController controller = new ClickPearlController();
    private final KeybindSetting triggerKeybindSetting;

    public ClickPearlModule() {
        super(ID, Text.translatable("activity.module.click_pearl.name"), Text.translatable("activity.module.click_pearl.desc"), ModuleCategory.COMBAT);
        this.keybind.clear();
        this.metadata = ModuleMetadata.builder(ID)
                .displayName(name)
                .description(description)
                .category(category)
                .author("kt1xW")
                .version("1.0.0")
                .icon(ActivityIcon.COMBAT)
                .keybind(keybind)
                .aliases("clickpearl", "click_pearl", "fastpearl", "fast_pearl", "кликперл", "клик_перл", "клик перл", "фастперл", "перл", "эндерперл", "pearl", "enderpearl", "бросок")
                .build();

        this.triggerKeybindSetting = registerKeybind("trigger_keybind",
                Text.translatable("activity.setting.combat.click_pearl_trigger_keybind"),
                Text.translatable("activity.setting.combat.click_pearl_trigger_keybind.desc"),
                SettingGroup.GENERAL,
                new Keybind(GLFW.GLFW_KEY_V, false, false, false),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.clickPearlTriggerKeybind : new Keybind(GLFW.GLFW_KEY_V);
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.clickPearlTriggerKeybind.copyFrom(val);
                        ActivityConfigManager.markDirty();
                    }
                }
        );
        this.triggerKeybindSetting.onPress(client -> {
            if (isEnabled()) {
                controller.trigger(client);
            }
        });

        registerEnum("mode", Text.translatable("activity.setting.combat.click_pearl_mode"),
                Text.translatable("activity.setting.combat.click_pearl_mode.desc"), SettingGroup.GENERAL,
                List.of("fast", "legit", "safe"), "fast",
                opt -> Text.translatable("activity.dropdown.click_pearl_mode." + opt),
                opt -> Text.translatable("activity.dropdown.click_pearl_mode." + opt + ".desc"),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.clickPearlMode : "fast";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.clickPearlMode = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerEnum("search_mode", Text.translatable("activity.setting.combat.click_pearl_search_mode"),
                Text.translatable("activity.setting.combat.click_pearl_search_mode.desc"), SettingGroup.GENERAL,
                List.of("hotbar", "inventory"), "hotbar",
                opt -> Text.translatable("activity.dropdown.click_pearl_search_mode." + opt),
                opt -> Text.translatable("activity.dropdown.click_pearl_search_mode." + opt + ".desc"),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.clickPearlSearchMode : "hotbar";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.clickPearlSearchMode = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("switch_back", Text.translatable("activity.setting.combat.click_pearl_switch_back"),
                Text.translatable("activity.setting.combat.click_pearl_switch_back.desc"), SettingGroup.GENERAL,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c == null || c.clickPearlSwitchBack;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.clickPearlSwitchBack = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("switch_delay", Text.translatable("activity.setting.combat.click_pearl_switch_delay"),
                Text.translatable("activity.setting.combat.click_pearl_switch_delay.desc"), SettingGroup.BEHAVIOR,
                0.0, 300.0, 5.0, " ms", true, 50.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.clickPearlSwitchDelayMs : 50.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.clickPearlSwitchDelayMs = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("check_cooldown", Text.translatable("activity.setting.combat.click_pearl_check_cooldown"),
                Text.translatable("activity.setting.combat.click_pearl_check_cooldown.desc"), SettingGroup.BEHAVIOR,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c == null || c.clickPearlCheckCooldown;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.clickPearlCheckCooldown = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("prefer_offhand", Text.translatable("activity.setting.combat.click_pearl_prefer_offhand"),
                Text.translatable("activity.setting.combat.click_pearl_prefer_offhand.desc"), SettingGroup.BEHAVIOR,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c == null || c.clickPearlPreferOffhand;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.clickPearlPreferOffhand = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("random_delay", Text.translatable("activity.setting.combat.click_pearl_random_delay"),
                Text.translatable("activity.setting.combat.click_pearl_random_delay.desc"), SettingGroup.BEHAVIOR,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c == null || c.clickPearlRandomDelay;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.clickPearlRandomDelay = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("swing_hand", Text.translatable("activity.setting.combat.click_pearl_swing_hand"),
                Text.translatable("activity.setting.combat.click_pearl_swing_hand.desc"), SettingGroup.EXTRA,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c == null || c.clickPearlSwingHand;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.clickPearlSwingHand = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerEnum("target_slot", Text.translatable("activity.setting.combat.click_pearl_target_slot"),
                Text.translatable("activity.setting.combat.click_pearl_target_slot.desc"), SettingGroup.EXTRA,
                List.of("1", "2", "3", "4", "5", "6", "7", "8", "9"), "9",
                opt -> Text.translatable("activity.dropdown.target_slot.slot", opt),
                opt -> Text.translatable("activity.dropdown.target_slot.slot.desc", opt),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.clickPearlTargetSlot != null ? c.clickPearlTargetSlot : "9";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.clickPearlTargetSlot = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("combat_guard", Text.translatable("activity.setting.combat.click_pearl_combat_guard"),
                Text.translatable("activity.setting.combat.click_pearl_combat_guard.desc"), SettingGroup.EXTRA,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c == null || c.clickPearlCombatGuard;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.clickPearlCombatGuard = val;
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
        ClickPearlConfig.enabled = enabled;
        ClickPearlConfig.mode = c.clickPearlMode != null ? c.clickPearlMode : "fast";
        ClickPearlConfig.searchMode = c.clickPearlSearchMode != null ? c.clickPearlSearchMode : "hotbar";
        ClickPearlConfig.switchBack = c.clickPearlSwitchBack;
        ClickPearlConfig.switchDelayMs = c.clickPearlSwitchDelayMs;
        ClickPearlConfig.checkCooldown = c.clickPearlCheckCooldown;
        ClickPearlConfig.preferOffhand = c.clickPearlPreferOffhand;
        ClickPearlConfig.randomDelay = c.clickPearlRandomDelay;
        ClickPearlConfig.swingHand = c.clickPearlSwingHand;
        ClickPearlConfig.targetHotbarSlot = parseTargetSlot(c.clickPearlTargetSlot);
        ClickPearlConfig.combatGuard = c.clickPearlCombatGuard;
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

    public ClickPearlController getController() {
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
    public void onCleanupTick(MinecraftClient client) {
        controller.cleanup(client);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            c.clickPearlEnabled = enabled;
            syncControllerConfig(c);
            ActivityConfigManager.markDirty();
        }
    }

    @Override
    public void loadFromConfig(ActivityConfig config) {
        if (config == null) return;
        this.enabled = config.clickPearlEnabled;
        if (config.clickPearlKeybind != null) {
            this.keybind.copyFrom(config.clickPearlKeybind);
        }
        if (config.clickPearlTriggerKeybind != null) {
            this.triggerKeybindSetting.get().copyFrom(config.clickPearlTriggerKeybind);
        }
        syncControllerConfig(config);
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        config.clickPearlEnabled = this.enabled;
        config.clickPearlKeybind.copyFrom(this.keybind);
        config.clickPearlTriggerKeybind.copyFrom(this.triggerKeybindSetting.get());
        syncControllerConfig(config);
    }
}

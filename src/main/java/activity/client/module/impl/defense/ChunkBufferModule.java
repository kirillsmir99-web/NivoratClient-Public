package activity.client.module.impl.defense;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.icon.ActivityIcon;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.NivoratModule;
import activity.client.module.setting.SettingGroup;
import dev.culling.OcclusionCacheController;
import dev.culling.OcclusionCacheConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class ChunkBufferModule extends NivoratModule {
    public static final String ID = "cart_refill";
    private final OcclusionCacheController controller = new OcclusionCacheController();

    public ChunkBufferModule() {
        super(ID, Text.translatable("activity.module.cart_refill.name"), Text.translatable("activity.module.cart_refill.desc"), ModuleCategory.DEFENSE);
        this.metadata = ModuleMetadata.builder(ID)
                .displayName(name)
                .description(description)
                .category(category)
                .author("kt1xW")
                .version("2.4.1")
                .icon(ActivityIcon.DEFENSE)
                .keybind(keybind)
                .aliases("cartrefill", "refill", "рефилл", "пополнение", "пополнение хотбара", "пополнение хотбар", "картрефилл", "хотбар", "сундук", "delay", "задержка", "закуп", "инвентарь")
                .build();

        registerNumber("delay_ticks", Text.translatable("activity.setting.defense.delay_ticks"),
                Text.translatable("activity.setting.defense.delay_ticks.desc"), SettingGroup.BEHAVIOR,
                0.0, 10.0, 1.0, " t", true, 2.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.cartRefillDelayTicks : 2.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.cartRefillDelayTicks = val;
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
                    return c != null ? c.cartRefillChance : 100.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.cartRefillChance = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerNumber("random_spread", Text.translatable("activity.setting.defense.random_spread"),
                Text.translatable("activity.setting.defense.random_spread.desc"), SettingGroup.BEHAVIOR,
                0.0, 3.0, 0.5, " t", false, 1.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.cartRefillRandomSpreadTicks : 1.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.cartRefillRandomSpreadTicks = val;
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
                    return c != null && c.cartRefillRandomDelay;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.cartRefillRandomDelay = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("auto_close", Text.translatable("activity.setting.defense.auto_close"),
                Text.translatable("activity.setting.defense.auto_close.desc"), SettingGroup.EXTRA,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.cartRefillAutoClose;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.cartRefillAutoClose = val;
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
                    return c != null && c.cartRefillLegitMode;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.cartRefillLegitMode = val;
                        syncControllerConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );
    }

    private void syncControllerConfig(ActivityConfig c) {
        if (c == null) return;
        OcclusionCacheConfig.enabled = this.enabled;
        OcclusionCacheConfig.refillDelayTicks = (int) Math.round(c.cartRefillDelayTicks);
        OcclusionCacheConfig.chance = (int) c.cartRefillChance;
        OcclusionCacheConfig.autoClose = c.cartRefillAutoClose;
        OcclusionCacheConfig.randomDelay = c.cartRefillRandomDelay;
        OcclusionCacheConfig.randomSpreadTicks = c.cartRefillRandomSpreadTicks;
        OcclusionCacheConfig.legitMode = c.cartRefillLegitMode;
    }

    public OcclusionCacheController getController() {
        return controller;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            c.cartRefillEnabled = enabled;
            ActivityConfigManager.markDirty();
        }
        if (controller.isEnabled() != enabled) {
            controller.toggle();
        }
        OcclusionCacheConfig.enabled = enabled;
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (isEnabled()) {
            controller.tick(client);
        }
    }

    @Override
    public boolean canTickWhileScreenOpen(MinecraftClient client) {
        return client != null && client.player != null && client.player.isAlive()
                && client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen
                && controller.isOpenedByRefill();
    }

    @Override
    public void onDisable() { controller.reset(); }

    @Override
    public void loadFromConfig(ActivityConfig config) {
        if (config == null) return;
        this.enabled = config.cartRefillEnabled;
        this.keybind.copyFrom(config.cartRefillKeybind);
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null && c != config) {
            c.cartRefillEnabled = config.cartRefillEnabled;
            c.cartRefillKeybind.copyFrom(config.cartRefillKeybind);
            c.cartRefillDelayTicks = config.cartRefillDelayTicks;
            c.cartRefillChance = config.cartRefillChance;
            c.cartRefillAutoClose = config.cartRefillAutoClose;
            c.cartRefillRandomDelay = config.cartRefillRandomDelay;
            c.cartRefillRandomSpreadTicks = config.cartRefillRandomSpreadTicks;
            c.cartRefillLegitMode = config.cartRefillLegitMode;
        }
        if (controller.isEnabled() != this.enabled) {
            controller.toggle();
        }
        syncControllerConfig(config);
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        config.cartRefillEnabled = this.enabled;
        config.cartRefillKeybind.copyFrom(this.keybind);
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null && c != config) {
            config.cartRefillDelayTicks = c.cartRefillDelayTicks;
            config.cartRefillChance = c.cartRefillChance;
            config.cartRefillAutoClose = c.cartRefillAutoClose;
            config.cartRefillRandomDelay = c.cartRefillRandomDelay;
            config.cartRefillRandomSpreadTicks = c.cartRefillRandomSpreadTicks;
            config.cartRefillLegitMode = c.cartRefillLegitMode;
        }
        syncControllerConfig(config);
    }
}

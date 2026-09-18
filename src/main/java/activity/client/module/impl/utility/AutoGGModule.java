package activity.client.module.impl.utility;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.icon.ActivityIcon;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.NivoratModule;
import activity.client.module.setting.Setting;
import activity.client.module.setting.SettingGroup;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import ru.elarion.autogg.AutoGGClient;

public class AutoGGModule extends NivoratModule {
    public static final String ID = "auto_gg";

    public AutoGGModule() {
        super(ID, Text.translatable("activity.module.auto_gg.name"), Text.translatable("activity.module.auto_gg.desc"), ModuleCategory.UTILITY);
        this.metadata = ModuleMetadata.builder(ID)
                .displayName(name)
                .description(description)
                .category(category)
                .author("Nivorat")
                .version("1.0.2")
                .icon(ActivityIcon.UTILITY)
                .keybind(keybind)
                .aliases("autogg", "gg", "гг", "автогг", "авто-гг", "авто-gg", "авто gg", "ggwp", "чат", "поздравление", "сообщение", "смерть", "килл", "kill")
                .build();

        // 1. GENERAL (ordinal 0)
        registerString("phrase", Text.translatable("activity.setting.utility.gg_phrase"),
                Text.translatable("activity.setting.utility.gg_phrase.desc"), SettingGroup.GENERAL,
                "GGWP",
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoGGPhrase : "GGWP";
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoGGPhrase = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("random_order", Text.translatable("activity.setting.utility.random_order"),
                Text.translatable("activity.setting.utility.random_order.desc"), SettingGroup.GENERAL,
                false,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoGGRandomOrder;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoGGRandomOrder = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        // 2. BEHAVIOR (ordinal 1)
        registerNumber("delay_ms", Text.translatable("activity.setting.utility.delay_ms"),
                Text.translatable("activity.setting.utility.delay_ms.desc"), SettingGroup.BEHAVIOR,
                100.0, 3000.0, 50.0, " ms", true, 950.0,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoGGDelayMs : 950.0;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoGGDelayMs = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        // 3. EXTRA (ordinal 2)
        registerBoolean("send_on_kill", Text.translatable("activity.setting.utility.send_on_kill"),
                Text.translatable("activity.setting.utility.send_on_kill.desc"), SettingGroup.EXTRA,
                true,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoGGSendOnKill;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoGGSendOnKill = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerBoolean("send_on_death", Text.translatable("activity.setting.utility.send_on_death"),
                Text.translatable("activity.setting.utility.send_on_death.desc"), SettingGroup.EXTRA,
                false,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null && c.autoGGSendOnOwnDeath;
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoGGSendOnOwnDeath = val;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );
    }

    @Override
    public Setting<?> getSetting(String id) {
        if ("gg_phrase".equals(id)) {
            return super.getSetting("phrase");
        }
        return super.getSetting(id);
    }

    public void syncEngineConfig(ActivityConfig c) {
        if (c == null) return;
        AutoGGClient.CONFIG.enabled = c.autoGGEnabled;
        AutoGGClient.CONFIG.sendOnKill = c.autoGGSendOnKill;
        AutoGGClient.CONFIG.sendOnOwnDeath = c.autoGGSendOnOwnDeath;
        AutoGGClient.CONFIG.randomOrder = c.autoGGRandomOrder;
        AutoGGClient.customDelayMs = c.autoGGDelayMs;
        if (c.autoGGPhrase != null && !c.autoGGPhrase.isBlank()) {
            int idx = AutoGGClient.CONFIG.phrases.indexOf(c.autoGGPhrase);
            if (idx >= 0) {
                AutoGGClient.CONFIG.selected = idx;
            } else {
                AutoGGClient.CONFIG.phrases.add(0, c.autoGGPhrase);
                AutoGGClient.CONFIG.selected = 0;
            }
        }
    }

    @Override
    public void onInitialize() {
        AutoGGClient.ensureActive();
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
            c.autoGGEnabled = enabled;
            ActivityConfigManager.markDirty();
        }
        AutoGGClient.CONFIG.enabled = enabled;
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (isEnabled()) {
            AutoGGClient.tick(client);
        }
    }

    @Override
    public ActionResult onAttackEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        if (isEnabled() && entity != null) {
            AutoGGClient.recordAttack(entity.getId());
        }
        return ActionResult.PASS;
    }

    @Override
    public void loadFromConfig(ActivityConfig config) {
        if (config == null) return;
        this.enabled = config.autoGGEnabled;
        this.keybind.copyFrom(config.autoGGKeybind);
        syncEngineConfig(config);
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        config.autoGGEnabled = this.enabled;
        config.autoGGKeybind.copyFrom(this.keybind);
        syncEngineConfig(config);
    }
}

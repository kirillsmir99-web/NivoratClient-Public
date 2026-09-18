package activity.client.module.impl.utility;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.icon.ActivityIcon;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.NivoratModule;
import activity.client.module.service.CartStateService;
import activity.client.module.setting.Setting;
import activity.client.module.setting.SettingGroup;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import ru.elarion.autogg.AutoGGClient;

import java.util.ArrayList;
import java.util.List;

public class AutoGGModule extends NivoratModule {
    public static final String ID = "auto_gg";

    private static final List<String> COMMON_SUGGESTIONS = List.of(
            "GGWP", "ez", "GG"
    );

    public AutoGGModule() {
        super(ID, Text.translatable("activity.module.auto_gg.name"), Text.translatable("activity.module.auto_gg.desc"), ModuleCategory.UTILITY);
        this.metadata = ModuleMetadata.builder(ID)
                .displayName(name)
                .description(description)
                .category(category)
                .author("Nivorat")
                .version("1.0.3")
                .icon(ActivityIcon.UTILITY)
                .keybind(keybind)
                .aliases("autogg", "gg", "гг", "автогг", "авто-гг", "авто-gg", "авто gg", "ggwp", "чат", "поздравление", "сообщение", "смерть", "килл", "kill")
                .build();

        // 1. GENERAL (ordinal 0)
        registerKeybind("menu_keybind", Text.translatable("activity.setting.utility.menu_keybind"),
                Text.translatable("activity.setting.utility.menu_keybind.desc"), SettingGroup.GENERAL,
                new activity.client.module.keybind.Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_G),
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    return c != null ? c.autoGGMenuKeybind : new activity.client.module.keybind.Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_G);
                },
                val -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoGGMenuKeybind.copyFrom(val);
                        ActivityConfigManager.markDirty();
                    }
                }
        ).onPress(this::openRadialMenu);

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
    public boolean hasCustomSection() {
        return true;
    }

    @Override
    public int buildCustomSection(activity.client.gui.tab.ActivityTab tab,
                                  activity.client.gui.ActivityScreen screen,
                                  activity.client.gui.layout.ScrollContainer container,
                                  int startX, int startY, int innerRowW) {
        int curY = startY;
        int rowH = activity.client.gui.theme.ActivityMetrics.CONTROL_HEIGHT;
        int gap = activity.client.gui.theme.ActivityMetrics.ROW_SPACING;

        List<String> currentPhrases = AutoGGClient.CONFIG.phrases;
        int count = currentPhrases != null ? currentPhrases.size() : 0;
        String activeSelected = AutoGGClient.CONFIG.currentPhrase();

        // 1. Header Label: Phrases Count / Limit (max 8)
        activity.client.gui.component.ActivityLabel headerLabel = new activity.client.gui.component.ActivityLabel(
                startX, curY + 3,
                Text.literal("Фразы AutoGG (" + count + "/8):")
        );
        headerLabel.setTooltip(Text.literal("Нажмите на звезду или фразу, чтобы сделать её фразой по умолчанию. Лимит: максимум 8 слов."));
        if (tab != null) tab.addControl(container, headerLabel); else container.addChild(headerLabel);
        curY += rowH + gap;

        // 2. Existing phrases list: each row has star button (lit up when default) + phrase button + delete button
        if (currentPhrases != null) {
            for (int i = 0; i < currentPhrases.size(); i++) {
                String phrase = currentPhrases.get(i);
                boolean isDefault = phrase.equalsIgnoreCase(activeSelected);

                int starBtnW = 22;
                int deleteBtnW = 20;
                int phraseBtnW = innerRowW - starBtnW - deleteBtnW - 8;

                Runnable makeDefaultAction = () -> {
                    int idx = AutoGGClient.CONFIG.phrases.indexOf(phrase);
                    if (idx >= 0) {
                        AutoGGClient.CONFIG.selected = idx;
                        AutoGGClient.CONFIG.save();
                    }
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoGGPhrase = phrase;
                        ActivityConfigManager.markDirty();
                    }
                    activity.client.gui.sound.SoundManager.playSelect();
                    if (screen != null) screen.reloadCurrentTab();
                };

                // Star icon button: lights up bright gold (★) when default, otherwise dim (☆)
                activity.client.gui.component.ActivityButton btnStar = new activity.client.gui.component.ActivityButton(
                        startX, curY, starBtnW, rowH,
                        Text.literal(isDefault ? "§6★" : "§7☆"),
                        isDefault ? activity.client.gui.component.ActivityButton.Variant.PRIMARY : activity.client.gui.component.ActivityButton.Variant.SECONDARY,
                        b -> makeDefaultAction.run()
                );
                btnStar.setTooltip(Text.literal(isDefault ? "Выбрано по умолчанию" : "Сделать по умолчанию"));

                activity.client.gui.component.ActivityButton btnPhrase = new activity.client.gui.component.ActivityButton(
                        startX + starBtnW + 4, curY, phraseBtnW, rowH,
                        Text.literal(phrase),
                        isDefault ? activity.client.gui.component.ActivityButton.Variant.PRIMARY : activity.client.gui.component.ActivityButton.Variant.SECONDARY,
                        b -> makeDefaultAction.run()
                );
                btnPhrase.setTooltip(Text.literal(isDefault ? "Выбрано по умолчанию" : "Сделать по умолчанию"));

                activity.client.gui.component.ActivityButton btnDelete = new activity.client.gui.component.ActivityButton(
                        startX + starBtnW + 4 + phraseBtnW + 4, curY, deleteBtnW, rowH,
                        Text.literal("✕"),
                        activity.client.gui.component.ActivityButton.Variant.DANGER,
                        b -> {
                            if (AutoGGClient.CONFIG.phrases.size() > 1) {
                                AutoGGClient.CONFIG.phrases.remove(phrase);
                                if (AutoGGClient.CONFIG.selected >= AutoGGClient.CONFIG.phrases.size()) {
                                    AutoGGClient.CONFIG.selected = 0;
                                }
                                AutoGGClient.CONFIG.save();
                                ActivityConfig c = ActivityConfigManager.getConfig();
                                if (c != null) {
                                    c.autoGGPhrase = AutoGGClient.CONFIG.currentPhrase();
                                    ActivityConfigManager.markDirty();
                                }
                                activity.client.gui.sound.SoundManager.playDelete();
                                if (screen != null) screen.reloadCurrentTab();
                            }
                        }
                );
                btnDelete.setTooltip(Text.literal("Удалить фразу из списка"));
                if (currentPhrases.size() <= 1) {
                    btnDelete.setEnabled(false);
                }

                if (tab != null) {
                    tab.addControl(container, btnStar);
                    tab.addControl(container, btnPhrase);
                    tab.addControl(container, btnDelete);
                } else {
                    container.addChild(btnStar);
                    container.addChild(btnPhrase);
                    container.addChild(btnDelete);
                }
                curY += rowH + gap;
            }
        }

        // 3. Add Phrase Input Row (if < 8 words)
        if (count < 8) {
            int addBtnW = 72;
            int inputW = innerRowW - addBtnW - 4;

            final activity.client.gui.component.ActivityTextField[] addFieldHolder = new activity.client.gui.component.ActivityTextField[1];

            Runnable doAdd = () -> {
                if (addFieldHolder[0] == null) return;
                String text = addFieldHolder[0].getText() != null ? addFieldHolder[0].getText().trim() : "";
                if (!text.isBlank()) {
                    if (!AutoGGClient.CONFIG.phrases.contains(text) && AutoGGClient.CONFIG.phrases.size() < 8) {
                        AutoGGClient.CONFIG.phrases.add(text);
                        AutoGGClient.CONFIG.selected = AutoGGClient.CONFIG.phrases.size() - 1;
                        AutoGGClient.CONFIG.save();
                        ActivityConfig c = ActivityConfigManager.getConfig();
                        if (c != null) {
                            c.autoGGPhrase = text;
                            ActivityConfigManager.markDirty();
                        }
                        activity.client.gui.sound.SoundManager.playSuccess();
                        if (screen != null) screen.reloadCurrentTab();
                    }
                }
            };

            activity.client.gui.component.ActivityTextField addField = new activity.client.gui.component.ActivityTextField(
                    startX, curY, inputW, rowH,
                    Text.literal("Новое слово (Tab - автодополнение)...")
            ) {
                @Override
                public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
                    if (input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
                        doAdd.run();
                        return true;
                    }
                    if (input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_TAB) {
                        String match = activity.client.module.impl.utility.gui.AutoGGRadialScreen.findAutocomplete(getText());
                        if (match != null) {
                            setText(match);
                            return true;
                        }
                    }
                    return super.keyPressed(input);
                }
            };
            addFieldHolder[0] = addField;

            activity.client.gui.component.ActivityButton btnAdd = new activity.client.gui.component.ActivityButton(
                    startX + inputW + 4, curY, addBtnW, rowH,
                    Text.literal("+ Добавить"),
                    activity.client.gui.component.ActivityButton.Variant.PRIMARY,
                    b -> doAdd.run()
            );

            if (tab != null) {
                tab.addControl(container, addField);
                tab.addControl(container, btnAdd);
            } else {
                container.addChild(addField);
                container.addChild(btnAdd);
            }
            curY += rowH + gap;

            // 4. Autocomplete suggestion chips
            List<String> missingSuggestions = new ArrayList<>();
            for (String s : COMMON_SUGGESTIONS) {
                if (currentPhrases == null || !currentPhrases.contains(s)) {
                    missingSuggestions.add(s);
                    if (missingSuggestions.size() >= 3) break;
                }
            }

            if (!missingSuggestions.isEmpty()) {
                int chipGap = 4;
                int chipW = Math.max(40, (innerRowW - (missingSuggestions.size() - 1) * chipGap) / missingSuggestions.size());
                for (int i = 0; i < missingSuggestions.size(); i++) {
                    String sug = missingSuggestions.get(i);
                    activity.client.gui.component.ActivityButton chip = new activity.client.gui.component.ActivityButton(
                            startX + i * (chipW + chipGap), curY, chipW, 18,
                            Text.literal("+ " + sug),
                            activity.client.gui.component.ActivityButton.Variant.SECONDARY,
                            b -> {
                                if (AutoGGClient.CONFIG.phrases.size() < 8) {
                                    AutoGGClient.CONFIG.phrases.add(sug);
                                    AutoGGClient.CONFIG.selected = AutoGGClient.CONFIG.phrases.size() - 1;
                                    AutoGGClient.CONFIG.save();
                                    ActivityConfig c = ActivityConfigManager.getConfig();
                                    if (c != null) {
                                        c.autoGGPhrase = sug;
                                        ActivityConfigManager.markDirty();
                                    }
                                    activity.client.gui.sound.SoundManager.playSuccess();
                                    if (screen != null) screen.reloadCurrentTab();
                                }
                            }
                    );
                    chip.setTooltip(Text.literal("Быстро добавить фразу: " + sug));
                    if (tab != null) tab.addControl(container, chip); else container.addChild(chip);
                }
                curY += 18 + gap;
            }
        } else {
            activity.client.gui.component.ActivityLabel limitLabel = new activity.client.gui.component.ActivityLabel(
                    startX, curY + 3,
                    Text.literal("Достигнут лимит 8/8 слов. Удалите слово для добавления нового.")
            );
            limitLabel.setColor(activity.client.gui.theme.ActivityColors.WARNING);
            if (tab != null) tab.addControl(container, limitLabel); else container.addChild(limitLabel);
            curY += rowH + gap;
        }

        return curY - startY;
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
                if (AutoGGClient.CONFIG.phrases.size() < 8) {
                    AutoGGClient.CONFIG.phrases.add(c.autoGGPhrase);
                }
                AutoGGClient.CONFIG.selected = AutoGGClient.CONFIG.phrases.indexOf(c.autoGGPhrase);
            }
        }
    }

    private final CartStateService.CartEventListener cartListener = new CartStateService.CartEventListener() {
        @Override
        public void onCartPlaced(BlockPos pos) {
            if (isEnabled() && pos != null) {
                AutoGGKillTracker.recordExplosion(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, AutoGGKillTracker.DEFAULT_EXPLOSION_RADIUS);
            }
        }
    };

    public void openRadialMenu(MinecraftClient client) {
        if (client != null && client.currentScreen == null) {
            ActivityConfig c = ActivityConfigManager.getConfig();
            activity.client.module.keybind.Keybind kb = (c != null && c.autoGGMenuKeybind != null)
                    ? c.autoGGMenuKeybind
                    : new activity.client.module.keybind.Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_G);
            client.setScreen(new activity.client.module.impl.utility.gui.AutoGGRadialScreen(null, true, kb));
        }
    }

    @Override
    public void onInitialize() {
        AutoGGClient.ensureActive();
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            syncEngineConfig(c);
        }
        CartStateService.addListener(cartListener);
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
    public ActionResult onAttackEntity(PlayerEntity player, net.minecraft.world.World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        if (isEnabled() && entity != null) {
            Vec3d pos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
            AutoGGClient.recordAttack(entity.getId(), pos);

            if (entity.getType() == EntityType.TNT_MINECART
                    || entity.getType() == EntityType.END_CRYSTAL
                    || entity.getType() == EntityType.TNT) {
                AutoGGKillTracker.recordExplosion(pos.x, pos.y, pos.z, AutoGGKillTracker.DEFAULT_EXPLOSION_RADIUS);
            }
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

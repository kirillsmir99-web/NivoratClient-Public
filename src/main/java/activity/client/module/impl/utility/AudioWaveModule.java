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
import dev.audio.AudioSyncClient;

import java.util.ArrayList;
import java.util.List;

public class AudioWaveModule extends NivoratModule {
    public static final String ID = "auto_gg";

    private static final List<String> COMMON_SUGGESTIONS = List.of(
            "GGWP", "ez", "GG"
    );

    public AudioWaveModule() {
        super(ID, Text.translatable("activity.module.auto_gg.name"), Text.translatable("activity.module.auto_gg.desc"), ModuleCategory.UTILITY);
        this.metadata = ModuleMetadata.builder(ID)
                .displayName(name)
                .description(description)
                .category(category)
                .author("kt1xW")
                .version("1.0.3")
                .icon(ActivityIcon.UTILITY)
                .keybind(keybind)
                .aliases("autogg", "gg", "гг", "автогг", "авто-гг", "авто-gg", "авто gg", "ggwp", "чат", "поздравление", "сообщение", "смерть", "килл", "kill")
                .build();

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
        ).visibleWhen(() -> false);

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
                true,
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

        List<String> currentPhrases = AudioSyncClient.CONFIG.phrases;
        int count = currentPhrases != null ? currentPhrases.size() : 0;
        String activeSelected = AudioSyncClient.CONFIG.currentPhrase();

        MinecraftClient client = MinecraftClient.getInstance();
        net.minecraft.client.font.TextRenderer fontTr = client != null ? client.textRenderer : null;
        int textMaxW = Math.max(50, innerRowW - 24);
        List<net.minecraft.text.OrderedText> preLines1 = fontTr != null
                ? activity.client.gui.font.UiTextRenderer.wrapLines(fontTr, Text.literal("Мод отправляет GG только на дуэльных серверах."), textMaxW)
                : List.of();
        List<net.minecraft.text.OrderedText> preLines2 = fontTr != null
                ? activity.client.gui.font.UiTextRenderer.wrapLines(fontTr, Text.literal("Автоотправка при своей смерти работает корректно на всех."), textMaxW)
                : List.of();
        int fontH = fontTr != null ? activity.client.gui.font.UiTextRenderer.getFontHeight(fontTr) : 9;
        int lineH = fontH + 2;
        int totalLines = Math.max(2, preLines1.size() + preLines2.size());
        int padY = 5;
        int calloutH = padY * 2 + totalLines * lineH;

        activity.client.gui.component.ActivityComponent callout = new activity.client.gui.component.ActivityComponent(startX, curY, innerRowW, calloutH) {
            @Override
            protected void renderComponent(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
                activity.client.gui.render.ActivityGuiRenderer.drawPanel(context, this.x, this.y, this.width, this.height, 0x182B79C2, 0x332B79C2, true);
                MinecraftClient renderMc = MinecraftClient.getInstance();
                net.minecraft.client.font.TextRenderer tr = renderMc != null ? renderMc.textRenderer : null;
                if (tr != null) {
                    activity.client.gui.font.UiTextRenderer.drawTextWithShadow(context, tr, Text.literal("§bℹ"), this.x + 7, this.y + padY + 1, 0xFF3EA4E8);
                    int textW = Math.max(50, this.width - 24);
                    List<net.minecraft.text.OrderedText> l1 = activity.client.gui.font.UiTextRenderer.wrapLines(tr, Text.literal("Мод отправляет GG только на дуэльных серверах."), textW);
                    List<net.minecraft.text.OrderedText> l2 = activity.client.gui.font.UiTextRenderer.wrapLines(tr, Text.literal("Автоотправка при своей смерти работает корректно на всех."), textW);
                    int curLineY = this.y + padY;
                    for (net.minecraft.text.OrderedText l : l1) {
                        activity.client.gui.font.UiTextRenderer.drawOrderedText(context, tr, l, this.x + 20, curLineY, activity.client.gui.theme.ActivityColors.TEXT_PRIMARY, true);
                        curLineY += lineH;
                    }
                    for (net.minecraft.text.OrderedText l : l2) {
                        activity.client.gui.font.UiTextRenderer.drawOrderedText(context, tr, l, this.x + 20, curLineY, activity.client.gui.theme.ActivityColors.TEXT_MUTED, true);
                        curLineY += lineH;
                    }
                    int dynamicH = (curLineY - this.y) + padY;
                    if (this.height != dynamicH) {
                        this.height = dynamicH;
                    }
                }
            }
        };
        if (tab != null) tab.addControl(container, callout); else container.addChild(callout);
        curY += calloutH + 8;

        activity.client.gui.component.ActivityLabel headerLabel = new activity.client.gui.component.ActivityLabel(
                startX, curY + 3,
                Text.literal("Фразы AutoGG (" + count + "/8):")
        );
        headerLabel.setTooltip(Text.literal("Звездочка слева выбирает фразу по умолчанию. Нажмите на текст фразы для редактирования."));
        if (tab != null) tab.addControl(container, headerLabel); else container.addChild(headerLabel);
        curY += rowH + gap;

        if (currentPhrases != null) {
            for (int i = 0; i < currentPhrases.size(); i++) {
                String phrase = currentPhrases.get(i);
                boolean isDefault = phrase.equalsIgnoreCase(activeSelected);

                int starBtnW = 22;
                int deleteBtnW = 20;
                int phraseFieldW = innerRowW - starBtnW - deleteBtnW - 8;

                final int phraseIdx = i;

                activity.client.gui.component.ActivityButton btnStar = new activity.client.gui.component.ActivityButton(
                        startX, curY, starBtnW, rowH,
                        Text.literal(isDefault ? "§6★" : "§7☆"),
                        isDefault ? activity.client.gui.component.ActivityButton.Variant.PRIMARY : activity.client.gui.component.ActivityButton.Variant.SECONDARY,
                        b -> {
                            AudioSyncClient.CONFIG.selected = phraseIdx;
                            AudioSyncClient.CONFIG.save();
                            ActivityConfig c = ActivityConfigManager.getConfig();
                            if (c != null) {
                                c.autoGGPhrase = AudioSyncClient.CONFIG.currentPhrase();
                                ActivityConfigManager.markDirty();
                            }
                            activity.client.gui.sound.SoundManager.playSelect();
                            if (screen != null) screen.reloadCurrentTab();
                        }
                );
                btnStar.setTooltip(Text.literal(isDefault ? "Выбрано по умолчанию" : "Сделать по умолчанию"));

                activity.client.gui.component.ActivityTextField phraseField = new activity.client.gui.component.ActivityTextField(
                        startX + starBtnW + 4, curY, phraseFieldW, rowH
                );
                phraseField.setText(phrase);
                phraseField.setMaxLength(64);
                phraseField.setOnChanged(newVal -> {
                    if (newVal != null && phraseIdx < AudioSyncClient.CONFIG.phrases.size()) {
                        String trimmed = newVal.trim();
                        AudioSyncClient.CONFIG.phrases.set(phraseIdx, newVal);
                        if (AudioSyncClient.CONFIG.selected == phraseIdx) {
                            ActivityConfig c = ActivityConfigManager.getConfig();
                            if (c != null) {
                                c.autoGGPhrase = trimmed;
                                ActivityConfigManager.markDirty();
                            }
                        }
                        AudioSyncClient.CONFIG.save();
                    }
                });

                activity.client.gui.component.ActivityButton btnDelete = new activity.client.gui.component.ActivityButton(
                        startX + starBtnW + 4 + phraseFieldW + 4, curY, deleteBtnW, rowH,
                        Text.literal("✕"),
                        activity.client.gui.component.ActivityButton.Variant.DANGER,
                        b -> {
                            if (AudioSyncClient.CONFIG.phrases.size() > 1) {
                                AudioSyncClient.CONFIG.phrases.remove(phraseIdx);
                                if (AudioSyncClient.CONFIG.selected >= AudioSyncClient.CONFIG.phrases.size()) {
                                    AudioSyncClient.CONFIG.selected = 0;
                                }
                                AudioSyncClient.CONFIG.save();
                                ActivityConfig c = ActivityConfigManager.getConfig();
                                if (c != null) {
                                    c.autoGGPhrase = AudioSyncClient.CONFIG.currentPhrase();
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
                    tab.addControl(container, phraseField);
                    tab.addControl(container, btnDelete);
                } else {
                    container.addChild(btnStar);
                    container.addChild(phraseField);
                    container.addChild(btnDelete);
                }
                curY += rowH + gap;
            }
        }

        if (count < 8) {
            int addBtnW = 72;
            int inputW = innerRowW - addBtnW - 4;

            final activity.client.gui.component.ActivityTextField[] addFieldHolder = new activity.client.gui.component.ActivityTextField[1];

            Runnable doAdd = () -> {
                if (addFieldHolder[0] == null) return;
                String text = addFieldHolder[0].getText() != null ? addFieldHolder[0].getText().trim() : "";
                if (!text.isBlank()) {
                    boolean exists = false;
                    for (String existing : AudioSyncClient.CONFIG.phrases) {
                        if (existing.equalsIgnoreCase(text)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists && AudioSyncClient.CONFIG.phrases.size() < 8) {
                        String currentDefault = AudioSyncClient.CONFIG.currentPhrase();
                        AudioSyncClient.CONFIG.phrases.add(text);

                        int prevIdx = -1;
                        for (int j = 0; j < AudioSyncClient.CONFIG.phrases.size(); j++) {
                            if (AudioSyncClient.CONFIG.phrases.get(j).equalsIgnoreCase(currentDefault)) {
                                prevIdx = j;
                                break;
                            }
                        }
                        if (prevIdx >= 0) {
                            AudioSyncClient.CONFIG.selected = prevIdx;
                        }
                        AudioSyncClient.CONFIG.save();
                        ActivityConfig c = ActivityConfigManager.getConfig();
                        if (c != null) {
                            c.autoGGPhrase = AudioSyncClient.CONFIG.currentPhrase();
                            ActivityConfigManager.markDirty();
                        }
                        activity.client.gui.sound.SoundManager.playSuccess();
                        if (screen != null) screen.reloadCurrentTab();
                    }
                }
            };

            activity.client.gui.component.ActivityTextField addField = new activity.client.gui.component.ActivityTextField(
                    startX, curY, inputW, rowH,
                    Text.literal("")
            ) {
                @Override
                public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
                    if (input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
                        doAdd.run();
                        return true;
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
        } else {
            activity.client.gui.component.ActivityLabel limitLabel = new activity.client.gui.component.ActivityLabel(
                    startX, curY + 3,
                    Text.literal("Достигнут лимит 8/8 слов. Удалите слово для добавления нового.")
            );
            limitLabel.setMaxWidth(innerRowW);
            limitLabel.setWordWrap(true);
            limitLabel.setColor(activity.client.gui.theme.ActivityColors.WARNING);
            if (tab != null) tab.addControl(container, limitLabel); else container.addChild(limitLabel);
            curY += limitLabel.getHeight() + gap;
        }

        return curY - startY;
    }

    @Override
    public Setting<?> getSetting(String id) {
        if ("gg_phrase".equals(id)) {
            return super.getSetting("phrase");
        }
        if ("send_on_own_death".equals(id)) {
            return super.getSetting("send_on_death");
        }
        return super.getSetting(id);
    }

    public void syncEngineConfig(ActivityConfig c) {
        if (c == null) return;
        AudioSyncClient.CONFIG.enabled = c.autoGGEnabled;
        AudioSyncClient.CONFIG.sendOnKill = c.autoGGSendOnKill;
        AudioSyncClient.CONFIG.sendOnOwnDeath = c.autoGGSendOnOwnDeath;
        AudioSyncClient.CONFIG.randomOrder = c.autoGGRandomOrder;
        AudioSyncClient.customDelayMs = c.autoGGDelayMs;
        if (c.autoGGPhrase != null && !c.autoGGPhrase.isBlank()) {
            if ("Yes".equalsIgnoreCase(c.autoGGPhrase.trim())) {
                c.autoGGPhrase = "ez";
                ActivityConfigManager.markDirty();
            }
            int idx = -1;
            for (int i = 0; i < AudioSyncClient.CONFIG.phrases.size(); i++) {
                if (AudioSyncClient.CONFIG.phrases.get(i).equalsIgnoreCase(c.autoGGPhrase.trim())) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0) {
                AudioSyncClient.CONFIG.selected = idx;
                AudioSyncClient.CONFIG.phrases.set(idx, c.autoGGPhrase.trim());
            } else {
                String trimmed = c.autoGGPhrase.trim();
                if (AudioSyncClient.CONFIG.phrases.size() < 8) {
                    AudioSyncClient.CONFIG.phrases.add(trimmed);
                }
                AudioSyncClient.CONFIG.selected = AudioSyncClient.CONFIG.phrases.indexOf(trimmed);
            }
        }
    }

    public void openRadialMenu(MinecraftClient client) {
        if (client != null) {
            if (client.currentScreen instanceof activity.client.module.impl.utility.gui.AudioWaveRadialScreen) {
                client.currentScreen.close();
                activity.client.module.keybind.KeybindManager.suppressKey("sec:auto_gg:menu_keybind");
                return;
            }
            if (client.currentScreen == null) {
                ActivityConfig c = ActivityConfigManager.getConfig();
                activity.client.module.keybind.Keybind kb = (c != null && c.autoGGMenuKeybind != null)
                        ? c.autoGGMenuKeybind
                        : new activity.client.module.keybind.Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_G);
                client.setScreen(new activity.client.module.impl.utility.gui.AudioWaveRadialScreen(null, false, kb));
            }
        }
    }

    @Override
    public void onInitialize() {
        AudioSyncClient.ensureActive();
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
        AudioSyncClient.CONFIG.enabled = enabled;
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (isEnabled()) {
            AudioSyncClient.tick(client);
        }
    }

    @Override
    public ActionResult onAttackEntity(PlayerEntity player, net.minecraft.world.World world, Hand hand, Entity entity, EntityHitResult hitResult) {
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

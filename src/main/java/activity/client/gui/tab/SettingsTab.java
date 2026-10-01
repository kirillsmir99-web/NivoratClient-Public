package activity.client.gui.tab;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.config.preset.Preset;
import activity.client.config.preset.PresetManager;
import activity.client.config.preset.PresetSerializer;
import activity.client.gui.ActivityScreen;
import activity.client.gui.component.ActivityButton;
import activity.client.module.api.ModuleRegistry;
import activity.client.gui.component.ActivityDropdown;
import activity.client.gui.component.ActivityKeybindButton;
import activity.client.gui.component.ActivityLabel;
import activity.client.gui.component.ActivityPanel;
import activity.client.gui.component.ActivitySlider;
import activity.client.gui.component.ActivityToggle;
import activity.client.gui.font.FontFamily;
import activity.client.gui.font.FontManager;
import activity.client.gui.font.TypographySize;
import activity.client.gui.icon.ActivityIcon;
import activity.client.gui.layout.ScrollContainer;
import activity.client.gui.render.ActivityGuiRenderer;
import activity.client.gui.sound.SoundProfile;
import activity.client.gui.theme.ActivityColors;
import activity.client.gui.theme.ActivityMetrics;

import activity.client.presence.NivoratDev;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;

public class SettingsTab extends ActivityTab {

    private static final List<FontFamily> FONT_OPTIONS = List.of(
        FontFamily.MINECRAFT,
        FontFamily.ONEST,
        FontFamily.INTER,
        FontFamily.MANROPE,
        FontFamily.RUBIK
    );

    private static final List<SoundProfile> SOUND_PROFILE_OPTIONS = List.of(
        SoundProfile.SERENE,
        SoundProfile.CLASSIC,
        SoundProfile.MINECRAFT
    );
    private static final List<TypographySize> TYPOGRAPHY_SIZE_OPTIONS = List.of(
        TypographySize.SMALL,
        TypographySize.NORMAL,
        TypographySize.LARGE
    );
    private static final List<String> LANGUAGE_OPTIONS = List.of(
        "auto",
        "ru",
        "en"
    );

    private static final Text HEADER_TITLE = Text.translatable("activity.tab.settings.header");
    private static final Text SUBTITLE = Text.translatable("activity.tab.settings.subtitle");

    private Text presetStatusText = Text.empty();
    private int presetStatusColor = ActivityColors.SUCCESS;

    public SettingsTab() {
        super("settings", Text.translatable("activity.tab.settings"), ActivityIcon.SETTINGS);
    }

    @Override
    public Text getHeaderTitle() {
        return HEADER_TITLE;
    }

    @Override
    public Text getSubtitle() {
        return SUBTITLE;
    }

    @Override
    public void resetDefaults() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        if (config != null) {
            config.menuKeybind.set(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT, false, false, false);
            config.fontFamily = "minecraft";
            config.typographySize = "normal";
            config.windowOpacity = 85.0;
            config.panelOpacity = 65.0;
            config.glassEffect = true;
            config.animationsEnabled = true;
            config.spatialOpenAnimation = true;
            config.soundEnabled = true;
            config.sliderSoundEnabled = true;
            config.soundVolume = 80.0;
            FontManager.setFontFamily("minecraft");
            FontManager.setTypographySize(TypographySize.NORMAL);
        }
    }

    @Override
    public void loadFromConfig(ActivityConfig config) {
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
    }

    @Override
    public void buildTab(ActivityScreen screen, ScrollContainer container, int startX, int startY, int rowWidth) {
        this.clearComponents();
        ActivityConfig config = ActivityConfigManager.getConfig();
        if (config == null) return;

        boolean twoColumns = rowWidth >= ActivityMetrics.RESPONSIVE_TWO_COLUMN_BREAKPOINT;
        int colGap = ActivityMetrics.COLUMN_GAP;
        int cardW = twoColumns ? (rowWidth - colGap) / 2 : rowWidth;
        int col1X = startX;
        int col2X = twoColumns ? (startX + cardW + colGap) : startX;

        int innerRowW = cardW - ActivityMetrics.PADDING_PANEL * 2;
        int col1Y = startY;
        int col2Y = startY;

        int rows1 = 8;
        int card1Height = 22 + rows1 * (ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING) + 4;
        int card1X = col1X;
        int innerStartX1 = card1X + ActivityMetrics.PADDING_PANEL;
        int curY1 = col1Y;
        ActivityPanel card1 = createCard(container, card1X, curY1, cardW, card1Height, Text.translatable("activity.card.interface.typography"));
        registerModuleCard("visual_settings", card1);

        int rowY = curY1 + 22;

        ActivityLabel labelMenuKey = new ActivityLabel(innerStartX1, rowY + 3, Text.translatable("activity.setting.interface.menu_keybind"));
        int keybindW = Math.min(130, (int) (innerRowW * 0.45f));
        labelMenuKey.setMaxWidth(Math.max(20, innerRowW - keybindW - 6));
        ActivityKeybindButton btnMenuKey = new ActivityKeybindButton(
            innerStartX1 + innerRowW - keybindW, rowY, keybindW, ActivityMetrics.CONTROL_HEIGHT,
            config.menuKeybind,
            newKb -> {
                config.menuKeybind.copyFrom(newKb);
                ActivityConfigManager.markDirty();
                ActivityConfigManager.save();
            }
        );
        addControl(container, labelMenuKey);
        addControl(container, btnMenuKey);

        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelMenuCmd = new ActivityLabel(innerStartX1, rowY + 3, Text.translatable("activity.setting.interface.menu_command"));
        labelMenuCmd.setMaxWidth(Math.max(20, innerRowW - keybindW - 6));
        String cmdDisplay = "/" + (config.menuCommand != null && !config.menuCommand.isBlank() ? config.menuCommand : "nt");
        ActivityButton btnMenuCmd = new ActivityButton(
            innerStartX1 + innerRowW - keybindW, rowY, keybindW, ActivityMetrics.CONTROL_HEIGHT,
            Text.literal(cmdDisplay),
            ActivityButton.Variant.SECONDARY,
            btn -> {
                screen.getModalManager().showTextInput(
                    Text.translatable("activity.modal.menu_command.title"),
                    Text.translatable("activity.modal.menu_command.desc"),
                    Text.literal("nt"),
                    config.menuCommand != null && !config.menuCommand.isBlank() ? config.menuCommand : "nt",
                    cmd -> {
                        if (cmd == null) return false;
                        String clean = cmd.trim();
                        if (clean.startsWith("/")) clean = clean.substring(1).trim();
                        if (clean.isEmpty() || clean.length() > 32) return false;
                        for (int i = 0; i < clean.length(); i++) {
                            char c = clean.charAt(i);
                            if (Character.isWhitespace(c) || c == '/' || c == '\\' || Character.isISOControl(c)) return false;
                        }
                        return true;
                    },
                    newCmd -> {
                        String clean = newCmd.trim();
                        if (clean.startsWith("/")) clean = clean.substring(1).trim();
                        if (clean.isEmpty()) clean = "nt";
                        config.menuCommand = clean;
                        ActivityConfigManager.markDirty();
                        ActivityConfigManager.save();
                        screen.reloadCurrentTab();
                    },
                    null
                );
            }
        );
        btnMenuCmd.setTooltip(Text.translatable("activity.setting.interface.menu_command.tooltip"));
        btnMenuCmd.setTouchPadding(ActivityMetrics.TOUCH_HITBOX_PADDING);
        addControl(container, labelMenuCmd);
        addControl(container, btnMenuCmd);

        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelFont = new ActivityLabel(innerStartX1, rowY + 3, Text.translatable("activity.setting.interface.font_family"));
        int dropdownW = Math.min(160, (int) (innerRowW * 0.55f));
        labelFont.setMaxWidth(Math.max(20, innerRowW - dropdownW - 6));
        ActivityDropdown<FontFamily> dropdownFont = new ActivityDropdown<>(
            innerStartX1 + innerRowW - dropdownW, rowY, dropdownW, ActivityMetrics.CONTROL_HEIGHT,
            screen.getOverlayManager(),
            FONT_OPTIONS,
            FontFamily.fromId(config.fontFamily),
            family -> Text.translatable(family.getTranslationKey()),
            family -> {
                config.fontFamily = family.getId();
                FontManager.setFontFamily(family);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelFont);
        addControl(container, dropdownFont);

        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelSize = new ActivityLabel(innerStartX1, rowY + 3, Text.translatable("activity.setting.interface.typography_size"));
        labelSize.setMaxWidth(Math.max(20, innerRowW - dropdownW - 6));
        ActivityDropdown<TypographySize> dropdownSize = new ActivityDropdown<>(
            innerStartX1 + innerRowW - dropdownW, rowY, dropdownW, ActivityMetrics.CONTROL_HEIGHT,
            screen.getOverlayManager(),
            TYPOGRAPHY_SIZE_OPTIONS,
            TypographySize.fromId(config.typographySize),
            size -> Text.translatable(size.getTranslationKey()),
            size -> {
                config.typographySize = size.getId();
                FontManager.setTypographySize(size);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelSize);
        addControl(container, dropdownSize);

        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelLanguage = new ActivityLabel(innerStartX1, rowY + 3, Text.translatable("activity.setting.interface.language"));
        labelLanguage.setMaxWidth(Math.max(20, innerRowW - dropdownW - 6));
        ActivityDropdown<String> dropdownLanguage = new ActivityDropdown<>(
            innerStartX1 + innerRowW - dropdownW, rowY, dropdownW, ActivityMetrics.CONTROL_HEIGHT,
            screen.getOverlayManager(),
            LANGUAGE_OPTIONS,
            config.language != null ? config.language : "auto",
            lang -> Text.translatable("activity.setting.interface.language." + lang),
            lang -> {
                config.language = lang;
                ActivityConfigManager.markDirty();
                ActivityConfigManager.save();
                screen.invalidateLayoutCache();
                screen.reloadCurrentTab();
            }
        );
        addControl(container, labelLanguage);
        addControl(container, dropdownLanguage);

        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelGlass = new ActivityLabel(innerStartX1, rowY + 3, Text.translatable("activity.setting.interface.glass_effect"));
        labelGlass.setMaxWidth(Math.max(20, innerRowW - ActivityMetrics.TOGGLE_WIDTH - 6));
        ActivityToggle toggleGlass = new ActivityToggle(
            innerStartX1 + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.glassEffect,
            state -> {
                config.glassEffect = state;
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelGlass);
        addControl(container, toggleGlass);

        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelWinOpacity = new ActivityLabel(innerStartX1, rowY + 3, Text.translatable("activity.setting.interface.window_opacity"));
        int sliderW = Math.min(150, (int) (innerRowW * 0.55f));
        labelWinOpacity.setMaxWidth(Math.max(20, innerRowW - sliderW - 6));
        ActivitySlider sliderWinOpacity = new ActivitySlider(
            innerStartX1 + innerRowW - sliderW, rowY, sliderW, ActivityMetrics.CONTROL_HEIGHT,
            30.0, 100.0, config.windowOpacity, 1.0,
            null,
            val -> Text.translatable("activity.unit.percent", String.format(Locale.ROOT, "%.0f", val)),
            val -> {
                config.windowOpacity = val;
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelWinOpacity);
        addControl(container, sliderWinOpacity);

        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelPanelOpacity = new ActivityLabel(innerStartX1, rowY + 3, Text.translatable("activity.setting.interface.panel_opacity"));
        labelPanelOpacity.setMaxWidth(Math.max(20, innerRowW - sliderW - 6));
        ActivitySlider sliderPanelOpacity = new ActivitySlider(
            innerStartX1 + innerRowW - sliderW, rowY, sliderW, ActivityMetrics.CONTROL_HEIGHT,
            20.0, 100.0, config.panelOpacity, 1.0,
            null,
            val -> Text.translatable("activity.unit.percent", String.format(Locale.ROOT, "%.0f", val)),
            val -> {
                config.panelOpacity = val;
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelPanelOpacity);
        addControl(container, sliderPanelOpacity);

        col1Y += card1Height + 10;

        int card2X = twoColumns ? col2X : col1X;
        int innerStartX2 = card2X + ActivityMetrics.PADDING_PANEL;
        int curY2 = twoColumns ? col2Y : col1Y;

        int rows2 = 6;
        int card2Height = 22 + rows2 * (ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING) + 4;
        ActivityPanel card2 = createCard(container, card2X, curY2, cardW, card2Height, Text.translatable("activity.card.interface.audio"));
        registerModuleCard("audio_settings", card2);
        registerCardAlias("motion_audio", card2);

        rowY = curY2 + 22;

        ActivityLabel labelSoundProfile = new ActivityLabel(innerStartX2, rowY + 3, Text.translatable("activity.setting.interface.sound_profile"));
        labelSoundProfile.setMaxWidth(Math.max(20, innerRowW - dropdownW - 6));
        ActivityDropdown<SoundProfile> dropdownSoundProfile = new ActivityDropdown<>(
            innerStartX2 + innerRowW - dropdownW, rowY, dropdownW, ActivityMetrics.CONTROL_HEIGHT,
            screen.getOverlayManager(),
            SOUND_PROFILE_OPTIONS,
            SoundProfile.fromId(config.soundProfile),
            SoundProfile::getDisplayText,
            selected -> {
                config.soundProfile = selected.getId();
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelSoundProfile);
        addControl(container, dropdownSoundProfile);

        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelSound = new ActivityLabel(innerStartX2, rowY + 3, Text.translatable("activity.setting.interface.audio_clicks"));
        labelSound.setMaxWidth(Math.max(20, innerRowW - ActivityMetrics.TOGGLE_WIDTH - 6));
        ActivityToggle toggleSound = new ActivityToggle(
            innerStartX2 + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.soundEnabled,
            state -> {
                config.soundEnabled = state;
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelSound);
        addControl(container, toggleSound);

        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelVol = new ActivityLabel(innerStartX2, rowY + 3, Text.translatable("activity.setting.interface.volume"));
        labelVol.setMaxWidth(Math.max(20, innerRowW - sliderW - 6));
        ActivitySlider sliderVol = new ActivitySlider(
            innerStartX2 + innerRowW - sliderW, rowY, sliderW, ActivityMetrics.CONTROL_HEIGHT,
            0.0, 100.0, config.soundVolume, 5.0,
            null,
            val -> Text.translatable("activity.unit.percent", String.format(Locale.ROOT, "%.0f", val)),
            val -> {
                config.soundVolume = val;
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelVol);
        addControl(container, sliderVol);

        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelRatchet = new ActivityLabel(innerStartX2, rowY + 3, Text.translatable("activity.setting.interface.slider_ratchet"));
        labelRatchet.setMaxWidth(Math.max(20, innerRowW - ActivityMetrics.TOGGLE_WIDTH - 6));
        ActivityToggle toggleRatchet = new ActivityToggle(
            innerStartX2 + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.sliderSoundEnabled,
            state -> {
                config.sliderSoundEnabled = state;
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelRatchet);
        addControl(container, toggleRatchet);

        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelAnim = new ActivityLabel(innerStartX2, rowY + 3, Text.translatable("activity.setting.interface.transitions"));
        labelAnim.setMaxWidth(Math.max(20, innerRowW - ActivityMetrics.TOGGLE_WIDTH - 6));
        ActivityToggle toggleAnim = new ActivityToggle(
            innerStartX2 + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.animationsEnabled,
            state -> {
                config.animationsEnabled = state;
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelAnim);
        addControl(container, toggleAnim);

        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelSpatial = new ActivityLabel(innerStartX2, rowY + 3, Text.translatable("activity.setting.interface.spatial_animation"));
        labelSpatial.setMaxWidth(Math.max(20, innerRowW - ActivityMetrics.TOGGLE_WIDTH - 6));
        ActivityToggle toggleSpatial = new ActivityToggle(
            innerStartX2 + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.spatialOpenAnimation,
            state -> {
                config.spatialOpenAnimation = state;
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelSpatial);
        addControl(container, toggleSpatial);

        if (twoColumns) {
            col2Y += card2Height + 10;
        } else {
            col1Y += card2Height + 10;
        }

        int card3X = col1X;
        int innerStartX3 = card3X + ActivityMetrics.PADDING_PANEL;
        int curY3 = col1Y;

        List<Preset> presets = PresetManager.getPresets();
        Preset resolvedPreset = PresetManager.getPresetById(config.activeProfile);
        if (resolvedPreset == null) resolvedPreset = PresetManager.getPresetByName(config.activeProfile);
        if (resolvedPreset == null) resolvedPreset = PresetManager.getDefaultPreset();
        final Preset currentPreset = resolvedPreset;

        boolean isCustom = !currentPreset.isBuiltin();
        boolean compactButtons = innerRowW < 240;
        boolean hasStatus = !this.presetStatusText.getString().isEmpty();
        int card3Rows = compactButtons ? 5 : 3;
        int card3Height = 22 + card3Rows * (ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING) + (hasStatus ? 18 : 4);
        ActivityPanel card3 = createCard(container, card3X, curY3, cardW, card3Height, Text.translatable("activity.card.settings.presets"));
        registerModuleCard("preset_settings", card3);
        registerCardAlias("presets", card3);

        rowY = curY3 + 22;

        ActivityLabel labelPreset = new ActivityLabel(innerStartX3, rowY + 3, Text.translatable("activity.setting.settings.active_preset"));
        int presetActionBtnW = ActivityMetrics.CONTROL_HEIGHT;
        int presetDropdownW = Math.min(160, (int) (innerRowW * 0.55f));
        int activeDropdownW = isCustom ? (presetDropdownW - presetActionBtnW * 2 - 8) : presetDropdownW;
        labelPreset.setMaxWidth(Math.max(20, innerRowW - presetDropdownW - 6));

        ActivityDropdown<Preset> dropdownPreset = new ActivityDropdown<>(
            innerStartX3 + innerRowW - presetDropdownW, rowY, activeDropdownW, ActivityMetrics.CONTROL_HEIGHT,
            screen.getOverlayManager(),
            presets,
            currentPreset,
            p -> p.isBuiltin() ? Text.translatable("activity.dropdown.profile.default") : Text.literal(p.getName()),
            p -> {
                config.activeProfile = p.isBuiltin() ? Preset.DEFAULT_PRESET_ID : p.getName();
                ActivityConfigManager.markDirty();
                screen.reloadCurrentTab();
            }
        );
        addControl(container, labelPreset);
        addControl(container, dropdownPreset);

        java.util.function.Consumer<Preset> showRenameModal = target -> {
            Preset toRename = target != null ? target : dropdownPreset.getSelectedOption();
            if (toRename == null || toRename.isBuiltin()) return;
            screen.getModalManager().showTextInput(
                Text.translatable("activity.modal.rename_preset.title"),
                Text.translatable("activity.modal.rename_preset.desc", toRename.getName()),
                Text.translatable("activity.modal.preset_name.placeholder"),
                toRename.getName(),
                newName -> {
                    if (newName == null || newName.trim().isEmpty() || newName.trim().length() > 32) return false;
                    String clean = newName.trim();
                    if (clean.equalsIgnoreCase(toRename.getName())) return true;
                    return !PresetManager.hasPresetNamed(clean);
                },
                newName -> {
                    String clean = newName.trim();
                    if (!clean.equalsIgnoreCase(toRename.getName())) {
                        PresetManager.renamePreset(toRename, clean);
                        this.presetStatusText = Text.translatable("activity.status.preset_renamed");
                        this.presetStatusColor = ActivityColors.SUCCESS;
                        screen.reloadCurrentTab();
                    }
                },
                null
            );
        };

        dropdownPreset.setOnRightClick(p -> {
            if (p != null && !p.isBuiltin()) {
                showRenameModal.accept(p);
            }
        });

        if (isCustom) {
            ActivityButton btnRename = new ActivityButton(
                innerStartX3 + innerRowW - presetActionBtnW * 2 - 4, rowY, presetActionBtnW, ActivityMetrics.CONTROL_HEIGHT,
                ActivityIcon.EDIT,
                Text.empty(),
                ActivityButton.Variant.SECONDARY,
                btn -> showRenameModal.accept(dropdownPreset.getSelectedOption())
            );
            btnRename.setOnRightClick(btn -> showRenameModal.accept(dropdownPreset.getSelectedOption()));
            btnRename.setTouchPadding(ActivityMetrics.TOUCH_HITBOX_PADDING);
            btnRename.setTooltip(Text.translatable("activity.tooltip.rename_preset"));
            addControl(container, btnRename);

            Preset toDelete = currentPreset;
            ActivityButton btnDelete = new ActivityButton(
                innerStartX3 + innerRowW - presetActionBtnW, rowY, presetActionBtnW, ActivityMetrics.CONTROL_HEIGHT,
                ActivityIcon.TRASH,
                Text.empty(),
                ActivityButton.Variant.DANGER,
                btn -> {
                    screen.getModalManager().showConfirmation(
                        Text.translatable("activity.modal.delete_preset.title", toDelete.getName()),
                        Text.translatable("activity.modal.delete_preset.desc"),
                        Text.translatable("activity.button.delete_preset"),
                        Text.translatable("activity.button.cancel"),
                        true,
                        () -> {
                            PresetManager.deletePreset(toDelete.getId());
                            config.activeProfile = Preset.DEFAULT_PRESET_ID;
                            PresetManager.applyPreset(PresetManager.getDefaultPreset(), config);
                            activity.client.gui.sound.SoundManager.playDelete();
                            this.presetStatusText = Text.translatable("activity.status.preset_deleted");
                            this.presetStatusColor = ActivityColors.WARNING;
                            screen.reloadCurrentTab();
                        },
                        null
                    );
                }
            );
            btnDelete.setTouchPadding(ActivityMetrics.TOUCH_HITBOX_PADDING);
            btnDelete.setTooltip(Text.translatable("activity.button.delete_preset"));
            addControl(container, btnDelete);
        }

        Preset activePresetTarget = currentPreset;

        Runnable doApply = () -> {
            PresetManager.applyPreset(activePresetTarget, config);
            activity.client.gui.sound.SoundManager.playPresetApply();
            this.presetStatusText = Text.translatable("activity.status.preset_applied");
            this.presetStatusColor = ActivityColors.SUCCESS;
            screen.reloadCurrentTab();
        };

        Runnable doSave = () -> {
            screen.getModalManager().showTextInput(
                Text.translatable("activity.modal.preset_name.title"),
                Text.translatable("activity.modal.preset_name.desc"),
                Text.translatable("activity.modal.preset_name.placeholder"),
                "",
                name -> {
                    if (name == null || name.trim().isEmpty() || name.trim().length() > 32) return false;
                    for (int i = 0; i < name.length(); i++) {
                        char c = name.charAt(i);
                        if (Character.isISOControl(c) || c == '\n' || c == '\r' || c == '\t') return false;
                    }
                    return true;
                },
                name -> {
                    String cleanName = name.trim();
                    Preset existing = PresetManager.getPresetByName(cleanName);
                    if (existing != null && !existing.isBuiltin()) {
                        screen.getModalManager().showDuplicatePreset(
                            cleanName,
                            () -> {
                                PresetManager.overwritePreset(existing, config);
                                config.activeProfile = existing.getName();
                                activity.client.gui.sound.SoundManager.playPresetSave();
                                this.presetStatusText = Text.translatable("activity.status.preset_saved");
                                this.presetStatusColor = ActivityColors.SUCCESS;
                                screen.reloadCurrentTab();
                            },
                            () -> {
                                Preset newP = PresetManager.addOrOverwriteImported(
                                    Preset.createCustom(cleanName, PresetSerializer.extractSettingsSnapshot(config)),
                                    false
                                );
                                config.activeProfile = newP.getName();
                                activity.client.gui.sound.SoundManager.playPresetSave();
                                this.presetStatusText = Text.translatable("activity.status.preset_saved");
                                this.presetStatusColor = ActivityColors.SUCCESS;
                                screen.reloadCurrentTab();
                            },
                            null
                        );
                    } else {
                        Preset p = PresetManager.createPreset(cleanName, config);
                        config.activeProfile = p.getName();
                        activity.client.gui.sound.SoundManager.playPresetSave();
                        this.presetStatusText = Text.translatable("activity.status.preset_saved");
                        this.presetStatusColor = ActivityColors.SUCCESS;
                        screen.reloadCurrentTab();
                    }
                }
            );
        };

        Runnable doCopy = () -> {
            Preset toExport = activePresetTarget.isBuiltin()
                ? Preset.createCustom(activePresetTarget.getName(), PresetSerializer.extractSettingsSnapshot(config))
                : activePresetTarget;
            String json = PresetSerializer.toClipboardJson(toExport);
            MinecraftClient.getInstance().keyboard.setClipboard(json);
            activity.client.gui.sound.SoundManager.playCopy();
            this.presetStatusText = Text.translatable("activity.status.preset_copied");
            this.presetStatusColor = ActivityColors.SUCCESS;
            screen.reloadCurrentTab();
        };

        Runnable doImport = () -> {
            String clip = MinecraftClient.getInstance().keyboard.getClipboard();
            if (clip == null || clip.isBlank()) {
                activity.client.gui.sound.SoundManager.playError();
                this.presetStatusText = Text.translatable("activity.status.preset_import_empty");
                this.presetStatusColor = ActivityColors.DANGER;
                screen.reloadCurrentTab();
                return;
            }

            Preset imported;
            try {
                imported = PresetSerializer.fromClipboardJson(clip);
            } catch (PresetSerializer.PresetValidationException e) {
                activity.client.gui.sound.SoundManager.playError();
                this.presetStatusText = Text.literal(e.getMessage());
                this.presetStatusColor = ActivityColors.DANGER;
                screen.reloadCurrentTab();
                return;
            } catch (Exception e) {
                activity.client.gui.sound.SoundManager.playError();
                this.presetStatusText = Text.translatable("activity.status.preset_import_error");
                this.presetStatusColor = ActivityColors.DANGER;
                screen.reloadCurrentTab();
                return;
            }

            if (PresetManager.hasPresetNamed(imported.getName())) {
                screen.getModalManager().showDuplicatePreset(
                    imported.getName(),
                    () -> {
                        Preset p = PresetManager.addOrOverwriteImported(imported, true);
                        config.activeProfile = p.getName();
                        PresetManager.applyPreset(p, config);
                        activity.client.gui.sound.SoundManager.playImport();
                        this.presetStatusText = Text.translatable("activity.status.preset_imported");
                        this.presetStatusColor = ActivityColors.SUCCESS;
                        screen.reloadCurrentTab();
                    },
                    () -> {
                        Preset p = PresetManager.addOrOverwriteImported(imported, false);
                        config.activeProfile = p.getName();
                        PresetManager.applyPreset(p, config);
                        activity.client.gui.sound.SoundManager.playImport();
                        this.presetStatusText = Text.translatable("activity.status.preset_imported");
                        this.presetStatusColor = ActivityColors.SUCCESS;
                        screen.reloadCurrentTab();
                    },
                    null
                );
            } else {
                Preset p = PresetManager.addOrOverwriteImported(imported, false);
                config.activeProfile = p.getName();
                PresetManager.applyPreset(p, config);
                activity.client.gui.sound.SoundManager.playImport();
                this.presetStatusText = Text.translatable("activity.status.preset_imported");
                this.presetStatusColor = ActivityColors.SUCCESS;
                screen.reloadCurrentTab();
            }
        };

        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        int btnGap = ActivityMetrics.COLUMN_GAP;

        if (compactButtons) {
            ActivityButton btnApply = new ActivityButton(
                innerStartX3, rowY, innerRowW, ActivityMetrics.BUTTON_HEIGHT,
                ActivityIcon.ENABLED, Text.translatable("activity.button.apply_preset"),
                ActivityButton.Variant.PRIMARY, b -> doApply.run()
            );
            addControl(container, btnApply);

            rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
            ActivityButton btnSave = new ActivityButton(
                innerStartX3, rowY, innerRowW, ActivityMetrics.BUTTON_HEIGHT,
                ActivityIcon.SAVE, Text.translatable("activity.button.save_config"),
                ActivityButton.Variant.SECONDARY, b -> doSave.run()
            );
            addControl(container, btnSave);

            rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
            ActivityButton btnCopy = new ActivityButton(
                innerStartX3, rowY, innerRowW, ActivityMetrics.BUTTON_HEIGHT,
                ActivityIcon.EXPORT, Text.translatable("activity.button.copy_preset"),
                ActivityButton.Variant.SECONDARY, b -> doCopy.run()
            );
            addControl(container, btnCopy);

            rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
            ActivityButton btnImport = new ActivityButton(
                innerStartX3, rowY, innerRowW, ActivityMetrics.BUTTON_HEIGHT,
                ActivityIcon.IMPORT, Text.translatable("activity.button.import_preset"),
                ActivityButton.Variant.SECONDARY, b -> doImport.run()
            );
            addControl(container, btnImport);
        } else {
            int halfBtnW = (innerRowW - btnGap) / 2;
            ActivityButton btnApply = new ActivityButton(
                innerStartX3, rowY, halfBtnW, ActivityMetrics.BUTTON_HEIGHT,
                ActivityIcon.ENABLED, Text.translatable("activity.button.apply_preset"),
                ActivityButton.Variant.PRIMARY, b -> doApply.run()
            );
            ActivityButton btnSave = new ActivityButton(
                innerStartX3 + halfBtnW + btnGap, rowY, halfBtnW, ActivityMetrics.BUTTON_HEIGHT,
                ActivityIcon.SAVE, Text.translatable("activity.button.save_config"),
                ActivityButton.Variant.SECONDARY, b -> doSave.run()
            );
            addControl(container, btnApply);
            addControl(container, btnSave);

            rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
            ActivityButton btnCopy = new ActivityButton(
                innerStartX3, rowY, halfBtnW, ActivityMetrics.BUTTON_HEIGHT,
                ActivityIcon.EXPORT, Text.translatable("activity.button.copy_preset"),
                ActivityButton.Variant.SECONDARY, b -> doCopy.run()
            );
            ActivityButton btnImport = new ActivityButton(
                innerStartX3 + halfBtnW + btnGap, rowY, halfBtnW, ActivityMetrics.BUTTON_HEIGHT,
                ActivityIcon.IMPORT, Text.translatable("activity.button.import_preset"),
                ActivityButton.Variant.SECONDARY, b -> doImport.run()
            );
            addControl(container, btnCopy);
            addControl(container, btnImport);
        }

        if (hasStatus) {
            rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
            ActivityLabel statusLbl = new ActivityLabel(innerStartX3 + 2, rowY + 3, this.presetStatusText, this.presetStatusColor);
            statusLbl.setMaxWidth(innerRowW - 4);
            addControl(container, statusLbl);
        }

        col1Y += card3Height + 10;

        int card4X = twoColumns ? col2X : col1X;
        int innerStartX4 = card4X + ActivityMetrics.PADDING_PANEL;
        int curY4 = twoColumns ? col2Y : col1Y;

        int card4Rows = 3;
        int card4Height = 22 + card4Rows * (ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING) + 4;
        ActivityPanel card4 = createCard(container, card4X, curY4, cardW, card4Height, Text.translatable("activity.card.settings.actions"));
        registerModuleCard("config_actions", card4);
        registerCardAlias("capitulate", card4);
        registerCardAlias("toggle_all", card4);
        registerCardAlias("modules", card4);

        rowY = curY4 + 22;
        int actionBtnW = (innerRowW - btnGap) / 2;
        ActivityButton btnRecenter = new ActivityButton(
            innerStartX4, rowY, actionBtnW, ActivityMetrics.CONTROL_HEIGHT,
            ActivityIcon.RESET_LAYOUT,
            Text.translatable("activity.button.recenter_window"),
            btn -> {
                config.windowPosX = -1;
                config.windowPosY = -1;
                ActivityConfigManager.markDirty();
                screen.recenterWindow();
            }
        );
        btnRecenter.setTouchPadding(ActivityMetrics.TOUCH_HITBOX_PADDING);
        ActivityButton btnReset = new ActivityButton(
            innerStartX4 + actionBtnW + btnGap, rowY, actionBtnW, ActivityMetrics.CONTROL_HEIGHT,
            ActivityIcon.RESTORE,
            Text.translatable("activity.button.reset_defaults"),
            ActivityButton.Variant.DANGER,
            btn -> {
                screen.getModalManager().showResetHoldConfirmation(
                    screen,
                    () -> {
                        resetDefaults();
                        ActivityConfigManager.resetDefaults();
                        ActivityConfigManager.save();
                        screen.reloadCurrentTab();
                    }
                );
            }
        );
        btnReset.setTouchPadding(ActivityMetrics.TOUCH_HITBOX_PADDING);
        addControl(container, btnRecenter);
        addControl(container, btnReset);

        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        boolean anyModuleEnabled = ModuleRegistry.isAnyModuleEnabled();
        ActivityButton btnToggleAllModules = new ActivityButton(
            innerStartX4, rowY, innerRowW, ActivityMetrics.CONTROL_HEIGHT,
            anyModuleEnabled ? ActivityIcon.DISABLED : ActivityIcon.ENABLED,
            anyModuleEnabled ? Text.translatable("activity.button.disable_all_modules") : Text.translatable("activity.button.enable_all_modules"),
            anyModuleEnabled ? ActivityButton.Variant.SECONDARY : ActivityButton.Variant.PRIMARY,
            btn -> {
                if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
                    return;
                }
                boolean target = !ModuleRegistry.isAnyModuleEnabled();
                ModuleRegistry.setAllEnabled(target);
                activity.client.gui.sound.SoundManager.playToggle(target);
                screen.showToast(
                    Text.translatable(target ? "activity.toast.all_modules_enabled" : "activity.toast.all_modules_disabled"),
                    null,
                    null
                );
                screen.reloadCurrentTab();
            }
        );
        btnToggleAllModules.setTouchPadding(ActivityMetrics.TOUCH_HITBOX_PADDING);
        addControl(container, btnToggleAllModules);

        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityButton btnCapitulate = new ActivityButton(
            innerStartX4, rowY, innerRowW, ActivityMetrics.CONTROL_HEIGHT,
            ActivityIcon.WARNING,
            Text.translatable("activity.button.capitulate"),
            ActivityButton.Variant.DANGER,
            btn -> {
                screen.getModalManager().showConfirmation(
                    Text.translatable("activity.modal.capitulate.title"),
                    Text.translatable("activity.modal.capitulate.desc"),
                    Text.translatable("activity.button.capitulate.confirm"),
                    Text.translatable("activity.button.cancel"),
                    true,
                    () -> {
                        activity.client.capitulation.CapitulationManager.capitulate(MinecraftClient.getInstance());
                    },
                    null
                );
            }
        );
        btnCapitulate.setBrandHoverColor(0xFFFF4757);
        btnCapitulate.setTouchPadding(ActivityMetrics.TOUCH_HITBOX_PADDING);
        addControl(container, btnCapitulate);


    }
}

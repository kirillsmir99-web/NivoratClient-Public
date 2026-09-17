package activity.client.gui.tab;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.config.preset.Preset;
import activity.client.config.preset.PresetManager;
import activity.client.config.preset.PresetSerializer;
import activity.client.gui.ActivityScreen;
import activity.client.gui.component.ActivityButton;
import activity.client.gui.component.ActivityDropdown;
import activity.client.gui.component.ActivityLabel;
import activity.client.gui.component.ActivityPanel;
import activity.client.gui.icon.ActivityIcon;
import activity.client.gui.layout.ScrollContainer;
import activity.client.gui.render.ActivityGuiRenderer;
import activity.client.gui.theme.ActivityColors;
import activity.client.gui.theme.ActivityMetrics;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Profiles, global configuration persistence, and honest module integration status tab.
 */
public class ConfigTab extends ActivityTab {

    private static final Text SUBTITLE = Text.translatable("activity.tab.config.subtitle");

    public ConfigTab() {
        super("config", Text.translatable("activity.tab.config"), activity.client.gui.icon.ActivityIcon.CONFIG);
    }

    @Override
    public Text getSubtitle() {
        return SUBTITLE;
    }

    @Override
    public void resetDefaults() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        if (config != null) {
            config.activeProfile = Preset.DEFAULT_PRESET_ID;
        }
    }

    @Override
    public void loadFromConfig(ActivityConfig config) {
        if (config == null) return;
        ModuleRegistry.loadAll(config);
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        ModuleRegistry.saveAll(config);
    }

    private Text presetStatusText = Text.empty();
    private int presetStatusColor = ActivityColors.SUCCESS;

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

        int dropdownW = Math.min(150, innerRowW / 2);

        // ==========================================
        // CARD 1: ПРОФИЛИ НАСТРОЕК И ПРЕСЕТЫ
        // ==========================================
        List<Preset> presets = PresetManager.getPresets();
        Preset currentPreset = PresetManager.getPresetById(config.activeProfile);
        if (currentPreset == null) currentPreset = PresetManager.getPresetByName(config.activeProfile);
        if (currentPreset == null) currentPreset = PresetManager.getDefaultPreset();

        boolean isCustom = !currentPreset.isBuiltin();
        boolean compactButtons = innerRowW < 240;
        boolean hasStatus = !this.presetStatusText.getString().isEmpty();
        int baseRows = compactButtons ? 6 : 4;
        int profileHeight = 22 + baseRows * (ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING) + (hasStatus ? 18 : 4);
        int card1X = col1X;
        int innerStartX1 = card1X + ActivityMetrics.PADDING_PANEL;
        int curY1 = col1Y;
        ActivityPanel card1 = createCard(container, card1X, curY1, cardW, profileHeight, Text.translatable("activity.card.config.profiles"));
        registerModuleCard("profiles", card1);

        int rowY = curY1 + 22;

        // Row 1.1: Profile selector dropdown + Trash button (for custom presets)
        ActivityLabel labelProfile = new ActivityLabel(innerStartX1, rowY + 3, Text.translatable("activity.setting.config.active_profile"));
        int trashBtnW = ActivityMetrics.CONTROL_HEIGHT;
        int activeDropdownW = isCustom ? (dropdownW - trashBtnW - 4) : dropdownW;
        labelProfile.setMaxWidth(Math.max(20, innerRowW - dropdownW - 4));

        ActivityDropdown<Preset> dropdownProfile = new ActivityDropdown<>(
            innerStartX1 + innerRowW - dropdownW, rowY, activeDropdownW, ActivityMetrics.CONTROL_HEIGHT,
            screen.getOverlayManager(), presets, currentPreset,
            p -> p.isBuiltin() ? Text.translatable("activity.dropdown.profile.default") : Text.literal(p.getName()),
            val -> {
                config.activeProfile = val.isBuiltin() ? Preset.DEFAULT_PRESET_ID : val.getName();
                ActivityConfigManager.markDirty();
                screen.reloadCurrentTab();
            }
        );
        addControl(container, labelProfile);
        addControl(container, dropdownProfile);

        if (isCustom) {
            Preset toDelete = currentPreset;
            ActivityButton btnDelete = new ActivityButton(
                innerStartX1 + innerRowW - trashBtnW, rowY, trashBtnW, ActivityMetrics.CONTROL_HEIGHT,
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
                            this.presetStatusText = Text.translatable("activity.status.preset_deleted");
                            this.presetStatusColor = ActivityColors.WARNING;
                            screen.reloadCurrentTab();
                        },
                        null
                    );
                }
            );
            btnDelete.setTouchPadding(ActivityMetrics.TOUCH_HITBOX_PADDING);
            addControl(container, btnDelete);
        }

        // Action Handlers
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
            activity.client.gui.sound.SoundManager.playPresetSave();
            this.presetStatusText = Text.translatable("activity.status.preset_copied");
            this.presetStatusColor = ActivityColors.SUCCESS;
            screen.reloadCurrentTab();
        };

        Runnable doImport = () -> {
            String clip = MinecraftClient.getInstance().keyboard.getClipboard();
            if (clip == null || clip.isBlank()) {
                this.presetStatusText = Text.translatable("activity.status.preset_import_empty");
                this.presetStatusColor = ActivityColors.DANGER;
                screen.reloadCurrentTab();
                return;
            }

            Preset imported;
            try {
                imported = PresetSerializer.fromClipboardJson(clip);
            } catch (PresetSerializer.PresetValidationException e) {
                this.presetStatusText = Text.literal(e.getMessage());
                this.presetStatusColor = ActivityColors.DANGER;
                screen.reloadCurrentTab();
                return;
            } catch (Exception e) {
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
                        activity.client.gui.sound.SoundManager.playPresetApply();
                        this.presetStatusText = Text.translatable("activity.status.preset_imported");
                        this.presetStatusColor = ActivityColors.SUCCESS;
                        screen.reloadCurrentTab();
                    },
                    () -> {
                        Preset p = PresetManager.addOrOverwriteImported(imported, false);
                        config.activeProfile = p.getName();
                        PresetManager.applyPreset(p, config);
                        activity.client.gui.sound.SoundManager.playPresetApply();
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
                activity.client.gui.sound.SoundManager.playPresetApply();
                this.presetStatusText = Text.translatable("activity.status.preset_imported");
                this.presetStatusColor = ActivityColors.SUCCESS;
                screen.reloadCurrentTab();
            }
        };

        // Row 1.2 & 1.3: Action buttons
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        int btnGap = ActivityMetrics.COLUMN_GAP;

        if (compactButtons) {
            ActivityButton btnApply = new ActivityButton(
                innerStartX1, rowY, innerRowW, ActivityMetrics.BUTTON_HEIGHT,
                ActivityIcon.ENABLED, Text.translatable("activity.button.apply_preset"),
                ActivityButton.Variant.PRIMARY, b -> doApply.run()
            );
            addControl(container, btnApply);

            rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
            ActivityButton btnSave = new ActivityButton(
                innerStartX1, rowY, innerRowW, ActivityMetrics.BUTTON_HEIGHT,
                ActivityIcon.SAVE, Text.translatable("activity.button.save_config"),
                ActivityButton.Variant.SECONDARY, b -> doSave.run()
            );
            addControl(container, btnSave);

            rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
            ActivityButton btnCopy = new ActivityButton(
                innerStartX1, rowY, innerRowW, ActivityMetrics.BUTTON_HEIGHT,
                ActivityIcon.EXPORT, Text.translatable("activity.button.copy_preset"),
                ActivityButton.Variant.SECONDARY, b -> doCopy.run()
            );
            addControl(container, btnCopy);

            rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
            ActivityButton btnImport = new ActivityButton(
                innerStartX1, rowY, innerRowW, ActivityMetrics.BUTTON_HEIGHT,
                ActivityIcon.IMPORT, Text.translatable("activity.button.import_preset"),
                ActivityButton.Variant.SECONDARY, b -> doImport.run()
            );
            addControl(container, btnImport);
        } else {
            int halfBtnW = (innerRowW - btnGap) / 2;
            ActivityButton btnApply = new ActivityButton(
                innerStartX1, rowY, halfBtnW, ActivityMetrics.BUTTON_HEIGHT,
                ActivityIcon.ENABLED, Text.translatable("activity.button.apply_preset"),
                ActivityButton.Variant.PRIMARY, b -> doApply.run()
            );
            ActivityButton btnSave = new ActivityButton(
                innerStartX1 + halfBtnW + btnGap, rowY, halfBtnW, ActivityMetrics.BUTTON_HEIGHT,
                ActivityIcon.SAVE, Text.translatable("activity.button.save_config"),
                ActivityButton.Variant.SECONDARY, b -> doSave.run()
            );
            addControl(container, btnApply);
            addControl(container, btnSave);

            rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
            ActivityButton btnCopy = new ActivityButton(
                innerStartX1, rowY, halfBtnW, ActivityMetrics.BUTTON_HEIGHT,
                ActivityIcon.EXPORT, Text.translatable("activity.button.copy_preset"),
                ActivityButton.Variant.SECONDARY, b -> doCopy.run()
            );
            ActivityButton btnImport = new ActivityButton(
                innerStartX1 + halfBtnW + btnGap, rowY, halfBtnW, ActivityMetrics.BUTTON_HEIGHT,
                ActivityIcon.IMPORT, Text.translatable("activity.button.import_preset"),
                ActivityButton.Variant.SECONDARY, b -> doImport.run()
            );
            addControl(container, btnCopy);
            addControl(container, btnImport);
        }

        // Row 1.4: Factory Reset Button with confirmation modal
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityButton btnReset = new ActivityButton(
            innerStartX1, rowY, innerRowW, ActivityMetrics.BUTTON_HEIGHT,
            ActivityIcon.RESET,
            Text.translatable("activity.button.reset_defaults"),
            ActivityButton.Variant.DANGER,
            b -> {
                screen.getModalManager().showConfirmation(
                    Text.translatable("activity.modal.reset.title"),
                    Text.translatable("activity.modal.reset.desc"),
                    Text.translatable("activity.button.reset"),
                    Text.translatable("activity.button.cancel"),
                    true,
                    () -> {
                        ActivityConfigManager.resetDefaults();
                        config.activeProfile = Preset.DEFAULT_PRESET_ID;
                        ActivityConfigManager.save();
                        activity.client.gui.sound.SoundManager.playPresetReset();
                        this.presetStatusText = Text.translatable("activity.status.preset_reset");
                        this.presetStatusColor = ActivityColors.WARNING;
                        screen.reloadCurrentTab();
                    },
                    null
                );
            }
        );
        addControl(container, btnReset);

        // Row 1.5: Dynamic Feedback Status Label
        if (hasStatus) {
            rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
            ActivityLabel statusLbl = new ActivityLabel(innerStartX1 + 2, rowY + 3, this.presetStatusText, this.presetStatusColor);
            statusLbl.setMaxWidth(innerRowW - 4);
            addControl(container, statusLbl);
        }

        col1Y += profileHeight + 10;

        // ==========================================
        // CARD 2: СТАТУС ИНТЕГРАЦИИ МОДУЛЕЙ
        // ==========================================
        int card2X = twoColumns ? col2X : col1X;
        int innerStartX2 = card2X + ActivityMetrics.PADDING_PANEL;
        int curY2 = twoColumns ? col2Y : col1Y;

        List<IModule> modules = ModuleRegistry.getAll();
        int statusRows = 4 + modules.size();
        int statusHeight = 22 + statusRows * (14 + ActivityMetrics.ROW_SPACING) + 8;
        ActivityPanel card2 = createCard(container, card2X, curY2, cardW, statusHeight, Text.translatable("activity.card.config.status"));
        registerModuleCard("status", card2);

        rowY = curY2 + 22;

        ActivityLabel labelUiStatus = new ActivityLabel(
            innerStartX2, rowY + 2,
            Text.translatable("activity.status.ui_framework"),
            ActivityColors.SUCCESS
        );
        labelUiStatus.setMaxWidth(innerRowW);
        addControl(container, labelUiStatus);

        rowY += 14 + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelConfigStatus = new ActivityLabel(
            innerStartX2, rowY + 2,
            Text.translatable("activity.status.config_binding"),
            ActivityColors.SUCCESS
        );
        labelConfigStatus.setMaxWidth(innerRowW);
        addControl(container, labelConfigStatus);

        rowY += 14 + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelCoresStatus = new ActivityLabel(
            innerStartX2, rowY + 2,
            Text.translatable("activity.status.combat_cores_stub"),
            0xFFE5A93C
        );
        labelCoresStatus.setMaxWidth(innerRowW);
        addControl(container, labelCoresStatus);

        rowY += 14 + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelHeader = new ActivityLabel(
            innerStartX2, rowY + 2,
            Text.translatable("activity.status.registered_modules_header"),
            ActivityColors.TEXT_SECONDARY
        );
        labelHeader.setMaxWidth(innerRowW);
        addControl(container, labelHeader);

        for (IModule module : modules) {
            rowY += 14 + ActivityMetrics.ROW_SPACING;
            Text moduleStatusLine = Text.translatable(
                "activity.status.module_format",
                module.getName(),
                module.getStatus().getDisplayText()
            );
            ActivityLabel labelMod = new ActivityLabel(
                innerStartX2 + 6, rowY + 2,
                moduleStatusLine,
                module.getStatus().getColor()
            );
            labelMod.setMaxWidth(Math.max(20, innerRowW - 8));
            addControl(container, labelMod);
        }
    }
}

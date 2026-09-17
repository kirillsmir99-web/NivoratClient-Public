package activity.client.gui.tab;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.ActivityScreen;
import activity.client.gui.component.ActivityDropdown;
import activity.client.gui.component.ActivityKeybindButton;
import activity.client.gui.component.ActivityLabel;
import activity.client.gui.component.ActivitySlider;
import activity.client.gui.component.ActivityTextField;
import activity.client.gui.component.ActivityToggle;
import activity.client.gui.layout.ScrollContainer;
import activity.client.gui.theme.ActivityMetrics;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.stub.AutoGGStub;
import activity.client.module.stub.AutoToolStub;
import activity.client.module.stub.HPReaperStub;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;

/**
 * Utility modules configuration tab: HPReaper, AutoTool, AutoGG, and Activity HUD.
 */
public class UtilityTab extends ActivityTab {

    private static final Text SUBTITLE = Text.translatable("activity.tab.utility.subtitle");

    private static final List<String> HP_MODES = List.of(
        "target_hp",
        "own_hp",
        "damage_diff",
        "compact"
    );

    private static final List<String> HUD_POSITIONS = List.of(
        "top_right",
        "top_left",
        "bottom_right",
        "bottom_left"
    );

    public UtilityTab() {
        super("utility", Text.translatable("activity.tab.utility"), activity.client.gui.icon.ActivityIcon.UTILITY);
    }

    @Override
    public Text getSubtitle() {
        return SUBTITLE;
    }

    @Override
    public void resetDefaults() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        if (config != null) {
            config.hpReaperEnabled = true;
            config.hpReaperKeybind.clear();
            config.hpReaperMode = "target_hp";

            config.autoToolEnabled = true;
            config.autoToolKeybind.clear();
            config.autoToolCombatGuard = true;
            config.autoToolDurabilitySaver = true;
            config.autoToolDurabilityThreshold = 5.0;
            config.autoToolPreferSilkTouch = false;
            config.autoToolRestorePrevious = true;

            config.autoGGEnabled = true;
            config.autoGGKeybind.clear();
            config.autoGGPhrase = "GGWP";
            config.autoGGSendOnOwnDeath = false;

            config.overlayEnabled = true;
            config.hudPosition = "top_right";
            config.overlayOpacity = 85.0;
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

    @Override
    public void buildTab(ActivityScreen screen, ScrollContainer container, int startX, int startY, int rowWidth) {
        this.clearComponents();
        ActivityConfig config = ActivityConfigManager.getConfig();
        IModule hp = ModuleRegistry.get(HPReaperStub.ID);
        IModule tool = ModuleRegistry.get(AutoToolStub.ID);
        IModule gg = ModuleRegistry.get(AutoGGStub.ID);

        boolean twoColumns = rowWidth >= ActivityMetrics.RESPONSIVE_TWO_COLUMN_BREAKPOINT;
        int colGap = ActivityMetrics.COLUMN_GAP;
        int cardW = twoColumns ? (rowWidth - colGap) / 2 : rowWidth;
        int col1X = startX;
        int col2X = twoColumns ? (startX + cardW + colGap) : startX;

        int innerRowW = cardW - ActivityMetrics.PADDING_PANEL * 2;
        int col1Y = startY;
        int col2Y = startY;

        int keybindBtnW = innerRowW < 200 ? 55 : (innerRowW < 240 ? 70 : 85);
        int sliderW = Math.min(150, (int) (innerRowW * 0.55f));
        int dropdownW = Math.min(130, innerRowW / 2);
        int moduleLabelMaxW = Math.max(20, innerRowW - ActivityMetrics.TOGGLE_WIDTH - ActivityMetrics.COLUMN_GAP - keybindBtnW - 4);
        int dropdownLabelMaxW = Math.max(20, innerRowW - dropdownW - 4);
        int sliderLabelMaxW = Math.max(20, innerRowW - sliderW - 4);
        int toggleLabelMaxW = Math.max(20, innerRowW - ActivityMetrics.TOGGLE_WIDTH - 4);

        // ==========================================
        // CARD 1: HPREAPER (Индикатор HP)
        // ==========================================
        int card1X = col1X;
        int innerStartX = card1X + ActivityMetrics.PADDING_PANEL;
        int curY = col1Y;
        int hpRows = 2;
        int hpHeight = 22 + hpRows * (ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING) + 4;
        registerModuleCard("hp_reaper", createCard(container, card1X, curY, cardW, hpHeight, Text.translatable("activity.card.utility.hp_reaper")));

        int rowY = curY + 22;

        // Row 1.1: Enable Toggle + Keybind
        ActivityLabel labelHp = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.module.hp_reaper.name"));
        labelHp.setMaxWidth(moduleLabelMaxW);
        ActivityKeybindButton btnHpKey = new ActivityKeybindButton(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH - ActivityMetrics.COLUMN_GAP - keybindBtnW, rowY,
            keybindBtnW, ActivityMetrics.CONTROL_HEIGHT,
            config.hpReaperKeybind,
            kb -> {
                config.hpReaperKeybind.copyFrom(kb);
                if (hp != null) hp.getKeybind().copyFrom(kb);
                ActivityConfigManager.markDirty();
            }
        );
        ActivityToggle toggleHp = new ActivityToggle(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.hpReaperEnabled,
            state -> {
                config.hpReaperEnabled = state;
                if (hp != null) hp.setEnabled(state);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelHp);
        addControl(container, btnHpKey);
        addControl(container, toggleHp);

        // Row 1.2: Display Mode Dropdown
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelHpMode = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.utility.display_mode"));
        labelHpMode.setMaxWidth(dropdownLabelMaxW);
        ActivityDropdown<String> dropdownHpMode = new ActivityDropdown<>(
            innerStartX + innerRowW - dropdownW, rowY, dropdownW, ActivityMetrics.CONTROL_HEIGHT,
            screen.getOverlayManager(), HP_MODES, config.hpReaperMode,
            modeKey -> Text.translatable("activity.dropdown.hp_mode." + modeKey),
            val -> {
                config.hpReaperMode = val;
                if (hp != null) hp.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelHpMode);
        addControl(container, dropdownHpMode);

        col1Y += hpHeight + 10;

        // ==========================================
        // CARD 2: AUTOTOOL (Авто-инструмент)
        // ==========================================
        int card2X = twoColumns ? col2X : col1X;
        innerStartX = card2X + ActivityMetrics.PADDING_PANEL;
        curY = twoColumns ? col2Y : col1Y;
        int toolRows = 6;
        int toolHeight = 22 + toolRows * (ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING) + 4;
        registerModuleCard("auto_tool", createCard(container, card2X, curY, cardW, toolHeight, Text.translatable("activity.card.utility.auto_tool")));

        rowY = curY + 22;

        // Row 2.1: Enable Toggle + Keybind
        ActivityLabel labelTool = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.module.auto_tool.name"));
        labelTool.setMaxWidth(moduleLabelMaxW);
        ActivityKeybindButton btnToolKey = new ActivityKeybindButton(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH - ActivityMetrics.COLUMN_GAP - keybindBtnW, rowY,
            keybindBtnW, ActivityMetrics.CONTROL_HEIGHT,
            config.autoToolKeybind,
            kb -> {
                config.autoToolKeybind.copyFrom(kb);
                if (tool != null) tool.getKeybind().copyFrom(kb);
                ActivityConfigManager.markDirty();
            }
        );
        ActivityToggle toggleTool = new ActivityToggle(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.autoToolEnabled,
            state -> {
                config.autoToolEnabled = state;
                if (tool != null) tool.setEnabled(state);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelTool);
        addControl(container, btnToolKey);
        addControl(container, toggleTool);

        // Row 2.2: Combat Guard Toggle
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelCombatGuard = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.utility.combat_guard"));
        labelCombatGuard.setMaxWidth(toggleLabelMaxW);
        ActivityToggle toggleCombatGuard = new ActivityToggle(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.autoToolCombatGuard,
            state -> {
                config.autoToolCombatGuard = state;
                if (tool != null) tool.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelCombatGuard);
        addControl(container, toggleCombatGuard);

        // Row 2.3: Durability Saver Toggle
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelSaver = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.utility.durability_saver"));
        labelSaver.setMaxWidth(toggleLabelMaxW);
        ActivityToggle toggleSaver = new ActivityToggle(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.autoToolDurabilitySaver,
            state -> {
                config.autoToolDurabilitySaver = state;
                if (tool != null) tool.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelSaver);
        addControl(container, toggleSaver);

        // Row 2.4: Durability Threshold Slider
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelThreshold = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.utility.durability_threshold"));
        labelThreshold.setMaxWidth(sliderLabelMaxW);
        ActivitySlider sliderThreshold = new ActivitySlider(
            innerStartX + innerRowW - sliderW, rowY, sliderW, ActivityMetrics.CONTROL_HEIGHT,
            1.0, 50.0, config.autoToolDurabilityThreshold, 1.0,
            Text.translatable("activity.setting.utility.threshold_label"),
            val -> Text.translatable("activity.unit.units", String.format(Locale.ROOT, "%.0f", val)),
            val -> {
                config.autoToolDurabilityThreshold = val;
                if (tool != null) tool.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelThreshold);
        addControl(container, sliderThreshold);

        // Row 2.5: Prefer Silk Touch Toggle
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelSilk = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.utility.prefer_silk_touch"));
        labelSilk.setMaxWidth(toggleLabelMaxW);
        ActivityToggle toggleSilk = new ActivityToggle(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.autoToolPreferSilkTouch,
            state -> {
                config.autoToolPreferSilkTouch = state;
                if (tool != null) tool.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelSilk);
        addControl(container, toggleSilk);

        // Row 2.6: Restore Previous Item Toggle
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelRestoreTool = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.utility.restore_previous_item"));
        labelRestoreTool.setMaxWidth(toggleLabelMaxW);
        ActivityToggle toggleRestoreTool = new ActivityToggle(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.autoToolRestorePrevious,
            state -> {
                config.autoToolRestorePrevious = state;
                if (tool != null) tool.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelRestoreTool);
        addControl(container, toggleRestoreTool);

        if (twoColumns) col2Y += toolHeight + 10; else col1Y += toolHeight + 10;

        // ==========================================
        // CARD 3: AUTOGG (Авто-GG)
        // ==========================================
        int card3X = col1X;
        innerStartX = card3X + ActivityMetrics.PADDING_PANEL;
        curY = col1Y;
        int ggRows = 3;
        int ggHeight = 22 + ggRows * (ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING) + 4;
        registerModuleCard("auto_gg", createCard(container, card3X, curY, cardW, ggHeight, Text.translatable("activity.card.utility.auto_gg")));

        rowY = curY + 22;

        // Row 3.1: Enable Toggle + Keybind
        ActivityLabel labelGG = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.module.auto_gg.name"));
        labelGG.setMaxWidth(moduleLabelMaxW);
        ActivityKeybindButton btnGGKey = new ActivityKeybindButton(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH - ActivityMetrics.COLUMN_GAP - keybindBtnW, rowY,
            keybindBtnW, ActivityMetrics.CONTROL_HEIGHT,
            config.autoGGKeybind,
            kb -> {
                config.autoGGKeybind.copyFrom(kb);
                if (gg != null) gg.getKeybind().copyFrom(kb);
                ActivityConfigManager.markDirty();
            }
        );
        ActivityToggle toggleGG = new ActivityToggle(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.autoGGEnabled,
            state -> {
                config.autoGGEnabled = state;
                if (gg != null) gg.setEnabled(state);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelGG);
        addControl(container, btnGGKey);
        addControl(container, toggleGG);

        // Row 3.2: Custom GG Phrase TextField
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelPhrase = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.utility.gg_phrase"));
        int textW = Math.min(140, innerRowW / 2);
        labelPhrase.setMaxWidth(Math.max(20, innerRowW - textW - 4));
        ActivityTextField fieldPhrase = new ActivityTextField(
            innerStartX + innerRowW - textW, rowY, textW, ActivityMetrics.INPUT_HEIGHT,
            Text.translatable("activity.setting.utility.phrase_placeholder")
        );
        fieldPhrase.setText(config.autoGGPhrase);
        fieldPhrase.setOnChanged(val -> {
            config.autoGGPhrase = val;
            if (gg != null) gg.loadFromConfig(config);
            ActivityConfigManager.markDirty();
        });
        addControl(container, labelPhrase);
        addControl(container, fieldPhrase);

        // Row 3.3: Send on Own Death Toggle
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelDeath = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.utility.send_on_death"));
        labelDeath.setMaxWidth(toggleLabelMaxW);
        ActivityToggle toggleDeath = new ActivityToggle(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.autoGGSendOnOwnDeath,
            state -> {
                config.autoGGSendOnOwnDeath = state;
                if (gg != null) gg.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelDeath);
        addControl(container, toggleDeath);

        curY += ggHeight + 10;

        // ==========================================
        // CARD 4: HUD ACTIVITY (Оверлей)
        // ==========================================
        int hudRows = 3;
        int hudHeight = 22 + hudRows * (ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING) + 4;
        registerModuleCard("hud_activity", createCard(container, startX, curY, rowWidth, hudHeight, Text.translatable("activity.card.utility.hud_activity")));

        rowY = curY + 22;

        // Row 4.1: Enable HUD Toggle
        ActivityLabel labelHud = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.general.enable_hud"));
        labelHud.setMaxWidth(toggleLabelMaxW);
        ActivityToggle toggleHud = new ActivityToggle(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.overlayEnabled,
            state -> {
                config.overlayEnabled = state;
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelHud);
        addControl(container, toggleHud);

        // Row 4.2: HUD Anchor Dropdown
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelHudPos = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.general.hud_anchor"));
        labelHudPos.setMaxWidth(dropdownLabelMaxW);
        ActivityDropdown<String> dropdownHudPos = new ActivityDropdown<>(
            innerStartX + innerRowW - dropdownW, rowY, dropdownW, ActivityMetrics.CONTROL_HEIGHT,
            screen.getOverlayManager(), HUD_POSITIONS, config.hudPosition,
            posKey -> Text.translatable("activity.dropdown.position." + posKey),
            val -> {
                config.hudPosition = val;
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelHudPos);
        addControl(container, dropdownHudPos);

        // Row 4.3: Overlay Opacity Slider
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelOpacity = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.general.overlay_opacity"));
        labelOpacity.setMaxWidth(sliderLabelMaxW);
        ActivitySlider sliderOpacity = new ActivitySlider(
            innerStartX + innerRowW - sliderW, rowY, sliderW, ActivityMetrics.CONTROL_HEIGHT,
            10.0, 100.0, config.overlayOpacity, 5.0,
            Text.translatable("activity.setting.general.opacity_label"),
            val -> Text.translatable("activity.unit.percent", String.format(Locale.ROOT, "%.0f", val)),
            val -> {
                config.overlayOpacity = val;
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelOpacity);
        addControl(container, sliderOpacity);
    }
}

package activity.client.gui.tab;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.ActivityScreen;
import activity.client.gui.component.ActivityDropdown;
import activity.client.gui.component.ActivityKeybindButton;
import activity.client.gui.component.ActivityLabel;
import activity.client.gui.component.ActivityPanel;
import activity.client.gui.component.ActivitySlider;
import activity.client.gui.component.ActivityToggle;
import activity.client.gui.layout.ScrollContainer;
import activity.client.gui.theme.ActivityMetrics;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.stub.AutoMaceStub;
import activity.client.module.stub.AutoShieldbreakerStub;
import activity.client.module.stub.AutoSpearStub;
import activity.client.module.stub.AutoStunSlamStub;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;

/**
 * Combat modules configuration tab: AutoMace, AutoSpear, AutoShieldbreaker, and AutoStunSlam.
 */
public class CombatTab extends ActivityTab {

    private static final Text SUBTITLE = Text.translatable("activity.tab.combat.subtitle");

    private static final List<String> MACE_SOURCES = List.of("sword_and_axe", "sword_only", "axe_only");
    private static final List<String> MACE_ENCHANTS = List.of("smart", "breach_only", "density_only");
    private static final List<String> BREAKER_MODES = List.of("full_auto", "semi_auto");

    public CombatTab() {
        super("combat", Text.translatable("activity.tab.combat"), activity.client.gui.icon.ActivityIcon.COMBAT);
    }

    @Override
    public Text getSubtitle() {
        return SUBTITLE;
    }

    @Override
    public void resetDefaults() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        if (config != null) {
            config.autoMaceEnabled = true;
            config.autoMaceKeybind.clear();
            config.autoMaceSourceMode = "sword_and_axe";
            config.autoMaceEnchantMode = "smart";
            config.autoMaceRestoreDelayMs = 90.0;
            config.autoMaceLegitMode = true;
            config.autoMaceMissChance = 10.0;

            config.autoSpearEnabled = true;
            config.autoSpearKeybind.clear();
            config.autoSpearRestoreDelayMs = 70.0;

            config.autoShieldbreakerEnabled = true;
            config.autoShieldbreakerKeybind.clear();
            config.autoShieldbreakerMode = "full_auto";
            config.autoShieldbreakerDistance = 2.85;
            config.autoShieldbreakerChance = 100.0;
            config.autoShieldbreakerSwitchDelayMs = 50.0;
            config.autoShieldbreakerRestoreDelayMs = 50.0;
            config.autoShieldbreakerLegitMode = true;

            config.autoStunSlamEnabled = true;
            config.autoStunSlamKeybind.clear();
            config.autoStunSlamMode = "full_auto";
            config.autoStunSlamDistance = 2.4;
            config.autoStunSlamChance = 75.0;
            config.autoStunSlamAxeDelayMs = 45.0;
            config.autoStunSlamMaceDelayMs = 45.0;
            config.autoStunSlamRestoreDelayMs = 50.0;
            config.autoStunSlamLegitMode = true;
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
        IModule mace = ModuleRegistry.get(AutoMaceStub.ID);
        IModule spear = ModuleRegistry.get(AutoSpearStub.ID);
        IModule breaker = ModuleRegistry.get(AutoShieldbreakerStub.ID);
        IModule stun = ModuleRegistry.get(AutoStunSlamStub.ID);

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
        // CARD 1: AUTOMACE (Авто-булава)
        // ==========================================
        int card1X = col1X;
        int innerStartX = card1X + ActivityMetrics.PADDING_PANEL;
        int curY = col1Y;
        int maceRows = 6;
        int maceHeight = 22 + maceRows * (ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING) + 4;
        registerModuleCard("auto_mace", createCard(container, card1X, curY, cardW, maceHeight, Text.translatable("activity.card.combat.auto_mace")));

        int rowY = curY + 22;

        // Row 1.1: Enable Toggle + Keybind
        ActivityLabel labelMace = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.module.auto_mace.name"));
        labelMace.setMaxWidth(moduleLabelMaxW);
        ActivityKeybindButton btnMaceKey = new ActivityKeybindButton(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH - ActivityMetrics.COLUMN_GAP - keybindBtnW, rowY,
            keybindBtnW, ActivityMetrics.CONTROL_HEIGHT,
            config.autoMaceKeybind,
            kb -> {
                config.autoMaceKeybind.copyFrom(kb);
                if (mace != null) mace.getKeybind().copyFrom(kb);
                ActivityConfigManager.markDirty();
            }
        );
        ActivityToggle toggleMace = new ActivityToggle(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.autoMaceEnabled,
            state -> {
                config.autoMaceEnabled = state;
                if (mace != null) mace.setEnabled(state);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelMace);
        addControl(container, btnMaceKey);
        addControl(container, toggleMace);

        // Row 1.2: Source Weapon Mode
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelSource = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.combat.source_mode"));
        labelSource.setMaxWidth(dropdownLabelMaxW);
        ActivityDropdown<String> dropdownSource = new ActivityDropdown<>(
            innerStartX + innerRowW - dropdownW, rowY, dropdownW, ActivityMetrics.CONTROL_HEIGHT,
            screen != null ? screen.getOverlayManager() : null, MACE_SOURCES, config.autoMaceSourceMode,
            val -> Text.translatable("activity.dropdown.source." + val),
            val -> {
                config.autoMaceSourceMode = val;
                if (mace != null) mace.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelSource);
        addControl(container, dropdownSource);

        // Row 1.3: Enchant Priority Mode
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelEnchant = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.combat.enchant_mode"));
        labelEnchant.setMaxWidth(dropdownLabelMaxW);
        ActivityDropdown<String> dropdownEnchant = new ActivityDropdown<>(
            innerStartX + innerRowW - dropdownW, rowY, dropdownW, ActivityMetrics.CONTROL_HEIGHT,
            screen != null ? screen.getOverlayManager() : null, MACE_ENCHANTS, config.autoMaceEnchantMode,
            val -> Text.translatable("activity.dropdown.enchant." + val),
            val -> {
                config.autoMaceEnchantMode = val;
                if (mace != null) mace.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelEnchant);
        addControl(container, dropdownEnchant);

        // Row 1.4: Restore Delay Slider
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelRestore = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.combat.restore_delay"));
        labelRestore.setMaxWidth(sliderLabelMaxW);
        ActivitySlider sliderRestore = new ActivitySlider(
            innerStartX + innerRowW - sliderW, rowY, sliderW, ActivityMetrics.CONTROL_HEIGHT,
            30.0, 250.0, config.autoMaceRestoreDelayMs, 5.0,
            Text.translatable("activity.setting.combat.delay_label"),
            val -> Text.translatable("activity.unit.ms", String.format(Locale.ROOT, "%.0f", val)),
            val -> {
                config.autoMaceRestoreDelayMs = val;
                if (mace != null) mace.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelRestore);
        addControl(container, sliderRestore);

        // Row 1.5: Mace Legit Toggle
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelMaceLegit = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.combat.legit_mode"));
        labelMaceLegit.setMaxWidth(toggleLabelMaxW);
        ActivityToggle toggleMaceLegit = new ActivityToggle(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.autoMaceLegitMode,
            state -> {
                config.autoMaceLegitMode = state;
                if (mace != null) mace.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        if (screen != null) {
            toggleMaceLegit.setConfirmTurnOff(
                Text.translatable("activity.modal.legit_off.title"),
                Text.translatable("activity.modal.legit_off.desc"),
                Text.translatable("activity.button.disable"),
                screen.getModalManager()
            );
        }
        addControl(container, labelMaceLegit);
        addControl(container, toggleMaceLegit);

        // Row 1.6: Miss Chance Slider
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelMiss = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.combat.miss_chance"));
        labelMiss.setMaxWidth(sliderLabelMaxW);
        ActivitySlider sliderMiss = new ActivitySlider(
            innerStartX + innerRowW - sliderW, rowY, sliderW, ActivityMetrics.CONTROL_HEIGHT,
            0.0, 50.0, config.autoMaceMissChance, 1.0,
            Text.translatable("activity.setting.combat.chance_label"),
            val -> Text.translatable("activity.unit.percent", String.format(Locale.ROOT, "%.0f", val)),
            val -> {
                config.autoMaceMissChance = val;
                if (mace != null) mace.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelMiss);
        addControl(container, sliderMiss);

        col1Y += maceHeight + 10;

        // ==========================================
        // CARD 2: AUTOSPEAR (Авто-копьё)
        // ==========================================
        int card2X = twoColumns ? col2X : col1X;
        innerStartX = card2X + ActivityMetrics.PADDING_PANEL;
        curY = twoColumns ? col2Y : col1Y;
        int spearRows = 2;
        int spearHeight = 22 + spearRows * (ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING) + 4;
        registerModuleCard("auto_spear", createCard(container, card2X, curY, cardW, spearHeight, Text.translatable("activity.card.combat.auto_spear")));

        rowY = curY + 22;

        // Row 2.1: Enable Toggle + Keybind
        ActivityLabel labelSpear = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.module.auto_spear.name"));
        labelSpear.setMaxWidth(moduleLabelMaxW);
        ActivityKeybindButton btnSpearKey = new ActivityKeybindButton(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH - ActivityMetrics.COLUMN_GAP - keybindBtnW, rowY,
            keybindBtnW, ActivityMetrics.CONTROL_HEIGHT,
            config.autoSpearKeybind,
            kb -> {
                config.autoSpearKeybind.copyFrom(kb);
                if (spear != null) spear.getKeybind().copyFrom(kb);
                ActivityConfigManager.markDirty();
            }
        );
        ActivityToggle toggleSpear = new ActivityToggle(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.autoSpearEnabled,
            state -> {
                config.autoSpearEnabled = state;
                if (spear != null) spear.setEnabled(state);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelSpear);
        addControl(container, btnSpearKey);
        addControl(container, toggleSpear);

        // Row 2.2: Spear Restore Delay Slider
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelSpearRestore = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.combat.spear_restore_delay"));
        labelSpearRestore.setMaxWidth(sliderLabelMaxW);
        ActivitySlider sliderSpearRestore = new ActivitySlider(
            innerStartX + innerRowW - sliderW, rowY, sliderW, ActivityMetrics.CONTROL_HEIGHT,
            10.0, 500.0, config.autoSpearRestoreDelayMs, 5.0,
            Text.translatable("activity.setting.combat.delay_label"),
            val -> Text.translatable("activity.unit.ms", String.format(Locale.ROOT, "%.0f", val)),
            val -> {
                config.autoSpearRestoreDelayMs = val;
                if (spear != null) spear.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelSpearRestore);
        addControl(container, sliderSpearRestore);

        if (twoColumns) col2Y += spearHeight + 10; else col1Y += spearHeight + 10;

        // ==========================================
        // CARD 3: AUTOSHIELDBREAKER (Сбив щита)
        // ==========================================
        int card3X = col1X;
        innerStartX = card3X + ActivityMetrics.PADDING_PANEL;
        curY = col1Y;
        int breakerRows = 6;
        int breakerHeight = 22 + breakerRows * (ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING) + 4;
        registerModuleCard("auto_shieldbreaker", createCard(container, card3X, curY, cardW, breakerHeight, Text.translatable("activity.card.combat.auto_shieldbreaker")));

        rowY = curY + 22;

        // Row 3.1: Enable Toggle + Keybind
        ActivityLabel labelBreaker = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.module.auto_shieldbreaker.name"));
        labelBreaker.setMaxWidth(moduleLabelMaxW);
        ActivityKeybindButton btnBreakerKey = new ActivityKeybindButton(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH - ActivityMetrics.COLUMN_GAP - keybindBtnW, rowY,
            keybindBtnW, ActivityMetrics.CONTROL_HEIGHT,
            config.autoShieldbreakerKeybind,
            kb -> {
                config.autoShieldbreakerKeybind.copyFrom(kb);
                if (breaker != null) breaker.getKeybind().copyFrom(kb);
                ActivityConfigManager.markDirty();
            }
        );
        ActivityToggle toggleBreaker = new ActivityToggle(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.autoShieldbreakerEnabled,
            state -> {
                config.autoShieldbreakerEnabled = state;
                if (breaker != null) breaker.setEnabled(state);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelBreaker);
        addControl(container, btnBreakerKey);
        addControl(container, toggleBreaker);

        // Row 3.2: Mode Dropdown
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelBreakerMode = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.combat.breaker_mode"));
        labelBreakerMode.setMaxWidth(dropdownLabelMaxW);
        ActivityDropdown<String> dropdownBreakerMode = new ActivityDropdown<>(
            innerStartX + innerRowW - dropdownW, rowY, dropdownW, ActivityMetrics.CONTROL_HEIGHT,
            screen != null ? screen.getOverlayManager() : null, BREAKER_MODES, config.autoShieldbreakerMode,
            modeKey -> Text.translatable("activity.dropdown.breaker." + modeKey),
            val -> {
                config.autoShieldbreakerMode = val;
                if (breaker != null) breaker.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelBreakerMode);
        addControl(container, dropdownBreakerMode);

        // Row 3.3: Distance Slider
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelDistance = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.combat.trigger_distance"));
        labelDistance.setMaxWidth(sliderLabelMaxW);
        ActivitySlider sliderDistance = new ActivitySlider(
            innerStartX + innerRowW - sliderW, rowY, sliderW, ActivityMetrics.CONTROL_HEIGHT,
            1.5, 4.5, config.autoShieldbreakerDistance, 0.05,
            Text.translatable("activity.setting.combat.distance_label"),
            val -> Text.translatable("activity.unit.blocks", String.format(Locale.ROOT, "%.2f", val)),
            val -> {
                config.autoShieldbreakerDistance = val;
                if (breaker != null) breaker.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelDistance);
        addControl(container, sliderDistance);

        // Row 3.4: Chance Slider
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelBreakerChance = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.combat.chance_label"));
        labelBreakerChance.setMaxWidth(sliderLabelMaxW);
        ActivitySlider sliderBreakerChance = new ActivitySlider(
            innerStartX + innerRowW - sliderW, rowY, sliderW, ActivityMetrics.CONTROL_HEIGHT,
            10.0, 100.0, config.autoShieldbreakerChance, 1.0,
            Text.translatable("activity.setting.combat.chance_label"),
            val -> Text.translatable("activity.unit.percent", String.format(Locale.ROOT, "%.0f", val)),
            val -> {
                config.autoShieldbreakerChance = val;
                if (breaker != null) breaker.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelBreakerChance);
        addControl(container, sliderBreakerChance);

        // Row 3.5: Switch Delay Slider
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelSwitchDelay = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.combat.switch_delay"));
        labelSwitchDelay.setMaxWidth(sliderLabelMaxW);
        ActivitySlider sliderSwitchDelay = new ActivitySlider(
            innerStartX + innerRowW - sliderW, rowY, sliderW, ActivityMetrics.CONTROL_HEIGHT,
            10.0, 200.0, config.autoShieldbreakerSwitchDelayMs, 5.0,
            Text.translatable("activity.setting.combat.delay_label"),
            val -> Text.translatable("activity.unit.ms", String.format(Locale.ROOT, "%.0f", val)),
            val -> {
                config.autoShieldbreakerSwitchDelayMs = val;
                if (breaker != null) breaker.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelSwitchDelay);
        addControl(container, sliderSwitchDelay);

        // Row 3.6: Breaker Legit Toggle
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelBreakerLegit = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.combat.legit_mode"));
        labelBreakerLegit.setMaxWidth(toggleLabelMaxW);
        ActivityToggle toggleBreakerLegit = new ActivityToggle(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.autoShieldbreakerLegitMode,
            state -> {
                config.autoShieldbreakerLegitMode = state;
                if (breaker != null) breaker.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        if (screen != null) {
            toggleBreakerLegit.setConfirmTurnOff(
                Text.translatable("activity.modal.legit_off.title"),
                Text.translatable("activity.modal.legit_off.desc"),
                Text.translatable("activity.button.disable"),
                screen.getModalManager()
            );
        }
        addControl(container, labelBreakerLegit);
        addControl(container, toggleBreakerLegit);

        col1Y += breakerHeight + 10;

        // ==========================================
        // CARD 4: AUTOSTUNSLAM (Авто Стан Слэм)
        // ==========================================
        int card4X = twoColumns ? col2X : col1X;
        innerStartX = card4X + ActivityMetrics.PADDING_PANEL;
        curY = twoColumns ? col2Y : col1Y;
        int stunRows = 5;
        int stunHeight = 22 + stunRows * (ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING) + 4;
        ActivityPanel cardStun = createCard(container, card4X, curY, cardW, stunHeight, Text.translatable("activity.card.combat.auto_stun_slam"));
        registerModuleCard("auto_stun_slam", cardStun);
        registerCardAlias("auto_stun_slime", cardStun);

        rowY = curY + 22;

        // Row 4.1: Enable Toggle + Keybind
        ActivityLabel labelStun = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.module.auto_stun_slam.name"));
        labelStun.setMaxWidth(moduleLabelMaxW);
        ActivityKeybindButton btnStunKey = new ActivityKeybindButton(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH - ActivityMetrics.COLUMN_GAP - keybindBtnW, rowY,
            keybindBtnW, ActivityMetrics.CONTROL_HEIGHT,
            config.autoStunSlamKeybind,
            kb -> {
                config.autoStunSlamKeybind.copyFrom(kb);
                if (stun != null) stun.getKeybind().copyFrom(kb);
                ActivityConfigManager.markDirty();
            }
        );
        ActivityToggle toggleStun = new ActivityToggle(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.autoStunSlamEnabled,
            state -> {
                config.autoStunSlamEnabled = state;
                if (stun != null) stun.setEnabled(state);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelStun);
        addControl(container, btnStunKey);
        addControl(container, toggleStun);

        // Row 4.2: Stun Distance Slider
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelStunDist = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.combat.trigger_distance"));
        labelStunDist.setMaxWidth(sliderLabelMaxW);
        ActivitySlider sliderStunDist = new ActivitySlider(
            innerStartX + innerRowW - sliderW, rowY, sliderW, ActivityMetrics.CONTROL_HEIGHT,
            1.5, 4.0, config.autoStunSlamDistance, 0.05,
            Text.translatable("activity.setting.combat.distance_label"),
            val -> Text.translatable("activity.unit.blocks", String.format(Locale.ROOT, "%.2f", val)),
            val -> {
                config.autoStunSlamDistance = val;
                if (stun != null) stun.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelStunDist);
        addControl(container, sliderStunDist);

        // Row 4.3: Axe Delay Slider
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelAxeDelay = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.combat.axe_delay"));
        labelAxeDelay.setMaxWidth(sliderLabelMaxW);
        ActivitySlider sliderAxeDelay = new ActivitySlider(
            innerStartX + innerRowW - sliderW, rowY, sliderW, ActivityMetrics.CONTROL_HEIGHT,
            10.0, 200.0, config.autoStunSlamAxeDelayMs, 5.0,
            Text.translatable("activity.setting.combat.delay_label"),
            val -> Text.translatable("activity.unit.ms", String.format(Locale.ROOT, "%.0f", val)),
            val -> {
                config.autoStunSlamAxeDelayMs = val;
                if (stun != null) stun.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelAxeDelay);
        addControl(container, sliderAxeDelay);

        // Row 4.4: Mace Delay Slider
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelMaceDelay = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.combat.mace_delay"));
        labelMaceDelay.setMaxWidth(sliderLabelMaxW);
        ActivitySlider sliderMaceDelay = new ActivitySlider(
            innerStartX + innerRowW - sliderW, rowY, sliderW, ActivityMetrics.CONTROL_HEIGHT,
            10.0, 200.0, config.autoStunSlamMaceDelayMs, 5.0,
            Text.translatable("activity.setting.combat.delay_label"),
            val -> Text.translatable("activity.unit.ms", String.format(Locale.ROOT, "%.0f", val)),
            val -> {
                config.autoStunSlamMaceDelayMs = val;
                if (stun != null) stun.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelMaceDelay);
        addControl(container, sliderMaceDelay);

        // Row 4.5: Stun Legit Toggle
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelStunLegit = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.combat.legit_mode"));
        labelStunLegit.setMaxWidth(toggleLabelMaxW);
        ActivityToggle toggleStunLegit = new ActivityToggle(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.autoStunSlamLegitMode,
            state -> {
                config.autoStunSlamLegitMode = state;
                if (stun != null) stun.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        if (screen != null) {
            toggleStunLegit.setConfirmTurnOff(
                Text.translatable("activity.modal.legit_off.title"),
                Text.translatable("activity.modal.legit_off.desc"),
                Text.translatable("activity.button.disable"),
                screen.getModalManager()
            );
        }
        addControl(container, labelStunLegit);
        addControl(container, toggleStunLegit);
    }
}

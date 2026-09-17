package activity.client.gui.tab;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.ActivityScreen;
import activity.client.gui.component.ActivityKeybindButton;
import activity.client.gui.component.ActivityLabel;
import activity.client.gui.component.ActivitySlider;
import activity.client.gui.component.ActivityToggle;
import activity.client.gui.layout.ScrollContainer;
import activity.client.gui.theme.ActivityMetrics;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.stub.AutoAnchorStub;
import activity.client.module.stub.AutoCartStub;
import activity.client.module.stub.AutoTotemStub;
import activity.client.module.stub.CartRefillStub;
import net.minecraft.text.Text;

import java.util.Locale;

/**
 * Defense modules configuration tab: AutoTotem, AutoCart, AutoAnchor, and CartRefill.
 */
public class DefenseTab extends ActivityTab {

    private static final Text SUBTITLE = Text.translatable("activity.tab.defense.subtitle");

    public DefenseTab() {
        super("defense", Text.translatable("activity.tab.defense"), activity.client.gui.icon.ActivityIcon.DEFENSE);
    }

    @Override
    public Text getSubtitle() {
        return SUBTITLE;
    }

    @Override
    public void resetDefaults() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        if (config != null) {
            config.autoTotemEnabled = true;
            config.autoTotemKeybind.clear();
            config.autoTotemTriggerHearts = 3.0;
            config.autoTotemRestoreHearts = 6.0;
            config.autoTotemChance = 100.0;
            config.autoTotemReturnItem = true;
            config.autoTotemReturnOnPop = true;

            config.autoCartEnabled = true;
            config.autoCartKeybind.clear();
            config.autoCartPlacementChance = 70.0;
            config.autoCartRailDelay = 2.0;
            config.autoCartCartDelay = 2.0;
            config.autoCartRestoreDelay = 2.0;
            config.autoCartLegitMode = true;

            config.autoAnchorEnabled = true;
            config.autoAnchorKeybind.clear();
            config.autoAnchorAutoExplode = false;
            config.autoAnchorAutoReturn = true;
            config.autoAnchorChargeDelay = 1.0;
            config.autoAnchorChance = 85.0;
            config.autoAnchorLegitMode = true;

            config.cartRefillEnabled = true;
            config.cartRefillKeybind.clear();
            config.cartRefillDelayTicks = 2.0;
            config.cartRefillChance = 100.0;
            config.cartRefillLegitMode = true;
            config.cartRefillAutoClose = true;
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
        IModule totem = ModuleRegistry.get(AutoTotemStub.ID);
        IModule cart = ModuleRegistry.get(AutoCartStub.ID);
        IModule anchor = ModuleRegistry.get(AutoAnchorStub.ID);
        IModule refill = ModuleRegistry.get(CartRefillStub.ID);

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
        int sliderLabelMaxW = Math.max(20, innerRowW - sliderW - 4);
        int toggleLabelMaxW = Math.max(20, innerRowW - ActivityMetrics.TOGGLE_WIDTH - 4);

        // ==========================================
        // CARD 1: AUTOTOTEM (Авто-тотем)
        // ==========================================
        int card1X = col1X;
        int innerStartX = card1X + ActivityMetrics.PADDING_PANEL;
        int curY = col1Y;
        int totemRows = 6;
        int totemHeight = 22 + totemRows * (ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING) + 4;
        registerModuleCard("auto_totem", createCard(container, card1X, curY, cardW, totemHeight, Text.translatable("activity.card.defense.auto_totem")));

        int rowY = curY + 22;

        // Row 1.1: Enable Toggle + Keybind
        ActivityLabel labelTotem = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.module.auto_totem.name"));
        labelTotem.setMaxWidth(moduleLabelMaxW);
        ActivityKeybindButton btnTotemKey = new ActivityKeybindButton(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH - ActivityMetrics.COLUMN_GAP - keybindBtnW, rowY,
            keybindBtnW, ActivityMetrics.CONTROL_HEIGHT,
            config.autoTotemKeybind,
            kb -> {
                config.autoTotemKeybind.copyFrom(kb);
                if (totem != null) totem.getKeybind().copyFrom(kb);
                ActivityConfigManager.markDirty();
            }
        );
        ActivityToggle toggleTotem = new ActivityToggle(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.autoTotemEnabled,
            state -> {
                config.autoTotemEnabled = state;
                if (totem != null) totem.setEnabled(state);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelTotem);
        addControl(container, btnTotemKey);
        addControl(container, toggleTotem);

        // Row 1.2: Trigger Hearts Slider
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelTrigger = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.defense.trigger_hearts"));
        labelTrigger.setMaxWidth(sliderLabelMaxW);
        ActivitySlider sliderTrigger = new ActivitySlider(
            innerStartX + innerRowW - sliderW, rowY, sliderW, ActivityMetrics.CONTROL_HEIGHT,
            1.0, 9.0, config.autoTotemTriggerHearts, 1.0,
            Text.translatable("activity.setting.defense.hearts_label"),
            val -> Text.translatable("activity.unit.hearts", String.format(Locale.ROOT, "%.0f", val)),
            val -> {
                config.autoTotemTriggerHearts = val;
                if (totem != null) totem.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelTrigger);
        addControl(container, sliderTrigger);

        // Row 1.3: Restore Hearts Slider
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelRestore = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.defense.restore_hearts"));
        labelRestore.setMaxWidth(sliderLabelMaxW);
        ActivitySlider sliderRestore = new ActivitySlider(
            innerStartX + innerRowW - sliderW, rowY, sliderW, ActivityMetrics.CONTROL_HEIGHT,
            4.0, 10.0, config.autoTotemRestoreHearts, 1.0,
            Text.translatable("activity.setting.defense.hearts_label"),
            val -> Text.translatable("activity.unit.hearts", String.format(Locale.ROOT, "%.0f", val)),
            val -> {
                config.autoTotemRestoreHearts = val;
                if (totem != null) totem.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelRestore);
        addControl(container, sliderRestore);

        // Row 1.4: Chance Slider
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelTotemChance = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.defense.chance_label"));
        labelTotemChance.setMaxWidth(sliderLabelMaxW);
        ActivitySlider sliderTotemChance = new ActivitySlider(
            innerStartX + innerRowW - sliderW, rowY, sliderW, ActivityMetrics.CONTROL_HEIGHT,
            10.0, 100.0, config.autoTotemChance, 1.0,
            Text.translatable("activity.setting.defense.chance_label"),
            val -> Text.translatable("activity.unit.percent", String.format(Locale.ROOT, "%.0f", val)),
            val -> {
                config.autoTotemChance = val;
                if (totem != null) totem.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelTotemChance);
        addControl(container, sliderTotemChance);

        // Row 1.5: Return Item Toggle
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelReturnItem = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.defense.return_item"));
        labelReturnItem.setMaxWidth(toggleLabelMaxW);
        ActivityToggle toggleReturnItem = new ActivityToggle(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.autoTotemReturnItem,
            state -> {
                config.autoTotemReturnItem = state;
                if (totem != null) totem.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelReturnItem);
        addControl(container, toggleReturnItem);

        // Row 1.6: Return on Pop Toggle
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelReturnPop = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.defense.return_on_pop"));
        labelReturnPop.setMaxWidth(toggleLabelMaxW);
        ActivityToggle toggleReturnPop = new ActivityToggle(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.autoTotemReturnOnPop,
            state -> {
                config.autoTotemReturnOnPop = state;
                if (totem != null) totem.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelReturnPop);
        addControl(container, toggleReturnPop);

        col1Y += totemHeight + 10;

        // ==========================================
        // CARD 2: AUTOCART (Авто-вагонетки)
        // ==========================================
        int card2X = twoColumns ? col2X : col1X;
        innerStartX = card2X + ActivityMetrics.PADDING_PANEL;
        curY = twoColumns ? col2Y : col1Y;
        int cartRows = 6;
        int cartHeight = 22 + cartRows * (ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING) + 4;
        registerModuleCard("auto_cart", createCard(container, card2X, curY, cardW, cartHeight, Text.translatable("activity.card.defense.auto_cart")));

        rowY = curY + 22;

        // Row 2.1: Enable Toggle + Keybind
        ActivityLabel labelCart = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.module.auto_cart.name"));
        labelCart.setMaxWidth(moduleLabelMaxW);
        ActivityKeybindButton btnCartKey = new ActivityKeybindButton(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH - ActivityMetrics.COLUMN_GAP - keybindBtnW, rowY,
            keybindBtnW, ActivityMetrics.CONTROL_HEIGHT,
            config.autoCartKeybind,
            kb -> {
                config.autoCartKeybind.copyFrom(kb);
                if (cart != null) cart.getKeybind().copyFrom(kb);
                ActivityConfigManager.markDirty();
            }
        );
        ActivityToggle toggleCart = new ActivityToggle(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.autoCartEnabled,
            state -> {
                config.autoCartEnabled = state;
                if (cart != null) cart.setEnabled(state);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelCart);
        addControl(container, btnCartKey);
        addControl(container, toggleCart);

        // Row 2.2: Placement Chance Slider
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelPlacement = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.defense.placement_chance"));
        labelPlacement.setMaxWidth(sliderLabelMaxW);
        ActivitySlider sliderPlacement = new ActivitySlider(
            innerStartX + innerRowW - sliderW, rowY, sliderW, ActivityMetrics.CONTROL_HEIGHT,
            0.0, 100.0, config.autoCartPlacementChance, 5.0,
            Text.translatable("activity.setting.defense.chance_label"),
            val -> Text.translatable("activity.unit.percent", String.format(Locale.ROOT, "%.0f", val)),
            val -> {
                config.autoCartPlacementChance = val;
                if (cart != null) cart.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelPlacement);
        addControl(container, sliderPlacement);

        // Row 2.3: Rail Delay Slider
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelRail = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.defense.rail_delay"));
        labelRail.setMaxWidth(sliderLabelMaxW);
        ActivitySlider sliderRail = new ActivitySlider(
            innerStartX + innerRowW - sliderW, rowY, sliderW, ActivityMetrics.CONTROL_HEIGHT,
            0.0, 10.0, config.autoCartRailDelay, 1.0,
            Text.translatable("activity.setting.defense.delay_label"),
            val -> Text.translatable("activity.unit.ticks", String.format(Locale.ROOT, "%.0f", val)),
            val -> {
                config.autoCartRailDelay = val;
                if (cart != null) cart.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelRail);
        addControl(container, sliderRail);

        // Row 2.4: Cart Delay Slider
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelCartDelay = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.defense.cart_delay"));
        labelCartDelay.setMaxWidth(sliderLabelMaxW);
        ActivitySlider sliderCartDelay = new ActivitySlider(
            innerStartX + innerRowW - sliderW, rowY, sliderW, ActivityMetrics.CONTROL_HEIGHT,
            0.0, 10.0, config.autoCartCartDelay, 1.0,
            Text.translatable("activity.setting.defense.delay_label"),
            val -> Text.translatable("activity.unit.ticks", String.format(Locale.ROOT, "%.0f", val)),
            val -> {
                config.autoCartCartDelay = val;
                if (cart != null) cart.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelCartDelay);
        addControl(container, sliderCartDelay);

        // Row 2.5: Restore Delay Slider
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelCartRestore = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.defense.restore_delay"));
        labelCartRestore.setMaxWidth(sliderLabelMaxW);
        ActivitySlider sliderCartRestore = new ActivitySlider(
            innerStartX + innerRowW - sliderW, rowY, sliderW, ActivityMetrics.CONTROL_HEIGHT,
            0.0, 10.0, config.autoCartRestoreDelay, 1.0,
            Text.translatable("activity.setting.defense.delay_label"),
            val -> Text.translatable("activity.unit.ticks", String.format(Locale.ROOT, "%.0f", val)),
            val -> {
                config.autoCartRestoreDelay = val;
                if (cart != null) cart.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelCartRestore);
        addControl(container, sliderCartRestore);

        // Row 2.6: Cart Legit Toggle
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelCartLegit = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.defense.legit_mode"));
        labelCartLegit.setMaxWidth(toggleLabelMaxW);
        ActivityToggle toggleCartLegit = new ActivityToggle(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.autoCartLegitMode,
            state -> {
                config.autoCartLegitMode = state;
                if (cart != null) cart.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        toggleCartLegit.setConfirmTurnOff(
            Text.translatable("activity.modal.legit_off.title"),
            Text.translatable("activity.modal.legit_off.desc"),
            Text.translatable("activity.button.disable"),
            screen.getModalManager()
        );
        addControl(container, labelCartLegit);
        addControl(container, toggleCartLegit);

        if (twoColumns) col2Y += cartHeight + 10; else col1Y += cartHeight + 10;

        // ==========================================
        // CARD 3: AUTOANCHOR (Авто-якорь)
        // ==========================================
        int card3X = twoColumns ? col2X : col1X;
        innerStartX = card3X + ActivityMetrics.PADDING_PANEL;
        curY = twoColumns ? col2Y : col1Y;
        int anchorRows = 6;
        int anchorHeight = 22 + anchorRows * (ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING) + 4;
        registerModuleCard("auto_anchor", createCard(container, card3X, curY, cardW, anchorHeight, Text.translatable("activity.card.defense.auto_anchor")));

        rowY = curY + 22;

        // Row 3.1: Enable Toggle + Keybind
        ActivityLabel labelAnchor = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.module.auto_anchor.name"));
        labelAnchor.setMaxWidth(moduleLabelMaxW);
        ActivityKeybindButton btnAnchorKey = new ActivityKeybindButton(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH - ActivityMetrics.COLUMN_GAP - keybindBtnW, rowY,
            keybindBtnW, ActivityMetrics.CONTROL_HEIGHT,
            config.autoAnchorKeybind,
            kb -> {
                config.autoAnchorKeybind.copyFrom(kb);
                if (anchor != null) anchor.getKeybind().copyFrom(kb);
                ActivityConfigManager.markDirty();
            }
        );
        ActivityToggle toggleAnchor = new ActivityToggle(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.autoAnchorEnabled,
            state -> {
                config.autoAnchorEnabled = state;
                if (anchor != null) anchor.setEnabled(state);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelAnchor);
        addControl(container, btnAnchorKey);
        addControl(container, toggleAnchor);

        // Row 3.2: Auto Explode Toggle
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelExplode = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.defense.auto_explode"));
        labelExplode.setMaxWidth(toggleLabelMaxW);
        ActivityToggle toggleExplode = new ActivityToggle(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.autoAnchorAutoExplode,
            state -> {
                config.autoAnchorAutoExplode = state;
                if (anchor != null) anchor.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelExplode);
        addControl(container, toggleExplode);

        // Row 3.3: Auto Return Toggle
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelReturnAnchor = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.defense.auto_return"));
        labelReturnAnchor.setMaxWidth(toggleLabelMaxW);
        ActivityToggle toggleReturnAnchor = new ActivityToggle(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.autoAnchorAutoReturn,
            state -> {
                config.autoAnchorAutoReturn = state;
                if (anchor != null) anchor.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelReturnAnchor);
        addControl(container, toggleReturnAnchor);

        // Row 3.4: Charge Delay Slider
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelCharge = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.defense.charge_delay"));
        labelCharge.setMaxWidth(sliderLabelMaxW);
        ActivitySlider sliderCharge = new ActivitySlider(
            innerStartX + innerRowW - sliderW, rowY, sliderW, ActivityMetrics.CONTROL_HEIGHT,
            0.0, 10.0, config.autoAnchorChargeDelay, 1.0,
            Text.translatable("activity.setting.defense.delay_label"),
            val -> Text.translatable("activity.unit.ticks", String.format(Locale.ROOT, "%.0f", val)),
            val -> {
                config.autoAnchorChargeDelay = val;
                if (anchor != null) anchor.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelCharge);
        addControl(container, sliderCharge);

        // Row 3.5: Anchor Chance Slider
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelAnchorChance = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.defense.chance_label"));
        labelAnchorChance.setMaxWidth(sliderLabelMaxW);
        ActivitySlider sliderAnchorChance = new ActivitySlider(
            innerStartX + innerRowW - sliderW, rowY, sliderW, ActivityMetrics.CONTROL_HEIGHT,
            10.0, 100.0, config.autoAnchorChance, 1.0,
            Text.translatable("activity.setting.defense.chance_label"),
            val -> Text.translatable("activity.unit.percent", String.format(Locale.ROOT, "%.0f", val)),
            val -> {
                config.autoAnchorChance = val;
                if (anchor != null) anchor.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelAnchorChance);
        addControl(container, sliderAnchorChance);

        // Row 3.6: Anchor Legit Toggle
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelAnchorLegit = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.defense.legit_mode"));
        labelAnchorLegit.setMaxWidth(toggleLabelMaxW);
        ActivityToggle toggleAnchorLegit = new ActivityToggle(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.autoAnchorLegitMode,
            state -> {
                config.autoAnchorLegitMode = state;
                if (anchor != null) anchor.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        toggleAnchorLegit.setConfirmTurnOff(
            Text.translatable("activity.modal.legit_off.title"),
            Text.translatable("activity.modal.legit_off.desc"),
            Text.translatable("activity.button.disable"),
            screen.getModalManager()
        );
        addControl(container, labelAnchorLegit);
        addControl(container, toggleAnchorLegit);

        if (twoColumns) col2Y += anchorHeight + 10; else col1Y += anchorHeight + 10;

        // ==========================================
        // CARD 4: CARTREFILL (Пополнение вагонеток)
        // ==========================================
        int card4X = col1X;
        innerStartX = card4X + ActivityMetrics.PADDING_PANEL;
        curY = col1Y;
        int refillRows = 5;
        int refillHeight = 22 + refillRows * (ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING) + 4;
        registerModuleCard("cart_refill", createCard(container, card4X, curY, cardW, refillHeight, Text.translatable("activity.card.defense.cart_refill")));

        rowY = curY + 22;

        // Row 4.1: Enable Toggle + Keybind
        ActivityLabel labelRefill = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.module.cart_refill.name"));
        labelRefill.setMaxWidth(moduleLabelMaxW);
        ActivityKeybindButton btnRefillKey = new ActivityKeybindButton(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH - ActivityMetrics.COLUMN_GAP - keybindBtnW, rowY,
            keybindBtnW, ActivityMetrics.CONTROL_HEIGHT,
            config.cartRefillKeybind,
            kb -> {
                config.cartRefillKeybind.copyFrom(kb);
                if (refill != null) refill.getKeybind().copyFrom(kb);
                ActivityConfigManager.markDirty();
            }
        );
        ActivityToggle toggleRefill = new ActivityToggle(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.cartRefillEnabled,
            state -> {
                config.cartRefillEnabled = state;
                if (refill != null) refill.setEnabled(state);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelRefill);
        addControl(container, btnRefillKey);
        addControl(container, toggleRefill);

        // Row 4.2: Refill Delay Slider
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelRefillDelay = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.defense.refill_delay"));
        labelRefillDelay.setMaxWidth(sliderLabelMaxW);
        ActivitySlider sliderRefillDelay = new ActivitySlider(
            innerStartX + innerRowW - sliderW, rowY, sliderW, ActivityMetrics.CONTROL_HEIGHT,
            0.0, 10.0, config.cartRefillDelayTicks, 1.0,
            Text.translatable("activity.setting.defense.delay_label"),
            val -> Text.translatable("activity.unit.ticks", String.format(Locale.ROOT, "%.0f", val)),
            val -> {
                config.cartRefillDelayTicks = val;
                if (refill != null) refill.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelRefillDelay);
        addControl(container, sliderRefillDelay);

        // Row 4.3: Refill Chance Slider
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelRefillChance = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.defense.chance_label"));
        labelRefillChance.setMaxWidth(sliderLabelMaxW);
        ActivitySlider sliderRefillChance = new ActivitySlider(
            innerStartX + innerRowW - sliderW, rowY, sliderW, ActivityMetrics.CONTROL_HEIGHT,
            10.0, 100.0, config.cartRefillChance, 1.0,
            Text.translatable("activity.setting.defense.chance_label"),
            val -> Text.translatable("activity.unit.percent", String.format(Locale.ROOT, "%.0f", val)),
            val -> {
                config.cartRefillChance = val;
                if (refill != null) refill.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelRefillChance);
        addControl(container, sliderRefillChance);

        // Row 4.4: Refill Legit Toggle
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelRefillLegit = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.defense.legit_mode"));
        labelRefillLegit.setMaxWidth(toggleLabelMaxW);
        ActivityToggle toggleRefillLegit = new ActivityToggle(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.cartRefillLegitMode,
            state -> {
                config.cartRefillLegitMode = state;
                if (refill != null) refill.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        toggleRefillLegit.setConfirmTurnOff(
            Text.translatable("activity.modal.legit_off.title"),
            Text.translatable("activity.modal.legit_off.desc"),
            Text.translatable("activity.button.disable"),
            screen.getModalManager()
        );
        addControl(container, labelRefillLegit);
        addControl(container, toggleRefillLegit);

        // Row 4.5: Auto Close Toggle
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelAutoClose = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.defense.auto_close"));
        labelAutoClose.setMaxWidth(toggleLabelMaxW);
        ActivityToggle toggleAutoClose = new ActivityToggle(
            innerStartX + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.cartRefillAutoClose,
            state -> {
                config.cartRefillAutoClose = state;
                if (refill != null) refill.loadFromConfig(config);
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelAutoClose);
        addControl(container, toggleAutoClose);
    }
}

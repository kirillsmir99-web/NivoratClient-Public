package activity.client.gui.tab;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.ActivityScreen;
import activity.client.gui.builder.ModuleCardBuilder;
import activity.client.gui.component.ActivityButton;
import activity.client.gui.component.ActivityDropdown;
import activity.client.gui.component.ActivityLabel;
import activity.client.gui.component.ActivitySlider;
import activity.client.gui.component.ActivityToggle;
import activity.client.gui.hud.CooldownHudEditorScreen;
import activity.client.gui.hud.NivoratHudEditorScreen;
import activity.client.gui.icon.ActivityIcon;
import activity.client.gui.layout.ScrollContainer;
import activity.client.gui.theme.ActivityMetrics;
import activity.client.module.api.IModule;
import net.minecraft.client.MinecraftClient;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.setting.Setting;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;

public class UtilityTab extends ActivityTab {

    private static final Text SUBTITLE = Text.translatable("activity.tab.utility.subtitle");

    private static final List<String> HUD_POSITIONS = List.of(
        "top_right",
        "top_left",
        "bottom_right",
        "bottom_left"
    );

    public UtilityTab() {
        super("utility", Text.translatable("activity.tab.utility"), ActivityIcon.UTILITY);
    }

    @Override
    public Text getSubtitle() {
        return SUBTITLE;
    }

    @Override
    public ModuleCategory getCategory() {
        return ModuleCategory.UTILITY;
    }

    @Override
    public void resetDefaults() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        if (config != null) {
            config.hpReaperEnabled = true;
            config.hpReaperKeybind.clear();
            config.hpReaperMode = "target_hp";
            config.hpReaperTargetFilter = "all_entities";
            config.hpReaperOwnHealthX = -1;
            config.hpReaperOwnHealthY = -1;
            config.hpReaperCrosshairTargetX = -1;
            config.hpReaperCrosshairTargetY = -1;
            config.hpReaperTargetHealthX = -1;
            config.hpReaperTargetHealthY = -1;
            config.hpReaperDiffX = -1;
            config.hpReaperDiffY = -1;

            config.autoToolEnabled = true;
            config.autoToolKeybind.clear();
            config.autoToolCombatGuard = true;
            config.autoToolDurabilitySaver = true;
            config.autoToolDurabilityThreshold = 5.0;
            config.autoToolPreferSilkTouch = false;
            config.autoToolRestorePrevious = true;
            config.autoToolLegitMode = true;
            config.autoToolSingleSlotMode = false;
            config.autoToolIgnoreInstantBreak = true;
            config.autoToolLockWhileMining = true;

            config.autoGGEnabled = true;
            config.autoGGKeybind.clear();
            config.autoGGPhrase = "GGWP";
            config.autoGGSendOnKill = true;
            config.autoGGSendOnOwnDeath = false;
            config.autoGGRandomOrder = false;
            config.autoGGDelayMs = 950.0;

            config.cartHudEnabled = true;
            config.cartHudKeybind.clear();
            config.cartHudCustomX = -1;
            config.cartHudCustomY = -1;

            config.overlayEnabled = false;
            config.hudPosition = "top_right";
            config.overlayOpacity = 85.0;
            config.hudCustomX = -1;
            config.hudCustomY = -1;
            config.hudShowActiveModules = false;

            for (IModule mod : ModuleRegistry.getByCategory(getCategory())) {
                mod.loadFromConfig(config);
                for (Setting<?> s : mod.getSettings()) {
                    s.reset();
                }
                mod.saveToConfig(config);
            }
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

        int gridEndY = activity.client.gui.builder.ModuleGridBuilder.build(this, screen, container,
            ModuleRegistry.getByCategory(getCategory()), startX, startY, rowWidth);

        int curY = gridEndY;
        int hudRows = 5;
        int hudHeight = 22 + hudRows * (ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING) + 4;
        registerModuleCard("hud_activity", createCard(container, startX, curY, rowWidth, hudHeight, Text.translatable("activity.card.utility.hud_activity")));

        int innerStartX = startX + ActivityMetrics.PADDING_PANEL;
        int fullInnerRowW = rowWidth - ActivityMetrics.PADDING_PANEL * 2;
        int sliderW = Math.min(150, (int) (fullInnerRowW * 0.55f));
        int dropdownW = Math.min(130, fullInnerRowW / 2);
        int sliderLabelMaxW = Math.max(20, fullInnerRowW - sliderW - 4);
        int dropdownLabelMaxW = Math.max(20, fullInnerRowW - dropdownW - 4);
        int toggleLabelMaxW = Math.max(20, fullInnerRowW - ActivityMetrics.TOGGLE_WIDTH - 4);

        int rowY = curY + 22;

        ActivityLabel labelHud = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.general.enable_hud"));
        labelHud.setMaxWidth(toggleLabelMaxW);
        ActivityToggle toggleHud = new ActivityToggle(
            innerStartX + fullInnerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.overlayEnabled,
            state -> {
                config.overlayEnabled = state;
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelHud);
        addControl(container, toggleHud);

        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelModules = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.general.hud_show_active_modules"));
        labelModules.setMaxWidth(toggleLabelMaxW);
        ActivityToggle toggleModules = new ActivityToggle(
            innerStartX + fullInnerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY,
            config.hudShowActiveModules,
            state -> {
                config.hudShowActiveModules = state;
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelModules);
        addControl(container, toggleModules);

        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelHudPos = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.general.hud_anchor"));
        labelHudPos.setMaxWidth(dropdownLabelMaxW);
        ActivityDropdown<String> dropdownHudPos = new ActivityDropdown<>(
            innerStartX + fullInnerRowW - dropdownW, rowY, dropdownW, ActivityMetrics.CONTROL_HEIGHT,
            screen != null ? screen.getOverlayManager() : null, HUD_POSITIONS, config.hudPosition,
            posKey -> Text.translatable("activity.dropdown.position." + posKey),
            val -> {
                config.hudPosition = val;
                ActivityConfigManager.markDirty();
            }
        );
        addControl(container, labelHudPos);
        addControl(container, dropdownHudPos);

        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelOpacity = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.setting.general.overlay_opacity"));
        labelOpacity.setMaxWidth(sliderLabelMaxW);
        ActivitySlider sliderOpacity = new ActivitySlider(
            innerStartX + fullInnerRowW - sliderW, rowY, sliderW, ActivityMetrics.CONTROL_HEIGHT,
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

        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityButton btnOpenEditor = new ActivityButton(
            innerStartX, rowY, fullInnerRowW, ActivityMetrics.CONTROL_HEIGHT,
            Text.translatable("activity.setting.general.open_hud_editor"),
            ActivityButton.Variant.SECONDARY,
            btn -> {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null) {
                    mc.setScreen(new NivoratHudEditorScreen(screen));
                }
            }
        );
        addControl(container, btnOpenEditor);
    }
}

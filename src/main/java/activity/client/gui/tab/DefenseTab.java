package activity.client.gui.tab;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.ActivityScreen;
import activity.client.gui.builder.ModuleCardBuilder;
import activity.client.gui.icon.ActivityIcon;
import activity.client.gui.layout.ScrollContainer;
import activity.client.gui.theme.ActivityMetrics;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.setting.Setting;
import net.minecraft.text.Text;

import java.util.List;

public class DefenseTab extends ActivityTab {

    private static final Text SUBTITLE = Text.translatable("activity.tab.defense.subtitle");

    public DefenseTab() {
        super("defense", Text.translatable("activity.tab.defense"), ActivityIcon.DEFENSE);
    }

    @Override
    public Text getSubtitle() {
        return SUBTITLE;
    }

    @Override
    public ModuleCategory getCategory() {
        return ModuleCategory.DEFENSE;
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
            config.autoCartKeybind = new activity.client.module.keybind.Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_I, true, true, false);
            config.autoCartPreset = "medium";
            config.autoCartPlacementChance = 100.0;
            config.autoCartMaxDistance = 4.4;
            config.autoCartMinDelayMs = 70.0;
            config.autoCartMaxDelayMs = 110.0;
            config.autoCartAllowSelfCart = false;
            config.autoCartAllowPitPlacement = true;
            config.autoCartRandomDelay = true;
            config.autoCartLegitMode = true;

            config.autoAnchorEnabled = true;
            config.autoAnchorKeybind.clear();
            config.autoAnchorPreset = "balanced";
            config.autoAnchorAutoExplode = false;
            config.autoAnchorAutoReturn = true;
            config.autoAnchorChargeDelay = 1.0;
            config.autoAnchorExplodeDelay = 1.0;
            config.autoAnchorChance = 85.0;
            config.autoAnchorTargetCharges = 1.0;
            config.autoAnchorLegitMode = true;

            config.cartRefillEnabled = true;
            config.cartRefillKeybind.clear();
            config.cartRefillDelayTicks = 2.0;
            config.cartRefillChance = 100.0;
            config.cartRefillAutoClose = true;
            config.cartRefillRandomDelay = true;
            config.cartRefillLegitMode = true;

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
        boolean twoColumns = rowWidth >= ActivityMetrics.RESPONSIVE_TWO_COLUMN_BREAKPOINT;
        int colGap = ActivityMetrics.COLUMN_GAP;
        int cardW = twoColumns ? (rowWidth - colGap) / 2 : rowWidth;
        int col1X = startX;
        int col2X = twoColumns ? (startX + cardW + colGap) : startX;
        int innerRowW = cardW - ActivityMetrics.PADDING_PANEL * 2;

        int col1Y = startY;
        int col2Y = startY;

        List<IModule> modules = ModuleRegistry.getByCategory(getCategory());
        for (int i = 0; i < modules.size(); i++) {
            IModule mod = modules.get(i);
            boolean isCol2 = twoColumns && (i % 2 == 1);
            int curX = isCol2 ? col2X : col1X;
            int curY = isCol2 ? col2Y : col1Y;

            int h = ModuleCardBuilder.buildCard(this, screen, container, mod, curX, curY, cardW, innerRowW);
            if (isCol2) {
                col2Y += h + 10;
            } else {
                col1Y += h + 10;
            }
        }
    }
}

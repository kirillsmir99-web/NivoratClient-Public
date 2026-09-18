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
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.keybind.Keybind;
import activity.client.module.setting.Setting;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

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
    public ModuleCategory getCategory() {
        return ModuleCategory.COMBAT;
    }

    @Override
    public void resetDefaults() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        if (config != null) {
            config.autoMaceEnabled = true;
            config.autoMaceKeybind.clear();
            config.autoMaceSourceMode = "sword_and_axe";
            config.autoMaceEnchantMode = "smart";
            config.autoMaceMissBehavior = "sword_hit";
            config.autoMaceRestoreDelayMs = 90.0;
            config.autoMaceLegitMode = true;
            config.autoMaceMissChance = 10.0;
            config.autoMaceRandomDelay = true;

            config.autoSpearEnabled = true;
            config.autoSpearKeybind = new Keybind(GLFW.GLFW_KEY_TAB);
            config.autoSpearSecurityMode = "legit";
            config.autoSpearPriorityMode = "auto";
            config.autoSpearRestoreDelayMs = 185.0;
            config.autoSpearMissChance = 0.0;
            config.autoSpearRandomDelay = true;

            config.autoShieldbreakerEnabled = true;
            config.autoShieldbreakerKeybind = new Keybind(GLFW.GLFW_KEY_J, true, true, false);
            config.autoShieldbreakerMode = "full_auto";
            config.autoShieldbreakerDistance = 2.85;
            config.autoShieldbreakerChance = 100.0;
            config.autoShieldbreakerSwitchDelayMs = 50.0;
            config.autoShieldbreakerRestoreDelayMs = 50.0;
            config.autoShieldbreakerRandomDelay = true;
            config.autoShieldbreakerAbortOnManualSwitch = true;
            config.autoShieldbreakerLegitMode = true;

            config.autoStunSlamEnabled = true;
            config.autoStunSlamKeybind = new Keybind(GLFW.GLFW_KEY_M, true, true, false);
            config.autoStunSlamMode = "full_auto";
            config.autoStunSlamDistance = 2.4;
            config.autoStunSlamChance = 75.0;
            config.autoStunSlamAirTimeSec = 1.0;
            config.autoStunSlamAxeDelayMs = 45.0;
            config.autoStunSlamMaceDelayMs = 45.0;
            config.autoStunSlamRestoreDelayMs = 50.0;
            config.autoStunSlamRandomDelay = true;
            config.autoStunSlamLegitMode = true;

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

            int h = activity.client.gui.builder.ModuleCardBuilder.buildCard(this, screen, container, mod, curX, curY, cardW, innerRowW);
            if (isCol2) {
                col2Y += h + 10;
            } else {
                col1Y += h + 10;
            }
        }

        ActivityPanel stunCard = getModuleCard("auto_stun_slam");
        if (stunCard != null) {
            registerCardAlias("auto_stun_slime", stunCard);
        }
    }
}

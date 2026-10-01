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
            config.autoStunSlamDistance = 2.85;
            config.autoStunSlamChance = 100.0;
            config.autoStunSlamAirTimeSec = 0.1;
            config.autoStunSlamAxeDelayMs = 0.0;
            config.autoStunSlamMaceDelayMs = 0.0;
            config.autoStunSlamRestoreDelayMs = 50.0;
            config.autoStunSlamRandomDelay = false;
            config.autoStunSlamLegitMode = true;

            config.autoPearlCatchEnabled = true;
            config.autoPearlCatchKeybind = new Keybind();
            config.autoPearlCatchActionKeybind = new Keybind(GLFW.GLFW_KEY_V, false, false, false);
            config.autoPearlCatchHorizontalKeybind = new Keybind(GLFW.GLFW_KEY_C, false, false, false);
            config.autoPearlCatchMode = "semi_auto";
            config.autoPearlCatchDirection = "vertical";
            config.autoPearlCatchThrowDelay = 2.0;
            config.autoPearlCatchRestoreSlot = true;
            config.autoPearlCatchRestoreCamera = false;
            config.autoPearlCatchRotationTimeMs = 135.0;
            config.autoPearlCatchLegitMode = true;
            config.autoPearlCatchHorizontalOffset = 9.2;

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
        int gridEndY = activity.client.gui.builder.ModuleGridBuilder.build(this, screen, container,
            ModuleRegistry.getByCategory(getCategory()), startX, startY, rowWidth);

        ActivityPanel stunCard = getModuleCard("auto_stun_slam");
        if (stunCard != null) {
            registerCardAlias("auto_stun_slime", stunCard);
        }
    }
}

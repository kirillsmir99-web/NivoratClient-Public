package activity.client.module.stub;

import activity.client.config.ActivityConfig;
import activity.client.module.api.ModuleCategory;
import activity.client.module.keybind.Keybind;
import net.minecraft.text.Text;

public class AutoToolStub extends AbstractModuleStub {

    public static final String ID = "auto_tool";

    public boolean combatGuard = true;
    public boolean durabilitySaver = true;
    public double durabilityThreshold = 5.0;
    public boolean preferSilkTouch = false;
    public boolean restorePreviousItem = true;

    public AutoToolStub() {
        super(
            ID,
            Text.translatable("activity.module.auto_tool.name"),
            Text.translatable("activity.module.auto_tool.desc"),
            ModuleCategory.UTILITY,
            new Keybind()
        );
    }

    @Override
    public void loadFromConfig(ActivityConfig config) {
        if (config == null) return;
        this.enabled = config.autoToolEnabled;
        this.keybind.copyFrom(config.autoToolKeybind);
        this.combatGuard = config.autoToolCombatGuard;
        this.durabilitySaver = config.autoToolDurabilitySaver;
        this.durabilityThreshold = config.autoToolDurabilityThreshold;
        this.preferSilkTouch = config.autoToolPreferSilkTouch;
        this.restorePreviousItem = config.autoToolRestorePrevious;
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        config.autoToolEnabled = this.enabled;
        config.autoToolKeybind.copyFrom(this.keybind);
        config.autoToolCombatGuard = this.combatGuard;
        config.autoToolDurabilitySaver = this.durabilitySaver;
        config.autoToolDurabilityThreshold = this.durabilityThreshold;
        config.autoToolPreferSilkTouch = this.preferSilkTouch;
        config.autoToolRestorePrevious = this.restorePreviousItem;
    }
}

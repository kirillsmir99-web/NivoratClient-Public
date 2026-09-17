package activity.client.module.stub;

import activity.client.config.ActivityConfig;
import activity.client.module.api.ModuleCategory;
import activity.client.module.keybind.Keybind;
import net.minecraft.text.Text;

public class AutoSpearStub extends AbstractModuleStub {

    public static final String ID = "auto_spear";

    public double restoreDelayMs = 70.0;

    public AutoSpearStub() {
        super(
            ID,
            Text.translatable("activity.module.auto_spear.name"),
            Text.translatable("activity.module.auto_spear.desc"),
            ModuleCategory.COMBAT,
            new Keybind()
        );
    }

    @Override
    public void loadFromConfig(ActivityConfig config) {
        if (config == null) return;
        this.enabled = config.autoSpearEnabled;
        this.keybind.copyFrom(config.autoSpearKeybind);
        this.restoreDelayMs = config.autoSpearRestoreDelayMs;
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        config.autoSpearEnabled = this.enabled;
        config.autoSpearKeybind.copyFrom(this.keybind);
        config.autoSpearRestoreDelayMs = this.restoreDelayMs;
    }
}

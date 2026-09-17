package activity.client.module.stub;

import activity.client.config.ActivityConfig;
import activity.client.module.api.ModuleCategory;
import activity.client.module.keybind.Keybind;
import net.minecraft.text.Text;

public class AutoShieldbreakerStub extends AbstractModuleStub {

    public static final String ID = "auto_shieldbreaker";

    public String mode = "full_auto";
    public double triggerDistance = 2.85;
    public double chance = 100.0;
    public double switchDelayMs = 50.0;
    public double restoreDelayMs = 50.0;
    public boolean legitMode = true;

    public AutoShieldbreakerStub() {
        super(
            ID,
            Text.translatable("activity.module.auto_shieldbreaker.name"),
            Text.translatable("activity.module.auto_shieldbreaker.desc"),
            ModuleCategory.COMBAT,
            new Keybind()
        );
    }

    @Override
    public void loadFromConfig(ActivityConfig config) {
        if (config == null) return;
        this.enabled = config.autoShieldbreakerEnabled;
        this.keybind.copyFrom(config.autoShieldbreakerKeybind);
        this.mode = config.autoShieldbreakerMode;
        this.triggerDistance = config.autoShieldbreakerDistance;
        this.chance = config.autoShieldbreakerChance;
        this.switchDelayMs = config.autoShieldbreakerSwitchDelayMs;
        this.restoreDelayMs = config.autoShieldbreakerRestoreDelayMs;
        this.legitMode = config.autoShieldbreakerLegitMode;
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        config.autoShieldbreakerEnabled = this.enabled;
        config.autoShieldbreakerKeybind.copyFrom(this.keybind);
        config.autoShieldbreakerMode = this.mode;
        config.autoShieldbreakerDistance = this.triggerDistance;
        config.autoShieldbreakerChance = this.chance;
        config.autoShieldbreakerSwitchDelayMs = this.switchDelayMs;
        config.autoShieldbreakerRestoreDelayMs = this.restoreDelayMs;
        config.autoShieldbreakerLegitMode = this.legitMode;
    }
}

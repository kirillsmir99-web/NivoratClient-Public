package activity.client.module.stub;

import activity.client.config.ActivityConfig;
import activity.client.module.api.ModuleCategory;
import activity.client.module.keybind.Keybind;
import net.minecraft.text.Text;

public class AutoStunSlamStub extends AbstractModuleStub {

    public static final String ID = "auto_stun_slam";
    public static final String ALIAS_OLD_ID = "auto_stun_slime";

    public String mode = "full_auto";
    public double triggerDistance = 2.85;
    public double chance = 100.0;
    public double axeDelayMs = 0.0;
    public double maceDelayMs = 0.0;
    public double restoreDelayMs = 50.0;
    public boolean legitMode = true;

    public AutoStunSlamStub() {
        super(
            ID,
            Text.translatable("activity.module.auto_stun_slam.name"),
            Text.translatable("activity.module.auto_stun_slam.desc"),
            ModuleCategory.COMBAT,
            new Keybind()
        );
    }

    public AutoStunSlamStub(String id, Text name, Text desc) {
        super(
            id,
            name,
            desc,
            ModuleCategory.COMBAT,
            new Keybind()
        );
    }

    @Override
    public void loadFromConfig(ActivityConfig config) {
        if (config == null) return;
        this.enabled = config.autoStunSlamEnabled;
        this.keybind.copyFrom(config.autoStunSlamKeybind);
        this.mode = config.autoStunSlamMode;
        this.triggerDistance = config.autoStunSlamDistance;
        this.chance = config.autoStunSlamChance;
        this.axeDelayMs = config.autoStunSlamAxeDelayMs;
        this.maceDelayMs = config.autoStunSlamMaceDelayMs;
        this.restoreDelayMs = config.autoStunSlamRestoreDelayMs;
        this.legitMode = config.autoStunSlamLegitMode;
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        config.autoStunSlamEnabled = this.enabled;
        config.autoStunSlamKeybind.copyFrom(this.keybind);
        config.autoStunSlamMode = this.mode;
        config.autoStunSlamDistance = this.triggerDistance;
        config.autoStunSlamChance = this.chance;
        config.autoStunSlamAxeDelayMs = this.axeDelayMs;
        config.autoStunSlamMaceDelayMs = this.maceDelayMs;
        config.autoStunSlamRestoreDelayMs = this.restoreDelayMs;
        config.autoStunSlamLegitMode = this.legitMode;
    }
}

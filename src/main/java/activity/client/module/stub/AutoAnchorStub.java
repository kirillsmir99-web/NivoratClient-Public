package activity.client.module.stub;

import activity.client.config.ActivityConfig;
import activity.client.module.api.ModuleCategory;
import activity.client.module.keybind.Keybind;
import net.minecraft.text.Text;

public class AutoAnchorStub extends AbstractModuleStub {

    public static final String ID = "auto_anchor";

    public boolean autoExplode = false;
    public boolean autoReturn = true;
    public double chargeDelay = 1.0;
    public double chance = 85.0;
    public boolean legitMode = true;

    public AutoAnchorStub() {
        super(
            ID,
            Text.translatable("activity.module.auto_anchor.name"),
            Text.translatable("activity.module.auto_anchor.desc"),
            ModuleCategory.DEFENSE,
            new Keybind()
        );
    }

    @Override
    public void loadFromConfig(ActivityConfig config) {
        if (config == null) return;
        this.enabled = config.autoAnchorEnabled;
        this.keybind.copyFrom(config.autoAnchorKeybind);
        this.autoExplode = config.autoAnchorAutoExplode;
        this.autoReturn = config.autoAnchorAutoReturn;
        this.chargeDelay = config.autoAnchorChargeDelay;
        this.chance = config.autoAnchorChance;
        this.legitMode = config.autoAnchorLegitMode;
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        config.autoAnchorEnabled = this.enabled;
        config.autoAnchorKeybind.copyFrom(this.keybind);
        config.autoAnchorAutoExplode = this.autoExplode;
        config.autoAnchorAutoReturn = this.autoReturn;
        config.autoAnchorChargeDelay = this.chargeDelay;
        config.autoAnchorChance = this.chance;
        config.autoAnchorLegitMode = this.legitMode;
    }
}

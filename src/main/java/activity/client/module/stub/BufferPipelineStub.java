package activity.client.module.stub;

import activity.client.config.ActivityConfig;
import activity.client.module.api.ModuleCategory;
import activity.client.module.keybind.Keybind;
import net.minecraft.text.Text;

public class BufferPipelineStub extends AbstractModuleStub {

    public static final String ID = "auto_totem";

    public double triggerHearts = 3.0;
    public double restoreHearts = 6.0;
    public double chance = 100.0;
    public boolean returnItem = true;
    public boolean returnOnPop = true;

    public BufferPipelineStub() {
        super(
            ID,
            Text.translatable("activity.module.auto_totem.name"),
            Text.translatable("activity.module.auto_totem.desc"),
            ModuleCategory.DEFENSE,
            new Keybind()
        );
    }

    @Override
    public void loadFromConfig(ActivityConfig config) {
        if (config == null) return;
        this.enabled = config.autoTotemEnabled;
        this.keybind.copyFrom(config.autoTotemKeybind);
        this.triggerHearts = config.autoTotemTriggerHearts;
        this.restoreHearts = config.autoTotemRestoreHearts;
        this.chance = config.autoTotemChance;
        this.returnItem = config.autoTotemReturnItem;
        this.returnOnPop = config.autoTotemReturnOnPop;
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        config.autoTotemEnabled = this.enabled;
        config.autoTotemKeybind.copyFrom(this.keybind);
        config.autoTotemTriggerHearts = this.triggerHearts;
        config.autoTotemRestoreHearts = this.restoreHearts;
        config.autoTotemChance = this.chance;
        config.autoTotemReturnItem = this.returnItem;
        config.autoTotemReturnOnPop = this.returnOnPop;
    }
}

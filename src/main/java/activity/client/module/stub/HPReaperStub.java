package activity.client.module.stub;

import activity.client.config.ActivityConfig;
import activity.client.module.api.ModuleCategory;
import activity.client.module.keybind.Keybind;
import net.minecraft.text.Text;

public class HPReaperStub extends AbstractModuleStub {

    public static final String ID = "hp_reaper";

    public String displayMode = "target_hp";

    public HPReaperStub() {
        super(
            ID,
            Text.translatable("activity.module.hp_reaper.name"),
            Text.translatable("activity.module.hp_reaper.desc"),
            ModuleCategory.UTILITY,
            new Keybind()
        );
    }

    @Override
    public void loadFromConfig(ActivityConfig config) {
        if (config == null) return;
        this.enabled = config.hpReaperEnabled;
        this.keybind.copyFrom(config.hpReaperKeybind);
        this.displayMode = config.hpReaperMode;
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        config.hpReaperEnabled = this.enabled;
        config.hpReaperKeybind.copyFrom(this.keybind);
        config.hpReaperMode = this.displayMode;
    }
}

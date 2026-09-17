package activity.client.module.stub;

import activity.client.config.ActivityConfig;
import activity.client.module.api.ModuleCategory;
import activity.client.module.keybind.Keybind;
import net.minecraft.text.Text;

public class AutoGGStub extends AbstractModuleStub {

    public static final String ID = "auto_gg";

    public String phrase = "GGWP";
    public boolean sendOnOwnDeath = false;

    public AutoGGStub() {
        super(
            ID,
            Text.translatable("activity.module.auto_gg.name"),
            Text.translatable("activity.module.auto_gg.desc"),
            ModuleCategory.UTILITY,
            new Keybind()
        );
    }

    @Override
    public void loadFromConfig(ActivityConfig config) {
        if (config == null) return;
        this.enabled = config.autoGGEnabled;
        this.keybind.copyFrom(config.autoGGKeybind);
        this.phrase = config.autoGGPhrase;
        this.sendOnOwnDeath = config.autoGGSendOnOwnDeath;
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        config.autoGGEnabled = this.enabled;
        config.autoGGKeybind.copyFrom(this.keybind);
        config.autoGGPhrase = this.phrase;
        config.autoGGSendOnOwnDeath = this.sendOnOwnDeath;
    }
}

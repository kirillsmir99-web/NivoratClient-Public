package activity.client.module.stub;

import activity.client.config.ActivityConfig;
import activity.client.module.api.ModuleCategory;
import activity.client.module.keybind.Keybind;
import net.minecraft.text.Text;

public class ParticlePhysicsStub extends AbstractModuleStub {

    public static final String ID = "auto_mace";

    public static final String DEFAULT_SOURCE_MODE = "sword_and_axe";
    public static final java.util.Set<String> ALLOWED_SOURCE_MODES = java.util.Set.of("any", "sword_and_axe", "sword_only", "axe_only");

    public String sourceMode = DEFAULT_SOURCE_MODE;
    public String enchantMode = "smart";
    public double restoreDelayMs = 90.0;
    public boolean legitMode = true;
    public double missChance = 10.0;

    public ParticlePhysicsStub() {
        super(
            ID,
            Text.translatable("activity.module.auto_mace.name"),
            Text.translatable("activity.module.auto_mace.desc"),
            ModuleCategory.COMBAT,
            new Keybind()
        );
    }

    public static String sanitizeSourceMode(String mode) {
        if (mode != null && ALLOWED_SOURCE_MODES.contains(mode)) {
            return mode;
        }
        return DEFAULT_SOURCE_MODE;
    }

    @Override
    public void loadFromConfig(ActivityConfig config) {
        if (config == null) return;
        this.enabled = config.autoMaceEnabled;
        this.keybind.copyFrom(config.autoMaceKeybind);
        this.sourceMode = sanitizeSourceMode(config.autoMaceSourceMode);
        this.enchantMode = config.autoMaceEnchantMode;
        this.restoreDelayMs = config.autoMaceRestoreDelayMs;
        this.legitMode = config.autoMaceLegitMode;
        this.missChance = config.autoMaceMissChance;
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        config.autoMaceEnabled = this.enabled;
        config.autoMaceKeybind.copyFrom(this.keybind);
        this.sourceMode = sanitizeSourceMode(this.sourceMode);
        config.autoMaceSourceMode = this.sourceMode;
        config.autoMaceEnchantMode = this.enchantMode;
        config.autoMaceRestoreDelayMs = this.restoreDelayMs;
        config.autoMaceLegitMode = this.legitMode;
        config.autoMaceMissChance = this.missChance;
    }
}

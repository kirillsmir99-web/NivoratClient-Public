package activity.client.module.stub;

import activity.client.config.ActivityConfig;
import activity.client.module.api.ModuleCategory;
import activity.client.module.keybind.Keybind;
import net.minecraft.text.Text;

public class OcclusionCacheStub extends AbstractModuleStub {

    public static final String ID = "auto_cart";

    public double placementChance = 70.0;
    public double railDelay = 2.0;
    public double cartDelay = 2.0;
    public double restoreDelay = 2.0;
    public boolean legitMode = true;

    public OcclusionCacheStub() {
        super(
            ID,
            Text.translatable("activity.module.auto_cart.name"),
            Text.translatable("activity.module.auto_cart.desc"),
            ModuleCategory.DEFENSE,
            new Keybind()
        );
    }

    @Override
    public void loadFromConfig(ActivityConfig config) {
        if (config == null) return;
        this.enabled = config.autoCartEnabled;
        this.keybind.copyFrom(config.autoCartKeybind);
        this.placementChance = config.autoCartPlacementChance;
        this.railDelay = config.autoCartRailDelay;
        this.cartDelay = config.autoCartCartDelay;
        this.restoreDelay = config.autoCartRestoreDelay;
        this.legitMode = config.autoCartLegitMode;
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        config.autoCartEnabled = this.enabled;
        config.autoCartKeybind.copyFrom(this.keybind);
        config.autoCartPlacementChance = this.placementChance;
        config.autoCartRailDelay = this.railDelay;
        config.autoCartCartDelay = this.cartDelay;
        config.autoCartRestoreDelay = this.restoreDelay;
        config.autoCartLegitMode = this.legitMode;
    }
}

package activity.client.module.stub;

import activity.client.config.ActivityConfig;
import activity.client.module.api.ModuleCategory;
import activity.client.module.keybind.Keybind;
import net.minecraft.text.Text;

public class ChunkBufferStub extends AbstractModuleStub {

    public static final String ID = "cart_refill";

    public double refillDelayTicks = 2.0;
    public double chance = 100.0;
    public boolean legitMode = true;
    public boolean autoClose = true;

    public ChunkBufferStub() {
        super(
            ID,
            Text.translatable("activity.module.cart_refill.name"),
            Text.translatable("activity.module.cart_refill.desc"),
            ModuleCategory.DEFENSE,
            new Keybind()
        );
    }

    @Override
    public void loadFromConfig(ActivityConfig config) {
        if (config == null) return;
        this.enabled = config.cartRefillEnabled;
        this.keybind.copyFrom(config.cartRefillKeybind);
        this.refillDelayTicks = config.cartRefillDelayTicks;
        this.chance = config.cartRefillChance;
        this.legitMode = config.cartRefillLegitMode;
        this.autoClose = config.cartRefillAutoClose;
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        config.cartRefillEnabled = this.enabled;
        config.cartRefillKeybind.copyFrom(this.keybind);
        config.cartRefillDelayTicks = this.refillDelayTicks;
        config.cartRefillChance = this.chance;
        config.cartRefillLegitMode = this.legitMode;
        config.cartRefillAutoClose = this.autoClose;
    }
}

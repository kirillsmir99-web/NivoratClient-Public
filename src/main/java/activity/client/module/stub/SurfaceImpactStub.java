package activity.client.module.stub;

import activity.client.config.ActivityConfig;
import activity.client.module.api.ModuleCategory;
import activity.client.module.keybind.Keybind;
import net.minecraft.text.Text;

public class SurfaceImpactStub extends AbstractModuleStub {

    public static final String ID = "water_drop";

    public String mode = "hotbar";
    public double fallThreshold = 4.0;
    public boolean pickupWater = true;
    public boolean switchBack = true;
    public String cameraMode = "off";
    public double pitchThreshold = 45.0;
    public double pickupDelayMs = 85.0;
    public double switchDelayMs = 130.0;
    public boolean randomDelay = true;
    public String targetSlot = "9";
    public boolean combatGuard = true;
    public boolean pearlGuard = true;
    public boolean netherAdapter = true;
    public boolean enableWater = true;
    public boolean enableWindCharge = true;
    public boolean enableHayBlock = true;
    public boolean enableSlimeBlock = true;
    public boolean enableCobweb = true;
    public boolean enablePowderSnow = true;

    public SurfaceImpactStub() {
        super(
            ID,
            Text.translatable("activity.module.water_drop.name"),
            Text.translatable("activity.module.water_drop.desc"),
            ModuleCategory.UTILITY,
            new Keybind()
        );
    }

    @Override
    public void loadFromConfig(ActivityConfig config) {
        if (config == null) return;
        this.enabled = config.waterDropEnabled;
        this.keybind.copyFrom(config.waterDropKeybind);
        this.mode = config.waterDropMode;
        this.fallThreshold = config.waterDropFallThreshold;
        this.pickupWater = config.waterDropPickupWater;
        this.switchBack = config.waterDropSwitchBack;
        this.cameraMode = config.waterDropCameraMode;
        this.pitchThreshold = config.waterDropPitchThreshold;
        this.pickupDelayMs = config.waterDropPickupDelayMs;
        this.switchDelayMs = config.waterDropSwitchDelayMs;
        this.randomDelay = config.waterDropRandomDelay;
        this.targetSlot = config.waterDropTargetSlot;
        this.combatGuard = config.waterDropCombatGuard;
        this.pearlGuard = config.waterDropPearlGuard;
        this.netherAdapter = config.waterDropNetherAdapter;
        this.enableWater = config.waterDropEnableWater;
        this.enableWindCharge = config.waterDropEnableWindCharge;
        this.enableHayBlock = config.waterDropEnableHayBlock;
        this.enableSlimeBlock = config.waterDropEnableSlimeBlock;
        this.enableCobweb = config.waterDropEnableCobweb;
        this.enablePowderSnow = config.waterDropEnablePowderSnow;
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        config.waterDropEnabled = this.enabled;
        config.waterDropKeybind.copyFrom(this.keybind);
        config.waterDropMode = this.mode;
        config.waterDropFallThreshold = this.fallThreshold;
        config.waterDropPickupWater = this.pickupWater;
        config.waterDropSwitchBack = this.switchBack;
        config.waterDropCameraMode = this.cameraMode;
        config.waterDropPitchThreshold = this.pitchThreshold;
        config.waterDropPickupDelayMs = this.pickupDelayMs;
        config.waterDropSwitchDelayMs = this.switchDelayMs;
        config.waterDropRandomDelay = this.randomDelay;
        config.waterDropTargetSlot = this.targetSlot;
        config.waterDropCombatGuard = this.combatGuard;
        config.waterDropPearlGuard = this.pearlGuard;
        config.waterDropNetherAdapter = this.netherAdapter;
        config.waterDropEnableWater = this.enableWater;
        config.waterDropEnableWindCharge = this.enableWindCharge;
        config.waterDropEnableHayBlock = this.enableHayBlock;
        config.waterDropEnableSlimeBlock = this.enableSlimeBlock;
        config.waterDropEnableCobweb = this.enableCobweb;
        config.waterDropEnablePowderSnow = this.enablePowderSnow;
    }
}

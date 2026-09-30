package activity.client.module.stub;

import activity.client.config.ActivityConfig;
import activity.client.module.api.ModuleCategory;
import activity.client.module.keybind.Keybind;
import net.minecraft.text.Text;

public class ClickPearlStub extends AbstractModuleStub {

    public static final String ID = "click_pearl";

    public String mode = "fast";
    public String searchMode = "hotbar";
    public boolean switchBack = true;
    public double switchDelayMs = 50.0;
    public boolean checkCooldown = true;
    public boolean preferOffhand = true;
    public boolean randomDelay = true;
    public boolean swingHand = true;
    public String targetSlot = "9";
    public boolean combatGuard = true;

    public ClickPearlStub() {
        super(
            ID,
            Text.translatable("activity.module.click_pearl.name"),
            Text.translatable("activity.module.click_pearl.desc"),
            ModuleCategory.COMBAT,
            new Keybind()
        );
    }

    @Override
    public void loadFromConfig(ActivityConfig config) {
        if (config == null) return;
        this.enabled = config.clickPearlEnabled;
        this.keybind.copyFrom(config.clickPearlKeybind);
        this.mode = config.clickPearlMode;
        this.searchMode = config.clickPearlSearchMode;
        this.switchBack = config.clickPearlSwitchBack;
        this.switchDelayMs = config.clickPearlSwitchDelayMs;
        this.checkCooldown = config.clickPearlCheckCooldown;
        this.preferOffhand = config.clickPearlPreferOffhand;
        this.randomDelay = config.clickPearlRandomDelay;
        this.swingHand = config.clickPearlSwingHand;
        this.targetSlot = config.clickPearlTargetSlot;
        this.combatGuard = config.clickPearlCombatGuard;
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        config.clickPearlEnabled = this.enabled;
        config.clickPearlKeybind.copyFrom(this.keybind);
        config.clickPearlMode = this.mode;
        config.clickPearlSearchMode = this.searchMode;
        config.clickPearlSwitchBack = this.switchBack;
        config.clickPearlSwitchDelayMs = this.switchDelayMs;
        config.clickPearlCheckCooldown = this.checkCooldown;
        config.clickPearlPreferOffhand = this.preferOffhand;
        config.clickPearlRandomDelay = this.randomDelay;
        config.clickPearlSwingHand = this.swingHand;
        config.clickPearlTargetSlot = this.targetSlot;
        config.clickPearlCombatGuard = this.combatGuard;
    }
}

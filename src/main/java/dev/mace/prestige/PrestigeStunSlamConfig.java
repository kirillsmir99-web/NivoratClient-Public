package dev.mace.prestige;

public final class PrestigeStunSlamConfig {
    public boolean enabled = true;
    public String mode = "full_auto";
    public double chance = 100.0;
    public double attackDelayMs = 0.0;
    public double triggerDistance = 3.0;
    public String airCondition = "blocks";
    public double minFallDistance = 3.0;
    public double airTimeSec = 1.0;
    public String enchantMode = "smart";
    public boolean silentAim = true;
    public boolean randomizer = true;
    public double randomJitterMs = 15.0;
    public boolean stayOnMace = false;

    public void resetDefaults() {
        this.enabled = true;
        this.mode = "full_auto";
        this.chance = 100.0;
        this.attackDelayMs = 0.0;
        this.triggerDistance = 3.0;
        this.airCondition = "blocks";
        this.minFallDistance = 3.0;
        this.airTimeSec = 1.0;
        this.enchantMode = "smart";
        this.silentAim = true;
        this.randomizer = true;
        this.randomJitterMs = 15.0;
        this.stayOnMace = false;
    }
}

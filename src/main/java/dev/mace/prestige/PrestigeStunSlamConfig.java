package dev.mace.prestige;

public final class PrestigeStunSlamConfig {
    public boolean enabled = true;
    public double triggerDistance = 3.0;
    public double minFallDistance = 1.3;
    public String enchantMode = "smart";
    public boolean silentAim = true;
    public boolean randomizer = true;
    public double randomJitterMs = 15.0;
    public boolean stayOnMace = false;

    public void resetDefaults() {
        this.enabled = true;
        this.triggerDistance = 3.0;
        this.minFallDistance = 1.3;
        this.enchantMode = "smart";
        this.silentAim = true;
        this.randomizer = true;
        this.randomJitterMs = 15.0;
        this.stayOnMace = false;
    }
}

package dev.mace.prestige;

public final class PrestigeAutoMaceConfig {
    public boolean enabled = true;
    public double minFallDistance = 1.25;
    public double attackDelayMs = 60.0;
    public boolean humanMode = true;
    public boolean randomJitter = true;
    public double densityThreshold = 7.0;
    public double swordMaceChance = 0.0;
    public boolean targetPlayers = true;
    public boolean targetMobs = true;
    public boolean autoSwitch = true;
    public boolean predictSwitch = false;
    public boolean unequipElytra = false;
    public boolean stayOnMace = false;
    public boolean silentAim = false;
    public double silentAimRange = 3.5;
    public boolean movementFix = true;
    public boolean breach = true;
    public boolean hitbox = false;
    public double hitboxExpand = 1.0;
    public String enchantMode = "smart";

    public void resetDefaults() {
        this.enabled = true;
        this.minFallDistance = 1.25;
        this.attackDelayMs = 60.0;
        this.humanMode = true;
        this.randomJitter = true;
        this.densityThreshold = 7.0;
        this.swordMaceChance = 0.0;
        this.targetPlayers = true;
        this.targetMobs = true;
        this.autoSwitch = true;
        this.predictSwitch = false;
        this.unequipElytra = false;
        this.stayOnMace = false;
        this.silentAim = false;
        this.silentAimRange = 3.5;
        this.movementFix = true;
        this.breach = true;
        this.hitbox = false;
        this.hitboxExpand = 1.0;
        this.enchantMode = "smart";
    }
}

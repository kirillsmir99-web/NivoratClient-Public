package dev.pearl;

public final class ClickPearlConfig {
    public static boolean enabled = true;
    public static String mode = "fast";
    public static String searchMode = "hotbar";
    public static boolean switchBack = true;
    public static boolean returnPearl = false;
    public static double switchDelayMs = 50.0;
    public static boolean checkCooldown = true;
    public static boolean preferOffhand = true;
    public static boolean randomDelay = true;
    public static boolean swingHand = true;
    public static int targetHotbarSlot = 9;
    public static boolean combatGuard = true;

    private ClickPearlConfig() {}

    public static int getTargetHotbarIndex() {
        return Math.max(0, Math.min(8, targetHotbarSlot - 1));
    }
}

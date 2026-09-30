package dev.impact;

public final class SurfaceImpactConfig {
    public static boolean enabled = true;
    public static int mode = 0;
    public static int fallThreshold = 4;
    public static boolean pickupWater = true;
    public static boolean switchBack = true;

    public static String cameraMode = "off";
    public static float pitchThreshold = 45.0F;
    public static double pickupDelayMs = 85.0;
    public static double switchDelayMs = 130.0;
    public static boolean randomDelay = true;

    public static int targetHotbarSlot = 9;
    public static boolean combatGuard = true;
    public static boolean pearlGuard = true;
    public static boolean netherAdapter = true;

    public static boolean enableWater = true;
    public static boolean enableWindCharge = true;
    public static boolean enableHayBlock = true;
    public static boolean enableSlimeBlock = true;
    public static boolean enableCobweb = true;
    public static boolean enablePowderSnow = true;

    private SurfaceImpactConfig() {}

    public static int getTargetHotbarIndex() {
        return Math.max(0, Math.min(8, targetHotbarSlot - 1));
    }
}

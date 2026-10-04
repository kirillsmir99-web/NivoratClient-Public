package dev.shader;

public final class ShaderPassConfig {
    public static final int MODE_FULL_AUTO = 0;
    public static final int MODE_SEMI_AUTO = 1;

    public static boolean enabled = true;
    public static int mode = MODE_FULL_AUTO;
    public static boolean legitMode = true;
    public static boolean abortOnManualSwitch = true;
    public static int chance = 100;
    public static double triggerDistance = 2.85D;
    public static int switchDelayMs = 50;
    public static boolean randomDelay = true;
    public static int randomMaxDelayMs = 70;
    public static int restoreDelayMs = 50;
    public static int randomMaxRestoreDelayMs = 65;
    public static double reactionDelaySec = 0.0;
    public static int cooldownTicks = 4;
    public static boolean checkAirTime = true;
    public static double maxAirTimeSec = 1.0;

    private ShaderPassConfig() {}

    public static void load() {

    }

    public static void save() {

    }
}

package dev.particle;

public final class ParticlePhysicsConfig {
    public static final int MODE_FULL_AUTO = 0;
    public static final int MODE_SEMI_AUTO = 1;

    public static final int ENCHANT_AUTO = 0;
    public static final int ENCHANT_DENSITY = 1;
    public static final int ENCHANT_BREACH = 2;

    public static boolean enabled = true;
    public static int mode = MODE_FULL_AUTO;
    public static int enchantPreference = ENCHANT_AUTO;
    public static boolean legitMode = true;
    public static int chance = 100;
    public static double triggerDistance = 2.85D;
    public static double airTimeSec = 0.1D;
    public static boolean randomDelay = false;
    public static int randomMaxDelayMs = 100;
    public static int randomMaxMaceDelayMs = 55;
    public static int randomMaxRestoreDelayMs = 50;
    public static int axeDelayMs = 0;
    public static int maceDelayMs = 0;
    public static int restoreDelayMs = 50;
    public static int cooldownTicks = 1;

    private ParticlePhysicsConfig() {}

    public static void load() {

    }

    public static void save() {

    }
}

package dev.sunder;

/**
 * In-memory runtime state for AutoStunSlam (AutoStunSlime) combat controller.
 * Bound to NivoratClient unified configuration ({@link activity.client.config.ActivityConfig}).
 * Independent file I/O has been removed in favor of NivoratConfigManager.
 */
public final class SunderConfig {
    public static final int MODE_FULL_AUTO = 0;
    public static final int MODE_SEMI_AUTO = 1;

    public static boolean enabled = true;
    public static int mode = MODE_FULL_AUTO;
    public static boolean legitMode = true;
    public static int chance = 75;
    public static double triggerDistance = 2.4D;
    public static double airTimeSec = 1.0D;
    public static boolean randomDelay = true;
    public static int randomMaxDelayMs = 100;
    public static int randomMaxMaceDelayMs = 55;
    public static int randomMaxRestoreDelayMs = 50;
    public static int axeDelayMs = 45;
    public static int maceDelayMs = 45;
    public static int restoreDelayMs = 50;
    public static int cooldownTicks = 15;

    private SunderConfig() {}

    public static void load() {
        // Bound to NivoratClient ActivityConfig — no independent file I/O
    }

    public static void save() {
        // Bound to NivoratClient ActivityConfig — no independent file I/O
    }
}

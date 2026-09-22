package dev.nivora;

/**
 * In-memory runtime state for AutoShieldbreaker combat controller.
 * Bound to NivoratClient unified configuration ({@link activity.client.config.ActivityConfig}).
 * Independent file I/O has been removed in favor of NivoratConfigManager.
 */
public final class ShieldBreakerConfig {
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

    private ShieldBreakerConfig() {}

    public static void load() {
        // Bound to NivoratClient ActivityConfig — no independent file I/O
    }

    public static void save() {
        // Bound to NivoratClient ActivityConfig — no independent file I/O
    }
}

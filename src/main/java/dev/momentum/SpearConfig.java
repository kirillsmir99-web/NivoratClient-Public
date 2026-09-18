package dev.momentum;

/**
 * In-memory runtime state for AutoSpear combat controller.
 * Bound to NivoratClient unified configuration ({@link activity.client.config.ActivityConfig}).
 * Independent file I/O has been removed in favor of NivoratConfigManager.
 */
public final class SpearConfig {
    public static final int PRIORITY_AUTO = 0;
    public static final int PRIORITY_LUNGE_1 = 1;
    public static final int PRIORITY_LUNGE_2 = 2;
    public static final int PRIORITY_LUNGE_3 = 3;
    public static final int PRIORITY_RANDOM = 4;

    public static final int MODE_LEGIT = 0;
    public static final int MODE_SEMI_LEGIT = 1;
    public static final int MODE_RAGE = 2;

    public static boolean enabled = true;
    public static int securityMode = MODE_LEGIT;
    public static boolean randomDelay = true;
    public static int maxDelayMs = 185;
    public static int missChance = 0;
    public static int priorityMode = PRIORITY_AUTO;

    private SpearConfig() {}

    public static int getMinFloor() {
        return switch (securityMode) {
            case MODE_SEMI_LEGIT -> 70;
            case MODE_RAGE -> 0;
            default -> 140;
        };
    }

    public static void load() {
        // Bound to NivoratClient ActivityConfig — no independent file I/O
    }

    public static void save() {
        // Bound to NivoratClient ActivityConfig — no independent file I/O
    }
}

package net.redstone.optimizer.config;

/**
 * In-memory runtime state for AutoMace combat engine.
 * Bound to NivoratClient unified configuration ({@link activity.client.config.ActivityConfig}).
 * Independent file I/O has been removed in favor of NivoratConfigManager.
 */
public final class RedstoneOptimizerConfig {
    public static final int MODE_SWORD_ONLY = 0;
    public static final int MODE_AXE_ONLY = 1;
    public static final int MODE_SWORD_AND_AXE = 2;

    public static final int ENCHANT_SMART = 0;
    public static final int ENCHANT_BREACH_ONLY = 1;
    public static final int ENCHANT_DENSITY_ONLY = 2;

    public static final int MISS_SWORD_HIT = 0;
    public static final int MISS_EMPTY_SWAP = 1;

    public static boolean enabled = true;
    public static int sourceMode = MODE_SWORD_AND_AXE;
    public static int enchantMode = ENCHANT_SMART;
    public static int missBehavior = MISS_SWORD_HIT;
    public static boolean randomDelay = true;
    public static int restoreDelayMs = 90;
    public static int randomMaxRestoreDelayMs = 120;
    public static boolean legitMode = true;
    public static int missChance = 10;

    private RedstoneOptimizerConfig() {}

    public static void load() {
        // Bound to NivoratClient ActivityConfig — no independent file I/O
    }

    public static void save() {
        // Bound to NivoratClient ActivityConfig — no independent file I/O
    }
}

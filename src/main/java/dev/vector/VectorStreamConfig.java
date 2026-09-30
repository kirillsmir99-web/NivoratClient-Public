package dev.vector;

public final class VectorStreamConfig {
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
    public static boolean maxSpeed = false;
    public static boolean checkCharge = false;
    public static boolean autoRepeat = true;

    private VectorStreamConfig() {}

    public static int getMinFloor() {
        if (maxSpeed || maxDelayMs <= 0) return 0;
        return switch (securityMode) {
            case MODE_SEMI_LEGIT -> Math.min(25, maxDelayMs);
            case MODE_RAGE -> 25;
            default -> Math.min(35, maxDelayMs);
        };
    }

    public static void load() {

    }

    public static void save() {

    }
}

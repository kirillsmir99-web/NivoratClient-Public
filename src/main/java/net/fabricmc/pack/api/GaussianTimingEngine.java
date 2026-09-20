package net.fabricmc.pack.api;

import java.util.Random;

public final class GaussianTimingEngine {
    private static final Random RNG = new Random();

    private GaussianTimingEngine() {}

    public static long getDelay(double mean, double stdDev, long min, long max) {
        if (max <= min) {
            return min;
        }
        double gaussian = RNG.nextGaussian();
        long delay = Math.round(mean + gaussian * stdDev);
        return Math.max(min, Math.min(max, delay));
    }

    public static long getCombatSwapDelay() {
        return getDelay(35.0D, 7.0D, 20L, 60L);
    }

    public static long getReactionDelay() {
        return getDelay(35.0D, 7.0D, 20L, 55L);
    }

    public static long getFastSwapDelay() {
        return getDelay(32.0D, 6.0D, 18L, 50L);
    }

    public static long getFastReactionDelay() {
        return getDelay(35.0D, 7.0D, 20L, 55L);
    }

    public static long getRefillOpenDelay() {
        return getDelay(28.0D, 5.0D, 15L, 45L);
    }

    public static long getRefillSwapDelay() {
        return getDelay(30.0D, 6.0D, 18L, 48L);
    }

    public static long getRefillCloseDelay() {
        return getDelay(25.0D, 5.0D, 15L, 40L);
    }

    public static long getShieldBreakerSwitchDelay() {
        return getDelay(30.0D, 6.0D, 15L, 50L);
    }

    public static long getShieldBreakerRestoreDelay() {
        return getDelay(28.0D, 5.0D, 15L, 45L);
    }

    public static long getMaceSwapDelay() {
        return getDelay(28.0D, 5.0D, 15L, 45L);
    }

    public static long getChatDelay() {
        return getDelay(950.0D, 160.0D, 700L, 1450L);
    }
}

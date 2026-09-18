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
        return getDelay(175.0D, 28.0D, 140L, 260L);
    }

    public static long getReactionDelay() {
        return getDelay(185.0D, 30.0D, 150L, 280L);
    }

    public static long getChatDelay() {
        return getDelay(950.0D, 160.0D, 700L, 1450L);
    }
}

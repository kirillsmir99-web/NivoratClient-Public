package net.fabricmc.pack.api;

import java.util.Random;

public final class GaussianTimingEngine {
    private static final Random RNG = new Random();

    private GaussianTimingEngine() {}

    public static int toActionTicks(double delayMs) {
        if (!Double.isFinite(delayMs) || delayMs <= 0.0D) return 1;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1.0D, Math.ceil(delayMs / 50.0D)));
    }

    public static int sampleActionTicks(double delayMs, boolean random) {
        int base = toActionTicks(delayMs);
        if (!random) return base;
        return (int) getDelay(base, 0.65D, Math.max(1L, (long) base - 1L), Math.min(Integer.MAX_VALUE, (long) base + 2L));
    }

    public static long getDelay(double mean, double stdDev, long min, long max) {
        if (max <= min) {
            return min;
        }
        double gaussian = RNG.nextGaussian();
        long delay = Math.round(mean + gaussian * stdDev);
        return Math.max(min, Math.min(max, delay));
    }

    public static long getCombatSwapDelay() {
        return getDelay(135.0D, 20.0D, 115L, 190L);
    }

    public static long getReactionDelay() {
        return getDelay(160.0D, 30.0D, 120L, 240L);
    }

    public static long getFastSwapDelay() {
        return getDelay(140.0D, 22.0D, 120L, 200L);
    }

    public static long getFastReactionDelay() {
        return getDelay(150.0D, 25.0D, 110L, 220L);
    }

    public static long getRefillOpenDelay() {
        return getDelay(105.0D, 22.0D, 75L, 160L);
    }

    public static long getRefillSwapDelay() {
        return getDelay(115.0D, 25.0D, 80L, 175L);
    }

    public static long getRefillCloseDelay() {
        return getDelay(95.0D, 20.0D, 65L, 150L);
    }

    public static long getShieldBreakerSwitchDelay() {
        return getDelay(180.0D, 35.0D, 140L, 260L);
    }

    public static long getShieldBreakerRestoreDelay() {
        return getDelay(95.0D, 18.0D, 70L, 140L);
    }

    public static long getMaceSwapDelay() {
        return getDelay(145.0D, 25.0D, 120L, 220L);
    }

    public static long getChatDelay() {
        return getDelay(950.0D, 160.0D, 700L, 1450L);
    }

    public static final long FAST_LEGIT_MIN_MS = 60L;
    public static final long FAST_LEGIT_MAX_MS = 180L;
    public static final int FAST_LEGIT_MIN_TICKS = 2;
    public static final int FAST_LEGIT_MAX_TICKS = 4;

    public static final double REFILL_OPEN_MEAN_MS = 105.0D;
    public static final double REFILL_OPEN_STD_DEV_MS = 22.0D;

    public static final double REFILL_SWAP_MEAN_MS = 115.0D;
    public static final double REFILL_SWAP_STD_DEV_MS = 25.0D;

    public static final double REFILL_CLOSE_MEAN_MS = 95.0D;
    public static final double REFILL_CLOSE_STD_DEV_MS = 20.0D;

    public static long getFastLegitDelay(double mean, double stdDev) {
        return getDelay(mean, stdDev, FAST_LEGIT_MIN_MS, FAST_LEGIT_MAX_MS);
    }

    public static int toFastLegitTicks(long ms) {
        return Math.max(FAST_LEGIT_MIN_TICKS, Math.min(FAST_LEGIT_MAX_TICKS, (int) Math.round(ms / 50.0D)));
    }

    public static int msToTicks(long ms) {
        return toFastLegitTicks(ms);
    }

    public static int getFastLegitDelayTicks(double mean, double stdDev) {
        return toFastLegitTicks(getFastLegitDelay(mean, stdDev));
    }

    public static long getFastLegitRefillOpenDelayMs() {
        return getFastLegitDelay(REFILL_OPEN_MEAN_MS, REFILL_OPEN_STD_DEV_MS);
    }

    public static int getFastLegitRefillOpenDelayTicks() {
        return toFastLegitTicks(getFastLegitRefillOpenDelayMs());
    }

    public static long getFastLegitRefillSwapDelayMs() {
        return getFastLegitDelay(REFILL_SWAP_MEAN_MS, REFILL_SWAP_STD_DEV_MS);
    }

    public static int getFastLegitRefillSwapDelayTicks() {
        return toFastLegitTicks(getFastLegitRefillSwapDelayMs());
    }

    public static long getFastLegitRefillCloseDelayMs() {
        return getFastLegitDelay(REFILL_CLOSE_MEAN_MS, REFILL_CLOSE_STD_DEV_MS);
    }

    public static int getFastLegitRefillCloseDelayTicks() {
        return toFastLegitTicks(getFastLegitRefillCloseDelayMs());
    }
}

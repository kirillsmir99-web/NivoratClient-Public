package net.fabricmc.pack.api;

import activity.client.internal.timing.TimingDomain;
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

    private static double sessionDriftOffset = 0.0D;
    private static long lastDriftUpdateTime = 0L;

    private static double getSessionDrift() {
        long now = System.currentTimeMillis();
        if (now - lastDriftUpdateTime > 20000L) {
            lastDriftUpdateTime = now;
            sessionDriftOffset = (RNG.nextDouble() - 0.5D) * 10.0D;
        }
        return sessionDriftOffset;
    }

    public static long getDelay(double mean, double stdDev, long min, long max) {
        if (max <= min) {
            return min;
        }
        double gaussian = RNG.nextGaussian();
        double drift = (mean >= 10.0D) ? getSessionDrift() : 0.0D;
        long delay = Math.round(mean + drift + gaussian * stdDev);
        return Math.max(min, Math.min(max, delay));
    }

    public static long getCombatSwapDelay() {
        return getDelay(TimingDomain.d(2259855304760987213L) , TimingDomain.d(2236176222345203277L) , TimingDomain.l(8893018580978572924L) , TimingDomain.l(8893018580978572977L) );
    }

    public static long getReactionDelay() {
        return getDelay(TimingDomain.d(2258694220482055757L) , TimingDomain.d(2234487372484939341L) , TimingDomain.l(8893018580978572919L) , TimingDomain.l(8893018580978573055L) );
    }

    public static long getFastSwapDelay() {
        return getDelay(TimingDomain.d(2260242332853964365L) , TimingDomain.d(2236739172298624589L) , TimingDomain.l(8893018580978572919L) , TimingDomain.l(8893018580978572999L) );
    }

    public static long getFastReactionDelay() {
        return getDelay(TimingDomain.d(2260453439086497357L) , TimingDomain.d(2235331797415071309L) , TimingDomain.l(8893018580978572897L) , TimingDomain.l(8893018580978573011L) );
    }

    public static long getRefillOpenDelay() {
        return getDelay(TimingDomain.d(2262564501411827277L) , TimingDomain.d(2236739172298624589L) , TimingDomain.l(8893018580978572868L) , TimingDomain.l(8893018580978572975L) );
    }

    public static long getRefillSwapDelay() {
        return getDelay(TimingDomain.d(2261016389039918669L) , TimingDomain.d(2235331797415071309L) , TimingDomain.l(8893018580978572895L) , TimingDomain.l(8893018580978572960L) );
    }

    public static long getRefillCloseDelay() {
        return getDelay(TimingDomain.d(2264112613783735885L) , TimingDomain.d(2236176222345203277L) , TimingDomain.l(8893018580978572878L) , TimingDomain.l(8893018580978572953L) );
    }

    public static long getShieldBreakerSwitchDelay() {
        return getDelay(TimingDomain.d(2259397907923832397L) , TimingDomain.d(2269249532108705357L) , TimingDomain.l(8893018580978572931L) , TimingDomain.l(8893018580978573067L) );
    }

    public static long getShieldBreakerRestoreDelay() {
        return getDelay(TimingDomain.d(2264112613783735885L) , TimingDomain.d(2237865072205467213L) , TimingDomain.l(8893018580978572873L) , TimingDomain.l(8893018580978572931L) );
    }

    public static long getMaceSwapDelay() {
        return getDelay(TimingDomain.d(2260347885970230861L) , TimingDomain.d(2235331797415071309L) , TimingDomain.l(8893018580978572919L) , TimingDomain.l(8893018580978573011L) );
    }

    public static long getChatDelay() {
        return getDelay(TimingDomain.d(2283833454339526221L) , TimingDomain.d(2258694220482055757L) , TimingDomain.l(8893018580978572467L) , TimingDomain.l(8893018580978572197L) );
    }

    public static final long FAST_LEGIT_MIN_MS = TimingDomain.l(8893018580978572851L) ;
    public static final long FAST_LEGIT_MAX_MS = TimingDomain.l(8893018580978572987L) ;
    public static final int FAST_LEGIT_MIN_TICKS = 2;
    public static final int FAST_LEGIT_MAX_TICKS = 4;

    public static final double REFILL_OPEN_MEAN_MS = TimingDomain.d(2262564501411827277L) ;
    public static final double REFILL_OPEN_STD_DEV_MS = TimingDomain.d(2236739172298624589L) ;

    public static final double REFILL_SWAP_MEAN_MS = TimingDomain.d(2261016389039918669L) ;
    public static final double REFILL_SWAP_STD_DEV_MS = TimingDomain.d(2235331797415071309L) ;

    public static final double REFILL_CLOSE_MEAN_MS = TimingDomain.d(2264112613783735885L) ;
    public static final double REFILL_CLOSE_STD_DEV_MS = TimingDomain.d(2236176222345203277L) ;

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

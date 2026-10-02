package dev.nivorat.arc;

import net.minecraft.util.math.MathHelper;

public final class ArcActionValidator {
    public static final float MIN_VALID_DELTA = 0.05f;
    public static final float MAX_VALID_DELTA = 90.0f;
    public static final float MAX_VELOCITY_DEG_S = 3500.0f;
    public static final float MAX_ACCELERATION_DEG_S2 = 45000.0f;
    public static final long MIN_ACTION_DELAY_MS = 20L;
    public static final long MAX_ACTION_DELAY_MS = 4000L;

    private ArcActionValidator() {}

    public static boolean isValidKinematicDelta(float deltaPitch, float deltaYaw, double dtSec) {
        if (!Float.isFinite(deltaPitch) || !Float.isFinite(deltaYaw) || !Double.isFinite(dtSec)) {
            return false;
        }
        if (dtSec < 0.005 || dtSec > 0.35) {
            return false;
        }
        if (Math.abs(deltaPitch) > 90.0f || Math.abs(deltaYaw) > 180.0f) {
            return false;
        }
        float magSq = deltaPitch * deltaPitch + deltaYaw * deltaYaw;
        if (magSq < (MIN_VALID_DELTA * MIN_VALID_DELTA) || magSq > (MAX_VALID_DELTA * MAX_VALID_DELTA)) {
            return false;
        }
        float mag = (float) Math.sqrt(magSq);
        float velocity = (float) (mag / dtSec);
        return velocity <= MAX_VELOCITY_DEG_S;
    }

    public static boolean isValidActionDelay(long delayMs) {
        return delayMs >= MIN_ACTION_DELAY_MS && delayMs <= MAX_ACTION_DELAY_MS;
    }

    public static boolean isValidBowDrawTicks(int ticks) {
        return ticks >= 3 && ticks <= 20;
    }

    public static float sanitizeAngleDelta(float angle) {
        if (!Float.isFinite(angle)) return 0.0f;
        return MathHelper.wrapDegrees(angle);
    }
}

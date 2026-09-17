package activity.client.gui.animation;

/**
 * Standard easing functions for smooth UI transitions and interpolation.
 */
@FunctionalInterface
public interface Easing {

    /**
     * Maps normalized linear time [0.0..1.0] to an eased interpolation factor.
     *
     * @param t normalized time between 0.0 and 1.0
     * @return eased value
     */
    float ease(float t);

    /** Linear constant velocity interpolation. */
    Easing LINEAR = t -> Math.clamp(t, 0.0f, 1.0f);

    /** Quadratic ease-out: brisk start, smooth deceleration. Ideal for hover states. */
    Easing EASE_OUT_QUAD = t -> {
        float c = Math.clamp(t, 0.0f, 1.0f);
        return 1.0f - (1.0f - c) * (1.0f - c);
    };

    /** Cubic ease-out: pronounced smooth deceleration. Standard for popups, drawers, and panels. */
    Easing EASE_OUT_CUBIC = t -> {
        float c = Math.clamp(t, 0.0f, 1.0f);
        float inv = 1.0f - c;
        return 1.0f - inv * inv * inv;
    };

    /** Quadratic ease-in-out: smooth acceleration followed by smooth deceleration. */
    Easing EASE_IN_OUT_QUAD = t -> {
        float c = Math.clamp(t, 0.0f, 1.0f);
        return c < 0.5f ? 2.0f * c * c : 1.0f - (float) Math.pow(-2.0f * c + 2.0f, 2) / 2.0f;
    };

    /** Quadratic ease-in: subtle acceleration from rest. */
    Easing EASE_IN_QUAD = t -> {
        float c = Math.clamp(t, 0.0f, 1.0f);
        return c * c;
    };

    /** Cubic ease-in: pronounced acceleration from rest. Standard for exit transitions. */
    Easing EASE_IN_CUBIC = t -> {
        float c = Math.clamp(t, 0.0f, 1.0f);
        return c * c * c;
    };

    /** Back ease-out: subtle overshoot spring effect. Excellent for toggle switches and button clicks. */
    Easing EASE_OUT_BACK = t -> {
        float c = Math.clamp(t, 0.0f, 1.0f);
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        return 1.0f + c3 * (float) Math.pow(c - 1.0f, 3) + c1 * (float) Math.pow(c - 1.0f, 2);
    };

    /** Smoothstep interpolation (Hermite ease-in-out): zero velocity at endpoints [0.0..1.0]. */
    Easing SMOOTH_STEP = t -> {
        float c = Math.clamp(t, 0.0f, 1.0f);
        return c * c * (3.0f - 2.0f * c);
    };
}

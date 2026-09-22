package activity.client.gui.animation;

@FunctionalInterface
public interface Easing {

    float ease(float t);

    Easing LINEAR = t -> Math.clamp(t, 0.0f, 1.0f);

    Easing EASE_OUT_QUAD = t -> {
        float c = Math.clamp(t, 0.0f, 1.0f);
        return 1.0f - (1.0f - c) * (1.0f - c);
    };

    Easing EASE_OUT_CUBIC = t -> {
        float c = Math.clamp(t, 0.0f, 1.0f);
        float inv = 1.0f - c;
        return 1.0f - inv * inv * inv;
    };

    Easing EASE_IN_OUT_QUAD = t -> {
        float c = Math.clamp(t, 0.0f, 1.0f);
        return c < 0.5f ? 2.0f * c * c : 1.0f - (float) Math.pow(-2.0f * c + 2.0f, 2) / 2.0f;
    };

    Easing EASE_IN_QUAD = t -> {
        float c = Math.clamp(t, 0.0f, 1.0f);
        return c * c;
    };

    Easing EASE_IN_CUBIC = t -> {
        float c = Math.clamp(t, 0.0f, 1.0f);
        return c * c * c;
    };

    Easing EASE_OUT_BACK = t -> {
        float c = Math.clamp(t, 0.0f, 1.0f);
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        return 1.0f + c3 * (float) Math.pow(c - 1.0f, 3) + c1 * (float) Math.pow(c - 1.0f, 2);
    };

    Easing SMOOTH_STEP = t -> {
        float c = Math.clamp(t, 0.0f, 1.0f);
        return c * c * (3.0f - 2.0f * c);
    };
}

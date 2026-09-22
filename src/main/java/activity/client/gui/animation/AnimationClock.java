package activity.client.gui.animation;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;

public final class AnimationClock {

    public static final float DURATION_HOVER = 0.10f;
    public static final float DURATION_FOCUS = 0.12f;
    public static final float DURATION_TOGGLE = 0.11f;
    public static final float DURATION_EXPAND = 0.14f;
    public static final float DURATION_TAB_SWITCH = 0.16f;
    public static final float DURATION_SCREEN_OPEN = 0.22f;
    public static final float DURATION_SCREEN_CLOSE = 0.15f;
    public static final double SCROLL_DECAY_RATE = 18.0;

    private static long lastFrameNano = 0;
    private static float deltaTime = 0.016f;
    private static float totalElapsedSeconds = 0.0f;

    private AnimationClock() {}

    public static boolean isAnimationsEnabled() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        return config == null || config.animationsEnabled;
    }

    public static void tick() {
        update();
    }

    public static void update() {
        long now = System.nanoTime();
        if (lastFrameNano > 0) {
            float rawDt = (now - lastFrameNano) / 1_000_000_000.0f;
            deltaTime = Math.clamp(rawDt, 0.001f, 0.05f);
        } else {
            deltaTime = 0.016f;
        }
        totalElapsedSeconds += deltaTime;
        lastFrameNano = now;
    }

    public static void reset() {
        lastFrameNano = 0;
        deltaTime = 0.016f;
    }

    public static void resetAll() {
        reset();
        totalElapsedSeconds = 0.0f;
    }

    public static float getDeltaTime() {
        return deltaTime;
    }

    public static float getElapsedSeconds() {
        return totalElapsedSeconds;
    }

    public static boolean isCursorBlinkVisible() {
        return ((int) (totalElapsedSeconds * 2.0f)) % 2 == 0;
    }

    public static boolean isCursorBlinkVisible(float startOffsetSeconds) {
        return ((int) (Math.max(0.0f, totalElapsedSeconds - startOffsetSeconds) * 2.0f)) % 2 == 0;
    }

    public static void advanceForTesting(float seconds) {
        deltaTime = Math.max(0.001f, seconds);
        totalElapsedSeconds += Math.max(0.0f, seconds);
    }

    public static float getPulse(float frequency) {
        if (!isAnimationsEnabled()) {
            return 1.0f;
        }
        return (float) ((Math.sin(totalElapsedSeconds * frequency) + 1.0) * 0.5);
    }

    public static float approach(float current, float target, float durationSeconds) {
        return approach(current, target, durationSeconds, deltaTime);
    }

    public static float approach(float current, float target, float durationSeconds, float dt) {
        if (!isAnimationsEnabled() || durationSeconds <= 0.0f) return target;
        if (dt <= 0.0f || Float.isNaN(dt)) return current;
        float speed = 1.0f / durationSeconds;
        float step = speed * dt;
        if (current < target) {
            return Math.min(target, current + step);
        } else if (current > target) {
            return Math.max(target, current - step);
        }
        return target;
    }

    public static float approachExp(float current, float target, float decayRate) {
        return approachExp(current, target, decayRate, deltaTime);
    }

    public static float approachExp(float current, float target, float decayRate, float dt) {
        if (!isAnimationsEnabled()) return target;
        if (dt <= 0.0f || Float.isNaN(dt)) return current;
        float factor = 1.0f - (float) Math.exp(-decayRate * dt);
        return current + (target - current) * factor;
    }

    public static double approachExp(double current, double target, double decayRate) {
        return approachExp(current, target, decayRate, (double) deltaTime);
    }

    public static double approachExp(double current, double target, double decayRate, double dt) {
        if (!isAnimationsEnabled() || decayRate <= 0.0f) return target;
        double clampedDt = Math.clamp(dt, 0.0001, 0.10);
        return current + (target - current) * (1.0 - Math.exp(-decayRate * clampedDt));
    }

    public static float easeOutQuad(float t) {
        float clamped = Math.clamp(t, 0.0f, 1.0f);
        return 1.0f - (1.0f - clamped) * (1.0f - clamped);
    }

    public static float easeOutCubic(float t) {
        float clamped = Math.clamp(t, 0.0f, 1.0f);
        float inv = 1.0f - clamped;
        return 1.0f - inv * inv * inv;
    }

    public static float easeInQuad(float t) {
        float clamped = Math.clamp(t, 0.0f, 1.0f);
        return clamped * clamped;
    }

    public static float easeInCubic(float t) {
        float clamped = Math.clamp(t, 0.0f, 1.0f);
        return clamped * clamped * clamped;
    }

    public static float smoothStep(float t) {
        float clamped = Math.clamp(t, 0.0f, 1.0f);
        return clamped * clamped * (3.0f - 2.0f * clamped);
    }
}

package activity.client.gui.animation;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;

/**
 * Centralized high-precision animation clock and transition math for the Activity GUI framework.
 *
 * <p>Provides:
 * <ul>
 *   <li>Single frame-rate independent delta time (dt) updated once per frame in {@code ActivityScreen.render}.</li>
 *   <li>Guaranteed spike-clamped delta time (1ms - 50ms) to eliminate animation skips after window freezes or lag spikes.</li>
 *   <li>Frame-rate independent linear and exponential transition formulas adhering to the 80-150ms budget.</li>
 *   <li>Global animation bypass when animations are disabled in configuration.</li>
 *   <li>Standardized easing functions (Quad, Cubic).</li>
 * </ul>
 */
public final class AnimationClock {

    public static final float DURATION_HOVER = 0.10f;          // 100ms transition
    public static final float DURATION_FOCUS = 0.12f;          // 120ms transition
    public static final float DURATION_TOGGLE = 0.11f;         // 110ms transition
    public static final float DURATION_EXPAND = 0.14f;         // 140ms accordion expand
    public static final float DURATION_TAB_SWITCH = 0.16f;     // 160ms tab content crossfade
    public static final float DURATION_SCREEN_OPEN = 0.22f;    // 220ms spatial window open
    public static final float DURATION_SCREEN_CLOSE = 0.15f;   // 150ms spatial window close
    public static final double SCROLL_DECAY_RATE = 18.0;       // ~120ms exponential response

    private static long lastFrameNano = 0;
    private static float deltaTime = 0.016f; // Initial default ~60 FPS (16.6ms)
    private static float totalElapsedSeconds = 0.0f;

    private AnimationClock() {}

    /**
     * @return true if smooth animations are enabled in user configuration.
     */
    public static boolean isAnimationsEnabled() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        return config == null || config.animationsEnabled;
    }

    /**
     * Centralized per-frame tick for the animation system.
     * Equivalent to {@link #update()}.
     */
    public static void tick() {
        update();
    }

    /**
     * Updates the global animation clock. Must be invoked once at the beginning of each frame in the top-level Screen render loop.
     */
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

    /**
     * Resets the clock upon screen open to prevent delta spikes.
     */
    public static void reset() {
        lastFrameNano = 0;
        deltaTime = 0.016f;
    }

    /**
     * Resets the clock including total elapsed time.
     */
    public static void resetAll() {
        reset();
        totalElapsedSeconds = 0.0f;
    }

    /**
     * @return current frame delta time in seconds, clamped between 0.001s and 0.050s
     */
    public static float getDeltaTime() {
        return deltaTime;
    }

    /**
     * @return total accumulated animation seconds tracked by this central clock
     */
    public static float getElapsedSeconds() {
        return totalElapsedSeconds;
    }

    /**
     * Frame-synchronized standard text cursor blink state (500ms cycle).
     */
    public static boolean isCursorBlinkVisible() {
        return ((int) (totalElapsedSeconds * 2.0f)) % 2 == 0;
    }

    /**
     * Frame-synchronized text cursor blink state with start offset.
     */
    public static boolean isCursorBlinkVisible(float startOffsetSeconds) {
        return ((int) (Math.max(0.0f, totalElapsedSeconds - startOffsetSeconds) * 2.0f)) % 2 == 0;
    }

    /**
     * Advances the clock by a specified duration in seconds for unit testing and deterministic simulation.
     */
    public static void advanceForTesting(float seconds) {
        deltaTime = Math.max(0.001f, seconds);
        totalElapsedSeconds += Math.max(0.0f, seconds);
    }

    /**
     * Frame-synchronized harmonic pulse in range [0.0f..1.0f].
     * Returns solid 1.0f if animations are disabled.
     *
     * @param frequency cycles per second factor
     */
    public static float getPulse(float frequency) {
        if (!isAnimationsEnabled()) {
            return 1.0f;
        }
        return (float) ((Math.sin(totalElapsedSeconds * frequency) + 1.0) * 0.5);
    }

    /**
     * Linearly approaches target with constant duration.
     * Snaps immediately if animations are disabled.
     *
     * @param current         current progress [0.0f..1.0f]
     * @param target          target progress [0.0f..1.0f]
     * @param durationSeconds transition duration in seconds
     * @return new progress
     */
    public static float approach(float current, float target, float durationSeconds) {
        return approach(current, target, durationSeconds, deltaTime);
    }

    /**
     * Linearly approaches target with constant duration using an explicit delta time.
     * Snaps immediately if animations are disabled.
     *
     * @param current         current progress [0.0f..1.0f]
     * @param target          target progress [0.0f..1.0f]
     * @param durationSeconds transition duration in seconds
     * @param dt              delta time in seconds
     * @return new progress
     */
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

    /**
     * Exponential decay smoothing towards target (framerate-independent ease-out).
     * Snaps immediately if animations are disabled.
     *
     * @param current   current value
     * @param target    target value
     * @param decayRate decay velocity factor
     * @return new smoothed value
     */
    public static float approachExp(float current, float target, float decayRate) {
        return approachExp(current, target, decayRate, deltaTime);
    }

    /**
     * Exponential decay smoothing towards target with explicit delta time.
     * Snaps immediately if animations are disabled.
     *
     * @param current   current value
     * @param target    target value
     * @param decayRate decay velocity factor
     * @param dt        delta time in seconds
     * @return new smoothed value
     */
    public static float approachExp(float current, float target, float decayRate, float dt) {
        if (!isAnimationsEnabled()) return target;
        if (dt <= 0.0f || Float.isNaN(dt)) return current;
        float factor = 1.0f - (float) Math.exp(-decayRate * dt);
        return current + (target - current) * factor;
    }

    /**
     * Double-precision exponential decay smoothing.
     * Snaps immediately if animations are disabled.
     *
     * @param current   current value
     * @param target    target value
     * @param decayRate decay velocity factor
     * @return new smoothed value
     */
    public static double approachExp(double current, double target, double decayRate) {
        return approachExp(current, target, decayRate, (double) deltaTime);
    }

    /**
     * Double-precision exponential decay smoothing with explicit delta time.
     * Snaps immediately if animations are disabled.
     *
     * @param current   current value
     * @param target    target value
     * @param decayRate decay velocity factor
     * @param dt        delta time in seconds
     * @return new smoothed value
     */
    public static double approachExp(double current, double target, double decayRate, double dt) {
        if (!isAnimationsEnabled() || decayRate <= 0.0f) return target;
        double clampedDt = Math.clamp(dt, 0.0001, 0.10);
        return current + (target - current) * (1.0 - Math.exp(-decayRate * clampedDt));
    }

    /**
     * Quadratic ease-out: starts briskly, smoothly decelerates.
     */
    public static float easeOutQuad(float t) {
        float clamped = Math.clamp(t, 0.0f, 1.0f);
        return 1.0f - (1.0f - clamped) * (1.0f - clamped);
    }

    /**
     * Cubic ease-out: pronounced smooth deceleration for popups and panels.
     */
    public static float easeOutCubic(float t) {
        float clamped = Math.clamp(t, 0.0f, 1.0f);
        float inv = 1.0f - clamped;
        return 1.0f - inv * inv * inv;
    }

    /**
     * Quadratic ease-in: subtle acceleration from rest.
     */
    public static float easeInQuad(float t) {
        float clamped = Math.clamp(t, 0.0f, 1.0f);
        return clamped * clamped;
    }

    /**
     * Cubic ease-in: pronounced acceleration from rest. Ideal for exit transitions.
     */
    public static float easeInCubic(float t) {
        float clamped = Math.clamp(t, 0.0f, 1.0f);
        return clamped * clamped * clamped;
    }

    /**
     * Smoothstep interpolation (Hermite ease-in-out): zero velocity at boundaries [0.0..1.0].
     */
    public static float smoothStep(float t) {
        float clamped = Math.clamp(t, 0.0f, 1.0f);
        return clamped * clamped * (3.0f - 2.0f * clamped);
    }
}

package activity.client.gui.animation;

/**
 * Reusable animated float value supporting easing curves, time-based delta updates,
 * and automatic instant snapping when animations are disabled in configuration.
 *
 * <p>Prevents scattered, inconsistent interpolation logic across UI components.
 */
public class AnimatedValue {

    private float current;
    private float target;
    private float startValue;
    private float elapsed;
    private float duration;
    private Easing easing;

    public AnimatedValue(float initialValue, float duration, Easing easing) {
        this.current = initialValue;
        this.target = initialValue;
        this.startValue = initialValue;
        this.duration = Math.max(0.001f, duration);
        this.elapsed = this.duration;
        this.easing = easing != null ? easing : Easing.EASE_OUT_CUBIC;
    }

    public AnimatedValue(float initialValue, float duration) {
        this(initialValue, duration, Easing.EASE_OUT_CUBIC);
    }

    public AnimatedValue(float initialValue) {
        this(initialValue, AnimationClock.DURATION_HOVER, Easing.EASE_OUT_QUAD);
    }

    /**
     * Sets a new animation target. If animations are disabled, immediately snaps to target.
     */
    public void setTarget(float newTarget) {
        if (Math.abs(this.target - newTarget) < 0.0001f) {
            return;
        }
        if (!AnimationClock.isAnimationsEnabled()) {
            this.current = newTarget;
            this.target = newTarget;
            this.startValue = newTarget;
            this.elapsed = this.duration;
            return;
        }
        this.startValue = this.current;
        this.target = newTarget;
        this.elapsed = 0.0f;
    }

    /**
     * Immediately snaps the current and target value without transition.
     */
    public void snap(float value) {
        this.current = value;
        this.target = value;
        this.startValue = value;
        this.elapsed = this.duration;
    }

    /**
     * Advances the animation by delta time (in seconds).
     */
    public void update(float dt) {
        if (!AnimationClock.isAnimationsEnabled()) {
            this.current = this.target;
            this.elapsed = this.duration;
            return;
        }
        float clampedDt = Math.clamp(dt, 0.0f, 0.10f);
        if (this.elapsed < this.duration) {
            this.elapsed = Math.min(this.duration, this.elapsed + clampedDt);
            float progress = this.elapsed / this.duration;
            float eased = this.easing.ease(progress);
            this.current = this.startValue + (this.target - this.startValue) * eased;
        } else {
            this.current = this.target;
        }
    }

    /**
     * Advances the animation using the centralized AnimationClock delta time.
     */
    public void update() {
        this.update(AnimationClock.getDeltaTime());
    }

    public float get() {
        return this.current;
    }

    public float getTarget() {
        return this.target;
    }

    public boolean isFinished() {
        return this.elapsed >= this.duration || !AnimationClock.isAnimationsEnabled();
    }

    public void setDuration(float duration) {
        this.duration = Math.max(0.001f, duration);
    }

    public void setEasing(Easing easing) {
        this.easing = easing != null ? easing : Easing.EASE_OUT_CUBIC;
    }
}

package activity.client.capitulation;

public final class HoldConfirmation {
    private final long durationNanos;
    private long startedAt;
    private boolean armed;

    public HoldConfirmation(long durationNanos) {
        if (durationNanos <= 0) throw new IllegalArgumentException("Hold duration must be positive");
        this.durationNanos = durationNanos;
    }

    public void begin(long now) {
        if (armed) return;
        startedAt = now;
        armed = true;
    }

    public boolean update(long now, boolean eligible) {
        if (!eligible) cancel();
        if (!armed || now - startedAt < durationNanos) return false;
        cancel();
        return true;
    }

    public float progress(long now) {
        return armed ? Math.clamp((float)(now - startedAt) / durationNanos, 0.0f, 1.0f) : 0.0f;
    }

    public void cancel() { armed = false; }
    public boolean isArmed() { return armed; }
}

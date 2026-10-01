package activity.client.gui.custom;

public final class HoverDelay<T> {
    private final long delayNanos;
    private T current;
    private long since;

    public HoverDelay(long delayNanos) { this.delayNanos = delayNanos; }
    public void update(T hovered, long now) {
        if (current != hovered) { current = hovered; since = now; }
    }
    public T ready(long now) { return current != null && now - since >= delayNanos ? current : null; }
}

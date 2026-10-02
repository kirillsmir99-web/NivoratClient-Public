package dev.nivorat.arc;

public record ArcMotorFrame(
        long timestampMs,
        float deltaPitch,
        float deltaYaw,
        float magnitude,
        float velocity,
        float acceleration,
        int quadrant,
        ActionType actionType,
        long actionDurationMs,
        boolean playerMoving
) {
    public ArcMotorFrame(long timestampMs, float deltaPitch, float deltaYaw, float magnitude, float velocity, float acceleration, int quadrant, ActionType actionType, long actionDurationMs) {
        this(timestampMs, deltaPitch, deltaYaw, magnitude, velocity, acceleration, quadrant, actionType, actionDurationMs, false);
    }

    public enum ActionType {
        NONE,
        CAMERA_TICK,
        BOW_RELEASE,
        RAIL_PLACE,
        CART_PLACE,
        EXPLOSION
    }

    public static int computeQuadrant(float deltaPitch, float deltaYaw) {
        double angleRad = Math.atan2(deltaPitch, deltaYaw);
        double angleDeg = Math.toDegrees(angleRad);
        if (angleDeg < 0) {
            angleDeg += 360.0;
        }
        int quad = (int) Math.floor((angleDeg + 22.5) / 45.0) % 8;
        return Math.max(0, Math.min(7, quad));
    }
}

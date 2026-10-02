package dev.raycast.async;

public final class AsyncSilentRot {
    private static volatile boolean active;
    private static volatile float y;
    private static volatile float p;
    private static volatile Object holder;
    private static volatile boolean moving;
    private static volatile float realY;

    private AsyncSilentRot() {
    }

    public static void set(float yaw, float pitch, Object who) {
        y = yaw;
        p = pitch;
        active = true;
        holder = who;
    }

    public static void beginMove(float realYaw) {
        realY = realYaw;
        moving = true;
    }

    public static void endMove() {
        moving = false;
    }

    public static boolean moving() {
        return moving;
    }

    public static float real() {
        return realY;
    }

    public static void stop(Object who) {
        if (holder == who) {
            active = false;
            holder = null;
        }
    }

    public static void forceStop() {
        active = false;
        holder = null;
        moving = false;
    }

    public static boolean on() {
        return active;
    }

    public static float yaw() {
        return y;
    }

    public static float pitch() {
        return p;
    }
}

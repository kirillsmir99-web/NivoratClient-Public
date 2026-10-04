package dev.raycast.internal;

public final class RaycastDomain {
    private static final int FLOAT_MASK = 0x3D1A9C8B;
    private static final long DOUBLE_MASK = 0x3D1A9C8B5F2E7D1AL;
    private static final int INT_MASK = 0x6C5B4A39;

    private RaycastDomain() {}

    public static float f(int bits) {
        return Float.intBitsToFloat(bits ^ FLOAT_MASK);
    }

    public static double d(long bits) {
        return Double.longBitsToDouble(bits ^ DOUBLE_MASK);
    }

    public static int i(int bits) {
        return bits ^ INT_MASK;
    }
}

package dev.mace.prestige.internal;

public final class MaceDomain {
    private static final int FLOAT_MASK = 0x7C9E5F3A;
    private static final long DOUBLE_MASK = 0x7C9E5F3A1D4E8F2CL;
    private static final int INT_MASK = 0x3E2D1C0B;

    private MaceDomain() {}

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

package dev.nivorat.arc.internal;

public final class ArcDomain {
    private static final int FLOAT_MASK = 0x6B8D4E2F;
    private static final long DOUBLE_MASK = 0x6B8D4E2F7A1C5B3EL;
    private static final int INT_MASK = 0x4A3C2B1D;

    private ArcDomain() {}

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

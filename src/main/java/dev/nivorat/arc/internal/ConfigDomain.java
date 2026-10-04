package dev.nivorat.arc.internal;

public final class ConfigDomain {
    private static final int FLOAT_MASK = 0x2A9F1B8E;
    private static final long DOUBLE_MASK = 0x2A9F1B8E4D3C2B1AL;
    private static final int INT_MASK = 0x1F2E3D4C;

    private ConfigDomain() {}

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

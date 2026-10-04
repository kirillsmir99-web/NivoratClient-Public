package net.fabricmc.pack.api.internal;

public final class CombatDomain {
    private static final int FLOAT_MASK = 0x4E2B8D7C;
    private static final long DOUBLE_MASK = 0x4E2B8D7C3B1A9F5EL;
    private static final int INT_MASK = 0x5D4C3B2A;

    private CombatDomain() {}

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

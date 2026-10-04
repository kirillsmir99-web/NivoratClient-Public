package activity.client.internal.timing;

public final class TimingDomain {
    private static final int FLOAT_MASK = 0x5F3C7E6D;
    private static final long DOUBLE_MASK = 0x5F3C7E6D2C0F8E4DL;
    private static final int INT_MASK = 0x7B6A5948;
    private static final long LONG_MASK = 0x7B6A59483C2D1E0FL;

    private TimingDomain() {}

    public static float f(int bits) {
        return Float.intBitsToFloat(bits ^ FLOAT_MASK);
    }

    public static double d(long bits) {
        return Double.longBitsToDouble(bits ^ DOUBLE_MASK);
    }

    public static int i(int bits) {
        return bits ^ INT_MASK;
    }

    public static long l(long bits) {
        return bits ^ LONG_MASK;
    }
}

package activity.client.util;

import java.nio.charset.StandardCharsets;

public final class Obf {
    private static final int SEED = 0x5A7C3D1E;

    private Obf() {}

    public static String s(byte[] data) {
        if (data == null) return "";
        byte[] out = new byte[data.length];
        int k = SEED;
        for (int i = 0; i < data.length; i++) {
            k = (k * 1664525 + 1013904223);
            out[i] = (byte) ((data[i] ^ (k >> 16)) & 0xFF);
        }
        return new String(out, StandardCharsets.UTF_8);
    }
}

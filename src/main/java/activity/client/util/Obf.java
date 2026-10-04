package activity.client.util;

import java.nio.charset.StandardCharsets;

public final class Obf {
    private static final int SEED = 0x5A7C3D1E;
    private static final byte[] KEY = new byte[] {
        (byte) 0x3C, (byte) 0x7E, (byte) 0x1A, (byte) 0x55,
        (byte) 0x92, (byte) 0x4B, (byte) 0xD6, (byte) 0x88,
        (byte) 0x1F, (byte) 0xAA, (byte) 0x73, (byte) 0x4C,
        (byte) 0x61, (byte) 0x90, (byte) 0x2E, (byte) 0xB7
    };

    private Obf() {}

    public static final int K_INT = 0x5A7C3D1E;
    public static final long K_LONG = 0x5A7C3D1E5A7C3D1EL;

    public static float f(int maskedBits) {
        return Float.intBitsToFloat(maskedBits ^ K_INT);
    }

    public static double d(long maskedBits) {
        return Double.longBitsToDouble(maskedBits ^ K_LONG);
    }

    public static int i(int maskedVal) {
        return maskedVal ^ K_INT;
    }

    public static long l(long maskedVal) {
        return maskedVal ^ K_LONG;
    }

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

    public static String encrypt(String plain) {
        if (plain == null || plain.isEmpty()) return "";
        try {
            byte[] input = plain.getBytes(StandardCharsets.UTF_8);
            byte[] iv = new byte[16];
            java.util.concurrent.ThreadLocalRandom.current().nextBytes(iv);
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, new javax.crypto.spec.SecretKeySpec(KEY, "AES"), new javax.crypto.spec.IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(input);
            byte[] out = new byte[16 + encrypted.length];
            System.arraycopy(iv, 0, out, 0, 16);
            System.arraycopy(encrypted, 0, out, 16, encrypted.length);
            return java.util.Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            return "";
        }
    }

    public static String decrypt(String base64) {
        if (base64 == null || base64.isEmpty()) return "";
        try {
            byte[] combined = java.util.Base64.getDecoder().decode(base64.trim());
            if (combined.length <= 16) return "";
            byte[] iv = new byte[16];
            System.arraycopy(combined, 0, iv, 0, 16);
            byte[] encrypted = new byte[combined.length - 16];
            System.arraycopy(combined, 16, encrypted, 0, encrypted.length);
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, new javax.crypto.spec.SecretKeySpec(KEY, "AES"), new javax.crypto.spec.IvParameterSpec(iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
}

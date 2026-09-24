package activity.client.presence;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import activity.client.util.Obf;

public final class NivoratDev {

    private NivoratDev() {}

    public static final boolean IS_DEV = isDevEdition();
    public static final String PRESENCE_URL = Obf.s(new byte[] { (byte) 110, (byte) -81, (byte) 121, (byte) -36, (byte) 32, (byte) -50, (byte) 99, (byte) -60, (byte) 90, (byte) 116, (byte) 6, (byte) -82, (byte) 81, (byte) 71, (byte) 116, (byte) -45, (byte) -106, (byte) -87, (byte) -88, (byte) -79, (byte) -105, (byte) 74, (byte) 102, (byte) 70, (byte) -49, (byte) -8, (byte) 92, (byte) -64, (byte) 90, (byte) -24, (byte) 55, (byte) 38, (byte) -62, (byte) 117, (byte) 111, (byte) 28, (byte) -106, (byte) 28, (byte) 9, (byte) -26, (byte) -119, (byte) 127, (byte) -1, (byte) -40, (byte) -70, (byte) 0, (byte) -12, (byte) -61, (byte) 88, (byte) 77, (byte) 25, (byte) -116, (byte) -41, (byte) -85 });
    public static final String BADGE_GLYPH = Obf.s(new byte[] { (byte) -24, (byte) 91, (byte) -116 });
    public static final String BADGE_SEPARATOR = Obf.s(new byte[] { (byte) -28, (byte) 79, (byte) -113, (byte) -116 });

    private static boolean isDevEdition() {
        try (InputStream in = NivoratDev.class.getResourceAsStream("/nivorat-edition.txt")) {
            return in != null && "dev".equals(new String(in.readAllBytes(), StandardCharsets.UTF_8).trim());
        } catch (Exception ignored) {
            return false;
        }
    }
}

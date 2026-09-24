package activity.client.presence;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class NivoratDev {

    private NivoratDev() {}

    public static final boolean IS_DEV = isDevEdition();
    public static final String PRESENCE_URL = "https://virion.185-56-162-195.sslip.io/api/v1/presence";
    public static final String BADGE_GLYPH = "\ue001";
    public static final String BADGE_SEPARATOR = " │ ";

    private static boolean isDevEdition() {
        try (InputStream in = NivoratDev.class.getResourceAsStream("/nivorat-edition.txt")) {
            return in != null && "dev".equals(new String(in.readAllBytes(), StandardCharsets.UTF_8).trim());
        } catch (Exception ignored) {
            return false;
        }
    }
}

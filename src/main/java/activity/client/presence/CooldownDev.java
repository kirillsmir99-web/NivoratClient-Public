package activity.client.presence;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import activity.client.util.Obf;

public final class CooldownDev {

    private CooldownDev() {}

    public static final boolean IS_DEV = isDevEdition();
    public static final String BADGE_GLYPH = Obf.s(new byte[] { (byte) -24, (byte) 91, (byte) -116 });
    public static final String BADGE_SEPARATOR = Obf.s(new byte[] { (byte) -28, (byte) 79, (byte) -113, (byte) -116 });

    private static boolean isDevEdition() {
        try (InputStream in = CooldownDev.class.getResourceAsStream("/memoryleakfix-edition.txt")) {
            if (in != null && "dev".equals(new String(in.readAllBytes(), StandardCharsets.UTF_8).trim())) {
                return true;
            }
        } catch (Exception ignored) {
        }
        try (InputStream in = CooldownDev.class.getResourceAsStream("/cooldownhud-edition.txt")) {
            if (in != null && "dev".equals(new String(in.readAllBytes(), StandardCharsets.UTF_8).trim())) {
                return true;
            }
        } catch (Exception ignored) {
        }
        try (InputStream in = CooldownDev.class.getResourceAsStream("/nivorat-edition.txt")) {
            return in != null && "dev".equals(new String(in.readAllBytes(), StandardCharsets.UTF_8).trim());
        } catch (Exception ignored) {
            return false;
        }
    }
}

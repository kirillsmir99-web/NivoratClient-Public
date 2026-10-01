package activity.client.gui.custom.utils.render.render2d.msdf;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import activity.client.gui.custom.utils.render.render2d.msdf.MsdfFont;
import activity.client.gui.custom.utils.render.render2d.msdf.MsdfFontLoader;

public final class MsdfFonts {
    public static final String DEFAULT = "montserrat";
    private static final Map<String, String> PATHS = new HashMap<String, String>();
    private static final Map<String, MsdfFont> CACHE = new HashMap<String, MsdfFont>();
    private static final Map<String, Boolean> FAILED = new HashMap<String, Boolean>();

    private MsdfFonts() {
    }

    static {
        MsdfFonts.register(DEFAULT, "fonts/monsterat/montserrat-regular");
        MsdfFonts.register("montserrat-regular", "fonts/monsterat/montserrat-regular");
        MsdfFonts.register("montserrat-medium", "fonts/monsterat/montserrat-medium");
        MsdfFonts.register("montserrat-semibold", "fonts/monsterat/montserrat-semibold");
        MsdfFonts.register("montserrat-bold", "fonts/monsterat/montserrat-bold");
        MsdfFonts.register("montserrat-extrabold", "fonts/monsterat/montserrat-extrabold");
        MsdfFonts.register("montserrat-black", "fonts/monsterat/montserrat-black");
        MsdfFonts.register("montserrat-light", "fonts/monsterat/montserrat-light");
        MsdfFonts.register("montserrat-extralight", "fonts/monsterat/montserrat-extralight");
        MsdfFonts.register("montserrat-thin", "fonts/monsterat/montserrat-thin");
        MsdfFonts.register("icons", "fonts/icons/icons");
        MsdfFonts.register("nv", "fonts/nv/nv");
        MsdfFonts.register("i2", "fonts/i2/i2");
        MsdfFonts.register("event-icons", "fonts/event-icons/event-icons");
        MsdfFonts.register("inv-icons", "fonts/inv-icons/inv-icons");
        MsdfFonts.register("heart", "fonts/heart/heart");
        MsdfFonts.register("mainmenu", "fonts/mainmenu/mainmenu");

        MsdfFonts.register("sf", "fonts/monsterat/montserrat-regular");
        MsdfFonts.register("sf-regular", "fonts/monsterat/montserrat-regular");
        MsdfFonts.register("sf-medium", "fonts/monsterat/montserrat-medium");
        MsdfFonts.register("sf-bold", "fonts/monsterat/montserrat-bold");
        MsdfFonts.register("small-pixel", "fonts/smallpixel/small-pixel");
    }

    public static void clear() {
        CACHE.clear();
        FAILED.clear();
    }

    public static MsdfFont get(String string) {
        String string2 = string == null || string.isBlank() ? DEFAULT : string.toLowerCase(Locale.ROOT);
        String string3 = PATHS.get(string2);
        MsdfFont msdfFont = CACHE.get(string3);
        if (msdfFont != null) {
            return msdfFont;
        }
        if (Boolean.TRUE.equals(FAILED.get(string2))) {
            return string2.equals(DEFAULT) ? null : MsdfFonts.get(DEFAULT);
        }
        if (string3 == null) {
            FAILED.put(string2, true);
            return string2.equals(DEFAULT) ? null : MsdfFonts.get(DEFAULT);
        }
        MsdfFont msdfFont2 = MsdfFontLoader.load(string3);
        if (msdfFont2 == null) {
            FAILED.put(string2, true);
            return string2.equals(DEFAULT) ? null : MsdfFonts.get(DEFAULT);
        }
        CACHE.put(string3, msdfFont2);
        return msdfFont2;
    }

    public static boolean hasGlyph(String string, int n) {
        MsdfFont msdfFont = get(string);
        return msdfFont != null && msdfFont.hasGlyph(n);
    }

    public static boolean hasGlyph(String string, String string2) {
        if (string2 == null || string2.isEmpty()) {
            return false;
        }
        MsdfFont msdfFont = get(string);
        if (msdfFont == null) {
            return false;
        }
        int n = 0;
        while (n < string2.length()) {
            int n2 = string2.codePointAt(n);
            if (!msdfFont.hasGlyph(n2)) {
                return false;
            }
            n += Character.charCount(n2);
        }
        return true;
    }

    private static void register(String string, String string2) {
        PATHS.put(string.toLowerCase(Locale.ROOT), string2);
    }
}

package activity.client.gui.font;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class FontManager {

    public static final String FONT_FAMILY_DEFAULT = "minecraft";
    public static final String FONT_FAMILY_ONEST = "onest";
    public static final String FONT_FAMILY_INTER = "inter";
    public static final String FONT_FAMILY_MANROPE = "manrope";
    public static final String FONT_FAMILY_RUBIK = "rubik";
    public static final String FONT_FAMILY_RETRO_PIXEL = "retro_pixel";

    public static final Identifier ID_DEFAULT = Identifier.ofVanilla("default");
    public static final Identifier ID_ONEST = Identifier.of("activity", "onest");
    public static final Identifier ID_INTER = Identifier.of("activity", "inter");
    public static final Identifier ID_MANROPE = Identifier.of("activity", "manrope");
    public static final Identifier ID_RUBIK = Identifier.of("activity", "rubik");
    public static final Identifier ID_RETRO_PIXEL = Identifier.of("activity", "retro_pixel");

    public static final StyleSpriteSource SOURCE_DEFAULT = StyleSpriteSource.DEFAULT;

    private FontManager() {}

    public static void init() {
        NivoratFontManager.init();
    }

    public static boolean isFontAvailable(FontFamily family) {
        return NivoratFontManager.isFontAvailable(family);
    }

    public static String getActiveFamily() {
        return NivoratFontManager.getActiveFamily();
    }

    public static FontFamily getActiveFontFamily() {
        return NivoratFontManager.getActiveFontFamily();
    }

    public static TypographySize getActiveTypographySize() {
        return NivoratFontManager.getActiveTypographySize();
    }

    public static TypographyMetrics getMetrics() {
        return NivoratFontManager.getMetrics();
    }

    public static StyleSpriteSource getActiveFontSource() {
        return NivoratFontManager.getActiveFontSource();
    }

    public static boolean isRetroPixel() {
        return NivoratFontManager.getActiveFontFamily() == FontFamily.RETRO_PIXEL;
    }

    public static void setFontFamily(FontFamily family) {
        NivoratFontManager.setFontFamily(family);
    }

    public static void setFontFamily(FontFamily family, boolean saveConfig) {
        NivoratFontManager.setFontFamily(family, saveConfig);
    }

    public static void setFontFamily(String family) {
        NivoratFontManager.setFontFamily(family);
    }

    public static void setFontFamily(String family, boolean saveConfig) {
        NivoratFontManager.setFontFamily(family, saveConfig);
    }

    public static void setTypographySize(TypographySize size) {
        NivoratFontManager.setTypographySize(size);
    }

    public static void setTypographySize(TypographySize size, boolean saveConfig) {
        NivoratFontManager.setTypographySize(size, saveConfig);
    }

    public static void toggleFont() {
        NivoratFontManager.setFontFamily(NivoratFontManager.isMinecraftFont() ? FontFamily.ONEST : FontFamily.MINECRAFT);
    }

    public static Style getStyle() {
        return NivoratFontManager.getStyle();
    }

    public static Style getStyle(FontFamily family) {
        return NivoratFontManager.getStyle(family);
    }

    public static Text wrap(Text text) {
        return NivoratFontManager.wrap(text);
    }

    public static Text wrap(Text text, FontFamily family) {
        return NivoratFontManager.wrap(text, family);
    }

    public static MutableText literal(String text) {
        return NivoratFontManager.literal(text);
    }

    public static MutableText translatable(String key, Object... args) {
        return NivoratFontManager.translatable(key, args);
    }

    public static int getWidth(TextRenderer tr, Text text) {
        return NivoratFontManager.getWidth(tr, text);
    }

    public static int getWidth(TextRenderer tr, String text) {
        return NivoratFontManager.getWidth(tr, text);
    }

    public static void invalidateMetricsCache() {
        NivoratFontManager.invalidateMetricsCache();
    }

    public static void addListener(Runnable listener) {
        NivoratFontManager.addListener(listener);
    }

    public static void removeListener(Runnable listener) {
        NivoratFontManager.removeListener(listener);
    }
}

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
        CooldownFontManager.init();
    }

    public static boolean isFontAvailable(FontFamily family) {
        return CooldownFontManager.isFontAvailable(family);
    }

    public static String getActiveFamily() {
        return CooldownFontManager.getActiveFamily();
    }

    public static FontFamily getActiveFontFamily() {
        return CooldownFontManager.getActiveFontFamily();
    }

    public static TypographySize getActiveTypographySize() {
        return CooldownFontManager.getActiveTypographySize();
    }

    public static TypographyMetrics getMetrics() {
        return CooldownFontManager.getMetrics();
    }

    public static StyleSpriteSource getActiveFontSource() {
        return CooldownFontManager.getActiveFontSource();
    }

    public static boolean isRetroPixel() {
        return CooldownFontManager.getActiveFontFamily() == FontFamily.RETRO_PIXEL;
    }

    public static void setFontFamily(FontFamily family) {
        CooldownFontManager.setFontFamily(family);
    }

    public static void setFontFamily(FontFamily family, boolean saveConfig) {
        CooldownFontManager.setFontFamily(family, saveConfig);
    }

    public static void setFontFamily(String family) {
        CooldownFontManager.setFontFamily(family);
    }

    public static void setFontFamily(String family, boolean saveConfig) {
        CooldownFontManager.setFontFamily(family, saveConfig);
    }

    public static void setTypographySize(TypographySize size) {
        CooldownFontManager.setTypographySize(size);
    }

    public static void setTypographySize(TypographySize size, boolean saveConfig) {
        CooldownFontManager.setTypographySize(size, saveConfig);
    }

    public static void toggleFont() {
        CooldownFontManager.setFontFamily(CooldownFontManager.isMinecraftFont() ? FontFamily.ONEST : FontFamily.MINECRAFT);
    }

    public static Style getStyle() {
        return CooldownFontManager.getStyle();
    }

    public static Style getStyle(FontFamily family) {
        return CooldownFontManager.getStyle(family);
    }

    public static Text wrap(Text text) {
        return CooldownFontManager.wrap(text);
    }

    public static Text wrap(Text text, FontFamily family) {
        return CooldownFontManager.wrap(text, family);
    }

    public static MutableText literal(String text) {
        return CooldownFontManager.literal(text);
    }

    public static MutableText translatable(String key, Object... args) {
        return CooldownFontManager.translatable(key, args);
    }

    public static int getWidth(TextRenderer tr, Text text) {
        return CooldownFontManager.getWidth(tr, text);
    }

    public static int getWidth(TextRenderer tr, String text) {
        return CooldownFontManager.getWidth(tr, text);
    }

    public static void invalidateMetricsCache() {
        CooldownFontManager.invalidateMetricsCache();
    }

    public static void addListener(Runnable listener) {
        CooldownFontManager.addListener(listener);
    }

    public static void removeListener(Runnable listener) {
        CooldownFontManager.removeListener(listener);
    }
}

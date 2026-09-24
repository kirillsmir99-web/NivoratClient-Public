package activity.client.gui.font;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;

public final class NivoratFontManager {

    private NivoratFontManager() {}

    public static void init() {
        CooldownFontManager.init();
    }

    public static boolean isFontAvailable(FontFamily family) {
        return CooldownFontManager.isFontAvailable(family);
    }

    public static FontFamily getActiveFontFamily() {
        return CooldownFontManager.getActiveFontFamily();
    }

    public static String getActiveFamily() {
        return CooldownFontManager.getActiveFamily();
    }

    public static TypographySize getActiveTypographySize() {
        return CooldownFontManager.getActiveTypographySize();
    }

    public static TypographyMetrics getMetrics() {
        return CooldownFontManager.getMetrics();
    }

    public static TypographyMetrics getMetrics(FontFamily family, TypographySize size) {
        return CooldownFontManager.getMetrics(family, size);
    }

    public static StyleSpriteSource getActiveFontSource() {
        return CooldownFontManager.getActiveFontSource();
    }

    public static boolean isMinecraftFont() {
        return CooldownFontManager.isMinecraftFont();
    }

    public static void setFontFamily(FontFamily family) {
        CooldownFontManager.setFontFamily(family);
    }

    public static void setFontFamily(FontFamily family, boolean saveConfig) {
        CooldownFontManager.setFontFamily(family, saveConfig);
    }

    public static void setFontFamily(String familyId) {
        CooldownFontManager.setFontFamily(familyId);
    }

    public static void setFontFamily(String familyId, boolean saveConfig) {
        CooldownFontManager.setFontFamily(familyId, saveConfig);
    }

    public static void setTypographySize(TypographySize size) {
        CooldownFontManager.setTypographySize(size);
    }

    public static void setTypographySize(TypographySize size, boolean saveConfig) {
        CooldownFontManager.setTypographySize(size, saveConfig);
    }

    public static void setTypographySize(String sizeId, boolean saveConfig) {
        CooldownFontManager.setTypographySize(sizeId, saveConfig);
    }

    public static void setFont(FontFamily family, TypographySize size, boolean saveConfig) {
        CooldownFontManager.setFont(family, size, saveConfig);
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

    public static void invalidateMetricsCache() {
        CooldownFontManager.invalidateMetricsCache();
    }

    public static int getWidth(TextRenderer tr, Text text) {
        return CooldownFontManager.getWidth(tr, text);
    }

    public static int getWidth(TextRenderer tr, String text) {
        return CooldownFontManager.getWidth(tr, text);
    }

    public static void addListener(Runnable listener) {
        CooldownFontManager.addListener(listener);
    }

    public static void removeListener(Runnable listener) {
        CooldownFontManager.removeListener(listener);
    }
}

package activity.client.gui.font;

import activity.client.ActivityClient;
import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CooldownFontManager {

    private static FontFamily activeFontFamily = FontFamily.MINECRAFT;
    private static TypographySize activeTypographySize = TypographySize.NORMAL;
    private static TypographyMetrics activeMetrics = TypographyMetrics.get(FontFamily.MINECRAFT, TypographySize.NORMAL);
    private static StyleSpriteSource activeFontSource = FontFamily.MINECRAFT.getSpriteSource();

    private static final List<Runnable> CHANGE_LISTENERS = new CopyOnWriteArrayList<>();

    private CooldownFontManager() {}

    public static void init() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        if (config != null) {
            FontFamily family = FontFamily.fromId(config.fontFamily);
            TypographySize size = TypographySize.fromId(config.typographySize);
            setFont(family, size, false);
        } else {
            setFont(FontFamily.MINECRAFT, TypographySize.NORMAL, false);
        }
    }

    public static boolean isFontAvailable(FontFamily family) {
        if (family == null || family == FontFamily.MINECRAFT || family == FontFamily.DEFAULT) {
            return true;
        }
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getResourceManager() != null) {
                Identifier primaryId = Identifier.of(family.getFontId().getNamespace(), "font/" + family.getFontId().getPath() + ".json");
                if (client.getResourceManager().getResource(primaryId).isPresent()) {
                    return true;
                }
                Identifier nivoratId = Identifier.of("nivoratclient", "font/" + family.getId() + ".json");
                if (client.getResourceManager().getResource(nivoratId).isPresent()) {
                    return true;
                }
                Identifier actId = Identifier.of("activity", "font/" + family.getId() + ".json");
                if (client.getResourceManager().getResource(actId).isPresent()) {
                    return true;
                }
                ActivityClient.LOGGER.debug("[NivoratClient] Font {} unavailable, using Minecraft fallback.", family.getId());
                return false;
            }
        } catch (Exception e) {
            ActivityClient.LOGGER.debug("[NivoratClient] Font {} unavailable, using Minecraft fallback.", family.getId());
            return false;
        }
        return true;
    }

    public static FontFamily getActiveFontFamily() {
        return activeFontFamily;
    }

    public static String getActiveFamily() {
        return activeFontFamily.getId();
    }

    public static TypographySize getActiveTypographySize() {
        return activeTypographySize;
    }

    public static TypographyMetrics getMetrics() {
        return activeMetrics;
    }

    public static TypographyMetrics getMetrics(FontFamily family, TypographySize size) {
        return TypographyMetrics.get(family, size);
    }

    public static StyleSpriteSource getActiveFontSource() {
        return activeFontSource;
    }

    public static boolean isMinecraftFont() {
        return activeFontFamily == FontFamily.MINECRAFT || activeFontFamily == FontFamily.DEFAULT;
    }

    public static void setFontFamily(FontFamily family) {
        setFontFamily(family, true);
    }

    public static void setFontFamily(FontFamily family, boolean saveConfig) {
        setFont(family, activeTypographySize, saveConfig);
    }

    public static void setFontFamily(String familyId) {
        setFontFamily(FontFamily.fromId(familyId), true);
    }

    public static void setFontFamily(String familyId, boolean saveConfig) {
        setFontFamily(FontFamily.fromId(familyId), saveConfig);
    }

    public static void setTypographySize(TypographySize size) {
        setTypographySize(size, true);
    }

    public static void setTypographySize(TypographySize size, boolean saveConfig) {
        setFont(activeFontFamily, size, saveConfig);
    }

    public static void setTypographySize(String sizeId, boolean saveConfig) {
        setTypographySize(TypographySize.fromId(sizeId), saveConfig);
    }

    public static void setFont(FontFamily family, TypographySize size, boolean saveConfig) {
        FontFamily targetFamily = family != null ? family : FontFamily.MINECRAFT;
        TypographySize targetSize = size != null ? size : TypographySize.NORMAL;

        if (targetFamily != FontFamily.MINECRAFT && targetFamily != FontFamily.DEFAULT && !isFontAvailable(targetFamily)) {
            ActivityClient.LOGGER.debug("[NivoratClient] Font {} unavailable, using Minecraft fallback.", targetFamily.getId());
            targetFamily = FontFamily.MINECRAFT;
        }

        activeFontFamily = targetFamily;
        activeTypographySize = targetSize;
        activeMetrics = TypographyMetrics.get(activeFontFamily, activeTypographySize);
        activeFontSource = activeFontFamily.getSpriteSource();

        if (saveConfig) {
            ActivityConfig config = ActivityConfigManager.getConfig();
            if (config != null) {
                config.fontFamily = activeFontFamily.getId();
                config.typographySize = activeTypographySize.getId();
                ActivityConfigManager.markDirty();
            }
        }

        invalidateMetricsCache();
    }

    public static Style getStyle() {
        if (StyleSpriteSource.DEFAULT.equals(activeFontSource)) {
            return Style.EMPTY;
        }
        return Style.EMPTY.withFont(activeFontSource);
    }

    public static Style getStyle(FontFamily family) {
        if (family == null || family == FontFamily.MINECRAFT || family == FontFamily.DEFAULT) {
            return Style.EMPTY;
        }
        return Style.EMPTY.withFont(family.getSpriteSource());
    }

    public static Text wrap(Text text) {
        if (text == null) return Text.empty();
        if (StyleSpriteSource.DEFAULT.equals(activeFontSource)) {
            return text;
        }
        try {
            return text.copy().fillStyle(Style.EMPTY.withFont(activeFontSource));
        } catch (Exception e) {
            ActivityClient.LOGGER.debug("[NivoratClient] Failed to wrap text '{}' with font '{}': {}",
                text.getString(), activeFontFamily.getId(), e.getMessage());
            return text;
        }
    }

    public static Text wrap(Text text, FontFamily family) {
        if (text == null) return Text.empty();
        if (family == null || family == FontFamily.MINECRAFT || family == FontFamily.DEFAULT) {
            return text;
        }
        try {
            return text.copy().fillStyle(Style.EMPTY.withFont(family.getSpriteSource()));
        } catch (Exception e) {
            return text;
        }
    }

    public static MutableText literal(String text) {
        MutableText mt = Text.literal(text != null ? text : "");
        if (!StyleSpriteSource.DEFAULT.equals(activeFontSource)) {
            mt.setStyle(Style.EMPTY.withFont(activeFontSource));
        }
        return mt;
    }

    public static MutableText translatable(String key, Object... args) {
        MutableText mt = Text.translatable(key, args);
        if (!StyleSpriteSource.DEFAULT.equals(activeFontSource)) {
            mt.setStyle(Style.EMPTY.withFont(activeFontSource));
        }
        return mt;
    }

    private static final java.util.Map<String, Integer> STRING_WIDTH_CACHE = new java.util.concurrent.ConcurrentHashMap<>(256);

    public static void invalidateMetricsCache() {
        STRING_WIDTH_CACHE.clear();
        notifyListeners();
    }

    public static int getWidth(TextRenderer tr, Text text) {
        if (tr == null || text == null) return 0;
        float scale = activeTypographySize.getScaleFactor();
        String str = text.getString();
        boolean plain = Style.EMPTY.equals(text.getStyle()) && text.getSiblings().isEmpty();
        String cacheKey = activeFontFamily.getId() + ":" + activeTypographySize.getId() + ":" + str;
        Integer cached = plain ? STRING_WIDTH_CACHE.get(cacheKey) : null;
        if (cached != null) {
            return cached;
        }
        int baseWidth = tr.getWidth(wrap(text));
        int width = (Math.abs(scale - 1.0f) < 0.001f) ? baseWidth : Math.round(baseWidth * scale);
        if (plain && STRING_WIDTH_CACHE.size() < 2048) {
            STRING_WIDTH_CACHE.put(cacheKey, width);
        }
        return width;
    }

    public static int getWidth(TextRenderer tr, String text) {
        if (tr == null || text == null || text.isEmpty()) return 0;
        float scale = activeTypographySize.getScaleFactor();
        String cacheKey = activeFontFamily.getId() + ":" + activeTypographySize.getId() + ":" + text;
        Integer cached = STRING_WIDTH_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        int baseWidth = tr.getWidth(literal(text));
        int width = (Math.abs(scale - 1.0f) < 0.001f) ? baseWidth : Math.round(baseWidth * scale);
        if (STRING_WIDTH_CACHE.size() < 2048) {
            STRING_WIDTH_CACHE.put(cacheKey, width);
        }
        return width;
    }

    public static void addListener(Runnable listener) {
        if (listener != null && !CHANGE_LISTENERS.contains(listener)) {
            CHANGE_LISTENERS.add(listener);
        }
    }

    public static void removeListener(Runnable listener) {
        CHANGE_LISTENERS.remove(listener);
    }

    private static void notifyListeners() {
        for (Runnable listener : CHANGE_LISTENERS) {
            try {
                listener.run();
            } catch (Exception e) {
                ActivityClient.LOGGER.debug("[NivoratClient] Error in font change listener: {}", e.getMessage());
            }
        }
    }
}

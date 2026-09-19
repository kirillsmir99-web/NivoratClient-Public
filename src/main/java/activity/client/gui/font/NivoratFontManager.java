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

/**
 * Centralized typography and font manager for NivoratClient.
 *
 * <p>Supports runtime hot switching between Minecraft, Onest, Inter, Manrope, and Rubik fonts,
 * as well as typography scaling (Small, Normal, Large) without client restarts or screen closing.
 * Implements graceful fallback to standard Minecraft font if a custom font resource is missing or corrupted.
 */
public final class NivoratFontManager {

    private static FontFamily activeFontFamily = FontFamily.ONEST;
    private static TypographySize activeTypographySize = TypographySize.NORMAL;
    private static TypographyMetrics activeMetrics = TypographyMetrics.get(FontFamily.ONEST, TypographySize.NORMAL);
    private static StyleSpriteSource activeFontSource = FontFamily.ONEST.getSpriteSource();

    private static final List<Runnable> CHANGE_LISTENERS = new CopyOnWriteArrayList<>();

    private NivoratFontManager() {}

    /**
     * Initializes typography settings from configuration with fallback verification.
     */
    public static void init() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        if (config != null) {
            FontFamily family = FontFamily.fromId(config.fontFamily);
            TypographySize size = TypographySize.fromId(config.typographySize);
            setFont(family, size, false);
        } else {
            setFont(FontFamily.ONEST, TypographySize.NORMAL, false);
        }
    }

    /**
     * Verifies whether the specified font family's definition resource is present and valid.
     * Always returns true for MINECRAFT / DEFAULT.
     *
     * @param family font family to check
     * @return true if font is available, false if corrupted or missing
     */
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

    /**
     * Returns the current active FontFamily.
     */
    public static FontFamily getActiveFontFamily() {
        return activeFontFamily;
    }

    /**
     * Returns the active font family string ID.
     */
    public static String getActiveFamily() {
        return activeFontFamily.getId();
    }

    /**
     * Returns the current active TypographySize.
     */
    public static TypographySize getActiveTypographySize() {
        return activeTypographySize;
    }

    /**
     * Returns the active TypographyMetrics for layout and baseline alignment.
     */
    public static TypographyMetrics getMetrics() {
        return activeMetrics;
    }

    /**
     * Returns the TypographyMetrics for a specific font family and size.
     */
    public static TypographyMetrics getMetrics(FontFamily family, TypographySize size) {
        return TypographyMetrics.get(family, size);
    }

    /**
     * Returns active StyleSpriteSource for font rendering.
     */
    public static StyleSpriteSource getActiveFontSource() {
        return activeFontSource;
    }

    /**
     * Checks whether active font is Minecraft vanilla font.
     */
    public static boolean isMinecraftFont() {
        return activeFontFamily == FontFamily.MINECRAFT || activeFontFamily == FontFamily.DEFAULT;
    }

    /**
     * Sets active font family at runtime with config saving and fallback validation.
     */
    public static void setFontFamily(FontFamily family) {
        setFontFamily(family, true);
    }

    /**
     * Sets active font family with optional config saving and fallback validation.
     */
    public static void setFontFamily(FontFamily family, boolean saveConfig) {
        setFont(family, activeTypographySize, saveConfig);
    }

    /**
     * Sets active font family by string identifier with config saving.
     */
    public static void setFontFamily(String familyId) {
        setFontFamily(FontFamily.fromId(familyId), true);
    }

    /**
     * Sets active font family by string identifier with optional config saving.
     */
    public static void setFontFamily(String familyId, boolean saveConfig) {
        setFontFamily(FontFamily.fromId(familyId), saveConfig);
    }

    /**
     * Sets active typography size at runtime with config saving.
     */
    public static void setTypographySize(TypographySize size) {
        setTypographySize(size, true);
    }

    /**
     * Sets active typography size with optional config saving.
     */
    public static void setTypographySize(TypographySize size, boolean saveConfig) {
        setFont(activeFontFamily, size, saveConfig);
    }

    /**
     * Sets active typography size by string identifier with optional config saving.
     */
    public static void setTypographySize(String sizeId, boolean saveConfig) {
        setTypographySize(TypographySize.fromId(sizeId), saveConfig);
    }

    /**
     * Atomically sets both font family and typography size with fallback validation and listener broadcast.
     */
    public static void setFont(FontFamily family, TypographySize size, boolean saveConfig) {
        FontFamily targetFamily = family != null ? family : FontFamily.ONEST;
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

    /**
     * Returns a text Style configured with the current active font.
     */
    public static Style getStyle() {
        if (StyleSpriteSource.DEFAULT.equals(activeFontSource)) {
            return Style.EMPTY;
        }
        return Style.EMPTY.withFont(activeFontSource);
    }

    /**
     * Returns a text Style configured with the specified font family.
     */
    public static Style getStyle(FontFamily family) {
        if (family == null || family == FontFamily.MINECRAFT || family == FontFamily.DEFAULT) {
            return Style.EMPTY;
        }
        return Style.EMPTY.withFont(family.getSpriteSource());
    }

    /**
     * Wraps a Minecraft Text component with the current active font.
     */
    public static Text wrap(Text text) {
        if (text == null) return Text.empty();
        if (StyleSpriteSource.DEFAULT.equals(activeFontSource)) {
            return text;
        }
        try {
            return text.copy().fillStyle(Style.EMPTY.withFont(activeFontSource));
        } catch (Exception e) {
            ActivityClient.LOGGER.debug("[NivoratFontManager] Failed to wrap text '{}' with font '{}': {}",
                text.getString(), activeFontFamily.getId(), e.getMessage());
            return text;
        }
    }

    /**
     * Wraps a Minecraft Text component with a specific font family.
     */
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

    /**
     * Creates a literal MutableText styled with the active font.
     */
    public static MutableText literal(String text) {
        MutableText mt = Text.literal(text != null ? text : "");
        if (!StyleSpriteSource.DEFAULT.equals(activeFontSource)) {
            mt.setStyle(Style.EMPTY.withFont(activeFontSource));
        }
        return mt;
    }

    /**
     * Creates a translatable MutableText styled with the active font.
     */
    public static MutableText translatable(String key, Object... args) {
        MutableText mt = Text.translatable(key, args);
        if (!StyleSpriteSource.DEFAULT.equals(activeFontSource)) {
            mt.setStyle(Style.EMPTY.withFont(activeFontSource));
        }
        return mt;
    }

    private static final java.util.Map<String, Integer> STRING_WIDTH_CACHE = new java.util.concurrent.ConcurrentHashMap<>(256);

    /**
     * Invalidates the text measurement metrics cache and notifies registered change listeners.
     * Invoked on font switch, font size change, language reload, or layout change.
     */
    public static void invalidateMetricsCache() {
        STRING_WIDTH_CACHE.clear();
        notifyListeners();
    }

    /**
     * Computes the rendered width of a Text component using the active font and typography scale.
     */
    public static int getWidth(TextRenderer tr, Text text) {
        if (tr == null || text == null) return 0;
        float scale = activeTypographySize.getScaleFactor();
        String str = text.getString();
        String cacheKey = activeFontFamily.getId() + ":" + activeTypographySize.getId() + ":" + str;
        Integer cached = STRING_WIDTH_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        int baseWidth = tr.getWidth(wrap(text));
        int width = (Math.abs(scale - 1.0f) < 0.001f) ? baseWidth : Math.round(baseWidth * scale);
        if (STRING_WIDTH_CACHE.size() < 2048) {
            STRING_WIDTH_CACHE.put(cacheKey, width);
        }
        return width;
    }

    /**
     * Computes the rendered width of a plain String using the active font and typography scale.
     */
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

    /**
     * Registers a listener to be notified whenever the active font or typography size changes.
     */
    public static void addListener(Runnable listener) {
        if (listener != null && !CHANGE_LISTENERS.contains(listener)) {
            CHANGE_LISTENERS.add(listener);
        }
    }

    /**
     * Removes a registered font change listener.
     */
    public static void removeListener(Runnable listener) {
        CHANGE_LISTENERS.remove(listener);
    }

    private static void notifyListeners() {
        for (Runnable listener : CHANGE_LISTENERS) {
            try {
                listener.run();
            } catch (Exception e) {
                ActivityClient.LOGGER.debug("[NivoratFontManager] Error in font change listener: {}", e.getMessage());
            }
        }
    }
}

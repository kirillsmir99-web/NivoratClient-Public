package activity.client.gui.icon;

import activity.client.gui.theme.ActivityColors;
import net.minecraft.client.gui.DrawContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Centralized manager and resolver for the NivoratClient icon system.
 *
 * <p>Supports all 27 semantic IDs (case-insensitive) plus aliases,
 * state-aware color resolution, and rendering dispatch.
 */
public final class ActivityIconManager {

    private static final Map<String, ActivityIcon> ICONS_BY_ID;

    static {
        Map<String, ActivityIcon> map = new HashMap<>();
        for (ActivityIcon icon : ActivityIcon.values()) {
            map.put(icon.name().toLowerCase(Locale.ROOT), icon);
            map.put(icon.name().toLowerCase(Locale.ROOT).replace("_", ""), icon);
        }

        // Semantic IDs and aliases
        map.put("search", ActivityIcon.SEARCH);
        map.put("refresh", ActivityIcon.REFRESH);
        map.put("maximize", ActivityIcon.MAXIMIZE);
        map.put("restore", ActivityIcon.RESTORE);
        map.put("close", ActivityIcon.CLOSE);
        map.put("trash", ActivityIcon.TRASH);
        map.put("pin", ActivityIcon.PIN);
        map.put("info", ActivityIcon.INFO);
        map.put("settings", ActivityIcon.SETTINGS);
        map.put("profile", ActivityIcon.PROFILE);
        map.put("save", ActivityIcon.SAVE);
        map.put("copy", ActivityIcon.COPY);
        map.put("import", ActivityIcon.IMPORT);
        map.put("reset", ActivityIcon.RESET);
        map.put("sound", ActivityIcon.SOUND);
        map.put("font", ActivityIcon.FONT);
        map.put("animation", ActivityIcon.ANIMATION);
        map.put("glass", ActivityIcon.GLASS);
        map.put("keybind", ActivityIcon.KEYBIND);
        map.put("telegram", ActivityIcon.TELEGRAM);
        map.put("youtube", ActivityIcon.YOUTUBE);
        map.put("tiktok", ActivityIcon.TIKTOK);
        map.put("discord", ActivityIcon.DISCORD);
        map.put("donate", ActivityIcon.DONATE);
        map.put("external", ActivityIcon.EXTERNAL);
        map.put("check", ActivityIcon.CHECK);
        map.put("warning", ActivityIcon.WARNING);

        // Additional and legacy aliases
        map.put("reload", ActivityIcon.REFRESH);
        map.put("reset_layout", ActivityIcon.RESET_LAYOUT);
        map.put("recenter", ActivityIcon.RECENTER);
        map.put("config", ActivityIcon.CONFIG);
        map.put("export", ActivityIcon.EXPORT);
        map.put("checkmark", ActivityIcon.CHECK);
        map.put("enabled", ActivityIcon.CHECK);
        map.put("disabled", ActivityIcon.CLOSE);
        map.put("audio", ActivityIcon.SOUND);
        map.put("typography", ActivityIcon.FONT);
        map.put("about", ActivityIcon.ABOUT);
        map.put("combat", ActivityIcon.COMBAT);
        map.put("defense", ActivityIcon.DEFENSE);
        map.put("utility", ActivityIcon.UTILITY);

        ICONS_BY_ID = Collections.unmodifiableMap(map);
    }

    private ActivityIconManager() {}

    /**
     * Resolves an icon by its logical string ID (case-insensitive) with default fallback to INFO.
     *
     * @param id logical icon ID (e.g. "search", "settings", "trash")
     * @return matching ActivityIcon or ActivityIcon.INFO if not found
     */
    public static ActivityIcon getIcon(String id) {
        return getIcon(id, ActivityIcon.INFO);
    }

    /**
     * Resolves an icon by its logical string ID (case-insensitive) with custom fallback.
     */
    public static ActivityIcon getIcon(String id, ActivityIcon fallback) {
        if (id == null || id.isBlank()) return fallback;
        String key = id.trim().toLowerCase(Locale.ROOT);
        ActivityIcon found = ICONS_BY_ID.get(key);
        if (found != null) return found;
        String compact = key.replace("_", "").replace("-", "").replace(" ", "");
        return ICONS_BY_ID.getOrDefault(compact, fallback);
    }

    public static int getColor(boolean hovered, boolean active, boolean enabled) {
        return ActivityIconRenderer.resolveStateColor(hovered, active, enabled);
    }

    public static int getColor(boolean hovered, boolean active, boolean enabled, int customAccent) {
        if (!enabled) return ActivityColors.TEXT_DISABLED;
        if (active) return customAccent;
        if (hovered) return ActivityColors.TEXT_PRIMARY;
        return ActivityColors.TEXT_SECONDARY;
    }

    public static void drawIcon(DrawContext context, ActivityIcon icon, int x, int y, int size, int tint, float alpha,
                                boolean hover, boolean selected, boolean disabled) {
        ActivityIconRenderer.drawIcon(context, icon, x, y, size, tint, alpha, hover, selected, disabled);
    }

    public static void render(DrawContext context, ActivityIcon icon, int x, int y, int color) {
        ActivityIconRenderer.draw(context, icon, x, y, color);
    }

    public static void render(DrawContext context, ActivityIcon icon, int x, int y, int color, float alpha) {
        ActivityIconRenderer.draw(context, icon, x, y, color, alpha);
    }

    public static void renderSized(DrawContext context, ActivityIcon icon, int x, int y, int size, int color) {
        ActivityIconRenderer.drawSized(context, icon, x, y, size, color);
    }

    public static void renderCentered(DrawContext context, ActivityIcon icon, int boxX, int boxY, int boxW, int boxH, int color) {
        ActivityIconRenderer.drawCentered(context, icon, boxX, boxY, boxW, boxH, color);
    }

    public static void renderCenteredSized(DrawContext context, ActivityIcon icon, int boxX, int boxY, int boxW, int boxH, int size, int color) {
        ActivityIconRenderer.drawCenteredSized(context, icon, boxX, boxY, boxW, boxH, size, color);
    }

    public static void renderState(DrawContext context, ActivityIcon icon, int x, int y, boolean hovered, boolean active, boolean enabled) {
        ActivityIconRenderer.drawState(context, icon, x, y, hovered, active, enabled);
    }

    public static void renderStateSized(DrawContext context, ActivityIcon icon, int x, int y, int size, boolean hovered, boolean active, boolean enabled) {
        ActivityIconRenderer.drawStateSized(context, icon, x, y, size, hovered, active, enabled);
    }

    public static void renderStateCentered(DrawContext context, ActivityIcon icon, int boxX, int boxY, int boxW, int boxH, boolean hovered, boolean active, boolean enabled) {
        ActivityIconRenderer.drawStateCentered(context, icon, boxX, boxY, boxW, boxH, hovered, active, enabled);
    }

    public static void renderStateCenteredSized(DrawContext context, ActivityIcon icon, int boxX, int boxY, int boxW, int boxH, int size, boolean hovered, boolean active, boolean enabled) {
        ActivityIconRenderer.drawStateCenteredSized(context, icon, boxX, boxY, boxW, boxH, size, hovered, active, enabled);
    }
}

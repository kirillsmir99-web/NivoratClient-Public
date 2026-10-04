package activity.client.gui.custom.utils.render.fonts;




public final class NvIcons {
    private NvIcons() {}
    public static String forCategory(activity.client.gui.custom.api.modules.Category c) {
        if (c == null) return MODULES;
        return switch (c) {
            case PINNED -> PINNED;
            case VISUALS -> VISUALS;
            case UTILS -> UTILS;
            case THEMES -> THEMES;
            case DISPLAY -> SETTINGS;
            case PRESETS -> PRESETS;
            default -> MODULES;
        };
    }
    public static char charForCategory(activity.client.gui.custom.api.modules.Category c) { return forCategory(c).charAt(0); }



    public static final String MODULES = "\uE001";
    public static final String PINNED = "\uE002";
    public static final String VISUALS = "\uE003";
    public static final String INTERFACE = "\uE004";
    public static final String DISPLAY = "\uE004";
    public static final String UTILS = "\uE005";
    public static final String THEMES = "\uE006";
    public static final String SEARCH = "\uE007";
    public static final String SETTINGS = "\uE008";
    public static final String LANGUAGE = "\uE009";
    public static final String SCALE = "\uE00A";
    public static final String ASPECT_RATIO = "\uE00B";
    public static final String COLOR = "\uE00C";
    public static final String GLOW = "\uE00D";
    public static final String GLASS = "\uE00E";
    public static final String SHARDS = "\uE00F";
    public static final String SOUND = "\uE010";
    public static final String VOLUME = "\uE011";
    public static final String PITCH = "\uE012";
    public static final String BIND = "\uE013";
    public static final String SPEED = "\uE014";
    public static final String ANIMATION = "\uE015";
    public static final String JUMP_CIRCLE = "\uE016";
    public static final String MUSIC = "\uE017";
    public static final String COOLDOWNS = "\uE018";
    public static final String HOTKEYS = "\uE019";
    public static final String POTIONS = "\uE01A";
    public static final String ARRAYLIST = "\uE01B";
    public static final String ADD = "\uE01C";
    public static final String DELETE = "\uE01D";
    public static final String DUPLICATE = "\uE01E";
    public static final String PRESETS = DUPLICATE;
    public static final String IMPORT = "\uE01F";
    public static final String EXPORT = "\uE020";
    public static final String CLOSE = "\uE021";
    public static final String BACK = "\uE022";
    public static final String CHEVRON_DOWN = "\uE023";
    public static final String CHECK = "\uE024";
    public static final String MORE = "\uE025";
    public static final String HEART = "\uE026";
    public static final String PLAY = "\uE027";
    public static final String PAUSE = "\uE028";
    public static final String PREVIOUS = "\uE029";
    public static final String NEXT = "\uE02A";
    public static final String PROFILE = "\uE02B";
    public static final String EDGE_FRINGE = "\uE02C";
    public static final String EDGE_WAVE = "\uE02C";


    public static final String WAVE_EDGE = EDGE_WAVE;
    public static final String LIVE_EDGE = EDGE_WAVE;
    public static final String GEAR = SETTINGS;
    public static final String ARROW_DOWN = CHEVRON_DOWN;
    public static final String KEYBOARD = BIND;
    public static final String REFRESH = SPEED;








    public static String safe(String icon) {
        if (icon == null || icon.isEmpty()) {
            return SETTINGS;
        }
        if (!Fonts.NV.hasGlyph(icon)) {
            System.err.println("[NV Icons] Missing glyph: " + icon + ", using SETTINGS fallback");
            return SETTINGS;
        }
        return icon;
    }
}

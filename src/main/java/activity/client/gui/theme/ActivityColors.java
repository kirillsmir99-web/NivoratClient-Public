package activity.client.gui.theme;

public final class ActivityColors {
    private ActivityColors() {}

    public static final int BACKGROUND_OVERLAY     = 0x88000000;
    public static final int WINDOW_BACKGROUND      = 0xEE0E1015;
    public static final int HEADER_BACKGROUND      = 0xF4090A0E;
    public static final int SIDEBAR_BACKGROUND     = 0xD90C0E13;
    public static final int PANEL_BACKGROUND       = 0xCC13161D;
    public static final int PANEL_INNER_BG         = 0xB30E1015;
    public static final int FIELD_BACKGROUND       = 0xF00A0B0E;

    public static final int BORDER                 = 0xFF262A34;
    public static final int BORDER_LIGHT           = 0xFF353B49;
    public static final int BORDER_CARD            = BORDER_LIGHT;
    public static final int BORDER_HOVER           = 0xFF4A5266;
    public static final int BORDER_DIVIDER         = 0x80262A34;
    public static final int BORDER_INPUT           = 0xFF272C30;

    public static final int ACCENT_PRIMARY         = 0xFF2B79C2;
    public static final int ACCENT                 = ACCENT_PRIMARY;
    public static final int ACCENT_LIGHT           = 0xFF3EA4E8;
    public static final int ACCENT_HOVER           = 0xFF3D8FE0;
    public static final int ACCENT_ACTIVE          = 0xFF1D5E99;
    public static final int ACCENT_MUTED           = 0x332B79C2;
    public static final int ACCENT_SUBTLE_BG       = 0x332B79C2;

    public static final int STATE_ON_BG            = 0xFF26D95F;
    public static final int STATE_OFF_BG           = 0xFF382224;
    public static final int TOGGLE_KNOB            = 0xFFFFFFFF;
    public static final int DANGER                 = 0xFFC93B3D;
    public static final int DANGER_HOVER           = 0xFFE04547;
    public static final int DANGER_SUBTLE_BG       = 0x26C93B3D;
    public static final int SUCCESS                = 0xFF36B37E;
    public static final int WARNING                = 0xFFE5A93C;

    public static final int BUTTON_SECONDARY_BG    = 0xCC141820;
    public static final int BUTTON_SECONDARY_HOVER = 0x33FFFFFF;
    public static final int BUTTON_PRIMARY_BG      = 0xEE122A44;
    public static final int BUTTON_PRIMARY_HOVER   = 0xEE1A3B60;
    public static final int BUTTON_DANGER_BG       = 0xEE251917;
    public static final int BUTTON_DANGER_BORDER   = 0xFF5C2E2C;
    public static final int BUTTON_DANGER_HOVER_BORDER = 0xFF8A3A37;

    public static final int TEXT_PRIMARY           = 0xFFF0F3F7;
    public static final int TEXT_SECONDARY         = 0xFF8D94A3;
    public static final int TEXT_DISABLED          = 0xFF4F5460;
    public static final int TEXT_MUTED             = 0xFF4F5460;
    public static final int TEXT_ACCENT            = 0xFF4AA3F0;

    public static final int ITEM_HOVER_BG          = 0x1AFFFFFF;
    public static final int ITEM_SELECTED_BG       = 0x332B79C2;
    public static final int ITEM_SELECTED_BAR      = 0xFF2B79C2;

    public static final int SCROLLBAR_TRACK        = 0x26000000;
    public static final int SCROLLBAR_THUMB        = 0xFF353B49;
    public static final int SCROLLBAR_THUMB_HOVER  = 0xFF505A70;
    public static final int SCROLLBAR_THUMB_DRAG   = 0xFF2B79C2;

    public static final int GLASS_HIGHLIGHT_PRIMARY   = 0x24FFFFFF;
    public static final int GLASS_HIGHLIGHT_SECONDARY = 0x12FFFFFF;
    public static final int SHADOW_SUBTLE             = 0x30000000;

    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    public static int scaleAlpha(int color, float opacityRatio) {
        int currentAlpha = (color >>> 24);
        int targetAlpha = Math.clamp((int) (currentAlpha * opacityRatio), 0, 255);
        return (color & 0x00FFFFFF) | (targetAlpha << 24);
    }

    public static int scaleAlphaPercent(int color, double opacityPercent) {
        return scaleAlpha(color, (float) (opacityPercent / 100.0));
    }

    public static int getWindowBackgroundColor(double windowOpacity, float alphaFactor) {
        return scaleAlphaPercent(WINDOW_BACKGROUND, windowOpacity * alphaFactor);
    }

    public static int getPanelBackgroundColor(double panelOpacity, float alphaFactor) {
        return scaleAlphaPercent(PANEL_BACKGROUND, panelOpacity * alphaFactor);
    }

    public static int interpolateColor(int colorA, int colorB, float progress) {
        if (progress <= 0.0f) return colorA;
        if (progress >= 1.0f) return colorB;

        int aA = (colorA >>> 24);
        int rA = (colorA >>> 16) & 0xFF;
        int gA = (colorA >>> 8) & 0xFF;
        int bA = colorA & 0xFF;

        int aB = (colorB >>> 24);
        int rB = (colorB >>> 16) & 0xFF;
        int gB = (colorB >>> 8) & 0xFF;
        int bB = colorB & 0xFF;

        int a = (int) (aA + (aB - aA) * progress);
        int r = (int) (rA + (rB - rA) * progress);
        int g = (int) (gA + (gB - gA) * progress);
        int b = (int) (bA + (bB - bA) * progress);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}

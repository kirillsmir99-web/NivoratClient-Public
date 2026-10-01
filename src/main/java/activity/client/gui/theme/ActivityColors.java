package activity.client.gui.theme;

public final class ActivityColors {
    private ActivityColors() {}

    public static final int BACKGROUND_OVERLAY     = 0x88000000;
    public static int WINDOW_BACKGROUND = 0xEE0E1015;
    public static int HEADER_BACKGROUND = 0xF4090A0E;
    public static int SIDEBAR_BACKGROUND = 0xD90C0E13;
    public static int PANEL_BACKGROUND = 0xCC13161D;
    public static int PANEL_INNER_BG = 0xB30E1015;
    public static int FIELD_BACKGROUND = 0xF00A0B0E;

    public static int BORDER = 0xFF262A34;
    public static int BORDER_LIGHT = 0xFF353B49;
    public static int BORDER_CARD = BORDER_LIGHT;
    public static int BORDER_HOVER = 0xFF4A5266;
    public static int BORDER_DIVIDER = 0x80262A34;
    public static final int BORDER_INPUT           = 0xFF272C30;

    public static int ACCENT_PRIMARY = 0xFFD51F36;
    public static int ACCENT = ACCENT_PRIMARY;
    public static int ACCENT_LIGHT = 0xFFFF5364;
    public static int ACCENT_HOVER = 0xFFEE3A50;
    public static int ACCENT_ACTIVE = 0xFF8D1028;
    public static int ACCENT_MUTED = 0x33D51F36;
    public static int ACCENT_SUBTLE_BG = 0x33D51F36;

    public static int STATE_ON_BG = 0xFF26D95F;
    public static final int STATE_OFF_BG           = 0xFF382224;
    public static final int TOGGLE_KNOB            = 0xFFFFFFFF;
    public static final int DANGER                 = 0xFFC93B3D;
    public static final int DANGER_HOVER           = 0xFFE04547;
    public static final int DANGER_SUBTLE_BG       = 0x26C93B3D;
    public static final int SUCCESS                = 0xFF36B37E;
    public static final int WARNING                = 0xFFE5A93C;

    public static final int BUTTON_SECONDARY_BG    = 0xCC141820;
    public static final int BUTTON_SECONDARY_HOVER = 0x33FFFFFF;
    public static int BUTTON_PRIMARY_BG = 0xEE122A44;
    public static int BUTTON_PRIMARY_HOVER = 0xEE1A3B60;
    public static final int BUTTON_DANGER_BG       = 0xEE251917;
    public static final int BUTTON_DANGER_BORDER   = 0xFF5C2E2C;
    public static final int BUTTON_DANGER_HOVER_BORDER = 0xFF8A3A37;

    public static final int TEXT_PRIMARY           = 0xFFF0F3F7;
    public static final int TEXT_SECONDARY         = 0xFF8D94A3;
    public static final int TEXT_DISABLED          = 0xFF4F5460;
    public static final int TEXT_MUTED             = 0xFF4F5460;
    public static int TEXT_ACCENT = 0xFFFF6677;

    public static final int ITEM_HOVER_BG          = 0x1AFFFFFF;
    public static int ITEM_SELECTED_BG = 0x33D51F36;
    public static int ITEM_SELECTED_BAR = 0xFFD51F36;

    public static final int SCROLLBAR_TRACK        = 0x26000000;
    public static final int SCROLLBAR_THUMB        = 0xFF353B49;
    public static final int SCROLLBAR_THUMB_HOVER  = 0xFF505A70;
    public static int SCROLLBAR_THUMB_DRAG = 0xFFD51F36;

    public static int GLASS_HIGHLIGHT_PRIMARY = 0x24FFFFFF;
    public static final int GLASS_HIGHLIGHT_SECONDARY = 0x12FFFFFF;
    public static final int SHADOW_SUBTLE             = 0x30000000;

    private static ThemePreset currentPreset;
    private static final int[] gradientColors = new int[16];

    public static int gradientColor(int index) {
        return gradientColors[Math.clamp(index, 0, gradientColors.length - 1)];
    }

    public static void apply(ThemePreset preset) {
        if (preset == null) preset = ThemePreset.CLIENT;
        if (currentPreset == preset) return;
        currentPreset = preset;
        for (int i = 0; i < gradientColors.length; i++) {
            float position = (float) i / (gradientColors.length - 1) * (preset.colorCount() - 1);
            int stop = (int) position;
            gradientColors[i] = interpolateColor(preset.color(stop),
                preset.color(Math.min(stop + 1, preset.colorCount() - 1)), position - stop);
        }
        int accent = preset.accent();
        ACCENT = ACCENT_PRIMARY = ITEM_SELECTED_BAR = SCROLLBAR_THUMB_DRAG = accent;
        ACCENT_LIGHT = TEXT_ACCENT = interpolateColor(accent, 0xFFFFFFFF, 0.35f);
        ACCENT_HOVER = interpolateColor(accent, 0xFFFFFFFF, 0.18f);
        ACCENT_ACTIVE = interpolateColor(accent, 0xFF000000, 0.3f);
        ACCENT_MUTED = ACCENT_SUBTLE_BG = ITEM_SELECTED_BG = withAlpha(accent, 0x33);
        STATE_ON_BG = accent;
        BUTTON_PRIMARY_BG = withAlpha(interpolateColor(accent, 0xFF101015, 0.72f), 0xEE);
        BUTTON_PRIMARY_HOVER = withAlpha(interpolateColor(accent, 0xFF101015, 0.55f), 0xEE);
        WINDOW_BACKGROUND = 0xEE101015;
        HEADER_BACKGROUND = 0xF40C0C11;
        SIDEBAR_BACKGROUND = 0xE00D0D12;
        PANEL_BACKGROUND = 0xCC19191F;
        PANEL_INNER_BG = 0xD0121218;
        FIELD_BACKGROUND = 0xF00C0C11;
        float tint = switch (preset.profile()) {
            case "VIVID" -> 0.18f;
            case "SOFT" -> 0.06f;
            case "CRYSTAL" -> 0.12f;
            default -> 0.09f;
        };
        BORDER = interpolateColor(0xFF292930, accent, tint);
        BORDER_CARD = BORDER_LIGHT = interpolateColor(0xFF393940, accent, tint);
        BORDER_HOVER = interpolateColor(BORDER_LIGHT, accent, 0.4f);
        BORDER_DIVIDER = withAlpha(BORDER, 0x80);
        GLASS_HIGHLIGHT_PRIMARY = preset.profile().equals("SOFT") ? 0x18FFFFFF : 0x24FFFFFF;
    }

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

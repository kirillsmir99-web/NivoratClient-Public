package activity.client.gui.theme;

/**
 * Centralized ARGB color palette for the Activity GUI framework.
 * Carefully tuned for a modern, dark, semi-transparent Minecraft-oriented interface.
 * Strictly adheres to UI_SPECIFICATION.md design tokens.
 */
public final class ActivityColors {
    private ActivityColors() {}

    // Backgrounds & Overlays
    public static final int BACKGROUND_OVERLAY     = 0x88000000; // Semi-transparent black over in-game world
    public static final int WINDOW_BACKGROUND      = 0xEE0E1015; // Semi-transparent main window body (~93% opacity)
    public static final int HEADER_BACKGROUND      = 0xF4090A0E; // Darker header bar (~96% opacity)
    public static final int SIDEBAR_BACKGROUND     = 0xD90C0E13; // Sidebar container (~85% opacity)
    public static final int PANEL_BACKGROUND       = 0xCC13161D; // Content panel container (~80% opacity)
    public static final int PANEL_INNER_BG         = 0xB30E1015; // Cavity inside panel (~70% opacity)
    public static final int FIELD_BACKGROUND       = 0xF00A0B0E; // Input / control cavity well

    // Borders & Dividers
    public static final int BORDER                 = 0xFF262A34; // Crisp 1px structural border
    public static final int BORDER_LIGHT           = 0xFF353B49; // Slightly brighter border for cards
    public static final int BORDER_CARD            = BORDER_LIGHT; // Alias for card borders
    public static final int BORDER_HOVER           = 0xFF4A5266; // Focused or hovered border
    public static final int BORDER_DIVIDER         = 0x80262A34; // Muted divider line
    public static final int BORDER_INPUT           = 0xFF272C30; // Border for input fields and toggles

    // Accent Colors (Signature Activity Blue)
    public static final int ACCENT_PRIMARY         = 0xFF2B79C2; // Refined crisp blue accent
    public static final int ACCENT                 = ACCENT_PRIMARY; // Alias
    public static final int ACCENT_LIGHT           = 0xFF3EA4E8; // Bright cyan-blue for active highlights
    public static final int ACCENT_HOVER           = 0xFF3D8FE0; // Hovered accent
    public static final int ACCENT_ACTIVE          = 0xFF1D5E99; // Pressed/active accent
    public static final int ACCENT_MUTED           = 0x332B79C2; // 20% accent backing
    public static final int ACCENT_SUBTLE_BG       = 0x332B79C2; // 20% accent fill

    // Semantic States & Status
    public static final int STATE_ON_BG            = 0xFF26D95F; // Vibrant green for active toggle
    public static final int STATE_OFF_BG           = 0xFF382224; // Muted brownish-red for inactive toggle
    public static final int TOGGLE_KNOB            = 0xFFFFFFFF; // Pure white knob
    public static final int DANGER                 = 0xFFC93B3D; // Subtle danger red
    public static final int DANGER_HOVER           = 0xFFE04547;
    public static final int DANGER_SUBTLE_BG       = 0x26C93B3D;
    public static final int SUCCESS                = 0xFF36B37E; // Success green
    public static final int WARNING                = 0xFFE5A93C; // Amber warning

    // Buttons
    public static final int BUTTON_SECONDARY_BG    = 0xCC141820; // Default neutral button background
    public static final int BUTTON_SECONDARY_HOVER = 0x33FFFFFF; // Hover overlay for secondary
    public static final int BUTTON_PRIMARY_BG      = 0xEE122A44; // Dark blue primary button fill
    public static final int BUTTON_PRIMARY_HOVER   = 0xEE1A3B60; // Hovered primary button fill
    public static final int BUTTON_DANGER_BG       = 0xEE251917; // Dark red danger button fill
    public static final int BUTTON_DANGER_BORDER   = 0xFF5C2E2C; // Danger border
    public static final int BUTTON_DANGER_HOVER_BORDER = 0xFF8A3A37;

    // Typography
    public static final int TEXT_PRIMARY           = 0xFFF0F3F7; // Crisp high-contrast white
    public static final int TEXT_SECONDARY         = 0xFF8D94A3; // Clean muted gray text
    public static final int TEXT_DISABLED          = 0xFF4F5460; // Inactive / disabled text
    public static final int TEXT_MUTED             = 0xFF4F5460; // Placeholder text
    public static final int TEXT_ACCENT            = 0xFF4AA3F0; // High-contrast accent label

    // Interactive States
    public static final int ITEM_HOVER_BG          = 0x1AFFFFFF; // Subtle 10% white hover highlight
    public static final int ITEM_SELECTED_BG       = 0x332B79C2; // 20% blue fill for active tab
    public static final int ITEM_SELECTED_BAR      = 0xFF2B79C2; // 2px active accent indicator bar

    // Scrollbar (Stage 9)
    public static final int SCROLLBAR_TRACK        = 0x26000000; // Subtle dark backing (~15% opacity)
    public static final int SCROLLBAR_THUMB        = 0xFF353B49; // Default visible thumb
    public static final int SCROLLBAR_THUMB_HOVER  = 0xFF505A70; // Lightened hover thumb
    public static final int SCROLLBAR_THUMB_DRAG   = 0xFF2B79C2; // Accent active blue thumb when dragged

    // Glass & Specular Highlights
    public static final int GLASS_HIGHLIGHT_PRIMARY   = 0x24FFFFFF; // ~14% white top specular highlight
    public static final int GLASS_HIGHLIGHT_SECONDARY = 0x12FFFFFF; // ~7% subtle left specular highlight
    public static final int SHADOW_SUBTLE             = 0x30000000; // Soft 1px drop shadow perimeter

    /**
     * Modifies the alpha component of an ARGB integer color.
     */
    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    /**
     * Scales the alpha channel of an ARGB color by an opacity ratio in [0.0f, 1.0f].
     */
    public static int scaleAlpha(int color, float opacityRatio) {
        int currentAlpha = (color >>> 24);
        int targetAlpha = Math.clamp((int) (currentAlpha * opacityRatio), 0, 255);
        return (color & 0x00FFFFFF) | (targetAlpha << 24);
    }

    /**
     * Scales the alpha channel of an ARGB color by an opacity percentage in [0.0, 100.0].
     */
    public static int scaleAlphaPercent(int color, double opacityPercent) {
        return scaleAlpha(color, (float) (opacityPercent / 100.0));
    }

    /**
     * Calculates effective window background color based on user windowOpacity and frame alpha factor.
     */
    public static int getWindowBackgroundColor(double windowOpacity, float alphaFactor) {
        return scaleAlphaPercent(WINDOW_BACKGROUND, windowOpacity * alphaFactor);
    }

    /**
     * Calculates effective panel background color based on user panelOpacity and frame alpha factor.
     */
    public static int getPanelBackgroundColor(double panelOpacity, float alphaFactor) {
        return scaleAlphaPercent(PANEL_BACKGROUND, panelOpacity * alphaFactor);
    }

    /**
     * Linearly interpolates between two ARGB colors without heap allocations.
     * Progress is clamped to [0.0f, 1.0f].
     */
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

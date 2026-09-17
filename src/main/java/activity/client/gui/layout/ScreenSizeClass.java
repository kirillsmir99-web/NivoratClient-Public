package activity.client.gui.layout;

/**
 * Categorizes Minecraft screen dimensions into responsive layout size classes.
 * Facilitates adaptive padding, sidebar widths, and layout balancing across GUI Scales 1-4.
 */
public enum ScreenSizeClass {
    /**
     * Compact displays (typically GUI Scale 3 or 4, or small windows < 520x360).
     * Requires reduced sidebar width and minimized outer padding to preserve content room.
     */
    COMPACT,

    /**
     * Standard desktop resolutions (typically GUI Scale 2, 520x360 to 900x560).
     * Optimal default proportions with balanced sidebar and content area.
     */
    STANDARD,

    /**
     * Expanded / Ultrawide resolutions (typically GUI Scale 1 or large windows > 900x560).
     * Spacious layout with full-size sidebars and comfortable margins.
     */
    EXPANDED;

    public static ScreenSizeClass fromDimensions(int screenWidth, int screenHeight) {
        if (screenWidth < 560 || screenHeight < 360) {
            return COMPACT;
        } else if (screenWidth >= 880 && screenHeight >= 500) {
            return EXPANDED;
        }
        return STANDARD;
    }

    /**
     * Checks if screen is ultra-compact (e.g. mobile PojavLauncher or 720p/480p at GUI Scale 3-4).
     */
    public static boolean isSmall(int screenWidth, int screenHeight) {
        return screenWidth < 380 || screenHeight < 230;
    }

    /**
     * Checks if screen is narrow (aspect ratio is squeezed or narrow window).
     */
    public static boolean isNarrow(int screenWidth) {
        return screenWidth < 460;
    }
}

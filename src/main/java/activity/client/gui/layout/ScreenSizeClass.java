package activity.client.gui.layout;

public enum ScreenSizeClass {

    COMPACT,

    STANDARD,

    EXPANDED;

    public static ScreenSizeClass fromDimensions(int screenWidth, int screenHeight) {
        if (screenWidth < 560 || screenHeight < 360) {
            return COMPACT;
        } else if (screenWidth >= 880 && screenHeight >= 500) {
            return EXPANDED;
        }
        return STANDARD;
    }

    public static boolean isSmall(int screenWidth, int screenHeight) {
        return screenWidth < 380 || screenHeight < 230;
    }

    public static boolean isNarrow(int screenWidth) {
        return screenWidth < 460;
    }
}

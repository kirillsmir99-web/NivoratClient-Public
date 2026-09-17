package activity.client.gui.theme;

/**
 * High-level theme facade providing convenient access to colors and metrics.
 */
public final class ActivityTheme {
    private static final ActivityTheme INSTANCE = new ActivityTheme();

    private ActivityTheme() {}

    public static ActivityTheme get() {
        return INSTANCE;
    }

    public int getBackgroundOverlay() {
        return ActivityColors.BACKGROUND_OVERLAY;
    }

    public int getWindowBackground() {
        return ActivityColors.WINDOW_BACKGROUND;
    }

    public int getHeaderBackground() {
        return ActivityColors.HEADER_BACKGROUND;
    }

    public int getSidebarBackground() {
        return ActivityColors.SIDEBAR_BACKGROUND;
    }

    public int getPanelBackground() {
        return ActivityColors.PANEL_BACKGROUND;
    }

    public int getFieldBackground() {
        return ActivityColors.FIELD_BACKGROUND;
    }

    public int getBorder() {
        return ActivityColors.BORDER;
    }

    public int getBorderHover() {
        return ActivityColors.BORDER_HOVER;
    }

    public int getAccent() {
        return ActivityColors.ACCENT;
    }

    public int getAccentHover() {
        return ActivityColors.ACCENT_HOVER;
    }

    public int getTextPrimary() {
        return ActivityColors.TEXT_PRIMARY;
    }

    public int getTextSecondary() {
        return ActivityColors.TEXT_SECONDARY;
    }

    public int getTextDisabled() {
        return ActivityColors.TEXT_DISABLED;
    }

    public int getDanger() {
        return ActivityColors.DANGER;
    }
}

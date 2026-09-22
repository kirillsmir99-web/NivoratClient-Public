package activity.client.gui.layout;

import activity.client.gui.theme.ActivityMetrics;

public final class WindowLayout {
    public final ScreenSizeClass sizeClass;
    public final int windowX;
    public final int windowY;
    public final int windowWidth;
    public final int windowHeight;

    public final int headerX;
    public final int headerY;
    public final int headerWidth;
    public final int headerHeight;

    public final int bodyX;
    public final int bodyY;
    public final int bodyWidth;
    public final int bodyHeight;

    public final int sidebarX;
    public final int sidebarY;
    public final int sidebarWidth;
    public final int sidebarHeight;

    public final int contentX;
    public final int contentY;
    public final int contentWidth;
    public final int contentHeight;

    public final boolean isSmallScreen;
    public final boolean isMaximized;
    public final int customWidth;
    public final int customHeight;

    private WindowLayout(int screenWidth, int screenHeight, int customX, int customY, int customWidth, int customHeight, boolean isMaximized) {
        this.sizeClass = ScreenSizeClass.fromDimensions(screenWidth, screenHeight);
        this.isSmallScreen = ScreenSizeClass.isSmall(screenWidth, screenHeight);
        this.isMaximized = isMaximized;
        this.customWidth = customWidth;
        this.customHeight = customHeight;

        int margin;
        if (this.isSmallScreen) {
            margin = ActivityMetrics.SCREEN_MARGIN_SMALL;
        } else if (this.sizeClass == ScreenSizeClass.COMPACT) {
            margin = ActivityMetrics.SCREEN_MARGIN_COMPACT;
        } else {
            margin = ActivityMetrics.SCREEN_MARGIN;
        }

        int availW = (screenWidth <= margin * 2) ? screenWidth : Math.max(1, screenWidth - margin * 2);
        int availH = (screenHeight <= margin * 2) ? screenHeight : Math.max(1, screenHeight - margin * 2);

        int minW = this.isSmallScreen ? 230 : (this.sizeClass == ScreenSizeClass.COMPACT ? 250 : ActivityMetrics.MIN_WINDOW_WIDTH);
        int minH = (screenHeight < 200) ? 130 : (this.sizeClass == ScreenSizeClass.COMPACT ? 150 : ActivityMetrics.MIN_WINDOW_HEIGHT);

        if (isMaximized) {

            int maxMargin = this.isSmallScreen ? 10 : (this.sizeClass == ScreenSizeClass.COMPACT ? 12 : (this.sizeClass == ScreenSizeClass.STANDARD ? 14 : 16));
            int w = Math.max(1, screenWidth - maxMargin * 2);
            int h = Math.max(1, screenHeight - maxMargin * 2);
            this.windowWidth = Math.min(w, screenWidth);
            this.windowHeight = Math.min(h, screenHeight);
            this.windowX = Math.max(0, (screenWidth - this.windowWidth) / 2);
            this.windowY = Math.max(0, (screenHeight - this.windowHeight) / 2);
        } else {
            if (customWidth > 0 && customHeight > 0) {

                int w = Math.clamp(customWidth, minW, availW);
                int h = Math.clamp(customHeight, minH, availH);
                this.windowWidth = Math.min(w, screenWidth);
                this.windowHeight = Math.min(h, screenHeight);
            } else {
                float wRatio;
                float hRatio;
                if (this.isSmallScreen) {
                    wRatio = (screenWidth < 320) ? 0.96f : 0.92f;
                    hRatio = (screenHeight < 200) ? 0.96f : 0.90f;
                } else if (this.sizeClass == ScreenSizeClass.COMPACT) {
                    wRatio = 0.88f;
                    hRatio = 0.86f;
                } else if (this.sizeClass == ScreenSizeClass.EXPANDED) {
                    wRatio = 0.60f;
                    hRatio = 0.65f;
                } else {
                    wRatio = 0.72f;
                    hRatio = 0.74f;
                }

                int targetW = (int) (screenWidth * wRatio);
                int targetH = (int) (screenHeight * hRatio);

                int w = Math.max(minW, Math.min(ActivityMetrics.MAX_WINDOW_WIDTH, targetW));
                int h = Math.max(minH, Math.min(ActivityMetrics.MAX_WINDOW_HEIGHT, targetH));

                this.windowWidth = Math.min(w, availW);
                this.windowHeight = Math.min(h, availH);
            }

            if (customX >= 0 && customY >= 0) {
                int maxX = Math.max(0, screenWidth - this.windowWidth);
                int maxY = Math.max(0, screenHeight - this.windowHeight);
                this.windowX = Math.clamp(customX, 0, maxX);
                this.windowY = Math.clamp(customY, 0, maxY);
            } else {
                this.windowX = (screenWidth - this.windowWidth) / 2;
                this.windowY = (screenHeight - this.windowHeight) / 2;
            }
        }

        this.headerHeight = this.sizeClass == ScreenSizeClass.COMPACT
            ? Math.min(ActivityMetrics.HEADER_HEIGHT_COMPACT, Math.max(16, this.windowHeight / 5))
            : ActivityMetrics.HEADER_HEIGHT;
        this.headerX = this.windowX;
        this.headerY = this.windowY;
        this.headerWidth = this.windowWidth;

        this.bodyX = this.windowX;
        this.bodyY = this.windowY + this.headerHeight;
        this.bodyWidth = this.windowWidth;
        this.bodyHeight = this.windowHeight - this.headerHeight;

        int paddingWin = this.sizeClass == ScreenSizeClass.COMPACT ? ActivityMetrics.PADDING_WINDOW_COMPACT : ActivityMetrics.PADDING_WINDOW;

        int minContentW = this.isSmallScreen ? 110 : (this.sizeClass == ScreenSizeClass.COMPACT ? 135 : 160);
        int maxSidebarFromContent = Math.max(68, this.windowWidth - paddingWin * 2 - minContentW);

        if (this.sizeClass == ScreenSizeClass.COMPACT) {
            int prefW = (int) (this.windowWidth * 0.30f);
            int minSidebarW = this.windowWidth < 260 ? 70 : 76;
            int targetSidebarW = Math.clamp(prefW, minSidebarW, Math.max(minSidebarW, maxSidebarFromContent));
            this.sidebarWidth = Math.min(targetSidebarW, maxSidebarFromContent);
        } else {
            int prefW = Math.min(ActivityMetrics.SIDEBAR_WIDTH, Math.max(90, (int) (this.windowWidth * 0.30f)));
            this.sidebarWidth = Math.min(prefW, maxSidebarFromContent);
        }
        this.sidebarX = this.bodyX;
        this.sidebarY = this.bodyY;
        this.sidebarHeight = this.bodyHeight;

        this.contentX = this.sidebarX + this.sidebarWidth + paddingWin;
        this.contentY = this.bodyY + paddingWin;
        int remainingContentW = (this.windowX + this.windowWidth) - this.contentX - paddingWin;
        this.contentWidth = Math.max(1, remainingContentW);
        this.contentHeight = Math.max(1, this.bodyHeight - paddingWin * 2);
    }

    public boolean isCompact() {
        return this.sizeClass == ScreenSizeClass.COMPACT;
    }

    public boolean isSmallScreen() {
        return this.isSmallScreen;
    }

    public int getContentPadding() {
        return isCompact() ? ActivityMetrics.PADDING_CONTENT_COMPACT : ActivityMetrics.PADDING_CONTENT;
    }

    public int getRowSpacing() {
        return isCompact() ? ActivityMetrics.ROW_SPACING_COMPACT : ActivityMetrics.ROW_SPACING;
    }

    public int getColumnGap() {
        return isCompact() ? ActivityMetrics.COLUMN_GAP_COMPACT : ActivityMetrics.COLUMN_GAP;
    }

    public static WindowLayout compute(int screenWidth, int screenHeight) {
        return compute(screenWidth, screenHeight, -1, -1, -1, -1, false);
    }

    public static WindowLayout compute(int screenWidth, int screenHeight, int customX, int customY) {
        return compute(screenWidth, screenHeight, customX, customY, -1, -1, false);
    }

    public static WindowLayout compute(int screenWidth, int screenHeight, int customX, int customY, boolean isMaximized) {
        return compute(screenWidth, screenHeight, customX, customY, -1, -1, isMaximized);
    }

    public static WindowLayout compute(int screenWidth, int screenHeight, int customX, int customY, int customWidth, int customHeight, boolean isMaximized) {
        return new WindowLayout(screenWidth, screenHeight, customX, customY, customWidth, customHeight, isMaximized);
    }
}

package activity.client.gui.custom.hud;

public record CooldownLayout(int width, int height, int columns, int rows, int cellWidth) {
    public static final int HEADER_HEIGHT = 22;
    public static final int ROW_HEIGHT = 14;
    public static final int PADDING = 9;
    public static final int COLUMN_GAP = 12;
    public static final int CONTENT_TOP = HEADER_HEIGHT + 5;

    public static CooldownLayout of(int count, int cellWidth, boolean vertical, int availableWidth) {
        int safeWidth = Math.max(48, Math.min(cellWidth, Math.max(48, availableWidth - PADDING * 2)));
        int columns = vertical ? 1 : Math.max(1, Math.min(count,
                (Math.max(48, availableWidth) - PADDING * 2 + COLUMN_GAP) / (safeWidth + COLUMN_GAP)));
        columns = Math.max(1, Math.min(3, columns));
        int rows = Math.max(0, (count + columns - 1) / columns);
        int width = Math.max(100, columns * safeWidth + (columns - 1) * COLUMN_GAP + PADDING * 2);
        return new CooldownLayout(width, rows == 0 ? HEADER_HEIGHT : CONTENT_TOP + rows * ROW_HEIGHT + 5,
                columns, rows, safeWidth);
    }
}

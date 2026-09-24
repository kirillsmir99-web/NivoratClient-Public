package activity.client.gui.icon;

import java.util.ArrayList;
import java.util.List;

public enum ActivityIcon {

    ANIMATION(0, 0, 24, 24,
        "XXXXXXXXXX",
        "X.X....X.X",
        "XXX.XX.XXX",
        "X.X.XX.X.X",
        "XXX.XX.XXX",
        "X.X.XX.X.X",
        "XXX.XX.XXX",
        "X.X....X.X",
        "XXXXXXXXXX",
        ".........."
    ),
    CHECK(24, 0, 24, 24,
        "..........",
        "........XX",
        ".......XXX",
        "......XXX.",
        "XX...XXX..",
        "XXX.XXX...",
        ".XXXXX....",
        "..XXX.....",
        "...X......",
        ".........."
    ),
    CHEVRON_DOWN(48, 0, 24, 24,
        "..........",
        "XX......XX",
        "XXXX..XXXX",
        ".XXXXXXXX.",
        "..XXXXXX..",
        "...XXXX...",
        "....XX....",
        "..........",
        "..........",
        ".........."
    ),
    CHEVRON_RIGHT(72, 0, 24, 24,
        "..XX......",
        "..XXXX....",
        "....XXXX..",
        "......XX..",
        "......XX..",
        "....XXXX..",
        "..XXXX....",
        "..XX......",
        "..........",
        ".........."
    ),
    CLOSE(96, 0, 24, 24,
        "XX......XX",
        ".XX....XX.",
        "..XX..XX..",
        "...XXXX...",
        "....XX....",
        "....XX....",
        "...XXXX...",
        "..XX..XX..",
        ".XX....XX.",
        "XX......XX"
    ),
    COPY(120, 0, 24, 24,
        "....XX....",
        "...XXXX...",
        "..XXXXXX..",
        ".XX.XX.XX.",
        "....XX....",
        "....XX....",
        "..........",
        "XX......XX",
        "XX......XX",
        "XXXXXXXXXX"
    ),
    DISCORD(144, 0, 24, 24,
        ".XX....XX.",
        "XXXXXXXXXX",
        "XXXXXXXXXX",
        "XX.XXXX.XX",
        "XX.XXXX.XX",
        "XXXXXXXXXX",
        ".XXXXXXXX.",
        "..XX..XX..",
        "..XX..XX..",
        ".........."
    ),
    DONATE(168, 0, 24, 24,
        ".XXX..XXX.",
        "XXXXXXXXXX",
        "XXXXXXXXXX",
        "XXXXXXXXXX",
        ".XXXXXXXX.",
        "..XXXXXX..",
        "...XXXX...",
        "....XX....",
        "..........",
        ".........."
    ),
    EXTERNAL(0, 24, 24, 24,
        "....XXXXXX",
        "....XXXXXX",
        "....XX..XX",
        "....XX..XX",
        "....XX....",
        "XX..XX....",
        "XX..XX....",
        "XX..XX....",
        "XXXXXXXXXX",
        "XXXXXXXXXX"
    ),
    FONT(24, 24, 24, 24,
        "...XXXX...",
        "..XXXXXX..",
        "..XX..XX..",
        ".XX....XX.",
        ".XXXXXXXX.",
        ".XXXXXXXX.",
        "XX......XX",
        "XX......XX",
        "XX......XX",
        ".........."
    ),
    GLASS(48, 24, 24, 24,
        ".XXXXXXXX.",
        "XX.....XXX",
        "XX....XXXX",
        "XX...XX.XX",
        "XX..XX..XX",
        "XX.XX...XX",
        "XXXX....XX",
        "XXX.....XX",
        ".XXXXXXXX.",
        ".........."
    ),
    IMPORT(72, 24, 24, 24,
        "....XX....",
        "....XX....",
        ".XX.XX.XX.",
        "..XXXXXX..",
        "...XXXX...",
        "....XX....",
        "..........",
        "XX......XX",
        "XX......XX",
        "XXXXXXXXXX"
    ),
    INFO(96, 24, 24, 24,
        "...XXXX...",
        "..XX..XX..",
        ".XX....XX.",
        ".XX.XX.XX.",
        ".XX....XX.",
        ".XX.XX.XX.",
        ".XX.XX.XX.",
        ".XX.XX.XX.",
        "..XX..XX..",
        "...XXXX..."
    ),
    KEYBIND(120, 24, 24, 24,
        "XXXXXXXXXX",
        "XX......XX",
        "XX.X..X.XX",
        "XX.X.X..XX",
        "XX.XX...XX",
        "XX.X.X..XX",
        "XX.X..X.XX",
        "XX......XX",
        "XXXXXXXXXX",
        ".........."
    ),
    MAXIMIZE(144, 24, 24, 24,
        "XXXXXXXXXX",
        "XXXXXXXXXX",
        "XX......XX",
        "XX......XX",
        "XX......XX",
        "XX......XX",
        "XX......XX",
        "XX......XX",
        "XXXXXXXXXX",
        "XXXXXXXXXX"
    ),
    PIN(168, 24, 24, 24,
        "....XX....",
        "...XXXX...",
        "..XXXXXX..",
        "....XX....",
        "....XX....",
        "....XX....",
        "....XX....",
        "....XX....",
        "....XX....",
        ".........."
    ),
    REFRESH(0, 48, 24, 24,
        "..XXXX.XX.",
        ".XX...XXXX",
        "XX......XX",
        "XX........",
        "XX........",
        "XX........",
        "XX......XX",
        ".XX...XXXX",
        "..XXXX.XX.",
        ".........."
    ),
    RESET(24, 48, 24, 24,
        "....XXXX..",
        "..XX...XXX",
        ".XX.....XX",
        "XX......X.",
        "XX...XXXXX",
        "XX...XXXXX",
        "XX......X.",
        ".XX.....XX",
        "..XX...XXX",
        "....XXXX.."
    ),
    RESTORE(48, 48, 24, 24,
        "..XXXXXX..",
        "..X....X..",
        "XXXXXX.X..",
        "X....X.X..",
        "X....XXXXX",
        "X....X...X",
        "XXXXXX...X",
        ".....X...X",
        ".....XXXXX",
        ".........."
    ),
    SAVE(72, 48, 24, 24,
        "XXXXXXXX..",
        "XX....XX.X",
        "XX....XXXX",
        "XXXXXXXXXX",
        "XXXXXXXXXX",
        "XX......XX",
        "XX.XXXX.XX",
        "XX.XXXX.XX",
        "XX......XX",
        "XXXXXXXXXX"
    ),
    SEARCH(96, 48, 24, 24,
        ".XXXXX....",
        "XX...XX...",
        "XX...XX...",
        "XX...XX...",
        ".XXXXX....",
        "....XXX...",
        ".....XXX..",
        "......XXX.",
        ".......XXX",
        "........XX"
    ),
    SETTINGS(120, 48, 24, 24,
        "...XXXX...",
        ".XX.XX.XX.",
        ".X.XXXX.X.",
        "XXX....XXX",
        "XX..XX..XX",
        "XX..XX..XX",
        "XXX....XXX",
        ".X.XXXX.X.",
        ".XX.XX.XX.",
        "...XXXX..."
    ),
    PROFILE(120, 48, 24, 24,
        "...XXXX...",
        "..XXXXXX..",
        "..XXXXXX..",
        "...XXXX...",
        "..........",
        "..XXXXXX..",
        ".XXXXXXXX.",
        "XXXXXXXXXX",
        "XXXXXXXXXX",
        ".........."
    ),
    SOUND(144, 48, 24, 24,
        "...XX.....",
        "..XXX..X..",
        ".XXXX...X.",
        "XXXXX....X",
        "XXXXX....X",
        "XXXXX....X",
        ".XXXX...X.",
        "..XXX..X..",
        "...XX.....",
        ".........."
    ),
    TELEGRAM(168, 48, 24, 24,
        ".........X",
        ".......XXX",
        ".....XXXXX",
        "...XXXXXXX",
        ".XX..XXXXX",
        "..X..XXXX.",
        "..X.XXXX..",
        ".XXXXX....",
        "XXXX......",
        "XX........"
    ),
    TIKTOK(0, 72, 24, 24,
        "....XXXXX.",
        "....XX..XX",
        "....XX...X",
        "....XX....",
        "....XX....",
        ".XXXXX....",
        "XXXXXX....",
        "XXXXXX....",
        ".XXXX.....",
        ".........."
    ),
    TRASH(24, 72, 24, 24,
        "...XXXX...",
        "....XX....",
        "XXXXXXXXXX",
        ".XXXXXXXX.",
        ".X.X..X.X.",
        ".X.X..X.X.",
        ".X.X..X.X.",
        ".X.X..X.X.",
        "..XXXXXX..",
        ".........."
    ),
    WARNING(48, 72, 24, 24,
        "....XX....",
        "...XXXX...",
        "..XX..XX..",
        "..XX..XX..",
        ".XX.XX.XX.",
        ".XX.XX.XX.",
        "XX......XX",
        "XX..XX..XX",
        "XXXXXXXXXX",
        ".........."
    ),
    YOUTUBE(72, 72, 24, 24,
        "XXXXXXXXXX",
        "XXXXXXXXXX",
        "XX..XXXXXX",
        "XX....XXXX",
        "XX......XX",
        "XX....XXXX",
        "XX..XXXXXX",
        "XXXXXXXXXX",
        "XXXXXXXXXX",
        ".........."
    ),

    EDIT(96, 72, 24, 24,
        ".......XX.",
        "......XXXX",
        ".....XXXX.",
        "....XXXX..",
        "...XXXX...",
        "..XXXX....",
        ".XXXX.....",
        "XXXX......",
        "XX........",
        ".........."
    ),

    COMBAT(120, 72, 24, 24,
        ".......XX.",
        "......XXX.",
        ".....XXX..",
        "....XXX...",
        "...XXX....",
        ".XXXX.....",
        "..XX......",
        ".XX.XX....",
        "XX........",
        ".........."
    ),
    DEFENSE(144, 72, 24, 24,
        ".XXXXXXXX.",
        "XXXXXXXXXX",
        "XX......XX",
        "XX......XX",
        "XX......XX",
        ".XX....XX.",
        ".XX....XX.",
        "..XX..XX..",
        "...XXXX...",
        "....XX...."
    ),
    UTILITY(168, 72, 24, 24,
        ".X..XX..X.",
        "..XXXXXX..",
        ".XXXXXXXX.",
        "XXX....XXX",
        "XX......XX",
        "XX......XX",
        "XXX....XXX",
        ".XXXXXXXX.",
        "..XXXXXX..",
        ".X..XX..X."
    ),

    ABOUT(96, 24, 24, 24,
        "...XXXX...",
        "..XX..XX..",
        ".XX....XX.",
        ".XX.XX.XX.",
        ".XX....XX.",
        ".XX.XX.XX.",
        ".XX.XX.XX.",
        ".XX.XX.XX.",
        "..XX..XX..",
        "...XXXX..."
    ),
    CONFIG(120, 48, 24, 24,
        "..X.......",
        "XXXXX.....",
        "..X.......",
        "..........",
        "......X...",
        "....XXXXX.",
        "......X...",
        "..........",
        "...X......",
        ".XXXXX...."
    ),
    RELOAD(0, 48, 24, 24,
        "..XXXX.XX.",
        ".XX...XXXX",
        "XX......XX",
        "XX........",
        "XX........",
        "XX........",
        "XX......XX",
        ".XX...XXXX",
        "..XXXX.XX.",
        ".........."
    ),
    RESET_LAYOUT(24, 48, 24, 24,
        "...XXXX...",
        "...X..X...",
        "...X..X...",
        "XXXX..XXXX",
        "X........X",
        "X........X",
        "XXXX..XXXX",
        "...X..X...",
        "...X..X...",
        "...XXXX..."
    ),
    RECENTER(24, 48, 24, 24,
        "...XXXX...",
        "...X..X...",
        "...X..X...",
        "XXXX..XXXX",
        "X........X",
        "X........X",
        "XXXX..XXXX",
        "...X..X...",
        "...X..X...",
        "...XXXX..."
    ),
    EXPORT(120, 0, 24, 24,
        "....XX....",
        "...XXXX...",
        "..XXXXXX..",
        ".XX.XX.XX.",
        "....XX....",
        "....XX....",
        "..........",
        "XX......XX",
        "XX......XX",
        "XXXXXXXXXX"
    ),
    ENABLED(24, 0, 24, 24,
        "..........",
        "........XX",
        ".......XXX",
        "......XXX.",
        "XX...XXX..",
        "XXX.XXX...",
        ".XXXXX....",
        "..XXX.....",
        "...X......",
        ".........."
    ),
    CHECKMARK(24, 0, 24, 24,
        "..........",
        "........XX",
        ".......XXX",
        "......XXX.",
        "XX...XXX..",
        "XXX.XXX...",
        ".XXXXX....",
        "..XXX.....",
        "...X......",
        ".........."
    ),
    DISABLED(96, 0, 24, 24,
        "...XXXX...",
        "..XXXXXX..",
        ".XX....XX.",
        ".XXXXXXXX.",
        "XXXXXXXXXX",
        "XXXXXXXXXX",
        ".XXXXXXXX.",
        ".XX....XX.",
        "..XXXXXX..",
        "...XXXX..."
    ),
    CHEVRON_UP(0, 96, 24, 24,
        "....XX....",
        "...XXXX...",
        "..XXXXXX..",
        ".XXXXXXXX.",
        "XXXX..XXXX",
        "XX......XX",
        "..........",
        "..........",
        "..........",
        ".........."
    ),
    FOLDER(72, 24, 24, 24,
        ".XXXX.....",
        "XXXXXX....",
        "XXXXXXXXXX",
        "XX......XX",
        "XX......XX",
        "XX......XX",
        "XX......XX",
        "XX......XX",
        "XXXXXXXXXX",
        ".........."
    );

    private final int u;
    private final int v;
    private final int regionW;
    private final int regionH;
    private final int width;
    private final int height;
    private final int[][] horizontalSpans;

    ActivityIcon(int u, int v, int regionW, int regionH, String... pattern) {
        this.u = u;
        this.v = v;
        this.regionW = regionW;
        this.regionH = regionH;

        this.height = pattern.length;
        int maxW = 0;
        for (String row : pattern) {
            if (row.length() > maxW) {
                maxW = row.length();
            }
        }
        this.width = maxW;

        List<int[]> spans = new ArrayList<>();
        for (int r = 0; r < pattern.length; r++) {
            String row = pattern[r];
            int start = -1;
            for (int c = 0; c < row.length(); c++) {
                char ch = row.charAt(c);
                boolean isPixel = (ch != '.' && ch != ' ');
                if (isPixel) {
                    if (start == -1) {
                        start = c;
                    }
                } else {
                    if (start != -1) {
                        spans.add(new int[]{r, start, c});
                        start = -1;
                    }
                }
            }
            if (start != -1) {
                spans.add(new int[]{r, start, row.length()});
            }
        }
        this.horizontalSpans = spans.toArray(new int[0][]);
    }

    ActivityIcon(String... pattern) {
        this(-1, -1, 0, 0, pattern);
    }

    public boolean hasAtlasRegion() {
        return this.u >= 0 && this.v >= 0 && this.regionW > 0 && this.regionH > 0;
    }

    public int getAtlasU() {
        return this.u;
    }

    public int getAtlasV() {
        return this.v;
    }

    public int getAtlasRegionW() {
        return this.regionW;
    }

    public int getAtlasRegionH() {
        return this.regionH;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int[][] getHorizontalSpans() {
        return this.horizontalSpans;
    }

    public void render(net.minecraft.client.gui.DrawContext context, int x, int y, int color) {
        ActivityIconRenderer.draw(context, this, x, y, color);
    }

    public void render(net.minecraft.client.gui.DrawContext context, int x, int y, int color, float alpha) {
        ActivityIconRenderer.draw(context, this, x, y, color, alpha);
    }

    public void renderSized(net.minecraft.client.gui.DrawContext context, int x, int y, int size, int color) {
        ActivityIconRenderer.drawSized(context, this, x, y, size, color);
    }

    public void renderScaled(net.minecraft.client.gui.DrawContext context, int x, int y, int scale, int color) {
        ActivityIconRenderer.drawScaled(context, this, x, y, scale, color);
    }

    public void renderCentered(net.minecraft.client.gui.DrawContext context, int boxX, int boxY, int boxW, int boxH, int color) {
        ActivityIconRenderer.drawCentered(context, this, boxX, boxY, boxW, boxH, color);
    }

    public void renderCenteredSized(net.minecraft.client.gui.DrawContext context, int boxX, int boxY, int boxW, int boxH, int size, int color) {
        ActivityIconRenderer.drawCenteredSized(context, this, boxX, boxY, boxW, boxH, size, color);
    }
}

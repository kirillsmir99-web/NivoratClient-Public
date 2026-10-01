package activity.client.module.impl.utility.gui;

import activity.client.gui.sound.SoundManager;
import activity.client.module.keybind.Keybind;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import dev.audio.AudioSyncClient;

import java.util.ArrayList;
import java.util.List;

public final class AudioWaveRadialScreen extends Screen {

    private final Screen parent;
    private boolean openedByHold;
    private final long openTime;
    private final Keybind boundKey;

    private double lastMouseX;
    private double lastMouseY;

    private final List<String> phrases = new ArrayList<>();
    private int hoveredSector = -1;
    private int lastHoveredSector = -1;
    private boolean lastHubHovered = false;

    public static final int INNER_RADIUS = 56;
    public static final int OUTER_RADIUS = 148;
    public static final int HUB_RADIUS = 46;

    public static final class Span {
        public final short y;
        public final short x1;
        public final short x2;

        public Span(int y, int x1, int x2) {
            this.y = (short) y;
            this.x1 = (short) x1;
            this.x2 = (short) x2;
        }
    }

    public static final class BlockSpan {
        public final short y1;
        public final short y2;
        public final short x1;
        public final short x2;

        public BlockSpan(int y1, int y2, int x1, int x2) {
            this.y1 = (short) y1;
            this.y2 = (short) y2;
            this.x1 = (short) x1;
            this.x2 = (short) x2;
        }
    }

    public static List<Span> optimizeSpans(List<Span> raw) {
        if (raw == null || raw.isEmpty()) return new ArrayList<>();
        List<Span> sorted = new ArrayList<>(raw);
        sorted.sort((a, b) -> {
            if (a.y != b.y) return Short.compare(a.y, b.y);
            return Short.compare(a.x1, b.x1);
        });

        List<Span> optimized = new ArrayList<>();
        Span current = sorted.get(0);
        int curY = current.y;
        int curX1 = current.x1;
        int curX2 = current.x2;

        for (int i = 1; i < sorted.size(); i++) {
            Span next = sorted.get(i);
            if (next.y == curY && next.x1 <= curX2) {
                curX2 = Math.max(curX2, next.x2);
            } else {
                optimized.add(new Span(curY, curX1, curX2));
                curY = next.y;
                curX1 = next.x1;
                curX2 = next.x2;
            }
        }
        optimized.add(new Span(curY, curX1, curX2));
        return optimized;
    }

    public static List<BlockSpan> coalesceSpans(List<Span> spans, int stepY) {
        if (spans == null || spans.isEmpty()) return new ArrayList<>();
        List<Span> sorted = optimizeSpans(spans);
        List<BlockSpan> result = new ArrayList<>();
        List<BlockSpan> active = new ArrayList<>();

        for (Span s : sorted) {
            for (int i = active.size() - 1; i >= 0; i--) {
                BlockSpan b = active.get(i);
                if (s.y > b.y2) {
                    result.add(b);
                    active.remove(i);
                }
            }

            boolean matched = false;
            for (int i = 0; i < active.size(); i++) {
                BlockSpan b = active.get(i);
                if (b.y2 == s.y && b.x1 == s.x1 && b.x2 == s.x2) {
                    active.set(i, new BlockSpan(b.y1, (short) (s.y + stepY), b.x1, b.x2));
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                active.add(new BlockSpan(s.y, (short) (s.y + stepY), s.x1, s.x2));
            }
        }
        result.addAll(active);
        return result;
    }

    public AudioWaveRadialScreen(Screen parent) {
        this(parent, false, new Keybind(GLFW.GLFW_KEY_G));
    }

    public AudioWaveRadialScreen(Screen parent, boolean openedByHold, Keybind boundKey) {
        super(Text.translatable("activity.module.auto_gg.name"));
        this.parent = parent;
        this.openedByHold = openedByHold;
        this.openTime = System.currentTimeMillis();
        this.boundKey = boundKey != null ? boundKey : new Keybind(GLFW.GLFW_KEY_G);
        loadPhrases();
    }

    public static List<String> getDefaultPhrases() {
        List<String> list = new ArrayList<>();
        if (AudioSyncClient.CONFIG != null && AudioSyncClient.CONFIG.phrases != null) {
            for (String p : AudioSyncClient.CONFIG.phrases) {
                if (p != null && !p.isBlank() && !list.contains(p)) {
                    list.add(p);
                    if (list.size() >= 8) break;
                }
            }
        }
        if (list.isEmpty()) {
            list.add("GGWP");
            list.add("ez");
            list.add("GG");
        }
        return list;
    }

    private void loadPhrases() {
        this.phrases.clear();
        this.phrases.addAll(getDefaultPhrases());
    }

    @Override
    protected void init() {
        super.init();
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
            if (this.client != null) this.client.setScreen(null);
            else super.close();
            return;
        }
        loadPhrases();
        SoundManager.playOpen();
    }

    @Override
    public void close() {
        SoundManager.playClose();
        if (this.client != null && this.client.getWindow() != null) {
            activity.client.module.keybind.KeybindManager.suppressAllHeldKeys(this.client.getWindow());
        }
        activity.client.module.keybind.KeybindManager.suppressKey("sec:auto_gg:menu_keybind");
        if (this.client != null) {
            this.client.setScreen(this.parent);
        } else {
            super.close();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;

        if (openedByHold && System.currentTimeMillis() - openTime > 150L) {
            if (client != null && client.getWindow() != null && boundKey != null && !boundKey.isUnbound()) {
                boolean ctrl = InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL) || InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
                boolean shift = InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) || InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
                boolean alt = InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_ALT) || InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_ALT);
                if (!boundKey.matchesWindow(client.getWindow(), ctrl, shift, alt)) {
                    triggerHoldRelease(mouseX, mouseY);
                    return;
                }
            }
        }

        int cx = width / 2;
        int cy = height / 2 - (height < 220 ? 4 : 8);

        context.fill(0, 0, width, height, 0x25000000);

        float scale = computeScale();
        double virtMouseX = cx + (mouseX - cx) / scale;
        double virtMouseY = cy + (mouseY - cy) / scale;

        int count = phrases.size();
        this.hoveredSector = getHoveredSector(virtMouseX, virtMouseY, cx, cy, count);

        if (this.hoveredSector != this.lastHoveredSector) {
            if (this.hoveredSector != -1) {
                SoundManager.playHoverImmediate();
            }
            this.lastHoveredSector = this.hoveredSector;
        }

        boolean hubHovered = isInsideHub(virtMouseX, virtMouseY, cx, cy, HUB_RADIUS);
        if (hubHovered != this.lastHubHovered) {
            if (hubHovered) {
                SoundManager.playHoverImmediate();
            }
            this.lastHubHovered = hubHovered;
        }

        context.getMatrices().pushMatrix();
        context.getMatrices().scaleAround(scale, scale, (float) cx, (float) cy);

        activity.client.gui.custom.NativeAutoGgWheel.render(context,cx,cy,phrases,AudioSyncClient.CONFIG.currentPhrase(),hoveredSector,hubHovered);

        context.getMatrices().popMatrix();

        super.render(context, mouseX, mouseY, delta);
    }

    public float computeScale() {
        float availH = height - 20;
        float availW = width - 16;
        float baseScale = Math.clamp(Math.min(availH / 320.0f, availW / 320.0f), 0.45f, 1.0f);
        long elapsed = System.currentTimeMillis() - openTime;
        float progress = Math.min(1.0f, elapsed / 160.0f);
        float ease = 1.0f - (float) Math.pow(1.0f - progress, 3);
        float animScale = 0.88f + 0.12f * ease;
        return baseScale * animScale;
    }

    public static int getHoveredSector(double mouseX, double mouseY, int cx, int cy, int count) {
        if (count <= 0) return -1;
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < INNER_RADIUS || distance > (OUTER_RADIUS + 16)) {
            return -1;
        }
        double angle = Math.atan2(dy, dx) + Math.PI / 2.0;
        if (angle < 0) angle += Math.PI * 2.0;
        double sectorAngle = (Math.PI * 2.0) / count;
        return (int) (angle / sectorAngle) % count;
    }

    public static boolean isInsideHub(double mouseX, double mouseY, int cx, int cy, int radius) {
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        return (dx * dx + dy * dy) <= (radius * radius);
    }

    private void triggerHoldRelease(double mouseX, double mouseY) {
        int cx = width / 2;
        int cy = height / 2 - (height < 220 ? 4 : 8);
        float scale = computeScale();
        double virtMx = cx + (mouseX - cx) / scale;
        double virtMy = cy + (mouseY - cy) / scale;
        int selected = getHoveredSector(virtMx, virtMy, cx, cy, phrases.size());
        if (selected >= 0 && selected < phrases.size()) {
            sendPhraseFromSector(phrases.get(selected));
        } else {
            this.openedByHold = false;
        }
    }

    private void sendPhraseFromSector(String phrase) {
        if (phrase == null || phrase.isBlank()) return;
        AudioSyncClient.sendPhraseDirect(phrase.trim());
        SoundManager.playSelect();
        close();
    }

    private boolean isTelegramHovered(double mouseX, double mouseY, int cx, int cy) {
        return activity.client.gui.custom.NativeAutoGgWheel.isFooterHovered(mouseX, mouseY, cx, cy);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x();
        double my = click.y();
        int cx = width / 2;
        int cy = height / 2 - (height < 220 ? 4 : 8);
        float scale = computeScale();
        double virtMx = cx + (mx - cx) / scale;
        double virtMy = cy + (my - cy) / scale;

        if (click.button() == 0 && isInsideHub(virtMx, virtMy, cx, cy, HUB_RADIUS)) {
            SoundManager.playClick();
            if (this.client != null) {
                var screen = activity.client.gui.custom.api.ui.UI.INSTANCE;
                this.client.setScreen(screen);
                screen.navigateToModule("auto_gg");
            }
            return true;
        }

        if (click.button() == 0 && isTelegramHovered(virtMx, virtMy, cx, cy)) {
            try {
                net.minecraft.util.Util.getOperatingSystem().open("https://t.me/virionDEV");
            } catch (Throwable t) {
                if (client != null && client.keyboard != null) {
                    client.keyboard.setClipboard("https://t.me/virionDEV");
                }
            }
            SoundManager.playClick();
            return true;
        }

        if (click.button() == 0) {
            int selected = getHoveredSector(virtMx, virtMy, cx, cy, phrases.size());
            if (selected >= 0 && selected < phrases.size()) {
                sendPhraseFromSector(phrases.get(selected));
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }

        if (boundKey != null && input.key() == boundKey.getKeyCode()) {
            if (System.currentTimeMillis() - openTime > 60L) {
                if (this.client != null && this.client.getWindow() != null) {
                    activity.client.module.keybind.KeybindManager.suppressAllHeldKeys(this.client.getWindow());
                }
                activity.client.module.keybind.KeybindManager.suppressKey("sec:auto_gg:menu_keybind");
                close();
                return true;
            }
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean keyReleased(KeyInput input) {
        if (openedByHold && boundKey != null && input.key() == boundKey.getKeyCode()) {
            triggerHoldRelease(lastMouseX, lastMouseY);
            return true;
        }
        return super.keyReleased(input);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {

    }

    private static void drawBlockList(DrawContext context, int cx, int cy, List<BlockSpan> blocks, int color) {
        if (blocks == null || blocks.isEmpty()) return;
        for (int i = 0; i < blocks.size(); i++) {
            BlockSpan b = blocks.get(i);
            context.fill(cx + b.x1, cy + b.y1, cx + b.x2, cy + b.y2, color);
        }
    }

    public static String findAutocomplete(String query) {
        List<String> candidates = List.of("GGWP", "ez", "GG", "Good Fight", "EZ", "GF", "Well Played", "WP");
        if (query == null || query.isBlank()) {
            return "GGWP";
        }
        String q = query.trim().toLowerCase(java.util.Locale.ROOT);
        for (String c : candidates) {
            if (c.toLowerCase(java.util.Locale.ROOT).startsWith(q) && !c.equalsIgnoreCase(query.trim())) {
                return c;
            }
        }
        return null;
    }

    public List<String> getPhrases() {
        return phrases;
    }
}

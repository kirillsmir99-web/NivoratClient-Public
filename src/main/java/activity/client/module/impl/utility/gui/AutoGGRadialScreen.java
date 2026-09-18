package activity.client.module.impl.utility.gui;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.ActivityScreen;
import activity.client.gui.component.ActivityButton;
import activity.client.gui.component.ActivityTextField;
import activity.client.gui.render.ActivityGuiRenderer;
import activity.client.gui.theme.ActivityColors;
import activity.client.module.keybind.Keybind;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import ru.elarion.autogg.AutoGGClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Modern dark glassmorphism Radial Menu for AutoGG phrases.
 *
 * <p>Matches the NivoratClient visual theme with:
 * <ul>
 *   <li>Translucent dark glass backdrop and glowing neon cyan highlights (#00D2FF).</li>
 *   <li>Hybrid interaction: hold-and-release (drag to phrase and release key) or tap-and-click.</li>
 *   <li>Central hub with module status and direct "⚙ Настройки" shortcut to ActivityScreen.</li>
 *   <li>Quick custom phrase text entry and editing field directly within the radial HUD.</li>
 * </ul>
 */
public final class AutoGGRadialScreen extends Screen {

    private final Screen parent;
    private boolean openedByHold;
    private final long openTime;
    private final Keybind boundKey;

    private double lastMouseX;
    private double lastMouseY;

    private final List<String> phrases = new ArrayList<>();
    private int hoveredSector = -1;

    // UI Widgets
    private ActivityTextField customPhraseField;
    private ActivityButton sendCustomButton;

    // Dimensions
    private static final int INNER_RADIUS = 56;
    private static final int OUTER_RADIUS = 148;
    private static final int HUB_RADIUS = 48;

    public AutoGGRadialScreen(Screen parent) {
        this(parent, false, new Keybind(GLFW.GLFW_KEY_G));
    }

    public AutoGGRadialScreen(Screen parent, boolean openedByHold, Keybind boundKey) {
        super(Text.translatable("activity.module.auto_gg.name"));
        this.parent = parent;
        this.openedByHold = openedByHold;
        this.openTime = System.currentTimeMillis();
        this.boundKey = boundKey != null ? boundKey : new Keybind(GLFW.GLFW_KEY_G);
        loadPhrases();
    }

    public static List<String> getDefaultPhrases() {
        List<String> list = new ArrayList<>();
        if (AutoGGClient.CONFIG != null && AutoGGClient.CONFIG.phrases != null) {
            for (String p : AutoGGClient.CONFIG.phrases) {
                if (p != null && !p.isBlank() && !list.contains(p)) {
                    list.add(p);
                }
            }
        }
        // Ensure at least 6 standard phrases for rich radial wheel balance
        String[] defaults = new String[] {"GGWP", "GG", "Well Played!", "EZ", "Good Fight", "GF", "Катка супер!", "Мощно!"};
        for (String def : defaults) {
            if (list.size() >= 8) break;
            if (!list.contains(def)) {
                list.add(def);
            }
        }
        return list;
    }

    private void loadPhrases() {
        this.phrases.clear();
        this.phrases.addAll(getDefaultPhrases());
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2 - 14;

        int fieldW = 160;
        int btnW = 75;
        int fieldY = cy + OUTER_RADIUS + 24;

        this.customPhraseField = new ActivityTextField(cx - (fieldW + btnW + 6) / 2, fieldY, fieldW, 22, Text.literal("Своя фраза..."));
        String curr = AutoGGClient.CONFIG.currentPhrase();
        this.customPhraseField.setText(curr != null ? curr : "GGWP");

        this.sendCustomButton = new ActivityButton(
                cx - (fieldW + btnW + 6) / 2 + fieldW + 6, fieldY, btnW, 22,
                Text.literal("Отправить"),
                ActivityButton.Variant.PRIMARY,
                b -> {
                    String custom = this.customPhraseField.getText();
                    if (custom != null && !custom.isBlank()) {
                        selectAndSend(custom.trim());
                    }
                }
        );
    }

    @Override
    public void close() {
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

        // Hold-and-release check
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
        int cy = height / 2 - 14;

        // 1. Dark glass background overlay
        context.fill(0, 0, width, height, ActivityColors.BACKGROUND_OVERLAY);

        // 2. Base radial ring fill
        drawRingFill(context, cx, cy, INNER_RADIUS, OUTER_RADIUS, 0xD00E1015);

        // 3. Hovered sector detection & glow
        int count = phrases.size();
        this.hoveredSector = getHoveredSector(mouseX, mouseY, cx, cy, count);

        if (this.hoveredSector >= 0 && this.hoveredSector < count) {
            double sectorAngle = (Math.PI * 2.0) / count;
            double a0 = -Math.PI / 2.0 + this.hoveredSector * sectorAngle;
            double a1 = a0 + sectorAngle;
            drawSectorArc(context, cx, cy, INNER_RADIUS, OUTER_RADIUS + 8, a0, a1, 0x4500D2FF);
        }

        // 4. Radial divider lines
        for (int i = 0; i < count; i++) {
            double a = -Math.PI / 2.0 + i * (Math.PI * 2.0 / count);
            drawRadialLine(context, cx, cy, INNER_RADIUS, OUTER_RADIUS, a, 0x55353B49);
        }

        // 5. Circular ring borders
        drawCircleBorder(context, cx, cy, INNER_RADIUS, 0x70353B49);
        drawCircleBorder(context, cx, cy, OUTER_RADIUS, 0x70353B49);

        // 6. Sector Text labels
        for (int i = 0; i < count; i++) {
            double mid = -Math.PI / 2.0 + (i + 0.5) * (Math.PI * 2.0 / count);
            int textRadius = (INNER_RADIUS + OUTER_RADIUS) / 2;
            int tx = cx + (int) Math.round(Math.cos(mid) * textRadius);
            int ty = cy + (int) Math.round(Math.sin(mid) * textRadius);

            boolean isHov = (this.hoveredSector == i);
            int color = isHov ? 0xFF00D2FF : 0xFFE0E6ED;
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(phrases.get(i)), tx, ty - 4, color);
        }

        // 7. Central Hub
        drawCircleFill(context, cx, cy, HUB_RADIUS, 0xF2090A0E);
        drawCircleBorder(context, cx, cy, HUB_RADIUS, 0xFF353B49);

        // Central title
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("AutoGG"), cx, cy - 26, 0xFF00D2FF);

        // Status pill
        boolean enabled = AutoGGClient.CONFIG.enabled;
        int statusColor = enabled ? ActivityColors.STATE_ON_BG : ActivityColors.DANGER;
        String statusText = enabled ? "● ВКЛ" : "● ВЫКЛ";
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(statusText), cx, cy - 12, statusColor);

        // Settings Button in hub
        int btnHubX = cx - 34;
        int btnHubY = cy + 4;
        int btnHubW = 68;
        int btnHubH = 18;
        boolean btnHov = isInside(mouseX, mouseY, btnHubX, btnHubY, btnHubW, btnHubH);
        int btnBg = btnHov ? ActivityColors.BUTTON_PRIMARY_HOVER : ActivityColors.BUTTON_SECONDARY_BG;
        int btnBorder = btnHov ? ActivityColors.ACCENT_LIGHT : ActivityColors.BORDER;
        ActivityGuiRenderer.drawPanel(context, btnHubX, btnHubY, btnHubW, btnHubH, btnBg, btnBorder, true);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("⚙ Меню"), cx, btnHubY + 5, btnHov ? 0xFFFFFFFF : 0xFFB0B8C4);

        // 8. Custom Phrase Field & Button
        if (this.customPhraseField != null) {
            this.customPhraseField.render(context, mouseX, mouseY, delta);
        }
        if (this.sendCustomButton != null) {
            this.sendCustomButton.render(context, mouseX, mouseY, delta);
        }

        // 9. Instructions hint
        String hint = openedByHold
                ? "Отпустите клавишу для выбора и отправки в чат"
                : "Кликните по сектору для мгновенной отправки";
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(hint), cx, cy + OUTER_RADIUS + 54, 0xFF8D94A3);

        super.render(context, mouseX, mouseY, delta);
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

    private void triggerHoldRelease(double mouseX, double mouseY) {
        int cx = width / 2;
        int cy = height / 2 - 14;
        int selected = getHoveredSector(mouseX, mouseY, cx, cy, phrases.size());
        if (selected >= 0 && selected < phrases.size()) {
            selectAndSend(phrases.get(selected));
        } else {
            this.openedByHold = false;
        }
    }

    private void selectAndSend(String phrase) {
        if (phrase == null || phrase.isBlank()) return;
        phrase = phrase.trim();

        // Update phrase order in config
        AutoGGClient.CONFIG.phrases.remove(phrase);
        AutoGGClient.CONFIG.phrases.add(0, phrase);
        AutoGGClient.CONFIG.selected = 0;
        AutoGGClient.CONFIG.save();

        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            c.autoGGPhrase = phrase;
            ActivityConfigManager.markDirty();
        }

        AutoGGClient.sendPhraseDirect(phrase);
        ActivityGuiRenderer.playClickSound();
        close();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x();
        double my = click.y();
        int cx = width / 2;
        int cy = height / 2 - 14;

        // 1. Settings button click
        int btnHubX = cx - 34;
        int btnHubY = cy + 4;
        int btnHubW = 68;
        int btnHubH = 18;
        if (click.button() == 0 && isInside(mx, my, btnHubX, btnHubY, btnHubW, btnHubH)) {
            ActivityGuiRenderer.playClickSound();
            if (this.client != null) {
                ActivityScreen screen = new ActivityScreen();
                this.client.setScreen(screen);
                screen.navigateToModule("auto_gg");
            }
            return true;
        }

        // 2. Custom text field click
        if (this.customPhraseField != null && this.customPhraseField.mouseClicked(click, doubled)) {
            return true;
        }

        // 3. Send custom button click
        if (this.sendCustomButton != null && this.sendCustomButton.mouseClicked(click, doubled)) {
            return true;
        }

        // 4. Sector click
        if (click.button() == 0) {
            int selected = getHoveredSector(mx, my, cx, cy, phrases.size());
            if (selected >= 0 && selected < phrases.size()) {
                selectAndSend(phrases.get(selected));
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (this.customPhraseField != null && this.customPhraseField.keyPressed(input)) {
            if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) {
                String text = this.customPhraseField.getText();
                if (text != null && !text.isBlank()) {
                    selectAndSend(text.trim());
                    return true;
                }
            }
            return true;
        }

        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }

        if (boundKey != null && input.key() == boundKey.getKeyCode()) {
            close();
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (this.customPhraseField != null && this.customPhraseField.charTyped(input)) {
            return true;
        }
        return super.charTyped(input);
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
        // Intentionally override to avoid vanilla dirt screen background
    }

    private static boolean isInside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static void drawCircleFill(DrawContext context, int cx, int cy, int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            int halfWidth = (int) Math.sqrt(radius * radius - y * y);
            context.fill(cx - halfWidth, cy + y, cx + halfWidth + 1, cy + y + 1, color);
        }
    }

    private static void drawRingFill(DrawContext context, int cx, int cy, int inner, int outer, int color) {
        for (int y = -outer; y <= outer; y++) {
            int outerHalf = (int) Math.sqrt(outer * outer - y * y);
            int innerHalf = (Math.abs(y) < inner) ? (int) Math.sqrt(inner * inner - y * y) : 0;
            if (innerHalf > 0) {
                context.fill(cx - outerHalf, cy + y, cx - innerHalf, cy + y + 1, color);
                context.fill(cx + innerHalf + 1, cy + y, cx + outerHalf + 1, cy + y + 1, color);
            } else {
                context.fill(cx - outerHalf, cy + y, cx + outerHalf + 1, cy + y + 1, color);
            }
        }
    }

    private static void drawCircleBorder(DrawContext context, int cx, int cy, int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            int halfWidth = (int) Math.sqrt(radius * radius - y * y);
            context.fill(cx - halfWidth, cy + y, cx - halfWidth + 1, cy + y + 1, color);
            context.fill(cx + halfWidth, cy + y, cx + halfWidth + 1, cy + y + 1, color);
        }
    }

    private static void drawRadialLine(DrawContext c, int cx, int cy, int inner, int outer, double angle, int color) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        for (int r = inner; r <= outer; r++) {
            int x = cx + (int) Math.round(cos * r);
            int y = cy + (int) Math.round(sin * r);
            c.fill(x, y, x + 1, y + 1, color);
        }
    }

    private static void drawSectorArc(DrawContext context, int cx, int cy, int inner, int outer, double startAngle, double endAngle, int color) {
        double step = 0.02;
        for (double a = startAngle; a <= endAngle; a += step) {
            double cos = Math.cos(a);
            double sin = Math.sin(a);
            for (int r = inner; r <= outer; r += 2) {
                int x = cx + (int) Math.round(cos * r);
                int y = cy + (int) Math.round(sin * r);
                context.fill(x, y, x + 2, y + 2, color);
            }
        }
    }

    public List<String> getPhrases() {
        return phrases;
    }
}

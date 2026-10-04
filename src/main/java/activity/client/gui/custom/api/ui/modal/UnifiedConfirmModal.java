package activity.client.gui.custom.api.ui.modal;

import activity.client.gui.custom.api.drags.Position;
import activity.client.gui.custom.api.ui.button.UnifiedButton;
import activity.client.gui.custom.api.ui.theme.ThemeManager;
import activity.client.gui.custom.mixin.accessor.GuiGraphicsExtractorAccessor;
import activity.client.gui.custom.utils.animations.Decelerate;
import activity.client.gui.custom.utils.animations.Direction;
import activity.client.gui.custom.utils.render.fonts.Fonts;
import activity.client.gui.custom.utils.render.render2d.Render2D;
import activity.client.gui.custom.utils.render.render2d.glow.BuiltGlow;
import activity.client.gui.custom.utils.sounds.Sounds;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public final class UnifiedConfirmModal {
    public static final class ModalButton {
        public final String text;
        public final UnifiedButton.Variant variant;
        public final Runnable action;

        public ModalButton(String text, UnifiedButton.Variant variant, Runnable action) {
            this.text = text;
            this.variant = variant;
            this.action = action;
        }
    }

    private static String currentTitle;
    private static String currentLine1;
    private static String currentLine2;
    private static int warningIconCol = 0;
    private static final List<ModalButton> buttons = new ArrayList<>();
    private static Runnable onDismissAction;
    private static boolean active = false;
    private static final Decelerate anim = createAnim(180);

    private static float lastModalX, lastModalY, lastModalW, lastModalH;
    private static final List<ButtonRect> lastButtonRects = new ArrayList<>();

    private static final class ButtonRect {
        final float x, y, w, h;
        final ModalButton btn;
        ButtonRect(float x, float y, float w, float h, ModalButton btn) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.btn = btn;
        }
    }

    private UnifiedConfirmModal() {}

    private static Decelerate createAnim(int ms) {
        Decelerate d = (Decelerate) new Decelerate().setMs(ms).setValue(1.0);
        d.setDirection(Direction.BACKWARDS);
        d.counter.setTime(System.currentTimeMillis() - 10000L);
        return d;
    }

    public static void show(String title, String line1, String line2, int iconCol, List<ModalButton> btns, Runnable onDismiss) {
        currentTitle = title;
        currentLine1 = line1;
        currentLine2 = line2;
        warningIconCol = iconCol;
        buttons.clear();
        if (btns != null) buttons.addAll(btns);
        onDismissAction = onDismiss;
        active = true;
        anim.setDirection(Direction.FORWARDS);
        try { Sounds.play("buttonclick"); } catch (Throwable ignored) {}
    }

    public static boolean isOpen() {
        return active || anim.getOutput().floatValue() > 0.01f;
    }

    public static void close() {
        if (!active) return;
        active = false;
        anim.setDirection(Direction.BACKWARDS);
        try { Sounds.play("buttonclick"); } catch (Throwable ignored) {}
        if (onDismissAction != null) {
            Runnable r = onDismissAction;
            onDismissAction = null;
            r.run();
        }
    }

    public static boolean keyPressed(int key) {
        if (!isOpen()) return false;
        if (key == 256) {
            close();
            return true;
        }
        if (key == 257 || key == 335) {
            for (ModalButton b : buttons) {
                if (b.variant == UnifiedButton.Variant.PRIMARY) {
                    executeButton(b);
                    return true;
                }
            }
        }
        return true;
    }

    public static boolean click(float mouseX, float mouseY, int button) {
        if (!isOpen()) return false;
        if (button == 0) {
            for (ButtonRect br : lastButtonRects) {
                if (mouseX >= br.x && mouseX <= br.x + br.w && mouseY >= br.y && mouseY <= br.y + br.h) {
                    executeButton(br.btn);
                    return true;
                }
            }
            if (mouseX < lastModalX || mouseX > lastModalX + lastModalW || mouseY < lastModalY || mouseY > lastModalY + lastModalH) {
                close();
                return true;
            }
        }
        return true;
    }

    private static void executeButton(ModalButton b) {
        active = false;
        anim.setDirection(Direction.BACKWARDS);
        onDismissAction = null;
        try { Sounds.play(b.variant == UnifiedButton.Variant.PRIMARY ? "select_category" : "buttonclick"); } catch (Throwable ignored) {}
        if (b.action != null) {
            b.action.run();
        }
    }

    public static void render(DrawContext context, float alpha) {
        if (!isOpen()) return;
        float t = anim.getOutput().floatValue();
        if (t <= 0.005f) return;
        float effectiveAlpha = alpha * t;

        Render2D.flush();
        ((GuiGraphicsExtractorAccessor) context).nv_getGuiRenderState().createNewRootLayer();
        Render2D.beginFrame(context);

        float sw = Position.screenWidth();
        float sh = Position.screenHeight();
        Render2D.rect(0, 0, sw, sh, 0, ThemeManager.rgba(0, 150.0f * effectiveAlpha));

        float btnH = UnifiedButton.DEFAULT_HEIGHT;
        float totalBtnsW = 0.0f;
        float gap = 6.0f;
        List<Float> btnWidths = new ArrayList<>();
        for (ModalButton b : buttons) {
            float bw = UnifiedButton.computeWidth(b.text, 56.0f);
            btnWidths.add(bw);
            totalBtnsW += bw;
        }
        if (!buttons.isEmpty()) {
            totalBtnsW += gap * (buttons.size() - 1);
        }

        float modalW = Math.max(220.0f, totalBtnsW + 28.0f);
        if (currentLine1 != null) {
            modalW = Math.max(modalW, Fonts.MONTSERRAT_MEDIUM.width(currentLine1, 6.0f) + 28.0f);
        }
        if (currentLine2 != null && !currentLine2.isEmpty()) {
            modalW = Math.max(modalW, Fonts.MONTSERRAT_MEDIUM.width(currentLine2, 5.8f) + 28.0f);
        }

        float modalH = (currentLine2 != null && !currentLine2.isEmpty()) ? 98.0f : 86.0f;
        float modalX = (sw - modalW) * 0.5f;
        float modalY = (sh - modalH) * 0.5f + (1.0f - t) * 8.0f;

        lastModalX = modalX;
        lastModalY = modalY;
        lastModalW = modalW;
        lastModalH = modalH;

        Render2D.glow(new BuiltGlow(modalX, modalY, modalW, modalH, new float[]{8.0f, 8.0f, 8.0f, 8.0f},
                ThemeManager.rgba(0x0e1117, 140.0f * effectiveAlpha), 0.45f, 12.0f, effectiveAlpha));
        Render2D.rect(modalX, modalY, modalW, modalH, 8.0f, ThemeManager.rgba(0x0e1117, 248.0f * effectiveAlpha));
        Render2D.outline(modalX, modalY, modalW, modalH, 8.0f, 0.75f,
                ThemeManager.rgba(0xFFFFFF, 35.0f * effectiveAlpha));

        float titleX = modalX + 14.0f;
        float titleY = modalY + 12.0f;
        if (warningIconCol != 0) {
            Fonts.MONTSERRAT_BOLD.draw("!", titleX, titleY - 0.5f, 8.0f, ThemeManager.rgba(warningIconCol, 255.0f * effectiveAlpha));
            titleX += 10.0f;
        }
        if (currentTitle != null) {
            Fonts.MONTSERRAT_SEMIBOLD.draw(currentTitle, titleX, titleY, 7.5f,
                    ThemeManager.rgba(0xFFFFFF, 245.0f * effectiveAlpha));
        }

        if (currentLine1 != null) {
            Fonts.MONTSERRAT_MEDIUM.draw(currentLine1, modalX + 14.0f, modalY + 28.0f, 6.0f,
                    ThemeManager.rgba(0xDCE2EE, 225.0f * effectiveAlpha));
        }
        if (currentLine2 != null && !currentLine2.isEmpty()) {
            Fonts.MONTSERRAT_MEDIUM.draw(currentLine2, modalX + 14.0f, modalY + 41.0f, 5.8f,
                    warningIconCol != 0 ? ThemeManager.rgba(warningIconCol, 240.0f * effectiveAlpha) : ThemeManager.rgba(0xAAAAAA, 210.0f * effectiveAlpha));
        }

        float btnY = modalY + modalH - btnH - 12.0f;
        float btnStartX = modalX + modalW - 14.0f - totalBtnsW;
        float mx = Position.mouseX();
        float my = Position.mouseY();

        lastButtonRects.clear();
        float curBtnX = btnStartX;
        for (int i = 0; i < buttons.size(); i++) {
            ModalButton mb = buttons.get(i);
            float bw = btnWidths.get(i);
            boolean hov = mx >= curBtnX && mx <= curBtnX + bw && my >= btnY && my <= btnY + btnH;
            UnifiedButton.render(context, curBtnX, btnY, bw, btnH, mb.text, mb.variant, effectiveAlpha, hov);
            lastButtonRects.add(new ButtonRect(curBtnX, btnY, bw, btnH, mb));
            curBtnX += bw + gap;
        }
    }
}

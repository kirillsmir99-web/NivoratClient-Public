package activity.client.gui.custom;

import activity.client.gui.custom.api.drags.Position;
import activity.client.gui.custom.api.ui.UI;
import activity.client.gui.custom.api.ui.theme.ClientAccent;
import activity.client.gui.custom.utils.render.fonts.Fonts;
import activity.client.gui.custom.utils.render.fonts.NvIcons;
import activity.client.gui.custom.utils.render.render2d.Render2D;
import activity.client.gui.custom.utils.render.render2d.glow.BuiltGlow;
import activity.client.gui.sound.SoundManager;
import dev.nivorat.arc.ArcCalibrationState;
import dev.nivorat.arc.ArcMotionProfile;
import dev.nivorat.arc.ArcMotorCalibrationService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.util.List;

public final class AutoCartCalibrationTopPanel {
    private static float currentSlideY = -70f;
    private static long lastRenderNs = 0L;
    private static boolean confirmReset = false;
    private static boolean manuallyOpened = false;

    private AutoCartCalibrationTopPanel() {}

    public static void open() {
        manuallyOpened = true;
    }

    public static void close() {
        manuallyOpened = false;
        confirmReset = false;
    }

    public static void toggle() {
        if (manuallyOpened) {
            close();
        } else {
            open();
        }
    }

    public static boolean isOpened() {
        return manuallyOpened;
    }

    public static void render(DrawContext context, int mouseX, int mouseY) {
        if (context == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;

        boolean inGui = mc.currentScreen == UI.INSTANCE;
        boolean shouldShow = manuallyOpened && inGui;
        ArcMotionProfile profile = ArcMotionProfile.getInstance();
        boolean session = ArcMotorCalibrationService.hasSession();

        long now = System.nanoTime();
        float dt = lastRenderNs > 0L ? (float) ((now - lastRenderNs) / 1_000_000_000.0) : 0.016f;
        lastRenderNs = now;

        float width = 380f;
        float height = 50f;
        float x;
        float targetY;

        if (inGui) {
            float px = UI.panelX();
            float py = UI.panelY();
            float pw = UI.panelW();
            width = Math.min(380f, pw);
            x = px + pw - width;
            if (py >= height + 6f) {
                targetY = shouldShow ? (py - height - 3f) : (py - height - 25f);
            } else {
                targetY = shouldShow ? (py + 4f) : (py - height - 15f);
            }
        } else {
            x = (Position.screenWidth() - width) * 0.5f;
            targetY = shouldShow ? 8f : -70f;
        }

        currentSlideY += (targetY - currentSlideY) * MathHelper.clamp(dt * 12f, 0.05f, 1.0f);

        if (!shouldShow && Math.abs(currentSlideY - targetY) < 1.0f) {
            return;
        }

        float y = currentSlideY;

        ArcCalibrationState state = profile.getState();
        int mastery = profile.getMasteryPercent();
        float confidence = profile.getConfidenceScore();
        long remainingMs = profile.getCalibrationRemainingTimeMs();
        long totalSec = (remainingMs + 999L) / 1000L;
        String timerStr = String.format("%02d:%02d", totalSec / 60L, totalSec % 60L);
        int currentDurationMins = ArcMotorCalibrationService.getTargetDurationMinutes();

        try (var frame = UnifiedHudRender.beginNative(context)) {
            CustomRender.panel(context, x, y, width, height, 6, 1);
            Render2D.glow(new BuiltGlow(x, y, width, height, new float[]{6f, 6f, 6f, 6f}, ClientAccent.accent(255), 0.22f, 4f, 1f));
            Render2D.outline(x, y, width, height, 6f, 0.7f, ClientAccent.accent(175));

            float badgeW = Fonts.MONTSERRAT_MEDIUM.width(state.getTitle(), 7f) + 12f;
            Render2D.rect(x + 10f, y + 8f, badgeW, 14f, 4f, (0x33 << 24) | (state.getColorRgba() & 0x00FFFFFF));
            Fonts.MONTSERRAT_MEDIUM.draw(state.getTitle(), x + 16f, y + 11.5f, 7f, state.getColorRgba());

            Fonts.MONTSERRAT_MEDIUM.draw("ОСВОЕНИЕ:", x + 16f + badgeW + 8f, y + 11.5f, 7.5f, 0xffd5dbe5);
            String masteryText = mastery + "%";
            Fonts.MONTSERRAT_MEDIUM.draw(masteryText, x + 16f + badgeW + 68f, y + 11.5f, 7.5f, ClientAccent.accentBright(255));

            float pillY = y + 8f;
            renderDurationPill(x + 172f, pillY, 30f, 14f, "1 мин", currentDurationMins == 1, !session, mouseX, mouseY);
            renderDurationPill(x + 205f, pillY, 30f, 14f, "2 мин", currentDurationMins == 2, !session, mouseX, mouseY);
            renderDurationPill(x + 238f, pillY, 30f, 14f, "5 мин", currentDurationMins == 5, !session, mouseX, mouseY);

            if (session) {
                Fonts.NV.msdf(NvIcons.ANIMATION, x + width - 110f, y + 11f, 8f, 0xff8fa0b5);
                Fonts.MONTSERRAT_MEDIUM.draw(timerStr, x + width - 98f, y + 11.5f, 7.5f, 0xffffffff);
            } else if (profile.isCalibrated()) {
                String confText = Math.round(confidence * 100f) + "%";
                Fonts.MONTSERRAT_MEDIUM.draw(confText, x + width - 70f, y + 11.5f, 7.5f, 0xff8fa0b5);
            }

            renderCloseButton(x + width - 20f, y + 8f, 14f, 14f, mouseX, mouseY);

            Render2D.rect(x + 10f, y + 25f, width - 20f, 3f, 1.5f, 0xff1e2330);
            Render2D.rect(x + 10f, y + 25f, (width - 20f) * (mastery / 100.0f), 3f, 1.5f, ClientAccent.accent(235));

            List<String> hints = profile.getMissingDataHints();
            String hintText;
            if (session) {
                hintText = hints.isEmpty() ? "Профиль моторики формируется" : hints.get(0);
            } else if (profile.isCalibrated()) {
                hintText = "Сэмплов: " + profile.getSampleCount();
            } else {
                hintText = currentDurationMins + "-мин калибровка";
            }
            Fonts.MONTSERRAT_MEDIUM.draw(hintText, x + 12f, y + 34f, 6.5f, 0xff8e9eb3);

            renderButton(x + width - 180f, y + 31f, 40f, 14f, "Старт", !session && mc.player != null, mouseX, mouseY);
            renderButton(x + width - 136f, y + 31f, 40f, 14f, profile.isCalibrationPaused() ? "Пуск" : "Пауза", session, mouseX, mouseY);
            renderButton(x + width - 92f, y + 31f, 40f, 14f, "Готово", session && profile.isReadyToFinish(), mouseX, mouseY);
            renderButton(x + width - 48f, y + 31f, 40f, 14f, confirmReset ? "Да?" : "Сброс", !session, mouseX, mouseY);
        }
    }

    private static void renderDurationPill(float bx, float by, float bw, float bh, String title, boolean selected, boolean enabled, int mouseX, int mouseY) {
        boolean hover = enabled && mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh;
        int bg = selected ? ClientAccent.accent(190) : (hover ? 0xcc2a3244 : 0x44212735);
        int textCol = selected ? 0xffffffff : (hover ? 0xffe2e8f0 : (enabled ? 0xff9ba7b8 : 0xff556073));
        Render2D.rect(bx, by, bw, bh, 3f, bg);
        float tw = Fonts.MONTSERRAT_MEDIUM.width(title, 6.5f);
        Fonts.MONTSERRAT_MEDIUM.draw(title, bx + (bw - tw) * 0.5f, by + 3.5f, 6.5f, textCol);
    }

    private static void renderCloseButton(float bx, float by, float bw, float bh, int mouseX, int mouseY) {
        boolean hover = mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh;
        int bg = hover ? 0xccdc2626 : 0x33212735;
        int textCol = hover ? 0xffffffff : 0xff8fa0b5;
        Render2D.rect(bx, by, bw, bh, 3f, bg);
        float tw = Fonts.MONTSERRAT_MEDIUM.width("×", 8f);
        Fonts.MONTSERRAT_MEDIUM.draw("×", bx + (bw - tw) * 0.5f, by + 2.5f, 8f, textCol);
    }

    private static void renderButton(float bx, float by, float bw, float bh, String title, boolean enabled, int mouseX, int mouseY) {
        boolean hover = mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh;
        int bg = enabled ? (hover ? ClientAccent.accent(160) : 0xcc212735) : 0x551a1e28;
        int textCol = enabled ? (hover ? 0xffffffff : 0xffc4cbd6) : 0xff556073;
        Render2D.rect(bx, by, bw, bh, 3f, bg);
        float tw = Fonts.MONTSERRAT_MEDIUM.width(title, 6.5f);
        Fonts.MONTSERRAT_MEDIUM.draw(title, bx + (bw - tw) * 0.5f, by + 3.5f, 6.5f, textCol);
    }

    public static boolean mouseClicked(Click click) {
        if (click == null || click.button() != 0 || !manuallyOpened) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return false;

        boolean inGui = mc.currentScreen == UI.INSTANCE;
        if (!inGui) return false;

        float width = 380f;
        float height = 50f;
        float px = UI.panelX();
        float pw = UI.panelW();
        width = Math.min(380f, pw);
        float x = px + pw - width;
        float y = currentSlideY;

        float mx = (float) click.x();
        float my = (float) click.y();

        if (mx < x || mx > x + width || my < y || my > y + height) {
            return false;
        }

        ArcMotionProfile profile = ArcMotionProfile.getInstance();
        boolean session = ArcMotorCalibrationService.hasSession();

        if (checkHit(x + width - 20f, y + 8f, 14f, 14f, mx, my)) {
            close();
            SoundManager.playClick();
            return true;
        }

        if (!session) {
            float pillY = y + 8f;
            if (checkHit(x + 172f, pillY, 30f, 14f, mx, my)) {
                ArcMotorCalibrationService.setTargetDurationMinutes(1);
                SoundManager.playClick();
                return true;
            }
            if (checkHit(x + 205f, pillY, 30f, 14f, mx, my)) {
                ArcMotorCalibrationService.setTargetDurationMinutes(2);
                SoundManager.playClick();
                return true;
            }
            if (checkHit(x + 238f, pillY, 30f, 14f, mx, my)) {
                ArcMotorCalibrationService.setTargetDurationMinutes(5);
                SoundManager.playClick();
                return true;
            }
        }

        if (checkHit(x + width - 180f, y + 31f, 40f, 14f, mx, my)) {
            if (!session && mc.player != null) {
                ArcMotorCalibrationService.start();
                SoundManager.playToggle(true);
                confirmReset = false;
                close();
                return true;
            }
        }

        if (checkHit(x + width - 136f, y + 31f, 40f, 14f, mx, my)) {
            if (session) {
                if (profile.isCalibrationPaused()) {
                    ArcMotorCalibrationService.resume();
                    SoundManager.playToggle(true);
                } else {
                    ArcMotorCalibrationService.pause();
                    SoundManager.playToggle(false);
                }
                confirmReset = false;
                return true;
            }
        }

        if (checkHit(x + width - 92f, y + 31f, 40f, 14f, mx, my)) {
            if (session && profile.isReadyToFinish()) {
                ArcMotorCalibrationService.finish();
                SoundManager.playSuccess();
                confirmReset = false;
                close();
                return true;
            }
        }

        if (checkHit(x + width - 48f, y + 31f, 40f, 14f, mx, my)) {
            if (!session) {
                if (!confirmReset) {
                    confirmReset = true;
                    SoundManager.playClick();
                } else {
                    ArcMotorCalibrationService.reset();
                    SoundManager.playDelete();
                    confirmReset = false;
                }
                return true;
            }
        }

        return true;
    }

    private static boolean checkHit(float bx, float by, float bw, float bh, float mx, float my) {
        return mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
    }
}

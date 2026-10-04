package activity.client.gui.custom;

import activity.client.gui.custom.api.drags.Position;
import activity.client.gui.custom.api.ui.UI;
import activity.client.gui.custom.api.ui.theme.ClientAccent;
import activity.client.gui.custom.utils.animations.Decelerate;
import activity.client.gui.custom.utils.animations.Direction;
import activity.client.gui.custom.utils.render.fonts.Fonts;
import activity.client.gui.custom.utils.render.fonts.NvIcons;
import activity.client.gui.custom.utils.render.render2d.Render2D;
import activity.client.gui.custom.utils.render.render2d.glow.BuiltGlow;
import activity.client.gui.custom.utils.sounds.Sounds;
import activity.client.gui.sound.SoundManager;
import dev.nivorat.arc.ArcCalibrationState;
import dev.nivorat.arc.ArcMotionProfile;
import dev.nivorat.arc.ArcMotorCalibrationService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public final class AutoCartCalibrationDrawer {
    private static final AutoCartCalibrationDrawer INSTANCE = new AutoCartCalibrationDrawer();
    private static final float COMPACT_HEIGHT = 44.0f;
    private static final float EXPANDED_HEIGHT = 108.0f;

    private final Decelerate dockAnim = (Decelerate) new Decelerate().setMs(220).setValue(1.0);
    private final Decelerate statsAnim = (Decelerate) new Decelerate().setMs(180).setValue(1.0);
    private boolean manuallyOpened = false;
    private boolean showStats = false;
    private boolean confirmReset = false;

    private AutoCartCalibrationDrawer() {
        this.dockAnim.setDirection(Direction.BACKWARDS);
        this.dockAnim.counter.setTime(System.currentTimeMillis() - 10000L);
        this.statsAnim.setDirection(Direction.BACKWARDS);
        this.statsAnim.counter.setTime(System.currentTimeMillis() - 10000L);
    }

    public static AutoCartCalibrationDrawer getInstance() {
        return INSTANCE;
    }

    public static void open() {
        INSTANCE.manuallyOpened = true;
        INSTANCE.confirmReset = false;
        INSTANCE.dockAnim.setDirection(Direction.FORWARDS);
        INSTANCE.dockAnim.counter.resetCounter();
        try {
            Sounds.play("module_settings_open");
        } catch (Throwable ignored) {}
    }

    public static void close() {
        if (!INSTANCE.manuallyOpened && INSTANCE.dockAnim.getDirection() == Direction.BACKWARDS) {
            return;
        }
        INSTANCE.manuallyOpened = false;
        INSTANCE.confirmReset = false;
        INSTANCE.dockAnim.setDirection(Direction.BACKWARDS);
        INSTANCE.dockAnim.counter.resetCounter();
        try {
            Sounds.play("module_settings_close");
        } catch (Throwable ignored) {}
    }

    public static void toggle() {
        if (isOpen()) {
            close();
        } else {
            open();
        }
    }

    public static boolean isOpen() {
        return INSTANCE.manuallyOpened || INSTANCE.dockAnim.getOutput().floatValue() > 0.005f;
    }

    public static float getVisualProtrusion() {
        float progress = INSTANCE.dockAnim.getOutput().floatValue();
        if (progress <= 0.005f && !INSTANCE.manuallyOpened) {
            return 0.0f;
        }
        float statsProgress = INSTANCE.statsAnim.getOutput().floatValue();
        float currentHeight = COMPACT_HEIGHT + (EXPANDED_HEIGHT - COMPACT_HEIGHT) * statsProgress;
        return currentHeight * progress;
    }

    public static void render(DrawContext context, int mouseX, int mouseY) {
        INSTANCE.renderInternal(context, mouseX, mouseY);
    }

    private void renderInternal(DrawContext context, int mouseX, int mouseY) {
        if (context == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;

        float progress = this.dockAnim.getOutput().floatValue();
        if (progress <= 0.005f && !this.manuallyOpened) {
            return;
        }

        float statsProgress = this.statsAnim.getOutput().floatValue();
        float currentHeight = COMPACT_HEIGHT + (EXPANDED_HEIGHT - COMPACT_HEIGHT) * statsProgress;

        float px = UI.panelX();
        float py = UI.panelY();
        float pw = UI.panelW();

        float width = pw;
        float x = px;

        float openY = Math.max(4.0f, py - currentHeight);
        float closedY = py;
        float y = closedY - (closedY - openY) * progress;
        float alpha = progress;

        ArcMotionProfile profile = ArcMotionProfile.getInstance();
        boolean session = ArcMotorCalibrationService.hasSession();
        ArcCalibrationState state = profile.getState();
        int mastery = profile.getMasteryPercent();
        float confidence = profile.getConfidenceScore();
        long remainingMs = profile.getCalibrationRemainingTimeMs();
        long totalSec = (remainingMs + 999L) / 1000L;
        String timerStr = String.format("%02d:%02d", totalSec / 60L, totalSec % 60L);
        int currentDurationMins = ArcMotorCalibrationService.getTargetDurationMinutes();

        float curMouseX = Position.mouseX();
        float curMouseY = Position.mouseY();

        Render2D.pushScissor(context, x - 4.0f, openY - 4.0f, width + 8.0f, currentHeight + 8.0f);

        try (var frame = UnifiedHudRender.beginNative(context)) {
            int accentRgb = ClientAccent.accentBright(255);

            Fonts.NV.msdf(NvIcons.ANIMATION, x + 9.0f, y + 8.0f, 9.0f, accentRgb);
            Fonts.MONTSERRAT_BOLD.draw("Калибровка AutoCart", x + 22.0f, y + 9.0f, 7.5f, 0xffffffff);

            float badgeW = Fonts.MONTSERRAT_MEDIUM.width(state.getTitle(), 6.0f) + 10.0f;
            float badgeX = x + 130.0f;
            float badgeY = y + 7.0f;
            Render2D.rect(badgeX, badgeY, badgeW, 12.0f, 3.0f, (0x33 << 24) | (state.getColorRgba() & 0x00FFFFFF));
            Render2D.outline(badgeX, badgeY, badgeW, 12.0f, 3.0f, 0.5f, (0x66 << 24) | (state.getColorRgba() & 0x00FFFFFF));
            Fonts.MONTSERRAT_MEDIUM.draw(state.getTitle(), badgeX + 5.0f, badgeY + 2.5f, 6.0f, state.getColorRgba());

            float telemX = badgeX + badgeW + 10.0f;
            String infoText = "Время: " + (session ? timerStr : "00:00") + "  •  Освоение: " + mastery + "%  •  Уверенность: " + Math.round(confidence * 100.0f) + "%";
            Fonts.MONTSERRAT_MEDIUM.draw(infoText, telemX, y + 9.0f, 6.0f, 0xff94a3b8);

            float rightEdge = x + width - 8.0f;
            renderCloseButton(rightEdge - 13.0f, y + 6.5f, 13.0f, 13.0f, curMouseX, curMouseY);

            float pillW = 26.0f;
            float pillH = 14.0f;
            float pillGap = 3.0f;
            float pillsStartX = x + 10.0f;
            renderDurationPill(pillsStartX, y + 23.0f, pillW, pillH, "1 мин", currentDurationMins == 1, !session, curMouseX, curMouseY);
            renderDurationPill(pillsStartX + (pillW + pillGap), y + 23.0f, pillW, pillH, "2 мин", currentDurationMins == 2, !session, curMouseX, curMouseY);
            renderDurationPill(pillsStartX + (pillW + pillGap) * 2.0f, y + 23.0f, pillW, pillH, "5 мин", currentDurationMins == 5, !session, curMouseX, curMouseY);

            float btnH = 15.0f;
            float btnY = y + 22.5f;
            float bStatsW = 62.0f;
            float bResetW = 42.0f;
            float bFinishW = 58.0f;
            float bStartW = 66.0f;

            float bStatsX = rightEdge - bStatsW;
            float bResetX = bStatsX - 5.0f - bResetW;
            float bFinishX = bResetX - 5.0f - bFinishW;
            float bStartX = bFinishX - 5.0f - bStartW;

            String startTitle = !session ? "Старт" : (profile.isCalibrationPaused() ? "Продолжить" : "Пауза");
            renderButton(bStartX, btnY, bStartW, btnH, startTitle, mc.player != null, true, curMouseX, curMouseY);
            renderButton(bFinishX, btnY, bFinishW, btnH, "Завершить", session && profile.isReadyToFinish(), false, curMouseX, curMouseY);
            renderButton(bResetX, btnY, bResetW, btnH, confirmReset ? "Точно" : "Сброс", !session, false, curMouseX, curMouseY);
            renderButton(bStatsX, btnY, bStatsW, btnH, "Статистика", true, showStats, curMouseX, curMouseY);

            if (statsProgress > 0.01f) {
                Render2D.rect(x + 10.0f, y + 42.5f, width - 20.0f, 0.6f, 0.3f, 0x22ffffff);

                Fonts.MONTSERRAT_MEDIUM.draw("ПРОГРЕСС ОБУЧЕНИЯ МОТОРИКИ:", x + 12.0f, y + 47.0f, 5.5f, 0xff94a3b8);
                String masteryText = mastery + "%";
                Fonts.MONTSERRAT_BOLD.draw(masteryText, x + 122.0f, y + 46.5f, 6.0f, accentRgb);

                float barY = y + 54.0f;
                float barW = width - 24.0f;
                Render2D.rect(x + 12.0f, barY, barW, 3.0f, 1.5f, 0xff1b212e);
                float fillW = barW * MathHelper.clamp(mastery / 100.0f, 0.0f, 1.0f);
                if (fillW > 0.0f) {
                    Render2D.rect(x + 12.0f, barY, fillW, 3.0f, 1.5f, accentRgb);
                }

                float cardGap = 5.0f;
                float cardW = (width - 24.0f - cardGap * 3.0f) / 4.0f;
                float cardH = 26.0f;
                float cardY = y + 61.0f;

                int sensPercent = mc.options != null ? (int) Math.round(mc.options.getMouseSensitivity().getValue() * 100.0) : 100;
                renderTelemetryCard(x + 12.0f, cardY, cardW, cardH, "УДАЧНЫЙ ВЗРЫВ", profile.getManualDetonationsCount() + " шт.", 0xfff59e0b);
                renderTelemetryCard(x + 12.0f + cardW + cardGap, cardY, cardW, cardH, "ВАША СЕНСА", sensPercent + "%", 0xff38bdf8);
                renderTelemetryCard(x + 12.0f + (cardW + cardGap) * 2.0f, cardY, cardW, cardH, "СЭМПЛЫ МОТОРИКИ", String.valueOf(profile.getSampleCount()), 0xffcbd5e1);
                renderTelemetryCard(x + 12.0f + (cardW + cardGap) * 3.0f, cardY, cardW, cardH, "ПЛАВНОСТЬ НАВОДКИ", Math.round(profile.getLearnedCameraSmoothness()) + " мс", accentRgb);

                String statusHint = profile.isCalibrated() ? ("Адаптивная модель откалибрована • точность " + Math.round(confidence * 100.0f) + "%") : "Идёт адаптивная запись движений при установке и взрыве";
                Fonts.MONTSERRAT_MEDIUM.draw(statusHint, x + 14.0f, y + 93.0f, 5.5f, 0xff8fa0b5);
            }
        }

        Render2D.popScissor(context);
    }

    private void renderTelemetryCard(float cx, float cy, float cw, float ch, String label, String value, int valColor) {
        Render2D.rect(cx, cy, cw, ch, 3.5f, 0x331b212f);
        Render2D.outline(cx, cy, cw, ch, 3.5f, 0.5f, 0x22ffffff);
        Fonts.MONTSERRAT_MEDIUM.draw(label, cx + 5.0f, cy + 4.5f, 5.0f, 0xff7b8b9f);
        Fonts.MONTSERRAT_BOLD.draw(value, cx + 5.0f, cy + 14.0f, 6.5f, valColor);
    }

    private void renderDurationPill(float bx, float by, float bw, float bh, String title, boolean selected, boolean enabled, float mouseX, float mouseY) {
        boolean hover = enabled && mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh;
        int bg = selected ? ClientAccent.accent(200) : (hover ? 0xcc2a3244 : 0x33212735);
        int textCol = selected ? 0xffffffff : (hover ? 0xffe2e8f0 : (enabled ? 0xff9ba7b8 : 0xff556073));
        Render2D.rect(bx, by, bw, bh, 3.0f, bg);
        if (selected) {
            Render2D.outline(bx, by, bw, bh, 3.0f, 0.6f, ClientAccent.accentBright(255));
        }
        float tw = Fonts.MONTSERRAT_MEDIUM.width(title, 5.5f);
        Fonts.MONTSERRAT_MEDIUM.draw(title, bx + (bw - tw) * 0.5f, by + 3.5f, 5.5f, textCol);
    }

    private void renderCloseButton(float bx, float by, float bw, float bh, float mouseX, float mouseY) {
        boolean hover = mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh;
        int bg = hover ? 0xccdc2626 : 0x33212735;
        int textCol = hover ? 0xffffffff : 0xff8fa0b5;
        Render2D.rect(bx, by, bw, bh, 3.0f, bg);
        float tw = Fonts.MONTSERRAT_MEDIUM.width("×", 7.5f);
        Fonts.MONTSERRAT_MEDIUM.draw("×", bx + (bw - tw) * 0.5f, by + 2.0f, 7.5f, textCol);
    }

    private void renderButton(float bx, float by, float bw, float bh, String title, boolean enabled, boolean primary, float mouseX, float mouseY) {
        boolean hover = enabled && mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh;
        int bg;
        if (!enabled) {
            bg = 0x441a1e28;
        } else if (primary) {
            bg = hover ? ClientAccent.accentBright(240) : ClientAccent.accent(200);
        } else {
            bg = hover ? 0xcc2b3548 : 0x881f2533;
        }
        int textCol = enabled ? (hover ? 0xffffffff : 0xffd1d5db) : 0xff556073;
        Render2D.rect(bx, by, bw, bh, 3.5f, bg);
        Render2D.outline(bx, by, bw, bh, 3.5f, 0.5f, enabled ? (hover ? 0x44ffffff : 0x22ffffff) : 0x11ffffff);
        float tw = Fonts.MONTSERRAT_BOLD.width(title, 5.5f);
        Fonts.MONTSERRAT_BOLD.draw(title, bx + (bw - tw) * 0.5f, by + 4.5f, 5.5f, textCol);
    }

    public static boolean mouseClicked(Click click) {
        return INSTANCE.handleMouseClicked(click);
    }

    private boolean handleMouseClicked(Click click) {
        if (click == null || click.button() != 0 || !isOpen()) {
            return false;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return false;

        float statsProgress = this.statsAnim.getOutput().floatValue();
        float currentHeight = COMPACT_HEIGHT + (EXPANDED_HEIGHT - COMPACT_HEIGHT) * statsProgress;

        float px = UI.panelX();
        float py = UI.panelY();
        float pw = UI.panelW();

        float width = pw;
        float x = px;
        float progress = this.dockAnim.getOutput().floatValue();
        float openY = Math.max(4.0f, py - currentHeight);
        float closedY = py;
        float y = closedY - (closedY - openY) * progress;

        float mx = Position.mouseX();
        float my = Position.mouseY();

        if (mx < x || mx > x + width || my < y || my > y + currentHeight) {
            return false;
        }

        float rightEdge = x + width - 8.0f;
        if (checkHit(rightEdge - 13.0f, y + 6.5f, 13.0f, 13.0f, mx, my)) {
            close();
            return true;
        }

        boolean session = ArcMotorCalibrationService.hasSession();
        ArcMotionProfile profile = ArcMotionProfile.getInstance();

        float pillW = 26.0f;
        float pillH = 14.0f;
        float pillGap = 3.0f;
        float pillsStartX = x + 10.0f;

        if (!session) {
            if (checkHit(pillsStartX, y + 23.0f, pillW, pillH, mx, my)) {
                ArcMotorCalibrationService.setTargetDurationMinutes(1);
                SoundManager.playClick();
                return true;
            }
            if (checkHit(pillsStartX + (pillW + pillGap), y + 23.0f, pillW, pillH, mx, my)) {
                ArcMotorCalibrationService.setTargetDurationMinutes(2);
                SoundManager.playClick();
                return true;
            }
            if (checkHit(pillsStartX + (pillW + pillGap) * 2.0f, y + 23.0f, pillW, pillH, mx, my)) {
                ArcMotorCalibrationService.setTargetDurationMinutes(5);
                SoundManager.playClick();
                return true;
            }
        }

        float btnH = 15.0f;
        float btnY = y + 22.5f;
        float bStatsW = 62.0f;
        float bResetW = 42.0f;
        float bFinishW = 58.0f;
        float bStartW = 66.0f;

        float bStatsX = rightEdge - bStatsW;
        float bResetX = bStatsX - 5.0f - bResetW;
        float bFinishX = bResetX - 5.0f - bFinishW;
        float bStartX = bFinishX - 5.0f - bStartW;

        if (checkHit(bStartX, btnY, bStartW, btnH, mx, my)) {
            if (!session) {
                if (mc.player != null) {
                    ArcMotorCalibrationService.start();
                    SoundManager.playToggle(true);
                    this.confirmReset = false;
                    mc.setScreen(null);
                }
            } else {
                if (profile.isCalibrationPaused()) {
                    ArcMotorCalibrationService.resume();
                    SoundManager.playToggle(true);
                    mc.setScreen(null);
                } else {
                    ArcMotorCalibrationService.pause();
                    SoundManager.playToggle(false);
                }
            }
            return true;
        }

        if (checkHit(bFinishX, btnY, bFinishW, btnH, mx, my)) {
            if (session && profile.isReadyToFinish()) {
                ArcMotorCalibrationService.finish();
                SoundManager.playSuccess();
                this.confirmReset = false;
                close();
            }
            return true;
        }

        if (checkHit(bResetX, btnY, bResetW, btnH, mx, my)) {
            if (!session) {
                if (!this.confirmReset) {
                    this.confirmReset = true;
                    SoundManager.playClick();
                } else {
                    ArcMotorCalibrationService.reset();
                    SoundManager.playDelete();
                    this.confirmReset = false;
                }
            }
            return true;
        }

        if (checkHit(bStatsX, btnY, bStatsW, btnH, mx, my)) {
            this.showStats = !this.showStats;
            this.statsAnim.setDirection(this.showStats ? Direction.FORWARDS : Direction.BACKWARDS);
            this.statsAnim.counter.resetCounter();
            SoundManager.playClick();
            return true;
        }

        return true;
    }

    public static boolean mouseDragged(Click click, double deltaX, double deltaY) {
        return false;
    }

    public static boolean mouseReleased(Click click) {
        return false;
    }

    public static boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        if (!isOpen()) return false;
        float px = UI.panelX();
        float py = UI.panelY();
        float pw = UI.panelW();
        float statsProgress = INSTANCE.statsAnim.getOutput().floatValue();
        float currentHeight = COMPACT_HEIGHT + (EXPANDED_HEIGHT - COMPACT_HEIGHT) * statsProgress;
        float progress = INSTANCE.dockAnim.getOutput().floatValue();
        float openY = Math.max(4.0f, py - currentHeight);
        float closedY = py;
        float y = closedY - (closedY - openY) * progress;
        return mouseX >= px && mouseX <= px + pw && mouseY >= y && mouseY <= y + currentHeight;
    }

    public static boolean keyPressed(int keyCode) {
        if (isOpen() && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return false;
    }

    private static boolean checkHit(float bx, float by, float bw, float bh, float mx, float my) {
        return mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
    }
}

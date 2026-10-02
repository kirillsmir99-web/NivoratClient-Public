package dev.nivorat.arc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public final class ArcMotorCalibrationService {
    private static volatile boolean active = false;

    static {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> pause());
    }

    private ArcMotorCalibrationService() {}

    public static void start() {
        if (hasSession()) return;
        active = true;
        ArcMotionProfile.getInstance().startCalibration();
        try {
            activity.client.module.api.ModuleEventDispatcher.updateActiveModules();
        } catch (Throwable ignored) {}
        try {
            int mins = ArcMotionProfile.getInstance().getTargetCalibrationDurationMinutes();
            activity.client.gui.overlay.ClientNotification.show(Text.literal("Калибровка моторики запущена (" + mins + " мин): используйте лук и вагонетки"));
        } catch (Throwable ignored) {}
    }

    public static void stop() {
        active = false;
        ArcMotionProfile.getInstance().cancelCalibration();
        try {
            activity.client.module.api.ModuleEventDispatcher.updateActiveModules();
        } catch (Throwable ignored) {}
    }

    public static boolean hasSession() {
        return active && ArcMotionProfile.getInstance().hasCalibrationSession();
    }

    public static void pause() {
        ArcMotionProfile.getInstance().pauseCalibration();
    }

    public static void resume() {
        ArcMotionProfile.getInstance().resumeCalibration();
    }

    public static void finish() {
        if (!hasSession()) return;
        ArcMotionProfile.getInstance().finishCalibration();
        checkCompletion();
    }

    public static void reset() {
        stop();
        ArcMotionProfile.getInstance().resetCalibration();
    }

    public static boolean isActive() {
        return active && ArcMotionProfile.getInstance().isCalibrating();
    }

    public static boolean isPaused() {
        return active && ArcMotionProfile.getInstance().isCalibrationPaused();
    }

    public static ArcCalibrationState getState() {
        return ArcMotionProfile.getInstance().getState();
    }

    public static int getProgress() {
        return ArcMotionProfile.getInstance().getCalibrationProgressPercent();
    }

    public static int getMastery() {
        return ArcMotionProfile.getInstance().getMasteryPercent();
    }

    public static float getConfidence() {
        return ArcMotionProfile.getInstance().getConfidenceScore();
    }

    public static List<String> getMissingHints() {
        return ArcMotionProfile.getInstance().getMissingDataHints();
    }

    public static long getRemainingTimeMs() {
        return ArcMotionProfile.getInstance().getCalibrationRemainingTimeMs();
    }

    public static int getManualDetonationsCount() {
        return ArcMotionProfile.getInstance().getManualDetonationsCount();
    }

    public static boolean isCalibrated() {
        return ArcMotionProfile.getInstance().isCalibrated();
    }

    public static void onBowReleased(int drawTicks) {
        if (isActive()) {
            ArcMotionProfile.getInstance().recordBowRelease(drawTicks);
        }
    }

    public static void onRailPlaced(BlockPos pos) {
        if (isActive()) {
            ArcMotionProfile.getInstance().recordRailPlacement(pos);
        }
    }

    public static void onCartPlaced(BlockPos pos) {
        if (isActive()) {
            ArcMotionProfile.getInstance().recordCartPlacement(pos);
        }
    }

    public static void onExplosion(double x, double y, double z) {
        if (isActive()) {
            ArcMotionProfile.getInstance().recordExplosion(x, y, z);
        }
    }

    public static void checkCompletion() {
        ArcMotionProfile.getInstance().flushCheckpoint();
        ArcMotionProfile.getInstance().isCalibrating();
        if (active && !ArcMotionProfile.getInstance().hasCalibrationSession()) {
            active = false;
            try {
                activity.client.module.api.ModuleEventDispatcher.updateActiveModules();
            } catch (Throwable ignored) {}
            if (!ArcMotionProfile.getInstance().didLastCalibrationSucceed()) {
                try {
                    activity.client.gui.overlay.ClientNotification.show(Text.literal("Недостаточно данных: повторите калибровку с движением камеры и установкой вагонеток."));
                } catch (Throwable ignored) {}
                return;
            }
            try {
                activity.client.module.api.ModuleEventDispatcher.updateActiveModules();
            } catch (Throwable ignored) {}
            try {
                activity.client.module.api.IModule mod = activity.client.module.api.ModuleRegistry.get("auto_cart");
                if (mod instanceof activity.client.module.impl.defense.OcclusionCacheModule cartMod) {
                    cartMod.activateLearnedPreset();
                } else {
                    activity.client.config.ActivityConfig c = activity.client.config.ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.autoCartMode = "beta_neural";
                        c.autoCartPreset = "learned";
                        activity.client.config.ActivityConfigManager.markDirty();
                    }
                    MorrowConfig.applyPreset(MorrowConfig.PRESET_LEARNED);
                }
            } catch (Throwable ignored) {}

            int mastery = ArcMotionProfile.getInstance().getMasteryPercent();
            int confidence = Math.round(ArcMotionProfile.getInstance().getConfidenceScore() * 100f);
            String msg = String.format("Калибровка завершена • Профиль сохранён • Освоение: %d%% (Уверенность: %d%%)", mastery, confidence);
            Text textMsg = Text.literal(msg);
            try {
                activity.client.gui.overlay.ClientNotification.show(textMsg, 15000L);
                activity.client.gui.sound.SoundManager.playSuccess();
            } catch (Throwable ignored) {}
        }
    }

    public static int getTargetDurationMinutes() {
        return ArcMotionProfile.getInstance().getTargetCalibrationDurationMinutes();
    }

    public static void setTargetDurationMinutes(int minutes) {
        ArcMotionProfile.getInstance().setTargetCalibrationDurationMinutes(minutes);
    }
}

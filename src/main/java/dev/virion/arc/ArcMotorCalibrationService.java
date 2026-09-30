package dev.virion.arc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class ArcMotorCalibrationService {
    private static volatile boolean active = false;

    private ArcMotorCalibrationService() {}

    public static void start() {
        active = true;
        ArcNeuralMotorProfile.getInstance().startCalibration();
        try {
            activity.client.module.api.ModuleEventDispatcher.updateActiveModules();
        } catch (Throwable ignored) {}
        try {
            activity.client.gui.overlay.ClientNotification.show(Text.literal("Калибровка моторики запущена (5 мин): используйте лук и вагонетки"));
        } catch (Throwable ignored) {}
    }

    public static void stop() {
        active = false;
        ArcNeuralMotorProfile.getInstance().cancelCalibration();
        try {
            activity.client.module.api.ModuleEventDispatcher.updateActiveModules();
        } catch (Throwable ignored) {}
    }

    public static boolean isActive() {
        return active && ArcNeuralMotorProfile.getInstance().isCalibrating();
    }

    public static int getProgress() {
        return ArcNeuralMotorProfile.getInstance().getCalibrationProgressPercent();
    }

    public static long getRemainingTimeMs() {
        return ArcNeuralMotorProfile.getInstance().getCalibrationRemainingTimeMs();
    }

    public static int getManualDetonationsCount() {
        return ArcNeuralMotorProfile.getInstance().getManualDetonationsCount();
    }

    public static boolean isCalibrated() {
        return ArcNeuralMotorProfile.getInstance().isCalibrated();
    }

    public static void onBowReleased(int drawTicks) {
        if (isActive()) {
            ArcNeuralMotorProfile.getInstance().recordBowRelease(drawTicks);
        }
    }

    public static void onRailPlaced(BlockPos pos) {
        if (isActive()) {
            ArcNeuralMotorProfile.getInstance().recordRailPlacement(pos);
        }
    }

    public static void onCartPlaced(BlockPos pos) {
        if (isActive()) {
            ArcNeuralMotorProfile.getInstance().recordCartPlacement(pos);
        }
    }

    public static void onExplosion(double x, double y, double z) {
        if (isActive()) {
            ArcNeuralMotorProfile.getInstance().recordExplosion(x, y, z);
        }
    }

    public static void checkCompletion() {
        if (active && !ArcNeuralMotorProfile.getInstance().isCalibrating()) {
            active = false;
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
                        c.autoCartPreset = "learned";
                        activity.client.config.ActivityConfigManager.markDirty();
                    }
                    MorrowConfig.applyPreset(MorrowConfig.PRESET_LEARNED);
                }
            } catch (Throwable ignored) {}

            int actions = ArcNeuralMotorProfile.getInstance().getTotalActionsCount();
            String msg = String.format("Калибровка на 5 минут завершена! Профиль моторики усвоен (Зафиксировано действий: %d). Активирован пресет 'Обученный'.", actions);
            Text textMsg = Text.literal(msg);
            try {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null && mc.player != null) {
                    mc.player.sendMessage(textMsg, false);
                    mc.player.sendMessage(textMsg, true);
                }
                activity.client.gui.overlay.ClientNotification.show(textMsg);
                activity.client.gui.sound.SoundManager.playSuccess();
            } catch (Throwable ignored) {}
        }
    }
}

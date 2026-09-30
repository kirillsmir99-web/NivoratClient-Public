package dev.virion.arc;

import net.minecraft.text.Text;

public final class ArcMotorCalibrationService {
    private static volatile boolean active = false;

    private ArcMotorCalibrationService() {}

    public static void start() {
        active = true;
        ArcNeuralMotorProfile.getInstance().startCalibration();
        try {
            activity.client.gui.overlay.ClientNotification.show(Text.literal("Калибровка моторики запущена: вращайте прицел"));
        } catch (Throwable ignored) {}
    }

    public static void stop() {
        active = false;
        ArcNeuralMotorProfile.getInstance().cancelCalibration();
    }

    public static boolean isActive() {
        return active && ArcNeuralMotorProfile.getInstance().isCalibrating();
    }

    public static int getProgress() {
        return ArcNeuralMotorProfile.getInstance().getCalibrationProgressPercent();
    }

    public static boolean isCalibrated() {
        return ArcNeuralMotorProfile.getInstance().isCalibrated();
    }

    public static void checkCompletion() {
        if (active && !ArcNeuralMotorProfile.getInstance().isCalibrating()) {
            active = false;
            try {
                activity.client.gui.overlay.ClientNotification.show(Text.literal("Калибровка завершена: моторика усвоена"));
            } catch (Throwable ignored) {}
        }
    }
}

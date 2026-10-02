package dev.nivorat.arc;

import net.minecraft.client.MinecraftClient;

public final class ArcInputRecorder {
    private static final ArcInputRecorder INSTANCE = new ArcInputRecorder();

    private boolean pausedByScreen = false;

    private ArcInputRecorder() {}

    public static ArcInputRecorder getInstance() {
        return INSTANCE;
    }

    public void updateScreenAndFocusState(MinecraftClient client) {
        if (!ArcMotorCalibrationService.hasSession()) {
            pausedByScreen = false;
            return;
        }

        boolean screenOpen = client == null || client.currentScreen != null || client.world == null
                || client.player == null || !client.player.isAlive()
                || !client.isWindowFocused();

        if (screenOpen) {
            if (ArcMotorCalibrationService.isActive()) {
                pausedByScreen = true;
                ArcMotorCalibrationService.pause();
            }
        } else {
            if (pausedByScreen && ArcMotionProfile.getInstance().isCalibrationPaused()) {
                pausedByScreen = false;
                ArcMotorCalibrationService.resume();
            }
        }
    }

    public void reset() {
        pausedByScreen = false;
    }
}

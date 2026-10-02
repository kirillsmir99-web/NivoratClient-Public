package activity.visualsmoke;

import activity.client.capitulation.CapitulationManager;
import activity.client.gui.custom.VisualSettingsStore;
import activity.client.gui.custom.api.drags.Position;
import activity.client.gui.custom.api.modules.Category;
import activity.client.gui.custom.api.ui.UI;
import activity.client.integration.NivoratEcosystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.MouseInput;
import net.fabricmc.pack.api.CombatLockManager;
import net.fabricmc.pack.api.TickBoundScheduler;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

final class EcosystemSmoke {
    private static int ticks;
    private static Map<String, Object> sounds;
    private static final AtomicBoolean pendingAction = new AtomicBoolean();

    static void tick(MinecraftClient client) throws Exception {
        ticks++;
        if (ticks == 1) {
            activity.client.gui.custom.api.localization.LocalizationManager.setLanguage("ru");
            client.setScreen(UI.INSTANCE);
            UI.INSTANCE.selectCategoryFromWorkspace(Category.DISPLAY);
            require(NivoratEcosystem.sections().isEmpty(), "Standalone client unexpectedly hid local settings");
            sounds = VisualSettingsStore.snapshot("sounds");
            VisualSettingsStore.apply("sounds", Map.of("ClientSounds.volume", 0.35f, "ClientSounds.toggleSoundChoice", "NV · Стекло"));
            VisualSettingsStore.save();
            var path = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("nc-visual.json");
            var data = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(path)).getAsJsonObject();
            require(Math.abs(data.get("ClientSounds.volume").getAsFloat() - 0.35f) < 0.001f, "Private sound volume not persisted");
            require(data.get("ClientSounds.toggleSoundChoice").getAsString().equals("NV · Стекло"), "Private sound selection not persisted");
            VisualSettingsStore.apply("sounds", sounds);
            VisualSettingsStore.save();
            try {
                java.nio.file.Files.createDirectories(java.nio.file.Path.of("build"));
                java.nio.file.Files.writeString(java.nio.file.Path.of("build/capitulation_smoke_pid.txt"), String.valueOf(ProcessHandle.current().pid()));
            } catch (Throwable ignored) {}
        }
        if (ticks == 30) capture(client, "ecosystem-settings-ru.png");
        if (ticks == 35) {
            try {
                java.nio.file.Files.writeString(java.nio.file.Path.of("build/smoke_state_before.txt"), "READY");
            } catch (Throwable ignored) {}
        }
        if (ticks == 40) press(client);
        if (ticks == 44) UI.INSTANCE.mouseReleased(new Click(0, 0, new MouseInput(0, 0)));
        if (ticks == 55) {
            require(!CapitulationManager.isCapitulated(), "Short click deactivated the client");
            dev.nivorat.arc.ArcMotorCalibrationService.start();
            TickBoundScheduler.runAfterTicks(200, () -> pendingAction.set(true));
            CombatLockManager.setLock("smoke.pending", true);
            press(client);
        }
        if (ticks == 70) capture(client, "ecosystem-hold-progress.png");
        if (ticks == 120) {
            require(CapitulationManager.isCapitulated(), "Full hold did not deactivate: focus=" + client.isWindowFocused());
            require(!pendingAction.get(), "Pending action ran after stop");
            require(!CombatLockManager.isLocked(), "Stop retained combat lock");
            require(!dev.nivorat.arc.ArcMotorCalibrationService.hasSession(), "Stop retained calibration");
            require(activity.client.module.api.ModuleEventDispatcher.getActiveTickModules().length == 0, "Stop retained active tick modules");
            require(!client.options.useKey.isPressed(), "Stop retained item use");
            require(client.currentScreen == null, "Stop retained menu");
            try {
                java.nio.file.Files.writeString(java.nio.file.Path.of("build/smoke_state_after.txt"), "READY");
            } catch (Throwable ignored) {}
        }
        if (ticks >= 150) {
            if (java.nio.file.Files.exists(java.nio.file.Path.of("build/smoke_audit_done.txt")) || ticks > 1200) {
                VisualSmoke.writeResult(true, "Local ecosystem entrypoint, private sound persistence, short-click cancellation and full-hold deactivation");
                client.scheduleStop();
            }
        }
    }

    private static void press(MinecraftClient client) throws Exception {
        float x = UI.panelX() + UI.contentXOff() + (UI.PANEL_W - UI.contentInset()) / 2;
        float y = UI.panelY() + UI.CONTENT_Y_OFFSET + UI.CONTENT_HEIGHT - 18;
        var window = client.getWindow();
        float scale = activity.client.gui.custom.utils.render.render2d.Render2DCoordinateSpace.designGuiScale();
        double nativeX = x * scale * window.getWidth() / window.getFramebufferWidth();
        double nativeY = y * scale * window.getHeight() / window.getFramebufferHeight();
        org.lwjgl.glfw.GLFW.glfwSetCursorPos(window.getHandle(), nativeX, nativeY);
        for (String axis : java.util.List.of("x", "y")) {
            var field = client.mouse.getClass().getDeclaredField(axis);
            field.setAccessible(true);
            field.setDouble(client.mouse, axis.equals("x") ? nativeX : nativeY);
        }
        require(UI.INSTANCE.mouseClicked(new Click(Position.mouseX(), Position.mouseY(), new MouseInput(0, 0)), false), "Stop button did not consume press");
    }

    private static void capture(MinecraftClient client, String name) {
        var directory = new java.io.File(System.getProperty("visual.smoke.output"));
        directory.mkdirs();
        net.minecraft.client.util.ScreenshotRecorder.saveScreenshot(directory, name, client.getFramebuffer(), 1, text -> {});
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}

package activity.client;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.ActivityScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivityClient implements ClientModInitializer {
    public static final String MOD_ID = "activity";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static boolean menuKeyDown = false;

    @Override
    public void onInitializeClient() {
        LOGGER.debug("[Activity] Initializing Activity client mod...");

        // Load persisted configuration from disk (or initialize clean defaults)
        ActivityConfigManager.load();
        activity.client.module.api.ModuleRegistry.initEvents();
        activity.client.gui.font.FontManager.init();
        activity.client.gui.sound.ActivitySoundEvents.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
                menuKeyDown = false;
                return;
            }

            if (client == null || client.player == null) {
                menuKeyDown = false;
                return;
            }

            Window window = client.getWindow();
            if (window == null || window.getHandle() == 0L) {
                menuKeyDown = false;
                return;
            }

            ActivityConfig config = ActivityConfigManager.getConfig();
            if (config == null || config.menuKeybind == null || config.menuKeybind.isUnbound()) {
                menuKeyDown = false;
                return;
            }

            boolean ctrl = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                    || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
            boolean shift = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                    || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
            boolean alt = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_ALT)
                    || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_ALT);

            boolean isDown = config.menuKeybind.matchesWindow(window, ctrl, shift, alt);

            // While any screen is open, keep track of key state so it doesn't trigger on close
            if (client.currentScreen != null) {
                menuKeyDown = isDown;
                return;
            }

            if (isDown && !menuKeyDown) {
                menuKeyDown = true;
                try {
                    client.setScreen(new ActivityScreen());
                } catch (Throwable ignored) {
                }
            } else if (!isDown && menuKeyDown) {
                menuKeyDown = false;
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
                return;
            }
            if (ActivityConfigManager.isDirty()) {
                LOGGER.debug("[Activity] Flushing dirty configuration on client shutdown...");
                ActivityConfigManager.save();
            }
        });

        LOGGER.debug("[Activity] Activity client loaded successfully. Masked menu keybind active.");
    }

    /**
     * Suppresses menu key trigger so that closing the screen via hotkey does not re-open it on tick.
     */
    public static void suppressMenuKey() {
        menuKeyDown = true;
    }
}

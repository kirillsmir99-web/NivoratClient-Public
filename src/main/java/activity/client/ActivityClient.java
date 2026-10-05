package activity.client;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.ActivityScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivityClient implements ClientModInitializer {
    public static final String MOD_ID = "activity";
    public static final Logger LOGGER = LoggerFactory.getLogger("MemoryLeakFix");

    public static KeyBinding openCooldownHudKey;
    public static KeyBinding openMenuKey;
    private static boolean menuKeyDown = false;

    @Override
    public void onInitializeClient() {

        ActivityConfigManager.load();
        activity.client.module.api.ModuleRegistry.initEvents();
        activity.client.gui.font.FontManager.init();
        activity.client.gui.sound.ActivitySoundEvents.register();
        activity.client.gui.custom.utils.sounds.SoundManager.init();
        activity.client.integration.ModMenuStealth.init();
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> activity.client.integration.NivoratEcosystem.discover());



        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            activity.client.module.service.CooldownTrackerService.tick(client);

            if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
                menuKeyDown = false;
                return;
            }

            if (client == null || client.player == null) {
                activity.client.module.service.CooldownTrackerService.clear();
                menuKeyDown = false;
                return;
            }

            Window window = client.getWindow();
            if (window == null || window.getHandle() == 0L) {
                menuKeyDown = false;
                return;
            }

            ActivityConfig config = ActivityConfigManager.getConfig();
            int menuKeyCode = (config != null && config.menuKeybind != null && !config.menuKeybind.isUnbound())
                    ? config.menuKeybind.getKeyCode()
                    : GLFW.GLFW_KEY_RIGHT_SHIFT;

            boolean ctrl = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                    || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
            boolean shift = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                    || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
            boolean alt = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_ALT)
                    || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_ALT);

            boolean configMatches = (config != null && config.menuKeybind != null && !config.menuKeybind.isUnbound())
                    && config.menuKeybind.matchesWindow(window, ctrl, shift, alt);

            boolean rawPressed = false;
            if (config != null && config.menuKeybind != null && config.menuKeybind.isMouseButton()) {
                int btn = config.menuKeybind.getMouseButton();
                rawPressed = btn >= 0 && btn <= GLFW.GLFW_MOUSE_BUTTON_LAST && GLFW.glfwGetMouseButton(window.getHandle(), btn) == GLFW.GLFW_PRESS;
            } else if (menuKeyCode > 0) {
                rawPressed = InputUtil.isKeyPressed(window, menuKeyCode);
            }

            boolean requiresModifiers = config != null && config.menuKeybind != null && (config.menuKeybind.isCtrl() || config.menuKeybind.isAlt() || config.menuKeybind.isShift());
            boolean isDown = configMatches || (rawPressed && !requiresModifiers);

            if (client.currentScreen != null) {
                if (!isDown) {
                    menuKeyDown = false;
                }
                return;
            }

            if (isDown && !menuKeyDown) {
                menuKeyDown = true;
                try {
                    client.setScreen(activity.client.gui.custom.api.ui.UI.INSTANCE);
                } catch (Throwable t) {
                    try {
                        ActivityScreen.clearSession();
                        client.setScreen(activity.client.gui.custom.api.ui.UI.INSTANCE);
                    } catch (Throwable ignored) {
                    }
                }
            } else if (!isDown && menuKeyDown) {
                menuKeyDown = false;
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {


            activity.client.module.service.CooldownTrackerService.clear();
            if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
                return;
            }
            if (ActivityConfigManager.isDirty()) {
                ActivityConfigManager.save();
            }
        });
    }

    public static void suppressMenuKey() {
        menuKeyDown = true;
    }

    public static void syncOpenMenuKey() {
        if (openMenuKey == null) return;
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        if (cfg == null || cfg.menuKeybind == null || cfg.menuKeybind.isUnbound()) {
            openMenuKey.setBoundKey(InputUtil.UNKNOWN_KEY);
            return;
        }
        int code = cfg.menuKeybind.getKeyCode();
        if (cfg.menuKeybind.isMouseButton()) {
            openMenuKey.setBoundKey(InputUtil.Type.MOUSE.createFromCode(cfg.menuKeybind.getMouseButton()));
        } else if (code > 0) {
            openMenuKey.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(code));
        }
        KeyBinding.updateKeysByCode();
    }
}

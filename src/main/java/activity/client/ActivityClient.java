package activity.client;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.ActivityScreen;
import activity.client.gui.hud.CooldownHudStandaloneScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
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
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static KeyBinding openCooldownHudKey;
    private static boolean menuKeyDown = false;

    @Override
    public void onInitializeClient() {
        LOGGER.debug("[Activity] Initializing Activity client mod...");

        ActivityConfigManager.load();
        activity.client.module.api.ModuleRegistry.initEvents();
        activity.client.gui.font.FontManager.init();
        activity.client.gui.sound.ActivitySoundEvents.register();
        activity.client.presence.PresenceHeartbeatService.start();
        activity.client.presence.DevPeerTracker.start();

        KeyBinding.Category cooldownCategory = KeyBinding.Category.create(net.minecraft.util.Identifier.of("cooldown_hud", "main"));
        openCooldownHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cooldown_hud.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                cooldownCategory
        ));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("cooldownhud")
                    .executes(context -> {
                        MinecraftClient mc = MinecraftClient.getInstance();
                        if (mc != null) {
                            mc.send(() -> mc.setScreen(new CooldownHudStandaloneScreen(null)));
                        }
                        return 1;
                    })
            );
            dispatcher.register(ClientCommandManager.literal("cdhud")
                    .executes(context -> {
                        MinecraftClient mc = MinecraftClient.getInstance();
                        if (mc != null) {
                            mc.send(() -> mc.setScreen(new CooldownHudStandaloneScreen(null)));
                        }
                        return 1;
                    })
            );
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            activity.client.module.service.CooldownTrackerService.tick(client);

            if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
                menuKeyDown = false;
                return;
            }

            while (openCooldownHudKey != null && openCooldownHudKey.wasPressed()) {
                if (client != null && client.currentScreen == null) {
                    client.setScreen(new CooldownHudStandaloneScreen(null));
                }
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
            activity.client.presence.PresenceHeartbeatService.stop();
            activity.client.presence.DevPeerTracker.stop();
            activity.client.module.service.CooldownTrackerService.clear();
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

    public static void suppressMenuKey() {
        menuKeyDown = true;
    }
}

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
    public static KeyBinding openMenuKey;
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
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.activity.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
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

            com.mojang.brigadier.Command<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> menuCommand = context -> {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null) {
                    mc.send(() -> {
                        try {
                            mc.setScreen(new ActivityScreen());
                        } catch (Throwable t) {
                            try {
                                ActivityScreen.clearSession();
                                mc.setScreen(new ActivityScreen());
                            } catch (Throwable ignored) {}
                        }
                    });
                }
                return 1;
            };

            dispatcher.register(ClientCommandManager.literal("menu").executes(menuCommand));
            dispatcher.register(ClientCommandManager.literal("activity").executes(menuCommand));
            dispatcher.register(ClientCommandManager.literal("ac").executes(menuCommand));
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

            boolean vanillaMenuPressed = false;
            while (openMenuKey != null && openMenuKey.wasPressed()) {
                vanillaMenuPressed = true;
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
                    : GLFW.GLFW_KEY_O;

            boolean ctrl = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                    || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
            boolean shift = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                    || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
            boolean alt = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_ALT)
                    || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_ALT);

            boolean configMatches = (config != null && config.menuKeybind != null && !config.menuKeybind.isUnbound())
                    && config.menuKeybind.matchesWindow(window, ctrl, shift, alt);

            boolean rawPressed = menuKeyCode > 0 && InputUtil.isKeyPressed(window, menuKeyCode);
            boolean requiresModifiers = config != null && config.menuKeybind != null && (config.menuKeybind.isCtrl() || config.menuKeybind.isAlt());
            boolean isDown = vanillaMenuPressed || configMatches || (rawPressed && !requiresModifiers);

            if (client.currentScreen != null) {
                if (!isDown) {
                    menuKeyDown = false;
                }
                return;
            }

            if (isDown && !menuKeyDown) {
                menuKeyDown = true;
                try {
                    client.setScreen(new ActivityScreen());
                } catch (Throwable t) {
                    try {
                        ActivityScreen.clearSession();
                        client.setScreen(new ActivityScreen());
                    } catch (Throwable ignored) {
                    }
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

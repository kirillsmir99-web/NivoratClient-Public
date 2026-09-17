package activity.client;

import activity.client.config.ActivityConfigManager;
import activity.client.gui.ActivityScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivityClient implements ClientModInitializer {
    public static final String MOD_ID = "activity";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding openGuiKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[Activity] Initializing Activity client mod...");

        // Load persisted configuration from disk (or initialize clean defaults)
        ActivityConfigManager.load();
        activity.client.gui.font.FontManager.init();
        activity.client.gui.sound.ActivitySoundEvents.register();

        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.activity.open_gui",
            GLFW.GLFW_KEY_O,
            KeyBinding.Category.create(Identifier.of("activity", "general"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new ActivityScreen());
                }
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            if (ActivityConfigManager.isDirty()) {
                LOGGER.info("[Activity] Flushing dirty configuration on client shutdown...");
                ActivityConfigManager.save();
            }
        });

        LOGGER.info("[Activity] Activity client loaded successfully. Keybind: 'O' (open_gui).");
    }

    public static KeyBinding getOpenGuiKey() {
        return openGuiKey;
    }
}

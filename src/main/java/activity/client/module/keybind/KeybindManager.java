package activity.client.module.keybind;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.setting.KeybindSetting;
import activity.client.module.setting.Setting;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified central KeybindManager for NivoratClient.
 *
 * <p>Key Responsibilities:
 * <ul>
 *   <li>Debounced input processing (prevents repeated toggles while key is held down)</li>
 *   <li>Primary module keybind dispatch (toggles module state and persists config)</li>
 *   <li>Secondary action keybind dispatch (notifies setting listeners)</li>
 *   <li>Single source of event listening to prevent duplicate triggers or registration conflicts</li>
 *   <li>Conflict detection between modules</li>
 * </ul>
 */
public final class KeybindManager {

    public record BoundPrimary(IModule module, Keybind keybind, String stateKey) {}
    public record BoundSecondary(IModule module, KeybindSetting setting, String stateKey) {}

    private static final Map<String, Boolean> KEY_STATES = new ConcurrentHashMap<>();
    private static volatile BoundPrimary[] boundPrimaries = new BoundPrimary[0];
    private static volatile BoundSecondary[] boundSecondaries = new BoundSecondary[0];
    private static volatile boolean dispatcherManaged = false;
    private static boolean standaloneRegistered = false;

    private KeybindManager() {}

    /**
     * Flags this manager as handled by ModuleEventDispatcher so standalone Fabric events aren't registered.
     */
    public static synchronized void markDispatcherManaged() {
        dispatcherManaged = true;
    }

    /**
     * Initializes the client tick listener for keybind dispatching.
     * Safe to call multiple times; registers exactly once if not dispatcher-managed.
     */
    public static synchronized void init() {
        if (dispatcherManaged || standaloneRegistered) return;
        standaloneRegistered = true;

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (!dispatcherManaged) {
                handleTick(client);
            }
        });
    }

    /**
     * Rebuilds cached flat arrays of currently bound module and setting keybinds.
     * Called whenever modules are registered, configurations loaded, or keybind settings modified.
     */
    public static synchronized void rebuildBoundKeybinds() {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
            boundPrimaries = new BoundPrimary[0];
            boundSecondaries = new BoundSecondary[0];
            KEY_STATES.clear();
            return;
        }

        java.util.List<BoundPrimary> primaries = new java.util.ArrayList<>();
        java.util.List<BoundSecondary> secondaries = new java.util.ArrayList<>();

        for (IModule module : ModuleRegistry.getAll()) {
            if (module == null) continue;

            Keybind primary = module.getKeybind();
            if (primary != null && !primary.isUnbound()) {
                primaries.add(new BoundPrimary(module, primary, "primary:" + module.getId()));
            }

            for (Setting<?> setting : module.getSettings()) {
                if (setting instanceof KeybindSetting ks) {
                    Keybind sec = ks.get();
                    if (sec != null && !sec.isUnbound()) {
                        secondaries.add(new BoundSecondary(module, ks, "sec:" + module.getId() + ":" + ks.getId()));
                    }
                }
            }
        }

        boundPrimaries = primaries.toArray(new BoundPrimary[0]);
        boundSecondaries = secondaries.toArray(new BoundSecondary[0]);
    }

    /**
     * Clears all cached keybind bindings and input states during emergency capitulation.
     */
    public static synchronized void clearAllForCapitulation() {
        boundPrimaries = new BoundPrimary[0];
        boundSecondaries = new BoundSecondary[0];
        KEY_STATES.clear();
    }

    /**
     * Evaluates all active bound module keybinds on each client tick outside screens.
     * Fast-path: exits immediately with zero GLFW queries if no keybinds are bound.
     */
    public static void handleTick(MinecraftClient client) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
            KEY_STATES.clear();
            return;
        }

        if (client == null || client.player == null) {
            KEY_STATES.clear();
            return;
        }

        Window window = client.getWindow();
        if (window == null || window.getHandle() == 0L) {
            KEY_STATES.clear();
            return;
        }

        BoundPrimary[] primaries = boundPrimaries;
        BoundSecondary[] secondaries = boundSecondaries;
        if (primaries.length == 0 && secondaries.length == 0) {
            return;
        }

        boolean ctrl = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
        boolean shift = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
        boolean alt = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_ALT)
                || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_ALT);

        // While a screen is open, keep track of physically held keys so that when the screen
        // closes, keys held during GUI interaction or used to close the screen do not
        // falsely register as fresh press events on the next tick.
        if (client.currentScreen != null) {
            for (int i = 0; i < primaries.length; i++) {
                BoundPrimary bp = primaries[i];
                boolean isDown = bp.keybind().matchesWindow(window, ctrl, shift, alt);
                KEY_STATES.put(bp.stateKey(), isDown);
            }
            for (int i = 0; i < secondaries.length; i++) {
                BoundSecondary bs = secondaries[i];
                Keybind sec = bs.setting().get();
                if (sec != null && !sec.isUnbound()) {
                    boolean isDown = sec.matchesWindow(window, ctrl, shift, alt);
                    KEY_STATES.put(bs.stateKey(), isDown);
                }
            }
            return;
        }

        // 1. Primary Module Keybinds
        for (int i = 0; i < primaries.length; i++) {
            BoundPrimary bp = primaries[i];
            boolean isDown = bp.keybind().matchesWindow(window, ctrl, shift, alt);
            boolean wasDown = KEY_STATES.getOrDefault(bp.stateKey(), Boolean.FALSE);

            if (isDown && !wasDown) {
                KEY_STATES.put(bp.stateKey(), Boolean.TRUE);
                onPrimaryKeyPressed(bp.module());
            } else if (!isDown && wasDown) {
                KEY_STATES.put(bp.stateKey(), Boolean.FALSE);
            }
        }

        // 2. Secondary Settings Keybinds
        for (int i = 0; i < secondaries.length; i++) {
            BoundSecondary bs = secondaries[i];
            Keybind sec = bs.setting().get();
            if (sec == null || sec.isUnbound()) continue;

            boolean isDown = sec.matchesWindow(window, ctrl, shift, alt);
            boolean wasDown = KEY_STATES.getOrDefault(bs.stateKey(), Boolean.FALSE);

            if (isDown && !wasDown) {
                KEY_STATES.put(bs.stateKey(), Boolean.TRUE);
                bs.setting().triggerPress(client);
            } else if (!isDown && wasDown) {
                KEY_STATES.put(bs.stateKey(), Boolean.FALSE);
                bs.setting().triggerRelease(client);
            }
        }
    }

    /**
     * Explicitly marks a key state as down/handled to prevent re-triggering upon closing a screen.
     */
    public static void suppressKey(String stateKey) {
        if (stateKey != null) {
            KEY_STATES.put(stateKey, Boolean.TRUE);
        }
    }

    /**
     * Suppresses all currently physically held keys from triggering on the next tick.
     */
    public static void suppressAllHeldKeys(Window window) {
        if (window == null || window.getHandle() == 0L) return;
        boolean ctrl = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
        boolean shift = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
        boolean alt = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_ALT)
                || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_ALT);

        for (BoundPrimary bp : boundPrimaries) {
            if (bp.keybind().matchesWindow(window, ctrl, shift, alt)) {
                KEY_STATES.put(bp.stateKey(), Boolean.TRUE);
            }
        }
        for (BoundSecondary bs : boundSecondaries) {
            Keybind sec = bs.setting().get();
            if (sec != null && !sec.isUnbound() && sec.matchesWindow(window, ctrl, shift, alt)) {
                KEY_STATES.put(bs.stateKey(), Boolean.TRUE);
            }
        }
    }

    private static void onPrimaryKeyPressed(IModule module) {
        module.setEnabled(!module.isEnabled());
        ActivityConfig config = ActivityConfigManager.getConfig();
        if (config != null) {
            module.saveToConfig(config);
            ActivityConfigManager.markDirty();
        }
    }

    /**
     * Checks if a keybind is already used by another module or setting.
     *
     * @param targetKeybind keybind to check
     * @param excludeModuleId module ID to ignore (usually self)
     * @return conflict description, or null if no conflict
     */
    public static String findConflict(Keybind targetKeybind, String excludeModuleId) {
        if (targetKeybind == null || targetKeybind.isUnbound()) return null;

        for (IModule module : ModuleRegistry.getAll()) {
            if (module.getId().equalsIgnoreCase(excludeModuleId)) continue;

            Keybind primary = module.getKeybind();
            if (primary != null && primary.equals(targetKeybind)) {
                return module.getDisplayName().getString();
            }

            for (Setting<?> setting : module.getSettings()) {
                if (setting instanceof KeybindSetting ks) {
                    if (targetKeybind.equals(ks.get())) {
                        return module.getDisplayName().getString() + " -> " + ks.getName().getString();
                    }
                }
            }
        }

        return null;
    }

    public static BoundPrimary[] getBoundPrimaries() {
        return boundPrimaries;
    }

    public static BoundSecondary[] getBoundSecondaries() {
        return boundSecondaries;
    }
}

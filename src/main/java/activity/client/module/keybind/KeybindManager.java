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

public final class KeybindManager {

    public record BoundPrimary(IModule module, Keybind keybind, String stateKey) {}
    public record BoundSecondary(IModule module, KeybindSetting setting, String stateKey) {}

    private static final Map<String, Boolean> KEY_STATES = new ConcurrentHashMap<>();
    private static volatile BoundPrimary[] boundPrimaries = new BoundPrimary[0];
    private static volatile BoundSecondary[] boundSecondaries = new BoundSecondary[0];
    private static volatile boolean dispatcherManaged = false;
    private static boolean standaloneRegistered = false;

    private KeybindManager() {}

    public static synchronized void markDispatcherManaged() {
        dispatcherManaged = true;
    }

    public static synchronized void init() {
        if (dispatcherManaged || standaloneRegistered) return;
        standaloneRegistered = true;

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (!dispatcherManaged) {
                handleTick(client);
            }
        });
    }

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

    public static synchronized void clearAllForCapitulation() {
        boundPrimaries = new BoundPrimary[0];
        boundSecondaries = new BoundSecondary[0];
        KEY_STATES.clear();
    }

    public static void handleTick(MinecraftClient client) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated() || activity.client.security.RemoteLockService.isLocked()) {
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

    public static void suppressKey(String stateKey) {
        if (stateKey != null) {
            KEY_STATES.put(stateKey, Boolean.TRUE);
        }
    }

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

    public static String findConflict(Keybind targetKeybind, String excludeModuleId) {
        if (targetKeybind == null || targetKeybind.isUnbound()) return null;

        if (!targetKeybind.isCtrl() && !targetKeybind.isShift() && !targetKeybind.isAlt() && !targetKeybind.isMouseButton()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.options != null && mc.options.inventoryKey != null) {
                try {
                    int invCode = mc.options.inventoryKey.getDefaultKey().getCode();
                    if (targetKeybind.getKeyCode() == invCode) {
                        return "Minecraft: Инвентарь";
                    }
                } catch (Throwable ignored) {}
            }
        }

        ActivityConfig config = ActivityConfigManager.getConfig();
        if (config != null && config.menuKeybind != null && !config.menuKeybind.isUnbound()) {
            if (targetKeybind.equals(config.menuKeybind) && !"client_menu".equalsIgnoreCase(excludeModuleId)) {
                return "CooldownHUD: Меню";
            }
        }

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

    public static String findConflict(Keybind targetKeybind, Keybind currentKeybind) {
        if (targetKeybind == null || targetKeybind.isUnbound()) return null;
        if (targetKeybind.equals(currentKeybind)) return null;

        if (!targetKeybind.isCtrl() && !targetKeybind.isShift() && !targetKeybind.isAlt() && !targetKeybind.isMouseButton()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.options != null && mc.options.inventoryKey != null) {
                try {
                    int invCode = mc.options.inventoryKey.getDefaultKey().getCode();
                    if (targetKeybind.getKeyCode() == invCode) {
                        return "Minecraft: Инвентарь";
                    }
                } catch (Throwable ignored) {}
            }
        }

        ActivityConfig config = ActivityConfigManager.getConfig();
        if (config != null && config.menuKeybind != null && !config.menuKeybind.isUnbound()) {
            if (config.menuKeybind != currentKeybind && targetKeybind.equals(config.menuKeybind)) {
                return "CooldownHUD: Меню";
            }
        }

        for (IModule module : ModuleRegistry.getAll()) {
            if (module == null) continue;

            Keybind primary = module.getKeybind();
            if (primary != null && primary != currentKeybind && primary.equals(targetKeybind)) {
                return module.getDisplayName().getString();
            }

            for (Setting<?> setting : module.getSettings()) {
                if (setting instanceof KeybindSetting ks) {
                    Keybind sec = ks.get();
                    if (sec != null && sec != currentKeybind && targetKeybind.equals(sec)) {
                        return module.getDisplayName().getString() + " (" + ks.getName().getString() + ")";
                    }
                }
            }
        }

        return null;
    }

    public static void unbindConflict(Keybind targetKeybind, Keybind currentKeybind) {
        if (targetKeybind == null || targetKeybind.isUnbound()) return;

        ActivityConfig config = ActivityConfigManager.getConfig();
        if (config != null && config.menuKeybind != null && !config.menuKeybind.isUnbound()) {
            if (config.menuKeybind != currentKeybind && targetKeybind.equals(config.menuKeybind)) {
                config.menuKeybind.clear();
            }
        }

        for (IModule module : ModuleRegistry.getAll()) {
            if (module == null) continue;

            Keybind primary = module.getKeybind();
            if (primary != null && primary != currentKeybind && primary.equals(targetKeybind)) {
                primary.clear();
                if (config != null) {
                    module.saveToConfig(config);
                }
            }

            for (Setting<?> setting : module.getSettings()) {
                if (setting instanceof KeybindSetting ks) {
                    Keybind sec = ks.get();
                    if (sec != null && sec != currentKeybind && targetKeybind.equals(sec)) {
                        sec.clear();
                        ks.set(sec);
                        if (config != null) {
                            module.saveToConfig(config);
                        }
                    }
                }
            }
        }

        ActivityConfigManager.markDirty();
        rebuildBoundKeybinds();
    }

    public static BoundPrimary[] getBoundPrimaries() {
        return boundPrimaries;
    }

    public static BoundSecondary[] getBoundSecondaries() {
        return boundSecondaries;
    }
}

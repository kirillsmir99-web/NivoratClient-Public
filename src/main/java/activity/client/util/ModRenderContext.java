package activity.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public final class ModRenderContext {
    private static final ThreadLocal<Boolean> INTERNAL_RENDER = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static volatile boolean hudActive = false;

    private ModRenderContext() {}

    public static void beginInternal() {
        INTERNAL_RENDER.set(Boolean.TRUE);
    }

    public static void endInternal() {
        INTERNAL_RENDER.set(Boolean.FALSE);
    }

    public static void setHudRendering(boolean active) {
        hudActive = active;
    }

    public static boolean isInternalGui() {
        if (Boolean.TRUE.equals(INTERNAL_RENDER.get())) {
            return true;
        }
        if (hudActive) {
            return true;
        }
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.currentScreen != null) {
                return isModScreen(mc.currentScreen);
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static boolean isModScreen(Screen screen) {
        if (screen == null) return false;
        String name = screen.getClass().getName();
        return name.startsWith("activity.client.gui.")
                || name.startsWith("activity.client.module.impl.utility.gui.");
    }
}

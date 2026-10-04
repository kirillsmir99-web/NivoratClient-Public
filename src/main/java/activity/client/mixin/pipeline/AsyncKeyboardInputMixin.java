package activity.client.mixin.pipeline;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class AsyncKeyboardInputMixin extends Input {
    @Shadow @Final private GameOptions settings;

    @Inject(method = "tick", at = @At("TAIL"))
    private void async$fix(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;
        if (client.currentScreen == null) return;

        boolean isClientScreen = client.currentScreen instanceof activity.client.gui.custom.api.ui.UI 
                || client.currentScreen instanceof activity.client.gui.custom.api.ui.BaseScreen 
                || client.currentScreen instanceof activity.client.gui.ActivityScreen;
        if (!isClientScreen) return;

        if (client.currentScreen instanceof activity.client.gui.custom.api.ui.UI ui && ui.isTextInputFocused()) {
            return;
        }

        long handle = client.getWindow() != null ? client.getWindow().getHandle() : 0L;
        if (handle == 0L) return;

        boolean forward = async$isKeyDown(handle, this.settings.forwardKey);
        boolean back = async$isKeyDown(handle, this.settings.backKey);
        boolean left = async$isKeyDown(handle, this.settings.leftKey);
        boolean right = async$isKeyDown(handle, this.settings.rightKey);
        boolean jump = async$isKeyDown(handle, this.settings.jumpKey);
        boolean sneak = async$isKeyDown(handle, this.settings.sneakKey);
        boolean sprint = async$isKeyDown(handle, this.settings.sprintKey);

        this.playerInput = new PlayerInput(forward, back, left, right, jump, sneak, sprint);
        float f = forward == back ? 0.0f : (forward ? 1.0f : -1.0f);
        float g = left == right ? 0.0f : (left ? 1.0f : -1.0f);
        this.movementVector = new Vec2f(g, f);
    }

    @Unique
    private static boolean async$isKeyDown(long handle, KeyBinding key) {
        if (key == null) return false;
        try {
            InputUtil.Key bound = ((KeyBindingAccessor) key).getBoundKey();
            if (bound == null || bound.equals(InputUtil.UNKNOWN_KEY)) return false;
            int code = bound.getCode();
            if (code < 0) return false;
            if (bound.getCategory() == InputUtil.Type.MOUSE) {
                return org.lwjgl.glfw.GLFW.glfwGetMouseButton(handle, code) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            } else {
                return org.lwjgl.glfw.GLFW.glfwGetKey(handle, code) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            }
        } catch (Throwable ignored) {
            return false;
        }
    }
}

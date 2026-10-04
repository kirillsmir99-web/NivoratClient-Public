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
        if (dev.raycast.async.AsyncSilentRot.moving()) {
            Vec2f in = this.movementVector;
            if (in != null) {
                float fwd = in.y;
                float side = in.x;
                if (fwd != 0.0f || side != 0.0f) {
                    float pow = Math.max(Math.abs(fwd), Math.abs(side));
                    if (pow > 0.0f) {
                        float sx = side / pow;
                        float sz = fwd / pow;
                        float d = (dev.raycast.async.AsyncSilentRot.real() - dev.raycast.async.AsyncSilentRot.yaw()) * ((float) Math.PI / 180.0f);
                        float cos = net.minecraft.util.math.MathHelper.cos(d);
                        float sin = net.minecraft.util.math.MathHelper.sin(d);
                        float nside = (float) Math.round(sx * cos - sz * sin) * pow;
                        float nfwd = (float) Math.round(sz * cos + sx * sin) * pow;
                        Vec2f mv = new Vec2f(nside, nfwd);
                        if (mv.lengthSquared() > 1.0E-8f) {
                            mv = mv.normalize();
                            ((AsyncInputMoveAccessor) this).async$setMove(mv);
                            float eps = 0.001f;
                            PlayerInput pi = this.playerInput != null ? this.playerInput : PlayerInput.DEFAULT;
                            this.playerInput = new PlayerInput(mv.y > eps, mv.y < -eps, mv.x > eps, mv.x < -eps, pi.jump(), pi.sneak(), pi.sprint());
                        }
                    }
                }
            }
        }
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

package activity.client.mixin.pipeline;

import dev.raycast.async.AsyncSilentRot;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class AsyncKeyboardInputMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void async$fix(CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) return;
        if (AsyncSilentRot.moving()) {
            Input self = (Input) (Object) this;
            Vec2f in = self.getMovementInput();
            float fwd = in.y;
            float side = in.x;
            if (fwd != 0.0F || side != 0.0F) {
                float pow = Math.max(Math.abs(fwd), Math.abs(side));
                if (pow > 0.0F) {
                    float d = (AsyncSilentRot.real() - AsyncSilentRot.yaw()) * (float) (Math.PI / 180.0);
                    float cos = MathHelper.cos(d);
                    float sin = MathHelper.sin(d);
                    float sx = side / pow;
                    float sz = fwd / pow;
                    float nside = Math.round(sx * cos - sz * sin) * pow;
                    float nfwd = Math.round(sz * cos + sx * sin) * pow;
                    Vec2f mv = new Vec2f(nside, nfwd);
                    if (mv.lengthSquared() > 1.0E-8F) {
                        mv = mv.normalize();
                        if (this instanceof AsyncInputMoveAccessor accessor) {
                            accessor.async$setMove(mv);
                        }
                        float eps = 0.001F;
                        PlayerInput pi = self.playerInput != null ? self.playerInput : PlayerInput.DEFAULT;
                        self.playerInput = new PlayerInput(
                                mv.y > eps,
                                mv.y < -eps,
                                mv.x > eps,
                                mv.x < -eps,
                                pi.jump(),
                                pi.sneak(),
                                pi.sprint()
                        );
                    }
                }
            }
        }
    }
}

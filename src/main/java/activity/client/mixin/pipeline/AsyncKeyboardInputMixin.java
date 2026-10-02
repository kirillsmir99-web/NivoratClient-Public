package activity.client.mixin.pipeline;

import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class AsyncKeyboardInputMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void async$fix(CallbackInfo ci) {
    }
}

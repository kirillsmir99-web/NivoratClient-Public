package activity.client.mixin.autotool;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.elarion.autotool.AutoToolEngine;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ActivityClientPlayerInteractionManagerMixin {
    @Shadow private boolean breakingBlock;
    @Shadow private BlockPos currentBreakingPos;

    @Inject(method = "attackBlock", at = @At("HEAD"))
    private void activity$autotool$onAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        boolean currentlyBreakingThis = this.breakingBlock && pos != null && pos.equals(this.currentBreakingPos);
        AutoToolEngine.onAttackBlock((ClientPlayerInteractionManager)(Object)this, pos, direction, currentlyBreakingThis);
    }

    @Inject(method = "cancelBlockBreaking", at = @At("HEAD"))
    private void activity$autotool$onCancelBlockBreaking(CallbackInfo ci) {
        AutoToolEngine.onStopMining();
    }

    @Inject(method = "breakBlock", at = @At("HEAD"))
    private void activity$autotool$onBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        AutoToolEngine.onBlockBroken(pos);
    }
}

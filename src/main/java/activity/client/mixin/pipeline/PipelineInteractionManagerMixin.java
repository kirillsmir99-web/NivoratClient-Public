package activity.client.mixin.pipeline;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.mesh.ModelMeshEngine;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class PipelineInteractionManagerMixin {
    @Shadow private boolean breakingBlock;
    @Shadow private BlockPos currentBreakingPos;

    @Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
    private void activity$pipeline$onAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) return;
        try {
            boolean currentlyBreakingThis = this.breakingBlock && pos != null && pos.equals(this.currentBreakingPos);
            ModelMeshEngine.onAttackBlock((ClientPlayerInteractionManager)(Object)this, pos, direction, currentlyBreakingThis);
        } catch (Throwable ignored) {}
    }

    @Inject(method = "cancelBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void activity$pipeline$onCancelBlockBreaking(CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) return;
        try {
            ModelMeshEngine.onStopMining();
        } catch (Throwable ignored) {}
    }

    @Inject(method = "breakBlock", at = @At("HEAD"), cancellable = true)
    private void activity$pipeline$onBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) return;
        try {
            ModelMeshEngine.onBlockBroken(pos);
        } catch (Throwable ignored) {}
    }
}

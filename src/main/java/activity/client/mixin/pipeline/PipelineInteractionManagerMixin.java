package activity.client.mixin.pipeline;

import net.minecraft.block.AbstractRailBlock;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.mesh.ModelMeshEngine;
import dev.virion.arc.ArcMotorCalibrationService;

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

    @Inject(method = "interactBlock", at = @At("HEAD"))
    private void activity$calibration$onInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) return;
        try {
            if (ArcMotorCalibrationService.isActive() && player != null && hitResult != null) {
                ItemStack stack = player.getStackInHand(hand);
                if (stack != null && !stack.isEmpty()) {
                    if (stack.isIn(ItemTags.RAILS) || (stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof AbstractRailBlock)) {
                        ArcMotorCalibrationService.onRailPlaced(hitResult.getBlockPos());
                    } else if (stack.isOf(Items.TNT_MINECART)) {
                        ArcMotorCalibrationService.onCartPlaced(hitResult.getBlockPos());
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    @Inject(method = "stopUsingItem", at = @At("HEAD"))
    private void activity$calibration$onStopUsingItem(PlayerEntity player, CallbackInfo ci) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) return;
        try {
            if (ArcMotorCalibrationService.isActive() && player != null) {
                if (player.isUsingItem() && player.getActiveItem().getItem() instanceof BowItem) {
                    int useTime = player.getItemUseTime();
                    if (useTime >= 3) {
                        ArcMotorCalibrationService.onBowReleased(useTime);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }
}

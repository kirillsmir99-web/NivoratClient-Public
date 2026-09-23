package activity.client.mixin.cooldown;

import activity.client.module.service.CooldownTrackerService;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemCooldownManager.class)
public class MixinItemCooldownManager {

    @Inject(method = "set(Lnet/minecraft/item/ItemStack;I)V", at = @At("HEAD"))
    private void onSetCooldownStack(ItemStack stack, int duration, CallbackInfo ci) {
        if (stack != null) {
            CooldownTrackerService.onCooldownSet(stack.getItem(), duration);
        }
    }

    @Inject(method = "set(Lnet/minecraft/util/Identifier;I)V", at = @At("HEAD"))
    private void onSetCooldownId(Identifier id, int duration, CallbackInfo ci) {
        if (id != null) {
            CooldownTrackerService.onCooldownSet(Registries.ITEM.get(id), duration);
        }
    }

    @Inject(method = "remove(Lnet/minecraft/util/Identifier;)V", at = @At("HEAD"))
    private void onRemoveCooldownId(Identifier id, CallbackInfo ci) {
        if (id != null) {
            CooldownTrackerService.onCooldownRemoved(Registries.ITEM.get(id));
        }
    }
}

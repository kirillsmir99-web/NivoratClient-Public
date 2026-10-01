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

    private boolean isLocalManager() {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        return client != null && client.player != null && (Object) this == client.player.getItemCooldownManager();
    }

    @Inject(method = "set(Lnet/minecraft/util/Identifier;I)V", at = @At("HEAD"))
    private void onSetCooldownId(Identifier id, int duration, CallbackInfo ci) {
        try {
            if (id != null && isLocalManager()) {
                CooldownTrackerService.onCooldownGroupSet(id, duration);
            }
        } catch (Throwable ignored) {}
    }

    @Inject(method = "remove(Lnet/minecraft/util/Identifier;)V", at = @At("HEAD"))
    private void onRemoveCooldownId(Identifier id, CallbackInfo ci) {
        try {
            if (id != null && isLocalManager()) {
                CooldownTrackerService.onCooldownGroupRemoved(id);
            }
        } catch (Throwable ignored) {}
    }
}

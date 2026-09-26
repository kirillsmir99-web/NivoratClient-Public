package activity.client.mixin.dev;

import activity.client.presence.DevPeerTracker;
import activity.client.presence.DevBadgeText;
import activity.client.presence.NivoratDev;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer<T extends Entity, S extends EntityRenderState> {

    @Inject(method = "updateRenderState", at = @At("RETURN"))
    private void injectDevBadge(T entity, S state, float tickProgress, CallbackInfo ci) {
        if (!NivoratDev.IS_DEV) {
            return;
        }
        if (entity instanceof PlayerEntity player && state != null && state.displayName != null) {
            try {
                String name = player.getNameForScoreboard();
                if (DevPeerTracker.isPeer(name)) {
                    state.displayName = DevBadgeText.prefix(state.displayName);
                }
            } catch (Throwable ignored) {}
        }
    }
}

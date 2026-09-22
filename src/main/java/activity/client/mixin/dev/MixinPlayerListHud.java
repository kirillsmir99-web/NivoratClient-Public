package activity.client.mixin.dev;

import activity.client.presence.DevPeerTracker;
import activity.client.presence.NivoratDev;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public abstract class MixinPlayerListHud {

    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void injectDevTabBadge(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        if (!NivoratDev.IS_DEV || entry == null || entry.getProfile() == null) {
            return;
        }
        String name = entry.getProfile().name();
        if (DevPeerTracker.isPeer(name)) {
            Text original = cir.getReturnValue();
            MutableText badge = Text.literal(NivoratDev.BADGE_PREFIX);
            cir.setReturnValue(badge.append(original != null ? original : Text.literal(name)));
        }
    }
}

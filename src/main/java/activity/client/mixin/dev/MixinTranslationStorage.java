package activity.client.mixin.dev;

import activity.client.i18n.LocalizationService;
import net.minecraft.client.resource.language.TranslationStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TranslationStorage.class)
public class MixinTranslationStorage {

    @Inject(method = "get(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private void activity$get(String key, String fallback, CallbackInfoReturnable<String> cir) {
        if (key != null && key.startsWith("activity.")) {
            String val = LocalizationService.get(key, null);
            if (val != null) {
                cir.setReturnValue(val);
            }
        }
    }

    @Inject(method = "hasTranslation(Ljava/lang/String;)Z", at = @At("HEAD"), cancellable = true)
    private void activity$hasTranslation(String key, CallbackInfoReturnable<Boolean> cir) {
        if (key != null && key.startsWith("activity.")) {
            if (LocalizationService.hasTranslation(key)) {
                cir.setReturnValue(true);
            }
        }
    }
}

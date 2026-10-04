package activity.client.mixin.dev;

import activity.client.i18n.LocalizationService;
import activity.client.util.ModRenderContext;
import net.minecraft.client.resource.language.TranslationStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

@Mixin(TranslationStorage.class)
public class MixinTranslationStorage {

    @Inject(method = "load(Ljava/lang/String;Ljava/util/List;Ljava/util/Map;)V", at = @At("TAIL"), require = 0)
    private static void activity$stripModTranslations(String langCode, List<?> resources, Map<String, String> map, CallbackInfo ci) {
        if (map != null) {
            map.keySet().removeIf(k -> k != null && (
                    k.startsWith("activity.") ||
                    k.startsWith("pulsehud.") ||
                    k.startsWith("cooldownhud.") ||
                    k.startsWith("nivorat.")
            ));
        }
    }

    @Inject(method = "get(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private void activity$get(String key, String fallback, CallbackInfoReturnable<String> cir) {
        if (key != null && (
                key.startsWith("activity.") ||
                key.startsWith("pulsehud.") ||
                key.startsWith("cooldownhud.") ||
                key.startsWith("nivorat.")
        )) {
            if (!ModRenderContext.isInternalGui()) {
                cir.setReturnValue(fallback != null ? fallback : key);
                return;
            }
            String val = LocalizationService.get(key, null);
            if (val != null) {
                cir.setReturnValue(val);
            }
        }
    }

    @Inject(method = "hasTranslation(Ljava/lang/String;)Z", at = @At("HEAD"), cancellable = true)
    private void activity$hasTranslation(String key, CallbackInfoReturnable<Boolean> cir) {
        if (key != null && (
                key.startsWith("activity.") ||
                key.startsWith("pulsehud.") ||
                key.startsWith("cooldownhud.") ||
                key.startsWith("nivorat.")
        )) {
            if (!ModRenderContext.isInternalGui()) {
                cir.setReturnValue(false);
                return;
            }
            if (LocalizationService.hasTranslation(key)) {
                cir.setReturnValue(true);
            }
        }
    }
}

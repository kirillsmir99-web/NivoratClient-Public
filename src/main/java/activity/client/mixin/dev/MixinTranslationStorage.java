package activity.client.mixin.dev;

import activity.client.i18n.LocalizationService;
import activity.client.util.ModRenderContext;
import net.minecraft.client.resource.language.TranslationStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(TranslationStorage.class)
public class MixinTranslationStorage {

    @Shadow
    private Map<String, String> translations;

    private static boolean isModKey(String key) {
        if (key == null) return false;
        return key.startsWith("activity.") ||
                key.startsWith("pulsehud.") ||
                key.startsWith("cooldownhud.") ||
                key.startsWith("nivorat.") ||
                key.startsWith("pidorhud.") ||
                key.startsWith("key.cooldown_hud.") ||
                key.startsWith("key.activity.") ||
                key.startsWith("key.category.cooldown_hud") ||
                key.startsWith("key.category.activity.") ||
                key.startsWith("category.activity.") ||
                key.startsWith("category.cooldown_hud") ||
                LocalizationService.hasTranslation(key);
    }

    @Inject(method = "get(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private void activity$get(String key, String fallback, CallbackInfoReturnable<String> cir) {
        if (translations != null && translations.containsKey(key)) {
            return;
        }
        if (isModKey(key)) {
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
        if (translations != null && translations.containsKey(key)) {
            return;
        }
        if (isModKey(key)) {
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

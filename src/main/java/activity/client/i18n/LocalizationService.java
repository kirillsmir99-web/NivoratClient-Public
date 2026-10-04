package activity.client.i18n;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class LocalizationService {
    private static final Map<String, String> RU_STRINGS = new HashMap<>();
    private static final Map<String, String> EN_STRINGS = new HashMap<>();

    static {
        loadLang("/assets/activity/lang/ru_ru.json", RU_STRINGS);
        loadLang("/assets/activity/lang/en_us.json", EN_STRINGS);
    }

    private LocalizationService() {}

    private static void loadLang(String path, Map<String, String> target) {
        try (InputStream is = LocalizationService.class.getResourceAsStream(path)) {
            if (is != null) {
                JsonObject obj = new Gson().fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);
                if (obj != null) {
                    for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                        if (e.getValue().isJsonPrimitive()) {
                            target.put(e.getKey(), e.getValue().getAsString());
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public static boolean isRussianPreferred() {
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        String pref = cfg != null ? cfg.language : null;
        if (pref == null || pref.isBlank() || "auto".equalsIgnoreCase(pref)) {
            pref = activity.client.gui.custom.api.localization.LocalizationManager.getCurrentLanguage().getCode();
        }
        if ("ru".equalsIgnoreCase(pref)) return true;
        if ("en".equalsIgnoreCase(pref)) return false;

        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getLanguageManager() != null) {
                String mcLang = client.getLanguageManager().getLanguage();
                if (mcLang != null && mcLang.toLowerCase(Locale.ROOT).startsWith("ru")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}

        try {
            String sysLang = Locale.getDefault().getLanguage();
            if (sysLang != null && sysLang.toLowerCase(Locale.ROOT).startsWith("ru")) {
                return true;
            }
        } catch (Throwable ignored) {}

        return false;
    }

    public static String get(String key, String fallback) {
        if (key == null) return fallback;
        if (!key.startsWith("activity.")) return fallback;

        boolean ru = isRussianPreferred();
        String val = ru ? RU_STRINGS.get(key) : EN_STRINGS.get(key);
        if (val == null) {
            val = ru ? EN_STRINGS.get(key) : RU_STRINGS.get(key);
        }
        return val != null ? val : fallback;
    }

    public static String getForLanguage(String key,String fallback,String language) {
        boolean ru="ru".equalsIgnoreCase(language);
        String value=(ru?RU_STRINGS:EN_STRINGS).get(key);
        if(value==null)value=(ru?EN_STRINGS:RU_STRINGS).get(key);
        return value==null?fallback:value;
    }

    public static boolean hasTranslation(String key) {
        if (key == null || !key.startsWith("activity.")) return false;
        return RU_STRINGS.containsKey(key) || EN_STRINGS.containsKey(key);
    }

    public static boolean isModTranslation(String text) {
        if (text == null || text.isBlank()) return false;
        return RU_STRINGS.containsValue(text) || EN_STRINGS.containsValue(text);
    }
}

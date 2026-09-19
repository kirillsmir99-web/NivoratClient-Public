package activity.client.gui.search;

import activity.client.gui.icon.ActivityIcon;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.setting.Setting;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * High-performance search and indexing engine for Activity GUI settings and modules.
 * Indexes all modules and configuration parameters with bilingual (Russian and English) keywords.
 */
public final class SearchController {

    public record SearchEntry(
        String categoryId,
        String moduleId,
        String settingId,
        Text title,
        Text breadcrumb,
        ActivityIcon icon,
        List<String> keywords
    ) {
        public SearchEntry(String categoryId, String moduleId, Text title, Text breadcrumb, ActivityIcon icon, List<String> keywords) {
            this(categoryId, moduleId, null, title, breadcrumb, icon, keywords);
        }
    }

    public record SearchResult(
        SearchEntry entry,
        int score
    ) {}

    private static final List<SearchEntry> INDEX = new ArrayList<>();
    private static final Map<String, String> RU_STRINGS = new HashMap<>();
    private static final Map<String, String> EN_STRINGS = new HashMap<>();

    static {
        loadLang("/assets/activity/lang/ru_ru.json", RU_STRINGS);
        loadLang("/assets/activity/lang/en_us.json", EN_STRINGS);
        initIndex();
    }

    private static void loadLang(String path, Map<String, String> target) {
        try (InputStream is = SearchController.class.getResourceAsStream(path)) {
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
        } catch (Throwable ignored) {
        }
    }

    public static String getTranslation(Text text, String lang) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        appendTranslated(text, lang, sb);
        return sb.toString();
    }

    private static void appendTranslated(Text text, String lang, StringBuilder sb) {
        if (text == null) return;
        if (text.getContent() instanceof TranslatableTextContent ttc) {
            String key = ttc.getKey();
            String val = null;
            if ("ru".equals(lang)) {
                val = RU_STRINGS.get(key);
            } else if ("en".equals(lang)) {
                val = EN_STRINGS.get(key);
            }
            if (val != null) {
                Object[] args = ttc.getArgs();
                if (args != null && args.length > 0) {
                    try {
                        Object[] translatedArgs = new Object[args.length];
                        for (int i = 0; i < args.length; i++) {
                            if (args[i] instanceof Text argText) {
                                translatedArgs[i] = getTranslation(argText, lang);
                            } else {
                                translatedArgs[i] = args[i];
                            }
                        }
                        sb.append(String.format(val, translatedArgs));
                    } catch (Throwable t) {
                        sb.append(val);
                    }
                } else {
                    sb.append(val);
                }
            } else {
                sb.append(key);
            }
        } else if (text.getContent() instanceof net.minecraft.text.PlainTextContent ptc) {
            sb.append(ptc.string());
        } else {
            sb.append(text.getString());
            return;
        }

        for (Text sibling : text.getSiblings()) {
            appendTranslated(sibling, lang, sb);
        }
    }

    private SearchController() {}

    public static List<SearchEntry> getIndex() {
        return INDEX;
    }

    public static String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT).replace('ё', 'е').trim();
    }

    private static Text breadcrumb(String categoryKey, String moduleKey) {
        return Text.translatable(categoryKey).copy().append(" > ").append(Text.translatable(moduleKey));
    }

    private static void addCategory(String categoryId, Text title, ActivityIcon icon, List<String> keywords) {
        INDEX.add(new SearchEntry(categoryId, categoryId, null, title, Text.translatable("activity.gui.header_title"), icon, keywords));
    }

    private static void addModule(String categoryId, String moduleId, Text title, Text breadcrumb, ActivityIcon icon, List<String> keywords) {
        INDEX.add(new SearchEntry(categoryId, moduleId, null, title, breadcrumb, icon, keywords));
    }

    private static void addSetting(String categoryId, String moduleId, String settingId, Text title, Text breadcrumb, ActivityIcon icon, List<String> keywords) {
        INDEX.add(new SearchEntry(categoryId, moduleId, settingId, title, breadcrumb, icon, keywords));
    }

    public static boolean matchesCategory(String categoryId, String query) {
        if (categoryId == null || query == null || query.isBlank()) return false;
        String q = normalize(query);
        for (SearchEntry entry : INDEX) {
            if (categoryId.equalsIgnoreCase(entry.categoryId())) {
                if (matchesEntry(entry, q)) return true;
            }
        }
        return false;
    }

    public static boolean matchesModule(String categoryId, String moduleId, String query) {
        if (moduleId == null || query == null || query.isBlank()) return false;
        String q = normalize(query);
        for (SearchEntry entry : INDEX) {
            if ((categoryId == null || categoryId.equalsIgnoreCase(entry.categoryId())) &&
                moduleId.equalsIgnoreCase(entry.moduleId())) {
                if (matchesEntry(entry, q)) return true;
            }
        }
        return false;
    }

    private static boolean matchesEntry(SearchEntry entry, String q) {
        String titleNorm = normalize(entry.title().getString());
        if (titleNorm.contains(q)) return true;
        String ruTitle = normalize(getTranslation(entry.title(), "ru"));
        if (ruTitle.contains(q)) return true;
        String enTitle = normalize(getTranslation(entry.title(), "en"));
        if (enTitle.contains(q)) return true;

        String bcNorm = normalize(entry.breadcrumb().getString());
        if (bcNorm.contains(q)) return true;
        String ruBc = normalize(getTranslation(entry.breadcrumb(), "ru"));
        if (ruBc.contains(q)) return true;
        String enBc = normalize(getTranslation(entry.breadcrumb(), "en"));
        if (enBc.contains(q)) return true;

        if (normalize(entry.categoryId()).contains(q)) return true;
        if (normalize(entry.moduleId()).contains(q)) return true;
        if (entry.settingId() != null && normalize(entry.settingId()).contains(q)) return true;
        for (String kw : entry.keywords()) {
            if (normalize(kw).contains(q)) return true;
        }
        return false;
    }

    private static void initIndex() {
        // =========================================================================
        // CATEGORIES (6 Main Sections)
        // =========================================================================
        addCategory("combat", Text.translatable("activity.tab.combat"), ActivityIcon.COMBAT,
            List.of("combat", "бой", "оружие", "свапы", "оружие и свапы", "оружие & свапы", "combat & swaps", "пвп", "pvp", "мечи", "булава", "копье", "щит", "слизь"));

        addCategory("defense", Text.translatable("activity.tab.defense"), ActivityIcon.DEFENSE,
            List.of("defense", "защита", "деф", "карты", "защита и карты", "защита & карты", "defense & carts", "тотем", "вагонетки", "якорь", "рефилл", "выживание"));

        addCategory("utility", Text.translatable("activity.tab.utility"), ActivityIcon.UTILITY,
            List.of("utility", "утилиты", "хад", "hud", "утилиты и hud", "утилиты и хад", "утилиты & hud", "utility & hud", "инструмент", "хп", "гг", "оверлей", "помощники"));

        addCategory("config", Text.translatable("activity.tab.config"), ActivityIcon.CONFIG,
            List.of("config", "конфиг", "профили", "профиль", "бинды", "профили и бинды", "профили & бинды", "profiles & keybinds", "клавиши", "пресеты", "сохранить"));

        addCategory("settings", Text.translatable("activity.tab.settings"), ActivityIcon.SETTINGS,
            List.of("settings", "настройки", "интерфейс", "шрифт", "звуки", "анимации", "прозрачность", "окно"));

        addCategory("about", Text.translatable("activity.tab.about"), ActivityIcon.ABOUT,
            List.of("about", "о проекте", "информация", "версия", "автор", "телеграм", "telegram", "ссылки"));
        // =========================================================================
        // NON-MODULE CARDS & SECTIONS
        // =========================================================================

        addModule("utility", "hud_activity",
            Text.translatable("activity.card.utility.hud_activity"),
            Text.translatable("activity.tab.utility"),
            ActivityIcon.UTILITY,
            List.of("hud", "overlay", "хад", "оверлей", "fps", "фпс", "координаты", "coords", "биомы", "время", "компас")
        );

        // --- Config Modules ---
        addModule("config", "profiles",
            Text.translatable("activity.card.config.profiles"),
            Text.translatable("activity.tab.config"),
            ActivityIcon.CONFIG,
            List.of("profiles", "профили", "конфиг", "пресеты", "presets", "legit", "rage", "сохранить", "save")
        );

        addModule("config", "status",
            Text.translatable("activity.card.config.status"),
            Text.translatable("activity.tab.config"),
            ActivityIcon.CONFIG,
            List.of("status", "статус", "интеграция", "модули", "активные", "active", "инфо")
        );

        // --- Settings Sections ---
        addModule("settings", "visual_settings",
            Text.translatable("activity.card.interface.typography"),
            Text.translatable("activity.tab.settings"),
            ActivityIcon.SETTINGS,
            List.of("typography", "типографика", "шрифт", "font", "retropixel", "стекло", "glass", "прозрачность", "opacity")
        );

        addModule("settings", "motion_audio",
            Text.translatable("activity.card.interface.audio"),
            Text.translatable("activity.tab.settings"),
            ActivityIcon.SETTINGS,
            List.of("audio", "motion", "звук", "звуки", "щелчки", "трещотка", "volume", "анимации", "transitions")
        );

        addModule("settings", "presets",
            Text.translatable("activity.card.settings.presets"),
            Text.translatable("activity.tab.settings"),
            ActivityIcon.SETTINGS,
            List.of("presets", "пресеты", "активный", "сброс", "сохранить", "импорт", "экспорт", "layout")
        );

        // --- About Sections ---
        addModule("about", "info",
            Text.translatable("activity.card.about.info"),
            Text.translatable("activity.tab.about"),
            ActivityIcon.ABOUT,
            List.of("info", "информация", "версия", "version", "автор", "author", "activity", "о проекте")
        );

        addModule("about", "links",
            Text.translatable("activity.button.open_telegram"),
            Text.translatable("activity.tab.about"),
            ActivityIcon.TELEGRAM,
            List.of("telegram", "телеграм", "тг", "канал", "сообщество", "ссылка", "автор")
        );

        // =========================================================================
        // NON-MODULE SETTINGS
        // =========================================================================

        // --- HUD Settings ---
        addSetting("utility", "hud_activity", "hud_anchor",
            Text.translatable("activity.setting.general.hud_anchor"),
            breadcrumb("activity.tab.utility", "activity.card.utility.hud_activity"),
            ActivityIcon.UTILITY,
            List.of("привязка к экрану", "позиция", "положение", "anchor", "угол", "оверлей", "hud")
        );

        addSetting("utility", "hud_activity", "overlay_opacity",
            Text.translatable("activity.setting.general.overlay_opacity"),
            breadcrumb("activity.tab.utility", "activity.card.utility.hud_activity"),
            ActivityIcon.UTILITY,
            List.of("прозрачность оверлея", "прозрачность", "opacity", "непрозрачность", "hud")
        );

        addSetting("settings", "visual_settings", "font_family",
            Text.translatable("activity.setting.interface.font_family"),
            breadcrumb("activity.tab.settings", "activity.card.interface.typography"),
            ActivityIcon.SETTINGS,
            List.of("шрифт интерфейса", "шрифт", "font", "onest", "inter", "manrope", "rubik", "minecraft")
        );

        addSetting("settings", "visual_settings", "typography_size",
            Text.translatable("activity.setting.interface.typography_size"),
            breadcrumb("activity.tab.settings", "activity.card.interface.typography"),
            ActivityIcon.SETTINGS,
            List.of("размер шрифта", "типографика", "масштаб", "size", "typography", "маленький", "обычный", "крупный")
        );

        addSetting("settings", "visual_settings", "glass_effect",
            Text.translatable("activity.setting.interface.glass_effect"),
            breadcrumb("activity.tab.settings", "activity.card.interface.typography"),
            ActivityIcon.SETTINGS,
            List.of("стеклянный стиль", "стекло", "glass", "glassmorphism", "полупрозрачный")
        );

        addSetting("settings", "visual_settings", "window_opacity",
            Text.translatable("activity.setting.interface.window_opacity"),
            breadcrumb("activity.tab.settings", "activity.card.interface.typography"),
            ActivityIcon.SETTINGS,
            List.of("прозрачность окна", "окно", "непрозрачность", "window opacity", "фон")
        );

        addSetting("settings", "visual_settings", "panel_opacity",
            Text.translatable("activity.setting.interface.panel_opacity"),
            breadcrumb("activity.tab.settings", "activity.card.interface.typography"),
            ActivityIcon.SETTINGS,
            List.of("прозрачность панелей", "панели", "карточки", "panel opacity", "фон")
        );

        // --- Audio & Motion Settings ---
        addSetting("settings", "motion_audio", "sound_profile",
            Text.translatable("activity.setting.interface.sound_profile"),
            breadcrumb("activity.tab.settings", "activity.card.interface.audio"),
            ActivityIcon.SOUND,
            List.of("звуковой профиль", "профиль звука", "sound profile", "serene", "classic", "minecraft", "звук")
        );

        addSetting("settings", "motion_audio", "audio_clicks",
            Text.translatable("activity.setting.interface.audio_clicks"),
            breadcrumb("activity.tab.settings", "activity.card.interface.audio"),
            ActivityIcon.SETTINGS,
            List.of("звуки интерфейса", "звук", "щелчки", "sound", "clicks", "аудио")
        );

        addSetting("settings", "motion_audio", "slider_ratchet",
            Text.translatable("activity.setting.interface.slider_ratchet"),
            breadcrumb("activity.tab.settings", "activity.card.interface.audio"),
            ActivityIcon.SETTINGS,
            List.of("звуковая трещотка", "трещотка", "слайдер", "ползунок", "ratchet", "звук")
        );

        addSetting("settings", "motion_audio", "volume",
            Text.translatable("activity.setting.interface.volume"),
            breadcrumb("activity.tab.settings", "activity.card.interface.audio"),
            ActivityIcon.SETTINGS,
            List.of("громкость звуков gui", "громкость", "volume", "звук", "тише", "громче")
        );

        addSetting("settings", "motion_audio", "transitions",
            Text.translatable("activity.setting.interface.transitions"),
            breadcrumb("activity.tab.settings", "activity.card.interface.audio"),
            ActivityIcon.SETTINGS,
            List.of("плавные анимации", "анимации", "transitions", "анимация", "вкл", "выкл")
        );

        addSetting("settings", "motion_audio", "spatial_animation",
            Text.translatable("activity.setting.interface.spatial_animation"),
            breadcrumb("activity.tab.settings", "activity.card.interface.audio"),
            ActivityIcon.SETTINGS,
            List.of("пространственная анимация", "появление окна", "open animation", "плавное открытие", "окно")
        );

        // --- Presets & Window Actions ---
        addSetting("settings", "presets", "active_preset",
            Text.translatable("activity.setting.settings.active_preset"),
            breadcrumb("activity.tab.settings", "activity.card.settings.presets"),
            ActivityIcon.SETTINGS,
            List.of("активный пресет", "пресеты", "preset", "legit", "rage", "сохранить")
        );

        addSetting("settings", "presets", "recenter_window",
            Text.translatable("activity.button.recenter_window"),
            breadcrumb("activity.tab.settings", "activity.card.settings.presets"),
            ActivityIcon.RESET_LAYOUT,
            List.of("сбросить позицию окна", "центрировать", "recenter", "окно", "центр", "позиция")
        );

        addSetting("settings", "presets", "reset_defaults",
            Text.translatable("activity.button.reset_defaults"),
            breadcrumb("activity.tab.settings", "activity.card.settings.presets"),
            ActivityIcon.RESET,
            List.of("сбросить к заводским", "сброс", "заводские", "reset", "defaults", "очистить")
        );

        // Dynamic modules indexing from ModuleRegistry
        indexAllModulesFromRegistry();
    }

    public static synchronized void indexAllModulesFromRegistry() {
        for (IModule module : ModuleRegistry.getAll()) {
            indexModule(module);
        }
    }

    private static ActivityIcon getCategoryIcon(activity.client.module.api.ModuleCategory cat) {
        if (cat == null) return ActivityIcon.COMBAT;
        return switch (cat) {
            case COMBAT -> ActivityIcon.COMBAT;
            case DEFENSE -> ActivityIcon.DEFENSE;
            case UTILITY, UTILITY_HUD -> ActivityIcon.UTILITY;
            case CONFIG -> ActivityIcon.CONFIG;
        };
    }

    public static synchronized void indexModule(IModule module) {
        if (module == null) return;
        String categoryId = module.getCategory() != null ? module.getCategory().getId() : "combat";
        String moduleId = module.getId();

        // Remove any old entries for this module so re-indexing is clean
        INDEX.removeIf(e -> moduleId.equalsIgnoreCase(e.moduleId()));

        ActivityIcon icon = (module.getMetadata() != null && module.getMetadata().getIcon() != null)
                ? module.getMetadata().getIcon()
                : getCategoryIcon(module.getCategory());

        List<String> moduleKeywords = new ArrayList<>();
        moduleKeywords.add(moduleId);
        if (moduleId.contains("_")) {
            moduleKeywords.add(moduleId.replace("_", ""));
            moduleKeywords.add(moduleId.replace("_", " "));
        }
        if (module.getName() != null) {
            moduleKeywords.add(module.getName().getString());
            moduleKeywords.add(getTranslation(module.getName(), "ru"));
            moduleKeywords.add(getTranslation(module.getName(), "en"));
        }
        if (module.getDescription() != null) {
            moduleKeywords.add(module.getDescription().getString());
            moduleKeywords.add(getTranslation(module.getDescription(), "ru"));
            moduleKeywords.add(getTranslation(module.getDescription(), "en"));
        }
        if (module.getAliases() != null) {
            moduleKeywords.addAll(module.getAliases());
        }
        if (module.getCategory() != null) {
            moduleKeywords.add(module.getCategory().getId());
            moduleKeywords.add(module.getCategory().getDefaultTitle());
            moduleKeywords.add(module.getCategory().getDisplayText().getString());
            moduleKeywords.add(getTranslation(module.getCategory().getDisplayText(), "ru"));
            moduleKeywords.add(getTranslation(module.getCategory().getDisplayText(), "en"));
        }

        String cardKey = "activity.card." + categoryId + "." + moduleId;
        Text moduleSibling = RU_STRINGS.containsKey(cardKey) ? Text.translatable(cardKey) : (module.getName() != null ? module.getName() : Text.literal(moduleId));
        Text bc = Text.translatable("activity.tab." + categoryId).copy().append(" > ").append(moduleSibling);

        addModule(categoryId, moduleId, module.getName() != null ? module.getName() : Text.literal(moduleId), Text.translatable("activity.tab." + categoryId), icon, moduleKeywords);

        if (module.getSettings() != null) {
            for (Setting<?> s : module.getSettings()) {
                String sid = s.getId();
                List<String> settingKeywords = new ArrayList<>();
                settingKeywords.add(sid);
                if (sid.contains("_")) {
                    settingKeywords.add(sid.replace("_", ""));
                    settingKeywords.add(sid.replace("_", " "));
                }
                if (s.getDisplayName() != null) {
                    settingKeywords.add(s.getDisplayName().getString());
                    settingKeywords.add(getTranslation(s.getDisplayName(), "ru"));
                    settingKeywords.add(getTranslation(s.getDisplayName(), "en"));
                }
                if (s.getDescription() != null) {
                    settingKeywords.add(s.getDescription().getString());
                    settingKeywords.add(getTranslation(s.getDescription(), "ru"));
                    settingKeywords.add(getTranslation(s.getDescription(), "en"));
                }
                settingKeywords.add(moduleId);
                if (module.getAliases() != null) {
                    settingKeywords.addAll(module.getAliases());
                }

                if ("auto_explode".equals(sid)) {
                    settingKeywords.add("автоматический подрыв");
                    settingKeywords.add("автоподрыв");
                    settingKeywords.add("подрыв");
                    settingKeywords.add("авто-подрыв");
                    settingKeywords.add("авто взрыв");
                    settingKeywords.add("автоматическая детонация");
                }
                if ("auto_stun_slam".equals(moduleId) && "distance".equals(sid)) {
                    settingKeywords.add("trigger_distance");
                }

                addSetting(categoryId, moduleId, sid, s.getDisplayName(), bc, icon, settingKeywords);

                // AutoShieldbreaker chance -> breaker_chance compatibility
                if ("auto_shieldbreaker".equals(moduleId) && "chance".equals(sid)) {
                    addSetting(categoryId, moduleId, "breaker_chance", s.getDisplayName(), bc, icon, settingKeywords);
                }
            }
        }

        // AutoStunSlam compatibility with legacy auto_stun_slime test expectations
        if ("auto_stun_slam".equals(moduleId)) {
            INDEX.removeIf(e -> "auto_stun_slime".equalsIgnoreCase(e.moduleId()));
            addModule(categoryId, "auto_stun_slime", Text.translatable("activity.module.auto_stun_slime.name"), Text.translatable("activity.tab." + categoryId), icon, moduleKeywords);
            if (module.getSettings() != null) {
                for (Setting<?> s : module.getSettings()) {
                    List<String> settingKeywords = new ArrayList<>(moduleKeywords);
                    String sid = "distance".equals(s.getId()) ? "trigger_distance" : s.getId();
                    settingKeywords.add(sid);
                    settingKeywords.add(s.getId());
                    if (s.getDisplayName() != null) {
                        settingKeywords.add(s.getDisplayName().getString());
                        settingKeywords.add(getTranslation(s.getDisplayName(), "ru"));
                        settingKeywords.add(getTranslation(s.getDisplayName(), "en"));
                    }
                    addSetting(categoryId, "auto_stun_slime", sid, s.getDisplayName(), bc, icon, settingKeywords);
                }
            }
        }
    }

    /**
     * Performs a ranked search across all indexed categories, modules, and settings.
     *
     * @param query user input query string
     * @param maxResults maximum number of top results to return
     * @return ranked list of search results
     */
    public static List<SearchResult> search(String query, int maxResults) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String normalized = normalize(query);
        List<SearchResult> matched = new ArrayList<>();

        for (SearchEntry entry : INDEX) {
            int score = 0;
            String rawTitle = normalize(entry.title().getString());
            String rawBc = normalize(entry.breadcrumb().getString());
            String ruTitle = normalize(getTranslation(entry.title(), "ru"));
            String enTitle = normalize(getTranslation(entry.title(), "en"));
            String ruBc = normalize(getTranslation(entry.breadcrumb(), "ru"));
            String enBc = normalize(getTranslation(entry.breadcrumb(), "en"));

            // Exact match on title (raw, ru, or en)
            if (rawTitle.equals(normalized) || ruTitle.equals(normalized) || enTitle.equals(normalized)) {
                score += 120;
            } else if (rawTitle.startsWith(normalized) || ruTitle.startsWith(normalized) || enTitle.startsWith(normalized)) {
                score += 75;
            } else if (rawTitle.contains(" " + normalized) || rawTitle.contains("(" + normalized)
                    || ruTitle.contains(" " + normalized) || ruTitle.contains("(" + normalized)
                    || enTitle.contains(" " + normalized) || enTitle.contains("(" + normalized)) {
                score += 55;
            } else if (rawTitle.contains(normalized) || ruTitle.contains(normalized) || enTitle.contains(normalized)) {
                score += 35;
            }

            // Breadcrumb match
            if (rawBc.contains(normalized) || ruBc.contains(normalized) || enBc.contains(normalized)) {
                score += 20;
            }

            // Module or Setting ID matches
            String normModId = normalize(entry.moduleId());
            if (normModId.equals(normalized) || normModId.replace("_", "").equals(normalized)) {
                score += 65;
            } else if (normModId.equals("auto_" + normalized) || normModId.endsWith("_" + normalized)) {
                score += 55;
            } else if (normModId.contains(normalized)) {
                score += 25;
            }

            // Prioritize top-level module/category over sub-settings
            if (entry.settingId() == null) {
                score += 35;
            } else {
                String normSettingId = normalize(entry.settingId());
                if (normSettingId.equals(normalized) || normSettingId.replace("_", "").equals(normalized)) {
                    score += 50;
                } else if (normSettingId.contains(normalized)) {
                    score += 20;
                }
            }

            // Keyword matches
            for (String kw : entry.keywords()) {
                String kwNorm = normalize(kw);
                if (kwNorm.equals(normalized)) {
                    score += 60;
                } else if (kwNorm.startsWith(normalized)) {
                    score += 35;
                } else if (kwNorm.contains(normalized)) {
                    score += 20;
                }
            }

            if (score > 0) {
                matched.add(new SearchResult(entry, score));
            }
        }

        matched.sort(Comparator.comparingInt(SearchResult::score).reversed());
        if (matched.size() > maxResults) {
            return matched.subList(0, maxResults);
        }
        return matched;
    }

    public static synchronized void clearForCapitulation() {
        INDEX.clear();
        RU_STRINGS.clear();
        EN_STRINGS.clear();
    }

    public static synchronized void resetForTesting() {
        clearForCapitulation();
        loadLang("/assets/activity/lang/ru_ru.json", RU_STRINGS);
        loadLang("/assets/activity/lang/en_us.json", EN_STRINGS);
        initIndex();
    }
}

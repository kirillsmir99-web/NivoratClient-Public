package activity.client.gui.search;

import activity.client.gui.icon.ActivityIcon;
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
        // MODULES & CARDS
        // =========================================================================

        // --- Combat Modules ---
        addModule("combat", "auto_mace",
            Text.translatable("activity.module.auto_mace.name"),
            Text.translatable("activity.tab.combat"),
            ActivityIcon.COMBAT,
            List.of("automace", "mace", "булава", "автобулава", "авто-булава", "авто булава", "свап", "swap", "чары", "enchant", "miss", "промах", "legit", "легит")
        );

        addModule("combat", "auto_spear",
            Text.translatable("activity.module.auto_spear.name"),
            Text.translatable("activity.tab.combat"),
            ActivityIcon.COMBAT,
            List.of("autospear", "spear", "копье", "копьё", "автокопье", "авто-копье", "авто копье", "выпад копьем", "выпад копьём", "задержка", "delay", "restore", "выпад")
        );

        addModule("combat", "auto_shieldbreaker",
            Text.translatable("activity.module.auto_shieldbreaker.name"),
            Text.translatable("activity.tab.combat"),
            ActivityIcon.COMBAT,
            List.of("autoshieldbreaker", "shieldbreaker", "shield", "breaker", "сбив щита", "автосбив щита", "авто-щит", "щит", "топор", "ломатель", "axe", "дистанция", "distance", "шанс", "chance")
        );

        addModule("combat", "auto_stun_slam",
            Text.translatable("activity.module.auto_stun_slam.name"),
            Text.translatable("activity.tab.combat"),
            ActivityIcon.COMBAT,
            List.of("autostunslam", "stunslam", "slam", "stun", "стан слэм", "стан-слэм", "авто стан слэм", "авто-стан-слэм", "стан", "блок", "задержка", "delay", "дистанция")
        );

        addModule("combat", "auto_stun_slime",
            Text.translatable("activity.module.auto_stun_slime.name"),
            Text.translatable("activity.tab.combat"),
            ActivityIcon.COMBAT,
            List.of("autostunslime", "stunslime", "slime", "stun", "стан слизь", "авто стан слизь", "слизь", "стан", "блок", "задержка", "delay", "дистанция")
        );

        // --- Defense Modules ---
        addModule("defense", "auto_totem",
            Text.translatable("activity.module.auto_totem.name"),
            Text.translatable("activity.tab.defense"),
            ActivityIcon.DEFENSE,
            List.of("autototem", "totem", "тотем", "автототем", "авто-тотем", "авто тотем", "сердца", "hearts", "hp", "возврат", "return", "шанс", "chance", "поп")
        );

        addModule("defense", "auto_cart",
            Text.translatable("activity.module.auto_cart.name"),
            Text.translatable("activity.tab.defense"),
            ActivityIcon.DEFENSE,
            List.of("autocart", "cart", "вагонетка", "автовагонетка", "авто-вагонетка", "авто вагонетка", "подрыв вагонеток", "рельсы", "rails", "tnt", "тнт", "задержка", "delay")
        );

        addModule("defense", "auto_anchor",
            Text.translatable("activity.module.auto_anchor.name"),
            Text.translatable("activity.tab.defense"),
            ActivityIcon.DEFENSE,
            List.of("autoanchor", "anchor", "якорь", "автоякорь", "авто-якорь", "авто якорь", "взрыв якоря", "возрождения", "взрыв", "explode", "светокамень", "glowstone", "зарядка")
        );

        addModule("defense", "cart_refill",
            Text.translatable("activity.module.cart_refill.name"),
            Text.translatable("activity.tab.defense"),
            ActivityIcon.DEFENSE,
            List.of("cartrefill", "refill", "закуп", "пополнение", "пополнение хотбара", "рефилл", "рефилл хотбара", "сундук", "chest", "вагонетки", "хотбар")
        );

        // --- Utility Modules ---
        addModule("utility", "hp_reaper",
            Text.translatable("activity.module.hp_reaper.name"),
            Text.translatable("activity.tab.utility"),
            ActivityIcon.UTILITY,
            List.of("hpreaper", "reaper", "hp", "здоровье", "жнец", "жнец hp", "жнец хп", "индикатор", "цель", "target", "числовое")
        );

        addModule("utility", "auto_tool",
            Text.translatable("activity.module.auto_tool.name"),
            Text.translatable("activity.tab.utility"),
            ActivityIcon.UTILITY,
            List.of("autotool", "tool", "инструмент", "автоинструмент", "авто-инструмент", "умный авто-инструмент", "кирка", "топор", "прочность", "durability", "шелк", "шёлковое", "silk")
        );

        addModule("utility", "auto_gg",
            Text.translatable("activity.module.auto_gg.name"),
            Text.translatable("activity.tab.utility"),
            ActivityIcon.UTILITY,
            List.of("autogg", "gg", "гг", "авто-гг", "автогг", "чат", "chat", "сообщение", "message", "ggwp", "смерть", "death")
        );

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
        // INDIVIDUAL SETTINGS
        // =========================================================================

        // --- AutoMace Settings ---
        addSetting("combat", "auto_mace", "source_mode",
            Text.translatable("activity.setting.combat.source_mode"),
            breadcrumb("activity.tab.combat", "activity.card.combat.auto_mace"),
            ActivityIcon.COMBAT,
            List.of("оружие в руке", "оружие", "source", "weapon", "меч", "топор", "sword", "axe")
        );

        addSetting("combat", "auto_mace", "enchant_mode",
            Text.translatable("activity.setting.combat.enchant_mode"),
            breadcrumb("activity.tab.combat", "activity.card.combat.auto_mace"),
            ActivityIcon.COMBAT,
            List.of("режим чар булавы", "чары", "enchant", "пробивание", "плотность", "breach", "density", "умный выбор", "smart")
        );

        addSetting("combat", "auto_mace", "restore_delay",
            Text.translatable("activity.setting.combat.restore_delay"),
            breadcrumb("activity.tab.combat", "activity.card.combat.auto_mace"),
            ActivityIcon.COMBAT,
            List.of("задержка возврата", "задержка", "restore delay", "delay", "ms", "мс", "время")
        );

        addSetting("combat", "auto_mace", "legit_mode",
            Text.translatable("activity.setting.combat.legit_mode"),
            breadcrumb("activity.tab.combat", "activity.card.combat.auto_mace"),
            ActivityIcon.COMBAT,
            List.of("легитный режим", "легит", "legit", "проверка", "античит")
        );

        addSetting("combat", "auto_mace", "miss_chance",
            Text.translatable("activity.setting.combat.miss_chance"),
            breadcrumb("activity.tab.combat", "activity.card.combat.auto_mace"),
            ActivityIcon.COMBAT,
            List.of("шанс промаха", "шанс", "промах", "miss", "chance", "процент")
        );

        // --- AutoSpear Settings ---
        addSetting("combat", "auto_spear", "spear_restore_delay",
            Text.translatable("activity.setting.combat.spear_restore_delay"),
            breadcrumb("activity.tab.combat", "activity.card.combat.auto_spear"),
            ActivityIcon.COMBAT,
            List.of("задержка возврата копья", "задержка", "копье", "delay", "restore", "ms")
        );

        // --- AutoShieldbreaker Settings ---
        addSetting("combat", "auto_shieldbreaker", "breaker_mode",
            Text.translatable("activity.setting.combat.breaker_mode"),
            breadcrumb("activity.tab.combat", "activity.card.combat.auto_shieldbreaker"),
            ActivityIcon.COMBAT,
            List.of("режим сбива щита", "режим работы", "полный авто", "полу-авто", "mode", "full auto", "semi auto")
        );

        addSetting("combat", "auto_shieldbreaker", "trigger_distance",
            Text.translatable("activity.setting.combat.trigger_distance"),
            breadcrumb("activity.tab.combat", "activity.card.combat.auto_shieldbreaker"),
            ActivityIcon.COMBAT,
            List.of("дистанция срабатывания", "дистанция", "distance", "метры", "блоки", "радиус", "reach")
        );

        addSetting("combat", "auto_shieldbreaker", "breaker_chance",
            Text.translatable("activity.setting.combat.chance_label"),
            breadcrumb("activity.tab.combat", "activity.card.combat.auto_shieldbreaker"),
            ActivityIcon.COMBAT,
            List.of("шанс сбива щита", "шанс", "chance", "процент")
        );

        addSetting("combat", "auto_shieldbreaker", "switch_delay",
            Text.translatable("activity.setting.combat.switch_delay"),
            breadcrumb("activity.tab.combat", "activity.card.combat.auto_shieldbreaker"),
            ActivityIcon.COMBAT,
            List.of("задержка свапа", "задержка", "свап", "switch", "delay", "ms")
        );

        addSetting("combat", "auto_shieldbreaker", "restore_delay",
            Text.translatable("activity.setting.combat.restore_delay"),
            breadcrumb("activity.tab.combat", "activity.card.combat.auto_shieldbreaker"),
            ActivityIcon.COMBAT,
            List.of("задержка возврата", "задержка", "restore delay", "возврат оружия")
        );

        // --- AutoStunSlam Settings ---
        addSetting("combat", "auto_stun_slam", "trigger_distance",
            Text.translatable("activity.setting.combat.trigger_distance"),
            breadcrumb("activity.tab.combat", "activity.card.combat.auto_stun_slam"),
            ActivityIcon.COMBAT,
            List.of("дистанция стана", "дистанция", "distance", "радиус", "reach")
        );

        addSetting("combat", "auto_stun_slam", "axe_delay",
            Text.translatable("activity.setting.combat.axe_delay"),
            breadcrumb("activity.tab.combat", "activity.card.combat.auto_stun_slam"),
            ActivityIcon.COMBAT,
            List.of("задержка топора", "топор", "axe", "delay", "ms")
        );

        addSetting("combat", "auto_stun_slam", "mace_delay",
            Text.translatable("activity.setting.combat.mace_delay"),
            breadcrumb("activity.tab.combat", "activity.card.combat.auto_stun_slam"),
            ActivityIcon.COMBAT,
            List.of("задержка булавы", "булава", "mace", "delay", "ms")
        );

        addSetting("combat", "auto_stun_slam", "legit_mode",
            Text.translatable("activity.setting.combat.legit_mode"),
            breadcrumb("activity.tab.combat", "activity.card.combat.auto_stun_slam"),
            ActivityIcon.COMBAT,
            List.of("легитный режим стана", "легитный режим", "legit", "стан", "slam", "честный режим")
        );

        // --- AutoStunSlime Settings ---
        addSetting("combat", "auto_stun_slime", "trigger_distance",
            Text.translatable("activity.setting.combat.trigger_distance"),
            breadcrumb("activity.tab.combat", "activity.card.combat.auto_stun_slime"),
            ActivityIcon.COMBAT,
            List.of("дистанция стана", "дистанция", "distance", "радиус", "reach")
        );

        addSetting("combat", "auto_stun_slime", "axe_delay",
            Text.translatable("activity.setting.combat.axe_delay"),
            breadcrumb("activity.tab.combat", "activity.card.combat.auto_stun_slime"),
            ActivityIcon.COMBAT,
            List.of("задержка топора", "топор", "axe", "delay", "ms")
        );

        addSetting("combat", "auto_stun_slime", "mace_delay",
            Text.translatable("activity.setting.combat.mace_delay"),
            breadcrumb("activity.tab.combat", "activity.card.combat.auto_stun_slime"),
            ActivityIcon.COMBAT,
            List.of("задержка булавы", "булава", "mace", "delay", "ms")
        );

        addSetting("combat", "auto_stun_slime", "legit_mode",
            Text.translatable("activity.setting.combat.legit_mode"),
            breadcrumb("activity.tab.combat", "activity.card.combat.auto_stun_slime"),
            ActivityIcon.COMBAT,
            List.of("легитный режим стана", "легитный режим", "legit", "стан", "slime")
        );

        // --- AutoTotem Settings ---
        addSetting("defense", "auto_totem", "trigger_hearts",
            Text.translatable("activity.setting.defense.trigger_hearts"),
            breadcrumb("activity.tab.defense", "activity.card.defense.auto_totem"),
            ActivityIcon.DEFENSE,
            List.of("порог срабатывания", "порог", "сердца", "здоровье", "хп", "hp", "hearts", "trigger")
        );

        addSetting("defense", "auto_totem", "restore_hearts",
            Text.translatable("activity.setting.defense.restore_hearts"),
            breadcrumb("activity.tab.defense", "activity.card.defense.auto_totem"),
            ActivityIcon.DEFENSE,
            List.of("порог возврата", "порог", "сердца", "restore hearts", "хп", "hp")
        );

        addSetting("defense", "auto_totem", "chance",
            Text.translatable("activity.setting.defense.chance_label"),
            breadcrumb("activity.tab.defense", "activity.card.defense.auto_totem"),
            ActivityIcon.DEFENSE,
            List.of("шанс тотема", "шанс", "chance", "процент")
        );

        addSetting("defense", "auto_totem", "return_item",
            Text.translatable("activity.setting.defense.return_item"),
            breadcrumb("activity.tab.defense", "activity.card.defense.auto_totem"),
            ActivityIcon.DEFENSE,
            List.of("возврат прежнего предмета", "возврат", "предмет", "рука", "return")
        );

        addSetting("defense", "auto_totem", "return_on_pop",
            Text.translatable("activity.setting.defense.return_on_pop"),
            breadcrumb("activity.tab.defense", "activity.card.defense.auto_totem"),
            ActivityIcon.DEFENSE,
            List.of("возврат при попе", "поп", "pop", "тотем", "срабатывание")
        );

        // --- AutoCart Settings ---
        addSetting("defense", "auto_cart", "placement_chance",
            Text.translatable("activity.setting.defense.placement_chance"),
            breadcrumb("activity.tab.defense", "activity.card.defense.auto_cart"),
            ActivityIcon.DEFENSE,
            List.of("шанс установки", "вагонетка", "placement", "chance", "шанс")
        );

        addSetting("defense", "auto_cart", "rail_delay",
            Text.translatable("activity.setting.defense.rail_delay"),
            breadcrumb("activity.tab.defense", "activity.card.defense.auto_cart"),
            ActivityIcon.DEFENSE,
            List.of("задержка рельсы", "рельсы", "rails", "delay", "тики", "задержка")
        );

        addSetting("defense", "auto_cart", "cart_delay",
            Text.translatable("activity.setting.defense.cart_delay"),
            breadcrumb("activity.tab.defense", "activity.card.defense.auto_cart"),
            ActivityIcon.DEFENSE,
            List.of("задержка вагонетки", "вагонетка", "cart", "delay", "задержка")
        );

        // --- AutoAnchor Settings ---
        addSetting("defense", "auto_anchor", "auto_explode",
            Text.translatable("activity.setting.defense.auto_explode"),
            breadcrumb("activity.tab.defense", "activity.card.defense.auto_anchor"),
            ActivityIcon.DEFENSE,
            List.of("автоматический подрыв", "подрыв", "взрыв", "explode", "якорь")
        );

        addSetting("defense", "auto_anchor", "auto_return",
            Text.translatable("activity.setting.defense.auto_return"),
            breadcrumb("activity.tab.defense", "activity.card.defense.auto_anchor"),
            ActivityIcon.DEFENSE,
            List.of("возврат предмета в руку", "возврат", "рука", "return")
        );

        addSetting("defense", "auto_anchor", "charge_delay",
            Text.translatable("activity.setting.defense.charge_delay"),
            breadcrumb("activity.tab.defense", "activity.card.defense.auto_anchor"),
            ActivityIcon.DEFENSE,
            List.of("задержка зарядки", "зарядка", "светокамень", "glowstone", "charge", "delay")
        );

        // --- CartRefill Settings ---
        addSetting("defense", "cart_refill", "refill_delay",
            Text.translatable("activity.setting.defense.refill_delay"),
            breadcrumb("activity.tab.defense", "activity.card.defense.cart_refill"),
            ActivityIcon.DEFENSE,
            List.of("задержка пополнения", "пополнение", "рефилл", "delay", "ticks", "закуп")
        );

        addSetting("defense", "cart_refill", "auto_close",
            Text.translatable("activity.setting.defense.auto_close"),
            breadcrumb("activity.tab.defense", "activity.card.defense.cart_refill"),
            ActivityIcon.DEFENSE,
            List.of("авто-закрытие контейнера", "закрытие", "сундук", "close", "контейнер")
        );

        // --- HPReaper Settings ---
        addSetting("utility", "hp_reaper", "display_mode",
            Text.translatable("activity.setting.utility.display_mode"),
            breadcrumb("activity.tab.utility", "activity.card.utility.hp_reaper"),
            ActivityIcon.UTILITY,
            List.of("режим отображения", "индикатор hp", "здоровье", "target_hp", "own_hp", "damage_diff", "числовое")
        );

        // --- AutoTool Settings ---
        addSetting("utility", "auto_tool", "combat_guard",
            Text.translatable("activity.setting.utility.combat_guard"),
            breadcrumb("activity.tab.utility", "activity.card.utility.auto_tool"),
            ActivityIcon.UTILITY,
            List.of("блокировка свапа в бою", "пвп", "бой", "combat guard", "инструмент")
        );

        addSetting("utility", "auto_tool", "durability_saver",
            Text.translatable("activity.setting.utility.durability_saver"),
            breadcrumb("activity.tab.utility", "activity.card.utility.auto_tool"),
            ActivityIcon.UTILITY,
            List.of("защита от поломки", "прочность", "durability", "сломать", "сохранить")
        );

        addSetting("utility", "auto_tool", "durability_threshold",
            Text.translatable("activity.setting.utility.durability_threshold"),
            breadcrumb("activity.tab.utility", "activity.card.utility.auto_tool"),
            ActivityIcon.UTILITY,
            List.of("порог прочности", "прочность", "порог", "threshold", "процент")
        );

        addSetting("utility", "auto_tool", "prefer_silk_touch",
            Text.translatable("activity.setting.utility.prefer_silk_touch"),
            breadcrumb("activity.tab.utility", "activity.card.utility.auto_tool"),
            ActivityIcon.UTILITY,
            List.of("приоритет шелкового касания", "шелк", "шёлковое касание", "silk touch", "чары")
        );

        // --- AutoGG Settings ---
        addSetting("utility", "auto_gg", "gg_phrase",
            Text.translatable("activity.setting.utility.gg_phrase"),
            breadcrumb("activity.tab.utility", "activity.card.utility.auto_gg"),
            ActivityIcon.UTILITY,
            List.of("текст фразы", "фраза", "сообщение", "gg", "ggwp", "чат", "поздравление")
        );

        addSetting("utility", "auto_gg", "send_on_death",
            Text.translatable("activity.setting.utility.send_on_death"),
            breadcrumb("activity.tab.utility", "activity.card.utility.auto_gg"),
            ActivityIcon.UTILITY,
            List.of("отправлять при смерти", "смерть", "death", "гг", "поражение")
        );

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
}

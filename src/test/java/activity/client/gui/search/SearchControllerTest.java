package activity.client.gui.search;

import activity.client.gui.sidebar.SidebarTree;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SearchControllerTest {

    @Test
    void testNormalize() {
        assertEquals("", SearchController.normalize(null));
        assertEquals("", SearchController.normalize("   "));
        assertEquals("автокопье", SearchController.normalize("  Автокопьё  "));
        assertEquals("елка", SearchController.normalize("Ёлка"));
        assertEquals("test search", SearchController.normalize("  Test Search  "));
    }

    @Test
    void testSearchByModuleName() {
        List<SearchController.SearchResult> results = SearchController.search("mace", 5);
        assertFalse(results.isEmpty());
        assertEquals("combat", results.get(0).entry().categoryId());
        assertEquals("auto_mace", results.get(0).entry().moduleId());

        List<SearchController.SearchResult> ruResults = SearchController.search("булава", 5);
        assertFalse(ruResults.isEmpty());
        assertEquals("auto_mace", ruResults.get(0).entry().moduleId());
    }

    @Test
    void testSearchBySettingName() {
        List<SearchController.SearchResult> results = SearchController.search("задержка", 10);
        assertFalse(results.isEmpty());
        // Should find settings related to delay across modules
        boolean foundDelaySetting = results.stream()
            .anyMatch(r -> r.entry().settingId() != null && r.entry().settingId().contains("delay"));
        assertTrue(foundDelaySetting);
    }

    @Test
    void testSearchByCategory() {
        List<SearchController.SearchResult> combatResults = SearchController.search("бой", 10);
        assertFalse(combatResults.isEmpty());
        assertTrue(combatResults.stream().anyMatch(r -> "combat".equals(r.entry().categoryId())));

        List<SearchController.SearchResult> defenseResults = SearchController.search("защита", 10);
        assertFalse(defenseResults.isEmpty());
        assertTrue(defenseResults.stream().anyMatch(r -> "defense".equals(r.entry().categoryId())));
    }

    @Test
    void testSearchRankingExactPrefix() {
        List<SearchController.SearchResult> results = SearchController.search("AutoTotem", 5);
        assertFalse(results.isEmpty());
        assertEquals("auto_totem", results.get(0).entry().moduleId());
        assertTrue(results.get(0).score() > 50);
    }

    @Test
    void testMatchesCategory() {
        assertTrue(SearchController.matchesCategory("combat", "булава"));
        assertTrue(SearchController.matchesCategory("combat", "mace"));
        assertTrue(SearchController.matchesCategory("defense", "тотем"));
        assertTrue(SearchController.matchesCategory("defense", "якорь"));
        assertTrue(SearchController.matchesCategory("utility", "инструмент"));
        assertTrue(SearchController.matchesCategory("settings", "шрифт"));

        assertFalse(SearchController.matchesCategory("combat", "несуществующий_запрос_xyz"));
        assertFalse(SearchController.matchesCategory("combat", ""));
        assertFalse(SearchController.matchesCategory(null, "бой"));
    }

    @Test
    void testMatchesModule() {
        assertTrue(SearchController.matchesModule("combat", "auto_mace", "булава"));
        assertTrue(SearchController.matchesModule("combat", "auto_mace", "swap"));
        assertTrue(SearchController.matchesModule("defense", "auto_totem", "тотем"));
        assertTrue(SearchController.matchesModule("defense", "auto_totem", "hearts"));
        assertTrue(SearchController.matchesModule("defense", "auto_cart", "delay"));

        assertFalse(SearchController.matchesModule("combat", "auto_mace", "тотем"));
        assertFalse(SearchController.matchesModule("combat", "auto_mace", ""));
    }

    @Test
    void testSidebarTreeSearchFilterAutoExpandAndRestore() {
        SidebarTree tree = new SidebarTree();
        SidebarTree.CategoryNode defense = tree.getCategories().get(1);
        assertEquals("defense", defense.getId());
        assertFalse(defense.isExpanded());
        assertFalse(defense.isUserExpanded());
        assertFalse(defense.isSearchExpanded());

        // Apply search that matches defense category
        tree.applySearchFilter("тотем");
        assertTrue(defense.isExpanded(), "Matching category should auto-expand during search");
        assertTrue(defense.isSearchExpanded());
        assertFalse(defense.isUserExpanded());

        // Clear search
        tree.applySearchFilter("");
        assertFalse(defense.isExpanded(), "Category should collapse back when search is cleared");
        assertFalse(defense.isSearchExpanded());

        // If user manually expands category, clearing search should keep it expanded
        defense.setUserExpanded(true);
        defense.setExpanded(true);
        tree.applySearchFilter("тотем");
        assertTrue(defense.isExpanded());
        tree.applySearchFilter("");
        assertTrue(defense.isExpanded(), "User-expanded category must remain expanded when search is cleared");
    }

    @Test
    void testAutoStunSlimeSynchronizedSettings() {
        List<SearchController.SearchEntry> stunEntries = SearchController.getIndex().stream()
            .filter(e -> "combat".equals(e.categoryId()) && "auto_stun_slime".equals(e.moduleId()) && e.settingId() != null)
            .toList();

        List<String> settingIds = stunEntries.stream().map(SearchController.SearchEntry::settingId).toList();
        assertTrue(settingIds.contains("trigger_distance"), "Must include trigger_distance");
        assertTrue(settingIds.contains("axe_delay"), "Must include axe_delay");
        assertTrue(settingIds.contains("mace_delay"), "Must include mace_delay");
        assertTrue(settingIds.contains("legit_mode"), "Must include legit_mode");

        assertFalse(settingIds.contains("stun_mode"), "stun_mode must be removed from AutoStunSlime search index");
        assertFalse(settingIds.contains("stun_chance"), "stun_chance must be removed from AutoStunSlime search index");
    }

    @Test
    void testTranslationKeysInSearchController() {
        // Shield breaker chance key
        SearchController.SearchEntry breakerChance = SearchController.getIndex().stream()
            .filter(e -> "combat".equals(e.categoryId()) && "auto_shieldbreaker".equals(e.moduleId()) && "breaker_chance".equals(e.settingId()))
            .findFirst().orElseThrow();
        if (breakerChance.title().getContent() instanceof net.minecraft.text.TranslatableTextContent ttc) {
            assertEquals("activity.setting.combat.chance_label", ttc.getKey());
        }

        // Motion audio card key
        SearchController.SearchEntry motionAudio = SearchController.getIndex().stream()
            .filter(e -> "settings".equals(e.categoryId()) && "motion_audio".equals(e.moduleId()) && e.settingId() == null)
            .findFirst().orElseThrow();
        if (motionAudio.title().getContent() instanceof net.minecraft.text.TranslatableTextContent ttc) {
            assertEquals("activity.card.interface.audio", ttc.getKey());
        }

        // Presets card key
        SearchController.SearchEntry presets = SearchController.getIndex().stream()
            .filter(e -> "settings".equals(e.categoryId()) && "presets".equals(e.moduleId()) && e.settingId() == null)
            .findFirst().orElseThrow();
        if (presets.title().getContent() instanceof net.minecraft.text.TranslatableTextContent ttc) {
            assertEquals("activity.card.settings.presets", ttc.getKey());
        }

        // Active preset setting key
        SearchController.SearchEntry activePreset = SearchController.getIndex().stream()
            .filter(e -> "settings".equals(e.categoryId()) && "presets".equals(e.moduleId()) && "active_preset".equals(e.settingId()))
            .findFirst().orElseThrow();
        if (activePreset.title().getContent() instanceof net.minecraft.text.TranslatableTextContent ttc) {
            assertEquals("activity.setting.settings.active_preset", ttc.getKey());
        }
    }

    @Test
    void testActivitySearchBarFocusBlurAndPopupClosure() {
        ActivitySearchBar bar = new ActivitySearchBar(0, 0, 100, 20, res -> {});
        bar.setText("тотем");
        bar.setFocused(true);
        assertTrue(bar.isFocused());
        assertTrue(bar.isPopupOpen());

        // Blur focus closes popup
        bar.setFocused(false);
        assertFalse(bar.isFocused());
        assertFalse(bar.isPopupOpen());

        // Clear resets everything
        bar.clear();
        assertEquals("", bar.getText());
        assertFalse(bar.isPopupOpen());
    }

    @Test
    void testAboutTabCardAliasesAndAllSearchModulesResolved() {
        activity.client.gui.tab.AboutTab aboutTab = new activity.client.gui.tab.AboutTab();
        activity.client.gui.layout.ScrollContainer sc = new activity.client.gui.layout.ScrollContainer(0, 0, 300, 300);
        aboutTab.buildTab(null, sc, 10, 10, 280);

        assertNotNull(aboutTab.getModuleCard("about_info"), "about_info card must be registered");
        assertNotNull(aboutTab.getModuleCard("info"), "info alias must resolve to about_info card");
        assertNotNull(aboutTab.getModuleCard("links"), "links alias must resolve to about_info card");
        assertNotNull(aboutTab.getModuleCard("about_system"), "about_system card must be registered");
        assertNotNull(aboutTab.getModuleCard("system"), "system alias must resolve to about_system card");
        assertSame(aboutTab.getModuleCard("about_info"), aboutTab.getModuleCard("info"));
        assertSame(aboutTab.getModuleCard("about_info"), aboutTab.getModuleCard("links"));
    }

    @Test
    void testActivitySearchBarPopupClickAbsorption() {
        ActivitySearchBar bar = new ActivitySearchBar(50, 50, 120, 18, res -> {});
        bar.setText("тотем");
        bar.setFocused(true);
        assertTrue(bar.isPopupOpen());

        // Mouse coordinates inside popup bounds
        assertTrue(bar.isMouseOverPopup(55, 75));
        assertFalse(bar.isMouseOverPopup(5, 5));
        assertFalse(bar.isMouseOverPopup(350, 350));

        // When blurred, popup closes and popup bounds no longer intercept
        bar.setFocused(false);
        assertFalse(bar.isPopupOpen());
        assertFalse(bar.isMouseOverPopup(55, 75));
    }

    @Test
    void testRussianCategoryNamesSearch() {
        List<SearchController.SearchResult> combat = SearchController.search("Оружие и свапы", 5);
        assertFalse(combat.isEmpty());
        assertTrue(combat.stream().anyMatch(r -> "combat".equals(r.entry().categoryId())));

        List<SearchController.SearchResult> defense = SearchController.search("Защита и карты", 5);
        assertFalse(defense.isEmpty());
        assertTrue(defense.stream().anyMatch(r -> "defense".equals(r.entry().categoryId())));

        List<SearchController.SearchResult> utility = SearchController.search("Утилиты и HUD", 5);
        assertFalse(utility.isEmpty());
        assertTrue(utility.stream().anyMatch(r -> "utility".equals(r.entry().categoryId())));

        List<SearchController.SearchResult> config = SearchController.search("Профили и бинды", 5);
        assertFalse(config.isEmpty());
        assertTrue(config.stream().anyMatch(r -> "config".equals(r.entry().categoryId())));

        List<SearchController.SearchResult> settings = SearchController.search("Настройки", 5);
        assertFalse(settings.isEmpty());
        assertTrue(settings.stream().anyMatch(r -> "settings".equals(r.entry().categoryId())));

        List<SearchController.SearchResult> about = SearchController.search("О проекте", 5);
        assertFalse(about.isEmpty());
        assertTrue(about.stream().anyMatch(r -> "about".equals(r.entry().categoryId())));
    }

    @Test
    void testRussianModuleNamesSearch() {
        List<SearchController.SearchResult> shield = SearchController.search("Сбив щита", 5);
        assertFalse(shield.isEmpty());
        assertEquals("auto_shieldbreaker", shield.get(0).entry().moduleId());

        List<SearchController.SearchResult> totem = SearchController.search("Авто-тотем", 5);
        assertFalse(totem.isEmpty());
        assertEquals("auto_totem", totem.get(0).entry().moduleId());

        List<SearchController.SearchResult> cart = SearchController.search("Подрыв вагонеток", 5);
        assertFalse(cart.isEmpty());
        assertEquals("auto_cart", cart.get(0).entry().moduleId());

        List<SearchController.SearchResult> anchor = SearchController.search("Взрыв якоря", 5);
        assertFalse(anchor.isEmpty());
        assertEquals("auto_anchor", anchor.get(0).entry().moduleId());

        List<SearchController.SearchResult> refill = SearchController.search("Пополнение хотбара", 5);
        assertFalse(refill.isEmpty());
        assertEquals("cart_refill", refill.get(0).entry().moduleId());

        List<SearchController.SearchResult> reaper = SearchController.search("Жнец HP", 5);
        assertFalse(reaper.isEmpty());
        assertEquals("hp_reaper", reaper.get(0).entry().moduleId());

        List<SearchController.SearchResult> tool = SearchController.search("Авто-инструмент", 5);
        assertFalse(tool.isEmpty());
        assertEquals("auto_tool", tool.get(0).entry().moduleId());

        List<SearchController.SearchResult> gg = SearchController.search("Авто-GG", 5);
        assertFalse(gg.isEmpty());
        assertEquals("auto_gg", gg.get(0).entry().moduleId());
    }

    @Test
    void testRussianSettingNamesSearch() {
        List<SearchController.SearchResult> sourceMode = SearchController.search("Оружие в руке", 5);
        assertFalse(sourceMode.isEmpty());
        assertEquals("source_mode", sourceMode.get(0).entry().settingId());

        List<SearchController.SearchResult> enchantMode = SearchController.search("Режим чар булавы", 5);
        assertFalse(enchantMode.isEmpty());
        assertEquals("enchant_mode", enchantMode.get(0).entry().settingId());

        List<SearchController.SearchResult> missChance = SearchController.search("Шанс промаха", 5);
        assertFalse(missChance.isEmpty());
        assertEquals("miss_chance", missChance.get(0).entry().settingId());

        List<SearchController.SearchResult> explode = SearchController.search("Автоматический подрыв", 5);
        assertFalse(explode.isEmpty());
        assertEquals("auto_explode", explode.get(0).entry().settingId());
    }

    @Test
    void testRecursiveBreadcrumbTranslation() {
        SearchController.SearchEntry maceSetting = SearchController.getIndex().stream()
            .filter(e -> "combat".equals(e.categoryId()) && "auto_mace".equals(e.moduleId()) && e.settingId() != null)
            .findFirst().orElseThrow();

        String ruBc = SearchController.getTranslation(maceSetting.breadcrumb(), "ru");
        assertTrue(ruBc.contains("Оружие и свапы"), "Breadcrumb must translate category key in Russian");
        assertTrue(ruBc.contains("AUTOMACE") || ruBc.contains("АВТО-БУЛАВА"), "Breadcrumb must recursively translate module sibling key in Russian: " + ruBc);

        String enBc = SearchController.getTranslation(maceSetting.breadcrumb(), "en");
        assertTrue(enBc.contains("Combat & Swaps"), "Breadcrumb must translate category key in English");
        assertTrue(enBc.contains("AUTOMACE"), "Breadcrumb must recursively translate module sibling key in English: " + enBc);
    }

    @Test
    void testTabSearchFilterHidesIrrelevantModules() {
        activity.client.gui.tab.CombatTab combatTab = new activity.client.gui.tab.CombatTab();
        activity.client.gui.layout.ScrollContainer sc = new activity.client.gui.layout.ScrollContainer(0, 0, 300, 400);
        combatTab.buildTab(null, sc, 10, 10, 280);

        // All 4 modules originally visible
        assertTrue(combatTab.getModuleCards().size() >= 4);
        assertTrue(combatTab.getModuleCard("auto_mace").isVisible());
        assertTrue(combatTab.getModuleCard("auto_spear").isVisible());
        assertTrue(combatTab.getModuleCard("auto_shieldbreaker").isVisible());
        assertTrue(combatTab.getModuleCard("auto_stun_slam").isVisible());

        // Search for "копье": only auto_spear matches
        combatTab.applySearchFilter(sc, "копье");
        assertTrue(combatTab.getModuleCard("auto_spear").isVisible(), "AutoSpear must be visible for 'копье'");
        assertFalse(combatTab.getModuleCard("auto_mace").isVisible(), "AutoMace must be hidden for 'копье'");
        assertFalse(combatTab.getModuleCard("auto_shieldbreaker").isVisible(), "AutoShieldbreaker must be hidden for 'копье'");
        assertFalse(combatTab.getModuleCard("auto_stun_slam").isVisible(), "AutoStunSlam must be hidden for 'копье'");

        // Search for "тотем" (which belongs to Defense, 0 matches in CombatTab):
        // Requirement: "При search: показывать только relevant settings/modules"
        combatTab.applySearchFilter(sc, "тотем");
        assertFalse(combatTab.getModuleCard("auto_mace").isVisible(), "AutoMace must be hidden for 'тотем'");
        assertFalse(combatTab.getModuleCard("auto_spear").isVisible(), "AutoSpear must be hidden for 'тотем'");
        assertFalse(combatTab.getModuleCard("auto_shieldbreaker").isVisible(), "AutoShieldbreaker must be hidden for 'тотем'");
        assertFalse(combatTab.getModuleCard("auto_stun_slam").isVisible(), "AutoStunSlam must be hidden for 'тотем'");

        // Clear search: all modules restored
        combatTab.applySearchFilter(sc, "");
        assertTrue(combatTab.getModuleCard("auto_mace").isVisible());
        assertTrue(combatTab.getModuleCard("auto_spear").isVisible());
        assertTrue(combatTab.getModuleCard("auto_shieldbreaker").isVisible());
        assertTrue(combatTab.getModuleCard("auto_stun_slam").isVisible());
    }
}

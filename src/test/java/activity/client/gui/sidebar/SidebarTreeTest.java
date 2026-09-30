package activity.client.gui.sidebar;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.icon.ActivityIcon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SidebarTreeTest {

    private SidebarTree sidebarTree;

    @BeforeEach
    void setUp() {
        ActivityConfigManager.resetDefaults();
        this.sidebarTree = new SidebarTree();
    }

    @Test
    void testCategoriesInitialization() {
        List<SidebarTree.CategoryNode> categories = sidebarTree.getCategories();
        assertEquals(4, categories.size());

        SidebarTree.CategoryNode combat = categories.get(0);
        assertEquals("combat", combat.getId());
        assertEquals(0, combat.getTabIndex());
        assertEquals(ActivityIcon.COMBAT, combat.getIcon());
        assertEquals(5, combat.getChildren().size());
        assertTrue(combat.isExpanded());

        SidebarTree.CategoryNode defense = categories.get(1);
        assertEquals("defense", defense.getId());
        assertEquals(1, defense.getTabIndex());
        assertEquals(ActivityIcon.DEFENSE, defense.getIcon());
        assertEquals(4, defense.getChildren().size());

        SidebarTree.CategoryNode utility = categories.get(2);
        assertEquals("utility", utility.getId());
        assertEquals(2, utility.getTabIndex());
        assertEquals(ActivityIcon.UTILITY, utility.getIcon());
        assertEquals(7, utility.getChildren().size());
        assertEquals("hp_reaper", utility.getChildren().get(0).getId());
        assertEquals("auto_tool", utility.getChildren().get(1).getId());
        assertEquals("auto_gg", utility.getChildren().get(2).getId());
        assertEquals("cart_hud", utility.getChildren().get(3).getId());
        assertEquals("cooldown_hud", utility.getChildren().get(4).getId());
        assertEquals("water_drop", utility.getChildren().get(5).getId());
        assertEquals("hud_activity", utility.getChildren().get(6).getId());

        SidebarTree.CategoryNode config = categories.get(3);
        assertEquals("config", config.getId());
        assertEquals(3, config.getTabIndex());
        assertEquals(ActivityIcon.CONFIG, config.getIcon());
        assertEquals(2, config.getChildren().size());
        assertEquals("profiles", config.getChildren().get(0).getId());
        assertEquals("status", config.getChildren().get(1).getId());
    }

    @Test
    void testFooterInitialization() {
        List<SidebarTree.FooterNode> footers = sidebarTree.getFooterItems();
        assertEquals(2, footers.size());

        SidebarTree.FooterNode settings = footers.get(0);
        assertEquals("settings", settings.getId());
        assertEquals(4, settings.getTabIndex());
        assertEquals(ActivityIcon.SETTINGS, settings.getIcon());

        SidebarTree.FooterNode about = footers.get(1);
        assertEquals("about", about.getId());
        assertEquals(5, about.getTabIndex());
        assertEquals(ActivityIcon.ABOUT, about.getIcon());
    }

    @Test
    void testExpandCollapseToggle() {
        SidebarTree.CategoryNode defense = sidebarTree.getCategories().get(1);
        assertFalse(defense.isExpanded());

        defense.toggleExpanded();
        assertTrue(defense.isExpanded());

        defense.toggleExpanded();
        assertFalse(defense.isExpanded());

        defense.setExpanded(true);
        assertTrue(defense.isExpanded());
    }

    @Test
    void testSelectedModuleTracking() {
        assertNull(sidebarTree.getSelectedModuleId());

        sidebarTree.setSelectedModuleId("auto_totem");
        assertEquals("auto_totem", sidebarTree.getSelectedModuleId());

        sidebarTree.clearSelectedModule();
        assertNull(sidebarTree.getSelectedModuleId());

        SidebarTree.CategoryNode defense = sidebarTree.getCategories().get(1);
        defense.setExpanded(false);
        assertFalse(defense.isExpanded());

        sidebarTree.setSelectedModule("defense", "auto_cart");
        assertEquals("auto_cart", sidebarTree.getSelectedModuleId());
        assertTrue(defense.isExpanded());
    }

    @Test
    void testChildActiveSupplierReflectsConfig() {
        SidebarTree.CategoryNode combat = sidebarTree.getCategories().get(0);
        SidebarTree.ModuleItem mace = combat.getChildren().get(0);
        assertEquals("auto_mace", mace.getId());

        ActivityConfig cfg = ActivityConfigManager.getConfig();
        cfg.autoMaceEnabled = true;
        assertTrue(mace.isActive());

        cfg.autoMaceEnabled = false;
        assertFalse(mace.isActive());
    }

    @Test
    void testHoverAnimationStep() {
        SidebarTree.CategoryNode combat = sidebarTree.getCategories().get(0);
        assertEquals(0.0f, combat.getHoverProgress(), 0.001f);

        combat.update(true, false, 0.05f);
        assertTrue(combat.getHoverProgress() > 0.0f);

        combat.update(false, false, 0.2f);
        assertEquals(0.0f, combat.getHoverProgress(), 0.001f);
    }

    @Test
    void testCategoryTitlesAndTranslations() {
        List<SidebarTree.CategoryNode> categories = sidebarTree.getCategories();
        assertEquals("activity.tab.combat", ((net.minecraft.text.TranslatableTextContent) categories.get(0).getTitle().getContent()).getKey());
        assertEquals("activity.tab.defense", ((net.minecraft.text.TranslatableTextContent) categories.get(1).getTitle().getContent()).getKey());
        assertEquals("activity.tab.utility", ((net.minecraft.text.TranslatableTextContent) categories.get(2).getTitle().getContent()).getKey());
        assertEquals("activity.tab.config", ((net.minecraft.text.TranslatableTextContent) categories.get(3).getTitle().getContent()).getKey());

        List<SidebarTree.FooterNode> footers = sidebarTree.getFooterItems();
        assertEquals("activity.tab.settings", ((net.minecraft.text.TranslatableTextContent) footers.get(0).getTitle().getContent()).getKey());
        assertEquals("activity.tab.about", ((net.minecraft.text.TranslatableTextContent) footers.get(1).getTitle().getContent()).getKey());
    }

    @Test
    void testSmoothAnimationWithoutLayoutJumps() {
        SidebarTree.CategoryNode defense = sidebarTree.getCategories().get(1);
        int totalChildrenHeight = defense.getChildren().size() * (SidebarTree.CHILD_ITEM_HEIGHT + SidebarTree.CHILD_GAP);

        defense.setExpanded(false);
        assertEquals(0.0f, defense.getExpandProgress(), 0.0001f);
        float easedZero = activity.client.gui.animation.AnimationClock.smoothStep(defense.getExpandProgress());
        int heightZero = (int) Math.round(totalChildrenHeight * easedZero);
        assertEquals(0, heightZero, "At progress 0, animated height must be exactly 0 without jump");

        defense.setExpanded(true);
        defense.update(false, false, 1.0f);
        assertEquals(1.0f, defense.getExpandProgress(), 0.0001f);
        float easedOne = activity.client.gui.animation.AnimationClock.smoothStep(defense.getExpandProgress());
        int heightOne = (int) Math.round(totalChildrenHeight * easedOne);
        assertEquals(totalChildrenHeight, heightOne, "At progress 1, animated height must match totalChildrenHeight");
    }

    @Test
    void testUserExpandedPreservationOnSelectModule() {
        SidebarTree.CategoryNode defense = sidebarTree.getCategories().get(1);
        defense.setExpanded(false);
        defense.setUserExpanded(false);
        defense.setSearchExpanded(false);

        sidebarTree.setSelectedModule("defense", "auto_totem");
        assertTrue(defense.isExpanded());
        assertTrue(defense.isUserExpanded());
        assertFalse(defense.isSearchExpanded());

        sidebarTree.applySearchFilter("mace");
        sidebarTree.applySearchFilter("");
        assertTrue(defense.isExpanded(), "Category marked userExpanded via module selection must remain open");
    }

    @Test
    void testCategorySelectionClearsModuleId() {
        sidebarTree.setSelectedModuleId("auto_totem");
        assertEquals("auto_totem", sidebarTree.getSelectedModuleId());

        sidebarTree.setSelectedModule("combat", "combat");
        assertNull(sidebarTree.getSelectedModuleId(), "Selecting a category itself must clear selectedModuleId");
        assertTrue(sidebarTree.getCategories().get(0).isExpanded());
        assertTrue(sidebarTree.getCategories().get(0).isUserExpanded());
    }

    @Test
    void testRapidSearchFilterTyping() {
        SidebarTree.CategoryNode defense = sidebarTree.getCategories().get(1);
        SidebarTree.CategoryNode utility = sidebarTree.getCategories().get(2);
        defense.setExpanded(false);
        defense.setUserExpanded(false);
        utility.setExpanded(false);
        utility.setUserExpanded(false);

        sidebarTree.applySearchFilter("т");
        sidebarTree.applySearchFilter("то");
        sidebarTree.applySearchFilter("тот");
        sidebarTree.applySearchFilter("тотем");
        assertTrue(defense.isExpanded(), "Defense should expand for totem");

        sidebarTree.applySearchFilter("тот");
        sidebarTree.applySearchFilter("то");
        sidebarTree.applySearchFilter("т");
        sidebarTree.applySearchFilter("");
        assertFalse(defense.isExpanded(), "Defense should collapse back when search cleared");

        sidebarTree.applySearchFilter("hud");
        assertTrue(utility.isExpanded(), "Utility should expand for hud search");
        assertFalse(defense.isExpanded());

        sidebarTree.applySearchFilter("");
        assertFalse(utility.isExpanded());
    }

    @Test
    void testAutoPearlCatchActiveStateInSidebarTree() {
        SidebarTree.CategoryNode combat = sidebarTree.getCategories().get(0);
        SidebarTree.ModuleItem pearlCatch = combat.getChildren().stream()
                .filter(item -> "auto_pearl_catch".equals(item.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(pearlCatch, "AutoPearlCatch must exist in combat category");
        assertTrue(pearlCatch.isActive(), "AutoPearlCatch should be active by default");

        activity.client.module.api.IModule mod = activity.client.module.api.ModuleRegistry.get("auto_pearl_catch");
        assertNotNull(mod);
        mod.setEnabled(false);
        assertFalse(pearlCatch.isActive(), "AutoPearlCatch must NOT be active when disabled");

        mod.setEnabled(true);
        assertTrue(pearlCatch.isActive(), "AutoPearlCatch must be active when re-enabled");
    }
}

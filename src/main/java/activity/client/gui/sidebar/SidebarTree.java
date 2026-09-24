package activity.client.gui.sidebar;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.ActivityScreen;
import activity.client.gui.animation.AnimationClock;
import activity.client.gui.font.FontManager;
import activity.client.gui.icon.ActivityIcon;
import activity.client.gui.icon.ActivityIconRenderer;
import activity.client.gui.layout.WindowLayout;
import activity.client.gui.render.ActivityGuiRenderer;
import activity.client.gui.render.ScissorHelper;
import activity.client.gui.tab.TabManager;
import activity.client.gui.theme.ActivityColors;
import activity.client.gui.theme.ActivityMetrics;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.ModuleRegistry;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public class SidebarTree {

    public static final int ITEM_HEIGHT = ActivityMetrics.SIDEBAR_ITEM_HEIGHT;
    public static final int ITEM_GAP = ActivityMetrics.SIDEBAR_ITEM_GAP;
    public static final int CHILD_ITEM_HEIGHT = ActivityMetrics.SIDEBAR_CHILD_HEIGHT;
    public static final int CHILD_GAP = ActivityMetrics.SIDEBAR_CHILD_GAP;

    public static class ModuleItem {
        private final String id;
        private final Text name;
        private final BooleanSupplier activeSupplier;
        private float hoverProgress = 0.0f;
        private boolean wasHovered = false;
        private Text cachedDisplayName = null;
        private int lastMaxChildW = -1;

        public ModuleItem(String id, Text name, BooleanSupplier activeSupplier) {
            this.id = id;
            this.name = name;
            this.activeSupplier = activeSupplier;
        }

        public void invalidateTextCache() {
            this.cachedDisplayName = null;
            this.lastMaxChildW = -1;
        }

        public Text getDisplayName(TextRenderer textRenderer, int maxChildW) {
            if (this.cachedDisplayName != null && this.lastMaxChildW == maxChildW) {
                return this.cachedDisplayName;
            }
            Text wrapped = FontManager.wrap(this.name);
            if (maxChildW > 8 && textRenderer.getWidth(wrapped) > maxChildW) {
                int ellW = textRenderer.getWidth("…");
                int targetW = Math.max(0, maxChildW - ellW);
                String trimmed = textRenderer.trimToWidth(wrapped.getString(), targetW) + "…";
                if (textRenderer.getWidth(trimmed) > maxChildW) {
                    trimmed = textRenderer.trimToWidth(trimmed, Math.max(0, maxChildW));
                }
                this.cachedDisplayName = Text.literal(trimmed);
            } else {
                this.cachedDisplayName = wrapped;
            }
            this.lastMaxChildW = maxChildW;
            return this.cachedDisplayName;
        }

        public String getId() {
            return id;
        }

        public Text getName() {
            return name;
        }

        public boolean isActive() {
            return activeSupplier != null && activeSupplier.getAsBoolean();
        }

        public float getHoverProgress() {
            return hoverProgress;
        }

        public void updateHover(boolean hovered, float dt) {
            if (hovered && !this.wasHovered) {
                activity.client.gui.sound.SoundManager.playHover();
            }
            this.wasHovered = hovered;
            float target = hovered ? 1.0f : 0.0f;
            if (!AnimationClock.isAnimationsEnabled()) {
                this.hoverProgress = target;
                return;
            }
            this.hoverProgress = AnimationClock.approach(this.hoverProgress, target, AnimationClock.DURATION_HOVER, dt);
        }
    }

    public static class CategoryNode {
        private final String id;
        private final int tabIndex;
        private final Text title;
        private final ActivityIcon icon;
        private final List<ModuleItem> children = new ArrayList<>();
        private boolean expanded = false;
        private float expandProgress = 0.0f;
        private float hoverProgress = 0.0f;
        private float chevronHoverProgress = 0.0f;
        private boolean wasHovered = false;

        public CategoryNode(String id, int tabIndex, Text title, ActivityIcon icon) {
            this.id = id;
            this.tabIndex = tabIndex;
            this.title = title;
            this.icon = icon;
        }

        public void addChild(String id, Text name, BooleanSupplier activeSupplier) {
            this.children.add(new ModuleItem(id, name, activeSupplier));
        }

        public String getId() {
            return id;
        }

        public int getTabIndex() {
            return tabIndex;
        }

        public Text getTitle() {
            return title;
        }

        public ActivityIcon getIcon() {
            return icon;
        }

        public List<ModuleItem> getChildren() {
            return children;
        }

        public boolean hasChildren() {
            return !children.isEmpty();
        }

        private boolean userExpanded = false;
        private boolean searchExpanded = false;

        public boolean isExpanded() {
            return expanded;
        }

        public void setExpanded(boolean expanded) {
            this.expanded = expanded;
            if (!AnimationClock.isAnimationsEnabled()) {
                this.expandProgress = expanded ? 1.0f : 0.0f;
            }
        }

        public void toggleExpanded() {
            this.expanded = !this.expanded;
            if (!AnimationClock.isAnimationsEnabled()) {
                this.expandProgress = this.expanded ? 1.0f : 0.0f;
            }
        }

        public boolean isUserExpanded() {
            return userExpanded;
        }

        public void setUserExpanded(boolean userExpanded) {
            this.userExpanded = userExpanded;
        }

        public boolean isSearchExpanded() {
            return searchExpanded;
        }

        public void setSearchExpanded(boolean searchExpanded) {
            this.searchExpanded = searchExpanded;
        }

        public float getExpandProgress() {
            return expandProgress;
        }

        public float getHoverProgress() {
            return hoverProgress;
        }

        public float getChevronHoverProgress() {
            return chevronHoverProgress;
        }

        public void update(boolean hovered, boolean chevronHovered, float dt) {
            if (hovered && !this.wasHovered) {
                activity.client.gui.sound.SoundManager.playHover();
            }
            this.wasHovered = hovered;

            float targetExpand = this.expanded ? 1.0f : 0.0f;
            float targetHover = hovered ? 1.0f : 0.0f;
            float targetChevron = chevronHovered ? 1.0f : 0.0f;

            if (!AnimationClock.isAnimationsEnabled()) {
                this.expandProgress = targetExpand;
                this.hoverProgress = targetHover;
                this.chevronHoverProgress = targetChevron;
                return;
            }

            this.expandProgress = AnimationClock.approach(this.expandProgress, targetExpand, AnimationClock.DURATION_EXPAND, dt);

            this.hoverProgress = AnimationClock.approach(this.hoverProgress, targetHover, AnimationClock.DURATION_HOVER, dt);
            this.chevronHoverProgress = AnimationClock.approach(this.chevronHoverProgress, targetChevron, AnimationClock.DURATION_HOVER, dt);
        }

        private Text cachedDisplayTitle = null;
        private int lastMaxTitleW = -1;

        public void invalidateTextCache() {
            this.cachedDisplayTitle = null;
            this.lastMaxTitleW = -1;
            for (ModuleItem child : this.children) {
                child.invalidateTextCache();
            }
        }

        public Text getDisplayTitle(TextRenderer textRenderer, int maxTitleW) {
            if (this.cachedDisplayTitle != null && this.lastMaxTitleW == maxTitleW) {
                return this.cachedDisplayTitle;
            }
            Text wrapped = FontManager.wrap(this.title);
            if (maxTitleW > 8 && textRenderer.getWidth(wrapped) > maxTitleW) {
                int ellW = textRenderer.getWidth("…");
                int targetW = Math.max(0, maxTitleW - ellW);
                String trimmed = textRenderer.trimToWidth(wrapped.getString(), targetW) + "…";
                if (textRenderer.getWidth(trimmed) > maxTitleW) {
                    trimmed = textRenderer.trimToWidth(trimmed, Math.max(0, maxTitleW));
                }
                this.cachedDisplayTitle = Text.literal(trimmed);
            } else {
                this.cachedDisplayTitle = wrapped;
            }
            this.lastMaxTitleW = maxTitleW;
            return this.cachedDisplayTitle;
        }
    }

    public static class FooterNode {
        private final String id;
        private final int tabIndex;
        private final Text title;
        private final ActivityIcon icon;
        private float hoverProgress = 0.0f;
        private Text cachedDisplayTitle = null;
        private int lastMaxFootW = -1;

        public FooterNode(String id, int tabIndex, Text title, ActivityIcon icon) {
            this.id = id;
            this.tabIndex = tabIndex;
            this.title = title;
            this.icon = icon;
        }

        public void invalidateTextCache() {
            this.cachedDisplayTitle = null;
            this.lastMaxFootW = -1;
        }

        public Text getDisplayTitle(TextRenderer textRenderer, int maxFootW) {
            if (this.cachedDisplayTitle != null && this.lastMaxFootW == maxFootW) {
                return this.cachedDisplayTitle;
            }
            Text wrapped = FontManager.wrap(this.title);
            if (maxFootW > 8 && textRenderer.getWidth(wrapped) > maxFootW) {
                int ellW = textRenderer.getWidth("…");
                int targetW = Math.max(0, maxFootW - ellW);
                String trimmed = textRenderer.trimToWidth(wrapped.getString(), targetW) + "…";
                if (textRenderer.getWidth(trimmed) > maxFootW) {
                    trimmed = textRenderer.trimToWidth(trimmed, Math.max(0, maxFootW));
                }
                this.cachedDisplayTitle = Text.literal(trimmed);
            } else {
                this.cachedDisplayTitle = wrapped;
            }
            this.lastMaxFootW = maxFootW;
            return this.cachedDisplayTitle;
        }

        public String getId() {
            return id;
        }

        public int getTabIndex() {
            return tabIndex;
        }

        public Text getTitle() {
            return title;
        }

        public ActivityIcon getIcon() {
            return icon;
        }

        public float getHoverProgress() {
            return hoverProgress;
        }

        private boolean wasHovered = false;

        public void updateHover(boolean hovered, float dt) {
            if (hovered && !this.wasHovered) {
                activity.client.gui.sound.SoundManager.playHover();
            }
            this.wasHovered = hovered;
            float target = hovered ? 1.0f : 0.0f;
            if (!AnimationClock.isAnimationsEnabled()) {
                this.hoverProgress = target;
                return;
            }
            this.hoverProgress = AnimationClock.approach(this.hoverProgress, target, AnimationClock.DURATION_HOVER, dt);
        }
    }

    private final List<CategoryNode> categories = new ArrayList<>();
    private final List<FooterNode> footerItems = new ArrayList<>();
    private double scrollOffset = 0.0;
    private String selectedModuleId = null;
    private String activeSearchQuery = "";

    private Text cachedWatermarkText = null;
    private int lastMaxWaterW = -1;
    private final java.util.Map<String, Text> cachedQaNames = new java.util.HashMap<>();
    private final java.util.Map<String, Integer> cachedQaLastMaxW = new java.util.HashMap<>();

    public void invalidateTextCache() {
        this.cachedWatermarkText = null;
        this.lastMaxWaterW = -1;
        this.cachedQaNames.clear();
        this.cachedQaLastMaxW.clear();
        for (CategoryNode cat : this.categories) {
            cat.invalidateTextCache();
        }
        for (FooterNode foot : this.footerItems) {
            foot.invalidateTextCache();
        }
    }

    private Text getWatermarkText(TextRenderer textRenderer, int maxWaterW) {
        if (this.cachedWatermarkText != null && this.lastMaxWaterW == maxWaterW) {
            return this.cachedWatermarkText;
        }
        Text watermarkText = Text.translatable("activity.watermark.footer");
        Text wrapped = FontManager.wrap(watermarkText);
        if (maxWaterW > 10 && textRenderer.getWidth(wrapped) > maxWaterW) {
            String trimmed = textRenderer.trimToWidth(wrapped.getString(), Math.max(0, maxWaterW - textRenderer.getWidth("…"))) + "…";
            this.cachedWatermarkText = Text.literal(trimmed);
        } else {
            this.cachedWatermarkText = wrapped;
        }
        this.lastMaxWaterW = maxWaterW;
        return this.cachedWatermarkText;
    }

    private Text getQaDisplayName(String moduleId, Text modName, TextRenderer textRenderer, int maxNameW) {
        Integer lastMax = this.cachedQaLastMaxW.get(moduleId);
        Text cached = this.cachedQaNames.get(moduleId);
        if (cached != null && lastMax != null && lastMax == maxNameW) {
            return cached;
        }
        Text wrapped = FontManager.wrap(modName);
        String displayStr = wrapped.getString();
        if (maxNameW > 8 && textRenderer.getWidth(wrapped) > maxNameW) {
            int ellW = textRenderer.getWidth("…");
            displayStr = textRenderer.trimToWidth(displayStr, Math.max(0, maxNameW - ellW)) + "…";
            cached = Text.literal(displayStr);
        } else {
            cached = wrapped;
        }
        this.cachedQaNames.put(moduleId, cached);
        this.cachedQaLastMaxW.put(moduleId, maxNameW);
        return cached;
    }

    public SidebarTree() {
        initNodes();
        FontManager.addListener(fontChangeListener);
    }

    private final Runnable fontChangeListener = this::invalidateTextCache;

    public void dispose() {
        FontManager.removeListener(fontChangeListener);
    }

    public String getActiveSearchQuery() {
        return activeSearchQuery;
    }

    public void applySearchFilter(String query) {
        this.activeSearchQuery = query != null ? query.trim() : "";
        if (this.activeSearchQuery.isEmpty()) {
            for (CategoryNode cat : this.categories) {
                if (cat.isSearchExpanded()) {
                    cat.setExpanded(cat.isUserExpanded());
                    cat.setSearchExpanded(false);
                }
            }
            return;
        }

        for (CategoryNode cat : this.categories) {
            boolean matches = activity.client.gui.search.SearchController.matchesCategory(cat.getId(), this.activeSearchQuery);
            if (matches) {
                if (!cat.isExpanded()) {
                    cat.setSearchExpanded(true);
                    cat.setExpanded(true);
                }
            } else {
                if (cat.isSearchExpanded()) {
                    cat.setExpanded(cat.isUserExpanded());
                    cat.setSearchExpanded(false);
                }
            }
        }
    }

    public String getSelectedModuleId() {
        return selectedModuleId;
    }

    public void setSelectedModuleId(String selectedModuleId) {
        this.selectedModuleId = selectedModuleId;
    }

    public void clearSelectedModule() {
        this.selectedModuleId = null;
    }

    public void setSelectedModule(String categoryId, String moduleId) {
        if (categoryId != null && categoryId.equalsIgnoreCase(moduleId)) {
            this.selectedModuleId = null;
        } else {
            this.selectedModuleId = moduleId;
        }
        for (CategoryNode cat : this.categories) {
            if (cat.getId().equalsIgnoreCase(categoryId)) {
                if (!cat.isExpanded()) {
                    cat.setExpanded(true);
                }
                cat.setUserExpanded(true);
                cat.setSearchExpanded(false);
                break;
            }
        }
    }

    private void initNodes() {

        CategoryNode combat = new CategoryNode("combat", 0, Text.translatable("activity.tab.combat"), ActivityIcon.COMBAT);
        populateCategoryFromRegistry(combat, activity.client.module.api.ModuleCategory.COMBAT);
        combat.setExpanded(true);
        combat.setUserExpanded(true);
        this.categories.add(combat);

        CategoryNode defense = new CategoryNode("defense", 1, Text.translatable("activity.tab.defense"), ActivityIcon.DEFENSE);
        populateCategoryFromRegistry(defense, activity.client.module.api.ModuleCategory.DEFENSE);
        this.categories.add(defense);

        CategoryNode utility = new CategoryNode("utility", 2, Text.translatable("activity.tab.utility"), ActivityIcon.UTILITY);
        populateCategoryFromRegistry(utility, activity.client.module.api.ModuleCategory.UTILITY);
        utility.addChild("hud_activity", Text.translatable("activity.module.hud_activity.name"), () -> {
            ActivityConfig c = ActivityConfigManager.getConfig();
            return c != null && c.overlayEnabled;
        });
        this.categories.add(utility);

        CategoryNode config = new CategoryNode("config", 3, Text.translatable("activity.tab.config"), ActivityIcon.CONFIG);
        config.addChild("profiles", Text.translatable("activity.card.config.profiles"), () -> true);
        config.addChild("status", Text.translatable("activity.card.config.status"), () -> true);
        this.categories.add(config);

        this.footerItems.add(new FooterNode("settings", 4, Text.translatable("activity.tab.settings"), ActivityIcon.SETTINGS));
        this.footerItems.add(new FooterNode("about", 5, Text.translatable("activity.tab.about"), ActivityIcon.ABOUT));
    }

    public void rebuildNodes() {
        this.categories.clear();
        this.footerItems.clear();
        this.invalidateTextCache();
        initNodes();
    }

    public void refresh() {
        rebuildNodes();
    }

    private void populateCategoryFromRegistry(CategoryNode node, activity.client.module.api.ModuleCategory category) {
        List<IModule> modules = ModuleRegistry.getByCategory(category);
        for (IModule mod : modules) {
            node.addChild(mod.getId(), mod.getName(), () -> isModuleActive(mod));
        }
    }

    private static boolean isModuleActive(IModule mod) {
        if (mod == null) return false;
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            Boolean fromConfig = isModuleEnabledInConfig(c, mod.getId());
            if (fromConfig != null) {
                return fromConfig;
            }
        }
        return mod.isEnabled();
    }

    private static Boolean isModuleEnabledInConfig(ActivityConfig c, String id) {
        Boolean direct = switch (id) {
            case "auto_mace" -> c.autoMaceEnabled;
            case "auto_spear" -> c.autoSpearEnabled;
            case "auto_shieldbreaker" -> c.autoShieldbreakerEnabled;
            case "auto_stun_slam", "auto_stun_slime" -> c.autoStunSlamEnabled;
            case "auto_totem" -> c.autoTotemEnabled;
            case "auto_cart" -> c.autoCartEnabled;
            case "auto_anchor" -> c.autoAnchorEnabled;
            case "cart_refill" -> c.cartRefillEnabled;
            case "hp_reaper" -> c.hpReaperEnabled;
            case "auto_tool" -> c.autoToolEnabled;
            case "auto_gg" -> c.autoGGEnabled;
            case "cart_hud" -> c.cartHudEnabled;
            case "cooldown_hud" -> c.cooldownHudEnabled;
            default -> null;
        };
        if (direct != null) {
            return direct;
        }
        ActivityConfig.ModuleConfigEntry entry = c.getModuleEntry(id);
        if (entry != null) {
            return entry.enabled;
        }
        return null;
    }

    public List<CategoryNode> getCategories() {
        return categories;
    }

    public List<FooterNode> getFooterItems() {
        return footerItems;
    }

    private int getFooterItemHeight(WindowLayout layout) {
        if (layout.sidebarHeight < 110) {
            return 16;
        } else if (layout.isCompact() && layout.sidebarHeight < 150) {
            return 18;
        }
        return ITEM_HEIGHT;
    }

    private int getFooterItemGap(WindowLayout layout) {
        return (layout.isCompact() || layout.sidebarHeight < 150) ? 1 : ITEM_GAP;
    }

    private int getFooterHeight(WindowLayout layout) {
        int footerItemsCount = this.footerItems.size();
        int itemH = getFooterItemHeight(layout);
        int itemGap = getFooterItemGap(layout);
        int padTop = (layout.isCompact() || layout.sidebarHeight < 150) ? 2 : 6;
        int padBottom = (layout.isCompact() || layout.sidebarHeight < 150) ? 2 : 4;
        int height = 1 + padTop + (footerItemsCount * itemH) + ((footerItemsCount - 1) * itemGap) + padBottom;
        if (layout.sidebarHeight >= 160) {
            height += 16;
        }
        return height;
    }

    public void render(DrawContext context, int mouseX, int mouseY, TextRenderer textRenderer, WindowLayout layout, TabManager tabManager) {
        render(context, mouseX, mouseY, textRenderer, layout, tabManager, 1.0f);
    }

    public void render(DrawContext context, int mouseX, int mouseY, TextRenderer textRenderer, WindowLayout layout, TabManager tabManager, float alphaFactor) {
        float dt = AnimationClock.getDeltaTime();
        int selectedTab = tabManager.getSelectedIndex();

        int sidebarX = layout.sidebarX;
        int sidebarY = layout.sidebarY;
        int sidebarW = layout.sidebarWidth;
        int sidebarH = layout.sidebarHeight;

        int footerHeight = getFooterHeight(layout);
        int footerY = Math.max(sidebarY + 18, sidebarY + sidebarH - footerHeight);
        int itemH = getFooterItemHeight(layout);
        int itemGap = getFooterItemGap(layout);

        int categoryAreaY = sidebarY + (layout.isCompact() ? ActivityMetrics.PADDING_WINDOW_COMPACT : ActivityMetrics.PADDING_WINDOW);
        int categoryAreaH = Math.max(16, footerY - categoryAreaY - 2);

        ActivityConfig config = ActivityConfigManager.getConfig();
        List<String> pinnedList = config != null ? config.getPinnedModules() : List.of();
        int qaHeight = getQuickAccessHeight(pinnedList);

        int simY = categoryAreaY - (int) Math.round(this.scrollOffset);
        simY += qaHeight;
        for (CategoryNode cat : this.categories) {
            int rowX = sidebarX + 2;
            int rowW = sidebarW - 4;
            int rowY = simY;
            boolean isRowHovered = (mouseX >= rowX && mouseX < rowX + rowW && mouseY >= rowY && mouseY < rowY + ITEM_HEIGHT && mouseY >= categoryAreaY && mouseY < categoryAreaY + categoryAreaH);
            boolean isChevronHovered = isRowHovered && (mouseX >= rowX + rowW - 18);
            cat.update(isRowHovered, isChevronHovered, dt);
            simY += ITEM_HEIGHT + ITEM_GAP;

            if (cat.getExpandProgress() > 0.001f && cat.hasChildren()) {
                float eased = AnimationClock.smoothStep(cat.getExpandProgress());
                int childrenHeight = cat.getChildren().size() * (CHILD_ITEM_HEIGHT + CHILD_GAP);
                int clipHeight = (int) Math.round(childrenHeight * eased);
                int childY = simY;
                for (ModuleItem child : cat.getChildren()) {
                    boolean isChildHovered = (mouseX >= rowX + 10 && mouseX < rowX + rowW && mouseY >= childY && mouseY < childY + CHILD_ITEM_HEIGHT && mouseY >= categoryAreaY && mouseY < categoryAreaY + categoryAreaH);
                    child.updateHover(isChildHovered, dt);
                    childY += CHILD_ITEM_HEIGHT + CHILD_GAP;
                }
                simY += clipHeight;
            }
        }

        int totalContentHeight = qaHeight;
        for (CategoryNode cat : this.categories) {
            totalContentHeight += ITEM_HEIGHT + ITEM_GAP;
            if (cat.getExpandProgress() > 0.001f && cat.hasChildren()) {
                float eased = AnimationClock.smoothStep(cat.getExpandProgress());
                int chH = cat.getChildren().size() * (CHILD_ITEM_HEIGHT + CHILD_GAP);
                totalContentHeight += (int) Math.round(chH * eased);
            }
        }
        double maxScroll = Math.max(0, totalContentHeight - categoryAreaH);
        this.scrollOffset = Math.clamp(this.scrollOffset, 0, maxScroll);

        ScissorHelper.pushScissor(context, sidebarX, categoryAreaY, sidebarW, categoryAreaH);
        try {
            int curY = categoryAreaY - (int) Math.round(this.scrollOffset);

            if (!pinnedList.isEmpty()) {
                renderQuickAccess(context, sidebarX, curY, sidebarW, pinnedList, textRenderer, mouseX, mouseY, categoryAreaY, categoryAreaH, alphaFactor);
                curY += qaHeight;
            }

            for (CategoryNode cat : this.categories) {
                int rowX = sidebarX + 2;
                int rowW = sidebarW - 4;
                int rowY = curY;

                boolean isSelected = (cat.getTabIndex() == selectedTab);

                renderCategoryRow(context, rowX, rowY, rowW, ITEM_HEIGHT, cat, isSelected, textRenderer, layout, alphaFactor);
                curY += ITEM_HEIGHT + ITEM_GAP;

                if (cat.getExpandProgress() > 0.001f && cat.hasChildren()) {
                    float eased = AnimationClock.smoothStep(cat.getExpandProgress());
                    int childrenHeight = cat.getChildren().size() * (CHILD_ITEM_HEIGHT + CHILD_GAP);
                    int clipHeight = (int) Math.round(childrenHeight * eased);

                    ScissorHelper.pushScissor(context, sidebarX, curY, sidebarW, clipHeight);
                    try {
                        int childY = curY;
                        for (ModuleItem child : cat.getChildren()) {
                            boolean isChildSelected = child.getId().equals(this.selectedModuleId) && (selectedTab == cat.getTabIndex());
                            renderChildRow(context, rowX + 8, childY, rowW - 8, CHILD_ITEM_HEIGHT, child, isChildSelected, textRenderer, eased, alphaFactor);
                            childY += CHILD_ITEM_HEIGHT + CHILD_GAP;
                        }
                    } finally {
                        ScissorHelper.popScissor(context);
                    }
                    curY += clipHeight;
                }
            }
        } finally {
            ScissorHelper.popScissor(context);
        }

        ActivityGuiRenderer.drawHorizontalLine(context, sidebarX, footerY, sidebarW, ActivityColors.scaleAlpha(ActivityColors.BORDER, alphaFactor));

        ScissorHelper.pushScissor(context, sidebarX, footerY, sidebarW, footerHeight);
        try {
            int itemY = footerY + (layout.isCompact() ? 3 : 5);
            for (FooterNode item : this.footerItems) {
                int rowX = sidebarX + 2;
                int rowW = sidebarW - 4;
                boolean isSelected = (item.getTabIndex() == selectedTab);
                boolean isHovered = (mouseX >= rowX && mouseX < rowX + rowW && mouseY >= itemY && mouseY < itemY + itemH);

                item.updateHover(isHovered, dt);
                renderFooterRow(context, rowX, itemY, rowW, itemH, item, isSelected, textRenderer, layout, alphaFactor);
                itemY += itemH + itemGap;
            }

            if (layout.sidebarHeight >= 160) {
                int waterY = itemY + 3;
                int waterX = sidebarX + (layout.isCompact() ? 4 : 8);
                int maxWaterW = sidebarW - (layout.isCompact() ? 8 : 16);
                if (maxWaterW > 10) {
                    Text wrapped = getWatermarkText(textRenderer, maxWaterW);
                    int waterColor = ActivityColors.scaleAlpha(ActivityColors.TEXT_MUTED, alphaFactor);
                    activity.client.gui.font.UiTextRenderer.drawTextWithShadow(context, textRenderer, wrapped, waterX, waterY, waterColor);
                }
            }
        } finally {
            ScissorHelper.popScissor(context);
        }
    }

    public int getQuickAccessHeight(List<String> pinned) {
        if (pinned == null || pinned.isEmpty()) return 0;
        return 16 + pinned.size() * (ITEM_HEIGHT + ITEM_GAP) + 4;
    }

    private void renderQuickAccess(
        DrawContext context,
        int sidebarX,
        int curY,
        int sidebarW,
        List<String> pinned,
        TextRenderer textRenderer,
        int mouseX,
        int mouseY,
        int categoryAreaY,
        int categoryAreaH
    ) {
        renderQuickAccess(context, sidebarX, curY, sidebarW, pinned, textRenderer, mouseX, mouseY, categoryAreaY, categoryAreaH, 1.0f);
    }

    private void renderQuickAccess(
        DrawContext context,
        int sidebarX,
        int curY,
        int sidebarW,
        List<String> pinned,
        TextRenderer textRenderer,
        int mouseX,
        int mouseY,
        int categoryAreaY,
        int categoryAreaH,
        float alphaFactor
    ) {

        if (textRenderer != null) {
            int headerY = curY;
            Text qaTitle = Text.translatable("activity.sidebar.quick_access");
            int qaTitleColor = ActivityColors.scaleAlpha(ActivityColors.TEXT_MUTED, alphaFactor);
            activity.client.gui.font.UiTextRenderer.drawText(context, textRenderer, qaTitle, sidebarX + 6, headerY + 3, qaTitleColor, false);
        }

        int itemY = curY + 16;
        for (String id : pinned) {
            int rowX = sidebarX + 2;
            int rowW = sidebarW - 4;
            boolean isHovered = mouseX >= rowX && mouseX < rowX + rowW && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT
                && mouseY >= categoryAreaY && mouseY < categoryAreaY + categoryAreaH;

            renderQuickAccessRow(context, rowX, itemY, rowW, ITEM_HEIGHT, id, isHovered, textRenderer, alphaFactor);
            itemY += ITEM_HEIGHT + ITEM_GAP;
        }

        int divColor = ActivityColors.scaleAlpha(ActivityColors.BORDER_DIVIDER, alphaFactor);
        ActivityGuiRenderer.drawHorizontalLine(context, sidebarX + 4, itemY + 1, sidebarW - 8, divColor);
    }

    private void renderQuickAccessRow(
        DrawContext context,
        int x,
        int y,
        int width,
        int height,
        String moduleId,
        boolean hovered,
        TextRenderer textRenderer
    ) {
        renderQuickAccessRow(context, x, y, width, height, moduleId, hovered, textRenderer, 1.0f);
    }

    private void renderQuickAccessRow(
        DrawContext context,
        int x,
        int y,
        int width,
        int height,
        String moduleId,
        boolean hovered,
        TextRenderer textRenderer,
        float alphaFactor
    ) {
        if (hovered) {
            int hovBg = ActivityColors.scaleAlpha(ActivityColors.ITEM_HOVER_BG, alphaFactor);
            ActivityGuiRenderer.fill(context, x, y, width, height, hovBg);
        }

        ModuleMetadata meta = ModuleRegistry.getMetadata(moduleId);
        IModule module = ModuleRegistry.get(moduleId);
        boolean isEnabled = module != null && module.isEnabled();

        ActivityIcon icon = meta != null ? meta.getIcon() : ActivityIcon.INFO;
        int iconColor = isEnabled ? ActivityColors.TEXT_PRIMARY : ActivityColors.TEXT_MUTED;
        iconColor = ActivityColors.scaleAlpha(iconColor, alphaFactor);
        if (icon != null) {
            int iconY = y + (height - icon.getHeight()) / 2;
            ActivityIconRenderer.draw(context, icon, x + 4, iconY, iconColor);
        }

        int toggleW = 16;
        int toggleH = 9;
        int toggleX = x + width - toggleW - 4;
        int toggleY = y + (height - toggleH) / 2;

        int switchBg = isEnabled ? ActivityColors.STATE_ON_BG : ActivityColors.STATE_OFF_BG;
        switchBg = ActivityColors.scaleAlpha(switchBg, alphaFactor);
        int switchBorder = ActivityColors.scaleAlpha(ActivityColors.BORDER_INPUT, alphaFactor);
        ActivityGuiRenderer.fill(context, toggleX, toggleY, toggleW, toggleH, switchBg);
        ActivityGuiRenderer.drawBorder(context, toggleX, toggleY, toggleW, toggleH, switchBorder);

        int knobX = isEnabled ? (toggleX + toggleW - 6) : (toggleX + 1);
        int knobY = toggleY + 1;
        int knobColor = ActivityColors.scaleAlpha(ActivityColors.TOGGLE_KNOB, alphaFactor);
        ActivityGuiRenderer.fill(context, knobX, knobY, 5, toggleH - 2, knobColor);

        int rightBound = toggleX - 4;
        if (module != null && textRenderer != null) {
            String kbStr = (module.getKeybind() != null && !module.getKeybind().isUnbound())
                ? "[" + module.getKeybind().getDisplayString() + "]"
                : (meta != null ? meta.getKeybindDisplay() : "[-]");
            int kbW = activity.client.gui.font.UiTextRenderer.getWidth(textRenderer, kbStr);
            if (kbW < 45 && rightBound - kbW > x + 30) {
                int kbX = rightBound - kbW;
                int fontH = activity.client.gui.font.UiTextRenderer.getFontHeight(textRenderer);
                int kbY = y + (height - fontH) / 2;
                int kbColor = ActivityColors.scaleAlpha(ActivityColors.TEXT_MUTED, alphaFactor);
                activity.client.gui.font.UiTextRenderer.drawText(context, textRenderer, kbStr, kbX, kbY, kbColor, false);
                rightBound = kbX - 4;
            }
        }

        if (textRenderer != null) {
            int nameX = x + 4 + (icon != null ? icon.getWidth() : 12) + 4;
            int maxNameW = Math.max(0, rightBound - nameX);
            Text modName = module != null ? module.getName() : (meta != null ? meta.getDisplayName() : Text.literal(moduleId));
            Text display = getQaDisplayName(moduleId, modName, textRenderer, maxNameW);
            if (maxNameW > 8) {
                int fontH = activity.client.gui.font.UiTextRenderer.getFontHeight(textRenderer);
                int nameY = y + (height - fontH) / 2;
                int textColor = isEnabled ? ActivityColors.TEXT_PRIMARY : ActivityColors.TEXT_SECONDARY;
                textColor = ActivityColors.scaleAlpha(textColor, alphaFactor);
                activity.client.gui.font.UiTextRenderer.drawTextWithShadow(context, textRenderer, display, nameX, nameY, textColor);
            }
        }
    }

    private void renderCategoryRow(DrawContext context, int x, int y, int width, int height,
                                   CategoryNode cat, boolean selected, TextRenderer textRenderer, WindowLayout layout) {
        renderCategoryRow(context, x, y, width, height, cat, selected, textRenderer, layout, 1.0f);
    }

    private void renderCategoryRow(DrawContext context, int x, int y, int width, int height,
                                   CategoryNode cat, boolean selected, TextRenderer textRenderer, WindowLayout layout, float alphaFactor) {
        if (selected) {
            int selBg = ActivityColors.scaleAlpha(ActivityColors.ITEM_SELECTED_BG, alphaFactor);
            int selBar = ActivityColors.scaleAlpha(ActivityColors.ITEM_SELECTED_BAR, alphaFactor);
            ActivityGuiRenderer.fill(context, x, y, width, height, selBg);
            ActivityGuiRenderer.fill(context, x, y, ActivityMetrics.INDICATOR_WIDTH, height, selBar);
        } else if (cat.getHoverProgress() > 0.001f) {
            int maxAlpha = (ActivityColors.ITEM_HOVER_BG >>> 24) & 0xFF;
            int alpha = (int) (maxAlpha * cat.getHoverProgress() * alphaFactor);
            int hoverColor = (alpha << 24) | (ActivityColors.ITEM_HOVER_BG & 0x00FFFFFF);
            ActivityGuiRenderer.fill(context, x, y, width, height, hoverColor);
        }

        int textColor;
        if (selected) {
            textColor = ActivityColors.TEXT_ACCENT;
        } else if (cat.getHoverProgress() > 0.001f) {
            textColor = ActivityColors.interpolateColor(ActivityColors.TEXT_SECONDARY, ActivityColors.TEXT_PRIMARY, cat.getHoverProgress());
        } else {
            textColor = ActivityColors.TEXT_SECONDARY;
        }
        textColor = ActivityColors.scaleAlpha(textColor, alphaFactor);

        int padX = layout.isCompact() ? ActivityMetrics.PADDING_WINDOW_COMPACT : ActivityMetrics.PADDING_WINDOW;
        int textX = x + padX;
        if (cat.getIcon() != null) {
            int iconY = y + (height - cat.getIcon().getHeight()) / 2;
            ActivityIconRenderer.draw(context, cat.getIcon(), textX, iconY, textColor);
            textX += cat.getIcon().getWidth() + (layout.isCompact() ? 3 : 5);
        }

        int fontH = activity.client.gui.font.UiTextRenderer.getFontHeight(textRenderer);
        int textY = y + (height - fontH) / 2;
        int chevronX = x + width - 12;
        int maxTitleW = cat.hasChildren() ? (chevronX - textX - 2) : (x + width - textX - 4);
        Text displayTitle = cat.getDisplayTitle(textRenderer, maxTitleW);
        if (maxTitleW > 8) {
            activity.client.gui.font.UiTextRenderer.drawTextWithShadow(context, textRenderer, displayTitle, textX, textY, textColor);
        }

        if (cat.hasChildren() && width >= 40) {
            float eased = AnimationClock.smoothStep(cat.getExpandProgress());
            int chevronY = y + (height - ActivityIcon.CHEVRON_RIGHT.getHeight()) / 2;
            int baseChevronColor = cat.getChevronHoverProgress() > 0.001f
                ? ActivityColors.interpolateColor(ActivityColors.TEXT_MUTED, ActivityColors.TEXT_PRIMARY, cat.getChevronHoverProgress())
                : ActivityColors.TEXT_MUTED;
            int chevronColor = ActivityColors.scaleAlpha(baseChevronColor, alphaFactor);

            if (eased <= 0.001f) {
                ActivityIconRenderer.draw(context, ActivityIcon.CHEVRON_RIGHT, chevronX, chevronY, chevronColor);
            } else if (eased >= 0.999f) {
                ActivityIconRenderer.draw(context, ActivityIcon.CHEVRON_DOWN, chevronX, chevronY, chevronColor);
            } else {
                ActivityIconRenderer.draw(context, ActivityIcon.CHEVRON_RIGHT, chevronX, chevronY, chevronColor, (1.0f - eased) * alphaFactor);
                ActivityIconRenderer.draw(context, ActivityIcon.CHEVRON_DOWN, chevronX, chevronY, chevronColor, eased * alphaFactor);
            }
        }
    }

    private void renderChildRow(DrawContext context, int x, int y, int width, int height,
                                ModuleItem child, boolean selected, TextRenderer textRenderer) {
        renderChildRow(context, x, y, width, height, child, selected, textRenderer, 1.0f, 1.0f);
    }

    private void renderChildRow(DrawContext context, int x, int y, int width, int height,
                                ModuleItem child, boolean selected, TextRenderer textRenderer, float parentEased) {
        renderChildRow(context, x, y, width, height, child, selected, textRenderer, parentEased, 1.0f);
    }

    private void renderChildRow(DrawContext context, int x, int y, int width, int height,
                                ModuleItem child, boolean selected, TextRenderer textRenderer, float parentEased, float globalAlpha) {
        float alphaFactor = Math.clamp(parentEased * 1.5f * globalAlpha, 0.0f, 1.0f);
        if (alphaFactor <= 0.01f) return;

        if (selected) {
            int selBg = ActivityColors.scaleAlpha(ActivityColors.ITEM_SELECTED_BG, alphaFactor);
            int selBar = ActivityColors.scaleAlpha(ActivityColors.ITEM_SELECTED_BAR, alphaFactor);
            ActivityGuiRenderer.fill(context, x, y, width, height, selBg);
            ActivityGuiRenderer.fill(context, x, y, ActivityMetrics.INDICATOR_WIDTH, height, selBar);
        } else if (child.getHoverProgress() > 0.001f) {
            int alpha = (int) (24 * child.getHoverProgress() * alphaFactor);
            int hoverColor = (alpha << 24) | 0x00FFFFFF;
            ActivityGuiRenderer.fill(context, x, y, width, height, hoverColor);
        }

        int dotX = x + 3;
        int dotY = y + (height - 4) / 2;
        boolean isSearchMatched = !this.activeSearchQuery.isEmpty() &&
            activity.client.gui.search.SearchController.matchesModule(null, child.getId(), this.activeSearchQuery);

        int dotColor;
        if (child.isActive()) {
            dotColor = selected ? 0xFF57F287 : 0xFF43B581;
        } else if (isSearchMatched) {
            dotColor = ActivityColors.ACCENT_PRIMARY;
        } else {
            dotColor = selected ? 0x888E9297 : 0x558E9297;
        }
        dotColor = ActivityColors.scaleAlpha(dotColor, alphaFactor);
        ActivityGuiRenderer.fill(context, dotX, dotY, 4, 4, dotColor);

        int textX = dotX + 7;
        int textColor;
        if (selected) {
            textColor = ActivityColors.TEXT_ACCENT;
        } else if (isSearchMatched) {
            textColor = ActivityColors.ACCENT_LIGHT;
        } else if (child.getHoverProgress() > 0.001f) {
            textColor = ActivityColors.interpolateColor(ActivityColors.TEXT_MUTED, ActivityColors.TEXT_PRIMARY, child.getHoverProgress());
        } else {
            textColor = ActivityColors.TEXT_MUTED;
        }
        textColor = ActivityColors.scaleAlpha(textColor, alphaFactor);
        int fontH = activity.client.gui.font.UiTextRenderer.getFontHeight(textRenderer);
        int textY = y + (height - fontH) / 2;
        int maxChildW = x + width - textX - 3;

        Text displayName = child.getDisplayName(textRenderer, maxChildW);
        if (maxChildW > 8) {
            activity.client.gui.font.UiTextRenderer.drawTextWithShadow(context, textRenderer, displayName, textX, textY, textColor);
        }
    }

    private void renderFooterRow(DrawContext context, int x, int y, int width, int height,
                                 FooterNode item, boolean selected, TextRenderer textRenderer, WindowLayout layout) {
        renderFooterRow(context, x, y, width, height, item, selected, textRenderer, layout, 1.0f);
    }

    private void renderFooterRow(DrawContext context, int x, int y, int width, int height,
                                 FooterNode item, boolean selected, TextRenderer textRenderer, WindowLayout layout, float alphaFactor) {
        if (selected) {
            int selBg = ActivityColors.scaleAlpha(ActivityColors.ITEM_SELECTED_BG, alphaFactor);
            int selBar = ActivityColors.scaleAlpha(ActivityColors.ITEM_SELECTED_BAR, alphaFactor);
            ActivityGuiRenderer.fill(context, x, y, width, height, selBg);
            ActivityGuiRenderer.fill(context, x, y, ActivityMetrics.INDICATOR_WIDTH, height, selBar);
        } else if (item.getHoverProgress() > 0.001f) {
            int maxAlpha = (ActivityColors.ITEM_HOVER_BG >>> 24) & 0xFF;
            int alpha = (int) (maxAlpha * item.getHoverProgress() * alphaFactor);
            int hoverColor = (alpha << 24) | (ActivityColors.ITEM_HOVER_BG & 0x00FFFFFF);
            ActivityGuiRenderer.fill(context, x, y, width, height, hoverColor);
        }

        int textColor = selected ? ActivityColors.TEXT_ACCENT : (item.getHoverProgress() > 0.001f ? ActivityColors.TEXT_PRIMARY : ActivityColors.TEXT_SECONDARY);
        textColor = ActivityColors.scaleAlpha(textColor, alphaFactor);

        int padX = layout.isCompact() ? ActivityMetrics.PADDING_WINDOW_COMPACT : ActivityMetrics.PADDING_WINDOW;
        int textX = x + padX;
        if (item.getIcon() != null) {
            int iconY = y + (height - item.getIcon().getHeight()) / 2;
            ActivityIconRenderer.draw(context, item.getIcon(), textX, iconY, textColor);
            textX += item.getIcon().getWidth() + (layout.isCompact() ? 3 : 5);
        }

        int fontH2 = activity.client.gui.font.UiTextRenderer.getFontHeight(textRenderer);
        int textY = y + (height - fontH2) / 2;
        int maxFootW = x + width - textX - 3;
        Text displayTitle = item.getDisplayTitle(textRenderer, maxFootW);
        if (maxFootW > 8) {
            activity.client.gui.font.UiTextRenderer.drawTextWithShadow(context, textRenderer, displayTitle, textX, textY, textColor);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, WindowLayout layout, ActivityScreen screen) {
        if (button != 0 && button != 1) return false;

        int sidebarX = layout.sidebarX;
        int sidebarY = layout.sidebarY;
        int sidebarW = layout.sidebarWidth;
        int sidebarH = layout.sidebarHeight;

        if (mouseX < sidebarX || mouseX >= sidebarX + sidebarW || mouseY < sidebarY || mouseY >= sidebarY + sidebarH) {
            return false;
        }

        int footerHeight = getFooterHeight(layout);
        int footerY = sidebarY + sidebarH - footerHeight;
        int itemH = getFooterItemHeight(layout);
        int itemGap = getFooterItemGap(layout);

        if (mouseY >= footerY) {
            if (button != 0) return true;
            int itemY = footerY + (layout.isCompact() ? 3 : 5);
            for (FooterNode item : this.footerItems) {
                int rowX = sidebarX + 2;
                int rowW = sidebarW - 4;
                if (mouseX >= rowX && mouseX < rowX + rowW && mouseY >= itemY && mouseY < itemY + itemH) {
                    this.selectedModuleId = null;
                    screen.setSelectedTab(item.getTabIndex());
                    activity.client.gui.sound.SoundManager.playTabSwitch();
                    return true;
                }
                itemY += itemH + itemGap;
            }
            if (layout.sidebarHeight >= 160 && mouseY >= itemY) {
                this.selectedModuleId = null;
                screen.setSelectedTab(5);
                activity.client.gui.sound.SoundManager.playTabSwitch();
                return true;
            }
            return true;
        }

        int categoryAreaY = sidebarY + (layout.isCompact() ? ActivityMetrics.PADDING_WINDOW_COMPACT : ActivityMetrics.PADDING_WINDOW);
        int categoryAreaH = Math.max(16, footerY - categoryAreaY - 2);

        if (mouseY >= categoryAreaY && mouseY < categoryAreaY + categoryAreaH) {
            int curY = categoryAreaY - (int) Math.round(this.scrollOffset);

            ActivityConfig config = ActivityConfigManager.getConfig();
            List<String> pinnedList = config != null ? config.getPinnedModules() : List.of();

            if (!pinnedList.isEmpty()) {
                int qaItemY = curY + 16;
                for (String pinnedId : pinnedList) {
                    int rowX = sidebarX + 2;
                    int rowW = sidebarW - 4;
                    int rowY = qaItemY;

                    if (mouseX >= rowX && mouseX < rowX + rowW && mouseY >= rowY && mouseY < rowY + ITEM_HEIGHT) {
                        if (button == 1) {

                            screen.openContextMenu(pinnedId, mouseX, mouseY);
                            activity.client.gui.sound.SoundManager.playClick();
                            return true;
                        } else if (button == 0) {

                            int toggleAreaX = rowX + rowW - ((layout.isCompact() || layout.isSmallScreen()) ? 28 : 24);
                            if (mouseX >= toggleAreaX) {
                                IModule mod = ModuleRegistry.get(pinnedId);
                                if (mod != null) {
                                    mod.setEnabled(!mod.isEnabled());
                                    if (config != null) {
                                        mod.saveToConfig(config);
                                        ActivityConfigManager.save();
                                    }
                                }
                                activity.client.gui.sound.SoundManager.playToggle(mod != null && mod.isEnabled());
                                return true;
                            } else {
                                this.selectedModuleId = pinnedId;
                                screen.navigateToModule(pinnedId);
                                activity.client.gui.sound.SoundManager.playSelect();
                                return true;
                            }
                        }
                    }
                    qaItemY += ITEM_HEIGHT + ITEM_GAP;
                }
                curY += getQuickAccessHeight(pinnedList);
            }

            for (CategoryNode cat : this.categories) {
                int rowX = sidebarX + 2;
                int rowW = sidebarW - 4;
                int rowY = curY;

                int catTouchPad = (layout.isCompact() || layout.isSmallScreen()) ? 2 : 0;
                if (button == 0 && mouseY >= rowY - catTouchPad && mouseY < rowY + ITEM_HEIGHT + catTouchPad) {

                    int chevronHitbox = (layout.isCompact() || layout.isSmallScreen()) ? 26 : 22;
                    if (cat.hasChildren() && mouseX >= rowX + rowW - chevronHitbox) {
                        cat.toggleExpanded();
                        cat.setUserExpanded(cat.isExpanded());
                        cat.setSearchExpanded(false);
                        if (cat.isExpanded()) {
                            activity.client.gui.sound.SoundManager.playCategoryExpand();
                        } else {
                            activity.client.gui.sound.SoundManager.playCategoryCollapse();
                        }
                        return true;
                    }

                    if (screen.getSelectedTab() == cat.getTabIndex() && cat.hasChildren()) {
                        cat.toggleExpanded();
                        cat.setUserExpanded(cat.isExpanded());
                        cat.setSearchExpanded(false);
                        if (cat.isExpanded()) {
                            activity.client.gui.sound.SoundManager.playCategoryExpand();
                        } else {
                            activity.client.gui.sound.SoundManager.playCategoryCollapse();
                        }
                        return true;
                    } else {
                        this.selectedModuleId = null;
                        screen.setSelectedTab(cat.getTabIndex());
                        if (cat.hasChildren() && !cat.isExpanded()) {
                            cat.setExpanded(true);
                            cat.setUserExpanded(true);
                            cat.setSearchExpanded(false);
                        } else if (cat.hasChildren()) {
                            cat.setUserExpanded(true);
                            cat.setSearchExpanded(false);
                        }
                        activity.client.gui.sound.SoundManager.playTabSwitch();
                        return true;
                    }
                }

                curY += ITEM_HEIGHT + ITEM_GAP;

                if (cat.getExpandProgress() > 0.001f && cat.hasChildren()) {
                    float eased = AnimationClock.smoothStep(cat.getExpandProgress());
                    int childrenHeight = cat.getChildren().size() * (CHILD_ITEM_HEIGHT + CHILD_GAP);
                    int clipHeight = (int) Math.round(childrenHeight * eased);
                    int childY = curY;
                    boolean canClickChildren = cat.isExpanded() || cat.getExpandProgress() >= 0.5f;
                    int touchChildPad = (layout.isCompact() || layout.isSmallScreen()) ? 4 : 2;
                    for (ModuleItem child : cat.getChildren()) {
                        if (canClickChildren && mouseY >= childY - touchChildPad && mouseY < childY + CHILD_ITEM_HEIGHT + touchChildPad && mouseY < curY + clipHeight
                                && mouseX >= rowX + 4 && mouseX < rowX + rowW) {
                            if (button == 1) {

                                screen.openContextMenu(child.getId(), mouseX, mouseY);
                                activity.client.gui.sound.SoundManager.playClick();
                                return true;
                            } else if (button == 0) {
                                this.selectedModuleId = child.getId();
                                screen.navigateToModule(cat.getId(), child.getId());
                                activity.client.gui.sound.SoundManager.playSelect();
                                return true;
                            }
                        }
                        childY += CHILD_ITEM_HEIGHT + CHILD_GAP;
                    }
                    curY += clipHeight;
                }
            }
        }

        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount, WindowLayout layout) {
        int sidebarX = layout.sidebarX;
        int sidebarY = layout.sidebarY;
        int sidebarW = layout.sidebarWidth;
        int sidebarH = layout.sidebarHeight;

        if (mouseX >= sidebarX && mouseX < sidebarX + sidebarW && mouseY >= sidebarY && mouseY < sidebarY + sidebarH) {
            this.scrollOffset -= amount * 16.0;
            return true;
        }
        return false;
    }
}

package activity.client.gui.tab;

import activity.client.gui.ActivityScreen;
import activity.client.gui.component.ActivityComponent;
import activity.client.gui.component.ActivityPanel;
import activity.client.gui.layout.ScrollContainer;
import activity.client.gui.theme.ActivityColors;
import activity.client.gui.icon.ActivityIcon;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public abstract class ActivityTab {

    private final String id;
    private final Text title;
    private final Text headerTitle;
    private final ActivityIcon icon;
    protected final List<ActivityComponent> components = new ArrayList<>();
    protected double scrollAmount = 0.0;

    public ActivityTab(String id, Text title) {
        this(id, title, Text.translatable("activity.tab." + id + ".header"), null);
    }

    public ActivityTab(String id, Text title, ActivityIcon icon) {
        this(id, title, Text.translatable("activity.tab." + id + ".header"), icon);
    }

    public ActivityTab(String id, Text title, Text headerTitle) {
        this(id, title, headerTitle, null);
    }

    public ActivityTab(String id, Text title, Text headerTitle, ActivityIcon icon) {
        this.id = id;
        this.title = title;
        this.headerTitle = headerTitle;
        this.icon = icon;
    }

    public String getId() {
        return id;
    }

    public Text getTitle() {
        return title;
    }

    public Text getHeaderTitle() {
        return headerTitle;
    }

    public ActivityIcon getIcon() {
        return icon;
    }

    public abstract Text getSubtitle();

    public List<ActivityComponent> getComponents() {
        return components;
    }

    public double getScrollAmount() {
        return scrollAmount;
    }

    public void setScrollAmount(double scrollAmount) {
        this.scrollAmount = scrollAmount;
    }

    private float hoverProgress = 0.0f;

    public float getHoverProgress() {
        return hoverProgress;
    }

    public void setHoverProgress(float hoverProgress) {
        this.hoverProgress = hoverProgress;
    }

    public void updateHover(boolean hovered, float dt) {
        float target = hovered ? 1.0f : 0.0f;
        float speed = 10.0f;
        if (this.hoverProgress < target) {
            this.hoverProgress = Math.min(target, this.hoverProgress + speed * dt);
        } else if (this.hoverProgress > target) {
            this.hoverProgress = Math.max(target, this.hoverProgress - speed * dt);
        }
    }

    protected <T extends ActivityComponent> T addComponent(T component) {
        if (component != null && !this.components.contains(component)) {
            this.components.add(component);
        }
        return component;
    }

    public static class ModuleSection {
        private final String moduleId;
        private final ActivityPanel card;
        private final List<ActivityComponent> controls = new ArrayList<>();
        private final int cardHeight;

        public ModuleSection(String moduleId, ActivityPanel card, int cardHeight) {
            this.moduleId = moduleId;
            this.card = card;
            this.cardHeight = cardHeight;
        }

        public String getModuleId() { return moduleId; }
        public ActivityPanel getCard() { return card; }
        public List<ActivityComponent> getControls() { return controls; }
        public int getCardHeight() { return cardHeight; }
    }

    private final java.util.Map<String, ActivityPanel> moduleCards = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, ModuleSection> moduleSections = new java.util.LinkedHashMap<>();
    private String currentBuildingModule = null;

    public <T extends ActivityComponent> T addControl(ScrollContainer container, T component) {
        if (component != null) {
            container.addChild(component);
            addComponent(component);
            if (this.currentBuildingModule != null && this.moduleSections.containsKey(this.currentBuildingModule)) {
                this.moduleSections.get(this.currentBuildingModule).controls.add(component);
            }
        }
        return component;
    }

    public ActivityPanel createCard(ScrollContainer container, int x, int y, int width, int height, Text title) {
        ActivityPanel card = new ActivityPanel(x, y, width, height, title);
        card.setBackgroundColor(ActivityColors.PANEL_INNER_BG);
        card.setBorderColor(ActivityColors.BORDER_CARD);
        card.setHeaderHeight(18);
        container.addChild(card);
        addComponent(card);
        return card;
    }

    public void registerModuleCard(String moduleId, ActivityPanel card) {
        if (moduleId != null && card != null) {
            this.moduleCards.put(moduleId, card);
            this.currentBuildingModule = moduleId;
            ModuleSection section = new ModuleSection(moduleId, card, card.getHeight());
            this.moduleSections.put(moduleId, section);
        }
    }

    public void registerCardAlias(String aliasId, ActivityPanel card) {
        if (aliasId != null && card != null) {
            this.moduleCards.put(aliasId, card);
        }
    }

    public ActivityPanel getModuleCard(String moduleId) {
        return moduleId != null ? this.moduleCards.get(moduleId) : null;
    }

    public java.util.Map<String, ActivityPanel> getModuleCards() {
        return java.util.Collections.unmodifiableMap(this.moduleCards);
    }

    public void applySearchFilter(ScrollContainer container, String query) {
        if (container == null) return;

        if (query == null || query.isBlank()) {
            for (ModuleSection sec : this.moduleSections.values()) {
                sec.card.setVisible(true);
                for (ActivityComponent c : sec.controls) {
                    c.setVisible(true);
                }
            }
            container.restoreOriginalPositions();
            return;
        }

        java.util.Set<String> matching = new java.util.HashSet<>();
        for (ModuleSection sec : this.moduleSections.values()) {
            if (activity.client.gui.search.SearchController.matchesModule(this.getId(), sec.moduleId, query)) {
                matching.add(sec.moduleId);
            }
        }

        if (!matching.isEmpty()) {
            java.util.Map<Integer, Integer> colCurrentY = new java.util.HashMap<>();
            for (ModuleSection sec : this.moduleSections.values()) {
                int origX = container.getComponentOrigRelX(sec.card);
                int origY = container.getComponentOrigRelY(sec.card);
                if (origY >= 0 && (!colCurrentY.containsKey(origX) || origY < colCurrentY.get(origX))) {
                    colCurrentY.put(origX, origY);
                }
            }

            for (ModuleSection sec : this.moduleSections.values()) {
                boolean matches = matching.contains(sec.moduleId);
                sec.card.setVisible(matches);
                for (ActivityComponent c : sec.controls) {
                    c.setVisible(matches);
                }

                if (matches) {
                    int origX = container.getComponentOrigRelX(sec.card);
                    int curY = colCurrentY.getOrDefault(origX, 0);
                    int origCardY = container.getComponentOrigRelY(sec.card);
                    int deltaY = curY - origCardY;
                    container.setComponentRelY(sec.card, curY);
                    for (ActivityComponent c : sec.controls) {
                        int origCtrlY = container.getComponentOrigRelY(c);
                        container.setComponentRelY(c, origCtrlY + deltaY);
                    }
                    colCurrentY.put(origX, curY + sec.cardHeight + 10);
                }
            }
            container.recomputeContentHeight();
            container.scrollTo(0);
        } else {

            for (ModuleSection sec : this.moduleSections.values()) {
                sec.card.setVisible(false);
                for (ActivityComponent c : sec.controls) {
                    c.setVisible(false);
                }
            }
            container.recomputeContentHeight();
            container.scrollTo(0);
        }
    }

    public void clearComponents() {
        this.components.clear();
        this.moduleCards.clear();
        this.moduleSections.clear();
        this.currentBuildingModule = null;
    }

    public abstract void buildTab(ActivityScreen screen, ScrollContainer container, int startX, int startY, int rowWidth);

    public activity.client.module.api.ModuleCategory getCategory() {
        return null;
    }

    public abstract void resetDefaults();

    public abstract void loadFromConfig(activity.client.config.ActivityConfig config);

    public abstract void saveToConfig(activity.client.config.ActivityConfig config);
}

package activity.client.gui;

import activity.client.ActivityClient;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.animation.AnimationClock;
import activity.client.gui.component.ActivityButton;
import activity.client.gui.component.ActivityComponent;
import activity.client.gui.component.ActivityDropdown;
import activity.client.gui.component.ActivityKeybindButton;
import activity.client.gui.component.ActivityPanel;
import activity.client.gui.component.ActivitySlider;
import activity.client.gui.component.ActivityTextField;
import activity.client.gui.component.ActivityToggle;
import activity.client.gui.layout.ScrollContainer;
import activity.client.gui.layout.WindowLayout;
import activity.client.gui.menu.ModuleContextMenu;
import activity.client.gui.modal.ModalManager;
import activity.client.gui.overlay.OverlayManager;
import activity.client.gui.render.ActivityGuiRenderer;
import activity.client.gui.render.ScissorHelper;
import activity.client.gui.search.ActivitySearchBar;
import activity.client.gui.sheet.AboutModuleSheet;
import activity.client.gui.sidebar.SidebarTree;
import activity.client.gui.tab.AboutTab;
import activity.client.gui.tab.ActivityTab;
import activity.client.gui.tab.CombatTab;
import activity.client.gui.tab.ConfigTab;
import activity.client.gui.tab.DefenseTab;
import activity.client.gui.tab.SettingsTab;
import activity.client.gui.tab.TabManager;
import activity.client.gui.tab.UtilityTab;
import activity.client.gui.theme.ActivityColors;
import activity.client.gui.theme.ActivityMetrics;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Main window screen and GUI orchestrator for Activity.
 * Manages top-level layout, component dispatching, focus coordination,
 * overlay routing, and tab switching with full state preservation.
 */
public class ActivityScreen extends Screen {

    private static final Text HEADER_TITLE = Text.translatable("activity.gui.header_title");
    private static final Text CONTENT_TITLE = Text.translatable("activity.gui.content_title");
    private static final Text CONTENT_SUBTITLE = Text.translatable("activity.gui.content_subtitle");

    protected final OverlayManager overlayManager = new OverlayManager();
    protected final ModalManager modalManager = new ModalManager(this.overlayManager);
    protected final List<ActivityComponent> components = new ArrayList<>();
    @Nullable
    protected ActivityComponent focusedComponent = null;

    private float currentAnimProgress = 1.0f;
    private float openTimer = 0.0f;
    private float closeTimer = 0.0f;
    private float closeStartProgress = 1.0f;
    private float tabSwitchProgress = 1.0f;
    private boolean closing = false;
    private boolean initializedOnce = false;

    // Maximize / Restore animation bounds
    private boolean isMaximizingTransition = false;
    private float maximizeAnimTimer = 1.0f;
    private int maximizeOldX = 0;
    private int maximizeOldY = 0;
    private int maximizeOldW = 0;
    private int maximizeOldH = 0;
    private int maximizeTargetX = 0;
    private int maximizeTargetY = 0;
    private int maximizeTargetW = 0;
    private int maximizeTargetH = 0;

    // Layout cache
    private WindowLayout cachedLayout = null;
    private int lastScreenWidth = -1;
    private int lastScreenHeight = -1;
    private int lastDragWinX = -1;
    private int lastDragWinY = -1;
    private int lastDragWinW = -1;
    private int lastDragWinH = -1;
    private boolean lastMaximized = false;

    // Text measurement cache
    private Text cachedDisplayHeader = null;
    private int cachedHeaderW = -1;
    private int lastMaxHeaderTitleW = -1;

    private Text cachedDisplayTitle = null;
    private int lastMaxTitleW = -1;
    private ActivityTab lastActiveTabTitle = null;

    private Text cachedDisplaySub = null;
    private int lastMaxSubW = -1;
    private ActivityTab lastActiveTabSub = null;

    private final activity.client.gui.layout.WindowDragController dragController = new activity.client.gui.layout.WindowDragController();
    private activity.client.gui.component.WindowControlButtons controlButtons;

    // ==========================================
    // GUI SESSION MEMORY (1-minute TTL)
    // ==========================================
    public static final long SESSION_MEMORY_TTL_MS = 60_000L;
    private static String lastSessionTabId = null;
    private static String lastSessionModuleId = null;
    private static long lastSessionCloseTimestamp = 0L;

    public static void recordSession(String tabId, String moduleId) {
        lastSessionTabId = tabId;
        lastSessionModuleId = moduleId;
        lastSessionCloseTimestamp = System.currentTimeMillis();
    }

    public static boolean hasValidSession() {
        return lastSessionTabId != null && (System.currentTimeMillis() - lastSessionCloseTimestamp <= SESSION_MEMORY_TTL_MS);
    }

    public static String getLastSessionTabId() {
        return lastSessionTabId;
    }

    public static String getLastSessionModuleId() {
        return lastSessionModuleId;
    }

    public static long getLastSessionCloseTimestamp() {
        return lastSessionCloseTimestamp;
    }

    public static void clearSession() {
        lastSessionTabId = null;
        lastSessionModuleId = null;
        lastSessionCloseTimestamp = 0L;
    }

    public boolean isMaximized() {
        return this.dragController.isMaximized();
    }

    public void invalidateLayoutCache() {
        this.cachedLayout = null;
    }

    public void invalidateTextCache() {
        this.cachedDisplayHeader = null;
        this.cachedHeaderW = -1;
        this.lastMaxHeaderTitleW = -1;
        this.cachedDisplayTitle = null;
        this.lastMaxTitleW = -1;
        this.lastActiveTabTitle = null;
        this.cachedDisplaySub = null;
        this.lastMaxSubW = -1;
        this.lastActiveTabSub = null;
        if (this.sidebarTree != null) {
            this.sidebarTree.invalidateTextCache();
        }
    }

    public WindowLayout computeLayout() {
        int winX = this.dragController.getWindowX();
        int winY = this.dragController.getWindowY();
        int winW = this.dragController.getWindowWidth();
        int winH = this.dragController.getWindowHeight();
        boolean max = this.dragController.isMaximized();

        if (this.cachedLayout != null &&
            this.lastScreenWidth == this.width &&
            this.lastScreenHeight == this.height &&
            this.lastDragWinX == winX &&
            this.lastDragWinY == winY &&
            this.lastDragWinW == winW &&
            this.lastDragWinH == winH &&
            this.lastMaximized == max) {
            return this.cachedLayout;
        }

        this.lastScreenWidth = this.width;
        this.lastScreenHeight = this.height;
        this.lastDragWinX = winX;
        this.lastDragWinY = winY;
        this.lastDragWinW = winW;
        this.lastDragWinH = winH;
        this.lastMaximized = max;
        this.cachedLayout = WindowLayout.compute(this.width, this.height, winX, winY, winW, winH, max);
        return this.cachedLayout;
    }

    protected final TabManager tabManager = new TabManager(List.of(
        new CombatTab(),
        new DefenseTab(),
        new UtilityTab(),
        new ConfigTab(),
        new SettingsTab(),
        new AboutTab()
    ));

    protected final SidebarTree sidebarTree = new SidebarTree();
    private String activeSearchQuery = "";
    protected final ActivitySearchBar searchBar = new ActivitySearchBar(
        0, 0, 120, 18,
        result -> {
            if (result != null) {
                this.onSearchResultSelected(result);
            }
        }
    );
    protected ScrollContainer currentScrollContainer = null;

    private final Runnable fontChangeListener = () -> {
        this.invalidateLayoutCache();
        this.invalidateTextCache();
        double currentScroll = this.tabManager.getSelectedTab() != null ? this.tabManager.getSelectedTab().getScrollAmount() : 0;
        boolean searchFocused = this.searchBar.isFocused();
        this.clearComponents();
        this.overlayManager.clear();
        this.focusedComponent = null;
        WindowLayout layout = computeLayout();
        this.initLayout(layout);
        if (this.currentScrollContainer != null) {
            this.currentScrollContainer.setScrollAmount(currentScroll);
        }
        if (this.tabManager.getSelectedTab() != null) {
            this.tabManager.getSelectedTab().setScrollAmount(currentScroll);
        }
        if (searchFocused) {
            this.searchBar.setFocused(true);
            this.focusedComponent = this.searchBar;
        }
    };

    public ActivityScreen() {
        super(HEADER_TITLE);
        this.tabManager.loadAllFromConfig(ActivityConfigManager.getConfig());
    }

    @Override
    protected void init() {
        super.init();
        AnimationClock.reset();
        this.searchBar.clear();
        this.searchBar.setFocused(false);
        this.activeSearchQuery = "";
        this.clearComponents();
        this.overlayManager.clear();
        this.focusedComponent = null;

        activity.client.config.ActivityConfig cfg = activity.client.config.ActivityConfigManager.getConfig();
        boolean globalAnim = cfg == null || cfg.animationsEnabled;
        boolean spatialAnim = globalAnim && cfg.spatialOpenAnimation;

        if (this.closing) {
            finishClose();
            return;
        }

        if (this.initializedOnce) {
            this.openTimer = 1.0f;
            this.currentAnimProgress = 1.0f;
            this.closing = false;
        } else {
            this.openTimer = spatialAnim ? 0.0f : 1.0f;
            this.currentAnimProgress = spatialAnim ? 0.0f : 1.0f;
            this.closing = false;
            activity.client.gui.sound.SoundManager.playOpen();
        }

        this.dragController.init();
        this.controlButtons = new activity.client.gui.component.WindowControlButtons(
            () -> {
                activity.client.config.ActivityConfigManager.load();
                this.tabManager.loadAllFromConfig(activity.client.config.ActivityConfigManager.getConfig());
                this.reloadCurrentTab();
            },
            this::toggleMaximize,
            this::close,
            () -> this.dragController.isMaximized()
        );

        activity.client.gui.font.FontManager.addListener(this.fontChangeListener);
        this.searchBar.setOnQueryChange(this::onSearchQueryChanged);

        if (!this.initializedOnce) {
            if (hasValidSession()) {
                int savedIndex = this.tabManager.getTabIndexById(lastSessionTabId);
                if (savedIndex >= 0) {
                    this.tabManager.setSelectedIndex(savedIndex);
                    if (lastSessionModuleId != null) {
                        this.sidebarTree.setSelectedModule(lastSessionTabId, lastSessionModuleId);
                    } else {
                        this.sidebarTree.clearSelectedModule();
                    }
                } else {
                    this.tabManager.setSelectedIndex(0);
                    this.sidebarTree.clearSelectedModule();
                }
            } else {
                this.tabManager.setSelectedIndex(0);
                this.sidebarTree.clearSelectedModule();
            }
        }

        WindowLayout layout = computeLayout();
        this.dragController.clampWindowPosition(this.width, this.height, layout.windowWidth, layout.windowHeight);
        layout = computeLayout();
        this.initLayout(layout);
        this.initializedOnce = true;
    }

    public void toggleMaximize() {
        WindowLayout currentLayout = computeLayout();
        int oldX = currentLayout.windowX;
        int oldY = currentLayout.windowY;
        int oldW = currentLayout.windowWidth;
        int oldH = currentLayout.windowHeight;

        this.dragController.toggleMaximize(currentLayout);
        this.invalidateLayoutCache();
        this.invalidateTextCache();
        double currentScroll = this.tabManager.getSelectedTab() != null ? this.tabManager.getSelectedTab().getScrollAmount() : 0;
        boolean searchFocused = this.searchBar.isFocused();
        WindowLayout newLayout = computeLayout();
        this.dragController.clampWindowPosition(this.width, this.height, newLayout.windowWidth, newLayout.windowHeight);
        newLayout = computeLayout();
        this.clearComponents();
        this.overlayManager.clear();
        this.focusedComponent = null;
        this.initLayout(newLayout);
        if (this.currentScrollContainer != null) {
            this.currentScrollContainer.setScrollAmount(currentScroll);
        }
        if (this.tabManager.getSelectedTab() != null) {
            this.tabManager.getSelectedTab().setScrollAmount(currentScroll);
        }
        if (searchFocused) {
            this.searchBar.setFocused(true);
            this.focusedComponent = this.searchBar;
        }

        activity.client.config.ActivityConfig config = activity.client.config.ActivityConfigManager.getConfig();
        boolean animEnabled = config == null || config.animationsEnabled;
        if (animEnabled) {
            this.isMaximizingTransition = true;
            this.maximizeAnimTimer = 0.0f;
            this.maximizeOldX = oldX;
            this.maximizeOldY = oldY;
            this.maximizeOldW = oldW;
            this.maximizeOldH = oldH;
            this.maximizeTargetX = newLayout.windowX;
            this.maximizeTargetY = newLayout.windowY;
            this.maximizeTargetW = newLayout.windowWidth;
            this.maximizeTargetH = newLayout.windowHeight;
        } else {
            this.isMaximizingTransition = false;
            this.maximizeAnimTimer = 1.0f;
        }
    }

    public void recenterWindow() {
        this.dragController.recenter();
        this.invalidateLayoutCache();
        this.invalidateTextCache();
        double currentScroll = this.tabManager.getSelectedTab() != null ? this.tabManager.getSelectedTab().getScrollAmount() : 0;
        WindowLayout newLayout = computeLayout();
        this.clearComponents();
        this.overlayManager.clear();
        this.focusedComponent = null;
        this.searchBar.setFocused(false);
        this.initLayout(newLayout);
        if (this.currentScrollContainer != null) {
            this.currentScrollContainer.setScrollAmount(currentScroll);
        }
        if (this.tabManager.getSelectedTab() != null) {
            this.tabManager.getSelectedTab().setScrollAmount(currentScroll);
        }
    }

    public void reloadCurrentTab() {
        this.invalidateLayoutCache();
        this.invalidateTextCache();
        double currentScroll = this.tabManager.getSelectedTab() != null ? this.tabManager.getSelectedTab().getScrollAmount() : 0;
        boolean searchFocused = this.searchBar.isFocused();
        WindowLayout newLayout = computeLayout();
        this.dragController.clampWindowPosition(this.width, this.height, newLayout.windowWidth, newLayout.windowHeight);
        newLayout = computeLayout();
        this.clearComponents();
        this.overlayManager.clear();
        this.focusedComponent = null;
        this.initLayout(newLayout);
        if (this.currentScrollContainer != null) {
            this.currentScrollContainer.setScrollAmount(currentScroll);
        }
        if (this.tabManager.getSelectedTab() != null) {
            this.tabManager.getSelectedTab().setScrollAmount(currentScroll);
        }
        if (searchFocused) {
            this.searchBar.setFocused(true);
            this.focusedComponent = this.searchBar;
        }
    }

    /**
     * Layout initialization hook. Arranges tab-specific components inside a scrollable container within the content area.
     * Preserves state and scroll position across tab switches and window resizes.
     *
     * @param layout the calculated responsive window layout
     */
    protected void initLayout(WindowLayout layout) {
        int padContent = layout.getContentPadding();
        int titleX = layout.contentX + padContent;
        int titleY = layout.contentY + padContent;

        // Position search bar responsively at top-right of content header
        int maxSearchW = layout.isCompact() ? 100 : 130;
        int minSearchW = Math.min(layout.isSmallScreen() ? 50 : 70, (int) (layout.contentWidth * 0.38f));
        int searchW = Math.clamp((int) (layout.contentWidth * 0.32f), minSearchW, maxSearchW);
        int searchX = layout.contentX + layout.contentWidth - padContent - searchW;
        int searchH = layout.isCompact() ? 16 : 18;

        this.searchBar.setX(searchX);
        this.searchBar.setY(titleY);
        this.searchBar.setWidth(searchW);
        this.searchBar.setHeight(searchH);
        this.addComponent(this.searchBar);

        // Content vertical start coordinate: on small screens, save vertical room by omitting secondary subtitle
        int headerTextH;
        if (layout.isSmallScreen() || layout.contentHeight < 100) {
            headerTextH = this.textRenderer.fontHeight + 4;
        } else if (layout.isCompact()) {
            headerTextH = this.textRenderer.fontHeight * 2 + 4;
        } else {
            headerTextH = this.textRenderer.fontHeight * 2 + 10;
        }

        int contentStartY = titleY + headerTextH;
        int contentHeight = Math.max(40, (layout.contentY + layout.contentHeight) - contentStartY - padContent);
        int contentWidth = Math.max(90, layout.contentWidth - padContent * 2);

        ActivityTab activeTab = this.tabManager.getSelectedTab();

        // Create ScrollContainer taking the full content area
        ScrollContainer scrollContainer = new ScrollContainer(titleX, contentStartY, contentWidth, contentHeight);
        scrollContainer.setOverlayManager(this.overlayManager);
        scrollContainer.setScrollAmount(activeTab != null ? activeTab.getScrollAmount() : 0);
        if (activeTab != null) {
            scrollContainer.setOnScroll(activeTab::setScrollAmount);
        }

        int rowW = contentWidth - ActivityMetrics.SCROLLBAR_WIDTH - (layout.isCompact() ? 4 : 6);
        int effectiveRowW = Math.min(rowW, ActivityMetrics.CONTENT_MAX_WIDTH);
        int effectiveStartX = titleX + (rowW - effectiveRowW) / 2;

        // Build widgets and cards for the active tab inside the scroll container
        if (activeTab != null) {
            activeTab.buildTab(this, scrollContainer, effectiveStartX, contentStartY, effectiveRowW);
        }

        this.currentScrollContainer = scrollContainer;
        this.addComponent(scrollContainer);

        if (!this.initializedOnce && hasValidSession() && lastSessionModuleId != null && activeTab != null) {
            ActivityPanel card = activeTab.getModuleCard(lastSessionModuleId);
            if (card != null) {
                scrollContainer.scrollToChild(card);
            }
        }

        if (activeTab != null && !this.activeSearchQuery.isEmpty()) {
            activeTab.applySearchFilter(scrollContainer, this.activeSearchQuery);
        }
    }

    private void onSearchQueryChanged(String query) {
        this.activeSearchQuery = query != null ? query.trim() : "";
        this.sidebarTree.applySearchFilter(this.activeSearchQuery);
        ActivityTab activeTab = this.tabManager.getSelectedTab();
        if (activeTab != null && this.currentScrollContainer != null) {
            activeTab.applySearchFilter(this.currentScrollContainer, this.activeSearchQuery);
        }
    }

    private void onSearchResultSelected(activity.client.gui.search.SearchController.SearchResult result) {
        if (result == null) return;
        String catId = result.entry().categoryId();
        String modId = result.entry().moduleId();

        this.searchBar.clear();
        this.searchBar.setFocused(false);
        this.focusedComponent = null;

        this.navigateToModule(catId, modId);

        ActivityTab activeTab = this.tabManager.getSelectedTab();
        if (activeTab != null) {
            ActivityPanel card = activeTab.getModuleCard(modId);
            if (card != null) {
                card.flashHighlight();
            }
        }
    }

    public void navigateToModule(String categoryId, String moduleId) {
        int tabIndex = this.tabManager.getTabIndexById(categoryId);
        if (tabIndex >= 0 && tabIndex != this.tabManager.getSelectedIndex()) {
            this.setSelectedTab(tabIndex);
        }
        this.sidebarTree.setSelectedModule(categoryId, moduleId);
        ActivityTab activeTab = this.tabManager.getSelectedTab();
        if (activeTab != null && this.currentScrollContainer != null) {
            ActivityPanel card = activeTab.getModuleCard(moduleId);
            if (card != null) {
                this.currentScrollContainer.scrollToChild(card);
                card.flashHighlight();
            } else {
                this.currentScrollContainer.scrollTo(0);
            }
        }
    }

    public void navigateToModule(String moduleId) {
        if (moduleId == null) return;
        activity.client.module.api.ModuleMetadata meta = activity.client.module.api.ModuleRegistry.getMetadata(moduleId);
        if (meta != null && meta.getCategory() != null) {
            navigateToModule(meta.getCategory().getId(), moduleId);
            return;
        }
        for (int i = 0; i < this.tabManager.getTabCount(); i++) {
            ActivityTab tab = this.tabManager.getTab(i);
            if (tab.getModuleCard(moduleId) != null) {
                navigateToModule(tab.getId(), moduleId);
                return;
            }
        }
    }

    public void openContextMenu(String moduleId, double mouseX, double mouseY) {
        if (moduleId == null) return;
        this.overlayManager.open(new ModuleContextMenu(this, moduleId, mouseX, mouseY));
    }

    public void openAboutModuleSheet(String moduleId) {
        if (moduleId == null) return;
        this.overlayManager.open(new AboutModuleSheet(this, moduleId));
    }

    public void showToast(activity.client.gui.overlay.ToastOverlay toast) {
        if (toast == null) return;
        this.overlayManager.closeMatching(o -> o instanceof activity.client.gui.overlay.ToastOverlay);
        this.overlayManager.open(toast);
    }

    public void showToast(Text message, @Nullable Text actionLabel, @Nullable Runnable onAction) {
        showToast(new activity.client.gui.overlay.ToastOverlay(message, actionLabel, onAction));
    }

    public void showOpenUrlErrorToast(String url) {
        showToast(activity.client.gui.overlay.ToastOverlay.forUrlError(url));
    }

    protected <T extends ActivityComponent> T addComponent(T component) {
        if (component != null && !this.components.contains(component)) {
            this.components.add(component);
        }
        return component;
    }

    protected void removeComponent(ActivityComponent component) {
        this.components.remove(component);
        if (this.focusedComponent == component) {
            this.focusedComponent = null;
        }
    }

    private void clearAllContainerFocus() {
        for (ActivityComponent comp : this.components) {
            if (comp instanceof ScrollContainer sc) {
                sc.clearFocus();
            }
        }
    }

    protected void clearComponents() {
        clearAllContainerFocus();
        this.components.clear();
        this.focusedComponent = null;
        this.currentScrollContainer = null;
    }

    public OverlayManager getOverlayManager() {
        return this.overlayManager;
    }

    public ModalManager getModalManager() {
        return this.modalManager;
    }

    public TabManager getTabManager() {
        return this.tabManager;
    }

    public int getSelectedTab() {
        return this.tabManager.getSelectedIndex();
    }

    public float getTabSwitchProgress() {
        return tabSwitchProgress;
    }

    public void setSelectedTab(int selectedTab) {
        if (selectedTab >= 0 && selectedTab < this.tabManager.getTabCount() &&
            this.tabManager.getSelectedIndex() != selectedTab) {
            this.searchBar.setFocused(false);
            if (this.focusedComponent == this.searchBar) {
                this.focusedComponent = null;
            }
            this.tabManager.setSelectedIndex(selectedTab);
            this.clearComponents();
            this.invalidateTextCache();
            WindowLayout layout = computeLayout();
            this.initLayout(layout);
            activity.client.gui.sound.SoundManager.playTabSwitch();
            this.tabSwitchProgress = AnimationClock.isAnimationsEnabled() ? 0.0f : 1.0f;
        }
    }

    /**
     * Cycles focus between interactive components using Tab / Shift+Tab.
     * Automatically scrolls the viewport if the focused component is inside a ScrollContainer.
     */
    protected void cycleFocus(boolean forward) {
        List<ActivityComponent> focusable = new ArrayList<>();
        for (ActivityComponent comp : this.components) {
            if (comp instanceof ScrollContainer sc) {
                focusable.addAll(sc.getFocusableComponents());
            } else if (comp.isVisible() && comp.isEnabled() &&
                (comp instanceof ActivityTextField || comp instanceof ActivityButton ||
                 comp instanceof ActivityToggle || comp instanceof ActivityDropdown ||
                 comp instanceof ActivitySlider || comp instanceof ActivityKeybindButton)) {
                focusable.add(comp);
            }
        }

        if (focusable.isEmpty()) return;

        int currentIndex = focusable.indexOf(this.focusedComponent);
        int nextIndex;
        if (currentIndex < 0) {
            nextIndex = forward ? 0 : focusable.size() - 1;
        } else {
            nextIndex = forward ? (currentIndex + 1) % focusable.size() : (currentIndex - 1 + focusable.size()) % focusable.size();
        }

        if (this.focusedComponent != null) {
            this.focusedComponent.setFocused(false);
        }

        ActivityComponent next = focusable.get(nextIndex);
        next.setFocused(true);
        this.focusedComponent = next;

        // Auto-scroll ScrollContainer to reveal newly focused widget
        for (ActivityComponent comp : this.components) {
            if (comp instanceof ScrollContainer sc) {
                if (sc.hasChild(next)) {
                    sc.setFocusedChild(next);
                    sc.scrollToVisible(next);
                } else {
                    sc.clearFocus();
                }
            }
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        float alphaFactor = Math.clamp(this.currentAnimProgress, 0.0f, 1.0f);
        int overlayColor = ActivityColors.scaleAlpha(ActivityColors.BACKGROUND_OVERLAY, alphaFactor);
        ActivityGuiRenderer.fill(context, 0, 0, this.width, this.height, overlayColor);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        AnimationClock.tick();
        this.renderBackground(context, mouseX, mouseY, delta);

        activity.client.config.ActivityConfig config = activity.client.config.ActivityConfigManager.getConfig();
        boolean globalAnim = config == null || config.animationsEnabled;
        boolean spatialAnim = globalAnim && config.spatialOpenAnimation;

        if (!spatialAnim) {
            this.currentAnimProgress = this.closing ? 0.0f : 1.0f;
            if (this.closing) {
                finishClose();
                return;
            }
        } else if (this.closing) {
            float dt = AnimationClock.getDeltaTime();
            this.closeTimer = Math.min(1.0f, this.closeTimer + dt / AnimationClock.DURATION_SCREEN_CLOSE);
            if (this.closeTimer >= 1.0f) {
                this.currentAnimProgress = 0.0f;
                finishClose();
                return;
            }
            float closeFactor = 1.0f - AnimationClock.easeInCubic(this.closeTimer);
            this.currentAnimProgress = this.closeStartProgress * closeFactor;
            if (this.currentAnimProgress <= 0.002f) {
                finishClose();
                return;
            }
        } else if (this.openTimer < 1.0f) {
            float dt = AnimationClock.getDeltaTime();
            this.openTimer = Math.min(1.0f, this.openTimer + dt / AnimationClock.DURATION_SCREEN_OPEN);
            this.currentAnimProgress = AnimationClock.easeOutCubic(this.openTimer);
            if (this.openTimer >= 1.0f) {
                this.currentAnimProgress = 1.0f;
            }
        } else {
            this.currentAnimProgress = 1.0f;
        }

        if (this.tabSwitchProgress < 1.0f) {
            if (!globalAnim) {
                this.tabSwitchProgress = 1.0f;
            } else {
                float dt = AnimationClock.getDeltaTime();
                this.tabSwitchProgress = Math.min(1.0f, this.tabSwitchProgress + dt / AnimationClock.DURATION_TAB_SWITCH);
            }
        }

        boolean isMaximizeAnimating = globalAnim && this.isMaximizingTransition;
        if (isMaximizeAnimating) {
            float dt = AnimationClock.getDeltaTime();
            this.maximizeAnimTimer = Math.min(1.0f, this.maximizeAnimTimer + dt / 0.16f);
            if (this.maximizeAnimTimer >= 1.0f) {
                this.isMaximizingTransition = false;
                isMaximizeAnimating = false;
            }
        }

        WindowLayout layout = computeLayout();

        double windowOpacity = config != null ? config.windowOpacity : 85.0;
        double panelOpacity = config != null ? config.panelOpacity : 65.0;
        boolean glassEffect = config == null || config.glassEffect;

        float progress = this.currentAnimProgress;
        boolean isAnimating = spatialAnim && (progress < 0.999f || this.closing);

        float scale = 0.88f + 0.12f * progress;
        float offsetX = 30.0f * (1.0f - progress);
        float offsetY = 25.0f * (1.0f - progress);
        float centerX = layout.windowX + layout.windowWidth / 2.0f;
        float centerY = layout.windowY + layout.windowHeight / 2.0f;

        int effectiveMouseX = (isAnimating || isMaximizeAnimating) ? -1 : mouseX;
        int effectiveMouseY = (isAnimating || isMaximizeAnimating) ? -1 : mouseY;

        context.getMatrices().pushMatrix();
        if (isAnimating) {
            context.getMatrices().translate(offsetX, offsetY);
            context.getMatrices().scaleAround(scale, scale, centerX, centerY);
        } else if (isMaximizeAnimating) {
            float t = AnimationClock.easeOutCubic(this.maximizeAnimTimer);
            float curX = this.maximizeOldX + (this.maximizeTargetX - this.maximizeOldX) * t;
            float curY = this.maximizeOldY + (this.maximizeTargetY - this.maximizeOldY) * t;
            float curW = this.maximizeOldW + (this.maximizeTargetW - this.maximizeOldW) * t;
            float curH = this.maximizeOldH + (this.maximizeTargetH - this.maximizeOldH) * t;
            float scaleX = this.maximizeTargetW > 0 ? curW / (float) this.maximizeTargetW : 1.0f;
            float scaleY = this.maximizeTargetH > 0 ? curH / (float) this.maximizeTargetH : 1.0f;
            context.getMatrices().translate(curX - this.maximizeTargetX * scaleX, curY - this.maximizeTargetY * scaleY);
            context.getMatrices().scale(scaleX, scaleY);
        }

        try {
            float alphaFactor = Math.clamp(progress, 0.0f, 1.0f);
            int windowBg = ActivityColors.getWindowBackgroundColor(windowOpacity, alphaFactor);
            int headerBg = ActivityColors.scaleAlphaPercent(ActivityColors.HEADER_BACKGROUND, windowOpacity * alphaFactor);
            int sidebarBg = ActivityColors.scaleAlphaPercent(ActivityColors.SIDEBAR_BACKGROUND, windowOpacity * alphaFactor);
            int panelBg = ActivityColors.getPanelBackgroundColor(panelOpacity, alphaFactor);
            int borderColor = ActivityColors.scaleAlpha(ActivityColors.BORDER, alphaFactor);
            int headTextColor = ActivityColors.scaleAlpha(ActivityColors.TEXT_PRIMARY, alphaFactor);
            float tabTransitionFactor = AnimationClock.easeOutCubic(this.tabSwitchProgress);
            int pageTitleColor = ActivityColors.scaleAlpha(ActivityColors.ACCENT_LIGHT, alphaFactor * tabTransitionFactor);
            int pageSubtitleColor = ActivityColors.scaleAlpha(ActivityColors.TEXT_SECONDARY, alphaFactor * tabTransitionFactor);

            // Step 1: Main window base, border, soft drop shadow, and glass highlights
            ActivityGuiRenderer.drawWindowFrame(context, layout.windowX, layout.windowY, layout.windowWidth, layout.windowHeight, windowBg, borderColor, glassEffect);

            // Step 2: Dark header bar with centered title and top-right controls
            ActivityGuiRenderer.fill(context, layout.headerX, layout.headerY, layout.headerWidth, layout.headerHeight, headerBg);
            ActivityGuiRenderer.drawHorizontalLine(context, layout.headerX, layout.headerY + layout.headerHeight - 1, layout.headerWidth, borderColor);
            int headTextY = layout.headerY + (layout.headerHeight - this.textRenderer.fontHeight) / 2;
            int controlsStartX = this.controlButtons != null ? this.controlButtons.getStartX(layout) : layout.headerX + layout.headerWidth - 6;
            int maxHeaderTitleW = controlsStartX - layout.headerX - 12;

            Text displayHeader;
            if (this.cachedDisplayHeader != null && this.lastMaxHeaderTitleW == maxHeaderTitleW) {
                displayHeader = this.cachedDisplayHeader;
            } else {
                Text wrappedHeader = activity.client.gui.font.FontManager.wrap(HEADER_TITLE);
                int headerW = this.textRenderer.getWidth(wrappedHeader);
                int idealCenterX = layout.headerX + layout.headerWidth / 2;
                this.cachedHeaderW = headerW;
                this.lastMaxHeaderTitleW = maxHeaderTitleW;
                if (idealCenterX + headerW / 2 > controlsStartX - 4) {
                    int ellipsisW = this.textRenderer.getWidth("…");
                    int targetW = Math.max(0, maxHeaderTitleW - ellipsisW);
                    String trimmed = this.textRenderer.trimToWidth(wrappedHeader.getString(), targetW) + "…";
                    if (this.textRenderer.getWidth(trimmed) > maxHeaderTitleW) {
                        trimmed = this.textRenderer.trimToWidth(trimmed, Math.max(0, maxHeaderTitleW));
                    }
                    this.cachedDisplayHeader = headerW > maxHeaderTitleW ? Text.literal(trimmed) : wrappedHeader;
                } else {
                    this.cachedDisplayHeader = wrappedHeader;
                }
                displayHeader = this.cachedDisplayHeader;
            }

            int idealCenterX = layout.headerX + layout.headerWidth / 2;
            if (idealCenterX + this.cachedHeaderW / 2 > controlsStartX - 4) {
                int titleX = layout.headerX + 8;
                ActivityGuiRenderer.drawText(context, this.textRenderer, displayHeader, titleX, headTextY, headTextColor);
            } else {
                context.drawCenteredTextWithShadow(this.textRenderer, displayHeader, idealCenterX, headTextY, headTextColor);
            }

            // Step 2.5: Top-right window controls (Recenter, Reload, Close)
            if (this.controlButtons != null) {
                this.controlButtons.render(context, effectiveMouseX, effectiveMouseY, layout, alphaFactor);
            }

            // Step 3: Sidebar background and vertical divider line
            ActivityGuiRenderer.fill(context, layout.sidebarX, layout.sidebarY, layout.sidebarWidth, layout.sidebarHeight, sidebarBg);
            ActivityGuiRenderer.drawVerticalLine(context, layout.sidebarX + layout.sidebarWidth, layout.sidebarY, layout.sidebarHeight, borderColor);

            // Step 4: Collapsible hierarchical sidebar navigation tree
            this.sidebarTree.render(context, effectiveMouseX, effectiveMouseY, this.textRenderer, layout, this.tabManager, alphaFactor);

            // Step 5: Content Area Panel
            ActivityGuiRenderer.drawPanel(context, layout.contentX, layout.contentY, layout.contentWidth, layout.contentHeight,
                panelBg, borderColor, glassEffect);

            // Content Area Header Texts
            int padContent = layout.getContentPadding();
            int titleX = layout.contentX + padContent;
            int titleY = layout.contentY + padContent;
            int maxTitleW = Math.max(10, this.searchBar.getX() - titleX - 4);

            ActivityTab activeTab = this.tabManager.getSelectedTab();
            Text displayTitle;
            if (this.cachedDisplayTitle != null && this.lastMaxTitleW == maxTitleW && this.lastActiveTabTitle == activeTab) {
                displayTitle = this.cachedDisplayTitle;
            } else {
                Text pageTitle = activeTab != null ? activeTab.getHeaderTitle() : CONTENT_TITLE;
                Text wrappedTitle = activity.client.gui.font.FontManager.wrap(pageTitle);
                if (this.textRenderer.getWidth(wrappedTitle) > maxTitleW) {
                    this.cachedDisplayTitle = Text.literal(this.textRenderer.trimToWidth(wrappedTitle.getString(), Math.max(6, maxTitleW - 6)) + "…");
                } else {
                    this.cachedDisplayTitle = wrappedTitle;
                }
                this.lastMaxTitleW = maxTitleW;
                this.lastActiveTabTitle = activeTab;
                displayTitle = this.cachedDisplayTitle;
            }
            ActivityGuiRenderer.drawText(context, this.textRenderer, displayTitle, titleX, titleY, pageTitleColor);

            if (!layout.isSmallScreen() && layout.contentHeight >= 100) {
                Text displaySub;
                if (this.cachedDisplaySub != null && this.lastMaxSubW == maxTitleW && this.lastActiveTabSub == activeTab) {
                    displaySub = this.cachedDisplaySub;
                } else {
                    Text pageSubtitle = activeTab != null ? activeTab.getSubtitle() : CONTENT_SUBTITLE;
                    Text wrappedSub = activity.client.gui.font.FontManager.wrap(pageSubtitle);
                    if (this.textRenderer.getWidth(wrappedSub) > maxTitleW) {
                        this.cachedDisplaySub = Text.literal(this.textRenderer.trimToWidth(wrappedSub.getString(), Math.max(6, maxTitleW - 6)) + "…");
                    } else {
                        this.cachedDisplaySub = wrappedSub;
                    }
                    this.lastMaxSubW = maxTitleW;
                    this.lastActiveTabSub = activeTab;
                    displaySub = this.cachedDisplaySub;
                }
                ActivityGuiRenderer.drawText(context, this.textRenderer, displaySub, titleX, titleY + this.textRenderer.fontHeight + 3, pageSubtitleColor);
            }

            // Step 6: Render active UI components with smooth tab transition
            boolean isTabSwitching = AnimationClock.isAnimationsEnabled() && this.tabSwitchProgress < 0.999f;
            float tabSlideOffsetY = isTabSwitching ? (1.0f - tabTransitionFactor) * 6.0f : 0.0f;

            if (isTabSwitching) {
                ScissorHelper.pushScissor(context, layout.contentX + 1, layout.contentY + 1, layout.contentWidth - 2, layout.contentHeight - 2);
                context.getMatrices().pushMatrix();
                context.getMatrices().translate(0.0f, tabSlideOffsetY);
            }
            try {
                for (ActivityComponent component : this.components) {
                    if (component.isVisible()) {
                        component.setAlpha(alphaFactor);
                        component.render(context, effectiveMouseX, effectiveMouseY, delta);
                    }
                }
            } finally {
                if (isTabSwitching) {
                    context.getMatrices().popMatrix();
                    // Render soft translucent scrim / veil that smoothly dissolves as the new tab content emerges
                    float fadeOutFactor = 1.0f - tabTransitionFactor; // 1.0 down to 0.0
                    if (fadeOutFactor > 0.005f) {
                        int veilAlpha = (int) (255 * fadeOutFactor * (panelOpacity / 100.0) * alphaFactor);
                        int veilColor = (veilAlpha << 24) | (panelBg & 0x00FFFFFF);
                        ActivityGuiRenderer.fill(context, layout.contentX + 1, layout.contentY + 1, layout.contentWidth - 2, layout.contentHeight - 2, veilColor);
                    }
                    ScissorHelper.popScissor(context);
                }
            }

            // Step 7: Render active Overlay on top of all components (Z=7 Layer)
            this.overlayManager.render(context, effectiveMouseX, effectiveMouseY, delta);

            // Step 7.5: Render search popup on top of all widgets
            if (!isAnimating && !isMaximizeAnimating) {
                this.searchBar.renderPopup(context, mouseX, mouseY);
            }

            // Step 8: Render tooltips for control buttons and scroll container
            if (!isAnimating && !isMaximizeAnimating && this.controlButtons != null) {
                this.controlButtons.renderTooltips(context, this.textRenderer, mouseX, mouseY);
            }
            if (!isAnimating && !isMaximizeAnimating && this.currentScrollContainer != null) {
                this.currentScrollContainer.renderTooltips(context, this.textRenderer, mouseX, mouseY);
            }

            // Step 8.5: Bottom footer text in main menu
            int footerX = layout.windowX + layout.windowWidth / 2;
            int footerY = Math.min(layout.windowY + layout.windowHeight + 4, this.height - this.textRenderer.fontHeight - 2);
            int footerColor = ActivityColors.scaleAlpha(ActivityColors.TEXT_MUTED, alphaFactor);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("ТГ канал автора модов - @virionDEV"), footerX, footerY, footerColor);
        } finally {
            context.getMatrices().popMatrix();
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (this.closing || this.currentAnimProgress < 1.0f || this.isMaximizingTransition) {
            return true;
        }

        // Priority 1: Top-level Overlay layer intercepts clicks (click-through protection)
        if (this.overlayManager.mouseClicked(click, doubled)) {
            if (this.searchBar.isFocused()) {
                this.searchBar.setFocused(false);
                if (this.focusedComponent == this.searchBar) {
                    this.focusedComponent = null;
                }
            }
            return true;
        }

        // Priority 1.1: Search dropdown popup click (click-through protection)
        if (this.searchBar.isMouseOverPopup(click.x(), click.y())) {
            this.searchBar.mouseClicked(click, doubled);
            return true;
        }

        // Blur search bar focus and close popup if clicking outside the search bar
        boolean overSearch = this.searchBar.isMouseOver(click.x(), click.y()) || this.searchBar.isMouseOverPopup(click.x(), click.y());
        if (!overSearch && this.searchBar.isFocused()) {
            this.searchBar.setFocused(false);
            if (this.focusedComponent == this.searchBar) {
                this.focusedComponent = null;
            }
        }

        WindowLayout layout = computeLayout();

        // Priority 1.5: Top-right window controls (Recenter, Reload, Close)
        if (this.controlButtons != null && this.controlButtons.mouseClicked(click.x(), click.y(), click.button(), layout)) {
            return true;
        }

        // Priority 1.6: Window dragging by header
        boolean overControls = this.controlButtons != null && this.controlButtons.isMouseOver(click.x(), click.y(), layout);
        boolean wasMaximized = this.dragController.isMaximized();
        if (click.button() == 0 && this.dragController.startDrag(click.x(), click.y(), layout, overControls)) {
            if (wasMaximized && !this.dragController.isMaximized()) {
                WindowLayout restoredLayout = computeLayout();
                this.dragController.clampWindowPosition(this.width, this.height, restoredLayout.windowWidth, restoredLayout.windowHeight);
                this.clearComponents();
                this.overlayManager.clear();
                this.focusedComponent = null;
                this.initLayout(restoredLayout);
            }
            return true;
        }

        // Priority 2: Collapsible hierarchical sidebar navigation tree
        if (this.sidebarTree.mouseClicked(click.x(), click.y(), click.button(), layout, this)) {
            return true;
        }

        // Priority 2.5: Right-click on module cards opens context menu
        if (click.button() == 1) {
            ActivityTab activeTab = this.tabManager.getSelectedTab();
            if (activeTab != null) {
                for (java.util.Map.Entry<String, ActivityPanel> entry : activeTab.getModuleCards().entrySet()) {
                    ActivityPanel card = entry.getValue();
                    if (card.isVisible() && card.isMouseOver(click.x(), click.y())) {
                        this.openContextMenu(entry.getKey(), click.x(), click.y());
                        return true;
                    }
                }
            }
        }

        // Priority 3: Interactive UI components (top-most component first)
        ActivityComponent clickedComponent = null;
        for (int i = this.components.size() - 1; i >= 0; i--) {
            ActivityComponent comp = this.components.get(i);
            if (comp.isVisible() && comp.isEnabled() && comp.isMouseOver(click.x(), click.y())) {
                if (comp.mouseClicked(click, doubled)) {
                    clickedComponent = comp;
                    break;
                }
            }
        }

        // Focus coordination: blur previously focused component if click was outside of it
        ActivityComponent focusTarget = clickedComponent;
        if (clickedComponent instanceof ScrollContainer sc && sc.getFocusedChild() != null) {
            focusTarget = sc.getFocusedChild();
        }

        if (this.focusedComponent != null && this.focusedComponent != focusTarget) {
            this.focusedComponent.setFocused(false);
            this.focusedComponent = null;
            for (ActivityComponent comp : this.components) {
                if (comp instanceof ScrollContainer sc) {
                    if (focusTarget == null || !sc.hasChild(focusTarget)) {
                        sc.clearFocus();
                    }
                }
            }
        }

        if (focusTarget != null && focusTarget.isFocused()) {
            this.focusedComponent = focusTarget;
        }

        if (clickedComponent != null) {
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (this.closing || this.currentAnimProgress < 1.0f || this.isMaximizingTransition) return true;

        if (this.dragController.isDragging()) {
            this.dragController.stopDrag();
            return true;
        }

        WindowLayout layout = computeLayout();
        if (this.controlButtons != null && this.controlButtons.mouseReleased(click.x(), click.y(), click.button(), layout)) {
            return true;
        }

        boolean handled = this.overlayManager.mouseReleased(click);

        for (int i = this.components.size() - 1; i >= 0; i--) {
            ActivityComponent comp = this.components.get(i);
            if (comp.isVisible() && comp.mouseReleased(click)) {
                handled = true;
            }
        }

        return handled || super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (this.closing || this.currentAnimProgress < 1.0f || this.isMaximizingTransition) return true;

        WindowLayout layout = computeLayout();
        if (this.dragController.isDragging()) {
            int oldWinX = layout.windowX;
            int oldWinY = layout.windowY;
            this.dragController.onDrag(click.x(), click.y(), this.width, this.height, layout.windowWidth, layout.windowHeight);
            int newWinX = this.dragController.getWindowX();
            int newWinY = this.dragController.getWindowY();
            int dx = newWinX - oldWinX;
            int dy = newWinY - oldWinY;
            if (dx != 0 || dy != 0) {
                this.searchBar.setX(this.searchBar.getX() + dx);
                this.searchBar.setY(this.searchBar.getY() + dy);
                if (this.currentScrollContainer != null) {
                    this.currentScrollContainer.setX(this.currentScrollContainer.getX() + dx);
                    this.currentScrollContainer.setY(this.currentScrollContainer.getY() + dy);
                }
            }
            return true;
        }

        if (this.overlayManager.mouseDragged(click, deltaX, deltaY)) {
            return true;
        }

        for (int i = this.components.size() - 1; i >= 0; i--) {
            ActivityComponent comp = this.components.get(i);
            if (comp.isVisible() && comp.mouseDragged(click, deltaX, deltaY)) {
                return true;
            }
        }

        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.closing || this.currentAnimProgress < 1.0f || this.isMaximizingTransition) return true;
        if (this.overlayManager.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }

        WindowLayout layout = computeLayout();
        if (this.sidebarTree.mouseScrolled(mouseX, mouseY, verticalAmount, layout)) {
            return true;
        }

        for (int i = this.components.size() - 1; i >= 0; i--) {
            ActivityComponent comp = this.components.get(i);
            if (comp.isVisible() && comp.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    protected boolean isAnyTextFieldFocused() {
        if (this.searchBar.isFocused()) {
            return true;
        }
        if (this.focusedComponent instanceof ActivityTextField atf && atf.isFocused()) {
            return true;
        }
        for (ActivityComponent comp : this.components) {
            if (comp instanceof ActivityTextField atf && atf.isFocused()) {
                return true;
            }
            if (comp instanceof ScrollContainer sc) {
                if (sc.getFocusedChild() instanceof ActivityTextField atf && atf.isFocused()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isAnyKeybindListening() {
        if (this.focusedComponent instanceof ActivityKeybindButton akb && akb.isListening()) {
            return true;
        }
        for (ActivityComponent comp : this.components) {
            if (comp instanceof ActivityKeybindButton akb && akb.isListening()) {
                return true;
            }
            if (comp instanceof ScrollContainer sc) {
                if (sc.getFocusedChild() instanceof ActivityKeybindButton akb && akb.isListening()) {
                    return true;
                }
                for (ActivityComponent child : sc.getChildren()) {
                    if (child instanceof ActivityKeybindButton akb && akb.isListening()) {
                        return true;
                    }
                    if (child instanceof ActivityPanel ap) {
                        for (ActivityComponent panelChild : ap.getChildren()) {
                            if (panelChild instanceof ActivityKeybindButton akb && akb.isListening()) {
                                 return true;
                            }
                        }
                    }
                }
            } else if (comp instanceof ActivityPanel ap) {
                for (ActivityComponent child : ap.getChildren()) {
                    if (child instanceof ActivityKeybindButton akb && akb.isListening()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (this.closing) {
            if (input.isEscape()) {
                finishClose();
                return true;
            }
            KeyBinding openGuiKey = ActivityClient.getOpenGuiKey();
            if (openGuiKey != null && openGuiKey.matchesKey(input)) {
                finishClose();
                return true;
            }
            return true;
        }

        if (this.currentAnimProgress < 1.0f) {
            if (input.isEscape()) {
                this.close();
                return true;
            }
            KeyBinding openGuiKey = ActivityClient.getOpenGuiKey();
            if (openGuiKey != null && openGuiKey.matchesKey(input)) {
                this.close();
                return true;
            }
            return true;
        }

        // Hotkey: Ctrl+F immediately focuses search bar
        if (input.hasCtrl() && input.key() == GLFW.GLFW_KEY_F) {
            this.searchBar.setFocused(true);
            if (this.focusedComponent != null && this.focusedComponent != this.searchBar) {
                this.focusedComponent.setFocused(false);
            }
            this.focusedComponent = this.searchBar;
            return true;
        }

        // Priority 1: Overlays intercept keys (e.g. Esc closes dropdown without closing screen)
        if (this.overlayManager.keyPressed(input)) {
            return true;
        }

        // Stale focus check on Escape blur (AUD-04)
        if (this.focusedComponent != null && !this.focusedComponent.isFocused()) {
            this.focusedComponent = null;
            clearAllContainerFocus();
        }

        // Priority 2: Focused component gets first opportunity to handle key input
        if (this.focusedComponent != null && this.focusedComponent.isVisible() && this.focusedComponent.isEnabled()) {
            if (this.focusedComponent.keyPressed(input)) {
                if (!this.focusedComponent.isFocused()) {
                    this.focusedComponent = null;
                    clearAllContainerFocus();
                }
                return true;
            }
        }
        if (this.focusedComponent != null && !this.focusedComponent.isFocused()) {
            this.focusedComponent = null;
            clearAllContainerFocus();
        }

        // Priority 3: Interactive components
        for (int i = this.components.size() - 1; i >= 0; i--) {
            ActivityComponent comp = this.components.get(i);
            if (comp != this.focusedComponent && comp.isVisible() && comp.isEnabled() && comp.keyPressed(input)) {
                return true;
            }
        }

        // Priority 4: Hotkey tab switching (Ctrl + 1..4)
        if (input.hasCtrl()) {
            int key = input.key();
            if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_4) {
                int targetTab = key - GLFW.GLFW_KEY_1;
                if (targetTab < this.tabManager.getTabCount()) {
                    this.setSelectedTab(targetTab);
                    ActivityGuiRenderer.playClickSound();
                    return true;
                }
            }
        }

        // Priority 5: Keyboard Tab navigation
        if (input.isTab() && !this.components.isEmpty()) {
            this.cycleFocus(!input.hasShift());
            return true;
        }

        // Priority 6: Hotkey screen close ('O' / registered keybind when no text field is focused and no keybind listening) (AUD-10)
        if (!isAnyTextFieldFocused() && !isAnyKeybindListening()) {
            KeyBinding openGuiKey = ActivityClient.getOpenGuiKey();
            if (openGuiKey != null && openGuiKey.matchesKey(input)) {
                this.close();
                return true;
            }
        }

        // Priority 7: Screen escape closing
        if (input.isEscape() && this.shouldCloseOnEsc() && !isAnyKeybindListening()) {
            this.close();
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean keyReleased(KeyInput input) {
        if (this.closing || this.currentAnimProgress < 1.0f) return true;

        if (this.overlayManager.keyReleased(input)) {
            return true;
        }

        for (int i = this.components.size() - 1; i >= 0; i--) {
            ActivityComponent comp = this.components.get(i);
            if (comp.isVisible() && comp.keyReleased(input)) {
                return true;
            }
        }

        return super.keyReleased(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (this.closing || this.currentAnimProgress < 1.0f) return true;

        if (this.overlayManager.charTyped(input)) {
            return true;
        }

        if (this.focusedComponent != null && !this.focusedComponent.isFocused()) {
            this.focusedComponent = null;
            clearAllContainerFocus();
        }

        if (this.focusedComponent != null && this.focusedComponent.isVisible() && this.focusedComponent.isEnabled()) {
            if (this.focusedComponent.charTyped(input)) {
                if (!this.focusedComponent.isFocused()) {
                    this.focusedComponent = null;
                    clearAllContainerFocus();
                }
                return true;
            }
        }
        if (this.focusedComponent != null && !this.focusedComponent.isFocused()) {
            this.focusedComponent = null;
            clearAllContainerFocus();
        }

        for (int i = this.components.size() - 1; i >= 0; i--) {
            ActivityComponent comp = this.components.get(i);
            if (comp != this.focusedComponent && comp.isVisible() && comp.isEnabled() && comp.charTyped(input)) {
                return true;
            }
        }

        return super.charTyped(input);
    }

    @Override
    public void tick() {
        super.tick();
        for (ActivityComponent comp : this.components) {
            comp.tick();
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void close() {
        if (!this.closing) {
            activity.client.gui.sound.SoundManager.playClose();
            ActivityTab curTab = this.tabManager.getSelectedTab();
            String curTabId = curTab != null ? curTab.getId() : null;
            String curModId = this.sidebarTree != null ? this.sidebarTree.getSelectedModuleId() : null;
            recordSession(curTabId, curModId);
        }
        activity.client.config.ActivityConfig config = activity.client.config.ActivityConfigManager.getConfig();
        boolean globalAnim = config == null || config.animationsEnabled;
        boolean spatialAnim = globalAnim && config.spatialOpenAnimation;
        if (spatialAnim && !this.closing) {
            this.closing = true;
            this.closeStartProgress = this.currentAnimProgress;
            this.closeTimer = 0.0f;
            this.overlayManager.clear();
            return;
        }
        finishClose();
    }

    public float getCurrentAnimProgress() {
        return this.currentAnimProgress;
    }

    public boolean isClosing() {
        return this.closing;
    }

    public boolean isMaximizingTransition() {
        return this.isMaximizingTransition;
    }

    public void finishClose() {
        ActivityTab curTab = this.tabManager.getSelectedTab();
        String curTabId = curTab != null ? curTab.getId() : null;
        String curModId = this.sidebarTree != null ? this.sidebarTree.getSelectedModuleId() : null;
        recordSession(curTabId, curModId);

        this.searchBar.clear();
        this.searchBar.setFocused(false);
        this.activeSearchQuery = "";
        activity.client.gui.font.FontManager.removeListener(this.fontChangeListener);
        if (ActivityConfigManager.isDirty()) {
            ActivityConfigManager.save();
        }
        this.overlayManager.clear();
        this.focusedComponent = null;
        clearAllContainerFocus();
        super.close();
    }
}

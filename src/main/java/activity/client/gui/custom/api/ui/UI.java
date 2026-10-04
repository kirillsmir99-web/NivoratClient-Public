package activity.client.gui.custom.api.ui;
import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import activity.client.gui.custom.IMinecraft;
import activity.client.gui.custom.api.drags.Position;
import activity.client.gui.custom.api.modules.Category;
import activity.client.gui.custom.api.modules.Module;
import activity.client.gui.custom.api.modules.ModuleManager;
import activity.client.gui.custom.api.modules.impl.Interface.ClickGui;
import activity.client.gui.custom.VisualMaterial;
import activity.client.gui.custom.api.modules.restrict.Server;
import activity.client.gui.custom.api.modules.restrict.ServerRestrictions;
import activity.client.gui.custom.api.ui.BaseScreen;
import activity.client.gui.custom.api.ui.BindPopup;
import activity.client.gui.custom.api.ui.SettingsPopup;
import activity.client.gui.custom.api.ui.inspector.InspectorRenderer;
import activity.client.gui.custom.api.ui.pin.PinManager;
import activity.client.gui.custom.api.ui.module.ModuleListRenderer;
import activity.client.gui.custom.api.ui.module.SearchField;
import activity.client.gui.custom.api.ui.settings.RenderHelper;
import activity.client.gui.custom.api.ui.settings.Setting;
import activity.client.gui.custom.api.ui.settings.impl.BindSetting;
import activity.client.gui.custom.api.ui.settings.impl.TextSetting;
import activity.client.gui.custom.api.ui.theme.AccentGradient;
import activity.client.gui.custom.api.ui.theme.ClientAccent;
import activity.client.gui.custom.api.ui.theme.ThemeManager;
import activity.client.gui.custom.api.ui.theme.ThemesRenderer;
import activity.client.gui.custom.api.ui.theme.ThemeEditorRenderer;
import activity.client.gui.custom.api.ui.window.GuiShatterAnimation;
import activity.client.gui.custom.api.ui.window.WorldGuiCloseAnimation;
import activity.client.gui.custom.mixin.accessor.GuiGraphicsExtractorAccessor;
import activity.client.gui.custom.utils.animations.Decelerate;
import activity.client.gui.custom.utils.animations.Direction;
import activity.client.gui.custom.utils.animations.GuiMotionAnimation;
import activity.client.gui.custom.utils.key.KeyBind;
import activity.client.gui.custom.utils.render.fonts.Fonts;
import activity.client.gui.custom.utils.render.fonts.NvIcons;
import activity.client.gui.custom.utils.render.others.RectUtil;
import activity.client.gui.custom.utils.render.post.guilayerblur.GuiCapture;
import activity.client.gui.custom.utils.render.post.guilayerblur.GuiCapture.Source;
import activity.client.gui.custom.utils.render.post.guilayerblur.GuiLayerBlurRenderer;
import activity.client.gui.custom.utils.render.post.guimotionblur.GuiMotionBlurRenderer;
import activity.client.gui.custom.utils.render.render2d.Render2D;
import activity.client.gui.custom.utils.render.render2d.Render2DCoordinateSpace;
import activity.client.gui.custom.utils.render.render2d.blur.BlurFramebuffer;
import activity.client.gui.custom.utils.render.render2d.gif.GifRenderer;
import activity.client.gui.custom.utils.render.render2d.glow.BuiltGlow;
import activity.client.gui.custom.utils.sounds.Sounds;

public class UI
extends BaseScreen
implements GuiCapture.Source {
    public static final UI INSTANCE = new UI();
    public static final float PANEL_W = 430.0f;
    public static final float PANEL_H = 290.0f;
    public static final float DEFAULT_SIDEBAR_W = 90.0f;
    public static final float MIN_SIDEBAR_W = 38.0f;
    public static final float MAX_SIDEBAR_W = 145.0f;
    public static final float SIDEBAR_W = DEFAULT_SIDEBAR_W;

    public static float customSidebarW = DEFAULT_SIDEBAR_W;
    public static boolean isDraggingSidebar = false;
    private static float sidebarDragGrabX = 0.0f;
    private float splitterHoverT = 0.0f;

    public static float sidebarW() {
        return Math.max(MIN_SIDEBAR_W, Math.min(MAX_SIDEBAR_W, customSidebarW));
    }

    public static float contentXOff() {
        return sidebarW() + 8.0f;
    }

    public static float contentInset() {
        return contentXOff() + 5.0f;
    }

    private static float customPanelX = -1.0f;
    private static float customPanelY = -1.0f;
    private static boolean isDraggingPanel = false;
    private static float dragGrabX = 0.0f;
    private static float dragGrabY = 0.0f;

    public static float panelW() {
        float dock = 0.0f;
        if (INSTANCE != null) {
            if (INSTANCE.inspector != null && INSTANCE.inspector.isOpen()) {
                dock = INSTANCE.inspector.dockWidth();
            } else if (INSTANCE.themesRenderer != null && INSTANCE.themesRenderer.getEditor() != null && INSTANCE.themesRenderer.getEditor().isOpen()) {
                dock = INSTANCE.themesRenderer.getEditor().dockWidth();
            } else if (INSTANCE.presetRenderer != null && INSTANCE.presetRenderer.isDrawerOpen()) {
                dock = INSTANCE.presetRenderer.dockWidth();
            }
        }
        return PANEL_W + dock;
    }

    public static float panelX() {
        if (activity.client.gui.custom.CollectionDrawer.isOpen()) return Math.max(activity.client.gui.custom.CollectionDrawer.width() + 8, (Position.screenWidth() - PANEL_W + activity.client.gui.custom.CollectionDrawer.width()) / 2);
        float curW = panelW();
        float defaultX = Math.max(8.0f, Position.screenWidth() / 2.0f - curW / 2.0f);
        if (customPanelX < 0.0f) {
            return defaultX;
        }
        float maxX = Math.max(8.0f, Position.screenWidth() - curW - 8.0f);
        return Math.max(8.0f, Math.min(maxX, customPanelX));
    }

    public static float panelY() {
        float baseY;
        float defaultY = Math.max(8.0f, Position.screenHeight() / 2.0f - PANEL_H / 2.0f);
        if (customPanelY < 0.0f) {
            baseY = defaultY;
        } else {
            float maxY = Math.max(8.0f, Position.screenHeight() - PANEL_H - 8.0f);
            baseY = Math.max(8.0f, Math.min(maxY, customPanelY));
        }

        return baseY;
    }

    public static final float CONTENT_X_OFF = DEFAULT_SIDEBAR_W + 8.0f;
    public static final float CONTENT_INSET = CONTENT_X_OFF + 5.0f;
    public static final float CONTENT_HEIGHT = 280.0f;
    public static final float CONTENT_Y_OFFSET = 5.0f;
    public static final float HEADER_H = 22.0f;
    public static final float HEADER_OFFSET = 26.0f;
    private static final Category[] MAIN_CATEGORIES = new Category[]{Category.VISUALS, Category.SWORD, Category.NPOT, Category.SMP, Category.BEAST, Category.AXE, Category.MACE, Category.CART, Category.CRYSTAL, Category.UHC, Category.DPOT};
    private static final Category[] SYSTEM_CATEGORIES = new Category[]{Category.THEMES,Category.PRESETS,Category.DISPLAY};
    private static final float CAT_COL_TOP = 31.0f;
    private static final float CAT_HEADER_H = 20.0f;
    private static final float CAT_SUB_GAP = 0.5f;
    private static final float CAT_SUB_ROW_H = 13.5f;
    private static final float CAT_OTHERS_GAP = 8.0f;
    private static final float CAT_OTHER_ROW_H = 17.0f;
    private Category targetCategory = Category.VISUALS;
    private Category contentCategory = Category.VISUALS;
    private String oldSubText = Category.VISUALS.getDisplayName();
    private String newSubText = Category.VISUALS.getDisplayName();
    private final Map<Category, Decelerate> categoryAnims = new EnumMap<Category, Decelerate>(Category.class);
    private final Decelerate subTextAnim = UI.createAnim(300);
    private final Decelerate modulesHeaderAnim = UI.createAnim(200);
    private boolean subTextAnimDone = true;
    private static final float CATEGORY_FADE_SEC = 0.15f;
    private float categoryT = 1.0f;
    private float themesRowT = 1.0f;
    private final ModuleListRenderer moduleList = new ModuleListRenderer();
    private final InspectorRenderer inspector = new InspectorRenderer();
    private final SearchField search = new SearchField();
    private final BindPopup bindPopup = new BindPopup();
    private final SettingsPopup settingsPopup = new SettingsPopup();
    private final ThemesRenderer themesRenderer = new ThemesRenderer();
    private final activity.client.gui.custom.PresetRenderer presetRenderer = new activity.client.gui.custom.PresetRenderer();
    public activity.client.gui.custom.PresetRenderer getPresetRenderer() { return presetRenderer; }
    private boolean presetDropdownOpen = false;
    private final Decelerate presetDropdownAnim = UI.createAnim(180);
    private List<activity.client.config.preset.LocalPresets.Entry> cachedPresetEntries = List.of();
    private String selectedPresetName = null;
    private int presetScrollOffset = 0;
    private float lastPresetBtnX, lastPresetBtnY, lastPresetBtnW, lastPresetBtnH;
    private float lastPresetDropX, lastPresetDropY, lastPresetDropW, lastPresetDropH;

    private void reloadPresetDropdown() {
        try {
            this.cachedPresetEntries = activity.client.config.preset.LocalPresets.list();
        } catch (Exception e) {
            this.cachedPresetEntries = List.of();
        }
        this.presetScrollOffset = 0;
    }

    public void applyPreset(activity.client.config.preset.LocalPresets.Entry entry) {
        try {
            activity.client.config.preset.LocalPresets.Preview preview = activity.client.config.preset.LocalPresets.read(entry.path());
            activity.client.config.preset.LocalPresets.apply(preview);
            this.selectedPresetName = entry.name();
            activity.client.gui.custom.utils.sounds.Sounds.play("buttonclick");
        } catch (Exception e) {
            activity.client.gui.custom.utils.sounds.Sounds.play("command_error");
        }
        this.presetDropdownOpen = false;
        this.presetDropdownAnim.setDirection(Direction.BACKWARDS);
    }

    public String getSelectedPresetName() {
        return this.selectedPresetName;
    }

    public void setSelectedPresetName(String name) {
        this.selectedPresetName = name;
    }


    public activity.client.gui.custom.api.ui.theme.ThemesRenderer getThemesRenderer(){return themesRenderer;}
    public void setSearchText(String text){search.setText(text);}
    public String getSearchText(){return search!=null?search.getText():"";}
    public boolean hasSearchText(){return search!=null&&search.hasText();}
    public void navigateToModule(String id){Module m=ModuleManager.get().findByName(id);if(m!=null){selectCategory(Category.VISUALS);openModuleSettings(m);}}
    public void setSelectedTab(int i){selectCategoryFromWorkspace(i==activity.client.gui.tab.ThemesTab.TAB_INDEX?Category.THEMES:i==4?Category.DISPLAY:Category.VISUALS);}
    public void openModuleInspector(String id){navigateToModule(id);}
    public InspectorRenderer getInspector() {
        return this.inspector;
    }
    private final GuiMotionAnimation screenAnim = new GuiMotionAnimation();
    private long lastNs = System.nanoTime();
    private static Screen pendingAfterClose;
    private static boolean motionBlurPending;
    private static float motionBlurOpacity;
    private static float motionBlurRadius;
    private static float motionBlurScale;
    private static float motionBlurOriginX;
    private static float motionBlurOriginY;
    private static int motionBlurX;
    private static int motionBlurY;
    private static int motionBlurW;
    private static int motionBlurH;
    private static int motionBlurSrcX;
    private static int motionBlurSrcY;
    private static int motionBlurSrcW;
    private static int motionBlurSrcH;
    private static final float[] motionBlurMask;
    private static final float CARD_BLUR_MAX_RADIUS = 22.0f;
    private static boolean cardStratumMarked;
    private static boolean cardBlurPending;
    private static final float[] cardBlurMask;
    private static int cardBlurMaskCount;
    private static boolean cardCaptureStaged;
    private static boolean cardBlurCaptured;
    private static boolean panelSplitMarked;
    private static boolean vanillaBlurRequested;
    private static boolean popupStratumMarked;
    private static final float POPUP_BLUR_GUI_RADIUS = 9.0f;
    private static final float[] popupBlurMask;
    private static int popupBlurMaskCount;
    private static boolean popupLayerBlurWanted;
    private static boolean popupBlurWanted;
    private static boolean popupBlurStaged;
    private static boolean popupBlurCaptured;
    private static boolean popupBlurPending;
    private static float popupBlurRadius;
    private static int popupBlurX;
    private static int popupBlurY;
    private static int popupBlurW;
    private static int popupBlurH;
    private static float popupBlurOriginX;
    private static float popupBlurOriginY;
    private static float guiCaptureScale;
    private static float guiCaptureBlurMainPx;
    private final activity.client.capitulation.HoldConfirmation capitulationHold = new activity.client.capitulation.HoldConfirmation(2_000_000_000L);
    private Screen integrationParent;
    private String integrationGroup;

    public static float moduleViewportHeight(Category category) {
        return CONTENT_HEIGHT - HEADER_OFFSET - (category == Category.DISPLAY ? 44.0f : 0.0f);
    }

    public static Screen integrationScreen(Screen parent, String group) {
        INSTANCE.integrationParent = parent;
        INSTANCE.integrationGroup = group;
        return INSTANCE;
    }

    @Override
    public void tick() {
        super.tick();
        MinecraftClient client = MinecraftClient.getInstance();
        boolean eligible = client.currentScreen == this && client.isWindowFocused()
                && targetCategory == Category.DISPLAY && contentCategory == Category.DISPLAY
                && screenAnim.canInteract() && !isDraggingPanel && !isDraggingSidebar
                && !inspector.isOpen() && !bindPopup.isOpen()
                && !settingsPopup.isVisible()
                && !activity.client.gui.custom.NativeBindAssignment.isOpen()
                && panicButtonContains(Position.mouseX(), Position.mouseY());
        if (capitulationHold.update(System.nanoTime(), eligible)) {
            activity.client.gui.sound.SoundManager.playSuccess();
            activity.client.capitulation.CapitulationManager.capitulate(client);
        }
    }

    private boolean panicButtonContains(float x, float y) {
        float left = panelX() + contentXOff() + 7;
        float top = panelY() + CONTENT_Y_OFFSET + CONTENT_HEIGHT - 29;
        return x >= left && x <= left + PANEL_W - contentInset() - 14 && y >= top && y <= top + 21;
    }

    private void renderCapitulation(float x, float y, float width, float alpha) {
        boolean ru = activity.client.i18n.LocalizationService.isRussianPreferred();
        float top = y + CONTENT_Y_OFFSET + CONTENT_HEIGHT - 29;
        float left = x + contentXOff() + 7;
        float buttonWidth = width - contentInset() - 14;
        Render2D.rect(left, top, buttonWidth, 21, 6, UI.color(92, 28, 36, 145, alpha));
        float progress = capitulationHold.progress(System.nanoTime());
        if (progress > 0) {
            float eased = progress * progress * (3f - 2f * progress);
            Render2D.rect(left + 3, top + 17, (buttonWidth - 6) * eased, 2, 1, UI.color(255, 95, 113, 230, alpha));
            float pulse = .5f + .5f * (float) Math.sin(progress * Math.PI * 4);
            Render2D.outline(left, top, buttonWidth, 21, 6, .6f, UI.color(255, 95, 113, Math.round(90 + pulse * 100), alpha));
        }
        String label = progress > 0 ? (ru ? "Остановка… " : "Stopping… ") + Math.round(progress * 100) + "%" : ru ? "Капитуляция · удерживай 2 секунды" : "Deactivate · hold for 2 seconds";
        Fonts.MONTSERRAT_MEDIUM.draw(RenderHelper.fitText(Fonts.MONTSERRAT_MEDIUM, label, buttonWidth - 16, 6), left + 8, top + 6, 6, UI.color(255, 225, 230, 245, alpha));
        String hint = ru ? "Остановить модули и очередь действий до перезапуска" : "Stop modules and pending actions until restart";
        Fonts.MONTSERRAT_MEDIUM.draw(RenderHelper.fitText(Fonts.MONTSERRAT_MEDIUM, hint, buttonWidth, 5), left, top - 10, 5, UI.color(190, 195, 210, 195, alpha));
    }
    private float parallaxX;
    private float parallaxY;
    private float lastCameraYaw = Float.NaN;
    private float lastCameraPitch;
    private static final String RU_LAYOUT = "\u0439\u0446\u0443\u043a\u0435\u043d\u0433\u0448\u0449\u0437\u0445\u044a\u0444\u044b\u0432\u0430\u043f\u0440\u043e\u043b\u0434\u0436\u044d\u044f\u0447\u0441\u043c\u0438\u0442\u044c\u0431\u044e\u0451";
    private static final String EN_LAYOUT = "qwertyuiop[]asdfghjkl;'zxcvbnm,.`";

    private UI() {
        super(Text.literal("NC"));
    }

    static {
        motionBlurMask = new float[12];
        cardBlurMask = new float[396];
        popupBlurMask = new float[18];
        guiCaptureScale = 1.0f;
    }

    public static boolean isOpen() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        return minecraftClient != null && minecraftClient.currentScreen == INSTANCE;
    }

    public static boolean isPanelActive() {
        if (isOpen()) {
            return true;
        }
        return INSTANCE != null && INSTANCE.screenAnim.isClosing();
    }

    private static int color(int n, int n2, int n3, int n4, float f) {
        int n5 = Math.max(0, Math.min(255, Math.round((float)n4 * f)));
        if (n5 <= 0) {
            return 0;
        }
        return n5 << 24 | (n & 255) << 16 | (n2 & 255) << 8 | n3 & 255;
    }

    public void warmupRender() {
        this.moduleList.warmup();
    }

    public void prepareCollectionDrawer() {
        this.inspector.close(); this.settingsPopup.close(); this.bindPopup.close(); this.releaseAllDrags(); capitulationHold.cancel();
    }

    public static void closeInto(Screen screen) {
        pendingAfterClose = screen;
        UI uI = INSTANCE;
        WorldGuiCloseAnimation.cancel();
        GuiShatterAnimation.cancel();
        uI.screenAnim.snapClosed();
        uI.releaseAllDrags();
        uI.settingsPopup.close();
        uI.inspector.close();
        uI.search.collapse();
        if (MinecraftClient.getInstance().currentScreen == uI) {
            MinecraftClient.getInstance().setScreen(screen);
        }
    }

    private static Decelerate createAnim(int n) {
        Decelerate decelerate = (Decelerate)new Decelerate().setMs(n).setValue(1.0);
        decelerate.setDirection(Direction.BACKWARDS);
        decelerate.counter.setTime(System.currentTimeMillis() - 10000L);
        return decelerate;
    }

    private static char iconChar(Category category) {
        return activity.client.gui.custom.utils.render.fonts.NvIcons.charForCategory(category);
    }

    private static boolean ctrlHeld() {
        long l = IMinecraft.mc.getWindow().getHandle();
        return GLFW.glfwGetKey((long)l, (int)341) == 1 || GLFW.glfwGetKey((long)l, (int)345) == 1;
    }



    public void close() {
        capitulationHold.cancel();
        WorldGuiCloseAnimation.cancel();
        GuiShatterAnimation.cancel();
        this.screenAnim.snapClosed();
        this.releaseAllDrags();
        this.settingsPopup.close();
        this.inspector.close();
        if (this.presetRenderer.isDrawerOpen()) {
            this.presetRenderer.closeDrawer();
        }
        this.search.collapse();
        this.presetDropdownOpen = false;
        this.presetDropdownAnim.setDirection(Direction.BACKWARDS);
        Sounds.play("gui_close");
        if (MinecraftClient.getInstance().currentScreen == this) {
            Screen parent = integrationParent;
            integrationParent = null;
            MinecraftClient.getInstance().setScreen(parent);
        }
    }

    private static boolean isMenuKeyMatch(int pressedKey, int boundKey) {
        if (pressedKey == boundKey) return true;
        if ((boundKey == 344 || boundKey == 340) && (pressedKey == 344 || pressedKey == 340)) return true;
        if ((boundKey == 345 || boundKey == 341) && (pressedKey == 345 || pressedKey == 341)) return true;
        if ((boundKey == 346 || boundKey == 342) && (pressedKey == 346 || pressedKey == 342)) return true;
        return false;
    }

    public boolean keyPressed(KeyInput input) {
        capitulationHold.cancel();
        if (this.presetDropdownOpen && input.key() == 256) {
            this.presetDropdownOpen = false;
            this.presetDropdownAnim.setDirection(Direction.BACKWARDS);
            activity.client.gui.custom.utils.sounds.Sounds.play("settings_close");
            return true;
        }
        if (activity.client.gui.custom.CollectionDrawer.isOpen()) { activity.client.gui.custom.CollectionDrawer.screen().keyPressed(input); return true; }
        if (activity.client.gui.custom.NativeBindAssignment.keyPressed(input.key())) return true;
        if (activity.client.gui.custom.AutoCartCalibrationDrawer.keyPressed(input.key())) return true;
        int n;
        Setting setting;
        if (this.screenAnim.isClosing()) {
            int n2;
            ClickGui clickGui = ModuleManager.get().get(ClickGui.class);
            int n3 = n2 = clickGui != null ? clickGui.getBind().getCode() : 344;
            if (isMenuKeyMatch(input.key(), n2)) {
                pendingAfterClose = null;
                WorldGuiCloseAnimation.reverse();
                GuiShatterAnimation.gather(WorldGuiCloseAnimation.isReversing() ? WorldGuiCloseAnimation.remainingNanos() : 0L);
                this.screenAnim.resumeOpening();
                Sounds.play("gui_open");
            }
            return true;
        }
        if (this.themesRenderer.getEditor().isOpen()) {
            if (this.themesRenderer.getEditor().keyPressed(input.key())) {
                return true;
            }
        }
        if (this.presetRenderer.isDrawerOpen()) {
            if (this.presetRenderer.drawerKeyPressed(input)) {
                return true;
            }
        }
        if (this.bindPopup.isOpen()) {
            if (this.bindPopup.keyPressed(input.key())) {
                return true;
            }
            if (input.key() == 256) {
                this.bindPopup.close();
            }
            return true;
        }
        if (this.contentCategory == Category.PRESETS && this.presetRenderer.keyPressed(input)) return true;
        if (this.contentCategory == Category.THEMES && this.themesRenderer.keyPressed(input)) {
            return true;
        }
        if (this.inspector.isOpen() && this.inspector.keyPressed(input)) {
            return true;
        }
        if (this.moduleList.keyPressed(input)) {
            return true;
        }
        if (this.search.isTyping() && this.search.keyPressed(input)) {
            return true;
        }
        ClickGui clickGui = ModuleManager.get().get(ClickGui.class);
        int n5 = n = clickGui != null ? clickGui.getBind().getCode() : 344;
        if (input.key() == 256) {
            if (this.inspector.isOpen()) {
                this.inspector.close();
                return true;
            }
            this.close();
            return true;
        }
        if (isMenuKeyMatch(input.key(), n)) {
            this.close();
            return true;
        }
        return super.keyPressed(input);
    }

    public void removed() {
        activity.client.gui.custom.CollectionDrawer.close();
        capitulationHold.cancel();
        activity.client.gui.custom.NativeBindAssignment.cancel();
        activity.client.gui.custom.VisualSettingsStore.save(); activity.client.config.ActivityConfigManager.save();
        if (!this.screenAnim.isClosing()) {
            this.screenAnim.snapClosed();
            GuiShatterAnimation.cancel();
            this.settingsPopup.close();
            this.inspector.close();
            this.search.collapse();
        }
        ThemeManager.clearLiveOverride();
        this.releaseAllDrags();
        super.removed();
    }

    protected void init() {
        activity.client.gui.custom.CustomRender.init(); activity.client.gui.custom.VisualSettingsStore.load();
        pendingAfterClose = null;
        BaseScreen.dropClosingOverlay();
        GuiCapture.bind(this);
        if (!this.search.hasText()) {
            this.search.collapse();
        }
        if (this.targetCategory == null) {
            this.targetCategory = Category.VISUALS;
        }
        if (this.contentCategory == null) {
            this.contentCategory = Category.VISUALS;
        }
        Category start = this.resolveStartCategory();
        if (start == null) start = Category.VISUALS;
        this.selectCategory(start);
        if (this.screenAnim.isClosing()) {
            WorldGuiCloseAnimation.reverse();
            GuiShatterAnimation.gather(WorldGuiCloseAnimation.isReversing() ? WorldGuiCloseAnimation.remainingNanos() : 0L);
            this.screenAnim.resumeOpening();
        } else {
            WorldGuiCloseAnimation.cancel();
            GuiShatterAnimation.cancel();
            this.screenAnim.startOpening();
        }
        this.lastNs = System.nanoTime();
        if (integrationGroup != null) {
            String group = integrationGroup;
            integrationGroup = null;
            Module module = switch (group) {
                case "appearance" -> VisualMaterial.getInstance();
                case "menu" -> ModuleManager.get().get(ClickGui.class);
                case "sounds" -> ModuleManager.get().get(activity.client.gui.custom.api.modules.impl.Utils.ClientSounds.class);
                default -> null;
            };
            if (module != null) openModuleSettings(module);
        }
    }

    public boolean mouseClicked(Click click, boolean doubled) {
        if (activity.client.gui.custom.AutoCartCalibrationDrawer.mouseClicked(click)) return true;
        if (activity.client.gui.custom.api.drags.DragSystem.get().mouseClicked(click)) return true;
        if (activity.client.gui.custom.CollectionDrawer.isOpen()) return activity.client.gui.custom.CollectionDrawer.screen().mouseClicked(click, doubled);
        if (activity.client.gui.custom.NativeBindAssignment.click(Position.mouseX(), Position.mouseY(), click.button())) return true;
        if (this.inspector.captureMouse(click.button())) return true;
        if (this.bindPopup.mouseBind(click.button())) return true;
        if(click.button()==2&&isModuleView()){Module m=moduleAtCursor();if(m!=null){bindPopup.open(m,Position.mouseX(),Position.mouseY());return true;}}
        ClickGui clickGui = ModuleManager.get().get(ClickGui.class);
        if (this.screenAnim.isClosing()) {
            if (clickGui != null && clickGui.getBind().getType() == activity.client.gui.custom.utils.key.InputType.MOUSE
                && clickGui.getBind().matchesMouseButton(click.button())) {
                pendingAfterClose = null;
                WorldGuiCloseAnimation.reverse();
                GuiShatterAnimation.gather(WorldGuiCloseAnimation.isReversing() ? WorldGuiCloseAnimation.remainingNanos() : 0L);
                this.screenAnim.resumeOpening();
                Sounds.play("gui_open");
            }
            return true;
        }
        if (!this.screenAnim.canInteract()) {
            return true;
        }
        if (clickGui != null && click.button() != 0 && clickGui.getBind().getType() == activity.client.gui.custom.utils.key.InputType.MOUSE
            && clickGui.getBind().matchesMouseButton(click.button())) {
            this.close();
            return true;
        }
        if (this.presetDropdownOpen && click.button() == 0) {
            float mx = Position.mouseX();
            float my = Position.mouseY();
            if (mx >= this.lastPresetDropX && mx <= this.lastPresetDropX + this.lastPresetDropW
                && my >= this.lastPresetDropY && my <= this.lastPresetDropY + this.lastPresetDropH) {
                float itemH = 15.0f;
                float startY = this.lastPresetDropY + 4.0f;
                int totalEntries = this.cachedPresetEntries.size();
                int visibleCount = totalEntries == 0 ? 0 : Math.min(totalEntries, 6);
                int startIdx = Math.max(0, Math.min(this.presetScrollOffset, Math.max(0, totalEntries - visibleCount)));
                for (int i = 0; i < visibleCount; i++) {
                    float rowY = startY + i * itemH;
                    if (my >= rowY && my < rowY + itemH) {
                        int entryIdx = startIdx + i;
                        if (entryIdx < totalEntries) {
                            applyPreset(this.cachedPresetEntries.get(entryIdx));
                        }
                        return true;
                    }
                }
                float footerY = startY + (totalEntries == 0 ? itemH : visibleCount * itemH) + 1.0f;
                if (my >= footerY && my <= footerY + 16.0f) {
                    this.presetDropdownOpen = false;
                    this.presetDropdownAnim.setDirection(Direction.BACKWARDS);
                    selectCategoryFromWorkspace(Category.PRESETS);
                    activity.client.gui.custom.utils.sounds.Sounds.play("select_category");
                    return true;
                }
                return true;
            }
            if (mx >= this.lastPresetBtnX && mx <= this.lastPresetBtnX + this.lastPresetBtnW
                && my >= this.lastPresetBtnY && my <= this.lastPresetBtnY + this.lastPresetBtnH) {
                this.presetDropdownOpen = false;
                this.presetDropdownAnim.setDirection(Direction.BACKWARDS);
                activity.client.gui.custom.utils.sounds.Sounds.play("settings_close");
                return true;
            }
            this.presetDropdownOpen = false;
            this.presetDropdownAnim.setDirection(Direction.BACKWARDS);
            activity.client.gui.custom.utils.sounds.Sounds.play("settings_close");
            return true;
        }
        if (!this.presetDropdownOpen && click.button() == 0) {
            float mx = Position.mouseX();
            float my = Position.mouseY();
            if (mx >= this.lastPresetBtnX && mx <= this.lastPresetBtnX + this.lastPresetBtnW
                && my >= this.lastPresetBtnY && my <= this.lastPresetBtnY + this.lastPresetBtnH) {
                reloadPresetDropdown();
                this.presetDropdownOpen = true;
                this.presetDropdownAnim.setDirection(Direction.FORWARDS);
                activity.client.gui.custom.utils.sounds.Sounds.play("settings_open");
                return true;
            }
        }
        if (this.themesRenderer.getEditor().isOpen()) {
            if (this.themesRenderer.getEditor().mouseClicked(Position.mouseX(), Position.mouseY(), click.button())) {
                return true;
            }
            Category category = this.categoryButtonAt(panelX(), panelY(), Position.mouseX(), Position.mouseY());
            if (category != null && click.button() == 0) {
                this.themesRenderer.getEditor().promptLeaveCategory(category);
                return true;
            }
            float f3 = panelX();
            float f4 = panelY();
            float f5 = Position.mouseX();
            float f6 = Position.mouseY();
            float thX = f3 + contentXOff();
            float thY = f4 + CONTENT_Y_OFFSET;
            float thW = PANEL_W - contentInset();
            float thH = CONTENT_HEIGHT;
            if (f5 >= thX && f5 <= thX + thW && f6 >= thY && f6 <= thY + thH) {
                if (this.contentCategory == Category.THEMES && click.button() == 0) {
                    if (this.themesRenderer.click(f3, f4, PANEL_W, f5, f6)) {
                        return true;
                    }
                }
            }
            return true;
        }
        float f = PANEL_W;
        float f2 = PANEL_H;
        float f3 = panelX();
        float f4 = panelY();
        float f5 = Position.mouseX();
        float f6 = Position.mouseY();

        boolean isShift = false;
        if (MinecraftClient.getInstance().getWindow() != null) {
            long winHandle = MinecraftClient.getInstance().getWindow().getHandle();
            isShift = GLFW.glfwGetKey(winHandle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                      GLFW.glfwGetKey(winHandle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        }

        if (isShift) {
            if (click.button() == 0) {
                float px = panelX();
                float py = panelY();
                if (f5 >= px - 20.0f && f5 <= px + panelW() + 20.0f && f6 >= py - 20.0f && f6 <= py + PANEL_H + 20.0f) {
                    isDraggingPanel = true;
                    dragGrabX = f5 - px;
                    dragGrabY = f6 - py;
                    return true;
                }
            } else if (click.button() == 2) {
                customPanelX = -1.0f;
                customPanelY = -1.0f;
                isDraggingPanel = false;
                return true;
            }
        }


        if (click.button() == 0 && this.contentCategory == Category.DISPLAY
                && !inspector.isOpen() && !bindPopup.isOpen() && !settingsPopup.isVisible()
                && panicButtonContains(f5, f6)) {
            capitulationHold.begin(System.nanoTime());
            Sounds.play("select_category");
            return true;
        }

        float railX = f3 + 5.0f + sidebarW() + 1.5f;
        float railY = f4 + 5.0f;
        float railH = PANEL_H - 10.0f;
        if (f5 >= railX - 4.0f && f5 <= railX + 4.0f && f6 >= railY && f6 <= railY + railH) {
            if (click.button() == 2 || doubled) {
                customSidebarW = DEFAULT_SIDEBAR_W;
                isDraggingSidebar = false;
                Sounds.play("select_category");
                return true;
            }
            if (click.button() == 0) {
                isDraggingSidebar = true;
                sidebarDragGrabX = f5 - (f3 + 5.0f + customSidebarW);
                return true;
            }
        }

        if (this.moduleList.mouseBind(click.button())) {
            return true;
        }
        if (this.bindPopup.isOpen() && this.bindPopup.mouseBind(click.button())) {
            return true;
        }
        if (this.bindPopup.isOpen() && click.button() == 0) {
            this.bindPopup.click(f5, f6);
            return true;
        }
        if (this.inspector.isOpen() && this.inspector.mouseClicked(f5, f6, click.button())) {
            return true;
        }
        if (this.search.mouseClicked(f5, f6, click.button())) {
            return true;
        }
        if (this.presetRenderer.isDrawerOpen() && this.presetRenderer.drawerMouseClicked(f5, f6, click.button())) {
            return true;
        }
        if (this.contentCategory == Category.PRESETS && this.presetRenderer.click(f5, f6, click.button())) return true;
        if (this.contentCategory == Category.THEMES && click.button() == 0 && this.themesRenderer.click(f3, f4, f, f5, f6)) {
            return true;
        }
        if (click.button() == 0 || click.button() == 1) {
            boolean bl = this.isModuleView();
            if (bl && this.moduleList.scrollbarGrab(f5, f6)) {
                return true;
            }
            float f7 = f3 + contentXOff();
            float f8 = f4 + CONTENT_Y_OFFSET + HEADER_OFFSET;
            float f9 = f - contentInset();
            float f10 = moduleViewportHeight(this.contentCategory);
            if (bl) {
                List<Module> list = this.filteredModules(this.contentCategory);
                if (this.moduleList.clickOverlays(f5, f6, list, f7, f8, f9)) {
                    return true;
                }
                if (f5 >= f7 && f5 <= f7 + f9 && f6 >= f8 && f6 <= f8 + f10) {
                    for (int i = 0; i < list.size(); ++i) {
                        Module module = list.get(i);
                        ModuleListRenderer.CardLayout layout = this.moduleList.getCardLayout(list, i, f7, f8, f9, f10, 1.0f, true);
                        if (layout == null || !layout.visible) continue;
                        if (!layout.containsCard(f5, f6)) continue;

                        if (module instanceof activity.client.gui.custom.PresetSettingsModule) {
                            this.selectCategoryFromWorkspace(Category.PRESETS);
                            return true;
                        }
                        if (module instanceof activity.client.gui.custom.CapabilityLogsModule logsModule) {
                            float baseH = this.moduleList.baseCardHeight(module, layout.cardW);
                            if (f6 > layout.cardY + baseH) {
                                this.moduleList.clickSettings(module, layout.cardX, layout.cardY, layout.cardW, f5, f6, click.button());
                            } else {
                                logsModule.copyLogsToClipboard();
                            }
                            return true;
                        }
                        if (module instanceof activity.client.integration.CompanionSettingsModule companion) {
                            activity.client.integration.NivoratEcosystem.open(companion.group(), this);
                            return true;
                        }

                        float baseH = this.moduleList.baseCardHeight(module, layout.cardW);
                        if (f6 > layout.cardY + baseH) {
                            this.moduleList.clickSettings(module, layout.cardX, layout.cardY, layout.cardW, f5, f6, click.button());
                            return true;
                        }

                        if (this.search.hasText() && UI.ctrlHeld()) {
                            Category category = module.getCategory();
                            this.moduleList.focusModule(module.getName());
                            if (this.targetCategory != category) {
                                Sounds.play("select_category");
                                this.selectCategory(category);
                            } else {
                                this.search.setText("");
                                this.search.blur();
                            }
                            return true;
                        }

                        if (click.button() == 1) {
                            if (!module.getSettings().all().isEmpty()) {
                                if (this.inspector.getFocusedModule() == module && this.inspector.isOpen()) {
                                    this.inspector.close();
                                } else {
                                    this.inspector.open(module);
                                }
                            }
                            return true;
                        }

                        if (click.button() == 0) {
                            if (layout.hitPin(f5, f6)) {
                                PinManager.toggle(module);
                                return true;
                            }
                            if (layout.hitSettings(f5, f6)) {
                                if (this.inspector.getFocusedModule() == module && this.inspector.isOpen()) {
                                    this.inspector.close();
                                } else {
                                    this.inspector.open(module);
                                }
                                return true;
                            }
                            if (module instanceof ClickGui || module instanceof VisualMaterial) {
                                if (this.inspector.getFocusedModule() == module && this.inspector.isOpen()) {
                                    this.inspector.close();
                                } else {
                                    this.inspector.open(module);
                                }
                            } else {
                                module.toggle();
                            }
                            return true;
                        }
                        return true;
                    }
                }
            }
            if (click.button() == 1) {
                return true;
            }
        }


        if (this.isModuleView() && click.button() == 0) {
            float f7 = f3 + contentXOff();
            float f8 = f4 + CONTENT_Y_OFFSET;
            float f9 = f - contentInset();
            float f10 = 6.0f;
            float profileLeft=f7+f9-6.0f;
            float langW = 34.0f;
            float langH = 14.0f;
            float langX = profileLeft - langW;
            float langY = f8 + (HEADER_H - langH) * 0.5f;

            if (f5 >= langX && f5 <= langX + langW && f6 >= langY && f6 <= langY + langH) {
                boolean isCurrentlyRu = activity.client.gui.custom.api.localization.LocalizationManager.getCurrentLanguage() == activity.client.gui.custom.api.localization.LocalizationManager.Language.RU;
                activity.client.gui.custom.api.localization.LocalizationManager.setLanguage(isCurrentlyRu ? activity.client.gui.custom.api.localization.LocalizationManager.Language.EN : activity.client.gui.custom.api.localization.LocalizationManager.Language.RU);
                this.moduleList.invalidateCache();
                Sounds.play("select_category");
                return true;
            }
        }

        if (click.button() != 0) {
            return super.mouseClicked(click, doubled);
        }

        Category category = this.categoryButtonAt(f3, f4, f5, f6);
        if (category != null) {
            if (category != this.targetCategory) {
                Sounds.play("select_category");
            }
            this.selectCategory(category);
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (activity.client.gui.custom.AutoCartCalibrationDrawer.mouseScrolled(mouseX, mouseY, verticalAmount)) return true;
        if (activity.client.gui.custom.CollectionDrawer.isOpen()) return activity.client.gui.custom.CollectionDrawer.screen().mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        if (activity.client.gui.custom.NativeBindAssignment.isOpen()) return true;
        if (!this.screenAnim.canInteract()) {
            return true;
        }
        if (this.presetDropdownOpen && Position.mouseX() >= this.lastPresetDropX && Position.mouseX() <= this.lastPresetDropX + this.lastPresetDropW && Position.mouseY() >= this.lastPresetDropY && Position.mouseY() <= this.lastPresetDropY + this.lastPresetDropH) {
            if (verticalAmount > 0 && this.presetScrollOffset > 0) this.presetScrollOffset--;
            else if (verticalAmount < 0 && this.presetScrollOffset + 6 < this.cachedPresetEntries.size()) this.presetScrollOffset++;
            return true;
        }
        if (this.bindPopup.isOpen()) {
            this.bindPopup.close();
            return true;
        }
        if (this.inspector.isOpen() && this.inspector.mouseScrolled(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        float f = PANEL_W;
        float f2 = panelX();
        float f3 = panelY();
        float f4 = f2 + contentXOff();
        float f5 = f3 + CONTENT_Y_OFFSET;
        float f6 = f - contentInset();
        float f7 = CONTENT_HEIGHT;
        double d = Position.mouseX();
        double d2 = Position.mouseY();
        if (this.themesRenderer.getEditor().isOpen()) {
            if (this.themesRenderer.getEditor().mouseScrolled(mouseX, mouseY, verticalAmount)) {
                return true;
            }
        }
        if (this.presetRenderer.isDrawerOpen()) {
            if (this.presetRenderer.drawerMouseScrolled(horizontalAmount, verticalAmount)) {
                return true;
            }
        }
        if (d >= (double)f4 && d <= (double)(f4 + f6) && d2 >= (double)f5 && d2 <= (double)(f5 + f7)) {
            if (this.contentCategory == Category.THEMES) {
                this.themesRenderer.scroll(verticalAmount, f7);
            } else if (this.contentCategory == Category.PRESETS) {
                this.presetRenderer.scroll(verticalAmount);

            } else {
                float f8 = f3 + CONTENT_Y_OFFSET + HEADER_OFFSET;
                float f9 = f - contentInset();
                if (!this.moduleList.scrollOverlays((float)d, (float)d2, verticalAmount, this.filteredModules(this.contentCategory), f4, f8, f9)) {
                    this.moduleList.scroll(verticalAmount, moduleViewportHeight(this.contentCategory));
                }
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (this.screenAnim.isClosing()) return true;
        if (activity.client.gui.custom.AutoCartCalibrationDrawer.mouseDragged(click, deltaX, deltaY)) return true;
        if (activity.client.gui.custom.api.drags.DragSystem.get().mouseDragged(click, deltaX, deltaY)) return true;
        if (this.themesRenderer.getEditor().mouseDragged(Position.mouseX(), Position.mouseY(), click.button())) return true;
        return super.mouseDragged(click, deltaX, deltaY);
    }

    public boolean mouseReleased(Click click) {
        activity.client.gui.custom.AutoCartCalibrationDrawer.mouseReleased(click);
        activity.client.gui.custom.api.drags.DragSystem.get().mouseReleased(click);
        if (click.button() == 0) capitulationHold.cancel();
        if (this.screenAnim.isClosing()) {
            return true;
        }
        if (this.themesRenderer.getEditor().isOpen()) {
            this.themesRenderer.getEditor().mouseReleased(click.x(), click.y(), click.button());
            return true;
        }
        if (this.presetRenderer.isDrawerOpen()) {
            if (this.presetRenderer.drawerMouseReleased(click.x(), click.y(), click.button())) {
                return true;
            }
        }
        if (click.button() == 0) {
            isDraggingPanel = false;
            isDraggingSidebar = false;
            this.moduleList.releaseAllDrags();
            this.bindPopup.releaseDrag();
        }
        this.inspector.mouseReleased(click.button());
        this.moduleList.scrollbarRelease();
        this.search.mouseReleased(click.button());
        this.themesRenderer.mouseReleased(click.x(), click.y(), click.button());
        return super.mouseReleased(click);
    }

    public boolean charTyped(CharInput input) {
        if (activity.client.gui.custom.CollectionDrawer.isOpen()) return activity.client.gui.custom.CollectionDrawer.screen().charTyped(input);
        if (activity.client.gui.custom.NativeBindAssignment.isOpen()) return true;
        if (this.screenAnim.isClosing()) {
            return true;
        }
        if (this.themesRenderer.getEditor().isOpen()) {
            if (this.themesRenderer.getEditor().charTyped((char) input.codepoint())) {
                return true;
            }
        }
        if (this.presetRenderer.isDrawerOpen()) {
            if (this.presetRenderer.drawerCharTyped(input)) {
                return true;
            }
        }
        if (this.contentCategory == Category.PRESETS && this.presetRenderer.charTyped(input)) return true;
        if (this.contentCategory == Category.THEMES && this.themesRenderer.charTyped(input)) {
            return true;
        }
        if (this.bindPopup.isOpen()) {
            return true;
        }
        if (this.inspector.isOpen() && this.inspector.charTyped(input)) {
            return true;
        }
        if (this.moduleList.charTyped(input)) {
            return true;
        }
        if (this.search.isTyping() && this.search.charTyped(input)) {
            return true;
        }
        return super.charTyped(input);
    }

    public static boolean isSearchTyping() {
        return UI.INSTANCE.search.isTyping();
    }

    public static boolean isInputActive() {
        if (!isOpen()) {
            return false;
        }
        if (INSTANCE.themesRenderer != null && INSTANCE.themesRenderer.getEditor().isOpen()) {
            return true;
        }
        if (INSTANCE.presetRenderer != null && INSTANCE.presetRenderer.isInputActive()) {
            return true;
        }
        if (isSearchTyping()) {
            return true;
        }
        if (INSTANCE.moduleList.isAnyWidgetFocused()) {
            return true;
        }
        if (INSTANCE.bindPopup != null && INSTANCE.bindPopup.isOpen()) {
            return true;
        }
        return false;
    }

    public static boolean guiCaptureActive() {
        boolean bl;
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (!GuiCapture.isBound(INSTANCE)) {
            return false;
        }
        boolean bl2 = bl = WorldGuiCloseAnimation.isActive() && !WorldGuiCloseAnimation.isFinished() && (minecraftClient == null || minecraftClient.currentScreen == null || WorldGuiCloseAnimation.isReversing());
        boolean bl3 = UI.INSTANCE.screenAnim.isClosing() && (WorldGuiCloseAnimation.isActive() ? bl : !UI.INSTANCE.screenAnim.isCloseFinished());
        return !(!UI.isOpen() && !bl3 || !UI.INSTANCE.screenAnim.isAnimating() && !bl);
    }


    private void releaseAllDrags() {
        isDraggingPanel = false;
        isDraggingSidebar = false;
        this.moduleList.releaseAllDrags();
        this.settingsPopup.releaseDrags();
        this.moduleList.scrollbarRelease();
    }

    public static float guiCaptureScale() {
        return guiCaptureScale;
    }

    public static boolean isSettingsPopupVisible() {
        return INSTANCE.settingsPopup.isVisible();
    }

    public void openModuleSettings(Module module) {
        if (module == null) {
            return;
        }
        this.selectCategoryFromWorkspace(module.getCategory());
        this.search.setText("");
        this.search.blur();
        this.moduleList.focusModule(module.getName());
        this.inspector.open(module);
    }

    private List<Module> filteredModules(Category category) {
        String query = this.search.getText().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            if (category == Category.PINNED) {
                return PinManager.getPinnedModules();
            }
            return ModuleManager.get().forCategory(category);
        }

        return activity.client.gui.custom.DetailedModuleSearch.search(query);
    }

    public static boolean isSettingsOpenFor(String string) {
        return UI.isOpen() && UI.INSTANCE.moduleList.isExpanded(string);
    }

    public static boolean consumePopupStratumMark() {
        boolean bl = popupStratumMarked;
        popupStratumMarked = false;
        return bl;
    }

    public static boolean consumePopupBlurCapture() {
        if (!popupBlurWanted) {
            return false;
        }
        popupBlurWanted = false;
        popupBlurCaptured = true;
        return true;
    }

    public static boolean consumePopupLayerCapture() {
        if (!popupLayerBlurWanted) {
            return false;
        }
        popupLayerBlurWanted = false;
        popupBlurCaptured = true;
        return true;
    }

    public static void requestVanillaBlurAtSplit() {
        vanillaBlurRequested = true;
    }

    public static boolean consumeVanillaBlurRequest() {
        boolean bl = vanillaBlurRequested;
        vanillaBlurRequested = false;
        return bl;
    }

    private static boolean isMainCategory(Category category) {
        if (category == null) {
            return false;
        }
        for (Category category2 : MAIN_CATEGORIES) {
            if (category2 != category) continue;
            return true;
        }
        return false;
    }

    public static void applyMainCompositeAtSplit() {
        if (!motionBlurPending) {
            return;
        }
        motionBlurPending = false;
        cardBlurPending = false;
        GuiMotionBlurRenderer.applyWithCopy(motionBlurOpacity, motionBlurRadius, motionBlurX, motionBlurY, motionBlurW, motionBlurH, motionBlurMask, 2, motionBlurSrcX, motionBlurSrcY, motionBlurSrcW, motionBlurSrcH, motionBlurScale, motionBlurOriginX, motionBlurOriginY);
    }

    private void stagePopupBlur() {
        if (!popupBlurStaged) {
            return;
        }
        popupBlurStaged = false;
        int n = 1;
        if (this.settingsPopup.writeBlurRect(popupBlurMask, n * 6)) {
            ++n;
        }
        if (this.bindPopup.writeBlurRect(popupBlurMask, n * 6)) {
            ++n;
        }
        if (n <= 1) {
            return;
        }
        float f = 0.0f;
        for (int i = 1; i < n; ++i) {
            f = Math.max(f, popupBlurMask[i * 6 + 5]);
        }
        if (f <= 0.004f) {
            return;
        }
        float f2 = Render2DCoordinateSpace.designGuiScale();
        float f3 = Math.max(0.5f, 9.0f * f2 * f);
        float f4 = Float.MAX_VALUE;
        float f5 = Float.MAX_VALUE;
        float f6 = -3.4028235E38f;
        float f7 = -3.4028235E38f;
        for (int i = 1; i < n; ++i) {
            int n2 = i * 6;
            float f8 = popupBlurMask[n2 + 5] > 0.004f ? f3 : 0.0f;
            float f9 = (popupBlurMask[n2] + this.parallaxX) * f2 - f8;
            float f10 = (popupBlurMask[n2 + 1] + this.parallaxY) * f2 - f8;
            float f11 = popupBlurMask[n2 + 2] * f2 + f8 * 2.0f;
            float f12 = popupBlurMask[n2 + 3] * f2 + f8 * 2.0f;
            UI.popupBlurMask[n2] = f9;
            UI.popupBlurMask[n2 + 1] = f10;
            UI.popupBlurMask[n2 + 2] = f11;
            UI.popupBlurMask[n2 + 3] = f12;
            f4 = Math.min(f4, f9);
            f5 = Math.min(f5, f10);
            f6 = Math.max(f6, f9 + f11);
            f7 = Math.max(f7, f10 + f12);
        }
        float f13 = 24.0f + f3;
        UI.popupBlurMask[0] = f4 - f13;
        UI.popupBlurMask[1] = f5 - f13;
        UI.popupBlurMask[2] = f6 - f4 + f13 * 2.0f;
        UI.popupBlurMask[3] = f7 - f5 + f13 * 2.0f;
        UI.popupBlurMask[4] = -1.0f;
        UI.popupBlurMask[5] = 0.0f;
        popupBlurMaskCount = n;
        float f14 = f13 + f3 * 2.0f + 8.0f;
        popupBlurRadius = f3;
        popupBlurX = Math.round(f4 - f14);
        popupBlurY = Math.round(f5 - f14);
        popupBlurW = Math.round(f6 - f4 + f14 * 2.0f);
        popupBlurH = Math.round(f7 - f5 + f14 * 2.0f);
        popupBlurOriginX = (f4 + f6) * 0.5f;
        popupBlurOriginY = (f5 + f7) * 0.5f;
        popupBlurPending = true;
    }

    private void stageCardBlur(float f, float f2, float f3) {
        float f4;
        float f5;
        float[] fArray;
        float f6;
        int n;
        if (motionBlurPending || !cardCaptureStaged) {
            return;
        }
        float f7 = f + contentXOff();
        float f8 = f3 - contentInset();
        if (this.moduleList.cardBlurCount() > 0) {
            n = this.moduleList.cardBlurCount();
            f6 = this.moduleList.cardBlurMaxPhase();
            fArray = this.moduleList.cardBlurRects();
            f5 = f2 + CONTENT_Y_OFFSET + HEADER_OFFSET;
            f4 = CONTENT_HEIGHT - HEADER_OFFSET;
        } else {
            n = this.themesRenderer.cardBlurCount();
            f6 = this.themesRenderer.cardBlurMaxPhase();
            fArray = this.themesRenderer.cardBlurRects();
            f5 = f2 + CONTENT_Y_OFFSET;
            f4 = CONTENT_HEIGHT;
        }
        if (n <= 0 || f6 <= 0.003f) {
            return;
        }
        float f9 = Render2DCoordinateSpace.designGuiScale();
        float f10 = f7 * f9;
        float f11 = f5 * f9;
        float f12 = f8 * f9;
        float f13 = f4 * f9;
        float f14 = 48.0f;
        UI.cardBlurMask[0] = f10 - f14;
        UI.cardBlurMask[1] = f11 - f14;
        UI.cardBlurMask[2] = f12 + f14 * 2.0f;
        UI.cardBlurMask[3] = f13 + f14 * 2.0f;
        UI.cardBlurMask[4] = -1.0f;
        UI.cardBlurMask[5] = 0.0f;
        int n2 = 1;
        for (int i = 0; i < n; ++i) {
            int n3 = i * 6;
            int n4 = n2 * 6;
            UI.cardBlurMask[n4] = fArray[n3] * f9;
            UI.cardBlurMask[n4 + 1] = fArray[n3 + 1] * f9;
            UI.cardBlurMask[n4 + 2] = fArray[n3 + 2] * f9;
            UI.cardBlurMask[n4 + 3] = fArray[n3 + 3] * f9;
            UI.cardBlurMask[n4 + 4] = fArray[n3 + 4];
            UI.cardBlurMask[n4 + 5] = fArray[n3 + 5] / f6;
            ++n2;
        }
        cardBlurMaskCount = n2;
        motionBlurOpacity = 1.0f;
        motionBlurRadius = Math.max(0.5f, 22.0f * f6);
        float f15 = f14 + motionBlurRadius * 2.0f + 8.0f;
        motionBlurX = Math.round(f10 - f15);
        motionBlurY = Math.round(f11 - f15);
        motionBlurW = Math.round(f12 + f15 * 2.0f);
        motionBlurH = Math.round(f13 + f15 * 2.0f);
        motionBlurScale = 1.0f;
        motionBlurOriginX = (f7 + f8 * 0.5f) * f9;
        motionBlurOriginY = (f5 + f4 * 0.5f) * f9;
        cardBlurPending = true;
    }

    public static boolean motionBlurCapturePending() {
        return motionBlurPending;
    }

    private static void beginShatter() {
        if (GuiShatterAnimation.resume()) {
            return;
        }
        float f = Render2DCoordinateSpace.designGuiScale();
        VisualMaterial interfaceModule = VisualMaterial.getInstance();
        float f2 = interfaceModule != null && interfaceModule.rectGlow.getValue() ? interfaceModule.rectGlowRadius.getFloat() : 0.0f;
        float f3 = (f2 + 14.0f) * f;
        float f4 = PANEL_W * f;
        float f5 = PANEL_H * f;
        float f6 = Position.screenWidth() * f;
        float f7 = Position.screenHeight() * f;
        float sx = panelX() * f;
        float sy = panelY() * f;
        GuiShatterAnimation.begin(sx, sy, f4, f5, f3, f6, f7);
    }

    public static boolean consumeCardStratumMark() {
        boolean bl = cardStratumMarked;
        cardStratumMarked = false;
        if (bl) {
            cardBlurCaptured = true;
        }
        return bl;
    }

    public static boolean popupLayerCapturePending() {
        return popupLayerBlurWanted;
    }

    public static float motionBlurCaptureRadius() {
        return motionBlurRadius;
    }

    public static void markPopupLayerCapture() {
        popupLayerBlurWanted = true;
    }

    public static boolean consumePanelSplitMark() {
        boolean bl = panelSplitMarked;
        panelSplitMarked = false;
        return bl;
    }

    public static void markPopupStratum(boolean bl, boolean bl2) {
        popupStratumMarked = true;
        popupBlurWanted = bl;
        popupBlurStaged = bl2;
        popupBlurCaptured = false;
    }



    public static void markCardStratum() {
        cardStratumMarked = true;
        cardCaptureStaged = true;
    }

    public static void dropPendingBlurs() {
        cardStratumMarked = false;
        panelSplitMarked = false;
        vanillaBlurRequested = false;
        popupStratumMarked = false;
        popupLayerBlurWanted = false;
        popupBlurWanted = false;
        popupBlurStaged = false;
        popupBlurCaptured = false;
        popupBlurPending = false;
        cardBlurPending = false;
        cardCaptureStaged = false;
        cardBlurCaptured = false;
    }

    public static float guiShatterProgress() {
        float f = WorldGuiCloseAnimation.isActive() ? WorldGuiCloseAnimation.progress() : UI.INSTANCE.screenAnim.closeProgress();
        return GuiShatterAnimation.progress(f);
    }

    @Override
    public float shatterProgress() {
        return UI.guiShatterProgress();
    }

    private void swapContentCategory(Category category) {
        this.moduleList.finishTransition();
        this.themesRenderer.finishTransition();
        this.presetDropdownOpen = false;
        this.presetDropdownAnim.setDirection(Direction.BACKWARDS);
        if (category == null) category = Category.VISUALS;
        this.contentCategory = category;
        if (this.presetRenderer.isDrawerOpen()) {
            this.presetRenderer.closeDrawer();
        }
        if (category == Category.PRESETS) this.presetRenderer.open();
        if (category == Category.THEMES) {
            this.themesRenderer.open(false);
        }
    }

    private void renderModuleHeader(DrawContext drawContext, float f, float f2, float f3, float f4, Category category, float f5, float f6) {
        float f7 = f + contentXOff();
        float f8 = f2 + CONTENT_Y_OFFSET + f6;
        float f9 = f3 - contentInset();
        float f10 = 6.0f;
        float f11 = 14.0f;
        float f12 = HEADER_H;
        float f13 = f8 + (f12 - f11) * 0.5f;
        float profileLeft=f7+f9-6.0f;

        float langW = 34.0f;
        float langH = 14.0f;
        float langX = profileLeft - langW;
        float langY = f8 + (f12 - langH) * 0.5f;

        float mouseX = Position.mouseX();
        float mouseY = Position.mouseY();
        boolean hoverLang = mouseX >= langX && mouseX <= langX + langW && mouseY >= langY && mouseY <= langY + langH;
        boolean isRu = activity.client.gui.custom.api.localization.LocalizationManager.getCurrentLanguage() == activity.client.gui.custom.api.localization.LocalizationManager.Language.RU;

        Render2D.rect(langX, langY, langW, langH, 3.5f, ThemeManager.rgba(0, (55.0f + (hoverLang ? 25.0f : 0.0f)) * f4));
        Render2D.outline(langX, langY, langW, langH, 3.5f, 0.6f, hoverLang ? ClientAccent.accent(160.0f * f4) : ThemeManager.rgba(0xFFFFFF, (18.0f + (hoverLang ? 16.0f : 0.0f)) * f4));
        if (hoverLang) {
            Render2D.glow(new BuiltGlow(langX, langY, langW, langH, new float[]{3.5f, 3.5f, 3.5f, 3.5f}, ClientAccent.accent(255.0f), 0.25f, 3.5f, f4));
        }

        float iconSize = 7.5f;
        int iconCol = hoverLang ? ClientAccent.accentBright(255.0f * f4) : ClientAccent.accentSoft(210.0f * f4);
        Fonts.NV.msdf(NvIcons.LANGUAGE, langX + 4.0f, langY + (langH - iconSize) * 0.5f, iconSize, iconCol);

        Render2D.rect(langX + 14.5f, langY + 3.0f, 0.6f, langH - 6.0f, 0.3f, ThemeManager.rgba(0xFFFFFF, 22.0f * f4));

        String langCode = isRu ? "RU" : "EN";
        float codeW = Fonts.MONTSERRAT_MEDIUM.width(langCode, 5.5f);
        int textCol = UI.color(255, 255, 255, hoverLang ? 255 : 220, f4);
        Fonts.MONTSERRAT_MEDIUM.draw(langCode, langX + langW - codeW - 4.5f, langY + 3.2f, 5.5f, textCol);

        float centerLogoSize = 22.0f;
        float logoCenterX = f7 + f9 * 0.5f;
        float logoCenterY = f8 + f12 * 0.5f;
        Render2D.glow(new BuiltGlow(logoCenterX - 10.0f, logoCenterY - 10.0f, 20.0f, 20.0f, new float[]{10.0f, 10.0f, 10.0f, 10.0f}, ClientAccent.accent(160.0f), 0.35f, 6.0f, f4));
        BrandMark.draw(logoCenterX - centerLogoSize * 0.5f, logoCenterY - centerLogoSize * 0.5f, centerLogoSize, f4);

        float searchX = f7 + 6.0f;
        float maxSearchW = Math.max(50.0f, (logoCenterX - centerLogoSize * 0.5f - 10.0f) - searchX);
        float targetExpandedW = Math.min(105.0f, maxSearchW);
        this.search.render(drawContext, searchX, f13, targetExpandedW, f11, f4, Position.mouseX(), Position.mouseY(), f5);
    }

    public boolean isTextInputFocused() {
        if (this.search != null && this.search.isTyping()) return true;
        if (activity.client.gui.custom.api.ui.settings.impl.TextSetting.isAnyFocused()) return true;
        if (this.presetRenderer != null && this.presetRenderer.isInputActive()) return true;
        if (this.themesRenderer != null && this.themesRenderer.getEditor() != null && this.themesRenderer.getEditor().isInputActive()) return true;
        return false;
    }

    @Override
    public float captureScale() {
        return guiCaptureScale;
    }

    private void renderSplitterRail(float panelX, float panelY, float panelH, float alpha, float dt) {
        float railX = panelX + 5.0f + sidebarW() + 1.5f;
        float railY = panelY + 5.0f;
        float railH = panelH - 10.0f;

        boolean overSplitter = Position.mouseX() >= railX - 4.0f && Position.mouseX() <= railX + 4.0f
                && Position.mouseY() >= railY && Position.mouseY() <= railY + railH;

        float targetHover = (overSplitter || isDraggingSidebar) ? 1.0f : 0.0f;
        float hoverSpeed = 1.0f - (float) Math.exp(-dt * 16.0f);
        this.splitterHoverT += (targetHover - this.splitterHoverT) * hoverSpeed;

        if (this.splitterHoverT > 0.01f) {
            Render2D.rect(railX - 0.5f, railY + 6.0f, 1.0f, railH - 12.0f, 0.5f, ClientAccent.accent(30.0f * this.splitterHoverT * alpha));

            float handleW = 2.0f;
            float handleH = 22.0f;
            float handleX = railX - handleW * 0.5f;
            float handleY = railY + (railH - handleH) * 0.5f;
            int handleCol = ClientAccent.accentBright((130.0f + 125.0f * this.splitterHoverT) * alpha);
            Render2D.rect(handleX, handleY, handleW, handleH, 1.0f, handleCol);
            Render2D.glow(new BuiltGlow(handleX, handleY, handleW, handleH, new float[]{1.0f, 1.0f, 1.0f, 1.0f}, ClientAccent.accent(255.0f), 0.35f, 4.0f, this.splitterHoverT * alpha));
        }
    }

    private void renderCategoryPanel(float f, float f2, float f3, float f4) {
        if (this.themesRenderer.getEditor().isOpen()) {
            f4 *= 0.55f;
        }
        float sidebarX = f;
        float sidebarW = sidebarW();
        boolean isCompact = sidebarW < 58.0f;
        boolean isExpanded = sidebarW > 105.0f;
        boolean isRu = activity.client.gui.custom.api.localization.LocalizationManager.getCurrentLanguage() == activity.client.gui.custom.api.localization.LocalizationManager.Language.RU;

        float headerY = f2 + 3.0f;
        float headerH = HEADER_H;
        RenderHelper.drawPanelBg(sidebarX, headerY, sidebarW, headerH, 12.0f, 0.0f, 0.0f, 0.0f, f4);
        float headerCenterY = headerY + headerH * 0.5f;

        float btnW = isCompact ? (sidebarW - 7.0f) : Math.min(sidebarW - 10.0f, 76.0f);
        float btnX = sidebarX + (sidebarW - btnW) * 0.5f;
        float btnY = headerY + 2.5f;
        float btnH = headerH - 5.0f;
        this.lastPresetBtnX = btnX;
        this.lastPresetBtnY = btnY;
        this.lastPresetBtnW = btnW;
        this.lastPresetBtnH = btnH;

        float pMx = Position.mouseX();
        float pMy = Position.mouseY();
        boolean hoverPreset = pMx >= btnX && pMx <= btnX + btnW && pMy >= btnY && pMy <= btnY + btnH;

        Render2D.rect(btnX, btnY, btnW, btnH, 3.5f, ThemeManager.rgba(0, (presetDropdownOpen ? 75.0f : hoverPreset ? 60.0f : 40.0f) * f4));
        Render2D.outline(btnX, btnY, btnW, btnH, 3.5f, 0.6f, (presetDropdownOpen || hoverPreset) ? ClientAccent.accent(160.0f * f4) : ThemeManager.rgba(0xFFFFFF, 18.0f * f4));
        if (hoverPreset || presetDropdownOpen) {
            Render2D.glow(new BuiltGlow(btnX, btnY, btnW, btnH, new float[]{3.5f, 3.5f, 3.5f, 3.5f}, ClientAccent.accent(255.0f), 0.25f, 3.5f, f4));
        }

        float pIconSize = 7.5f;
        int pIconCol = (hoverPreset || presetDropdownOpen) ? ClientAccent.accentBright(255.0f * f4) : ClientAccent.accentSoft(210.0f * f4);

        if (isCompact) {
            Fonts.NV.msdf(NvIcons.PROFILE, btnX + (btnW - pIconSize) * 0.5f, btnY + (btnH - pIconSize) * 0.5f, pIconSize, pIconCol);
        } else {
            Fonts.NV.msdf(NvIcons.PROFILE, btnX + 4.5f, btnY + (btnH - pIconSize) * 0.5f, pIconSize, pIconCol);
            String presetLabel = this.selectedPresetName != null && !this.selectedPresetName.isEmpty() ? this.selectedPresetName : (isRu ? "Пресеты" : "Presets");
            float maxLabelW = btnW - (4.5f + pIconSize + 3.0f + 11.0f);
            float fontH = 6.5f;
            float labelW = Fonts.MONTSERRAT_MEDIUM.width(presetLabel, fontH);
            if (labelW > maxLabelW && presetLabel.length() > 6) {
                while (presetLabel.length() > 4 && Fonts.MONTSERRAT_MEDIUM.width(presetLabel + "…", fontH) > maxLabelW) {
                    presetLabel = presetLabel.substring(0, presetLabel.length() - 1);
                }
                presetLabel += "…";
            }
            int textCol = UI.color(255, 255, 255, (hoverPreset || presetDropdownOpen) ? 255 : 210, f4);
            Fonts.MONTSERRAT_MEDIUM.draw(presetLabel, btnX + 4.5f + pIconSize + 3.5f, btnY + (btnH - fontH) * 0.5f + 0.5f, fontH, textCol);
            Fonts.NV.msdf(NvIcons.CHEVRON_DOWN, btnX + btnW - 9.5f, btnY + (btnH - 5.0f) * 0.5f, 5.0f, pIconCol);
        }

        float catStartY = headerY + headerH + 6.0f;
        for (int i = 0; i < MAIN_CATEGORIES.length; ++i) {
            Category category = MAIN_CATEGORIES[i];
            float itemY = catStartY + (float)i * (CAT_SUB_ROW_H + CAT_SUB_GAP);
            float itemCenterY = itemY + CAT_SUB_ROW_H * 0.5f;
            float anim = this.getCategoryAnim(category).getOutput().floatValue();

            if (anim > 0.01f) {
                float indH = 8.0f * anim;
                float indW = 2.0f;
                float indX = sidebarX + (isCompact ? 2.0f : 3.0f);
                float indY = itemCenterY - indH * 0.5f;
                Render2D.rect(indX, indY, indW, indH, 1.0f, ClientAccent.accent(255.0f * anim * f4));
                Render2D.glow(new BuiltGlow(indX, indY, indW, indH, new float[]{1.0f, 1.0f, 1.0f, 1.0f}, ClientAccent.accent(255.0f), 0.35f, 4.0f, anim * f4));
            }

            int textAlpha = Math.min(255, 140 + Math.round(anim * 115.0f));
            int textCol = UI.color(255, 255, 255, textAlpha, f4);
            float iconSize = 10.5f;
            int iconCol = anim > 0.01f ? ClientAccent.accentBright((float)textAlpha * f4) : UI.color(255, 255, 255, textAlpha, f4);
            int moduleCount = category == Category.PINNED ? PinManager.getPinnedCount() : (category == Category.THEMES ? (activity.client.gui.custom.api.ui.theme.Theme.values().length + activity.client.gui.custom.api.ui.theme.CustomThemeManager.getCustomThemes().size()) : ModuleManager.get().forCategory(category).size());

            if (isCompact) {
                float iconX = sidebarX + (sidebarW - iconSize) * 0.5f;
                activity.client.gui.custom.KitIcons.draw(category, iconX, itemCenterY - iconSize * 0.5f, iconSize, iconCol);
                if (category == Category.PINNED && moduleCount > 0) {
                    float dotSize = 3.5f;
                    Render2D.rect(iconX + iconSize - 1.0f, itemCenterY - iconSize * 0.5f - 1.0f, dotSize, dotSize, dotSize * 0.5f, ClientAccent.accentBright(255.0f * f4));
                }
            } else if (isExpanded) {
                float iconX = sidebarX + 9.0f;
                activity.client.gui.custom.KitIcons.draw(category, iconX, itemCenterY - iconSize * 0.5f, iconSize, iconCol);

                String badgeText = moduleCount + (category == Category.PINNED ? (isRu ? " закр." : " pin") : (category == Category.THEMES ? (isRu ? " тем" : " themes") : (isRu ? " мод." : " mods")));
                float bTextW = Fonts.MONTSERRAT_MEDIUM.width(badgeText, 5.0f);
                float badgeW = bTextW + 8.0f;
                float badgeH = 11.0f;
                float badgeX = sidebarX + sidebarW - 7.0f - badgeW;
                float badgeY = itemCenterY - badgeH * 0.5f;
                drawSidebarLabel(category.getDisplayName(), iconX + iconSize + 5.0f, itemCenterY, badgeX - iconX - iconSize - 11.0f, textCol);
                int bgCol = anim > 0.01f ? ClientAccent.accent(35.0f * anim * f4) : UI.color(255, 255, 255, 12, f4);
                Render2D.rect(badgeX, badgeY, badgeW, badgeH, 3.5f, bgCol);
                int countCol = anim > 0.01f ? ClientAccent.accentBright(240.0f * f4) : UI.color(255, 255, 255, 160, f4);
                Fonts.MONTSERRAT_MEDIUM.draw(badgeText, badgeX + 4.0f, badgeY + 2.5f, 5.0f, countCol);
            } else {
                float iconX = sidebarX + 9.0f;
                activity.client.gui.custom.KitIcons.draw(category, iconX, itemCenterY - iconSize * 0.5f, iconSize, iconCol);

                String count = String.valueOf(moduleCount);
                float countW = Fonts.MONTSERRAT_MEDIUM.width(count, 5.5f);
                drawSidebarLabel(category.getDisplayName(), iconX + iconSize + 5.0f, itemCenterY, sidebarX + sidebarW - 13.0f - countW - iconX - iconSize - 5.0f, textCol);
                Fonts.MONTSERRAT_MEDIUM.draw(count, sidebarX + sidebarW - 8.0f - countW, itemCenterY - 2.5f, 5.5f, UI.color(255, 255, 255, Math.round(80.0f + 80.0f * anim), f4));
            }
        }


        float sidebarBottom = f2 + PANEL_H - 10.0f;
        float systemStartY = sidebarBottom - (float)SYSTEM_CATEGORIES.length * (CAT_OTHER_ROW_H + CAT_SUB_GAP) - 4.0f;
        float dividerY = systemStartY - 6.0f;
        float divPad = isCompact ? 5.0f : 8.0f;
        Render2D.rect(sidebarX + divPad, dividerY, sidebarW - divPad * 2.0f, 0.6f, 0.3f, UI.color(255, 255, 255, 18, f4));

        for (int i = 0; i < SYSTEM_CATEGORIES.length; ++i) {
            Category category = SYSTEM_CATEGORIES[i];
            float rowVisibility = 1.0f;
            float itemY = systemStartY + (float)i * (CAT_OTHER_ROW_H + CAT_SUB_GAP);
            float itemCenterY = itemY + CAT_OTHER_ROW_H * 0.5f;
            float themesAnim = this.getCategoryAnim(category).getOutput().floatValue();

            if (themesAnim > 0.01f) {
                float indH = 8.0f * themesAnim;
                float indW = 2.0f;
                float indX = sidebarX + (isCompact ? 2.0f : 3.0f);
                float indY = itemCenterY - indH * 0.5f;
                Render2D.rect(indX, indY, indW, indH, 1.0f, ClientAccent.accent(255.0f * themesAnim * f4 * rowVisibility));
                Render2D.glow(new BuiltGlow(indX, indY, indW, indH, new float[]{1.0f, 1.0f, 1.0f, 1.0f}, ClientAccent.accent(255.0f), 0.35f, 4.0f, themesAnim * f4 * rowVisibility));
            }

            int n6 = Math.round((float)Math.min(255, 140 + Math.round(themesAnim * 115.0f)) * rowVisibility);
            int n7 = UI.color(255, 255, 255, n6, f4);
            float iconSize = 10.5f;
            int iconCol = themesAnim > 0.01f ? ClientAccent.accentBright((float)n6 * f4) : UI.color(255, 255, 255, n6, f4);

            if (isCompact) {
                float iconX = sidebarX + (sidebarW - iconSize) * 0.5f;
                activity.client.gui.custom.KitIcons.draw(category, iconX, itemCenterY - iconSize * 0.5f, iconSize, iconCol);
            } else if (isExpanded) {
                float iconX = sidebarX + 9.0f;
                activity.client.gui.custom.KitIcons.draw(category, iconX, itemCenterY - iconSize * 0.5f, iconSize, iconCol);
                Fonts.MONTSERRAT_MEDIUM.draw(category.getDisplayName(), iconX + iconSize + 5.0f, itemCenterY - 3.5f + 0.5f, 7.0f, n7);

                String badgeText = "v1.0";
                float bTextW = Fonts.MONTSERRAT_MEDIUM.width(badgeText, 5.0f);
                float badgeW = bTextW + 8.0f;
                float badgeH = 11.0f;
                float badgeX = sidebarX + sidebarW - 7.0f - badgeW;
                float badgeY = itemCenterY - badgeH * 0.5f;
                int bgCol = themesAnim > 0.01f ? ClientAccent.accent(35.0f * themesAnim * f4) : UI.color(255, 255, 255, 12, f4);
                Render2D.rect(badgeX, badgeY, badgeW, badgeH, 3.5f, bgCol);
                int countCol = themesAnim > 0.01f ? ClientAccent.accentBright(240.0f * f4) : UI.color(255, 255, 255, 160, f4);
                Fonts.MONTSERRAT_MEDIUM.draw(badgeText, badgeX + 4.0f, badgeY + 2.5f, 5.0f, countCol);
            } else {
                float iconX = sidebarX + 9.0f;
                activity.client.gui.custom.KitIcons.draw(category, iconX, itemCenterY - iconSize * 0.5f, iconSize, iconCol);
                Fonts.MONTSERRAT_MEDIUM.draw(category.getDisplayName(), iconX + iconSize + 5.0f, itemCenterY - 3.5f + 0.5f, 7.0f, n7);
            }
        }
    }

    private static void drawSidebarLabel(String text, float x, float centerY, float maxWidth, int color) {
        float width = Fonts.MONTSERRAT_MEDIUM.width(text, 7.0f);
        float size = Math.max(6.2f, Math.min(7.0f, 7.0f * Math.max(1, maxWidth) / Math.max(1, width)));
        String fitted = activity.client.gui.custom.api.ui.settings.RenderHelper.fitText(Fonts.MONTSERRAT_MEDIUM, text, Math.max(1, maxWidth), size);
        Fonts.MONTSERRAT_MEDIUM.draw(fitted, x, centerY - size * .5f + .5f, size, color);
    }

    public static String categoryIconPath(Category category) {
        if (category == null) return "nivorat:textures/icons/modules.png";
        return switch (category) {
            case PINNED -> "nivorat:textures/icons/pinned.png";
            case VISUALS -> "nivorat:textures/icons/visuals.png";
            case DISPLAY -> "nivorat:textures/icons/interface.png";
            case UTILS -> "nivorat:textures/icons/utils.png";
            case THEMES -> "nivorat:textures/icons/themes.png";
            case PRESETS -> "nivorat:textures/icons/utils.png";
            case CART -> "minecraft:textures/block/tnt_side.png";
            default -> "nivorat:textures/icons/modules.png";
        };
    }

    private void updateCameraParallax() {
        ClientPlayerEntity clientPlayerEntity = IMinecraft.mc.player;
        if (clientPlayerEntity == null) {
            this.lastCameraYaw = Float.NaN;
            this.parallaxX = 0.0f;
            this.parallaxY = 0.0f;
            return;
        }
        float f = clientPlayerEntity.getYaw();
        float f2 = clientPlayerEntity.getPitch();
        if (Float.isNaN(this.lastCameraYaw)) {
            this.lastCameraYaw = f;
            this.lastCameraPitch = f2;
        }
        float f3 = MathHelper.wrapDegrees((float)(f - this.lastCameraYaw));
        float f4 = f2 - this.lastCameraPitch;
        this.lastCameraYaw = f;
        this.lastCameraPitch = f2;
        if (!this.screenAnim.isClosing()) {
            this.parallaxX = 0.0f;
            this.parallaxY = 0.0f;
            return;
        }
        double d = Math.max(30.0, (double)((Integer)IMinecraft.mc.options.getFov().getValue()).intValue());
        float f5 = (float)((double)Position.screenHeight() / d);
        this.parallaxX -= f3 * f5;
        this.parallaxY -= f4 * f5;
    }


    @Override
    public boolean captureActive() {
        return UI.guiCaptureActive();
    }

    public static float guiCaptureBlurRadius() {
        return guiCaptureBlurMainPx;
    }

    private void renderNoResults(float f, float f2, float f3, float f4) {
        float f5 = f + contentXOff();
        float f6 = f3 - contentInset();
        float f7 = f2 + CONTENT_Y_OFFSET + HEADER_OFFSET;
        float f8 = CONTENT_HEIGHT - HEADER_OFFSET;
        String string = activity.client.gui.custom.api.localization.Lang.get("ui.search.not_found", "Ничего не найдено");
        float f9 = 6.5f;
        float f10 = Fonts.MONTSERRAT_MEDIUM.width(string, f9);
        Fonts.MONTSERRAT_MEDIUM.draw(string, f5 + (f6 - f10) * 0.5f, f7 + f8 * 0.5f - 3.0f, f9, UI.color(255, 255, 255, 110, f4));
    }

    private void renderEmptyPinned(DrawContext drawContext, float f, float f2, float f3, float f4) {
        float f5 = f + contentXOff();
        float f6 = f2 + CONTENT_Y_OFFSET + HEADER_OFFSET;
        float f7 = PANEL_W - contentInset();
        float f8 = CONTENT_HEIGHT - HEADER_OFFSET;
        float centerY = f6 + f8 * 0.5f;

        float iconSize = 22.0f;
        float iconX = f5 + (f7 - iconSize) * 0.5f;
        Fonts.NV.msdf(NvIcons.PINNED, iconX, centerY - 28.0f, iconSize, ClientAccent.accentBright(220.0f * f4));

        String title = activity.client.gui.custom.api.localization.Lang.get("ui.pinned.empty.title", "Нет закреплённых модулей");
        float titleSize = 7.0f;
        float titleW = Fonts.MONTSERRAT_MEDIUM.width(title, titleSize);
        Fonts.MONTSERRAT_MEDIUM.draw(title, f5 + (f7 - titleW) * 0.5f, centerY, titleSize, UI.color(255, 255, 255, 225, f4));

        String sub = activity.client.gui.custom.api.localization.Lang.get("ui.pinned.empty.desc", "Нажмите на булавку в карточке чтобы закрепить модуль здесь");
        float subSize = 5.5f;
        float subW = Fonts.MONTSERRAT_MEDIUM.width(sub, subSize);
        Fonts.MONTSERRAT_MEDIUM.draw(sub, f5 + (f7 - subW) * 0.5f, centerY + 11.0f, subSize, UI.color(255, 255, 255, 115, f4));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        activity.client.gui.custom.ChatHudLayout.render(context);
        activity.client.gui.custom.AutoCartCalibrationDrawer.render(context, mouseX, mouseY);
    }

    @Override
    protected void renderScreen(DrawContext drawContext, int n, int n2, float f) {
        if (isDraggingPanel) {
            if (MinecraftClient.getInstance().getWindow() != null &&
                GLFW.glfwGetMouseButton(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_1) != GLFW.GLFW_PRESS) {
                isDraggingPanel = false;
            } else {
                float mouseX = Position.mouseX();
                float mouseY = Position.mouseY();
                customPanelX = mouseX - dragGrabX;
                customPanelY = mouseY - dragGrabY;
            }
        }
        this.renderPanel(drawContext);
        activity.client.gui.custom.api.drags.DragSystem.get().renderInGui(drawContext);
    }

    private void renderModuleHeaderPanel(float f, float f2, float f3, float f4, float f5) {
        RenderHelper.drawPanelBg(f, f2, f3, f4, 0.0f, 12.0f, 0.0f, 0.0f, f5);
    }

    private boolean isModuleView() {
        return this.contentCategory != null
            && this.contentCategory != Category.THEMES
            && this.contentCategory != Category.PRESETS;
    }

    private static String savedStartCategoryName = null;

    public static void setStartCategoryName(String name) {
        savedStartCategoryName = name;
    }

    public static String getLastCategoryName() {
        if (INSTANCE != null && INSTANCE.contentCategory != null) {
            return INSTANCE.contentCategory.name();
        }
        return savedStartCategoryName != null ? savedStartCategoryName : Category.VISUALS.name();
    }

    private Category resolveStartCategory() {
        if (savedStartCategoryName != null) {
            try {
                return Category.valueOf(savedStartCategoryName);
            } catch (Exception ignored) {}
        }
        return Category.VISUALS;
    }

    private void selectCategory(Category category) {
        if (category == null) {
            category = Category.VISUALS;
        }
        if (category == this.targetCategory && category == this.contentCategory) {
            return;
        }
        this.search.collapse();
        this.settingsPopup.close();
        this.presetDropdownOpen = false;
        this.presetDropdownAnim.setDirection(Direction.BACKWARDS);
        if (this.inspector.isOpen()) {
            this.inspector.close();
        }
        if (this.presetRenderer.isDrawerOpen()) {
            this.presetRenderer.closeDrawer();
        }
        this.targetCategory = category;
        savedStartCategoryName = category.name();
        activity.client.gui.custom.api.config.ConfigManager.markDirty();
        this.oldSubText = this.newSubText;
        this.newSubText = category.getDisplayName();
        this.subTextAnim.setDirection(Direction.FORWARDS);
        this.subTextAnim.counter.resetCounter();
        this.subTextAnimDone = false;
        for (Category category4 : Category.values()) {
            this.getCategoryAnim(category4).setDirection(category4 == category ? Direction.FORWARDS : Direction.BACKWARDS);
        }
        this.modulesHeaderAnim.setDirection(UI.isMainCategory(category) ? Direction.FORWARDS : Direction.BACKWARDS);
    }

    private void updateCategoryCrossfade(float f) {
        if (this.targetCategory == this.contentCategory) {
            if (this.contentCategory != null && this.categoryT < 1.0f) {
                this.categoryT = Math.min(1.0f, this.categoryT + f / 0.15f);
            }
            return;
        }
        if (this.contentCategory == null) {
            this.swapContentCategory(this.targetCategory);
            this.categoryT = 0.0f;
        } else {
            this.categoryT = Math.max(0.0f, this.categoryT - f / 0.15f);
            if (this.categoryT <= 0.0f) {
                this.categoryT = 0.0f;
                this.swapContentCategory(this.targetCategory);
            }
        }
    }

    public static void flushMotionBlur() {
        cardStratumMarked = false;
        panelSplitMarked = false;
        vanillaBlurRequested = false;
        popupStratumMarked = false;
        popupLayerBlurWanted = false;
        popupBlurWanted = false;
        popupBlurStaged = false;
        cardCaptureStaged = false;
        boolean bl = popupBlurCaptured;
        popupBlurCaptured = false;
        boolean bl2 = cardBlurCaptured;
        cardBlurCaptured = false;
        if (motionBlurPending) {
            motionBlurPending = false;
            cardBlurPending = false;
            popupBlurPending = false;
            GuiMotionBlurRenderer.applyWithCopy(motionBlurOpacity, motionBlurRadius, motionBlurX, motionBlurY, motionBlurW, motionBlurH, motionBlurMask, 2, motionBlurSrcX, motionBlurSrcY, motionBlurSrcW, motionBlurSrcH, motionBlurScale, motionBlurOriginX, motionBlurOriginY);
            return;
        }
        if (popupBlurPending && bl) {
            popupBlurPending = false;
            cardBlurPending = false;
            GuiMotionBlurRenderer.applyWithCopy(1.0f, popupBlurRadius, popupBlurX, popupBlurY, popupBlurW, popupBlurH, popupBlurMask, popupBlurMaskCount, popupBlurX, popupBlurY, popupBlurW, popupBlurH, 1.0f, popupBlurOriginX, popupBlurOriginY, true);
            return;
        }
        popupBlurPending = false;
        if (cardBlurPending && bl2) {
            cardBlurPending = false;
            GuiMotionBlurRenderer.applyWithCopy(motionBlurOpacity, motionBlurRadius, motionBlurX, motionBlurY, motionBlurW, motionBlurH, cardBlurMask, cardBlurMaskCount, motionBlurX, motionBlurY, motionBlurW, motionBlurH, 1.0f, motionBlurOriginX, motionBlurOriginY);
        }
    }

    @Override
    public float captureBlurRadius() {
        return guiCaptureBlurMainPx;
    }

    private Decelerate getCategoryAnim(Category category2) {
        return this.categoryAnims.computeIfAbsent(category2, category -> UI.createAnim(200));
    }

    private void renderPanel(DrawContext drawContext) {
        float f;
        boolean bl;
        float f2;
        this.screenAnim.updateFrame();
        this.updateCameraParallax();
        boolean bl2 = WorldGuiCloseAnimation.isDetachedRender();
        if (bl2) {
            this.parallaxX = 0.0f;
            this.parallaxY = 0.0f;
        }
        if (this.screenAnim.isCloseFinished() && !bl2) {
            if (MinecraftClient.getInstance().currentScreen == this) {
                this.screenAnim.snapClosed();
                MinecraftClient.getInstance().setScreen(null);
            }
            return;
        }
        float f3 = this.screenAnim.scale();
        float f4 = Math.max(Math.abs(this.parallaxX), Math.abs(this.parallaxY));
        if (f4 > 0.5f) {
            f2 = Math.min(1.0f, f4 / 20.0f);
            f3 += (1.0f - f3) * f2;
        }
        guiCaptureScale = f3;
        f2 = bl2 ? WorldGuiCloseAnimation.blurRadius() : this.screenAnim.blurRadius();
        guiCaptureBlurMainPx = Math.max(f2, GuiShatterAnimation.blurRadius()) * Render2DCoordinateSpace.designGuiScale();
        float f5 = this.screenAnim.alpha();
        float f6 = bl2 ? 1.0f : f5;
        float f7 = panelW();
        float f8 = PANEL_H;
        float f9 = panelX();
        float f10 = panelY();
        float drawerH = activity.client.gui.custom.AutoCartCalibrationDrawer.getVisualProtrusion();
        Render2D.rect(-10.0f, -10.0f, Position.screenWidth() + 20.0f, Position.screenHeight() + 20.0f, 0.0f, UI.color(0, 0, 0, 60, f5));
        if (UI.guiCaptureActive()) {
            Render2D.flush();
            Render2D.beginFrame(drawContext);
            GuiRenderState guiRenderState = ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState();
            guiRenderState.createNewRootLayer();
            guiRenderState.applyBlur();
        }
        this.moduleList.resetCardBlur();
        this.themesRenderer.resetCardBlur();
        drawContext.getMatrices().pushMatrix();
        if (this.parallaxX != 0.0f || this.parallaxY != 0.0f) {
            drawContext.getMatrices().translate(this.parallaxX, this.parallaxY);
        }
        if (isDraggingSidebar) {
            float targetW = (float)Position.mouseX() - (f9 + 5.0f) - sidebarDragGrabX;
            customSidebarW = Math.max(MIN_SIDEBAR_W, Math.min(MAX_SIDEBAR_W, targetW));
        }
        RectUtil.drawClientRect(f9, f10 - drawerH, f7, f8 + drawerH, 12.0f, f6, 6.0f);
        float f11 = 12.0f;
        RenderHelper.drawPanelBg(f9 + 5.0f, f10 + 5.0f, sidebarW(), f8 - 10.0f, f11, 0.0f, 0.0f, f11, f6);
        long l = System.nanoTime();
        float f12 = Math.min(0.1f, (float)(l - this.lastNs) / 1.0E9f);
        this.lastNs = l;
        this.themesRowT = 1.0f;
        this.renderCategoryPanel(f9 + 5.0f, f10 + 2.0f, f8, f6);

        this.renderSplitterRail(f9, f10, f8, f6, f12);

        float mainW = PANEL_W - contentInset();
        float rightRadius = (this.inspector.isOpen() || this.themesRenderer.getEditor().isOpen() || this.presetRenderer.isDrawerOpen()) ? 0.0f : f11;
        RenderHelper.drawPanelBg(f9 + contentXOff(), f10 + CONTENT_Y_OFFSET, mainW, CONTENT_HEIGHT, 0.0f, rightRadius, rightRadius, 0.0f, f6);
        this.updateCategoryCrossfade(f12);
        float f14 = f6 * this.categoryT;
        float f15 = this.screenAnim.isClosing() ? 1.0f : this.categoryT;
        boolean bl3 = !UI.guiCaptureActive() && !this.bindPopup.isVisible() && !this.settingsPopup.isVisible();
        this.moduleList.setAppearComposite(bl3);
        this.themesRenderer.setAppearComposite(bl3);
        if (f14 > 0.01f && this.contentCategory != null) {
            if (this.contentCategory == Category.THEMES) {
                this.themesRenderer.render(drawContext, f9, f10, PANEL_W, f14, f15, f12);
            } else if (this.contentCategory == Category.PRESETS) {
                this.presetRenderer.render(drawContext, f9 + contentXOff(), f10 + CONTENT_Y_OFFSET, PANEL_W - contentInset(), CONTENT_HEIGHT, f14);
            } else {
                List<Module> list = this.filteredModules(this.contentCategory);
                if (this.contentCategory == Category.PINNED && list.isEmpty() && !this.search.hasText()) {
                    this.renderEmptyPinned(drawContext, f9, f10, PANEL_W, f14);
                } else {
                    this.moduleList.render(drawContext, f9, f10, PANEL_W, f14, f15, f12, this.contentCategory, list);
                    if (list.isEmpty() && this.search.hasText()) {
                        this.renderNoResults(f9, f10, PANEL_W, f14);
                    }
                }
                if (this.contentCategory == Category.DISPLAY) renderCapitulation(f9, f10, PANEL_W, f14);
            }
        }
        float f16 = this.modulesHeaderAnim.getOutput().floatValue();
        boolean bl4 = bl = this.contentCategory != null && this.contentCategory != Category.THEMES
            && this.contentCategory != Category.PRESETS;
        if (bl && f6 > 0.01f && f16 > 0.004f) {
            f = f9 + contentXOff();
            float f17 = f10 + CONTENT_Y_OFFSET;
            float f18 = mainW;
            float f19 = HEADER_H;
            float f20 = f6 * f16;
            this.renderModuleHeaderPanel(f, f17, f18, f19, f20);
            Render2D.pushScissor(drawContext, f, f17, f18, f19);
            this.renderModuleHeader(drawContext, f9, f10, PANEL_W, f20, this.contentCategory, f12, 0.0f);
            Render2D.popScissor(drawContext);
        }

        if (this.inspector.isOpen()) {
            float inspX = InspectorRenderer.getX();
            float inspW = InspectorRenderer.INSPECTOR_WIDTH;
            Render2D.pushScissor(drawContext, inspX - 1.0f, f10 + CONTENT_Y_OFFSET, this.inspector.dockWidth() + 2.0f, CONTENT_HEIGHT);
            this.inspector.render(drawContext, inspX, f10 + CONTENT_Y_OFFSET, inspW, CONTENT_HEIGHT, f6, f12);
            Render2D.popScissor(drawContext);
        }

        if (this.themesRenderer.getEditor().isOpen()) {
            float dockX = f9 + PANEL_W;
            float dockW = this.themesRenderer.getEditor().dockWidth();
            Render2D.pushScissor(drawContext, dockX - 1.0f, f10 + CONTENT_Y_OFFSET, dockW + 2.0f, CONTENT_HEIGHT);
            this.themesRenderer.getEditor().renderInspector(drawContext, dockX, f10 + CONTENT_Y_OFFSET, activity.client.gui.custom.api.ui.theme.ThemeEditorRenderer.INSPECTOR_WIDTH, CONTENT_HEIGHT, f6, f12);
            Render2D.popScissor(drawContext);
        }

        if (this.presetRenderer.isDrawerOpen()) {
            float dockX = f9 + PANEL_W;
            float dockW = this.presetRenderer.dockWidth();
            Render2D.pushScissor(drawContext, dockX - 1.0f, f10 + CONTENT_Y_OFFSET, dockW + 2.0f, CONTENT_HEIGHT);
            this.presetRenderer.renderDrawer(drawContext, dockX, f10 + CONTENT_Y_OFFSET, activity.client.gui.custom.PresetRenderer.DRAWER_WIDTH, CONTENT_HEIGHT, f6, f12);
            Render2D.popScissor(drawContext);
        }
        boolean bl5 = this.settingsPopup.isVisible();
        boolean bl6 = this.bindPopup.isVisible();
        if (bl5 || bl6) {
            boolean bl7;
            boolean bl8 = this.settingsPopup.blurPhase() > 0.004f;
            boolean bl9 = this.bindPopup.blurPhase() > 0.004f;
            boolean bl10 = bl7 = !UI.guiCaptureActive() && bl5 && bl6 && bl9 && !bl8;
            if (!UI.guiCaptureActive()) {
                GuiRenderState guiRenderState = ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState();
                guiRenderState.createNewRootLayer();
                guiRenderState.applyBlur();
                UI.markPopupStratum((bl8 || bl9) && !bl7, bl8 || bl9);
            }
            if (bl5) {
                this.settingsPopup.render(drawContext, f6);
            }
            if (bl7) {
                UI.markPopupLayerCapture();
                GuiLayerBlurRenderer.markPopupBoundary(drawContext);
            }
            if (bl6) {
                this.bindPopup.render(drawContext, f6);
            }
        }
        if(UI.isOpen()&&!UI.guiCaptureActive()&&!this.bindPopup.isVisible()&&!this.settingsPopup.isVisible()&&!this.themesRenderer.getEditor().isOpen()&&!this.presetRenderer.isDrawerOpen()){
            if(!activity.client.gui.custom.CollectionDrawer.isOpen()) { if(this.inspector.isOpen())this.inspector.renderExplanation(drawContext, f6);else if(this.isModuleView())this.moduleList.renderExplanation(drawContext, f6); }
        }
        activity.client.gui.custom.NativeBindAssignment.render(drawContext, f6);
        this.renderPresetDropdown(drawContext, f6);
        drawContext.getMatrices().popMatrix();
        activity.client.gui.custom.CollectionDrawer.render(drawContext, (int) Position.mouseX(), (int) Position.mouseY(), 0);
        if (this.themesRenderer.getEditor().isOpen()) {
            this.themesRenderer.getEditor().renderOverlays(drawContext, f6);
        }
        if (!UI.guiCaptureActive()) {
            this.stagePopupBlur();
            this.stageCardBlur(f9, f10, f7);
        }
    }

    private void renderPresetDropdown(DrawContext drawContext, float alpha) {
        float dropT = this.presetDropdownAnim.getOutput().floatValue();
        if (dropT <= 0.005f) return;
        float totalAlpha = alpha * dropT;

        float dropW = Math.max(this.lastPresetBtnW + 12.0f, 90.0f);
        float dropX = this.lastPresetBtnX + (this.lastPresetBtnW - dropW) * 0.5f;
        float dropY = this.lastPresetBtnY + this.lastPresetBtnH + 3.0f;
        boolean isRu = activity.client.gui.custom.api.localization.LocalizationManager.getCurrentLanguage() == activity.client.gui.custom.api.localization.LocalizationManager.Language.RU;

        int totalEntries = this.cachedPresetEntries.size();
        int visibleCount = totalEntries == 0 ? 1 : Math.min(totalEntries, 6);
        float itemH = 15.0f;
        float footerH = 16.0f;
        float dropH = 4.0f + visibleCount * itemH + 1.0f + footerH + 4.0f;
        this.lastPresetDropX = dropX;
        this.lastPresetDropY = dropY;
        this.lastPresetDropW = dropW;
        this.lastPresetDropH = dropH;

        float animY = dropY - (1.0f - dropT) * 6.0f;
        float scale = 0.94f + 0.06f * dropT;
        float originX = dropX + dropW * 0.5f;
        float originY = dropY;

        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().translate(originX, originY);
        drawContext.getMatrices().scale(scale, scale);
        drawContext.getMatrices().translate(-originX, -originY);

        Render2D.glow(new BuiltGlow(dropX, animY, dropW, dropH, new float[]{6.0f, 6.0f, 6.0f, 6.0f}, ClientAccent.accent(110.0f * totalAlpha), 0.5f, 10.0f, totalAlpha));
        Render2D.rect(dropX, animY, dropW, dropH, 6.0f, ThemeManager.rgba(0x0e1117, 245.0f * totalAlpha));
        Render2D.outline(dropX, animY, dropW, dropH, 6.0f, 0.75f, ClientAccent.accent(150.0f * totalAlpha));

        float mx = Position.mouseX();
        float my = Position.mouseY();
        float curY = animY + 4.0f;

        if (totalEntries == 0) {
            String emptyLabel = isRu ? "Нет пресетов" : "No presets";
            Fonts.MONTSERRAT_MEDIUM.draw(emptyLabel, dropX + 8.0f, curY + 4.0f, 6.0f, UI.color(255, 255, 255, 120, totalAlpha));
            curY += itemH;
        } else {
            int startIdx = Math.max(0, Math.min(this.presetScrollOffset, Math.max(0, totalEntries - visibleCount)));
            for (int i = 0; i < visibleCount; i++) {
                int entryIdx = startIdx + i;
                if (entryIdx >= totalEntries) break;
                activity.client.config.preset.LocalPresets.Entry entry = this.cachedPresetEntries.get(entryIdx);
                boolean hoverItem = mx >= dropX + 2.0f && mx <= dropX + dropW - 2.0f && my >= curY && my <= curY + itemH;
                boolean isSelected = entry.name().equalsIgnoreCase(this.selectedPresetName);

                if (hoverItem || isSelected) {
                    Render2D.rect(dropX + 3.0f, curY + 1.0f, dropW - 6.0f, itemH - 2.0f, 3.5f,
                        isSelected ? ClientAccent.accent(50.0f * totalAlpha) : ThemeManager.rgba(0xFFFFFF, 18.0f * totalAlpha));
                    if (isSelected) {
                        Render2D.outline(dropX + 3.0f, curY + 1.0f, dropW - 6.0f, itemH - 2.0f, 3.5f, 0.6f, ClientAccent.accent(130.0f * totalAlpha));
                    }
                }

                float rowIconSize = 6.0f;
                int rowIconCol = isSelected ? ClientAccent.accentBright(255.0f * totalAlpha) : (hoverItem ? UI.color(255, 255, 255, 240, totalAlpha) : UI.color(255, 255, 255, 140, totalAlpha));
                Fonts.NV.msdf(isSelected ? NvIcons.CHECK : NvIcons.PROFILE, dropX + 7.0f, curY + (itemH - rowIconSize) * 0.5f, rowIconSize, rowIconCol);

                String name = entry.name();
                float maxNameW = dropW - 22.0f;
                float nameFontH = 6.0f;
                if (Fonts.MONTSERRAT_MEDIUM.width(name, nameFontH) > maxNameW && name.length() > 6) {
                    while (name.length() > 4 && Fonts.MONTSERRAT_MEDIUM.width(name + "…", nameFontH) > maxNameW) {
                        name = name.substring(0, name.length() - 1);
                    }
                    name += "…";
                }
                int nameCol = isSelected ? ClientAccent.accentBright(255.0f * totalAlpha) : (hoverItem ? UI.color(255, 255, 255, 255, totalAlpha) : UI.color(255, 255, 255, 190, totalAlpha));
                Fonts.MONTSERRAT_MEDIUM.draw(name, dropX + 16.5f, curY + (itemH - nameFontH) * 0.5f + 0.5f, nameFontH, nameCol);

                curY += itemH;
            }
        }

        Render2D.rect(dropX + 6.0f, curY, dropW - 12.0f, 0.6f, 0.3f, ThemeManager.rgba(0xFFFFFF, 20.0f * totalAlpha));
        curY += 1.0f;

        boolean hoverManage = mx >= dropX + 2.0f && mx <= dropX + dropW - 2.0f && my >= curY && my <= curY + footerH;
        if (hoverManage) {
            Render2D.rect(dropX + 3.0f, curY + 1.0f, dropW - 6.0f, footerH - 2.0f, 3.5f, ClientAccent.accent(35.0f * totalAlpha));
        }
        String manageText = isRu ? "Все пресеты" : "All presets";
        float mFontH = 6.0f;
        int mCol = hoverManage ? ClientAccent.accentBright(255.0f * totalAlpha) : ClientAccent.accentSoft(200.0f * totalAlpha);
        Fonts.NV.msdf(NvIcons.PROFILE, dropX + 7.0f, curY + (footerH - 6.0f) * 0.5f, 6.0f, mCol);
        Fonts.MONTSERRAT_MEDIUM.draw(manageText, dropX + 16.5f, curY + (footerH - mFontH) * 0.5f + 0.5f, mFontH, mCol);

        drawContext.getMatrices().popMatrix();
    }

    private static String layoutNormalize(String string) {
        StringBuilder stringBuilder = new StringBuilder(string.length());
        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            int n = RU_LAYOUT.indexOf(c);
            stringBuilder.append(n >= 0 ? EN_LAYOUT.charAt(n) : c);
        }
        return stringBuilder.toString();
    }

    private static String toRuLayout(String string) {
        StringBuilder stringBuilder = new StringBuilder(string.length());
        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            int n = EN_LAYOUT.indexOf(c);
            stringBuilder.append(n >= 0 ? RU_LAYOUT.charAt(n) : c);
        }
        return stringBuilder.toString();
    }


    public static void renderClosingPanelOverHud(DrawContext drawContext) {
        boolean bl;
        UI uI = INSTANCE;
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        WorldGuiCloseAnimation.updateFrame();
        if (GuiCapture.isBound(uI) && WorldGuiCloseAnimation.isReversing() && WorldGuiCloseAnimation.isFinished()) {
            WorldGuiCloseAnimation.cancel();
            GuiShatterAnimation.cancel();
            if (!uI.screenAnim.isClosing()) {
                uI.screenAnim.snapOpen();
            }
        }
        boolean bl2 = GuiCapture.isBound(uI);
        if (minecraftClient.currentScreen != null) {
            if (minecraftClient.currentScreen != uI) {
                if (bl2 && WorldGuiCloseAnimation.isActive()) {
                    WorldGuiCloseAnimation.cancel();
                }
                if (uI.screenAnim.isClosing()) {
                    uI.screenAnim.snapClosed();
                }
                pendingAfterClose = null;
            }
            return;
        }
        if (bl2 && WorldGuiCloseAnimation.isActive() && (minecraftClient.world == null || minecraftClient.options.hudHidden || WorldGuiCloseAnimation.isFinished() || WorldGuiCloseAnimation.surfaceChanged() || !GuiLayerBlurRenderer.available())) {
            WorldGuiCloseAnimation.cancel();
            if (uI.screenAnim.isClosing()) {
                uI.screenAnim.snapClosed();
                if (pendingAfterClose != null) {
                    Screen screen = pendingAfterClose;
                    pendingAfterClose = null;
                    minecraftClient.setScreen(screen);
                }
            }
            return;
        }
        boolean bl3 = bl2 && WorldGuiCloseAnimation.isActive();
        boolean bl4 = bl = uI.screenAnim.isClosing() && (bl3 || !uI.screenAnim.isCloseFinished());
        if (!bl) {
            if (uI.screenAnim.isClosing() && pendingAfterClose != null) {
                Screen screen = pendingAfterClose;
                pendingAfterClose = null;
                uI.screenAnim.snapClosed();
                minecraftClient.setScreen(screen);
            }
            return;
        }
        Render2D.beginFrame(drawContext);
        if (bl3) {
            BlurFramebuffer.beginWorldScope();
        }
        try {
            uI.renderPanel(drawContext);
        }
        finally {
            BlurFramebuffer.endWorldScope();
        }
        Render2D.flush();
        GuiLayerBlurRenderer.markPanelEnd(drawContext);
    }


    private Module moduleAtCursor() {
        if (!this.isModuleView()) {
            return null;
        }
        float f = PANEL_W;
        float f2 = panelX();
        float f3 = panelY();
        float f4 = Position.mouseX();
        float f5 = Position.mouseY();
        float f6 = f2 + contentXOff();
        float f7 = f3 + CONTENT_Y_OFFSET + HEADER_OFFSET;
        float f8 = f - contentInset();
        float f9 = moduleViewportHeight(this.contentCategory);
        if (f4 < f6 || f4 > f6 + f8 || f5 < f7 || f5 > f7 + f9) {
            return null;
        }
        List<Module> list = this.filteredModules(this.contentCategory);
        for (int i = 0; i < list.size(); ++i) {
            ModuleListRenderer.CardLayout layout = this.moduleList.getCardLayout(list, i, f6, f7, f8, f9, 1.0f, true);
            if (layout == null || !layout.visible) continue;
            if (layout.containsCard(f4, f5)) {
                return list.get(i);
            }
        }
        return null;
    }

    private Category categoryButtonAt(float f, float f2, float f3, float f4) {
        float f5;
        float f6 = f + 5.0f;
        float f7 = sidebarW();
        float f8 = f6 + 2.0f;
        float f9 = f6 + f7 - 2.0f;
        float f10 = f2 + 2.0f;
        float headerY = f10 + 3.0f;
        float catStartY = headerY + HEADER_H + 6.0f;
        for (int i = 0; i < MAIN_CATEGORIES.length; ++i) {
            f5 = catStartY + (float)i * (CAT_SUB_ROW_H + CAT_SUB_GAP);
            if (!(f3 >= f8) || !(f3 <= f9) || !(f4 >= f5) || !(f4 <= f5 + CAT_SUB_ROW_H)) continue;
            return MAIN_CATEGORIES[i];
        }
        float sidebarBottom = f10 + PANEL_H - 10.0f;
        float systemStartY = sidebarBottom - (float)SYSTEM_CATEGORIES.length * (CAT_OTHER_ROW_H + CAT_SUB_GAP) - 4.0f;
        for (int i = 0; i < SYSTEM_CATEGORIES.length; ++i) {
            float f14 = systemStartY + (float)i * (CAT_OTHER_ROW_H + CAT_SUB_GAP);
            if (!(f3 >= f8) || !(f3 <= f9) || !(f4 >= f14) || !(f4 <= f14 + CAT_OTHER_ROW_H)) continue;
            return SYSTEM_CATEGORIES[i];
        }
        return null;
    }

    public void selectCategoryFromWorkspace(Category target) {
        if (target != null && target != this.targetCategory) {
            Sounds.play("select_category");
            this.selectCategory(target);
        }
    }



}

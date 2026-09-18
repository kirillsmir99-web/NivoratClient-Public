package activity.client.gui;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.builder.SettingComponentFactory;
import activity.client.gui.component.ActivityButton;
import activity.client.gui.component.ActivityComponent;
import activity.client.gui.component.ActivityKeybindButton;
import activity.client.gui.component.ActivityLabel;
import activity.client.gui.component.ActivityPanel;
import activity.client.gui.component.ActivityToggle;
import activity.client.gui.layout.ScrollContainer;
import activity.client.gui.modal.ModalManager;
import activity.client.gui.overlay.OverlayManager;
import activity.client.gui.sheet.AboutModuleSheet;
import activity.client.gui.tab.ActivityTab;
import activity.client.gui.theme.ActivityMetrics;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.NivoratModule;
import activity.client.module.setting.Setting;
import activity.client.module.setting.SettingGroup;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Generic unified module settings view for NivoratClient.
 * Can be opened as a standalone Screen or used via static buildCard() to embed into tabs.
 */
public class ModuleSettingsView extends Screen {

    private final IModule module;
    private final Screen parent;
    private final OverlayManager overlayManager = new OverlayManager();
    private final ModalManager modalManager = new ModalManager(this.overlayManager);
    private ScrollContainer scrollContainer;

    public ModuleSettingsView(IModule module) {
        this(module, null);
    }

    public ModuleSettingsView(IModule module, Screen parent) {
        super(module != null ? module.getName() : Text.literal("Module Settings"));
        this.module = module;
        this.parent = parent;
    }

    public ModuleSettingsView(NivoratModule module) {
        this((IModule) module, null);
    }

    public ModuleSettingsView(NivoratModule module, Screen parent) {
        this((IModule) module, parent);
    }

    public IModule getModule() {
        return this.module;
    }

    public Screen getParentScreen() {
        return this.parent;
    }

    public ScrollContainer getScrollContainer() {
        return this.scrollContainer;
    }

    public ModuleMetadata getMetadata() {
        return this.module != null ? this.module.getMetadata() : null;
    }

    public OverlayManager getOverlayManager() {
        return this.overlayManager;
    }

    public ModalManager getModalManager() {
        return this.modalManager;
    }

    public void openAboutModuleSheet(String moduleId) {
        if (moduleId == null) return;
        this.overlayManager.open(new AboutModuleSheet(this.parent instanceof ActivityScreen act ? act : null, moduleId));
    }

    public void reloadView() {
        if (this.scrollContainer != null) {
            double scroll = this.scrollContainer.getScrollAmount();
            this.scrollContainer.clear();
            int cardW = Math.min(420, this.width - 40);
            int cardX = (this.width - cardW) / 2;
            int innerRowW = cardW - ActivityMetrics.PADDING_PANEL * 2;
            buildCard(null, null, this.scrollContainer, this.module, cardX, 0, cardW, innerRowW,
                    this::openAboutModuleSheet, this.overlayManager, this.modalManager, this::reloadView);
            this.scrollContainer.setScrollAmount(scroll);
        }
    }

    @Override
    protected void init() {
        super.init();
        if (this.module == null) return;
        this.overlayManager.clear();

        int cardW = Math.min(420, this.width - 40);
        int cardX = (this.width - cardW) / 2;
        int cardY = 35;
        int cardH = this.height - 75;
        int innerRowW = cardW - ActivityMetrics.PADDING_PANEL * 2;

        this.scrollContainer = new ScrollContainer(cardX, cardY, cardW, cardH);
        this.scrollContainer.setOverlayManager(this.overlayManager);

        // Back button at bottom center
        int btnW = 100;
        int btnH = 20;
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.back"),
                b -> close()
        ).dimensions((this.width - btnW) / 2, this.height - 30, btnW, btnH).build());

        // Build module card into scrollContainer
        buildCard(null, null, this.scrollContainer, this.module, cardX, 0, cardW, innerRowW,
                this::openAboutModuleSheet, this.overlayManager, this.modalManager, this::reloadView);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Subtle dark translucent background
        context.fill(0, 0, this.width, this.height, 0xD00A0D14);

        // Header Title
        context.drawTextWithShadow(
                this.textRenderer,
                this.title,
                (this.width - this.textRenderer.getWidth(this.title)) / 2,
                15,
                0xFFFFFF
        );

        if (this.scrollContainer != null) {
            this.scrollContainer.render(context, mouseX, mouseY, delta);
            this.scrollContainer.renderTooltips(context, this.textRenderer, mouseX, mouseY);
        }

        this.overlayManager.render(context, mouseX, mouseY, delta);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (this.overlayManager.mouseClicked(click, doubled)) {
            return true;
        }
        if (this.scrollContainer != null && this.scrollContainer.mouseClicked(click, doubled)) {
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (this.overlayManager.mouseReleased(click)) {
            return true;
        }
        if (this.scrollContainer != null && this.scrollContainer.mouseReleased(click)) {
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (this.overlayManager.mouseDragged(click, deltaX, deltaY)) {
            return true;
        }
        if (this.scrollContainer != null && this.scrollContainer.mouseDragged(click, deltaX, deltaY)) {
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.scrollContainer != null && this.scrollContainer.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (this.overlayManager.keyPressed(input)) {
            return true;
        }
        if (input.isEscape()) {
            close();
            return true;
        }
        if (this.scrollContainer != null && this.scrollContainer.keyPressed(input)) {
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (this.overlayManager.charTyped(input)) {
            return true;
        }
        if (this.scrollContainer != null && this.scrollContainer.charTyped(input)) {
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public void close() {
        this.overlayManager.clear();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    /**
     * Builds a standardized, consistent module settings card.
     * Supports both tab embedding and standalone scroll container.
     */
    public static int buildCard(
            ActivityTab tab,
            ActivityScreen screen,
            ScrollContainer container,
            IModule module,
            int cardX,
            int cardY,
            int cardW,
            int innerRowW
    ) {
        return buildCard(tab, screen, container, module, cardX, cardY, cardW, innerRowW,
                screen != null ? screen::openAboutModuleSheet : null,
                screen != null ? screen.getOverlayManager() : (container != null ? container.getOverlayManager() : null),
                screen != null ? screen.getModalManager() : null,
                screen != null ? screen::reloadCurrentTab : null);
    }

    /**
     * Builds a standardized, consistent module settings card with reactive visibility and overlay support.
     */
    public static int buildCard(
            ActivityTab tab,
            ActivityScreen screen,
            ScrollContainer container,
            IModule module,
            int cardX,
            int cardY,
            int cardW,
            int innerRowW,
            Consumer<String> onOpenAbout,
            OverlayManager overlayManager,
            ModalManager modalManager,
            Runnable onReload
    ) {
        if (module == null) return 0;

        Set<String> initialVisibleIds = new HashSet<>();
        List<Setting<?>> settings = new ArrayList<>();
        for (Setting<?> s : module.getSettings()) {
            if (s.isVisible()) {
                settings.add(s);
                initialVisibleIds.add(s.getId());
            }
        }
        // Strict group order: GENERAL (0) -> BEHAVIOR (1) -> EXTRA (2) -> ADVANCED (3)
        settings.sort(Comparator.comparingInt(s -> s.getGroup().ordinal()));

        int rowCount = 1 + settings.size(); // Row 1 is Title, About button, Keybind, Enable toggle
        int cardHeight = 22 + rowCount * (ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING) + 4;

        Text cardTitle = Text.translatable("activity.card." + module.getCategory().name().toLowerCase(Locale.ROOT) + "." + module.getId());
        ActivityPanel card;
        if (tab != null) {
            card = tab.createCard(container, cardX, cardY, cardW, cardHeight, cardTitle);
            tab.registerModuleCard(module.getId(), card);
        } else {
            card = new ActivityPanel(cardX, cardY, cardW, cardHeight, cardTitle);
            container.addChild(card);
        }

        int innerStartX = cardX + ActivityMetrics.PADDING_PANEL;
        int rowY = cardY + 22;

        // Metric calculations for consistent header alignment
        int toggleW = ActivityMetrics.TOGGLE_WIDTH;
        int gap = ActivityMetrics.COLUMN_GAP;
        int keybindBtnW = innerRowW < 200 ? 55 : (innerRowW < 240 ? 70 : 85);
        int aboutBtnW = 20;

        int toggleX = innerStartX + innerRowW - toggleW;
        int keybindX = toggleX - gap - keybindBtnW;
        int aboutX = keybindX - gap - aboutBtnW;
        int moduleLabelMaxW = Math.max(20, aboutX - innerStartX - 4);

        // ==========================================
        // 1. Header (Title, About, Keybind, Toggle)
        // ==========================================
        ActivityLabel labelTitle = new ActivityLabel(innerStartX, rowY + 3, module.getName());
        labelTitle.setMaxWidth(moduleLabelMaxW);
        if (module.getDescription() != null) {
            labelTitle.setTooltip(module.getDescription());
        }

        ActivityButton btnAbout = new ActivityButton(
                aboutX, rowY, aboutBtnW, ActivityMetrics.CONTROL_HEIGHT,
                Text.literal("ℹ"),
                btn -> {
                    if (screen != null) {
                        screen.openAboutModuleSheet(module.getId());
                    } else if (onOpenAbout != null) {
                        onOpenAbout.accept(module.getId());
                    }
                }
        );
        btnAbout.setTooltip(Text.translatable("activity.sidebar.about"));

        ActivityKeybindButton btnKey = new ActivityKeybindButton(
                keybindX, rowY, keybindBtnW, ActivityMetrics.CONTROL_HEIGHT,
                module.getKeybind(),
                kb -> {
                    module.getKeybind().copyFrom(kb);
                    ActivityConfig config = ActivityConfigManager.getConfig();
                    if (config != null) {
                        module.saveToConfig(config);
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        java.util.concurrent.atomic.AtomicReference<Set<String>> lastVisibleRef = new java.util.concurrent.atomic.AtomicReference<>(initialVisibleIds);
        Runnable onModified = () -> {
            ActivityConfig config = ActivityConfigManager.getConfig();
            if (config != null) {
                module.saveToConfig(config);
                ActivityConfigManager.markDirty();
            }
            Set<String> currentVisible = new HashSet<>();
            for (Setting<?> s : module.getSettings()) {
                if (s.isVisible()) currentVisible.add(s.getId());
            }
            if (!currentVisible.equals(lastVisibleRef.get())) {
                lastVisibleRef.set(currentVisible);
                if (screen != null) {
                    screen.reloadCurrentTab();
                } else if (onReload != null) {
                    onReload.run();
                }
            }
        };

        for (Setting<?> s : module.getSettings()) {
            s.addListener(v -> onModified.run());
        }

        ActivityToggle toggleEnabled = new ActivityToggle(
                toggleX, rowY,
                module.isEnabled(),
                state -> {
                    module.setEnabled(state);
                    onModified.run();
                }
        );

        if (tab != null) {
            tab.addControl(container, labelTitle);
            tab.addControl(container, btnAbout);
            tab.addControl(container, btnKey);
            tab.addControl(container, toggleEnabled);
        } else {
            container.addChild(labelTitle);
            container.addChild(btnAbout);
            container.addChild(btnKey);
            container.addChild(toggleEnabled);
        }

        // ==========================================
        // 2..5 Grouped Settings (via SettingComponentFactory)
        // ==========================================
        for (Setting<?> s : settings) {
            rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
            SettingComponentFactory.SettingRow row = SettingComponentFactory.createRow(
                    s, innerStartX, rowY, innerRowW, overlayManager, modalManager, onModified
            );
            if (row != null) {
                if (row.label() != null) {
                    if (tab != null) tab.addControl(container, row.label());
                    else container.addChild(row.label());
                }
                if (row.control() != null) {
                    if (tab != null) tab.addControl(container, row.control());
                    else container.addChild(row.control());
                }
            }
        }

        // ==========================================
        // 6. Custom Section Extension
        // ==========================================
        if (module.hasCustomSection()) {
            rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
            int customHeight = module.buildCustomSection(tab, screen, container, innerStartX, rowY, innerRowW);
            cardHeight += customHeight;
            card.setHeight(cardHeight);
        }

        return cardHeight;
    }
}

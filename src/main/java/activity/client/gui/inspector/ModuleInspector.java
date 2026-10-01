package activity.client.gui.inspector;

import activity.client.gui.ActivityScreen;
import activity.client.gui.view.ModuleSettingsView;
import activity.client.gui.layout.ScrollContainer;
import activity.client.gui.overlay.Overlay;
import activity.client.gui.theme.ActivityColors;
import activity.client.gui.theme.ActivityMetrics;
import activity.client.gui.render.ActivityGuiRenderer;
import activity.client.gui.icon.ActivityIcon;
import activity.client.gui.icon.ActivityIconRenderer;
import activity.client.module.api.IModule;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;

public final class ModuleInspector implements Overlay {
    private final java.util.function.Supplier<activity.client.gui.layout.WindowLayout> layoutProvider;
    private final activity.client.gui.overlay.OverlayManager overlays;
    private final activity.client.gui.modal.ModalManager modals;
    private final java.util.function.Consumer<String> openAbout;
    private final IModule module;
    private ScrollContainer container;
    private int x, y, width, height;
    private boolean closed;
    public ModuleInspector(ActivityScreen screen, IModule module) {
        this(module, screen::computeLayout, screen.getOverlayManager(), screen.getModalManager(), screen::openAboutModuleSheet);
    }
    public ModuleInspector(IModule module,
            java.util.function.Supplier<activity.client.gui.layout.WindowLayout> layoutProvider,
            activity.client.gui.overlay.OverlayManager overlays,
            activity.client.gui.modal.ModalManager modals,
            java.util.function.Consumer<String> openAbout) {
        this.module = module;
        this.layoutProvider = layoutProvider;
        this.overlays = overlays;
        this.modals = modals;
        this.openAbout = openAbout;
    }
    @Override public void onOpen() { rebuild(); activity.client.gui.sound.SoundManager.playDropdownOpen(); }
    private void rebuild() {
        double scroll = container != null ? container.getScrollAmount() : 0;
        if (container != null) container.clearChildren();
        var layout = layoutProvider.get();
        width = Math.min(310, Math.max(90, layout.windowWidth - 16));
        height = layout.windowHeight - layout.headerHeight - 8;
        x = layout.windowX + layout.windowWidth - width - 4;
        y = layout.headerY + layout.headerHeight + 4;
        container = new ScrollContainer(x + 6, y + 24, width - 12, Math.max(20, height - 30));
        container.setOverlayManager(overlays);
        int cardW = width - 18;
        ModuleSettingsView.buildCard(null, null, container, module, x + 6, y + 24, cardW,
            cardW - ActivityMetrics.PADDING_PANEL * 2, openAbout,
            overlays, modals, this::rebuild);
        container.setScrollAmount(scroll);
    }
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        ActivityGuiRenderer.drawPanel(context, x, y, width, height, ActivityColors.WINDOW_BACKGROUND,
            ActivityColors.BORDER_HOVER, true);
        ActivityIconRenderer.draw(context, ActivityIcon.CLOSE, x + width - 20, y + 6, 12, ActivityColors.TEXT_PRIMARY, 1);
        container.render(context, mouseX, mouseY, delta);
        container.renderTooltips(context, net.minecraft.client.MinecraftClient.getInstance().textRenderer, mouseX, mouseY);
    }
    @Override public boolean contains(double mx, double my) { return mx >= x && mx < x + width && my >= y && my < y + height; }
    @Override public boolean mouseClicked(Click click, boolean doubled) {
        if (click.y() < y + 24 && click.x() >= x + width - 26) { close(); return true; }
        return container.mouseClicked(click, doubled);
    }
    @Override public boolean mouseReleased(Click c) { return container.mouseReleased(c); }
    @Override public boolean mouseDragged(Click c, double dx, double dy) { return container.mouseDragged(c, dx, dy); }
    @Override public boolean mouseScrolled(double mx, double my, double h, double v) { return container.mouseScrolled(mx, my, h, v); }
    @Override public boolean keyPressed(KeyInput k) { return container.keyPressed(k); }
    @Override public boolean keyReleased(KeyInput k) { return container.keyReleased(k); }
    @Override public boolean charTyped(CharInput c) { return container.charTyped(c); }
    @Override public void close() {
        if (closed) return;
        closed = true;
        if (container != null) container.clearChildren();
        activity.client.gui.sound.SoundManager.playDropdownClose();
    }
    @Override public boolean isClosed() { return closed; }
}

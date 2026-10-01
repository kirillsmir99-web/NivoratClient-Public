package activity.client.gui.component;

import activity.client.gui.ActivityScreen;
import activity.client.gui.font.UiTextRenderer;
import activity.client.gui.icon.ActivityIcon;
import activity.client.gui.icon.ActivityIconRenderer;
import activity.client.gui.render.ActivityGuiRenderer;
import activity.client.gui.theme.ActivityColors;
import activity.client.module.api.IModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class ModulePreviewCard extends ActivityPanel {
    public static final int HEIGHT = 42;
    private final IModule module;
    private final ActivityScreen screen;
    private Text displayName;
    private Text description;
    private int cachedWidth = -1;
    private float highlightRemaining;

    public ModulePreviewCard(ActivityScreen screen, IModule module, int x, int y, int width) {
        super(x, y, width, HEIGHT);
        this.screen = screen;
        this.module = module;
        setTooltip(Text.translatable("activity.card.open_settings"));
    }

    @Override public void flashHighlight() {
        highlightRemaining = activity.client.gui.animation.AnimationClock.isAnimationsEnabled() ? 1.5f : 0;
    }
    @Override public boolean isHighlighted() { return highlightRemaining > 0; }
    @Override public void onFontChanged() { cachedWidth = -1; }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!this.visible) return;
        highlightRemaining = Math.max(0, highlightRemaining - activity.client.gui.animation.AnimationClock.getDeltaTime());
        var tr = MinecraftClient.getInstance().textRenderer;
        boolean hover = isMouseOver(mouseX, mouseY);
        var config = activity.client.config.ActivityConfigManager.getConfig();
        double opacity = config != null ? config.panelOpacity : 65;
        activity.client.gui.render.RoundedPanelRenderer.draw(context, x, y, width, height,
            ActivityColors.scaleAlphaPercent(ActivityColors.PANEL_INNER_BG, opacity * alpha));
        activity.client.gui.render.RoundedPanelRenderer.drawBorder(context, x, y, width, height,
            ActivityColors.scaleAlpha(hover ? ActivityColors.BORDER_HOVER : ActivityColors.BORDER_CARD, alpha));
        if (cachedWidth != width) {
            displayName = Text.literal(UiTextRenderer.trimToWidth(tr, module.getName().getString(), Math.max(12, width - 65)));
            description = Text.literal(UiTextRenderer.trimToWidth(tr,
                module.getDescription() != null ? module.getDescription().getString() : "", Math.max(12, width - 20)));
            cachedWidth = width;
        }
        UiTextRenderer.drawTextWithShadow(context, tr, displayName, x + 10, y + 9,
            ActivityColors.scaleAlpha(ActivityColors.TEXT_PRIMARY, alpha));
        UiTextRenderer.drawTextWithShadow(context, tr, description, x + 10, y + 25,
            ActivityColors.scaleAlpha(ActivityColors.TEXT_SECONDARY, alpha));
        int color = module.isEnabled() ? ActivityColors.ACCENT_PRIMARY : ActivityColors.TEXT_DISABLED;
        activity.client.gui.render.RoundedPanelRenderer.draw(context, x + width - 45, y + 10, 18, 10,
            ActivityColors.scaleAlpha(color, alpha));
        ActivityGuiRenderer.fill(context, x + width - (module.isEnabled() ? 34 : 43), y + 12, 6, 6,
            ActivityColors.scaleAlpha(0xFFFFFFFF, alpha));
        ActivityIconRenderer.draw(context, ActivityIcon.SETTINGS, x + width - 22, y + 8, 12,
            ActivityColors.TEXT_SECONDARY, alpha);
        if (isHighlighted()) ActivityGuiRenderer.drawBorder(context, x, y, width, height,
            ActivityColors.scaleAlpha(ActivityColors.ACCENT_LIGHT, alpha));
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (!visible || !enabled || !isMouseOver(click.x(), click.y())) return false;
        if (click.button() == 1 || (click.button() == 0 && click.x() >= x + width - 24)) {
            screen.openModuleInspector(module.getId());
            return true;
        }
        if (click.button() == 0) {
            module.setEnabled(!module.isEnabled());
            var config = activity.client.config.ActivityConfigManager.getConfig();
            if (config != null) {
                module.saveToConfig(config);
                activity.client.config.ActivityConfigManager.markDirty();
            }
            activity.client.gui.sound.SoundManager.playToggle(module.isEnabled());
            return true;
        }
        return false;
    }
}

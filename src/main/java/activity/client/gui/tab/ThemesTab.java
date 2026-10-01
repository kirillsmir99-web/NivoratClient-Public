package activity.client.gui.tab;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.ActivityScreen;
import activity.client.gui.component.ActivityButton;
import activity.client.gui.component.ActivityPanel;
import activity.client.gui.icon.ActivityIcon;
import activity.client.gui.layout.ScrollContainer;
import activity.client.gui.theme.ActivityColors;
import activity.client.gui.theme.ThemePreset;
import activity.client.gui.render.ActivityGuiRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class ThemesTab extends ActivityTab {
    public static final int TAB_INDEX = 16;
    public ThemesTab() { super("themes", Text.translatable("activity.tab.themes"), ActivityIcon.GLASS); }
    @Override public Text getSubtitle() { return Text.translatable("activity.themes.subtitle"); }
    @Override public void resetDefaults() { select(ThemePreset.CLIENT); }
    @Override public void loadFromConfig(ActivityConfig config) { ActivityColors.apply(ThemePreset.fromId(config.guiTheme)); }
    @Override public void saveToConfig(ActivityConfig config) { }
    private static void select(ThemePreset preset) {
        ActivityConfig config = ActivityConfigManager.getConfig();
        if (config == null) return;
        config.guiTheme = preset.id();
        ActivityColors.apply(preset);
        ActivityConfigManager.markDirty();
    }
    @Override public void buildTab(ActivityScreen screen, ScrollContainer container, int x, int y, int width) {
        clearComponents();
        var config = ActivityConfigManager.getConfig();
        ThemePreset selected = ThemePreset.fromId(config != null ? config.guiTheme : null);
        String group = "";
        int col = 0;
        int columns = width >= 280 ? 2 : 1;
        int cardW = (width - (columns - 1) * 8) / columns;
        for (ThemePreset preset : ThemePreset.values()) {
            if (!group.equals(preset.group())) {
                if (col != 0) { y += 64; col = 0; }
                group = preset.group();
                var heading = new activity.client.gui.component.ActivityLabel(x, y,
                    Text.translatable("activity.theme_group." + group.toLowerCase(java.util.Locale.ROOT)));
                addControl(container, heading);
                y += 18;
            }
            int cardX = x + col * (cardW + 8);
            ActivityPanel preview = new ActivityPanel(cardX, y, cardW, 56) {
                @Override public void render(DrawContext context, int mx, int my, float delta) {
                    ActivityGuiRenderer.drawPanel(context, this.x, this.y, this.width, this.height,
                        ActivityColors.scaleAlpha(ActivityColors.PANEL_INNER_BG, this.alpha),
                        ActivityColors.scaleAlpha(selected == preset ? preset.accent() : ActivityColors.BORDER_CARD, this.alpha), true);
                    int stops = preset.colorCount();
                    for (int i = 0; i < stops; i++) {
                        int left = 8 + i * (this.width - 16) / stops;
                        int right = 8 + (i + 1) * (this.width - 16) / stops;
                        ActivityGuiRenderer.fill(context, this.x + left, this.y + 8, right - left, 9,
                            ActivityColors.scaleAlpha(preset.color(i), this.alpha));
                    }
                }
            };
            container.addChild(preview);
            addComponent(preview);
            var button = new ActivityButton(cardX + 6, y + 25, cardW - 12, 23,
                Text.literal(preset.displayName()), selected == preset ? ActivityButton.Variant.PRIMARY : ActivityButton.Variant.SECONDARY,
                ignored -> { select(preset); screen.reloadCurrentTab(); });
            addControl(container, button);
            if (++col == columns) { col = 0; y += 64; }
        }
    }
}

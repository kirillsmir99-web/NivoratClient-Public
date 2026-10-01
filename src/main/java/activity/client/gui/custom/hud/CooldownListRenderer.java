package activity.client.gui.custom.hud;

import activity.client.gui.custom.CustomRender;
import activity.client.gui.custom.UnifiedHudRender;
import activity.client.gui.custom.VisualText;
import activity.client.gui.custom.api.ui.theme.ClientAccent;
import activity.client.gui.custom.api.ui.theme.ThemeManager;
import activity.client.gui.custom.utils.animations.Easing;
import activity.client.gui.custom.utils.animations.SmoothAnimation;
import activity.client.gui.custom.utils.render.fonts.Fonts;
import activity.client.gui.custom.utils.render.others.RectUtil;
import activity.client.gui.custom.utils.render.render2d.Render2D;
import activity.client.i18n.LocalizationService;
import activity.client.module.service.CooldownTrackerService.CooldownEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CooldownListRenderer {
    private static final float ROW_SIZE = 6.5f;
    private static final Easing ROW_EASING = value -> value * value * (3.0 - 2.0 * value);
    private final Map<Item, RowState> rows = new LinkedHashMap<>();
    private final SmoothAnimation width = new SmoothAnimation();
    private String cachedLanguage;
    private String title;

    public boolean hasRows() {
        return !rows.isEmpty();
    }

    public void clear() {
        rows.clear();
        width.set(0);
    }

    public static String itemName(CooldownEntry entry) {
        if (entry.item == null) return "";
        String key = "activity.hud.cooldown.item." + Registries.ITEM.getId(entry.item).getPath();
        return VisualText.safe(LocalizationService.get(key, entry.item.getName().getString()), VisualText.language());
    }

    public static int cellWidth(List<CooldownEntry> entries) {
        float width = 70f;
        for (var entry : entries) {
            String name = itemName(entry);
            float nameWidth = MinecraftClient.getInstance() == null ? name.length() * 4f : Fonts.MONTSERRAT_MEDIUM.width(name, ROW_SIZE);
            width = Math.max(width, 13f + nameWidth + 10f + timeWidth(entry.getFormattedRemaining()));
        }
        return (int) Math.ceil(width);
    }

    private static float timeWidth(String time) {
        return MinecraftClient.getInstance() == null ? time.length() * 4f : Fonts.MONTSERRAT_MEDIUM.width(time, ROW_SIZE);
    }

    public void render(DrawContext context, List<CooldownEntry> entries, int x, int y, boolean vertical) {
        String language = VisualText.language();
        if (!language.equals(cachedLanguage)) {
            cachedLanguage = language;
            title = LocalizationService.get("activity.hud.cooldown.title", "Cooldowns");
            for (var row : rows.values()) row.name = itemName(row.entry);
        }
        for (var row : rows.values()) row.active = false;
        for (var entry : entries) {
            var row = rows.computeIfAbsent(entry.item, item -> new RowState());
            if (row.entry != entry || row.name == null) row.name = itemName(entry);
            row.entry = entry;
            row.active = true;
            row.time = entry.getFormattedRemaining();
        }
        var iterator = rows.values().iterator();
        while (iterator.hasNext()) {
            var row = iterator.next();
            if (row.active != row.lastActive) {
                row.animation.run(row.active ? 1.0 : 0.0, row.active ? 0.22 : 0.18, ROW_EASING, false);
                row.lastActive = row.active;
            }
            row.animation.update();
            if (!row.active && row.animation.get() <= 0.001f) iterator.remove();
        }
        if (rows.isEmpty()) return;
        int cellWidth = 70;
        float progress = 0f;
        for (var row : rows.values()) {
            cellWidth = Math.max(cellWidth, (int) Math.ceil(13f + Fonts.MONTSERRAT_MEDIUM.width(row.name, ROW_SIZE) + 10f + timeWidth(row.time)));
            progress += Math.clamp(row.animation.get(), 0f, 1f);
        }
        var layout = CooldownLayout.of(rows.size(), cellWidth, vertical, context.getScaledWindowWidth() - Math.max(2, x) - 2);
        if (width.get() <= 0.01f) width.set(layout.width());
        if (Math.abs(width.getToValue() - layout.width()) > 0.25) width.run(layout.width(), 0.24, ROW_EASING, false);
        width.update();
        float panelWidth = Math.max(layout.width(), width.get());
        float contentHeight = vertical ? progress * CooldownLayout.ROW_HEIGHT : layout.rows() * CooldownLayout.ROW_HEIGHT;
        float panelHeight = CooldownLayout.CONTENT_TOP + contentHeight + 5;
        float alpha = Math.clamp(progress, 0f, 1f);
        try (var frame = UnifiedHudRender.begin(context)) {
            RectUtil.drawClientRectFixedRadius(x, y, panelWidth, panelHeight, 7f, alpha, 0);
            Fonts.MONTSERRAT_MEDIUM.draw(title, x + 9, y + 7, 8.2f, color(0xffffff, alpha * .95f));
            HudIcons.draw(HudIcons.TIMER, x + panelWidth - 18, y + 7, 8, ClientAccent.accentBright(220 * alpha));
            Render2D.rect(x + 9, y + CooldownLayout.HEADER_HEIGHT, panelWidth - 18, .5f, 0f, ThemeManager.rgba(0xffffff, 28 * alpha));
            Render2D.rect(x + 9, y + CooldownLayout.HEADER_HEIGHT, 12f * alpha, .7f, .3f, ClientAccent.accentBright(210 * alpha));
            float rowY = y + CooldownLayout.CONTENT_TOP;
            int index = 0;
            for (var row : rows.values()) {
                float visible = Math.clamp(row.animation.get(), 0f, 1f);
                if (visible <= .001f) continue;
                int column = vertical ? 0 : index % layout.columns();
                if (!vertical) rowY = y + CooldownLayout.CONTENT_TOP + index / layout.columns() * CooldownLayout.ROW_HEIGHT;
                float rowX = x + 9 + column * (layout.cellWidth() + CooldownLayout.COLUMN_GAP);
                float valueProgress = Math.clamp((visible - .12f) / .88f, 0f, 1f);
                HudIcons.draw(HudIcons.slot(row.entry.item), rowX - 7 * (1 - visible), rowY + 2, 9, ClientAccent.accentSoft(230 * alpha * visible));
                float valueWidth = timeWidth(row.time);
                float valueX = vertical ? x + panelWidth - 9 - valueWidth : rowX + layout.cellWidth() - valueWidth;
                Fonts.MONTSERRAT_MEDIUM.draw(CustomRender.fit(row.name, valueX - rowX - 22, ROW_SIZE), rowX + 13 - 7 * (1 - visible), rowY + 2, ROW_SIZE, color(0xd5dbe5, alpha * visible));
                int valueColor = row.entry.getRemainingSeconds() <= 1.5f ? 0xff7b86 : 0xd5dbe5;
                Fonts.MONTSERRAT_MEDIUM.draw(row.time, valueX + 7 * (1 - valueProgress), rowY + 2, ROW_SIZE, color(valueColor, alpha * valueProgress));
                if (vertical) rowY += CooldownLayout.ROW_HEIGHT * visible;
                index++;
            }
        }
    }

    private static int color(int rgb, float alpha) {
        return Math.round(Math.clamp(alpha, 0f, 1f) * 255) << 24 | rgb;
    }

    private static final class RowState {
        private final SmoothAnimation animation = new SmoothAnimation();
        private CooldownEntry entry;
        private String name;
        private String time;
        private boolean active;
        private boolean lastActive;
    }
}

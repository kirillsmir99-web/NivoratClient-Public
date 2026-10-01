package activity.client.gui.hud;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.module.service.CooldownTrackerService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import activity.client.gui.custom.hud.CooldownLayout;
import activity.client.gui.custom.hud.CooldownListRenderer;
import net.minecraft.item.Items;

import java.util.List;

public final class CooldownHudOverlay {

    public static final int ITEM_SIZE = 16;
    public static final int ITEM_HEIGHT = CooldownLayout.ROW_HEIGHT;
    public static final int GAP = CooldownLayout.COLUMN_GAP;
    private static final CooldownListRenderer LIVE = new CooldownListRenderer();
    private static final CooldownListRenderer PREVIEW = new CooldownListRenderer();

    private CooldownHudOverlay() {}

    public static int getDefaultX(int screenWidth, int totalWidth) {
        return Math.max(2, (screenWidth - totalWidth) / 2);
    }

    public static int getDefaultY(int screenHeight, int totalHeight) {
        return Math.max(2, screenHeight - 85);
    }

    public static int getEffectiveX(int screenWidth, int totalWidth) {
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null && c.cooldownHudCustomX >= 0) {
            return Math.max(2, Math.min(screenWidth - totalWidth - 2, c.cooldownHudCustomX));
        }
        return getDefaultX(screenWidth, totalWidth);
    }

    public static int getEffectiveY(int screenHeight, int totalHeight) {
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null && c.cooldownHudCustomY >= 0) {
            return Math.max(2, Math.min(screenHeight - totalHeight - 2, c.cooldownHudCustomY));
        }
        return getDefaultY(screenHeight, totalHeight);
    }

    public static int getDefaultX(int screenWidth) {
        return getDefaultX(screenWidth, 60);
    }

    public static int getDefaultY(int screenHeight) {
        return getDefaultY(screenHeight, ITEM_HEIGHT);
    }

    public static int getEffectiveX(int screenWidth) {
        return getEffectiveX(screenWidth, 60);
    }

    public static int getEffectiveY(int screenHeight) {
        return getEffectiveY(screenHeight, ITEM_HEIGHT);
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) {
            LIVE.clear();
            return;
        }

        ActivityConfig config = ActivityConfigManager.getConfig();
        if (config == null || !config.cooldownHudEnabled) {
            LIVE.clear();
            return;
        }

        List<CooldownTrackerService.CooldownEntry> entries = CooldownTrackerService.getActiveEntries();
        if (entries.isEmpty() && !LIVE.hasRows()) {
            return;
        }

        boolean vertical = config.cooldownHudVertical;
        int totalW = calculateTotalWidth(mc.textRenderer, entries, vertical);
        int totalH = calculateTotalHeight(entries, vertical);
        int startX = getEffectiveX(context.getScaledWindowWidth(), totalW);
        int startY = getEffectiveY(context.getScaledWindowHeight(), totalH);

        LIVE.render(context, entries, startX, startY, vertical);
    }

    public static void renderCooldownList(DrawContext context, TextRenderer textRenderer,
                                          List<CooldownTrackerService.CooldownEntry> entries,
                                          int startX, int startY, boolean vertical) {
        if (entries == null || entries.isEmpty() || textRenderer == null) {
            return;
        }

        PREVIEW.render(context, entries, startX, startY, vertical);
    }

    public static int getCooldownColor(float remainingSeconds) {
        if (remainingSeconds > 3.0f) {
            return 0xFFFFFF;
        } else if (remainingSeconds > 1.5f) {
            return 0xFFFF55;
        } else {
            return 0xFF5555;
        }
    }

    private static CooldownLayout layout(List<CooldownTrackerService.CooldownEntry> entries, boolean vertical) {
        var mc = MinecraftClient.getInstance();
        int available = mc == null || mc.getWindow() == null ? 320 : mc.getWindow().getScaledWidth() - 4;
        List<CooldownTrackerService.CooldownEntry> safe = entries == null ? List.of() : entries;
        return CooldownLayout.of(safe.size(), CooldownListRenderer.cellWidth(safe), vertical, available);
    }

    public static int calculateTotalWidth(TextRenderer textRenderer, List<CooldownTrackerService.CooldownEntry> entries, boolean vertical) {
        return layout(entries, vertical).width();
    }

    public static int calculateTotalHeight(List<CooldownTrackerService.CooldownEntry> entries, boolean vertical) {
        return layout(entries, vertical).height();
    }

    public static List<CooldownTrackerService.CooldownEntry> getMockEntriesForPreview() {
        return List.of(
                new CooldownTrackerService.CooldownEntry(Items.ENDER_PEARL, 290),
                new CooldownTrackerService.CooldownEntry(Items.TNT_MINECART, 3200),
                new CooldownTrackerService.CooldownEntry(Items.WIND_CHARGE, 160),
                new CooldownTrackerService.CooldownEntry(Items.BOW, 60),
                new CooldownTrackerService.CooldownEntry(Items.GOLDEN_APPLE, 84),
                new CooldownTrackerService.CooldownEntry(Items.CHORUS_FRUIT, 42),
                new CooldownTrackerService.CooldownEntry(Items.COOKED_BEEF, 120),
                new CooldownTrackerService.CooldownEntry(Items.SHIELD, 100),
                new CooldownTrackerService.CooldownEntry(Items.MACE, 20)
        );
    }
}

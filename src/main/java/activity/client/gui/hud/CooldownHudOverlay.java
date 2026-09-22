package activity.client.gui.hud;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.module.service.CooldownTrackerService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

public final class CooldownHudOverlay {

    public static final int ITEM_SIZE = 16;
    public static final int ITEM_HEIGHT = 18;
    public static final int GAP = 6;

    private CooldownHudOverlay() {}

    public static int getDefaultX(int screenWidth) {
        return screenWidth / 2 + 10;
    }

    public static int getDefaultY(int screenHeight) {
        return screenHeight - 44;
    }

    public static int getEffectiveX(int screenWidth) {
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null && c.cooldownHudCustomX >= 0) {
            return Math.max(2, Math.min(screenWidth - 50, c.cooldownHudCustomX));
        }
        return getDefaultX(screenWidth);
    }

    public static int getEffectiveY(int screenHeight) {
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null && c.cooldownHudCustomY >= 0) {
            return Math.max(2, Math.min(screenHeight - 20, c.cooldownHudCustomY));
        }
        return getDefaultY(screenHeight);
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) {
            return;
        }

        ActivityConfig config = ActivityConfigManager.getConfig();
        if (config == null || !config.cooldownHudEnabled) {
            return;
        }

        List<CooldownTrackerService.CooldownEntry> entries = CooldownTrackerService.getActiveEntries();
        if (entries.isEmpty()) {
            return;
        }

        int startX = getEffectiveX(context.getScaledWindowWidth());
        int startY = getEffectiveY(context.getScaledWindowHeight());
        boolean vertical = config.cooldownHudVertical;

        renderCooldownList(context, mc.textRenderer, entries, startX, startY, vertical);
    }

    public static void renderCooldownList(DrawContext context, TextRenderer textRenderer,
                                          List<CooldownTrackerService.CooldownEntry> entries,
                                          int startX, int startY, boolean vertical) {
        if (entries == null || entries.isEmpty() || textRenderer == null) {
            return;
        }

        int curX = startX;
        int curY = startY;

        for (CooldownTrackerService.CooldownEntry entry : entries) {
            context.drawItem(entry.iconStack, curX, curY);

            String timeText = entry.getFormattedRemaining();
            int color = getCooldownColor(entry.getRemainingSeconds());

            int textX = curX + ITEM_SIZE + 2;
            int textY = curY + (ITEM_HEIGHT - textRenderer.fontHeight) / 2;
            context.drawTextWithShadow(textRenderer, timeText, textX, textY, color);

            int textWidth = textRenderer.getWidth(timeText);
            int elementWidth = ITEM_SIZE + 2 + textWidth;

            if (vertical) {
                curY += ITEM_HEIGHT + GAP;
            } else {
                curX += elementWidth + GAP;
            }
        }
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

    public static int calculateTotalWidth(TextRenderer textRenderer, List<CooldownTrackerService.CooldownEntry> entries, boolean vertical) {
        if (entries == null || entries.isEmpty() || textRenderer == null) {
            return 40;
        }
        if (vertical) {
            int maxW = 0;
            for (CooldownTrackerService.CooldownEntry e : entries) {
                int w = ITEM_SIZE + 2 + textRenderer.getWidth(e.getFormattedRemaining());
                if (w > maxW) maxW = w;
            }
            return maxW;
        } else {
            int total = 0;
            for (int i = 0; i < entries.size(); i++) {
                total += ITEM_SIZE + 2 + textRenderer.getWidth(entries.get(i).getFormattedRemaining());
                if (i < entries.size() - 1) total += GAP;
            }
            return total;
        }
    }

    public static int calculateTotalHeight(List<CooldownTrackerService.CooldownEntry> entries, boolean vertical) {
        if (entries == null || entries.isEmpty()) {
            return ITEM_HEIGHT;
        }
        if (vertical) {
            return entries.size() * ITEM_HEIGHT + (entries.size() - 1) * GAP;
        } else {
            return ITEM_HEIGHT;
        }
    }

    public static List<CooldownTrackerService.CooldownEntry> getMockEntriesForPreview() {
        return List.of(
                new CooldownTrackerService.CooldownEntry(Items.ENDER_PEARL, 290),
                new CooldownTrackerService.CooldownEntry(Items.TNT_MINECART, 3200),
                new CooldownTrackerService.CooldownEntry(Items.WIND_CHARGE, 84)
        );
    }
}

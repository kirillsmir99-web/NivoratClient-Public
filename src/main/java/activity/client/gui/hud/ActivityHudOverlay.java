package activity.client.gui.hud;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ActivityHudOverlay {

    public static final String DEFAULT_TITLE = "PulseHUD";
    private static String cachedTitle = null;
    private static Text cachedTitleText = null;
    public static final int PILL_HEIGHT = 14;
    public static final int MODULE_ROW_HEIGHT = 12;
    public static final int ROW_GAP = 2;
    public static final int PADDING_H = 6;

    private ActivityHudOverlay() {}

    public static Text resolveTitleText(ActivityConfig config) {
        String str = (config != null && config.customTitle != null && !config.customTitle.isBlank() && !"Activity HUD".equals(config.customTitle))
                ? config.customTitle
                : DEFAULT_TITLE;
        if (cachedTitleText == null || !str.equals(cachedTitle)) {
            cachedTitle = str;
            cachedTitleText = Text.literal(str);
        }
        return cachedTitleText;
    }

    public static List<IModule> getActiveModules() {
        List<IModule> active = new ArrayList<>();
        try {
            for (IModule mod : ModuleRegistry.getAll()) {
                if (mod != null && mod.isEnabled()) {
                    active.add(mod);
                }
            }
        } catch (Throwable ignored) {}
        return active;
    }

    public static int getWatermarkWidth(MinecraftClient client, ActivityConfig config) {
        if (client == null || client.textRenderer == null) return 80;
        Text titleText = resolveTitleText(config);
        return client.textRenderer.getWidth(titleText) + 17;
    }

    public static int getTotalWidth(MinecraftClient client, ActivityConfig config) {
        int maxW = getWatermarkWidth(client, config);
        if (config != null && config.hudShowActiveModules && client != null && client.textRenderer != null) {
            for (IModule mod : getActiveModules()) {
                int modW = client.textRenderer.getWidth(mod.getName()) + 10;
                if (modW > maxW) {
                    maxW = modW;
                }
            }
        }
        return maxW;
    }

    public static int getTotalHeight(MinecraftClient client, ActivityConfig config) {
        int h = PILL_HEIGHT;
        if (config != null && config.hudShowActiveModules) {
            List<IModule> active = getActiveModules();
            if (!active.isEmpty()) {
                h += 3 + active.size() * (MODULE_ROW_HEIGHT + ROW_GAP);
            }
        }
        return h;
    }

    public static int getEffectiveX(ActivityConfig config, int windowWidth, int totalWidth) {
        if (config != null && config.hudCustomX >= 0) {
            return Math.max(2, Math.min(windowWidth - totalWidth - 2, config.hudCustomX));
        }
        String pos = (config != null && config.hudPosition != null) ? config.hudPosition.toLowerCase(Locale.ROOT) : "top_right";
        if (pos.contains("left")) {
            return PADDING_H;
        } else {
            return Math.max(2, windowWidth - totalWidth - PADDING_H);
        }
    }

    public static int getEffectiveY(ActivityConfig config, int windowHeight, int totalHeight) {
        if (config != null && config.hudCustomY >= 0) {
            return Math.max(2, Math.min(windowHeight - totalHeight - 2, config.hudCustomY));
        }
        String pos = (config != null && config.hudPosition != null) ? config.hudPosition.toLowerCase(Locale.ROOT) : "top_right";
        if (pos.contains("bottom")) {
            return Math.max(2, windowHeight - totalHeight - PADDING_H);
        } else {
            return PADDING_H;
        }
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        activity.client.gui.custom.NativeVisualHud.render(context,tickCounter);
    }

    public static void renderHud(DrawContext context, MinecraftClient client, ActivityConfig config, int x, int y, int totalWidth, int windowWidth) {
        if (context == null || client == null || client.textRenderer == null) return;

        Text titleText = resolveTitleText(config);
        int watermarkWidth = client.textRenderer.getWidth(titleText) + 17;

        float opacityNorm = (float) (Math.max(10.0, Math.min(100.0, config != null ? config.overlayOpacity : 85.0)) / 100.0);
        int alpha = Math.max(25, Math.min(255, Math.round(opacityNorm * 255.0f)));

        int bgColor = (alpha << 24) | 0x0A0D14;
        int borderColor = (Math.max(20, alpha / 2) << 24) | 0x3EA4E8;
        int dotColor = (alpha << 24) | 0x3EA4E8;
        int textColor = (alpha << 24) | 0xFFFFFF;

        boolean rightAligned = (x + totalWidth / 2) > (windowWidth / 2);

        int wmX = rightAligned ? (x + totalWidth - watermarkWidth) : x;
        int wmY = y;

        context.fill(wmX, wmY, wmX + watermarkWidth, wmY + PILL_HEIGHT, bgColor);

        context.fill(wmX, wmY, wmX + watermarkWidth, wmY + 1, borderColor);
        context.fill(wmX, wmY + PILL_HEIGHT - 1, wmX + watermarkWidth, wmY + PILL_HEIGHT, borderColor);
        context.fill(wmX, wmY + 1, wmX + 1, wmY + PILL_HEIGHT - 1, borderColor);
        context.fill(wmX + watermarkWidth - 1, wmY + 1, wmX + watermarkWidth, wmY + PILL_HEIGHT - 1, borderColor);

        context.fill(wmX + 4, wmY + 5, wmX + 8, wmY + 9, dotColor);

        context.drawTextWithShadow(client.textRenderer, titleText, wmX + 11, wmY + 3, textColor);

        if (config != null && config.hudShowActiveModules) {
            List<IModule> active = getActiveModules();
            if (!active.isEmpty()) {
                int curY = wmY + PILL_HEIGHT + 3;
                int moduleAlpha = Math.max(20, (int) (alpha * 0.85f));
                int modBgColor = (moduleAlpha << 24) | 0x0A0D14;
                int modTextColor = (alpha << 24) | 0xDFE8F2;
                int accentColor = (alpha << 24) | 0x3EA4E8;

                for (IModule mod : active) {
                    Text name = mod.getName();
                    int modTextW = client.textRenderer.getWidth(name);
                    int modW = modTextW + 10;
                    int modX = rightAligned ? (x + totalWidth - modW) : x;

                    context.fill(modX, curY, modX + modW, curY + MODULE_ROW_HEIGHT, modBgColor);

                    if (rightAligned) {
                        context.fill(modX + modW - 2, curY, modX + modW, curY + MODULE_ROW_HEIGHT, accentColor);
                        context.drawTextWithShadow(client.textRenderer, name, modX + 3, curY + 2, modTextColor);
                    } else {
                        context.fill(modX, curY, modX + 2, curY + MODULE_ROW_HEIGHT, accentColor);
                        context.drawTextWithShadow(client.textRenderer, name, modX + 5, curY + 2, modTextColor);
                    }

                    curY += MODULE_ROW_HEIGHT + ROW_GAP;
                }
            }
        }
    }
}

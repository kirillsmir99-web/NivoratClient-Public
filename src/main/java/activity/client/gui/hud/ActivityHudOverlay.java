package activity.client.gui.hud;

import activity.client.config.ActivityConfig;
import activity.client.gui.custom.api.ui.BrandMark;
import activity.client.gui.custom.api.ui.theme.ClientAccent;
import activity.client.gui.custom.utils.render.render2d.Render2D;
import activity.client.gui.custom.utils.render.render2d.glow.BuiltGlow;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ActivityHudOverlay {

    public static final String DEFAULT_TITLE = "PulseHUD";
    public static final int PILL_HEIGHT = 15;
    public static final int MODULE_ROW_HEIGHT = 12;
    public static final int ROW_GAP = 2;
    public static final int PADDING_H = 6;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private ActivityHudOverlay() {}

    private record Segment(String text, int color) {}

    private static List<Segment> buildSegments(MinecraftClient client, ActivityConfig config, int alpha) {
        List<Segment> segments = new ArrayList<>();
        boolean inCalibration = dev.nivorat.arc.ArcMotorCalibrationService.hasSession();
        if (inCalibration) {
            dev.nivorat.arc.ArcMotionProfile profile = dev.nivorat.arc.ArcMotionProfile.getInstance();
            int mastery = profile.getMasteryPercent();
            long remMs = profile.getCalibrationRemainingTimeMs();
            long sec = (remMs + 999L) / 1000L;
            String timerStr = String.format("%02d:%02d", sec / 60L, sec % 60L);
            segments.add(new Segment("Калибровка: " + mastery + "%", (alpha << 24) | 0x38BDF8));
            segments.add(new Segment(timerStr, (alpha << 24) | 0xFFFFFF));
            segments.add(new Segment(profile.getManualDetonationsCount() + " взрывов", (alpha << 24) | 0xF59E0B));
            if (client != null) {
                segments.add(new Segment(client.getCurrentFps() + " FPS", (alpha << 24) | 0xCBD5E1));
            }
        } else {
            String serverName = "Одиночная игра";
            if (client != null && client.getCurrentServerEntry() != null && client.getCurrentServerEntry().address != null) {
                serverName = client.getCurrentServerEntry().address.toLowerCase(Locale.ROOT);
            }
            segments.add(new Segment(serverName, (alpha << 24) | 0xA78BFA));
            if (client != null) {
                segments.add(new Segment(client.getCurrentFps() + " FPS", (alpha << 24) | 0xE2E8F0));
                if (client.getSession() != null && client.getSession().getUsername() != null && !client.getSession().getUsername().isBlank()) {
                    segments.add(new Segment(client.getSession().getUsername(), (alpha << 24) | 0xF472B6));
                }
            }
            String timeStr = LocalTime.now().format(TIME_FORMATTER);
            segments.add(new Segment(timeStr, (alpha << 24) | 0xFFFFFF));
        }
        return segments;
    }

    public static Text resolveTitleText(ActivityConfig config) {
        String base = (config != null && config.customTitle != null && !config.customTitle.isBlank() && !"Activity HUD".equals(config.customTitle))
                ? config.customTitle
                : DEFAULT_TITLE;
        StringBuilder sb = new StringBuilder(base);
        MinecraftClient mc = MinecraftClient.getInstance();
        if (dev.nivorat.arc.ArcMotorCalibrationService.hasSession()) {
            int mastery = dev.nivorat.arc.ArcMotorCalibrationService.getMastery();
            long remMs = dev.nivorat.arc.ArcMotorCalibrationService.getRemainingTimeMs();
            long sec = (remMs + 999L) / 1000L;
            String timeStr = String.format("%02d:%02d", sec / 60L, sec % 60L);
            sb.append(" | Калибровка: ").append(mastery).append("%");
            sb.append(" | ").append(timeStr);
            if (mc != null) sb.append(" | ").append(mc.getCurrentFps()).append(" FPS");
        } else {
            if (mc != null) {
                sb.append(" | ").append(mc.getCurrentFps()).append(" FPS");
                if (mc.getSession() != null && mc.getSession().getUsername() != null && !mc.getSession().getUsername().isBlank()) {
                    sb.append(" | ").append(mc.getSession().getUsername());
                }
            }
        }
        return Text.literal(sb.toString());
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
        List<Segment> segments = buildSegments(client, config, 255);
        int width = 10 + 6;
        for (int i = 0; i < segments.size(); i++) {
            width += client.textRenderer.getWidth(segments.get(i).text());
            if (i < segments.size() - 1) {
                width += client.textRenderer.getWidth("|") + 10;
            }
        }
        width += 6;
        return Math.max(width, client.textRenderer.getWidth(resolveTitleText(config)) + 17);
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
        activity.client.gui.custom.NativeVisualHud.render(context, tickCounter);
    }

    public static void renderInGame(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null || mc.options.hudHidden) return;
        ActivityConfig config = activity.client.config.ActivityConfigManager.getConfig();
        boolean hasCalib = dev.nivorat.arc.ArcMotorCalibrationService.hasSession();
        if (config == null || (!config.overlayEnabled && !hasCalib)) return;

        int windowW = mc.getWindow().getScaledWidth();
        int windowH = mc.getWindow().getScaledHeight();
        int totalW = getTotalWidth(mc, config);
        int totalH = getTotalHeight(mc, config);
        int x = getEffectiveX(config, windowW, totalW);
        int y = getEffectiveY(config, windowH, totalH);

        renderHud(context, mc, config, x, y, totalW, windowW);
    }

    public static void renderHud(DrawContext context, MinecraftClient client, ActivityConfig config, int x, int y, int totalWidth, int windowWidth) {
        if (context == null || client == null || client.textRenderer == null) return;

        float opacityNorm = (float) (Math.max(10.0, Math.min(100.0, config != null ? config.overlayOpacity : 85.0)) / 100.0);
        int alpha = Math.max(25, Math.min(255, Math.round(opacityNorm * 255.0f)));

        boolean rightAligned = (x + totalWidth / 2) > (windowWidth / 2);

        if (config != null && config.hudShowActiveModules) {
            List<IModule> active = getActiveModules();
            if (!active.isEmpty()) {
                int curY = y;
                int moduleAlpha = Math.max(20, (int) (alpha * 0.85f));
                int modBgColor = (moduleAlpha << 24) | 0x0A0D14;
                int modTextColor = (alpha << 24) | 0xDFE8F2;
                int accentColor = (alpha << 24) | 0x3EA4E8;

                for (IModule mod : active) {
                    Text name = mod.getName();
                    int modTextW = client.textRenderer.getWidth(name);
                    int modW = modTextW + 10;
                    int modX = rightAligned ? (x + totalWidth - modW) : x;

                    Render2D.rect(modX, curY, modW, MODULE_ROW_HEIGHT, 3.0f, modBgColor);

                    if (rightAligned) {
                        Render2D.rect(modX + modW - 2, curY, 2, MODULE_ROW_HEIGHT, 1.0f, accentColor);
                        context.drawTextWithShadow(client.textRenderer, name, modX + 3, curY + 2, modTextColor);
                    } else {
                        Render2D.rect(modX, curY, 2, MODULE_ROW_HEIGHT, 1.0f, accentColor);
                        context.drawTextWithShadow(client.textRenderer, name, modX + 5, curY + 2, modTextColor);
                    }

                    curY += MODULE_ROW_HEIGHT + ROW_GAP;
                }
            }
        }
    }
}

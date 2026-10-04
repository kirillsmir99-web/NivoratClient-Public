package activity.client.gui.custom.api.drags;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import activity.client.gui.custom.VisualMaterial;
import activity.client.gui.custom.api.modules.ModuleManager;
import activity.client.gui.custom.api.modules.impl.Interface.WatermarkModule;
import activity.client.gui.custom.api.ui.BrandMark;
import activity.client.gui.custom.api.ui.theme.ClientAccent;
import activity.client.gui.custom.api.ui.theme.ThemeManager;
import activity.client.gui.custom.api.ui.theme.ThemeProfile;
import activity.client.gui.custom.utils.animations.Easings;
import activity.client.gui.custom.utils.animations.SmoothAnimation;
import activity.client.gui.custom.utils.color.ColorUtil;
import activity.client.gui.custom.utils.render.others.RectUtil;
import activity.client.gui.custom.utils.render.render2d.Render2D;
import activity.client.gui.custom.utils.render.render2d.glass.BuiltGlass;
import activity.client.gui.custom.utils.render.render2d.glow.BuiltGlow;
import dev.nivorat.arc.ArcMotionProfile;
import dev.nivorat.arc.ArcMotorCalibrationService;

public final class WatermarkComp extends Draggable {
    private static final float H = 20.0f;
    private static final String INFO_FONT = "montserrat-bold";
    private static final String FALLBACK_NAME = "Player";
    private static final DateTimeFormatter TIME_FORMAT_24 = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_FORMAT_12 = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private final SmoothAnimation visibility = new SmoothAnimation();
    private final SmoothAnimation animatedWidth = new SmoothAnimation();
    private final SmoothAnimation serverAnim = new SmoothAnimation();
    private final SmoothAnimation fpsAnim = new SmoothAnimation();
    private final SmoothAnimation pingAnim = new SmoothAnimation();
    private final SmoothAnimation nickAnim = new SmoothAnimation();
    private final SmoothAnimation timeAnim = new SmoothAnimation();
    private final SmoothAnimation combatAnim = new SmoothAnimation();
    private final SmoothAnimation calibAnim = new SmoothAnimation();

    private boolean widthInitialized;
    private boolean lastTargetVisible;
    private float currentWidth = 80.0f;

    private String cachedServer = "";
    private float serverWidth;
    private String cachedFps = "";
    private float fpsWidth;
    private String cachedPing = "";
    private float pingWidth;
    private String cachedName = "";
    private float nameWidth;
    private String timeText = "";
    private float timeWidth;
    private String cachedCombat = "";
    private float combatWidth;
    private String cachedCalib = "";
    private float calibWidth;
    private boolean centered = true;

    public WatermarkComp() {
        super("watermark", (Position.screenWidth() - 100.0f) * 0.5f, 6.0f);
        this.centered = true;
        this.visibility.set(1.0);
        this.serverAnim.set(1.0);
        this.fpsAnim.set(1.0);
        this.pingAnim.set(1.0);
        this.nickAnim.set(1.0);
        this.timeAnim.set(1.0);
        this.combatAnim.set(0.0);
        this.calibAnim.set(0.0);
    }

    public boolean isCentered() {
        return this.centered;
    }

    public void setCentered(boolean centered) {
        this.centered = centered;
    }

    @Override
    public void resetToDefault() {
        this.centered = true;
        float defaultX = (Position.screenWidth() - this.width()) * 0.5f;
        float defaultY = 6.0f;
        this.getDrag().setTargetX(defaultX);
        this.getDrag().setTargetY(defaultY);
        this.getDrag().syncToTarget();
    }

    @Override
    public String displayName() {
        return "Watermark";
    }

    public float getScale() {
        WatermarkModule mod = ModuleManager.get().get(WatermarkModule.class);
        return mod != null ? mod.scale() : 1.0f;
    }

    @Override
    public float width() {
        float w = this.currentWidth > 0.0f ? this.currentWidth : this.computeDesiredWidth();
        return w * this.getScale();
    }

    @Override
    public float height() {
        return H * this.getScale();
    }

    @Override
    public float getX() {
        if (this.centered) {
            return (Position.screenWidth() - this.width()) * 0.5f;
        }
        return super.getX();
    }

    @Override
    public float getY() {
        return super.getY();
    }

    @Override
    public boolean isInteractive() {
        return this.shouldShow();
    }

    private static int indexedPaletteColor(int[] palette, float phase) {
        int n = palette.length;
        if (n <= 1) {
            return palette[0];
        }
        float f2 = normalizeCycle(phase) / 360.0f;
        float f3 = f2 < 0.5f ? f2 * 2.0f : (1.0f - f2) * 2.0f;
        float f4 = f3 * (float)(n - 1);
        int n2 = (int)f4;
        if (n2 > n - 2) {
            n2 = n - 2;
        }
        return ColorUtil.lerpColor(palette[n2], palette[n2 + 1], f4 - (float)n2);
    }

    private static int[] watermarkPalette() {
        int[] nArray = ClientAccent.currentPalette();
        if (nArray == null || nArray.length == 0) {
            int n = ColorUtil.lerpColor(-2234369, -1, 0.18f);
            return new int[]{n, ColorUtil.lerpColor(n, -1, 0.46f)};
        }
        int[] nArray2 = new int[nArray.length];
        for (int i = 0; i < nArray.length; ++i) {
            nArray2[i] = ColorUtil.lerpColor(opaque(nArray[i]), -1, 0.18f);
        }
        return nArray2;
    }

    private static float indexedGradientPhase(WatermarkModule mod) {
        if (mod != null && !mod.chromaWave.getValue()) {
            return 0.0f;
        }
        float speed = mod != null ? mod.waveSpeed.getFloat() : 1.2f;
        long period = Math.max(100L, (long)(1200.0f / Math.max(0.1f, speed)));
        return (float)(System.currentTimeMillis() % period) / (float)period * 360.0f;
    }

    private static float normalizeCycle(float f) {
        float f2 = f % 360.0f;
        return f2 < 0.0f ? f2 + 360.0f : f2;
    }

    private String serverName() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.isInSingleplayer() || mc.isIntegratedServerRunning()) {
            return "Одиночная игра";
        }
        if (mc.getCurrentServerEntry() != null && mc.getCurrentServerEntry().address != null) {
            return mc.getCurrentServerEntry().address;
        }
        return "Онлайн";
    }

    private String fpsText() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.getCurrentFps() + " FPS";
    }

    private String pingText() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int ping = 0;
        if (mc.getNetworkHandler() != null && mc.player != null) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) {
                ping = Math.max(0, entry.getLatency());
            }
        }
        return ping + "ms";
    }

    private void updateInfoCache() {
        WatermarkModule mod = ModuleManager.get().get(WatermarkModule.class);
        boolean showServer = mod != null && mod.showServer.getValue();
        boolean showFps = mod != null && mod.showFps.getValue();
        boolean showPing = mod != null && mod.showPing.getValue();
        boolean showNick = mod == null || mod.showNick.getValue();
        boolean showTime = mod == null || mod.showTime.getValue();

        if (showServer) {
            String s = this.serverName();
            if (!s.equals(this.cachedServer) || this.serverWidth <= 0.0f) {
                this.cachedServer = s;
                this.serverWidth = measureString(INFO_FONT, s, 8.0f);
            }
        } else {
            this.cachedServer = "";
            this.serverWidth = 0.0f;
        }

        if (showFps) {
            String f = this.fpsText();
            if (!f.equals(this.cachedFps) || this.fpsWidth <= 0.0f) {
                this.cachedFps = f;
                this.fpsWidth = measureString(INFO_FONT, f, 8.0f);
            }
        } else {
            this.cachedFps = "";
            this.fpsWidth = 0.0f;
        }

        if (showPing) {
            String p = this.pingText();
            if (!p.equals(this.cachedPing) || this.pingWidth <= 0.0f) {
                this.cachedPing = p;
                this.pingWidth = measureString(INFO_FONT, p, 8.0f);
            }
        } else {
            this.cachedPing = "";
            this.pingWidth = 0.0f;
        }

        if (showNick) {
            String n = playerName();
            if (!n.equals(this.cachedName) || this.nameWidth <= 0.0f) {
                this.cachedName = n;
                this.nameWidth = measureString(INFO_FONT, n, 8.0f);
            }
        } else {
            this.cachedName = "";
            this.nameWidth = 0.0f;
        }

        if (showTime) {
            boolean is12 = mod != null && "12 часов".equals(mod.timeFormat.getValue());
            DateTimeFormatter fmt = is12 ? TIME_FORMAT_12 : TIME_FORMAT_24;
            String t = LocalTime.now(java.time.ZoneId.systemDefault()).format(fmt);
            if (!t.equals(this.timeText) || this.timeWidth <= 0.0f) {
                this.timeText = t;
                this.timeWidth = measureString(INFO_FONT, t, 8.0f);
            }
        } else {
            this.timeText = "";
            this.timeWidth = 0.0f;
        }

        boolean hasCalib = ArcMotorCalibrationService.hasSession();
        if (hasCalib) {
            ArcMotionProfile profile = ArcMotionProfile.getInstance();
            long remMs = profile.getCalibrationRemainingTimeMs();
            long sec = (remMs + 999L) / 1000L;
            String timerStr = String.format("%02d:%02d", sec / 60L, sec % 60L);
            String calibStr = "Калибровка " + profile.getMasteryPercent() + "% • " + timerStr;
            if (!calibStr.equals(this.cachedCalib) || this.calibWidth <= 0.0f) {
                this.cachedCalib = calibStr;
                this.calibWidth = measureString(INFO_FONT, calibStr, 8.0f);
            }
        } else {
            this.cachedCalib = "";
            this.calibWidth = 0.0f;
        }
    }

    private static float measureString(String font, String text, float size) {
        if (text == null || text.isEmpty()) {
            return 0.0f;
        }
        float w = 0.0f;
        int codeLength;
        for (int i = 0; i < text.length(); i += codeLength) {
            int cp = text.codePointAt(i);
            codeLength = Character.charCount(cp);
            String glyph = text.substring(i, i + codeLength);
            w += Render2D.msdfWidth(font, glyph, size);
        }
        return w;
    }

    private static float drawGradientString(String font, String text, float x, float y, float size, int startIndex, int[] palette, float alpha, float phase, boolean isMinimal) {
        float effAlpha = Math.max(0.0f, Math.min(1.0f, alpha));
        if (effAlpha <= 0.003921569f || text == null || text.isEmpty()) {
            return 0.0f;
        }
        float curX = x;
        int idx = startIndex;
        int codeLength;
        for (int i = 0; i < text.length(); i += codeLength) {
            int cp = text.codePointAt(i);
            codeLength = Character.charCount(cp);
            String glyph = text.substring(i, i + codeLength);
            int shadowCol = ColorUtil.multAlpha(0xFF000000, effAlpha * (isMinimal ? 0.8f : 0.45f));
            Render2D.msdfText(font, glyph, curX + 0.5f, y + 0.5f, size, shadowCol);
            int color = ColorUtil.multAlpha(indexedPaletteColor(palette, phase + (float)idx * 15.0f), effAlpha);
            Render2D.msdfText(font, glyph, curX, y, size, color);
            curX += Render2D.msdfWidth(font, glyph, size);
            ++idx;
        }
        return curX - x;
    }

    private static void drawVerticalSeparator(float x, float centerY, float alpha) {
        float h = 8.5f;
        float y = centerY - h * 0.5f;
        int sepColor = ColorUtil.multAlpha(0xFFFFFFFF, alpha * 0.22f);
        Render2D.rect(x, y, 1.0f, h, 0.5f, sepColor);
    }

    private void updateChipAnimations(boolean showServer, boolean showFps, boolean showPing, boolean showNick, boolean showTime, boolean showCalib) {
        updateSingleChipAnim(this.serverAnim, showServer);
        updateSingleChipAnim(this.fpsAnim, showFps);
        updateSingleChipAnim(this.pingAnim, showPing);
        updateSingleChipAnim(this.nickAnim, showNick);
        updateSingleChipAnim(this.timeAnim, showTime);
        updateSingleChipAnim(this.calibAnim, showCalib);
    }

    private static void updateSingleChipAnim(SmoothAnimation anim, boolean target) {
        double targetVal = target ? 1.0 : 0.0;
        if (Math.abs(anim.getToValue() - targetVal) > 0.01) {
            anim.run(targetVal, target ? 0.22 : 0.18, Easings.CUBIC_OUT, false);
        }
        anim.update();
    }

    public float computeDesiredWidth() {
        WatermarkModule watermarkModule = ModuleManager.get().get(WatermarkModule.class);
        this.updateInfoCache();

        boolean showServer = watermarkModule != null && watermarkModule.showServer.getValue() && !this.cachedServer.isEmpty();
        boolean showFps = watermarkModule != null && watermarkModule.showFps.getValue() && !this.cachedFps.isEmpty();
        boolean showPing = watermarkModule != null && watermarkModule.showPing.getValue() && !this.cachedPing.isEmpty();
        boolean showNick = watermarkModule == null || watermarkModule.showNick.getValue() && !this.cachedName.isEmpty();
        boolean showTime = watermarkModule == null || watermarkModule.showTime.getValue() && !this.timeText.isEmpty();
        boolean showCalib = ArcMotorCalibrationService.hasSession() && !this.cachedCalib.isEmpty();

        this.updateChipAnimations(showServer, showFps, showPing, showNick, showTime, showCalib);

        float padX = 12.5f;
        float sepGap = 13.0f;
        float totalW = padX + 11.0f;

        float cF = (float) this.calibAnim.get();
        if (cF > 0.005f) totalW += (sepGap + this.calibWidth) * cF;

        float sF = (float) this.serverAnim.get();
        if (sF > 0.005f) totalW += (sepGap + this.serverWidth) * sF;

        float fF = (float) this.fpsAnim.get();
        if (fF > 0.005f) totalW += (sepGap + this.fpsWidth) * fF;

        float pF = (float) this.pingAnim.get();
        if (pF > 0.005f) totalW += (sepGap + this.pingWidth) * pF;

        float nF = (float) this.nickAnim.get();
        if (nF > 0.005f) totalW += (sepGap + this.nameWidth) * nF;

        float tF = (float) this.timeAnim.get();
        if (tF > 0.005f) totalW += (sepGap + this.timeWidth) * tF;

        totalW += padX;
        return Math.max(36.0f, totalW);
    }

    @Override
    protected void render(DrawContext drawContext) {
        boolean bl = this.shouldShow();
        if (bl != this.lastTargetVisible) {
            this.visibility.run(bl ? 1.0 : 0.0, bl ? 0.18 : 0.12, Easings.CUBIC_OUT, false);
            this.lastTargetVisible = bl;
        }
        this.visibility.update();
        float f = (float) this.visibility.get();
        if (bl && f <= 0.01f) {
            f = 0.01f;
        }
        if (f <= 0.01f && !bl) {
            return;
        }

        WatermarkModule watermarkModule = ModuleManager.get().get(WatermarkModule.class);
        float targetW = this.computeDesiredWidth();
        if (!this.widthInitialized || this.currentWidth <= 0.0f || Math.abs(this.currentWidth - targetW) > 75.0f) {
            this.animatedWidth.set(targetW);
            this.currentWidth = targetW;
            this.widthInitialized = true;
        } else {
            if (Math.abs(this.animatedWidth.getToValue() - (double) targetW) > 0.5) {
                this.animatedWidth.run(targetW, 0.22, Easings.CUBIC_OUT, false);
            }
        }
        this.animatedWidth.update();
        this.currentWidth = (float) this.animatedWidth.get();
        float capsuleW = Math.max(this.currentWidth, targetW);

        float padX = 12.5f;
        float sepGap = 13.0f;
        float screenW = Position.screenWidth();

        float originX;
        float originY;

        if (this.getDrag().isDragging()) {
            float curCenterX = this.getDrag().getTargetX() + capsuleW * 0.5f;
            if (Math.abs(curCenterX - screenW * 0.5f) <= 14.0f) {
                this.centered = true;
                this.getDrag().setTargetX((screenW - capsuleW) * 0.5f);
                this.getDrag().setSnapLineX(screenW * 0.5f);
            } else {
                this.centered = false;
            }
        }

        if (this.centered) {
            originX = (screenW - capsuleW) * 0.5f;
            originY = this.getY();
            this.getDrag().setTargetX(originX);
            if (!this.getDrag().isDragging()) {
                this.getDrag().syncToTarget();
            }
        } else {
            originX = this.getX();
            originY = this.getY();
        }

        String style = watermarkModule != null ? watermarkModule.style.getValue() : "Капсула";
        boolean isPill = !"Минимализм".equalsIgnoreCase(style);
        boolean isMinimal = "Минимализм".equalsIgnoreCase(style);

        float s = this.getScale();
        float animScale = 0.92f + f * 0.08f;
        float totalScale = s * animScale;
        float centerX = originX + capsuleW * 0.5f;
        float centerY = originY + 10.0f;

        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().translate(centerX, centerY);
        drawContext.getMatrices().scale(totalScale, totalScale);
        drawContext.getMatrices().translate(-centerX, -centerY);

        Render2D.beginFrame(drawContext);

        int[] palette = watermarkPalette();
        float phase = indexedGradientPhase(watermarkModule);

        if (isPill) {
            int bgCol = ColorUtil.rgba(12, 14, 22, (int)(200 * f));
            Render2D.rect(originX, originY, capsuleW, 20.0f, 10.0f, bgCol);
            drawWatermarkGlass(originX, originY, capsuleW, 20.0f, 10.0f, f);

            int borderCol = ColorUtil.rgba(255, 255, 255, (int)(75 * f));
            Render2D.outline(originX, originY, capsuleW, 20.0f, 10.0f, 0.9f, borderCol);
            RectUtil.drawClientGlowOnly(originX, originY, capsuleW, 20.0f, 10.0f, f * 0.90f);
        }

        float curX = originX + padX;
        float textY = originY + (20.0f - 8.5f) * 0.5f - 0.5f;
        float middleY = originY + 10.0f;
        int glyphIdx = 1;

        float logoY = originY + (20.0f - 11.0f) * 0.5f;
        BrandMark.draw(curX, logoY, 11.0f, f);
        curX += 11.0f;

        float calF = (float) this.calibAnim.get();
        if (calF > 0.01f && !this.cachedCalib.isEmpty()) {
            float chipAlpha = f * Math.min(1.0f, calF * 1.25f);
            drawVerticalSeparator(curX + sepGap * 0.5f - 0.5f, middleY, chipAlpha);
            curX += sepGap * calF;
            int[] calibPalette = new int[]{ClientAccent.accentBright(255), 0xFFFFFFFF};
            drawGradientString(INFO_FONT, this.cachedCalib, curX, textY + 0.5f, 8.0f, glyphIdx, calibPalette, chipAlpha, phase, isMinimal);
            curX += this.calibWidth * calF;
            glyphIdx += Math.round(this.calibWidth / 8.0f) + 1;
        }

        float sF = (float) this.serverAnim.get();
        if (sF > 0.01f && !this.cachedServer.isEmpty()) {
            float chipAlpha = f * Math.min(1.0f, sF * 1.25f);
            drawVerticalSeparator(curX + sepGap * 0.5f - 0.5f, middleY, chipAlpha);
            curX += sepGap * sF;
            drawGradientString(INFO_FONT, this.cachedServer, curX, textY + 0.5f, 8.0f, glyphIdx, palette, chipAlpha, phase, isMinimal);
            curX += this.serverWidth * sF;
            glyphIdx += Math.round(this.serverWidth / 8.0f) + 1;
        }

        float fF = (float) this.fpsAnim.get();
        if (fF > 0.01f && !this.cachedFps.isEmpty()) {
            float chipAlpha = f * Math.min(1.0f, fF * 1.25f);
            drawVerticalSeparator(curX + sepGap * 0.5f - 0.5f, middleY, chipAlpha);
            curX += sepGap * fF;
            drawGradientString(INFO_FONT, this.cachedFps, curX, textY + 0.5f, 8.0f, glyphIdx, palette, chipAlpha, phase, isMinimal);
            curX += this.fpsWidth * fF;
            glyphIdx += Math.round(this.fpsWidth / 8.0f) + 1;
        }

        float pF = (float) this.pingAnim.get();
        if (pF > 0.01f && !this.cachedPing.isEmpty()) {
            float chipAlpha = f * Math.min(1.0f, pF * 1.25f);
            drawVerticalSeparator(curX + sepGap * 0.5f - 0.5f, middleY, chipAlpha);
            curX += sepGap * pF;
            drawGradientString(INFO_FONT, this.cachedPing, curX, textY + 0.5f, 8.0f, glyphIdx, palette, chipAlpha, phase, isMinimal);
            curX += this.pingWidth * pF;
            glyphIdx += Math.round(this.pingWidth / 8.0f) + 1;
        }

        float nF = (float) this.nickAnim.get();
        if (nF > 0.01f && !this.cachedName.isEmpty()) {
            float chipAlpha = f * Math.min(1.0f, nF * 1.25f);
            drawVerticalSeparator(curX + sepGap * 0.5f - 0.5f, middleY, chipAlpha);
            curX += sepGap * nF;
            drawGradientString(INFO_FONT, this.cachedName, curX, textY + 0.5f, 8.0f, glyphIdx, palette, chipAlpha, phase, isMinimal);
            curX += this.nameWidth * nF;
            glyphIdx += Math.round(this.nameWidth / 8.0f) + 1;
        }

        float tF = (float) this.timeAnim.get();
        if (tF > 0.01f && !this.timeText.isEmpty()) {
            float chipAlpha = f * Math.min(1.0f, tF * 1.25f);
            drawVerticalSeparator(curX + sepGap * 0.5f - 0.5f, middleY, chipAlpha);
            curX += sepGap * tF;
            drawGradientString(INFO_FONT, this.timeText, curX, textY + 0.5f, 8.0f, glyphIdx, palette, chipAlpha, phase, isMinimal);
            curX += this.timeWidth * tF;
            glyphIdx += Math.round(this.timeWidth / 8.0f) + 1;
        }

        Render2D.flush();
        drawContext.getMatrices().popMatrix();
    }

    private static String playerName() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getSession() != null && mc.getSession().getUsername() != null && !mc.getSession().getUsername().isBlank()) {
            return mc.getSession().getUsername();
        }
        if (mc.player != null && mc.player.getGameProfile().name() != null && !mc.player.getGameProfile().name().isBlank()) {
            return mc.player.getGameProfile().name();
        }
        return FALLBACK_NAME;
    }

    private boolean shouldShow() {
        MinecraftClient mc = MinecraftClient.getInstance();
        WatermarkModule mod = ModuleManager.get().get(WatermarkModule.class);
        if (mod == null || !mod.isEnabled()) {
            return false;
        }
        if (mod.hideInF3.getValue() && mc.getDebugHud() != null && mc.getDebugHud().shouldShowDebugHud()) {
            return false;
        }
        return true;
    }

    private static void drawWatermarkGlass(float x, float y, float w, float h, float radius, float alpha) {
        VisualMaterial interfaceModule = VisualMaterial.getInstance();
        float effAlpha = Math.max(0.0f, Math.min(1.0f, alpha));
        float r = Math.max(0.0f, Math.min(radius, Math.min(w, h) * 0.5f));
        float blur = ThemeManager.blurRadius(18.0f);
        int primaryCol = interfaceModule == null ? -857872385 : interfaceModule.clientPrimaryColor();
        boolean useSec = interfaceModule != null && interfaceModule.usesSecondClientColor();
        int secCol = useSec ? interfaceModule.clientSecondaryColor() : primaryCol;
        int finalSec = useSec ? secCol : primaryCol;
        ThemeProfile profile = ThemeManager.currentProfile();
        float pEdge = profile != null ? profile.edge : 1.0f;
        float pSharp = profile != null ? profile.specular : 1.0f;
        float pRefract = profile != null ? profile.refraction : 1.0f;
        float pDarkening = profile != null ? profile.darkening : 1.0f;
        float edgeStrength = 0.18f * pEdge;
        float edgeSharpness = 55.0f * pSharp;
        float refractStrength = 0.3f * pRefract;

        BuiltGlass builtGlass = new BuiltGlass(
                x, y, w, h, r, r, r, r, primaryCol, effAlpha, edgeSharpness, primaryCol, pDarkening, true, edgeStrength, refractStrength, 0.5f, 0.0f)
                .withBlurRadius(blur)
                .withSecondColor(finalSec, 0.0f);
        if (interfaceModule != null && interfaceModule.isWaveEdgeActiveFor(true)) {
            float seed = (float) Math.abs((int)(x * 17.0f + y * 31.0f + w * 7.0f + h) % 10000);
            builtGlass = builtGlass.withWaveEdge(
                true,
                effAlpha,
                interfaceModule.waveEdgeIntensity.getFloat(),
                interfaceModule.waveEdgeSize.getFloat(),
                interfaceModule.waveEdgeDensity.getFloat(),
                interfaceModule.waveEdgeMotion.getValue(),
                interfaceModule.waveEdgeSpeed.getFloat(),
                interfaceModule.waveEdgeGlow.getFloat(),
                seed,
                0
            );
        }
        Render2D.glass(builtGlass);
    }

    private static int opaque(int n) {
        return n & 0xFFFFFF | 0xFF000000;
    }
}

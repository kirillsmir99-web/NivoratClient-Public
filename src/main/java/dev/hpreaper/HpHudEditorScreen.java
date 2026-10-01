package dev.hpreaper;

import activity.client.config.ActivityConfigManager;
import activity.client.gui.custom.CustomRender;
import activity.client.gui.custom.UnifiedHudRender;
import activity.client.gui.custom.api.drags.Position;
import activity.client.gui.custom.api.ui.theme.ClientAccent;
import activity.client.gui.custom.utils.render.fonts.Fonts;
import activity.client.gui.custom.utils.render.render2d.Render2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

public final class HpHudEditorScreen extends Screen {
    private final Screen parent;
    private boolean dragging;
    private float grabX, grabY;
    public HpHudEditorScreen(Screen parent) { super(Text.literal("HP Reaper")); this.parent = parent; }
    public HpHudEditorScreen() { this(null); }
    private boolean ru() { return activity.client.i18n.LocalizationService.isRussianPreferred(); }
    private HealthHudOverlay.DisplayMode previewMode() { return VitalityConfig.displayMode == HealthHudOverlay.DisplayMode.DISABLED ? HealthHudOverlay.DisplayMode.OWN_HEALTH : VitalityConfig.displayMode; }
    private int elementWidth() { return HealthHudOverlay.getPreviewWidth(textRenderer, previewMode()); }
    private int x() { return HealthHudOverlay.getEffectiveX(previewMode(), (int) Position.screenWidth(), elementWidth()); }
    private int y() { return HealthHudOverlay.getEffectiveY(previewMode(), (int) Position.screenHeight(), 48); }
    private void move(float x, float y) {
        VitalityConfig.setModePos(previewMode(), Math.round(Math.clamp(x, 2, Position.screenWidth() - elementWidth() - 2)),
                Math.round(Math.clamp(y, 25, Position.screenHeight() - 50)));
    }
    @Override public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) return true;
        float mx = Position.mouseX(), my = Position.mouseY();
        if (mx >= 18 && mx <= 208) {
            if (my >= 73 && my <= 95) cycleMode();
            else if (my >= 102 && my <= 124) VitalityConfig.resetModePos(VitalityConfig.displayMode);
            else if (my >= 131 && my <= 153) close();
        }
        if (mx >= x() && mx <= x() + Math.max(elementWidth(), 68) && my >= y() - 23 && my <= y() + 24) {
            dragging = true; grabX = mx - x(); grabY = my - y();
        }
        return true;
    }
    @Override public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragging) { move(Position.mouseX() - grabX, Position.mouseY() - grabY); return true; }
        return false;
    }
    @Override public boolean mouseReleased(Click click) { dragging = false; return true; }
    @Override public boolean keyPressed(KeyInput input) {
        switch (input.key()) {
            case 256, 257 -> close();
            case 258 -> cycleMode();
            case 82 -> VitalityConfig.resetModePos(VitalityConfig.displayMode);
            case 262 -> move(x() + 1, y());
            case 263 -> move(x() - 1, y());
            case 264 -> move(x(), y() + 1);
            case 265 -> move(x(), y() - 1);
            default -> { return false; }
        }
        return true;
    }
    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        HealthHudOverlay.renderPreview(context, client, x(), y(), previewMode());
        try (var frame = UnifiedHudRender.beginNative(context)) {
            CustomRender.panel(context, 8, 12, 210, 153, 9, 1);
            Fonts.MONTSERRAT_MEDIUM.draw("HP REAPER", 18, 23, 10, -1);
            Fonts.MONTSERRAT_MEDIUM.draw(ru() ? "Перетащите HUD • стрелки для точности" : "Drag HUD • arrow keys to fine-tune", 18, 42, 6, 0xffaeb8cc);
            Fonts.MONTSERRAT_MEDIUM.draw("X " + x() + "  ·  Y " + y(), 18, 58, 7, ClientAccent.accentBright(255));
            button(context, 73, modeLabel());
            button(context, 102, ru() ? "Сбросить позицию" : "Reset position");
            button(context, 131, ru() ? "Готово" : "Done");
        }
    }
    private String modeLabel() {
        return switch (VitalityConfig.displayMode) {
            case OWN_HEALTH -> ru() ? "Своё HP" : "Own HP";
            case CROSSHAIR_AND_TARGET -> ru() ? "Своё HP и цель" : "Own HP and target";
            case TARGET_HEALTH -> ru() ? "HP цели" : "Target HP";
            case OWN_TARGET_AND_DIFFERENCE -> ru() ? "Своё HP · цель · разница" : "Own HP · target · difference";
            case DISABLED -> ru() ? "Выключено" : "Disabled";
        };
    }
    private void button(DrawContext context, float y, String label) {
        boolean hover = Position.mouseX() >= 18 && Position.mouseX() <= 208 && Position.mouseY() >= y && Position.mouseY() <= y + 22;
        Render2D.rect(18, y, 190, 22, 4, hover ? ClientAccent.accent(180) : 0xff252936);
        Fonts.MONTSERRAT_MEDIUM.draw(CustomRender.fit(label, 176, 7), 25, y + 7, 7, -1);
    }
    private void cycleMode() { HealthHudOverlay.cycleDisplayMode(); saveState(); }
    private void saveState() {
        var cfg = ActivityConfigManager.getConfig();
        cfg.hpReaperMode = switch (VitalityConfig.displayMode) {
            case OWN_HEALTH -> "own_hp";
            case CROSSHAIR_AND_TARGET -> "compact";
            case OWN_TARGET_AND_DIFFERENCE -> "damage_diff";
            default -> "target_hp";
        };
        cfg.hpReaperEnabled = VitalityConfig.displayMode != HealthHudOverlay.DisplayMode.DISABLED;
        cfg.hpReaperOwnHealthX = VitalityConfig.ownHealthX; cfg.hpReaperOwnHealthY = VitalityConfig.ownHealthY;
        cfg.hpReaperCrosshairTargetX = VitalityConfig.crosshairTargetX; cfg.hpReaperCrosshairTargetY = VitalityConfig.crosshairTargetY;
        cfg.hpReaperTargetHealthX = VitalityConfig.targetHealthX; cfg.hpReaperTargetHealthY = VitalityConfig.targetHealthY;
        cfg.hpReaperDiffX = VitalityConfig.diffX; cfg.hpReaperDiffY = VitalityConfig.diffY;
        var module = activity.client.module.api.ModuleRegistry.get("hp_reaper");
        if (module != null) module.setEnabled(cfg.hpReaperEnabled);
        ActivityConfigManager.markDirty(); ActivityConfigManager.save();
    }
    @Override public void close() {
        saveState();
        client.setScreen(parent);
    }
    @Override public boolean shouldPause() { return false; }
}

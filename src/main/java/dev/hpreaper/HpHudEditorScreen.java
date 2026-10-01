package dev.hpreaper;

import activity.client.config.ActivityConfigManager;
import activity.client.gui.custom.NativeHudEditorScreen;
import activity.client.gui.custom.api.drags.Position;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

public final class HpHudEditorScreen extends NativeHudEditorScreen {
    public HpHudEditorScreen(Screen parent) { super("HP REAPER", parent); }
    public HpHudEditorScreen() { this(null); }
    @Override protected int hudWidth() { return HealthHudOverlay.getPreviewWidth(textRenderer, VitalityConfig.displayMode); }
    @Override protected int hudHeight() { return HealthHudOverlay.getPreviewHeight(VitalityConfig.displayMode); }
    @Override protected int hudX() { return HealthHudOverlay.getEffectiveX(VitalityConfig.displayMode, (int) Position.screenWidth(), hudWidth()); }
    @Override protected int hudY() { return HealthHudOverlay.getEffectiveY(VitalityConfig.displayMode, (int) Position.screenHeight(), hudHeight()); }
    @Override protected void moveHud(float x, float y) {
        if (VitalityConfig.displayMode == HealthHudOverlay.DisplayMode.DISABLED) return;
        VitalityConfig.setModePos(VitalityConfig.displayMode,
                Math.round(Math.clamp(x, 2, Math.max(2, Position.screenWidth() - hudWidth() - 2))),
                Math.round(Math.clamp(y, hudHeight() > 24 ? 25 : 4, Math.max(25, Position.screenHeight() - 20))));
    }
    @Override protected void resetHud() { VitalityConfig.resetModePos(VitalityConfig.displayMode); }
    @Override protected void saveHud() { saveState(); }
    @Override protected void drawHud(DrawContext context) { HealthHudOverlay.renderPreview(context, client, hudX(), hudY(), VitalityConfig.displayMode); }
    @Override protected void cycleMode() { HealthHudOverlay.cycleDisplayMode(); saveState(); }
    @Override protected String modeLabel() {
        return switch (VitalityConfig.displayMode) {
            case OWN_HEALTH -> ru() ? "Своё HP" : "Own HP";
            case CROSSHAIR_AND_TARGET -> ru() ? "Своё HP и цель" : "Own HP and target";
            case TARGET_HEALTH -> ru() ? "HP цели" : "Target HP";
            case OWN_TARGET_AND_DIFFERENCE -> ru() ? "Своё HP · цель · разница" : "Own HP · target · difference";
            case DISABLED -> ru() ? "Выключено" : "Disabled";
        };
    }
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
}

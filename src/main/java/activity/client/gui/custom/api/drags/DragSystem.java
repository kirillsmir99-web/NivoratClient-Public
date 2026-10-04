package activity.client.gui.custom.api.drags;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.custom.api.ui.UI;

public final class DragSystem {
    private static final DragSystem INSTANCE = new DragSystem();
    private final List<Draggable> elements = new ArrayList<>();
    private Draggable activeDrag = null;
    private final WatermarkComp watermark;
    private boolean eventsRegistered = false;

    private DragSystem() {
        this.watermark = new WatermarkComp();
        this.register(this.watermark);
        this.loadSavedPositions();
        this.registerScreenEvents();
    }

    public static DragSystem get() {
        return INSTANCE;
    }

    public WatermarkComp getWatermark() {
        return this.watermark;
    }

    public void register(Draggable draggable) {
        if (!this.elements.contains(draggable)) {
            this.elements.add(draggable);
        }
    }

    public boolean isDragModeActive() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.currentScreen == null) {
            return false;
        }
        return mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof UI;
    }

    private void registerScreenEvents() {
        if (eventsRegistered) return;
        eventsRegistered = true;
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof ChatScreen) {
                ScreenMouseEvents.allowMouseClick(screen).register((scr, click) -> {
                    return !DragSystem.get().mouseClicked(click);
                });
                ScreenMouseEvents.allowMouseRelease(screen).register((scr, click) -> {
                    return !DragSystem.get().mouseReleased(click);
                });
                ScreenEvents.afterRender(screen).register((scr, context, mouseX, mouseY, delta) -> {
                    DragSystem.get().renderInGui(context);
                });
            }
        });
    }

    public void renderInHud(DrawContext context) {
        if (context == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) return;
        boolean dragMode = this.isDragModeActive();
        if (!dragMode && this.activeDrag != null) {
            this.activeDrag.getDrag().release();
            this.activeDrag = null;
            this.savePositions();
        }
        float mx = Position.mouseX();
        float my = Position.mouseY();
        for (Draggable d : this.elements) {
            if (dragMode && d.isInteractive()) {
                d.getDrag().tick(mx, my, d.overlayWidth(), d.overlayHeight(), true);
            }
            d.getDrag().updateTilt(mx, false);
            if (!d.getDrag().isDragging()) {
                d.getDrag().applyScreenClamp(d.width(), d.height());
            }
            d.renderNormal(context);
        }
    }

    public void renderInGui(DrawContext context) {
        if (context == null || !this.isDragModeActive()) return;
        float mx = Position.mouseX();
        float my = Position.mouseY();
        for (Draggable d : this.elements) {
            if (d.isInteractive()) {
                d.getDrag().tick(mx, my, d.overlayWidth(), d.overlayHeight(), true);
                d.renderNormal(context);
                d.getDrag().renderOverlay(context, d.width(), d.height(), d.overlayWidth(), d.overlayHeight());
            }
        }
    }

    public boolean mouseClicked(Click click) {
        if (click == null || !this.isDragModeActive()) return false;
        float mx = Position.mouseX();
        float my = Position.mouseY();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.currentScreen instanceof UI) {
            float px = UI.panelX();
            float py = UI.panelY();
            float pw = UI.panelW();
            float ph = UI.PANEL_H;
            if (mx >= px && mx <= px + pw && my >= py && my <= py + ph) {
                return false;
            }
        }
        if (click.button() == 1) {
            if (this.activeDrag != null) {
                this.activeDrag.getDrag().cancel();
                this.activeDrag = null;
                this.savePositions();
                return true;
            }
            for (int i = this.elements.size() - 1; i >= 0; i--) {
                Draggable d = this.elements.get(i);
                if (d.isInteractive() && d.hitTest(mx, my)) {
                    d.resetToDefault();
                    this.savePositions();
                    return true;
                }
            }
            return false;
        }
        if (click.button() == 0) {
            for (int i = this.elements.size() - 1; i >= 0; i--) {
                Draggable d = this.elements.get(i);
                if (d.isInteractive() && d.getDrag().tryGrab(mx, my, d.width(), d.height())) {
                    this.activeDrag = d;
                    return true;
                }
            }
        }
        return false;
    }

    public boolean mouseDragged(Click click, double dx, double dy) {
        if (this.activeDrag != null) {
            float mx = Position.mouseX();
            float my = Position.mouseY();
            this.activeDrag.getDrag().tick(mx, my, this.activeDrag.overlayWidth(), this.activeDrag.overlayHeight(), true);
            return true;
        }
        return false;
    }

    public boolean mouseReleased(Click click) {
        if (this.activeDrag != null) {
            this.activeDrag.getDrag().release();
            this.activeDrag = null;
            this.savePositions();
            return true;
        }
        return false;
    }

    public void savePositions() {
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        if (cfg != null && this.watermark != null) {
            if (this.watermark.isCentered()) {
                cfg.hudCustomX = -1;
                cfg.hudCustomY = Math.round(this.watermark.getDrag().getTargetY());
            } else {
                cfg.hudCustomX = Math.round(this.watermark.getDrag().getTargetX());
                cfg.hudCustomY = Math.round(this.watermark.getDrag().getTargetY());
            }
            ActivityConfigManager.markDirty();
        }
    }

    public void loadSavedPositions() {
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        if (cfg != null && this.watermark != null) {
            if (cfg.hudCustomX >= 0 && cfg.hudCustomY >= 0) {
                this.watermark.getDrag().setTargetX(cfg.hudCustomX);
                this.watermark.getDrag().setTargetY(cfg.hudCustomY);
                this.watermark.getDrag().syncToTarget();
                this.watermark.setCentered(false);
            } else {
                this.watermark.setCentered(true);
                if (cfg.hudCustomY >= 0) {
                    this.watermark.getDrag().setTargetY(cfg.hudCustomY);
                    this.watermark.getDrag().syncToTarget();
                }
            }
        }
    }
}

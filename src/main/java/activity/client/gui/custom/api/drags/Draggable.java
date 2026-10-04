package activity.client.gui.custom.api.drags;

import net.minecraft.client.gui.DrawContext;

public abstract class Draggable {
    private final String id;
    private final DragController drag;
    private final float defaultX;
    private final float defaultY;
    private boolean visible = true;

    protected Draggable(String string, float f, float f2) {
        this.id = string;
        this.defaultX = f;
        this.defaultY = f2;
        this.drag = new DragController(f, f2);
    }

    public String getId() {
        return this.id;
    }

    public String displayName() {
        return this.id;
    }

    public abstract float width();

    public Position getPosition() {
        return new Position(this.drag.getTargetX(), this.drag.getTargetY());
    }

    public abstract float height();

    public float overlayHeight() {
        return this.height();
    }

    public void renderNormal(DrawContext drawContext) {
        if (!this.visible) {
            return;
        }
        this.drag.beginRender();
        float f = this.drag.getTiltAngle();
        if (Math.abs(f) < 0.01f) {
            try {
                this.render(drawContext);
            } finally {
                this.drag.endRender();
            }
            return;
        }
        float f2 = this.getX() + this.width() * 0.5f;
        float f3 = this.getY() + this.height() * 0.5f;
        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().translate(f2, f3);
        drawContext.getMatrices().rotate((float)Math.toRadians(f));
        drawContext.getMatrices().translate(-f2, -f3);
        try {
            this.render(drawContext);
        } finally {
            drawContext.getMatrices().popMatrix();
            this.drag.endRender();
        }
    }

    public boolean isInteractive() {
        return this.visible;
    }

    public float overlayWidth() {
        return this.width();
    }

    protected abstract void render(DrawContext var1);

    public boolean isVisible() {
        return this.visible;
    }

    public void resetToDefault() {
        this.drag.setTargetX(this.defaultX);
        this.drag.setTargetY(this.defaultY);
        this.drag.syncToTarget();
    }

    public boolean hitTest(float f, float f2) {
        return this.drag.isHovered(f, f2, this.width(), this.height());
    }

    public DragController getDrag() {
        return this.drag;
    }

    public float getX() {
        return this.drag.getRenderX();
    }

    public float getY() {
        return this.drag.getRenderY();
    }

    public void setVisible(boolean bl) {
        this.visible = bl;
    }
}

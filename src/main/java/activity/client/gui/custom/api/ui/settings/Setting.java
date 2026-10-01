package activity.client.gui.custom.api.ui.settings;
import net.minecraft.client.gui.DrawContext;

public interface Setting {
    public String name();
    default String description(){return "";}

    public float height();

    default public void renderOverlay(DrawContext drawContext, float f, float f2, float f3, float f4) {
    }

    public void render(float var1, float var2, float var3, float var4);

    default public boolean isVisible() {
        return true;
    }

    public boolean click(float var1, float var2, float var3, float var4, float var5);

    default public void releaseDrag() {
    }

    default public boolean middleClick(float f, float f2, float f3, float f4, float f5) {
        return false;
    }

    default public void closeOverlay() {
    }

    default public void closeOverlay(boolean playSound) {
        this.closeOverlay();
    }

    default public boolean isOverlayOpen() {
        return false;
    }

    default public boolean scrollOverlay(float f, float f2, float f3, float f4, float f5, double d) {
        return false;
    }

    default public boolean clickOverlay(float f, float f2, float f3, float f4, float f5) {
        return false;
    }

    default public boolean hasOverlay() {
        return false;
    }

    default public float preferredWidth() {
        return 120.0f;
    }
}

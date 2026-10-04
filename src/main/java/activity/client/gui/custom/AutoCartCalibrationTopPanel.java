package activity.client.gui.custom;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;

public final class AutoCartCalibrationTopPanel {
    private AutoCartCalibrationTopPanel() {}

    public static void open() {
        AutoCartCalibrationDrawer.open();
    }

    public static void close() {
        AutoCartCalibrationDrawer.close();
    }

    public static void toggle() {
        AutoCartCalibrationDrawer.toggle();
    }

    public static boolean isOpened() {
        return AutoCartCalibrationDrawer.isOpen();
    }

    public static void render(DrawContext context, int mouseX, int mouseY) {
        AutoCartCalibrationDrawer.render(context, mouseX, mouseY);
    }

    public static boolean mouseClicked(Click click) {
        return AutoCartCalibrationDrawer.mouseClicked(click);
    }
}

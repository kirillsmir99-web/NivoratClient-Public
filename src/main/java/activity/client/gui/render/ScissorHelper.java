package activity.client.gui.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2f;

public final class ScissorHelper {

    private ScissorHelper() {}

    public static void pushScissor(DrawContext context, int x, int y, int width, int height) {
        if(activity.client.gui.custom.CustomRender.active()){activity.client.gui.custom.utils.render.render2d.Render2D.pushScissor(context,x,y,width,height);return;}
        if (context == null) return;
        if (width <= 0 || height <= 0) {
            context.enableScissor(x, y, x, y);
            return;
        }

        Matrix3x2f matrix = context.getMatrices();
        if (matrix != null) {
            float x1 = x;
            float y1 = y;
            float x2 = x + width;
            float y2 = y + height;

            float tx1 = x1 * matrix.m00 + y1 * matrix.m10 + matrix.m20;
            float ty1 = x1 * matrix.m01 + y1 * matrix.m11 + matrix.m21;
            float tx2 = x2 * matrix.m00 + y2 * matrix.m10 + matrix.m20;
            float ty2 = x2 * matrix.m01 + y2 * matrix.m11 + matrix.m21;

            int minX = Math.round(Math.min(tx1, tx2));
            int minY = Math.round(Math.min(ty1, ty2));
            int maxX = Math.round(Math.max(tx1, tx2));
            int maxY = Math.round(Math.max(ty1, ty2));

            context.enableScissor(minX, minY, maxX, maxY);
        } else {
            int x2 = Math.max(x, x + width);
            int y2 = Math.max(y, y + height);
            context.enableScissor(x, y, x2, y2);
        }
    }

    public static void popScissor(DrawContext context) {
        if(activity.client.gui.custom.CustomRender.active()){activity.client.gui.custom.utils.render.render2d.Render2D.popScissor(context);return;}
        if (context == null) return;
        context.disableScissor();
    }

    public static boolean contains(DrawContext context, int x, int y) {
        if (context == null) return false;
        Matrix3x2f matrix = context.getMatrices();
        if (matrix != null) {
            float tx = x * matrix.m00 + y * matrix.m10 + matrix.m20;
            float ty = x * matrix.m01 + y * matrix.m11 + matrix.m21;
            return context.scissorContains(Math.round(tx), Math.round(ty));
        }
        return context.scissorContains(x, y);
    }

    public static boolean intersects(int compX, int compY, int compWidth, int compHeight,
                                     int viewX, int viewY, int viewWidth, int viewHeight) {
        return compX < viewX + viewWidth && compX + compWidth > viewX &&
               compY < viewY + viewHeight && compY + compHeight > viewY;
    }

    public static int[] getFramebufferScissor(MinecraftClient client, int guiX, int guiY, int guiWidth, int guiHeight) {
        if (client == null || client.getWindow() == null) {
            return new int[]{guiX, guiY, guiWidth, guiHeight};
        }
        double scale = client.getWindow().getScaleFactor();
        int fbX = (int) Math.round(guiX * scale);
        int fbWidth = (int) Math.round(guiWidth * scale);
        int fbHeight = (int) Math.round(guiHeight * scale);
        int fbY = (int) Math.round(client.getWindow().getFramebufferHeight() - (guiY + guiHeight) * scale);
        return new int[]{fbX, fbY, fbWidth, fbHeight};
    }
}

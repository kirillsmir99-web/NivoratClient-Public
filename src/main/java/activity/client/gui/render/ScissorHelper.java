package activity.client.gui.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2f;

/**
 * Utility helper for managing scissor clipping regions, viewport containment,
 * Axis-Aligned Bounding Box (AABB) visibility culling, and OpenGL coordinate transformations.
 *
 * <p>Integrates natively with Minecraft 1.21.11 {@link DrawContext} scissor stack.
 * Seamlessly transforms scissor bounds through the current {@link Matrix3x2f} matrix stack
 * so that animations and scaled/translated viewports are clipped correctly.
 */
public final class ScissorHelper {

    private ScissorHelper() {}

    /**
     * Safely pushes a scissor clipping region to the draw context in GUI coordinates,
     * mapped through the current matrix transformation (if any).
     *
     * <p>Automatically converts (x, y, width, height) to (x1, y1, x2, y2) bounds.
     * If width or height is non-positive, an empty clipping area is activated to prevent rendering.
     *
     * @param context draw context
     * @param x       left coordinate in GUI pixels
     * @param y       top coordinate in GUI pixels
     * @param width   width in GUI pixels
     * @param height  height in GUI pixels
     */
    public static void pushScissor(DrawContext context, int x, int y, int width, int height) {
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

    /**
     * Pops the topmost scissor region from the draw context.
     *
     * @param context draw context
     */
    public static void popScissor(DrawContext context) {
        if (context == null) return;
        context.disableScissor();
    }

    /**
     * Checks if a point (x, y) is inside the active scissor region on the draw context.
     *
     * @param context draw context
     * @param x       point X
     * @param y       point Y
     * @return true if the point falls inside the current scissor stack
     */
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

    /**
     * Axis-Aligned Bounding Box (AABB) intersection test.
     * Used for culling off-screen components before rendering or dispatching input.
     *
     * @param compX      component left
     * @param compY      component top
     * @param compWidth  component width
     * @param compHeight component height
     * @param viewX      viewport left
     * @param viewY      viewport top
     * @param viewWidth  viewport width
     * @param viewHeight viewport height
     * @return true if the component overlaps the viewport
     */
    public static boolean intersects(int compX, int compY, int compWidth, int compHeight,
                                     int viewX, int viewY, int viewWidth, int viewHeight) {
        return compX < viewX + viewWidth && compX + compWidth > viewX &&
               compY < viewY + viewHeight && compY + compHeight > viewY;
    }

    /**
     * Converts logical GUI coordinates to OpenGL framebuffer coordinates.
     * Handles GUI Scale factor and OpenGL's bottom-left origin coordinate system.
     *
     * @param client    Minecraft client instance
     * @param guiX      logical X
     * @param guiY      logical Y
     * @param guiWidth  logical width
     * @param guiHeight logical height
     * @return an integer array representing [fbX, fbY, fbWidth, fbHeight]
     */
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

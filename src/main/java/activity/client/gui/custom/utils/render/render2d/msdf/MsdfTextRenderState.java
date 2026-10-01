package activity.client.gui.custom.utils.render.render2d.msdf;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import activity.client.gui.custom.utils.render.render2d.msdf.MsdfTextRenderer;

final class MsdfTextRenderState
implements SimpleGuiElementRenderState {
    private final Matrix3x2f pose;
    private final TextureSetup textureSetup;
    private final ScreenRect scissorArea;
    private final boolean shimmer;
    private final boolean wave;
    private static final int SEGMENT_SIZE = 16;
    private static final class Segment {
        final float[] geometry = new float[14 * SEGMENT_SIZE];
        final int[] colors = new int[4 * SEGMENT_SIZE];
        int count;
        Segment next;
    }
    private Segment first;
    private Segment last;
    private int quadCount;
    private float rotationAngle = Float.NaN;
    private float rotationSin;
    private float rotationCos;
    private ScreenRect cachedBounds;
    private boolean boundsValid;
    private float minX = Float.MAX_VALUE;
    private float minY = Float.MAX_VALUE;
    private float maxX = -3.4028235E38f;
    private float maxY = -3.4028235E38f;

    MsdfTextRenderState(Matrix3x2f matrix3x2f, TextureSetup textureSetup, ScreenRect screenRect, boolean bl, boolean bl2) {
        this.pose = new Matrix3x2f((Matrix3x2fc)matrix3x2f);
        this.textureSetup = textureSetup;
        this.scissorArea = screenRect;
        this.shimmer = bl;
        this.wave = bl2;
    }

    private void include(float f, float f2) {
        this.minX = Math.min(this.minX, f);
        this.minY = Math.min(this.minY, f2);
        this.maxX = Math.max(this.maxX, f);
        this.maxY = Math.max(this.maxY, f2);
    }

    public ScreenRect bounds() {
        if (boundsValid) return cachedBounds;
        cachedBounds = calculateBounds();
        boundsValid = true;
        return cachedBounds;
    }

    private ScreenRect calculateBounds() {
        if (this.quadCount == 0) {
            return new ScreenRect(0, 0, 1, 1);
        }
        int n = (int)Math.floor(this.minX);
        int n2 = (int)Math.floor(this.minY);
        int n3 = Math.max(1, (int)Math.ceil(this.maxX - this.minX));
        int n4 = Math.max(1, (int)Math.ceil(this.maxY - this.minY));
        ScreenRect screenRect = new ScreenRect(n, n2, n3, n4).transformEachVertex((Matrix3x2fc)(Object)this.pose);
        return this.scissorArea == null ? screenRect : this.scissorArea.intersection(screenRect);
    }

    public RenderPipeline pipeline() {
        if (this.wave) {
            return MsdfTextRenderer.MSDF_WAVE_PIPELINE;
        }
        return this.shimmer ? MsdfTextRenderer.MSDF_SHIMMER_PIPELINE : MsdfTextRenderer.MSDF_PIPELINE;
    }

    public ScreenRect scissorArea() {
        return this.scissorArea;
    }

    public TextureSetup textureSetup() {
        return this.textureSetup;
    }

    public void setupVertices(VertexConsumer vertices) {
        for (Segment segment = first; segment != null; segment = segment.next) {
            float[] geometry = segment.geometry;
            int[] colors = segment.colors;
            for (int i = 0; i < segment.count; i++) {
            int g = i * 14, c = i * 4;
            emit(vertices, geometry[g], geometry[g + 1], geometry[g + 8], geometry[g + 9], colors[c], geometry[g + 12]);
            emit(vertices, geometry[g + 2], geometry[g + 3], geometry[g + 8], geometry[g + 11], colors[c + 3], geometry[g + 12]);
            emit(vertices, geometry[g + 4], geometry[g + 5], geometry[g + 10], geometry[g + 11], colors[c + 2], geometry[g + 13]);
            emit(vertices, geometry[g + 6], geometry[g + 7], geometry[g + 10], geometry[g + 9], colors[c + 1], geometry[g + 13]);
            }
        }
    }

    private void emit(VertexConsumer vertices, float x, float y, float u, float v, int color, float lineWidth) {
        if (shimmer || wave) shimmerVertex(vertices, x, y, u, v, color, lineWidth);
        else vertex(vertices, x, y, u, v, color);
    }

    private void vertex(VertexConsumer vertexConsumer, float f, float f2, float f3, float f4, int n) {
        vertexConsumer.vertex((Matrix3x2fc)(Object)this.pose, f, f2).texture(f3, f4).color(n);
    }

    void addGlyph(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int n, int n2, int n3, int n4, float f9, float f10, float f11, float f12, float f13) {
        float f14 = f;
        float f15 = f2;
        float f16 = f;
        float f17 = f4;
        float f18 = f3;
        float f19 = f4;
        float f20 = f3;
        float f21 = f2;
        if (Math.abs(f11) >= 0.001f) {
            if (Float.compare(rotationAngle, f11) != 0) {
                float radians = (float)Math.toRadians(f11);
                rotationSin = (float)Math.sin(radians);
                rotationCos = (float)Math.cos(radians);
                rotationAngle = f11;
            }
            float f23 = rotationSin;
            float f24 = rotationCos;
            f14 = f12 + (f - f12) * f24 - (f2 - f13) * f23;
            f15 = f13 + (f - f12) * f23 + (f2 - f13) * f24;
            f16 = f12 + (f - f12) * f24 - (f4 - f13) * f23;
            f17 = f13 + (f - f12) * f23 + (f4 - f13) * f24;
            f18 = f12 + (f3 - f12) * f24 - (f4 - f13) * f23;
            f19 = f13 + (f3 - f12) * f23 + (f4 - f13) * f24;
            f20 = f12 + (f3 - f12) * f24 - (f2 - f13) * f23;
            f21 = f13 + (f3 - f12) * f23 + (f2 - f13) * f24;
        }
        if (last == null || last.count == SEGMENT_SIZE) {
            Segment segment = new Segment();
            if (last == null) first = segment;
            else last.next = segment;
            last = segment;
        }
        float[] geometry = last.geometry;
        int[] colors = last.colors;
        int g = last.count * 14, c = last.count * 4;
        last.count++;
        geometry[g] = f14; geometry[g + 1] = f15;
        geometry[g + 2] = f16; geometry[g + 3] = f17;
        geometry[g + 4] = f18; geometry[g + 5] = f19;
        geometry[g + 6] = f20; geometry[g + 7] = f21;
        geometry[g + 8] = f5; geometry[g + 9] = f6;
        geometry[g + 10] = f7; geometry[g + 11] = f8;
        geometry[g + 12] = f9; geometry[g + 13] = f10;
        colors[c] = n; colors[c + 1] = n2; colors[c + 2] = n3; colors[c + 3] = n4;
        quadCount++;
        boundsValid = false;
        this.include(f14, f15);
        this.include(f16, f17);
        this.include(f18, f19);
        this.include(f20, f21);
    }

    private void shimmerVertex(VertexConsumer vertexConsumer, float f, float f2, float f3, float f4, int n, float f5) {
        vertexConsumer.vertex((Matrix3x2fc)(Object)this.pose, f, f2).texture(f3, f4).color(n).lineWidth(f5);
    }


}

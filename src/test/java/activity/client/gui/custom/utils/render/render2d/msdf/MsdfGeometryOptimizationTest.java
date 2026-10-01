package activity.client.gui.custom.utils.render.render2d.msdf;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MsdfGeometryOptimizationTest {
    @Test void growingPackedBuffersPreserveVertexAndGradientOrdering() {
        var state = new MsdfTextRenderState(new Matrix3x2f(), TextureSetup.empty(), null, false, false);
        for (int i = 0; i < 100; i++) state.addGlyph(i,2,i+3,6,0,0,1,1,11,22,33,44,0,1,0,0,0);
        var vertices = new ArrayList<float[]>();
        var colors = new ArrayList<Integer>();
        var consumer = (VertexConsumer) Proxy.newProxyInstance(VertexConsumer.class.getClassLoader(),new Class<?>[]{VertexConsumer.class},(proxy,method,args)->{
            if (method.getName().equals("vertex") && args.length == 3 && args[0] instanceof org.joml.Matrix3x2fc) vertices.add(new float[]{(float)args[1],(float)args[2]});
            if (method.getName().equals("color") && args.length == 1) colors.add((int)args[0]);
            return method.getReturnType()==VertexConsumer.class ? proxy : null;
        });
        state.setupVertices(consumer);
        assertEquals(400,vertices.size());
        assertArrayEquals(new float[]{0,2},vertices.getFirst());
        assertArrayEquals(new float[]{99,6},vertices.get(397));
        assertArrayEquals(new float[]{102,6},vertices.get(398));
        assertEquals(java.util.List.of(11,44,33,22),colors.subList(0,4));
        assertEquals(102,state.bounds().width());
    }

    @Test void rotatedGlyphBoundsCoverAllCorners() {
        var state = new MsdfTextRenderState(new Matrix3x2f(),TextureSetup.empty(),null,false,false);
        state.addGlyph(0,0,4,2,0,0,1,1,0,0,0,0,0,1,90,0,0);
        var bounds=state.bounds();
        assertTrue(bounds.position().x()<=-2);
        assertTrue(bounds.width()>=2 && bounds.height()>=4);
    }
}

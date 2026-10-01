package activity.client.gui.custom.utils.render.render2d.msdf;

import net.minecraft.util.Identifier;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MsdfFontOptimizationTest {
    @Test void cachedMetricsPreserveKerningNewlinesFallbackAndScaleChanges() {
        var a = MsdfGlyph.nonDrawable(.5f);
        var b = MsdfGlyph.nonDrawable(.7f);
        var fallback = MsdfGlyph.nonDrawable(.3f);
        var font = new MsdfFont(Identifier.of("activity","test"),1,1,1,1,0,Map.of(65,a,66,b,63,fallback,0x1f600,b),Map.of(((long)65<<32)|66,-.1f));
        for (float size : new float[]{6.5f,7f,6.5f,0f}) {
            assertEquals((.5f-.1f+.7f)*size,font.width("AB",size),.00001f);
            assertEquals(.7f*size,font.width("A\nB",size),.00001f);
            assertEquals(.3f*size,font.width("Ж",size),.00001f);
            assertEquals(.7f*size,font.width("😀",size),.00001f);
            for(int i=0;i<400;i++)font.width("AB"+i,size);
            assertEquals((.5f-.1f+.7f)*size,font.width("AB",size),.00001f);
        }
    }
}

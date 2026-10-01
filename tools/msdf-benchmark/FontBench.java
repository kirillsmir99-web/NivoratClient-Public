package activity.client.gui.custom.utils.render.render2d.msdf;

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;

public final class FontBench {
    private static volatile float sink;
    private static final ThreadMXBean memory = (ThreadMXBean) ManagementFactory.getThreadMXBean();
    private static final long thread = Thread.currentThread().threadId();

    private static void measure(String name, int iterations, Runnable action) {
        for (int i=0;i<3000;i++) action.run();
        long bytes=memory.getThreadAllocatedBytes(thread), start=System.nanoTime();
        for (int i=0;i<iterations;i++) action.run();
        long duration=System.nanoTime()-start, allocated=memory.getThreadAllocatedBytes(thread)-bytes;
        System.out.printf(java.util.Locale.ROOT,"BENCH %s %.1f ns/op %.1f bytes/op%n",name,(double)duration/iterations,(double)allocated/iterations);
    }

    public static void main(String[] args) {
        memory.setThreadAllocatedMemoryEnabled(true);
        Map<Integer,MsdfGlyph> glyphs=new HashMap<>();
        for(int i=32;i<0x530;i++)glyphs.put(i,MsdfGlyph.nonDrawable(.6f));
        var font=new MsdfFont(Identifier.of("activity","benchmark"),1,1,1,1,0,glyphs,Map.of());
        measure("repeat-width",100000,()->sink=font.width("Настройки модулей и анимации",6.5f));
        measure("glyph-stream",100000,()->{float total=0;for(int i=0x410;i<0x430;i++)total+=font.glyph(i).advance();sink=total;});
        var pose=new Matrix3x2f();
        var texture=TextureSetup.empty();
        for(float rotation:new float[]{0,25}){
            measure("geometry-"+rotation,10000,()->{
                var state=new MsdfTextRenderState(pose,texture,null,false,false);
                for(int i=0;i<80;i++)state.addGlyph(i,0,i+1,2,0,0,1,1,1,2,3,4,0,1,rotation,0,0);
                sink=state.bounds().width();
            });
        }
    }
}

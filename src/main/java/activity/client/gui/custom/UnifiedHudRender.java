package activity.client.gui.custom;

import activity.client.gui.custom.api.ui.theme.ClientAccent;
import activity.client.gui.custom.api.ui.theme.ThemeManager;
import activity.client.gui.custom.utils.render.fonts.Fonts;
import activity.client.gui.custom.utils.render.others.RectUtil;
import activity.client.gui.custom.utils.render.render2d.Render2D;
import activity.client.gui.custom.utils.render.render2d.Render2DCoordinateSpace;
import net.minecraft.client.gui.DrawContext;

public final class UnifiedHudRender {
    public static final float FONT_SIZE=8f;
    private UnifiedHudRender() {}
    public static Scope begin(DrawContext context){return new Scope(context, false);}
    public static Scope beginNative(DrawContext context){return new Scope(context, true);}
    public static float textWidth(String text){return Fonts.MONTSERRAT_MEDIUM.width(text,FONT_SIZE);}
    public static int measure(String text){
        if(net.minecraft.client.MinecraftClient.getInstance()==null)return Math.max(1,text.length()*5);
        return (int)Math.ceil(textWidth(text));
    }
    public static void card(float x,float y,float width,float height){
        RectUtil.drawClientRectFixedRadius(x,y,width,height,5f,1f,0);
        Render2D.outline(x,y,width,height,5f,0.6f,ClientAccent.accent(130f));
    }
    public static void text(String value,float x,float y,int color){Fonts.MONTSERRAT_MEDIUM.draw(value,x,y,FONT_SIZE,color);}
    public static void progress(float x,float y,float width,float value){
        Render2D.rect(x,y,width,1.5f,0.75f,ThemeManager.rgba(0xffffff,30));
        if(value>0)Render2D.rect(x,y,width*Math.clamp(value,0f,1f),1.5f,0.75f,ClientAccent.accentBright(240));
    }
    public static final class Scope implements AutoCloseable {
        private final DrawContext context;private final boolean own;
        private Scope(DrawContext context, boolean nativeCoordinates){
            this.context=context;own=!CustomRender.active();
            CustomRender.init();VisualSettingsStore.load();
            context.getMatrices().pushMatrix();
            float inverse=nativeCoordinates ? 1f : 1f/Render2DCoordinateSpace.guiIndependentScale();
            context.getMatrices().scaleLocal(inverse,inverse);
            if(own)CustomRender.enter(context);
        }
        @Override public void close(){try{if(own)CustomRender.leave();}finally{context.getMatrices().popMatrix();}}
    }
}

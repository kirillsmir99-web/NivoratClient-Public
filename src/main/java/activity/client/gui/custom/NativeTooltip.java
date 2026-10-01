package activity.client.gui.custom;

import activity.client.gui.custom.api.drags.Position;
import activity.client.gui.custom.api.ui.theme.ClientAccent;
import activity.client.gui.custom.utils.render.fonts.Fonts;
import net.minecraft.client.gui.DrawContext;
import activity.client.gui.custom.mixin.accessor.GuiGraphicsExtractorAccessor;
import activity.client.gui.custom.api.ui.theme.ThemeManager;
import activity.client.gui.custom.utils.render.render2d.Render2D;
import java.util.*;

public final class NativeTooltip {
    private static String cachedDescription;
    private static float cachedWidth;
    private static List<String> cachedLines = List.of();
    private NativeTooltip() {}
    public static List<String> wrap(String text,float width,float size){
        List<String> lines=new ArrayList<>();String line="";
        for(String paragraph:text.split("\\n",-1)){
            for(String word:paragraph.split(" +")){
                String next=line.isEmpty()?word:line+" "+word;
                if(!line.isEmpty()&&Fonts.MONTSERRAT_MEDIUM.width(next,size)>width){lines.add(line);line=word;}else line=next;
                while(Fonts.MONTSERRAT_MEDIUM.width(line,size)>width&&line.codePointCount(0,line.length())>1){
                    int count=1,best=1;while(count<line.codePointCount(0,line.length())){int end=line.offsetByCodePoints(0,count);if(Fonts.MONTSERRAT_MEDIUM.width(line.substring(0,end),size)>width)break;best=end;count++;}
                    lines.add(line.substring(0,best));line=line.substring(best);
                }
            }
            if(!line.isEmpty()){lines.add(line);line="";}
        }
        return lines;
    }
    public static void draw(DrawContext context,String title,String description,String category,float alpha){
        if(description==null||description.isBlank()||alpha<=.01f)return;
        float width=Math.min(185f,Position.screenWidth()-16f);
        if(!description.equals(cachedDescription)||Float.compare(width,cachedWidth)!=0){
            cachedDescription=description;cachedWidth=width;cachedLines=List.copyOf(wrap(description,width-20f,6.5f));
        }
        List<String> lines=cachedLines;
        boolean footer=category!=null&&!category.isBlank();
        float fixedHeight=footer?43f:28f;
        int visible=Math.min(lines.size(),Math.max(1,(int)((Position.screenHeight()-16f-fixedHeight)/10f)));
        float height=fixedHeight+visible*10f;
        var placement=TooltipPlacement.place(Position.mouseX(),Position.mouseY(),width,height,Position.screenWidth(),Position.screenHeight());
        float x=placement.x(),y=placement.y();
        Render2D.flush();
        ((GuiGraphicsExtractorAccessor)context).nv_getGuiRenderState().createNewRootLayer();
        Render2D.beginFrame(context);
        Render2D.rect(x+1,y+2,width,height,7f,ThemeManager.rgba(0x000000,50f*alpha));
        Render2D.rect(x,y,width,height,7f,ThemeManager.rgba(0x11131b,250f*alpha));
        Render2D.outline(x,y,width,height,7f,.6f,ClientAccent.accentSoft(85f*alpha));
        Render2D.rect(x+9,y+9,2f,8f,1f,ClientAccent.accentBright(240f*alpha));
        Fonts.MONTSERRAT_MEDIUM.draw(CustomRender.fit(title,width-28f,7f),x+16,y+8,7f,(Math.round(245*alpha)<<24)|0xffffff);
        float ty=y+25;
        for(int i=0;i<visible;i++){
            String line=i==visible-1&&visible<lines.size()?CustomRender.fit(lines.get(i)+"…",width-20f,6.5f):lines.get(i);
            Fonts.MONTSERRAT_MEDIUM.draw(line,x+10,ty,6.5f,(Math.round(235*alpha)<<24)|0xd9dfe9);
            ty+=10;
        }
        if(footer){
            Render2D.rect(x+10,ty+1,width-20f,.5f,0f,ThemeManager.rgba(0xffffff,22f*alpha));
            Fonts.MONTSERRAT_MEDIUM.draw(category,x+10,ty+5,5.5f,ClientAccent.accentSoft(215f*alpha));
        }
    }
}

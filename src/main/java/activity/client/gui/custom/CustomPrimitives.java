package activity.client.gui.custom;
public final class CustomPrimitives {
 private CustomPrimitives(){}
 public static void fill(net.minecraft.client.gui.DrawContext c,int x,int y,int right,int bottom,int color){
 if(CustomRender.active())CustomRender.fill(c,x,y,right,bottom,color);else c.fill(x,y,right,bottom,color);
 }
}

package activity.client.gui.custom;
import net.minecraft.client.gui.DrawContext;
import activity.client.gui.custom.utils.render.render2d.Render2D;
import activity.client.gui.custom.utils.render.others.RectUtil;
import activity.client.gui.custom.utils.render.fonts.Fonts;
import activity.client.gui.custom.api.ui.theme.*;
import activity.client.gui.theme.ThemePreset;
public final class CustomRender {
 private static boolean active;
 private static String themeId;
 private static boolean initialized;
 public static boolean active() { return active; }
 public static void enter(DrawContext context) { active=true; ThemeManager.beginFrame(); ClientAccent.beginFrame(); activity.client.gui.custom.utils.render.render2d.ClientPalette.update(); Render2D.beginFrame(context); }
 public static void leave() { Render2D.flush(); active=false; }
 public static void enterLayout() { active=true; }
 public static void leaveLayout() { active=false; }
 public static void init() {
  if(initialized) return;
  Render2D.init();
  activity.client.gui.custom.utils.sounds.SoundManager.init();
  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(client -> { Render2D.close(); activity.client.gui.custom.utils.render.render2d.ClientSplits.closeBuffer(); });
  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> VisualSettingsStore.tick());
  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(client -> VisualSettingsStore.save());
  initialized=true;
 }
 public static void theme(ThemePreset preset) {
  if(preset.id().equals(themeId)) return;
  themeId=preset.id();
  if(preset==ThemePreset.CLIENT) ThemeManager.set(Theme.CLIENT);
  else ThemeManager.set(Theme.valueOf(preset.name()));
 }
 public static void rect(DrawContext c,float x,float y,float w,float h,float r,int color) { if(w>0 && h>0) Render2D.rect(x,y,w,h,r,color); }
 public static void fill(DrawContext c,int x,int y,int right,int bottom,int color) { rect(c,x,y,right-x,bottom-y,0,color); }
 public static void outline(DrawContext c,float x,float y,float w,float h,float r,int color) { Render2D.outline(x,y,w,h,r,0.6f,color); }
 public static void panel(DrawContext c,float x,float y,float w,float h,float r,float a) { RectUtil.drawClientRectFixedRadius(x,y,w,h,r,a,0); }
 public static void text(DrawContext c,String s,float x,float y,float size,int color) { Fonts.MONTSERRAT_MEDIUM.draw(s,x,y,size,color); }
 public static int width(String s,float size) { return Math.round(Fonts.MONTSERRAT_MEDIUM.width(s,size)); }
 public static String fit(String s,float w,float size) {
  if(s==null || w<=0)return "";
  if(width(s,size)<=w)return s;
  int low=0,high=s.codePointCount(0,s.length()),best=0;
  while(low<=high){int mid=(low+high)>>>1,end=s.offsetByCodePoints(0,mid);if(width(s.substring(0,end)+"…",size)<=w){best=end;low=mid+1;}else high=mid-1;}
  return best==0?"":s.substring(0,best)+"…";
 }
 private CustomRender() {}
}

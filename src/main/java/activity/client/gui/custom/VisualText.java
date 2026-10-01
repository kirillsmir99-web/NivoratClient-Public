package activity.client.gui.custom;
import activity.client.i18n.LocalizationService;
import activity.client.module.api.IModule;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
public final class VisualText {
 private VisualText(){}
 public static String category(activity.client.gui.custom.api.modules.Module module){
  if(module.delegate==null)return module.getCategory().getDisplayName();
  boolean ru="ru".equals(language());return switch(module.delegate.getCategory()){
   case COMBAT -> ru?"Бой":"Combat";case DEFENSE -> ru?"Защита":"Defense";case UTILITY -> ru?"Утилиты и HUD":"Utility and HUD";default -> module.delegate.getCategory().name();};
 }

 public static String bilingual(String ru,String en){return "ru".equals(language())?ru:en;}
 public static String language(){return LocalizationService.isRussianPreferred()?"ru":"en";}
 public static String resolve(Text text){return resolve(text,language());}
 public static String resolve(Text text,String language){
  if(text==null)return "";
  String value;
  if(text.getContent() instanceof TranslatableTextContent t){
   value=LocalizationService.getForLanguage(t.getKey(),t.getFallback()==null?t.getKey():t.getFallback(),language);
   if(t.getArgs().length>0){Object[] args=new Object[t.getArgs().length];for(int i=0;i<args.length;i++)args[i]=t.getArgs()[i] instanceof Text a?resolve(a,language):t.getArgs()[i];try{value=String.format(value,args);}catch(java.util.IllegalFormatException ignored){}}
  }else value=text.getContent() instanceof net.minecraft.text.PlainTextContent p?p.string():text.getString();
  if(text.getSiblings().isEmpty())return safe(value,language);
  StringBuilder result=new StringBuilder(value);for(Text sibling:text.getSiblings())result.append(resolve(sibling,language));return safe(result.toString(),language);
 }
 private static final java.util.regex.Pattern FORMATTING=java.util.regex.Pattern.compile("§[0-9a-fk-or]");
 public static String safe(String text,String language){
  if(text.indexOf('&')>=0)text=text.replace("&","ru".equals(language)?"и":"and");
  if(text.indexOf('\'')>=0)text=text.replace("'","’");
  if(text.indexOf('>')>=0)text=text.replace(">","›");
  if(text.indexOf('✅')>=0)text=text.replace("✅","");
  if(text.indexOf('✓')>=0)text=text.replace("✓","");
  return text.indexOf('§')>=0?FORMATTING.matcher(text).replaceAll(""):text;
 }
 public static String moduleName(IModule module){return moduleName(module,language());}
 public static String moduleName(IModule module,String language){return safe(LocalizationService.getForLanguage("activity.module."+module.getId()+".name",resolve(module.getName(),language),language),language);}
 public static String moduleDescription(IModule module){return moduleDescription(module,language());}
 public static String moduleDescription(IModule module,String language){return safe(LocalizationService.getForLanguage("activity.module."+module.getId()+".desc",resolve(module.getDescription(),language),language),language);}
 public static String settingName(IModule module,activity.client.module.setting.Setting<?> setting){return settingName(module,setting,language());}
 public static String settingName(IModule module,activity.client.module.setting.Setting<?> setting,String language){return safe(LocalizationService.getForLanguage("activity.visual."+module.getId()+"."+setting.getId()+".name",resolve(setting.getName(),language),language),language);}
 public static String settingDescription(IModule module,activity.client.module.setting.Setting<?> setting){return settingDescription(module,setting,language());}
 public static String settingDescription(IModule module,activity.client.module.setting.Setting<?> setting,String language){return safe(LocalizationService.getForLanguage("activity.visual."+module.getId()+"."+setting.getId()+".desc",resolve(setting.getDescription(),language),language),language);}
}

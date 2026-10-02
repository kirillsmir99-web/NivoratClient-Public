package activity.client.gui.custom.api.modules;
import activity.client.gui.custom.api.modules.settings.Setting;
import activity.client.gui.custom.utils.key.KeyBind;
import activity.client.module.api.IModule;
public class Module implements activity.client.gui.custom.IMinecraft {
 public enum BindMode {TOGGLE,HOLD}
 public static class Settings extends java.util.ArrayList<Setting>{public java.util.List<Setting> all(){return this;}}
 protected final Settings settings=new Settings();
 private final String name,description; private final Category category; private boolean enabled=true;private KeyBind bind=KeyBind.NONE;
 public final IModule delegate;
 public Module(String name,String description,Category category){this.name=name;this.description=description;this.category=category;delegate=null;}
 public Module(IModule delegate){this.name=delegate.getId();this.description=activity.client.gui.custom.VisualText.moduleDescription(delegate);this.category=Category.VISUALS;this.delegate=delegate;settings.addAll(activity.client.gui.custom.SettingsBridge.models(delegate));}
 public <T extends Setting> T register(T setting){settings.add(setting);setting.setChangeListener(activity.client.gui.custom.api.config.ConfigManager::markDirty);return setting;}
 public String getName(){return name;} public String getId(){return delegate!=null?delegate.getId():name;} public String getDisplayName(){return delegate==null?activity.client.gui.custom.api.localization.Lang.translateModule(name,activity.client.gui.custom.api.localization.Lang.translateSetting(name)):activity.client.gui.custom.VisualText.moduleName(delegate);}
 public String getDescription(){return delegate==null?activity.client.gui.custom.api.localization.Lang.translateModuleDesc(name,description):activity.client.gui.custom.VisualText.moduleDescription(delegate);}
 public Category getCategory(){return category;}public Settings getSettings(){return settings;}
 public boolean defaultEnabled(){return true;}public boolean isEnabled(){return delegate==null?enabled:delegate.isEnabled();}
 public void setEnabled(boolean value){if(delegate==null)enabled=value;else{delegate.setEnabled(value);delegate.saveToConfig(activity.client.config.ActivityConfigManager.getConfig());activity.client.config.ActivityConfigManager.markDirty();}}
 public void toggle(){setEnabled(!isEnabled());var sounds=ModuleManager.get().get(activity.client.gui.custom.api.modules.impl.Utils.ClientSounds.class);sounds.onModuleToggle(this,isEnabled());ModuleManager.get().get(activity.client.gui.custom.api.modules.impl.Interface.NotificationsModule.class).onModuleToggle(this,isEnabled());}
 public KeyBind getBind(){if(delegate==null)return bind;int code=delegate.getKeybind().getKeyCode();return code<=activity.client.module.keybind.Keybind.MOUSE_OFFSET?KeyBind.mouse(activity.client.module.keybind.Keybind.MOUSE_OFFSET-code):KeyBind.keyboard(code);}
 public void setBind(KeyBind value){
  if(delegate==null){bind=value;return;}
  var current=delegate.getKeybind();
  int code=value.getCode();
  if(value.getType()==activity.client.gui.custom.utils.key.InputType.MOUSE)code=activity.client.module.keybind.Keybind.MOUSE_OFFSET-(code==1002?2:code);
  var next=new activity.client.module.keybind.Keybind(code);
  activity.client.gui.custom.NativeBindAssignment.request(current,next,()->{
   current.copyFrom(next);delegate.saveToConfig(activity.client.config.ActivityConfigManager.getConfig());activity.client.config.ActivityConfigManager.markDirty();
  });
 }

 public BindMode getBindMode(){return BindMode.TOGGLE;}public void setBindMode(BindMode mode){if(mode==BindMode.HOLD)activity.client.gui.overlay.ClientNotification.show(net.minecraft.text.Text.literal(activity.client.i18n.LocalizationService.isRussianPreferred()?"Этот модуль использует переключение по клавише":"This module uses toggle key bindings"));}
}

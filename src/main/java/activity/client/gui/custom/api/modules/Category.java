package activity.client.gui.custom.api.modules;
public enum Category {
 VISUALS("Все","All"), NPOT("НПОТ","NPOT"),CRYSTAL("КПВП","Crystal"),UHC("УХК","UHC"),SMP("СМП","SMP"),MACE("Мейсы","Mace"),BEAST("Бисты","Beast"),SWORD("OP","OP"),AXE("Топоры","Axe"),DPOT("ДПОТ","DPOT"),CART("КАРТ","Cart"),DISPLAY("Настройки","Settings"),UTILS("Утилиты","Utility"),PINNED("Закреплённые","Pinned"),THEMES("Темы","Themes"),PRESETS("Пресеты","Presets");
 private final String ru,en; Category(String ru,String en){this.ru=ru;this.en=en;}
 public String getDisplayName(){return activity.client.i18n.LocalizationService.isRussianPreferred()?ru:en;}
 public String toString(){return getDisplayName();}
}

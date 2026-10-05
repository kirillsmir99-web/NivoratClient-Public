package activity.client.gui.custom.api.modules.impl.Interface;

import activity.client.gui.custom.api.config.ConfigManager;
import activity.client.gui.custom.api.modules.Category;
import activity.client.gui.custom.api.modules.Module;
import activity.client.gui.custom.api.modules.settings.impl.ModeSetting;
import activity.client.gui.custom.api.modules.settings.impl.SeparatorSetting;
import activity.client.gui.custom.api.modules.settings.impl.SliderSetting;
import activity.client.gui.custom.utils.key.KeyBind;

public final class ClickGui extends Module {
    public static final String SCALE_AUTO = "Авто";
    public static final String SCALE_MINECRAFT = "По Minecraft";
    public static final String SCALE_CUSTOM = "Свой";

    private final SeparatorSetting menuSeparator = this.register(new SeparatorSetting("Меню"));
    public final activity.client.gui.custom.api.modules.settings.impl.BindSetting menuKey = this.register(
        new activity.client.gui.custom.api.modules.settings.impl.BindSetting("Клавиша меню", "Клавиша для открытия и закрытия меню клиента")
    );
    public final activity.client.gui.custom.api.modules.settings.impl.StringSetting menuCommandSetting = this.register(
        new activity.client.gui.custom.api.modules.settings.impl.StringSetting("Команда открытия", "Кастомная команда чата для открытия меню (например, /menu). Оставьте пустой для отключения.", "", 32)
    );

    public final SeparatorSetting networkSeparator = this.register(new SeparatorSetting("Сеть и защита"));
    public final activity.client.gui.custom.api.modules.settings.impl.BooleanSetting srpSpoofSetting = this.register(
        new activity.client.gui.custom.api.modules.settings.impl.BooleanSetting("Спуфер ресурс-пака", "Не скачивать серверный ресурс-пак. Внимание: выключите при игре на серверах с проверкой уникальных токен-ссылок.", false)
    );

    public final SeparatorSetting scaleSeparator = this.register(new SeparatorSetting("Масштаб"));

    public final ModeSetting scaleMode = this.register(new ModeSetting(
        "Масштаб",
        "Вариант масштабирования интерфейса меню.",
        SCALE_AUTO,
        SCALE_AUTO, SCALE_MINECRAFT, SCALE_CUSTOM
    ));

    public final SliderSetting customScale = this.register(new SliderSetting(
        "Размер",
        "Пользовательский размер интерфейса меню в процентах."
    ).range(0.70f, 1.30f).increment(0.05f).setValue(1.0f).visible(() -> this.scaleMode.is(SCALE_CUSTOM)));

    public ClickGui() {
        super("ClickGui", "Клавиша открытия меню и масштаб интерфейса.", Category.DISPLAY);

        activity.client.module.setting.KeybindSetting ks = new activity.client.module.setting.KeybindSetting(
            "menu_keybind",
            net.minecraft.text.Text.literal("Клавиша меню"),
            net.minecraft.text.Text.literal("Клавиша для открытия и закрытия меню клиента"),
            (activity.client.module.setting.SettingSection) null,
            new activity.client.module.keybind.Keybind(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT),
            () -> activity.client.config.ActivityConfigManager.getConfig().menuKeybind,
            kb -> {
                var cfg = activity.client.config.ActivityConfigManager.getConfig();
                cfg.menuKeybind.copyFrom(kb);
                activity.client.ActivityClient.syncOpenMenuKey();
                activity.client.config.ActivityConfigManager.markDirty();
                activity.client.config.ActivityConfigManager.save();
            }
        );
        this.menuKey.setSource(ks);
        var initialCfg = activity.client.config.ActivityConfigManager.getConfig();
        if (initialCfg != null && initialCfg.menuCommand != null) {
            this.menuCommandSetting.setText(initialCfg.menuCommand);
        }
        this.menuCommandSetting.setChangeListener(() -> {
            var cfg = activity.client.config.ActivityConfigManager.getConfig();
            if (cfg != null) {
                String txt = this.menuCommandSetting.getText();
                String clean = txt != null ? txt.trim() : "";
                while (clean.startsWith("/")) clean = clean.substring(1).trim();
                cfg.menuCommand = clean;
                activity.client.config.ActivityConfigManager.markDirty();
                activity.client.config.ActivityConfigManager.save();
            }
        });
        this.srpSpoofSetting.setValue(initialCfg != null && initialCfg.srpSpoof);
        this.srpSpoofSetting.setChangeListener(() -> {
            var cfg = activity.client.config.ActivityConfigManager.getConfig();
            if (cfg != null) {
                boolean val = this.srpSpoofSetting.getValue();
                cfg.srpSpoof = val;
                activity.client.config.ActivityConfigManager.markDirty();
                activity.client.config.ActivityConfigManager.save();
            }
        });
        this.scaleMode.setChangeListener(ConfigManager::markDirty);
        this.customScale.setChangeListener(ConfigManager::markDirty);
    }

    @Override
    public String getDisplayName() {
        return activity.client.i18n.LocalizationService.isRussianPreferred() ? "Меню и спуфер" : "Menu & Spoofer";
    }

    @Override
    public String getDescription() {
        return activity.client.i18n.LocalizationService.isRussianPreferred()
            ? "Клавиша открытия меню, масштаб интерфейса и спуфер ресурс-пака."
            : "Menu keybind, UI scale, and server resource pack spoofer.";
    }
 @Override public KeyBind getBind() {
     var cfg = activity.client.config.ActivityConfigManager.getConfig();
     if (cfg == null || cfg.menuKeybind == null) return new KeyBind(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
     int code = cfg.menuKeybind.getKeyCode();
     return code <= activity.client.module.keybind.Keybind.MOUSE_OFFSET
         ? KeyBind.mouse(activity.client.module.keybind.Keybind.MOUSE_OFFSET - code)
         : KeyBind.keyboard(code);
 }
 @Override public void setBind(KeyBind bind) {
     var cfg = activity.client.config.ActivityConfigManager.getConfig();
     if (cfg == null) return;
     int code = bind.getCode();
     if (bind.getType() == activity.client.gui.custom.utils.key.InputType.MOUSE) {
         code = activity.client.module.keybind.Keybind.MOUSE_OFFSET - (code == 1002 ? 2 : code);
     }
     cfg.menuKeybind.set(code, cfg.menuKeybind.isCtrl(), cfg.menuKeybind.isShift(), cfg.menuKeybind.isAlt());
     activity.client.ActivityClient.syncOpenMenuKey();
     activity.client.config.ActivityConfigManager.markDirty();
     activity.client.config.ActivityConfigManager.save();
 }
}

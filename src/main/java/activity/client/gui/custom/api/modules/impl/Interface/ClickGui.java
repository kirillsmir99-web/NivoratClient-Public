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
        super("ClickGui", "Открывает клик-меню клиента.", Category.DISPLAY);


        this.scaleMode.setChangeListener(ConfigManager::markDirty);
        this.customScale.setChangeListener(ConfigManager::markDirty);
    }
 @Override public KeyBind getBind() { return new KeyBind(activity.client.config.ActivityConfigManager.getConfig().menuKeybind.getKeyCode()); }
 @Override public void setBind(KeyBind bind) { var cfg=activity.client.config.ActivityConfigManager.getConfig(); cfg.menuKeybind.set(bind.getCode(),cfg.menuKeybind.isCtrl(),cfg.menuKeybind.isShift(),cfg.menuKeybind.isAlt()); activity.client.config.ActivityConfigManager.markDirty(); }
}

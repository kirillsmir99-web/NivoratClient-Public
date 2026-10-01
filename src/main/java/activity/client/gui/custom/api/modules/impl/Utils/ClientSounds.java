package activity.client.gui.custom.api.modules.impl.Utils;

import net.minecraft.sound.SoundEvent;
import activity.client.gui.custom.api.config.ConfigManager;
import activity.client.gui.custom.api.modules.Category;
import activity.client.gui.custom.api.modules.Module;
import activity.client.gui.custom.api.modules.settings.impl.BooleanSetting;
import activity.client.gui.custom.api.modules.settings.impl.ModeSetting;
import activity.client.gui.custom.api.modules.settings.impl.NumberSetting;
import activity.client.gui.custom.api.modules.settings.impl.SeparatorSetting;
import activity.client.gui.custom.utils.sounds.SoundManager;

public final class ClientSounds extends Module {
    private static ClientSounds instance;

    private final SeparatorSetting themeSeparator = this.register(new SeparatorSetting("Звуковой профиль"));
    private final ModeSetting soundType = this.register(new ModeSetting(
        "Тема звуков",
        "Общий звуковой профиль клиента.",
        "NV · Стекло",
        "NV · Стекло", "Serene", "Лаунчер", "Пузыри", "Киберпанк"
    ));

    private final SeparatorSetting toggleSeparator = this.register(new SeparatorSetting("Звуки переключения"));
    private final BooleanSetting moduleToggleSound = this.register(new BooleanSetting(
        "Вкл/выкл модулей",
        "Звук включения и выключения модулей.",
        true
    ));
    private final ModeSetting toggleSoundChoice = this.register(new ModeSetting(
        "Звук вкл/выкл",
        "Выбор звука включения и выключения модулей.",
        "По теме",
        "По теме", "NV · Стекло", "Serene", "Лаунчер", "Пузыри", "Киберпанк"
    ));
    private final NumberSetting volume = this.register(new NumberSetting(
        "Громкость",
        "Громкость звуков вкл/выкл модулей.",
        1.0, 0.0, 1.0, 0.05
    ));
    private final NumberSetting pitch = this.register(new NumberSetting(
        "Высота тона",
        "Высота тона звука вкл/выкл модулей.",
        1.0, 0.5, 2.0, 0.05
    ));

    private final SeparatorSetting interfaceSeparator = this.register(new SeparatorSetting("Звуки интерфейса"));
    public final NumberSetting interfaceVolume = this.register(new NumberSetting(
        "Громкость интерфейса",
        "Громкость звуков меню, ползунков и UI.",
        1.0, 0.0, 1.0, 0.05
    ));
    public final BooleanSetting guiSound = this.register(new BooleanSetting(
        "Открытие/закрытие меню",
        "Звук открытия и закрытия меню.",
        true
    ));
    private final ModeSetting guiOpenSoundChoice = this.register(new ModeSetting(
        "Звук меню",
        "Выбор звука открытия и закрытия меню.",
        "По теме",
        "По теме", "NV · Стекло", "Serene", "Лаунчер", "Пузыри", "Киберпанк"
    ));
    private final BooleanSetting sliderSound = this.register(new BooleanSetting(
        "Ползунок",
        "Звук перемещения ползунка.",
        true
    ));
    private final ModeSetting sliderSoundChoice = this.register(new ModeSetting(
        "Звук ползунка",
        "Выбор звука перемещения ползунков.",
        "По теме",
        "По теме", "NV · Стекло", "Serene", "Лаунчер", "Пузыри", "Киберпанк"
    ));
    private final BooleanSetting categorySound = this.register(new BooleanSetting(
        "Смена категории",
        "Звук переключения категории.",
        true
    ));
    private final ModeSetting categorySoundChoice = this.register(new ModeSetting(
        "Звук категорий",
        "Выбор звука смены категории.",
        "По теме",
        "По теме", "NV · Стекло", "Serene", "Лаунчер", "Пузыри", "Киберпанк"
    ));
    private final BooleanSetting moduleSettingsSound = this.register(new BooleanSetting(
        "Настройки модуля",
        "Звук открытия и закрытия настроек модуля.",
        true
    ));
    private final BooleanSetting dropdownSound = this.register(new BooleanSetting(
        "\u0412\u044b\u043f\u0430\u0434\u0430\u044e\u0449\u0438\u0435 \u0441\u043f\u0438\u0441\u043a\u0438",
        "\u0417\u0432\u0443\u043a \u043e\u0442\u043a\u0440\u044b\u0442\u0438\u044f \u0438 \u0437\u0430\u043a\u0440\u044b\u0442\u0438\u044f \u0432\u044b\u043f\u0430\u0434\u0430\u044e\u0449\u0438\u0445 \u0441\u043f\u0438\u0441\u043a\u043e\u0432.",
        true
    ));
    private final BooleanSetting searchTypingSound = this.register(new BooleanSetting(
        "\u0412\u0432\u043e\u0434 \u0432 \u043f\u043e\u0438\u0441\u043a\u0435",
        "\u0417\u0432\u0443\u043a \u0432\u0432\u043e\u0434\u0430 \u0442\u0435\u043a\u0441\u0442\u0430 \u0432 \u043f\u043e\u0438\u0441\u043a\u0435.",
        true
    ));

    private final SeparatorSetting chatSeparator = this.register(new SeparatorSetting("\u0417\u0432\u0443\u043a\u0438 \u0447\u0430\u0442\u0430"));
    private final NumberSetting chatVolume = this.register(new NumberSetting(
        "\u0413\u0440\u043e\u043c\u043a\u043e\u0441\u0442\u044c \u0447\u0430\u0442\u0430",
        "\u0413\u0440\u043e\u043c\u043a\u043e\u0441\u0442\u044c \u0437\u0432\u0443\u043a\u043e\u0432 \u0447\u0430\u0442\u0430 \u0438 \u043a\u043e\u043c\u0430\u043d\u0434.",
        1.0, 0.0, 1.0, 0.05
    ));
    private final BooleanSetting commandErrorSound = this.register(new BooleanSetting(
        "\u041e\u0448\u0438\u0431\u043a\u0438 \u043a\u043e\u043c\u0430\u043d\u0434",
        "\u0417\u0432\u0443\u043a \u043e\u0448\u0438\u0431\u043a\u0438 \u0438\u043b\u0438 \u043d\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u043d\u043e\u0439 \u043a\u043e\u043c\u0430\u043d\u0434\u044b.",
        true
    ));

    public ClientSounds() {
        super("Client Sounds", "\u0417\u0432\u0443\u043a\u0438 \u043a\u043b\u0438\u0435\u043d\u0442\u0430 \u0434\u043b\u044f \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u0439 \u043c\u043e\u0434\u0443\u043b\u0435\u0439 \u0438 \u043c\u0435\u043d\u044e.", Category.UTILS);
        instance = this;

        this.soundType.setChangeListener(() -> {
            if (!ConfigManager.isLoading()) {
                SoundEvent event = this.getToggleSoundEvent(true);
                SoundManager.playSoundDirect(event, this.volume.getFloat(), this.pitch.getFloat());
            }
        });
        this.sliderSoundChoice.setChangeListener(() -> {
            if (!ConfigManager.isLoading()) {
                SoundEvent event = this.resolveSliderSound();
                SoundManager.playSoundDirect(event, this.getInterfaceVolume(), 1.0f);
            }
        });
        this.toggleSoundChoice.setChangeListener(() -> {
            if (!ConfigManager.isLoading()) {
                SoundEvent event = this.getToggleSoundEvent(true);
                SoundManager.playSoundDirect(event, this.volume.getFloat(), this.pitch.getFloat());
            }
        });
        this.guiOpenSoundChoice.setChangeListener(() -> {
            if (!ConfigManager.isLoading()) {
                SoundEvent event = this.resolveGuiOpenSound();
                SoundManager.playSoundDirect(event, this.getInterfaceVolume(), 1.0f);
            }
        });
        this.categorySoundChoice.setChangeListener(() -> {
            if (!ConfigManager.isLoading()) {
                SoundEvent event = this.resolveCategorySound();
                SoundManager.playSoundDirect(event, this.getInterfaceVolume(), 1.0f);
            }
        });
    }

    public static ClientSounds getInstance() {
        return instance;
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    public void onModuleToggle(Module module, boolean enabled) {
        if (ConfigManager.isLoading()) {
            return;
        }
        if (module == this) {
            return;
        }
        this.playToggleSound(enabled);
    }

    public float getMasterVolume() {
        return this.volume.getFloat();
    }

    public float getInterfaceVolume() {
        return this.interfaceVolume.getFloat() * this.volume.getFloat();
    }

    public float getChatVolume() {
        return this.chatVolume.getFloat() * this.volume.getFloat();
    }

    public float getVolumeFor(String string) {
        if (string == null) {
            return this.getInterfaceVolume();
        }
        return switch (string) {
            case "gui_open", "gui_close", "select_category", "module_settings_open", "module_settings_close", "settings_open", "settings_close", "slider", "search_typing", "buttonclick", "button_click", "pin", "unpin" -> this.getInterfaceVolume();
            case "command_error" -> this.getChatVolume();
            default -> this.getMasterVolume();
        };
    }

    public SoundEvent getToggleSoundEvent(boolean enabled) {
        String choice = this.toggleSoundChoice.getSelected();
        String theme = "По теме".equals(choice) ? this.soundType.getSelected() : choice;
        return switch (theme) {
            case "NV · Стекло" -> enabled ? SoundManager.NV_GLASS_TOGGLE_ON : SoundManager.NV_GLASS_TOGGLE_OFF;
            case "Serene" -> enabled ? SoundManager.SERENE_TOGGLE_ON : SoundManager.SERENE_TOGGLE_OFF;
            case "Лаунчер" -> enabled ? SoundManager.LAUNCHER_TOGGLE_ON : SoundManager.LAUNCHER_TOGGLE_OFF;
            case "Пузыри" -> enabled ? SoundManager.BUBBLE_TOGGLE_ON : SoundManager.BUBBLE_TOGGLE_OFF;
            case "Киберпанк" -> enabled ? SoundManager.CYBER_TOGGLE_ON : SoundManager.CYBER_TOGGLE_OFF;
            default -> enabled ? SoundManager.SERENE_TOGGLE_ON : SoundManager.SERENE_TOGGLE_OFF;
        };
    }

    private void playToggleSound(boolean bl) {
        if (!this.isEnabled() || !this.moduleToggleSound.getValue()) {
            return;
        }
        float f = this.volume.getFloat();
        float f2 = this.pitch.getFloat();
        SoundEvent event = this.getToggleSoundEvent(bl);
        SoundManager.playSoundDirect(event, f, f2);
    }

    public SoundEvent resolveSliderSound() {
        String choice = this.sliderSoundChoice.getSelected();
        String theme = "По теме".equals(choice) ? this.soundType.getSelected() : choice;
        return switch (theme) {
            case "NV · Стекло" -> SoundManager.NV_GLASS_SLIDER;
            case "Serene" -> SoundManager.SERENE_SLIDER;
            case "Лаунчер" -> SoundManager.LAUNCHER_SLIDER;
            case "Пузыри" -> SoundManager.BUBBLE_SLIDER;
            case "Киберпанк" -> SoundManager.CYBER_SLIDER;
            default -> SoundManager.SERENE_SLIDER;
        };
    }

    public SoundEvent resolveGuiOpenSound() {
        String choice = this.guiOpenSoundChoice.getSelected();
        String theme = "По теме".equals(choice) ? this.soundType.getSelected() : choice;
        return switch (theme) {
            case "NV · Стекло" -> SoundManager.NV_GLASS_OPEN;
            case "Serene" -> SoundManager.SERENE_OPEN;
            case "Лаунчер" -> SoundManager.LAUNCHER_OPEN;
            case "Пузыри" -> SoundManager.BUBBLE_OPEN;
            case "Киберпанк" -> SoundManager.CYBER_OPEN;
            default -> SoundManager.SERENE_OPEN;
        };
    }

    public SoundEvent resolveGuiCloseSound() {
        String choice = this.guiOpenSoundChoice.getSelected();
        String theme = "По теме".equals(choice) ? this.soundType.getSelected() : choice;
        return switch (theme) {
            case "NV · Стекло" -> SoundManager.NV_GLASS_CLOSE;
            case "Serene" -> SoundManager.SERENE_CLOSE;
            case "Лаунчер" -> SoundManager.LAUNCHER_CLOSE;
            case "Пузыри" -> SoundManager.BUBBLE_CLOSE;
            case "Киберпанк" -> SoundManager.CYBER_CLOSE;
            default -> SoundManager.SERENE_CLOSE;
        };
    }

    public SoundEvent resolveCategorySound() {
        String choice = this.categorySoundChoice.getSelected();
        String theme = "По теме".equals(choice) ? this.soundType.getSelected() : choice;
        return switch (theme) {
            case "NV · Стекло" -> SoundManager.NV_GLASS_CATEGORY;
            case "Serene" -> SoundManager.SERENE_CATEGORY;
            case "Лаунчер" -> SoundManager.LAUNCHER_CATEGORY;
            case "Пузыри" -> SoundManager.BUBBLE_CATEGORY;
            case "Киберпанк" -> SoundManager.CYBER_CATEGORY;
            default -> SoundManager.SERENE_CATEGORY;
        };
    }

    public SoundEvent resolveSoundEvent(String string, SoundEvent defaultEvent) {
        if (string == null) {
            return defaultEvent;
        }
        return switch (string) {
            case "slider" -> this.resolveSliderSound();
            case "gui_open" -> this.resolveGuiOpenSound();
            case "gui_close" -> this.resolveGuiCloseSound();
            case "select_category" -> this.resolveCategorySound();
            case "settings_open" -> switch (this.soundType.getSelected()) {
                case "NV · Стекло" -> SoundManager.NV_GLASS_DROPDOWN_OPEN;
                case "Serene" -> SoundManager.SERENE_DROPDOWN_OPEN;
                case "Лаунчер" -> SoundManager.LAUNCHER_DROPDOWN_OPEN;
                case "Пузыри" -> SoundManager.BUBBLE_OPEN;
                case "Киберпанк" -> SoundManager.CYBER_BUTTON;
                default -> SoundManager.SERENE_DROPDOWN_OPEN;
            };
            case "settings_close" -> switch (this.soundType.getSelected()) {
                case "NV · Стекло" -> SoundManager.NV_GLASS_DROPDOWN_CLOSE;
                case "Serene" -> SoundManager.SERENE_DROPDOWN_CLOSE;
                case "Лаунчер" -> SoundManager.LAUNCHER_DROPDOWN_CLOSE;
                case "Пузыри" -> SoundManager.BUBBLE_CLOSE;
                case "Киберпанк" -> SoundManager.CYBER_BUTTON;
                default -> SoundManager.SERENE_DROPDOWN_CLOSE;
            };
            case "module_settings_open" -> switch (this.soundType.getSelected()) {
                case "NV · Стекло" -> SoundManager.NV_GLASS_OPEN;
                case "Serene" -> SoundManager.SERENE_OPEN;
                case "Лаунчер" -> SoundManager.LAUNCHER_DROPDOWN_OPEN;
                case "Пузыри" -> SoundManager.BUBBLE_OPEN;
                case "Киберпанк" -> SoundManager.CYBER_OPEN;
                default -> SoundManager.SERENE_OPEN;
            };
            case "module_settings_close" -> switch (this.soundType.getSelected()) {
                case "NV · Стекло" -> SoundManager.NV_GLASS_CLOSE;
                case "Serene" -> SoundManager.SERENE_CLOSE;
                case "Лаунчер" -> SoundManager.LAUNCHER_DROPDOWN_CLOSE;
                case "Пузыри" -> SoundManager.BUBBLE_CLOSE;
                case "Киберпанк" -> SoundManager.CYBER_CLOSE;
                default -> SoundManager.SERENE_CLOSE;
            };
            case "buttonclick", "button_click" -> switch (this.soundType.getSelected()) {
                case "NV · Стекло" -> SoundManager.NV_GLASS_BUTTON;
                case "Serene" -> SoundManager.SERENE_BUTTON;
                case "Лаунчер" -> SoundManager.LAUNCHER_BUTTON;
                case "Пузыри" -> SoundManager.BUBBLE_BUTTON;
                case "Киберпанк" -> SoundManager.CYBER_BUTTON;
                default -> SoundManager.SERENE_BUTTON;
            };
            case "pin" -> SoundManager.SERENE_PIN;
            case "unpin" -> SoundManager.SERENE_UNPIN;
            case "search_typing" -> this.resolveSliderSound();
            default -> defaultEvent;
        };
    }


    public static boolean isAllowed(String string) {
        ClientSounds clientSounds = instance;
        if (clientSounds == null || string == null) {
            return true;
        }
        if (!clientSounds.isEnabled()) {
            return false;
        }
        return switch (string) {
            case "gui_open", "gui_close" -> clientSounds.guiSound.getValue();
            case "select_category" -> clientSounds.categorySound.getValue();
            case "module_settings_open", "module_settings_close" -> clientSounds.moduleSettingsSound.getValue();
            case "settings_open", "settings_close" -> clientSounds.dropdownSound.getValue();
            case "slider" -> clientSounds.sliderSound.getValue();
            case "search_typing" -> clientSounds.searchTypingSound.getValue();
            case "command_error" -> clientSounds.commandErrorSound.getValue();
            default -> true;
        };
    }

    public float getPitch() {
        return this.pitch.getFloat();
    }

    public float getVolume() {
        return this.volume.getFloat();
    }
}

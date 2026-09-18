package activity.client.module.example;

import activity.client.config.ActivityConfig;
import activity.client.gui.ActivityScreen;
import activity.client.gui.component.ActivityLabel;
import activity.client.gui.icon.ActivityIcon;
import activity.client.gui.layout.ScrollContainer;
import activity.client.gui.tab.ActivityTab;
import activity.client.gui.theme.ActivityMetrics;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.NivoratModule;
import activity.client.module.keybind.Keybind;
import activity.client.module.setting.BooleanSetting;
import activity.client.module.setting.DoubleSetting;
import activity.client.module.setting.EnumSetting;
import activity.client.module.setting.IntegerSetting;
import activity.client.module.setting.KeybindSetting;
import activity.client.module.setting.NumberSetting;
import activity.client.module.setting.NumberUnit;
import activity.client.module.setting.SettingSection;
import activity.client.module.setting.StringSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Reference implementation demonstrating how to build a complete feature module
 * using the Nivorat Module SDK.
 *
 * <p><b>NOTE:</b> This is an educational template for developers and is NOT registered
 * in production {@code BuiltinModules}.
 */
@SuppressWarnings("unused")
public class ExampleModule extends NivoratModule {

    public static final String ID = "example_module";

    // Runtime state variables (Single Source of Truth)
    private boolean sampleSwitch = true;
    private double delayMs = 120.0;
    private int burstCount = 3;
    private double reachDistance = 3.5;
    private String triggerMode = "smart";
    private String customMessage = "Hello Nivorat!";
    private final Keybind secondaryKeybind = new Keybind(GLFW.GLFW_KEY_V);

    // Typed Setting references
    private BooleanSetting switchSetting;
    private NumberSetting delaySetting;
    private IntegerSetting burstSetting;
    private DoubleSetting reachSetting;
    private EnumSetting modeSetting;
    private StringSetting messageSetting;
    private KeybindSetting secondaryKeySetting;

    public ExampleModule() {
        super(
                ID,
                Text.literal("Пример Модуля"),
                Text.literal("Демонстрационный модуль для разработчиков SDK"),
                ModuleCategory.COMBAT
        );

        // 1. Metadata Configuration
        this.metadata = ModuleMetadata.builder(ID)
                .displayName(name)
                .description(description)
                .category(category)
                .author("Nivorat Developer")
                .version("1.0.0")
                .lastUpdated("2026-09-17")
                .icon(ActivityIcon.COMBAT)
                .keybind(keybind)
                .build();

        // 2. Settings Registration (Automatic UI Binding & Categorization)

        // General Section: Main Mode Selector & Primary Toggle
        this.switchSetting = registerBoolean(
                "sample_switch",
                Text.literal("Включить фильтрацию"),
                Text.literal("Активирует базовый фильтр целей"),
                SettingSection.GENERAL,
                true,
                () -> sampleSwitch,
                val -> sampleSwitch = val
        );

        this.modeSetting = registerEnum(
                "mode",
                Text.literal("Режим работы"),
                Text.literal("Выбор профиля работы модуля"),
                SettingSection.GENERAL,
                List.of("smart", "aggressive", "passive"),
                "smart",
                opt -> switch (opt) {
                    case "aggressive" -> Text.literal("Агрессивный");
                    case "passive" -> Text.literal("Пассивный");
                    default -> Text.literal("Умный (Smart)");
                },
                () -> triggerMode,
                val -> triggerMode = val
        );

        // Behavior Section: Numbers & Timings with Units
        this.delaySetting = registerNumber(
                "delay_ms",
                Text.literal("Задержка срабатывания"),
                Text.literal("Таймаут между действиями в миллисекундах"),
                SettingSection.BEHAVIOR,
                10.0, 500.0, 5.0, NumberUnit.MS, true,
                120.0,
                () -> delayMs,
                val -> delayMs = val
        );

        this.reachSetting = registerDouble(
                "reach_distance",
                Text.literal("Дистанция взаимодействия"),
                Text.literal("Максимальное расстояние до цели в блоках"),
                SettingSection.BEHAVIOR,
                1.0, 6.0, 0.1, NumberUnit.BLOCKS,
                3.5,
                () -> reachDistance,
                val -> reachDistance = val
        );

        // Advanced Section: Integers, Keys, and Visibility Conditions
        this.burstSetting = registerInteger(
                "burst_count",
                Text.literal("Число повторений"),
                Text.literal("Количество действий за один цикл"),
                SettingSection.ADVANCED,
                1, 10, 1, NumberUnit.TICKS,
                3,
                () -> burstCount,
                val -> burstCount = val
        );
        // Setting can be made conditionally visible
        this.burstSetting.visibleWhen(() -> sampleSwitch);

        this.messageSetting = registerString(
                "custom_message",
                Text.literal("Сообщение"),
                Text.literal("Текст для автоматической отправки в чат"),
                SettingSection.ADVANCED,
                "Hello Nivorat!",
                () -> customMessage,
                val -> customMessage = val
        );

        this.secondaryKeySetting = registerKeybind(
                "secondary_key",
                Text.literal("Дополнительная кнопка"),
                Text.literal("Кнопка для мгновенного выполнения вспомогательного действия"),
                SettingSection.ADVANCED,
                secondaryKeybind,
                () -> secondaryKeybind,
                kb -> secondaryKeybind.copyFrom(kb)
        );

        // Action Setting: Button trigger
        registerAction(
                "trigger_action",
                Text.literal("Сбросить параметры"),
                Text.literal("Возвращает настройки модуля к заводским значениям"),
                SettingSection.ADVANCED,
                () -> getSettings().forEach(activity.client.module.setting.Setting::reset)
        );
    }

    // ==========================================
    // LIFECYCLE HOOKS
    // ==========================================

    @Override
    public void onInitialize() {
        // Called once when module is registered
    }

    @Override
    public void onEnable() {
        // Called when module is toggled ON
    }

    @Override
    public void onDisable() {
        // Called when module is toggled OFF
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (!isEnabled() || client.player == null) return;
        // Periodic per-tick logic
    }

    @Override
    public ActionResult onAttackEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        if (!isEnabled()) return ActionResult.PASS;
        // Combat entity attack hook
        return ActionResult.PASS;
    }

    @Override
    public void onRenderHud(DrawContext context, RenderTickCounter tickCounter) {
        if (!isEnabled()) return;
        // In-game HUD rendering hook
    }

    // ==========================================
    // EXTENSION POINT (Optional Custom Section)
    // ==========================================

    @Override
    public boolean hasCustomSection() {
        return true;
    }

    @Override
    public int buildCustomSection(ActivityTab tab, ActivityScreen screen, ScrollContainer container,
                                  int startX, int startY, int innerRowW) {
        // Example custom UI widget embedded inside the module card
        ActivityLabel customInfo = new ActivityLabel(
                startX, startY + 3,
                Text.literal("§7[Custom Section] Индивидуальный компонент модуля")
        );
        customInfo.setMaxWidth(innerRowW);
        if (tab != null) {
            tab.addControl(container, customInfo);
        } else if (container != null) {
            container.addChild(customInfo);
        }
        return ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
    }

    // ==========================================
    // CONFIG PERSISTENCE
    // ==========================================

    @Override
    public void loadFromConfig(ActivityConfig config) {
        // If the module has dedicated fields in ActivityConfig:
        // this.enabled = config.exampleEnabled;
        // this.keybind.copyFrom(config.exampleKeybind);
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        // If the module has dedicated fields in ActivityConfig:
        // config.exampleEnabled = this.enabled;
        // config.exampleKeybind.copyFrom(this.keybind);
    }
}

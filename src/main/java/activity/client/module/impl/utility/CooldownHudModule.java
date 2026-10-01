package activity.client.module.impl.utility;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.hud.CooldownHudEditorScreen;
import activity.client.gui.hud.CooldownHudOverlay;
import activity.client.gui.icon.ActivityIcon;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.NivoratModule;
import activity.client.module.setting.NumberUnit;
import activity.client.module.setting.SettingGroup;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

public class CooldownHudModule extends NivoratModule {
    public static final String ID = "cooldown_hud";

    public boolean vertical = false;
    public double minDuration = 2.5;

    public CooldownHudModule() {
        super(ID, Text.literal("Cooldown HUD"), Text.literal("Отображение активных серверных кулдаунов PVP-предметов"), ModuleCategory.UTILITY);
        this.metadata = ModuleMetadata.builder(ID)
                .displayName(name)
                .description(description)
                .category(category)
                .author("kt1xW")
                .version("1.0.0")
                .icon(ActivityIcon.UTILITY)
                .keybind(keybind)
                .aliases("cooldown", "cooldowns", "cd", "кд", "кулдауны", "кулдаун", "cooldown_hud", "таймер")
                .build();

        registerBoolean(
                "vertical",
                Text.literal("Вертикальный список"),
                Text.literal("Отображать кулдауны вертикальным списком вместо горизонтальной строки"),
                SettingGroup.GENERAL,
                false,
                () -> this.vertical,
                val -> {
                    this.vertical = val;
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.cooldownHudVertical = val;
                        ActivityConfigManager.markDirty();
                    }
                }
        );

        registerAction("custom_items", Text.literal("Свои предметы"), Text.literal("Поиск предметов по названию, ID или имени в хотбаре"), SettingGroup.EXTRA,
                () -> { var mc = MinecraftClient.getInstance(); mc.send(() -> mc.setScreen(new activity.client.gui.custom.NativeCollectionScreen(mc.currentScreen, activity.client.gui.custom.NativeCollectionScreen.Kind.COOLDOWN))); });
        registerAction("open_editor", Text.literal("Редактировать место Cooldown HUD"),
                Text.literal("Интерактивное перемещение и настройка отображения кулдаунов"), SettingGroup.EXTRA,
                () -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc != null) {
                        mc.send(() -> mc.setScreen(new CooldownHudEditorScreen(mc.currentScreen)));
                    }
                }
        );

        registerAction("reset_position", Text.literal("Сбросить позицию"),
                Text.literal("Сбросить координаты HUD к значениям по умолчанию"), SettingGroup.EXTRA,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.cooldownHudCustomX = -1;
                        c.cooldownHudCustomY = -1;
                        ActivityConfigManager.markDirty();
                    }
                }
        );
    }

    @Override
    public boolean hasTickLogic() {
        return false;
    }

    @Override
    public void onRenderHud(DrawContext context, RenderTickCounter tickCounter) {
        if (isEnabled()) {
            CooldownHudOverlay.render(context, tickCounter);
        }
    }

    @Override
    public void loadFromConfig(ActivityConfig config) {
        if (config == null) return;
        this.enabled = config.cooldownHudEnabled;
        this.keybind.copyFrom(config.cooldownHudKeybind);
        this.vertical = config.cooldownHudVertical;
        this.minDuration = config.cooldownHudMinDuration;
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        config.cooldownHudEnabled = this.enabled;
        config.cooldownHudKeybind.copyFrom(this.keybind);
        config.cooldownHudVertical = this.vertical;
        config.cooldownHudMinDuration = this.minDuration;
    }
}

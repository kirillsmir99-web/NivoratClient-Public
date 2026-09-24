package activity.client.module.impl.utility;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.icon.ActivityIcon;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.NivoratModule;
import activity.client.module.setting.SettingGroup;
import dev.carthud.CartHudConfig;
import dev.carthud.CartHudEditorScreen;
import dev.carthud.CartHudOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

public class CartHudModule extends NivoratModule {
    public static final String ID = "cart_hud";

    public CartHudModule() {
        super(ID, Text.translatable("activity.module.cart_hud.name"), Text.translatable("activity.module.cart_hud.desc"), ModuleCategory.UTILITY);
        this.metadata = ModuleMetadata.builder(ID)
                .displayName(name)
                .description(description)
                .category(category)
                .author("kt1xW")
                .version("1.0.0")
                .icon(ActivityIcon.UTILITY)
                .keybind(keybind)
                .aliases("carthud", "hud", "хад", "картхад", "оверлей", "вагонетки", "редактор", "editor", "позиция")
                .build();

        registerAction("open_editor", Text.translatable("activity.setting.utility.open_carthud_editor"),
                Text.translatable("activity.setting.utility.open_carthud_editor.desc"), SettingGroup.EXTRA,
                () -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc != null) {
                        mc.send(() -> mc.setScreen(new CartHudEditorScreen(mc.currentScreen)));
                    }
                }
        );

        registerAction("reset_position", Text.translatable("activity.setting.utility.reset_carthud_pos"),
                Text.translatable("activity.setting.utility.reset_carthud_pos.desc"), SettingGroup.EXTRA,
                () -> {
                    ActivityConfig c = ActivityConfigManager.getConfig();
                    if (c != null) {
                        c.cartHudCustomX = -1;
                        c.cartHudCustomY = -1;
                        syncEngineConfig(c);
                        ActivityConfigManager.markDirty();
                    }
                }
        );
    }

    public void syncEngineConfig(ActivityConfig c) {
        if (c == null) return;
        CartHudConfig.enabled = c.cartHudEnabled;
        CartHudConfig.customX = c.cartHudCustomX;
        CartHudConfig.customY = c.cartHudCustomY;
    }

    @Override
    public void onInitialize() {
        CartHudConfig.load();
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            syncEngineConfig(c);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            c.cartHudEnabled = enabled;
            ActivityConfigManager.markDirty();
        }
        CartHudConfig.enabled = enabled;
    }

    @Override
    public boolean hasTickLogic() {
        return false;
    }

    @Override
    public void onRenderHud(DrawContext context, RenderTickCounter tickCounter) {
        if (isEnabled()) {
            CartHudOverlay.render(context, tickCounter);
        }
    }

    @Override
    public void loadFromConfig(ActivityConfig config) {
        if (config == null) return;
        this.enabled = config.cartHudEnabled;
        this.keybind.copyFrom(config.cartHudKeybind);
        syncEngineConfig(config);
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
        if (config == null) return;
        config.cartHudEnabled = this.enabled;
        config.cartHudKeybind.copyFrom(this.keybind);
        config.cartHudCustomX = CartHudConfig.customX;
        config.cartHudCustomY = CartHudConfig.customY;
        syncEngineConfig(config);
    }
}

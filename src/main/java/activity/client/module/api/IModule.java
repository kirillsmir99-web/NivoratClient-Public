package activity.client.module.api;

import activity.client.config.ActivityConfig;
import activity.client.module.keybind.Keybind;
import activity.client.module.setting.Setting;
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

import java.util.Collections;
import java.util.List;

/**
 * Common contract for all Activity gameplay modules and integration stubs.
 */
public interface IModule {

    String getId();

    Text getName();

    Text getDescription();

    ModuleCategory getCategory();

    boolean isEnabled();

    void setEnabled(boolean enabled);

    Keybind getKeybind();

    ModuleStatus getStatus();

    default Text getDisplayName() {
        return getName();
    }

    default String getVersion() {
        ModuleMetadata meta = getMetadata();
        return meta != null ? meta.getVersion() : "1.0.0";
    }

    default String getAuthor() {
        ModuleMetadata meta = getMetadata();
        return meta != null ? meta.getAuthor() : "Nivorat";
    }

    default ModuleMetadata getMetadata() {
        return null;
    }

    default List<String> getAliases() {
        ModuleMetadata meta = getMetadata();
        return meta != null ? meta.getAliases() : Collections.emptyList();
    }

    default boolean isStub() {
        return getStatus() == ModuleStatus.STUB_PENDING_CORE;
    }

    default List<Setting<?>> getSettings() {
        return Collections.emptyList();
    }

    default Setting<?> getSetting(String id) {
        if (id == null) return null;
        for (Setting<?> setting : getSettings()) {
            if (id.equals(setting.getId())) {
                return setting;
            }
        }
        return null;
    }

    default void onInitialize() {}

    default void onEnable() {}

    default void onDisable() {}

    default boolean hasTickLogic() {
        return true;
    }

    default void onTick(MinecraftClient client) {}

    default void onClientTick(MinecraftClient client) {
        onTick(client);
    }

    default void onWorldJoin(MinecraftClient client, net.minecraft.client.world.ClientWorld world) {}

    default void onWorldLeave(MinecraftClient client) {}

    default ActionResult onAttackEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        return ActionResult.PASS;
    }

    default void onRenderHud(DrawContext context, RenderTickCounter tickCounter) {}

    /**
     * Extension point for complex HUD modules (e.g. HPReaper, CartHUD) to declare custom UI sections.
     *
     * @return true if module provides custom UI rendering beyond standard generated settings
     */
    default boolean hasCustomSection() {
        return false;
    }

    /**
     * Builds custom UI controls or previews into the module card.
     *
     * @param tab parent activity tab
     * @param screen parent activity screen
     * @param container scroll container receiving widgets
     * @param startX start X inside card
     * @param startY start Y inside card
     * @param innerRowW width of content area
     * @return additional height consumed by custom section
     */
    default int buildCustomSection(activity.client.gui.tab.ActivityTab tab,
                                  activity.client.gui.ActivityScreen screen,
                                  activity.client.gui.layout.ScrollContainer container,
                                  int startX, int startY, int innerRowW) {
        return 0;
    }

    void loadFromConfig(ActivityConfig config);

    void saveToConfig(ActivityConfig config);
}

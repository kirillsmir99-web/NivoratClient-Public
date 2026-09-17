package activity.client.module.api;

import activity.client.config.ActivityConfig;
import activity.client.module.keybind.Keybind;
import net.minecraft.text.Text;

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

    default ModuleMetadata getMetadata() {
        return null;
    }

    default boolean isStub() {
        return getStatus() == ModuleStatus.STUB_PENDING_CORE;
    }

    void loadFromConfig(ActivityConfig config);

    void saveToConfig(ActivityConfig config);
}

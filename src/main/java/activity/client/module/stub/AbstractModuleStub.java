package activity.client.module.stub;

import activity.client.module.api.IModule;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleStatus;
import activity.client.module.keybind.Keybind;
import net.minecraft.text.Text;

import java.util.Objects;

/**
 * Base class for Activity integration stubs.
 * Clearly marks modules as awaiting game-mechanic logic connection while
 * maintaining full GUI, configuration, and keybind functionality.
 */
public abstract class AbstractModuleStub implements IModule {

    private final String id;
    private final Text name;
    private final Text description;
    private final ModuleCategory category;
    protected final Keybind keybind;
    protected boolean enabled;

    public AbstractModuleStub(String id, Text name, Text description, ModuleCategory category, Keybind defaultKeybind) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.category = Objects.requireNonNull(category, "category");
        this.keybind = defaultKeybind != null
            ? new Keybind(defaultKeybind.getKeyCode(), defaultKeybind.isCtrl(), defaultKeybind.isShift(), defaultKeybind.isAlt())
            : new Keybind();
        this.enabled = true;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public Text getName() {
        return this.name;
    }

    @Override
    public Text getDescription() {
        return this.description;
    }

    @Override
    public ModuleCategory getCategory() {
        return this.category;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            activity.client.module.api.ModuleEventDispatcher.updateActiveModules();
        }
    }

    @Override
    public Keybind getKeybind() {
        return this.keybind;
    }

    @Override
    public ModuleStatus getStatus() {
        return ModuleStatus.STUB_PENDING_CORE;
    }

    protected activity.client.module.api.ModuleMetadata metadata;

    @Override
    public activity.client.module.api.ModuleMetadata getMetadata() {
        if (this.metadata == null) {
            this.metadata = activity.client.module.api.ModuleMetadata.builder(this.id)
                .displayName(this.name)
                .description(this.description)
                .category(this.category)
                .keybind(this.keybind)
                .icon(resolveDefaultIcon())
                .build();
        }
        return this.metadata;
    }

    protected activity.client.gui.icon.ActivityIcon resolveDefaultIcon() {
        if (this.category == ModuleCategory.COMBAT) return activity.client.gui.icon.ActivityIcon.COMBAT;
        if (this.category == ModuleCategory.DEFENSE) return activity.client.gui.icon.ActivityIcon.DEFENSE;
        if (this.category == ModuleCategory.UTILITY) return activity.client.gui.icon.ActivityIcon.UTILITY;
        return activity.client.gui.icon.ActivityIcon.CONFIG;
    }
}

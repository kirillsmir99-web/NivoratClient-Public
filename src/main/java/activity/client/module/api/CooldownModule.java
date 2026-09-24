package activity.client.module.api;

import net.minecraft.text.Text;

public abstract class CooldownModule extends AbstractModule {

    public CooldownModule(String id, Text name, Text description, ModuleCategory category) {
        super(id, name, description, category);
    }
}

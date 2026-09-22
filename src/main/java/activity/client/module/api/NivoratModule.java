package activity.client.module.api;

import net.minecraft.text.Text;

public abstract class NivoratModule extends AbstractModule {

    public NivoratModule(String id, Text name, Text description, ModuleCategory category) {
        super(id, name, description, category);
    }
}

package activity.client.module.api;

import net.minecraft.text.Text;

/**
 * Standard base class for NivoratClient modules.
 *
 * <p>Provides full access to the Nivorat Module SDK:
 * <ul>
 *   <li>Automatic lifecycle management ({@link #onInitialize()}, {@link #onEnable()}, {@link #onDisable()}, {@link #onTick})</li>
 *   <li>Typed settings registration ({@link #registerBoolean}, {@link #registerNumber}, etc.)</li>
 *   <li>Single Source of Truth state binding</li>
 *   <li>Automatic GUI rendering and section grouping</li>
 *   <li>Unified keybind binding</li>
 * </ul>
 */
public abstract class NivoratModule extends AbstractModule {

    public NivoratModule(String id, Text name, Text description, ModuleCategory category) {
        super(id, name, description, category);
    }
}

package activity.client.module.api;

import activity.client.module.impl.combat.AutoMaceModule;
import activity.client.module.impl.combat.AutoPearlCatchModule;
import activity.client.module.impl.combat.AutoShieldbreakerModule;
import activity.client.module.impl.combat.AutoSpearModule;
import activity.client.module.impl.combat.AutoStunSlamModule;
import activity.client.module.impl.defense.AutoAnchorModule;
import activity.client.module.impl.defense.AutoCartModule;
import activity.client.module.impl.defense.AutoTotemModule;
import activity.client.module.impl.defense.CartRefillModule;
import activity.client.module.impl.utility.AutoGGModule;
import activity.client.module.impl.utility.AutoToolModule;
import activity.client.module.impl.utility.CartHudModule;
import activity.client.module.impl.utility.HPReaperModule;

/**
 * Registry bootstrapper for all 13 built-in NivoratClient modules.
 */
public final class BuiltinModules {

    private BuiltinModules() {}

    public static void registerAll() {
        // 1. Combat
        ModuleRegistry.register(new AutoMaceModule());
        ModuleRegistry.register(new AutoSpearModule());
        ModuleRegistry.register(new AutoShieldbreakerModule());
        ModuleRegistry.register(new AutoStunSlamModule());
        ModuleRegistry.register(new AutoPearlCatchModule());

        // 2. Defense
        ModuleRegistry.register(new AutoTotemModule());
        ModuleRegistry.register(new AutoCartModule());
        ModuleRegistry.register(new AutoAnchorModule());
        ModuleRegistry.register(new CartRefillModule());

        // 3. Utility
        ModuleRegistry.register(new HPReaperModule());
        ModuleRegistry.register(new AutoToolModule());
        ModuleRegistry.register(new AutoGGModule());
        ModuleRegistry.register(new CartHudModule());
    }
}

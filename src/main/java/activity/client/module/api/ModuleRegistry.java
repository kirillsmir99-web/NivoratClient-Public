package activity.client.module.api;

import activity.client.config.ActivityConfig;

import activity.client.module.stub.AutoAnchorStub;
import activity.client.module.stub.AutoCartStub;
import activity.client.module.stub.AutoGGStub;
import activity.client.module.stub.AutoMaceStub;
import activity.client.module.stub.AutoShieldbreakerStub;
import activity.client.module.stub.AutoSpearStub;
import activity.client.module.stub.AutoStunSlamStub;
import activity.client.module.stub.AutoToolStub;
import activity.client.module.stub.AutoTotemStub;
import activity.client.module.stub.CartRefillStub;
import activity.client.module.stub.HPReaperStub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thread-safe central registry for all registered Activity modules and integration stubs.
 */
public final class ModuleRegistry {

    private static final Map<String, IModule> MODULES = new LinkedHashMap<>();

    static {
        // Combat
        register(new AutoMaceStub());
        register(new AutoSpearStub());
        register(new AutoShieldbreakerStub());
        register(new AutoStunSlamStub());

        // Defense
        register(new AutoTotemStub());
        register(new AutoCartStub());
        register(new AutoAnchorStub());
        register(new CartRefillStub());

        // Utility
        register(new HPReaperStub());
        register(new AutoToolStub());
        register(new AutoGGStub());
    }

    private ModuleRegistry() {}

    public static synchronized void register(IModule module) {
        if (module != null) {
            MODULES.put(module.getId(), module);
        }
    }

    public static synchronized IModule get(String id) {
        if (id == null) return null;
        if ("auto_stun_slime".equals(id) || "autostunslime".equalsIgnoreCase(id)) {
            IModule slam = MODULES.get(AutoStunSlamStub.ID);
            if (slam != null) return slam;
        }
        return MODULES.get(id);
    }

    public static synchronized ModuleMetadata getMetadata(String id) {
        IModule module = get(id);
        return module != null ? module.getMetadata() : null;
    }

    public static synchronized List<IModule> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(MODULES.values()));
    }

    public static synchronized List<IModule> getByCategory(ModuleCategory category) {
        List<IModule> result = new ArrayList<>();
        for (IModule module : MODULES.values()) {
            if (module.getCategory() == category) {
                result.add(module);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static synchronized void loadAll(ActivityConfig config) {
        for (IModule module : MODULES.values()) {
            module.loadFromConfig(config);
        }
    }

    public static synchronized void saveAll(ActivityConfig config) {
        for (IModule module : MODULES.values()) {
            module.saveToConfig(config);
        }
    }
}

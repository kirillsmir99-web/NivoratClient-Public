package activity.client.module.api;

public enum ModuleState {
    ENABLED,
    DISABLED;

    public boolean isEnabled() {
        return this == ENABLED;
    }
}

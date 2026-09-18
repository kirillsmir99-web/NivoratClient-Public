package activity.client.module.api;

/**
 * Operational runtime state for an Activity/Nivorat module.
 */
public enum ModuleState {
    ENABLED,
    DISABLED;

    public boolean isEnabled() {
        return this == ENABLED;
    }
}

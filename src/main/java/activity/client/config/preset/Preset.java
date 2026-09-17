package activity.client.config.preset;

import com.google.gson.JsonObject;

import java.util.Objects;

/**
 * Representation of a gameplay and module configuration preset.
 *
 * <p>Contains metadata and a clean snapshot of module settings, excluding
 * transient UI state (window coordinates, current scroll, search query, active profile).
 */
public class Preset {

    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final String CURRENT_CLIENT_VERSION = "v1.0.0";
    public static final String DEFAULT_PRESET_ID = "default";
    public static final String DEFAULT_PRESET_NAME = "По умолчанию";

    private final String id;
    private String name;
    private final long createdAt;
    private long updatedAt;
    private final int schemaVersion;
    private final String clientVersion;
    private final boolean builtin;
    private JsonObject settings;

    public Preset(String id, String name, long createdAt, long updatedAt,
                  int schemaVersion, String clientVersion, boolean builtin, JsonObject settings) {
        this.id = id != null ? id : java.util.UUID.randomUUID().toString();
        this.name = name != null ? name : "";
        this.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
        this.updatedAt = updatedAt > 0 ? updatedAt : this.createdAt;
        this.schemaVersion = schemaVersion > 0 ? schemaVersion : CURRENT_SCHEMA_VERSION;
        this.clientVersion = clientVersion != null && !clientVersion.isBlank() ? clientVersion : CURRENT_CLIENT_VERSION;
        this.builtin = builtin;
        this.settings = settings != null ? settings.deepCopy() : new JsonObject();
    }

    public static Preset createCustom(String name, JsonObject settings) {
        long now = System.currentTimeMillis();
        String id = "preset_" + now + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        return new Preset(id, name, now, now, CURRENT_SCHEMA_VERSION, CURRENT_CLIENT_VERSION, false, settings);
    }

    public static Preset createDefault(JsonObject defaultSettings) {
        return new Preset(DEFAULT_PRESET_ID, DEFAULT_PRESET_NAME, 0L, 0L, CURRENT_SCHEMA_VERSION, CURRENT_CLIENT_VERSION, true, defaultSettings);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public boolean isBuiltin() {
        return builtin;
    }

    public boolean isDeletable() {
        return !builtin;
    }

    public JsonObject getSettings() {
        return settings;
    }

    public void setSettings(JsonObject settings) {
        this.settings = settings != null ? settings.deepCopy() : new JsonObject();
    }

    public Preset copy() {
        return new Preset(id, name, createdAt, updatedAt, schemaVersion, clientVersion, builtin, settings);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Preset preset = (Preset) o;
        return Objects.equals(id, preset.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name;
    }
}

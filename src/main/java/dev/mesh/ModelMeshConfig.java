package dev.mesh;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ModelMeshConfig {
    public boolean enabled = true;

    public boolean singleSlotMode = false;
    public int singleSlot = 0;
    public boolean legitMode = true;

    public boolean combatGuard = true;
    public boolean weaponSwitch = true;
    public boolean ignoreInstantBreak = true;
    public boolean lockWhileMining = true;

    public boolean durabilitySaver = true;
    public int durabilityThreshold = 5;
    public boolean preferSilkTouch = false;
    public boolean restorePreviousItem = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path getConfigPath() {
        try {
            var l = FabricLoader.getInstance();
            if (l != null && l.getConfigDir() != null) return l.getConfigDir().resolve("mesh.json");
        } catch (Throwable ignored) {}
        return Path.of("config", "mesh.json");
    }

    public static ModelMeshConfig load() {
        try {
            Path path = getConfigPath();
            if (Files.exists(path)) {
                ModelMeshConfig config = GSON.fromJson(Files.readString(path), ModelMeshConfig.class);
                if (config != null) {
                    return config;
                }
            }
        } catch (Exception ignored) {
        }
        return new ModelMeshConfig();
    }

    public void save() {
    }
}

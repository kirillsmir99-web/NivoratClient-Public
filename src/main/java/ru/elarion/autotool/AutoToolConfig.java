package ru.elarion.autotool;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public final class AutoToolConfig {
    public boolean enabled = true;
    
    
    public boolean singleSlotMode = false;     
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
            if (l != null && l.getConfigDir() != null) return l.getConfigDir().resolve("autotool.json");
        } catch (Throwable ignored) {}
        return Path.of("config", "autotool.json");
    }

    public static AutoToolConfig load() {
        try {
            Path path = getConfigPath();
            if (Files.exists(path)) {
                AutoToolConfig config = GSON.fromJson(Files.readString(path), AutoToolConfig.class);
                if (config != null) {
                    return config;
                }
            }
        } catch (Exception ignored) {
        }
        AutoToolConfig fallback = new AutoToolConfig();
        fallback.save();
        return fallback;
    }

    public void save() {
        try {
            Path path = getConfigPath();
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, GSON.toJson(this));
        } catch (Exception ignored) {
        }
    }
}

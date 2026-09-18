package ru.elarion.autogg;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class AutoGGConfig {
    public boolean enabled = true;
    public boolean sendOnKill = true;
    public boolean sendOnOwnDeath = false;
    public boolean randomOrder = false;
    public List<String> phrases = new ArrayList<>(List.of("GG", "GGWP", "EZ"));
    public int selected = 0;

    private static final Gson G = new GsonBuilder().setPrettyPrinting().create();

    private static Path p() {
        try {
            var l = FabricLoader.getInstance();
            if (l != null && l.getConfigDir() != null) return l.getConfigDir().resolve("autogg.json");
        } catch (Throwable ignored) {}
        return Path.of("config", "autogg.json");
    }

    public static AutoGGConfig load() {
        try {
            if (Files.exists(p())) {
                AutoGGConfig c = G.fromJson(Files.readString(p()), AutoGGConfig.class);
                if (c == null) c = new AutoGGConfig();
                c.enabled = true;
                if (c.phrases == null) c.phrases = new ArrayList<>();
                c.phrases.removeIf(s -> s == null || s.isBlank() || s.equalsIgnoreCase("Новая фраза"));
                if (c.phrases.isEmpty()) {
                    c.phrases.add("GG");
                    c.phrases.add("GGWP");
                    c.phrases.add("EZ");
                }
                c.selected = Math.max(0, Math.min(c.selected, c.phrases.size() - 1));
                return c;
            }
        } catch (Exception ignored) {
        }
        return new AutoGGConfig();
    }

    public String currentPhrase() {
        if (phrases == null || phrases.isEmpty()) return "GG";
        int idx = Math.max(0, Math.min(selected, phrases.size() - 1));
        return phrases.get(idx);
    }

    public String nextPhrase() {
        if (phrases == null || phrases.isEmpty()) return "GG";
        if (randomOrder) {
            int idx = (int) (Math.random() * phrases.size());
            return phrases.get(idx);
        }
        int idx = Math.max(0, Math.min(selected, phrases.size() - 1));
        String result = phrases.get(idx);
        selected = (idx + 1) % phrases.size();
        save();
        return result;
    }

    public String phrase() {
        return nextPhrase();
    }

    public void save() {
        try {
            Files.createDirectories(p().getParent());
            Files.writeString(p(), G.toJson(this));
        } catch (Exception ignored) {
        }
    }
}


package dev.audio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class AudioSyncConfig {
    public static final int MAX_PHRASES = 8;
    public static final List<String> DEFAULT_PHRASES = List.of("GGWP", "ez", "GG");

    public boolean enabled = true;
    public boolean sendOnKill = true;
    public boolean sendOnOwnDeath = true;
    public boolean randomOrder = false;
    public List<String> phrases = new ArrayList<>(DEFAULT_PHRASES);
    public List<String> favorites = new ArrayList<>();
    public int selected = 0;

    private static final Gson G = new GsonBuilder().setPrettyPrinting().create();

    private static Path p() {
        try {
            var l = FabricLoader.getInstance();
            if (l != null && l.getConfigDir() != null) return l.getConfigDir().resolve("autogg.json");
        } catch (Throwable ignored) {}
        return Path.of("config", "autogg.json");
    }

    public static AudioSyncConfig load() {
        try {
            if (Files.exists(p())) {
                AudioSyncConfig c = G.fromJson(Files.readString(p()), AudioSyncConfig.class);
                if (c == null) c = new AudioSyncConfig();
                c.enabled = true;
                if (c.phrases == null) c.phrases = new ArrayList<>();
                c.phrases.removeIf(s -> s == null || s.isBlank());

                List<String> deduped = new ArrayList<>();
                for (String p : c.phrases) {
                    String trimmed = p.trim();
                    if (deduped.stream().noneMatch(existing -> existing.equalsIgnoreCase(trimmed))) {
                        deduped.add(trimmed);
                    }
                }
                c.phrases = deduped;

                if (c.phrases.isEmpty()) {
                    c.phrases = new ArrayList<>(DEFAULT_PHRASES);
                }

                if (c.phrases.size() > MAX_PHRASES) {
                    c.phrases = new ArrayList<>(c.phrases.subList(0, MAX_PHRASES));
                }

                c.selected = Math.max(0, Math.min(c.selected, c.phrases.size() - 1));
                if (c.favorites == null) c.favorites = new ArrayList<>();
                List<String> loadedPhrases = c.phrases;
                c.favorites.removeIf(phrase -> !loadedPhrases.contains(phrase));
                return c;
            }
        } catch (Exception ignored) {
        }
        return new AudioSyncConfig();
    }

    public String currentPhrase() {
        if (phrases == null || phrases.isEmpty()) return "GGWP";
        int idx = Math.max(0, Math.min(selected, phrases.size() - 1));
        if (favorites != null && !favorites.isEmpty() && !favorites.contains(phrases.get(idx)))
            for (String phrase : phrases) if (favorites.contains(phrase)) return phrase;
        return phrases.get(idx);
    }

    public String nextPhrase() {
        if (phrases == null || phrases.isEmpty()) return "GGWP";
        if (favorites != null && !favorites.isEmpty()) {
            List<String> available = phrases.stream().filter(favorites::contains).toList();
            if (!available.isEmpty()) return randomOrder ? available.get(ThreadLocalRandom.current().nextInt(available.size())) : currentPhrase();
        }
        if (randomOrder) {
            int idx = ThreadLocalRandom.current().nextInt(phrases.size());
            return phrases.get(idx);
        }
        int idx = Math.max(0, Math.min(selected, phrases.size() - 1));
        return phrases.get(idx);
    }

    public String phrase() {
        return nextPhrase();
    }

    public void save() {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
            return;
        }
        try {
            if (phrases != null && phrases.size() > MAX_PHRASES) {
                phrases = new ArrayList<>(phrases.subList(0, MAX_PHRASES));
            }
            Files.createDirectories(p().getParent());
            activity.client.gui.custom.utils.storage.AtomicFiles.writeUtf8(p(), G.toJson(this));
        } catch (Exception ignored) {
        }
    }
}

package activity.client.module;

import dev.audio.AudioSyncConfig;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AutoGgFavoritesTest {
    @Test void automaticMessagesOnlyUseFavoritesAndIgnoreDeletedPhrases() {
        var cfg = new AudioSyncConfig();
        cfg.phrases = new ArrayList<>(List.of("GG", "WP", "GF")); cfg.selected = 0;
        cfg.favorites = new ArrayList<>(List.of("WP", "GF", "deleted"));
        assertEquals("WP", cfg.currentPhrase()); assertEquals("WP", cfg.nextPhrase());
        cfg.randomOrder = true;
        for (int i = 0; i < 100; i++) assertTrue(List.of("WP", "GF").contains(cfg.nextPhrase()));
        cfg.phrases.remove("WP"); assertEquals("GF", cfg.currentPhrase());
        cfg.favorites.clear(); cfg.randomOrder = false; assertEquals("GG", cfg.nextPhrase());
    }
}

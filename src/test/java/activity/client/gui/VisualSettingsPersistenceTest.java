package activity.client.gui;

import activity.client.gui.custom.VisualSettingsStore;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class VisualSettingsPersistenceTest {
    @Test void privateSoundFieldsAreIncludedAndMalformedValuesDoNotBlockOtherSettings() {
        var before = VisualSettingsStore.snapshot("sounds");
        assertTrue(before.containsKey("ClientSounds.volume"));
        assertTrue(before.containsKey("ClientSounds.pitch"));
        assertTrue(before.containsKey("ClientSounds.toggleSoundChoice"));
        try {
            VisualSettingsStore.apply("sounds", Map.of("ClientSounds.volume", 0.35f,
                    "ClientSounds.pitch", "invalid", "ClientSounds.guiSound", false));
            var changed = VisualSettingsStore.snapshot("sounds");
            assertEquals(0.35f, (Float)changed.get("ClientSounds.volume"), 0.001f);
            assertEquals(before.get("ClientSounds.pitch"), changed.get("ClientSounds.pitch"));
            assertEquals(false, changed.get("ClientSounds.guiSound"));
            VisualSettingsStore.apply("sounds", Map.of("ClientSounds.volume", Float.NaN));
            assertEquals(changed.get("ClientSounds.volume"), VisualSettingsStore.snapshot("sounds").get("ClientSounds.volume"));
        } finally { VisualSettingsStore.apply("sounds", before); }
    }
}

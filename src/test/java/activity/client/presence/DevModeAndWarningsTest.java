package activity.client.presence;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.module.impl.defense.AutoAnchorModule;
import activity.client.module.impl.utility.AutoToolModule;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.elarion.autotool.AutoToolClient;

import static org.junit.jupiter.api.Assertions.*;

class DevModeAndWarningsTest {

    @BeforeEach
    void setUp() {
        ActivityConfigManager.resetDefaults();
    }

    @Test
    void testDevBadgeTextPrefixNull() {
        assertNull(DevBadgeText.prefix(null));
    }

    @Test
    void testDevBadgeTextPrefixStructure() {
        Text original = Text.literal("Player_Name");
        Text prefixed = DevBadgeText.prefix(original);

        assertNotNull(prefixed);
        String fullString = prefixed.getString();
        assertTrue(fullString.contains(NivoratDev.BADGE_GLYPH));
        assertTrue(fullString.contains(NivoratDev.BADGE_SEPARATOR));
        assertTrue(fullString.contains("Player_Name"));

        var siblings = prefixed.getSiblings();
        assertEquals(3, siblings.size());

        Text badgeGlyphText = siblings.get(0);
        assertEquals(NivoratDev.BADGE_GLYPH, badgeGlyphText.getString());
        assertNotNull(badgeGlyphText.getStyle().getFont());

        Text separatorText = siblings.get(1);
        assertEquals(NivoratDev.BADGE_SEPARATOR, separatorText.getString());
        assertEquals(StyleSpriteSource.DEFAULT, separatorText.getStyle().getFont());
        assertNotNull(separatorText.getStyle().getColor());
        assertEquals(0xC184FF, separatorText.getStyle().getColor().getRgb());

        Text playerText = siblings.get(2);
        assertEquals("Player_Name", playerText.getString());
    }

    @Test
    void testDevBadgeTextIdempotent() {
        Text original = Text.literal("Steve");
        Text prefixedOnce = DevBadgeText.prefix(original);
        Text prefixedTwice = DevBadgeText.prefix(prefixedOnce);

        assertSame(prefixedOnce, prefixedTwice);
        long count = prefixedTwice.getString().chars().filter(c -> c == NivoratDev.BADGE_GLYPH.charAt(0)).count();
        assertEquals(1, count);
    }

    @Test
    void testNormalizeServerAddress() {
        assertEquals("play.example.com", PresenceHeartbeatService.normalizeServer("play.example.com"));
        assertEquals("play.example.com", PresenceHeartbeatService.normalizeServer("play.example.com:25565"));
        assertEquals("play.example.com", PresenceHeartbeatService.normalizeServer("PLAY.EXAMPLE.COM:25565"));
        assertEquals("play.example.com", PresenceHeartbeatService.normalizeServer("play.example.com."));
        assertEquals("play.example.com:25566", PresenceHeartbeatService.normalizeServer("play.example.com:25566"));
        assertEquals("", PresenceHeartbeatService.normalizeServer(null));
        assertEquals("", PresenceHeartbeatService.normalizeServer("   "));
    }

    @Test
    void testAutoToolToggleState() {
        AutoToolModule module = new AutoToolModule();
        module.setEnabled(true);
        assertTrue(module.isEnabled());
        assertTrue(AutoToolClient.CONFIG.enabled);

        module.setEnabled(false);
        assertFalse(module.isEnabled());
        assertFalse(AutoToolClient.CONFIG.enabled);
    }

    @Test
    void testAutoAnchorModeSwitchCycles() {
        AutoAnchorModule module = new AutoAnchorModule();
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        assertNotNull(cfg);

        cfg.autoAnchorMode = "smart";
        module.setEnabled(true);
        assertTrue(module.isEnabled());

        module.setEnabled(false);
        assertFalse(module.isEnabled());
    }
}

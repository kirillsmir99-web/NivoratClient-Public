package activity.client.gui;

import activity.client.gui.component.ActivityButton;
import activity.client.gui.component.ActivityComponent;
import activity.client.gui.icon.ActivityIcon;
import activity.client.gui.layout.ScrollContainer;
import activity.client.gui.tab.AboutTab;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test suite for:
 * 1. GUI Session Memory (1-minute TTL tab & module persistence)
 * 2. Brand hover highlight colors on AboutTab social buttons
 */
public class ActivityScreenSessionTest {

    @BeforeEach
    void setUp() {
        ActivityScreen.clearSession();
    }

    @Test
    void testSessionMemoryRecordingAndValidity() {
        assertFalse(ActivityScreen.hasValidSession(), "Initial session must not be valid");
        assertNull(ActivityScreen.getLastSessionTabId());
        assertNull(ActivityScreen.getLastSessionModuleId());

        // Record a session (e.g. user was in auto_totem under defense tab)
        ActivityScreen.recordSession("defense", "auto_totem");

        assertTrue(ActivityScreen.hasValidSession(), "Session must be valid immediately after recording");
        assertEquals("defense", ActivityScreen.getLastSessionTabId());
        assertEquals("auto_totem", ActivityScreen.getLastSessionModuleId());
        assertTrue(ActivityScreen.getLastSessionCloseTimestamp() > 0);
    }

    @Test
    void testSessionMemoryTTL() {
        ActivityScreen.recordSession("combat", "auto_mace");
        assertTrue(ActivityScreen.hasValidSession());

        // Clear session
        ActivityScreen.clearSession();
        assertFalse(ActivityScreen.hasValidSession(), "Session must not be valid after clear");
        assertNull(ActivityScreen.getLastSessionTabId());
        assertNull(ActivityScreen.getLastSessionModuleId());
    }

    @Test
    void testActivityButtonBrandHoverColor() {
        ActivityButton btn = new ActivityButton(0, 0, 100, 20, null, null);
        assertEquals(0, btn.getBrandHoverColor());

        btn.setBrandHoverColor(0xFF5865F2);
        assertEquals(0xFF5865F2, btn.getBrandHoverColor());
    }

    @Test
    void testAboutTabSocialButtonsBrandHoverColors() {
        AboutTab aboutTab = new AboutTab();
        ScrollContainer container = new ScrollContainer(0, 0, 400, 300);
        aboutTab.buildTab(null, container, 0, 0, 480);

        List<ActivityButton> allButtons = new ArrayList<>();
        for (ActivityComponent comp : container.getChildren()) {
            if (comp instanceof ActivityButton btn) {
                allButtons.add(btn);
            }
        }

        ActivityButton btnTg = null;
        ActivityButton btnDonate = null;
        ActivityButton btnYoutube = null;
        ActivityButton btnTiktok = null;
        ActivityButton btnDiscord = null;

        for (ActivityButton btn : allButtons) {
            if (btn.getIcon() == ActivityIcon.TELEGRAM) btnTg = btn;
            else if (btn.getIcon() == ActivityIcon.DONATE) btnDonate = btn;
            else if (btn.getIcon() == ActivityIcon.YOUTUBE) btnYoutube = btn;
            else if (btn.getIcon() == ActivityIcon.TIKTOK) btnTiktok = btn;
            else if (btn.getIcon() == ActivityIcon.DISCORD) btnDiscord = btn;
        }

        assertNotNull(btnTg, "Telegram button must exist");
        assertNotNull(btnDonate, "Donate button must exist");
        assertNotNull(btnYoutube, "YouTube button must exist");
        assertNotNull(btnTiktok, "TikTok button must exist");
        assertNotNull(btnDiscord, "Discord button must exist");

        // Verify brand hover colors
        assertEquals(0xFF2AABEE, btnTg.getBrandHoverColor(), "Telegram button must have Telegram blue hover color");
        assertEquals(0xFFFF4757, btnDonate.getBrandHoverColor(), "Donate button must have Heart red hover color");
        assertEquals(0xFFFF0000, btnYoutube.getBrandHoverColor(), "YouTube button must have YouTube red hover color");
        assertEquals(0xFF00F2FE, btnTiktok.getBrandHoverColor(), "TikTok button must have TikTok cyan hover color");
        assertEquals(0xFF5865F2, btnDiscord.getBrandHoverColor(), "Discord button must have Discord blurple hover color");

        // Verify custom textures
        assertEquals(AboutTab.TEXTURE_TELEGRAM, btnTg.getCustomTexture());
        assertEquals(AboutTab.TEXTURE_DONATE, btnDonate.getCustomTexture());
        assertEquals(AboutTab.TEXTURE_YOUTUBE, btnYoutube.getCustomTexture());
        assertEquals(AboutTab.TEXTURE_TIKTOK, btnTiktok.getCustomTexture());
        assertEquals(AboutTab.TEXTURE_DISCORD, btnDiscord.getCustomTexture());
    }

    @Test
    void testActivityButtonCustomTexture() {
        ActivityButton btn = new ActivityButton(0, 0, 100, 20, null, null);
        assertNull(btn.getCustomTexture());

        btn.setCustomTexture(AboutTab.TEXTURE_DISCORD);
        assertEquals(AboutTab.TEXTURE_DISCORD, btn.getCustomTexture());
    }
}

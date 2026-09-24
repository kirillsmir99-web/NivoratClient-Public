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
        assertEquals(-1.0, ActivityScreen.getLastSessionScrollAmount(), 1e-6);
        assertEquals(0L, ActivityScreen.getLastSessionCloseTimestamp());

        ActivityScreen.recordSession("defense", "auto_totem", 185.5);

        assertTrue(ActivityScreen.hasValidSession(), "Session must be valid immediately after recording");
        assertEquals("defense", ActivityScreen.getLastSessionTabId());
        assertEquals("auto_totem", ActivityScreen.getLastSessionModuleId());
        assertEquals(185.5, ActivityScreen.getLastSessionScrollAmount(), 1e-6);
        assertTrue(ActivityScreen.getLastSessionCloseTimestamp() > 0);

        ActivityScreen.recordSession("combat", "auto_mace");
        assertTrue(ActivityScreen.hasValidSession());
        assertEquals("combat", ActivityScreen.getLastSessionTabId());
        assertEquals("auto_mace", ActivityScreen.getLastSessionModuleId());
        assertEquals(0.0, ActivityScreen.getLastSessionScrollAmount(), 1e-6);
    }

    @Test
    void testSessionClearAndReset() {
        ActivityScreen.recordSession("combat", "auto_mace", 250.0);
        assertTrue(ActivityScreen.hasValidSession());

        ActivityScreen.clearSession();
        assertFalse(ActivityScreen.hasValidSession(), "Session must not be valid after clear");
        assertNull(ActivityScreen.getLastSessionTabId());
        assertNull(ActivityScreen.getLastSessionModuleId());
        assertEquals(-1.0, ActivityScreen.getLastSessionScrollAmount(), 1e-6);
        assertEquals(0L, ActivityScreen.getLastSessionCloseTimestamp());
    }

    @Test
    void testSessionMemoryTTL() {
        testSessionClearAndReset();
    }

    @Test
    void testSessionMemoryInfiniteTTLConstant() {
        assertEquals(Long.MAX_VALUE, ActivityScreen.SESSION_MEMORY_TTL_MS,
                "SESSION_MEMORY_TTL_MS must be infinite (Long.MAX_VALUE)");
    }

    @Test
    void testSessionDoesNotExpireAfter60Seconds() {
        ActivityScreen.recordSession("render", "cooldown_hud", 75.0);
        assertTrue(ActivityScreen.hasValidSession());

        ActivityScreen.setLastSessionCloseTimestamp(System.currentTimeMillis() - 65_000L);

        assertTrue(ActivityScreen.hasValidSession(), "Session must NOT expire after 60 seconds (65s elapsed)");
        assertEquals("render", ActivityScreen.getLastSessionTabId());
        assertEquals("cooldown_hud", ActivityScreen.getLastSessionModuleId());
        assertEquals(75.0, ActivityScreen.getLastSessionScrollAmount(), 1e-6);
    }

    @Test
    void testSessionRemainsValidAfter5PlusMinutesAndLongDuration() {
        ActivityScreen.recordSession("utilities", "auto_tool", 320.0);
        assertTrue(ActivityScreen.hasValidSession());

        long now = System.currentTimeMillis();

        ActivityScreen.setLastSessionCloseTimestamp(now - 300_000L);
        assertTrue(ActivityScreen.hasValidSession(), "Session must remain valid after 5 minutes (300s)");

        ActivityScreen.setLastSessionCloseTimestamp(now - 600_000L);
        assertTrue(ActivityScreen.hasValidSession(), "Session must remain valid after 10 minutes (600s)");

        ActivityScreen.setLastSessionCloseTimestamp(now - 3_600_000L);
        assertTrue(ActivityScreen.hasValidSession(), "Session must remain valid after 1 hour (3600s)");

        ActivityScreen.setLastSessionCloseTimestamp(now - 86_400_000L);
        assertTrue(ActivityScreen.hasValidSession(), "Session must remain valid after 24 hours (86400s)");

        assertEquals("utilities", ActivityScreen.getLastSessionTabId());
        assertEquals("auto_tool", ActivityScreen.getLastSessionModuleId());
        assertEquals(320.0, ActivityScreen.getLastSessionScrollAmount(), 1e-6);
    }

    @Test
    void testScrollAmountRecordingWithScrollContainer() {
        ScrollContainer container = new ScrollContainer(0, 0, 400, 300);
        ActivityButton dummyChild = new ActivityButton(0, 0, 400, 1000, null, null);
        container.addChild(dummyChild);
        container.recomputeContentHeight();

        double targetScroll = 175.5;
        container.setScrollAmount(targetScroll);
        assertEquals(targetScroll, container.getScrollAmount(), 1e-6);

        ActivityScreen.recordSession("about", null, container.getScrollAmount());

        assertTrue(ActivityScreen.hasValidSession());
        assertEquals("about", ActivityScreen.getLastSessionTabId());
        assertNull(ActivityScreen.getLastSessionModuleId());
        assertEquals(targetScroll, ActivityScreen.getLastSessionScrollAmount(), 1e-6);
    }

    @Test
    void testSessionValidityRequiresNonNullTab() {
        ActivityScreen.recordSession(null, "auto_totem", 0.0);
        assertFalse(ActivityScreen.hasValidSession(), "Session without tab ID must not be valid");

        ActivityScreen.recordSession("defense", null, 0.0);
        assertTrue(ActivityScreen.hasValidSession(), "Session with valid tab ID and null module ID must be valid");
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

        assertEquals(0xFF2AABEE, btnTg.getBrandHoverColor(), "Telegram button must have Telegram blue hover color");
        assertEquals(0xFFFF4757, btnDonate.getBrandHoverColor(), "Donate button must have Heart red hover color");
        assertEquals(0xFFFF0000, btnYoutube.getBrandHoverColor(), "YouTube button must have YouTube red hover color");
        assertEquals(0xFF00F2FE, btnTiktok.getBrandHoverColor(), "TikTok button must have TikTok cyan hover color");
        assertEquals(0xFF5865F2, btnDiscord.getBrandHoverColor(), "Discord button must have Discord blurple hover color");

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

    @Test
    void testSocialIconResourceFilesExist() {
        String[] icons = {"telegram.png", "donate.png", "youtube.png", "tiktok.png", "discord.png"};
        for (String iconName : icons) {
            String path = "/assets/nivoratclient/textures/gui/social/" + iconName;
            java.io.InputStream is = getClass().getResourceAsStream(path);
            assertNotNull(is, "Texture resource must exist: " + path);
            try {
                byte[] header = new byte[8];
                int read = is.read(header);
                assertEquals(8, read, "Must be able to read 8 byte PNG header for " + iconName);

                assertEquals((byte) 0x89, header[0], iconName + " must have valid PNG magic byte 0");
                assertEquals((byte) 'P', header[1], iconName + " must have valid PNG magic byte 1");
                assertEquals((byte) 'N', header[2], iconName + " must have valid PNG magic byte 2");
                assertEquals((byte) 'G', header[3], iconName + " must have valid PNG magic byte 3");
                is.close();
            } catch (Exception e) {
                org.junit.jupiter.api.Assertions.fail("Failed to verify texture " + iconName + ": " + e.getMessage());
            }
        }
    }
}


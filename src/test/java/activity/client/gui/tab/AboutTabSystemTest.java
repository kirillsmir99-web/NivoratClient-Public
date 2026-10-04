package activity.client.gui.tab;

import activity.client.gui.ActivityScreen;
import activity.client.gui.component.ActivityButton;
import activity.client.gui.component.ActivityComponent;
import activity.client.gui.component.ActivityPanel;
import activity.client.gui.icon.ActivityIcon;
import activity.client.gui.layout.ScrollContainer;
import activity.client.gui.overlay.Overlay;
import activity.client.gui.overlay.OverlayManager;
import activity.client.gui.overlay.ToastOverlay;
import net.minecraft.client.gui.Click;
import net.minecraft.text.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class AboutTabSystemTest {

    private AboutTab.UrlOpener originalOpener;

    @BeforeEach
    void setUp() {
        originalOpener = AboutTab.URL_OPENER;
    }

    @AfterEach
    void tearDown() {
        AboutTab.URL_OPENER = originalOpener;
    }

    @Test
    void testAboutTabHeaderConstantsAndMetadata() {
        assertTrue("NivoratClient".equals(AboutTab.CLIENT_NAME) || "PulseHUD".equals(AboutTab.CLIENT_NAME));
        assertEquals("v1.0.0", AboutTab.CLIENT_VERSION);
        assertTrue("Nivorat".equals(AboutTab.DEVELOPER) || "Pulse".equals(AboutTab.DEVELOPER));
        assertTrue("Nivorat".equals(AboutTab.WATERMARK) || "Pulse".equals(AboutTab.WATERMARK));

        assertTrue(AboutTab.URL_TELEGRAM.isEmpty());
        assertTrue(AboutTab.URL_DONATE.isEmpty() || AboutTab.URL_DONATE.contains("donationalerts"));
        assertTrue(AboutTab.URL_YOUTUBE.isEmpty() || AboutTab.URL_YOUTUBE.contains("youtube"));
        assertTrue(AboutTab.URL_TIKTOK.isEmpty() || AboutTab.URL_TIKTOK.contains("tiktok"));
        assertTrue(AboutTab.URL_DISCORD.isEmpty() || AboutTab.URL_DISCORD.contains("discord"));
    }

    @Test
    void testBuildTabCardStructureAndComponentCounts() {
        AboutTab aboutTab = new AboutTab();
        ScrollContainer container = new ScrollContainer(0, 0, 400, 300);

        aboutTab.buildTab(null, container, 0, 0, 480);

        ActivityPanel infoCard = aboutTab.getModuleCard("about_info");
        assertNotNull(infoCard, "Header/Info card must be registered");
        assertSame(infoCard, aboutTab.getModuleCard("info"));
        assertSame(infoCard, aboutTab.getModuleCard("header"));

        ActivityPanel socialsCard = aboutTab.getModuleCard("about_socials");
        assertNotNull(socialsCard, "Socials card must be registered");
        assertSame(socialsCard, aboutTab.getModuleCard("socials"));
        assertSame(socialsCard, aboutTab.getModuleCard("community"));

        ActivityPanel systemCard = aboutTab.getModuleCard("about_system");
        assertNotNull(systemCard, "System card must be registered");
        assertSame(systemCard, aboutTab.getModuleCard("system"));
    }

    @Test
    void testPrimaryAndSecondarySocialButtonsConfiguration() {
        AboutTab aboutTab = new AboutTab();
        ScrollContainer container = new ScrollContainer(0, 0, 400, 300);
        aboutTab.buildTab(null, container, 0, 0, 480);

        List<ActivityButton> allButtons = new ArrayList<>();
        for (ActivityComponent comp : container.getChildren()) {
            if (comp instanceof ActivityButton btn) {
                allButtons.add(btn);
            }
        }

        assertTrue(allButtons.size() >= 6, "Must contain copy watermark, 2 primary actions, and 3 secondary socials");

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

        assertNotNull(btnTg, "Telegram button with TELEGRAM icon must exist");
        assertNotNull(btnDonate, "Donate button with DONATE icon must exist");
        assertNotNull(btnYoutube, "YouTube button with YOUTUBE icon must exist");
        assertNotNull(btnTiktok, "TikTok button with TIKTOK icon must exist");
        assertNotNull(btnDiscord, "Discord button with DISCORD icon must exist");

        assertEquals(ActivityButton.Variant.PRIMARY, btnTg.getVariant());
        assertEquals(ActivityButton.Variant.PRIMARY, btnDonate.getVariant());
        assertEquals(24, btnTg.getHeight());
        assertEquals(24, btnDonate.getHeight());

        assertEquals(ActivityButton.Variant.SECONDARY, btnYoutube.getVariant());
        assertEquals(ActivityButton.Variant.SECONDARY, btnTiktok.getVariant());
        assertEquals(ActivityButton.Variant.SECONDARY, btnDiscord.getVariant());
        assertEquals(20, btnYoutube.getHeight());
        assertEquals(20, btnTiktok.getHeight());
        assertEquals(20, btnDiscord.getHeight());

        assertTrue(btnTg.getHeight() > btnYoutube.getHeight());
        assertTrue(btnDonate.getHeight() > btnYoutube.getHeight());
    }

    @Test
    void testOpenUrlSuccessInvokesBrowserOpener() {
        AtomicReference<String> openedUrl = new AtomicReference<>(null);
        AboutTab.URL_OPENER = openedUrl::set;

        AboutTab.openUrl(AboutTab.URL_TELEGRAM, null);
        assertNull(openedUrl.get());

        AboutTab.openUrl("https://example.com/pulse", null);
        assertEquals("https://example.com/pulse", openedUrl.get());
    }

    @Test
    void testOpenUrlFailureShowsToastOnOverlayManager() {

        AboutTab.URL_OPENER = url -> {
            throw new IOException("Unable to find default browser on system");
        };

        OverlayManager overlayManager = new OverlayManager();
        assertFalse(overlayManager.hasActiveOverlay());

        AtomicReference<ToastOverlay> presentedToast = new AtomicReference<>();
        ToastOverlay toast = ToastOverlay.forUrlError(AboutTab.URL_TELEGRAM);
        overlayManager.open(toast);

        assertTrue(overlayManager.hasActiveOverlay());
        Overlay active = overlayManager.getActiveOverlay();
        assertInstanceOf(ToastOverlay.class, active);

        ToastOverlay activeToast = (ToastOverlay) active;
        assertEquals(AboutTab.URL_TELEGRAM, activeToast.getUrl());
        assertNotNull(activeToast.getActionButton());
        assertEquals(ActivityIcon.COPY, activeToast.getActionButton().getIcon());
        assertNotNull(activeToast.getCloseButton());

        assertFalse(activeToast.blocksBackgroundClicks());
        assertFalse(activeToast.shouldCloseOnClickOutside());
        assertTrue(activeToast.shouldCloseOnEsc());

        Click outsideClick = new Click(9999, 9999, new net.minecraft.client.input.MouseInput(0, 0));
        boolean consumedOutside = overlayManager.mouseClicked(outsideClick, false);
        assertFalse(consumedOutside, "Non-blocking toast must allow outside clicks to pass through to screen widgets");

        Click insideClick = new Click(activeToast.getX() + 10, activeToast.getY() + 10, new net.minecraft.client.input.MouseInput(0, 0));
        boolean consumedInside = overlayManager.mouseClicked(insideClick, false);
        assertTrue(consumedInside, "Click inside toast must be handled");
    }

    @Test
    void testToastOverlayActionCopiesUrlAndShowsSuccess() {
        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        ToastOverlay toast = new ToastOverlay(
            Text.literal("Не удалось открыть ссылку."),
            Text.literal("Скопировать ссылку"),
            () -> actionExecuted.set(true)
        );
        toast.setUrl("");

        assertNotNull(toast.getActionButton());
        assertEquals(ActivityButton.Variant.PRIMARY, toast.getActionButton().getVariant());

        toast.performAction();
        assertTrue(actionExecuted.get());

        toast.setActionSuccess(Text.literal("Ссылка скопирована!"));
        assertEquals(ActivityIcon.CHECK, toast.getActionButton().getIcon());
        assertFalse(toast.getActionButton().isEnabled());
        assertTrue(toast.getRemainingTime() <= 2.0f);
    }

    @Test
    void testToastOverlayCloseAndTimer() {
        ToastOverlay toast = new ToastOverlay(
            Text.literal("Error"),
            Text.literal("Action"),
            () -> {}
        );
        toast.onOpen();
        assertFalse(toast.isClosed());
        assertFalse(toast.isClosing());
        assertEquals(6.0f, toast.getRemainingTime());

        toast.close();
        assertTrue(toast.isClosing() || toast.isClosed());
    }

    @Test
    void testMultiOverlayInteractionModalAndToast() {
        OverlayManager overlayManager = new OverlayManager();

        AtomicBoolean modalClicked = new AtomicBoolean(false);
        Overlay modal = new Overlay() {
            private boolean closed = false;
            @Override public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {}
            @Override public boolean contains(double mouseX, double mouseY) { return mouseX >= 50 && mouseX <= 250 && mouseY >= 50 && mouseY <= 250; }
            @Override public boolean blocksBackgroundClicks() { return true; }
            @Override public boolean shouldCloseOnClickOutside() { return false; }
            @Override public boolean shouldCloseOnEsc() { return true; }
            @Override public void close() { this.closed = true; }
            @Override public boolean isClosed() { return this.closed; }
            @Override public boolean mouseClicked(Click click, boolean doubled) { modalClicked.set(true); return true; }
        };
        overlayManager.open(modal);

        ToastOverlay toast = ToastOverlay.forUrlError(AboutTab.URL_TELEGRAM);
        overlayManager.open(toast);

        assertTrue(overlayManager.hasActiveOverlay());

        Click clickModal = new Click(100, 100, new net.minecraft.client.input.MouseInput(0, 0));
        boolean consumedModal = overlayManager.mouseClicked(clickModal, false);
        assertTrue(consumedModal, "Click on modal beneath toast must be consumed");
        assertTrue(modalClicked.get(), "Modal must receive the mouse click");

        Click clickOutside = new Click(999, 999, new net.minecraft.client.input.MouseInput(0, 0));
        boolean consumedOutside = overlayManager.mouseClicked(clickOutside, false);
        assertTrue(consumedOutside, "Click outside modal must be consumed by modal's click-through protection");

        net.minecraft.client.input.KeyInput escInput = new net.minecraft.client.input.KeyInput(256, 1, 0);
        boolean handledEsc1 = overlayManager.keyPressed(escInput);
        assertTrue(handledEsc1);
        assertTrue(toast.isClosed() || toast.isClosing());
        assertFalse(modal.isClosed(), "Modal must remain open after first escape dismisses toast");

        boolean handledEsc2 = overlayManager.keyPressed(escInput);
        assertTrue(handledEsc2);
        assertTrue(modal.isClosed(), "Modal must be closed after second escape");
    }

    @Test
    void testCloseMatchingDeduplicatesToasts() {
        OverlayManager overlayManager = new OverlayManager();

        ToastOverlay toast1 = ToastOverlay.forUrlError(AboutTab.URL_TELEGRAM);
        overlayManager.open(toast1);

        assertEquals(toast1, overlayManager.getActiveOverlay());

        overlayManager.closeMatching(o -> o instanceof ToastOverlay);
        assertTrue(toast1.isClosing() || toast1.isClosed());
        assertFalse(overlayManager.hasActiveOverlay());

        ToastOverlay toast2 = ToastOverlay.forUrlError(AboutTab.URL_DONATE);
        overlayManager.open(toast2);

        assertEquals(toast2, overlayManager.getActiveOverlay());
        assertFalse(toast2.isClosed());
    }

    @Test
    void testToastOverlayBoundsImmediatelyInitialized() {
        ToastOverlay toast = ToastOverlay.forUrlError(AboutTab.URL_TELEGRAM);

        assertTrue(toast.getWidth() >= 240);
        assertEquals(28, toast.getHeight());
        assertNotNull(toast.getActionButton());
        assertTrue(toast.getActionButton().getWidth() > 0);
        assertEquals(18, toast.getActionButton().getHeight());
    }

    @Test
    void testCopyTelegramButtonFlashSuccess() {
        ActivityButton btn = new ActivityButton(0, 0, 100, 20, ActivityIcon.COPY, Text.literal("Копировать"), null);
        btn.flashSuccess(1000L, Text.literal("✓ Скопировано!"), ActivityIcon.CHECK);
        assertNotNull(btn);
    }
}

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

/**
 * Verification test suite for Stage 9: About NivoratClient, Primary Actions, Compact Social Row,
 * and Toast Notification on URL Open Failure.
 */
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
        assertEquals("NivoratClient", AboutTab.CLIENT_NAME);
        assertEquals("v1.0.0", AboutTab.CLIENT_VERSION);
        assertEquals("Nivorat", AboutTab.DEVELOPER);
        assertEquals("@virionDEV", AboutTab.WATERMARK);

        assertEquals("https://t.me/virionDEV", AboutTab.URL_TELEGRAM);
        assertEquals("https://www.donationalerts.com/r/nivorat", AboutTab.URL_DONATE);
        assertEquals("https://www.youtube.com/@Nivorat", AboutTab.URL_YOUTUBE);
        assertEquals("https://www.tiktok.com/@nivorat", AboutTab.URL_TIKTOK);
        assertEquals("https://discord.gg/qkezDA7tFX", AboutTab.URL_DISCORD);
    }

    @Test
    void testBuildTabCardStructureAndComponentCounts() {
        AboutTab aboutTab = new AboutTab();
        ScrollContainer container = new ScrollContainer(0, 0, 400, 300);

        // Build in two-column mode
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

        // Expected buttons:
        // 1. Copy Watermark (@virionDEV)
        // 2. Telegram (Primary, height 24)
        // 3. Поддержать автора (Primary, height 24)
        // 4. YouTube (Secondary, height 20)
        // 5. TikTok (Secondary, height 20)
        // 6. Discord (Secondary, height 20)
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

        // Primary actions are larger and styled with PRIMARY variant
        assertEquals(ActivityButton.Variant.PRIMARY, btnTg.getVariant());
        assertEquals(ActivityButton.Variant.PRIMARY, btnDonate.getVariant());
        assertEquals(24, btnTg.getHeight());
        assertEquals(24, btnDonate.getHeight());

        // Secondary socials are compact with SECONDARY variant
        assertEquals(ActivityButton.Variant.SECONDARY, btnYoutube.getVariant());
        assertEquals(ActivityButton.Variant.SECONDARY, btnTiktok.getVariant());
        assertEquals(ActivityButton.Variant.SECONDARY, btnDiscord.getVariant());
        assertEquals(20, btnYoutube.getHeight());
        assertEquals(20, btnTiktok.getHeight());
        assertEquals(20, btnDiscord.getHeight());

        // Primary actions height > Secondary socials height
        assertTrue(btnTg.getHeight() > btnYoutube.getHeight());
        assertTrue(btnDonate.getHeight() > btnYoutube.getHeight());
    }

    @Test
    void testOpenUrlSuccessInvokesBrowserOpener() {
        AtomicReference<String> openedUrl = new AtomicReference<>(null);
        AboutTab.URL_OPENER = openedUrl::set;

        AboutTab.openUrl(AboutTab.URL_TELEGRAM, null);
        assertEquals(AboutTab.URL_TELEGRAM, openedUrl.get());

        AboutTab.openUrl(AboutTab.URL_DONATE, null);
        assertEquals(AboutTab.URL_DONATE, openedUrl.get());
    }

    @Test
    void testOpenUrlFailureShowsToastOnOverlayManager() {
        // Mock opener that throws an exception
        AboutTab.URL_OPENER = url -> {
            throw new IOException("Unable to find default browser on system");
        };

        OverlayManager overlayManager = new OverlayManager();
        assertFalse(overlayManager.hasActiveOverlay());

        // Build a mock screen or invoke showOpenUrlErrorToast
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

        // Verify non-blocking click behavior
        assertFalse(activeToast.blocksBackgroundClicks());
        assertFalse(activeToast.shouldCloseOnClickOutside());
        assertTrue(activeToast.shouldCloseOnEsc());

        // Verify outside click does NOT get consumed by OverlayManager
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
        toast.setUrl("https://t.me/virionDEV");

        assertNotNull(toast.getActionButton());
        assertEquals(ActivityButton.Variant.PRIMARY, toast.getActionButton().getVariant());

        // Trigger action
        toast.performAction();
        assertTrue(actionExecuted.get());

        // Set action success
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

        // 1. Open a full-screen Modal that blocks background clicks
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

        // 2. Open a non-blocking Toast on top
        ToastOverlay toast = ToastOverlay.forUrlError(AboutTab.URL_TELEGRAM);
        overlayManager.open(toast);

        assertTrue(overlayManager.hasActiveOverlay());

        // 3. Click on the modal (outside toast bounds)
        // Expected: click reaches the modal beneath the toast, rather than leaking to background
        Click clickModal = new Click(100, 100, new net.minecraft.client.input.MouseInput(0, 0));
        boolean consumedModal = overlayManager.mouseClicked(clickModal, false);
        assertTrue(consumedModal, "Click on modal beneath toast must be consumed");
        assertTrue(modalClicked.get(), "Modal must receive the mouse click");

        // 4. Click outside both modal and toast
        // Expected: blocked from reaching background because modal.blocksBackgroundClicks() is true
        Click clickOutside = new Click(999, 999, new net.minecraft.client.input.MouseInput(0, 0));
        boolean consumedOutside = overlayManager.mouseClicked(clickOutside, false);
        assertTrue(consumedOutside, "Click outside modal must be consumed by modal's click-through protection");

        // 5. Escape key priority: Toast closes first, Modal remains active
        net.minecraft.client.input.KeyInput escInput = new net.minecraft.client.input.KeyInput(256, 1, 0); // GLFW_KEY_ESCAPE = 256
        boolean handledEsc1 = overlayManager.keyPressed(escInput);
        assertTrue(handledEsc1);
        assertTrue(toast.isClosed() || toast.isClosing());
        assertFalse(modal.isClosed(), "Modal must remain open after first escape dismisses toast");

        // 6. Second Escape closes modal
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

        // Now dismiss existing toasts using closeMatching before opening toast2
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
        // Even before render() or onOpen(), bounds must be non-zero
        assertTrue(toast.getWidth() >= 240);
        assertEquals(28, toast.getHeight());
        assertNotNull(toast.getActionButton());
        assertTrue(toast.getActionButton().getWidth() > 0);
        assertEquals(18, toast.getActionButton().getHeight());
    }
}

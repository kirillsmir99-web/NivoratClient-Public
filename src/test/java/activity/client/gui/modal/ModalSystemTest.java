package activity.client.gui.modal;

import activity.client.gui.component.ActivityToggle;
import activity.client.gui.overlay.OverlayManager;
import net.minecraft.text.Text;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class ModalSystemTest {

    private OverlayManager overlayManager;
    private ModalManager modalManager;

    @BeforeEach
    void setUp() {
        overlayManager = new OverlayManager();
        modalManager = new ModalManager(overlayManager);
    }

    @Test
    void testModalManagerLifecycle() {
        assertFalse(modalManager.hasActiveModal());
        assertNull(modalManager.getActiveModal());

        AtomicBoolean confirmed = new AtomicBoolean(false);
        ConfirmationModal modal = modalManager.showConfirmation(
            Text.literal("Test Title"),
            Text.literal("Test Description"),
            Text.literal("Confirm"),
            true,
            () -> confirmed.set(true)
        );

        assertTrue(modalManager.hasActiveModal());
        assertEquals(modal, modalManager.getActiveModal());
        assertTrue(overlayManager.hasActiveOverlay());

        assertTrue(modal.contains(0, 0));
        assertTrue(modal.contains(1920, 1080));
        assertFalse(modal.shouldCloseOnClickOutside());
        assertTrue(modal.shouldCloseOnEsc());

        modalManager.close(modal);
        assertTrue(modal.isClosing() || modal.isClosed());
    }

    @Test
    void testConfirmationModalCallbacks() {
        AtomicBoolean confirmed = new AtomicBoolean(false);
        AtomicBoolean cancelled = new AtomicBoolean(false);

        ConfirmationModal modal = new ConfirmationModal(
            Text.literal("Confirm Action"),
            Text.literal("Are you sure?"),
            Text.literal("Yes"),
            Text.literal("No"),
            false,
            () -> confirmed.set(true),
            () -> cancelled.set(true)
        );
        modalManager.open(modal);

        assertFalse(confirmed.get());
        assertFalse(cancelled.get());

        modal.getCancelButton().onPress();
        assertTrue(cancelled.get());
        assertFalse(confirmed.get());
    }

    @Test
    void testConfirmationModalConfirm() {
        AtomicBoolean confirmed = new AtomicBoolean(false);
        AtomicBoolean cancelled = new AtomicBoolean(false);

        ConfirmationModal modal = new ConfirmationModal(
            Text.literal("Confirm Action"),
            Text.literal("Are you sure?"),
            Text.literal("Yes"),
            Text.literal("No"),
            true,
            () -> confirmed.set(true),
            () -> cancelled.set(true)
        );
        modalManager.open(modal);

        modal.getConfirmButton().onPress();
        assertTrue(confirmed.get());
        assertFalse(cancelled.get());
    }

    @Test
    void testTextInputModalValidationAndSubmit() {
        AtomicReference<String> submitted = new AtomicReference<>(null);
        AtomicBoolean cancelled = new AtomicBoolean(false);

        TextInputModal modal = new TextInputModal(
            Text.literal("Enter Preset Name"),
            Text.literal("Name must not be empty"),
            Text.literal("Preset name..."),
            "",
            val -> val != null && !val.trim().isEmpty() && val.length() <= 16,
            submitted::set,
            () -> cancelled.set(true)
        );
        modalManager.open(modal);

        assertFalse(modal.getConfirmButton().isEnabled());

        modal.getInputField().setText("MyPreset");
        assertTrue(modal.getConfirmButton().isEnabled());

        modal.getConfirmButton().onPress();
        assertEquals("MyPreset", submitted.get());
        assertFalse(cancelled.get());
    }

    @Test
    void testTextInputModalValidationRejection() {
        AtomicReference<String> submitted = new AtomicReference<>(null);

        TextInputModal modal = new TextInputModal(
            Text.literal("Enter Preset Name"),
            Text.literal("Must be non-empty"),
            Text.literal("Placeholder"),
            "InitialValid",
            val -> val != null && !val.trim().isEmpty(),
            submitted::set
        );
        modalManager.open(modal);

        assertTrue(modal.getConfirmButton().isEnabled());

        modal.getInputField().setText("   ");
        assertFalse(modal.getConfirmButton().isEnabled());

        modal.getConfirmButton().onPress();
        assertNull(submitted.get());
    }

    @Test
    void testToggleConfirmTurnOff_CancelLeavesOn() {
        AtomicBoolean stateVar = new AtomicBoolean(true);
        ActivityToggle toggle = new ActivityToggle(0, 0, true, stateVar::set);

        toggle.setConfirmTurnOff(
            Text.literal("Turn Off?"),
            Text.literal("Are you sure you want to turn this off?"),
            Text.literal("Turn Off"),
            modalManager
        );

        toggle.toggle();

        assertTrue(modalManager.hasActiveModal());
        BaseModal activeModal = modalManager.getActiveModal();
        assertInstanceOf(ConfirmationModal.class, activeModal);

        ConfirmationModal confirmModal = (ConfirmationModal) activeModal;

        assertTrue(toggle.getState());
        assertTrue(stateVar.get());

        confirmModal.getCancelButton().onPress();

        assertTrue(toggle.getState());
        assertTrue(stateVar.get());
    }

    @Test
    void testToggleConfirmTurnOff_ConfirmTurnsOff() {
        AtomicBoolean stateVar = new AtomicBoolean(true);
        ActivityToggle toggle = new ActivityToggle(0, 0, true, stateVar::set);

        toggle.setConfirmTurnOff(
            Text.literal("Turn Off?"),
            Text.literal("Are you sure?"),
            Text.literal("Turn Off"),
            modalManager
        );

        toggle.toggle();

        assertTrue(modalManager.hasActiveModal());
        ConfirmationModal confirmModal = (ConfirmationModal) modalManager.getActiveModal();

        confirmModal.getConfirmButton().onPress();

        assertFalse(toggle.getState());
        assertFalse(stateVar.get());
    }

    @Test
    void testToggleConfirmTurnOff_TurnOnImmediateWithoutModal() {
        AtomicBoolean stateVar = new AtomicBoolean(false);
        ActivityToggle toggle = new ActivityToggle(0, 0, false, stateVar::set);

        toggle.setConfirmTurnOff(
            Text.literal("Turn Off?"),
            Text.literal("Are you sure?"),
            Text.literal("Turn Off"),
            modalManager
        );

        toggle.toggle();

        assertFalse(modalManager.hasActiveModal());
        assertTrue(toggle.getState());
        assertTrue(stateVar.get());
    }
}

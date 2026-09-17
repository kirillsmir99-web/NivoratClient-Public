package activity.client.gui.modal;

import activity.client.gui.overlay.Overlay;
import activity.client.gui.overlay.OverlayManager;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Unified modal management system for NivoratClient.
 *
 * <p>Integrates directly with {@link OverlayManager} to provide single-point
 * opening, dismissal, confirmation routing, and text input modals across all screens and tabs.
 */
public class ModalManager {

    private final OverlayManager overlayManager;

    public ModalManager(OverlayManager overlayManager) {
        this.overlayManager = overlayManager != null ? overlayManager : new OverlayManager();
    }

    public OverlayManager getOverlayManager() {
        return overlayManager;
    }

    /**
     * Opens a modal dialog on top of the overlay stack.
     *
     * @param modal modal instance to present
     */
    public void open(BaseModal modal) {
        if (modal == null) return;
        modal.setModalManager(this);
        activity.client.gui.sound.SoundManager.playModalOpen();
        this.overlayManager.open(modal);
    }

    /**
     * Dismisses the specified modal dialog.
     */
    public void close(BaseModal modal) {
        if (modal == null) return;
        activity.client.gui.sound.SoundManager.playModalClose();
        this.overlayManager.close(modal);
    }

    /**
     * Closes the active topmost modal dialog.
     */
    public void closeActiveModal() {
        Overlay top = this.overlayManager.getActiveOverlay();
        if (top instanceof BaseModal modal) {
            modal.close();
            this.overlayManager.close(modal);
        }
    }

    /**
     * @return true if a modal dialog is currently open
     */
    public boolean hasActiveModal() {
        Overlay top = this.overlayManager.getActiveOverlay();
        return top instanceof BaseModal && !top.isClosed();
    }

    /**
     * @return active topmost BaseModal, or null if no modal is active
     */
    @Nullable
    public BaseModal getActiveModal() {
        Overlay top = this.overlayManager.getActiveOverlay();
        return top instanceof BaseModal modal && !modal.isClosed() ? modal : null;
    }

    /**
     * Presents a standard confirmation dialog.
     */
    public ConfirmationModal showConfirmation(Text title, Text description, Text confirmText, Text cancelText,
                                              boolean isDanger, Runnable onConfirm, Runnable onCancel) {
        ConfirmationModal modal = new ConfirmationModal(title, description, confirmText, cancelText, isDanger, onConfirm, onCancel);
        this.open(modal);
        return modal;
    }

    /**
     * Presents a standard confirmation dialog with default cancel button text.
     */
    public ConfirmationModal showConfirmation(Text title, Text description, Text confirmText, boolean isDanger, Runnable onConfirm) {
        return this.showConfirmation(title, description, confirmText, Text.translatable("activity.button.cancel"), isDanger, onConfirm, null);
    }

    /**
     * Presents a validated single-line text input modal dialog.
     */
    public TextInputModal showTextInput(Text title, Text description, Text placeholder, String initialValue,
                                        Predicate<String> validator, Consumer<String> onConfirm, Runnable onCancel) {
        TextInputModal modal = new TextInputModal(title, description, placeholder, initialValue, validator, onConfirm, onCancel);
        this.open(modal);
        return modal;
    }

    /**
     * Presents a validated single-line text input modal dialog without explicit cancel callback.
     */
    public TextInputModal showTextInput(Text title, Text description, Text placeholder, String initialValue,
                                        Predicate<String> validator, Consumer<String> onConfirm) {
        return this.showTextInput(title, description, placeholder, initialValue, validator, onConfirm, null);
    }

    /**
     * Presents a modal dialog for resolving a preset with duplicate name during import.
     */
    public DuplicatePresetModal showDuplicatePreset(String presetName, Runnable onOverwrite, Runnable onSaveAsNew, Runnable onCancel) {
        DuplicatePresetModal modal = new DuplicatePresetModal(presetName, onOverwrite, onSaveAsNew, onCancel);
        this.open(modal);
        return modal;
    }

    /**
     * Presents the 5-second continuous hold confirmation modal for factory settings reset.
     */
    public ResetHoldConfirmationModal showResetHoldConfirmation(activity.client.gui.ActivityScreen parentScreen, Runnable onReset) {
        ResetHoldConfirmationModal modal = new ResetHoldConfirmationModal(parentScreen, onReset);
        this.open(modal);
        return modal;
    }
}

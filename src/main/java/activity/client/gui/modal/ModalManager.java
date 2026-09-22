package activity.client.gui.modal;

import activity.client.gui.overlay.Overlay;
import activity.client.gui.overlay.OverlayManager;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class ModalManager {

    private final OverlayManager overlayManager;

    public ModalManager(OverlayManager overlayManager) {
        this.overlayManager = overlayManager != null ? overlayManager : new OverlayManager();
    }

    public OverlayManager getOverlayManager() {
        return overlayManager;
    }

    public void open(BaseModal modal) {
        if (modal == null) return;
        modal.setModalManager(this);
        activity.client.gui.sound.SoundManager.playModalOpen();
        this.overlayManager.open(modal);
    }

    public void close(BaseModal modal) {
        if (modal == null) return;
        activity.client.gui.sound.SoundManager.playModalClose();
        this.overlayManager.close(modal);
    }

    public void closeActiveModal() {
        Overlay top = this.overlayManager.getActiveOverlay();
        if (top instanceof BaseModal modal) {
            modal.close();
            this.overlayManager.close(modal);
        }
    }

    public boolean hasActiveModal() {
        Overlay top = this.overlayManager.getActiveOverlay();
        return top instanceof BaseModal && !top.isClosed();
    }

    @Nullable
    public BaseModal getActiveModal() {
        Overlay top = this.overlayManager.getActiveOverlay();
        return top instanceof BaseModal modal && !modal.isClosed() ? modal : null;
    }

    public ConfirmationModal showConfirmation(Text title, Text description, Text confirmText, Text cancelText,
                                              boolean isDanger, Runnable onConfirm, Runnable onCancel) {
        ConfirmationModal modal = new ConfirmationModal(title, description, confirmText, cancelText, isDanger, onConfirm, onCancel);
        this.open(modal);
        return modal;
    }

    public ConfirmationModal showConfirmation(Text title, Text description, Text confirmText, boolean isDanger, Runnable onConfirm) {
        return this.showConfirmation(title, description, confirmText, Text.translatable("activity.button.cancel"), isDanger, onConfirm, null);
    }

    public TextInputModal showTextInput(Text title, Text description, Text placeholder, String initialValue,
                                        Predicate<String> validator, Consumer<String> onConfirm, Runnable onCancel) {
        TextInputModal modal = new TextInputModal(title, description, placeholder, initialValue, validator, onConfirm, onCancel);
        this.open(modal);
        return modal;
    }

    public TextInputModal showTextInput(Text title, Text description, Text placeholder, String initialValue,
                                        Predicate<String> validator, Consumer<String> onConfirm) {
        return this.showTextInput(title, description, placeholder, initialValue, validator, onConfirm, null);
    }

    public DuplicatePresetModal showDuplicatePreset(String presetName, Runnable onOverwrite, Runnable onSaveAsNew, Runnable onCancel) {
        DuplicatePresetModal modal = new DuplicatePresetModal(presetName, onOverwrite, onSaveAsNew, onCancel);
        this.open(modal);
        return modal;
    }

    public ResetHoldConfirmationModal showResetHoldConfirmation(activity.client.gui.ActivityScreen parentScreen, Runnable onReset) {
        ResetHoldConfirmationModal modal = new ResetHoldConfirmationModal(parentScreen, onReset);
        this.open(modal);
        return modal;
    }
}

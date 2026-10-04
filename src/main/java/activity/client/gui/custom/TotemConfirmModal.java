package activity.client.gui.custom;

import activity.client.gui.custom.api.modules.Module;
import activity.client.gui.custom.api.ui.button.UnifiedButton;
import activity.client.gui.custom.api.ui.modal.UnifiedConfirmModal;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

public final class TotemConfirmModal {
    private static Module pendingModule;

    private TotemConfirmModal() {}

    public static void open(Module module) {
        pendingModule = module;
        boolean ru = activity.client.i18n.LocalizationService.isRussianPreferred();
        String title = ru ? "Предупреждение" : "Warning";
        String line1 = ru ? "Вы уверены, что хотите использовать модификатор?" : "Are you sure you want to use this modifier?";
        String line2 = ru ? "Мод ещё является недоработанным." : "The mod is still a work in progress.";
        String confirmText = ru ? "Включить" : "Enable";
        String cancelText = ru ? "Отмена" : "Cancel";

        UnifiedConfirmModal.show(
                title,
                line1,
                line2,
                0xFFAA28,
                List.of(
                        new UnifiedConfirmModal.ModalButton(cancelText, UnifiedButton.Variant.SECONDARY, TotemConfirmModal::cancel),
                        new UnifiedConfirmModal.ModalButton(confirmText, UnifiedButton.Variant.PRIMARY, TotemConfirmModal::confirm)
                ),
                TotemConfirmModal::cancel
        );
    }

    public static boolean isOpen() {
        return UnifiedConfirmModal.isOpen();
    }

    public static void cancel() {
        pendingModule = null;
    }

    public static void confirm() {
        if (pendingModule != null) {
            pendingModule.toggle();
        }
        pendingModule = null;
    }

    public static boolean keyPressed(int key) {
        return UnifiedConfirmModal.keyPressed(key);
    }

    public static boolean click(float mouseX, float mouseY, int button) {
        return UnifiedConfirmModal.click(mouseX, mouseY, button);
    }

    public static void render(DrawContext context, float alpha) {
        UnifiedConfirmModal.render(context, alpha);
    }
}

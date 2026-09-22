package activity.client.gui.modal;

import activity.client.gui.component.ActivityButton;
import activity.client.gui.font.NivoratFontManager;
import activity.client.gui.theme.ActivityColors;
import activity.client.gui.theme.ActivityMetrics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.List;

public class ConfirmationModal extends BaseModal {

    private final Text description;
    private final Text confirmText;
    private final Text cancelText;
    private final boolean isDanger;
    private final Runnable onConfirm;

    private ActivityButton cancelButton;
    private ActivityButton confirmButton;

    public ConfirmationModal(Text title, Text description, Text confirmText, Text cancelText,
                             boolean isDanger, Runnable onConfirm, Runnable onCancel) {
        super(title, 290, 136);
        this.description = description != null ? description : Text.empty();
        this.confirmText = confirmText != null ? confirmText : Text.translatable("activity.button.confirm");
        this.cancelText = cancelText != null ? cancelText : Text.translatable("activity.button.cancel");
        this.isDanger = isDanger;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;

        this.initButtons();
        this.updateResponsiveBounds();
    }

    public ConfirmationModal(Text title, Text description, Text confirmText, boolean isDanger, Runnable onConfirm) {
        this(title, description, confirmText, Text.translatable("activity.button.cancel"), isDanger, onConfirm, null);
    }

    private void initButtons() {
        this.cancelButton = new ActivityButton(
            0, 0, 80, ActivityMetrics.CONTROL_HEIGHT,
            this.cancelText,
            ActivityButton.Variant.SECONDARY,
            btn -> this.cancel()
        );

        this.confirmButton = new ActivityButton(
            0, 0, 80, ActivityMetrics.CONTROL_HEIGHT,
            this.confirmText,
            this.isDanger ? ActivityButton.Variant.DANGER : ActivityButton.Variant.PRIMARY,
            btn -> {
                if (this.onConfirm != null) {
                    try {
                        this.onConfirm.run();
                    } catch (Exception ignored) {}
                }
                this.close();
            }
        );

        this.addChild(this.cancelButton);
        this.addChild(this.confirmButton);
    }

    @Override
    protected void layoutChildren(int modalX, int modalY, int modalWidth, int modalHeight) {
        if (this.cancelButton == null || this.confirmButton == null) return;

        int btnHeight = ActivityMetrics.CONTROL_HEIGHT;
        int btnY = modalY + modalHeight - btnHeight - 12;
        int spacing = 10;
        int padding = 14;
        int btnWidth = (modalWidth - padding * 2 - spacing) / 2;

        this.cancelButton.setX(modalX + padding);
        this.cancelButton.setY(btnY);
        this.cancelButton.setWidth(btnWidth);
        this.cancelButton.setHeight(btnHeight);

        this.confirmButton.setX(modalX + padding + btnWidth + spacing);
        this.confirmButton.setY(btnY);
        this.confirmButton.setWidth(btnWidth);
        this.confirmButton.setHeight(btnHeight);
    }

    @Override
    protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc != null ? mc.textRenderer : null;
        if (tr == null || this.description == null) return;

        int wrapWidth = this.width - 28;
        List<OrderedText> lines = activity.client.gui.font.UiTextRenderer.wrapLines(tr, this.description, wrapWidth);
        int lineY = this.y + 36;
        int fontH = activity.client.gui.font.UiTextRenderer.getLineHeight();

        for (OrderedText line : lines) {
            activity.client.gui.font.UiTextRenderer.drawOrderedText(context, tr, line, this.x + 14, lineY, ActivityColors.TEXT_SECONDARY, false);
            lineY += fontH + 2;
        }
    }

    public Text getDescription() {
        return description;
    }

    public boolean isDanger() {
        return isDanger;
    }

    public ActivityButton getCancelButton() {
        return cancelButton;
    }

    public ActivityButton getConfirmButton() {
        return confirmButton;
    }
}

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

public class DuplicatePresetModal extends BaseModal {

    private final Text description;
    private final Runnable onOverwrite;
    private final Runnable onSaveAsNew;

    private ActivityButton cancelButton;
    private ActivityButton overwriteButton;
    private ActivityButton saveAsNewButton;

    public DuplicatePresetModal(String presetName, Runnable onOverwrite, Runnable onSaveAsNew, Runnable onCancel) {
        super(Text.translatable("activity.modal.duplicate_preset.title"), 320, 160);
        this.description = Text.translatable("activity.modal.duplicate_preset.desc", presetName);
        this.onOverwrite = onOverwrite;
        this.onSaveAsNew = onSaveAsNew;
        this.onCancel = onCancel;

        this.initButtons();
        this.updateResponsiveBounds();
    }

    private void initButtons() {
        this.cancelButton = new ActivityButton(
            0, 0, 80, ActivityMetrics.CONTROL_HEIGHT,
            Text.translatable("activity.button.cancel"),
            ActivityButton.Variant.SECONDARY,
            btn -> this.cancel()
        );

        this.overwriteButton = new ActivityButton(
            0, 0, 90, ActivityMetrics.CONTROL_HEIGHT,
            Text.translatable("activity.button.overwrite"),
            ActivityButton.Variant.DANGER,
            btn -> {
                if (this.onOverwrite != null) {
                    try {
                        this.onOverwrite.run();
                    } catch (Exception ignored) {}
                }
                this.close();
            }
        );

        this.saveAsNewButton = new ActivityButton(
            0, 0, 120, ActivityMetrics.CONTROL_HEIGHT,
            Text.translatable("activity.button.save_as_new"),
            ActivityButton.Variant.PRIMARY,
            btn -> {
                if (this.onSaveAsNew != null) {
                    try {
                        this.onSaveAsNew.run();
                    } catch (Exception ignored) {}
                }
                this.close();
            }
        );

        this.addChild(this.cancelButton);
        this.addChild(this.saveAsNewButton);
        this.addChild(this.overwriteButton);
    }

    @Override
    protected void layoutChildren(int modalX, int modalY, int modalWidth, int modalHeight) {
        if (this.cancelButton == null || this.overwriteButton == null || this.saveAsNewButton == null) return;

        int padding = 12;
        int btnHeight = ActivityMetrics.CONTROL_HEIGHT;
        int spacing = 6;

        int row1Y = modalY + modalHeight - (btnHeight * 2 + spacing + 10);
        int halfW = (modalWidth - padding * 2 - spacing) / 2;

        this.saveAsNewButton.setX(modalX + padding);
        this.saveAsNewButton.setY(row1Y);
        this.saveAsNewButton.setWidth(halfW);
        this.saveAsNewButton.setHeight(btnHeight);

        this.overwriteButton.setX(modalX + padding + halfW + spacing);
        this.overwriteButton.setY(row1Y);
        this.overwriteButton.setWidth(halfW);
        this.overwriteButton.setHeight(btnHeight);

        int row2Y = modalY + modalHeight - (btnHeight + 10);
        this.cancelButton.setX(modalX + padding);
        this.cancelButton.setY(row2Y);
        this.cancelButton.setWidth(modalWidth - padding * 2);
        this.cancelButton.setHeight(btnHeight);
    }

    @Override
    protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc != null ? mc.textRenderer : null;
        if (tr == null || this.description == null) return;

        int wrapWidth = this.width - 24;
        List<OrderedText> lines = tr.wrapLines(NivoratFontManager.wrap(this.description), wrapWidth);
        int lineY = this.y + 36;
        int fontH = NivoratFontManager.getMetrics().getLineHeight();

        for (OrderedText line : lines) {
            context.drawText(tr, line, this.x + 12, lineY, ActivityColors.TEXT_SECONDARY, false);
            lineY += fontH + 2;
        }
    }

    public ActivityButton getCancelButton() {
        return cancelButton;
    }

    public ActivityButton getOverwriteButton() {
        return overwriteButton;
    }

    public ActivityButton getSaveAsNewButton() {
        return saveAsNewButton;
    }
}

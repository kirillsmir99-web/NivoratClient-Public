package activity.client.gui.modal;

import activity.client.gui.component.ActivityButton;
import activity.client.gui.component.ActivityTextField;
import activity.client.gui.font.NivoratFontManager;
import activity.client.gui.theme.ActivityColors;
import activity.client.gui.theme.ActivityMetrics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class TextInputModal extends BaseModal {

    private final Text description;
    private final Text placeholder;
    private final String initialValue;
    private final Predicate<String> validator;
    private final Consumer<String> onConfirm;

    private ActivityTextField inputField;
    private ActivityButton cancelButton;
    private ActivityButton confirmButton;

    public TextInputModal(Text title, Text description, Text placeholder, String initialValue,
                          Predicate<String> validator, Consumer<String> onConfirm, Runnable onCancel) {
        super(title, 290, 146);
        this.description = description != null ? description : Text.empty();
        this.placeholder = placeholder != null ? placeholder : Text.empty();
        this.initialValue = initialValue != null ? initialValue : "";
        this.validator = validator != null ? validator : str -> true;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;

        this.initControls();
        this.updateResponsiveBounds();
    }

    public TextInputModal(Text title, Text description, Text placeholder, String initialValue,
                          Predicate<String> validator, Consumer<String> onConfirm) {
        this(title, description, placeholder, initialValue, validator, onConfirm, null);
    }

    private void initControls() {
        this.inputField = new ActivityTextField(0, 0, 100, ActivityMetrics.CONTROL_HEIGHT, this.placeholder);
        this.inputField.setText(this.initialValue);
        this.inputField.setOnChanged(this::onTextChanged);

        this.cancelButton = new ActivityButton(
            0, 0, 80, ActivityMetrics.CONTROL_HEIGHT,
            Text.translatable("activity.button.cancel"),
            ActivityButton.Variant.SECONDARY,
            btn -> this.cancel()
        );

        this.confirmButton = new ActivityButton(
            0, 0, 80, ActivityMetrics.CONTROL_HEIGHT,
            Text.translatable("activity.button.save"),
            ActivityButton.Variant.PRIMARY,
            btn -> this.confirm()
        );

        this.addChild(this.inputField);
        this.addChild(this.cancelButton);
        this.addChild(this.confirmButton);

        boolean valid = this.validator.test(this.inputField.getText());
        this.confirmButton.setEnabled(valid);
    }

    private void onTextChanged(String newText) {
        boolean valid = this.validator.test(newText);
        this.confirmButton.setEnabled(valid);
    }

    private void confirm() {
        String value = this.inputField != null ? this.inputField.getText() : "";
        if (this.validator.test(value)) {
            if (this.onConfirm != null) {
                try {
                    this.onConfirm.accept(value);
                } catch (Exception ignored) {}
            }
            this.close();
        }
    }

    @Override
    public void onOpen() {
        super.onOpen();
        if (this.inputField != null) {
            this.inputField.setFocused(true);
            this.focusedChild = this.inputField;
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (this.closing || this.closed) return true;

        int key = input.key();
        if ((key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER)
                && this.confirmButton != null && this.confirmButton.isEnabled()) {
            this.confirm();
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    protected void layoutChildren(int modalX, int modalY, int modalWidth, int modalHeight) {
        if (this.inputField == null || this.cancelButton == null || this.confirmButton == null) return;

        int padding = 14;
        int inputW = modalWidth - padding * 2;
        int inputY = modalY + 54;

        this.inputField.setX(modalX + padding);
        this.inputField.setY(inputY);
        this.inputField.setWidth(inputW);
        this.inputField.setHeight(ActivityMetrics.CONTROL_HEIGHT);

        int btnHeight = ActivityMetrics.CONTROL_HEIGHT;
        int btnY = modalY + modalHeight - btnHeight - 12;
        int spacing = 10;
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
        List<OrderedText> lines = tr.wrapLines(NivoratFontManager.wrap(this.description), wrapWidth);
        int lineY = this.y + 34;

        for (OrderedText line : lines) {
            context.drawText(tr, line, this.x + 14, lineY, ActivityColors.TEXT_SECONDARY, false);
            lineY += NivoratFontManager.getMetrics().getLineHeight() + 2;
        }
    }

    public String getValue() {
        return this.inputField != null ? this.inputField.getText() : "";
    }

    public ActivityTextField getInputField() {
        return inputField;
    }

    public ActivityButton getCancelButton() {
        return cancelButton;
    }

    public ActivityButton getConfirmButton() {
        return confirmButton;
    }
}

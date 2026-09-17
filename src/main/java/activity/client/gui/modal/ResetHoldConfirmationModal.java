package activity.client.gui.modal;

import activity.client.gui.ActivityScreen;
import activity.client.gui.component.ActivityButton;
import activity.client.gui.component.ActivityComponent;
import activity.client.gui.font.UiTextRenderer;
import activity.client.gui.render.ActivityGuiRenderer;
import activity.client.gui.render.ScissorHelper;
import activity.client.gui.sound.SoundManager;
import activity.client.gui.theme.ActivityColors;
import activity.client.gui.theme.ActivityMetrics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

/**
 * 5-second continuous hold modal dialog for Factory Reset confirmation.
 *
 * <p>Protects against accidental resets:
 * <ul>
 *   <li>Requires pressing and continuously holding Left Mouse Button for 5000 ms.</li>
 *   <li>Visual smooth 0% -&gt; 100% horizontal progress fill inside danger button.</li>
 *   <li>Dynamic countdown: "Удерживайте ещё 4.5 с", "Удерживайте ещё 3.2 с", etc.</li>
 *   <li>Early release (&lt; 5000ms), cursor exit, or Escape key resets progress to 0 and cancels action.</li>
 *   <li>At 100% (5000ms): executes reset exactly once, plays confirmation sound, closes modal,
 *       and triggers toast "Настройки восстановлены".</li>
 *   <li>User custom presets are fully preserved.</li>
 * </ul>
 */
public class ResetHoldConfirmationModal extends BaseModal {

    private static final long HOLD_DURATION_MS = 5000L;

    private final Text description;
    private final ActivityScreen parentScreen;
    private final Runnable onReset;

    private ActivityButton cancelButton;
    private HoldButton holdButton;

    public ResetHoldConfirmationModal(ActivityScreen parentScreen, Runnable onReset) {
        super(Text.translatable("activity.modal.reset.title"), 320, 152);
        this.parentScreen = parentScreen;
        this.onReset = onReset;
        this.description = Text.translatable("activity.modal.reset.desc");

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

        this.holdButton = new HoldButton(
            0, 0, 140, ActivityMetrics.CONTROL_HEIGHT,
            HOLD_DURATION_MS,
            this::onHoldCompleted
        );

        this.addChild(this.cancelButton);
        this.addChild(this.holdButton);
    }

    private void onHoldCompleted() {
        if (this.onReset != null) {
            try {
                this.onReset.run();
            } catch (Exception ignored) {}
        }
        SoundManager.playSuccess();
        this.close();
        if (this.parentScreen != null) {
            this.parentScreen.showToast(Text.translatable("activity.toast.settings_restored"), null, null);
        }
    }

    @Override
    protected void layoutChildren(int modalX, int modalY, int modalWidth, int modalHeight) {
        if (this.cancelButton == null || this.holdButton == null) return;

        int btnHeight = ActivityMetrics.CONTROL_HEIGHT;
        int btnY = modalY + modalHeight - btnHeight - 14;
        int padding = 14;
        int spacing = 10;
        int availableW = modalWidth - padding * 2 - spacing;

        int cancelW = Math.max(70, (int) (availableW * 0.38f));
        int holdW = availableW - cancelW;

        this.cancelButton.setX(modalX + padding);
        this.cancelButton.setY(btnY);
        this.cancelButton.setWidth(cancelW);
        this.cancelButton.setHeight(btnHeight);

        this.holdButton.setX(modalX + padding + cancelW + spacing);
        this.holdButton.setY(btnY);
        this.holdButton.setWidth(holdW);
        this.holdButton.setHeight(btnHeight);
    }

    @Override
    protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc != null ? mc.textRenderer : null;
        if (tr == null || this.description == null) return;

        int wrapWidth = this.width - 28;
        List<OrderedText> lines = UiTextRenderer.wrapLines(tr, this.description, wrapWidth);
        int lineY = this.y + 36;
        int lineHeight = UiTextRenderer.getLineHeight();

        for (OrderedText line : lines) {
            UiTextRenderer.drawOrderedText(context, tr, line, this.x + 14, lineY, ActivityColors.TEXT_SECONDARY, false);
            lineY += lineHeight + 2;
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.isEscape()) {
            if (this.holdButton != null) {
                this.holdButton.resetHold();
            }
            this.cancel();
            return true;
        }
        return super.keyPressed(input);
    }

    public HoldButton getHoldButton() {
        return holdButton;
    }

    /**
     * Dedicated button component tracking 5000ms hold state and rendering danger progress fill.
     */
    public static class HoldButton extends ActivityComponent {

        private final long holdDurationMs;
        private final Runnable onComplete;
        private final Text defaultText;

        private boolean holding = false;
        private long holdStartTime = 0L;
        private float holdProgress = 0.0f;
        private boolean executed = false;

        public HoldButton(int x, int y, int width, int height, long holdDurationMs, Runnable onComplete) {
            super(x, y, width, height);
            this.holdDurationMs = holdDurationMs;
            this.onComplete = onComplete;
            this.defaultText = Text.translatable("activity.modal.reset.hold_btn");
        }

        public void resetHold() {
            this.holding = false;
            this.holdProgress = 0.0f;
            this.holdStartTime = 0L;
        }

        public boolean isHolding() {
            return holding;
        }

        public float getHoldProgress() {
            return holdProgress;
        }

        public boolean isExecuted() {
            return executed;
        }

        @Override
        public boolean mouseClicked(Click click, boolean doubled) {
            if (this.enabled && this.visible && click.button() == 0 && this.isMouseOver(click.x(), click.y())) {
                if (!executed) {
                    this.holding = true;
                    this.holdStartTime = System.currentTimeMillis();
                    this.holdProgress = 0.0f;
                    SoundManager.playClick();
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseReleased(Click click) {
            if (this.holding) {
                resetHold();
                return true;
            }
            return false;
        }

        @Override
        protected void renderComponent(DrawContext context, int mouseX, int mouseY, float delta) {
            MinecraftClient mc = MinecraftClient.getInstance();

            // Verify continuous hold: LMB pressed and cursor inside button hitbox
            if (this.holding && !this.executed) {
                boolean lmbDown = true;
                if (mc != null && mc.getWindow() != null) {
                    long handle = mc.getWindow().getHandle();
                    lmbDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
                }
                boolean hovered = this.isMouseOver(mouseX, mouseY);

                if (!lmbDown || !hovered) {
                    resetHold();
                } else {
                    long elapsed = System.currentTimeMillis() - this.holdStartTime;
                    this.holdProgress = Math.clamp((float) elapsed / this.holdDurationMs, 0.0f, 1.0f);

                    if (elapsed >= this.holdDurationMs) {
                        this.executed = true;
                        this.holding = false;
                        if (this.onComplete != null) {
                            this.onComplete.run();
                        }
                        return;
                    }
                }
            }

            int bgColor = ActivityColors.BUTTON_DANGER_BG;
            int borderColor = this.holding ? ActivityColors.DANGER : ActivityColors.BUTTON_DANGER_BORDER;

            // Background & border
            ActivityGuiRenderer.drawPanel(context, this.x, this.y, this.width, this.height, bgColor, borderColor);

            // Progress bar fill (0% -> 100% left to right)
            if (this.holdProgress > 0.001f) {
                int fillW = Math.max(1, Math.round((this.width - 2) * this.holdProgress));
                int fillColor = ActivityColors.withAlpha(0xE03535, Math.round(160 + 95 * this.holdProgress));
                ActivityGuiRenderer.fill(context, this.x + 1, this.y + 1, fillW, this.height - 2, fillColor);

                // Leading glow edge line
                int glowColor = ActivityColors.withAlpha(0xFFFFFF, Math.round(180 * this.holdProgress));
                ActivityGuiRenderer.fill(context, this.x + fillW, this.y + 1, 1, this.height - 2, glowColor);
            }

            // Button label with dynamic countdown
            TextRenderer tr = mc != null ? mc.textRenderer : null;
            if (tr != null) {
                Text displayLabel;
                if (this.holding && this.holdProgress < 1.0f) {
                    long remainingMs = Math.max(0L, this.holdDurationMs - (System.currentTimeMillis() - this.holdStartTime));
                    float remainingSec = remainingMs / 1000.0f;
                    displayLabel = Text.literal(String.format(Locale.ROOT, "Удерживайте ещё %.1f с", remainingSec));
                } else {
                    displayLabel = this.defaultText;
                }

                int fontH = UiTextRenderer.getFontHeight(tr);
                int textY = this.y + (this.height - fontH) / 2;
                int textColor = this.holding ? 0xFFFFFFFF : ActivityColors.interpolateColor(ActivityColors.DANGER, 0xFFFF8080, this.hovered ? 1.0f : 0.0f);

                ScissorHelper.pushScissor(context, this.x + 2, this.y + 1, Math.max(1, this.width - 4), Math.max(1, this.height - 2));
                try {
                    UiTextRenderer.drawCenteredTextWithShadow(context, tr, displayLabel, this.x + this.width / 2, textY, textColor);
                } finally {
                    ScissorHelper.popScissor(context);
                }
            }
        }
    }
}

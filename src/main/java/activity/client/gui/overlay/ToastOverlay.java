package activity.client.gui.overlay;

import activity.client.gui.animation.AnimationClock;
import activity.client.gui.component.ActivityButton;
import activity.client.gui.icon.ActivityIcon;
import activity.client.gui.icon.ActivityIconRenderer;
import activity.client.gui.render.ActivityGuiRenderer;
import activity.client.gui.sound.SoundManager;
import activity.client.gui.theme.ActivityColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Non-intrusive floating toast notification component adhering to the NivoratClient acrylic design system.
 * Renders on top (Z=7 Layer) without blocking clicks outside its bounds.
 * Supports informative icon, message, optional interactive action button (e.g. copy link), and auto-dismiss timer.
 */
public class ToastOverlay implements Overlay {

    private final Text message;
    private final Text actionLabel;
    private final Runnable onAction;
    private String url;

    private final ActivityButton actionButton;
    private final ActivityButton closeButton;

    private int x;
    private int y;
    private int width = 320;
    private int height = 28;

    private float animProgress = 0.0f;
    private boolean closing = false;
    private boolean closed = false;
    private float remainingTime = 6.0f;
    private boolean hovered = false;

    public ToastOverlay(Text message, @Nullable Text actionLabel, @Nullable Runnable onAction) {
        this.message = message != null ? message : Text.empty();
        this.actionLabel = actionLabel;
        this.onAction = onAction;

        if (actionLabel != null && onAction != null) {
            this.actionButton = new ActivityButton(
                0, 0, 110, 18,
                ActivityIcon.COPY,
                actionLabel,
                ActivityButton.Variant.PRIMARY,
                btn -> performAction()
            );
        } else {
            this.actionButton = null;
        }

        this.closeButton = new ActivityButton(
            0, 0, 16, 18,
            ActivityIcon.CLOSE,
            null,
            ActivityButton.Variant.SECONDARY,
            btn -> close()
        );

        this.animProgress = AnimationClock.isAnimationsEnabled() ? 0.0f : 1.0f;
        updateLayout();
    }

    /**
     * Creates a toast notification for browser link opening failure with a copy-to-clipboard action.
     *
     * @param url the link that failed to open
     * @return initialized ToastOverlay
     */
    public static ToastOverlay forUrlError(String url) {
        Text message = Text.translatable("activity.toast.open_url_failed");
        Text action = Text.translatable("activity.toast.copy_url");

        AtomicReference<ToastOverlay> toastRef = new AtomicReference<>();
        ToastOverlay toast = new ToastOverlay(message, action, () -> {
            activity.client.gui.tab.AboutTab.copyToClipboard(url);
            SoundManager.playPresetSave();
            ToastOverlay t = toastRef.get();
            if (t != null) {
                t.setActionSuccess(Text.translatable("activity.toast.url_copied"));
            }
        });

        toastRef.set(toast);
        toast.setUrl(url);
        return toast;
    }

    /**
     * Updates action button state upon successful execution (e.g. copied to clipboard).
     */
    public void setActionSuccess(Text successLabel) {
        if (this.actionButton != null) {
            this.actionButton.setMessage(successLabel);
            this.actionButton.setIcon(ActivityIcon.CHECK);
            this.actionButton.setEnabled(false);
        }
        this.remainingTime = Math.min(this.remainingTime, 1.8f);
    }

    public void performAction() {
        if (this.onAction != null) {
            try {
                this.onAction.run();
            } catch (Exception ignored) {}
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Text getMessage() {
        return message;
    }

    @Nullable
    public ActivityButton getActionButton() {
        return actionButton;
    }

    public ActivityButton getCloseButton() {
        return closeButton;
    }

    public float getRemainingTime() {
        return remainingTime;
    }

    public void setRemainingTime(float remainingTime) {
        this.remainingTime = remainingTime;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public boolean blocksBackgroundClicks() {
        return false;
    }

    @Override
    public boolean shouldCloseOnClickOutside() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !this.closing && !this.closed;
    }

    public void updateLayout() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int screenW = mc != null && mc.getWindow() != null ? mc.getWindow().getScaledWidth() : 400;
        TextRenderer tr = mc != null ? mc.textRenderer : null;
        int msgW = tr != null && this.message != null ? tr.getWidth(this.message) : 140;

        int pad = 8;
        int iconW = 12;
        int gap = 6;
        int closeBtnW = 16;
        int actionBtnW = this.actionButton != null ? Math.max(90, (tr != null && this.actionButton.getMessage() != null ? tr.getWidth(this.actionButton.getMessage()) + 26 : 100)) : 0;

        int desiredW = pad + iconW + gap + msgW + gap + (actionBtnW > 0 ? actionBtnW + gap : 0) + closeBtnW + pad;
        this.width = Math.clamp(desiredW, 240, Math.max(240, screenW - 24));
        this.height = 28;

        this.x = (screenW - this.width) / 2;
        int targetY = 14;
        int startY = -this.height - 8;
        float eased = AnimationClock.smoothStep(this.animProgress);
        this.y = (int) (startY + (targetY - startY) * eased);

        // Position children
        int curX = this.x + this.width - pad - closeBtnW;
        int btnY = this.y + (this.height - 18) / 2;
        this.closeButton.setX(curX);
        this.closeButton.setY(btnY);
        this.closeButton.setWidth(closeBtnW);
        this.closeButton.setHeight(18);

        if (this.actionButton != null) {
            curX -= gap + actionBtnW;
            this.actionButton.setX(curX);
            this.actionButton.setY(btnY);
            this.actionButton.setWidth(actionBtnW);
            this.actionButton.setHeight(18);
        }
    }

    @Override
    public void onOpen() {
        this.closing = false;
        this.closed = false;
        this.animProgress = AnimationClock.isAnimationsEnabled() ? 0.0f : 1.0f;
        this.remainingTime = 6.0f;
        updateLayout();
    }

    @Override
    public void close() {
        if (!AnimationClock.isAnimationsEnabled()) {
            this.closed = true;
            this.closing = false;
            return;
        }
        if (!this.closing) {
            this.closing = true;
        }
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    public boolean isClosing() {
        return this.closing;
    }

    @Override
    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX <= this.x + this.width &&
               mouseY >= this.y && mouseY <= this.y + this.height;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean animEnabled = AnimationClock.isAnimationsEnabled();
        if (animEnabled) {
            float target = this.closing ? 0.0f : 1.0f;
            float duration = this.closing ? AnimationClock.DURATION_SCREEN_CLOSE : AnimationClock.DURATION_SCREEN_OPEN;
            this.animProgress = AnimationClock.approach(this.animProgress, target, duration);
            if (this.closing && this.animProgress <= 0.01f) {
                this.closed = true;
                return;
            }
        } else {
            this.animProgress = this.closing ? 0.0f : 1.0f;
            if (this.closing) {
                this.closed = true;
                return;
            }
        }

        // Auto-dismiss countdown (paused when mouse is hovering the toast)
        this.hovered = contains(mouseX, mouseY);
        if (!this.hovered && !this.closing) {
            this.remainingTime -= AnimationClock.getDeltaTime();
            if (this.remainingTime <= 0.0f) {
                this.close();
            }
        }

        updateLayout();

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc != null ? mc.textRenderer : null;
        int pad = 8;
        int iconW = 12;
        int gap = 6;

        float alphaFactor = Math.clamp(this.animProgress, 0.0f, 1.0f);
        int panelBg = ActivityColors.withAlpha(0x12141A, Math.round(242 * alphaFactor));
        int borderColor = ActivityColors.withAlpha(ActivityColors.WARNING, Math.round(225 * alphaFactor));

        // Acrylic Glass Panel
        ActivityGuiRenderer.drawPanel(context, this.x, this.y, this.width, this.height, panelBg, borderColor, true);

        // Specular top highlight
        int glintColor = ActivityColors.withAlpha(0xFFFFFF, Math.round(40 * alphaFactor));
        ActivityGuiRenderer.fill(context, this.x + 1, this.y + 1, this.width - 2, 1, glintColor);

        // Warning Icon
        int iconColor = ActivityColors.withAlpha(ActivityColors.WARNING, Math.round(255 * alphaFactor));
        ActivityIconRenderer.drawCenteredSized(context, ActivityIcon.WARNING, this.x + pad, this.y, iconW + 2, this.height, 10, iconColor);

        // Message Text
        if (tr != null && this.message != null) {
            int textX = this.x + pad + iconW + gap;
            int textY = this.y + (this.height - tr.fontHeight) / 2;
            int textColor = ActivityColors.withAlpha(ActivityColors.TEXT_PRIMARY, Math.round(255 * alphaFactor));
            int availTextW = (this.actionButton != null ? this.actionButton.getX() : this.closeButton.getX()) - textX - gap;

            if (availTextW > 20 && tr.getWidth(this.message) > availTextW) {
                int ellW = tr.getWidth("…");
                String trimmed = tr.trimToWidth(this.message.getString(), Math.max(0, availTextW - ellW)) + "…";
                context.drawText(tr, Text.literal(trimmed), textX, textY, textColor, false);
            } else {
                context.drawText(tr, this.message, textX, textY, textColor, false);
            }
        }

        // Render Action Button
        if (this.actionButton != null) {
            this.actionButton.render(context, mouseX, mouseY, delta);
        }

        // Render Close Button
        this.closeButton.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (this.closed) return false;
        if (this.closing) return true;

        if (this.closeButton.isMouseOver(click.x(), click.y())) {
            this.closeButton.mouseClicked(click, doubled);
            return true;
        }

        if (this.actionButton != null && this.actionButton.isMouseOver(click.x(), click.y())) {
            this.actionButton.mouseClicked(click, doubled);
            return true;
        }

        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (this.closed) return false;
        if (this.closing) return true;
        if (this.closeButton.mouseReleased(click)) return true;
        if (this.actionButton != null && this.actionButton.mouseReleased(click)) return true;
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (this.closed || this.closing) return false;
        if (input.isEscape() && shouldCloseOnEsc()) {
            this.close();
            return true;
        }
        return false;
    }
}

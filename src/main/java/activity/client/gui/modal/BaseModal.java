package activity.client.gui.modal;

import activity.client.gui.animation.AnimationClock;
import activity.client.gui.component.ActivityComponent;
import activity.client.gui.font.NivoratFontManager;
import activity.client.gui.overlay.Overlay;
import activity.client.gui.render.ActivityGuiRenderer;
import activity.client.gui.sound.SoundManager;
import activity.client.gui.theme.ActivityColors;
import activity.client.gui.theme.ActivityMetrics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseModal implements Overlay {

    protected final Text title;
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected final int preferredWidth;
    protected final int preferredHeight;

    protected float animProgress = 0.0f;
    protected boolean closing = false;
    protected boolean closed = false;

    protected ModalManager modalManager;
    protected Runnable onCancel;

    protected final List<ActivityComponent> children = new ArrayList<>();
    protected ActivityComponent focusedChild = null;

    public BaseModal(Text title, int preferredWidth, int preferredHeight) {
        this.title = title != null ? title : Text.empty();
        this.preferredWidth = preferredWidth;
        this.preferredHeight = preferredHeight;
        this.updateResponsiveBounds();
    }

    public void setModalManager(ModalManager modalManager) {
        this.modalManager = modalManager;
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    public void updateResponsiveBounds() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int screenW = mc != null && mc.getWindow() != null ? mc.getWindow().getScaledWidth() : 400;
        int screenH = mc != null && mc.getWindow() != null ? mc.getWindow().getScaledHeight() : 300;

        this.width = Math.clamp(this.preferredWidth, 180, Math.max(180, screenW - 24));
        this.height = Math.clamp(this.preferredHeight, 100, Math.max(100, screenH - 24));
        this.x = (screenW - this.width) / 2;
        this.y = (screenH - this.height) / 2;

        this.layoutChildren(this.x, this.y, this.width, this.height);
    }

    protected abstract void layoutChildren(int modalX, int modalY, int modalWidth, int modalHeight);

    protected <T extends ActivityComponent> T addChild(T child) {
        if (child != null && !this.children.contains(child)) {
            this.children.add(child);
        }
        return child;
    }

    @Override
    public void onOpen() {
        this.closing = false;
        this.closed = false;
        this.animProgress = AnimationClock.isAnimationsEnabled() ? 0.0f : 1.0f;
        this.updateResponsiveBounds();
        SoundManager.playClick();
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

    public void cancel() {
        if (this.onCancel != null) {
            try {
                this.onCancel.run();
            } catch (Exception ignored) {}
        }
        this.close();
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

        return true;
    }

    @Override
    public boolean shouldCloseOnClickOutside() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int screenW = mc != null && mc.getWindow() != null ? mc.getWindow().getScaledWidth() : 400;
        int screenH = mc != null && mc.getWindow() != null ? mc.getWindow().getScaledHeight() : 300;

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

        float eased = AnimationClock.smoothStep(this.animProgress);

        int scrimAlpha = Math.round(150 * eased);
        int scrimColor = (scrimAlpha << 24) | 0x000000;
        ActivityGuiRenderer.fill(context, 0, 0, screenW, screenH, scrimColor);

        float scale = 0.90f + 0.10f * eased;
        float centerX = this.x + this.width / 2.0f;
        float centerY = this.y + this.height / 2.0f;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(centerX, centerY);
        context.getMatrices().scale(scale, scale);
        context.getMatrices().translate(-centerX, -centerY);

        try {

            int modalBg = ActivityColors.withAlpha(0x12141A, Math.round(235 * eased));
            int modalBorder = ActivityColors.withAlpha(0x38FFFFFF, Math.round(255 * eased));
            ActivityGuiRenderer.fill(context, this.x, this.y, this.width, this.height, modalBg);
            ActivityGuiRenderer.drawBorder(context, this.x, this.y, this.width, this.height, modalBorder);

            int topGlint = ActivityColors.withAlpha(0xFFFFFF, Math.round(45 * eased));
            ActivityGuiRenderer.fill(context, this.x + 1, this.y + 1, this.width - 2, 1, topGlint);

            TextRenderer tr = mc != null ? mc.textRenderer : null;
            if (tr != null && this.title != null) {
                activity.client.gui.font.UiTextRenderer.drawText(context, tr, this.title, this.x + 14, this.y + 12, ActivityColors.TEXT_PRIMARY, false);

                int sepColor = ActivityColors.withAlpha(0x28FFFFFF, Math.round(255 * eased));
                ActivityGuiRenderer.fill(context, this.x + 10, this.y + 28, this.width - 20, 1, sepColor);
            }

            renderContent(context, mouseX, mouseY, delta);

            for (ActivityComponent child : this.children) {
                if (child.isVisible()) {
                    child.render(context, mouseX, mouseY, delta);
                }
            }
        } finally {
            context.getMatrices().popMatrix();
        }
    }

    protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {}

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (this.closing || this.closed) return true;

        double mx = click.x();
        double my = click.y();

        if (mx >= this.x && mx <= this.x + this.width && my >= this.y && my <= this.y + this.height) {
            for (int i = this.children.size() - 1; i >= 0; i--) {
                ActivityComponent child = this.children.get(i);
                if (child.isVisible() && child.isEnabled() && child.isMouseOver(mx, my)) {
                    if (this.focusedChild != null && this.focusedChild != child) {
                        this.focusedChild.setFocused(false);
                    }
                    this.focusedChild = child;
                    child.setFocused(true);
                    return child.mouseClicked(click, doubled);
                }
            }
            if (this.focusedChild != null) {
                this.focusedChild.setFocused(false);
                this.focusedChild = null;
            }
            return true;
        }

        if (this.shouldCloseOnClickOutside()) {
            this.cancel();
        }
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (this.closing || this.closed) return true;
        for (int i = this.children.size() - 1; i >= 0; i--) {
            ActivityComponent child = this.children.get(i);
            if (child.isVisible() && child.mouseReleased(click)) {
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (this.closing || this.closed) return true;
        if (this.focusedChild != null && this.focusedChild.mouseDragged(click, deltaX, deltaY)) {
            return true;
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (this.closing || this.closed) return true;

        if (input.isEscape() && this.shouldCloseOnEsc()) {
            this.cancel();
            return true;
        }

        if (this.focusedChild != null && this.focusedChild.keyPressed(input)) {
            return true;
        }

        for (int i = this.children.size() - 1; i >= 0; i--) {
            ActivityComponent child = this.children.get(i);
            if (child != this.focusedChild && child.isVisible() && child.isEnabled() && child.keyPressed(input)) {
                return true;
            }
        }

        return true;
    }

    @Override
    public boolean keyReleased(KeyInput input) {
        if (this.closing || this.closed) return true;
        if (this.focusedChild != null && this.focusedChild.keyReleased(input)) {
            return true;
        }
        return true;
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (this.closing || this.closed) return true;
        if (this.focusedChild != null && this.focusedChild.charTyped(input)) {
            return true;
        }
        return true;
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
}

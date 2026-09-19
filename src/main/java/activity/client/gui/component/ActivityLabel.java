package activity.client.gui.component;

import activity.client.gui.font.FontManager;
import activity.client.gui.theme.ActivityColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Text label component supporting custom colors, shadows, alignment, and dynamic font updates.
 */
public class ActivityLabel extends ActivityComponent {

    private Text text;
    private int color = ActivityColors.TEXT_PRIMARY;
    private boolean centered = false;
    private boolean shadow = true;
    private int maxWidth = -1;
    private Text cachedDisplayText = null;
    private int cachedDisplayWidth = -1;
    private int lastCalculatedMaxWidth = -2;
    private boolean wordWrap = false;
    private java.util.List<net.minecraft.text.OrderedText> cachedWrappedLines = null;

    public boolean isWordWrap() {
        return wordWrap;
    }

    public ActivityLabel setWordWrap(boolean wordWrap) {
        this.wordWrap = wordWrap;
        this.cachedDisplayText = null;
        this.cachedWrappedLines = null;
        return this;
    }

    public Text getTooltip() {
        return tooltip;
    }

    @Override
    public ActivityLabel setTooltip(Text tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public ActivityLabel setMaxWidth(int maxWidth) {
        if (this.maxWidth != maxWidth) {
            this.maxWidth = maxWidth;
            this.cachedDisplayText = null;
            this.cachedDisplayWidth = -1;
            this.cachedWrappedLines = null;
        }
        return this;
    }

    public ActivityLabel(int x, int y, Text text) {
        super(x, y, 0, 9);
        this.text = text;
        updateDimensions();
    }

    public ActivityLabel(int x, int y, int width, int height, Text text) {
        super(x, y, width, height);
        this.text = text;
    }

    public ActivityLabel(int x, int y, Text text, int color) {
        this(x, y, text);
        this.color = color;
    }

    public Text getText() {
        return text;
    }

    public void setText(Text text) {
        this.text = text;
        this.cachedDisplayText = null;
        this.cachedDisplayWidth = -1;
        this.cachedWrappedLines = null;
        updateDimensions();
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public boolean isCentered() {
        return centered;
    }

    public void setCentered(boolean centered) {
        this.centered = centered;
    }

    public boolean hasShadow() {
        return shadow;
    }

    public void setShadow(boolean shadow) {
        this.shadow = shadow;
    }

    private void updateDimensions() {
        if (this.width == 0 && this.text != null) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.textRenderer != null) {
                TextRenderer tr = mc.textRenderer;
                this.width = activity.client.gui.font.UiTextRenderer.getWidth(tr, this.text);
                this.height = activity.client.gui.font.UiTextRenderer.getFontHeight(tr);
            } else {
                float scale = activity.client.gui.font.UiTextRenderer.getScaleFactor();
                this.width = Math.max(10, Math.round(this.text.getString().length() * 6 * scale));
                this.height = Math.max(7, Math.round(9 * scale));
            }
        }
    }

    @Override
    public void onFontChanged() {
        this.width = 0;
        this.cachedDisplayText = null;
        this.cachedDisplayWidth = -1;
        this.cachedWrappedLines = null;
        updateDimensions();
    }

    @Override
    protected void renderComponent(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.text == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client != null ? client.textRenderer : null;
        if (tr == null) return;
        int renderColor = this.enabled ? this.color : ActivityColors.TEXT_DISABLED;
        int fontH = activity.client.gui.font.UiTextRenderer.getFontHeight(tr);

        if (this.wordWrap && this.maxWidth > 0) {
            if (this.cachedWrappedLines == null || this.lastCalculatedMaxWidth != this.maxWidth) {
                this.cachedWrappedLines = activity.client.gui.font.UiTextRenderer.wrapLines(tr, this.text, this.maxWidth);
                this.lastCalculatedMaxWidth = this.maxWidth;
                int lineH = fontH + 2;
                this.height = Math.max(fontH, this.cachedWrappedLines.size() * lineH);
            }
            int lineH = fontH + 2;
            int curY = this.y;
            for (net.minecraft.text.OrderedText line : this.cachedWrappedLines) {
                activity.client.gui.font.UiTextRenderer.drawOrderedText(context, tr, line, this.x, curY, renderColor, this.shadow);
                curY += lineH;
            }
            return;
        }

        int textY = this.y + (this.height - fontH) / 2;

        if (this.cachedDisplayText == null || this.lastCalculatedMaxWidth != this.maxWidth) {
            int currentWidth = activity.client.gui.font.UiTextRenderer.getWidth(tr, this.text);
            if (this.maxWidth > 0 && currentWidth > this.maxWidth) {
                int ellW = activity.client.gui.font.UiTextRenderer.getWidth(tr, "…");
                int targetW = Math.max(0, this.maxWidth - ellW);
                String trimmed = activity.client.gui.font.UiTextRenderer.trimToWidth(tr, this.text.getString(), targetW) + "…";
                if (activity.client.gui.font.UiTextRenderer.getWidth(tr, trimmed) > this.maxWidth) {
                    trimmed = activity.client.gui.font.UiTextRenderer.trimToWidth(tr, trimmed, Math.max(0, this.maxWidth));
                }
                this.cachedDisplayText = Text.literal(trimmed);
            } else {
                this.cachedDisplayText = this.text;
            }
            this.cachedDisplayWidth = activity.client.gui.font.UiTextRenderer.getWidth(tr, this.cachedDisplayText);
            this.lastCalculatedMaxWidth = this.maxWidth;
        }

        Text display = this.cachedDisplayText;

        if (this.centered) {
            int centerX = this.x + this.width / 2;
            activity.client.gui.font.UiTextRenderer.drawCenteredText(context, tr, display, centerX, textY, renderColor, this.shadow);
        } else {
            activity.client.gui.font.UiTextRenderer.drawText(context, tr, display, this.x, textY, renderColor, this.shadow);
        }
    }
}

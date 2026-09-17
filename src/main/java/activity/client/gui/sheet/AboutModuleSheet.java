package activity.client.gui.sheet;

import activity.client.gui.ActivityScreen;
import activity.client.gui.animation.AnimationClock;
import activity.client.gui.font.FontManager;
import activity.client.gui.icon.ActivityIcon;
import activity.client.gui.icon.ActivityIconRenderer;
import activity.client.gui.overlay.Overlay;
import activity.client.gui.render.ActivityGuiRenderer;
import activity.client.gui.sound.SoundManager;
import activity.client.gui.theme.ActivityColors;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.ModuleRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.List;

/**
 * Animated right-side sheet displaying comprehensive module information.
 *
 * <p>Content includes:
 * <ul>
 *   <li>Title & Icon</li>
 *   <li>Status badge (Включен / Выключен)</li>
 *   <li>Author (Nivorat)</li>
 *   <li>Version (1.0.0)</li>
 *   <li>Last updated date (2026-09-16)</li>
 *   <li>Keybind display</li>
 *   <li>Multi-line wrapped description</li>
 *   <li>Channel notice (@virionDEV)</li>
 *   <li>Telegram action button (opens t.me/virionDEV)</li>
 *   <li>Open Settings action button (navigates to module in tab)</li>
 * </ul>
 */
public class AboutModuleSheet implements Overlay {

    public static final int SHEET_WIDTH = 260;

    private final ActivityScreen screen;
    private final String moduleId;
    private final ModuleMetadata metadata;

    private boolean closed = false;
    private float slideProgress = 0.0f;

    // Hover states
    private boolean closeHovered = false;
    private boolean telegramHovered = false;
    private boolean settingsHovered = false;

    private List<OrderedText> cachedDescriptionLines = null;
    private int lastDescInnerW = -1;
    private Text cachedNoticeText = null;
    private int lastNoticeInnerW = -1;

    public void invalidateTextCache() {
        this.cachedDescriptionLines = null;
        this.lastDescInnerW = -1;
        this.cachedNoticeText = null;
        this.lastNoticeInnerW = -1;
    }

    public AboutModuleSheet(ActivityScreen screen, String moduleId) {
        this.screen = screen;
        this.moduleId = moduleId;
        ModuleMetadata meta = ModuleRegistry.getMetadata(moduleId);
        if (meta == null) {
            meta = ModuleMetadata.builder(moduleId)
                .displayName(Text.literal(moduleId))
                .description(Text.empty())
                .build();
        }
        this.metadata = meta;
        FontManager.addListener(this::invalidateTextCache);
    }

    public String getModuleId() {
        return moduleId;
    }

    public ModuleMetadata getMetadata() {
        return metadata;
    }

    public float getSlideProgress() {
        return slideProgress;
    }

    private int getScreenWidth() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc != null && mc.getWindow() != null ? mc.getWindow().getScaledWidth() : 400;
    }

    private int getScreenHeight() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc != null && mc.getWindow() != null ? mc.getWindow().getScaledHeight() : 300;
    }

    public int getEffectiveWidth() {
        int screenW = getScreenWidth();
        return Math.min(SHEET_WIDTH, Math.max(160, screenW - 30));
    }

    public int getRenderX() {
        int screenW = getScreenWidth();
        int width = getEffectiveWidth();
        float eased = AnimationClock.easeOutCubic(this.slideProgress);
        return screenW - (int) Math.round(width * eased);
    }

    @Override
    public boolean contains(double mouseX, double mouseY) {
        int renderX = getRenderX();
        int screenW = getScreenWidth();
        int screenH = getScreenHeight();
        return mouseX >= renderX && mouseX <= screenW && mouseY >= 0 && mouseY <= screenH;
    }

    @Override
    public boolean shouldCloseOnClickOutside() {
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void close() {
        this.closed = true;
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.closed) return;

        float dt = AnimationClock.getDeltaTime();
        float target = 1.0f;
        if (!AnimationClock.isAnimationsEnabled()) {
            this.slideProgress = target;
        } else {
            this.slideProgress = AnimationClock.approach(this.slideProgress, target, 0.18f, dt);
        }

        float eased = AnimationClock.easeOutCubic(this.slideProgress);
        if (eased <= 0.01f) return;

        int screenW = getScreenWidth();
        int screenH = getScreenHeight();
        int width = getEffectiveWidth();
        int renderX = screenW - (int) Math.round(width * eased);

        // 1. Semi-transparent backdrop to the left
        int backdropColor = ActivityColors.scaleAlpha(0x66000000, eased);
        ActivityGuiRenderer.fill(context, 0, 0, renderX, screenH, backdropColor);

        // 2. Sheet Panel Body
        int panelBg = ActivityColors.scaleAlpha(ActivityColors.WINDOW_BACKGROUND, eased);
        int borderColor = ActivityColors.scaleAlpha(ActivityColors.BORDER_LIGHT, eased);
        ActivityGuiRenderer.fill(context, renderX, 0, width, screenH, panelBg);

        // 1px left border
        ActivityGuiRenderer.fill(context, renderX, 0, 1, screenH, borderColor);

        // Glass highlight along left rim
        if (eased > 0.1f) {
            int highlightColor = ActivityColors.scaleAlpha(ActivityColors.GLASS_HIGHLIGHT_PRIMARY, eased);
            ActivityGuiRenderer.fill(context, renderX + 1, 0, 1, screenH, highlightColor);
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer textRenderer = mc != null ? mc.textRenderer : null;

        // 3. Header Bar (28px height)
        int headerH = 28;
        int headerBg = ActivityColors.scaleAlpha(ActivityColors.HEADER_BACKGROUND, eased);
        ActivityGuiRenderer.fill(context, renderX + 1, 0, width - 1, headerH, headerBg);
        ActivityGuiRenderer.drawHorizontalLine(context, renderX, headerH, width, ActivityColors.scaleAlpha(ActivityColors.BORDER_DIVIDER, eased));

        // Header Title
        if (textRenderer != null) {
            Text headerTitle = Text.translatable("activity.sheet.about_title");
            Text wrappedHeader = FontManager.wrap(headerTitle);
            int titleY = (headerH - textRenderer.fontHeight) / 2;
            context.drawTextWithShadow(textRenderer, wrappedHeader, renderX + 12, titleY, ActivityColors.scaleAlpha(ActivityColors.TEXT_MUTED, eased));
        }

        // Close Button (18x18)
        int closeX = renderX + width - 22;
        int closeY = (headerH - 14) / 2;
        this.closeHovered = mouseX >= closeX - 2 && mouseX < closeX + 16 && mouseY >= closeY - 2 && mouseY < closeY + 16;
        int closeColor = this.closeHovered ? ActivityColors.TEXT_PRIMARY : ActivityColors.TEXT_MUTED;
        closeColor = ActivityColors.scaleAlpha(closeColor, eased);
        if (this.closeHovered) {
            ActivityGuiRenderer.fill(context, closeX - 2, closeY - 2, 16, 16, ActivityColors.scaleAlpha(ActivityColors.ITEM_HOVER_BG, eased));
        }
        ActivityIconRenderer.draw(context, ActivityIcon.CLOSE, closeX, closeY, closeColor);

        // 4. Content Area
        int curY = headerH + 12;
        int innerX = renderX + 12;
        int innerW = width - 24;

        // Module Icon + Display Name
        ActivityIcon icon = this.metadata.getIcon();
        if (icon != null) {
            int iconY = curY + 1;
            ActivityIconRenderer.draw(context, icon, innerX, iconY, ActivityColors.scaleAlpha(ActivityColors.TEXT_ACCENT, eased));
        }

        if (textRenderer != null) {
            int titleX = innerX + (icon != null ? icon.getWidth() + 8 : 0);
            int titleMaxW = Math.max(10, innerX + innerW - titleX);
            Text modTitle = FontManager.wrap(this.metadata.getDisplayName());
            String trimmedTitle = textRenderer.trimToWidth(modTitle.getString(), titleMaxW);
            context.drawTextWithShadow(textRenderer, Text.literal(trimmedTitle), titleX, curY + 2, ActivityColors.scaleAlpha(ActivityColors.TEXT_PRIMARY, eased));
        }
        curY += 24;

        // Status Badge (Включен / Выключен)
        IModule module = ModuleRegistry.get(this.moduleId);
        boolean isEnabled = module != null && module.isEnabled();

        int dotColor = isEnabled ? ActivityColors.STATE_ON_BG : ActivityColors.DANGER;
        dotColor = ActivityColors.scaleAlpha(dotColor, eased);
        ActivityGuiRenderer.fill(context, innerX, curY + 3, 6, 6, dotColor);

        if (textRenderer != null) {
            MutableText statusLabel = Text.translatable("activity.sheet.status").append(": ");
            Text statusVal = Text.translatable(isEnabled ? "activity.sheet.status_enabled" : "activity.sheet.status_disabled");
            int stTextColor = isEnabled ? ActivityColors.TEXT_PRIMARY : ActivityColors.TEXT_SECONDARY;
            context.drawTextWithShadow(textRenderer, FontManager.wrap(statusLabel.append(statusVal)), innerX + 10, curY + 2, ActivityColors.scaleAlpha(stTextColor, eased));
        }
        curY += 18;

        // Divider
        ActivityGuiRenderer.drawHorizontalLine(context, innerX, curY, innerW, ActivityColors.scaleAlpha(ActivityColors.BORDER_DIVIDER, eased));
        curY += 8;

        // Metadata rows
        curY = renderMetaRow(context, textRenderer, innerX, curY, innerW, Text.translatable("activity.sheet.author"), Text.literal(this.metadata.getAuthor()), eased);
        curY = renderMetaRow(context, textRenderer, innerX, curY, innerW, Text.translatable("activity.sheet.version"), Text.literal(this.metadata.getVersion()), eased);
        curY = renderMetaRow(context, textRenderer, innerX, curY, innerW, Text.translatable("activity.sheet.last_updated"), Text.literal(this.metadata.getLastUpdated()), eased);
        curY = renderMetaRow(context, textRenderer, innerX, curY, innerW, Text.translatable("activity.sheet.keybind"), Text.literal(this.metadata.getKeybindDisplay()), eased);

        curY += 4;
        ActivityGuiRenderer.drawHorizontalLine(context, innerX, curY, innerW, ActivityColors.scaleAlpha(ActivityColors.BORDER_DIVIDER, eased));
        curY += 8;

        // Description
        if (textRenderer != null) {
            Text descLabel = Text.translatable("activity.sheet.description");
            context.drawTextWithShadow(textRenderer, FontManager.wrap(descLabel), innerX, curY, ActivityColors.scaleAlpha(ActivityColors.TEXT_SECONDARY, eased));
            curY += 14;

            Text descText = this.metadata.getDescription();
            if (this.cachedDescriptionLines == null || this.lastDescInnerW != innerW) {
                this.cachedDescriptionLines = textRenderer.wrapLines(FontManager.wrap(descText), innerW);
                this.lastDescInnerW = innerW;
            }
            List<OrderedText> lines = this.cachedDescriptionLines;
            int maxLines = Math.min(lines.size(), (screenH - curY - 90) / 10);
            for (int i = 0; i < maxLines; i++) {
                context.drawTextWithShadow(textRenderer, lines.get(i), innerX, curY, ActivityColors.scaleAlpha(ActivityColors.TEXT_PRIMARY, eased));
                curY += 10;
            }
        }

        // Channel notice: "Все обновления модификаций: @virionDEV"
        int noticeY = screenH - 74;
        if (textRenderer != null && noticeY > curY) {
            if (this.cachedNoticeText == null || this.lastNoticeInnerW != innerW) {
                Text noticeText = Text.translatable("activity.sheet.telegram_notice");
                Text wrappedNotice = FontManager.wrap(noticeText);
                String trimmedNotice = textRenderer.trimToWidth(wrappedNotice.getString(), innerW);
                this.cachedNoticeText = Text.literal(trimmedNotice);
                this.lastNoticeInnerW = innerW;
            }
            context.drawTextWithShadow(textRenderer, this.cachedNoticeText, innerX, noticeY, ActivityColors.scaleAlpha(ActivityColors.ACCENT_LIGHT, eased));
        }

        // Action Buttons at bottom
        int btnW = innerW;
        int btnH = 20;

        // Button 1: Telegram
        int btnTgY = screenH - 50;
        this.telegramHovered = mouseX >= innerX && mouseX < innerX + btnW && mouseY >= btnTgY && mouseY < btnTgY + btnH;
        int tgBg = ActivityColors.scaleAlpha(this.telegramHovered ? ActivityColors.BUTTON_SECONDARY_HOVER : ActivityColors.BUTTON_SECONDARY_BG, eased);
        ActivityGuiRenderer.fill(context, innerX, btnTgY, btnW, btnH, tgBg);
        ActivityGuiRenderer.drawBorder(context, innerX, btnTgY, btnW, btnH, ActivityColors.scaleAlpha(ActivityColors.BORDER_CARD, eased));

        int tgColor = ActivityColors.scaleAlpha(this.telegramHovered ? ActivityColors.TEXT_ACCENT : ActivityColors.TEXT_PRIMARY, eased);
        ActivityIconRenderer.draw(context, ActivityIcon.TELEGRAM, innerX + 8, btnTgY + (btnH - 12) / 2, tgColor);
        if (textRenderer != null) {
            Text tgLabel = FontManager.wrap(Text.translatable("activity.sheet.button_telegram"));
            int labelX = innerX + 24;
            int labelY = btnTgY + (btnH - textRenderer.fontHeight) / 2;
            context.drawTextWithShadow(textRenderer, tgLabel, labelX, labelY, tgColor);
        }

        // Button 2: Open Settings
        int btnSettingsY = screenH - 26;
        this.settingsHovered = mouseX >= innerX && mouseX < innerX + btnW && mouseY >= btnSettingsY && mouseY < btnSettingsY + btnH;
        int setBg = ActivityColors.scaleAlpha(this.settingsHovered ? ActivityColors.BUTTON_PRIMARY_HOVER : ActivityColors.BUTTON_PRIMARY_BG, eased);
        ActivityGuiRenderer.fill(context, innerX, btnSettingsY, btnW, btnH, setBg);
        ActivityGuiRenderer.drawBorder(context, innerX, btnSettingsY, btnW, btnH, ActivityColors.scaleAlpha(ActivityColors.BORDER_HOVER, eased));

        int setColor = ActivityColors.scaleAlpha(this.settingsHovered ? ActivityColors.ACCENT_LIGHT : ActivityColors.TEXT_PRIMARY, eased);
        ActivityIconRenderer.draw(context, ActivityIcon.SETTINGS, innerX + 8, btnSettingsY + (btnH - 12) / 2, setColor);
        if (textRenderer != null) {
            Text setLabel = FontManager.wrap(Text.translatable("activity.sheet.button_settings"));
            int labelX = innerX + 24;
            int labelY = btnSettingsY + (btnH - textRenderer.fontHeight) / 2;
            context.drawTextWithShadow(textRenderer, setLabel, labelX, labelY, setColor);
        }
    }

    private int renderMetaRow(DrawContext context, TextRenderer textRenderer, int x, int y, int width, Text label, Text value, float alpha) {
        if (textRenderer == null) return y + 14;

        int labelColor = ActivityColors.scaleAlpha(ActivityColors.TEXT_SECONDARY, alpha);
        int valColor = ActivityColors.scaleAlpha(ActivityColors.TEXT_PRIMARY, alpha);

        Text wrappedLabel = FontManager.wrap(label);
        Text wrappedVal = FontManager.wrap(value);

        context.drawTextWithShadow(textRenderer, wrappedLabel, x, y, labelColor);
        int valW = textRenderer.getWidth(wrappedVal);
        context.drawTextWithShadow(textRenderer, wrappedVal, x + width - valW, y, valColor);

        return y + 13;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (this.closed) return false;

        int renderX = getRenderX();
        int screenW = getScreenWidth();
        int screenH = getScreenHeight();

        // Clicking outside the sheet dismisses it
        if (click.x() < renderX || click.x() > screenW || click.y() < 0 || click.y() > screenH) {
            close();
            return true;
        }

        int width = getEffectiveWidth();
        int headerH = 28;

        // Close Button
        int closeX = renderX + width - 22;
        int closeY = (headerH - 14) / 2;
        if (click.x() >= closeX - 5 && click.x() < closeX + 19 && click.y() >= closeY - 5 && click.y() < closeY + 19) {
            SoundManager.playClick();
            close();
            return true;
        }

        int innerX = renderX + 12;
        int innerW = width - 24;
        int btnW = innerW;
        int btnH = 20;
        int touchPadY = 2; // Expands 20px button to 24px touch hitbox

        // Button 1: Telegram
        int btnTgY = screenH - 50;
        if (click.x() >= innerX && click.x() < innerX + btnW && click.y() >= btnTgY - touchPadY && click.y() < btnTgY + btnH + touchPadY) {
            activity.client.gui.tab.AboutTab.openUrl(this.metadata.getTelegramUrl(), this.screen);
            return true;
        }

        // Button 2: Open Settings
        int btnSettingsY = screenH - 26;
        if (click.x() >= innerX && click.x() < innerX + btnW && click.y() >= btnSettingsY - touchPadY && click.y() < btnSettingsY + btnH + touchPadY) {
            SoundManager.playClick();
            close();
            if (this.screen != null) {
                this.screen.navigateToModule(this.moduleId);
            }
            return true;
        }

        // Any click inside the sheet is consumed
        return true;
    }
}

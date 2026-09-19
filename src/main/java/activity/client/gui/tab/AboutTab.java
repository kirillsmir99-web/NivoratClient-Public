package activity.client.gui.tab;

import activity.client.ActivityClient;
import activity.client.config.ActivityConfig;
import activity.client.gui.ActivityScreen;
import activity.client.gui.component.ActivityButton;
import activity.client.gui.component.ActivityLabel;
import activity.client.gui.component.ActivityPanel;
import activity.client.gui.icon.ActivityIcon;
import activity.client.gui.layout.ScrollContainer;
import activity.client.gui.overlay.ToastOverlay;
import activity.client.gui.sound.SoundManager;
import activity.client.gui.theme.ActivityColors;
import activity.client.gui.theme.ActivityMetrics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

/**
 * About and project information tab: Mod version, Authorship watermark, primary social actions, and secondary socials.
 */
public class AboutTab extends ActivityTab {

    public static final String URL_TELEGRAM = "https://t.me/virionDEV";
    public static final String URL_DONATE = "https://www.donationalerts.com/r/nivorat";
    public static final String URL_YOUTUBE = "https://www.youtube.com/@Nivorat";
    public static final String URL_TIKTOK = "https://www.tiktok.com/@nivorat";
    public static final String URL_DISCORD = "https://discord.gg/qkezDA7tFX";
    public static final String WATERMARK = "@virionDEV";
    public static final String CLIENT_NAME = "NivoratClient";
    public static final String CLIENT_VERSION = "v1.0.0";
    public static final String DEVELOPER = "Nivorat";

    public static final Identifier TEXTURE_TELEGRAM = Identifier.of("nivoratclient", "textures/gui/social/telegram.png");
    public static final Identifier TEXTURE_DONATE = Identifier.of("nivoratclient", "textures/gui/social/donate.png");
    public static final Identifier TEXTURE_YOUTUBE = Identifier.of("nivoratclient", "textures/gui/social/youtube.png");
    public static final Identifier TEXTURE_TIKTOK = Identifier.of("nivoratclient", "textures/gui/social/tiktok.png");
    public static final Identifier TEXTURE_DISCORD = Identifier.of("nivoratclient", "textures/gui/social/discord.png");

    private static final Text HEADER_TITLE = Text.translatable("activity.tab.about.header");
    private static final Text SUBTITLE = Text.translatable("activity.tab.about.subtitle");

    @FunctionalInterface
    public interface UrlOpener {
        void open(String url) throws Exception;
    }

    public static UrlOpener URL_OPENER = url -> {
        Util.getOperatingSystem().open(URI.create(url));
    };

    public AboutTab() {
        super("about", Text.translatable("activity.tab.about"), ActivityIcon.ABOUT);
    }

    @Override
    public Text getHeaderTitle() {
        return HEADER_TITLE;
    }

    @Override
    public Text getSubtitle() {
        return SUBTITLE;
    }

    @Override
    public void resetDefaults() {
    }

    @Override
    public void loadFromConfig(ActivityConfig config) {
    }

    @Override
    public void saveToConfig(ActivityConfig config) {
    }

    /**
     * Attempts to open the specified URL in the system default browser.
     * If opening fails, presents a non-blocking toast with a copy-to-clipboard action.
     *
     * @param url    the URL to open
     * @param screen active ActivityScreen for displaying the error toast
     */
    public static void openUrl(String url, @Nullable ActivityScreen screen) {
        if (url == null || url.trim().isEmpty()) return;
        try {
            URL_OPENER.open(url);
            SoundManager.playLinkOpen();
        } catch (Throwable t) {
            ActivityScreen targetScreen = screen;
            if (targetScreen == null) {
                try {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc != null && mc.currentScreen instanceof ActivityScreen actScreen) {
                        targetScreen = actScreen;
                    }
                } catch (Throwable ignored) {}
            }
            if (targetScreen != null) {
                targetScreen.showOpenUrlErrorToast(url);
            } else {
                copyToClipboard(url);
                try {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc != null && mc.player != null) {
                        mc.player.sendMessage(Text.translatable("activity.toast.url_copied"), false);
                    } else {
                        ActivityClient.LOGGER.info("[Activity] URL copied to clipboard: {}", url);
                    }
                } catch (Throwable ignored) {
                    ActivityClient.LOGGER.info("[Activity] URL copied to clipboard: {}", url);
                }
            }
        }
    }

    public static void copyToClipboard(String text) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.keyboard != null) {
                mc.keyboard.setClipboard(text);
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void buildTab(ActivityScreen screen, ScrollContainer container, int startX, int startY, int rowWidth) {
        this.clearComponents();
        boolean twoColumns = rowWidth >= ActivityMetrics.RESPONSIVE_TWO_COLUMN_BREAKPOINT;
        int colGap = ActivityMetrics.COLUMN_GAP;
        int cardW = twoColumns ? (rowWidth - colGap) / 2 : rowWidth;
        int col1X = startX;
        int col2X = twoColumns ? (startX + cardW + colGap) : startX;

        int innerRowW = cardW - ActivityMetrics.PADDING_PANEL * 2;
        int col1Y = startY;
        int col2Y = startY;

        // ==========================================
        // CARD 1: HEADER & CLIENT INFORMATION
        // ==========================================
        int card1X = col1X;
        int innerStartX = card1X + ActivityMetrics.PADDING_PANEL;
        int curY = col1Y;
        int rows1 = 5;
        int card1Height = 22 + rows1 * (ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING) + 4;
        ActivityPanel card1 = createCard(container, card1X, curY, cardW, card1Height, Text.translatable("activity.card.about.info"));
        registerModuleCard("about_info", card1);
        registerCardAlias("info", card1);
        registerCardAlias("links", card1);
        registerCardAlias("header", card1);

        int rowY = curY + 22;

        int labelMaxW = Math.min(105, Math.max(50, (int) (innerRowW * 0.40f)));
        int valW = Math.min(190, Math.max(60, innerRowW - labelMaxW - 6));

        // Row 1.1: Name
        ActivityLabel labelName = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.about.mod_name"));
        labelName.setMaxWidth(labelMaxW);
        ActivityLabel valName = new ActivityLabel(innerStartX + innerRowW - valW, rowY + 3, Text.translatable("activity.about.val_name"));
        valName.setMaxWidth(valW);
        addControl(container, labelName);
        addControl(container, valName);

        // Row 1.2: Mod Version
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelVer = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.about.version"));
        labelVer.setMaxWidth(labelMaxW);
        ActivityLabel valVer = new ActivityLabel(innerStartX + innerRowW - valW, rowY + 3, Text.translatable("activity.about.val_version"));
        valVer.setMaxWidth(valW);
        addControl(container, labelVer);
        addControl(container, valVer);

        // Row 1.3: Developer / Разработчик
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelAuthor = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.about.author"));
        labelAuthor.setMaxWidth(labelMaxW);
        ActivityLabel valAuthor = new ActivityLabel(innerStartX + innerRowW - valW, rowY + 3, Text.translatable("activity.about.val_author"));
        valAuthor.setMaxWidth(valW);
        addControl(container, labelAuthor);
        addControl(container, valAuthor);

        // Row 1.4: Watermark
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelTg = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.about.watermark_label"));
        labelTg.setMaxWidth(labelMaxW);
        ActivityLabel valTg = new ActivityLabel(innerStartX + innerRowW - valW, rowY + 3, Text.translatable("activity.about.telegram_watermark"), ActivityColors.TEXT_ACCENT);
        valTg.setMaxWidth(valW);
        addControl(container, labelTg);
        addControl(container, valTg);

        // Row 1.5: Copy Watermark
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityButton btnCopyTg = new ActivityButton(
            innerStartX, rowY, innerRowW, ActivityMetrics.CONTROL_HEIGHT,
            ActivityIcon.COPY,
            Text.translatable("activity.button.copy_telegram"),
            btn -> {
                copyToClipboard(WATERMARK);
                SoundManager.playPresetSave();
            }
        );
        btnCopyTg.setTouchPadding(ActivityMetrics.TOUCH_HITBOX_PADDING);
        addControl(container, btnCopyTg);

        // ==========================================
        // CARD 2: PRIMARY ACTIONS & SECONDARY SOCIALS
        // ==========================================
        int card2X = twoColumns ? col2X : col1X;
        innerStartX = card2X + ActivityMetrics.PADDING_PANEL;
        curY = twoColumns ? col2Y : (col1Y + card1Height + 10);

        int primaryBtnH = 24;
        int secBtnH = 20;
        int spacing = ActivityMetrics.ROW_SPACING;
        int cardSocialsHeight = 22 + primaryBtnH + spacing + primaryBtnH + spacing + secBtnH + 6;

        ActivityPanel cardSocials = createCard(container, card2X, curY, cardW, cardSocialsHeight, Text.translatable("activity.card.about.socials"));
        registerModuleCard("about_socials", cardSocials);
        registerCardAlias("socials", cardSocials);
        registerCardAlias("community", cardSocials);
        registerCardAlias("telegram", cardSocials);
        registerCardAlias("donate", cardSocials);

        rowY = curY + 22;

        // Primary Action 1: Telegram (Prominent, height 24, Variant.PRIMARY)
        ActivityButton btnTg = new ActivityButton(
            innerStartX, rowY, innerRowW, primaryBtnH,
            ActivityIcon.TELEGRAM,
            Text.translatable("activity.button.telegram"),
            ActivityButton.Variant.PRIMARY,
            btn -> openUrl(URL_TELEGRAM, screen)
        );
        btnTg.setBrandHoverColor(0xFF2AABEE);
        btnTg.setCustomTexture(TEXTURE_TELEGRAM);
        btnTg.setTouchPadding(ActivityMetrics.TOUCH_HITBOX_PADDING);
        addControl(container, btnTg);

        // Primary Action 2: Поддержать автора (Prominent, height 24, Variant.PRIMARY)
        rowY += primaryBtnH + spacing;
        ActivityButton btnDonate = new ActivityButton(
            innerStartX, rowY, innerRowW, primaryBtnH,
            ActivityIcon.DONATE,
            Text.translatable("activity.button.donate_author"),
            ActivityButton.Variant.PRIMARY,
            btn -> openUrl(URL_DONATE, screen)
        );
        btnDonate.setBrandHoverColor(0xFFFF4757);
        btnDonate.setCustomTexture(TEXTURE_DONATE);
        btnDonate.setTouchPadding(ActivityMetrics.TOUCH_HITBOX_PADDING);
        addControl(container, btnDonate);

        // Secondary Socials: Compact horizontal row (YouTube, TikTok, Discord)
        rowY += primaryBtnH + spacing;
        int secGap = 4;
        int secBtnW = Math.max(20, (innerRowW - secGap * 2) / 3);
        int lastSecBtnW = Math.max(20, innerRowW - secBtnW * 2 - secGap * 2);

        int secX = innerStartX;
        ActivityButton btnYoutube = new ActivityButton(
            secX, rowY, secBtnW, secBtnH,
            ActivityIcon.YOUTUBE,
            Text.translatable("activity.button.youtube"),
            ActivityButton.Variant.SECONDARY,
            btn -> openUrl(URL_YOUTUBE, screen)
        );
        btnYoutube.setBrandHoverColor(0xFFFF0000);
        btnYoutube.setCustomTexture(TEXTURE_YOUTUBE);
        btnYoutube.setTouchPadding(ActivityMetrics.TOUCH_HITBOX_PADDING);
        addControl(container, btnYoutube);

        secX += secBtnW + secGap;
        ActivityButton btnTiktok = new ActivityButton(
            secX, rowY, secBtnW, secBtnH,
            ActivityIcon.TIKTOK,
            Text.translatable("activity.button.tiktok"),
            ActivityButton.Variant.SECONDARY,
            btn -> openUrl(URL_TIKTOK, screen)
        );
        btnTiktok.setBrandHoverColor(0xFF00F2FE);
        btnTiktok.setCustomTexture(TEXTURE_TIKTOK);
        btnTiktok.setTouchPadding(ActivityMetrics.TOUCH_HITBOX_PADDING);
        addControl(container, btnTiktok);

        secX += secBtnW + secGap;
        ActivityButton btnDiscord = new ActivityButton(
            secX, rowY, lastSecBtnW, secBtnH,
            ActivityIcon.DISCORD,
            Text.translatable("activity.button.discord"),
            ActivityButton.Variant.SECONDARY,
            btn -> openUrl(URL_DISCORD, screen)
        );
        btnDiscord.setBrandHoverColor(0xFF5865F2);
        btnDiscord.setCustomTexture(TEXTURE_DISCORD);
        btnDiscord.setTouchPadding(ActivityMetrics.TOUCH_HITBOX_PADDING);
        addControl(container, btnDiscord);

        // ==========================================
        // CARD 3: SYSTEM ENVIRONMENT & DIAGNOSTICS
        // ==========================================
        int card3X = col1X;
        innerStartX = card3X + ActivityMetrics.PADDING_PANEL;
        curY = twoColumns ? (col1Y + card1Height + 10) : (rowY + secBtnH + 14);

        int rows3 = 5;
        int card3Height = 22 + rows3 * (ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING) + 4;
        ActivityPanel card3 = createCard(container, card3X, curY, cardW, card3Height, Text.translatable("activity.card.about.system"));
        registerModuleCard("about_system", card3);
        registerCardAlias("system", card3);
        registerCardAlias("diagnostics", card3);

        rowY = curY + 22;

        // Row 3.1: Minecraft
        ActivityLabel labelMc = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.about.minecraft"));
        labelMc.setMaxWidth(labelMaxW);
        ActivityLabel valMc = new ActivityLabel(innerStartX + innerRowW - valW, rowY + 3, Text.translatable("activity.about.val_minecraft"));
        valMc.setMaxWidth(valW);
        addControl(container, labelMc);
        addControl(container, valMc);

        // Row 3.2: Loader
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelLoader = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.about.loader"));
        labelLoader.setMaxWidth(labelMaxW);
        ActivityLabel valLoader = new ActivityLabel(innerStartX + innerRowW - valW, rowY + 3, Text.translatable("activity.about.val_loader"));
        valLoader.setMaxWidth(valW);
        addControl(container, labelLoader);
        addControl(container, valLoader);

        // Row 3.3: Build Info
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelBuild = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.about.build_info"));
        labelBuild.setMaxWidth(labelMaxW);
        ActivityLabel valBuild = new ActivityLabel(innerStartX + innerRowW - valW, rowY + 3, Text.translatable("activity.about.val_build_info"));
        valBuild.setMaxWidth(valW);
        addControl(container, labelBuild);
        addControl(container, valBuild);

        // Row 3.4: Active Font
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelFont = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.about.active_font"));
        labelFont.setMaxWidth(labelMaxW);
        Text fontText = "retro_pixel".equalsIgnoreCase(activity.client.gui.font.FontManager.getActiveFamily())
            ? Text.translatable("activity.font.retro_pixel")
            : Text.translatable("activity.font.minecraft_default");
        ActivityLabel valFont = new ActivityLabel(innerStartX + innerRowW - valW, rowY + 3, fontText);
        valFont.setMaxWidth(valW);
        addControl(container, labelFont);
        addControl(container, valFont);

        // Row 3.5: Modules
        rowY += ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
        ActivityLabel labelMods = new ActivityLabel(innerStartX, rowY + 3, Text.translatable("activity.about.modules_count"));
        labelMods.setMaxWidth(labelMaxW);
        ActivityLabel valMods = new ActivityLabel(innerStartX + innerRowW - valW, rowY + 3, Text.translatable("activity.about.val_modules_count"));
        valMods.setMaxWidth(valW);
        addControl(container, labelMods);
        addControl(container, valMods);
    }
}

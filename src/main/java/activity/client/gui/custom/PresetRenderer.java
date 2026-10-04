package activity.client.gui.custom;

import activity.client.config.preset.LocalPresets;
import activity.client.gui.custom.api.drags.Position;
import activity.client.gui.custom.api.ui.UI;
import activity.client.gui.custom.api.ui.settings.RenderHelper;
import activity.client.gui.custom.api.ui.theme.ClientAccent;
import activity.client.gui.custom.api.ui.theme.ThemeManager;
import activity.client.gui.custom.utils.animations.Decelerate;
import activity.client.gui.custom.utils.animations.Direction;
import activity.client.gui.custom.utils.color.ColorUtil;
import activity.client.gui.custom.utils.render.fonts.Fonts;
import activity.client.gui.custom.utils.render.fonts.NvIcons;
import activity.client.gui.custom.utils.render.render2d.Render2D;
import activity.client.gui.custom.utils.render.render2d.glow.BuiltGlow;
import activity.client.gui.custom.utils.sounds.Sounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.*;

public final class PresetRenderer {
    public static final float DRAWER_WIDTH = 210.0f;
    private static final float HEADER_HEIGHT = 22.0f;
    private static final float FOOTER_HEIGHT = 28.0f;

    public static final List<String> ALL_FEATURE_MODULES = List.of(
        "auto_totem",
        "auto_mace",
        "auto_stun_slam",
        "auto_shieldbreaker",
        "auto_anchor",
        "auto_cart",
        "auto_spear",
        "auto_pearl_catch",
        "click_pearl",
        "cart_refill",
        "hp_reaper",
        "auto_tool",
        "auto_gg",
        "cart_hud",
        "cooldown_hud",
        "water_drop"
    );

    private List<LocalPresets.Entry> entries = List.of();
    private Set<String> favorites = Set.of();

    private float scroll = 0.0f;
    private float scrollTarget = 0.0f;

    private boolean drawerOpen = false;
    private final Decelerate drawerAnim = (Decelerate) new Decelerate().setMs(240).setValue(1.0);
    private String drawerName = "";
    private boolean nameFocused = false;
    private boolean nameSelectedAll = false;
    private float nameHoverT = 0.0f;
    private float nameFocusT = 0.0f;
    private final List<Float> nameCharAnim = new ArrayList<>();
    private float nameAnimCursorX = 0.0f;
    private boolean nameCursorSnap = true;

    private boolean autoActivate = true;
    private LocalPresets.Template drawerTemplate = LocalPresets.Template.CURRENT;
    private final EnumSet<LocalPresets.Part> drawerParts = EnumSet.allOf(LocalPresets.Part.class);
    private final Set<String> drawerModules = new LinkedHashSet<>(ALL_FEATURE_MODULES);

    private float drawerScroll = 0.0f;
    private float drawerScrollTarget = 0.0f;

    private String toastMessage = null;
    private long toastTime = 0;
    private Path deletingPath = null;
    private long deletePromptTime = 0;

    private float lastX, lastY, lastW, lastH;
    private float lastImpX, lastImpY, lastImpW, lastImpH;
    private float lastCreateX, lastCreateY, lastCreateW, lastCreateH;

    private float lastDrawerX, lastDrawerY, lastDrawerW, lastDrawerH;
    private float lastCloseX, lastCloseY, lastCloseW, lastCloseH;
    private float lastCancelX, lastCancelY, lastCancelW, lastCancelH;
    private float lastSaveX, lastSaveY, lastSaveW, lastSaveH;
    private float lastNameBoxX, lastNameBoxY, lastNameBoxW, lastNameBoxH;
    private float lastPill1X, lastPill1Y, lastPillW, lastPillH;
    private float lastPill2X, lastPill2Y;
    private final float[] lastPartRowY = new float[7];
    private final float[] lastModRowY = new float[16];
    private float lastToggleAllModsX, lastToggleAllModsY, lastToggleAllModsW;
    private float lastAutoActY;
    private float lastToggleAllX, lastToggleAllY, lastToggleAllW;

    public PresetRenderer() {
        this.drawerAnim.setDirection(Direction.BACKWARDS);
        this.drawerAnim.counter.setTime(System.currentTimeMillis() - 10000L);
    }

    private boolean isRu() {
        return activity.client.i18n.LocalizationService.isRussianPreferred();
    }

    private String tr(String ru, String en) {
        return isRu() ? ru : en;
    }

    private String resolvePresetServerTag(String name) {
        if (name == null || name.isEmpty()) return tr("Для всех серверов", "For all servers");
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("really") || lower.contains("rw")) return "Для ReallyWorld";
        if (lower.contains("funtime") || lower.contains("ft")) return "Для FunTime";
        if (lower.contains("anarchy") || lower.contains("анарх")) return "Для Анархии";
        if (lower.contains("smp")) return "Для SMP";
        if (lower.contains("pvp") || lower.contains("пвп") || lower.contains("дуэл") || lower.contains("duel")) return "Для PvP";
        if (lower.contains("crystal") || lower.contains("кристал") || lower.contains("holy")) return "Для HolyWorld";
        return tr("Для всех серверов", "For all servers");
    }

    private String getModuleLabel(String id) {
        return switch (id) {
            case "auto_totem" -> tr("Авто-тотем (AutoTotem)", "Auto Totem");
            case "auto_mace" -> tr("Авто-мейс (AutoMace)", "Auto Mace");
            case "auto_stun_slam" -> tr("Стан-слэм (StunSlam)", "Stun Slam");
            case "auto_shieldbreaker" -> tr("Щит-брейкер (ShieldBreaker)", "Shield Breaker");
            case "auto_anchor" -> tr("Авто-якорь (AutoAnchor)", "Auto Anchor");
            case "auto_cart" -> tr("Авто-тележка (AutoCart)", "Auto Cart");
            case "auto_spear" -> tr("Авто-гарпун (AutoSpear)", "Auto Spear");
            case "auto_pearl_catch" -> tr("Ловля перлов (PearlCatch)", "Pearl Catch");
            case "click_pearl" -> tr("Клик-перл (ClickPearl)", "Click Pearl");
            case "cart_refill" -> tr("Рефилл тележек (CartRefill)", "Cart Refill");
            case "hp_reaper" -> tr("ХП Рипер (HPReaper)", "HP Reaper");
            case "auto_tool" -> tr("Авто-тул (AutoTool)", "Auto Tool");
            case "auto_gg" -> tr("Авто-ГГ (AutoGG)", "Auto GG");
            case "cart_hud" -> tr("Карт HUD (CartHUD)", "Cart HUD");
            case "cooldown_hud" -> tr("Кулдаун HUD (CooldownHUD)", "Cooldown HUD");
            case "water_drop" -> tr("Авто-сейв (AutoSave)", "Auto Save");
            default -> id;
        };
    }

    private static int color(int r, int g, int b, int a, float alpha) {
        int finalA = Math.max(0, Math.min(255, Math.round((float) a * alpha)));
        return (finalA << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public void open() {
        this.scroll = 0.0f;
        this.scrollTarget = 0.0f;
        this.drawerOpen = false;
        this.drawerAnim.setDirection(Direction.BACKWARDS);
        this.drawerAnim.counter.setTime(System.currentTimeMillis() - 10000L);
        this.deletingPath = null;
        this.reload();
    }

    public void reload() {
        try {
            this.entries = LocalPresets.list();
            this.favorites = LocalPresets.favorites();
        } catch (Exception e) {
            this.entries = List.of();
            this.favorites = Set.of();
        }
    }

    public void showToast(String msg) {
        this.toastMessage = msg;
        this.toastTime = System.currentTimeMillis();
    }

    public boolean isDrawerOpen() {
        return this.drawerOpen || this.drawerAnim.getOutput().floatValue() > 0.01f;
    }

    public float dockProgress() {
        return this.drawerAnim.getOutput().floatValue();
    }

    public float dockWidth() {
        return DRAWER_WIDTH * this.dockProgress();
    }

    public boolean isInputActive() {
        return this.drawerOpen && this.nameFocused;
    }

    public void openDrawer() {
        if (UI.INSTANCE != null) {
            UI.INSTANCE.getInspector().close();
            UI.INSTANCE.getThemesRenderer().getEditor().close();
        }
        this.drawerOpen = true;
        this.drawerAnim.setDirection(Direction.FORWARDS);
        this.drawerName = "";
        this.nameCharAnim.clear();
        this.nameFocused = true;
        this.nameSelectedAll = false;
        this.nameCursorSnap = true;
        this.drawerTemplate = LocalPresets.Template.CURRENT;
        this.drawerParts.clear();
        this.drawerParts.addAll(EnumSet.allOf(LocalPresets.Part.class));
        this.drawerParts.remove(LocalPresets.Part.APPEARANCE);
        this.drawerModules.clear();
        this.drawerModules.addAll(ALL_FEATURE_MODULES);
        this.autoActivate = true;
        this.drawerScroll = 0.0f;
        this.drawerScrollTarget = 0.0f;
        try { Sounds.play("module_settings_open"); } catch (Throwable ignored) {}
    }

    public void closeDrawer() {
        this.drawerOpen = false;
        this.nameFocused = false;
        this.nameSelectedAll = false;
        this.drawerAnim.setDirection(Direction.BACKWARDS);
        try { Sounds.play("module_settings_close"); } catch (Throwable ignored) {}
    }

    public void render(DrawContext drawContext, float x, float y, float w, float h, float alpha) {
        this.lastX = x;
        this.lastY = y;
        this.lastW = w;
        this.lastH = h;

        float fSpeed = 1.0f - (float) Math.exp(-0.016f * 14.0f);
        this.scroll += (this.scrollTarget - this.scroll) * fSpeed;
        if (Math.abs(this.scrollTarget - this.scroll) < 0.05f) {
            this.scroll = this.scrollTarget;
        }

        if (this.deletingPath != null && System.currentTimeMillis() - this.deletePromptTime > 3500L) {
            this.deletingPath = null;
        }

        float mx = Position.mouseX();
        float my = Position.mouseY();
        float dt = 0.016f;

        float contentW = Math.min(276.0f, w - 24.0f);
        float contentX = x + (w - contentW) * 0.5f;

        float headerH = 26.0f;
        float btnH = 14.0f;
        float btnY = y + (headerH - btnH) * 0.5f;

        this.lastImpW = 52.0f;
        this.lastImpH = btnH;
        this.lastImpX = contentX + contentW - this.lastImpW;
        this.lastImpY = btnY;
        boolean hoverImp = mx >= lastImpX && mx <= lastImpX + lastImpW && my >= lastImpY && my <= lastImpY + lastImpH;
        Render2D.rect(lastImpX, lastImpY, lastImpW, lastImpH, 3.5f, hoverImp ? ClientAccent.accent(45.0f * alpha) : ThemeManager.rgba(0xFFFFFF, 14.0f * alpha));
        Render2D.outline(lastImpX, lastImpY, lastImpW, lastImpH, 3.5f, 0.6f, hoverImp ? ClientAccent.accent(160.0f * alpha) : ThemeManager.rgba(0xFFFFFF, 18.0f * alpha));
        if (hoverImp) {
            Render2D.glow(new BuiltGlow(lastImpX, lastImpY, lastImpW, lastImpH, new float[]{3.5f, 3.5f, 3.5f, 3.5f}, ClientAccent.accent(180.0f * alpha), 0.25f, 3.5f, alpha));
        }
        Fonts.NV.msdf(NvIcons.IMPORT, lastImpX + 5.0f, lastImpY + 3.8f, 6.0f, hoverImp ? ClientAccent.accentBright(255.0f * alpha) : color(255, 255, 255, 200, alpha));
        Fonts.MONTSERRAT_MEDIUM.draw(tr("Импорт", "Import"), lastImpX + 15.0f, lastImpY + 3.5f, 5.2f, hoverImp ? ClientAccent.accentBright(255.0f * alpha) : color(255, 255, 255, 200, alpha));

        float createTextW = Fonts.MONTSERRAT_MEDIUM.width(tr("Создать", "Create"), 5.2f);
        this.lastCreateW = 6.0f + 6.0f + 4.0f + createTextW + 6.0f;
        this.lastCreateH = btnH;
        this.lastCreateX = lastImpX - this.lastCreateW - 4.0f;
        this.lastCreateY = btnY;
        boolean hoverCreate = mx >= lastCreateX && mx <= lastCreateX + lastCreateW && my >= lastCreateY && my <= lastCreateY + lastCreateH;
        Render2D.rect(lastCreateX, lastCreateY, lastCreateW, lastCreateH, 3.5f, ClientAccent.accent(hoverCreate ? 220.0f * alpha : 175.0f * alpha));
        if (hoverCreate) {
            Render2D.glow(new BuiltGlow(lastCreateX, lastCreateY, lastCreateW, lastCreateH, new float[]{3.5f, 3.5f, 3.5f, 3.5f}, ClientAccent.accent(255.0f * alpha), 0.35f, 4.0f, alpha));
        }
        float createIconY = lastCreateY + (btnH - 6.0f) * 0.5f;
        float createTextY = lastCreateY + (btnH - 5.2f) * 0.5f - 0.2f;
        Fonts.NV.msdf(NvIcons.ADD, lastCreateX + 6.0f, createIconY, 6.0f, color(255, 255, 255, 255, alpha));
        Fonts.MONTSERRAT_MEDIUM.draw(tr("Создать", "Create"), lastCreateX + 16.0f, createTextY, 5.2f, color(255, 255, 255, 255, alpha));

        Fonts.MONTSERRAT_SEMIBOLD.draw(tr("Пресеты конфигурации", "Configuration presets"), contentX + 1.0f, btnY + 3.5f, 6.2f, color(255, 255, 255, 210, alpha));

        float cardsStartY = y + headerH + 5.0f;
        float cardsH = h - headerH - 8.0f;

        List<LocalPresets.Entry> filtered = new ArrayList<>();
        String query = UI.INSTANCE != null ? UI.INSTANCE.getSearchText().trim().toLowerCase(Locale.ROOT) : "";
        boolean hasSearchText = UI.INSTANCE != null && UI.INSTANCE.hasSearchText();
        for (LocalPresets.Entry e : this.entries) {
            if (query.isEmpty() || e.name().toLowerCase(Locale.ROOT).contains(query)) {
                filtered.add(e);
            }
        }

        if (filtered.isEmpty()) {
            float emptyY = cardsStartY + cardsH * 0.4f;
            Fonts.NV.msdf(NvIcons.PRESETS, contentX + (contentW - 16.0f) * 0.5f, emptyY - 14.0f, 16.0f, color(255, 255, 255, 60, alpha));
            String emptyTitle = hasSearchText ? tr("Ничего не найдено", "No presets found") : tr("Нет сохранённых пресетов", "No presets saved");
            String emptySub = hasSearchText ? tr("Попробуйте изменить поисковый запрос", "Try another search query") : tr("Нажмите кнопку \"Создать\", чтобы сохранить текущую сборку", "Click \"Create\" to save your current build");
            float ew = Fonts.MONTSERRAT_MEDIUM.width(emptyTitle, 7.0f);
            Fonts.MONTSERRAT_MEDIUM.draw(emptyTitle, contentX + (contentW - ew) * 0.5f, emptyY + 8.0f, 7.0f, color(255, 255, 255, 160, alpha));
            float esw = Fonts.MONTSERRAT_MEDIUM.width(emptySub, 5.5f);
            Fonts.MONTSERRAT_MEDIUM.draw(emptySub, contentX + (contentW - esw) * 0.5f, emptyY + 20.0f, 5.5f, color(255, 255, 255, 100, alpha));
        } else {
            float gap = 6.0f;
            float cardW = (contentW - gap) * 0.5f;
            float cardH = 50.0f;
            int totalRows = (filtered.size() + 1) / 2;
            float contentH = totalRows * (cardH + gap);
            float maxScroll = Math.max(0.0f, contentH - cardsH);
            this.scrollTarget = Math.max(0.0f, Math.min(this.scrollTarget, maxScroll));

            Render2D.pushScissor(drawContext, contentX, cardsStartY, contentW, cardsH);

            for (int i = 0; i < filtered.size(); i++) {
                LocalPresets.Entry entry = filtered.get(i);
                int col = i % 2;
                int row = i / 2;
                float cardX = contentX + (float) col * (cardW + gap);
                float cardY = cardsStartY + (float) row * (cardH + gap) - this.scroll;

                if (cardY + cardH < cardsStartY - 2.0f || cardY > cardsStartY + cardsH + 2.0f) {
                    continue;
                }

                boolean isHovered = mx >= cardX && mx <= cardX + cardW && my >= cardY && my <= cardY + cardH;
                String activeName = UI.INSTANCE.getSelectedPresetName();
                boolean isActive = activeName != null && activeName.equalsIgnoreCase(entry.name());
                boolean isFav = this.favorites.contains(entry.path().getFileName().toString());

                Render2D.rect(cardX, cardY, cardW, cardH, 4.0f, ThemeManager.rgba(0x101319, (isHovered ? 215.0f : 175.0f) * alpha));
                int borderCol = isActive ? ClientAccent.accent(220.0f * alpha) : (isHovered ? ThemeManager.rgba(0xFFFFFF, 38.0f * alpha) : ThemeManager.rgba(0xFFFFFF, 18.0f * alpha));
                Render2D.outline(cardX, cardY, cardW, cardH, 4.0f, 0.7f, borderCol);

                if (isActive) {
                    Render2D.glow(new BuiltGlow(cardX, cardY, cardW, cardH, new float[]{4.0f, 4.0f, 4.0f, 4.0f}, ClientAccent.accent(180.0f * alpha), 0.25f, 4.0f, alpha));
                    Render2D.rect(cardX + 2.0f, cardY + 8.0f, 2.0f, cardH - 16.0f, 1.0f, ClientAccent.accent(255.0f * alpha));
                } else if (isHovered) {
                    Render2D.glow(new BuiltGlow(cardX, cardY, cardW, cardH, new float[]{4.0f, 4.0f, 4.0f, 4.0f}, ClientAccent.accent(80.0f * alpha), 0.2f, 3.5f, alpha));
                }

                float iconSize = 7.0f;
                int iconCol = isActive ? ClientAccent.accentBright(255.0f * alpha) : color(255, 255, 255, 220, alpha);
                Fonts.NV.msdf(isActive ? NvIcons.CHECK : NvIcons.PRESETS, cardX + 8.0f, cardY + 8.0f, iconSize, iconCol);

                float nameFont = 6.5f;
                String displayName = entry.name();
                float maxNameW = cardW - 46.0f;
                if (Fonts.MONTSERRAT_MEDIUM.width(displayName, nameFont) > maxNameW && displayName.length() > 6) {
                    while (displayName.length() > 4 && Fonts.MONTSERRAT_MEDIUM.width(displayName + "...", nameFont) > maxNameW) {
                        displayName = displayName.substring(0, displayName.length() - 1);
                    }
                    displayName += "...";
                }
                Fonts.MONTSERRAT_MEDIUM.draw(displayName, cardX + 18.0f, cardY + 7.5f, nameFont, color(255, 255, 255, isActive ? 255 : 230, alpha));

                String metaStr = resolvePresetServerTag(entry.name());
                Fonts.MONTSERRAT_MEDIUM.draw(metaStr, cardX + 8.0f, cardY + 22.5f, 5.2f, isActive ? ClientAccent.accentBright(200.0f * alpha) : color(255, 255, 255, 140, alpha));

                float favBtnW = 10.0f;
                float favBtnH = 10.0f;
                float favX = cardX + cardW - 14.0f;
                float favY = cardY + 6.0f;
                boolean hovFav = mx >= favX - 2.0f && mx <= favX + favBtnW + 2.0f && my >= favY - 2.0f && my <= favY + favBtnH + 2.0f;
                int favCol = isFav ? ClientAccent.accentBright(255.0f * alpha) : (hovFav ? color(255, 255, 255, 220, alpha) : color(255, 255, 255, 80, alpha));
                Fonts.NV.msdf(NvIcons.PINNED, favX, favY, 6.0f, favCol);

                float actionH = 11.5f;
                float actionY = cardY + cardH - actionH - 5.0f;

                boolean isDeletingThis = this.deletingPath != null && this.deletingPath.equals(entry.path());
                float delW = isDeletingThis ? 36.0f : 14.0f;
                float delX = cardX + cardW - delW - 6.0f;
                boolean hovDel = mx >= delX && mx <= delX + delW && my >= actionY && my <= actionY + actionH;
                Render2D.rect(delX, actionY, delW, actionH, 2.5f, hovDel ? (isDeletingThis ? ThemeManager.rgba(0xaa2222, 220.0f * alpha) : ThemeManager.rgba(0xffffff, 25.0f * alpha)) : (isDeletingThis ? ThemeManager.rgba(0x882222, 170.0f * alpha) : ThemeManager.rgba(0xffffff, 10.0f * alpha)));
                if (isDeletingThis) {
                    float delTextW = Fonts.MONTSERRAT_MEDIUM.width(tr("Удалить", "Delete"), 4.5f);
                    Fonts.MONTSERRAT_MEDIUM.draw(tr("Удалить", "Delete"), delX + (delW - delTextW) * 0.5f, actionY + 2.5f, 4.5f, color(255, 255, 255, 255, alpha));
                } else {
                    float delIconSize = 7.0f;
                    float iconX = delX + (delW - delIconSize) * 0.5f;
                    float iconY = actionY + (actionH - delIconSize) * 0.5f;
                    int delCol = hovDel ? ThemeManager.rgba(0xff6666, 255.0f * alpha) : color(255, 255, 255, 210, alpha);
                    Fonts.NV.msdf(NvIcons.DELETE, iconX, iconY, delIconSize, delCol);
                    Fonts.NV.msdf(NvIcons.DELETE, iconX + 0.3f, iconY, delIconSize, delCol);
                }

                float expW = 14.0f;
                float expX = delX - expW - 4.0f;
                boolean hovExp = mx >= expX && mx <= expX + expW && my >= actionY && my <= actionY + actionH;
                Render2D.rect(expX, actionY, expW, actionH, 2.5f, hovExp ? ThemeManager.rgba(0xffffff, 25.0f * alpha) : ThemeManager.rgba(0xffffff, 10.0f * alpha));
                float expIconSize = 7.0f;
                float expIconX = expX + (expW - expIconSize) * 0.5f;
                float expIconY = actionY + (actionH - expIconSize) * 0.5f;
                int expCol = hovExp ? ClientAccent.accentBright(255.0f * alpha) : color(255, 255, 255, 210, alpha);
                Fonts.NV.msdf(NvIcons.EXPORT, expIconX, expIconY, expIconSize, expCol);
                Fonts.NV.msdf(NvIcons.EXPORT, expIconX + 0.3f, expIconY, expIconSize, expCol);

                if (isActive) {
                    float actTextW = Fonts.MONTSERRAT_MEDIUM.width(tr("Активен", "Active"), 4.5f);
                    float actBadgeW = actTextW + 8.0f;
                    float actBadgeX = cardX + 8.0f;
                    Render2D.rect(actBadgeX, actionY, actBadgeW, actionH, 2.5f, ClientAccent.accent(40.0f * alpha));
                    Render2D.outline(actBadgeX, actionY, actBadgeW, actionH, 2.5f, 0.5f, ClientAccent.accent(160.0f * alpha));
                    Fonts.MONTSERRAT_MEDIUM.draw(tr("Активен", "Active"), actBadgeX + 4.0f, actionY + 2.5f, 4.5f, ClientAccent.accentBright(255.0f * alpha));
                }
            }

            Render2D.popScissor(drawContext);
        }

        renderToast(drawContext, x, y, w, h, alpha);
    }

    public void renderDrawer(DrawContext drawContext, float x, float y, float w, float h, float alpha, float dt) {
        float progress = this.dockProgress();
        if (progress <= 0.005f) return;

        float effectiveAlpha = alpha * progress;
        float mx = Position.mouseX();
        float my = Position.mouseY();

        this.lastDrawerX = x;
        this.lastDrawerY = y;
        this.lastDrawerW = w;
        this.lastDrawerH = h;

        Render2D.rect(x - 3.5f, y + 6.0f, 1.0f, h - 12.0f, 0.5f, ThemeManager.rgba(0xFFFFFF, 14.0f * effectiveAlpha));
        RenderHelper.drawPanelBg(x, y, w, h, 0.0f, 12.0f, 12.0f, 0.0f, effectiveAlpha);

        Fonts.NV.msdf(NvIcons.PRESETS, x + 10.0f, y + 8.5f, 6.5f, ClientAccent.accentBright(240.0f * effectiveAlpha));
        Fonts.MONTSERRAT_MEDIUM.draw(tr("Создание пресета", "Create Preset"), x + 20.0f, y + 7.5f, 6.5f, color(255, 255, 255, 245, effectiveAlpha));

        this.lastCloseW = 12.0f;
        this.lastCloseH = 12.0f;
        this.lastCloseX = x + w - 10.0f - this.lastCloseW;
        this.lastCloseY = y + (HEADER_HEIGHT - this.lastCloseH) * 0.5f;
        boolean closeHov = mx >= lastCloseX - 2.0f && mx <= lastCloseX + lastCloseW + 2.0f && my >= lastCloseY - 2.0f && my <= lastCloseY + lastCloseH + 2.0f;
        Fonts.NV.msdf(NvIcons.CLOSE, lastCloseX + 2.5f, lastCloseY + 2.5f, 6.0f, color(255, 255, 255, closeHov ? 255 : 140, effectiveAlpha));

        Render2D.rect(x + 8.0f, y + HEADER_HEIGHT - 1.0f, w - 16.0f, 0.6f, 0.3f, ThemeManager.rgba(0xFFFFFF, 14.0f * effectiveAlpha));

        float footerY = y + h - FOOTER_HEIGHT;
        Render2D.rect(x + 8.0f, footerY, w - 16.0f, 0.6f, 0.3f, ThemeManager.rgba(0xFFFFFF, 14.0f * effectiveAlpha));

        float btnH = 18.0f;
        float btnY = footerY + (FOOTER_HEIGHT - btnH) * 0.5f;
        float gap = 6.0f;
        float totalBtnW = w - 16.0f;
        this.lastCancelW = (totalBtnW - gap) * 0.42f;
        this.lastSaveW = totalBtnW - gap - this.lastCancelW;
        this.lastCancelH = btnH;
        this.lastSaveH = btnH;
        this.lastCancelX = x + 8.0f;
        this.lastCancelY = btnY;
        this.lastSaveX = this.lastCancelX + this.lastCancelW + gap;
        this.lastSaveY = btnY;

        boolean cancelHov = mx >= lastCancelX && mx <= lastCancelX + lastCancelW && my >= lastCancelY && my <= lastCancelY + lastCancelH;
        Render2D.rect(lastCancelX, lastCancelY, lastCancelW, lastCancelH, 4.0f, cancelHov ? ThemeManager.rgba(0xFFFFFF, 26.0f * effectiveAlpha) : ThemeManager.rgba(0xFFFFFF, 14.0f * effectiveAlpha));
        Render2D.outline(lastCancelX, lastCancelY, lastCancelW, lastCancelH, 4.0f, 0.6f, ThemeManager.rgba(0xFFFFFF, 18.0f * effectiveAlpha));
        float cW = Fonts.MONTSERRAT_MEDIUM.width(tr("Отмена", "Cancel"), 5.5f);
        Fonts.MONTSERRAT_MEDIUM.draw(tr("Отмена", "Cancel"), lastCancelX + (lastCancelW - cW) * 0.5f, lastCancelY + 4.2f, 5.5f, color(255, 255, 255, cancelHov ? 255 : 200, effectiveAlpha));

        boolean saveHov = mx >= lastSaveX && mx <= lastSaveX + lastSaveW && my >= lastSaveY && my <= lastSaveY + lastSaveH;
        int saveBg = saveHov ? ClientAccent.accent(245.0f * effectiveAlpha) : ClientAccent.accent(200.0f * effectiveAlpha);
        Render2D.rect(lastSaveX, lastSaveY, lastSaveW, lastSaveH, 4.0f, saveBg);
        if (saveHov) {
            Render2D.glow(new BuiltGlow(lastSaveX, lastSaveY, lastSaveW, lastSaveH, new float[]{4.0f, 4.0f, 4.0f, 4.0f}, ClientAccent.accent(255.0f * effectiveAlpha), 0.35f, 4.0f, effectiveAlpha));
        }
        float sW = Fonts.MONTSERRAT_MEDIUM.width(tr("Сохранить", "Save"), 5.5f);
        Fonts.MONTSERRAT_MEDIUM.draw(tr("Сохранить", "Save"), lastSaveX + (lastSaveW - sW) * 0.5f, lastSaveY + 4.2f, 5.5f, color(255, 255, 255, 255, effectiveAlpha));

        float bodyY = y + HEADER_HEIGHT + 3.0f;
        float bodyH = h - HEADER_HEIGHT - FOOTER_HEIGHT - 6.0f;
        float bodyX = x + 8.0f;
        float bodyW = w - 16.0f;

        float scrollSpeed = 1.0f - (float) Math.exp(-dt * 14.0f);
        this.drawerScroll += (this.drawerScrollTarget - this.drawerScroll) * scrollSpeed;
        if (Math.abs(this.drawerScrollTarget - this.drawerScroll) < 0.05f) {
            this.drawerScroll = this.drawerScrollTarget;
        }

        Render2D.pushScissor(drawContext, bodyX - 2.0f, bodyY, bodyW + 4.0f, bodyH);

        float curY = bodyY + 4.0f - this.drawerScroll;

        Fonts.MONTSERRAT_MEDIUM.draw(tr("Название пресета", "Preset name"), bodyX + 1.0f, curY, 5.5f, ThemeManager.rgba(0xa0a5b9, 190.0f * effectiveAlpha));
        curY += 8.5f;

        this.lastNameBoxX = bodyX;
        this.lastNameBoxY = curY;
        this.lastNameBoxW = bodyW;
        this.lastNameBoxH = 19.0f;

        boolean hoverName = mx >= lastNameBoxX && mx <= lastNameBoxX + lastNameBoxW && my >= lastNameBoxY && my <= lastNameBoxY + lastNameBoxH;
        float f13 = 1.0f - (float) Math.exp(-dt * 16.0f);
        float f14 = 1.0f - (float) Math.exp(-dt * 11.0f);
        this.nameHoverT += ((hoverName ? 1.0f : 0.0f) - this.nameHoverT) * f13;
        this.nameFocusT += ((this.nameFocused ? 1.0f : 0.0f) - this.nameFocusT) * f14;

        Render2D.rect(lastNameBoxX, lastNameBoxY, lastNameBoxW, lastNameBoxH, 3.5f, ThemeManager.rgba(0x06080b, (180.0f + 40.0f * this.nameFocusT) * effectiveAlpha));
        if (this.nameFocusT > 0.01f) {
            Render2D.glow(new BuiltGlow(lastNameBoxX, lastNameBoxY, lastNameBoxW, lastNameBoxH, new float[]{3.5f, 3.5f, 3.5f, 3.5f}, ClientAccent.accent(255.0f), 0.28f, 4.0f, this.nameFocusT * effectiveAlpha));
        }
        int baseOutline = ThemeManager.rgba(0xFFFFFF, (18.0f + 14.0f * this.nameHoverT) * effectiveAlpha);
        int focusOutline = ClientAccent.accent((160.0f + 95.0f * this.nameFocusT) * effectiveAlpha);
        Render2D.outline(lastNameBoxX, lastNameBoxY, lastNameBoxW, lastNameBoxH, 3.5f, 0.6f, ColorUtil.lerpColor(baseOutline, focusOutline, this.nameFocusT));

        float textStartX = lastNameBoxX + 6.0f;
        float textVisibleW = lastNameBoxW - 12.0f;
        boolean hasName = !this.drawerName.isEmpty();

        Render2D.pushScissor(drawContext, textStartX - 1.0f, lastNameBoxY, textVisibleW + 2.0f, lastNameBoxH);
        float step = dt * 3.6f;
        boolean hasStagger = false;
        for (int i = 0; i < this.nameCharAnim.size(); i++) {
            float val = this.nameCharAnim.get(i);
            if (val < 1.0f) {
                this.nameCharAnim.set(i, Math.min(1.0f, val + step));
                hasStagger = true;
            }
        }

        if (hasName) {
            float textW = Fonts.MONTSERRAT_MEDIUM.width(this.drawerName, 5.5f);
            if (this.nameSelectedAll) {
                Render2D.rect(textStartX - 1.0f, lastNameBoxY + 2.5f, textW + 2.0f, lastNameBoxH - 5.0f, 2.0f, ClientAccent.accent(110.0f * effectiveAlpha));
            }
            if (!hasStagger) {
                Fonts.MONTSERRAT_MEDIUM.draw(this.drawerName, textStartX, lastNameBoxY + 4.2f, 5.5f, color(255, 255, 255, 240, effectiveAlpha));
            } else {
                float pen = textStartX;
                for (int i = 0; i < this.drawerName.length(); i++) {
                    String glyph = String.valueOf(this.drawerName.charAt(i));
                    float prog = i < this.nameCharAnim.size() ? Math.max(0.0f, Math.min(1.0f, this.nameCharAnim.get(i))) : 1.0f;
                    float eased = 1.0f - (float) Math.pow(1.0f - prog, 3);
                    int op = Math.max(0, Math.min(255, Math.round(240.0f * effectiveAlpha * eased)));
                    if (op > 0) {
                        Fonts.MONTSERRAT_MEDIUM.draw(glyph, pen, lastNameBoxY + 4.2f + 1.5f * (1.0f - eased), 5.5f, color(255, 255, 255, op, effectiveAlpha));
                    }
                    pen += Fonts.MONTSERRAT_MEDIUM.width(glyph, 5.5f);
                }
            }
        } else {
            Fonts.MONTSERRAT_MEDIUM.draw(tr("Например: Ranked PVP", "e.g. Ranked PVP"), textStartX, lastNameBoxY + 4.2f, 5.5f, color(255, 255, 255, Math.round(90.0f + 30.0f * this.nameHoverT), effectiveAlpha));
        }

        float cursorTargetX = Fonts.MONTSERRAT_MEDIUM.width(this.drawerName, 5.5f);
        if (this.nameCursorSnap || !this.nameFocused) {
            this.nameAnimCursorX = cursorTargetX;
            this.nameCursorSnap = false;
        } else {
            this.nameAnimCursorX += (cursorTargetX - this.nameAnimCursorX) * (1.0f - (float) Math.exp(-dt * 20.0f));
        }

        if (this.nameFocusT > 0.01f) {
            float pulse = (float) (Math.sin((double) System.currentTimeMillis() / 200.0) * 0.5 + 0.5);
            float cx = textStartX + this.nameAnimCursorX;
            Render2D.rect(cx, lastNameBoxY + 3.0f, 0.7f, lastNameBoxH - 6.0f, 0.0f, ClientAccent.accent((70.0f + 185.0f * pulse) * this.nameFocusT * effectiveAlpha));
        }
        Render2D.popScissor(drawContext);
        curY += lastNameBoxH + 9.0f;

        Fonts.MONTSERRAT_MEDIUM.draw(tr("Базовые настройки", "Base settings"), bodyX + 1.0f, curY, 5.5f, ThemeManager.rgba(0xa0a5b9, 190.0f * effectiveAlpha));
        curY += 8.5f;

        this.lastPillW = (bodyW - 4.0f) * 0.5f;
        this.lastPillH = 17.0f;
        this.lastPill1X = bodyX;
        this.lastPill1Y = curY;
        this.lastPill2X = bodyX + lastPillW + 4.0f;
        this.lastPill2Y = curY;

        boolean actCur = this.drawerTemplate == LocalPresets.Template.CURRENT;
        boolean actDef = this.drawerTemplate == LocalPresets.Template.DEFAULTS;
        boolean hovP1 = mx >= lastPill1X && mx <= lastPill1X + lastPillW && my >= lastPill1Y && my <= lastPill1Y + lastPillH;
        boolean hovP2 = mx >= lastPill2X && mx <= lastPill2X + lastPillW && my >= lastPill2Y && my <= lastPill2Y + lastPillH;

        Render2D.rect(lastPill1X, lastPill1Y, lastPillW, lastPillH, 3.5f, actCur ? ClientAccent.accent(190.0f * effectiveAlpha) : (hovP1 ? ThemeManager.rgba(0xFFFFFF, 24.0f * effectiveAlpha) : ThemeManager.rgba(0xFFFFFF, 12.0f * effectiveAlpha)));
        Render2D.outline(lastPill1X, lastPill1Y, lastPillW, lastPillH, 3.5f, 0.6f, actCur ? ClientAccent.accent(240.0f * effectiveAlpha) : ThemeManager.rgba(0xFFFFFF, 18.0f * effectiveAlpha));
        float p1w = Fonts.MONTSERRAT_MEDIUM.width(tr("Текущие", "Current"), 5.0f);
        Fonts.MONTSERRAT_MEDIUM.draw(tr("Текущие", "Current"), lastPill1X + (lastPillW - p1w) * 0.5f, lastPill1Y + 4.0f, 5.0f, color(255, 255, 255, actCur ? 255 : 180, effectiveAlpha));

        Render2D.rect(lastPill2X, lastPill2Y, lastPillW, lastPillH, 3.5f, actDef ? ClientAccent.accent(190.0f * effectiveAlpha) : (hovP2 ? ThemeManager.rgba(0xFFFFFF, 24.0f * effectiveAlpha) : ThemeManager.rgba(0xFFFFFF, 12.0f * effectiveAlpha)));
        Render2D.outline(lastPill2X, lastPill2Y, lastPillW, lastPillH, 3.5f, 0.6f, actDef ? ClientAccent.accent(240.0f * effectiveAlpha) : ThemeManager.rgba(0xFFFFFF, 18.0f * effectiveAlpha));
        float p2w = Fonts.MONTSERRAT_MEDIUM.width(tr("По умолчанию", "Default"), 5.0f);
        Fonts.MONTSERRAT_MEDIUM.draw(tr("По умолчанию", "Default"), lastPill2X + (lastPillW - p2w) * 0.5f, lastPill2Y + 4.0f, 5.0f, color(255, 255, 255, actDef ? 255 : 180, effectiveAlpha));
        curY += lastPillH + 9.0f;

        Fonts.MONTSERRAT_MEDIUM.draw(tr("Включить в пресет", "Include in preset"), bodyX + 1.0f, curY, 5.5f, ThemeManager.rgba(0xa0a5b9, 190.0f * effectiveAlpha));
        LocalPresets.Part[] partsList = new LocalPresets.Part[]{
            LocalPresets.Part.MODULES, LocalPresets.Part.BINDS, LocalPresets.Part.HUD, LocalPresets.Part.THEMES, LocalPresets.Part.SOUNDS, LocalPresets.Part.CART_PROFILE, LocalPresets.Part.MENU
        };
        String[] partsLabels = new String[]{
            tr("Модули и параметры", "Modules & settings"),
            tr("Бинды и клавиши", "Keybinds"),
            tr("Позиции HUD элементов", "HUD positions"),
            tr("Цветовая тема", "Color theme"),
            tr("Звуки клиента", "Client sounds"),
            tr("Профиль авто-тележки", "Auto-Cart profile"),
            tr("Настройки меню", "Menu settings")
        };

        boolean allSelected = true;
        for (LocalPresets.Part p : partsList) {
            if (!this.drawerParts.contains(p)) {
                allSelected = false;
                break;
            }
        }
        String toggleAllStr = allSelected ? tr("Снять всё", "Deselect all") : tr("Выбрать всё", "Select all");
        this.lastToggleAllW = Fonts.MONTSERRAT_MEDIUM.width(toggleAllStr, 5.0f);
        this.lastToggleAllX = bodyX + bodyW - this.lastToggleAllW - 2.0f;
        this.lastToggleAllY = curY;
        boolean hovToggle = mx >= lastToggleAllX - 2.0f && mx <= lastToggleAllX + lastToggleAllW + 2.0f && my >= curY - 1.0f && my <= curY + 9.0f;
        Fonts.MONTSERRAT_MEDIUM.draw(toggleAllStr, lastToggleAllX, curY, 5.0f, hovToggle ? ClientAccent.accentBright(255.0f * effectiveAlpha) : ThemeManager.rgba(0xa0a5b9, 160.0f * effectiveAlpha));
        curY += 8.5f;

        for (int i = 0; i < partsList.length; i++) {
            LocalPresets.Part p = partsList[i];
            boolean checked = this.drawerParts.contains(p);
            float rowY = curY;
            this.lastPartRowY[i] = rowY;
            float cbSize = 8.5f;
            float cbX = bodyX + 3.0f;
            float cbY = rowY + 1.5f;

            boolean hoverRow = mx >= bodyX && mx <= bodyX + bodyW && my >= rowY && my <= rowY + 14.0f;
            if (hoverRow) {
                Render2D.rect(bodyX, rowY - 1.0f, bodyW, 14.0f, 3.0f, ThemeManager.rgba(0xFFFFFF, 8.0f * effectiveAlpha));
            }

            Render2D.rect(cbX, cbY, cbSize, cbSize, 2.0f, checked ? ClientAccent.accent(220.0f * effectiveAlpha) : ThemeManager.rgba(0x0a0c10, 200.0f * effectiveAlpha));
            Render2D.outline(cbX, cbY, cbSize, cbSize, 2.0f, 0.6f, checked ? ClientAccent.accent(255.0f * effectiveAlpha) : ThemeManager.rgba(0xFFFFFF, 25.0f * effectiveAlpha));
            if (checked) {
                Fonts.NV.msdf(NvIcons.CHECK, cbX + 1.5f, cbY + 1.5f, 5.5f, color(255, 255, 255, 255, effectiveAlpha));
            }

            Fonts.MONTSERRAT_MEDIUM.draw(partsLabels[i], cbX + cbSize + 6.0f, rowY + 2.0f, 5.0f, color(255, 255, 255, checked ? 240 : 160, effectiveAlpha));
            curY += 15.0f;
        }

        if (this.drawerParts.contains(LocalPresets.Part.MODULES)) {
            curY += 3.0f;
            Fonts.MONTSERRAT_MEDIUM.draw(tr("Выбор функционала", "Select features"), bodyX + 1.0f, curY, 5.2f, ThemeManager.rgba(0xa0a5b9, 190.0f * effectiveAlpha));
            boolean allModsSelected = this.drawerModules.size() == ALL_FEATURE_MODULES.size();
            String toggleAllModsStr = allModsSelected ? tr("Снять всё", "Deselect all") : tr("Выбрать всё", "Select all");
            this.lastToggleAllModsW = Fonts.MONTSERRAT_MEDIUM.width(toggleAllModsStr, 4.8f);
            this.lastToggleAllModsX = bodyX + bodyW - this.lastToggleAllModsW - 2.0f;
            this.lastToggleAllModsY = curY;
            boolean hovToggleMods = mx >= lastToggleAllModsX - 2.0f && mx <= lastToggleAllModsX + lastToggleAllModsW + 2.0f && my >= curY - 1.0f && my <= curY + 8.0f;
            Fonts.MONTSERRAT_MEDIUM.draw(toggleAllModsStr, lastToggleAllModsX, curY, 4.8f, hovToggleMods ? ClientAccent.accentBright(255.0f * effectiveAlpha) : ThemeManager.rgba(0xa0a5b9, 160.0f * effectiveAlpha));
            curY += 8.0f;

            for (int i = 0; i < ALL_FEATURE_MODULES.size(); i++) {
                String modId = ALL_FEATURE_MODULES.get(i);
                boolean modChecked = this.drawerModules.contains(modId);
                float rowY = curY;
                this.lastModRowY[i] = rowY;
                float cbSize = 7.5f;
                float cbX = bodyX + 5.0f;
                float cbY = rowY + 1.5f;

                boolean hoverRow = mx >= bodyX && mx <= bodyX + bodyW && my >= rowY && my <= rowY + 13.0f;
                if (hoverRow) {
                    Render2D.rect(bodyX, rowY - 1.0f, bodyW, 13.0f, 2.5f, ThemeManager.rgba(0xFFFFFF, 8.0f * effectiveAlpha));
                }

                Render2D.rect(cbX, cbY, cbSize, cbSize, 2.0f, modChecked ? ClientAccent.accent(220.0f * effectiveAlpha) : ThemeManager.rgba(0x0a0c10, 200.0f * effectiveAlpha));
                Render2D.outline(cbX, cbY, cbSize, cbSize, 2.0f, 0.6f, modChecked ? ClientAccent.accent(255.0f * effectiveAlpha) : ThemeManager.rgba(0xFFFFFF, 25.0f * effectiveAlpha));
                if (modChecked) {
                    Fonts.NV.msdf(NvIcons.CHECK, cbX + 1.2f, cbY + 1.2f, 5.0f, color(255, 255, 255, 255, effectiveAlpha));
                }

                Fonts.MONTSERRAT_MEDIUM.draw(getModuleLabel(modId), cbX + cbSize + 5.0f, rowY + 1.8f, 4.8f, color(255, 255, 255, modChecked ? 240 : 150, effectiveAlpha));
                curY += 13.5f;
            }
        }

        curY += 4.0f;
        this.lastAutoActY = curY;
        boolean hoverAuto = mx >= bodyX && mx <= bodyX + bodyW && my >= curY && my <= curY + 14.0f;
        if (hoverAuto) {
            Render2D.rect(bodyX, curY - 1.0f, bodyW, 14.0f, 3.0f, ThemeManager.rgba(0xFFFFFF, 8.0f * effectiveAlpha));
        }
        float aX = bodyX + 3.0f;
        float aY = curY + 1.5f;
        Render2D.rect(aX, aY, 8.5f, 8.5f, 2.0f, this.autoActivate ? ClientAccent.accent(220.0f * effectiveAlpha) : ThemeManager.rgba(0x0a0c10, 200.0f * effectiveAlpha));
        Render2D.outline(aX, aY, 8.5f, 8.5f, 2.0f, 0.6f, this.autoActivate ? ClientAccent.accent(255.0f * effectiveAlpha) : ThemeManager.rgba(0xFFFFFF, 25.0f * effectiveAlpha));
        if (this.autoActivate) {
            Fonts.NV.msdf(NvIcons.CHECK, aX + 1.5f, aY + 1.5f, 5.5f, color(255, 255, 255, 255, effectiveAlpha));
        }
        Fonts.MONTSERRAT_MEDIUM.draw(tr("Активировать сразу", "Activate on create"), aX + 14.5f, curY + 2.0f, 5.0f, color(255, 255, 255, this.autoActivate ? 240 : 160, effectiveAlpha));
        curY += 18.0f;

        float totalH = curY - bodyY + this.drawerScroll;
        float maxDrawerScroll = Math.max(0.0f, totalH - bodyH);
        this.drawerScrollTarget = Math.max(0.0f, Math.min(this.drawerScrollTarget, maxDrawerScroll));

        Render2D.popScissor(drawContext);
    }

    private void renderToast(DrawContext drawContext, float x, float y, float w, float h, float alpha) {
        if (this.toastMessage == null || this.toastTime == 0) return;
        long elapsed = System.currentTimeMillis() - this.toastTime;
        if (elapsed > 2400L) {
            this.toastMessage = null;
            return;
        }

        float toastProgress = (float) elapsed / 2400.0f;
        float toastAlpha = toastProgress < 0.15f ? (toastProgress / 0.15f) : (toastProgress > 0.85f ? (1.0f - toastProgress) / 0.15f : 1.0f);
        float totalAlpha = alpha * toastAlpha;

        float textW = Fonts.MONTSERRAT_MEDIUM.width(this.toastMessage, 6.0f);
        float toastW = textW + 28.0f;
        float toastH = 17.0f;
        float toastX = x + (w - toastW) * 0.5f;
        float toastY = y + h - toastH - 8.0f;

        Render2D.rect(toastX, toastY, toastW, toastH, 8.5f, ThemeManager.rgba(0x0e1117, 240.0f * totalAlpha));
        Render2D.outline(toastX, toastY, toastW, toastH, 8.5f, 0.6f, ClientAccent.accent(180.0f * totalAlpha));
        Fonts.NV.msdf(NvIcons.CHECK, toastX + 6.0f, toastY + 5.0f, 6.5f, ClientAccent.accentBright(255.0f * totalAlpha));
        Fonts.MONTSERRAT_MEDIUM.draw(this.toastMessage, toastX + 16.5f, toastY + 5.0f, 6.0f, color(255, 255, 255, 255, totalAlpha));
    }

    public boolean click(float mx, float my, int button) {
        if (button != 0) return false;

        float x = this.lastX;
        float y = this.lastY;
        float w = this.lastW;
        float h = this.lastH;

        if (mx >= lastImpX && mx <= lastImpX + lastImpW && my >= lastImpY && my <= lastImpY + lastImpH) {
            this.importPresetFromClipboard();
            Sounds.play("buttonclick");
            return true;
        }

        if (mx >= lastCreateX && mx <= lastCreateX + lastCreateW && my >= lastCreateY && my <= lastCreateY + lastCreateH) {
            this.openDrawer();
            return true;
        }

        float headerH = 26.0f;
        float cardsStartY = y + headerH + 5.0f;
        float cardsH = h - headerH - 8.0f;

        float contentW = Math.min(276.0f, w - 24.0f);
        float contentX = x + (w - contentW) * 0.5f;

        List<LocalPresets.Entry> filtered = new ArrayList<>();
        String query = UI.INSTANCE != null ? UI.INSTANCE.getSearchText().trim().toLowerCase(Locale.ROOT) : "";
        for (LocalPresets.Entry e : this.entries) {
            if (query.isEmpty() || e.name().toLowerCase(Locale.ROOT).contains(query)) {
                filtered.add(e);
            }
        }

        if (mx < contentX || mx > contentX + contentW || my < cardsStartY || my > cardsStartY + cardsH) {
            return false;
        }

        float gap = 6.0f;
        float cardW = (contentW - gap) * 0.5f;
        float cardH = 50.0f;

        for (int i = 0; i < filtered.size(); i++) {
            LocalPresets.Entry entry = filtered.get(i);
            int col = i % 2;
            int row = i / 2;
            float cardX = contentX + (float) col * (cardW + gap);
            float cardY = cardsStartY + (float) row * (cardH + gap) - this.scroll;

            if (cardY + cardH < cardsStartY || cardY > cardsStartY + cardsH) {
                continue;
            }

            if (mx >= cardX && mx <= cardX + cardW && my >= cardY && my <= cardY + cardH) {
                float favBtnW = 12.0f;
                float favBtnH = 12.0f;
                float favX = cardX + cardW - 14.0f;
                float favY = cardY + 6.0f;
                if (mx >= favX - 2.0f && mx <= favX + favBtnW + 2.0f && my >= favY - 2.0f && my <= favY + favBtnH + 2.0f) {
                    try {
                        LocalPresets.toggleFavorite(entry.path());
                        this.reload();
                        Sounds.play("click");
                    } catch (Exception ignored) {}
                    return true;
                }

                float actionH = 11.5f;
                float actionY = cardY + cardH - actionH - 5.0f;

                boolean isDeletingThis = this.deletingPath != null && this.deletingPath.equals(entry.path());
                float delW = isDeletingThis ? 36.0f : 14.0f;
                float delX = cardX + cardW - delW - 6.0f;

                float expW = 14.0f;
                float expX = delX - expW - 4.0f;

                if (mx >= delX && mx <= delX + delW && my >= actionY && my <= actionY + actionH) {
                    if (isDeletingThis) {
                        try {
                            LocalPresets.delete(entry.path());
                            this.deletingPath = null;
                            this.reload();
                            this.showToast(tr("Пресет удалён", "Preset deleted"));
                            Sounds.play("click");
                        } catch (Exception e) {
                            this.showToast(tr("Не удалось удалить", "Delete failed"));
                        }
                    } else {
                        this.deletingPath = entry.path();
                        this.deletePromptTime = System.currentTimeMillis();
                        Sounds.play("buttonclick");
                    }
                    return true;
                }

                if (mx >= expX && mx <= expX + expW && my >= actionY && my <= actionY + actionH) {
                    try {
                        Path exp = LocalPresets.export(entry.path());
                        String json = LocalPresets.exportString(entry.path());
                        MinecraftClient.getInstance().keyboard.setClipboard(json != null && !json.isEmpty() ? json : exp.toAbsolutePath().toString());
                        this.showToast(tr("Пресет скопирован в буфер", "Preset copied to clipboard"));
                        Sounds.play("buttonclick");
                    } catch (Exception e) {
                        this.showToast(tr("Ошибка экспорта", "Export error"));
                    }
                    return true;
                }

                UI.INSTANCE.applyPreset(entry);
                this.showToast(tr("Пресет '" + entry.name() + "' применён", "Preset '" + entry.name() + "' applied"));
                return true;
            }
        }

        return false;
    }

    public boolean drawerMouseClicked(float mx, float my, int button) {
        if (button != 0 || !this.isDrawerOpen()) return false;

        float x = this.lastDrawerX;
        float y = this.lastDrawerY;
        float w = this.lastDrawerW;
        float h = this.lastDrawerH;

        if (mx < x || mx > x + w || my < y || my > y + h) {
            return false;
        }

        if (mx >= lastCloseX - 3.0f && mx <= lastCloseX + lastCloseW + 3.0f && my >= lastCloseY - 3.0f && my <= lastCloseY + lastCloseH + 3.0f) {
            this.closeDrawer();
            return true;
        }

        if (mx >= lastCancelX && mx <= lastCancelX + lastCancelW && my >= lastCancelY && my <= lastCancelY + lastCancelH) {
            this.closeDrawer();
            return true;
        }

        if (mx >= lastSaveX && mx <= lastSaveX + lastSaveW && my >= lastSaveY && my <= lastSaveY + lastSaveH) {
            this.saveDrawerPreset();
            return true;
        }

        if (mx >= lastNameBoxX && mx <= lastNameBoxX + lastNameBoxW && my >= lastNameBoxY && my <= lastNameBoxY + lastNameBoxH) {
            this.nameFocused = true;
            this.nameSelectedAll = false;
            this.nameCursorSnap = true;
            return true;
        } else if (this.nameFocused) {
            this.nameFocused = false;
            this.nameSelectedAll = false;
        }

        if (mx >= lastPill1X && mx <= lastPill1X + lastPillW && my >= lastPill1Y && my <= lastPill1Y + lastPillH) {
            this.drawerTemplate = LocalPresets.Template.CURRENT;
            Sounds.play("buttonclick");
            return true;
        }
        if (mx >= lastPill2X && mx <= lastPill2X + lastPillW && my >= lastPill2Y && my <= lastPill2Y + lastPillH) {
            this.drawerTemplate = LocalPresets.Template.DEFAULTS;
            Sounds.play("buttonclick");
            return true;
        }

        float bodyY = y + HEADER_HEIGHT + 3.0f;
        float bodyH = h - HEADER_HEIGHT - FOOTER_HEIGHT - 6.0f;

        if (my >= bodyY && my <= bodyY + bodyH) {
            LocalPresets.Part[] partsList = new LocalPresets.Part[]{
                LocalPresets.Part.MODULES, LocalPresets.Part.BINDS, LocalPresets.Part.HUD, LocalPresets.Part.THEMES, LocalPresets.Part.SOUNDS, LocalPresets.Part.CART_PROFILE, LocalPresets.Part.MENU
            };
            if (mx >= lastToggleAllX - 3.0f && mx <= lastToggleAllX + lastToggleAllW + 3.0f && my >= lastToggleAllY - 2.0f && my <= lastToggleAllY + 11.0f) {
                boolean allSelected = true;
                for (LocalPresets.Part p : partsList) {
                    if (!this.drawerParts.contains(p)) {
                        allSelected = false;
                        break;
                    }
                }
                if (allSelected) {
                    this.drawerParts.clear();
                } else {
                    this.drawerParts.addAll(Arrays.asList(partsList));
                }
                Sounds.play("buttonclick");
                return true;
            }

            for (int i = 0; i < partsList.length; i++) {
                float rowY = this.lastPartRowY[i];
                if (mx >= lastNameBoxX && mx <= lastNameBoxX + lastNameBoxW && my >= rowY && my <= rowY + 14.0f) {
                    LocalPresets.Part p = partsList[i];
                    if (this.drawerParts.contains(p)) {
                        this.drawerParts.remove(p);
                    } else {
                        this.drawerParts.add(p);
                    }
                    Sounds.play("buttonclick");
                    return true;
                }
            }

            if (this.drawerParts.contains(LocalPresets.Part.MODULES)) {
                if (mx >= lastToggleAllModsX - 3.0f && mx <= lastToggleAllModsX + lastToggleAllModsW + 3.0f && my >= lastToggleAllModsY - 2.0f && my <= lastToggleAllModsY + 10.0f) {
                    if (this.drawerModules.size() == ALL_FEATURE_MODULES.size()) {
                        this.drawerModules.clear();
                    } else {
                        this.drawerModules.addAll(ALL_FEATURE_MODULES);
                    }
                    Sounds.play("buttonclick");
                    return true;
                }

                for (int i = 0; i < ALL_FEATURE_MODULES.size(); i++) {
                    float rowY = this.lastModRowY[i];
                    if (mx >= lastNameBoxX && mx <= lastNameBoxX + lastNameBoxW && my >= rowY && my <= rowY + 13.0f) {
                        String modId = ALL_FEATURE_MODULES.get(i);
                        if (this.drawerModules.contains(modId)) {
                            this.drawerModules.remove(modId);
                        } else {
                            this.drawerModules.add(modId);
                        }
                        Sounds.play("buttonclick");
                        return true;
                    }
                }
            }

            if (mx >= lastNameBoxX && mx <= lastNameBoxX + lastNameBoxW && my >= lastAutoActY && my <= lastAutoActY + 14.0f) {
                this.autoActivate = !this.autoActivate;
                Sounds.play("buttonclick");
                return true;
            }
        }

        return true;
    }

    public boolean drawerMouseReleased(double mx, double my, int button) {
        return false;
    }

    public boolean drawerMouseScrolled(double horiz, double vert) {
        if (!this.isDrawerOpen()) return false;
        float mx = Position.mouseX();
        float my = Position.mouseY();
        if (mx >= lastDrawerX && mx <= lastDrawerX + lastDrawerW && my >= lastDrawerY && my <= lastDrawerY + lastDrawerH) {
            this.drawerScrollTarget -= (float) (vert * 18.0);
            return true;
        }
        return false;
    }

    public boolean drawerKeyPressed(KeyInput input) {
        if (!this.isDrawerOpen()) return false;

        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.closeDrawer();
            return true;
        }

        if (this.nameFocused) {
            long handle = MinecraftClient.getInstance().getWindow().getHandle();
            boolean ctrl = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == 1
                    || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == 1;

            if (ctrl && input.key() == GLFW.GLFW_KEY_A) {
                if (!this.drawerName.isEmpty()) {
                    this.nameSelectedAll = true;
                }
                return true;
            }
            if (ctrl && input.key() == GLFW.GLFW_KEY_C) {
                if (this.nameSelectedAll && !this.drawerName.isEmpty()) {
                    MinecraftClient.getInstance().keyboard.setClipboard(this.drawerName);
                }
                return true;
            }
            if (ctrl && input.key() == GLFW.GLFW_KEY_X) {
                if (this.nameSelectedAll && !this.drawerName.isEmpty()) {
                    MinecraftClient.getInstance().keyboard.setClipboard(this.drawerName);
                    this.drawerName = "";
                    this.nameCharAnim.clear();
                    this.nameSelectedAll = false;
                }
                return true;
            }
            if (ctrl && input.key() == GLFW.GLFW_KEY_V) {
                String clip = MinecraftClient.getInstance().keyboard.getClipboard();
                if (clip != null && !clip.isEmpty()) {
                    String clean = clip.replaceAll("[\\r\\n]", "").trim();
                    if (this.nameSelectedAll) {
                        this.drawerName = "";
                        this.nameCharAnim.clear();
                        this.nameSelectedAll = false;
                    }
                    int rem = 24 - this.drawerName.length();
                    if (rem > 0) {
                        String add = clean.length() > rem ? clean.substring(0, rem) : clean;
                        this.drawerName = this.drawerName + add;
                        for (int i = 0; i < add.length(); i++) {
                            this.nameCharAnim.add(Float.valueOf(1.0f));
                        }
                    }
                    Sounds.play("search_typing");
                }
                return true;
            }

            if (input.key() == GLFW.GLFW_KEY_BACKSPACE || input.key() == GLFW.GLFW_KEY_DELETE) {
                if (this.nameSelectedAll) {
                    this.drawerName = "";
                    this.nameCharAnim.clear();
                    this.nameSelectedAll = false;
                    Sounds.play("search_typing");
                    return true;
                }
                if (!this.drawerName.isEmpty()) {
                    this.drawerName = this.drawerName.substring(0, this.drawerName.length() - 1);
                    if (!this.nameCharAnim.isEmpty()) {
                        this.nameCharAnim.remove(this.nameCharAnim.size() - 1);
                    }
                    Sounds.play("search_typing");
                    return true;
                }
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) {
                this.nameFocused = false;
                this.nameSelectedAll = false;
                this.saveDrawerPreset();
                return true;
            }
            return true;
        }

        if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) {
            this.saveDrawerPreset();
            return true;
        }

        return false;
    }

    public boolean drawerCharTyped(CharInput input) {
        if (!this.isDrawerOpen() || !this.nameFocused) return false;
        int codepoint = input.codepoint();
        if (Character.isISOControl(codepoint)) return false;
        char c = (char) codepoint;

        if (this.nameSelectedAll) {
            this.drawerName = "";
            this.nameCharAnim.clear();
            this.nameSelectedAll = false;
        }
        if (this.drawerName.length() < 24) {
            this.drawerName += c;
            this.nameCharAnim.add(Float.valueOf(-0.14f));
            Sounds.play("search_typing");
        }
        return true;
    }

    private void saveDrawerPreset() {
        String name = this.drawerName.trim();
        if (name.isEmpty()) {
            this.showToast(tr("Введите название пресета!", "Enter a preset name!"));
            Sounds.play("command_error");
            return;
        }
        if (this.drawerParts.isEmpty()) {
            this.showToast(tr("Выберите хотя бы один раздел!", "Select at least one section!"));
            Sounds.play("command_error");
            return;
        }
        try {
            Path file = LocalPresets.create(name, this.drawerTemplate, this.drawerParts, this.drawerParts.contains(LocalPresets.Part.MODULES) ? this.drawerModules : null);
            this.reload();
            if (this.autoActivate) {
                for (LocalPresets.Entry e : this.entries) {
                    if (e.name().equalsIgnoreCase(name)) {
                        UI.INSTANCE.applyPreset(e);
                        break;
                    }
                }
            }
            this.closeDrawer();
            this.showToast(tr("Пресет '" + name + "' сохранён!", "Preset '" + name + "' saved!"));
            Sounds.play("select_category");
        } catch (Exception e) {
            this.showToast(tr("Ошибка: " + e.getMessage(), "Error: " + e.getMessage()));
            Sounds.play("command_error");
        }
    }

    private void importPresetFromClipboard() {
        try {
            String clip = MinecraftClient.getInstance().keyboard.getClipboard();
            if (clip == null || clip.trim().isEmpty()) {
                this.showToast(tr("Буфер обмена пуст!", "Clipboard is empty!"));
                Sounds.play("command_error");
                return;
            }
            clip = clip.trim();
            if (clip.startsWith("\"") && clip.endsWith("\"")) {
                clip = clip.substring(1, clip.length() - 1).trim();
            }
            if (clip.startsWith("{") && clip.endsWith("}")) {
                LocalPresets.importString(clip);
                this.reload();
                this.showToast(tr("Пресет импортирован!", "Preset imported!"));
                Sounds.play("select_category");
                return;
            }
            try {
                Path path = Path.of(clip);
                LocalPresets.importFile(path);
                this.reload();
                this.showToast(tr("Пресет импортирован!", "Preset imported!"));
                Sounds.play("select_category");
            } catch (Exception ex) {
                LocalPresets.importString(clip);
                this.reload();
                this.showToast(tr("Пресет импортирован!", "Preset imported!"));
                Sounds.play("select_category");
            }
        } catch (Exception e) {
            this.showToast(tr("Не удалось импортировать", "Import failed"));
            Sounds.play("command_error");
        }
    }

    public void scroll(double verticalAmount) {
        this.scrollTarget -= (float) (verticalAmount * 24.0);
    }

    public boolean charTyped(CharInput input) {
        return false;
    }

    public boolean keyPressed(KeyInput input) {
        return false;
    }

    public boolean mouseReleased(int button) {
        return false;
    }
}

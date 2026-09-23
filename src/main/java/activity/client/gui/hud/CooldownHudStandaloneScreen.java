package activity.client.gui.hud;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.font.UiTextRenderer;
import activity.client.gui.render.ActivityGuiRenderer;
import activity.client.gui.sound.SoundManager;
import activity.client.gui.theme.ActivityColors;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.impl.utility.CooldownHudModule;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public final class CooldownHudStandaloneScreen extends Screen {

    private final Screen parent;

    private static final int PANEL_W = 320;
    private static final int PANEL_H = 264;
    private int panelX = -1;
    private int panelY = -1;

    private boolean isDragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    private int btnToggleEnabledX, btnToggleEnabledY, btnToggleEnabledW, btnToggleEnabledH;
    private int btnToggleOrientX, btnToggleOrientY, btnToggleOrientW, btnToggleOrientH;
    private int btnStepDecX, btnStepDecY, btnStepDecW, btnStepDecH;
    private int btnStepIncX, btnStepIncY, btnStepIncW, btnStepIncH;
    private int btnOpenEditorX, btnOpenEditorY, btnOpenEditorW, btnOpenEditorH;
    private int btnResetPosX, btnResetPosY, btnResetPosW, btnResetPosH;
    private int btnDoneX, btnDoneY, btnDoneW, btnDoneH;

    private int lastHoveredBtn = -1;

    public CooldownHudStandaloneScreen(Screen parent) {
        super(Text.literal("Cooldown HUD"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        if (panelX < 0 || panelY < 0) {
            panelX = (width - PANEL_W) / 2;
            panelY = (height - PANEL_H) / 2;
        }
        panelX = Math.max(4, Math.min(width - PANEL_W - 4, panelX));
        panelY = Math.max(4, Math.min(height - PANEL_H - 4, panelY));
        SoundManager.playOpen();
    }

    @Override
    public void close() {
        SoundManager.playClose();
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            ActivityConfigManager.save();
        }
        if (this.client != null) {
            this.client.setScreen(this.parent);
        } else {
            super.close();
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) Math.round(click.x());
        int my = (int) Math.round(click.y());
        int button = click.buttonInfo().button();

        if (button == 0) {
            if (mx >= panelX && mx <= panelX + PANEL_W && my >= panelY && my <= panelY + 28) {
                isDragging = true;
                dragOffsetX = mx - panelX;
                dragOffsetY = my - panelY;
                return true;
            }

            if (mx >= btnToggleEnabledX && mx <= btnToggleEnabledX + btnToggleEnabledW &&
                my >= btnToggleEnabledY && my <= btnToggleEnabledY + btnToggleEnabledH) {
                toggleEnabled();
                return true;
            }

            if (mx >= btnToggleOrientX && mx <= btnToggleOrientX + btnToggleOrientW &&
                my >= btnToggleOrientY && my <= btnToggleOrientY + btnToggleOrientH) {
                toggleOrientation();
                return true;
            }

            if (mx >= btnStepDecX && mx <= btnStepDecX + btnStepDecW &&
                my >= btnStepDecY && my <= btnStepDecY + btnStepDecH) {
                adjustDuration(-0.5);
                return true;
            }

            if (mx >= btnStepIncX && mx <= btnStepIncX + btnStepIncW &&
                my >= btnStepIncY && my <= btnStepIncY + btnStepIncH) {
                adjustDuration(0.5);
                return true;
            }

            if (mx >= btnOpenEditorX && mx <= btnOpenEditorX + btnOpenEditorW &&
                my >= btnOpenEditorY && my <= btnOpenEditorY + btnOpenEditorH) {
                SoundManager.playClick();
                if (this.client != null) {
                    this.client.setScreen(new CooldownHudEditorScreen(this));
                }
                return true;
            }

            if (mx >= btnResetPosX && mx <= btnResetPosX + btnResetPosW &&
                my >= btnResetPosY && my <= btnResetPosY + btnResetPosH) {
                resetPosition();
                return true;
            }

            if (mx >= btnDoneX && mx <= btnDoneX + btnDoneW &&
                my >= btnDoneY && my <= btnDoneY + btnDoneH) {
                SoundManager.playClick();
                close();
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.buttonInfo().button() == 0 && isDragging) {
            isDragging = false;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (isDragging) {
            int newX = (int) Math.round(click.x() - dragOffsetX);
            int newY = (int) Math.round(click.y() - dragOffsetY);
            panelX = Math.max(4, Math.min(width - PANEL_W - 4, newX));
            panelY = Math.max(4, Math.min(height - PANEL_H - 4, newY));
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    private void toggleEnabled() {
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            boolean newState = !c.cooldownHudEnabled;
            c.cooldownHudEnabled = newState;
            IModule mod = ModuleRegistry.get(CooldownHudModule.ID);
            if (mod instanceof CooldownHudModule chm) {
                chm.setEnabled(newState);
            }
            ActivityConfigManager.save();
        }
        SoundManager.playClick();
    }

    private void toggleOrientation() {
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            boolean newVertical = !c.cooldownHudVertical;
            c.cooldownHudVertical = newVertical;
            IModule mod = ModuleRegistry.get(CooldownHudModule.ID);
            if (mod instanceof CooldownHudModule chm) {
                chm.vertical = newVertical;
            }
            ActivityConfigManager.save();
        }
        SoundManager.playClick();
    }

    private void adjustDuration(double delta) {
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            double current = c.cooldownHudMinDuration;
            double updated = Math.round((current + delta) * 10.0) / 10.0;
            updated = Math.max(0.5, Math.min(10.0, updated));
            c.cooldownHudMinDuration = updated;
            IModule mod = ModuleRegistry.get(CooldownHudModule.ID);
            if (mod instanceof CooldownHudModule chm) {
                chm.minDuration = updated;
            }
            ActivityConfigManager.save();
        }
        SoundManager.playClick();
    }

    private void resetPosition() {
        ActivityConfig c = ActivityConfigManager.getConfig();
        if (c != null) {
            c.cooldownHudCustomX = -1;
            c.cooldownHudCustomY = -1;
            ActivityConfigManager.save();
        }
        SoundManager.playSelect();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        ActivityGuiRenderer.fill(context, 0, 0, width, height, ActivityColors.BACKGROUND_OVERLAY);

        ActivityConfig config = ActivityConfigManager.getConfig();
        boolean enabled = config != null && config.cooldownHudEnabled;
        boolean vertical = config != null && config.cooldownHudVertical;
        double minDuration = (config != null) ? config.cooldownHudMinDuration : 2.5;

        ActivityGuiRenderer.drawWindowFrame(context, panelX, panelY, PANEL_W, PANEL_H, ActivityColors.WINDOW_BACKGROUND, ActivityColors.BORDER, true);
        ActivityGuiRenderer.fill(context, panelX + 1, panelY + 1, PANEL_W - 2, 28, ActivityColors.HEADER_BACKGROUND);
        ActivityGuiRenderer.drawGlassHighlight(context, panelX, panelY, PANEL_W, PANEL_H, 1.0f);

        if (textRenderer != null) {
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal("Cooldown HUD"), panelX + PANEL_W / 2, panelY + 6, ActivityColors.TEXT_PRIMARY);
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal("Автономные настройки модуля"), panelX + PANEL_W / 2, panelY + 17, ActivityColors.TEXT_MUTED);
        }

        int curY = panelY + 36;
        int leftColX = panelX + 14;
        int rowH = 28;

        btnToggleEnabledW = 100;
        btnToggleEnabledH = 20;
        btnToggleEnabledX = panelX + PANEL_W - 14 - btnToggleEnabledW;
        btnToggleEnabledY = curY + (rowH - btnToggleEnabledH) / 2;

        ActivityGuiRenderer.drawPanel(context, leftColX - 4, curY, PANEL_W - 20, rowH, ActivityColors.PANEL_INNER_BG, ActivityColors.BORDER, false);
        if (textRenderer != null) {
            UiTextRenderer.drawTextWithShadow(context, textRenderer, Text.literal("Отображение HUD"), leftColX + 4, curY + 9, ActivityColors.TEXT_PRIMARY);
        }

        boolean hoverEnabled = mouseX >= btnToggleEnabledX && mouseX <= btnToggleEnabledX + btnToggleEnabledW &&
                               mouseY >= btnToggleEnabledY && mouseY <= btnToggleEnabledY + btnToggleEnabledH;
        int bgEnabled = hoverEnabled ? (enabled ? 0xDD2A4D30 : 0xDD4D2A2A) : (enabled ? 0xDD1B3320 : 0xDD331B1B);
        int borderEnabled = enabled ? 0xFF44CC66 : 0xFFCC4444;
        String textEnabled = enabled ? "ВКЛЮЧЕН" : "ВЫКЛЮЧЕН";
        int colorEnabled = enabled ? 0xFF77FF88 : 0xFFFF7777;
        ActivityGuiRenderer.drawPanel(context, btnToggleEnabledX, btnToggleEnabledY, btnToggleEnabledW, btnToggleEnabledH, bgEnabled, borderEnabled, true);
        if (textRenderer != null) {
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal(textEnabled), btnToggleEnabledX + btnToggleEnabledW / 2, btnToggleEnabledY + 6, colorEnabled);
        }

        curY += rowH + 6;

        btnToggleOrientW = 120;
        btnToggleOrientH = 20;
        btnToggleOrientX = panelX + PANEL_W - 14 - btnToggleOrientW;
        btnToggleOrientY = curY + (rowH - btnToggleOrientH) / 2;

        ActivityGuiRenderer.drawPanel(context, leftColX - 4, curY, PANEL_W - 20, rowH, ActivityColors.PANEL_INNER_BG, ActivityColors.BORDER, false);
        if (textRenderer != null) {
            UiTextRenderer.drawTextWithShadow(context, textRenderer, Text.literal("Ориентация списка"), leftColX + 4, curY + 9, ActivityColors.TEXT_PRIMARY);
        }

        boolean hoverOrient = mouseX >= btnToggleOrientX && mouseX <= btnToggleOrientX + btnToggleOrientW &&
                              mouseY >= btnToggleOrientY && mouseY <= btnToggleOrientY + btnToggleOrientH;
        int bgOrient = hoverOrient ? ActivityColors.BUTTON_SECONDARY_HOVER : ActivityColors.BUTTON_SECONDARY_BG;
        int borderOrient = hoverOrient ? ActivityColors.BORDER_HOVER : ActivityColors.BORDER;
        String textOrient = vertical ? "Вертикальный" : "Горизонтальный";
        ActivityGuiRenderer.drawPanel(context, btnToggleOrientX, btnToggleOrientY, btnToggleOrientW, btnToggleOrientH, bgOrient, borderOrient, true);
        if (textRenderer != null) {
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal(textOrient), btnToggleOrientX + btnToggleOrientW / 2, btnToggleOrientY + 6, ActivityColors.TEXT_PRIMARY);
        }

        curY += rowH + 6;

        btnStepDecW = 20;
        btnStepDecH = 20;
        btnStepIncW = 20;
        btnStepIncH = 20;
        int valWidth = 48;
        btnStepIncX = panelX + PANEL_W - 14 - btnStepIncW;
        btnStepIncY = curY + (rowH - btnStepIncH) / 2;
        int valBoxX = btnStepIncX - 4 - valWidth;
        btnStepDecX = valBoxX - 4 - btnStepDecW;
        btnStepDecY = btnStepIncY;

        ActivityGuiRenderer.drawPanel(context, leftColX - 4, curY, PANEL_W - 20, rowH, ActivityColors.PANEL_INNER_BG, ActivityColors.BORDER, false);
        if (textRenderer != null) {
            UiTextRenderer.drawTextWithShadow(context, textRenderer, Text.literal("Фильтр микро-КД"), leftColX + 4, curY + 5, ActivityColors.TEXT_PRIMARY);
            UiTextRenderer.drawTextWithShadow(context, textRenderer, Text.literal("Игнорировать ванильные КД"), leftColX + 4, curY + 16, ActivityColors.TEXT_MUTED);
        }

        boolean hoverDec = mouseX >= btnStepDecX && mouseX <= btnStepDecX + btnStepDecW &&
                           mouseY >= btnStepDecY && mouseY <= btnStepDecY + btnStepDecH;
        int bgDec = hoverDec ? ActivityColors.BUTTON_SECONDARY_HOVER : ActivityColors.BUTTON_SECONDARY_BG;
        ActivityGuiRenderer.drawPanel(context, btnStepDecX, btnStepDecY, btnStepDecW, btnStepDecH, bgDec, hoverDec ? ActivityColors.BORDER_HOVER : ActivityColors.BORDER, true);
        if (textRenderer != null) {
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal("-"), btnStepDecX + btnStepDecW / 2, btnStepDecY + 6, ActivityColors.TEXT_PRIMARY);
        }

        ActivityGuiRenderer.drawPanel(context, valBoxX, btnStepDecY, valWidth, btnStepDecH, ActivityColors.WINDOW_BACKGROUND, ActivityColors.BORDER, false);
        if (textRenderer != null) {
            String valStr = String.format(Locale.ROOT, "%.1fc", minDuration);
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal(valStr), valBoxX + valWidth / 2, btnStepDecY + 6, ActivityColors.TEXT_PRIMARY);
        }

        boolean hoverInc = mouseX >= btnStepIncX && mouseX <= btnStepIncX + btnStepIncW &&
                           mouseY >= btnStepIncY && mouseY <= btnStepIncY + btnStepIncH;
        int bgInc = hoverInc ? ActivityColors.BUTTON_SECONDARY_HOVER : ActivityColors.BUTTON_SECONDARY_BG;
        ActivityGuiRenderer.drawPanel(context, btnStepIncX, btnStepIncY, btnStepIncW, btnStepIncH, bgInc, hoverInc ? ActivityColors.BORDER_HOVER : ActivityColors.BORDER, true);
        if (textRenderer != null) {
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal("+"), btnStepIncX + btnStepIncW / 2, btnStepIncY + 6, ActivityColors.TEXT_PRIMARY);
        }

        curY += rowH + 8;

        btnOpenEditorX = leftColX - 4;
        btnOpenEditorY = curY;
        btnOpenEditorW = PANEL_W - 20;
        btnOpenEditorH = 22;

        boolean hoverEditor = mouseX >= btnOpenEditorX && mouseX <= btnOpenEditorX + btnOpenEditorW &&
                              mouseY >= btnOpenEditorY && mouseY <= btnOpenEditorY + btnOpenEditorH;
        int bgEditor = hoverEditor ? ActivityColors.BUTTON_PRIMARY_HOVER : ActivityColors.BUTTON_PRIMARY_BG;
        int borderEditor = hoverEditor ? ActivityColors.ACCENT_LIGHT : ActivityColors.ACCENT_PRIMARY;
        ActivityGuiRenderer.drawPanel(context, btnOpenEditorX, btnOpenEditorY, btnOpenEditorW, btnOpenEditorH, bgEditor, borderEditor, true);
        if (textRenderer != null) {
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal("Открыть визуальный редактор (HUD)"), btnOpenEditorX + btnOpenEditorW / 2, btnOpenEditorY + 7, 0xFFFFFFFF);
        }

        curY += btnOpenEditorH + 6;

        btnResetPosX = leftColX - 4;
        btnResetPosY = curY;
        btnResetPosW = PANEL_W - 20;
        btnResetPosH = 22;

        boolean hoverReset = mouseX >= btnResetPosX && mouseX <= btnResetPosX + btnResetPosW &&
                             mouseY >= btnResetPosY && mouseY <= btnResetPosY + btnResetPosH;
        int bgReset = hoverReset ? ActivityColors.BUTTON_SECONDARY_HOVER : ActivityColors.BUTTON_SECONDARY_BG;
        int borderReset = hoverReset ? ActivityColors.BORDER_HOVER : ActivityColors.BORDER;
        ActivityGuiRenderer.drawPanel(context, btnResetPosX, btnResetPosY, btnResetPosW, btnResetPosH, bgReset, borderReset, true);
        if (textRenderer != null) {
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal("Сбросить координаты HUD"), btnResetPosX + btnResetPosW / 2, btnResetPosY + 7, 0xFFFF8888);
        }

        btnDoneW = 140;
        btnDoneH = 24;
        btnDoneX = panelX + (PANEL_W - btnDoneW) / 2;
        btnDoneY = panelY + PANEL_H - btnDoneH - 10;

        boolean hoverDone = mouseX >= btnDoneX && mouseX <= btnDoneX + btnDoneW &&
                            mouseY >= btnDoneY && mouseY <= btnDoneY + btnDoneH;
        int bgDone = hoverDone ? ActivityColors.BUTTON_PRIMARY_HOVER : ActivityColors.BUTTON_PRIMARY_BG;
        int borderDone = hoverDone ? ActivityColors.ACCENT_LIGHT : ActivityColors.ACCENT_PRIMARY;
        ActivityGuiRenderer.drawPanel(context, btnDoneX, btnDoneY, btnDoneW, btnDoneH, bgDone, borderDone, true);
        if (textRenderer != null) {
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal("Готово"), btnDoneX + btnDoneW / 2, btnDoneY + 8, 0xFFFFFFFF);
        }

        int hoveredBtn = hoverEnabled ? 0 : (hoverOrient ? 1 : (hoverDec ? 2 : (hoverInc ? 3 : (hoverEditor ? 4 : (hoverReset ? 5 : (hoverDone ? 6 : -1))))));
        if (hoveredBtn != lastHoveredBtn) {
            if (hoveredBtn != -1) SoundManager.playHoverImmediate();
            lastHoveredBtn = hoveredBtn;
        }

        super.render(context, mouseX, mouseY, delta);
    }
}

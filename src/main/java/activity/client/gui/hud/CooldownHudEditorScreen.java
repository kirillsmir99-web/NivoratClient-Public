package activity.client.gui.hud;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.render.ActivityGuiRenderer;
import activity.client.gui.sound.SoundManager;
import activity.client.gui.theme.ActivityColors;
import activity.client.module.service.CooldownTrackerService;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class CooldownHudEditorScreen extends Screen {

    private final Screen parent;
    private boolean isDragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    private int panelX = -1;
    private int panelY = -1;
    private static final int PANEL_W = 160;
    private static final int PANEL_H = 100;
    private boolean isPanelDragging = false;
    private int panelDragOffsetX = 0;
    private int panelDragOffsetY = 0;

    private int btnToggleOrientX, btnToggleOrientY, btnToggleOrientW, btnToggleOrientH;
    private int btnResetX, btnResetY, btnResetW, btnResetH;
    private int btnDoneX, btnDoneY, btnDoneW, btnDoneH;

    public CooldownHudEditorScreen(Screen parent) {
        super(Text.literal("Cooldown HUD • Настройка позиции"));
        this.parent = parent;
    }

    public CooldownHudEditorScreen() {
        this(null);
    }

    @Override
    public void close() {
        isDragging = false;
        isPanelDragging = false;
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
    protected void init() {
        super.init();
        if (panelX < 0 || panelY < 0) {
            panelX = 20;
            panelY = 20;
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) Math.round(click.x());
        int my = (int) Math.round(click.y());
        int button = click.buttonInfo().button();

        if (button == 0) {
            if (mx >= btnToggleOrientX && mx <= btnToggleOrientX + btnToggleOrientW &&
                my >= btnToggleOrientY && my <= btnToggleOrientY + btnToggleOrientH) {
                ActivityConfig c = ActivityConfigManager.getConfig();
                if (c != null) {
                    c.cooldownHudVertical = !c.cooldownHudVertical;
                    ActivityConfigManager.markDirty();
                }
                SoundManager.playClick();
                return true;
            }

            if (mx >= btnResetX && mx <= btnResetX + btnResetW &&
                my >= btnResetY && my <= btnResetY + btnResetH) {
                ActivityConfig c = ActivityConfigManager.getConfig();
                if (c != null) {
                    c.cooldownHudCustomX = -1;
                    c.cooldownHudCustomY = -1;
                    ActivityConfigManager.markDirty();
                }
                SoundManager.playClick();
                return true;
            }

            if (mx >= btnDoneX && mx <= btnDoneX + btnDoneW &&
                my >= btnDoneY && my <= btnDoneY + btnDoneH) {
                close();
                return true;
            }

            if (mx >= panelX && mx <= panelX + PANEL_W && my >= panelY && my <= panelY + PANEL_H) {
                isPanelDragging = true;
                panelDragOffsetX = mx - panelX;
                panelDragOffsetY = my - panelY;
                return true;
            }
        }

        ActivityConfig config = ActivityConfigManager.getConfig();
        boolean vertical = config != null && config.cooldownHudVertical;
        List<CooldownTrackerService.CooldownEntry> mockEntries = CooldownHudOverlay.getMockEntriesForPreview();

        int curX = CooldownHudOverlay.getEffectiveX(width);
        int curY = CooldownHudOverlay.getEffectiveY(height);
        int totalW = CooldownHudOverlay.calculateTotalWidth(textRenderer, mockEntries, vertical);
        int totalH = CooldownHudOverlay.calculateTotalHeight(mockEntries, vertical);

        boolean insideWidget = mx >= curX - 4 && mx <= curX + totalW + 4 && my >= curY - 4 && my <= curY + totalH + 4;

        if (button == 0 && insideWidget) {
            isDragging = true;
            dragOffsetX = mx - curX;
            dragOffsetY = my - curY;
            SoundManager.playClick();
            return true;
        } else if (button == 1 && insideWidget) {
            if (config != null) {
                config.cooldownHudCustomX = -1;
                config.cooldownHudCustomY = -1;
                ActivityConfigManager.markDirty();
            }
            SoundManager.playSelect();
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.buttonInfo().button() == 0) {
            isPanelDragging = false;
            if (isDragging) {
                isDragging = false;
                ActivityConfigManager.save();
                return true;
            }
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (isPanelDragging) {
            int newPX = (int) Math.round(click.x() - panelDragOffsetX);
            int newPY = (int) Math.round(click.y() - panelDragOffsetY);
            panelX = Math.max(2, Math.min(width - PANEL_W - 2, newPX));
            panelY = Math.max(2, Math.min(height - PANEL_H - 2, newPY));
            return true;
        }

        if (isDragging) {
            ActivityConfig config = ActivityConfigManager.getConfig();
            boolean vertical = config != null && config.cooldownHudVertical;
            List<CooldownTrackerService.CooldownEntry> mockEntries = CooldownHudOverlay.getMockEntriesForPreview();
            int totalW = CooldownHudOverlay.calculateTotalWidth(textRenderer, mockEntries, vertical);
            int totalH = CooldownHudOverlay.calculateTotalHeight(mockEntries, vertical);

            int newX = (int) Math.round(click.x() - dragOffsetX);
            int newY = (int) Math.round(click.y() - dragOffsetY);

            int clampedX = Math.max(2, Math.min(width - totalW - 2, newX));
            int clampedY = Math.max(2, Math.min(height - totalH - 2, newY));

            if (config != null) {
                config.cooldownHudCustomX = clampedX;
                config.cooldownHudCustomY = clampedY;
                ActivityConfigManager.markDirty();
            }
            return true;
        }

        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x55000000);

        ActivityConfig config = ActivityConfigManager.getConfig();
        boolean vertical = config != null && config.cooldownHudVertical;
        List<CooldownTrackerService.CooldownEntry> mockEntries = CooldownHudOverlay.getMockEntriesForPreview();

        int curX = CooldownHudOverlay.getEffectiveX(width);
        int curY = CooldownHudOverlay.getEffectiveY(height);
        int totalW = CooldownHudOverlay.calculateTotalWidth(textRenderer, mockEntries, vertical);
        int totalH = CooldownHudOverlay.calculateTotalHeight(mockEntries, vertical);

        boolean hovered = mouseX >= curX - 4 && mouseX <= curX + totalW + 4 && mouseY >= curY - 4 && mouseY <= curY + totalH + 4;
        int outlineColor = (isDragging || hovered) ? 0xAA7C4DFF : 0x44FFFFFF;
        ActivityGuiRenderer.drawBorder(context, curX - 4, curY - 4, totalW + 8, totalH + 8, outlineColor);

        CooldownHudOverlay.renderCooldownList(context, textRenderer, mockEntries, curX, curY, vertical);

        renderControlPanel(context, mouseX, mouseY, vertical);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderControlPanel(DrawContext context, int mouseX, int mouseY, boolean vertical) {
        ActivityGuiRenderer.drawPanel(context, panelX, panelY, PANEL_W, PANEL_H, 0xDD12131A, 0x447C4DFF, true);

        context.drawTextWithShadow(textRenderer, "Cooldown HUD", panelX + 10, panelY + 8, ActivityColors.TEXT_PRIMARY);

        btnToggleOrientX = panelX + 10;
        btnToggleOrientY = panelY + 24;
        btnToggleOrientW = PANEL_W - 20;
        btnToggleOrientH = 18;

        String orientText = vertical ? "Вид: Вертикальный" : "Вид: Горизонтальный";
        boolean hoverOrient = mouseX >= btnToggleOrientX && mouseX <= btnToggleOrientX + btnToggleOrientW &&
                             mouseY >= btnToggleOrientY && mouseY <= btnToggleOrientY + btnToggleOrientH;
        int bgOrient = hoverOrient ? 0xFF2A2D3D : 0xFF1C1E29;
        ActivityGuiRenderer.drawPanel(context, btnToggleOrientX, btnToggleOrientY, btnToggleOrientW, btnToggleOrientH, bgOrient, ActivityColors.BORDER, false);
        context.drawTextWithShadow(textRenderer, orientText, btnToggleOrientX + 8, btnToggleOrientY + 5, ActivityColors.TEXT_PRIMARY);

        btnResetX = panelX + 10;
        btnResetY = panelY + 48;
        btnResetW = (PANEL_W - 25) / 2;
        btnResetH = 18;

        boolean hoverReset = mouseX >= btnResetX && mouseX <= btnResetX + btnResetW &&
                            mouseY >= btnResetY && mouseY <= btnResetY + btnResetH;
        int bgReset = hoverReset ? 0xFF352020 : 0xFF241515;
        ActivityGuiRenderer.drawPanel(context, btnResetX, btnResetY, btnResetW, btnResetH, bgReset, ActivityColors.BORDER, false);
        context.drawTextWithShadow(textRenderer, "Сброс", btnResetX + 12, btnResetY + 5, 0xFFFF7777);

        btnDoneX = btnResetX + btnResetW + 5;
        btnDoneY = btnResetY;
        btnDoneW = btnResetW;
        btnDoneH = 18;

        boolean hoverDone = mouseX >= btnDoneX && mouseX <= btnDoneX + btnDoneW &&
                           mouseY >= btnDoneY && mouseY <= btnDoneY + btnDoneH;
        int bgDone = hoverDone ? 0xFF203520 : 0xFF152415;
        ActivityGuiRenderer.drawPanel(context, btnDoneX, btnDoneY, btnDoneW, btnDoneH, bgDone, ActivityColors.BORDER, false);
        context.drawTextWithShadow(textRenderer, "Готово", btnDoneX + 10, btnDoneY + 5, 0xFF77FF77);

        context.drawTextWithShadow(textRenderer, "Тяните мышкой или ПКМ", panelX + 10, panelY + 75, 0x88AAAAAA);
    }
}

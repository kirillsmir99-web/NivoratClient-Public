package activity.client.gui.dev;

import activity.client.gui.component.ActivityButton;
import activity.client.gui.component.ActivityTextField;
import activity.client.gui.font.UiTextRenderer;
import activity.client.gui.render.ActivityGuiRenderer;
import activity.client.gui.sound.SoundManager;
import activity.client.gui.theme.ActivityColors;
import activity.client.presence.DevAuthService;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class DevLoginScreen extends Screen {
    private final Screen parent;
    private final List<ActivityButton> buttons = new ArrayList<>();
    private ActivityTextField usernameField;
    private ActivityTextField passwordField;
    private volatile String status = "";
    private int statusColor = 0xCCBBDD;
    private boolean successBorder;
    private boolean errorBorder;
    private boolean submitting;

    public DevLoginScreen(Screen parent) {
        super(Text.literal("Nivorat Dev — вход"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        buttons.clear();
        int boxW = Math.min(260, width - 24);
        int boxH = 220;
        int left = (width - boxW) / 2;
        int top = Math.max(16, (height - boxH) / 2);
        int innerW = boxW - 32;
        int halfBtnW = (innerW - 8) / 2;

        usernameField = new ActivityTextField(left + 16, top + 52, innerW, 20, Text.literal("Логин"));
        usernameField.setMaxLength(32);

        passwordField = new ActivityTextField(left + 16, top + 92, innerW, 20, Text.literal("Пароль"));
        passwordField.setMaxLength(128);
        passwordField.setPasswordMode(true);

        ActivityButton btnLogin = new ActivityButton(left + 16, top + 148, halfBtnW, 20, Text.literal("Войти"), ActivityButton.Variant.PRIMARY, button -> submit());
        ActivityButton btnBack = new ActivityButton(left + 16 + halfBtnW + 8, top + 148, halfBtnW, 20, Text.literal("Назад"), ActivityButton.Variant.SECONDARY, button -> close());
        btnBack.setBrandHoverColor(ActivityColors.ACCENT_PRIMARY);

        ActivityButton btnLogout = new ActivityButton(left + 16, top + 176, innerW, 20, Text.literal("Выйти из режима разработчика"), ActivityButton.Variant.DANGER, button -> {
            DevAuthService.logout();
            status = "Режим разработчика отключён";
            statusColor = 0xFFFFAA00;
            successBorder = false;
            errorBorder = false;
            SoundManager.playClose();
        });

        buttons.add(btnLogin);
        buttons.add(btnBack);
        buttons.add(btnLogout);

        if (DevAuthService.isLoggedIn()) {
            status = "Dev-доступ активен";
            statusColor = 0xFF55FF55;
            successBorder = true;
            errorBorder = false;
        } else {
            status = "";
            statusColor = 0xCCBBDD;
            successBorder = false;
            errorBorder = false;
        }

        usernameField.setFocused(true);
    }

    private void submit() {
        if (submitting) return;
        if (usernameField == null || passwordField == null) return;
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        passwordField.setText("");
        submitting = true;
        status = "Подключение...";
        statusColor = 0xFFCCCCCC;
        successBorder = false;
        errorBorder = false;
        SoundManager.playButtonPrimary();

        DevAuthService.login(username, password).thenAccept(result -> {
            if (client != null) client.execute(() -> {
                submitting = false;
                if ("Dev-доступ активен".equals(result)) {
                    status = "Dev-доступ успешно активирован";
                    statusColor = 0xFF55FF55;
                    successBorder = true;
                    errorBorder = false;
                    SoundManager.playSuccess();
                } else {
                    if (result != null && (result.contains("пароль") || result.contains("логин") || result.contains("Проверь"))) {
                        status = "Вы неправильно ввели пароль";
                    } else {
                        status = result != null ? result : "Ошибка подключения";
                    }
                    statusColor = 0xFFFF5555;
                    successBorder = false;
                    errorBorder = true;
                    SoundManager.playError();
                }
            });
        });
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xD00E1015);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderBackground(context, mouseX, mouseY, deltaTicks);

        int boxW = Math.min(260, width - 24);
        int boxH = 220;
        int left = (width - boxW) / 2;
        int top = Math.max(16, (height - boxH) / 2);

        int borderColor = successBorder ? 0xFF28C840 : (errorBorder ? 0xFFFF5555 : 0x38FFFFFF);
        boolean glassGlow = successBorder || errorBorder;
        ActivityGuiRenderer.drawPanel(context, left, top, boxW, boxH, 0xF511141C, borderColor, glassGlow);

        UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal("РЕЖИМ РАЗРАБОТЧИКА"), left + boxW / 2, top + 12, ActivityColors.ACCENT_PRIMARY);
        UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal("Nivorat Dev — Авторизация"), left + boxW / 2, top + 24, ActivityColors.TEXT_MUTED);

        context.fill(left + 16, top + 36, left + boxW - 16, top + 37, 0x25FFFFFF);

        UiTextRenderer.drawTextWithShadow(context, textRenderer, Text.literal("Логин:"), left + 16, top + 42, ActivityColors.TEXT_SECONDARY);
        UiTextRenderer.drawTextWithShadow(context, textRenderer, Text.literal("Пароль:"), left + 16, top + 82, ActivityColors.TEXT_SECONDARY);

        if (usernameField != null) {
            usernameField.render(context, mouseX, mouseY, deltaTicks);
        }
        if (passwordField != null) {
            passwordField.render(context, mouseX, mouseY, deltaTicks);
        }

        if (status != null && !status.isEmpty()) {
            UiTextRenderer.drawCenteredTextWithShadow(context, textRenderer, Text.literal(status), left + boxW / 2, top + 124, statusColor);
        }

        for (ActivityButton btn : buttons) {
            btn.render(context, mouseX, mouseY, deltaTicks);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (usernameField != null && usernameField.mouseClicked(click, doubled)) {
            if (passwordField != null) passwordField.setFocused(false);
            return true;
        }
        if (passwordField != null && passwordField.mouseClicked(click, doubled)) {
            if (usernameField != null) usernameField.setFocused(false);
            return true;
        }
        for (ActivityButton btn : buttons) {
            if (btn.mouseClicked(click, doubled)) {
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        for (ActivityButton btn : buttons) {
            btn.mouseMoved(mouseX, mouseY);
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (usernameField != null && usernameField.isFocused() && usernameField.charTyped(input)) {
            return true;
        }
        if (passwordField != null && passwordField.isFocused() && passwordField.charTyped(input)) {
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input != null) {
            int key = input.key();
            if (key == GLFW.GLFW_KEY_TAB) {
                if (usernameField != null && usernameField.isFocused()) {
                    usernameField.setFocused(false);
                    if (passwordField != null) passwordField.setFocused(true);
                    return true;
                } else if (passwordField != null && passwordField.isFocused()) {
                    passwordField.setFocused(false);
                    if (usernameField != null) usernameField.setFocused(true);
                    return true;
                }
            } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                submit();
                return true;
            } else if (key == GLFW.GLFW_KEY_ESCAPE) {
                close();
                return true;
            }
        }

        if (usernameField != null && usernameField.isFocused() && usernameField.keyPressed(input)) {
            return true;
        }
        if (passwordField != null && passwordField.isFocused() && passwordField.keyPressed(input)) {
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

package activity.client.gui.dev;

import activity.client.gui.component.ActivityButton;
import activity.client.gui.render.ActivityGuiRenderer;
import activity.client.gui.theme.ActivityColors;
import activity.client.presence.DevAuthService;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class DevLoginScreen extends Screen {
    private final Screen parent;
    private final List<ActivityButton> buttons = new ArrayList<>();
    private TextFieldWidget usernameField;
    private TextFieldWidget passwordField;
    private volatile String status = "";
    private boolean submitting;

    public DevLoginScreen(Screen parent) {
        super(Text.literal("Nivorat Dev — вход"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        buttons.clear();
        int boxW = Math.min(220, width - 24);
        int left = (width - boxW) / 2;
        int top = Math.max(26, height / 2 - 62);
        int halfBtnW = (boxW - 10) / 2;
        usernameField = addDrawableChild(new TextFieldWidget(textRenderer, left, top, boxW, 20, Text.literal("Логин")));
        usernameField.setMaxLength(32);
        usernameField.setPlaceholder(Text.literal("Логин"));
        passwordField = addDrawableChild(new TextFieldWidget(textRenderer, left, top + 26, boxW, 20, Text.literal("Пароль")));
        passwordField.setMaxLength(128);
        passwordField.setPlaceholder(Text.literal("Пароль"));
        passwordField.addFormatter((value, start) -> Text.literal("●".repeat(value.length())).asOrderedText());
        ActivityButton btnLogin = new ActivityButton(left, top + 54, halfBtnW, 20, Text.literal("Войти"), ActivityButton.Variant.PRIMARY, button -> submit());
        ActivityButton btnBack = new ActivityButton(left + halfBtnW + 10, top + 54, halfBtnW, 20, Text.literal("Назад"), ActivityButton.Variant.SECONDARY, button -> close());
        btnBack.setBrandHoverColor(ActivityColors.ACCENT_PRIMARY);
        ActivityButton btnLogout = new ActivityButton(left, top + 80, boxW, 20, Text.literal("Выйти из дев"), ActivityButton.Variant.DANGER, button -> {
            DevAuthService.logout();
            status = "Dev-доступ отключён";
        });
        buttons.add(btnLogin);
        buttons.add(btnBack);
        buttons.add(btnLogout);
        if (DevAuthService.isLoggedIn()) status = "Dev-доступ активен";
        setInitialFocus(usernameField);
    }

    private void submit() {
        if (submitting) return;
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        passwordField.setText("");
        submitting = true;
        status = "Подключение...";
        DevAuthService.login(username, password).thenAccept(result -> {
            if (client != null) client.execute(() -> {
                status = result;
                submitting = false;
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
        int boxW = Math.min(220, width - 24);
        int left = (width - boxW) / 2;
        int top = Math.max(26, height / 2 - 62);
        ActivityGuiRenderer.drawPanel(context, left - 12, top - 22, boxW + 24, 150, 0xF012141A, 0x38FFFFFF, false);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, top - 15, 0xFFFFFF);
        if (status != null && !status.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(status), width / 2, top + 114, 0xCCBBDD);
        }
        super.render(context, mouseX, mouseY, deltaTicks);
        for (ActivityButton btn : buttons) {
            btn.render(context, mouseX, mouseY, deltaTicks);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
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
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

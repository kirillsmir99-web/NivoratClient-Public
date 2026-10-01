package activity.client.gui.overlay;

import activity.client.gui.ActivityScreen;
import activity.client.gui.ModuleSettingsView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class ClientNotification {

    private ClientNotification() {}

    public static void show(Text text) {
        if(text!=null)activity.client.gui.custom.api.modules.impl.Interface.NotificationsModule.notify((text.getContent() instanceof net.minecraft.text.TranslatableTextContent t ? activity.client.i18n.LocalizationService.get(t.getKey(),text.getString()) : text.getString()),3000);
    }
}

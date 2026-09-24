package activity.client.gui.overlay;

import activity.client.gui.ActivityScreen;
import activity.client.gui.ModuleSettingsView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class ClientNotification {

    private ClientNotification() {}

    public static void show(Text text) {
        if (text == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        if (mc.currentScreen instanceof ActivityScreen screen) {
            screen.showToast(text, null, null);
        } else if (mc.currentScreen instanceof ModuleSettingsView msv) {
            msv.showToast(text);
        }
        if (mc.player != null) {
            mc.player.sendMessage(text, true);
        }
    }
}

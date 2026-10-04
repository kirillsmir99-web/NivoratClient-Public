package activity.client.util;

import activity.client.i18n.LocalizationService;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.RenameItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;

public final class PacketSanitizer {
    private PacketSanitizer() {}

    public static boolean shouldCancelOrSanitize(Packet<?> packet) {
        if (packet instanceof UpdateSignC2SPacket signPacket) {
            String[] text = signPacket.getText();
            if (text != null) {
                for (int i = 0; i < text.length; i++) {
                    if (text[i] != null && isSensitiveText(text[i])) {
                        text[i] = "";
                    }
                }
            }
            return false;
        }
        if (packet instanceof RenameItemC2SPacket renamePacket) {
            String name = renamePacket.getName();
            if (name != null && isSensitiveText(name)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSensitiveText(String text) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("pulsehud") || lower.contains("cooldownhud") || lower.contains("nivorat")) {
            return true;
        }
        if (lower.startsWith("activity.") || lower.startsWith("pulsehud.") || lower.startsWith("cooldownhud.") || lower.startsWith("nivorat.")) {
            return true;
        }
        return LocalizationService.isModTranslation(text);
    }
}

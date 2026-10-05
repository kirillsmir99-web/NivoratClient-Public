package activity.client.util;

import activity.client.i18n.LocalizationService;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.play.ChatCommandSignedC2SPacket;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.c2s.play.RenameItemC2SPacket;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.util.Identifier;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Locale;

public final class PacketSanitizer {
    private PacketSanitizer() {}

    public static boolean shouldCancelOrSanitize(Packet<?> packet) {
        if (packet instanceof RequestCommandCompletionsC2SPacket completionsPacket) {
            return isMenuOrSensitiveCommand(completionsPacket.getPartialCommand());
        }
        if (packet instanceof CommandExecutionC2SPacket cmdPacket) {
            return isMenuOrSensitiveCommand(cmdPacket.command());
        }
        if (packet instanceof ChatCommandSignedC2SPacket signedPacket) {
            return isMenuOrSensitiveCommand(signedPacket.command());
        }
        if (packet instanceof ChatMessageC2SPacket chatPacket) {
            return isMenuOrSensitiveCommand(chatPacket.chatMessage());
        }
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
            return false;
        }
        if (packet instanceof CustomPayloadC2SPacket customPacket) {
            return shouldCancelOrSanitizeCustomPayload(customPacket.payload());
        }
        return false;
    }

    public static boolean shouldCancelOrSanitizeCustomPayload(CustomPayload payload) {
        if (payload == null || payload.getId() == null || payload.getId().id() == null) {
            return false;
        }
        Identifier channelId = payload.getId().id();
        String ns = channelId.getNamespace();
        String path = channelId.getPath();

        if (isSensitiveChannel(ns, path)) {
            return true;
        }

        if ((ns.equals("minecraft") || ns.equals("c"))
                && (path.equals("register") || path.equals("unregister"))) {
            return shouldCancelRegistration(payload);
        }

        return false;
    }

    public static boolean isSensitiveChannel(String namespace, String path) {
        if (namespace == null || path == null) return false;
        String nsLower = namespace.toLowerCase(Locale.ROOT);
        String pathLower = path.toLowerCase(Locale.ROOT);

        if (nsLower.equals("activity") || nsLower.equals("cooldownhud") || nsLower.equals("pulsehud")
                || nsLower.equals("nivorat") || nsLower.equals("nivoratclient") || nsLower.equals("pidorhud")) {
            return true;
        }
        if (nsLower.startsWith("fabric") || pathLower.contains("fabric")
                || pathLower.contains("screen-handler-registry") || pathLower.contains("dimension")) {
            return true;
        }
        return false;
    }

    private static boolean shouldCancelRegistration(CustomPayload payload) {
        try {
            Method channelsMethod = null;
            for (Method m : payload.getClass().getMethods()) {
                if (m.getName().equals("channels") && m.getParameterCount() == 0) {
                    channelsMethod = m;
                    break;
                }
            }
            if (channelsMethod != null) {
                Object result = channelsMethod.invoke(payload);
                if (result instanceof Collection<?> channels) {
                    if (channels.isEmpty()) return true;
                    for (Object obj : channels) {
                        if (obj instanceof Identifier id) {
                            if (isSensitiveChannel(id.getNamespace(), id.getPath())) {
                                return true;
                            }
                        }
                    }
                }
            } else {
                return true;
            }
        } catch (Throwable ignored) {
            return true;
        }
        return false;
    }

    public static boolean isSensitiveText(String text) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("pulsehud") || lower.contains("cooldownhud") || lower.contains("nivorat")
                || lower.contains("pidorhud")) {
            return true;
        }
        if (lower.startsWith("activity.") || lower.startsWith("pulsehud.") || lower.startsWith("cooldownhud.")
                || lower.startsWith("nivorat.") || lower.startsWith("pidorhud.")
                || lower.startsWith("key.cooldown_hud.")
                || lower.startsWith("key.activity.")
                || lower.startsWith("key.category.cooldown_hud")
                || lower.startsWith("key.category.activity.")
                || lower.startsWith("category.activity.")
                || lower.startsWith("category.cooldown_hud")) {
            return true;
        }
        if (LocalizationService.hasTranslation(lower)) {
            return true;
        }
        return LocalizationService.isModTranslation(text);
    }

    public static boolean isMenuOrSensitiveCommand(String cmd) {
        if (cmd == null || cmd.isBlank()) return false;
        String clean = cmd.trim();
        while (clean.startsWith("/")) {
            clean = clean.substring(1).trim();
        }
        String firstWord = clean;
        int spaceIdx = clean.indexOf(' ');
        if (spaceIdx > 0) {
            firstWord = clean.substring(0, spaceIdx);
        }
        activity.client.config.ActivityConfig cfg = activity.client.config.ActivityConfigManager.getConfig();
        String activeCmd = cfg != null ? cfg.menuCommand : null;
        if (activeCmd != null && !activeCmd.isBlank()) {
            while (activeCmd.startsWith("/")) {
                activeCmd = activeCmd.substring(1).trim();
            }
            if (!activeCmd.isEmpty() && firstWord.equalsIgnoreCase(activeCmd)) {
                return true;
            }
        }
        return isSensitiveText(cmd);
    }
}

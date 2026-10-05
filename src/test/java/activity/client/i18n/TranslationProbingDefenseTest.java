package activity.client.i18n;

import activity.client.MemoryLeakFixClient;
import activity.client.gui.hud.ActivityHudOverlay;
import activity.client.gui.tab.AboutTab;
import activity.client.util.ModRenderContext;
import activity.client.util.PacketSanitizer;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.packet.c2s.play.RenameItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class TranslationProbingDefenseTest {

    @Test
    void testModRenderContextLifecycle() {
        assertFalse(ModRenderContext.isInternalGui());

        ModRenderContext.beginInternal();
        try {
            assertTrue(ModRenderContext.isInternalGui());
        } finally {
            ModRenderContext.endInternal();
        }

        assertFalse(ModRenderContext.isInternalGui());

        ModRenderContext.setHudRendering(true);
        try {
            assertTrue(ModRenderContext.isInternalGui());
        } finally {
            ModRenderContext.setHudRendering(false);
        }

        assertFalse(ModRenderContext.isInternalGui());
    }

    @Test
    void testPacketSanitizerSensitivity() {
        assertTrue(PacketSanitizer.isSensitiveText("PulseHUD"));
        assertTrue(PacketSanitizer.isSensitiveText("pulsehud"));
        assertTrue(PacketSanitizer.isSensitiveText("pidorhud"));
        assertTrue(PacketSanitizer.isSensitiveText("activity.tab.about.header"));
        assertTrue(PacketSanitizer.isSensitiveText("activity.hud.cooldown.title"));
        assertTrue(PacketSanitizer.isSensitiveText("ABOUT: MEMORYLEAKFIX"));
        assertTrue(PacketSanitizer.isSensitiveText("О ПРОЕКТЕ: MEMORYLEAKFIX"));
        assertFalse(PacketSanitizer.isSensitiveText("Clean Vanilla Text"));
        assertFalse(PacketSanitizer.isSensitiveText("Stone Sword"));
        assertFalse(PacketSanitizer.isSensitiveText("key.category.minecraft.movement"));
        assertFalse(PacketSanitizer.isSensitiveText("key.category.minecraft.gameplay"));
        assertFalse(PacketSanitizer.isSensitiveText("key.category.minecraft.inventory"));
        assertFalse(PacketSanitizer.isSensitiveText("key.category.minecraft.text.entityculling.title"));
    }

    @Test
    void testSignPacketSanitization() {
        UpdateSignC2SPacket signPacket = new UpdateSignC2SPacket(
                BlockPos.ORIGIN,
                true,
                "Clean line",
                "PulseHUD probe",
                "activity.hud.cooldown.title",
                "Another clean line"
        );

        boolean cancel = PacketSanitizer.shouldCancelOrSanitize(signPacket);
        assertFalse(cancel);
        assertEquals("Clean line", signPacket.getText()[0]);
        assertEquals("", signPacket.getText()[1]);
        assertEquals("", signPacket.getText()[2]);
        assertEquals("Another clean line", signPacket.getText()[3]);
    }

    @Test
    void testAnvilRenamePacketSanitization() {
        RenameItemC2SPacket probePacket = new RenameItemC2SPacket("PulseHUD");
        assertTrue(PacketSanitizer.shouldCancelOrSanitize(probePacket));

        RenameItemC2SPacket normalPacket = new RenameItemC2SPacket("My Diamond Sword");
        assertFalse(PacketSanitizer.shouldCancelOrSanitize(normalPacket));
    }

    @Test
    void testCommandPacketSanitization() {
        activity.client.config.ActivityConfig cfg = activity.client.config.ActivityConfigManager.getConfig();
        String oldCmd = cfg != null ? cfg.menuCommand : "";
        try {
            if (cfg != null) cfg.menuCommand = "";

            net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket unconfiguredNt =
                    new net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket("nt");
            assertFalse(PacketSanitizer.shouldCancelOrSanitize(unconfiguredNt));

            if (cfg != null) cfg.menuCommand = "mycustom";

            net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket customTab =
                    new net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket(1, "/mycustom");
            assertTrue(PacketSanitizer.shouldCancelOrSanitize(customTab));

            net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket customSpaceTab =
                    new net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket(2, "/mycustom ");
            assertTrue(PacketSanitizer.shouldCancelOrSanitize(customSpaceTab));

            net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket customCmd =
                    new net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket("mycustom");
            assertTrue(PacketSanitizer.shouldCancelOrSanitize(customCmd));

            net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket cleanTab =
                    new net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket(3, "/help");
            assertFalse(PacketSanitizer.shouldCancelOrSanitize(cleanTab));

            net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket cleanCmd =
                    new net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket("gamemode creative");
            assertFalse(PacketSanitizer.shouldCancelOrSanitize(cleanCmd));
        } finally {
            if (cfg != null) cfg.menuCommand = oldCmd;
        }
    }

    @Test
    void testClientConstantsNeutralized() {
        assertEquals("MemoryLeakFix", MemoryLeakFixClient.CLIENT_NAME);
        assertEquals("MemoryLeakFix", AboutTab.CLIENT_NAME);
        assertEquals("MemoryLeakFix", ActivityHudOverlay.DEFAULT_TITLE);
    }

    @Test
    void testFabricModJsonSanitized() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/fabric.mod.json")) {
            assertNotNull(is);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();

            if (root.has("provides")) {
                JsonArray provides = root.getAsJsonArray("provides");
                for (var elem : provides) {
                    assertNotEquals("nivoratclient", elem.getAsString().toLowerCase());
                }
            }

            JsonObject entrypoints = root.getAsJsonObject("entrypoints");
            assertNotNull(entrypoints);
            assertFalse(entrypoints.has("nivorat:settings_v1"));

            JsonArray clientEps = entrypoints.getAsJsonArray("client");
            assertNotNull(clientEps);
            assertEquals(1, clientEps.size());
            assertEquals("activity.client.MemoryLeakFixClient", clientEps.get(0).getAsString());
        }
    }

    @Test
    void testChannelFiltering() {
        assertTrue(PacketSanitizer.isSensitiveChannel("activity", "test"));
        assertTrue(PacketSanitizer.isSensitiveChannel("nivorat", "sync"));
        assertTrue(PacketSanitizer.isSensitiveChannel("nivoratclient", "data"));
        assertTrue(PacketSanitizer.isSensitiveChannel("cooldownhud", "sync"));
        assertTrue(PacketSanitizer.isSensitiveChannel("pulsehud", "sync"));
        assertTrue(PacketSanitizer.isSensitiveChannel("pidorhud", "sync"));
        assertTrue(PacketSanitizer.isSensitiveChannel("fabric", "registry/sync/v1"));
        assertTrue(PacketSanitizer.isSensitiveChannel("fabric-screen-handler-registry", "v1"));
        assertFalse(PacketSanitizer.isSensitiveChannel("minecraft", "brand"));
    }

    @Test
    void testNamespaceStealth() {
        assertEquals("activity", activity.client.gui.sound.ActivitySoundEvents.MOD_ID);
        assertEquals("activity", activity.client.gui.icon.ActivityIconRenderer.ATLAS_ID.getNamespace());
        assertEquals("activity", AboutTab.TEXTURE_TELEGRAM.getNamespace());
        assertEquals("activity", AboutTab.TEXTURE_DONATE.getNamespace());
        assertEquals("activity", AboutTab.TEXTURE_YOUTUBE.getNamespace());
        assertEquals("activity", AboutTab.TEXTURE_TIKTOK.getNamespace());
        assertEquals("activity", AboutTab.TEXTURE_DISCORD.getNamespace());
    }

    @Test
    void testAllModKeysIntercepted() {
        assertTrue(LocalizationService.hasTranslation("activity.module.auto_totem.name"));
        assertTrue(LocalizationService.hasTranslation("theme.profile.crystal"));
        assertTrue(LocalizationService.hasTranslation("category.pinned"));
        assertTrue(LocalizationService.hasTranslation("ui.search.placeholder"));
        assertTrue(LocalizationService.hasTranslation("module.capability_logs.name"));

        assertTrue(PacketSanitizer.isSensitiveText("theme.profile.crystal"));
        assertTrue(PacketSanitizer.isSensitiveText("category.pinned"));
        assertTrue(PacketSanitizer.isSensitiveText("ui.search.placeholder"));
        assertTrue(PacketSanitizer.isSensitiveText("module.capability_logs.name"));

        assertTrue(PacketSanitizer.isSensitiveText("Кристальный"));
        assertTrue(PacketSanitizer.isSensitiveText("Закреплённые"));
        assertTrue(PacketSanitizer.isSensitiveText("Логи способностей"));
    }
}

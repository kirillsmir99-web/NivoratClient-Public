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
}

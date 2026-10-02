package activity.client.diagnostic;

import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Diagnostic & Telemetry Engine Tests")
class DiagnosticEngineTest {

    @BeforeEach
    void setUp() {
        DiagnosticEngine.resetForTesting();
    }

    @AfterEach
    void tearDown() {
        DiagnosticEngine.resetForTesting();
    }

    @Test
    @DisplayName("Engine starts active with empty queues")
    void testInitialState() {
        assertTrue(DiagnosticEngine.isActive());
        assertTrue(DiagnosticEngine.getFpsDrops().isEmpty());
        assertTrue(DiagnosticEngine.getActionHistory().isEmpty());
        assertTrue(DiagnosticEngine.getErrorHistory().isEmpty());
        assertTrue(DiagnosticEngine.getPacketHistory().isEmpty());
    }

    @Test
    @DisplayName("Record actions correctly tracks success and failures")
    void testRecordActions() {
        DiagnosticEngine.recordAction("click_pearl", "trigger", true, "slot=2");
        DiagnosticEngine.recordAction("click_pearl", "use_pearl", false, "pearl_on_cooldown");

        var actions = DiagnosticEngine.getActionHistory();
        assertEquals(2, actions.size());

        var first = actions.get(0);
        assertEquals("click_pearl", first.module());
        assertEquals("trigger", first.action());
        assertTrue(first.success());
        assertEquals("slot=2", first.details());

        var second = actions.get(1);
        assertEquals("click_pearl", second.module());
        assertEquals("use_pearl", second.action());
        assertFalse(second.success());
        assertEquals("pearl_on_cooldown", second.details());
    }

    @Test
    @DisplayName("Record errors captures phase, exception class and truncated stack trace")
    void testRecordErrors() {
        RuntimeException ex = new RuntimeException("Simulated network timeout");
        DiagnosticEngine.recordError("auto_cart", "bow_draw", ex);

        var errors = DiagnosticEngine.getErrorHistory();
        assertEquals(1, errors.size());

        var err = errors.get(0);
        assertEquals("auto_cart", err.module());
        assertEquals("bow_draw", err.phase());
        assertEquals("RuntimeException", err.errorType());
        assertEquals("Simulated network timeout", err.message());
        assertNotNull(err.stackTrace());
        assertTrue(err.stackTrace().contains("at "));
    }

    @Test
    @DisplayName("Record packet summarizes known packets")
    void testRecordPackets() {
        UpdateSelectedSlotC2SPacket update = new UpdateSelectedSlotC2SPacket(3);
        DiagnosticEngine.recordPacket(update);

        var packets = DiagnosticEngine.getPacketHistory();
        assertEquals(1, packets.size());

        var pkt = packets.get(0);
        assertEquals("UpdateSelectedSlotC2SPacket", pkt.packetType());
        assertTrue(pkt.summary().contains("slot=3"));
    }

    @Test
    @DisplayName("Generate report contains all diagnostic sections and statistics")
    void testGenerateReport() {
        DiagnosticEngine.recordAction("auto_totem", "swap", false, "no_totem_available");
        DiagnosticEngine.recordError("auto_spear", "stab", new IllegalStateException("Slot sync error"));
        DiagnosticEngine.recordPacket(new UpdateSelectedSlotC2SPacket(5));

        String report = DiagnosticEngine.generateReport();
        assertNotNull(report);
        assertTrue(report.contains("NIVORAT CLIENT DIAGNOSTIC REPORT"));
        assertTrue(report.contains("SECTION 1: UNEXECUTED ACTIONS & FAILED CLICKS"));
        assertTrue(report.contains("SECTION 2: MODULE RUNTIME ERRORS & EXCEPTIONS"));
        assertTrue(report.contains("SECTION 3: FPS DROPS & STUTTER SPIKES"));
        assertTrue(report.contains("SECTION 4: RECENT OUTGOING PACKET STREAM"));
        assertTrue(report.contains("no_totem_available"));
        assertTrue(report.contains("Slot sync error"));
        assertTrue(report.contains("slot=5"));
    }

    @Test
    @DisplayName("Export report writes valid UTF-8 file to target folder")
    void testExportReportToFile() throws IOException {
        DiagnosticEngine.recordAction("combat", "attack", true, "entity=zombie");
        Path tempDir = Files.createTempDirectory("diag_test");
        try {
            File exported = DiagnosticEngine.exportReportToFile(tempDir.toFile());
            assertNotNull(exported);
            assertTrue(exported.exists());
            assertTrue(exported.length() > 0);
            String content = Files.readString(exported.toPath());
            assertTrue(content.contains("NIVORAT CLIENT DIAGNOSTIC REPORT"));
            assertTrue(content.contains("combat::attack"));
        } finally {
            try (var s = Files.walk(tempDir)) {
                s.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                });
            }
        }
    }

    @Test
    @DisplayName("Purge for capitulation completely resets and deactivates telemetry")
    void testCapitulationPurge() {
        DiagnosticEngine.recordAction("click_pearl", "trigger", true, "slot=1");
        DiagnosticEngine.recordError("core", "render", new Exception("Test"));
        DiagnosticEngine.recordPacket(new UpdateSelectedSlotC2SPacket(1));

        DiagnosticEngine.purgeForCapitulation();

        assertFalse(DiagnosticEngine.isActive());
        assertTrue(DiagnosticEngine.getActionHistory().isEmpty());
        assertTrue(DiagnosticEngine.getErrorHistory().isEmpty());
        assertTrue(DiagnosticEngine.getPacketHistory().isEmpty());
        assertTrue(DiagnosticEngine.getFpsDrops().isEmpty());

        DiagnosticEngine.recordAction("click_pearl", "trigger", true, "ignored");
        assertTrue(DiagnosticEngine.getActionHistory().isEmpty());
    }
}

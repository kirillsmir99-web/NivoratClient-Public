package activity.client.config.migration;

import activity.client.config.ActivityConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class LegacyConfigMigrationTest {

    @TempDir
    Path tempConfigDir;

    private ActivityConfig config;

    @BeforeEach
    void setUp() {
        config = new ActivityConfig();
    }

    @Test
    void testLegacyConfigMigrationAll12Modules() throws IOException {

        Files.writeString(tempConfigDir.resolve("redstone_optimizer.properties"),
                "enabled=true\n" +
                "smart_mode=false\n" +
                "enchant_mode=breach\n" +
                "miss_behavior=empty_swap\n" +
                "source_mode=axe_only\n" +
                "restore_delay_ms=125.0\n" +
                "random_delay=false\n" +
                "legit_mode=false\n" +
                "miss_chance=15.0\n",
                StandardCharsets.UTF_8);

        Files.writeString(tempConfigDir.resolve("momentum_tweaks.properties"),
                "enabled=true\n" +
                "security_mode=rage\n" +
                "priority_mode=lunge_2\n" +
                "restore_delay_ms=250.0\n" +
                "variance_pct=5.0\n" +
                "random_delay=false\n",
                StandardCharsets.UTF_8);

        Files.writeString(tempConfigDir.resolve("autoshieldbreaker.properties"),
                "enabled=true\n" +
                "full_auto=false\n" +
                "distance=3.2\n" +
                "chance=90.0\n" +
                "switch_delay_ms=75.0\n" +
                "restore_delay_ms=65.0\n" +
                "random_delay=false\n" +
                "legit_mode=false\n",
                StandardCharsets.UTF_8);

        Files.writeString(tempConfigDir.resolve("autostunslime.properties"),
                "enabled=true\n" +
                "mode=semi_auto\n" +
                "distance=3.1\n" +
                "chance=85.0\n" +
                "air_time_sec=2.0\n" +
                "axe_delay_ms=60.0\n" +
                "mace_delay_ms=55.0\n" +
                "restore_delay_ms=70.0\n" +
                "random_delay=false\n" +
                "legit_mode=false\n",
                StandardCharsets.UTF_8);

        Files.writeString(tempConfigDir.resolve("autototem.properties"),
                "enabled=true\n" +
                "slot_mode=offhand\n" +
                "trigger_hearts=4.5\n" +
                "restore_hearts=8.0\n" +
                "chance=95.0\n" +
                "return_item=true\n" +
                "return_on_pop=false\n",
                StandardCharsets.UTF_8);

        Files.writeString(tempConfigDir.resolve("autocart.properties"),
                "enabled=true\n" +
                "preset=safe\n" +
                "placement_chance=92.0\n" +
                "max_distance=3.8\n" +
                "min_delay_ms=60.0\n" +
                "max_delay_ms=130.0\n" +
                "allow_self_cart=true\n" +
                "allow_pit_placement=false\n" +
                "random_delay=false\n" +
                "legit_mode=false\n" +
                "rail_delay=3.0\n" +
                "cart_delay=4.0\n" +
                "restore_delay=5.0\n",
                StandardCharsets.UTF_8);

        Files.writeString(tempConfigDir.resolve("luminance_tweaks.properties"),
                "enabled=true\n" +
                "anchor_preset=fast\n" +
                "auto_explode=true\n" +
                "auto_return=false\n" +
                "charge_delay=2.0\n" +
                "explode_delay=3.0\n" +
                "anchor_chance=98.0\n" +
                "target_charges=3.0\n" +
                "safe_mode=false\n",
                StandardCharsets.UTF_8);

        Files.writeString(tempConfigDir.resolve("storage_tweaks.properties"),
                "enabled=true\n" +
                "refill_delay_ticks=4.0\n" +
                "refill_chance=85.0\n" +
                "auto_close_container=false\n" +
                "random_delay=false\n" +
                "safe_mode=false\n",
                StandardCharsets.UTF_8);

        Files.writeString(tempConfigDir.resolve("hp_reaper.properties"),
                "enabled=true\n" +
                "hud_mode=damage_diff\n" +
                "filter_type=players_only\n" +
                "own_hp_x=150\n" +
                "own_hp_y=250\n" +
                "target_hp_x=350\n" +
                "target_hp_y=450\n" +
                "crosshair_x=550\n" +
                "crosshair_y=650\n" +
                "diff_x=750\n" +
                "diff_y=850\n",
                StandardCharsets.UTF_8);

        Files.writeString(tempConfigDir.resolve("autotool.json"),
                "{\n" +
                "  \"enabled\": true,\n" +
                "  \"combat_guard\": false,\n" +
                "  \"durability_saver\": true,\n" +
                "  \"durability_threshold\": 12.0,\n" +
                "  \"prefer_silk_touch\": true,\n" +
                "  \"restore_previous\": false,\n" +
                "  \"legit_mode\": false,\n" +
                "  \"single_slot_mode\": true,\n" +
                "  \"ignore_instant_break\": false,\n" +
                "  \"lock_while_mining\": false\n" +
                "}\n",
                StandardCharsets.UTF_8);

        Files.writeString(tempConfigDir.resolve("autogg.json"),
                "{\n" +
                "  \"enabled\": true,\n" +
                "  \"phrase\": \"Well Played!\",\n" +
                "  \"send_on_kill\": true,\n" +
                "  \"send_on_own_death\": true,\n" +
                "  \"random_order\": true,\n" +
                "  \"delay_ms\": 1500.0\n" +
                "}\n",
                StandardCharsets.UTF_8);

        Files.writeString(tempConfigDir.resolve("cart_hud.properties"),
                "enabled=true\n" +
                "custom_x=220\n" +
                "custom_y=330\n",
                StandardCharsets.UTF_8);

        Files.writeString(tempConfigDir.resolve("redstone_keys.properties"),
                "key_auto_mace=66\n" +
                "key_auto_spear=86\n",
                StandardCharsets.UTF_8);

        Files.writeString(tempConfigDir.resolve("pvp_keys.properties"),
                "key_auto_shieldbreaker=71\n" +
                "key_auto_stun_slime=72\n",
                StandardCharsets.UTF_8);

        assertTrue(LegacyConfigMigrator.hasAnyLegacyConfig(tempConfigDir));

        boolean migrated = LegacyConfigMigrator.migrate(config, tempConfigDir);
        assertTrue(migrated, "Migration should report success");

        assertTrue(config.autoMaceEnabled);
        assertEquals("breach_only", config.autoMaceEnchantMode);
        assertEquals("empty_swap", config.autoMaceMissBehavior);
        assertEquals("axe_only", config.autoMaceSourceMode);
        assertEquals(125.0, config.autoMaceRestoreDelayMs);
        assertFalse(config.autoMaceRandomDelay);
        assertFalse(config.autoMaceLegitMode);
        assertEquals(15.0, config.autoMaceMissChance);
        assertEquals(66, config.autoMaceKeybind.getKeyCode());

        assertTrue(config.autoSpearEnabled);
        assertEquals("rage", config.autoSpearSecurityMode);
        assertEquals("lunge_2", config.autoSpearPriorityMode);
        assertEquals(250.0, config.autoSpearRestoreDelayMs);
        assertEquals(5.0, config.autoSpearMissChance);
        assertEquals(86, config.autoSpearKeybind.getKeyCode());

        assertTrue(config.autoShieldbreakerEnabled);
        assertEquals("semi_auto", config.autoShieldbreakerMode);
        assertEquals(3.2, config.autoShieldbreakerDistance);
        assertEquals(90.0, config.autoShieldbreakerChance);
        assertEquals(75.0, config.autoShieldbreakerSwitchDelayMs);
        assertEquals(65.0, config.autoShieldbreakerRestoreDelayMs);
        assertEquals(71, config.autoShieldbreakerKeybind.getKeyCode());

        assertTrue(config.autoStunSlamEnabled);
        assertEquals("semi_auto", config.autoStunSlamMode);
        assertEquals(3.1, config.autoStunSlamDistance);
        assertEquals(85.0, config.autoStunSlamChance);
        assertEquals(2.0, config.autoStunSlamAirTimeSec);
        assertEquals(60.0, config.autoStunSlamAxeDelayMs);
        assertEquals(55.0, config.autoStunSlamMaceDelayMs);
        assertEquals(70.0, config.autoStunSlamRestoreDelayMs);
        assertEquals(72, config.autoStunSlamKeybind.getKeyCode());

        assertTrue(config.autoTotemEnabled);
        assertEquals("offhand", config.autoTotemMode);
        assertEquals(4.5, config.autoTotemTriggerHearts);
        assertEquals(8.0, config.autoTotemRestoreHearts);
        assertEquals(95.0, config.autoTotemChance);
        assertFalse(config.autoTotemReturnOnPop);

        assertTrue(config.autoCartEnabled);
        assertEquals("safe", config.autoCartPreset);
        assertEquals(92.0, config.autoCartPlacementChance);
        assertEquals(3.8, config.autoCartMaxDistance);
        assertEquals(60.0, config.autoCartMinDelayMs);
        assertEquals(130.0, config.autoCartMaxDelayMs);
        assertTrue(config.autoCartAllowSelfCart);
        assertFalse(config.autoCartAllowPitPlacement);

        assertTrue(config.autoAnchorEnabled);
        assertEquals("fast", config.autoAnchorPreset);
        assertTrue(config.autoAnchorAutoExplode);
        assertFalse(config.autoAnchorAutoReturn);
        assertEquals(2.0, config.autoAnchorChargeDelay);
        assertEquals(3.0, config.autoAnchorExplodeDelay);
        assertEquals(98.0, config.autoAnchorChance);
        assertEquals(3.0, config.autoAnchorTargetCharges);

        assertTrue(config.cartRefillEnabled);
        assertEquals(4.0, config.cartRefillDelayTicks);
        assertEquals(85.0, config.cartRefillChance);
        assertFalse(config.cartRefillAutoClose);

        assertTrue(config.hpReaperEnabled);
        assertEquals("damage_diff", config.hpReaperMode);
        assertEquals("players_only", config.hpReaperTargetFilter);
        assertEquals(150, config.hpReaperOwnHealthX);
        assertEquals(250, config.hpReaperOwnHealthY);

        assertTrue(config.autoToolEnabled);
        assertFalse(config.autoToolCombatGuard);
        assertTrue(config.autoToolDurabilitySaver);
        assertEquals(12.0, config.autoToolDurabilityThreshold);
        assertTrue(config.autoToolPreferSilkTouch);
        assertFalse(config.autoToolRestorePrevious);

        assertTrue(config.autoGGEnabled);
        assertEquals("Well Played!", config.autoGGPhrase);
        assertTrue(config.autoGGSendOnKill);
        assertTrue(config.autoGGSendOnOwnDeath);
        assertTrue(config.autoGGRandomOrder);
        assertEquals(1500.0, config.autoGGDelayMs);

        assertTrue(config.cartHudEnabled);
        assertEquals(220, config.cartHudCustomX);
        assertEquals(330, config.cartHudCustomY);

        assertTrue(config.legacyMigrationDone);
        assertEquals(1, config.legacyMigrationVersion);
        Path marker = tempConfigDir.resolve(LegacyConfigMigrator.MIGRATION_MARKER_FILE);
        assertTrue(Files.exists(marker), "Migration marker file must exist");
        String markerContent = Files.readString(marker);
        assertTrue(markerContent.contains("migration_version=1"));
        assertTrue(markerContent.contains("client=NivoratClient"));
    }

    @Test
    void testMigrationIdempotency() throws IOException {
        Files.writeString(tempConfigDir.resolve("redstone_optimizer.properties"),
                "restore_delay_ms=175.0\n", StandardCharsets.UTF_8);

        boolean first = LegacyConfigMigrator.migrate(config, tempConfigDir);
        assertTrue(first);
        assertEquals(175.0, config.autoMaceRestoreDelayMs);

        config.autoMaceRestoreDelayMs = 99.0;

        boolean second = LegacyConfigMigrator.migrate(config, tempConfigDir);
        assertFalse(second, "Second migration must not run");
        assertEquals(99.0, config.autoMaceRestoreDelayMs, "User changes must not be overwritten by redundant migration");
    }

    @Test
    void testClampingOutOfRangeValues() throws IOException {
        Files.writeString(tempConfigDir.resolve("redstone_optimizer.properties"),
                "miss_chance=999.0\n" +
                "restore_delay_ms=10000.0\n",
                StandardCharsets.UTF_8);

        Files.writeString(tempConfigDir.resolve("autotool.json"),
                "{\n" +
                "  \"durability_threshold\": 999.0\n" +
                "}\n",
                StandardCharsets.UTF_8);

        Files.writeString(tempConfigDir.resolve("autoshieldbreaker.properties"),
                "distance=99.0\n",
                StandardCharsets.UTF_8);

        LegacyConfigMigrator.migrate(config, tempConfigDir);

        assertEquals(50.0, config.autoMaceMissChance, "miss_chance max is 50.0");
        assertEquals(300.0, config.autoMaceRestoreDelayMs, "restore_delay max is 300.0");
        assertEquals(50.0, config.autoToolDurabilityThreshold, "durability_threshold max is 50.0");
        assertEquals(4.5, config.autoShieldbreakerDistance, "distance max is 4.5");
    }

    @Test
    void testCorruptedLegacyFilesDoNotCrash() throws IOException {

        Files.writeString(tempConfigDir.resolve("autotool.json"),
                "{{NOT_VALID_JSON...", StandardCharsets.UTF_8);

        Files.writeString(tempConfigDir.resolve("autoshieldbreaker.properties"),
                "distance=3.3\n", StandardCharsets.UTF_8);

        assertDoesNotThrow(() -> {
            boolean ok = LegacyConfigMigrator.migrate(config, tempConfigDir);
            assertTrue(ok);
        });

        assertEquals(3.3, config.autoShieldbreakerDistance);
    }

    @Test
    void testDirectAutoStunSlamFilenameMigration() throws IOException {
        Files.writeString(tempConfigDir.resolve("autostunslam.properties"),
                "enabled=true\n" +
                "distance=2.9\n" +
                "chance=65.0\n",
                StandardCharsets.UTF_8);

        boolean ok = LegacyConfigMigrator.migrate(config, tempConfigDir);
        assertTrue(ok);
        assertEquals(2.9, config.autoStunSlamDistance);
        assertEquals(65.0, config.autoStunSlamChance);
    }

    @Test
    void testSecondaryLegacyFileNamesMigration() throws IOException {

        Files.writeString(tempConfigDir.resolve("morrow.properties"),
                "enabled=true\n" +
                "preset=safe\n" +
                "placementChance=88\n",
                StandardCharsets.UTF_8);

        Files.writeString(tempConfigDir.resolve("autospear.properties"),
                "enabled=true\n" +
                "security_mode=semi_legit\n",
                StandardCharsets.UTF_8);

        Files.writeString(tempConfigDir.resolve("cartrefill.properties"),
                "enabled=true\n" +
                "chance=91.0\n",
                StandardCharsets.UTF_8);

        Files.writeString(tempConfigDir.resolve("vitality_tweaks.properties"),
                "enabled=true\n" +
                "display_mode=compact\n",
                StandardCharsets.UTF_8);

        Files.writeString(tempConfigDir.resolve("carthud.properties"),
                "enabled=true\n" +
                "custom_x=275\n",
                StandardCharsets.UTF_8);

        boolean ok = LegacyConfigMigrator.migrate(config, tempConfigDir);
        assertTrue(ok);

        assertEquals("safe", config.autoCartPreset);
        assertEquals(88.0, config.autoCartPlacementChance);
        assertEquals("semi_legit", config.autoSpearSecurityMode);
        assertEquals(91.0, config.cartRefillChance);
        assertEquals("compact", config.hpReaperMode);
        assertEquals(275, config.cartHudCustomX);
    }

    @Test
    void testJsonLegacyConfigMigration() throws IOException {
        Files.writeString(tempConfigDir.resolve("automace.json"),
                "{\n" +
                "  \"enabled\": true,\n" +
                "  \"source_mode\": \"axe_only\",\n" +
                "  \"enchant_mode\": \"breach_only\",\n" +
                "  \"restore_delay_ms\": 115.0,\n" +
                "  \"miss_chance\": 22.0\n" +
                "}\n",
                StandardCharsets.UTF_8);

        Files.writeString(tempConfigDir.resolve("autoshieldbreaker.json"),
                "{\n" +
                "  \"enabled\": true,\n" +
                "  \"distance\": 3.4,\n" +
                "  \"chance\": 85.0,\n" +
                "  \"mode\": \"semi_auto\",\n" +
                "  \"switch_delay_ms\": 65.0\n" +
                "}\n",
                StandardCharsets.UTF_8);

        Files.writeString(tempConfigDir.resolve("autostunslime.json"),
                "{\n" +
                "  \"enabled\": true,\n" +
                "  \"distance\": 2.8,\n" +
                "  \"chance\": 70.0,\n" +
                "  \"air_time_sec\": 1.5\n" +
                "}\n",
                StandardCharsets.UTF_8);

        Files.writeString(tempConfigDir.resolve("autototem.json"),
                "{\n" +
                "  \"enabled\": true,\n" +
                "  \"trigger_hearts\": 5.0,\n" +
                "  \"restore_hearts\": 7.0,\n" +
                "  \"mode\": \"offhand\"\n" +
                "}\n",
                StandardCharsets.UTF_8);

        Files.writeString(tempConfigDir.resolve("keys.json"),
                "{\n" +
                "  \"auto_mace\": {\"code\": 66},\n" +
                "  \"auto_shieldbreaker\": 71\n" +
                "}\n",
                StandardCharsets.UTF_8);

        assertTrue(LegacyConfigImporter.hasAnyLegacyConfig(tempConfigDir));

        boolean ok = LegacyConfigImporter.migrate(config, tempConfigDir);
        assertTrue(ok);

        assertTrue(config.autoMaceEnabled);
        assertEquals("axe_only", config.autoMaceSourceMode);
        assertEquals("breach_only", config.autoMaceEnchantMode);
        assertEquals(115.0, config.autoMaceRestoreDelayMs);
        assertEquals(22.0, config.autoMaceMissChance);
        assertEquals(66, config.autoMaceKeybind.getKeyCode());

        assertTrue(config.autoShieldbreakerEnabled);
        assertEquals(3.4, config.autoShieldbreakerDistance);
        assertEquals(85.0, config.autoShieldbreakerChance);
        assertEquals("semi_auto", config.autoShieldbreakerMode);
        assertEquals(65.0, config.autoShieldbreakerSwitchDelayMs);
        assertEquals(71, config.autoShieldbreakerKeybind.getKeyCode());

        assertTrue(config.autoStunSlamEnabled);
        assertEquals(2.8, config.autoStunSlamDistance);
        assertEquals(70.0, config.autoStunSlamChance);
        assertEquals(1.5, config.autoStunSlamAirTimeSec);

        assertTrue(config.autoTotemEnabled);
        assertEquals(5.0, config.autoTotemTriggerHearts);
        assertEquals(7.0, config.autoTotemRestoreHearts);
        assertEquals("offhand", config.autoTotemMode);
    }

    @Test
    void testLegacyConfigImporterFacadeAndForceMigration() throws IOException {
        Files.writeString(tempConfigDir.resolve("automace.json"),
                "{\"restore_delay_ms\": 180.0}\n",
                StandardCharsets.UTF_8);

        assertTrue(LegacyConfigImporter.hasAnyLegacyConfig(tempConfigDir));
        assertFalse(LegacyConfigImporter.isAlreadyMigrated(config, tempConfigDir));

        LegacyConfigImporter.markMigrated(config, tempConfigDir);
        assertTrue(LegacyConfigImporter.isAlreadyMigrated(config, tempConfigDir));

        assertFalse(LegacyConfigImporter.migrate(config, tempConfigDir));

        boolean forced = LegacyConfigImporter.migrate(config, tempConfigDir, true);
        assertTrue(forced, "Force migration must succeed even if marked as migrated");
        assertEquals(180.0, config.autoMaceRestoreDelayMs);
    }
}

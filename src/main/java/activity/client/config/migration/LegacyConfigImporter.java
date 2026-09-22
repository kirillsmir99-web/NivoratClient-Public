package activity.client.config.migration;

import activity.client.config.ActivityConfig;
import activity.client.config.NivoratConfigManager;

import java.nio.file.Path;

public final class LegacyConfigImporter {

    public static final String MIGRATION_MARKER_FILE = LegacyConfigMigrator.MIGRATION_MARKER_FILE;
    public static final int CURRENT_MIGRATION_VERSION = LegacyConfigMigrator.CURRENT_MIGRATION_VERSION;

    private LegacyConfigImporter() {}

    public static Path resolveConfigDir() {
        return LegacyConfigMigrator.resolveConfigDir();
    }

    public static boolean hasAnyLegacyConfig(Path configDir) {
        return LegacyConfigMigrator.hasAnyLegacyConfig(configDir);
    }

    public static boolean isAlreadyMigrated(ActivityConfig config, Path configDir) {
        return LegacyConfigMigrator.isAlreadyMigrated(config, configDir);
    }

    public static void markMigrated(ActivityConfig config, Path configDir) {
        LegacyConfigMigrator.markMigrated(config, configDir);
    }

    public static boolean checkAndMigrate(ActivityConfig config) {
        return LegacyConfigMigrator.checkAndMigrate(config);
    }

    public static boolean migrate(ActivityConfig config, Path configDir) {
        return LegacyConfigMigrator.migrate(config, configDir);
    }

    public static boolean migrate(ActivityConfig config, Path configDir, boolean force) {
        return LegacyConfigMigrator.migrate(config, configDir, force);
    }

    public static boolean importLegacyConfigs() {
        return NivoratConfigManager.importLegacyConfigs();
    }
}

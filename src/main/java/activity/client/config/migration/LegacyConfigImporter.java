package activity.client.config.migration;

import activity.client.config.ActivityConfig;
import activity.client.config.NivoratConfigManager;

import java.nio.file.Path;

/**
 * Legacy configuration import service and facade for NivoratClient.
 * Provides a standardized public API for discovering and importing legacy standalone
 * mod configurations across all 12 modules into the consolidated configuration schema.
 */
public final class LegacyConfigImporter {

    public static final String MIGRATION_MARKER_FILE = LegacyConfigMigrator.MIGRATION_MARKER_FILE;
    public static final int CURRENT_MIGRATION_VERSION = LegacyConfigMigrator.CURRENT_MIGRATION_VERSION;

    private LegacyConfigImporter() {}

    /**
     * @return the resolved active configuration directory
     */
    public static Path resolveConfigDir() {
        return LegacyConfigMigrator.resolveConfigDir();
    }

    /**
     * Checks if any legacy configuration file exists in the specified directory.
     */
    public static boolean hasAnyLegacyConfig(Path configDir) {
        return LegacyConfigMigrator.hasAnyLegacyConfig(configDir);
    }

    /**
     * Checks if migration has already been executed for this installation.
     */
    public static boolean isAlreadyMigrated(ActivityConfig config, Path configDir) {
        return LegacyConfigMigrator.isAlreadyMigrated(config, configDir);
    }

    /**
     * Marks migration as completed with version tracking and marker file creation.
     */
    public static void markMigrated(ActivityConfig config, Path configDir) {
        LegacyConfigMigrator.markMigrated(config, configDir);
    }

    /**
     * Standard migration check called upon client boot.
     */
    public static boolean checkAndMigrate(ActivityConfig config) {
        return LegacyConfigMigrator.checkAndMigrate(config);
    }

    /**
     * Performs migration of all available legacy mod files into the target configuration.
     */
    public static boolean migrate(ActivityConfig config, Path configDir) {
        return LegacyConfigMigrator.migrate(config, configDir);
    }

    /**
     * Performs migration of all available legacy mod files with optional force flag.
     */
    public static boolean migrate(ActivityConfig config, Path configDir, boolean force) {
        return LegacyConfigMigrator.migrate(config, configDir, force);
    }

    /**
     * Triggers inspection and migration of legacy mod configuration files.
     */
    public static boolean importLegacyConfigs() {
        return NivoratConfigManager.importLegacyConfigs();
    }
}

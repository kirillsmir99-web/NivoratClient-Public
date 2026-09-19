package dev.hpreaper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public final class VitalityConfig {
    public static int ownHealthX = -1;
    public static int ownHealthY = -1;
    public static int crosshairTargetX = -1;
    public static int crosshairTargetY = -1;
    public static int targetHealthX = -1;
    public static int targetHealthY = -1;
    public static int diffX = -1;
    public static int diffY = -1;

    public static HealthHudOverlay.DisplayMode displayMode = HealthHudOverlay.DisplayMode.OWN_HEALTH;
    public static TargetFilter targetFilter = TargetFilter.ALL_ENTITIES;

    private static final File CONFIG_FILE = new File("config/hp_reaper.properties");
    private static boolean saving = false;

    public static int getModeX(HealthHudOverlay.DisplayMode mode) {
        return switch (mode) {
            case OWN_HEALTH -> ownHealthX;
            case CROSSHAIR_AND_TARGET -> crosshairTargetX;
            case TARGET_HEALTH -> targetHealthX;
            case OWN_TARGET_AND_DIFFERENCE, DISABLED -> diffX;
        };
    }

    public static int getModeY(HealthHudOverlay.DisplayMode mode) {
        return switch (mode) {
            case OWN_HEALTH -> ownHealthY;
            case CROSSHAIR_AND_TARGET -> crosshairTargetY;
            case TARGET_HEALTH -> targetHealthY;
            case OWN_TARGET_AND_DIFFERENCE, DISABLED -> diffY;
        };
    }

    public static void setModePos(HealthHudOverlay.DisplayMode mode, int x, int y) {
        switch (mode) {
            case OWN_HEALTH -> {
                ownHealthX = x;
                ownHealthY = y;
            }
            case CROSSHAIR_AND_TARGET -> {
                crosshairTargetX = x;
                crosshairTargetY = y;
            }
            case TARGET_HEALTH -> {
                targetHealthX = x;
                targetHealthY = y;
            }
            case OWN_TARGET_AND_DIFFERENCE, DISABLED -> {
                diffX = x;
                diffY = y;
            }
        }
    }

    public static void applyPosToAll(int x, int y) {
        ownHealthX = x;
        ownHealthY = y;
        crosshairTargetX = x;
        crosshairTargetY = y;
        targetHealthX = x;
        targetHealthY = y;
        diffX = x;
        diffY = y;
    }

    public static void resetModePos(HealthHudOverlay.DisplayMode mode) {
        setModePos(mode, -1, -1);
    }

    public static void resetAll() {
        applyPosToAll(-1, -1);
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            return;
        }
        int legacyX = -1;
        int legacyY = -1;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(CONFIG_FILE), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                switch (key) {
                    case "own_health_x" -> ownHealthX = Integer.parseInt(value);
                    case "own_health_y" -> ownHealthY = Integer.parseInt(value);
                    case "crosshair_target_x" -> crosshairTargetX = Integer.parseInt(value);
                    case "crosshair_target_y" -> crosshairTargetY = Integer.parseInt(value);
                    case "target_health_x" -> targetHealthX = Integer.parseInt(value);
                    case "target_health_y" -> targetHealthY = Integer.parseInt(value);
                    case "diff_x" -> diffX = Integer.parseInt(value);
                    case "diff_y" -> diffY = Integer.parseInt(value);
                    case "custom_x" -> legacyX = Integer.parseInt(value);
                    case "custom_y" -> legacyY = Integer.parseInt(value);
                    case "target_filter" -> {
                        try {
                            targetFilter = TargetFilter.valueOf(value);
                        } catch (Exception ignored) {
                        }
                    }
                    case "display_mode" -> {
                        try {
                            displayMode = HealthHudOverlay.DisplayMode.valueOf(value);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        if (legacyX >= 0 || legacyY >= 0) {
            if (ownHealthX < 0) ownHealthX = legacyX;
            if (ownHealthY < 0) ownHealthY = legacyY;
            if (crosshairTargetX < 0) crosshairTargetX = legacyX;
            if (crosshairTargetY < 0) crosshairTargetY = legacyY;
            if (targetHealthX < 0) targetHealthX = legacyX;
            if (targetHealthY < 0) targetHealthY = legacyY;
            if (diffX < 0) diffX = legacyX;
            if (diffY < 0) diffY = legacyY;
        }
    }

    public static void save() {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
            return;
        }
        if (saving) {
            return;
        }
        saving = true;
        try {
            File dir = CONFIG_FILE.getParentFile();
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(CONFIG_FILE), StandardCharsets.UTF_8)) {
                writer.write("own_health_x=" + ownHealthX + "\n");
                writer.write("own_health_y=" + ownHealthY + "\n");
                writer.write("crosshair_target_x=" + crosshairTargetX + "\n");
                writer.write("crosshair_target_y=" + crosshairTargetY + "\n");
                writer.write("target_health_x=" + targetHealthX + "\n");
                writer.write("target_health_y=" + targetHealthY + "\n");
                writer.write("diff_x=" + diffX + "\n");
                writer.write("diff_y=" + diffY + "\n");
                writer.write("custom_x=" + getModeX(displayMode) + "\n");
                writer.write("custom_y=" + getModeY(displayMode) + "\n");
                writer.write("target_filter=" + targetFilter.name() + "\n");
                writer.write("display_mode=" + displayMode.name() + "\n");
            } catch (Exception ignored) {
            }
        } finally {
            saving = false;
        }
    }

    public enum TargetFilter {
        ALL_ENTITIES("message.hpreaper.filter.all", "§aВсе"),
        PLAYERS_ONLY("message.hpreaper.filter.players", "§bИгроки"),
        HOSTILE_AND_PLAYERS("message.hpreaper.filter.hostiles", "§6Игроки+Монстры");

        private final String translationKey;
        private final String shortName;

        TargetFilter(String translationKey, String shortName) {
            this.translationKey = translationKey;
            this.shortName = shortName;
        }

        public String translationKey() {
            return translationKey;
        }

        public String getShortName() {
            return shortName;
        }

        public TargetFilter next() {
            return switch (this) {
                case ALL_ENTITIES -> PLAYERS_ONLY;
                case PLAYERS_ONLY -> HOSTILE_AND_PLAYERS;
                case HOSTILE_AND_PLAYERS -> ALL_ENTITIES;
            };
        }
    }
}

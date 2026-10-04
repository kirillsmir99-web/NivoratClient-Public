package dev.nivorat.arc.internal;

import dev.nivorat.arc.ArcMotionProfile;
import dev.nivorat.arc.MorrowConfig;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;

public final class ArcPresetEngine {
    private ArcPresetEngine() {}

    public static void applyPreset(int p) {
        int target = Math.max(0, Math.min(ConfigDomain.i(523124040), p)); 
        MorrowConfig.preset = target;

        if (target == ConfigDomain.i(523124044)) { 
            applyFast();
        } else if (target == ConfigDomain.i(523124045)) { 
            applyMedium();
        } else if (target == ConfigDomain.i(523124046)) { 
            applySafe();
        } else if (target == ConfigDomain.i(523124047)) { 
            applyLearned();
        }
    }

    private static void applyFast() {
        MorrowConfig.placementChance = ConfigDomain.i(523123992); 
        MorrowConfig.legitMode = false;
        MorrowConfig.maxDistance = ConfigDomain.d(4616752568008174797L); 
        MorrowConfig.allowPitPlacement = true;
        MorrowConfig.randomDelay = true;
        MorrowConfig.minDelayMs = ConfigDomain.i(523124079); 
        MorrowConfig.maxDelayMs = ConfigDomain.i(523124094); 
        MorrowConfig.cameraMode = "packet";
        MorrowConfig.autoCamera = false;
        MorrowConfig.cameraSmoothnessMs = ConfigDomain.i(523124091); 
        MorrowConfig.cameraReturn = true;
        MorrowConfig.cameraReturnSmoothnessMs = ConfigDomain.i(523124091); 
        MorrowConfig.cameraCurve = ConfigDomain.i(523124056); 
        MorrowConfig.cameraRandomness = ConfigDomain.i(523124056); 
        MorrowConfig.cameraMouseGcd = true;
        MorrowConfig.adaptiveAim = true;
        MorrowConfig.autonomousPlacement = true;
    }

    private static void applyMedium() {
        MorrowConfig.placementChance = ConfigDomain.i(523123992); 
        MorrowConfig.legitMode = true;
        MorrowConfig.maxDistance = ConfigDomain.d(4616527384189849805L); 
        MorrowConfig.allowPitPlacement = true;
        MorrowConfig.randomDelay = true;
        MorrowConfig.minDelayMs = ConfigDomain.i(523124094); 
        MorrowConfig.maxDelayMs = ConfigDomain.i(523123996); 
        MorrowConfig.cameraMode = "auto";
        MorrowConfig.autoCamera = true;
        MorrowConfig.cameraSmoothnessMs = ConfigDomain.i(523124002); 
        MorrowConfig.cameraReturn = true;
        MorrowConfig.cameraReturnSmoothnessMs = ConfigDomain.i(523124008); 
        MorrowConfig.cameraCurve = ConfigDomain.i(523124068); 
        MorrowConfig.cameraRandomness = ConfigDomain.i(523124079); 
        MorrowConfig.cameraMouseGcd = true;
        MorrowConfig.adaptiveAim = true;
        MorrowConfig.autonomousPlacement = true;
    }

    private static void applySafe() {
        MorrowConfig.placementChance = ConfigDomain.i(523123992); 
        MorrowConfig.legitMode = true;
        MorrowConfig.maxDistance = ConfigDomain.d(4616302200371524813L); 
        MorrowConfig.allowPitPlacement = true;
        MorrowConfig.randomDelay = true;
        MorrowConfig.minDelayMs = ConfigDomain.i(523123987); 
        MorrowConfig.maxDelayMs = ConfigDomain.i(523124171); 
        MorrowConfig.cameraMode = "auto";
        MorrowConfig.autoCamera = true;
        MorrowConfig.cameraSmoothnessMs = ConfigDomain.i(523124216); 
        MorrowConfig.cameraReturn = true;
        MorrowConfig.cameraReturnSmoothnessMs = ConfigDomain.i(523124201); 
        MorrowConfig.cameraCurve = ConfigDomain.i(523124065); 
        MorrowConfig.cameraRandomness = ConfigDomain.i(523124068); 
        MorrowConfig.cameraMouseGcd = true;
        MorrowConfig.adaptiveAim = true;
        MorrowConfig.autonomousPlacement = true;
    }

    private static void applyLearned() {
        ArcMotionProfile prof = ArcMotionProfile.getInstance();
        MorrowConfig.placementChance = ConfigDomain.i(523123992); 
        MorrowConfig.legitMode = true;
        MorrowConfig.maxDistance = ConfigDomain.d(4616527384189849805L); 
        MorrowConfig.allowPitPlacement = true;
        MorrowConfig.randomDelay = true;
        MorrowConfig.minDelayMs = prof.getLearnedMinDelayMs();
        MorrowConfig.maxDelayMs = prof.getLearnedMaxDelayMs();
        MorrowConfig.cameraMode = "auto";
        MorrowConfig.autoCamera = true;
        MorrowConfig.cameraSmoothnessMs = prof.getLearnedCameraSmoothness();
        int offset = ConfigDomain.i(523124038); 
        int floorVal = ConfigDomain.i(523124079); 
        MorrowConfig.cameraReturn = true;
        MorrowConfig.cameraReturnSmoothnessMs = Math.max(floorVal, prof.getLearnedCameraSmoothness() - offset);
        MorrowConfig.cameraCurve = Math.round(prof.getCurvatureBias() * ConfigDomain.f(1750539150)); 
        MorrowConfig.cameraRandomness = Math.round(prof.getTremorVolatility() * ConfigDomain.f(1768233870)); 
        MorrowConfig.cameraMouseGcd = true;
        MorrowConfig.adaptiveAim = true;
        MorrowConfig.autonomousPlacement = true;
    }

    public static void resetDefaults() {
        MorrowConfig.placementChance = ConfigDomain.i(523123992); 
        MorrowConfig.legitMode = true;
        MorrowConfig.randomDelay = true;
        MorrowConfig.preset = ConfigDomain.i(523124045); 
        MorrowConfig.allowPitPlacement = true;
        MorrowConfig.maxDistance = ConfigDomain.d(4616527384189849805L); 
        MorrowConfig.allowSelfCart = false;
        MorrowConfig.minDelayMs = ConfigDomain.i(523124094); 
        MorrowConfig.maxDelayMs = ConfigDomain.i(523123996); 
        MorrowConfig.useMainhandCart = true;
        MorrowConfig.cameraMode = "auto";
        MorrowConfig.autoCamera = true;
        MorrowConfig.cameraSmoothnessMs = ConfigDomain.i(523124002); 
        MorrowConfig.cameraReturn = true;
        MorrowConfig.cameraReturnSmoothnessMs = ConfigDomain.i(523124008); 
        MorrowConfig.cameraCurve = ConfigDomain.i(523124068); 
        MorrowConfig.cameraRandomness = ConfigDomain.i(523124079); 
        MorrowConfig.cameraMouseGcd = true;
        MorrowConfig.adaptiveAim = true;
        MorrowConfig.autonomousPlacement = true;
    }

    public static void load() {
        if (!Files.exists(MorrowConfig.CONFIG_PATH)) {
            return;
        }
        try (InputStream input = Files.newInputStream(MorrowConfig.CONFIG_PATH)) {
            Properties properties = new Properties();
            properties.load(input);
            MorrowConfig.placementChance = clamp(Integer.parseInt(properties.getProperty("placementChance", "100")), 0, 100);
            MorrowConfig.legitMode = Boolean.parseBoolean(properties.getProperty("legitMode", "true"));
            MorrowConfig.randomDelay = Boolean.parseBoolean(properties.getProperty("randomDelay", "true"));
            MorrowConfig.preset = clamp(Integer.parseInt(properties.getProperty("preset", "1")), 0, 4);
            MorrowConfig.allowPitPlacement = Boolean.parseBoolean(properties.getProperty("allowPitPlacement", "true"));
            MorrowConfig.maxDistance = clampDouble(Double.parseDouble(properties.getProperty("maxDistance", "4.4")), 1.5D, 4.5D);
            MorrowConfig.allowSelfCart = Boolean.parseBoolean(properties.getProperty("allowSelfCart", "false"));
            MorrowConfig.minDelayMs = clamp(Integer.parseInt(properties.getProperty("minDelayMs", "50")), 10, 200);
            MorrowConfig.maxDelayMs = clamp(Integer.parseInt(properties.getProperty("maxDelayMs", "80")), 10, 300);
            MorrowConfig.useMainhandCart = Boolean.parseBoolean(properties.getProperty("useMainhandCart", "true"));
            MorrowConfig.cameraMode = properties.getProperty("cameraMode", "auto");
            MorrowConfig.autoCamera = Boolean.parseBoolean(properties.getProperty("autoCamera", "true"));
            MorrowConfig.cameraSmoothnessMs = clamp(Integer.parseInt(properties.getProperty("cameraSmoothnessMs", "110")), 35, 300);
            MorrowConfig.cameraReturn = Boolean.parseBoolean(properties.getProperty("cameraReturn", "true"));
            MorrowConfig.cameraReturnSmoothnessMs = clamp(Integer.parseInt(properties.getProperty("cameraReturnSmoothnessMs", "100")), 35, 300);
            MorrowConfig.cameraCurve = clamp(Integer.parseInt(properties.getProperty("cameraCurve", "40")), 0, 100);
            MorrowConfig.cameraRandomness = clamp(Integer.parseInt(properties.getProperty("cameraRandomness", "35")), 0, 100);
            MorrowConfig.cameraMouseGcd = Boolean.parseBoolean(properties.getProperty("cameraMouseGcd", "true"));
            MorrowConfig.adaptiveAim = Boolean.parseBoolean(properties.getProperty("adaptiveAim", "true"));
            MorrowConfig.autonomousPlacement = Boolean.parseBoolean(properties.getProperty("autonomousPlacement", "true"));
            if (MorrowConfig.maxDelayMs < MorrowConfig.minDelayMs) MorrowConfig.maxDelayMs = MorrowConfig.minDelayMs;
        } catch (Exception ignored) {
            resetDefaults();
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

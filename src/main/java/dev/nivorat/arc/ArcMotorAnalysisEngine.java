package dev.nivorat.arc;

import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public final class ArcMotorAnalysisEngine {
    private final int[] quadrantCounts = new int[8];
    private final int[] magnitudeBins = new int[4];
    private final List<Float> velocitySamples = new ArrayList<>();
    private final List<Long> dt1Samples = new ArrayList<>();
    private final List<Long> dt2Samples = new ArrayList<>();
    private final List<Float> actionPitchDeltas = new ArrayList<>();
    private final List<Float> actionYawDeltas = new ArrayList<>();

    private int slowMovements = 0;
    private int fastMovements = 0;
    private int microAdjustments = 0;
    private int longSweeps = 0;
    private int directionChanges = 0;
    private int accelerationPhases = 0;
    private int decelerationPhases = 0;

    private int playerMovingCameraFrames = 0;
    private int playerStandingCameraFrames = 0;
    private int actionsWhileMoving = 0;
    private int actionsWhileStanding = 0;
    private int directionChangesWhileMoving = 0;

    private int bowReleases = 0;
    private int railPlacements = 0;
    private int cartPlacements = 0;
    private int successfulDetonations = 0;
    private int completedSequences = 0;
    private int distinctSituations = 0;

    private int validSamplesCount = 0;
    private int rejectedSamplesCount = 0;
    private float totalQualityScoreSum = 0.0f;

    private float previousVelocity = 0.0f;
    private float previousDeltaYaw = 0.0f;
    private float previousDeltaPitch = 0.0f;
    private int totalFrames = 0;

    public synchronized void recordFrame(ArcMotorFrame frame) {
        if (frame == null) return;
        totalFrames++;

        float mag = frame.magnitude();
        float vel = frame.velocity();
        float dPitch = frame.deltaPitch();
        float dYaw = frame.deltaYaw();

        int quad = frame.quadrant();
        if (quad >= 0 && quad < 8) {
            quadrantCounts[quad]++;
        }

        if (mag < 3.0f) {
            magnitudeBins[0]++;
        } else if (mag < 15.0f) {
            magnitudeBins[1]++;
        } else if (mag < 45.0f) {
            magnitudeBins[2]++;
        } else {
            magnitudeBins[3]++;
        }

        if (velocitySamples.size() < 500) {
            velocitySamples.add(vel);
        }

        if (vel < 45.0f && mag > 0.05f) {
            slowMovements++;
        } else if (vel >= 150.0f) {
            fastMovements++;
        }

        if (mag >= 0.1f && mag <= 3.5f) {
            microAdjustments++;
        } else if (mag >= 20.0f) {
            longSweeps++;
        }

        if (totalFrames > 1) {
            if ((dYaw > 0.5f && previousDeltaYaw < -0.5f) || (dYaw < -0.5f && previousDeltaYaw > 0.5f)
                    || (dPitch > 0.5f && previousDeltaPitch < -0.5f) || (dPitch < -0.5f && previousDeltaPitch > 0.5f)) {
                directionChanges++;
                if (frame.playerMoving()) {
                    directionChangesWhileMoving++;
                }
            }

            float accel = (vel - previousVelocity);
            if (accel > 150.0f) {
                accelerationPhases++;
            } else if (accel < -150.0f) {
                decelerationPhases++;
            }
        }

        previousVelocity = vel;
        previousDeltaYaw = dYaw;
        previousDeltaPitch = dPitch;

        if (frame.playerMoving()) {
            playerMovingCameraFrames++;
        } else {
            playerStandingCameraFrames++;
        }
    }

    public synchronized void recordBowRelease(boolean playerMoving) {
        bowReleases++;
        if (playerMoving) {
            actionsWhileMoving++;
        } else {
            actionsWhileStanding++;
        }
    }

    public synchronized void recordBowRelease() {
        recordBowRelease(false);
    }

    public synchronized void recordRailPlacement(long dtFromBowMs, boolean playerMoving, float pitchDelta, float yawDelta) {
        railPlacements++;
        if (playerMoving) {
            actionsWhileMoving++;
        } else {
            actionsWhileStanding++;
        }

        float quality = evaluateTimingQuality(dtFromBowMs);
        if (quality >= 0.4f) {
            validSamplesCount++;
            totalQualityScoreSum += quality;
            if (dtFromBowMs >= ArcActionValidator.MIN_ACTION_DELAY_MS && dtFromBowMs <= ArcActionValidator.MAX_ACTION_DELAY_MS) {
                if (dt1Samples.size() < 100) {
                    dt1Samples.add(dtFromBowMs);
                }
            }
            if (actionPitchDeltas.size() < 100 && Float.isFinite(pitchDelta)) {
                actionPitchDeltas.add(Math.abs(pitchDelta));
            }
            if (actionYawDeltas.size() < 100 && Float.isFinite(yawDelta)) {
                actionYawDeltas.add(Math.abs(yawDelta));
            }
            AutoCartLogger.logSampleQuality("rail", true, quality, "valid_timing");
        } else {
            rejectedSamplesCount++;
            AutoCartLogger.logSampleQuality("rail", false, quality, "timing_outlier");
        }
    }

    public synchronized void recordRailPlacement(long dtFromBowMs) {
        recordRailPlacement(dtFromBowMs, false, 0.0f, 0.0f);
    }

    public synchronized void recordCartPlacement(long dtFromRailMs, boolean playerMoving, float pitchDelta, float yawDelta) {
        cartPlacements++;
        if (playerMoving) {
            actionsWhileMoving++;
        } else {
            actionsWhileStanding++;
        }

        float quality = evaluateTimingQuality(dtFromRailMs);
        if (quality >= 0.4f) {
            validSamplesCount++;
            totalQualityScoreSum += quality;
            if (dtFromRailMs >= ArcActionValidator.MIN_ACTION_DELAY_MS && dtFromRailMs <= ArcActionValidator.MAX_ACTION_DELAY_MS) {
                if (dt2Samples.size() < 100) {
                    dt2Samples.add(dtFromRailMs);
                }
            }
            if (actionPitchDeltas.size() < 100 && Float.isFinite(pitchDelta)) {
                actionPitchDeltas.add(Math.abs(pitchDelta));
            }
            if (actionYawDeltas.size() < 100 && Float.isFinite(yawDelta)) {
                actionYawDeltas.add(Math.abs(yawDelta));
            }
            completedSequences++;
            AutoCartLogger.logSampleQuality("cart", true, quality, "valid_timing");
        } else {
            rejectedSamplesCount++;
            AutoCartLogger.logSampleQuality("cart", false, quality, "timing_outlier");
        }
    }

    public synchronized void recordCartPlacement(long dtFromRailMs) {
        recordCartPlacement(dtFromRailMs, false, 0.0f, 0.0f);
    }

    public synchronized void recordDetonation() {
        successfulDetonations++;
        validSamplesCount++;
        totalQualityScoreSum += 1.0f;
    }

    public synchronized void recordSituation(double distance) {
        if (distance > 3.2) {
            distinctSituations |= 1;
        } else {
            distinctSituations |= 2;
        }
    }

    private static float evaluateTimingQuality(long dtMs) {
        if (dtMs < ArcActionValidator.MIN_ACTION_DELAY_MS || dtMs > 3000L) {
            return 0.1f;
        }
        if (dtMs >= 35L && dtMs <= 400L) {
            return 1.0f;
        }
        if (dtMs <= 800L) {
            return 0.75f;
        }
        return 0.45f;
    }

    public synchronized float computeCameraCoverage() {
        if (totalFrames == 0) return 0.0f;
        float sSlow = Math.min(1.0f, slowMovements / 15.0f);
        float sFast = Math.min(1.0f, fastMovements / 10.0f);
        float sMicro = Math.min(1.0f, microAdjustments / 15.0f);
        float sSweeps = Math.min(1.0f, longSweeps / 6.0f);
        float sDir = Math.min(1.0f, directionChanges / 8.0f);
        float sAccel = Math.min(1.0f, accelerationPhases / 6.0f);
        float sDecel = Math.min(1.0f, decelerationPhases / 6.0f);

        float kin = computeKinematicCoverage();
        float dyn = (sSlow + sFast + sMicro + sSweeps + sDir + sAccel + sDecel) / 7.0f;
        return MathHelper.clamp(kin * 0.5f + dyn * 0.5f, 0.0f, 1.0f);
    }

    public synchronized float computeKinematicCoverage() {
        int activeQuads = 0;
        for (int c : quadrantCounts) {
            if (c >= 2) activeQuads++;
        }
        float sQuad = activeQuads / 8.0f;
        int activeBins = 0;
        for (int b : magnitudeBins) {
            if (b >= 2) activeBins++;
        }
        float sBin = activeBins / 4.0f;
        float sVel = Math.min(1.0f, velocitySamples.size() / 20.0f);
        return MathHelper.clamp(sQuad * 0.5f + sBin * 0.25f + sVel * 0.25f, 0.0f, 1.0f);
    }

    public synchronized float computeActionCoverage() {
        float bowScore = Math.min(1.0f, bowReleases / 3.0f);
        float railScore = Math.min(1.0f, railPlacements / 3.0f);
        float cartScore = Math.min(1.0f, cartPlacements / 3.0f);
        float detScore = Math.min(1.0f, successfulDetonations / 2.0f);
        return (bowScore + railScore + cartScore + detScore) / 4.0f;
    }

    public synchronized float computeActionTimingCoverage() {
        float bScore = Math.min(1.0f, bowReleases / 3.0f);
        float rScore = Math.min(1.0f, dt1Samples.size() / 3.0f);
        float cScore = Math.min(1.0f, dt2Samples.size() / 3.0f);
        float seqScore = Math.min(1.0f, completedSequences / 3.0f);
        return (bScore + rScore + cScore + seqScore) / 4.0f;
    }

    public synchronized float computeTimingConsistency() {
        int pairs = dt1Samples.size() + dt2Samples.size();
        if (pairs == 0) return 0.0f;

        float pairVolumeScore = Math.min(1.0f, pairs / 6.0f);
        float stability1 = computeStability(dt1Samples);
        float stability2 = computeStability(dt2Samples);
        float meanStability = (dt1Samples.isEmpty() ? 0.5f : stability1) * 0.5f + (dt2Samples.isEmpty() ? 0.5f : stability2) * 0.5f;

        return MathHelper.clamp(pairVolumeScore * 0.6f + meanStability * 0.4f, 0.0f, 1.0f);
    }

    public synchronized float computeCombinedMovementCoverage() {
        float sMoving = Math.min(1.0f, playerMovingCameraFrames / 20.0f);
        float sStanding = Math.min(1.0f, playerStandingCameraFrames / 10.0f);
        float sActMoving = Math.min(1.0f, actionsWhileMoving / 2.0f);
        float sActStanding = Math.min(1.0f, actionsWhileStanding / 2.0f);
        float sTurnMoving = Math.min(1.0f, directionChangesWhileMoving / 4.0f);
        return (sMoving + sStanding + sActMoving + sActStanding + sTurnMoving) / 5.0f;
    }

    public synchronized float computeRealActionSamplesCoverage() {
        float bowS = Math.min(1.0f, bowReleases / 4.0f);
        float railS = Math.min(1.0f, dt1Samples.size() / 4.0f);
        float cartS = Math.min(1.0f, dt2Samples.size() / 4.0f);
        float detS = Math.min(1.0f, successfulDetonations / 2.0f);
        float sitS = Integer.bitCount(distinctSituations) / 2.0f;
        return (bowS * 0.25f) + (railS * 0.25f) + (cartS * 0.25f) + (detS * 0.15f) + (sitS * 0.10f);
    }

    public synchronized float computeStabilityScore() {
        float timingStab = computeTimingConsistency();
        int totalRecorded = validSamplesCount + rejectedSamplesCount;
        float cleanRatio = totalRecorded > 0 ? (float) validSamplesCount / (float) totalRecorded : 0.0f;
        float confidence = Math.min(1.0f, validSamplesCount / 8.0f);
        return MathHelper.clamp((timingStab * 0.4f) + (cleanRatio * 0.3f) + (confidence * 0.3f), 0.0f, 1.0f);
    }

    public synchronized float computeVolumeCoverage() {
        return MathHelper.clamp(totalFrames / 30.0f, 0.0f, 1.0f);
    }

    public synchronized float computeRawMastery() {
        float cam = computeCameraCoverage();
        float timing = computeActionTimingCoverage();
        float combined = computeCombinedMovementCoverage();
        float realSamples = computeRealActionSamplesCoverage();
        float stability = computeStabilityScore();

        return (cam * 0.20f) + (timing * 0.25f) + (combined * 0.20f) + (realSamples * 0.25f) + (stability * 0.10f);
    }

    public synchronized int computeMasteryPercent(long elapsedMs, long targetDurationMs) {
        float c1 = computeCameraCoverage();
        float c2 = computeActionTimingCoverage();
        float c3 = computeCombinedMovementCoverage();
        float c4 = computeRealActionSamplesCoverage();
        float c5 = computeStabilityScore();

        float raw = (c1 * 0.20f) + (c2 * 0.25f) + (c3 * 0.20f) + (c4 * 0.25f) + (c5 * 0.10f);
        long safeTarget = Math.max(30000L, targetDurationMs);
        float timeRatio = MathHelper.clamp((float) elapsedMs / (float) safeTarget, 0.0f, 1.0f);

        int mastery = Math.round(raw * 100.0f);

        if (c4 <= 0.05f) {
            mastery = Math.min(mastery, 25);
        } else if (c4 < 0.25f) {
            mastery = Math.min(mastery, 35);
        } else if (c2 < 0.3f || c4 < 0.3f) {
            mastery = Math.min(mastery, 50);
        } else if (c2 < 0.5f || c4 < 0.5f) {
            mastery = Math.min(mastery, 70);
        }

        if (timeRatio < 0.80f || c1 < 0.9f || c2 < 0.9f || c3 < 0.9f || c4 < 0.9f || c5 < 0.9f) {
            mastery = Math.min(mastery, 95);
        }

        int finalMastery = MathHelper.clamp(mastery, 0, 100);
        AutoCartLogger.logMasteryCategories(c1, c2, c3, c4, c5, finalMastery);
        return finalMastery;
    }

    public synchronized int computeMasteryPercent() {
        ArcMotionProfile profile = ArcMotionProfile.getInstance();
        long elapsed = profile.getCalibrationElapsedMs();
        long target = profile.getTargetCalibrationDurationMs();
        return computeMasteryPercent(elapsed, target);
    }

    private static float computeStability(List<Long> samples) {
        if (samples.size() < 2) return 0.5f;
        double mean = 0.0;
        for (long s : samples) mean += s;
        mean /= samples.size();
        double variance = 0.0;
        for (long s : samples) {
            double d = s - mean;
            variance += d * d;
        }
        double stdDev = Math.sqrt(variance / samples.size());
        double cv = mean > 0.0 ? (stdDev / mean) : 1.0;
        return (float) MathHelper.clamp(1.0 - cv * 0.75, 0.0, 1.0);
    }

    public synchronized float getMeanActionPitchDelta() {
        if (actionPitchDeltas.isEmpty()) return 4.5f;
        float sum = 0.0f;
        for (float f : actionPitchDeltas) sum += f;
        return sum / actionPitchDeltas.size();
    }

    public synchronized float getMaxActionPitchDelta() {
        if (actionPitchDeltas.isEmpty()) return 12.0f;
        float max = 0.0f;
        for (float f : actionPitchDeltas) {
            if (f > max) max = f;
        }
        return Math.max(5.0f, max);
    }

    public synchronized float getMeanActionYawDelta() {
        if (actionYawDeltas.isEmpty()) return 6.0f;
        float sum = 0.0f;
        for (float f : actionYawDeltas) sum += f;
        return sum / actionYawDeltas.size();
    }

    public synchronized int getValidSamplesCount() {
        return validSamplesCount;
    }

    public synchronized int getRejectedSamplesCount() {
        return rejectedSamplesCount;
    }

    public synchronized float getAverageSampleQuality() {
        int total = validSamplesCount + rejectedSamplesCount;
        return total > 0 ? (totalQualityScoreSum / total) : 0.0f;
    }

    public synchronized List<String> getMissingDataHints() {
        List<String> hints = new ArrayList<>();
        if (totalFrames < 10) {
            hints.add("Двигайте мышью для сбора моторики");
        } else {
            float cam = computeCameraCoverage();
            if (cam < 0.5f) {
                hints.add("Двигайте камеру: делайте плавные проводки и резкие переводы");
            }
        }
        if (computeCombinedMovementCoverage() < 0.4f) {
            hints.add("Двигайтесь персонажем: бегайте и прыгайте во время обзора");
        }
        if (bowReleases < 3) {
            hints.add("Сделайте выстрел из лука (" + bowReleases + "/3)");
        }
        if (railPlacements < 3) {
            hints.add("Установите рельсу (" + railPlacements + "/3)");
        }
        if (cartPlacements < 3) {
            hints.add("Поставьте вагонетку (" + cartPlacements + "/3)");
        }
        if (successfulDetonations < 2) {
            hints.add("Взорвите вагонетку (" + successfulDetonations + "/2)");
        }
        return hints;
    }

    public synchronized void reset() {
        for (int i = 0; i < quadrantCounts.length; i++) quadrantCounts[i] = 0;
        for (int i = 0; i < magnitudeBins.length; i++) magnitudeBins[i] = 0;
        velocitySamples.clear();
        dt1Samples.clear();
        dt2Samples.clear();
        actionPitchDeltas.clear();
        actionYawDeltas.clear();
        slowMovements = 0;
        fastMovements = 0;
        microAdjustments = 0;
        longSweeps = 0;
        directionChanges = 0;
        accelerationPhases = 0;
        decelerationPhases = 0;
        playerMovingCameraFrames = 0;
        playerStandingCameraFrames = 0;
        actionsWhileMoving = 0;
        actionsWhileStanding = 0;
        directionChangesWhileMoving = 0;
        bowReleases = 0;
        railPlacements = 0;
        cartPlacements = 0;
        successfulDetonations = 0;
        completedSequences = 0;
        distinctSituations = 0;
        validSamplesCount = 0;
        rejectedSamplesCount = 0;
        totalQualityScoreSum = 0.0f;
        previousVelocity = 0.0f;
        previousDeltaYaw = 0.0f;
        previousDeltaPitch = 0.0f;
        totalFrames = 0;
    }

    public synchronized float getAverageActionPitchDelta() {
        if (actionPitchDeltas.isEmpty()) return 0.0f;
        float sum = 0.0f;
        for (float d : actionPitchDeltas) sum += d;
        return sum / actionPitchDeltas.size();
    }
}

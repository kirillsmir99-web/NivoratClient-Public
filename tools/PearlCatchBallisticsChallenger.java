package tools;

import dev.kinetictweaks.trajectory.PearlCatchTrajectory;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * Challenger empirical stress harness executing directly against compiled
 * dev.kinetictweaks.trajectory.PearlCatchTrajectory.
 */
public class PearlCatchBallisticsChallenger {

    record TestCase(String group, String name, Vec3d vel, float yaw, boolean onGround) {}

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("JAVA EMPIRICAL CHALLENGER: dev.kinetictweaks.trajectory.PearlCatchTrajectory");
        System.out.println("Executing directly on JVM against compiled bytecode (delay=2, threshold <= 0.5 bl)");
        System.out.println("================================================================================");

        List<TestCase> cases = buildSystematicCases();
        System.out.printf("\n[PHASE 1] Executing %d Systematic Test Cases in Java...\n", cases.size());

        Map<String, List<Double>> groupErrors = new LinkedHashMap<>();
        int systematicFailures = 0;

        for (TestCase tc : cases) {
            PearlCatchTrajectory.Solution sol = PearlCatchTrajectory.solve3D(2, tc.yaw, tc.vel, tc.onGround, -1.0f);
            double[] dists = simulateIndependentFlight(sol, 2, tc.yaw, tc.vel, tc.onGround);
            double distToBurst = dists[0];
            double distToCenter = dists[1];
            double err = Math.min(distToBurst, distToCenter);

            groupErrors.computeIfAbsent(tc.group, k -> new ArrayList<>()).add(err);

            if (err > 0.5 || sol.residualError() > 0.5 || !sol.valid() || Double.isNaN(err) || Double.isInfinite(err)) {
                systematicFailures++;
                System.err.printf("FAILURE in %s: err=%.6f bl, residual=%.6f bl, valid=%b\n",
                        tc.name, err, sol.residualError(), sol.valid());
            }
        }

        System.out.println("\n--- Java Systematic Groups Summary ---");
        System.out.printf("%-22s | %-5s | %-11s | %-11s | %s\n", "Group", "Count", "Max Err", "Mean Err", "Status");
        System.out.println("-------------------------------------------------------------------------");
        for (Map.Entry<String, List<Double>> entry : groupErrors.entrySet()) {
            double maxErr = entry.getValue().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            double meanErr = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            String status = maxErr <= 0.5 ? "PASS (<= 0.5)" : "FAIL (> 0.5)";
            System.out.printf("%-22s | %-5d | %-8.6f bl | %-8.6f bl | %s\n",
                    entry.getKey(), entry.getValue().size(), maxErr, meanErr, status);
        }
        System.out.printf("\nSystematic Failures (> 0.5 blocks): %d / %d\n", systematicFailures, cases.size());

        // Phase 2: 10,000 Monte Carlo samples in Java
        System.out.println("\n================================================================================");
        System.out.println("[PHASE 2] Executing 10,000 Java Monte Carlo Stress Cases (delay=2)...");
        Random rng = new Random(1337L);
        int mcSamples = 10000;
        double[] mcErrors = new double[mcSamples];
        int mcFailures = 0;
        long t0 = System.currentTimeMillis();

        for (int i = 0; i < mcSamples; i++) {
            float yaw = (float) (rng.nextDouble() * 360.0 - 180.0);
            boolean onGround = rng.nextBoolean();
            double hSpeed = rng.nextDouble() * 0.45;
            double hAngle = rng.nextDouble() * 2.0 * Math.PI;
            double vx = hSpeed * Math.cos(hAngle);
            double vz = hSpeed * Math.sin(hAngle);
            double vy = onGround ? 0.0 : (rng.nextDouble() * 3.0 - 1.2); // vy in [-1.2, 1.8]

            Vec3d vel = new Vec3d(vx, vy, vz);
            PearlCatchTrajectory.Solution sol = PearlCatchTrajectory.solve3D(2, yaw, vel, onGround, -1.0f);
            double[] dists = simulateIndependentFlight(sol, 2, yaw, vel, onGround);
            double err = Math.min(dists[0], dists[1]);
            mcErrors[i] = err;

            if (err > 0.5 || sol.residualError() > 0.5 || !sol.valid() || Double.isNaN(err) || Double.isInfinite(err)) {
                mcFailures++;
            }
        }
        long elapsed = System.currentTimeMillis() - t0;
        System.out.printf("Executed %d Java Monte Carlo samples in %d ms\n", mcSamples, elapsed);

        Arrays.sort(mcErrors);
        double minMc = mcErrors[0];
        double p50Mc = mcErrors[(int) (mcSamples * 0.50)];
        double meanMc = Arrays.stream(mcErrors).average().orElse(0.0);
        double p95Mc = mcErrors[(int) (mcSamples * 0.95)];
        double p99Mc = mcErrors[(int) (mcSamples * 0.99)];
        double p999Mc = mcErrors[(int) (mcSamples * 0.999)];
        double maxMc = mcErrors[mcSamples - 1];

        System.out.println("\n--- Java Monte Carlo Statistical Distribution ---");
        System.out.printf("  Min Error:          %.6f blocks\n", minMc);
        System.out.printf("  50th Percentile:    %.6f blocks\n", p50Mc);
        System.out.printf("  Mean Error:         %.6f blocks\n", meanMc);
        System.out.printf("  95th Percentile:    %.6f blocks\n", p95Mc);
        System.out.printf("  99th Percentile:    %.6f blocks\n", p99Mc);
        System.out.printf("  99.9th Percentile:  %.6f blocks\n", p999Mc);
        System.out.printf("  Max Error (p100):   %.6f blocks\n", maxMc);
        System.out.printf("  Threshold (<= 0.5): %s\n", (maxMc <= 0.5 ? "MET" : "VIOLATED"));
        System.out.printf("  Monte Carlo Failures: %d / %d\n", mcFailures, mcSamples);

        // Phase 3: Explicit Required Movement Profiles
        System.out.println("\n================================================================================");
        System.out.println("[PHASE 3] Explicit Verification of Required Motion Profiles");
        System.out.println("================================================================================");

        List<TestCase> keyCases = List.of(
                new TestCase("Key", "Stationary (v=0, onGround=true)", Vec3d.ZERO, 0.0f, true),
                new TestCase("Key", "Stationary Airborne (v=0, onGround=false)", Vec3d.ZERO, 0.0f, false),
                new TestCase("Key", "Forward Sprint (vz=0.28, yaw=0.0)", new Vec3d(0.0, 0.0, 0.28), 0.0f, true),
                new TestCase("Key", "Diagonal Sprint (vx=0.2, vz=0.2, yaw=0.0)", new Vec3d(0.2, 0.0, 0.2), 0.0f, true),
                new TestCase("Key", "Weak Wind Jump (vy=0.1, onGround=false)", new Vec3d(0.0, 0.1, 0.0), 0.0f, false),
                new TestCase("Key", "Weak Wind Jump (vy=0.4, onGround=false)", new Vec3d(0.0, 0.4, 0.0), 0.0f, false),
                new TestCase("Key", "Standard Wind Jump (vy=0.9, onGround=false)", new Vec3d(0.0, 0.9, 0.0), 0.0f, false),
                new TestCase("Key", "Max Boost Wind Jump (vy=1.45, onGround=false)", new Vec3d(0.0, 1.45, 0.0), 0.0f, false),
                new TestCase("Key", "Extreme Boost Wind Jump (vy=1.80, onGround=false)", new Vec3d(0.0, 1.80, 0.0), 0.0f, false),
                new TestCase("Key", "Falling Slow (vy=-0.1, onGround=false)", new Vec3d(0.0, -0.1, 0.0), 0.0f, false),
                new TestCase("Key", "Falling Moderate (vy=-0.4, onGround=false)", new Vec3d(0.0, -0.4, 0.0), 0.0f, false),
                new TestCase("Key", "Falling Fast (vy=-0.8, onGround=false)", new Vec3d(0.0, -0.8, 0.0), 0.0f, false),
                new TestCase("Key", "Falling Terminal (vy=-1.2, onGround=false)", new Vec3d(0.0, -1.2, 0.0), 0.0f, false)
        );

        for (TestCase tc : keyCases) {
            PearlCatchTrajectory.Solution sol = PearlCatchTrajectory.solve3D(2, tc.yaw, tc.vel, tc.onGround, -1.0f);
            double[] dists = simulateIndependentFlight(sol, 2, tc.yaw, tc.vel, tc.onGround);
            double minD = Math.min(dists[0], dists[1]);
            System.out.printf("Profile: %-46s\n", tc.name);
            System.out.printf("  PearlPitch=%6.2f°, WindPitch=%6.2f°, WindYaw=%6.2f°\n",
                    sol.pearlPitch(), sol.windPitch(), sol.windYaw());
            System.out.printf("  InterceptTick=%d, SolverResidual=%.6f bl\n", sol.interceptTick(), sol.residualError());
            System.out.printf("  Independent DistToBurst=%.6f bl, DistToCenter=%.6f bl -> MinDist=%.6f bl\n",
                    dists[0], dists[1], minD);
            System.out.printf("  Verdict: %s\n\n", (minD <= 0.5 ? "PASS" : "FAIL"));
        }

        System.out.println("================================================================================");
        System.out.printf("TOTAL JAVA EVALUATIONS: %d\n", cases.size() + mcSamples);
        System.out.printf("TOTAL FAILURES (> 0.5 blocks): %d\n", systematicFailures + mcFailures);
        if (systematicFailures == 0 && mcFailures == 0 && maxMc <= 0.5) {
            System.out.println("OVERALL JVM EMPIRICAL VERDICT: CONFIRM_CORRECTNESS");
        } else {
            System.out.println("OVERALL JVM EMPIRICAL VERDICT: REJECT");
        }
        System.out.println("================================================================================");

        if (systematicFailures > 0 || mcFailures > 0 || maxMc > 0.5) {
            System.exit(1);
        }
    }

    private static double[] simulateIndependentFlight(
            PearlCatchTrajectory.Solution sol,
            int delayTicks,
            float playerYaw,
            Vec3d playerVel,
            boolean onGround
    ) {
        double vertSpeed = onGround ? 0.0 : playerVel.y;
        Vec3d inheritedPearlVel = new Vec3d(playerVel.x, vertSpeed, playerVel.z);
        Vec3d inheritedWindVel = new Vec3d(playerVel.x, vertSpeed, playerVel.z);

        double plY = 0.0;
        double curVy = vertSpeed;
        for (int d = 0; d < delayTicks; d++) {
            plY += curVy;
            curVy = (curVy - 0.08) * 0.98;
        }
        Vec3d windOrigin = new Vec3d(playerVel.x * delayTicks, plY, playerVel.z * delayTicks);

        Vec3d pearlDir = PearlCatchTrajectory.getDirectionVector(sol.pearlPitch(), playerYaw);
        Vec3d pearlVel = pearlDir.multiply(PearlCatchTrajectory.PEARL_SPEED).add(inheritedPearlVel);
        Vec3d pearlPos = Vec3d.ZERO;

        for (int t = 1; t <= sol.interceptTick(); t++) {
            pearlPos = pearlPos.add(pearlVel);
            pearlVel = new Vec3d(
                    pearlVel.x * PearlCatchTrajectory.PEARL_DRAG,
                    (pearlVel.y - PearlCatchTrajectory.PEARL_GRAVITY) * PearlCatchTrajectory.PEARL_DRAG,
                    pearlVel.z * PearlCatchTrajectory.PEARL_DRAG
            );
        }

        Vec3d burstTarget = pearlPos.subtract(0.0, PearlCatchTrajectory.BURST_OFFSET_Y, 0.0);

        int flightTicks = sol.interceptTick() - delayTicks;
        Vec3d windDir = PearlCatchTrajectory.getDirectionVector(sol.windPitch(), sol.windYaw());
        Vec3d windVel = windDir.multiply(PearlCatchTrajectory.WIND_CHARGE_SPEED).add(inheritedWindVel);
        Vec3d windArrivalPos = windOrigin.add(windVel.multiply(flightTicks));

        double distToBurst = windArrivalPos.distanceTo(burstTarget);
        double distToCenter = windArrivalPos.distanceTo(pearlPos);

        return new double[]{distToBurst, distToCenter};
    }

    private static List<TestCase> buildSystematicCases() {
        List<TestCase> cases = new ArrayList<>();

        // 1. Stationary
        for (float yaw : new float[]{0.0f, 45.0f, 90.0f, 180.0f, 270.0f, -90.0f}) {
            for (boolean ground : new boolean[]{true, false}) {
                cases.add(new TestCase("Stationary", String.format("Stationary yaw=%.0f ground=%b", yaw, ground),
                        Vec3d.ZERO, yaw, ground));
            }
        }

        // 2. Forward Sprinting
        for (double speed : new double[]{0.20, 0.25, 0.28, 0.35}) {
            for (float yaw : new float[]{0.0f, 45.0f, 90.0f, 180.0f, -90.0f}) {
                double rad = Math.toRadians(yaw);
                double vx = -Math.sin(rad) * speed;
                double vz = Math.cos(rad) * speed;
                cases.add(new TestCase("ForwardSprint", String.format("ForwardSprint speed=%.2f yaw=%.0f", speed, yaw),
                        new Vec3d(vx, 0.0, vz), yaw, true));
            }
        }

        // 3. Diagonal Sprinting
        double[][] diagSpeeds = new double[][]{
                {0.2, 0.2}, {-0.2, 0.2}, {0.2, -0.2}, {-0.2, -0.2},
                {0.28 / Math.sqrt(2), 0.28 / Math.sqrt(2)},
                {-0.28 / Math.sqrt(2), 0.28 / Math.sqrt(2)}
        };
        for (double[] ds : diagSpeeds) {
            for (float yaw : new float[]{0.0f, 45.0f, 90.0f}) {
                cases.add(new TestCase("DiagonalSprint", String.format("DiagSprint vx=%.3f vz=%.3f yaw=%.0f", ds[0], ds[1], yaw),
                        new Vec3d(ds[0], 0.0, ds[1]), yaw, true));
            }
        }

        // 4. Weak Wind Jump (vy = 0.1, 0.2, 0.3, 0.4)
        for (double vy : new double[]{0.1, 0.2, 0.3, 0.4}) {
            for (double hSpeed : new double[]{0.0, 0.15, 0.28}) {
                for (float yaw : new float[]{0.0f, 90.0f}) {
                    double rad = Math.toRadians(yaw);
                    double vx = -Math.sin(rad) * hSpeed;
                    double vz = Math.cos(rad) * hSpeed;
                    cases.add(new TestCase("WeakWindJump", String.format("WeakWindJump vy=%.2f h=%.2f yaw=%.0f", vy, hSpeed, yaw),
                            new Vec3d(vx, vy, vz), yaw, false));
                }
            }
        }

        // 5. Standard Wind Jump (vy = 0.6, 0.8, 0.9, 1.0)
        for (double vy : new double[]{0.6, 0.8, 0.9, 1.0}) {
            for (double hSpeed : new double[]{0.0, 0.15, 0.28}) {
                for (float yaw : new float[]{0.0f, 45.0f, 90.0f}) {
                    double rad = Math.toRadians(yaw);
                    double vx = -Math.sin(rad) * hSpeed;
                    double vz = Math.cos(rad) * hSpeed;
                    cases.add(new TestCase("StandardWindJump", String.format("StandardWindJump vy=%.2f h=%.2f yaw=%.0f", vy, hSpeed, yaw),
                            new Vec3d(vx, vy, vz), yaw, false));
                }
            }
        }

        // 6. Max Boost Wind Jump (vy = 1.2, 1.45, 1.6, 1.8)
        for (double vy : new double[]{1.2, 1.45, 1.6, 1.8}) {
            for (double hSpeed : new double[]{0.0, 0.15, 0.28}) {
                for (float yaw : new float[]{0.0f, 45.0f, 90.0f}) {
                    double rad = Math.toRadians(yaw);
                    double vx = -Math.sin(rad) * hSpeed;
                    double vz = Math.cos(rad) * hSpeed;
                    cases.add(new TestCase("MaxBoostWindJump", String.format("MaxBoostWindJump vy=%.2f h=%.2f yaw=%.0f", vy, hSpeed, yaw),
                            new Vec3d(vx, vy, vz), yaw, false));
                }
            }
        }

        // 7. Falling (vy = -0.1, -0.2, -0.4, -0.6, -0.8, -1.0, -1.2)
        for (double vy : new double[]{-0.1, -0.2, -0.4, -0.6, -0.8, -1.0, -1.2}) {
            for (double hSpeed : new double[]{0.0, 0.15, 0.28}) {
                for (float yaw : new float[]{0.0f, 45.0f, 90.0f}) {
                    double rad = Math.toRadians(yaw);
                    double vx = -Math.sin(rad) * hSpeed;
                    double vz = Math.cos(rad) * hSpeed;
                    cases.add(new TestCase("Falling", String.format("Falling vy=%.2f h=%.2f yaw=%.0f", vy, hSpeed, yaw),
                            new Vec3d(vx, vy, vz), yaw, false));
                }
            }
        }

        // 8. Lateral Strafe
        for (double strafe : new double[]{-0.28, 0.28}) {
            cases.add(new TestCase("LateralStrafe", String.format("LateralStrafe vx=%.2f yaw=0.0", strafe),
                    new Vec3d(strafe, 0.0, 0.0), 0.0f, true));
        }

        return cases;
    }
}

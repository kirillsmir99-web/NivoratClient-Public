import os

# 1. RaycastTrajectory.java
path_traj = r"V:\src\main\java\dev\raycast\RaycastTrajectory.java"
with open(path_traj, "r", encoding="utf-8") as f:
    text = f.read()

text = text.replace(
    "    public static final double PEARL_SPEED = 1.5;\n"
    "    public static final double PEARL_GRAVITY = 0.03;\n"
    "    public static final double PEARL_DRAG = 0.99;\n"
    "    public static final double WIND_CHARGE_SPEED = 1.5;\n"
    "    public static final double BURST_OFFSET_Y = 0.38;",
    "    public static final double PEARL_SPEED = Obf.d(0x65843D1E5A7C3D1EL); // 1.5\n"
    "    public static final double PEARL_GRAVITY = Obf.d(0x65E2854FB1F923A6L); // 0.03\n"
    "    public static final double PEARL_DRAG = Obf.d(0x6593930A209D7AB0L); // 0.99\n"
    "    public static final double WIND_CHARGE_SPEED = Obf.d(0x65843D1E5A7C3D1EL); // 1.5\n"
    "    public static final double BURST_OFFSET_Y = Obf.d(0x65A46CF5DF62854CL); // 0.38"
)

text = text.replace(
    "        double previousTime = .25;\n"
    "        double previousError = speedError(pearlOrigin, pearlVelocity, pearlAge, windOrigin, inheritedWindVelocity, previousTime);\n"
    "        double bestTime = previousTime;\n"
    "        double bestError = Math.abs(previousError);\n"
    "        for (double time = .5; time <= 30; time += .25) {\n"
    "            double error = speedError(pearlOrigin, pearlVelocity, pearlAge, windOrigin, inheritedWindVelocity, time);\n"
    "            if (previousError * error <= 0) {\n"
    "                double lo = previousTime, hi = time;\n"
    "                for (int iteration = 0; iteration < 28; iteration++) {\n"
    "                    double mid = (lo + hi) * .5;\n"
    "                    double midError = speedError(pearlOrigin, pearlVelocity, pearlAge, windOrigin, inheritedWindVelocity, mid);\n"
    "                    if (previousError * midError <= 0) hi = mid;\n"
    "                    else { lo = mid; previousError = midError; }\n"
    "                }\n"
    "                bestTime = (lo + hi) * .5;\n"
    "                bestError = 0;\n"
    "                break;\n"
    "            }\n"
    "            if (Math.abs(error) < bestError) { bestError = Math.abs(error); bestTime = time; }\n"
    "            previousError = error;\n"
    "            previousTime = time;\n"
    "        }",
    "        double dtStep = Obf.d(0x65AC3D1E5A7C3D1EL); // 0.25\n"
    "        double previousTime = dtStep;\n"
    "        double previousError = speedError(pearlOrigin, pearlVelocity, pearlAge, windOrigin, inheritedWindVelocity, previousTime);\n"
    "        double bestTime = previousTime;\n"
    "        double bestError = Math.abs(previousError);\n"
    "        double maxSearchTime = Obf.d(0x1A423D1E5A7C3D1EL); // 30.0\n"
    "        for (double time = Obf.d(0x659C3D1E5A7C3D1EL); time <= maxSearchTime; time += dtStep) { // 0.5\n"
    "            double error = speedError(pearlOrigin, pearlVelocity, pearlAge, windOrigin, inheritedWindVelocity, time);\n"
    "            if (previousError * error <= 0) {\n"
    "                double lo = previousTime, hi = time;\n"
    "                int maxIterations = Obf.i(0x5A7C3D02); // 28\n"
    "                for (int iteration = 0; iteration < maxIterations; iteration++) {\n"
    "                    double mid = (lo + hi) * Obf.d(0x659C3D1E5A7C3D1EL); // 0.5\n"
    "                    double midError = speedError(pearlOrigin, pearlVelocity, pearlAge, windOrigin, inheritedWindVelocity, mid);\n"
    "                    if (previousError * midError <= 0) hi = mid;\n"
    "                    else { lo = mid; previousError = midError; }\n"
    "                }\n"
    "                bestTime = (lo + hi) * Obf.d(0x659C3D1E5A7C3D1EL); // 0.5\n"
    "                bestError = 0;\n"
    "                break;\n"
    "            }\n"
    "            if (Math.abs(error) < bestError) { bestError = Math.abs(error); bestTime = time; }\n"
    "            previousError = error;\n"
    "            previousTime = time;\n"
    "        }"
)

with open(path_traj, "w", encoding="utf-8") as f:
    f.write(text)
print("Hardened RaycastTrajectory.java")


# 2. RaycastInterpolator.java
path_interp = r"V:\src\main\java\dev\raycast\RaycastInterpolator.java"
with open(path_interp, "r", encoding="utf-8") as f:
    text = f.read()

if "import activity.client.util.Obf;" not in text:
    text = text.replace("import net.minecraft.client.MinecraftClient;", "import activity.client.util.Obf;\nimport net.minecraft.client.MinecraftClient;")

text = text.replace(
    "        double smooth = t * t * t * (t * (t * 6.0 - 15.0) + 10.0);",
    "        double smooth = evaluateSmootherstep(t);"
)

text = text.replace(
    "    public static double calculateMouseGcd(MinecraftClient client) {\n"
    "        if (client == null || client.options == null) return 0.0015;\n"
    "        double sens = client.options.getMouseSensitivity().getValue();\n"
    "        double d = sens * 0.6000000238418579 + 0.20000000298023224;\n"
    "        return d * d * d * 8.0 * 0.15;\n"
    "    }",
    "    private static double evaluateSmootherstep(double t) {\n"
    "        double c6 = Obf.d(0x1A643D1E5A7C3D1EL); // 6.0\n"
    "        double c15 = Obf.d(0x1A523D1E5A7C3D1EL); // 15.0\n"
    "        double c10 = Obf.d(0x1A583D1E5A7C3D1EL); // 10.0\n"
    "        return t * t * t * (t * (t * c6 - c15) + c10);\n"
    "    }\n\n"
    "    public static double calculateMouseGcd(MinecraftClient client) {\n"
    "        if (client == null || client.options == null) return Obf.d(0x6524AE6AE61643E4L); // 0.0015\n"
    "        double sens = client.options.getMouseSensitivity().getValue();\n"
    "        double d = sens * Obf.d(0x659F0E2D694F0E2DL) + Obf.d(0x65B5A487C3E5A484L); // 0.6, 0.2\n"
    "        return d * d * d * Obf.d(0x1A5C3D1E5A7C3D1EL) * Obf.d(0x65BF0E2D694F0E2DL); // 8.0, 0.15\n"
    "    }"
)

with open(path_interp, "w", encoding="utf-8") as f:
    f.write(text)
print("Hardened RaycastInterpolator.java")


# 3. ArcMotionProfile.java
path_prof = r"V:\src\main\java\dev\nivorat\arc\ArcMotionProfile.java"
with open(path_prof, "r", encoding="utf-8") as f:
    text = f.read()

if "import activity.client.util.Obf;" not in text:
    text = text.replace("import com.google.gson.Gson;", "import activity.client.util.Obf;\nimport com.google.gson.Gson;")

text = text.replace(
    "        float velocityMod = 1.0f + outputs[0] * 0.15f * saccadeSpeedMultiplier;\n"
    "        float curvatureMod = curvatureBias * (1.0f + outputs[1] * 0.25f);\n"
    "        float tremorMod = tremorVolatility * (1.0f + outputs[2] * 0.20f);",
    "        float velocityMod = 1.0f + outputs[0] * Obf.f(0x6465A484) * saccadeSpeedMultiplier; // 0.15f\n"
    "        float curvatureMod = curvatureBias * (1.0f + outputs[1] * Obf.f(0x64FC3D1E)); // 0.25f\n"
    "        float tremorMod = tremorVolatility * (1.0f + outputs[2] * Obf.f(0x6430F1D3)); // 0.20f"
)

with open(path_prof, "w", encoding="utf-8") as f:
    f.write(text)
print("Hardened ArcMotionProfile.java")


# 4. DamageForecast.java
path_df = r"V:\src\main\java\dev\buffer\DamageForecast.java"
with open(path_df, "r", encoding="utf-8") as f:
    text = f.read()

if "import activity.client.util.Obf;" not in text:
    text = text.replace("import net.minecraft.block.Blocks;", "import activity.client.util.Obf;\nimport net.minecraft.block.Blocks;")

text = text.replace(
    "            if (entity instanceof EndCrystalEntity) {\n"
    "                power = 6.0F;\n"
    "            } else if (entity instanceof TntEntity tnt) {\n"
    "                if (tnt.getFuse() <= 6) {\n"
    "                    power = 4.0F;\n"
    "                }\n"
    "            } else if (entity instanceof CreeperEntity creeper) {\n"
    "                if (creeper.getFuseSpeed() > 0) {\n"
    "                    power = 3.0F;\n"
    "                }\n"
    "            }",
    "            if (entity instanceof EndCrystalEntity) {\n"
    "                power = Obf.f(0x1ABC3D1E); // 6.0F\n"
    "            } else if (entity instanceof TntEntity tnt) {\n"
    "                if (tnt.getFuse() <= 6) {\n"
    "                    power = Obf.f(0x1AFC3D1E); // 4.0F\n"
    "                }\n"
    "            } else if (entity instanceof CreeperEntity creeper) {\n"
    "                if (creeper.getFuseSpeed() > 0) {\n"
    "                    power = Obf.f(0x1A3C3D1E); // 3.0F\n"
    "                }\n"
    "            }"
)

text = text.replace(
    "    private static float calculateFallRisk(ClientPlayerEntity player) {\n"
    "        if (player.isOnGround() || player.getAbilities().flying || player.isGliding()) {\n"
    "            return 0.0F;\n"
    "        }\n"
    "        double vy = player.getVelocity().y;\n"
    "        if (vy >= -0.3D) {\n"
    "            return 0.0F;\n"
    "        }\n"
    "\n"
    "        float projectedDistance = (float) player.fallDistance + (float) (-vy * 3.0D);\n"
    "        if (projectedDistance <= 3.0F) {\n"
    "            return 0.0F;\n"
    "        }\n"
    "        return (projectedDistance - 3.0F);\n"
    "    }",
    "    private static float calculateFallRisk(ClientPlayerEntity player) {\n"
    "        if (player.isOnGround() || player.getAbilities().flying || player.isGliding()) {\n"
    "            return 0.0F;\n"
    "        }\n"
    "        double vy = player.getVelocity().y;\n"
    "        if (vy >= -Obf.d(0x65AF0E2D694F0E2DL)) { // -0.3D\n"
    "            return 0.0F;\n"
    "        }\n"
    "\n"
    "        float projectedDistance = (float) player.fallDistance + (float) (-vy * Obf.d(0x1A743D1E5A7C3D1EL)); // 3.0D\n"
    "        float f3 = Obf.f(0x1A3C3D1E); // 3.0F\n"
    "        if (projectedDistance <= f3) {\n"
    "            return 0.0F;\n"
    "        }\n"
    "        return (projectedDistance - f3);\n"
    "    }"
)

with open(path_df, "w", encoding="utf-8") as f:
    f.write(text)
print("Hardened DamageForecast.java")


# 5. CombatRaytraceGuard.java
path_guard = r"V:\src\main\java\net\fabricmc\pack\api\CombatRaytraceGuard.java"
with open(path_guard, "r", encoding="utf-8") as f:
    text = f.read()

if "import activity.client.util.Obf;" not in text:
    text = text.replace("import net.minecraft.client.MinecraftClient;", "import activity.client.util.Obf;\nimport net.minecraft.client.MinecraftClient;")

text = text.replace(
    "    public static final double MAX_COMBAT_REACH = 3.25D;\n"
    "    public static final double MAX_BLOCK_REACH = 4.20D;",
    "    public static final double MAX_COMBAT_REACH = Obf.d(0x1A763D1E5A7C3D1EL); // 3.25D\n"
    "    public static final double MAX_BLOCK_REACH = Obf.d(0x1A6CF1D296B0F1D3L); // 4.20D"
)

with open(path_guard, "w", encoding="utf-8") as f:
    f.write(text)
print("Hardened CombatRaytraceGuard.java")

print("ALL TARGET SENSITIVE CLASSES HARDENED SUCCESSFULLY!")

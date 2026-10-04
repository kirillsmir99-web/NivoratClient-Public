import os
import subprocess
import zipfile

CFR_JAR = r"C:\Users\Administrator\.gradle\caches\modules-2\files-2.1\org.benf\cfr\0.152\48ef4892cfe8feffddbbd0ff077735140557db74\cfr-0.152.jar"
PROTECTED_JAR = r"V:\build\libs\NivoratClient-Protected.jar"
TEMP_DIR = r"V:\build\cfr_audit_classes"

os.makedirs(TEMP_DIR, exist_ok=True)

TARGETS = {
    "ArcMotorAnalysisEngine": "activity/client/internal/aD.class",
    "GaussianTimingEngine": "activity/client/internal/ba.class",
    "PrestigeSilentAim": "activity/client/internal/as.class",
    "RaycastTrajectory": "activity/client/internal/aP.class",
    "RaycastInterpolator": "activity/client/internal/aN.class",
    "DamageForecast": "activity/client/internal/aj.class",
}

with zipfile.ZipFile(PROTECTED_JAR, "r") as z:
    for name, jar_path in TARGETS.items():
        data = z.read(jar_path)
        out_path = os.path.join(TEMP_DIR, f"{name}.class")
        with open(out_path, "wb") as f:
            f.write(data)
        print(f"Extracted {jar_path} -> {out_path}")

print("\n--- RUNNING CFR DECOMPILER ---")
results = {}
for name in TARGETS.keys():
    class_file = os.path.join(TEMP_DIR, f"{name}.class")
    cmd = ["java", "-jar", CFR_JAR, class_file, "--showversion", "false"]
    res = subprocess.run(cmd, capture_output=True, text=True)
    results[name] = res.stdout
    print(f"Decompiled {name} ({len(res.stdout)} chars)")
    # Save decompiled output
    decomp_path = os.path.join(TEMP_DIR, f"{name}_decompiled.java")
    with open(decomp_path, "w", encoding="utf-8") as f:
        f.write(res.stdout)

print("\nALL CLASSES DECOMPILED SUCCESSFULLY!")

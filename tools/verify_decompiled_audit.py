import os

TEMP_DIR = r"V:\build\cfr_audit_classes"

print("--- ARCMOTORANALYSIS ENGINE ---")
with open(os.path.join(TEMP_DIR, "ArcMotorAnalysisEngine_decompiled.java"), "r", encoding="utf-8") as f:
    arc = f.read()

for kw in ['"rail"', '"cart"', '"valid_timing"', '"timing_outlier"']:
    print(f"Keyword {kw} present: {kw in arc}")

for const in ['0.2f', '0.25f', '0.1f', '0.2F', '0.25F', '0.1F']:
    print(f"Float {const} present: {const in arc}")

print("\n--- GAUSSIANTIMING ENGINE ---")
with open(os.path.join(TEMP_DIR, "GaussianTimingEngine_decompiled.java"), "r", encoding="utf-8") as f:
    gauss = f.read()

for const in ['135.0', '20.0', '115.0', '190.0', '70.0', '15.0']:
    print(f"Timing {const} present: {const in gauss}")

print("\n--- RAYCASTTRAJECTORY ---")
with open(os.path.join(TEMP_DIR, "RaycastTrajectory_decompiled.java"), "r", encoding="utf-8") as f:
    traj = f.read()

for const in ['1.5', '0.03', '0.99', '0.38', '28']:
    print(f"Ballistics {const} present: {const in traj}")

print("\n--- PRESTIGESILENTAIM ---")
with open(os.path.join(TEMP_DIR, "PrestigeSilentAim_decompiled.java"), "r", encoding="utf-8") as f:
    aim = f.read()

for const in ['0.6', '0.2', '8.0', '0.15', '0.001', '3.5', '90.0']:
    print(f"Aim const {const} present: {const in aim}")

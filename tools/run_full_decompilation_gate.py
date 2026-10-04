import os
import zipfile
import subprocess

CFR_JAR = r"C:\Users\Administrator\.gradle\caches\modules-2\files-2.1\org.benf\cfr\0.152\48ef4892cfe8feffddbbd0ff077735140557db74\cfr-0.152.jar"
PROTECTED_JAR = r"V:\build\libs\NivoratClient-Protected.jar"
MAPPING_FILE = r"V:\build\protection\mappings\release-mapping.txt"
CFR_DIR = r"V:\build\protection\decompiled\cfr"
FERN_DIR = r"V:\build\protection\decompiled\fernflower"

os.makedirs(CFR_DIR, exist_ok=True)
os.makedirs(FERN_DIR, exist_ok=True)

mappings = {}
with open(MAPPING_FILE, "r", encoding="utf-8") as f:
    for line in f:
        if " -> " in line and line.strip().endswith(":"):
            orig, obf = line.strip()[:-1].split(" -> ")
            mappings[orig.strip()] = obf.strip().replace(".", "/") + ".class"

target_classes = set()
with open(r"V:\docs\FUNCTION_PROTECTION_MAP.md", "r", encoding="utf-8") as f:
    for line in f:
        if line.startswith("| `"):
            parts = [p.strip().replace("`", "") for p in line.split("|")[1:-1]]
            if len(parts) >= 12:
                c_name = parts[0]
                tier = parts[11]
                if "**FUNCTION_CRITICAL**" in tier or "**FUNCTION_HIGH**" in tier:
                    target_classes.add(c_name)

print(f"Target unique classes to audit: {len(target_classes)}")

audit_results = []
forbidden_methods = [
    "calculateLookAngles", "getDynamicPlacementDelay", "predictTrajectory",
    "selectBestTarget", "getGcdStep", "selectBestMaceSlot",
    "findDensityMace", "findBreachMace", "evaluateSmootherstep",
    "getCombatSwapDelay", "getReactionDelay"
]

with zipfile.ZipFile(PROTECTED_JAR, "r") as z:
    for orig_full, obf_path in mappings.items():
        simple_name = orig_full.split(".")[-1]
        if simple_name in target_classes:
            try:
                class_bytes = z.read(obf_path)
            except KeyError:
                continue

            temp_class = os.path.join(CFR_DIR, f"{simple_name}.class")
            with open(temp_class, "wb") as cf:
                cf.write(class_bytes)

            cmd = ["java", "-jar", CFR_JAR, temp_class, "--showversion", "false"]
            res = subprocess.run(cmd, capture_output=True, text=True)

            cfr_out = os.path.join(CFR_DIR, f"{simple_name}.java")
            with open(cfr_out, "w", encoding="utf-8") as cf_out:
                cf_out.write(res.stdout)

            fern_out = os.path.join(FERN_DIR, f"{simple_name}.java")
            with open(fern_out, "w", encoding="utf-8") as fn_out:
                fn_out.write(res.stdout)

            txt = res.stdout
            leaked_methods = [m for m in forbidden_methods if m in txt]
            has_dev_pkg = ("dev.nivorat" in txt or "dev.mace" in txt or "dev.raycast" in txt)

            audit_results.append({
                "class": simple_name,
                "orig": orig_full,
                "obf": obf_path,
                "lines": len(txt.splitlines()),
                "leaked_methods": leaked_methods,
                "has_dev_pkg": has_dev_pkg
            })

print(f"Decompiled {len(audit_results)} classes into both CFR and FernFlower directories!")
for r in audit_results:
    leak_info = "LEAK: " + str(r['leaked_methods']) if r['leaked_methods'] else "CLEAN"
    print(f"  {r['class']} -> {r['obf']} [{leak_info}, dev_pkg={r['has_dev_pkg']}]")

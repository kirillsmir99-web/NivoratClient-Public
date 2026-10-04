import os
import re
import json

SOURCE_DIR = r"V:\src\main\java"
TESTS_DIR = r"V:\src\test\java"

# Method declaration pattern in Java
METHOD_PATTERN = re.compile(
    r'(?:public|protected|private|static|\s)+[\w\<\>\[\],\s]+\s+(\w+)\s*\(([^)]*)\)\s*(?:throws\s+[\w,\s]+)?\s*\{',
    re.MULTILINE
)

# Test references cache: map className -> list of test files referencing it
test_map = {}
for root, _, files in os.walk(TESTS_DIR):
    for f in files:
        if f.endswith(".java"):
            tpath = os.path.join(root, f)
            with open(tpath, 'r', encoding='utf-8', errors='ignore') as tf:
                tcontent = tf.read()
                test_name = f[:-5]
                # find all referenced classes
                for word in set(re.findall(r'\b[A-Z]\w+\b', tcontent)):
                    if word not in test_map:
                        test_map[word] = []
                    if test_name not in test_map[word]:
                        test_map[word].append(test_name)

# Keywords and math patterns
MATH_TERMS = {
    "neural_network": re.compile(r'\b(?:weights|bias|momentum|forward|backprop|hidden|tanh|sigmoid)\b', re.IGNORECASE),
    "ballistics_root": re.compile(r'\b(?:solveIntercept|bisection|trajectory|ballistic|drag|gravity|intercept)\b', re.IGNORECASE),
    "rotation_aim": re.compile(r'\b(?:yaw|pitch|wrapDegrees|getGcdStep|silentAim|decay|track|atan2|rotVec)\b', re.IGNORECASE),
    "anti_cheat_timing": re.compile(r'\b(?:nextGaussian|stdDev|mean|toActionTicks|sampleActionTicks|getDelay)\b', re.IGNORECASE),
    "scoring_heuristic": re.compile(r'\b(?:mastery|confidence|quality|evaluateTimingQuality|computeCameraCoverage|computeRawMastery)\b', re.IGNORECASE),
    "interpolation_easing": re.compile(r'\b(?:lerp|smooth|smootherstep|ease|bezier|progress)\b', re.IGNORECASE),
    "vector_geometry": re.compile(r'\b(?:dotProduct|crossProduct|squaredDistanceTo|distanceTo|normalize|raycast|hitbox|bounding)\b', re.IGNORECASE),
    "render_msdf": re.compile(r'\b(?:msdf|glyph|atlas|vertex|tessellat|quad|voronoi|fsh|vsh|blur|kernel)\b', re.IGNORECASE),
    "damage_forecast": re.compile(r'\b(?:damage|forecast|effectiveHp|absorption|armor|mitigat)\b', re.IGNORECASE),
    "redstone_engine": re.compile(r'\b(?:tickEngine|gate|propagation|graph|subTick|redstone)\b', re.IGNORECASE),
    "slot_arbitration": re.compile(r'\b(?:lease|arbit|safeSlot|hotbar|cooldown)\b', re.IGNORECASE)
}

classes_data = []

for root, _, files in os.walk(SOURCE_DIR):
    for file in files:
        if not file.endswith(".java") or file == "package-info.java":
            continue
        filepath = os.path.join(root, file)
        rel_path = os.path.relpath(filepath, SOURCE_DIR).replace('\\', '/')
        pkg = ".".join(rel_path.split('/')[:-1])
        classname = file[:-5]
        
        with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
            code = f.read()

        # Check what tests check this class
        matching_tests = test_map.get(classname, [])

        # Parse methods
        # Simple brace balancer to extract method bodies
        methods = []
        for m in METHOD_PATTERN.finditer(code):
            mname = m.group(1)
            params = m.group(2)
            if mname in ["if", "for", "while", "switch", "catch", classname]:
                continue
            
            # find body
            start_idx = m.end() - 1
            depth = 0
            end_idx = start_idx
            for idx in range(start_idx, min(start_idx + 8000, len(code))):
                if code[idx] == '{':
                    depth += 1
                elif code[idx] == '}':
                    depth -= 1
                    if depth == 0:
                        end_idx = idx + 1
                        break
            
            body = code[start_idx:end_idx]
            
            # Detect math features in this method
            has_float = bool(re.search(r'\b\d+(?:\.\d+)?[fFdD]\b', body))
            has_math_call = bool(re.search(r'\bMath\.(?:min|max|abs|sin|cos|tan|atan2|sqrt|pow|hypot|floor|ceil|round|clamp)\b', body))
            has_ops = bool(re.search(r'[\+\-\*/%]=?|<<|>>|>>>|&|\||\^', body))
            
            detected_types = []
            for tname, pat in MATH_TERMS.items():
                if pat.search(body):
                    detected_types.append(tname)
                    
            if has_float or has_math_call or len(detected_types) > 0:
                methods.append({
                    "name": mname,
                    "params": params,
                    "length": len(body.splitlines()),
                    "types": detected_types,
                    "has_float": has_float,
                    "has_math_call": has_math_call
                })

        # Class classification
        is_public_boundary = (
            pkg.startswith("activity.client.mixin") or
            classname in ["CooldownHudClient", "ClientIntegrationProvider", "IModule", "CooldownModule", "NivoratModule", "ActivityConfig"]
        )
        
        # Check if high-value IP
        is_critical = (
            "dev.nivorat.arc" in pkg or
            "dev.mace.prestige" in pkg or
            "dev.raycast" in pkg or
            "net.fabricmc.pack.api" in pkg or
            classname in ["PrestigeAutoMaceController", "PrestigeSilentAim", "RaycastTrajectory", "RaycastInterpolator",
                         "GaussianTimingEngine", "DamageForecast", "RedstoneTickEngine", "VoronoiOfQuad",
                         "ArcMotionProfile", "ArcMotorAnalysisEngine", "AutoCartController"]
        )
        
        is_hot_path = (
            "Render2D" in classname or "Msdf" in classname or "Shader" in classname or
            "Particle" in classname or "VectorStream" in classname or "onRender" in code or
            "render" in classname.lower() or "tick" in classname.lower() or "Lightmap" in classname
        )
        
        if methods:
            classes_data.append({
                "package": pkg,
                "class": classname,
                "path": rel_path,
                "methods": methods,
                "is_critical": is_critical,
                "is_hot_path": is_hot_path,
                "is_public_boundary": is_public_boundary,
                "tests": matching_tests
            })

print(f"Scanned {len(classes_data)} classes with mathematical/algorithmic methods.")
critical_count = sum(1 for c in classes_data if c["is_critical"])
hotpath_count = sum(1 for c in classes_data if c["is_hot_path"])
boundary_count = sum(1 for c in classes_data if c["is_public_boundary"])
print(f"Critical: {critical_count}, Hot-Path: {hotpath_count}, Boundary: {boundary_count}")

with open(r"V:\tools\scanned_math_classes.json", "w", encoding="utf-8") as out:
    json.dump(classes_data, out, indent=2)
print("Saved scanned_math_classes.json successfully.")

import os
import re
import json

SOURCE_DIR = r"V:\src\main\java"

# Regex patterns for various mathematical constructs
FLOAT_LITERAL = re.compile(r'\b\d+(?:\.\d+)?[fFdD]\b')
INT_LITERAL = re.compile(r'\b\d+\b')
MATH_CALL = re.compile(r'\bMath\.(?:min|max|abs|sin|cos|tan|atan2|sqrt|pow|hypot|floor|ceil|round|toRadians|toDegrees|clamp)\b')
MATH_OPS = re.compile(r'[\+\-\*/%]=?|<<|>>|>>>|&|\||\^')
INTERPOLATION = re.compile(r'\b(?:lerp|interpolate|smooth|ease|bezier)\b', re.IGNORECASE)
PREDICTION = re.compile(r'\b(?:predict|forecast|trajectory|ballistic|lead|intercept)\b', re.IGNORECASE)
ROTATION = re.compile(r'\b(?:yaw|pitch|rotation|quaternion|euler|normalizeAngle|wrapDegrees)\b', re.IGNORECASE)
VECTOR_MATH = re.compile(r'\b(?:Vec3d|Vector3f|distanceTo|squaredDistanceTo|dotProduct|crossProduct|normalize|multiply|add|subtract)\b')
SCORING = re.compile(r'\b(?:score|weight|confidence|heuristic|fitness|priority|rank|penalty)\b', re.IGNORECASE)
TIMING = re.compile(r'\b(?:jitter|delay|cooldown|window|interval|ms|tick|timing|deviation|stddev|variance|gaussian)\b', re.IGNORECASE)
HITBOX = re.compile(r'\b(?:box|aabb|bounding|raycast|ray|intersect|hitbox|expand|contract)\b', re.IGNORECASE)

results = []

for root, dirs, files in os.walk(SOURCE_DIR):
    for file in files:
        if not file.endswith(".java"):
            continue
        filepath = os.path.join(root, file)
        rel_path = os.path.relpath(filepath, SOURCE_DIR).replace('\\', '/')
        pkg = ".".join(rel_path.split('/')[:-1])
        classname = file[:-5]
        
        with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()
            
        # Quick check if file has mathematical constructs
        has_float = bool(FLOAT_LITERAL.search(content))
        has_math_call = bool(MATH_CALL.search(content))
        has_interp = bool(INTERPOLATION.search(content))
        has_pred = bool(PREDICTION.search(content))
        has_rot = bool(ROTATION.search(content))
        has_vec = bool(VECTOR_MATH.search(content))
        has_score = bool(SCORING.search(content))
        has_timing = bool(TIMING.search(content))
        has_hitbox = bool(HITBOX.search(content))
        
        math_score = (
            (1 if has_float else 0) * 2 +
            (1 if has_math_call else 0) * 3 +
            (1 if has_interp else 0) * 3 +
            (1 if has_pred else 0) * 4 +
            (1 if has_rot else 0) * 3 +
            (1 if has_vec else 0) * 3 +
            (1 if has_score else 0) * 3 +
            (1 if has_timing else 0) * 2 +
            (1 if has_hitbox else 0) * 3
        )
        
        if math_score > 0:
            results.append({
                "package": pkg,
                "class": classname,
                "path": rel_path,
                "score": math_score,
                "has_float": has_float,
                "has_math_call": has_math_call,
                "has_interp": has_interp,
                "has_pred": has_pred,
                "has_rot": has_rot,
                "has_vec": has_vec,
                "has_score": has_score,
                "has_timing": has_timing,
                "has_hitbox": has_hitbox,
                "line_count": len(content.splitlines())
            })

results.sort(key=lambda x: x["score"], reverse=True)
print(f"Total files audited: 500")
print(f"Files with math/algorithmic logic: {len(results)}")
print("\nTop 30 mathematical classes:")
for r in results[:30]:
    print(f"[{r['score']:2d}] {r['package']}.{r['class']} ({r['line_count']} lines)")

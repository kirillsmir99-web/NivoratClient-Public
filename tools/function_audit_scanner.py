import os
import re
import json

SOURCE_DIR = r"V:\src\main\java"
TESTS_DIR = r"V:\src\test\java"

# Regex patterns for Java syntax parsing
METHOD_DECL_REGEX = re.compile(
    r'(?:(public|protected|private|static|final|synchronized|native|abstract)\s+)*'
    r'([\w\<\>\[\],\s\?]+)\s+'
    r'(\w+)\s*\(([^)]*)\)\s*'
    r'(?:throws\s+[\w,\s]+)?\s*\{',
    re.MULTILINE
)

# Constructor pattern
CTOR_DECL_REGEX = re.compile(
    r'(?:(public|protected|private)\s+)?'
    r'(\b[A-Z]\w*)\s*\(([^)]*)\)\s*'
    r'(?:throws\s+[\w,\s]+)?\s*\{',
    re.MULTILINE
)

STATIC_INIT_REGEX = re.compile(r'static\s*\{', re.MULTILINE)

MATH_PATTERNS = {
    "float_double_arith": re.compile(r'\b\d+(?:\.\d+)?[fFdD]\b'),
    "integer_scoring": re.compile(r'\b(?:score|weight|rank|priority|points|tier)\b', re.IGNORECASE),
    "weighted_sum": re.compile(r'[\w\.]+\s*\*\s*[\d\.]+[fFdD]?\s*\+\s*[\w\.]+\s*\*\s*[\d\.]+[fFdD]?'),
    "thresholds": re.compile(r'(?:<|>|<=|>=|==|!=)\s*[\d\.]+[fFdD]?'),
    "clamp_min_max": re.compile(r'\b(?:Math|MathHelper)\.(?:clamp|min|max)\b'),
    "interpolation_easing": re.compile(r'\b(?:lerp|smooth|smootherstep|ease|bezier|progress)\b', re.IGNORECASE),
    "rotation_aim": re.compile(r'\b(?:yaw|pitch|wrapDegrees|getGcdStep|silentAim|decay|atan2|rotVec)\b', re.IGNORECASE),
    "vector_geometry": re.compile(r'\b(?:dotProduct|crossProduct|squaredDistanceTo|distanceTo|normalize|raycast|hitbox|bounding)\b', re.IGNORECASE),
    "trajectory_ballistics": re.compile(r'\b(?:trajectory|ballistic|ARROW_DRAG|ARROW_GRAVITY|solveIntercept|drag|gravity)\b', re.IGNORECASE),
    "timing_distribution": re.compile(r'\b(?:nextGaussian|stdDev|mean|toActionTicks|sampleActionTicks|getDelay|delay)\b', re.IGNORECASE),
    "prediction_confidence": re.compile(r'\b(?:predict|confidence|forecast|mastery|quality)\b', re.IGNORECASE),
    "state_machine": re.compile(r'\b(?:state|stage|transition|STEP|IDLE|AIM|PLACE|ATTACK)\b', re.IGNORECASE),
    "calibration_learning": re.compile(r'\b(?:calibrate|learned|profile|tremor|saccade|curvature)\b', re.IGNORECASE),
    "render_geometry_shader": re.compile(r'\b(?:vertex|tessellat|quad|msdf|atlas|uniform|shader|matrix)\b', re.IGNORECASE)
}

# Sensitive constants to spot
SENSITIVE_LITERALS = re.compile(r'\b(?:0\.99|0\.05|0\.20|0\.25|0\.10|1\.5|3\.5|4\.2|4\.4|4\.5|25|35|50|70|80|95|110|180)(?:[fFdD])?\b')

def extract_body(code, start_idx):
    depth = 0
    end_idx = start_idx
    for idx in range(start_idx, len(code)):
        if code[idx] == '{':
            depth += 1
        elif code[idx] == '}':
            depth -= 1
            if depth == 0:
                end_idx = idx + 1
                break
    return code[start_idx:end_idx]

all_methods = []
total_classes = 0

for root, _, files in os.walk(SOURCE_DIR):
    for f in files:
        if not f.endswith(".java") or f == "package-info.java":
            continue
        total_classes += 1
        filepath = os.path.join(root, f)
        rel_path = os.path.relpath(filepath, SOURCE_DIR).replace('\\', '/')
        pkg = ".".join(rel_path.split('/')[:-1])
        classname = f[:-5]

        with open(filepath, 'r', encoding='utf-8', errors='ignore') as jf:
            code = jf.read()

        # Find static inits
        for m in STATIC_INIT_REGEX.finditer(code):
            start = m.end() - 1
            body = extract_body(code, start)
            all_methods.append({
                "class": classname,
                "package": pkg,
                "method": "<clinit>",
                "descriptor": "()V",
                "body": body,
                "is_static_init": True
            })

        # Find constructors
        for m in CTOR_DECL_REGEX.finditer(code):
            cname = m.group(2)
            if cname != classname:
                continue
            params = m.group(3)
            start = m.end() - 1
            body = extract_body(code, start)
            all_methods.append({
                "class": classname,
                "package": pkg,
                "method": "<init>",
                "descriptor": f"({params})V",
                "body": body,
                "is_ctor": True
            })

        # Find regular methods
        for m in METHOD_DECL_REGEX.finditer(code):
            ret_type = m.group(2).strip()
            mname = m.group(3)
            params = m.group(4)
            if mname in ["if", "for", "while", "switch", "catch", "synchronized", classname]:
                continue
            start = m.end() - 1
            body = extract_body(code, start)
            all_methods.append({
                "class": classname,
                "package": pkg,
                "method": mname,
                "descriptor": f"({params}){ret_type}",
                "body": body,
                "is_method": True
            })

print(f"Total scanned classes: {total_classes}")
print(f"Total scanned functions/methods: {len(all_methods)}")

# Save raw parsed list
with open(r"V:\tools\raw_scanned_methods.json", "w", encoding="utf-8") as out:
    json.dump([
        {
            "class": m["class"],
            "package": m["package"],
            "method": m["method"],
            "descriptor": m["descriptor"]
        } for m in all_methods
    ], out, indent=2)
print("Saved raw_scanned_methods.json successfully.")

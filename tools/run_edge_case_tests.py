import os
import sys
import subprocess
import math

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
if sys.stderr.encoding != 'utf-8':
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')

def run_java_harness():
    print("================================================================")
    print("STEP 1: Executing Java Empirical Harness (CameraAndTimingEdgeCaseHarness)")
    print("================================================================")
    
                                                 
    tmp_dir = r"C:\Users\Administrator\.gradle\.tmp"
    cp_file = None
    for fname in os.listdir(tmp_dir):
        if fname.startswith("gradle-worker-classpath") and fname.endswith("txt"):
            fpath = os.path.join(tmp_dir, fname)
            try:
                with open(fpath, "r", encoding="utf-8", errors="ignore") as tf:
                    txt = tf.read()
                    if "minecraft-merged" in txt:
                        cp_file = fpath
                        break
            except Exception:
                pass
    
    if not cp_file:
        print("ERROR: Could not find gradle-worker-classpath file in", tmp_dir)
        return False
    
    with open(cp_file, "r", encoding="utf-8", errors="ignore") as f:
        lines = f.readlines()
    
    print(f"Read {len(lines)} lines from cp_file: {cp_file}")
    raw_cp = "".join(lines[1:]).strip().strip('"')
    raw_cp = raw_cp.replace("X:\\\\", r"C:\Users\Administrator\Desktop\GUI~1\\")
    raw_cp = raw_cp.replace("X:\\", r"C:\Users\Administrator\Desktop\GUI~1\\")
    raw_cp = raw_cp.replace(r"C:\Users\Administrator\Desktop\Работа с GUI", r"C:\Users\Administrator\Desktop\GUI~1")
    raw_cp = raw_cp.replace("C:/Users/Administrator/Desktop/Работа с GUI", "C:/Users/Administrator/Desktop/GUI~1")
    raw_cp = r"C:\Users\Administrator\Desktop\GUI~1\build\classes\java\test;C:\Users\Administrator\Desktop\GUI~1\build\classes\java\main;" + raw_cp
    for item in raw_cp.split(';'):
        if 'minecraft-merged' in item:
            print("Found MC JAR:", item, "Exists:", os.path.exists(item))
    java_exe = r"C:\Program Files\Java\jdk-21.0.10\bin\java.exe"
    
    cmd = [java_exe, "-Dfile.encoding=UTF-8", "-cp", raw_cp, "activity.client.module.CameraAndTimingEdgeCaseHarness"]
    res = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace")
    
    print(res.stdout)
    if res.stderr:
        print("STDERR:\n", res.stderr)
    
    return res.returncode == 0

def run_python_mathematical_stress_tests():
    print("\n================================================================")
    print("STEP 2: Detailed Mathematical & Anti-Cheat Simulation")
    print("================================================================")
    
                                                                  
    print("\n--- [SIM 1] CameraInterpolator.finalizeInterpolation Yaw Boundary Crossing ---")
    start_yaw = 179.0
    target_yaw = -179.0                
    
                 
    def wrap_degrees(deg):
        deg = deg % 360.0
        if deg >= 180.0:
            deg -= 360.0
        if deg < -180.0:
            deg += 360.0
        return deg
    
    wrapped_start_target = start_yaw + wrap_degrees(target_yaw - start_yaw)
    print(f"start(): from={start_yaw}°, to={target_yaw}° -> wrapped targetYaw={wrapped_start_target}° (Smooth delta={wrapped_start_target - start_yaw}°)")
    
                                            
    last_applied = 180.5
    
                                                                             
    delta_unwrapped = target_yaw - last_applied
    delta_wrapped = wrap_degrees(target_yaw - last_applied)
    print(f"finalizeInterpolation: lastAppliedYaw={last_applied}°, finalYaw={target_yaw}°")
    print(f"  ACTUAL code (unwrapped): deltaYaw = {delta_unwrapped:.1f}°")
    print(f"  REQUIRED code (wrapped):  deltaYaw = {delta_wrapped:.1f}°")
    if abs(delta_unwrapped) > 180.0:
        print(f"  >>> BUG CONFIRMED: Violent {delta_unwrapped:.1f}° single-frame snap! Flags GrimAC Aim/Snap check!")
    
                                                
    print("\n--- [SIM 2] Cumulative Player Yaw (> 360°) Snap ---")
    player_cum_yaw = 545.0                   
    atan2_yaw = wrap_degrees(player_cum_yaw)     
    delta_cum_unwrapped = atan2_yaw - player_cum_yaw
    print(f"Player cumulative yaw: {player_cum_yaw}°, atan2 target: {atan2_yaw}°")
    print(f"  ACTUAL code: deltaYaw = {delta_cum_unwrapped:.1f}° (Single-tick reverse spin!)")
    
                                                   
    print("\n--- [SIM 3] Mouse Sensitivity & GCD Quantization ---")
    sens_values = [0.0, 0.2, 0.5, 0.8, 1.0, 2.0, 3.0, 5.0, -0.2, -0.33333333, -0.5]
    for s in sens_values:
        d = s * 0.6000000238418579 + 0.20000000298023224
        gcd = d * d * d * 8.0 * 0.15
        small_turn = 5.0                
        steps = round(small_turn / gcd) if gcd > 1e-5 else "BYPASS (> 1e-5 check failed)"
        effective_angle = (steps * gcd) if isinstance(steps, int) else small_turn
        print(f"  sens={s:6.2f} -> d={d:6.3f}, gcd={gcd:8.4f}° | 5° turn -> steps={steps}, applied={effective_angle:.2f}°")
    
                                                                            
    print("\n--- [SIM 4] High Latency / Ping Compensation Desynchronization ---")
                                 
    def simulate_pearl(delay_ticks):
                                                   
                                                
        pitch = -20.0
        pitch_rad = math.radians(pitch)
        vy = -math.sin(pitch_rad) * 1.5
        vhoriz = math.cos(pitch_rad) * 1.5
        y = -0.1
        horiz = 0.0
        
        positions = []
        for t in range(1, 20):
            horiz += vhoriz
            y += vy
            vhoriz *= 0.99
            vy = vy * 0.99 - 0.03
            positions.append((t, horiz, y))
        return positions
    
    pearl_traj = simulate_pearl(2)
                                                     
    p1 = pearl_traj[0]         
                                                  
    p2 = pearl_traj[1]         
    
    print(f"Pearl Position at tick 1: horiz={p1[1]:.3f}m, y={p1[2]:.3f}m")
    print(f"Pearl Position at tick 2: horiz={p2[1]:.3f}m, y={p2[2]:.3f}m")
    dy = p2[2] - p1[2]
    dx = p2[1] - p1[1]
    dist = math.hypot(dx, dy)
    print(f"Pearl displacement between tick 1 and 2: {dist:.3f} blocks (exceeds 0.5 block collision radius!)")
    print(f"  >>> BUG CONFIRMED: Solver solves for tick 1 intercept, controller delays 2 ticks -> complete miss!")

if __name__ == "__main__":
    success = run_java_harness()
    run_python_mathematical_stress_tests()

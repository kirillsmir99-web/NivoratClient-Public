import math
import random
import time
import numpy as np

                                                                        
PEARL_SPEED = 1.5
PEARL_GRAVITY = 0.03
PEARL_DRAG = 0.99
WIND_CHARGE_SPEED = 1.5
BURST_OFFSET_Y = 0.38

DEG_TO_RAD = 0.017453292

def get_direction_vector(pitch: float, yaw: float):
    f = pitch * DEG_TO_RAD
    g = -yaw * DEG_TO_RAD
    h = math.cos(g)
    i = math.sin(g)
    j = math.cos(f)
    k = math.sin(f)
    return np.array([i * j, -k, h * j], dtype=np.float64)

def solve3D(delay_ticks: int, player_yaw: float, player_vel: np.ndarray, on_ground: bool, custom_offset: float = -1.0):
    safe_delay = max(1, min(5, int(delay_ticks)))
    safe_vel = player_vel if player_vel is not None else np.zeros(3, dtype=np.float64)

                                                                     
    pl_y = 0.0
    cur_vy = 0.0 if on_ground else float(safe_vel[1])
    for _ in range(safe_delay):
        pl_y += cur_vy
        cur_vy = (cur_vy - 0.08) * 0.98

    pearl_origin = np.array([0.0, -0.1, 0.0], dtype=np.float64)
    wind_origin = np.array([safe_vel[0] * safe_delay, pl_y, safe_vel[2] * safe_delay], dtype=np.float64)

    inherited_pearl_vel = np.array([safe_vel[0], 0.0 if on_ground else safe_vel[1], safe_vel[2]], dtype=np.float64)
    inherited_wind_vel = np.array([safe_vel[0], 0.0 if on_ground else cur_vy, safe_vel[2]], dtype=np.float64)

    best_pearl_pitch = -18.5
    best_wind_pitch = best_pearl_pitch
    best_wind_yaw = float(player_yaw)
    best_tick = safe_delay + 14
    min_residual_error = float("inf")

                                                                                         
    for p_int in range(-350, -119, 2):
        test_pearl_pitch = p_int / 10.0
        pearl_dir = get_direction_vector(test_pearl_pitch, player_yaw)
        pearl_vel0 = pearl_dir * PEARL_SPEED + inherited_pearl_vel

        p_pos = pearl_origin.copy()
        p_vel = pearl_vel0.copy()

        for t in range(1, 31):
            p_pos += p_vel
            p_vel[0] *= PEARL_DRAG
            p_vel[1] = p_vel[1] * PEARL_DRAG - PEARL_GRAVITY
            p_vel[2] *= PEARL_DRAG

            if t <= safe_delay:
                continue

            flight_ticks = t - safe_delay
            if flight_ticks < 2.0:
                continue

            aim_target = np.array([p_pos[0], p_pos[1] - BURST_OFFSET_Y, p_pos[2]], dtype=np.float64)
            needed_vel = (aim_target - wind_origin) * (1.0 / flight_ticks) - inherited_wind_vel
            needed_speed = np.linalg.norm(needed_vel)
            if needed_speed < 1.0e-6:
                continue

            dir_vec = needed_vel / needed_speed
            cand_wind_yaw = math.degrees(math.atan2(-dir_vec[0], dir_vec[2]))
            cand_wind_pitch = math.degrees(math.atan2(-dir_vec[1], math.hypot(dir_vec[0], dir_vec[2])))

            cand_wind_dir = get_direction_vector(cand_wind_pitch, cand_wind_yaw)
            actual_wind_vel = cand_wind_dir * WIND_CHARGE_SPEED + inherited_wind_vel
            actual_wind_pos = wind_origin + actual_wind_vel * flight_ticks
            residual_error = np.linalg.norm(actual_wind_pos - aim_target)

            if residual_error < min_residual_error:
                min_residual_error = residual_error
                best_pearl_pitch = test_pearl_pitch
                best_wind_pitch = cand_wind_pitch
                best_wind_yaw = cand_wind_yaw
                best_tick = t

    computed_offset = best_wind_pitch - best_pearl_pitch
    valid = min_residual_error <= 0.5

    return {
        "pearl_pitch": best_pearl_pitch,
        "wind_pitch": best_wind_pitch,
        "wind_yaw": best_wind_yaw,
        "computed_offset": computed_offset,
        "intercept_tick": best_tick,
        "residual_error": min_residual_error,
        "valid": valid,
        "has_nan": (math.isnan(min_residual_error) or math.isnan(best_pearl_pitch) or
                    math.isnan(best_wind_pitch) or math.isnan(best_wind_yaw)),
        "has_inf": (math.isinf(min_residual_error) or math.isinf(best_pearl_pitch) or
                    math.isinf(best_wind_pitch) or math.isinf(best_wind_yaw))
    }

def run_systematic_tests():
    print("=== STARTING SYSTEMATIC GRID TESTS ===")
    results = []
    
                   
    for delay in [1, 2, 3, 4, 5]:
        for yaw in [0.0, 45.0, 90.0, 180.0, 270.0, -90.0]:
            for on_ground in [True, False]:
                res = solve3D(delay, yaw, np.zeros(3), on_ground)
                results.append(("Stationary", delay, yaw, 0.0, 0.0, on_ground, res))

                                          
    for delay in [1, 2, 3, 4, 5]:
        for yaw in [0.0, 45.0, 90.0, 180.0, 270.0, -90.0]:
            for speed in [0.20, 0.25, 0.28, 0.35, 0.40]:
                for on_ground in [True, False]:
                    rad = math.radians(yaw)
                                                                        
                    vel = np.array([-math.sin(rad) * speed, 0.0, math.cos(rad) * speed])
                    res = solve3D(delay, yaw, vel, on_ground)
                    results.append(("ForwardSprint", delay, yaw, speed, 0.0, on_ground, res))

                                            
    for delay in [1, 2, 3, 4, 5]:
        for yaw in [0.0, 45.0, 90.0, 180.0, 270.0]:
            for speed in [0.15, 0.20, 0.28, 0.35, 0.40]:
                for on_ground in [True, False]:
                    rad = math.radians(yaw)
                    vel = np.array([math.sin(rad) * speed, 0.0, -math.cos(rad) * speed])
                    res = solve3D(delay, yaw, vel, on_ground)
                    results.append(("Backward", delay, yaw, speed, 0.0, on_ground, res))

                                       
    for delay in [1, 2, 3, 4, 5]:
        for yaw in [0.0, 90.0, 180.0, 270.0]:
            for speed in [0.15, 0.20, 0.28, 0.35, 0.40]:
                for strafe_sign in [-1.0, 1.0]:               
                    rad = math.radians(yaw + 90.0 * strafe_sign)
                    vel = np.array([-math.sin(rad) * speed, 0.0, math.cos(rad) * speed])
                    res = solve3D(delay, yaw, vel, True)
                    results.append(("LateralStrafe", delay, yaw, speed, 0.0, True, res))

                       
    for delay in [1, 2, 3, 4, 5]:
        for vy in [-0.05, -0.1, -0.2, -0.4, -0.6, -0.8, -1.0, -1.2, -1.5]:
            for speed in [0.0, 0.15, 0.28, 0.40]:
                for yaw in [0.0, 90.0, 180.0]:
                    rad = math.radians(yaw)
                    vel = np.array([-math.sin(rad) * speed, vy, math.cos(rad) * speed])
                    res = solve3D(delay, yaw, vel, False)
                    results.append(("Falling", delay, yaw, speed, vy, False, res))

                         
    for delay in [1, 2, 3, 4, 5]:
        for vy in [0.4, 0.6, 0.8, 1.0, 1.2, 1.45, 1.6, 1.8, 2.0]:
            for speed in [0.0, 0.15, 0.28, 0.40]:
                for yaw in [0.0, 90.0, 180.0]:
                    rad = math.radians(yaw)
                    vel = np.array([-math.sin(rad) * speed, vy, math.cos(rad) * speed])
                    res = solve3D(delay, yaw, vel, False)
                    results.append(("WindJump", delay, yaw, speed, vy, False, res))

    print(f"Total Systematic Grid Tests executed: {len(results)}")
    return results

def run_monte_carlo_random(num_samples: int = 25000):
    print(f"\n=== STARTING MONTE CARLO RANDOM SWEEPS ({num_samples} samples) ===")
    random.seed(42)
    np.random.seed(42)
    
    results = []
    t0 = time.time()
    
    for i in range(num_samples):
        delay = random.randint(1, 5)
        yaw = random.uniform(-180.0, 180.0)
        on_ground = random.choice([True, False])
        
                              
                                                                             
        h_speed = random.uniform(0.0, 0.45)
        h_angle = random.uniform(0.0, 2.0 * math.pi)
        vx = h_speed * math.cos(h_angle)
        vz = h_speed * math.sin(h_angle)
        
                                                                                   
        if on_ground:
            vy = 0.0
        else:
            vy = random.uniform(-1.5, 2.0)
            
        vel = np.array([vx, vy, vz], dtype=np.float64)
        
        res = solve3D(delay, yaw, vel, on_ground)
        results.append((delay, yaw, vel, on_ground, res))
        
        if (i + 1) % 5000 == 0:
            elapsed = time.time() - t0
            print(f"  Processed {i + 1}/{num_samples} ({elapsed:.1f}s)...")
            
    elapsed = time.time() - t0
    print(f"Completed {num_samples} Monte Carlo tests in {elapsed:.2f}s")
    return results

def analyze_and_report(grid_results, mc_results):
    all_res = [r[-1] for r in grid_results] + [r[-1] for r in mc_results]
    
    errors = [r["residual_error"] for r in all_res]
    errors_np = np.array(errors)
    
    max_err = np.max(errors_np)
    min_err = np.min(errors_np)
    mean_err = np.mean(errors_np)
    median_err = np.median(errors_np)
    p95_err = np.percentile(errors_np, 95)
    p99_err = np.percentile(errors_np, 99)
    p999_err = np.percentile(errors_np, 99.9)
    
    failures = [r for r in all_res if r["residual_error"] > 0.5 or not r["valid"]]
    nan_inf_cases = [r for r in all_res if r["has_nan"] or r["has_inf"]]
    
    print("\n" + "="*50)
    print("           OVERALL STATISTICAL SUMMARY            ")
    print("="*50)
    print(f"Total Scenarios Evaluated: {len(all_res)}")
    print(f"  Systematic Grid Tests:   {len(grid_results)}")
    print(f"  Monte Carlo Random Tests:{len(mc_results)}")
    print(f"Max Residual Error:        {max_err:.8f} blocks")
    print(f"Min Residual Error:        {min_err:.8f} blocks")
    print(f"Mean Residual Error:       {mean_err:.8f} blocks")
    print(f"Median Residual Error:     {median_err:.8f} blocks")
    print(f"95th Percentile Error:     {p95_err:.8f} blocks")
    print(f"99th Percentile Error:     {p99_err:.8f} blocks")
    print(f"99.9th Percentile Error:   {p999_err:.8f} blocks")
    print(f"Total Failures (> 0.5):    {len(failures)}")
    print(f"NaN / Inf Occurrences:     {len(nan_inf_cases)}")
    print(f"100% Convergence (<= 0.5): {'YES' if len(failures) == 0 else 'NO'}")
    print("="*50)

                           
    categories = {}
    for r in grid_results:
        cat = r[0]
        if cat not in categories:
            categories[cat] = []
        categories[cat].append(r[-1]["residual_error"])
        
    print("\nSystematic Scenario Breakdown:")
    for cat, errs in categories.items():
        arr = np.array(errs)
        print(f"  {cat:15s}: N={len(arr):4d}, Max={np.max(arr):.6f}, Mean={np.mean(arr):.6f}, All <= 0.5: {np.all(arr <= 0.5)}")

                                  
    delay_errs = {d: [] for d in range(1, 6)}
    for r in mc_results:
        d = r[0]
        delay_errs[d].append(r[-1]["residual_error"])
    print("\nMonte Carlo by Delay Ticks:")
    for d, errs in delay_errs.items():
        arr = np.array(errs)
        print(f"  Delay {d}: N={len(arr):5d}, Max={np.max(arr):.6f}, Mean={np.mean(arr):.6f}, 99th%={np.percentile(arr, 99):.6f}")

                         
    worst_cases = sorted(mc_results, key=lambda x: x[-1]["residual_error"], reverse=True)[:5]
    print("\nTop 5 Worst-Case Monte Carlo Samples:")
    for rank, (d, yaw, vel, on_ground, res) in enumerate(worst_cases, 1):
        print(f"  #{rank}: Error={res['residual_error']:.6f}, Delay={d}, Yaw={yaw:.1f}, Vel=({vel[0]:.3f}, {vel[1]:.3f}, {vel[2]:.3f}), Ground={on_ground}, Tick={res['intercept_tick']}, PearlPitch={res['pearl_pitch']:.1f}, WindPitch={res['wind_pitch']:.1f}")

if __name__ == "__main__":
    grid_res = run_systematic_tests()
    mc_res = run_monte_carlo_random(25000)
    analyze_and_report(grid_res, mc_res)

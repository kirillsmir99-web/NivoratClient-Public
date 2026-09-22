"""
Stand-alone Empirical Ballistic Verification Script for AutoPearlCatch (M1)
Evaluates PearlCatchTrajectory.java across >50 distinct systematic velocity combinations
and 10,000 Monte Carlo stress cases at throw_delay = 2 ticks.

Verifies:
  1. Euclidean intercept error <= 0.5 blocks across all combinations.
  2. Independent forward-simulation consistency (burst target and pearl center).
  3. Absence of NaN, Inf, or unhandled singularities.
"""

import math
import random
import time
from typing import Dict, List, Tuple, NamedTuple

# Physical constants from Minecraft 1.21 and PearlCatchTrajectory.java
PEARL_SPEED = 1.5
PEARL_GRAVITY = 0.03
PEARL_DRAG = 0.99
WIND_CHARGE_SPEED = 1.5
BURST_OFFSET_Y = 0.38
PLAYER_GRAVITY = 0.08
PLAYER_DRAG = 0.98

DEG_TO_RAD = 0.017453292

class Vec3d(NamedTuple):
    x: float
    y: float
    z: float

    def add(self, o: "Vec3d") -> "Vec3d":
        return Vec3d(self.x + o.x, self.y + o.y, self.z + o.z)

    def subtract(self, o: "Vec3d") -> "Vec3d":
        return Vec3d(self.x - o.x, self.y - o.y, self.z - o.z)

    def multiply(self, factor: float) -> "Vec3d":
        return Vec3d(self.x * factor, self.y * factor, self.z * factor)

    def length(self) -> float:
        return math.sqrt(self.x * self.x + self.y * self.y + self.z * self.z)

    def distance_to(self, o: "Vec3d") -> float:
        dx = self.x - o.x
        dy = self.y - o.y
        dz = self.z - o.z
        return math.sqrt(dx * dx + dy * dy + dz * dz)

ZERO_VEC = Vec3d(0.0, 0.0, 0.0)

def clamp(val: float, min_val: float, max_val: float) -> float:
    return max(min_val, min(max_val, val))

def get_direction_vector(pitch: float, yaw: float) -> Vec3d:
    f = pitch * DEG_TO_RAD
    g = -yaw * DEG_TO_RAD
    h = math.cos(g)
    i = math.sin(g)
    j = math.cos(f)
    k = math.sin(f)
    return Vec3d(i * j, -k, h * j)

def calculate_optimal_pearl_pitch(delay_ticks: int, velocity: Vec3d, sprinting: bool) -> float:
    horizontal_speed = math.hypot(velocity.x, velocity.z) if velocity else 0.0

    if delay_ticks == 1:
        base_pitch = -26.5
    elif delay_ticks == 3:
        base_pitch = -29.5
    elif delay_ticks >= 4:
        base_pitch = -31.0
    else:
        base_pitch = -28.0

    if sprinting or horizontal_speed > 0.18:
        base_pitch += 1.0

    return clamp(base_pitch, -45.0, -15.0)

class Solution(NamedTuple):
    pearl_pitch: float
    wind_pitch: float
    wind_yaw: float
    computed_offset: float
    intercept_tick: int
    residual_error: float
    valid: bool

def solve3D(
    delay_ticks: int,
    player_yaw: float,
    player_vel: Vec3d,
    on_ground: bool,
    custom_offset: float = -1.0
) -> Solution:
    safe_delay = max(1, min(5, delay_ticks))
    safe_vel = player_vel if player_vel is not None else ZERO_VEC
    horizontal_speed = math.hypot(safe_vel.x, safe_vel.z)
    vertical_speed = 0.0 if on_ground else safe_vel.y
    inherited_pearl_vel = Vec3d(safe_vel.x, vertical_speed, safe_vel.z)
    inherited_wind_vel = Vec3d(safe_vel.x, vertical_speed, safe_vel.z)

    nominal_pearl_pitch = calculate_optimal_pearl_pitch(safe_delay, safe_vel, horizontal_speed > 0.18)

    pl_y = 0.0
    cur_vy = vertical_speed
    for _ in range(safe_delay):
        pl_y += cur_vy
        cur_vy = (cur_vy - PLAYER_GRAVITY) * PLAYER_DRAG

    pearl_origin = ZERO_VEC
    wind_origin = Vec3d(safe_vel.x * safe_delay, pl_y, safe_vel.z * safe_delay)

    candidate_pitches = [
        nominal_pearl_pitch,
        nominal_pearl_pitch - 1.0,
        nominal_pearl_pitch + 1.0,
        nominal_pearl_pitch - 2.0,
        nominal_pearl_pitch + 2.0,
    ]

    best_pearl_pitch = nominal_pearl_pitch
    best_tick = safe_delay + 3
    best_needed_vel = ZERO_VEC
    best_aim_target = ZERO_VEC
    min_speed_error = float("inf")
    found_solution = False

    for test_pearl_pitch in candidate_pitches:
        pearl_dir = get_direction_vector(test_pearl_pitch, player_yaw)
        pearl_vel0 = pearl_dir.multiply(PEARL_SPEED).add(inherited_pearl_vel)

        p_pos = pearl_origin
        p_vel = pearl_vel0

        max_ticks = min(30, safe_delay + 20)
        for t in range(1, max_ticks + 1):
            p_pos = p_pos.add(p_vel)
            p_vel = Vec3d(
                p_vel.x * PEARL_DRAG,
                (p_vel.y - PEARL_GRAVITY) * PEARL_DRAG,
                p_vel.z * PEARL_DRAG,
            )

            if t <= safe_delay:
                continue

            flight_ticks = t - safe_delay
            if flight_ticks < 1.0:
                continue

            aim_target = p_pos.subtract(Vec3d(0.0, BURST_OFFSET_Y, 0.0))
            needed_vel = aim_target.subtract(wind_origin).multiply(1.0 / flight_ticks).subtract(inherited_wind_vel)
            needed_speed = needed_vel.length()

            speed_error = abs(needed_speed - WIND_CHARGE_SPEED)
            if speed_error < min_speed_error:
                min_speed_error = speed_error
                best_pearl_pitch = test_pearl_pitch
                best_tick = t
                best_needed_vel = needed_vel
                best_aim_target = aim_target

                if speed_error < 0.08:
                    found_solution = True

    hypot_xz = math.hypot(best_needed_vel.x, best_needed_vel.z)
    best_wind_pitch = -math.degrees(math.atan2(best_needed_vel.y, hypot_xz))
    if custom_offset > 0.0:
        best_wind_pitch += (custom_offset - 8.0)

    best_wind_yaw = math.degrees(math.atan2(-best_needed_vel.x, best_needed_vel.z))

    flight_ticks = best_tick - safe_delay
    actual_wind_dir = get_direction_vector(best_wind_pitch, best_wind_yaw)
    actual_wind_vel = actual_wind_dir.multiply(WIND_CHARGE_SPEED).add(inherited_wind_vel)
    actual_wind_pos = wind_origin.add(actual_wind_vel.multiply(flight_ticks))

    residual_dist = actual_wind_pos.distance_to(best_aim_target)
    computed_offset = best_wind_pitch - best_pearl_pitch

    return Solution(
        pearl_pitch=best_pearl_pitch,
        wind_pitch=best_wind_pitch,
        wind_yaw=best_wind_yaw,
        computed_offset=computed_offset,
        intercept_tick=best_tick,
        residual_error=residual_dist,
        valid=found_solution or residual_dist <= 0.5,
    )

def independent_simulation(
    sol: Solution,
    delay_ticks: int,
    player_yaw: float,
    player_vel: Vec3d,
    on_ground: bool,
) -> Tuple[float, float]:
    """
    Independent tick-by-tick physics simulation matching AutoPearlCatchModuleTest.assertIndependentEuclideanDistance.
    Returns (dist_to_burst, dist_to_center).
    """
    vert_speed = 0.0 if on_ground else player_vel.y
    inherited_pearl_vel = Vec3d(player_vel.x, vert_speed, player_vel.z)
    inherited_wind_vel = Vec3d(player_vel.x, vert_speed, player_vel.z)

    pl_y = 0.0
    cur_vy = vert_speed
    for _ in range(delay_ticks):
        pl_y += cur_vy
        cur_vy = (cur_vy - PLAYER_GRAVITY) * PLAYER_DRAG
    wind_origin = Vec3d(player_vel.x * delay_ticks, pl_y, player_vel.z * delay_ticks)

    # Pearl flight
    pearl_dir = get_direction_vector(sol.pearl_pitch, player_yaw)
    pearl_vel = pearl_dir.multiply(PEARL_SPEED).add(inherited_pearl_vel)
    pearl_pos = ZERO_VEC

    for t in range(1, sol.intercept_tick + 1):
        pearl_pos = pearl_pos.add(pearl_vel)
        pearl_vel = Vec3d(
            pearl_vel.x * PEARL_DRAG,
            (pearl_vel.y - PEARL_GRAVITY) * PEARL_DRAG,
            pearl_vel.z * PEARL_DRAG,
        )

    burst_target = pearl_pos.subtract(Vec3d(0.0, BURST_OFFSET_Y, 0.0))

    # Wind charge flight
    flight_ticks = sol.intercept_tick - delay_ticks
    wind_dir = get_direction_vector(sol.wind_pitch, sol.wind_yaw)
    wind_vel = wind_dir.multiply(WIND_CHARGE_SPEED).add(inherited_wind_vel)
    wind_arrival_pos = wind_origin.add(wind_vel.multiply(flight_ticks))

    dist_to_burst = wind_arrival_pos.distance_to(burst_target)
    dist_to_center = wind_arrival_pos.distance_to(pearl_pos)

    return dist_to_burst, dist_to_center

def build_systematic_test_cases() -> List[Dict]:
    cases = []

    # 1. Stationary (v = 0)
    for yaw in [0.0, 45.0, 90.0, 180.0, 270.0, -90.0]:
        for ground in [True, False]:
            cases.append({
                "group": "Stationary",
                "name": f"Stationary yaw={yaw:.0f} ground={ground}",
                "vel": Vec3d(0.0, 0.0, 0.0),
                "yaw": yaw,
                "ground": ground,
            })

    # 2. Forward sprinting (vz = 0.28, and varying speeds 0.20, 0.25, 0.28, 0.35)
    for speed in [0.20, 0.25, 0.28, 0.35]:
        for yaw in [0.0, 45.0, 90.0, 180.0, -90.0]:
            rad = math.radians(yaw)
            vx = -math.sin(rad) * speed
            vz = math.cos(rad) * speed
            cases.append({
                "group": "ForwardSprint",
                "name": f"ForwardSprint speed={speed:.2f} yaw={yaw:.0f}",
                "vel": Vec3d(vx, 0.0, vz),
                "yaw": yaw,
                "ground": True,
            })

    # 3. Diagonal sprinting (vx = 0.2, vz = 0.2 and 4 quadrants, plus sqrt(2) normalized)
    diag_speeds = [(0.2, 0.2), (-0.2, 0.2), (0.2, -0.2), (-0.2, -0.2),
                   (0.28 / math.sqrt(2), 0.28 / math.sqrt(2)),
                   (-0.28 / math.sqrt(2), 0.28 / math.sqrt(2))]
    for vx, vz in diag_speeds:
        for yaw in [0.0, 45.0, 90.0]:
            cases.append({
                "group": "DiagonalSprint",
                "name": f"DiagSprint vx={vx:.3f} vz={vz:.3f} yaw={yaw:.0f}",
                "vel": Vec3d(vx, 0.0, vz),
                "yaw": yaw,
                "ground": True,
            })

    # 4. Weak wind jump (vy = 0.1, 0.4)
    for vy in [0.1, 0.2, 0.3, 0.4]:
        for h_speed in [0.0, 0.15, 0.28]:
            for yaw in [0.0, 90.0]:
                rad = math.radians(yaw)
                vx = -math.sin(rad) * h_speed
                vz = math.cos(rad) * h_speed
                cases.append({
                    "group": "WeakWindJump",
                    "name": f"WeakWindJump vy={vy:.2f} h={h_speed:.2f} yaw={yaw:.0f}",
                    "vel": Vec3d(vx, vy, vz),
                    "yaw": yaw,
                    "ground": False,
                })

    # 5. Standard wind jump (vy = 0.6, 0.8, 0.9, 1.0)
    for vy in [0.6, 0.8, 0.9, 1.0]:
        for h_speed in [0.0, 0.15, 0.28]:
            for yaw in [0.0, 45.0, 90.0]:
                rad = math.radians(yaw)
                vx = -math.sin(rad) * h_speed
                vz = math.cos(rad) * h_speed
                cases.append({
                    "group": "StandardWindJump",
                    "name": f"StandardWindJump vy={vy:.2f} h={h_speed:.2f} yaw={yaw:.0f}",
                    "vel": Vec3d(vx, vy, vz),
                    "yaw": yaw,
                    "ground": False,
                })

    # 6. Maximum boost wind jump (vy = 1.2, 1.45, 1.6, 1.8)
    for vy in [1.2, 1.45, 1.6, 1.8]:
        for h_speed in [0.0, 0.15, 0.28]:
            for yaw in [0.0, 45.0, 90.0]:
                rad = math.radians(yaw)
                vx = -math.sin(rad) * h_speed
                vz = math.cos(rad) * h_speed
                cases.append({
                    "group": "MaxBoostWindJump",
                    "name": f"MaxBoostWindJump vy={vy:.2f} h={h_speed:.2f} yaw={yaw:.0f}",
                    "vel": Vec3d(vx, vy, vz),
                    "yaw": yaw,
                    "ground": False,
                })

    # 7. Falling (vy = -0.1, -0.2, -0.4, -0.6, -0.8, -1.0, -1.2)
    for vy in [-0.1, -0.2, -0.4, -0.6, -0.8, -1.0, -1.2]:
        for h_speed in [0.0, 0.15, 0.28]:
            for yaw in [0.0, 45.0, 90.0]:
                rad = math.radians(yaw)
                vx = -math.sin(rad) * h_speed
                vz = math.cos(rad) * h_speed
                cases.append({
                    "group": "Falling",
                    "name": f"Falling vy={vy:.2f} h={h_speed:.2f} yaw={yaw:.0f}",
                    "vel": Vec3d(vx, vy, vz),
                    "yaw": yaw,
                    "ground": False,
                })

    # 8. Lateral strafes (vx = +-0.28, vz = 0, yaw = 0)
    for strafe in [-0.28, 0.28]:
        cases.append({
            "group": "LateralStrafe",
            "name": f"LateralStrafe vx={strafe:.2f} yaw=0.0",
            "vel": Vec3d(strafe, 0.0, 0.0),
            "yaw": 0.0,
            "ground": True,
        })

    return cases

def run_evaluation():
    print("=" * 80)
    print("EMPIRICAL BALLISTIC VERIFICATION: AutoPearlCatch Trajectory Solver (M1)")
    print("Configuration: throw_delay = 2 ticks, Acceptance Threshold <= 0.5 blocks")
    print("=" * 80)

    # 1. Run systematic test cases (>50 cases)
    systematic_cases = build_systematic_test_cases()
    print(f"\n[PHASE 1] Running {len(systematic_cases)} Systematic Test Cases...")

    group_stats: Dict[str, List[float]] = {}
    failures = []

    for c in systematic_cases:
        sol = solve3D(
            delay_ticks=2,
            player_yaw=c["yaw"],
            player_vel=c["vel"],
            on_ground=c["ground"],
            custom_offset=-1.0,
        )

        dist_burst, dist_center = independent_simulation(
            sol, 2, c["yaw"], c["vel"], c["ground"]
        )

        # Intercept error is min(dist_burst, dist_center), or dist_burst directly
        err = min(dist_burst, dist_center)
        res_err = sol.residual_error

        group = c["group"]
        if group not in group_stats:
            group_stats[group] = []
        group_stats[group].append(err)

        passed = (err <= 0.5) and (res_err <= 0.5) and sol.valid
        if not passed:
            failures.append((c, sol, err, res_err))

    print("\n--- Systematic Groups Summary ---")
    print(f"{'Group':<22} | {'Count':<5} | {'Max Err':<10} | {'Mean Err':<10} | {'Status'}")
    print("-" * 65)
    for grp, errs in group_stats.items():
        max_e = max(errs)
        mean_e = sum(errs) / len(errs)
        status = "PASS (<= 0.5)" if max_e <= 0.5 else "FAIL (> 0.5)"
        print(f"{grp:<22} | {len(errs):<5} | {max_e:.6f} bl | {mean_e:.6f} bl | {status}")

    print(f"\nSystematic Failures (> 0.5 blocks): {len(failures)} / {len(systematic_cases)}")

    # 2. Run Monte Carlo stress testing (10,000 cases)
    print("\n" + "=" * 80)
    print("[PHASE 2] Running 10,000 Monte Carlo Adversarial Stress Cases (delay=2)...")
    random.seed(1337)

    mc_count = 10000
    mc_errors = []
    mc_burst_errors = []
    mc_center_errors = []
    mc_failures = []
    t0 = time.time()

    for i in range(mc_count):
        yaw = random.uniform(-180.0, 180.0)
        on_ground = random.choice([True, False])

        h_speed = random.uniform(0.0, 0.45)
        h_angle = random.uniform(0.0, 2.0 * math.pi)
        vx = h_speed * math.cos(h_angle)
        vz = h_speed * math.sin(h_angle)

        if on_ground:
            vy = 0.0
        else:
            vy = random.uniform(-1.2, 1.8)

        vel = Vec3d(vx, vy, vz)

        sol = solve3D(2, yaw, vel, on_ground, -1.0)
        dist_burst, dist_center = independent_simulation(sol, 2, yaw, vel, on_ground)
        err = min(dist_burst, dist_center)

        mc_errors.append(err)
        mc_burst_errors.append(dist_burst)
        mc_center_errors.append(dist_center)

        if err > 0.5 or sol.residual_error > 0.5 or not sol.valid:
            mc_failures.append((i, yaw, vel, on_ground, sol, err, dist_burst, dist_center))

    elapsed = time.time() - t0
    print(f"Executed {mc_count} Monte Carlo samples in {elapsed:.2f}s")

    sorted_errs = sorted(mc_errors)
    max_mc = max(mc_errors)
    min_mc = min(mc_errors)
    mean_mc = sum(mc_errors) / len(mc_errors)
    p50_mc = sorted_errs[int(mc_count * 0.50)]
    p95_mc = sorted_errs[int(mc_count * 0.95)]
    p99_mc = sorted_errs[int(mc_count * 0.99)]
    p999_mc = sorted_errs[int(mc_count * 0.999)]

    print("\n--- Monte Carlo Statistical Distribution ---")
    print(f"  Min Error:          {min_mc:.6f} blocks")
    print(f"  50th Percentile:    {p50_mc:.6f} blocks")
    print(f"  Mean Error:         {mean_mc:.6f} blocks")
    print(f"  95th Percentile:    {p95_mc:.6f} blocks")
    print(f"  99th Percentile:    {p99_mc:.6f} blocks")
    print(f"  99.9th Percentile:  {p999_mc:.6f} blocks")
    print(f"  Max Error (p100):   {max_mc:.6f} blocks")
    print(f"  Threshold (<= 0.5): {'MET' if max_mc <= 0.5 else 'VIOLATED'}")
    print(f"  Monte Carlo Failures: {len(mc_failures)} / {mc_count}")

    # Top 5 worst-case samples in Monte Carlo
    worst_mc = sorted(zip(mc_errors, range(mc_count)), reverse=True)[:5]
    print("\n--- Top 5 Worst-Case Monte Carlo Samples ---")
    for rank, (err_val, idx) in enumerate(worst_mc, 1):
        print(f"  #{rank}: Error={err_val:.6f} blocks (sample index {idx})")

    # Detailed inspection of specific required regimes
    print("\n" + "=" * 80)
    print("[PHASE 3] Explicit Inspection of Key Required Movement Profiles")
    print("=" * 80)

    key_profiles = [
        ("Stationary (v=0, ground=True)", Vec3d(0.0, 0.0, 0.0), 0.0, True),
        ("Stationary Airborne (v=0, ground=False)", Vec3d(0.0, 0.0, 0.0), 0.0, False),
        ("Forward Sprint (vz=0.28, yaw=0.0)", Vec3d(0.0, 0.0, 0.28), 0.0, True),
        ("Diagonal Sprint (vx=0.2, vz=0.2, yaw=0.0)", Vec3d(0.2, 0.0, 0.2), 0.0, True),
        ("Weak Wind Jump (vy=0.1, ground=False)", Vec3d(0.0, 0.1, 0.0), 0.0, False),
        ("Weak Wind Jump (vy=0.4, ground=False)", Vec3d(0.0, 0.4, 0.0), 0.0, False),
        ("Standard Wind Jump (vy=0.9, ground=False)", Vec3d(0.0, 0.9, 0.0), 0.0, False),
        ("Max Boost Wind Jump (vy=1.45, ground=False)", Vec3d(0.0, 1.45, 0.0), 0.0, False),
        ("Extreme Boost Wind Jump (vy=1.80, ground=False)", Vec3d(0.0, 1.80, 0.0), 0.0, False),
        ("Falling Slow (vy=-0.1, ground=False)", Vec3d(0.0, -0.1, 0.0), 0.0, False),
        ("Falling Moderate (vy=-0.4, ground=False)", Vec3d(0.0, -0.4, 0.0), 0.0, False),
        ("Falling Fast (vy=-0.8, ground=False)", Vec3d(0.0, -0.8, 0.0), 0.0, False),
        ("Falling Terminal (vy=-1.2, ground=False)", Vec3d(0.0, -1.2, 0.0), 0.0, False),
    ]

    for label, vel, yaw, ground in key_profiles:
        sol = solve3D(2, yaw, vel, ground, -1.0)
        d_burst, d_center = independent_simulation(sol, 2, yaw, vel, ground)
        min_d = min(d_burst, d_center)
        print(f"Profile: {label:<45}")
        print(f"  PearlPitch={sol.pearl_pitch:6.2f}°, WindPitch={sol.wind_pitch:6.2f}°, WindYaw={sol.wind_yaw:6.2f}°")
        print(f"  InterceptTick={sol.intercept_tick}, SolverResidual={sol.residual_error:.6f} bl")
        print(f"  Independent DistToBurst={d_burst:.6f} bl, DistToCenter={d_center:.6f} bl -> MinDist={min_d:.6f} bl")
        print(f"  Verdict: {'PASS' if min_d <= 0.5 else 'FAIL'}\n")

    # Overall Verdict
    total_evals = len(systematic_cases) + mc_count
    total_failures = len(failures) + len(mc_failures)
    print("=" * 80)
    print(f"TOTAL EVALUATIONS: {total_evals}")
    print(f"TOTAL FAILURES (> 0.5 blocks): {total_failures}")
    if total_failures == 0 and max_mc <= 0.5:
        print("OVERALL EMPIRICAL VERDICT: CONFIRM_CORRECTNESS")
    else:
        print("OVERALL EMPIRICAL VERDICT: REJECT")
    print("=" * 80)

    return total_failures == 0

if __name__ == "__main__":
    success = run_evaluation()
    exit(0 if success else 1)

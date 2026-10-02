import os
import sys
import time
import json
import subprocess

if sys.stdout.encoding != 'utf-8':
    try:
        sys.stdout.reconfigure(encoding='utf-8')
        sys.stderr.reconfigure(encoding='utf-8')
    except Exception:
        pass

from audit_process_memory import scan_process_memory, get_loaded_modules

def run_audit():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    os.chdir(root)

    build_dir = os.path.join(root, "build")
    game_build_dir = os.path.join(build_dir, "visual-smoke", "game", "build")
    os.makedirs(game_build_dir, exist_ok=True)

    pid_file = os.path.join(game_build_dir, "capitulation_smoke_pid.txt")
    before_file = os.path.join(game_build_dir, "smoke_state_before.txt")
    after_file = os.path.join(game_build_dir, "smoke_state_after.txt")

    for f in [pid_file, before_file, after_file]:
        if os.path.exists(f):
            try: os.remove(f)
            except: pass

    terms = [
        "Nivorat", "AutoCart", "ClickPearl", "Capitulation",
        "VortexHUD", "activity.client", "AutoTotem", "AutoSpear"
    ]

    print("[1/5] Mapping subst V: drive and starting Minecraft in-game Capitulation test...")
    subprocess.run('subst V: "' + root + '"', shell=True)

    gradle_cmd = 'cmd /c "V: && .\\gradlew.bat publicVisualSmoke -PecosystemSmoke --init-script tools/public-visual-smoke.init.gradle"'
    proc = subprocess.Popen(gradle_cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)

    pid = None
    print("[2/5] Waiting for Minecraft process (javaw.exe) to initialize...")
    start_time = time.time()
    while time.time() - start_time < 90:
        if os.path.exists(pid_file):
            try:
                with open(pid_file, "r") as f:
                    content = f.read().strip()
                    if content:
                        pid = int(content)
                        break
            except:
                pass
        if proc.poll() is not None:
            break
        time.sleep(0.5)

    if not pid:
        out, _ = proc.communicate(timeout=10)
        subprocess.run('subst V: /d', shell=True)
        print("Failed to acquire Minecraft PID. Output:\n", out[-1000:])
        return

    print(f"-> Minecraft active! Detected PID: {pid}")

    print("[3/5] Waiting for ACTIVE state (in-game with menu open, modules ready, before capitulation)...")
    while time.time() - start_time < 120:
        if os.path.exists(before_file):
            break
        if proc.poll() is not None:
            break
        time.sleep(0.3)

    print("-> Taking Memory Snapshot 1 (BEFORE Capitulation)...")
    snap_before = scan_process_memory(pid, terms)
    mods_before = get_loaded_modules(pid)

    print("[4/5] Waiting for CAPITULATION trigger (2-second hold, full deactivation & GC scrub)...")
    while time.time() - start_time < 150:
        if os.path.exists(after_file):
            break
        if proc.poll() is not None:
            break
        time.sleep(0.3)

    print("-> Taking Memory Snapshot 2 (AFTER Capitulation)...")
    snap_after = scan_process_memory(pid, terms)
    mods_after = get_loaded_modules(pid)

    done_file = os.path.join(game_build_dir, "smoke_audit_done.txt")
    try:
        with open(done_file, "w") as f:
            f.write("DONE")
    except Exception as e:
        print(f"Warning: could not write done_file: {e}")

    print("[5/5] Waiting for game test completion...")
    out, _ = proc.communicate(timeout=60)
    subprocess.run('subst V: /d', shell=True)

    report = {
        "timestamp": time.strftime("%Y-%m-%d %H:%M:%S"),
        "target_pid": pid,
        "before_capitulation": snap_before,
        "after_capitulation": snap_after,
        "loaded_modules_before": mods_before,
        "loaded_modules_after": mods_after
    }

    report_path = os.path.join(build_dir, "capitulation_memory_audit.json")
    with open(report_path, "w", encoding="utf-8") as f:
        json.dump(report, f, indent=2, ensure_ascii=False)

    print("\n" + "=" * 80)
    print("      CAPITULATION IN-GAME RUN & PROCESS HACKER AUDIT REPORT")
    print("=" * 80)
    print(f"Minecraft PID: {pid}")
    print(f"Memory scanned: {snap_before.get('scanned_mb')} MB (Before) -> {snap_after.get('scanned_mb')} MB (After)")
    print("\n--- String Search Occurrences in Virtual Memory (Process Hacker Strings equivalent) ---")
    print(f"{'SEARCH STRING':<25} | {'BEFORE CAPITULATION':<20} | {'AFTER CAPITULATION':<20}")
    print("-" * 75)
    for term in terms:
        c1 = snap_before.get("string_hits", {}).get(term, 0)
        c2 = snap_after.get("string_hits", {}).get(term, 0)
        print(f"{term:<25} | {c1:<20} | {c2:<20}")

    print("\n--- Loaded Modules / File Handles ---")
    print(f"Total signature modules before: {len(mods_before)}, after: {len(mods_after)}")
    for m in mods_after:
        print(f"  - {m}")
    print("=" * 80)
    print(f"Detailed JSON report saved to: {report_path}\n")

if __name__ == '__main__':
    run_audit()

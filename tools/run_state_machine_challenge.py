import os
import sys
import subprocess

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
if sys.stderr.encoding != 'utf-8':
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')

print("================================================================================")
print("EXECUTING STATE MACHINE EMPIRICAL CHALLENGE (PearlCatchStateMachineChallenger)")
print("================================================================================")

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
    print("ERROR: cp_file not found")
    sys.exit(1)

with open(cp_file, "r", encoding="utf-8", errors="ignore") as f:
    lines = f.readlines()

print(f"Loaded classpath descriptor: {cp_file}")
raw_cp = "".join(lines[1:]).strip().strip('"')
raw_cp = raw_cp.replace("X:\\\\", r"C:\Users\Administrator\Desktop\GUI~1\\")
raw_cp = raw_cp.replace("X:\\", r"C:\Users\Administrator\Desktop\GUI~1\\")
raw_cp = raw_cp.replace(r"C:\Users\Administrator\Desktop\Работа с GUI", r"C:\Users\Administrator\Desktop\GUI~1")
raw_cp = raw_cp.replace("C:/Users/Administrator/Desktop/Работа с GUI", "C:/Users/Administrator/Desktop/GUI~1")
raw_cp = r"C:\Users\Administrator\Desktop\GUI~1\build\classes\java\test;C:\Users\Administrator\Desktop\GUI~1\build\classes\java\main;" + raw_cp

javac_exe = r"C:\Program Files\Java\jdk-21.0.10\bin\javac.exe"
java_exe = r"C:\Program Files\Java\jdk-21.0.10\bin\java.exe"

source_file = r"C:\Users\Administrator\Desktop\GUI~1\tools\PearlCatchStateMachineChallenger.java"
print(f"Compiling {source_file}...")
res_compile = subprocess.run([javac_exe, "-encoding", "UTF-8", "-cp", raw_cp, source_file], capture_output=True, text=True)
if res_compile.returncode != 0:
    print("Compilation failed:\n", res_compile.stderr)
    sys.exit(1)

print("Compilation successful! Executing Java Empirical Challenger on JVM...\n")
run_cp = raw_cp + r";C:\Users\Administrator\Desktop\GUI~1"
res_run = subprocess.run([java_exe, "-Dfile.encoding=UTF-8", "-cp", run_cp, "tools.PearlCatchStateMachineChallenger"], capture_output=True, text=True)
print(res_run.stdout)
if res_run.stderr:
    print("STDERR:\n", res_run.stderr)

sys.exit(res_run.returncode)

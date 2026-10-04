import os
import re

SRC_DIR = r"V:\src\main\java"
str_pat = re.compile(r'"[^"\\]*(?:\\.[^"\\]*)*"')

# We want to find files where comments exist
found = []

for root, _, files in os.walk(SRC_DIR):
    for f in files:
        if f.endswith(".java"):
            fpath = os.path.join(root, f)
            with open(fpath, "r", encoding="utf-8") as file:
                lines = file.readlines()
            
            modified = False
            new_lines = []
            for idx, line in enumerate(lines, 1):
                # Check for single line comments: // ...
                # Be careful not to strip inside strings
                # Find occurrences of // that are not in strings
                # Split line by string literals
                parts = []
                last_end = 0
                for match in str_pat.finditer(line):
                    parts.append((match.start(), match.end(), "str"))
                
                # Check where // occurs outside string matches
                comment_idx = line.find("//")
                while comment_idx != -1:
                    # check if inside string
                    in_str = any(start <= comment_idx < end for start, end, t in parts)
                    if not in_str:
                        # Found comment
                        line_cleaned = line[:comment_idx].rstrip() + "\n"
                        if line_cleaned.strip() == "":
                            line_cleaned = "" # empty line if comment was the whole line
                        found.append((fpath, idx, line.strip()))
                        line = line_cleaned
                        modified = True
                        break
                    comment_idx = line.find("//", comment_idx + 2)
                
                if line != "":
                    new_lines.append(line)
            
            if modified:
                with open(fpath, "w", encoding="utf-8") as file:
                    file.writelines(new_lines)
                print(f"Stripped comments from {fpath}")

print(f"Found and stripped {len(found)} comments.")
for p, l, text in found:
    print(f"  {os.path.basename(p)}:{l} -> {text}")

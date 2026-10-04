import os
import re

SRC_DIR = r"V:\src\main\java"
str_pat = re.compile(r'"(?:[^"\\]|\\.)*"')

def strip_comments_from_code(code):
    strings = []
    def save_str(m):
        idx = len(strings)
        strings.append(m.group(0))
        return f"___STRING_LITERAL_{idx}___"

    code_no_str = str_pat.sub(save_str, code)
    code_no_block = re.sub(r'/\*.*?\*/', '', code_no_str, flags=re.DOTALL)
    code_no_line = re.sub(r'//[^\r\n]*', '', code_no_block)

    for idx, s in enumerate(strings):
        code_no_line = code_no_line.replace(f"___STRING_LITERAL_{idx}___", s)

    return code_no_line

files_modified = 0
for root, _, files in os.walk(SRC_DIR):
    for f in files:
        if f.endswith(".java"):
            p = os.path.join(root, f)
            with open(p, "r", encoding="utf-8") as file:
                orig = file.read()
            cleaned = strip_comments_from_code(orig)
            if cleaned != orig:
                with open(p, "w", encoding="utf-8") as file:
                    file.write(cleaned)
                files_modified += 1

print(f"Stripped all comments from {files_modified} files.")

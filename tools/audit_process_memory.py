import sys
import os
import time
import ctypes
from ctypes import wintypes

PROCESS_QUERY_INFORMATION = 0x0400
PROCESS_VM_READ = 0x0010

class MEMORY_BASIC_INFORMATION(ctypes.Structure):
    _fields_ = [
        ("BaseAddress", ctypes.c_void_p),
        ("AllocationBase", ctypes.c_void_p),
        ("AllocationProtect", wintypes.DWORD),
        ("PartitionId", wintypes.WORD),
        ("RegionSize", ctypes.c_size_t),
        ("State", wintypes.DWORD),
        ("Protect", wintypes.DWORD),
        ("Type", wintypes.DWORD),
    ]

MEM_COMMIT = 0x1000
PAGE_NOACCESS = 0x01
PAGE_GUARD = 0x100

READABLE_PROTECTIONS = [
    0x02, # PAGE_READONLY
    0x04, # PAGE_READWRITE
    0x20, # PAGE_EXECUTE_READ
    0x40, # PAGE_EXECUTE_READWRITE
]

def scan_process_memory(pid, search_terms):
    kernel32 = ctypes.windll.kernel32
    psapi = ctypes.windll.psapi
    handle = kernel32.OpenProcess(PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, False, pid)
    if not handle:
        return {"error": f"Failed to open process {pid}, error: {ctypes.GetLastError()}", "scanned_mb": 0.0, "regions": 0, "string_hits": {}, "mapped_jars": []}

    mbi = MEMORY_BASIC_INFORMATION()
    address = 0
    max_address = 0x7FFFFFFFFFFF if sys.maxsize > 2**32 else 0x7FFFFFFF

    results = {term: 0 for term in search_terms}
    total_scanned_bytes = 0
    total_regions = 0
    mapped_jars = set()

    try:
        while address < max_address:
            res = kernel32.VirtualQueryEx(handle, ctypes.c_void_p(address), ctypes.byref(mbi), ctypes.sizeof(mbi))
            if res == 0:
                break

            base = mbi.BaseAddress or 0
            size = mbi.RegionSize or 0

            if mbi.State == MEM_COMMIT and not (mbi.Protect & PAGE_GUARD) and not (mbi.Protect & PAGE_NOACCESS):
                if any((mbi.Protect & p) == p for p in READABLE_PROTECTIONS):
                    total_regions += 1
                    
                    if mbi.Type in (0x40000, 0x1000000): # MEM_MAPPED, MEM_IMAGE
                        name_buf = ctypes.create_unicode_buffer(1024)
                        if psapi.GetMappedFileNameW(handle, ctypes.c_void_p(base), name_buf, 1024):
                            fn = name_buf.value
                            if any(k in fn.lower() for k in ['nivorat', 'vortex', 'cooldownhud', 'smoke']):
                                mapped_jars.add(fn)

                    buffer = (ctypes.c_char * min(size, 4 * 1024 * 1024))()
                    bytes_read = ctypes.c_size_t()
                    chunk_offset = 0
                    while chunk_offset < size:
                        chunk_size = min(size - chunk_offset, len(buffer))
                        if kernel32.ReadProcessMemory(handle, ctypes.c_void_p(base + chunk_offset), buffer, chunk_size, ctypes.byref(bytes_read)):
                            read_len = bytes_read.value
                            total_scanned_bytes += read_len
                            data = bytes(buffer[:read_len])
                            for term in search_terms:
                                term_bytes_ascii = term.encode('utf-8')
                                term_bytes_lower = term.lower().encode('utf-8')
                                count = data.count(term_bytes_ascii)
                                if term_bytes_lower != term_bytes_ascii:
                                    count += data.count(term_bytes_lower)
                                results[term] += count
                        chunk_offset += chunk_size

            address = base + size
    finally:
        kernel32.CloseHandle(handle)

    return {
        "pid": pid,
        "scanned_mb": round(total_scanned_bytes / (1024 * 1024), 2),
        "regions": total_regions,
        "string_hits": results,
        "mapped_jars": sorted(list(mapped_jars))
    }

def get_loaded_modules(pid):
    try:
        kernel32 = ctypes.windll.kernel32
        psapi = ctypes.windll.psapi

        kernel32.OpenProcess.restype = wintypes.HANDLE
        kernel32.OpenProcess.argtypes = [wintypes.DWORD, wintypes.BOOL, wintypes.DWORD]

        psapi.EnumProcessModulesEx.restype = wintypes.BOOL
        psapi.EnumProcessModulesEx.argtypes = [
            wintypes.HANDLE,
            ctypes.POINTER(wintypes.HMODULE),
            wintypes.DWORD,
            ctypes.POINTER(wintypes.DWORD),
            wintypes.DWORD
        ]

        h_process = kernel32.OpenProcess(0x0400 | 0x0010, False, pid)
        if not h_process:
            return []
        max_mods = 1024
        h_modules = (wintypes.HMODULE * max_mods)()
        needed = wintypes.DWORD()
        res = []
        cb = wintypes.DWORD(ctypes.sizeof(h_modules))
        if psapi.EnumProcessModulesEx(h_process, h_modules, cb, ctypes.byref(needed), 0x03):
            count = min(needed.value // ctypes.sizeof(wintypes.HMODULE), max_mods)
            for i in range(count):
                mod_name = ctypes.create_unicode_buffer(1024)
                if psapi.GetModuleFileNameExW(h_process, h_modules[i], mod_name, 1024):
                    fn = mod_name.value
                    if any(k in fn.lower() for k in ['nivorat', 'vortex', 'activity', 'smoke', 'minecraft', 'java']):
                        res.append(fn)
        kernel32.CloseHandle(h_process)
        return res
    except Exception as e:
        return [str(e)]

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Usage: python audit_process_memory.py <PID>")
        sys.exit(1)

    pid = int(sys.argv[1])
    terms = ["Nivorat", "AutoCart", "ClickPearl", "Capitulation", "VortexHUD", "activity.client", "AutoTotem", "AutoSpear"]
    res = scan_process_memory(pid, terms)
    mods = get_loaded_modules(pid)
    print("=== PROCESS HACKER MEMORY STRING AUDIT SIMULATION ===")
    print(f"Target PID: {pid}")
    print(f"Scanned Memory: {res.get('scanned_mb')} MB across {res.get('regions')} regions")
    print("String Hit Results:")
    for k, v in res.get("string_hits", {}).items():
        print(f"  [{k}]: {v} occurrences found in memory")
    print("Mapped JAR files:")
    for j in res.get("mapped_jars", []):
        print(f"  - {j}")
    print("Loaded Modules matching signatures:")
    for m in mods:
        print(f"  - {m}")

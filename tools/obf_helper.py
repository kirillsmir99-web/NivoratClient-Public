import struct

SEED = 0x5A7C3D1E
K_INT = 0x5A7C3D1E
K_LONG = 0x5A7C3D1E5A7C3D1E

def encode_s(s: str) -> str:
    k = SEED
    data = []
    for b in s.encode('utf-8'):
        k = (k * 1664525 + 1013904223) & 0xFFFFFFFF
        keystream = (k >> 16) & 0xFF
        data.append(b ^ keystream)
    return 'new byte[] {' + ', '.join(f'(byte) 0x{x:02X}' for x in data) + '}'

def mask_f(val: float) -> str:
    bits = struct.unpack('>I', struct.pack('>f', val))[0]
    return f'0x{(bits ^ K_INT):08X}'

def mask_d(val: float) -> str:
    bits = struct.unpack('>Q', struct.pack('>d', val))[0]
    return f'0x{(bits ^ K_LONG):016X}L'

def mask_i(val: int) -> str:
    return f'0x{(val ^ K_INT):08X}'

def mask_l(val: int) -> str:
    return f'0x{(val ^ K_LONG):016X}L'

if __name__ == '__main__':
    words = ['rail', 'cart', 'valid_timing', 'timing_outlier', 'prediction', 'confidence', 'calibration', 'humanized', 'rotation', 'score', 'target', 'trajectory']
    for w in words:
        print(f'// "{w}"')
        print(f'Obf.s({encode_s(w)})')

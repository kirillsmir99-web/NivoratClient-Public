import struct

DOMAINS = {
    "Arc": {
        "f_mask": 0x6B8D4E2F,
        "d_mask": 0x6B8D4E2F7A1C5B3E,
        "i_mask": 0x4A3C2B1D,
    },
    "Mace": {
        "f_mask": 0x7C9E5F3A,
        "d_mask": 0x7C9E5F3A1D4E8F2C,
        "i_mask": 0x3E2D1C0B,
    },
    "Raycast": {
        "f_mask": 0x3D1A9C8B,
        "d_mask": 0x3D1A9C8B5F2E7D1A,
        "i_mask": 0x6C5B4A39,
    },
    "Combat": {
        "f_mask": 0x4E2B8D7C,
        "d_mask": 0x4E2B8D7C3B1A9F5E,
        "i_mask": 0x5D4C3B2A,
    },
    "Timing": {
        "f_mask": 0x5F3C7E6D,
        "d_mask": 0x5F3C7E6D2C0F8E4D,
        "i_mask": 0x7B6A5948,
        "l_mask": 0x7B6A59483C2D1E0F,
    },
    "Config": {
        "f_mask": 0x2A9F1B8E,
        "d_mask": 0x2A9F1B8E4D3C2B1A,
        "i_mask": 0x1F2E3D4C,
    }
}

def float_to_bits(val: float) -> int:
    return struct.unpack('>I', struct.pack('>f', float(val)))[0]

def double_to_bits(val: float) -> int:
    return struct.unpack('>Q', struct.pack('>d', float(val)))[0]

def encode_float(domain: str, val: float) -> str:
    raw = float_to_bits(val)
    masked = raw ^ DOMAINS[domain]["f_mask"]
    # signed 32-bit integer hex or dec
    signed = struct.unpack('>i', struct.pack('>I', masked))[0]
    return f"{domain}Domain.f({signed}) /* {val}f */"

def encode_double(domain: str, val: float) -> str:
    raw = double_to_bits(val)
    masked = raw ^ DOMAINS[domain]["d_mask"]
    signed = struct.unpack('>q', struct.pack('>Q', masked))[0]
    return f"{domain}Domain.d({signed}L) /* {val} */"

def encode_int(domain: str, val: int) -> str:
    masked = (val & 0xFFFFFFFF) ^ DOMAINS[domain]["i_mask"]
    signed = struct.unpack('>i', struct.pack('>I', masked))[0]
    return f"{domain}Domain.i({signed}) /* {val} */"

def encode_long(domain: str, val: int) -> str:
    masked = (val & 0xFFFFFFFFFFFFFFFF) ^ DOMAINS[domain]["l_mask"]
    signed = struct.unpack('>q', struct.pack('>Q', masked))[0]
    return f"{domain}Domain.l({signed}L) /* {val}L */"

if __name__ == "__main__":
    print("ArcDomain 0.99d:", encode_double("Arc", 0.99))
    print("ArcDomain 0.05d:", encode_double("Arc", 0.05))
    print("ArcDomain 80:", encode_int("Arc", 80))
    print("MaceDomain 7.0d:", encode_double("Mace", 7.0))
    print("MaceDomain 25.0d:", encode_double("Mace", 25.0))
    print("ConfigDomain 50:", encode_int("Config", 50))
    print("ConfigDomain 80:", encode_int("Config", 80))
    print("ConfigDomain 110:", encode_int("Config", 110))

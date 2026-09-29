#!/usr/bin/env python3
"""
Safe JVM .class file rebrander, used to turn GGDonutShop.jar into
AstraDonutShop.jar.

Rewrites only CONSTANT_Utf8 constant-pool entries (class/member names,
descriptors, signatures, and string literal contents) using an ordered
list of exact byte-string replacements. All other constant-pool entry
types and every byte after the constant pool are copied through
unmodified.

Why this is safe: the JVM class file constant pool is simply a
sequential list of self-length-prefixed entries. There is no outer
"constant pool length" field anywhere else in the class file format;
everything past the constant pool (fields, methods, Code attributes,
etc.) references entries purely by *index*, never by absolute byte
offset. Because this tool never adds, removes, or reorders constant
pool entries -- it only rewrites the bytes of existing Utf8 entries,
possibly changing their length -- every other part of the class file
(including every method's actual bytecode, stack map tables, and line
number tables) stays byte-for-byte identical. This was verified for
every class in GGDonutShop.jar: each method's Code attribute bytes are
identical before and after patching.

Usage:
    python3 classpatch.py <in.class> <out.class>

See rebrand_jar.py in this directory for the full jar rebuild driver.
"""
import struct
import sys

# Tag -> fixed payload size in bytes (excluding the 1-byte tag itself),
# for every constant pool entry type except Utf8 (tag 1), which is
# variable-length and handled specially.
FIXED_SIZES = {
    3: 4,   # Integer
    4: 4,   # Float
    5: 8,   # Long (takes 2 cp slots)
    6: 8,   # Double (takes 2 cp slots)
    7: 2,   # Class
    8: 2,   # String
    9: 4,   # Fieldref
    10: 4,  # Methodref
    11: 4,  # InterfaceMethodref
    12: 4,  # NameAndType
    15: 3,  # MethodHandle
    16: 2,  # MethodType
    17: 4,  # Dynamic
    18: 4,  # InvokeDynamic
    19: 2,  # Module
    20: 2,  # Package
}
DOUBLE_SLOT_TAGS = {5, 6}

# Ordered, exact byte-for-byte replacements used for the
# GGDonutShop -> AstraDonutShop rebrand. Verified against a full string
# dump of every class file in the original jar to avoid any accidental
# substring collisions with unrelated words (e.g. "toggle", "trigger",
# "EGG", "LEGGINGS", "NUGGET" all legitimately contain "gg"/"GG" and
# must NOT be touched).
REPLACEMENTS = [
    (b"net/notdakuxd/nxdonutshop", b"net/astralab/astradonutshop"),
    (b"net.notdakuxd.nxdonutshop", b"net.astralab.astradonutshop"),
    (b"NxSpriteResolver", b"AstraSpriteResolver"),
    (b"NxDonutShop", b"AstraDonutShop"),
    (b"GG DonutShop", b"AstraDonutShop"),
    (b"NotDaKuxD", b"DevSolentz"),
    (b"ggdonutshop.admin", b"astradonutshop.admin"),
    (b"nxdonutshop.admin", b"astradonutshop.admin"),
]


def apply_replacements(data: bytes, replacements=REPLACEMENTS) -> bytes:
    for old, new in replacements:
        data = data.replace(old, new)
    return data


def patch_class_bytes(buf: bytes, replacements=REPLACEMENTS):
    if buf[0:4] != b"\xca\xfe\xba\xbe":
        raise ValueError("Not a class file (bad magic)")

    pos = 8  # magic(4) + minor(2) + major(2)
    (cp_count,) = struct.unpack_from(">H", buf, pos)
    pos += 2

    out = bytearray()
    out += buf[0:pos]  # header through constant_pool_count, unchanged

    changed = 0
    i = 1
    while i < cp_count:
        tag = buf[pos]
        if tag == 1:  # Utf8
            (length,) = struct.unpack_from(">H", buf, pos + 1)
            raw = buf[pos + 3: pos + 3 + length]
            new_raw = apply_replacements(raw, replacements)
            if new_raw != raw:
                changed += 1
            out.append(1)
            out += struct.pack(">H", len(new_raw))
            out += new_raw
            pos += 3 + length
            i += 1
        else:
            size = FIXED_SIZES.get(tag)
            if size is None:
                raise ValueError(f"Unknown constant pool tag {tag} at pos {pos}")
            out += buf[pos: pos + 1 + size]
            pos += 1 + size
            i += 2 if tag in DOUBLE_SLOT_TAGS else 1

    # Everything from here to EOF only ever references the constant pool
    # by index, never by byte offset -> safe to copy verbatim.
    out += buf[pos:]
    return bytes(out), changed


def main():
    if len(sys.argv) != 3:
        print("usage: classpatch.py <in.class> <out.class>", file=sys.stderr)
        sys.exit(1)
    in_path, out_path = sys.argv[1], sys.argv[2]
    with open(in_path, "rb") as f:
        buf = f.read()
    new_buf, changed = patch_class_bytes(buf, REPLACEMENTS)
    with open(out_path, "wb") as f:
        f.write(new_buf)
    print(f"{in_path}: {changed} constant(s) rewritten")


if __name__ == "__main__":
    main()

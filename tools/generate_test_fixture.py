"""Write a deterministic empty 8x8x8 GameTest template using standard NBT."""
import gzip
import struct
from pathlib import Path


def string(value):
    return struct.pack(">H", len(value)) + value.encode("utf-8")


data = b"\x0a\x00\x00"  # Unnamed root compound.
data += b"\x03" + string("DataVersion") + struct.pack(">i", 3465)
data += b"\x09" + string("size") + b"\x03" + struct.pack(">iiii", 3, 8, 8, 8)
data += b"\x09" + string("palette") + b"\x0a" + struct.pack(">i", 1)
data += b"\x08" + string("Name") + string("minecraft:air") + b"\x00"
for name in ("blocks", "entities"):
    data += b"\x09" + string(name) + b"\x0a" + struct.pack(">i", 0)
data += b"\x00"
path = Path(__file__).resolve().parents[1] / "src/gametest/resources/data/conveyance/structures/empty.nbt"
path.parent.mkdir(parents=True, exist_ok=True)
path.write_bytes(gzip.compress(data, mtime=0))

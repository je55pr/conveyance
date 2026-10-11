"""Terrain polish assets; no imported item sprites are modified."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"


def write(root, path, data):
    target = root / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def box(low, high, texture):
    return {"from": low, "to": high, "faces": {side: {"texture": "#" + texture, "uv": [0, 0, 16, 16]}
        for side in ("down", "up", "north", "south", "east", "west")}}


def generate_all(root=ROOT):
    write(root, "assets/conveyance/models/block/tree_log.json", {"parent": "minecraft:block/cube_column", "textures": {
        "side": "minecraft:block/oak_log", "end": "minecraft:block/oak_log_top"}})
    variants = {"tile=9": {"model": "conveyance:block/oil_deposit"}}
    for tile in range(9):
        ox, oz = tile % 3 * 16, tile // 3 * 16
        elements = [box([0, 0, 0], [16, 16, 16], "stone")]
        # Overlapping horizontal strips give the well a rounded 3x3 outline.
        for low, high in (([6, 16.01, 12], [42, 16.02, 36]), ([12, 16.01, 6], [36, 16.02, 42]),
                          ([9, 16.01, 9], [39, 16.02, 39])):
            a = [max(low[0], ox) - ox, low[1], max(low[2], oz) - oz]
            b = [min(high[0], ox + 16) - ox, high[1], min(high[2], oz + 16) - oz]
            if a[0] < b[0] and a[2] < b[2]: elements.append(box(a, b, "oil"))
        name = f"oil_deposit_tile_{tile}"
        write(root, f"assets/conveyance/models/block/{name}.json", {"parent": "minecraft:block/block", "textures": {
            "stone": "minecraft:block/stone", "oil": "minecraft:block/black_concrete", "particle": "minecraft:block/black_concrete"}, "elements": elements})
        variants[f"tile={tile}"] = {"model": "conveyance:block/" + name}
    write(root, "assets/conveyance/blockstates/oil_deposit.json", {"variants": variants})


if __name__ == "__main__":
    generate_all()

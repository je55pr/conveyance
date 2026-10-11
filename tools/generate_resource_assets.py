"""Assets for natural resource deposits without inventory items or processing."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"


def generate_all(root=ROOT):
    assets = {
        "assets/conveyance/models/block/uranium_ore.json": {"parent": "minecraft:block/emerald_ore"},
        "assets/conveyance/blockstates/uranium_ore.json": {"variants": {"": {"model": "conveyance:block/uranium_ore"}}},
        "data/conveyance/loot_tables/blocks/uranium_ore.json": {"type": "minecraft:block", "pools": []}}
    for path, data in assets.items():
        target = root / path; target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    for language in ("en_us", "en_gb"):
        target = root / f"assets/conveyance/lang/{language}.json"
        data = json.loads(target.read_text(encoding="utf-8"))
        data.update({"block.conveyance.uranium_ore": "Uranium Ore", "map.conveyance.uranium": "Uranium"})
        target.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__": generate_all()

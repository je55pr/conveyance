"""Generate the prototype factory's block models, recipes and translations."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"


def write(path, data):
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def box(start, end, texture):
    return {"from": start, "to": end,
            "faces": {side: {"texture": "#" + texture}
                      for side in ("down", "up", "north", "south", "west", "east")}}


write("assets/conveyance/models/block/miner_casing.json", {
    "parent": "minecraft:block/cube_all", "textures": {"all": "minecraft:block/iron_block"}})
write("assets/conveyance/models/block/miner_controller.json", {
    "parent": "minecraft:block/orientable",
    "textures": {"top": "minecraft:block/iron_block", "side": "minecraft:block/smooth_stone",
                 "front": "minecraft:block/furnace_front"}})
parts = [{"when": {"part": "|".join(str(p) for p in range(1, 48))},
          "apply": {"model": "conveyance:block/miner_casing"}}]
for facing, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
    parts.append({"when": {"part": "0", "facing": facing},
                  "apply": {"model": "conveyance:block/miner_controller", "y": rotation}})
write("assets/conveyance/blockstates/miner.json", {"multipart": parts})
write("assets/conveyance/blockstates/miner_part.json", {"variants": {"": {"model": "conveyance:block/miner_casing"}}})
write("assets/conveyance/models/item/miner.json", {"parent": "conveyance:block/miner_controller"})

write("assets/conveyance/models/block/conveyor.json", {
    "textures": {"body": "minecraft:block/black_concrete", "arrow": "minecraft:block/yellow_concrete",
                 "particle": "minecraft:block/black_concrete"},
    "elements": [box([0, 0, 0], [16, 2, 16], "body"),
                 box([7, 2, 5], [9, 2.3, 13], "arrow"),
                 box([5, 2, 3], [11, 2.3, 6], "arrow"),
                 box([7, 2, 1], [9, 2.3, 3], "arrow")]})
write("assets/conveyance/models/block/inserter.json", {
    "textures": {"body": "minecraft:block/iron_block", "arm": "minecraft:block/yellow_concrete",
                 "particle": "minecraft:block/iron_block"},
    "elements": [box([3, 0, 3], [13, 4, 13], "body"),
                 box([6, 4, 6], [10, 10, 10], "arm"),
                 box([6, 10, 1], [10, 12, 15], "arm"),
                 box([4, 10, 0], [12, 12, 3], "body")]})
for name in ("conveyor", "inserter"):
    write(f"assets/conveyance/blockstates/{name}.json", {"variants": {
        f"facing={facing}": {"model": f"conveyance:block/{name}", "y": rotation}
        for facing, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    write(f"assets/conveyance/models/item/{name}.json", {"parent": f"conveyance:block/{name}"})

pole_textures = {"body": "minecraft:block/iron_block", "insulator": "minecraft:block/black_concrete",
                 "particle": "minecraft:block/iron_block"}
for part in range(4):
    elements = [box([6, 0, 6], [10, 16, 10], "body")]
    if part == 0:
        elements.append(box([2, 0, 2], [14, 4, 14], "body"))
    if part == 3:
        elements.extend([box([0, 10, 5], [16, 13, 11], "body"),
                         box([0, 13, 5], [3, 16, 11], "insulator"),
                         box([13, 13, 5], [16, 16, 11], "insulator")])
    write(f"assets/conveyance/models/block/power_pole_{part}.json", {
        "textures": pole_textures, "elements": elements})
for name in ("power_pole", "power_pole_part"):
    write(f"assets/conveyance/blockstates/{name}.json", {"variants": {
        f"part={part}": {"model": f"conveyance:block/power_pole_{part}"} for part in range(4)}})
write("assets/conveyance/models/item/power_pole.json", {
    "textures": pole_textures,
    "elements": [box([6, 0, 6], [10, 16, 10], "body"), box([2, 0, 2], [14, 2, 14], "body"),
                 box([0, 12, 5], [16, 14, 11], "body"), box([0, 14, 5], [3, 16, 11], "insulator"),
                 box([13, 14, 5], [16, 16, 11], "insulator")]})
for lit in (False, True):
    name = "generator_on" if lit else "generator"
    write(f"assets/conveyance/models/block/{name}.json", {
        "parent": "minecraft:block/orientable",
        "textures": {"top": "minecraft:block/iron_block", "side": "minecraft:block/copper_block",
                     "front": "minecraft:block/furnace_front_on" if lit else "minecraft:block/furnace_front"}})
write("assets/conveyance/blockstates/generator.json", {"variants": {
    "lit=false": {"model": "conveyance:block/generator"}, "lit=true": {"model": "conveyance:block/generator_on"}}})
write("assets/conveyance/models/item/generator.json", {"parent": "conveyance:block/generator"})

for name, casing in (("burner_miner", "polished_blackstone"), ("assembler", "copper_block"), ("industrial_furnace", "bricks")):
    write(f"assets/conveyance/models/block/{name}_casing.json", {
        "parent": "minecraft:block/cube_all", "textures": {"all": f"minecraft:block/{casing}"}})
    for lit in (False, True):
        model = name + ("_on" if lit else "")
        front = "blast_furnace_front" if name == "assembler" else "furnace_front"
        if lit and name != "assembler":
            front += "_on"
        write(f"assets/conveyance/models/block/{model}.json", {
            "parent": "minecraft:block/orientable", "textures": {
                "side": f"minecraft:block/{casing}", "top": "minecraft:block/iron_block", "front": f"minecraft:block/{front}"}})
    models = [{"when": {"part": "|".join(str(part) for part in range(1, 18))}, "apply": {"model": f"conveyance:block/{name}_casing"}}]
    for facing, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for lit in (False, True):
            models.append({"when": {"part": "0", "facing": facing, "lit": str(lit).lower()},
                           "apply": {"model": f"conveyance:block/{name}" + ("_on" if lit else ""), "y": rotation}})
    write(f"assets/conveyance/blockstates/{name}.json", {"multipart": models})
    write(f"assets/conveyance/blockstates/{name}_part.json", {"variants": {"": {"model": f"conveyance:block/{name}_casing"}}})
    write(f"assets/conveyance/models/item/{name}.json", {"parent": f"conveyance:block/{name}"})
    for block in (name, name + "_part"):
        write(f"data/conveyance/loot_tables/blocks/{block}.json", {"type": "minecraft:block", "pools": []})

write("data/conveyance/loot_tables/blocks/miner.json", {"type": "minecraft:block", "pools": []})
write("data/conveyance/loot_tables/blocks/miner_part.json", {"type": "minecraft:block", "pools": []})
write("data/conveyance/loot_tables/blocks/conveyor.json", {"type": "minecraft:block", "pools": []})
for name in ("power_pole", "power_pole_part", "generator"):
    write(f"data/conveyance/loot_tables/blocks/{name}.json", {"type": "minecraft:block", "pools": []})
for name in ("inserter",):
    write(f"data/conveyance/loot_tables/blocks/{name}.json", {
        "type": "minecraft:block", "pools": [{"rolls": 1,
            "entries": [{"type": "minecraft:item", "name": f"conveyance:{name}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

recipes = {
    "conveyor": (["III", "CCC"], {"I": "iron_ingot", "C": "copper_ingot"}, 8),
    "inserter": ([" I ", "ICI", " C "], {"I": "iron_ingot", "C": "copper_ingot"}, 1),
    "miner": (["III", "ICI", "III"], {"I": "iron_block", "C": "copper_block"}, 1),
    "power_pole": ([" C ", " I ", " I "], {"I": "iron_ingot", "C": "copper_ingot"}, 1),
    "generator": (["III", "IFI", "ICI"], {"I": "iron_ingot", "F": "furnace", "C": "copper_ingot"}, 1),
    "burner_miner": (["III", "IFI", "CCC"], {"I": "iron_ingot", "F": "furnace", "C": "cobblestone"}, 1),
    "assembler": (["III", "ICI", "III"], {"I": "iron_ingot", "C": "copper_ingot"}, 1),
    "industrial_furnace": (["CCC", "CIC", "CCC"], {"C": "cobblestone", "I": "iron_ingot"}, 1)
}
for name, (pattern, ingredients, count) in recipes.items():
    write(f"data/conveyance/recipes/{name}.json", {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
        "key": {key: {"item": "minecraft:" + item} for key, item in ingredients.items()},
        "result": {"item": "conveyance:" + name, "count": count}})

for language in ("en_us", "en_gb"):
    file = f"assets/conveyance/lang/{language}.json"
    data = json.loads((ROOT / file).read_text(encoding="utf-8"))
    data.update({"itemGroup.conveyance.factory": "Conveyance Factory",
                 "block.conveyance.miner": "Miner",
                 "block.conveyance.conveyor": "Conveyor Belt",
                 "block.conveyance.inserter": "Inserter",
                 "block.conveyance.power_pole": "Power Pole",
                 "block.conveyance.generator": "Coal Generator",
                 "block.conveyance.burner_miner": "Burner Miner",
                 "block.conveyance.assembler": "Assembling Machine",
                 "block.conveyance.industrial_furnace": "Industrial Furnace",
                 "message.conveyance.machine_space": "Machine needs clear %sx%s space, %s blocks high, on solid level ground.",
                 "tooltip.conveyance.burner_miner": "2x2x2 coal miner. One fuel slot; outputs to a belt in front of its controller.",
                 "tooltip.conveyance.assembler": "3x3x2 assembler. 60 kW. Select a recipe, supply ingredients, extract the output.",
                 "tooltip.conveyance.industrial_furnace": "2x2x3 coal furnace. One input, one fuel and one output slot.",
                 "menu.conveyance.inputs": "Inputs",
                 "menu.conveyance.output": "Output",
                 "menu.conveyance.requires": "Requires %s x %s per batch",
                 "message.conveyance.pole_space": "Power pole needs 4 blocks of clear height on solid ground.",
                 "tooltip.conveyance.power_pole": "Places a 4-block-tall pole. Link range: %s blocks. Machine coverage: %s blocks.",
                 "message.conveyance.no_power_coverage": "Outside power pole coverage.",
                 "message.conveyance.power_idle": "Supply: %s kW | No connected machines | %s poles",
                 "message.conveyance.power_status": "Supply: %s / %s kW | %s%% power | %s poles, %s machines",
                 "message.conveyance.miner_space": "Miner needs clear 4x4 space, 3 blocks high, on solid level ground.",
                 "tooltip.conveyance.miner": "Places a 4x4x3 miner. Put an outward-facing belt directly in front of its controller."})
    write(file, data)

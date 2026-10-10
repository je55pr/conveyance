"""Generate the prototype factory's block models, recipes and translations."""
import json
import math
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

for material in ("grass_block", "dirt", "coarse_dirt", "sand", "stone", "andesite", "granite", "bedrock", "iron_ore", "copper_ore", "coal_ore"):
    name = "terrain_" + material
    write(f"assets/conveyance/models/block/{name}.json", {"parent": f"minecraft:block/{material}"})
    write(f"assets/conveyance/blockstates/{name}.json", {"variants": {"": {"model": f"conveyance:block/{name}"}}})
    write(f"data/conveyance/loot_tables/blocks/{name}.json", {"type": "minecraft:block", "pools": []})


for name, body, frame in (("wood_crate", "acacia_planks", "dark_oak_planks"),
                          ("iron_crate", "iron_block", "polished_deepslate")):
    elements = [box([1, 1, 1], [15, 15, 15], "body")]
    for x in (0, 13):
        for z in (0, 13):
            elements.append(box([x, 0, z], [x + 3, 16, z + 3], "frame"))
    for y in (0, 13):
        elements.extend([box([0, y, 0], [16, y + 3, 3], "frame"),
                         box([0, y, 13], [16, y + 3, 16], "frame"),
                         box([0, y, 3], [3, y + 3, 13], "frame"),
                         box([13, y, 3], [16, y + 3, 13], "frame")])
    write(f"assets/conveyance/models/block/{name}.json", {
        "parent": "minecraft:block/block", "textures": {"body": f"minecraft:block/{body}",
        "frame": f"minecraft:block/{frame}", "particle": f"minecraft:block/{body}"}, "elements": elements})
    write(f"assets/conveyance/blockstates/{name}.json", {"variants": {"": {"model": f"conveyance:block/{name}"}}})
    write(f"assets/conveyance/models/item/{name}.json", {"parent": f"conveyance:block/{name}"})
    write(f"data/conveyance/loot_tables/blocks/{name}.json", {"type": "minecraft:block", "pools": []})

write("assets/conveyance/models/block/tree_log.json", {
    "parent": "minecraft:block/cube_column", "textures": {
        "side": "minecraft:block/acacia_log", "end": "minecraft:block/acacia_log_top"}})
leaf = box([0, 0, 0], [16, 16, 16], "leaf")
for side, face in leaf["faces"].items():
    face.update({"tintindex": 0, "cullface": side})
write("assets/conveyance/models/block/tree_leaves.json", {
    "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
    "textures": {"leaf": "minecraft:block/acacia_leaves", "particle": "minecraft:block/acacia_leaves"},
    "elements": [leaf]})
for name in ("tree_log", "tree_leaves"):
    write(f"assets/conveyance/blockstates/{name}.json", {"variants": {"": {"model": f"conveyance:block/{name}"}}})
write("data/conveyance/loot_tables/blocks/tree_log.json", {"type": "minecraft:block", "pools": [{
    "rolls": 1, "entries": [{"type": "minecraft:item", "name": "conveyance:wood"}],
    "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
write("data/conveyance/loot_tables/blocks/tree_leaves.json", {"type": "minecraft:block", "pools": []})
write("data/minecraft/tags/blocks/logs.json", {"values": ["conveyance:tree_log"]})
write("data/minecraft/tags/blocks/leaves.json", {"values": ["conveyance:tree_leaves"]})

# Crates replace vanilla chest items, including mobile chest variants. Keep
# generated policy tags consistent with the baseline generator's source list.
old_chests = ["chest", "chest_minecart", "bamboo_chest_raft"] + [
    f"{wood}_chest_boat" for wood in ("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry")]
for kind in ("items", "blocks"):
    path = f"data/conveyance/tags/{kind}/disabled.json"
    policy = json.loads((ROOT / path).read_text(encoding="utf-8"))
    extras = old_chests if kind == "items" else ["chest"]
    policy["values"] = sorted(set(policy["values"]) | {"minecraft:" + name for name in extras})
    write(path, policy)

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

BELT_TEXTURES = {"body": "minecraft:block/gray_concrete", "frame": "minecraft:block/light_gray_concrete",
                 "divider": "minecraft:block/black_concrete", "arrow": "minecraft:block/light_gray_concrete",
                 "particle": "minecraft:block/gray_concrete"}

def belt_model(elements, textures=None):
    return {"parent": "minecraft:block/block", "ambientocclusion": False,
            "textures": textures or BELT_TEXTURES, "elements": elements}

def belt_arrow(y):
    return [box([7.5, y, 5], [8.5, y + 0.12, 12], "arrow"),
            box([6.5, y, 3], [9.5, y + 0.12, 5], "arrow"),
            box([7.5, y, 1], [8.5, y + 0.12, 3], "arrow")]

write("assets/conveyance/models/block/conveyor.json", belt_model([
    box([2, 0, 0], [14, 1, 16], "body"),
    box([2, 1, 0], [3, 1.2, 16], "frame"),
    box([13, 1, 0], [14, 1.2, 16], "frame"),
    box([7.75, 1, 0], [8.25, 1.05, 16], "divider")] + belt_arrow(1.08)))

# A quarter annulus joins the west entrance to the north exit. Its inner and
# outer lanes have radii 5 and 11, matching the conveyor's item paths.
corner = []
for z in range(16):
    run_start, run_material = 0, None
    for x in range(17):
        radius = math.hypot(x + 0.5, z + 0.5)
        material = None
        if x < 16 and 2 <= radius <= 14:
            material = "frame" if radius < 3 or radius > 13 else "divider" if abs(radius - 8) < 0.45 else "body"
        if material != run_material:
            if run_material is not None:
                top = 1.2 if run_material == "frame" else 1.05 if run_material == "divider" else 1
                corner.append(box([run_start, 0, z], [x, top, z + 1], run_material))
            run_start, run_material = x, material
corner += [
    box([7.5, 1.23, 1], [8.5, 1.35, 4], "arrow"),
    box([6.5, 1.23, 2], [9.5, 1.35, 3], "arrow")]
write("assets/conveyance/models/block/conveyor_left.json", belt_model(corner))
mirrored = json.loads(json.dumps(corner))
for element in mirrored:
    element["from"][0], element["to"][0] = 16 - element["to"][0], 16 - element["from"][0]
write("assets/conveyance/models/block/conveyor_right.json", belt_model(mirrored))
write("assets/conveyance/blockstates/conveyor.json", {"variants": {
    f"facing={facing},turn={turn}": {"model": f"conveyance:block/conveyor{suffix}", "y": rotation}
    for facing, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270))
    for turn, suffix in (("straight", ""), ("left", "_left"), ("right", "_right"))}})
write("assets/conveyance/models/item/conveyor.json", {"parent": "conveyance:block/conveyor"})

def incline(element):
    element["rotation"] = {"origin": [8, 8, 8], "axis": "x", "angle": 45, "rescale": False}
    return element

for mode in ("up", "down", "stuck"):
    body = [box([2, 7.5, -3.3137], [14, 8.5, 19.3137], "body"),
            box([2, 8.5, -3.3137], [3, 8.7, 19.3137], "frame"),
            box([13, 8.5, -3.3137], [14, 8.7, 19.3137], "frame"),
            box([7.75, 8.5, -3.3137], [8.25, 8.6, 19.3137], "divider")]
    textures = dict(BELT_TEXTURES)
    if mode == "stuck":
        textures["arrow"] = "minecraft:block/red_concrete"
        arrows = [box([6, 8.8, 7.5], [10, 8.92, 8.5], "arrow"),
                  box([7.5, 8.8, 6], [8.5, 8.92, 10], "arrow")]
    else:
        arrows = belt_arrow(8.8)
        if mode == "down":
            for arrow in arrows:
                arrow["from"][2], arrow["to"][2] = 16 - arrow["to"][2], 16 - arrow["from"][2]
    write(f"assets/conveyance/models/block/slope_conveyor_{mode}.json",
          belt_model([incline(element) for element in body + arrows], textures))
write("assets/conveyance/blockstates/slope_conveyor.json", {"multipart": [
    {"when": {"facing": facing, "flow": flow}, "apply": {"model": f"conveyance:block/slope_conveyor_{mode}", "y": rotation}}
    for facing, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270))
    for flow, mode in (("up", "up"), ("down", "down"), ("unset|stuck_up|stuck_down", "stuck"))]})
write("assets/conveyance/models/item/slope_conveyor.json", {"parent": "conveyance:block/slope_conveyor_up"})
write("data/conveyance/loot_tables/blocks/slope_conveyor.json", {"type": "minecraft:block", "pools": []})

write("assets/conveyance/models/block/inserter.json", {
    "textures": {"body": "minecraft:block/iron_block", "arm": "minecraft:block/yellow_concrete",
                 "particle": "minecraft:block/iron_block"},
    "elements": [box([3, 0, 3], [13, 4, 13], "body"),
                 box([6, 4, 6], [10, 10, 10], "arm"),
                 box([6, 10, 1], [10, 12, 15], "arm"),
                 box([4, 10, 0], [12, 12, 3], "body")]})
write("assets/conveyance/models/block/burner_inserter.json", {
    "textures": {"body": "minecraft:block/copper_block", "arm": "minecraft:block/black_concrete",
                 "particle": "minecraft:block/copper_block"},
    "elements": [box([3, 0, 3], [13, 4, 13], "body"), box([6, 4, 6], [10, 10, 10], "arm"),
                 box([6, 10, 1], [10, 12, 15], "arm"), box([4, 10, 0], [12, 12, 3], "body")]})
write("assets/conveyance/models/block/long_inserter.json", {
    "textures": {"body": "minecraft:block/iron_block", "arm": "minecraft:block/red_concrete", "particle": "minecraft:block/iron_block"},
    "elements": [box([3, 0, 3], [13, 4, 13], "body"), box([6, 4, 6], [10, 10, 10], "arm"),
                 box([6, 10, -8], [10, 12, 24], "arm"), box([4, 10, -9], [12, 12, -6], "body")]})

write("assets/conveyance/models/block/junction.json", {
    "textures": {"body": "minecraft:block/gray_concrete", "hub": "minecraft:block/light_gray_concrete",
                 "particle": "minecraft:block/gray_concrete"},
    "elements": [box([0, 0, 0], [16, 8, 16], "body"), box([5, 8, 5], [11, 8.1, 11], "hub")]})
for port, color in (("input", "light_gray_concrete"), ("output", "light_gray_concrete")):
    if port == "input":
        elements = [box([7, 8, 0], [9, 8.2, 4], "arrow"), box([5, 8, 3], [11, 8.2, 5], "arrow"),
                    box([7, 8, 5], [9, 8.2, 7], "arrow")]
    else:
        elements = [box([7, 8, 2], [9, 8.2, 7], "arrow"), box([5, 8, 2], [11, 8.2, 4], "arrow"),
                    box([7, 8, 0], [9, 8.2, 2], "arrow")]
    write(f"assets/conveyance/models/block/junction_{port}.json", {
        "textures": {"arrow": f"minecraft:block/{color}", "particle": f"minecraft:block/{color}"}, "elements": elements})
junction_parts = [{"apply": {"model": "conveyance:block/junction"}}]
for facing, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
    for port in ("input", "output"):
        junction_parts.append({"when": {facing: port}, "apply": {"model": f"conveyance:block/junction_{port}", "y": rotation}})
write("assets/conveyance/blockstates/junction.json", {"multipart": junction_parts})
write("assets/conveyance/models/item/junction.json", {"parent": "conveyance:block/junction"})
write("data/conveyance/loot_tables/blocks/junction.json", {"type": "minecraft:block", "pools": []})
for name in ("inserter", "burner_inserter", "long_inserter"):
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

for lit in (False, True):
    name = "radar_on" if lit else "radar"
    dish = box([1, 10, 3], [15, 12, 13], "dish")
    dish["rotation"] = {"origin": [8, 10, 8], "axis": "x", "angle": 22.5}
    write(f"assets/conveyance/models/block/{name}.json", {
        "parent": "minecraft:block/block", "textures": {
            "body": "minecraft:block/polished_deepslate", "dish": "minecraft:block/iron_block",
            "light": "minecraft:block/lime_concrete" if lit else "minecraft:block/red_concrete",
            "particle": "minecraft:block/iron_block"},
        "elements": [box([2, 0, 2], [14, 4, 14], "body"), box([6, 4, 6], [10, 12, 10], "dish"),
                     dish, box([7, 12, 7], [9, 16, 9], "dish"), box([6, 2, 1.9], [10, 3, 2], "light")]})
write("assets/conveyance/blockstates/radar.json", {"variants": {
    "lit=false": {"model": "conveyance:block/radar"}, "lit=true": {"model": "conveyance:block/radar_on"}}})
write("assets/conveyance/models/item/radar.json", {"parent": "conveyance:block/radar_on"})
write("data/conveyance/loot_tables/blocks/radar.json", {"type": "minecraft:block", "pools": []})
write("assets/conveyance/models/block/radar_base.json", {
    "parent": "minecraft:block/block", "textures": {"body": "minecraft:block/polished_deepslate",
    "particle": "minecraft:block/iron_block"}, "elements": [box([0, 0, 0], [16, 4, 16], "body")]})
for lit in (False, True):
    dish = box([-16, 10, 1], [32, 12, 15], "dish")
    dish["rotation"] = {"origin": [8, 10, 8], "axis": "x", "angle": 22.5}
    write(f"assets/conveyance/models/block/radar_head{'_on' if lit else ''}.json", {
        "parent": "minecraft:block/block", "textures": {
            "body": "minecraft:block/polished_deepslate", "dish": "minecraft:block/iron_block",
            "light": "minecraft:block/lime_concrete" if lit else "minecraft:block/red_concrete",
            "particle": "minecraft:block/iron_block"},
        "elements": [box([0, 0, 0], [16, 4, 16], "body"), box([6, 4, 6], [10, 12, 10], "dish"),
                     dish, box([7, 12, 7], [9, 16, 9], "dish"), box([6, 2, -0.1], [10, 3, 0], "light")]})

def radar_model(part, lit):
    return f"conveyance:block/radar_head{'_on' if lit else ''}" if part == 4 else "conveyance:block/radar_base"

write("assets/conveyance/blockstates/radar.json", {"variants": {
    f"part={part},facing={facing},lit={str(lit).lower()},multiblock={str(multi).lower()}": {
        "model": radar_model(part, lit) if multi else f"conveyance:block/radar{'_on' if lit else ''}", "y": rotation}
    for part in range(9) for facing, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270))
    for lit in (False, True) for multi in (False, True)}})
write("assets/conveyance/blockstates/radar_part.json", {"variants": {
    f"part={part},facing={facing},lit={str(lit).lower()}": {"model": radar_model(part, lit), "y": rotation}
    for part in range(9) for facing, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270))
    for lit in (False, True)}})
write("data/conveyance/loot_tables/blocks/radar_part.json", {"type": "minecraft:block", "pools": []})

panel = {"parent": "minecraft:block/block", "textures": {
    "frame": "minecraft:block/iron_block", "cell": "minecraft:block/blue_concrete", "wire": "minecraft:block/light_gray_concrete", "particle": "minecraft:block/blue_concrete"},
    "elements": [box([0, 0, 0], [16, 3, 16], "frame"), box([1, 3, 1], [15, 4, 15], "cell")] +
                [box([1, 4, z], [15, 4.1, z + 0.3], "wire") for z in (4, 8, 12)] +
                [box([8, 4, 1], [8.3, 4.1, 15], "wire")]}
write("assets/conveyance/models/block/solar_panel_casing.json", panel)
for lit in (False, True):
    root_panel = json.loads(json.dumps(panel))
    root_panel["textures"]["status"] = "minecraft:block/lime_concrete" if lit else "minecraft:block/black_concrete"
    root_panel["elements"].append(box([2, 3, 0], [5, 4, 1], "status"))
    write("assets/conveyance/models/block/solar_panel" + ("_on" if lit else "") + ".json", root_panel)
parts = [{"when": {"part": "|".join(str(p) for p in range(1, 18))}, "apply": {"model": "conveyance:block/solar_panel_casing"}}]
for lit in (False, True):
    parts.append({"when": {"part": "0", "lit": str(lit).lower()}, "apply": {"model": "conveyance:block/solar_panel" + ("_on" if lit else "")}})
write("assets/conveyance/blockstates/solar_panel.json", {"multipart": parts})
write("assets/conveyance/blockstates/solar_panel_part.json", {"variants": {"": {"model": "conveyance:block/solar_panel_casing"}}})
write("assets/conveyance/models/item/solar_panel.json", {"parent": "conveyance:block/solar_panel_on"})
for name in ("solar_panel", "solar_panel_part"):
    write(f"data/conveyance/loot_tables/blocks/{name}.json", {"type": "minecraft:block", "pools": []})

for name, casing in (("burner_miner", "polished_blackstone"), ("assembler", "copper_block"), ("industrial_furnace", "bricks"), ("research_lab", "light_blue_concrete")):
    write(f"assets/conveyance/models/block/{name}_casing.json", {
        "parent": "minecraft:block/cube_all", "textures": {"all": f"minecraft:block/{casing}"}})
    for lit in (False, True):
        model = name + ("_on" if lit else "")
        front = "redstone_lamp" if name == "research_lab" else "blast_furnace_front" if name == "assembler" else "furnace_front"
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

def item_model(name, textures, elements, flat=True):
    write(f"assets/conveyance/models/item/{name}.json", {
        "parent": "minecraft:block/block", "gui_light": "front",
        "textures": textures | {"particle": next(iter(textures.values()))}, "elements": elements,
        "display": {"gui": {"rotation": [90, 0, 0] if flat else [15, -30, 0], "scale": [1.05, 1.05, 1.05]},
                    "ground": {"translation": [0, 2, 0], "scale": [0.35, 0.35, 0.35]}}})

item_model("wood", {"wood": "minecraft:block/acacia_log", "cut": "minecraft:block/acacia_log_top"}, [
    box([2, 4, 3], [6, 9, 14], "wood"), box([7, 4, 2], [11, 9, 13], "wood"),
    box([5, 9, 4], [9, 13, 15], "wood"), box([2, 4, 2.9], [6, 9, 3], "cut"),
    box([7, 4, 1.9], [11, 9, 2], "cut"), box([5, 9, 3.9], [9, 13, 4], "cut")], flat=False)

gear = [box([4, 7, 4], [7, 9, 12], "metal"), box([9, 7, 4], [12, 9, 12], "metal"),
        box([7, 7, 4], [9, 9, 7], "metal"), box([7, 7, 9], [9, 9, 12], "metal")]
teeth = [box([6, 7, 1], [10, 9, 4], "metal"), box([6, 7, 12], [10, 9, 15], "metal"),
         box([1, 7, 6], [4, 9, 10], "metal"), box([12, 7, 6], [15, 9, 10], "metal")]
gear.extend(teeth)
for element in teeth:
    gear.append(element | {"rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45}})
item_model("gear", {"metal": "minecraft:block/iron_block"}, gear)
item_model("copper_wire", {"copper": "minecraft:block/copper_block"}, [
    box([2, 7, 2], [5, 9, 14], "copper"), box([11, 7, 2], [14, 9, 14], "copper"),
    box([5, 7, 2], [11, 9, 5], "copper"), box([5, 7, 11], [11, 9, 14], "copper"),
    box([1, 7, 10], [3, 9, 15], "copper")])
item_model("electronic_circuit", {"board": "minecraft:block/green_concrete", "trace": "minecraft:block/yellow_concrete", "chip": "minecraft:block/black_concrete"}, [
    box([2, 7, 2], [14, 8, 14], "board"), box([6, 8, 5], [10, 9, 11], "chip"),
    box([3, 8, 3], [4, 8.3, 13], "trace"), box([12, 8, 3], [13, 8.3, 13], "trace"),
    box([4, 8, 6], [6, 8.3, 7], "trace"), box([10, 8, 9], [12, 8.3, 10], "trace")])
item_model("red_science_pack", {"body": "minecraft:block/red_concrete", "label": "minecraft:block/white_concrete", "cap": "minecraft:block/black_concrete"}, [
    box([3, 2, 3], [13, 11, 13], "body"), box([4, 11, 4], [12, 14, 12], "cap"),
    box([5, 4, 2.9], [11, 9, 3], "label"), box([7, 6, 2.8], [9, 8, 2.9], "body")], flat=False)
green_pack = json.loads((ROOT / "assets/conveyance/models/item/red_science_pack.json").read_text(encoding="utf-8"))
green_pack["textures"]["body"] = "minecraft:block/green_concrete"
write("assets/conveyance/models/item/green_science_pack.json", green_pack)
item_model("steel_ingot", {"steel": "minecraft:block/gray_concrete", "top": "minecraft:block/iron_block"}, [
    box([2, 3, 4], [14, 6, 12], "steel"), box([3, 6, 5], [13, 8, 11], "top")], flat=False)

write("data/conveyance/loot_tables/blocks/miner.json", {"type": "minecraft:block", "pools": []})
write("data/conveyance/loot_tables/blocks/miner_part.json", {"type": "minecraft:block", "pools": []})
write("data/conveyance/loot_tables/blocks/conveyor.json", {"type": "minecraft:block", "pools": []})
write("data/conveyance/loot_tables/blocks/burner_inserter.json", {"type": "minecraft:block", "pools": []})
for name in ("power_pole", "power_pole_part", "generator"):
    write(f"data/conveyance/loot_tables/blocks/{name}.json", {"type": "minecraft:block", "pools": []})
for name in ("inserter", "long_inserter"):
    write(f"data/conveyance/loot_tables/blocks/{name}.json", {
        "type": "minecraft:block", "pools": [{"rolls": 1,
            "entries": [{"type": "minecraft:item", "name": f"conveyance:{name}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

recipes = {
    "conveyor": ("logistics", {"iron_ingot": 2, "conveyance:gear": 1}, 8),
    "slope_conveyor": ("logistics", {"conveyance:conveyor": 2, "conveyance:gear": 1}, 1),
    "inserter": ("logistics", {"iron_ingot": 3, "conveyance:gear": 1, "conveyance:electronic_circuit": 1}, 1),
    "burner_inserter": ("logistics", {"iron_ingot": 1, "conveyance:gear": 1}, 1),
    "long_inserter": ("logistics", {"iron_ingot": 2, "conveyance:gear": 1, "conveyance:inserter": 1}, 1),
    "miner": ("production", {"iron_ingot": 10, "conveyance:gear": 5, "conveyance:electronic_circuit": 3}, 1),
    "power_pole": ("power", {"iron_ingot": 2, "conveyance:copper_wire": 1}, 1),
    "generator": ("power", {"iron_ingot": 6, "conveyance:gear": 1, "conveyance:copper_wire": 1}, 1),
    "burner_miner": ("production", {"iron_ingot": 3, "conveyance:gear": 3, "conveyance:industrial_furnace": 1}, 1),
    "assembler": ("production", {"iron_ingot": 7, "conveyance:electronic_circuit": 1, "conveyance:gear": 1}, 1),
    "industrial_furnace": ("production", {"cobblestone": 5}, 1),
    "junction": ("logistics", {"iron_ingot": 3, "copper_ingot": 1}, 1),
    "wood_crate": ("logistics", {"conveyance:wood": 8}, 1),
    "iron_crate": ("logistics", {"iron_ingot": 8}, 1),
    "iron_pickaxe": ("production", {"iron_ingot": 5}, 1),
    "gear": ("intermediates", {"iron_ingot": 2}, 1),
    "copper_wire": ("intermediates", {"copper_ingot": 1}, 2),
    "electronic_circuit": ("intermediates", {"conveyance:copper_wire": 3, "iron_ingot": 1}, 1),
    "red_science_pack": ("science", {"conveyance:gear": 1, "copper_ingot": 1}, 1),
    "green_science_pack": ("science", {"conveyance:conveyor": 1, "conveyance:inserter": 1}, 1),
    "solar_panel": ("power", {"conveyance:steel_ingot": 4, "conveyance:electronic_circuit": 4, "conveyance:copper_wire": 1}, 1),
    "research_lab": ("science", {"iron_ingot": 4, "conveyance:electronic_circuit": 2, "conveyance:gear": 3}, 1),
    "radar": ("production", {"iron_ingot": 6, "conveyance:electronic_circuit": 2, "conveyance:gear": 1}, 1)
}
for name, (tab, ingredients, count) in recipes.items():
    write(f"data/conveyance/recipes/{name}.json", {
        "type": "conveyance:factory_crafting", "tab": tab,
        "ingredients": [{"item": item if ":" in item else "minecraft:" + item, "count": amount} for item, amount in ingredients.items()],
        "result": {"item": "minecraft:iron_pickaxe" if name == "iron_pickaxe" else "conveyance:" + name, "count": count}})
for removed in ("iron_block", "copper_block"):
    (ROOT / f"data/conveyance/recipes/{removed}.json").unlink(missing_ok=True)
(ROOT / "data/conveyance/recipes/storage_chest.json").unlink(missing_ok=True)
write("data/conveyance/recipes/steel_ingot.json", {
    "type": "conveyance:bulk_smelting", "ingredient": {"item": "minecraft:iron_ingot"}, "input_count": 5,
    "result": {"item": "conveyance:steel_ingot", "count": 1}, "cookingtime": 320})
write("data/conveyance/recipes/stone_bricks.json", {
    "type": "conveyance:bulk_smelting", "ingredient": {"item": "minecraft:cobblestone"}, "input_count": 2,
    "result": {"item": "minecraft:stone_bricks", "count": 1}, "cookingtime": 64})

for language in ("en_us", "en_gb"):
    write(f"assets/minecraft/lang/{language}.json", {"block.minecraft.cobblestone": "Stone"})
    file = f"assets/conveyance/lang/{language}.json"
    data = json.loads((ROOT / file).read_text(encoding="utf-8"))
    data.pop("menu.conveyance.craft_controls", None)
    data.pop("menu.conveyance.recursive", None)
    data.pop("menu.conveyance.category.all", None)
    data.update({"itemGroup.conveyance.factory": "Conveyance Factory",
                 "block.conveyance.wood_crate": "Wooden Crate",
                 "block.conveyance.iron_crate": "Iron Crate",
                 "block.conveyance.tree_log": "Savannah Tree Log",
                 "block.conveyance.tree_leaves": "Savannah Tree Leaves",
                 "item.conveyance.wood": "Wood",
                 "tooltip.conveyance.wood": "Fuel: 15 seconds in burners/generators (half of coal). Used to craft wooden crates.",
                 "tooltip.conveyance.crate": "%s slots, up to %s items. Picking up returns the crate and contents separately.",
                 "block.conveyance.miner": "Miner",
                 "block.conveyance.conveyor": "Conveyor Belt",
                 "block.conveyance.junction": "Conveyor Junction",
                 "tooltip.conveyance.junction": "Half-block junction. Point belts toward it for input, away for output. Shares lanes round robin.",
                 "message.conveyance.junction_status": "Junction: %s inputs, %s outputs | %s stored items",
                 "block.conveyance.inserter": "Inserter",
                 "block.conveyance.burner_inserter": "Burner Inserter",
                 "tooltip.conveyance.burner_inserter": "Fuel-powered arm. Takes coal, charcoal or Wood from its input. Right-click for fuel; sneak-right-click empty-handed to rotate.",
                 "block.conveyance.power_pole": "Power Pole",
                 "block.conveyance.generator": "Burner Generator",
                 "block.conveyance.radar": "Radar",
                 "tooltip.conveyance.radar": "%s kW. Explores %sx%s blocks, one chunk every 2 seconds. Works while loaded. Open the Map tab in E.",
                 "message.conveyance.radar_status": "Radar: %s/%s chunks revealed | %s%% power | %sx%s blocks",
                 "menu.conveyance.map_tab": "Map",
                 "map.conveyance.center": "Here",
                 "map.conveyance.explored": "%s chunks explored",
                 "map.conveyance.controls": "Drag: pan | Scroll: zoom | Cyan: radar range",
                 "map.conveyance.unknown": "Unexplored",
                 "map.conveyance.land": "Land",
                 "map.conveyance.dry": "Dry ground",
                 "map.conveyance.water": "Water",
                 "map.conveyance.shore": "Shore",
                 "map.conveyance.rock": "Rock",
                 "map.conveyance.ridge": "Granite ridge",
                 "map.conveyance.forest": "Forest",
                 "map.conveyance.iron": "Iron",
                 "map.conveyance.copper": "Copper",
                 "map.conveyance.coal": "Coal",
                 "map.conveyance.stone": "Stone",
                 "map.conveyance.factory": "Factory",
                 "block.conveyance.burner_miner": "Burner Miner",
                 "block.conveyance.assembler": "Assembling Machine",
                 "block.conveyance.industrial_furnace": "Stone Furnace",
                 "block.minecraft.cobblestone": "Stone",
                 "block.conveyance.research_lab": "Research Lab",
                 "item.conveyance.gear": "Iron Gear",
                 "item.conveyance.copper_wire": "Copper Wire",
                 "item.conveyance.electronic_circuit": "Electronic Circuit",
                 "item.conveyance.red_science_pack": "Red Science Pack",
                 "tooltip.conveyance.research_lab": "3x3x2 research lab. 60 kW. Select research in E, then supply red science packs.",
                 "message.conveyance.machine_space": "Machine needs clear %sx%s space, %s blocks high, on solid level ground.",
                 "tooltip.conveyance.burner_miner": "2x2x2 burner miner/quarry. Needs fuel, a deposit underneath, and a belt or crate touching the lower furnace face. Coal can feed a facing burner miner.",
                 "tooltip.conveyance.assembler": "3x3x2 assembler. 60 kW. Select a recipe, supply ingredients, extract the output.",
                 "tooltip.conveyance.industrial_furnace": "2x2x3 burner furnace. Accepts coal, charcoal or Wood. One input, one fuel and one output slot.",
                 "menu.conveyance.inputs": "Inputs",
                 "menu.conveyance.output": "Output",
                 "menu.conveyance.requires": "Requires %s x %s per batch",
                 "menu.conveyance.fuel_seconds": "Fuel: %ss",
                 "menu.conveyance.generator_idle": "Idle",
                 "menu.conveyance.generator_output": "%s kW",
                 "menu.conveyance.inventory": "Inventory",
                 "menu.conveyance.handcrafting": "Crafting",
                 "menu.conveyance.inventory_tab": "Inventory / Crafting",
                 "menu.conveyance.research_tab": "Research",
                 "menu.conveyance.tree_fit": "Fit",
                 "menu.conveyance.tree_controls": "Drag to pan | Scroll to zoom",
                 "menu.conveyance.hotbar": "Hotbar",
                 "menu.conveyance.storage_size": "72 slots | 100 per stack",
                 "menu.conveyance.search": "Search recipes",
                 "menu.conveyance.recipe_picker": "Select recipe",
                 "menu.conveyance.no_recipes": "No matching recipes",
                 "menu.conveyance.batch": "%s/batch",
                 "menu.conveyance.have_need": "%s / %s  %s",
                 "menu.conveyance.ingredients": "Ingredients",
                 "menu.conveyance.total_raw": "Total raw resources",
                 "message.conveyance.craft_failed": "Missing ingredients or inventory space.",
                 "message.conveyance.excavation_locked": "Terrain removal requires Excavation research.",
                 "message.conveyance.inventory_full": "Inventory full.",
                 "research.conveyance.automation": "Research: Automation",
                 "research.conveyance.excavation": "Research: Excavation",
                 "research.conveyance.automation_done": "Automation unlocked",
                 "research.conveyance.excavation_done": "Excavation unlocked",
                 "research.conveyance.automation_info": "Unlock assembling, electric mining, inserters and junctions.",
                 "research.conveyance.excavation_info": "Unlock terrain removal. Requires Automation.",
                 "research.conveyance.automation_progress": "Automation: %s/%s",
                 "research.conveyance.excavation_progress": "Excavation: %s/%s",
                 "research.conveyance.science_cost": "%s / %s red science packs researched",
                 "research.conveyance.lab_required": "Supply red science to a powered research lab.",
                 "research.conveyance.automation_short": "Automation",
                 "research.conveyance.excavation_short": "Excavation",
                 "research.conveyance.idle": "Idle",
                 "research.conveyance.start": "Start research",
                 "research.conveyance.completed": "Completed",
                 "research.conveyance.active": "Researching",
                 "research.conveyance.locked": "Locked",
                 "research.conveyance.state.completed": "Completed",
                 "research.conveyance.state.active": "Researching",
                 "research.conveyance.state.available": "Available",
                 "research.conveyance.state.locked": "Locked",
                 "research.conveyance.requires": "Requires: %s",
                 "research.conveyance.unlocks": "Unlocks",
                 "research.conveyance.unlock_item": "Unlocks: %s",
                 "technology.conveyance.basic_industry.name": "Basic Industry",
                 "technology.conveyance.basic_industry.description": "Starter recipes are available immediately.",
                 "technology.conveyance.automation.name": "Automation",
                 "technology.conveyance.automation.short": "Automation",
                 "technology.conveyance.automation.description": "Unlock assembling machines to automate parts and science production.",
                 "technology.conveyance.logistics.name": "Logistics",
                 "technology.conveyance.logistics.short": "Logistics",
                 "technology.conveyance.logistics.description": "Unlock electric inserters and conveyor junctions.",
                 "technology.conveyance.electric_mining.name": "Electric Mining",
                 "technology.conveyance.electric_mining.short": "Mining",
                 "technology.conveyance.electric_mining.description": "Unlock electric miners and iron/copper block recipes.",
                 "technology.conveyance.excavation.name": "Excavation",
                 "technology.conveyance.excavation.short": "Excavation",
                 "technology.conveyance.excavation.description": "Unlock terrain removal after Logistics and Electric Mining.",
                 "message.conveyance.research_reset": "Conveyance research reset. Select a technology and feed red science to a powered lab.",
                 "message.conveyance.pole_space": "Power pole needs 4 blocks of clear height on solid ground.",
                 "tooltip.conveyance.power_pole": "Places a 4-block-tall pole. Link range: %s blocks. Machine coverage: %s blocks.",
                 "message.conveyance.no_power_coverage": "Outside power pole coverage.",
                 "message.conveyance.power_idle": "Supply: %s kW | No connected machines | %s poles",
                 "message.conveyance.power_status": "Supply: %s / %s kW | %s%% power | %s poles, %s machines",
                 "message.conveyance.miner_space": "Miner needs clear 3x3 space, 3 blocks high, on solid level ground.",
                 "tooltip.conveyance.miner": "3x3x3 electric miner/quarry. Outputs ore or stone to a belt, crate or machine in front."})
    data.update({
        "block.conveyance.long_inserter": "Long Inserter",
        "block.conveyance.solar_panel": "Solar Panel",
        "item.conveyance.green_science_pack": "Green Science Pack",
        "item.conveyance.steel_ingot": "Steel Ingot",
        "tooltip.conveyance.long_inserter": "Reaches two blocks on each side. Base: 15 kW, one item per second. Empty-hand right-click to rotate.",
        "tooltip.conveyance.solar_panel": "Places a 3x3 panel, one-quarter block high. Up to 60 kW in daylight with clear sky. Break any part to recover it.",
        "message.conveyance.solar_output": "Solar panel: %s / 60 kW | Needs daylight, open sky and power pole coverage",
        "tooltip.conveyance.research_lab": "3x3x2 lab. Base 60 kW. Red and green input slots; consumes the selected research's pack combination.",
        "tooltip.conveyance.assembler": "3x3x2 assembler. Base 60 kW. Select a recipe, supply ingredients, extract the output.",
        "tooltip.conveyance.wood": "Base fuel: 15 seconds in burners/generators, half of coal. Burner Efficiency extends fuel duration.",
        "tooltip.conveyance.industrial_furnace": "2x2x3 Stone Furnace. Ore/stone: 3.2s, steel: 16s. Uses coal, charcoal or Wood.",
        "menu.conveyance.category.intermediates": "Intermediates",
        "menu.conveyance.category.logistics": "Logistics",
        "menu.conveyance.category.production": "Production",
        "menu.conveyance.category.power": "Power",
        "menu.conveyance.category.science": "Science",
        "tooltip.conveyance.inserter": "Electric arm. Reaches one block on either side, moving two items per second at 15 kW before upgrades.",
        "tooltip.conveyance.steel_ingot": "Smelt five iron ingots into one steel ingot in a Stone Furnace. Base time: 16 seconds.",
        "tooltip.conveyance.green_science_pack": "Research material for labs, supplied alongside red science. Made from one belt and one electric inserter.",
        "tooltip.conveyance.iron_pickaxe": "Construction tool for ordinary placed blocks. Natural terrain remains in place when harvested.",
        "block.conveyance.slope_conveyor": "Slope Conveyor",
        "tooltip.conveyance.conveyor": "Aim into the empty cell ahead of a belt to extend it. New belts follow your facing direction, including corners. Sneak to place normally.",
        "tooltip.conveyance.slope_conveyor": "Changes height by one block. Direction follows the incoming belt; red means stuck. At a belt end, sneak-place to descend. Keep the cell above clear.",
        "message.conveyance.terrain_protected": "Natural terrain stays in place. Harvest resources or build with ordinary blocks.",
        "message.conveyance.slope_up": "Slope: uphill",
        "message.conveyance.slope_down": "Slope: downhill",
        "message.conveyance.slope_stuck": "Slope stuck: needs one incoming conveyor and an unambiguous outlet. Keep the block above clear.",
        "status.conveyance.miner.mining": "Mining",
        "status.conveyance.miner.no_deposit": "No deposit",
        "status.conveyance.miner.no_fuel": "No fuel",
        "status.conveyance.miner.no_output": "No output",
        "status.conveyance.miner.output_blocked": "Blocked",
        "status.conveyance.miner.mining.description": "Mining. Resources go directly into the connected output.",
        "status.conveyance.miner.no_deposit.description": "Place the miner over an ore or stone deposit.",
        "status.conveyance.miner.no_fuel.description": "Put coal, charcoal or Wood into the fuel slot.",
        "status.conveyance.miner.no_output.description": "Place a belt or crate directly against the lower furnace-textured face. Without an output, the miner waits and saves fuel.",
        "status.conveyance.miner.output_blocked.description": "The output is full, points toward the miner, or cannot accept this resource.",
        "research.conveyance.science_cost": "Research units: %s / %s",
        "research.conveyance.cost_red": "%s red science packs",
        "research.conveyance.cost_red_green": "%s red + %s green science packs",
        "research.conveyance.upgrades": "Upgrades",
        "research.conveyance.upgrade_item": "Upgrades: %s",
        "research.conveyance.lab_required": "Supply every required pack type to a powered lab.",
        "message.conveyance.research_reset": "Research reset. Select a technology and supply its packs to powered labs."
    })
    technology_text = {
        "automation": ("Automation", "Auto", "Unlock assembling machines."),
        "excavation": ("Metal Tools", "Tools", "Unlock metal tools for faster construction block mining. Natural terrain remains protected."),
        "logistics": ("Logistics", "Logistics", "Unlock electric inserters and balancing junctions."),
        "electric_mining": ("Electric Mining", "Mining", "Unlock powered miners and quarries."),
        "metal_storage": ("Metal Storage", "Storage", "Unlock iron crates."),
        "surveying": ("Surveying", "Survey", "Unlock radar exploration."),
        "long_inserters": ("Long Inserters", "Long arms", "Move items across two blocks on either side."),
        "steel_processing": ("Steel Processing", "Steel", "Smelt five iron ingots into one steel ingot."),
        "green_science": ("Green Science", "Green sci", "Craft green packs from a belt and an inserter."),
        "solar_energy": ("Solar Energy", "Solar", "Unlock 3x3 solar panels producing up to 60 kW."),
        "research_speed_1": ("Research Speed I", "Lab I", "Labs research 25% faster."),
        "research_speed_2": ("Research Speed II", "Lab II", "Labs research 50% faster in total."),
        "mining_speed": ("Advanced Mining", "Drilling", "Electric and burner miners work 25% faster."),
        "assembly_speed": ("Advanced Assembling", "Assembly", "Existing assemblers work 50% faster."),
        "smelting_speed": ("Advanced Smelting", "Smelting", "Existing furnaces work 50% faster."),
        "inserter_speed": ("Inserter Speed", "Arm speed", "All inserters work 50% faster."),
        "power_efficiency": ("Electrical Efficiency", "Power use", "Electrical machines use 20% less power."),
        "burner_efficiency": ("Burner Efficiency", "Fuel use", "Fuel lasts 25% longer in all burner machines.")
    }
    for key, (title, short, description) in technology_text.items():
        for suffix, text in (("name", title), ("short", short), ("description", description)):
            data[f"technology.conveyance.{key}.{suffix}"] = text
    data.update({
        "tooltip.conveyance.wood": "Fuel and a material for wooden crates.",
        "tooltip.conveyance.crate": "Stores items. Pickup returns the crate and its contents separately.",
        "tooltip.conveyance.miner": "Electric miner and quarry. Delivers resources directly to the connected output.",
        "tooltip.conveyance.burner_miner": "Fuel-powered miner and quarry. Delivers resources at its furnace face; facing coal miners can feed each other.",
        "tooltip.conveyance.assembler": "Automates the selected crafting recipe from supplied ingredients.",
        "tooltip.conveyance.industrial_furnace": "Smelts ore, stone and steel using fuel.",
        "tooltip.conveyance.research_lab": "Consumes the science pack combination required by the selected research.",
        "tooltip.conveyance.solar_panel": "Generates electricity in daylight with a clear view of the sky.",
        "tooltip.conveyance.generator": "Generates electricity while burning fuel.",
        "tooltip.conveyance.inserter": "Moves items from behind the arm to its front, using electricity.",
        "tooltip.conveyance.long_inserter": "Electric arm that moves items across a larger gap.",
        "tooltip.conveyance.burner_inserter": "Moves items using fuel. Can take fuel from its input to start itself.",
        "tooltip.conveyance.radar": "Automatically explores the map while loaded and powered.",
        "tooltip.conveyance.power_pole": "Connects nearby poles and supplies machines within its coverage.",
        "tooltip.conveyance.junction": "Balances connected outputs independently for each lane; left and right stay separate.",
        "tooltip.conveyance.conveyor": "Two independent lanes. Corners follow incoming belts. Aim at the air ahead or click a belt end to extend it.",
        "tooltip.conveyance.slope_conveyor": "Carries both lanes between heights. Click a belt end to extend uphill; sneak-click for downhill. A red mark means an ambiguous connection.",
        "tooltip.conveyance.stat.power_usage": "Power usage: %s kW",
        "tooltip.conveyance.stat.generation": "Power generation: %s kW",
        "tooltip.conveyance.stat.generation_peak": "Power generation: %s kW (peak)",
        "tooltip.conveyance.stat.fuel_usage": "Fuel consumption: %s kW while working",
        "tooltip.conveyance.stat.fuel_energy": "Fuel energy: %s MJ",
        "tooltip.conveyance.stat.fuel_energy_effective": "With current research: %s MJ",
        "tooltip.conveyance.stat.throughput": "Throughput: %s items/s",
        "tooltip.conveyance.stat.throughput_lane": "Per lane: %s items/s",
        "tooltip.conveyance.stat.throughput_port": "Per port: %s items/s",
        "tooltip.conveyance.stat.throughput_max": "Maximum throughput: %s items/s",
        "tooltip.conveyance.stat.electric_throughput": "Electrical throughput: unlimited",
        "tooltip.conveyance.stat.link_range": "Connection range: %s blocks",
        "tooltip.conveyance.stat.coverage": "Coverage radius: %s blocks",
        "tooltip.conveyance.stat.transfer": "Transfer rate: %s items/s",
        "tooltip.conveyance.stat.reach": "Reach: %s blocks on each side",
        "tooltip.conveyance.stat.mining": "Mining rate: %s items/s at full power",
        "tooltip.conveyance.stat.assembly": "Assembly rate: %s batches/s at full power",
        "tooltip.conveyance.stat.research": "Research rate: %s units/s at full power",
        "tooltip.conveyance.stat.size": "Size: %s × %s × %s blocks",
        "tooltip.conveyance.stat.buffer": "Belt buffer: %s items",
        "tooltip.conveyance.stat.storage": "Storage: %s slots",
        "tooltip.conveyance.stat.scan_area": "Scan area: %s × %s blocks"
    })
    data["message.conveyance.starter_crate"] = "Starter crate placed nearby at %s, %s, %s."
    data["block.conveyance.radar_part"] = "Radar Frame"
    for group, label in {"miners": "Miners", "inserters": "Inserters", "conveyors": "Conveyors", "storage": "Storage",
                         "furnaces": "Furnaces", "assemblers": "Assemblers", "generators": "Generators", "poles": "Power poles",
                         "radars": "Radars", "labs": "Labs", "science": "Science packs", "tools": "Tools", "components": "Components"}.items():
        data[f"menu.conveyance.group.{group}"] = label
    write(file, data)

# User-supplied item sprites take precedence over prototype voxel icons. Keep
# this last so regenerating assets never replaces imported art with a block model.
for sprite in sorted((ROOT / "assets/conveyance/textures/item").glob("*.png")):
    write(f"assets/conveyance/models/item/{sprite.stem}.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"conveyance:item/{sprite.stem}"}})

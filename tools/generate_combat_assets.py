"""Native model/recipe assets for the first factory-defence prototype."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"


def write(root, path, data):
    target = root / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def box(start, end, texture):
    return {"from": start, "to": end, "faces": {side: {"texture": "#" + texture, "uv": [0, 0, 16, 16]}
        for side in ("down", "up", "north", "south", "east", "west")}}


def generate_all(root=ROOT):
    recipes = {
        "pistol": (1, {"minecraft:iron_ingot": 5, "minecraft:copper_ingot": 5}),
        "firearm_magazine": (1, {"minecraft:iron_ingot": 4}),
        "repair_pack": (1, {"conveyance:gear": 2, "conveyance:electronic_circuit": 2}),
        "gun_turret": (8, {"minecraft:iron_ingot": 20, "minecraft:copper_ingot": 10, "conveyance:gear": 10}),
        "stone_wall": (.5, {"minecraft:stone_bricks": 5})}
    for name, (seconds, ingredients) in recipes.items():
        write(root, f"data/conveyance/recipes/{name}.json", {"type": "conveyance:factory_crafting", "tab": "combat", "crafting_time": seconds,
            "ingredients": [{"item": item, "count": count} for item, count in ingredients.items()], "result": {"item": "conveyance:" + name, "count": 1}})
    write(root, "data/conveyance/damage_type/bullet.json", {"message_id": "conveyance.bullet", "scaling": "never", "exhaustion": 0.0})
    write(root, "data/minecraft/tags/damage_type/bypasses_cooldown.json", {"replace": False, "values": ["conveyance:bullet"]})
    write(root, "data/conveyance/loot_tables/entities/small_biter.json", {"type": "minecraft:entity", "pools": []})
    write(root, "data/conveyance/loot_tables/entities/biter_nest.json", {"type": "minecraft:entity", "pools": []})
    for name in ("gun_turret", "gun_turret_part", "stone_wall", "wreck"):
        write(root, f"data/conveyance/loot_tables/blocks/{name}.json", {"type": "minecraft:block", "pools": []})
    textures = {"metal": "minecraft:block/iron_block", "dark": "minecraft:block/polished_deepslate", "copper": "minecraft:block/copper_block",
        "yellow": "minecraft:block/yellow_concrete", "red": "minecraft:block/red_concrete", "stone": "minecraft:block/stone_bricks", "particle": "minecraft:block/iron_block"}
    write(root, "assets/conveyance/models/block/wreck.json", {"parent": "minecraft:block/block", "textures": textures,
        "elements": [box([1, 0, 1], [15, 2, 15], "dark"), box([2, 2, 3], [7, 6, 12], "metal"),
            box([8, 2, 5], [14, 4, 14], "dark"), box([10, 4, 6], [12, 7, 9], "copper")]})
    write(root, "assets/conveyance/blockstates/wreck.json", {"variants": {"": {"model": "conveyance:block/wreck"}}})
    for name, parent in (("post", "template_wall_post"), ("side", "template_wall_side"), ("side_tall", "template_wall_side_tall"), ("inventory", "wall_inventory")):
        write(root, f"assets/conveyance/models/block/stone_wall_{name}.json", {"parent": "minecraft:block/" + parent, "textures": {"wall": "minecraft:block/stone_bricks"}})
    wall = [{"when": {"up": "true"}, "apply": {"model": "conveyance:block/stone_wall_post"}}]
    for side, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for height, suffix in (("low", "side"), ("tall", "side_tall")):
            wall.append({"when": {side: height}, "apply": {"model": "conveyance:block/stone_wall_" + suffix, "y": rotation, "uvlock": True}})
    write(root, "assets/conveyance/blockstates/stone_wall.json", {"multipart": wall})
    write(root, "assets/conveyance/models/item/stone_wall.json", {"parent": "conveyance:block/stone_wall_inventory"})
    write(root, "data/minecraft/tags/blocks/walls.json", {"replace": False, "values": ["conveyance:stone_wall"]})
    shapes = json.loads((root / "assets/conveyance/machine_shapes.json").read_text(encoding="utf-8"))
    shapes["gun_turret"] = []
    for part in range(12):
        px, py, pz = part % 2, part // 4, part // 2 % 2
        elements, solids = [], []
        pieces = (([2, 1, 2], [30, 5, 30], "dark"), ([3, 5, 3], [29, 28, 29], "metal"),
                  ([2, 28, 2], [30, 31, 30], "metal"), ([11, 31, 11], [21, 32, 21], "dark")) if py < 2 else (([1, 32, 1], [31, 41, 31], None),)
        for lo, hi, texture in pieces:
            a = [max(lo[0], px * 16) - px * 16, max(lo[1], py * 16) - py * 16, max(lo[2], pz * 16) - pz * 16]
            b = [min(hi[0], (px + 1) * 16) - px * 16, min(hi[1], (py + 1) * 16) - py * 16, min(hi[2], (pz + 1) * 16) - pz * 16]
            if any(b[i] <= a[i] for i in range(3)): continue
            if texture: elements.append(box(a, b, texture))
            solids.append([v / 16 for v in a + b])
        shapes["gun_turret"].append(solids)
        write(root, f"assets/conveyance/models/block/gun_turret_base_{part}.json", {"parent": "minecraft:block/block", "textures": textures, "elements": elements})
    write(root, "assets/conveyance/machine_shapes.json", shapes)
    variants = {}
    for part in range(50):
        for facing, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            for lit in (False, True):
                variants[f"part={part},facing={facing},lit={str(lit).lower()}"] = {"model": f"conveyance:block/gun_turret_base_{part}" if part < 8 else "conveyance:block/machines/empty", "y": rotation}
    for name in ("gun_turret", "gun_turret_part"): write(root, f"assets/conveyance/blockstates/{name}.json", {"variants": variants})
    head = [box([-6, 0, -6], [6, 6, 6], "metal"), box([-5, 6, -5], [5, 8, 5], "dark"),
        box([-2, 2, -14], [2, 5, -6], "dark"), box([-3, 1, -15], [3, 6, -12], "metal"), box([4, 2, -3], [7, 5, 3], "copper")]
    write(root, "assets/conveyance/models/block/gun_turret_head.json", {"parent": "minecraft:block/block", "textures": textures, "elements": head})
    icons = {
        "gun_turret": [box([1, 0, 1], [15, 3, 15], "dark"), box([3, 3, 3], [13, 8, 13], "metal"), box([4, 8, 4], [12, 13, 12], "metal"), box([6, 9, 0], [10, 12, 4], "dark")],
        "pistol": [box([3, 8, 3], [13, 12, 14], "dark"), box([8, 2, 10], [12, 9, 14], "metal"), box([4, 7, 1], [8, 11, 3], "metal")],
        "firearm_magazine": [box([3, 1, 4], [13, 10, 12], "dark")] + [box([x, 10, 5], [x + 2, 15, 11], "copper") for x in (3, 6, 9)],
        "repair_pack": [box([2, 2, 3], [14, 11, 13], "metal"), box([5, 11, 6], [11, 14, 10], "dark"), box([6, 5, 2], [10, 8, 3], "red")]}
    for name, elements in icons.items():
        if not (root / f"assets/conveyance/textures/item/{name}.png").exists():
            write(root, f"assets/conveyance/models/item/{name}.json", {"parent": "minecraft:block/block", "textures": textures, "elements": elements})
    labels = {
        "entity.conveyance.small_biter": "Small Biter", "entity.conveyance.biter_nest": "Biter Nest", "item.conveyance.pistol": "Pistol", "item.conveyance.firearm_magazine": "Firearm Magazine", "item.conveyance.repair_pack": "Repair Pack",
        "map.conveyance.biter_nest": "Biter nest — X %s, Z %s",
        "message.conveyance.attack_incoming": "Biter attack approaching: %s attackers from X %s, Z %s.",
        "hud.conveyance.attack": "Attack incoming · %s biters", "hud.conveyance.attack_from": "Nest: X %s, Z %s",
        "message.conveyance.nest_created": "Biter nest created at X %s, Z %s.", "message.conveyance.nest_no_space": "No clear, loaded ground for a nest nearby.",
        "message.conveyance.no_nest": "No active nest within 256 blocks.", "message.conveyance.nest_provoked": "Added pollution at the nest at X %s, Z %s; it can send a wave once its attack budget fills.",
        "message.conveyance.nest_info": "Nest X %s, Z %s: local pollution %s, attack budget %s, wave cooldown %ss.",
        "block.conveyance.gun_turret": "Gun Turret", "block.conveyance.gun_turret_part": "Gun Turret Frame", "block.conveyance.stone_wall": "Stone Wall", "block.conveyance.wreck": "Machine Wreck",
        "tooltip.conveyance.pistol": "Hold left click to fire. Uses magazines from your inventory; also works with the B-mode cursor. Shots originate at your character.",
        "tooltip.conveyance.firearm_magazine": "Feeds pistols and gun turrets. Inserters load up to ten magazines per turret.",
        "tooltip.conveyance.gun_turret": "A 2 x 2 x 2 base with a rotating gun above it; reserves three blocks of height. Supply magazines directly or with inserters. Fires over two-block walls without electricity.",
        "tooltip.conveyance.stone_wall": "Two-block-high connecting defence. One item places the whole wall; breaking either half picks it up.",
        "tooltip.conveyance.repair_pack": "Right-click a damaged building to fully repair it, or a wreck to rebuild its machine with its contents and settings.",
        "message.conveyance.wreck": "Machine wreck: use a repair pack to rebuild it. Picking it up recovers contents only.",
        "message.conveyance.repair_blocked": "Cannot rebuild: clear the machine's original footprint, including players standing in it.",
        "message.conveyance.repaired": "Building repaired.", "message.conveyance.no_ammo": "No firearm magazines.", "message.conveyance.ammo_loaded": "Loaded %s × %s.",
        "menu.conveyance.ammunition": "Ammunition", "menu.conveyance.loaded_rounds": "Loaded rounds: %s / 10",
        "menu.conveyance.category.combat": "Combat", "menu.conveyance.group.weapons": "Weapons", "menu.conveyance.group.ammunition": "Ammunition", "menu.conveyance.group.defences": "Defences", "menu.conveyance.group.repairs": "Repairs",
        "tooltip.conveyance.stat.health": "Health: %s", "tooltip.conveyance.stat.weapon_range": "Weapon range: %s blocks", "tooltip.conveyance.stat.shots": "Firing rate: %s shots/s", "tooltip.conveyance.stat.shot_damage": "Damage per shot: %s", "tooltip.conveyance.stat.rounds": "Rounds per magazine: %s",
        "technology.conveyance.basic_defence.name": "Basic Defence", "technology.conveyance.basic_defence.short": "Defence", "technology.conveyance.basic_defence.description": "Unlock ammunition-fed gun turrets and connecting stone walls."}
    for language in ("en_us", "en_gb"):
        path = root / f"assets/conveyance/lang/{language}.json"
        translations = json.loads(path.read_text(encoding="utf-8")); translations.update(labels); write(root, f"assets/conveyance/lang/{language}.json", translations)


if __name__ == "__main__":
    generate_all()

# Keep expanded early-game assets consistent after regenerating base assets.
import runpy
runpy.run_path(str(Path(__file__).with_name("generate_early_game_assets.py")))

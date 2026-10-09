"""Generate the 1.20.1 baseline data from an unmodified Mojang client jar.

Usage: python tools/generate_baseline.py path/to/1.20.1/client.jar
No third-party dependencies. Generated JSON is checked in; normal builds do not
require this script or depend on local Gradle cache paths.
"""

import argparse
import json
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "src/main/resources"

DISABLED = set("""
enchanting_table enchanted_book experience_bottle brewing_stand splash_potion
lingering_potion tipped_arrow golden_apple enchanted_golden_apple suspicious_stew
ender_pearl ender_eye ender_chest end_crystal end_rod dragon_egg dragon_head
dragon_wall_head dragon_breath elytra beacon conduit heart_of_the_sea nautilus_shell
recovery_compass echo_shard totem_of_undying nether_star blaze_rod blaze_powder
calibrated_sculk_sensor respawn_anchor crying_obsidian glowstone glowstone_dust
nether_gold_ore nether_quartz_ore sea_lantern prismarine_shard prismarine_crystals
prismarine prismarine_stairs prismarine_slab prismarine_wall
prismarine_bricks prismarine_brick_stairs prismarine_brick_slab
dark_prismarine dark_prismarine_stairs dark_prismarine_slab
ghast_tear magma_cream phantom_membrane spider_eye fermented_spider_eye
glistering_melon_slice rabbit_foot fire_charge slime_ball slime_block honey_block
shulker_shell shulker_box chorus_fruit popped_chorus_fruit chorus_plant chorus_flower
end_stone end_stone_bricks end_stone_brick_slab end_stone_brick_stairs end_stone_brick_wall
netherrack nether_wart nether_wart_block warped_wart_block nether_brick nether_bricks
nether_brick_slab nether_brick_stairs nether_brick_wall nether_brick_fence
red_nether_bricks red_nether_brick_slab red_nether_brick_stairs red_nether_brick_wall
chiseled_nether_bricks cracked_nether_bricks soul_sand soul_soil soul_fire
soul_torch soul_wall_torch soul_lantern soul_campfire shroomlight nether_sprouts
ancient_debris netherite_ingot netherite_scrap netherite_block netherite_upgrade_smithing_template
weeping_vines weeping_vines_plant twisting_vines twisting_vines_plant
spawner nether_portal end_portal end_gateway end_portal_frame reinforced_deepslate
redstone redstone_ore deepslate_redstone_ore redstone_block redstone_wire
redstone_torch redstone_wall_torch redstone_lamp repeater comparator lever
piston sticky_piston piston_head moving_piston observer dispenser dropper hopper
hopper_minecart powered_rail detector_rail activator_rail trapped_chest
chest chest_minecart bamboo_chest_raft oak_chest_boat spruce_chest_boat
birch_chest_boat jungle_chest_boat acacia_chest_boat dark_oak_chest_boat
mangrove_chest_boat cherry_chest_boat
tripwire tripwire_hook daylight_detector target fishing_rod
anvil chipped_anvil damaged_anvil grindstone bee_nest beehive
cod_bucket salmon_bucket tropical_fish_bucket pufferfish_bucket axolotl_bucket tadpole_bucket
""".split())
PREFIXES = ("sculk", "purpur_", "crimson_", "warped_", "stripped_crimson_",
            "stripped_warped_", "potted_crimson_", "potted_warped_", "netherite_", "infested_")
SUFFIXES = ("_spawn_egg", "_shulker_box", "_button", "_pressure_plate",
            "_head", "_wall_head", "_skull", "_wall_skull")


def disabled(name):
    return name in DISABLED or name.startswith(PREFIXES) or name.endswith(SUFFIXES)


def write(relative, data):
    path = RESOURCES / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def generate(jar_path):
    with ZipFile(jar_path) as jar:
        names = set(jar.namelist())

        def load(path):
            return json.loads(jar.read(path))

        blocks = {Path(n).stem for n in names if n.startswith("assets/minecraft/blockstates/")}
        items = {Path(n).stem for n in names if n.startswith("assets/minecraft/models/item/")
                 and n.endswith(".json") and not Path(n).stem.startswith("template_")
                 and not Path(n).stem.endswith("_trim")}
        banned_items = {"minecraft:" + n for n in items if disabled(n)}
        banned_blocks = {"minecraft:" + n for n in blocks if disabled(n)}
        write("data/conveyance/tags/items/disabled.json", {"values": sorted(banned_items)})
        write("data/conveyance/tags/blocks/disabled.json", {"values": sorted(banned_blocks)})

        # Every vanilla structure uses a biome tag. Emptying those tags prevents
        # placement even when the world's Generate Structures option is enabled.
        structure_paths = sorted(n for n in names if n.startswith("data/minecraft/worldgen/structure/")
                                 and n.endswith(".json"))
        structure_tags = set()
        for path in structure_paths:
            tag = load(path)["biomes"]
            if not isinstance(tag, str) or not tag.startswith("#minecraft:"):
                raise ValueError(f"Unexpected structure biome selector in {path}: {tag}")
            structure_tags.add(tag.removeprefix("#minecraft:"))
        for tag in sorted(structure_tags):
            write(f"data/minecraft/tags/worldgen/biome/{tag}.json", {"replace": True, "values": []})

        biomes = sorted(n for n in names if n.startswith("data/minecraft/worldgen/biome/")
                        and n.endswith(".json"))
        biome_ids = ["minecraft:" + Path(n).stem for n in biomes]
        creatures = sorted({entry["type"] for path in biomes
                            for group in load(path)["spawners"].values() for entry in group})
        write("data/conveyance/forge/biome_modifier/remove_spawns.json", {
            "type": "forge:remove_spawns", "biomes": biome_ids, "entity_types": creatures})

        features = ["desert_well", "fossil_lower", "fossil_upper", "monster_room",
                    "monster_room_deep", "ore_infested", "ore_redstone", "ore_redstone_lower",
                    "sculk_patch_ancient_city", "sculk_patch_deep_dark", "sculk_vein"]
        for feature in features:
            assert f"data/minecraft/worldgen/placed_feature/{feature}.json" in names
        write("data/conveyance/tags/worldgen/placed_feature/disabled.json", {
            "values": ["minecraft:" + feature for feature in features]})
        write("data/conveyance/forge/biome_modifier/remove_features.json", {
            "type": "forge:remove_features", "biomes": biome_ids,
            "features": "#conveyance:disabled"})

        # The optional starting bonus chest is a configured feature rather than
        # a structure, so changing structure biome tags does not disable it.
        write("data/minecraft/worldgen/configured_feature/bonus_chest.json", {
            "type": "minecraft:no_op", "config": {}})

        # Some tree features create occupied bee nests directly, without normal
        # creature spawning. Preserve the trees while removing those decorators.
        removed_nest_decorators = 0
        for path in sorted(n for n in names if n.startswith("data/minecraft/worldgen/configured_feature/")
                           and n.endswith(".json")):
            feature = load(path)
            if feature["type"] != "minecraft:tree":
                continue
            decorators = feature["config"].get("decorators", [])
            retained = [d for d in decorators if d["type"] != "minecraft:beehive"]
            if retained != decorators:
                feature["config"]["decorators"] = retained
                write(path, feature)
                removed_nest_decorators += 1

        def tag_items(tag, visited=frozenset()):
            if tag in visited:
                return set()
            namespace, name = tag.split(":", 1)
            path = f"data/{namespace}/tags/items/{name}.json"
            if path not in names:
                return set()
            result = set()
            for value in load(path)["values"]:
                value = value if isinstance(value, str) else value["id"]
                result |= tag_items(value[1:], visited | {tag}) if value.startswith("#") else {value}
            return result

        def blocked_ingredient(ingredient):
            if isinstance(ingredient, list):
                return bool(ingredient) and all(blocked_ingredient(i) for i in ingredient)
            if "item" in ingredient:
                return ingredient["item"] in banned_items
            if "tag" in ingredient:
                choices = tag_items(ingredient["tag"])
                return bool(choices) and choices <= banned_items
            return False

        removed_recipes = []
        for path in sorted(n for n in names if n.startswith("data/minecraft/recipes/") and n.endswith(".json")):
            recipe = load(path)
            output = recipe.get("result", {})
            output = output if isinstance(output, str) else output.get("item")
            ingredients = list(recipe.get("key", {}).values()) + recipe.get("ingredients", [])
            ingredients += [recipe[k] for k in ("ingredient", "base", "addition", "template") if k in recipe]
            if output in banned_items or any(blocked_ingredient(i) for i in ingredients):
                # Forge checks conditions before parsing the serializer. This
                # intentionally suppresses the recipe instead of crafting air.
                write(path, {"conditions": [{"type": "forge:false"}]})
                removed_recipes.append(Path(path).stem)

        advancements = sorted(n for n in names if n.startswith("data/minecraft/advancements/")
                              and n.endswith(".json"))
        for path in advancements:
            write(path, {"criteria": {"disabled": {"trigger": "minecraft:impossible"}}})

        # Keep IDs registered so existing saves remain readable. Removed blocks
        # yield nothing, rather than allowing an old redstone ore or spawner to
        # provide items through a different route.
        for block in sorted(banned_blocks):
            path = f"data/minecraft/loot_tables/blocks/{block.split(':', 1)[1]}.json"
            if path in names:
                write(path, {"type": "minecraft:block", "pools": []})

        # Names reflect that the remaining biome contains ordinary deep caves.
        for language in ("en_us", "en_gb"):
            write(f"assets/minecraft/lang/{language}.json", {"biome.minecraft.deep_dark": "Deep Caves"})

        report = {"minecraft": "1.20.1", "disabled_items": len(banned_items),
                  "disabled_blocks": len(banned_blocks), "disabled_structures": len(structure_paths),
                  "disabled_structure_biome_tags": len(structure_tags), "disabled_features": features,
                  "trees_without_bee_nests": removed_nest_decorators,
                  "disabled_recipes": removed_recipes, "disabled_advancements": len(advancements)}
        report_path = ROOT / "tools/baseline-report.json"
        report_path.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
        print(json.dumps({k: len(v) if k == "disabled_recipes" else v
                          for k, v in report.items()}))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("minecraft_jar", type=Path)
    generate(parser.parse_args().minecraft_jar)

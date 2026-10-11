"""Steam power recipes, pipe geometry and translations; no vanilla fluid blocks."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"


def write(root, path, data):
    target = root / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def box(start, end, texture):
    return {"from": start, "to": end, "faces": {
        side: {"texture": "#" + texture, "uv": [0, 0, 16, 16]}
        for side in ("down", "up", "north", "south", "west", "east")}}


def generate_all(root=ROOT):
    # Base-game Factorio 2.0 recipes; our ingots correspond to its plates.
    recipes = {
        "pipe": ("logistics", {"minecraft:iron_ingot": 1}),
        "offshore_pump": ("power", {"conveyance:gear": 2, "conveyance:pipe": 3}),
        "boiler": ("power", {"conveyance:industrial_furnace": 1, "conveyance:pipe": 4}),
        "steam_engine": ("power", {"conveyance:gear": 8, "conveyance:pipe": 5, "minecraft:iron_ingot": 10})}
    for name, (tab, ingredients) in recipes.items():
        write(root, f"data/conveyance/recipes/{name}.json", {
            "type": "conveyance:factory_crafting", "tab": tab, "crafting_time": .5,
            "ingredients": [{"item": item, "count": count} for item, count in ingredients.items()],
            "result": {"item": "conveyance:" + name, "count": 1}})
        for block in (name,) if name == "pipe" else (name, name + "_part"):
            write(root, f"data/conveyance/loot_tables/blocks/{block}.json", {"type": "minecraft:block", "pools": []})
    for fluid, marker in ((0, "gray_concrete"), (1, "light_blue_concrete"), (2, "orange_concrete")):
        write(root, f"assets/conveyance/models/block/pipe_core_{fluid}.json", {
            "parent": "minecraft:block/block", "textures": {
                "body": "minecraft:block/iron_block", "marker": "minecraft:block/" + marker, "particle": "minecraft:block/iron_block"},
            "elements": [box([5, 5, 5], [11, 11, 11], "body"), box([6, 11, 6], [10, 11.1, 10], "marker")]})
    arms = {"north": ([5, 5, 0], [11, 11, 5]), "south": ([5, 5, 11], [11, 11, 16]),
            "west": ([0, 5, 5], [5, 11, 11]), "east": ([11, 5, 5], [16, 11, 11]),
            "down": ([5, 0, 5], [11, 5, 11]), "up": ([5, 11, 5], [11, 16, 11])}
    parts = [{"when": {"fluid": str(fluid)}, "apply": {"model": f"conveyance:block/pipe_core_{fluid}"}} for fluid in range(3)]
    for side, (start, end) in arms.items():
        write(root, f"assets/conveyance/models/block/pipe_{side}.json", {
            "parent": "minecraft:block/block", "textures": {"body": "minecraft:block/iron_block", "particle": "minecraft:block/iron_block"},
            "elements": [box(start, end, "body")]})
        parts.append({"when": {side: "true"}, "apply": {"model": f"conveyance:block/pipe_{side}"}})
    write(root, "assets/conveyance/blockstates/pipe.json", {"multipart": parts})
    write(root, "assets/conveyance/models/item/pipe.json", {
        "parent": "minecraft:block/block", "textures": {"body": "minecraft:block/iron_block", "particle": "minecraft:block/iron_block"},
        "elements": [box([5, 5, 0], [11, 11, 16], "body"), box([4, 4, 6], [12, 12, 10], "body")]})
    translations = {
        "block.conveyance.pipe": "Pipe", "block.conveyance.boiler": "Boiler",
        "block.conveyance.boiler_part": "Boiler Frame", "block.conveyance.steam_engine": "Steam Engine",
        "block.conveyance.steam_engine_part": "Steam Engine Frame", "block.conveyance.offshore_pump": "Offshore Pump",
        "block.conveyance.offshore_pump_part": "Offshore Pump Frame",
        "fluid.conveyance.empty": "Empty", "fluid.conveyance.water": "Water", "fluid.conveyance.steam": "Steam (165\u00b0C)",
        "tooltip.conveyance.pipe": "Connects on all six sides, including vertically. Shares fluid with connected pipes and fixed machine ports. Blue: water; orange: steam.",
        "tooltip.conveyance.offshore_pump": "Place on a bank touching water. Automatically faces its intake into the water; the blue outlet is at normal building height. Needs no fuel or electricity.",
        "tooltip.conveyance.boiler": "Burns fuel to heat water into steam. Blue ports pass water through left and right; the orange front port outputs steam. One boiler supplies two steam engines.",
        "tooltip.conveyance.steam_engine": "Produces electricity from steam according to power demand. Orange ports on both ends pass steam through. Needs power pole coverage.",
        "tooltip.conveyance.wood": "Burnable wood, with half the fuel energy of coal. Used to craft wooden crates.",
        "tooltip.conveyance.stat.steam_production": "Steam output: %s units/s",
        "tooltip.conveyance.stat.water_consumption": "Water consumption: %s units/s",
        "tooltip.conveyance.stat.steam_consumption": "Steam consumption: %s units/s at full output",
        "tooltip.conveyance.stat.water_production": "Water output: %s units/s",
        "tooltip.conveyance.stat.steam_temperature": "Steam temperature: %s\u00b0C",
        "tooltip.conveyance.stat.fluid_throughput": "Fluid throughput: unlimited",
        "tooltip.conveyance.stat.fluid_storage": "Fluid storage: %s units",
        "message.conveyance.fluid_mixing": "Cannot connect water and steam in the same pipe network.",
        "message.conveyance.pump_shore": "Place the pump on a bank, touching water one block below its intake.",
        "message.conveyance.fluid_status": "%s: %s / %s units. Pipe throughput: unlimited.",
        "status.conveyance.fluid.idle": "Idle: no power demand",
        "status.conveyance.fluid.working": "Working",
        "status.conveyance.fluid.no_water": "Waiting for water",
        "status.conveyance.fluid.no_fuel": "Waiting for fuel",
        "status.conveyance.fluid.output_full": "Fluid storage full",
        "status.conveyance.fluid.no_steam": "Waiting for steam",
        "status.conveyance.fluid.no_coverage": "Needs power pole coverage",
        "menu.conveyance.fluids": "Fluids",
        "menu.conveyance.fluid_buffer": "%s: %s / %s",
        "menu.conveyance.fluid_fuel": "Fuel reserve: %s MJ",
        "menu.conveyance.steam_rate": "Steam output: %s/s",
        "menu.conveyance.steam_consumption": "Steam consumption: %s/s",
        "menu.conveyance.water_rate": "Water output: %s/s",
        "menu.conveyance.fluid_boiler_ports": "Blue: water through sides. Orange: steam at front.",
        "menu.conveyance.fluid_engine_ports": "Steam passes through both ends. Maximum output: 900 kW.",
        "menu.conveyance.fluid_pump_ports": "Blue outlet faces inland, one block above the bank.",
        "menu.conveyance.group.pipes": "Pipes", "menu.conveyance.group.pumps": "Pumps", "menu.conveyance.group.boilers": "Boilers"}
    for language in ("en_us", "en_gb"):
        path = f"assets/conveyance/lang/{language}.json"
        data = json.loads((root / path).read_text(encoding="utf-8"))
        data.update(translations)
        write(root, path, data)


if __name__ == "__main__":
    generate_all()

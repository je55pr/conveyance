"""Oil/chemistry/science prototype assets and creative generator metadata."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"


def write(root, path, data):
    target = root / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def box(start, end, texture):
    return {"from": start, "to": end, "faces": {side: {"texture": "#" + texture, "uv": [0, 0, 16, 16]}
        for side in ("down", "up", "north", "south", "west", "east")}}


def generate_all(root=ROOT):
    recipes = {
        "pumpjack": ("production", 5, {"conveyance:steel_ingot": 5, "conveyance:gear": 10, "conveyance:electronic_circuit": 5, "conveyance:pipe": 10}, 1, False),
        "oil_refinery": ("production", 8, {"conveyance:steel_ingot": 15, "conveyance:gear": 10, "minecraft:stone_bricks": 10, "conveyance:electronic_circuit": 10, "conveyance:pipe": 10}, 1, False),
        "chemical_plant": ("production", 5, {"conveyance:steel_ingot": 5, "conveyance:gear": 5, "conveyance:electronic_circuit": 5, "conveyance:pipe": 5}, 1, False),
        "engine_unit": ("intermediates", 10, {"conveyance:steel_ingot": 1, "conveyance:gear": 1, "conveyance:pipe": 2}, 1, True),
        "advanced_circuit": ("intermediates", 6, {"conveyance:electronic_circuit": 2, "conveyance:plastic_bar": 2, "conveyance:copper_wire": 4}, 1, False),
        "chemical_science_pack": ("science", 24, {"conveyance:engine_unit": 2, "conveyance:advanced_circuit": 3, "conveyance:sulfur": 1}, 2, False),
        "generator": ("power", .5, {"minecraft:iron_ingot": 1}, 1, False)}
    for name, (tab, seconds, inputs, count, machine_only) in recipes.items():
        write(root, f"data/conveyance/recipes/{name}.json", {"type": "conveyance:factory_crafting", "tab": tab,
            "crafting_time": seconds, "machine_only": machine_only, "creative_only": name == "generator",
            "ingredients": [{"item": item, "count": amount} for item, amount in inputs.items()], "result": {"item": "conveyance:" + name, "count": count}})
    for name in ("plastic_bar", "sulfur"):
        write(root, f"data/conveyance/recipes/creative_{name}.json", {"type":"conveyance:factory_crafting", "tab":"intermediates", "creative_only":True,
            "ingredients":[{"item":"minecraft:iron_ingot","count":1}], "result":{"item":"conveyance:"+name,"count":1}})
    def fluid(channel, kind, amount): return {"channel": channel, "fluid": kind, "amount": amount}
    chemistry = {
        "basic_oil_processing": ("oil_refinery", 5, {}, [fluid(0,"crude_oil",100)], [fluid(4,"petroleum",45)], None),
        "plastic_bar": ("chemical_plant", 1, {"minecraft:coal":1}, [fluid(0,"petroleum",20)], [], ("plastic_bar",2)),
        "sulfur": ("chemical_plant", 1, {}, [fluid(0,"petroleum",30),fluid(1,"water",30)], [], ("sulfur",2)),
        "advanced_oil_processing": ("oil_refinery", 5, {}, [fluid(0,"crude_oil",100),fluid(1,"water",50)], [fluid(2,"heavy_oil",25),fluid(3,"light_oil",45),fluid(4,"petroleum",55)], None),
        "heavy_oil_cracking": ("chemical_plant", 2, {}, [fluid(0,"heavy_oil",40),fluid(1,"water",30)], [fluid(2,"light_oil",30)], None),
        "light_oil_cracking": ("chemical_plant", 2, {}, [fluid(0,"light_oil",30),fluid(1,"water",30)], [fluid(2,"petroleum",20)], None)}
    for name, (machine, seconds, inputs, fluid_inputs, outputs, result) in chemistry.items():
        data = {"type":"conveyance:chemical_processing", "machine":machine, "seconds":seconds,
            "items":[{"item":item,"count":amount} for item,amount in inputs.items()], "inputs":fluid_inputs, "outputs":outputs}
        if result: data["result"] = {"item":"conveyance:"+result[0],"count":result[1]}
        write(root,f"data/conveyance/recipes/{name}.json",data)
    for name in ("pumpjack", "oil_refinery", "chemical_plant"):
        for block in (name, name+"_part"): write(root,f"data/conveyance/loot_tables/blocks/{block}.json",{"type":"minecraft:block","pools":[]})
    write(root,"assets/conveyance/models/block/oil_deposit.json",{"parent":"minecraft:block/block", "textures":{
        "stone":"minecraft:block/stone", "oil":"minecraft:block/black_concrete", "particle":"minecraft:block/black_concrete"},
        "elements":[box([0,0,0],[16,16,16],"stone"),box([3,16,3],[13,16.01,13],"oil"),box([5,16,1],[11,16.01,15],"oil")]})
    write(root,"assets/conveyance/blockstates/oil_deposit.json",{"variants":{"":{"model":"conveyance:block/oil_deposit"}}})
    write(root,"data/conveyance/loot_tables/blocks/oil_deposit.json",{"type":"minecraft:block","pools":[]})
    colors = ("gray_concrete","light_blue_concrete","orange_concrete","black_concrete","purple_concrete","brown_concrete","yellow_concrete")
    parts=[]
    for index,color in enumerate(colors):
        write(root,f"assets/conveyance/models/block/pipe_core_{index}.json",{"parent":"minecraft:block/block", "textures":{
            "body":"minecraft:block/iron_block","marker":"minecraft:block/"+color,"particle":"minecraft:block/iron_block"},
            "elements":[box([5,5,5],[11,11,11],"body"),box([6,11,6],[10,11.1,10],"marker")]})
        parts.append({"when":{"fluid":str(index)},"apply":{"model":f"conveyance:block/pipe_core_{index}"}})
    for side in ("north","south","east","west","up","down"): parts.append({"when":{side:"true"},"apply":{"model":"conveyance:block/pipe_"+side}})
    write(root,"assets/conveyance/blockstates/pipe.json",{"multipart":parts})
    icon_textures={"metal":"minecraft:block/iron_block","dark":"minecraft:block/polished_deepslate","red":"minecraft:block/red_concrete","blue":"minecraft:block/light_blue_concrete","yellow":"minecraft:block/yellow_concrete","white":"minecraft:block/white_concrete","copper":"minecraft:block/copper_block"}
    icons={
        "engine_unit":[box([2,4,3],[14,12,13],"dark"),box([3,8,4],[13,14,12],"metal"),box([6,5,0],[10,9,3],"metal"),box([4,14,5],[6,16,7],"copper"),box([10,14,9],[12,16,11],"copper")],
        "plastic_bar":[box([1,5,3],[15,9,13],"white"),box([2,9,4],[14,10,12],"metal")],
        "sulfur":[box([3,2,4],[13,7,12],"yellow"),box([5,7,5],[11,11,10],"yellow")],
        "advanced_circuit":[box([1,6,2],[15,8,14],"red"),box([5,8,5],[11,10,11],"dark")]+[box([x,8,3],[x+1,8.2,13],"copper") for x in (2,4,12)],
        "chemical_science_pack":[box([5,0,5],[11,2,11],"metal"),box([3,2,3],[13,11,13],"blue"),box([5,11,5],[11,14,11],"white"),box([5,14,5],[11,16,11],"metal")]}
    for name,elements in icons.items():
        sprite = root/f"assets/conveyance/textures/item/{name}.png"
        write(root,f"assets/conveyance/models/item/{name}.json", {"parent":"minecraft:item/generated","textures":{"layer0":f"conveyance:item/{name}"}}
              if sprite.exists() else {"parent":"minecraft:block/block","textures":icon_textures,"elements":elements})
    translations={
        "block.conveyance.generator":"Creative Generator", "tooltip.conveyance.generator":"Creative-only generator. Powers its pole network without fuel or limits.",
        "tooltip.conveyance.stat.generation_unlimited":"Power generation: unlimited", "tooltip.conveyance.stat.creative_only":"Creative mode only",
        "message.conveyance.creative_only":"This item is available only in Creative mode.",
        "block.conveyance.pumpjack":"Pumpjack", "block.conveyance.oil_refinery":"Oil Refinery", "block.conveyance.chemical_plant":"Chemical Plant", "block.conveyance.oil_deposit":"Oil Well",
        "tooltip.conveyance.pumpjack":"Extracts crude oil from a well beneath its base. The front-right port outputs oil; wells remain intact.",
        "tooltip.conveyance.oil_refinery":"Refines crude oil. Inputs behind: water on the left, crude on the right. Outputs in front: heavy, light and petroleum from left to right.",
        "tooltip.conveyance.chemical_plant":"Processes fluid and item recipes. Inputs behind: primary fluid on the left, water on the right. Fluid output is at the front-left.",
        "item.conveyance.engine_unit":"Engine Unit", "item.conveyance.plastic_bar":"Plastic Bar", "item.conveyance.sulfur":"Sulfur", "item.conveyance.advanced_circuit":"Advanced Circuit", "item.conveyance.chemical_science_pack":"Chemical Science Pack",
        "tooltip.conveyance.engine_unit":"Manufactured in an assembling machine. Cannot be hand crafted in Survival.",
        "tooltip.conveyance.chemical_science_pack":"Blue science for advanced research. Labs consume it alongside the technology's other required packs.",
        "tooltip.conveyance.stat.crafting_speed":"Crafting speed: %sx", "tooltip.conveyance.stat.oil_production":"Crude oil output: %s units/s",
        "fluid.conveyance.crude_oil":"Crude oil", "fluid.conveyance.petroleum":"Petroleum gas", "fluid.conveyance.heavy_oil":"Heavy oil", "fluid.conveyance.light_oil":"Light oil",
        "message.conveyance.fluid_mixing":"Cannot mix different fluids in one pipe network.",
        "message.conveyance.chemical_selection_failed":"Cannot change recipe. Check fluid connections and inventory space.",
        "message.conveyance.starter_oil":"Starter oil wells placed near X %s, Z %s.", "map.conveyance.oil":"Oil wells",
        "menu.conveyance.group.chemistry":"Chemical production", "menu.conveyance.oil_rate":"Crude oil output: %s/s", "menu.conveyance.recipe_seconds":"Base recipe time: %ss", "menu.conveyance.recipe_products":"Products",
        "status.conveyance.chemical.select_recipe":"Select a recipe", "status.conveyance.chemical.no_oil":"Needs an oil well underneath",
        "status.conveyance.chemical.inputs":"Waiting for inputs", "status.conveyance.chemical.output_full":"Output storage full",
        "status.conveyance.chemical.no_power":"Waiting for power", "status.conveyance.chemical.working":"Working",
        "research.conveyance.cost_red_green_blue":"%s red + %s green + %s blue science packs"}
    for name in ("pumpjack","oil_refinery","chemical_plant"): translations["block.conveyance."+name+"_part"] = translations["block.conveyance."+name]+" Frame"
    titles={"basic_oil_processing":"Basic oil processing","plastic_bar":"Plastic bars","sulfur":"Sulfur","advanced_oil_processing":"Advanced oil processing","heavy_oil_cracking":"Heavy oil cracking","light_oil_cracking":"Light oil cracking"}
    for key,title in titles.items(): translations["recipe.conveyance."+key]=title
    techs={"engine":("Engine","Unlock engine units, manufactured from steel, gears and pipes."),
        "oil_processing":("Oil Processing","Unlock pumpjacks, oil refineries, chemical plants and basic refining."),
        "plastics":("Plastics","Turn coal and petroleum into plastic bars."),
        "sulfur_processing":("Sulfur Processing","Produce sulfur from petroleum gas and water."),
        "advanced_circuits":("Advanced Circuits","Build advanced circuits from electronic circuits, copper wire and plastic."),
        "chemical_science":("Chemical Science","Unlock blue science made from engines, advanced circuits and sulfur."),
        "advanced_oil_processing":("Advanced Oil Processing","Produce heavy oil, light oil and more petroleum; unlock heavy/light oil cracking.")}
    for key,(title,description) in techs.items():
        for suffix,value in (("name",title),("short",title),("description",description)): translations[f"technology.conveyance.{key}.{suffix}"]=value
    for language in ("en_us","en_gb"):
        path=f"assets/conveyance/lang/{language}.json"; data=json.loads((root/path).read_text(encoding="utf-8")); data.update(translations); write(root,path,data)


if __name__=="__main__": generate_all()

# Keep expanded early-game assets consistent after regenerating base assets.
import runpy
runpy.run_path(str(Path(__file__).with_name("generate_early_game_assets.py")))

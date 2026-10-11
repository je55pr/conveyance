"""Additional early-game assets and balance from Factorio base 2.0.77.

Uses Conveyance's existing art. Does not replace the user's texture files.
"""
import copy
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'

def write(path, data):
    target = RES / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')

def ingredient(name, count):
    aliases = {'iron':'minecraft:iron_ingot', 'copper':'minecraft:copper_ingot', 'coal':'minecraft:coal', 'stone':'minecraft:cobblestone', 'wood':'conveyance:wood'}
    return {'item': aliases.get(name, 'conveyance:' + name), 'count': count}

# Recipes use counted ingredients; one batch makes one result unless specified.
RECIPES = {
 'assembler': ('production', .5, {'electronic_circuit':3,'gear':5,'iron':9}),
 'assembler_2': ('production', .5, {'steel_ingot':2,'electronic_circuit':3,'gear':5,'assembler':1}),
 'steel_furnace': ('production', 3, {'steel_ingot':6,'stone_bricks':10}),
 'fast_inserter': ('logistics', .5, {'electronic_circuit':2,'iron':2,'inserter':1}),
 'junction': ('logistics', 1, {'electronic_circuit':5,'iron':5,'conveyor':4}),
 'fast_conveyor': ('logistics', .5, {'gear':5,'conveyor':1}),
 'fast_slope_conveyor': ('logistics', .5, {'gear':5,'slope_conveyor':1}),
 'fast_junction': ('logistics', 2, {'junction':1,'gear':10,'electronic_circuit':10}),
 'steel_crate': ('logistics', .5, {'steel_ingot':8}),
 'storage_tank': ('production', 3, {'iron':20,'steel_ingot':5}),
 'fluid_pump': ('production', 2, {'engine_unit':1,'steel_ingot':1,'pipe':1}),
 'accumulator': ('power', 10, {'iron':2,'battery':5}),
 'speed_module': ('production', 15, {'advanced_circuit':5,'electronic_circuit':5}),
 'productivity_module': ('production', 15, {'advanced_circuit':5,'electronic_circuit':5}),
 'efficiency_module': ('production', 15, {'advanced_circuit':5,'electronic_circuit':5}),
 'submachine_gun': ('combat', 10, {'gear':10,'copper':5,'iron':10}),
 'shotgun': ('combat', 10, {'iron':15,'gear':5,'copper':10,'wood':5}),
 'shotgun_shell': ('combat', 3, {'copper':2,'iron':2}),
 'piercing_magazine': ('combat', 6, {'firearm_magazine':2,'steel_ingot':1,'copper':2}),
 'grenade': ('combat', 8, {'iron':5,'coal':10}),
}
for name, (tab, seconds, costs) in RECIPES.items():
    write(f'data/conveyance/recipes/{name}.json', {'type':'conveyance:factory_crafting', 'tab':tab, 'crafting_time':seconds,
        'ingredients':[ingredient(item,count) for item,count in costs.items()], 'result':{'item':'conveyance:'+name,'count':1}})

def chemical(name, items, inputs, outputs=(), result=None, seconds=1):
    data = {'type':'conveyance:chemical_processing', 'machine':'chemical_plant', 'seconds':seconds,
        'items':[ingredient(item,count) for item,count in items.items()],
        'inputs':[{'channel':index,'fluid':fluid,'amount':amount} for index,(fluid,amount) in enumerate(inputs)],
        'outputs':[{'channel':2+index,'fluid':fluid,'amount':amount} for index,(fluid,amount) in enumerate(outputs)]}
    if result: data['result'] = {'item':'conveyance:'+result,'count':1}
    write(f'data/conveyance/recipes/{name}.json', data)

chemical('sulfuric_acid', {'sulfur':5,'iron':1}, [('water',100)], [('sulfuric_acid',50)])
chemical('battery', {'iron':1,'copper':1}, [('sulfuric_acid',20)], result='battery', seconds=4)
chemical('solid_fuel', {}, [('petroleum',20)], result='solid_fuel')

def clone_machine(original, new, texture_changes):
    for path in (RES / f'assets/conveyance/models/block/machines/{original}').glob('*.obj'):
        target = RES / f'assets/conveyance/models/block/machines/{new}/{path.name}'
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(path.read_text(encoding='utf-8'), encoding='utf-8')
    for path in (RES / f'assets/conveyance/models/block/machines/{original}').glob('*.json'):
        model = json.loads(path.read_text())
        # All the casing geometry comes from the original model generator.
        text = json.dumps(model).replace('/'+original+'/', '/'+new+'/')
        for old, replacement in texture_changes.items(): text = text.replace(old,replacement)
        write(f'assets/conveyance/models/block/machines/{new}/{path.name}', json.loads(text))
    for suffix in ('','_part'):
        text = (RES / f'assets/conveyance/blockstates/{original}{suffix}.json').read_text().replace('/'+original+'/', '/'+new+'/')
        write(f'assets/conveyance/blockstates/{new}{suffix}.json', json.loads(text))
    write(f'assets/conveyance/models/item/{new}.json', {'parent':f'conveyance:block/machines/{new}/item'})

clone_machine('assembler','assembler_2', {'minecraft:block/light_gray_concrete':'minecraft:block/blue_concrete', 'minecraft:block/copper_block':'minecraft:block/iron_block'})
clone_machine('industrial_furnace','steel_furnace', {'minecraft:block/bricks':'minecraft:block/iron_block','minecraft:block/stone_bricks':'minecraft:block/polished_deepslate'})
shape_path = RES / 'assets/conveyance/machine_shapes.json'
shapes = json.loads(shape_path.read_text())
shapes['assembler_2'] = copy.deepcopy(shapes['assembler'])
shapes['steel_furnace'] = copy.deepcopy(shapes['industrial_furnace'])

for original,new in [('conveyor','fast_conveyor'),('slope_conveyor','fast_slope_conveyor'),('junction','fast_junction'),('inserter','fast_inserter'),('iron_crate','steel_crate')]:
    write(f'assets/conveyance/blockstates/{new}.json', json.loads((RES/f'assets/conveyance/blockstates/{original}.json').read_text().replace('block/'+original, 'block/'+new)))
    for path in (RES/'assets/conveyance/models/block').glob(original+'*.json'):
        name = path.stem.replace(original,new,1)
        data = json.loads(path.read_text().replace('block/'+original,'block/'+new))
        if original in ('conveyor','slope_conveyor','junction'):
            for texture in ('frame','arrow'):
                if texture in data.get('textures',{}): data['textures'][texture] = 'minecraft:block/red_concrete'
        if original == 'inserter':
            for key,value in data.get('textures',{}).items():
                if 'yellow' in value: data['textures'][key] = 'minecraft:block/blue_concrete'
        if original == 'iron_crate':
            data['textures']['body'] = 'minecraft:block/netherite_block'
            data['textures']['frame'] = 'minecraft:block/iron_block'
        write(f'assets/conveyance/models/block/{name}.json',data)
    write(f'assets/conveyance/models/item/{new}.json', {'parent':f'conveyance:block/{new}_up' if new == 'fast_slope_conveyor' else f'conveyance:block/{new}'})

# Transport tooltips are derived from the moving queue's speed, rather than stale copy.

def box(start,end,texture):
    return {'from':start,'to':end,'faces':{side:{'texture':'#'+texture} for side in ('north','south','west','east','up','down')}}

def utility_model(name,width,depth,height,boxes):
    parts = []; bounds = []
    for part in range(width*depth*height):
        ox,oz,oy = part%width, part//width%depth, part//(width*depth)
        elements = []; collisions = []
        for start,end,texture in boxes:
            low = [max(start[i], [ox,oy,oz][i]) for i in range(3)]
            high = [min(end[i], [ox,oy,oz][i]+1) for i in range(3)]
            if any(low[i]>=high[i] for i in range(3)): continue
            local_low = [low[i]-[ox,oy,oz][i] for i in range(3)]; local_high = [high[i]-[ox,oy,oz][i] for i in range(3)]
            elements.append(box([n*16 for n in local_low],[n*16 for n in local_high],texture)); collisions.append(local_low+local_high)
        bounds.append(collisions)
        write(f'assets/conveyance/models/block/machines/{name}/part_{part}.json', {'parent':'minecraft:block/block','textures':{
            'body':'minecraft:block/iron_block','dark':'minecraft:block/polished_deepslate','accent':'minecraft:block/cyan_concrete','particle':'minecraft:block/iron_block'},'elements':elements})
        parts.append(part)
    variants = {}
    for part in range(50):
        for facing,turn in [('north',0),('east',90),('south',180),('west',270)]:
            for lit in ('false','true'): variants[f'part={part},facing={facing},lit={lit}'] = {'model':f'conveyance:block/machines/{name}/part_{part}' if part in parts else 'conveyance:block/machines/empty','y':turn}
    for suffix in ('','_part'): write(f'assets/conveyance/blockstates/{name}{suffix}.json',{'variants':variants})
    shapes[name] = bounds
    # A compact inventory representation; world geometry uses the entire footprint.
    write(f'assets/conveyance/models/block/{name}_item.json', {'parent':'minecraft:block/cube_all','textures':{'all':'minecraft:block/iron_block'}})
    write(f'assets/conveyance/models/item/{name}.json',{'parent':f'conveyance:block/{name}_item'})

utility_model('storage_tank',3,3,2,[([.12,0,.12],[2.88,.2,2.88],'dark'),([.45,.2,.15],[2.55,1.8,2.85],'body'),([.15,.2,.45],[2.85,1.8,2.55],'body'),([.45,1.8,.45],[2.55,1.95,2.55],'dark')])
utility_model('fluid_pump',1,2,1,[([.1,0,.1],[.9,.2,1.9],'dark'),([.2,.2,.2],[.8,.8,1.8],'body'),([.15,.4,.65],[.85,.9,1.35],'accent')])
utility_model('accumulator',2,2,2,[([.1,0,.1],[1.9,.2,1.9],'dark'),([.25,.2,.25],[1.75,1.65,1.75],'body'),([.2,1.5,.2],[1.8,1.8,1.8],'dark'),([.65,1.8,.65],[1.35,1.95,1.35],'accent')])
write('assets/conveyance/machine_shapes.json',shapes)

ITEM_TEXTURES = {'battery':'minecraft:item/honey_bottle','solid_fuel':'minecraft:item/charcoal','speed_module':'minecraft:item/lapis_lazuli',
 'productivity_module':'minecraft:item/redstone','efficiency_module':'minecraft:item/emerald','piercing_magazine':'minecraft:item/copper_ingot',
 'submachine_gun':'minecraft:item/crossbow_standby','shotgun':'minecraft:item/crossbow_standby','shotgun_shell':'minecraft:item/iron_nugget','grenade':'minecraft:item/slime_ball'}
for name,texture in ITEM_TEXTURES.items(): write(f'assets/conveyance/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':texture}})

NAMES = {'assembler_2':'Assembling Machine 2','steel_furnace':'Steel Furnace','storage_tank':'Storage Tank','fluid_pump':'Pump','accumulator':'Accumulator',
 'fast_conveyor':'Fast Conveyor','fast_slope_conveyor':'Fast Slope Conveyor','fast_junction':'Fast Junction','fast_inserter':'Fast Inserter','steel_crate':'Steel Crate',
 'battery':'Battery','solid_fuel':'Solid Fuel','speed_module':'Speed Module','productivity_module':'Productivity Module','efficiency_module':'Efficiency Module',
 'piercing_magazine':'Piercing Rounds Magazine','submachine_gun':'Submachine Gun','shotgun':'Shotgun','shotgun_shell':'Shotgun Shell','grenade':'Grenade'}
for locale in ('en_us','en_gb'):
    lang_path = RES/f'assets/conveyance/lang/{locale}.json'; lang = json.loads(lang_path.read_text())
    for name,title in NAMES.items():
        lang[f'item.conveyance.{name}'] = title; lang[f'block.conveyance.{name}'] = title
    lang.update({'fluid.conveyance.sulfuric_acid':'Sulfuric acid','menu.conveyance.modules':'Modules','menu.conveyance.module_effects':'Machine modifiers',
        'overlay.conveyance.layer.power':'Power coverage','overlay.conveyance.layer.recipes':'Production icons',
        'overlay.conveyance.layer.warnings':'Machine warnings','overlay.conveyance.layer.arrows':'Inserter directions',
        'overlay.conveyance.enabled':'Enabled: click to hide','overlay.conveyance.disabled':'Disabled: click to show',
        'overlay.conveyance.help.power':'Show every nearby pole coverage area. Hovering or placing a pole always previews its coverage at the selected height.',
        'overlay.conveyance.help.recipes':'Show machine output and research icons. Badges fade out as machines become too small on screen.',
        'overlay.conveyance.help.warnings':'Show faults such as missing power, fuel, recipes and ammunition. Hover a badge for details.',
        'overlay.conveyance.help.arrows':'Show all inserter paths. Blue arrows mark pickup; orange arrows mark drop. Hovering or placing an inserter always previews its path.',
        'overlay.conveyance.operation':'Activity: %s','overlay.conveyance.power_fraction':'Electricity supplied: %s%%',
        'overlay.conveyance.status.working':'Working','overlay.conveyance.status.idle':'Idle',
        'overlay.conveyance.status.no_coverage':'Outside power-pole coverage','overlay.conveyance.status.no_power':'Network has no power',
        'overlay.conveyance.status.low_power':'Insufficient electricity','overlay.conveyance.status.no_fuel':'Out of fuel',
        'overlay.conveyance.status.no_recipe':'No recipe selected','overlay.conveyance.status.inputs':'Waiting for ingredients',
        'overlay.conveyance.status.output_full':'Output is full or disconnected','overlay.conveyance.status.no_resource':'No resource beneath the machine',
        'overlay.conveyance.status.no_water':'No water supply','overlay.conveyance.status.no_steam':'No steam supply','overlay.conveyance.status.no_ammo':'Out of ammunition',
        'recipe.conveyance.sulfuric_acid':'Sulfuric acid','recipe.conveyance.battery':'Battery','recipe.conveyance.solid_fuel':'Solid fuel',
        'research.conveyance.production_progress':'Production progress: %s / %s',
        'technology.conveyance.stronger_explosives.name':'Stronger Explosives 1','technology.conveyance.stronger_explosives.short':'Explosives 1',
        'technology.conveyance.stronger_explosives.description':'Grenades deal 25% more damage.',
        'tooltip.conveyance.module_install':'Right-click a compatible machine to install. Shift + right-click with an empty hand to manage modules.',
        'tooltip.conveyance.speed_module':'Speed +20%; energy +50%.', 'tooltip.conveyance.productivity_module':'Productivity +4%; speed -5%; energy +40%; pollution +5%.',
        'tooltip.conveyance.efficiency_module':'Energy -30%; minimum active consumption 20%.',
        'message.conveyance.accumulator':'Stored energy: %s / %s MJ', 'message.conveyance.fluid_storage':'%s: %s fluid stored locally',
        'tooltip.conveyance.accumulator':'Stores 5 MJ; charges/discharges at up to 300 kW.', 'tooltip.conveyance.storage_tank':'Stores 25,000 fluid. Prevents mixing.',
        'tooltip.conveyance.fluid_pump':'Transfers fluid towards its output while powered. Unlimited throughput.',
        'tooltip.conveyance.stat.module_slots':'Module slots: %s',
        'tooltip.conveyance.conveyor':'Two independent item lanes. Items wait at the output.',
        'tooltip.conveyance.fast_conveyor':'Two independent item lanes. Twice the standard conveyor speed.',
        'tooltip.conveyance.stat.energy_storage':'Energy storage: %s MJ',
        'tooltip.conveyance.stat.energy_transfer':'Charge/discharge limit: %s kW'})
    # Keep the authoritative technology text across runs of older generators.
    balance = ROOT/'tools/early_research_translations.json'
    if balance.exists(): lang.update(json.loads(balance.read_text()))
    lang['technology.conveyance.steel_axe.description'] = 'Hand harvesting is 100% faster. Terrain remains protected.'
    write(f'assets/conveyance/lang/{locale}.json',lang)

for name in ('fast_conveyor','fast_slope_conveyor','fast_junction','fast_inserter','steel_crate','assembler_2','steel_furnace','storage_tank','fluid_pump','accumulator'):
    write(f'data/conveyance/loot_tables/blocks/{name}.json',{'type':'minecraft:block','pools':[]})
print('Generated early-game tiers, recipes, models and translations.')
import runpy
runpy.run_path(str(Path(__file__).with_name('generate_laser_assets.py')))

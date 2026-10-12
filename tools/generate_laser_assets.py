"""Laser defences and military science, using Conveyance's native model assets."""
import copy
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'

def write(path, value):
    target = RES / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(value, indent=2) + '\n', encoding='utf-8')

def box(lo, hi, texture):
    return {'from': lo, 'to': hi, 'faces': {side: {'texture': '#' + texture, 'uv': [0, 0, 16, 16]}
        for side in ('down', 'up', 'north', 'south', 'east', 'west')}}

def crafting(name, seconds, costs, count=1, tab='combat'):
    write(f'data/conveyance/recipes/{name}.json', {'type': 'conveyance:factory_crafting', 'tab': tab, 'crafting_time': seconds,
        'ingredients': [{'item': item, 'count': amount} for item, amount in costs.items()], 'result': {'item': 'conveyance:' + name, 'count': count}})

crafting('laser_turret', 20, {'conveyance:steel_ingot': 20, 'conveyance:electronic_circuit': 20, 'conveyance:battery': 12})
crafting('military_science_pack', 10, {'conveyance:piercing_magazine': 1, 'conveyance:grenade': 1, 'conveyance:stone_wall': 2}, 2, 'science')

textures = json.loads((RES / 'assets/conveyance/models/block/gun_turret_base_0.json').read_text(encoding='utf-8'))['textures']
textures = dict(textures, lens='minecraft:block/red_concrete', pale='minecraft:block/light_gray_concrete')
shapes = json.loads((RES / 'assets/conveyance/machine_shapes.json').read_text(encoding='utf-8'))
shapes['laser_turret'] = []
pieces = [([2, 0, 2], [30, 5, 30], 'dark'), ([4, 5, 4], [28, 27, 28], 'metal'),
          ([3, 27, 3], [29, 31, 29], 'dark'), ([11, 31, 11], [21, 32, 21], 'metal'),
          ([6, 8, 3], [10, 24, 4], 'copper'), ([22, 8, 3], [26, 24, 4], 'copper'),
          ([1, 32, 1], [31, 43, 31], None)]
for part in range(12):
    offset = [part % 2 * 16, part // 4 * 16, part // 2 % 2 * 16]
    elements, solids = [], []
    for lo, hi, texture in pieces:
        a = [max(lo[i], offset[i]) - offset[i] for i in range(3)]
        b = [min(hi[i], offset[i] + 16) - offset[i] for i in range(3)]
        if any(b[i] <= a[i] for i in range(3)): continue
        if texture: elements.append(box(a, b, texture))
        solids.append([v / 16 for v in a + b])
    shapes['laser_turret'].append(solids)
    write(f'assets/conveyance/models/block/laser_turret_base_{part}.json', {'parent': 'minecraft:block/block', 'textures': textures, 'elements': elements})
write('assets/conveyance/machine_shapes.json', shapes)
variants = {f'part={part},facing={facing},lit={str(lit).lower()}':
    {'model': f'conveyance:block/laser_turret_base_{part}' if part < 8 else 'conveyance:block/machines/empty', 'y': rotation}
    for part in range(50) for facing, rotation in (('north', 0), ('east', 90), ('south', 180), ('west', 270)) for lit in (False, True)}
for name in ('laser_turret', 'laser_turret_part'):
    write(f'assets/conveyance/blockstates/{name}.json', {'variants': variants})
    write(f'data/conveyance/loot_tables/blocks/{name}.json', {'type': 'minecraft:block', 'pools': []})
head = [box([-6, 0, -5], [6, 6, 5], 'metal'), box([-5, 6, -4], [5, 10, 4], 'pale'),
        box([-4, 2, -11], [4, 7, -5], 'dark'), box([-3, 3, -12], [3, 6, -11], 'lens'),
        box([-7, 1, -3], [-6, 5, 3], 'copper'), box([6, 1, -3], [7, 5, 3], 'copper')]
write('assets/conveyance/models/block/laser_turret_head.json', {'parent': 'minecraft:block/block', 'textures': textures, 'elements': head})
write('assets/conveyance/models/item/laser_turret.json', {'parent': 'minecraft:block/block', 'textures': textures, 'elements':
    [box([1, 0, 1], [15, 3, 15], 'dark'), box([3, 3, 3], [13, 9, 13], 'metal'), box([4, 9, 4], [12, 14, 12], 'pale'),
     box([5, 10, 1], [11, 13, 4], 'dark'), box([6, 11, 0], [10, 12, 1], 'lens')]})
grey = copy.deepcopy(json.loads((RES / 'assets/conveyance/models/item/red_science_pack.json').read_text(encoding='utf-8')))
grey['textures']['body'] = 'minecraft:block/gray_concrete'
write('assets/conveyance/models/item/military_science_pack.json', grey)

write('data/conveyance/damage_type/laser.json', {'message_id': 'conveyance.laser', 'scaling': 'never', 'exhaustion': 0})
for name in ('bypasses_cooldown', 'bypasses_armor'):
    path = RES / f'data/minecraft/tags/damage_type/{name}.json'
    tag = json.loads(path.read_text(encoding='utf-8')) if path.exists() else {'replace': False, 'values': []}
    if 'conveyance:laser' not in tag['values']: tag['values'].append('conveyance:laser')
    write(f'data/minecraft/tags/damage_type/{name}.json', tag)

labels = {
 'block.conveyance.laser_turret': 'Laser Turret', 'block.conveyance.laser_turret_part': 'Laser Turret Frame',
 'item.conveyance.military_science_pack': 'Military Science Pack',
 'tooltip.conveyance.laser_turret': 'Powered defence with a rotating laser above a 2 x 2 x 2 base. Shoots over two-block walls. Batteries are used to construct it; no ammunition is needed.',
 'tooltip.conveyance.military_science_pack': 'Grey science for laser turrets and military research. Labs accept all four science types.',
 'tooltip.conveyance.stat.shot_energy': 'Energy per shot: %s kJ', 'tooltip.conveyance.stat.laser_buffer': 'Energy buffer: %s kJ',
 'tooltip.conveyance.stat.laser_input': 'Maximum recharge input: %s kW',
 'menu.conveyance.laser_power': 'Laser and electricity', 'menu.conveyance.laser_charge': 'Charge: %s / %s kJ',
 'menu.conveyance.laser_input': 'Input: %s kW (%s%% supplied)',
 'research.conveyance.cost_red_green_blue_grey': '%s red + %s green + %s blue + %s grey',
 'status.conveyance.laser.no_coverage': 'Outside power-pole coverage', 'status.conveyance.laser.no_power': 'No electricity',
 'status.conveyance.laser.charging': 'Charging for the next shot', 'status.conveyance.laser.ready': 'Ready; waiting for enemies',
 'status.conveyance.laser.firing': 'Firing', 'overlay.conveyance.status.charging': 'Charging for the next shot',
 'death.attack.conveyance.laser': '%1$s was hit by a laser',
}
techs = {
 'military_science': ('Military Science Pack', 'Military science', 'Unlock grey science made from piercing magazines, grenades and walls.'),
 'lasers': ('Laser', 'Laser', 'Develop laser weapons using battery technology and chemical science.'),
 'laser_turrets': ('Laser Turret', 'Laser turret', 'Unlock electrically powered laser turrets.'),
}
for level, modifier in ((1, 20), (2, 20), (3, 30)):
    techs[f'laser_damage_{level}'] = (f'Laser Weapons Damage {level}', f'Laser damage {level}', f'Laser damage increases by another {modifier}% of base damage.')
for level, modifier in ((1, 10), (2, 20), (3, 30)):
    techs[f'laser_speed_{level}'] = (f'Laser Shooting Speed {level}', f'Laser speed {level}', f'Lasers fire another {modifier}% faster. Each shot still consumes 800 kJ.')
research_path = ROOT / 'tools/early_research_translations.json'
research = json.loads(research_path.read_text(encoding='utf-8'))
for key, values in techs.items():
    for suffix, value in zip(('name', 'short', 'description'), values):
        name = f'technology.conveyance.{key}.{suffix}'; labels[name] = value; research[name] = value
research_path.write_text(json.dumps(research, indent=2) + '\n', encoding='utf-8')
for language in ('en_us', 'en_gb'):
    path = f'assets/conveyance/lang/{language}.json'
    translations = json.loads((RES / path).read_text(encoding='utf-8')); translations.update(labels); write(path, translations)

# Run after the complete generator chain, so placeholder icons cannot replace supplied PNGs.
import runpy
runpy.run_path(str(Path(__file__).with_name('apply_item_textures.py')), run_name='__main__')

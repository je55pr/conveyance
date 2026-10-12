# Conveyance

A standalone industrial overhaul for Minecraft Java Edition **1.20.1**, using
**Forge 47.4.26** and **Java 17**. No other mods are required.

## Version 0.52.1: supplied item textures

Steel and sulfur now use the supplied PNG icons. The iron-stick sprite has a
matching model ready for a future item. Asset generation finishes by applying
all supplied item textures, so placeholder models cannot overwrite them.
Run `python tools/apply_item_textures.py` to connect newly added item PNGs without
regenerating other assets. Filenames should match the item ID. The creative
generator retains its magenta model. Restart Minecraft for this version.

## Version 0.52.0: laser turrets and military science

Laser turrets have a 2 x 2 x 2 base with a rotating head above it, visible red
beams, 24-block range, 20 base damage and 1.5 shots per second. They fire over
two-block walls and leave repairable wrecks. No ammunition slots are needed.
Each shot consumes 800 kJ from an 801 kJ buffer; network charging is limited to
9.6 MW with a 24 kW idle drain. Charging demand and accumulator discharge are
included in the electricity network. A right-click screen shows charge, input
and weapon stats; B-mode warnings identify missing coverage/power and charging.

The existing oil -> sulfur -> sulfuric acid -> battery production chain supplies
the turret recipe: 20 steel, 20 electronic circuits and 12 batteries, taking
20 seconds to craft. Military science makes two grey packs from one piercing
magazine, one grenade and two walls in 10 seconds. Labs now have four fixed
science slots, preserving the previous red, green and blue slots.

New military science, laser, laser turret and tier 1-3 laser damage/speed research
use Factorio base 2.0.77 prerequisites, costs and times. New technologies are
appended to preserve existing research. See [the production and research guide](docs/laser-turrets.md).
Restart Minecraft; no new world or research reset is needed.

## Version 0.51.2: free B-mode camera

WASD can move the B-mode camera beyond the five-by-five-chunk building area.
Distant terrain follows the camera's ground focus as you explore. Building and
machine interaction still use the existing reach around the player. Returning
to normal view returns to your character; entering B mode starts over the player.
Restart Minecraft.

## Version 0.51.1: distant B-mode overview

B mode can zoom out to a camera distance of 4096 blocks, enough to fit the
1024-block terrain radius (roughly 2048 blocks across) on screen. Close-up zoom
and the default camera distance are unchanged. Distant terrain stays centred
on the camera's ground focus, and projection/fog distances account for camera
height. The near clipping plane increases at high zoom to preserve depth precision.
Mouse targeting skips the empty sky and traces only through the world's
height. Building reach remains five by five chunks; this adds an overview without
loading extra world chunks or increasing the terrain radius. Restart Minecraft.

## Version 0.51.0: B-mode factory overlays

Four top-right buttons toggle power coverage, production icons, warnings and
inserter directions. Recipes and important warnings start enabled. Power-pole
coverage automatically appears while hovering or placing a pole, and inserter
pickup/drop arrows appear while hovering or placing an inserter. The arrows use
blue for pickup and orange for drop, including long inserters.

Coverage follows the actual eight-block distance rule at the selected building
height. All-coverage shading merges overlapping poles. Camera-facing production
badges identify assembler/furnace output, mined resources, chemical recipes and
active lab research. Recipe badges hide when machines become too small on screen;
important warnings remain visible. Hover a badge for its name, activity and status.

The server sends bounded snapshots four times per second from already-loaded
nearby chunks while B mode is active. It distinguishes missing pole coverage,
empty electrical networks, insufficient generation, empty fuel, missing recipes,
missing deposits, water/steam shortages and empty turrets. Input shortages and
blocked outputs are available on hover without filling normal factories with
alarms. Overlays load no extra chunks and don't change machine behaviour.
Restart Minecraft. Research and existing worlds are preserved.

## Version 0.50.2: icons alongside tooltip names

Item names have icon rows in ordinary inventory, research, hotbar-link and
crafting queue tooltips. Research prerequisites, unlocks, production targets and
science costs show their corresponding icons. Recipe tooltip titles include
their output icon. Shared native tooltip components preserve colours, wrapping
and placement. Production milestones use a production-progress label. Restart Minecraft.

## Version 0.50.1: production milestone tooltips

Research graph progress uses each technology's actual target, including
production milestones with no science-pack cost. Research tooltips render after
the final button, and progress bars reject non-finite values. Restart Minecraft.

## Version 0.50.0: Factorio early research and equipment tiers

The research tree now has 44 technologies, using Factorio base 2.0.77 science
costs, research durations and direct prerequisites where the underlying content
exists. Steam power, electronics, red science, steel axe and oil processing use
production milestones; handcrafting and factory production both count.
Long inserters unlock with Automation, iron crates are starter recipes, and
turrets, walls and repair packs have separate research. Existing prototype
research resets once because the old global upgrades have been replaced.

Assembling machine 2, steel furnaces, fast inserters, fast conveyors/slopes/junctions
and steel crates are separate craftable equipment. Sulfuric acid and batteries
extend chemistry; tanks hold 25,000 fluid and powered pumps transfer between
separate input/output networks. Accumulators store 5 MJ with 300 kW charging and
discharging, prioritising factory demand and primary generation.

Tier-one speed, productivity and efficiency modules modify compatible machines.
Right-click a machine with a module to install it; Shift + right-click with an
empty hand opens its module inventory. Modules and pending bonus ore return
separately when picking up machines. Mining productivity grants bonus ore rather
than faster drills. Labs use technology-specific research time and the base
20%/30% research-speed upgrades.

Military research unlocks submachine guns and shotguns; Military 2 adds piercing
ammunition and grenades. Bullet damage and shooting-speed researches affect
handheld weapons and gun turrets. Grenades damage enemies without editing terrain.

Conveyance retains one pole type, unlimited fluid throughput, protected natural
terrain and custom slope/four-way junction footprints. Missing trains, vehicles,
armour, circuit networks and other independent systems are not represented by
empty research nodes. Art for new items is provisional. Restart Minecraft.

Balance reference: [Wube's 2.0.77 technology data](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua),
[recipes](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/recipe.lua)
and [module effects](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/item.lua).

## Version 0.49.1: compact recipe grids

Handcrafting and assembler recipe pickers use a compact icon grid without row
headings. Each item family still starts on its own row, with matching spacing
between rows and columns. Restart Minecraft.

## Version 0.49.0: research cards and direct connections

Distant terrain returns to a 1,024-block radius, including projection, fog and
cache bounds. The research screen now has a scrollable, three-column card grid on the left
and selected technology details with a local graph on the right. Only direct
prerequisites and direct dependent technologies appear, with clickable nodes
to inspect their own connections. Cards prioritise active/available research,
then locked technologies and completed research. Red means unavailable, amber
means researchable (including active research), and green means researched.
Cards show only their icon, name and science cost, with status conveyed by colour.
Progress, unlock icons and detailed item tooltips remain in the selected technology pane.
The card grid scrolls independently; the local graph retains pan/zoom/Fit.
Inspecting a technology is separate from starting it with the research button.
Existing research saves and progression are unchanged. Restart Minecraft.

## Version 0.48.3: doubled distant terrain radius

Distant terrain now reaches 2,048 blocks from the camera, twice the previous
radius and roughly four times the visible area. The projection, horizon fog,
tile scheduling, cache eviction and prediction-cache bounds follow the larger
distance automatically. Mesh uploads retain their per-frame budget and the
worker still builds nearby tiles first, so the outer landscape fills in
gradually. Restart Minecraft; existing worlds are supported.

## Version 0.48.2: matching distant lighting and water

Distant terrain uses Minecraft's live light map and the world's face shading,
replacing the approximate daylight multiplier. Day/night colour, brightness,
gamma and weather lighting now follow the same lighting texture as real chunks.
Water uses the biome tint and the native water sprite's transparency in a
separate pass, with sandy seabeds and depth-dependent skylight beneath it.
This removes the opaque bright-blue water approximation and brings the ground
and water colours closer at the handoff. Small differences from detailed tree
shadows and ambient occlusion remain in the simplified distant geometry.
Restart Minecraft; existing worlds are supported.

## Version 0.48.1: distant texture scale and section handoff

Distant surfaces repeat their atlas sprite once per block on each axis, including
the full height of cliff walls. Repetition happens in the shader with unwrapped
texture derivatives for correct mip selection, keeping the simplified geometry
and avoiding stretched 4-block textures or bleeding into adjacent atlas sprites.

The handoff now uses a coverage texture with an independent ready flag for each
16-block height section of each nearby Minecraft chunk. Uncompiled underground
or empty sections no longer keep LOD surfaces on top of finished real geometry.
Faces on chunk boundaries sample inside the owning solid, and tree canopies can
consult neighbouring chunks across LOD tile boundaries. Restart Minecraft to
load the new vertex data and shader together; existing worlds are supported.

## Version 0.48.0: distant terrain

Conveyance renders a simplified natural landscape out to 1,024 blocks in both
normal and overhead views. Ground, beaches, opaque distant water, granite
cliffs, exact surface ore/oil footprints and simplified tree silhouettes use
the existing seed and saved generation version. Factories and distant entities
are omitted. The server sends generator settings on login/respawn/dimension
changes; no extra chunks are loaded, generated, simulated or revealed on the map.

A low-priority worker builds 64 x 64 terrain tiles. Only eight completed CPU
meshes can wait for upload, render-thread uploads are limited per frame, GPU
tiles outside the surrounding view are disposed, prediction caches are pruned,
and disconnect/resource
reload cancels the worker and closes buffers. A per-tile shader mask hides the
distant representation wherever the normal chunk sections have compiled, so
normal terrain takes over as it loads. Prediction shows the natural landscape;
tree clearing and other player edits outside loaded chunks are not represented
in this first terrain-only prototype.

The projection reaches beyond the distant terrain and ordinary distance fog
starts near the far horizon rather than at the normal chunk limit. Pollution
still draws closer smog, and underwater/lava fog keeps its normal behaviour.
The distant shader applies atlas textures, directional shading, day/night
brightness and the same fog. Tiles stream in after joining or moving into a new
area. Restart Minecraft; existing Conveyance worlds use their saved terrain rules.

## Version 0.47.0: clustered biter nests

Newly discovered enemy regions have a 48% nest-site candidate chance, down from
72% (one third fewer candidate placements). Each accepted site contains 2-4
independent nests within 14 blocks of the first nest on each axis, with at least
6 blocks between centres. Sites without room for two nests are skipped. All
footprints remain on flat, dry, clear ground, outside ores, oil and tree trunks,
and outside the 144-block safe radius around spawn. Cluster footprints stay
inside their region so neighbouring sites cannot overlap.

Radar/map discovery and loaded terrain use the same deterministic cluster
prediction without loading distant chunks. Each nest keeps its own pollution
budget, defenders, health and destroyed state. Existing discovered regions
keep their saved nests; restart and explore new regions or create a new world
to see the new layout throughout. Terrain generation remains version 4.

Grass sides now use the cutout render layer so the transparent grass overlay
reveals dirt rather than drawing black over it; the asset generator retains
the setting when rebuilding models.

## Version 0.46.0: conditional starter resources and rare oil

Coal now uses pure black on the map, including with pollution enabled. New
worlds use generation version 4: random oil fields occur in 1/15 of candidate
regions, five times less frequently than version 3's 1/3. Oil wells remain
3 x 3, and no guaranteed starter oil field is added on login in any world.
Previously placed wells in older saves are not deleted.

Natural ore can now generate near spawn, outside a small protected centre.
Starter iron, copper, coal and stone are fallback guarantees. At world-rule
initialization, each type checks for a usable natural deposit within 128 blocks
with a dry walkable route from spawn. Only missing types get a fallback patch,
normally 72-112 blocks away with a preferred 56-block gap from other deposits.
The whole fallback footprint must be flat, accessible, clear of natural ores
and clear of potential oil sites. Placement relaxes spacing only if necessary;
rare rescue sites in the protected inner plain keep bad RNG from softlocking
resource access. Natural ore takes priority and is never overwritten.

The search uses predicted terrain without loading chunks, and runs once per
generator context. Results depend on the seed rather than chunk generation order.
Older generation versions keep their previous ore layouts. Create a new world
for version 4's changes; the coal map colour also applies to existing saves.

Gun turrets now have a 2 x 2 x 2 base with the rotating head above it; placement
reserves a 2 x 2 x 3 volume for the head's movement. Rendering, ghost previews,
collision shapes and the firing origin use the raised geometry. Stone walls
place two connected blocks from one item, share health across both halves,
and are picked up together when either half is broken. Wall collisions are
exactly two blocks high, and wall parts can be rebuilt from their wrecks.
Existing old turrets/walls should be picked up and placed again to get the
new full footprints; no item-state migration is needed for the prototype.

## Version 0.45.0: map contrast and sparser dry forests

Nests colour their 3 x 3 footprint bright red in the map raster, including coarse
overview pixels; the former outlined icons are removed. Clearing/discovering
nests invalidates the corresponding map colour cache. Oil is vivid magenta,
iron cyan, copper orange, coal light slate, stone cream and uranium lime.
Resource colours remain intact under the pollution overlay. Overview pixels
are at least one screen pixel so small important features remain visible.

New worlds use saved generation version 3. Random ore candidate rates are a
third of version 2: 0.2/3 per basic ore, 0.08/3 uranium. Natural oil fields also
occur in one third of their previous regions. Starter ore and the nearby oil
field remain guaranteed. Granite cliff paths are doubled to 320-544 blocks,
with a wider region search to avoid clipping long paths at generation boundaries.

Forest density follows the same seeded dryness field as grass colours: greener
wet regions retain dense pockets, average regions have around half the trees,
and the driest regions have about 90% fewer tree candidates. The starter wood
grove remains protected. Versions 1 and 2 keep their previous layout/density
rules; a new world is required for version 3's generation changes.
The dryness noise and density curve now form part of generation version 3's
layout rules and must remain stable; RGB palette colours can still change freely.

Biters currently avoid water routes (negative water pathfinding cost) and have
a FloatGoal if they fall in. They do not deliberately swim across seas. The
abstract distant-wave entry-point shortcut does not yet check the full terrain
route, so it can bypass water/cliff barriers when materializing a group.

## Version 0.44.0: playthrough terrain, water and surface colours

New worlds use saved `generation_version: 2`. Older saves without the field
decode as version 1 and keep their previous terrain, ore, river, cliff and oil
layout rules when generating more chunks. Layout changes must introduce a new
generation version and retain the old branches: do not change version 2's rules
under an existing playthrough. Colour/model improvements can change independently.
Restart Minecraft and create a new world to use this layout.

Version 2 reduces random deposit candidates by 80% (20% chance per basic ore
per 128-block region, 8% uranium), preserving the four separated starter patches.
Random ore candidates require a flat, base-level 3 x 3 centre. The guaranteed
starter lake is centred 64 blocks from spawn between ore directions, with a
roughly 40 x 32 block footprint and a usable bank about 40-50 blocks away.
The shore/shallow-water height steps and raised offshore-pump outlet are unchanged.

Rivers are removed. Seas/lakes have less fine shoreline noise, and every wet
4 x 4 cell must belong to a 2 x 2 wet group, preventing isolated single-cell
pools and single-cell-width channels. Only granite cliffs generate, with 160-272
block paths, narrow cross-sections, heights up to six three-block sections,
tapered ends and an interior gap. Large clear plains remain protected.

Oil wells are complete 3 x 3 surface footprints, excluding cliffs, water, ores
and tree trunks. The nine model tiles form one broad dark well, suitable for a
3 x 3 pumpjack. The nearby starter oil field also places three full wells.
Older single-block wells keep their legacy model. Savannah trunks now use brown
oak bark and end grain; trees still produce the same Wood item.

Grass uses a smooth seed-dependent cosmetic climate, fading from green through
gold to brown over hundreds of blocks. The map uses the same base palette.
Local pollution blends grass toward grey; active nest territories blend it
purple at the edges and red nearer nests, fading back when the nest is cleared.
These are tints, not replacement terrain blocks or separate biomes. A 17 x 17
chunk ambient packet carries pollution and nest influence. Client meshes rebuild
when visible colour bands change, including neighbouring chunks to keep blending
continuous. Existing smog and pollution-map controls retain their behaviour.

## Version 0.43.1: Creative map reveal

The Creative map has a `Reveal 2,000 × 2,000` button. It reveals roughly that
square around the character, including resource patches and predicted nests,
and zooms out to frame it. Chunk-aligned sampling can add a small border.
The reveal runs from the centre outwards, with progress on the button, up to
128 tiles and an eight-millisecond sampling budget per server tick. It continues
if the map closes, skips already revealed tiles and uses the radar's terrain
sampling without generating or loading distant world chunks. Survival has no
button, and the server also requires Creative mode. Jobs cancel if the player
leaves the world, dies or leaves Creative mode; completed tiles remain explored.

## Version 0.43.0: nests and pollution attacks

Adds shootable 350-health biter nests with a breathing placeholder model.
Natural nests use irregular seeded placement on clear, flat, dry terrain, with
a 144-block exclusion radius around the world spawn. Each 128 x 128 region has
a 72% chance to search for one suitable nest site. They avoid trees, ore, wells
and existing buildings. The same discovery path works in existing worlds;
explored or loaded regions are checked without regenerating terrain.

Nests absorb up to 2 local pollution units per second into a persistent budget,
spending 4 units per small biter. A nest can dispatch 4-8 attackers per wave,
at least 60 seconds apart, towards a nearby pollution concentration. Budgets
cap at 64 units. Unloaded nests keep their abstract pollution/budget bookkeeping
without loading chunks or simulating mobs. A distant wave travels abstractly at
4 blocks/s, then materializes on active terrain 48-96 blocks from its destination;
nearby nests spawn attackers directly. Queued waves wait if no suitable active
ground is available. Spawns check actual ground and collision. Automatic spawning
stops at 48 local or 256 loaded biters.

Approaching a nest also wakes a local group of up to two defenders, which stays
near the nest unless provoked. Missing defenders replenish at most once per ten
seconds. Long-distance attackers navigate in short waypoints. Clearing a nest
permanently removes its automatic spawning and pollution absorption; surviving
biters still need dealing with. Pistols, turrets and B-mode aiming can target
both biters and nests. Neither produces items or experience on death.

The map shows revealed nests as red outlined markers and loaded biters as red
dots. Radar scans discover distant nest sites without loading terrain; enemy
markers stay hidden in unexplored chunks. Nest hover labels include coordinates.
An incoming-wave notification appears in chat and briefly on the HUD, below the
B-mode controls when needed.

For a quick playtest with commands enabled, use `/conveyance combat nest` to
place a nest about 48 blocks ahead, then `/conveyance combat provoke` to add
pollution at the nearest nest and direct its first queued wave towards your
current location. A wave normally starts after about eight seconds if space
is available. `/conveyance combat nest_info` shows its pollution, budget and
cooldown. `/summon conveyance:biter_nest` also creates a persistent nest.
These rates and wave sizes are prototype balance; enemy evolution is not added.

## Version 0.42.0: combat and repairable wrecks

Adds a first defence prototype: custom six-legged small biters, a pistol,
firearm magazines, ammunition-fed 2 x 2 gun turrets and connecting stone walls.
There are no natural spawns or pollution attacks yet. With commands enabled,
use `/conveyance combat kit` for supplies, `/conveyance combat wave 8` to spawn
an attacking group around you, and `/conveyance combat clear` to remove nearby
biters. `/summon conveyance:small_biter` also works. Waves skip unloaded chunks
and water, so fewer enemies can appear if the surrounding terrain is unsuitable.

Pistols fire at 4 shots/s over 15 blocks; turrets fire at 10 shots/s over 18
blocks. Each magazine supplies ten 5-damage rounds. Turrets use no power,
automatically aim at visible biters and fire over stone walls. Inserters supply
up to ten magazines; right-clicking a turret with ammunition fills its slot.
Hold left click with a pistol to fire, including cursor aiming in B mode. Shots
always originate from your character, with the same range in both views.
Player combat uses Minecraft health; biters deal 2 damage to players and 7 to
buildings every half-second. They path around terrain and attack obstructing
factory buildings and walls. Native pathfinding is the prototype starting point.

Buildings have persistent health shared across their entire multiblock footprint.
Destroyed buildings become low rubble over their base; the wreck stores the
original footprint, settings, processing state, items and local fluid buffers.
Right-click with one repair pack to rebuild a wreck or fully heal a damaged
building. The original space must be clear, including players standing in it.
Picking up any part of a wreck salvages its items into your inventory and removes
the whole wreck; it does not recover the machine itself. A full inventory leaves
the wreck in place. Combat and enemy deaths never produce loose item entities.
To inspect wrecks quickly, aim at a building in normal view and use
`/conveyance combat damage 1000`.

Pistols, magazines and repair packs are starter recipes. Basic Defence costs
10 red science and unlocks walls and turrets. New recipes have a Combat tab in
handcrafting and assembly pickers. Combat models are provisional native geometry;
existing user-supplied item sprites continue to take precedence.

## Version 0.41.1: uranium deposits

New terrain can generate green uranium patches level with the base ground,
using the same irregular per-block footprints as existing ores. Uranium has a
40% chance of a deposit candidate per resource region and is excluded from the
starter resource area. Existing iron/copper/coal/stone placement keeps its seeds
and priority. Uranium appears in map resource labels and radar terrain previews.
Its natural block has no inventory item, drops or extraction/processing yet.

## Version 0.41.0: oil, chemical science and creative power

Adds surface oil wells, 3 x 3 pumpjacks, 5 x 5 refineries and 3 x 3 chemical
plants, all two blocks tall. New chunks generate non-depleting wells on flat
terrain; existing worlds receive one nearby starter field on login, with its
coordinates in chat. Oil wells are labelled on the map, including radar previews.
Pumpjacks produce 10 crude oil/s at 90 kW, with output on the front-right corner.

Refineries draw 420 kW plus 14 kW standby; chemical plants draw 210 kW plus 7 kW
standby. Both have a separate icon recipe picker and processing view using the
shared compact factory window. Solid inputs are automatically limited to twice
the selected recipe's requirements. Fixed fluid ports are active according to
the recipe, with unlimited pipe throughput and mixing prevention across all
fluid types. Fluid buffers save locally; changes to recipes refund item slots.

Basic refining: 100 crude -> 45 petroleum in 5 seconds. Chemical plants make
2 plastic from 20 petroleum + 1 coal in 1 second, or 2 sulfur from 30 petroleum
+ 30 water in 1 second. Engine units require an assembler (1 steel + 1 gear +
2 pipes, 10-second base recipe). Advanced circuits cost 2 circuits + 2 plastic +
4 copper wire with a 6-second base recipe. Two blue science packs cost 2 engines
+ 3 advanced circuits + 1 sulfur, with a 24-second base recipe.

Assemblers now have five input slots and use recipe duration at Factorio
assembling machine 1's crafting speed of 0.5, instead of a fixed two seconds.
Blue science therefore takes 48 seconds per batch before speed research.
Recursive hand crafting never manufactures engine units; existing engines can
be used to craft the final science packs. Creative crafting can give any new
solid item directly, including the chemical plant products.

Research gains Engine, Oil Processing, Plastics, Sulfur Processing, Advanced
Circuits and Chemical Science nodes. Labs now have red, green and blue slots;
each technology declares its required science combination. Advanced Oil
Processing spends all three pack types and unlocks three-output refining plus
heavy/light cracking. Advanced refining consumes 100 crude + 50 water to produce
25 heavy oil + 45 light oil + 55 petroleum per five-second batch. Refineries take
water at rear-left and crude at rear-right; front outputs are heavy, light and
petroleum from left to right. Chemical plants take primary fluid at rear-left
and water at rear-right, with fluid output at front-left.

The old generator is now a Creative Generator. It has no fuel inventory,
pollution or consumption and supplies unlimited power to its connected pole
network. Its item/recipe are Creative-only; Survival placement is rejected and
switching out of Creative removes its inventory items. Breaking one in Survival
does not grant the item. Creative metal uses magenta diffuse tint while retaining
the metal texture, with the same material applied to the item model. Existing
placed generators convert in place; their former fuel is discarded.

Inventory protocol is 14; both sides need the updated mod. Custom item sprites
are preserved when regenerating machine assets, with the Creative Generator
using its magenta model icon. No tests are added; build and client loading are
checked for this prototype.

## Version 0.40.0: Factorio base power consumption

Audited against normal-quality Factorio 2.0 prototype data and its official wiki.
Power accounting now retains fractional kW, including standby load, and shared
tooltips report the same values used by machine ticks. Base values before our
existing efficiency/speed research:

| Machine | Working consumption | Idle consumption |
| --- | --- | --- |
| Electric miner | 90 kW electricity | 0 |
| Burner miner | 150 kW fuel | 0 |
| Stone furnace | 90 kW fuel | 0 |
| Assembler (assembling machine 1) | 75 + 2.5 = 77.5 kW electricity | 2.5 kW |
| Research lab | 60 kW electricity | 0 |
| Radar | 300 kW electricity while enabled | 300 kW |
| Inserter | 4.2 kW rotation, up to 14.7 kW movement, plus 0.4 kW drain | 0.4 kW |
| Long inserter | 6 kW rotation, up to 21 kW movement, plus 0.4 kW drain | 0.4 kW |
| Burner inserter | 39 kW rotation, up to 144 kW movement | 0 |
| Boiler | Up to 1,800 kW fuel | 0 once steam storage is full |
| Offshore pump / pipes / belts / junctions / poles / crates | 0 | 0 |

Inserter pickup/drop spikes use Factorio's 5/60-second duration (4/60 for long
inserters), averaged across Minecraft's 20 Hz ticks. This matches the base power
rates and peak draw; transfer timings still use our prototype cycles, so energy
per transfer differs from Factorio. Idle drain is constant; our existing power
efficiency research affects active consumption and speed research affects inserter
movement draw. Standby consumers now create generator demand even without work.

Burners save an MJ reserve, replacing the shared generator-duration countdown.
A coal item supplies exactly 4 MJ and Wood 2 MJ, with partial final ticks using
only the remaining energy. Burner miners therefore get 26.667 active seconds
per coal and Stone furnaces 44.444 seconds before efficiency research. Burner
inserters consume energy for motion and begin with Factorio's 0.5 MJ starter
reserve. Their fuel screen shows MJ because operating time varies with motion.
Existing saved fuel countdowns convert to stored energy on load.

Steam engines and solar panels already match their 900 kW and 60 kW peak output.
The custom direct-fuel generator retains 120 kW; it has no vanilla Factorio
equivalent. Generator/steam screens also retain fractional output for small loads.
Inventory protocol is now 13 because power display values changed scale.

Sources: [Factorio 2.0.72 data](https://github.com/wube/factorio-data/tree/2.0.72/base/prototypes),
[electric system and cumulative drain](https://wiki.factorio.com/Electric_system),
and [inserter power characteristics](https://wiki.factorio.com/Inserters#Power_usage).

## Version 0.39.1: preserve build controls after closing menus

Closing inventory, map, technology or machine menus while B mode is active now
returns directly to the build screen, retaining the free mouse and camera pan,
zoom and height. The previous Forge screen-opening hook never ran when a screen
closed to null, allowing vanilla to capture the mouse for player head movement.
The null destination is now replaced before mouse capture. Exiting B mode still
restores normal POV and mouse controls.

## Version 0.39.0: water, pipes and steam power

Adds offshore pumps, six-way pipes, 3 x 2 boilers and 3 x 5 steam engines. These
are starter recipes and use Factorio 2.0's ingredients and 0.5-second crafting
times. Boilers and 1 x 2 pumps are two blocks tall; engines occupy one layer.
Break any machine part to recover its plain item and fuel separately. Fluid and
heat reserves are discarded on pickup.

Place a pump on a flat bank touching water one block below the intake. Placement
from B mode's default Y=1 snaps down onto the Y=0 shoreline. It faces the water
automatically, draws an intake pipe down into it and supplies water through the
raised blue inland port at Y=1, the same level as ordinary buildings. Pumps
need no electricity or fuel and produce up to 1,200 water/s without draining the
lake. Pipes connect horizontally and vertically with unlimited throughput.
Connections that would mix water and steam are rejected, including connections
between empty networks with incompatible machine ports.

Boilers have two blue water ports on opposite sides and one orange steam output
at the front. At full output each consumes 6 water/s and 1.8 MW of fuel to make
60 steam/s at 165 degrees C. Steam engines have orange ports at both ends and
consume up to 30 steam/s for 900 kW each. One boiler supplies two engines. Coal
contains 4 MJ and Wood 2 MJ, shared by the fuel tooltips and generator logic.
Engines throttle to electrical demand after solar generation; boilers pause when
steam storage fills. Only working boilers emit pollution (30/min at full output).

Fluid storage is 100 units per pipe/pump and 200 per boiler fluid channel/engine.
Connected storage shares its contents immediately; unloaded chunks disconnect.
Each block saves its local share so disconnecting and reloading preserves fluid.
Right-click machines for the shared compact inventory and fluid status window;
right-click a pipe with an empty hand for its network contents. Boilers accept
fuel from inserters (five-item automatic buffer) and the usual right-click fuel
fill/sweep. Blue pipe markers indicate water, orange markers indicate steam.

Balance sources: [Factorio steam power](https://wiki.factorio.com/Power_production)
and [Factorio 2.0.72 prototype data](https://github.com/wube/factorio-data/tree/2.0.72/base/prototypes).

## Version 0.38.2: radar texture fix

The wide radar dish and protruding status light now use explicit texture
coordinates within their own sprites. Powered and idle radars no longer sample
neighbouring textures from the block atlas, such as wood logs and grindstones.

## Version 0.38.1: compact idle research indicator

Idle research uses a small top-right badge containing the lab icon and **T** (or
its rebound key). Selecting a technology expands it to the research name, units,
percentage and progress bar. It collapses again when research finishes or clears.

## Version 0.38.0: overhead camera transitions

B starts eight wheel steps closer than the old distance of 48: about 17.3 blocks.
Entering and exiting use a quick 0.3-second eased camera flight, blending position,
yaw, pitch and field of view between normal POV and the overhead camera. Exiting
lands on the live normal camera position, so movement during the return is smooth.
Pressing B again during a transition reverses from the current rendered camera.

Camera bob and held-item rendering stay suppressed during the flight; the player
model appears once the camera is clear of the body. Remote placement waits until
the entry finishes. Pollution fog adjusts with the transition as well.

## Version 0.37.1: closer overhead zoom and faster panning

B mode can now zoom to a camera distance of 4 blocks, down from 18, so belt icons
can appear about 4.5 times larger at maximum zoom. WASD panning is about 2.2 times
faster at normal and distant zoom levels, with a minimum of 12 blocks per second
when zoomed close. Movement retains its smooth frame interpolation. Camera
tuning constants are grouped in `client/OverheadView.java`.

## Version 0.37.0: camera-facing belt cargo and Q toggle

Items on belts, slopes and junctions now use their inventory-icon presentation
and rotate to face the live camera. This works in first person and the overhead
view; cargo keeps its existing lane positions and transport timing.

Q clears the hovered building type when it is already selected. Repeated Q
presses on the same owned building therefore toggle it on and off. From the Q
cell, pointing at another owned type switches to it; air and unavailable types
still clear the hand. The existing first Q from a numbered shortcut clears it.

## Version 0.36.0: separate recipe picking and processing

Assemblers show either their recipe picker or their processing view. New
assemblers start without a recipe and open the picker automatically. Once a
selection is confirmed by the server, the picker closes and inputs, output and
progress appear. A gear icon in the production header (**Change recipe**) reopens
the picker; its back arrow returns to the existing recipe without changing it.

The picker uses its full compact right panel, with categories, search and grouped
recipe icons. Machine slots and processing displays are hidden during selection;
the 80-slot inventory and linked hotbar remain available. Selecting the current
recipe also returns to processing. A rejected selection keeps the picker open.
Stone furnaces retain automatic input-based smelting.

Existing saved assembler recipes remain selected. Fresh assemblers no longer
silently default to conveyors. Recipe-selection acknowledgement uses inventory
protocol 12, so both sides need the updated mod.

## Version 0.35.1: compact centered windows

Inventory/crafting and factory windows are sized to their contents and centred,
instead of expanding to fill the screen. Fuel/research machines use a 342 × 240
window; crates and Stone furnaces use 360 × 240; inventory/crafting and assemblers
use 396 × 240 (GUI pixels). All still show the full 10 × 8 inventory and hotbar.
Recipe lists scroll within their compact panels.

Only map and technology expand to the available screen area. Switching between
those tabs and inventory resizes and recentres the window, preserving search
text and the map/research view. Compact sizing is shared in FactoryWindowStyle.

## Version 0.35.0: shared factory windows

**E closes any machine or crate screen**, including the assembler recipe picker.
In overhead mode it returns to the building view. M and T still open map and
technology. Machine windows share a responsive two-panel shell: the full
**10 × 8 player inventory** and **ten linked hotbar cells** on the left, with
machine inputs, outputs, status and recipe selection on the right.

The sweep covers Stone furnaces, assemblers, labs, solid-fuel generators, burner
miners, burner inserters, and wooden/iron crates. Machine storage capacities stay
appropriate to each machine (one fuel slot, two science slots, etc.). Shift-click
outputs can use all 80 player slots, and inputs can be loaded from any of them.
Closing safely returns any carried stack to inventory or the existing return queue.

Shared implementation lives in:
- `client/FactoryWindowStyle.java`: panel, slot, progress and text colors.
- `client/FactoryWindowScreen.java`: sizing, shell, hotbar and common interactions.
- `player/PlayerStorageLayout.java`: columns, rows, spacing and storage mapping.
- `factory/FactoryMenu.java`: inventory slots, transfers, number-key links and cursor returns.

The E screen also uses the shared storage geometry and window drawing style.
Inventory protocol is now 11; both client and server need the updated mod.

## Version 0.34.2: Q refreshes its temporary selection

While the Q cell is selected, Q always picks the hovered building type, replacing
any existing temporary selection. Aiming into air or at a building with no owned
items clears it. Q from one of the 1–9 shortcuts still returns to empty hand first.

## Version 0.34.1: Q requires an owned building item

Q only picks a building when at least one matching item exists in the player's
80 inventory slots, including in Creative. Otherwise it stays in empty hand.
Using the last owned item clears the temporary selection automatically.

## Version 0.34.0: empty hand and temporary building selection

The E inventory is now **10 columns × 8 rows (80 storage slots)**. The linked
hotbar has ten cells: a permanent **empty-hand / Q** cell followed by nine item
shortcuts on **1–9**. The empty cell cannot be rebound and is selected on joining
or respawning. Scroll selection includes it.

**Q** switches from any item shortcut to empty hand. While empty, pointing at a
factory building and pressing **Q** temporarily selects that building type.
Pressing **Q** again picks the current target, or clears it if unavailable. This works in first person and the overhead view;
it requires an existing inventory stack, does not remove
the pointed-at building, and leaves permanent shortcuts untouched. The Q cell
shows the temporary item and its available count while one is selected.

Q replaces its default drop-item binding. A separately customized drop binding
is retained; explicit inventory throwing remains available. Inventory sorting,
recursive crafting, refunds and pickups use all 80 slots. Inventory network
protocol is 10, and remote-building protocol is 2.

## Version 0.33.1: smooth overhead panning

The overhead camera is aligned with north at the top (zero rotation offset).
WASD movement retains fractional coordinates and is interpolated each render
frame, so it moves smoothly between blocks. Opening a menu freezes the pan at
its current destination rather than repeating the last movement.

## Version 0.33.0: overhead building view

Press **B** to toggle a live, angled overhead camera. The character stays in the
world while **WASD** pans the camera and the mouse wheel zooms. Remote interaction
covers the 5 × 5 Minecraft chunks centred on the character's current chunk (80 ×
80 blocks). Unloaded chunks are unavailable; the camera does not generate terrain.

The default machine base is **Y=1**, just above the normal ground at Y=0. The height
buttons or **Page Up / Page Down** change that plane one block at a time; **Base
layer** resets it. Placement uses the selected plane, including when roofs or
foliage are under the cursor. Existing support and whole-footprint checks still
apply. A blue rectangle marks the permitted area; placement ghosts turn red when
blocked or beyond its boundary.

**1–9** or the clickable linked hotbar selects an item. Hold **left-click** to
place, **R** rotates the preview, and **Shift + left-click** picks up a machine or
chops a tree (0.5 seconds for factory blocks). With an empty hand, left-click also
harvests terrain in place. **Right-click** opens machines and crates or loads held
fuel; **Ctrl + right-click** collects products, with sweep transfers supported.
**C / V** copies and pastes assembler recipes; **Shift + R** turns an existing
single-block conveyor or inserter. Middle-click links the pointed-at building.

**E / M / T** still open inventory, map and technology. Closing one of those or a
machine menu returns to the overhead view. **B** or **Escape** from the building
screen returns to normal POV. The remote session only extends interaction within
its area; normal first-person reach is unchanged.

## Version 0.32.0: timed hand crafting

Survival hand crafting runs through a persistent queue. Left-click queues one
batch, right-click queues up to five, and Shift-click queues the available bulk
amount. Ingredients are reserved on queueing, so other requests cannot spend
them again. Missing intermediates are crafted as timed steps before their parent
recipe, and finished products arrive one batch at a time. You can close E and
keep building or gathering while the queue runs.

Recipes show their time per batch and the time including currently missing
intermediates. Most components, crates, inserters and belts take 0.5 seconds;
miners, labs and radars take 2 seconds; the custom generator takes 3 seconds.
Red science takes 5 seconds, green science 6 seconds, and solar panels 10 seconds.
The science and solar defaults follow the
[red science](https://wiki.factorio.com/Automation_science_pack),
[green science](https://wiki.factorio.com/Logistic_science_pack) and
[solar panel](https://wiki.factorio.com/Solar_panel) recipes. Each factory crafting
JSON has a `crafting_time` value in seconds. Assembler timing is unchanged.

The bottom-left HUD shows the current step, progress, queued items and remaining
time. E shows queue icons beneath the hotbar links, or in the inventory header
on short screens. Scroll over the icons to view more orders; right-click an icon
to cancel that order and return its unused materials and completed intermediates.
Already-delivered products remain in storage. If storage fills, a completed
product waits in the queue until space becomes available. No ground items are
created. Queues and reserved materials survive reloads and respawns.

Creative crafting remains free and instant for testing. Inventory protocol 9
requires matching client/server versions because recipe times and queue updates
are now synchronized. Existing saves work without a new world.

## Version 0.31.0: distinct machine models

Machines now have custom silhouettes inside their existing reserved footprints:
electric and burner miners use open steel frames around downward-facing augers;
stone furnaces taper into tall chimneys; research labs use faceted blue domes with
metal triangle framing. Assemblers have recessed casings and vented tops, burner
generators have compact engines and exhausts, and solar panels sit on angled frames.

Casings and bases stop short of their footprint edges, leaving visible seams
between adjacent machines. Collision and selection shapes follow the bodies and
tapered tops, rather than invisible full cubes. All multiblock reservation sizes,
placement items, pickup behaviour and miner output locations are retained.

Static geometry is split across the footprint's structural blocks and rendered
in the normal chunk mesh. Only the miners' drill assemblies render dynamically.
They spin while extraction advances, pause without fuel/power or with blocked
output, and follow research speed and partial power. Placement ghosts and item
models show the complete machines. Existing placed machines use the new visuals.

Models, clipping and collision geometry come from
`tools/generate_machine_models.py`, called by the normal asset generator.
The meshes use Minecraft textures; imported item sprites still take precedence.

## Version 0.30.0: animated inserters

Electric, long and burner inserters now pick up an item, swing their arm to the
destination carrying it, deposit it, and return empty to the input. Arms lift
slightly during the swing; long arms span their two-block reach. Placement ghosts
show the complete arm, and an arrow on the base marks the output direction.

Motion follows actual server transfer progress. Blocked destinations leave the
arm holding its item at the output, and losing electricity or fuel pauses the
stroke. Held cargo and the return phase survive saving/reloading. Burner arms
retain automatic refuelling from their input or a held fuel item.

Full cycles retain the existing rates: 2 items/s for electric inserters and
1 item/s for long/burner inserters before research. Speed research and partial
power scale the motion; fractional progress carries across cycles. Items remain
inside the transfer system and are rendered at the gripper.

## Version 0.29.1: research HUD

A compact panel at the top right shows the selected technology's icon and name,
completed research units, percentage and progress bar. It updates from the same
shared research state as the labs and technology screen. When no research is
selected, it shows **Press T to select research**, using the configured technology
key if rebound. It hides with F1 and returns to the selection prompt on completion.

## Version 0.29.0: pollution simulation, map overlay and smog

The map has a **Pollution: On/Off** button. Red tint shows pollution over explored
terrain, with stronger areas darker and hovered values shown approximately.
The setting is remembered; it only hides or shows the layer. Pollution keeps
simulating and affecting the atmosphere regardless of the map setting.

Pollution is persistent world data, measured per 16x16 Minecraft chunk. Working
factories add pollution, and once per second each cell sends 2% of its remaining
pollution to each of its four neighbours. Trees and water consume pollution;
felling trees immediately reduces their absorption. Loaded environments are
sampled periodically and saved before unloading, so cleared forests stay cleared
for the simulation. Unvisited terrain uses our deterministic generator to predict
natural absorbers without loading or generating Minecraft chunks. Tiny residual
values are discarded to keep the field sparse. There are no pollution blocks,
entities, dropped items or particles.

Full-activity rates are 12/min for burner miners, 10/min for electric miners,
4/min for assemblers and 2/min for stone furnaces, based on the
[Factorio pollution reference](https://wiki.factorio.com/Pollution).
Our custom generator emits 30/min at full output, proportional to actual demand,
and burner inserters emit 1/min. Idle, blocked and unpowered machines do not emit.
Power-efficiency research also reduces electric miner and assembler emissions.
Machine item tooltips show the rate from these same definitions. Solar panels,
electric inserters, labs, radars and belts have no direct emissions.

Prototype absorption is 0.18 units/min per standing tree and 0.006 units/min per
water column. Increasing local pollution gradually turns the sky and fog greener
and brings fog closer, with smooth transitions as you move. Night brightness and
underwater/lava fog remain intact. Visual smog starts above 5 local units and
reaches full strength at 80; spread and absorption continue in unloaded areas.
The server exposes queries and consumption for future enemy attack budgets.

Existing worlds start with zero pollution. **No new world is needed.** To preview
heavy smog immediately in a Creative world with commands enabled, use
`/conveyance pollution add 400`. `/conveyance pollution get` shows the current
chunk's level and absorption rate; `/conveyance pollution clear` removes the
world's pollution. These commands require operator permission. Map protocol 4
requires matching client/server versions.

## Version 0.28.0: organic terrain and factory clearings

Seas and rivers now follow continuous, warped noise rather than connecting every
region east/west and north/south. Coastlines are irregular, waterways vary in
direction and width, and river coverage is sparse enough to leave broad land
spans. This removes the repeated grid of roughly square islands.

Large clearings suppress both forests and raised walls. Spawn has flat, dry land
within an 88-block radius and a tree-free core within 64 blocks, with a nearby
wood grove and small lake outside the initial factory space. Dense forests,
granite ridges and rocky barriers still occur beyond the clearings.

Starter iron, copper, coal and stone patches sit roughly 45–65 blocks from spawn,
in separate directions with seed-dependent rotation and spacing. Random deposits
are excluded from the surrounding 96-block radius so they do not crowd these
four starters. The four-block terrain cells, three-block elevation steps and
one-block shallows remain.

Use a **new world** to try the complete layout. Existing generated chunks retain
their terrain; newly generated chunks use these rules.

## Version 0.27.2: dedicated inventory, map and technology keys

**E** opens Inventory/Crafting, **M** opens the Map and **T** opens Technology.
Each key chooses its own page instead of restoring the last main tab. The UI tabs
still switch between pages, and the crafting category is remembered. Pressing a
page's key again closes it; pressing another page's key switches directly there.
Shortcuts also work from factory menus, while focused search boxes accept normal
typing. Cursor items are returned safely before changing pages.

Minecraft chat moves from T to **Enter** when both chat and Technology use their
default T binding. Custom chat bindings are retained. Map and Technology bindings
are configurable under Conveyance in Controls. Inventory protocol 8 requires
matching client and server versions.

## Version 0.27.1: automatic inventory sorting

The player's 72 storage slots automatically merge matching partial stacks and
sort into resources, intermediates, logistics, production, power, science, tools
and other items. Families such as conveyors, inserters, crates and miners stay
together, with full stacks before partial stacks and empty slots at the end.
Different item tags or tool damage remain separate.

Sorting follows the visual grid in E and runs after inventory changes, including
crafting, placement, pickups and factory transfers. Holding an item on the cursor
pauses sorting until it is placed or returned, so drag operations stay stable.
Hotbar links retain their assignments and total counts; the held item is resolved
again after reordering.

## Version 0.27.0: linked hotbar and factory shortcuts

The hotbar contains item links. Each icon shows the total stock across the full
72-slot inventory, and using it automatically draws from the next stack when one
runs out. Real items stay in storage; assigning or clearing a link does not move
or discard stock. Links persist across world reloads and respawns. Creative links
can place items without stock. Normal Q/Ctrl+Q throwing still removes real items.

E shows all 72 storage slots and a separate row of nine links. Drag/click an item
onto a link, press 1–9 while hovering a stack, or Shift-click a stack to assign it.
Middle-click a crafting recipe or a placed block to link that item to the selected
hotbar position. Right-click a link to clear it. Factory menus use the same links
and total counts in their bottom row.

Crafting recipe left-click makes one batch; right-click makes up to five batches,
limited by available materials and space. Shift-left-click keeps the existing
bulk-crafting behaviour. Recursive crafting and Creative free crafting apply to
all three actions.

In the world, **C** copies an assembler recipe and **V** pastes it. Hold V and sweep
over a row to paste once per assembler, including when aiming at casing blocks.
Changing recipes returns incompatible inputs and old output to the inventory;
if those items cannot fit, the change is declined.

Right-click a furnace, generator, burner miner or burner inserter while fuel is
selected to fill its fuel slot, drawing from all matching inventory stacks.
Hold right-click and sweep to fill each machine once. **Ctrl+right-click** collects
finished furnace/assembler products or crate contents, with the same sweep
behaviour. Collections leave machine inputs and fuel in place, and any products
that do not fit remain in the machine. Inventory protocol 7 requires matching
client/server versions.

## Version 0.26.0: Factorio building footprints

Electric miners now occupy **3×3** ground blocks rather than 4×4. Their three-block
height is retained, and extraction checks the nine blocks beneath the base.
Existing miners shrink to the new footprint when their structural chunks load.

Radars now occupy **3×3**, with a base plate and a central dish. One item places the
whole structure; breaking any part returns one radar to the inventory. Placement
ghosts, support/collision checks and power coverage use the complete footprint.
Old single-block radars expand automatically when the surrounding ground is
supported and clear; crowded ones retain their original appearance and work
until they can expand or are picked up and placed again.

All other direct Factorio counterparts already matched their ground footprints.
Tooltips now show dimensions for every factory building. The solid-fuel generator,
four-way junction and height-changing conveyor use their custom footprints.
See [the sourced footprint audit](docs/building-footprints.md) for the complete list.

## Version 0.25.1: generators follow power demand

Burner generators stay idle without demand, retaining both stored fuel and the
unused energy from an already-started fuel item. Fuel consumption scales with
actual power supplied: a 60 kW load uses half the fuel rate of a 120 kW load.
Solar power is used first, with remaining demand shared proportionally across
fueled generators. Fuel energy is saved precisely across world reloads.

Miners, assemblers, labs and electric inserters only request power when they can
work. Empty inputs, missing deposits and blocked outputs no longer create idle
demand. Radars continue drawing power for scanning and map refreshes. Generator
menus show Idle or their current output in kW.

## Version 0.25.0: two-lane conveyors

Flat belts and slopes have two independent lanes, each holding four items and
moving up to four items per second. A blocked lane does not stop its neighbour.
Single side inputs automatically form curved corner belts. Rear inputs keep a
straight belt, with side inputs loading onto the near lane. Both lanes retain
their identities through corners, slopes and junctions; junctions balance input
and output ports separately for each lane.

Electric, long and burner inserters place on the nearest lane when approaching
a perpendicular belt. On aligned belts they place to the inserter's left,
including when the belt faces towards the inserter. They inspect both source
lanes for items the destination accepts; burner arms can find fuel on either.
Right-click a belt with an item to load the lane nearest the clicked point.

Belts, corner models, slopes and junctions use neutral grey surfaces, with thin
rails and lane dividers. Stuck slopes retain a small red warning mark. Tooltips
show total/per-lane throughput and the expanded buffers. Existing single-lane
belt and junction contents load into the left lane.

## Version 0.24.0: assembler recipe picker

Assembling machines now open a large recipe picker alongside their input/output
slots and the player inventory. Choose a recipe directly from category tabs,
search across categories, or scroll through the same labelled groups used by
hand crafting. The current recipe has a green outline. Hovering shows item facts,
ingredient icons and total raw resource costs.

Survival lists unlocked assembler recipes; Creative lists every compatible
recipe, and machines can run recipes selected in Creative without research.
Recipe selection is checked on the server. Changing a recipe resets production
progress and retains items already in the machine. Inventory protocol 6 requires
matching client and server versions.

## Version 0.23.2: starter supplies and uniform grassy terrain

New terrain no longer generates coarse dirt; those plains use grass over normal
dirt. Previously generated coarse-dirt blocks remain in existing chunks.

On first joining a world, each player receives a wooden crate nearby containing
**100 Stone, 100 iron ingots, 100 copper ingots, 100 coal and 100 Wood**. This also
applies once to players in older prototype saves. The crate is placed in clear
space without replacing terrain, trees or machines. It is not regenerated by
relogging or dying; its received marker is saved with the player.

## Version 0.23.1: supplied item textures

Gear, copper wire, electronic circuit and Wood now use Jess's pixel-art item
textures. Asset generation detects imported item PNGs and preserves those icons
when rerun. Placed block models remain separate. See [the texture inventory](docs/textures.md)
for the extra component, construction and GUI textures ready for future content.

## Version 0.23.0: machine facts and building previews

Item tooltips use a shared fact section sourced from machine definitions:
electricity use, generation, fuel energy, pole connection/coverage ranges,
transport rates, footprints and storage. The same facts appear in inventories,
crafting recipes and research unlocks. Research changes the effective power and
rates shown. Current fuel settings give coal/charcoal 3.6 MJ and Wood 1.8 MJ;
Burner Efficiency displays the additional usable energy. Fuel balance is retained.
Belts and slopes move four items per second; junctions support four per port,
up to sixteen with all four outputs connected.

Held construction blocks, belts and machines show translucent placement ghosts.
Multiblock ghosts include every part of the structure. Green outlines mean the
placement fits; red outlines indicate an obstruction or missing support. No
items are consumed by the preview.

Click an existing belt/slope to extend its output directly. Slope items continue
uphill at that endpoint, or downhill while sneaking. Air targeting still works.
The helper no longer falls back to stacking another slope over an existing one.

Crafting categories now contain labelled rows for miners, inserters, conveyors,
storage, furnaces, assemblers, generators, poles, radars, labs, science and tools.
Search results use the same groups. Recipe ingredients and raw costs remain as
icon rows below the item's standard description and facts.

## Version 0.22.3: larger workbench and remembered tabs

E uses the window with twelve pixels of margin on each side. The map, research
canvas and recipe grid resize with the available width and height. Map overviews
adapt their detail level to keep large windows and distant zoom views complete.
Map protocol 3 requires matching client/server versions; explored data remains.

The selected main tab and crafting category are remembered across openings and
client restarts in `config/conveyance-ui.json`. All Recipes has been removed;
the five categories remain, while search can find recipes across categories.
Hover an icon in the research unlocks/upgrades strip for its item details and,
for an upgrade, the selected technology's effect. Existing icon art is unchanged.

## Version 0.22.2: conveyor corners

The placement helper snaps to the next empty cell while flat belts follow the
player's facing direction. Turn 90 degrees to place a corner without needing
to rotate the new belt afterward. Slope routing remains automatic.

## Version 0.22.1: Creative crafting access

Creative players see every Conveyance hand-crafting recipe in E, regardless of
research. Click to receive a batch for free, or Shift-click for a full stack.
The menu refreshes when changing game mode; Survival retains its normal research
and material requirements. Creative access does not change the world's research.

## Version 0.22.0: terrain materials, ordinary pickups and slope belts

Start a **fresh world** for the terrain distinction. Natural earth, rock, sand,
bedrock and ores are terrain-only block types with no inventory items. Resource
harvesting and miners leave them in place. Ordinary inventory blocks remain
placeable and removable; trees can still be felled. Existing generated chunks
retain their old Minecraft terrain IDs. The former Excavation node now unlocks
Metal Tools for construction blocks, without unlocking terrain removal.

Factory pickups return a plain, stackable item plus every stored ingredient,
fuel item, output and held/belt/junction cargo separately. All must fit in the
72-slot inventory before the object is removed. Machines restart with empty
slots and fresh progress when placed again. Old packed factory items in player
storage are unpacked automatically; no machine state stays on the inventory item.
All factory blocks, crates and multiblock parts take **0.5 seconds** to break in
Survival, regardless of tool.

**Slope Conveyor** is a starter recipe: two belts and one gear. One block spans
one horizontal block and one vertical block. Place its base at the lower level
of the step: a low incoming belt resolves uphill flow; a high incoming belt
resolves downhill flow. Yellow arrows go up, blue arrows go down. Red slopes are
stuck: missing/multiple inputs, incompatible/multiple outlets, or blocked
headroom. Queued items are retained until the connection is resolved. A full
downstream queue pauses transport without changing direction. Leave the cell
above the slope clear. Multiple slopes can climb the world's three-block steps.

Belts are now one pixel high and twelve pixels wide. Aim into the empty cell
ahead of an existing belt and right-click with a belt item to extend a bridge;
the new flat belt follows the player's facing direction. Sneaking bypasses that snap
for flat belts. With a slope item, sneak-extension puts the base one level lower
for a descending ramp. Regular floor placement also supports descending slopes.

## Version 0.21.2: visible burner miner status

The burner miner's fuel screen shows **Mining**, **No deposit**, **No fuel**,
**No output** or **Blocked**, plus mining progress. Hover the status for the
specific setup requirement. A miner needs a belt, crate or compatible machine
touching the lower furnace-textured face; it waits without consuming fuel when
there is no output. Facing burner miners over coal can feed each other's fuel
slots. No output inventory or world drops are needed.

## Version 0.21.1: coal miner pairs and recipe material totals

Burner miners facing one another over coal can fill each other's single fuel
slots to 100. The five-item inserter target previously stopped both at five
coal, before either used its remaining fuel. Direct drill-to-drill coal output
now uses the full slot; inserters and other machine fuel targets remain five.
Collect coal when both slots fill to let the pair continue.

Recipe tooltips show two rows of icons: immediate ingredients, then **Total raw
resources** for one batch from scratch. The latter expands intermediates and
smelting, counting whole batches and reusing spare components within the chain.
For example, a circuit costs three wires and one iron ingot, or one raw iron
and two raw copper from scratch (with a spare wire). Totals describe material
cost, excluding furnace fuel. Crafting control/explanation text has been removed.

## Version 0.21.0: Survival harvesting and hand crafting

Breaking blocks sends their loot into the full 72-slot inventory. A tree or
multiblock is checked as one transaction: if its complete pickup cannot fit,
it stays intact. Factory contents remain packed with their machine, belt or
crate. Terrain harvesting before Excavation still leaves its resource in place.
Only deliberate player throws create ground items; death keeps inventory.

E crafting now has All, Intermediates, Logistics, Production, Power and Science
tabs. Unavailable recipes have a dark overlay; the count includes recursively
craftable intermediates. Ingredient costs use item icons with quantity badges.
Click crafts one batch, Shift-click crafts up to 256. Missing unlocked hand
crafting intermediates are made automatically, with batch leftovers retained.
Smelting remains a machine process and assemblers require explicit components.
For example, one iron ingot and two copper ingots make one circuit, leaving one
spare wire. All materials and inventory room are checked before committing.

Recipes use counted materials, with no nine-slot crafting-grid restriction:

- Electric miner: 10 iron ingots, 5 gears, 3 electronic circuits.
- Burner miner: 3 iron ingots, 3 gears, 1 Stone Furnace.
- Stone Furnace: 5 stone. Cobblestone is displayed as Stone.

The Stone Furnace takes 3.2 seconds for iron, copper and stone bricks, or 16
seconds for steel, before research upgrades. Two stone make one stone brick
block. Metal block recipes have been removed. Existing furnace/item IDs and
research progress stay compatible; inventory protocol 5 requires matching mods.

## Version 0.20.0: green science, steel, long inserters and solar power

The Research tab now contains **18 technologies**, with real recipe unlocks and
global machine upgrades. Red research opens automation, logistics, electric
mining, metal storage, surveying, long inserters, steel and green science.
Red+green branches open solar power, excavation and speed/efficiency upgrades.
The graph fits its actual nodes, marks pack colours and supports pan/zoom.
See [the complete tree and costs](docs/research-tree.md).

- **Green Science Pack:** one Conveyor Belt + one electric Inserter. Available
  after Green Science research, by hand or through an assembler.
- **Labs:** two separate input slots, red on the left and green on the right.
  Red research consumes one red per unit; two-colour research consumes one of
  each together. Work pauses without the complete combination, retaining both
  stock and partial progress. Base cycle: three seconds at full power.
- **Long Inserter:** reaches two blocks behind and two ahead, at the same height,
  using 15 kW. Base rate is one item/second. It works with crates, machine parts,
  belts and junctions, including across an empty gap. Empty-hand right-click
  rotates it. It follows the same automatic input targets as other inserters.
- **Steel Ingot:** unlocked by Steel Processing, made in the Industrial Furnace
  from **five iron ingots**, taking 16 seconds before speed upgrades. It uses a
  quantity-aware industrial smelting recipe, not hand crafting or vanilla
  one-input smelting. Inserters target ten iron in its input, for two batches.
- **Solar Panel:** one item places a **3x3** structure, one-quarter block tall.
  Breaking any part recovers one item. It supplies up to **60 kW** around midday
  with nine exposed cells, falling with sun angle or shade and stopping at night.
  It connects through the same poles. There is no power storage yet; fuel
  generators still burn while loaded. Recipe: four steel, four circuits, one wire.

Research upgrades apply to already-placed machinery: lab speed +25% then +50%
total; mining +25%; assembling/smelting/inserter speed +50%; electrical consumer
power use -20%; burner fuel duration +25%. Wood remains half coal's duration.
Belts/junctions keep their existing rates and connections retain unlimited power
throughput. Speed upgrades do not add new machine inventories or replacement tiers.

**Prototype progression restarts in older worlds** when loading this research
revision. Terrain, map exploration and placed factories remain. Starter recipes
still provide burners, belts, wood crates, components, generators, poles, labs and
red packs, so the new tree can be played from the beginning. Iron storage, metal
blocks, radar and mining tools now have meaningful research gates. Inventory
network protocol 4 requires matching mod versions on client and server.

Try Automation -> Logistics -> Green Science alongside Metal Storage -> Steel
Processing, then feed both pack colours to a lab and research Solar Energy.
Build with `assemble`; no tests are added or run in this prototype.

## Small automatic machine buffers

Inserters feed machine inputs only up to **two recipe batches**. An assembler
making belts (two iron + one gear per batch) receives at most **four iron and two
gears**. Furnace inputs receive two batches: two ore, four stone or ten iron for steel. Fuel inventories on
generators, furnaces, burner miners and burner inserters receive **5% of an item
stack**, which is five items with the current 100-item stack limit. Research labs
also use that five-item target because they have no crafting/smelting recipe.

Both inserter types check the target before taking cargo. From a mixed crate,
they skip ingredients already at their target and take another needed material.
Belts retain their queue order, leaving unwanted/full-input cargo available for
other inserters. Machine consumption opens space for the next automatic top-up.

Direct miner-to-machine output uses the same targets, including fuel delivery.
Crates remain bulk storage with full stacks. Manual loading can exceed the
automatic target; inserters resume filling after the stored amount falls below it.
Existing overfilled inventories are consumed normally. No tests are added or run.

## Map and automatic radar exploration

Open **E -> Map** for a shared, persistent overhead map. Drag with either mouse
button to pan; scroll to zoom around the cursor; **Here** recentres on the player.
The green cross marks your position and the white dot shows your facing. Hover
resource patches for their names; ordinary terrain has no tooltip. Black areas are unexplored. Colours
identify land, water, forests, cliffs, iron, copper, coal, stone and factory blocks.

The map zooms out eight times farther than the original view, showing roughly
6,200 blocks across and 2,460 blocks vertically at maximum panel width. A full
928x928 radar area fits comfortably. Distant views use coarser overview pages;
small resource deposits retain their colours instead of blending into the ground.
The legend lists resources only. Existing exploration data is retained; map
network protocol 2 requires matching client/server versions.

Walking reveals a 3x3 chunk area around the player, refreshing loaded terrain
every second. Revealed areas are saved with the world and shared by all players.
Clearing trees or placing factories near the player updates the map. Map data is
an overview; it does not expose a live camera or run distant machines.

The **Radar** is a single-block starter prototype, costing six iron ingots,
two electronic circuits and one gear. Place it within power pole coverage:

- Reserves **30 kW**. One generator can supply one electric miner plus one radar.
- Reveals **one chunk every two seconds** at full power, slowing with shortages.
  Scans from the centre outward across **928x928 blocks**, matching Factorio's
  default exploration distance. This is 29x29 sectors of 32 blocks, or 58x58
  Minecraft chunks (3,364 total), aligned to the radar's central 32-block sector.
- Overlapping radars skip shared discoveries and focus on unexplored chunks.
- After scanning its area, periodically refreshes already-loaded terrain there.
  Continuous nearby coverage is deferred; this remains gradual exploration.
- Cyan map markers show radar positions; outlines show their scanning bounds.
- Right-click to see local explored count and power satisfaction. A green light
  indicates power; red indicates it is stopped. Breaking returns a packed radar
  with its cursor/work preserved. Revealed terrain remains when a radar is removed.
- Runs only while its own chunk is loaded. Distant unknown map tiles are sampled
  directly from our terrain/forest rules; scanning never loads or generates chunks.

Existing worlds work. Their map starts unexplored and fills as players walk or
radars scan. Try a generator, pole and radar, then open Map and watch fog disappear.
This prototype is built with `assemble`; no tests are added or run.

## Crates, forests and Wood fuel

Forest tree-placement chances are now multiplied by 1.5. The three-block
minimum trunk separation remains, so already packed areas increase less than
sparser ones. Existing trees are unchanged; explore new chunks or use a new world.

**Wooden Crates** replace chests: eight Wood crafts one crate with 16 storage
slots (up to 1,600 items). **Iron Crates** cost eight iron ingots and hold 32
slots (up to 3,200 items). Both are single blocks, available immediately in the
E crafting panel and the factory Creative tab. Right-click to open; shift-click
to transfer stacks. Adjacent crates stay independent. Breaking a crate returns
one item with its contents packed inside. Miners output directly into them and
inserters can load/unload them. Vanilla chest items and recipes are disabled.

Newly generated land includes custom **Savannah Trees**, grouped into dense
forests, sparse fringes and occasional isolated trees. A nearby grove is centred
around X=48, Z=-24, clipped to suitable dry ground. Spawn and surface resource
patches remain free of trunks.

- Trees are 7-12 blocks tall, with single-block-wide trunks and 2-3 clear trunk
  blocks before the canopy begins. Trunks can be three blocks apart, independently
  of the terrain's 4x4 grid. Canopies overlap; you can walk beneath them.
- Break any log to fell that whole tree, before or after Excavation research.
  Each log gives one **Wood**; most goes straight to your 72-slot inventory.
  Shared canopy leaves remain attached to standing neighbours.
- Leaves are decorative, with no decay, saplings, apples or other drops.
- Wood fuels every burner machine and generator for **15 seconds**, half of
  coal/charcoal's 30 seconds. Burner inserters can take Wood from their input
  to refuel themselves. Blocked/idle burner machines still pause fuel use;
  generators burn continuously while loaded.
- Wood also works in retained vanilla furnaces at half coal's vanilla burn time.
- Trees have no ticking block entities. Generation and leaf ownership are
  deterministic across chunk boundaries and saved in ordinary block states.

Use a **new world** to find forests near spawn, or explore new chunks in an
existing world. Try chopping a tree, crafting a Wooden Crate, and feeding Wood
to a stone/ore Burner Miner pointed directly into the crate. No tests were added
or run; this prototype is built with `assemble` and evaluated through play.

## Stone extraction and burner automation

Both miners now act as **quarries** on stone, granite, andesite, diorite and other
ordinary rock, producing cobblestone while keeping the terrain intact. Ores take
priority if a footprint also touches rock, so ore drills do not mix stone into
their output. Burner/electric rates remain one item per two/one seconds.

New terrain includes irregular **surface stone patches**, flush with the Y=0
plain. A starter patch is around X=8, Z=8; more are seeded across the world.
Already-generated chunks stay as they are: use existing rock formations as quarry
sites, or explore new chunks for patches.

Miners can output straight into a **belt, junction, crate, furnace or another
machine's accepted input/fuel slot** directly in front of their controller. No
inserter or dropped cargo is needed. Blocked outputs pause mining and burner fuel
use. Put a furnace's controller or casing at the output, with its footprint
extending away from the drill. Two burner coal miners facing each other can refill
one another; adding one initial coal starts the pair.

The **Burner Inserter** is a starter recipe: **one iron ingot + one iron gear**.
It works without electricity, has one fuel slot, and moves one item per
second. When it needs fuel, it takes one available coal/charcoal/Wood from its input
before forwarding cargo. The remaining items continue to the destination.

- Works with belts, junctions, crates and machine inventories. Machine inputs and
  fuel slots are reserved; extraction takes finished output only.
- Idle, empty-source and blocked-output arms pause their fuel consumption. One
  coal provides 30 seconds of active transfer work.
- Right-click for its fuel inventory. **Sneak-right-click with an empty hand** to
  rotate; electric inserters retain ordinary empty-hand rotation.
- Breaking returns one packed arm with its fuel, held cargo and remaining work,
  including in Creative. Fuel interfaces now show remaining burn seconds.

Try **stone quarry -> crate** and **ore drill -> furnace**, then build a coal
belt with burner inserters to fuel the drills and furnaces. A burner inserter on
the furnace output can send ingots to storage before electricity is available.

## Inventory tabs and research graph

The top of **E** now has **Inventory / Crafting** and **Research** tabs. The first
keeps the 72-slot inventory and icon crafting grid. Research fills the screen
with a linked technology graph; inventory slots are hidden and inactive there.
Put down any item held by the mouse before switching tabs.

- Click a research node to inspect its prerequisites, science cost, progress and
  unlock icons. Click **Start research** in its details panel to activate it.
- Green nodes are completed; gold nodes are actively researching; muted yellow
  nodes are available; grey nodes are locked. The inspected node has a white border.
- Drag empty canvas or right-drag to pan, scroll to zoom around the cursor, and
  click **Fit** to centre the whole tree. Locked nodes can still be inspected.
- Prerequisites are enforced on the server. Changing active research retains
  consumed science progress; powered labs contribute to the selected technology.

The current tree has a completed **Basic Industry** root and four technologies:

| Technology | Prerequisites | Red packs | Unlocks |
| --- | --- | --- | --- |
| Automation | Basic Industry | 10 | Assembling machines |
| Logistics | Automation | 15 | Electric inserters and junctions |
| Electric Mining | Automation | 15 | Electric miners and iron/copper block recipes |
| Excavation | Logistics + Electric Mining | 20 | Terrain removal |

Graph positions, prerequisites, costs and recipe unlocks share a technology
definition in `Technology.java`. Earlier completed Automation research retains
its previously granted logistics/mining unlocks. For a fresh test of the branches,
use `/conveyance research reset` in a world with commands enabled.

## Icon crafting, components and red science

Crafting now uses a compact **icon grid**. Each icon has a craftable-quantity
badge; hover it for its name, batch size and ingredient counts. Click for one
batch, Shift-click for bulk crafting, and scroll/search to find recipes.

New component recipes work by hand and in assemblers:

| Input | Output |
| --- | --- |
| 2 iron ingots | 1 iron gear |
| 1 copper ingot | 2 copper wire |
| 3 copper wire + 1 iron ingot | 1 electronic circuit |
| 1 iron gear + 1 copper ingot | 1 red science pack |

Belts, poles, generators, inserters and assemblers now use these components in
their recipes. The items have distinct models made from native voxel geometry.

The **Research Lab** places as a **3x3x2** multiblock, needs **60 kW**, and has
**one red-science input slot**. Choose a technology in E's Research tab, put
red science packs in a powered lab, and it consumes one pack every **three
seconds** at full power. Inserters can feed any side; science is not extracted
automatically. Multiple labs contribute to the same world-wide research, with
brownouts reducing their speed proportionally. The lab screen shows the selected
technology, shared pack count and local processing progress.

- **Automation:** 10 red science packs. Unlocks assembling machines.
- **Logistics / Electric Mining:** 15 red packs each, after Automation.
- **Excavation:** 20 red science packs, after both branches. Unlocks terrain removal.
- Components, science, labs, generators and poles are starter recipes, so the
  first research can be completed with hand-crafted parts and hand-fed science.
- Research buttons now **select a technology** rather than buying it immediately.
  Creative crafting is still free; labs still need power and real science packs.
- Existing completed research remains completed. To replay research in a test
  world with commands enabled, use **`/conveyance research reset`**.

Try crafting a generator, pole and lab, selecting Automation, and loading coal
and ten red packs. After Automation, assemblers can automate gear and red-science
production. Logistics unlocks inserters to keep the labs supplied; Electric Mining
unlocks the larger drill, and completing both opens Excavation.

## Harvesting, expanded inventory and hand crafting

Press **E** for the new inventory in Survival or Creative. The left panel has
**72 storage slots**, including the normal nine-slot hotbar. There is no player
model, crafting grid, armor panel or offhand slot. The right panel lists unlocked
recipes, their output per batch and the number of items craftable from your
inventory. Search by name and scroll over the list. Hover a recipe for ingredient
counts. Click to craft one batch; Shift-click crafts in bulk when there is room.
Crafting-table interaction also opens this interface.

The additional 36 slots save with the player, receive ordinary item pickups and
participate in crafting. Their contents follow the usual `keepInventory` death
rule. Existing vanilla slots remain in place; all stacks still cap at 100.
Machine/crate interfaces currently show the normal player slots; use E to move
items from additional storage when supplying them manually.

Before **Excavation** research, hold left-click on an ore, rock or earth block
to harvest **one resource per second**, without removing the block or needing a
tool. Iron/copper yield raw metal, coal yields coal, rock yields cobblestone and
earth yields dirt. Sand, gravel and clay can also be collected. Resources go
straight into storage; full inventory pauses collection. Machines and other
constructed devices can still be dismantled normally. After Excavation, ordinary
terrain breaking is enabled, with normal Minecraft tool requirements.

The graph in the Research tab is a **prototype research system**,
shared by everyone in the world and saved with that world:

- **Automation:** select it, then process 10 red packs in powered research labs.
- **Logistics / Electric Mining:** require Automation, then 15 red packs each.
- **Excavation:** requires both branches, then 20 red packs processed in labs.
- In **Creative**, crafting does not consume ingredients. Research still needs
  to be selected, powered and supplied with science packs.

A new Survival player can harvest eight cobblestone, craft an Industrial Furnace,
collect coal/raw metals, smelt ingots and craft a Burner Miner. Starter recipes
also include belts, wooden/iron crates and an all-metal iron pickaxe.
The starter furnace no longer requires iron, and machines no longer depend on a
wood-based vanilla furnace recipe. Iron/copper block recipes support the electric
miner's existing ingredients after Automation is unlocked.

## Conveyor junctions

The **Conveyor Junction** is a 1x1 block with a half-block-high body. Place belts
at its four horizontal sides, at the same block Y as the junction. A belt pointing
toward it becomes an input; one pointing away becomes an output. **Green arrows
point in, yellow arrows point out.** Rotate or replace a neighboring belt to
change the layout automatically. Empty-hand right-click shows its port counts
and stored cargo.

- **1 -> 2** alternates between outputs; **1 -> 3** cycles through three outputs.
- **3 -> 1** takes one item from each ready input in turn.
- **2 -> 2** shares both input and output lanes. A single busy input is split
  across both available outputs; both busy inputs receive equal turns.
- Empty inputs and occupied output positions are skipped. A blocked output holds
  one item while available outputs keep moving; when every output is full, the
  input belts back up. Equal sharing applies while lanes are ready/available.
- Each port has one input and one output position, with stored items rendered on
  top. Cargo has no dropped item physics or despawning. Junctions require no power.
- Inserters pointing into/out of a junction also form input/output ports. They
  feed or collect its stored items directly. Separate junctions with a belt when
  chaining them; direct junction-to-junction links are not configured here.
- Cargo and round-robin positions survive saving and pickup. Breaking returns
  one packed junction item, including in Creative. Existing worlds work.

Try one input belt on the south side pointing north, with west and east output
belts pointing west/east. Feed coal into the input belt, then collect from the
two output lines with inserters and crates. The junction is also an assembler recipe.

## Burner mining, assembling and industrial smelting

Three new machines are in the **Conveyance Factory** tab. Each places with one
item, needs a clear supported footprint, and returns as one machine item when
any part is broken. Right-click any part to use its compact interface.

- **Burner Miner: 2x2x2.** One coal/charcoal/Wood slot, no electricity. Mines one
  resource every two seconds from its 2x2 footprint, keeping ore patches intact.
  Put an outward-facing belt directly in front of its controller at the bottom
  layer. Missing/full belts pause both mining and fuel consumption.
- **Assembling Machine: 3x3x2.** Requires **60 kW** and makes one recipe batch
  every two seconds at full power. Choose a factory recipe with the left/right
  buttons. Ingredient quantities are counted without a crafting grid. Up to
  four material buffers and one output slot keep its storage compact. Faint
  icons show required ingredients and quantities; hover them for details.
  Inserters supply the matching input buffers and extract only finished output.
  Changing the recipe leaves existing ingredients available to remove manually.
- **Industrial Furnace: 2x2x3.** Coal/charcoal/Wood powered, with one input slot, one
  fuel slot and one output slot. Uses existing smelting recipes and cooking times,
  including raw iron/copper to ingots. Inserters can feed input and fuel through
  any side and extract finished output. Full outputs pause processing and fuel use.
- Burner machines get **30 seconds of active work** per coal/charcoal, or 15 per Wood. Fuel,
  ingredients, output, selected recipe and progress survive normal saving/pickup.
- **Every item and block has a maximum stack size of 100**, including tools,
  buckets and machine items. Matching metadata is still required to combine
  stacks: different damage, names or packed machine contents remain separate.
  Standard containers also accept 100 per slot; equipment-specific slots retain
  their own placement rules. Belts still carry individual items in their queue.

Assembler recipes currently cover Conveyance's factory items. Their costs are
read from existing recipe data, without requiring any spatial arrangement.
The player's crafting interface uses the same quantity-based approach.

Try **burner miner -> belt -> inserter -> industrial furnace**, manually add coal,
then use output inserters to send ingots to an assembler set to Conveyor Belt.
Supply two iron and one gear per batch, provide pole coverage, and extract
eight belts per batch. Existing prototype worlds work; no migration is performed.

## Direct miner output and single-slot generators

Miners have no inventory or inventory screen. Put a belt directly in front of
the controller's furnace face, at the miner's bottom layer, with its arrow pointing
away from the miner. Ore feeds straight into the belt queue. A missing or full
belt pauses mining; items are never dropped into the world. Right-clicking any
miner part shows power status.

Generators have **one fuel slot**, accepting one stack of coal, charcoal or Wood.
Right-click to open the compact screen; shift-click moves fuel between that slot
and your inventory. Inserters can still feed the generator automatically.
Old miner inventories and extra generator slots are discarded in this prototype.

Try **miner -> belts -> inserter -> crate**, with a fueled generator and pole
coverage. Existing worlds can be used.

## Automatic power networks

The **Conveyance Factory** Creative tab includes a Power Pole and Burner
Generator. Existing factory worlds work; miners and inserters now require power.

- One Power Pole item places a **1x1x4** structure. Breaking any part returns one
  pole item and removes the whole structure, including in Creative.
- Poles automatically form networks within **24 blocks**, with visible overhead
  wires. Connections have unlimited throughput. Adding generation increases
  available power across the entire connected network.
- Machines and generators connect automatically within an **8-block radius** of
  a pole's base. Distance includes height; for miners, any part of the footprint
  can be within range. Coverage overlap counts each machine and generator once.
- A Burner Generator supplies **120 kW**. Right-click it to open its fuel inventory
  and add coal, charcoal or Wood. Coal/charcoal burns for **30 seconds**, Wood for 15, while loaded,
  including when the network is idle. Inserters can supply fuel automatically.
- Each connected miner reserves **90 kW** and each inserter **15 kW** in this
  prototype, including when blocked or idle. Conveyors run without power.
- Network satisfaction scales machine speed proportionally: **50% supply means
  50% speed**. With no generation or coverage, machines stop and retain their
  fuel, held item and partial work. Power is shared fairly across consumers.
- Right-click any pole part with an empty hand for supply, demand, satisfaction,
  pole count and machine count. Inspecting a miner, opening a generator, or rotating an
  inserter, also shows its network status above the hotbar.
- Generator fuel and remaining burn time survive breaking/replacing the generator.
  Network connections rebuild automatically after loading a world. This iteration
  uses **loaded chunks only**; distant factories do not keep chunks loaded.

To try it, place a pole within 8 blocks of both a generator and your factory line,
then add coal. **One miner plus two inserters uses exactly one generator's 120 kW**.
Add another covered miner to see the network slow down; add a second fueled
generator to restore full speed. Extend the network with poles up to 24 blocks
apart, with an additional pole near each remote group of machines.

## Factory and stored conveyor queues

The **Conveyance Factory** Creative tab contains a Miner, Conveyor Belt and
Inserter. Existing ore worlds can be used for this factory iteration.

- One Miner item places a **4x4x3** machine, extending away from its controller
  corner according to your facing. It needs clear space on level, solid ground.
- At full power the miner produces one raw iron, raw copper or coal per second
  from ore blocks beneath its footprint. Patches remain intact.
- The miner outputs directly to a belt in front of its controller. Right-click
  any part with an empty hand to see power status.
- Breaking any part dismantles the whole miner and returns **one machine item**,
  preserving its partial work. This also works in Creative for prototyping.
- Conveyor arrows indicate travel direction. Each belt stores four individual
  items in one lane. Items move between belts directly, including around corners,
  with no dropped item entities, physics or despawning. Blocked outputs make the
  line queue back; the front item waits until an inserter or the next belt accepts it.
- Right-click with a resource item (such as coal or raw iron) to load one item
  directly. In Creative this copies one item; in Survival it takes one from your
  hand. Inserters can also load building blocks from a crate.
- Belt queues survive saving/reloading. Breaking a belt returns one belt item
  with its cargo packed inside; placing it restores the queue, including in Creative.
  Loose item drops left over from 0.8.0 can be picked up and loaded by right-clicking.
- Inserters take one item from the block behind them and deliver it to the block
  in front. They work with crates, furnace inventories, generator
  fuel inventories and the front of belt queues. A full destination makes the inserter hold its item.
  Keep the line at the same height for this first version.
- Right-click a belt or inserter with an empty hand to rotate its direction.

Try **miner -> belts -> inserter -> crate**. Use a 0.7.0 ore world or
create a new Creative world, place a miner over a patch, and build the line beside
its bottom layer. Models, throughput and recipes are placeholders for playtesting;
ore depletion and inserter animations are not implemented yet.

## Terrain and surface resources

New worlds offer one locked **Conveyance** preset. The generator starts from its
own terrain rules, without vanilla carving, decoration, ores, vegetation,
structures, or creature generation.

- The world is 64 blocks high: buildable blocks run from Y=-32 through Y=31.
- Iron, copper, coal and stone form irregular per-block patches on the Y=0 plains.
  They replace the surface blocks without changing elevation, leaving a level
  platform for miners. Patches of all four resources are near spawn;
  additional patches are seeded across the world and clipped to dry, flat land.
- Broad seas use warped, continuous noise. Sparse winding river contours vary
  in direction and width rather than forming a grid; a small lake is near spawn.
- Water reaches Y=-1, with its surface at Y=0. A low sandy shore makes the walk
  from base ground into the shallows occur in one-block steps.
- Shoreline water is one block deep, with its highest seabed block at Y=-2.
  Deeper beds continue at Y=-5, -8, -11... Water cells keep 4x4 footprints;
  the shallow fringe is an intentional exception to three-block vertical steps.
- Every 4x4 footprint has a flat surface, with elevation in three-block steps.
- Plains stay at ground-block Y=0. Elevation belongs to winding, narrow wall
  features, with base-level ground around them instead of raised fields.
- Low ridges now expose granite and rise one to three sections (3-9 blocks).
  Rocky walls have exposed stone/andesite faces and rise up to six sections
  (18 blocks). Their crests vary, with tapered ends and gaps through the barriers.
- All raised terrain uses rock on its sides and top. There are no dirt or grass
  caps on the raised features; dirt/grass remain on the base plains.
- Spawn has an 88-block-radius flat, dry area with a 64-block-radius tree-free
  core. Large clearings across the world also suppress cliffs and forests.
  Players stand at Y=1 on the plain's Y=0 ground.
- Ground is solid to a three-block bedrock layer at Y=-32. The base contains
  33 blocks / 11 three-block layers of ground. Mountains leave at least 13 blocks
  of building space above them. Horizontal generation remains effectively infinite.
- The temporary palette is stone, granite, andesite, dirt, sand and grass tinted by a dry
  **Alien Savannah** biome. These are placeholders for testing geometry.
- World seeds still vary the terrain. Saving/reloading retains the generator.

Use a **new Creative world** to fly around and test foundations and resource
patches and forests. Tree chopping supplies Wood for crates and starter fuel.
Use a fresh world for this revision; changing the generator and dimension bounds
does not migrate the terrain or player position saved by earlier prototypes.

## Stripped-back baseline

Building, mining, crafting, furnaces, physical containers, ordinary rails, boats,
and tools remain. Their vanilla items are available in Creative even when this
prototype does not generate their resources.

Removed from normal play:

- All vanilla creatures, including animals, fish, villagers, monsters, bosses,
  allays, and golems. Players and decorative armor stands remain.
- All vanilla generated structures, including villages, mineshafts, ancient
  cities, temples, shipwrecks, strongholds, and ruined portals. Dungeon rooms,
  desert wells, fossils, starting bonus chests, and naturally occupied bee nests
  are also disabled.
- Nether and End travel, portal creation, teleporting items, and magical storage.
- Hunger, XP orbs, levels, and their HUD elements. Slow natural health
  regeneration remains; saturation-based rapid healing is disabled.
- Enchanting, enchanted books, brewing, magical potions and food, beacons,
  conduits, totems, sculk, Netherite, and fantasy Nether/End materials.
- Vanilla redstone circuitry and devices, including buttons, pressure plates,
  pistons, hoppers, dispensers, observers, and powered/detector/activator rails.
- Vanilla status effects on players and vanilla advancement progression.
- Anvil/grindstone interfaces and fishing, pending future industrial replacements.

Ordinary water bottles, glass bottles, cauldrons, and minerals such as lapis,
quartz, amethyst, basalt, and obsidian remain available as building blocks for
future systems. Some retained resources currently have no Survival source because
their creatures or dimensions are gone; this release does not add replacements.

Disabled content is removed from Creative tabs and search, blocked from use,
and removed from carried inventories, equipment, and opened containers. Equipment
is retained with its enchantments stripped. Disabled dropped items and XP orbs
cannot enter the world. Recipes are suppressed through data and a server filter,
including Forge's overrides and recipes restored by `/reload`.

**Start a new test world for this version.** Already-generated terrain and
structures are not erased from existing saves. Existing creatures are blocked
when loaded, and disabled items are removed rather than refunded. Nether and End
remain registered for save compatibility, but entry is blocked; a player saved
there is returned to the Overworld on their next tick.

Install the mod on both the client and server for multiplayer.

## Run the development game

Close the previously launched development game, then from PowerShell in this folder:

```powershell
powershell -ExecutionPolicy Bypass -File .\dev.ps1 runClient
```

Open **Mods** to find **Conveyance 0.38.1**, then use an existing ore world or create a
**new Creative world**.
The only world type should be **Conveyance**; normal and hidden alternate preset
lists contain only that preset. Seed entry and game mode remain available.
Structures and bonus-chest generation are forced off. Minecraft marks custom
world generation Experimental; proceed with the standard confirmation for this
new test world if shown.

Look around near spawn for iron, copper and coal patches, and granite/rock walls, then follow their routes and
gaps and nearby water. F3 identifies the feature, ground height and water
depth. Try walking from dry ground through the sandy shore into shallow water,
then look for the three-block drop to the next shelf. Try building a large
foundation and climbing across terrace boundaries. Dig down to check the solid
fill; there should be no caves or underground ore veins. Save and reopen the world before exploring
more chunks to check continuity.

In a Creative world, search for pistons, enchanting tables, spawn eggs, and
ender pearls; they should be absent. Ordinary building blocks and tools remain.
With commands enabled, `/summon minecraft:cow` should fail, XP should stay zero,
and a supplied magical item should disappear on the next player tick.

## Build an installable jar

```powershell
powershell -ExecutionPolicy Bypass -File .\dev.ps1 assemble
```

The resulting file is `build/libs/conveyance-1.20.1-0.38.1.jar`. Put it in the
`mods` folder of a Minecraft 1.20.1 instance with Forge 47.4.26 installed. Replace
the previous Conveyance jar; do not install both versions together.

The development helper uses the local Java 17 JDK in `.tools/java/` when present,
without changing your system Java settings. On another computer, install a
Java 17 JDK and set `JAVA_HOME` before running it. IntelliJ IDEA can also open
this folder as a Gradle project; use Java 17 as its Gradle JVM.

## Prototype workflow and data generation

This revision is built with `assemble` and evaluated through play. The older
integration suite remains in `src/gametest/` but is not updated or run during
these prototype iterations. Its terrain expectations describe version 0.3.0.
Test classes and templates are excluded from the installable jar.

For a new dedicated server, use `level-type=conveyance:terraced` in
`server.properties`. The default `minecraft:normal` alias also uses this generator;
other generators are rejected when creating a new world. Existing saves keep
their stored generator.

Checked-in JSON is generated from Mojang's unmodified 1.20.1 client jar with
`tools/generate_baseline.py`; `tools/baseline-report.json` records its coverage.
Normal builds need neither Python nor a manually supplied Minecraft jar. The
NBT test template can be recreated with `tools/generate_test_fixture.py`.

The Gradle wrapper comes from the Forge MDK. Its supplied Forge license is
preserved in `gradle/FORGE-MDK-LICENSE.txt`.

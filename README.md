# Conveyance

A standalone industrial overhaul for Minecraft Java Edition **1.20.1**, using
**Forge 47.4.26** and **Java 17**. No other mods are required.

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
  platform for future miners. Patches of all three resources are near spawn;
  additional patches are seeded across the world and clipped to dry, flat land.
- Broad seas are selected by a seeded regional mask. Winding river channels
  join shared nodes and merge into those seas; a connected river is near spawn.
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
- Spawn has a 32x32 level area, with both kinds of wall nearby for quick feedback.
  The seed rotates those examples and places more winding walls across the world.
  Players stand at Y=1 on the plain's Y=0 ground.
- Ground is solid to a three-block bedrock layer at Y=-32. The base contains
  33 blocks / 11 three-block layers of ground. Mountains leave at least 13 blocks
  of building space above them. Horizontal generation remains effectively infinite.
- The temporary palette is stone, granite, andesite, dirt, coarse dirt, sand and grass tinted by a dry
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

Open **Mods** to find **Conveyance 0.20.0**, then use an existing ore world or create a
**new Creative world**.
The only world type should be **Conveyance**; normal and hidden alternate preset
lists contain only that preset. Seed entry and game mode remain available.
Structures and bonus-chest generation are forced off. Minecraft marks custom
world generation Experimental; proceed with the standard confirmation for this
new test world if shown.

Look around near spawn for iron, copper and coal patches, and granite/rock walls, then follow their routes and
gaps and the nearby river. F3 identifies the feature, ground height and water
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

The resulting file is `build/libs/conveyance-1.20.1-0.20.0.jar`. Put it in the
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

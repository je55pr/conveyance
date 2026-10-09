# Factorio early-game technology audit

Audit date: 9 October 2026. Comparison target: Conveyance 0.19.3, Minecraft 1.20.1.

The main list uses **Factorio 2.0.77 base game**, the stable release reported by the [official release API](https://factorio.com/api/latest-releases). Experimental 2.1.21 is excluded. Optional Quality/Space Age changes are noted separately.

I filtered Wube's 193 base technology definitions for a nonempty science-pack cost containing only automation/red and/or logistic/green packs. This produces **61 technologies: 15 red-only, 46 red+green, no green-only**. All 61 also have prerequisites reachable without spending another science colour; milestone triggers are handled separately. Counts, unit times, prerequisites and effects come from the [version-pinned official definitions](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua). The accompanying CSV contains those fields for every listed technology.

**Cost notation:** 200 R+G means 200 red **and** 200 green, not 200 packs combined. These are each technology's own costs; prerequisite research, lab-speed modifiers and productivity are not included.

**Comparison policy:** Present means accessible factory gameplay exists in Conveyance. Partial means a meaningful subset exists. Covered differently means our design already addresses the purpose without the same unlock/tier. Missing includes systems deliberately deferred. Stock Minecraft items available only in Creative are not counted as completed factory progression.

Conveyance currently has four real research nodes, all red-only: Automation, Logistics, Electric Mining and Excavation. Red packs, powered labs, burners, electric mining, stored belts, fair junctions, both inserter power types, smart input buffers, crates, shared exploration and automatic pole networks are already implemented. Most starter factory items have no research gate yet.

## Most useful next additions

1. **Green science and multi-pack labs/research.** This creates a reason to automate belts and inserters and expands the current four-node progression. A green item alone is insufficient: labs need to consume the required pack combination per research unit.
2. **Underground belts, longer-reach inserters, user-selected filters and splitter priorities.** These improve factory layouts immediately. Our junctions already balance inputs and outputs; that capability does not need reimplementation.
3. **Steel and production upgrades.** Add a steel intermediate and meaningful assembler/furnace upgrades; steel storage is optional rather than a prerequisite for having usable storage.
4. **Factory lighting, paving and landfill tools.** These make building and navigating large factories easier before combat or fluids are introduced.
5. **Research/mining efficiency or speed progression.** Lab speed, bonus mining output and modules add reasons to improve an established production line. Factorio's module branch depends on oil/plastics/advanced circuits; changing that order here would be a deliberate design choice.

The full red/green stage also includes cars/trains, oil chemistry, solar/storage, defenses and wearable equipment. Fluids and enemies can remain deferred while we develop the production/logistics branch. Pole throughput already follows our intended unlimited design; there is no need to add throughput tiers. Equipment-grid research also needs a decision because our inventory deliberately removes armor slots.

## Complete science-funded list

### Red-only research (15)

| Technology | Cost | Unlock / benefit | Conveyance status and gap |
|---|---:|---|---|
| [Electric mining drill](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L107) | 25 R | Electric ore/stone miner | **Present.** Electric Mining already unlocks our 4x4x3 miner. |
| [Automation](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L1912) | 10 R | Assembler 1 and long-handed inserter | **Partial.** Assembler exists; long-handed inserters do not. |
| [Logistics](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L2101) | 20 R | Underground belts and splitters | **Partial.** Junctions cover splitting/balancing; underground belts, player filters and priorities are missing. |
| [Fast inserter](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L2081) | 30 R | Fast inserter | **Partial.** Electric arms are faster than burner arms, but there is no faster electric variant or dedicated research. |
| [Steel processing](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L1981) | 50 R | Steel plates and steel chest | **Missing.** No steel material, steelmaking recipe or steel crate. Iron crates are already available. |
| [Logistic science pack](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L1958) | 75 R | Green science pack | **Missing.** Only red packs exist. Labs and research costs currently support a single pack type. |
| [Radar](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L147) | 20 R | Radar exploration and nearby live coverage | **Partial.** Exploration and map exist; continuous coverage is deferred. Radar is a starter item. |
| [Lamp](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L2217) | 10 R | Electric lamp | **Missing.** No powered factory lighting item or recipe. |
| [Repair pack](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L127) | 25 R | Repair damaged structures | **Missing / combat.** No machine health, damage or repair item yet. |
| [Stone wall](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L2549) | 10 R | Stone wall | **Partial / combat.** Ordinary blocks can form barriers; no dedicated wall recipe, wall health or defensive behavior. |
| [Gun turret](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L2281) | 10 R | Gun turret | **Missing / combat.** No turrets, ammunition loading or targeting. |
| [Military](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L2025) | 10 R | Submachine gun, shotgun and shotgun shells | **Missing / combat.** No industrial firearm or ammunition progression. |
| [Heavy armor](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L2261) | 30 R | Heavy armor | **Missing / equipment.** Our inventory has no armor interface; protection needs a deliberate player-equipment design. |
| [Physical projectile damage 1](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L167) | 100 R | Bullet/shell damage +10%; turret-specific bonus +10% | **Missing / combat.** No firearm, gun-turret or combat upgrade system. |
| [Weapon shooting speed 1](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L236) | 100 R | Bullet and shotgun firing speed +10% | **Missing / combat.** No weapon rate upgrade system. |

### Red + green: production, science and upgrades (11)

| Technology | Cost | Unlock / benefit | Conveyance status and gap |
|---|---:|---|---|
| [Automation 2](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L1937) | 40 R + 40 G | Assembler 2 | **Missing.** No faster assembler tier, module sockets or fluid crafting. |
| [Advanced material processing](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L2382) | 75 R + 75 G | Steel furnace | **Missing.** One burner furnace exists; no faster furnace progression. |
| [Engine](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L2446) | 100 R + 100 G | Engine unit | **Missing.** No engine intermediate or engine-based recipes. |
| [Lab research speed 1](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L2302) | 100 R + 100 G | Lab research speed +20% | **Missing.** Labs take three seconds per pack at full power; no speed research. |
| [Lab research speed 2](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L2326) | 200 R + 200 G | Another +30% lab research speed | **Missing.** No second lab-speed level. |
| [Chemical science pack](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L2594) | 75 R + 75 G | Blue science pack | **Missing / later science.** No chemical science or multi-colour research. This unlock itself costs only red and green. |
| [Modules](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L4734) | 100 R + 100 G | Foundation for module research; no direct recipe unlock | **Missing.** No module sockets or machine modifiers. |
| [Speed module](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L4751) | 50 R + 50 G | Speed module 1 | **Missing.** No speed modules or associated power trade-off. |
| [Productivity module](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L4829) | 50 R + 50 G | Productivity module 1 | **Missing.** No bonus-output modules or their operating trade-offs. |
| [Efficiency module](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L4907) | 50 R + 50 G | Efficiency module 1 | **Missing.** No modules that reduce machine power use. |
| [Mining productivity 1](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L5212) | 250 R + 250 G | Mining productivity +10% | **Missing.** Infinite patches exist, but there is no researched bonus yield/throughput. |

### Red + green: logistics and factory control (8)

| Technology | Cost | Unlock / benefit | Conveyance status and gap |
|---|---:|---|---|
| [Logistics 2](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L2494) | 200 R + 200 G | Fast belts, fast underground belts and fast splitter | **Missing.** One conveyor speed and one junction type exist; no fast transport tier. |
| [Bulk inserter](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L1697) | 150 R + 150 G | Bulk inserter; initial bulk carrying bonus | **Missing.** Both of our inserter types carry one item; no bulk arm. |
| [Inserter capacity bonus 1](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L1725) | 200 R + 200 G | Bulk inserter carrying capacity +1 | **Missing.** No researched carrying-capacity bonuses. |
| [Inserter capacity bonus 2](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L1750) | 250 R + 250 G | Normal and bulk carrying capacity +1 each | **Missing.** No multiple-item normal inserters or capacity upgrades. |
| [Circuit network](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L5380) | 100 R + 100 G | Industrial signals, combinators, power switch, speaker, display and iron sticks | **Missing.** No factory control network, sensors or programmable controls. This would be our own industrial system, separate from the removed vanilla redstone. |
| [Railway](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L2125) | 75 R + 75 G | Rails, locomotive, cargo wagon and iron sticks | **Missing.** No factory train system or progression recipes. Creative-only stock Minecraft rails/minecarts do not count. |
| [Automated rail transportation](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L2161) | 200 R + 200 G | Train stops, rail signals and chain signals | **Missing.** No train schedules, stations or signaling. |
| [Automobilism](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L2193) | 100 R + 100 G | Car | **Missing.** No industrial vehicle, vehicle fuel inventory or cargo transport. |

### Red + green: power, terrain and inventory (7)

| Technology | Cost | Unlock / benefit | Conveyance status and gap |
|---|---:|---|---|
| [Solar energy](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L2237) | 250 R + 250 G | Solar panel | **Missing.** Only fuel generators produce electricity. |
| [Electric energy distribution 1](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L2350) | 120 R + 120 G | Medium/big poles and iron sticks | **Covered differently.** Automatic poles and unlimited connection throughput already exist. Larger reach/coverage could be optional research, without cable throughput tiers. |
| [Electric energy accumulators](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L3746) | 150 R + 150 G | Accumulator | **Missing.** No stored electrical energy or charge/discharge behavior. |
| [Concrete](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L2406) | 250 R + 250 G | Concrete/refined/hazard paving and iron sticks | **Missing.** No factory paving recipes, area-placement tool or movement bonuses. |
| [Landfill](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L2470) | 50 R + 50 G | Landfill | **Partial.** Players can place ordinary blocks in water, but there is no landfill item, area tool or research. |
| [Toolbelt](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L2526) | 100 R + 100 G | Player inventory +10 slots | **Covered differently.** We start with 72 slots. Extra capacity is already part of the default inventory; no separate upgrade. |
| [Cliff explosives](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L2892) | 200 R + 200 G | Cliff demolition charges | **Partial.** Excavation allows manual terrain removal; no area demolition consumable. Space Age moves this technology later. |

### Red + green: oil, fluids and materials (9)

| Technology | Cost | Unlock / benefit | Conveyance status and gap |
|---|---:|---|---|
| [Fluid handling](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L4544) | 50 R + 50 G | Tanks, pumps, barrels and barrel filling/emptying | **Missing / fluids.** Fluids were deliberately deferred. No pipes, fluid inventories or pumping system. |
| [Oil gathering](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L4572) | 100 R + 100 G | Pumpjack | **Missing / fluids.** No crude-oil deposits or extraction. |
| [Plastics](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L4714) | 200 R + 200 G | Plastic bars | **Missing / fluids.** No petrochemical material chain. |
| [Sulfur processing](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L4690) | 150 R + 150 G | Sulfur and sulfuric acid | **Missing / fluids.** No chemical plants or sulfur chain. |
| [Advanced circuit](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L2996) | 200 R + 200 G | Advanced circuit | **Missing / fluids.** Only the basic electronic circuit exists; no red circuit or plastic input. |
| [Battery](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L3872) | 150 R + 150 G | Battery intermediate | **Missing / fluids.** No battery material or sulfuric-acid production. |
| [Explosives](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L2868) | 100 R + 100 G | Explosives intermediate | **Missing / fluids and combat.** No chemical/explosive materials or production chain. |
| [Flammables](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L2920) | 50 R + 50 G | Prerequisite for flame weapons; no direct recipe unlock | **Missing / fluids and combat.** No flammable-fluid or flame-weapon progression. Flamethrower research itself also needs military science. |
| [Fluid wagon](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L3046) | 200 R + 200 G | Fluid wagon | **Missing / fluids.** Neither train freight nor fluid storage/transport exists. |

### Red + green: defense and player equipment (11)

| Technology | Cost | Unlock / benefit | Conveyance status and gap |
|---|---:|---|---|
| [Military 2](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L2053) | 20 R + 20 G | Piercing ammunition and grenades | **Missing / combat.** No advanced ammunition or throwable explosives. |
| [Military science pack](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L2621) | 30 R + 30 G | Military science pack | **Missing / combat.** No military science. The technology qualifies even though it unlocks another science colour. |
| [Stronger explosives 1](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L295) | 100 R + 100 G | Grenade damage +25% | **Missing / combat.** No grenade or explosion damage research. |
| [Physical projectile damage 2](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L201) | 200 R + 200 G | Another +10% bullet/shell and turret-specific damage | **Missing / combat.** No second damage level. |
| [Weapon shooting speed 2](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L265) | 200 R + 200 G | Another +20% bullet and shotgun firing speed | **Missing / combat.** No second firing-rate level. |
| [Gate](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L2569) | 100 R + 100 G | Gate | **Missing / combat.** No automatic industrial gate. |
| [Modular armor](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L3394) | 100 R + 100 G | Modular armor with equipment grid | **Missing / equipment.** No wearable equipment grid; this needs a design decision alongside the removed armor UI. |
| [Solar panel equipment](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L4381) | 100 R + 100 G | Portable solar panel | **Missing / equipment.** No portable generation or equipment grid. |
| [Battery equipment](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L4343) | 50 R + 50 G | Personal battery | **Missing / equipment.** No wearable energy storage or equipment grid. |
| [Night vision equipment](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L4280) | 50 R + 50 G | Night-vision equipment | **Missing / equipment.** No wearable powered equipment or night-vision system. |
| [Belt immunity equipment](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L4299) | 50 R + 50 G | Belt-immunity equipment | **Missing / equipment.** Our prototype belts do not push players; this is low priority unless that behavior is added. |

## Milestone technologies that also belong to this stage

These five technologies consume **no science packs** in Factorio 2.0.77. They are included because omitting them would hide opening-game and oil-chain capabilities. Their prerequisites are reachable within the red/green stage.

| Technology | Trigger | Unlock / benefit | Conveyance comparison |
|---|---|---|---|
| [Steam power](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L17) | Craft 50 iron plates | Pipes, underground pipes, offshore pump, boiler and steam engine | Electricity exists via burner generators; a steam/fluid power plant is deferred. |
| [Electronics](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L52) | Craft 10 copper plates | Wire, electronic circuits, labs, inserters and small poles | Core capabilities exist. Electric inserters are gated under our Logistics research instead of this milestone. |
| [Automation science pack](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L87) | Craft one lab | Red science pack | Already a starter recipe; no milestone condition. |
| [Steel axe](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L2005) | Craft 50 steel plates | Character mining speed +100% | No global mining-speed research. Excavation unlocks removal rather than this speed bonus. |
| [Oil processing](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua#L4592) | Mine crude oil | Refinery, chemical plant, basic refining and petroleum-gas solid fuel | Oil/fluid chemistry is deferred. Its pumpjack research is Oil gathering, already in the main list. |

## Optional expansion changes

- **Quality module** is an additional **500 red + 500 green** technology when the Quality mod is enabled. It unlocks the first quality module plus uncommon/rare quality. It requires Modules. Conveyance has no quality system; it is optional inspiration, not a missing base-game requirement. [Official Quality definitions](https://github.com/wube/factorio-data/blob/2.0.77/quality/prototypes/technology.lua#L3).
- With **Space Age enabled**, **Cliff explosives** moves out of this stage. Its 500-unit cost includes chemical, space and metallurgic packs as well as red/green. The other main-list red/green costs remain in this stage. [Official Space Age override](https://github.com/wube/factorio-data/blob/2.0.77/space-age/base-data-updates.lua#L549).
- Space Age's own technology definitions and Elevated Rails do not add another technology costing exclusively these two packs in this release. Elevated rail research requires later science. [Space Age definitions](https://github.com/wube/factorio-data/blob/2.0.77/space-age/prototypes/technology.lua), [Elevated rail definitions](https://github.com/wube/factorio-data/blob/2.0.77/elevated-rails/prototypes/technology/elevated-rails.lua).

## Important exclusions and non-technology gaps

- **Electric furnace, robotics, braking force 1 and laser turrets** require chemical/blue science. **Flamethrower and energy-shield equipment** require military science. They are not omissions from the two-colour list.
- **Chemical science pack** and **Military science pack** are included because their research costs use only red/green, even though their outputs introduce new science colours.
- **Flammables** and **Modules** have no immediate recipe unlock in the base definitions. They are still qualifying technologies and prerequisites for later research.
- Generic early features outside this strict technology list also matter: stone-brick smelting, factory build/deconstruction conveniences, player-selected inserter filters, two-lane belt behavior and a reason for defenses/enemy pressure. These should not be mistaken for extra red/green technology entries.
- **Automation incentives deserve attention.** Our hand crafting currently completes instantly, including bulk crafting, and most factory recipes are available by hand. Machine-only intermediates (such as an engine production step) or a crafting queue/time cost could make automated production more valuable. This is a design assessment rather than another technology entry.
- Fluid handling also enables generated barrel fill/empty recipes; those are added by the [base data-updates script](https://github.com/wube/factorio-data/blob/2.0.77/base/data-updates.lua), in addition to its three directly declared unlocks.

## Comparison evidence

Local snapshot: `src/main/java/dev/conveyance/player/Technology.java`, `ResearchState.java`, `HandCrafting.java`; `factory/FactoryContent.java`, `MachineEntity.java`, `ItemTransfer.java`, `InserterEntity.java`, `BurnerInserterEntity.java`, `PowerNetwork.java`; `README.md` for the documented prototype constraints. The four research entries and starter recipes were checked directly, not inferred from item names.

This audit changes documentation only. No gameplay changes, builds or tests were performed.

# Early research balance

Reference: Factorio base **2.0.77**, without Space Age. Research pack costs are quantities of **each** listed science pack. Times are seconds per unit at base lab speed.

| Technology | Cost / production milestone | Seconds/unit | Direct prerequisites | Unlocks / effect |
|---|---|---:|---|---|
| Steam Power | Produce 50 minecraft:iron_ingot | — | None | pipe, offshore_pump, boiler, steam_engine |
| Electronics | Produce 10 minecraft:copper_ingot | — | None | copper_wire, electronic_circuit, research_lab, inserter, power_pole |
| Automation Science | Produce 1 conveyance:research_lab | — | Steam Power, Electronics | red_science_pack |
| Automation | 10 red | 10 | Automation Science | assembler, long_inserter |
| Logistics | 20 red | 15 | Automation Science | junction |
| Electric Mining | 25 red | 10 | Automation Science | miner |
| Radar | 20 red | 10 | Automation Science | radar |
| Repair Pack | 25 red | 10 | Automation Science | repair_pack |
| Steel Processing | 50 red | 5 | Automation Science | steel_ingot, steel_crate |
| Logistic Science | 75 red | 5 | Automation Science | green_science_pack |
| Fast Inserter | 30 red | 15 | Automation Science | fast_inserter |
| Stone Wall | 10 red | 10 | Automation Science | stone_wall |
| Gun Turret | 10 red | 10 | Automation Science | gun_turret |
| Military | 10 red | 15 | Automation Science | submachine_gun, shotgun, shotgun_shell |
| Military 2 | 20 red + green | 15 | Military, Steel Processing, Logistic Science | piercing_magazine, grenade |
| Automation 2 | 40 red + green | 15 | Automation, Steel Processing, Logistic Science | assembler_2 |
| Advanced Material Processing | 75 red + green | 30 | Steel Processing, Logistic Science | steel_furnace |
| Solar Energy | 250 red + green | 30 | Steel Processing, Logistic Science | solar_panel |
| Lab Research Speed 1 | 100 red + green | 30 | Automation 2 | Labs research 20% faster. |
| Lab Research Speed 2 | 200 red + green | 30 | Lab Research Speed 1 | Labs research 50% faster in total. |
| Engine | 100 red + green | 15 | Steel Processing, Logistic Science | engine_unit |
| Fluid Handling | 50 red + green | 15 | Automation 2, Engine | storage_tank, fluid_pump |
| Oil Gathering | 100 red + green | 30 | Fluid Handling | pumpjack |
| Oil Processing | Extract crude oil | — | Oil Gathering | oil_refinery, chemical_plant, basic_oil_processing, solid_fuel |
| Plastics | 200 red + green | 30 | Oil Processing | plastic_bar |
| Sulfur Processing | 150 red + green | 30 | Oil Processing | sulfuric_acid, sulfur |
| Advanced Circuit | 200 red + green | 15 | Plastics | advanced_circuit |
| Chemical Science Pack | 75 red + green | 10 | Advanced Circuit, Sulfur Processing | chemical_science_pack |
| Battery | 150 red + green | 30 | Sulfur Processing | battery |
| Electric Energy Accumulators | 150 red + green | 30 | Steel Processing, Logistic Science, Battery | accumulator |
| Logistics 2 | 200 red + green | 30 | Logistics, Logistic Science | fast_conveyor, fast_junction, fast_slope_conveyor |
| Modules | 100 red + green | 30 | Advanced Circuit | Unlock specialised modules for compatible machines. |
| Speed Module | 50 red + green | 30 | Modules | speed_module |
| Productivity Module | 50 red + green | 30 | Modules | productivity_module |
| Efficiency Module | 50 red + green | 30 | Modules | efficiency_module |
| Mining Productivity 1 | 250 red + green | 60 | Advanced Circuit | Mining drills produce 10% additional resources. |
| Physical Projectile Damage 1 | 100 red | 30 | Military | Bullet damage and gun turret damage each gain 10%. |
| Physical Projectile Damage 2 | 100 red + green | 30 | Physical Projectile Damage 1, Logistic Science | Bullet damage and gun turret damage each gain another 10%. |
| Weapon Shooting Speed 1 | 100 red | 30 | Military | Bullet weapons fire 10% faster. |
| Weapon Shooting Speed 2 | 100 red + green | 30 | Weapon Shooting Speed 1, Logistic Science | Bullet weapons fire another 20% faster. |
| Steel Axe | Produce 50 conveyance:steel_ingot | — | Steel Processing | Hand harvesting is 100% faster. Terrain remains protected. |
| Metal Tools | 30 red + green | 15 | Steel Processing, Logistic Science | iron_pickaxe |
| Advanced Oil Processing | 75 red + green + blue | 30 | Chemical Science Pack | advanced_oil_processing, heavy_oil_cracking, light_oil_cracking |
| Stronger Explosives 1 | 100 red + green | 30 | Military 2 | Grenade damage +25% |
| Military Science Pack | 30 red + green | 15 | Military 2, Stone Wall | military_science_pack |
| Laser | 100 red + green + blue | 30 | Battery, Chemical Science Pack | Enables laser research |
| Laser Turret | 150 red + green + blue + grey | 30 | Laser, Military Science Pack | laser_turret |
| Laser Weapons Damage 1 | 100 red + green + blue + grey | 30 | Laser, Military Science Pack | Laser damage +20% |
| Laser Weapons Damage 2 | 200 red + green + blue + grey | 30 | Laser Weapons Damage 1 | Laser damage another +20% |
| Laser Weapons Damage 3 | 300 red + green + blue + grey | 60 | Laser Weapons Damage 2 | Laser damage another +30% |
| Laser Shooting Speed 1 | 50 red + green + blue + grey | 30 | Laser, Military Science Pack | Laser shooting speed +10% |
| Laser Shooting Speed 2 | 100 red + green + blue + grey | 30 | Laser Shooting Speed 1 | Laser shooting speed another +20% |
| Laser Shooting Speed 3 | 200 red + green + blue + grey | 60 | Laser Shooting Speed 2 | Laser shooting speed another +30% |

## Conveyance differences

- One pole type replaces the distribution pole branch; accumulator prerequisites use Steel Processing, Logistic Science and Battery.
- Metal Tools remains a custom research. Natural terrain stays protected.
- Slopes and four-way junctions replace underground belts and Factorio splitters in the same logistics tiers.
- Fluid throughput is unlimited. Storage tanks and directional powered pumps are available; barrels and underground pipes are omitted.
- Vehicles, trains, armour, circuit networks and other systems without implementation have no placeholder technology.
- Tier-one module speed, energy, productivity and pollution effects match the base definitions. Productivity is limited to intermediates and science in assemblers; labs, miners and chemistry also support it.
- Existing research resets once when upgrading from the previous prototype tree.

[Technology source](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua), [recipe source](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/recipe.lua), [module effects](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/item.lua).

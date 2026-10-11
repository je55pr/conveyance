# Laser turrets

This extends the existing oil and battery chain. Sulfuric acid is a piped fluid;
batteries are chemical-plant products and cannot be handcrafted from ore.

1. Research Oil Gathering, extract crude oil and unlock Oil Processing.
2. Refine 100 crude oil into 45 petroleum gas in 5 seconds.
3. Research Sulfur Processing. A chemical plant turns 30 water and 30 petroleum
   gas into 2 sulfur in 1 second.
4. Another chemical plant turns 5 sulfur, 1 iron ingot and 100 water into 50
   sulfuric acid in 1 second. Acid leaves its front-left output port; water enters
   the rear-left input port, relative to the machine's facing.
5. Research Battery. Pipe acid into the battery plant's rear-left port. It uses
   20 acid, 1 iron ingot and 1 copper ingot per battery in 4 seconds.
6. Build the military science line: 1 piercing magazine, 1 grenade and 2 walls
   make 2 grey science packs in 10 seconds. Insert them into the lab's fourth slot.
7. Research Laser, then Laser Turret. Each turret costs 20 steel ingots,
   20 electronic circuits and 12 batteries, with a 20-second crafting duration.
8. Place the turret within power-pole coverage. Batteries are construction
   ingredients; operation consumes electricity and needs no ammunition.

All recipe durations above are base durations. Assembler tiers and installed
speed modules affect actual machine production times. Sulfuric acid and battery
plants use the existing fixed, typed ports and reject mixed-fluid connections.

| Technology | Science units | Seconds per unit | Prerequisites |
| --- | --- | --- | --- |
| Military Science Pack | 30 red + green | 15 | Military 2, Stone Wall |
| Laser | 100 red + green + blue | 30 | Battery, Chemical Science Pack |
| Laser Turret | 150 red + green + blue + grey | 30 | Laser, Military Science Pack |
| Laser Weapons Damage 1 | 100 of each of the four packs | 30 | Laser, Military Science Pack |
| Laser Weapons Damage 2 | 200 of each | 30 | Laser Weapons Damage 1 |
| Laser Weapons Damage 3 | 300 of each | 60 | Laser Weapons Damage 2 |
| Laser Shooting Speed 1 | 50 of each of the four packs | 30 | Laser, Military Science Pack |
| Laser Shooting Speed 2 | 100 of each | 30 | Laser Shooting Speed 1 |
| Laser Shooting Speed 3 | 200 of each | 60 | Laser Shooting Speed 2 |

The damage upgrades add 20%, 20%, then 30% of base damage. Speed upgrades add
10%, 20%, then 30% of base speed. Fully upgraded at this stage, a laser deals
34 damage at 2.4 shots/second. Each shot still costs 800 kJ, so speed upgrades
also increase electricity demand.

The turret has 1000 health, a 24-block range and a 2 x 2 horizontal footprint.
Conveyance reserves three blocks of height to put its gun above two-block walls.
Its stored energy is capped at 801 kJ, with a maximum 9600 kW input and 24 kW
idle drain. Without sufficient generation, it charges more slowly and eventually
waits for enough energy to fire. One steam engine cannot sustain its base firing
rate: 1.5 shots/s consumes 1200 kW, plus 24 kW idle. Accumulators can supply bursts.

Research progress and existing lab inventories are preserved. Lab slots remain
red, green, blue and grey, in that order. The machine's right-click screen shows
charge and power allocation; B mode supplies coverage and power warnings.
Shots and damage are server authoritative. Beams are temporary client graphics,
without dropped items, entities or physics. Shared electricity networks retain
Conveyance's equal proportional allocation between consumers.

Balance references: [Wube's base 2.0.77 technologies](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/technology.lua),
[recipes](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/recipe.lua),
and [turret prototypes](https://github.com/wube/factorio-data/blob/2.0.77/base/prototypes/entity/turrets.lua).

# Building footprints

Checked against the official Factorio Wiki on 10 October 2026. One Factorio tile
maps to one Minecraft ground block. Footprint means occupied width and depth;
Minecraft height is a separate design choice.

| Conveyance building | Factorio counterpart and source | Factorio footprint | Conveyance footprint | Audit result |
| --- | --- | --- | --- | --- |
| Electric miner | [Electric mining drill](https://wiki.factorio.com/Electric_mining_drill) | 3×3 | 3×3 | Changed from 4×4 |
| Burner miner | [Burner mining drill](https://wiki.factorio.com/Burner_mining_drill) | 2×2 | 2×2 | Already matched |
| Assembling machine | [Assembling machine 1](https://wiki.factorio.com/Assembling_machine_1) | 3×3 | 3×3 | Already matched |
| Stone furnace | [Stone furnace](https://wiki.factorio.com/Stone_furnace) | 2×2 | 2×2 | Already matched |
| Research lab | [Lab](https://wiki.factorio.com/Lab) | 3×3 | 3×3 | Already matched |
| Solar panel | [Solar panel](https://wiki.factorio.com/Solar_panel) | 3×3 | 3×3 | Already matched |
| Radar | [Radar](https://wiki.factorio.com/Radar) | 3×3 | 3×3 | Changed from 1×1 |
| Power pole | [Small electric pole](https://wiki.factorio.com/Small_electric_pole) | 1×1 | 1×1 | Already matched |
| Wooden crate | [Wooden chest](https://wiki.factorio.com/Wooden_chest) | 1×1 | 1×1 | Already matched |
| Iron crate | [Iron chest](https://wiki.factorio.com/Iron_chest) | 1×1 | 1×1 | Already matched |
| Conveyor belt | [Transport belt](https://wiki.factorio.com/Transport_belt) | 1×1 | 1×1 | Already matched |
| Inserter | [Inserter](https://wiki.factorio.com/Inserter) | 1×1 | 1×1 | Already matched |
| Burner inserter | [Burner inserter](https://wiki.factorio.com/Burner_inserter) | 1×1 | 1×1 | Already matched |
| Long inserter | [Long-handed inserter](https://wiki.factorio.com/Long-handed_inserter) | 1×1 | 1×1 | Already matched |

Custom buildings keep their existing ground footprints, as requested:

| Conveyance building | Footprint | Comparison |
| --- | --- | --- |
| Solid-fuel generator | 1×1 | Produces electricity directly from fuel; Factorio's [steam engine](https://wiki.factorio.com/Steam_engine) is 3×5 and uses steam |
| Four-way junction | 1×1 | Has four configurable ports; Factorio's [splitter](https://wiki.factorio.com/Splitter) occupies 1×2 |
| Slope conveyor | 1×1 | Carries items between Minecraft heights; reserves headroom above the ramp |

Current Minecraft heights: electric miner 3, burner miner 2, assembler 2, stone
furnace 3, lab 2, solar panel 1, radar 1, pole 4. Other buildings use one block of
height; slopes require two blocks of clearance. Plates and belts have lower
collision shapes within their reserved cells.

Existing electric miners are resized on load. Existing radars expand when their
new footprint is clear and supported; a blocked legacy radar remains usable until
re-placed or its expansion space becomes available.

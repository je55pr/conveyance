# Supplied textures

Original PNG files are installed without resampling or recolouring under
`src/main/resources/assets/conveyance/textures/`. All are 16×16 except
`gui/selector_2.png`, which is 32×32.

| Supplied name | Asset path under textures | Use |
|---|---|---|
| gear_wheel.png | item/gear.png | Iron Gear icon |
| copper_cable.png | item/copper_wire.png | Copper Wire icon |
| basic_circuit.png | item/electronic_circuit.png | Electronic Circuit icon |
| wood.png | item/wood.png | Wood icon |
| advanced_circuit.png | item/advanced_circuit.png | Ready for future content |
| processing_circuit.png | item/processing_circuit.png | Ready for future content |
| cpu.png | item/cpu.png | Ready for future content |
| plastic_bar.png | item/plastic_bar.png | Ready for future content |
| floor.png | block/floor.png | Construction texture, not assigned to a block yet |
| floor_2.png | block/floor_2.png | Construction texture variant |
| wall.png | block/wall.png | Construction texture, not assigned to a block yet |
| wall_2.png | block/wall_2.png | Construction texture variant |
| wall_3.png | block/wall_3.png | Construction texture variant |
| station.png | block/station.png | Ready for future station content |
| selector.png | gui/selector.png | GUI selection texture, not assigned yet |
| selector_2.png | gui/selector_2.png | GUI selection texture variant |
| selector_3.png | gui/selector_3.png | GUI selection texture variant |

`tools/generate_factory_assets.py` gives imported item PNGs precedence over the
prototype item models. Item sprites are separate from placed block textures.

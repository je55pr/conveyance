# Conveyance

A standalone industrial overhaul for Minecraft Java Edition **1.20.1**, using
**Forge 47.4.26** and **Java 17**. No other mods are required.

## Version 0.7.0: surface resources prototype

New worlds offer one locked **Conveyance** preset. The generator starts from its
own terrain rules, without vanilla carving, decoration, ores, vegetation,
structures, or creature generation.

- The world is 64 blocks high: buildable blocks run from Y=-32 through Y=31.
- Iron, copper and coal form irregular per-block patches on the Y=0 plains.
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
patches. There are no trees or miners yet, so the Survival progression is incomplete.
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

Open **Mods** to find **Conveyance 0.7.0**, then create a **new Creative world**.
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

The resulting file is `build/libs/conveyance-1.20.1-0.7.0.jar`. Put it in the
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

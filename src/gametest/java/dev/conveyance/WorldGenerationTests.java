package dev.conveyance;

import com.mojang.serialization.JsonOps;
import dev.conveyance.world.TerracedChunkGenerator;
import dev.conveyance.world.TerracedTerrain;
import dev.conveyance.world.WorldGeneration;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.tags.WorldPresetTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Conveyance.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WorldGenerationTests {
    @GameTest(template = "empty")
    public static void terrainLatticeAndSeeds(GameTestHelper helper) {
        var first = new TerracedTerrain(123456);
        var sameSeed = new TerracedTerrain(123456);
        var otherSeed = new TerracedTerrain(654321);
        boolean seedChangesWorld = false;
        Set<Integer> heights = new java.util.HashSet<>();
        for (int x = -1600; x < 1600; x += 79) {
            for (int z = -1600; z < 1600; z += 83) {
                int baseX = Math.floorDiv(x, 4) * 4, baseZ = Math.floorDiv(z, 4) * 4;
                int height = first.surfaceHeight(baseX, baseZ);
                heights.add(height);
                helper.assertTrue(height % 3 == 0, "A surface is between three-block layers");
                for (int dx = 0; dx < 4; dx++) {
                    for (int dz = 0; dz < 4; dz++) {
                        helper.assertTrue(first.surfaceHeight(baseX + dx, baseZ + dz) == height,
                                "A four-by-four cell is not flat, including negative coordinates");
                    }
                }
                helper.assertTrue(sameSeed.surfaceHeight(x, z) == first.surfaceHeight(x, z),
                        "The same seed changes the world");
                seedChangesWorld |= otherSeed.surfaceHeight(x, z) != first.surfaceHeight(x, z);
                helper.assertTrue(Math.abs(first.surfaceHeight(baseX + 4, baseZ) - height) <= 3,
                        "Adjacent terraces jump more than one layer");
            }
        }
        helper.assertTrue(seedChangesWorld && heights.size() > 6, "Terrain has insufficient variation");
        for (int x = -48; x < 48; x += 4) {
            for (int z = -48; z < 48; z += 4) {
                helper.assertTrue(first.surfaceHeight(x, z) == 84, "Starting area is not level");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void liveWorldUsesSolidTerracedGeneration(GameTestHelper helper) {
        var level = helper.getLevel();
        var generator = level.getChunkSource().getGenerator();
        helper.assertTrue(generator instanceof TerracedChunkGenerator, "The actual test world uses another generator");
        helper.assertTrue(level.getMinBuildHeight() == -96 && level.getMaxBuildHeight() == 384,
                "The world bounds do not fit complete three-block layers");
        var position = new BlockPos.MutableBlockPos();
        for (int cellX : new int[] {-132, -4, 124}) {
            int x = cellX * 4, z = 400;
            // Level#getHeight returns the world bottom for an unloaded column.
            // Load the real chunk before comparing its blocks and heightmap.
            level.getChunk(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
            int height = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            helper.assertTrue(height % 3 == 0, "Generated blocks do not land on a three-block layer");
            for (int dx = 0; dx < 4; dx++) {
                for (int dz = 0; dz < 4; dz++) {
                    helper.assertTrue(level.getHeight(Heightmap.Types.WORLD_SURFACE, x + dx, z + dz) == height,
                            "A generated terrain cell is not flat");
                    for (int y = level.getMinBuildHeight(); y < height; y++) {
                        var state = level.getBlockState(position.set(x + dx, y, z + dz));
                        helper.assertTrue(state.is(Blocks.BEDROCK) || state.is(Blocks.STONE)
                                || state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK)
                                || state.is(Blocks.COARSE_DIRT), "Generated a cave, ore, water or unexpected block");
                    }
                    helper.assertTrue(level.getBlockState(position.set(x + dx, height, z + dz)).isAir(),
                            "Unexpected content above the ground");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void presetsAndSavedCodec(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var presets = registries.registryOrThrow(Registries.WORLD_PRESET);
        for (var tag : java.util.List.of(WorldPresetTags.NORMAL, WorldPresetTags.EXTENDED)) {
            var entries = presets.getTag(tag).orElseThrow();
            helper.assertTrue(entries.size() == 1 && entries.get(0).is(WorldGeneration.PRESET),
                    "Another selectable or hidden world type remains");
        }
        helper.assertTrue(presets.getHolderOrThrow(WorldPresets.NORMAL).value().overworld().orElseThrow()
                .generator() instanceof TerracedChunkGenerator, "Default server world generation is not Conveyance");
        var generator = helper.getLevel().getChunkSource().getGenerator();
        var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        var saved = ChunkGenerator.CODEC.encodeStart(ops, generator).getOrThrow(false, message -> {
            throw new IllegalStateException(message);
        });
        var restored = ChunkGenerator.CODEC.parse(ops, saved).getOrThrow(false, message -> {
            throw new IllegalStateException(message);
        });
        helper.assertTrue(restored instanceof TerracedChunkGenerator, "Saved world restores a different generator");
        var random = helper.getLevel().getChunkSource().randomState();
        for (int x = -900; x < 900; x += 117) {
            int first = generator.getBaseHeight(x, 700, Heightmap.Types.WORLD_SURFACE, helper.getLevel(), random);
            int reloaded = restored.getBaseHeight(x, 700, Heightmap.Types.WORLD_SURFACE, helper.getLevel(), random);
            helper.assertTrue(first == reloaded, "Saving and reloading changes terrain height");
            var column = restored.getBaseColumn(x, 700, helper.getLevel(), random);
            helper.assertTrue(!column.getBlock(first - 1).isAir() && column.getBlock(first).isAir(),
                    "Height queries disagree with the generated ground column");
        }
        helper.succeed();
    }
}

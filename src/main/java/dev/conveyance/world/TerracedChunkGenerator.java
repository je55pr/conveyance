package dev.conveyance.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public final class TerracedChunkGenerator extends ChunkGenerator {
    public static final Codec<TerracedChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Biome.CODEC.fieldOf("biome").forGetter(generator -> generator.biome)
    ).apply(instance, TerracedChunkGenerator::new));
    private static final ResourceLocation TERRAIN_RANDOM = ResourceLocation.fromNamespaceAndPath("conveyance", "terrain");
    private final Holder<Biome> biome;
    private volatile TerrainContext cachedTerrain;

    private record TerrainContext(RandomState state, TerracedTerrain terrain, ForestTerrain forest) {}

    public TerracedChunkGenerator(Holder<Biome> biome) {
        super(new FixedBiomeSource(biome));
        this.biome = biome;
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() { return CODEC; }

    private TerrainContext context(RandomState state) {
        TerrainContext context = cachedTerrain;
        if (context == null || context.state() != state) {
            synchronized (this) {
                context = cachedTerrain;
                if (context == null || context.state() != state) {
                    long seed = state.getOrCreateRandomFactory(TERRAIN_RANDOM).at(0, 0, 0).nextLong();
                    var terrain = new TerracedTerrain(seed);
                    context = new TerrainContext(state, terrain, new ForestTerrain(seed, terrain));
                    cachedTerrain = context;
                }
            }
        }
        return context;
    }
    private TerracedTerrain terrain(RandomState state) { return context(state).terrain(); }

    public byte[] mapTile(RandomState state, int chunkX, int chunkZ) {
        TerrainContext context = context(state);
        int minX = chunkX * 16, minZ = chunkZ * 16;
        boolean[] forest = context.forest().canopyPixels(minX, minZ);
        byte[] pixels = new byte[256];
        var cells = new TerracedTerrain.Cell[16];
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            int cellIndex = z / 4 * 4 + x / 4;
            if (cells[cellIndex] == null) cells[cellIndex] = context.terrain().cellAt(minX + x, minZ + z);
            var cell = cells[cellIndex];
            var ore = cell.feature() == TerracedTerrain.Feature.PLAIN ? context.terrain().oreAt(minX + x, minZ + z) : TerracedTerrain.Ore.NONE;
            pixels[z * 16 + x] = dev.conveyance.map.MapPalette.terrain(cell, ore, forest[z * 16 + x]);
        }
        return pixels;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender,
            RandomState randomState, StructureManager structures, ChunkAccess chunk) {
        TerrainContext context = context(randomState);
        TerracedTerrain terrain = context.terrain();
        var position = new BlockPos.MutableBlockPos();
        var surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        var floor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int minY = Math.max(getMinY(), chunk.getMinBuildHeight());
        for (int cellX = 0; cellX < 16; cellX += TerracedTerrain.CELL_WIDTH) {
            for (int cellZ = 0; cellZ < 16; cellZ += TerracedTerrain.CELL_WIDTH) {
                TerracedTerrain.Cell cell = terrain.cellAt(minX + cellX, minZ + cellZ);
                int height = Math.min(chunk.getMaxBuildHeight(), cell.surfaceHeight());
                int filledHeight = cell.water() ? Math.max(height, TerracedTerrain.SEA_LEVEL) : height;
                for (int dx = 0; dx < TerracedTerrain.CELL_WIDTH; dx++) {
                    for (int dz = 0; dz < TerracedTerrain.CELL_WIDTH; dz++) {
                        int x = minX + cellX + dx, z = minZ + cellZ + dz;
                        BlockState ore = surfaceOre(terrain, cell, x, z);
                        for (int y = minY; y < filledHeight; y++) {
                            chunk.setBlockState(position.set(x, y, z), material(y, height, cell, ore), false);
                        }
                        if (height > minY) {
                            BlockState top = material(height - 1, height, cell, ore);
                            floor.update(cellX + dx, height - 1, cellZ + dz, top);
                            surface.update(cellX + dx, filledHeight - 1, cellZ + dz,
                                    material(filledHeight - 1, height, cell, ore));
                        }
                    }
                }
            }
        }
        context.forest().generate(chunk);
        return CompletableFuture.completedFuture(chunk);
    }

    private static BlockState surfaceOre(TerracedTerrain terrain, TerracedTerrain.Cell cell, int x, int z) {
        if (cell.feature() != TerracedTerrain.Feature.PLAIN || cell.surfaceHeight() != TerracedTerrain.START_HEIGHT) {
            return null;
        }
        return switch (terrain.oreAt(x, z)) {
            case IRON -> Blocks.IRON_ORE.defaultBlockState();
            case COPPER -> Blocks.COPPER_ORE.defaultBlockState();
            case COAL -> Blocks.COAL_ORE.defaultBlockState();
            case STONE -> Blocks.STONE.defaultBlockState();
            case NONE -> null;
        };
    }

    private static BlockState material(int y, int height, TerracedTerrain.Cell cell, BlockState ore) {
        if (y < TerracedTerrain.MIN_Y) return Blocks.AIR.defaultBlockState();
        if (y >= height) {
            return (cell.water() && y < TerracedTerrain.SEA_LEVEL ? Blocks.WATER : Blocks.AIR).defaultBlockState();
        }
        if (y < TerracedTerrain.MIN_Y + TerracedTerrain.LAYER_HEIGHT) return Blocks.BEDROCK.defaultBlockState();
        if (ore != null && y == height - 1) return ore;
        if (y >= TerracedTerrain.BASE_GROUND_Y && cell.feature() == TerracedTerrain.Feature.ROCK_WALL) {
            int band = Math.floorDiv(y - TerracedTerrain.START_HEIGHT, TerracedTerrain.LAYER_HEIGHT);
            return (Math.floorMod(band, 3) == 1 ? Blocks.ANDESITE : Blocks.STONE).defaultBlockState();
        }
        if (y >= TerracedTerrain.BASE_GROUND_Y && cell.feature() == TerracedTerrain.Feature.LOW_RIDGE) {
            return Blocks.GRANITE.defaultBlockState();
        }
        if (y < height - TerracedTerrain.LAYER_HEIGHT) return Blocks.STONE.defaultBlockState();
        if (cell.water() || cell.feature() == TerracedTerrain.Feature.SHORE) return Blocks.SAND.defaultBlockState();
        if (cell.dry()) return Blocks.COARSE_DIRT.defaultBlockState();
        return (y == height - 1 ? Blocks.GRASS_BLOCK : Blocks.DIRT).defaultBlockState();
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor height,
            RandomState randomState) {
        var cell = terrain(randomState).cellAt(x, z);
        boolean floor = type == Heightmap.Types.OCEAN_FLOOR || type == Heightmap.Types.OCEAN_FLOOR_WG;
        int surface = cell.water() && !floor ? Math.max(cell.surfaceHeight(), TerracedTerrain.SEA_LEVEL)
                : cell.surfaceHeight();
        return Math.max(height.getMinBuildHeight(), Math.min(height.getMaxBuildHeight(), surface));
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor height, RandomState randomState) {
        TerracedTerrain terrain = terrain(randomState);
        TerracedTerrain.Cell cell = terrain.cellAt(x, z);
        int surface = cell.surfaceHeight();
        BlockState ore = surfaceOre(terrain, cell, x, z);
        var blocks = new BlockState[height.getHeight()];
        for (int i = 0; i < blocks.length; i++) blocks[i] = material(height.getMinBuildHeight() + i, surface, cell, ore);
        return new NoiseColumn(height.getMinBuildHeight(), blocks);
    }

    // This prototype owns every generation pass. There is no inherited carving,
    // biome decoration, ore placement, structure or mob pass. Seas, rivers,
    // resources and forests are filled directly by our own generation rules.
    @Override public void applyCarvers(WorldGenRegion region, long seed, RandomState state,
            BiomeManager biomes, StructureManager structures, ChunkAccess chunk, GenerationStep.Carving step) {}
    @Override public void buildSurface(WorldGenRegion region, StructureManager structures,
            RandomState state, ChunkAccess chunk) {}
    @Override public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structures) {}
    @Override public void createStructures(RegistryAccess registries,
            net.minecraft.world.level.chunk.ChunkGeneratorStructureState state, StructureManager structures,
            ChunkAccess chunk, StructureTemplateManager templates) {}
    @Override public void createReferences(WorldGenLevel level, StructureManager structures, ChunkAccess chunk) {}
    @Override public void spawnOriginalMobs(WorldGenRegion region) {}
    @Override public int getGenDepth() { return TerracedTerrain.WORLD_HEIGHT; }
    @Override public int getSeaLevel() { return TerracedTerrain.SEA_LEVEL; }
    @Override public int getMinY() { return TerracedTerrain.MIN_Y; }
    @Override public int getSpawnHeight(LevelHeightAccessor height) { return TerracedTerrain.START_HEIGHT; }

    @Override
    public void addDebugScreenInfo(List<String> lines, RandomState state, BlockPos pos) {
        var cell = terrain(state).cellAt(pos.getX(), pos.getZ());
        String feature = switch (cell.feature()) {
            case PLAIN -> "Plain";
            case LOW_RIDGE -> "Granite ridge";
            case ROCK_WALL -> "Rock wall";
            case SHORE -> "Shore";
            case SEA -> "Sea";
            case RIVER -> "River";
        };
        lines.add("Conveyance: " + feature + " / ground Y " + (cell.surfaceHeight() - 1)
                + (cell.water() ? " / water depth " + cell.waterDepth() : ""));
        if (cell.feature() == TerracedTerrain.Feature.PLAIN) {
            var ore = terrain(state).oreAt(pos.getX(), pos.getZ());
            if (ore != TerracedTerrain.Ore.NONE) lines.add("Surface resource: " + ore.name().toLowerCase(java.util.Locale.ROOT));
        }
    }
}

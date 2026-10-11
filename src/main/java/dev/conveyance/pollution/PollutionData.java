package dev.conveyance.pollution;

import dev.conveyance.factory.FactoryContent;
import dev.conveyance.map.WorldMapData;
import dev.conveyance.world.ForestLogBlock;
import dev.conveyance.world.TerracedChunkGenerator;
import dev.conveyance.world.TerracedTerrain;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;

/** A persistent, sparse field of pollution units per 16x16 Minecraft chunk. */
public final class PollutionData extends SavedData {
    public static final int AMBIENT_WIDTH = 17;
    public static final double TREE_PER_MINUTE = 0.18, WATER_PER_MINUTE = 0.006;
    private static final double MIN_AMOUNT = 0.0001, MAX_AMOUNT = 1_000_000, DIFFUSION = 0.02;
    private static final double MAP_SCALE = Math.log1p(100);
    private record Sink(int water, int trees) {
        double perMinute() { return water * WATER_PER_MINUTE + trees * TREE_PER_MINUTE; }
    }
    private final Map<Long, Double> cells = new HashMap<>();
    private final Map<Long, Sink> sinks = new HashMap<>();
    private final Map<Long, Long> sampledAt = new HashMap<>();
    private long revision;

    public static PollutionData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(PollutionData::load, PollutionData::new, "conveyance_pollution");
    }
    public static void emit(Level level, BlockPos pos, double perMinute) {
        if (perMinute > 0 && level instanceof ServerLevel server && level.dimension() == Level.OVERWORLD)
            get(server).add(pos, perMinute / 1200);
    }
    public double at(int chunkX, int chunkZ) { return cells.getOrDefault(ChunkPos.asLong(chunkX, chunkZ), 0.0); }
    public double at(BlockPos pos) { return at(Math.floorDiv(pos.getX(), 16), Math.floorDiv(pos.getZ(), 16)); }
    public long revision() { return revision; }
    public void add(BlockPos pos, double amount) {
        if (!Double.isFinite(amount) || amount <= 0) return;
        long key = ChunkPos.asLong(Math.floorDiv(pos.getX(), 16), Math.floorDiv(pos.getZ(), 16));
        cells.merge(key, Math.min(MAX_AMOUNT, amount), (a, b) -> Math.min(MAX_AMOUNT, a + b));
        setDirty();
    }
    public long[] pollutedChunks() { return cells.keySet().stream().mapToLong(Long::longValue).toArray(); }
    public BlockPos strongestNearby(BlockPos origin, int radius) {
        int cx = origin.getX() >> 4, cz = origin.getZ() >> 4; double best = 0; BlockPos target = null;
        for (var entry : cells.entrySet()) {
            int x = ChunkPos.getX(entry.getKey()), z = ChunkPos.getZ(entry.getKey());
            if (Math.abs(x - cx) > radius || Math.abs(z - cz) > radius || entry.getValue() <= best) continue;
            best = entry.getValue(); target = new BlockPos(x * 16 + 8, TerracedTerrain.START_HEIGHT, z * 16 + 8);
        }
        return target;
    }
    /** Nests spend locally absorbed pollution on attack groups. */
    public double consume(BlockPos pos, double requested) {
        if (!Double.isFinite(requested) || requested <= 0) return 0;
        long key = ChunkPos.asLong(Math.floorDiv(pos.getX(), 16), Math.floorDiv(pos.getZ(), 16));
        double existing = cells.getOrDefault(key, 0.0), consumed = Math.min(existing, requested);
        if (consumed == 0) return 0;
        if (existing - consumed < MIN_AMOUNT) cells.remove(key); else cells.put(key, existing - consumed);
        revision++; setDirty();
        return consumed;
    }
    public void clear() { cells.clear(); revision++; setDirty(); }
    public void changed() { revision++; }

    public void tick(ServerLevel level) {
        if (cells.isEmpty()) return;
        var next = new HashMap<Long, Double>();
        // Every cell reads the same old field: diffusion is independent of map
        // iteration order and never recursively spreads twice in one update.
        for (var entry : cells.entrySet()) {
            int x = ChunkPos.getX(entry.getKey()), z = ChunkPos.getZ(entry.getKey());
            double amount = Math.max(0, entry.getValue() - absorption(level, x, z) / 60);
            if (amount < MIN_AMOUNT) continue;
            double share = amount * DIFFUSION;
            merge(next, x, z, amount - 4 * share);
            if (share > 0) {
                merge(next, x - 1, z, share); merge(next, x + 1, z, share);
                merge(next, x, z - 1, share); merge(next, x, z + 1, share);
            }
        }
        next.values().removeIf(amount -> amount < MIN_AMOUNT);
        cells.clear(); cells.putAll(next);
        revision++; setDirty();
    }
    private static void merge(Map<Long, Double> destination, int x, int z, double amount) {
        if (x < -1875000 || x > 1875000 || z < -1875000 || z > 1875000 || amount <= 0) return;
        destination.merge(ChunkPos.asLong(x, z), amount, (a, b) -> Math.min(MAX_AMOUNT, a + b));
    }
    public double absorption(ServerLevel level, int x, int z) {
        long key = ChunkPos.asLong(x, z);
        var chunk = level.getChunkSource().getChunkNow(x, z);
        Sink sink = sinks.get(key);
        if (chunk != null) {
            long last = sampledAt.getOrDefault(key, Long.MIN_VALUE), now = level.getGameTime();
            if (sink == null || last == Long.MIN_VALUE || now - last >= 100) {
                observe(chunk, now); sink = sinks.get(key);
            }
        } else if (sink == null) {
            // This predicts natural absorbers without creating or loading the
            // distant chunk. Once visited, its actual environment is retained.
            if (level.getChunkSource().getGenerator() instanceof TerracedChunkGenerator generator) {
                var nature = generator.environment(level.getChunkSource().randomState(), x, z);
                sink = new Sink(nature.waterColumns(), nature.trees());
            } else sink = new Sink(0, 0);
            sinks.put(key, sink); setDirty();
        }
        return sink.perMinute();
    }
    public void observe(ChunkAccess chunk, long time) {
        int water = 0, trees = 0;
        var pos = new BlockPos.MutableBlockPos();
        int minX = chunk.getPos().getMinBlockX(), minZ = chunk.getPos().getMinBlockZ();
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            var root = chunk.getBlockState(pos.set(minX + x, TerracedTerrain.START_HEIGHT, minZ + z));
            if (root.is(FactoryContent.TREE_LOG.get()) && root.getValue(ForestLogBlock.LAYER) == 0) trees++;
            int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            boolean surfaceWater = chunk.getBlockState(pos.set(minX + x, top, minZ + z)).getFluidState().is(FluidTags.WATER);
            boolean seaWater = chunk.getBlockState(pos.set(minX + x, TerracedTerrain.SEA_LEVEL - 1, minZ + z)).getFluidState().is(FluidTags.WATER);
            if (surfaceWater || seaWater) water++;
        }
        long key = chunk.getPos().toLong();
        Sink sink = new Sink(water, trees), old = sinks.put(key, sink);
        sampledAt.put(key, time);
        if (!sink.equals(old)) setDirty();
    }

    /** Sparse overlay pages use their strongest explored chunk at distant zoom. */
    public Map<Long, byte[]> overlay(WorldMapData map, int minX, int minZ, int maxX, int maxZ, int step) {
        var pages = new HashMap<Long, byte[]>();
        for (var entry : cells.entrySet()) {
            int cx = ChunkPos.getX(entry.getKey()), cz = ChunkPos.getZ(entry.getKey());
            int x = Math.floorDiv(cx, step), z = Math.floorDiv(cz, step);
            if (x < minX || x > maxX || z < minZ || z > maxZ || !map.known(cx, cz)) continue;
            byte encoded = encode(entry.getValue());
            if (encoded == 0) continue;
            var pixels = pages.computeIfAbsent(ChunkPos.asLong(x, z), ignored -> new byte[256]);
            if (step < 16) {
                int size = 16 / step, px = Math.floorMod(cx, step) * size, pz = Math.floorMod(cz, step) * size;
                for (int dz = 0; dz < size; dz++) for (int dx = 0; dx < size; dx++) pixels[(pz + dz) * 16 + px + dx] = encoded;
            } else {
                int chunksPerPixel = step / 16;
                int pixel = Math.floorMod(cz, step) / chunksPerPixel * 16 + Math.floorMod(cx, step) / chunksPerPixel;
                if ((encoded & 255) > (pixels[pixel] & 255)) pixels[pixel] = encoded;
            }
        }
        return pages;
    }
    private static byte encode(double amount) { return amount < 0.02 ? 0 : (byte) Math.min(255, Math.max(1, Math.round(Math.log1p(amount) / MAP_SCALE * 255))); }
    public static double decode(int encoded) { return Math.expm1(Math.max(0, Math.min(255, encoded)) / 255.0 * MAP_SCALE); }
    public float[] nearby(int minX, int minZ) {
        float[] values = new float[AMBIENT_WIDTH * AMBIENT_WIDTH];
        for (int z = 0; z < AMBIENT_WIDTH; z++) for (int x = 0; x < AMBIENT_WIDTH; x++) values[z * AMBIENT_WIDTH + x] = (float) at(minX + x, minZ + z);
        return values;
    }

    private static PollutionData load(CompoundTag tag) {
        var data = new PollutionData();
        var cells = tag.getList("Cells", Tag.TAG_COMPOUND);
        for (int i = 0; i < cells.size(); i++) {
            var cell = cells.getCompound(i); double amount = cell.getDouble("Amount");
            if (Double.isFinite(amount) && amount >= MIN_AMOUNT) data.cells.put(cell.getLong("Pos"), Math.min(MAX_AMOUNT, amount));
        }
        var sinks = tag.getList("Sinks", Tag.TAG_COMPOUND);
        for (int i = 0; i < sinks.size(); i++) {
            var sink = sinks.getCompound(i);
            data.sinks.put(sink.getLong("Pos"), new Sink(Math.max(0, Math.min(256, sink.getInt("Water"))), Math.max(0, Math.min(256, sink.getInt("Trees")))));
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        var savedCells = new ListTag();
        cells.forEach((pos, amount) -> {
            var cell = new CompoundTag(); cell.putLong("Pos", pos); cell.putDouble("Amount", amount); savedCells.add(cell);
        });
        var savedSinks = new ListTag();
        sinks.forEach((pos, sink) -> {
            var entry = new CompoundTag(); entry.putLong("Pos", pos); entry.putInt("Water", sink.water); entry.putInt("Trees", sink.trees); savedSinks.add(entry);
        });
        tag.put("Cells", savedCells); tag.put("Sinks", savedSinks);
        return tag;
    }

    // Common-side packet cache: no Minecraft client classes load on the server.
    public record ClientField(int minX, int minZ, long seed, float[] pollution, float[] territory) {
        private double at(float[] field, int x, int z) {
            x -= minX; z -= minZ;
            return x < 0 || z < 0 || x >= AMBIENT_WIDTH || z >= AMBIENT_WIDTH ? 0 : field[z * AMBIENT_WIDTH + x];
        }
        private double sample(float[] field, double blockX, double blockZ) {
            double x = blockX / 16 - .5, z = blockZ / 16 - .5; int cx = (int) Math.floor(x), cz = (int) Math.floor(z);
            double tx = x - cx, tz = z - cz;
            return (1 - tz) * ((1 - tx) * at(field, cx, cz) + tx * at(field, cx + 1, cz)) + tz * ((1 - tx) * at(field, cx, cz + 1) + tx * at(field, cx + 1, cz + 1));
        }
        public double pollutionAt(double x, double z) { return sample(pollution, x, z); }
        public double territoryAt(double x, double z) { return sample(territory, x, z); }
    }
    private static volatile ClientField client = emptyClient();
    private static ClientField emptyClient() { return new ClientField(0, 0, 0, new float[AMBIENT_WIDTH * AMBIENT_WIDTH], new float[AMBIENT_WIDTH * AMBIENT_WIDTH]); }
    public static ClientField clientField() { return client; }
    public static void updateClient(int minX, int minZ, long seed, float[] values, float[] territory) { client = new ClientField(minX, minZ, seed, values.clone(), territory.clone()); }
    public static void resetClient() { client = emptyClient(); }
    public static double clientSample(double x, double z) { return client.pollutionAt(x, z); }
}

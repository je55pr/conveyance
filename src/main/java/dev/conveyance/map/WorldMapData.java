package dev.conveyance.map;

import dev.conveyance.world.TerracedChunkGenerator;
import dev.conveyance.factory.RadarEntity;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;

/** One shared, persistent explored map. Sampling distant terrain never loads chunks. */
public final class WorldMapData extends SavedData {
    public record Tile(int x, int z, long revision, byte[] pixels) {}
    public record ViewTile(int x, int z, int step, long revision, byte[] pixels, long pollutionRevision, byte[] pollution) {
        public ViewTile(int x, int z, int step, long revision, byte[] pixels) { this(x, z, step, revision, pixels, 0, new byte[256]); }
    }
    private final Map<Long, Tile> tiles = new HashMap<>();
    private final Set<Long> radars = new HashSet<>();
    private long revision;
    public static WorldMapData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(WorldMapData::load, WorldMapData::new, "conveyance_map");
    }
    private static WorldMapData load(CompoundTag tag) {
        WorldMapData data = new WorldMapData();
        ListTag tiles = tag.getList("Tiles", Tag.TAG_COMPOUND);
        for (int index = 0; index < tiles.size(); index++) {
            CompoundTag tile = tiles.getCompound(index);
            byte[] pixels = tile.getByteArray("Pixels");
            if (pixels.length != 256) continue;
            int x = tile.getInt("X"), z = tile.getInt("Z");
            data.tiles.put(ChunkPos.asLong(x, z), new Tile(x, z, ++data.revision, pixels));
        }
        for (long radar : tag.getLongArray("Radars")) data.radars.add(radar);
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        ListTag saved = new ListTag();
        for (Tile tile : tiles.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("X", tile.x); entry.putInt("Z", tile.z); entry.putByteArray("Pixels", tile.pixels);
            saved.add(entry);
        }
        tag.put("Tiles", saved);
        tag.putLongArray("Radars", radars.stream().mapToLong(Long::longValue).toArray());
        return tag;
    }
    public boolean known(int x, int z) { return tiles.containsKey(ChunkPos.asLong(x, z)); }
    public int count() { return tiles.size(); }
    public long revision() { return revision; }
    public boolean reveal(ServerLevel level, int x, int z) {
        dev.conveyance.combat.EnemyNests.get(level).discoverMap(level, x, z);
        byte[] pixels = sample(level, x, z);
        if (pixels == null) return false;
        long key = ChunkPos.asLong(x, z);
        Tile previous = tiles.get(key);
        if (previous != null && Arrays.equals(previous.pixels, pixels)) return false;
        tiles.put(key, new Tile(x, z, ++revision, pixels));
        setDirty();
        return true;
    }
    public void radar(BlockPos pos, boolean exists) {
        boolean changed = exists ? radars.add(pos.asLong()) : radars.remove(pos.asLong());
        if (changed) { revision++; setDirty(); }
    }
    public int coverage(int minX, int minZ, int maxX, int maxZ) {
        int count = 0;
        for (int x = minX; x < maxX; x++) for (int z = minZ; z < maxZ; z++) if (known(x, z)) count++;
        return count;
    }
    /** Each overview page remains 16x16 pixels while covering more chunks. */
    public List<ViewTile> window(int minX, int minZ, int maxX, int maxZ, int step) {
        if (step >= 16) return overviewWindow(minX, minZ, maxX, maxZ, step);
        var result = new java.util.ArrayList<ViewTile>();
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            byte[] pixels = null;
            long version = 0;
            for (int cz = 0; cz < step; cz++) for (int cx = 0; cx < step; cx++) {
                Tile tile = tiles.get(ChunkPos.asLong(x * step + cx, z * step + cz));
                if (tile == null) continue;
                if (pixels == null) pixels = new byte[256];
                version = Math.max(version, tile.revision);
                int perChunk = 16 / step;
                for (int pz = 0; pz < 16; pz += step) for (int px = 0; px < 16; px += step) {
                    int index = (cz * perChunk + pz / step) * 16 + cx * perChunk + px / step;
                    pixels[index] = MapPalette.overview(tile.pixels, px, pz, step);
                }
            }
            if (pixels != null) result.add(new ViewTile(x, z, step, version, pixels));
        }
        return result;
    }
    private static final class Overview {
        final int[] colors = new int[256 * MapPalette.size()];
        long revision;
    }
    private List<ViewTile> overviewWindow(int minX, int minZ, int maxX, int maxZ, int step) {
        var pages = new HashMap<Long, Overview>();
        int chunksPerPixel = step / 16;
        // Visit only explored chunks, rather than millions of unknown chunk coordinates.
        for (Tile tile : tiles.values()) {
            int x = Math.floorDiv(tile.x, step), z = Math.floorDiv(tile.z, step);
            if (x < minX || x > maxX || z < minZ || z > maxZ) continue;
            var page = pages.computeIfAbsent(ChunkPos.asLong(x, z), ignored -> new Overview());
            int pixel = Math.floorMod(tile.z, step) / chunksPerPixel * 16 + Math.floorMod(tile.x, step) / chunksPerPixel;
            int value = MapPalette.overview(tile.pixels, 0, 0, 16) & 255;
            page.colors[pixel * MapPalette.size() + value]++;
            page.revision = Math.max(page.revision, tile.revision);
        }
        var result = new java.util.ArrayList<ViewTile>();
        pages.forEach((position, page) -> {
            byte[] pixels = new byte[256];
            for (int pixel = 0; pixel < 256; pixel++) pixels[pixel] = MapPalette.dominant(page.colors, pixel * MapPalette.size());
            result.add(new ViewTile(ChunkPos.getX(position), ChunkPos.getZ(position), step, page.revision, pixels));
        });
        return result;
    }
    public long[] radars(int minX, int minZ, int maxX, int maxZ) {
        return radars.stream().filter(packed -> {
            BlockPos pos = BlockPos.of(packed);
            return RadarEntity.minChunk(pos.getX()) <= maxX && RadarEntity.maxChunkExclusive(pos.getX()) > minX
                    && RadarEntity.minChunk(pos.getZ()) <= maxZ && RadarEntity.maxChunkExclusive(pos.getZ()) > minZ;
        }).limit(4096).mapToLong(Long::longValue).toArray();
    }
    private static byte[] sample(ServerLevel level, int x, int z) {
        var chunk = level.getChunkSource().getChunkNow(x, z);
        if (chunk == null) {
            return level.getChunkSource().getGenerator() instanceof TerracedChunkGenerator generator
                    ? generator.mapTile(level.getChunkSource().randomState(), x, z) : null;
        }
        byte[] pixels = new byte[256];
        var pos = new BlockPos.MutableBlockPos();
        for (int dz = 0; dz < 16; dz++) for (int dx = 0; dx < 16; dx++) {
            int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, dx, dz);
            var surface = chunk.getBlockState(pos.set(x * 16 + dx, y, z * 16 + dz));
            byte resource = MapPalette.resource(chunk.getBlockState(pos.set(x * 16 + dx, 0, z * 16 + dz)));
            pixels[dz * 16 + dx] = resource != MapPalette.UNKNOWN ? resource : MapPalette.surface(surface, y);
        }
        return pixels;
    }

    // Common-side client cache: no rendering classes are loaded on a dedicated server.
    private static final Map<Integer, Map<Long, ViewTile>> CLIENT = new HashMap<>();
    private static long clientRevision;
    private static int clientCount;
    private static long[] clientRadars = new long[0];
    private static long[] clientNests = new long[0], clientBiters = new long[0];
    private static long clientNestRevision;
    public static long clientNestRevision() { return clientNestRevision; }
    private static int revealDone, revealTotal;
    public static void revealProgress(int done, int total) { revealDone = done; revealTotal = total; }
    public static boolean revealing() { return revealTotal > revealDone; }
    public static int revealPercent() { return revealTotal == 0 ? 0 : (int) (100L * revealDone / revealTotal); }
    public static ViewTile clientTile(int x, int z, int step) {
        var pages = CLIENT.get(step);
        return pages == null ? null : pages.get(ChunkPos.asLong(x, z));
    }
    public static long clientRevision() { return clientRevision; }
    public static int clientCount() { return clientCount; }
    public static long[] clientRadars() { return clientRadars; }
    public static long[] clientNests() { return clientNests; }
    public static long[] clientBiters() { return clientBiters; }
    public static void updateClient(long revision, int count, List<ViewTile> tiles, long[] radars, long[] nests, long[] biters) {
        for (ViewTile tile : tiles) CLIENT.computeIfAbsent(tile.step, ignored -> new HashMap<>()).put(ChunkPos.asLong(tile.x, tile.z), tile);
        clientRevision = revision; clientCount = count; clientRadars = radars;
        if (!Arrays.equals(clientNests, nests)) clientNestRevision++;
        clientNests = nests; clientBiters = biters;
    }
    public static void resetClient() { CLIENT.clear(); clientRevision = 0; clientCount = 0; clientRadars = new long[0]; clientNests = new long[0]; clientBiters = new long[0]; clientNestRevision = 0; revealDone = 0; revealTotal = 0; }
}

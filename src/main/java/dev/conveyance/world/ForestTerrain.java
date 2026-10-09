package dev.conveyance.world;

import dev.conveyance.factory.FactoryContent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/** Deterministic forests, clipped to each chunk to avoid cross-chunk generation writes. */
public final class ForestTerrain {
    private static final double DENSITY_MULTIPLIER = 1.5;
    private record Tree(int x, int z, int y, int height, int clearance, double priority) {
        BlockPos root() { return new BlockPos(x, y, z); }
    }
    private final long seed;
    private final TerracedTerrain terrain;
    public ForestTerrain(long seed, TerracedTerrain terrain) { this.seed = seed; this.terrain = terrain; }

    private List<Tree> trees(int minX, int minZ) {
        Map<Long, Tree> candidates = new HashMap<>();
        List<Tree> trees = new ArrayList<>();
        for (int gx = Math.floorDiv(minX - 3, 3); gx <= Math.floorDiv(minX + 18, 3); gx++) {
            for (int gz = Math.floorDiv(minZ - 3, 3); gz <= Math.floorDiv(minZ + 18, 3); gz++) {
                Tree tree = candidate(gx, gz, candidates);
                if (tree == null) continue;
                boolean crowded = false;
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    Tree other = candidate(gx + dx, gz + dz, candidates);
                    if (other != null && other.priority < tree.priority
                            && squared(other.x - tree.x) + squared(other.z - tree.z) < 9) crowded = true;
                }
                if (!crowded) trees.add(tree);
            }
        }
        trees.sort(Comparator.comparingDouble(Tree::priority).thenComparingInt(Tree::x).thenComparingInt(Tree::z));
        return trees;
    }
    public boolean[] canopyPixels(int minX, int minZ) {
        boolean[] pixels = new boolean[256];
        for (Tree tree : trees(minX, minZ)) {
            int radius = tree.height >= 10 ? 3 : 2;
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                int x = tree.x + dx, z = tree.z + dz;
                if (inside(x, z, minX, minZ) && squared(dx) + squared(dz) <= radius * radius + 1)
                    pixels[(z - minZ) * 16 + x - minX] = true;
            }
        }
        return pixels;
    }
    public void generate(ChunkAccess chunk) {
        int minX = chunk.getPos().getMinBlockX(), minZ = chunk.getPos().getMinBlockZ();
        List<Tree> trees = trees(minX, minZ);
        // Place every trunk first so overlapping foliage cannot cover a trunk.
        for (Tree tree : trees) {
            if (!inside(tree.x, tree.z, minX, minZ)) continue;
            for (int layer = 0; layer < tree.height - 1; layer++) {
                put(chunk, tree.root().above(layer), FactoryContent.TREE_LOG.get().defaultBlockState()
                        .setValue(ForestLogBlock.LAYER, layer).setValue(ForestLogBlock.HEIGHT, tree.height)
                        .setValue(ForestLogBlock.CLEARANCE, tree.clearance));
            }
        }
        for (Tree tree : trees) {
            for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
                if (!inside(tree.x + dx, tree.z + dz, minX, minZ)) continue;
                for (int layer = tree.clearance; layer < tree.height; layer++) {
                    if (!inCanopy(dx, layer, dz, tree.height, tree.clearance)) continue;
                    BlockPos pos = tree.root().offset(dx, layer, dz);
                    if (chunk.getBlockState(pos).isAir()) put(chunk, pos, ForestLeavesBlock.ownedBy(pos, tree.root()));
                }
            }
        }
    }
    public static boolean inCanopy(int dx, int layer, int dz, int height, int clearance) {
        if (layer < clearance || layer >= height) return false;
        int widest = height >= 10 ? 3 : 2;
        int radius = (int) Math.ceil(widest * Math.sin(Math.PI * (layer - clearance + 1) / (height - clearance + 1)));
        return squared(dx) + squared(dz) <= radius * radius + 1;
    }
    private Tree candidate(int gx, int gz, Map<Long, Tree> cache) {
        long key = ((long) gx << 32) ^ (gz & 0xffffffffL);
        if (cache.containsKey(key)) return cache.get(key);
        Tree tree = makeTree(gx, gz);
        cache.put(key, tree);
        return tree;
    }
    private Tree makeTree(int gx, int gz) {
        int x = gx * 3 + (int) (unit(gx, gz, 1) * 3), z = gz * 3 + (int) (unit(gx, gz, 2) * 3);
        if (Math.abs(x) < 18 && Math.abs(z) < 18) return null;
        double forest = noise(x, z, 96, 3) * 0.8 + noise(x, z, 32, 4) * 0.2;
        // A nearby grove makes the first Wood/crate/fuel loop quick to try.
        double groveX = x - 48.0, groveZ = z + 24.0;
        double grove = 1 - (groveX * groveX / 1600.0 + groveZ * groveZ / 900.0);
        forest = Math.max(forest, grove);
        double density = forest < 0.44 ? 0.008 : Math.min(0.92, 0.18 + (forest - 0.44) * 3.5);
        density = Math.min(1, density * DENSITY_MULTIPLIER);
        if (unit(gx, gz, 5) > density) return null;
        var cell = terrain.cellAt(x, z);
        if (cell.feature() != TerracedTerrain.Feature.PLAIN || cell.surfaceHeight() != TerracedTerrain.START_HEIGHT
                || terrain.oreAt(x, z) != TerracedTerrain.Ore.NONE) return null;
        int height = 7 + (int) (unit(gx, gz, 6) * 6);
        int clearance = 2 + (int) (unit(gx, gz, 7) * 2);
        return new Tree(x, z, cell.surfaceHeight(), height, clearance, unit(gx, gz, 8));
    }
    private static boolean inside(int x, int z, int minX, int minZ) { return x >= minX && x < minX + 16 && z >= minZ && z < minZ + 16; }
    private static int squared(int value) { return value * value; }
    private static void put(ChunkAccess chunk, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        chunk.setBlockState(pos, state, false);
        chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG).update(pos.getX() & 15, pos.getY(), pos.getZ() & 15, state);
        chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG).update(pos.getX() & 15, pos.getY(), pos.getZ() & 15, state);
    }
    private double noise(int x, int z, int scale, long salt) {
        int gx = Math.floorDiv(x, scale), gz = Math.floorDiv(z, scale);
        double tx = smooth(Math.floorMod(x, scale) / (double) scale), tz = smooth(Math.floorMod(z, scale) / (double) scale);
        return lerp(tz, lerp(tx, unit(gx, gz, salt), unit(gx + 1, gz, salt)), lerp(tx, unit(gx, gz + 1, salt), unit(gx + 1, gz + 1, salt)));
    }
    private double unit(int x, int z, long salt) {
        long bits = seed ^ (salt * 0x632BE59BD9B4E019L) ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL);
        bits = (bits ^ (bits >>> 30)) * 0xBF58476D1CE4E5B9L;
        bits = (bits ^ (bits >>> 27)) * 0x94D049BB133111EBL;
        bits ^= bits >>> 31;
        return (bits >>> 11) * 0x1.0p-53;
    }
    private static double smooth(double value) { return value * value * (3 - 2 * value); }
    private static double lerp(double t, double from, double to) { return from + (to - from) * t; }
}

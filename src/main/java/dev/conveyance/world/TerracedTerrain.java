package dev.conveyance.world;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** Pure, seed-dependent terrain rules. All coordinates are global block coordinates. */
public final class TerracedTerrain {
    public static final int CELL_WIDTH = 4;
    public static final int LAYER_HEIGHT = 3;
    public static final int MIN_Y = -32;
    public static final int WORLD_HEIGHT = 64;
    public static final int BASE_GROUND_Y = 0;
    public static final int START_HEIGHT = BASE_GROUND_Y + 1;
    public static final int MAX_MOUNTAIN_SECTIONS = 6;
    public static final int SEA_LEVEL = 0;
    private static final int RIVER_SPACING = 64;
    private final long seed;
    private static final int REGION_WIDTH = 48;
    private final ConcurrentHashMap<Long, List<Wall>> regions = new ConcurrentHashMap<>();
    private final List<Wall> startingWalls;
    private final ConcurrentHashMap<Long, List<Channel>> rivers = new ConcurrentHashMap<>();
    private final Channel startingRiver;
    private static final int ORE_REGION_WIDTH = 128;
    private final ConcurrentHashMap<Long, List<OrePatch>> oreRegions = new ConcurrentHashMap<>();
    private final List<OrePatch> startingOres;

    public enum Feature { PLAIN, LOW_RIDGE, ROCK_WALL, SHORE, SEA, RIVER }
    public enum Ore { NONE, IRON, COPPER, COAL, STONE }
    private record OrePatch(Ore ore, double x, double z, double radiusX, double radiusZ,
            double angle, long salt) {}
    public record Cell(int surfaceHeight, Feature feature, boolean dry) {
        public boolean water() { return feature == Feature.SEA || feature == Feature.RIVER; }
        public int waterDepth() { return water() ? SEA_LEVEL - surfaceHeight : 0; }
    }
    private record WaterArea(Feature feature, int depth) {}

    private record Channel(double halfWidth, double[] x, double[] z) {
        double bankDistance(double px, double pz) {
            double nearest = Double.POSITIVE_INFINITY;
            for (int i = 0; i < x.length - 1; i++) {
                double dx = x[i + 1] - x[i], dz = z[i + 1] - z[i];
                double along = clamp(((px - x[i]) * dx + (pz - z[i]) * dz) / (dx * dx + dz * dz));
                double ox = px - lerp(along, x[i], x[i + 1]);
                double oz = pz - lerp(along, z[i], z[i + 1]);
                nearest = Math.min(nearest, ox * ox + oz * oz);
            }
            return halfWidth - Math.sqrt(nearest);
        }
    }

    private record Wall(Feature feature, double halfWidth, double[] x, double[] z, int[] peaks) {
        int sectionsAt(double px, double pz) {
            int result = 0;
            for (int i = 0; i < x.length - 1; i++) {
                double dx = x[i + 1] - x[i], dz = z[i + 1] - z[i];
                double along = clamp(((px - x[i]) * dx + (pz - z[i]) * dz) / (dx * dx + dz * dz));
                double rx = px - lerp(along, x[i], x[i + 1]);
                double rz = pz - lerp(along, z[i], z[i + 1]);
                double crossSection = 1 - (rx * rx + rz * rz) / (halfWidth * halfWidth);
                if (crossSection > 0) {
                    double crest = lerp(along, peaks[i], peaks[i + 1]);
                    result = Math.max(result, (int) Math.round(crest * crossSection));
                }
            }
            return result;
        }
    }

    public TerracedTerrain(long seed) {
        this.seed = seed;
        // Nearby examples make this iteration quick to try. The seed rotates
        // their layout; the rest of the world uses the same kinds of paths.
        int rotation = (int) Math.floor(unit(0, 0, 0x3C6EF372L) * 4);
        startingWalls = List.of(
                rotate(new Wall(Feature.LOW_RIDGE, 1.8,
                        new double[] {-18, -12, -6, 0, 6, 12, 18},
                        new double[] {-8, -7, -10, -9, -7, -8, -10},
                        new int[] {0, 2, 3, 0, 2, 3, 0}), rotation),
                rotate(new Wall(Feature.ROCK_WALL, 1.4,
                        new double[] {9, 10, 13, 12, 15, 12},
                        new double[] {6, 10, 14, 18, 22, 26},
                        new int[] {0, 4, 6, 0, 5, 0}), rotation));
        double[] left = riverNode(-1, 0), right = riverNode(0, 0);
        startingRiver = new Channel(3.5,
                new double[] {left[0], -20, -10, 0, 10, 20, right[0]},
                new double[] {left[1], 12, 8, 8, 10, 14, right[1]});
        startingOres = List.of(
                new OrePatch(Ore.IRON, -8, -8, 5.5, 4.5, 0.3, 0x428A2F98L),
                new OrePatch(Ore.COPPER, 8, -8, 5.0, 5.5, -0.4, 0x71374491L),
                new OrePatch(Ore.COAL, -8, 8, 5.5, 4.0, 0.6, 0xB5C0FBCFL),
                new OrePatch(Ore.STONE, 8, 8, 5.5, 5.0, -0.2, 0x3956C25BL));
    }

    /** First block above solid ground or seabed; water may occupy this position. */
    public int surfaceHeight(int blockX, int blockZ) {
        return cellAt(blockX, blockZ).surfaceHeight();
    }

    public Cell cellAt(int blockX, int blockZ) {
        int cellX = Math.floorDiv(blockX, CELL_WIDTH), cellZ = Math.floorDiv(blockZ, CELL_WIDTH);
        double px = cellX + 0.5, pz = cellZ + 0.5;
        boolean dry = isDryGround(blockX, blockZ);
        WaterArea water = waterArea(cellX, cellZ);
        boolean edge = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                boolean neighborWater = waterArea(cellX + dx, cellZ + dz) != null;
                if (neighborWater != (water != null)) edge = true;
            }
        }
        if (water != null) {
            // Any wet cell bordering land is exactly one block deep. Interior
            // beds use depths 1, 4, 7... below the same level water surface.
            int depth = edge ? 1 : water.depth();
            return new Cell(SEA_LEVEL - depth, water.feature(), false);
        }
        if (edge) {
            // Base feet Y=1 -> dry shore feet Y=0 -> shallow-water feet Y=-1.
            return new Cell(SEA_LEVEL, Feature.SHORE, false);
        }
        if (Math.max(Math.abs(px), Math.abs(pz)) < 4) {
            return new Cell(START_HEIGHT, Feature.PLAIN, dry);
        }
        if (oceanField(cellX, cellZ) > 0.08 || riverBankDistance(px, pz) > -2) {
            return new Cell(START_HEIGHT, Feature.PLAIN, dry);
        }
        int sections = 0;
        Feature feature = Feature.PLAIN;
        int regionX = Math.floorDiv(cellX, REGION_WIDTH), regionZ = Math.floorDiv(cellZ, REGION_WIDTH);
        for (int rx = regionX - 1; rx <= regionX + 1; rx++) {
            for (int rz = regionZ - 1; rz <= regionZ + 1; rz++) {
                for (Wall wall : region(rx, rz)) {
                    int candidate = wall.sectionsAt(px, pz);
                    if (candidate > sections || (candidate == sections && candidate > 0
                            && wall.feature() == Feature.ROCK_WALL)) {
                        sections = candidate;
                        feature = wall.feature();
                    }
                }
            }
        }
        for (Wall wall : startingWalls) {
            int candidate = wall.sectionsAt(px, pz);
            if (candidate > sections || (candidate == sections && candidate > 0
                    && wall.feature() == Feature.ROCK_WALL)) {
                sections = candidate;
                feature = wall.feature();
            }
        }
        return new Cell(START_HEIGHT + Math.min(MAX_MOUNTAIN_SECTIONS, sections) * LAYER_HEIGHT, feature, dry);
    }

    private WaterArea waterArea(int x, int z) {
        if (Math.max(Math.abs(x + 0.5), Math.abs(z + 0.5)) < 4) return null;
        double sea = oceanField(x, z);
        if (sea > 0.2) {
            int sections = Math.min(6, (int) ((sea - 0.2) / 0.2));
            return new WaterArea(Feature.SEA, 1 + sections * LAYER_HEIGHT);
        }
        double bank = riverBankDistance(x + 0.5, z + 0.5);
        if (bank >= 0) {
            int sections = Math.min(3, (int) Math.max(0, Math.floor((bank - 1.5) / 1.5)));
            return new WaterArea(Feature.RIVER, 1 + sections * LAYER_HEIGHT);
        }
        return null;
    }

    private double oceanField(int x, int z) {
        double distance = Math.max(Math.abs(x + 0.5), Math.abs(z + 0.5));
        double startingLand = 1 - smooth(clamp((distance - 8) / 32));
        return noise(x, z, 160, 0x510E527FL) + 0.2 * noise(x, z, 48, 0x9B05688CL)
                - 1.4 * startingLand;
    }

    private double riverBankDistance(double x, double z) {
        double distance = startingRiver.bankDistance(x, z);
        int rx = (int) Math.floor(x / RIVER_SPACING), rz = (int) Math.floor(z / RIVER_SPACING);
        for (int ix = rx - 1; ix <= rx + 1; ix++) {
            for (int iz = rz - 1; iz <= rz + 1; iz++) {
                long key = ((long) ix << 32) ^ (iz & 0xffffffffL);
                int regionX = ix, regionZ = iz;
                var channels = rivers.computeIfAbsent(key, ignored -> List.of(
                        makeChannel(regionX, regionZ, true), makeChannel(regionX, regionZ, false)));
                for (Channel channel : channels) distance = Math.max(distance, channel.bankDistance(x, z));
            }
        }
        return distance;
    }

    private double[] riverNode(int x, int z) {
        return new double[] {
                (x + 0.5) * RIVER_SPACING + (unit(x, z, 0x1F83D9ABL) - 0.5) * 20,
                (z + 0.5) * RIVER_SPACING + (unit(x, z, 0x5BE0CD19L) - 0.5) * 20};
    }

    private Channel makeChannel(int rx, int rz, boolean east) {
        double[] from = riverNode(rx, rz), to = riverNode(rx + (east ? 1 : 0), rz + (east ? 0 : 1));
        double dx = to[0] - from[0], dz = to[1] - from[1];
        double length = Math.sqrt(dx * dx + dz * dz);
        long salt = east ? 0xA54FF53AL : 0x3C6EF373L;
        double phase = unit(rx, rz, salt) * Math.PI * 2;
        double bend = 6 + unit(rx, rz, salt + 1) * 4;
        double[] x = new double[5], z = new double[5];
        for (int i = 0; i < 5; i++) {
            double t = i / 4.0;
            double sideways = Math.sin(t * Math.PI) * Math.sin(t * Math.PI * 2 + phase) * bend;
            x[i] = lerp(t, from[0], to[0]) - dz / length * sideways;
            z[i] = lerp(t, from[1], to[1]) + dx / length * sideways;
        }
        // Shared nodes make neighboring channels join across region/chunk edges.
        return new Channel(2.5 + unit(rx, rz, salt + 2) * 2.5, x, z);
    }

    private List<Wall> region(int x, int z) {
        long key = ((long) x << 32) ^ (z & 0xffffffffL);
        return regions.computeIfAbsent(key, ignored -> List.of(
                makeWall(x, z, Feature.LOW_RIDGE, 0x57AF6D12L),
                makeWall(x, z, Feature.ROCK_WALL, 0xC2B2AE35L)));
    }

    private Wall makeWall(int rx, int rz, Feature feature, long salt) {
        double cx = (rx + unit(rx, rz, salt)) * REGION_WIDTH;
        double cz = (rz + unit(rx, rz, salt + 1)) * REGION_WIDTH;
        double angle = unit(rx, rz, salt + 2) * Math.PI * 2;
        double length = 24 + unit(rx, rz, salt + 3) * 24;
        double bend = 3 + unit(rx, rz, salt + 4) * 4;
        double phase = unit(rx, rz, salt + 5) * Math.PI * 2;
        double[] x = new double[7], z = new double[7];
        int[] peaks = new int[7];
        int gap = 2 + (int) (unit(rx, rz, salt + 6) * 3);
        for (int i = 0; i < 7; i++) {
            double t = i / 6.0;
            double along = (t - 0.5) * length;
            double sideways = Math.sin(t * Math.PI * 2 + phase) * bend;
            x[i] = cx + Math.cos(angle) * along - Math.sin(angle) * sideways;
            z[i] = cz + Math.sin(angle) * along + Math.cos(angle) * sideways;
            peaks[i] = feature == Feature.ROCK_WALL
                    ? 3 + (int) (unit(rx, rz, salt + 10 + i) * 4)
                    : 1 + (int) (unit(rx, rz, salt + 10 + i) * 3);
        }
        peaks[0] = peaks[6] = peaks[gap] = 0;
        return new Wall(feature, feature == Feature.ROCK_WALL ? 1.4 : 1.8, x, z, peaks);
    }

    private static Wall rotate(Wall wall, int turns) {
        double[] x = wall.x().clone(), z = wall.z().clone();
        for (int i = 0; i < x.length; i++) {
            for (int turn = 0; turn < turns; turn++) {
                double previous = x[i]; x[i] = -z[i]; z[i] = previous;
            }
        }
        return new Wall(wall.feature(), wall.halfWidth(), x, z, wall.peaks());
    }

    private double unit(int x, int z, long salt) {
        return (value(x, z, salt) + 1) / 2;
    }

    /** Per-block resource footprints, independent of the four-block terrain grid. */
    public Ore oreAt(int blockX, int blockZ) {
        for (OrePatch patch : startingOres) {
            if (insidePatch(patch, blockX, blockZ)) return patch.ore();
        }
        int rx = Math.floorDiv(blockX, ORE_REGION_WIDTH), rz = Math.floorDiv(blockZ, ORE_REGION_WIDTH);
        for (int x = rx - 1; x <= rx + 1; x++) {
            for (int z = rz - 1; z <= rz + 1; z++) {
                long key = ((long) x << 32) ^ (z & 0xffffffffL);
                int regionX = x, regionZ = z;
                var patches = oreRegions.computeIfAbsent(key, ignored -> List.of(
                        makeOre(regionX, regionZ, Ore.IRON),
                        makeOre(regionX, regionZ, Ore.COPPER),
                        makeOre(regionX, regionZ, Ore.COAL),
                        makeOre(regionX, regionZ, Ore.STONE)));
                for (OrePatch patch : patches) {
                    if (insidePatch(patch, blockX, blockZ)) return patch.ore();
                }
            }
        }
        return Ore.NONE;
    }

    private OrePatch makeOre(int x, int z, Ore ore) {
        long salt = 0xE9B5DBA5L + ore.ordinal() * 97L;
        return new OrePatch(ore,
                (x + unit(x, z, salt)) * ORE_REGION_WIDTH,
                (z + unit(x, z, salt + 1)) * ORE_REGION_WIDTH,
                7 + unit(x, z, salt + 2) * 8,
                6 + unit(x, z, salt + 3) * 7,
                unit(x, z, salt + 4) * Math.PI * 2, salt);
    }

    private boolean insidePatch(OrePatch patch, int x, int z) {
        double dx = x + 0.5 - patch.x(), dz = z + 0.5 - patch.z();
        double cos = Math.cos(patch.angle()), sin = Math.sin(patch.angle());
        double px = (dx * cos + dz * sin) / patch.radiusX();
        double pz = (-dx * sin + dz * cos) / patch.radiusZ();
        double radius = px * px + pz * pz;
        if (radius > 1.35) return false;
        double edge = 1 + 0.2 * noise(x, z, 4, patch.salt())
                + 0.12 * noise(x, z, 11, patch.salt() + 1);
        return radius <= edge;
    }

    public boolean isDryGround(int blockX, int blockZ) {
        return noise(Math.floorDiv(blockX, CELL_WIDTH), Math.floorDiv(blockZ, CELL_WIDTH),
                64, 0x6A09E667L) < -0.15;
    }

    private double noise(int x, int z, int scale, long salt) {
        int gx = Math.floorDiv(x, scale);
        int gz = Math.floorDiv(z, scale);
        double tx = smooth(Math.floorMod(x, scale) / (double) scale);
        double tz = smooth(Math.floorMod(z, scale) / (double) scale);
        return lerp(tz, lerp(tx, value(gx, gz, salt), value(gx + 1, gz, salt)),
                lerp(tx, value(gx, gz + 1, salt), value(gx + 1, gz + 1, salt)));
    }

    private double value(int x, int z, long salt) {
        long bits = seed ^ salt ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL);
        bits = (bits ^ (bits >>> 30)) * 0xBF58476D1CE4E5B9L;
        bits = (bits ^ (bits >>> 27)) * 0x94D049BB133111EBL;
        bits ^= bits >>> 31;
        return (bits >>> 11) * 0x1.0p-53 * 2 - 1;
    }

    private static double clamp(double value) { return Math.max(0, Math.min(1, value)); }
    private static double smooth(double value) { return value * value * (3 - 2 * value); }
    private static double lerp(double t, double from, double to) { return from + (to - from) * t; }
}

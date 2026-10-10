package dev.conveyance.world;

import java.util.ArrayList;
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
    private final long seed;
    private static final int REGION_WIDTH = 48;
    private final ConcurrentHashMap<Long, List<Wall>> regions = new ConcurrentHashMap<>();
    private final Basin startingLake;
    private final Basin startingGrove;
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

    private record Basin(double x, double z, double radiusX, double radiusZ, double angle) {
        double radius(double px, double pz) {
            double dx = px - x, dz = pz - z;
            double cos = Math.cos(angle), sin = Math.sin(angle);
            double rx = (dx * cos + dz * sin) / radiusX;
            double rz = (-dx * sin + dz * cos) / radiusZ;
            return Math.sqrt(rx * rx + rz * rz);
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
        // A roomy initial factory area, with separated deposits around it and
        // water outside it. The seed rotates the whole layout by any angle.
        double angle = unit(0, 0, 0x3C6EF372L) * Math.PI * 2;
        startingLake = new Basin(Math.cos(angle) * 36, Math.sin(angle) * 36,
                8 + unit(0, 0, 0x510E527EL) * 2, 5 + unit(0, 0, 0x510E527DL) * 2, angle + 0.6);
        startingGrove = new Basin(Math.cos(angle + 2.2) * 28, Math.sin(angle + 2.2) * 28, 7, 6, angle);
        var ores = new ArrayList<OrePatch>();
        for (Ore ore : List.of(Ore.IRON, Ore.COPPER, Ore.COAL, Ore.STONE)) {
            long salt = 0x428A2F98L + ore.ordinal() * 97L;
            double direction = angle + (ore.ordinal() - 1) * Math.PI / 2
                    + (unit(0, 0, salt) - 0.5) * 0.3;
            double distance = 45 + unit(0, 0, salt + 1) * 20;
            ores.add(new OrePatch(ore, Math.cos(direction) * distance, Math.sin(direction) * distance,
                    5.5 + unit(0, 0, salt + 2), 4.5 + unit(0, 0, salt + 3), direction + 0.4, salt));
        }
        startingOres = List.copyOf(ores);
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
        if (Math.hypot(px, pz) < 22 || openArea(px, pz) > 0.6) {
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
        return new Cell(START_HEIGHT + Math.min(MAX_MOUNTAIN_SECTIONS, sections) * LAYER_HEIGHT, feature, dry);
    }

    private WaterArea waterArea(int x, int z) {
        double px = x + 0.5, pz = z + 0.5;
        if (Math.hypot(px, pz) < 22) return null;
        double lake = 1 - startingLake.radius(px, pz) + 0.1 * noise(px, pz, 5, 0x510E527CL);
        if (lake > 0) return new WaterArea(Feature.SEA, 1 + Math.min(3, (int) (lake * 4)) * LAYER_HEIGHT);
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
        double px = x + 0.5, pz = z + 0.5;
        double startingLand = 1 - smooth(clamp((Math.hypot(px, pz) - 22) / 20));
        double wx = warpedX(px, pz), wz = warpedZ(px, pz);
        return noise(wx, wz, 150, 0x510E527FL) + 0.25 * noise(wx, wz, 48, 0x9B05688CL)
                + 0.08 * noise(wx, wz, 16, 0x9B05688DL) - 0.1 * openArea(px, pz) - 1.4 * startingLand;
    }

    private double riverBankDistance(double x, double z) {
        double startingLand = smooth(clamp((Math.hypot(x, z) - 22) / 20));
        double wx = warpedX(x, z), wz = warpedZ(x, z);
        // Sparse contours rather than a channel on every region edge. The
        // independent flow mask tapers their ends and leaves large land spans.
        double flow = smooth(clamp((noise(wx, wz, 190, 0xA54FF53AL) - 0.05) / 0.35));
        if (flow == 0 || startingLand == 0) return -100;
        double valley = Math.abs(noise(wx, wz, 94, 0x3C6EF373L)
                + 0.22 * noise(wx, wz, 36, 0x3C6EF374L));
        double width = (0.035 + 0.02 * unitNoise(wx, wz, 70, 0x3C6EF375L)) * flow * startingLand;
        return (width - valley) * 80;
    }

    private double warpedX(double x, double z) {
        return x + 24 * noise(x, z, 96, 0x1F83D9ABL) + 6 * noise(x, z, 32, 0x1F83D9ACL);
    }
    private double warpedZ(double x, double z) {
        return z + 24 * noise(x, z, 96, 0x5BE0CD19L) + 6 * noise(x, z, 32, 0x5BE0CD1AL);
    }

    /** Shared by cliffs and forests so clear plains really have building room. */
    public double openAreaWeight(int blockX, int blockZ) {
        return openArea(Math.floorDiv(blockX, CELL_WIDTH) + 0.5, Math.floorDiv(blockZ, CELL_WIDTH) + 0.5);
    }
    public double forestCover(int blockX, int blockZ) {
        double x = blockX / (double) CELL_WIDTH, z = blockZ / (double) CELL_WIDTH;
        return 0.8 * unitNoise(x, z, 32, 0x6A09E668L) + 0.2 * unitNoise(x, z, 12, 0x6A09E669L);
    }
    public double startingGroveWeight(int blockX, int blockZ) {
        double radius = startingGrove.radius(blockX / (double) CELL_WIDTH, blockZ / (double) CELL_WIDTH);
        return Math.max(0, 1 - radius * radius);
    }
    private double openArea(double x, double z) {
        double start = 1 - smooth(clamp((Math.hypot(x, z) - 16) / 10));
        double field = noise(x, z, 112, 0xBB67AE85L) + 0.2 * noise(x, z, 40, 0xBB67AE86L);
        return Math.max(start, smooth(clamp((field - 0.05) / 0.42)));
    }

    private List<Wall> region(int x, int z) {
        long key = ((long) x << 32) ^ (z & 0xffffffffL);
        return regions.computeIfAbsent(key, ignored -> {
            var walls = new ArrayList<Wall>();
            if (unit(x, z, 0x57AF6D11L) < 0.72) walls.add(makeWall(x, z, Feature.LOW_RIDGE, 0x57AF6D12L));
            if (unit(x, z, 0xC2B2AE34L) < 0.55) walls.add(makeWall(x, z, Feature.ROCK_WALL, 0xC2B2AE35L));
            return List.copyOf(walls);
        });
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

    private double unit(int x, int z, long salt) {
        return (value(x, z, salt) + 1) / 2;
    }

    /** Per-block resource footprints, independent of the four-block terrain grid. */
    public Ore oreAt(int blockX, int blockZ) {
        for (OrePatch patch : startingOres) {
            if (insidePatch(patch, blockX, blockZ)) return patch.ore();
        }
        // Random deposits cannot crowd the four deliberately spaced starters.
        if (Math.hypot(blockX + 0.5, blockZ + 0.5) < 96) return Ore.NONE;
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

    private double unitNoise(double x, double z, int scale, long salt) { return (noise(x, z, scale, salt) + 1) / 2; }

    // Continuous simplex noise avoids the axis-aligned plateaus of interpolated
    // square cells. Hashing lattice gradients keeps it seed-dependent everywhere.
    private double noise(double x, double z, int scale, long salt) {
        double px = x / scale + 19.19 + ((salt >>> 8) & 1023) * 0.031;
        double pz = z / scale - 27.73 + ((salt >>> 20) & 1023) * 0.029;
        double skew = (px + pz) * 0.3660254037844386;
        int ix = (int) Math.floor(px + skew), iz = (int) Math.floor(pz + skew);
        double unskew = (ix + iz) * 0.21132486540518713;
        double x0 = px - (ix - unskew), z0 = pz - (iz - unskew);
        int dx = x0 > z0 ? 1 : 0, dz = 1 - dx;
        return 70 * (corner(ix, iz, x0, z0, salt)
                + corner(ix + dx, iz + dz, x0 - dx + 0.21132486540518713, z0 - dz + 0.21132486540518713, salt)
                + corner(ix + 1, iz + 1, x0 - 0.5773502691896257, z0 - 0.5773502691896257, salt));
    }
    private double corner(int x, int z, double dx, double dz, long salt) {
        double falloff = 0.5 - dx * dx - dz * dz;
        if (falloff <= 0) return 0;
        double gradient = switch ((int) (unit(x, z, salt) * 8)) {
            case 0 -> dx + dz;
            case 1 -> dx - dz;
            case 2 -> -dx + dz;
            case 3 -> -dx - dz;
            case 4 -> dx;
            case 5 -> -dx;
            case 6 -> dz;
            default -> -dz;
        };
        falloff *= falloff;
        return falloff * falloff * gradient;
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

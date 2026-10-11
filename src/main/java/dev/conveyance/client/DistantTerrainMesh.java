package dev.conveyance.client;

import dev.conveyance.world.DistantTerrainNetwork;
import dev.conveyance.world.ForestTerrain;
import dev.conveyance.world.SurfacePalette;
import dev.conveyance.world.TerracedTerrain;
import java.util.ArrayList;
import java.util.List;

/** CPU-only geometry. No client world, texture atlas, chunks or GL calls are used here. */
final class DistantTerrainMesh {
    private static final float WATER_HEIGHT = -1 / 9F - .001F;
    static final int GRASS = 0, DIRT = 1, SAND = 2, GRANITE = 3, STONE = 4, WATER = 5,
            IRON = 6, COPPER = 7, COAL = 8, URANIUM = 9, OIL = 10, LOG = 11, LEAVES = 12;
    record Quad(float[] positions, int texture, int colour, int skyLight) {
        Quad(float[] positions, int texture, int colour) { this(positions, texture, colour, 15); }
    }
    record Data(int x, int z, List<Quad> faces) {}
    private final DistantTerrainNetwork.Source source;
    private final TerracedTerrain terrain;
    private final ForestTerrain forest;
    DistantTerrainMesh(DistantTerrainNetwork.Source source) {
        this.source = source;
        terrain = new TerracedTerrain(source.seed(), source.generation());
        forest = new ForestTerrain(source.seed(), terrain);
    }
    void trim(int x, int z) { terrain.trimPredictionCaches(x, z, DistantTerrainRenderer.DISTANCE + 128); }
    Data build(int pageX, int pageZ) {
        int baseX = pageX * DistantTerrainRenderer.PAGE, baseZ = pageZ * DistantTerrainRenderer.PAGE;
        var faces = new ArrayList<Quad>();
        var cells = new TerracedTerrain.Cell[18][18];
        for (int z = -1; z <= 16; z++) for (int x = -1; x <= 16; x++) cells[z + 1][x + 1] = terrain.cellAt(baseX + x * 4, baseZ + z * 4);
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            if (Thread.currentThread().isInterrupted()) return new Data(pageX, pageZ, List.of());
            var cell = cells[z + 1][x + 1];
            int wx = baseX + x * 4, wz = baseZ + z * 4;
            float lx = x * 4, lz = z * 4, y = cell.surfaceHeight() - .02F;
            int texture = texture(cell);
            int colour = texture == GRASS ? SurfacePalette.base(source.colourSeed(), wx + 2, wz + 2) : 0xFFFFFF;
            if (cell.water()) {
                top(faces, lx, y, lz, lx + 4, lz + 4, SAND, 0xFFFFFF, Math.max(0, 15 - cell.waterDepth()));
                top(faces, lx, WATER_HEIGHT, lz, lx + 4, lz + 4, WATER, 0xFFFFFF);
            } else if (cell.feature() == TerracedTerrain.Feature.PLAIN && cell.surfaceHeight() == TerracedTerrain.START_HEIGHT) {
                int[] materials = new int[16]; boolean resource = false;
                for (int dz = 0; dz < 4; dz++) for (int dx = 0; dx < 4; dx++) {
                    int material = ore(wx + dx, wz + dz);
                    materials[dz * 4 + dx] = material; resource |= material != GRASS;
                }
                if (resource) {
                    for (int dz = 0; dz < 4; dz++) for (int dx = 0; dx < 4; dx++) {
                        int material = materials[dz * 4 + dx];
                        top(faces, lx + dx, y, lz + dz, lx + dx + 1, lz + dz + 1, material, material == GRASS ? colour : 0xFFFFFF);
                    }
                } else top(faces, lx, y, lz, lx + 4, lz + 4, texture, colour);
            } else top(faces, lx, y, lz, lx + 4, lz + 4, texture, colour);
            int side = texture == GRASS ? DIRT : texture;
            int light = cell.water() ? Math.max(0, 15 - cell.waterDepth()) : 15;
            wall(faces, lx, lz, lx + 4, lz, y, height(cells[z][x + 1]), side, light);
            wall(faces, lx + 4, lz + 4, lx, lz + 4, y, height(cells[z + 2][x + 1]), side, light);
            wall(faces, lx, lz + 4, lx, lz, y, height(cells[z + 1][x]), side, light);
            wall(faces, lx + 4, lz, lx + 4, lz + 4, y, height(cells[z + 1][x + 2]), side, light);
        }
        for (int z = 0; z < 64; z += 16) for (int x = 0; x < 64; x += 16) {
            for (var tree : forest.visualTrees(baseX + x, baseZ + z)) {
                float tx = tree.x() - baseX, tz = tree.z() - baseZ;
                float radius = tree.height() >= 10 ? 2.5F : 1.9F;
                box(faces, tx + .16F, tree.y(), tz + .16F, tx + .84F, tree.y() + tree.height() - 1, tz + .84F, LOG, 0xFFFFFF);
                float bottom = tree.y() + tree.clearance(), middle = tree.y() + tree.height() * .65F, top = tree.y() + tree.height() - .2F;
                box(faces, tx + .5F - radius, bottom, tz + .5F - radius, tx + .5F + radius, middle, tz + .5F + radius, LEAVES, source.foliage());
                box(faces, tx + .5F - radius * .72F, middle, tz + .5F - radius * .72F, tx + .5F + radius * .72F, top, tz + .5F + radius * .72F, LEAVES, source.foliage());
            }
        }
        return new Data(pageX, pageZ, faces);
    }
    private int ore(int x, int z) {
        return switch (terrain.oreAt(x, z)) {
            case IRON -> IRON; case COPPER -> COPPER; case COAL -> COAL; case STONE -> STONE; case URANIUM -> URANIUM;
            case NONE -> terrain.oilAt(x, z) ? OIL : GRASS;
        };
    }
    private static int texture(TerracedTerrain.Cell cell) {
        return switch (cell.feature()) {
            case LOW_RIDGE -> GRANITE; case ROCK_WALL -> STONE; case SEA, RIVER, SHORE -> SAND; default -> GRASS;
        };
    }
    private static float height(TerracedTerrain.Cell cell) { return cell.surfaceHeight() - .02F; }
    private static void top(List<Quad> out, float x0, float y, float z0, float x1, float z1, int texture, int colour) {
        top(out, x0, y, z0, x1, z1, texture, colour, 15);
    }
    private static void top(List<Quad> out, float x0, float y, float z0, float x1, float z1, int texture, int colour, int skyLight) {
        out.add(new Quad(new float[]{x0, y, z0, x0, y, z1, x1, y, z1, x1, y, z0}, texture, colour, skyLight));
    }
    private static void wall(List<Quad> out, float x0, float z0, float x1, float z1, float high, float low, int texture, int skyLight) {
        if (high <= low) return;
        out.add(new Quad(new float[]{x0, low, z0, x0, high, z0, x1, high, z1, x1, low, z1}, texture, 0xFFFFFF, skyLight));
    }
    private static void box(List<Quad> out, float x0, float y0, float z0, float x1, float y1, float z1, int texture, int colour) {
        top(out, x0, y1, z0, x1, z1, texture, colour);
        out.add(new Quad(new float[]{x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1}, texture, colour));
        out.add(new Quad(new float[]{x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0}, texture, colour));
        out.add(new Quad(new float[]{x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1}, texture, colour));
        out.add(new Quad(new float[]{x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0}, texture, colour));
        out.add(new Quad(new float[]{x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1}, texture, colour));
    }
}

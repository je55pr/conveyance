package dev.conveyance.map;

import dev.conveyance.factory.FactoryContent;
import dev.conveyance.world.TerracedTerrain;
import dev.conveyance.world.TerrainBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class MapPalette {
    public static final byte UNKNOWN = 0, LAND = 1, DRY = 2, WATER = 3, SHORE = 4, ROCK = 5,
            RIDGE = 6, FOREST = 7, IRON = 8, COPPER = 9, COAL = 10, STONE = 11, FACTORY = 12;
    private static final int[] COLORS = {0x0C131B, 0xA2A46A, 0xA38C60, 0x467CA9, 0xC8BD91,
            0x828D96, 0xA17665, 0x465E36, 0xBCCBD4, 0xE29450, 0x30333C, 0xAAAAA0, 0xA1B7BA};
    private static final String[] NAMES = {"unknown", "land", "dry", "water", "shore", "rock", "ridge",
            "forest", "iron", "copper", "coal", "stone", "factory"};
    public static int color(int value) { return COLORS[value >= 0 && value < COLORS.length ? value : 0]; }
    public static String name(int value) { return NAMES[value >= 0 && value < NAMES.length ? value : 0]; }
    public static boolean isResource(int value) { return value >= IRON && value <= STONE; }
    public static int size() { return COLORS.length; }
    public static byte overview(byte[] pixels, int x, int z, int size) {
        if (size == 1) return pixels[z * 16 + x];
        int[] counts = new int[COLORS.length];
        for (int dz = 0; dz < size; dz++) for (int dx = 0; dx < size; dx++) {
            int value = pixels[(z + dz) * 16 + x + dx] & 255;
            if (value < counts.length) counts[value]++;
        }
        return dominant(counts, 0);
    }
    public static byte dominant(int[] counts, int offset) {
        // Small deposits stay visible when zooming out instead of disappearing
        // into the most common ground colour.
        int resource = IRON;
        for (int value = IRON + 1; value <= STONE; value++) if (counts[offset + value] > counts[offset + resource]) resource = value;
        if (counts[offset + resource] > 0) return (byte) resource;
        if (counts[offset + FACTORY] > 0) return FACTORY;
        int most = UNKNOWN;
        for (int value = 1; value < COLORS.length; value++) if (counts[offset + value] > counts[offset + most]) most = value;
        return (byte) most;
    }
    public static byte ore(TerracedTerrain.Ore ore) { return switch (ore) {
        case IRON -> IRON; case COPPER -> COPPER; case COAL -> COAL; case STONE -> STONE; default -> UNKNOWN;
    }; }
    public static byte terrain(TerracedTerrain.Cell cell, TerracedTerrain.Ore ore, boolean forest) {
        if (cell.water()) return WATER;
        if (cell.feature() == TerracedTerrain.Feature.SHORE) return SHORE;
        if (cell.feature() == TerracedTerrain.Feature.ROCK_WALL) return ROCK;
        if (cell.feature() == TerracedTerrain.Feature.LOW_RIDGE) return RIDGE;
        byte resource = ore(ore);
        return resource != UNKNOWN ? resource : forest ? FOREST : LAND;
    }
    public static byte resource(BlockState state) {
        state = TerrainBlocks.material(state);
        return state.is(BlockTags.IRON_ORES) ? IRON : state.is(BlockTags.COPPER_ORES) ? COPPER
                : state.is(BlockTags.COAL_ORES) ? COAL : UNKNOWN;
    }
    public static byte surface(BlockState state, int y) {
        state = TerrainBlocks.material(state);
        byte resource = resource(state);
        if (resource != UNKNOWN) return resource;
        if (state.is(FactoryContent.TREE_LOG.get()) || state.is(FactoryContent.TREE_LEAVES.get())) return FOREST;
        if (state.isAir()) return UNKNOWN;
        if (state.is(Blocks.WATER)) return WATER;
        if (state.is(Blocks.SAND)) return SHORE;
        if (state.is(Blocks.GRANITE)) return RIDGE;
        if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Blocks.COBBLESTONE)) return y > 0 ? ROCK : STONE;
        if (BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("conveyance")) return FACTORY;
        return state.is(Blocks.COARSE_DIRT) ? DRY : LAND;
    }
    private MapPalette() {}
}

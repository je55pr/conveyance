package dev.conveyance.client;

import dev.conveyance.pollution.PollutionData;
import dev.conveyance.world.SurfacePalette;
import java.util.HashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

/** Tint meshes only need rebuilding when a visible colour band changes. */
public final class SurfaceColors {
    public static int grass(BlockPos pos) {
        var field = PollutionData.clientField();
        return SurfacePalette.overlay(SurfacePalette.base(field.seed(), pos.getX(), pos.getZ()), field.pollutionAt(pos.getX(), pos.getZ()), field.territoryAt(pos.getX(), pos.getZ()));
    }
    public static void receive(int minX, int minZ, long seed, float[] pollution, float[] territory) {
        var old = PollutionData.clientField();
        PollutionData.updateClient(minX, minZ, seed, pollution, territory); var current = PollutionData.clientField();
        var game = Minecraft.getInstance(); if (game.level == null) return;
        if (old.seed() != seed) { game.level.clearTintCaches(); game.levelRenderer.allChanged(); return; }
        var chunks = new HashSet<Long>(); include(chunks, old); include(chunks, current);
        var dirty = new HashSet<Long>();
        for (long packed : chunks) {
            int cx = ChunkPos.getX(packed), cz = ChunkPos.getZ(packed);
            if (tone(old, cx, cz) == tone(current, cx, cz)) continue;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) dirty.add(ChunkPos.asLong(cx + dx, cz + dz));
        }
        for (long packed : dirty) {
            int cx = ChunkPos.getX(packed), cz = ChunkPos.getZ(packed);
            if (game.level.getChunkSource().getChunkNow(cx, cz) == null) continue;
            for (int sy = game.level.getMinSection(); sy < game.level.getMaxSection(); sy++) game.levelRenderer.setSectionDirty(cx, sy, cz);
        }
    }
    private static int tone(PollutionData.ClientField field, int cx, int cz) {
        return (int) (SurfacePalette.pollutionStrength(field.pollutionAt(cx * 16 + 8, cz * 16 + 8)) * 16)
                | (int) (field.territoryAt(cx * 16 + 8, cz * 16 + 8) * 16) << 8;
    }
    private static void include(HashSet<Long> chunks, PollutionData.ClientField field) {
        for (int x = field.minX() - 1; x <= field.minX() + PollutionData.AMBIENT_WIDTH; x++)
            for (int z = field.minZ() - 1; z <= field.minZ() + PollutionData.AMBIENT_WIDTH; z++) chunks.add(ChunkPos.asLong(x, z));
    }
    private SurfaceColors() {}
}
